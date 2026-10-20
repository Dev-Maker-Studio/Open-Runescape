# Renderer

`code/renderer` is the Rust modern renderer, using WebGPU through `wgpu` with WGSL shaders translated by naga. It is a standalone Cargo project, outside the `code/harness` workspace. The frame input is in `src/RENDERER-FRAME-FORMAT.md`.

## Layout

| Path | Task |
| --- | --- |
| `code/renderer/src/gpu.rs` | Instance, adapter, device and queue |
| `code/renderer/src/display.rs` | Surface configuration and presenting |
| `code/renderer/src/pipelines.rs` | Bind group layouts and the pipeline cache |
| `code/renderer/src/resources.rs` | Mesh, texture and uniform buffers keyed by id, bind group cache, tessellated water meshes |
| `code/renderer/src/targets.rs` | Depth target, shadow map and the scene copy that water reads |
| `code/renderer/src/renderer.rs` | Frame loop, shadow pass, main pass, draw submission |
| `code/renderer/src/water.rs` | Screen space tessellation of HD water on the CPU |
| `code/renderer/src/frame.rs` | Frame structs, replay loader and writer |
| `code/renderer/src/pass.rs` | Pass table, vertex structs, layouts and texture binding kinds |
| `code/renderer/src/uniforms.rs` | Uniform block structs |
| `code/renderer/src/scene.rs` | Deterministic built-in scenes and frame sources |
| `code/renderer/src/math.rs` | Matrix helpers |
| `code/renderer/src/shaders.rs` | Embedded WGSL sources |
| `code/renderer/src/capture.rs` | Headless device, offscreen render and readback |
| `code/renderer/src/main.rs`, `window.rs` | Native command line and window |
| `code/renderer/src/web.rs` | Browser entry exported by wasm bindgen |
| `code/renderer/src/bin/shaderc.rs` | GLSL to WGSL translation tool |
| `code/renderer/shaders` | Generated WGSL, committed |
| `code/renderer/tests` | Device free tests and GPU tests that skip without an adapter |

## Run Native

```
cd code/renderer
cargo build --release
./target/release/renderer-app --scene all
./target/release/renderer-app --scene all --frames 3 --out frame.png
```

| Option | Meaning |
| --- | --- |
| `--scene name` | `model`, `ground`, `sprite`, `shadow`, `particle`, `water`, `material` or `all`, default `all` |
| `--frames N` | Frames to render, the last is written, in a window the program exits after N |
| `--out file.png` | Render offscreen without a window and write a PNG |
| `--size WxH` | Frame size, default 800x600 |
| `--present` | Always use a window, present `--frames` frames (default 30) and exit, for RenderDoc frame marking |
| `--replay file.orf` | Use a frame file instead of a scene |
| `--dump file.orf` | Write the scene as a frame file and exit |
| `--check-shaders` | Create every WGSL module on the device and print the validation result |

- The backend is Vulkan, `WGPU_BACKEND=vulkan` forces it.
- A RenderDoc capture launches `renderer-app` with `--out` or the window mode.

## Run Web

```
cd code/renderer
./build-web.sh
```

- The script builds the `wasm32-unknown-unknown` library and runs `wasm-bindgen --target web`, writing `renderer.js` and `renderer_bg.wasm` into `code/web/static`.
- `boot.js` calls the exported `App.create(canvas, scene)` and then `App.frame()` on every animation frame.
- The page accepts `?scene=name`.

## Passes

Every pass is a `Pass` value in `pass.rs`, numbered as in the frame format, with one vertex layout, one uniform block, one list of texture binding kinds and one default render state. Draws run in file order, except that shadow draws are collected into the shadow pass first.

