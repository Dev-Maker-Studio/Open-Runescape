import test from "node:test";
import assert from "node:assert/strict";
import { setImmediate } from "node:timers/promises";
import { openWorldTerrain } from "../static/world-terrain.js";
import { openWorldFloors, prepareWorldFloorGround } from "../static/world-floors.js";
import { openWorldMaterials, prepareWorldMaterialGround } from "../static/world-materials.js";

const MIB = 1024 * 1024;
const MAX_VERTICES = 1016064;
const secret = "FAKE_PRIVATE_GROUND_SECRET";
const unavailable = { message: "world ground preparation unavailable" };
const tileUnavailable = { message: "world ground tile unavailable" };
const invalidPreparation = { message: "invalid world ground preparation" };
const invalidCoordinates = { message: "invalid world ground coordinates" };
const invalidQuery = { message: "invalid world ground query" };
const closed = { message: "world terrain owner is closed" };
const initial = () => ({ base: { x: 3200, y: 3200 }, mapSize: 72, mapKind: "standard", regionIds: [12850, 12851, 13106, 13107], chunkCount: 0 });
const summaryKeys = ["scope", "plane", "mapSize", "tileCount", "vertexCount", "emittedVertexCount", "terrainLowered", "texturePixelsDecoded", "renderedWorld"];
const tileKeys = ["x", "y", "shape", "rotation", "splitEdges", "vertices", "blendingColors", "emittedVertices", "hasShadows"];
const vertexKeys = ["offsetX", "offsetZ", "mainColor", "texture", "textureScale"];

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

function paletteFor(size) {
  return { scope: "underlay-packed-hsl", mapSize: size, planes: 4, tileCount: 4 * size * size, knownColors: 4 * size * size, unavailableColors: 0, unavailableDefinitionIds: [], rgbPrepared: false, renderedWorld: false };
}

function summaryFor(plane = 0, mapSize = 72, tileCount = 1, vertexCount = 6, emittedVertexCount = 3) {
  return { scope: "blended-surface-geometry", plane, mapSize, tileCount, vertexCount, emittedVertexCount, terrainLowered: false, texturePixelsDecoded: false, renderedWorld: false };
}

function tileFor(x = 0, y = 0, count = 6) {
  return {
    x, y, shape: 14, rotation: 3, splitEdges: [true, false, true, false],
    vertices: Array.from({ length: count }, (_, at) => ({ offsetX: at % 2 ? 512 : 0, offsetZ: at % 2 ? 0 : 512, mainColor: at % 2 ? 65535 : -1, texture: at % 2 ? 65534 : -1, textureScale: at % 2 ? 262140 : 0 })),
    blendingColors: Array.from({ length: count }, (_, at) => at % 2 ? 65535 : -1),
    emittedVertices: 3,
    hasShadows: true,
  };
}

const terrainModes = ["close", "abort", "deadline", "timer"];
const dependencyModes = ["floors", "materials", "floorsAbort", "materialsAbort"];
const allModes = [...terrainModes, ...dependencyModes];
const relay = "ws://127.0.0.1:8110/js5";
const TERRAIN_LIFETIME = 5000;
const FLOOR_LIFETIME = 3000;
const MATERIAL_LIFETIME = 4000;

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

function materialReportFor() {
  return {
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
}

function hookedSignal(state, name) {
  const controller = new AbortController();
  return {
    get aborted() {
      state.dependencyReads.push(name);
      state.hooks[name]?.();
      return controller.signal.aborted;
    },
    addEventListener: (...args) => controller.signal.addEventListener(...args),
    removeEventListener: (...args) => { state.hooks[name + ":remove"]?.(); return controller.signal.removeEventListener(...args); },
    abort: (reason) => controller.abort(reason),
  };
}

function floorKit(state, hookName, lifetimeMs = FLOOR_LIFETIME) {
  const own = { received: 0, feeds: 0, freed: 0, closed: 0, hooks: {} };
  class FloorProbe {
    constructor() { own.probe = this; }
    outgoing() { return new Uint8Array([15]); }
    feed(bytes) { own.received += bytes.length; own.feeds++; }
    complete() { return own.feeds === 2; }
    finish() {}
    report() { return JSON.stringify(floorReportFor(own.received)); }
    underlay() { return "null"; }
    overlay() { return "null"; }
    free() { own.hooks.free?.(); own.freed++; }
  }
  class Socket {
    constructor() { own.socket = this; }
    send() {}
    close() { own.hooks.close?.(); own.closed++; }
  }
  const signal = hookedSignal(state, hookName);
  return { own, signal, open: openWorldFloors, options: { js5Relay: relay, load: async () => FloorProbe, WebSocketClass: Socket, timeoutMs: 1000, lifetimeMs, signal } };
}

function materialKit(state, hookName, lifetimeMs = MATERIAL_LIFETIME) {
  const own = { received: 0, feeds: 0, freed: 0, closed: 0, hooks: {} };
  class MaterialProbe {
    constructor() { own.probe = this; }
    outgoing() { return new Uint8Array([15]); }
    feed(bytes) { own.received += bytes.length; own.feeds++; }
    complete() { return own.feeds === 2; }
    finish() {}
    report() { return JSON.stringify(materialReportFor()); }
    material() { throw "material not found: 0"; }
    free() { own.hooks.free?.(); own.freed++; }
  }
  class Socket {
    constructor() { own.socket = this; }
    send() {}
    close() { own.hooks.close?.(); own.closed++; }
  }
  const signal = hookedSignal(state, hookName);
  return { own, signal, open: openWorldMaterials, options: { js5Relay: relay, load: async () => MaterialProbe, WebSocketClass: Socket, timeoutMs: 1000, lifetimeMs, signal } };
}

async function openKit(kit) {
  const pending = kit.open(kit.options);
  await setImmediate();
  assert.ok(kit.own.socket);
  kit.own.socket.onopen();
  for (let at = 0; at < 2; at++) {
    const bytes = new Uint8Array([1]);
    kit.own.socket.onmessage({ data: bytes.buffer });
    assert.equal(bytes[0], 0);
  }
  return pending;
}

function assertClosedKit(kit) {
  assert.equal(kit.own.freed, 1);
  assert.equal(kit.own.closed, 1);
}

function fixture() {
  const state = {
    received: 0, feeds: 0, freed: 0, closed: 0, prepares: [], prepareThis: [], tiles: [], dependencyReads: [], hooks: {},
  };
  const floorsKit = floorKit(state, "floorState");
  const materialsKit = materialKit(state, "materialState");
  const fault = (name, ...args) => {
    state.hooks[name]?.(...args);
    if (state.errorAt === name) throw new Error(secret);
  };
  class Probe {
    constructor(token, regions) { state.probe = this; state.regions = JSON.parse(regions); }
    outgoing() { return new Uint8Array([15]); }
    feed(bytes) { state.received += bytes.length; state.feeds++; }
    complete() { return state.feeds === 2; }
    finish() {}
    compose(x, y, size) { return JSON.stringify(surfaceFor(state, x, y, size)); }
    report() { return JSON.stringify(reportFor(state)); }
    height() { return 0; }
    sceneHeight() { return 0; }
    sceneTile() { return "null"; }
    composePalette(floorProbe) {
      assert.equal(floorProbe, floorsKit.own.probe);
      return JSON.stringify(paletteFor(options.initialWorld.mapSize));
    }
    paletteColor() { return 0; }
    prepareGround(...args) {
      state.prepares.push(args);
      state.prepareThis.push(this);
      fault("prepareGround", ...args);
      const [, , plane] = args;
      return Object.hasOwn(state, "summaryValue") ? state.summaryValue : JSON.stringify(summaryFor(plane, options.initialWorld.mapSize));
    }
    groundTile(...args) {
      state.tiles.push(args);
      fault("groundTile", ...args);
      return Object.hasOwn(state, "tileValue") ? state.tileValue : JSON.stringify(tileFor(args[1], args[2]));
    }
    free() { state.hooks.free?.(); state.freed++; }
  }
  class Socket {
    constructor() { state.socket = this; }
    send() {}
    close() { state.hooks.socketClose?.(); state.closed++; }
  }
  const abort = new AbortController();
  const options = {
    js5Relay: relay, initialWorld: initial(), floors: null,
    load: async () => Probe, WebSocketClass: Socket, timeoutMs: 1000, lifetimeMs: TERRAIN_LIFETIME,
    signal: {
      get aborted() { return abort.signal.aborted; },
      addEventListener: (...args) => abort.signal.addEventListener(...args),
      removeEventListener: (...args) => { state.hooks["terrain:remove"]?.(); return abort.signal.removeEventListener(...args); },
    },
  };
  return { state, options, Probe, floorsKit, materialsKit, abort, floors: null, materials: null, floorProbe: null, materialProbe: null };
}

async function retained(f) {
  f.floors = await openKit(f.floorsKit);
  f.materials = await openKit(f.materialsKit);
  f.floorProbe = f.floorsKit.own.probe;
  f.materialProbe = f.materialsKit.own.probe;
  if (Object.hasOwn(f.options, "floors")) f.options.floors = f.floors;
  const pending = openWorldTerrain(f.options);
  await setImmediate();
  assert.ok(f.state.socket);
  f.state.socket.onopen();
  for (let at = 0; at < 2; at++) {
    const bytes = new Uint8Array([1]);
    f.state.socket.onmessage({ data: bytes.buffer });
    assert.equal(bytes[0], 0);
  }
  return pending;
}

async function usingTerrain(callback, setup = () => {}) {
  const f = fixture();
  setup(f);
  const owner = await retained(f);
  try { await callback(f, owner); }
  finally {
    owner.close();
    f.floors.close();
    f.materials.close();
    assert.equal(f.state.freed, 1);
    assert.equal(f.state.closed, 1);
    assertClosedKit(f.floorsKit);
    assertClosedKit(f.materialsKit);
  }
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
    advance: (amount) => { now += amount; },
    pending: () => timers.filter((timer) => timer.active),
    expire() {
      const timer = timers.find((timer) => timer.active && timer.delay === TERRAIN_LIFETIME);
      assert.ok(timer);
      timer.active = false;
      timer.callback();
    },
  };
}

