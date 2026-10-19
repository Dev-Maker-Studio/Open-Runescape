package com.rs.game.content.quests.hazeel_cult.npcs

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapClausTheChef() {
    onNpcClick(Npc.npc_886, Npc.carni_chef) { (player, npc) ->
        val stage = player.questManager.getStage(Quest.HAZEEL_CULT)
        player.startConversation {
            when {
                stage == STAGE_UNSTARTED -> npc(npc, HeadE.FRUSTRATED, "Sorry! Can't stop to chat! You would be AMAZED at how many meals this family gets through daily!")
                stage == STAGE_COMPLETE && player.isCultSide() -> {
                    player(HeadE.CALM_TALK, "Hiya.")
                    npc(npc, HeadE.FRUSTRATED, "I hear Sir Ceril doesn't want you around here anymore.")
                    player(HeadE.CALM_TALK, "Sure he doesn't.")
                }
                stage == STAGE_COMPLETE -> {
                    player(HeadE.CALM_TALK, "Hiya.")
                    npc(npc, HeadE.HAPPY_TALKING, "Well hello there adventurer! Are we fit and well?")
                    player(HeadE.HAPPY_TALKING, "Yep, fine thanks.")
                    npc(npc, HeadE.HAPPY_TALKING, "Glad to hear it.")
                }
                player.isCultSide() && stage >= STAGE_FIND_SCROLL -> {
                    if (stage >= STAGE_RETURN_SCROLL) {
                        player(HeadE.CALM_TALK, "Hello there.")
                        npc(npc, HeadE.ANGRY, "Those animals... how could they do that to poor Scruffy? What kind of lowlife would do that?")
                        player(HeadE.FRUSTRATED, "Get over it, it was only a dog.")
                    } else {
                        player(HeadE.CALM_TALK, "Hello Claus.")
                        npc(npc, HeadE.WORRIED, "I... just CAN'T understand it! How? HOW could someone poison my delicious food without me even noticing? I... I just don't understand!")
                        npc(npc, HeadE.SAD, "Oh man... I hope I don't lose my job over this... but HOW? How did they do it? And why would the villain want to kill poor Scruffy anyway? He was such a good dog...")
                        simple("Claus wipes away a tear.")
                        player(HeadE.CALM_TALK, "Yeah. It's a real mystery, huh?")
                    }
                }
                player.isCultSide() && stage >= STAGE_BRANCH_PROGRESS -> {
                    npc(npc, HeadE.CALM_TALK, "Hello there. Caught any of those weird thieves yet?")
                    player(HeadE.CALM_TALK, "Nope. Afraid not.")
                    npc(npc, HeadE.HAPPY_TALKING, "Rather you than me!")
                }
                else -> {
                    npc(npc, HeadE.CALM_TALK, "You're that adventurer the family asked to help get those weird cultists who keep breaking in, right?")
                    player(HeadE.CALM_TALK, "That's me.")
                    npc(npc, HeadE.HAPPY_TALKING, "Good luck with that!")
                }
            }
        }
    }
}
