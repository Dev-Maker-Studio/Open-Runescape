package com.rs.game.content.bosses.godwars.factions.zaros.attack

import com.rs.game.content.bosses.godwars.factions.zaros.Nex
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim

private const val LOCK_TICKS = 3
private const val PRAYER_BLOCK_TICKS = 12

object Drag : NexAttack {
    override fun attack(nex: Nex, target: Entity): Int {
        val dragged = nex.possibleTargets
            .filterIsInstance<Player>()
            .maxByOrNull { Utils.getDistance(it.x, it.y, nex.x, nex.y) } ?: return nex.attackSpeed
        dragged.lock(LOCK_TICKS)
        dragged.spotAnim(Spotanim.rand_player_yklagor_impact_death_grip)
        dragged.forceMove(nex.getNearestTeleTile(1), Seq.rand_boss_demon_ibanblast_spotanim, 0, 60)
        dragged.stopAnim()
        nex.anim(Seq.godwars2_zaros_alternate_cast_attack)
        nex.combatTarget = dragged
        WorldTasks.schedule {
            dragged.sendMessage("You've been injured and you can't use protective prayers!")
            dragged.setProtectionPrayBlock(PRAYER_BLOCK_TICKS)
            dragged.sendMessage("You're stunned.")
        }
        return nex.attackSpeed
    }
}
