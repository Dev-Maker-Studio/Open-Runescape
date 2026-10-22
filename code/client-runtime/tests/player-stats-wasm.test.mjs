import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import init, { OriginalLobbySession } from "../../web/static/protocol-runtime.js";

await init({ module_or_path: await readFile(new URL("../../web/static/protocol-runtime_bg.wasm", import.meta.url)) });
const fixture = (name) => readFile(new URL(`../../login/tests/fixtures/${name}`, import.meta.url));
const lobbySuccess = await fixture("lobby-success.wire");
const incoming = await fixture("isaac-incoming.words");
const outgoing = await fixture("isaac-outgoing.words");
const worldSuccess = await fixture("world-success.wire");
const initialWorld = await fixture("world-state-standard-104.payload");
const metadata = new Map((await readFile(new URL("../../protocol/tests/fixtures/game-server.tsv", import.meta.url), "utf8")).trim().split("\n").map((line) => {
  const [wire, , name, size] = line.split("\t");
  return [Number(wire), { name, size: Number(size) }];
}));
const opcode = (name) => [...metadata].find(([, entry]) => entry.name === name)[0];
const UPDATE_STAT = opcode("UPDATE_STAT");
const RUN_ENERGY = opcode("RUN_ENERGY");
const PLAYER_WEIGHT = opcode("PLAYER_WEIGHT");
const PING = opcode("PING");

function config() {
  return {
    identity: { kind: "username", username: "offline_Œ_727" },
    password: "OFFLINE_ONLY_€",
    seed: [1, 4294967294, 0x12345678, 0x80000000],
    socialNonce: [0xfedcba98, 0x76543210], clientRandomSeed: [0x01234567, 0x89abcdef],
    gameId: 0, language: 2,
    random24: Array.from({ length: 24 }, (_, i) => (i * 7 + 3) & 255),
    affiliateP: "FAKE_AFFILIATE_™",
    preferences: Array.from({ length: 35 }, (_, i) => (i * 11 + 27) & 255),
    loginToken: "FAKE_LOGIN_€", affiliate: 0x89abcdef | 0, sessionToken: 0x76543210,
    js5AuthToken: "FAKE_JS5_ÿ",
    indexCrcs: Array.from({ length: 36 }, (_, i) => (i * 0x1234567 + 0x80000000) >>> 0),
  };
}

function worldConfig() {
  const supplied = config();
  delete supplied.password;
  delete supplied.gameId;
  delete supplied.language;
  return {
    ...supplied,
    reconnecting: false, displayMode: 2, screenWidth: 1280, screenHeight: 720, antiAliasing: 4,
    machineInfo: {
      operatingSystem: 3, x64Os: true, osVendor: 9, javaVendor: 4,
      javaBuild: 17, javaSubBuild: 2, javaUpdate: 3, unknownFlag: false,
      maxMemory: 4096, processors: 8, ram: 0xabcdef, cpuClock: 4200,
      gpuModel: "FAKE_GPU_€", gpuDriverVersion: "FAKE_DRIVER_™", directxVersion: "12.1", directxLetter: "A",
      dxDriverMonth: 10, dxDriverYear: 2026, cpuType: "FAKE_CPU", cpuData: "OFFLINE_CPU_Œ",
      cpuCores: 16, cpuFamilyId: 254, cpuFeatureFlags: [0x89abcdef, 0x12345678, 0x80000000], cpuModelInfo: 0xfedcba98,
    },
    varVerifyId: 0xfedcba98 | 0, machineUid: [0x88776655, 0x44332211],
    loginToken: "FAKE_CLIENT_LOGIN_€", supplementaryLoginData: "FAKE_EXTRA_ÿ",
    jagtheoraLoaded: true, isApplet: false, hasSignedTermsAgreement: true, loginType: 7,
    js5AuthToken: "FAKE_WORLD_JS5_™", differentWorld: true, worldId: 42,
  };
}

