package org.darkan.world.harness

import com.rs.plugin.PluginManager
import com.rs.plugin.annotations.PluginEventHandler
import com.rs.plugin.handlers.PluginHandler
import org.darkan.core.getClasses
import org.darkan.core.getClassesWithAnnotation
import java.lang.reflect.Modifier

/**
 * Replays [PluginManager.loadPlugins]'s registration loop and reports what it would choke on,
 * without the top-level catch that hides the failure.
 */
object PluginLoadProbe {

    data class Failure(val owner: String, val field: String, val error: String)

    data class Result(
        val eventTypes: Int,
        val handlerClasses: Int,
        val registered: Int,
        val failures: List<Failure>,
    )

    fun probe(): Result {
        val eventTypes = getClasses("com.rs.plugin.events")
        val classes = getClassesWithAnnotation("com.rs", PluginEventHandler::class.java)
        val failures = mutableListOf<Failure>()
        val visited = mutableSetOf<java.lang.reflect.Field>()
        var registered = 0
        for (clazz in classes) {
            val fields = try {
                clazz.fields
            } catch (e: Throwable) {
                failures.add(Failure(clazz.name, "<fields>", "${e::class.simpleName}: ${e.message}"))
                continue
            }
            for (field in fields) {
                if (!Modifier.isStatic(field.modifiers) || !visited.add(field)) continue
                if (field.type != PluginHandler::class.java &&
                    field.type.superclass != PluginHandler::class.java
                ) continue
                try {
                    field.isAccessible = true
                    val value = field.get(null)
                        ?: throw NullPointerException("static PluginHandler field is null")
                    @Suppress("UNCHECKED_CAST")
                    (value as PluginHandler<*>).keys()
                    registered++
                } catch (e: Throwable) {
                    failures.add(Failure(clazz.name, field.name, "${e::class.simpleName}: ${e.message}"))
                }
            }
        }
        return Result(eventTypes.size, classes.size, registered, failures)
    }
}
