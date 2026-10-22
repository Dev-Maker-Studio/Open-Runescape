import test from "node:test";
import assert from "node:assert/strict";
import { setImmediate } from "node:timers/promises";
import { scriptGraphProbe } from "../static/js5-probe.js";

const relay = "ws://127.0.0.1:8110/js5";
function fixture() {
  const state = { phase: 0, freed: 0, closed: 0, calls: 0, bytes: new Uint8Array([1, 2, 3]), scripts: null };
  class Probe {
    constructor(...args) { state.args = args; }
    outgoing() { return new Uint8Array([15]); }
    feed() { state.phase++; }
    complete() { return state.phase === 2; }
    report() { return JSON.stringify({ root: 1, authenticated: false, scripts: state.scripts ?? [{ id: 1, bytes: state.bytes.length }] }); }
    file() { assert.equal(state.freed, 0); return state.bytes; }
    run(ints) { assert.equal(state.freed, 0); state.calls++; state.ints = JSON.parse(ints); return JSON.stringify({ ints: [42] }); }
    free() { state.freed++; }
    finish() { if (!this.complete()) throw new Error("incomplete script graph"); }
  }
  class Socket {
    constructor() { state.socket = this; }
    send() {}
    close() { state.closed++; }
  }
  return { state, options: { load: async () => Probe, WebSocketClass: Socket, timeoutMs: 1000 } };
}
async function open(f) { await setImmediate(); f.state.socket.onopen(); }
function message(f, bytes = new ArrayBuffer(1)) { f.state.socket.onmessage({ data: bytes }); }
function cleaned(f) { assert.equal(f.state.freed, 1); assert.equal(f.state.closed, 1); assert.equal(f.state.socket.onmessage, null); }

test("graph loading does not invent arguments or execute scripts", async () => {
  const f = fixture();
  const pending = scriptGraphProbe(relay, "test", 1, f.options);
  await open(f);
  message(f); message(f);
  const result = await pending;
  assert.deepEqual(f.state.args, ["test", 1]);
  assert.equal(result.execution, null);
  assert.equal(f.state.calls, 0);
  assert.deepEqual(result.files.get(1), new Uint8Array([1, 2, 3]));
  cleaned(f);
});

test("explicit arguments execute while the wasm repository remains alive", async () => {
  const f = fixture();
  const pending = scriptGraphProbe(relay, "test", 1, { ...f.options, ints: [-2147483648, 2147483647] });
  await open(f); message(f); message(f);
  const result = await pending;
  assert.deepEqual(result.execution, { ints: [42] });
  assert.deepEqual(f.state.ints, [-2147483648, 2147483647]);
  assert.equal(f.state.calls, 1);
  cleaned(f);
});

test("graph IDs and arguments reject JavaScript integer wrapping before wasm allocation", async () => {
  const f = fixture();
  for (const id of [-1, 1000001, 2 ** 32, 0.5, NaN, "1"]) await assert.rejects(scriptGraphProbe(relay, "test", id, f.options), /script identity/);
  for (const ints of [null, "[]", [2 ** 31], [-2147483649], [NaN], [0.1], ["0"], new Array(1001).fill(0)]) {
    await assert.rejects(scriptGraphProbe(relay, "test", 1, { ...f.options, ints }), /integer arguments/);
  }
  assert.equal(f.state.socket, undefined);
});

test("graph collection enforces count, per-file and cumulative byte bounds", async () => {
  for (const kind of ["count", "file", "total"]) {
    const f = fixture();
    if (kind === "count") f.state.scripts = Array.from({ length: 65 }, (_, id) => ({ id, bytes: 3 }));
    else if (kind === "file") f.state.bytes = new Uint8Array(65537);
    else { f.state.bytes = new Uint8Array(65536); f.state.scripts = Array.from({ length: 17 }, (_, id) => ({ id, bytes: 65536 })); }
    const pending = scriptGraphProbe(relay, "test", 1, f.options);
    const rejected = assert.rejects(pending, /limit/);
    await open(f); message(f); message(f);
    await rejected; cleaned(f);
  }
});

test("oversized graph wire and incomplete close discard all results", async () => {
  for (const close of [false, true]) {
    const f = fixture();
    const pending = scriptGraphProbe(relay, "test", 1, f.options);
    const rejected = assert.rejects(pending, close ? /incomplete/ : /byte limit/);
    await open(f);
    if (close) { message(f); f.state.socket.onclose(); }
    else message(f, new ArrayBuffer(16 * 1024 * 1024 + 1));
    await rejected; cleaned(f);
    assert.equal(f.state.calls, 0);
  }
});
