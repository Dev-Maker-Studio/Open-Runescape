package com.rs.game.content.minigames.barbarianassault.penance

import com.rs.engine.pathfinder.routeEntityToTile
import com.rs.game.content.minigames.barbarianassault.Bait
import com.rs.game.content.minigames.barbarianassault.BarbarianAssaultGame
import com.rs.game.content.minigames.barbarianassault.PenanceFamily
import org.darkan.core.Utils
import world.gregs.voidps.type.Tile

private const val PHASES = 10
private const val WANDER_PHASE = 6
private const val WANDER_DISTANCE = 5
private const val BAD_FOOD_TICKS = 3
private const val WEST_LIMIT = 14
private const val EAST_LIMIT = 45
private const val QUEEN_EAST_LIMIT = 44
private const val ESCAPE_Y = 14
private const val ESCAPE_X = 30
private const val STUCK_TICKS = 10
private const val NORMAL_RETREAT_Y = 38
private const val QUEEN_RETREAT_Y = 30

class PenanceRunner(id: Int, tile: Tile, game: BarbarianAssaultGame, ordinal: Int) : Penance(id, tile, game, PenanceFamily.RUNNER, ordinal) {
    private var phase = 1
    private var selector = 0
    private var destination: Tile = tile
    private var target: Bait? = null
    private var badFood = 0
    private var stuck = 0
    private var following = false

    override fun tickPenance() {
        phase = phase % PHASES + 1
        if ((phase == 1 || phase == WANDER_PHASE) && local.y <= ESCAPE_Y) {
            game.runnerEscaped(this)
            return
        }
        if (phase == 1) selector = (selector + 1) % 4
        if (badFood > 0) {
            if (phase == 1 || phase == WANDER_PHASE) badFood--
            if (!stepTowards(destination)) badFood = 0
            return
        }
        val current = target?.takeIf { game.baitExists(it) }
        if (current == null && target != null) {
            target = null
            if (phase == 1) return
        }
        if (current != null) {
            if (current.tile == tile) {
                eat(current)
                return
            }
            if (!stepTowards(current.tile)) target = null
            return
        }
        if (following || searches()) {
            game.findBait(this)?.let {
                target = it
                if (it.tile == tile) eat(it) else stepTowards(it.tile)
                return
            }
            following = false
        }
        if (phase == WANDER_PHASE) destination = wanderDestination()
        stuck = if (stepTowards(destination) || destination == tile) 0 else stuck + 1
        if (stuck >= STUCK_TICKS) detour()
    }

    private fun detour() {
        val next = routeEntityToTile(this, game.local(ESCAPE_X, ESCAPE_Y)).coords.firstOrNull { it.x != x || it.y != y } ?: return
        destination = Tile.of(next.x, next.y, plane)
        stepTowards(destination)
    }

    private fun searches() = when (phase) {
        2, 5 -> selector == 2
        3, WANDER_PHASE -> selector == 3
        4 -> selector == 1
        else -> false
    }

    private fun eat(bait: Bait) {
        target = null
        if (!game.consumeBait(bait)) return
        if (!bait.good) {
            forceTalk("Blughhhh.")
            following = false
            badFood = BAD_FOOD_TICKS
            selector = 0
            if (phase > 5) phase -= 5
            destination = game.local(local.x, if (game.wave.queen) QUEEN_RETREAT_Y else NORMAL_RETREAT_Y)
            return
        }
        if (!game.springTrap(this)) {
            forceTalk("Chomp, chomp.")
            following = true
        }
    }

    private fun wanderDestination(): Tile {
        val here = local
        val funnel = when {
            here.x <= 27 && here.y in 16..17 -> game.local(30, 16)
            here.x == 25 && here.y == 15 -> game.local(26, 16)
            here.x <= 32 && here.y <= 16 -> game.local(30, 14)
            here.x > 32 && here.y in 15..16 -> game.local(31, 16)
            else -> null
        }
        if (funnel != null) return funnel
        val eastLimit = if (game.wave.queen) QUEEN_EAST_LIMIT else EAST_LIMIT
        return when (Utils.random(6)) {
            4 -> game.local((here.x - WANDER_DISTANCE).coerceAtLeast(WEST_LIMIT), here.y)
            5 -> game.local((here.x + WANDER_DISTANCE).coerceAtMost(eastLimit), here.y)
            else -> game.local(here.x, here.y - WANDER_DISTANCE)
        }
    }
}
