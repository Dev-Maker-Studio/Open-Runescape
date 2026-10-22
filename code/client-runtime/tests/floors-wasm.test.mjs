import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import init, { Js5WorldFloorProbe, Js5WorldTerrainProbe } from "../../web/static/protocol-runtime.js";

await init({ module_or_path: await readFile(new URL("../../web/static/protocol-runtime_bg.wasm", import.meta.url)) });
const fixture = (path) => readFile(new URL(path, import.meta.url));
const floorWire = await fixture("./fixtures/floors-native.wire");
const floorOracle = JSON.parse(await fixture("./fixtures/floors-native.json"));
const terrainWire = await fixture("./fixtures/terrain-native.wire");
const terrainOracle = JSON.parse(await fixture("./fixtures/terrain-native.json"));
const nativePalette = await fixture("../../scene/tests/fixtures/palette.tsv");
const paletteRows = nativePalette.toString("utf8").trim().split("\n").map((line) => line.split("\t"));
const digest = (bytes) => createHash("sha256").update(bytes).digest("hex");

function replies(bytes) {
  const result = [];
  let at = 109;
  while (at < bytes.length) {
    const start = at;
    const index = bytes[at];
    const archive = bytes.readUInt32BE(at + 1);
    const compression = bytes[at + 5];
    const compressedBytes = bytes.readUInt32BE(at + 6);
    const payload = compressedBytes + (compression === 0 ? 0 : 4);
    const expandedBytes = compression === 0 ? compressedBytes : bytes.readUInt32BE(at + 10);
    at += 10;
    let position = 10;
    for (let i = 0; i < payload; i++) {
      if (position === 512) {
        assert.equal(bytes[at++], 255);
        position = 1;
      }
      at++;
      position++;
    }
    assert.ok(at <= bytes.length);
    result.push({ index, archive, start, end: at, bytes: bytes.subarray(start, at), containerBytes: 5 + payload, expandedBytes });
  }
  assert.equal(at, bytes.length);
  return result;
}

const floorParts = replies(floorWire);
const terrainParts = replies(terrainWire);
const composeIds = [0, 1, 256, 257];
const composeWire = Buffer.concat([
  terrainWire.subarray(0, 109),
  ...terrainParts.filter((part) => part.index === 255 || part.archive === 7).map((part) => part.bytes),
]);
const invalidNumbers = [undefined, null, "0", "65535", false, true, {}, [], [0], new Number(0), new Uint8Array([0]), () => 0, Symbol("0"), 0n, NaN, Infinity, -Infinity, -1, -0.5, 0.5, 2 ** 32, 2 ** 32 + 1, Number.MAX_SAFE_INTEGER];

function loadedFloors() {
  const probe = new Js5WorldFloorProbe("synthetic");
  try {
    probe.outgoing();
    probe.feed(floorWire);
    assert.equal(probe.complete(), true);
    return probe;
  } catch (error) { probe.free(); throw error; }
}

function loadedTerrain() {
  const probe = new Js5WorldTerrainProbe("synthetic", JSON.stringify(composeIds));
  try {
    probe.outgoing();
    probe.feed(composeWire);
    assert.equal(probe.complete(), true);
    return probe;
  } catch (error) { probe.free(); throw error; }
}

function assertFloorTerminal(probe) {
  assert.equal(probe.complete(), false);
  assert.equal(probe.outgoing().length, 0);
  assert.throws(() => probe.report(), /not complete/);
  for (const id of [0, 3, 65535]) {
    assert.throws(() => probe.underlay(id), /not complete/);
    assert.throws(() => probe.overlay(id), /not complete/);
  }
  assert.throws(() => probe.feed(new Uint8Array()));
  assert.throws(() => probe.finish());
}

