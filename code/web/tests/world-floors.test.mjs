import test from "node:test";
import assert from "node:assert/strict";
import { setImmediate } from "node:timers/promises";
import { openWorldFloors } from "../static/world-floors.js";
import { PUBLIC_LOGIN_DEFAULTS } from "../static/login-config.js";

const MIB = 1024 * 1024;
const relay = "ws://127.0.0.1:8110/js5";
const secret = "FAKE_PRIVATE_SERVER_SECRET";
const unavailable = { message: "verified world floor definitions unavailable" };
const cancelled = { message: "world floor definitions cancelled" };
const timedOut = { message: "world floor definitions timed out" };
const definitionUnavailable = { message: "world floor definition unavailable" };
const paletteUnavailable = { message: "world floor palette unavailable" };
const closed = { message: "world floor definitions owner is closed" };

function reportFor(state) {
  return {
    revision: 727, subrevision: 1, prefetchCount: 27, index: 2,
    referenceRevision: -1234, referenceArchives: 24, referenceFiles: 8,
    referenceCrcVerified: true, referenceWhirlpoolVerified: true,
    authenticated: false, renderedWorld: false, opcode8SideEffectsApplied: false,
    totalWireBytes: state.received, totalContainerBytes: 96, totalExpandedBytes: 80, totalOperations: 1000,
    underlays: { archive: 1, archiveRevision: -5, containerBytes: 37, expandedBytes: 32, fileSlots: 6, populatedDefinitions: 3, definitionBytes: 10, crcVerified: true, whirlpoolVerified: false },
    overlays: { archive: 4, archiveRevision: 25, containerBytes: 29, expandedBytes: 24, fileSlots: 4, populatedDefinitions: 2, definitionBytes: 8, crcVerified: true, whirlpoolVerified: true },
    limits: {
      maxReferenceArchives: 65536, maxReferenceFiles: 262144,
      maxDefinitionsPerArchive: 65536, maxTotalDefinitions: 131072, maxFloorId: 65535,
      maxDefinitionBytes: 4096, maxTagsPerDefinition: 256,
      maxContainerBytes: 4 * MIB, maxExpandedContainerBytes: 8 * MIB,
      maxTotalWireBytes: 20 * MIB, maxTotalContainerBytes: 16 * MIB,
      maxTotalExpandedBytes: 24 * MIB, maxTotalOperations: 128 * MIB,
      operationUnit: "wire/container/expanded/split/definition bytes and bounded metadata records",
    },
  };
}

function underlayFor(id) {
  const present = [0, 2, 5].includes(id);
  return {
    id, present, rgb: present ? 0x123456 : 0, texture: -1, scale: 512, shadowed: true, occlude: true,
    hue: present ? -12 : 0, saturation: present ? 80 : 0, lightness: present ? 34 : 0, hueMultiplier: present ? 100 : 0,
  };
}

function overlayFor(id) {
  const present = [1, 3].includes(id);
  return {
    id, present, overlayId: id, slot: ((present ? 9 : 8) << 8) | id,
    tileColorHsl: present ? 5234 : 0, minimapColorHsl: -1, texture: -1, textureScale: 512,
    occlude: true, shadowed: true, blendsWithUnderlay: false,
    waterColor: 1190717, waterFogDepth: 512, waterIntensity: 255,
    opcode20: 63, unusedOpcode21: 0, unusedOpcode22: 64, sawOpcode8: present,
  };
}

function paletteFor(mapSize = 72) {
  const tileCount = 4 * mapSize * mapSize;
  return {
    scope: "underlay-packed-hsl", mapSize, planes: 4, tileCount,
    knownColors: tileCount - 12, unavailableColors: 12, unavailableDefinitionIds: [7, 254],
    rgbPrepared: false, renderedWorld: false,
  };
}

function fixture(overrides = {}) {
  const state = {
    loads: 0, created: 0, freed: 0, closed: 0, finishes: 0, reports: 0,
    phase: 0, completeAfter: 2, received: 0, feeds: [], outgoing: [], sent: [], underlay: [], overlay: [],
    ...overrides,
  };
  const fault = (method) => {
    state.hooks?.[method]?.();
    if (state.errorAt === method) throw new Error(secret);
  };
  class Probe {
    constructor(token) { state.created++; state.token = token; state.probe = this; fault("constructor"); }
    outgoing() {
      fault("outgoing");
      const bytes = state.outgoingValue ? state.outgoingValue() : new Uint8Array([state.phase ? 6 : 15]);
      state.outgoing.push(bytes);
      return bytes;
    }
    feed(bytes) {
      state.feeds.push(bytes.length <= 1024 ? [...bytes] : { bytes: bytes.length, first: bytes[0] });
      state.received += bytes.length;
      state.phase++;
      fault("feed");
    }
    complete() { fault("complete"); return Object.hasOwn(state, "completeValue") ? state.completeValue : state.phase >= state.completeAfter; }
    finish() { state.finishes++; fault("finish"); assert.ok(state.phase >= state.completeAfter); }
    report() {
      state.reports++;
      fault("report");
      const report = reportFor(state);
      state.mutateReport?.(report);
      return state.reportText ?? JSON.stringify(report);
    }
    underlay(id) {
      state.underlay.push(id);
      fault("underlay");
      const value = underlayFor(id);
      state.mutateUnderlay?.(value);
      return state.underlayText ?? JSON.stringify(value);
    }
    overlay(id) {
      state.overlay.push(id);
      fault("overlay");
      const value = overlayFor(id);
      state.mutateOverlay?.(value);
      return state.overlayText ?? JSON.stringify(value);
    }
    free() { state.freed++; fault("free"); }
  }
  class Socket {
    constructor(url) { state.socket = this; state.url = url; fault("socket"); }
    send(bytes) { state.sent.push([...bytes]); fault("send"); }
    close() { state.closed++; fault("close"); }
  }
  const options = {
    js5Relay: relay, load: async () => { state.loads++; fault("load"); return Probe; },
    WebSocketClass: Socket, timeoutMs: 1000, lifetimeMs: 2000,
  };
  return { state, options, Probe, Socket };
}

