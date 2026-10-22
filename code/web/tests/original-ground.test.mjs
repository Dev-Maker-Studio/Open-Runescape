import test from "node:test";
import assert from "node:assert/strict";
import { openOriginalSession } from "../static/original-session.js";

function fixture() {
  const calls = { prepare: [], tile: [], close: [], connected: true };
  const floors = { state: () => ({ accepted: true, closed: false }), close: () => calls.close.push("floors") };
  const materials = { state: () => ({ accepted: true, closed: false }), close: () => calls.close.push("materials") };
  let ground = null;
  const terrain = {
    state: () => ({ accepted: true, closed: false, scope: "surface-only", renderedWorld: false, ...(ground ? { ground } : {}) }),
    prepareGround(...args) {
      calls.prepare.push(args);
      calls.onPrepare?.();
      if (args.length > 4 || args.some((value, at) => at > 1 && (!Number.isSafeInteger(value) || value < 0 || value > (at === 2 ? 3 : 1016064)))) throw new Error("FAKE_PRIVATE_GROUND_ERROR");
      ground = Object.freeze({ scope: "blended-surface-geometry", plane: args[2] ?? 0, terrainLowered: false, renderedWorld: false });
      return ground;
    },
    groundState() { calls.onState?.(); return ground; },
    groundTile(...args) {
      calls.tile.push(args);
      calls.onTile?.();
      if (args.length !== 3 || !ground || args.some((value, at) => !Number.isSafeInteger(value) || value < 0 || value > (at ? 71 : 3)) || args[0] !== ground.plane) throw new Error("FAKE_PRIVATE_GROUND_ERROR");
      return Object.freeze({ x: args[1], y: args[2], shape: 12 });
    },
    close: () => { ground = null; calls.close.push("terrain"); },
  };
  const world = {
    ready: Promise.resolve({ initialWorld: { mapKind: "standard", base: { x: 56, y: 56 }, mapSize: 72, regionIds: [0, 1, 256, 257] } }),
    state: () => ({ connected: calls.connected }),
    close: () => { calls.connected = false; calls.close.push("world"); },
  };
  const options = {
    takeConfig: async () => JSON.stringify({ browserAccount: {} }),
    observeOptions: async () => ({ fontIndex: 32, worldId: 22 }),
    buildConfig: async () => ({ lobby: {}, world: { worldId: 22 } }),
    fetchLayout: async () => ({ varpCount: 0, definitionFiles: 0 }),
    openLobby: async () => ({ ready: Promise.resolve(), state: () => ({ connected: true, worldList: { version: 0, worlds: [{ worldNumber: 22, flags: 0 }] } }), startWorld: () => world, close: () => calls.close.push("lobby") }),
    openFloors: async () => floors,
    openMaterials: async () => materials,
    openTerrain: async (value) => { assert.equal(value.floors, floors); return terrain; },
  };
  return { calls, options, floors, materials, terrain };
}

test("production original sessions offer optional bounded ground access without allocating geometry at login", async () => {
  const f = fixture();
  const session = await openOriginalSession(f.options);
  try {
    assert.equal(f.calls.prepare.length, 0);
    assert.equal(session.groundState(), null);
    assert.equal(session.state().renderedWorld, false);
    assert.equal(session.state().terrain.scope, "surface-only");
    const report = session.prepareGround();
    assert.equal(report.terrainLowered, false);
    assert.equal(report.renderedWorld, false);
    assert.deepEqual(f.calls.prepare[0], [f.floors, f.materials]);
    assert.equal(session.groundState(), report);
    assert.ok(Object.isFrozen(report));
    assert.deepEqual(session.groundTile(0, 71, 0), { x: 71, y: 0, shape: 12 });
    const next = session.prepareGround(3, 1016064);
    assert.deepEqual(f.calls.prepare[1], [f.floors, f.materials, 3, 1016064]);
    assert.equal(next.plane, 3);
    for (const args of [[4], ["0"], [0, "1"], [0, 1016065], [0, 1, 2]]) assert.throws(() => session.prepareGround(...args), { message: "original ground preparation unavailable" });
    for (const args of [[], [3, 0], [3, 72, 0], ["3", 0, 0], [0, 0, 0], [3, 0, 0, 0]]) assert.throws(() => session.groundTile(...args), { message: "original ground tile unavailable" });
    assert.throws(() => session.groundState(0), { message: "invalid original ground query" });
    assert.equal(session.groundState(), next);
    assert.equal(JSON.stringify(session.state()).includes("FAKE_PRIVATE_GROUND_ERROR"), false);
    assert.equal("probe" in session, false);
    assert.equal("floors" in session, false);
    assert.equal("materials" in session, false);
  } finally { session.close(); }
  assert.deepEqual(f.calls.close, ["terrain", "floors", "materials", "world", "lobby"]);
  assert.equal(session.groundState(), null);
  assert.throws(() => session.prepareGround(), { message: "original ground preparation unavailable" });
  assert.throws(() => session.groundTile(0, 0, 0), { message: "original ground tile unavailable" });
  session.close();
  assert.equal(f.calls.close.length, 5);
});

test("original ground wrappers reject reentrant closure or disconnected worlds before publication", async () => {
  for (const point of ["prepare", "state", "tile"]) {
    for (const action of ["close", "disconnect"]) {
      const f = fixture();
      const session = await openOriginalSession(f.options);
      session.prepareGround();
      f.calls[`on${point[0].toUpperCase()}${point.slice(1)}`] = () => {
        if (action === "close") session.close();
        else f.calls.connected = false;
      };
      if (point === "state") assert.equal(session.groundState(), null);
      else assert.throws(() => point === "prepare" ? session.prepareGround(1) : session.groundTile(0, 0, 0), { message: point === "prepare" ? "original ground preparation unavailable" : "original ground tile unavailable" });
      assert.equal(session.groundState(), null);
      assert.throws(() => session.prepareGround(), { message: "original ground preparation unavailable" });
      assert.throws(() => session.groundTile(0, 0, 0), { message: "original ground tile unavailable" });
      session.close();
    }
  }
});

test("original ground abort guards remain active after initial session preparation", async () => {
  const f = fixture();
  const abort = new AbortController();
  const session = await openOriginalSession({ ...f.options, signal: abort.signal });
  session.prepareGround();
  abort.abort("FAKE_PRIVATE_GROUND_ERROR");
  assert.equal(session.groundState(), null);
  const prepared = f.calls.prepare.length;
  assert.throws(() => session.prepareGround(), { message: "original ground preparation unavailable" });
  assert.throws(() => session.groundTile(0, 0, 0), { message: "original ground tile unavailable" });
  assert.equal(f.calls.prepare.length, prepared);
  session.close();
});
