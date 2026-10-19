package com.rs.game.content.quests.the_grand_tree.npcs

import org.darkan.core.gameval.gen.Npc
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import org.darkan.core.Utils
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick

@ServerStartupEvent
fun mapShipyardWorkersGrandTree() {
    onNpcClick(Npc.shipyardworker1, Npc.shipyardworker2, options = arrayOf("Talk-to")) { (player, npc) -> randomWorkerDialogue(player, npc) }
    onNpcClick(Npc.grandtree_shipyardguard, options = arrayOf("Talk-to")) { (player, npc) ->
        player.startConversation { npc(npc, HeadE.SKEPTICAL, "This gate is for authorised shipyard workers only.") }
    }
}

internal fun shipyardGateChallenge(player: Player, npc: NPC, onAccessGranted: () -> Unit) {
    player.startConversation {
        npc(npc, HeadE.ANGRY, "Hey you! What are you up to?")
        player(HeadE.CALM_TALK, "I'm trying to open the gate!")
        npc(npc, HeadE.FRUSTRATED, "I can see that! Why?")
        options {
            op("I'm from the Ministry of Health and Safety.") {
                player(HeadE.CALM_TALK, "I'm from the Ministry of Health and Safety.")
                npc(npc, HeadE.SKEPTICAL, "Never 'erd of 'em.")
                player(HeadE.FRUSTRATED, "You will respect my authority!")
                npc(npc, HeadE.ANGRY, "Get out of here before I give you a beating!")
            }
            op("Glough sent me.") {
                player(HeadE.CALM_TALK, "Glough sent me.")
                npc(npc, HeadE.SKEPTICAL, "Hmm...really? What for?")
                player(HeadE.FRUSTRATED, "You're wasting my time! Take me to your superior!")
                npc(npc, HeadE.CALM_TALK, "OK. Password.")
                exec { passwordOne(player, npc, onAccessGranted) }
            }
            op("I'm just looking around.") {
                player(HeadE.CALM_TALK, "I'm just looking around.")
                npc(npc, HeadE.ANGRY, "This ain't a museum! Leave now!")
                player(HeadE.FRUSTRATED, "I'll leave when I choose!")
                npc(npc, HeadE.SKEPTICAL, "Well you're not on the list so you're not coming in. Go away.")
                player(HeadE.FRUSTRATED, "Well I'll just stand here then until you let me in.")
                npc(npc, HeadE.CALM_TALK, "You do that!")
                player(HeadE.CALM_TALK, "I will!")
                npc(npc, HeadE.SKEPTICAL, "Yeah?")
                player(HeadE.CALM_TALK, "Yeah!")
                npc(npc, HeadE.CALM_TALK, "...")
                player(HeadE.CALM_TALK, "...")
                player(HeadE.CONFUSED, "So are you going to let me in then?")
                npc(npc, HeadE.CALM_TALK, "No.")
                player(HeadE.CALM_TALK, "...")
                npc(npc, HeadE.CALM_TALK, "...")
                player(HeadE.CONFUSED, "You bored yet?")
                npc(npc, HeadE.CALM_TALK, "No. I can stand here all day.")
                player(HeadE.CALM_TALK, "...")
                npc(npc, HeadE.CALM_TALK, "...")
                player(HeadE.SAD_MILD_LOOK_DOWN, "Alright you win. I'll find another way in.")
                npc(npc, HeadE.SKEPTICAL, "No you won't.")
                player(HeadE.FRUSTRATED, "Yes I will.")
                npc(npc, HeadE.FRUSTRATED, "I'm not starting that again. Maybe if I ignore you you'll go away...")
            }
        }
    }
}

private fun passwordOne(player: Player, npc: NPC, onAccessGranted: () -> Unit) =
    passwordPart(player, npc, listOf("Ka.", "Ko.", "Ke.")) { first ->
        passwordTwo(player, npc, first == 0, onAccessGranted)
    }

private fun passwordTwo(player: Player, npc: NPC, correct: Boolean, onAccessGranted: () -> Unit) =
    passwordPart(player, npc, listOf("Lo.", "Lu.", "Le.")) { second ->
        passwordThree(player, npc, correct && second == 1, onAccessGranted)
    }

private fun passwordThree(player: Player, npc: NPC, correct: Boolean, onAccessGranted: () -> Unit) =
    passwordPart(player, npc, listOf("Mon.", "Min.", "Men.")) { third ->
        if (correct && third == 1) {
            player.startConversation {
                npc(npc, HeadE.HAPPY_TALKING, "Sorry to have kept you.") {
                    player.grandTreeAttribs().setB(ATTR_SHIPYARD_ACCESS, true)
                    onAccessGranted()
                }
            }
        } else {
            player.startConversation { npc(npc, HeadE.ANGRY, "You have no idea!") { npc.combatTarget = player } }
        }
    }

