import { loadRuntime } from "./js5-probe.js";

const MAX_RECEIVED = 1024 * 1024;
const MAX_MESSAGE = 64 * 1024;

function relayUrl(relay, kind) {
  let url;
  try { url = new URL(relay); } catch { throw new Error(`exact loopback ${kind} relay URL required`); }
  if (!["ws:", "wss:"].includes(url.protocol) || !["127.0.0.1", "localhost", "[::1]"].includes(url.hostname) || url.pathname !== `/${kind}` || url.search || url.hash || url.username || url.password) {
    throw new Error(`exact loopback ${kind} relay URL required`);
  }
  return url.href;
}

function bounded(value, min, max) {
  return Number.isSafeInteger(value) && value >= min && value <= max;
}

function inventorySnapshot(text, keys, validate) {
  const value = JSON.parse(text);
  if (value === null) return null;
  if (typeof value !== "object" || Array.isArray(value) || Object.keys(value).sort().join(",") !== keys || !validate(value)) throw new Error("invalid original inventory result");
  return Object.freeze(value);
}

function playerStatsRecord(value, keys) {
  return value !== null && typeof value === "object" && !Array.isArray(value) && Object.keys(value).sort().join(",") === keys;
}

function playerStatsSnapshot(value) {
  if (!playerStatsRecord(value, "playerWeight,playerWeightUpdates,recentSkillUpdates,runEnergy,runEnergyUpdates,skillCount,skillUpdateCycle,statUpdates,updates") || value.skillCount !== 25 || ![value.updates, value.statUpdates, value.runEnergyUpdates, value.playerWeightUpdates].every((count) => bounded(count, 0, Number.MAX_SAFE_INTEGER)) || value.updates !== value.statUpdates + value.runEnergyUpdates + value.playerWeightUpdates || !bounded(value.skillUpdateCycle, -2147483648, 2147483647) || !bounded(value.runEnergy, 0, 255) || !bounded(value.playerWeight, -32768, 32767) || !Array.isArray(value.recentSkillUpdates) || value.recentSkillUpdates.length !== 32 || !value.recentSkillUpdates.every((id) => bounded(id, 0, 24))) throw new Error("invalid original player stats result");
  Object.freeze(value.recentSkillUpdates);
  return Object.freeze(value);
}

function skillSnapshot(value, id) {
  if (!playerStatsRecord(value, "baseLevel,experience,visibleLevel") || !bounded(value.experience, -2147483648, 2147483647) || !bounded(value.visibleLevel, 0, 255) || !bounded(value.baseLevel, 0, id === 24 ? 120 : 99)) throw new Error("invalid original skill result");
  return Object.freeze(value);
}

function playerStatsResult(text, validate) {
  if (typeof text !== "string" || text.length > MAX_MESSAGE) throw new Error("invalid original player stats result");
  return validate(JSON.parse(text));
}

function deadlines(timeoutMs, lifetimeMs) {
  if (!bounded(timeoutMs, 1, 30000) || !bounded(lifetimeMs, timeoutMs, 300000)) throw new Error("bounded lobby deadlines required");
}

function configBound(configJson) {
  if (typeof configJson !== "string" || configJson.length > MAX_MESSAGE) throw new Error("bounded private lobby configuration required");
}

async function runtimeWithin(load, timeoutMs, signal) {
  let timer;
  let abort;
  try {
    signal?.throwIfAborted();
    return await Promise.race([
      load(),
      new Promise((_, reject) => { timer = setTimeout(() => reject(new Error("original lobby runtime load timed out")), timeoutMs); }),
      new Promise((_, reject) => { abort = () => reject(new Error("original lobby cancelled")); signal?.addEventListener("abort", abort, { once: true }); }),
    ]);
  } finally { clearTimeout(timer); signal?.removeEventListener("abort", abort); }
}

export async function openLobbySession(relay, configJson, {
  load = async () => (await loadRuntime()).OriginalLobbySession,
  WebSocketClass = globalThis.WebSocket,
  timeoutMs = 15000,
  lifetimeMs = 120000,
  signal,
} = {}) {
  const url = relayUrl(relay, "lobby");
  deadlines(timeoutMs, lifetimeMs);
  configBound(configJson);
  let Session;
  try { Session = await runtimeWithin(load, timeoutMs, signal); }
  catch { throw new Error("original lobby runtime unavailable"); }
  let session;
  try { session = new Session(configJson); }
  catch { throw new Error("invalid private lobby configuration"); }
  configJson = null;
  return openSocketSession(url, session, { kind: "lobby", WebSocketClass, timeoutMs, lifetimeMs, signal });
}

