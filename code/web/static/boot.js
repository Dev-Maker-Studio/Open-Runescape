const canvas = document.getElementById("screen");
const status = document.getElementById("status");

function fit() {
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
  const tick = () => {
    app.frame();
    requestAnimationFrame(tick);
  };
  tick();
}

addEventListener("resize", fit);
fit();

const gpu = backend();
if (!gpu) {
  status.textContent = "WebGL2 or WebGPU is required";
} else {
  status.textContent = `${gpu} ready, renderer module not installed`;
  load("renderer").catch((e) => { status.textContent = String(e); });
}
