import test from "node:test";
import assert from "node:assert/strict";
import { openLobbySession } from "../static/lobby-session.js";

function fixture() {
  const state = { sockets: [], sent: [], lobbyClosed: 0, lobbyFreed: 0, worldClosed: 0, worldFreed: 0, worldCreated: 0, views: [], worldLegacyStarts: 0, worldStateCounts: [], worldTicks: 0, varpReads: [], worlds: [] };
  class World {
    phaseValue = "sendHandshake";
    active = new Map();
    pending = new Map();
    serverAssignments = 0;
    published = 0;
    clears = 0;
    idleTicks = 0;
    pingQueued = false;
    flushPending = false;
    gameTicks = 0;
    flushed = 0;
    constructor(count) { this.count = count; state.worlds.push(this); }
    enableInventory() {
      if (state.inventoryEnableError) throw new Error("FAKE_SECRET_TOKEN");
      assert.equal(this.phaseValue, "sendHandshake");
      this.inventoryEnabled = true;
    }
    container(id, negativeKey) {
      state.containerRead?.();
      return JSON.stringify(state.containerResult ?? { id, negativeKey, slots: 2 });
    }
    item() {
      state.itemRead?.();
      return JSON.stringify(state.itemResult ?? { itemId: 42, amount: -2147483648, tooltip: "Item €" });
    }
    outgoing() {
      let bytes = new Uint8Array();
      if (this.phaseValue === "sendHandshake") { this.phaseValue = "awaitHandshake"; bytes = new Uint8Array([14]); }
      else if (this.phaseValue === "sendRequest") { this.phaseValue = "awaitResponse"; bytes = new Uint8Array([16, 0, 1, 7]); }
      else if (this.pingQueued) { this.pingQueued = false; this.flushPending = true; bytes = new Uint8Array([77]); }
      state.views.push(bytes);
      return bytes;
    }
    phase() { return this.phaseValue === "world" ? "worldReady" : this.phaseValue; }
    feed(bytes) {
      if (bytes[0] === 255) throw new Error("FAKE_SECRET_TOKEN");
      let event = null;
      if (this.phaseValue === "awaitHandshake") { assert.equal(bytes[0], 0); this.phaseValue = "sendRequest"; }
      else if (this.phaseValue === "awaitResponse" && state.terminalEvent) { this.phaseValue = "rejected"; event = state.terminalEvent; }
      else if (this.phaseValue === "awaitResponse") { assert.equal(bytes[0], 2); this.phaseValue = "awaitFirstPacket"; event = { kind: "loginAccepted" }; }
      else if (this.phaseValue === "awaitFirstPacket") {
        if (bytes[0] !== 85) throw new Error("FAKE_SECRET_TOKEN");
        this.phaseValue = "world";
        event = { kind: "worldReady" };
      } else if (this.count !== undefined && bytes[0] === 115) {
        assert.ok(bytes.length >= 3);
        this.pending.set(bytes[1], bytes[2]);
        this.serverAssignments++;
        return JSON.stringify({ consumed: 3, event: { kind: "variableReceived", id: bytes[1], value: bytes[2] } });
      } else if (this.count !== undefined && bytes[0] === 100) {
        this.active.clear();
        this.pending.clear();
        this.clears++;
        event = { kind: "variablesCleared" };
      } else event = { kind: "opaquePacket" };
      return JSON.stringify({ consumed: 1, event });
    }
    report() {
      return JSON.stringify({
        phase: this.phaseValue, accepted: ["awaitFirstPacket", "world"].includes(this.phaseValue), initial: this.phaseValue === "world" ? { ownTile: { plane: 0, x: 3200, y: 3200 } } : null,
        ...(this.inventoryEnabled ? { inventory: { containers: 1, slots: 2, tooltipBytes: 0, updates: 1, fullUpdates: 1, partialUpdates: 0, stops: 0 } } : {}),
        ...(this.count === undefined ? {} : { variables: { count: this.count, scheduled: this.pending.size, serverAssignments: this.serverAssignments, published: this.published, clears: this.clears, nonZero: [...this.active.values()].filter((value) => value !== 0).length } }),
      });
    }
    tick() {
      state.worldTicks++;
      if (state.tickError) throw new Error("FAKE_SECRET_TOKEN");
      const events = [...this.pending].map(([id, value]) => ({ kind: "variablePublished", id, previous: this.active.get(id) ?? 0, value }));
      for (const [id, value] of this.pending) this.active.set(id, value);
      this.pending.clear();
      this.published += events.length;
      return JSON.stringify({ events });
    }
    varp(id) {
      state.varpReads.push(id);
      if (state.varpError) throw new Error("FAKE_SECRET_TOKEN");
      return this.active.get(id) ?? 0;
    }
    gameTick() {
      assert.equal(this.phaseValue, "world");
      this.gameTicks++;
      if (state.gameTickError) throw new Error("FAKE_SECRET_TOKEN");
      this.idleTicks = Math.min(255, this.idleTicks + 1);
      if (this.idleTicks > 50 && !this.pingQueued && !this.flushPending) this.pingQueued = true;
    }
    markFlushed() {
      assert.equal(this.phaseValue, "world");
      assert.equal(this.flushPending, true);
      assert.equal(this.pingQueued, false);
      if (state.flushError) throw new Error("FAKE_SECRET_TOKEN");
      this.flushPending = false;
      this.idleTicks = 0;
      this.flushed++;
    }
    confirm() {}
    finish() { if (this.phaseValue !== "world" || this.pingQueued || this.flushPending) throw new Error("FAKE_SECRET_TOKEN"); }
    close() { state.worldClosed++; }
    free() { state.worldFreed++; }
  }
  class Lobby {
    phaseValue = "sendHandshake";
    outgoing() {
      if (this.phaseValue === "sendHandshake") { this.phaseValue = "awaitHandshake"; return new Uint8Array([14]); }
      if (this.phaseValue === "sendRequest") { this.phaseValue = "awaitResponse"; return new Uint8Array([19]); }
      return new Uint8Array();
    }
    feed(bytes) {
      if (bytes[0] === 255) throw new Error("FAKE_SECRET_TOKEN");
      let event = null;
      if (this.phaseValue === "awaitHandshake") { assert.equal(bytes[0], 0); this.phaseValue = "sendRequest"; }
      else { assert.equal(bytes[0], 2); this.phaseValue = "lobby"; event = { kind: "loginAccepted" }; }
      return JSON.stringify({ consumed: 1, event });
    }
    phase() { return this.phaseValue; }
    report() { return JSON.stringify({ phase: this.phaseValue, accepted: this.phaseValue === "lobby", worldList: null }); }
    startWorld() { assert.equal(this.phaseValue, "lobby"); state.worldCreated++; state.worldLegacyStarts++; return new World(); }
    startWorldState(config, count) { assert.equal(config, "{}"); assert.equal(this.phaseValue, "lobby"); state.worldCreated++; state.worldStateCounts.push(count); return new World(count); }
    markFlushed() {}
    close() { state.lobbyClosed++; }
    free() { state.lobbyFreed++; }
    finish() {}
  }
  class Socket {
    constructor(url) { this.url = url; this.readyState = 0; this.bufferedAmount = 0; this.closed = 0; state.sockets.push(this); state.onSocketConstruct?.(this); }
    send(bytes) { state.sent.push({ url: this.url, bytes: [...bytes] }); }
    close() { this.readyState = 3; this.closed++; }
  }
  return { state, options: { load: async () => Lobby, WebSocketClass: Socket, timeoutMs: 1000, lifetimeMs: 2000 } };
}

