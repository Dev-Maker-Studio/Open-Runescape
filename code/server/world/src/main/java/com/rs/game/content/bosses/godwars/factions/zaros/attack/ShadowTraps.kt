package com.rs.game.content.bosses.godwars.factions.zaros.attack

import com.rs.game.World
import com.rs.game.content.bosses.godwars.factions.zaros.Nex
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile

private const val TRAP_LIFETIME = 4
private const val DETONATE_DELAY = 3
private const val MIN_DAMAGE = 400

object ShadowTraps : NexAttack {
    override fun attack(nex: Nex, target: Entity): Int {
        nex.forceTalk("Fear the Shadow!")
        nex.voiceEffect(target, Sound.sound_3314, true)
        nex.anim(Seq.godwars2_zaros_smash_attack)
        nex.spotAnim(Spotanim.zaros_smash_attack_spotanim)
        val traps = nex.possibleTargets.mapTo(LinkedHashSet()) { Tile.of(it.x, it.y, it.plane) }
        traps.forEach { World.spawnObjectTemporary(GameObject(Loc.godwars2_shadow_smash, ObjectShape.SCENERY_INTERACT, 0, it), TRAP_LIFETIME) }
        WorldTasks.schedule(DETONATE_DELAY) {
            val caught = nex.possibleTargets
            for (trap in traps) {
                World.sendSpotAnim(trap, Spotanim.spell_shadow_barrage_impact)
                caught.filter { it.x == trap.x && it.y == trap.y }
                    .forEach { it.applyHit(Hit(nex, Utils.getRandomInclusive(MIN_DAMAGE) + MIN_DAMAGE, HitLook.TRUE_DAMAGE)) }
            }
        }
        return nex.attackSpeed
    }
}
