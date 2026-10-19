package com.rs.game.content.quests.underground_pass.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Varbit
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.underground_pass.giveUniqueQuestItem
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapKardiaAndCat() {
    instantiateNpc(Npc.cavewitchcat) { id, tile -> KardiaCat(id, tile) }

    onNpcClick(Npc.cavewitch, Npc.cavewitchcat) { (player, npc) ->
        when (npc.id) {
            Npc.cavewitchcat -> {
                player.giveUniqueQuestItem(Obj.cavewitchcat)
                player.sendMessage("The cat allows you to pick it up.")
            }

            Npc.cavewitch -> player.startConversation {
                if (player.vars.getVarBit(Varbit.upass_gave_cat) == 1) {
                    npc(npc, HAPPY_TALKING, "You found her! My poor little cat.")
                    player(CALM_TALK, "She was wandering outside.")
                    npc(npc, HAPPY_TALKING, "You may look through my chest as a reward.")
                } else {
                    npc(npc, FRUSTRATED, "Go away! I have no time for strangers while my cat is missing.")
                }
            }
        }
    }
}

private class KardiaCat(id: Int, tile: Tile) : NPC(id, tile) {
    override fun withinDistance(player: Player, distance: Int): Boolean {
        val returned = player.vars.getVarBit(Varbit.upass_gave_cat) == 1
        val carrying = player.inventory.containsItem(Obj.cavewitchcat)
        return !returned && !carrying && super.withinDistance(player, distance)
    }
}
