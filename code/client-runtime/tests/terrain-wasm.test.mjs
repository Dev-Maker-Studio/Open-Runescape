import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import init, { Js5WorldTerrainProbe } from "../../web/static/protocol-runtime.js";

await init({ module_or_path: await readFile(new URL("../../web/static/protocol-runtime_bg.wasm", import.meta.url)) });
const wire = await readFile(new URL("./fixtures/terrain-native.wire", import.meta.url));
const oracle = JSON.parse(await readFile(new URL("./fixtures/terrain-native.json", import.meta.url), "utf8"));
const requested = oracle.provenance.requestedRegions;
const digest = (bytes) => createHash("sha256").update(bytes).digest("hex");

function replies(bytes) {
  const result = [];
  let at = 109;
  while (at < bytes.length) {
    const start = at;
    const index = bytes[at];
    const archive = bytes.readUInt32BE(at + 1);
    const compression = bytes[at + 5];
    const payload = bytes.readUInt32BE(at + 6) + (compression === 0 ? 0 : 4);
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
    result.push({ index, archive, bytes: bytes.subarray(start, at) });
  }
  assert.equal(at, bytes.length);
  return result;
}

const parts = replies(wire);
const composeIds = [0, 1, 256, 257];
const composeWire = Buffer.concat([
  wire.subarray(0, 109),
  ...parts.filter((part) => part.index === 255 || part.archive === 7).map((part) => part.bytes),
]);

function loaded(ids = composeIds, bytes = composeWire) {
  const probe = new Js5WorldTerrainProbe("synthetic", JSON.stringify(ids));
  probe.outgoing();
  probe.feed(bytes);
  assert.equal(probe.complete(), true);
  return probe;
}

function assertTerminal(probe) {
  assert.equal(probe.complete(), false);
  assert.equal(probe.outgoing().length, 0);
  assert.throws(() => probe.report());
  assert.throws(() => probe.height(0, 0, 0, 0));
  assert.throws(() => probe.compose(0, 0, 72));
  assert.throws(() => probe.sceneHeight(0, 0, 0));
  assert.throws(() => probe.sceneTile(0, 0, 0));
  assert.throws(() => probe.feed(new Uint8Array()));
  assert.throws(() => probe.finish());
}

test("terrain native fixture pins unchanged copied jar, helper, generator and JS5 bytes", async () => {
  assert.equal(oracle.provenance.jarSha256, "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048");
  assert.equal(digest(wire), oracle.wireSha256);
  assert.equal(digest(await readFile(new URL("./native/TerrainFixtures.java", import.meta.url))), oracle.provenance.helperSha256);
  assert.equal(digest(await readFile(new URL("./native/AssetChainFixtures.java", import.meta.url))), oracle.provenance.commonHelperSha256);
  assert.equal(digest(await readFile(new URL("./native/generate_terrain.py", import.meta.url))), oracle.provenance.generatorSha256);
  assert.deepEqual(parts.map(({ index, archive }) => [index, archive]), [[255, 255], [255, 5], [5, 62001], [5, 32000], [5, 7]]);
});

test("terrain constructor rejects empty, duplicate, oversized and non-u16 region JSON without coercion", () => {
  for (const json of ["", "null", "{}", "[]", "[1,1]", "[65536]", "[-1]", "[1.5]", "[\"1\"]", "[true]", "[null]", "[NaN]", "[0] trailing", JSON.stringify(Array.from({ length: 37 }, (_, i) => i)), " ".repeat(513) + "[0]"]) {
    assert.throws(() => new Js5WorldTerrainProbe("synthetic", json));
  }
  for (const value of [null, undefined, 0, false, [], {}]) {
    assert.throws(() => new Js5WorldTerrainProbe("synthetic", value));
  }
  const ids = Array.from({ length: 36 }, (_, i) => 65535 - i);
  const probe = new Js5WorldTerrainProbe("synthetic", JSON.stringify(ids));
  try {
    assert.equal(probe.complete(), false);
    assert.equal(probe.outgoing()[0], 15);
  } finally { probe.free(); }
});

