# Darkan Bot - Project Overview

## Project Description

Darkan Bot is a refactored and deobfuscated version of the RuneScape game client from mid-2012 (Client build #727). This project serves as the game client for the Darkan server emulation project, which aims to recreate the 2012 era RuneScape experience.

**Website**: https://darkan.org

## Current State

This codebase has undergone an extensive Java-to-Kotlin migration and is nearing completion:
- **993 Kotlin files** (99.8% migrated)
- **2 Java files remaining** to be migrated:
  - `src/main/java/com/jagex/game/runetek5/config/rsinterface/cs2/CS2Interpreter.java`
  - `src/main/java/com/jagex/game/runetek5/graphics/model/ModelGL.java`

Recent commits show active migration work with the pattern "MORE KOTLIN LMAO", indicating steady progress toward a fully Kotlin codebase.

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
- **Kotlin 2.2.21**: Primary language (99.8% of codebase)
- **Java 17**: Target JVM version, legacy code (0.2% remaining)

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
│   ├── java/          # Legacy Java code (2 files remaining)
│   │   └── com/jagex/
│   └── kotlin/        # Kotlin code (993 files)
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
- 99.8% of codebase converted to Kotlin
- All native libraries migrated
- Bot framework fully in Kotlin
- Most game engine components migrated

### Remaining Work
Two complex files requiring careful migration:
1. **CS2Interpreter.java**: ClientScript 2 interpreter (game scripting engine)
2. **ModelGL.java**: OpenGL 3D model rendering

Both files likely involve complex logic that requires thorough testing after migration.

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
- The migration is nearly complete - only 2 files remain
- Always run full builds to verify Java-Kotlin interop
- Be extremely careful with nullability (legacy code is null-heavy)
- Preserve public APIs for compatibility
- The CS2Interpreter and ModelGL files are complex and critical
- Don't use git commands (per migration guidelines)
- Use the conversion tool in `./tools/` for Java-to-Kotlin conversion

### Understanding the Codebase
- `com.jagex` = Original game engine (deobfuscated)
- `com.darkan` = Custom bot/automation layer
- Most obfuscated names have been renamed to meaningful identifiers
- The code represents mid-2012 Java patterns, now modernized with Kotlin

### Testing & Building
- Full build required after changes: `./gradlew build`
- Main entry point for testing: `com.darkan.bot.BotKt`
- Watch for Java-Kotlin interop issues at boundaries
- Verify both compilation AND runtime behavior
