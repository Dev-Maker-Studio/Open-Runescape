package com.darkan.bot.scripts

interface ConfigItem<T> {
    val name: String
    val description: String
    var value: T

    /** Restores the value the item was declared with. */
    fun reset() {}
}

interface ConfigurableScript

/** Implement on a script to hide settings that do not apply to the current choices; re-evaluated after every change. */
interface ConfigVisibilityProvider {
    fun isConfigItemVisible(fieldName: String, item: ConfigItem<*>): Boolean
}

/** An action rendered as a button beside a config item; [run] returns the new value, or null to leave it unchanged. */
class ConfigAction<T>(val label: String, val run: () -> T?)

interface ActionableConfig<T> {
    var action: ConfigAction<T>?
}

fun <T, I> I.withAction(label: String, run: () -> T?): I where I : ConfigItem<T>, I : ActionableConfig<T> {
    action = ConfigAction(label, run)
    return this
}

class BooleanConfigItem(
    override val name: String,
    override val description: String,
    private val initialValue: Boolean = false
) : ConfigItem<Boolean>, ActionableConfig<Boolean> {
    override var value: Boolean = initialValue
    override var action: ConfigAction<Boolean>? = null

    override fun reset() {
        value = initialValue
    }
}

class IntConfigItem(
    override val name: String,
    override val description: String,
    private val initialValue: Int = 0,
    val min: Int = Int.MIN_VALUE,
    val max: Int = Int.MAX_VALUE
) : ConfigItem<Int>, ActionableConfig<Int> {
    override var value: Int = initialValue
    override var action: ConfigAction<Int>? = null

    override fun reset() {
        value = initialValue
    }
}

class StringConfigItem(
    override val name: String,
    override val description: String,
    private val initialValue: String = ""
) : ConfigItem<String>, ActionableConfig<String> {
    override var value: String = initialValue
    override var action: ConfigAction<String>? = null

    override fun reset() {
        value = initialValue
    }
}

class OptionsConfigItem<T>(
    override val name: String,
    override val description: String,
    val options: Array<T>,
    private val initialValue: T
) : ConfigItem<T>, ActionableConfig<T> {
    override var value: T = initialValue
    override var action: ConfigAction<T>? = null

    override fun reset() {
        value = initialValue
    }
}

class EnumConfigItem<T : Enum<T>>(
    override val name: String,
    override val description: String,
    val enumValues: Array<T>,
    private val initialValue: T
) : ConfigItem<T>, ActionableConfig<T> {
    override var value: T = initialValue
    override var action: ConfigAction<T>? = null

    override fun reset() {
        value = initialValue
    }
}

/** Read-only status line, usually paired with [withAction] to produce the text it shows. */
class InfoDisplayConfigItem(
    override val name: String,
    override val description: String,
    private val initialValue: String = ""
) : ConfigItem<String>, ActionableConfig<String> {
    override var value: String = initialValue
    override var action: ConfigAction<String>? = null

    override fun reset() {
        value = initialValue
    }
}

/**
 * Valueless marker: the items declared after it, up to the next section, render under a
 * collapsible header.
 */
class ConfigSection(
    override val name: String,
    override val description: String = "",
    val defaultOpen: Boolean = true
) : ConfigItem<Unit> {
    override var value: Unit = Unit
}

/**
 * Remembers each script's settings by class name, so they survive stopping, restarting and
 * reloading the scripts from disk.
 */
object ScriptConfigStore {
    private val configCache = mutableMapOf<String, Map<String, Any?>>()

    fun save(script: Any) {
        val values = mutableMapOf<String, Any?>()
        forEachStorableItem(script) { field, item -> values[field.name] = item.value }
        configCache[script.javaClass.name] = values
    }

    fun applyTo(script: Any) {
        val saved = configCache[script.javaClass.name] ?: return
        forEachStorableItem(script) { field, item ->
            if (!saved.containsKey(field.name)) return@forEachStorableItem
            val value = saved[field.name]?.let { item.rebind(it) }
            try {
                @Suppress("UNCHECKED_CAST")
                (item as ConfigItem<Any?>).value = value
            } catch (e: Exception) {
                println("Failed to restore config value for ${field.name}: $e")
            }
        }
    }

    private inline fun forEachStorableItem(script: Any, action: (java.lang.reflect.Field, ConfigItem<*>) -> Unit) {
        script.javaClass.declaredFields.forEach { field ->
            field.isAccessible = true
            val item = field.get(script)
            if (item is ConfigItem<*> && item !is InfoDisplayConfigItem && item !is ConfigSection) action(field, item)
        }
    }

    /**
     * A reloaded script compiles into a fresh classloader, so a cached enum constant belongs to a
     * different class than the one the new script reads back. Match by name against the item's own choices.
     */
    private fun ConfigItem<*>.rebind(saved: Any): Any? {
        if (saved !is Enum<*>) return saved
        val choices = when (this) {
            is EnumConfigItem<*> -> enumValues
            is OptionsConfigItem<*> -> options
            else -> return null
        }
        return choices.firstOrNull { (it as? Enum<*>)?.name == saved.name }
    }
}
