import test from "node:test";
import assert from "node:assert/strict";
import { setTimeout as delay } from "node:timers/promises";
import { openLobbySession } from "../static/lobby-session.js";

function validStats(overrides = {}) {
  return { skillCount: 25, updates: 6, statUpdates: 3, runEnergyUpdates: 2, playerWeightUpdates: 1, recentSkillUpdates: Array.from({ length: 32 }, (_, at) => at % 25), skillUpdateCycle: 3, runEnergy: 100, playerWeight: -5, ...overrides };
}

function validSkill(overrides = {}) {
  return { experience: 1000, visibleLevel: 10, baseLevel: 10, ...overrides };
}

function fixture() {
  const state = { sockets: [], calls: [], sent: [], outgoing: [], lobbyClosed: 0, lobbyFreed: 0, worldClosed: 0, worldFreed: 0, worldStarts: 0, ticks: 0, gameTicks: 0, marked: 0 };
  class World {
    constructor() { this.stage = "sendHandshake"; }
    enablePlayerStats() {
      state.calls.push("enablePlayerStats");
      assert.equal(this.stage, "sendHandshake");
      assert.equal(state.sockets.length, 1);
      if (state.enableFailure) throw new Error("FAKE_PRIVATE_RUNTIME_VALUE");
      state.statsEnabled = true;
    }
    enableInventory() { state.calls.push("enableInventory"); }
    phase() { return this.stage; }
    outgoing() {
      let bytes = new Uint8Array();
      if (this.stage === "sendHandshake") { this.stage = "awaitHandshake"; bytes = new Uint8Array([14]); }
      else if (this.stage === "sendRequest") { this.stage = "awaitResponse"; bytes = new Uint8Array([16, 0, 1, 1]); }
      else if (state.pending) { bytes = state.pending; state.pending = null; }
      state.outgoing.push(bytes);
      return bytes;
    }
    feed(bytes) {
      if (this.stage === "awaitHandshake") {
        assert.equal(bytes[0], 0);
        this.stage = "sendRequest";
        return JSON.stringify({ consumed: 1, event: null });
      }
      if (this.stage === "awaitResponse") {
        assert.equal(bytes[0], 2);
        this.stage = "worldReady";
        return JSON.stringify({ consumed: 1, event: { kind: "worldReady" } });
      }
      return JSON.stringify({ consumed: bytes.length, event: { kind: "opaquePacket" } });
    }
    confirm() { state.calls.push("confirm"); }
    report() { return JSON.stringify({ phase: this.stage, accepted: this.stage === "worldReady", ...(state.statsEnabled ? { playerStats: "statsReport" in state ? state.statsReport : validStats() } : {}), ...(state.reportExtra ?? {}) }); }
    skill(id) { state.calls.push(["skill", id]); state.onQuery?.(); return JSON.stringify(state.skillValue ?? null); }
    playerStats() { state.calls.push("playerStats"); state.onQuery?.(); return JSON.stringify(state.statsValue ?? null); }
    tick() { state.ticks++; if (state.ticks === 1) state.pending = new Uint8Array([53]); return "{}"; }
    gameTick() { state.gameTicks++; }
    markFlushed() { state.marked++; }
    finish() {}
    close() { state.worldClosed++; }
    free() { state.worldFreed++; }
  }
  class Lobby {
    constructor() { this.stage = "sendHandshake"; }
    phase() { return this.stage; }
    outgoing() {
      if (this.stage === "sendHandshake") { this.stage = "awaitHandshake"; return new Uint8Array([14]); }
      if (this.stage === "sendRequest") { this.stage = "awaitResponse"; return new Uint8Array([19]); }
      return new Uint8Array();
    }
    feed(bytes) {
      if (this.stage === "awaitHandshake") { this.stage = "sendRequest"; return JSON.stringify({ consumed: 1, event: null }); }
      this.stage = "lobby";
      return JSON.stringify({ consumed: 1, event: { kind: "loginAccepted" } });
    }
    report() { return JSON.stringify({ phase: this.stage, accepted: this.stage === "lobby" }); }
    markFlushed() {}
    startWorld() { state.worldStarts++; state.calls.push("startWorld"); state.world = new World(); return state.world; }
    startWorldState(config, count) { state.varpCount = count; state.worldStarts++; state.calls.push("startWorldState"); state.world = new World(); return state.world; }
    close() { state.lobbyClosed++; }
    free() { state.lobbyFreed++; }
  }
  class Socket {
    constructor(url) {
      state.calls.push(["socket", url.endsWith("/world") ? "world" : "lobby"]);
      this.readyState = 0;
      this.bufferedAmount = 0;
      this.closed = 0;
      state.sockets.push(this);
    }
    send(bytes) { state.sent.push([...bytes]); }
    close() { this.closed++; this.readyState = 3; }
  }
  return { state, options: { load: async () => Lobby, WebSocketClass: Socket, timeoutMs: 1000, lifetimeMs: 2000 } };
}

