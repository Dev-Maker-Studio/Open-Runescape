import { openLobbySession, takePrivateLobbyConfig } from "./lobby-session.js";
import { buildLoginConfig, fetchLoginCrcs } from "./login-config.js";
import { observeBrowserLoginOptions } from "./browser-login-profile.js";
import { loadWorldVarpLayout } from "./world-config.js";
import { openWorldTerrain } from "./world-terrain.js";
import { openWorldFloors } from "./world-floors.js";
import { openWorldMaterials } from "./world-materials.js";
import { openWorldLocs } from "./world-locs.js";

function describe(error) {
  const text = String(error?.message ?? error).replace(/[^\x20-\x7e]/g, " ").trim();
  return text.length > 200 ? `${text.slice(0, 200)}...` : text;
}

function wipePart(part) {
  if (!part || typeof part !== "object") return;
  for (const key of ["seed", "clientRandomSeed", "machineUid"]) if (Array.isArray(part[key])) part[key].fill(0);
  part.password = part.identity = null;
}

function wipeConfig(config) {
  if (!config || typeof config !== "object") return;
  for (const part of [config.browserAccount, config.lobby, config.world]) wipePart(part);
}

function selectedJs5Relay(relay) {
  if (typeof relay !== "string" || relay.length > 512 || !/^wss?:\/\/(?:127\.0\.0\.1|localhost|\[::1\])(?::[0-9]{1,5})?\/js5$/.test(relay)) throw new Error("original asset route unavailable");
  const url = new URL(relay);
  if (!["ws:", "wss:"].includes(url.protocol) || !["127.0.0.1", "localhost", "[::1]"].includes(url.hostname) || url.pathname !== "/js5" || url.username || url.password || url.search || url.hash || url.port === "0") throw new Error("original asset route unavailable");
  return url.href;
}

function selectedWorld(lobby, worldId, { signal, timeoutMs }) {
  return new Promise((resolve, reject) => {
    let done = false;
    let requested = false;
    let poll;
    let timer;
    const finish = (error, world) => {
      if (done) return;
      done = true;
      clearTimeout(timer);
      clearTimeout(poll);
      signal?.removeEventListener("abort", abort);
      if (error) reject(new Error("selected original world unavailable"));
      else resolve(world);
    };
    const abort = () => finish(true);
    const check = () => {
      if (done) return;
      try {
        if (signal?.aborted) throw new Error();
        const snapshot = lobby.state();
        if (!snapshot || snapshot.connected === false || snapshot.accepted === false) throw new Error();
        const list = snapshot.worldList;
        let version = 0;
        if (list != null) {
          if (!Array.isArray(list.worlds) || list.worlds.length > 65536) throw new Error();
          if (list.version !== null) {
            if (!Number.isSafeInteger(list.version) || list.version < -2147483648 || list.version > 2147483647) throw new Error();
            version = list.version;
          }
          const matches = list.worlds.filter((world) => world?.worldNumber === worldId);
          if (matches.length > 1) throw new Error();
          if (matches.length === 1) {
            const flags = matches[0].flags;
            if (!Number.isSafeInteger(flags) || flags < -2147483648 || flags > 2147483647) throw new Error();
            finish(false, { flags });
            return;
          }
        }
        if (!requested) {
          requested = true;
          lobby.requestWorldList(version);
        }
        if (!done) poll = setTimeout(check, 20);
      } catch { finish(true); }
    };
    timer = setTimeout(() => finish(true), timeoutMs);
    signal?.addEventListener("abort", abort, { once: true });
    check();
  });
}

