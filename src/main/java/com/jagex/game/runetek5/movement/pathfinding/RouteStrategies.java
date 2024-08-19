package com.jagex.game.runetek5.movement.pathfinding;

import com.jagex.game.runetek5.config.object.ObjectShapes;

public class RouteStrategies {

	public static final FixedTileStrategy FIXED_TILE = new FixedTileStrategy();
	public static final GroundItemStrategy GROUND_ITEM = new GroundItemStrategy();
	public static final PlayerStrategy PLAYER = new PlayerStrategy();
    public static final NPCStrategy NPC = new NPCStrategy();
    public static final WallObjectStrategy WALL_OBJECT = new WallObjectStrategy();
    public static final NormalObjectStrategy NORMAL_OBJECT = new NormalObjectStrategy();

    RouteStrategies() {
        throw new Error();
    }
    
    public static RouteStrategy forGroundItem(int x, int y) {
        RouteStrategies.GROUND_ITEM.destinationX = x;
        RouteStrategies.GROUND_ITEM.destinationY = y;
        RouteStrategies.GROUND_ITEM.destinationSizeX = 1;
        RouteStrategies.GROUND_ITEM.destinationSizeY = 1;
        return RouteStrategies.GROUND_ITEM;
    }
    
    public static RouteStrategy forPlayer(int x, int y, int sizeX, int sizeY) {
        RouteStrategies.PLAYER.destinationX = x;
        RouteStrategies.PLAYER.destinationY = y;
        RouteStrategies.PLAYER.destinationSizeX = sizeX;
        RouteStrategies.PLAYER.destinationSizeY = sizeY;
        RouteStrategies.PLAYER.clippingFlag = 0; //TODO:
        return RouteStrategies.PLAYER;
    }

    public static RouteStrategy forFixedTile(int x, int y) {
        FIXED_TILE.destinationX = x;
        FIXED_TILE.destinationY = y;
        FIXED_TILE.destinationSizeX = 1;
        FIXED_TILE.destinationSizeY = 1;
        return FIXED_TILE;
    }

    public static RouteStrategy forWallObject(int x, int y, int sizeX, int sizeY, ObjectShapes shape, int i_5) {
        WALL_OBJECT.destinationX = x;
        WALL_OBJECT.destinationY = y;
        WALL_OBJECT.destinationSizeX = sizeX;
        WALL_OBJECT.destinationSizeY = sizeY;
        WALL_OBJECT.locShape = shape;
        WALL_OBJECT.rotation = i_5;
        return WALL_OBJECT;
    }

    public static RouteStrategy forNpc(int x, int y, int sizeX, int sizeY) {
        NPC.destinationX = x;
        NPC.destinationY = y;
        NPC.destinationSizeX = sizeX;
        NPC.destinationSizeY = sizeY;
        NPC.clippingFlag = 0;
        return NPC;
    }

	public static RouteStrategy forInteractiveObject(int x, int y, ObjectShapes shape, int i_5) {
	    NORMAL_OBJECT.destinationX = x;
	    NORMAL_OBJECT.destinationY = y;
	    NORMAL_OBJECT.destinationSizeX = 0;
	    NORMAL_OBJECT.destinationSizeY = 0;
	    NORMAL_OBJECT.locShape = shape;
	    NORMAL_OBJECT.rotation = i_5;
	    return NORMAL_OBJECT;
	}
}
