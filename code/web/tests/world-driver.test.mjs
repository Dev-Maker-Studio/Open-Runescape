import test from "node:test";
import assert from "node:assert/strict";
import { createWorldDriver } from "../static/world-driver.js";

function fixture({ regionSource = null, cycleFails = false } = {}) {
  const calls = [];
  const listeners = new Map();
  const target = {
    addEventListener: (type, fn) => listeners.set(type, fn),
    removeEventListener: (type) => listeners.delete(type),
  };
  const app = {
    terrain_begin: () => calls.push(["begin"]),
    terrain_layer: (...args) => calls.push(["layer", args[0], args[1], args[4]]),
    terrain_show_live: (...args) => calls.push(["live", ...args]),
  };
  const inputs = [];
  class ClientInput {
    constructor() { this.camera = []; this.cycles = []; inputs.push(this); }
    setCamera(a, b) { this.camera.push([a, b]); }
    cycle(session, now, rendering) { this.cycles.push([session, now, rendering]); }
    free() { this.freed = true; }
  }
  const rgb = { free: () => calls.push(["rgb-free"]) };
  class HslRgbLookup { constructor(unit) { calls.push(["rgb", unit]); return rgb; } }
  const session = {
    initialWorld: () => ({ base: { x: 7904, y: 8456 }, mapSize: 104, regionIds: [31620, 31621] }),
    sceneTile: (plane, x, y) => JSON.stringify({ renderFlags: plane === 1 && x === 3 && y === 4 ? 2 : 1 }),
    sceneHeight: (plane, x, y) => plane * 1000 + x * 10 + y,
    prepareGround: (plane) => calls.push(["prepare", plane]),
    addShadowLocs: (id, data, x, y) => calls.push(["locs", id, data.length, x, y]),
    addShadowLocType: (id, data) => calls.push(["type", id, data.length]),
    addShadowLocModel: (id, data) => calls.push(["model", id, data.length]),
    bakeShadows: (textures) => { calls.push(["bake", textures]); return { placements: 1 }; },
    lowerGround: () => ({ materials: [{ materialId: -1, textureScale: 1 }, { materialId: 7, textureScale: 2 }] }),
    loweredMaterial: (i) => ({ vertices: new Uint8Array(i + 1), indices: new Uint32Array(3) }),
    cycleInput: (input, now, rendering) => {
      if (cycleFails) throw new Error("closed");
      calls.push(["cycle", now]);
      input.cycle(session, now, rendering);
    },
  };
  const textures = { size: (id) => id * 2, render: async () => new Uint8Array(4), close: () => calls.push(["textures-close"]) };
  const options = {
    app, canvas: { width: 640, height: 480 }, session, runtime: { ClientInput, HslRgbLookup },
    openTextureSet: async (relay) => { calls.push(["textures", relay]); return textures; },
    relay: "ws://127.0.0.1:8111/js5", target, regionSource,
  };
  return { calls, listeners, target, inputs, options, session };
}

test("start installs the input, bakes shadows before lowering and builds layers", async () => {
  const f = fixture();
  const driver = createWorldDriver(f.options);
  const summary = await driver.start();
  assert.equal(f.target.openrsClientInput, f.inputs[0]);
  assert.equal(summary.layers, 2);
  assert.equal(summary.shadows.placements, 1);
  const names = f.calls.map((c) => c[0]);
  assert.ok(names.indexOf("prepare") < names.indexOf("bake"));
  assert.ok(names.indexOf("bake") < names.indexOf("rgb"));
  assert.ok(names.includes("rgb-free"));
  await assert.rejects(driver.start(), { message: "world driver already started" });
});

