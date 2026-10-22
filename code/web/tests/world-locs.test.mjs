import test from "node:test";
import assert from "node:assert/strict";
import { setImmediate } from "node:timers/promises";
import { worldLocProbe } from "../static/js5-probe.js";
import { openWorldLocs } from "../static/world-locs.js";

const relay = "ws://127.0.0.1:8111/js5";

function fixture() {
  const state = { freed: 0, closed: 0, lent: [], phase: 0 };
  const session = { id: "session" };
  class Probe {
    constructor(token, owner) { state.token = token; state.owner = owner; }
    outgoing() { return new Uint8Array(state.phase ? 0 : [15]); }
    feed() { state.phase++; }
    complete() { return state.phase === 1; }
    finish() {}
    report() { return JSON.stringify({ authenticated: true, regions: [{ regionId: 10, present: true }, { regionId: 11, present: false }, { regionId: 12, present: true }], placements: 3, typeIds: 3, typesFound: 3, typesDecoded: 3, typesUndecodable: 0, modelIds: 3, modelsFound: 2 }); }
    regionLocs(id) { return new Uint8Array([id]); }
    regionTypeIds(id) { return id === 10 ? new Int32Array([1, 2]) : new Int32Array([2, 3]); }
    locType(id) { return new Uint8Array([id, id]); }
    modelIds(id) { return Int32Array.from(id === 3 ? [100, 101] : [100]); }
    locModel(id) { return id === 101 ? undefined : new Uint8Array([id]); }
    free() { state.freed++; }
  }
  class Socket {
    constructor(url) { state.socket = this; state.url = url; }
    send() {}
    close() { state.closed++; }
  }
  const lend = (use) => { state.lent.push(1); return use(session); };
  return { state, session, lend, options: { load: async () => Probe, WebSocketClass: Socket, timeoutMs: 1000 } };
}

test("worldLocProbe lends the session, collects regions, types and deduplicated models", async () => {
  const f = fixture();
  const pending = worldLocProbe(relay, f.lend, f.options);
  await setImmediate();
  f.state.socket.onopen();
  f.state.socket.onmessage({ data: new Uint8Array([1]).buffer });
  const result = await pending;
  assert.equal(f.state.owner, f.session);
  assert.equal(f.state.lent.length, 1);
  assert.deepEqual([...result.regions.keys()], [10, 12]);
  assert.deepEqual([...result.types.keys()], [1, 2, 3]);
  assert.deepEqual([...result.models.keys()], [100]);
  assert.deepEqual(result.typeModels.get(3), [100, 101]);
  assert.equal(f.state.freed, 1);
  assert.equal(f.state.closed, 1);
});

test("worldLocProbe needs a lender and a loopback relay", async () => {
  const f = fixture();
  await assert.rejects(worldLocProbe(relay, null, f.options), /lender required/);
  await assert.rejects(worldLocProbe("ws://example.com/js5", f.lend, f.options), /loopback/);
});

test("worldLocProbe frees the probe when the relay fails", async () => {
  const f = fixture();
  const pending = worldLocProbe(relay, f.lend, f.options);
  pending.catch(() => {});
  await setImmediate();
  f.state.socket.onopen();
  f.state.socket.onerror();
  await assert.rejects(pending, /relay connection failed/);
  assert.equal(f.state.freed, 1);
});

function snapshot() {
  return {
    report: { authenticated: true, regions: [{ regionId: 7, present: true, keyed: true, decrypted: true, expandedBytes: 3, placements: 4, secret: 1 }, {}], placements: 4, typeIds: 2, typesFound: 2, typesDecoded: 2, typesUndecodable: 0, modelIds: 2, modelsFound: 1 },
    regions: new Map([[7, { data: new Uint8Array(3), typeIds: new Int32Array([5]) }]]),
    types: new Map([[5, new Uint8Array(2)]]),
    models: new Map([[50, new Uint8Array(4)]]),
    typeModels: new Map([[5, [50, 51]]]),
  };
}

test("openWorldLocs serves the snapshot and clears it on close", async () => {
  const seen = [];
  const source = await openWorldLocs({ js5Relay: relay, lend: () => {}, probe: async (r, l, o) => { seen.push([r, typeof l, "signal" in o]); return snapshot(); } });
  assert.deepEqual(seen, [[relay, "function", true]]);
  assert.equal(source.state().placements, 4);
  assert.equal(source.state().regions, 2);
  assert.deepEqual(source.state().regionReports[0], { regionId: 7, present: true, keyed: true, decrypted: true, expandedBytes: 3, placements: 4 });
  assert.equal(source.locs(7).data.length, 3);
  assert.equal(source.locs(8), undefined);
  assert.equal(source.locType(5).length, 2);
  assert.deepEqual(source.locModels(5).map((m) => [m.id, m.data.length]), [[50, 4]]);
  assert.equal(source.locModel(50).length, 4);
  source.close();
  source.close();
  assert.throws(() => source.locs(7), /unavailable/);
  assert.throws(() => source.state(), /unavailable/);
});

test("openWorldLocs hides probe failure detail", async () => {
  await assert.rejects(openWorldLocs({ js5Relay: relay, lend: () => {}, probe: async () => { throw new Error("secret detail"); } }), { message: "original world locs unavailable" });
});
