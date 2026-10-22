# Test Games

`code/games` is a standalone Cargo project, outside the harness workspace, with the `games` library and the `openrs-games` binary. It is the first game host of the frame stream in `FRAME-STREAM.md`: three small games built from the renderer's own passes and the scene helpers of `code/renderer/src/scene.rs`, simulated at a fixed 60 Hz and streamed to any number of browsers, each with its own game instance. The Kotlin client becomes the next host, speaking the same protocol.

## Layout

| Path | Task |
| --- | --- |
| `code/games/src/game.rs` | The `Game` trait, the tick rate, the game table and fast forward |
| `code/games/src/draw.rs` | Sprite HUD builder with a digit font, the camera, and the ground unprojection of a click |
| `code/games/src/orbit.rs`, `walker.rs`, `buttons.rs` | The three games |
| `code/games/src/host.rs` | WebSocket sessions, resource deduplication by id and hash, the flow control window |
| `code/games/src/dump.rs` | Consecutive frames as self contained `.orf` files |
| `code/games/src/main.rs` | The `openrs-games` command line |
| `code/games/tests` | Scripted input tests and the WebSocket integration test |

The project depends on `code/renderer` with `default-features = false`, which leaves out `wgpu` and every GPU module, so the host builds in seconds and runs headless. The renderer's `gpu` feature is on by default for the native binary, the tests and the wasm build.

## Game Trait

```rust
pub trait Game: Send {
    fn step(&mut self, dt: f32, inputs: &[Input]);
    fn frame(&mut self, width: u32, height: u32) -> Frame;
}
```

- `step` runs once per tick with the inputs that arrived since the last tick, in order, with `dt` of one sixtieth of a second.
- `frame` builds the draw list for the canvas size the browser reported. Meshes and textures are included when they are new or changed, static ones only in the first frame, and the host sends only what the connection has not seen.
- `game::create_at(name, n)` creates a game and steps it `n` times without input, the fast forward of `--frame N`.

## Games

| Game | Tests | Content |
| --- | --- | --- |
| `orbit` | Animation over many frames, determinism | Three spinning, bobbing textured cubes on the terrain with shadows, HD water in the valleys, 24 rising particles, a camera orbiting the center, and a HUD with a bar and the tick number. State is the tick number alone, so frame `N` is identical in every run and `--frame N` jumps there |
| `walker` | Keyboard, mouse and latency | A cube character on the terrain. `W` `A` `S` `D` or the arrows move it 4 units per second along the world axes, a left click moves it to the clicked ground point and shows a small marker there until it arrives, a key press cancels the walk. The camera follows from behind and above. The HUD shows the position as `(x + 11) * 10` and `(z + 11) * 10`, and the click count in yellow |
| `buttons` | Pointer input | Three buttons that brighten under the pointer, darken while pressed and count their clicks, a slider whose knob or track can be dragged and which the wheel nudges by five percent, with the value in percent, and a dot under the pointer |

- Walker clamps the position to 11 units from the center so the character stays on the terrain. Losing focus releases every key.
- Buttons counts a click only when the press and the release hit the same button.
- Every game clears to its own color and draws its HUD through the sprite pass with the 8 by 8 digit texture, layer per digit.

## Commands

| Command | Effect |
| --- | --- |
| `openrs-games serve [--port P]` | Listens on `127.0.0.1`, default port 8100, one thread per connection, the game chosen by `?game=NAME` or the path |
| `openrs-games dump NAME N DIR [--frame S] [--size WxH]` | Writes `frame-S.orf` to `frame-(S+N-1).orf`, each with every mesh and texture the game has produced so far, default 800 by 600, headless |
| `openrs-games list` | The game names |
| `openrs games list` | The same through the harness, building the host first when it is stale |
| `openrs web up --game NAME` | Starts the host, opens the page on that game, see `WEB-HARNESS.md` |

```
cd code/games
cargo build --release
./target/release/openrs-games dump orbit 3 /tmp/orbit --frame 90
../renderer/target/release/renderer-app --replay /tmp/orbit/frame-90.orf --out orbit.png
```

## Session

1. The handshake reads the game name from the request, an unknown name gets one text message with the names and a close.
2. `hello` goes out with the default size, then the socket is switched to non blocking.
3. Every tick: read every pending input and ack, step the game, and if the window allows it build the frame, send a `resource` message for new or changed meshes and textures, then the `frame` message with the frame number and the last input sequence. Otherwise count a drop and send a `ping` once a second.
4. The tick waits for the next 16.7 ms boundary and never catches up on lost time, so a stalled host does not burst.

## Determinism

- `orbit` has no input and depends only on the tick number: `dump orbit 1 D --frame 10` equals frame 10 of `dump orbit 13 D`, byte for byte, which `dump::tests` checks with the host's hash.
- Walker and buttons are deterministic for a given input sequence, which the scripted tests rely on, but a live session depends on when inputs arrive relative to ticks.

## Tests

| Test | Checks |
| --- | --- |
| `stream::tests` in the renderer | Message encode and decode both ways, resource merging, browser side drop counting, the ping answer |
| `host::tests` | The window drops frames until an ack, resources go once unless their bytes change, the game name from the URI |
| `dump::tests` | Orbit frame hashes equal across runs and across fast forward, every game dumps files that parse and reference only meshes they carry |
| `tests/games.rs` | Walker moves 4 units per second on a key, stops on focus loss, walks to a click and arrives, buttons hover, press, release, cancel, drag, wheel |
| `tests/host.rs` | A WebSocket client runs through every game: hello, resources first, frame 1, resize and key echoed by sequence with the new size, the host stops at two unacked frames and pings, resumes after an ack, and an unknown game is refused |

```
cd code/games && cargo test
```

## Measured

On the private display shared with other browsers, page at 48 frames per second.

| Game | Frame message | Bytes per frame with resources | Round trip | Host drops |
| --- | --- | --- | --- | --- |
| `orbit` | 2.8 KB | 4.5 KB, the particles and the HUD travel as a resource each frame | 55 ms for the first inputs, sent during the connection | 29 of 433 frames with the page behind 60 Hz |
| `walker` | 1.0 KB | 1.1 KB | 35 ms | 4 of 1031 |
| `buttons` | 217 B | 200 B | 17 to 25 ms, 77 ms once during a 35 event burst | 2 of 77 |
