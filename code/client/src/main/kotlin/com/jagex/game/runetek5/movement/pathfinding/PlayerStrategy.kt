package com.jagex.game.runetek5.movement.pathfinding

import com.jagex.game.runetek5.scene.CollisionMap

class PlayerStrategy : RouteStrategy() {
    @JvmField
    var clippingFlag: Int = 0

    override fun canExit(size: Int, x: Int, y: Int, clipMap: CollisionMap): Boolean {
        return clipMap.walkable(
            x, y, size, size,
            super.destinationX, super.destinationY,
            super.destinationSizeX, super.destinationSizeY,
            clippingFlag
        )
    }
}
