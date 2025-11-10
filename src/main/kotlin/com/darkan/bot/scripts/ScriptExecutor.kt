package com.darkan.bot.scripts

import com.darkan.bot.UI
import com.darkan.bot.random
import com.darkan.bot.scripts.event.Event
import com.darkan.bot.Bot
import java.io.File
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

private object StayloggedInTask: Script() {
    private var nextAfkKeypress = 0L

    override suspend fun loop() {
        if (System.currentTimeMillis() >= nextAfkKeypress) {
            nextAfkKeypress = System.currentTimeMillis() + random(4596, 5529)
            Bot.sendClick()
        }
    }
}

object ScriptExecutor {
    private val _scripts = mutableMapOf<String, ScriptMetadata>()
    val scripts: Map<String, ScriptMetadata> get() = _scripts

    private val activeScripts = ConcurrentHashMap<String, Script>()
    private val eventBus = ConcurrentLinkedDeque<Event>()
    var tick = 0L

    fun mainLogic() {
        tick++
        UI.updateAllXPPerHour()
        try {
            if (activeScripts.isNotEmpty())
                StayloggedInTask.tick()
            while (eventBus.isNotEmpty()) {
                val event = eventBus.poll()
                activeScripts.takeIf { it.isNotEmpty() }?.values?.forEach { it._processEvent(event) }
            }
            activeScripts.values.forEach { it.tick() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pushEvent(event: Event) = eventBus.addLast(event)

    fun activate(script: Script) { activeScripts[script.javaClass.name] = script }

    fun deactivate(script: Script) = activeScripts.remove(script.javaClass.name)

    fun stopAll() = activeScripts.values.toList().forEach { it.stop() }

    private fun registerScript(scriptClass: Class<out Script>) {
        val annotation = scriptClass.getAnnotation(ScriptDescription::class.java)
        if (annotation != null) {
            val metadata = ScriptMetadata(
                scriptClass = scriptClass,
                name = annotation.name,
                version = annotation.version,
                author = annotation.author,
                description = annotation.description
            )
            _scripts[metadata.toString()] = metadata
        }
    }

    fun loadScripts() {
        fun isScriptClass(clazz: Class<*>): Class<out Script>? {
            return if (
                Script::class.java.isAssignableFrom(clazz) &&
                clazz.isAnnotationPresent(ScriptDescription::class.java) &&
                !Modifier.isAbstract(clazz.modifiers)
            ) {
                @Suppress("UNCHECKED_CAST")
                clazz as Class<out Script>
            } else
                null
        }

        val scriptsDir = getScriptsDirectoryFile()

        val scriptsDirectoryScanner = ClassScannerFactory.create(
            path = scriptsDir.absolutePath,
            type = ScannerType.BOTH
        )

        val currentProjectScanner = CurrentProjectScriptsScanner()

        val foundClassPaths = scriptsDirectoryScanner.scan() + currentProjectScanner.scan()
        val scripts = foundClassPaths
            .mapNotNull { isScriptClass(it) }
            .sortedBy { it.getAnnotation(ScriptDescription::class.java).name }

        scripts.forEach(::registerScript)
    }

    private fun getScriptsDirectoryFile(): File {
        val userHome = System.getProperty("user.home")
        val scriptsDir = File(userHome, ".cheddarcheese").resolve("scripts")
        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs()
        }
        return scriptsDir
    }
}

data class ScriptMetadata(
    val scriptClass: Class<out Script>,
    val name: String,
    val version: String,
    val author: String,
    val description: String
) {
    override fun toString(): String = "$name v$version by $author"
}