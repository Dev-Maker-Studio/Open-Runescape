import test from "node:test";
import assert from "node:assert/strict";
import { createWorldDriver } from "../static/world-driver.js";
import { createDefinitionCache, createSceneService, heightGrid, hoverTargets, localTile, parseMeta, partitionDraw, parseNpcs, sceneMaterials } from "../static/world-scene.js";

const BASE = { x: 3200, y: 3200 };

test("height grid indexes plane then x then y and marks gaps", () => {
  const grid = heightGrid((p, x, y) => (p === 1 && x === 2 ? null : p * 1000 + x * 10 + y), 3, 2);
  assert.equal(grid.length, 2 * 16);
  assert.equal(grid[(0 * 4 + 1) * 4 + 2], 12);
  assert.equal(grid[(1 * 4 + 3) * 4 + 1], 1031);
  assert.equal(grid[(1 * 4 + 2) * 4 + 0], -2147483648);
});

test("meta and npc lists parse flat integer arrays", () => {
  const meta = parseMeta(Int32Array.from([0, 5, 0, 3205, 3206, 12, 2, 7, 0, 3204, 3205, 20]));
  assert.deepEqual(meta[1], { kind: 2, id: 7, plane: 0, x: 3204, y: 3205, triangles: 20 });
  assert.deepEqual(localTile(meta, 7, BASE), { x: 4, y: 5, plane: 0 });
  assert.equal(localTile(meta, 8, BASE), null);
  const npcs = parseNpcs(Int32Array.from([3, 130, 3204, 3205, 0]));
  assert.equal(npcs.get(3).typeId, 130);
  assert.deepEqual([...sceneMaterials(Int16Array.from([-1, 4, 4, 9]))].sort(), [4, 9]);
});

test("hover targets resolve definitions per entity and report pending ones", () => {
  const meta = [
    { kind: 0, id: 11, plane: 0, x: BASE.x + 4, y: BASE.y + 5, triangles: 1 },
    { kind: 1, id: 995, plane: 0, x: BASE.x + 4, y: BASE.y + 5, triangles: 1 },
    { kind: 3, id: 3, plane: 0, x: BASE.x + 4, y: BASE.y + 5, triangles: 1 },
    { kind: 2, id: 9, plane: 1, x: BASE.x + 4, y: BASE.y + 5, triangles: 1 },
    { kind: 0, id: 12, plane: 0, x: BASE.x + 8, y: BASE.y + 8, triangles: 1 },
  ];
  const npcs = new Map([[3, { index: 3, typeId: 130, x: BASE.x + 4, y: BASE.y + 5, plane: 0 }]]);
  const defs = {
    "0:11": { name: "Tree", actions: ["Chop down", null, null, null, null], primaryCursorIndex: 0, primaryCursor: 3 },
    "1:995": { name: "Coins", actions: [null, null, "Take", null, null] },
    "2:130": { name: "Guard", combatLevel: 21, visible: true, gameType: 0, actions: ["Talk-to", null, "Attack", null, null] },
  };
  const result = hoverTargets({ tile: { x: 4, y: 5 }, base: BASE, plane: 0, meta, npcs, ownIndex: 1, definition: (k, id) => defs[`${k}:${id}`] });
  assert.deepEqual(result.targets.map((t) => t.kind), ["tile", "loc", "npc", "player", "ground"]);
  const byKind = Object.fromEntries(result.targets.map((t) => [t.kind, t]));
  assert.equal(byKind.player.differentPlane, true);
  assert.equal(byKind.npc.name, "Guard");
  assert.equal(byKind.npc.actions[2], "Attack");
  assert.equal(byKind.loc.name, "Tree");
  assert.equal(byKind.loc.startX, 4);
  assert.equal(byKind.loc.primaryCursor, 3);
  assert.equal(byKind.ground.items[0].name, "Coins");
  assert.equal(byKind.ground.samePlane, true);
  assert.deepEqual(result.pending, []);
  const waiting = hoverTargets({ tile: { x: 4, y: 5 }, base: BASE, plane: 0, meta, npcs, ownIndex: 1, definition: () => undefined });
  assert.deepEqual(waiting.pending.map((p) => p[0]).sort(), [0, 1, 2]);
  assert.deepEqual(waiting.targets.map((t) => t.kind), ["tile", "player"]);
});

