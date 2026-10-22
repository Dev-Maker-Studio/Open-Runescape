import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import init, { Js5WorldMaterialProbe } from "../../web/static/protocol-runtime.js";

await init({ module_or_path: await readFile(new URL("../../web/static/protocol-runtime_bg.wasm", import.meta.url)) });
const fixture = (path) => readFile(new URL(path, import.meta.url));
const wire = await fixture("./fixtures/material-native.wire");
const oracle = JSON.parse(await fixture("./fixtures/material-native.json"));
const readonlyFixtures = new Map(await Promise.all(Object.keys(oracle.fixtures).map(async (name) => [name, await fixture(`./fixtures/${name}`)])));
const native = await fixture("../../scene/tests/fixtures/material.tsv");
const nativeRows = native.toString("utf8").trim().split("\n").map((line) => line.split("\t"));
const digest = (bytes) => createHash("sha256").update(bytes).digest("hex");
const names = ["detailsOnly", "isSmall", "skipTriangles", "brightness", "alpha", "effectId", "effectParam1", "colorHsl", "textureSpeedU", "textureSpeedV", "aBool2087", "isBrickTile", "mipmapping", "repeatS", "repeatT", "hdr", "combineMode", "effectParam2", "effectCombiner"];
const bools = new Set([0, 1, 2, 10, 11, 13, 14, 15]);

function expectedMaterial(id, values) {
  return Object.fromEntries([["id", id], ...names.map((name, at) => [name, bools.has(at) ? values[at] === 1 : values[at]])]);
}

function nativeMaterials(name) {
  return nativeRows.filter(([kind, value]) => kind === "material" && value === name)
    .map(([, , id, ...values]) => expectedMaterial(Number(id), values.map(Number)));
}

function replies(bytes) {
  const result = [];
  let at = 109;
  while (at < bytes.length) {
    const start = at;
    const index = bytes[at];
    const archive = bytes.readUInt32BE(at + 1);
    const size = bytes.readUInt32BE(at + 6) + (bytes[at + 5] === 0 ? 0 : 4);
    const container = [...bytes.subarray(at + 5, at + 10)];
    at += 10;
    let position = 10;
    for (let i = 0; i < size; i++) {
      if (position === 512) {
        assert.equal(bytes[at++], 255);
        position = 1;
      }
      container.push(bytes[at++]);
      position++;
    }
    assert.ok(at <= bytes.length);
    result.push({ index, archive, start, end: at, container: Buffer.from(container), bytes: bytes.subarray(start, at) });
  }
  assert.equal(at, bytes.length);
  return result;
}

