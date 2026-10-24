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

## Desktop Browser Preview

The only active configuration in `.claude/launch.json` is `sol-fleet-web`. Historical fixture, account and relay entries were archived under `code/web/target/launch-backups/`; several referenced expired private bootstrap files and cannot be used without recreating their fixtures. It serves port 18420 using the persistent `code/web/target/release/openrs-web` binary and the verified bundle in `code/web/target/combined-final-1792784175/static`. All 36 bundle files match its publication manifest. This is the last verified bundle, not a fresh build of later runtime changes.

The preview unsets `OPENRS_DEV_LOGIN_CONFIG`, `OPENRS_DEV_ORIGIN` and `FILES_DIR`. The old temporary fleet bootstrap, shared-files directory and staged binary no longer exist after runtime cleanup; depending on them prevents startup. Private-bootstrap security validation remains unchanged. Use `?login=1` for the existing interactive login form rather than a missing fleet bootstrap. Login still requires running lobby/world services and fixed-upstream relays; starting this page does not start those services or authenticate an account.

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
