import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import init, { GameHandshakeProbe, Js5AssetProbe, Js5Probe, Js5ScriptGraphProbe, runCoreScript } from "../../web/static/protocol-runtime.js";

await init({ module_or_path: await readFile(new URL("../../web/static/protocol-runtime_bg.wasm", import.meta.url)) });
const fixture = (name) => readFile(new URL(`../../protocol/tests/fixtures/${name}`, import.meta.url));

test("real wasm sends the native game handshake without a login request", () => {
  const probe = new GameHandshakeProbe();
  try {
    assert.deepEqual(Array.from(probe.outgoing()), [14]);
    assert.equal(probe.outgoing().length, 0);
    assert.equal(probe.complete(), false);
    assert.throws(() => probe.report(), /not complete/);
    probe.feed(new Uint8Array([0]));
    assert.deepEqual(JSON.parse(probe.report()), { revision: 727, subrevision: 1, responseCode: 0, accepted: true, authenticated: false, scope: "original-login-handshake-only" });
    assert.equal(probe.outgoing().length, 0);
    probe.finish();
  } finally { probe.free(); }
  for (const code of [1, 3, 5, 6, 14, 255]) {
    const rejected = new GameHandshakeProbe();
    try {
      rejected.outgoing();
      assert.throws(() => rejected.feed(new Uint8Array([code])), /rejected/);
      assert.throws(() => rejected.report());
    } finally { rejected.free(); }
  }
  const invalid = new GameHandshakeProbe();
  try {
    invalid.outgoing();
    assert.throws(() => invalid.feed(new Uint8Array([0, 0])), /boundary/);
    assert.throws(() => invalid.report());
  } finally { invalid.free(); }
});

test("real wasm probe decodes independently generated Kotlin wire fixtures", async () => {
  const handshake = await fixture("handshake.wire");
  const wire = await fixture("version2.wire");
  for (const size of [1, 2, 7, 109, 512]) {
    const probe = new Js5Probe("abc");
    try {
      assert.deepEqual(Buffer.from(probe.outgoing()), handshake);
      probe.feed(new Uint8Array(109));
      assert.equal(probe.outgoing().length, 18);
      for (let at = 0; at < wire.length; at += size) probe.feed(wire.subarray(at, at + size));
      assert.equal(probe.complete(), true);
      const report = JSON.parse(probe.report());
      assert.equal(report.indices[0].crc, -1985229329);
      assert.equal(report.indices[1].revision, -1);
      assert.equal(report.signatureBytes, 65);
      assert.equal(report.authenticated, false);
      probe.finish();
    } finally { probe.free(); }
  }
});

test("real wasm fetches verified gzip assets from native-generated JS5 chain", async () => {
  const wire = await readFile(new URL("./fixtures/asset-chain.wire", import.meta.url));
  const oracle = JSON.parse(await readFile(new URL("./fixtures/asset-chain-native.json", import.meta.url), "utf8"));
  const script = await readFile(new URL("../../cs2/tests/fixtures/vm-flow.bin", import.meta.url));
  const generator = await readFile(new URL("./native/AssetChainFixtures.java", import.meta.url));
  const digest = (bytes) => createHash("sha256").update(bytes).digest("hex");
  assert.equal(digest(wire), oracle.provenance.wireSha256);
  assert.equal(digest(script), oracle.provenance.inputSha256);
  assert.equal(digest(generator), oracle.provenance.generatorSha256);
  for (const size of [1, 2, 7, 109, 511, 512, 513, wire.length]) {
    const probe = new Js5AssetProbe("test", 12, 2163);
    try {
      assert.equal(probe.outgoing()[0], 15);
      let sent = 0;
      for (let at = 0; at < wire.length; at += size) {
        probe.feed(wire.subarray(at, at + size));
        sent += probe.outgoing().length;
      }
      assert.equal(sent, 30);
      assert.equal(probe.complete(), true);
      assert.deepEqual(JSON.parse(probe.report()), oracle.report);
      assert.deepEqual(Buffer.from(probe.file(0)), script);
      assert.throws(() => probe.file(1));
      probe.finish();
    } finally { probe.free(); }
  }
  for (const corrupt of [false, true]) {
    const probe = new Js5AssetProbe("test", 12, 2163);
    try {
      if (corrupt) {
        const damaged = Uint8Array.from(wire);
        damaged[damaged.length - 1] ^= 1;
        assert.throws(() => probe.feed(damaged), /CRC/);
      } else {
        probe.feed(wire.subarray(0, wire.length - 1));
        assert.throws(() => probe.finish());
      }
      assert.equal(probe.complete(), false);
      assert.throws(() => probe.report());
      assert.throws(() => probe.file(0));
    } finally { probe.free(); }
  }
});

