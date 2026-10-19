package com.rs.game.content.minigames.barbarianassault

import org.darkan.core.gameval.gen.Loc
import world.gregs.voidps.type.Tile

private const val LOBBY_BASE_X = 2560
private const val LOBBY_BASE_Y = 5248

private fun lobby(x: Int, y: Int): Tile = Tile.of(LOBBY_BASE_X + x, LOBBY_BASE_Y + y, 0)

val LOBBY_ARRIVAL: Tile = lobby(33, 6)
val OUTPOST_LADDER_EXIT: Tile = Tile.of(2535, 3572, 0)

enum class WaitingRoom(
    val door: Int,
    doorX: Int,
    doorY: Int,
    insideY: Int,
    outsideY: Int,
    private val minX: Int,
    private val maxX: Int,
    private val minY: Int,
    private val maxY: Int,
    ladderX: Int,
    ladderY: Int,
) {
    ONE(Loc.barbassault_door_01, 18, 47, 48, 45, 14, 23, 47, 60, 22, 59),
    TWO(Loc.barbassault_door_02, 28, 47, 48, 45, 24, 33, 47, 60, 32, 59),
    THREE(Loc.barbassault_door_03, 39, 47, 48, 45, 34, 43, 47, 60, 42, 59),
    FOUR(Loc.barbassault_door_04, 49, 47, 48, 45, 44, 53, 47, 60, 52, 59),
    FIVE(Loc.barbassault_door_05, 18, 44, 43, 46, 13, 22, 32, 44, 21, 32),
    SIX(Loc.barbassault_door_06, 28, 44, 43, 46, 23, 32, 32, 44, 31, 32),
    SEVEN(Loc.barbassault_door_07, 39, 44, 43, 46, 35, 44, 32, 44, 43, 32),
    EIGHT(Loc.barbassault_door_08, 49, 44, 43, 46, 45, 54, 32, 44, 53, 32),
    NINE(Loc.barbassault_door_09, 18, 28, 27, 30, 13, 22, 15, 28, 21, 16),
    TEN(Loc.barbassault_door_10, 28, 28, 27, 30, 23, 32, 15, 28, 31, 16);

    val doorTile: Tile = lobby(doorX, doorY)
    val inside: Tile = lobby(doorX, insideY)
    val outside: Tile = lobby(doorX, outsideY)
    val ladder: Tile = lobby(ladderX, ladderY)
    val wave: Wave get() = Wave.entries[ordinal]

    fun contains(tile: Tile) = tile.plane == 0 &&
        tile.x - LOBBY_BASE_X in minX..maxX &&
        tile.y - LOBBY_BASE_Y in minY..maxY

    fun arrivalTile(slot: Int): Tile = inside.transform(slot % 3 - 1, if (inside.y > doorTile.y) slot / 3 else -(slot / 3))

    companion object {
        fun forDoor(loc: Int) = entries.firstOrNull { it.door == loc }

        fun forLadder(tile: Tile) = entries.firstOrNull { it.ladder == tile }

        fun containing(tile: Tile) = entries.firstOrNull { it.contains(tile) }

        fun of(wave: Wave) = entries[wave.ordinal]
    }
}
