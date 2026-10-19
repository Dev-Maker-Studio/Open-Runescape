package com.rs.game.content.holidayevents.halloween.hw10.dialogue

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.WebState
import com.rs.game.content.holidayevents.halloween.hw10.hasEek
import com.rs.game.content.holidayevents.halloween.hw10.hw10Stage
import com.rs.game.content.holidayevents.halloween.hw10.inPartyHouse
import com.rs.game.content.holidayevents.halloween.hw10.resetSpiderPuzzle
import com.rs.game.content.holidayevents.halloween.hw10.webState
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc

private const val ALLAN = Npc.hw10_alan
private const val AFTER_HINT = "afterHint"
private const val AFTER_MUSIC = "afterMusic"
private const val AFTER_SPIDER_JOB = "afterSpiderJob"
private const val AFTER_EDGAR = "afterEdgar"

@ServerStartupEvent
fun mapAllan() {
    onNpcClick(ALLAN, options = arrayOf("Talk-to")) { (player) ->
        player.startConversation {
            when {
                player.inPartyHouse || player.hw10Stage >= Hw10Stage.PARTY || player.webState == WebState.SOLVED -> thanks(player)
                player.webState == WebState.PUZZLE -> puzzleHelp(player)
                else -> {
                    npc(ALLAN, HeadE.HAPPY_TALKING, "Oh, isn't everything wonderful?")
                    npc(ALLAN, HeadE.CALM_TALK, "Sorry, I was talking to myself. You should probably speak to Miss Web here: she generally doesn't like it when I talk too much.")
                }
            }
        }
    }
}

private fun DialogueBuilder.puzzleHelp(player: Player) {
    options {
        op("Can you reset the spiders for me?") { reset(player) }
        op("Can you give me a hint?") {
            hint()
            label(AFTER_HINT)
            options {
                op("Can you reset the spiders for me?") { reset(player) }
                op("How are spiders helping with music?") {
                    spidersAndMusic(player)
                    goto(AFTER_MUSIC)
                }
                op("I must be on my way.")
            }
        }
        op("How are spiders helping with music?") {
            spidersAndMusic(player)
            label(AFTER_MUSIC)
            options {
                op("Can you reset the spiders for me?") { reset(player) }
                op("Can you give me a hint?") {
                    hint()
                    goto(AFTER_HINT)
                }
                op("I have to leave.")
            }
        }
    }
}

private fun DialogueBuilder.reset(player: Player) {
    player(HeadE.CONFUSED, "Can you reset the spiders for me?")
    npc(ALLAN, HeadE.CALM_TALK, "If you're sure, I can send them back to their original positions on the dancefloor.")
    options {
        op("Yes, I'm sure.") {
            player(HeadE.CALM_TALK, "Yes, I'm sure.")
            npc(ALLAN, HeadE.CALM_TALK, "Okay, they're all back to where they started from.", Runnable { player.resetSpiderPuzzle() })
        }
        op("No, I'll keep at it.") {
            player(HeadE.CALM_TALK, "No, I'll keep at it.")
            npc(ALLAN, HeadE.CALM_TALK, "Very well. Just give me a shout if you change your mind.")
        }
    }
}

private fun DialogueBuilder.hint() {
    player(HeadE.CONFUSED, "Can you give me a hint?")
    npc(ALLAN, HeadE.CALM_TALK, "Trying to move one spider may cause other spiders to move. Have a look at how each spider moves when you try to move the different spiders.")
    npc(ALLAN, HeadE.CALM_TALK, "Each spider only moves between two places on the dancefloor, so you should be able to work it out.")
}

private fun DialogueBuilder.spidersAndMusic(player: Player) {
    player(HeadE.CONFUSED, "How are spiders helping with music?")
    npc(ALLAN, HeadE.CALM_TALK, "It's quite simple really. Each spider represents an instrument that Minstrel Web is playing. Their position on the dancefloor tells Minstrel Web which instrument needs to be played. It's like notes on a piece of paper,")
    npc(ALLAN, HeadE.CALM_TALK, "but more colourful.")
    npc(ALLAN, HeadE.HAPPY_TALKING, "It's obvious if you speak fluent spider.")
    if (player.hasEek) {
        eek("I speak fluent spider.")
        eek("And raven.")
        eek("And goblin.")
        eek("Okay, goblin was a lie, but I am learning it, honest.")
        eek("The raven bit might also be a lie.")
    }
}

private fun DialogueBuilder.thanks(player: Player) {
    npc(ALLAN, HeadE.HAPPY_TALKING, "Thank you once again for your help, ${player.displayName}. I don't know what we'd have done without you.")
    options {
        op("How did you end up working for a spider?") {
            spiderJob()
            label(AFTER_SPIDER_JOB)
            options {
                op("Do you know Edgar?") {
                    edgar()
                    goto(AFTER_EDGAR)
                }
                op("I must be on my way.")
            }
        }
        op("Do you know Edgar?") {
            edgar()
            label(AFTER_EDGAR)
            options {
                op("How did you end up working for a spider?") {
                    spiderJob()
                    goto(AFTER_SPIDER_JOB)
                }
                op("I must be on my way.")
            }
        }
    }
}

private fun DialogueBuilder.spiderJob() {
    player(HeadE.CONFUSED, "How did you end up working for a spider?")
    npc(ALLAN, HeadE.HAPPY_TALKING, "Oh it's quite simple really. I found the job on the web.")
    player(HeadE.CONFUSED, "Huh?")
    npc(ALLAN, HeadE.CALM_TALK, "I may have accidently flown into her web and it was either help her with the party, or be eaten. Not a hard choice, really.")
}

private fun DialogueBuilder.edgar() {
    player(HeadE.CONFUSED, "Do you know Edgar?")
    npc(ALLAN, HeadE.CALM_TALK, "The raven upstairs?")
    player(HeadE.CALM_TALK, "That's the one.")
    npc(ALLAN, HeadE.CALM_TALK, "Nope, never met him.")
    npc(ALLAN, HeadE.LAUGH, "Only kidding. He's my brother. He came to help out with the spiders...sort of. In hindsight, inviting him wasn't the best plan.")
    npc(ALLAN, HeadE.SAD, "He has a habit of bringing the mood down.")
}
