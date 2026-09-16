package com.darkan.bot.scripts

import java.io.File
import java.lang.reflect.Modifier
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val directory = File(args.firstOrNull() ?: getScriptsDirectory().absolutePath)
    val scanner = KotlinScriptScanner(directory)
    val libraries = scanner.libraries()
    val files = scanner.scripts()
    if (files.isEmpty() && libraries.isEmpty()) {
        println("No .kts scripts found in ${directory.absolutePath}")
        exitProcess(1)
    }

    var failures = 0
    for (library in libraries) {
        val path = scanner.relativePath(library)
        val errors = scanner.compileErrors(library)
        if (errors.isEmpty()) {
            println("OK   $path (shared, compiled into every script)")
        } else {
            failures++
            println("FAIL $path (shared, breaks every script)")
            errors.forEach { println("     $it") }
        }
    }

    val loadedFrom = mutableMapOf<String, String>()
    for (file in files) {
        val path = scanner.relativePath(file)
        val scriptClass = scanner.load(file)
        if (scriptClass == null) {
            failures++
            println("FAIL $path")
            continue
        }
        val problems = mutableListOf<String>()
        val description = scriptClass.getAnnotation(ScriptDescription::class.java)
        if (description == null) problems += "missing @ScriptDescription, the client will not list it"
        if (Modifier.isAbstract(scriptClass.modifiers)) problems += "script class is abstract"
        runCatching { scriptClass.getDeclaredConstructor() }
            .onFailure { problems += "no no-arg constructor, the client cannot start it" }
        loadedFrom.put(scriptClass.name, path)?.let { previous ->
            loadedFrom[scriptClass.name] = previous
            problems += "file name collides with $previous, so both compile to ${scriptClass.name}; the client loads only one. Rename one of them."
        }
        if (problems.isEmpty()) {
            println("OK   $path -> ${description?.name} v${description?.version} by ${description?.author} [${description?.category}]")
        } else {
            failures++
            println("FAIL $path")
            problems.forEach { println("     $it") }
        }
    }

    val total = files.size + libraries.size
    println("${total - failures}/$total file(s) passed")
    exitProcess(if (failures == 0) 0 else 1)
}