function assertPaletteSummary(summary, mapSize = 72) {
  assert.deepEqual(summary, {
    scope: "underlay-packed-hsl",
    mapSize,
    planes: 4,
    tileCount: 4 * mapSize * mapSize,
    knownColors: 4 * mapSize * mapSize,
    unavailableColors: 0,
    unavailableDefinitionIds: [],
    rgbPrepared: false,
    renderedWorld: false,
  });
  for (const key of ["mapSize", "planes", "tileCount", "knownColors", "unavailableColors"]) {
    assert.equal(Number.isSafeInteger(summary[key]), true);
  }
}

test("floors and palette evidence pins copied native jar, isolated generators and offline wire bytes", async () => {
  const jarSha256 = "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048";
  for (const [name, oracle, wire, helper] of [
    ["floors", floorOracle, floorWire, "FloorFixtures.java"],
    ["terrain", terrainOracle, terrainWire, "TerrainFixtures.java"],
  ]) {
    assert.equal(oracle.provenance.jarSha256, jarSha256);
    assert.equal(digest(wire), oracle.wireSha256);
    assert.equal(digest(await fixture(`./native/${helper}`)), oracle.provenance.helperSha256);
    assert.equal(digest(await fixture("./native/AssetChainFixtures.java")), oracle.provenance.commonHelperSha256);
    assert.equal(digest(await fixture(`./native/generate_${name}.py`)), oracle.provenance.generatorSha256);
    assert.match(oracle.provenance.isolation, /bwrap --unshare-net/);
    assert.match(oracle.provenance.scope, /no native startup, real cache, network, account or display/);
  }
  assert.equal(digest(nativePalette), "583a59c39fb999eacdbf66910ac90820ff96d17b904d999eba7c84e234c8e37b");
  assert.deepEqual(paletteRows.filter(([kind]) => kind === "jar-sha256"), [["jar-sha256", jarSha256]]);
  assert.deepEqual(paletteRows.filter(([kind]) => kind === "palette-method-sha256"), [["palette-method-sha256", "3543825198d091b2dabff021da1b107b57b327f8769d7377a5a9bf019dfc51f3"]]);
  assert.deepEqual(floorParts.map(({ index, archive }) => [index, archive]), [[255, 255], [255, 2], [2, 1], [2, 4]]);
  assert.deepEqual(terrainParts.map(({ index, archive }) => [index, archive]), [[255, 255], [255, 5], [5, 62001], [5, 32000], [5, 7]]);
});