function server() {
  let at = 0;
  return (id, payload = Buffer.alloc(0)) => {
    const encrypt = (byte) => {
      assert.ok(at * 4 < incoming.length);
      return (byte + incoming.readUInt32BE(4 * at++)) & 255;
    };
    const header = id < 128 ? [encrypt(id)] : [encrypt((id >> 8) + 128), encrypt(id & 255)];
    const entry = metadata.get(id);
    assert.ok(entry);
    if (entry.size === -1) header.push(payload.length);
    else if (entry.size === -2) header.push(payload.length >> 8, payload.length & 255);
    else assert.equal(payload.length, entry.size);
    return Buffer.concat([Buffer.from(header), payload]);
  };
}

function createdWorld({ enabled = true, varpCount, inventory = false } = {}) {
  const lobby = new OriginalLobbySession(JSON.stringify(config()));
  let world;
  try {
    assert.deepEqual([...lobby.outgoing()], [14]);
    assert.deepEqual(JSON.parse(lobby.feed(Uint8Array.of(0))), { consumed: 1, event: null });
    assert.ok(lobby.outgoing().length > 0);
    assert.deepEqual(JSON.parse(lobby.feed(lobbySuccess)), { consumed: lobbySuccess.length, event: { kind: "loginAccepted" } });
    assert.equal(lobby.phase(), "lobby");
    world = varpCount === undefined ? lobby.startWorld(JSON.stringify(worldConfig())) : lobby.startWorldState(JSON.stringify(worldConfig()), varpCount);
    assert.equal(world.phase(), "sendHandshake");
    if (inventory) assert.equal(world.enableInventory(), undefined);
    if (enabled) assert.equal(world.enablePlayerStats(), undefined);
    return world;
  } catch (error) { world?.close(); world?.free(); throw error; }
  finally { lobby.close(); lobby.free(); }
}

function ready(options = {}) {
  const world = createdWorld(options);
  const encode = server();
  try {
    assert.deepEqual([...world.outgoing()], [14]);
    assert.deepEqual(JSON.parse(world.feed(Uint8Array.of(0))), { consumed: 1, event: null });
    assert.ok(world.outgoing().length > 0);
    if (options.confirmation) {
      assert.deepEqual(JSON.parse(world.feed(Uint8Array.of(1))).event, { kind: "confirmationRequired" });
      world.confirm();
      assert.deepEqual([...world.outgoing()], [(26 + outgoing.readUInt32BE(0)) & 255]);
    }
    const reply = Buffer.from(worldSuccess);
    reply.writeUInt16BE(1, 8);
    assert.deepEqual(JSON.parse(world.feed(reply)).event, { kind: "loginAccepted" });
    assert.deepEqual(JSON.parse(world.feed(encode(85, initialWorld))).event, { kind: "worldReady" });
    assert.equal(world.phase(), "worldReady");
    return { world, encode };
  } catch (error) { world.close(); world.free(); throw error; }
}

function feedChunks(world, bytes, size = bytes.length) {
  const events = [];
  for (let start = 0; start < bytes.length; start += size) {
    const chunk = bytes.subarray(start, start + size);
    let at = 0;
    while (at < chunk.length) {
      const result = JSON.parse(world.feed(chunk.subarray(at)));
      assert.ok(result.consumed > 0 && result.consumed <= chunk.length - at);
      at += result.consumed;
      if (result.event) events.push(result.event);
    }
  }
  return events;
}

function report(world) {
  const value = JSON.parse(world.report());
  const text = JSON.stringify(value).toLowerCase();
  for (const secret of ["password", "seed", "token", "offline_only", "offline_œ", "fake_login", "fake_js5", "fake_world"]) assert.equal(text.includes(secret), false);
  return value;
}

