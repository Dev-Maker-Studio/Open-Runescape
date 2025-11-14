package com.jagex.game.runetek5.movement.pathfinding

import com.jagex.game.runetek5.scene.CollisionMap

class NPCStrategy : RouteStrategy() {
    @JvmField
    var clippingFlag: Int = 0

    override fun canExit(size: Int, x: Int, y: Int, var4: CollisionMap): Boolean {
        return var4.canReachNpc(
            x, y, size,
            super.destinationX, super.destinationY,
            super.destinationSizeX, super.destinationSizeY,
            clippingFlag
        )
    }
}
