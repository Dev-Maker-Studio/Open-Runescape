import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import init, {
  Js5WorldTerrainProbe,
  Js5WorldFloorProbe,
  Js5WorldMaterialProbe,
} from "../../web/static/protocol-runtime.js";

await init({ module_or_path: await readFile(new URL("../../web/static/protocol-runtime_bg.wasm", import.meta.url)) });
const fixture = (path) => readFile(new URL(path, import.meta.url));
const floorWire = await fixture("./fixtures/floors-native.wire");
const materialWire = await fixture("./fixtures/material-native.wire");
const nativeBytes = await fixture("../../scene/tests/fixtures/ground-geometry.json");
const native = JSON.parse(nativeBytes);
const nativeCall = native.cases.find(({ name }) => name === "boundary-12-0").calls[0];
const regions = [0, 1, 256, 257];
const size = 72;
const requiredVertices = size * size * 6;
const maxVertices = 1016064;
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

function loaded(Type, bytes, ...args) {
  const probe = new Type("synthetic", ...args);
  try {
    assert.equal(probe.outgoing()[0], 15);
    for (let at = 0; at < bytes.length; at += 511) probe.feed(bytes.subarray(at, at + 511));
    assert.equal(probe.complete(), true);
    probe.finish();
    return probe;
  } catch (error) {
    probe.free();
    throw error;
  }
}

function ready(bytes = underlayWire, compose = true) {
  const probes = [];
  try {
    const terrain = loaded(Js5WorldTerrainProbe, bytes, JSON.stringify(regions));
    probes.push(terrain);
    const floors = loaded(Js5WorldFloorProbe, floorWire);
    probes.push(floors);
    const materials = loaded(Js5WorldMaterialProbe, materialWire);
    probes.push(materials);
    if (compose) {
      terrain.compose(56, 56, size);
      terrain.composePalette(floors);
    }
    return { terrain, floors, materials, free() { for (const probe of probes.reverse()) probe.free(); } };
  } catch (error) {
    for (const probe of probes.reverse()) probe.free();
    throw error;
  }
}

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

function prepare(state, plane = 0, budget = requiredVertices) {
  return JSON.parse(state.terrain.prepareGround(state.floors, state.materials, plane, budget));
}

function terminal(state) {
  const { terrain, floors, materials } = state;
  assert.equal(terrain.complete(), false);
  assert.equal(terrain.outgoing().length, 0);
  assert.throws(() => terrain.report(), /not complete/);
  assert.throws(() => terrain.compose(56, 56, size), /not complete/);
  assert.throws(() => terrain.composePalette(floors));
  assert.throws(() => terrain.prepareGround(floors, materials, 0, requiredVertices));
  assert.throws(() => terrain.groundTile(0, 0, 0), /not complete/);
  assert.throws(() => terrain.feed(new Uint8Array()), /has failed/);
  assert.throws(() => terrain.finish(), /connection ended before complete terrain probe/);
}

test("ground WASM uses pinned native geometry and explicitly synthetic verified JS5 chains", () => {
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
  const state = ready();
  try {
    const report = JSON.parse(state.terrain.report());
    assert.equal(report.authenticated, false);
    assert.equal(report.referenceCrcVerified, true);
    assert.equal(report.referenceWhirlpoolVerified, true);
    assert.deepEqual(report.regions.map(({ regionId }) => regionId), regions);
    for (const region of report.regions) {
      assert.equal(region.present, true);
      assert.equal(region.crcVerified, true);
      assert.equal(region.whirlpoolVerified, true);
    }
    assert.equal(state.terrain.paletteColor(0, 0, 0), 0);
    assert.deepEqual(JSON.parse(state.terrain.sceneTile(0, 0, 0)), {
      underlayId: 4, overlayId: 0, renderFlags: 0, overlayShape: 0, overlayRotation: 0,
    });
  } finally { state.free(); }
});

test("served ground binding matches every ordered native vertex on all four complete planes", () => {
  const state = ready();
  try {
    for (let plane = 0; plane < 4; plane++) {
      assert.deepEqual(prepare(state, plane), summary(plane));
      for (let x = 0; x < size; x++) {
        for (let y = 0; y < size; y++) {
          assert.deepEqual(JSON.parse(state.terrain.groundTile(plane, x, y)), expectedTile(x, y));
        }
      }
      for (let other = 0; other < 4; other++) {
        if (other !== plane) assert.throws(() => state.terrain.groundTile(other, 0, 0), /plane not prepared/);
      }
    }
    const before = state.terrain.groundTile(3, 71, 71);
    state.terrain.finish();
    state.terrain.finish();
    state.terrain.feed(new Uint8Array());
    assert.equal(state.terrain.groundTile(3, 71, 71), before);
  } finally { state.free(); }
});

