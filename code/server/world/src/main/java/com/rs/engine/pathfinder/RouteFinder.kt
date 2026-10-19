@file:Suppress("DuplicatedCode")

package com.rs.engine.pathfinder

import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.game.model.gameobject.GameObject.RouteType
import org.darkan.core.map.WorldCollision
import org.darkan.core.map.pathfinder.PathFinder
import org.darkan.core.map.pathfinder.Route
import org.darkan.core.map.pathfinder.RouteCoordinates
import org.darkan.core.map.pathfinder.DEFAULT_SEARCH_MAP_SIZE
import org.darkan.core.model.GroundItem
import world.gregs.voidps.type.Tile
import org.darkan.core.net.prot.packet.MinimapFlag
import org.darkan.core.net.prot.packet.Walk

private const val DEFAULT_MAX_TURNS = 25

@JvmOverloads
fun routeEntityToObject(entity: Entity, obj: GameObject, maxTurns: Int = DEFAULT_MAX_TURNS): Route {
    return PathFinder(flags = WorldCollision.allFlags, useRouteBlockerFlags = true)
        .findPath(
            entity.x, entity.y,
            obj.x, obj.y,
            entity.plane,
            collision = entity.collisionStrategy,
            srcSize = entity.size,
            destWidth = if (obj.rotation == 0 || obj.rotation == 2) obj.definitions.sizeX else obj.definitions.sizeY,
            destHeight = if (obj.rotation == 0 || obj.rotation == 2) obj.definitions.sizeY else obj.definitions.sizeX,
            objRot = obj.rotation,
            objShape = if (obj.routeType == RouteType.WALK_ONTO) -1 else obj.type.id,
            accessBitMask = if (obj.rotation != 0) ((obj.definitions.blockFlag shl obj.rotation) and 0xF) + (obj.definitions.blockFlag shr (4 - obj.rotation)) else obj.definitions.blockFlag,
            maxTurns = maxTurns
        )
}

@JvmOverloads
fun routeEntityToEntity(entity: Entity, target: Entity, maxTurns: Int = DEFAULT_MAX_TURNS): Route {
    return PathFinder(flags = WorldCollision.allFlags, useRouteBlockerFlags = true)
        .findPath(
            entity.x, entity.y,
            target.x, target.y,
            entity.plane,
            collision = entity.collisionStrategy,
            objShape = -2,
            srcSize = entity.size,
            destWidth = target.size,
            destHeight = target.size,
            maxTurns = maxTurns
        )
}

@JvmOverloads
fun routeEntityToTile(entity: Entity, tile: Tile, maxTurns: Int = DEFAULT_MAX_TURNS): Route {
    return PathFinder(flags = WorldCollision.allFlags, useRouteBlockerFlags = true)
        .findPath(entity.x, entity.y, tile.x, tile.y, entity.plane, collision = entity.collisionStrategy, srcSize = entity.size, maxTurns = maxTurns)
}

@JvmOverloads
fun routeEntityWalkRequest(entity: Entity, request: Walk, maxTurns: Int = DEFAULT_MAX_TURNS): Route {
    return PathFinder(flags = WorldCollision.allFlags, useRouteBlockerFlags = true)
        .findPath(entity.x, entity.y, request.x, request.y, entity.plane, collision = entity.collisionStrategy, srcSize = entity.size, maxTurns = maxTurns)
}

@JvmOverloads
fun routeEntityTo(entity: Entity, target: Any, maxTurns: Int = DEFAULT_MAX_TURNS): Route {
    return when(target) {
        is Entity -> routeEntityToEntity(entity, target, maxTurns)
        is GameObject -> routeEntityToObject(entity, target, maxTurns)
        is GroundItem -> routeEntityToTile(entity, target.tile, maxTurns)
        is Tile -> routeEntityToTile(entity, target, maxTurns)
        else -> throw IllegalStateException("Unexpected value: $target")
    }
}

fun walkRoute(entity: Entity, route: Route, forceSteps: Boolean): Boolean {
    if (!route.success) return false
    if (entity is Player) entity.stopAll()
    entity.resetWalkSteps()
    addSteps(entity, route, forceSteps)
    return true
}

@JvmOverloads
fun addSteps(entity: Entity, route: Route, forceSteps: Boolean, maxSteps: Int = -1) {
    var lastStep: RouteCoordinates? = null
    for (coord in route.coords) {
        if (!entity.addWalkSteps(coord.x, coord.y, maxSteps, true, forceSteps)) break
        lastStep = coord
    }
    if (lastStep != null && entity is Player) {
        val tile = Tile.of(lastStep.x, lastStep.y, entity.plane)
        entity.session.queuePacket(MinimapFlag(tile.getXInScene(entity.getSceneBaseChunkId()), tile.getYInScene(entity.getSceneBaseChunkId())))
    }
}
