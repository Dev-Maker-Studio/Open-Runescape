import test from "node:test";
import assert from "node:assert/strict";
import { setImmediate } from "node:timers/promises";
import { assetProbe } from "../static/js5-probe.js";

const relay = "ws://127.0.0.1:8110/js5";

function fixture() {
  const state = { phase: 0, created: 0, loads: 0, freed: 0, closed: 0, sent: [], data: new Uint8Array([7, 8, 9]) };
  class Probe {
    constructor(...args) { state.args = args; state.created++; state.probe = this; }
    outgoing() { return new Uint8Array([state.phase ? 1 : 15]); }
    feed() { state.phase++; }
    complete() { return state.phase === 3; }
    report() { return JSON.stringify({ index: 12, archive: 2163, authenticated: false, files: [{ id: 3, bytes: state.data.length }] }); }
    file(id) { assert.equal(id, 3); return state.data; }
    finish() { if (!this.complete()) throw new Error("incomplete asset archive"); }
    free() { state.freed++; }
  }
  class Socket {
    constructor() { state.socket = this; }
    send(bytes) { state.sent.push([...bytes]); }
    close() { state.closed++; }
  }
  return { state, Probe, Socket, options: { load: async () => { state.loads++; return Probe; }, WebSocketClass: Socket, timeoutMs: 1000 } };
}

async function open(f) {
  await setImmediate();
  f.state.socket.onopen();
}

function message(f, data = new ArrayBuffer(1)) { f.state.socket.onmessage({ data }); }
function cleaned(f) {
  assert.equal(f.state.freed, 1);
  assert.equal(f.state.closed, 1);
  for (const handler of ["onopen", "onmessage", "onerror", "onclose"]) assert.equal(f.state.socket[handler], null);
}

test("asset wrapper retains bounded decoded files after freeing wasm probe", async () => {
  const f = fixture();
  const pending = assetProbe(relay, "test", 12, 2163, f.options);
  await open(f);
  for (let i = 0; i < 3; i++) message(f);
  const result = await pending;
  assert.deepEqual(f.state.args, ["test", 12, 2163]);
  assert.equal(result.report.authenticated, false);
  assert.deepEqual(result.files.get(3), new Uint8Array([7, 8, 9]));
  assert.equal(f.state.sent.length, 4);
  cleaned(f);
});

test("asset identity validation rejects JS integer wrapping before wasm allocation", async () => {
  const f = fixture();
  for (const [index, archive] of [[-1, 0], [255, 0], [2 ** 32, 0], [12, -1], [12, 2 ** 31], [12, NaN], [12, 0.5], ["12", 2163]]) {
    await assert.rejects(assetProbe(relay, "test", index, archive, f.options), /asset identity/);
  }
  assert.equal(f.state.socket, undefined);
  assert.equal(f.state.freed, 0);
});

test("asset session wire-byte limit rejects before feeding wasm", async () => {
  const f = fixture();
  const pending = assetProbe(relay, "test", 12, 2163, f.options);
  const rejected = assert.rejects(pending, /byte limit/);
  await open(f);
  message(f, new ArrayBuffer(12 * 1024 * 1024 + 1));
  await rejected;
  assert.equal(f.state.phase, 0);
  cleaned(f);
});

test("asset file collection enforces total expanded byte budget", async () => {
  const f = fixture();
  f.state.data = new Uint8Array(8 * 1024 * 1024 + 1);
  const pending = assetProbe(relay, "test", 12, 2163, f.options);
  const rejected = assert.rejects(pending, /file byte limit/);
  await open(f);
  for (let i = 0; i < 3; i++) message(f);
  await rejected;
  cleaned(f);
});

test("asset close cannot return a partial archive", async () => {
  const f = fixture();
  const pending = assetProbe(relay, "test", 12, 2163, f.options);
  const rejected = assert.rejects(pending, /incomplete asset archive/);
  await open(f);
  message(f);
  f.state.socket.onclose();
  await rejected;
  cleaned(f);
});

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}

