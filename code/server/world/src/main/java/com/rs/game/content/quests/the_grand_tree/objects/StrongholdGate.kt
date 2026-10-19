package com.rs.game.content.quests.the_grand_tree.objects

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.model.entity.player.Player

fun blockStrongholdGate(player: Player): Boolean {
    if (player.grandTreeStage !in STAGE_ESCAPED_PRISON until STAGE_FIND_DACONIA)
        return false
    player.startConversation {
        npc(Npc.gnomekingsguard, HeadE.ANGRY, "Halt human!")
        player(HeadE.CONFUSED, "What?, why?")
        npc(Npc.gnomekingsguard, HeadE.CALM_TALK, "By order of the head tree guardian! You can not leave!")
        player(HeadE.FRUSTRATED, "That's crazy! Why?!")
        npc(Npc.gnomekingsguard, HeadE.SKEPTICAL, "Humans are planning to attack our stronghold. You could be a spy!")
        player(HeadE.FRUSTRATED, "That's ridiculous!")
        npc(Npc.gnomekingsguard, HeadE.SAD_MILD_LOOK_DOWN, "Maybe, but that's the orders, I'm sorry.")
    }
    return true
}
