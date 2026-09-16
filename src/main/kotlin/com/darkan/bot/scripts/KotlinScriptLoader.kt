package com.darkan.bot.scripts

import java.io.File
import kotlinx.coroutines.runBlocking
import kotlin.script.experimental.api.*
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

@KotlinScript(
    fileExtension = "kts",
    compilationConfiguration = BotScriptCompilationConfiguration::class
)
abstract class BotScript : Script()

class BotScriptCompilationConfiguration : ScriptCompilationConfiguration({
    defaultImports(
        "com.darkan.bot.Bot",
        "com.darkan.bot.Bot.*",
        "com.darkan.bot.scripts.*",
        "com.darkan.bot.scripts.event.*",
        "com.darkan.bot.utils.*",
        "kotlinx.coroutines.*"
    )

    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }

    ide {
        acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }
})

class KotlinScriptScanner(
    private val scriptsDirectory: File
) : AbstractClassScanner.DefaultClassScanner() {

    private val scriptingHost = BasicJvmScriptingHost()
    private val compiledScripts = mutableMapOf<String, Class<out Script>>()

    val libraryDirectory = File(scriptsDirectory, LIBRARY_DIRECTORY)

    fun libraries(): List<File> = ktsFilesIn(libraryDirectory) { true }

    /** Every script in the folder tree: any `.kts` outside `lib/`, so users can group them into folders. */
    fun scripts(): List<File> = ktsFilesIn(scriptsDirectory) { directory -> !directory.isLibrary() }

    fun relativePath(file: File): String =
        file.toRelativeString(scriptsDirectory).replace(File.separatorChar, '/')

    private fun ktsFilesIn(root: File, enter: (File) -> Boolean): List<File> {
        if (!root.isDirectory) return emptyList()
        return root.walkTopDown()
            .onEnter { directory -> directory == root || (!directory.isHidden && directory.name != ".git" && enter(directory)) }
            .filter { it.isFile && it.extension.equals("kts", ignoreCase = true) }
            .sortedBy { relativePath(it) }
            .toList()
    }

    private fun File.isLibrary(): Boolean = canonicalPath == libraryDirectory.canonicalPath

    override fun scan(): List<Class<*>> {
        if (!scriptsDirectory.exists() || !scriptsDirectory.isDirectory) {
            return emptyList()
        }

        val loadedScripts = mutableListOf<Class<*>>()
        val loadedFrom = mutableMapOf<String, String>()

        for (file in scripts()) {
            val path = relativePath(file)
            try {
                val scriptClass = load(file) ?: continue
                val previous = loadedFrom.put(scriptClass.name, path)
                if (previous != null) {
                    loadedFrom[scriptClass.name] = previous
                    println("✗ Skipped KotlinScript $path: its file name collides with $previous, so both compile to ${scriptClass.name}. Rename one of them.")
                    continue
                }
                loadedScripts.add(scriptClass)
                println("✓ Loaded KotlinScript: $path")
            } catch (e: Exception) {
                println("✗ Failed to load KotlinScript $path: ${e.message}")
                e.printStackTrace()
            }
        }

        return loadedScripts
    }

    fun load(file: File): Class<out Script>? {
        try {
            val cachedClass = compiledScripts[file.absolutePath]
            if (cachedClass != null) {
                return cachedClass
            }

            val scriptSource = file.toScriptSource()
            val compilationConfig = configurationImporting(libraries().filter { it.canonicalPath != file.canonicalPath })

            val evalResult = scriptingHost.eval(scriptSource, compilationConfig, null)

            return when (evalResult) {
                is ResultWithDiagnostics.Success -> {
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
                        println("  The file must end with an expression such as MyScript()")
                        null
                    }
                }
                is ResultWithDiagnostics.Failure -> {
                    println("Compilation failed for ${file.name}:")
                    evalResult.reports
                        .filter { it.severity >= ScriptDiagnostic.Severity.WARNING }
                        .forEach { report ->
                            val where = report.sourcePath?.let { File(it).name } ?: file.name
                            println("  ${report.severity}: ${report.message}")
                            val location = report.location
                            if (location != null) {
                                println("    at $where:${location.start.line}:${location.start.col}")
                            } else if (where != file.name) {
                                println("    in $where")
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

    fun compileErrors(file: File): List<String> {
        val result = runBlocking { scriptingHost.compiler(file.toScriptSource(), configurationImporting(libraries())) }
        return result.reports
            .filter { it.severity >= ScriptDiagnostic.Severity.ERROR }
            .map { report ->
                val location = report.location?.let { ":${it.start.line}:${it.start.col}" } ?: ""
                "${file.name}$location: ${report.message}"
            }
    }

    /**
     * Imported scripts are compiled alongside the script that imports them, so a library must not
     * be handed its own import list; that reads as a dependency cycle and fails every compilation.
     */
    private fun configurationImporting(libraries: List<File>): ScriptCompilationConfiguration {
        val base = BotScriptCompilationConfiguration()
        if (libraries.isEmpty()) return base
        val libraryPaths = libraries.map { it.canonicalPath }.toSet()
        val librarySources = libraries.map { it.toScriptSource() }
        return ScriptCompilationConfiguration(base) {
            refineConfiguration {
                beforeCompiling { context ->
                    val path = (context.script as? FileBasedScriptSource)?.file?.canonicalPath
                    if (path != null && path in libraryPaths) {
                        context.compilationConfiguration.asSuccess()
                    } else {
                        context.compilationConfiguration.with { importScripts(librarySources) }.asSuccess()
                    }
                }
            }
        }
    }

    fun clearCache() {
        compiledScripts.clear()
    }

    companion object {
        const val LIBRARY_DIRECTORY = "lib"
    }
}

fun getScriptsDirectory(): File {
    val scriptsDir = File(System.getProperty("user.home"), ".darkan/scripts")
    if (!scriptsDir.exists()) {
        scriptsDir.mkdirs()
    }
    return scriptsDir
}