const emptyStats = () => ({ skillCount: 25, updates: 0, statUpdates: 0, runEnergyUpdates: 0, playerWeightUpdates: 0, recentSkillUpdates: Array(32).fill(0), skillUpdateCycle: 0, runEnergy: 0, playerWeight: 0 });
const emptySkill = { experience: 0, visibleLevel: 0, baseLevel: 0 };
const stats = (world) => JSON.parse(world.playerStats());
const skill = (world, id) => JSON.parse(world.skill(id));

function stat(id, experience, visibleLevel) {
  const bytes = Buffer.alloc(6);
  bytes.writeInt32BE(experience);
  bytes[4] = id;
  bytes[5] = visibleLevel;
  return bytes;
}

function weight(value) {
  const bytes = Buffer.alloc(2);
  bytes.writeInt16BE(value);
  return bytes;
}

function statEvent(id, experience, visibleLevel, baseLevel) {
  return { kind: "statUpdated", opcode: UPDATE_STAT, name: "UPDATE_STAT", skillId: id, experience, visibleLevel, baseLevel };
}

function terminal(world, phase) {
  assert.equal(world.phase(), phase);
  assert.deepEqual(report(world), { phase, accepted: false, worldReady: false, initialWorld: null });
  assert.throws(() => world.skill(0), /unavailable|closed|failed/);
  assert.throws(() => world.playerStats(), /unavailable|closed|failed/);
  assert.throws(() => world.enablePlayerStats(), /before handshake|closed|failed/);
  assert.throws(() => world.varp(0), /unavailable|closed|failed/);
  assert.throws(() => world.container(0, false), /unavailable|closed|failed/);
  assert.throws(() => world.item(0, false, 0), /unavailable|closed|failed/);
}

function ping(world, word = 0) {
  for (let i = 0; i < 50; i++) { world.gameTick(); assert.deepEqual([...world.outgoing()], []); }
  world.gameTick();
  assert.deepEqual([...world.outgoing()], [outgoing.readUInt32BE(word * 4) & 255]);
  world.markFlushed();
}

test("actual WASM stats opt-in is explicit early once and query snapshots start at native zero defaults", () => {
  const world = createdWorld();
  try {
    assert.deepEqual(stats(world), emptyStats());
    assert.deepEqual(report(world).playerStats, emptyStats());
    for (let id = 0; id < 25; id++) assert.deepEqual(skill(world, id), emptySkill);
    assert.deepEqual(skill(world, -0), emptySkill);
    assert.throws(() => world.enablePlayerStats(), /world player stats must be enabled before handshake/);
    assert.equal(world.phase(), "sendHandshake");
    assert.deepEqual([...world.outgoing()], [14]);
    assert.throws(() => world.enablePlayerStats(), /world player stats must be enabled before handshake/);
    assert.deepEqual(stats(world), emptyStats());
    world.close();
    terminal(world, "closed");
    world.close();
    terminal(world, "closed");
  } finally { world.close(); world.free(); }
});

test("actual WASM disabled stats stay opaque even for invalid skill payloads and preserve legacy schemas", () => {
  const { world, encode } = ready({ enabled: false, inventory: true, varpCount: 4 });
  try {
    const before = report(world);
    assert.equal(Object.hasOwn(before, "playerStats"), false);
    assert.deepEqual(before.inventory, { containers: 0, slots: 0, tooltipBytes: 0, updates: 0, fullUpdates: 0, partialUpdates: 0, stops: 0 });
    assert.deepEqual(before.variables, { count: 4, scheduled: 0, serverAssignments: 0, published: 0, clears: 0, nonZero: 0 });
    for (const [id, payload] of [[UPDATE_STAT, stat(255, -2147483648, 255)], [RUN_ENERGY, Buffer.from([255])], [PLAYER_WEIGHT, weight(-32768)]]) {
      assert.deepEqual(feedChunks(world, encode(id, payload), 1), [{ kind: "opaquePacket", opcode: id, name: metadata.get(id).name, payloadBytes: payload.length }]);
    }
    assert.deepEqual(report(world), before);
    assert.throws(() => world.playerStats(), /world player stats state unavailable/);
    assert.throws(() => world.skill(0), /world player stats state unavailable/);
    assert.throws(() => world.enablePlayerStats(), /world player stats must be enabled before handshake/);
    assert.equal(world.varp(0), 0);
    assert.equal(world.container(0, false), "null");
    world.finish();
    terminal(world, "closed");
  } finally { world.close(); world.free(); }
});

