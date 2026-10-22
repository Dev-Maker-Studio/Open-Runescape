const KEY_DOWN = 0, KEY_UP = 1, MOVE = 2, DOWN = 3, UP = 4, WHEEL = 5, RESIZE = 6, FOCUS = 7;
const INPUT = 0, ACK = 1;
const s = { app: null, canvas: null, ws: null, game: null, host: null, state: "closed", seq: 0, sent: new Map(), rtt: null, rttMax: 0, shown: 0, sends: 0, error: null, opened: 0 };

function encode(kind, button, a, b, code) {
  const bytes = new TextEncoder().encode(code).slice(0, 255);
  const buf = new Uint8Array(16 + bytes.length);
  const v = new DataView(buf.buffer);
  buf[0] = INPUT;
  v.setUint32(1, ++s.seq, true);
  buf[5] = kind;
  buf[6] = button;
  v.setFloat32(7, a, true);
  v.setFloat32(11, b, true);
  buf[15] = bytes.length;
  buf.set(bytes, 16);
  return buf;
}

function send(kind, button = 0, a = 0, b = 0, code = "") {
  if (s.ws?.readyState !== WebSocket.OPEN) return;
  s.ws.send(encode(kind, button, a, b, code));
  s.sends++;
  s.sent.set(s.seq, performance.now());
  if (s.sent.size > 256) s.sent.delete(s.sent.keys().next().value);
}

function pixel(e) {
  const r = s.canvas.getBoundingClientRect();
  return [(e.clientX - r.left) * s.canvas.width / r.width, (e.clientY - r.top) * s.canvas.height / r.height];
}

export function nativeTextChunks(text) {
  const out = [];
  let part = "";
  for (const ch of text) {
    if (new TextEncoder().encode(part + ch).length > 128) {
      out.push(`Text:${part}`);
      part = "";
    }
    part += ch;
  }
  if (part) out.push(`Text:${part}`);
  return out;
}

export function nativeFrameSize(bytes) {
  if (bytes[0] !== 0) return null;
  const hello = JSON.parse(new TextDecoder().decode(bytes.subarray(1)));
  const { width, height } = hello;
  if (!Number.isInteger(width) || !Number.isInteger(height) || width < 1 || height < 1 || width > 8192 || height > 8192) throw new Error("invalid native frame size");
  return [width, height];
}

function sendText(text) {
  if (s.game !== "native") return;
  for (const code of nativeTextChunks(text)) send(KEY_DOWN, 0, 0, 0, code);
}

function listen() {
  const c = s.canvas;
  addEventListener("keydown", (e) => { if (s.game === "native" || !e.repeat) send(KEY_DOWN, 0, 0, 0, e.code); if (e.code.startsWith("Arrow") || e.code === "Space") e.preventDefault(); });
  addEventListener("keyup", (e) => send(KEY_UP, 0, 0, 0, e.code));
  addEventListener("keypress", (e) => { if (!e.isComposing && Array.from(e.key).length === 1 && !e.ctrlKey && !e.metaKey) sendText(e.key); });
  c.addEventListener("compositionend", (e) => sendText(e.data || ""));
  c.addEventListener("mousemove", (e) => send(MOVE, e.buttons, ...pixel(e)));
  c.addEventListener("mousedown", (e) => send(DOWN, e.button, ...pixel(e)));
  c.addEventListener("mouseup", (e) => send(UP, e.button, ...pixel(e)));
  addEventListener("mouseup", (e) => { if (s.game === "native" && e.target !== c) send(UP, e.button, ...pixel(e)); });
  c.addEventListener("wheel", (e) => { send(WHEEL, 0, e.deltaX, e.deltaY); e.preventDefault(); }, { passive: false });
  c.addEventListener("contextmenu", (e) => e.preventDefault());
  addEventListener("resize", () => send(RESIZE, 0, c.width, c.height));
  addEventListener("focus", () => send(FOCUS, 0, 1));
  addEventListener("blur", () => send(FOCUS, 0, 0));
}

export function disconnect() {
  if (s.ws) { s.ws.onclose = null; s.ws.close(); }
  s.ws = null;
  s.state = "closed";
  s.app?.disconnect();
  s.sent.clear();
  s.rtt = null;
  s.rttMax = 0;
  s.shown = 0;
}

export function connect(game, host = s.host || `${location.hostname}:8100`) {
  disconnect();
  Object.assign(s, { game, host, state: "connecting", error: null, opened: s.opened + 1 });
  const ws = new WebSocket(`ws://${host}/?game=${encodeURIComponent(game)}`);
  ws.binaryType = "arraybuffer";
  ws.onopen = () => { s.state = "open"; send(RESIZE, 0, s.canvas.width, s.canvas.height); send(FOCUS, 0, document.hasFocus() ? 1 : 0); };
  ws.onmessage = (e) => {
    if (typeof e.data === "string") { s.error = e.data; return; }
    try {
      const bytes = new Uint8Array(e.data);
      if (s.game === "native") {
        const size = nativeFrameSize(bytes);
        if (size) {
          s.canvas.dataset.fixed = "1";
          [s.canvas.width, s.canvas.height] = size;
          send(RESIZE, 0, ...size);
        }
      }
      const reply = s.app.push(bytes);
      if (reply) ws.send(reply);
    } catch (err) { s.error = String(err); }
  };
  ws.onerror = () => { s.error = `websocket error to ${host}`; };
  ws.onclose = (e) => { s.state = "closed"; if (!s.error) s.error = `closed ${e.code}`; };
  s.ws = ws;
}

export function afterFrame() {
  if (!s.app || !s.ws) return;
  const shown = s.app.shown();
  if (shown === s.shown) return;
  s.shown = shown;
  if (s.ws.readyState === WebSocket.OPEN) {
    const buf = new Uint8Array(5);
    buf[0] = ACK;
    new DataView(buf.buffer).setUint32(1, shown, true);
    s.ws.send(buf);
  }
  const seq = JSON.parse(s.app.stats()).inputSeq;
  const t = s.sent.get(seq);
  if (t !== undefined) {
    s.rtt = performance.now() - t;
    s.rttMax = Math.max(s.rttMax, s.rtt);
    for (const k of s.sent.keys()) if (k <= seq) s.sent.delete(k);
  }
}

export function stats() {
  const st = s.app ? JSON.parse(s.app.stats()) : {};
  return { game: s.game, host: s.host, state: s.state, ...st, seq: s.seq, sends: s.sends, rttMs: s.rtt === null ? null : Math.round(s.rtt * 10) / 10, rttMaxMs: Math.round(s.rttMax * 10) / 10, opened: s.opened, error: s.error };
}

export function setup(app, canvas) {
  Object.assign(s, { app, canvas });
  listen();
  const p = new URLSearchParams(location.search);
  const game = p.get("game");
  if (game) connect(game, p.get("host") || undefined);
}
