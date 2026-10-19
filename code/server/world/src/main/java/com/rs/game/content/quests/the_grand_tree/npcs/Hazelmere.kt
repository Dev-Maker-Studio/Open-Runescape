package com.rs.game.content.quests.the_grand_tree.npcs

import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapHazelmereGrandTree() {
    onNpcClick(Npc.grandtree_hazelmere, options = arrayOf("Talk-to")) { (player, npc) ->
        player.startConversation {
            options {
                if (player.grandTreeStage in STAGE_STARTED..STAGE_HAZELMERE_MESSAGE)
                    op("Talk about The Grand Tree.") { exec { hazelmereGrandTreeDialogue(player, npc) } }
                op("Try to speak to Hazelmere.") {
                    simple("The mage starts to speak but all you hear is:")
                    npc(npc, HeadE.CALM_TALK, "Blah. Blah, blah, blah, blah...blah!")
                }
                op("Never mind.")
            }
        }
    }
}

private fun hazelmereGrandTreeDialogue(player: Player, npc: NPC) {
    player.startConversation {
        when (player.grandTreeStage) {
            STAGE_STARTED -> {
                simple("The mage starts to speak but all you hear is:")
                npc(npc, HeadE.CALM_TALK, "Blah. Blah, blah, blah, blah...blah!")
                if (!player.inventory.containsOneItem(Obj.grandtree_barksample)) {
                    simple("You need the bark sample from King Narnode before Hazelmere can help.")
                } else {
                    simple("You give the bark sample to Hazelmere.")
                    simple("The mage carefully examines the sample.") { player.inventory.deleteItem(Obj.grandtree_barksample, 1) }
                    npc(npc, HeadE.CALM_TALK, "Blah, blah...Daconia...blah, blah.")
                    player(HeadE.CONFUSED, "Can you write this down and I'll try and translate it?")
                    npc(npc, HeadE.CONFUSED, "Blah, blah?")
                    simple("You make a writing motion. The mage scribbles something down on a scroll.")
                    simple("Hazelmere has given you the scroll.") {
                        player.inventory.addItem(Obj.grandtree_scroll)
                        player.setGrandTreeStage(STAGE_HAZELMERE_MESSAGE)
                    }
                }
            }
            STAGE_HAZELMERE_MESSAGE -> {
                npc(npc, HeadE.CALM_TALK, "Blah, blah....Daconia...blah, blah.")
                if (player.inventory.containsOneItem(Obj.grandtree_scroll)) {
                    simple("You still can't understand Hazelmere. The mage wrote it down for you on a scroll.")
                } else {
                    player(HeadE.CONFUSED, "Can you write this down and I'll try and translate it?")
                    if (!player.inventory.hasFreeSlots()) {
                        npc(npc, HeadE.CONFUSED, "Blah, blah?")
                        simple("The mage seems aware that you don't have enough inventory space to take anything.")
                    } else {
                        npc(npc, HeadE.CONFUSED, "Blah, blah?")
                        simple("You make a writing motion. The mage scribbles something down on a scroll.")
                        simple("Hazelmere has given you the scroll.") { player.inventory.addItem(Obj.grandtree_scroll) }
                    }
                }
            }
        }
    }
}
