package com.rs.plugin.events

import com.rs.game.model.entity.player.Player
import com.rs.plugin.handlers.InterfaceOnInterfaceHandler
import com.rs.plugin.handlers.PluginHandler
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap

private const val ANY_TARGET = -1

class IFOnIFEvent(
    val player: Player,
    val fromInterfaceId: Int,
    val fromComponentId: Int,
    val fromSlotId: Int,
    val fromSlotId2: Int,
    val toInterfaceId: Int,
    val toComponentId: Int,
    val toSlotId: Int,
    val toSlotId2: Int,
) : PluginEvent {
    val fromComponent get() = fromInterfaceId shl 16 or fromComponentId

    fun intercepted(): Boolean = INTERCEPTORS[fromComponent]?.any { it(this) } == true

    override fun getMethod(): PluginHandler<out PluginEvent>? =
        find((fromInterfaceId shl 16) + fromComponentId, (toInterfaceId shl 16) + toComponentId) ?: find(fromInterfaceId, toInterfaceId)

    private fun find(fromKey: Int, toKey: Int): InterfaceOnInterfaceHandler? {
        val forward = FROM_HANDLERS[fromKey]
        val mapping = forward ?: TO_HANDLERS[toKey] ?: return null
        val otherInterface = if (forward == null) fromInterfaceId else toInterfaceId
        val otherComponent = if (forward == null) fromComponentId else toComponentId
        return mapping[(otherInterface shl 16) + otherComponent] ?: mapping[otherInterface] ?: mapping[ANY_TARGET]
    }

    companion object {
        private val FROM_HANDLERS = HashMap<Any, MutableMap<Any, InterfaceOnInterfaceHandler>>()
        private val TO_HANDLERS = HashMap<Any, MutableMap<Any, InterfaceOnInterfaceHandler>>()
        private val INTERCEPTORS = Int2ObjectOpenHashMap<MutableList<(IFOnIFEvent) -> Boolean>>()

        fun intercept(fromComponents: IntArray, claim: (IFOnIFEvent) -> Boolean) {
            for (component in fromComponents) (INTERCEPTORS[component] ?: ArrayList<(IFOnIFEvent) -> Boolean>().also { INTERCEPTORS.put(component, it) }).add(claim)
        }

        @JvmStatic
        fun registerMethod(eventType: Class<*>, method: PluginHandler<out PluginEvent>) {
            val handler = method as InterfaceOnInterfaceHandler
            map(handler, false)
            if (handler.isBidirectional) map(handler, true)
        }

        private fun map(handler: InterfaceOnInterfaceHandler, reverse: Boolean) {
            val fromKeys = if (reverse) handler.toKeys else handler.fromKeys
            val toKeys = if (reverse) handler.fromKeys else handler.toKeys
            if (fromKeys != null) fill(FROM_HANDLERS, fromKeys, toKeys, handler)
            else if (toKeys != null) fill(TO_HANDLERS, toKeys, null, handler)
        }

        private fun fill(handlers: MutableMap<Any, MutableMap<Any, InterfaceOnInterfaceHandler>>, keys: Array<Any>, targets: Array<Any>?, handler: InterfaceOnInterfaceHandler) {
            for (key in keys) {
                val mapping = handlers.getOrPut(key) { HashMap() }
                if (targets == null) mapping[ANY_TARGET] = handler
                else targets.forEach { mapping[it] = handler }
            }
        }
    }
}
