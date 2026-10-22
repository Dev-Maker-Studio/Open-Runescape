import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { webcrypto } from "node:crypto";
import {
  buildLoginConfig, deriveBrowserMachineInfo, defaultLoginPreferences,
  browserLoginProvenance, fetchLoginCrcs, PUBLIC_LOGIN_DEFAULTS,
} from "../static/login-config.js";

const account = () => ({ identity: { kind: "username", username: "FAKE_BROWSER_ONLY" }, password: "FAKE_PASSWORD_ONLY" });
const capabilities = () => ({ platform: "Linux x86_64", architecture: "x86", bitness: "64", hardwareConcurrency: 2, jsHeapSizeLimit: 244 * 1048576 });
const masterReport = () => ({
  revision: 727, subrevision: 1, prefetchCount: 27, authenticated: false, signatureBytes: 128,
  indices: Array.from({ length: 37 }, (_, index) => ({ index, crc: index % 2 ? -index - 1 : index + 100, revision: index + 1 })),
});

function fixture() {
  const state = { probes: 0, randomCalls: 0, randomBuffers: [] };
  const machineInfo = deriveBrowserMachineInfo(capabilities());
  const options = {
    viewport: { width: 1024, height: 768 },
    machineInfo,
    preferences: defaultLoginPreferences({ maxMemory: machineInfo.maxMemory, processors: machineInfo.processors, isArm: false }),
    fontIndex: 32,
    worldId: 22,
    probe: async (relay, publicToken, { signal }) => {
      state.probes++;
      state.relay = relay;
      state.publicTokenMatches = publicToken === PUBLIC_LOGIN_DEFAULTS.js5AuthToken;
      state.signal = signal;
      return masterReport();
    },
    crypto: { getRandomValues(bytes) {
      state.randomCalls++;
      state.randomBuffers.push(bytes);
      for (let index = 0; index < bytes.length; index++) bytes[index] = state.randomCalls * 100 + index;
      return bytes;
    } },
  };
  return { options, state };
}

const hex = (bytes) => Buffer.from(bytes).toString("hex");

test("public Loader values and source-derived preference variants match isolated unchanged native fixture", async () => {
  const native = JSON.parse(await readFile(new URL("../../client-runtime/tests/fixtures/bootstrap-defaults.json", import.meta.url), "utf8"));
  const publicValues = native.loaderDefaults;
  assert.equal(PUBLIC_LOGIN_DEFAULTS.affiliateP, publicValues.affiliateP);
  assert.equal(PUBLIC_LOGIN_DEFAULTS.loginToken, publicValues.clientKey);
  assert.equal(PUBLIC_LOGIN_DEFAULTS.js5AuthToken, publicValues.JS5token);
  assert.equal(PUBLIC_LOGIN_DEFAULTS.sessionToken, publicValues.sessionToken);
  assert.equal(PUBLIC_LOGIN_DEFAULTS.gameId, publicValues.game);
  assert.equal(PUBLIC_LOGIN_DEFAULTS.language, publicValues.language);
  for (const value of [native.defaults, ...Object.values(native.variants)]) {
    assert.equal(hex(defaultLoginPreferences({ maxMemory: value.input.maxMemory, processors: value.input.availableProcessors, isArm: value.processorSpecs.isArm })), value.hex);
  }
});

test("browser metadata uses observed runtime memory and concurrency without JVM or optional hardware claims", () => {
  const value = deriveBrowserMachineInfo(capabilities());
  assert.equal(value.operatingSystem, 3);
  assert.equal(value.x64Os, true);
  assert.equal(value.maxMemory, 245);
  assert.equal(value.processors, 2);
  assert.deepEqual([value.javaVendor, value.javaBuild, value.javaSubBuild, value.javaUpdate], [4, 0, 0, 0]);
  assert.deepEqual([value.ram, value.cpuClock, value.cpuCores, value.cpuModelInfo], [0, 0, 0, 0]);
  assert.deepEqual([value.gpuModel, value.cpuType, value.cpuData, value.directxVersion], ["", "", "", ""]);
  for (const [platform, expected] of [["Win32", 1], ["MacIntel", 2], ["Linux", 3], ["", 4], ["SomethingElse", 4]]) {
    const result = deriveBrowserMachineInfo({ ...capabilities(), platform, architecture: "", bitness: "" });
    assert.equal(result.operatingSystem, expected);
    assert.equal(result.x64Os, false);
  }
});

