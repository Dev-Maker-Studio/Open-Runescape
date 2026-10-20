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
  if (st.app && !st.error && t - st.last >= interval) {
    const c = performance.now();
    try {
      st.app.frame();
      tick();
    } catch (e) {
      fail(e);
    }
    st.cpuMs += (performance.now() - c - st.cpuMs) * 0.1;
    if (st.last) st.frameMs += (t - st.last - st.frameMs) * 0.1;
    st.last = t;
  }
  requestAnimationFrame(loop);
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
    loaded: i.loaded ?? null,
    frameSize: i.frameSize ?? null,
    summary: i.summary ?? null,
    frameMs: Math.round(st.frameMs * 100) / 100,
    cpuMs: Math.round(st.cpuMs * 1000) / 1000,
    lastError: st.error,
    gpuError: i.gpuError || null,
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
  scene: async (name = "all") => {
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
  png: async () => {
    const rb = need().capture();
    tick();
    if (!rb) throw new Error("surface texture unavailable");
    const [w, h] = [rb.width, rb.height];
    const rgba = await rb.read();
    const c = new OffscreenCanvas(w, h);
    c.getContext("2d").putImageData(new ImageData(new Uint8ClampedArray(rgba.buffer, rgba.byteOffset, rgba.length), w, h), 0, 0);
    return { width: w, height: h, data: await base64(await c.convertToBlob({ type: "image/png" })) };
  },
  dump: async () => ({ data: await base64(new Blob([need().dump()])), summary: summary() }),
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

window.openrs = { run, state, describe };

export async function install(app, canvas, backend) {
  Object.assign(st, { app, canvas, backend });
  const a = await navigator.gpu?.requestAdapter().catch(() => null);
  const i = a?.info ?? {};
  st.gpu = a ? { vendor: i.vendor, architecture: i.architecture, device: i.device, description: i.description, fallback: i.isFallbackAdapter ?? a.isFallbackAdapter ?? false } : null;
  requestAnimationFrame(loop);
}
