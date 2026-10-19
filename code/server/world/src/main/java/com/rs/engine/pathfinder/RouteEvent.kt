package com.rs.engine.pathfinder

import com.rs.game.World
import com.rs.game.content.Effect
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.map.pathfinder.Route
import org.darkan.core.model.GroundItem
import world.gregs.voidps.type.Tile
import org.darkan.core.net.prot.packet.MinimapFlag
import com.rs.utils.WorldUtil

class RouteEvent(private val target: Any, private val onReachedEvent: Runnable, private val onNearestEvent: (() -> Boolean)?) {
    constructor(target: Any, event: Runnable): this(target, event, null)

    //TODO add optimized boolean for stationary targets that doesn't recalculate the path to check status each time
    fun processEvent(entity: Entity): Boolean {
        if (!simpleCheck(entity)) {
            cantReachThat(entity as? Player)
            return true
        }

        val route = routeEntityTo(entity, target)
        if (route.failed || route.size <= 0) {
            if (route.failed) {
                cantReachThat(entity as? Player)
                return true
            }
            if (route.size <= 0) {
                (entity as? Player)?.session?.queuePacket(MinimapFlag(255, 255))
                if (route.alternative) {
                    return onNearestEvent?.invoke() ?: run {
                        cantReachThat(entity as? Player)
                        return true
                    }
                }
                onReachedEvent.run()
            }
            return true
        }

        if (entity.hasEffect(Effect.FREEZE) || (target is Entity && target.hasWalkSteps() && WorldUtil.collides(entity, target)))
            return false

        entity.resetWalkSteps()
        addSteps(entity, route, true)
        return false
    }

    private fun simpleCheck(entity: Entity): Boolean {
        return when(target) {
            is Entity -> entity.plane == target.plane && !target.hasFinished()
            is GameObject -> entity.plane == target.level && World.getObject(target.tile, target.type) != null
            is GroundItem -> entity.plane == target.tile.plane && ChunkManager.getChunk(target.tile.getChunkId()).itemExists(target)
            is Tile -> entity.plane == target.plane
            else -> throw RuntimeException("$target is not instanceof any reachable entity.")
        }
    }

    val cantReachThat = { player: Player? ->
        player?.let {
            it.sendMessage("You can't reach that.")
            it.session.queuePacket(MinimapFlag(255, 255))
        }
    }
}