async function opened(f) {
  await setImmediate();
  assert.ok(f.state.socket);
  f.state.socket.onopen();
}

function message(f, values = [1]) {
  const data = new Uint8Array(values);
  f.state.socket.onmessage({ data: data.buffer });
  assert.deepEqual([...data], Array(data.length).fill(0));
}

async function retained(f) {
  const pending = openWorldFloors(f.options);
  await opened(f);
  message(f);
  message(f);
  return pending;
}

function cleaned(f, freed = 1) {
  assert.equal(f.state.freed, freed);
  assert.equal(f.state.closed, 1);
  for (const handler of ["onopen", "onmessage", "onerror", "onclose"]) assert.equal(f.state.socket[handler], null);
  for (const bytes of f.state.outgoing) if (ArrayBuffer.isView(bytes)) assert.ok(new Uint8Array(bytes.buffer, bytes.byteOffset, bytes.byteLength).every((value) => value === 0));
}

async function rejectReport(overrides) {
  const f = fixture(overrides);
  const pending = openWorldFloors(f.options);
  const rejected = assert.rejects(pending, unavailable);
  await opened(f);
  message(f);
  message(f);
  await rejected;
  cleaned(f);
}

function fakeTimers(t) {
  let now = 0;
  const timers = [];
  t.mock.method(globalThis.performance, "now", () => now);
  t.mock.method(globalThis, "setTimeout", (callback, delay) => {
    const timer = { callback, delay, active: true };
    timers.push(timer);
    return timer;
  });
  t.mock.method(globalThis, "clearTimeout", (timer) => { if (timer) timer.active = false; });
  return {
    pending: (delay) => timers.filter((timer) => timer.active && (delay === undefined || timer.delay === delay)),
    advance: (amount) => { now += amount; },
    fire(timer) { assert.ok(timer?.active); timer.active = false; timer.callback(); },
  };
}

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}

test("verified archive 1 and 4 definitions retain WASM after closing JS5 until explicit owner close", async () => {
  const f = fixture();
  const pending = openWorldFloors(f.options);
  await opened(f);
  assert.equal(f.state.url, relay);
  assert.equal(f.state.socket.binaryType, "arraybuffer");
  assert.equal(f.state.token, PUBLIC_LOGIN_DEFAULTS.js5AuthToken);
  assert.deepEqual(f.state.sent, [[15]]);
  message(f, [7, 8]);
  assert.equal(f.state.finishes, 0);
  message(f, [9]);
  const owner = await pending;
  assert.ok(Object.isFrozen(owner));
  assert.deepEqual(f.state.feeds, [[7, 8], [9]]);
  assert.equal(f.state.finishes, 1);
  assert.equal(f.state.reports, 1);
  cleaned(f, 0);
  const state = owner.state();
  assert.equal(state.accepted, true);
  assert.equal(state.closed, false);
  assert.equal(state.phase, "ready");
  assert.equal(state.scope, "floor-definitions-only");
  assert.equal(state.authenticated, false);
  assert.equal(state.renderedWorld, false);
  assert.equal(state.opcode8SideEffectsApplied, false);
  assert.equal(state.receivedBytes, 3);
  assert.equal(state.report.underlays.archive, 1);
  assert.equal(state.report.overlays.archive, 4);
  assert.equal(state.report.underlays.whirlpoolVerified, false);
  assert.equal(state.report.referenceRevision, -1234);
  assert.deepEqual(owner.underlay(0), underlayFor(0));
  assert.deepEqual(owner.overlay(1), overlayFor(1));
  assert.equal(owner.overlay(1).sawOpcode8, true);
  assert.equal(owner.state().opcode8SideEffectsApplied, false);
  owner.close();
  owner.close();
  assert.equal(owner.state().accepted, false);
  assert.equal(owner.state().closed, true);
  assert.equal(owner.state().phase, "closed");
  assert.equal(owner.state().report.referenceRevision, -1234);
  assert.equal(owner.state().error, null);
  assert.throws(() => owner.underlay(0), closed);
  assert.throws(() => owner.overlay(1), closed);
  assert.throws(() => owner.composeTerrain({ composePalette: () => JSON.stringify(paletteFor()) }), closed);
  cleaned(f);
});

test("report and per-id objects are isolated and unknown private fields never cross the owner boundary", async () => {
  const f = fixture({
    mutateReport(report) { report.private = secret; report.limits.private = secret; report.underlays.private = secret; report.overlays.private = secret; },
    mutateUnderlay(value) { value.private = secret; value.renderedWorld = true; },
    mutateOverlay(value) { value.private = secret; value.rgbPalette = [secret]; },
  });
  const owner = await retained(f);
  try {
    const state = owner.state();
    state.accepted = false;
    state.report.limits.maxFloorId = -1;
    state.report.underlays.archive = 99;
    state.report.overlays.populatedDefinitions = 0;
    const underlay = owner.underlay(0);
    const overlay = owner.overlay(1);
    underlay.rgb = 0;
    overlay.waterColor = 0;
    assert.equal(owner.state().accepted, true);
    assert.equal(owner.state().report.limits.maxFloorId, 65535);
    assert.equal(owner.state().report.underlays.archive, 1);
    assert.equal(owner.state().report.overlays.populatedDefinitions, 2);
    assert.deepEqual(owner.underlay(0), underlayFor(0));
    assert.deepEqual(owner.overlay(1), overlayFor(1));
    assert.equal(JSON.stringify([owner.state(), owner.underlay(0), owner.overlay(1)]).includes(secret), false);
    assert.equal(Object.hasOwn(owner.underlay(0), "renderedWorld"), false);
    assert.equal(Object.hasOwn(owner.overlay(1), "rgbPalette"), false);
  } finally { owner.close(); }
});

