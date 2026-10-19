package com.rs.game.content.minigames.pest.npcs

import com.rs.engine.pathfinder.routeEntityToTile
import com.rs.engine.pathfinder.walkRoute
import com.rs.game.content.minigames.pest.PestControlGame
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.npc.NPC
import com.rs.utils.WorldUtil
import org.darkan.core.Utils
import world.gregs.voidps.type.Tile

private const val AGGRO_DISTANCE = 10
private const val KNIGHT_SIGHT = 15
private const val ADVANCE_TURNS = 25
private const val ADVANCE_INTERVAL = 4

enum class PestFamily(val spawnWeight: Int, val attacksKnight: Boolean) {
    SPLATTER(2, false),
    SHIFTER(3, true),
    RAVAGER(2, false),
    SPINNER(2, false),
    TORCHER(3, true),
    DEFILER(3, true),
    BRAWLER(1, false),
}

open class Pest(id: Int, tile: Tile, val game: PestControlGame, val portal: PestPortal, val family: PestFamily) : NPC(id, tile, true) {

    init {
        setForceMultiArea(true)
        setForceAgressive(true)
        setIgnoreDocile(true)
        setNoDistanceCheck(true)
        setRandomWalk(false)
        setForceAggroDistance(AGGRO_DISTANCE)
    }

    protected open val reachesTowers = false

    override fun processNPC() {
        super.processNPC()
        if (isDead || hasFinished() || combat.hasTarget() || hasWalkSteps() || tickCounter % ADVANCE_INTERVAL != 0L) return
        advance()
    }

    protected open fun advance() {
        val knight = game.knight
        if (WorldUtil.isInRange(this, knight, 2)) return
        walkTowards(knight.middleTile)
    }

    protected fun walkTowards(tile: Tile) {
        walkRoute(this, routeEntityToTile(this, tile, ADVANCE_TURNS), true)
    }

    override fun getPossibleTargets(): List<Entity> {
        val players = game.players.filter {
            !it.isDead && (reachesTowers || it.plane == plane) && WorldUtil.isInRange(this, it, AGGRO_DISTANCE)
        }
        val knight = game.knight
        if (family.attacksKnight && !knight.isDead && WorldUtil.isInRange(this, knight, KNIGHT_SIGHT) && (players.isEmpty() || Utils.random(2) == 0))
            return listOf(knight)
        return players
    }

    override fun removeHitpoints(hit: Hit) {
        super.removeHitpoints(hit)
        game.credit(hit)
    }

    override fun finish() {
        super.finish()
        game.removePest(this)
    }
}