test("shadow inputs are fed per region before the bake", async () => {
  const regionSource = {
    locs: async (id) => (id === 31620 ? { data: new Uint8Array(5), typeIds: [3, 4] } : null),
    locType: async (id) => new Uint8Array(id),
    locModel: async (id) => (id === 4 ? new Uint8Array(9) : null),
  };
  const f = fixture({ regionSource });
  const driver = createWorldDriver(f.options);
  const summary = await driver.start();
  assert.deepEqual(f.calls.filter((c) => ["locs", "type", "model"].includes(c[0])), [["locs", 31620, 5, 7904, 8456], ["type", 3, 3], ["type", 4, 4], ["model", 4, 9]]);
  assert.deepEqual([summary.stats.shadowRegions, summary.stats.shadowTypes, summary.stats.shadowModels], [1, 2, 1]);
});

test("without a region source the session loc source feeds shadows by model id and closes", async () => {
  const f = fixture();
  let closed = 0;
  const source = {
    locs: async (id) => (id === 31620 ? { data: new Uint8Array(5), typeIds: new Int32Array([3, 4]) } : id === 31621 ? { data: new Uint8Array(2), typeIds: new Int32Array([4]) } : null),
    locType: async (id) => new Uint8Array(id),
    locModels: async (id) => [{ id: 900, data: new Uint8Array(2) }, { id: 900 + id, data: new Uint8Array(id) }],
    state: () => ({ regions: 2, placements: 7, typeIds: 2 }),
    close: () => { closed++; },
  };
  f.session.locSource = async () => source;
  const summary = await createWorldDriver(f.options).start();
  assert.deepEqual(f.calls.filter((c) => c[0] === "model"), [["model", 900, 2], ["model", 903, 3], ["model", 904, 4]]);
  assert.deepEqual(f.calls.filter((c) => c[0] === "type").map((c) => c[1]), [3, 4]);
  assert.deepEqual([summary.stats.shadowRegions, summary.stats.shadowTypes, summary.stats.shadowModels], [2, 2, 3]);
  assert.equal(closed, 1);
  assert.equal(summary.shadowError, null);
  assert.deepEqual(summary.locState, { regions: 2, placements: 7, typeIds: 2 });
});

test("a failing loc source degrades without failing start", async () => {
  const f = fixture();
  f.session.locSource = async () => { throw new Error("original world locs unavailable"); };
  const driver = createWorldDriver(f.options);
  const summary = await driver.start();
  assert.equal(summary.started, true);
  assert.equal(summary.shadowError, "original world locs unavailable");
  assert.equal(summary.stats.shadowRegions, 0);
  assert.ok(f.calls.some((c) => c[0] === "bake"));
});

test("a frame cycles the input and shows the live scene with the tile, camera and environment", async () => {
  const f = fixture();
  const driver = createWorldDriver({ ...f.options, environment: { color: 0x112233, depth: 5, offset: 6 } });
  driver.on("playerTile", () => ({ x: 51.9, y: 55 }));
  await driver.start();
  f.calls.length = 0;
  assert.equal(driver.frame(1000), true);
  assert.deepEqual(f.calls.map((c) => c[0]), ["cycle", "begin", "layer", "layer", "live"]);
  const live = f.calls.at(-1);
  assert.deepEqual(live.slice(1, 8), [51, 55, 640, 480, 1960, 0, 0]);
  assert.equal(live[8], true);
  assert.deepEqual(live.slice(9, 12), [0x112233, 5, 6]);
  assert.equal(live[12], (0xff112233) | 0);
  assert.equal(f.inputs[0].cycles[0][2], true);
  f.calls.length = 0;
  assert.equal(driver.frame(1020), false);
  assert.deepEqual(f.calls.map((c) => c[0]), ["cycle"]);
});

test("arrow keys steer the camera into the input and redraw", async () => {
  const f = fixture();
  const driver = createWorldDriver(f.options);
  await driver.start();
  driver.frame(1);
  f.listeners.get("keydown")({ code: "ArrowLeft" });
  f.listeners.get("keydown")({ code: "ArrowUp" });
  f.listeners.get("keydown")({ code: "KeyA" });
  f.calls.length = 0;
  assert.equal(driver.frame(2), true);
  assert.deepEqual(f.inputs[0].camera.at(-1), [1960 - 96, 96]);
  assert.deepEqual(f.calls.at(-1).slice(5, 8), [1960 - 96, 96, 0]);
  f.listeners.get("keyup")({ code: "ArrowLeft" });
  f.listeners.get("keyup")({ code: "ArrowUp" });
  assert.equal(driver.frame(3), false);
  driver.setCamera(2000, 100);
  assert.equal(driver.frame(4), true);
  assert.throws(() => driver.setCamera(-1, 0), /14-bit/);
});