export async function openOriginalSession({
  scene = false,
  lobbyRelay = "ws://127.0.0.1:8110/lobby",
  worldRelay = "ws://127.0.0.1:8110/world",
  takeConfig = takePrivateLobbyConfig,
  openLobby = openLobbySession,
  buildConfig = buildLoginConfig,
  fetchCrcs = fetchLoginCrcs,
  fetchLayout = loadWorldVarpLayout,
  openTerrain = openWorldTerrain,
  openFloors = openWorldFloors,
  openMaterials = openWorldMaterials,
  openLocs = openWorldLocs,
  observeOptions = observeBrowserLoginOptions,
  js5Relay = "ws://127.0.0.1:8110/js5",
  worldJs5Relay = "ws://127.0.0.1:8111/js5",
  worldListTimeoutMs = 15000,
  withUnderwater = false,
  fontIndex,
  worldId,
  onState = () => {},
  signal,
} = {}) {
  let config;
  let lobbyConfig;
  let worldConfig;
  let preparation;
  let varpLayout;
  let assetRelay;
  let lobby;
  let world;
  let terrain;
  let floors;
  let materials;
  let underwater;
  let underwaterError;
  let assetStage = null;
  let closed = false;
  const locSources = [];
  const released = new WeakSet();
  const release = (owner) => {
    if (!owner || !["object", "function"].includes(typeof owner) || released.has(owner)) return;
    released.add(owner);
    try { owner.close(); } catch {}
  };
  const close = () => {
    if (closed) return;
    closed = true;
    for (const owner of [...locSources, underwater, terrain, floors, materials, world, lobby]) release(owner);
  };
  const wait = async (promise) => {
    let abort;
    try {
      return await Promise.race([
        promise,
        new Promise((_, reject) => {
          abort = () => reject(new Error("original session cancelled"));
          if (signal?.aborted) abort();
          else signal?.addEventListener("abort", abort, { once: true });
        }),
      ]);
    } finally { signal?.removeEventListener("abort", abort); }
  };
  const retain = async (opening, assign) => {
    const loading = Promise.resolve(opening);
    loading.then((owner) => { if (closed || signal?.aborted) release(owner); }, () => {});
    const owner = await wait(loading);
    if (closed || signal?.aborted) {
      release(owner);
      throw new Error("original session cancelled");
    }
    assign(owner);
  };
  signal?.addEventListener("abort", close, { once: true });
  try {
    signal?.throwIfAborted();
    let text = await wait(takeConfig({ signal }));
    try { config = JSON.parse(text); }
    catch { throw new Error("invalid private session configuration"); }
    finally { text = null; }
    if (config && !Array.isArray(config) && Object.keys(config).join(",") === "browserAccount") {
      if (!Number.isSafeInteger(worldListTimeoutMs) || worldListTimeoutMs < 1 || worldListTimeoutMs > 30000) throw new Error("bounded original world-list deadline required");
      const options = await wait(observeOptions({ fontIndex, worldId, signal }));
      if (![32, 34].includes(options.fontIndex)) throw new Error("original font selection unavailable");
      preparation = { fontIndex: options.fontIndex };
      signal?.throwIfAborted();
      const envelope = config;
      try {
        const building = buildConfig(envelope.browserAccount, { ...options, js5Relay, signal });
        building.then((result) => { if (signal?.aborted) wipeConfig(result); }, () => {});
        config = await wait(building);
      } finally { wipeConfig(envelope); }
    }
    if (!config || Array.isArray(config) || Object.keys(config).sort().join(",") !== "lobby,world" || !config.lobby || !config.world || Array.isArray(config.lobby) || Array.isArray(config.world) || typeof config.lobby !== "object" || typeof config.world !== "object") {
      throw new Error("private lobby and world configurations required");
    }
    if (preparation) {
      const selectedId = config.world.worldId;
      if (!Number.isSafeInteger(selectedId) || selectedId < 1 || selectedId > 65535) throw new Error("original world selection unavailable");
      preparation.worldId = selectedId;
    }
    lobbyConfig = JSON.stringify(config.lobby);
    worldConfig = JSON.stringify(config.world);
    wipeConfig(config);
    config = null;
    signal?.throwIfAborted();
    const opening = openLobby(lobbyRelay, lobbyConfig, { signal });
    opening.then((session) => { if (signal?.aborted) session.close(); }, () => {});
    lobby = await wait(opening);
    lobbyConfig = null;
    await wait(lobby.ready);
    signal?.throwIfAborted();
    if (preparation) {
      const selected = await wait(selectedWorld(lobby, preparation.worldId, { signal, timeoutMs: worldListTimeoutMs }));
      assetRelay = selectedJs5Relay(selected.flags & 0x20 ? worldJs5Relay : js5Relay);
      if (selected.flags & 0x20) {
        assetStage = "world CRCs";
        const indexCrcs = await wait(fetchCrcs({ js5Relay: assetRelay, fontIndex: preparation.fontIndex, signal }));
        signal?.throwIfAborted();
        if (!Array.isArray(indexCrcs) || indexCrcs.length !== 36 || Array.from(indexCrcs).some((crc) => !Number.isSafeInteger(crc) || crc < 0 || crc > 0xffffffff)) throw new Error("original world CRCs unavailable");
        const prepared = JSON.parse(worldConfig);
        try {
          prepared.indexCrcs = [...indexCrcs];
          worldConfig = JSON.stringify(prepared);
        } finally { wipePart(prepared); }
      }
      assetStage = "variable layout";
      const layout = await wait(fetchLayout({ js5Relay: assetRelay, signal }));
      signal?.throwIfAborted();
      if (!layout || !Number.isSafeInteger(layout.varpCount) || layout.varpCount < 0 || layout.varpCount > 65536 || !Number.isSafeInteger(layout.definitionFiles) || layout.definitionFiles < 0 || layout.definitionFiles > layout.varpCount) throw new Error("original variable layout unavailable");
      varpLayout = Object.freeze({ varpCount: layout.varpCount, definitionFiles: layout.definitionFiles, source: "js5-index-2-archive-16-file-slots", authenticated: false });
    }
    signal?.throwIfAborted();
    assetStage = null;
    world = lobby.startWorld(worldRelay, worldConfig, varpLayout ? { varpCount: varpLayout.varpCount, inventory: true, playerStats: true, ...(scene ? { scene: true } : {}) } : undefined);
    worldConfig = null;
    const ready = await wait(world.ready);
    signal?.throwIfAborted();
    if (preparation) {
      assetStage = "floors";
      await retain(openFloors({ js5Relay: assetRelay, signal }), (owner) => { floors = owner; });
      signal?.throwIfAborted();
      assetStage = "terrain";
      await retain(openTerrain({ js5Relay: assetRelay, initialWorld: ready.initialWorld, floors, signal }), (owner) => { terrain = owner; });
      signal?.throwIfAborted();
      assetStage = "materials";
      await retain(openMaterials({ js5Relay: assetRelay, signal }), (owner) => { materials = owner; });
      signal?.throwIfAborted();
      if (withUnderwater) {
        assetStage = "underwater terrain";
        try { await retain(openTerrain({ js5Relay: assetRelay, initialWorld: ready.initialWorld, underwater: true, signal }), (owner) => { underwater = owner; }); }
        catch (error) { signal?.throwIfAborted(); underwater = undefined; underwaterError = describe(error); }
      }
      assetStage = null;
    }
    const ground = () => {
      if (closed || signal?.aborted || world.state().connected === false) throw new Error("original ground unavailable");
      return terrain;
    };
    return Object.freeze({
      state: () => ({ lobby: lobby.state(), world: world.state(), scope: terrain ? "original-login-varps-and-surface-terrain" : "original-login-and-initial-world-only", ...(varpLayout ? { definitions: varpLayout } : {}), ...(terrain ? { terrain: terrain.state() } : {}), ...(floors ? { floorDefinitions: floors.state() } : {}), ...(withUnderwater ? { lit: Boolean(underwater), ...(underwaterError ? { underwaterError } : {}) } : {}), ...(materials ? { materialDefinitions: materials.state() } : {}), gameplay: false, renderedWorld: false }),
      ...(varpLayout ? {
        varp: (id) => world.varp(id),
        container: (id, negativeKey) => world.container(id, negativeKey),
        item: (id, negativeKey, slot) => world.item(id, negativeKey, slot),
        skill: (id) => world.skill(id),
        playerStats: (...args) => {
          if (args.length) throw new Error("invalid original player stats query");
          return world.playerStats();
        },
        cycleInput: (input, now, rendering) => world.cycleInput(input, now, rendering),
        ...(scene && typeof world.lendSceneSession === "function" ? { lendScene: (use) => world.lendSceneSession(use) } : {}),
      } : {}),
      ...(terrain ? {
        terrainHeight: (region, plane, x, y) => terrain.height(region, plane, x, y),
        sceneHeight: (plane, x, y) => terrain.sceneHeight(plane, x, y),
        sceneTile: (plane, x, y) => terrain.sceneTile(plane, x, y),
        ...(floors ? { paletteColor: (plane, x, y) => terrain.paletteColor(plane, x, y) } : {}),
      } : {}),
      ...(floors ? { underlay: (id) => floors.underlay(id), overlay: (id) => floors.overlay(id) } : {}),
      ...(materials ? { material: (id) => materials.material(id) } : {}),
      ...(terrain && floors && materials && typeof terrain.prepareGround === "function" && typeof terrain.groundState === "function" && typeof terrain.groundTile === "function" ? {
        prepareGround(...args) {
          try {
            if (closed || signal?.aborted || world.state().connected === false || args.length > 2) throw new Error();
            const report = terrain.prepareGround(floors, materials, ...args);
            if (closed || signal?.aborted || world.state().connected === false) throw new Error();
            return report;
          } catch { throw new Error("original ground preparation unavailable"); }
        },
        groundState(...args) {
          if (args.length) throw new Error("invalid original ground query");
          try {
            if (closed || signal?.aborted || world.state().connected === false) return null;
            const report = terrain.groundState();
            if (closed || signal?.aborted || world.state().connected === false) return null;
            return report;
          } catch { throw new Error("original ground state unavailable"); }
        },
        groundTile(...args) {
          try {
            if (closed || signal?.aborted || world.state().connected === false) throw new Error();
            const tile = terrain.groundTile(...args);
            if (closed || signal?.aborted || world.state().connected === false) throw new Error();
            return tile;
          } catch { throw new Error("original ground tile unavailable"); }
        },
      } : {}),
      ...(terrain && materials && typeof terrain.lowerGround === "function" && typeof terrain.loweredMaterial === "function" ? {
        lowerGround: (rgb) => (underwater ? ground().lowerGround(materials, rgb, underwater) : ground().lowerGround(materials, rgb)),
        loweredMaterial: (index) => ground().loweredMaterial(index),
        lit: () => Boolean(underwater),
      } : {}),
      ...(terrain && typeof terrain.bakeShadows === "function" ? {
        addShadowLocs: (regionId, data, baseX, baseY) => ground().addShadowLocs(regionId, data, baseX, baseY),
        addShadowLocType: (id, data) => ground().addShadowLocType(id, data),
        addShadowLocModel: (id, data) => ground().addShadowLocModel(id, data),
        bakeShadows: (texturesEnabled) => ground().bakeShadows(texturesEnabled),
      } : {}),
      ...(terrain && varpLayout && typeof world.lendLocSession === "function" && typeof terrain.bakeShadows === "function" ? {
        locSource: async () => {
          if (closed || signal?.aborted || world.state().connected === false) throw new Error("original world locs unavailable");
          const source = await openLocs({ js5Relay: assetRelay, lend: (use) => world.lendLocSession(use), signal });
          if (closed || signal?.aborted) {
            release(source);
            throw new Error("original world locs unavailable");
          }
          locSources.push(source);
          return source;
        },
      } : {}),
      ...(terrain && ready.initialWorld && typeof terrain.bakeShadows === "function" ? { initialWorld: () => JSON.parse(JSON.stringify(ready.initialWorld)) } : {}),
      close,
    });
  } catch (error) {
    const stage = closed || signal?.aborted ? null : assetStage;
    close();
    try { onState({ lobby: lobby?.state() ?? null, world: world?.state() ?? null, scope: "original-login-and-initial-world-only", gameplay: false, renderedWorld: false }); } catch {}
    throw new Error(stage ? `original session did not reach initial world state (${stage}: ${describe(error)})` : "original session did not reach initial world state");
  } finally {
    signal?.removeEventListener("abort", close);
    wipeConfig(config);
    config = lobbyConfig = worldConfig = preparation = null;
  }
}
