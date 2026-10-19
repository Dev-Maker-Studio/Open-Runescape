package com.rs.game.content.bosses.godwars.factions.zaros.attack

import com.rs.game.World
import com.rs.game.content.bosses.godwars.factions.zaros.Nex
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import com.rs.utils.Ticks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.model.ObjectShape

private const val ICICLE_DELAY = 6
private const val ICICLE_LIFETIME_SECONDS = 7
private const val PRAYER_BLOCK_TICKS = 12
private val ICICLE_OFFSETS = (2 downTo -2 step 2).flatMap { y -> (2 downTo -2 step 2).map { x -> x to y } }.filter { (x, y) -> x != y }

object ContainThis : NexAttack {
    override fun attack(nex: Nex, target: Entity): Int {
        nex.forceTalk("Contain this!")
        nex.voiceEffect(target, Sound.sound_3316, true)
        nex.anim(Seq.godwars2_zaros_smash_attack)
        val base = nex.transform(1, 1, 0)
        nex.resetWalkSteps()
        WorldTasks.schedule(ICICLE_DELAY) {
            for ((x, y) in ICICLE_OFFSETS) {
                val tile = base.transform(x, y, 0)
                val icicle = GameObject(Loc.godwars2_avatar_icicle, ObjectShape.SCENERY_INTERACT, 0, tile)
                val size = (icicle.definitions.sizeX + icicle.definitions.sizeY) / 2
                if (tile == base || !World.floorAndWallsFree(tile, size)) continue
                nex.possibleTargets
                    .filterIsInstance<Player>()
                    .filter { it.x == tile.x && it.y == tile.y }
                    .forEach { spike(nex, it) }
                World.spawnObjectTemporary(icicle, Ticks.fromSeconds(ICICLE_LIFETIME_SECONDS))
            }
        }
        return nex.attackSpeed * 2
    }

    private fun spike(nex: Nex, player: Player) {
        player.anim(Seq.agilityarena_player_spiked)
        player.applyHit(Hit(nex, Utils.random(200, 350), HitLook.TRUE_DAMAGE))
        player.sendMessage("The icicle spikes you to the spot!")
        player.sendMessage("You've been injured and can't use " + (if (player.prayer.isCurses) "deflect curses" else "protection prayers ") + "!")
        player.resetWalkSteps()
        player.setProtectionPrayBlock(PRAYER_BLOCK_TICKS)
    }
}
