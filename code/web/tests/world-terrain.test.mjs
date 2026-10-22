import test from "node:test";
import assert from "node:assert/strict";
import { setImmediate } from "node:timers/promises";
import { openWorldTerrain } from "../static/world-terrain.js";
import { openWorldFloors } from "../static/world-floors.js";
import { PUBLIC_LOGIN_DEFAULTS } from "../static/login-config.js";

const MIB = 1024 * 1024;
const relay = "ws://127.0.0.1:8110/js5";
const secret = "FAKE_PRIVATE_SERVER_SECRET";
const unavailable = { message: "verified world terrain unavailable" };
const cancelled = { message: "world terrain cancelled" };
const timedOut = { message: "world terrain timed out" };
const initial = () => ({ base: { x: 3200, y: 3200 }, mapSize: 72, mapKind: "standard", regionIds: [12850, 12851, 13106, 13107], chunkCount: 0 });

function reportFor(state) {
  return {
    index: 5,
    referenceRevision: 1234,
    referenceArchives: state.regions.length,
    referenceFiles: state.regions.length,
    retainedArchives: state.regions.length,
    referenceCrcVerified: true,
    referenceWhirlpoolVerified: true,
    authenticated: false,
    totalWireBytes: state.received,
    totalContainerBytes: 65536 + 10,
    totalExpandedBytes: 65536 + 10,
    totalOperations: 131072 + 10,
    limits: {
      maxRegions: 36,
      maxReferenceArchives: 262144,
      maxReferenceFiles: 262144,
      maxReferenceRecords: 524288,
      maxRetainedArchives: 36,
      maxRetainedFiles: 4096,
      maxContainerBytes: 4 * MIB,
      maxExpandedContainerBytes: 8 * MIB,
      maxTerrainFileBytes: MIB,
      maxTagsPerTile: 256,
      tileTagLimitIsDiagnostic: true,
      maxTotalWireBytes: 32 * MIB,
      maxTotalContainerBytes: 24 * MIB,
      maxTotalExpandedBytes: 32 * MIB,
      maxTotalOperations: 128 * MIB,
      operationUnit: "wire/container/expanded/split/tile bytes and bounded metadata records",
    },
    regions: state.regions.map((regionId, index) => ({
      regionId,
      origin: [(regionId >> 8) * 64, (regionId & 255) * 64],
      present: true,
      archiveId: 100 + index,
      archiveRevision: -10 + index,
      containerBytes: 16384,
      expandedBytes: 16384,
      terrainBytes: 16384,
      suffixBytes: 0,
      tileCount: 16384,
      operations: 32768,
      crcVerified: true,
      whirlpoolVerified: index !== 0,
      effectsDecoded: false,
    })),
  };
}

function surfaceFor(state, x, y, size) {
  const stride = size + 1;
  const heightCount = 4 * stride * stride;
  return {
    scope: "surface-only", baseTile: [x, y], mapSize: size, stride, planes: 4, heightCount,
    knownHeights: heightCount - 17, unavailableHeights: 17,
    regions: state.regions.map((regionId) => ({ regionId, availability: "present" })),
    effectsDecoded: false, renderedWorld: false,
  };
}

