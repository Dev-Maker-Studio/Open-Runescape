package com.jagex.game.runetek5.movement.pathfinding;

import com.jagex.game.runetek5.scene.CollisionMap;

public class NPCStrategy extends RouteStrategy {

    public int clippingFlag;

    @Override
    public boolean canExit(int size, int srcX, int srcY, CollisionMap clipping) {
        return clipping.canReachNpc(srcX, srcY, size, super.destinationX, super.destinationY, super.destinationSizeX, super.destinationSizeY, this.clippingFlag);
    }
}
