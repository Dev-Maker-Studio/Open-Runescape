import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import { setImmediate } from "node:timers/promises";
import init, {
  Js5WorldTerrainProbe,
  Js5WorldFloorProbe,
  Js5WorldMaterialProbe,
} from "../static/protocol-runtime.js";
import { openWorldTerrain } from "../static/world-terrain.js";
import { openWorldFloors } from "../static/world-floors.js";
import { openWorldMaterials } from "../static/world-materials.js";
import { PUBLIC_LOGIN_DEFAULTS } from "../static/login-config.js";

await init({ module_or_path: await readFile(new URL("../static/protocol-runtime_bg.wasm", import.meta.url)) });
const fixture = (path) => readFile(new URL(path, import.meta.url));
const floorWire = await fixture("../../client-runtime/tests/fixtures/floors-native.wire");
const materialWire = await fixture("../../client-runtime/tests/fixtures/material-native.wire");
const nativeBytes = await fixture("../../scene/tests/fixtures/ground-geometry.json");
const native = JSON.parse(nativeBytes);
const nativeCall = native.cases.find(({ name }) => name === "boundary-12-0").calls[0];
const regions = [0, 1, 256, 257];
const size = 72;
const requiredVertices = size * size * 6;
const maxVertices = 1016064;
const lifetimeMs = 300000;
const groundError = /ground.*(?:unavailable|closed)|owner.*closed|invalid.*(?:ground|coordinates|plane|budget|vertices)/i;
const digest = (bytes) => createHash("sha256").update(bytes).digest("hex");
const chains = {
  underlay: {
    tags: [85, 1, 0],
    archiveWhirlpool: "8edcfc7ebb1174207f34e98d23d626cd60023312fab39dc23dd14dd71883de786c40410a925f502fe05fdc1c5ee2b86017e67638e30353ae4ce18ff921b26335",
    referenceWhirlpool: "7647cbc2644fd8f27f6c26ae4f29dc8851cb54f6f97946730bc96b6c5aac10bca75128a3a40e0c5f1273579435459788c0977adba7699e2c2b301f8c7fa9e4c3",
    sha256: "7b27a9436d1e97b74f56692a31789c4f13bfc7d12797b1ab500d11b4c570e28a",
    bytes: 198075,
  },
  empty: {
    tags: [1, 0],
    archiveWhirlpool: "62bb66089e983ba90178c03281c852f2648ee71fa7a0618c1e41d89d506d8bce6d61f210212a23c2c8f9696f2a24e24bd5429e78ec4d51914416b155858e739c",
    referenceWhirlpool: "42aff1371bab770ac69ddff53a643c8a5a0a6b0376ce7e43b0faa78e35e8d1d7e4bf14064304518936f87bd6af23d3b7552c1e8cb2db04353b74db27a114db33",
    sha256: "7c0652ce7a53c3a518b05964796c34db4b908739db363cb084efaa8bbf6a17a9",
    bytes: 132411,
  },
};

function u16(value) {
  const bytes = Buffer.alloc(2);
  bytes.writeUInt16BE(value);
  return bytes;
}

function u32(value) {
  const bytes = Buffer.alloc(4);
  bytes.writeUInt32BE(value >>> 0);
  return bytes;
}

