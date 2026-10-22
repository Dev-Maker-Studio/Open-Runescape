import { afterFrame, connect, disconnect, setup, stats as streamStats } from "./stream.js";
import { assetProbe, coreScriptProbe, cs2RunProbe, loadRuntime, gameHandshakeProbe, protocolProbe, scriptGraphProbe } from "./js5-probe.js";
import { openLobbySession, takePrivateLobbyConfig } from "./lobby-session.js";
import { openOriginalSession } from "./original-session.js";
import { observeBrowserLoginOptions } from "./browser-login-profile.js";
import { PUBLIC_LOGIN_DEFAULTS, browserLoginProvenance } from "./login-config.js";
import { openWorldTerrain } from "./world-terrain.js";
import { openWorldFloors } from "./world-floors.js";
import { openWorldMaterials } from "./world-materials.js";
import { openTextureSet } from "./world-textures.js";
import { createWorldDriver } from "./world-driver.js";
import { createUiDriver } from "./ui-driver.js";
import { createWorldAudio } from "./audio.js";

const lines = [];
const st = { app: null, canvas: null, backend: null, gpu: null, frames: 0, last: 0, frameMs: 0, cpuMs: 0, waiters: [], error: null };

function keep(level, args) {
  lines.push(`${level} ${args.map((a) => (typeof a === "string" ? a : String(a?.stack ?? a))).join(" ")}`);
  if (lines.length > 200) lines.shift();
}

for (const level of ["log", "info", "warn", "error", "debug"]) {
  const base = console[level].bind(console);
  console[level] = (...args) => {
    keep(level, args);
    base(...args);
  };
}

export function fail(e) {
  st.error = String(e?.stack ?? e);
  keep("error", [st.error]);
}

addEventListener("error", (e) => fail(e.error ?? e.message));
addEventListener("unhandledrejection", (e) => fail(e.reason));

const input = { counts: {}, last: [] };

function forwardInput(type, e, x, y) {
  const target = globalThis.openrsClientInput;
  if (!target) return;
  const now = Math.round(performance.timeOrigin + performance.now());
  const button = Math.min(Math.max(e.button ?? 0, 0), 2);
  if (type === "mousemove") target.mouse(-1, x, y, now, 0);
  else if (type === "mousedown") target.mouse(button, x, y, now, 0);
  else if (type === "mouseup") target.mouse(3 + button, x, y, now, 0);
  else if (type === "wheel") target.mouse(6, x, y, now, Math.sign(e.deltaY));
  else if (type === "keydown") {
    target.keyDown(e.code, now);
    if (e.key.length === 1) target.keyTyped(e.key.charCodeAt(0), now);
  } else if (type === "keyup") target.keyUp(e.code, now);
  if (type === "contextmenu" || (type === "keydown" && e.code.startsWith("Arrow"))) e.preventDefault();
}

for (const type of ["keydown", "keyup", "keypress", "mousedown", "mouseup", "mousemove", "click", "dblclick", "contextmenu", "wheel"]) {
  addEventListener(type, (e) => {
    const r = st.canvas?.getBoundingClientRect();
    const k = (v, o) => (r ? Math.round((v - o) * (st.canvas.width / r.width)) : v);
    input.counts[type] = (input.counts[type] ?? 0) + 1;
    input.last.push(type.startsWith("key") ? { type, key: e.key, code: e.code, frame: st.frames } : { type, x: k(e.clientX, r?.left), y: k(e.clientY, r?.top), button: e.button, dx: e.deltaX, dy: e.deltaY, frame: st.frames });
    if (input.last.length > 16) input.last.shift();
    forwardInput(type, e, k(e.clientX, r?.left), k(e.clientY, r?.top));
  }, true);
}

function need() {
  if (!st.app) throw new Error(st.error ? `renderer failed: ${st.error}` : "renderer not ready");
  return st.app;
}

function info() {
  return st.app ? JSON.parse(st.app.info()) : {};
}

function tick() {
  st.frames++;
  st.waiters = st.waiters.filter((w) => (st.frames >= w.at ? (w.done(), false) : true));
}

const interval = 1000 / (Number(new URLSearchParams(location.search).get("fps")) || 60) - 0.5;

function loop(t) {
  st.rafs = (st.rafs || 0) + 1;
  if (st.app && !st.error && !st.busy && t - st.last >= interval) {
    const c = performance.now();
    try {
      st.worldDriver?.frame();
      st.app.frame();
      tick();
      afterFrame();
    } catch (e) {
      fail(e);
    }
    st.cpuMs += (performance.now() - c - st.cpuMs) * 0.1;
    if (st.last) st.frameMs += (t - st.last - st.frameMs) * 0.1;
    st.last = st.last ? Math.max(st.last + interval, t - interval) : t;
  }
  requestAnimationFrame(loop);
}

function unsignedArgument(value, max, error = "bounded terrain coordinate required") {
  if (typeof value !== "string" || !/^(0|[1-9]\d{0,4})$/.test(value) || Number(value) > max) throw new Error(error);
  return Number(value);
}