function observedSignal(controller) {
  const active = new Set();
  return {
    active,
    get aborted() { return controller.signal.aborted; },
    addEventListener(event, listener, options) {
      assert.equal(event, "abort");
      active.add(listener);
      controller.signal.addEventListener(event, listener, options);
    },
    removeEventListener(event, listener) {
      assert.equal(event, "abort");
      active.delete(listener);
      controller.signal.removeEventListener(event, listener);
    },
  };
}

test("asset preabort and same-turn abort cannot load wasm or allocate a socket", async () => {
  for (const before of [true, false]) {
    const f = fixture();
    const controller = new AbortController();
    if (before) controller.abort();
    const pending = assetProbe(relay, "test", 2, 16, { ...f.options, signal: controller.signal });
    const rejected = assert.rejects(pending, /exchange cancelled/);
    if (!before) controller.abort();
    await rejected;
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.freed, 0);
    assert.equal(f.state.socket, undefined);
  }
});

test("asset abort while wasm loads removes subscriptions and ignores a late runtime", async () => {
  for (const lateFailure of [false, true]) {
    const f = fixture();
    const runtime = deferred();
    const controller = new AbortController();
    const signal = observedSignal(controller);
    let loads = 0;
    const pending = assetProbe(relay, "test", 2, 16, {
      ...f.options, signal, load: () => { loads++; return runtime.promise; },
    });
    const rejected = assert.rejects(pending, /exchange cancelled/);
    await setImmediate();
    assert.equal(loads, 1);
    controller.abort();
    await rejected;
    assert.equal(signal.active.size, 0);
    if (lateFailure) runtime.reject(new Error("FAKE_LATE_RUNTIME_ERROR"));
    else runtime.resolve(f.Probe);
    await setImmediate();
    assert.equal(f.state.created, 0);
    assert.equal(f.state.freed, 0);
    assert.equal(f.state.socket, undefined);
  }
});

test("asset runtime timeout does not allocate from a late runtime", async (t) => {
  t.mock.timers.enable({ apis: ["setTimeout"] });
  const f = fixture();
  const runtime = deferred();
  const controller = new AbortController();
  const signal = observedSignal(controller);
  const pending = assetProbe(relay, "test", 2, 16, {
    ...f.options, timeoutMs: 25, signal, load: () => runtime.promise,
  });
  const rejected = assert.rejects(pending, /runtime load timed out/);
  await Promise.resolve();
  t.mock.timers.tick(25);
  await rejected;
  assert.equal(signal.active.size, 0);
  runtime.resolve(f.Probe);
  await setImmediate();
  assert.equal(f.state.created, 0);
  assert.equal(f.state.socket, undefined);
});

test("asset runtime-load rejection removes cancellation subscriptions", async () => {
  const f = fixture();
  const controller = new AbortController();
  const signal = observedSignal(controller);
  await assert.rejects(assetProbe(relay, "test", 2, 16, {
    ...f.options, signal, load: async () => { throw new Error("fake runtime unavailable"); },
  }), /fake runtime unavailable/);
  assert.equal(signal.active.size, 0);
  assert.equal(f.state.created, 0);
  controller.abort();
});

test("asset abort immediately after loading still cannot construct a probe", async () => {
  const f = fixture();
  const controller = new AbortController();
  await assert.rejects(assetProbe(relay, "test", 2, 16, {
    ...f.options, signal: controller.signal,
    load: () => { controller.abort(); return f.Probe; },
  }), /exchange cancelled/);
  assert.equal(f.state.created, 0);
  assert.equal(f.state.socket, undefined);
});

