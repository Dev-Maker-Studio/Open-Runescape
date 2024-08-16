package com.jagex.game.runetek5.movement.pathfinding;

import com.jagex.game.runetek5.scene.CollisionMap;

public class FixedTileStrategy extends RouteStrategy {

    @Override
    public boolean canExit(int size, int x, int y, CollisionMap clipping) {
        return x == super.destinationX && y == super.destinationY;
    }
}
