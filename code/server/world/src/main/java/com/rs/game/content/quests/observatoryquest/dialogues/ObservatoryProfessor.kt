package com.rs.game.content.quests.observatoryquest

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapObservatoryProfessor() {
    onNpcClick(Npc.observatory_professor, options = arrayOf("Talk-to")) { (player, npc) ->
        if (talkToObservatoryDomeProfessor(player, npc)) return@onNpcClick
        when (player.getQuestStage(Quest.OBSERVATORY_QUEST)) {
            STAGE_UNSTARTED -> startObservatoryQuest(player)
            STAGE_GATHERING_PLANKS -> gatheringPlanks(player)
            STAGE_GETTING_BRONZE -> gatheringBronze(player)
            STAGE_GETTING_MOLTEN_GLASS -> gatheringMoltenGlass(player)
            STAGE_FINDING_LENS_MOULD -> findingLensMould(player)
            STAGE_MAKING_LENS -> makingLens(player)
            STAGE_TRAVELLING_TO_OBSERVATORY -> player.sendMessage("The professor has gone ahead to the Observatory dome. Best you follow him to see the finished telescope.")
        }
    }
}

private fun startObservatoryQuest(player: Player) {
    if (player.skills.getLevelForXp(Skills.CRAFTING) < 10) {
        player.sendMessage("You need a Crafting level of 10 to help repair the telescope.")
        return
    }
    player.startConversation {
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "What would you like to talk about?")
        options {
            op("Talk about the Observatory Quest.") {
                player(HeadE.CALM_TALK, "Hi, I was...")
                npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Welcome to the magnificent wonder of the Observatory, where wonder is all around you, where the stars can be clutched from the heavens!")
                player(HeadE.CALM_TALK, "Wow, nice intro.")
                npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Why, thanks! How might I help you?")
                options {
                    op("I'm totally lost.") {
                        player(HeadE.CALM_TALK, "I'm totally lost.")
                        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Lost? It must have been those pesky goblins that led you astray.")
                        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Head north-east to find the city of Ardougne.")
                        player(HeadE.CALM_TALK, "I'm sure I'll find the way.")
                        player(HeadE.CALM_TALK, "Thanks for all your help.")
                        npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "No problem at all. Come and visit again!")
                    }
                    op("An Observatory?") {
                        observatoryIntroduction(player)
                    }
                    op("I'm just passing through.") {
                        player(HeadE.CALM_TALK, "I'm just passing through.")
                        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Fair enough. Not everyone is interested in this place, I suppose.")
                    }
                }
            }
            op("Talk about Treasure Trails.") {
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "Perhaps we can talk about that another time.")
            }
        }
    }
}

private fun DialogueBuilder.observatoryIntroduction(player: Player) {
        player(HeadE.CALM_TALK, "An Observatory?")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Of course. We have a superb telescope up in the Observatory, on the hill.")
        npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "A truly marvellous invention, the likes of which you'll never behold again.")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Well, it would be if it worked.")
        npc(Npc.observatory_professor, HeadE.ANGRY, "Don't interrupt!")
        player(HeadE.CONFUSED, "What? It doesn't work?")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Oh, no, no, no. Don't listen to him, he's joking. Aren't you, my FAITHFUL assistant?")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Nope, dead serious. Hasn't been working for a long time.")
        npc(Npc.observatory_professor, HeadE.ANGRY, "Arghhh! Get back to work and stop sticking your nose in!")
        player(HeadE.CONFUSED, "So, it's broken. How come?")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Oh, I suppose there's no use keeping it secret. Did you see those houses outside?")
        player(HeadE.CALM_TALK, "Up on the hill? Yes, I've seen them.")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "It's a horde of goblins.")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Since they moved here they have caused nothing but trouble.")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Last week, my telescope was tampered with.")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Now, parts need replacing before it can be used again. They've even been messing around in the dungeons under this area. Something needs to be done.")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Strikes me that this visitor could help us.")
        npc(Npc.observatory_professor, HeadE.ANGRY, "Stop being so rude.")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "...")
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "Although, he has a point. What do you say?")
        player(HeadE.CONFUSED, "What, me?")
        options {
            op("Sounds interesting, what can I do for you?") {
                player(HeadE.CALM_TALK, "Sounds interesting, what can I do for you?")
                npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Oh, thanks so much.")
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "I shall need some materials for the telescope, so it can be used again.")
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "Let's start with three planks of wood for the telescope base. My assistant will help with obtaining these, won't you?")
                npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "More work for me, then. I suppose I have no say in it.")
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "Go talk to him if you need some advice.")
                player(HeadE.CALM_TALK, "Okay, I'll be right back.") {
                    if (player.getQuestStage(Quest.OBSERVATORY_QUEST) == STAGE_UNSTARTED && player.skills.getLevelForXp(Skills.CRAFTING) >= 10) {
                        player.set(OBSERVATORY_CONSTELLATION_ATTR, -1)
                        player.set(OBSERVATORY_VIEWED_CONSTELLATION_ATTR, false)
                        player.setQuestStage(Quest.OBSERVATORY_QUEST, STAGE_GATHERING_PLANKS)
                    }
                }
            }
            op("Not right now.") {
                player(HeadE.CALM_TALK, "Oh, sorry, I don't have time for that.")
                npc(Npc.observatory_professor, HeadE.SAD, "Oh dear. I really do need some help.")
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "If you see anyone who can help then please send them my way.")
            }
        }
}