function assertNoOwners(value, f) {
  if (value === null || typeof value !== "object") return;
  for (const privateValue of [f.floors, f.materials, f.floorProbe, f.materialProbe, f.state.probe]) assert.notEqual(value, privateValue);
  for (const child of Object.values(value)) assertNoOwners(child, f);
  assert.equal(JSON.stringify(value).includes(secret), false);
}

function assertFrozenTile(tile) {
  assert.deepEqual(Object.keys(tile).sort(), [...tileKeys].sort());
  assert.equal(Object.getPrototypeOf(tile), Object.prototype);
  assert.ok(Object.isFrozen(tile));
  for (const value of [tile.splitEdges, tile.vertices, ...(tile.blendingColors === null ? [] : [tile.blendingColors]), ...tile.vertices]) assert.ok(Object.isFrozen(value));
  for (const vertex of tile.vertices) {
    assert.deepEqual(Object.keys(vertex).sort(), [...vertexKeys].sort());
    assert.equal(Object.getPrototypeOf(vertex), Object.prototype);
  }
}

const nonIntegers = () => [undefined, null, false, true, "0", "1", NaN, Infinity, -Infinity, 0.5, 1n, Symbol(), new Number(0), [], {}, { valueOf() { assert.fail("must not coerce"); }, toString() { assert.fail("must not coerce"); } }];
const malformedOutputs = () => [undefined, null, false, 1, {}, [], Symbol(), new String("null"), Promise.resolve("null"), { toString() { assert.fail("must not coerce output"); }, toJSON() { assert.fail("must not stringify output"); } }, "", "{", "undefined", "NaN", "null trailing", "[]", "[null]", "true", "0", JSON.stringify(secret), JSON.stringify("null")];

function invalidatedBy(mode, f, owner, clock) {
  if (mode === "close") owner.close();
  if (mode === "abort") f.abort.abort(secret);
  if (mode === "deadline") clock.advance(TERRAIN_LIFETIME);
  if (mode === "timer") clock.expire();
  if (mode === "floors") f.floors.close();
  if (mode === "materials") f.materials.close();
  if (mode === "floorsAbort") f.floorsKit.signal.abort(secret);
  if (mode === "materialsAbort") f.materialsKit.signal.abort(secret);
}

function assertRevoked(f, owner, mode) {
  assert.equal(owner.groundState(), null);
  assert.equal(Object.hasOwn(owner.state(), "ground"), false);
  const terrainClosed = terrainModes.includes(mode);
  assert.equal(owner.state().closed, terrainClosed);
  assert.equal(owner.state().accepted, !terrainClosed);
  assert.throws(() => owner.groundTile(0, 0, 0), terrainClosed ? closed : tileUnavailable);
  assert.throws(() => owner.prepareGround(f.floors, f.materials), terrainClosed ? closed : unavailable);
  assertNoOwners(owner.state(), f);
}

function acceptingState() {
  return { accepted: true, closed: false };
}

function forgedOwners(f) {
  return [
    undefined, null, false, 0, "owner", {},
    { state: acceptingState },
    { state: acceptingState, composeTerrain: () => paletteFor(72), close() {} },
    { ...f.floors }, { ...f.materials },
    Object.create(f.floors), Object.create(f.materials),
    new Proxy(f.floors, {}), new Proxy(f.materials, {}),
    Object.freeze({ state: f.floors.state, close: f.floors.close, composeTerrain: f.floors.composeTerrain }),
    Object.freeze({ state: f.materials.state, close: f.materials.close, material: f.materials.material }),
    structuredClone(f.floors.state()),
  ];
}

function recordingProxy(seen, gets) {
  return new Proxy({}, {
    get(_, key) { gets.push(key); return (...args) => { seen.push(args); return acceptingState(); }; },
    has(_, key) { gets.push(key); return true; },
    getPrototypeOf() { gets.push("getPrototypeOf"); return Object.prototype; },
  });
}

function probesOf(f) {
  return [f.floorProbe, f.materialProbe, f.state.probe];
}

function assertNoProbe(f, seen) {
  for (const args of seen) for (const arg of args) for (const probe of probesOf(f)) assert.notEqual(arg, probe);
}

test("ground starts unavailable and preparation synchronously publishes only a frozen bounded summary", async () => {
  await usingTerrain((f, owner) => {
    assert.equal(owner.groundState(), null);
    assert.equal(Object.hasOwn(owner.state(), "ground"), false);
    assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable);
    assert.deepEqual(f.state.tiles, []);
    const prepared = owner.prepareGround(f.floors, f.materials);
    assert.deepEqual(prepared, summaryFor());
    assert.equal(prepared instanceof Promise, false);
    assert.ok(Object.isFrozen(prepared));
    assert.deepEqual(Object.keys(prepared).sort(), [...summaryKeys].sort());
    assert.throws(() => { prepared.plane = 3; }, TypeError);
    assert.deepEqual(owner.groundState(), prepared);
    assert.deepEqual(f.state.prepares, [[f.floorProbe, f.materialProbe, 0, MAX_VERTICES]]);
    assert.equal(f.state.prepares.length, 1);
    assert.equal(f.state.prepares[0].length, 4);
    assert.equal(f.state.prepares[0][0], f.floorProbe);
    assert.equal(f.state.prepares[0][1], f.materialProbe);
    assert.notEqual(f.floorProbe, f.materialProbe);
    assert.notEqual(f.floorProbe, f.state.probe);
    assert.notEqual(f.materialProbe, f.state.probe);
    assert.equal(f.state.prepareThis[0], f.state.probe);
    const state = owner.state();
    assert.deepEqual(state.ground, prepared);
    state.ground.vertexCount = 0;
    assert.deepEqual(owner.groundState(), summaryFor());
    assertNoOwners(state, f);
    assertNoOwners(prepared, f);
    assert.equal(state.terrainLowered, undefined);
    assert.equal(state.renderedWorld, false);
  });
});

