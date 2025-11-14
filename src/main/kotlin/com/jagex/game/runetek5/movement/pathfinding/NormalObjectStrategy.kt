package com.jagex.game.runetek5.movement.pathfinding

import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.scene.CollisionMap

class NormalObjectStrategy : RouteStrategy() {
    @JvmField
    var rotation: Int = 0

    @JvmField
    var locShape: ObjectShapes? = null

    override fun canExit(size: Int, x: Int, y: Int, var4: CollisionMap): Boolean {
        return var4.canReachLoc(
            x, y, size,
            super.destinationX, super.destinationY,
            locShape!!.getValue(), rotation
        )
    }
}