test("only literal exact loopback JS5 routes can load or allocate", async () => {
  const invalid = [
    undefined, null, 123, {}, "/js5", "ws://example.com/js5", "ws://localhost.evil/js5", "ws://127.0.0.1.evil/js5",
    "http://localhost/js5", "ws://localhost/world", "ws://localhost/js5/", "ws://localhost/js5?upstream=private", "ws://localhost/js5?", "ws://localhost/js5#", "ws://localhost/js5#private",
    "ws://user@localhost/js5", "ws://user:password@localhost/js5", "ws://127.1/js5", "ws://2130706433/js5", "ws://0x7f000001/js5", "ws://127.000.000.001/js5",
    "ws://LOCALHOST/js5", "ws://localhost./js5", "ws://[::ffff:127.0.0.1]/js5", "ws://localhost/x/../js5", "ws://localhost/%6as5",
    " ws://localhost/js5", "ws://localhost/js5\n", "ws://localhost:0/js5", "ws://localhost:00000/js5", "ws://localhost:65536/js5", "ws://localhost:123456/js5",
  ];
  for (const js5Relay of invalid) {
    const f = fixture();
    await assert.rejects(openWorldFloors({ ...f.options, js5Relay }), unavailable);
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.socket, undefined);
  }
  for (const js5Relay of ["ws://localhost/js5", "wss://localhost:8110/js5", "ws://[::1]:8110/js5", "wss://127.0.0.1/js5", "ws://127.0.0.1:65535/js5"]) {
    const f = fixture();
    f.options.js5Relay = js5Relay;
    const owner = await retained(f);
    try { assert.equal(f.state.url, new URL(js5Relay).href); }
    finally { owner.close(); }
  }
});

test("timeouts, lifetimes and injected constructors are bounded and noncoercing before runtime load", async () => {
  for (const [key, values] of Object.entries({
    timeoutMs: [0, -1, 30001, Infinity, NaN, "100", 1.5, null, 1n],
    lifetimeMs: [0, -1, 300001, Infinity, NaN, "100", 1.5, null, 1n],
    load: [null, {}, false, "private"], WebSocketClass: [null, {}, false, "private"],
  })) for (const value of values) {
    const f = fixture();
    await assert.rejects(openWorldFloors({ ...f.options, [key]: value }), unavailable);
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.socket, undefined);
  }
  for (const options of [null, false, [], "private", { get js5Relay() { throw new Error(secret); } }]) await assert.rejects(openWorldFloors(options), unavailable);
});

test("pre-abort and same-turn cancellation never call runtime loaders", async () => {
  for (const preAborted of [true, false]) {
    const f = fixture();
    const abort = new AbortController();
    if (preAborted) abort.abort(secret);
    const pending = openWorldFloors({ ...f.options, signal: abort.signal });
    const rejected = assert.rejects(pending, cancelled);
    if (!preAborted) abort.abort(secret);
    await rejected;
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.socket, undefined);
  }
});

test("invalid signals and thrown abort subscription errors stay static and allocate nothing", async () => {
  for (const signal of [false, {}, { aborted: secret }, { get aborted() { throw new Error(secret); } }, { aborted: false, addEventListener() { throw new Error(secret); }, removeEventListener() { throw new Error(secret); } }]) {
    const f = fixture();
    await assert.rejects(openWorldFloors({ ...f.options, signal }), unavailable);
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.created, 0);
  }
});

test("abort immediately rejects an ignored loader signal and ignores late fulfillment or rejection", async () => {
  for (const result of ["resolve", "reject", "never"]) {
    const f = fixture();
    const loading = deferred();
    const abort = new AbortController();
    f.options.load = () => loading.promise;
    const pending = openWorldFloors({ ...f.options, signal: abort.signal });
    const rejected = assert.rejects(pending, cancelled);
    await setImmediate();
    abort.abort(secret);
    await rejected;
    if (result !== "never") loading[result](result === "resolve" ? f.Probe : new Error(secret));
    await setImmediate();
    assert.equal(f.state.created, 0);
    assert.equal(f.state.freed, 0);
    assert.equal(f.state.socket, undefined);
  }
});

test("one bounded deadline includes a never-settling loader and ignores late classes", async (t) => {
  const clock = fakeTimers(t);
  for (const late of [false, true]) {
    const f = fixture();
    const loading = deferred();
    f.options.load = () => loading.promise;
    const pending = openWorldFloors(f.options);
    const rejected = assert.rejects(pending, timedOut);
    await setImmediate();
    clock.fire(clock.pending(1000)[0]);
    await rejected;
    if (late) loading.resolve(f.Probe);
    await setImmediate();
    assert.equal(f.state.created, 0);
    assert.equal(f.state.freed, 0);
    assert.equal(f.state.socket, undefined);
    assert.equal(clock.pending().length, 0);
  }
});

test("loader time is not reset at socket creation and synchronous loading cannot exceed the deadline", async (t) => {
  const clock = fakeTimers(t);
  const f = fixture();
  const loading = deferred();
  f.options.load = () => loading.promise;
  const pending = openWorldFloors(f.options);
  const rejected = assert.rejects(pending, timedOut);
  await setImmediate();
  const deadline = clock.pending(1000)[0];
  clock.advance(900);
  loading.resolve(f.Probe);
  await opened(f);
  assert.deepEqual(clock.pending(), [deadline]);
  clock.advance(100);
  message(f);
  await rejected;
  cleaned(f);
  const next = fixture();
  next.options.load = () => { clock.advance(1000); return next.Probe; };
  await assert.rejects(openWorldFloors(next.options), timedOut);
  assert.equal(next.state.created, 0);
  assert.equal(next.state.socket, undefined);
  assert.equal(clock.pending().length, 0);
});