test("success replaces a single prepared plane while invalid arguments and native failures preserve the previous plane", async () => {
  await usingTerrain((f, owner) => {
    const before = owner.prepareGround(f.floors, f.materials);
    const tile = owner.groundTile(0, 0, 0);
    assert.throws(() => owner.prepareGround(f.floors, f.materials, 4), invalidPreparation);
    for (const errorAt of ["prepareGround"]) {
      f.state.errorAt = errorAt;
      const nativeCalls = f.state.prepares.length;
      assert.throws(() => owner.prepareGround(f.floors, f.materials, 1), unavailable);
      assert.equal(f.state.prepares.length, nativeCalls + 1);
      assert.deepEqual(owner.groundState(), before);
      assert.deepEqual(owner.groundTile(0, 0, 0), tile);
      assert.throws(() => owner.groundTile(1, 0, 0), tileUnavailable);
    }
    f.state.errorAt = undefined;
    assert.deepEqual(owner.prepareGround(f.floors, f.materials, 3), summaryFor(3));
    assert.deepEqual(f.state.prepares.at(-1), [f.floorProbe, f.materialProbe, 3, MAX_VERTICES]);
    for (const plane of [0, 1, 2]) assert.throws(() => owner.groundTile(plane, 0, 0), tileUnavailable);
    assert.deepEqual(owner.groundTile(3, 0, 0), tile);
    assert.deepEqual(before, summaryFor());
    assert.deepEqual(owner.prepareGround(f.floors, f.materials), summaryFor());
  });
});

test("prepareGround validates exact arity and noncoercing optional integers before calling dependencies or native code", async () => {
  await usingTerrain((f, owner) => {
    const before = owner.prepareGround(f.floors, f.materials);
    const calls = f.state.prepares.length;
    const reads = f.state.dependencyReads.length;
    for (const args of [[], [f.floors], [f.floors, f.materials, 0, 6, undefined], [f.floors, f.materials, undefined], [f.floors, f.materials, 0, undefined]]) {
      assert.throws(() => owner.prepareGround(...args), invalidPreparation);
    }
    for (const value of [...nonIntegers(), -1, 4, 4294967296, Number.MAX_SAFE_INTEGER + 1]) assert.throws(() => owner.prepareGround(f.floors, f.materials, value), invalidPreparation);
    for (const value of [...nonIntegers(), -1, MAX_VERTICES + 1, 4294967296, Number.MAX_SAFE_INTEGER + 1]) assert.throws(() => owner.prepareGround(f.floors, f.materials, 0, value), invalidPreparation);
    assert.equal(f.state.prepares.length, calls);
    assert.equal(f.state.dependencyReads.length, reads);
    assert.deepEqual(owner.groundState(), before);
    assert.deepEqual(owner.prepareGround(f.floors, f.materials, -0, 7), summaryFor());
    assert.deepEqual(f.state.prepares.at(-1), [f.floorProbe, f.materialProbe, -0, 7]);
    f.state.summaryValue = JSON.stringify(summaryFor(0, 72, 0, 0, 0));
    assert.deepEqual(owner.prepareGround(f.floors, f.materials, -0, -0), summaryFor(0, 72, 0, 0, 0));
    assert.deepEqual(f.state.prepares.at(-1), [f.floorProbe, f.materialProbe, -0, -0]);
  });
});

test("ground queries reject extra or omitted arguments and validate every coordinate without coercion", async () => {
  for (const mapSize of [72, 104, 120, 136, 168]) {
    await usingTerrain((f, owner) => {
      owner.prepareGround(f.floors, f.materials, 3);
      for (const args of [[undefined], [null], [0], [0, 0], [0, 0, 0, undefined]]) assert.throws(() => owner.groundState(...args), invalidQuery);
      for (const args of [[], [3], [3, 0], [3, 0, 0, undefined]]) assert.throws(() => owner.groundTile(...args), invalidCoordinates);
      for (const value of [...nonIntegers(), -1, 4294967296, Number.MAX_SAFE_INTEGER + 1]) {
        for (let at = 0; at < 3; at++) {
          const args = [3, 0, 0];
          args[at] = value;
          assert.throws(() => owner.groundTile(...args), invalidCoordinates);
        }
      }
      for (const args of [[4, 0, 0], [3, mapSize, 0], [3, 0, mapSize]]) assert.throws(() => owner.groundTile(...args), invalidCoordinates);
      assert.deepEqual(f.state.tiles, []);
      assert.deepEqual(owner.groundTile(3, -0, -0), tileFor());
      assert.deepEqual(owner.groundTile(3, mapSize - 1, mapSize - 1), tileFor(mapSize - 1, mapSize - 1));
      assert.deepEqual(f.state.tiles, [[3, -0, -0], [3, mapSize - 1, mapSize - 1]]);
      assert.deepEqual(owner.groundState(), summaryFor(3, mapSize));
    }, (f) => { f.options.initialWorld.mapSize = mapSize; });
  }
});

test("ground preparation requires the original palette floor owner and valid retained dependencies", async () => {
  await usingTerrain((f, owner) => {
    const before = owner.prepareGround(f.floors, f.materials);
    const calls = f.state.prepares.length;
    for (const floors of [undefined, null, false, {}, { ...f.floors }, { state: f.floors.state, prepareTerrainGround: f.floors.prepareTerrainGround }]) {
      assert.throws(() => owner.prepareGround(floors, f.materials), unavailable);
      assert.deepEqual(owner.groundState(), before);
    }
    for (const materials of [undefined, null, false, {}, { state: null }, { state() { throw new Error(secret); } }]) {
      assert.throws(() => owner.prepareGround(f.floors, materials), unavailable);
      assert.deepEqual(owner.groundState(), before);
    }
    assert.equal(f.state.prepares.length, calls);
  });
  await usingTerrain((f, owner) => {
    assert.throws(() => owner.prepareGround(f.floors, f.materials), unavailable);
    assert.equal(owner.groundState(), null);
    assert.deepEqual(f.state.prepares, []);
  }, (f) => { delete f.options.floors; });
});

test("missing bridge and native ground methods fail statically without replacing a valid plane", async () => {
  for (const [target, name] of [["probe", "prepareGround"], ["probe", "groundTile"]]) {
    for (const value of [undefined, null, 0, "method", {}]) {
      await usingTerrain((f, owner) => {
        const before = owner.prepareGround(f.floors, f.materials);
        const object = f.state.probe;
        const original = object[name];
        object[name] = value;
        const calls = f.state.prepares.length;
        assert.throws(() => owner.prepareGround(f.floors, f.materials, 1), unavailable);
        assert.equal(f.state.prepares.length, calls);
        assert.deepEqual(owner.groundState(), before);
        object[name] = original;
        assert.deepEqual(owner.groundTile(0, 0, 0), tileFor());
      });
    }
  }
});

test("ground summaries accept empty and maximum allocation bounds for every map size and independent emitted counts", async () => {
  for (const mapSize of [72, 104, 120, 136, 168]) {
    await usingTerrain((f, owner) => {
      for (const plane of [0, 1, 2, 3]) {
        const full = mapSize * mapSize;
        for (const [tiles, allocated, emitted] of [[0, 0, 0], [1, 3, 0], [1, 36, 3], [full, full * 3, full * 3], [full, full * 36, full * 36]]) {
          f.state.summaryValue = JSON.stringify(summaryFor(plane, mapSize, tiles, allocated, emitted));
          const actual = owner.prepareGround(f.floors, f.materials, plane, allocated);
          assert.deepEqual(actual, summaryFor(plane, mapSize, tiles, allocated, emitted));
          assert.deepEqual(owner.groundState(), actual);
          assert.ok(Object.isFrozen(actual));
        }
      }
    }, (f) => { f.options.initialWorld.mapSize = mapSize; });
  }
});

test("ground summary output must be bounded JSON with the exact schema and no extra keys", async () => {
  await usingTerrain((f, owner) => {
    const valid = summaryFor(1);
    const invalid = [...malformedOutputs(), "null", " ".repeat(65537) + JSON.stringify(valid)];
    for (const key of summaryKeys) {
      const missing = { ...valid };
      delete missing[key];
      invalid.push(JSON.stringify(missing), JSON.stringify({ ...missing, [key.toUpperCase()]: valid[key] }));
    }
    for (const key of ["privateOwner", "__proto__", "constructor", "prototype", "toString"]) invalid.push(JSON.stringify({ ...valid, [key]: secret }));
    invalid.push(valid, Promise.resolve(valid));
    for (const value of invalid) {
      f.state.summaryValue = value;
      assert.throws(() => owner.prepareGround(f.floors, f.materials, 1), unavailable);
      assert.equal(owner.groundState(), null);
      assert.equal(owner.state().accepted, true);
      assert.equal(f.state.freed, 0);
      assertNoOwners(owner.state(), f);
    }
    delete f.state.summaryValue;
    assert.deepEqual(owner.prepareGround(f.floors, f.materials, 1), valid);
  });
});

