package com.jagex.game.runetek5.movement.pathfinding

import com.jagex.game.runetek5.scene.CollisionMap

class GroundItemStrategy : RouteStrategy() {
    override fun canExit(size: Int, x: Int, y: Int, clipMap: CollisionMap): Boolean {
        return CollisionMap.canReach(
            x, y, size, size,
            super.destinationX, super.destinationY,
            super.destinationSizeX, super.destinationSizeY
        )
    }
}
