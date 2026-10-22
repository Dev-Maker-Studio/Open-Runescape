import test from "node:test";
import assert from "node:assert/strict";
import { setImmediate } from "node:timers/promises";
import { gameHandshakeProbe } from "../static/js5-probe.js";

function fixture() {
  const state = { sent: [], freed: 0, closed: 0, sentHandshake: false, complete: false };
  class Probe {
    outgoing() {
      if (state.sentHandshake) return new Uint8Array();
      state.sentHandshake = true;
      return new Uint8Array([14]);
    }
    feed(bytes) {
      if (bytes.length !== 1 || bytes[0] !== 0) throw new Error("game handshake rejected");
      state.complete = true;
    }
    complete() { return state.complete; }
    report() { return JSON.stringify({ accepted: true, authenticated: false, scope: "original-login-handshake-only" }); }
    finish() { if (!state.complete) throw new Error("incomplete game handshake"); }
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

function cleaned(f) {
  assert.equal(f.state.freed, 1);
  assert.equal(f.state.closed, 1);
  assert.equal(f.state.socket.onmessage, null);
  assert.equal(f.state.socket.onclose, null);
}

test("lobby and world probes send only the original handshake and close before login", async () => {
  for (const path of ["lobby", "world"]) {
    const f = fixture();
    const pending = gameHandshakeProbe(`ws://127.0.0.1:8110/${path}`, f.options);
    await opened(f);
    assert.deepEqual(f.state.sent, [[14]]);
    assert.equal(f.state.socket.binaryType, "arraybuffer");
    f.state.socket.onmessage({ data: new Uint8Array([0]).buffer });
    assert.deepEqual(await pending, { accepted: true, authenticated: false, scope: "original-login-handshake-only" });
    assert.deepEqual(f.state.sent, [[14]]);
    cleaned(f);
  }
});

test("handshake probes reject non-loopback routes and all URL selectors before loading", async () => {
  const f = fixture();
  for (const url of ["ws://example.com/lobby", "ws://127.0.0.1.evil/world", "https://localhost/world", "ws://localhost/js5", "ws://localhost/lobby?upstream=x", "ws://localhost/world#x", "ws://user@localhost/lobby", "ws://localhost/world/"]) {
    await assert.rejects(gameHandshakeProbe(url, f.options), /relay URL/);
  }
  assert.equal(f.state.socket, undefined);
});

test("text oversized and rejected responses close and free the probe", async () => {
  for (const data of ["text", new Uint8Array([0, 0]).buffer, new Uint8Array([3]).buffer]) {
    const f = fixture();
    const pending = gameHandshakeProbe("ws://localhost/lobby", f.options);
    const rejected = assert.rejects(pending, /binary|byte limit|rejected/);
    await opened(f);
    f.state.socket.onmessage({ data });
    await rejected;
    cleaned(f);
  }
});

test("early close and socket errors cannot authenticate", async () => {
  for (const event of ["onclose", "onerror"]) {
    const f = fixture();
    const pending = gameHandshakeProbe("ws://localhost/world", f.options);
    const rejected = assert.rejects(pending, /incomplete|connection failed/);
    await opened(f);
    f.state.socket[event]();
    await rejected;
    cleaned(f);
  }
});

test("handshake timeout closes transport and frees wasm", async () => {
  const f = fixture();
  const pending = gameHandshakeProbe("ws://localhost/lobby", { ...f.options, timeoutMs: 20 });
  const rejected = assert.rejects(pending, /timed out/);
  await opened(f);
  await rejected;
  cleaned(f);
});

test("runtime load timeout and invalid timeouts do not allocate sockets", async () => {
  const f = fixture();
  for (const timeoutMs of [0, -1, Infinity, NaN, 30001]) {
    await assert.rejects(gameHandshakeProbe("ws://localhost/lobby", { ...f.options, timeoutMs }), /bounded/);
  }
  await assert.rejects(gameHandshakeProbe("ws://localhost/lobby", { ...f.options, load: () => new Promise(() => {}), timeoutMs: 10 }), /load timed out/);
  assert.equal(f.state.socket, undefined);
  assert.equal(f.state.freed, 0);
});

test("socket construction failure frees the probe", async () => {
  const f = fixture();
  await assert.rejects(gameHandshakeProbe("ws://localhost/lobby", { ...f.options, WebSocketClass: class { constructor() { throw new Error("socket construction failed"); } } }), /construction failed/);
  assert.equal(f.state.freed, 1);
});
