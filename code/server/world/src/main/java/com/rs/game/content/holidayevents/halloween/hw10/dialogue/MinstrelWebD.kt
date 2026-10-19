package com.rs.game.content.holidayevents.halloween.hw10.dialogue

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Spider
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Task
import com.rs.game.content.holidayevents.halloween.hw10.WebState
import com.rs.game.content.holidayevents.halloween.hw10.did2009
import com.rs.game.content.holidayevents.halloween.hw10.hasEek
import com.rs.game.content.holidayevents.halloween.hw10.hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.inPartyHouse
import com.rs.game.content.holidayevents.halloween.hw10.spiderPuzzleSolved
import com.rs.game.content.holidayevents.halloween.hw10.spidersMoved
import com.rs.game.content.holidayevents.halloween.hw10.spidersFound
import com.rs.game.content.holidayevents.halloween.hw10.webState
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc

private const val WEB = Npc.hw10_djweb
private const val ALLAN = Npc.hw10_alan
private const val W1 = "w1"
private const val PARTY_MENU = "party"
private const val AGAIN = "again"
private const val AFTER_TASK = "afterTask"

@ServerStartupEvent
fun mapMinstrelWeb() {
    onNpcClick(WEB, options = arrayOf("Talk-to")) { (player) -> talkToMinstrelWeb(player) }
}

private fun talkToMinstrelWeb(player: Player) = player.startConversation {
    when {
        player.inPartyHouse || player.hw10Stage >= Hw10Stage.PARTY -> partyMenu()
        player.hw10Stage < Hw10Stage.PREPARING -> npc(WEB, HeadE.SPIDER_CALM, "Busy.")
        else -> when (player.webState) {
            WebState.NOT_ASKED -> firstTalk(player)
            WebState.SEARCHING -> if (player.spidersFound < Hw10Spider.entries.size) searching(player) else allFound(player)
            WebState.PUZZLE -> when {
                player.spiderPuzzleSolved -> puzzleSolved(player)
                player.spidersMoved -> {
                    player(HeadE.CONFUSED, "Are they all in the right positions now?")
                    npc(WEB, HeadE.SPIDER_CALM, "Nope.")
                }
                else -> webMenu(player, puzzle = true)
            }
            WebState.SOLVED -> afterTask(player)
        }
    }
}

private fun DialogueBuilder.firstTalk(player: Player) {
    player(HeadE.CALM_TALK, "Hello there.")
    npc(WEB, HeadE.SPIDER_CALM, "'Lo.")
    if (player.hasEek) {
        eek("Hi there, big spider!")
        npc(WEB, HeadE.SPIDER_CALM, "'Lo.")
        eek("You're enormous!")
        eek("Is it because you ate all the flies?")
        npc(WEB, HeadE.SPIDER_CALM, "Yup.")
    }
    label(W1)
    options {
        op("Ahh, the stoic silent type.") { stoic() }
        op("Need any help?") { needHelp(player) }
        op("Who are you?") { whoAreYou() }
        if (player.did2009)
            op("Aren't you the spider from last year?") {
                lastYear(player, puzzle = false)
                goto(W1)
            }
    }
}

private fun DialogueBuilder.needHelp(player: Player) {
    player(HeadE.CALM_TALK, "Need any help?")
    npc(WEB, HeadE.SPIDER_CALM, "Yes.")
    player(HeadE.CONFUSED, "Erm...care to tell me what?")
    npc(WEB, HeadE.SPIDER_CALM, "Spiders.")
    options {
        op("Can you be more specific?") {
            player(HeadE.CONFUSED, "Any chance of being more specific about these spiders?")
            npc(WEB, HeadE.SPIDER_CALM, "Assistants.")
            player(HeadE.CONFUSED, "Ahh, so they were helping you with your...erm...music?")
            npc(WEB, HeadE.SPIDER_CALM, "Yup.")
            player(HeadE.CONFUSED, "How?")
            npc(WEB, HeadE.SPIDER_CALM, "See lights?")
            player(HeadE.CALM_TALK, "Erm, yes. On the dancefloor?")
            npc(WEB, HeadE.SPIDER_CALM, "They stand there.")
            player(HeadE.CONFUSED, "That helps you with your music?")
            npc(WEB, HeadE.SPIDER_CALM, "Yup.")
            player(HeadE.CONFUSED, "Spiders?")
            npc(WEB, HeadE.SPIDER_CALM, "Yup.")
            spiderSearchBrief(player)
        }
        op("Spiders?") {
            player(HeadE.CONFUSED, "Spiders?")
            npc(WEB, HeadE.SPIDER_CALM, "Yup.")
            spiderSearchBrief(player)
        }
    }
}

