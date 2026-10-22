import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import init, { OriginalLobbySession } from "../../web/static/protocol-runtime.js";

await init({ module_or_path: await readFile(new URL("../../web/static/protocol-runtime_bg.wasm", import.meta.url)) });
const fixture = (name) => readFile(new URL(`../../login/tests/fixtures/${name}`, import.meta.url));
const success = await fixture("lobby-success.wire");
const incoming = await fixture("isaac-incoming.words");
const outgoing = await fixture("isaac-outgoing.words");
const basic = await fixture("world-list-basic.payload");
const counts = await fixture("world-list-counts.payload");
const metadata = new Map((await readFile(new URL("../../protocol/tests/fixtures/game-server.tsv", import.meta.url), "utf8")).trim().split("\n").map((line) => {
  const [wire, , name, size] = line.split("\t");
  return [Number(wire), { name, size: Number(size) }];
}));

function config() {
  return {
    identity: { kind: "username", username: "offline_Œ_727" },
    password: "OFFLINE_ONLY_€",
    seed: [1, 4294967294, 0x12345678, 0x80000000],
    socialNonce: [0xfedcba98, 0x76543210],
    clientRandomSeed: [0x01234567, 0x89abcdef],
    gameId: 0,
    language: 2,
    random24: Array.from({ length: 24 }, (_, i) => (i * 7 + 3) & 255),
    affiliateP: "FAKE_AFFILIATE_™",
    preferences: Array.from({ length: 35 }, (_, i) => (i * 11 + 27) & 255),
    loginToken: "FAKE_LOGIN_€",
    affiliate: 0x89abcdef | 0,
    sessionToken: 0x76543210,
    js5AuthToken: "FAKE_JS5_ÿ",
    indexCrcs: Array.from({ length: 36 }, (_, i) => (i * 0x1234567 + 0x80000000) >>> 0),
  };
}

function awaiting() {
  const session = new OriginalLobbySession(JSON.stringify(config()));
  assert.equal(session.phase(), "sendHandshake");
  assert.deepEqual(Array.from(session.outgoing()), [14]);
  assert.equal(session.phase(), "awaitHandshake");
  assert.equal(session.outgoing().length, 0);
  assert.deepEqual(JSON.parse(session.feed(Uint8Array.of(0))), { consumed: 1, event: null });
  assert.equal(session.phase(), "sendRequest");
  assert.ok(session.outgoing().length > 0);
  assert.equal(session.phase(), "awaitResponse");
  return session;
}

function accepted(defaultWorld = 42) {
  const session = awaiting();
  const reply = Buffer.from(success);
  const worldAt = reply.length - Buffer.byteLength("\0offline.invalid\0") - 2;
  assert.equal(reply.readUInt16BE(worldAt), 42);
  reply.writeUInt16BE(defaultWorld ?? 65535, worldAt);
  assert.deepEqual(JSON.parse(session.feed(reply)), { consumed: reply.length, event: { kind: "loginAccepted" } });
  assert.equal(session.phase(), "lobby");
  return session;
}

function server() {
  let at = 0;
  return (opcode, payload = Buffer.alloc(0)) => {
    const encrypt = (byte) => (byte + incoming.readUInt32BE(4 * at++)) & 255;
    const header = opcode < 128 ? [encrypt(opcode)] : [encrypt((opcode >> 8) + 128), encrypt(opcode & 255)];
    const { size } = metadata.get(opcode);
    if (size === -1) header.push(payload.length);
    else if (size === -2) header.push(payload.length >> 8, payload.length & 255);
    else assert.equal(payload.length, size);
    return Buffer.concat([Buffer.from(header), payload]);
  };
}

function report(session) {
  const result = JSON.parse(session.report());
  assert.deepEqual(Object.keys(result).sort(), ["accepted", "phase", "worldList"]);
  const text = JSON.stringify(result).toLowerCase();
  for (const secret of ["password", "seed", "token", "offline_only", "fake_lobby_issued", "offline_œ", "fake_login", "fake_js5", "opaque-secret"]) {
    assert.equal(text.includes(secret), false);
  }
  return result;
}

function clean(session, phase) {
  assert.equal(session.phase(), phase);
  assert.deepEqual(report(session), { phase, accepted: false, worldList: null });
  assert.throws(() => session.outgoing());
  assert.throws(() => session.feed(Uint8Array.of(0)));
}

function feedChunks(session, wire, size) {
  const events = [];
  for (let start = 0; start < wire.length; start += size) {
    const chunk = wire.subarray(start, start + size);
    let at = 0;
    while (at < chunk.length) {
      const feed = JSON.parse(session.feed(chunk.subarray(at)));
      assert.ok(feed.consumed > 0 && feed.consumed <= chunk.length - at);
      at += feed.consumed;
      if (feed.event) events.push(feed.event);
    }
  }
  return events;
}

test("typed wasm lobby config preserves the independent native request bytes and exact handshake boundary", async () => {
  for (const [identity, name] of [
    [{ kind: "username", username: "offline_Œ_727" }, "lobby-username.wire"],
    [{ kind: "token", token: [0x88776655, 0x44332211] }, "lobby-token.wire"],
  ]) {
    const supplied = config();
    supplied.identity = identity;
    const session = new OriginalLobbySession(JSON.stringify(supplied));
    try {
      assert.deepEqual(report(session), { phase: "sendHandshake", accepted: false, worldList: null });
      assert.deepEqual(Array.from(session.outgoing()), [14]);
      assert.deepEqual(JSON.parse(session.feed(Uint8Array.of(0, 2, 99))), { consumed: 1, event: null });
      assert.equal(session.phase(), "sendRequest");
      const wire = session.outgoing();
      assert.ok(wire instanceof Uint8Array);
      assert.ok(Buffer.from(wire).equals(await fixture(name)));
      wire.fill(0);
      assert.equal(session.outgoing().length, 0);
      assert.equal(session.phase(), "awaitResponse");
      session.close();
      clean(session, "closed");
    } finally { session.free(); }
  }
});

