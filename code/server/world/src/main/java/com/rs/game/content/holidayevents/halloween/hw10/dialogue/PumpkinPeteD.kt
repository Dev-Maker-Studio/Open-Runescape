package com.rs.game.content.holidayevents.halloween.hw10.dialogue

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.engine.dialogue.statements.QuestStartStatement
import com.rs.engine.quest.Quest
import com.rs.game.content.holidayevents.halloween.hw10.EekState
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.hasEek
import com.rs.game.content.holidayevents.halloween.hw10.hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.hw10TasksDone
import com.rs.game.content.holidayevents.halloween.hw10.eekState
import com.rs.game.content.holidayevents.halloween.hw10.emoteLearned
import com.rs.game.content.holidayevents.halloween.hw10.playPartyCutscene
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.managers.EmotesManager.Emote
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

private const val PETE_OUTSIDE = Npc.hw10_pumpkin_pete
private const val PETE = Npc.hw10_pumpkin_pete_2

@ServerStartupEvent
fun mapPumpkinPete() {
    onNpcClick(PETE_OUTSIDE, options = arrayOf("Talk-to")) { (player) -> talkToPumpkinPeteOutside(player) }
    onNpcClick(PETE, options = arrayOf("Talk-to")) { (player) -> talkToPumpkinPeteInside(player) }
    onNpcClick(Npc.hw10_party_pete, options = arrayOf("Talk-to")) { (player) ->
        player.startConversation { npc(Npc.hw10_party_pete, HeadE.HAPPY_TALKING, "Great party, dude! Why aren't you dancing? Come on man, get your boogie on!") }
    }
}

private fun talkToPumpkinPeteOutside(player: Player) = player.startConversation {
    jackLanternGreeting(player, PETE_OUTSIDE)
    when (player.hw10Stage) {
        Hw10Stage.NOT_STARTED -> {
            npc(PETE_OUTSIDE, HeadE.HAPPY_TALKING, "Hello there.")
            npc(PETE_OUTSIDE, HeadE.HAPPY_TALKING, "After an autograph, are we?")
            options {
                op("Sure.") {
                    player(HeadE.HAPPY_TALKING, "Sure.")
                    player(HeadE.HAPPY_TALKING, "I've always wanted your autograph.")
                    if (player.hasEek) {
                        eek("Oooh, can I have one? I've always wanted one.")
                        eek("I once asked for Party Pete's autograph, but he tried to squish me with the paper. I don't think he likes spiders.")
                        eek("You like spiders don't you Pumpkin Pete?")
                        npc(PETE_OUTSIDE, HeadE.HAPPY_TALKING, "I think spiders are fantastic, my eight-legged friend!")
                        npc(PETE_OUTSIDE, HeadE.HAPPY_TALKING, "Although, I would appreciate it if you stopped trying to web inside my pumpkin head.")
                        eek("Hahahaha! Sorry, old habits die hard.")
                    }
                    whoIsPete(player)
                }
                op("Erm, who are you?") { whoIsPete(player) }
            }
        }
        Hw10Stage.STARTED, Hw10Stage.PREPARING -> npc(PETE_OUTSIDE, HeadE.HAPPY_TALKING, "Head on through the portal. I'll meet you on the other side.")
        Hw10Stage.PARTY, Hw10Stage.COMPLETE -> npc(PETE_OUTSIDE, HeadE.HAPPY_TALKING, "What are you doing out here? The real party is back through that portal. Come on, you know you want to get down and boogie.")
    }
}