function fakeTimers(t) {
  const timers = [];
  t.mock.method(globalThis, "setTimeout", (callback, delay) => {
    const timer = { callback, delay, active: true };
    timers.push(timer);
    return timer;
  });
  t.mock.method(globalThis, "clearTimeout", (timer) => { if (timer) timer.active = false; });
  return {
    pending: (delay) => timers.filter((timer) => timer.active && (delay === undefined || timer.delay === delay)),
    fire(timer) { assert.ok(timer?.active); timer.active = false; timer.callback(); },
  };
}

function open(socket) { socket.readyState = 1; socket.onopen(); }
function send(socket, bytes) { socket.onmessage({ data: new Uint8Array(bytes).buffer }); }

async function accepted(f) {
  const lobby = await openLobbySession("ws://127.0.0.1:8110/lobby", "{}", f.options);
  open(f.state.sockets[0]);
  send(f.state.sockets[0], [0, 2]);
  await lobby.ready;
  return lobby;
}

async function readyState(f, varpCount = 2) {
  const lobby = await accepted(f);
  const world = lobby.startWorld("ws://localhost/world", "{}", { varpCount });
  const socket = f.state.sockets[1];
  open(socket);
  send(socket, [0, 2, 85]);
  await world.ready;
  return { world, socket, native: f.state.worlds[0] };
}

function serviceTicks(timers, count) {
  for (let tick = 0; tick < count; tick++) {
    assert.equal(timers.pending(20).length, 1);
    timers.fire(timers.pending(20)[0]);
  }
}

function worldSends(f) {
  return f.state.sent.filter((entry) => entry.url.endsWith("/world")).map((entry) => entry.bytes);
}

function assertReleased(f, world, timers) {
  assert.equal(f.state.worldClosed, 1);
  assert.equal(f.state.worldFreed, 1);
  assert.equal(world.state().connected, false);
  assert.equal(world.state().accepted, false);
  assert.equal("variables" in world.state(), false);
  assert.equal(JSON.stringify(world.state()).includes("FAKE_SECRET"), false);
  assert.equal(timers.pending().length, 0);
  assert.ok(f.state.views.every((bytes) => bytes.every((byte) => byte === 0)));
}

