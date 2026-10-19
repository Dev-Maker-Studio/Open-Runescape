# CLAUDE.md - Darkan Server Project Guide

Darkan is a multiplayer game server emulator for **revision 727** of the RuneScape protocol: one
lobby server (authentication, accounts, world list, friends/ignores/friends chat/clans) and any
number of world servers (gameplay). Kotlin 2.4.20, Gradle 9.7.1, Java 26, MongoDB. Library versions
live in the Gradle build files.

```
Game client ──TCP 43594──▶ LOBBY  (raw socket 43594, HTTP API + social gateway on 4040, JS5)
                              ▲ ws://lobby:4040/social/ws
Game client ──TCP 43595──▶ WORLD(s) (raw socket 43595, SocialClient)
                     both ──▶ MongoDB
```

- **The lobby is authoritative** for social state; worlds reach it over the social gateway
  WebSocket. Nothing revision-specific crosses that socket: `SocialAction` / `SocialEvent`
  (`core/.../social/SocialProtocol.kt`) are the whole vocabulary, and each side renders its own
  client packets with `SocialPackets`. See `docs/social-gateway.md`.
- **Suspend functions everywhere** for I/O. Game-tick code (actions, interactions) is not suspend.
- **Packet handlers** implement `PacketHandler<Player, Packet>` and are discovered by classpath
  scanning of the module's `server.packet` package. No content in packet handlers.
- **600 ms engine tick** (`EngineLoop`, `WORLD_CYCLE_NANOS`).

---

## ⚠️ MANDATORY: Code Standards

Every edit to source, tests, scripts or the cache source tree follows
`.claude/skills/darkan-code-standards/SKILL.md`. The shared `.claude/settings.json` injects it
before every Edit/Write through `.claude/hooks/code-standards.sh`; read it anyway before you start.
In short:

1. **No comments** except on something genuinely complex whose behaviour is unclear.
2. **Kotlin, never new Java**; convert a touched Java file when player saves/serialization allow.
3. **Logic lives where it belongs** - move misplaced code when you see it.
4. **No magic numbers** - every id is a gameval constant, placeholders included; an asset we
   add ourselves is named for its purpose in the identified-name convention, never a placeholder.
5. **A varbit when the client must own the state** - new content whose interface has to reflect,
   remember or refresh a value on its own (`saveVarBit`, first free bit of the feature's parent
   varp, no overlap, CS2 var hooks instead of server-side `setIFGraphic`). Server-only state stays
   in the player save.
6. **No AI attribution** in commits, merge requests or code - ever.

Game content - world-module plugins and dialogues, cache definitions, imported assets - follows
`.claude/skills/content-development/SKILL.md` (the same hook injects it for `world/src` and the
definition, model and catalog paths of `cache-source/`). Plugin hooks follow
`.claude/skills/kotlin-plugins/SKILL.md` (injected for `world/src/main`): top-level
`@ServerStartupEvent` functions using the `com.rs.plugin.kts` DSL, in compiled Kotlin, never
`.plugin.kts` scripts. Static `new XHandler(...)` / `@JvmField` handler fields are deprecated, and a
file using them is converted properly whenever it is touched. Clientscript, interface and graphic work in `cache-source/` also follows
`.claude/skills/cs2-engineer/SKILL.md` (the same hook injects it for those paths); add what you learn
about the client to it in the same change.

---

## ⚠️ MANDATORY: Specialized Agent Invocation

These agents are the **exclusive code owners** of their domains. Any change in an owned path, however
small, goes through the owning agent (invoke several when a task crosses boundaries, and give each
full context). Do not guess at RS-specific formats, byte orderings or protocol details.

| Agent | Owns | Covers |
|-------|------|--------|
| **cache-library-engineer** | `core/src/main/kotlin/world/gregs/voidps/**` (`buffer/`, `cache/`, `type/`) | Definition decoders/encoders, `Reader`/`Writer`, `BufferReader`/`BufferWriter`/`JagExtensions.kt`, `Cache`/`FileCache`/`MemoryCache`/`Index`, compression (GZIP, BZIP2, LZMA), `Xtea`/`CRC`/`Whirlpool`/`Huffman`, JS5 file providers, sectors and archive addressing, `Tile`/`Region`/`Zone`, definition mixins |
| **networking-protocol-engineer** | `core/src/main/kotlin/org/darkan/core/net/**` | `ClientProt`/`ServerProt` definitions, `Rev727.kt` codec registration, `Rev727UpdateProtocol.kt` and update masks, `Codec.kt` DSL, `Session.kt`, `Isaac.kt`, `PacketHandlers.kt`, request/response opcodes, login tokens, packet inversion from `../darkan-bot`, any code reading or writing network buffers |
| **rendering-tools-engineer** | `tools/**`, `cs2/**` | Shaders, meshes, instancing/batching, camera, culling/LOD, model/texture/terrain loading, ImGui, the viewers; and the headless CS2 toolchain in `cs2/`, which depends only on `:core` and must stay that way (no LWJGL, GLFW, ImGui, JOML or `:tools`) |

**Exception:** only an explicit user statement ("don't use agents for this", "do this yourself")
lifts the requirement.

---

## Modules