test("asset abort before socket open and during exchange frees once and detaches stale handlers", async () => {
  for (const opened of [false, true]) {
    const f = fixture();
    const controller = new AbortController();
    const signal = observedSignal(controller);
    const pending = assetProbe(relay, "test", 2, 16, { ...f.options, signal });
    const rejected = assert.rejects(pending, /exchange cancelled/);
    await setImmediate();
    if (opened) { f.state.socket.onopen(); message(f); }
    const handlers = {
      open: f.state.socket.onopen, message: f.state.socket.onmessage,
      error: f.state.socket.onerror, close: f.state.socket.onclose,
    };
    const phase = f.state.phase;
    const sent = f.state.sent.length;
    controller.abort();
    await rejected;
    cleaned(f);
    assert.equal(signal.active.size, 0);
    handlers.open();
    handlers.message({ data: new ArrayBuffer(1) });
    handlers.error();
    handlers.close();
    controller.abort();
    await setImmediate();
    cleaned(f);
    assert.equal(f.state.phase, phase);
    assert.equal(f.state.sent.length, sent);
  }
});

test("asset exchange timeout frees exactly once and removes signal listeners", async (t) => {
  t.mock.timers.enable({ apis: ["setTimeout"] });
  const f = fixture();
  const controller = new AbortController();
  const signal = observedSignal(controller);
  const pending = assetProbe(relay, "test", 2, 16, { ...f.options, signal, timeoutMs: 40 });
  const rejected = assert.rejects(pending, /JS5 probe timed out/);
  await open(f);
  t.mock.timers.tick(40);
  await rejected;
  cleaned(f);
  assert.equal(signal.active.size, 0);
  controller.abort();
  t.mock.timers.tick(1000);
  cleaned(f);
});

test("asset cancellation inside the wasm probe constructor frees it without creating a socket", async () => {
  const f = fixture();
  const controller = new AbortController();
  class CancellingProbe extends f.Probe {
    constructor(...args) { super(...args); controller.abort(); }
  }
  await assert.rejects(assetProbe(relay, "test", 2, 16, {
    ...f.options, signal: controller.signal, load: async () => CancellingProbe,
  }), /exchange cancelled/);
  assert.equal(f.state.created, 1);
  assert.equal(f.state.freed, 1);
  assert.equal(f.state.socket, undefined);
});

test("asset cancellation inside the websocket constructor closes the returned socket and frees wasm", async () => {
  const f = fixture();
  const controller = new AbortController();
  const signal = observedSignal(controller);
  class CancellingSocket extends f.Socket {
    constructor(...args) { super(...args); controller.abort(); }
  }
  await assert.rejects(assetProbe(relay, "test", 2, 16, {
    ...f.options, signal, WebSocketClass: CancellingSocket,
  }), /exchange cancelled/);
  cleaned(f);
  assert.equal(signal.active.size, 0);
  assert.equal(f.state.sent.length, 0);
});

test("asset successful completion transfers file ownership and a later abort cannot free twice", async () => {
  const f = fixture();
  const controller = new AbortController();
  const signal = observedSignal(controller);
  const pending = assetProbe(relay, "test", 2, 16, { ...f.options, signal });
  await open(f);
  for (let i = 0; i < 3; i++) message(f);
  const result = await pending;
  cleaned(f);
  assert.equal(signal.active.size, 0);
  controller.abort();
  await setImmediate();
  cleaned(f);
  assert.deepEqual(result.files.get(3), new Uint8Array([7, 8, 9]));
  result.files.get(3).fill(0);
  result.files.clear();
});

test("asset connection and binary-format errors detach handlers and free the probe", async () => {
  for (const fail of [
    (f) => f.state.socket.onerror(),
    (f) => message(f, "not binary"),
    (f) => { f.state.probe.report = () => "not JSON"; for (let i = 0; i < 3; i++) message(f); },
  ]) {
    const f = fixture();
    const controller = new AbortController();
    const signal = observedSignal(controller);
    const pending = assetProbe(relay, "test", 2, 16, { ...f.options, signal });
    const rejected = assert.rejects(pending);
    await open(f);
    fail(f);
    await rejected;
    cleaned(f);
    assert.equal(signal.active.size, 0);
  }
});