test("inventory world option is strict, early and cleans failed activation", async () => {
  const f = fixture();
  const lobby = await accepted(f);
  try {
    for (const inventory of [1, 0, "true", null, {}, [], new Boolean(true)]) {
      assert.throws(() => lobby.startWorld("ws://localhost/world", "{}", { inventory }), /must be boolean/);
    }
    assert.equal(f.state.worldCreated, 0);
    f.state.inventoryEnableError = true;
    assert.throws(() => lobby.startWorld("ws://localhost/world", "{}", { inventory: true }), /invalid original world handoff configuration/);
    assert.equal(f.state.worldClosed, 1);
    assert.equal(f.state.worldFreed, 1);
    assert.equal(f.state.sockets.length, 1);
  } finally { lobby.close(); }
});

test("inventory accessors return independent checked snapshots and clear on close", async () => {
  const f = fixture();
  const lobby = await accepted(f);
  const world = lobby.startWorld("ws://localhost/world", "{}", { inventory: true });
  try {
    assert.equal(f.state.worlds[0].inventoryEnabled, true);
    assert.throws(() => world.container(93, false), /state unavailable/);
    open(f.state.sockets[1]);
    send(f.state.sockets[1], [0, 2, 85]);
    await world.ready;
    const first = world.container(93, false);
    assert.deepEqual(first, { id: 93, negativeKey: false, slots: 2 });
    assert.ok(Object.isFrozen(first));
    assert.notEqual(first, world.container(93, false));
    const item = world.item(93, false, 0);
    assert.deepEqual(item, { itemId: 42, amount: -2147483648, tooltip: "Item €" });
    assert.ok(Object.isFrozen(item));
    for (const id of [-1, 65536, 1.5, NaN, Infinity, "93", null, true, {}, [], 2n]) {
      assert.throws(() => world.container(id, false), /bounded/);
      assert.throws(() => world.item(id, false, 0), /bounded/);
    }
    for (const namespace of [0, 1, "false", null, {}, []]) assert.throws(() => world.container(93, namespace), /bounded/);
    for (const slot of [-1, 65535, 0.5, NaN, "0", null, true]) assert.throws(() => world.item(93, false, slot), /bounded/);
    for (const result of [{ id: 92, negativeKey: false, slots: 2 }, { id: 93, negativeKey: false, slots: 0 }, { id: 93, negativeKey: 0, slots: 2 }, { id: 93, negativeKey: false, slots: 2, extra: 1 }, []]) {
      f.state.containerResult = result;
      assert.throws(() => world.container(93, false), /^Error: original container unavailable$/);
    }
    for (const result of [{ itemId: -2, amount: 0, tooltip: null }, { itemId: 0, amount: 2147483648, tooltip: null }, { itemId: 0, amount: 0, tooltip: {} }, { itemId: 0, amount: 0, tooltip: null, extra: 1 }]) {
      f.state.itemResult = result;
      assert.throws(() => world.item(93, false, 0), /^Error: original inventory item unavailable$/);
    }
  } finally { world.close(); lobby.close(); }
  assert.equal("inventory" in world.state(), false);
  assert.throws(() => world.item(93, false, 0), /state unavailable/);
});

test("inventory accessors cannot publish after reentrant closure", async () => {
  for (const method of ["container", "item"]) {
    const f = fixture();
    const lobby = await accepted(f);
    const world = lobby.startWorld("ws://localhost/world", "{}", { inventory: true });
    open(f.state.sockets[1]);
    send(f.state.sockets[1], [0, 2, 85]);
    await world.ready;
    f.state[`${method}Read`] = () => world.close();
    assert.throws(() => world[method](93, false, 0), /unavailable/);
    assert.equal(f.state.worldFreed, 1);
    lobby.close();
  }
});

test("world handoff uses a separate original socket and closes lobby only after parsed world readiness", async () => {
  const f = fixture();
  const lobby = await accepted(f);
  const world = lobby.startWorld("ws://127.0.0.1:8110/world", "{}");
  assert.equal(f.state.worldCreated, 1);
  assert.equal(f.state.lobbyClosed, 0);
  const socket = f.state.sockets[1];
  open(socket);
  assert.deepEqual(f.state.sent.at(-1).bytes, [14]);
  send(socket, [0, 2]);
  let ready = false;
  world.ready.then(() => { ready = true; });
  await Promise.resolve();
  assert.equal(ready, false);
  assert.equal(world.state().initial, null);
  assert.equal(f.state.lobbyClosed, 0);
  assert.deepEqual(f.state.sent.at(-1).bytes, [16, 0, 1, 7]);
  send(socket, [85, 29]);
  assert.deepEqual((await world.ready).initial, { ownTile: { plane: 0, x: 3200, y: 3200 } });
  assert.equal(world.state().opaquePackets, 1);
  assert.equal(f.state.lobbyClosed, 1);
  assert.equal(f.state.lobbyFreed, 1);
  assert.equal(f.state.sockets[0].closed, 1);
  assert.ok(f.state.views.every((bytes) => bytes.every((byte) => byte === 0)));
  assert.equal(lobby.state().connected, false);
  world.close();
  world.close();
  assert.equal(f.state.worldClosed, 1);
  assert.equal(f.state.worldFreed, 1);
});

