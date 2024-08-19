package com.jagex.game.runetek5.config.object;

import com.jagex.game.runetek5.config.Identifiable;

public enum ObjectShapes implements Identifiable {
    WALL_STRAIGHT(0, 0),
    WALL_DIAGONAL_CORNER(1, 0),
    WALL_WHOLE_CORNER(2, 0),
    WALL_STRAIGHT_CORNER(3, 0),
    STRAIGHT_INSIDE_WALL_DEC(4, 1),
    STRAIGHT_OUSIDE_WALL_DEC(5, 1),
    DIAGONAL_OUTSIDE_WALL_DEC(6, 1),
    DIAGONAL_INSIDE_WALL_DEC(7, 1),
    DIAGONAL_INWALL_DEC(8, 1),
    WALL_INTERACT(9, 2),
    SCENERY_INTERACT(10, 2),
    GROUND_INTERACT(11, 2),
    STRAIGHT_SLOPE_ROOF(12, 2),
    DIAGONAL_SLOPE_ROOF(13, 2),
    DIAGONAL_SLOPE_CONNECT_ROOF(14, 2),
    STRAIGHT_SLOPE_CORNER_CONNECT_ROOF(15, 2),
    STRAIGHT_SLOPE_CORNER_ROOF(16, 2),
    STRAIGHT_FLAT_ROOF(17, 2),
    STRAIGHT_BOTTOM_EDGE_ROOF(18, 2),
    DIAGONAL_BOTTOM_EDGE_CONNECT_ROOF(19, 2),
    STRAIGHT_BOTTOM_EDGE_CONNECT_ROOF(20, 2),
    STRAIGHT_BOTTOM_EDGE_CONNECT_CORNER_ROOF(21, 2),
    GROUND_DECORATION(22, 3);

    public final int shape;
    public final int group;
	public static final int[] BOUNDARY_CULL_TYPES = {16, 32, 64, 128};
	public static final int[] OBJECT_CULL_TYPES = {1, 2, 4, 8};

    ObjectShapes(int type, int group) {
        this.shape = type;
        this.group = group;
    }
    
    @Override
    public int getValue() {
        return this.shape;
    }

    public static byte method7853(int shape, int i_1) {
	    return (byte) (shape != WALL_INTERACT.shape ? 0 : ((i_1 & 0x1) == 0 ? 1 : 2));
	}

	public static int getWallCullType(int type, int rotation) {
        return type == WALL_DIAGONAL_CORNER.shape || type == WALL_STRAIGHT_CORNER.shape         		
    		? ObjectShapes.BOUNDARY_CULL_TYPES[rotation & 0x3]
    		: ObjectShapes.OBJECT_CULL_TYPES[rotation & 0x3];
    }

    public static byte getRotatedWall(int type, int rotation) {
        return (byte) (type != WALL_INTERACT.shape ? 0 : ((rotation & 0x1) == 0 ? 1 : 2));
    }
    
    public static boolean isWallDecoration(int type) {
        return type >= STRAIGHT_INSIDE_WALL_DEC.shape && type <= DIAGONAL_INWALL_DEC.shape;
    }

    public static boolean isRoofEdge(int type) {
        return type >= STRAIGHT_BOTTOM_EDGE_ROOF.shape && type <= STRAIGHT_BOTTOM_EDGE_CONNECT_CORNER_ROOF.shape;
    }

    public static boolean isRoof(int tyoe) {
        return tyoe >= STRAIGHT_SLOPE_ROOF.shape && tyoe <= STRAIGHT_FLAT_ROOF.shape;
    }

	public static boolean isWall(int type) {
	    return type >= WALL_STRAIGHT.shape && type <= WALL_STRAIGHT_CORNER.shape || type == WALL_INTERACT.shape;
	}
}