test("ground summary numeric relationships, literal flags and fields reject malformed types and bounds", async () => {
  await usingTerrain((f, owner) => {
    const valid = summaryFor();
    const invalid = [];
    for (const key of ["plane", "mapSize", "tileCount", "vertexCount", "emittedVertexCount"]) {
      for (const value of [null, true, false, "0", [], {}, -1, 0.5, 4294967296, Number.MAX_SAFE_INTEGER + 1]) invalid.push(JSON.stringify({ ...valid, [key]: value }));
      invalid.push(JSON.stringify(valid).replace(`"${key}":${valid[key]}`, `"${key}":1e400`));
    }
    for (const [key, values] of [
      ["scope", [null, false, "surface-only", {}, []]], ["plane", [1, 4]], ["mapSize", [0, 73, 104]],
      ["tileCount", [0, 72 * 72 + 1]], ["vertexCount", [0, 2, 4, 37, 39]], ["emittedVertexCount", [1, 4, 9]],
      ...["terrainLowered", "texturePixelsDecoded", "renderedWorld"].map((key) => [key, [true, null, 0, "false", [], {}]]),
    ]) for (const value of values) invalid.push(JSON.stringify({ ...valid, [key]: value }));
    for (const value of invalid) {
      f.state.summaryValue = value;
      assert.throws(() => owner.prepareGround(f.floors, f.materials), unavailable);
      assert.equal(owner.groundState(), null);
    }
    f.state.summaryValue = JSON.stringify(valid);
    assert.throws(() => owner.prepareGround(f.floors, f.materials, 0, 5), unavailable);
    assert.deepEqual(owner.prepareGround(f.floors, f.materials, 0, 6), valid);
  });
});

test("malformed post-native summaries invalidate previous geometry rather than publishing stale metadata", async () => {
  await usingTerrain((f, owner) => {
    const previous = owner.prepareGround(f.floors, f.materials);
    const sample = owner.groundTile(0, 0, 0);
    f.state.summaryValue = JSON.stringify({ ...summaryFor(1), privateOwner: secret });
    assert.throws(() => owner.prepareGround(f.floors, f.materials, 1), unavailable);
    assert.equal(owner.groundState(), null);
    assert.equal(Object.hasOwn(owner.state(), "ground"), false);
    assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable);
    assert.throws(() => owner.groundTile(1, 0, 0), tileUnavailable);
    assert.deepEqual(previous, summaryFor());
    assert.deepEqual(sample, tileFor());
    delete f.state.summaryValue;
    assert.deepEqual(owner.prepareGround(f.floors, f.materials), summaryFor());
  });
});

test("ground tile snapshots are deeply frozen independent exact-schema samples with allocated and emitted lengths kept distinct", async () => {
  await usingTerrain((f, owner) => {
    owner.prepareGround(f.floors, f.materials);
    const first = owner.groundTile(0, 0, 0);
    assert.deepEqual(first, tileFor());
    assert.equal(first.vertices.length, 6);
    assert.equal(first.emittedVertices, 3);
    assertFrozenTile(first);
    for (const mutate of [
      () => { first.shape = 0; }, () => first.vertices.pop(), () => { first.vertices[0].mainColor = 0; },
      () => first.splitEdges.reverse(), () => first.blendingColors.fill(0), () => { first.privateOwner = secret; },
    ]) assert.throws(mutate, TypeError);
    const next = owner.groundTile(0, 0, 0);
    assert.deepEqual(next, first);
    assert.notEqual(next, first);
    for (const key of ["splitEdges", "vertices", "blendingColors"]) assert.notEqual(next[key], first[key]);
    for (let at = 0; at < first.vertices.length; at++) assert.notEqual(next.vertices[at], first.vertices[at]);
    f.state.tileValue = JSON.stringify({ ...tileFor(), shape: 0, hasShadows: false });
    assert.equal(owner.groundTile(0, 0, 0).shape, 0);
    assert.equal(first.shape, 14);
    assertNoOwners(first, f);
    owner.close();
    assert.deepEqual(first, tileFor());
    assert.equal(owner.groundState(), null);
  });
});

test("ground tiles accept all shapes, rotations and triangle allocations including zero true emission and null blending", async () => {
  await usingTerrain((f, owner) => {
    owner.prepareGround(f.floors, f.materials);
    for (let shape = 0; shape <= 14; shape++) {
      for (let rotation = 0; rotation <= 3; rotation++) {
        for (let count = 3; count <= 36; count += 3) {
          for (const emittedVertices of [0, count]) {
            const expected = { ...tileFor(71, 71, count), shape, rotation, emittedVertices, blendingColors: emittedVertices ? null : Array(count).fill(-1), hasShadows: !!rotation };
            f.state.tileValue = JSON.stringify(expected);
            const actual = owner.groundTile(0, 71, 71);
            assert.deepEqual(actual, expected);
            assertFrozenTile(actual);
          }
        }
      }
    }
    f.state.tileValue = "null";
    assert.equal(owner.groundTile(0, 0, 0), null);
    assert.deepEqual(owner.groundState(), summaryFor());
  });
});

test("tile and vertex schemas reject every missing or extra key without destroying prepared ground", async () => {
  await usingTerrain((f, owner) => {
    const prepared = owner.prepareGround(f.floors, f.materials);
    const invalid = [];
    for (const key of tileKeys) {
      const missing = tileFor();
      delete missing[key];
      invalid.push(missing, { ...missing, [key.toUpperCase()]: tileFor()[key] });
    }
    for (const key of vertexKeys) {
      const missing = tileFor();
      delete missing.vertices[0][key];
      invalid.push(missing);
      const renamed = tileFor();
      delete renamed.vertices[0][key];
      renamed.vertices[0][key.toUpperCase()] = tileFor().vertices[0][key];
      invalid.push(renamed);
    }
    for (const key of ["privateOwner", "__proto__", "constructor", "prototype", "toString"]) {
      invalid.push({ ...tileFor(), [key]: secret });
      const tile = tileFor();
      tile.vertices[0] = { ...tile.vertices[0], [key]: secret };
      invalid.push(tile);
    }
    for (const value of invalid) {
      f.state.tileValue = JSON.stringify(value);
      assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable);
      assert.deepEqual(owner.groundState(), prepared);
      assertNoOwners(owner.state(), f);
    }
  });
});

test("ground tile JSON output rejects malformed types, overlong strings and unparsed objects without coercion", async () => {
  await usingTerrain((f, owner) => {
    owner.prepareGround(f.floors, f.materials);
    for (const value of [...malformedOutputs(), tileFor(), Promise.resolve(tileFor()), " ".repeat(16385) + "null"]) {
      f.state.tileValue = value;
      assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable);
      assert.deepEqual(owner.groundState(), summaryFor());
      assert.equal(f.state.freed, 0);
    }
    f.state.tileValue = " ".repeat(16380) + "null";
    assert.equal(owner.groundTile(0, 0, 0), null);
    f.state.errorAt = "groundTile";
    assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable);
    assert.deepEqual(owner.groundState(), summaryFor());
    f.state.errorAt = undefined;
    delete f.state.tileValue;
    assert.deepEqual(owner.groundTile(0, 0, 0), tileFor());
  });
});

test("ground tile fields enforce exact coordinates, booleans, triangle lengths and independent blending and emission bounds", async () => {
  await usingTerrain((f, owner) => {
    owner.prepareGround(f.floors, f.materials);
    const invalid = [];
    for (const [key, values] of [
      ["x", [1, -1, 72, "0", null, false, {}, []]], ["y", [1, -1, 72, "0", null, false, {}, []]],
      ["shape", [-1, 15, 0.5, "0", null, false, {}, [], 4294967296]],
      ["rotation", [-1, 4, 0.5, "0", null, false, {}, [], 4294967296]],
      ["splitEdges", [null, {}, true, "false", [], [true, false, true], [true, false, true, false, true], [0, false, true, false], [true, null, true, false]]],
      ["vertices", [null, {}, "vertices", [], [null, null, null], Array(2).fill({}), Array(4).fill({}), tileFor(0, 0, 39).vertices]],
      ["blendingColors", [false, {}, "colors", [], Array(3).fill(0), Array(9).fill(0), [0, 0, 0, 0, 0, null], [0, 0, 0, 0, 0, false], [0, 0, 0, 0, 0, "0"], Array(6).fill(-2), Array(6).fill(65536), Array(6).fill(0.5)]],
      ["emittedVertices", [-1, 1, 4, 9, "0", null, false, {}, [], 4294967296]],
      ["hasShadows", [null, 0, 1, "true", {}, []]],
    ]) for (const value of values) invalid.push(JSON.stringify({ ...tileFor(), [key]: value }));
    for (const key of ["x", "y", "shape", "rotation", "emittedVertices"]) invalid.push(JSON.stringify(tileFor()).replace(`"${key}":${tileFor()[key]}`, `"${key}":1e400`));
    for (const value of invalid) {
      f.state.tileValue = value;
      assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable);
      assert.deepEqual(owner.groundState(), summaryFor());
    }
  });
});