test("asset websocket constructor failure still releases the allocated wasm probe", async () => {
  const f = fixture();
  const controller = new AbortController();
  const signal = observedSignal(controller);
  class ThrowingSocket { constructor() { throw new Error("fake socket constructor failure"); } }
  await assert.rejects(assetProbe(relay, "test", 2, 16, {
    ...f.options, signal, WebSocketClass: ThrowingSocket,
  }), /fake socket constructor failure/);
  assert.equal(f.state.created, 1);
  assert.equal(f.state.freed, 1);
  assert.equal(f.state.socket, undefined);
  assert.equal(signal.active.size, 0);
});

test("asset length-validation failure wipes both earlier and offending materialized file buffers", async () => {
  const f = fixture();
  const buffers = [new Uint8Array([7, 8, 9]), new Uint8Array([1, 2])];
  const pending = assetProbe(relay, "test", 2, 16, f.options);
  const rejected = assert.rejects(pending, /asset file byte limit/);
  await open(f);
  f.state.probe.report = () => JSON.stringify({ files: [{ id: 0, bytes: 3 }, { id: 1, bytes: 3 }] });
  f.state.probe.file = (id) => buffers[id];
  for (let i = 0; i < 3; i++) message(f);
  await rejected;
  for (const bytes of buffers) assert.ok(bytes.every((byte) => byte === 0));
  cleaned(f);
});

test("asset cumulative expansion failure wipes every returned buffer including the one exceeding the limit", async () => {
  const f = fixture();
  const buffers = [new Uint8Array(4 * 1024 * 1024).fill(7), new Uint8Array(4 * 1024 * 1024 + 1).fill(9)];
  const pending = assetProbe(relay, "test", 2, 16, f.options);
  const rejected = assert.rejects(pending, /asset file byte limit/);
  await open(f);
  f.state.probe.report = () => JSON.stringify({ files: buffers.map((bytes, id) => ({ id, bytes: bytes.length })) });
  f.state.probe.file = (id) => buffers[id];
  for (let i = 0; i < 3; i++) message(f);
  await rejected;
  for (const bytes of buffers) assert.ok(bytes.every((byte) => byte === 0));
  cleaned(f);
});

test("asset duplicate IDs reject before rematerializing an orphan buffer and wipe the first file", async () => {
  const f = fixture();
  const bytes = new Uint8Array([7, 8, 9]);
  let calls = 0;
  const pending = assetProbe(relay, "test", 2, 16, f.options);
  const rejected = assert.rejects(pending, /duplicate asset file ID/);
  await open(f);
  f.state.probe.report = () => JSON.stringify({ files: [{ id: 3, bytes: 3 }, { id: 3, bytes: 3 }] });
  f.state.probe.file = (id) => { assert.equal(id, 3); calls++; return bytes; };
  for (let i = 0; i < 3; i++) message(f);
  await rejected;
  assert.equal(calls, 1);
  assert.deepEqual(bytes, new Uint8Array(3));
  cleaned(f);
});

test("asset later file materialization errors wipe previously collected definitions", async () => {
  for (const invalidType of [false, true]) {
    const f = fixture();
    const bytes = new Uint8Array([7, 8, 9]);
    const pending = assetProbe(relay, "test", 2, 16, f.options);
    const rejected = assert.rejects(pending, invalidType ? /asset file bytes required/ : /fake file decoding failed/);
    await open(f);
    f.state.probe.report = () => JSON.stringify({ files: [{ id: 0, bytes: 3 }, { id: 1, bytes: 3 }] });
    f.state.probe.file = (id) => {
      if (id === 0) return bytes;
      if (invalidType) return [1, 2, 3];
      throw new Error("fake file decoding failed");
    };
    for (let i = 0; i < 3; i++) message(f);
    await rejected;
    assert.deepEqual(bytes, new Uint8Array(3));
    cleaned(f);
  }
});
