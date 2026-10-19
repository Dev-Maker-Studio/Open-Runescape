package com.rs.game.content.holidayevents.halloween.hw10.dialogue

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.content.holidayevents.halloween.hw10.Hw10Spider
import com.rs.game.content.holidayevents.halloween.hw10.SpiderPosition
import com.rs.game.content.holidayevents.halloween.hw10.inHw10House
import com.rs.game.content.holidayevents.halloween.hw10.inPartyHouse
import com.rs.game.content.holidayevents.halloween.hw10.position
import com.rs.game.content.holidayevents.halloween.hw10.WebState
import com.rs.game.content.holidayevents.halloween.hw10.edgarSceneWatched
import com.rs.game.content.holidayevents.halloween.hw10.findSpider
import com.rs.game.content.holidayevents.halloween.hw10.hasEek
import com.rs.game.content.holidayevents.halloween.hw10.spiderRunsOff
import com.rs.game.content.holidayevents.halloween.hw10.webState
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.managers.EmotesManager.Emote
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onEmote
import com.rs.plugin.kts.onNpcClick
import org.darkan.core.gameval.gen.Npc
import world.gregs.voidps.type.Tile

private const val SPIDER = Npc.hw10_spider_start_6
private const val EDGAR = Npc.hw10_edgar

private val SPIDER_TILE = Tile.of(4319, 5456, 1)
private const val TRICK_RANGE = 4

@ServerStartupEvent
fun mapTurquoiseSpider() {
    onNpcClick(Npc.npc_12420, options = arrayOf("Talk-to")) { (player) -> player.startConversation { talkToTurquoiseSpider(player) } }

    onNpcClick(Npc.npc_12420, options = arrayOf("Scare")) { (player) -> scareTurquoiseSpider(player) }

    onEmote(Emote.TRICK) { (player) ->
        if (player.inPartyHouse || !player.inHw10House || !player.tile.withinDistance(SPIDER_TILE, TRICK_RANGE))
            return@onEmote
        if (player.position(Hw10Spider.TURQUOISE) == SpiderPosition.HIDING)
            scareTurquoiseSpider(player)
        else
            player.startConversation {
                npc(EDGAR, HeadE.SAD, "*Sigh*")
                npc(EDGAR, HeadE.SAD, "I've seen more frightening things.")
            }
    }
}

private fun scareTurquoiseSpider(player: Player) {
    when {
        player.webState == WebState.NOT_ASKED -> player.startConversation { player(HeadE.CONFUSED, "Why would I want to scare that spider? It's not doing anyone any harm.") }
        !player.edgarSceneWatched -> player.startConversation { npc(EDGAR, HeadE.SAD, "Maybe you should try talking to people before trying to scare them. *Sigh*") }
        else -> {
            val allFound = player.findSpider(Hw10Spider.TURQUOISE)
            player.startConversation {
                player(HeadE.ANGRY, "Boo!")
                npc(SPIDER, HeadE.SPIDER_EXCLAIM, "Aaaaargh!")
                player(HeadE.HAPPY_TALKING, "Well, that seemed to work.")
                spiderRunsOff(player, allFound)
            }
        }
    }
}

fun DialogueBuilder.talkToTurquoiseSpider(player: Player) {
    when {
        player.webState == WebState.NOT_ASKED -> {
            npc(EDGAR, HeadE.CALM_TALK, "...Nevermore.")
            npc(SPIDER, HeadE.SPIDER_CALM, "Shhh! He's just getting to the scary bit!")
        }
        player.edgarSceneWatched -> {
            wantsAScare(player)
            npc(EDGAR, HeadE.SAD, "*Sigh*")
        }
        else -> edgarScene(player)
    }
}

private fun DialogueBuilder.edgarScene(player: Player) {
    npc(EDGAR, HeadE.CALM_TALK, "...Nevermore.")
    npc(SPIDER, HeadE.SPIDER_CALM, "Ooooh, and then what happened?")
    npc(EDGAR, HeadE.CALM_TALK, "Only this and nothing more.")
    npc(SPIDER, HeadE.SPIDER_CALM, "Oooooh I see, then what happened?")
    if (player.hasEek)
        eek("I want to know!")
    npc(EDGAR, HeadE.SAD, "*Sigh*")
    npc(EDGAR, HeadE.SAD, "No one understands real horror.")
    npc(EDGAR, HeadE.CALM_TALK, "It's horror of the imagination: he's creating all his ills and his worries, not some monstrous creature.")
    npc(SPIDER, HeadE.SPIDER_CALM, "It would work better with monsters.")
    npc(EDGAR, HeadE.SAD, "*Sigh*")
    player(HeadE.CALM_TALK, "Sorry to interject here, but can I get the spider to head back to the dancefloor?")
    wantsAScare(player)
    if (player.hasEek) {
        eek("Boo! Ha ha ha ha ha ha!")
        eek("Did that scare you?")
        npc(SPIDER, HeadE.SPIDER_CALM, "No, that was rubbish.")
        eek("Awww.")
        eek("I bet ${player.displayName} can scare you!")
    }
}

private fun DialogueBuilder.wantsAScare(player: Player) =
    npc(SPIDER, HeadE.SPIDER_SAD, "Nooo, I don't want to go! Not until I've had a really good scare.", Runnable { player.edgarSceneWatched = true })
