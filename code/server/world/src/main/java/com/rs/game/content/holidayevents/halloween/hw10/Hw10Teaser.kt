package com.rs.game.content.holidayevents.halloween.hw10

import com.rs.engine.cutscenekt.Cutscene
import com.rs.engine.cutscenekt.cutscene
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import org.darkan.core.gameval.gen.Midi
import org.darkan.core.gameval.gen.Npc

private const val PARTY_HOUSE_CHUNK_X = 67 * 8
private const val PARTY_HOUSE_CHUNK_Y = 83 * 8
private const val PARTY_HOUSE_CHUNKS = 8
private const val LINE_HOLD_TICKS = 5

private const val TEASER_LINE = "<col=ff6600>Partying is Such Sweet Sorrow</col> - the Grim Reaper's Halloween party is coming to Darkan soon!"

private class Placement(val npc: Int, val x: Int, val y: Int, val faceX: Int, val faceY: Int)

private val LIT_TILES = mapOf(
    Hw10Spider.PURPLE to (34 to 24),
    Hw10Spider.BLUE to (30 to 26),
    Hw10Spider.RED to (32 to 22),
    Hw10Spider.GREEN to (30 to 21),
    Hw10Spider.ORANGE to (29 to 24),
    Hw10Spider.TURQUOISE to (32 to 27),
    Hw10Spider.PINK to (33 to 21),
    Hw10Spider.BEIGE to (31 to 24),
)

private val PARTY_CROWD = listOf(
    Placement(Npc.hw10_helga_band, 31, 30, 31, 20),
    Placement(Npc.hw10_greta_band, 32, 31, 32, 20),
    Placement(Npc.hw10_moira_band, 33, 30, 33, 20),
    Placement(Npc.hw10_djweb, 28, 29, 30, 25),
    Placement(Npc.hw10_alan, 27, 30, 30, 25),
    Placement(Npc.hw10_dancer_headbang, 28, 26, 32, 30),
    Placement(Npc.hw10_dancer_headbang2, 34, 26, 32, 30),
    Placement(Npc.hw10_dancer_dance, 28, 22, 32, 30),
    Placement(Npc.hw10_dancer_dance2, 34, 22, 32, 30),
    Placement(Npc.hw10_dancer_dance_spin, 33, 25, 32, 30),
    Placement(Npc.hw10_party_pete, 33, 23, 32, 30),
) + LIT_TILES.map { (spider, tile) -> Placement(spider.npc, tile.first, tile.second, 32, 30) }

fun Player.playHw10Teaser() = cutscene {
    hideGameframe()
    fadeInAndWait()
    dynamicRegion(tile, PARTY_HOUSE_CHUNK_X, PARTY_HOUSE_CHUNK_Y, PARTY_HOUSE_CHUNKS, PARTY_HOUSE_CHUNKS)
    entityTeleTo(player, 25, 36)
    PARTY_CROWD.forEach { place(it) }
    val zabeth = place(Placement(Npc.hw10_black_zabeth_band, 32, 29, 32, 20))
    val grim = place(Placement(Npc.npc_15661, 31, 15, 31, 25))
    val pete = place(Placement(Npc.hw10_pumpkin_pete_2, 31, 23, 31, 17))
    wait(1)
    player.musicsManager.forcePlayMusic(Midi.midi_908)
    wait(2)

    camPos(31, 14, 2900)
    camLook(32, 26, 300)
    fadeOutAndWait()
    camPos(31, 18, 2200, 2, 12)
    wait(4)

    camPos(32, 24, 1450)
    camLook(32, 29, 400)
    wait(1)
    zabeth.forceTalk("ONE TWO THREE FOUR!")
    wait(LINE_HOLD_TICKS)

    camPos(30, 23, 1300)
    camLook(30, 27, 300)
    camPos(29, 22, 1150, 2, 12)
    wait(5)

    camPos(32, 23, 1500)
    camLook(32, 18, 500)
    entityWalkTo(grim, 31, 18)
    wait(4)
    grim.faceTile(tileFromLocal(32, 25))
    wait(1)
    grim.forceTalk("A party... for me? I don't know what to say.")
    wait(LINE_HOLD_TICKS)

    camPos(30, 20, 1500)
    camLook(31, 23, 700)
    wait(1)
    pete.forceTalk("Halloween is coming - don't miss the party of the century!")
    wait(LINE_HOLD_TICKS)
    camPos(31, 14, 2800, 2, 10)
    camLook(31, 25, 200, 4, 4)
    wait(4)

    fadeInAndWait()
    returnPlayerFromInstance()
    player.sendMessage(TEASER_LINE)
    restoreGameframe()
    player.musicsManager.reset()
    fadeOutAndWait()
}

private fun Cutscene.place(placement: Placement): NPC =
    npcCreate(placement.npc, placement.x, placement.y, 0).also { it.faceTile(tileFromLocal(placement.faceX, placement.faceY)) }
