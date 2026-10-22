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
  return (opcode, payload = Buffer.alloc(0)) => {
    const encrypt = (byte) => {
      assert.ok(at * 4 < incoming.length);
      return (byte + incoming.readUInt32BE(4 * at++)) & 255;
    };
    const header = opcode < 128 ? [encrypt(opcode)] : [encrypt((opcode >> 8) + 128), encrypt(opcode & 255)];
    const entry = metadata.get(opcode);
    assert.ok(entry);
    if (entry.size === -1) header.push(payload.length);
    else if (entry.size === -2) header.push(payload.length >> 8, payload.length & 255);
    else assert.equal(payload.length, entry.size);
    return Buffer.concat([Buffer.from(header), payload]);
  };
}

function createdWorld({ enabled = true, varpCount } = {}) {
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
    if (enabled) assert.equal(world.enableInventory(), undefined);
    return world;
  } catch (error) { world?.close(); world?.free(); throw error; }
  finally { lobby.close(); lobby.free(); }
}

async function ready(options = {}) {
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
  for (const secret of ["password", "seed", "token", "offline_only", "offline_œ", "fake_login", "fake_js5", "fake_world", "tooltip-text"]) assert.equal(text.includes(secret), false);
  return value;
}

function container(world, id, negativeKey) {
  return JSON.parse(world.container(id, negativeKey));
}

function item(world, id, negativeKey, slot) {
  return JSON.parse(world.item(id, negativeKey, slot));
}

const emptyInventory = { containers: 0, slots: 0, tooltipBytes: 0, updates: 0, fullUpdates: 0, partialUpdates: 0, stops: 0 };

function full(id = 93, negativeKey = false, entries = [[100, 5], [65534, -2147483648]], tooltips) {
  const header = Buffer.alloc(5);
  header.writeUInt16BE(id);
  header[2] = Number(negativeKey) | (tooltips ? 2 : 0);
  header.writeUInt16BE(entries.length, 3);
  const parts = [header];
  for (const [itemId, amount] of entries) {
    if (amount < 0 || amount >= 255) {
      const be = Buffer.alloc(4);
      be.writeInt32BE(amount);
      parts.push(Buffer.from([129, be[1], be[0], be[3], be[2]]));
    } else parts.push(Buffer.from([(128 - amount) & 255]));
    const encoded = itemId + 1;
    parts.push(Buffer.from([((encoded & 255) + 128) & 255, encoded >> 8]));
  }
  if (tooltips) {
    const mask = Buffer.alloc(Math.ceil(entries.length / 8));
    for (const slot of tooltips.keys()) mask[slot >> 3] |= 1 << (slot & 7);
    parts.push(mask);
    for (let slot = 0; slot < entries.length; slot++) if (tooltips.has(slot)) parts.push(tooltips.get(slot), Buffer.from([0]));
  }
  return Buffer.concat(parts);
}

function smart(value) {
  return Buffer.from(value < 128 ? [value] : [(value + 32768) >> 8, value & 255]);
}

function partial(id, negativeKey, entries, tooltipMode = false) {
  const head = Buffer.alloc(3);
  head.writeUInt16BE(id);
  head[2] = Number(negativeKey) | (tooltipMode ? 2 : 0);
  const parts = [head];
  for (const { slot, itemId, amount = 0, tooltipOp = 1, tooltip } of entries) {
    parts.push(smart(tooltipMode ? slot * 4 + tooltipOp : slot));
    const encoded = Buffer.alloc(2);
    encoded.writeUInt16BE(itemId + 1);
    parts.push(encoded);
    if (itemId !== -1) {
      if (amount < 0 || amount >= 255) {
        const escaped = Buffer.alloc(5);
        escaped[0] = 255;
        escaped.writeInt32BE(amount, 1);
        parts.push(escaped);
      } else parts.push(Buffer.from([amount]));
    }
    if (tooltipMode && tooltipOp === 2) parts.push(tooltip, Buffer.from([0]));
  }
  return Buffer.concat(parts);
}

function stop(id, negativeKey) {
  const bytes = Buffer.alloc(3);
  bytes[0] = negativeKey ? 255 : 0;
  bytes.writeUInt16LE(id, 1);
  return bytes;
}

