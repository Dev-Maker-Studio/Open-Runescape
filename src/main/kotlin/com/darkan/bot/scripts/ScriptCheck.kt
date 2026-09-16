package com.darkan.bot.scripts

import java.io.File
import java.lang.reflect.Modifier
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val directory = File(args.firstOrNull() ?: getScriptsDirectory().absolutePath)
    val files = directory.listFiles { file -> file.extension.equals("kts", ignoreCase = true) }
        ?.sortedBy { it.name }
        .orEmpty()
    if (files.isEmpty()) {
        println("No .kts scripts found in ${directory.absolutePath}")
        exitProcess(1)
    }

    val scanner = KotlinScriptScanner(directory)
    var failures = 0
    for (library in scanner.libraries()) {
        val errors = scanner.compileErrors(library)
        if (errors.isEmpty()) {
            println("OK   ${library.relativeTo(directory).path} (shared, compiled into every script)")
        } else {
            failures++
            println("FAIL ${library.relativeTo(directory).path} (shared, breaks every script)")
            errors.forEach { println("     $it") }
        }
    }
    for (file in files) {
        val scriptClass = scanner.load(file)
        if (scriptClass == null) {
            failures++
            println("FAIL ${file.name}")
            continue
        }
        val problems = mutableListOf<String>()
        val description = scriptClass.getAnnotation(ScriptDescription::class.java)
        if (description == null) problems += "missing @ScriptDescription, the client will not list it"
        if (Modifier.isAbstract(scriptClass.modifiers)) problems += "script class is abstract"
        runCatching { scriptClass.getDeclaredConstructor() }
            .onFailure { problems += "no no-arg constructor, the client cannot start it" }
        if (problems.isEmpty()) {
            println("OK   ${file.name} -> ${description?.name} v${description?.version} by ${description?.author} [${description?.category}]")
        } else {
            failures++
            println("FAIL ${file.name}")
            problems.forEach { println("     $it") }
        }
    }
    println("${files.size + scanner.libraries().size - failures}/${files.size + scanner.libraries().size} file(s) passed")
    exitProcess(if (failures == 0) 0 else 1)
}
