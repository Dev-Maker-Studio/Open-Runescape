import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import init, { HslRgbLookup } from "../../web/static/protocol-runtime.js";

await init({ module_or_path: await readFile(new URL("../../web/static/protocol-runtime_bg.wasm", import.meta.url)) });
const fixtures = new URL("../../scene/tests/fixtures/", import.meta.url);
const manifest = await readFile(new URL("rgb.tsv", fixtures), "utf8");
const fromBits = (bits) => {
  const bytes = Buffer.alloc(8);
  bytes.writeBigUInt64LE(BigInt(`0x${bits}`));
  return bytes.readDoubleLE();
};

test("served RGB WASM tables match every unchanged native oracle entry with explicit sample provenance", async () => {
  let cases = 0;
  let compared = 0;
  for (const line of manifest.split("\n").filter((line) => line.startsWith("case\t"))) {
    const [, name, , , randomBits, gammaBits, filename, sha256] = line.split("\t");
    const native = await readFile(new URL(filename, fixtures));
    assert.equal(createHash("sha256").update(native).digest("hex"), sha256);
    assert.equal(native.length, 2 * 65536 * 4);
    const unit = fromBits(randomBits);
    const lookup = new HslRgbLookup(unit);
    try {
      assert.deepEqual(JSON.parse(lookup.report()), {
        scope: "hsl-rgb-lookup", randomUnit: unit, gamma: fromBits(gammaBits), gammaBits,
        entries: 65536, renderedWorld: false,
      });
      const rgb = lookup.rgbTable();
      const unsigned = lookup.unsignedRgbTable();
      assert.ok(rgb instanceof Uint32Array && unsigned instanceof Uint32Array);
      assert.equal(rgb.length, 65536);
      assert.equal(unsigned.length, 65536);
      for (let index = 0; index < 65536; index++) {
        assert.equal(rgb[index], native.readUInt32LE(index * 4), `${name}/${index}/rgb`);
        assert.equal(unsigned[index], native.readUInt32LE((65536 + index) * 4), `${name}/${index}/swapped`);
        assert.equal(lookup.rgb(index), rgb[index]);
        assert.equal(lookup.unsignedRgb(index), unsigned[index]);
        compared += 2;
      }
      rgb.fill(0xffffffff);
      unsigned.fill(0xffffffff);
      assert.equal(lookup.rgb(0), 0);
      assert.equal(lookup.unsignedRgb(0), 0);
      assert.ok(lookup.rgb(65535) <= 0xffffff);
    } finally { lookup.free(); }
    cases++;
  }
  assert.equal(cases, 8);
  assert.equal(compared, 1048576);
});

test("RGB sample and index values reject coercion, wrapping and invalid floats without mutating lookup", () => {
  for (const value of [undefined, null, "0", false, true, [], {}, 0n, NaN, Infinity, -Infinity, -1, 1, 2 ** 32]) {
    assert.throws(() => new HslRgbLookup(value));
  }
  const lookup = new HslRgbLookup(0);
  try {
    const before = lookup.report();
    const last = lookup.rgb(65535);
    for (const value of [undefined, null, "0", false, true, [], {}, 0n, NaN, Infinity, -Infinity, -1, 0.5, 65536, 2 ** 32]) {
      assert.throws(() => lookup.rgb(value));
      assert.throws(() => lookup.unsignedRgb(value));
    }
    assert.equal(lookup.report(), before);
    assert.equal(lookup.rgb(65535), last);
    assert.equal(lookup.rgb(-0), 0);
  } finally { lookup.free(); }
});