test("state handoff validates explicit counts without coercion and legacy has no world tick or accessor", async (t) => {
  const timers = fakeTimers(t);
  const f = fixture();
  const lobby = await accepted(f);
  let coerced = false;
  const coercible = { valueOf() { coerced = true; return 2; }, toString() { coerced = true; return "2"; } };
  for (const varpCount of [-1, 65537, 4294967296, 0.5, NaN, Infinity, -Infinity, "2", true, null, 2n, {}, [], new Number(2), coercible]) {
    assert.throws(() => lobby.startWorld("ws://localhost/world", "{}", { varpCount }), /bounded original variable count/);
  }
  assert.equal(coerced, false);
  assert.equal(f.state.worldCreated, 0);
  assert.equal(f.state.lobbyClosed, 0);
  const legacy = lobby.startWorld("ws://localhost/world", "{}", { varpCount: undefined });
  assert.equal(f.state.worldLegacyStarts, 1);
  assert.deepEqual(f.state.worldStateCounts, []);
  assert.equal("tick" in legacy, false);
  assert.equal("varp" in legacy, false);
  open(f.state.sockets[1]);
  send(f.state.sockets[1], [0, 2, 85]);
  await legacy.ready;
  assert.equal(timers.pending(20).length, 0);
  assert.equal(f.state.worldTicks, 0);
  assert.equal("variables" in legacy.state(), false);
  legacy.close();
  assert.equal(timers.pending().length, 0);
  for (const varpCount of [0, 1, 65536]) {
    const g = fixture();
    const acceptedLobby = await accepted(g);
    const world = acceptedLobby.startWorld("ws://localhost/world", "{}", { varpCount });
    assert.deepEqual(g.state.worldStateCounts, [varpCount]);
    assert.equal(g.state.worldLegacyStarts, 0);
    assert.equal(typeof world.tick, "function");
    assert.equal(typeof world.varp, "function");
    assert.equal(world.state().accepted, false);
    assert.throws(() => world.varp(0), /state unavailable/);
    assert.throws(() => world.tick(), /not ready/);
    assert.equal(timers.pending(20).length, 0);
    open(g.state.sockets[1]);
    send(g.state.sockets[1], [0, 2, 85]);
    await world.ready;
    assert.equal(world.state().variables.count, varpCount);
    if (varpCount) assert.equal(world.varp(varpCount - 1), 0);
    assert.throws(() => world.varp(varpCount), /verified span/);
    world.close();
    assert.equal("variables" in world.state(), false);
    assert.equal(timers.pending().length, 0);
  }
});

test("state readiness schedules one idle service after the entire message and refreshes summary counters", async (t) => {
  const timers = fakeTimers(t);
  const f = fixture();
  const lobby = await accepted(f);
  const world = lobby.startWorld("ws://localhost/world", "{}", { varpCount: 3 });
  const socket = f.state.sockets[1];
  assert.throws(() => world.varp(1), /state unavailable/);
  assert.equal(timers.pending(20).length, 0);
  open(socket);
  send(socket, [0, 2]);
  assert.equal(timers.pending(20).length, 0);
  assert.equal(f.state.lobbyClosed, 0);
  assert.throws(() => world.tick(), /not ready/);
  send(socket, [85, 115, 1, 8, 115, 1, 9, 29]);
  const ready = await world.ready;
  assert.equal(ready.opaquePackets, 1);
  assert.deepEqual(ready.variables, { count: 3, scheduled: 1, serverAssignments: 2, published: 0, clears: 0, nonZero: 0 });
  assert.equal(f.state.worldTicks, 0);
  assert.equal(f.state.lobbyClosed, 1);
  assert.equal(timers.pending(20).length, 1);
  assert.equal(world.varp(1), 0);
  const service = timers.pending(20)[0];
  timers.fire(service);
  assert.equal(f.state.worldTicks, 1);
  assert.equal(world.varp(1), 9);
  assert.deepEqual(world.state().variables, { count: 3, scheduled: 0, serverAssignments: 2, published: 1, clears: 0, nonZero: 1 });
  assert.equal(timers.pending(20).length, 1);
  assert.deepEqual(f.state.sent.filter((entry) => entry.url.endsWith("/world")).map((entry) => entry.bytes), [[14], [16, 0, 1, 7]]);
  const snapshot = world.state();
  snapshot.variables.count = 999;
  assert.equal(world.state().variables.count, 3);
  send(socket, [115, 2, 7, 100]);
  assert.deepEqual(world.state().variables, { count: 3, scheduled: 0, serverAssignments: 3, published: 1, clears: 1, nonZero: 0 });
  assert.equal(world.varp(1), 0);
  const ticked = world.tick();
  assert.equal(f.state.worldTicks, 2);
  assert.equal(ticked.variables.published, 1);
  world.close();
  assert.equal(timers.pending().length, 0);
  const before = f.state.worldTicks;
  service.callback();
  assert.equal(f.state.worldTicks, before);
});

