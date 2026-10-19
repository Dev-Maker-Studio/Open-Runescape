package com.jagex.game.runetek5.movement.pathfinding

import com.jagex.game.runetek5.config.`object`.ObjectShapes

/**
 * Factory for creating and configuring route strategies for pathfinding.
 */
object RouteStrategies {

    @JvmField val FIXED_TILE = FixedTileStrategy()
    @JvmField val GROUND_ITEM = GroundItemStrategy()
    @JvmField val PLAYER = PlayerStrategy()
    @JvmField val NPC = NPCStrategy()
    @JvmField val WALL_OBJECT = WallObjectStrategy()
    @JvmField val NORMAL_OBJECT = NormalObjectStrategy()

    /**
     * Creates a route strategy for ground items.
     *
     * @param x The destination x coordinate
     * @param y The destination y coordinate
     * @return The configured ground item route strategy
     */
    @JvmStatic
    fun forGroundItem(x: Int, y: Int): RouteStrategy {
        GROUND_ITEM.destinationX = x
        GROUND_ITEM.destinationY = y
        GROUND_ITEM.destinationSizeX = 1
        GROUND_ITEM.destinationSizeY = 1
        return GROUND_ITEM
    }

    /**
     * Creates a route strategy for players.
     *
     * @param x The destination x coordinate
     * @param y The destination y coordinate
     * @param sizeX The player width
     * @param sizeY The player height
     * @return The configured player route strategy
     */
    @JvmStatic
    fun forPlayer(x: Int, y: Int, sizeX: Int, sizeY: Int): RouteStrategy {
        PLAYER.destinationX = x
        PLAYER.destinationY = y
        PLAYER.destinationSizeX = sizeX
        PLAYER.destinationSizeY = sizeY
        PLAYER.clippingFlag = 0 // TODO
        return PLAYER
    }

    /**
     * Creates a route strategy for a fixed tile destination.
     *
     * @param x The destination x coordinate
     * @param y The destination y coordinate
     * @return The configured fixed tile route strategy
     */
    @JvmStatic
    fun forFixedTile(x: Int, y: Int): RouteStrategy {
        FIXED_TILE.destinationX = x
        FIXED_TILE.destinationY = y
        FIXED_TILE.destinationSizeX = 1
        FIXED_TILE.destinationSizeY = 1
        return FIXED_TILE
    }

    /**
     * Creates a route strategy for wall objects.
     *
     * @param x The destination x coordinate
     * @param y The destination y coordinate
     * @param sizeX The object width
     * @param sizeY The object height
     * @param shape The object shape
     * @param rotation The object rotation
     * @return The configured wall object route strategy
     */
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

    /**
     * Creates a route strategy for NPCs.
     *
     * @param x The destination x coordinate
     * @param y The destination y coordinate
     * @param sizeX The NPC width
     * @param sizeY The NPC height
     * @return The configured NPC route strategy
     */
    @JvmStatic
    fun forNpc(x: Int, y: Int, sizeX: Int, sizeY: Int): RouteStrategy {
        NPC.destinationX = x
        NPC.destinationY = y
        NPC.destinationSizeX = sizeX
        NPC.destinationSizeY = sizeY
        NPC.clippingFlag = 0
        return NPC
    }

    /**
     * Creates a route strategy for interactive objects.
     *
     * @param x The destination x coordinate
     * @param y The destination y coordinate
     * @param shape The object shape
     * @param rotation The object rotation
     * @return The configured interactive object route strategy
     */
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
