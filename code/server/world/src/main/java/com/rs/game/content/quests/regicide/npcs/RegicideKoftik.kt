package com.rs.game.content.quests.regicide.npcs

import com.rs.game.content.quests.underground_pass.npcs.giveKoftikCloth
import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapRegicideKoftik() {
    onNpcClick(Npc.caveguide7) { (player, npc) -> regicideKoftikDialogue(player, npc) }
}

fun regicideKoftikDialogue(player: Player, npc: NPC) {
    player.startConversation {
        when (npc.id) {
            Npc.npc_972 -> {
                player(CALM_TALK, "Hello Koftik.")
                npc(npc, CALM_TALK, "Hello Adventurer, how's things?")
                player(CALM_TALK, "Not bad, yourself?")
                npc(npc, CALM_TALK, "I'm good... just keeping an eye out.")
            }
            Npc.caveguide2 -> {
                player(CALM_TALK, "Hi Koftik.")
                npc(npc, CALM_TALK, "Hi - I found this cloth amongst the charred remains of some arrows, perhaps you need it?")
                exec { player.giveKoftikCloth() }
            }
            Npc.caveguide7 -> {
                player(CALM_TALK, "Hello Koftik.")
                npc(npc, CALM_TALK, "Hello ${player.displayName}. How's things?")
                val attributes = player.questManager.getAttribs(Quest.REGICIDE)
                if (System.currentTimeMillis() >= attributes.getL("koftikFoodDue")) {
                    player(CALM_TALK, "I got pretty hurt getting here. These traps don't get any easier.")
                    npc(npc, CALM_TALK, "Nope, they don't.")
                    exec {
                        if (System.currentTimeMillis() >= attributes.getL("koftikFoodDue")) {
                            player.inventory.addItemDrop(Obj.bread, 2)
                            player.inventory.addItemDrop(Obj.stew, 1)
                            attributes.setL("koftikFoodDue", System.currentTimeMillis() + 300000L)
                        }
                    }
                    npc(npc, CALM_TALK, "Here eat this, it should help.")
                    player(CALM_TALK, "Thanks Koftik.")
                    npc(npc, CALM_TALK, "No problem.")
                } else {
                    player(CALM_TALK, "These traps don't get any easier. But I'm alright, thanks.")
                    npc(npc, CALM_TALK, "Good to hear.")
                }
                npc(npc, CALM_TALK, "The magi have made all the reparations to the well of voyage... step into it when ready. You will be the first to use it this age.")
                player(CALM_TALK, "Thank you Koftik, see you again soon.")
                npc(npc, CALM_TALK, "I hope so.")
            }
        }
    }
}