function inventoryEvent(opcode, id, negativeKey, update, recordCount, slotCount, containerPresent = true) {
  return { kind: "inventoryUpdated", opcode, name: metadata.get(opcode).name, id, negativeKey, update, recordCount, slotCount, containerPresent };
}

function terminal(world, phase) {
  assert.equal(world.phase(), phase);
  assert.deepEqual(report(world), { phase, accepted: false, worldReady: false, initialWorld: null });
  assert.throws(() => world.container(0, false), /closed|failed/);
  assert.throws(() => world.item(0, false, 0), /closed|failed/);
  assert.throws(() => world.outgoing(), /closed|failed/);
  assert.throws(() => world.feed(Uint8Array.of(0)), /closed|failed/);
}

test("inventory packet builders match the native revision-727 wire examples", () => {
  assert.deepEqual([...full()], [0, 93, 0, 0, 2, 123, 229, 0, 129, 0, 128, 0, 0, 127, 255]);
  assert.deepEqual([...partial(93, false, [{ slot: 4, itemId: 42, amount: 7 }])], [0, 93, 0, 4, 0, 43, 7]);
  assert.deepEqual([...stop(93, false)], [0, 93, 0]);
  assert.deepEqual([...stop(0x5678, true)], [255, 0x78, 0x56]);
});

test("actual WASM inventory opt-in is early, non-repeatable, and nullable before acceptance", () => {
  const world = createdWorld();
  try {
    assert.deepEqual(report(world).inventory, emptyInventory);
    assert.equal(world.container(0, false), "null");
    assert.equal(world.item(65535, true, 65534), "null");
    assert.throws(() => world.enableInventory(), /world inventory must be enabled before handshake/);
    assert.equal(world.phase(), "sendHandshake");
    assert.deepEqual(report(world).inventory, emptyInventory);
    assert.deepEqual([...world.outgoing()], [14]);
    assert.throws(() => world.enableInventory(), /world inventory must be enabled before handshake/);
    assert.equal(world.container(93, false), "null");
    assert.deepEqual(JSON.parse(world.feed(Uint8Array.of(0))), { consumed: 1, event: null });
    assert.ok(world.outgoing().length > 0);
    assert.equal(world.item(93, false, 0), "null");
    world.close();
    terminal(world, "closed");
    assert.throws(() => world.enableInventory(), /world session is closed/);
    world.close();
    terminal(world, "closed");
  } finally { world.close(); world.free(); }
});

test("actual WASM late opt-in cannot change the default opaque world mode", () => {
  const world = createdWorld({ enabled: false });
  try {
    assert.equal(Object.hasOwn(report(world), "inventory"), false);
    assert.throws(() => world.container(93, false), /world inventory state unavailable/);
    assert.throws(() => world.item(93, false, 0), /world inventory state unavailable/);
    assert.deepEqual([...world.outgoing()], [14]);
    assert.throws(() => world.enableInventory(), /world inventory must be enabled before handshake/);
    assert.equal(world.phase(), "awaitHandshake");
    assert.equal(Object.hasOwn(report(world), "inventory"), false);
  } finally { world.close(); world.free(); }
});

for (const varpCount of [undefined, 4]) test(`actual WASM inventory is independent of variable state (${varpCount ?? "legacy handoff"})`, async () => {
  const { world, encode } = await ready({ varpCount, confirmation: varpCount === 4 });
  try {
    assert.equal(Object.hasOwn(report(world), "variables"), varpCount !== undefined);
    assert.deepEqual(report(world).inventory, emptyInventory);
    assert.deepEqual(feedChunks(world, encode(5, full())), [inventoryEvent(5, 93, false, "full", 2, 2)]);
    assert.deepEqual(item(world, 93, false, 1), { itemId: 65534, amount: -2147483648, tooltip: null });
    if (varpCount === undefined) assert.throws(() => world.varp(0), /world variable state unavailable/);
    else assert.equal(world.varp(0), 0);
  } finally { world.close(); world.free(); }
});

