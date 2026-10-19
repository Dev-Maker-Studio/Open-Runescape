package com.rs.game.content.holidayevents.halloween.hw10.dialogue

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Task
import com.rs.game.content.holidayevents.halloween.hw10.WerewolfState
import com.rs.game.content.holidayevents.halloween.hw10.hasMoonshineIngredients
import com.rs.game.content.holidayevents.halloween.hw10.hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.werewolfState
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq

private const val GRETA = Npc.hw10_greta
private const val HELGA = Npc.hw10_helga
private const val MOIRA = Npc.hw10_moira
private const val MOONSHINE = Obj.obj_19842
private const val HELGA_HINT_GIVEN = "hw10HelgaHintGiven"

private val SINGERS = mapOf(Npc.npc_12382 to GRETA, Npc.npc_12386 to HELGA, Npc.npc_12390 to MOIRA)

@ServerStartupEvent
fun mapWerewolves() {
    onNpcClick(*SINGERS.keys.toTypedArray<Any>(), options = arrayOf("Talk-to")) { (player, npc) ->
        val wolf = SINGERS.getValue(npc.id)
        player.startConversation {
            when {
                player.hw10Stage < Hw10Stage.PREPARING -> npc(wolf, HeadE.CALM_TALK, "I'm a dragon longsword, short and stout. Here's my handle, I'll poke your eye out.")
                player.werewolfState == WerewolfState.NOT_ASKED -> firstTalk(player, wolf)
                player.inventory.containsItem(MOONSHINE) -> handIn(player, wolf)
                player.hasMoonshineIngredients -> recipeHint(player, wolf)
                else -> missingIngredients(wolf)
            }
        }
    }
}

private fun DialogueBuilder.firstTalk(player: Player, wolf: Int) {
    player(HeadE.CALM_TALK, "Hello there.")
    when (wolf) {
        HELGA -> {
            npc(HELGA, HeadE.HAPPY_TALKING, "Hi there. I'm ${player.displayName}, pleased to meet you.")
            player(HeadE.CONFUSED, "That's my name.")
            npc(HELGA, HeadE.CONFUSED, "Oh right, I must be someone else.")
        }
        MOIRA -> npc(MOIRA, HeadE.HAPPY_TALKING, "Hi there. You must be the helper Pumpkin Pete was raving about.")
        else -> npc(GRETA, HeadE.HAPPY_TALKING, "Oh! Hello there, human. I'm afraid I'm not able to sign any autographs today; I've just had my claws manicured.")
    }
    options {
        op("Who are you?") { whoAreWe(player, wolf) }
        op("Need any help?") { needHelp(player) }
        op("I'll be off.") { leave(wolf) }
    }
}

private fun DialogueBuilder.whoAreWe(player: Player, wolf: Int) {
    player(HeadE.CONFUSED, "Who are you?")
    npc(GRETA, HeadE.HAPPY_TALKING, "We're backing singers for Adamantite Damsel.")
    npc(GRETA, HeadE.HAPPY_TALKING, "We're the band playing at the party, in case you didn't know.")
    npc(GRETA, HeadE.CALM_TALK, "I'm Greta.")
    npc(HELGA, HeadE.HAPPY_TALKING, "I'm Steve.")
    npc(GRETA, HeadE.CALM_TALK, "You're Helga.")
    npc(HELGA, HeadE.CONFUSED, "I'm Helga...")
    npc(MOIRA, HeadE.CALM_TALK, "I'm Moira.")
    player(HeadE.SKEPTICAL, "I was expecting slightly more...werewolfy names.")
    npc(GRETA, HeadE.CALM_TALK, "These are werewolf names...just not the werewolf names you may be used to.")
    player(HeadE.CALM_TALK, "Fair enough.")
    options {
        op("I've never heard of Adamantite Damsel.") {
            player(HeadE.CONFUSED, "I've never heard of Adamantite Damsel.")
            npc(GRETA, HeadE.AMAZED, "Never heard of us?")
            npc(GRETA, HeadE.CALM_TALK, "Oh wait, I suppose not: we tend to play the spirit realms more than the mortal realms.")
            npc(MOIRA, HeadE.HAPPY_TALKING, "We're one of the most popular bands in the spirit realms.")
            npc(MOIRA, HeadE.HAPPY_TALKING, "You must have heard of Zabeth Corvid, our lead singer? His musical stylings are legendary!")
            options {
                op("Never heard of him.") {
                    player(HeadE.CALM_TALK, "Never heard of him.")
                    npc(GRETA, HeadE.AMAZED, "Wow! You really don't know your music.")
                    helpOrLeave(player, wolf)
                }
                op("Oh yeah, I know him.") {
                    player(HeadE.HAPPY_TALKING, "Oh yeah, I know him.")
                    npc(MOIRA, HeadE.HAPPY_TALKING, "Then you're in for a treat tonight!")
                    helpOrLeave(player, wolf)
                }
            }
        }
        op("Need any help?") { needHelp(player) }
        op("I'll be off.") { leave(wolf) }
    }
}

private fun DialogueBuilder.helpOrLeave(player: Player, wolf: Int) {
    options {
        op("Need any help?") { needHelp(player) }
        op("I'll be off.") { leave(wolf) }
    }
}

private fun DialogueBuilder.leave(wolf: Int) {
    player(HeadE.CALM_TALK, "I'll be off.")
    npc(wolf, HeadE.HAPPY_TALKING, "Don't forget to come and see us perform!")
}