function message(socket, values) {
  const data = Uint8Array.from(values).buffer;
  socket.onmessage({ data });
  assert.ok(new Uint8Array(data).every((byte) => byte === 0));
}

async function lobby(f) {
  const handle = await openLobbySession("ws://127.0.0.1:8110/lobby", "{}", f.options);
  f.state.sockets[0].readyState = 1;
  f.state.sockets[0].onopen();
  message(f.state.sockets[0], [0, 2]);
  await handle.ready;
  return handle;
}

function cleanup(lobbyHandle, worldHandle) {
  worldHandle?.close();
  lobbyHandle.close();
}

test("player stats opt-in is a strict boolean checked before native world handoff", async () => {
  const f = fixture();
  const handle = await lobby(f);
  try {
    for (const playerStats of [null, 0, 1, "true", "false", [], {}, new Boolean(true)]) {
      assert.throws(() => handle.startWorld("ws://localhost:8110/world", "{}", { playerStats }), /^Error: original player stats option must be boolean$/);
    }
    assert.equal(f.state.worldStarts, 0);
    assert.equal(f.state.sockets.length, 1);
  } finally { handle.close(); }
});

test("default and explicitly disabled player stats preserve the legacy world handle and do not enable native state", async () => {
  for (const options of [{}, { playerStats: false }]) {
    const f = fixture();
    const handle = await lobby(f);
    const world = handle.startWorld("ws://localhost:8110/world", "{}", options);
    try {
      assert.equal(f.state.calls.includes("enablePlayerStats"), false);
      assert.equal(Object.hasOwn(world, "skill"), false);
      assert.equal(Object.hasOwn(world, "playerStats"), false);
      assert.equal(Object.hasOwn(world.state(), "playerStats"), false);
      assert.deepEqual(Object.keys(world).sort(), ["close", "confirm", "ready", "state"]);
    } finally { cleanup(handle, world); }
    assert.equal(f.state.worldClosed, 1);
    assert.equal(f.state.worldFreed, 1);
  }
});

test("player stats activate before world socket construction and before the raw handshake", async () => {
  const f = fixture();
  const handle = await lobby(f);
  const world = handle.startWorld("ws://localhost:8110/world", "{}", { playerStats: true });
  try {
    assert.deepEqual(f.state.calls.slice(-3), ["startWorld", "enablePlayerStats", ["socket", "world"]]);
    assert.equal(f.state.statsEnabled, true);
    assert.equal(f.state.world.stage, "sendHandshake");
    assert.equal(Object.isFrozen(world), true);
    assert.equal(Object.hasOwn(world, "session"), false);
    assert.equal(Object.hasOwn(world, "runtime"), false);
    assert.equal(Object.hasOwn(world, "socket"), false);
    assert.equal(Object.hasOwn(world, "free"), false);
    f.state.sockets[1].readyState = 1;
    f.state.sockets[1].onopen();
    assert.equal(f.state.world.stage, "awaitHandshake");
    assert.deepEqual(f.state.sent.at(-1), [14]);
    assert.ok(f.state.outgoing.every((bytes) => bytes.every((byte) => byte === 0)));
  } finally { cleanup(handle, world); }
});