private fun gatheringPlanks(player: Player) {
    player.startConversation {
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "What would you like to talk about?")
        options {
            op("Talk about the Observatory Quest.") {
                player(HeadE.HAPPY_TALKING, "Hi again!")
                npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "It's my helping hand, back again.")
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "Do you have the planks yet?")
                if (player.inventory.getAmountOf(Obj.woodplank) < 3) {
                    player(HeadE.CALM_TALK, "Sorry, not yet. Three planks was it?")
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "It was indeed.")
                } else {
                    player(HeadE.HAPPY_TALKING, "Yes, I've got them. Here they are.")
                    npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Well done. This will make a big difference.") {
                        if (player.getQuestStage(Quest.OBSERVATORY_QUEST) == STAGE_GATHERING_PLANKS && player.inventory.containsItem(Obj.woodplank, 3)) {
                            player.inventory.deleteItem(Obj.woodplank, 3)
                            player.setQuestStage(Quest.OBSERVATORY_QUEST, STAGE_GETTING_BRONZE)
                        }
                    }
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "Now, the bronze for the tube. Oh, assistant!")
                    npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "All right, ${player.displayName}. Let me know if you need advice.")
                }
            }
            op("Talk about Treasure Trails.") {
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "Perhaps we can talk about that another time.")
            }
        }
    }
}

private fun gatheringBronze(player: Player) {
    player.startConversation {
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "What would you like to talk about?")
        options {
            op("Talk about the Observatory Quest.") {
                player(HeadE.CALM_TALK, "Hi.")
                npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "The traveller returns!")
                player(HeadE.CALM_TALK, "Still working hard?")
                npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Some of us are.")
                npc(Npc.observatory_professor, HeadE.ANGRY, "What did I tell you about speaking when spoken to?")
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "So, ${player.displayName}, you have the bronze bar?")
                if (!player.inventory.containsItem(Obj.bronze_bar, 1)) {
                    player(HeadE.CALM_TALK, "Not yet.")
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "Please bring me one, then.")
                } else {
                    player(HeadE.HAPPY_TALKING, "I certainly do. Here you go.")
                    npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Great. Now all I need is the lens made.") {
                        if (player.getQuestStage(Quest.OBSERVATORY_QUEST) == STAGE_GETTING_BRONZE && player.inventory.containsItem(Obj.bronze_bar, 1)) {
                            player.inventory.deleteItem(Obj.bronze_bar, 1)
                            player.setQuestStage(Quest.OBSERVATORY_QUEST, STAGE_GETTING_MOLTEN_GLASS)
                        }
                    }
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "Please get me some molten glass.")
                    npc(Npc.observatory_professor, HeadE.ANGRY, "Oi! Lazy bones!")
                    player(HeadE.CONFUSED, "What? I'm not lazy.")
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "Not you! I'm talking to my assistant.")
                    npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "I heard you, old man. ${player.displayName}, come to me if you need advice.")
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "Thank you. Wait a minute, who are you calling 'old'?")
                }
            }
            op("Talk about Treasure Trails.") {
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "Perhaps we can talk about that another time.")
            }
        }
    }
}

