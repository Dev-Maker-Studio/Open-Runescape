package com.jagex.game.runetek5.movement.pathfinding

import com.jagex.game.runetek5.scene.CollisionMap

class FixedTileStrategy : RouteStrategy() {
    override fun canExit(size: Int, x: Int, y: Int, var4: CollisionMap): Boolean {
        return x == super.destinationX && y == super.destinationY
    }
}