test("the compass turns about the vertical axis (roll) and never rolls the screen (pitch)", async () => {
  const f = fixture();
  const driver = createWorldDriver(f.options);
  await driver.start();
  for (const compass of [0, 96, 4096, 16000]) {
    driver.setCamera(1960, compass);
    f.calls.length = 0;
    assert.equal(driver.frame(compass + 1), true);
    const live = f.calls.at(-1);
    assert.deepEqual(live.slice(5, 8), [1960, compass, 0]);
  }
});

test("the summary reports the arguments of the last live view", async () => {
  const f = fixture();
  const driver = createWorldDriver({ ...f.options, environment: { color: 0x112233, depth: 5, offset: 6 } });
  driver.on("playerTile", () => ({ x: 51, y: 55 }));
  assert.equal(driver.summary().view, null);
  await driver.start();
  driver.setCamera(2000, 300);
  driver.frame(1);
  assert.deepEqual(driver.summary().view, { tile: { x: 51, y: 55 }, width: 640, height: 480, tilt: 2000, compass: 300, environment: { enabled: true, color: 0x112233, depth: 5, offset: 6, sky: (0xff112233) | 0 } });
});

test("hooks plug in for layers, invalidation and removal", async () => {
  const f = fixture();
  const driver = createWorldDriver(f.options);
  await driver.start();
  driver.frame(1);
  let extra = 0;
  const off = driver.on("layers", (app) => { extra += 1; app.terrain_layer(99, 1, new Uint8Array(1), new Uint32Array(1), 0, new Uint8Array(0)); });
  assert.equal(driver.frame(2), true);
  assert.equal(extra, 1);
  assert.ok(f.calls.some((c) => c[0] === "layer" && c[1] === 99));
  let pending = true;
  driver.on("invalidate", () => { const was = pending; pending = false; return was; });
  assert.equal(driver.frame(3), true);
  assert.equal(driver.frame(4), false);
  off();
  driver.invalidate();
  f.calls.length = 0;
  driver.frame(5);
  assert.ok(!f.calls.some((c) => c[0] === "layer" && c[1] === 99));
  assert.throws(() => driver.on("nope", () => {}), /known driver hook/);
});

test("a closed session stops the driver and restores the global input", async () => {
  const f = fixture({ cycleFails: true });
  f.target.openrsClientInput = "previous";
  const driver = createWorldDriver(f.options);
  await driver.start();
  assert.notEqual(f.target.openrsClientInput, "previous");
  assert.equal(driver.frame(1), false);
  const summary = driver.summary();
  assert.equal(summary.stopped, true);
  assert.equal(summary.error, "closed");
  assert.equal(f.target.openrsClientInput, "previous");
  assert.equal(f.listeners.size, 0);
  assert.equal(f.inputs[0].freed, true);
  assert.ok(f.calls.some((c) => c[0] === "textures-close"));
});

test("missing capabilities are refused up front", () => {
  const f = fixture();
  assert.throws(() => createWorldDriver({ ...f.options, session: { ...f.session, bakeShadows: undefined } }), /lacks bakeShadows/);
  assert.throws(() => createWorldDriver({ ...f.options, app: {} }), /live terrain/);
  assert.throws(() => createWorldDriver({ ...f.options, runtime: {} }), /client input/);
});