test("every vertex integer is bounded without truncation and texture scales must remain multiples of four", async () => {
  await usingTerrain((f, owner) => {
    owner.prepareGround(f.floors, f.materials);
    for (const [key, min, max] of [["offsetX", 0, 512], ["offsetZ", 0, 512], ["mainColor", -1, 65535], ["texture", -1, 65534], ["textureScale", 0, 262140]]) {
      for (const value of [null, false, true, "0", {}, [], min - 1, max + 1, 0.5, 4294967296, Number.MAX_SAFE_INTEGER + 1, ...(key === "textureScale" ? [1, 2, 3, 262139] : [])]) {
        const tile = tileFor();
        tile.vertices[0][key] = value;
        f.state.tileValue = JSON.stringify(tile);
        assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable);
      }
      const tile = tileFor();
      f.state.tileValue = JSON.stringify(tile).replace(`"${key}":${tile.vertices[0][key]}`, `"${key}":1e400`);
      assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable);
      for (const value of [min, max]) {
        tile.vertices[0][key] = value;
        f.state.tileValue = JSON.stringify(tile);
        assert.deepEqual(owner.groundTile(0, 0, 0), tile);
      }
    }
  });
});

test("forged dependency reports and owners cannot prepare ground and leave previously published ground untouched", async () => {
  for (const report of [undefined, null, false, [], {}, Promise.resolve({ accepted: true, closed: false }), { accepted: true }, { closed: false }, { accepted: false, closed: false }, { accepted: true, closed: true }, { accepted: 1, closed: false }, { accepted: true, closed: 0 }, { get accepted() { assert.fail("must not evaluate dependency report accessors"); }, closed: false }]) {
    await usingTerrain((f, owner) => {
      const before = owner.prepareGround(f.floors, f.materials);
      const calls = f.state.prepares.length;
      const forged = { state: () => report };
      assert.throws(() => owner.prepareGround(f.floors, forged), unavailable);
      assert.equal(f.state.prepares.length, calls);
      assert.deepEqual(owner.groundState(), before);
      assert.deepEqual(owner.groundTile(0, 0, 0), tileFor());
      assert.equal(owner.state().accepted, true);
    });
  }
});

test("forged floors and materials owners are rejected before any native call and never replace genuine ground", async () => {
  await usingTerrain((f, owner) => {
    const before = owner.prepareGround(f.floors, f.materials);
    const tile = owner.groundTile(0, 0, 0);
    const calls = f.state.prepares.length;
    for (const forged of forgedOwners(f)) {
      assert.throws(() => owner.prepareGround(forged, f.materials), unavailable);
      assert.throws(() => owner.prepareGround(forged, f.materials, 1, 6), unavailable);
      assert.throws(() => owner.prepareGround(f.floors, forged), unavailable);
      assert.throws(() => owner.prepareGround(f.floors, forged, 1, 6), unavailable);
      assert.throws(() => owner.prepareGround(forged, forged), unavailable);
      assert.equal(f.state.prepares.length, calls);
      assert.deepEqual(owner.groundState(), before);
      assert.deepEqual(owner.groundTile(0, 0, 0), tile);
    }
    assert.throws(() => owner.prepareGround(f.materials, f.floors), unavailable);
    assert.throws(() => owner.prepareGround(f.materials, f.materials), unavailable);
    assert.throws(() => owner.prepareGround(f.floors, f.floors), unavailable);
    assert.equal(f.state.prepares.length, calls);
    assert.deepEqual(owner.groundState(), before);
    assertNoOwners(owner.state(), f);
  });
});

test("a genuine floors owner other than the terrain palette owner is rejected and a closed or foreign materials owner cannot prepare", async () => {
  await usingTerrain(async (f, owner) => {
    const before = owner.prepareGround(f.floors, f.materials);
    const calls = f.state.prepares.length;
    const otherFloors = floorKit(f.state, "otherFloorState");
    const floors = await openKit(otherFloors);
    const closedMaterials = materialKit(f.state, "closedMaterialState");
    const closedOwner = await openKit(closedMaterials);
    closedOwner.close();
    try {
      assert.throws(() => owner.prepareGround(floors, f.materials), unavailable);
      assert.throws(() => owner.prepareGround(f.floors, closedOwner), unavailable);
      assert.throws(() => owner.prepareGround(floors, closedOwner), unavailable);
      assert.equal(f.state.prepares.length, calls);
      assert.deepEqual(owner.groundState(), before);
    } finally {
      floors.close();
      closedOwner.close();
    }
    assertClosedKit(otherFloors);
    assertClosedKit(closedMaterials);
  });
});

test("a genuine second materials owner is accepted and the native probe receives exactly its own probe", async () => {
  await usingTerrain(async (f, owner) => {
    const other = materialKit(f.state, "otherMaterialState");
    const materials = await openKit(other);
    try {
      assert.deepEqual(owner.prepareGround(f.floors, materials, 2, 99), summaryFor(2));
      assert.equal(f.state.prepares.length, 1);
      assert.equal(f.state.prepares[0][0], f.floorProbe);
      assert.equal(f.state.prepares[0][1], other.own.probe);
      assert.notEqual(f.state.prepares[0][1], f.materialProbe);
      assert.deepEqual(f.state.prepares[0].slice(2), [2, 99]);
      f.materials.close();
      assert.equal(owner.groundState().plane, 2);
      materials.close();
      assert.equal(owner.groundState(), null);
    } finally { materials.close(); }
    assertClosedKit(other);
  });
});

test("closed dependencies are rechecked by state, tile and preparation independently before native access", async () => {
  for (const dependency of ["floors", "materials"]) {
    for (const query of ["state", "groundState", "groundTile", "prepareGround"]) {
      await usingTerrain((f, owner) => {
        const held = owner.prepareGround(f.floors, f.materials);
        const tile = owner.groundTile(0, 0, 0);
        const counts = [f.state.prepares.length, f.state.tiles.length];
        f[dependency].close();
        if (query === "state") assert.equal(Object.hasOwn(owner.state(), "ground"), false);
        if (query === "groundState") assert.equal(owner.groundState(), null);
        if (query === "groundTile") assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable);
        if (query === "prepareGround") assert.throws(() => owner.prepareGround(f.floors, f.materials), unavailable);
        assertRevoked(f, owner, dependency);
        assert.deepEqual([f.state.prepares.length, f.state.tiles.length], counts);
        assert.deepEqual(held, summaryFor());
        assert.deepEqual(tile, tileFor());
      });
    }
  }
});

test("terrain close, abort and elapsed or fired lifetime revoke ground even before a timer callback runs", async (t) => {
  const clock = fakeTimers(t);
  for (const mode of terrainModes) {
    await usingTerrain((f, owner) => {
      const held = owner.prepareGround(f.floors, f.materials);
      const tile = owner.groundTile(0, 0, 0);
      const counts = [f.state.prepares.length, f.state.tiles.length];
      invalidatedBy(mode, f, owner, clock);
      assertRevoked(f, owner, mode);
      assert.deepEqual([f.state.prepares.length, f.state.tiles.length], counts);
      assert.deepEqual(held, summaryFor());
      assert.deepEqual(tile, tileFor());
      assert.throws(() => owner.groundState(undefined), invalidQuery);
    });
    assert.equal(clock.pending().length, 0);
  }
});

