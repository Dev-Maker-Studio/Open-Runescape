# Harness

The harness is the `openrs` command line tool in `code/harness`. It starts the client, the local server and RenderDoc, lets an agent embody a player, and measures how closely the Rust renderer matches the client. This page lists every command and points to the document that covers it.

```mermaid
flowchart TD
  A[Agent] --> U[openrs up or fleet up]
  U --> C[Client on private display]
  C --> S[Local or shared lobby and world]
  A --> D[openrs do, look, trail]
  D --> C
  A --> E[openrs equiv]
  E --> R1[Client capture]
  E --> R2[Renderer capture]
  R1 --> F[Diff report]
  R2 --> F
```

# Build

```bash
cd code/harness && cargo build --release
cd code/client && ./gradlew shadowJar
cd code/renderer && cargo build --release
cd code/web && cargo build --release
```

- The packed cache is built once with `openrs cache build`.
- RenderDoc is built once with `openrs capture setup`.
- `openrs up` and `openrs fleet up` keep the client jar and the packed cache fresh on their own, see Freshness.

# Local Loop

The default test loop runs entirely on this machine, with the client, the lobby and the world all reading one packed cache.

```mermaid
flowchart LR
  U[assets/cache] --> P[assets/cache-packed]
  P --> C[Client]
  P --> L[Local lobby]
  P --> W[Local world]
  C --> L
  C --> W
```

1. `openrs up` checks the pack, starts the local lobby and world on ports 44594 and 44595 if they are not running, and logs a local account into the world.
2. `openrs do`, `look` and `trail` drive the client.
3. `openrs down` stops the client, and `openrs down all` also stops the web client, the display and the local server.

- The local server is started with `CACHE_BUILD_PATH` set to `assets/cache-packed`, so the server and the client share one pack and the login CRC check always matches.
- The local world is world 100, started with `WORLD_BOTS_ALLOWED=true` so clients with the bot sidebar list it. Every local client is pinned to it with `-Dautologin.world=100`, and AutoLogin sets the game host for that world before the world login.
- A recorded process id only counts when its command line is this repository's server, client or Xwayland, so a stale pid file never blocks a start or stops an unrelated process.
- `openrs up shared` targets the shared lobby instead, whose cache build differs, see `CACHE.md`.
- Measured start of the local lobby and world on the shared pack: 8 s and 12 s.

# Commands

| Command | Purpose | Document |
| --- | --- | --- |
| `openrs cache build`, `status`, `verify` | Packed cache from `assets/cache` and the CRC check against a lobby | `CACHE.md` |
| `openrs up`, `down` | One client in the world, ready for commands | `CONTROL_PROTOCOL.md` |
| `openrs do`, `look`, `trail` | Commands, vision and footprints for one client | `CONTROL_PROTOCOL.md` |
| `openrs run`, `show`, `hide`, `toggle`, `shot`, `stop` | Private display, null audio and the display toggle | `CONTROL_PROTOCOL.md` |
| `openrs web up`, `down`, `do`, `look`, `trail`, `run`, `show`, `hide`, `toggle` | The web client in an isolated Chromium on the private display, driven over DevTools | `WEB-HARNESS.md` |
| `openrs player` | Player files, interfaces and the local lobby and world | `PLAYER-INTERFACES.md` |
| `openrs fleet` | Up to nine clients in parallel, and `fleet compare` for each slot | `FLEET.md` |
| `openrs resume` | Restarts what a crash left behind, including the web client | This page |
| `openrs capture` | RenderDoc captures, summaries and diffs | `RENDERDOC.md` |
| `openrs equiv` | Equivalence loop between the client and the renderer | `RENDERDOC.md` |
| `openrs doctor` | One fast check of everything the harness needs | This page |
| `openrs verify` | Scripted live end to end pass of the whole harness | This page |
| `openrs help` | Every command with one line each | This page |

`openrs help` prints the same table the command line dispatches from, so the list never drifts from the commands that exist.

# Freshness

`openrs up` and `openrs fleet up` check three things before anything starts.