private fun DialogueBuilder.spiderSearchBrief(player: Player) {
    player(HeadE.CONFUSED, "So, there are other spiders around?")
    npc(WEB, HeadE.SPIDER_CALM, "Yup.")
    player(HeadE.CONFUSED, "How many?")
    npc(WEB, HeadE.SPIDER_CALM, "Eight.")
    player(HeadE.CONFUSED, "Where?")
    npc(WEB, HeadE.SPIDER_CALM, "Hiding.")
    player(HeadE.CONFUSED, "Where are they hiding?")
    npc(WEB, HeadE.SPIDER_CALM, "Dunno.")
    if (player.hasEek) {
        eek("They're probably hiding in dark places. Thats where I would hide, so I could be a scary spider!")
        eek("You'd never see me coming. All of a sudden I'd be like 'RARGH! I'm going to eat your eyeballs' and you'd all be like 'Aaaaargh, a scary spider!'.")
        eek("Hahahahahahaha.")
    } else {
        npc(WEB, HeadE.SPIDER_CALM, "Dark places.")
        npc(WEB, HeadE.SPIDER_CALM, "Go look.")
    }
    player(HeadE.CALM_TALK, "I'll go see if I can find them then, shall I?")
    npc(WEB, HeadE.SPIDER_CALM, "Yup.")
    player(HeadE.CALM_TALK, "Okay, I'm on it. I'll be right back.", Runnable { player.webState = WebState.SEARCHING })
}

private fun DialogueBuilder.searching(player: Player) {
    player(HeadE.CONFUSED, "Have I found them all yet?")
    npc(WEB, HeadE.SPIDER_CALM, "Nope.")
    player(HeadE.CONFUSED, "How many do I have left?")
    npc(WEB, HeadE.SPIDER_CALM, "${Hw10Spider.entries.size - player.spidersFound}.")
    player(HeadE.CALM_TALK, "I'll keep looking, then.")
    webMenu(player, puzzle = false)
}

private fun DialogueBuilder.allFound(player: Player) {
    player(HeadE.CONFUSED, "Have I found them all yet?")
    npc(WEB, HeadE.SPIDER_CALM, "Yup.")
    player(HeadE.CONFUSED, "What now?")
    npc(WEB, HeadE.SPIDER_CALM, "Fix music.")
    player(HeadE.CONFUSED, "How do we do that?")
    npc(WEB, HeadE.SPIDER_CALM, "Get 'em under lights.")
    player(HeadE.CONFUSED, "How?")
    npc(WEB, HeadE.SPIDER_CALM, "Kick 'em.")
    if (player.hasEek) {
        eek("No, that's not very nice!")
        eek("Don't kick them, that would be mean.")
        eek("You could write them a strongly worded letter. Or you could make big scary faces like this.")
        eek("RARGH! I'm a big scary monster! RARGH!")
    }
    player(HeadE.CALM_TALK, "I'll try a less violent approach first.")
    npc(WEB, HeadE.SPIDER_CALM, "S'pose.", Runnable { player.webState = WebState.PUZZLE })
    npc(ALLAN, HeadE.CALM_TALK, "Psst, I'll help you out if you get a bit stuck.")
    webMenu(player, puzzle = true)
}

