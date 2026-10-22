import test from "node:test";
import assert from "node:assert/strict";
import { setImmediate } from "node:timers/promises";
import { protocolProbe } from "../static/js5-probe.js";

const relay = "ws://127.0.0.1:8110/js5";

function fixture() {
  const state = { sent: [], feeds: [], freed: 0, closed: 0, phase: 0 };
  class Probe {
    outgoing() { return new Uint8Array([state.phase ? 6 : 15]); }
    feed(bytes) { state.feeds.push([...bytes]); state.phase++; }
    complete() { return state.phase === 2; }
    report() { return JSON.stringify({ revision: 727, authenticated: false }); }
    finish() { if (!this.complete()) throw new Error("incomplete version table"); }
    free() { state.freed++; }
  }
  class Socket {
    constructor(url) { state.socket = this; state.url = url; }
    send(bytes) { state.sent.push([...bytes]); }
    close() { state.closed++; }
  }
  return { state, options: { load: async () => Probe, WebSocketClass: Socket, timeoutMs: 1000 } };
}

async function opened(f) {
  await setImmediate();
  f.state.socket.onopen();
}

const message = (f, bytes) => f.state.socket.onmessage({ data: new Uint8Array(bytes).buffer });

function cleaned(f) {
  assert.equal(f.state.freed, 1);
  assert.equal(f.state.closed, 1);
  assert.equal(f.state.socket.onmessage, null);
  assert.equal(f.state.socket.onclose, null);
}

test("browser wrapper forwards binary data and always frees completed wasm state", async () => {
  const f = fixture();
  const pending = protocolProbe(relay, "test", f.options);
  await opened(f);
  assert.equal(f.state.socket.binaryType, "arraybuffer");
  assert.deepEqual(f.state.sent, [[15]]);
  message(f, [0, 1]);
  message(f, [255, 2]);
  assert.deepEqual(await pending, { revision: 727, authenticated: false });
  assert.deepEqual(f.state.feeds, [[0, 1], [255, 2]]);
  cleaned(f);
});

test("browser cannot choose a non-loopback relay or route", async () => {
  const f = fixture();
  for (const url of ["ws://example.com/js5", "ws://127.0.0.1.evil/js5", "http://localhost/js5", "ws://localhost/world", "ws://localhost/js5?upstream=x", "ws://localhost/js5#x", "ws://user@localhost/js5"]) {
    await assert.rejects(protocolProbe(url, "test", f.options), /loopback/);
  }
  assert.equal(f.state.socket, undefined);
});

test("text and oversized messages reject with cleanup", async () => {
  for (const data of ["text", new ArrayBuffer(128 * 1024 + 1)]) {
    const f = fixture();
    const pending = protocolProbe(relay, "test", f.options);
    const rejected = assert.rejects(pending, /binary|byte limit/);
    await opened(f);
    f.state.socket.onmessage({ data });
    await rejected;
    cleaned(f);
  }
});

test("premature close and connection errors cannot succeed", async () => {
  for (const event of ["onclose", "onerror"]) {
    const f = fixture();
    const pending = protocolProbe(relay, "test", f.options);
    const rejected = assert.rejects(pending, /incomplete|connection failed/);
    await opened(f);
    f.state.socket[event]();
    await rejected;
    cleaned(f);
  }
});

test("session timeout closes socket and frees wasm state", async () => {
  const f = fixture();
  f.options.timeoutMs = 20;
  const pending = protocolProbe(relay, "test", f.options);
  const rejected = assert.rejects(pending, /timed out/);
  await opened(f);
  await rejected;
  cleaned(f);
});

test("runtime load is bounded without allocating a socket", async () => {
  const f = fixture();
  f.options.load = () => new Promise(() => {});
  f.options.timeoutMs = 10;
  await assert.rejects(protocolProbe(relay, "test", f.options), /load timed out/);
  assert.equal(f.state.socket, undefined);
  assert.equal(f.state.freed, 0);
});

