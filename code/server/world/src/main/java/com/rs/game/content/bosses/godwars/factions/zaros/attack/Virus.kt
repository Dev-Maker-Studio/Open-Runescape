package com.rs.game.content.bosses.godwars.factions.zaros.attack

import com.rs.game.content.bosses.godwars.factions.zaros.Nex
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound

private const val MAX_DAMAGE = 100
private const val DRAIN = 0.05
private val DRAINED_STATS = intArrayOf(Constants.MAGIC, Constants.RANGE, Constants.ATTACK, Constants.STRENGTH, Constants.DEFENSE)

object Virus : NexAttack {
    override fun attack(nex: Nex, target: Entity): Int {
        nex.forceTalk("Let the virus flow through you.")
        nex.voiceEffect(target, Sound.sound_3296, true)
        nex.anim(Seq.godwars2_zaros_cast_attack)
        spread(nex, target)
        return nex.attackSpeed
    }

    /** The infection jumps from carrier to carrier, so anyone in a chain of adjacent players catches it. */
    private fun spread(nex: Nex, patientZero: Entity) {
        val healthy = nex.possibleTargets.toMutableList()
        val carriers = ArrayDeque(listOf(patientZero))
        while (carriers.isNotEmpty()) {
            val carrier = carriers.removeFirst()
            val caught = healthy.filter { Utils.getDistance(it.x, it.y, carrier.x, carrier.y) <= 1 }
            healthy.removeAll(caught)
            caught.forEach { infect(nex, it) }
            carriers.addAll(caught)
        }
    }

    private fun infect(nex: Nex, victim: Entity) {
        victim.forceTalk("*Cough*")
        victim.applyHit(Hit(nex, Utils.getRandomInclusive(MAX_DAMAGE), HitLook.TRUE_DAMAGE))
        DRAINED_STATS.forEach { victim.lowerStat(it, DRAIN, 0.0) }
    }
}
