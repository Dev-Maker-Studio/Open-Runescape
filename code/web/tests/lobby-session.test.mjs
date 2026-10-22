import test from "node:test";
import assert from "node:assert/strict";
import { setImmediate, setTimeout as delay } from "node:timers/promises";
import { openLobbySession, takePrivateLobbyConfig } from "../static/lobby-session.js";

function fixture() {
  const state = { sent: [], marked: 0, closed: 0, freed: 0, phase: "sendHandshake", operations: [], outgoingViews: [], received: [] };
  class Session {
    constructor(config) { state.config = config; }
    phase() { return state.phase; }
    outgoing() {
      let bytes = new Uint8Array();
      if (state.phase === "sendHandshake") { bytes = new Uint8Array([14]); state.phase = "awaitHandshake"; }
      else if (state.phase === "sendRequest") { bytes = new Uint8Array([19, 0, 1, 42]); state.phase = "awaitResponse"; }
      else if (state.pending) { bytes = state.pending; state.pending = null; }
      state.outgoingViews.push(bytes);
      return bytes;
    }
    feed(bytes) {
      state.received.push([...bytes]);
      if (state.phase === "awaitHandshake") {
        assert.equal(bytes[0], 0);
        state.phase = "sendRequest";
        return JSON.stringify({ consumed: 1, event: null });
      }
      if (state.terminalEvent) {
        state.phase = "rejected";
        return JSON.stringify({ consumed: 1, event: state.terminalEvent });
      }
      if (state.phase === "awaitResponse") {
        assert.equal(bytes[0], 2);
        state.phase = "lobby";
        return JSON.stringify({ consumed: 1, event: { kind: "loginAccepted" } });
      }
      if (state.zeroProgress) return JSON.stringify({ consumed: 0, event: null });
      if (state.rejectKind) return JSON.stringify({ consumed: 1, event: { kind: state.rejectKind } });
      if (bytes[0] === 103) {
        state.worldList = { version: 4, worlds: [{ worldNumber: 1 }] };
        return JSON.stringify({ consumed: 1, event: { kind: "worldListUpdated" } });
      }
      return JSON.stringify({ consumed: bytes.length, event: { kind: "opaquePacket" } });
    }
    report() { return JSON.stringify({ phase: state.phase, accepted: state.phase === "lobby", worldList: state.worldList ?? null }); }
    markFlushed() { assert.equal(state.phase, "lobby"); state.marked++; }
    confirm() { state.operations.push("confirm"); }
    requestWorldList(version) { state.operations.push(version); state.pending = new Uint8Array([47]); }
    tick() { state.operations.push("tick"); }
    finish() { if (state.phase !== "lobby") throw new Error("FAKE_PRIVATE_VALUE"); }
    close() { state.closed++; }
    free() { state.freed++; }
  }
  class Socket {
    constructor(url) { state.socket = this; state.url = url; this.readyState = 0; this.bufferedAmount = 0; }
    send(bytes) { state.sent.push([...bytes]); }
    close() { state.socketClosed = (state.socketClosed ?? 0) + 1; this.readyState = 3; }
  }
  return { state, options: { load: async () => Session, WebSocketClass: Socket, timeoutMs: 1000, lifetimeMs: 2000 } };
}

async function opened(f) {
  const handle = await openLobbySession("ws://127.0.0.1:8110/lobby", "{}", f.options);
  f.state.socket.readyState = 1;
  f.state.socket.onopen();
  return handle;
}

function message(f, bytes) {
  const data = new Uint8Array(bytes).buffer;
  f.state.socket.onmessage({ data });
  assert.ok(new Uint8Array(data).every((byte) => byte === 0));
}

function cleaned(f) {
  assert.equal(f.state.freed, 1);
  assert.equal(f.state.closed, 1);
  assert.equal(f.state.socketClosed, 1);
  assert.equal(f.state.socket.onmessage, null);
}

test("original lobby socket gates login on handshake and consumes coalesced boundaries", async () => {
  const f = fixture();
  const handle = await opened(f);
  assert.deepEqual(f.state.sent, [[14]]);
  await delay(10);
  assert.equal(f.state.marked, 0);
  message(f, [0, 2, 103, 29]);
  assert.equal((await handle.ready).accepted, true);
  assert.deepEqual(f.state.sent, [[14], [19, 0, 1, 42]]);
  assert.deepEqual(handle.state().worldList, { version: 4, worlds: [{ worldNumber: 1 }] });
  assert.equal(handle.state().opaquePackets, 1);
  assert.equal(handle.state().receivedBytes, 4);
  assert.ok(f.state.outgoingViews.every((bytes) => bytes.every((byte) => byte === 0)));
  await delay(10);
  assert.equal(f.state.marked, 1);
  handle.close();
  assert.equal(handle.state().accepted, false);
  assert.equal(handle.state().connected, false);
  cleaned(f);
});

