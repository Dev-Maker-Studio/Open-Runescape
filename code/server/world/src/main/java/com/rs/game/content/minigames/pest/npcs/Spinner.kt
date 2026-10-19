package com.rs.game.content.minigames.pest.npcs

import com.rs.game.World
import com.rs.game.content.minigames.pest.PestControlGame
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.tasks.WorldTasks
import com.rs.utils.WorldUtil
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import world.gregs.voidps.type.Tile

private const val HEAL_RANGE = 2
private const val HEAL_INTERVAL = 5
private const val HEAL_FRACTION = 50
private const val DETONATION_DELAY = 3
private const val DETONATION_RADIUS = 1
private const val DETONATION_DAMAGE = 50
private const val DETONATION_POISON = 18

class Spinner(id: Int, tile: Tile, game: PestControlGame, portal: PestPortal) : Pest(id, tile, game, portal, PestFamily.SPINNER) {
    private var nextHeal = 0L
    private var detonating = false

    override fun processNPC() {
        if (isDead || hasFinished() || detonating || isLocked) return
        if (combat.hasTarget() || portal.destroyed) {
            super.processNPC()
            return
        }
        if (!WorldUtil.isInRange(this, portal, HEAL_RANGE)) {
            if (!hasWalkSteps()) walkTowards(portal.middleTile)
            return
        }
        if (World.getServerTicks() < nextHeal || portal.hitpoints >= portal.maxHitpoints) return
        nextHeal = World.getServerTicks() + HEAL_INTERVAL
        resetWalkSteps()
        faceEntity(portal)
        anim(Seq.pest_spinner_repairing)
        spotAnim(Spotanim.pest_spinner_sparks)
        portal.heal(portal.maxHitpoints / HEAL_FRACTION)
    }

    override fun advance() {}

    override fun getPossibleTargets(): List<Entity> = emptyList()

    fun detonate() {
        if (detonating || isDead || hasFinished()) return
        detonating = true
        resetWalkSteps()
        combat.removeTarget()
        anim(Seq.pest_spinner_explode)
        WorldTasks.schedule(DETONATION_DELAY) {
            if (hasFinished()) return@schedule
            World.sendSpotAnim(tile, Spotanim.pest_spinner_explode_spot)
            for (player in game.players) {
                if (player.isDead || !WorldUtil.isInRange(this, player, DETONATION_RADIUS)) continue
                player.applyHit(Hit(this, DETONATION_DAMAGE, HitLook.TRUE_DAMAGE))
                player.poison.makePoisoned(DETONATION_POISON)
            }
            finish()
        }
    }
}
