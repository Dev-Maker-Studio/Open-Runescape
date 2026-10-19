package com.rs.game.content.world.areas.lighthouse

import com.rs.engine.pathfinder.RouteEvent
import com.rs.game.content.skills.agility.Agility
import com.rs.game.content.world.doors.Doors
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import world.gregs.voidps.type.Tile

private class BasaltGap(val beachSide: Int, val beachSideTile: Tile, val lighthouseSide: Int, val lighthouseSideTile: Tile, val xp: Double = 0.0, val failShore: Tile? = null)

private val SOUTH_BEACH = Tile.of(2522, 3594, 0)
private val NORTH_SHORE = Tile.of(2514, 3620, 0)

private val BASALT_GAPS = listOf(
    BasaltGap(Loc.horror_jumping_spot1, Tile.of(2522, 3595, 0), Loc.horror_jumping_spot2, Tile.of(2522, 3597, 0)),
    BasaltGap(Loc.horror_jumping_spot3, Tile.of(2522, 3600, 0), Loc.horror_jumping_spot4, Tile.of(2522, 3602, 0), 2.0, SOUTH_BEACH),
    BasaltGap(Loc.horror_jumping_spot5, Tile.of(2518, 3611, 0), Loc.horror_jumping_spot6, Tile.of(2516, 3611, 0), 2.0),
    BasaltGap(Loc.horror_jumping_spot7, Tile.of(2514, 3613, 0), Loc.horror_jumping_spot8, Tile.of(2514, 3615, 0), 2.0, NORTH_SHORE),
    BasaltGap(Loc.horror_jumping_spot9, Tile.of(2514, 3617, 0), Loc.horror_jumping_spot10, Tile.of(2514, 3619, 0)),
)

@ServerStartupEvent
fun mapLighthouse() {
    onObjectClick(Loc.horror_ladder_top, Loc.horror_ladder_base) { (player, obj) ->
        player.useLadder(if (obj.id == Loc.horror_ladder_base) Tile.of(2510, 3644, 0) else Tile.of(2519, 9995, 1))
    }
    onObjectClick(Loc.horror_far_right_door, Loc.horror_far_left_door) { (player, obj) -> Doors.handleDoor(player, obj) }
    onObjectClick(Loc.horror_ladder_base2, Loc.horror_ladder_top2) { (player, obj) ->
        val up = obj.id == Loc.horror_ladder_top2
        player.useLadder(player.transform(0, if (up) 3 else -3, if (up) -1 else 1))
    }
    onObjectClick(Loc.horror_broken_bridge_cross) { (player, obj) ->
        player.useStairs(-1, player.transform(if (player.x < obj.x) 3 else -3, 0, 0), 1, 1)
    }
    onObjectClick(*BASALT_GAPS.flatMap { listOf(it.beachSide, it.lighthouseSide) }.toTypedArray<Any>(), checkDistance = false) { (player, obj) ->
        val gap = BASALT_GAPS.first { obj.id == it.beachSide || obj.id == it.lighthouseSide }
        val (from, to) = if (obj.id == gap.lighthouseSide) gap.beachSideTile to gap.lighthouseSideTile else gap.lighthouseSideTile to gap.beachSideTile
        if (player.tile == to) jumpBasalt(player, gap, from)
        else player.setRouteEvent(RouteEvent(from) { if (player.tile == from) jumpBasalt(player, gap, to) })
    }
}

private fun jumpBasalt(player: Player, gap: BasaltGap, to: Tile) {
    val shore = gap.failShore
    if (shore == null || Agility.rollSuccess(player, 5, 255)) {
        player.forceMove(to, Seq.human_spot_jump, 5, 30) { if (gap.xp > 0) player.skills.addXp(Constants.AGILITY, gap.xp) }
        return
    }
    player.lock()
    player.faceTile(to)
    player.schedule {
        player.anim(Seq.human_wobbleandfall_r)
        wait(2)
        player.spotAnim(Spotanim.watersplash)
        wait(1)
        player.anim(-1)
        player.tele(shore)
        player.applyHit(Hit.flat(player, Utils.randomInclusive(1, 100)))
        player.skills.addXp(Constants.AGILITY, 0.5)
        player.sendMessage("You slip off the rock and fall into the water, washing up on the shore.")
        player.unlock()
    }
}