test("dependency liveness reads, forged state getters and forged state calls cannot reentrantly close terrain before entering native preparation", async (t) => {
  const clock = fakeTimers(t);
  for (const point of ["floorState", "materialState", "forgedStateGetter", "forgedState"]) {
    for (const mode of terrainModes) {
      await usingTerrain((f, owner) => {
        const invalidate = () => invalidatedBy(mode, f, owner, clock);
        let materials = f.materials;
        if (point === "forgedStateGetter") materials = { get state() { invalidate(); return acceptingState; } };
        else if (point === "forgedState") materials = { state() { invalidate(); return acceptingState(); } };
        else f.state.hooks[point] = invalidate;
        assert.throws(() => owner.prepareGround(f.floors, materials), closed);
        assert.deepEqual(f.state.prepares, []);
        assert.equal(owner.groundState(), null);
        assert.equal(owner.state().closed, true);
        assertNoOwners(owner.state(), f);
      });
      assert.equal(clock.pending().length, 0);
    }
  }
});

test("genuine dependency liveness hooks closing a dependency before native preparation reject preparation without native access", async () => {
  for (const point of ["floorState", "materialState"]) {
    for (const mode of dependencyModes) {
      await usingTerrain((f, owner) => {
        f.state.hooks[point] = () => invalidatedBy(mode, f, owner);
        assert.throws(() => owner.prepareGround(f.floors, f.materials), unavailable);
        assert.deepEqual(f.state.prepares, []);
        assert.equal(owner.groundState(), null);
        assert.equal(owner.state().accepted, true);
        assert.equal(owner.state().closed, false);
        assertNoOwners(owner.state(), f);
      });
    }
  }
});

test("preparation rechecks dependency and terrain ownership after native success, malformed output or a thrown failure", async (t) => {
  const clock = fakeTimers(t);
  for (const mode of allModes) {
    for (const result of ["valid", "invalid", "throw"]) {
      await usingTerrain((f, owner) => {
        owner.prepareGround(f.floors, f.materials);
        f.state.hooks.prepareGround = () => invalidatedBy(mode, f, owner, clock);
        if (result === "invalid") f.state.summaryValue = "{";
        if (result === "throw") f.state.errorAt = "prepareGround";
        assert.throws(() => owner.prepareGround(f.floors, f.materials, 1), dependencyModes.includes(mode) ? unavailable : closed);
        assert.equal(f.state.prepares.length, 2);
        assertRevoked(f, owner, mode);
      });
      assert.equal(clock.pending().length, 0);
    }
  }
});

test("preparation cannot publish if dependency state invalidates terrain during the post-native ownership check", async (t) => {
  const clock = fakeTimers(t);
  for (const dependency of ["floorState", "materialState"]) {
    for (const mode of allModes) {
      await usingTerrain((f, owner) => {
        f.state.hooks[dependency] = () => { if (f.state.prepares.length) invalidatedBy(mode, f, owner, clock); };
        assert.throws(() => owner.prepareGround(f.floors, f.materials), dependencyModes.includes(mode) ? unavailable : closed);
        assert.equal(f.state.prepares.length, 1);
        assertRevoked(f, owner, mode);
      });
      assert.equal(clock.pending().length, 0);
    }
  }
});

test("JSON.parse reentrancy cannot publish preparation after close, abort, deadline or dependency invalidation", async (t) => {
  const clock = fakeTimers(t);
  const parse = JSON.parse;
  for (const mode of allModes) {
    for (const throws of [false, true]) {
      await usingTerrain((f, owner) => {
        owner.prepareGround(f.floors, f.materials);
        const text = JSON.stringify(summaryFor(1));
        f.state.summaryValue = text;
        const mock = t.mock.method(JSON, "parse", (value, ...args) => {
          if (value === text) {
            invalidatedBy(mode, f, owner, clock);
            if (throws) throw new Error(secret);
          }
          return parse(value, ...args);
        });
        try {
          assert.throws(() => owner.prepareGround(f.floors, f.materials, 1), dependencyModes.includes(mode) ? unavailable : closed);
        } finally { mock.mock.restore(); }
        assertRevoked(f, owner, mode);
      });
      assert.equal(clock.pending().length, 0);
    }
  }
});

test("tile method lookup and native results cannot return after reentrant terrain or dependency invalidation", async (t) => {
  const clock = fakeTimers(t);
  for (const point of ["getter", "native"]) {
    for (const mode of allModes) {
      for (const result of ["valid", "null", "invalid", "throw"]) {
        await usingTerrain((f, owner) => {
          owner.prepareGround(f.floors, f.materials);
          if (result === "null") f.state.tileValue = "null";
          if (result === "invalid") f.state.tileValue = "{";
          if (result === "throw") f.state.errorAt = "groundTile";
          if (point === "getter") {
            const method = f.state.probe.groundTile;
            Object.defineProperty(f.state.probe, "groundTile", { configurable: true, get() { invalidatedBy(mode, f, owner, clock); return method; } });
          } else f.state.hooks.groundTile = () => invalidatedBy(mode, f, owner, clock);
          assert.throws(() => owner.groundTile(0, 0, 0), dependencyModes.includes(mode) ? tileUnavailable : closed);
          assertRevoked(f, owner, mode);
        });
        assert.equal(clock.pending().length, 0);
      }
    }
  }
});

test("tile JSON parsing rechecks ownership for objects, null and thrown parsing failures", async (t) => {
  const clock = fakeTimers(t);
  const parse = JSON.parse;
  for (const mode of allModes) {
    for (const result of ["valid", "null", "throw"]) {
      await usingTerrain((f, owner) => {
        owner.prepareGround(f.floors, f.materials);
        const text = result === "null" ? "null" : JSON.stringify(tileFor());
        f.state.tileValue = text;
        const mock = t.mock.method(JSON, "parse", (value, ...args) => {
          if (value === text) {
            invalidatedBy(mode, f, owner, clock);
            if (result === "throw") throw new Error(secret);
          }
          return parse(value, ...args);
        });
        try {
          assert.throws(() => owner.groundTile(0, 0, 0), dependencyModes.includes(mode) ? tileUnavailable : closed);
        } finally { mock.mock.restore(); }
        assertRevoked(f, owner, mode);
      });
      assert.equal(clock.pending().length, 0);
    }
  }
});

test("reentrant dependency queries invalidate checked ground and cannot republish it in an in-flight prepare or sample", async () => {
  for (const dependency of ["floorState", "materialState"]) {
    for (const point of ["query", "prepareNative", "prepareParse", "tileNative", "tileParse"]) {
      await usingTerrain(async (f, owner) => {
        owner.prepareGround(f.floors, f.materials);
        const reenter = () => { assert.equal(owner.groundState(), null); };
        const enable = () => { f.state.hooks[dependency] = reenter; };
        let mock;
        if (point === "query") enable();
        if (point === "prepareNative") f.state.hooks.prepareGround = enable;
        if (point === "tileNative") f.state.hooks.groundTile = enable;
        if (point.endsWith("Parse")) {
          const parse = JSON.parse;
          const text = JSON.stringify(point === "prepareParse" ? summaryFor(1) : tileFor());
          f.state[point === "prepareParse" ? "summaryValue" : "tileValue"] = text;
          mock = { parse, text };
          JSON.parse = (value, ...args) => { if (value === text) enable(); return parse(value, ...args); };
        }
        try {
          if (point === "query") assert.equal(owner.groundState(), null);
          else if (point.startsWith("prepare")) assert.throws(() => owner.prepareGround(f.floors, f.materials, 1), unavailable);
          else assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable);
        } finally { if (mock) JSON.parse = mock.parse; }
        delete f.state.hooks[dependency];
        assert.equal(owner.groundState(), null);
        assert.equal(Object.hasOwn(owner.state(), "ground"), false);
        assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable);
        assert.equal(owner.state().accepted, true);
      });
    }
  }
});

