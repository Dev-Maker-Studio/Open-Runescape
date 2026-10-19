package com.rs.game.content.quests.underground_pass.npcs

import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.regicide.npcs.scheduleRegicideMessenger
import com.rs.game.content.quests.underground_pass.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player

fun undergroundPassKingLathasDialogue(player: Player, npc: NPC) {
    player.startConversation {
        when (player.getQuestStage(Quest.UNDERGROUND_PASS)) {
            STAGE_UNSTARTED -> {
                player(CALM_TALK, "Good day.")
                npc(npc, CALM_TALK, "And to you adventurer. I assume you're here about King Tyras?")
                options {
                    op("I am. What's our plan?") {
                        player(CALM_TALK, "I am. What's our plan?")
                        npc(npc, CALM_TALK, "The scouts report there is only one way through the mountains: the Underground Pass.")
                        npc(npc, SKEPTICAL, "It is a treacherous place. Few who enter return with their minds intact.")
                        npc(npc, SAD, "A Zamorakian named Iban has made the pass his home. His followers claim he is the son of Zamorak.")
                        npc(npc, CALM_TALK, "I need someone to find a route through to the western lands. Will you undertake this quest?")
                        options {
                            op("Yes, I'll do it.") {
                                if (!Quest.UNDERGROUND_PASS.meetsReqs(player, "to start this quest.")) {
                                    simple("You do not meet the requirements to start Underground Pass.")
                                } else {
                                    questStart(Quest.UNDERGROUND_PASS)
                                    player(HAPPY_TALKING, "Yes, I'll do it.")
                                    npc(npc, HAPPY_TALKING, "Excellent. The entrance lies at the far end of West Ardougne.")
                                    npc(npc, CALM_TALK, "My scout Koftik is waiting there. Speak to him, then find a way through the pass.")
                                    npc(npc, SKEPTICAL, "Take plenty of food and good armour. Return to me once the route is open.")
                                    exec { player.setQuestStage(Quest.UNDERGROUND_PASS, STAGE_FIND_KOFTIK) }
                                }
                            }
                            op("No, I don't think I'm ready.")
                        }
                    }
                    op("I'm just passing through.") {
                        player(CALM_TALK, "I'm just passing through.")
                        npc(npc, CALM_TALK, "Then enjoy your stay in Ardougne.")
                    }
                }
            }

            STAGE_RETURN_TO_LATHAS -> {
                npc(npc, CALM_TALK, "The adventurer returns, any news?")
                player(HAPPY_TALKING, "The Underground Pass is open. I defeated Iban and his minions.")
                npc(npc, HAPPY_TALKING, "You have? That's excellent news! Then the way west is clear?")
                player(SKEPTICAL, "The portal was damaged when Iban fell. It will need repairing.")
                npc(npc, CALM_TALK, "I will send my finest mages at once. You have done more than I could have hoped.")
                npc(npc, HAPPY_TALKING, "Please accept the gratitude of Ardougne.")
                exec {
                    player.completeQuest(Quest.UNDERGROUND_PASS)
                    player.scheduleRegicideMessenger()
                }
            }

            STAGE_COMPLETE -> {
                npc(npc, HAPPY_TALKING, "Once again, thank you. My mages are working to secure the route through the pass.")
                player(CALM_TALK, "I'm glad I could help.")
            }

            else -> {
                npc(npc, CALM_TALK, "How goes your search for a route through the Underground Pass?")
                player(CALM_TALK, "I'm still working my way through it.")
                npc(npc, SKEPTICAL, "Be careful. Iban's influence is said to break even the strongest minds.")
            }
        }
    }
}
