package com.rs.game.content.holidayevents.halloween.hw10

import com.rs.engine.dialogue.DialogueBuilder
import com.rs.engine.dialogue.HeadE
import com.rs.engine.dialogue.startConversation
import com.rs.game.World
import com.rs.game.content.holidayevents.halloween.hw09.SpiderStatement
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.managers.EmotesManager.Emote
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.getInteractionDistance
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Varbit
import world.gregs.voidps.type.Tile

enum class Hw10Spider(
    val colour: String,
    val varbit: Int,
    val npc: Int,
    val offTileParent: Int,
    val onTileParent: Int,
    val offTile: Tile,
    val litTile: Tile,
    val hidingSpot: Tile? = null,
) {
    PURPLE("Purple", Varbit.hw10_spider_1, Npc.hw10_spider_1, Npc.npc_12402, Npc.npc_12410, Tile.of(4322, 5465, 0), Tile.of(4322, 5464, 0), Tile.of(4323, 5458, 0)),
    BLUE("Blue", Varbit.hw10_spider_2, Npc.hw10_spider_2, Npc.npc_12403, Npc.npc_12411, Tile.of(4317, 5466, 0), Tile.of(4318, 5466, 0), Tile.of(4323, 5468, 1)),
    RED("Red", Varbit.hw10_spider_3, Npc.hw10_spider_3, Npc.npc_12404, Npc.npc_12412, Tile.of(4320, 5463, 0), Tile.of(4320, 5462, 0), Tile.of(4323, 5465, 1)),
    GREEN("Green", Varbit.hw10_spider_4, Npc.hw10_spider_4, Npc.npc_12405, Npc.npc_12413, Tile.of(4317, 5461, 0), Tile.of(4318, 5461, 0), Tile.of(4328, 5475, 1)),
    ORANGE("Orange", Varbit.hw10_spider_5, Npc.hw10_spider_5, Npc.npc_12406, Npc.npc_12414, Tile.of(4316, 5464, 0), Tile.of(4317, 5464, 0)),
    TURQUOISE("Turquoise", Varbit.hw10_spider_6, Npc.hw10_spider_6, Npc.npc_12407, Npc.npc_12415, Tile.of(4321, 5467, 0), Tile.of(4320, 5467, 0)),
    PINK("Pink", Varbit.hw10_spider_7, Npc.hw10_spider_7, Npc.npc_12408, Npc.npc_12416, Tile.of(4322, 5461, 0), Tile.of(4321, 5461, 0)),
    BEIGE("Beige", Varbit.hw10_spider_8, Npc.hw10_spider_8, Npc.npc_12409, Npc.npc_12417, Tile.of(4319, 5465, 0), Tile.of(4319, 5464, 0));

    val toggles: List<Hw10Spider>
        get() = when (this) {
            BLUE -> listOf(BEIGE, RED)
            PINK -> listOf(PINK, ORANGE)
            ORANGE -> listOf(TURQUOISE)
            PURPLE -> listOf(PURPLE, PINK)
            RED -> listOf(ORANGE)
            TURQUOISE -> listOf(RED, BEIGE)
            BEIGE -> listOf(GREEN, PURPLE)
            GREEN -> listOf(GREEN, BLUE)
        }

    companion object {
        val byFloorParent = entries.flatMap { listOf(it.offTileParent to it, it.onTileParent to it) }.toMap()
        val byHidingSpot = entries.filter { it.hidingSpot != null }.associateBy { it.hidingSpot!! }
    }
}

