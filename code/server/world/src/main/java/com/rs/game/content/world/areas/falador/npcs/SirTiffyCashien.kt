package com.rs.game.content.world.areas.falador.npcs

import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.npc.NPC
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.utils.shop.ShopsHandler
import org.darkan.core.gameval.gen.Npc

@ServerStartupEvent
fun mapSirTiffyCashien() {
    instantiateNpc(Npc.rd_teleporter_guy) { id, tile -> NPC(id, tile).apply { setKeepsOwnFacing(true) } }

    onNpcClick(Npc.rd_teleporter_guy, options = arrayOf("Talk-to")) { (player, npc) ->
        player.startConversation {
            if (!Quest.RECRUITMENT_DRIVE.isImplemented) {
                player(HeadE.HAPPY_TALKING, "Hello.")
                npc(npc, HeadE.HAPPY_TALKING, "What ho, ${player.getPronoun("sir", "milady")}. Spiffing day for a walk in the park, what?")
                options {
                    op("Can I buy some armour?") {
                        player(HeadE.CALM_TALK, "Can I buy some armour?")
                        npc(npc, HeadE.HAPPY_TALKING, "Of course, dear ${player.getPronoun("boy", "gal")}.")
                        exec { ShopsHandler.openShop(player, "initiate_rank_armory") }
                    }
                    op("Goodbye.") {
                        player(HeadE.CALM_TALK, "Well, see you around, Tiffy.")
                        npc(npc, HeadE.HAPPY_TALKING, "Well, see you around, Tiffy.")
                    }
                }
            } else if (player.questManager.getStage(Quest.RECRUITMENT_DRIVE) == 0) {
                player(HeadE.HAPPY_TALKING, "Hello.")
                npc(npc, HeadE.HAPPY_TALKING, "What ho, ${player.getPronoun("sir", "milady")}. Spiffing day for a walk in the park, what?")
                player(HeadE.CONFUSED, "Spiffing?")
                npc(npc, HeadE.HAPPY_TALKING, "Absolutely, top-hole! Well, can't stay and chat all day, dontchaknow! Ta-ta for now!")
                player(HeadE.CONFUSED, "Erm...goodbye.")
            }
        }
    }
}