test("world keepalive counts only explicit service ticks and publishes variables before the fifty-first game tick", async (t) => {
  const timers = fakeTimers(t);
  const f = fixture();
  const { world, socket, native } = await readyState(f);
  const order = [];
  const tick = native.tick.bind(native);
  native.tick = () => { order.push("tick"); return tick(); };
  const gameTick = native.gameTick.bind(native);
  native.gameTick = () => { order.push("gameTick"); return gameTick(); };
  const outgoing = native.outgoing.bind(native);
  native.outgoing = () => { order.push("outgoing"); return outgoing(); };
  const transportSend = socket.send.bind(socket);
  socket.send = (bytes) => { order.push("send"); transportSend(bytes); };
  const flush = native.markFlushed.bind(native);
  native.markFlushed = () => { order.push("markFlushed"); return flush(); };
  timers.fire(timers.pending(0)[0]);
  assert.equal(native.flushed, 0);
  for (let index = 0; index < 100; index++) {
    send(socket, [29]);
    world.tick();
  }
  assert.equal(native.gameTicks, 0);
  assert.equal(native.idleTicks, 0);
  assert.equal(native.flushed, 0);
  assert.equal(timers.pending(0).length, 0);
  assert.deepEqual(worldSends(f), [[14], [16, 0, 1, 7]]);
  assert.equal(timers.pending(20).length, 1);
  serviceTicks(timers, 50);
  assert.equal(native.gameTicks, 50);
  assert.equal(native.idleTicks, 50);
  assert.equal(native.flushed, 0);
  assert.equal(native.flushPending, false);
  assert.equal(timers.pending(0).length, 0);
  assert.deepEqual(worldSends(f), [[14], [16, 0, 1, 7]]);
  send(socket, [115, 1, 7]);
  assert.equal(world.varp(1), 0);
  order.length = 0;
  serviceTicks(timers, 1);
  assert.deepEqual(order, ["tick", "gameTick", "outgoing", "send"]);
  assert.equal(world.varp(1), 7);
  assert.equal(native.gameTicks, 51);
  assert.equal(native.idleTicks, 51);
  assert.equal(native.flushPending, true);
  assert.equal(native.flushed, 0);
  assert.deepEqual(worldSends(f), [[14], [16, 0, 1, 7], [77]]);
  timers.fire(timers.pending(0)[0]);
  assert.equal(order.at(-1), "markFlushed");
  assert.equal(native.flushed, 1);
  assert.equal(native.idleTicks, 0);
  assert.equal(native.flushPending, false);
  serviceTicks(timers, 50);
  assert.equal(native.gameTicks, 101);
  assert.equal(native.flushed, 1);
  assert.equal(native.idleTicks, 50);
  assert.equal(worldSends(f).length, 3);
  assert.equal(timers.pending(0).length, 0);
  serviceTicks(timers, 1);
  assert.equal(worldSends(f).length, 4);
  assert.equal(native.flushed, 1);
  timers.fire(timers.pending(0)[0]);
  assert.equal(native.flushed, 2);
  assert.equal(native.idleTicks, 0);
  world.close();
  assertReleased(f, world, timers);
});

test("delayed login flush after world readiness never acknowledges an empty world send or resets idle", async (t) => {
  const timers = fakeTimers(t);
  const f = fixture();
  const lobby = await accepted(f);
  const world = lobby.startWorld("ws://localhost/world", "{}", { varpCount: 2 });
  const socket = f.state.sockets[1];
  const native = f.state.worlds[0];
  socket.bufferedAmount = 99;
  open(socket);
  send(socket, [0, 2, 85]);
  await world.ready;
  assert.equal(native.flushed, 0);
  timers.fire(timers.pending(0)[0]);
  assert.equal(timers.pending(5).length, 1);
  serviceTicks(timers, 10);
  assert.equal(native.idleTicks, 10);
  socket.bufferedAmount = 0;
  timers.fire(timers.pending(5)[0]);
  assert.equal(native.flushed, 0);
  assert.equal(native.idleTicks, 10);
  assert.equal(world.state().error, null);
  assert.equal(timers.pending(5).length, 0);
  assert.equal(timers.pending(0).length, 0);
  for (let index = 0; index < 100; index++) world.tick();
  assert.equal(native.gameTicks, 10);
  assert.equal(native.idleTicks, 10);
  assert.equal(native.flushed, 0);
  assert.deepEqual(worldSends(f), [[14], [16, 0, 1, 7]]);
  world.close();
  assertReleased(f, world, timers);
});