test("native player stats activation errors release the unexposed world exactly once and do not open its socket", async () => {
  const f = fixture();
  const handle = await lobby(f);
  f.state.enableFailure = true;
  try {
    assert.throws(() => handle.startWorld("ws://localhost:8110/world", "{}", { playerStats: true }), /^Error: invalid original world handoff configuration$/);
    assert.equal(f.state.sockets.length, 1);
    assert.equal(f.state.worldClosed, 1);
    assert.equal(f.state.worldFreed, 1);
    assert.equal(JSON.stringify(handle.state()).includes("FAKE_PRIVATE"), false);
  } finally { handle.close(); }
});

async function readyWorld(f, handle, options = {}) {
  const world = handle.startWorld("ws://localhost:8110/world", "{}", { playerStats: true, ...options });
  const socket = f.state.sockets[1];
  socket.readyState = 1;
  socket.onopen();
  message(socket, [0]);
  message(socket, [2]);
  await world.ready;
  return world;
}

const failure = (world) => world.state();

test("player stats alone keep the world serviced and a stats-only world opts into nothing else", async () => {
  const f = fixture();
  const handle = await lobby(f);
  const world = await readyWorld(f, handle);
  try {
    assert.equal(f.state.calls.includes("enableInventory"), false);
    assert.equal(f.state.varpCount, undefined);
    await delay(100);
    assert.ok(f.state.ticks >= 3);
    assert.equal(f.state.gameTicks, f.state.ticks);
    const seen = f.state.ticks;
    world.close();
    await delay(60);
    assert.equal(f.state.ticks, seen);
  } finally { cleanup(handle, world); }
  assert.equal(f.state.worldClosed, 1);
  assert.equal(f.state.worldFreed, 1);
});

test("inventory alone does not enable player stats or expose queries", async () => {
  const f = fixture();
  const handle = await lobby(f);
  const world = handle.startWorld("ws://localhost:8110/world", "{}", { inventory: true });
  try {
    assert.equal(f.state.calls.includes("enablePlayerStats"), false);
    assert.equal(Object.hasOwn(world, "skill"), false);
    assert.equal(Object.hasOwn(world, "playerStats"), false);
  } finally { cleanup(handle, world); }
});

test("ready report carries a frozen exact-key player stats summary including boundary values", async () => {
  const f = fixture();
  f.state.statsReport = validStats({ skillUpdateCycle: -2147483648, runEnergy: 255, playerWeight: -32768, recentSkillUpdates: Array.from({ length: 32 }, (_, at) => (at % 2 ? 24 : 0)) });
  const handle = await lobby(f);
  const world = await readyWorld(f, handle);
  try {
    const first = world.state();
    assert.ok(Object.isFrozen(first));
    assert.ok(Object.isFrozen(first.playerStats));
    assert.ok(Object.isFrozen(first.playerStats.recentSkillUpdates));
    assert.deepEqual(Object.keys(first.playerStats).sort(), ["playerWeight", "playerWeightUpdates", "recentSkillUpdates", "runEnergy", "runEnergyUpdates", "skillCount", "skillUpdateCycle", "statUpdates", "updates"]);
    assert.equal(first.playerStats.skillUpdateCycle, -2147483648);
    assert.notEqual(world.state(), first);
    f.state.statsReport = validStats({ skillUpdateCycle: 2147483647, runEnergy: 0, playerWeight: 32767 });
    world.confirm();
    assert.equal(world.state().playerStats.skillUpdateCycle, 2147483647);
    assert.equal(first.playerStats.skillUpdateCycle, -2147483648);
  } finally { cleanup(handle, world); }
});

