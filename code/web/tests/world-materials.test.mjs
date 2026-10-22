import test from "node:test";
import assert from "node:assert/strict";
import { setImmediate } from "node:timers/promises";
import { openWorldMaterials } from "../static/world-materials.js";
import { PUBLIC_LOGIN_DEFAULTS } from "../static/login-config.js";

const MIB = 1024 * 1024;
const relay = "ws://127.0.0.1:8110/js5";
const secret = "FAKE_PRIVATE_MATERIAL_SECRET";
const unavailable = { message: "verified world material metadata unavailable" };
const cancelled = { message: "world material metadata cancelled" };
const timedOut = { message: "world material metadata timed out" };
const closed = { message: "world material metadata owner is closed" };
const materialUnavailable = { message: "world material metadata record unavailable" };
const booleanFields = ["detailsOnly", "isSmall", "skipTriangles", "aBool2087", "isBrickTile", "repeatS", "repeatT", "hdr"];
const signedByteFields = ["brightness", "alpha", "effectId", "effectParam1", "textureSpeedU", "textureSpeedV", "mipmapping"];
const numericRanges = {
  ...Object.fromEntries(signedByteFields.map((key) => [key, [-128, 127]])),
  colorHsl: [-32768, 32767], combineMode: [0, 255], effectParam2: [-0x80000000, 0x7fffffff], effectCombiner: [0, 255],
};

function population(report, slots, present) {
  report.slots = slots;
  report.present = present;
  report.asset.expandedBytes = 2 + slots + 23 * present;
  report.asset.files[0].bytes = report.asset.expandedBytes;
}

function reportFor() {
  const report = {
    scope: "material-metadata",
    asset: {
      revision: 727, subrevision: 1, prefetchCount: 27, index: 26, archive: 0,
      referenceRevision: -1234, archiveRevision: -5,
      referenceCrcVerified: true, referenceWhirlpoolVerified: true,
      archiveCrcVerified: true, archiveWhirlpoolVerified: false, authenticated: false,
      containerBytes: 96, expandedBytes: 77, fileSlots: 1, files: [{ id: 0, bytes: 77, script: null }],
    },
    slots: 6, present: 3, texturePixelsDecoded: false, renderedWorld: false,
  };
  return report;
}

function materialFor(id) {
  return {
    id, detailsOnly: true, isSmall: false, skipTriangles: true,
    brightness: -12, alpha: -128, effectId: 127, effectParam1: -3, colorHsl: -12345,
    textureSpeedU: -1, textureSpeedV: 2, aBool2087: false, isBrickTile: true,
    mipmapping: -4, repeatS: true, repeatT: false, hdr: true,
    combineMode: 255, effectParam2: -123456789, effectCombiner: 128,
  };
}

function fixture(overrides = {}) {
  const state = {
    loads: 0, created: 0, freed: 0, closed: 0, finishes: 0, reports: 0,
    phase: 0, completeAfter: 2, received: 0, feeds: [], incoming: [], outgoing: [], sent: [], materials: [], calls: {},
    ...overrides,
  };
  const fault = (point) => {
    state.calls[point] = (state.calls[point] ?? 0) + 1;
    state.hooks?.[point]?.();
    if (state.errorAt === point) throw Object.hasOwn(state, "thrown") ? state.thrown : new Error(secret);
  };
  class Probe {
    constructor(token) { state.created++; state.token = token; state.probe = this; fault("constructor"); }
    outgoing() {
      assert.equal(this, state.probe);
      fault("outgoing");
      const bytes = state.outgoingValue ? state.outgoingValue() : new Uint8Array([state.phase ? 6 : 15]);
      state.outgoing.push(bytes);
      return bytes;
    }
    feed(bytes) {
      assert.equal(this, state.probe);
      state.incoming.push(bytes);
      state.feeds.push(bytes.length <= 1024 ? [...bytes] : { bytes: bytes.length, first: bytes[0] });
      state.received += bytes.length;
      state.phase++;
      fault("feed");
    }
    complete() { assert.equal(this, state.probe); fault("complete"); return Object.hasOwn(state, "completeValue") ? state.completeValue : state.phase >= state.completeAfter; }
    finish() { assert.equal(this, state.probe); state.finishes++; fault("finish"); assert.ok(state.phase >= state.completeAfter); }
    report() {
      assert.equal(this, state.probe);
      state.reports++;
      fault("report");
      const report = reportFor();
      state.mutateReport?.(report);
      return Object.hasOwn(state, "reportText") ? state.reportText : JSON.stringify(report);
    }
    material(id) {
      assert.equal(this, state.probe);
      state.materials.push(id);
      fault("material");
      if (Object.hasOwn(state, "materialFailure")) throw state.materialFailure;
      if (!state.forceMaterial && ![0, 2, 5].includes(id)) throw `material not found: ${id}`;
      const value = materialFor(id);
      state.mutateMaterial?.(value);
      return Object.hasOwn(state, "materialText") ? state.materialText : JSON.stringify(value);
    }
    free() { assert.equal(this, state.probe); state.freed++; fault("free"); }
  }
  class Socket {
    constructor(url) { state.socket = this; state.url = url; fault("socket"); }
    send(bytes) { assert.equal(this, state.socket); state.sent.push(bytes.length <= 1024 ? [...bytes] : { length: bytes.length, first: bytes[0], last: bytes.at(-1) }); fault("send"); }
    close() { assert.equal(this, state.socket); state.closed++; fault("close"); }
  }
  const options = {
    js5Relay: relay,
    load: async () => { state.loads++; fault("load"); return Probe; },
    WebSocketClass: Socket, timeoutMs: 1000, lifetimeMs: 2000,
  };
  return { state, options, Probe, Socket };
}

async function opened(f) {
  await setImmediate();
  assert.ok(f.state.socket);
  assert.equal(typeof f.state.socket.onopen, "function");
  f.state.socket.onopen();
}

function message(f, values = [1]) {
  const data = new Uint8Array(values);
  f.state.socket.onmessage({ data: data.buffer });
  assert.ok(data.every((value) => value === 0));
}

async function retained(f) {
  const pending = openWorldMaterials(f.options);
  await opened(f);
  message(f);
  message(f);
  return pending;
}

function wiped(value) {
  if (ArrayBuffer.isView(value)) assert.ok(new Uint8Array(value.buffer, value.byteOffset, value.byteLength).every((byte) => byte === 0));
}

function cleaned(f, freed = 1) {
  assert.equal(f.state.freed, freed);
  assert.equal(f.state.closed, 1);
  for (const key of ["onopen", "onmessage", "onerror", "onclose"]) assert.equal(f.state.socket[key], null);
  for (const bytes of [...f.state.outgoing, ...f.state.incoming]) wiped(bytes);
}

async function rejectReport(overrides) {
  const f = fixture(overrides);
  const pending = openWorldMaterials(f.options);
  const rejected = assert.rejects(pending, unavailable);
  await opened(f);
  message(f);
  message(f);
  await rejected;
  cleaned(f);
}

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
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

function accessor(object, name, get) {
  const method = object[name];
  Object.defineProperty(object, name, { configurable: true, get() { return get(method); } });
}

