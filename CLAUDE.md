# Darkan Bot - Project Overview

## Project Description

Darkan Bot is a refactored and deobfuscated version of the RuneScape game client from mid-2012 (Client build #727). This project serves as the game client for the Darkan server emulation project, which aims to recreate the 2012 era RuneScape experience.

**Website**: https://darkan.org

## Current State

The Java-to-Kotlin migration is **complete**. `code/client/src/main/java/` no longer exists;
the codebase is 100% Kotlin.

Current focus is deobfuscation and structural cleanup of the migrated code
rather than migration itself.

## Architecture

### Core Packages

#### 1. `com.darkan.bot` - Bot Framework
Custom bot functionality built on top of the game client:
- **Bot.kt**: Main entry point and bot controller
- **scripts/**: Script execution framework for automated gameplay
- **utils/**: Bot utility functions and helpers

#### 2. `com.jagex.game` - Game Client Engine
The original Jagex client code, extensively deobfuscated and refactored:

- **runetek5/**: Core RuneTek 5 game engine
  - `cache/`: Game asset caching system
  - `client/`: Main game client, input handling, console
  - `config/`: Game configuration and definitions (items, NPCs, objects, interfaces)
  - `entity/`: Game entities (players, NPCs, ground items, game objects)
  - `graphics/`: Rendering system and graphics models
  - `network/`: Client-server network protocol
  - `scene/`: 3D scene management and rendering
  - `sound/`: Audio system
  - `movement/`: Pathfinding and route calculation
  - `js5/`: JS5 archive format (Jagex's asset format)
  - `toolkit/`: Graphics toolkit abstraction layer
    Toolkit ids (`AbstractToolkit.toolkitForType`): 0 (`BOOTSTRAP_MODE`) is `SafeModeToolkit`, used
    only for the loading screen, the CPU benchmark and the offscreen item icon rasterizer; every
    other id creates `ModernGLToolkit` (`ToolkitPreference.MODERN_OPENGL` = 3, kept so the cache's
    per-toolkit option bitmasks and scripts apply unchanged). The legacy OpenGL toolkit
    (`OpenGLNativeToolkit`, `AbstractNativeToolkit`, the `Native*` shaders, samplers and buffers,
    `ShaderGL`/`ShaderMappingGL`, the `MipmapTextureSampler*` family, `MaskedTextureSurface`,
    `NativeParticleRenderer`, `VertexDeclaration*`, the `companion` enums) is deleted; only
    `OpenGlToolkit` survives as the parent of `ModernGLToolkit`. `toolkit/opengl/OpenGLConventions.kt`
    holds the GL enum constants the surviving code imports. `ToolkitPreference` is invisible and
    always reports 3.
    The GL context is created in `jaggl/OpenGL.kt` through `toolkit/context/GlxContext` (GLX on the
    AWT canvas's X11 window). Development runs on Linux with Hyprland only; other platforms are
    reached through the WebGPU build, so there are no Windows or macOS context paths.

- **launch/**: Client loader and initialization
- **addon/**: Client addon system

#### 3. Native Libraries
Low-level native interface libraries (all migrated to Kotlin):
- **jaclib/**: Hardware info, memory management, timing, networking
- **jaggl/**: OpenGL graphics bindings
- **jagtheora/**: Theora video codec (Ogg, Vorbis, Theora containers)

## Technology Stack

### Build System
- **Gradle**: Build automation (Kotlin DSL)
- **Shadow Plugin**: Creates fat JAR with all dependencies

### Languages
- **Kotlin 2.2.21**: The entire codebase
- **Java 17**: Target JVM version

### Key Dependencies
- **Kotlin Coroutines**: Asynchronous programming
- **Kotlinx Serialization**: JSON serialization
- **Kotlin Scripting**: Runtime script compilation and execution
- **ClassGraph**: Classpath scanning and reflection
- **DarkLaf**: Dark theme look-and-feel for Swing UI

## Build & Run

### Main Class
```
com.darkan.bot.BotKt
```

### Gradle Tasks
```bash
cd code/client && ./gradlew build       # Build and create shadow JAR
./gradlew shadowJar   # Create fat JAR: build/libs/darkan-client-shaded-2.0.0.jar
./gradlew run         # Run the client
```

### Output
- Standard JAR: `build/libs/darkan-bot-2.0.0.jar`
- Shadow JAR: `build/libs/darkan-client-shaded-2.0.0.jar`

## CI/CD (GitLab)

`.gitlab-ci.yml` builds the fully shadowed jar with LWJGL natives for Linux, macOS and Windows
(`-PallNatives=true`, automatic on CI) and publishes it:

- Every push to `dev`: latest dev build, overwritten each time
  `https://gitlab.com/darkanrs/darkan-bot/-/jobs/artifacts/dev/raw/dist/darkan-client.jar?job=build`
  (also in the generic package registry as `darkan-client/dev-latest/darkan-client.jar`)
- Every `vX.Y.Z` tag: a GitLab release versioned from the tag, with permalinks
  `.../-/releases/permalink/latest` and `.../-/releases/permalink/latest/downloads/darkan-client.jar`
- Cut a release: `git tag v2.1.0 && git push origin v2.1.0`
- The version and commit land in the jar manifest (`Implementation-Version`, `Git-Commit`) and
  show in the sidebar's Settings > About. Local builds keep version 2.0.0 and host-only natives.

## Crash Reporting

`com.darkan.crash` owns the player-facing crash flow; the launcher (`~/darkan/client-loader`) reads the
same files, so the two must stay in sync:

- `CrashHandler.install()` runs first in `main`: wraps stdout/stderr in `ConsoleRecorder` (last 400
  lines), installs the default uncaught-exception handler and names the engine thread `darkan-game`.
- Fatal errors (`GameShell.run`/`initializeFrame` catch blocks, uncaught on `main`/`darkan-game`) call
  `CrashHandler.fatal`: writes `~/.darkanrs/logs/crash-<UTC stamp>.txt`, prints the summary to stderr,
  shows `CrashDialog` (copy report → open Discord → paste) and exits with status 1 when it closes.
  `DARKAN_HOME` overrides the root, like the launcher.
- The file starts with the Discord-sized summary (a fenced block ≤ 1800 chars) followed by the line
  `==== Full details below …`; the launcher slices on that marker. Don't change the file name pattern
  or the marker without updating `crates/launcher-core/src/report.rs`.
- Recoverable errors go through `PacketEncoder.sendErrorToServer` → `CrashHandler.noteError` (no
  network; Jagex's `clienterror.ws` is gone) and appear in the next report's "recent errors".
- Account name and password are scrubbed from every report.
- Test hooks: `-Ddarkan.crashTest=1` crashes at startup; console commands `crash` (real game-thread
  path) and `bugreport` (dialog without a crash); `-Ddarkan.crashDialog=false` skips the dialog.
  The bot sidebar's Settings > About has "Report a bug…".

## Key Features

### 1. Bot Scripting Framework
- Public scripts live in the `darkanrs/bot-scripts` GitLab repo (checked out locally at `~/.darkan/scripts`,
  the folder the client scans); no scripts ship inside the client. That repo's `CLAUDE.md` and
  `darkan-script-dev` skill read this client's API from GitLab raw URLs on `dev`.
- `java -cp darkan-client.jar com.darkan.bot.scripts.ScriptCheckKt <dir>` compiles a folder of `.kts`
  scripts exactly as the client does (`KotlinScriptScanner.compile`) and exits 1 on any failure.
- Scripts are listed without compiling: `KotlinScriptScanner.catalog()` reads `ScriptHeader` (the
  `@ScriptDescription` literals, the class name and a direct `ConfigurableScript`) and the sidebar compiles a
  script on its first Start/Settings (`ScriptMetadata.load()`, off the EDT, cached per file + lib stamp).
  A header that is not plain literals falls back to compiling at listing time. Nothing loads unless
  `-Dbot.enabled=true`. `ScriptCheckKt` flags headers the client cannot read without compiling.
- Scripts load from the whole folder tree (`KotlinScriptScanner.scripts()`), skipping `lib/`, hidden
  folders and `.git`; a compiled script is named after its file alone, so a duplicate file name
  anywhere in the tree is skipped with a message instead of shadowing the first.
- `<scripts>/lib/*.kts` is shared code: the loader adds it as `importScripts` for every script (a
  `beforeCompiling` refinement skips lib files themselves to avoid the self-import cycle).
- Script settings: `scripts/ScriptConfig.kt` is pure data (items, `ConfigSection`, `ConfigAction`/
  `withAction`, `ConfigVisibilityProvider`, `ScriptConfigStore`), ported from darkan-3-undercut so
  scripts never touch Swing; `ui/ScriptConfigWrapper.kt` renders the form and calls
  `onConfigUpdated()`. Values persist per class name across reload via `ScriptInstances.instantiate`.
- `ScriptRepository` (JGit, bundled) keeps the scripts folder a checkout of `darkanrs/bot-scripts` `dev`:
  `Bot.main` runs it async before the first load when `-Dbot.enabled=true`
  (`-Dbot.scripts.sync=false` disables, `-Dbot.scripts.remote=<url>` overrides), the sidebar's
  "Update scripts" button reruns it, and `ScriptRepositoryKt <dir>` is the CLI. Init in place +
  rename colliding files to `.local`; fast-forward only; never touches dirty trees, other branches or
  diverged commits. `ScriptExecutor.addReloadListener` refreshes the sidebar after any load.
- Kotlin-based scripting engine
- Runtime script compilation
- Automated gameplay capabilities
- Script hot-reloading support

### 2. Deobfuscation & Refactoring
The original obfuscated client has been extensively refactored:
- Meaningful class and method names
- Proper package organization
- Idiomatic Kotlin code patterns
- Improved code readability and maintainability

### 3. Client Enhancements
- Console command system
- Dark UI theme
- Modern Kotlin patterns (data classes, extension functions, coroutines)
- Enhanced input handling

### 4. Game Engine
Full RuneTek 5 engine implementation including:
- 3D scene rendering
- Entity management (players, NPCs, objects, items)
- Network protocol handling
- Asset cache management (JS5)
- Pathfinding system
- Audio/video playback
- UI/interface system

## Development Workflow

### Migration Strategy
The project follows an incremental migration approach:
1. Start with smallest, simplest files
2. Avoid files with complex reflection or generics initially
3. Maintain binary compatibility at public API boundaries
4. Full build validation after each migration
5. Careful nullability analysis (legacy code favors nullable types)

### Code Quality Guidelines
- Preserve existing APIs unless clearly safe to change
- Use idiomatic Kotlin features gradually
- Conservative nullability (nullable by default if uncertain)
- Minimal refactoring during migration (unless fixing clear bugs)
- Fix obvious typos and deobfuscate names when confident

## Project Structure

```
OpenRS-Web/
├── _APP-SPECIFICATION.md
├── _MARKDOWN-FORMAT.md
├── assets/
│   ├── cache/         # Unpacked cache, cloned separately, gitignored
│   └── cache-packed/  # Built by `openrs cache build`, gitignored
├── code/
│   ├── client/        # Kotlin client, Gradle project (run ./gradlew here)
│   │   └── src/main/kotlin/
│   │       ├── com/darkan/    # Bot framework and harness control server
│   │       ├── com/jagex/     # Game engine
│   │       ├── jaclib/        # Native hardware/memory libs
│   │       ├── jaggl/         # OpenGL bindings (LWJGL core context)
│   │       └── jagtheora/     # Video codec
│   ├── server/        # Darkan lobby and world
│   ├── harness/       # Rust `openrs` harness
│   ├── renderer/      # Rust WebGPU renderer (wgpu, WGSL via naga)
│   ├── games/         # Rust game hosts for the WebSocket frame stream
│   ├── renderdoc/     # RenderDoc capture and equivalence tooling
│   └── web/           # Rust web server and page
└── src/               # English markdowns, start with src/HARNESS.md
```

## Migration Status

### Completed
- 100% of the codebase converted to Kotlin
- All native libraries migrated
- Bot framework fully in Kotlin
- CS2 engine split into a package with a two-level JIT-friendly dispatch

### Remaining Work
Deobfuscation, not migration. The CS2 engine lives in `config/rsinterface/cs2/`;
its 1006 opcode names equal the darkanrs/server `Cs2Op` enum (Jagex's 727 names).

## Historical Context

### RuneScape Client Build #727 (Mid-2012)
- Uses RuneTek 5 engine
- Represents the "EoC pre-release" era of RuneScape
- Original code was heavily obfuscated
- This project has reverse-engineered and deobfuscated the client

### Darkan Server Project
This client is designed to work with the Darkan server emulator, which recreates the 2012 RuneScape server infrastructure. Together they provide a complete 2012-era RuneScape experience.

## Notes for AI Assistants

### When Working on This Project
- Always run full builds after changes
- Be extremely careful with nullability (legacy code is null-heavy)
- Preserve public APIs for compatibility
- The CS2 engine is critical; keep `ClientScriptOpCode` names equal to the server's `Cs2Op`, and never reorder the enum or change its flags
- Don't use git commands (per migration guidelines)

### Understanding the Codebase
- `com.jagex` = Original game engine (deobfuscated)
- `com.darkan` = Custom bot/automation layer
- Most obfuscated names have been renamed to meaningful identifiers
- The code represents mid-2012 Java patterns, now modernized with Kotlin

### Testing & Building
- Full build required after changes: `./gradlew build`
- Main entry point for testing: `com.darkan.bot.BotKt`
- Verify both compilation AND runtime behavior
