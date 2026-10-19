package com.rs.game.content.minigames.pest.npcs

import com.rs.game.content.minigames.pest.PestControlGame
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import world.gregs.voidps.type.Tile

class PestPortal(
    val game: PestControlGame,
    val position: PortalPosition,
    tile: Tile,
    private val activeId: Int,
    shieldedId: Int,
    private val lifepoints: Int,
) : NPC(shieldedId, tile, true) {
    var shielded = true
        private set
    var destroyed = false
        private set

    init {
        setForceMultiArea(true)
        setCantFollowUnderCombat(true)
        setNoDistanceCheck(true)
        setRandomWalk(false)
        hitpoints = lifepoints
    }

    override fun getMaxHitpoints() = if (lifepoints > 0) lifepoints else super.getMaxHitpoints()

    override fun processNPC() {}

    override fun setCombatTarget(entity: Entity?) {}

    fun dropShield() {
        if (!shielded || destroyed) return
        shielded = false
        transformIntoNPC(activeId)
        game.sendTeamMessage("The ${position.description} portal shield has been dropped!")
    }

    override fun canBeAttackedBy(player: Player) = !shielded && !destroyed

    override fun handlePreHit(hit: Hit) {
        if (shielded || destroyed || hit.look == HitLook.POISON_DAMAGE) hit.damage = 0
        super.handlePreHit(hit)
    }

    override fun removeHitpoints(hit: Hit) {
        super.removeHitpoints(hit)
        game.credit(hit)
    }

    override fun sendDeath(source: Entity) {
        if (destroyed) return
        destroyed = true
        resetWalkSteps()
        combat.removeTarget()
        anim(combatDefinitions.deathEmote)
        game.portalDestroyed(this)
        WorldTasks.schedule(combatDefinitions.deathDelay.coerceAtLeast(1)) { finish() }
    }
}

enum class PortalPosition(val description: String, val localX: Int, val localY: Int) {
    WEST("purple, western", 4, 31),
    EAST("blue, eastern", 56, 28),
    SOUTH_EAST("yellow, south-eastern", 45, 10),
    SOUTH_WEST("red, south-western", 21, 9),
}