function fixture(overrides = {}) {
  const state = {
    loads: 0, created: 0, freed: 0, closed: 0, finishes: 0, reports: 0,
    phase: 0, completeAfter: 2, received: 0, feeds: [], outgoing: [], sent: [], compose: [], height: [], sceneHeight: [], sceneTile: [],
    ...overrides,
  };
  const fault = (method) => {
    state.hooks?.[method]?.();
    if (state.errorAt === method) throw new Error(secret);
  };
  class Probe {
    constructor(token, regions) {
      state.created++;
      state.token = token;
      state.regionJson = regions;
      state.regions = JSON.parse(regions);
      fault("constructor");
    }
    outgoing() {
      fault("outgoing");
      const bytes = state.outgoingValue?.() ?? new Uint8Array([state.phase ? 6 : 15]);
      state.outgoing.push(bytes);
      return bytes;
    }
    feed(bytes) {
      state.feeds.push(bytes.length <= 1024 ? [...bytes] : { bytes: bytes.length, first: bytes[0] });
      state.received += bytes.length;
      state.phase++;
      fault("feed");
    }
    complete() { fault("complete"); return state.completeValue === undefined ? state.phase >= state.completeAfter : state.completeValue; }
    finish() { state.finishes++; fault("finish"); assert.ok(state.phase >= state.completeAfter); }
    compose(x, y, size) {
      state.compose.push([x, y, size]);
      fault("compose");
      const surface = surfaceFor(state, x, y, size);
      state.mutateSurface?.(surface);
      return state.surfaceText ?? JSON.stringify(state.surface ?? surface);
    }
    report() {
      state.reports++;
      fault("report");
      const report = reportFor(state);
      state.mutateReport?.(report);
      return state.reportText ?? JSON.stringify(report);
    }
    height(...args) { state.height.push(args); fault("height"); return Object.hasOwn(state, "heightValue") ? state.heightValue : -432; }
    sceneHeight(...args) { state.sceneHeight.push(args); fault("sceneHeight"); return Object.hasOwn(state, "sceneValue") ? state.sceneValue : -876; }
    sceneTile(...args) {
      state.sceneTile.push(args);
      fault("sceneTile");
      return Object.hasOwn(state, "tileValue") ? state.tileValue : JSON.stringify({ underlayId: 255, overlayId: 0, renderFlags: 32, overlayShape: 11, overlayRotation: 3 });
    }
    composePalette(floors) {
      state.floorProbe = floors;
      state.composeThis = this;
      fault("composePalette");
      return Object.hasOwn(state, "composeValue") ? state.composeValue : JSON.stringify(state.palette ?? paletteFor());
    }
    paletteColor(...args) { state.paletteRead = args; fault("paletteColor"); return Object.hasOwn(state, "paletteValue") ? state.paletteValue : 12345; }
    free() { state.freed++; fault("free"); }
  }
  class Socket {
    constructor(url) { state.socket = this; state.url = url; fault("socket"); }
    send(bytes) { state.sent.push([...bytes]); fault("send"); }
    close() { state.closed++; fault("close"); }
  }
  const options = {
    js5Relay: relay,
    initialWorld: initial(),
    load: async () => { state.loads++; fault("load"); return Probe; },
    WebSocketClass: Socket,
    timeoutMs: 1000,
    lifetimeMs: 2000,
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
  const pending = openWorldTerrain(f.options);
  await opened(f);
  message(f);
  message(f);
  return pending;
}

function cleaned(f, freed = 1) {
  assert.equal(f.state.freed, freed);
  assert.equal(f.state.closed, 1);
  for (const handler of ["onopen", "onmessage", "onerror", "onclose"]) assert.equal(f.state.socket[handler], null);
  for (const bytes of f.state.outgoing) assert.ok(bytes.every((value) => value === 0));
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

function paletteFor(size = 72) {
  return { scope: "underlay-packed-hsl", mapSize: size, planes: 4, tileCount: 4 * size * size, knownColors: 4 * size * size, unavailableColors: 0, unavailableDefinitionIds: [], rgbPrepared: false, renderedWorld: false };
}

function floorReportFor(received) {
  return {
    revision: 727, subrevision: 1, prefetchCount: 27, index: 2,
    referenceRevision: -1234, referenceArchives: 24, referenceFiles: 8,
    referenceCrcVerified: true, referenceWhirlpoolVerified: true,
    authenticated: false, renderedWorld: false, opcode8SideEffectsApplied: false,
    totalWireBytes: received, totalContainerBytes: 96, totalExpandedBytes: 80, totalOperations: 1000,
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

async function genuineFloors() {
  const own = { received: 0, feeds: 0, freed: 0, closed: 0 };
  class FloorProbe {
    constructor() { own.probe = this; }
    outgoing() { return new Uint8Array([15]); }
    feed(bytes) { own.received += bytes.length; own.feeds++; }
    complete() { return own.feeds === 2; }
    finish() {}
    report() { return JSON.stringify(floorReportFor(own.received)); }
    underlay() { return "null"; }
    overlay() { return "null"; }
    free() { own.freed++; }
  }
  class FloorSocket {
    constructor() { own.socket = this; }
    send() {}
    close() { own.closed++; }
  }
  const pending = openWorldFloors({ js5Relay: relay, load: async () => FloorProbe, WebSocketClass: FloorSocket, timeoutMs: 1000, lifetimeMs: 120000 });
  await setImmediate();
  assert.ok(own.socket);
  own.socket.onopen();
  for (let at = 0; at < 2; at++) {
    const bytes = new Uint8Array([1]);
    own.socket.onmessage({ data: bytes.buffer });
    assert.equal(bytes[0], 0);
  }
  const owner = await pending;
  return { own, owner };
}

function assertFloorsFreed(floors) {
  assert.equal(floors.own.freed, 1);
  assert.equal(floors.own.closed, 1);
}

function forgedFloorsFor(floors, seen) {
  const record = (name) => (...args) => { seen.push([name, args]); return paletteFor(); };
  return [
    null, false, 0, "floors", {},
    { composeTerrain: 1 },
    { composeTerrain: record("composeTerrain") },
    { composeTerrain: record("composeTerrain"), composePalette: record("composePalette"), state: record("state"), close: record("close") },
    Object.freeze({ composeTerrain: record("composeTerrain"), state: () => ({ accepted: true, closed: false }) }),
    { ...floors.owner },
    { ...floors.owner, composeTerrain: record("composeTerrain") },
    Object.create(floors.owner),
    Object.defineProperty(Object.create(floors.owner), "composeTerrain", { value: record("composeTerrain") }),
    new Proxy(floors.owner, { get(target, key) { seen.push(["get", [String(key)]]); return Reflect.get(target, key); } }),
    structuredClone({ accepted: true, closed: false }),
  ];
}

test("scene tiles retain raw byte metadata as immutable independent snapshots without requiring floors", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    const expected = { underlayId: 255, overlayId: 0, renderFlags: 32, overlayShape: 11, overlayRotation: 3 };
    const tile = owner.sceneTile(3, 71, 71);
    assert.deepEqual(tile, expected);
    assert.equal(Object.getPrototypeOf(tile), Object.prototype);
    assert.ok(Object.isFrozen(tile));
    assert.throws(() => { tile.overlayShape = 0; }, TypeError);
    const next = owner.sceneTile(3, 71, 71);
    assert.notEqual(next, tile);
    assert.deepEqual(next, expected);
    assert.deepEqual(f.state.sceneTile, [[3, 71, 71], [3, 71, 71]]);
    f.state.tileValue = "null";
    assert.equal(owner.sceneTile(0, 0, 0), null);
    f.state.tileValue = JSON.stringify({ overlayRotation: 0, overlayShape: 0, renderFlags: 0, overlayId: 255, underlayId: 0 });
    assert.deepEqual(owner.sceneTile(0, 0, 0), { underlayId: 0, overlayId: 255, renderFlags: 0, overlayShape: 0, overlayRotation: 0 });
    assert.equal(owner.state().effectsDecoded, false);
    assert.equal(owner.state().renderedWorld, false);
    assert.equal(owner.state().accepted, true);
    cleaned(f, 0);
  } finally { owner.close(); }
  cleaned(f);
});

test("scene tile coordinates are strict noncoercing tile bounds for every native map size", async () => {
  for (const mapSize of [72, 104, 120, 136, 168]) {
    const f = fixture();
    f.options.initialWorld.mapSize = mapSize;
    const owner = await retained(f);
    try {
      const invalid = [undefined, null, NaN, Infinity, -Infinity, -1, 0.5, "0", true, 1n, Symbol(), 4294967296, { valueOf() { assert.fail("must not coerce coordinates"); } }];
      for (const value of invalid) {
        for (let at = 0; at < 3; at++) {
          const args = [0, 0, 0];
          args[at] = value;
          assert.throws(() => owner.sceneTile(...args), { message: "invalid world terrain coordinates" });
        }
      }
      for (const args of [[4, 0, 0], [0, mapSize, 0], [0, 0, mapSize]]) {
        assert.throws(() => owner.sceneTile(...args), { message: "invalid world terrain coordinates" });
      }
      assert.deepEqual(f.state.sceneTile, []);
      owner.sceneTile(0, 0, 0);
      owner.sceneTile(3, mapSize - 1, mapSize - 1);
      assert.deepEqual(f.state.sceneTile, [[0, 0, 0], [3, mapSize - 1, mapSize - 1]]);
      assert.equal(owner.state().accepted, true);
    } finally { owner.close(); }
    const calls = f.state.sceneTile.length;
    assert.throws(() => owner.sceneTile(0, 0, 0), { message: "world terrain owner is closed" });
    assert.equal(f.state.sceneTile.length, calls);
    cleaned(f);
  }
});

test("scene tile binding is mandatory before allocating transport", async () => {
  for (const value of [undefined, null, 1, "sceneTile", {}]) {
    const f = fixture();
    f.Probe.prototype.sceneTile = value;
    await assert.rejects(openWorldTerrain(f.options), unavailable);
    assert.equal(f.state.created, 1);
    assert.equal(f.state.freed, 1);
    assert.equal(f.state.socket, undefined);
  }
});

test("scene tiles accept only bounded JSON strings containing null or the exact unsigned schema", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    const valid = { underlayId: 1, overlayId: 2, renderFlags: 3, overlayShape: 4, overlayRotation: 1 };
    const invalid = [
      undefined, null, false, 1, {}, valid, [], new String("null"), Promise.resolve("null"), Symbol(),
      { toString() { assert.fail("must not coerce output"); }, toJSON() { assert.fail("must not stringify output"); } },
      "", "{", "undefined", "NaN", "null trailing", "[]", "[null]", "true", "0", JSON.stringify(secret), JSON.stringify("null"),
      " ".repeat(1025) + "null", "{}", JSON.stringify({ ...valid, privateAccount: secret }),
      ...["__proto__", "constructor", "prototype", "toString"].map((key) => JSON.stringify({ ...valid, [key]: 0 })),
    ];
    for (const [key, max] of Object.entries({ underlayId: 255, overlayId: 255, renderFlags: 32, overlayShape: 11, overlayRotation: 3 })) {
      const missing = { ...valid };
      delete missing[key];
      invalid.push(JSON.stringify(missing));
      const renamed = { ...missing, [key.toUpperCase()]: valid[key] };
      invalid.push(JSON.stringify(renamed));
      for (const value of [null, false, true, "0", {}, [], -1, 0.5, max + 1, 4294967296, 4294967296 + max, -4294967296, 9007199254740992]) {
        invalid.push(JSON.stringify({ ...valid, [key]: value }));
      }
      invalid.push(JSON.stringify(valid).replace(`"${key}":${valid[key]}`, `"${key}":1e400`));
      for (const value of [0, max]) {
        f.state.tileValue = JSON.stringify({ ...valid, [key]: value });
        assert.deepEqual(owner.sceneTile(0, 0, 0), { ...valid, [key]: value });
      }
    }
    for (const value of invalid) {
      f.state.tileValue = value;
      assert.throws(() => owner.sceneTile(0, 0, 0), { message: "world terrain sample unavailable" });
      assert.equal(owner.state().accepted, true);
      assert.equal(f.state.freed, 0);
    }
    f.state.errorAt = "sceneTile";
    assert.throws(() => owner.sceneTile(0, 0, 0), { message: "world terrain sample unavailable" });
    f.state.errorAt = undefined;
    f.state.tileValue = JSON.stringify(valid);
    assert.deepEqual(owner.sceneTile(0, 0, 0), valid);
    assert.equal(JSON.stringify(owner.state()).includes(secret), false);
  } finally { owner.close(); }
  cleaned(f);
});