test("real wasm CS2 matches native Kotlin flow, string, long and arithmetic fixtures", async () => {
  for (const name of ["vm-flow", "vm-strings", "vm-longs", "vm-numeric", "vm-text"]) {
    const bytes = await readFile(new URL(`../../cs2/tests/fixtures/${name}.bin`, import.meta.url));
    const oracle = (await readFile(new URL(`../../cs2/tests/fixtures/${name}.vm.tsv`, import.meta.url), "utf8")).trim().split("\n").map((line) => line.split("\t"));
    const actual = JSON.parse(runCoreScript(bytes));
    const values = (kind) => oracle.filter((row) => row[0] === kind).map((row) => row[1]);
    const text = (value) => value === "null" ? null : Buffer.from(value.slice(4), "hex").toString("utf8");
    assert.equal(actual.scope, "pure-core-no-host");
    assert.equal(actual.instructionsExecuted, Number(values("instructions")[0]));
    assert.deepEqual(actual.ints, values("int").map(Number));
    assert.deepEqual(actual.strings, values("string").map(text));
    assert.deepEqual(actual.longs, values("long"));
    assert.deepEqual(actual.localInts, values("local-int").map(Number));
    assert.deepEqual(actual.localStrings, values("local-string").map(text));
    assert.deepEqual(actual.localLongs, values("local-long"));
    assert.deepEqual(actual.arrays, [[], [], [], [], []]);
    assert.deepEqual(oracle.filter((row) => row[0] === "array").map((row) => Number(row[2])), [0, 0, 0, 0, 0]);
  }
});

test("real wasm loads transitive native scripts and preserves calls, typed stacks, locals and arrays", async () => {
  const wire = await readFile(new URL("./fixtures/script-graph.wire", import.meta.url));
  const oracle = JSON.parse(await readFile(new URL("./fixtures/script-graph-native.json", import.meta.url), "utf8"));
  const digest = (bytes) => createHash("sha256").update(bytes).digest("hex");
  assert.equal(digest(wire), oracle.wireSha256);
  assert.equal(digest(await readFile(new URL("./native/ScriptGraphFixtures.java", import.meta.url))), oracle.generatorSha256);
  assert.equal(digest(await readFile(new URL("./native/AssetChainFixtures.java", import.meta.url))), oracle.helperSha256);
  for (const size of [1, 2, 7, 109, 511, 512, 513, wire.length]) {
    const probe = new Js5ScriptGraphProbe("test", 900001);
    try {
      probe.outgoing();
      let sent = 0;
      for (let at = 0; at < wire.length; at += size) { probe.feed(wire.subarray(at, at + size)); sent += probe.outgoing().length; }
      assert.equal(sent, 36);
      const graph = JSON.parse(probe.report());
      assert.equal(graph.authenticated, false);
      assert.equal(graph.scripts.length, 2);
      assert.deepEqual(graph.scripts[0].calls, [900002]);
      assert.deepEqual(graph.scripts[1].calls, []);
      for (let i = 0; i < 2; i++) {
        const entry = graph.scripts[i], native = oracle.scripts[i];
        assert.equal(entry.id, native.id);
        assert.equal(entry.bytes, native.bytes);
        assert.equal(entry.instructions, native.script.instructions);
        assert.equal(entry.argumentInts, native.script.argumentInts);
        assert.equal(entry.argumentStrings, native.script.argumentStrings);
        assert.equal(entry.argumentLongs, native.script.argumentLongs);
        assert.equal(digest(probe.file(entry.id)), native.sha256);
      }
      const result = JSON.parse(probe.run("[]"));
      assert.equal(result.instructionsExecuted, 37);
      assert.deepEqual(result.ints, [70, 11, 22, 81, -1, 71]);
      assert.deepEqual(result.strings, ["lower", "firstsecond", "caller"]);
      assert.deepEqual(result.longs, ["73", "74", "72"]);
      assert.deepEqual(result.localInts, [71]);
      assert.deepEqual(result.localStrings, ["caller"]);
      assert.deepEqual(result.localLongs, ["72"]);
      assert.deepEqual(result.arrays, [[], [], [], [], [-1]]);
      assert.throws(() => probe.run("[1]"), /ArgumentCount/);
      assert.throws(() => probe.run("null"), /invalid script integer arguments/);
      probe.finish();
      assert.throws(() => probe.feed(new Uint8Array([0])));
      assert.throws(() => probe.file(900001));
      assert.throws(() => probe.run("[]"));
    } finally { probe.free(); }
  }
});

test("real wasm script identities reject JavaScript coercion before repository lookup", async () => {
  const invalid = [4294967296, 4294967296 + 900001, NaN, Infinity, -Infinity, 1.5, -1, 1000001, "900001", null, undefined, true, 900001n, {}, []];
  for (const id of invalid) assert.throws(() => new Js5ScriptGraphProbe("test", id), /invalid script identity/);
  for (const id of [0, 1, 1000000]) {
    const probe = new Js5ScriptGraphProbe("test", id);
    probe.free();
  }
  const wire = await readFile(new URL("./fixtures/script-graph.wire", import.meta.url));
  const probe = new Js5ScriptGraphProbe("test", 900001);
  try {
    probe.feed(wire);
    for (const id of invalid) assert.throws(() => probe.file(id), /invalid script identity/);
    assert.deepEqual(Buffer.from(probe.file(900001)), await readFile(new URL("../../cs2/tests/fixtures/vm-root.bin", import.meta.url)));
    assert.equal(probe.complete(), true);
  } finally { probe.free(); }
});

test("real wasm probe rejects premature close and malformed reply without a report", () => {
  const probe = new Js5Probe("abc");
  try {
    probe.feed(new Uint8Array(109));
    assert.throws(() => probe.feed(new Uint8Array([255, 0, 0, 0, 255, 0, 127, 255, 255, 255])));
    assert.equal(probe.complete(), false);
    assert.throws(() => probe.report());
    assert.throws(() => probe.finish());
  } finally { probe.free(); }
});