test("orbit mode feeds elapsed time, key directions, jitter and ctrl wheel zoom to the renderer", async () => {
  const f = fixture();
  const orbitCalls = [];
  f.options.app.orbit_reset = (...args) => orbitCalls.push(["reset", ...args]);
  f.options.app.orbit_zoom = (rotation) => orbitCalls.push(["zoom", rotation]);
  f.options.app.terrain_show_orbit = (...args) => orbitCalls.push(["show", ...args]);
  f.options.orbit = true;
  f.options.random = () => 0.5;
  const driver = createWorldDriver(f.options);
  await driver.start();
  assert.deepEqual(orbitCalls[0], ["reset", 0, 0, 0, 0]);
  driver.frame(1000);
  f.listeners.get("keydown")({ code: "ArrowLeft" });
  f.listeners.get("keydown")({ code: "ArrowUp" });
  driver.frame(1050);
  const show = orbitCalls.filter((c) => c[0] === "show");
  assert.deepEqual(show[0].slice(5, 6), [0]);
  assert.deepEqual(show[1].slice(5, 10), [50, 2, 1, -1, 4]);
  assert.equal(f.calls.some((c) => c[0] === "live"), false);
  f.listeners.get("wheel")({ ctrlKey: true, deltaY: 120 });
  f.listeners.get("wheel")({ ctrlKey: false, deltaY: -120 });
  assert.deepEqual(orbitCalls.filter((c) => c[0] === "zoom"), [["zoom", 1]]);
});

function orbitFixture() {
  const f = fixture();
  const orbitCalls = [];
  f.options.app.orbit_reset = (...args) => orbitCalls.push(["reset", ...args]);
  f.options.app.orbit_zoom = (rotation) => orbitCalls.push(["zoom", rotation]);
  f.options.app.orbit_ground = (...args) => orbitCalls.push(["ground", ...args]);
  f.options.app.terrain_show_orbit = (...args) => orbitCalls.push(["show", ...args]);
  f.options.random = () => 0.5;
  return { f, orbitCalls };
}

test("orbit is the default live camera and the ground grid and bridge flags are fed once", async () => {
  const { f, orbitCalls } = orbitFixture();
  f.options.plane = 2;
  const driver = createWorldDriver(f.options);
  await driver.start();
  const ground = orbitCalls.filter((c) => c[0] === "ground");
  assert.equal(ground.length, 1);
  const [, tiles, plane, heights, bridges] = ground[0];
  assert.equal(tiles, 104);
  assert.equal(plane, 2);
  assert.equal(heights.length, 4 * 105 * 105);
  assert.equal(heights[(0 * 105 + 3) * 105 + 4], 34);
  assert.equal(heights[(2 * 105 + 104) * 105 + 104], 2000 + 1040 + 104);
  assert.equal(bridges.length, 104 * 104);
  assert.equal(bridges[3 * 104 + 4], 1);
  assert.equal(bridges.reduce((a, b) => a + b, 0), 1);
  driver.frame(1000);
  driver.frame(1020);
  assert.equal(orbitCalls.filter((c) => c[0] === "ground").length, 1);
  assert.equal(f.calls.some((c) => c[0] === "live"), false);
});

test("orbit false keeps the legacy live path and feeds no ground", async () => {
  const { f, orbitCalls } = orbitFixture();
  f.options.orbit = false;
  f.options.app.terrain_show_live = (...args) => f.calls.push(["live", ...args]);
  const driver = createWorldDriver(f.options);
  await driver.start();
  driver.frame(1000);
  assert.equal(orbitCalls.length, 0);
  assert.equal(f.calls.some((c) => c[0] === "live"), true);
});

test("a ground the renderer rejects does not stop the driver", async () => {
  const { f, orbitCalls } = orbitFixture();
  f.options.app.orbit_ground = () => { throw new Error("bad grid"); };
  const messages = [];
  f.options.onStatus = (text) => messages.push(text);
  const driver = createWorldDriver(f.options);
  await driver.start();
  assert.equal(driver.frame(1000), true);
  assert.ok(messages.some((m) => m.includes("orbit ground unavailable: bad grid")));
  assert.equal(orbitCalls.filter((c) => c[0] === "show").length, 1);
});