test("definition cache keeps ready values and retries pending ones", () => {
  let answers = ["", "", '{"name":"Rock"}'];
  let calls = 0;
  const cache = createDefinitionCache((use) => use({ sceneDefinition: () => { calls += 1; return answers.shift() ?? "null"; } }));
  assert.equal(cache.get(0, 1), undefined);
  assert.equal(cache.get(0, 1), undefined);
  assert.deepEqual(cache.get(0, 1), { name: "Rock" });
  assert.deepEqual(cache.get(0, 1), { name: "Rock" });
  assert.equal(calls, 3);
});

function fakeWorld(needRounds) {
  const log = [];
  const world = {
    sceneNeeds: () => Uint32Array.from(needRounds.shift() ?? []),
    sceneAddFile: (index, archive, file, bytes) => log.push(["file", index, archive, file, bytes.length]),
    sceneFinishGroup: (index, archive) => { log.push(["finish", index, archive]); return '{"retried":2}'; },
    sceneAbsent: (index, archive) => { log.push(["absent", index, archive]); return '{"retried":1}'; },
  };
  return { world, log };
}

test("scene service fetches needs, adds files, finishes groups and marks failures absent", async () => {
  const { world, log } = fakeWorld([[7, 3, 16, 1, 18, 2], [7, 9], []]);
  const fetched = [];
  const service = createSceneService({
    lend: (use) => use(world),
    relay: "ws://r/js5",
    token: "t",
    fetchArchive: async (relay, token, index, archive) => {
      fetched.push([relay, token, index, archive]);
      if (index === 18) throw new Error("down");
      if (index === 16) return { report: {}, files: new Map() };
      return { report: {}, files: new Map([[0, new Uint8Array(4)], [5, new Uint8Array(2)]]) };
    },
  });
  await service.pump();
  const stats = service.stats();
  assert.equal(fetched.filter((f) => f[2] === 18).length, 2);
  assert.deepEqual(log.filter((l) => l[0] === "absent"), [["absent", 16, 1], ["absent", 18, 2]]);
  assert.deepEqual(log.filter((l) => l[0] === "file"), [["file", 7, 3, 0, 4], ["file", 7, 3, 5, 2], ["file", 7, 9, 0, 4], ["file", 7, 9, 5, 2]]);
  assert.equal(stats.archives, 4);
  assert.equal(stats.retried, 2 + 1 + 1 + 2);
  assert.equal(stats.failures, 1);
  assert.equal(fetched[0][1], "t");
});

test("scene service stops after close and never overlaps pumps", async () => {
  const { world, log } = fakeWorld([[7, 3], [7, 3], []]);
  let release;
  const gate = new Promise((resolve) => { release = resolve; });
  const service = createSceneService({ lend: (use) => use(world), fetchArchive: async () => { await gate; return { files: new Map([[0, new Uint8Array(1)]]) }; } });
  const first = service.pump();
  assert.equal(service.busy(), true);
  assert.equal(await service.pump(), 0);
  service.close();
  release();
  await first;
  assert.deepEqual(log, []);
});

