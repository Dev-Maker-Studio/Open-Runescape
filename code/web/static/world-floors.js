import { loadRuntime } from "./js5-probe.js";
import { PUBLIC_LOGIN_DEFAULTS } from "./login-config.js";
import { prepareWorldMaterialGround } from "./world-materials.js";

const OWNERS = new WeakMap();

export function worldFloorsRetained(owner) {
  const entry = OWNERS.get(owner);
  return entry ? entry.live() : false;
}

export function composeWorldFloorPalette(owner, terrainProbe) {
  const entry = OWNERS.get(owner);
  if (!entry) throw new Error("world floor palette unavailable");
  return entry.compose(terrainProbe);
}

export function prepareWorldFloorGround(owner, borrower, materials, plane, maxVertices) {
  const entry = OWNERS.get(owner);
  if (!entry) throw new Error(GROUND_UNAVAILABLE);
  return entry.prepare(borrower, materials, plane, maxVertices);
}

const MIB = 1024 * 1024;
const MAX_MESSAGE = 4 * MIB;
const MAX_RECEIVED = 20 * MIB;
const UNAVAILABLE = "verified world floor definitions unavailable";
const CANCELLED = "world floor definitions cancelled";
const TIMED_OUT = "world floor definitions timed out";
const CLOSED = "world floor definitions owner is closed";
const DEFINITION_UNAVAILABLE = "world floor definition unavailable";
const PALETTE_UNAVAILABLE = "world floor palette unavailable";
const GROUND_UNAVAILABLE = "world ground preparation unavailable";
const OPERATION_UNIT = "wire/container/expanded/split/definition bytes and bounded metadata records";
const LIMITS = {
  maxReferenceArchives: 65536,
  maxReferenceFiles: 262144,
  maxDefinitionsPerArchive: 65536,
  maxTotalDefinitions: 131072,
  maxFloorId: 65535,
  maxDefinitionBytes: 4096,
  maxTagsPerDefinition: 256,
  maxContainerBytes: MAX_MESSAGE,
  maxExpandedContainerBytes: 8 * MIB,
  maxTotalWireBytes: MAX_RECEIVED,
  maxTotalContainerBytes: 16 * MIB,
  maxTotalExpandedBytes: 24 * MIB,
  maxTotalOperations: 128 * MIB,
};
const UNDERLAY_DEFAULTS = {
  rgb: 0, texture: -1, scale: 512, shadowed: true, occlude: true,
  hue: 0, saturation: 0, lightness: 0, hueMultiplier: 0,
};
const OVERLAY_DEFAULTS = {
  tileColorHsl: 0, minimapColorHsl: -1, texture: -1, textureScale: 512,
  occlude: true, shadowed: true, blendsWithUnderlay: false,
  waterColor: 1190717, waterFogDepth: 512, waterIntensity: 255,
  opcode20: 63, unusedOpcode21: 0, unusedOpcode22: 64, sawOpcode8: false,
};

function bounded(value, min, max) {
  return Number.isSafeInteger(value) && value >= min && value <= max;
}

function integer(value, min, max) {
  if (!bounded(value, min, max)) throw new Error(UNAVAILABLE);
  return value;
}

function boolean(value) {
  if (typeof value !== "boolean") throw new Error(UNAVAILABLE);
  return value;
}

function record(value) {
  if (!value || typeof value !== "object" || Array.isArray(value) || ![Object.prototype, null].includes(Object.getPrototypeOf(value))) throw new Error(UNAVAILABLE);
  return value;
}

function relayUrl(relay) {
  if (typeof relay !== "string" || relay.length > 512 || !/^wss?:\/\/(?:127\.0\.0\.1|localhost|\[::1\])(?::[0-9]{1,5})?\/js5$/.test(relay)) throw new Error(UNAVAILABLE);
  const url = new URL(relay);
  if (!["ws:", "wss:"].includes(url.protocol) || !["127.0.0.1", "localhost", "[::1]"].includes(url.hostname) || url.pathname !== "/js5" || url.username || url.password || url.search || url.hash || url.port === "0") throw new Error(UNAVAILABLE);
  return url.href;
}