test("constructor failures free wasm allocation", async () => {
  const f = fixture();
  f.options.WebSocketClass = class { constructor() { throw new Error("socket creation failed"); } };
  await assert.rejects(protocolProbe(relay, "test", f.options), /socket creation failed/);
  assert.equal(f.state.freed, 1);
});

test("missing token and unbounded timeout are rejected before loading", async () => {
  const f = fixture();
  for (const timeoutMs of [0, Infinity, 30001]) {
    await assert.rejects(protocolProbe(relay, "test", { ...f.options, timeoutMs }), /bounded/);
  }
  await assert.rejects(protocolProbe(relay, "", f.options), /token/);
  assert.equal(f.state.socket, undefined);
});

test("pre-aborted probe does not load runtime or allocate", async () => {
  const f = fixture();
  const abort = new AbortController();
  abort.abort("private cancellation reason");
  f.options.load = () => { assert.fail("runtime must not load"); };
  await assert.rejects(protocolProbe(relay, "test", { ...f.options, signal: abort.signal }), { message: "protocol exchange cancelled" });
  assert.equal(f.state.socket, undefined);
  assert.equal(f.state.freed, 0);
});

test("abort during runtime load cannot allocate a late probe or socket", async () => {
  const f = fixture();
  const abort = new AbortController();
  let loaded;
  const Probe = await f.options.load();
  f.options.load = () => new Promise((resolve) => { loaded = resolve; });
  const pending = protocolProbe(relay, "test", { ...f.options, signal: abort.signal });
  const rejected = assert.rejects(pending, { message: "protocol exchange cancelled" });
  await setImmediate();
  abort.abort("private cancellation reason");
  await rejected;
  loaded(Probe);
  await setImmediate();
  assert.equal(f.state.socket, undefined);
  assert.equal(f.state.freed, 0);
});

test("abort on runtime completion or probe allocation cannot open a socket", async () => {
  for (const phase of ["load", "create"]) {
    const f = fixture();
    const abort = new AbortController();
    const Probe = await f.options.load();
    f.options.load = async () => {
      if (phase === "load") abort.abort("private cancellation reason");
      return class extends Probe {
        constructor() { super(); abort.abort("private cancellation reason"); }
      };
    };
    await assert.rejects(protocolProbe(relay, "test", { ...f.options, signal: abort.signal }), { message: "protocol exchange cancelled" });
    assert.equal(f.state.socket, undefined);
    assert.equal(f.state.freed, phase === "create" ? 1 : 0);
  }
});

test("abort during socket creation closes the late socket and frees once", async () => {
  const f = fixture();
  const abort = new AbortController();
  const Socket = f.options.WebSocketClass;
  f.options.WebSocketClass = class extends Socket {
    constructor(url) { super(url); abort.abort("private cancellation reason"); }
  };
  await assert.rejects(protocolProbe(relay, "test", { ...f.options, signal: abort.signal }), { message: "protocol exchange cancelled" });
  cleaned(f);
});

test("abort during exchange detaches handlers and late callbacks cannot publish", async () => {
  const f = fixture();
  const abort = new AbortController();
  const pending = protocolProbe(relay, "test", { ...f.options, signal: abort.signal });
  const rejected = assert.rejects(pending, { message: "protocol exchange cancelled" });
  await opened(f);
  const lateMessage = f.state.socket.onmessage;
  const lateOpen = f.state.socket.onopen;
  abort.abort("private cancellation reason");
  await rejected;
  lateMessage({ data: new Uint8Array([1]).buffer });
  lateOpen();
  assert.deepEqual(f.state.feeds, []);
  assert.deepEqual(f.state.sent, [[15]]);
  cleaned(f);
});

test("completion detaches abort ownership without double cleanup", async () => {
  const f = fixture();
  const abort = new AbortController();
  const pending = protocolProbe(relay, "test", { ...f.options, signal: abort.signal });
  await opened(f);
  message(f, [1]);
  message(f, [2]);
  await pending;
  abort.abort();
  cleaned(f);
});