function openSocketSession(url, session, { kind, WebSocketClass, timeoutMs, lifetimeMs, signal, varpCount, inventory = false, playerStats = false, scene = false, onReady = () => {} }) {
  if (signal?.aborted) {
    try { session.close(); } catch {}
    try { session.free(); } catch {}
    throw new Error(`original ${kind} cancelled`);
  }
  let socket;
  let done = false;
  let received = 0;
  let packets = 0;
  let worldStarted = false;
  let worldStarting = false;
  let flushTimer;
  let worldFlushPending = false;
  let authTimer;
  let lifetimeTimer;
  let tickTimer;
  let resolveReady;
  let rejectReady;
  let reachedReady = false;
  let current = { connected: false, phase: "sendHandshake", accepted: false, worldList: null, receivedBytes: 0, opaquePackets: 0, error: null };
  const label = `original ${kind}`;
  const readyKind = kind === "lobby" ? "loginAccepted" : "worldReady";
  const ready = new Promise((resolve, reject) => { resolveReady = resolve; rejectReady = reject; });
  ready.catch(() => {});
  const snapshot = () => {
    const value = structuredClone(current);
    if (!playerStats) return value;
    if (Object.hasOwn(value, "playerStats")) value.playerStats = playerStatsSnapshot(value.playerStats);
    return Object.freeze(value);
  };
  const update = () => {
    const text = session.report();
    if (playerStats && (done || typeof text !== "string" || text.length > MAX_MESSAGE)) throw new Error();
    const report = JSON.parse(text);
    if (playerStats) {
      if (report === null || typeof report !== "object" || Array.isArray(report)) throw new Error();
      const terminal = ["rejected", "queued", "retryRequested", "reconnectRequired"].includes(report.phase);
      if (terminal) {
        if (Object.hasOwn(report, "playerStats")) throw new Error();
      } else {
        report.playerStats = playerStatsSnapshot(report.playerStats);
      }
    }
    const connected = socket?.readyState === 1;
    if (playerStats && done) throw new Error();
    current = { ...report, connected, receivedBytes: received, opaquePackets: packets, error: null };
  };
  const close = (error = null) => {
    if (done) return;
    done = true;
    current = { ...current, connected: false, phase: error ? "failed" : "closed", accepted: false, error };
    delete current.variables;
    delete current.inventory;
    delete current.playerStats;
    clearTimeout(flushTimer);
    clearTimeout(authTimer);
    clearTimeout(lifetimeTimer);
    clearTimeout(tickTimer);
    signal?.removeEventListener("abort", abort);
    if (socket) {
      socket.onopen = socket.onmessage = socket.onerror = socket.onclose = null;
      try { socket.close(); } catch {}
    }
    const closing = session;
    session = null;
    try { closing.close(); } catch {}
    try { closing.free(); } catch {}
    if (!reachedReady) rejectReady(new Error(error ?? `${label} session closed before acceptance`));
  };
  const abort = () => close(`${label} cancelled`);
  signal?.addEventListener("abort", abort, { once: true });
  const flushed = () => {
    if (done) return;
    try {
      if (socket.bufferedAmount === 0) {
        if (kind === "lobby" && session.phase() === "lobby") session.markFlushed();
        if (worldFlushPending) {
          session.markFlushed();
          worldFlushPending = false;
        }
        flushTimer = undefined;
      } else {
        flushTimer = setTimeout(flushed, 5);
      }
    } catch { close(`${label} flush failed`); }
  };
  const send = () => {
    const bytes = session.outgoing();
    if (!bytes.length) return;
    try {
      const worldSend = kind === "world" && (varpCount !== undefined || inventory || playerStats || scene) && session.phase() === "worldReady";
      socket.send(bytes);
      worldFlushPending ||= worldSend;
    } finally { bytes.fill(0); }
    if (done) return;
    clearTimeout(flushTimer);
    flushTimer = setTimeout(flushed, 0);
  };
  const apply = (action) => {
    if (done) throw new Error(`${label} session is closed`);
    try {
      action();
      send();
      update();
      return snapshot();
    } catch {
      close(`${label} operation failed`);
      throw new Error(`${label} operation failed`);
    }
  };
  const service = () => {
    if (done) return;
    try {
      apply(() => {
        JSON.parse(session.tick());
        session.gameTick();
      });
    } catch { return; }
    if (!done) tickTimer = setTimeout(service, 20);
  };
  authTimer = setTimeout(() => close(`${label} authentication timed out`), timeoutMs);
  lifetimeTimer = setTimeout(() => close(`${label} diagnostic lifetime reached`), lifetimeMs);
  try {
    socket = new WebSocketClass(url);
    if (done) { socket.close(); throw new Error(); }
    socket.binaryType = "arraybuffer";
    socket.onopen = () => {
      if (done) return;
      try { send(); update(); }
      catch { close(`${label} handshake send failed`); }
    };
    socket.onmessage = (event) => {
      if (done) return;
      try {
        if (!(event.data instanceof ArrayBuffer)) throw new Error();
        const bytes = new Uint8Array(event.data);
        if (bytes.length > MAX_MESSAGE || received + bytes.length > MAX_RECEIVED) throw new Error();
        received += bytes.length;
        let at = 0;
        let becameReady = false;
        while (at < bytes.length) {
          const feed = JSON.parse(session.feed(bytes.subarray(at)));
          if (!bounded(feed.consumed, 1, bytes.length - at)) throw new Error();
          at += feed.consumed;
          send();
          update();
          if (feed.event?.kind === "opaquePacket") packets++;
          if (feed.event?.kind === readyKind) becameReady = true;
          if (["rejected", "queued", "retryRequested", "reconnectRequired"].includes(feed.event?.kind)) {
            const responseCode = feed.event.kind === "rejected" && bounded(feed.event.responseCode, 0, 255) ? feed.event.responseCode : null;
            current = { ...current, outcome: feed.event.kind, ...(responseCode === null ? {} : { responseCode }) };
            close(`${label} ${feed.event.kind}${responseCode === null ? "" : ` (code ${responseCode})`}`);
            return;
          }
        }
        update();
        if (becameReady && !reachedReady) {
          onReady();
          if (done) return;
          reachedReady = true;
          clearTimeout(authTimer);
          if (kind === "world" && (varpCount !== undefined || inventory || playerStats || scene)) tickTimer = setTimeout(service, 20);
          resolveReady(snapshot());
        }
      } catch { close(`invalid ${label} response`); }
      finally { if (event.data instanceof ArrayBuffer) new Uint8Array(event.data).fill(0); }
    };
    socket.onerror = () => close(`${label} relay connection failed`);
    socket.onclose = (event) => {
      if (event && (event.code !== 1000 || event.wasClean === false)) {
        close(`${label} connection ended unexpectedly`);
        return;
      }
      try { session.finish(); }
      catch { close(`${label} connection ended unexpectedly`); return; }
      close();
    };
  } catch {
    close(`${label} socket construction failed`);
    throw new Error(`${label} socket construction failed`);
  }
  return Object.freeze({
    ready,
    state: snapshot,
    confirm: () => apply(() => session.confirm()),
    ...(kind === "lobby" ? {
      requestWorldList: (version) => {
        if (!bounded(version, -2147483648, 2147483647)) throw new Error("signed world list version required");
        return apply(() => session.requestWorldList(version));
      },
      tick: () => apply(() => session.tick()),
      startWorld: (relay, configJson, { timeoutMs: worldTimeout = timeoutMs, lifetimeMs: worldLifetime = lifetimeMs, varpCount, inventory: worldInventory = false, playerStats: worldPlayerStats = false, scene: worldScene = false } = {}) => {
        if (done || worldStarted || worldStarting) throw new Error("original world handoff unavailable");
        const worldUrl = relayUrl(relay, "world");
        deadlines(worldTimeout, worldLifetime);
        configBound(configJson);
        if (varpCount !== undefined && !bounded(varpCount, 0, 65536)) throw new Error("bounded original variable count required");
        if (typeof worldInventory !== "boolean") throw new Error("original inventory option must be boolean");
        if (typeof worldPlayerStats !== "boolean") throw new Error("original player stats option must be boolean");
        if (typeof worldScene !== "boolean") throw new Error("original scene option must be boolean");
        let world;
        worldStarting = true;
        try {
          world = varpCount === undefined ? session.startWorld(configJson) : session.startWorldState(configJson, varpCount);
          if (worldInventory) world.enableInventory();
          if (worldPlayerStats) world.enablePlayerStats();
          if (worldScene) world.enableScene();
          if (done) throw new Error();
        } catch {
          if (world) {
            try { world.close(); } catch {}
            try { world.free(); } catch {}
          }
          throw new Error("invalid original world handoff configuration");
        } finally { worldStarting = false; }
        worldStarted = true;
        return openSocketSession(worldUrl, world, {
          kind: "world", WebSocketClass, timeoutMs: worldTimeout, lifetimeMs: worldLifetime, signal, varpCount, inventory: worldInventory, playerStats: worldPlayerStats, scene: worldScene, onReady: () => close(),
        });
      },
    } : varpCount === undefined ? {} : {
      tick: () => {
        if (!reachedReady) throw new Error("original world is not ready");
        return apply(() => JSON.parse(session.tick()));
      },
      varp: (id) => {
        if (done || !reachedReady) throw new Error("original world state unavailable");
        if (!bounded(id, 0, varpCount - 1)) throw new Error("original variable ID outside verified span");
        try { return session.varp(id); }
        catch { throw new Error("original variable unavailable"); }
      },
    }),
    ...(kind === "world" && inventory ? {
      container: (id, negativeKey) => {
        if (done || !reachedReady) throw new Error("original world state unavailable");
        if (!bounded(id, 0, 65535) || typeof negativeKey !== "boolean") throw new Error("bounded original container key required");
        try {
          const value = inventorySnapshot(session.container(id, negativeKey), "id,negativeKey,slots", (entry) => entry.id === id && entry.negativeKey === negativeKey && bounded(entry.slots, 1, 65535));
          if (done) throw new Error();
          return value;
        } catch { throw new Error("original container unavailable"); }
      },
      item: (id, negativeKey, slot) => {
        if (done || !reachedReady) throw new Error("original world state unavailable");
        if (!bounded(id, 0, 65535) || typeof negativeKey !== "boolean" || !bounded(slot, 0, 65534)) throw new Error("bounded original item query required");
        try {
          const value = inventorySnapshot(session.item(id, negativeKey, slot), "amount,itemId,tooltip", (entry) => bounded(entry.itemId, -1, 65534) && bounded(entry.amount, -2147483648, 2147483647) && (entry.tooltip === null || (typeof entry.tooltip === "string" && entry.tooltip.length <= 65535)));
          if (done) throw new Error();
          return value;
        } catch { throw new Error("original inventory item unavailable"); }
      },
    } : {}),
    ...(kind === "world" && playerStats ? {
      skill: (id) => {
        if (done || !reachedReady) throw new Error("original world state unavailable");
        if (!bounded(id, 0, 24)) throw new Error("bounded original skill ID required");
        try {
          const value = playerStatsResult(session.skill(id), (entry) => skillSnapshot(entry, id));
          if (done) throw new Error();
          return value;
        } catch { throw new Error("original skill unavailable"); }
      },
      playerStats: () => {
        if (done || !reachedReady) throw new Error("original world state unavailable");
        try {
          const value = playerStatsResult(session.playerStats(), playerStatsSnapshot);
          if (done) throw new Error();
          return value;
        } catch { throw new Error("original player stats unavailable"); }
      },
    } : {}),
    ...(kind === "world" && varpCount !== undefined ? {
      lendLocSession: (use) => {
        if (done || !reachedReady || typeof use !== "function") throw new Error("original world state unavailable");
        try {
          const result = use(session);
          if (result && typeof result.then === "function") throw new Error();
          return result;
        } catch { throw new Error("original world locs unavailable"); }
      },
    } : {}),
    ...(kind === "world" && scene ? {
      lendSceneSession: (use) => {
        if (done || !reachedReady || typeof use !== "function") throw new Error("original world scene unavailable");
        try {
          const result = use(session);
          if (result && typeof result.then === "function") throw new Error();
          return result;
        } catch { throw new Error("original world scene unavailable"); }
      },
    } : {}),
    ...(kind === "world" && (varpCount !== undefined || inventory || playerStats) ? {
      cycleInput: (input, now, rendering) => {
        if (done || !reachedReady) throw new Error("original world state unavailable");
        if (typeof input?.cycle !== "function" || !Number.isFinite(now) || typeof rendering !== "boolean") throw new Error("client input cycle arguments required");
        return apply(() => input.cycle(session, now, rendering));
      },
    } : {}),
    close: () => close(),
  });
}