test("verified index 26 archive 0 metadata closes JS5 while retaining the material probe until explicit close", async () => {
  const f = fixture();
  const pending = openWorldMaterials(f.options);
  await opened(f);
  assert.equal(f.state.url, relay);
  assert.equal(f.state.socket.binaryType, "arraybuffer");
  assert.equal(f.state.token, PUBLIC_LOGIN_DEFAULTS.js5AuthToken);
  assert.deepEqual(f.state.sent, [[15]]);
  message(f, [7, 8]);
  assert.equal(f.state.finishes, 0);
  message(f, [9]);
  const owner = await pending;
  try {
    assert.ok(Object.isFrozen(owner));
    assert.deepEqual(Object.keys(owner).sort(), ["close", "material", "state"]);
    assert.deepEqual(f.state.feeds, [[7, 8], [9]]);
    assert.deepEqual(f.state.sent, [[15], [6], [6]]);
    assert.equal(f.state.finishes, 1);
    assert.equal(f.state.reports, 1);
    assert.deepEqual(owner.state(), {
      accepted: true, closed: false, phase: "ready", scope: "material-metadata-only",
      authenticated: false, texturePixelsDecoded: false, renderedWorld: false,
      receivedBytes: 3, report: reportFor(), error: null,
    });
    assert.deepEqual(owner.material(0), materialFor(0));
    assert.deepEqual(owner.material(2), materialFor(2));
    cleaned(f, 0);
  } finally { owner.close(); }
  owner.close();
  assert.deepEqual(owner.state(), {
    accepted: false, closed: true, phase: "closed", scope: "material-metadata-only",
    authenticated: false, texturePixelsDecoded: false, renderedWorld: false,
    receivedBytes: 3, report: reportFor(), error: null,
  });
  assert.throws(() => owner.material(0), closed);
  assert.throws(() => owner.material("0"), closed);
  cleaned(f);
});

test("state snapshots are deep isolated and material records expose only 19 frozen native fields plus id", async () => {
  const f = fixture({
    mutateReport(r) {
      r.private = secret;
      r.totalWireBytes = secret;
      r.limits = { secret };
      r.asset.private = secret;
      r.asset.files[0].private = secret;
    },
    mutateMaterial(v) { v.private = secret; v.texturePixels = [secret]; v.renderedWorld = true; v.__proto__ = { private: secret }; },
  });
  const owner = await retained(f);
  try {
    const state = owner.state();
    assert.deepEqual(state.report, reportFor());
    state.accepted = false;
    state.report.asset.index = 0;
    state.report.asset.files[0].bytes = 0;
    state.report.asset.files.push({ private: secret });
    state.report.slots = 0;
    const material = owner.material(0);
    assert.ok(Object.isFrozen(material));
    assert.equal(Object.keys(material).length, 20);
    assert.deepEqual(material, materialFor(0));
    assert.throws(() => { material.alpha = 0; }, TypeError);
    assert.throws(() => { material.private = secret; }, TypeError);
    assert.equal(owner.state().accepted, true);
    assert.deepEqual(owner.state().report, reportFor());
    assert.notEqual(owner.state(), owner.state());
    assert.notEqual(owner.material(0), material);
    assert.equal(JSON.stringify([owner.state(), material, owner]).includes(secret), false);
    assert.equal(Object.hasOwn(owner.state().report, "totalWireBytes"), false);
    assert.equal(Object.hasOwn(material, "texturePixels"), false);
  } finally { owner.close(); }
  assert.equal(JSON.stringify(owner.state()).includes(secret), false);
  cleaned(f);
});

test("only exact literal loopback JS5 routes can load a runtime or allocate resources", async () => {
  const invalid = [
    undefined, null, 123, true, {}, new String(relay), "/js5", "ws://example.com/js5", "ws://localhost.evil/js5", "ws://127.0.0.1.evil/js5",
    "http://localhost/js5", "https://localhost/js5", "ws://localhost/world", "ws://localhost/js5/", "ws://localhost/JS5",
    "ws://localhost/js5?upstream=private", "ws://localhost/js5?", "ws://localhost/js5#", "ws://localhost/js5#private",
    "ws://user@localhost/js5", "ws://user:password@localhost/js5", "ws://127.1/js5", "ws://2130706433/js5", "ws://0x7f000001/js5", "ws://127.000.000.001/js5",
    "ws://LOCALHOST/js5", "ws://localhost./js5", "ws://[::ffff:127.0.0.1]/js5", "ws://localhost/x/../js5", "ws://localhost/%6as5",
    " ws://localhost/js5", "ws://localhost/js5\n", "ws://localhost:0/js5", "ws://localhost:00000/js5", "ws://localhost:65536/js5", "ws://localhost:123456/js5",
    "ws://localhost:8110\\js5", `ws://localhost/${"x".repeat(512)}/js5`,
  ];
  for (const js5Relay of invalid) {
    const f = fixture();
    await assert.rejects(openWorldMaterials({ ...f.options, js5Relay }), unavailable);
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.socket, undefined);
  }
  for (const js5Relay of ["ws://localhost/js5", "wss://localhost:8110/js5", "ws://[::1]:8110/js5", "wss://[::1]/js5", "wss://127.0.0.1/js5", "ws://127.0.0.1:65535/js5"]) {
    const f = fixture();
    f.options.js5Relay = js5Relay;
    const owner = await retained(f);
    try { assert.equal(f.state.url, new URL(js5Relay).href); }
    finally { owner.close(); }
    cleaned(f);
  }
});

test("options and injected lifetimes are noncoercing and bounded before any runtime load", async () => {
  const coercible = { valueOf() { assert.fail("must not coerce options"); }, toString() { assert.fail("must not stringify options"); } };
  for (const [key, values] of Object.entries({
    timeoutMs: [0, -1, 30001, Infinity, NaN, "100", 1.5, null, 1n, true, coercible],
    lifetimeMs: [0, -1, 300001, Infinity, NaN, "100", 1.5, null, 1n, false, coercible],
    load: [null, {}, false, "private"], WebSocketClass: [null, {}, false, "private"], js5Relay: [coercible],
  })) for (const value of values) {
    const f = fixture();
    await assert.rejects(openWorldMaterials({ ...f.options, [key]: value }), unavailable);
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.socket, undefined);
  }
  for (const options of [null, false, [], "private", 1, Object.create({ js5Relay: relay }), { get js5Relay() { throw new Error(secret); } }]) await assert.rejects(openWorldMaterials(options), unavailable);
  const f = fixture();
  f.options = Object.assign(Object.create(null), f.options, { private: secret });
  const owner = await retained(f);
  owner.close();
  cleaned(f);
});

test("inclusive timeout and lifetime endpoints are accepted and exchange timers are replaced only at retention", async (t) => {
  const clock = fakeTimers(t);
  for (const [timeoutMs, lifetimeMs] of [[1, 1], [30000, 300000]]) {
    const f = fixture();
    Object.assign(f.options, { timeoutMs, lifetimeMs });
    const pending = openWorldMaterials(f.options);
    assert.equal(clock.pending(timeoutMs).length, 1);
    await opened(f);
    message(f);
    message(f);
    const owner = await pending;
    assert.equal(clock.pending().length, 1);
    assert.equal(clock.pending()[0].delay, lifetimeMs);
    owner.close();
    assert.equal(clock.pending().length, 0);
    cleaned(f);
  }
});

test("pre-abort and same-turn abort reject without ever loading the runtime", async () => {
  for (const preAborted of [true, false]) {
    const f = fixture();
    const abort = new AbortController();
    if (preAborted) abort.abort(secret);
    const pending = openWorldMaterials({ ...f.options, signal: abort.signal });
    const rejected = assert.rejects(pending, cancelled);
    if (!preAborted) abort.abort(secret);
    await rejected;
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.socket, undefined);
  }
});

