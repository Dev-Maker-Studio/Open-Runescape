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
fun mapAnitaGrandTree() {
    onNpcClick(Npc.grandtree_anita, options = arrayOf("Talk-to")) { (player, npc) ->
        player.startConversation {
            options {
                if ((player.grandTreeStage == STAGE_LUMBER_ORDER && player.grandTreeAttribs().getB(ATTR_CHARLIE_REVEALED_KEY)) ||
                    player.grandTreeStage in STAGE_GLOUGHS_KEY..STAGE_INVASION_PLANS)
                    op("Talk about The Grand Tree.") { exec { anitaGrandTreeDialogue(player, npc) } }
                op("Say hello.") { simple("Anita is busy cleaning.") }
                op("Never mind.")
            }
        }
    }
}

private fun anitaGrandTreeDialogue(player: Player, npc: NPC) {
    if (player.inventory.containsOneItem(Obj.grandtree_gloughskey)) {
        player.sendMessage("You already have Glough's key.")
        return
    }
    player.startConversation {
        player(HeadE.HAPPY_TALKING, "Hello there.")
        npc(npc, HeadE.HAPPY_TALKING, "Oh hello, I've seen you with the King.")
        player(HeadE.CALM_TALK, "Yes, I'm helping him with a problem.")
        npc(npc, HeadE.HAPPY_TALKING, "You must know my boyfriend Glough then?")
        player(HeadE.HAPPY_TALKING, "Indeed!")
        npc(npc, HeadE.CALM_TALK, "Could you do me a favour?")
        player(HeadE.CALM_TALK, "I suppose so.")
        npc(npc, HeadE.CALM_TALK, "Please give this key to Glough, he left it here last night.")
        if (!player.inventory.hasFreeSlots()) {
            npc(npc, HeadE.WORRIED, "You can't carry anymore. Come back when you have some space.")
        } else {
            simple("Anita gives you a key.")
            npc(npc, HeadE.HAPPY_TALKING, "Thanks a lot.")
            player(HeadE.HAPPY_TALKING, "No...thank you!") {
                player.inventory.addItem(Obj.grandtree_gloughskey)
                if (player.grandTreeStage < STAGE_GLOUGHS_KEY)
                    player.setGrandTreeStage(STAGE_GLOUGHS_KEY)
            }
        }
    }
}
