package com.rs.game.content.bosses.godwars.factions.zaros.attack

import com.rs.game.World
import com.rs.game.content.bosses.godwars.factions.zaros.Nex
import com.rs.game.model.entity.BodyGlow
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.gameval.gen.Spotanim

private const val SAFE_DISTANCE = 5
private const val PUNISH_FRACTION = 0.1

object BloodSacrifice : NexAttack {
    override fun attack(nex: Nex, target: Entity): Int {
        if (target !is Player) return 0
        nex.forceTalk("I demand a blood sacrifice!")
        nex.voiceEffect(Sound.sound_3293, true)
        target.setNextBodyGlow(BodyGlow(nex.attackSpeed * 30, 0, 0, 0, 255))
        target.sendMessage("Nex has marked you as a sacrifice, RUN!")
        WorldTasks.schedule(nex.attackSpeed) {
            if (Utils.getDistance(nex.tile, target.tile) >= SAFE_DISTANCE) return@schedule
            target.sendMessage("You didn't make it far enough in time - Nex fires a punishing attack!")
            nex.anim(Seq.godwars2_zaros_cast_attack)
            for (t in nex.possibleTargets)
                World.sendProjectile(nex, t, Spotanim.blood_blitz_travel, 41 to 16, 41, 5, 16, 0) {
                    nex.healHit(t.hitpoints)
                    t.applyHit(Hit(nex, (t.hitpoints * PUNISH_FRACTION).toInt(), HitLook.TRUE_DAMAGE))
                }
        }
        return nex.attackSpeed * 2
    }
}
