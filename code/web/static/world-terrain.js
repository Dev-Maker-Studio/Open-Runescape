import { loadRuntime } from "./js5-probe.js";
import { PUBLIC_LOGIN_DEFAULTS } from "./login-config.js";
import { worldFloorsRetained, prepareWorldFloorGround, composeWorldFloorPalette } from "./world-floors.js";
import { worldMaterialsRetained, lendWorldMaterialProbe } from "./world-materials.js";

const MIB = 1024 * 1024;
const MAX_MESSAGE = 4 * MIB;
const MAX_RECEIVED = 32 * MIB;
const UNAVAILABLE = "verified world terrain unavailable";
const CANCELLED = "world terrain cancelled";
const TIMED_OUT = "world terrain timed out";
const GROUND_UNAVAILABLE = "world ground preparation unavailable";
const GROUND_LOWERING_UNAVAILABLE = "world ground lowering unavailable";
const GROUND_TILE_UNAVAILABLE = "world ground tile unavailable";
const MAX_GROUND_VERTICES = 1016064;
const GROUND_BORROWERS = new WeakMap();
const UNDERWATER_PROBES = new WeakMap();

function underwaterReport(value, regionIds) {
  const report = jsonReport(value);
  if (report.index !== 5 || report.authenticated !== false || report.referenceCrcVerified !== true || report.referenceWhirlpoolVerified !== true) throw new Error(UNAVAILABLE);
  if (!Array.isArray(report.regions) || report.regions.length !== regionIds.length) throw new Error(UNAVAILABLE);
  const regions = report.regions.map((region, at) => {
    record(region);
    if (region.regionId !== regionIds[at] || typeof region.present !== "boolean" || region.effectsDecoded !== false) throw new Error(UNAVAILABLE);
    return { regionId: region.regionId, present: region.present };
  });
  return { index: 5, regions };
}

export function invokeGroundBorrower(borrower, floors, materials, plane, maxVertices) {
  const prepare = GROUND_BORROWERS.get(borrower);
  if (!prepare) throw new Error(GROUND_UNAVAILABLE);
  return prepare(floors, materials, plane, maxVertices);
}
const MAP_SIZES = [72, 104, 120, 136, 168];
const OPERATION_UNIT = "wire/container/expanded/split/tile bytes and bounded metadata records";
const LIMITS = {
  maxRegions: 36,
  maxReferenceArchives: 262144,
  maxReferenceFiles: 262144,
  maxReferenceRecords: 524288,
  maxRetainedArchives: 36,
  maxRetainedFiles: 4096,
  maxContainerBytes: MAX_MESSAGE,
  maxExpandedContainerBytes: 8 * MIB,
  maxTerrainFileBytes: MIB,
  maxTagsPerTile: 256,
  maxTotalWireBytes: MAX_RECEIVED,
  maxTotalContainerBytes: 24 * MIB,
  maxTotalExpandedBytes: 32 * MIB,
  maxTotalOperations: 128 * MIB,
};

function bounded(value, min, max) {
  return Number.isSafeInteger(value) && value >= min && value <= max;
}

function integer(value, min, max) {
  if (!bounded(value, min, max)) throw new Error(UNAVAILABLE);
  return value;
}

function record(value) {
  if (!value || typeof value !== "object" || Array.isArray(value) || ![Object.prototype, null].includes(Object.getPrototypeOf(value))) throw new Error(UNAVAILABLE);
  return value;
}

function field(value, key) {
  const descriptor = Object.getOwnPropertyDescriptor(record(value), key);
  if (!descriptor || !Object.hasOwn(descriptor, "value")) throw new Error(UNAVAILABLE);
  return descriptor.value;
}

function relayUrl(relay) {
  if (typeof relay !== "string" || relay.length > 512 || !/^wss?:\/\/(?:127\.0\.0\.1|localhost|\[::1\])(?::[0-9]{1,5})?\/js5$/.test(relay)) throw new Error(UNAVAILABLE);
  const url = new URL(relay);
  if (!["ws:", "wss:"].includes(url.protocol) || !["127.0.0.1", "localhost", "[::1]"].includes(url.hostname) || url.pathname !== "/js5" || url.username || url.password || url.search || url.hash || url.port === "0") throw new Error(UNAVAILABLE);
  return url.href;
}