test("successful reentrant plane replacement during a tile read invalidates the in-flight sample", async (t) => {
  for (const point of ["getter", "native", "parse"]) {
    await t.test(point, async (s) => {
      await usingTerrain((f, owner) => {
        owner.prepareGround(f.floors, f.materials);
        const replace = () => {
          assert.deepEqual(owner.prepareGround(f.floors, f.materials, 1), summaryFor(1));
        };
        let mock;
        if (point === "getter") {
          const method = f.state.probe.groundTile;
          Object.defineProperty(f.state.probe, "groundTile", { configurable: true, get() {
            Object.defineProperty(f.state.probe, "groundTile", { configurable: true, writable: true, value: method });
            replace();
            return method;
          } });
        }
        if (point === "native") f.state.hooks.groundTile = replace;
        if (point === "parse") {
          const parse = JSON.parse;
          const text = JSON.stringify(tileFor());
          f.state.tileValue = text;
          mock = s.mock.method(JSON, "parse", (value, ...args) => {
            if (value === text) replace();
            return parse(value, ...args);
          });
        }
        try { assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable); }
        finally { mock?.mock.restore(); delete f.state.hooks.groundTile; }
        assert.deepEqual(owner.groundState(), summaryFor(1));
        assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable);
        assert.deepEqual(owner.groundTile(1, 0, 0), tileFor());
      });
    });
  }
});

test("genuine dispatch gives the native probe only the genuine floor and material probes with the exact plane and budget on every preparation", async () => {
  await usingTerrain((f, owner) => {
    for (const [plane, budget] of [[0, MAX_VERTICES], [1, 12], [2, 6], [3, MAX_VERTICES]]) {
      assert.deepEqual(owner.prepareGround(f.floors, f.materials, plane, budget), summaryFor(plane));
      const args = f.state.prepares.at(-1);
      assert.equal(args.length, 4);
      assert.equal(args[0], f.floorProbe);
      assert.equal(args[1], f.materialProbe);
      assert.deepEqual(args.slice(2), [plane, budget]);
      assert.equal(f.state.prepareThis.at(-1), f.state.probe);
    }
    assert.equal(f.state.prepares.length, 4);
    for (const args of f.state.prepares) for (const value of args.slice(0, 2)) {
      assert.notEqual(value, owner);
      assert.notEqual(value, f.floors);
      assert.notEqual(value, f.materials);
    }
    assert.equal(f.state.tiles.length, 0);
    owner.close();
    assert.throws(() => owner.prepareGround(f.floors, f.materials), closed);
    assert.equal(f.state.prepares.length, 4);
  });
});

test("a forged material callback cannot capture the floor probe through the private floor dispatch or the terrain bridge", async () => {
  await usingTerrain((f, owner) => {
    const before = owner.prepareGround(f.floors, f.materials);
    const seen = [];
    const gets = [];
    const callbacks = (name) => ({ [name]: (...args) => { seen.push(args); return JSON.stringify(summaryFor()); } });
    const forgedMaterials = [
      recordingProxy(seen, gets),
      { ...callbacks("prepareGround"), ...callbacks("prepare"), ...callbacks("prepareTerrainGround"), ...callbacks("prepareWorldMaterialGround"), state: acceptingState },
      Object.freeze({ ...callbacks("prepareGround"), ...callbacks("material"), close() {}, state: acceptingState }),
      { ...f.materials, ...callbacks("prepareGround") },
      Object.assign(Object.create(f.materials), callbacks("prepareGround")),
    ];
    const borrowers = [
      Object.freeze({ ...callbacks("prepareGround") }),
      recordingProxy(seen, gets),
      (...args) => { seen.push(args); },
    ];
    const calls = f.state.prepares.length;
    for (const materials of forgedMaterials) {
      for (const borrower of borrowers) {
        assert.throws(() => prepareWorldFloorGround(f.floors, borrower, materials, 0, MAX_VERTICES), unavailable);
      }
      assert.throws(() => owner.prepareGround(f.floors, materials), unavailable);
      assert.throws(() => owner.prepareGround(f.floors, materials, 1, 12), unavailable);
    }
    assert.deepEqual(seen.filter((args) => args.length), []);
    assertNoProbe(f, seen);
    assert.equal(f.state.prepares.length, calls);
    assert.deepEqual(owner.groundState(), before);
    assert.deepEqual(owner.groundTile(0, 0, 0), tileFor());
  });
});

test("a fake borrower callback cannot capture the material probe through the private material or floor dispatch", async () => {
  await usingTerrain((f, owner) => {
    const before = owner.prepareGround(f.floors, f.materials);
    const seen = [];
    const gets = [];
    const capture = (...args) => { seen.push(args); return JSON.stringify(summaryFor()); };
    const fakes = [
      { prepareGround: capture },
      Object.freeze({ prepareGround: capture }),
      Object.create({ prepareGround: capture }),
      recordingProxy(seen, gets),
      capture,
      Object.assign(() => {}, { prepareGround: capture }),
      new (class Borrower { prepareGround(...args) { return capture(...args); } })(),
      null, undefined, 0, "borrower",
    ];
    const calls = f.state.prepares.length;
    for (const borrower of fakes) {
      assert.throws(() => prepareWorldMaterialGround(f.materials, borrower, f.floorProbe, 0, MAX_VERTICES), unavailable);
      assert.throws(() => prepareWorldFloorGround(f.floors, borrower, f.materials, 0, MAX_VERTICES), unavailable);
      assert.throws(() => prepareWorldFloorGround(f.floors, borrower, f.materials, 1, 12), unavailable);
    }
    assert.equal(seen.length, 0);
    assert.deepEqual(gets, []);
    assertNoProbe(f, seen);
    assert.equal(f.state.prepares.length, calls);
    assert.deepEqual(owner.groundState(), before);
    assert.deepEqual(owner.groundTile(0, 0, 0), tileFor());
  });
});

test("private dispatch functions reject forged owners, wrong arity values and out-of-range arguments before touching a borrower", async () => {
  await usingTerrain((f, owner) => {
    const before = owner.prepareGround(f.floors, f.materials);
    const seen = [];
    const borrower = { prepareGround: (...args) => { seen.push(args); } };
    for (const forged of forgedOwners(f)) {
      assert.throws(() => prepareWorldFloorGround(forged, borrower, f.materials, 0, MAX_VERTICES), unavailable);
      assert.throws(() => prepareWorldMaterialGround(forged, borrower, f.floorProbe, 0, MAX_VERTICES), unavailable);
    }
    for (const [plane, budget] of [[-1, 6], [4, 6], [0, -1], [0, MAX_VERTICES + 1], [0.5, 6], [0, 0.5], ["0", 6], [0, "6"], [undefined, 6], [0, undefined]]) {
      assert.throws(() => prepareWorldFloorGround(f.floors, borrower, f.materials, plane, budget), unavailable);
      assert.throws(() => prepareWorldMaterialGround(f.materials, borrower, f.floorProbe, plane, budget), unavailable);
    }
    assert.deepEqual(seen, []);
    assert.equal(f.state.prepares.length, 1);
    assert.deepEqual(owner.groundState(), before);
  });
});

test("private dispatch with a fake borrower from inside native preparation cannot reach the native probe or alter the in-flight result", async () => {
  await usingTerrain((f, owner) => {
    const seen = [];
    const borrower = { prepareGround: (...args) => { seen.push(args); } };
    let attempts = 0;
    f.state.hooks.prepareGround = () => {
      attempts++;
      assert.throws(() => prepareWorldFloorGround(f.floors, borrower, f.materials, 0, MAX_VERTICES), unavailable);
      assert.throws(() => prepareWorldMaterialGround(f.materials, borrower, f.floorProbe, 0, MAX_VERTICES), unavailable);
      assert.throws(() => owner.prepareGround(f.floors, f.materials), unavailable);
      assert.equal(owner.groundState(), null);
    };
    assert.deepEqual(owner.prepareGround(f.floors, f.materials), summaryFor());
    assert.equal(attempts, 1);
    assert.deepEqual(seen, []);
    assert.equal(f.state.prepares.length, 1);
    assert.deepEqual(owner.groundTile(0, 0, 0), tileFor());
  });
});

test("a failed or invalid native preparation after genuine dispatch invalidates old geometry and later genuine preparation succeeds", async () => {
  await usingTerrain((f, owner) => {
    owner.prepareGround(f.floors, f.materials);
    for (const mode of ["throw", "invalid"]) {
      if (mode === "throw") f.state.errorAt = "prepareGround";
      else { f.state.errorAt = undefined; f.state.summaryValue = "{"; }
      assert.throws(() => owner.prepareGround(f.floors, f.materials, 1), unavailable);
      if (mode === "throw") assert.deepEqual(owner.groundState(), summaryFor(0));
      else assert.equal(owner.groundState(), null);
    }
    f.state.errorAt = undefined;
    delete f.state.summaryValue;
    assert.equal(f.state.prepares.length, 3);
    assert.deepEqual(owner.prepareGround(f.floors, f.materials, 1), summaryFor(1));
    assertNoOwners(owner.state(), f);
  });
});