test("loader exceptions, malformed exports and missing methods never leak thrown secrets", async () => {
  for (const load of [() => { throw new Error(secret); }, async () => { throw new Error(secret); }, async () => undefined, async () => ({ private: secret })]) {
    const f = fixture();
    await assert.rejects(openWorldFloors({ ...f.options, load }), unavailable);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.socket, undefined);
  }
  for (const method of ["outgoing", "feed", "complete", "finish", "report", "underlay", "overlay"]) {
    const f = fixture();
    f.Probe.prototype[method] = null;
    await assert.rejects(openWorldFloors(f.options), unavailable);
    assert.equal(f.state.freed, 1);
    assert.equal(f.state.socket, undefined);
  }
  const f = fixture({ errorAt: "constructor" });
  await assert.rejects(openWorldFloors(f.options), unavailable);
  assert.equal(f.state.created, 1);
  assert.equal(f.state.socket, undefined);
});

test("abort during loading, probe construction and socket construction frees every returned allocation once", async () => {
  for (const point of ["load", "constructor", "socket"]) {
    const abort = new AbortController();
    const f = fixture({ hooks: { [point]: () => abort.abort(secret) } });
    await assert.rejects(openWorldFloors({ ...f.options, signal: abort.signal }), cancelled);
    assert.equal(f.state.created, point === "load" ? 0 : 1);
    assert.equal(f.state.freed, point === "load" ? 0 : 1);
    if (point === "socket") cleaned(f);
    else assert.equal(f.state.socket, undefined);
  }
  const f = fixture();
  f.options.WebSocketClass = class { constructor() { throw new Error(secret); } };
  await assert.rejects(openWorldFloors(f.options), unavailable);
  assert.equal(f.state.freed, 1);
});

test("abort listeners remain through retention but are removed on every terminal path", async () => {
  for (const outcome of ["success", "abort", "error"]) {
    const abort = new AbortController();
    const listeners = new Set();
    const add = abort.signal.addEventListener.bind(abort.signal);
    const remove = abort.signal.removeEventListener.bind(abort.signal);
    abort.signal.addEventListener = (kind, listener, options) => { listeners.add(listener); add(kind, listener, options); };
    abort.signal.removeEventListener = (kind, listener) => { listeners.delete(listener); remove(kind, listener); };
    const f = fixture();
    const pending = openWorldFloors({ ...f.options, signal: abort.signal });
    const rejected = outcome === "success" ? null : assert.rejects(pending, outcome === "abort" ? cancelled : unavailable);
    await opened(f);
    assert.equal(listeners.size, 1);
    if (outcome === "success") {
      message(f);
      message(f);
      const owner = await pending;
      assert.equal(listeners.size, 1);
      owner.close();
    } else {
      if (outcome === "abort") abort.abort(secret);
      else f.state.socket.onerror();
      await rejected;
    }
    assert.equal(listeners.size, 0);
    cleaned(f);
  }
});

