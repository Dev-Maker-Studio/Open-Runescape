package com.rs.game.content.bosses.godwars.factions.zaros.attack

import com.rs.game.content.bosses.godwars.factions.zaros.Nex
import com.rs.game.content.bosses.godwars.factions.zaros.NexCutScene
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.gameval.gen.Spotanim
import world.gregs.voidps.type.Tile

private const val MAX_DAMAGE = 650
private const val CAMERA_PULLBACK = 7

/** Where Nex lines up before the charge, and where the camera sits to watch it, relative to the arena centre. */
private class Charge(val offsetX: Int, val offsetY: Int, val camOffsetX: Int, val camOffsetY: Int) {
    val northSouth: Boolean get() = offsetX == 0
}

private val CHARGES = arrayOf(
    Charge(0, 11, 0, -CAMERA_PULLBACK),
    Charge(10, 0, -CAMERA_PULLBACK, 0),
    Charge(0, -10, 0, CAMERA_PULLBACK),
    Charge(-11, 0, CAMERA_PULLBACK, 0),
)

object NoEscape : NexAttack {
    override fun attack(nex: Nex, target: Entity): Int {
        nex.forceTalk("There is...")
        nex.voiceEffect(target, Sound.sound_3294, true)
        nex.setCantInteract(true)
        nex.combat.removeTarget()
        val center = nex.respawnTile
        val charge = CHARGES[Utils.random(CHARGES.size)]
        val from = center.transform(charge.offsetX, charge.offsetY, 0)
        WorldTasks.scheduleTimer { tick ->
            when (tick) {
                0 -> {
                    nex.anim(Seq.godwars2_zaros_dash_attack)
                    nex.spotAnim(Spotanim.zaros_dust_trail_spotanim)
                }
                1 -> {
                    nex.tele(from)
                    nex.forceTalk("NO ESCAPE!")
                    nex.voiceEffect(target, Sound.sound_3292, true)
                    nex.forceMove(center, 25, 90)
                    nex.calculatePossibleTargets(center, from, charge.northSouth)
                        .filterIsInstance<Player>()
                        .forEach { trample(nex, it, from, center.transform(1 + charge.camOffsetX, 1 + charge.camOffsetY, 0)) }
                }
                3 -> nex.tele(center)
                4 -> {
                    nex.combatTarget = target
                    nex.setCantInteract(false)
                    return@scheduleTimer false
                }
            }
            true
        }
        return nex.attackSpeed
    }

    private fun trample(nex: Nex, player: Player, from: Tile, camera: Tile) {
        player.cutsceneManager.play(NexCutScene(camera, from))
        player.applyHit(Hit(nex, Utils.getRandomInclusive(MAX_DAMAGE), HitLook.TRUE_DAMAGE))
        player.forceMove(player.tile, Seq.seq_10070, 0, 30)
    }
}