function crc32(bytes) {
  let crc = 0xffffffff;
  for (const byte of bytes) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function raw(bytes) {
  return Buffer.concat([Buffer.from([0]), u32(bytes.length), bytes]);
}

function reply(index, archive, container) {
  const bytes = [index, ...u32(archive), ...container.subarray(0, 5)];
  let position = 10;
  for (const byte of container.subarray(5)) {
    if (position === 512) {
      bytes.push(255);
      position = 1;
    }
    bytes.push(byte);
    position++;
  }
  return Buffer.from(bytes);
}

function terrainWire(spec) {
  const payload = Buffer.alloc(16384 * spec.tags.length);
  for (let at = 0; at < payload.length; at += spec.tags.length) payload.set(spec.tags, at);
  const archive = raw(payload);
  const nameHashes = ["m0_0", "m0_1", "m1_0", "m1_1"].map((name) => {
    let hash = 0;
    for (const byte of Buffer.from(name)) hash = (Math.imul(hash, 31) + byte) | 0;
    return u32(hash);
  });
  const reference = raw(Buffer.concat([
    Buffer.from([6]), u32(727), Buffer.from([3]), u16(4),
    u16(100), u16(1), u16(1), u16(1), ...nameHashes,
    ...regions.map(() => u32(crc32(archive))),
    ...regions.map(() => Buffer.from(spec.archiveWhirlpool, "hex")),
    ...regions.map(() => u32(44)), ...regions.map(() => u16(1)),
    ...regions.map(() => u16(0)), ...regions.map(() => u32(0)),
  ]));
  const version = raw(Buffer.concat([
    Buffer.from([6]), Buffer.alloc(5 * 72), u32(crc32(reference)), u32(727),
    Buffer.from(spec.referenceWhirlpool, "hex"), Buffer.alloc(128, 0x5a),
  ]));
  return Buffer.concat([
    Buffer.alloc(109), reply(255, 255, version), reply(255, 5, reference),
    ...regions.map((_, at) => reply(5, 100 + at, archive)),
  ]);
}

const underlayWire = terrainWire(chains.underlay);
const emptyWire = terrainWire(chains.empty);

function summary(plane, tileCount = size * size, vertexCount = requiredVertices) {
  return {
    scope: "blended-surface-geometry", plane, mapSize: size, tileCount, vertexCount,
    emittedVertexCount: vertexCount, terrainLowered: false,
    texturePixelsDecoded: false, renderedWorld: false,
  };
}

function expectedTile(x, y) {
  return {
    x, y, shape: nativeCall.effective_shape, rotation: nativeCall.effective_rotation,
    splitEdges: [false, false, false, false],
    vertices: nativeCall.offsets_x.map((offsetX, at) => ({
      offsetX, offsetZ: nativeCall.offsets_z[at], mainColor: nativeCall.main_colors[at],
      texture: nativeCall.textures[at], textureScale: nativeCall.texture_scales[at],
    })),
    blendingColors: nativeCall.blending_colors, emittedVertices: nativeCall.offsets_x.length,
    hasShadows: nativeCall.shadows,
  };
}

function transport(Type) {
  const state = { created: 0, freed: 0, closes: 0, preparations: 0, palettes: 0, sent: [], outgoing: [] };
  class Probe extends Type {
    constructor(...args) {
      super(...args);
      state.created++;
      state.token = args[0];
    }
    outgoing() {
      const bytes = super.outgoing();
      state.outgoing.push(bytes);
      return bytes;
    }
    free() { state.freed++; super.free(); }
  }
  const LoadedProbe = Type === Js5WorldTerrainProbe ? class extends Probe {
    composePalette(...args) { state.palettes++; return super.composePalette(...args); }
    prepareGround(...args) { state.preparations++; return super.prepareGround(...args); }
  } : Probe;
  class Socket {
    constructor(url) { state.url = url; state.socket = this; }
    send(bytes) { state.sent.push([...bytes]); }
    close() { state.closes++; }
  }
  return {
    state,
    options: {
      js5Relay: "ws://127.0.0.1:8110/js5", load: async () => LoadedProbe,
      WebSocketClass: Socket, timeoutMs: 30000, lifetimeMs,
    },
  };
}

async function loadOwner(open, f, wire, fragmentSize, extra = {}) {
  const pending = open({ ...f.options, ...extra });
  const outcome = pending.then((owner) => ({ owner }), (error) => ({ error }));
  await setImmediate();
  assert.ok(f.state.socket);
  const { onopen, onmessage } = f.state.socket;
  assert.equal(typeof onopen, "function");
  assert.equal(typeof onmessage, "function");
  onopen();
  assert.equal(f.state.sent[0][0], 15);
  assert.equal(f.state.token, PUBLIC_LOGIN_DEFAULTS.js5AuthToken);
  for (let at = 0; at < wire.length; at += fragmentSize) {
    const data = Uint8Array.from(wire.subarray(at, at + fragmentSize));
    onmessage({ data: data.buffer });
    assert.ok(data.every((value) => value === 0));
  }
  const result = await outcome;
  if (result.error) throw result.error;
  assert.equal(f.state.closes, 1);
  assert.equal(f.state.freed, 0);
  for (const bytes of f.state.outgoing) assert.ok(bytes.every((value) => value === 0));
  return result.owner;
}

function cleaned(f) {
  assert.equal(f.state.created, 1);
  assert.equal(f.state.freed, 1);
  assert.equal(f.state.closes, 1);
  for (const name of ["onopen", "onmessage", "onerror", "onclose"]) assert.equal(f.state.socket[name], null);
  for (const bytes of f.state.outgoing) assert.ok(bytes.every((value) => value === 0));
}

async function ready({ wire = underlayWire, fragmentSize = 511, withFloors = true, clock } = {}) {
  const fixtures = {
    floors: transport(Js5WorldFloorProbe),
    materials: transport(Js5WorldMaterialProbe),
    terrain: transport(Js5WorldTerrainProbe),
  };
  const owners = {};
  const controllers = {};
  const timers = {};
  const close = () => {
    for (const owner of Object.values(owners).reverse()) { owner.close(); owner.close(); }
  };
  const load = async (name, open, bytes, extra = {}) => {
    controllers[name] = new AbortController();
    owners[name] = await loadOwner(open, fixtures[name], bytes, fragmentSize, { ...extra, signal: controllers[name].signal });
    if (clock) timers[name] = clock.pending(lifetimeMs).at(-1);
  };
  try {
    await load("floors", openWorldFloors, floorWire);
    await load("materials", openWorldMaterials, materialWire);
    await load("terrain", openWorldTerrain, wire, {
      initialWorld: { base: { x: 56, y: 56 }, mapSize: size, mapKind: "standard", regionIds: [...regions] },
      ...(withFloors ? { floors: owners.floors } : {}),
    });
    return { ...owners, fixtures, controllers, timers, close, cleaned() { for (const f of Object.values(fixtures)) cleaned(f); } };
  } catch (error) {
    close();
    throw error;
  }
}

function frozen(value) {
  assert.ok(Object.isFrozen(value));
  if (value && typeof value === "object") for (const child of Object.values(value)) {
    if (child && typeof child === "object") frozen(child);
  }
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

test("browser ground owners use pinned native geometry and fragmented synthetic verified JS5 chains", async () => {
  assert.equal(native.jar_sha256, "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048");
  assert.equal(digest(nativeBytes), "5239f0dbae1197c9b65a0ef2b98ab6816bcf8a3be3936c8d43e63f7cf04907a5");
  assert.match(native.capture, /original MapLoader.addBlendedTiles/);
  assert.equal(nativeCall.effective_shape, 12);
  assert.equal(nativeCall.effective_rotation, 0);
  assert.deepEqual(nativeCall.main_colors, [0, 0, 0, 0, 0, 0]);
  assert.deepEqual(nativeCall.texture_scales, [512, 512, 512, 512, 512, 512]);
  assert.equal(nativeCall.draw_heights_a, null);
  assert.equal(nativeCall.draw_heights_b, null);
  for (const [spec, wire] of [[chains.underlay, underlayWire], [chains.empty, emptyWire]]) {
    assert.equal(wire.length, spec.bytes);
    assert.equal(digest(wire), spec.sha256);
  }
  for (const fragmentSize of [7, 511, 4093]) {
    const state = await ready({ fragmentSize });
    try {
      const snapshot = state.terrain.state();
      assert.equal(snapshot.receivedBytes, underlayWire.length);
      assert.equal(snapshot.authenticated, false);
      assert.equal(snapshot.renderedWorld, false);
      assert.equal(snapshot.report.referenceCrcVerified, true);
      assert.equal(snapshot.report.referenceWhirlpoolVerified, true);
      assert.deepEqual(snapshot.report.regions.map(({ regionId }) => regionId), regions);
      for (const region of snapshot.report.regions) {
        assert.equal(region.present, true);
        assert.equal(region.crcVerified, true);
        assert.equal(region.whirlpoolVerified, true);
      }
      assert.equal(snapshot.palette.scope, "underlay-packed-hsl");
      assert.equal(snapshot.palette.knownColors, 4 * size * size);
      assert.equal(state.terrain.paletteColor(0, 0, 0), 0);
      assert.deepEqual(state.terrain.sceneTile(0, 0, 0), {
        underlayId: 4, overlayId: 0, renderFlags: 0, overlayShape: 0, overlayRotation: 0,
      });
      assert.ok(Object.isFrozen(state.terrain));
      assert.equal("probe" in state.terrain, false);
      assert.equal(state.fixtures.terrain.state.palettes, 1);
      assert.equal(state.fixtures.terrain.state.preparations, 0);
      assert.equal(state.terrain.groundState(), null);
      assert.throws(() => state.terrain.groundTile(0, 0, 0), groundError);
      const prepared = state.terrain.prepareGround(state.floors, state.materials);
      assert.equal(state.fixtures.terrain.state.preparations, 1);
      assert.deepEqual(prepared, summary(0));
      frozen(prepared);
      assert.equal(prepared instanceof Promise, false);
      assert.deepEqual(state.terrain.groundState(), summary(0));
      assert.deepEqual(state.terrain.groundTile(0, 0, 0), expectedTile(0, 0));
      assert.equal(state.fixtures.terrain.state.freed, 0);
    } finally { state.close(); }
    state.cleaned();
  }
});

test("real ground preparation returns frozen summaries and independent deeply frozen native tile snapshots on every plane", async () => {
  const state = await ready();
  try {
    for (let plane = 0; plane < 4; plane++) {
      const prepared = state.terrain.prepareGround(state.floors, state.materials, plane, requiredVertices);
      assert.deepEqual(prepared, summary(plane));
      frozen(prepared);
      const retained = state.terrain.groundState();
      assert.deepEqual(retained, summary(plane));
      frozen(retained);
      assert.throws(() => { prepared.plane = 9; }, TypeError);
      for (const [x, y] of [[0, 0], [0, 71], [7, 8], [8, 7], [35, 36], [63, 64], [64, 63], [71, 0], [71, 71]]) {
        const tile = state.terrain.groundTile(plane, x, y);
        assert.deepEqual(tile, expectedTile(x, y));
        frozen(tile);
        assert.throws(() => { tile.vertices[0].mainColor = 123; }, TypeError);
        assert.throws(() => { tile.vertices.push({}); }, TypeError);
        assert.throws(() => { tile.splitEdges[0] = true; }, TypeError);
        if (tile.blendingColors !== null) assert.throws(() => { tile.blendingColors[0] = 123; }, TypeError);
        const next = state.terrain.groundTile(plane, x, y);
        assert.notEqual(next, tile);
        assert.notEqual(next.vertices, tile.vertices);
        assert.notEqual(next.vertices[0], tile.vertices[0]);
        assert.deepEqual(next, expectedTile(x, y));
      }
      for (let other = 0; other < 4; other++) {
        if (other !== plane) assert.throws(() => state.terrain.groundTile(other, 0, 0), groundError);
      }
    }
  } finally { state.close(); }
  state.cleaned();
});

test("failed real ground budgets preserve the old plane and successful preparation replaces it", async () => {
  const state = await ready();
  try {
    assert.deepEqual(state.terrain.prepareGround(state.floors, state.materials, 0, requiredVertices), summary(0));
    const before = state.terrain.groundTile(0, 71, 71);
    const beforeState = state.terrain.groundState();
    for (const budget of [0, requiredVertices - 1]) {
      assert.throws(() => state.terrain.prepareGround(state.floors, state.materials, 1, budget), groundError);
      assert.deepEqual(state.terrain.groundState(), beforeState);
      assert.deepEqual(state.terrain.groundTile(0, 71, 71), before);
      assert.throws(() => state.terrain.groundTile(1, 71, 71), groundError);
      assert.equal(state.terrain.state().accepted, true);
    }
    assert.deepEqual(state.terrain.prepareGround(state.floors, state.materials, 1, requiredVertices), summary(1));
    assert.throws(() => state.terrain.groundTile(0, 71, 71), groundError);
    assert.deepEqual(state.terrain.groundTile(1, 71, 71), expectedTile(71, 71));
    assert.deepEqual(before, expectedTile(71, 71));
    assert.deepEqual(beforeState, summary(0));
    assert.deepEqual(state.terrain.prepareGround(state.floors, state.materials, 3, maxVertices), summary(3));
    assert.throws(() => state.terrain.groundTile(1, 71, 71), groundError);
    assert.deepEqual(state.terrain.groundState(), summary(3));
    assert.deepEqual(state.terrain.prepareGround(state.floors, state.materials), summary(0));
  } finally { state.close(); }
  state.cleaned();
});

test("ground owner numeric coordinates and vertex budgets reject coercion wrapping and fractions without replacing native geometry", async () => {
  const state = await ready();
  try {
    state.terrain.prepareGround(state.floors, state.materials);
    const before = state.terrain.groundTile(0, 0, 0);
    let coerced = 0;
    const hostile = {
      valueOf() { coerced++; return 0; },
      toString() { coerced++; return "0"; },
      [Symbol.toPrimitive]() { coerced++; return 0; },
    };
    const invalid = [undefined, null, "0", "3", "1016064", false, true, {}, [], [0], new Number(0),
      new Uint8Array([0]), () => 0, Symbol("0"), 0n, NaN, Infinity, -Infinity,
      -1, -0.5, 0.5, 2 ** 32, 2 ** 32 + 1, Number.MAX_SAFE_INTEGER, hostile];
    for (const value of invalid) {
      assert.throws(() => state.terrain.prepareGround(state.floors, state.materials, value, requiredVertices), groundError);
      assert.throws(() => state.terrain.prepareGround(state.floors, state.materials, 1, value), groundError);
      for (let coordinate = 0; coordinate < 3; coordinate++) {
        const args = [0, 0, 0];
        args[coordinate] = value;
        assert.throws(() => state.terrain.groundTile(...args), groundError);
      }
      assert.deepEqual(state.terrain.groundState(), summary(0));
      assert.deepEqual(state.terrain.groundTile(0, 0, 0), before);
    }
    for (const args of [[], [0], [0, 0], [undefined, 0, 0], [0, undefined, 0], [0, 0, undefined],
      [4, 0, 0], [255, 0, 0], [0, 72, 0], [0, 0, 72], [0, 73, 0], [0, 0, 73]]) {
      assert.throws(() => state.terrain.groundTile(...args), groundError);
    }
    for (const plane of [4, 255, 65535]) {
      assert.throws(() => state.terrain.prepareGround(state.floors, state.materials, plane, requiredVertices), groundError);
    }
    for (const budget of [maxVertices + 1, maxVertices + 0.5, 2 ** 32 - 1]) {
      assert.throws(() => state.terrain.prepareGround(state.floors, state.materials, 1, budget), groundError);
    }
    assert.equal(coerced, 0);
    assert.deepEqual(state.terrain.groundTile(-0, -0, -0), before);
    assert.deepEqual(state.terrain.prepareGround(state.floors, state.materials, -0, requiredVertices), summary(0));
  } finally { state.close(); }
  state.cleaned();
});

test("only the original retained floor owner and a live material owner can prepare actual browser ground", async () => {
  const state = await ready();
  const alternate = transport(Js5WorldFloorProbe);
  let otherFloors;
  try {
    otherFloors = await loadOwner(openWorldFloors, alternate, floorWire, 13);
    state.terrain.prepareGround(state.floors, state.materials);
    for (const [floors, materials] of [[otherFloors, state.materials], [state.materials, state.floors],
      [{}, state.materials], [state.floors, {}], [null, state.materials], [state.floors, null]]) {
      assert.throws(() => state.terrain.prepareGround(floors, materials, 1, requiredVertices), groundError);
      assert.deepEqual(state.terrain.groundState(), summary(0));
      assert.deepEqual(state.terrain.groundTile(0, 0, 0), expectedTile(0, 0));
    }
  } finally { otherFloors?.close(); state.close(); }
  state.cleaned();
  cleaned(alternate);
  const withoutPalette = await ready({ withFloors: false });
  try {
    assert.equal(withoutPalette.terrain.groundState(), null);
    assert.throws(() => withoutPalette.terrain.prepareGround(withoutPalette.floors, withoutPalette.materials), groundError);
    assert.equal(withoutPalette.terrain.groundState(), null);
    assert.throws(() => withoutPalette.terrain.groundTile(0, 0, 0), groundError);
  } finally { withoutPalette.close(); }
  withoutPalette.cleaned();
});

test("empty verified JS5 terrain permits a zero vertex budget and null tiles only within the prepared plane", async () => {
  const state = await ready({ wire: emptyWire, fragmentSize: 109 });
  try {
    assert.equal(state.terrain.groundState(), null);
    for (let plane = 0; plane < 4; plane++) {
      const prepared = state.terrain.prepareGround(state.floors, state.materials, plane, -0);
      assert.deepEqual(prepared, summary(plane, 0, 0));
      frozen(prepared);
      assert.deepEqual(state.terrain.groundState(), summary(plane, 0, 0));
      for (const [x, y] of [[0, 0], [0, 71], [71, 0], [35, 36], [71, 71]]) {
        assert.equal(state.terrain.groundTile(plane, x, y), null);
      }
      assert.throws(() => state.terrain.groundTile(plane, 72, 0), groundError);
      assert.throws(() => state.terrain.groundTile(plane, 0, 72), groundError);
      assert.throws(() => state.terrain.groundTile((plane + 1) % 4, 0, 0), groundError);
    }
    assert.deepEqual(state.terrain.prepareGround(state.floors, state.materials), summary(0, 0, 0));
    assert.equal(state.terrain.groundTile(-0, -0, -0), null);
  } finally { state.close(); }
  state.cleaned();
});

test("close expiry and abort of any real ground dependency disallow retained preparation state and tile queries", async (t) => {
  const clock = fakeTimers(t);
  for (const action of ["close", "expiry", "abort"]) {
    for (const name of ["floors", "materials", "terrain"]) {
      await t.test(`${name} ${action}`, async () => {
        const state = await ready({ clock });
        try {
          assert.equal(clock.pending(lifetimeMs).length, 3);
          assert.deepEqual(state.terrain.prepareGround(state.floors, state.materials), summary(0));
          const held = state.terrain.groundTile(0, 0, 0);
          if (action === "close") state[name].close();
          else if (action === "expiry") clock.fire(state.timers[name]);
          else state.controllers[name].abort();
          assert.equal(state[name].state().closed, true);
          assert.equal(state.fixtures[name].state.freed, 1);
          assert.equal(state.terrain.groundState(), null);
          assert.throws(() => state.terrain.groundTile(0, 0, 0), groundError);
          assert.throws(() => state.terrain.prepareGround(state.floors, state.materials, 1, requiredVertices), groundError);
          assert.deepEqual(held, expectedTile(0, 0));
          frozen(held);
        } finally { state.close(); }
        state.cleaned();
        assert.equal(clock.pending().length, 0);
      });
    }
  }
});