test("normal EOF, abnormal close and socket error before completion never finish or publish", async () => {
  for (const event of ["normal", "abnormal", "error"]) {
    const f = fixture();
    const pending = openWorldFloors(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    if (event === "error") f.state.socket.onerror(new Error(secret));
    else f.state.socket.onclose({ code: event === "normal" ? 1000 : 1006, wasClean: event === "normal", reason: secret });
    await rejected;
    assert.equal(f.state.finishes, 0);
    assert.equal(f.state.reports, 0);
    cleaned(f);
  }
});

test("messages require ArrayBuffers and enforce inclusive 4 MiB message and 20 MiB cumulative budgets", async () => {
  for (const data of [secret, new ArrayBuffer(4 * MIB + 1), new Uint8Array([7]), new DataView(new Uint8Array([7]).buffer), null, new Blob(["private"])]) {
    const f = fixture();
    const pending = openWorldFloors(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    f.state.socket.onmessage({ data });
    await rejected;
    if (data instanceof ArrayBuffer) assert.ok(new Uint8Array(data).every((value) => value === 0));
    if (ArrayBuffer.isView(data)) assert.ok(new Uint8Array(data.buffer).every((value) => value === 0));
    assert.deepEqual(f.state.feeds, []);
    cleaned(f);
  }
  const f = fixture({ completeAfter: Infinity });
  const pending = openWorldFloors(f.options);
  const rejected = assert.rejects(pending, unavailable);
  await opened(f);
  for (let at = 0; at < 5; at++) {
    const bytes = new Uint8Array(4 * MIB).fill(7);
    f.state.socket.onmessage({ data: bytes.buffer });
    assert.ok(bytes.every((value) => value === 0));
  }
  assert.equal(f.state.received, 20 * MIB);
  assert.equal(f.state.feeds.length, 5);
  message(f);
  await rejected;
  assert.equal(f.state.received, 20 * MIB);
  assert.equal(f.state.feeds.length, 5);
  cleaned(f);
});

test("outgoing bytes are bounded and wiped even for rejected types, send failure and cancellation", async () => {
  for (const outgoingValue of [() => new Uint8Array(4 * MIB + 1).fill(7), () => new Uint16Array([7, 8]), () => new DataView(new Uint8Array([7, 8]).buffer), () => secret]) {
    const f = fixture({ outgoingValue });
    const pending = openWorldFloors(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    await rejected;
    cleaned(f);
  }
  for (const point of ["send", "outgoing"]) {
    const abort = new AbortController();
    const f = fixture();
    if (point === "send") f.state.errorAt = "send";
    else f.state.hooks = { outgoing: () => abort.abort(secret) };
    const pending = openWorldFloors({ ...f.options, signal: abort.signal });
    const rejected = assert.rejects(pending, point === "send" ? unavailable : cancelled);
    await opened(f);
    await rejected;
    cleaned(f);
  }
  const f = fixture({ outgoingValue: () => new Uint8Array(4 * MIB) });
  const pending = openWorldFloors(f.options);
  const rejected = assert.rejects(pending, unavailable);
  await opened(f);
  assert.equal(f.state.sent[0].length, 4 * MIB);
  f.state.socket.onerror();
  await rejected;
  cleaned(f);
});

test("only boolean completion may finish and report, and runtime failures always clean up", async () => {
  for (const completeValue of [1, "true", null, {}, undefined]) {
    const f = fixture({ completeValue });
    const pending = openWorldFloors(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    message(f);
    await rejected;
    assert.equal(f.state.finishes, 0);
    assert.equal(f.state.reports, 0);
    cleaned(f);
  }
  for (const errorAt of ["outgoing", "send", "feed", "complete", "finish", "report"]) {
    const f = fixture({ errorAt });
    const pending = openWorldFloors(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    if (!["outgoing", "send"].includes(errorAt)) message(f);
    if (["finish", "report"].includes(errorAt)) message(f);
    await rejected;
    cleaned(f);
  }
});

test("all report identities, integrity claims and typed counters must match the fixed floor contract", async () => {
  const mutations = [
    ...["revision", "subrevision", "prefetchCount", "index"].map((key) => (r) => { r[key]++; }),
    ...["referenceCrcVerified", "referenceWhirlpoolVerified"].map((key) => (r) => { r[key] = false; }),
    ...["authenticated", "renderedWorld", "opcode8SideEffectsApplied"].map((key) => (r) => { r[key] = true; }),
    ...["referenceRevision", "referenceArchives", "referenceFiles", "totalWireBytes", "totalContainerBytes", "totalExpandedBytes", "totalOperations"].flatMap((key) => [
      (r) => { r[key] = secret; }, (r) => { r[key] = 1.5; }, (r) => { delete r[key]; },
    ]),
    (r) => { r.referenceRevision = 0x80000000; }, (r) => { r.referenceRevision = -0x80000001; },
    (r) => { r.referenceArchives = 1; }, (r) => { r.referenceArchives = 65537; },
    (r) => { r.referenceFiles = 262145; }, (r) => { r.referenceFiles = 4; },
    (r) => { r.totalWireBytes++; }, (r) => { r.totalWireBytes = 20 * MIB + 1; },
    (r) => { r.totalContainerBytes = 16 * MIB + 1; }, (r) => { r.totalContainerBytes = 65; },
    (r) => { r.totalExpandedBytes = 24 * MIB + 1; }, (r) => { r.totalExpandedBytes = 55; },
    (r) => { r.totalOperations = 128 * MIB + 1; }, (r) => { r.totalOperations = -1; },
    (r) => { r.underlays = []; }, (r) => { r.overlays = null; },
    (r) => { [r.underlays, r.overlays] = [r.overlays, r.underlays]; },
  ];
  for (const mutateReport of mutations) await rejectReport({ mutateReport });
  for (const reportText of ["{", JSON.stringify(secret), "null", "[]", "x".repeat(65537), 7, {}]) await rejectReport({ reportText });
});

test("every declared floor cap and operation unit is exact rather than an arbitrary runtime claim", async () => {
  const limits = reportFor({ received: 0 }).limits;
  for (const key of Object.keys(limits)) {
    await rejectReport({ mutateReport(r) { r.limits[key] = key === "operationUnit" ? secret : limits[key] + 1; } });
    await rejectReport({ mutateReport(r) { delete r.limits[key]; } });
  }
  for (const limits of [null, [], secret]) await rejectReport({ mutateReport(r) { r.limits = limits; } });
});

test("wrong archive identities, integrity, slot counts and impossible definition byte totals are rejected", async () => {
  const mutations = [
    (a) => { a.archive++; }, (a) => { a.archiveRevision = 0x80000000; },
    (a) => { a.crcVerified = false; }, (a) => { a.crcVerified = "true"; }, (a) => { a.whirlpoolVerified = 1; },
    (a) => { a.containerBytes = 4; }, (a) => { a.containerBytes = 4 * MIB + 1; },
    (a) => { a.expandedBytes = 8 * MIB + 1; }, (a) => { a.expandedBytes = -1; },
    (a) => { a.fileSlots = 65537; }, (a) => { a.fileSlots = a.populatedDefinitions - 1; },
    (a) => { a.populatedDefinitions = 65537; }, (a) => { a.populatedDefinitions = 0; },
    (a) => { a.definitionBytes = a.populatedDefinitions - 1; },
    (a) => { a.definitionBytes = a.populatedDefinitions * 4096 + 1; },
    (a) => { a.definitionBytes = a.expandedBytes + 1; },
    (a) => { a.definitionBytes = -1; },
    ...["archiveRevision", "containerBytes", "expandedBytes", "fileSlots", "populatedDefinitions", "definitionBytes"].map((key) => (a) => { a[key] = "0"; }),
  ];
  for (const key of ["underlays", "overlays"]) for (const mutate of mutations) await rejectReport({ mutateReport(r) { mutate(r[key]); } });
});

test("reference maximums, sparse definitions and explicitly empty verified archives remain distinct", async () => {
  const f = fixture({ mutateReport(r) { r.referenceArchives = 65536; r.referenceFiles = 262144; } });
  const owner = await retained(f);
  try {
    assert.equal(owner.state().report.referenceArchives, 65536);
    assert.equal(owner.state().report.referenceFiles, 262144);
    assert.equal(owner.state().report.underlays.fileSlots, 6);
    assert.equal(owner.state().report.underlays.populatedDefinitions, 3);
    assert.deepEqual(owner.underlay(1), underlayFor(1));
    assert.deepEqual(owner.underlay(65535), underlayFor(65535));
    assert.deepEqual(owner.overlay(65535), overlayFor(65535));
  } finally { owner.close(); }
  const empty = fixture({ mutateReport(r) {
    r.referenceFiles = 0;
    for (const a of [r.underlays, r.overlays]) Object.assign(a, { fileSlots: 0, populatedDefinitions: 0, definitionBytes: 0, expandedBytes: 0 });
  }, mutateUnderlay(v) { Object.assign(v, underlayFor(65535), { id: v.id }); }, mutateOverlay(v) { Object.assign(v, overlayFor(65535), { id: v.id, overlayId: v.id, slot: (8 << 8) | v.id }); } });
  const defaults = await retained(empty);
  try {
    assert.equal(defaults.state().report.underlays.populatedDefinitions, 0);
    assert.equal(defaults.state().report.overlays.populatedDefinitions, 0);
    assert.equal(defaults.underlay(0).present, false);
    assert.equal(defaults.overlay(0).present, false);
  } finally { defaults.close(); }
});

test("invalid IDs never reach WASM and do not corrupt retained definition ownership", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    for (const id of [undefined, null, NaN, Infinity, -1, 65536, 1.5, "0", true, 1n, { valueOf() { assert.fail("must not coerce"); } }]) {
      assert.throws(() => owner.underlay(id), { message: "invalid world floor definition id" });
      assert.throws(() => owner.overlay(id), { message: "invalid world floor definition id" });
    }
    assert.deepEqual(f.state.underlay, []);
    assert.deepEqual(f.state.overlay, []);
    assert.deepEqual(owner.underlay(5), underlayFor(5));
    assert.deepEqual(owner.overlay(3), overlayFor(3));
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
  } finally { owner.close(); }
});

test("underlay schema requires bounded finite native fields and exact absent defaults", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    const mutations = [
      (v) => { v.id++; }, (v) => { v.present = "true"; },
      ...["rgb", "texture", "scale", "hue", "saturation", "lightness", "hueMultiplier"].flatMap((key) => [
        (v) => { v[key] = "0"; }, (v) => { v[key] = null; }, (v) => { v[key] = 1.5; }, (v) => { delete v[key]; },
      ]),
      (v) => { v.rgb = 0x1000000; }, (v) => { v.rgb = -1; }, (v) => { v.texture = 65535; }, (v) => { v.texture = -2; },
      (v) => { v.scale = 262144; }, (v) => { v.scale = 513; }, (v) => { v.hue = -256; }, (v) => { v.hue = 256; },
      ...["saturation", "lightness", "hueMultiplier"].map((key) => (v) => { v[key] = 256; }),
      (v) => { v.shadowed = 1; }, (v) => { v.occlude = null; },
    ];
    for (const mutateUnderlay of mutations) {
      f.state.mutateUnderlay = mutateUnderlay;
      assert.throws(() => owner.underlay(0), definitionUnavailable);
    }
    for (const key of Object.keys(underlayFor(1)).filter((key) => !["id", "present"].includes(key))) {
      f.state.mutateUnderlay = (v) => { v[key] = typeof v[key] === "boolean" ? !v[key] : v[key] + (key === "scale" ? 4 : 1); };
      assert.throws(() => owner.underlay(1), definitionUnavailable);
    }
    f.state.mutateUnderlay = undefined;
    assert.deepEqual(owner.underlay(1), underlayFor(1));
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
  } finally { owner.close(); }
});

test("overlay schema rejects forged identities, non-native slots, invalid fields and altered absent defaults", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    const mutations = [
      (v) => { v.id++; }, (v) => { v.overlayId++; }, (v) => { v.present = 1; },
      ...["slot", "tileColorHsl", "minimapColorHsl", "texture", "textureScale", "waterColor", "waterFogDepth", "waterIntensity", "opcode20", "unusedOpcode21", "unusedOpcode22"].flatMap((key) => [
        (v) => { v[key] = "0"; }, (v) => { v[key] = null; }, (v) => { v[key] = 1.5; }, (v) => { delete v[key]; },
      ]),
      (v) => { v.slot = 0; }, (v) => { v.slot = 65536; }, (v) => { v.slot++; },
      (v) => { v.tileColorHsl = -2; }, (v) => { v.minimapColorHsl = 65536; },
      (v) => { v.texture = 65535; }, (v) => { v.texture = -2; }, (v) => { v.textureScale = 262144; }, (v) => { v.textureScale = 513; },
      (v) => { v.waterColor = 0x1000000; }, (v) => { v.waterFogDepth = 8161; }, (v) => { v.waterFogDepth = 513; },
      (v) => { v.waterIntensity = 256; }, (v) => { v.opcode20 = 65536; }, (v) => { v.unusedOpcode21 = 256; }, (v) => { v.unusedOpcode22 = 65536; },
      ...["occlude", "shadowed", "blendsWithUnderlay", "sawOpcode8"].map((key) => (v) => { v[key] = 1; }),
    ];
    for (const mutateOverlay of mutations) {
      f.state.mutateOverlay = mutateOverlay;
      assert.throws(() => owner.overlay(1), definitionUnavailable);
    }
    for (const key of Object.keys(overlayFor(0)).filter((key) => !["id", "present", "overlayId"].includes(key))) {
      f.state.mutateOverlay = (v) => { v[key] = typeof v[key] === "boolean" ? !v[key] : v[key] + (key === "textureScale" ? 4 : key === "waterFogDepth" ? 32 : key === "slot" ? 256 : 1); };
      assert.throws(() => owner.overlay(0), definitionUnavailable);
    }
    f.state.mutateOverlay = undefined;
    assert.deepEqual(owner.overlay(0), overlayFor(0));
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
  } finally { owner.close(); }
});