test("actual WASM late enable after only the handshake drain is nonterminal and cannot opt in", () => {
  const world = createdWorld({ enabled: false });
  try {
    assert.deepEqual([...world.outgoing()], [14]);
    assert.throws(() => world.enablePlayerStats(), /world player stats must be enabled before handshake/);
    assert.equal(world.phase(), "awaitHandshake");
    assert.equal(Object.hasOwn(report(world), "playerStats"), false);
  } finally { world.close(); world.free(); }
});

for (const size of [1, 2, 3, 7, 31, 65535]) test(`actual WASM stats use the existing packet cipher and preserve fragmentation (${size})`, () => {
  const { world, encode } = ready();
  try {
    const wire = Buffer.concat([encode(UPDATE_STAT, stat(24, 2147483647, 255)), encode(RUN_ENERGY, Buffer.from([255])), encode(PLAYER_WEIGHT, weight(-32768)), encode(PING)]);
    assert.deepEqual(feedChunks(world, wire, size), [
      statEvent(24, 2147483647, 255, 120),
      { kind: "runEnergyUpdated", opcode: RUN_ENERGY, name: "RUN_ENERGY", runEnergy: 255 },
      { kind: "playerWeightUpdated", opcode: PLAYER_WEIGHT, name: "PLAYER_WEIGHT", playerWeight: -32768 },
      { kind: "opaquePacket", opcode: PING, name: "PING", payloadBytes: 0 },
    ]);
    assert.deepEqual(skill(world, 24), { experience: 2147483647, visibleLevel: 255, baseLevel: 120 });
    const expected = { ...emptyStats(), updates: 3, statUpdates: 1, runEnergyUpdates: 1, playerWeightUpdates: 1, skillUpdateCycle: 1, runEnergy: 255, playerWeight: -32768 };
    expected.recentSkillUpdates[0] = 24;
    assert.deepEqual(stats(world), expected);
    assert.deepEqual(report(world).playerStats, expected);
    ping(world);
    world.finish();
    terminal(world, "closed");
  } finally { world.close(); world.free(); }
});

for (const size of [1, 31, 65535]) test(`actual WASM stats do not consume login or initial-map frame boundaries (${size})`, () => {
  const world = createdWorld();
  const encode = server();
  try {
    assert.deepEqual([...world.outgoing()], [14]);
    world.feed(Uint8Array.of(0));
    assert.ok(world.outgoing().length > 0);
    const reply = Buffer.from(worldSuccess);
    reply.writeUInt16BE(1, 8);
    const first = encode(85, initialWorld);
    const wire = Buffer.concat([reply, first, encode(UPDATE_STAT, stat(24, 104273167, 120)), encode(PING)]);
    assert.deepEqual(feedChunks(world, wire, size), [
      { kind: "loginAccepted" }, { kind: "worldReady" },
      statEvent(24, 104273167, 120, 120),
      { kind: "opaquePacket", opcode: PING, name: "PING", payloadBytes: 0 },
    ]);
    assert.equal(report(world).initialWorld.playerIndex, 1);
    assert.deepEqual(skill(world, 24), { experience: 104273167, visibleLevel: 120, baseLevel: 120 });
    assert.equal(stats(world).statUpdates, 1);
  } finally { world.close(); world.free(); }
});

