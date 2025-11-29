# KotlinScript (.kts) Support for Bot Scripts

## Overview

The Darkan bot now supports loading and executing KotlinScript (.kts) files as bot scripts. This allows users to write and test scripts without needing to compile them into JARs or class files.

## Features

### 1. **Dynamic Script Loading**
- Scripts are loaded from `~/.darkan/scripts/` directory
- Supports `.kts` files alongside existing `.jar` and `.class` files
- Scripts are automatically compiled and loaded when the "Reload Scripts" button is clicked

### 2. **Auto-Import API**
Scripts automatically have access to all Bot API methods through default imports:
- `com.darkan.bot.Bot` and `Bot.*`
- `com.darkan.bot.scripts.*`
- `com.darkan.bot.scripts.event.*`
- `com.darkan.bot.utils.*`
- `kotlinx.coroutines.*`

### 3. **Hot Reloading**
- Edit scripts in your favorite text editor
- Click "Reload Scripts" in the UI to recompile and load changes
- No need to restart the bot

### 4. **UI Integration**
- New "Reload Scripts" button in the Scripts tab
- New "Open Scripts Folder" button for quick access
- Scripts appear in the same list as compiled scripts
- Full support for start/stop/settings like normal scripts

## Implementation Details

### Key Files

1. **`KotlinScriptLoader.kt`** (src/main/kotlin/com/darkan/bot/scripts/)
   - `BotScript` - Base class for .kts scripts (extends `Script`)
   - `BotScriptConfiguration` - Compilation settings and imports
   - `KotlinScriptScanner` - Discovers and compiles .kts files
   - `getScriptsDirectory()` - Returns the scripts folder location
   - `createSampleScript()` - Creates sample script and README on first run

2. **`ScriptExecutor.kt`** (updated)
   - `loadScripts()` - Now includes KotlinScriptScanner in addition to JAR/class scanners
   - Combines all script sources (built-in, JAR, class, and .kts)

3. **`ScriptSelectionPanel.kt`** (updated)
   - Added "Reload Scripts" button
   - Added "Open Scripts Folder" button
   - Shows success/error dialogs for reload operations

4. **`build.gradle.kts`** (updated)
   - Added Kotlin scripting dependencies:
     - `kotlin-scripting-common`
     - `kotlin-scripting-jvm`
     - `kotlin-scripting-jvm-host`
     - `kotlin-scripting-dependencies`
     - `kotlin-scripting-dependencies-maven`

### Script Structure

All .kts scripts must follow this structure:

```kotlin
@ScriptDescription(
    name = "Script Name",
    version = "1.0",
    author = "Your Name",
    description = "What the script does",
    visible = true,
    category = ScriptCategory.OTHER
)
class MyScript : BotScript() {
    override suspend fun loop() {
        // Your bot logic here
    }
}

// Must return an instance
MyScript()
```

### Compilation Process

1. User clicks "Reload Scripts" or bot starts up
2. `ScriptExecutor.loadScripts()` is called
3. `KotlinScriptScanner.scan()` finds all .kts files in scripts directory
4. For each .kts file:
   - File is read and parsed as script source
   - Script is compiled with `BotScriptConfiguration` (includes imports and classpath)
   - Script is evaluated to get an instance
   - Instance's class is extracted and cached
   - Class is returned as `Class<out Script>`
5. Script classes are registered in ScriptExecutor
6. Scripts appear in UI alongside compiled scripts

### Error Handling

- Compilation errors are logged to console with line numbers
- Failed scripts don't prevent other scripts from loading
- Friendly error dialogs in UI if reload fails completely

## Usage Guide

### For Users

1. **Find the scripts folder:**
   - Click "Open Scripts Folder" in the Scripts tab
   - Or navigate to `~/.darkan/scripts/` manually

2. **Create a script:**
   - Create a new `.kts` file (e.g., `MyScript.kts`)
   - See `SampleScript.kts` for a template
   - See `README.md` in scripts folder for API documentation

3. **Load the script:**
   - Click "Reload Scripts" button
   - Script will appear in the list if compilation succeeds
   - Check logs tab for any errors

4. **Run the script:**
   - Configure settings if needed (click "Settings" button)
   - Click "Start" to run
   - Click "Stop" to stop

### For Developers

#### Adding New API Methods

To make methods available to scripts, add them to Bot.kt. They'll automatically be available through the `Bot.*` import.

#### Modifying Script Configuration

Edit `BotScriptConfiguration` in `KotlinScriptLoader.kt` to:
- Add more default imports
- Change compilation settings
- Add dependencies

#### Debugging Scripts

Scripts print to the same console/log as the main bot. Use:
```kotlin
println("Debug message")
```

#### Testing Script Changes

1. Edit the .kts file
2. Click "Reload Scripts"
3. Start the script
4. Check output in Logs tab

No need to restart the bot!

## Sample Scripts

### Basic Script

```kotlin
@ScriptDescription(
    name = "Position Logger",
    version = "1.0",
    author = "Me",
    description = "Logs player position every 5 seconds"
)
class PositionLogger : BotScript() {
    override suspend fun loop() {
        val pos = getMyPlayerPosition()
        println("Position: (${pos.x}, ${pos.y}, ${pos.z})")
        delay(5000)
    }
}

PositionLogger()
```

### Configurable Script

```kotlin
@ScriptDescription(
    name = "Configurable Miner",
    version = "1.0",
    author = "Me",
    description = "Mines ore and banks"
)
class ConfigurableMiner : BotScript(), ConfigurableScript {
    val oreName = StringConfigItem(
        name = "Ore",
        description = "Ore to mine",
        initialValue = "Iron ore"
    )

    override suspend fun loop() {
        if (inventory.freeSlots() <= 0) {
            openClosestBank()
            delayUntil(5000) { interfaceOpen(762) }
            bankAll()
        } else {
            clickClosestObject(oreName.value, "mine")
            delayUntil(3000) { isAnimating() }
        }
    }
}

ConfigurableMiner()
```

## Troubleshooting

### Script doesn't appear after reload

- Check that file ends with `.kts`
- Check logs tab for compilation errors
- Verify `@ScriptDescription` annotation is present
- Ensure class extends `BotScript`
- Make sure script returns an instance at the end

### Compilation errors

- Check that all required methods/properties are imported
- Verify syntax is valid Kotlin
- See error messages in logs tab (includes line numbers)

### Script stops unexpectedly

- Check for exceptions in logs tab
- Add try-catch blocks for debugging
- Use `println()` to track execution flow

## Benefits

1. **Rapid Development** - Edit and test without recompilation
2. **Easy Distribution** - Share single .kts file instead of JAR
3. **Learning Friendly** - See sample scripts with full API access
4. **Version Control** - Easy to track changes in text files
5. **No Build Tools** - Users don't need Gradle/Maven

## Limitations

1. Scripts are recompiled on each reload (takes a few seconds)
2. Cannot use external dependencies (only what's in bot classpath)
3. Slightly slower startup than pre-compiled classes
4. IDE support varies (some IDEs won't recognize .kts files well)

## Future Enhancements

Possible improvements:
- Script templates in UI
- Built-in script editor
- Dependency resolution for .kts files
- Script marketplace/sharing
- Better error reporting in UI
- Script debugging tools
