package com.rs.game.content.quests.deserttreasure

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE.*
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapAsgarniaSmithDialogues() {
    onNpcClick(Npc.npc_1918) { (player) -> asgarniaSmithDialogue(player) }
}

private fun asgarniaSmithDialogue(player: Player) {
    player.startConversation {
        when (player.getQuestStage(Quest.DESERT_TREASURE)) {
            STAGE_UNSTARTED -> startDesertTreasure(player)
            STAGE_TERRY_BALANDO -> waitingForTerry(player)
            STAGE_RETURN_TO_ASGARNIA -> returningWithTranslation(player)
            STAGE_MIRROR_MIRROR -> treasureHunt(player)
            STAGE_DIAMONDS_OF_AZZANADRA -> {
                if (DesertTreasureDiamonds.hasAllDiamonds(player))
                    allFourDiamondsAsgarnia(player)
            }
            STAGE_IFTAH_YA_SIMSIM -> allFourDiamondsAsgarnia(player)
            STAGE_RETURN_TO_JALDRAOCHT -> diamondsPlacedAsgarnia(player)
        }
    }
}

private fun DialogueBuilder.startDesertTreasure(player: Player) {
    player(CALM_TALK, "Hello there.")
    npc(Npc.npc_1918, SAD, "Howdy stranger. What brings you out to these parts?")

    label("mainOptions")
    options {
        op("What are you doing here?") {
            player(CONFUSED, "Nothing much - What are you doing here? It doesn't seem like there's much to do out here in the desert!")
            npc(Npc.npc_1918, CALM_TALK, "Well, that's where you'd be wrong. I work for the Archaeological Society of Varrock, and have been excavating around here recently.")
            npc(Npc.npc_1918, CONFUSED, "Was there something you specifically wanted?")
            goto("mainOptions")
        }

        op("Do you have any quests?") {
            player(CALM_TALK, "Do you have any quests? Call it a hunch, but you look like the type of man who might...")
            npc(Npc.npc_1918, CALM_TALK, "Well, it's funny you should say that. I'm not sure if I would really call it a quest as such, but I found this ancient stone tablet in one of my excavations, and it would really help me out if you could go and take it back to the Varrock Dig Site for me and get it examined.")
            npc(Npc.npc_1918, CALM_TALK, "It's very old, and I don't recognise any of the inscriptions on it.")

            options {
                op("Not right now") {
                    player(CALM_TALK, "No thanks. Playing delivery ${if (player.appearance.isMale) "boy" else "girl"} doesn't really sound much like fun to me.")
                    npc(Npc.npc_1918, CALM_TALK, "Well, okay. Can't say as I blame you for thinking that. I'll go take it there myself later I guess.")
                }

                op("Accept quest") {
                    if (!Quest.DESERT_TREASURE.meetsReqs(player, "to start this quest.")) {
                        simple("You do not meet the requirements to start Desert Treasure.")
                    } else if (!player.inventory.hasFreeSlots()) {
                        npc(Npc.npc_1918, CALM_TALK, "Well, you're going to have to clear some junk out of your inventory before I hand this over.")
                    } else {
                        questStart(Quest.DESERT_TREASURE)
                        player(CALM_TALK, "Sure, I was heading that way anyway. Any particular person at Varrock Dig Site you want me to talk to?")
                        npc(Npc.npc_1918, CALM_TALK, "His name's Terry Balando. Give it to nobody but him. I'm sorry, I can't entrust you with the actual tablet I found, it is far too valuable to give away, but I took these etchings - they should be enough for him to make a preliminary translation on.")
                        npc(Npc.npc_1918, CALM_TALK, "Come back and let me know what he says. I would hate to waste my time excavating anything that isn't worth my time as a world famous archaeologist!") {
                            resetEblisIngredients(player)
                            player.inventory.addItemDrop(Obj.four_diamonds_etchings, 1)
                            player.setQuestStage(Quest.DESERT_TREASURE, STAGE_TERRY_BALANDO)
                        }
                    }
                }
            }
        }

        op("Who are you?") {
            player(CONFUSED, "Who are you, anyway?")
            npc(Npc.npc_1918, CALM_TALK, "You don't recognise me? I am the world famous Asgarnia Smith, Archaeologist extraordinaire!")
            npc(Npc.npc_1918, CALM_TALK, "I am the one who discovered the long forgotten Temple of Ikov! The one who unearthed the strange trap filled arena in Brimhaven!")
            npc(Npc.npc_1918, CONFUSED, "I'm the leading archaeological expert of our time! I was voted archaeologist of the year four years running by the Varrock Herald! Are you REALLY sure you've never heard of me?")
            player(CALM_TALK, "No, I really haven't.")
            npc(Npc.npc_1918, CONFUSED, "Well then, I'm confused. Why did you come over and speak to me if you don't know who I am? What do you want?")
            goto("mainOptions")
        }

        op("Nothing really") {
            player(CALM_TALK, "Nothing really. I'm just wandering around for no good reason.")
            npc(Npc.npc_1918, CALM_TALK, "Uh-uh. Well, if you'll excuse me, I have a lot more work to do before I can bed down for the night.")
        }
    }
}