test("typed wasm config rejects coercion, negative and overflowing words, array bounds and omissions without field values", () => {
  const reject = (supplied) => assert.throws(() => new OriginalLobbySession(typeof supplied === "string" ? supplied : JSON.stringify(supplied)), /lobby configuration|lobby request configuration/);
  for (const field of Object.keys(config())) {
    const supplied = config();
    delete supplied[field];
    reject(supplied);
  }
  for (const [field, length, max] of [["seed", 4, 4294967295], ["socialNonce", 2, 4294967295], ["clientRandomSeed", 2, 4294967295], ["random24", 24, 255], ["preferences", 35, 255], ["indexCrcs", 36, 4294967295]]) {
    for (const invalid of [-1, max + 1, 1.5, "1", true, null, {}, []]) {
      const supplied = config();
      supplied[field][0] = invalid;
      reject(supplied);
    }
    for (const count of [0, length - 1, length + 1]) {
      const supplied = config();
      supplied[field] = Array(count).fill(0);
      reject(supplied);
    }
  }
  reject(JSON.stringify(config()).replace('"seed":[1,', '"seed":[1.0,'));
  reject({ ...config(), unexpected: "secret-never-returned" });
  reject(JSON.stringify(config()).replace("{", '{"password":"secret-never-returned",'));
  reject(" ".repeat(65537));
  for (const value of ["secret-never-returned\u0000", "secret-never-returned😀"]) {
    const supplied = config();
    supplied.password = value;
    let error;
    try { new OriginalLobbySession(JSON.stringify(supplied)); } catch (caught) { error = String(caught); }
    assert.equal(error, "invalid lobby request configuration");
    assert.equal(error.includes("secret-never-returned"), false);
  }
  for (const identity of [{ kind: "username", username: "x", token: [0, 1] }, { kind: "token", token: [0, 1], username: "x" }, { kind: "token", token: [4294967295, 4294967295] }]) {
    reject({ ...config(), identity });
  }
});

test("wasm credential boundaries reject coercion and oversized data before copying incoming values", () => {
  let coerced = false;
  const coercible = { toString() { coerced = true; return JSON.stringify(config()); } };
  for (const value of [null, undefined, true, 1, 1n, {}, [], new String(JSON.stringify(config())), coercible]) {
    assert.throws(() => new OriginalLobbySession(value), /invalid lobby configuration/);
    const lobby = accepted();
    try { assert.throws(() => lobby.startWorld(value), /invalid world configuration/); assert.equal(lobby.phase(), "lobby"); }
    finally { lobby.close(); lobby.free(); }
  }
  assert.equal(coerced, false);
  assert.throws(() => new OriginalLobbySession("€".repeat(22000)), /lobby configuration byte limit/);
  const lobby = accepted();
  try { assert.throws(() => lobby.startWorld("€".repeat(22000)), /world configuration byte limit/); }
  finally { lobby.close(); lobby.free(); }
  for (const value of [null, undefined, 1, [], [0], new ArrayBuffer(1), new DataView(new ArrayBuffer(1)), new Uint16Array(1), new Uint8Array(65537)]) {
    const lobby = accepted();
    try { assert.throws(() => lobby.feed(value), /invalid lobby incoming type|lobby incoming chunk byte limit/); clean(lobby, "failed"); }
    finally { lobby.free(); }
    const world = worldAwaiting();
    try { assert.throws(() => world.feed(value), /invalid world incoming type|world incoming chunk byte limit/); worldClean(world, "failed"); }
    finally { world.free(); }
  }
});

test("wasm lobby chains original response, ISAAC stream, split world list and opaque packets without applying them", async () => {
  for (const confirmation of [false, true]) {
    for (const size of [1, 2, 3, 7, 31, 63, 127, 65536]) {
      const session = awaiting();
      try {
        let outWord = 0;
        if (confirmation) {
          assert.deepEqual(JSON.parse(session.feed(Uint8Array.of(1, 99))), { consumed: 1, event: { kind: "confirmationRequired" } });
          assert.equal(session.phase(), "awaitConfirmation");
          session.confirm();
          assert.equal(session.phase(), "sendConfirmation");
          assert.equal(session.outgoing()[0], (26 + outgoing.readUInt32BE(4 * outWord++)) & 255);
        }
        const encode = server();
        const packets = [];
        for (const name of ["world-list-split3-0.payload", "world-list-split3-1.payload", "world-list-split3-2.payload"]) {
          packets.push(encode(103, await fixture(name)), encode(29));
        }
        packets.push(encode(103, counts), encode(128, Buffer.from([0xfe, 0xa5, 0, 1, 2, 3])), encode(2, Buffer.from("opaque-secret-not-applied")));
        const wire = Buffer.concat([Buffer.from([42, 0x12, 0x34]), success, ...packets]);
        const events = feedChunks(session, wire, size);
        assert.equal(events.filter((event) => event.kind === "timeSync").length, 1);
        assert.equal(events.filter((event) => event.kind === "loginAccepted").length, 1);
        assert.equal(events.filter((event) => event.kind === "worldListPending").length, 2);
        assert.equal(events.filter((event) => event.kind === "worldListUpdated").length, 2);
        assert.equal(events.filter((event) => event.kind === "opaquePacket").length, 5);
        assert.deepEqual(events.at(-1), { kind: "opaquePacket", opcode: 2, name: "PROCESS_DEV_CONSOLE_COMMAND", payloadBytes: 25 });
        const result = report(session);
        assert.equal(result.accepted, true);
        assert.equal(result.phase, "lobby");
        assert.equal(result.worldList.version, 0x89abcdef | 0);
        assert.equal(result.worldList.start, 0);
        assert.equal(result.worldList.worlds.length, 2);
        assert.equal(result.worldList.worlds[0].host, "first.invalid");
        assert.equal(result.worldList.worlds[1].host, "second.invalid");
        assert.deepEqual(result.worldList.worlds.map((world) => world.playerCount), [65534, 42]);
        session.requestWorldList(-2147483648);
        const request = session.outgoing();
        assert.equal(request[0], (47 + outgoing.readUInt32BE(4 * outWord++)) & 255);
        assert.deepEqual(Array.from(request.slice(1)), [128, 0, 0, 0]);
        session.markFlushed();
        for (let tick = 0; tick < 50; tick++) {
          session.tick();
          assert.equal(session.outgoing().length, 0);
        }
        session.tick();
        assert.equal(session.outgoing()[0], outgoing.readUInt32BE(4 * outWord++) & 255);
        session.tick();
        assert.equal(session.outgoing().length, 0);
        session.markFlushed();
        session.finish();
        session.finish();
        clean(session, "closed");
      } finally { session.free(); }
    }
  }
});