function preparation(initialWorld) {
  if (field(initialWorld, "mapKind") !== "standard") throw new Error(UNAVAILABLE);
  const mapSize = field(initialWorld, "mapSize");
  if (!MAP_SIZES.includes(mapSize)) throw new Error(UNAVAILABLE);
  const base = field(initialWorld, "base");
  const x = integer(field(base, "x"), 0, 16384 - mapSize);
  const y = integer(field(base, "y"), 0, 16384 - mapSize);
  if (x % 8 || y % 8) throw new Error(UNAVAILABLE);
  const regions = field(initialWorld, "regionIds");
  if (!Array.isArray(regions) || regions.length < 1 || regions.length > 36) throw new Error(UNAVAILABLE);
  const regionIds = [];
  for (let at = 0; at < regions.length; at++) {
    const descriptor = Object.getOwnPropertyDescriptor(regions, at);
    if (!descriptor || !Object.hasOwn(descriptor, "value")) throw new Error(UNAVAILABLE);
    const id = integer(descriptor.value, 0, 65535);
    if (regionIds.includes(id)) throw new Error(UNAVAILABLE);
    regionIds.push(id);
  }
  return { base: { x, y }, mapSize, regionIds };
}

function jsonReport(value) {
  if (typeof value !== "string" || value.length > 64 * 1024) throw new Error(UNAVAILABLE);
  return record(JSON.parse(value));
}

function terrainReport(value, regionIds, received) {
  const report = jsonReport(value);
  if (report.index !== 5 || report.authenticated !== false || report.referenceCrcVerified !== true || report.referenceWhirlpoolVerified !== true) throw new Error(UNAVAILABLE);
  const limits = record(report.limits);
  const safeLimits = {};
  for (const [key, expected] of Object.entries(LIMITS)) {
    if (limits[key] !== expected) throw new Error(UNAVAILABLE);
    safeLimits[key] = expected;
  }
  if (limits.tileTagLimitIsDiagnostic !== true || limits.operationUnit !== OPERATION_UNIT) throw new Error(UNAVAILABLE);
  safeLimits.tileTagLimitIsDiagnostic = true;
  safeLimits.operationUnit = OPERATION_UNIT;
  if (!Array.isArray(report.regions) || report.regions.length !== regionIds.length) throw new Error(UNAVAILABLE);
  const regions = report.regions.map((region, at) => {
    record(region);
    const regionId = regionIds[at];
    const origin = [(regionId >> 8) * 64, (regionId & 255) * 64];
    if (region.regionId !== regionId || !Array.isArray(region.origin) || region.origin.length !== 2 || region.origin.some((value, index) => value !== origin[index]) || typeof region.present !== "boolean" || region.effectsDecoded !== false || typeof region.crcVerified !== "boolean" || typeof region.whirlpoolVerified !== "boolean") throw new Error(UNAVAILABLE);
    const safe = {
      regionId, origin, present: region.present,
      archiveId: region.archiveId === null ? null : integer(region.archiveId, 0, 0x7fffffff),
      archiveRevision: region.archiveRevision === null ? null : integer(region.archiveRevision, -0x80000000, 0x7fffffff),
      containerBytes: integer(region.containerBytes, 0, MAX_MESSAGE),
      expandedBytes: integer(region.expandedBytes, 0, 8 * MIB),
      terrainBytes: integer(region.terrainBytes, 0, MIB),
      suffixBytes: integer(region.suffixBytes, 0, MIB),
      tileCount: integer(region.tileCount, 0, 4 * 64 * 64),
      operations: integer(region.operations, 0, LIMITS.maxTotalOperations),
      crcVerified: region.crcVerified,
      whirlpoolVerified: region.whirlpoolVerified,
      effectsDecoded: false,
    };
    if (safe.present) {
      if (safe.archiveId === null || safe.archiveRevision === null || !safe.crcVerified || safe.tileCount !== 4 * 64 * 64 || safe.containerBytes < 5 || safe.terrainBytes < safe.tileCount || safe.terrainBytes + safe.suffixBytes > MIB || safe.terrainBytes + safe.suffixBytes > safe.expandedBytes) throw new Error(UNAVAILABLE);
    } else if (safe.archiveId !== null || safe.archiveRevision !== null || safe.containerBytes || safe.expandedBytes || safe.terrainBytes || safe.suffixBytes || safe.tileCount || safe.operations || safe.crcVerified || safe.whirlpoolVerified) throw new Error(UNAVAILABLE);
    return safe;
  });
  const safe = {
    index: 5,
    referenceRevision: integer(report.referenceRevision, -0x80000000, 0x7fffffff),
    referenceArchives: integer(report.referenceArchives, 0, LIMITS.maxReferenceArchives),
    referenceFiles: integer(report.referenceFiles, 0, LIMITS.maxReferenceFiles),
    retainedArchives: integer(report.retainedArchives, 0, LIMITS.maxRetainedArchives),
    referenceCrcVerified: true,
    referenceWhirlpoolVerified: true,
    authenticated: false,
    totalWireBytes: integer(report.totalWireBytes, 0, MAX_RECEIVED),
    totalContainerBytes: integer(report.totalContainerBytes, 0, LIMITS.maxTotalContainerBytes),
    totalExpandedBytes: integer(report.totalExpandedBytes, 0, LIMITS.maxTotalExpandedBytes),
    totalOperations: integer(report.totalOperations, 0, LIMITS.maxTotalOperations),
    limits: safeLimits,
    regions,
  };
  const presentArchives = regions.filter((region) => region.present).length;
  if (safe.referenceArchives + safe.referenceFiles > LIMITS.maxReferenceRecords || safe.retainedArchives !== presentArchives || safe.retainedArchives > safe.referenceArchives || safe.retainedArchives > safe.referenceFiles) throw new Error(UNAVAILABLE);
  if (safe.totalWireBytes !== received || regions.reduce((sum, region) => sum + region.containerBytes, 0) > safe.totalContainerBytes || regions.reduce((sum, region) => sum + region.expandedBytes, 0) > safe.totalExpandedBytes || regions.reduce((sum, region) => sum + region.operations, 0) > safe.totalOperations) throw new Error(UNAVAILABLE);
  return safe;
}

