import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { gunzipSync } from "node:zlib";
import { createHash } from "node:crypto";
import init, { Js5TextureSet } from "../static/protocol-runtime.js";
import { bgraFromRgba, openTextureSet } from "../static/world-textures.js";

await init({ module_or_path: await readFile(new URL("../static/protocol-runtime_bg.wasm", import.meta.url)) });
const assets = new URL("../../assets/tests/fixtures/", import.meta.url);
const table = new Uint8Array(await readFile(new URL("materials.payload", assets)));
const raw = gunzipSync(await readFile(new URL("textures.bin.gz", assets)));
const view = new DataView(raw.buffer, raw.byteOffset, raw.byteLength);
let at = 0;
const u32 = () => { const value = view.getUint32(at); at += 4; return value; };
const take = (length) => { const out = new Uint8Array(raw.buffer, raw.byteOffset + at, length); at += length; return out; };
const sprites = new Map();
for (let count = u32(); count > 0; count -= 1) { const id = u32(); sprites.set(id, take(u32())); }
const materials = [];
for (let count = u32(); count > 0; count -= 1) {
  const id = u32();
  const flags = raw[at]; at += 3;
  const texture = take(u32());
  materials.push({ id, flags, texture, hash: Buffer.from(take(32)).toString("hex") });
}

const probe = (requests) => async (_relay, _token, index, archive) => {
  requests.push([index, archive]);
  if (index === 26) return { files: new Map([[0, table]]) };
  if (index === 9) return { files: new Map([[0, materials.find((m) => m.id === archive).texture]]) };
  if (index === 8) return { files: new Map([[0, sprites.get(archive)]]) };
  throw new Error("unexpected index");
};

test("swizzle turns RGBA into little-endian ARGB byte order", () => {
  assert.deepEqual([...bgraFromRgba(Uint8Array.of(1, 2, 3, 4))], [3, 2, 1, 4]);
  assert.throws(() => bgraFromRgba(Uint8Array.of(1, 2, 3)));
});

test("browser wasm renders sampled materials to the native oracle digests", async () => {
  const requests = [];
  const set = await openTextureSet("ws://127.0.0.1:1/js5", { token: "t", probe: probe(requests), load: async () => Js5TextureSet });
  const cached = materials.filter((m) => m.flags & 33 && m.flags & 1);
  assert.ok(cached.length > 2000);
  const sample = cached.filter((_, i) => i % 97 === 0).slice(0, 24);
  assert.ok(sample.length >= 20);
  const results = await set.digests(sample.map((m) => m.id));
  for (const [i, result] of results.entries()) {
    assert.equal(result.sha256, sample[i].hash, `material ${sample[i].id}`);
    assert.equal(result.bytes, result.size * result.size * 4);
  }
  assert.ok(requests.some(([index]) => index === 9));
  set.close();
});

test("a digest computed by node crypto equals the oracle for the same pixels", async () => {
  const set = await openTextureSet("ws://127.0.0.1:1/js5", { token: "t", probe: probe([]), load: async () => Js5TextureSet });
  const entry = materials.find((m) => m.flags & 33 && m.flags & 1);
  const rgba = await set.render(entry.id);
  assert.equal(createHash("sha256").update(bgraFromRgba(rgba)).digest("hex"), entry.hash);
  set.close();
});