test("wasm success consumes exactly its frame and retains only Rust token before the next coalesced packet", () => {
  const packet = server()(103, basic);
  for (const cut of [0, 1, 2, 3, success.length - 1, success.length]) {
    const session = awaiting();
    try {
      assert.equal(JSON.parse(session.feed(success.subarray(0, cut))).consumed, cut);
      let tail = Buffer.concat([success.subarray(cut), packet]);
      if (cut < success.length) {
        const feed = JSON.parse(session.feed(tail));
        assert.deepEqual(feed, { consumed: success.length - cut, event: { kind: "loginAccepted" } });
        tail = tail.subarray(feed.consumed);
      }
      assert.deepEqual(JSON.parse(session.feed(tail)), { consumed: packet.length, event: { kind: "worldListUpdated" } });
      assert.equal(report(session).accepted, true);
      assert.equal("worldToken" in session, false);
      assert.equal("token" in session, false);
    } finally { session.free(); }
  }
});

test("wasm queued rejected retry and reconnect responses are explicit terminal phases with no accepted state", () => {
  for (const [wire, phase, kind] of [[[0], "rejected", "rejected"], [[3], "rejected", "rejected"], [[29, 9], "rejected", "rejected"], [[45, 1, 2, 3], "rejected", "rejected"], [[255], "rejected", "rejected"], [[21, 9], "queued", "queued"], [[23], "retry", "retryRequested"], [[15], "reconnect", "reconnectRequired"]]) {
    const session = awaiting();
    try {
      const event = { kind, ...(kind === "rejected" ? { responseCode: wire[0] } : {}) };
      assert.deepEqual(JSON.parse(session.feed(Uint8Array.from([...wire, 99]))), { consumed: wire.length, event });
      assert.deepEqual(report(session), { phase, accepted: false, worldList: null });
      assert.equal(session.outgoing().length, 0);
      session.finish();
      clean(session, "closed");
    } finally { session.free(); }
  }
});

test("wasm invalid phases, transport truncation, malformed world lists and oversized input fail closed", () => {
  const actions = [
    (session) => session.confirm(),
    (session) => session.feed(new Uint8Array(65537)),
    (session) => session.feed(server()(103, Uint8Array.of(1, 9))),
    (session) => { session.feed(server()(103, basic).subarray(0, 2)); session.finish(); },
    (session) => { session.requestWorldList(0); session.requestWorldList(0); },
    (session) => { session.requestWorldList(0); session.finish(); },
  ];
  for (const action of actions) {
    const session = accepted();
    try { assert.throws(() => action(session)); clean(session, "failed"); }
    finally { session.free(); }
  }
  for (const cut of [0, 1, 2, success.length - 1]) {
    const session = awaiting();
    try {
      session.feed(success.subarray(0, cut));
      assert.throws(() => session.finish(), /during login/);
      clean(session, "failed");
    } finally { session.free(); }
  }
  for (const value of [4294967296, -2147483649, 2147483648, NaN, Infinity, -Infinity, 1.5, "1", null, undefined, true, 1n, {}, []]) {
    const session = accepted();
    try {
      assert.throws(() => session.requestWorldList(value), /invalid world list version/);
      clean(session, "failed");
    } finally { session.free(); }
  }
});

test("wasm lobby lifetime uses a bounded monotonic clock and fails safely when it is missing or throws", () => {
  const original = Object.getOwnPropertyDescriptor(globalThis, "performance");
  const restore = () => Object.defineProperty(globalThis, "performance", original);
  let time = 1;
  try {
    Object.defineProperty(globalThis, "performance", { configurable: true, value: { now: () => time } });
    const session = accepted();
    try {
      time += 300000;
      assert.throws(() => session.outgoing(), /lifetime limit/);
      clean(session, "failed");
    } finally { session.free(); }
    Object.defineProperty(globalThis, "performance", { configurable: true, value: undefined });
    assert.throws(() => new OriginalLobbySession(JSON.stringify(config())), /monotonic clock unavailable/);
    Object.defineProperty(globalThis, "performance", { configurable: true, value: { now() { throw new Error("clock-secret-never-returned"); } } });
    assert.throws(() => new OriginalLobbySession(JSON.stringify(config())), (error) => String(error) === "lobby monotonic clock unavailable");
  } finally { restore(); }
});

