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

class ScriptCompileResult(val scriptClass: Class<out Script>?, val errors: List<String>)

class ScriptLoadException(message: String) : Exception(message)

class KotlinScriptScanner(
    private val scriptsDirectory: File
) {
    val libraryDirectory = File(scriptsDirectory, LIBRARY_DIRECTORY)

    fun libraries(): List<File> = ktsFilesIn(libraryDirectory) { true }

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

    fun catalog(): List<ScriptMetadata> {
        if (!scriptsDirectory.isDirectory) return emptyList()
        val claimed = mutableMapOf<String, String>()
        val libraryStamp = stampOf(libraries())
        return scripts().mapNotNull { file ->
            val path = relativePath(file)
            val previous = claimed.putIfAbsent(file.nameWithoutExtension, path)
            if (previous != null) {
                println("✗ Skipped KotlinScript $path: its file name collides with $previous, so both compile to the same class. Rename one of them.")
                return@mapNotNull null
            }
            try {
                describe(file, path, libraryStamp)
            } catch (e: Exception) {
                println("✗ Failed to read KotlinScript $path: ${e.message}")
                null
            }
        }
    }

    private fun describe(file: File, path: String, libraryStamp: String): ScriptMetadata? {
        return when (val header = ScriptHeader.parse(file.readText())) {
            ScriptHeader.Absent -> {
                println("✗ Skipped KotlinScript $path: no @ScriptDescription")
                null
            }
            is ScriptHeader.Unreadable -> {
                println("… KotlinScript $path: @ScriptDescription must use plain literals to be listed without compiling (${header.reason}); compiling it now")
                load(file)?.let { ScriptMetadata.of(it, id = path) }
            }
            is ScriptHeader.Parsed -> {
                if (!header.visible) return null
                ScriptMetadata(
                    id = path,
                    className = header.className,
                    name = header.name,
                    version = header.version,
                    author = header.author,
                    description = header.description,
                    category = header.category,
                    declaredConfigurable = header.configurable,
                    scriptClass = cached(file, libraryStamp),
                ) {
                    val result = compile(file)
                    result.scriptClass ?: throw ScriptLoadException(
                        result.errors.ifEmpty { listOf("$path did not compile; see the console") }.joinToString("\n")
                    )
                }
            }
        }
    }

    fun load(file: File): Class<out Script>? = compile(file).scriptClass

    fun compile(file: File): ScriptCompileResult = synchronized(lock) {
        val libraries = libraries().filter { it.canonicalPath != file.canonicalPath }
        val stamp = stampOf(listOf(file)) + stampOf(libraries)
        compiledScripts[file.canonicalPath]?.takeIf { it.first == stamp }?.let { return ScriptCompileResult(it.second, emptyList()) }

        val started = System.nanoTime()
        val errors = mutableListOf<String>()
        try {
            val evalResult = scriptingHost.eval(file.toScriptSource(), configurationImporting(libraries), null)
            when (evalResult) {
                is ResultWithDiagnostics.Success -> {
                    val returnValue = evalResult.value.returnValue
                    val scriptInstance = when (returnValue) {
                        is ResultValue.Value -> returnValue.value
                        else -> returnValue.scriptInstance
                    }

                    if (scriptInstance is Script) {
                        @Suppress("UNCHECKED_CAST")
                        val scriptClass = scriptInstance::class.java as Class<out Script>
                        compiledScripts[file.canonicalPath] = stamp to scriptClass
                        println("✓ Compiled KotlinScript ${relativePath(file)} in ${(System.nanoTime() - started) / 1_000_000} ms")
                        return ScriptCompileResult(scriptClass, emptyList())
                    }
                    errors += "${file.name} must end with an expression such as MyScript() (got ${scriptInstance?.javaClass?.name ?: "null"})"
                }
                is ResultWithDiagnostics.Failure -> {
                    evalResult.reports
                        .filter { it.severity >= ScriptDiagnostic.Severity.WARNING }
                        .forEach { report ->
                            val where = report.sourcePath?.let { File(it).name } ?: file.name
                            val location = report.location?.let { ":${it.start.line}:${it.start.col}" } ?: ""
                            if (report.severity >= ScriptDiagnostic.Severity.ERROR) {
                                errors += "$where$location: ${report.message}"
                            }
                            println("  ${report.severity}: ${report.message}")
                            if (location.isNotEmpty() || where != file.name) println("    at $where$location")
                        }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errors += "${file.name}: ${e.message}"
        }
        println("✗ Failed to compile KotlinScript ${relativePath(file)}")
        errors.forEach { println("  $it") }
        ScriptCompileResult(null, errors)
    }

    private fun cached(file: File, libraryStamp: String): Class<out Script>? =
        compiledScripts[file.canonicalPath]?.takeIf { it.first == stampOf(listOf(file)) + libraryStamp }?.second

    private fun stampOf(files: List<File>): String =
        files.joinToString("") { "${it.canonicalPath}:${it.lastModified()}:${it.length()};" }

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

    companion object {
        const val LIBRARY_DIRECTORY = "lib"

        private val lock = Any()
        private val scriptingHost by lazy { BasicJvmScriptingHost() }
        private val compiledScripts = HashMap<String, Pair<String, Class<out Script>>>()
    }
}

fun getScriptsDirectory(): File {
    val scriptsDir = File(System.getProperty("user.home"), ".darkan/scripts")
    if (!scriptsDir.exists()) {
        scriptsDir.mkdirs()
    }
    return scriptsDir
}
