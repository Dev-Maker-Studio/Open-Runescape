package com.rs.game.content.bosses.corp

import com.rs.game.World
import com.rs.game.model.WorldProjectile
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import org.darkan.core.Utils
import com.rs.utils.WorldUtil
import java.util.function.Consumer

class DarkEnergyCore(private val beast: CorporealBeast) : NPC(8127, beast.tile, true) {

    init {
        setForceMultiArea(true)
        isIgnoreDocile = true
    }

    override fun processNPC() {
        if (isDead || hasFinished()) return

        if (!isLocked && !isHidden && !poison.isPoisoned && tickCounter % 2L == 0L) {
            val target = beast.possibleTargets.filter { it is Player && it.withinDistance(this.tile, 1) }.firstOrNull()
            if (target == null)
                jump()
            else
                sapLife()
        }
    }

    private fun jump() {
        val target = beast.possibleTargets.takeIf { it.isNotEmpty() }?.let { it[Utils.getRandomInclusive(it.size - 1)] }
        if (target == null) {
            finish()
            beast.removeDarkEnergyCore()
            return
        }
        lock()
        schedule {
            val start = this@DarkEnergyCore.tile
            val destination = target.tile
            val distance = Utils.getDistance(start, destination)
            isHidden = true
            val speed = when {
                distance <= 2 -> 30
                distance <= 5 -> 60
                else -> 90
            }

            val travelTime = World.sendProjectileAbsoluteSpeed(
                start, destination,
                1828,
                heights = 15 to 15,
                delay = 0,
                speed,
                offset = 20,
                angle = 30
            ).taskDelay

            wait(travelTime+1)
            tele(destination)
            isHidden = false
            wait(2)
            unlock()
        }
    }

    private fun sapLife() {
        beast.possibleTargets.filter { it is Player && it.withinDistance(this.tile, 1) }.forEach { target ->
            val damage = Utils.random(50, 100)
            target.applyHit(Hit(this, damage, HitLook.TRUE_DAMAGE))
            beast.heal(damage)
            (target as? Player)?.sendMessage("The dark core creature steals some life from you for its master.", true)
        }
    }

    override fun getMagePrayerMultiplier() = 0.6

    override fun sendDeath(source: Entity?) {
        super.sendDeath(source)
        beast.removeDarkEnergyCore()
    }
}