function worldConfig() {
  const supplied = config();
  delete supplied.password;
  delete supplied.gameId;
  delete supplied.language;
  return {
    ...supplied,
    reconnecting: false,
    displayMode: 2,
    screenWidth: 1280,
    screenHeight: 720,
    antiAliasing: 4,
    machineInfo: {
      operatingSystem: 3, x64Os: true, osVendor: 9, javaVendor: 4,
      javaBuild: 17, javaSubBuild: 2, javaUpdate: 3, unknownFlag: false,
      maxMemory: 4096, processors: 8, ram: 0xabcdef, cpuClock: 4200,
      gpuModel: "FAKE_GPU_€", gpuDriverVersion: "FAKE_DRIVER_™",
      directxVersion: "12.1", directxLetter: "A", dxDriverMonth: 10,
      dxDriverYear: 2026, cpuType: "FAKE_CPU", cpuData: "OFFLINE_CPU_Œ",
      cpuCores: 16, cpuFamilyId: 254,
      cpuFeatureFlags: [0x89abcdef, 0x12345678, 0x80000000], cpuModelInfo: 0xfedcba98,
    },
    varVerifyId: 0xfedcba98 | 0,
    machineUid: [0x88776655, 0x44332211],
    loginToken: "FAKE_CLIENT_LOGIN_€",
    supplementaryLoginData: "FAKE_EXTRA_ÿ",
    jagtheoraLoaded: true,
    isApplet: false,
    hasSignedTermsAgreement: true,
    loginType: 7,
    js5AuthToken: "FAKE_WORLD_JS5_™",
    differentWorld: true,
    worldId: 42,
  };
}

function worldAwaiting(varpCount) {
  const lobby = accepted();
  try {
    const json = JSON.stringify(worldConfig());
    const world = varpCount === undefined ? lobby.startWorld(json) : lobby.startWorldState(json, varpCount);
    assert.equal(lobby.phase(), "lobby");
    assert.equal(world.phase(), "sendHandshake");
    assert.deepEqual(Array.from(world.outgoing()), [14]);
    assert.deepEqual(JSON.parse(world.feed(Uint8Array.of(0))), { consumed: 1, event: null });
    assert.equal(world.phase(), "sendRequest");
    assert.ok(world.outgoing().length > 0);
    assert.equal(world.phase(), "awaitResponse");
    return world;
  } finally { lobby.close(); lobby.free(); }
}

async function worldResponse(index) {
  const reply = Buffer.from(await fixture("world-success.wire"));
  reply.writeUInt16BE(index, 8);
  return reply;
}

function worldReport(world) {
  const value = JSON.parse(world.report());
  assert.deepEqual(Object.keys(value).sort(), ["accepted", "initialWorld", "phase", "worldReady"]);
  const text = JSON.stringify(value).toLowerCase();
  for (const secret of ["token", "password", "seed", "owner", "username", "xtea", "key", "fake_", "offline_"]) assert.equal(text.includes(secret), false);
  if (value.initialWorld) assert.deepEqual(Object.keys(value.initialWorld).sort(), ["base", "chunkCount", "mapKind", "mapSize", "playerIndex", "regionIds", "tile"]);
  return value;
}

function worldClean(world, phase) {
  assert.deepEqual(worldReport(world), { phase, accepted: false, worldReady: false, initialWorld: null });
  assert.equal(world.phase(), phase);
  assert.throws(() => world.outgoing());
  assert.throws(() => world.feed(Uint8Array.of(0)));
}

async function worldOracle(name) {
  const values = await fixture(`world-state-${name}.native-values`);
  let at = 0;
  const read = () => { const value = values.readInt32BE(at); at += 4; return value; };
  const playerIndex = read(), plane = read(), x = read(), y = read();
  read(); read();
  const otherPlayers = read();
  at += otherPlayers * 20;
  read(); read();
  const mapSize = read();
  read(); read(); read(); read();
  const base = { x: read(), y: read() };
  read();
  const descriptorCount = read();
  let chunkCount = 0;
  for (let i = 0; i < descriptorCount; i++) if (read() !== -1) chunkCount++;
  const regionIds = [];
  const regions = read();
  for (let i = 0; i < regions; i++) { regionIds.push(read()); at += 16; }
  return { playerIndex, tile: { plane, x, y }, base, mapSize, mapKind: descriptorCount === 0 ? "standard" : "dynamic", regionIds, chunkCount };
}

test("real wasm world request uses actual lobby-issued token internally and matches the native oracle", async () => {
  const lobby = accepted(43);
  let world;
  try {
    world = lobby.startWorld(JSON.stringify(worldConfig()));
    assert.equal(lobby.phase(), "lobby");
    assert.deepEqual(Array.from(world.outgoing()), [14]);
    assert.deepEqual(JSON.parse(world.feed(Uint8Array.of(0, 99))), { consumed: 1, event: null });
    const request = world.outgoing();
    assert.ok(Buffer.from(request).equals(await fixture("world-normal.wire")));
    request.fill(0);
    assert.equal(world.phase(), "awaitResponse");
    assert.deepEqual(worldReport(world), { phase: "awaitResponse", accepted: false, worldReady: false, initialWorld: null });
    assert.equal("worldToken" in world, false);
    assert.equal("token" in world, false);
    world.close();
    worldClean(world, "closed");
    assert.equal(lobby.phase(), "lobby");
  } finally { world?.free(); lobby.close(); lobby.free(); }
});

function decryptWorldRequest(request) {
  const encryptedAt = 14 + request.readUInt16BE(12);
  const plain = Buffer.from(request.subarray(encryptedAt));
  const seed = config().seed;
  const mix = (value) => (((value << 4) ^ (value >>> 5)) + value) >>> 0;
  for (let at = 0; at + 8 <= plain.length; at += 8) {
    let left = plain.readUInt32BE(at), right = plain.readUInt32BE(at + 4);
    let sum = 0xc6ef3720;
    for (let round = 0; round < 32; round++) {
      right = (right - (mix(left) ^ ((sum + seed[(sum >>> 11) & 3]) >>> 0))) >>> 0;
      sum = (sum - 0x9e3779b9) >>> 0;
      left = (left - (mix(right) ^ ((sum + seed[sum & 3]) >>> 0))) >>> 0;
    }
    plain.writeUInt32BE(left, at);
    plain.writeUInt32BE(right, at + 4);
  }
  return { encryptedAt, plain };
}

