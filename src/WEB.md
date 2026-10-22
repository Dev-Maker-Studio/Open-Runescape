# Web

`code/web` is a standalone Cargo project, outside the `code/harness` workspace. It serves the static page that will host the transpiled modern renderer.

## Layout

| Path | Task |
| --- | --- |
| `code/web/src/main.rs` | Configuration, worker threads, graceful shutdown |
| `code/web/src/files.rs` | Path resolution, precompressed lookup, response headers |
| `code/web/src/mime.rs` | Extension to MIME type, including `.wasm` |
| `code/web/static/index.html` | Full viewport canvas |
| `code/web/static/boot.js` | WebGPU and WebGL2 check, renderer module slot |
| `code/web/static/bridge.js` | Frame loop, `window.openrs` commands for the harness, console capture |
| `code/web/static/stream.js` | WebSocket to a game host, input events with sequence numbers, frame acks, stream stats, see `FRAME-STREAM.md` |

## Run

```
cd code/web
cargo build --release
PORT=8080 ./target/release/openrs-web
```

## Configuration

| Variable | Default | Meaning |
| --- | --- | --- |
| `PORT` | `8080` | Listening port |
| `HOST` | `127.0.0.1` | Bind address |
| `STATIC_DIR` | `static` | Directory served |
| `FILES_DIR` | unset | Directory served under `/files/`, with `no-store`, used by the harness for frame files |

## Serving

- Every response carries COOP `same-origin` and COEP `require-corp` so wasm threads and shared memory can be enabled later.
- A file `name.ext.br` or `name.ext.gz` next to `name.ext` is sent with the matching `Content-Encoding` when the browser accepts it, brotli first.
- HTML is sent `no-cache`, every other file is cached for an hour.
- Paths containing `..` are rejected, under `/files/` as well.
- `SIGINT` and `SIGTERM` stop all workers and exit.

## Renderer Slot

- `boot.js` loads `renderer.js` and `renderer_bg.wasm` from `static` when they exist.
- The module default export receives the wasm URL and an object with the canvas and the detected backend.

```mermaid
flowchart LR
  B[Browser] --> S[openrs-web]
  S --> I[index.html]
  I --> J[boot.js]
  J --> G{WebGPU or WebGL2}
  G --> R[renderer wasm]
```

## Entry Point

- The `web` entry point is `code/web`, started by `openrs-web`.
- `openrs web up` starts the server on the first free port from 8091 and opens the page in Chromium on the private display, see `WEB-HARNESS.md`.
- `bridge.js` runs the frame loop and calls `App.frame()` once per animation frame, paced to 60 frames per second or `?fps=N`.
- `?game=NAME&host=HOST:PORT` connects `stream.js` to a game host and the page renders the streamed frames instead of a scene, see `FRAME-STREAM.md` and `GAMES.md`.