test("middle drag maps to the arrow directions for one frame and ignores other buttons", async () => {
  const { f, orbitCalls } = orbitFixture();
  const driver = createWorldDriver(f.options);
  await driver.start();
  driver.frame(1000);
  const prevented = [];
  f.listeners.get("mousemove")({ movementX: 40, movementY: 40 });
  driver.frame(1020);
  f.listeners.get("mousedown")({ button: 0, preventDefault: () => prevented.push(0) });
  f.listeners.get("mousemove")({ movementX: 40, movementY: 40 });
  driver.frame(1040);
  f.listeners.get("mousedown")({ button: 1, preventDefault: () => prevented.push(1) });
  f.listeners.get("mousemove")({ movementX: 7, movementY: -3 });
  f.listeners.get("mousemove")({ movementX: 5, movementY: -4 });
  driver.frame(1060);
  driver.frame(1080);
  f.listeners.get("mousemove")({ movementX: -9, movementY: 9 });
  driver.frame(1100);
  f.listeners.get("mouseup")({ button: 1 });
  f.listeners.get("mousemove")({ movementX: -9, movementY: 9 });
  driver.frame(1120);
  const dirs = orbitCalls.filter((c) => c[0] === "show").map((c) => c.slice(7, 9));
  assert.deepEqual(dirs, [[0, 0], [0, 0], [0, 0], [-1, -1], [0, 0], [1, 1], [0, 0]]);
  assert.deepEqual(prevented, [1]);
});

test("drag combines with held arrows and stays within one step", async () => {
  const { f, orbitCalls } = orbitFixture();
  const driver = createWorldDriver(f.options);
  await driver.start();
  driver.frame(1000);
  f.listeners.get("keydown")({ code: "ArrowRight" });
  f.listeners.get("keydown")({ code: "ArrowDown" });
  f.listeners.get("mousedown")({ button: 1 });
  f.listeners.get("mousemove")({ movementX: 50, movementY: -50 });
  driver.frame(1020);
  f.listeners.get("keyup")({ code: "ArrowRight" });
  f.listeners.get("keyup")({ code: "ArrowDown" });
  f.listeners.get("mousemove")({ movementX: 50, movementY: -50 });
  driver.frame(1040);
  const dirs = orbitCalls.filter((c) => c[0] === "show").map((c) => c.slice(7, 9));
  assert.deepEqual(dirs.slice(1), [[-1, 0], [-1, -1]]);
});

test("driver removes the mouse listeners when it stops", async () => {
  const { f } = orbitFixture();
  const driver = createWorldDriver(f.options);
  await driver.start();
  assert.ok(["mousedown", "mousemove", "mouseup", "wheel", "keydown", "keyup"].every((t) => f.listeners.has(t)));
  driver.stop();
  assert.equal(f.listeners.size, 0);
});

test("an interface driver steps each frame, sizes the world to its game area and is stopped with the driver", async () => {
  const f = fixture();
  const steps = [];
  let stopped = 0;
  const ui = { step: (now) => { steps.push(now); return true; }, gameRect: () => ({ x: 4, y: 4, width: 512, height: 334 }), stop: () => { stopped += 1; } };
  const driver = createWorldDriver({ ...f.options, ui });
  driver.on("playerTile", () => ({ x: 51, y: 55 }));
  await driver.start();
  f.calls.length = 0;
  assert.equal(driver.frame(1000), true);
  assert.deepEqual(steps, [1000]);
  assert.deepEqual(f.calls.at(-1).slice(1, 5), [51, 55, 512, 334]);
  driver.stop();
  assert.equal(stopped, 1);
});

test("a ui step that reports a change forces a show of an otherwise clean frame", async () => {
  const f = fixture();
  let changed = false;
  const ui = { step: () => changed, gameRect: () => null, stop: () => {} };
  const driver = createWorldDriver({ ...f.options, ui });
  driver.on("playerTile", () => ({ x: 51, y: 55 }));
  await driver.start();
  assert.equal(driver.frame(1000), true);
  assert.equal(driver.frame(1040), false);
  changed = true;
  assert.equal(driver.frame(1080), true);
});
