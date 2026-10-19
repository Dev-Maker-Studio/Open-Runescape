package com.rs.plugin.events

import com.rs.game.model.entity.player.Player
import com.rs.plugin.handlers.GroundItemClickHandler
import com.rs.plugin.handlers.PluginHandler
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.darkan.core.model.GroundItem

class GroundItemClickEvent(
    val player: Player,
    val item: GroundItem,
    val opNum: Int,
    val isAtItem: Boolean,
) : PluginEvent {
    val option: String = item.definitions.floorOptions.getOrNull(opNum - 1) ?: ""

    override fun getMethod(): PluginHandler<out PluginEvent>? = HANDLERS[item.id]?.get(option)?.takeIf { isAtItem || !it.isCheckDistance }

    operator fun component1() = player

    operator fun component2() = item

    operator fun component3() = option

    companion object {
        private val HANDLERS = Int2ObjectOpenHashMap<MutableMap<String, GroundItemClickHandler>>()

        @JvmStatic
        fun registerMethod(eventType: Class<*>, method: PluginHandler<out PluginEvent>) {
            val handler = method as GroundItemClickHandler
            for (key in handler.keys()) {
                val options = HANDLERS[key as Int] ?: HashMap<String, GroundItemClickHandler>().also { HANDLERS.put(key, it) }
                for (option in handler.options)
                    if (options.put(option, handler) != null) System.err.println("ERROR: Duplicate GroundItemClick methods for key: $key option: $option")
            }
        }
    }
}