test("lobby flush acknowledgement waits until websocket queue drains", async () => {
  const f = fixture();
  const handle = await opened(f);
  f.state.socket.bufferedAmount = 100;
  message(f, [0, 2]);
  await handle.ready;
  await delay(15);
  assert.equal(f.state.marked, 0);
  f.state.socket.bufferedAmount = 0;
  await delay(15);
  assert.equal(f.state.marked, 1);
  handle.requestWorldList(-1);
  handle.tick();
  assert.deepEqual(f.state.operations, [-1, "tick"]);
  assert.deepEqual(f.state.sent.at(-1), [47]);
  assert.throws(() => handle.requestWorldList(0x100000000), /signed/);
  handle.close();
});

test("lobby URLs and bounds reject before loading secrets or opening sockets", async () => {
  const f = fixture();
  let loads = 0;
  const options = { ...f.options, load: async () => { loads++; return class {}; } };
  for (const url of ["ws://example.com/lobby", "https://localhost/lobby", "ws://localhost/world", "ws://localhost/lobby?x=1", "ws://user@localhost/lobby", "ws://localhost/lobby#x", "ws://localhost/lobby/"]) {
    await assert.rejects(openLobbySession(url, "{}", options), /relay URL/);
  }
  for (const timeoutMs of [0, 30001, Infinity, 1.5]) {
    await assert.rejects(openLobbySession("ws://localhost/lobby", "{}", { ...options, timeoutMs }), /bounded/);
  }
  await assert.rejects(openLobbySession("ws://localhost/lobby", "{}", { ...options, lifetimeMs: 300001 }), /bounded/);
  await assert.rejects(openLobbySession("ws://localhost/lobby", "x".repeat(65537), options), /configuration/);
  assert.equal(loads, 0);
  assert.equal(f.state.socket, undefined);
});

test("lobby malformed binary and decoder progress fail closed with static errors", async () => {
  for (const data of ["FAKE_PRIVATE_VALUE", new Uint8Array(65537).buffer]) {
    const f = fixture();
    const handle = await opened(f);
    const rejected = assert.rejects(handle.ready, /^Error: invalid original lobby response$/);
    f.state.socket.onmessage({ data });
    await rejected;
    cleaned(f);
  }
  const f = fixture();
  const handle = await opened(f);
  message(f, [0, 2]);
  await handle.ready;
  f.state.zeroProgress = true;
  message(f, [29]);
  assert.equal(handle.state().phase, "failed");
  cleaned(f);
});

test("lobby rejection does not trigger retry or automatic confirmation", async () => {
  for (const kind of ["rejected", "queued", "retryRequested", "reconnectRequired"]) {
    const f = fixture();
    const handle = await opened(f);
    message(f, [0, 2]);
    await handle.ready;
    f.state.rejectKind = kind;
    message(f, [42]);
    assert.equal(handle.state().error, `original lobby ${kind}`);
    assert.deepEqual(f.state.operations, []);
    cleaned(f);
  }
});

test("lobby rejection reports only an exact bounded typed response code", async () => {
  for (const responseCode of [0, 3, 29, 45, 255]) {
    const f = fixture();
    const handle = await opened(f);
    message(f, [0]);
    f.state.terminalEvent = { kind: "rejected", responseCode, detail: "FAKE_PRIVATE_DETAIL", password: "FAKE_PRIVATE_PASSWORD", extraInfo: 123456 };
    const rejected = assert.rejects(handle.ready, { message: `original lobby rejected (code ${responseCode})` });
    message(f, [3]);
    await rejected;
    assert.equal(handle.state().responseCode, responseCode);
    assert.equal(handle.state().outcome, "rejected");
    assert.equal(handle.state().error, `original lobby rejected (code ${responseCode})`);
    for (const key of ["detail", "password", "extraInfo"]) assert.equal(Object.hasOwn(handle.state(), key), false);
    assert.equal(JSON.stringify(handle.state()).includes("FAKE_PRIVATE"), false);
    handle.close();
    assert.equal(handle.state().responseCode, responseCode);
    cleaned(f);
  }
});

