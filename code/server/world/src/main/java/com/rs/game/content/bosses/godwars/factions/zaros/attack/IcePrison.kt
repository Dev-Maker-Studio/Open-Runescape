package com.rs.game.content.bosses.godwars.factions.zaros.attack

import com.rs.game.World
import com.rs.game.content.bosses.godwars.factions.zaros.Nex
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.tasks.WorldTasks
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Sound
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile
import kotlin.math.abs

private const val SHATTER_DELAY = 8

object IcePrison : NexAttack {
    override fun attack(nex: Nex, target: Entity): Int {
        nex.forceTalk("Die now, in a prison of ice!")
        nex.voiceEffect(target, Sound.sound_3308, true)
        nex.anim(Seq.godwars2_zaros_cast_attack)
        World.sendProjectile(nex, target, Spotanim.ice_blitz_travel, 20 to 20, 20, 15, 10)
        val base = Tile.of(target.x, target.y, target.plane)
        target.tempAttribs.setB("inIcePrison", true)
        val walls = (-1..1).flatMap { x -> (-1..1).map { y -> base.transform(x, y, 0) } }
            .filter { !it.matches(base) }
            .map { GameObject(Loc.godwars2_avatar_icicle2, ObjectShape.SCENERY_INTERACT, 0, it) }
            .filter { World.floorAndWallsFree(it.tile, (it.definitions.sizeX + it.definitions.sizeY) / 2) }
            .onEach { World.spawnObject(it) }
        WorldTasks.schedule(SHATTER_DELAY) {
            target.tempAttribs.setB("inIcePrison", false)
            if (abs(target.x - base.x) <= 1 && abs(target.y - base.y) <= 1) {
                (target as? Player)?.sendMessage("The centre of the ice prison freezes you to the bone!")
                target.resetWalkSteps()
                target.applyHit(Hit(nex, Utils.random(600, 800), HitLook.TRUE_DAMAGE))
            }
            WorldTasks.schedule { walls.forEach { World.removeObject(it) } }
        }
        return nex.attackSpeed * 2
    }
}
