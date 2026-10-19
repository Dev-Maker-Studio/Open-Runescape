package com.rs.game.content.quests.observatoryquest

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import kotlin.random.Random
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

@ServerStartupEvent
fun mapObservatoryAssistant() {
    onNpcClick(Npc.qip_obs_proffesors_assistant) { (player) ->
        player.startConversation {
            when (player.getQuestStage(Quest.OBSERVATORY_QUEST)) {
                STAGE_UNSTARTED -> assistantBeforeQuest()
                STAGE_GATHERING_PLANKS -> {
                    assistantGreeting()
                    plankHelp(player)
                }
                STAGE_GETTING_BRONZE -> {
                    assistantGreeting()
                    bronzeHelp(player)
                }
                STAGE_GETTING_MOLTEN_GLASS -> {
                    assistantGreeting()
                    moltenGlassHelp(player)
                }
                STAGE_FINDING_LENS_MOULD -> {
                    assistantGreeting()
                    lensMouldHelp(player)
                }
                STAGE_MAKING_LENS -> {
                    assistantGreeting()
                    lensHelp(player)
                }
                STAGE_TRAVELLING_TO_OBSERVATORY -> departingDialogue()
                STAGE_OBSERVING_CONSTELLATION -> assistantGeneralDialogue()
                STAGE_COMPLETE -> {
                    player(HeadE.HAPPY_TALKING, "Hello again.")
                    npc(Npc.qip_obs_proffesors_assistant, HeadE.HAPPY_TALKING, "My name's Patrick. Thank you for helping us repair the telescope.")
                    if (!player.getBool("observatoryQuestAssistantWine")) {
                        npc(Npc.qip_obs_proffesors_assistant, HeadE.HAPPY_TALKING, "Have a jug of wine as a thank-you.") {
                            if (player.isQuestComplete(Quest.OBSERVATORY_QUEST) && !player.getBool("observatoryQuestAssistantWine")) {
                                player.set("observatoryQuestAssistantWine", true)
                                player.inventory.addItemDrop(Obj.jug_wine, 1)
                            }
                        }
                    }
                    npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "The astronomy book on the table tells the story of Scorpius. His grave is north of here.")
                }
            }
        }
    }
}

private fun DialogueBuilder.assistantBeforeQuest() {
    player(HeadE.CALM_TALK, "Hi, are you busy?")
    npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Me? I'm always busy.")
    npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "See that man there? That's the professor. If he had his way, I think he'd never let me sleep!")
    npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Anyway, how might I help you?")
    options {
        op("I was wondering what you do here.") {
            player(HeadE.CALM_TALK, "I was wondering what you do here.")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.HAPPY_TALKING, "Glad you ask. This is the Observatory reception.")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Up on the cliff is the Observatory dome, from which you can view the heavens.")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Usually...")
            player(HeadE.SKEPTICAL, "What do you mean, 'usually'?")
            npc(Npc.observatory_professor, HeadE.CALM_TALK, "Ahem. You have work to get back to.")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "I'd speak with the professor. He'll explain.")
        }
        op("Just looking around, thanks.") {
            player(HeadE.CALM_TALK, "Just looking around, thanks.")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Okay, just don't break anything. If you need any help, let me know.")
        }
        op("Can I have a look through that telescope?") {
            player(HeadE.CALM_TALK, "Can I have a look through that telescope?")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "You can. You won't see much though.")
            player(HeadE.SKEPTICAL, "And that's because?")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Just talk to the professor. He'll fill you in.")
        }
    }
}

private fun DialogueBuilder.assistantGreeting() {
    when (Random.nextInt(3)) {
        0 -> {
            player(HeadE.CALM_TALK, "Hello there.")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Yes?")
        }
        1 -> {
            player(HeadE.CALM_TALK, "Might I have a word?")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Sure, how can I help you?")
        }
        else -> {
            player(HeadE.CALM_TALK, "Can I speak with you?")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Why, of course. What is it?")
        }
    }
}

private fun DialogueBuilder.plankHelp(player: Player) {
    val planks = player.inventory.getAmountOf(Obj.woodplank)
    when {
        planks == 0 -> {
            player(HeadE.CALM_TALK, "Where can I find planks of wood? I need some for the telescope's base.")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "I understand planks can be found at Port Khazard, to the east of here. There are some at the Barbarian Outpost, too.")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Failing that, you could always ask the Sawmill Operator. He's to the north-east of Varrock, by the Lumber Yard.")
        }
        planks < 3 -> {
            player(HeadE.CALM_TALK, "I've got a plank.")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "That's nice.")
            player(HeadE.CALM_TALK, "You know, for the telescope's base.")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Well done. Remember that you'll need three in total.")
        }
        else -> {
            player(HeadE.HAPPY_TALKING, "I've got some planks for the telescope's base.")
            npc(Npc.qip_obs_proffesors_assistant, HeadE.HAPPY_TALKING, "Good work! The professor has been eagerly waiting them.")
        }
    }
}

