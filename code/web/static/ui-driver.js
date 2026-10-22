const CLICK_BUTTON = 0;
const HELD_CHAR = 0xffff;
const TYPED_CODE = -1;
const CONSOLE_CHARS = new Set([96, 167, 178]);
const PARALLEL = 4;
const LEGACY_TO_AWT = new Map([
  [13, 10], [46, 127], [45, 155], [186, 59], [187, 61], [188, 44], [189, 45], [190, 46], [191, 47],
  [219, 91], [220, 92], [221, 93], [192, 192], [222, 222],
]);

export function awtKeyCode(legacyKeyCode) {
  if (!Number.isInteger(legacyKeyCode) || legacyKeyCode <= 0) return 0;
  const mapped = LEGACY_TO_AWT.get(legacyKeyCode);
  if (mapped !== undefined) return mapped;
  return legacyKeyCode >= 91 && legacyKeyCode <= 93 ? 0 : legacyKeyCode;
}

const fnv = (words) => {
  let hash = 0x811c9dc5;
  for (let i = 0; i < words.length; i += 1) hash = Math.imul(hash ^ words[i], 0x01000193);
  return hash >>> 0;
};

export function createUiDriver({
  runtime,
  app,
  canvas,
  session,
  fetchArchive,
  relay,
  token,
  target = globalThis,
  onStatus = () => {},
} = {}) {
  if (typeof runtime?.InterfaceUi !== "function") throw new Error("runtime lacks the interface ui");
  if (typeof app?.set_ui_overlay !== "function") throw new Error("renderer lacks the interface overlay");
  if (typeof session?.openInterfaces !== "function") throw new Error("world session lacks open interfaces");
  const ui = new runtime.InterfaceUi(canvas.width, canvas.height);
  const mouse = { x: -1, y: -1, down: false, clickX: -1, clickY: -1, wheel: 0 };
  const keys = [];
  const held = new Set();
  const stats = { frames: 0, uploads: 0, loads: 0, absent: 0, failures: 0, hooks: 0, clicks: 0, keys: 0 };
  let stopped = false;
  let loading = false;
  let revision = -2;
  let lastHash = -1;
  let lastGame = "";
  let game = null;
  let report = null;
  let lastError = null;

  const status = (text) => { try { onStatus(text); } catch {} };
  if (canvas.tabIndex < 0) canvas.tabIndex = 0;

  const point = (event) => {
    const r = canvas.getBoundingClientRect();
    if (!(r.width > 0 && r.height > 0)) return null;
    return { x: Math.floor((event.clientX - r.left) * (canvas.width / r.width)), y: Math.floor((event.clientY - r.top) * (canvas.height / r.height)) };
  };
  const mouseMove = (event) => {
    const p = point(event);
    if (!p) return;
    mouse.x = p.x;
    mouse.y = p.y;
  };
  const mouseDown = (event) => {
    if (event.button !== CLICK_BUTTON) return;
    const p = point(event);
    if (!p) return;
    canvas.focus?.();
    mouse.x = p.x;
    mouse.y = p.y;
    mouse.down = true;
    mouse.clickX = p.x;
    mouse.clickY = p.y;
    stats.clicks += 1;
  };
  const mouseUp = (event) => {
    if (event.button === CLICK_BUTTON) mouse.down = false;
  };
  const mouseLeave = () => {
    mouse.x = -1;
    mouse.y = -1;
    mouse.down = false;
  };
  const wheel = (event) => {
    if (event.ctrlKey || !Number.isFinite(event.deltaY) || event.deltaY === 0) return;
    mouse.wheel += event.deltaY > 0 ? 1 : -1;
  };
  const editable = (event) => {
    const tag = event.target?.tagName;
    return tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT" || event.target?.isContentEditable === true;
  };
  const keyDown = (event) => {
    if (editable(event) || event.metaKey) return;
    const code = runtime.InterfaceUi.rsKeyCode(awtKeyCode(event.keyCode), true);
    let used = false;
    if (code !== 0) {
      keys.push(code, HELD_CHAR);
      held.add(code);
      used = true;
    }
    if (typeof event.key === "string" && event.key.length === 1 && !event.ctrlKey && !event.altKey) {
      const unit = event.key.charCodeAt(0);
      if (!CONSOLE_CHARS.has(unit) && runtime.InterfaceUi.validKeyChar(unit)) {
        keys.push(TYPED_CODE, unit);
        used = true;
      }
    }
    if (used) {
      stats.keys += 1;
      if (event.target === canvas) event.preventDefault?.();
    }
  };
  const keyUp = (event) => {
    const code = runtime.InterfaceUi.rsKeyCode(awtKeyCode(event.keyCode), false);
    if (code !== 0) held.delete(code);
  };
  const blur = () => { held.clear(); mouse.down = false; };
  canvas.addEventListener?.("mousemove", mouseMove);
  canvas.addEventListener?.("mousedown", mouseDown);
  canvas.addEventListener?.("mouseleave", mouseLeave);
  canvas.addEventListener?.("wheel", wheel, { passive: true });
  target.addEventListener?.("mouseup", mouseUp, true);
  target.addEventListener?.("keydown", keyDown, true);
  target.addEventListener?.("keyup", keyUp, true);
  target.addEventListener?.("blur", blur);

  const load = async (index, group) => {
    for (let attempt = 0; attempt < 2; attempt += 1) {
      try { return await fetchArchive(relay, token, index, group); } catch (error) { lastError = String(error?.message ?? error); }
    }
    stats.failures += 1;
    return null;
  };

  const pump = async () => {
    if (loading || stopped) return;
    loading = true;
    try {
      for (let guard = 0; guard < 32 && !stopped; guard += 1) {
        const flat = ui.needs();
        if (!flat.length) break;
        const needs = [];
        for (let at = 0; at + 1 < flat.length; at += 2) needs.push([flat[at], flat[at + 1]]);
        for (let at = 0; at < needs.length && !stopped; at += PARALLEL) {
          const slice = needs.slice(at, at + PARALLEL);
          const loaded = await Promise.all(slice.map(([index, group]) => load(index, group)));
          slice.forEach(([index, group], i) => {
            const files = loaded[i]?.files;
            if (!files || files.size === 0) { ui.absent(index, group); stats.absent += 1; return; }
            for (const [file, bytes] of files) ui.addFile(index, group, file, bytes);
            ui.finishGroup(index, group);
            stats.loads += 1;
          });
        }
        lastHash = -1;
      }
    } catch (error) {
      lastError = String(error?.message ?? error);
      status(`ui driver: ${lastError}`);
    } finally { loading = false; }
  };

  const step = () => {
    if (stopped) return false;
    if (ui.width() !== canvas.width || ui.height() !== canvas.height) {
      ui.resize(canvas.width, canvas.height);
      lastHash = -1;
    }
    const open = session.openInterfaces();
    if (open[1] !== revision || revision === -2) {
      const subs = Int32Array.from(open.slice(2));
      ui.setOpen(open[0], subs);
      revision = open[1];
    }
    let result;
    try {
      result = JSON.parse(ui.frame(mouse.x, mouse.y, mouse.down, mouse.clickX, mouse.clickY, mouse.wheel, Int32Array.from(keys), Int32Array.from(held)));
    } catch (error) {
      lastError = String(error?.message ?? error);
      stats.failures += 1;
      return false;
    }
    mouse.clickX = -1;
    mouse.clickY = -1;
    mouse.wheel = 0;
    keys.length = 0;
    stats.frames += 1;
    stats.hooks += result.hooks;
    report = result;
    if (result.missingSprites.length || result.missingFonts.length || ui.needs().length) pump();
    if (!result.drawn) {
      if (game !== null) { app.clear_ui_overlay(); game = null; lastGame = ""; lastHash = -1; return true; }
      return false;
    }
    const rect = result.gameArea ? { x: result.gameArea[0], y: result.gameArea[1], width: result.gameArea[2], height: result.gameArea[3] } : null;
    const gameKey = rect ? `${rect.x},${rect.y},${rect.width},${rect.height}` : "";
    const rgba = ui.rgba();
    const hash = fnv(new Uint32Array(rgba.buffer, rgba.byteOffset, rgba.byteLength >> 2));
    if (hash === lastHash && gameKey === lastGame) return false;
    app.set_ui_overlay(ui.width(), ui.height(), rgba, rect?.x ?? 0, rect?.y ?? 0, rect?.width ?? 0, rect?.height ?? 0);
    lastHash = hash;
    lastGame = gameKey;
    game = rect;
    stats.uploads += 1;
    return true;
  };

  const stop = () => {
    if (stopped) return;
    stopped = true;
    canvas.removeEventListener?.("mousemove", mouseMove);
    canvas.removeEventListener?.("mousedown", mouseDown);
    canvas.removeEventListener?.("mouseleave", mouseLeave);
    canvas.removeEventListener?.("wheel", wheel);
    target.removeEventListener?.("mouseup", mouseUp, true);
    target.removeEventListener?.("keydown", keyDown, true);
    target.removeEventListener?.("keyup", keyUp, true);
    target.removeEventListener?.("blur", blur);
    try { app.clear_ui_overlay(); } catch {}
    try { ui.free?.(); } catch {}
  };

  return Object.freeze({
    step,
    stop,
    gameRect: () => game,
    hooks: () => (stopped ? [] : JSON.parse(ui.hooks())),
    summary: () => ({ scope: "ui-driver", stopped, error: lastError, game, report, stats: { ...stats }, loading }),
  });
}
