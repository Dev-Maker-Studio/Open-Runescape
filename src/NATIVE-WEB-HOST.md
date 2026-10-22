# Native Web Host

The native web host turns an already running Kotlin client into a live game host for the browser renderer. The native client owns the account, cache, lobby connection, world simulation, CS2 scripts and interfaces. The browser receives draw commands and sends canvas input back. This is hosted gameplay, not a second implementation of the game in JavaScript or wasm.

## What Runs Where

| Component | Responsibility |
| --- | --- |
| Existing Kotlin client | Login, game state, networking, CS2, input consumption and full ORF3/ORF4 recordings |
| `code/native-host` | Local control adapter, bounded spool reader, resource identity, WebSocket flow control and diagnostics |
| Existing WebGPU page | Rendering, canvas input, frame acknowledgements and round-trip measurements |

The standalone browser scenes and test games do not contain the native client's CS2 interpreter, account session or server protocol. Opening the WebGPU page alone therefore does not log in or run the real game. Connecting it to this host uses the native client's live gameplay and interface scripts without moving those systems into the browser.

Rendering still follows the Rust renderer's supported passes. A working connection is not a claim of pixel equivalence, complete shader coverage or audio streaming. The stream transfers graphics and input, not native sound.

## Start The Host

Start and log in the native client through the existing, separately managed workflow. Use an existing account. The host never creates accounts, launches the client, starts a lobby or changes server configuration.

```bash
cd code/native-host
cargo build --release
./target/release/openrs-native-host \
  --control /run/user/1000/openrs-native-dev/control.sock \
  --spool /tmp/openrs-native-spool \
  --port 8101 \
  --fps 5
```

The host binds only `127.0.0.1`. The FPS range is 1 through 30; the default is 5. The port defaults to 8101. The control endpoint is a local Unix socket, not a server login endpoint.

Open the existing WebGPU page with this query, replacing the HTTP port with the page server's port.

```text
http://127.0.0.1:8090/?game=native&host=127.0.0.1:8101
```

## Browser Harness

The additive native mode builds a stale host binary and starts only the host, page server and isolated browser. It connects to the supplied native control socket. It never calls `openrs up` or manages the native client or lobby.

```bash
export OPENRS_WEB_STATE=/tmp/openrs-native-browser
export OPENRS_STATE=/tmp/openrs-native-browser-display
openrs web up --native \
  --native-control /run/user/1000/openrs-native-dev/control.sock \
  --native-spool /tmp/openrs-native-spool \
  --native-port 8101
openrs web do stream
openrs web do state
openrs web do describe
openrs web down
```

Set both variables consistently for every command targeting this browser. Its `OPENRS_STATE` must differ from the already running native client's state directory, so the browser gets a separate private display and cannot cover the native canvas. A separate directory keeps its Chromium profile, page server, host PID and restart arguments separate from another web session. Without `--native-spool`, the spool lives in `native-spool` inside that state directory. A port conflict is an error, not permission to take over another host.

The harness waits for a native stream frame before reporting readiness. `openrs resume web` can restart this host and browser from saved state. `openrs web down` stops its own host, browser and page server, leaving the native client and game servers alone. The host handles SIGTERM and SIGINT by stopping its recording before exiting.

The ordinary `openrs web up --game walker` path is unchanged. Native mode cannot be combined with a scene or `--game`.

## Session Identity

One host represents one native client session and one controlling WebSocket connection. A second connection receives HTTP 503 with `native controller occupied`. Non-loopback browser origins are refused. This is a local development control surface, not an authenticated public gateway.

A successful connection issues `stream-start`; disconnect issues `stream-stop`. Starting a recording resets the native input sequence and releases held keys and buttons. Stopping also releases input. Reconnecting creates a fresh resource dictionary, not a second account session. Resource epoch changes are not reconnects: they do not issue `stream-start` or `stream-stop`, reset input sequences, release held buttons or change focus. Changing accounts remains a native-client operation.

A spool lock prevents two host processes from using one directory. After an abrupt host crash, a replacement host may stop an orphaned producer only when its reported recording directory matches that locked spool. A recording in a different directory is not taken over.

The ordinary native control socket remains available for `state`, `describe`, `stream-state` and other existing controls. The WebSocket adapter does not proxy arbitrary control commands.