test("invalid or unrelated terminal codes are ignored without coercion or event copying", async () => {
  for (const responseCode of [undefined, null, -1, 256, 1.5, "3", true, {}, [], { responseCode: 3, password: "FAKE_PRIVATE_PASSWORD" }, "FAKE_PRIVATE_PASSWORD"]) {
    const f = fixture();
    const handle = await opened(f);
    message(f, [0]);
    f.state.terminalEvent = { kind: "rejected", responseCode, code: 3, response_code: 3, detail: "FAKE_PRIVATE_DETAIL" };
    const rejected = assert.rejects(handle.ready, { message: "original lobby rejected" });
    message(f, [3]);
    await rejected;
    assert.equal(Object.hasOwn(handle.state(), "responseCode"), false);
    assert.equal(JSON.stringify(handle.state()).includes("FAKE_PRIVATE"), false);
    assert.equal(Object.hasOwn(handle.state(), "code"), false);
    assert.equal(Object.hasOwn(handle.state(), "response_code"), false);
    cleaned(f);
  }
  for (const kind of ["queued", "retryRequested", "reconnectRequired"]) {
    const f = fixture();
    const handle = await opened(f);
    message(f, [0]);
    f.state.terminalEvent = { kind, responseCode: 3, detail: "FAKE_PRIVATE_DETAIL" };
    const rejected = assert.rejects(handle.ready, { message: `original lobby ${kind}` });
    message(f, [3]);
    await rejected;
    assert.equal(Object.hasOwn(handle.state(), "responseCode"), false);
    assert.equal(JSON.stringify(handle.state()).includes("FAKE_PRIVATE"), false);
    cleaned(f);
  }
});

test("lobby authentication timeout and early socket close release state once", async () => {
  const f = fixture();
  f.options.timeoutMs = 15;
  const handle = await opened(f);
  await assert.rejects(handle.ready, /authentication timed out/);
  handle.close();
  cleaned(f);
  const early = fixture();
  const earlyHandle = await opened(early);
  early.state.socket.onclose();
  await assert.rejects(earlyHandle.ready, /^Error: original lobby connection ended unexpectedly$/);
  cleaned(early);
});

test("lobby lifetime closes accepted sessions and no automatic ticks are invented", async () => {
  const f = fixture();
  f.options.timeoutMs = 10;
  f.options.lifetimeMs = 35;
  const handle = await opened(f);
  message(f, [0, 2]);
  await handle.ready;
  await delay(50);
  assert.equal(handle.state().error, "original lobby diagnostic lifetime reached");
  assert.deepEqual(f.state.operations, []);
  cleaned(f);
});

test("lobby runtime and socket errors do not leak thrown private values", async () => {
  const f = fixture();
  await assert.rejects(openLobbySession("ws://localhost/lobby", "{}", { ...f.options, load: async () => class { constructor() { throw new Error("FAKE_PRIVATE_VALUE"); } } }), /^Error: invalid private lobby configuration$/);
  await assert.rejects(openLobbySession("ws://localhost/lobby", "{}", { ...f.options, WebSocketClass: class { constructor() { throw new Error("FAKE_PRIVATE_VALUE"); } } }), /^Error: original lobby socket construction failed$/);
  assert.equal(f.state.freed, 1);
});

test("private lobby bootstrap uses a fixed single-use POST without URL credentials", async () => {
  let request;
  const secret = "{\"fake\":\"OFFLINE_ONLY\"}";
  const returned = await takePrivateLobbyConfig({ origin: "http://127.0.0.1:8092", fetcher: async (url, options) => {
    request = { url, options };
    return new Response(secret);
  } });
  assert.equal(returned, secret);
  assert.equal(request.url, "http://127.0.0.1:8092/__openrs/dev-login");
  assert.equal(request.options.method, "POST");
  assert.deepEqual(request.options.headers, { "Content-Type": "application/json", "X-OpenRS-Intent": "original-login" });
  assert.equal(request.options.redirect, "error");
  assert.equal(request.options.credentials, "omit");
  assert.equal(request.options.cache, "no-store");
  assert.equal(request.options.body, "{}");
});

test("private bootstrap rejects remote origins and bounds response before retaining text", async () => {
  let calls = 0;
  const fetcher = async () => { calls++; return new Response("{}"); };
  for (const origin of ["https://example.com", "http://127.0.0.1.evil", "https://localhost"]) {
    await assert.rejects(takePrivateLobbyConfig({ origin, fetcher }), /loopback/);
  }
  assert.equal(calls, 0);
  const overflow = new Uint8Array(65537).fill(88);
  await assert.rejects(takePrivateLobbyConfig({ origin: "http://localhost", fetcher: async () => new Response(new ReadableStream({ start(controller) { controller.enqueue(overflow); controller.close(); } })) }), /invalid private lobby bootstrap body/);
  assert.ok(overflow.every((byte) => byte === 0));
  await assert.rejects(takePrivateLobbyConfig({ origin: "http://localhost", fetcher: async () => new Response("FAKE_PRIVATE_VALUE", { status: 403 }) }), /^Error: private lobby bootstrap unavailable$/);
});
