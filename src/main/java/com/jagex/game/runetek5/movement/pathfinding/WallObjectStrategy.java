package com.jagex.game.runetek5.movement.pathfinding;

import com.jagex.game.runetek5.config.object.ObjectShapes;
import com.jagex.game.runetek5.scene.CollisionMap;

public class WallObjectStrategy extends RouteStrategy {

    public int rotation;
    public ObjectShapes locShape;

    @Override
    public boolean canExit(int srcY, int size, int srcX, CollisionMap clipping) {
        return clipping.canReachLoc(size, srcX, srcY, super.destinationX, super.destinationY, this.locShape.getValue(), this.rotation);
    }

}