function js5RelayArgument(relay) {
  try {
    if (typeof relay !== "string" || relay.length > 512 || !/^wss?:\/\/(?:127\.0\.0\.1|localhost|\[::1\])(?::[0-9]{1,5})?\/js5$/.test(relay)) throw new Error();
    const url = new URL(relay);
    if (!["ws:", "wss:"].includes(url.protocol) || !["127.0.0.1", "localhost", "[::1]"].includes(url.hostname) || url.pathname !== "/js5" || url.username || url.password || url.search || url.hash || url.port === "0") throw new Error();
    return url.href;
  } catch { throw new Error("exact loopback JS5 relay required"); }
}

function closeFloorTerrain(owner) {
  if (!owner || st.terrainFloors !== owner) return;
  let failed = false;
  try { st.terrainAbort?.abort(); } catch { failed = true; }
  try { st.terrain?.close(); } catch { failed = true; }
  if (failed) throw new Error("surface terrain probe unavailable");
}

function floorDefinition(owner, method, id, label) {
  const value = unsignedArgument(id, 65535, "bounded floor definition ID required");
  try {
    if (!owner || typeof owner[method] !== "function") throw new Error();
    return owner[method](value);
  } catch { throw new Error(label); }
}

function materialState(owner) {
  try { return owner?.state() ?? null; }
  catch { throw new Error("material metadata probe unavailable"); }
}

function materialMetadata(owner, id, label) {
  const value = unsignedArgument(id, 65535, "bounded material ID required");
  try {
    const method = owner?.material;
    if (typeof method !== "function") throw new Error();
    return method.call(owner, value);
  } catch { throw new Error(label); }
}

function paletteColor(owner, plane, x, y, label) {
  const coordinates = [unsignedArgument(plane, 3), unsignedArgument(x, 167), unsignedArgument(y, 167)];
  try {
    if (!owner || typeof owner.paletteColor !== "function") throw new Error();
    return { packedHsl: owner.paletteColor(...coordinates), rgbPrepared: false, renderedWorld: false };
  } catch { throw new Error(label); }
}

function sceneTile(owner, plane, x, y, label) {
  const coordinates = [unsignedArgument(plane, 3), unsignedArgument(x, 167), unsignedArgument(y, 167)];
  try {
    if (!owner || typeof owner.sceneTile !== "function") throw new Error();
    return { tile: owner.sceneTile(...coordinates), effectsDecoded: false, renderedWorld: false };
  } catch { throw new Error(label); }
}

function groundArguments(args) {
  if (args.length > 2) throw new Error("bounded ground plane and vertex budget required");
  const parse = (value, max) => {
    if (typeof value !== "string" || !/^(0|[1-9]\d{0,6})$/.test(value) || Number(value) > max) throw new Error("bounded ground plane and vertex budget required");
    return Number(value);
  };
  return [args.length ? parse(args[0], 3) : 0, args.length > 1 ? parse(args[1], 1016064) : 1016064];
}

function groundState(owner, args = [], label = "surface ground unavailable") {
  if (args.length) throw new Error("ground state takes no arguments");
  try {
    if (!owner) return null;
    const method = owner.groundState;
    if (typeof method !== "function") throw new Error();
    return method.call(owner);
  } catch { throw new Error(label); }
}

function groundTile(owner, args, label) {
  if (args.length !== 3) throw new Error("bounded ground coordinates required");
  const coordinates = args.map((value, at) => unsignedArgument(value, at ? 167 : 3, "bounded ground coordinates required"));
  try {
    const method = owner?.groundTile;
    if (typeof method !== "function") throw new Error();
    return { tile: method.call(owner, ...coordinates), terrainLowered: false, texturePixelsDecoded: false, renderedWorld: false };
  } catch { throw new Error(label); }
}

function timeout(args) {
  const t = args.find((a) => a.startsWith("timeout="));
  return t ? Number(t.slice(8)) : 30000;
}

function waitFrames(n, ms = 30000) {
  need();
  const start = performance.now();
  return new Promise((resolve, reject) => {
    const w = { at: st.frames + n, done: () => { clearTimeout(timer); resolve({ frames: st.frames, ms: Math.round(performance.now() - start) }); } };
    const timer = setTimeout(() => {
      st.waiters = st.waiters.filter((x) => x !== w);
      reject(new Error("wait timed out"));
    }, ms);
    st.waiters.push(w);
  });
}

function list(m) {
  return Object.entries(m).map(([k, v]) => `${k}=${v}`).join(",");
}

function summary(s = info().summary) {
  if (!s) return "no frame loaded";
  const sum = (m) => Object.values(m).reduce((a, b) => a + b, 0);
  return `replay: ${s.draws} draws, drawn ${sum(s.drawn)} [${list(s.drawn)}], skipped ${sum(s.skipped)} [${list(s.skipped)}]`;
}

function size(w, h) {
  const c = st.canvas;
  const ratio = devicePixelRatio || 1;
  c.dataset.fixed = "1";
  c.width = w;
  c.height = h;
  c.style.width = `${w / ratio}px`;
  c.style.height = `${h / ratio}px`;
}

async function base64(blob) {
  const url = await new Promise((resolve, reject) => {
    const r = new FileReader();
    r.onload = () => resolve(r.result);
    r.onerror = () => reject(r.error);
    r.readAsDataURL(blob);
  });
  return url.slice(url.indexOf(",") + 1);
}