test("presence metadata must agree with verified file slots and native defaults", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    f.state.mutateUnderlay = (v) => { v.present = true; };
    assert.throws(() => owner.underlay(6), definitionUnavailable);
    f.state.mutateUnderlay = (v) => { Object.assign(v, underlayFor(1), { id: v.id }); };
    assert.throws(() => owner.underlay(5), definitionUnavailable);
    f.state.mutateOverlay = (v) => { v.present = true; };
    assert.throws(() => owner.overlay(4), definitionUnavailable);
    f.state.mutateOverlay = (v) => { Object.assign(v, overlayFor(0), { id: v.id, overlayId: v.id, slot: (8 << 8) | v.id }); };
    assert.throws(() => owner.overlay(3), definitionUnavailable);
    assert.equal(owner.state().accepted, true);
  } finally { owner.close(); }
  const dense = fixture({ mutateReport(r) { r.underlays.populatedDefinitions = 6; } });
  const full = await retained(dense);
  try { assert.throws(() => full.underlay(1), definitionUnavailable); }
  finally { full.close(); }
});

test("lookup exceptions and malformed JSON remain static non-destructive failures", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    for (const key of ["underlay", "overlay"]) {
      for (const value of ["{", JSON.stringify(secret), "null", "[]", "x".repeat(65537), 7, {}, "{\"id\":0,\"present\":true,\"__proto__\":{\"rgb\":123}}"] ) {
        f.state[`${key}Text`] = value;
        assert.throws(() => owner[key](key === "underlay" ? 0 : 1), definitionUnavailable);
      }
      delete f.state[`${key}Text`];
      f.state.errorAt = key;
      assert.throws(() => owner[key](key === "underlay" ? 0 : 1), definitionUnavailable);
      f.state.errorAt = undefined;
    }
    assert.deepEqual(owner.underlay(0), underlayFor(0));
    assert.deepEqual(owner.overlay(1), overlayFor(1));
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
  } finally { owner.close(); }
});