| Artifact | Stale when | Rebuilt with |
| --- | --- | --- |
| Client jar `code/client/build/libs/darkan-client-shaded-2.0.0-all.jar` | Missing, or older than the newest file or folder in `code/client/src`, `build.gradle.kts`, `settings.gradle` or `gradle.properties` | `./gradlew shadowJar` in `code/client` |
| Renderer `code/renderer/target/release/renderer-app` | Missing, or older than the newest file in `code/renderer/src`, `shaders` or `Cargo.toml` | `cargo build --release --bin renderer-app` in `code/renderer` |
| Packed cache `assets/cache-packed` | The manifest hash of `assets/cache` differs from the stamp, as in `openrs cache build` | The `openrs cache build` logic |

- Each rebuild prints one line such as `rebuilt client jar (older than the newest client source) in 41.2s`. Nothing is printed when all are fresh.
- The renderer is always rebuilt when stale, because no running process holds it. A jar or pack rebuild is skipped with a message while any client runs on it, because a rewritten jar or pack breaks a running JVM.
- `openrs up` skips the check when its client is already in the world.
- `--no-build` skips the check, for example `openrs up --no-build` or `openrs fleet up 9 --no-build`.
- The cache manifest is hashed by several threads, so a fresh check of the 410 000 file tree takes about 0.2 s.

# Doctor

`openrs doctor` runs every check in parallel and prints one line per finding, with a fix after `fix:` on every line that is not a pass. It takes about half a second when things are fine and exits 0 when no line is `FAIL`.

```text
WARN jar        client jar is older than the newest client source  fix: openrs up rebuilds it, or cd code/client && ./gradlew shadowJar
PASS cache      packed cache matches assets/cache
FAIL sandbox    restricted sandbox detected, home, gradle, runtime not writable: gradle, the client and the local server cannot run in this session  fix: run openrs from an unrestricted shell
12 pass, 5 warn, 5 fail in 260 ms
```

| Check | Pass | Warn | Fail |
| --- | --- | --- | --- |
| `jar` | Jar newer than every client source | Jar stale, `up` rebuilds it | Jar missing |
| `cache` | Pack matches `assets/cache` | Pack stale or missing, `up` builds it | `assets/cache` missing or unreadable |
| `renderdoc` | `openrs capture verify` finds everything | | Anything missing, fix `openrs capture setup` |
| `gpu` | Vulkan lists a hardware device, or device nodes exist without `vulkaninfo` | Vulkan lists only software devices | No `/dev/nvidia*` or `/dev/dri/renderD*` |
| `display` | Live Wayland socket and Xwayland installed | | No socket or no Xwayland |
| `hyprland` | `hyprctl` reaches the running Hyprland | | Not reachable, the private display and the toggle need it |
| `import` | ImageMagick present for the `look` fallback | Missing | |
| `home`, `gradle`, `runtime`, `target` | Writable | | Not writable |
| `sandbox` | | | Shown when home, `~/.gradle` or `XDG_RUNTIME_DIR` is read only, which means a restricted sandbox |
| `java`, `jdk` | Java 17 or newer, a JDK 26 for the server tools | No JDK 26 | No Java or an old one |
| `gradlew-client`, `gradlew-server` | Wrapper distribution downloaded | Not downloaded yet | Wrapper missing |
| `local0`, `local1`, `shared` and other servers | Lobby and world ports open | Local ports closed with the server installed, or the shared lobby unreachable | Local ports closed and the server not installed |
| `fleet` | The fleet file parses, every slot has a role, account, tile, server and credentials | | The first problem found |
| `disk` | At least 5 GB free under `code/harness/target` | Under 5 GB | Under 1 GB |

- The servers come from `openrs up`, every server of the fleet file including each local instance, and the shared lobby, each probed once.

# Verify

`openrs verify` drives the whole harness live, one small step at a time, and stops at the first failure with the step name and the last lines of output. It needs an unrestricted session with the GPU, the display and the local server.