test("terrain precompletion accessors never publish partial terrain or a synthetic composition", () => {
  const probe = new Js5WorldTerrainProbe("synthetic", JSON.stringify(composeIds));
  try {
    assert.equal(probe.complete(), false);
    assert.throws(() => probe.report(), /not complete/);
    assert.throws(() => probe.height(0, 0, 0, 0), /not complete/);
    assert.throws(() => probe.compose(0, 0, 72), /not complete/);
    assert.throws(() => probe.sceneHeight(0, 0, 0), /not complete/);
    assert.equal(typeof probe.file, "undefined");
    assert.equal(typeof probe.terrain, "undefined");
    assert.throws(() => probe.finish());
    assertTerminal(probe);
  } finally { probe.free(); }
});

test("fragmented original JS5 pipeline verifies names and native gzip terrain without exposing raw data", () => {
  for (const size of [1, 2, 7, 109, 511, 512, 513, wire.length]) {
    const probe = new Js5WorldTerrainProbe("synthetic", JSON.stringify(requested));
    try {
      probe.outgoing();
      let sent = 0;
      for (let at = 0; at < wire.length; at += size) {
        probe.feed(wire.subarray(at, at + size));
        sent += probe.outgoing().length;
      }
      assert.equal(sent, 42);
      assert.equal(probe.complete(), true);
      const report = JSON.parse(probe.report());
      assert.deepEqual(report.regions.map((region) => region.regionId), requested);
      assert.equal(report.index, 5);
      assert.equal(report.referenceRevision, 727);
      assert.equal(report.referenceCrcVerified, true);
      assert.equal(report.referenceWhirlpoolVerified, true);
      assert.equal(report.authenticated, false);
      assert.equal(report.totalWireBytes, wire.length);
      assert.equal(report.retainedArchives, oracle.regions.length);
      assert.ok(Number.isSafeInteger(report.referenceArchives));
      assert.ok(Number.isSafeInteger(report.referenceFiles));
      assert.ok(report.referenceArchives >= report.retainedArchives);
      assert.ok(report.referenceFiles >= report.retainedArchives);
      assert.ok(report.referenceArchives + report.referenceFiles <= 524288);
      assert.equal(report.limits.maxReferenceArchives, 262144);
      assert.equal(report.limits.maxReferenceFiles, 262144);
      assert.equal(report.limits.maxReferenceRecords, 524288);
      assert.equal(report.limits.maxRetainedArchives, 36);
      assert.equal(report.limits.maxRetainedFiles, 4096);
      assert.equal(report.limits.maxRegions, 36);
      assert.equal(report.limits.maxTagsPerTile, 256);
      assert.equal(report.limits.tileTagLimitIsDiagnostic, true);
      assert.ok(report.totalOperations <= report.limits.maxTotalOperations);
      assert.deepEqual(report.regions[0].origin, [16320, 16320]);
      assert.equal(report.regions[0].suffixBytes, 3);
      assert.equal(report.regions[0].effectsDecoded, false);
      assert.equal(report.regions[1].present, false);
      assert.equal(report.regions[1].archiveId, null);
      assert.equal(report.regions[1].tileCount, 0);
      for (const region of oracle.regions) {
        const actual = report.regions.find((entry) => entry.regionId === region.regionId);
        assert.equal(actual.archiveId, region.archiveId);
        assert.equal(actual.present, true);
        assert.equal(actual.expandedBytes, region.fileBytes);
        assert.equal(actual.tileCount, 16384);
        assert.equal(actual.crcVerified, true);
        assert.equal(actual.whirlpoolVerified, true);
        const height = region.height === 1 ? 0 : -32 * region.height;
        assert.equal(probe.height(region.regionId, 0, 0, 0), height);
        assert.equal(probe.height(region.regionId, 1, 0, 0), height - 960);
      }
      assert.throws(() => probe.height(12850, 0, 0, 0), /unavailable/);
      assert.throws(() => probe.height(2, 0, 0, 0), /unavailable/);
      assert.throws(() => probe.compose(0, 0, 72), /layout/);
      assert.equal(probe.complete(), true);
      assert.throws(() => probe.sceneHeight(0, 0, 0), /not composed/);
      assert.equal(probe.report().includes("synthetic"), false);
      assert.equal(probe.report().includes("m255_255"), false);
      probe.finish();
      probe.finish();
    } finally { probe.free(); }
  }
});