private val SEARCHABLE_LOCS: Map<Int, List<Tile>> = mapOf(
    Loc.hw09_spooky_clock to listOf(Tile.of(4309, 5459, 0), Tile.of(4323, 5465, 1), Tile.of(4328, 5461, 1)),
    Loc.hw09_cabinet_offsetx to listOf(Tile.of(4314, 5472, 0), Tile.of(4315, 5475, 0), Tile.of(4323, 5454, 0), Tile.of(4323, 5456, 0), Tile.of(4323, 5459, 1), Tile.of(4334, 5471, 1)),
    Loc.hw09_wardrobe to listOf(Tile.of(4318, 5473, 1), Tile.of(4320, 5473, 1)),
    Loc.hw09_wardrobe_offsetz to listOf(Tile.of(4329, 5457, 1)),
    Loc.hw09_dresser to listOf(Tile.of(4334, 5457, 0), Tile.of(4319, 5475, 1)),
    Loc.hw10_cabinet to listOf(Tile.of(4316, 5459, 1)),
    Loc.hw10_foot_locker to listOf(Tile.of(4316, 5460, 1)),
    Loc.hw10_clock_1 to listOf(Tile.of(4323, 5458, 0)),
    Loc.hw10_towel_1 to listOf(Tile.of(4328, 5475, 1)),
    Loc.hw10_bed to listOf(Tile.of(4329, 5461, 1)),
    Loc.hw10_four_poster_bed to listOf(Tile.of(4323, 5468, 1)),
)

private val LOOK_UNDER = intArrayOf(Loc.hw10_bed, Loc.hw10_four_poster_bed)

private const val DANCE_FLOOR_REACH = 7

private const val RUNS_OFF = "The spider runs off to the dancefloor."

fun Player.position(spider: Hw10Spider): SpiderPosition = SpiderPosition.entries[vars.getVarBit(spider.varbit).coerceAtMost(SpiderPosition.entries.lastIndex)]

private fun Player.setPosition(spider: Hw10Spider, position: SpiderPosition) = vars.saveVarBit(spider.varbit, position.ordinal)

val Player.spidersFound get() = Hw10Spider.entries.count { position(it) != SpiderPosition.HIDING }

val Player.spidersMoved get() = Hw10Spider.entries.any { position(it) == SpiderPosition.ON_TILE }

val Player.spiderPuzzleSolved get() = Hw10Spider.entries.all { position(it) == SpiderPosition.ON_TILE }

fun Player.resetSpiderPuzzle() = Hw10Spider.entries.forEach { setPosition(it, SpiderPosition.OFF_TILE) }

fun Player.solveSpiderPuzzle() = Hw10Spider.entries.forEach { setPosition(it, SpiderPosition.ON_TILE) }

fun Player.findSpider(spider: Hw10Spider): Boolean {
    setPosition(spider, SpiderPosition.OFF_TILE)
    return spidersFound == Hw10Spider.entries.size
}

fun DialogueBuilder.spiderRunsOff(player: Player, allFound: Boolean) {
    if (allFound)
        player(HeadE.CALM_TALK, "Okay, I think that's all of them. Best head back to the dancefloor.", Runnable { player.sendMessage(RUNS_OFF) })
    else
        exec { player.sendMessage(RUNS_OFF) }
}

@ServerStartupEvent
fun mapSpiderPuzzle() {
    onObjectClick(*SEARCHABLE_LOCS.keys.toTypedArray<Any>(), tiles = SEARCHABLE_LOCS.values.flatten().flatMap { listOf(it, it.inPartyHouse()) }.toTypedArray()) { (player, obj) ->
        searchHidingSpot(player, obj.id, Hw10Spider.byHidingSpot[obj.tile])
    }

    onNpcClick(*Hw10Spider.byFloorParent.keys.toTypedArray<Any>(), options = arrayOf("Move")) { (player, npc) ->
        moveFloorSpider(player, Hw10Spider.byFloorParent.getValue(npc.id))
    }

    getInteractionDistance(*Hw10Spider.byFloorParent.keys.toTypedArray<Any>()) { _, _ -> DANCE_FLOOR_REACH }

    onNpcClick(*Hw10Spider.entries.map { it.npc }.toTypedArray<Any>(), options = arrayOf("Move")) { (player) ->
        player.startConversation { player(HeadE.HAPPY_TALKING, "They look happy enough where they are. I'll leave them to enjoy the party.") }
    }

    onNpcClick(Npc.npc_12419, options = arrayOf("Move")) { (player) ->
        shooSpider(player, Hw10Spider.ORANGE) {
            player(HeadE.CALM_TALK, "Shouldn't you be on the dancefloor?")
            player(HeadE.CALM_TALK, "Go on, shoo.")
        }
    }

    onNpcClick(Npc.npc_12421, options = arrayOf("Interact")) { (player) ->
        shooSpider(player, Hw10Spider.PINK) { player(HeadE.ANGRY, "Oi! Get back to work, you!") }
    }

    onNpcClick(Npc.npc_12422, options = arrayOf("Interact")) { (player) ->
        shooSpider(player, Hw10Spider.BEIGE) { player(HeadE.ANGRY, "Go, spider! Flee before me!") }
    }
}

