import { fail, install } from "./bridge.js";
import { relayUrls } from "./relay-ports.js";

const canvas = document.getElementById("screen");
const status = document.getElementById("status");

function fit() {
  if (canvas.dataset.fixed) return;
  const ratio = window.devicePixelRatio || 1;
  canvas.width = Math.floor(canvas.clientWidth * ratio);
  canvas.height = Math.floor(canvas.clientHeight * ratio);
}

function backend() {
  if (navigator.gpu) return "webgpu";
  if (canvas.getContext("webgl2")) return "webgl2";
  return null;
}

async function load(name) {
  const module = await import(`./${name}.js`);
  await module.default({ module_or_path: `./${name}_bg.wasm` });
  if (!module.App) return;
  const app = await module.App.create(canvas, new URLSearchParams(location.search).get("scene"));
  status.textContent = "";
  await install(app, canvas, gpu);
  const params = new URLSearchParams(location.search);
  if (params.get("demo") === "terrain") await terrainDemo();
  if (params.get("world") === "1") await worldSession();
}

async function worldSession() {
  try {
    const urls = relayUrls(location.search, location.hostname);
    for (const [name, args] of [["standalone-login", [urls.lobby, urls.world, urls.js5, urls.accountJs5]], ["world-drive", [urls.js5, "0"]]]) {
      status.textContent = `world: ${name}`;
      const result = await window.openrs.run(name, args);
      if (!result.ok) throw new Error(`${name}: ${result.error}`);
    }
    status.textContent = "world: driving (arrows turn the camera)";
  } catch (e) {
    status.textContent = `world failed: ${e.message ?? e}`;
  }
}

async function terrainDemo() {
  const relay = `ws://${location.hostname}:8111/js5`;
  const regions = "31620,31621,31876,31877,32132,32133";
  const steps = [
    ["floors-probe", [relay]],
    ["terrain-probe", [relay, 7904, 8456, 104, regions, "floors"]],
    ["materials-probe", [relay]],
    ["underwater-probe", [relay, 7904, 8456, 104, regions]],
    ["ground-prepare", [0]],
    ["ground-lower", ["0.730967787376657", "lit"]],
    ["terrain-scene", [relay, 51, 55, canvas.clientWidth, canvas.clientHeight]],
  ];
  try {
    for (const [name, args] of steps) {
      status.textContent = `terrain demo: ${name}`;
      const result = await window.openrs.run(name, args.map(String));
      if (!result.ok) throw new Error(`${name}: ${result.error}`);
    }
    status.textContent = "terrain demo: verifying presented frame";
    const before = window.openrs.state().frames;
    const wait = await window.openrs.run("wait-frames", ["30"]);
    if (!wait.ok) throw new Error(`verify: ${wait.error}`);
    const shown = window.openrs.state();
    if (shown.loaded !== "terrain") throw new Error(`verify: frame source is ${shown.loaded}, not terrain`);
    if (shown.frames < before + 30) throw new Error("verify: frames are not advancing");
    if (shown.lastError || shown.gpuError) throw new Error(`verify: ${shown.lastError ?? shown.gpuError}`);
    status.textContent = "terrain demo: world 22 plane 0, lit, textured (no objects, placeholder sky/fog/camera)";
  } catch (e) {
    status.textContent = `terrain demo failed: ${e.message ?? e}`;
  }
}

addEventListener("resize", fit);
fit();

const gpu = backend();
if (!gpu) {
  status.textContent = "WebGL2 or WebGPU is required";
} else {
  status.textContent = `${gpu} ready, renderer module not installed`;
  load("renderer").catch((e) => {
    status.textContent = String(e);
    fail(e);
  });
}