private fun passwordPart(player: Player, npc: NPC, parts: List<String>, next: (Int) -> Unit) {
    player.startConversation {
        options {
            parts.forEachIndexed { index, part -> op(part) { player(HeadE.CALM_TALK, part); exec { next(index) } } }
        }
    }
}

private fun randomWorkerDialogue(player: Player, npc: NPC) {
    player.startConversation {
        when (Utils.random(17)) {
            0 -> { player(HeadE.HAPPY_TALKING, "Hello."); player(HeadE.AMAZED_MILD, "Quite a few ships you're building!"); npc(npc, HeadE.HAPPY_TALKING, "This is just the start! The completed fleet will be awesome!") }
            1, 9 -> { player(HeadE.HAPPY_TALKING, "Hello."); npc(npc, HeadE.FRUSTRATED, "No time to talk we've a fleet to build!") }
            2 -> { player(HeadE.HAPPY_TALKING, "Hello."); npc(npc, HeadE.HAPPY_TALKING, "Hello matey!"); player(HeadE.CALM_TALK, "How are you?"); npc(npc, HeadE.SAD_MILD_LOOK_DOWN, "Tired!"); player(HeadE.CALM_TALK, "You shouldn't work so hard!") }
            3 -> { player(HeadE.HAPPY_TALKING, "Hello."); player(HeadE.AMAZED_MILD, "Quite an impressive set up!"); npc(npc, HeadE.CALM_TALK, "It needs to be. There's no other way to build a fleet of this size!") }
            4 -> { player(HeadE.HAPPY_TALKING, "Hello."); npc(npc, HeadE.CONFUSED, "Hello there. I haven't seen you before?"); player(HeadE.CALM_TALK, "I'm new!"); npc(npc, HeadE.HAPPY_TALKING, "Well it's hard work, but the pay is good.") }
            5 -> { player(HeadE.HAPPY_TALKING, "Hello!"); player(HeadE.CALM_TALK, "How are you?"); npc(npc, HeadE.FRUSTRATED, "Too busy to waste time gossiping!"); player(HeadE.FRUSTRATED, "Touchy!") }
            6 -> { player(HeadE.HAPPY_TALKING, "Hello. Looks like hard work?"); npc(npc, HeadE.HAPPY_TALKING, "I like to keep busy.") }
            7 -> { player(HeadE.HAPPY_TALKING, "Hello!"); player(HeadE.CONFUSED, "So where are you sailing?"); npc(npc, HeadE.CONFUSED, "What do you mean?"); player(HeadE.HAPPY_TALKING, "Don't worry, just kidding!") }
            8 -> { player(HeadE.HAPPY_TALKING, "Hello!"); npc(npc, HeadE.SKEPTICAL, "Hello there, are you too lazy to work as well?"); player(HeadE.CALM_TALK, "Something like that."); npc(npc, HeadE.HAPPY_TALKING, "I'm just sun bathing!") }
            10 -> { player(HeadE.HAPPY_TALKING, "Hello!"); player(HeadE.CONFUSED, "What are you building?"); npc(npc, HeadE.SKEPTICAL, "Are you serious?"); player(HeadE.HAPPY_TALKING, "Of course not! You're obviously building a boat.") }
            11, 16 -> { player(HeadE.HAPPY_TALKING, "Hello!"); npc(npc, HeadE.FRUSTRATED, "I'm getting tired of this!"); player(HeadE.CONFUSED, "What?"); npc(npc, HeadE.FRUSTRATED, "Breaking my back for pennies! It's just not on!") }
            12 -> { player(HeadE.HAPPY_TALKING, "Hello!"); npc(npc, HeadE.SKEPTICAL, "What do you want?"); player(HeadE.FRUSTRATED, "Is that any way to talk to your new superior?"); npc(npc, HeadE.NERVOUS, "Oh, I'm sorry, I didn't realise!") }
            13 -> { player(HeadE.HAPPY_TALKING, "Hello!"); npc(npc, HeadE.WORRIED, "Ouch!"); player(HeadE.CONFUSED, "What's wrong?!"); npc(npc, HeadE.WORRIED, "I cut my finger!"); npc(npc, HeadE.WORRIED, "Do you have a bandage?"); player(HeadE.SAD_MILD_LOOK_DOWN, "I'm afraid not."); npc(npc, HeadE.CALM_TALK, "That's ok, I'll use my shirt.") }
            14 -> { player(HeadE.HAPPY_TALKING, "Hello!"); npc(npc, HeadE.CALM_TALK, "Can I help you?"); player(HeadE.CALM_TALK, "I'm just looking around."); npc(npc, HeadE.FRUSTRATED, "Well there's plenty of work to be done, so if you don't mind..."); npc(npc, HeadE.CALM_TALK, "Of course. Sorry to have disturbed you.") }
            else -> { player(HeadE.HAPPY_TALKING, "Hello. You look busy!"); npc(npc, HeadE.WORRIED, "We need double the men to get this order out on time!") }
        }
    }
}
