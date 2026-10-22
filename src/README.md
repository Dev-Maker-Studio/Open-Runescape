# OpenRS Web

OpenRS Web takes the 2012 RuneScape client, build 727, from the Darkan project and moves its modern renderer to the web in Rust, while the cache, lobby and server stay as they are.

| Part | Location |
| --- | --- |
| Kotlin client | `code/client` |
| Lobby and world server | `code/server` |
| Unpacked and packed cache | `assets/cache`, `assets/cache/.packed` |
| AI development harness | `code/harness`, the `openrs` command |
| Rust WebGPU renderer | `code/renderer` |
| Streaming test-game hosts | `code/games` |
| RenderDoc tooling | `code/renderdoc` |
| Web server and page | `code/web` |

- Start with `HARNESS.md`, which lists every command, the latest 55-step verification and measured results.
- `FRAME-STREAM.md` and `GAMES.md` describe the host-to-browser architecture and interactive test games.
- `SESSION-START.md` is the prompt for starting a new Claude Code session on this project.
- The Darkan website is https://darkan.org