test("real wasm derives the native different-world flag from the private lobby default, not the caller", async () => {
  const nativeWire = await fixture("world-normal.wire");
  const nativePlain = await fixture("world-normal.xtea");
  const flagAt = nativePlain.length - 36 * 4 - 5;
  assert.equal(nativePlain[flagAt], 1);
  assert.equal(nativePlain.readInt32BE(flagAt + 1), 42);
  assert.deepEqual(decryptWorldRequest(nativeWire).plain, nativePlain);
  for (const [defaultWorld, selectedWorld, expected] of [[42, 42, false], [43, 42, true], [null, 42, true], [null, -1, false]]) {
    const lobby = accepted(defaultWorld);
    try {
      for (const callerFlag of [false, true]) {
        const world = lobby.startWorld(JSON.stringify({ ...worldConfig(), differentWorld: callerFlag, worldId: selectedWorld }));
        try {
          assert.deepEqual(Array.from(world.outgoing()), [14]);
          world.feed(Uint8Array.of(0));
          const request = Buffer.from(world.outgoing());
          const { encryptedAt, plain } = decryptWorldRequest(request);
          assert.deepEqual(request.subarray(0, encryptedAt), nativeWire.subarray(0, encryptedAt));
          const expectedPlain = Buffer.from(nativePlain);
          expectedPlain[flagAt] = Number(expected);
          expectedPlain.writeInt32BE(selectedWorld, flagAt + 1);
          assert.deepEqual(plain, expectedPlain);
          assert.deepEqual(report(lobby), { phase: "lobby", accepted: true, worldList: null });
        } finally { world.close(); world.free(); }
      }
      lobby.close();
      assert.throws(() => lobby.startWorld(JSON.stringify(worldConfig())), /lobby session is closed/);
    } finally { lobby.close(); lobby.free(); }
  }
});

test("real wasm world config is typed, complete, credential-free and explicitly rejects reconnect", () => {
  const lobby = accepted();
  try {
    for (const field of Object.keys(worldConfig())) {
      const supplied = worldConfig();
      delete supplied[field];
      assert.throws(() => lobby.startWorld(JSON.stringify(supplied)), /invalid world configuration/);
    }
    for (const field of Object.keys(worldConfig().machineInfo)) {
      const supplied = worldConfig();
      delete supplied.machineInfo[field];
      assert.throws(() => lobby.startWorld(JSON.stringify(supplied)), /invalid world configuration/);
    }
    for (const field of ["password", "lobbyAuthToken", "worldToken", "worldLoginToken"]) assert.throws(() => lobby.startWorld(JSON.stringify({ ...worldConfig(), [field]: "secret-never-returned" })), /invalid world configuration/);
    assert.throws(() => lobby.startWorld(JSON.stringify({ ...worldConfig(), reconnecting: true })), /world reconnect is unsupported/);
    assert.throws(() => lobby.startWorld(" ".repeat(65537)), /world configuration byte limit/);
    for (const invalid of [-1, 4294967296, "1", 1.5, null]) {
      const supplied = worldConfig();
      supplied.machineInfo.cpuFeatureFlags[0] = invalid;
      assert.throws(() => lobby.startWorld(JSON.stringify(supplied)), /invalid world configuration/);
    }
    assert.equal(lobby.phase(), "lobby");
  } finally { lobby.close(); lobby.free(); }
  const unaccepted = new OriginalLobbySession(JSON.stringify(config()));
  try { assert.throws(() => unaccepted.startWorld(JSON.stringify(worldConfig())), /requires an accepted lobby/); }
  finally { unaccepted.free(); }
});

test("real wasm world entry decodes native own player and standard or dynamic map without leaking keys", async () => {
  for (const [name, opcode] of [["standard-104", 85], ["dynamic-size-0", 51], ["dynamic-empty", 51]]) {
    const payload = await fixture(`world-state-${name}.payload`);
    const oracle = await worldOracle(name);
    assert.equal(oracle.mapKind, opcode === 85 ? "standard" : "dynamic");
    if (name === "dynamic-empty") assert.equal(oracle.chunkCount, 0);
    for (const size of [1, 2, 3, 7, 511, 1024, 65536]) {
      const world = worldAwaiting();
      try {
        const encode = server();
        const first = encode(opcode, payload);
        const wire = Buffer.concat([await worldResponse(oracle.playerIndex), first, encode(29), encode(128, Buffer.from([0xfe, 0xa5, 0, 1, 2, 3]))]);
        const events = feedChunks(world, wire, size);
        assert.deepEqual(events.slice(0, 2), [{ kind: "loginAccepted" }, { kind: "worldReady" }]);
        assert.deepEqual(events[2], { kind: "opaquePacket", opcode: 29, name: "PING", payloadBytes: 0 });
        assert.deepEqual(events[3], { kind: "opaquePacket", opcode: 128, name: "MIDI_SONG_LOCATION", payloadBytes: 6 });
        assert.deepEqual(worldReport(world), { phase: "worldReady", accepted: true, worldReady: true, initialWorld: oracle });
        assert.equal(world.outgoing().length, 0);
        world.finish();
        worldClean(world, "closed");
      } finally { world.free(); }
    }
  }
});

test("real wasm world login and initial state each leave coalesced suffix bytes unconsumed", async () => {
  const world = worldAwaiting();
  try {
    const encode = server();
    const reply = await worldResponse(1);
    const first = encode(85, await fixture("world-state-standard-104.payload"));
    const ping = encode(29);
    const wire = Buffer.concat([reply, first, ping]);
    assert.deepEqual(JSON.parse(world.feed(wire)), { consumed: reply.length, event: { kind: "loginAccepted" } });
    assert.equal(world.phase(), "awaitInitialWorld");
    assert.equal(worldReport(world).worldReady, false);
    assert.deepEqual(JSON.parse(world.feed(wire.subarray(reply.length))), { consumed: first.length, event: { kind: "worldReady" } });
    assert.deepEqual(JSON.parse(world.feed(ping)), { consumed: ping.length, event: { kind: "opaquePacket", opcode: 29, name: "PING", payloadBytes: 0 } });
  } finally { world.close(); world.free(); }
});