test("ground arguments reject JS coercion, fractions and wrapping without replacing retained geometry", () => {
  const state = ready();
  try {
    prepare(state);
    const before = state.terrain.groundTile(0, 0, 0);
    const report = state.terrain.report();
    let coerced = 0;
    const hostile = {
      valueOf() { coerced++; return 0; },
      toString() { coerced++; return "0"; },
      [Symbol.toPrimitive]() { coerced++; return 0; },
    };
    const invalid = [undefined, null, "0", "3", "1016064", false, true, {}, [], [0],
      new Number(0), new Uint8Array([0]), () => 0, Symbol("0"), 0n, NaN, Infinity,
      -Infinity, -1, -0.5, 0.5, 2 ** 32, 2 ** 32 + 1, Number.MAX_SAFE_INTEGER, hostile];
    for (const value of invalid) {
      assert.throws(() => state.terrain.prepareGround(state.floors, state.materials, value, requiredVertices));
      assert.throws(() => state.terrain.prepareGround(state.floors, state.materials, 1, value));
      for (let coordinate = 0; coordinate < 3; coordinate++) {
        const args = [0, 0, 0];
        args[coordinate] = value;
        assert.throws(() => state.terrain.groundTile(...args));
      }
      assert.equal(state.terrain.complete(), true);
      assert.equal(state.terrain.report(), report);
      assert.equal(state.terrain.groundTile(0, 0, 0), before);
    }
    for (const plane of [4, 255, 65535]) {
      assert.throws(() => state.terrain.prepareGround(state.floors, state.materials, plane, requiredVertices));
      assert.throws(() => state.terrain.groundTile(plane, 0, 0));
    }
    for (const args of [[0, 72, 0], [0, 0, 72], [0, 73, 0], [0, 0, 73]]) {
      assert.throws(() => state.terrain.groundTile(...args));
    }
    for (const budget of [maxVertices + 1, maxVertices + 0.5, 2 ** 32 - 1]) {
      assert.throws(() => state.terrain.prepareGround(state.floors, state.materials, 1, budget));
    }
    assert.equal(coerced, 0);
    assert.equal(state.terrain.groundTile(-0, -0, -0), before);
    assert.equal(state.terrain.groundTile(0, 0, 0), before);
    assert.deepEqual(prepare(state, -0), summary(0));
  } finally { state.free(); }
});

test("exact budget failures are transactional and successful preparations replace only one plane", () => {
  const state = ready();
  try {
    assert.deepEqual(prepare(state, 0, requiredVertices), summary(0));
    const before = state.terrain.groundTile(0, 71, 71);
    for (const budget of [0, requiredVertices - 1]) {
      assert.throws(() => prepare(state, 1, budget), /VertexBudget/);
      assert.equal(state.terrain.groundTile(0, 71, 71), before);
      assert.throws(() => state.terrain.groundTile(1, 71, 71), /plane not prepared/);
    }
    assert.deepEqual(prepare(state, 1, requiredVertices), summary(1));
    assert.throws(() => state.terrain.groundTile(0, 71, 71), /plane not prepared/);
    assert.deepEqual(JSON.parse(state.terrain.groundTile(1, 71, 71)), expectedTile(71, 71));
    assert.deepEqual(prepare(state, 3, maxVertices), summary(3));
    assert.throws(() => state.terrain.groundTile(1, 71, 71), /plane not prepared/);
  } finally { state.free(); }
});

test("zero-vertex ground admits a zero budget and returns JSON null only at valid retained coordinates", () => {
  const state = ready(emptyWire);
  try {
    for (let plane = 0; plane < 4; plane++) {
      assert.deepEqual(prepare(state, plane, -0), summary(plane, 0, 0));
      for (const [x, y] of [[0, 0], [0, 71], [71, 0], [35, 36], [71, 71]]) {
        assert.equal(state.terrain.groundTile(plane, x, y), "null");
      }
      assert.throws(() => state.terrain.groundTile(plane, 72, 0));
      assert.throws(() => state.terrain.groundTile((plane + 1) % 4, 0, 0), /plane not prepared/);
    }
    assert.deepEqual(prepare(state, 0, maxVertices), summary(0, 0, 0));
    assert.equal(state.terrain.groundTile(-0, -0, -0), "null");
  } finally { state.free(); }
});

