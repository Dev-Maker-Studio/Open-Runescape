import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";

const source = (await readFile(new URL("../static/bridge.js", import.meta.url), "utf8")).replace(/^import [^\n]*\n/gm, "").replace(/^export /gm, "");
const relay = "ws://127.0.0.1:8110/js5";
const terrainArgs = [relay, "56", "56", "72", "0,1,256,257", "floors"];
const secret = "FAKE_PRIVATE_GROUND_FAILURE";
const plain = (value) => JSON.parse(JSON.stringify(value));
const summary = (plane) => Object.freeze({ scope: "blended-surface-geometry", plane, mapSize: 72, tileCount: 5184, vertexCount: 31104, emittedVertexCount: 31104, terrainLowered: false, texturePixelsDecoded: false, renderedWorld: false });

function resource(scope) {
  const calls = { closes: 0, closed: false, preparations: [], tiles: [] };
  let ground = null;
  let dependencies = [];
  const live = () => {
    if (calls.closed) throw new Error(secret);
    if (dependencies.some((owner) => owner.calls.closed)) ground = null;
  };
  const owner = {
    calls,
    state() { calls.onState?.(); return { accepted: !calls.closed, closed: calls.closed, scope, renderedWorld: false }; },
    close() { if (!calls.closed) calls.closes++; calls.closed = true; ground = null; },
    prepareGround(floors, materials, plane, budget) {
      live();
      calls.preparations.push([floors, materials, plane, budget]);
      calls.onPrepare?.();
      if (budget < 31104) throw new Error(secret);
      ground = summary(plane);
      dependencies = [floors, materials];
      live();
      return ground;
    },
    groundState() { live(); return ground; },
    groundTile(plane, x, y) {
      live();
      if (!ground || plane !== ground.plane || x >= 72 || y >= 72) throw new Error(secret);
      calls.tiles.push([plane, x, y]);
      calls.onTile?.();
      return Object.freeze({ x, y, shape: 12 });
    },
  };
  return owner;
}

function fixture() {
  const calls = { terrain: [], floors: [], materials: [], accounts: [], logs: [] };
  const noop = () => {};
  const context = vm.createContext({
    URL, URLSearchParams, AbortController, window: {}, location: { search: "", href: "http://localhost/" },
    addEventListener: noop, connect: noop, disconnect: noop, streamStats: () => ({}),
    console: Object.fromEntries(["log", "info", "warn", "error", "debug"].map((name) => [name, (...args) => calls.logs.push(args)])),
    openWorldFloors: async (options) => { const owner = resource("floor-definitions-only"); calls.floors.push({ options, owner }); return owner; },
    openWorldMaterials: async (options) => { const owner = resource("material-metadata-only"); calls.materials.push({ options, owner }); return owner; },
    openWorldTerrain: async (options) => { const owner = resource("surface-only"); calls.terrain.push({ options, owner }); return owner; },
    openOriginalSession: async () => {
      const owner = resource("original");
      const state = owner.state;
      const prepare = owner.prepareGround;
      const floors = resource("floor-definitions-only");
      const materials = resource("material-metadata-only");
      owner.state = () => ({ ...state(), world: { connected: !owner.calls.closed } });
      owner.prepareGround = (plane, budget) => prepare(floors, materials, plane, budget);
      calls.accounts.push(owner);
      return owner;
    },
  });
  new vm.Script(source, { filename: "bridge.js" }).runInContext(context);
  return { calls, run: context.window.openrs.run, snapshot: context.window.openrs.state };
}

async function retained(f) {
  for (const [command, args] of [["floors-probe", [relay]], ["materials-probe", [relay]], ["terrain-probe", terrainArgs]]) assert.equal((await f.run(command, args)).ok, true);
  return { floors: f.calls.floors[0].owner, materials: f.calls.materials[0].owner, terrain: f.calls.terrain[0].owner };
}

async function cleanup(f) {
  for (const command of ["terrain-close", "materials-close", "floors-close", "standalone-session-close"]) await f.run(command);
}