test("malformed player stats reports fail the session without publishing them", async () => {
  const mutations = [
    { extra: 1 }, { skillCount: 24 }, { skillCount: 26 }, { skillCount: "25" },
    { recentSkillUpdates: Array(31).fill(0) }, { recentSkillUpdates: Array(33).fill(0) },
    { recentSkillUpdates: [...Array(31).fill(0), 25] }, { recentSkillUpdates: [...Array(31).fill(0), -1] },
    { recentSkillUpdates: [...Array(31).fill(0), 1.5] }, { recentSkillUpdates: [...Array(31).fill(0), "1"] }, { recentSkillUpdates: null },
    { skillUpdateCycle: 2147483648 }, { skillUpdateCycle: -2147483649 }, { skillUpdateCycle: 1.5 }, { skillUpdateCycle: "1" },
    { runEnergy: 256 }, { runEnergy: -1 }, { runEnergy: 1.5 }, { playerWeight: 32768 }, { playerWeight: -32769 },
    { updates: 7 }, { updates: -1 }, { statUpdates: -1, updates: 4 },
  ];
  for (const mutation of mutations) {
    const f = fixture();
    const handle = await lobby(f);
    const world = await readyWorld(f, handle);
    try {
      f.state.statsReport = validStats(mutation);
      assert.throws(() => world.confirm(), /^Error: original world operation failed$/);
      const after = failure(world);
      assert.equal(after.phase, "failed");
      assert.equal(after.error, "original world operation failed");
      assert.equal(Object.hasOwn(after, "playerStats"), false);
    } finally { cleanup(handle, world); }
    assert.equal(f.state.worldClosed, 1);
    assert.equal(f.state.worldFreed, 1);
  }
  for (const report of [null, [], "x", 1]) {
    const f = fixture();
    const handle = await lobby(f);
    const world = await readyWorld(f, handle);
    try {
      f.state.statsReport = report;
      assert.throws(() => world.confirm(), /^Error: original world operation failed$/);
      assert.equal(Object.hasOwn(world.state(), "playerStats"), false);
    } finally { cleanup(handle, world); }
  }
  for (const key of ["updates", "recentSkillUpdates", "skillCount", "runEnergy", "playerWeight", "skillUpdateCycle", "statUpdates", "runEnergyUpdates", "playerWeightUpdates"]) {
    const f = fixture();
    const handle = await lobby(f);
    const world = await readyWorld(f, handle);
    try {
      f.state.statsReport = validStats({ [key]: undefined });
      assert.throws(() => world.confirm(), /^Error: original world operation failed$/);
    } finally { cleanup(handle, world); }
  }
});

test("a world report with no player stats at all fails an opted-in session", async () => {
  const f = fixture();
  const handle = await lobby(f);
  const world = await readyWorld(f, handle);
  try {
    f.state.reportExtra = { playerStats: undefined };
    assert.throws(() => world.confirm(), /^Error: original world operation failed$/);
    assert.equal(world.state().phase, "failed");
  } finally { cleanup(handle, world); }
});

test("a malformed report before ready rejects readiness and never exposes it", async () => {
  const f = fixture();
  f.state.statsReport = validStats({ skillCount: 24 });
  const handle = await lobby(f);
  const world = handle.startWorld("ws://localhost:8110/world", "{}", { playerStats: true });
  const socket = f.state.sockets[1];
  socket.readyState = 1;
  socket.onopen();
  assert.equal(world.state().phase, "failed");
  assert.equal(Object.hasOwn(world.state(), "playerStats"), false);
  await assert.rejects(world.ready);
  cleanup(handle, world);
  assert.equal(f.state.worldClosed, 1);
  assert.equal(f.state.worldFreed, 1);
});

test("terminal report phases carry no player stats and a terminal report with stats fails", async () => {
  const f = fixture();
  const handle = await lobby(f);
  const world = await readyWorld(f, handle);
  try {
    f.state.reportExtra = { phase: "rejected", accepted: false, playerStats: undefined };
    world.confirm();
    assert.equal(world.state().phase, "rejected");
    assert.equal(Object.hasOwn(world.state(), "playerStats"), false);
    f.state.reportExtra = { phase: "queued", accepted: false, playerStats: validStats() };
    assert.throws(() => world.confirm(), /^Error: original world operation failed$/);
    assert.equal(world.state().phase, "failed");
  } finally { cleanup(handle, world); }
});