test("missing or unrepresentable honest runtime metadata fails statically", () => {
  for (const jsHeapSizeLimit of [undefined, null, NaN, Infinity, -1, 0, 1.5, 65535 * 1048576]) {
    assert.throws(() => deriveBrowserMachineInfo({ ...capabilities(), jsHeapSizeLimit }), /browser runtime memory limit/);
  }
  for (const hardwareConcurrency of [undefined, null, 0, -1, 1.5, 256, Infinity]) {
    assert.throws(() => deriveBrowserMachineInfo({ ...capabilities(), hardwareConcurrency }), /browser processor metadata unavailable/);
  }
  assert.throws(() => defaultLoginPreferences({ maxMemory: 245, processors: 2 }), /architecture unavailable/);
});

test("builder derives independent fresh per-attempt seeds and ephemeral instance ID without fixture fields", async () => {
  const { options, state } = fixture();
  const original = account();
  const snapshot = structuredClone(original);
  const result = await buildLoginConfig(original, options);
  assert.deepEqual(original, snapshot);
  assert.deepEqual(Object.keys(result).sort(), ["lobby", "world"]);
  assert.deepEqual(result.lobby.identity, original.identity);
  assert.deepEqual(result.world.identity, original.identity);
  assert.equal(result.lobby.password, original.password);
  assert.equal(Object.hasOwn(result.world, "password"), false);
  assert.notDeepEqual(result.lobby.seed, result.world.seed);
  assert.deepEqual(result.lobby.clientRandomSeed, result.world.clientRandomSeed);
  for (const field of ["identity", "socialNonce", "clientRandomSeed", "random24", "preferences", "indexCrcs"]) assert.notEqual(result.lobby[field], result.world[field]);
  assert.deepEqual(result.lobby.socialNonce, [0, 0]);
  assert.deepEqual(result.world.socialNonce, [0, 0]);
  assert.deepEqual(result.lobby.random24, Array(24).fill(255));
  assert.equal(state.randomCalls, 4);
  assert.ok(state.randomBuffers.every((buffer) => buffer.every((value) => value === 0)));
  assert.equal(state.probes, 1);
  assert.equal(state.relay, "ws://127.0.0.1:8110/js5");
  assert.equal(state.publicTokenMatches, true);
  assert.deepEqual([result.world.displayMode, result.world.screenWidth, result.world.screenHeight], [2, 1024, 768]);
  assert.deepEqual([result.world.reconnecting, result.world.jagtheoraLoaded, result.world.isApplet, result.world.hasSignedTermsAgreement], [false, false, false, false]);
  assert.equal(result.world.supplementaryLoginData, null);
  assert.equal(result.world.loginType, 232);
  assert.equal(result.world.worldId, 22);
  assert.equal(result.world.varVerifyId, 0);
  assert.equal(result.world.differentWorld, true);
  const second = await buildLoginConfig(original, options);
  assert.notDeepEqual(result.lobby.seed, second.lobby.seed);
  assert.notDeepEqual(result.world.seed, second.world.seed);
  assert.notDeepEqual(result.world.machineUid, second.world.machineUid);
});

test("native CRC order selects font32 or font34 then tips33 cutscene35 vorbis36 with unsigned bit preservation", async () => {
  for (const fontIndex of [32, 34]) {
    const { options } = fixture();
    options.fontIndex = fontIndex;
    const { lobby, world } = await buildLoginConfig(account(), options);
    const entries = masterReport().indices;
    const expected = [...entries.slice(0, 32), entries[fontIndex], entries[33], entries[35], entries[36]].map(({ crc }) => crc >>> 0);
    assert.equal(lobby.indexCrcs.length, 36);
    assert.deepEqual(lobby.indexCrcs, expected);
    assert.deepEqual(world.indexCrcs, expected);
    assert.notDeepEqual(expected, entries.slice(0, 36).map(({ crc }) => crc >>> 0));
  }
});

test("public CRC helper needs no account or entropy and preserves native ordering for both fonts", async () => {
  for (const fontIndex of [32, 34]) {
    const abort = new AbortController();
    const { options, state } = fixture();
    const result = await fetchLoginCrcs({ fontIndex, js5Relay: "wss://[::1]:8111/js5", signal: abort.signal, probe: options.probe });
    const entries = masterReport().indices;
    assert.deepEqual(result, [...entries.slice(0, 32), entries[fontIndex], entries[33], entries[35], entries[36]].map(({ crc }) => crc >>> 0));
    assert.equal(state.relay, "wss://[::1]:8111/js5");
    assert.equal(state.publicTokenMatches, true);
    assert.equal(state.signal, abort.signal);
    assert.equal(state.randomCalls, 0);
  }
});