test("native numeric endpoint values remain available without coercion or palette synthesis", async () => {
  const f = fixture({ mutateUnderlay(v) {
    Object.assign(v, { rgb: 0xffffff, texture: 65534, scale: 262140, hue: -255, saturation: 255, lightness: 255, hueMultiplier: 255, shadowed: false, occlude: false });
  }, mutateOverlay(v) {
    Object.assign(v, { slot: (255 << 8) | v.id, tileColorHsl: -1, minimapColorHsl: 65535, texture: 65534, textureScale: 262140, waterColor: 0xffffff, waterFogDepth: 8160, waterIntensity: 0, opcode20: 65535, unusedOpcode21: 255, unusedOpcode22: 65535, occlude: false, shadowed: false, blendsWithUnderlay: true });
  } });
  const owner = await retained(f);
  try {
    assert.equal(owner.underlay(0).hue, -255);
    assert.equal(owner.underlay(0).texture, 65534);
    assert.equal(owner.overlay(1).waterFogDepth, 8160);
    assert.equal(owner.overlay(1).tileColorHsl, -1);
    assert.equal(Object.hasOwn(owner.overlay(1), "rgb"), false);
    assert.equal(owner.state().renderedWorld, false);
  } finally { owner.close(); }
});

test("terrain composition accepts only packed-HSL summary metadata and returns deep isolated safe objects", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    for (const mapSize of [72, 104, 120, 136, 168]) {
      const terrain = { calls: 0, composePalette(probe) {
        assert.equal(this, terrain);
        assert.equal(probe, f.state.probe);
        this.calls++;
        const summary = paletteFor(mapSize);
        summary.private = secret;
        summary.rgb = [secret];
        return JSON.stringify(summary);
      } };
      const summary = owner.composeTerrain(terrain);
      assert.deepEqual(summary, paletteFor(mapSize));
      assert.equal(terrain.calls, 1);
      summary.unavailableDefinitionIds.fill(0);
      assert.deepEqual(owner.composeTerrain(terrain).unavailableDefinitionIds, [7, 254]);
      assert.equal(JSON.stringify(summary).includes(secret), false);
      assert.equal(Object.hasOwn(summary, "rgb"), false);
    }
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
  } finally { owner.close(); }
});

test("invalid terrain targets and malformed packed-HSL summaries cannot corrupt floor ownership", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    for (const target of [undefined, null, false, {}, { composePalette: 1 }, { get composePalette() { throw new Error(secret); } }, { composePalette() { throw new Error(secret); } }]) assert.throws(() => owner.composeTerrain(target), paletteUnavailable);
    const mutations = [
      (s) => { s.scope = "rgb"; }, (s) => { s.mapSize = "72"; }, (s) => { s.mapSize = 64; }, (s) => { s.planes = 3; },
      (s) => { s.tileCount--; }, (s) => { s.knownColors++; }, (s) => { s.knownColors = -1; }, (s) => { s.unavailableColors = "12"; },
      (s) => { s.unavailableDefinitionIds.reverse(); }, (s) => { s.unavailableDefinitionIds = [7, 7]; },
      (s) => { s.unavailableDefinitionIds = [-1]; }, (s) => { s.unavailableDefinitionIds = [255]; }, (s) => { s.unavailableDefinitionIds = [256]; },
      (s) => { s.unavailableDefinitionIds = ["7"]; }, (s) => { s.unavailableDefinitionIds = null; },
      (s) => { s.unavailableDefinitionIds = Array(256).fill(0); },
      (s) => { s.rgbPrepared = true; }, (s) => { s.renderedWorld = true; },
    ];
    for (const mutate of mutations) assert.throws(() => owner.composeTerrain({ composePalette() { const s = paletteFor(); mutate(s); return JSON.stringify(s); } }), paletteUnavailable);
    for (const value of ["{", "null", "[]", JSON.stringify(secret), "x".repeat(65537), {}, null]) assert.throws(() => owner.composeTerrain({ composePalette: () => value }), paletteUnavailable);
    const endpoints = paletteFor();
    endpoints.knownColors = endpoints.tileCount;
    endpoints.unavailableColors = 0;
    endpoints.unavailableDefinitionIds = Array.from({ length: 255 }, (_, id) => id);
    assert.deepEqual(owner.composeTerrain({ composePalette: () => JSON.stringify(endpoints) }), endpoints);
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
    assert.deepEqual(owner.underlay(0), underlayFor(0));
  } finally { owner.close(); }
});