test("fragmented real WASM floor chain matches every native flat definition and reports bounded verification", () => {
  for (const size of [1, 2, 7, 109, 511, 512, 513, floorWire.length]) {
    const probe = new Js5WorldFloorProbe("synthetic");
    try {
      assert.equal(probe.outgoing()[0], 15);
      let sent = 0;
      for (let at = 0; at < floorWire.length; at += size) {
        probe.feed(floorWire.subarray(at, at + size));
        sent += probe.outgoing().length;
      }
      assert.equal(sent, 36);
      assert.equal(probe.complete(), true);
      const report = JSON.parse(probe.report());
      assert.equal(report.revision, 727);
      assert.equal(report.subrevision, 1);
      assert.equal(report.prefetchCount, 27);
      assert.equal(report.index, 2);
      assert.equal(report.referenceRevision, 727);
      assert.equal(report.referenceArchives, 4);
      assert.equal(report.referenceFiles, 8);
      assert.equal(report.referenceCrcVerified, true);
      assert.equal(report.referenceWhirlpoolVerified, true);
      assert.equal(report.authenticated, false);
      assert.equal(report.renderedWorld, false);
      assert.equal(report.opcode8SideEffectsApplied, false);
      assert.equal(report.totalWireBytes, floorWire.length);
      assert.equal(report.totalContainerBytes, floorParts.reduce((total, part) => total + part.containerBytes, 0));
      assert.equal(report.totalExpandedBytes, floorParts.reduce((total, part) => total + part.expandedBytes, 0));
      for (const [name, archive, slots, populatedDefinitions, definitionBytes] of [
        ["underlays", 1, floorOracle.underlayFileSlots, 3, 19],
        ["overlays", 4, floorOracle.overlayFileSlots, 2, 13],
      ]) {
        const part = floorParts.find((entry) => entry.index === 2 && entry.archive === archive);
        assert.deepEqual(report[name], {
          archive, archiveRevision: 44, containerBytes: part.containerBytes,
          expandedBytes: part.expandedBytes, fileSlots: slots, populatedDefinitions,
          definitionBytes, crcVerified: true, whirlpoolVerified: true,
        });
        for (const expected of floorOracle[name]) assert.deepEqual(JSON.parse(probe[name === "underlays" ? "underlay" : "overlay"](expected.id)), expected);
      }
      assert.deepEqual(report.limits, {
        maxReferenceArchives: 65536, maxReferenceFiles: 262144,
        maxDefinitionsPerArchive: 65536, maxTotalDefinitions: 131072, maxFloorId: 65535,
        maxDefinitionBytes: 4096, maxTagsPerDefinition: 256,
        maxContainerBytes: 4 * 1024 * 1024, maxExpandedContainerBytes: 8 * 1024 * 1024,
        maxTotalWireBytes: 20 * 1024 * 1024, maxTotalContainerBytes: 16 * 1024 * 1024,
        maxTotalExpandedBytes: 24 * 1024 * 1024, maxTotalOperations: 128 * 1024 * 1024,
        operationUnit: "wire/container/expanded/split/definition bytes and bounded metadata records",
      });
      for (const [actual, limit] of [["totalWireBytes", "maxTotalWireBytes"], ["totalContainerBytes", "maxTotalContainerBytes"], ["totalExpandedBytes", "maxTotalExpandedBytes"], ["totalOperations", "maxTotalOperations"]]) {
        assert.equal(Number.isSafeInteger(report[actual]), true);
        assert.ok(report[actual] > 0 && report[actual] <= report.limits[limit]);
      }
      assert.equal(JSON.parse(probe.underlay(2)).present, false);
      assert.equal(JSON.parse(probe.underlay(3)).present, true);
      assert.equal(JSON.parse(probe.overlay(5)).slot, 65285);
      assert.equal(JSON.parse(probe.overlay(65535)).slot, 65535);
      assert.deepEqual(floorOracle.nativeLazyOverlayIds, [5, 1, 5, 0, 65535]);
      assert.deepEqual(floorOracle.nativeLazyOpcode8Trace, [5, 1, 1, 1, 1]);
      assert.equal(typeof probe.file, "undefined");
      assert.equal(typeof probe.repository, "undefined");
      assert.equal(probe.report().includes("synthetic"), false);
      probe.finish();
      probe.finish();
      probe.feed(new Uint8Array());
      assert.equal(probe.complete(), true);
    } finally { probe.free(); }
  }
});

test("floor lookups require both sequential archive replies and finish clears every incomplete stage", () => {
  const probe = new Js5WorldFloorProbe("synthetic");
  try {
    probe.outgoing();
    probe.feed(floorWire.subarray(0, 109));
    assert.equal(probe.outgoing().length, 18);
    for (const [part, next] of [[floorParts[0], [1, 255, 0, 0, 0, 2]], [floorParts[1], [1, 2, 0, 0, 0, 1]], [floorParts[2], [1, 2, 0, 0, 0, 4]]]) {
      probe.feed(part.bytes);
      assert.deepEqual(Array.from(probe.outgoing()), next);
      assert.equal(probe.complete(), false);
      assert.throws(() => probe.report(), /not complete/);
      assert.throws(() => probe.underlay(0), /not complete/);
      assert.throws(() => probe.overlay(1), /not complete/);
    }
    probe.feed(floorParts[3].bytes);
    assert.equal(probe.complete(), true);
    assert.equal(probe.outgoing().length, 0);
  } finally { probe.free(); }
  for (const end of [0, 1, 108, 109, ...floorParts.slice(0, -1).map((part) => part.end), floorWire.length - 1]) {
    const incomplete = new Js5WorldFloorProbe("synthetic");
    try {
      incomplete.feed(floorWire.subarray(0, end));
      assert.equal(incomplete.complete(), false);
      assert.throws(() => incomplete.underlay(0), /not complete/);
      assert.throws(() => incomplete.overlay(65535), /not complete/);
      assert.throws(() => incomplete.finish());
      assertFloorTerminal(incomplete);
      assert.throws(() => incomplete.feed(floorWire.subarray(end)));
    } finally { incomplete.free(); }
  }
});