test("invalid signal shapes and subscription failures expose only a static error and allocate nothing", async () => {
  const signals = [
    false, {}, { aborted: secret }, { aborted: false, addEventListener: 1, removeEventListener() {} },
    { aborted: false, addEventListener() {}, removeEventListener: null },
    { get aborted() { throw new Error(secret); } },
    { aborted: false, get addEventListener() { throw new Error(secret); } },
    { aborted: false, addEventListener() { throw new Error(secret); }, removeEventListener() { throw new Error(secret); } },
  ];
  for (const signal of signals) {
    const f = fixture();
    await assert.rejects(openWorldMaterials({ ...f.options, signal }), unavailable);
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.socket, undefined);
  }
});

test("ignored loader cancellation rejects immediately and discards late fulfillment, rejection, or a never-settled load", async () => {
  for (const result of ["resolve", "reject", "never"]) {
    const f = fixture();
    const loading = deferred();
    const abort = new AbortController();
    f.options.load = () => loading.promise;
    const pending = openWorldMaterials({ ...f.options, signal: abort.signal });
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

test("a never-settling loader shares the bounded exchange deadline and late loader results are ignored", async (t) => {
  const clock = fakeTimers(t);
  for (const result of ["resolve", "reject", "never"]) {
    const f = fixture();
    const loading = deferred();
    f.options.load = () => loading.promise;
    const pending = openWorldMaterials(f.options);
    const rejected = assert.rejects(pending, timedOut);
    await setImmediate();
    clock.fire(clock.pending(1000)[0]);
    await rejected;
    if (result !== "never") loading[result](result === "resolve" ? f.Probe : new Error(secret));
    await setImmediate();
    assert.equal(f.state.created, 0);
    assert.equal(f.state.freed, 0);
    assert.equal(f.state.socket, undefined);
    assert.equal(clock.pending().length, 0);
  }
});

test("loader time is never reset on socket creation and synchronous loading cannot exceed the original deadline", async (t) => {
  const clock = fakeTimers(t);
  const f = fixture();
  const loading = deferred();
  f.options.load = () => loading.promise;
  const pending = openWorldMaterials(f.options);
  const rejected = assert.rejects(pending, timedOut);
  await setImmediate();
  const deadline = clock.pending(1000)[0];
  clock.advance(999);
  loading.resolve(f.Probe);
  await opened(f);
  assert.deepEqual(clock.pending(), [deadline]);
  clock.advance(1);
  message(f);
  await rejected;
  assert.equal(f.state.feeds.length, 0);
  cleaned(f);
  const next = fixture();
  next.options.load = () => { clock.advance(1000); return next.Probe; };
  await assert.rejects(openWorldMaterials(next.options), timedOut);
  assert.equal(next.state.created, 0);
  assert.equal(next.state.socket, undefined);
  assert.equal(clock.pending().length, 0);
});

test("synchronous probe construction, startup getters and socket construction all consume the same deadline", async (t) => {
  const clock = fakeTimers(t);
  for (const point of ["constructor", "getter", "socket"]) {
    const f = fixture();
    if (point === "getter") accessor(f.Probe.prototype, "material", (method) => { clock.advance(1000); return method; });
    else f.state.hooks = { [point]: () => clock.advance(1000) };
    await assert.rejects(openWorldMaterials(f.options), timedOut);
    assert.equal(f.state.created, 1);
    assert.equal(f.state.freed, 1);
    if (point === "socket") cleaned(f);
    else assert.equal(f.state.socket, undefined);
    assert.equal(clock.pending().length, 0);
  }
});

test("loader failures, nonconstructible exports and throwing constructors never reveal runtime secrets", async () => {
  for (const load of [
    () => { throw new Error(secret); }, async () => { throw secret; },
    async () => undefined, async () => null, async () => ({ private: secret }), async () => (() => {}),
  ]) {
    const f = fixture();
    await assert.rejects(openWorldMaterials({ ...f.options, load }), unavailable);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.socket, undefined);
  }
  const f = fixture({ errorAt: "constructor" });
  await assert.rejects(openWorldMaterials(f.options), unavailable);
  assert.equal(f.state.created, 1);
  assert.equal(f.state.socket, undefined);
});

test("all required probe methods are validated before socket allocation including material and free", async () => {
  for (const method of ["outgoing", "feed", "complete", "finish", "report", "material", "free"]) {
    for (const throwing of [false, true]) {
      const f = fixture();
      if (throwing) accessor(f.Probe.prototype, method, () => { throw new Error(secret); });
      else f.Probe.prototype[method] = null;
      await assert.rejects(openWorldMaterials(f.options), unavailable);
      assert.equal(f.state.created, 1);
      assert.equal(f.state.freed, method === "free" ? 0 : 1);
      assert.equal(f.state.socket, undefined);
    }
  }
});

test("abort during loading or constructors frees each returned allocation once without publishing", async () => {
  for (const point of ["load", "constructor", "socket"]) {
    const abort = new AbortController();
    const f = fixture({ hooks: { [point]: () => abort.abort(secret) } });
    await assert.rejects(openWorldMaterials({ ...f.options, signal: abort.signal }), cancelled);
    assert.equal(f.state.created, point === "load" ? 0 : 1);
    assert.equal(f.state.freed, point === "load" ? 0 : 1);
    if (point === "socket") cleaned(f);
    else assert.equal(f.state.socket, undefined);
  }
  const f = fixture();
  f.options.WebSocketClass = class { constructor() { throw new Error(secret); } };
  await assert.rejects(openWorldMaterials(f.options), unavailable);
  assert.equal(f.state.freed, 1);
});

test("reentrant abort from every startup method getter stops validation before creating a socket", async () => {
  for (const name of ["outgoing", "feed", "complete", "finish", "report", "material", "free"]) {
    const f = fixture();
    const abort = new AbortController();
    accessor(f.Probe.prototype, name, (method) => { abort.abort(secret); return method; });
    await assert.rejects(openWorldMaterials({ ...f.options, signal: abort.signal }), cancelled);
    assert.equal(f.state.created, 1);
    assert.equal(f.state.freed, 1);
    assert.equal(f.state.socket, undefined);
    assert.equal(f.state.calls[name] ?? 0, name === "free" ? 1 : 0);
  }
});

test("socket binaryType setter cancellation cannot install callbacks or leak the returned transport", async () => {
  const f = fixture();
  const abort = new AbortController();
  Object.defineProperty(f.Socket.prototype, "binaryType", { set() { abort.abort(secret); } });
  await assert.rejects(openWorldMaterials({ ...f.options, signal: abort.signal }), cancelled);
  cleaned(f);
});

test("abort listeners remain through retention and are removed on success close and all pending terminal paths", async () => {
  for (const outcome of ["success", "abort", "error", "report"]) {
    const abort = new AbortController();
    const listeners = new Set();
    const add = abort.signal.addEventListener.bind(abort.signal);
    const remove = abort.signal.removeEventListener.bind(abort.signal);
    abort.signal.addEventListener = (kind, listener, options) => { assert.equal(kind, "abort"); assert.deepEqual(options, { once: true }); listeners.add(listener); add(kind, listener, options); };
    abort.signal.removeEventListener = (kind, listener) => { assert.equal(kind, "abort"); listeners.delete(listener); remove(kind, listener); };
    const f = fixture();
    const pending = openWorldMaterials({ ...f.options, signal: abort.signal });
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
      else if (outcome === "error") f.state.socket.onerror(new Error(secret));
      else { f.state.reportText = secret; message(f); message(f); }
      await rejected;
    }
    assert.equal(listeners.size, 0);
    cleaned(f);
  }
});

test("clean EOF, abnormal close and socket errors before completion never finish or publish", async () => {
  for (const event of ["normal", "abnormal", "error"]) {
    const f = fixture();
    const pending = openWorldMaterials(f.options);
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

test("malformed message payloads and throwing data accessors reject statically and wipe rejected byte views", async () => {
  for (const data of [
    secret, new Uint8Array(4 * MIB + 1).fill(7).buffer,
    new Uint8Array([7, 8]), new Uint16Array([700, 800]), new DataView(new Uint8Array([7, 8]).buffer),
    null, undefined, new Blob([secret]), {},
  ]) {
    const f = fixture();
    const pending = openWorldMaterials(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    f.state.socket.onmessage({ data });
    await rejected;
    if (data instanceof ArrayBuffer) assert.ok(new Uint8Array(data).every((value) => value === 0));
    wiped(data);
    assert.deepEqual(f.state.feeds, []);
    cleaned(f);
  }
  const f = fixture();
  const pending = openWorldMaterials(f.options);
  const rejected = assert.rejects(pending, unavailable);
  await opened(f);
  f.state.socket.onmessage({ get data() { throw new Error(secret); } });
  await rejected;
  cleaned(f);
});

test("a data getter that aborts cannot feed its returned buffer and still has that buffer wiped", async () => {
  const f = fixture();
  const abort = new AbortController();
  const pending = openWorldMaterials({ ...f.options, signal: abort.signal });
  const rejected = assert.rejects(pending, cancelled);
  await opened(f);
  const data = new Uint8Array([71, 81]);
  f.state.socket.onmessage({ get data() { abort.abort(secret); return data.buffer; } });
  await rejected;
  assert.deepEqual([...data], [0, 0]);
  assert.deepEqual(f.state.feeds, []);
  assert.equal(f.state.finishes, 0);
  cleaned(f);
});

test("inclusive 4 MiB messages can reach exactly 12 MiB cumulative bytes without trusting a report wire counter", async (t) => {
  fakeTimers(t);
  const f = fixture({ completeAfter: 3 });
  const pending = openWorldMaterials(f.options);
  await opened(f);
  for (let at = 0; at < 3; at++) {
    const bytes = new Uint8Array(4 * MIB).fill(7);
    f.state.socket.onmessage({ data: bytes.buffer });
    assert.ok(bytes.every((value) => value === 0));
  }
  const owner = await pending;
  try {
    assert.equal(owner.state().receivedBytes, 12 * MIB);
    assert.equal(f.state.received, 12 * MIB);
    assert.equal(f.state.feeds.length, 3);
    assert.equal(Object.hasOwn(owner.state().report, "totalWireBytes"), false);
  } finally { owner.close(); }
  cleaned(f);
});

test("one byte above the 12 MiB cumulative limit is wiped and rejected before entering WASM", async (t) => {
  fakeTimers(t);
  const f = fixture({ completeAfter: Infinity });
  const pending = openWorldMaterials(f.options);
  const rejected = assert.rejects(pending, unavailable);
  await opened(f);
  for (let at = 0; at < 3; at++) message(f, new Uint8Array(4 * MIB).fill(7));
  assert.equal(f.state.received, 12 * MIB);
  message(f, [99]);
  await rejected;
  assert.equal(f.state.received, 12 * MIB);
  assert.equal(f.state.feeds.length, 3);
  assert.equal(f.state.finishes, 0);
  cleaned(f);
});

test("outgoing messages reject non-Uint8Array or oversized values and wipe all returned byte views", async () => {
  for (const outgoingValue of [
    () => new Uint8Array(4 * MIB + 1).fill(7), () => new Uint16Array([700, 800]),
    () => new DataView(new Uint8Array([7, 8]).buffer), () => new Uint8Array([7]).buffer,
    () => secret, () => null, () => undefined, () => [7, 8],
  ]) {
    const f = fixture({ outgoingValue });
    const pending = openWorldMaterials(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    await rejected;
    assert.deepEqual(f.state.sent, []);
    cleaned(f);
  }
});

test("empty outgoing messages are skipped and an inclusive 4 MiB outgoing message is sent then wiped", async (t) => {
  fakeTimers(t);
  for (const size of [0, 4 * MIB]) {
    const f = fixture({ outgoingValue: () => new Uint8Array(size).fill(7) });
    const pending = openWorldMaterials(f.options);
    await opened(f);
    assert.equal(f.state.sent.length, size === 0 ? 0 : 1);
    if (size) assert.equal(f.state.sent[0].length, size);
    message(f);
    message(f);
    const owner = await pending;
    owner.close();
    cleaned(f);
  }
});

test("outgoing subarray wiping affects only the returned bytes even when transport send throws", async () => {
  for (const fail of [false, true]) {
    const backing = new Uint8Array([91, 7, 8, 92]);
    const f = fixture({ outgoingValue: () => backing.subarray(1, 3), errorAt: fail ? "send" : undefined });
    const pending = openWorldMaterials(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    assert.deepEqual([...backing], [91, 0, 0, 92]);
    assert.deepEqual(f.state.sent, [[7, 8]]);
    if (!fail) f.state.socket.onerror(new Error(secret));
    await rejected;
    cleaned(f);
  }
});

test("incoming and outgoing aliases are wiped on feed failure, send failure and reentrant abort", async () => {
  for (const point of ["feed", "send", "outgoing"]) {
    for (const aborting of [false, true]) {
      const abort = new AbortController();
      const f = fixture();
      const pending = openWorldMaterials({ ...f.options, signal: abort.signal });
      const rejected = assert.rejects(pending, aborting ? cancelled : unavailable);
      await opened(f);
      if (aborting) f.state.hooks = { [point]: () => abort.abort(secret) };
      else f.state.errorAt = point;
      message(f, [77, 88]);
      await rejected;
      assert.equal(f.state.finishes, 0);
      cleaned(f);
    }
  }
});

test("completion must be a strict boolean before finish or report can run", async () => {
  for (const completeValue of [1, 0, "true", null, {}, [], undefined, 1n]) {
    const f = fixture({ completeValue });
    const pending = openWorldMaterials(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    message(f);
    await rejected;
    assert.equal(f.state.finishes, 0);
    assert.equal(f.state.reports, 0);
    cleaned(f);
  }
});

test("corruption and exceptions at every exchange stage produce static failures and release owned resources", async () => {
  for (const errorAt of ["outgoing", "send", "feed", "complete", "finish", "report"]) {
    const f = fixture({ errorAt });
    const pending = openWorldMaterials(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    if (!["outgoing", "send"].includes(errorAt)) message(f);
    if (["finish", "report"].includes(errorAt)) message(f);
    await rejected;
    cleaned(f);
  }
  for (const thrown of [secret, new Error(secret), { message: secret }, { toString() { assert.fail("must not stringify corruption"); } }]) {
    const f = fixture({ errorAt: "feed", thrown });
    const pending = openWorldMaterials(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    message(f, [0xff, 0, 0xff]);
    await rejected;
    assert.equal(f.state.finishes, 0);
    assert.equal(f.state.reports, 0);
    cleaned(f);
  }
});

test("report parsing accepts only bounded JSON object strings and never publishes malformed reports", async () => {
  for (const reportText of ["{", JSON.stringify(secret), "null", "[]", "true", "1", "x".repeat(65537), 7, {}, null, undefined, new String(JSON.stringify(reportFor()))]) await rejectReport({ reportText });
  for (const mutateReport of [
    (r) => { r.asset = []; }, (r) => { r.asset = null; }, (r) => { r.asset = secret; },
    (r) => { r.asset.files = {}; }, (r) => { r.asset.files = null; }, (r) => { r.asset.files = []; },
    (r) => { r.asset.files.push({ ...r.asset.files[0] }); }, (r) => { r.asset.files[0] = []; }, (r) => { r.asset.files[0] = null; },
  ]) await rejectReport({ mutateReport });
});

test("report JSON length is inclusive at 64 KiB and unknown metadata is ignored rather than exported", async () => {
  const report = reportFor();
  report.private = "";
  const length = JSON.stringify(report).length;
  report.private = "x".repeat(65536 - length);
  const text = JSON.stringify(report);
  assert.equal(text.length, 65536);
  const f = fixture({ reportText: text });
  const owner = await retained(f);
  try { assert.deepEqual(owner.state().report, reportFor()); }
  finally { owner.close(); }
  await rejectReport({ reportText: `${text} ` });
});

test("all fixed 727 material identities, verification claims and metadata-only scope flags are mandatory", async () => {
  const identities = { revision: 727, subrevision: 1, prefetchCount: 27, index: 26, archive: 0, fileSlots: 1 };
  for (const [key, expected] of Object.entries(identities)) {
    for (const value of [expected + 1, String(expected), null]) await rejectReport({ mutateReport(r) { r.asset[key] = value; } });
    await rejectReport({ mutateReport(r) { delete r.asset[key]; } });
  }
  for (const key of ["referenceCrcVerified", "referenceWhirlpoolVerified", "archiveCrcVerified"]) {
    for (const value of [false, "true", 1, null]) await rejectReport({ mutateReport(r) { r.asset[key] = value; } });
    await rejectReport({ mutateReport(r) { delete r.asset[key]; } });
  }
  for (const key of ["texturePixelsDecoded", "renderedWorld"]) {
    for (const value of [true, "false", 0, null]) await rejectReport({ mutateReport(r) { r[key] = value; } });
    await rejectReport({ mutateReport(r) { delete r[key]; } });
  }
  for (const value of [true, "false", 0, null]) await rejectReport({ mutateReport(r) { r.asset.authenticated = value; } });
  await rejectReport({ mutateReport(r) { delete r.asset.authenticated; } });
  for (const value of ["world-materials", "material-metadata-only", "floor-definitions", null]) await rejectReport({ mutateReport(r) { r.scope = value; } });
  await rejectReport({ mutateReport(r) { delete r.scope; } });
  for (const value of [1, "0", null]) await rejectReport({ mutateReport(r) { r.asset.files[0].id = value; } });
  for (const value of [false, 0, "null", {}, secret]) await rejectReport({ mutateReport(r) { r.asset.files[0].script = value; } });
  for (const key of ["id", "script"]) await rejectReport({ mutateReport(r) { delete r.asset.files[0][key]; } });
});

test("archive Whirlpool verification is a strict boolean rather than a mandatory true or an untyped claim", async () => {
  for (const value of [undefined, null, 0, 1, "false", "true", {}]) await rejectReport({ mutateReport(r) { r.asset.archiveWhirlpoolVerified = value; } });
  for (const value of [false, true]) {
    const f = fixture({ mutateReport(r) { r.asset.archiveWhirlpoolVerified = value; } });
    const owner = await retained(f);
    try { assert.equal(owner.state().report.asset.archiveWhirlpoolVerified, value); }
    finally { owner.close(); }
  }
});

test("report counters and signed revisions reject coercion, missing values and out-of-range native numbers", async () => {
  for (const key of ["referenceRevision", "archiveRevision", "containerBytes", "expandedBytes"]) {
    for (const value of ["0", null, 1.5, NaN, Infinity, {}, true]) await rejectReport({ mutateReport(r) { r.asset[key] = value; } });
    await rejectReport({ mutateReport(r) { delete r.asset[key]; } });
  }
  for (const key of ["referenceRevision", "archiveRevision"]) {
    for (const value of [-0x80000001, 0x80000000]) await rejectReport({ mutateReport(r) { r.asset[key] = value; } });
  }
  for (const key of ["slots", "present"]) {
    for (const value of ["0", null, -1, 1.5, 65536, NaN, Infinity, false, {}]) await rejectReport({ mutateReport(r) { r[key] = value; } });
    await rejectReport({ mutateReport(r) { delete r[key]; } });
  }
  for (const value of ["77", null, 1.5, NaN, Infinity, true]) await rejectReport({ mutateReport(r) { r.asset.files[0].bytes = value; } });
  await rejectReport({ mutateReport(r) { delete r.asset.files[0].bytes; } });
});

test("container and expanded limits and exact material byte accounting reject impossible or inflated reports", async () => {
  for (const mutateReport of [
    (r) => { r.asset.containerBytes = 4; }, (r) => { r.asset.containerBytes = 4 * MIB + 1; },
    (r) => { r.asset.expandedBytes = 1; }, (r) => { r.asset.expandedBytes = 8 * MIB + 1; },
    (r) => { r.present = r.slots + 1; },
    (r) => { r.asset.files[0].bytes--; }, (r) => { r.asset.files[0].bytes++; },
    (r) => { r.asset.expandedBytes++; }, (r) => { r.asset.expandedBytes--; },
    (r) => { r.asset.files[0].bytes = 2 + 65535 * 24 + 1; r.asset.expandedBytes = r.asset.files[0].bytes; },
    (r) => { population(r, 0, 0); r.asset.files[0].bytes = 0; r.asset.expandedBytes = 0; },
    (r) => { population(r, 6, 3); r.asset.files[0].bytes += 2; r.asset.expandedBytes += 2; },
    (r) => { population(r, 65535, 65535); r.asset.expandedBytes = 8 * MIB; r.asset.files[0].bytes = 8 * MIB; },
  ]) await rejectReport({ mutateReport });
});

test("empty, all-sparse, dense and maximum-slot reports preserve the exact 2 + slots + 23 * present accounting", async () => {
  for (const [slots, present] of [[0, 0], [6, 0], [6, 6], [65535, 0], [65535, 65535]]) {
    const f = fixture({ mutateReport(r) { population(r, slots, present); }, forceMaterial: true });
    const owner = await retained(f);
    try {
      assert.equal(owner.state().report.slots, slots);
      assert.equal(owner.state().report.present, present);
      assert.equal(owner.state().report.asset.files[0].bytes, 2 + slots + 23 * present);
      assert.equal(owner.state().report.asset.expandedBytes, 2 + slots + 23 * present);
      assert.equal(owner.material(slots), null);
      assert.equal(owner.material(65535), null);
      assert.deepEqual(f.state.materials, []);
      if (present) assert.deepEqual(owner.material(slots - 1), materialFor(slots - 1));
      else if (slots) {
        f.state.materialFailure = "material not found: 0";
        assert.equal(owner.material(0), null);
      }
    } finally { owner.close(); }
    cleaned(f);
  }
});

test("signed revision endpoints and maximum container bytes are accepted without inventing wire totals", async () => {
  for (const [referenceRevision, archiveRevision] of [[-0x80000000, 0x7fffffff], [0x7fffffff, -0x80000000]]) {
    const f = fixture({ mutateReport(r) { Object.assign(r.asset, { referenceRevision, archiveRevision, containerBytes: 4 * MIB }); } });
    const owner = await retained(f);
    try {
      assert.equal(owner.state().report.asset.referenceRevision, referenceRevision);
      assert.equal(owner.state().report.asset.archiveRevision, archiveRevision);
      assert.equal(owner.state().report.asset.containerBytes, 4 * MIB);
      assert.equal(owner.state().receivedBytes, 2);
    } finally { owner.close(); }
  }
  const f = fixture({ mutateReport(r) { population(r, 0, 0); r.asset.containerBytes = 5; } });
  const owner = await retained(f);
  owner.close();
});

test("invalid material IDs never coerce or reach WASM and valid out-of-slot IDs return null locally", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    const coercible = { valueOf() { assert.fail("must not coerce material IDs"); }, toString() { assert.fail("must not stringify material IDs"); } };
    for (const id of [undefined, null, NaN, Infinity, -Infinity, -1, 65536, 1.5, "0", true, false, 1n, Symbol("private"), coercible, new Number(0), []]) assert.throws(() => owner.material(id), { message: "invalid world material id" });
    assert.equal(owner.material(6), null);
    assert.equal(owner.material(65535), null);
    assert.deepEqual(f.state.materials, []);
    assert.deepEqual(owner.material(-0), materialFor(0));
    assert.equal(Object.is(owner.material(-0).id, 0), true);
    assert.deepEqual(owner.material(5), materialFor(5));
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
  } finally { owner.close(); }
});

test("only an exact raw material-not-found string means a sparse slot is absent", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    assert.equal(owner.material(1), null);
    for (const error of [
      new Error("material not found: 1"), { message: "material not found: 1" }, new String("material not found: 1"),
      "material not found: 0", "material not found: 01", "material not found: 1\n", "Material not found: 1", secret, undefined, null,
      { toString() { assert.fail("must not coerce missing exceptions"); } },
    ]) {
      f.state.materialFailure = error;
      assert.throws(() => owner.material(1), materialUnavailable);
    }
    delete f.state.materialFailure;
    assert.deepEqual(owner.material(0), materialFor(0));
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
  } finally { owner.close(); }
});

test("dense reports cannot claim missing slots and zero-present reports cannot return fabricated records", async () => {
  for (const present of [0, 6]) {
    const f = fixture({ mutateReport(r) { population(r, 6, present); }, forceMaterial: true });
    const owner = await retained(f);
    try {
      if (present === 0) assert.throws(() => owner.material(0), materialUnavailable);
      else assert.deepEqual(owner.material(0), materialFor(0));
      f.state.materialFailure = "material not found: 0";
      if (present === 0) assert.equal(owner.material(0), null);
      else assert.throws(() => owner.material(0), materialUnavailable);
      assert.equal(owner.state().accepted, true);
      assert.equal(f.state.freed, 0);
    } finally { owner.close(); }
  }
});

test("material JSON and identity failures remain static and do not destroy retained ownership", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    for (const materialText of ["{", JSON.stringify(secret), "null", "[]", "true", "x".repeat(65537), 7, {}, null, undefined, new String(JSON.stringify(materialFor(0)))]) {
      f.state.materialText = materialText;
      assert.throws(() => owner.material(0), materialUnavailable);
    }
    delete f.state.materialText;
    for (const id of [1, "0", null, undefined, false]) {
      f.state.mutateMaterial = (v) => { v.id = id; };
      assert.throws(() => owner.material(0), materialUnavailable);
    }
    f.state.mutateMaterial = undefined;
    f.state.errorAt = "material";
    assert.throws(() => owner.material(0), materialUnavailable);
    f.state.errorAt = undefined;
    assert.deepEqual(owner.material(0), materialFor(0));
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
  } finally { owner.close(); }
});

test("material JSON accepts an inclusive 64 KiB allowlisted record but rejects one extra byte", async () => {
  const value = { ...materialFor(0), private: "" };
  value.private = "x".repeat(65536 - JSON.stringify(value).length);
  const materialText = JSON.stringify(value);
  assert.equal(materialText.length, 65536);
  const f = fixture({ materialText });
  const owner = await retained(f);
  try {
    assert.deepEqual(owner.material(0), materialFor(0));
    f.state.materialText += " ";
    assert.throws(() => owner.material(0), materialUnavailable);
    assert.equal(owner.state().accepted, true);
  } finally { owner.close(); }
});

test("every material boolean is strict, mandatory and independently validated", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    for (const key of booleanFields) {
      for (const value of [0, 1, "true", "false", null, {}, undefined]) {
        f.state.mutateMaterial = (v) => { v[key] = value; };
        assert.throws(() => owner.material(0), materialUnavailable, key);
      }
      for (const value of [false, true]) {
        f.state.mutateMaterial = (v) => { v[key] = value; };
        assert.equal(owner.material(0)[key], value);
      }
    }
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
  } finally { owner.close(); }
});

test("every numeric material field rejects coercion, fractions and values outside its true native signed or unsigned range", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    for (const [key, [min, max]] of Object.entries(numericRanges)) {
      for (const value of [min - 1, max + 1, "0", null, 0.5, NaN, Infinity, -Infinity, true, {}, [], undefined]) {
        f.state.mutateMaterial = (v) => { v[key] = value; };
        assert.throws(() => owner.material(0), materialUnavailable, `${key}: ${String(value)}`);
      }
    }
    f.state.mutateMaterial = undefined;
    assert.deepEqual(owner.material(0), materialFor(0));
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
  } finally { owner.close(); }
});