test("buffered world sends coalesce across service and manual ticks until buffer-zero acknowledgement", async (t) => {
  const timers = fakeTimers(t);
  const f = fixture();
  const { world, socket, native } = await readyState(f);
  timers.fire(timers.pending(0)[0]);
  socket.bufferedAmount = 99;
  serviceTicks(timers, 51);
  assert.equal(native.flushPending, true);
  assert.equal(native.flushed, 0);
  assert.deepEqual(worldSends(f), [[14], [16, 0, 1, 7], [77]]);
  timers.fire(timers.pending(0)[0]);
  const poll = timers.pending(5)[0];
  assert.ok(poll);
  serviceTicks(timers, 300);
  for (let index = 0; index < 100; index++) {
    world.tick();
    send(socket, [29]);
  }
  assert.equal(native.gameTicks, 351);
  assert.equal(native.idleTicks, 255);
  assert.equal(native.flushed, 0);
  assert.equal(native.pingQueued, false);
  assert.equal(native.flushPending, true);
  assert.equal(worldSends(f).length, 3);
  assert.deepEqual(timers.pending(5), [poll]);
  assert.equal(timers.pending(0).length, 0);
  timers.fire(poll);
  assert.equal(native.flushed, 0);
  assert.equal(timers.pending(5).length, 1);
  socket.bufferedAmount = 0;
  timers.fire(timers.pending(5)[0]);
  assert.equal(native.flushed, 1);
  assert.equal(native.flushPending, false);
  assert.equal(native.idleTicks, 0);
  assert.equal(timers.pending(5).length, 0);
  serviceTicks(timers, 50);
  assert.equal(worldSends(f).length, 3);
  assert.equal(native.idleTicks, 50);
  assert.equal(native.flushed, 1);
  serviceTicks(timers, 1);
  assert.equal(worldSends(f).length, 4);
  assert.equal(native.flushed, 1);
  timers.fire(timers.pending(0)[0]);
  assert.equal(native.flushed, 2);
  world.close();
  assertReleased(f, world, timers);
});

test("pending keepalive errors close and abort cancel service and flush callbacks with static errors", async (t) => {
  const timers = fakeTimers(t);
  for (const [reason, expected] of [
    ["close", null],
    ["abort", "original world cancelled"],
    ["error", "original world relay connection failed"],
    ["unclean", "original world connection ended unexpectedly"],
    ["unflushedEof", "original world connection ended unexpectedly"],
    ["tick", "original world operation failed"],
    ["gameTick", "original world operation failed"],
    ["flush", "original world flush failed"],
    ["send", "original world operation failed"],
    ["lifetime", "original world diagnostic lifetime reached"],
  ]) {
    const f = fixture();
    const controller = new AbortController();
    f.options.signal = controller.signal;
    const { world, socket, native } = await readyState(f);
    timers.fire(timers.pending(0)[0]);
    socket.bufferedAmount = 99;
    serviceTicks(timers, reason === "send" ? 50 : 51);
    const service = timers.pending(20)[0];
    const flush = timers.pending(0)[0];
    const callbacks = { open: socket.onopen, message: socket.onmessage, error: socket.onerror, close: socket.onclose };
    if (reason === "close") world.close();
    else if (reason === "abort") controller.abort("FAKE_SECRET_TOKEN");
    else if (reason === "error") socket.onerror(new Error("FAKE_SECRET_TOKEN"));
    else if (reason === "unclean") socket.onclose({ code: 1011, wasClean: false, reason: "FAKE_SECRET_TOKEN" });
    else if (reason === "unflushedEof") socket.onclose({ code: 1000, wasClean: true, reason: "FAKE_SECRET_TOKEN" });
    else if (reason === "tick") { f.state.tickError = true; timers.fire(service); }
    else if (reason === "gameTick") { f.state.gameTickError = true; timers.fire(service); }
    else if (reason === "flush") { f.state.flushError = true; socket.bufferedAmount = 0; timers.fire(flush); }
    else if (reason === "send") { socket.send = () => { throw new Error("FAKE_SECRET_TOKEN"); }; timers.fire(service); }
    else timers.fire(timers.pending(2000)[0]);
    assert.equal(world.state().error, expected, reason);
    assert.equal(world.state().phase, expected ? "failed" : "closed", reason);
    assertReleased(f, world, timers);
    const ticks = native.gameTicks;
    const variables = f.state.worldTicks;
    const acknowledgements = native.flushed;
    const sent = worldSends(f).length;
    service.callback();
    flush?.callback();
    callbacks.open();
    callbacks.message({ data: new Uint8Array([85, 115, 1, 9]).buffer });
    callbacks.error(new Error("FAKE_SECRET_TOKEN"));
    callbacks.close({ code: 1000, wasClean: true });
    controller.abort();
    world.close();
    assert.equal(native.gameTicks, ticks, reason);
    assert.equal(f.state.worldTicks, variables, reason);
    assert.equal(native.flushed, acknowledgements, reason);
    assert.equal(worldSends(f).length, sent, reason);
    assertReleased(f, world, timers);
  }
});