function jsonReport(value) {
  if (typeof value !== "string" || value.length > 64 * 1024) throw new Error(UNAVAILABLE);
  return record(JSON.parse(value));
}

function archiveReport(value, archive) {
  const report = record(value);
  if (report.archive !== archive || report.crcVerified !== true) throw new Error(UNAVAILABLE);
  const safe = {
    archive,
    archiveRevision: integer(report.archiveRevision, -0x80000000, 0x7fffffff),
    containerBytes: integer(report.containerBytes, 5, MAX_MESSAGE),
    expandedBytes: integer(report.expandedBytes, 0, LIMITS.maxExpandedContainerBytes),
    fileSlots: integer(report.fileSlots, 0, LIMITS.maxDefinitionsPerArchive),
    populatedDefinitions: integer(report.populatedDefinitions, 0, LIMITS.maxDefinitionsPerArchive),
    definitionBytes: integer(report.definitionBytes, 0, LIMITS.maxExpandedContainerBytes),
    crcVerified: true,
    whirlpoolVerified: boolean(report.whirlpoolVerified),
  };
  if (safe.populatedDefinitions > safe.fileSlots || (safe.fileSlots === 0) !== (safe.populatedDefinitions === 0) || safe.definitionBytes < safe.populatedDefinitions || safe.definitionBytes > safe.populatedDefinitions * LIMITS.maxDefinitionBytes || safe.definitionBytes > safe.expandedBytes) throw new Error(UNAVAILABLE);
  return safe;
}

function floorReport(value, received) {
  const report = jsonReport(value);
  if (report.revision !== 727 || report.subrevision !== 1 || report.prefetchCount !== 27 || report.index !== 2 || report.referenceCrcVerified !== true || report.referenceWhirlpoolVerified !== true || report.authenticated !== false || report.renderedWorld !== false || report.opcode8SideEffectsApplied !== false) throw new Error(UNAVAILABLE);
  const limits = record(report.limits);
  const safeLimits = {};
  for (const [key, expected] of Object.entries(LIMITS)) {
    if (limits[key] !== expected) throw new Error(UNAVAILABLE);
    safeLimits[key] = expected;
  }
  if (limits.operationUnit !== OPERATION_UNIT) throw new Error(UNAVAILABLE);
  safeLimits.operationUnit = OPERATION_UNIT;
  const underlays = archiveReport(report.underlays, 1);
  const overlays = archiveReport(report.overlays, 4);
  const safe = {
    revision: 727, subrevision: 1, prefetchCount: 27, index: 2,
    referenceRevision: integer(report.referenceRevision, -0x80000000, 0x7fffffff),
    referenceArchives: integer(report.referenceArchives, 2, LIMITS.maxReferenceArchives),
    referenceFiles: integer(report.referenceFiles, 0, LIMITS.maxReferenceFiles),
    referenceCrcVerified: true, referenceWhirlpoolVerified: true,
    authenticated: false, renderedWorld: false, opcode8SideEffectsApplied: false,
    totalWireBytes: integer(report.totalWireBytes, 0, MAX_RECEIVED),
    totalContainerBytes: integer(report.totalContainerBytes, 0, LIMITS.maxTotalContainerBytes),
    totalExpandedBytes: integer(report.totalExpandedBytes, 0, LIMITS.maxTotalExpandedBytes),
    totalOperations: integer(report.totalOperations, 0, LIMITS.maxTotalOperations),
    underlays, overlays, limits: safeLimits,
  };
  const definitions = underlays.populatedDefinitions + overlays.populatedDefinitions;
  if (definitions > LIMITS.maxTotalDefinitions || definitions > safe.referenceFiles || underlays.containerBytes + overlays.containerBytes > safe.totalContainerBytes || underlays.expandedBytes + overlays.expandedBytes > safe.totalExpandedBytes || safe.totalWireBytes !== received) throw new Error(UNAVAILABLE);
  return safe;
}