test("actual WASM enabled stats cannot substitute for the required initial map packet", () => {
  const world = createdWorld({ inventory: true, varpCount: 4 });
  const encode = server();
  try {
    world.outgoing();
    world.feed(Uint8Array.of(0));
    world.outgoing();
    const reply = Buffer.from(worldSuccess);
    reply.writeUInt16BE(1, 8);
    world.feed(reply);
    const packet = Buffer.concat([encode(UPDATE_STAT, stat(0, 83, 2)).subarray(0, UPDATE_STAT < 128 ? 1 : 2), Buffer.from([0, 6]), stat(0, 83, 2)]);
    assert.throws(() => feedChunks(world, packet, 1), /invalid initial world/);
    terminal(world, "failed");
  } finally { world.close(); world.free(); }
});

test("actual WASM ring wraps at 32 slots and skill, report, and playerStats queries return fresh snapshots", () => {
  const { world, encode } = ready();
  try {
    const raw = world.playerStats();
    assert.equal(typeof raw, "string");
    const before = JSON.parse(raw);
    const ring = Array(32).fill(0);
    for (let index = 0; index < 70; index++) {
      const id = index % 25;
      ring[index & 31] = id;
      const experience = index % 2 === 0 ? -2147483648 : 2147483647;
      const baseLevel = experience < 0 ? 1 : id === 24 ? 120 : 99;
      assert.deepEqual(feedChunks(world, encode(UPDATE_STAT, stat(id, experience, 255)), 1), [statEvent(id, experience, 255, baseLevel)]);
      assert.deepEqual(skill(world, id), { experience, visibleLevel: 255, baseLevel });
    }
    for (const energy of [0, 100, 101, 255]) feedChunks(world, encode(RUN_ENERGY, Buffer.from([energy])));
    for (const value of [-32768, -1, 0, 32767]) feedChunks(world, encode(PLAYER_WEIGHT, weight(value)));
    assert.deepEqual(before, emptyStats());
    const expected = { ...emptyStats(), updates: 78, statUpdates: 70, runEnergyUpdates: 4, playerWeightUpdates: 4, recentSkillUpdates: ring, skillUpdateCycle: 70, runEnergy: 255, playerWeight: 32767 };
    assert.deepEqual(stats(world), expected);
    assert.deepEqual(Object.keys(stats(world)).sort(), Object.keys(emptyStats()).sort());
    const snapshot = stats(world);
    const reportSnapshot = report(world);
    const skillSnapshot = skill(world, 0);
    snapshot.recentSkillUpdates.fill(255);
    reportSnapshot.playerStats.updates = 999;
    skillSnapshot.experience = 123;
    assert.deepEqual(stats(world), expected);
    assert.notEqual(skill(world, 0).experience, 123);
    world.close();
    terminal(world, "closed");
    assert.deepEqual(JSON.parse(raw), emptyStats());
  } finally { world.close(); world.free(); }
});

test("actual WASM skill IDs reject coercion and all invalid types and bounds without mutating live state", () => {
  const { world, encode } = ready();
  let coerced = 0;
  const coercing = { valueOf() { coerced++; return 0; }, toString() { coerced++; return "0"; }, [Symbol.toPrimitive]() { coerced++; return 0; } };
  const invalid = [undefined, null, true, false, "0", "24", 0n, Symbol("id"), new Number(0), {}, [], [0], () => 0, coercing, NaN, Infinity, -Infinity, -1, 0.5, 25, 255, 256, 2 ** 32, Number.MAX_SAFE_INTEGER, Number.MAX_VALUE];
  try {
    feedChunks(world, encode(UPDATE_STAT, stat(0, -2147483648, 255)));
    const before = report(world);
    for (const id of invalid) assert.throws(() => world.skill(id), /invalid world skill id/);
    assert.equal(coerced, 0);
    assert.equal(world.phase(), "worldReady");
    assert.deepEqual(report(world), before);
    assert.deepEqual(skill(world, -0), { experience: -2147483648, visibleLevel: 255, baseLevel: 1 });
    assert.deepEqual(skill(world, 24), emptySkill);
  } finally { world.close(); world.free(); }
});

