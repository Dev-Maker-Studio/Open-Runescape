import { PUBLIC_LOGIN_DEFAULTS } from "./login-config.js";

let runtime;

export async function loadRuntime() {
  runtime ??= (async () => {
    const module = await import("./protocol-runtime.js");
    const response = await fetch(new URL("./protocol-runtime_bg.wasm", import.meta.url), { signal: AbortSignal.timeout(15000) });
    if (!response.ok) throw new Error(`protocol runtime: HTTP ${response.status}`);
    await module.default({ module_or_path: await response.arrayBuffer() });
    return module;
  })().catch((error) => { runtime = null; throw error; });
  return runtime;
}

async function boundedRuntime(load, timeoutMs, signal) {
  let timer;
  let abort;
  try {
    if (signal?.aborted) throw new Error("protocol exchange cancelled");
    const cancelled = new Promise((_, reject) => {
      abort = () => reject(new Error("protocol exchange cancelled"));
      signal?.addEventListener("abort", abort, { once: true });
      if (signal?.aborted) abort();
    });
    return await Promise.race([
      Promise.resolve().then(() => {
        if (signal?.aborted) throw new Error("protocol exchange cancelled");
        return load();
      }),
      new Promise((_, reject) => { timer = setTimeout(() => reject(new Error("protocol runtime load timed out")), timeoutMs); }),
      cancelled,
    ]);
  } finally {
    clearTimeout(timer);
    signal?.removeEventListener("abort", abort);
  }
}

export async function protocolProbe(relay, token, { load = async () => (await loadRuntime()).Js5Probe, WebSocketClass = globalThis.WebSocket, timeoutMs = 15000, signal } = {}) {
  return exchange(relay, token, { load, WebSocketClass, timeoutMs, signal, maxBytes: 128 * 1024,
    create: (Probe) => new Probe(token), collect: (probe) => JSON.parse(probe.report()) });
}

export async function assetProbe(relay, token, index, archive, { load = async () => (await loadRuntime()).Js5AssetProbe, WebSocketClass = globalThis.WebSocket, timeoutMs = 30000, signal } = {}) {
  if (!Number.isSafeInteger(index) || index < 0 || index >= 255 || !Number.isSafeInteger(archive) || archive < 0 || archive > 0x7fffffff) {
    throw new Error("invalid JS5 asset identity");
  }
  return exchange(relay, token, { load, WebSocketClass, timeoutMs, signal, maxBytes: 12 * 1024 * 1024,
    create: (Probe) => new Probe(token, index, archive), collect: (probe) => {
      const report = JSON.parse(probe.report());
      const files = new Map();
      const owned = new Set();
      let total = 0;
      try {
        for (const file of report.files) {
          if (files.has(file.id)) throw new Error("duplicate asset file ID");
          const bytes = probe.file(file.id);
          if (!(bytes instanceof Uint8Array)) throw new Error("asset file bytes required");
          owned.add(bytes);
          total += bytes.length;
          if (bytes.length !== file.bytes || total > 8 * 1024 * 1024) throw new Error("asset file byte limit");
          files.set(file.id, bytes);
        }
        return { report, files };
      } catch (error) {
        for (const bytes of owned) bytes.fill(0);
        files.clear();
        throw error;
      }
    } });
}

export async function worldLocProbe(relay, lend, { load = async () => (await loadRuntime()).Js5WorldLocProbe, WebSocketClass = globalThis.WebSocket, timeoutMs = 30000, signal } = {}) {
  if (typeof lend !== "function") throw new Error("world session lender required");
  const token = PUBLIC_LOGIN_DEFAULTS.js5AuthToken;
  return exchange(relay, token, { load, WebSocketClass, timeoutMs, signal, maxBytes: 96 * 1024 * 1024,
    create: (Probe) => lend((session) => new Probe(token, session)), collect: (probe) => {
      const report = JSON.parse(probe.report());
      const regions = new Map();
      const types = new Map();
      const models = new Map();
      const typeModels = new Map();
      for (const region of report.regions) {
        if (!region.present) continue;
        const data = probe.regionLocs(region.regionId);
        const typeIds = probe.regionTypeIds(region.regionId);
        if (!(data instanceof Uint8Array) || !(typeIds instanceof Int32Array)) throw new Error("region loc bytes required");
        regions.set(region.regionId, { data, typeIds });
        for (const id of typeIds) {
          if (types.has(id)) continue;
          const type = probe.locType(id);
          if (!(type instanceof Uint8Array)) continue;
          types.set(id, type);
          const ids = Array.from(probe.modelIds(id));
          typeModels.set(id, ids);
          for (const model of ids) {
            if (models.has(model)) continue;
            const bytes = probe.locModel(model);
            if (bytes instanceof Uint8Array) models.set(model, bytes);
          }
        }
      }
      return { report, regions, types, models, typeModels };
    } });
}