## Producer Contract

Requests and replies are newline-delimited JSON over the native Unix control socket.

```json
{"cmd":"stream-start","args":["/tmp/openrs-native-spool","5"]}
```

The other producer commands are `stream-stop` and `stream-state`, both with an empty argument list. Replies use the existing `{ "ok": true, "data": ... }` or `{ "ok": false, "error": ... }` envelope.

The producer writes only two reusable ORF slots, `frame-0.orf` and `frame-1.orf`. Each complete slot is published by atomic replacement. It then atomically replaces `latest.json`.

```json
{
  "frame": 42,
  "slot": 0,
  "file": "frame-0.orf",
  "width": 1280,
  "height": 720,
  "tick": 1234,
  "inputSeq": 17,
  "nanos": 987654321,
  "sha256": "64 lowercase hexadecimal characters",
  "summary": "producer draw summary"
}
```

| Field | Meaning |
| --- | --- |
| `frame` | Producer frame number, unsigned 32-bit |
| `slot`, `file` | Slot 0 or 1 and its exact corresponding filename |
| `width`, `height` | Actual native render dimensions, also present in the ORF header |
| `tick` | Native game tick observed for the recording |
| `inputSeq` | Last browser input sequence consumed by the native input pulse before capture |
| `nanos` | Producer capture timestamp, used as an opaque identity value |
| `sha256` | SHA-256 of the complete ORF slot bytes |
| `summary` | Human-readable producer diagnostics; not sent as executable data |

The host reads metadata, reads the slot, verifies its digest and rereads metadata. A changed publication or mismatched digest is a race, not a valid frame. It drops that candidate and retries on the next publication. ORF3 and ORF4 are accepted; typed texture records require ORF4. Metadata and frame dimensions must agree.

## WebSocket Protocol

The host uses the existing GPU-independent `renderer::stream` codec with `game="native"`. See `FRAME-STREAM.md` for the binary layout. The hello tick value is the requested capture rate, not the native game's simulation tick rate.

1. `hello` describes the native frame size and capture rate.
2. `resource` carries newly encountered meshes and textures.
3. `frame` carries uniforms, instances, draws, producer frame number, consumed input sequence and cumulative drop count.
4. `ping` keeps the browser responsive while the send window is full or no capture arrives.

The host hashes resource content with SHA-256, including mesh layout and texture dimensions. It assigns connection-local IDs and rewrites draw references. Identical content retains its browser identity even if a later full recording changes native resource IDs. Changed content receives a distinct identity even if the producer reuses the old ID. Uniforms and instance records travel with every frame.

At most two sent frames are unacknowledged. The window tracks the actual sent frame numbers, so a large producer-number gap cannot wedge it. While blocked, the host keeps only the newest metadata candidate. Replaced candidates and skipped producer frame numbers contribute to the drop counter. An acknowledgement of a sent frame retires it and older outstanding frames, then the newest candidate can be sent.

## Resource Lifetime And Compatibility

The native host offers resource epoch version 1 through the optional hello field `"resourceEpochs":1`. An updated consumer answers with the exact binary bytes `[3,1]`: browser-to-host kind 3 and version 1. Only after receiving that acceptance may the host send resets. No WebSocket subprotocol or new connection is required. The ordinary games continue to send their original hello and never receive an epoch acceptance. An updated browser connected to an older native host likewise sends no acceptance when the hello lacks the field.

A reset is host-to-browser kind 4, followed by one version byte equal to 1, a little-endian `u32` epoch number and a little-endian `u32` last frame number from the previous epoch. Its total length is exactly ten bytes. Epoch zero begins with hello; each reset advances by exactly one. Unsupported versions, padding, skipped or repeated epochs, an unmatched frame barrier and a reset before the previous frame has been taken for rendering are rejected.

Before adding content that would exceed 65,536 identities or 512 MiB of mesh and texture content, the host drains both outstanding frames. It also rotates after 4,096 sent frames within an epoch, even if content is unchanged, so GPU-cache bookkeeping cannot accumulate indefinitely behind stable resource IDs. While draining, input and pings continue and only the newest spool metadata is retained. Stale, duplicate and never-sent acknowledgements cannot satisfy the drain. The reset names the last actually sent frame, not an inferred producer number.