| Module | What it is |
|--------|------------|
| `core/` | Shared library: `net` (sessions, protocol, rev 727 codecs, login tokens), `model`, `mongo`, `crypt` (Argon2id, legacy AES), `worldlist`, `social` (+ `social.gateway`), `engine`, `gameval` |
| `lobby/` | Lobby server: socket server, packet handlers, Ktor HTTP API (`web/Routes.kt`), `social/SocialGateway` host |
| `world/` | World server: socket server, gameplay (legacy `com.rs.*` tree + Kotlin), `social/SocialClient` |
| `cs2/` | Headless CS2 clientscript toolchain: decompiler, TS compiler, VM (`docs/cs2-editor.md`) |
| `tools/` | Cache/map/interface/CS2 editor suite (LWJGL) |
| `migration/` | Legacy → modern database migration (`docs/migration.md`) |
| `cache-source/` | git submodule: the unpacked 727 cache + `gamevals/<type>.json` catalogs (`docs/cache-source.md`) |

Configuration is read in `core/src/main/kotlin/org/darkan/core/EnvVars.kt` from a root `.env`;
`docs/environment.md` explains the commonly tuned variables.

---

## Building and Testing

```bash
./gradlew build                          # all modules + the fast test suites
./gradlew :lobby:run                     # lobby (blocking)
./gradlew :world:run                     # world (blocking, needs a lobby)
./gradlew test                           # fast suites only
./gradlew :core:cacheTest :cs2:cacheTest # whole-cache and parity tests (opt-in; minutes, need the packed cache at CACHE_PATH)
./gradlew :lobby:shadowJar
./gradlew :tools:run --args="gameval dump cache-source/gamevals/work/727"   # every 727 definition to JSONL

# The cache source tree (docs/cache-source.md). 'verify' exits 1 unless packing the tree reproduces
# the packed cache byte for byte, bar the clientscripts it compiles back to equivalent bytecode and
# the two tables derived from them.
./gradlew :tools:run --args="cache unpack cache-source"
./gradlew :tools:run --args="cache verify cache-source ../cache"
./gradlew :tools:run --args="cache build cache-source data/cache"
./gradlew :tools:run --args="cache status cache-source"
```

Two suites per module:

- `test` (`src/test`): fast unit and harness tests. `./gradlew build` runs them, and the root build
  fails a module whose suite exceeds `darkan.testBudgetSeconds` (default 20) of test time.
- `cacheTest` (`src/cacheTest`, core and cs2): everything that opens the packed cache or a source
  tree. Opt-in only; nothing in `build`, `check` or CI runs them. Run them when a change touches
  what they cover.

A test that opens the cache or runs for more than a few seconds goes in `cacheTest`, never in
`test`, and nothing gets wired to run on every build. `./gradlew :buildSrc:test` runs the gameval
generator's tests on demand.

---

## Gamevals

The catalogs in `cache-source/gamevals/<type>.json` are **canonical and hand-edited**; there is
nothing to regenerate them from. A name added by hand meets the evidence bar in
`cache-source/gamevals/CONTRACT.md`. The typed constants in `org.darkan.core.gameval.gen` (`Obj.coins`,
`Varbit`, `Loc.loc_54321`, ...) are **not committed**: every build generates them from the
catalogs, so renaming a catalog entry renames the constant and breaks every stale call site, and a
conflicting catalog fails the build. Runtime catalogs come from `GAMEVAL_PATH` and need only a
restart. Use `Obj.coins` or `Gameval.requireId(Gameval.INV, "worn")`, never a magic number.

The legacy package `com.rs.engine.variables` is **removed**. If a legacy re-sync batch
(`docs/legacy-baseline.md`) reintroduces it or imports of it, migrate the callers to
`org.darkan.core.gameval.gen` and delete it again. Full details: `docs/gameval.md`; cache archive
names (map squares, native libraries, shaders) are not gamevals: `docs/cache-archive-names.md`.

---

## Documentation

- `docs/environment.md` - `.env` variables and what the non-obvious ones mean
- `docs/social-gateway.md` - Social gateway architecture, gateway messages, the per-player website socket
- `docs/headless-client.md` - The client side of core: bidirectional codec, `ClientSession`, login encoding, JS5 cache sync, entity update decoding
- `docs/community-worlds.md` - Running your own world against the dev lobby: the public community token, what the lobby enforces, the token-replay limit
- `docs/clans.md` - The clan system: lobby-authoritative `ClanService`, typed `ClanAction` commands over the gateway, world `ClanRegistry`, shared `ClanRules`/`ClanPackets`
- `docs/deploy.md` - VM layout, systemd units, CI-driven deploys and the legacy cut-over runbook
- `docs/migration.md` - Legacy database migration tooling and runbook
- `docs/legacy-baseline.md` - Which legacy commits the modern tree is synced to
- `docs/gameval.md` - Gameval name catalogs: runtime API, typed constants, regeneration
- `docs/cs2-editor.md` - The CS2 clientscript toolchain (`cs2/`), its CLI, and compiling from server code
- `docs/rs3-port.md` - Porting RS3 assets into the 727 cache: `tools rs3port`, the material crosswalk, the calibration gate
- `docs/cache-source.md` - The unpacked cache: layout, parity guarantees, codecs, build and `CACHE_SOURCE_PATH`
- `cache-source/gamevals/CONTRACT.md` - The evidence rules under which every gameval name was proven
- `README.md` - Quick start guide
