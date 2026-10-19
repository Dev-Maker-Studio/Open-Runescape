package com.rs.game.content.minigames.fistofguthix

import world.gregs.voidps.type.Tile
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

val ARENA_ORIGIN: Tile = Tile.of(1616, 5648, 0)
private const val ARENA_SIZE_X = 96
private const val ARENA_SIZE_Y = 88
private const val CENTRE_X = 48.0
private const val CENTRE_Y = 48.0
private const val CENTRE_RADIUS = 2

private val CARDINALS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

private fun arena(x: Int, y: Int): Tile = ARENA_ORIGIN.transform(x, y, 0)

class House(val portal: Tile) {
    val barriers: List<Tile> = CARDINALS.map { (dx, dy) -> portal.transform(dx * 2, dy * 2, 0) }

    fun inside(tile: Tile) = tile.plane == portal.plane && max(abs(tile.x - portal.x), abs(tile.y - portal.y)) <= 1 && tile != portal

    fun interior(): List<Tile> = CARDINALS.map { (dx, dy) -> portal.transform(dx, dy, 0) }

    fun innerSide(barrier: Tile): Tile = portal.transform((barrier.x - portal.x) / 2, (barrier.y - portal.y) / 2, 0)
}

object FogArena {
    val houses = listOf(House(arena(35, 55)), House(arena(51, 56)), House(arena(40, 35)), House(arena(60, 40)))
    val dispensers = listOf(
        arena(10, 49), arena(18, 25), arena(22, 73), arena(45, 12),
        arena(45, 83), arena(71, 67), arena(78, 28), arena(84, 47),
    )
    val centreTiles: List<Tile> = (-CENTRE_RADIUS until CENTRE_RADIUS).flatMap { dx -> (-CENTRE_RADIUS until CENTRE_RADIUS).map { dy -> arena(CENTRE_X.toInt() + dx, CENTRE_Y.toInt() + dy) } }

    fun contains(tile: Tile) = tile.plane == ARENA_ORIGIN.plane && tile.x - ARENA_ORIGIN.x in 0 until ARENA_SIZE_X && tile.y - ARENA_ORIGIN.y in 0 until ARENA_SIZE_Y

    fun distanceFromCentre(tile: Tile) = hypot(tile.x - ARENA_ORIGIN.x + 0.5 - CENTRE_X, tile.y - ARENA_ORIGIN.y + 0.5 - CENTRE_Y)

    fun chargeRate(tile: Tile): Int {
        val distance = distanceFromCentre(tile)
        return when {
            distance < 4 -> 28
            distance < 8 -> 21
            distance < 18 -> 15
            distance < 26 -> 10
            distance < 34 -> 6
            else -> 3
        }
    }

    fun tilesWithin(minDistance: Double, maxDistance: Double): List<Tile> = (0 until ARENA_SIZE_X).flatMap { x -> (0 until ARENA_SIZE_Y).map { y -> arena(x, y) } }
        .filter { distanceFromCentre(it) in minDistance..maxDistance }

    fun houseAt(tile: Tile) = houses.firstOrNull { it.inside(tile) }

    fun houseOfBarrier(tile: Tile) = houses.firstOrNull { tile in it.barriers }
}
