package com.rs.plugin.events

import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.handlers.NPCClickHandler
import com.rs.plugin.handlers.PluginHandler

private const val GLOBAL = "global"

class NPCClickEvent(
    val player: Player,
    @get:JvmName("getNPC") val npc: NPC,
    val opNum: Int,
    @get:JvmName("isAtNPC") val isAtNPC: Boolean,
) : PluginEvent {
    val option: String = npc.getDefinitions(player).getOption(opNum) ?: ""

    @get:JvmName("getNPCId")
    val npcId get() = npc.id

    fun intercepted() = INTERCEPTORS.any { (it.options.isEmpty() || option in it.options) && it.claim(player, npc) }

    override fun getMethod(): PluginHandler<out PluginEvent>? {
        val keyed = HANDLERS[npc.id] ?: HANDLERS[npc.definitions.name]
        val method = if (keyed == null) optionGlobal(required = true) else keyed[option] ?: keyed[GLOBAL] ?: optionGlobal(required = false)
        return method?.takeIf { isAtNPC || !it.isCheckDistance }
    }

    private fun optionGlobal(required: Boolean): NPCClickHandler? {
        val global = HANDLERS[option] ?: return null
        return global[GLOBAL] ?: if (required) throw RuntimeException("No global NPCClick method for option: $option") else null
    }

    operator fun component1() = player

    operator fun component2() = npc

    operator fun component3() = option

    private class Interceptor(val options: Set<String>, val claim: (Player, NPC) -> Boolean)

    companion object {
        private val HANDLERS = HashMap<Any, MutableMap<String, NPCClickHandler>>()
        private val INTERCEPTORS = ArrayList<Interceptor>()

        fun intercept(options: Set<String>, claim: (Player, NPC) -> Boolean) {
            INTERCEPTORS += Interceptor(options, claim)
        }

        @JvmStatic
        fun registerMethod(eventType: Class<*>, method: PluginHandler<out PluginEvent>) {
            val handler = method as NPCClickHandler
            val options = handler.options
            val keys = handler.keys()
            when {
                options.isNullOrEmpty() -> keys.forEach { put(it, GLOBAL, handler, "global NPCClick methods for key: $it") }
                keys.isNullOrEmpty() || keys[0] == null -> options.forEach { put(it, GLOBAL, handler, "global NPCClick option methods for key: $it") }
                else -> for (option in options) keys.forEach { put(it, option, handler, "NPCClick option methods for key: $it option: $option method: $method") }
            }
        }

        private fun put(key: Any, option: String, handler: NPCClickHandler, duplicate: String) {
            if (HANDLERS.getOrPut(key) { HashMap() }.put(option, handler) != null) System.err.println("ERROR: Duplicate $duplicate")
        }
    }
}