test("verified completed probes plus both surface composition stages are required before publication", () => {
  const state = ready(underlayWire, false);
  const incompleteFloors = new Js5WorldFloorProbe("synthetic");
  const incompleteMaterials = new Js5WorldMaterialProbe("synthetic");
  const incompleteTerrain = new Js5WorldTerrainProbe("synthetic", JSON.stringify(regions));
  try {
    assert.throws(() => prepare(state), /surface not composed/);
    assert.throws(() => state.terrain.groundTile(0, 0, 0), /surface not composed/);
    assert.throws(() => incompleteTerrain.prepareGround(state.floors, state.materials, 0, requiredVertices));
    assert.throws(() => incompleteTerrain.groundTile(0, 0, 0), /not complete/);
    state.terrain.compose(56, 56, size);
    assert.throws(() => prepare(state), /palette not composed/);
    assert.throws(() => state.terrain.groundTile(0, 0, 0), /ground not prepared/);
    state.terrain.composePalette(state.floors);
    prepare(state);
    const before = state.terrain.groundTile(0, 0, 0);
    for (const [floors, materials, error] of [
      [incompleteFloors, state.materials, /floor probe not complete/],
      [state.floors, incompleteMaterials, /material probe not complete/],
    ]) {
      assert.throws(() => state.terrain.prepareGround(floors, materials, 1, requiredVertices), error);
      assert.equal(state.terrain.groundTile(0, 0, 0), before);
    }
    assert.throws(() => incompleteFloors.finish());
    assert.throws(() => incompleteMaterials.finish());
    assert.throws(() => state.terrain.prepareGround(incompleteFloors, state.materials, 1, requiredVertices));
    assert.throws(() => state.terrain.prepareGround(state.floors, incompleteMaterials, 1, requiredVertices));
    assert.equal(state.terrain.groundTile(0, 0, 0), before);
  } finally {
    incompleteTerrain.free();
    incompleteMaterials.free();
    incompleteFloors.free();
    state.free();
  }
});

test("successful height or palette recomposition invalidates ground while failed recomposition is nondestructive", () => {
  const state = ready();
  const incompleteFloors = new Js5WorldFloorProbe("synthetic");
  try {
    prepare(state);
    const before = state.terrain.groundTile(0, 0, 0);
    assert.throws(() => state.terrain.compose(57, 56, size), /layout/);
    assert.equal(state.terrain.groundTile(0, 0, 0), before);
    assert.throws(() => state.terrain.composePalette(incompleteFloors), /not complete/);
    assert.equal(state.terrain.groundTile(0, 0, 0), before);
    state.terrain.composePalette(state.floors);
    assert.throws(() => state.terrain.groundTile(0, 0, 0), /ground not prepared/);
    assert.deepEqual(prepare(state), summary(0));
    assert.equal(state.terrain.groundTile(0, 0, 0), before);
    state.terrain.compose(56, 56, size);
    assert.throws(() => state.terrain.groundTile(0, 0, 0), /ground not prepared/);
    assert.throws(() => prepare(state), /palette not composed/);
    state.terrain.composePalette(state.floors);
    assert.deepEqual(prepare(state), summary(0));
    assert.equal(state.terrain.groundTile(0, 0, 0), before);
  } finally { incompleteFloors.free(); state.free(); }
});

test("late terminal terrain feed failures discard previously prepared geometry and cannot recover", () => {
  for (const trailing of [Buffer.from([0]), Buffer.from([255, 255])]) {
    const state = ready();
    try {
      prepare(state);
      assert.deepEqual(JSON.parse(state.terrain.groundTile(0, 71, 71)), expectedTile(71, 71));
      assert.throws(() => state.terrain.feed(trailing), /unexpected bytes/);
      terminal(state);
    } finally { state.free(); }
  }
});

test("terminal terrain EOF integrity and coalesced trailing errors never publish or prepare ground", () => {
  const state = ready();
  try {
    for (const failure of ["eof", "crc", "coalesced-trailing"]) {
      const terrain = new Js5WorldTerrainProbe("synthetic", JSON.stringify(regions));
      const failed = { terrain, floors: state.floors, materials: state.materials };
      try {
        terrain.outgoing();
        if (failure === "eof") {
          terrain.feed(underlayWire.subarray(0, -1));
          assert.equal(terrain.complete(), false);
          assert.throws(() => terrain.finish());
        } else if (failure === "crc") {
          const bad = Buffer.from(underlayWire);
          bad[bad.length - 1] ^= 1;
          assert.throws(() => terrain.feed(bad), /invalid terrain container or integrity check/);
        } else {
          assert.throws(() => terrain.feed(Buffer.concat([underlayWire, Buffer.from([0])])), /unexpected bytes/);
        }
        terminal(failed);
      } finally { terrain.free(); }
    }
  } finally { state.free(); }
});
