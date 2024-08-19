package com.jagex.game.runetek5.movement.pathfinding;

import com.jagex.game.runetek5.config.object.ObjectShapes;
import com.jagex.game.runetek5.scene.CollisionMap;

public class NormalObjectStrategy extends RouteStrategy {

    public int rotation;
    public ObjectShapes locShape;

    @Override
    public boolean canExit(int size, int srcX, int srcY, CollisionMap clipping) {
        return clipping.canReachLoc(srcX, srcY, size, super.destinationX, super.destinationY, this.locShape.getValue(), this.rotation);
    }
}
