package com.rs.game.content.quests.the_grand_tree.objects

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.model.entity.player.Player

fun blockGrandTreeDoors(player: Player): Boolean {
    if (player.grandTreeStage !in STAGE_ESCAPED_PRISON until STAGE_FIND_DACONIA ||
        player.y > 3491 || player.grandTreeAttribs().getB(ATTR_CONFRONTED_GLOUGH_ABOUT_FLEET))
        return false
    player.startConversation {
        npc(Npc.gnomekingsguard, HeadE.CALM_TALK, "I'm afraid that we have orders not to let you in.")
        player(HeadE.CONFUSED, "Orders from who?")
        npc(Npc.gnomekingsguard, HeadE.CALM_TALK, "The head tree guardian, he says you're a spy!")
        player(HeadE.ANGRY, "Glough!")
        npc(Npc.gnomekingsguard, HeadE.SAD_MILD_LOOK_DOWN, "I'm sorry but you'll have to leave.")
    }
    return true
}