test("state accessor checks direct numeric bounds before touching wasm and sanitizes host errors", async (t) => {
  const timers = fakeTimers(t);
  const f = fixture();
  const lobby = await accepted(f);
  const world = lobby.startWorld("ws://localhost/world", "{}", { varpCount: 3 });
  open(f.state.sockets[1]);
  send(f.state.sockets[1], [0, 2, 85]);
  await world.ready;
  let coerced = false;
  const coercible = { valueOf() { coerced = true; return 1; } };
  for (const id of [-1, 3, 65536, 4294967296, 0.5, NaN, Infinity, "1", true, null, undefined, 1n, {}, [], new Number(1), coercible]) {
    assert.throws(() => world.varp(id), /verified span/);
  }
  assert.equal(coerced, false);
  assert.deepEqual(f.state.varpReads, []);
  assert.equal(world.varp(2), 0);
  assert.deepEqual(f.state.varpReads, [2]);
  f.state.varpError = true;
  assert.throws(() => world.varp(1), { message: "original variable unavailable" });
  assert.equal(world.state().error, null);
  world.close();
  assert.throws(() => world.varp(0), /state unavailable/);
  assert.equal("variables" in world.state(), false);
  assert.equal(timers.pending().length, 0);
});

test("state corrupt readiness suffix cannot start service or close the accepted lobby", async (t) => {
  const timers = fakeTimers(t);
  const f = fixture();
  const lobby = await accepted(f);
  const world = lobby.startWorld("ws://localhost/world", "{}", { varpCount: 2 });
  open(f.state.sockets[1]);
  const lateMessage = f.state.sockets[1].onmessage;
  send(f.state.sockets[1], [0, 2, 85, 115, 1, 9, 255]);
  await assert.rejects(world.ready, { message: "invalid original world response" });
  assert.equal(f.state.lobbyClosed, 0);
  assert.equal(f.state.worldTicks, 0);
  assert.equal(timers.pending(20).length, 0);
  assert.equal("variables" in world.state(), false);
  assert.equal(world.state().accepted, false);
  assert.throws(() => world.varp(1), /state unavailable/);
  lateMessage({ data: new Uint8Array([85]).buffer });
  assert.equal(f.state.worldFreed, 1);
  assert.equal(f.state.lobbyClosed, 0);
  lobby.close();
  assert.equal(timers.pending().length, 0);
});

test("state timers and late callbacks cannot revive a closed aborted failed or rejected world", async (t) => {
  const timers = fakeTimers(t);
  for (const reason of ["close", "abort", "error", "invalid", "tick"]) {
    const f = fixture();
    const controller = new AbortController();
    f.options.signal = controller.signal;
    const lobby = await accepted(f);
    const world = lobby.startWorld("ws://localhost/world", "{}", { varpCount: 2 });
    const socket = f.state.sockets[1];
    open(socket);
    send(socket, [0, 2, 85, 115, 1, 9]);
    await world.ready;
    const callbacks = { open: socket.onopen, message: socket.onmessage, error: socket.onerror, close: socket.onclose };
    const timer = timers.pending(20)[0];
    if (reason === "close") world.close();
    else if (reason === "abort") controller.abort();
    else if (reason === "error") socket.onerror(new Error("FAKE_SECRET_TOKEN"));
    else if (reason === "invalid") send(socket, [255]);
    else { f.state.tickError = true; timers.fire(timer); }
    const ticks = f.state.worldTicks;
    assert.equal(timers.pending().length, 0, reason);
    assert.equal("variables" in world.state(), false, reason);
    assert.equal(world.state().accepted, false, reason);
    assert.equal(JSON.stringify(world.state()).includes("FAKE_SECRET"), false);
    assert.throws(() => world.varp(1), /state unavailable/);
    timer.callback();
    callbacks.open();
    callbacks.message({ data: new Uint8Array([85, 115, 1, 7]).buffer });
    callbacks.error();
    callbacks.close({ code: 1000, wasClean: true });
    world.close();
    assert.equal(f.state.worldTicks, ticks);
    assert.equal(f.state.worldClosed, 1);
    assert.equal(f.state.worldFreed, 1);
    assert.equal(timers.pending().length, 0);
  }
  const f = fixture();
  const lobby = await accepted(f);
  const world = lobby.startWorld("ws://localhost/world", "{}", { varpCount: 2 });
  open(f.state.sockets[1]);
  f.state.terminalEvent = { kind: "rejected", responseCode: 3 };
  send(f.state.sockets[1], [0, 3]);
  await assert.rejects(world.ready, /rejected \(code 3\)/);
  assert.equal(timers.pending(20).length, 0);
  assert.equal("variables" in world.state(), false);
  assert.equal(f.state.worldTicks, 0);
  lobby.close();
  assert.equal(timers.pending().length, 0);
});

