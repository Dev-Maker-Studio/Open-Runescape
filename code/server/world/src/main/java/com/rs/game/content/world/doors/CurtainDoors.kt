package com.rs.game.content.world.doors

import com.rs.engine.pathfinder.RouteEvent
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc

@ServerStartupEvent
fun mapDesertCurtainDoors() {
    onObjectClick(Loc.desertdoorclosed, checkDistance = false) { (player, curtain, option) ->
        if (option != "Open" || player.isLocked) return@onObjectClick
        val inside = curtain.tile
        val outside = when (curtain.rotation) {
            0 -> inside.transform(-1, 0, 0)
            1 -> inside.transform(0, 1, 0)
            2 -> inside.transform(1, 0, 0)
            else -> inside.transform(0, -1, 0)
        }
        val fromOutside = when (curtain.rotation) {
            0 -> player.x < curtain.x
            1 -> player.y > curtain.y
            2 -> player.x > curtain.x
            else -> player.y < curtain.y
        }
        val approach = if (fromOutside) outside else inside
        val destination = if (fromOutside) inside else outside
        val cross = Runnable {
            if (player.isLocked || !player.tile.matches(approach) || !curtain.exists()) return@Runnable
            player.faceObject(curtain)
            player.lock(2)
            player.addWalkSteps(destination, 1, false)
        }
        player.stopAll()
        if (player.tile.matches(approach)) cross.run()
        else player.setRouteEvent(RouteEvent(approach, cross))
    }
}