private fun DialogueBuilder.puzzleSolved(player: Player) {
    player(HeadE.CONFUSED, "Are they all in the right positions now?")
    npc(WEB, HeadE.SPIDER_CALM, "Yup.")
    player(HeadE.CONFUSED, "So, is that it?")
    npc(WEB, HeadE.SPIDER_CALM, "Yup.")
    player(HeadE.SKEPTICAL, "I take it a 'thanks' is out of the question?")
    simple("[The spider seems to be ignoring you now.]")
    player(HeadE.CALM_TALK, "No, no - you're welcome.", Runnable { player.webState = WebState.SOLVED })
    npc(ALLAN, HeadE.CALM_TALK, "We really are rather grateful. Miss Web is just...very busy right now.")
    npc(ALLAN, HeadE.HAPPY_TALKING, "However, we should be able to get the music back on track now. Thank you.")
    checkInWithPete(player, Hw10Task.SPIDERS)
}

private fun DialogueBuilder.webMenu(player: Player, puzzle: Boolean) {
    options {
        op("Ahh, the stoic, silent type.") { stoic() }
        op("Who are you?") { whoAreYou() }
        if (player.did2009)
            op("Aren't you the spider from last year?") {
                lastYear(player, puzzle)
                webMenuAgain(player, puzzle)
            }
        op(if (puzzle || player.did2009) "I should get going." else "[Leave]")
    }
}

private fun DialogueBuilder.afterTask(player: Player) {
    player(HeadE.HAPPY_TALKING, "Hi there!")
    simple("[The spider seems to be ignoring you]")
    label(AFTER_TASK)
    options {
        op("Ahh, the stoic, silent type.") { stoic() }
        op("Who are you?") { whoAreYou() }
        if (player.did2009)
            op("Aren't you the spider from last year?") {
                lastYear(player, puzzle = false)
                goto(AFTER_TASK)
            }
        op("I'll be leaving now.")
    }
}

private fun DialogueBuilder.webMenuAgain(player: Player, puzzle: Boolean) {
    label(AGAIN)
    options {
        op("Ahh, the stoic, silent type.") { stoic() }
        op("Who are you?") { whoAreYou() }
        op("Aren't you the spider from last year?") {
            lastYear(player, puzzle)
            goto(AGAIN)
        }
        op(if (puzzle) "I have to get going." else "I have to leave.")
    }
}

private fun DialogueBuilder.partyMenu() {
    label(PARTY_MENU)
    options {
        op("This music is great.") {
            player(HeadE.HAPPY_TALKING, "This music is great.")
            npc(WEB, HeadE.SPIDER_CALM, "Yup.")
            player(HeadE.CONFUSED, "Too busy to talk?")
            npc(WEB, HeadE.SPIDER_CALM, "Yup.")
            goto(PARTY_MENU)
        }
        op("This music is terrible.") {
            player(HeadE.SKEPTICAL, "This music is terrible.")
            simple("[She makes angry clicking noises.]")
            player(HeadE.SCARED, "*Gulp* Erm, gotta run.")
            goto(PARTY_MENU)
        }
        op("Having fun?") {
            player(HeadE.HAPPY_TALKING, "Having fun?")
            npc(WEB, HeadE.SPIDER_CALM, "Yup.")
            goto(PARTY_MENU)
        }
    }
}

private fun DialogueBuilder.stoic() {
    player(HeadE.CALM_TALK, "Ahhh, the stoic, silent type, are you?")
    npc(WEB, HeadE.SPIDER_CALM, "Huh?")
    player(HeadE.CALM_TALK, "Stoic, as in steadfast...resolute and...erm...phlegmatic.")
    npc(WEB, HeadE.SPIDER_CALM, "D'un care.")
}

private fun DialogueBuilder.whoAreYou() {
    player(HeadE.CONFUSED, "Who are you?")
    npc(WEB, HeadE.SPIDER_CALM, "Minstrel Web.")
}

private fun DialogueBuilder.lastYear(player: Player, puzzle: Boolean) {
    player(HeadE.CONFUSED, "Aren't you the spider who spent last Halloween in Grim's bathtub?")
    npc(WEB, HeadE.SPIDER_CALM, "Yup.")
    player(HeadE.CONFUSED, "No Spider Queen this year?")
    npc(WEB, HeadE.SPIDER_CALM, "Nope.")
    if (player.hasEek) {
        eek("Maybe we should invite her to the party.")
        eek("On second thought, maybe not.")
    } else if (puzzle)
        npc(WEB, HeadE.SPIDER_CALM, "Bored.")
}