private fun DialogueBuilder.needHelp(player: Player) {
    player(HeadE.CALM_TALK, "Need any help?")
    npc(MOIRA, HeadE.HAPPY_TALKING, "Ooh, now you mention it, yes.")
    npc(MOIRA, HeadE.CALM_TALK, "We need our voices to be in top condition for our performance, but there's precious little to drink around here.")
    npc(MOIRA, HeadE.CALM_TALK, "Could you get us something?")
    options {
        op("Sure, that seems straightforward enough.") {
            player(HeadE.CALM_TALK, "Sure, that seems straightforward enough.")
            player(HeadE.CONFUSED, "What would you like?")
            npc(GRETA, HeadE.HAPPY_TALKING, "Moonshine would be lovely!")
            player(HeadE.CONFUSED, "You want me to make homemade alcohol?")
            npc(MOIRA, HeadE.CALM_TALK, "No disrespect, but no.")
            npc(MOIRA, HeadE.CALM_TALK, "Moonshine is a werewolf drink and it needs to be made fresh, so you can't find it just lying around.")
            options {
                op("How do I make it?") {
                    player(HeadE.CONFUSED, "How do I make it?")
                    npc(GRETA, HeadE.CONFUSED, "Hmmm, good point. I can't remember exactly.")
                    npc(HELGA, HeadE.ANGRY, "Wait - Steve knows, but he's not telling. Mean old Steve.")
                    npc(MOIRA, HeadE.CALM_TALK, "Adventurer, between us, we might know enough to make it. See if you can some ingredients and then ask each of us and see if you can work out the full recipe.")
                    npc(MOIRA, HeadE.CALM_TALK, "Be wary of Helga's answers, though; she tends to live in a world of her own and she will always be incorrect.", Runnable {
                        player.werewolfState = WerewolfState.MOONSHINE_REQUESTED
                    })
                }
                op("I can't be bothered.") {
                    player(HeadE.CALM_TALK, "I can't be bothered.")
                    npc(MOIRA, HeadE.ANGRY, "Charming. Don't expect a song dedication, then.")
                }
            }
        }
        op("I'd rather not.") {
            player(HeadE.CALM_TALK, "I'd rather not.")
            npc(MOIRA, HeadE.SAD, "Oh. Well, I suppose we'll just have to croak our way through the set.")
        }
    }
}

private fun DialogueBuilder.handIn(player: Player, wolf: Int) {
    npc(wolf, HeadE.HAPPY_TALKING, "Oooh, is that moonshine I smell?")
    player(HeadE.CALM_TALK, "Yup, I mixed the revolting stuff myself.")
    npc(wolf, HeadE.HAPPY_TALKING, "Well, give it here!")
    item(MOONSHINE, "You hand over the vial of moonshine to the werewolves.", Runnable {
        if (!player.inventory.containsItem(MOONSHINE))
            return@Runnable
        player.inventory.deleteItem(MOONSHINE, 1)
        player.anim(Seq.hw10_player_give_or_receive)
        player.werewolfState = WerewolfState.MOONSHINE_DELIVERED
    })
    npc(wolf, HeadE.HAPPY_TALKING, "Ahhh, delicious. We should be able to perform properly now.")
    npc(MOIRA, HeadE.HAPPY_TALKING, "Thank you, human.")
    npc(HELGA, HeadE.HAPPY_TALKING, "See you on the dancefloor.")
    npc(GRETA, HeadE.HAPPY_TALKING, "We'll see if we can dedicate a song to you.")
    checkInWithPete(player, Hw10Task.MOONSHINE)
}

private fun DialogueBuilder.missingIngredients(wolf: Int) {
    npc(wolf, HeadE.CALM_TALK, "You don't appear to have all the ingredients yet. It will be better to talk to us when you do.")
    options {
        op("Where can I find the ingredients?") {
            player(HeadE.CONFUSED, "Where can I find the ingredients?")
            npc(wolf, HeadE.CALM_TALK, "I don't know. Have you looked around the house?")
        }
        op("What is it you want me to make again?") {
            player(HeadE.CONFUSED, "What is it you want me to make again?")
            npc(wolf, HeadE.CALM_TALK, "Moonshine! Look around the house for the ingredients, then ask each of us about the recipe.")
        }
    }
}

private fun DialogueBuilder.recipeHint(player: Player, wolf: Int) {
    when (wolf) {
        MOIRA -> {
            npc(MOIRA, HeadE.HAPPY_TALKING, "Ah, I see you found a number of ingredients. Excellent.")
            npc(MOIRA, HeadE.CALM_TALK, "To make moonshine... I'm certain that it uses each of the ingredients, but I can't remember the order.")
            npc(MOIRA, HeadE.CALM_TALK, "I remember straining it through something first: something that grew in Granny Doomeye's back garden.")
            npc(MOIRA, HeadE.CALM_TALK, "She was a particularly unpleasant woman.")
            npc(MOIRA, HeadE.HAPPY_TALKING, "Good as a sandwich filling, though.")
        }
        HELGA -> {
            val repeat = player.tempAttribs.getB(HELGA_HINT_GIVEN)
            player.tempAttribs.setB(HELGA_HINT_GIVEN, !repeat)
            if (repeat)
                npc(HELGA, HeadE.CALM_TALK, "Or Bernard.")
            else
                npc(HELGA, HeadE.CALM_TALK, "Right, this is how you make moonshine. The blood is always the last ingredient and is never the first thing to be poured into the bottle. I'm as sure of that as my name is Steve.")
        }
        else -> npc(GRETA, HeadE.HAPPY_TALKING, "Let's see, how to make moonshine... I do so love moonshine, it's delicious. It's the sprinkles on top that make it really tasty.")
    }
}
