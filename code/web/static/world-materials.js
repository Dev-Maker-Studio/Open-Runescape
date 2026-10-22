import { loadRuntime } from "./js5-probe.js";
import { PUBLIC_LOGIN_DEFAULTS } from "./login-config.js";
import { invokeGroundBorrower } from "./world-terrain.js";

const OWNERS = new WeakMap();

export function worldMaterialsRetained(owner) {
  const entry = OWNERS.get(owner);
  return entry ? entry.live() : false;
}

export function lendWorldMaterialProbe(owner, use) {
  const entry = OWNERS.get(owner);
  if (!entry || typeof use !== "function") throw new Error("world material lending unavailable");
  return entry.lend(use);
}

export function prepareWorldMaterialGround(owner, borrower, floorProbe, plane, maxVertices) {
  const entry = OWNERS.get(owner);
  if (!entry) throw new Error("world ground preparation unavailable");
  return entry.prepare(borrower, floorProbe, plane, maxVertices);
}

const MIB = 1024 * 1024;
const MAX_MESSAGE = 4 * MIB;
const MAX_RECEIVED = 12 * MIB;
const MAX_EXPANDED = 8 * MIB;
const MAX_SLOTS = 65535;
const MAX_DEFINITION_BYTES = 2 + MAX_SLOTS * 24;
const UNAVAILABLE = "verified world material metadata unavailable";
const CANCELLED = "world material metadata cancelled";
const TIMED_OUT = "world material metadata timed out";
const CLOSED = "world material metadata owner is closed";
const MATERIAL_UNAVAILABLE = "world material metadata record unavailable";

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

function materialReport(value) {
  const report = jsonReport(value);
  if (report.scope !== "material-metadata" || report.texturePixelsDecoded !== false || report.renderedWorld !== false) throw new Error(UNAVAILABLE);
  const asset = record(report.asset);
  if (asset.revision !== 727 || asset.subrevision !== 1 || asset.prefetchCount !== 27 || asset.index !== 26 || asset.archive !== 0 || asset.referenceCrcVerified !== true || asset.referenceWhirlpoolVerified !== true || asset.archiveCrcVerified !== true || asset.authenticated !== false || asset.fileSlots !== 1 || !Array.isArray(asset.files) || asset.files.length !== 1) throw new Error(UNAVAILABLE);
  const file = record(asset.files[0]);
  if (file.id !== 0 || file.script !== null) throw new Error(UNAVAILABLE);
  const slots = integer(report.slots, 0, MAX_SLOTS);
  const present = integer(report.present, 0, slots);
  const bytes = integer(file.bytes, 2, MAX_DEFINITION_BYTES);
  const expandedBytes = integer(asset.expandedBytes, 2, MAX_EXPANDED);
  if (bytes !== 2 + slots + 23 * present || expandedBytes !== bytes) throw new Error(UNAVAILABLE);
  return {
    scope: "material-metadata",
    asset: {
      revision: 727, subrevision: 1, prefetchCount: 27, index: 26, archive: 0,
      referenceRevision: integer(asset.referenceRevision, -0x80000000, 0x7fffffff),
      archiveRevision: integer(asset.archiveRevision, -0x80000000, 0x7fffffff),
      referenceCrcVerified: true, referenceWhirlpoolVerified: true,
      archiveCrcVerified: true, archiveWhirlpoolVerified: boolean(asset.archiveWhirlpoolVerified),
      authenticated: false, containerBytes: integer(asset.containerBytes, 5, MAX_MESSAGE),
      expandedBytes, fileSlots: 1, files: [{ id: 0, bytes, script: null }],
    },
    slots, present, texturePixelsDecoded: false, renderedWorld: false,
  };
}

function materialRecord(value, id) {
  const report = jsonReport(value);
  if (report.id !== id) throw new Error(UNAVAILABLE);
  return Object.freeze({
    id: report.id,
    detailsOnly: boolean(report.detailsOnly), isSmall: boolean(report.isSmall), skipTriangles: boolean(report.skipTriangles),
    brightness: integer(report.brightness, -128, 127), alpha: integer(report.alpha, -128, 127),
    effectId: integer(report.effectId, -128, 127), effectParam1: integer(report.effectParam1, -128, 127),
    colorHsl: integer(report.colorHsl, -32768, 32767),
    textureSpeedU: integer(report.textureSpeedU, -128, 127), textureSpeedV: integer(report.textureSpeedV, -128, 127),
    aBool2087: boolean(report.aBool2087), isBrickTile: boolean(report.isBrickTile),
    mipmapping: integer(report.mipmapping, -128, 127),
    repeatS: boolean(report.repeatS), repeatT: boolean(report.repeatT), hdr: boolean(report.hdr),
    combineMode: integer(report.combineMode, 0, 255),
    effectParam2: integer(report.effectParam2, -0x80000000, 0x7fffffff),
    effectCombiner: integer(report.effectCombiner, 0, 255),
  });
}