export async function scriptGraphProbe(relay, token, root, { ints, load = async () => (await loadRuntime()).Js5ScriptGraphProbe, WebSocketClass = globalThis.WebSocket, timeoutMs = 30000 } = {}) {
  if (!Number.isSafeInteger(root) || root < 0 || root > 1000000) throw new Error("invalid root script identity");
  if (ints !== undefined && (!Array.isArray(ints) || ints.length > 1000 || ints.some((value) => !Number.isSafeInteger(value) || value < -2147483648 || value > 2147483647))) {
    throw new Error("invalid script integer arguments");
  }
  return exchange(relay, token, { load, WebSocketClass, timeoutMs, maxBytes: 16 * 1024 * 1024,
    create: (Probe) => new Probe(token, root), collect: (probe) => {
      const report = JSON.parse(probe.report());
      const files = new Map();
      let total = 0;
      if (report.scripts.length > 64) throw new Error("script graph count limit");
      for (const script of report.scripts) {
        const bytes = probe.file(script.id);
        total += bytes.length;
        if (bytes.length !== script.bytes || bytes.length > 64 * 1024 || total > 1024 * 1024) throw new Error("script graph file byte limit");
        files.set(script.id, bytes);
      }
      const execution = ints === undefined ? null : JSON.parse(probe.run(JSON.stringify(ints)));
      return { report, files, execution };
    } });
}

export async function cs2RunProbe(relay, root, session, { ints = [], strings = [], load = async () => (await loadRuntime()).Js5Cs2Run, WebSocketClass = globalThis.WebSocket, timeoutMs = 30000 } = {}) {
  if (!Number.isSafeInteger(root) || root < 0 || root > 1000000) throw new Error("invalid root script identity");
  if (!Array.isArray(ints) || ints.length > 1000 || ints.some((value) => !Number.isSafeInteger(value) || value < -2147483648 || value > 2147483647)) {
    throw new Error("invalid script integer arguments");
  }
  if (!Array.isArray(strings) || strings.length > 64 || strings.some((value) => value !== null && typeof value !== "string")) {
    throw new Error("invalid script string arguments");
  }
  const stateful = session !== null && session !== undefined;
  if (stateful && typeof session.varp !== "function") throw new Error("independent world session required");
  const token = PUBLIC_LOGIN_DEFAULTS.js5AuthToken;
  return exchange(relay, token, { load, WebSocketClass, timeoutMs, maxBytes: 16 * 1024 * 1024,
    create: (Probe) => new Probe(token, root),
    collect: (probe) => JSON.parse(stateful ? probe.run(session, JSON.stringify(ints), JSON.stringify(strings)) : probe.runStateless(JSON.stringify(ints), JSON.stringify(strings))) });
}

export async function gameHandshakeProbe(relay, { load = async () => (await loadRuntime()).GameHandshakeProbe, WebSocketClass = globalThis.WebSocket, timeoutMs = 10000 } = {}) {
  const path = new URL(relay).pathname;
  if (!["/lobby", "/world"].includes(path)) throw new Error("exact lobby or world handshake relay URL required");
  return exchange(relay, null, { load, WebSocketClass, timeoutMs, maxBytes: 1, path, label: "game handshake", tokenRequired: false,
    create: (Probe) => new Probe(), collect: (probe) => JSON.parse(probe.report()) });
}