test("scene tile reads recheck retained ownership after reentrant close, abort or lifetime expiration", async (t) => {
  const clock = fakeTimers(t);
  for (const mode of ["close", "abort", "lifetime"]) {
    for (const result of ["valid", "null", "invalid", "throw"]) {
      const f = fixture();
      const abort = new AbortController();
      f.options.signal = abort.signal;
      const owner = await retained(f);
      f.state.hooks = { sceneTile() {
        if (mode === "close") owner.close();
        if (mode === "abort") abort.abort(secret);
        if (mode === "lifetime") clock.fire(clock.pending(2000)[0]);
      } };
      if (result === "null") f.state.tileValue = "null";
      if (result === "invalid") f.state.tileValue = "{";
      if (result === "throw") f.state.errorAt = "sceneTile";
      assert.throws(() => owner.sceneTile(0, 0, 0), { message: "world terrain owner is closed" });
      assert.equal(owner.state().closed, true);
      assert.equal(owner.state().accepted, false);
      assert.equal(JSON.stringify(owner.state()).includes(secret), false);
      assert.throws(() => owner.sceneTile(0, 0, 0), { message: "world terrain owner is closed" });
      assert.deepEqual(f.state.sceneTile, [[0, 0, 0]]);
      owner.close();
      cleaned(f);
      assert.equal(clock.pending().length, 0);
    }
  }
});

test("retained lifetime expiration revokes scene tile reads before entering WASM", async (t) => {
  const clock = fakeTimers(t);
  const f = fixture();
  const owner = await retained(f);
  clock.fire(clock.pending(2000)[0]);
  assert.throws(() => owner.sceneTile(0, 0, 0), { message: "world terrain owner is closed" });
  assert.deepEqual(f.state.sceneTile, []);
  cleaned(f);
});

test("optional genuine floor owner composes packed HSL and retains checked palette samples", async () => {
  const f = fixture();
  const floors = await genuineFloors();
  const summary = paletteFor();
  f.state.palette = summary;
  f.options.floors = floors.owner;
  const owner = await retained(f);
  try {
    assert.ok(f.state.composeThis instanceof f.Probe);
    assert.equal(f.state.floorProbe, floors.own.probe);
    assert.deepEqual(owner.state().palette, summary);
    summary.knownColors = 0;
    assert.equal(owner.state().palette.knownColors, 4 * 72 * 72);
    assert.equal(owner.paletteColor(3, 71, 71), 12345);
    assert.deepEqual(f.state.paletteRead, [3, 71, 71]);
    for (const args of [[4, 0, 0], [0, 72, 0], [0, 0, 72], [0, -1, 0], [0, NaN, 0], [0, 0.5, 0], ["0", 0, 0], [0, 4294967296, 0]]) {
      assert.throws(() => owner.paletteColor(...args), /invalid world terrain coordinates/);
    }
    for (const value of [null, -2147483648, 2147483647]) {
      f.state.paletteValue = value;
      assert.equal(owner.paletteColor(0, 0, 0), value);
    }
    for (const value of [undefined, NaN, Infinity, "1", 2147483648, -2147483649, 1.5]) {
      f.state.paletteValue = value;
      assert.throws(() => owner.paletteColor(0, 0, 0), /world terrain sample unavailable/);
    }
    f.state.errorAt = "paletteColor";
    assert.throws(() => owner.paletteColor(0, 0, 0), { message: "world terrain sample unavailable" });
    assert.equal(floors.owner.state().accepted, true);
  } finally { owner.close(); }
  assert.throws(() => owner.paletteColor(0, 0, 0), /owner is closed/);
  assert.equal(floors.owner.state().accepted, true);
  assert.equal(floors.own.freed, 0);
  floors.owner.close();
  assertFloorsFreed(floors);
  cleaned(f);
});

test("optional palette bridge rejects hostile summaries and frees terrain without owning genuine floors", async () => {
  const invalid = [
    null, [], {}, Promise.resolve(paletteFor()), "", "{", "null", "[]", JSON.stringify(secret), "x".repeat(65537),
    ...[
      ["scope", "rgb"], ["mapSize", 104], ["planes", 3], ["tileCount", 1],
      ["rgbPrepared", true], ["renderedWorld", true], ["knownColors", -1],
      ["knownColors", 0], ["unavailableColors", NaN], ["unavailableColors", "0"],
      ["unavailableDefinitionIds", [255]], ["unavailableDefinitionIds", [1, 1]],
      ["unavailableDefinitionIds", [2, 1]], ["unavailableDefinitionIds", ["1"]],
      ["unavailableDefinitionIds", [NaN]],
    ].map(([key, value]) => JSON.stringify({ ...paletteFor(), [key]: value })),
  ];
  for (const result of invalid) {
    const f = fixture();
    f.state.composeValue = result;
    const floors = await genuineFloors();
    f.options.floors = floors.owner;
    const pending = openWorldTerrain(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    message(f);
    message(f);
    await rejected;
    cleaned(f);
    assert.equal(floors.owner.state().accepted, true);
    assert.equal(floors.own.freed, 0);
    floors.owner.close();
    assertFloorsFreed(floors);
  }
});

test("optional palette bridge rejects forged floors before creating the terrain probe or delivering any argument", async () => {
  const floors = await genuineFloors();
  const seen = [];
  for (const forged of forgedFloorsFor(floors, seen)) {
    const f = fixture();
    await assert.rejects(openWorldTerrain({ ...f.options, floors: forged }), unavailable);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.socket, undefined);
    assert.equal(f.state.composeThis, undefined);
  }
  assert.deepEqual(seen, []);
  floors.owner.close();
  const f = fixture();
  await assert.rejects(openWorldTerrain({ ...f.options, floors: floors.owner }), unavailable);
  assert.equal(f.state.created, 0);
  assert.equal(f.state.loads, 0);
  assertFloorsFreed(floors);
});