test("floor IDs accept u16 endpoints and reject direct JS coercion and wrapping without discarding the repository", () => {
  const probe = loadedFloors();
  try {
    const before = probe.report();
    let coerced = 0;
    const hostile = { valueOf() { coerced++; return 0; }, toString() { coerced++; return "0"; } };
    for (const id of [...invalidNumbers, 65536, 65535.5, hostile]) {
      assert.throws(() => probe.underlay(id));
      assert.throws(() => probe.overlay(id));
      assert.equal(probe.complete(), true);
      assert.equal(probe.report(), before);
      assert.deepEqual(JSON.parse(probe.underlay(0)), floorOracle.underlays[0]);
      assert.deepEqual(JSON.parse(probe.overlay(65535)), floorOracle.overlays.at(-1));
    }
    assert.equal(coerced, 0);
    for (const id of [0, 1, 2, 3, 65534, 65535]) {
      assert.equal(JSON.parse(probe.underlay(id)).id, id);
      assert.equal(JSON.parse(probe.overlay(id)).overlayId, id);
    }
    probe.finish();
  } finally { probe.free(); }
});

test("floor integrity identity and trailing failures permanently hide all decoded definitions", () => {
  for (const trailing of [Buffer.from([0]), Buffer.from([255, 255])]) {
    const probe = loadedFloors();
    try {
      assert.throws(() => probe.feed(trailing));
      assertFloorTerminal(probe);
    } finally { probe.free(); }
  }
  for (const kind of ["reference", "underlay", "overlay", "identity", "coalesced-trailing", "handshake"]) {
    const probe = new Js5WorldFloorProbe("synthetic");
    try {
      const bytes = Buffer.from(floorWire);
      if (kind === "reference") bytes[floorParts[1].end - 1] ^= 1;
      if (kind === "underlay") bytes[floorParts[2].end - 1] ^= 1;
      if (kind === "overlay") bytes[floorParts[3].end - 1] ^= 1;
      if (kind === "identity") bytes[floorParts[3].start] = 5;
      const invalid = kind === "coalesced-trailing" ? Buffer.concat([bytes, Buffer.from([0])]) : kind === "handshake" ? Buffer.from([6]) : bytes;
      assert.throws(() => probe.feed(invalid));
      assertFloorTerminal(probe);
    } finally { probe.free(); }
  }
});

test("palette access before complete terrain or surface composition and with incomplete floors stays unavailable", () => {
  const terrain = new Js5WorldTerrainProbe("synthetic", JSON.stringify(composeIds));
  const floors = loadedFloors();
  const incomplete = new Js5WorldFloorProbe("synthetic");
  try {
    assert.throws(() => terrain.paletteColor(0, 0, 0), /not complete/);
    assert.throws(() => terrain.composePalette(floors), /not composed/);
    terrain.feed(composeWire);
    assert.throws(() => terrain.paletteColor(0, 0, 0), /not composed/);
    assert.throws(() => terrain.composePalette(floors), /not composed/);
    terrain.compose(0, 0, 72);
    incomplete.feed(floorWire.subarray(0, floorParts[2].end));
    assert.throws(() => terrain.composePalette(incomplete), /not complete/);
    assert.throws(() => terrain.paletteColor(0, 0, 0), /not composed/);
    assert.throws(() => incomplete.finish());
    assert.throws(() => terrain.composePalette(incomplete), /not complete/);
    assert.throws(() => terrain.paletteColor(0, 0, 0), /not composed/);
    assertPaletteSummary(JSON.parse(terrain.composePalette(floors)));
    const report = terrain.report();
    assert.throws(() => terrain.composePalette(incomplete), /not complete/);
    assert.equal(terrain.paletteColor(0, 0, 0), 0);
    assert.equal(terrain.report(), report);
  } finally { terrain.free(); floors.free(); incomplete.free(); }
});