private fun DialogueBuilder.whoIsPete(player: Player) {
    player(HeadE.CONFUSED, "Erm, who are you?")
    npc(PETE_OUTSIDE, HeadE.AMAZED, "You don't know who I am?")
    npc(PETE_OUTSIDE, HeadE.HAPPY_TALKING, "I'm Pumpkin Pete, Halloween celebrity and expert party organiser.")
    if (player.hasEek) {
        eek("Hahaha!")
        eek("Everyone knows that!")
        eek("...")
        eek("Okay, I didn't know either.")
    }
    player(HeadE.CONFUSED, "You're throwing someone a party?")
    npc(PETE_OUTSIDE, HeadE.HAPPY_TALKING, "Indeed, for none other than the Grim Reaper himself!")
    npc(PETE_OUTSIDE, HeadE.CALM_TALK, "He always seems to have a miserable time at Halloween, so I thought I'd cheer him up a bit.")
    npc(PETE_OUTSIDE, HeadE.HAPPY_TALKING, "Say, you wouldn't mind helping out, would you? You seem a capable individual. There'll be a brilliant party at the end if you do.")
    if (player.eekState == EekState.OWNED)
        npc(PETE_OUTSIDE, HeadE.HAPPY_TALKING, "By the way, the spiders in my pumpkin head tell me that they'd like to see Eek again, so you could bring her along. Up to you.")
    var learnedTrick = false
    statementWithOptions(QuestStartStatement(Quest.HALLOWEEN_2010), {
        npc(PETE_OUTSIDE, HeadE.HAPPY_TALKING, "Splendid! Head through that portal and I'll tell you what needs doing. Together, we'll make this the best party ever!", Runnable {
            player.hw10Stage = Hw10Stage.STARTED
            learnedTrick = player.emotesManager.unlockEmote(Emote.TRICK)
        })
        exec {
            if (learnedTrick)
                player.startConversation { emoteLearned("Trick") }
        }
    }, {
        npc(PETE_OUTSIDE, HeadE.SAD, "Oh. Well, if you change your mind, you know where to find me. I'll be right here, next to this rather large portal.")
    })
}

private fun talkToPumpkinPeteInside(player: Player) = player.startConversation {
    jackLanternGreeting(player, PETE)
    when (player.hw10Stage) {
        Hw10Stage.NOT_STARTED, Hw10Stage.STARTED -> firstTalkInside(player)
        Hw10Stage.PREPARING -> if (player.hw10TasksDone) partyFinale(player) else progressCheck()
        Hw10Stage.PARTY, Hw10Stage.COMPLETE -> npc(PETE, HeadE.HAPPY_TALKING, "Greetings ${player.displayName}, my good friend. I hope you're enjoying the party.")
    }
}

private fun DialogueBuilder.firstTalkInside(player: Player) {
    player(HeadE.HAPPY_TALKING, "Hello again, Pumpkin Pete.")
    if (player.appearance.isMale)
        npc(PETE, HeadE.HAPPY_TALKING, "Ah! ${player.displayName}, my dear boy, what a pleasure to see you!")
    else
        npc(PETE, HeadE.HAPPY_TALKING, "Ah! ${player.displayName} my dear girl, what a pleasure to see you!")
    npc(PETE, HeadE.HAPPY_TALKING, "Are you ready to create the best party of all time?")
    options {
        op("Yes.") {
            npc(PETE, HeadE.HAPPY_TALKING, "Marvellous. Wander around and talk to people, see where you can lend a hand.", Runnable { player.hw10Stage = Hw10Stage.PREPARING })
            npc(PETE, HeadE.LAUGH, "Be careful offering a hand though: some of them bite.")
            npc(PETE, HeadE.CALM_TALK, "If you get confused at any point, come talk to me and I'll tell you how you are doing.")
        }
        op("I need a moment.") {
            player(HeadE.CALM_TALK, "I need a moment.")
            npc(PETE, HeadE.HAPPY_TALKING, "Take all the time you need. Talk to me again when you're ready to get this party started!")
        }
    }
}

private const val AFTER_WHO = "afterWho"
private const val AFTER_WHY = "afterWhy"

private fun DialogueBuilder.progressCheck() {
    player(HeadE.CONFUSED, "How am I doing?")
    npc(PETE, HeadE.CALM_TALK, "Hmmm, let me have a think.")
    npc(PETE, HeadE.CALM_TALK, "You've helped out a little so far, but there's still more to be done.")
    options {
        op("Who are you?") { whoIsPeteThenMenu() }
        op("Why are you holding a party for Grim?") { whyThePartyThenMenu() }
        op("I should get going.")
    }
}