test("skill queries return frozen exact three-key snapshots with the level 120 exception for skill 24", async () => {
  const f = fixture();
  const handle = await lobby(f);
  const world = await readyWorld(f, handle);
  try {
    f.state.skillValue = validSkill({ experience: -2147483648, visibleLevel: 255, baseLevel: 99 });
    const value = world.skill(0);
    assert.deepEqual(value, { experience: -2147483648, visibleLevel: 255, baseLevel: 99 });
    assert.ok(Object.isFrozen(value));
    assert.deepEqual(Object.keys(value).sort(), ["baseLevel", "experience", "visibleLevel"]);
    f.state.skillValue = validSkill({ experience: 2147483647, visibleLevel: 0, baseLevel: 0 });
    assert.equal(world.skill(23).experience, 2147483647);
    f.state.skillValue = validSkill({ baseLevel: 120 });
    assert.equal(world.skill(24).baseLevel, 120);
    assert.deepEqual(f.state.calls.filter((call) => Array.isArray(call) && call[0] === "skill").map((call) => call[1]), [0, 23, 24]);
    f.state.statsValue = validStats();
    const stats = world.playerStats();
    assert.ok(Object.isFrozen(stats));
    assert.ok(Object.isFrozen(stats.recentSkillUpdates));
    assert.equal(stats.skillCount, 25);
  } finally { cleanup(handle, world); }
});

test("malformed skill snapshots are rejected with a static error", async () => {
  const f = fixture();
  const handle = await lobby(f);
  const world = await readyWorld(f, handle);
  try {
    const cases = [
      [0, { ...validSkill(), extra: 1 }], [0, { experience: 1, visibleLevel: 1 }], [0, { ...validSkill(), baseLevel: undefined }],
      [0, validSkill({ baseLevel: 100 })], [23, validSkill({ baseLevel: 100 })], [24, validSkill({ baseLevel: 121 })], [0, validSkill({ baseLevel: -1 })],
      [0, validSkill({ experience: 2147483648 })], [0, validSkill({ experience: -2147483649 })], [0, validSkill({ experience: 1.5 })], [0, validSkill({ experience: "1" })],
      [0, validSkill({ visibleLevel: 256 })], [0, validSkill({ visibleLevel: -1 })], [0, validSkill({ visibleLevel: 1.5 })],
      [0, null], [0, []], [0, "x"], [0, 5],
    ];
    for (const [id, value] of cases) {
      f.state.skillValue = value;
      assert.throws(() => world.skill(id), /^Error: original skill unavailable$/);
    }
    for (const value of [null, [], validStats({ skillCount: 24 }), { ...validStats(), extra: 1 }]) {
      f.state.statsValue = value;
      assert.throws(() => world.playerStats(), /^Error: original player stats unavailable$/);
    }
    assert.equal(world.state().phase, "worldReady");
  } finally { cleanup(handle, world); }
});

test("skill ids are strict integers in 0 to 24 and queries are gated before ready and after close", async () => {
  const f = fixture();
  const handle = await lobby(f);
  const world = handle.startWorld("ws://localhost:8110/world", "{}", { playerStats: true });
  assert.throws(() => world.skill(0), /^Error: original world state unavailable$/);
  assert.throws(() => world.playerStats(), /^Error: original world state unavailable$/);
  const socket = f.state.sockets[1];
  socket.readyState = 1;
  socket.onopen();
  message(socket, [0]);
  message(socket, [2]);
  await world.ready;
  f.state.skillValue = validSkill();
  f.state.statsValue = validStats();
  const before = f.state.calls.length;
  for (const id of [-1, 25, 1.5, "0", null, undefined, NaN, 0n, true, {}]) assert.throws(() => world.skill(id), /^Error: bounded original skill ID required$/);
  assert.equal(f.state.calls.length, before);
  assert.equal(world.skill(0).baseLevel, 10);
  world.close();
  assert.throws(() => world.skill(0), /^Error: original world state unavailable$/);
  assert.throws(() => world.playerStats(), /^Error: original world state unavailable$/);
  assert.equal(Object.hasOwn(world.state(), "playerStats"), false);
  assert.equal(world.state().phase, "closed");
  handle.close();
  assert.equal(f.state.worldClosed, 1);
  assert.equal(f.state.worldFreed, 1);
});