function definition(value, id, archive, overlay) {
  const report = jsonReport(value);
  if (report.id !== id || typeof report.present !== "boolean") throw new Error(UNAVAILABLE);
  if (report.present ? id >= archive.fileSlots : id < archive.fileSlots && (id === archive.fileSlots - 1 || archive.populatedDefinitions === archive.fileSlots)) throw new Error(UNAVAILABLE);
  const safe = overlay ? {
    id, present: report.present,
    overlayId: integer(report.overlayId, id, id),
    slot: integer(report.slot, 0, 65535),
    tileColorHsl: integer(report.tileColorHsl, -1, 65535),
    minimapColorHsl: integer(report.minimapColorHsl, -1, 65535),
    texture: integer(report.texture, -1, 65534),
    textureScale: integer(report.textureScale, 0, 262140),
    occlude: boolean(report.occlude), shadowed: boolean(report.shadowed),
    blendsWithUnderlay: boolean(report.blendsWithUnderlay),
    waterColor: integer(report.waterColor, 0, 0xffffff),
    waterFogDepth: integer(report.waterFogDepth, 0, 8160),
    waterIntensity: integer(report.waterIntensity, 0, 255),
    opcode20: integer(report.opcode20, 0, 65535),
    unusedOpcode21: integer(report.unusedOpcode21, 0, 255),
    unusedOpcode22: integer(report.unusedOpcode22, 0, 65535),
    sawOpcode8: boolean(report.sawOpcode8),
  } : {
    id, present: report.present,
    rgb: integer(report.rgb, 0, 0xffffff),
    texture: integer(report.texture, -1, 65534),
    scale: integer(report.scale, 0, 262140),
    shadowed: boolean(report.shadowed), occlude: boolean(report.occlude),
    hue: integer(report.hue, -255, 255),
    saturation: integer(report.saturation, 0, 255),
    lightness: integer(report.lightness, 0, 255),
    hueMultiplier: integer(report.hueMultiplier, 0, 255),
  };
  if (overlay ? safe.textureScale % 4 || safe.waterFogDepth % 32 || (safe.slot & id) !== id || (safe.slot & 255) !== (id & 255) : safe.scale % 4) throw new Error(UNAVAILABLE);
  if (!safe.present) {
    const defaults = overlay ? { ...OVERLAY_DEFAULTS, overlayId: id, slot: (8 << 8) | id } : UNDERLAY_DEFAULTS;
    for (const [key, expected] of Object.entries(defaults)) if (safe[key] !== expected) throw new Error(UNAVAILABLE);
  }
  return safe;
}

function paletteSummary(value) {
  const summary = jsonReport(value);
  if (summary.scope !== "underlay-packed-hsl" || ![72, 104, 120, 136, 168].includes(summary.mapSize) || summary.planes !== 4 || summary.tileCount !== 4 * summary.mapSize * summary.mapSize || summary.rgbPrepared !== false || summary.renderedWorld !== false || !Array.isArray(summary.unavailableDefinitionIds) || summary.unavailableDefinitionIds.length > 255) throw new Error(UNAVAILABLE);
  const tileCount = summary.tileCount;
  const knownColors = integer(summary.knownColors, 0, tileCount);
  const unavailableColors = integer(summary.unavailableColors, 0, tileCount);
  if (knownColors + unavailableColors !== tileCount) throw new Error(UNAVAILABLE);
  let previous = -1;
  const unavailableDefinitionIds = summary.unavailableDefinitionIds.map((value) => {
    const id = integer(value, 0, 254);
    if (id <= previous) throw new Error(UNAVAILABLE);
    previous = id;
    return id;
  });
  return {
    scope: "underlay-packed-hsl", mapSize: summary.mapSize, planes: 4, tileCount,
    knownColors, unavailableColors, unavailableDefinitionIds, rgbPrepared: false, renderedWorld: false,
  };
}

