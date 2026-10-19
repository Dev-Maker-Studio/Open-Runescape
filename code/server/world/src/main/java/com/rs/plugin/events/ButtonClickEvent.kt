package com.rs.plugin.events

import com.rs.game.model.entity.player.Player
import com.rs.plugin.handlers.PluginHandler
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

class ButtonClickEvent(
    val player: Player,
    val interfaceId: Int,
    val componentId: Int,
    val slotId: Int,
    val slotId2: Int,
    val opNum: Int,
) : PluginEvent {
    val component get() = interfaceId shl 16 or componentId

    override fun getMethod(): PluginHandler<out PluginEvent>? = COMPONENT_HANDLERS[component] ?: HANDLERS[interfaceId]

    operator fun component1() = player

    operator fun component2() = interfaceId

    operator fun component3() = componentId

    operator fun component4() = slotId

    operator fun component5() = slotId2

    operator fun component6() = opNum

    companion object {
        private val HANDLERS = HashMap<Any, PluginHandler<out PluginEvent>>()
        private val COMPONENT_HANDLERS = Int2ObjectOpenHashMap<PluginHandler<out PluginEvent>>()

        @JvmStatic
        fun registerMethod(eventType: Class<*>, method: PluginHandler<out PluginEvent>) {
            for (key in method.keys())
                if (HANDLERS.put(key, method) != null) System.err.println("ERROR: Duplicate ButtonClick methods for key: $key")
        }

        fun registerComponents(components: IntArray, method: PluginHandler<out PluginEvent>) {
            for (component in components)
                if (COMPONENT_HANDLERS.put(component, method) != null) System.err.println("ERROR: Duplicate ButtonClick methods for component: $component")
        }
    }
}