private fun DialogueBuilder.whoIsPeteThenMenu() {
    whoIsPumpkinPete()
    label(AFTER_WHO)
    options {
        op("Why are you holding a party for Grim?") {
            whyTheParty()
            goto(AFTER_WHY)
        }
        op("I'm going now.")
    }
}

private fun DialogueBuilder.whyThePartyThenMenu() {
    whyTheParty()
    label(AFTER_WHY)
    options {
        op("Who are you?") {
            whoIsPumpkinPete()
            goto(AFTER_WHO)
        }
        op("I have to leave now.")
    }
}

private fun DialogueBuilder.whoIsPumpkinPete() {
    player(HeadE.CONFUSED, "Who are you?")
    npc(PETE, HeadE.HAPPY_TALKING, "Pumpkin Pete. You may know me from such Halloween stories as 'Drowsy Hollow', 'Halloween Dihydrogen Monoxide' and, of course, my personal favourite 'Aaaaaargh GET IT OFF ME!'")
    player(HeadE.SKEPTICAL, "I can't say I've heard of any of them.")
    npc(PETE, HeadE.SAD, "Really? I shall have to fire my agent. Again.")
}

private fun DialogueBuilder.whyTheParty() {
    player(HeadE.CONFUSED, "Why are you holding a party for Grim?")
    npc(PETE, HeadE.HAPPY_TALKING, "You mean besides the fact that parties are awesome?")
    player(HeadE.CALM_TALK, "Yes, besides that.")
    npc(PETE, HeadE.CALM_TALK, "Grim just seems to get into such scrapes at Halloween. I thought I'd cheer him up for once.")
    npc(PETE, HeadE.HAPPY_TALKING, "And what better way to cheer someone up than with an epic party!")
}

private fun DialogueBuilder.partyFinale(player: Player) {
    player(HeadE.CONFUSED, "How am I doing?")
    npc(PETE, HeadE.CALM_TALK, "Hmmm, let me have a think.")
    npc(PETE, HeadE.HAPPY_TALKING, "Well, thanks to you, everything is running smoothly.")
    npc(PETE, HeadE.HAPPY_TALKING, "You really must stay for the party, it's going to be AWESOME!")
    npc(PETE, HeadE.HAPPY_TALKING, "There's an amazing dancefloor, which we will clear free from spiders. Oh, and let's not forget the band, they're setting up as we speak.")
    npc(PETE, HeadE.CALM_TALK, "Shall we move onto the party? You won't be able to come back to setting up later.")
    options("<col=ff0000>Items dropped here will be lost.</col><br><col=ff0000>Do you want to continue?</col>") {
        op("Yes") {
            npc(PETE, HeadE.HAPPY_TALKING, "I can't let you go through all that effort without a special award, either.")
            npc(PETE, HeadE.CALM_TALK, "Tell you what: since you've been so helpful, take this...erm...")
            npc(PETE, HeadE.HAPPY_TALKING, "Aha! Take this brooch! Yes.")
            npc(PETE, HeadE.HAPPY_TALKING, "This brooch has magical powers: it's guaranteed to make you skinnier.")
            npc(PETE, HeadE.HAPPY_TALKING, "Just put it on and marvel at how thin you are.")
            npc(PETE, HeadE.HAPPY_TALKING, "Well, it looks like the band are ready, so why not head over to that dancefloor and enjoy yourself. You most certainly deserve it.")
            exec { playPartyCutscene(player) }
        }
        op("No")
    }
}

private fun DialogueBuilder.jackLanternGreeting(player: Player, pete: Int) {
    if (player.equipment.hatId != Obj.hw06_pumpkin_head)
        return
    when {
        player.hw10Stage >= Hw10Stage.PREPARING -> npc(pete, HeadE.HAPPY_TALKING, "Still looking good, I see.")
        player.appearance.isMale -> npc(pete, HeadE.HAPPY_TALKING, "What-ho! Now there's a good looking chap!")
        else -> npc(pete, HeadE.HAPPY_TALKING, "What-ho! Greetings, fair lady.")
    }
}