test("closing during a query withholds the value and releases the world once", async () => {
  for (const query of [(world) => world.skill(1), (world) => world.playerStats()]) {
    const f = fixture();
    const handle = await lobby(f);
    const world = await readyWorld(f, handle);
    f.state.skillValue = validSkill();
    f.state.statsValue = validStats();
    f.state.onQuery = () => world.close();
    assert.throws(() => query(world), /^Error: original (skill|player stats) unavailable$/);
    assert.equal(world.state().phase, "closed");
    world.close();
    handle.close();
    assert.equal(f.state.worldClosed, 1);
    assert.equal(f.state.worldFreed, 1);
  }
});

test("player stats sessions expire at the diagnostic lifetime and then refuse queries and drop the summary", async () => {
  const f = fixture();
  const handle = await lobby(f);
  const world = await readyWorld(f, handle, { timeoutMs: 20, lifetimeMs: 60 });
  f.state.skillValue = validSkill();
  assert.equal(world.skill(0).baseLevel, 10);
  await delay(120);
  const after = world.state();
  assert.equal(after.phase, "failed");
  assert.equal(after.error, "original world diagnostic lifetime reached");
  assert.equal(Object.hasOwn(after, "playerStats"), false);
  assert.throws(() => world.skill(0), /^Error: original world state unavailable$/);
  assert.equal(f.state.worldClosed, 1);
  assert.equal(f.state.worldFreed, 1);
  assert.equal(f.state.sockets[1].closed, 1);
  handle.close();
});

test("cycleInput passes the world session to the input once ready and flushes what it queued", async () => {
  const f = fixture();
  const handle = await lobby(f);
  const world = handle.startWorld("ws://localhost:8110/world", "{}", { playerStats: true });
  try {
    const input = { cycle: () => { throw new Error("must not run before ready"); } };
    assert.throws(() => world.cycleInput(input, 1, true), /^Error: original world state unavailable$/);
    const socket = f.state.sockets[1];
    socket.readyState = 1;
    socket.onopen();
    message(socket, [0]);
    message(socket, [2]);
    await world.ready;
    const seen = [];
    const ok = { cycle: (session, now, rendering) => { seen.push([session, now, rendering]); f.state.pending = new Uint8Array([77]); } };
    world.cycleInput(ok, 1234, true);
    assert.equal(seen.length, 1);
    assert.equal(seen[0][0], f.state.world);
    assert.deepEqual(seen[0].slice(1), [1234, true]);
    assert.deepEqual(f.state.sent.at(-1), [77]);
    assert.throws(() => world.cycleInput({}, 1, true), /^Error: client input cycle arguments required$/);
    assert.throws(() => world.cycleInput(ok, Number.NaN, true), /^Error: client input cycle arguments required$/);
    assert.throws(() => world.cycleInput(ok, 1, "yes"), /^Error: client input cycle arguments required$/);
    assert.throws(() => world.cycleInput({ cycle: () => { throw new Error("FAKE_PRIVATE"); } }, 1, true), /^Error: original world operation failed$/);
    assert.throws(() => world.cycleInput(ok, 1, true), /session is closed|unavailable/);
  } finally { cleanup(handle, world); }
});

test("a world without state options exposes no cycleInput", async () => {
  const f = fixture();
  const handle = await lobby(f);
  const world = handle.startWorld("ws://localhost:8110/world", "{}", {});
  try { assert.equal(Object.hasOwn(world, "cycleInput"), false); } finally { cleanup(handle, world); }
});