test("optional palette bridge validates probe methods and catches cancellation or deadline inside composition", async (t) => {
  for (const method of ["composePalette", "paletteColor"]) {
    const f = fixture();
    f.Probe.prototype[method] = undefined;
    const floors = await genuineFloors();
    f.options.floors = floors.owner;
    await assert.rejects(openWorldTerrain(f.options), unavailable);
    assert.equal(f.state.freed, 1);
    assert.equal(floors.owner.state().accepted, true);
    floors.owner.close();
    assertFloorsFreed(floors);
  }
  const clock = fakeTimers(t);
  for (const mode of ["abort", "deadline", "throw"]) {
    const f = fixture();
    const abort = new AbortController();
    f.options.signal = abort.signal;
    const floors = await genuineFloors();
    f.options.floors = floors.owner;
    f.state.hooks = { composePalette() {
      if (mode === "abort") abort.abort(secret);
      if (mode === "deadline") clock.advance(1000);
      if (mode === "throw") throw new Error(secret);
    } };
    const pending = openWorldTerrain(f.options);
    const rejected = assert.rejects(pending, mode === "abort" ? cancelled : mode === "deadline" ? timedOut : unavailable);
    await opened(f);
    message(f);
    message(f);
    await rejected;
    cleaned(f);
    assert.equal(floors.owner.state().accepted, true);
    floors.owner.close();
    assertFloorsFreed(floors);
  }
});

test("verified terrain fetch closes transport but retains the decoded probe until owner closes", async () => {
  const f = fixture();
  const world = f.options.initialWorld;
  Object.defineProperty(world, "mapKeys", { get() { assert.fail("map keys must never be inspected"); } });
  const pending = openWorldTerrain(f.options);
  world.base.x = 0;
  world.regionIds.fill(1);
  await opened(f);
  assert.equal(f.state.url, relay);
  assert.equal(f.state.socket.binaryType, "arraybuffer");
  assert.equal(f.state.token, PUBLIC_LOGIN_DEFAULTS.js5AuthToken);
  assert.equal(f.state.regionJson, JSON.stringify(initial().regionIds));
  assert.deepEqual(f.state.sent, [[15]]);
  message(f, [7, 8]);
  assert.equal(f.state.finishes, 0);
  message(f, [9]);
  const owner = await pending;
  assert.ok(Object.isFrozen(owner));
  assert.deepEqual(f.state.feeds, [[7, 8], [9]]);
  assert.equal(f.state.finishes, 1);
  assert.deepEqual(f.state.compose, [[3200, 3200, 72]]);
  assert.equal(f.state.reports, 1);
  cleaned(f, 0);
  const state = owner.state();
  assert.equal(state.accepted, true);
  assert.equal(state.closed, false);
  assert.equal(state.phase, "ready");
  assert.equal(state.authenticated, false);
  assert.equal(state.scope, "surface-only");
  assert.equal(state.effectsDecoded, false);
  assert.equal(state.renderedWorld, false);
  assert.equal(state.report.referenceRevision, 1234);
  assert.equal(state.receivedBytes, 3);
  assert.equal(state.report.regions[0].whirlpoolVerified, false);
  assert.equal(owner.height(12850, 0, 0, 0), -432);
  assert.equal(owner.sceneHeight(3, 72, 72), -876);
  owner.close();
  owner.close();
  assert.equal(owner.state().accepted, false);
  assert.equal(owner.state().closed, true);
  assert.equal(owner.state().report.referenceRevision, 1234);
  assert.throws(() => owner.height(12850, 0, 0, 0), /owner is closed/);
  assert.throws(() => owner.sceneHeight(0, 0, 0), /owner is closed/);
  cleaned(f);
});

test("state snapshots cannot mutate retained reports or summaries", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    const snapshot = owner.state();
    snapshot.report.regions[0].origin.fill(-1);
    snapshot.report.limits.maxRegions = 100;
    snapshot.surface.mapSize = 1;
    snapshot.accepted = false;
    const next = owner.state();
    assert.deepEqual(next.report.regions[0].origin, [3200, 3200]);
    assert.equal(next.report.limits.maxRegions, 36);
    assert.equal(next.surface.mapSize, 72);
    assert.equal(next.accepted, true);
  } finally { owner.close(); }
});

test("unrecognized runtime fields never escape through the safe report", async () => {
  const f = fixture({ mutateReport(report) {
    report.privateAccount = secret;
    report.regions[0].mapKeys = [secret];
    report.limits.serverError = secret;
  } });
  const owner = await retained(f);
  try { assert.equal(JSON.stringify(owner.state()).includes(secret), false); }
  finally { owner.close(); }
});

test("only exact loopback JS5 routes can allocate a runtime or socket", async () => {
  const invalid = [
    undefined, null, 123, {}, "/js5", "ws://example.com/js5", "ws://localhost.evil/js5", "ws://127.0.0.1.evil/js5",
    "http://localhost/js5", "ws://localhost/world", "ws://localhost/js5/", "ws://localhost/js5?upstream=private", "ws://localhost/js5?", "ws://localhost/js5#", "ws://localhost/js5#private",
    "ws://user@localhost/js5", "ws://user:password@localhost/js5", "ws://127.1/js5", "ws://2130706433/js5", "ws://0x7f000001/js5", "ws://127.000.000.001/js5",
    "ws://localhost/x/../js5", "ws://localhost/%6as5", " ws://localhost/js5", "ws://localhost/js5\n", "ws://localhost:0/js5", "ws://localhost:65536/js5",
  ];
  for (const js5Relay of invalid) {
    const f = fixture();
    await assert.rejects(openWorldTerrain({ ...f.options, js5Relay }), unavailable);
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.socket, undefined);
  }
});

test("ws and wss relay routes support all three literal loopback hosts without routing derivation", async () => {
  for (const js5Relay of ["ws://localhost/js5", "wss://localhost:8110/js5", "ws://[::1]:8110/js5", "wss://127.0.0.1/js5"]) {
    const f = fixture();
    f.options.js5Relay = js5Relay;
    const owner = await retained(f);
    try { assert.equal(f.state.url, new URL(js5Relay).href); }
    finally { owner.close(); }
  }
});

