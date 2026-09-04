# Darkan Bot - Project Overview

## Project Description

Darkan Bot is a refactored and deobfuscated version of the RuneScape game client from mid-2012 (Client build #727). This project serves as the game client for the Darkan server emulation project, which aims to recreate the 2012 era RuneScape experience.

**Website**: https://darkan.org

## Current State

The Java-to-Kotlin migration is **complete**. `src/main/java/` no longer exists;
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
./gradlew build       # Build and create shadow JAR
./gradlew shadowJar   # Create fat JAR: build/libs/darkan-client-shaded-2.0.0.jar
./gradlew run         # Run the client
```

### Output
- Standard JAR: `build/libs/darkan-bot-2.0.0.jar`
- Shadow JAR: `build/libs/darkan-client-shaded-2.0.0.jar`

## Key Features

### 1. Bot Scripting Framework
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
darkan-bot/
├── src/main/
│   └── kotlin/        # All source (100% Kotlin)
│       ├── com/
│       │   ├── darkan/    # Bot framework
│       │   └── jagex/     # Game engine
│       ├── jaclib/        # Native hardware/memory libs
│       ├── jaggl/         # OpenGL bindings
│       └── jagtheora/     # Video codec
├── build.gradle.kts   # Build configuration
└── README.md          # Project readme

```

## Migration Status

### Completed
- 100% of the codebase converted to Kotlin
- All native libraries migrated
- Bot framework fully in Kotlin
- CS2 engine split into a package with a two-level JIT-friendly dispatch

### Remaining Work
Deobfuscation, not migration. The CS2 engine has been refactored into
`config/rsinterface/cs2/` with an audit harness in `tools/cs2/` — see
`tools/cs2/README.md`. 316 of its 1006 opcodes are still unidentified and keep
their `instrNNNN` names deliberately.

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
- The CS2 engine is critical; run `python3 tools/cs2/verify.py` after touching it
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