function sceneFixture({ needs = [[2, 5], []] } = {}) {
  const calls = [];
  const target = { addEventListener() {}, removeEventListener() {} };
  const app = {
    terrain_begin: () => calls.push(["begin"]),
    terrain_layer: () => calls.push(["layer"]),
    terrain_scene: (positions, hsl, alphas, materials, rgb) => { calls.push(["scene", positions.length, hsl.length, alphas.length, materials.length, rgb.length]); return 2; },
    terrain_scene_has_texture: () => false,
    terrain_scene_texture: (...args) => calls.push(["texture", args[0], args[1], args[2], args[3].length]),
    terrain_show_live: (...args) => calls.push(["live", args[0], args[1]]),
  };
  const inputs = [];
  class ClientInput {
    constructor() { this.log = []; inputs.push(this); }
    setCamera() {}
    enableMenu(w, h) { this.log.push(["enableMenu", w, h]); }
    setMenuContext(...args) { this.log.push(["context", ...args]); }
    setMenuState(json) { this.log.push(["state", JSON.parse(json).me.index]); }
    setHover(json) { this.log.push(["hover", JSON.parse(json).map((t) => t.kind)]); }
    cycle() {}
    free() {}
  }
  class HslRgbLookup { rgbTable() { return new Uint32Array(65536); } free() {} }
  const wasm = {
    heights: null,
    sceneHeights: (tiles, planes, grid) => { wasm.heights = [tiles, planes, grid.length]; },
    sceneNeeds: () => Uint32Array.from(needs.shift() ?? []),
    sceneAddFile: (...args) => calls.push(["add", args[0], args[1], args[2]]),
    sceneFinishGroup: () => '{"retried":1}',
    sceneAbsent: () => '{"retried":0}',
    sceneDraw: () => JSON.stringify({ meshes: 3, triangles: 2, incomplete: false, pending: 1 }),
    advanceOverlay: (n) => calls.push(["overlay", n]),
    advanceScene: (n) => calls.push(["cycles", n]),
    overlay: () => "{}",
    chatLines: () => "[]",
    sceneDrawMeta: () => Int32Array.from([2, 77, 0, 3210, 3212, 1, 0, 11, 0, 3210, 3212, 1]),
    sceneDrawPositions: () => new Int32Array(18),
    sceneDrawColors: () => new Int16Array(2),
    sceneDrawAlphas: () => new Uint8Array(2),
    sceneDrawMaterials: () => Int16Array.from([-1, 6]),
    sceneNpcs: () => new Int32Array(0),
    sceneDefinition: (kind, id) => (kind === 0 && id === 11 ? '{"name":"Tree","actions":["Chop down"]}' : ""),
  };
  const session = {
    initialWorld: () => ({ base: { x: 3200, y: 3200 }, mapSize: 104, regionIds: [], playerIndex: 77 }),
    sceneTile: () => "{}",
    sceneHeight: (p, x, y) => (x > 100 ? null : 5),
    prepareGround: () => {},
    bakeShadows: () => ({ placements: 0 }),
    lowerGround: () => ({ materials: [{ materialId: -1, textureScale: 1 }] }),
    loweredMaterial: () => ({ vertices: new Uint8Array(1), indices: new Uint32Array(3) }),
    cycleInput: (input, now, rendering) => input.cycle(session, now, rendering),
    lendScene: (use) => use(wasm),
  };
  const textures = { size: () => 64, render: async () => new Uint8Array(8), close() {} };
  const options = {
    app, canvas: { width: 640, height: 480 }, session, runtime: { ClientInput, HslRgbLookup },
    openTextureSet: async () => textures, relay: "ws://r/js5", target, token: "tok",
    fetchArchive: async (relay, token, index, archive) => ({ report: {}, files: new Map([[1, new Uint8Array(3)]]) }),
    sceneIntervalMs: 0,
  };
  return { calls, inputs, options, wasm };
}