After the drain, the host clears its content-identity dictionary, sends reset, sends every mesh and texture needed by the next frame and then sends that frame. IDs may be reused only after reset. Producer frame numbers, consumed-input sequences, acknowledgements and cumulative statistics continue across epochs. A single frame larger than the resource budget is still rejected; rotating cannot make that frame fit. Overflow of the `u32` epoch counter is an explicit error, not a wraparound.

The consumer forgets old resource IDs and rejects draws referencing resources not declared in the current epoch. It retains the last displayed frame and its GPU cache until the first complete frame of the new epoch is ready. Immediately before rendering that frame, it replaces the entire renderer resource cache, releasing old mesh arenas, textures, bind groups, uniforms, instances and water caches. Pipelines, the GPU device, display, canvas dimensions and input controller remain intact. In-flight GPU commands may retain their own references until completion. Disconnect also releases the stream cache; an existing static ORF replay is reconstructed from its retained file bytes rather than replayed with missing resources.

An older browser ignores the optional hello field and sends no acceptance. The new host then preserves connection-local IDs and ends the connection at a resource or epoch-frame budget instead of sending an unknown reset or silently reusing IDs. Such a browser must reconnect or be updated. All dictionaries, negotiation and barriers belong to one connection; one controller cannot reset another host's session.

## Input

Each binary input message is forwarded unchanged in sequence to the native `remote-input` command.

```json
{"cmd":"remote-input","args":["17","3","0","120","240"]}
```

The arguments are sequence, event kind, button, x or first value, y or second value, and an optional `KeyboardEvent.code`. Event kind numbers match `FRAME-STREAM.md`. Resize reports the browser viewport; native injection maps browser canvas coordinates into the actual native canvas. It does not resize the desktop or inject into unrelated windows.

Physical key codes produce pressed and released events. Native streams preserve repeated physical key-down events, including held Backspace, without deriving typed text from them. Text is a separate key-down extension, `Text:<text>`, with up to 128 UTF-8 payload bytes and no control characters. The host forwards it without synthesizing duplicate text. Native validation can reject characters the legacy client cannot represent. Input sequence acknowledgement is conservative: the producer echoes it only after the native input-consumption pulse, not merely after a control request arrived.

Control failures are reported as sanitized diagnostic text messages and in `host.json`; input values and native response bodies are not logged by the host. Native control error details can be inspected directly through the local control workflow.

## Bounds And Failure Behavior

| Limit | Value |
| --- | --- |
| Active controller | One |
| Concurrent host worker threads | One |
| Inbound WebSocket frame or message | 1 KiB |
| Input rate | 1,024 events per second |
| Native control reply | 64 KiB |
| Native control read and write timeout | Two seconds each |
| WebSocket handshake and write timeout | Two seconds each |
| Browser inactivity timeout | 15 seconds |
| Producer publication inactivity timeout | 30 seconds |
| Full ORF and total expanded records | 128 MiB |
| Individual compressed or expanded record | 32 MiB |
| Records per frame | 100,000 |
| Draw texture references | 16 |
| Frame width and height | 1 through 8,192 |
| Resource identities per epoch | 65,536 |
| Mesh and texture content per epoch | 512 MiB |
| Sent frames per epoch | 4,096 |

The host validates and decodes each record through the shared ORF parser without constructing a second complete expanded frame. Inflation uses a bounded output buffer and requires an exact output length, valid checksum and complete compressed-input consumption. Record appends are transactional. The parser validates record lengths, dimensions and draw references. Draw records accept the canonical size or the older Kotlin writer's exact four-byte zero tail; nonzero tails, other padding lengths and padding on other record types are rejected. Malformed spool frames are dropped with diagnostics; a later valid publication can recover. Negotiated consumers rotate epochs at the cumulative resource or frame ceiling. A single oversized frame or an unnegotiated legacy connection still ends with an explicit diagnostic instead of silently evicting browser resources. Backpressure never creates a growing frame queue or a sequence of spool files.

A temporary browser read timeout is retried. A failed or timed-out send closes the session. A browser that stops acknowledging cannot grow the send window, and a browser that stops responding is disconnected. Recording errors remain visible through native `stream-state`; the host does not restart or alter the native client to hide them.