| Pass | Shader | Target | Textures | Default state |
| --- | --- | --- | --- | --- |
| Model, Instanced | `model`, `instanced` | Main | 2D | Depth test and write |
| Ground | `ground` | Main | 2D ground shadow, shadow map | Depth test and write |
| Terrain | `groundTextured` | Main | Noise volume, 2D, ground shadow, texture | Depth test and write |
| Sprite | `sprite` | Main | 2D array | No depth, alpha blend |
| Shadow | `shadow` | Shadow map only, no color | 2D alpha texture | Depth test and write, front faces culled |
| Particle | `particle` | Main | 2D | Depth test, no write, alpha blend |
| HdWater | `hdWater` | Main after the scene copy | Wave volume, scene color, scene depth, foam volume, environment cube, shadow, reflection | Depth test and write, less or equal, alpha blend |
| Material | `materialCubeSpec` | Main | 2D | Depth test and write |
| Water, WaterHD, WaterLapping, WaterReflecting, WaterRippling, Waterfall | Same names | Main | 2D, volume and cube as the shader declares | Depth test and write |

```mermaid
flowchart LR
  S[Shadow draws] --> SM[Shadow map depth]
  SM --> G[Ground draws]
  M[Model ground sprite particle material draws] --> C[Color and depth]
  C --> CP[Scene copy]
  CP --> W[HdWater draws]
  W --> C
```

### Frame Loop

1. Resources are synchronized: meshes, instances, textures and uniforms of the frame are uploaded, 3D and cube views are created for textures that a draw binds as a volume or a cube, and HD water meshes are tessellated.
2. Pipelines and bind groups are created for every live draw.
3. If any shadow draw exists, or any draw binds the shadow map, a depth only pass clears the shadow map to 1 and renders the shadow draws into it with the full map as viewport.
4. The main pass clears color and depth and renders every other draw in file order.
5. Before the first HD water draw of a run, the main pass ends, color and depth are copied into the scene copy textures, and the pass resumes without clearing. Water fragments read refraction and thickness from the copy.

### Shadow Map

- The shadow map is a `Depth32Float` texture, sized from the shadow draws' viewport width, 2048 by default as `ShadowPass` in the client.
- A draw binds it by using the reserved texture id `4294967294` in the slot of `uShadowMap` or `uShadowTex`. The ground scene does this.
- Depth textures are bound as unfilterable float with a nearest sampler. The ground shader does its own PCF, so this equals the client's nearest depth texture.
- The light matrix in the shadow uniform is depth remapped like every first matrix of a block, the `uShadowMatrix` of the ground block is not, so the shader's `z * 0.5 + 0.5` equals the stored depth.

### Water Technique

The client renders HD water with `hdWaterTess.vert`, `hdWater.tesc` and `hdWater.tese` on GL 4.0, and naga has no tessellation stages. The renderer replaces the three stages by a vertex stage and a CPU tessellator with the same functions.

- `shaderc` synthesizes `hdWater.vert` from the client files: the attribute declarations of `hdWaterTess.vert`, the outputs of the evaluation shader, and the uniforms, `octave` function and `main` body of `hdWater.tese` with the barycentric interpolations replaced by the attributes and the noise fetch given an explicit level zero.
- `water.rs` subdivides every triangle of a water mesh into `N * N` triangles on a barycentric lattice, where `N` is the ceiling of the largest of the three edge levels. An edge level is the same function as `hdWater.tesc`: the screen distance of the two endpoints, projected with `uViewProjection` and the draw viewport, divided by 24 pixels and clamped to 1 to 16. Meshes whose block has `uHasWaveNoise` off are drawn as is.
- The tessellated mesh is cached per mesh and uniform and rebuilt only when the view projection or viewport changes.

Equivalence: the evaluation shader is a function of the barycentric interpolation of the three patch vertices. Linear interpolation commutes with the per vertex reads, so running the same body in a vertex stage over lattice vertices that are the barycentric interpolations of the patch corners yields the same position, edge and depth at each lattice vertex, and the same `vWorld` and `vEdge` varyings for the fragment stage. The differences are the sampling pattern only: the GPU uses fractional odd spacing with a separate level per outer edge, the renderer uses uniform spacing with one level per triangle. Adjacent triangles with different levels therefore do not share subdivision points, which the GPU tessellator stitches. The displacement fades to zero at shore edges through the edge alpha and beyond 6000 units, so the mismatch is limited to open water between tiles of different screen size.

