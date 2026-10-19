package com.rs.plugin.events

import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.managers.EmotesManager.Emote
import com.rs.plugin.handlers.PluginHandler

class EmoteEvent(val player: Player, val emote: Emote) : PluginEvent {
    override fun getMethods(): List<PluginHandler<out PluginEvent>> = HANDLERS[emote] ?: emptyList()

    operator fun component1() = player

    operator fun component2() = emote

    companion object {
        private val HANDLERS = HashMap<Emote, MutableList<PluginHandler<out PluginEvent>>>()

        @JvmStatic
        fun registerMethod(eventType: Class<*>, method: PluginHandler<out PluginEvent>) {
            for (key in method.keys())
                HANDLERS.getOrPut(key as Emote) { ArrayList() }.add(method)
        }
    }
}