for (const size of [1, 2, 3, 7, 31, 65535]) test(`actual WASM full and partial inventory share ISAAC and preserve fragmentation (${size})`, async () => {
  const { world, encode } = await ready();
  try {
    const ping = [...metadata].find(([, value]) => value.name === "PING")[0];
    const wire = Buffer.concat([
      encode(5, full()),
      encode(4, partial(93, false, [{ slot: 4, itemId: 42, amount: 7 }])),
      encode(ping),
    ]);
    assert.deepEqual(feedChunks(world, wire, size), [
      inventoryEvent(5, 93, false, "full", 2, 2),
      inventoryEvent(4, 93, false, "partial", 1, 5),
      { kind: "opaquePacket", opcode: ping, name: "PING", payloadBytes: 0 },
    ]);
    assert.deepEqual(container(world, 93, false), { id: 93, negativeKey: false, slots: 5 });
    assert.deepEqual(item(world, 93, false, 0), { itemId: 100, amount: 5, tooltip: null });
    assert.deepEqual(item(world, 93, false, 1), { itemId: 65534, amount: -2147483648, tooltip: null });
    assert.deepEqual(item(world, 93, false, 3), { itemId: -1, amount: 0, tooltip: null });
    assert.deepEqual(item(world, 93, false, 4), { itemId: 42, amount: 7, tooltip: null });
    assert.equal(item(world, 93, false, 5), null);
    assert.equal(container(world, 93, true), null);
    assert.equal(item(world, 93, true, 0), null);
    assert.deepEqual(report(world).inventory, { containers: 1, slots: 5, tooltipBytes: 0, updates: 2, fullUpdates: 1, partialUpdates: 1, stops: 0 });
  } finally { world.close(); world.free(); }
});

test("actual WASM full replacement clears old slots without shrinking allocated capacity and snapshots are isolated", async () => {
  const { world, encode } = await ready();
  try {
    feedChunks(world, encode(5, full()));
    feedChunks(world, encode(4, partial(93, false, [{ slot: 4, itemId: 42, amount: 2147483647 }])));
    const beforeContainer = container(world, 93, false);
    const beforeItem = item(world, 93, false, 4);
    const beforeReport = report(world);
    const raw = world.item(93, false, 4);
    assert.equal(typeof raw, "string");
    assert.deepEqual(JSON.parse(raw), { itemId: 42, amount: 2147483647, tooltip: null });
    assert.deepEqual(feedChunks(world, encode(5, full(93, false, [[-1, -1]]))), [inventoryEvent(5, 93, false, "full", 1, 5)]);
    assert.deepEqual(item(world, 93, false, 0), { itemId: -1, amount: -1, tooltip: null });
    assert.deepEqual(item(world, 93, false, 4), { itemId: -1, amount: 0, tooltip: null });
    assert.deepEqual(container(world, 93, false), { id: 93, negativeKey: false, slots: 5 });
    assert.deepEqual(beforeItem, { itemId: 42, amount: 2147483647, tooltip: null });
    assert.equal(beforeReport.inventory.updates, 2);
    beforeItem.amount = 17;
    beforeContainer.slots = 1;
    beforeReport.inventory.updates = 999;
    assert.equal(container(world, 93, false).slots, 5);
    assert.equal(item(world, 93, false, 4).amount, 0);
    assert.equal(report(world).inventory.updates, 3);
    world.close();
    terminal(world, "closed");
    assert.deepEqual(JSON.parse(raw), { itemId: 42, amount: 2147483647, tooltip: null });
  } finally { world.close(); world.free(); }
});