function surfaceSummary(value, prepared) {
  const summary = jsonReport(value);
  const stride = prepared.mapSize + 1;
  const heightCount = 4 * stride * stride;
  if (summary.scope !== "surface-only" || summary.mapSize !== prepared.mapSize || summary.stride !== stride || summary.planes !== 4 || summary.heightCount !== heightCount || summary.effectsDecoded !== false || summary.renderedWorld !== false || !Array.isArray(summary.baseTile) || summary.baseTile.length !== 2 || summary.baseTile[0] !== prepared.base.x || summary.baseTile[1] !== prepared.base.y || !Array.isArray(summary.regions) || summary.regions.length !== prepared.regionIds.length) throw new Error(UNAVAILABLE);
  const knownHeights = integer(summary.knownHeights, 0, heightCount);
  const unavailableHeights = integer(summary.unavailableHeights, 0, heightCount);
  if (knownHeights + unavailableHeights !== heightCount) throw new Error(UNAVAILABLE);
  const regions = summary.regions.map((region, at) => {
    record(region);
    if (region.regionId !== prepared.regionIds[at] || !["present", "provenAbsent", "unavailable"].includes(region.availability)) throw new Error(UNAVAILABLE);
    return { regionId: region.regionId, availability: region.availability };
  });
  return {
    scope: "surface-only", baseTile: [prepared.base.x, prepared.base.y], mapSize: prepared.mapSize,
    stride, planes: 4, heightCount, knownHeights, unavailableHeights, regions,
    effectsDecoded: false, renderedWorld: false,
  };
}

function sceneTileSnapshot(value) {
  if (typeof value !== "string" || value.length > 1024) throw new Error(UNAVAILABLE);
  const tile = JSON.parse(value);
  if (tile === null) return null;
  record(tile);
  const ranges = { underlayId: 255, overlayId: 255, renderFlags: 32, overlayShape: 11, overlayRotation: 3 };
  const keys = Object.keys(tile);
  if (keys.length !== Object.keys(ranges).length || keys.some((key) => !Object.hasOwn(ranges, key))) throw new Error(UNAVAILABLE);
  return Object.freeze(Object.fromEntries(Object.entries(ranges).map(([key, max]) => [key, integer(tile[key], 0, max)])));
}

function exactKeys(value, keys) {
  record(value);
  if (Object.keys(value).length !== keys.length || keys.some((key) => !Object.hasOwn(value, key))) throw new Error(UNAVAILABLE);
}

function groundSummary(value, prepared, plane, maxVertices) {
  const summary = jsonReport(value);
  exactKeys(summary, ["scope", "plane", "mapSize", "tileCount", "vertexCount", "emittedVertexCount", "terrainLowered", "texturePixelsDecoded", "renderedWorld"]);
  if (summary.scope !== "blended-surface-geometry" || summary.plane !== plane || summary.mapSize !== prepared.mapSize || summary.terrainLowered !== false || summary.texturePixelsDecoded !== false || summary.renderedWorld !== false) throw new Error(UNAVAILABLE);
  const tileCount = integer(summary.tileCount, 0, prepared.mapSize * prepared.mapSize);
  const vertexCount = integer(summary.vertexCount, tileCount * 3, Math.min(maxVertices, tileCount * 36));
  const emittedVertexCount = integer(summary.emittedVertexCount, 0, vertexCount);
  if (vertexCount % 3 || emittedVertexCount % 3) throw new Error(UNAVAILABLE);
  return Object.freeze({ scope: "blended-surface-geometry", plane: summary.plane + 0, mapSize: prepared.mapSize, tileCount, vertexCount, emittedVertexCount, terrainLowered: false, texturePixelsDecoded: false, renderedWorld: false });
}

