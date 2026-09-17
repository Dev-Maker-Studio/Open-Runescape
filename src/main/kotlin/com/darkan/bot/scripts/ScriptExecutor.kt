package com.darkan.bot.scripts

import com.darkan.bot.UI
import com.darkan.bot.formatElapsedTime
import com.darkan.bot.random
import com.darkan.bot.scripts.event.Event
import com.darkan.bot.Bot
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
    @Volatile
    private var registry: Map<String, ScriptMetadata> = emptyMap()
    val scripts: Map<String, ScriptMetadata> get() = registry
    private val reloadListeners = CopyOnWriteArrayList<() -> Unit>()

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

    /** Called on the loading thread after every [loadScripts]; UI listeners must hop to the EDT themselves. */
    fun addReloadListener(listener: () -> Unit) {
        reloadListeners.add(listener)
    }

    @Synchronized
    fun loadScripts() {
        val started = System.nanoTime()
        val scriptsDir = getScriptsDirectory()

        val prebuilt = ClassScannerFactory.create(scriptsDir.absolutePath, ScannerType.BOTH).scan()
            .mapNotNull(::asScriptClass)
            .mapNotNull { ScriptMetadata.of(it) }
        val found = (prebuilt + KotlinScriptScanner(scriptsDir).catalog()).sortedBy { it.name.lowercase() }

        val previous = registry
        found.forEach { metadata ->
            val running = previous[metadata.id]?.runningClass() ?: return@forEach
            if (running !== metadata.scriptClass) metadata.replacedClass = running
        }
        registry = found.associateBy { it.id }

        println("✓ Listed ${registry.size} script(s) in ${(System.nanoTime() - started) / 1_000_000} ms")
        reloadListeners.forEach { listener ->
            try {
                listener()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private fun asScriptClass(clazz: Class<*>): Class<out Script>? {
        if (!Script::class.java.isAssignableFrom(clazz) || Modifier.isAbstract(clazz.modifiers)) return null
        @Suppress("UNCHECKED_CAST")
        return clazz as Class<out Script>
    }
}

class ScriptMetadata(
    val id: String,
    val className: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val category: ScriptCategory = ScriptCategory.OTHER,
    private val declaredConfigurable: Boolean,
    scriptClass: Class<out Script>? = null,
    private val compile: () -> Class<out Script>,
) {
    @Volatile
    var scriptClass: Class<out Script>? = scriptClass
        private set

    @Volatile
    internal var replacedClass: Class<out Script>? = null

    val loaded: Boolean get() = scriptClass != null

    val configurable: Boolean
        get() = scriptClass?.let { ConfigurableScript::class.java.isAssignableFrom(it) } ?: declaredConfigurable

    val running: Boolean get() = runningClass() != null

    fun runningClass(): Class<out Script>? =
        listOfNotNull(scriptClass, replacedClass).firstOrNull { ScriptExecutor.isScriptRunning(it) }

    fun load(): Class<out Script> {
        scriptClass?.let { return it }
        synchronized(this) {
            scriptClass?.let { return it }
            return compile().also { scriptClass = it }
        }
    }

    override fun toString(): String = "$name v$version by $author"

    companion object {
        fun of(scriptClass: Class<out Script>, id: String = scriptClass.name): ScriptMetadata? {
            val annotation = scriptClass.getAnnotation(ScriptDescription::class.java) ?: return null
            if (!annotation.visible) return null
            return ScriptMetadata(
                id = id,
                className = scriptClass.simpleName,
                name = annotation.name,
                version = annotation.version,
                author = annotation.author,
                description = annotation.description,
                category = annotation.category,
                declaredConfigurable = ConfigurableScript::class.java.isAssignableFrom(scriptClass),
                scriptClass = scriptClass,
            ) { scriptClass }
        }
    }
}