async function exchange(relay, token, { load, WebSocketClass, timeoutMs, maxBytes, create, collect, path = "/js5", label = "JS5", tokenRequired = true, signal }) {
  const url = new URL(relay);
  if (!(["ws:", "wss:"].includes(url.protocol) && ["127.0.0.1", "localhost", "[::1]"].includes(url.hostname)) || url.pathname !== path || url.search || url.hash || url.username || url.password) {
    throw new Error(`exact loopback ${label} relay URL required`);
  }
  if ((tokenRequired && (typeof token !== "string" || !token)) || !Number.isFinite(timeoutMs) || timeoutMs < 1 || timeoutMs > 30000) {
    throw new Error(tokenRequired ? "JS5 token and bounded timeout required" : "bounded handshake timeout required");
  }
  const Probe = await boundedRuntime(load, timeoutMs, signal);
  if (signal?.aborted) throw new Error("protocol exchange cancelled");
  const probe = create(Probe);
  return new Promise((resolve, reject) => {
    let ws;
    let timer;
    let done = false;
    let received = 0;
    const abort = () => finish(new Error("protocol exchange cancelled"));
    const closeSocket = () => {
      if (!ws) return;
      ws.onopen = ws.onmessage = ws.onerror = ws.onclose = null;
      try { ws.close(); } catch {}
    };
    const finish = (error, report) => {
      if (done) return;
      done = true;
      clearTimeout(timer);
      signal?.removeEventListener("abort", abort);
      closeSocket();
      try { probe.free(); }
      catch { error ??= new Error("protocol probe cleanup failed"); }
      if (error) reject(error); else resolve(report);
    };
    timer = setTimeout(() => finish(new Error(`${label} probe timed out`)), timeoutMs);
    signal?.addEventListener("abort", abort, { once: true });
    const send = () => {
      const bytes = probe.outgoing();
      if (bytes.length) ws.send(bytes);
    };
    try {
      if (signal?.aborted) { abort(); return; }
      ws = new WebSocketClass(url.href);
      if (done) { closeSocket(); return; }
      ws.binaryType = "arraybuffer";
      ws.onopen = () => {
        if (done) return;
        try { send(); } catch (error) { finish(error); }
      };
      ws.onmessage = (event) => {
        if (done) return;
        try {
          if (!(event.data instanceof ArrayBuffer)) throw new Error(`binary ${label} response required`);
          received += event.data.byteLength;
          if (received > maxBytes) throw new Error(`${label} probe byte limit`);
          probe.feed(new Uint8Array(event.data));
          send();
          if (probe.complete()) finish(null, collect(probe));
        } catch (error) { finish(error); }
      };
      ws.onerror = () => finish(new Error("JS5 relay connection failed"));
      ws.onclose = () => {
        if (done) return;
        try { probe.finish(); finish(null, collect(probe)); }
        catch (error) { finish(error); }
      };
    } catch (error) { finish(error); }
  });
}

export async function coreScriptProbe(path, { fetcher = globalThis.fetch, load = loadRuntime, origin = globalThis.location.origin } = {}) {
  const url = new URL(path, origin);
  if (url.origin !== origin || !url.pathname.startsWith("/files/") || url.search || url.hash || url.username || url.password) {
    throw new Error("same-origin supplied script fixture required");
  }
  const response = await fetcher(url.href, { cache: "no-store", redirect: "error", credentials: "omit", signal: AbortSignal.timeout(15000) });
  if (!response.ok) throw new Error(`script fixture: HTTP ${response.status}`);
  const reader = response.body.getReader();
  const chunks = [];
  let size = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      size += value.length;
      if (size > 64 * 1024) {
        await reader.cancel();
        throw new Error("script fixture byte limit");
      }
      chunks.push(value);
    }
  } finally { reader.releaseLock(); }
  const bytes = new Uint8Array(size);
  let at = 0;
  for (const chunk of chunks) { bytes.set(chunk, at); at += chunk.length; }
  const module = await boundedRuntime(load, 15000);
  return JSON.parse(module.runCoreScript(bytes));
}