private fun DialogueBuilder.bronzeHelp(player: Player) {
    if (!player.inventory.containsItem(Obj.bronze_bar, 1)) {
        player(HeadE.CALM_TALK, "Can you help me? How do I go about getting a bronze bar?")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "You'll need to use tin and copper ore on a furnace to produce this metal.")
        player(HeadE.CALM_TALK, "Right you are.")
    } else {
        player(HeadE.HAPPY_TALKING, "The bronze bar is ready, and waiting for the professor.")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.HAPPY_TALKING, "He'll surely be pleased. Go ahead and give it to him.")
    }
}

private fun DialogueBuilder.moltenGlassHelp(player: Player) {
    if (!player.inventory.containsItem(Obj.molten_glass, 1)) {
        player(HeadE.CALM_TALK, "What's the best way for me to get molten glass?")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "There are many ways, but I'd suggest making it yourself. Get yourself a bucket of sand and some soda ash, which you can get from using seaweed with a furnace. Use the soda ash and sand together in a furnace and bang -")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.HAPPY_TALKING, "molten glass is all yours. There's a book about it on the table if you want to know more.")
        player(HeadE.HAPPY_TALKING, "Thank you!")
    } else {
        player(HeadE.HAPPY_TALKING, "I managed to get hold of some molten glass.")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "I suggest you have a word with the professor, in that case.")
    }
}

private fun DialogueBuilder.lensMouldHelp(player: Player) {
    if (!player.inventory.containsItem(Obj.lens_mould, 1)) {
        player(HeadE.CALM_TALK, "Where can I find this lens mould you mentioned?")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "I'm sure I heard one of those goblins talking about it. I bet they've hidden it somewhere. Probably using it for some strange purpose, I'm sure.")
        player(HeadE.SKEPTICAL, "What makes you say that?")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "I had a nice new star chart, until recently. I went out to do an errand for the professor the other day, only to see a goblin using it...")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "...as some kind of makeshift hankey to blow his nose!")
        player(HeadE.SKEPTICAL, "Oh dear.")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "You may want to look through the dungeon they have under their little village.")
        player(HeadE.HAPPY_TALKING, "Thanks for the advice.")
    } else {
        player(HeadE.HAPPY_TALKING, "I have the lens mould.")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.HAPPY_TALKING, "Well done on finding that! I am honestly quite impressed. Make sure you take it straight to the professor.")
        player(HeadE.CALM_TALK, "Will do.")
    }
}

private fun DialogueBuilder.lensHelp(player: Player) {
    if (!player.inventory.containsItem(Obj.lens, 1)) {
        player(HeadE.CALM_TALK, "How should I make this lens?")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Just use the molten glass with the mould. Simple.")
        player(HeadE.HAPPY_TALKING, "Thanks!")
    } else {
        player(HeadE.HAPPY_TALKING, "Do you like this lens? Good, huh?")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.SKEPTICAL, "Nice. What's that scratch?")
        player(HeadE.NERVOUS, "Oh, erm, that's a feature.")
        player(HeadE.CALM_TALK, "Yes, that's it! Indubitably, it facilitates the triangulation of photonic illumination to the correct...")
        npc(Npc.qip_obs_proffesors_assistant, HeadE.FRUSTRATED, "Stop! You can't confuse me with big words. Just pray the professor doesn't notice.")
    }
}

private fun DialogueBuilder.departingDialogue() {
    player(HeadE.HAPPY_TALKING, "Hello again.")
    npc(Npc.qip_obs_proffesors_assistant, HeadE.HAPPY_TALKING, "Ah, it's the telescope repairman! The professor is waiting for you in the Observatory.")
    player(HeadE.CALM_TALK, "How can I get to the Observatory?")
    npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Well, since the bridge was ruined, you will have to travel through the dungeon under the goblins' settlement.")
}

private fun DialogueBuilder.assistantGeneralDialogue() {
    player(HeadE.CALM_TALK, "Hello there.")
    npc(Npc.qip_obs_proffesors_assistant, HeadE.CALM_TALK, "Yes?")
}
