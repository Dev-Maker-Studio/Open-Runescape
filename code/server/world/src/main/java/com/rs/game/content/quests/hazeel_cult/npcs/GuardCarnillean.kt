package com.rs.game.content.quests.hazeel_cult.npcs

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapCarnilleanGuard() {
    onNpcClick(Npc.npc_887, Npc.hazeelcult_guard) { (player, npc) ->
        player.startConversation {
            player(HeadE.CALM_TALK, "Hello.")
            if (player.isCultSide() && player.questManager.getStage(Quest.HAZEEL_CULT) >= STAGE_BRANCH_PROGRESS)
                npc(npc, HeadE.FRUSTRATED, "The sarge is furious! Those loony cultists have been back without me noticing!")
            else
                npc(npc, HeadE.HAPPY_TALKING, "I hear you're after that loony cult. It always gladdens me when civilians assist the law.")
        }
    }
}
