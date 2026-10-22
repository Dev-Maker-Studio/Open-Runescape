import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";

const source = (await readFile(new URL("../static/bridge.js", import.meta.url), "utf8")).replace(/^import [^\n]*\n/gm, "").replace(/^export /gm, "");
const relay = "ws://127.0.0.1:8111/js5";

function fixture({ report, textureSize = 4 }) {
  const calls = { begin: 0, layers: [], shows: [], renders: [], closes: 0, disconnects: 0, meshes: [] };
  const noop = () => {};
  const app = {
    terrain_begin() { calls.begin += 1; },
    terrain_layer(...args) { calls.layers.push(args); return calls.layers.length - 1; },
    terrain_show(...args) { calls.shows.push(args); return JSON.stringify({ draws: calls.layers.length }); },
  };
  const context = vm.createContext({
    URL, URLSearchParams, AbortController, performance, setTimeout, clearTimeout, setInterval, clearInterval,
    devicePixelRatio: 1, window: {}, location: { search: "", href: "http://localhost/" },
    addEventListener: noop, connect: noop, streamStats: () => ({}),
    disconnect: () => { calls.disconnects += 1; },
    console: { log: noop, info: noop, warn: noop, error: noop, debug: noop },
    requestAnimationFrame: noop,
    openTextureSet: async () => ({
      size: () => textureSize,
      render: async (id) => { calls.renders.push(id); return new Uint8Array(textureSize * textureSize * 4).fill(7); },
      close: () => { calls.closes += 1; },
    }),
  });
  new vm.Script(source, { filename: "bridge.js" }).runInContext(context);
  const canvas = { dataset: {}, style: {}, width: 0, height: 0 };
  const terrain = {
    loweredMaterial(index) {
      calls.meshes.push(index);
      return { vertices: new Uint8Array(40 * 3).fill(index + 1), indices: new Uint32Array([0, 1, 2]) };
    },
  };
  new vm.Script("globalThis.__st = st; globalThis.__tick = tick;", { filename: "probe.js" }).runInContext(context);
  context.__st.app = app;
  context.__st.canvas = canvas;
  context.__st.terrain = terrain;
  context.__st.loweredReport = report;
  const timer = setInterval(() => context.__tick(), 5);
  return { calls, canvas, run: context.window.openrs.run, st: context.__st, stop: () => clearInterval(timer) };
}

const material = (materialId, textureScale) => ({ materialId, textureScale });

test("terrain-scene uploads every lowered material with its decoded texture and shows the player tile", async () => {
  const f = fixture({ report: { materials: [material(154, 692), material(128, 1024), material(-1, 1024)] } });
  const result = await f.run("terrain-scene", [relay]);
  f.stop();
  assert.equal(result.ok, true, result.error);
  assert.equal(result.data.layers, 3);
  assert.equal(result.data.texturedLayers, 2);
  assert.equal(result.data.renderedWorld, false);
  assert.equal(f.calls.begin, 1);
  assert.equal(f.calls.disconnects, 1);
  assert.deepEqual(f.calls.renders, [154, 128]);
  assert.deepEqual(f.calls.meshes, [0, 1, 2]);
  assert.deepEqual(f.calls.layers.map((args) => [args[0], args[1], args[4], args[5].length]), [[154, 692, 4, 64], [128, 1024, 4, 64], [-1, 1024, 0, 0]]);
  assert.equal(f.calls.layers[0][2].length, 120);
  assert.deepEqual(Array.from(f.calls.layers[0][3]), [0, 1, 2]);
  assert.deepEqual(f.calls.shows, [[51, 55, 1280, 720]]);
  assert.equal(f.calls.closes, 1);
  assert.equal(f.canvas.width, 1280);
  assert.equal(f.canvas.height, 720);
});

test("terrain-scene honours explicit tile and size arguments", async () => {
  const f = fixture({ report: { materials: [material(128, 1024)] } });
  const result = await f.run("terrain-scene", [relay, "40", "30", "640", "360"]);
  f.stop();
  assert.equal(result.ok, true, result.error);
  assert.deepEqual(f.calls.shows, [[40, 30, 640, 360]]);
  assert.equal(f.canvas.width, 640);
});

test("terrain-scene refuses a non loopback relay, a missing lowering and a missing terrain without touching the renderer", async () => {
  const f = fixture({ report: { materials: [material(128, 1024)] } });
  assert.equal((await f.run("terrain-scene", ["ws://example.com/js5"])).error, "exact loopback JS5 relay required");
  f.st.loweredReport = null;
  assert.equal((await f.run("terrain-scene", [relay])).error, "ground lowering report unavailable");
  f.st.terrain = null;
  assert.equal((await f.run("terrain-scene", [relay])).error, "ground not lowered");
  f.stop();
  assert.equal(f.calls.begin, 0);
  assert.equal(f.calls.disconnects, 0);
  assert.equal(f.calls.layers.length, 0);
});

test("terrain-scene closes the texture set when a layer upload fails", async () => {
  const f = fixture({ report: { materials: [material(128, 1024)] } });
  f.st.app.terrain_layer = () => { throw new Error("layer rejected"); };
  const result = await f.run("terrain-scene", [relay]);
  f.stop();
  assert.equal(result.ok, false);
  assert.equal(result.error, "layer rejected");
  assert.equal(f.calls.closes, 1);
  assert.equal(f.calls.shows.length, 0);
});