```mermaid
flowchart LR
  D[doctor] --> U[up]
  U --> C[Control commands]
  C --> I[Interface edits and look]
  I --> F[dump-frame and replay]
  F --> E[equiv and RenderDoc retry]
  E --> FL[fleet up 9, do, compare, status]
  FL --> X[down all]
```

| Step | What is exercised |
| --- | --- |
| `doctor` | `openrs doctor` exits 0 |
| `down-before`, `reset-player` | No client or fleet runs, the `claudeqa` inventory is cleared and the player placed at 3222 3218 |
| `up`, `time-to-world` | `openrs up` with scripts enabled, then the `[Milestone]` lines of the client log |
| `reads` | Every state read in one batch |
| `teleport`, `walk`, `waits` | `command tele`, `walk` and every wait form |
| `chat`, `give-item` | Public chat seen by `wait-until chat`, an item added and seen by `wait-until inv` |
| `widget-drop`, `take` | The item dropped through `widget 679 0 8`, found on the ground and taken back |
| `npc`, `object` | Option 1 on the first named npc and object |
| `scripts` | The first script started, seen running and stopped |
| `look`, `look-display` | A frame readback, then `look` with `OPENRS_LOOK_VIA=display`, which forces the display screenshot fallback |
| `iface-open`, `iface-text-look`, `iface-item-model`, `iface-close` | `openrs player iface` opens interface 275, sets text, an item and a model live, `look` sees it, then it closes |
| `dump-frame`, `renderer-replay` | A `.orf` dump replayed headless by `renderer-app --replay` |
| `equiv` | `openrs equiv --live --cold`, which must run in replay mode on a dumped frame |
| `renderdoc-retry` | A capture that fails takes the retry path, then a renderer capture with `--retries` succeeds |
| `down`, `fleet-up`, `fleet-time-to-world`, `fleet-do` | Nine slots, their world entry times and one parallel `fleet do` round |
| `fleet-compare` | `openrs fleet compare` on all nine slots at once, each slot's SSIM recorded |
| `fleet-status`, `fleet-down`, `down-all` | The memory totals and the shutdown |

- `openrs verify list` prints the steps and `openrs verify --from <step>` resumes at one.
- The summary is written to `code/harness/target/verify/summary.json` with the status, time and detail of every step and the measurements: milestones, time to world, the equivalence verdict and score, the fleet world entry times and the fleet memory totals.
- Screenshots, the frame dump, the replay image and the capture folders land next to it.
- A failure leaves the clients running for inspection, `openrs down all` and `openrs fleet down` clean up.

# Documents

| Document | Topic |
| --- | --- |
| `CACHE.md` | Packed cache, login CRC table and the shared lobby's cache build |
| `CONTROL_PROTOCOL.md` | Control server, wire format, waits, vision, footprints and the embodied tester guide |
| `PLAYER-INTERFACES.md` | Interfaces stored in the player file and live edits |
| `FLEET.md` | Nine client fleet, roles, start tiles, accounts and measurements |
| `RENDERDOC.md` | Capture setup, frame summary, diff and the equivalence loop |
| `RENDERER.md` | The Rust WebGPU renderer and the shader translation |
| `RENDERER-FRAME-FORMAT.md` | The `.orf` frame dump and replay format |
| `MODERN-RENDERER-WEB-TRANSFER.md` | What the modern renderer is and the plan to move it to the web |
| `WEB.md` | The Rust web server and page |
| `WEB-HARNESS.md` | `openrs web`, the browser, the page bridge and its commands |
| `SESSION-START.md` | The prompt for starting a new Claude Code session on this project |

# Transpilation Work Split

Nine agents can work on the renderer at once, one per fleet slot.

| Slot | Role | Work |
| --- | --- | --- |
| 1 | model-pass | Model pipeline and instancing |
| 2 | ground-pass | Terrain, overlays and textured ground |
| 3 | sprite-ui-pass | Sprites, fonts and interfaces |
| 4 | shadow | Shadow pass |
| 5 | particles | Particle pass |
| 6 | water | Water, including a replacement for the tessellated water shaders |
| 7 | textures-materials | Texture arrays and material effects |
| 8 | scene-camera-lighting | Camera, fog and lighting uniforms |
| 9 | equivalence-diff | Runs `openrs equiv` on every scene and reports the score trend |