test("actual WASM namespace stop removes only the addressed bank and empty updates remain bounded", async () => {
  const { world, encode } = await ready();
  try {
    feedChunks(world, encode(5, full(65535, false)));
    feedChunks(world, encode(5, full(65535, true, [[1, 254]])));
    feedChunks(world, encode(4, partial(0, false, [])));
    assert.equal(container(world, -0, false), null);
    assert.equal(item(world, 0, false, -0), null);
    assert.deepEqual(feedChunks(world, encode(42, stop(65535, false)), 1), [inventoryEvent(42, 65535, false, "stopTransmit", 0, 0, false)]);
    assert.equal(container(world, 65535, false), null);
    assert.equal(item(world, 65535, false, 0), null);
    assert.deepEqual(container(world, 65535, true), { id: 65535, negativeKey: true, slots: 1 });
    assert.deepEqual(item(world, 65535, true, 0), { itemId: 1, amount: 254, tooltip: null });
    assert.equal(item(world, 65535, true, 65534), null);
    assert.deepEqual(report(world).inventory, { containers: 1, slots: 1, tooltipBytes: 0, updates: 4, fullUpdates: 2, partialUpdates: 1, stops: 1 });
    feedChunks(world, encode(42, stop(65535, true)));
    feedChunks(world, encode(42, stop(65535, true)));
    assert.deepEqual(report(world).inventory, { ...emptyInventory, updates: 6, fullUpdates: 2, partialUpdates: 1, stops: 3 });
    assert.equal(world.finish(), undefined);
    terminal(world, "closed");
  } finally { world.close(); world.free(); }
});

test("actual WASM inventory preserves and clears native CP1252 tooltips without exposing their text in reports", async () => {
  const { world, encode } = await ready();
  try {
    feedChunks(world, encode(5, full(93, false, [[100, 255]], new Map([[0, Buffer.from("tooltip-text")]]))));
    assert.deepEqual(item(world, 93, false, 0), { itemId: 100, amount: 255, tooltip: "tooltip-text" });
    assert.equal(report(world).inventory.tooltipBytes, Buffer.byteLength("tooltip-text"));
    feedChunks(world, encode(4, partial(93, false, [{ slot: 0, itemId: 65534, amount: 2147483647, tooltipOp: 0 }], true)));
    assert.deepEqual(item(world, 93, false, 0), { itemId: 65534, amount: 2147483647, tooltip: "tooltip-text" });
    feedChunks(world, encode(4, partial(93, false, [{ slot: 0, itemId: 65534, amount: -2147483648, tooltipOp: 2, tooltip: Buffer.from([0x80]) }], true)));
    assert.deepEqual(item(world, 93, false, 0), { itemId: 65534, amount: -2147483648, tooltip: "€" });
    assert.equal(report(world).inventory.tooltipBytes, 3);
    feedChunks(world, encode(4, partial(93, false, [{ slot: 0, itemId: -1 }])));
    assert.deepEqual(item(world, 93, false, 0), { itemId: -1, amount: 0, tooltip: null });
    assert.equal(report(world).inventory.tooltipBytes, 0);
  } finally { world.close(); world.free(); }
});

test("actual WASM inventory arguments reject coercion and invalid bounds without changing live state", async () => {
  const { world, encode } = await ready();
  let coerced = 0;
  const coercing = { valueOf() { coerced++; return 93; }, toString() { coerced++; return "93"; }, [Symbol.toPrimitive]() { coerced++; return 93; } };
  const invalidNumbers = [undefined, null, true, false, "93", "0", "65535", 93n, Symbol("id"), new Number(93), {}, [], [93], () => 93, coercing, NaN, Infinity, -Infinity, -1, 0.5, 65536, 2 ** 32, Number.MAX_SAFE_INTEGER, Number.MAX_VALUE];
  const invalidNamespaces = [undefined, null, 0, 1, -0, "true", "false", new Boolean(false), {}, [], coercing, Symbol("namespace"), 1n];
  try {
    feedChunks(world, encode(5, full()));
    const before = report(world);
    for (const id of invalidNumbers) {
      assert.throws(() => world.container(id, false), /invalid world container id/);
      assert.throws(() => world.item(id, false, 0), /invalid world container id/);
    }
    for (const namespace of invalidNamespaces) {
      assert.throws(() => world.container(93, namespace), /invalid world container namespace/);
      assert.throws(() => world.item(93, namespace, 0), /invalid world container namespace/);
    }
    for (const slot of [...invalidNumbers, 65535]) assert.throws(() => world.item(93, false, slot), /invalid world inventory slot/);
    assert.equal(coerced, 0);
    assert.equal(world.phase(), "worldReady");
    assert.deepEqual(report(world), before);
    assert.deepEqual(item(world, 93, false, -0), { itemId: 100, amount: 5, tooltip: null });
    assert.equal(world.container(0, false), "null");
    assert.equal(world.container(65535, true), "null");
    assert.equal(world.item(93, false, 65534), "null");
    feedChunks(world, encode(4, partial(93, false, [{ slot: 0, itemId: 7, amount: 0 }])));
    assert.deepEqual(item(world, 93, false, 0), { itemId: 7, amount: 0, tooltip: null });
  } finally { world.close(); world.free(); }
});