test("surface composition keeps native heights, proven absence, seams and positive padding distinct", () => {
  const probe = loaded();
  try {
    assert.throws(() => probe.sceneHeight(0, 0, 0), /not composed/);
    const summary = JSON.parse(probe.compose(0, 0, 72));
    assert.deepEqual(summary.baseTile, [0, 0]);
    assert.equal(summary.scope, "surface-only");
    assert.equal(summary.mapSize, 72);
    assert.equal(summary.stride, 73);
    assert.equal(summary.planes, 4);
    assert.equal(summary.heightCount, 4 * 73 * 73);
    assert.equal(summary.knownHeights, summary.heightCount);
    assert.equal(summary.unavailableHeights, 0);
    assert.equal(summary.effectsDecoded, false);
    assert.equal(summary.renderedWorld, false);
    assert.deepEqual(summary.regions, [
      { regionId: 0, availability: "present" },
      { regionId: 1, availability: "provenAbsent" },
      { regionId: 256, availability: "provenAbsent" },
      { regionId: 257, availability: "provenAbsent" },
    ]);
    for (const [plane, x, y] of [[0, 0, 0], [1, 0, 0], [0, 1, 1], [2, 63, 63]]) {
      assert.equal(probe.sceneHeight(plane, x, y), probe.height(0, plane, x, y));
    }
    assert.equal(probe.sceneHeight(0, 70, 70), 0);
    assert.equal(probe.sceneHeight(3, 70, 70), -2880);
    assert.equal(probe.sceneHeight(0, 64, 3), probe.height(0, 0, 63, 3));
    assert.equal(probe.sceneHeight(2, 3, 64), probe.height(0, 2, 3, 63));
    for (const plane of [0, 1, 2, 3]) {
      assert.equal(probe.sceneHeight(plane, 72, 0), 0);
      assert.equal(probe.sceneHeight(plane, 0, 72), 0);
      assert.equal(probe.sceneHeight(plane, 72, 72), 0);
    }
    const clipped = JSON.parse(probe.compose(8, 8, 72));
    assert.deepEqual(clipped.baseTile, [8, 8]);
    assert.equal(probe.sceneHeight(0, 0, 0), probe.height(0, 0, 8, 8));
    const larger = JSON.parse(probe.compose(0, 0, 104));
    assert.equal(larger.stride, 105);
    assert.equal(probe.sceneHeight(3, 104, 104), 0);
    probe.finish();
    assert.equal(probe.sceneHeight(0, 1, 1), probe.height(0, 0, 1, 1));
  } finally { probe.free(); }
});

test("height and composition coordinates reject coercion, wrapping and invalid layouts nondestructively", () => {
  const probe = loaded();
  try {
    probe.compose(0, 0, 72);
    const before = probe.sceneHeight(0, 1, 1);
    const report = probe.report();
    const invalid = [undefined, null, "0", false, true, {}, [], 0n, NaN, Infinity, -Infinity, -1, 0.5, 2 ** 32];
    for (const value of invalid) {
      for (let coordinate = 0; coordinate < 4; coordinate++) {
        const args = [0, 0, 0, 0];
        args[coordinate] = value;
        assert.throws(() => probe.height(...args));
      }
      for (let coordinate = 0; coordinate < 3; coordinate++) {
        const args = [0, 1, 1];
        args[coordinate] = value;
        assert.throws(() => probe.sceneHeight(...args));
        const layout = [0, 0, 72];
        layout[coordinate] = value;
        assert.throws(() => probe.compose(...layout));
      }
    }
    for (const args of [[65536, 0, 0, 0], [0, 4, 0, 0], [0, 0, 64, 0], [0, 0, 0, 64]]) {
      assert.throws(() => probe.height(...args));
    }
    for (const args of [[4, 0, 0], [0, 73, 0], [0, 0, 73]]) assert.throws(() => probe.sceneHeight(...args));
    for (const args of [[1, 0, 72], [0, 1, 72], [0, 0, 64], [0, 0, 136], [16384, 0, 72], [64, 64, 72]]) {
      assert.throws(() => probe.compose(...args));
    }
    assert.equal(probe.complete(), true);
    assert.equal(probe.report(), report);
    assert.equal(probe.sceneHeight(0, 1, 1), before);
  } finally { probe.free(); }
});