test("i8, i16 and i32 signed endpoints survive exactly rather than being accepted as unsigned bit patterns", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    for (const endpoint of [0, 1]) {
      f.state.mutateMaterial = (v) => {
        for (const [key, bounds] of Object.entries(numericRanges)) v[key] = bounds[endpoint];
      };
      const material = owner.material(0);
      for (const [key, bounds] of Object.entries(numericRanges)) assert.equal(material[key], bounds[endpoint], key);
      assert.ok(Object.isFrozen(material));
    }
    for (const [key, value] of [...signedByteFields.map((key) => [key, 255]), ["colorHsl", 65535], ["effectParam2", 0xffffffff]]) {
      f.state.mutateMaterial = (v) => { v[key] = value; };
      assert.throws(() => owner.material(0), materialUnavailable);
    }
    assert.equal(owner.state().texturePixelsDecoded, false);
    assert.equal(owner.state().renderedWorld, false);
  } finally { owner.close(); }
});

test("all material fields must be own JSON values and unknown prototype-shaped keys cannot escape the allowlist", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    for (const key of Object.keys(materialFor(0))) {
      f.state.mutateMaterial = (v) => { delete v[key]; };
      assert.throws(() => owner.material(0), materialUnavailable, key);
    }
    f.state.mutateMaterial = undefined;
    f.state.materialText = JSON.stringify({ ...materialFor(0), ["__proto__"]: { private: secret }, constructor: secret, prototype: secret });
    assert.deepEqual(owner.material(0), materialFor(0));
    assert.equal(Object.getPrototypeOf(owner.material(0)), Object.prototype);
    assert.equal(Object.hasOwn(owner.material(0), "__proto__"), false);
    assert.equal(Object.hasOwn(owner.material(0), "constructor"), false);
    assert.equal(JSON.stringify(owner.material(0)).includes(secret), false);
  } finally { owner.close(); }
});