test("dynamic maps are rejected even when no dynamic chunks are populated", async () => {
  for (const chunkCount of [0, 1]) {
    const f = fixture();
    f.options.initialWorld.mapKind = "dynamic";
    f.options.initialWorld.chunkCount = chunkCount;
    await assert.rejects(openWorldTerrain(f.options), unavailable);
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.created, 0);
  }
});

test("invalid initial maps and noncoercing integer arguments fail before loading", async () => {
  const mutations = [
    (world) => { delete world.mapKind; },
    (world) => { world.mapKind = "unknown"; },
    (world) => { world.mapKind = { toString() { assert.fail("must not coerce"); } }; },
    ...[null, [], "private", 0].map((base) => (world) => { world.base = base; }),
    ...["3200", null, undefined, NaN, Infinity, -8, 3201, 16320, 0x100000000, 1n].map((x) => (world) => { world.base.x = x; }),
    ...["3200", -1, NaN, 16320].map((y) => (world) => { world.base.y = y; }),
    ...["72", 0, 64, 73, 169, Infinity, null, 1n].map((size) => (world) => { world.mapSize = size; }),
    ...[[], Array(37).fill(1), null, "12850", [1, 1], ["1"], [-1], [65536], [NaN], [Infinity], [1.5], [1n], new Uint16Array([1]), Array(1)].map((ids) => (world) => { world.regionIds = ids; }),
    (world) => { Object.defineProperty(world, "base", { get() { assert.fail("must not evaluate input accessors"); } }); },
    (world) => { Object.defineProperty(world.base, "x", { get() { assert.fail("must not evaluate input accessors"); } }); },
    (world) => { Object.defineProperty(world.regionIds, "0", { get() { assert.fail("must not evaluate input accessors"); } }); },
  ];
  for (const mutate of mutations) {
    const f = fixture();
    mutate(f.options.initialWorld);
    await assert.rejects(openWorldTerrain(f.options), unavailable);
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.socket, undefined);
  }
  for (const initialWorld of [undefined, null, [], "private", 1]) {
    const f = fixture();
    await assert.rejects(openWorldTerrain({ ...f.options, initialWorld }), unavailable);
    assert.equal(f.state.loads, 0);
  }
});

test("the inclusive maximum region count and u16 identities are accepted without passing other world data", async () => {
  const f = fixture();
  const world = initial();
  world.regionIds = [...Array(35).keys(), 65535];
  f.options.initialWorld = world;
  const pending = openWorldTerrain(f.options);
  const rejected = assert.rejects(pending, unavailable);
  await opened(f);
  assert.deepEqual(f.state.regions, world.regionIds);
  assert.equal(f.state.regionJson.includes("base"), false);
  f.state.socket.onerror();
  await rejected;
  cleaned(f);
});

test("all native map sizes and bounded aligned bases reach compose unchanged", async () => {
  for (const mapSize of [72, 104, 120, 136, 168]) {
    const f = fixture();
    f.options.initialWorld.mapSize = mapSize;
    f.options.initialWorld.base = { x: 0, y: 16384 - mapSize };
    const owner = await retained(f);
    try { assert.deepEqual(f.state.compose, [[0, 16384 - mapSize, mapSize]]); }
    finally { owner.close(); }
  }
});

test("timeouts and retained lifetimes must be bounded noncoercing integers", async () => {
  for (const timeoutMs of [0, -1, 30001, Infinity, NaN, "100", 1.5, null, 1n]) {
    const f = fixture();
    await assert.rejects(openWorldTerrain({ ...f.options, timeoutMs }), unavailable);
    assert.equal(f.state.loads, 0);
  }
  for (const lifetimeMs of [0, -1, 300001, Infinity, NaN, "100", 1.5, null, 1n]) {
    const f = fixture();
    await assert.rejects(openWorldTerrain({ ...f.options, lifetimeMs }), unavailable);
    assert.equal(f.state.loads, 0);
  }
});

test("pre-aborted preparation never loads runtime or allocates", async () => {
  const f = fixture();
  const abort = new AbortController();
  abort.abort(secret);
  await assert.rejects(openWorldTerrain({ ...f.options, signal: abort.signal }), cancelled);
  assert.equal(f.state.loads, 0);
  assert.equal(f.state.created, 0);
  assert.equal(f.state.socket, undefined);
});

test("same-turn cancellation cannot call even an injected runtime loader", async () => {
  const f = fixture();
  const abort = new AbortController();
  const pending = openWorldTerrain({ ...f.options, signal: abort.signal });
  const rejected = assert.rejects(pending, cancelled);
  abort.abort(secret);
  await rejected;
  assert.equal(f.state.loads, 0);
  assert.equal(f.state.created, 0);
  assert.equal(f.state.socket, undefined);
});

