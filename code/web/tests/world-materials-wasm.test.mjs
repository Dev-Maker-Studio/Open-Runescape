import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { setImmediate } from "node:timers/promises";
import init, { Js5WorldMaterialProbe } from "../static/protocol-runtime.js";
import { openWorldMaterials } from "../static/world-materials.js";

await init({ module_or_path: await readFile(new URL("../static/protocol-runtime_bg.wasm", import.meta.url)) });
const fixtures = new URL("../../client-runtime/tests/fixtures/", import.meta.url);
const oracle = JSON.parse(await readFile(new URL("material-native.json", fixtures)));
const names = ["detailsOnly", "isSmall", "skipTriangles", "brightness", "alpha", "effectId", "effectParam1", "colorHsl", "textureSpeedU", "textureSpeedV", "aBool2087", "isBrickTile", "mipmapping", "repeatS", "repeatT", "hdr", "combineMode", "effectParam2", "effectCombiner"];
const bools = new Set([0, 1, 2, 10, 11, 13, 14, 15]);
const native = (row) => Object.fromEntries([["id", row.id], ...names.map((name, at) => [name, bools.has(at) ? row.values[at] === 1 : row.values[at]])]);
const unavailable = { message: "verified world material metadata unavailable" };

function transport() {
  const state = { freed: 0, created: 0, closes: 0, sent: [] };
  class Probe extends Js5WorldMaterialProbe {
    constructor(token) { super(token); state.created++; }
    free() { state.freed++; super.free(); }
  }
  class Socket {
    constructor(url) { state.url = url; state.socket = this; }
    send(bytes) { state.sent.push([...bytes]); }
    close() { state.closes++; }
  }
  return { state, options: { js5Relay: "ws://127.0.0.1:8110/js5", load: async () => Probe, WebSocketClass: Socket, timeoutMs: 1000, lifetimeMs: 2000 } };
}

async function feed(f, wire, size) {
  await setImmediate();
  assert.ok(f.state.socket);
  const { onopen, onmessage } = f.state.socket;
  onopen();
  assert.equal(f.state.sent[0][0], 15);
  for (let at = 0; at < wire.length; at += size) {
    const data = Uint8Array.from(wire.subarray(at, at + size));
    onmessage({ data: data.buffer });
    assert.ok(data.every((value) => value === 0));
  }
}

function cleaned(f) {
  assert.equal(f.state.created, 1);
  assert.equal(f.state.freed, 1);
  assert.equal(f.state.closes, 1);
  for (const name of ["onopen", "onmessage", "onerror", "onclose"]) assert.equal(f.state.socket[name], null);
}

test("browser material owner retains actual served WASM and exact native signed records across fragmented synthetic JS5", async () => {
  const wire = await readFile(new URL("material-native.wire", fixtures));
  for (const size of [1, 7, 109, 512, wire.length]) {
    const f = transport();
    const pending = openWorldMaterials(f.options);
    await feed(f, wire, size);
    const owner = await pending;
    try {
      assert.ok(Object.isFrozen(owner));
      assert.deepEqual(Object.keys(owner).sort(), ["close", "material", "state"]);
      assert.equal(f.state.freed, 0);
      assert.equal(f.state.closes, 1);
      const snapshot = owner.state();
      assert.equal(snapshot.receivedBytes, wire.length);
      assert.equal(snapshot.scope, "material-metadata-only");
      assert.equal(snapshot.report.slots, 4);
      assert.equal(snapshot.report.present, 2);
      assert.equal(snapshot.report.asset.files[0].bytes, 2 + 4 + 23 * 2);
      assert.equal(snapshot.authenticated, false);
      assert.equal(snapshot.texturePixelsDecoded, false);
      assert.equal(snapshot.renderedWorld, false);
      for (const row of oracle.materialFields) {
        const value = owner.material(row.id);
        assert.deepEqual(value, native(row));
        assert.ok(Object.isFrozen(value));
        assert.equal(Object.keys(value).length, 20);
      }
      for (const id of [1, 2, 4, 128, 154, 65535]) assert.equal(owner.material(id), null);
      for (const id of ["0", 0n, true, NaN, 0.5, -1, 65536, { valueOf() { assert.fail("must not coerce"); } }]) assert.throws(() => owner.material(id), { message: "invalid world material id" });
      assert.deepEqual(owner.material(0), native(oracle.materialFields[0]));
    } finally { owner.close(); owner.close(); }
    cleaned(f);
    assert.equal(owner.state().closed, true);
    assert.throws(() => owner.material(0), { message: "world material metadata owner is closed" });
  }
});

test("browser owner preserves actual signed i32 endpoints and material IDs128/154 without pixel claims", async () => {
  for (const [name, param2] of [["material-param2-min.wire", -2147483648], ["material-param2-max.wire", 2147483647], ["material-id-addressing.wire", null], ["material-without-archive-digest.wire", null]]) {
    const wire = await readFile(new URL(name, fixtures));
    const f = transport();
    const pending = openWorldMaterials(f.options);
    await feed(f, wire, 13);
    const owner = await pending;
    try {
      if (param2 !== null) assert.equal(owner.material(0).effectParam2, param2);
      if (name === "material-id-addressing.wire") {
        assert.equal(owner.state().report.slots, 155);
        assert.equal(owner.material(128).id, 128);
        assert.equal(owner.material(154).id, 154);
        assert.equal(owner.material(127), null);
        assert.equal(owner.material(155), null);
      }
      assert.equal(owner.state().report.asset.archiveWhirlpoolVerified, name !== "material-without-archive-digest.wire");
      assert.equal(owner.state().texturePixelsDecoded, false);
      assert.equal(owner.state().renderedWorld, false);
      assert.equal("file" in owner, false);
      assert.equal("probe" in owner, false);
      assert.equal("getPixels" in owner, false);
    } finally { owner.close(); }
    cleaned(f);
  }
});

test("actual served WASM corruption and invalid metadata never publish a retained browser owner", async () => {
  for (const name of ["material-corrupt.wire", "material-missing0.wire", "material-extra-files.wire", "material-metadata-truncated.wire", "material-trailing.wire", "material-truncated.wire"]) {
    const wire = await readFile(new URL(name, fixtures));
    const f = transport();
    const pending = openWorldMaterials(f.options);
    const rejected = assert.rejects(pending, unavailable);
    await feed(f, wire, wire.length);
    f.state.socket.onclose?.({ code: 1000 });
    await rejected;
    cleaned(f);
  }
});