test("ground harness prepares one retained plane with explicit defaults and no new loaders or accounts", async () => {
  const f = fixture();
  assert.equal((await f.run("ground-state")).data, null);
  assert.equal((await f.run("ground-prepare")).error, "surface ground unavailable");
  const { terrain, floors, materials } = await retained(f);
  assert.equal((await f.run("ground-state")).data, null);
  assert.equal(terrain.calls.preparations.length, 0);
  assert.deepEqual(plain((await f.run("ground-prepare")).data), summary(0));
  assert.deepEqual(terrain.calls.preparations, [[floors, materials, 0, 1016064]]);
  assert.deepEqual(plain((await f.run("ground-state")).data), summary(0));
  assert.deepEqual(plain((await f.run("ground-tile", ["0", "71", "0"])).data), { tile: { x: 71, y: 0, shape: 12 }, terrainLowered: false, texturePixelsDecoded: false, renderedWorld: false });
  assert.ok(Object.isFrozen((await f.run("ground-tile", ["0", "0", "0"])).data.tile));
  assert.equal(f.calls.floors.length, 1);
  assert.equal(f.calls.materials.length, 1);
  assert.equal(f.calls.terrain.length, 1);
  assert.equal(f.calls.accounts.length, 0);
  assert.equal(JSON.stringify(f.snapshot()).includes("prepareTerrainGround"), false);
  assert.deepEqual(f.calls.logs, []);
  await cleanup(f);
});

test("ground harness rejects noncanonical plane budget and coordinate arguments without delegation", async () => {
  const f = fixture();
  const { terrain } = await retained(f);
  const invalid = [undefined, null, 0, false, true, 1n, Symbol(), {}, "", "-0", "-1", "+0", "00", "0.0", "1e0", "0x0", " 0", "0 ", "0\n", "NaN", "Infinity", "9007199254740992"];
  for (const value of invalid) {
    for (const args of [[value], ["0", value]]) assert.equal((await f.run("ground-prepare", args)).error, "bounded ground plane and vertex budget required");
    for (let at = 0; at < 3; at++) {
      const args = ["0", "0", "0"];
      args[at] = value;
      assert.equal((await f.run("ground-tile", args)).error, "bounded ground coordinates required");
    }
  }
  for (const args of [["4"], ["0", "1016065"], ["0", "1", "2"]]) assert.equal((await f.run("ground-prepare", args)).error, "bounded ground plane and vertex budget required");
  for (const args of [[], ["0"], ["0", "0"], ["0", "0", "0", "0"], ["4", "0", "0"], ["0", "168", "0"], ["0", "0", "168"]]) assert.equal((await f.run("ground-tile", args)).error, "bounded ground coordinates required");
  assert.equal((await f.run("ground-state", ["0"])).error, "ground state takes no arguments");
  assert.equal(terrain.calls.preparations.length, 0);
  assert.equal(terrain.calls.tiles.length, 0);
  assert.equal(terrain.calls.closed, false);
  await cleanup(f);
});

test("ground preparation budget failure preserves old plane while successful preparation replaces it", async () => {
  const f = fixture();
  await retained(f);
  assert.equal((await f.run("ground-prepare", ["1", "31104"])).ok, true);
  assert.equal((await f.run("ground-prepare", ["2", "0"])).error, "surface ground unavailable");
  assert.deepEqual(plain((await f.run("ground-state")).data), summary(1));
  assert.equal((await f.run("ground-tile", ["1", "0", "0"])).ok, true);
  assert.equal((await f.run("ground-tile", ["2", "0", "0"])).error, "surface ground tile unavailable");
  assert.equal((await f.run("ground-prepare", ["3"])).ok, true);
  assert.equal((await f.run("ground-state")).data.plane, 3);
  assert.equal((await f.run("ground-tile", ["1", "0", "0"])).ok, false);
  assert.equal((await f.run("ground-tile", ["3", "72", "0"])).error, "surface ground tile unavailable");
  await cleanup(f);
});