function wipe(value) {
  try { if (ArrayBuffer.isView(value)) new Uint8Array(value.buffer, value.byteOffset, value.byteLength).fill(0); }
  catch {}
}

export async function openWorldMaterials(options = {}) {
  let url;
  let signal;
  let load;
  let WebSocketClass;
  let timeoutMs;
  let lifetimeMs;
  let preCancelled = false;
  try {
    let js5Relay;
    ({ js5Relay, signal, load = async () => (await loadRuntime()).Js5WorldMaterialProbe, WebSocketClass = globalThis.WebSocket, timeoutMs = 30000, lifetimeMs = 120000 } = record(options));
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
    const invoke = (name, ...args) => {
      check();
      const owned = probe;
      const method = owned?.[name];
      check();
      if (typeof method !== "function") throw new Error(UNAVAILABLE);
      const value = method.call(owned, ...args);
      check();
      return value;
    };
    const owner = Object.freeze({
      state: () => { maintain(); return structuredClone(current); },
      material(id) {
        retained();
        if (!bounded(id, 0, 65535)) throw new Error("invalid world material id");
        if (id >= current.report.slots) return null;
        let value;
        let owned;
        let method;
        try {
          owned = probe;
          method = owned?.material;
          retained();
          if (typeof method !== "function") throw new Error(MATERIAL_UNAVAILABLE);
        } catch {
          retained();
          throw new Error(MATERIAL_UNAVAILABLE);
        }
        try {
          value = method.call(owned, id);
          retained();
        } catch (error) {
          retained();
          if (error === `material not found: ${id}` && current.report.present < current.report.slots) return null;
          throw new Error(MATERIAL_UNAVAILABLE);
        }
        try {
          if (current.report.present === 0) throw new Error(MATERIAL_UNAVAILABLE);
          const safe = materialRecord(value, id);
          retained();
          return safe;
        } catch {
          retained();
          throw new Error(MATERIAL_UNAVAILABLE);
        }
      },
      close: () => close(),
    });
    OWNERS.set(owner, {
      live: () => { maintain(); return phase === "retained"; },
      lend(use) {
        retained();
        try {
          const value = use(probe);
          retained();
          return value;
        } catch {
          retained();
          throw new Error("world material lending unavailable");
        }
      },
      prepare(borrower, floorProbe, plane, maxVertices) {
        retained();
        try {
          if (arguments.length !== 4 || !bounded(plane, 0, 3) || !bounded(maxVertices, 0, 1016064)) throw new Error();
          const value = invokeGroundBorrower(borrower, floorProbe, probe, plane, maxVertices);
          retained();
          return value;
        } catch {
          retained();
          throw new Error("world ground preparation unavailable");
        }
      },
    });
    const send = () => {
      let bytes;
      try {
        check();
        const ownedProbe = probe;
        const outgoing = ownedProbe?.outgoing;
        check();
        if (typeof outgoing !== "function") throw new Error(UNAVAILABLE);
        bytes = outgoing.call(ownedProbe);
        check();
        if (!(bytes instanceof Uint8Array) || bytes.length > MAX_MESSAGE) throw new Error(UNAVAILABLE);
        if (bytes.length) {
          const owned = socket;
          const method = owned?.send;
          check();
          if (typeof method !== "function") throw new Error(UNAVAILABLE);
          method.call(owned, bytes);
          check();
        }
      } finally { wipe(bytes); }
    };
    const complete = () => {
      const ready = invoke("complete");
      if (ready === false) return;
      if (ready !== true) throw new Error(UNAVAILABLE);
      invoke("finish");
      const report = materialReport(invoke("report"));
      check();
      closeSocket();
      check();
      current = { accepted: true, closed: false, phase: "ready", scope: "material-metadata-only", authenticated: false, texturePixelsDecoded: false, renderedWorld: false, receivedBytes: received, report, error: null };
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
        for (const method of ["outgoing", "feed", "complete", "finish", "report", "material", "free"]) {
          const value = probe[method];
          check();
          if (typeof value !== "function") throw new Error(UNAVAILABLE);
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
            invoke("feed", bytes);
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