private fun searchHidingSpot(player: Player, loc: Int, spider: Hw10Spider?) {
    val line = when {
        player.inPartyHouse -> emptySpotLine(loc)
        player.webState == WebState.NOT_ASKED -> "I probably shouldn't be poking around Grim's house just yet."
        player.spidersFound == Hw10Spider.entries.size -> "I've found all the spiders. There's no need to keep looking."
        spider != null && player.position(spider) != SpiderPosition.HIDING -> "I've already found that spider."
        spider != null -> null
        Hw10Spider.entries.none { it.hidingSpot != null && player.position(it) == SpiderPosition.HIDING } -> "They can't all be hiding, surely? Maybe I should look around a bit more."
        else -> emptySpotLine(loc)
    }
    if (line != null || spider == null) {
        player.startConversation { player(HeadE.CONFUSED, line ?: emptySpotLine(loc)) }
        return
    }
    World.sendSpotAnim(spider.hidingSpot!!, Spotanim.hw10_giant_spider_run_to_fade)
    val allFound = player.findSpider(spider)
    val learnsTerrified = player.emotesManager.unlockEmote(Emote.SCARED)
    player.startConversation {
        player(HeadE.SCARED, "Eek! A huge spider just leapt out and ran off.")
        if (learnsTerrified)
            emoteLearned("Terrified")
        if (player.hasEek)
            statement(SpiderStatement("I saw! No need to shout."))
        spiderRunsOff(player, allFound)
    }
}

private fun emptySpotLine(loc: Int) = if (loc in LOOK_UNDER) "No spiders hiding under here." else "Hmmm. There don't appear to be any spiders in there."

private fun shooSpider(player: Player, spider: Hw10Spider, lines: DialogueBuilder.() -> Unit) {
    if (player.webState == WebState.NOT_ASKED) {
        player.startConversation { player(HeadE.CONFUSED, "That's a big spider. I wonder what it's doing here; maybe someone in the house knows.") }
        return
    }
    val allFound = player.findSpider(spider)
    player.startConversation {
        lines()
        spiderRunsOff(player, allFound)
    }
}

private fun moveFloorSpider(player: Player, clicked: Hw10Spider) {
    when (player.webState) {
        WebState.NOT_ASKED, WebState.SEARCHING -> player.startConversation {
            if (player.spidersFound < Hw10Spider.entries.size)
                player(HeadE.CALM_TALK, "I've already found this spider.")
            else
                player(HeadE.CALM_TALK, "I should probably speak to Minstrel Web first: she might get annoyed otherwise.")
        }
        WebState.SOLVED -> player.startConversation { player(HeadE.CALM_TALK, "They're all in the right place. I'd better leave them be.") }
        WebState.PUZZLE -> {
            if (!player.spiderPuzzleSolved)
                for (spider in clicked.toggles)
                    player.setPosition(spider, if (player.position(spider) == SpiderPosition.ON_TILE) SpiderPosition.OFF_TILE else SpiderPosition.ON_TILE)
            if (player.spiderPuzzleSolved)
                player.startConversation { player(HeadE.HAPPY_TALKING, "They all seem to be in the right place. I should go and speak to Minstrel Web.") }
        }
    }
}