function groundTileSnapshot(value, x, y) {
  if (typeof value !== "string" || value.length > 16 * 1024) throw new Error(UNAVAILABLE);
  const tile = JSON.parse(value);
  if (tile === null) return null;
  exactKeys(tile, ["x", "y", "shape", "rotation", "splitEdges", "vertices", "blendingColors", "emittedVertices", "hasShadows"]);
  if (tile.x !== x || tile.y !== y || !Array.isArray(tile.splitEdges) || tile.splitEdges.length !== 4 || tile.splitEdges.some((edge) => typeof edge !== "boolean") || !Array.isArray(tile.vertices) || !bounded(tile.vertices.length, 3, 36) || tile.vertices.length % 3 || typeof tile.hasShadows !== "boolean") throw new Error(UNAVAILABLE);
  const vertices = Object.freeze(tile.vertices.map((vertex) => {
    exactKeys(vertex, ["offsetX", "offsetZ", "mainColor", "texture", "textureScale"]);
    const textureScale = integer(vertex.textureScale, 0, 262140);
    if (textureScale % 4) throw new Error(UNAVAILABLE);
    return Object.freeze({ offsetX: integer(vertex.offsetX, 0, 512), offsetZ: integer(vertex.offsetZ, 0, 512), mainColor: integer(vertex.mainColor, -1, 65535), texture: integer(vertex.texture, -1, 65534), textureScale });
  }));
  let blendingColors = null;
  if (tile.blendingColors !== null) {
    if (!Array.isArray(tile.blendingColors) || tile.blendingColors.length !== vertices.length) throw new Error(UNAVAILABLE);
    blendingColors = Object.freeze(tile.blendingColors.map((color) => integer(color, -1, 65535)));
  }
  const emittedVertices = integer(tile.emittedVertices, 0, vertices.length);
  if (emittedVertices % 3) throw new Error(UNAVAILABLE);
  return Object.freeze({ x: tile.x + 0, y: tile.y + 0, shape: integer(tile.shape, 0, 14), rotation: integer(tile.rotation, 0, 3), splitEdges: Object.freeze([...tile.splitEdges]), vertices, blendingColors, emittedVertices, hasShadows: tile.hasShadows });
}

function wipe(value) {
  try { if (ArrayBuffer.isView(value)) new Uint8Array(value.buffer, value.byteOffset, value.byteLength).fill(0); }
  catch {}
}