test("abort subscriptions belong to the retained owner and are removed on every terminal path", async () => {
  for (const outcome of ["success", "abort", "error"]) {
    const abort = new AbortController();
    const listeners = new Set();
    const add = abort.signal.addEventListener.bind(abort.signal);
    const remove = abort.signal.removeEventListener.bind(abort.signal);
    abort.signal.addEventListener = (kind, listener, options) => { listeners.add(listener); add(kind, listener, options); };
    abort.signal.removeEventListener = (kind, listener) => { listeners.delete(listener); remove(kind, listener); };
    const f = fixture();
    const pending = openWorldTerrain({ ...f.options, signal: abort.signal });
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

test("invalid cancellation inputs and thrown preparation errors remain static", async () => {
  for (const signal of [false, {}, { aborted: secret }, { get aborted() { throw new Error(secret); } }, { aborted: false, addEventListener() { throw new Error(secret); }, removeEventListener() { throw new Error(secret); } }]) {
    const f = fixture();
    await assert.rejects(openWorldTerrain({ ...f.options, signal }), unavailable);
    assert.equal(f.state.loads, 0);
    assert.equal(f.state.created, 0);
  }
});

test("abort during runtime load rejects immediately and ignores both late resolution and rejection", async () => {
  for (const result of ["resolve", "reject"]) {
    const f = fixture();
    const loading = deferred();
    const abort = new AbortController();
    f.options.load = () => loading.promise;
    const pending = openWorldTerrain({ ...f.options, signal: abort.signal });
    const rejected = assert.rejects(pending, cancelled);
    await setImmediate();
    abort.abort(secret);
    await rejected;
    loading[result](result === "resolve" ? f.Probe : new Error(secret));
    await setImmediate();
    assert.equal(f.state.created, 0);
    assert.equal(f.state.freed, 0);
    assert.equal(f.state.socket, undefined);
  }
});

test("runtime load has a bounded timeout and late runtime cannot allocate", async (t) => {
  const clock = fakeTimers(t);
  const f = fixture();
  const loading = deferred();
  f.options.load = () => loading.promise;
  const pending = openWorldTerrain(f.options);
  const rejected = assert.rejects(pending, timedOut);
  await setImmediate();
  clock.fire(clock.pending(1000)[0]);
  await rejected;
  loading.resolve(f.Probe);
  await setImmediate();
  assert.equal(f.state.created, 0);
  assert.equal(f.state.freed, 0);
  assert.equal(f.state.socket, undefined);
  assert.equal(clock.pending().length, 0);
});

test("one deadline bounds runtime loading and the whole exchange without resetting after load", async (t) => {
  const clock = fakeTimers(t);
  const f = fixture();
  const loading = deferred();
  f.options.load = () => loading.promise;
  const pending = openWorldTerrain(f.options);
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
  assert.equal(f.state.finishes, 0);
  cleaned(f);
  assert.equal(clock.pending().length, 0);
});

test("expiration during synchronous runtime loading cannot allocate", async (t) => {
  const clock = fakeTimers(t);
  const f = fixture();
  f.options.load = () => { clock.advance(1000); return f.Probe; };
  await assert.rejects(openWorldTerrain(f.options), timedOut);
  assert.equal(f.state.created, 0);
  assert.equal(f.state.socket, undefined);
  assert.equal(clock.pending().length, 0);
});

test("runtime errors, invalid exports and preparation failures use static errors", async () => {
  for (const load of [() => { throw new Error(secret); }, async () => { throw new Error(secret); }, async () => undefined, async () => ({ private: secret })]) {
    const f = fixture();
    await assert.rejects(openWorldTerrain({ ...f.options, load }), unavailable);
    assert.equal(f.state.created, 0);
    assert.equal(f.state.socket, undefined);
  }
  const f = fixture({ errorAt: "constructor" });
  await assert.rejects(openWorldTerrain(f.options), unavailable);
  assert.equal(f.state.created, 1);
  assert.equal(f.state.socket, undefined);
});

test("abort on runtime resolution or probe creation prevents sockets and frees completed allocations once", async () => {
  for (const point of ["load", "constructor"]) {
    const f = fixture();
    const abort = new AbortController();
    f.state.hooks = { [point]: () => abort.abort(secret) };
    await assert.rejects(openWorldTerrain({ ...f.options, signal: abort.signal }), cancelled);
    assert.equal(f.state.created, point === "constructor" ? 1 : 0);
    assert.equal(f.state.freed, point === "constructor" ? 1 : 0);
    assert.equal(f.state.socket, undefined);
  }
});

test("socket construction failures free the probe and never expose thrown errors", async () => {
  const f = fixture();
  f.options.WebSocketClass = class { constructor() { throw new Error(secret); } };
  await assert.rejects(openWorldTerrain(f.options), unavailable);
  assert.equal(f.state.freed, 1);
});

test("abort during socket construction closes the late socket and frees exactly once", async () => {
  const abort = new AbortController();
  const f = fixture({ hooks: { socket: () => abort.abort(secret) } });
  await assert.rejects(openWorldTerrain({ ...f.options, signal: abort.signal }), cancelled);
  cleaned(f);
});

test("binary-only input and the inclusive 4 MiB per-message bound are enforced and wiped", async () => {
  for (const data of ["private server text", new ArrayBuffer(4 * MIB + 1), new Uint8Array([7]), null]) {
    const f = fixture();
    const pending = openWorldTerrain(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    f.state.socket.onmessage({ data });
    await rejected;
    if (data instanceof ArrayBuffer) assert.ok(new Uint8Array(data).every((value) => value === 0));
    assert.deepEqual(f.state.feeds, []);
    cleaned(f);
  }
  const f = fixture();
  f.state.completeAfter = Infinity;
  const pending = openWorldTerrain(f.options);
  const rejected = assert.rejects(pending, unavailable);
  await opened(f);
  const bytes = new Uint8Array(4 * MIB).fill(7);
  f.state.socket.onmessage({ data: bytes.buffer });
  assert.equal(f.state.received, 4 * MIB);
  assert.ok(bytes.every((value) => value === 0));
  f.state.socket.onerror();
  await rejected;
  cleaned(f);
});

test("cumulative wire input is bounded to exactly 32 MiB before feeding the next byte", async () => {
  const f = fixture({ completeAfter: Infinity });
  const pending = openWorldTerrain(f.options);
  const rejected = assert.rejects(pending, unavailable);
  await opened(f);
  for (let at = 0; at < 8; at++) {
    const bytes = new Uint8Array(4 * MIB);
    f.state.socket.onmessage({ data: bytes.buffer });
  }
  assert.equal(f.state.received, 32 * MIB);
  assert.equal(f.state.feeds.length, 8);
  message(f, [1]);
  await rejected;
  assert.equal(f.state.received, 32 * MIB);
  cleaned(f);
});

test("abnormal close, normal premature close and relay errors never call finish or publish", async () => {
  for (const event of ["normal", "abnormal", "error"]) {
    const f = fixture();
    const pending = openWorldTerrain(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    if (event === "error") f.state.socket.onerror(new Error(secret));
    else f.state.socket.onclose({ code: event === "normal" ? 1000 : 1006, wasClean: event === "normal", reason: secret });
    await rejected;
    assert.equal(f.state.finishes, 0);
    assert.equal(f.state.reports, 0);
    assert.deepEqual(f.state.compose, []);
    cleaned(f);
  }
});

test("all transport, finish, composition and report exceptions fail safely with complete cleanup", async () => {
  for (const errorAt of ["outgoing", "send", "feed", "complete", "finish", "compose", "report"]) {
    const f = fixture({ errorAt });
    const pending = openWorldTerrain(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    if (!["outgoing", "send"].includes(errorAt)) message(f);
    if (["finish", "compose", "report"].includes(errorAt)) message(f);
    await rejected;
    cleaned(f);
  }
});

test("only boolean completion can trigger finish and composition", async () => {
  for (const completeValue of [1, "true", null, {}]) {
    const f = fixture({ completeValue });
    const pending = openWorldTerrain(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    message(f);
    await rejected;
    assert.equal(f.state.finishes, 0);
    assert.deepEqual(f.state.compose, []);
    cleaned(f);
  }
});

test("a dense map reference reports its full bounded scan separately from retained regions", async () => {
  const f = fixture({ mutateReport(report) {
    report.referenceArchives = 262144;
    report.referenceFiles = 262144;
  } });
  const owner = await retained(f);
  try {
    const report = owner.state().report;
    assert.equal(report.referenceArchives, 262144);
    assert.equal(report.referenceFiles, 262144);
    assert.equal(report.retainedArchives, 4);
    assert.equal(report.limits.maxReferenceRecords, 524288);
    assert.equal(report.limits.maxRetainedArchives, 36);
    assert.equal(report.limits.maxRetainedFiles, 4096);
  } finally { owner.close(); }
});

test("malformed reports and forged integrity claims cannot produce a retained owner", async () => {
  const mutations = [
    (r) => { r.index = 6; }, (r) => { r.authenticated = true; },
    (r) => { r.referenceCrcVerified = false; }, (r) => { r.referenceWhirlpoolVerified = false; },
    (r) => { r.referenceRevision = secret; }, (r) => { r.totalWireBytes++; },
    (r) => { r.referenceArchives = 262145; }, (r) => { r.referenceFiles = 262145; },
    (r) => { r.referenceArchives = "4"; }, (r) => { r.referenceFiles = 0; },
    (r) => { r.retainedArchives = 37; }, (r) => { r.retainedArchives--; },
    (r) => { r.limits.maxReferenceArchives++; }, (r) => { r.limits.maxReferenceFiles++; },
    (r) => { r.limits.maxReferenceRecords++; }, (r) => { r.limits.maxRetainedArchives++; }, (r) => { r.limits.maxRetainedFiles++; },
    (r) => { r.totalContainerBytes = 24 * MIB + 1; }, (r) => { r.totalExpandedBytes = -1; }, (r) => { r.totalOperations = "0"; },
    (r) => { r.regions.pop(); }, (r) => { r.regions[0].regionId++; }, (r) => { r.regions[0].origin[0]++; },
    (r) => { r.regions[0].crcVerified = false; }, (r) => { r.regions[0].effectsDecoded = true; },
    (r) => { r.regions[0].whirlpoolVerified = "false"; }, (r) => { r.regions[0].tileCount = 0; },
    (r) => { r.regions[0].terrainBytes = MIB + 1; }, (r) => { r.regions[0].archiveId = null; },
    (r) => { r.regions[0].present = false; }, (r) => { r.regions[0].expandedBytes = 0; },
    (r) => { r.limits.maxRegions = 37; }, (r) => { r.limits.tileTagLimitIsDiagnostic = false; }, (r) => { r.limits.operationUnit = secret; },
  ];
  for (const mutateReport of mutations) {
    const f = fixture({ mutateReport });
    const pending = openWorldTerrain(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    message(f);
    message(f);
    await rejected;
    cleaned(f);
  }
  for (const reportText of ["{", JSON.stringify(secret), "null", "[]", "x".repeat(65537)]) {
    const f = fixture({ reportText });
    const pending = openWorldTerrain(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    message(f);
    message(f);
    await rejected;
    cleaned(f);
  }
});

test("abort in feed, send, completion, finish, composition, report and socket closing cannot publish late", async () => {
  for (const point of ["feed", "send", "complete", "finish", "compose", "report", "close"]) {
    const abort = new AbortController();
    const f = fixture();
    const pending = openWorldTerrain({ ...f.options, signal: abort.signal });
    const rejected = assert.rejects(pending, cancelled);
    await opened(f);
    f.state.hooks = { [point]: () => abort.abort(secret) };
    message(f);
    if (["finish", "compose", "report", "close"].includes(point)) message(f);
    await rejected;
    cleaned(f);
    assert.equal(f.state.reports, ["report", "close"].includes(point) ? 1 : 0);
  }
});

test("abort and explicit close after retention preserve history but revoke all accessors", async () => {
  const f = fixture();
  const abort = new AbortController();
  f.options.signal = abort.signal;
  const owner = await retained(f);
  abort.abort(secret);
  assert.equal(owner.state().error, cancelled.message);
  assert.equal(owner.state().closed, true);
  assert.equal(owner.state().accepted, false);
  assert.equal(JSON.stringify(owner.state()).includes(secret), false);
  assert.throws(() => owner.height(12850, 0, 0, 0), /owner is closed/);
  owner.close();
  cleaned(f);
});

test("retained owner lifetime closes and frees once, and explicit close cancels every timer", async (t) => {
  const clock = fakeTimers(t);
  const f = fixture();
  const owner = await retained(f);
  assert.equal(clock.pending(1000).length, 0);
  assert.equal(clock.pending(2000).length, 1);
  clock.fire(clock.pending(2000)[0]);
  assert.equal(owner.state().accepted, false);
  assert.equal(owner.state().closed, true);
  owner.close();
  assert.equal(clock.pending().length, 0);
  cleaned(f);
  const next = fixture();
  const other = await retained(next);
  other.close();
  assert.equal(clock.pending().length, 0);
  cleaned(next);
});

test("late transport callbacks are inert after failure and successful retention", async () => {
  for (const success of [false, true]) {
    const f = fixture();
    const abort = new AbortController();
    const pending = openWorldTerrain({ ...f.options, signal: abort.signal });
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
    const calls = { feeds: f.state.feeds.length, sends: f.state.sent.length, reports: f.state.reports, finishes: f.state.finishes };
    onopen();
    const late = new Uint8Array([8, 9]);
    onmessage({ data: late.buffer });
    onerror(new Error(secret));
    onclose({ code: 1000, wasClean: true, reason: secret });
    assert.deepEqual([...late], [0, 0]);
    assert.deepEqual({ feeds: f.state.feeds.length, sends: f.state.sent.length, reports: f.state.reports, finishes: f.state.finishes }, calls);
    if (owner) {
      assert.equal(owner.state().accepted, true);
      assert.equal(owner.height(12850, 0, 0, 0), -432);
      owner.close();
    }
    cleaned(f);
  }
});

test("height accessors validate all noncoercing bounds without destroying terrain ownership", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    const invalid = [undefined, null, NaN, Infinity, -1, 1.5, "0", true, 1n, { valueOf() { assert.fail("must not coerce"); } }];
    for (const value of invalid) {
      for (let at = 0; at < 4; at++) {
        const args = [12850, 0, 0, 0];
        args[at] = value;
        assert.throws(() => owner.height(...args), /invalid world terrain coordinates/);
      }
      for (let at = 0; at < 3; at++) {
        const args = [0, 0, 0];
        args[at] = value;
        assert.throws(() => owner.sceneHeight(...args), /invalid world terrain coordinates/);
      }
    }
    for (const args of [[65536, 0, 0, 0], [12849, 0, 0, 0], [12850, 4, 0, 0], [12850, 0, 64, 0], [12850, 0, 0, 64]]) assert.throws(() => owner.height(...args), /invalid world terrain coordinates/);
    for (const args of [[4, 0, 0], [0, 73, 0], [0, 0, 73]]) assert.throws(() => owner.sceneHeight(...args), /invalid world terrain coordinates/);
    assert.deepEqual(f.state.height, []);
    assert.deepEqual(f.state.sceneHeight, []);
    assert.equal(owner.height(13107, 3, 63, 63), -432);
    assert.equal(owner.sceneHeight(3, 72, 72), -876);
    assert.deepEqual(f.state.height, [[13107, 3, 63, 63]]);
    assert.deepEqual(f.state.sceneHeight, [[3, 72, 72]]);
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
  } finally { owner.close(); }
});

test("only actual null samples map to null; invalid API results and exceptions remain static non-destructive errors", async () => {
  const f = fixture();
  const owner = await retained(f);
  try {
    f.state.heightValue = null;
    f.state.sceneValue = null;
    assert.equal(owner.height(12850, 0, 0, 0), null);
    assert.equal(owner.sceneHeight(0, 0, 0), null);
    for (const value of [undefined, "0", false, {}, NaN, Infinity, 1.5, 0x80000000, -0x80000001]) {
      f.state.heightValue = value;
      f.state.sceneValue = value;
      assert.throws(() => owner.height(12850, 0, 0, 0), { message: "world terrain sample unavailable" });
      assert.throws(() => owner.sceneHeight(0, 0, 0), { message: "world terrain sample unavailable" });
    }
    f.state.errorAt = "height";
    assert.throws(() => owner.height(12850, 0, 0, 0), { message: "world terrain sample unavailable" });
    f.state.errorAt = "sceneHeight";
    assert.throws(() => owner.sceneHeight(0, 0, 0), { message: "world terrain sample unavailable" });
    f.state.errorAt = undefined;
    f.state.heightValue = 0;
    f.state.sceneValue = -0x80000000;
    assert.equal(owner.height(12850, 0, 0, 0), 0);
    assert.equal(owner.sceneHeight(0, 0, 0), -0x80000000);
    assert.equal(owner.state().accepted, true);
    assert.equal(f.state.freed, 0);
  } finally { owner.close(); }
});

test("actual composed surface counts and availability are retained instead of synthesized from requests", async () => {
  const f = fixture({ mutateSurface(surface) {
    surface.knownHeights = surface.heightCount - 107;
    surface.unavailableHeights = 107;
    surface.regions[1].availability = "provenAbsent";
    surface.regions[2].availability = "unavailable";
    surface.privateServerValue = secret;
    surface.regions[0].privateMapKey = secret;
  } });
  const owner = await retained(f);
  try {
    assert.deepEqual(owner.state().surface, {
      scope: "surface-only", baseTile: [3200, 3200], mapSize: 72, stride: 73, planes: 4,
      heightCount: 4 * 73 * 73, knownHeights: 4 * 73 * 73 - 107, unavailableHeights: 107,
      regions: [
        { regionId: 12850, availability: "present" },
        { regionId: 12851, availability: "provenAbsent" },
        { regionId: 13106, availability: "unavailable" },
        { regionId: 13107, availability: "present" },
      ],
      effectsDecoded: false, renderedWorld: false,
    });
    assert.equal(JSON.stringify(owner.state()).includes(secret), false);
  } finally { owner.close(); }
});

test("malformed composed surface summaries cannot publish an owner", async () => {
  const mutations = [
    (s) => { s.scope = "full-world"; }, (s) => { s.baseTile[0]++; }, (s) => { s.baseTile = { x: 3200, y: 3200 }; },
    (s) => { s.mapSize = "72"; }, (s) => { s.stride = 72; }, (s) => { s.planes = 3; },
    (s) => { s.heightCount--; }, (s) => { s.knownHeights++; }, (s) => { s.knownHeights = -1; },
    (s) => { s.unavailableHeights = "17"; }, (s) => { s.regions.reverse(); }, (s) => { s.regions.pop(); },
    (s) => { s.regions[0].availability = secret; }, (s) => { s.regions[0].regionId = "12850"; },
    (s) => { s.effectsDecoded = true; }, (s) => { s.renderedWorld = true; },
  ];
  for (const mutateSurface of mutations) {
    const f = fixture({ mutateSurface });
    const pending = openWorldTerrain(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    message(f);
    message(f);
    await rejected;
    cleaned(f);
  }
  for (const surfaceText of ["{", JSON.stringify(secret), "null", "[]", "x".repeat(65537)]) {
    const f = fixture({ surfaceText });
    const pending = openWorldTerrain(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    message(f);
    message(f);
    await rejected;
    cleaned(f);
  }
});

test("proven absent terrain is safe metadata, not a fabricated regional zero-height accessor", async () => {
  const f = fixture({ mutateReport(report) {
    report.retainedArchives--;
    Object.assign(report.regions[0], {
      present: false, archiveId: null, archiveRevision: null,
      containerBytes: 0, expandedBytes: 0, terrainBytes: 0, suffixBytes: 0, tileCount: 0, operations: 0,
      crcVerified: false, whirlpoolVerified: false,
    });
  }, mutateSurface(surface) { surface.regions[0].availability = "provenAbsent"; } });
  const owner = await retained(f);
  try {
    assert.equal(owner.state().report.regions[0].present, false);
    assert.equal(owner.state().surface.regions[0].availability, "provenAbsent");
    f.state.errorAt = "height";
    assert.throws(() => owner.height(12850, 0, 0, 0), { message: "world terrain sample unavailable" });
    f.state.sceneValue = 0;
    assert.equal(owner.sceneHeight(0, 0, 0), 0);
    assert.equal(owner.state().accepted, true);
  } finally { owner.close(); }
});

test("outgoing arrays are wiped on send failure, cancellation and rejected byte shapes", async () => {
  for (const outgoingValue of [() => new Uint8Array(4 * MIB + 1).fill(7), () => new Uint16Array([7, 8]), () => new DataView(new Uint8Array([7, 8]).buffer)]) {
    const f = fixture({ outgoingValue });
    const pending = openWorldTerrain(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await opened(f);
    await rejected;
    for (const value of f.state.outgoing) assert.ok(new Uint8Array(value.buffer).every((byte) => byte === 0));
    assert.equal(f.state.freed, 1);
    assert.equal(f.state.closed, 1);
  }
  for (const point of ["send", "outgoing"]) {
    const f = fixture();
    const abort = new AbortController();
    if (point === "send") f.state.errorAt = "send";
    else f.state.hooks = { outgoing: () => abort.abort(secret) };
    const pending = openWorldTerrain({ ...f.options, signal: abort.signal });
    const rejected = assert.rejects(pending, point === "send" ? unavailable : cancelled);
    await opened(f);
    await rejected;
    cleaned(f);
  }
});

test("saved callbacks fired synchronously by socket close cannot invalidate verified completion", async () => {
  const f = fixture();
  const pending = openWorldTerrain(f.options);
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
    onmessage({ get data() { throw new Error(secret); } });
    assert.equal(owner.state().accepted, true);
  } finally { owner.close(); }
  cleaned(f);
});

test("the entire completion path is within the original deadline, including composition", async (t) => {
  const clock = fakeTimers(t);
  const f = fixture();
  const pending = openWorldTerrain(f.options);
  const rejected = assert.rejects(pending, timedOut);
  await opened(f);
  message(f);
  f.state.hooks = { compose: () => clock.advance(1000) };
  message(f);
  await rejected;
  assert.equal(f.state.reports, 0);
  assert.equal(clock.pending().length, 0);
  cleaned(f);
});

test("cleanup exceptions cannot leak server text or cause repeated cleanup", async () => {
  const f = fixture({ errorAt: "close" });
  const owner = await retained(f);
  f.state.errorAt = "free";
  owner.close();
  owner.close();
  assert.equal(owner.state().closed, true);
  assert.equal(JSON.stringify(owner.state()).includes(secret), false);
  cleaned(f);
});