test("missing, throwing and nonfunction retained material accessors cannot leak private failures or free the owner", async () => {
  for (const replacement of ["missing", "throwing", "nonfunction"]) {
    const f = fixture();
    const owner = await retained(f);
    try {
      const original = f.Probe.prototype.material;
      if (replacement === "throwing") accessor(f.state.probe, "material", () => { throw new Error(secret); });
      else Object.defineProperty(f.state.probe, "material", { configurable: true, value: replacement === "missing" ? undefined : secret });
      assert.throws(() => owner.material(0), materialUnavailable);
      assert.equal(owner.state().accepted, true);
      assert.equal(f.state.freed, 0);
      Object.defineProperty(f.state.probe, "material", { configurable: true, value: original });
      assert.deepEqual(owner.material(0), materialFor(0));
    } finally { owner.close(); }
    cleaned(f);
  }
});

test("a material getter throwing the exact missing string is unavailable rather than a sparse native absence", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    accessor(f.state.probe, "material", () => { throw "material not found: 1"; });
    assert.throws(() => owner.material(1), materialUnavailable);
    assert.deepEqual(f.state.materials, []);
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
  } finally { owner.close(); }
  cleaned(f);
});

test("retained material getters are guarded before and after accessor execution and never call a method after reentrant close", async () => {
  for (const outcome of ["return", "throw"]) {
    const f = fixture();
    const owner = await retained(f);
    let reads = 0;
    let calls = 0;
    accessor(f.state.probe, "material", () => {
      reads++;
      owner.close();
      if (outcome === "throw") throw new Error(secret);
      return () => { calls++; return JSON.stringify(materialFor(0)); };
    });
    assert.throws(() => owner.material(0), closed);
    assert.equal(reads, 1);
    assert.equal(calls, 0);
    assert.throws(() => owner.material(0), closed);
    assert.equal(reads, 1);
    assert.equal(owner.state().accepted, false);
    assert.equal(owner.state().error, null);
    owner.close();
    cleaned(f);
  }
});