export async function openWorldTerrain({
  js5Relay,
  initialWorld,
  floors,
  underwater = false,
  signal,
  load = async () => (await loadRuntime()).Js5WorldTerrainProbe,
  WebSocketClass = globalThis.WebSocket,
  timeoutMs = 30000,
  lifetimeMs = 120000,
} = {}) {
  let url;
  let prepared;
  let preCancelled = false;
  try {
    if (signal != null && (typeof signal.aborted !== "boolean" || typeof signal.addEventListener !== "function" || typeof signal.removeEventListener !== "function")) throw new Error(UNAVAILABLE);
    preCancelled = signal?.aborted === true;
    if (preCancelled) throw new Error(CANCELLED);
    url = relayUrl(js5Relay);
    prepared = preparation(initialWorld);
    if (!bounded(timeoutMs, 1, 30000) || !bounded(lifetimeMs, 1, 300000) || typeof load !== "function" || typeof WebSocketClass !== "function" || typeof underwater !== "boolean" || (underwater && floors !== undefined) || (floors !== undefined && worldFloorsRetained(floors) !== true)) throw new Error(UNAVAILABLE);
  } catch {
    throw new Error(preCancelled ? CANCELLED : UNAVAILABLE);
  }
  initialWorld = null;
  return new Promise((resolve, reject) => {
    let phase = "pending";
    let probe;
    let socket;
    let transportClosed = false;
    let exchangeTimer;
    let lifetimeTimer;
    let retentionDeadline;
    let received = 0;
    let current;
    let ground = null;
    let groundSources = null;
    let groundBusy = false;
    let groundChecking = false;
    let groundEpoch = 0;
    const deadline = performance.now() + timeoutMs;
    const snapshot = () => {
      maintain();
      maintainGround();
      return { ...structuredClone(current), ...(ground && !groundBusy ? { ground: structuredClone(ground) } : {}) };
    };
    const clearGround = () => { ground = groundSources = null; groundEpoch++; };
    const freeProbe = () => {
      const owned = probe;
      probe = null;
      if (owned) { try { owned.free(); } catch {} }
    };
    const closeSocket = () => {
      const owned = socket;
      socket = null;
      if (!owned) return;
      transportClosed = true;
      for (const handler of ["onopen", "onmessage", "onerror", "onclose"]) { try { owned[handler] = null; } catch {} }
      try { owned.close(); } catch {}
    };
    const close = (error = null) => {
      if (phase === "closed") return;
      const pending = phase === "pending";
      phase = "closed";
      clearGround();
      if (current) current = { ...current, accepted: false, closed: true, phase: "closed", error };
      clearTimeout(exchangeTimer);
      clearTimeout(lifetimeTimer);
      try { signal?.removeEventListener("abort", abort); } catch {}
      closeSocket();
      freeProbe();
      if (pending) reject(new Error(error ?? UNAVAILABLE));
    };
    const abort = () => close(CANCELLED);
    const check = () => {
      if (signal?.aborted) close(CANCELLED);
      if (phase === "pending" && performance.now() >= deadline) close(TIMED_OUT);
      if (phase !== "pending") throw new Error(UNAVAILABLE);
    };
    const maintain = () => {
      if (phase === "closed") return;
      try {
        if (signal?.aborted) close(CANCELLED);
        if (phase === "retained" && performance.now() >= retentionDeadline) close();
      } catch { close(UNAVAILABLE); }
    };
    const retained = () => {
      maintain();
      if (phase !== "retained") throw new Error("world terrain owner is closed");
    };
    const dependencies = (sources) => {
      for (const source of sources) {
        retained();
        const state = source?.state;
        retained();
        if (typeof state !== "function") throw new Error(GROUND_UNAVAILABLE);
        const report = state.call(source);
        retained();
        if (field(report, "accepted") !== true || field(report, "closed") !== false) throw new Error(GROUND_UNAVAILABLE);
      }
      if (worldFloorsRetained(sources[0]) !== true || worldMaterialsRetained(sources[1]) !== true) throw new Error(GROUND_UNAVAILABLE);
      retained();
    };
    const maintainGround = () => {
      if (!ground) return;
      if (groundChecking) { clearGround(); return; }
      groundChecking = true;
      try { dependencies(groundSources); }
      catch { clearGround(); }
      finally { groundChecking = false; }
    };
    const sample = (method, args) => {
      retained();
      let value;
      try { value = probe[method](...args); }
      catch { retained(); throw new Error("world terrain sample unavailable"); }
      retained();
      if (value !== null && !bounded(value, -0x80000000, 0x7fffffff)) throw new Error("world terrain sample unavailable");
      return value;
    };
    const owner = Object.freeze({
      state: snapshot,
      height(region, plane, x, y) {
        if (!bounded(region, 0, 65535) || !prepared.regionIds.includes(region) || !bounded(plane, 0, 3) || !bounded(x, 0, 63) || !bounded(y, 0, 63)) throw new Error("invalid world terrain coordinates");
        return sample("height", [region, plane, x, y]);
      },
      sceneHeight(plane, x, y) {
        if (!bounded(plane, 0, 3) || !bounded(x, 0, prepared.mapSize) || !bounded(y, 0, prepared.mapSize)) throw new Error("invalid world terrain coordinates");
        return sample("sceneHeight", [plane, x, y]);
      },
      sceneTile(plane, x, y) {
        if (!bounded(plane, 0, 3) || !bounded(x, 0, prepared.mapSize - 1) || !bounded(y, 0, prepared.mapSize - 1)) throw new Error("invalid world terrain coordinates");
        retained();
        let value;
        try { value = probe.sceneTile(plane, x, y); }
        catch {
          retained();
          throw new Error("world terrain sample unavailable");
        }
        retained();
        try {
          const tile = sceneTileSnapshot(value);
          retained();
          return tile;
        } catch { retained(); throw new Error("world terrain sample unavailable"); }
      },
      ...(floors ? { paletteColor(plane, x, y) {
        if (!bounded(plane, 0, 3) || !bounded(x, 0, prepared.mapSize - 1) || !bounded(y, 0, prepared.mapSize - 1)) throw new Error("invalid world terrain coordinates");
        return sample("paletteColor", [plane, x, y]);
      } } : {}),
      prepareGround(floorOwner, materials, plane, maxVertices) {
        retained();
        if (arguments.length < 2 || arguments.length > 4) throw new Error("invalid world ground preparation");
        if (arguments.length < 3) plane = 0;
        if (arguments.length < 4) maxVertices = MAX_GROUND_VERTICES;
        if (!bounded(plane, 0, 3) || !bounded(maxVertices, 0, MAX_GROUND_VERTICES)) throw new Error("invalid world ground preparation");
        if (groundBusy || floorOwner !== floors || !floors) throw new Error(GROUND_UNAVAILABLE);
        groundBusy = true;
        let preparedGround = false;
        let borrowing = true;
        let borrowEntered = false;
        let borrower;
        try {
          maintainGround();
          const epoch = groundEpoch;
          const sources = [floorOwner, materials];
          dependencies(sources);
          if (typeof probe.prepareGround !== "function" || typeof probe.groundTile !== "function") throw new Error(GROUND_UNAVAILABLE);
          retained();
          borrower = Object.freeze({
            prepareGround(borrowedFloors, borrowedMaterials, borrowedPlane, borrowedMaxVertices) {
              retained();
              if (!borrowing || borrowEntered || arguments.length !== 4 || borrowedPlane !== plane || borrowedMaxVertices !== maxVertices || epoch !== groundEpoch) throw new Error(GROUND_UNAVAILABLE);
              borrowEntered = true;
              dependencies(sources);
              const method = probe.prepareGround;
              retained();
              dependencies(sources);
              if (typeof method !== "function" || epoch !== groundEpoch) throw new Error(GROUND_UNAVAILABLE);
              const value = method.call(probe, borrowedFloors, borrowedMaterials, plane, maxVertices);
              preparedGround = true;
              retained();
              dependencies(sources);
              return value;
            },
          });
          GROUND_BORROWERS.set(borrower, borrower.prepareGround);
          const value = prepareWorldFloorGround(floorOwner, borrower, materials, plane, maxVertices);
          if (!preparedGround) throw new Error(GROUND_UNAVAILABLE);
          retained();
          dependencies(sources);
          const summary = groundSummary(value, prepared, plane, maxVertices);
          retained();
          dependencies(sources);
          if (epoch !== groundEpoch) throw new Error(GROUND_UNAVAILABLE);
          groundEpoch++;
          ground = summary;
          groundSources = sources;
          return summary;
        } catch {
          if (preparedGround) clearGround();
          maintainGround();
          retained();
          throw new Error(GROUND_UNAVAILABLE);
        } finally { borrowing = false; GROUND_BORROWERS.delete(borrower); groundBusy = false; }
      },
      groundState() {
        if (arguments.length) throw new Error("invalid world ground query");
        maintain();
        maintainGround();
        return groundBusy ? null : ground;
      },
      lowerGround(materials, rgb, underwaterOwner) {
        retained();
        if (arguments.length < 2 || arguments.length > 3 || !rgb || typeof rgb !== "object") throw new Error(GROUND_LOWERING_UNAVAILABLE);
        maintainGround();
        if (groundBusy || !ground || !groundSources || !groundSources.includes(materials)) throw new Error(GROUND_LOWERING_UNAVAILABLE);
        try {
          const lit = arguments.length === 3;
          const rawUnderwater = lit ? UNDERWATER_PROBES.get(underwaterOwner) : null;
          if (lit && !rawUnderwater) throw new Error();
          const method = lit ? probe.lowerGroundLit : probe.lowerGround;
          if (typeof method !== "function") throw new Error();
          const text = lendWorldMaterialProbe(materials, (rawMaterials) => (lit ? method.call(probe, rgb, rawMaterials, rawUnderwater()) : method.call(probe, rgb, rawMaterials)));
          retained();
          if (typeof text !== "string" || text.length > 64 * 1024) throw new Error();
          const report = JSON.parse(text);
          if (report?.scope !== "textured-ground-lowering" || report.plane !== ground.plane || report.terrainLowered !== true || report.renderedWorld !== false) throw new Error();
          return report;
        } catch { retained(); throw new Error(GROUND_LOWERING_UNAVAILABLE); }
      },
      addShadowLocs(regionId, data, baseX, baseY) {
        retained();
        if (arguments.length !== 4 || !(data instanceof Uint8Array) || data.length > MAX_MESSAGE) throw new Error(GROUND_LOWERING_UNAVAILABLE);
        try { return probe.addShadowLocs(regionId, data, baseX, baseY); }
        catch { retained(); throw new Error(GROUND_LOWERING_UNAVAILABLE); }
      },
      addShadowLocType(id, data) {
        retained();
        if (arguments.length !== 2 || !(data instanceof Uint8Array) || data.length > MAX_MESSAGE) throw new Error(GROUND_LOWERING_UNAVAILABLE);
        try { probe.addShadowLocType(id, data); }
        catch { retained(); throw new Error(GROUND_LOWERING_UNAVAILABLE); }
      },
      addShadowLocModel(id, data) {
        retained();
        if (arguments.length !== 2 || !(data instanceof Uint8Array) || data.length > MAX_MESSAGE) throw new Error(GROUND_LOWERING_UNAVAILABLE);
        try { probe.addShadowLocModel(id, data); }
        catch { retained(); throw new Error(GROUND_LOWERING_UNAVAILABLE); }
      },
      bakeShadows(texturesEnabled) {
        retained();
        if (arguments.length !== 1 || typeof texturesEnabled !== "boolean") throw new Error(GROUND_LOWERING_UNAVAILABLE);
        try { return JSON.parse(probe.bakeShadows(texturesEnabled)); }
        catch { retained(); throw new Error(GROUND_LOWERING_UNAVAILABLE); }
      },
      loweredMaterial(index) {
        retained();
        if (arguments.length !== 1 || !bounded(index, 0, 4096)) throw new Error(GROUND_LOWERING_UNAVAILABLE);
        try {
          const vertices = probe.loweredVertices(index);
          const indices = probe.loweredIndices(index);
          if (!(vertices instanceof Uint8Array) || !(indices instanceof Uint32Array)) throw new Error();
          return { vertices, indices };
        } catch { retained(); throw new Error(GROUND_LOWERING_UNAVAILABLE); }
      },
      groundTile(plane, x, y) {
        retained();
        if (arguments.length !== 3 || !bounded(plane, 0, 3) || !bounded(x, 0, prepared.mapSize - 1) || !bounded(y, 0, prepared.mapSize - 1)) throw new Error("invalid world ground coordinates");
        maintainGround();
        if (groundBusy || !ground || ground.plane !== plane) throw new Error(GROUND_TILE_UNAVAILABLE);
        const epoch = groundEpoch;
        try {
          const method = probe.groundTile;
          retained();
          if (typeof method !== "function") throw new Error(GROUND_TILE_UNAVAILABLE);
          const value = method.call(probe, plane, x, y);
          retained();
          maintainGround();
          if (!ground || epoch !== groundEpoch) throw new Error(GROUND_TILE_UNAVAILABLE);
          const tile = groundTileSnapshot(value, x, y);
          retained();
          maintainGround();
          if (!ground || epoch !== groundEpoch) throw new Error(GROUND_TILE_UNAVAILABLE);
          return tile;
        } catch {
          maintainGround();
          retained();
          throw new Error(GROUND_TILE_UNAVAILABLE);
        }
      },
      close: () => close(),
    });
    const send = () => {
      check();
      let bytes;
      try {
        bytes = probe.outgoing();
        check();
        if (!(bytes instanceof Uint8Array) || bytes.length > MAX_MESSAGE) throw new Error(UNAVAILABLE);
        if (bytes.length) socket.send(bytes);
        check();
      } finally { wipe(bytes); }
    };
    const complete = () => {
      check();
      const ready = probe.complete();
      check();
      if (ready === false) return;
      if (ready !== true) throw new Error(UNAVAILABLE);
      probe.finish();
      check();
      if (underwater) {
        const summary = jsonReport(probe.composeUnderwater(prepared.base.x, prepared.base.y, prepared.mapSize));
        check();
        if (summary.scope !== "underwater-surface" || summary.mapSize !== prepared.mapSize || summary.renderedWorld !== false) throw new Error(UNAVAILABLE);
        const regionsPresent = integer(summary.presentRegions, 0, prepared.regionIds.length);
        const underwaterRegions = underwaterReport(probe.report(), prepared.regionIds);
        check();
        closeSocket();
        check();
        current = { accepted: true, closed: false, phase: "ready", scope: "underwater-surface", effectsDecoded: false, renderedWorld: false, authenticated: false, receivedBytes: received, report: underwaterRegions, presentRegions: regionsPresent, error: null };
        phase = "retained";
        clearTimeout(exchangeTimer);
        retentionDeadline = performance.now() + lifetimeMs;
        lifetimeTimer = setTimeout(() => close(), lifetimeMs);
        const handle = Object.freeze({ state: snapshot, close: () => close() });
        UNDERWATER_PROBES.set(handle, () => {
          retained();
          if (!probe || regionsPresent < 1) throw new Error(GROUND_LOWERING_UNAVAILABLE);
          return probe;
        });
        resolve(handle);
        return;
      }
      const surface = surfaceSummary(probe.compose(prepared.base.x, prepared.base.y, prepared.mapSize), prepared);
      check();
      const report = terrainReport(probe.report(), prepared.regionIds, received);
      check();
      let palette;
      if (floors) {
        const result = record(composeWorldFloorPalette(floors, probe));
        check();
        const tileCount = 4 * prepared.mapSize * prepared.mapSize;
        if (result.scope !== "underlay-packed-hsl" || result.mapSize !== prepared.mapSize || result.planes !== 4 || result.tileCount !== tileCount || result.rgbPrepared !== false || result.renderedWorld !== false || !Array.isArray(result.unavailableDefinitionIds) || result.unavailableDefinitionIds.length > 255) throw new Error(UNAVAILABLE);
        const knownColors = integer(result.knownColors, 0, tileCount);
        const unavailableColors = integer(result.unavailableColors, 0, tileCount);
        if (knownColors + unavailableColors !== tileCount) throw new Error(UNAVAILABLE);
        const unavailableDefinitionIds = result.unavailableDefinitionIds.map((id, at, ids) => {
          integer(id, 0, 254);
          if (at && id <= ids[at - 1]) throw new Error(UNAVAILABLE);
          return id;
        });
        palette = { scope: "underlay-packed-hsl", mapSize: prepared.mapSize, planes: 4, tileCount, knownColors, unavailableColors, unavailableDefinitionIds, rgbPrepared: false, renderedWorld: false };
      }
      check();
      closeSocket();
      check();
      current = { accepted: true, closed: false, phase: "ready", scope: "surface-only", effectsDecoded: false, renderedWorld: false, authenticated: false, receivedBytes: received, report, surface, ...(palette ? { palette } : {}), error: null };
      phase = "retained";
      clearTimeout(exchangeTimer);
      retentionDeadline = performance.now() + lifetimeMs;
      lifetimeTimer = setTimeout(() => close(), lifetimeMs);
      resolve(owner);
    };
    try {
      exchangeTimer = setTimeout(() => close(TIMED_OUT), timeoutMs);
      signal?.addEventListener("abort", abort, { once: true });
    } catch { close(UNAVAILABLE); return; }
    Promise.resolve().then(() => {
      check();
      return load();
    }).then((Probe) => {
      try {
        check();
        if (underwater) {
          if (typeof Probe.underwater !== "function") throw new Error(UNAVAILABLE);
          probe = Probe.underwater(PUBLIC_LOGIN_DEFAULTS.js5AuthToken, JSON.stringify(prepared.regionIds));
        } else {
          probe = new Probe(PUBLIC_LOGIN_DEFAULTS.js5AuthToken, JSON.stringify(prepared.regionIds));
        }
        if (phase === "closed") { freeProbe(); return; }
        check();
        const required = underwater
          ? ["outgoing", "feed", "complete", "finish", "composeUnderwater", "report", "free"]
          : ["outgoing", "feed", "complete", "finish", "compose", "report", "height", "sceneHeight", "sceneTile", "free", ...(floors ? ["composePalette", "paletteColor"] : [])];
        for (const method of required) {
          if (typeof probe[method] !== "function") throw new Error(UNAVAILABLE);
          check();
        }
        socket = new WebSocketClass(url);
        if (phase === "closed") { closeSocket(); return; }
        check();
        socket.binaryType = "arraybuffer";
        check();
        socket.onopen = () => {
          if (phase !== "pending" || transportClosed) return;
          try { send(); } catch { close(UNAVAILABLE); }
        };
        socket.onmessage = (event) => {
          let data;
          let bytes;
          try {
            data = event?.data;
            if (data instanceof ArrayBuffer) bytes = new Uint8Array(data);
            if (phase !== "pending" || transportClosed) return;
            check();
            if (!bytes || bytes.length > MAX_MESSAGE || received + bytes.length > MAX_RECEIVED) throw new Error(UNAVAILABLE);
            received += bytes.length;
            probe.feed(bytes);
            check();
            send();
            complete();
          } catch { if (phase === "pending" && !transportClosed) close(UNAVAILABLE); }
          finally { wipe(bytes ?? data); }
        };
        socket.onerror = () => { if (phase === "pending" && !transportClosed) close(UNAVAILABLE); };
        socket.onclose = () => { if (phase === "pending" && !transportClosed) close(UNAVAILABLE); };
        check();
      } catch { close(UNAVAILABLE); }
    }, () => close(UNAVAILABLE));
  });
}