export async function takePrivateLobbyConfig({ fetcher = globalThis.fetch, origin = globalThis.location?.origin, signal } = {}) {
  let url;
  try { url = new URL("/__openrs/dev-login", origin); } catch { throw new Error("loopback development origin required"); }
  if (url.protocol !== "http:" || !["127.0.0.1", "localhost", "[::1]"].includes(url.hostname)) throw new Error("loopback development origin required");
  let response;
  try {
    signal?.throwIfAborted();
    response = await fetcher(url.href, {
      method: "POST", headers: { "Content-Type": "application/json", "X-OpenRS-Intent": "original-login" },
      body: "{}", cache: "no-store", credentials: "omit", redirect: "error", signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(15000)]) : AbortSignal.timeout(15000),
    });
  } catch { throw new Error("private lobby bootstrap failed"); }
  if (!response.ok) throw new Error("private lobby bootstrap unavailable");
  const reader = response.body.getReader();
  const chunks = [];
  let size = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      chunks.push(value);
      size += value.length;
      if (size > MAX_MESSAGE) { await reader.cancel(); throw new Error("private lobby configuration exceeds bound"); }
    }
    const bytes = new Uint8Array(size);
    try {
      let at = 0;
      for (const chunk of chunks) { bytes.set(chunk, at); at += chunk.length; }
      return new TextDecoder("utf-8", { fatal: true }).decode(bytes);
    } finally { bytes.fill(0); }
  } catch { throw new Error("invalid private lobby bootstrap body"); }
  finally {
    for (const chunk of chunks) chunk.fill(0);
    reader.releaseLock();
  }
}