test("real wasm state mode requires an explicit numeric definition span before coercion", () => {
  const lobby = accepted();
  let coerced = false;
  const coercible = { valueOf() { coerced = true; return 2; }, toString() { coerced = true; return "2"; } };
  try {
    for (const count of [-1, 65537, 4294967296, 1.5, NaN, Infinity, -Infinity, "2", true, null, undefined, 2n, {}, [], new Number(2), coercible]) {
      assert.throws(() => lobby.startWorldState(JSON.stringify(worldConfig()), count), /invalid world variable count/);
      assert.equal(lobby.phase(), "lobby");
    }
    assert.equal(coerced, false);
    for (const count of [0, 2, 65536]) {
      const world = lobby.startWorldState(JSON.stringify(worldConfig()), count);
      try {
        assert.deepEqual(JSON.parse(world.report()).variables, { count, scheduled: 0, serverAssignments: 0, published: 0, clears: 0, nonZero: 0 });
        if (count) assert.equal(world.varp(count - 1), 0);
        assert.throws(() => world.varp(count), /variable id/);
        for (const id of [-1, 65536, 4294967296, 0.5, NaN, Infinity, "0", true, null, undefined, 0n, {}, [], new Number(0), coercible]) {
          assert.throws(() => world.varp(id), /invalid world variable id/);
        }
        assert.equal(world.phase(), "sendHandshake");
        world.close();
        worldClean(world, "closed");
        assert.throws(() => world.varp(0), /closed/);
      } finally { world.free(); }
    }
    assert.equal(coerced, false);
    const legacy = lobby.startWorld(JSON.stringify(worldConfig()));
    try {
      assert.equal("variables" in JSON.parse(legacy.report()), false);
      assert.throws(() => legacy.varp(0), /state unavailable/);
      assert.deepEqual(JSON.parse(legacy.tick()), { events: [] });
    } finally { legacy.close(); legacy.free(); }
  } finally { lobby.close(); lobby.free(); }
});

function smallVarp(id, value) {
  return Buffer.from([value & 255, (id + 128) & 255, id >> 8]);
}

function largeVarp(id, value) {
  const bytes = Buffer.alloc(4);
  bytes.writeInt32BE(value);
  return Buffer.from([bytes[1], bytes[0], bytes[3], bytes[2], (id + 128) & 255, id >> 8]);
}

async function stateReady(count, confirmation = false) {
  const world = worldAwaiting(count);
  const encode = server();
  try {
    if (confirmation) {
      assert.deepEqual(JSON.parse(world.feed(Uint8Array.of(1))).event, { kind: "confirmationRequired" });
      world.confirm();
      const bytes = world.outgoing();
      assert.deepEqual([...bytes], [(26 + outgoing.readUInt32BE(0)) & 255]);
      bytes.fill(0);
    }
    assert.deepEqual(JSON.parse(world.feed(await worldResponse(1))).event, { kind: "loginAccepted" });
    assert.deepEqual(JSON.parse(world.feed(encode(85, await fixture("world-state-standard-104.payload")))).event, { kind: "worldReady" });
    return { world, encode };
  } catch (error) { world.close(); world.free(); throw error; }
}

function gameTicks(world, count = 51) {
  for (let index = 0; index < count; index++) world.gameTick();
}

test("real wasm world keepalive consumes each native ISAAC word in order after optional confirmation", async () => {
  for (const confirmed of [false, true]) {
    const { world } = await stateReady(undefined, confirmed);
    try {
      assert.equal(typeof world.gameTick, "function");
      assert.equal(typeof world.markFlushed, "function");
      for (let word = Number(confirmed); word < outgoing.length / 4; word++) {
        gameTicks(world, 50);
        assert.equal(world.outgoing().length, 0);
        assert.equal(world.gameTick(), undefined);
        const bytes = world.outgoing();
        assert.ok(bytes instanceof Uint8Array);
        assert.deepEqual([...bytes], [outgoing.readUInt32BE(word * 4) & 255]);
        bytes.fill(0);
        assert.equal(world.outgoing().length, 0);
        assert.equal(world.markFlushed(), undefined);
      }
      world.finish();
      worldClean(world, "closed");
      assert.throws(() => world.gameTick(), /closed/);
      assert.throws(() => world.markFlushed(), /closed/);
    } finally { world.close(); world.free(); }
  }
});

test("real wasm variable tick and feed never infer game ticks and pending keepalives coalesce through drain until flush", async () => {
  const original = Object.getOwnPropertyDescriptor(globalThis, "performance");
  let now = 1000;
  try {
    Object.defineProperty(globalThis, "performance", { configurable: true, value: { now: () => now } });
    const { world, encode } = await stateReady(2);
    try {
      for (let index = 0; index < 100; index++) assert.deepEqual(JSON.parse(world.tick()), { events: [] });
      world.feed(encode(115, smallVarp(1, -7)));
      now = 11000;
      gameTicks(world, 50);
      assert.equal(world.varp(1), 0);
      assert.equal(world.outgoing().length, 0);
      assert.deepEqual(JSON.parse(world.tick()), { events: [{ kind: "variablePublished", id: 1, previous: 0, value: -7 }] });
      for (let index = 0; index < 100; index++) world.feed(encode(29));
      assert.equal(world.outgoing().length, 0);
      world.gameTick();
      gameTicks(world, 1000);
      const first = world.outgoing();
      assert.deepEqual([...first], [outgoing.readUInt32BE(0) & 255]);
      first.fill(0);
      for (let index = 0; index < 1000; index++) {
        world.gameTick();
        assert.equal(world.outgoing().length, 0);
      }
      world.feed(encode(115, smallVarp(1, 9)));
      now += 2;
      assert.equal(world.varp(1), -7);
      assert.deepEqual(JSON.parse(world.tick()), { events: [{ kind: "variablePublished", id: 1, previous: -7, value: 9 }] });
      assert.equal(world.outgoing().length, 0);
      world.markFlushed();
      gameTicks(world, 50);
      assert.equal(world.outgoing().length, 0);
      world.gameTick();
      const second = world.outgoing();
      assert.deepEqual([...second], [outgoing.readUInt32BE(4) & 255]);
      second.fill(0);
      world.markFlushed();
      assert.equal(world.varp(1), 9);
      world.finish();
      worldClean(world, "closed");
    } finally { world.close(); world.free(); }
  } finally { Object.defineProperty(globalThis, "performance", original); }
});