test("abort during lookup and composition immediately revokes methods without publishing a late result", async () => {
  for (const point of ["underlay", "overlay", "compose", "getter"]) {
    const f = fixture();
    const abort = new AbortController();
    f.options.signal = abort.signal;
    const owner = await retained(f);
    if (point === "underlay" || point === "overlay") {
      f.state.hooks = { [point]: () => abort.abort(secret) };
      assert.throws(() => owner[point](point === "underlay" ? 0 : 1), closed);
    } else {
      let called = false;
      const target = point === "compose" ? { composePalette() { called = true; abort.abort(secret); return JSON.stringify(paletteFor()); } } : { get composePalette() { abort.abort(secret); return () => { called = true; return JSON.stringify(paletteFor()); }; } };
      assert.throws(() => owner.composeTerrain(target), closed);
      assert.equal(called, point === "compose");
    }
    assert.equal(owner.state().error, cancelled.message);
    assert.equal(owner.state().closed, true);
    owner.close();
    cleaned(f);
  }
});

test("abort in every exchange stage including transport closing cannot publish late", async () => {
  for (const point of ["feed", "send", "complete", "finish", "report", "close"]) {
    const f = fixture();
    const abort = new AbortController();
    const pending = openWorldFloors({ ...f.options, signal: abort.signal });
    const rejected = assert.rejects(pending, cancelled);
    await opened(f);
    f.state.hooks = { [point]: () => abort.abort(secret) };
    message(f);
    if (["finish", "report", "close"].includes(point)) message(f);
    await rejected;
    cleaned(f);
  }
});

test("the original deadline covers synchronous report validation and socket close", async (t) => {
  const clock = fakeTimers(t);
  for (const point of ["finish", "report", "close"]) {
    const f = fixture();
    const pending = openWorldFloors(f.options);
    const rejected = assert.rejects(pending, timedOut);
    await opened(f);
    message(f);
    f.state.hooks = { [point]: () => clock.advance(1000) };
    message(f);
    await rejected;
    assert.equal(clock.pending().length, 0);
    cleaned(f);
  }
});

test("retained lifetime, explicit close and post-retention abort free once and cancel timers", async (t) => {
  const clock = fakeTimers(t);
  for (const cause of ["lifetime", "close", "abort"]) {
    const f = fixture();
    const abort = new AbortController();
    f.options.signal = abort.signal;
    const owner = await retained(f);
    assert.equal(clock.pending(1000).length, 0);
    assert.equal(clock.pending(2000).length, 1);
    if (cause === "lifetime") clock.fire(clock.pending(2000)[0]);
    else if (cause === "close") owner.close();
    else abort.abort(secret);
    assert.equal(owner.state().accepted, false);
    assert.equal(owner.state().closed, true);
    assert.equal(owner.state().error, cause === "abort" ? cancelled.message : null);
    assert.equal(JSON.stringify(owner.state()).includes(secret), false);
    assert.throws(() => owner.underlay(0), closed);
    assert.throws(() => owner.overlay(1), closed);
    owner.close();
    abort.abort(secret);
    assert.equal(clock.pending().length, 0);
    cleaned(f);
  }
});

test("saved late transport callbacks cannot resurrect a failed or successfully retained owner", async () => {
  for (const success of [false, true]) {
    const f = fixture();
    const abort = new AbortController();
    const pending = openWorldFloors({ ...f.options, signal: abort.signal });
    const rejected = success ? null : assert.rejects(pending, cancelled);
    await opened(f);
    const { onopen, onmessage, onerror, onclose } = f.state.socket;
    let owner;
    if (success) {
      message(f);
      message(f);
      owner = await pending;
    } else {
      abort.abort(secret);
      await rejected;
    }
    const calls = { feeds: f.state.feeds.length, sent: f.state.sent.length, finishes: f.state.finishes, reports: f.state.reports };
    onopen();
    const late = new Uint8Array([7, 8]);
    onmessage({ data: late.buffer });
    onmessage({ get data() { throw new Error(secret); } });
    onerror(new Error(secret));
    onclose({ code: 1000, wasClean: true, reason: secret });
    assert.deepEqual([...late], [0, 0]);
    assert.deepEqual({ feeds: f.state.feeds.length, sent: f.state.sent.length, finishes: f.state.finishes, reports: f.state.reports }, calls);
    if (owner) {
      assert.equal(owner.state().accepted, true);
      assert.deepEqual(owner.underlay(0), underlayFor(0));
      owner.close();
    }
    cleaned(f);
  }
});

test("synchronously saved callbacks during transport close cannot invalidate verified completion", async () => {
  const f = fixture();
  const pending = openWorldFloors(f.options);
  await opened(f);
  const { onopen, onmessage, onerror, onclose } = f.state.socket;
  f.state.hooks = { close() {
    onopen();
    onmessage({ data: new Uint8Array([99]).buffer });
    onmessage({ get data() { throw new Error(secret); } });
    onerror();
    onclose({ code: 1000, wasClean: true });
  } };
  message(f);
  message(f);
  const owner = await pending;
  try {
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.feeds.length, 2);
  } finally { owner.close(); }
  cleaned(f);
});

test("hostile cancellation accessors after retention cannot leak thrown values through public methods", async () => {
  for (const method of ["underlay", "overlay", "composeTerrain"]) {
    const f = fixture();
    let hostile = false;
    f.options.signal = {
      get aborted() { if (hostile) throw new Error(secret); return false; },
      addEventListener() {}, removeEventListener() {},
    };
    const owner = await retained(f);
    hostile = true;
    assert.throws(() => owner[method](method === "composeTerrain" ? { composePalette: () => JSON.stringify(paletteFor()) } : 0), closed);
    assert.equal(owner.state().error, unavailable.message);
    assert.equal(JSON.stringify(owner.state()).includes(secret), false);
    owner.close();
    cleaned(f);
  }
});

test("cleanup exceptions never repeat frees or leak runtime and transport errors", async () => {
  const f = fixture({ errorAt: "close" });
  const owner = await retained(f);
  f.state.errorAt = "free";
  owner.close();
  owner.close();
  assert.equal(owner.state().closed, true);
  assert.equal(JSON.stringify(owner.state()).includes(secret), false);
  cleaned(f);
});