test("actual WASM stats coexist with inventory and VARPs and preserve confirmation plus keepalive ordering", () => {
  const { world, encode } = ready({ inventory: true, varpCount: 4, confirmation: true });
  try {
    feedChunks(world, encode(5, Buffer.from([0, 93, 0, 0, 1, 123, 229, 0])));
    feedChunks(world, encode(115, Buffer.from([128, 129, 0])));
    feedChunks(world, encode(UPDATE_STAT, stat(1, -1, 255)));
    assert.deepEqual(JSON.parse(world.item(93, false, 0)), { itemId: 100, amount: 5, tooltip: null });
    assert.deepEqual(skill(world, 1), { experience: -1, visibleLevel: 255, baseLevel: 1 });
    assert.equal(world.varp(1), 0);
    assert.equal(report(world).variables.serverAssignments, 1);
    assert.equal(report(world).inventory.updates, 1);
    assert.equal(stats(world).updates, 1);
    ping(world, 1);
    world.finish();
    terminal(world, "closed");
  } finally { world.close(); world.free(); }
});

for (const id of [25, 255]) test(`actual WASM invalid recognized skill fails the whole session and clears all retained state (${id})`, () => {
  const { world, encode } = ready({ inventory: true, varpCount: 4 });
  try {
    feedChunks(world, encode(5, Buffer.from([0, 93, 0, 0, 1, 123, 229, 0])));
    feedChunks(world, encode(UPDATE_STAT, stat(0, 83, 2)));
    for (let i = 0; i < 51; i++) world.gameTick();
    assert.throws(() => feedChunks(world, encode(UPDATE_STAT, stat(id, 0, 0)), 1), /invalid world player stats packet/);
    terminal(world, "failed");
    assert.throws(() => world.outgoing(), /world session has failed/);
    world.close();
    terminal(world, "closed");
  } finally { world.close(); world.free(); }
});

for (const [id, payload] of [[UPDATE_STAT, stat(24, 1, 1)], [RUN_ENERGY, Buffer.from([255])], [PLAYER_WEIGHT, weight(-32768)]]) test(`actual WASM truncated stats packet fails on finish without partial update (${metadata.get(id).name})`, () => {
  const { world, encode } = ready();
  try {
    const wire = encode(id, payload);
    assert.deepEqual(feedChunks(world, wire.subarray(0, -1), 1), []);
    assert.deepEqual(stats(world), emptyStats());
    assert.throws(() => world.finish(), /world connection ended during packet/);
    terminal(world, "failed");
  } finally { world.close(); world.free(); }
});

for (const [wire, phase] of [[Uint8Array.of(3), "rejected"], [Uint8Array.of(21, 2), "queued"], [Uint8Array.of(23), "retry"], [Uint8Array.of(15), "reconnect"]]) test(`actual WASM login terminal clears enabled stats (${phase})`, () => {
  const world = createdWorld({ inventory: true, varpCount: 4 });
  try {
    world.outgoing();
    world.feed(Uint8Array.of(0));
    world.outgoing();
    world.feed(wire);
    terminal(world, phase);
    world.finish();
    terminal(world, "closed");
  } finally { world.close(); world.free(); }
});

test("actual WASM stats queries and enable use the existing monotonic lifetime guard", () => {
  const descriptor = Object.getOwnPropertyDescriptor(globalThis, "performance");
  let now = 0;
  Object.defineProperty(globalThis, "performance", { configurable: true, value: { now: () => now } });
  try {
    for (const operation of [(world) => world.skill(0), (world) => world.playerStats(), (world) => world.enablePlayerStats()]) {
      now = 0;
      const world = createdWorld();
      try {
        now = 300000;
        assert.throws(() => operation(world), /world session lifetime limit/);
        terminal(world, "failed");
      } finally { world.close(); world.free(); }
    }
  } finally {
    if (descriptor) Object.defineProperty(globalThis, "performance", descriptor);
    else delete globalThis.performance;
  }
});