test("driver with a scene sets heights, serves needs, appends meshes and tiles from the local player", async () => {
  const f = sceneFixture();
  const driver = createWorldDriver(f.options);
  const summary = await driver.start();
  assert.deepEqual(f.wasm.heights, [104, 4, 4 * 105 * 105]);
  assert.equal(summary.scene.enabled, true);
  assert.deepEqual(f.inputs[0].log.slice(0, 2), [["enableMenu", 640, 480], ["state", 77]]);
  driver.frame(1000);
  await new Promise((resolve) => setTimeout(resolve, 20));
  driver.frame(2000);
  const names = f.calls.map((c) => c[0]);
  assert.ok(names.includes("add"));
  assert.ok(names.indexOf("begin") < names.indexOf("scene"));
  assert.ok(names.indexOf("scene") < names.indexOf("live"));
  const scene = f.calls.find((c) => c[0] === "scene");
  assert.deepEqual(scene.slice(1), [18, 2, 2, 2, 65536]);
  assert.deepEqual(f.calls.find((c) => c[0] === "texture"), ["texture", 6, 64, 64, 8]);
  const live = f.calls.filter((c) => c[0] === "live").at(-1);
  assert.deepEqual(live.slice(1), [10, 12]);
  assert.equal(driver.summary().stats.sceneLayers, 2);
});

test("driver feeds hover targets with resolved definitions for the hovered tile", async () => {
  const f = sceneFixture();
  const driver = createWorldDriver(f.options);
  await driver.start();
  driver.setHoverTile(10, 12);
  driver.frame(1000);
  driver.frame(2000);
  const hovers = f.inputs[0].log.filter((l) => l[0] === "hover");
  assert.deepEqual(hovers.at(-1)[1], ["tile", "player", "loc"]);
  const contexts = f.inputs[0].log.filter((l) => l[0] === "context");
  assert.deepEqual(contexts[0].slice(1), [false, 3200, 3200, 77, -1, -1, -1]);
  const unhooked = driver.on("hoverTile", () => ({ x: 10, y: 12 }));
  unhooked();
  driver.setHoverTile(null);
  driver.frame(3000);
  assert.deepEqual(f.inputs[0].log.filter((l) => l[0] === "hover").at(-1)[1], []);
});

test("a loc on the hovered tile appears once its definition is served", async () => {
  const f = sceneFixture();
  f.wasm.sceneDrawMeta = () => Int32Array.from([2, 77, 0, 3210, 3212, 1, 0, 11, 0, 3210, 3212, 1]);
  const driver = createWorldDriver(f.options);
  await driver.start();
  driver.setHoverTile(10, 12);
  driver.frame(1000);
  const hovers = f.inputs[0].log.filter((l) => l[0] === "hover");
  assert.deepEqual(hovers.at(-1)[1], ["tile", "player", "loc"]);
});

test("without scene support the driver behaves as before", async () => {
  const f = sceneFixture();
  delete f.options.session.lendScene;
  const driver = createWorldDriver(f.options);
  const summary = await driver.start();
  assert.equal(summary.scene.enabled, false);
  driver.frame(1000);
  assert.equal(f.calls.some((c) => c[0] === "scene"), false);
});

test("the driver camera follows the local player in scene tiles, not in absolute world coordinates", async () => {
  const f = sceneFixture();
  f.wasm.sceneDrawMeta = () => Int32Array.from([2, 77, 0, 3251, 3255, 1]);
  const driver = createWorldDriver(f.options);
  await driver.start();
  driver.frame(1000);
  driver.frame(2000);
  const live = f.calls.filter((c) => c[0] === "live").at(-1);
  assert.deepEqual(live.slice(1, 3), [51, 55]);
  assert.deepEqual(driver.summary().view.tile, { x: 51, y: 55 });
});

test("a player outside the scene never moves the camera off the terrain", async () => {
  const f = sceneFixture();
  f.wasm.sceneDrawMeta = () => Int32Array.from([2, 77, 0, 9000, -40, 1]);
  const driver = createWorldDriver(f.options);
  await driver.start();
  driver.frame(1000);
  driver.frame(2000);
  const live = f.calls.filter((c) => c[0] === "live").at(-1);
  assert.deepEqual(live.slice(1, 3), [103, 0]);
});

