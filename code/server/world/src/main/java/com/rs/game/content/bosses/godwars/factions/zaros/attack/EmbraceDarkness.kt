package com.rs.game.content.bosses.godwars.factions.zaros.attack

import com.rs.game.content.bosses.godwars.factions.zaros.Nex
import com.rs.game.content.bosses.godwars.factions.zaros.NexPhase
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.gameval.gen.Varc

private const val FULL_LIGHT = 255
private const val DARKNESS_RANGE = 30

object EmbraceDarkness : NexAttack {
    override fun attack(nex: Nex, target: Entity): Int {
        nex.forceTalk("Embrace darkness!")
        nex.voiceEffect(target, Sound.sound_3322, true)
        nex.anim(Seq.godwars2_zaros_blast_away)
        nex.spotAnim(Spotanim.zaros_blast_away_dust_spotanim)
        WorldTasks.scheduleTimer { _ ->
            val players = nex.possibleTargets.filterIsInstance<Player>()
            if (nex.phase != NexPhase.SHADOW || nex.hasFinished()) {
                players.forEach { it.packets.sendVarc(Varc.varc_1435, FULL_LIGHT) }
                return@scheduleTimer false
            }
            if (Utils.getRandomInclusive(2) == 0)
                players.forEach {
                    val distance = Utils.getDistance(it.x, it.y, nex.x, nex.y).toInt().coerceAtMost(DARKNESS_RANGE)
                    it.packets.sendVarc(Varc.varc_1435, distance * FULL_LIGHT / DARKNESS_RANGE)
                }
            true
        }
        return nex.attackSpeed
    }
}
