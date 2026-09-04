package com.darkan.bot.scripts

import com.darkan.bot.UI
import com.darkan.bot.formatElapsedTime
import com.darkan.bot.random
import com.darkan.bot.scripts.event.Event
import com.darkan.bot.Bot
import java.io.File
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList

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

    private val _activeScripts = ConcurrentHashMap<String, Script>()
    val activeScripts: Collection<Script> get() = _activeScripts.values
    private val eventBus = ConcurrentLinkedDeque<Event>()
    private val eventListeners = CopyOnWriteArrayList<(Event) -> Unit>()
    var tick = 0L

    private val scriptStartTimes = ConcurrentHashMap<String, Long>()

    @Volatile private var pausingScript: Script? = null

    fun mainLogic() {
        tick++
        try {
            if (activeScripts.isNotEmpty())
                StayloggedInTask.tick()
            while (eventBus.isNotEmpty()) {
                try {
                    val event = eventBus.poll()
                    _activeScripts.takeIf { it.isNotEmpty() }?.values?.forEach { it._processEvent(event) }
                } catch(e: Throwable) {
                    e.printStackTrace()
                }
            }
            val currentPausing = pausingScript
            if (currentPausing != null)
                currentPausing.tick()
            else
                _activeScripts.values.forEach { it.tick() }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun pushEvent(event: Event) {
        eventBus.addLast(event)
        if (eventListeners.isNotEmpty()) eventListeners.forEach { listener ->
            try {
                listener(event)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    /** Observes every event as it is pushed, on the pushing thread. Used by the sidebar's var watcher. */
    fun addEventListener(listener: (Event) -> Unit) {
        eventListeners.add(listener)
    }

    fun removeEventListener(listener: (Event) -> Unit) {
        eventListeners.remove(listener)
    }

    fun activate(script: Script) {
        _activeScripts[script.javaClass.name] = script
        scriptStartTimes[script.javaClass.name] = System.currentTimeMillis()
    }

    fun deactivate(script: Script) {
        _activeScripts.remove(script.javaClass.name)
        scriptStartTimes.remove(script.javaClass.name)
    }

    fun isScriptRunning(scriptClass: Class<*>): Boolean { return _activeScripts.containsKey(scriptClass.name) }

    fun getScriptInstance(clazz: Class<*>): Script? { return _activeScripts[clazz.name] }

    fun deactivate(scriptClass: Class<*>) {
        val instance = _activeScripts[scriptClass.name]
        if (instance != null)
            instance.stop()
    }

    fun stopAll() = _activeScripts.values.toList().forEach { it.stop() }

    fun getScriptRuntimeFormatted(scriptClass: Class<*>): String? {
        val start = scriptStartTimes[scriptClass.name] ?: return null
        val elapsedMillis = System.currentTimeMillis() - start
        val totalSeconds = elapsedMillis / 1000
        val hrs = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        return "%02d:%02d:%02d".format(hrs, mins, secs)
    }

    fun pauseOthers(script: Script): Boolean {
        return if (pausingScript == null || pausingScript == script) {
            pausingScript = script
            true
        } else false
    }

    fun resumeOthers(script: Script): Boolean {
        return if (pausingScript == script) {
            pausingScript = null
            true
        } else false
    }

    fun isPaused(): Boolean = pausingScript != null

    private fun clearScripts() {
        _scripts.clear()
    }

    private fun registerScript(scriptClass: Class<out Script>) {
        val annotation = scriptClass.getAnnotation(ScriptDescription::class.java)
        if (annotation != null && annotation.visible) {
            val metadata = ScriptMetadata(
                scriptClass = scriptClass,
                name = annotation.name,
                version = annotation.version,
                author = annotation.author,
                description = annotation.description,
                category = annotation.category
            )
            _scripts[metadata.toString()] = metadata
        }
    }

    fun loadScripts() {
        println("🔍 ScriptExecutor.loadScripts() called")

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

        // Scan for compiled classes (JAR and .class files)
        val scriptsDirectoryScanner = ClassScannerFactory.create(
            path = scriptsDir.absolutePath,
            type = ScannerType.BOTH
        )

        // Scan for current project scripts
        val currentProjectScanner = CurrentProjectScriptsScanner()

        // Scan for .kts files
        val kotlinScriptScanner = KotlinScriptScanner(scriptsDir)

        // Combine all found scripts
        val foundClassPaths = scriptsDirectoryScanner.scan() +
                             currentProjectScanner.scan() +
                             kotlinScriptScanner.scan()

        val scripts = foundClassPaths
            .mapNotNull { isScriptClass(it) }
            .sortedBy { it.getAnnotation(ScriptDescription::class.java).name }

        clearScripts()
        scripts.forEach(::registerScript)

        println("✓ Loaded ${scripts.size} script(s)")
    }

    private fun getScriptsDirectoryFile(): File {
        return getScriptsDirectory()
    }
}

data class ScriptMetadata(
    val scriptClass: Class<out Script>,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val category: ScriptCategory = ScriptCategory.OTHER
) {
    override fun toString(): String = "$name v$version by $author"
}
