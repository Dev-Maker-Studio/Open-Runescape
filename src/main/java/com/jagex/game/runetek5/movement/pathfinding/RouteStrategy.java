package com.jagex.game.runetek5.movement.pathfinding;

import com.jagex.game.runetek5.scene.CollisionMap;

public abstract class RouteStrategy {

    public int destinationX;
    public int destinationY;
    public int destinationSizeX;
    public int destinationSizeY;

    public abstract boolean canExit(int size, int x, int y, CollisionMap var4);
}