1. The agent places its slot at the role's start tile with `openrs fleet place`.
2. It dumps a frame with `dump-frame` and replays it in the renderer.
3. It changes the renderer and runs `openrs equiv` until its pass matches.

# Accounts

- Local accounts `fleet1` to `fleet9` are created on the local server by `openrs fleet up`. Their passwords are generated into `~/.config/openrs/fleet-local.json`, outside the repository.
- Accounts on the shared lobby are created by the user and named in `code/harness/fleet.toml`, which git ignores, through `env:` or `file:` credential sources.

# Verified Results

The full live pass, `openrs verify`, passed all 34 steps on this machine against the local lobby and world, with the client, lobby and world on one packed cache.

| Measure | Value |
| --- | --- |
| Cache ready | 3.3 s after process start |
| Lobby logged in | 4.3 s |
| First world frame | 8.1 s |
| Nine slot fleet up | 74 s, slowest slot in the world after 14.0 s |
| Nine `fleet do` calls in parallel | 46 ms in total |
| Nine `fleet compare` runs in parallel | 39 s, SSIM 0.875 to 0.997 |
| Nine clients | 8.0 GB PSS and 3.9 cores, display 156 MB |
| One local lobby and world pair | Lobby 3.1 GB to 3.5 GB RSS, world 1.9 GB RSS |
| Equivalence score on a real dumped frame | 73.1, up from the first score of 64.4 |
| Whole verify run | 360 s, including a renderer rebuild |

```mermaid
flowchart LR
  A[up 8 s] --> B[control and interfaces]
  B --> C[dump-frame]
  C --> D[renderer replay]
  D --> E[equiv 73.1]
  E --> F[fleet of 9 and compare]
  F --> G[down]
```

- The equivalence score tracks the transpilation. The rise from 64.4 to 73.1 came from the first round of the nine slot agents, which replayed the `surfaceMask` draws and fixed frame wide colour handling.
- Offscreen draws, such as the minimap, are still skipped on replay.
- RenderDoc replays run one at a time across every `openrs` process, behind the lock file `openrs-gpu-replay.lock` in the temp folder, because each full replay of a game frame needs gigabytes of GPU memory and the card has 6 GB.

# Crash Recovery

`openrs resume` restarts what a crash left behind.

1. Every fleet slot whose state file remains and whose client is gone brings the fleet back up to that size. `fleet up` skips slots that still run and starts the local servers they need.
2. A single client whose state file remains and whose process is gone is started again with `openrs up`.
3. When nothing was left behind it prints `nothing to resume`.

- `openrs down` and `openrs fleet down` remove the state files, so a clean shutdown is never resumed.
- Measured after a session crash: the nine slot fleet and its three server pairs came back in 95 s.

# Known Limits

| Limit | Effect |
| --- | --- |
| Shared lobby cache build differs from `assets/cache` | Login works through the lobby's CRC table, but content can differ from the shared lobby |
| Offscreen targets not replayed | The minimap and other offscreen draws are skipped on replay, which lowers the equivalence score |
| Platform | Development runs on Linux with Hyprland only: the client's GL context is GLX and the private display needs Hyprland. Every other platform is reached through the WebGPU build |
| Server memory | Three local server pairs for nine slots use about 15 GB, mostly the lobbies holding the packed cache in memory |
| Water reflections and bloom | The planar reflection target and bloom are not ported to the renderer yet |
| Frame stream to the browser | Not built, see `MODERN-RENDERER-WEB-TRANSFER.md` |
| Sandboxed sessions | A Claude Code session whose Bash commands run in a sandbox cannot build to `~/.gradle`, see the GPU or start a display, so it cannot run the client; `openrs doctor` reports this as a restricted sandbox |