## Host Status

The host atomically replaces `host.json` in its spool about five times per second while connected and at session end.

| Field | Meaning |
| --- | --- |
| `pid`, `connected` | Host process identity and controller presence |
| `frame`, `frames` | Latest observed producer frame and observed publication count |
| `sent`, `dropped`, `acks` | Sent frames, dropped candidates or gaps, and accepted acknowledgements |
| `inputs`, `inputSeq` | Successfully forwarded input count and latest sequence echoed by a sent frame |
| `resources` | Cumulative resource records sent on this connection, including epoch redeclarations |
| `epoch` | Current resource epoch, initially zero |
| `errors`, `error` | Error count and most recent sanitized diagnostic |

Counters describe the current or most recently ended connection and reset for a new controller. The state file is compact; it is not an append-only event log. Browser stream statistics also expose `epoch`, `epochResources` and `epochBytes`; the latter two describe current-epoch declarations, not total GPU allocation size. The browser's `openrs web do stream` adds receive, render, RTT and WebSocket statistics.

## Synthetic Tests

These commands need no native game, account or GPU for host tests.

```bash
cd code/native-host
cargo test
cargo build --release
cargo tree -e normal
cd ../renderer
cargo test
./build-web.sh
cd ../games
cargo test
cd ../harness
cargo test -p web
cargo build --workspace --release
node --test ../web/tests/stream-input.test.mjs
```

The native-host tests use a fake Unix control server, synthetic ORF3 frames and real loopback WebSocket clients. They cover content identity across changed native IDs, changed content behind one ID, exact input forwarding, text-byte limits, a full send window, producer-number gaps, newest-frame recovery, controller exclusion, disconnect cleanup, malformed-record limits and spool digest races. Browser harness tests cover the additive argument parser alongside the existing game and input tests. Lifecycle tests additionally exercise 4,096 changing synthetic frames through 1,365 rotations with a four-identity test budget, and 300 frames through 99 rotations over a real loopback WebSocket. Byte-budget rotation and a three-frame test ceiling are checked separately. These are measured synthetic counts, not an unattended gameplay endurance claim.

A native GPU test renders 256 synthetic frames across 64 cache replacements. After GPU completion, allocated buffer, texture, texture-view and bind-group handle counts return to the same post-reset baseline on every iteration, and pipelines remain present. This proves that replacing the resource cache discards its GPU handles in that test; it does not establish long-running Chromium GPU-process memory stability or immediate driver memory reclamation. Live login, browser endurance, input continuity through a real-session rotation and visual equivalence remain separate checks.

# Live Integration Notes

- Use different `OPENRS_STATE` directories for native and browser virtual displays. A browser covering the native window on the same nested display stalled frame capture in the live test.
- Native hello dimensions define the browser drawing-buffer size, so pointer coordinates agree with the actual scene and CS2 interface pixels.
- Browser physical keys and explicit `Text:` payloads are sent separately; do not synthesize typed characters twice.
- The supplied account reached world 22, a browser skills-tab click changed native UI, a ground click changed player coordinates, and browser typing appeared once in the native chat entry.
- The initial sample delivered 2.5 FPS. After the core polygon-mode query correction, a 12.15-second sample delivered 4.77 FPS, with zero host errors/drops or stream/GPU errors. Inventory selection, right-click menu opening and dismissal had consumed-input acknowledgement waits of about 423 to 1,017 ms. Different scenes and resource loads prevent treating these samples as a controlled speedup.
- The corrected capture emitted four sparse history regions totaling 5,224 proven untouched pixels. Three same-frame comparisons passed unchanged strict thresholds at mean/minimum SSIM 0.9968 and identical serialization, while nine offscreen draws remained explicitly skipped. This sampled pass does not establish complete rendering or gameplay equivalence.
- The latest first browser connection timed out before receiving a capture; a direct native capture probe and explicit reconnect recovered it. The first-start timeout remains unexplained.
- See `TRANSPILATION-STATUS.md` for the remaining CS2, water-capture, resource-lifetime and performance blockers.

# Capture Profiling