test("public CRC helper refuses non-loopback or decorated relays before calling even an injected probe", async () => {
  for (const js5Relay of [null, {}, "not a URL", "ws://private.invalid/js5", "https://localhost/js5", "ws://localhost/js5?upstream=FAKE_PRIVATE_REASON", "ws://localhost/js5#FAKE_PRIVATE_REASON", "ws://user:FAKE_PRIVATE_REASON@localhost/js5", "ws://127.0.0.1/world", "ws://localhost/JS5", "ws://localhost/" + "x".repeat(512)]) {
    let probed = false;
    await assert.rejects(fetchLoginCrcs({ js5Relay, fontIndex: 32, probe: async () => { probed = true; return masterReport(); } }), { message: "hosted JS5 master unavailable" });
    assert.equal(probed, false);
  }
  for (const fontIndex of [undefined, 0, 33, "32"]) {
    let probed = false;
    await assert.rejects(fetchLoginCrcs({ fontIndex, probe: async () => { probed = true; return masterReport(); } }), { message: "browser font index selection unavailable" });
    assert.equal(probed, false);
  }
});

test("public CRC helper rejects malformed or falsely authenticated world masters with static errors", async () => {
  for (const probe of [
    async () => null,
    async () => ({ ...masterReport(), authenticated: true }),
    async () => ({ ...masterReport(), indices: masterReport().indices.slice(0, 36) }),
    async () => new Proxy({}, { getPrototypeOf() { throw new Error("FAKE_PRIVATE_REASON"); } }),
    async () => { throw new Error("FAKE_PRIVATE_REASON"); },
  ]) {
    await assert.rejects(fetchLoginCrcs({ fontIndex: 32, probe }), (error) => {
      assert.match(error.message, /hosted JS5 master/);
      assert.equal(error.message.includes("FAKE_PRIVATE_REASON"), false);
      return true;
    });
  }
});

test("public CRC helper cancellation prevents a late custom master from supplying CRCs", async () => {
  for (const early of [true, false]) {
    const abort = new AbortController();
    let notify;
    let release;
    const started = new Promise((resolve) => { notify = resolve; });
    if (early) abort.abort("FAKE_PRIVATE_REASON");
    const pending = fetchLoginCrcs({ fontIndex: 34, signal: abort.signal, probe: () => { notify(); return new Promise((resolve) => { release = resolve; }); } });
    const rejected = assert.rejects(pending, { message: "browser login configuration cancelled" });
    if (!early) { await started; abort.abort("FAKE_PRIVATE_REASON"); }
    await rejected;
    release?.(masterReport());
  }
});

test("private input accepts only caller username and password and never reads accessor fields", async () => {
  const inputs = [
    null, [], {}, { ...account(), seed: [1, 2, 3, 4] }, { ...account(), machineInfo: {} },
    { ...account(), identity: { kind: "token", token: [1, 2] } },
    { ...account(), identity: { kind: "username", username: "", extra: true } },
    { ...account(), password: "" }, { ...account(), password: "x".repeat(87) },
    { ...account(), password: "wrong\u0000value" }, { ...account(), password: "emoji_😀" },
    { ...account(), identity: { kind: "username", username: "x".repeat(321) } },
    { ...account(), identity: { kind: "username", username: "\ud800" } },
  ];
  let accessed = false;
  inputs.push({ identity: account().identity, get password() { accessed = true; throw new Error("FAKE_SECRET_MUST_NOT_ESCAPE"); } });
  for (const input of inputs) {
    const { options, state } = fixture();
    await assert.rejects(buildLoginConfig(input, options), /^(Error: )?(invalid|unsupported) private browser/);
    assert.equal(state.probes, 0);
    assert.equal(state.randomCalls, 0);
  }
  assert.equal(accessed, false);
});

test("valid CP1252 identities and bounded passwords are preserved without normalization", async () => {
  const { options } = fixture();
  const input = { identity: { kind: "username", username: "FAKE_Œ_€_ÿ" }, password: "€".repeat(86) };
  const result = await buildLoginConfig(input, options);
  assert.equal(result.lobby.password, input.password);
  assert.equal(result.lobby.identity.username, input.identity.username);
});

