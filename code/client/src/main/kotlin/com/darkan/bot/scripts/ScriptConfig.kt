package com.darkan.bot.scripts

interface ConfigItem<T> {
    val name: String
    val description: String
    var value: T

    fun reset() {}
}

interface ConfigurableScript

interface ConfigVisibilityProvider {
    fun isConfigItemVisible(fieldName: String, item: ConfigItem<*>): Boolean
}

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

class ConfigSection(
    override val name: String,
    override val description: String = "",
    val defaultOpen: Boolean = true
) : ConfigItem<Unit> {
    override var value: Unit = Unit
}

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
