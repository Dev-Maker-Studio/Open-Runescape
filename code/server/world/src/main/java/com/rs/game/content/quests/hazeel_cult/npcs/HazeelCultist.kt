package com.rs.game.content.quests.hazeel_cult.npcs

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapHazeelCultist() {
    onNpcClick(Npc.hazeel_cultist, options = arrayOf("Talk-to")) { (player, npc) ->
        player.startConversation {
            player(HeadE.CALM_TALK, "Hello.")
            when {
                !player.isCultSide() -> npc(npc, HeadE.ANGRY, "You have no place here! Leave now, before someone makes you!")
                player.isQuestComplete(Quest.HAZEEL_CULT) -> npc(npc, HeadE.HAPPY_TALKING, "I bow before you in honour for bringing Lord Hazeel back to this realm!")
                player.questManager.getStage(Quest.HAZEEL_CULT) >= STAGE_RETURN_SCROLL -> npc(npc, HeadE.HAPPY_TALKING, "Since you brought us the scroll, we can now perform the revivification ceremony!")
                player.questManager.getStage(Quest.HAZEEL_CULT) >= STAGE_FIND_SCROLL -> npc(npc, HeadE.CALM_TALK, "Until we retrieve the scroll of Hazeel, we cannot revive our master. You must find it!")
                else -> npc(npc, HeadE.CALM_TALK, "Leave me alone, can't you see I'm busy? We have a lot to do preparing for Lord Hazeel's return.")
            }
        }
    }
}