test("served scene tile binding validates raw attributes, clipped recomposition and nondestructive coordinates", () => {
  const probe = loaded();
  const empty = { underlayId: 0, overlayId: 0, renderFlags: 0, overlayShape: 0, overlayRotation: 0 };
  try {
    assert.throws(() => probe.sceneTile(0, 0, 0), /not composed/);
    probe.compose(0, 0, 72);
    for (let plane = 0; plane < 4; plane++) {
      for (let x = 0; x < 72; x++) {
        for (let y = 0; y < 72; y++) assert.deepEqual(JSON.parse(probe.sceneTile(plane, x, y)), empty);
      }
    }
    const before = probe.sceneTile(3, 71, 71);
    for (const value of [undefined, null, "0", false, true, [], {}, 0n, NaN, Infinity, -Infinity, -1, 0.5, 2 ** 32]) {
      for (let coordinate = 0; coordinate < 3; coordinate++) {
        const args = [0, 0, 0];
        args[coordinate] = value;
        assert.throws(() => probe.sceneTile(...args));
      }
    }
    for (const args of [[4, 0, 0], [0, 72, 0], [0, 0, 72]]) assert.throws(() => probe.sceneTile(...args));
    assert.equal(probe.sceneTile(3, 71, 71), before);
    assert.throws(() => probe.compose(1, 0, 72));
    assert.equal(probe.sceneTile(3, 71, 71), before);
    probe.compose(8, 8, 72);
    assert.deepEqual(JSON.parse(probe.sceneTile(0, 55, 55)), empty);
    assert.deepEqual(JSON.parse(probe.sceneTile(0, 56, 56)), empty);
    probe.compose(0, 0, 104);
    assert.deepEqual(JSON.parse(probe.sceneTile(3, 103, 103)), empty);
    assert.throws(() => probe.feed(new Uint8Array([0])));
    assertTerminal(probe);
  } finally { probe.free(); }
});

test("feed failure invalidates even previously complete terrain and stored composition", () => {
  for (const trailing of [new Uint8Array([0]), new Uint8Array([255, 255])]) {
    const probe = loaded();
    try {
      probe.compose(0, 0, 72);
      assert.equal(typeof probe.sceneHeight(0, 1, 1), "number");
      assert.throws(() => probe.feed(trailing));
      assertTerminal(probe);
    } finally { probe.free(); }
  }
  for (const kind of ["crc", "eof", "identity", "coalesced-trailing"]) {
    const probe = new Js5WorldTerrainProbe("synthetic", JSON.stringify(requested));
    try {
      const bytes = Buffer.from(wire);
      if (kind === "crc") bytes[bytes.length - 1] ^= 1;
      if (kind === "identity") bytes[109] = 4;
      if (kind === "eof") {
        probe.feed(bytes.subarray(0, bytes.length - 1));
        assert.throws(() => probe.finish());
      } else if (kind === "coalesced-trailing") {
        assert.throws(() => probe.feed(Buffer.concat([bytes, Buffer.from([0])])));
      } else {
        assert.throws(() => probe.feed(bytes));
      }
      assertTerminal(probe);
    } finally { probe.free(); }
  }
});
