package com.jagex.game.runetek5.movement.pathfinding

import com.jagex.game.runetek5.scene.CollisionMap

abstract class RouteStrategy {
    @JvmField
    var destinationX: Int = 0

    @JvmField
    var destinationY: Int = 0

    @JvmField
    var destinationSizeX: Int = 0

    @JvmField
    var destinationSizeY: Int = 0

    abstract fun canExit(size: Int, x: Int, y: Int, clipMap: CollisionMap): Boolean
}