function state() {
  const i = info();
  const c = st.canvas;
  return {
    ready: !!st.app && !st.error,
    backend: st.backend,
    wgpu: i.backend ?? null,
    adapter: i.adapter ?? null,
    gpu: st.gpu,
    format: i.format ?? null,
    canvas: c && { width: c.width, height: c.height, cssWidth: c.clientWidth, cssHeight: c.clientHeight, dpr: devicePixelRatio },
    frames: st.frames,
    rafs: st.rafs || 0,
    loaded: i.loaded ?? null,
    frameSize: i.frameSize ?? null,
    summary: i.summary ?? null,
    frameMs: Math.round(st.frameMs * 100) / 100,
    cpuMs: Math.round(st.cpuMs * 1000) / 1000,
    lastError: st.error,
    gpuError: i.gpuError || null,
    stream: streamStats(),
    protocol: st.protocol ?? null,
    gameHandshake: st.gameHandshake ?? null,
    standaloneLobby: st.lobby?.state() ?? null,
    standaloneSession: st.original?.state() ?? null,
    surfaceTerrain: st.terrain?.state() ?? null,
    floorDefinitions: st.floors?.state() ?? null,
    materialDefinitions: materialState(st.materials),
    asset: st.asset ?? null,
    scriptGraph: st.scriptGraph ?? null,
    input: { counts: input.counts, last: input.last.slice(-8) },
    url: location.href,
  };
}

function describe() {
  const s = state();
  const g = s.gpu ?? {};
  return [
    `frame ${s.frames} ${s.loaded ?? "nothing"} ${s.canvas?.width}x${s.canvas?.height} ${s.backend ?? "no gpu"} ${s.wgpu ?? ""} ${s.ready ? "ready" : "not ready"}`,
    `adapter ${s.adapter?.name || "unknown"}, gpu ${g.vendor ?? "?"} ${g.architecture ?? "?"}, fallback ${g.fallback ?? "?"}`,
    summary(s.summary),
    `timing ${s.frameMs} ms per frame, ${s.cpuMs} ms cpu`,
    `stream ${s.stream.game ? `${s.stream.game} ${s.stream.state} host frame ${s.stream.hostFrame} shown ${s.stream.shown} received ${s.stream.received} dropped ${s.stream.dropped} host dropped ${s.stream.hostDropped} rtt ${s.stream.rttMs ?? "?"} ms` : "none"}`,
    `error ${s.lastError ?? s.gpuError ?? "none"}`,
    `console ${lines.length} lines${lines.length ? `, last: ${lines[lines.length - 1].slice(0, 200)}` : ""}`,
  ].join("\n");
}