test("real wasm keepalive invalid stages empty and premature flush acknowledgements and pending EOF fail terminally", async () => {
  for (const method of ["gameTick", "markFlushed"]) {
    for (const acceptedWorld of [false, true]) {
      const world = worldAwaiting(2);
      try {
        if (acceptedWorld) world.feed(await worldResponse(1));
        assert.throws(() => world[method](), /invalid world keepalive phase|invalid world flush phase/);
        worldClean(world, "failed");
        assert.throws(() => world.gameTick(), /failed/);
        assert.throws(() => world.markFlushed(), /failed/);
      } finally { world.close(); world.free(); }
    }
  }
  for (const [action, expected] of [
    [(world) => world.markFlushed(), /world flush requires outgoing bytes/],
    [(world) => { gameTicks(world, 50); world.outgoing(); world.markFlushed(); }, /world flush requires outgoing bytes/],
    [(world) => { gameTicks(world); world.markFlushed(); }, /world outgoing buffer must be drained/],
    [(world) => { gameTicks(world); world.finish(); }, /world connection ended with unflushed bytes/],
    [(world) => { gameTicks(world); world.outgoing(); world.finish(); }, /world connection ended with unflushed bytes/],
    [(world) => { gameTicks(world); world.outgoing(); world.markFlushed(); world.markFlushed(); }, /world flush requires outgoing bytes/],
    [(world, encode) => { gameTicks(world); world.feed(encode(115, smallVarp(2, 7))); }, /invalid world variable packet/],
    [(world, encode) => { gameTicks(world); world.outgoing(); world.feed(encode(115, smallVarp(2, 7))); }, /invalid world variable packet/],
  ]) {
    const { world, encode } = await stateReady(2);
    try {
      assert.throws(() => action(world, encode), expected);
      worldClean(world, "failed");
      assert.throws(() => world.varp(0), /failed/);
      assert.throws(() => world.gameTick(), /failed/);
      assert.throws(() => world.markFlushed(), /failed/);
    } finally { world.close(); world.free(); }
  }
  for (const drained of [false, true]) {
    const { world } = await stateReady(2);
    try {
      gameTicks(world);
      if (drained) world.outgoing().fill(0);
      world.close();
      worldClean(world, "closed");
      assert.throws(() => world.gameTick(), /closed/);
      assert.throws(() => world.markFlushed(), /closed/);
      world.finish();
    } finally { world.free(); }
  }
});

test("real wasm game ticks and flush acknowledgements retain lifetime and operation budgets", async () => {
  const original = Object.getOwnPropertyDescriptor(globalThis, "performance");
  let now = 1000;
  try {
    Object.defineProperty(globalThis, "performance", { configurable: true, value: { now: () => now } });
    for (const method of ["gameTick", "markFlushed"]) {
      for (const drained of [false, true]) {
        now = 1000;
        const { world } = await stateReady(2);
        try {
          gameTicks(world);
          if (drained) world.outgoing().fill(0);
          now = 301000;
          assert.throws(() => world[method](), /world session lifetime limit/);
          worldClean(world, "failed");
        } finally { world.close(); world.free(); }
      }
    }
    now = 1000;
    const { world } = await stateReady(2);
    try {
      assert.throws(() => gameTicks(world, 65536), /world session operation limit/);
      worldClean(world, "failed");
    } finally { world.close(); world.free(); }
  } finally { Object.defineProperty(globalThis, "performance", original); }
});

test("real wasm pending varps publish on idle ticks with strict monotonic deadlines and clear both banks", async () => {
  const original = Object.getOwnPropertyDescriptor(globalThis, "performance");
  let now = 1000;
  try {
    Object.defineProperty(globalThis, "performance", { configurable: true, value: { now: () => now } });
    const { world, encode } = await stateReady(3);
    try {
      const first = encode(115, smallVarp(1, -128));
      const second = encode(8, largeVarp(1, -2147483648));
      const third = encode(8, largeVarp(2, 2147483647));
      const rest = Buffer.concat([first, second, third]);
      assert.deepEqual(JSON.parse(world.feed(rest)), { consumed: first.length, event: { kind: "variableReceived", opcode: 115, name: "VARP_SMALL", id: 1, value: -128 } });
      assert.deepEqual(JSON.parse(world.feed(rest.subarray(first.length))), { consumed: second.length, event: { kind: "variableReceived", opcode: 8, name: "VARP_LARGE", id: 1, value: -2147483648 } });
      assert.deepEqual(JSON.parse(world.feed(third)).event, { kind: "variableReceived", opcode: 8, name: "VARP_LARGE", id: 2, value: 2147483647 });
      assert.equal(world.varp(1), 0);
      assert.deepEqual(JSON.parse(world.tick()), { events: [] });
      now = 1001.99;
      assert.deepEqual(JSON.parse(world.tick()), { events: [] });
      now = 1002;
      assert.deepEqual(JSON.parse(world.tick()), { events: [
        { kind: "variablePublished", id: 1, previous: 0, value: -2147483648 },
        { kind: "variablePublished", id: 2, previous: 0, value: 2147483647 },
      ] });
      assert.equal(world.varp(1), -2147483648);
      assert.equal(world.varp(2), 2147483647);
      world.feed(encode(115, smallVarp(1, 5)));
      assert.deepEqual(JSON.parse(world.feed(encode(100))).event, { kind: "variablesCleared", opcode: 100, name: "CLEAR_VARPS" });
      now = 1100;
      assert.deepEqual(JSON.parse(world.tick()), { events: [] });
      assert.equal(world.varp(1), 0);
      assert.equal(world.varp(2), 0);
      const result = JSON.parse(world.report());
      assert.deepEqual(result.variables, { count: 3, scheduled: 0, serverAssignments: 4, published: 2, clears: 1, nonZero: 0 });
      assert.deepEqual(Object.keys(result.variables).sort(), ["clears", "count", "nonZero", "published", "scheduled", "serverAssignments"]);
      assert.equal(world.outgoing().length, 0);
      world.close();
      worldClean(world, "closed");
    } finally { world.free(); }
  } finally { Object.defineProperty(globalThis, "performance", original); }
});