test("reentrant material calls that close or abort cannot return a late record or reinterpret a late missing exception", async () => {
  for (const cause of ["close", "abort"]) {
    for (const result of ["record", "missing", "throw"]) {
      const f = fixture();
      const abort = new AbortController();
      f.options.signal = abort.signal;
      const owner = await retained(f);
      f.state.probe.material = () => {
        if (cause === "close") owner.close();
        else abort.abort(secret);
        if (result === "missing") throw "material not found: 0";
        if (result === "throw") throw new Error(secret);
        return JSON.stringify(materialFor(0));
      };
      assert.throws(() => owner.material(0), closed);
      assert.equal(owner.state().error, cause === "abort" ? cancelled.message : null);
      assert.equal(JSON.stringify(owner.state()).includes(secret), false);
      owner.close();
      cleaned(f);
    }
  }
});

test("abort in a retained material getter prevents its returned function from running", async () => {
  const f = fixture();
  const abort = new AbortController();
  f.options.signal = abort.signal;
  const owner = await retained(f);
  let called = false;
  accessor(f.state.probe, "material", () => {
    abort.abort(secret);
    return () => { called = true; return JSON.stringify(materialFor(0)); };
  });
  assert.throws(() => owner.material(0), closed);
  assert.equal(called, false);
  assert.equal(owner.state().error, cancelled.message);
  cleaned(f);
});