test("native empty-underlay fixture palette matches all signed-i32 cells, native samples and every plane hash", () => {
  const terrain = loadedTerrain();
  const floors = loadedFloors();
  try {
    terrain.compose(0, 0, 72);
    assertPaletteSummary(JSON.parse(terrain.composePalette(floors)));
    const cases = paletteRows.filter(([kind, , size, pattern]) => kind === "case" && size === "72" && pattern === "empty");
    assert.equal(cases.length, 1);
    const caseId = cases[0][1];
    const nativeHashes = paletteRows.filter(([kind, id]) => kind === "plane-hash" && id === caseId);
    const samples = paletteRows.filter(([kind, id]) => kind === "sample" && id === caseId);
    assert.equal(nativeHashes.length, 4);
    assert.equal(samples.length, 384);
    for (const [, , plane, expectedHash] of nativeHashes) {
      const bytes = Buffer.alloc(72 * 72 * 4);
      for (let x = 0; x < 72; x++) {
        for (let y = 0; y < 72; y++) {
          const color = terrain.paletteColor(Number(plane), x, y);
          assert.equal(typeof color, "number");
          assert.equal(Number.isInteger(color), true);
          assert.ok(color >= -(2 ** 31) && color <= 2 ** 31 - 1);
          assert.notEqual(color, null);
          bytes.writeInt32LE(color, (x * 72 + y) * 4);
        }
      }
      let hash = 0xcbf29ce484222325n;
      for (const byte of bytes) hash = BigInt.asUintN(64, (hash ^ BigInt(byte)) * 0x100000001b3n);
      assert.equal(hash.toString(16).padStart(16, "0"), expectedHash);
    }
    for (const [, , plane, x, y, expected] of samples) assert.equal(terrain.paletteColor(Number(plane), Number(x), Number(y)), Number(expected));
    for (const plane of [0, 1, 2, 3]) {
      for (const [x, y] of [[0, 0], [63, 63], [63, 64], [64, 63], [64, 64], [71, 71]]) assert.equal(terrain.paletteColor(plane, x, y), 0);
    }
    terrain.finish();
    floors.finish();
    assert.equal(terrain.paletteColor(3, 71, 71), 0);
  } finally { terrain.free(); floors.free(); }
});

test("palette coordinates reject null, nonnumbers, bounds and casts without changing a composed palette", () => {
  const terrain = loadedTerrain();
  const floors = loadedFloors();
  try {
    terrain.compose(0, 0, 72);
    const summary = terrain.composePalette(floors);
    const report = terrain.report();
    let coerced = 0;
    const hostile = { valueOf() { coerced++; return 0; }, toString() { coerced++; return "0"; } };
    for (const value of [...invalidNumbers, hostile]) {
      for (let coordinate = 0; coordinate < 3; coordinate++) {
        const args = [0, 0, 0];
        args[coordinate] = value;
        assert.throws(() => terrain.paletteColor(...args));
      }
    }
    for (const args of [[4, 0, 0], [256, 0, 0], [0, 72, 0], [0, 0, 72], [0, 65536, 0], [0, 0, 65536]]) assert.throws(() => terrain.paletteColor(...args));
    assert.equal(coerced, 0);
    assert.equal(terrain.complete(), true);
    assert.equal(terrain.report(), report);
    assert.equal(terrain.paletteColor(0, 0, 0), 0);
    assert.equal(terrain.paletteColor(3, 71, 71), 0);
    assert.equal(terrain.composePalette(floors), summary);
  } finally { terrain.free(); floors.free(); }
});

