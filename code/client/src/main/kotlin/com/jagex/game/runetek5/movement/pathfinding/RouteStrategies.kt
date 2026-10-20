package com.jagex.game.runetek5.movement.pathfinding

import com.jagex.game.runetek5.config.`object`.ObjectShapes

object RouteStrategies {

    @JvmField val FIXED_TILE = FixedTileStrategy()
    @JvmField val GROUND_ITEM = GroundItemStrategy()
    @JvmField val PLAYER = PlayerStrategy()
    @JvmField val NPC = NPCStrategy()
    @JvmField val WALL_OBJECT = WallObjectStrategy()
    @JvmField val NORMAL_OBJECT = NormalObjectStrategy()

    @JvmStatic
    fun forGroundItem(x: Int, y: Int): RouteStrategy {
        GROUND_ITEM.destinationX = x
        GROUND_ITEM.destinationY = y
        GROUND_ITEM.destinationSizeX = 1
        GROUND_ITEM.destinationSizeY = 1
        return GROUND_ITEM
    }

    @JvmStatic
    fun forPlayer(x: Int, y: Int, sizeX: Int, sizeY: Int): RouteStrategy {
        PLAYER.destinationX = x
        PLAYER.destinationY = y
        PLAYER.destinationSizeX = sizeX
        PLAYER.destinationSizeY = sizeY
        PLAYER.clippingFlag = 0
        return PLAYER
    }

    @JvmStatic
    fun forFixedTile(x: Int, y: Int): RouteStrategy {
        FIXED_TILE.destinationX = x
        FIXED_TILE.destinationY = y
        FIXED_TILE.destinationSizeX = 1
        FIXED_TILE.destinationSizeY = 1
        return FIXED_TILE
    }

    @JvmStatic
    fun forWallObject(x: Int, y: Int, sizeX: Int, sizeY: Int, shape: ObjectShapes, rotation: Int): RouteStrategy {
        WALL_OBJECT.destinationX = x
        WALL_OBJECT.destinationY = y
        WALL_OBJECT.destinationSizeX = sizeX
        WALL_OBJECT.destinationSizeY = sizeY
        WALL_OBJECT.locShape = shape
        WALL_OBJECT.rotation = rotation
        return WALL_OBJECT
    }

    @JvmStatic
    fun forNpc(x: Int, y: Int, sizeX: Int, sizeY: Int): RouteStrategy {
        NPC.destinationX = x
        NPC.destinationY = y
        NPC.destinationSizeX = sizeX
        NPC.destinationSizeY = sizeY
        NPC.clippingFlag = 0
        return NPC
    }

    @JvmStatic
    fun forInteractiveObject(x: Int, y: Int, shape: ObjectShapes, rotation: Int): RouteStrategy {
        NORMAL_OBJECT.destinationX = x
        NORMAL_OBJECT.destinationY = y
        NORMAL_OBJECT.destinationSizeX = 0
        NORMAL_OBJECT.destinationSizeY = 0
        NORMAL_OBJECT.locShape = shape
        NORMAL_OBJECT.rotation = rotation
        return NORMAL_OBJECT
    }
}