test("missing public runtime inputs fail before probe or randomness", async () => {
  for (const key of ["viewport", "machineInfo", "preferences", "fontIndex", "worldId"]) {
    const { options, state } = fixture();
    delete options[key];
    await assert.rejects(buildLoginConfig(account(), options), /browser .*unavailable/);
    assert.equal(state.probes, 0);
    assert.equal(state.randomCalls, 0);
  }
});

test("invalid public runtime ranges and policy marker fail without requests", async () => {
  const edits = [
    (o) => o.viewport.width = 0,
    (o) => o.viewport.height = 65536,
    (o) => o.viewport.width = 100.5,
    (o) => o.machineInfo.maxMemory = 0,
    (o) => o.machineInfo.ram = 0x1000000,
    (o) => o.machineInfo.cpuFeatureFlags = [0, 0],
    (o) => o.machineInfo.cpuType = "darkan-headless-client",
    (o) => o.machineInfo.gpuModel = "x".repeat(41),
    (o) => o.preferences = Array(35).fill(0),
    (o) => o.preferences[1] = 256,
    (o) => o.fontIndex = 33,
    (o) => o.worldId = -1,
    (o) => o.machineUid = [0, -1],
  ];
  for (const edit of edits) {
    const { options, state } = fixture();
    edit(options);
    await assert.rejects(buildLoginConfig(account(), options), /browser/);
    assert.equal(state.probes, 0);
    assert.equal(state.randomCalls, 0);
  }
});

test("malformed incomplete reordered or falsely authenticated master reports cannot supply login CRCs", async () => {
  const edits = [
    (r) => r.revision = 728,
    (r) => r.subrevision = 2,
    (r) => r.prefetchCount = 26,
    (r) => r.authenticated = true,
    (r) => r.indices.pop(),
    (r) => r.indices.reverse(),
    (r) => r.indices[12].index = 11,
    (r) => r.indices[20].crc = 0xffffffff,
    (r) => r.indices[20].crc = 1.5,
    (r) => r.indices[20].revision = Infinity,
    (r) => r.indices[20].privateUnexpected = "FAKE_SECRET",
  ];
  for (const edit of edits) {
    const { options, state } = fixture();
    options.probe = async () => { const report = masterReport(); edit(report); return report; };
    await assert.rejects(buildLoginConfig(account(), options), /hosted JS5 master/);
    assert.equal(state.randomCalls, 0);
  }
});

test("probe and randomness failures return static errors without reflected values", async () => {
  for (const fault of ["probe", "crypto"]) {
    const { options, state } = fixture();
    options[fault] = fault === "probe" ? async () => { throw new Error("FAKE_SECRET_MUST_NOT_ESCAPE"); } : { getRandomValues() { throw new Error("FAKE_SECRET_MUST_NOT_ESCAPE"); } };
    await assert.rejects(buildLoginConfig(account(), options), fault === "probe" ? /^Error: hosted JS5 master unavailable$/ : /^Error: browser cryptographic randomness unavailable$/);
    assert.ok(state.randomBuffers.every((buffer) => buffer.every((value) => value === 0)));
  }
  const { options, state } = fixture();
  options.crypto = null;
  await assert.rejects(buildLoginConfig(account(), options), /cryptographic randomness unavailable/);
  assert.equal(state.probes, 0);
});

test("cancellation before or during JS5 cannot generate login material or retain abort listeners", async () => {
  for (const early of [true, false]) {
    const { options, state } = fixture();
    const controller = new AbortController();
    options.signal = controller.signal;
    options.probe = () => new Promise(() => {});
    if (early) controller.abort("FAKE_PRIVATE_REASON");
    const pending = buildLoginConfig(account(), options);
    const rejected = assert.rejects(pending, /^Error: browser login configuration cancelled$/);
    if (!early) controller.abort("FAKE_PRIVATE_REASON");
    await rejected;
    assert.equal(state.randomCalls, 0);
  }
});