private fun gatheringMoltenGlass(player: Player) {
    player.startConversation {
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "What would you like to talk about?")
        options {
            op("Talk about the Observatory Quest.") {
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "How are you getting on finding me some molten glass?")
                if (!player.inventory.containsItem(Obj.molten_glass, 1)) {
                    player(HeadE.CALM_TALK, "Still working on it.")
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "I really need it. Please hurry.")
                } else {
                    player(HeadE.HAPPY_TALKING, "Here it is.")
                    npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Excellent work, let's make the lens.") {
                        if (player.getQuestStage(Quest.OBSERVATORY_QUEST) == STAGE_GETTING_MOLTEN_GLASS && player.inventory.containsItem(Obj.molten_glass, 1)) {
                            player.inventory.deleteItem(Obj.molten_glass, 1)
                            player.setQuestStage(Quest.OBSERVATORY_QUEST, STAGE_FINDING_LENS_MOULD)
                        }
                    }
                    npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "The lens has to have precisely the right dimensions.")
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "Well, obviously, hence why we have a lens mould.")
                    npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "A goblin has stolen our mould, though.")
                    npc(Npc.observatory_professor, HeadE.ANGRY, "Great, just what I need. ${player.displayName}, I don't suppose you could find it?")
                    player(HeadE.CALM_TALK, "I'll have a look - where should I start?")
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "No idea. You could ask my USELESS assistant if you want.")
                    npc(Npc.qip_obs_proffesors_assistant, HeadE.SAD, "Why must I put up with this?")
                }
            }
            op("Talk about Treasure Trails.") {
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "Perhaps we can talk about that another time.")
            }
        }
    }
}

private fun findingLensMould(player: Player) {
    player.startConversation {
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "What would you like to talk about?")
        options {
            op("Talk about the Observatory Quest.") {
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "Did you bring me the mould?")
                if (!player.inventory.containsItem(Obj.lens_mould, 1)) {
                    player(HeadE.CALM_TALK, "Still looking for it.")
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "Please try and find it; again, my assistant may be able to help.")
                } else {
                    player(HeadE.HAPPY_TALKING, "I certainly have. You'll never guess what they were doing with it.")
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "Well, from the smell I'd guess cooking some vile concoction.")
                    player(HeadE.CALM_TALK, "Wow, good guess. Well, here you go.")
                    npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Let me warn you before you hand that over: his last attempt at Crafting blew up, and the mess took me a week to clear away.")
                    player(HeadE.CONFUSED, "Explosion?")
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "Erm, yes. I think in this instance you had probably better do it.")
                    player(HeadE.CALM_TALK, "I suppose it's better I don't ask.")
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "You can use the mould with molten glass to make a new lens.") {
                        if (player.getQuestStage(Quest.OBSERVATORY_QUEST) == STAGE_FINDING_LENS_MOULD && player.inventory.containsItem(Obj.lens_mould, 1)) {
                            player.inventory.addItemDrop(Obj.molten_glass, 1)
                            player.setQuestStage(Quest.OBSERVATORY_QUEST, STAGE_MAKING_LENS)
                        }
                    }
                }
            }
            op("Talk about Treasure Trails.") {
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "Perhaps we can talk about that another time.")
            }
        }
    }
}

private fun makingLens(player: Player) {
    player.startConversation {
        npc(Npc.observatory_professor, HeadE.CALM_TALK, "What would you like to talk about?")
        options {
            op("Talk about the Observatory Quest.") {
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "Is the lens finished?")
                if (!player.inventory.containsItem(Obj.lens, 1)) {
                    player(HeadE.CONFUSED, "How do I make it again?")
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "Use the molten glass with the mould.")
                    player(HeadE.CALM_TALK, "Huh. Simple.")
                } else {
                    player(HeadE.HAPPY_TALKING, "Yes, here it is. You may as well take this mould too.")
                    npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Wonderful, at last I can fix the telescope.")
                    npc(Npc.observatory_professor, HeadE.HAPPY_TALKING, "Would you accompany me to the Observatory? You simply must see the telescope in operation.")
                    player(HeadE.HAPPY_TALKING, "Sounds interesting. Count me in.")
                    npc(Npc.observatory_professor, HeadE.CALM_TALK, "Superb. You'll have to go via the dungeon under the goblin settlement, seeing as the bridge is broken. You'll find stairs up to the Observatory from there.")
                    player(HeadE.CALM_TALK, "Okay. See you there.") {
                        if (player.getQuestStage(Quest.OBSERVATORY_QUEST) == STAGE_MAKING_LENS && player.inventory.containsItem(Obj.lens, 1)) {
                            player.inventory.deleteItem(Obj.lens, 1)
                            player.inventory.deleteItem(Obj.lens_mould, 1)
                            player.setQuestStage(Quest.OBSERVATORY_QUEST, STAGE_TRAVELLING_TO_OBSERVATORY)
                            syncObservatoryProfessor(player)
                            player.sendMessage("The professor has gone ahead to the Observatory dome. Best you follow him to see the finished telescope.")
                        }
                    }
                }
            }
            op("Talk about Treasure Trails.") {
                npc(Npc.observatory_professor, HeadE.CALM_TALK, "Perhaps we can talk about that another time.")
            }
        }
    }
}
