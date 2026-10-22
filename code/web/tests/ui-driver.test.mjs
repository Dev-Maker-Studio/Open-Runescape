import test from "node:test";
import assert from "node:assert/strict";
import { awtKeyCode, createUiDriver } from "../static/ui-driver.js";

function fixture({ report = {}, rgba = new Uint8Array(16), loads = {} } = {}) {
  const calls = [];
  const listeners = new Map();
  const canvasListeners = new Map();
  const canvas = {
    width: 765, height: 503, tabIndex: -1,
    getBoundingClientRect: () => ({ left: 10, top: 20, width: 765, height: 503 }),
    addEventListener: (type, fn) => canvasListeners.set(type, fn),
    removeEventListener: (type) => canvasListeners.delete(type),
    focus: () => calls.push(["focus"]),
  };
  const target = {
    addEventListener: (type, fn) => listeners.set(type, fn),
    removeEventListener: (type) => listeners.delete(type),
  };
  const frames = [];
  let needs = [];
  class InterfaceUi {
    constructor(w, h) { calls.push(["new", w, h]); this.w = w; this.h = h; }
    static rsKeyCode(vk, down) { return vk === 65 ? 48 : vk === 10 ? 84 : vk === 96 && down ? 0 : 0; }
    static validKeyChar(unit) { return unit > 0 && unit < 128; }
    width() { return this.w; }
    height() { return this.h; }
    resize(w, h) { this.w = w; this.h = h; calls.push(["resize", w, h]); }
    setOpen(top, subs) { calls.push(["open", top, Array.from(subs)]); }
    frame(...args) {
      frames.push(args.map((v) => (ArrayBuffer.isView(v) ? Array.from(v) : v)));
      return JSON.stringify({ drawn: true, gameArea: [4, 4, 512, 334, 0], hooks: 1, missingSprites: [], missingFonts: [], ...report });
    }
    rgba() { return rgba; }
    needs() { return needs; }
    addFile(index, group, file, bytes) { calls.push(["file", index, group, file, bytes.length]); }
    finishGroup(index, group) { calls.push(["finish", index, group]); needs = []; }
    absent(index, group) { calls.push(["absent", index, group]); needs = []; }
    hooks() { return "[]"; }
    free() { calls.push(["free"]); }
  }
  const app = {
    set_ui_overlay: (...args) => calls.push(["overlay", args[0], args[1], args[3], args[4], args[5], args[6]]),
    clear_ui_overlay: () => calls.push(["clear"]),
  };
  const session = { openInterfaces: () => Int32Array.from([548, 3, 35913728, 752, 1]) };
  const fetchArchive = async (relay, token, index, group) => loads[`${index}:${group}`] ?? null;
  const driver = createUiDriver({ runtime: { InterfaceUi }, app, canvas, session, fetchArchive, relay: "r", token: "t", target });
  return { calls, listeners, canvasListeners, canvas, driver, frames, setNeeds: (n) => { needs = n; } };
}

test("awt key codes map the browser punctuation and enter keys", () => {
  assert.equal(awtKeyCode(13), 10);
  assert.equal(awtKeyCode(188), 44);
  assert.equal(awtKeyCode(46), 127);
  assert.equal(awtKeyCode(91), 0);
  assert.equal(awtKeyCode(65), 65);
});

test("a step feeds the open interfaces, maps the mouse and uploads the overlay with its game area", () => {
  const f = fixture();
  f.canvasListeners.get("mousemove")({ clientX: 110, clientY: 70 });
  assert.equal(f.driver.step(), true);
  assert.deepEqual(f.calls.find((c) => c[0] === "open"), ["open", 548, [35913728, 752, 1]]);
  assert.deepEqual(f.frames[0].slice(0, 3), [100, 50, false]);
  assert.deepEqual(f.calls.find((c) => c[0] === "overlay"), ["overlay", 765, 503, 4, 4, 512, 334]);
  assert.deepEqual(f.driver.gameRect(), { x: 4, y: 4, width: 512, height: 334 });
  assert.equal(f.driver.step(), false);
});

test("a click is delivered once with its canvas position and focuses the canvas", () => {
  const f = fixture();
  f.canvasListeners.get("mousedown")({ button: 0, clientX: 30, clientY: 40 });
  f.driver.step();
  f.driver.step();
  assert.deepEqual(f.frames[0].slice(2, 5), [true, 20, 20]);
  assert.deepEqual(f.frames[1].slice(3, 5), [-1, -1]);
  assert.ok(f.calls.some((c) => c[0] === "focus"));
});

test("keys queue a held press and a typed character, skipping the console key and modifiers", () => {
  const f = fixture();
  const down = f.listeners.get("keydown");
  down({ keyCode: 65, key: "a", target: f.canvas, preventDefault: () => f.calls.push(["prevent"]) });
  down({ keyCode: 192, key: "`", target: f.canvas });
  down({ keyCode: 65, key: "a", ctrlKey: true, target: f.canvas });
  f.driver.step();
  assert.deepEqual(f.frames[0][6], [48, 0xffff, -1, 97, 48, 0xffff]);
  assert.deepEqual(f.frames[0][7], [48]);
  assert.ok(f.calls.some((c) => c[0] === "prevent"));
  f.listeners.get("keyup")({ keyCode: 65 });
  f.driver.step();
  assert.deepEqual(f.frames[1][7], []);
});

test("typing into a form field is ignored", () => {
  const f = fixture();
  f.listeners.get("keydown")({ keyCode: 65, key: "a", target: { tagName: "INPUT" } });
  f.driver.step();
  assert.deepEqual(f.frames[0][6], []);
});

test("needed archives are fetched, added and finished, absent ones are marked", async () => {
  const f = fixture({ loads: { "8:12": { files: new Map([[0, new Uint8Array(5)]]) } } });
  f.setNeeds([8, 12, 3, 99]);
  f.driver.step();
  await new Promise((r) => setTimeout(r, 10));
  assert.ok(f.calls.some((c) => c[0] === "file" && c[1] === 8 && c[2] === 12 && c[4] === 5));
  assert.ok(f.calls.some((c) => c[0] === "finish" && c[2] === 12));
  assert.ok(f.calls.some((c) => c[0] === "absent" && c[2] === 99));
  assert.equal(f.driver.summary().stats.loads, 1);
});

test("a canvas resize resizes the ui and stop releases listeners and the overlay", () => {
  const f = fixture();
  f.canvas.width = 800;
  f.driver.step();
  assert.ok(f.calls.some((c) => c[0] === "resize" && c[1] === 800));
  f.driver.stop();
  assert.equal(f.listeners.size, 0);
  assert.equal(f.canvasListeners.size, 0);
  assert.ok(f.calls.some((c) => c[0] === "clear"));
  assert.ok(f.calls.some((c) => c[0] === "free"));
  assert.equal(f.driver.step(), false);
});
