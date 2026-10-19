package com.rs.plugin.events

import com.rs.game.model.entity.player.Player
import com.rs.plugin.handlers.InterfaceOnGroundItemHandler
import com.rs.plugin.handlers.PluginHandler
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.darkan.core.model.GroundItem

private const val ANY_ITEM = -1

class IFOnGroundItemEvent(
    val player: Player,
    val interfaceId: Int,
    val componentId: Int,
    val slotId: Int,
    val item: GroundItem,
) : PluginEvent {
    override fun getMethod(): PluginHandler<out PluginEvent>? = find(COMPONENT_HANDLERS[interfaceId shl 16 or componentId]) ?: find(INTERFACE_HANDLERS[interfaceId])

    private fun find(byItem: Int2ObjectOpenHashMap<InterfaceOnGroundItemHandler>?) = byItem?.let { it[item.id] ?: it[ANY_ITEM] }

    companion object {
        private val COMPONENT_HANDLERS = Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<InterfaceOnGroundItemHandler>>()
        private val INTERFACE_HANDLERS = Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<InterfaceOnGroundItemHandler>>()

        @JvmStatic
        fun registerMethod(eventType: Class<*>, method: PluginHandler<out PluginEvent>) {
            val handler = method as InterfaceOnGroundItemHandler
            handler.fromComponents.forEach { put(COMPONENT_HANDLERS, it, handler) }
            handler.fromInterfaceIds.forEach { put(INTERFACE_HANDLERS, it, handler) }
        }

        private fun put(handlers: Int2ObjectOpenHashMap<Int2ObjectOpenHashMap<InterfaceOnGroundItemHandler>>, key: Int, handler: InterfaceOnGroundItemHandler) {
            val byItem = handlers[key] ?: Int2ObjectOpenHashMap<InterfaceOnGroundItemHandler>().also { handlers.put(key, it) }
            for (itemId in handler.itemIds ?: intArrayOf(ANY_ITEM))
                if (byItem.put(itemId, handler) != null) System.err.println("ERROR: Duplicate IFOnGroundItem methods for key: $key item: $itemId")
        }
    }
}