for (const [name, opcode, payload] of [
  ["full missing slot bytes", 5, Buffer.from([0, 93, 0, 0, 1])],
  ["full trailing bytes", 5, Buffer.concat([full(), Buffer.from([0])])],
  ["full missing tooltip terminator", 5, full(93, false, [[1, 1]], new Map([[0, Buffer.from("tooltip-text")]])).subarray(0, -1)],
  ["partial missing record bytes", 4, Buffer.from([0, 93, 0, 4])],
  ["partial truncated escaped amount", 4, Buffer.from([0, 93, 0, 0, 0, 1, 255, 0, 0, 0])],
  ["partial missing tooltip terminator", 4, Buffer.from([0, 93, 2, 2, 0, 1, 1, 65])],
]) test(`actual WASM malformed recognized inventory fails closed and drops retained state (${name})`, async () => {
  const { world, encode } = await ready({ varpCount: 4 });
  try {
    feedChunks(world, encode(5, full()));
    assert.ok(container(world, 93, false));
    assert.throws(() => feedChunks(world, encode(opcode, payload), 1), /invalid world inventory packet/);
    terminal(world, "failed");
    assert.throws(() => world.enableInventory(), /world session has failed/);
    assert.throws(() => world.varp(0), /world session has failed/);
    world.close();
    terminal(world, "closed");
  } finally { world.close(); world.free(); }
});

test("actual WASM truncated inventory frame fails on finish and clears the bank", async () => {
  const { world, encode } = await ready();
  try {
    feedChunks(world, encode(5, full()));
    const incomplete = encode(4, partial(93, false, [{ slot: 0, itemId: 0, amount: 1 }]));
    assert.deepEqual(feedChunks(world, incomplete.subarray(0, -1), 1), []);
    assert.deepEqual(item(world, 93, false, 0), { itemId: 100, amount: 5, tooltip: null });
    assert.throws(() => world.finish(), /world connection ended during packet/);
    terminal(world, "failed");
  } finally { world.close(); world.free(); }
});

test("actual WASM default inventory container budget fails closed instead of retaining an unbounded bank", async () => {
  const { world, encode } = await ready();
  try {
    for (let id = 0; id < 128; id++) feedChunks(world, encode(5, full(id, false, [[1, 1]])));
    assert.deepEqual(report(world).inventory, { containers: 128, slots: 128, tooltipBytes: 0, updates: 128, fullUpdates: 128, partialUpdates: 0, stops: 0 });
    assert.throws(() => feedChunks(world, encode(5, full(128, false, [[1, 1]]))), /invalid world inventory packet/);
    terminal(world, "failed");
  } finally { world.close(); world.free(); }
});

test("actual WASM disabled legacy inventory packets stay opaque, even when their recognized payload would be malformed", async () => {
  const { world, encode } = await ready({ enabled: false });
  try {
    for (const [opcode, payload] of [[5, full()], [4, partial(93, false, [])], [42, stop(93, false)], [5, Buffer.from([0, 93, 0, 0, 1])], [4, Buffer.from([0, 93, 0, 4])]]) {
      assert.deepEqual(feedChunks(world, encode(opcode, payload), 1), [{ kind: "opaquePacket", opcode, name: metadata.get(opcode).name, payloadBytes: payload.length }]);
      assert.equal(Object.hasOwn(report(world), "inventory"), false);
    }
    assert.equal(world.phase(), "worldReady");
    assert.throws(() => world.container(93, false), /world inventory state unavailable/);
    assert.throws(() => world.item(93, false, 0), /world inventory state unavailable/);
    assert.throws(() => world.enableInventory(), /world inventory must be enabled before handshake/);
    world.finish();
    terminal(world, "closed");
  } finally { world.close(); world.free(); }
});