`stream-state.profile` and the latest spool metadata report readback, atlas packing, resource hashing, compression, writer completion, checksum and publication timings. Fields ending in `Nanos` use nanoseconds; byte, event, pixel and region fields are counts. Thread stages overlap and cannot be summed as sequential wall time. Live captures finalize writer compression on a completion worker while offline sequences keep synchronous same-frame screenshots.

The second live measurement delivered about 3.3 new frames per second, versus the earlier 2.5-FPS sample. Three UI input acknowledgements took approximately 0.72 to 1.03 seconds. These measurements do not establish 60-FPS live streaming. After encoded-record reuse, a later warm profile measured approximately 69 ms of resource hashing, 35 ms of texture readback and 1.7 ms of compression, with a 197 ms capture wall time; the producer remains the bottleneck.

Coverage diagnostics use bounded, static numeric fields in the same per-capture profile. `coverageSpriteTight` and `coverageMaskTight` count draws accepted by the audited two-dimensional proof; `coverageSpriteFallback*` and `coverageMaskFallback*` distinguish conservative fallback gates. `historyEligiblePixels` reports the untouched-pixel count even when zero. A positive count does not bypass the history area, byte or rectangle limits.

At most one `coverageFirstFull*` operation category identifies the first transition of accumulated coverage to the whole canvas. It does not mean that operation alone touched every pixel. The accompanying fields preserve numeric native framebuffer IDs, raster/attribute state, or blit mask, filter, rectangles and scissor state as applicable; these are GL state values, not timings. Viewport, scissor and blit rectangles use native framebuffer bottom-left coordinates. `coverageFirstFullArgumentsKnown=0` distinguishes an uninstrumented native draw whose mode, index type and instance count are unknown; those fields are omitted rather than populated with valid-looking zeros. Only the first transition captures these details. The diagnostics do not loosen coverage or identity-copy checks and do not record raw buffers or textures.

The core-profile polygon-mode query is scalar: `coverageFirstFullPolygonMode` records its actual first result. Unused query-array storage is not a second polygon mode. Tight raster coverage requires exact `GL_FILL`; non-fill and unknown modes still fall back conservatively.

Native stop now archives runtime logs in `code/harness/target/session-logs`. Use `code/client/tools/build-check.sh` for serialized full client builds when several agents share the checkout; never rewrite the jar while its client is running.

# Natural Production Rotation

A live native-player session crossed the production 4096-sent-frame boundary without reconnecting. Epoch zero advanced to epoch one, the host remained connected with zero errors, and a browser input after reset was echoed as consumed sequence 30. The observation window was 645 seconds; it began after the session was already running.

Evidence is stored in `code/harness/target/loop/iteration2-live-epochs.json`. This verifies production reset and input continuity. It does not establish long-duration browser GPU-memory bounds.

# Texture Precision

Normalized recordings remain ORF3. A recording containing typed textures uses ORF4 with kind-6 records; the host accepts both versions and validates format, dimensions, exact byte length, sampler bits and bounded inflation before forwarding resources. Old consumers reject ORF4 instead of misreading half-floats. Static replay and the browser stream use the same Rust codec. Resource hashes include texture format and typed sampler state, including across resource epoch resets.

The first typed path covers native GL_RGBA16F and GL_RGB16F 2D textures. Components travel as raw little-endian binary16, with RGB16F expanded to RGBA16Float using native alpha 1. HDR values above one and negative values are not clamped or converted to RGBA8. Swizzles operate on whole half-float components. WebGPU uses filterable RGBA16Float with the native nearest/linear minification, magnification and repeat, clamp-to-edge or mirrored-repeat addressing at base level.

Typed textures declare one mip level and clamp LOD to zero. A native GPU test samples LOD 4 and observes the base level; no byte-oriented mip averaging runs on half-floats. Native mip chains are not transferred, so minification parity is incomplete. Typed sprite atlases, cubes, volumes, other floating formats, integer formats and sRGB remain explicit unsupported captures. Finite binary16 values survive transport exactly; shader arithmetic and GPU NaN canonicalization are separate precision limits.

The shared typed fixture is checked by the JVM writer, bounded host parser, stream epoch round trip and native WebGPU sampling tests with 4.0, -0.5, 0 and 1. Malformed formats, lengths, sampler bits and inflation limits are rejected. These are synthetic checks, not proof that the previously missing live model now renders or that native and browser pixels are equivalent.