test("nested preparation and tile reads are rejected while a ground preparation owns the native bridge", async () => {
  await usingTerrain((f, owner) => {
    owner.prepareGround(f.floors, f.materials);
    f.state.hooks.prepareGround = () => {
      assert.equal(owner.groundState(), null);
      assert.equal(Object.hasOwn(owner.state(), "ground"), false);
      assert.throws(() => owner.prepareGround(f.floors, f.materials, 2), unavailable);
      assert.throws(() => owner.groundTile(0, 0, 0), tileUnavailable);
      assert.throws(() => owner.groundTile(1, 0, 0), tileUnavailable);
    };
    assert.deepEqual(owner.prepareGround(f.floors, f.materials, 1), summaryFor(1));
    assert.equal(f.state.prepares.length, 2);
    assert.deepEqual(f.state.tiles, []);
    assert.deepEqual(owner.groundTile(1, 0, 0), tileFor());
    assertNoOwners(owner.state(), f);
  });
});

test("forged floors passed to openWorldTerrain never receive the terrain probe, any argument or any property read", async () => {
  const floors = await openKit(floorKit({ dependencyReads: [], hooks: {} }, "forgedFloorState"));
  const seen = [];
  const record = (name) => (...args) => { seen.push([name, args]); return JSON.stringify(paletteFor(72)); };
  const forged = [
    null, false, 0, "floors", {}, { composeTerrain: 1 }, { composePalette: 1 }, { state: 1 },
    { composeTerrain: record("composeTerrain"), composePalette: record("composePalette"), state: record("state"), close: record("close") },
    Object.freeze({ composeTerrain: record("composeTerrain"), composePalette: record("composePalette"), state: acceptingState }),
    { get composeTerrain() { seen.push(["get", ["composeTerrain"]]); return record("composeTerrain"); }, get state() { seen.push(["get", ["state"]]); return acceptingState; } },
    { ...floors },
    Object.create(floors),
    Object.defineProperty(Object.create(floors), "composeTerrain", { value: record("composeTerrain") }),
    new Proxy(floors, { get(target, key) { seen.push(["get", [String(key)]]); return Reflect.get(target, key); } }),
    structuredClone({ accepted: true, closed: false }),
  ];
  for (const value of forged) {
    const f = fixture();
    f.options.floors = value;
    await assert.rejects(openWorldTerrain(f.options), { message: "verified world terrain unavailable" });
    assert.equal(f.state.probe, undefined);
    assert.equal(f.state.socket, undefined);
    assert.equal(f.state.received, 0);
  }
  assert.deepEqual(seen, []);
  floors.close();
});

test("floors and materials teardown hooks observe an already closed owner snapshot and revoked reads", async () => {
  for (const [kind, make, probeRead] of [["floors", floorKit, (owner) => owner.underlay(0)], ["materials", materialKit, (owner) => owner.material(0)]]) {
    for (const point of ["signal", "free"]) {
      const state = { dependencyReads: [], hooks: {} };
      const kit = make(state, "teardownState");
      const owner = await openKit(kit);
      assert.equal(owner.state().accepted, true);
      const seen = [];
      const observe = () => {
        const snapshot = owner.state();
        let read = "returned";
        try { probeRead(owner); } catch (error) { read = error.message; }
        seen.push({ accepted: snapshot.accepted, closed: snapshot.closed, phase: snapshot.phase, read });
      };
      if (point === "signal") state.hooks["teardownState:remove"] = observe;
      else kit.own.hooks.free = observe;
      owner.close();
      assert.equal(seen.length, 1, `${kind} ${point}`);
      assert.equal(seen[0].accepted, false, `${kind} ${point}`);
      assert.equal(seen[0].closed, true, `${kind} ${point}`);
      assert.equal(seen[0].phase, "closed", `${kind} ${point}`);
      assert.match(seen[0].read, /closed/, `${kind} ${point}`);
      owner.close();
      assert.equal(seen.length, 1);
      assertClosedKit(kit);
    }
  }
});

test("floors and materials teardown through abort and lifetime expiry also exposes only closed snapshots to hooks", async () => {
  for (const make of [floorKit, materialKit]) {
    for (const mode of ["abort", "lifetime"]) {
      const state = { dependencyReads: [], hooks: {} };
      const kit = make(state, "teardownState", 1000);
      const owner = await openKit(kit);
      const seen = [];
      kit.own.hooks.free = () => { const snapshot = owner.state(); seen.push([snapshot.accepted, snapshot.closed]); };
      if (mode === "abort") kit.signal.abort(secret);
      else await new Promise((resolve) => setTimeout(resolve, 1100));
      assert.deepEqual(seen, [[false, true]], mode);
      assertClosedKit(kit);
      assert.equal(JSON.stringify(owner.state()).includes(secret), false);
    }
  }
});

test("terrain teardown hooks observe a closed owner with no ground, a null groundState and a throwing groundTile", async () => {
  for (const point of ["terrain:remove", "free"]) {
    await usingTerrain((f, owner) => {
      const prepared = owner.prepareGround(f.floors, f.materials);
      assert.deepEqual(owner.groundState(), prepared);
      assert.deepEqual(owner.groundTile(0, 0, 0), tileFor());
      const seen = [];
      f.state.hooks[point] = () => {
        const snapshot = owner.state();
        let tile = "returned";
        try { owner.groundTile(0, 0, 0); } catch (error) { tile = error.message; }
        let prepare = "returned";
        try { owner.prepareGround(f.floors, f.materials); } catch (error) { prepare = error.message; }
        seen.push({ accepted: snapshot.accepted, closed: snapshot.closed, phase: snapshot.phase, ground: Object.hasOwn(snapshot, "ground"), groundState: owner.groundState(), tile, prepare });
      };
      owner.close();
      assert.equal(seen.length, 1, point);
      assert.deepEqual(seen[0], { accepted: false, closed: true, phase: "closed", ground: false, groundState: null, tile: closed.message, prepare: closed.message }, point);
      owner.close();
      assert.equal(seen.length, 1);
      assert.equal(f.state.prepares.length, 1);
      assert.equal(f.state.tiles.length, 1);
    });
  }
});

test("terrain teardown hooks fired by abort or lifetime expiry see a closed owner with no ground", async (t) => {
  const clock = fakeTimers(t);
  for (const mode of ["abort", "timer"]) {
    await usingTerrain((f, owner) => {
      owner.prepareGround(f.floors, f.materials);
      const seen = [];
      f.state.hooks.free = () => {
        const snapshot = owner.state();
        seen.push({ closed: snapshot.closed, ground: Object.hasOwn(snapshot, "ground"), groundState: owner.groundState() });
      };
      if (mode === "abort") f.abort.abort(secret);
      else clock.expire();
      assert.deepEqual(seen, [{ closed: true, ground: false, groundState: null }], mode);
      assert.throws(() => owner.groundTile(0, 0, 0), closed);
    });
  }
});

test("socket close hooks during a pending exchange cannot resurrect or publish any owner", async () => {
  for (const make of [floorKit, materialKit]) {
    const state = { dependencyReads: [], hooks: {} };
    const kit = make(state, "pendingState");
    const pending = kit.open(kit.options);
    const rejected = assert.rejects(pending, { message: /cancelled/ });
    await setImmediate();
    kit.own.socket.onopen();
    let calls = 0;
    kit.own.hooks.close = () => { calls++; };
    kit.signal.abort(secret);
    await rejected;
    assert.equal(calls, 1);
    assertClosedKit(kit);
  }
  const f = fixture();
  delete f.options.floors;
  const pending = openWorldTerrain(f.options);
  const rejected = assert.rejects(pending, { message: "world terrain cancelled" });
  await setImmediate();
  f.state.socket.onopen();
  let calls = 0;
  f.state.hooks.socketClose = () => { calls++; };
  f.abort.abort(secret);
  await rejected;
  assert.equal(calls, 1);
  assert.equal(f.state.closed, 1);
  assert.equal(f.state.freed, 1);
});
