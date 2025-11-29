package com.darkan.bot.scripts

import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

/**
 * Base class for KotlinScript (.kts) bot scripts
 * Automatically imports all common Bot API classes and utilities
 */
@KotlinScript(
    fileExtension = "kts",
    compilationConfiguration = BotScriptCompilationConfiguration::class
)
abstract class BotScript : Script()

/**
 * Script compilation configuration for .kts files
 * Defines imports, classpath, and other compilation settings
 */
class BotScriptCompilationConfiguration : ScriptCompilationConfiguration({
    // Add all necessary imports automatically
    defaultImports(
        "com.darkan.bot.Bot",
        "com.darkan.bot.Bot.*",
        "com.darkan.bot.scripts.*",
        "com.darkan.bot.scripts.event.*",
        "com.darkan.bot.utils.*",
        "kotlinx.coroutines.*"
    )

    jvm {
        // Use current classpath for script compilation
        dependenciesFromCurrentContext(wholeClasspath = true)
    }

    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
})

/**
 * Scanner for discovering and loading .kts script files
 */
class KotlinScriptScanner(
    private val scriptsDirectory: File
) : AbstractClassScanner.DefaultClassScanner() {

    private val scriptingHost = BasicJvmScriptingHost()
    private val compiledScripts = mutableMapOf<String, Class<out Script>>()

    override fun scan(): List<Class<*>> {
        if (!scriptsDirectory.exists() || !scriptsDirectory.isDirectory) {
            return emptyList()
        }

        val ktsFiles = scriptsDirectory.listFiles { file ->
            file.extension.equals("kts", ignoreCase = true)
        } ?: return emptyList()

        val loadedScripts = mutableListOf<Class<*>>()

        for (file in ktsFiles) {
            try {
                val scriptClass = compileAndLoadScript(file)
                if (scriptClass != null) {
                    loadedScripts.add(scriptClass)
                    println("✓ Loaded KotlinScript: ${file.name}")
                }
            } catch (e: Exception) {
                println("✗ Failed to load KotlinScript ${file.name}: ${e.message}")
                e.printStackTrace()
            }
        }

        return loadedScripts
    }

    private fun compileAndLoadScript(file: File): Class<out Script>? {
        try {
            // Check cache first
            val cachedClass = compiledScripts[file.absolutePath]
            if (cachedClass != null) {
                return cachedClass
            }

            // Create script source from file
            val scriptSource = file.toScriptSource()

            // Use the compilation configuration from the annotation
            val compilationConfig = BotScriptCompilationConfiguration()

            // Evaluate the script
            val evalResult = scriptingHost.eval(scriptSource, compilationConfig, null)

            return when (evalResult) {
                is ResultWithDiagnostics.Success -> {
                    // Get the return value from the script (the instance returned at the end)
                    val returnValue = evalResult.value.returnValue
                    val scriptInstance = when (returnValue) {
                        is ResultValue.Value -> returnValue.value
                        else -> returnValue.scriptInstance
                    }

                    if (scriptInstance != null && scriptInstance is Script) {
                        @Suppress("UNCHECKED_CAST")
                        val scriptClass = scriptInstance::class.java as Class<out Script>
                        compiledScripts[file.absolutePath] = scriptClass
                        scriptClass
                    } else {
                        println("Warning: Script ${file.name} did not produce a valid Script instance")
                        println("  Return value type: ${scriptInstance?.javaClass?.name ?: "null"}")
                        null
                    }
                }
                is ResultWithDiagnostics.Failure -> {
                    println("Compilation failed for ${file.name}:")
                    evalResult.reports.forEach { report ->
                        println("  ${report.severity}: ${report.message}")
                        report.location?.let { location ->
                            println("    at ${location.start.line}:${location.start.col}")
                        }
                    }
                    null
                }
            }
        } catch (e: Exception) {
            println("Error compiling script ${file.name}: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    /**
     * Clears cached compiled scripts (useful for reloading)
     */
    fun clearCache() {
        compiledScripts.clear()
    }
}

/**
 * Extension function to get the scripts directory
 */
fun getScriptsDirectory(): File {
    val userHome = System.getProperty("user.home")
    val scriptsDir = File(userHome, ".darkan/scripts")
    if (!scriptsDir.exists()) {
        scriptsDir.mkdirs()

        // Create a sample script on first run
        createSampleScript(scriptsDir)
    }
    return scriptsDir
}

/**
 * Creates a sample .kts script to help users get started
 */
private fun createSampleScript(scriptsDir: File) {
    val sampleFile = File(scriptsDir, "SampleScript.kts")
    if (!sampleFile.exists()) {
        sampleFile.writeText("""
// Sample KotlinScript Bot Script
// This demonstrates how to create a simple bot script
//
// All Bot API methods are automatically imported and available:
// - Bot.* (all methods like getMyPlayerPosition(), clickObject(), etc.)
// - Script methods (delay(), delayUntil(), waitForEvent(), etc.)
// - Event classes (XPDrop, Chat, etc.)
// - Utilities (Skill, Tile, etc.)

import com.darkan.bot.scripts.ScriptCategory

@ScriptDescription(
    name = "Sample KotlinScript",
    version = "1.0",
    author = "Bot User",
    description = "A sample KotlinScript that demonstrates the API",
    visible = true,
    category = ScriptCategory.OTHER
)
class SampleKotlinScript : BotScript() {

    override fun onStart() {
        println("=== Sample KotlinScript Started ===")
    }

    override suspend fun loop() {
        // Your bot logic goes here
        val pos = getMyPlayerPosition()
        println("Loop tick - Position: (${'$'}{pos.x}, ${'$'}{pos.y}, ${'$'}{pos.z})")

        // Example: Check inventory
        println("Free inventory slots: ${'$'}{inventory.freeSlots()}")

        // Example: Get current skill levels
        println("Combat level: ${'$'}{getLevel(Skill.ATTACK.ordinal)}")

        // Wait 5 seconds before next loop
        delay(5000)
    }

    override fun onStop() {
        println("=== Sample KotlinScript Stopped ===")
    }
}

// Return an instance of the script
SampleKotlinScript()
        """.trimIndent())
        println("Created sample script at: ${sampleFile.absolutePath}")
    }

    // Also create a README
    val readmeFile = File(scriptsDir, "README.md")
    if (!readmeFile.exists()) {
        readmeFile.writeText("""
# Darkan Bot Scripts

This folder contains bot scripts that can be loaded and executed by the Darkan bot.

## Supported Script Types

1. **KotlinScript (.kts files)** - Write scripts directly in Kotlin without compilation
2. **Compiled Classes (.class files)** - Compiled Kotlin/Java classes that extend Script
3. **JAR files (.jar)** - Packaged scripts in JAR format

## Writing KotlinScript Files

KotlinScript files (.kts) are the easiest way to write bot scripts. They are automatically compiled and loaded when you click "Reload Scripts" in the UI.

### Basic Structure

```kotlin
@ScriptDescription(
    name = "My Script",
    version = "1.0",
    author = "Your Name",
    description = "What this script does",
    visible = true,
    category = ScriptCategory.OTHER
)
class MyScript : BotScript() {
    override suspend fun loop() {
        // Your bot logic here
        delay(600) // Wait 600ms between loops
    }
}

// Return instance
MyScript()
```

### Available API Methods

All methods from the Bot object are automatically imported:

- **Movement**: `walkTo(tile)`, `getDistanceTo(tile/npc/object)`, `isWalking()`
- **Inventory**: `inventory.contains(id)`, `inventory.freeSlots()`, `clickItem(name, option)`
- **Banking**: `openClosestBank()`, `bankAll()`, `depositAllExcept(ids)`, `withdrawAllOfItem(name)`
- **Objects**: `clickClosestObject(name, action)`, `getClosestObject(id/name)`
- **NPCs**: `clickNPC(npc, option)`, `getClosestNPC(id/name)`
- **Ground Items**: `takeGroundItem(item)`, `findGroundItem(id/name)`
- **Skills**: `getXp(skillId)`, `getLevel(skillId)`
- **Delays**: `delay(ms)`, `delayUntil(timeout) { condition }`, `delayWhile { condition }`
- **Events**: `waitForXPDrop(skill)`, `waitForChatContaining(type, text)`

### Example: Simple Woodcutting Script

```kotlin
@ScriptDescription(
    name = "Simple Woodcutter",
    version = "1.0",
    author = "Me",
    description = "Cuts trees and banks logs"
)
class SimpleWoodcutter : BotScript() {
    override suspend fun loop() {
        if (inventory.freeSlots() <= 0) {
            openClosestBank()
            delayUntil(5000) { interfaceOpen(762) }
            bankAll()
        } else {
            if (isAnimating()) return
            clickClosestObject("tree", "chop")
            delayUntil(2000) { isAnimating() }
        }
    }
}

SimpleWoodcutter()
```

## Reloading Scripts

1. Click the "Reload Scripts" button in the Bot UI
2. All .kts files in this folder will be recompiled
3. New scripts will appear in the script list

## Script Folder Location

- Default: `~/.darkan/scripts/` (or `%USERPROFILE%\.darkan\scripts\` on Windows)

## Tips

- Use `println()` for debugging output
- Check the logs tab for compilation errors
- Scripts are recompiled on every reload, so you can edit and test quickly
- Add `@ScriptDescription(visible = false)` to hide scripts from the UI during development
        """.trimIndent())
        println("Created README at: ${readmeFile.absolutePath}")
    }
}
