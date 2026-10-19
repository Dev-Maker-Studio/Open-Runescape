package com.rs.game.content.quests.hazeel_cult.npcs

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapPhilipeCarnillean() {
    onNpcClick(Npc.npc_888, Npc.carni_philipe_hazeelcultquest, Npc.carni_philipe_follower) { (player, npc) ->
        val stage = player.questManager.getStage(Quest.HAZEEL_CULT)
        player.startConversation {
            when {
                player.isCultSide() && stage >= STAGE_BRANCH_PROGRESS -> {
                    simple("The boy looks very upset.")
                    npc(npc, HeadE.SAD, "My dog, Scruffy, is dead. I didn't want my dinner, so I gave it to him, and he died.")
                    player(HeadE.WORRIED, "Oh dear, how sad.")
                }
                stage == STAGE_UNSTARTED || stage == STAGE_COMPLETE -> {
                    player(HeadE.CALM_TALK, "Hello.")
                    npc(npc, HeadE.ANGRY, "Toys! I want TOYS!")
                    player(HeadE.CALM_TALK, "Sorry, I don't have any.")
                    npc(npc, HeadE.ANGRY, "Sweets! Gimme sweets!")
                    player(HeadE.CALM_TALK, "Nope, sorry, I don't have any sweets either.")
                    npc(npc, HeadE.ANGRY, "I hate you! HATE YOU!")
                }
                else -> {
                    npc(npc, HeadE.HAPPY_TALKING, "Daddy says you're going to get his armour back from the bad men who stole it.")
                    player(HeadE.CALM_TALK, "Yup, that's the plan.")
                    npc(npc, HeadE.HAPPY_TALKING, "Can I watch you kill them?")
                    player(HeadE.FRUSTRATED, "No!")
                }
            }
        }
    }
}