private fun DialogueBuilder.waitingForTerry(player: Player) {
    player(CALM_TALK, "Hello there.")
    npc(Npc.npc_1918, CALM_TALK, "So what did Terry Balando say about those etchings? Did he give you a translation for me?")

    if (player.inventory.containsOneItem(Obj.four_diamonds_etchings)) {
        player(CALM_TALK, "Um... yeah... about that... I kind of didn't go and speak to him yet...")
        npc(Npc.npc_1918, CALM_TALK, "Well what are you waiting for? A written invitation? All I want you to do is take those etchings up to Varrock Dig Site for me!")
        player(CALM_TALK, "Okay, I'll do that then.")
    } else if (player.bank.containsItem(Obj.four_diamonds_etchings)) {
        player(CALM_TALK, "Um... yeah... about that... I kind of put those etchings in my bank...")
        npc(Npc.npc_1918, CALM_TALK, "Well what are you waiting for? Take them out and get them to Terry Balando at the Varrock Dig Site!")
        player(CALM_TALK, "Okay, I'll do that then.")
    } else if (player.inventory.hasFreeSlots()) {
        player(CALM_TALK, "Um... yeah... about that... I kind of lost those etchings you gave me...")
        npc(Npc.npc_1918, CALM_TALK, "Well how in the world did you do that? (sigh) Okay, I'll make another set, but try not to lose these ones this time!") {
            player.inventory.addItemDrop(Obj.four_diamonds_etchings, 1)
        }
    } else {
        player(CALM_TALK, "Um... yeah... about that... I kind of lost those etchings you gave me...")
        npc(Npc.npc_1918, CALM_TALK, "Well how in the world did you do that? (sigh) I'd make another set, but you don't have room in your inventory.")
    }
}

private fun DialogueBuilder.returningWithTranslation(player: Player) {
    player(CALM_TALK, "Hello there.")
    npc(Npc.npc_1918, CALM_TALK, "So what did Terry Balando say about those etchings? Did he give you a translation for me?")

    if (!player.inventory.containsOneItem(Obj.four_diamonds_translation_primer)) {
        player(CALM_TALK, "Yeah, he did. But I don't have it with me.")
        npc(Npc.npc_1918, CALM_TALK, "...")
        npc(Npc.npc_1918, CALM_TALK, "I see. Well could you go and get me that translation? It could be vital!")
        return
    }

    player(CALM_TALK, "Yeah, he did. I have it here in this book.")
    npc(Npc.npc_1918, CALM_TALK, "Did you take a read of this? It will do you good to understand how this profession works. Here, have a read.")

    options {
        op("Read book") {
            player(CALM_TALK, "I'll have a read.")
            simple("You read through Terry Balando's translation.")
            npc(Npc.npc_1918, CALM_TALK, "Excellent. Just give me a moment to read this, and talk to me again in a second.") {
                player.setQuestStage(Quest.DESERT_TREASURE, STAGE_MIRROR_MIRROR)
            }
        }

        op("Don't read book") {
            player(CALM_TALK, "Yeah, I did. Kind of boring really.")
            npc(Npc.npc_1918, CALM_TALK, "Excellent. Just give me a moment to read this, and talk to me again in a second.") {
                player.setQuestStage(Quest.DESERT_TREASURE, STAGE_MIRROR_MIRROR)
            }
        }
    }
}