test("partitionDraw separates hint meshes so the page draws them without depth", () => {
  const meta = [
    { kind: 0, triangles: 1 },
    { kind: 6, triangles: 2 },
    { kind: 3, triangles: 1 },
    { kind: 7, triangles: 1 },
  ];
  const data = {
    positions: Int32Array.from({ length: 45 }, (_, i) => i),
    colors: Int16Array.from([10, 11, 12, 13, 14]),
    alphas: Uint8Array.from([0, 1, 2, 3, 4]),
    materials: Int16Array.from([-1, -1, -1, -1, -1]),
  };
  const { main, overlay } = partitionDraw(meta, data);
  assert.deepEqual([...main.colors], [10, 13]);
  assert.deepEqual([...overlay.colors], [11, 12, 14]);
  assert.deepEqual([...overlay.alphas], [1, 2, 4]);
  assert.equal(main.positions.length, 18);
  assert.equal(overlay.positions[0], 9);
});

test("the scene clock runs in 20 ms cycles and a stall is capped at ten catch-up cycles", async () => {
  const f = sceneFixture();
  const driver = createWorldDriver(f.options);
  await driver.start();
  driver.frame(1000);
  driver.frame(1100);
  driver.frame(5000);
  await new Promise((resolve) => setTimeout(resolve, 20));
  driver.frame(5400);
  const owed = f.calls.filter((c) => c[0] === "cycles").map((c) => c[1]);
  assert.ok(owed.length > 0);
  assert.ok(owed.every((n) => n >= 1 && n <= 10));
});

test("entity hits from the model projection replace the tile match for players and npcs", () => {
  const meta = [
    { kind: 2, id: 5, plane: 0, x: 3300, y: 3300, triangles: 1 },
    { kind: 2, id: 6, plane: 0, x: 3210, y: 3212, triangles: 1 },
  ];
  const common = { tile: { x: 10, y: 12 }, base: BASE, plane: 0, meta, npcs: new Map(), ownIndex: 1, definition: () => null };
  const byTile = hoverTargets(common).targets.filter((t) => t.kind === "player").map((t) => t.index);
  assert.deepEqual(byTile, [6]);
  const byHits = hoverTargets({ ...common, entityHits: new Set(["2:5"]) }).targets.filter((t) => t.kind === "player").map((t) => t.index);
  assert.deepEqual(byHits, [5]);
});

test("hintScreen stacks overhead labels with the page text widths and offsets them into the game rect", async () => {
  const f = sceneFixture();
  f.options.app.terrain_live_view_projection = () => new Float32Array(16);
  f.wasm.hintScreen = () => JSON.stringify({
    overhead: [{ index: 1, text: "hello", colour: 16776960, effect: 0, progress: 0, x: 100, y: 200 }, { index: 2, text: "world", colour: 65535, effect: 0, progress: 0, x: 104, y: 200 }],
    arrows: [{ slot: 0, sprite: 1, x: 10, y: 20 }],
  });
  const seen = [];
  f.options.runtime.stackOverheadLabels = (flat, padding) => { seen.push([...flat], padding); return Int32Array.from([200, 184]); };
  f.options.measureText = (text) => text.length * 10;
  f.options.overheadPadding = 16;
  const driver = createWorldDriver(f.options);
  await driver.start();
  const report = driver.hintScreen();
  assert.deepEqual(seen, [[100, 200, 25, 104, 200, 25], 16]);
  assert.deepEqual(report.overhead.map((l) => l.y), [200, 184]);
  assert.deepEqual(report.arrows, [{ slot: 0, sprite: 1, x: 10, y: 20 }]);
});

test("minimapPick forwards the thirteen-value view to the native helper and returns the tile", async () => {
  const f = sceneFixture();
  const calls = [];
  f.options.runtime.Picker = { minimapPick: (view, x, y) => { calls.push([[...view], x, y]); return Int32Array.from([30, 31]); } };
  const driver = createWorldDriver(f.options);
  await driver.start();
  const view = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13];
  assert.deepEqual(driver.minimapPick(view, 40, 50), { x: 30, y: 31 });
  assert.deepEqual(calls, [[view, 40, 50]]);
});
