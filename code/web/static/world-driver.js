const DEFAULT_COLOR = 13156520;
const TILT_DEFAULT = 1960;
const TILT_MIN = 1024;
const TILT_MAX = 3072;
const TURN_STEP = 96;
const FULL_TURN = 16384;
const ORBIT_CYCLE_MS = 20;
const CYCLE_MS = 20;
const MAX_CYCLES_PER_FRAME = 10;
const BRIDGE_PLANE = 1;
const BRIDGE_FLAG = 2;
const MIDDLE_BUTTON = 1;
import { createDefinitionCache, createSceneService, heightGrid, hoverTargets, localTile, menuState, parseMeta, partitionDraw, parseNpcs, parseOverlay, sceneMaterials } from "./world-scene.js";

const HOOKS = ["beforeCycle", "afterCycle", "layers", "playerTile", "invalidate", "hoverTile"];
const SCENE_INTERVAL_MS = 600;

const unsigned = (value, max) => Number.isSafeInteger(value) && value >= 0 && value <= max;

export function createWorldDriver({
  app,
  canvas,
  session,
  runtime,
  openTextureSet,
  relay,
  plane = 0,
  randomUnit = 0.730967787376657,
  texturesEnabled = true,
  environment = {},
  regionSource = null,
  target = globalThis,
  clock = () => Math.round(performance.timeOrigin + performance.now()),
  onStatus = () => {},
  fetchArchive = null,
  token = "",
  sceneIntervalMs = SCENE_INTERVAL_MS,
  orbit = true,
  random = Math.random,
  minimapView = null,
  measureText = null,
  overheadPadding = 16,
  audio = null,
  ui = null,
} = {}) {
  for (const method of ["cycleInput", "prepareGround", "lowerGround", "loweredMaterial", "bakeShadows", "initialWorld", "sceneTile"]) {
    if (typeof session?.[method] !== "function") throw new Error(`world session lacks ${method}`);
  }
  if (typeof app?.terrain_begin !== "function" || typeof app?.terrain_layer !== "function" || typeof app?.terrain_show_live !== "function") throw new Error("renderer lacks live terrain");
  if (typeof runtime?.ClientInput !== "function" || typeof runtime?.HslRgbLookup !== "function") throw new Error("runtime lacks client input");
  const env = {
    enabled: environment.enabled ?? true,
    color: environment.color ?? DEFAULT_COLOR,
    depth: environment.depth ?? 0,
    offset: environment.offset ?? 0,
    sky: environment.sky ?? (0xff000000 | (environment.color ?? DEFAULT_COLOR)) | 0,
  };
  const hooks = Object.fromEntries(HOOKS.map((name) => [name, []]));
  const camera = { tilt: TILT_DEFAULT, compass: 0 };
  const held = new Set();
  const orbitOn = orbit === true && typeof app.terrain_show_orbit === "function" && typeof app.orbit_reset === "function";
  let orbitAt = null;
  let orbitDebt = 0;
  let dragging = false;
  let dragX = 0;
  let dragY = 0;
  let groundLive = false;
  const randomBits = () => Math.floor(random() * 8);
  const direction = (positive, negative) => (held.has(positive) ? 1 : 0) - (held.has(negative) ? 1 : 0);
  const stats = { frames: 0, shown: 0, cycles: 0, shadowRegions: 0, shadowTypes: 0, shadowModels: 0, errors: 0, sceneRefreshes: 0, sceneErrors: 0, sceneMeshes: 0, sceneTriangles: 0, sceneLayers: 0, staticLocs: 0, sceneTextures: 0, hoverFeeds: 0 };
  const sceneOn = typeof session.lendScene === "function" && typeof fetchArchive === "function" && typeof session.sceneHeight === "function";
  const lend = (use) => session.lendScene(use);
  let service = null;
  let definitions = null;
  let rgbTable = null;
  let sceneData = null;
  let sceneAt = -Infinity;
  let sceneSummary = null;
  let sceneMeta = [];
  let sceneNpcs = new Map();
  let sceneOverlay = null;
  let overlayDebt = 0;
  let sceneDebt = 0;
  let nextCycleAt = null;
  let ownIndex = -1;
  let worldBase = { x: 0, y: 0 };
  let mapSize = 104;
  let hoverAt = null;
  let hoverKey = "";
  let picker = null;
  let mouseAt = null;
  let menuReady = false;
  let texturing = false;
  const registered = new Set();
  let input = null;
  let textures = null;
  let layers = [];
  let shadowReport = null;
  let shadowError = null;
  let locState = null;
  let started = false;
  let stopped = false;
  let dirty = true;
  let lastKey = "";
  let lastError = null;
  let lastView = null;
  let previousInstalled;

  const status = (text) => { try { onStatus(text); } catch {} };
  const stop = (reason) => {
    if (stopped) return;
    stopped = true;
    lastError = reason;
    audio?.close();
    try { ui?.stop?.(); } catch {}
    target.removeEventListener?.("keydown", keyDown, true);
    target.removeEventListener?.("keyup", keyUp, true);
    target.removeEventListener?.("wheel", wheel, true);
    target.removeEventListener?.("mousedown", mouseDown, true);
    target.removeEventListener?.("mousemove", mouseMove, true);
    target.removeEventListener?.("mouseup", mouseUp, true);
    if (input && target.openrsClientInput === input) target.openrsClientInput = previousInstalled;
    try { service?.close(); } catch {}
    try { textures?.close(); } catch {}
    try { input?.free?.(); } catch {}
    try { picker?.free?.(); } catch {}
    status(`world driver stopped: ${reason}`);
  };

  const feedShadows = async (world) => {
    let source = regionSource;
    let owned = false;
    if (!source && typeof session.locSource === "function") {
      try { source = await session.locSource(); owned = true; }
      catch (error) {
        shadowError = String(error?.message ?? error);
        status(`world driver: shadow locs unavailable: ${shadowError}`);
        return;
      }
    }
    if (!source) return;
    try { locState = source.state?.() ?? null; } catch { locState = null; }
    const types = new Set();
    const models = new Set();
    try {
      const { base, regionIds } = world;
      for (const regionId of regionIds) {
        const region = await source.locs(regionId);
        if (!region?.data?.length) continue;
        session.addShadowLocs(regionId, region.data, base.x, base.y);
        stats.shadowRegions += 1;
        if (sceneOn) {
          try {
            stats.staticLocs += lend((w) => (typeof w.addStaticLocs === "function" ? w.addStaticLocs(regionId, region.data) : 0));
          } catch (error) { stats.sceneErrors += 1; status(`world driver: static locs failed: ${String(error?.message ?? error)}`); }
        }
        for (const id of region.typeIds ?? []) {
          if (types.has(id)) continue;
          types.add(id);
          const type = await source.locType(id);
          if (!type?.length) continue;
          session.addShadowLocType(id, type);
          stats.shadowTypes += 1;
          if (typeof source.locModels === "function") {
            for (const model of (await source.locModels(id, type)) ?? []) {
              if (models.has(model.id) || !model.data?.length) continue;
              models.add(model.id);
              session.addShadowLocModel(model.id, model.data);
              stats.shadowModels += 1;
            }
          } else {
            const model = await source.locModel?.(id);
            if (model?.length) { session.addShadowLocModel(id, model); stats.shadowModels += 1; }
          }
        }
      }
    } finally {
      if (owned) { try { source.close?.(); } catch {} }
    }
  };

  const buildLayers = async () => {
    const rgb = new runtime.HslRgbLookup(randomUnit);
    let report;
    try { report = session.lowerGround(rgb); }
    finally { rgb.free(); }
    if (!report || !Array.isArray(report.materials)) throw new Error("ground lowering report unavailable");
    const built = [];
    for (let i = 0; i < report.materials.length; i += 1) {
      const entry = report.materials[i];
      let rgba = new Uint8Array(0);
      let side = 0;
      if (entry.materialId >= 0) {
        side = textures.size(entry.materialId);
        rgba = await textures.render(entry.materialId);
      }
      const mesh = session.loweredMaterial(i);
      built.push({ materialId: entry.materialId, textureScale: entry.textureScale, vertices: mesh.vertices, indices: mesh.indices, side, rgba });
    }
    return built;
  };

  const bridgeFlags = (tiles) => {
    const flags = new Uint8Array(tiles * tiles);
    for (let x = 0; x < tiles; x += 1) {
      for (let y = 0; y < tiles; y += 1) {
        let tileInfo = null;
        try { tileInfo = JSON.parse(session.sceneTile(BRIDGE_PLANE, x, y)); } catch { tileInfo = null; }
        if (Number.isInteger(tileInfo?.renderFlags) && (tileInfo.renderFlags & BRIDGE_FLAG) !== 0) flags[x * tiles + y] = 1;
      }
    }
    return flags;
  };

  const feedGround = (tiles, grid) => {
    if (!orbitOn || typeof app.orbit_ground !== "function" || !grid) return;
    try {
      app.orbit_ground(tiles, plane, grid, bridgeFlags(tiles));
      groundLive = true;
    } catch (error) {
      status(`world driver: orbit ground unavailable: ${String(error?.message ?? error)}`);
    }
  };

  const startScene = async (world) => {
    const tiles = world.mapSize ?? 104;
    const grid = heightGrid((p, x, y) => session.sceneHeight(p, x, y), tiles, 4);
    lend((w) => w.sceneHeights(tiles, 4, grid));
    definitions = createDefinitionCache(lend);
    const rgb = new runtime.HslRgbLookup(randomUnit);
    try { rgbTable = rgb.rgbTable(); } finally { rgb.free(); }
    service = createSceneService({ lend, fetchArchive, relay, token, onStatus: status, onProvided: () => { sceneAt = -Infinity; dirty = true; } });
    input.enableMenu(canvas.width, canvas.height);
    try { picker = new runtime.Picker(tiles, grid, bridgeFlags(tiles)); } catch (error) { picker = null; status(`world driver: picking unavailable: ${String(error?.message ?? error)}`); }
    input.setMenuState(JSON.stringify(menuState(ownIndex)));
    menuReady = true;
    feedGround(tiles, grid);
    status("world driver: scene enabled");
  };

  const ensureTextures = async (ids) => {
    if (texturing) return;
    texturing = true;
    try {
      for (const id of ids) {
        if (registered.has(id) || app.terrain_scene_has_texture?.(id)) { registered.add(id); continue; }
        const side = textures.size(id);
        if (!(side > 0)) { registered.add(id); continue; }
        const rgba = await textures.render(id);
        app.terrain_scene_texture(id, side, side, rgba);
        registered.add(id);
        stats.sceneTextures += 1;
        dirty = true;
      }
    } catch (error) {
      stats.sceneErrors += 1;
      status(`world driver: scene texture failed: ${String(error?.message ?? error)}`);
    } finally { texturing = false; }
  };

  const dueCycles = (now) => {
    if (nextCycleAt === null) { nextCycleAt = now + CYCLE_MS; return 1; }
    if (now < nextCycleAt) return 0;
    let due = 0;
    while (now >= nextCycleAt && due < MAX_CYCLES_PER_FRAME) { nextCycleAt += CYCLE_MS; due += 1; }
    if (now - nextCycleAt > CYCLE_MS) nextCycleAt = now + CYCLE_MS;
    return due;
  };

  const refreshScene = (now) => {
    if (!sceneOn || !service || now - sceneAt < sceneIntervalMs) return;
    sceneAt = now;
    try {
      const owed = overlayDebt;
      overlayDebt = 0;
      const owedScene = sceneDebt;
      sceneDebt = 0;
      const fresh = lend((w) => {
        if (owed > 0 && typeof w.advanceOverlay === "function") w.advanceOverlay(owed);
        if (owedScene > 0 && typeof w.advanceScene === "function") w.advanceScene(owedScene);
        const summary = JSON.parse(w.sceneDraw());
        return {
          summary,
          meta: w.sceneDrawMeta(),
          positions: w.sceneDrawPositions(),
          colors: w.sceneDrawColors(),
          alphas: w.sceneDrawAlphas(),
          materials: w.sceneDrawMaterials(),
          npcs: w.sceneNpcs(),
          overlay: parseOverlay(w.overlay(), w.chatLines(100)),
        };
      });
      stats.sceneRefreshes += 1;
      sceneSummary = fresh.summary;
      sceneMeta = parseMeta(fresh.meta);
      sceneNpcs = parseNpcs(fresh.npcs);
      sceneOverlay = fresh.overlay;
      sceneData = fresh;
      stats.sceneMeshes = sceneMeta.length;
      stats.sceneTriangles = fresh.colors.length;
      dirty = true;
      const wanted = sceneMaterials(fresh.materials);
      if ([...wanted].some((id) => !registered.has(id))) ensureTextures(wanted);
      if (fresh.summary.pending > 0 || fresh.summary.incomplete) service.pump().catch(() => {});
      else if (hoverAt) service.pump().catch(() => {});
    } catch (error) {
      stats.sceneErrors += 1;
      status(`world driver: scene refresh failed: ${String(error?.message ?? error)}`);
    }
  };

  const viewRect = () => ui?.gameRect?.() ?? { x: 0, y: 0, width: canvas.width, height: canvas.height };

  const entityHitSet = () => {
    if (!mouseAt || orbitOn || !sceneOn || typeof app.terrain_live_view_projection !== "function") return null;
    const vr = viewRect();
    if (mouseAt.x < vr.x || mouseAt.y < vr.y || mouseAt.x >= vr.x + vr.width || mouseAt.y >= vr.y + vr.height) return null;
    try {
      const here = tile();
      const vp = app.terrain_live_view_projection(here.x, here.y, vr.width, vr.height, camera.tilt, camera.compass, 0);
      const flat = lend((w) => (typeof w.sceneEntityHits === "function"
        ? w.sceneEntityHits(vp, mouseAt.x - vr.x, mouseAt.y - vr.y, vr.width / 2, vr.height / 2, vr.width / 2, vr.height / 2)
        : null));
      if (!flat) return null;
      const hits = new Set();
      for (let at = 0; at + 1 < flat.length; at += 2) hits.add(`${flat[at]}:${flat[at + 1]}`);
      return hits;
    } catch (error) { stats.errors += 1; status(`world driver: entity hover failed: ${String(error?.message ?? error)}`); return null; }
  };

  const hintScreen = () => {
    if (!sceneOn || orbitOn || typeof app.terrain_live_view_projection !== "function") return { overhead: [], arrows: [] };
    try {
      const vr = viewRect();
      const here = tile();
      const vp = app.terrain_live_view_projection(here.x, here.y, vr.width, vr.height, camera.tilt, camera.compass, 0);
      const report = lend((w) => (typeof w.hintScreen === "function"
        ? JSON.parse(w.hintScreen(vp, vr.width / 2, vr.height / 2, vr.width / 2, vr.height / 2, 0, 0))
        : null));
      if (!report) return { overhead: [], arrows: [] };
      const boxes = [];
      for (const label of report.overhead) {
        const width = typeof measureText === "function" ? measureText(label.text) : label.text.length * 6;
        boxes.push(label.x, label.y, width >> 1);
      }
      const stacked = boxes.length && typeof runtime.stackOverheadLabels === "function" ? runtime.stackOverheadLabels(Int32Array.from(boxes), overheadPadding) : null;
      report.overhead.forEach((label, at) => {
        label.x += vr.x;
        label.y = (stacked ? stacked[at] : label.y) + vr.y;
      });
      for (const arrow of report.arrows) { arrow.x += vr.x; arrow.y += vr.y; }
      return report;
    } catch (error) { stats.errors += 1; status(`world driver: hint screen failed: ${String(error?.message ?? error)}`); return { overhead: [], arrows: [] }; }
  };

  const minimapPick = (view, x, y) => {
    const found = runtime.Picker.minimapPick(Int32Array.from(view), x, y);
    return found.length === 2 ? { x: found[0], y: found[1] } : null;
  };

  const feedHover = () => {
    if (!menuReady || !input) return;
    let at = hoverAt;
    const vr = viewRect();
    const inside = mouseAt && mouseAt.x >= vr.x && mouseAt.y >= vr.y && mouseAt.x < vr.x + vr.width && mouseAt.y < vr.y + vr.height;
    if (!at && picker && !orbitOn && inside && typeof app.terrain_live_view_projection === "function") {
      try {
        const here = tile();
        picker.setPlane(plane);
        const vp = app.terrain_live_view_projection(here.x, here.y, vr.width, vr.height, camera.tilt, camera.compass, 0);
        const hit = picker.tile(vp, mouseAt.x - vr.x, mouseAt.y - vr.y, vr.width, vr.height, 1);
        if (hit.length === 2) at = { x: hit[0], y: hit[1] };
      } catch (error) { stats.errors += 1; status(`world driver: pick failed: ${String(error?.message ?? error)}`); }
    }
    if (!at && typeof minimapView === "function" && runtime.Picker?.minimapPick) {
      try {
        const over = minimapView();
        if (over?.view && over.mouse) at = minimapPick(over.view, over.mouse.x, over.mouse.y);
      } catch (error) { stats.errors += 1; status(`world driver: minimap pick failed: ${String(error?.message ?? error)}`); }
    }
    for (const hook of hooks.hoverTile) {
      const value = hook();
      if (value && Number.isFinite(value.x) && Number.isFinite(value.y)) { at = { x: Math.floor(value.x), y: Math.floor(value.y) }; break; }
    }
    input.setMenuContext(false, worldBase.x, worldBase.y, ownIndex, -1, -1, -1);
    if (!at) {
      if (hoverKey !== "") { input.setHover("[]"); hoverKey = ""; }
      return;
    }
    const built = hoverTargets({ tile: at, base: worldBase, plane, meta: sceneMeta, npcs: sceneNpcs, ownIndex, definition: (kind, id) => definitions.get(kind, id), entityHits: entityHitSet() });
    const json = JSON.stringify(built.targets);
    const key = `${at.x},${at.y},${json}`;
    if (key !== hoverKey) {
      input.setHover(json);
      hoverKey = key;
      stats.hoverFeeds += 1;
    }
    if (built.pending.length && service) service.pump().catch(() => {});
  };

  const appendScene = () => {
    if (!sceneData || !sceneData.colors.length) return;
    const parts = partitionDraw(sceneMeta, sceneData);
    let count = 0;
    if (parts.main.colors.length) count += app.terrain_scene(parts.main.positions, parts.main.colors, parts.main.alphas, parts.main.materials, rgbTable);
    if (parts.overlay.colors.length && typeof app.terrain_scene_overlay === "function") {
      count += app.terrain_scene_overlay(parts.overlay.positions, parts.overlay.colors, parts.overlay.alphas, parts.overlay.materials, rgbTable);
    }
    stats.sceneLayers = count;
  };

  const start = async () => {
    if (started) throw new Error("world driver already started");
    started = true;
    try {
      const world = session.initialWorld();
      if (!world || !Array.isArray(world.regionIds) || !world.base) throw new Error("initial world unavailable");
      input = new runtime.ClientInput();
      ownIndex = world.playerIndex ?? -1;
      worldBase = { x: world.base.x, y: world.base.y };
      mapSize = world.mapSize ?? 104;
      previousInstalled = target.openrsClientInput;
      target.openrsClientInput = input;
      status("world driver: opening textures");
      textures = await openTextureSet(relay);
      status("world driver: preparing ground");
      session.prepareGround(plane);
      await feedShadows(world);
      shadowReport = session.bakeShadows(texturesEnabled);
      status("world driver: lowering ground");
      layers = await buildLayers();
      if (sceneOn) await startScene(world);
      else if (typeof session.sceneHeight === "function") feedGround(mapSize, heightGrid((p, x, y) => session.sceneHeight(p, x, y), mapSize, 4));
      if (orbitOn) app.orbit_reset(Math.floor(random() * 100) - 50, Math.floor(random() * 110) - 55, Math.floor(random() * 80) - 40, Math.floor(random() * 160) - 80);
      dirty = true;
      status(`world driver: ${layers.length} layers, ${shadowReport?.placements ?? 0} shadow placements`);
      return summary();
    } catch (error) {
      stop(String(error?.message ?? error));
      throw error;
    }
  };

  const tile = () => {
    for (const hook of hooks.playerTile) {
      const value = hook();
      if (value && Number.isFinite(value.x) && Number.isFinite(value.y)) return { x: Math.floor(value.x), y: Math.floor(value.y) };
    }
    if (sceneOn) {
      const own = localTile(sceneMeta, ownIndex, worldBase);
      if (own) return { x: Math.min(Math.max(own.x, 0), mapSize - 1), y: Math.min(Math.max(own.y, 0), mapSize - 1) };
    }
    return { x: mapSize >> 1, y: mapSize >> 1 };
  };

  const steer = () => {
    let moved = false;
    if (held.has("ArrowLeft")) { camera.compass = (camera.compass + TURN_STEP) % FULL_TURN; moved = true; }
    if (held.has("ArrowRight")) { camera.compass = (camera.compass - TURN_STEP + FULL_TURN) % FULL_TURN; moved = true; }
    if (held.has("ArrowUp") && camera.tilt > TILT_MIN) { camera.tilt = Math.max(TILT_MIN, camera.tilt - TURN_STEP); moved = true; }
    if (held.has("ArrowDown") && camera.tilt < TILT_MAX) { camera.tilt = Math.min(TILT_MAX, camera.tilt + TURN_STEP); moved = true; }
    if (moved) {
      input.setCamera(camera.tilt, camera.compass);
      dirty = true;
    }
  };

  const onKey = (down) => (event) => {
    if (typeof event?.code !== "string" || !event.code.startsWith("Arrow")) return;
    if (down) held.add(event.code);
    else held.delete(event.code);
  };
  const keyDown = onKey(true);
  const keyUp = onKey(false);
  target.addEventListener?.("keydown", keyDown, true);
  target.addEventListener?.("keyup", keyUp, true);
  const mouseDown = (event) => {
    if (!orbitOn || event?.button !== MIDDLE_BUTTON) return;
    dragging = true;
    dragX = 0;
    dragY = 0;
    event.preventDefault?.();
  };
  const mouseMove = (event) => {
    if (Number.isFinite(event?.clientX) && Number.isFinite(event?.clientY)) {
      const r = canvas.getBoundingClientRect();
      mouseAt = r.width > 0 && r.height > 0 ? { x: Math.round((event.clientX - r.left) * (canvas.width / r.width)), y: Math.round((event.clientY - r.top) * (canvas.height / r.height)) } : null;
    }
    if (!dragging) return;
    dragX += Number.isFinite(event?.movementX) ? event.movementX : 0;
    dragY += Number.isFinite(event?.movementY) ? event.movementY : 0;
  };
  const mouseUp = (event) => {
    if (event?.button !== MIDDLE_BUTTON) return;
    dragging = false;
    dragX = 0;
    dragY = 0;
  };
  target.addEventListener?.("mousedown", mouseDown, true);
  target.addEventListener?.("mousemove", mouseMove, true);
  target.addEventListener?.("mouseup", mouseUp, true);
  const dragDirection = (delta) => (delta > 0 ? 1 : delta < 0 ? -1 : 0);
  const clampDirection = (value) => Math.max(-1, Math.min(1, value));
  const wheel = (event) => {
    if (!orbitOn || !event?.ctrlKey || !Number.isFinite(event.deltaY) || event.deltaY === 0) return;
    app.orbit_zoom(event.deltaY > 0 ? 1 : -1);
    dirty = true;
  };
  target.addEventListener?.("wheel", wheel, true);

  const drainCamera = () => {
    if (!orbitOn || typeof session.takeCameraCommands !== "function" || typeof app.camera_apply !== "function") return;
    const commands = session.takeCameraCommands();
    if (commands.length === 0) return;
    const base = session.cameraSceneBase();
    if (base.length === 2) app.camera_base(base[0], base[1]);
    for (let i = 0; i + 6 <= commands.length; i += 6) {
      app.camera_apply(commands[i], commands[i + 1], commands[i + 2], commands[i + 3], commands[i + 4], commands[i + 5]);
    }
  };

  const frame = (now = clock()) => {
    if (!started || stopped || !input || !layers.length) return false;
    stats.frames += 1;
    const context = { now, camera, input, session, stats };
    try {
      for (const hook of hooks.beforeCycle) hook(context);
      steer();
      const due = dueCycles(now);
      for (let cycle = 0; cycle < due; cycle += 1) session.cycleInput(input, now, cycle === due - 1);
      stats.cycles += due;
      overlayDebt += due;
      sceneDebt += due;
      drainCamera();
      if (audio) audio.update({ events: session.takeAudioEvents?.() ?? [], tile: tile(), base: session.cameraSceneBase?.() ?? [], plane, roll: camera.compass, now });
      for (const hook of hooks.afterCycle) hook(context);
      for (const hook of hooks.invalidate) if (hook(context) === true) dirty = true;
      refreshScene(now);
      if (ui && ui.step(now) === true) dirty = true;
      feedHover();
      const at = tile();
      const { width, height } = viewRect();
      const key = `${at.x},${at.y},${width},${height},${camera.tilt},${camera.compass}`;
      if (!orbitOn && !dirty && key === lastKey) return false;
      app.terrain_begin();
      for (const layer of layers) app.terrain_layer(layer.materialId, layer.textureScale, layer.vertices, layer.indices, layer.side, layer.rgba);
      appendScene();
      for (const hook of hooks.layers) hook(app, { ...context, tile: at });
      if (orbitOn) {
        const elapsed = orbitAt === null ? 0 : Math.min(Math.max(now - orbitAt, 0), 200);
        orbitAt = now;
        orbitDebt += elapsed;
        const cycles = Math.floor(orbitDebt / ORBIT_CYCLE_MS);
        orbitDebt -= cycles * ORBIT_CYCLE_MS;
        const yawDirection = clampDirection(direction("ArrowLeft", "ArrowRight") - dragDirection(dragX));
        const pitchDirection = clampDirection(direction("ArrowDown", "ArrowUp") + dragDirection(dragY));
        dragX = 0;
        dragY = 0;
        app.terrain_show_orbit(at.x, at.y, width, height, elapsed, cycles, yawDirection, pitchDirection, randomBits(), env.enabled, env.color, env.depth, env.offset, env.sky);
      } else {
        app.terrain_show_live(at.x, at.y, width, height, camera.tilt, camera.compass, 0, env.enabled, env.color, env.depth, env.offset, env.sky);
      }
      lastKey = key;
      lastView = { tile: at, width, height, tilt: camera.tilt, compass: camera.compass, environment: { ...env } };
      dirty = false;
      stats.shown += 1;
      return true;
    } catch (error) {
      stats.errors += 1;
      stop(String(error?.message ?? error));
      return false;
    }
  };

  const summary = () => ({
    scope: "world-driver", started, stopped, error: lastError, layers: layers.length,
    scene: sceneOn ? { enabled: true, draw: sceneSummary, overlay: sceneOverlay, service: service?.stats() ?? null, definitions: definitions?.size() ?? 0, textures: registered.size, hover: hoverAt } : { enabled: false },
    camera: { ...camera }, view: lastView, shadows: shadowReport, shadowError, locState, stats: { ...stats },
    hooks: Object.fromEntries(HOOKS.map((name) => [name, hooks[name].length])),
  });

  return Object.freeze({
    start,
    frame,
    stop: () => stop("closed"),
    summary,
    invalidate: () => { dirty = true; },
    hintScreen,
    minimapPick,
    setHoverTile: (x, y) => {
      if (x === null || x === undefined) { hoverAt = null; return; }
      if (!Number.isSafeInteger(x) || !Number.isSafeInteger(y)) throw new Error("integer scene tile required");
      hoverAt = { x, y };
    },
    on: (name, fn) => {
      if (!HOOKS.includes(name) || typeof fn !== "function") throw new Error("known driver hook and function required");
      hooks[name].push(fn);
      dirty = true;
      return () => { const at = hooks[name].indexOf(fn); if (at >= 0) hooks[name].splice(at, 1); };
    },
    setCamera: (tilt, compass) => {
      if (!unsigned(tilt, FULL_TURN - 1) || !unsigned(compass, FULL_TURN - 1)) throw new Error("14-bit camera angles required");
      camera.tilt = tilt;
      camera.compass = compass;
      input?.setCamera(tilt, compass);
      dirty = true;
    },
  });
}