private fun DialogueBuilder.treasureHunt(player: Player) {
    player(CALM_TALK, "Hello there.")
    npc(Npc.npc_1918, CALM_TALK, "Hmmm. Interesting. It seems to me like there's some kind of treasure hidden out in the desert.")
    npc(Npc.npc_1918, CALM_TALK, "So what do you say? Fancy being a treasure hunter like me?")
    player(CONFUSED, "Uh... don't you mean archaeologist?")
    npc(Npc.npc_1918, CALM_TALK, "Yeeees... An archaeologist...")
    npc(Npc.npc_1918, CALM_TALK, "It's all this hot sun getting to me I think.")
    npc(Npc.npc_1918, CALM_TALK, "Well anyway, let's just assume there is a treasure hidden in the desert somewhere, and let's just say for the sake of argument that maybe if I found a big stash of gold and treasure I wouldn't necessarily just hand it all over to the Museum of Varrock.")
    npc(Npc.npc_1918, CALM_TALK, "Let's also say, purely hypothetically, that if there were such a big stash of treasure and someone were to help me find it, then that hypothetical person might be entitled to, oh, let's say a purely for the sake of argument thirty percent split...")
    options {
        op("Deal.") {
            player(HAPPY_TALKING, "Deal. I'm in.")
            npc(Npc.npc_1918, CALM_TALK, "Excellent! Then let's get looking.")
            npc(Npc.npc_1918, CALM_TALK, "Head south to that bandit camp. I bet if there's anything out here, they'll know about it.")
            npc(Npc.npc_1918, CALM_TALK, "Don't let on it might be valuable though! Those thieves will steal it as soon as look at you!") {
                player.setQuestStage(Quest.DESERT_TREASURE, STAGE_TALK_TO_BARTENDER)
            }
        }
        op("No thanks.") {
            player(CALM_TALK, "I think I'll pass.")
            npc(Npc.npc_1918, CALM_TALK, "Suit yourself. I'll find the treasure myself.")
        }
    }
}

private fun DialogueBuilder.allFourDiamondsAsgarnia(player: Player) {
    player(CALM_TALK, "Hello there.")
    npc(Npc.npc_1918, CALM_TALK, "Hello again. How's the treasure hunting going?")
    player(CALM_TALK, "Well, I've found four diamonds.")
    npc(Npc.npc_1918, CONFUSED, "What? But you've been gone ages!")
    npc(Npc.npc_1918, CALM_TALK, "C'mon, you can be square with me, you must have found more treasure than just four measly diamonds by now!")
    player(CALM_TALK, "No, I really haven't.")
    npc(Npc.npc_1918, CALM_TALK, "Hmmm. Well, okay a deal is a deal. Hand two of them over.")
    player(CALM_TALK, "Actually, I think I'm going to need them to gain access to the real treasure...")
    npc(Npc.npc_1918, HAPPY_TALKING, "REALLY? Great, I love treasure!")
    npc(Npc.npc_1918, HAPPY_TALKING, "Don't worry about two measly diamonds if you think they'll lead to something better!")
    npc(Npc.npc_1918, CALM_TALK, "I don't usually get out of bed for less than a hundred gold bars!")
}

private fun DialogueBuilder.diamondsPlacedAsgarnia(player: Player) {
    player(CALM_TALK, "Hello there.")
    npc(Npc.npc_1918, CALM_TALK, "Hello again, apprentice archaeologist. How's the treasure hunting going?")
    player(CALM_TALK, "Well... I found four diamonds...")
    npc(Npc.npc_1918, HAPPY_TALKING, "Great! Pass me my share, and it's a good job all around!")
    player(CALM_TALK, "Um... sorry, I can't.")
    npc(Npc.npc_1918, CONFUSED, "Huh? You're not trying to break a deal here are ya???")
    player(CALM_TALK, "No, it's just... There were these weird pillars around this pyramid...")
    npc(Npc.npc_1918, CONFUSED, "Yes?")
    player(NERVOUS, "Um... Well...")
    player(NERVOUS, "They kind of... ate the diamonds...")
    npc(Npc.npc_1918, CONFUSED, "Pyramid columns ate your diamonds?")
    npc(Npc.npc_1918, CONFUSED, "That's a crazy story kid. Why would you go putting diamonds into them in the first place anyway? Diamonds!")
    npc(Npc.npc_1918, CALM_TALK, "Of all things!")
    player(CALM_TALK, "Well, I have this feeling there is some better treasure inside that pyramid, and I needed to put the diamonds in to gain entrance.")
    npc(Npc.npc_1918, HAPPY_TALKING, "Well what are you wasting time talking to me for?")
    npc(Npc.npc_1918, HAPPY_TALKING, "Go get me some treasure, pal!")
}