function wipe(value) {
  try { if (ArrayBuffer.isView(value)) new Uint8Array(value.buffer, value.byteOffset, value.byteLength).fill(0); }
  catch {}
}

export async function openWorldFloors(options = {}) {
  let url;
  let signal;
  let load;
  let WebSocketClass;
  let timeoutMs;
  let lifetimeMs;
  let preCancelled = false;
  try {
    let js5Relay;
    ({ js5Relay, signal, load = async () => (await loadRuntime()).Js5WorldFloorProbe, WebSocketClass = globalThis.WebSocket, timeoutMs = 30000, lifetimeMs = 120000 } = record(options));
    if (signal != null && (typeof signal.aborted !== "boolean" || typeof signal.addEventListener !== "function" || typeof signal.removeEventListener !== "function")) throw new Error(UNAVAILABLE);
    preCancelled = signal?.aborted === true;
    if (preCancelled) throw new Error(CANCELLED);
    url = relayUrl(js5Relay);
    if (!bounded(timeoutMs, 1, 30000) || !bounded(lifetimeMs, 1, 300000) || typeof load !== "function" || typeof WebSocketClass !== "function") throw new Error(UNAVAILABLE);
  } catch {
    throw new Error(preCancelled ? CANCELLED : UNAVAILABLE);
  }
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
    const deadline = performance.now() + timeoutMs;
    const snapshot = () => { maintain(); return structuredClone(current); };
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
      if (phase !== "retained") throw new Error(CLOSED);
    };
    const lookup = (method, id) => {
      retained();
      if (!bounded(id, 0, LIMITS.maxFloorId)) throw new Error("invalid world floor definition id");
      try {
        const value = probe[method](id);
        retained();
        return definition(value, id, current.report[method === "overlay" ? "overlays" : "underlays"], method === "overlay");
      } catch {
        retained();
        throw new Error(DEFINITION_UNAVAILABLE);
      }
    };
    const composePalette = (terrainProbe) => {
      retained();
      try {
        const compose = terrainProbe?.composePalette;
        retained();
        if (typeof compose !== "function") throw new Error(PALETTE_UNAVAILABLE);
        const value = compose.call(terrainProbe, probe);
        retained();
        const summary = paletteSummary(value);
        retained();
        return summary;
      } catch {
        retained();
        throw new Error(PALETTE_UNAVAILABLE);
      }
    };
    const owner = Object.freeze({
      state: snapshot,
      underlay: (id) => lookup("underlay", id),
      overlay: (id) => lookup("overlay", id),
      composeTerrain: (terrainProbe) => composePalette(terrainProbe),
      close: () => close(),
    });
    OWNERS.set(owner, {
      live: () => { maintain(); return phase === "retained"; },
      compose: composePalette,
      prepare(borrower, materials, plane, maxVertices) {
        retained();
        try {
          if (arguments.length !== 4 || !bounded(plane, 0, 3) || !bounded(maxVertices, 0, 1016064)) throw new Error(GROUND_UNAVAILABLE);
          const value = prepareWorldMaterialGround(materials, borrower, probe, plane, maxVertices);
          retained();
          return value;
        } catch {
          retained();
          throw new Error(GROUND_UNAVAILABLE);
        }
      },
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
      const report = floorReport(probe.report(), received);
      check();
      closeSocket();
      check();
      current = { accepted: true, closed: false, phase: "ready", scope: "floor-definitions-only", authenticated: false, renderedWorld: false, opcode8SideEffectsApplied: false, receivedBytes: received, report, error: null };
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
        probe = new Probe(PUBLIC_LOGIN_DEFAULTS.js5AuthToken);
        if (phase === "closed") { freeProbe(); return; }
        check();
        for (const method of ["outgoing", "feed", "complete", "finish", "report", "underlay", "overlay", "free"]) {
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
