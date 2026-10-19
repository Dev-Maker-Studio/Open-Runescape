package com.rs.game.content.commands.trent

import com.rs.engine.command.Commands
import com.rs.game.World
import com.rs.game.content.bosses.qbd.rollQbdKillAndAddToChest
import com.rs.game.model.entity.Hit
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.Rights
import org.darkan.core.model.SpotAnim
import world.gregs.voidps.type.Tile
import org.darkan.core.Utils
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.utils.WorldUtil

@ServerStartupEvent
fun addTrentCommands() {
    Commands.add(Rights.OWNER, "qbdloot", "add qbd kill loot to chest") { p, _ ->
        rollQbdKillAndAddToChest(p)
    }

    Commands.add(Rights.OWNER, "hittest [id]", "test hit types") { p, args ->
        val type = Hit.HitLook.entries.getOrNull(args[0].toInt()) ?: return@add
        p.applyHit(Hit(p, args[1].toInt(), type))
    }

    Commands.add(Rights.OWNER, "jessehittest [id]", "test hit types") { p, _ ->
        p.fakeHit(Hit(p, 5928388, Hit.HitLook.MELEE_DAMAGE))
        p.fakeHit(Hit(p, 10, Hit.HitLook.RANGE_CRIT).setText("<col=FF0000><shad=000000>INSTANT KILL"))
        p.fakeHit(Hit(p, 100, Hit.HitLook.MAGIC_DAMAGE).addSoaking(50000002))
        p.fakeHit(Hit(p, 5552, Hit.HitLook.MELEE_DAMAGE))
        p.fakeHit(Hit(p, 5928388, Hit.HitLook.MELEE_DAMAGE).addSoaking(50000002))
    }

    Commands.add(Rights.OWNER, "delayprojtest", "Precise timing projectile/spotanim") { p, _ ->
        val startTile = p.tile
        var delay = 0

        val tiles = (-5..5).flatMap { x -> (-5..5)
            .map { y -> x to y } }
            .filterNot { (x, y) -> x == 0 && y == 0 }
            .sortedBy { (x, y) -> Utils.getDistance(startTile, startTile.transform(x, y)) }

        tiles.forEach { (x, y) ->
            val endTile = startTile.transform(x, y)
            val distanceDelay = (Utils.getDistance(startTile, endTile) * 22).toInt()

            World.sendProjectile(startTile, endTile, Spotanim.mah4_light_projectile_a, heights = 20 to 5, delay, speed = 22, angle = 30)
            World.sendSpotAnim(endTile, SpotAnim(Spotanim.mah4_reward_spot_leech_impact_7, delay + distanceDelay, 0, WorldUtil.getFaceDirection(endTile, p).id))

            delay += 5
        }
    }
}