test("invalid surface compose preserves palette while successful recompose clears it until recomposed", () => {
  const terrain = loadedTerrain();
  const floors = loadedFloors();
  try {
    terrain.compose(0, 0, 72);
    const summary = terrain.composePalette(floors);
    for (const args of [[null, 0, 72], [2 ** 32, 0, 72], [1, 0, 72], [0, 1, 72], [0, 0, 64], [0, 0, 136], [64, 64, 72]]) {
      assert.throws(() => terrain.compose(...args));
      assert.equal(terrain.paletteColor(0, 0, 0), 0);
      assert.equal(terrain.paletteColor(3, 71, 71), 0);
      assert.equal(terrain.complete(), true);
    }
    for (const args of [[0, 0, 72], [8, 8, 72], [0, 0, 104]]) {
      const surface = JSON.parse(terrain.compose(...args));
      assert.equal(surface.mapSize, args[2]);
      assert.equal(typeof terrain.sceneHeight(0, 0, 0), "number");
      assert.throws(() => terrain.paletteColor(0, 0, 0), /not composed/);
      assertPaletteSummary(JSON.parse(terrain.composePalette(floors)), args[2]);
      assert.equal(terrain.paletteColor(3, args[2] - 1, args[2] - 1), 0);
      assert.throws(() => terrain.paletteColor(0, args[2], 0));
    }
    terrain.compose(0, 0, 72);
    assert.equal(terrain.composePalette(floors), summary);
  } finally { terrain.free(); floors.free(); }
});

test("terminal terrain failure drops owned palette and surface even after successful composition", () => {
  const floors = loadedFloors();
  try {
    for (const trailing of [Buffer.from([0]), Buffer.from([255, 255])]) {
      const terrain = loadedTerrain();
      try {
        terrain.compose(0, 0, 72);
        terrain.composePalette(floors);
        assert.equal(terrain.paletteColor(0, 0, 0), 0);
        assert.throws(() => terrain.feed(trailing));
        assert.equal(terrain.complete(), false);
        assert.equal(terrain.outgoing().length, 0);
        assert.throws(() => terrain.report());
        assert.throws(() => terrain.sceneHeight(0, 0, 0));
        assert.throws(() => terrain.paletteColor(0, 0, 0), /not complete/);
        assert.throws(() => terrain.compose(0, 0, 72));
        assert.throws(() => terrain.composePalette(floors));
        assert.throws(() => terrain.feed(new Uint8Array()));
        assert.throws(() => terrain.finish());
        assert.equal(floors.complete(), true);
        assert.deepEqual(JSON.parse(floors.underlay(0)), floorOracle.underlays[0]);
      } finally { terrain.free(); }
    }
  } finally { floors.free(); }
});

test("composed palette owns its data after the floor repository fails or its WASM wrapper is freed", () => {
  for (const failFirst of [false, true]) {
    const terrain = loadedTerrain();
    const floors = loadedFloors();
    let freed = false;
    try {
      terrain.compose(0, 0, 72);
      assertPaletteSummary(JSON.parse(terrain.composePalette(floors)));
      if (failFirst) {
        assert.throws(() => floors.feed(Buffer.from([0])));
        assertFloorTerminal(floors);
        assert.throws(() => terrain.composePalette(floors), /not complete/);
      }
      assert.equal(terrain.paletteColor(0, 0, 0), 0);
      floors.free();
      freed = true;
      for (const plane of [0, 1, 2, 3]) assert.equal(terrain.paletteColor(plane, 71, 71), 0);
      terrain.finish();
      assert.equal(terrain.paletteColor(0, 0, 0), 0);
      terrain.compose(0, 0, 72);
      assert.throws(() => terrain.paletteColor(0, 0, 0), /not composed/);
    } finally { terrain.free(); if (!freed) floors.free(); }
  }
});
