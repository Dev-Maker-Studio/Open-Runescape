import test from "node:test";
import assert from "node:assert/strict";
import { nativeTextChunks, nativeFrameSize, setup } from "../static/stream.js";

const helloBytes = (width, height) => new Uint8Array([0, ...new TextEncoder().encode(JSON.stringify({ width, height }))]);

test("native frame dimensions define the drawing-buffer input coordinate space", () => {
  assert.deepEqual(nativeFrameSize(helloBytes(765, 503)), [765, 503]);
  assert.equal(nativeFrameSize(new Uint8Array([1, 0])), null);
  assert.throws(() => nativeFrameSize(helloBytes(0, 503)), /invalid native frame size/);
  assert.throws(() => nativeFrameSize(helloBytes(765, 100000)), /invalid native frame size/);
});

test("native text respects the UTF-8 payload limit without splitting characters", () => {
  const text = "é".repeat(129);
  const chunks = nativeTextChunks(text);
  assert.equal(chunks.map((x) => x.slice(5)).join(""), text);
  assert.ok(chunks.every((x) => new TextEncoder().encode(x.slice(5)).length <= 128));
  assert.deepEqual(nativeTextChunks(""), []);
  assert.deepEqual(nativeTextChunks("abc"), ["Text:abc"]);
});

function controller(game = "native") {
  const events = new Map();
  const sent = [];
  globalThis.addEventListener = (name, fn) => events.set(name, fn);
  globalThis.location = { search: `?game=${game}&host=127.0.0.1:8101`, hostname: "127.0.0.1" };
  globalThis.document = { hasFocus: () => true };
  globalThis.WebSocket = class {
    static OPEN = 1;
    constructor() { this.readyState = 1; globalThis.lastSocket = this; }
    send(bytes) { sent.push(bytes); }
    close() { this.readyState = 3; }
  };
  const canvas = {
    width: 765,
    height: 503,
    addEventListener: (name, fn) => events.set(`canvas:${name}`, fn),
    getBoundingClientRect: () => ({ left: 0, top: 0, width: 765, height: 503 }),
  };
  setup({ disconnect() {} }, canvas);
  globalThis.lastSocket.onopen();
  sent.length = 0;
  return { events, sent };
}

const code = (bytes) => new TextDecoder().decode(bytes.slice(16));

test("physical key events and printable text are delivered separately once", () => {
  const { events, sent } = controller();
  events.get("keydown")({ code: "KeyA", repeat: false, preventDefault() {} });
  events.get("keypress")({ key: "a", isComposing: false, ctrlKey: false, metaKey: false });
  events.get("keyup")({ code: "KeyA" });
  assert.deepEqual(sent.map(code), ["KeyA", "Text:a", "KeyA"]);
  assert.deepEqual(sent.map((b) => b[5]), [0, 0, 1]);
  sent.length = 0;
  events.get("keypress")({ key: "c", isComposing: false, ctrlKey: true, metaKey: false });
  assert.equal(sent.length, 0);
  events.get("mouseup")({ target: {}, button: 0, clientX: 800, clientY: 510 });
  assert.equal(sent.length, 1);
  assert.equal(sent[0][5], 4);
  assert.equal(new DataView(sent[0].buffer).getFloat32(7, true), 800);
});

test("native streams preserve key repeats without synthesizing duplicate text", () => {
  const { events, sent } = controller();
  const key = (code, repeat) => events.get("keydown")({ code, repeat, preventDefault() {} });
  key("KeyA", false);
  events.get("keypress")({ key: "a", isComposing: false });
  key("KeyA", true);
  events.get("keypress")({ key: "a", isComposing: false });
  events.get("keyup")({ code: "KeyA" });
  key("Backspace", false);
  key("Backspace", true);
  key("Backspace", true);
  events.get("keyup")({ code: "Backspace" });
  assert.deepEqual(sent.map(code), ["KeyA", "Text:a", "KeyA", "Text:a", "KeyA", "Backspace", "Backspace", "Backspace", "Backspace"]);
  assert.deepEqual(sent.map((b) => b[5]), [0, 0, 0, 0, 1, 0, 0, 0, 1]);
});

test("test games keep their existing held-key repeat filtering", () => {
  const { events, sent } = controller("walker");
  events.get("keydown")({ code: "KeyA", repeat: false, preventDefault() {} });
  events.get("keydown")({ code: "KeyA", repeat: true, preventDefault() {} });
  events.get("keypress")({ key: "a", isComposing: false });
  events.get("keyup")({ code: "KeyA" });
  assert.deepEqual(sent.map(code), ["KeyA", "KeyA"]);
  assert.deepEqual(sent.map((b) => b[5]), [0, 1]);
});