const commands = {
  state,
  describe,
  tick: () => st.frames,
  frame: () => st.frames,
  "wait-frames": (n = "1", ...rest) => waitFrames(Number(n) || 1, timeout(rest)),
  game: async (name, host) => {
    if (!name) throw new Error("usage: game <name> [host:port]");
    connect(name, host);
    const start = performance.now();
    while (!(streamStats().received > 0) && performance.now() - start < 10000) await waitFrames(1);
    if (!(streamStats().received > 0)) throw new Error(`no frame from ${streamStats().host}: ${streamStats().error ?? "timeout"}`);
    return streamStats();
  },
  stream: () => streamStats(),
  "standalone-profile": async () => {
    const options = await observeBrowserLoginOptions({ worldId: 22, fontIndex: 32 });
    return { provenance: browserLoginProvenance(), viewport: options.viewport, operatingSystem: options.machineInfo.operatingSystem, maxMemory: options.machineInfo.maxMemory, processors: options.machineInfo.processors, preferenceBytes: options.preferences.length, fontIndex: options.fontIndex, worldId: options.worldId };
  },
  "standalone-session-state": () => st.original?.state() ?? st.originalAttempt ?? null,
  "standalone-varp": (id) => {
    if (!st.original?.varp) throw new Error("independent variable state unavailable");
    if (typeof id !== "string" || !/^(0|[1-9]\d{0,4})$/.test(id) || Number(id) > 65535) throw new Error("bounded variable ID required");
    return { id: Number(id), value: st.original.varp(Number(id)) };
  },
  "world-stats": (...args) => {
    if (args.length) throw new Error("world stats takes no arguments");
    try {
      const method = st.original?.playerStats;
      if (typeof method !== "function") throw new Error();
      return method.call(st.original);
    } catch { throw new Error("independent player stats unavailable"); }
  },
  "world-skill": (...args) => {
    if (args.length !== 1) throw new Error("bounded skill ID required");
    const id = unsignedArgument(args[0], 24, "bounded skill ID required");
    try {
      const method = st.original?.skill;
      if (typeof method !== "function") throw new Error();
      return method.call(st.original, id);
    } catch { throw new Error("independent player skill unavailable"); }
  },
  "standalone-terrain-height": (region, plane, x, y) => {
    if (!st.original?.terrainHeight) throw new Error("independent surface terrain unavailable");
    return { height: st.original.terrainHeight(unsignedArgument(region, 65535), unsignedArgument(plane, 3), unsignedArgument(x, 63), unsignedArgument(y, 63)) };
  },
  "standalone-scene-height": (plane, x, y) => {
    if (!st.original?.sceneHeight) throw new Error("independent surface terrain unavailable");
    return { height: st.original.sceneHeight(unsignedArgument(plane, 3), unsignedArgument(x, 168), unsignedArgument(y, 168)) };
  },
  "standalone-scene-tile": (plane, x, y) => sceneTile(st.original, plane, x, y, "independent surface terrain tile unavailable"),
  "standalone-underlay": (id) => floorDefinition(st.original, "underlay", id, "independent floor definitions unavailable"),
  "standalone-overlay": (id) => floorDefinition(st.original, "overlay", id, "independent floor definitions unavailable"),
  "standalone-material": (id) => materialMetadata(st.original, id, "independent material metadata unavailable"),
  "standalone-palette-color": (plane, x, y) => paletteColor(st.original, plane, x, y, "independent surface palette unavailable"),
  "standalone-ground-prepare": (...args) => {
    const values = groundArguments(args);
    try {
      const method = st.original?.prepareGround;
      if (typeof method !== "function") throw new Error();
      return method.call(st.original, ...values);
    } catch { throw new Error("independent surface ground unavailable"); }
  },
  "standalone-ground-state": (...args) => groundState(st.original, args, "independent surface ground unavailable"),
  "standalone-ground-tile": (...args) => groundTile(st.original, args, "independent surface ground tile unavailable"),
  "floors-probe": async (relay) => {
    if (st.floorsBusy) throw new Error("floor definitions probe already running");
    const js5Relay = js5RelayArgument(relay);
    st.floorsBusy = true;
    const abort = new AbortController();
    st.floorsAbort = abort;
    let owner;
    let discarded = false;
    let cancel;
    const discard = (value) => {
      if (!value || discarded) return;
      discarded = true;
      try { value.close(); } catch {}
    };
    try {
      let failed = false;
      try { closeFloorTerrain(st.floors); } catch { failed = true; }
      try { st.floors?.close(); } catch { failed = true; }
      if (failed) throw new Error("floor definitions probe unavailable");
      st.floors = null;
      st.floorsRelay = null;
      const cancelled = new Promise((_, reject) => {
        cancel = () => reject(new Error("floor definitions probe cancelled"));
        abort.signal.addEventListener("abort", cancel, { once: true });
        if (abort.signal.aborted) cancel();
      });
      const loading = Promise.resolve().then(() => {
        if (abort.signal.aborted) throw new Error("floor definitions probe cancelled");
        return openWorldFloors({ js5Relay, signal: abort.signal });
      });
      loading.then((late) => { if (abort.signal.aborted) discard(late); }, () => {});
      owner = await Promise.race([loading, cancelled]);
      if (abort.signal.aborted) throw new Error("floor definitions probe cancelled");
      const report = owner.state();
      if (abort.signal.aborted) throw new Error("floor definitions probe cancelled");
      st.floors = owner;
      st.floorsRelay = js5Relay;
      return report;
    } catch {
      discard(owner);
      throw new Error(abort.signal.aborted ? "floor definitions probe cancelled" : "floor definitions probe unavailable");
    } finally {
      abort.signal.removeEventListener("abort", cancel);
      if (st.floorsAbort === abort) st.floorsAbort = null;
      st.floorsBusy = false;
    }
  },
  "floors-state": () => {
    try { return st.floors?.state() ?? null; }
    catch { throw new Error("floor definitions probe unavailable"); }
  },
  "floor-underlay": (id) => floorDefinition(st.floors, "underlay", id, "floor definitions probe unavailable"),
  "floor-overlay": (id) => floorDefinition(st.floors, "overlay", id, "floor definitions probe unavailable"),
  "floors-close": () => {
    let failed = false;
    try { st.floorsAbort?.abort(); } catch { failed = true; }
    try { closeFloorTerrain(st.floors); } catch { failed = true; }
    try { st.floors?.close(); } catch { failed = true; }
    if (failed) throw new Error("floor definitions probe unavailable");
    try { return st.floors?.state() ?? null; }
    catch { throw new Error("floor definitions probe unavailable"); }
  },
  "materials-probe": async (relay) => {
    if (st.materialsBusy) throw new Error("material metadata probe already running");
    const js5Relay = js5RelayArgument(relay);
    st.materialsBusy = true;
    const abort = new AbortController();
    st.materialsAbort = abort;
    let owner;
    let discarded = false;
    let cancel;
    const discard = (value) => {
      if (!value || discarded) return;
      discarded = true;
      try { value.close(); } catch {}
    };
    try {
      try { st.materials?.close(); }
      catch { throw new Error("material metadata probe unavailable"); }
      st.materials = null;
      st.materialsRelay = null;
      const cancelled = new Promise((_, reject) => {
        cancel = () => reject(new Error("material metadata probe cancelled"));
        abort.signal.addEventListener("abort", cancel, { once: true });
        if (abort.signal.aborted) cancel();
      });
      const loading = Promise.resolve().then(() => {
        if (abort.signal.aborted) throw new Error("material metadata probe cancelled");
        return openWorldMaterials({ js5Relay, signal: abort.signal });
      });
      loading.then((late) => { if (abort.signal.aborted) discard(late); }, () => {});
      owner = await Promise.race([loading, cancelled]);
      if (abort.signal.aborted) throw new Error("material metadata probe cancelled");
      const report = owner.state();
      if (abort.signal.aborted) throw new Error("material metadata probe cancelled");
      st.materials = owner;
      st.materialsRelay = js5Relay;
      return report;
    } catch {
      discard(owner);
      throw new Error(abort.signal.aborted ? "material metadata probe cancelled" : "material metadata probe unavailable");
    } finally {
      abort.signal.removeEventListener("abort", cancel);
      if (st.materialsAbort === abort) st.materialsAbort = null;
      st.materialsBusy = false;
    }
  },
  "materials-state": () => materialState(st.materials),
  material: (id) => materialMetadata(st.materials, id, "material metadata probe unavailable"),
  "materials-close": () => {
    let failed = false;
    try { st.materialsAbort?.abort(); } catch { failed = true; }
    try { st.materials?.close(); } catch { failed = true; }
    if (failed) throw new Error("material metadata probe unavailable");
    try { return st.materials?.state() ?? null; }
    catch { throw new Error("material metadata probe unavailable"); }
  },
  "terrain-probe": async (relay, baseX, baseY, mapSize, regionIds, composeFloors) => {
    if (st.terrainBusy) throw new Error("surface terrain probe already running");
    const js5Relay = js5RelayArgument(relay);
    if (composeFloors !== undefined && composeFloors !== "floors") throw new Error("optional terrain palette argument must be floors");
    let floors;
    if (composeFloors === "floors") {
      try {
        const state = st.floors?.state();
        if (!state || state.accepted !== true || state.closed !== false || st.floorsRelay !== js5Relay) throw new Error();
        floors = st.floors;
      } catch { throw new Error("retained floor definitions from the same JS5 relay required"); }
    }
    if (typeof regionIds !== "string" || regionIds.length > 215) throw new Error("bounded terrain regions required");
    const regions = regionIds.split(",").map((id) => unsignedArgument(id, 65535));
    if (regions.length > 36) throw new Error("bounded terrain regions required");
    const initialWorld = { mapKind: "standard", base: { x: unsignedArgument(baseX, 16384), y: unsignedArgument(baseY, 16384) }, mapSize: unsignedArgument(mapSize, 168), regionIds: regions };
    st.terrainBusy = true;
    const abort = new AbortController();
    st.terrainAbort = abort;
    st.terrainFloors = floors ?? null;
    let owner;
    let discarded = false;
    let cancel;
    const discard = (value) => {
      if (!value || discarded) return;
      discarded = true;
      try { value.close(); } catch {}
    };
    try {
      st.terrain?.close();
      st.terrain = null;
      st.terrainRelay = null;
      const cancelled = new Promise((_, reject) => {
        cancel = () => reject(new Error("surface terrain probe cancelled"));
        abort.signal.addEventListener("abort", cancel, { once: true });
        if (abort.signal.aborted) cancel();
      });
      const loading = Promise.resolve().then(() => {
        if (abort.signal.aborted) throw new Error("surface terrain probe cancelled");
        return openWorldTerrain({ js5Relay, initialWorld, ...(floors ? { floors } : {}), signal: abort.signal });
      });
      loading.then((late) => { if (abort.signal.aborted) discard(late); }, () => {});
      owner = await Promise.race([loading, cancelled]);
      if (abort.signal.aborted) throw new Error("surface terrain probe cancelled");
      const report = owner.state();
      if (abort.signal.aborted) throw new Error("surface terrain probe cancelled");
      st.terrain = owner;
      st.terrainRelay = js5Relay;
      return report;
    } catch {
      discard(owner);
      throw new Error(abort.signal.aborted ? "surface terrain probe cancelled" : "surface terrain probe unavailable");
    } finally {
      abort.signal.removeEventListener("abort", cancel);
      if (st.terrainAbort === abort) st.terrainAbort = null;
      st.terrainBusy = false;
    }
  },
  "terrain-height": (region, plane, x, y) => {
    if (!st.terrain) throw new Error("surface terrain probe unavailable");
    return { height: st.terrain.height(unsignedArgument(region, 65535), unsignedArgument(plane, 3), unsignedArgument(x, 63), unsignedArgument(y, 63)) };
  },
  "surface-height": (plane, x, y) => {
    if (!st.terrain) throw new Error("surface terrain probe unavailable");
    return { height: st.terrain.sceneHeight(unsignedArgument(plane, 3), unsignedArgument(x, 168), unsignedArgument(y, 168)) };
  },
  "surface-tile": (plane, x, y) => sceneTile(st.terrain, plane, x, y, "surface terrain tile unavailable"),
  "surface-palette-color": (plane, x, y) => paletteColor(st.terrain, plane, x, y, "surface terrain palette unavailable"),
  "ground-prepare": (...args) => {
    const values = groundArguments(args);
    const terrain = st.terrain;
    const floors = st.floors;
    const materials = st.materials;
    try {
      const live = () => {
        if (!terrain || !floors || !materials || st.terrain !== terrain || st.floors !== floors || st.materials !== materials || st.terrainFloors !== floors || !st.terrainRelay || st.terrainRelay !== st.floorsRelay || st.terrainRelay !== st.materialsRelay) throw new Error();
        for (const owner of [terrain, floors, materials]) {
          const report = owner.state();
          if (report?.accepted !== true || report.closed !== false) throw new Error();
        }
        if (st.terrain !== terrain || st.floors !== floors || st.materials !== materials) throw new Error();
      };
      live();
      const method = terrain.prepareGround;
      live();
      if (typeof method !== "function") throw new Error();
      const report = method.call(terrain, floors, materials, ...values);
      live();
      return report;
    } catch { throw new Error("surface ground unavailable"); }
  },
  "texture-probe": async (relay, materialIds) => {
    const js5Relay = js5RelayArgument(relay);
    if (typeof materialIds !== "string" || materialIds.length > 400 || !/^\d{1,5}(,\d{1,5})*$/.test(materialIds)) throw new Error("bounded material IDs required");
    const ids = materialIds.split(",").map(Number);
    const set = await openTextureSet(js5Relay);
    try {
      const results = await set.digests(ids);
      return { scope: "browser-texture-pixels", slots: set.slots, fetched: set.fetched, materials: results, renderedWorld: false };
    } finally {
      set.close();
    }
  },
  "underwater-probe": async (relay, baseX, baseY, mapSize, regionIds) => {
    const js5Relay = js5RelayArgument(relay);
    if (typeof regionIds !== "string" || regionIds.length > 215) throw new Error("bounded terrain regions required");
    const regions = regionIds.split(",").map((id) => unsignedArgument(id, 65535));
    if (regions.length > 36) throw new Error("bounded terrain regions required");
    const initialWorld = { mapKind: "standard", base: { x: unsignedArgument(baseX, 16384), y: unsignedArgument(baseY, 16384) }, mapSize: unsignedArgument(mapSize, 168), regionIds: regions };
    try { st.underwater?.close(); } catch {}
    st.underwater = null;
    try {
      const owner = await openWorldTerrain({ js5Relay, initialWorld, underwater: true });
      st.underwater = owner;
      return owner.state();
    } catch { throw new Error("underwater terrain probe unavailable"); }
  },
  "ground-lower": async (randomUnit, lit) => {
    const terrain = st.terrain;
    const materials = st.materials;
    if (!terrain || !materials || typeof terrain.lowerGround !== "function") throw new Error("surface ground lowering unavailable");
    if (lit !== undefined && lit !== "lit") throw new Error("optional lowering argument must be lit");
    if (lit === "lit" && !st.underwater) throw new Error("retained underwater terrain required for lit lowering");
    const unit = Number(randomUnit);
    if (!Number.isFinite(unit) || unit < 0 || unit >= 1) throw new Error("random unit in [0, 1) required");
    const { HslRgbLookup } = await loadRuntime();
    const rgb = new HslRgbLookup(unit);
    try {
      const lowered = lit === "lit" ? terrain.lowerGround(materials, rgb, st.underwater) : terrain.lowerGround(materials, rgb);
      st.loweredReport = lowered;
      return lowered;
    } finally {
      rgb.free();
    }
  },
  "terrain-scene": async (relay, tileX = "51", tileZ = "55", width = "1280", height = "720") => {
    const js5Relay = js5RelayArgument(relay);
    const terrain = st.terrain;
    if (!terrain || typeof terrain.loweredMaterial !== "function") throw new Error("ground not lowered");
    const app = need();
    const report = st.loweredReport;
    if (!report || !Array.isArray(report.materials)) throw new Error("ground lowering report unavailable");
    disconnect();
    const set = await openTextureSet(js5Relay);
    try {
      app.terrain_begin();
      let textured = 0;
      for (let i = 0; i < report.materials.length; i += 1) {
        const entry = report.materials[i];
        let rgba = new Uint8Array(0);
        let side = 0;
        if (entry.materialId >= 0) {
          side = set.size(entry.materialId);
          rgba = await set.render(entry.materialId);
          textured += 1;
        }
        const mesh = terrain.loweredMaterial(i);
        app.terrain_layer(entry.materialId, entry.textureScale, mesh.vertices, mesh.indices, side, rgba);
      }
      const summary = app.terrain_show(Number(tileX), Number(tileZ), Number(width), Number(height));
      size(Number(width), Number(height));
      await waitFrames(2);
      return { scope: "terrain-scene", layers: report.materials.length, texturedLayers: textured, summary, renderedWorld: false };
    } finally {
      set.close();
    }
  },
  "ground-state": (...args) => groundState(st.terrain, args),
  "ground-tile": (...args) => groundTile(st.terrain, args, "surface ground tile unavailable"),
  "terrain-close": () => {
    try { st.underwater?.close(); st.underwater = null; } catch {}
    try { st.terrainAbort?.abort(); st.terrain?.close(); return st.terrain?.state() ?? null; }
    catch { throw new Error("surface terrain probe unavailable"); }
  },
  "standalone-session-close": () => { st.worldDriver?.stop(); st.worldDriver = null; st.originalAbort?.abort(); st.original?.close(); return st.original?.state() ?? null; },
  "world-drive": async (relay = "ws://127.0.0.1:8111/js5", plane = "0") => {
    if (!st.original) throw new Error("independent session required");
    if (st.worldDriver && !st.worldDriver.summary().stopped) throw new Error("world driver already running");
    const app = need();
    disconnect();
    const runtime = await loadRuntime();
    const ui = typeof runtime.InterfaceUi === "function" && typeof app.set_ui_overlay === "function" ? createUiDriver({ runtime, app, canvas: st.canvas, session: st.original, fetchArchive: assetProbe, relay: js5RelayArgument(relay), token: PUBLIC_LOGIN_DEFAULTS.js5AuthToken, onStatus: (text) => keep("info", [text]) }) : null;
    const driver = createWorldDriver({ app, canvas: st.canvas, session: st.original, runtime, openTextureSet, relay: js5RelayArgument(relay), plane: unsignedArgument(plane, 3), onStatus: (text) => keep("info", [text]), fetchArchive: assetProbe, ui, token: PUBLIC_LOGIN_DEFAULTS.js5AuthToken, audio: createWorldAudio({ fetchArchive: assetProbe, relay: js5RelayArgument(relay), token: PUBLIC_LOGIN_DEFAULTS.js5AuthToken, onStatus: (text) => keep("info", [text]) }) });
    st.worldDriver = driver;
    return await driver.start();
  },
  "world-drive-state": () => st.worldDriver?.summary() ?? null,
  "world-drive-stop": () => { const summary = st.worldDriver?.summary() ?? null; st.worldDriver?.stop(); return summary; },
  "standalone-login": async (lobbyRelay = "ws://127.0.0.1:8110/lobby", worldRelay = "ws://127.0.0.1:8110/world", worldJs5Relay = "ws://127.0.0.1:8111/js5", js5Relay = "ws://127.0.0.1:8110/js5") => {
    if (st.originalBusy || st.lobbyBusy || st.lobby?.state().connected || st.original?.state().world.connected) throw new Error("independent session already active");
    st.originalBusy = true;
    const abort = new AbortController();
    st.originalAbort = abort;
    try {
      st.original?.close();
      st.original = null;
      st.originalAttempt = null;
      st.original = await openOriginalSession({ scene: true, lobbyRelay, worldRelay, worldJs5Relay, js5Relay, worldId: 22, fontIndex: 32, withUnderwater: true, signal: abort.signal, onState: (state) => { st.originalAttempt = state; } });
      return st.original.state();
    } finally { st.originalBusy = false; st.originalAbort = null; }
  },
  "standalone-lobby-state": () => st.lobby?.state() ?? null,
  "standalone-lobby-close": () => { st.lobby?.close(); return st.lobby?.state() ?? null; },
  "standalone-lobby-tick": () => {
    if (!st.lobby) throw new Error("no independent lobby session");
    return st.lobby.tick();
  },
  "standalone-lobby-login": async (relay = "ws://127.0.0.1:8110/lobby") => {
    if (st.originalBusy || st.lobbyBusy || st.lobby?.state().connected || st.original?.state().world.connected) throw new Error("independent lobby session already active");
    st.lobbyBusy = true;
    try {
      st.lobby?.close();
      st.lobby = await openLobbySession(relay, await takePrivateLobbyConfig());
      await st.lobby.ready;
      return st.lobby.state();
    } finally { st.lobbyBusy = false; }
  },
  "game-handshake-probe": async (relay) => {
    if (st.gameHandshakeBusy) throw new Error("game handshake probe already running");
    st.gameHandshakeBusy = true;
    st.gameHandshake = null;
    try {
      st.gameHandshake = await gameHandshakeProbe(relay);
      return st.gameHandshake;
    } finally {
      st.gameHandshakeBusy = false;
    }
  },
  "protocol-probe": async (relay, token) => {
    if (st.protocolBusy) throw new Error("protocol probe already running");
    st.protocolBusy = true;
    st.protocol = null;
    try {
      st.protocol = await protocolProbe(relay, token);
      return st.protocol;
    } finally {
      st.protocolBusy = false;
    }
  },
  "asset-probe": async (relay, token, index, archive) => {
    if (st.assetBusy) throw new Error("asset probe already running");
    st.assetBusy = true;
    st.asset = null;
    st.assetFiles = null;
    try {
      const result = await assetProbe(relay, token, Number(index), Number(archive));
      st.asset = result.report;
      st.assetFiles = result.files;
      return st.asset;
    } finally {
      st.assetBusy = false;
    }
  },
  "asset-file": (id) => {
    const value = Number(id);
    if (!Number.isSafeInteger(value) || value < 0 || value > 0x7fffffff) throw new Error("invalid asset file ID");
    const bytes = st.assetFiles?.get(value);
    if (!bytes) throw new Error("asset file not loaded");
    if (bytes.length > 64 * 1024) throw new Error("asset diagnostic export byte limit");
    return Array.from(bytes);
  },
  "script-graph-probe": async (relay, token, root, ints) => {
    if (st.scriptGraphBusy) throw new Error("script graph probe already running");
    st.scriptGraphBusy = true;
    st.scriptGraph = null;
    st.scriptGraphFiles = null;
    try {
      const result = await scriptGraphProbe(relay, token, Number(root), { ints: ints === undefined ? undefined : JSON.parse(ints) });
      st.scriptGraph = { ...result.report, execution: result.execution };
      st.scriptGraphFiles = result.files;
      return st.scriptGraph;
    } finally {
      st.scriptGraphBusy = false;
    }
  },
  "script-graph-file": (id) => {
    const value = Number(id);
    if (!Number.isSafeInteger(value) || value < 0 || value > 1000000) throw new Error("invalid script file ID");
    const bytes = st.scriptGraphFiles?.get(value);
    if (!bytes) throw new Error("script file not loaded");
    if (bytes.length > 64 * 1024) throw new Error("script diagnostic export byte limit");
    return Array.from(bytes);
  },
  "cs2-probe": async (path) => coreScriptProbe(path),
  "cs2-run": async (relay, scriptId, ints = "[]", strings = "[]") => {
    const js5Relay = js5RelayArgument(relay);
    const root = unsignedArgument(scriptId, 99999, "bounded script ID required");
    let intValues;
    let stringValues;
    try {
      if (typeof ints !== "string" || typeof strings !== "string" || ints.length > 16384 || strings.length > 16384) throw new Error();
      intValues = JSON.parse(ints);
      stringValues = JSON.parse(strings);
    } catch { throw new Error("script arguments must be JSON arrays"); }
    if (st.cs2RunBusy) throw new Error("cs2 run already running");
    st.cs2RunBusy = true;
    try {
      return await cs2RunProbe(js5Relay, root, st.original?.varp ? st.original : null, { ints: intValues, strings: stringValues });
    } finally {
      st.cs2RunBusy = false;
    }
  },
  scene: async (name = "all") => {
    disconnect();
    need().scene(name);
    await waitFrames(1);
    return summary();
  },
  replay: async (url, label = url) => {
    const r = await fetch(url, { cache: "no-store" });
    if (!r.ok) throw new Error(`${url}: HTTP ${r.status}`);
    need().replay(new Uint8Array(await r.arrayBuffer()), label);
    const [w, h] = info().frameSize;
    size(w, h);
    await waitFrames(2);
    return summary();
  },
  resize: async (wh = "") => {
    const [w, h] = wh.split("x").map(Number);
    if (!(w > 0 && h > 0)) throw new Error("usage: resize WxH");
    size(w, h);
    await waitFrames(1);
    return `${st.canvas.width}x${st.canvas.height}`;
  },
  console: (n = "20") => lines.slice(-Number(n)),
  rect: () => {
    const r = st.canvas.getBoundingClientRect();
    return { left: r.left, top: r.top, width: r.width, height: r.height, cw: st.canvas.width, ch: st.canvas.height };
  },
  input: () => input,
  "input-reset": () => {
    input.counts = {};
    input.last = [];
  },
  png: async () => {
    const rb = need().capture();
    tick();
    if (!rb) throw new Error("surface texture unavailable");
    const [w, h] = [rb.width, rb.height];
    const rgba = await rb.read();
    const c = new OffscreenCanvas(w, h);
    c.getContext("2d").putImageData(new ImageData(new Uint8ClampedArray(rgba.buffer, rgba.byteOffset, rgba.length), w, h), 0, 0);
    const seen = new Set();
    for (let i = 0; i < rgba.length; i += 388) seen.add((rgba[i] << 16) | (rgba[i + 1] << 8) | rgba[i + 2]);
    return { width: w, height: h, colors: seen.size, data: await base64(await c.convertToBlob({ type: "image/png" })) };
  },
  dump: async () => ({ data: await base64(new Blob([need().dump()])), summary: summary() }),
  bench: async (n = "60") => {
    st.busy = true;
    try {
      return await need().bench(Number(n) || 60);
    } finally {
      st.busy = false;
    }
  },
  help: () => [...Object.keys(commands).filter((k) => k !== "png" && k !== "dump"), "screenshot", "dump-frame"].sort(),
};

async function run(cmd, args = []) {
  const f = commands[cmd];
  if (!f) return { ok: false, error: `unknown command ${cmd}, see help`, frames: st.frames };
  try {
    return { ok: true, data: (await f(...args)) ?? null, frames: st.frames };
  } catch (e) {
    return { ok: false, error: String(e?.message ?? e), frames: st.frames };
  }
}

window.openrs = { run, state, describe, game: { connect, disconnect, stats: streamStats } };

export async function install(app, canvas, backend) {
  Object.assign(st, { app, canvas, backend });
  setup(app, canvas);
  const a = await navigator.gpu?.requestAdapter().catch(() => null);
  const i = a?.info ?? {};
  st.gpu = a ? { vendor: i.vendor, architecture: i.architecture, device: i.device, description: i.description, fallback: i.isFallbackAdapter ?? a.isFallbackAdapter ?? false } : null;
  requestAnimationFrame(loop);
}