test("late cancellation and partial crypto failures wipe every temporary entropy buffer", async () => {
  for (const cancel of [true, false]) {
    const { options } = fixture();
    const buffers = [];
    const controller = new AbortController();
    options.signal = controller.signal;
    options.crypto = { getRandomValues(bytes) {
      buffers.push(bytes);
      bytes.fill(123);
      if (buffers.length === 3) {
        if (cancel) controller.abort("FAKE_PRIVATE_REASON");
        else throw new Error("FAKE_PRIVATE_REASON");
      }
      return bytes;
    } };
    await assert.rejects(buildLoginConfig(account(), options), cancel ? /^Error: browser login configuration cancelled$/ : /^Error: browser cryptographic randomness unavailable$/);
    assert.ok(buffers.length >= 3);
    assert.ok(buffers.every((bytes) => bytes.every((value) => value === 0)));
  }
});

test("unexpected hostile object failures and invalid relay inputs cannot reflect private values", async () => {
  const { options } = fixture();
  const input = new Proxy(account(), { ownKeys() { throw new Error("FAKE_PRIVATE_REASON"); } });
  await assert.rejects(buildLoginConfig(input, options), /^Error: invalid browser login configuration$/);
  delete options.probe;
  for (const js5Relay of ["not a URL", "https://private.invalid/js5", "ws://user:FAKE_PRIVATE_REASON@localhost/js5", "ws://localhost/world"]) {
    await assert.rejects(buildLoginConfig(account(), { ...options, js5Relay }), /^Error: hosted JS5 master unavailable$/);
  }
});

test("real WebCrypto creates different attempt material and copies caller-supplied observed metadata", async () => {
  const { options } = fixture();
  options.crypto = webcrypto;
  options.machineUid = [7, 8];
  const first = await buildLoginConfig(account(), options);
  const second = await buildLoginConfig(account(), options);
  assert.notDeepEqual(first.lobby.seed, second.lobby.seed);
  assert.notDeepEqual(first.world.seed, second.world.seed);
  assert.notDeepEqual(first.lobby.clientRandomSeed, second.lobby.clientRandomSeed);
  assert.deepEqual(first.world.machineUid, options.machineUid);
  assert.notEqual(first.world.machineUid, options.machineUid);
  assert.notEqual(first.world.machineInfo, options.machineInfo);
  assert.notEqual(first.world.machineInfo.cpuFeatureFlags, options.machineInfo.cpuFeatureFlags);
  assert.notEqual(first.lobby.preferences, options.preferences);
});

test("safe provenance has no attempt, identity, credentials, hardware identifiers, or claim of authentication", () => {
  const provenance = browserLoginProvenance();
  assert.equal(provenance.authenticated, false);
  assert.equal(provenance.machineUid, "ephemeral-browser-instance-not-hardware-attestation");
  assert.equal(Object.isFrozen(provenance), true);
  const serialized = JSON.stringify(provenance);
  for (const secret of [account().password, account().identity.username, "seed", PUBLIC_LOGIN_DEFAULTS.js5AuthToken]) assert.equal(serialized.includes(secret), false);
});

test("generated production-shaped configs are accepted by actual Rust wasm while world token stays Rust-owned", async () => {
  const runtime = await import("../static/protocol-runtime.js");
  await runtime.default({ module_or_path: await readFile(new URL("../static/protocol-runtime_bg.wasm", import.meta.url)) });
  const { options } = fixture();
  const configs = await buildLoginConfig(account(), options);
  const lobby = new runtime.OriginalLobbySession(JSON.stringify(configs.lobby));
  let world;
  try {
    const handshake = lobby.outgoing();
    assert.equal(handshake.length, 1);
    handshake.fill(0);
    lobby.feed(new Uint8Array([0]));
    const request = lobby.outgoing();
    assert.ok(request.length > 128);
    request.fill(0);
    const success = new Uint8Array(await readFile(new URL("../../login/tests/fixtures/lobby-success.wire", import.meta.url)));
    const result = JSON.parse(lobby.feed(success));
    success.fill(0);
    assert.equal(result.event.kind, "loginAccepted");
    world = lobby.startWorld(JSON.stringify(configs.world));
    const worldHandshake = world.outgoing();
    assert.equal(worldHandshake.length, 1);
    worldHandshake.fill(0);
    world.feed(new Uint8Array([0]));
    const worldRequest = world.outgoing();
    assert.ok(worldRequest.length > 128);
    worldRequest.fill(0);
    assert.equal(world.phase(), "awaitResponse");
  } finally {
    world?.close();
    world?.free();
    lobby.close();
    lobby.free();
  }
});
