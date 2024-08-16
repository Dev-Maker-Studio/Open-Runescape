package com.jagex.game.runetek5.movement.pathfinding;

import com.jagex.game.runetek5.scene.CollisionMap;

public class GroundItemStrategy extends RouteStrategy {

    @Override
    public boolean canExit(int size, int x, int y, CollisionMap clipping) {
        return CollisionMap.canReach(x, y, size, size, super.destinationX, super.destinationY, super.destinationSizeX, super.destinationSizeY);
    }
}