test("ground preparation requires the palette owner and exact shared JS5 route", async () => {
  for (const mismatch of ["materials", "floors", "palette"]) {
    const f = fixture();
    await f.run("floors-probe", [relay]);
    await f.run("materials-probe", [mismatch === "materials" ? "ws://127.0.0.1:8111/js5" : relay]);
    const args = [...terrainArgs];
    if (mismatch === "floors") args[0] = "ws://127.0.0.1:8111/js5";
    if (mismatch === "palette" || mismatch === "floors") args.pop();
    assert.equal((await f.run("terrain-probe", args)).ok, true);
    assert.equal((await f.run("ground-prepare")).error, "surface ground unavailable");
    assert.equal(f.calls.terrain[0].owner.calls.preparations.length, 0);
    await cleanup(f);
  }
});

test("closing dependencies revokes ground without closing independent terrain or account owners", async () => {
  for (const command of ["materials-close", "floors-close", "terrain-close"]) {
    const f = fixture();
    const { terrain } = await retained(f);
    await f.run("standalone-login");
    assert.equal((await f.run("ground-prepare")).ok, true);
    await f.run(command);
    assert.equal((await f.run("ground-tile", ["0", "0", "0"])).error, "surface ground tile unavailable");
    assert.equal((await f.run("ground-prepare")).error, "surface ground unavailable");
    if (command === "materials-close") {
      assert.equal(terrain.calls.closed, false);
      assert.equal((await f.run("ground-state")).data, null);
    }
    assert.equal(f.calls.accounts[0].calls.closed, false);
    await cleanup(f);
  }
});

test("ground commands keep injected method and getter errors static", async () => {
  const f = fixture();
  const { terrain } = await retained(f);
  for (const [method, command, args, error] of [
    ["prepareGround", "ground-prepare", [], "surface ground unavailable"],
    ["groundState", "ground-state", [], "surface ground unavailable"],
    ["groundTile", "ground-tile", ["0", "0", "0"], "surface ground tile unavailable"],
  ]) {
    const saved = terrain[method];
    for (const value of [null, 1, {}, () => { throw new Error(secret); }]) {
      terrain[method] = value;
      assert.equal((await f.run(command, args)).error, error);
    }
    Object.defineProperty(terrain, method, { configurable: true, get() { throw new Error(secret); } });
    assert.equal((await f.run(command, args)).error, error);
    Object.defineProperty(terrain, method, { configurable: true, writable: true, value: saved });
  }
  assert.equal(JSON.stringify(f.calls.logs).includes(secret), false);
  await cleanup(f);
});

test("optional account ground commands delegate explicitly and never allocate diagnostics", async () => {
  const f = fixture();
  assert.equal((await f.run("standalone-ground-state")).data, null);
  assert.equal((await f.run("standalone-ground-prepare")).error, "independent surface ground unavailable");
  assert.equal((await f.run("standalone-ground-tile", ["0", "0", "0"])).error, "independent surface ground tile unavailable");
  await f.run("standalone-login");
  const owner = f.calls.accounts[0];
  assert.equal(owner.calls.preparations.length, 0);
  assert.equal((await f.run("standalone-ground-prepare", ["2", "31104"])).ok, true);
  assert.equal((await f.run("standalone-ground-state")).data.plane, 2);
  assert.equal((await f.run("standalone-ground-tile", ["2", "0", "0"])).data.terrainLowered, false);
  assert.equal((await f.run("standalone-ground-prepare", ["2", "1016065"])).error, "bounded ground plane and vertex budget required");
  assert.equal((await f.run("standalone-ground-tile", ["2", "168", "0"])).error, "bounded ground coordinates required");
  assert.equal(f.calls.floors.length + f.calls.materials.length + f.calls.terrain.length, 0);
  assert.equal(f.calls.accounts.length, 1);
  for (const command of ["ground-prepare", "ground-state", "ground-tile", "standalone-ground-prepare", "standalone-ground-state", "standalone-ground-tile"]) assert.ok((await f.run("help")).data.includes(command));
  await cleanup(f);
});