function packReply(index, archive, container) {
  const header = Buffer.alloc(10);
  header[0] = index;
  header.writeUInt32BE(archive, 1);
  container.copy(header, 5, 0, 5);
  const bytes = [...header];
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

const parts = replies(wire);

function loaded(bytes = wire) {
  const probe = new Js5WorldMaterialProbe("synthetic");
  try {
    probe.outgoing();
    probe.feed(bytes);
    assert.equal(probe.complete(), true);
    return probe;
  } catch (error) { probe.free(); throw error; }
}

function unpublished(probe) {
  assert.equal(probe.complete(), false);
  assert.throws(() => probe.report(), /not complete/);
  for (const id of [0, 128, 154, 65535]) assert.throws(() => probe.material(id), /not complete/);
}

function terminal(probe) {
  unpublished(probe);
  assert.equal(probe.outgoing().length, 0);
  assert.throws(() => probe.feed(new Uint8Array()), /has failed/);
  assert.throws(() => probe.feed(wire), /has failed/);
  assert.throws(() => probe.finish(), /has failed/);
  unpublished(probe);
}

function assertSummary(probe, expectedSlots = 4, expectedPresent = 2, archiveWhirlpool = true) {
  const report = JSON.parse(probe.report());
  assert.equal(report.scope, "material-metadata");
  assert.equal(report.slots, expectedSlots);
  assert.equal(report.present, expectedPresent);
  assert.equal(report.texturePixelsDecoded, false);
  assert.equal(report.renderedWorld, false);
  assert.equal(report.asset.revision, 727);
  assert.equal(report.asset.subrevision, 1);
  assert.equal(report.asset.prefetchCount, 27);
  assert.equal(report.asset.index, 26);
  assert.equal(report.asset.archive, 0);
  assert.equal(report.asset.referenceRevision, 727);
  assert.equal(report.asset.archiveRevision, 44);
  assert.equal(report.asset.referenceCrcVerified, true);
  assert.equal(report.asset.referenceWhirlpoolVerified, true);
  assert.equal(report.asset.archiveCrcVerified, true);
  assert.equal(report.asset.archiveWhirlpoolVerified, archiveWhirlpool);
  assert.equal(report.asset.authenticated, false);
  assert.equal(report.asset.fileSlots, 1);
  assert.equal(report.asset.files.length, 1);
  assert.equal(report.asset.files[0].id, 0);
  assert.equal(report.asset.files[0].script, null);
  assert.equal(probe.report().includes("synthetic"), false);
  assert.equal(typeof probe.file, "undefined");
  assert.equal(typeof probe.table, "undefined");
  assert.equal(typeof probe.getPixels, "undefined");
  return report;
}

test("material fixtures pin native metadata values and explicitly synthetic unauthenticated JS5 transport", async () => {
  assert.equal(oracle.sourceJarSha256, "78edce8b9dd6ef6cf712ebadb1ed3299aa3dec4c1e885dcebd99476aa5d76048");
  assert.equal(digest(native), oracle.sourceFixtureSha256);
  assert.deepEqual(nativeRows.filter(([kind]) => kind === "jar-sha256"), [["jar-sha256", oracle.sourceJarSha256]]);
  assert.match(oracle.origin, /Synthetic JS5/);
  assert.match(oracle.origin, /not captured live cache or actual material IDs128\/154/);
  assert.match(oracle.extraFilesPolicy, /stricter than native/);
  assert.equal(oracle.authenticated, false);
  assert.equal(Object.keys(oracle.fixtures).length, 11);
  for (const [name, expected] of Object.entries(oracle.fixtures)) {
    const data = await fixture(`./fixtures/${name}`);
    assert.equal(data.length, expected.bytes);
    assert.equal(digest(data), expected.sha256);
  }
  assert.deepEqual(parts.map(({ index, archive }) => [index, archive]), [[255, 255], [255, 26], [26, 0]]);
  assert.equal(parts[2].container.subarray(5).toString("hex"), oracle.materialPayloadHex);
  assert.deepEqual(nativeMaterials(oracle.nativeCase), oracle.materialFields.map(({ id, values }) => expectedMaterial(id, values)));
});

test("real WASM fragmented material chain publishes all nineteen exact signed fields only on final complete file", () => {
  const expected = nativeMaterials(oracle.nativeCase);
  for (const size of [1, 2, 7, 109, 511, 512, 513, wire.length]) {
    const probe = new Js5WorldMaterialProbe("synthetic");
    try {
      assert.equal(probe.outgoing()[0], 15);
      let sent = 0;
      for (let at = 0; at < wire.length; at += size) {
        probe.feed(wire.subarray(at, at + size));
        sent += probe.outgoing().length;
        if (at + size < wire.length) unpublished(probe);
      }
      assert.equal(sent, 30);
      assert.equal(probe.complete(), true);
      const report = assertSummary(probe);
      assert.equal(report.asset.containerBytes, parts[2].container.length);
      assert.equal(report.asset.expandedBytes, parts[2].container.length - 5);
      assert.equal(report.asset.files[0].bytes, parts[2].container.length - 5);
      for (const value of expected) {
        const actual = JSON.parse(probe.material(value.id));
        assert.deepEqual(actual, value);
        assert.equal(Object.keys(actual).length, 20);
      }
      assert.ok(JSON.parse(probe.material(0)).brightness < 0);
      assert.ok(JSON.parse(probe.material(0)).alpha < 0);
      assert.ok(JSON.parse(probe.material(0)).colorHsl < 0);
      probe.finish();
      probe.finish();
      probe.feed(new Uint8Array());
      assert.equal(probe.complete(), true);
    } finally { probe.free(); }
  }
});

test("material root and reference phases expose only exact sequential requests and no early report", () => {
  const probe = new Js5WorldMaterialProbe("synthetic");
  try {
    probe.outgoing();
    unpublished(probe);
    probe.feed(wire.subarray(0, 109));
    assert.equal(probe.outgoing().length, 18);
    probe.feed(parts[0].bytes);
    assert.deepEqual(Array.from(probe.outgoing()), [1, 255, 0, 0, 0, 26]);
    unpublished(probe);
    probe.feed(parts[1].bytes);
    assert.deepEqual(Array.from(probe.outgoing()), [1, 26, 0, 0, 0, 0]);
    unpublished(probe);
    probe.feed(parts[2].bytes.subarray(0, -1));
    assert.equal(probe.outgoing().length, 0);
    unpublished(probe);
    probe.feed(parts[2].bytes.subarray(-1));
    assertSummary(probe);
  } finally { probe.free(); }
});

test("material IDs reject direct JS coercion fractions wrapping and invalid numbers without losing a valid table", () => {
  const probe = loaded();
  try {
    const before = probe.report();
    const first = probe.material(0);
    let coerced = 0;
    const hostile = {
      valueOf() { coerced++; return 0; },
      toString() { coerced++; return "0"; },
      [Symbol.toPrimitive]() { coerced++; return 0; },
    };
    const invalid = [undefined, null, "0", "65535", false, true, {}, [], [0], new Number(0), new Uint8Array([0]), () => 0, Symbol("0"), 0n, NaN, Infinity, -Infinity, -1, -0.5, 0.5, 65535.5, 65536, 2 ** 32, 2 ** 32 + 1, Number.MAX_SAFE_INTEGER, hostile];
    for (const id of invalid) {
      assert.throws(() => probe.material(id));
      assert.equal(probe.complete(), true);
      assert.equal(probe.report(), before);
      assert.equal(probe.material(0), first);
    }
    assert.equal(coerced, 0);
    assert.throws(() => probe.material(1), /not found/);
    assert.throws(() => probe.material(2), /not found/);
    assert.throws(() => probe.material(4), /outside table/);
    assert.throws(() => probe.material(65535), /outside table/);
    assert.equal(probe.material(-0), first);
    assert.equal(probe.report(), before);
    probe.finish();
  } finally { probe.free(); }
});

test("synthetic native ID-addressing fixture preserves IDs128 and154 without pretending they are live cache definitions", async () => {
  const bytes = await fixture("./fixtures/material-id-addressing.wire");
  const probe = loaded(bytes);
  try {
    assertSummary(probe, 155, 2);
    for (const expected of nativeMaterials("id-addressing")) assert.deepEqual(JSON.parse(probe.material(expected.id)), expected);
    for (const id of [0, 127, 129, 153]) assert.throws(() => probe.material(id), /not found/);
    assert.throws(() => probe.material(155), /outside table/);
    assert.throws(() => probe.material(65535), /outside table/);
    assert.equal(probe.complete(), true);
  } finally { probe.free(); }
});

test("material bindings preserve signed i32 parameter endpoints and honest optional archive Whirlpool status", async () => {
  for (const [name, nativeCase, value] of [
    ["material-param2-min.wire", "param2--2147483648", -(2 ** 31)],
    ["material-param2-max.wire", "param2-2147483647", 2 ** 31 - 1],
  ]) {
    const probe = loaded(await fixture(`./fixtures/${name}`));
    try {
      assertSummary(probe, 1, 1);
      assert.deepEqual(JSON.parse(probe.material(0)), nativeMaterials(nativeCase)[0]);
      assert.equal(JSON.parse(probe.material(0)).effectParam2, value);
    } finally { probe.free(); }
  }
  const probe = loaded(await fixture("./fixtures/material-without-archive-digest.wire"));
  try {
    assertSummary(probe, 4, 2, false);
    for (const expected of nativeMaterials(oracle.nativeCase)) assert.deepEqual(JSON.parse(probe.material(expected.id)), expected);
  } finally { probe.free(); }
});

test("incomplete material stream at every stage and checked truncated fixture remain unpublished and fail sticky on finish", async () => {
  for (const end of [0, 1, 108, 109, parts[0].end - 1, parts[0].end, parts[1].end, wire.length - 1]) {
    const probe = new Js5WorldMaterialProbe("synthetic");
    try {
      probe.feed(wire.subarray(0, end));
      unpublished(probe);
      assert.throws(() => probe.finish());
      terminal(probe);
    } finally { probe.free(); }
  }
  const probe = new Js5WorldMaterialProbe("synthetic");
  try {
    probe.feed(await fixture("./fixtures/material-truncated.wire"));
    unpublished(probe);
    assert.throws(() => probe.finish());
    terminal(probe);
  } finally { probe.free(); }
});

test("verified corrupt missing-file extra-file truncated-metadata and trailing-metadata fixtures clear all material state", async () => {
  for (const [name, error] of [
    ["material-corrupt.wire", /CRC/],
    ["material-missing0.wire", /only file 0/],
    ["material-extra-files.wire", /only file 0/],
    ["material-metadata-truncated.wire", /expected/],
    ["material-trailing.wire", /trailing/],
  ]) {
    const probe = new Js5WorldMaterialProbe("synthetic");
    try {
      assert.throws(() => probe.feed(readonlyFixtures.get(name)), error);
      terminal(probe);
    } finally { probe.free(); }
  }
});

test("late material bytes discard previously published data and coalesced trailing bytes never expose success", () => {
  for (const combined of [false, true]) {
    const probe = combined ? new Js5WorldMaterialProbe("synthetic") : loaded();
    try {
      if (combined) {
        assert.throws(() => probe.feed(Buffer.concat([wire, Buffer.from([0])])), /unexpected bytes/);
      } else {
        assert.ok(probe.material(0));
        assert.throws(() => probe.feed(Buffer.from([0])), /unexpected bytes/);
      }
      terminal(probe);
    } finally { probe.free(); }
  }
});

test("material root digest CRC identity and handshake failures cannot publish a table or recover", () => {
  const root = Buffer.from(parts[0].container);
  root[5 + 1 + 26 * 72 + 8] ^= 1;
  const badRoot = Buffer.concat([wire.subarray(0, 109), packReply(255, 255, root), parts[1].bytes, parts[2].bytes]);
  const badReference = Buffer.from(wire);
  badReference[parts[1].end - 1] ^= 1;
  const badArchive = Buffer.from(wire);
  badArchive[parts[2].end - 1] ^= 1;
  const badIdentity = Buffer.from(wire);
  badIdentity[parts[2].start] = 9;
  for (const [bytes, error] of [[badRoot, /Whirlpool/], [badReference, /CRC/], [badArchive, /CRC/], [badIdentity, /identity/], [Buffer.from([6]), /handshake/]]) {
    const probe = new Js5WorldMaterialProbe("synthetic");
    try {
      assert.throws(() => probe.feed(bytes), error);
      terminal(probe);
    } finally { probe.free(); }
  }
});