The fragment shader is patched for WebGPU's top left origin: `gl_FragCoord.xy` is flipped to the GL convention, and every scene color, depth and reflection fetch flips its texture coordinate back, so the unprojection with `uViewport` and `uInvViewProjection` matches the client.

### Not Ported

- The planar reflection target: water samples the white texture when `uUsePlanar` is set.
- Bloom and the surface mask, fullscreen and `optimized` programs of the client.

## Shader Translation

`./shaders.sh` runs `shaderc`, which reads the GLSL files, rewrites them into the Vulkan flavored GLSL that the naga frontend accepts, parses and validates them with naga, and writes WGSL with the naga writer. Every patch asserts that its target text exists, so a client shader change that moves a patched line fails the translation instead of silently dropping the rewrite.

### Rewrites

- Loose uniforms of a vertex and fragment pair merge into one std140 block at group 0 binding 0.
- `bool` uniforms become `uint` members with a `#define` that restores the name.
- A `vec2` uniform array becomes a `vec4` array to satisfy the uniform stride rule.
- Each `sampler` becomes a texture and a sampler at group 1 bindings `2i` and `2i + 1`.
- Varyings and fragment outputs get explicit locations matched by name.
- A texture fetch in the vertex stage, in the ground shadow PCF loop, in `shadow.frag` and in `sprite.frag` uses an explicit level zero, because WGSL forbids implicit derivatives in vertex or non uniform control flow.
- `sprite.frag` replaces the eight sampler array and its switch by one layered array texture.
- `groundTextured.vert` writes a varying instead of `gl_ClipDistance` and `groundTextured.frag` discards on it.
- `hdWater.vert` is synthesized from the tessellation stages and `hdWater.frag` flips its screen coordinates, see the water technique.

### Result Per Shader

Every file translates and passes both naga validation and the device shader module check.

| Pair | Vertex | Fragment | Note |
| --- | --- | --- | --- |
| `batched` | clean | clean | No pass, the client draws its batches with `model` |
| `ground` | clean | clean | Level zero fetches |
| `groundTextured` | rewritten | rewritten | Clip distance replaced, vec2 array widened |
| `hdWater` | synthesized | rewritten | Wave displacement from `hdWater.tese`, screen flip |
| `materialCubeSpec` | clean | clean | |
| `model` | clean | clean | |
| `particle` | clean | clean | |
| `shadow` | clean | rewritten | Level zero alpha fetch, no color output |
| `sprite` | clean | rewritten | Layered array texture |
| `water` | clean | clean | |
| `waterHD` | clean | clean | |
| `waterLapping` | clean | clean | |
| `waterReflecting` | clean | clean | |
| `waterRippling` | clean | clean | |
| `waterfall` | clean | clean | |

## Tests

`cargo test` runs without a GPU. Tests that need an adapter print `skip` and pass when none is found.

| File | Needs GPU | Checks |
| --- | --- | --- |
| `tests/shaders.rs` | No | Every WGSL module parses and validates with naga, every pass has a vertex and a fragment stage, only the shadow fragment has no color output |
| `tests/layout.rs` | No | Group 1 bindings, their dimensions and sampler slots match the pass table, vertex inputs match the attribute formats and strides, uniform block sizes match `Pass::block_size`, every Rust uniform struct matches its block member by member through naga reflection, the reflected layout of every pass equals the client fixture `code/client/src/test/resources/pass-layouts.json` (written with `PASS_LAYOUTS_WRITE=1`) |
| `tests/routing.rs` | No | Every client shader name routes to one pass and back, a frame with one draw per pass replays with every shader counted as drawn, the scenes cover the new passes, the water tessellator follows screen size |
| `tests/replay.rs` | Round trip only | Frame file parse and write, the synthetic client fixture, skipped draw counting |
| `tests/passes.rs` | Yes | Every scene renders two frames and paints something |

```
cd code/renderer
cargo test --offline
cargo build --release --offline
cargo build --target wasm32-unknown-unknown --offline
```
