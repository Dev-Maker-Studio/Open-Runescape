package com.jagex.game.runetek5.config.`object`

import com.jagex.game.runetek5.config.Identifiable

enum class ObjectShapes(@JvmField val shape: Int, @JvmField val group: Int) : Identifiable {
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

    override fun getValue(): Int {
        return shape
    }

    companion object {
        @JvmField val BOUNDARY_CULL_TYPES = intArrayOf(16, 32, 64, 128)
        @JvmField val OBJECT_CULL_TYPES = intArrayOf(1, 2, 4, 8)

        @JvmStatic
        fun getWallInteractByte(shape: Int, rotation: Int): Byte {
            return if (shape != WALL_INTERACT.shape) 0 else if ((rotation and 0x1) == 0) 1 else 2
        }

        @JvmStatic
        fun getWallCullType(type: Int, rotation: Int): Int {
            return if (type == WALL_DIAGONAL_CORNER.shape || type == WALL_STRAIGHT_CORNER.shape) {
                BOUNDARY_CULL_TYPES[rotation and 0x3]
            } else {
                OBJECT_CULL_TYPES[rotation and 0x3]
            }
        }

        @JvmStatic
        fun getRotatedWall(type: Int, rotation: Int): Byte {
            return if (type != WALL_INTERACT.shape) 0 else if ((rotation and 0x1) == 0) 1 else 2
        }

        @JvmStatic
        fun isWallDecoration(type: Int): Boolean {
            return type >= STRAIGHT_INSIDE_WALL_DEC.shape && type <= DIAGONAL_INWALL_DEC.shape
        }

        @JvmStatic
        fun isRoofEdge(type: Int): Boolean {
            return type >= STRAIGHT_BOTTOM_EDGE_ROOF.shape && type <= STRAIGHT_BOTTOM_EDGE_CONNECT_CORNER_ROOF.shape
        }

        @JvmStatic
        fun isRoof(type: Int): Boolean {
            return type >= STRAIGHT_SLOPE_ROOF.shape && type <= STRAIGHT_FLAT_ROOF.shape
        }

        @JvmStatic
        fun isWall(type: Int): Boolean {
            return (type >= WALL_STRAIGHT.shape && type <= WALL_STRAIGHT_CORNER.shape) || type == WALL_INTERACT.shape
        }
    }
}
