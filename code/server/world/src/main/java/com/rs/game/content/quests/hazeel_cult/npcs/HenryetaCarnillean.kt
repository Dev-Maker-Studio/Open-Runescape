package com.rs.game.content.quests.hazeel_cult.npcs

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapHenryetaCarnillean() {
    onNpcClick(Npc.carnillean_wife) { (player, npc) ->
        player.startConversation {
            player(HeadE.CALM_TALK, "Hello madam.")
            when {
                player.isQuestComplete(Quest.HAZEEL_CULT) && player.isCultSide() -> npc(npc, HeadE.ANGRY, "My husband instructed me to have no dealings with an utter scoundrel such as yourself.")
                player.isQuestComplete(Quest.HAZEEL_CULT) -> npc(npc, HeadE.HAPPY_TALKING, "Since you dealt with those horrible hooligan cultists, things have vastly improved for us!")
                player.isCultSide() && player.questManager.getStage(Quest.HAZEEL_CULT) >= STAGE_BRANCH_PROGRESS -> npc(npc, HeadE.SAD, "I am far too depressed to talk right now. Oh...poor Scruffy...")
                else -> npc(npc, HeadE.WORRIED, "Those awful hooligan cultists are ruining my complexion! I just don't feel safe here anymore!")
            }
        }
    }
}
