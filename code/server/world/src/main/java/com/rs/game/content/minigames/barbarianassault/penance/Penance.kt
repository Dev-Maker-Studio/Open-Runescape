package com.rs.game.content.minigames.barbarianassault.penance

import com.rs.game.content.minigames.barbarianassault.BarbarianAssaultGame
import com.rs.game.content.minigames.barbarianassault.PenanceFamily
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.npc.NPC
import org.darkan.core.map.pathfinder.Direction
import world.gregs.voidps.type.Tile
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

open class Penance(id: Int, tile: Tile, val game: BarbarianAssaultGame, val family: PenanceFamily, val ordinal: Int) : NPC(id, tile, true) {
    var stunTicks = 0
    var dying = false
        private set

    init {
        setRandomWalk(false)
        setForceMultiArea(true)
        setIgnoreDocile(true)
        setNoDistanceCheck(true)
    }

    val local: Tile get() = game.toLocal(tile)

    override fun processNPC() {
        if (dying || isDead) return
        if (stunTicks > 0) {
            stunTicks--
            resetWalkSteps()
            return
        }
        tickPenance()
    }

    protected open fun tickPenance() = super.processNPC()

    fun eggStun(ticks: Int) {
        stunTicks = max(stunTicks, ticks)
        resetWalkSteps()
        combat.removeTarget()
    }

    override fun getPossibleTargets(): List<Entity> = game.targetsFor(this)

    override fun sendDeath(source: Entity) {
        if (dying) return
        dying = true
        game.penanceDied(this)
        super.sendDeath(source)
    }

    override fun drop() {
        mostDamageReceivedSourcePlayer?.sendNPCKill(name)
        game.dropEggs(this)
    }

    override fun finish() {
        super.finish()
        game.penanceRemoved(this)
    }

    protected fun stepTowards(destination: Tile): Boolean {
        val dx = (destination.x - x).sign
        val dy = (destination.y - y).sign
        if (dx == 0 && dy == 0) return false
        if (dx != 0 && dy != 0 && canStep(dx, dy)) return walkOneStep(dx, dy, true)
        if (dx != 0 && canStep(dx, 0)) return walkOneStep(dx, 0, true)
        if (dy != 0 && canStep(0, dy)) return walkOneStep(0, dy, true)
        return false
    }

    private fun canStep(dx: Int, dy: Int): Boolean {
        val direction = Direction.forDelta(dx, dy) ?: return false
        return game.canPenanceStep(this, direction)
    }

    protected fun chebyshev(a: Tile, b: Tile) = max(abs(a.x - b.x), abs(a.y - b.y))
}