test("real wasm state malformed packets actual-span ids rejection and clock faults drop bank", async () => {
  for (const [opcode, payload] of [[115, smallVarp(2, 1)], [8, largeVarp(2, 1)]]) {
    const { world, encode } = await stateReady(2);
    try {
      assert.throws(() => world.feed(encode(opcode, payload)), /invalid world variable packet/);
      worldClean(world, "failed");
      assert.throws(() => world.varp(0), /failed/);
    } finally { world.free(); }
  }
  for (const [opcode, payload] of [[115, smallVarp(1, 1)], [8, largeVarp(1, 1)]]) {
    const { world, encode } = await stateReady(2);
    try {
      const wire = encode(opcode, payload);
      assert.equal(JSON.parse(world.feed(wire.subarray(0, -1))).event, null);
      assert.throws(() => world.finish(), /during packet/);
      worldClean(world, "failed");
    } finally { world.free(); }
  }
  const rejected = worldAwaiting(2);
  try {
    assert.deepEqual(JSON.parse(rejected.feed(Uint8Array.of(3, 99))), { consumed: 1, event: { kind: "rejected", responseCode: 3 } });
    assert.equal("variables" in JSON.parse(rejected.report()), false);
    assert.throws(() => rejected.varp(0), /state unavailable/);
  } finally { rejected.close(); rejected.free(); }
  const early = worldAwaiting(2);
  try {
    const reply = await worldResponse(1);
    const encode = server();
    const fixed = encode(115, smallVarp(1, 1));
    const first = Buffer.concat([fixed.subarray(0, 1), Buffer.from([0, 3]), fixed.subarray(1)]);
    const wire = Buffer.concat([reply, first]);
    assert.equal(JSON.parse(early.feed(wire)).consumed, reply.length);
    assert.equal(JSON.parse(early.report()).worldReady, false);
    assert.throws(() => early.feed(first), /initial world/);
    worldClean(early, "failed");
  } finally { early.free(); }
  const original = Object.getOwnPropertyDescriptor(globalThis, "performance");
  let now = 1000;
  try {
    Object.defineProperty(globalThis, "performance", { configurable: true, value: { now: () => now } });
    for (const invalid of [1009, NaN, Infinity, 301000]) {
      now = 1000;
      const { world } = await stateReady(2);
      try {
        now = 1010;
        world.tick();
        now = invalid;
        assert.throws(() => world.tick(), /variable service|monotonic clock|lifetime limit/);
        worldClean(world, "failed");
      } finally { world.free(); }
    }
  } finally { Object.defineProperty(globalThis, "performance", original); }
});

test("real wasm world failures never claim readiness and preserve confirmation and terminal boundaries", async () => {
  for (const [opcode, payload] of [[29, Buffer.alloc(0)], [85, Buffer.alloc(7)], [85, (await fixture("world-state-standard-104.payload")).subarray(0, -1)]]) {
    const world = worldAwaiting();
    try {
      world.feed(await worldResponse(1));
      const encoded = server()(opcode, payload);
      const wire = opcode === 29 ? Buffer.concat([encoded, Buffer.alloc(2)]) : encoded;
      assert.throws(() => world.feed(wire), /initial world state/);
      worldClean(world, "failed");
    } finally { world.free(); }
  }
  for (const index of [0, 2048, 65535]) {
    const world = worldAwaiting();
    const reply = await worldResponse(index);
    try { assert.throws(() => world.feed(reply), /player index/); worldClean(world, "failed"); }
    finally { world.free(); }
  }
  const world = worldAwaiting();
  try {
    assert.deepEqual(JSON.parse(world.feed(Uint8Array.of(1, 99))), { consumed: 1, event: { kind: "confirmationRequired" } });
    world.confirm();
    assert.equal(world.outgoing()[0], (26 + outgoing.readUInt32BE(0)) & 255);
    world.feed(await worldResponse(1));
    const initial = server()(85, await fixture("world-state-standard-104.payload"));
    world.feed(initial.subarray(0, -1));
    assert.throws(() => world.finish(), /during initial packet/);
    worldClean(world, "failed");
  } finally { world.free(); }
  for (const [wire, phase] of [[[0], "rejected"], [[3], "rejected"], [[29, 9], "rejected"], [[45, 1, 2, 3], "rejected"], [[255], "rejected"], [[21, 2], "queued"], [[23], "retry"], [[15], "reconnect"]]) {
    const world = worldAwaiting();
    try {
      const feed = JSON.parse(world.feed(Uint8Array.from([...wire, 99])));
      assert.equal(feed.consumed, wire.length);
      if (phase === "rejected") assert.deepEqual(feed.event, { kind: "rejected", responseCode: wire[0] });
      assert.deepEqual(worldReport(world), { phase, accepted: false, worldReady: false, initialWorld: null });
      world.finish();
      worldClean(world, "closed");
    } finally { world.free(); }
  }
});
