package org.darkan.core.map.pathfinder

import org.darkan.core.Utils

enum class Direction(@JvmField val id: Int, @JvmField val dx: Int, @JvmField val dy: Int) {
    NORTH(0, 0, 1),
    NORTHEAST(1, 1, 1),
    EAST(2, 1, 0),
    SOUTHEAST(3, 1, -1),
    SOUTH(4, 0, -1),
    SOUTHWEST(5, -1, -1),
    WEST(6, -1, 0),
    NORTHWEST(7, -1, 1);

    val angle: Int
        get() = Utils.getAngleTo(dx, dy)

    val isDiagonal: Boolean
        get() = dx != 0 && dy != 0

    companion object {
        @JvmStatic
		fun random(): Direction {
            return entries[Utils.random(entries.size)]
        }

        @JvmStatic
		fun getById(id: Int): Direction {
            return when (id) {
                0 -> NORTH
                1 -> NORTHEAST
                2 -> EAST
                3 -> SOUTHEAST
                4 -> SOUTH
                5 -> SOUTHWEST
                6 -> WEST
                7 -> NORTHWEST
                else -> SOUTH
            }
        }

        @JvmStatic
		fun rotateClockwise(dir: Direction, rotation: Int): Direction {
            return getById((dir.id + rotation) and 0x7)
        }

        @JvmStatic
		fun fromAngle(angle: Int): Direction? {
            val delta = Utils.getDirection(angle) ?: return SOUTH
            return forDelta(delta[0].toInt(), delta[1].toInt())
        }

        @JvmStatic
		fun forDelta(dx: Int, dy: Int): Direction? {
            if (dy >= 1 && dx >= 1) return NORTHEAST
            if (dy <= -1 && dx >= 1) return SOUTHEAST
            return if (dy <= -1 && dx <= -1) SOUTHWEST
            else if (dy >= 1 && dx <= -1) NORTHWEST
            else if (dy >= 1) NORTH
            else if (dx >= 1) EAST
            else if (dy <= -1) SOUTH
            else if (dx <= -1) WEST
            else null
        }
    }
}