test("pending method accessors are guarded after lookup and cannot invoke returned functions after abort", async () => {
  for (const name of ["outgoing", "feed", "complete", "finish", "report", "send"]) {
    const f = fixture();
    const abort = new AbortController();
    const pending = openWorldMaterials({ ...f.options, signal: abort.signal });
    const rejected = assert.rejects(pending, cancelled);
    await opened(f);
    const target = name === "send" ? f.state.socket : f.state.probe;
    let called = false;
    accessor(target, name, () => { abort.abort(secret); return () => { called = true; throw new Error(secret); }; });
    message(f);
    if (["finish", "report"].includes(name)) message(f);
    await rejected;
    assert.equal(called, false, name);
    cleaned(f);
  }
});

test("pending method getters that throw expose only static errors and always wipe the input buffer", async () => {
  for (const name of ["outgoing", "feed", "complete", "finish", "report", "send"]) {
    const f = fixture();
    const pending = openWorldMaterials(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    accessor(name === "send" ? f.state.socket : f.state.probe, name, () => { throw new Error(secret); });
    message(f);
    if (["finish", "report"].includes(name)) message(f);
    await rejected;
    cleaned(f);
  }
});

test("methods replaced with nonfunctions after startup fail without exposing private replacements", async () => {
  for (const name of ["outgoing", "feed", "complete", "finish", "report", "send"]) {
    const f = fixture();
    const pending = openWorldMaterials(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    (name === "send" ? f.state.socket : f.state.probe)[name] = { private: secret };
    message(f);
    if (["finish", "report"].includes(name)) message(f);
    await rejected;
    cleaned(f);
  }
});

test("abort in every exchange call including successful transport close prevents publication", async () => {
  for (const point of ["feed", "outgoing", "send", "complete", "finish", "report", "close"]) {
    const f = fixture();
    const abort = new AbortController();
    const pending = openWorldMaterials({ ...f.options, signal: abort.signal });
    const rejected = assert.rejects(pending, cancelled);
    await opened(f);
    f.state.hooks = { [point]: () => abort.abort(secret) };
    message(f);
    if (["finish", "report", "close"].includes(point)) message(f);
    await rejected;
    cleaned(f);
  }
});

test("the original deadline covers every synchronous exchange call and final socket close", async (t) => {
  const clock = fakeTimers(t);
  for (const point of ["feed", "outgoing", "send", "complete", "finish", "report", "close"]) {
    const f = fixture();
    const pending = openWorldMaterials(f.options);
    const rejected = assert.rejects(pending, timedOut);
    await opened(f);
    f.state.hooks = { [point]: () => clock.advance(1000) };
    message(f);
    if (["finish", "report", "close"].includes(point)) message(f);
    await rejected;
    assert.equal(clock.pending().length, 0);
    cleaned(f);
  }
});

test("the deadline is rechecked after pending accessors before calling their returned methods", async (t) => {
  const clock = fakeTimers(t);
  for (const name of ["outgoing", "feed", "complete", "finish", "report", "send"]) {
    const f = fixture();
    const pending = openWorldMaterials(f.options);
    const rejected = assert.rejects(pending, timedOut);
    await opened(f);
    let called = false;
    accessor(name === "send" ? f.state.socket : f.state.probe, name, () => {
      clock.advance(1000);
      return () => { called = true; throw new Error(secret); };
    });
    message(f);
    if (["finish", "report"].includes(name)) message(f);
    await rejected;
    assert.equal(called, false, name);
    cleaned(f);
  }
});

test("retention timers, explicit close and post-retention abort free once and clear every timer", async (t) => {
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
    assert.equal(owner.state().phase, "closed");
    assert.equal(owner.state().error, cause === "abort" ? cancelled.message : null);
    assert.equal(JSON.stringify(owner.state()).includes(secret), false);
    assert.throws(() => owner.material(0), closed);
    owner.close();
    abort.abort(secret);
    assert.equal(clock.pending().length, 0);
    cleaned(f);
  }
});

test("expired retention is enforced by state and material even when the timer callback has not run", async (t) => {
  const clock = fakeTimers(t);
  for (const method of ["state", "material"]) {
    const f = fixture();
    const owner = await retained(f);
    clock.advance(1999);
    assert.equal(owner.state().accepted, true);
    assert.deepEqual(owner.material(0), materialFor(0));
    const calls = f.state.materials.length;
    clock.advance(1);
    if (method === "state") assert.equal(owner.state().closed, true);
    else assert.throws(() => owner.material(0), closed);
    assert.equal(f.state.materials.length, calls);
    assert.equal(owner.state().error, null);
    assert.equal(clock.pending().length, 0);
    cleaned(f);
  }
});

test("retention expiry in a material getter blocks invocation and expiry inside a material call blocks its result", async (t) => {
  const clock = fakeTimers(t);
  for (const point of ["getter", "call"]) {
    const f = fixture();
    const owner = await retained(f);
    let called = false;
    if (point === "getter") accessor(f.state.probe, "material", () => {
      clock.advance(2000);
      return () => { called = true; return JSON.stringify(materialFor(0)); };
    });
    else f.state.probe.material = () => { called = true; clock.advance(2000); return JSON.stringify(materialFor(0)); };
    assert.throws(() => owner.material(0), closed);
    assert.equal(called, point === "call");
    assert.equal(owner.state().closed, true);
    assert.equal(clock.pending().length, 0);
    cleaned(f);
  }
});

test("hostile cancellation getters after retention close the owner without exposing their thrown secret", async () => {
  for (const method of ["state", "material"]) {
    const f = fixture();
    let hostile = false;
    f.options.signal = {
      get aborted() { if (hostile) throw new Error(secret); return false; },
      addEventListener() {}, removeEventListener() {},
    };
    const owner = await retained(f);
    hostile = true;
    if (method === "state") assert.equal(owner.state().closed, true);
    else assert.throws(() => owner.material(0), closed);
    assert.equal(owner.state().error, unavailable.message);
    assert.equal(JSON.stringify(owner.state()).includes(secret), false);
    owner.close();
    cleaned(f);
  }
});

test("saved late callbacks cannot resurrect failed, retained or explicitly closed owners and still wipe late payloads", async () => {
  for (const outcome of ["abort", "success", "closed"]) {
    const f = fixture();
    const abort = new AbortController();
    const pending = openWorldMaterials({ ...f.options, signal: abort.signal });
    const rejected = outcome === "abort" ? assert.rejects(pending, cancelled) : null;
    await opened(f);
    const { onopen, onmessage, onerror, onclose } = f.state.socket;
    let owner;
    if (outcome === "abort") { abort.abort(secret); await rejected; }
    else { message(f); message(f); owner = await pending; if (outcome === "closed") owner.close(); }
    const before = { ...f.state.calls };
    onopen();
    for (const data of [new Uint8Array([7, 8]).buffer, new Uint8Array([7, 8]), new DataView(new Uint8Array([7, 8]).buffer)]) {
      onmessage({ data });
      if (data instanceof ArrayBuffer) assert.deepEqual([...new Uint8Array(data)], [0, 0]);
      else wiped(data);
    }
    onmessage({ get data() { throw new Error(secret); } });
    onerror(new Error(secret));
    onclose({ code: 1000, wasClean: true, reason: secret });
    assert.deepEqual(f.state.calls, before);
    if (owner) {
      assert.equal(owner.state().accepted, outcome === "success");
      if (outcome === "success") assert.deepEqual(owner.material(0), materialFor(0));
      owner.close();
    }
    cleaned(f);
  }
});

test("synchronously saved callbacks during transport close cannot invalidate verified completion", async () => {
  const f = fixture();
  const pending = openWorldMaterials(f.options);
  await opened(f);
  const { onopen, onmessage, onerror, onclose } = f.state.socket;
  const late = new Uint8Array([99]);
  f.state.hooks = { close() {
    onopen();
    onmessage({ data: late.buffer });
    onmessage({ get data() { throw new Error(secret); } });
    onerror(new Error(secret));
    onclose({ code: 1000, wasClean: true, reason: secret });
  } };
  message(f);
  message(f);
  const owner = await pending;
  try {
    assert.equal(owner.state().accepted, true);
    assert.equal(owner.state().receivedBytes, 2);
    assert.equal(f.state.feeds.length, 2);
    assert.deepEqual([...late], [0]);
  } finally { owner.close(); }
  cleaned(f);
});

test("cleanup exceptions and reentrant close never repeat frees or expose private transport errors", async () => {
  const f = fixture({ errorAt: "close" });
  const owner = await retained(f);
  f.state.errorAt = "free";
  f.state.hooks = { free() { owner.close(); } };
  owner.close();
  owner.close();
  assert.equal(owner.state().closed, true);
  assert.equal(owner.state().error, null);
  assert.equal(JSON.stringify(owner.state()).includes(secret), false);
  cleaned(f);
});

test("cleanup tolerates throwing handler setters, close accessors, free accessors and listener removal", async () => {
  for (const point of ["handlers", "close"]) {
    const f = fixture();
    const pending = openWorldMaterials(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    const { onerror } = f.state.socket;
    if (point === "handlers") for (const name of ["onopen", "onmessage", "onerror", "onclose"]) Object.defineProperty(f.state.socket, name, { configurable: true, set() { throw new Error(secret); } });
    else accessor(f.state.socket, "close", () => { throw new Error(secret); });
    onerror(new Error(secret));
    await rejected;
    onerror(new Error(secret));
    assert.equal(f.state.freed, 1);
    assert.equal(f.state.closed, point === "close" ? 0 : 1);
    for (const bytes of f.state.outgoing) wiped(bytes);
  }
  for (const point of ["free", "remove"]) {
    const f = fixture();
    const abort = new AbortController();
    f.options.signal = abort.signal;
    const owner = await retained(f);
    if (point === "free") accessor(f.state.probe, "free", () => { throw new Error(secret); });
    else abort.signal.removeEventListener = () => { throw new Error(secret); };
    owner.close();
    owner.close();
    assert.equal(owner.state().closed, true);
    assert.equal(JSON.stringify(owner.state()).includes(secret), false);
    cleaned(f, point === "free" ? 0 : 1);
  }
});

test("reentrant state reads and lookups from free observe the closed snapshot before native cleanup", async () => {
  for (const cause of ["close", "abort"]) {
    const f = fixture();
    const abort = new AbortController();
    f.options.signal = abort.signal;
    const owner = await retained(f);
    let observed;
    let reads = 0;
    accessor(f.state.probe, "material", () => { reads++; return () => JSON.stringify(materialFor(0)); });
    f.state.hooks = { free() {
      observed = owner.state();
      assert.throws(() => owner.material(0), closed);
      owner.close();
    } };
    if (cause === "abort") abort.abort(secret);
    else owner.close();
    assert.ok(observed);
    assert.equal(observed.accepted, false);
    assert.equal(observed.closed, true);
    assert.equal(observed.phase, "closed");
    assert.equal(observed.error, cause === "abort" ? cancelled.message : null);
    assert.equal(reads, 0);
    cleaned(f);
  }
});
