package com.rs.game.content.transportation

import com.rs.game.content.quests.the_grand_tree.npcs.captainErrdoDialogue
import com.rs.game.content.quests.the_grand_tree.npcs.confirmGrandTreeQuestFlight
import com.rs.game.content.quests.the_grand_tree.npcs.finishGrandTreeQuestFlight
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onButtonClick
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Varp
import world.gregs.voidps.type.Tile

private val GRAND_TREE_QUEST_CRASH = Tile.of(2918, 3057, 0)

private val GLIDERS = arrayOf(
    Tile.of(3, 38, 54, 32, 47),
    Tile.of(0, 44, 54, 29, 42),
    Tile.of(0, 51, 53, 57, 38),
    Tile.of(0, 51, 50, 20, 11),
    Tile.of(0, 46, 46, 29, 25),
    Tile.of(0, 39, 46, 53, 27),
    Tile.of(0, 39, 49, 0, 55)
)

private val CONFIGS = arrayOf(
    intArrayOf(-1, 1, 3, 4, 7, 10, 12),
    intArrayOf(-1, 2, -1, 5, 6, 11, 13)
)

@ServerStartupEvent
fun mapGnomeGliders() {
    onButtonClick(Interface.interface_138) { e ->
        val player = e.player
        if (player.tempAttribs.getB("using_carrier")) return@onButtonClick
        when (e.component) {
            Component.interface_138.component_138_19 -> sendGlider(player, 6, false)
            Component.interface_138.component_138_22 -> {
                val questFlight = confirmGrandTreeQuestFlight(player, Runnable {
                    sendGlider(player, 4, false, GRAND_TREE_QUEST_CRASH) { finishGrandTreeQuestFlight(player) }
                })
                if (!questFlight) sendGlider(player, 4, false)
            }
            Component.interface_138.component_138_24 -> sendGlider(player, 1, false)
            Component.interface_138.component_138_25 -> sendGlider(player, 2, false)
            Component.interface_138.component_138_26 -> sendGlider(player, 3, false)
            Component.interface_138.component_138_27 -> sendGlider(player, 5, false)
        }
    }

    onNpcClick(Npc.gnormadium_avlafrim, Npc.pilot_al_kharid, Npc.pilot_white_wolf, Npc.npc_3811, Npc.pilot_karamja, Npc.glo3_bailey) { (player, npc) ->
        when (npc.id) {
            Npc.npc_3811 -> {
                captainErrdoDialogue(player, npc)
                return@onNpcClick
            }
            Npc.pilot_al_kharid -> sendGlider(player, 3, true)
            Npc.pilot_white_wolf -> sendGlider(player, 1, true)
            Npc.pilot_karamja -> sendGlider(player, 4, true)
            Npc.gnormadium_avlafrim -> sendGlider(player, 5, true)
            Npc.glo3_bailey -> sendGlider(player, 6, true)
        }
        player.interfaceManager.sendInterface(Interface.interface_138)
    }
}

private fun sendGlider(
    player: Player,
    index: Int,
    isReturning: Boolean,
    destination: Tile = GLIDERS[if (isReturning) 0 else index],
    onArrival: (() -> Unit)? = null
) {
    if (!player.isQuestComplete(Quest.GRAND_TREE) && onArrival == null) {
        player.sendMessage("You must complete The Grand Tree before using the gnome gliders.")
        return
    }
    player.vars.setVar(Varp.varp_153, CONFIGS[if (isReturning) 1 else 0][index])
    player.tempAttribs.setB("using_carrier", true)
    FadingScreen.fade(player, 3) {
        player.useStairs(-1, destination, 0, 2)
        player.closeInterfaces()
        player.tempAttribs.removeB("using_carrier")
        player.vars.setVar(Varp.varp_153, 0)
        onArrival?.invoke()
    }
}