test("state abort during socket construction releases the bank and leaves no service callback", async (t) => {
  const timers = fakeTimers(t);
  const f = fixture();
  const controller = new AbortController();
  f.options.signal = controller.signal;
  const lobby = await accepted(f);
  f.state.onSocketConstruct = (socket) => { if (socket.url.endsWith("/world")) controller.abort(); };
  assert.throws(() => lobby.startWorld("ws://localhost/world", "{}", { varpCount: 2 }), { message: "original world socket construction failed" });
  assert.equal(f.state.worldCreated, 1);
  assert.equal(f.state.worldClosed, 1);
  assert.equal(f.state.worldFreed, 1);
  assert.equal(f.state.worldTicks, 0);
  assert.equal(f.state.sockets[1].closed, 1);
  assert.equal(f.state.lobbyClosed, 1);
  assert.equal(timers.pending().length, 0);
});

test("world rejection preserves only bounded typed response codes and keeps the lobby open", async () => {
  for (const responseCode of [0, 3, 29, 45, 255, undefined, -1, 256, 1.5, "3", { token: "FAKE_SECRET_TOKEN" }]) {
    const f = fixture();
    const lobby = await accepted(f);
    const world = lobby.startWorld("ws://localhost/world", "{}");
    const socket = f.state.sockets[1];
    open(socket);
    f.state.terminalEvent = { kind: "rejected", responseCode, detail: "FAKE_SECRET_TOKEN", extraInfo: 123456 };
    const valid = Number.isSafeInteger(responseCode) && responseCode >= 0 && responseCode <= 255;
    const reason = `original world rejected${valid ? ` (code ${responseCode})` : ""}`;
    const rejected = assert.rejects(world.ready, { message: reason });
    send(socket, [0, 3]);
    await rejected;
    assert.equal(world.state().error, reason);
    assert.equal(world.state().outcome, "rejected");
    assert.equal(Object.hasOwn(world.state(), "responseCode"), valid);
    if (valid) assert.equal(world.state().responseCode, responseCode);
    assert.equal(JSON.stringify(world.state()).includes("FAKE_SECRET"), false);
    for (const key of ["detail", "extraInfo", "token"]) assert.equal(Object.hasOwn(world.state(), key), false);
    assert.equal(f.state.worldClosed, 1);
    assert.equal(f.state.worldFreed, 1);
    assert.equal(f.state.lobbyClosed, 0);
    lobby.close();
  }
});

test("invalid world route or configuration cannot select arbitrary upstreams or consume a handoff", async () => {
  const f = fixture();
  const lobby = await accepted(f);
  for (const url of ["ws://example.com/world", "ws://localhost/lobby", "ws://localhost/world?host=x", "ws://localhost/world#x", "ws://user@localhost/world"]) {
    assert.throws(() => lobby.startWorld(url, "{}"), /relay URL/);
  }
  assert.throws(() => lobby.startWorld("ws://localhost/world", "x".repeat(65537)), /configuration/);
  assert.equal(f.state.worldCreated, 0);
  const world = lobby.startWorld("ws://localhost/world", "{}");
  assert.throws(() => lobby.startWorld("ws://localhost/world", "{}"), /unavailable/);
  world.close();
  await assert.rejects(world.ready, /closed before acceptance/);
  lobby.close();
});

test("error websocket close codes fail with a static reason", async () => {
  const f = fixture();
  const lobby = await accepted(f);
  f.state.sockets[0].onclose({ code: 1011, wasClean: true, reason: "FAKE_SECRET_REASON" });
  assert.equal(lobby.state().phase, "failed");
  assert.equal(lobby.state().error, "original lobby connection ended unexpectedly");
  assert.equal(JSON.stringify(lobby.state()).includes("FAKE_SECRET_REASON"), false);
  assert.equal(f.state.lobbyFreed, 1);
});

test("coalesced corrupt suffix cannot announce readiness or close the parent lobby", async () => {
  const f = fixture();
  const lobby = await accepted(f);
  const world = lobby.startWorld("ws://localhost/world", "{}");
  open(f.state.sockets[1]);
  send(f.state.sockets[1], [0, 2, 85, 255]);
  await assert.rejects(world.ready, /^Error: invalid original world response$/);
  assert.equal(world.state().accepted, false);
  assert.equal(f.state.lobbyClosed, 0);
  assert.equal(f.state.worldFreed, 1);
  lobby.close();

  const g = fixture();
  const other = await openLobbySession("ws://localhost/lobby", "{}", g.options);
  open(g.state.sockets[0]);
  send(g.state.sockets[0], [0, 2, 255]);
  await assert.rejects(other.ready, /^Error: invalid original lobby response$/);
  assert.equal(other.state().accepted, false);
  assert.equal(g.state.lobbyFreed, 1);
});

test("malformed first world packet cannot become ready and does not expose a token in errors", async () => {
  const f = fixture();
  const lobby = await accepted(f);
  const world = lobby.startWorld("ws://localhost/world", "{}");
  open(f.state.sockets[1]);
  send(f.state.sockets[1], [0, 2, 51]);
  await assert.rejects(world.ready, /^Error: invalid original world response$/);
  assert.equal(world.state().accepted, false);
  assert.equal(world.state().initial, null);
  assert.equal(f.state.worldFreed, 1);
  assert.equal(f.state.lobbyClosed, 0);
  lobby.close();
});
