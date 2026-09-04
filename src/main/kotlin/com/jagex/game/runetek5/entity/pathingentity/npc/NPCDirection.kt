package com.jagex.game.runetek5.entity.pathingentity.npc

import com.jagex.game.runetek5.config.Identifiable

enum class NPCDirection(
    @JvmField val value: Int,
    @JvmField val direction: Int
) : Identifiable {
    NORTH(0, 0),
    EAST(2, 1),
    SOUTH_EAST(3, 2),
    NORTH_WEST(7, 3),
    SOUTH(6, 4),
    WEST(4, 5),
    NORTH_EAST(5, 6),
    SOUTH_WEST(1, 7);

    fun getRotation(): NPCDirection {
        return when (this.value) {
            0 -> SOUTH
            1 -> NORTH_WEST
            2 -> WEST
            3 -> NORTH_EAST
            4 -> EAST
            5 -> SOUTH_EAST
            6 -> NORTH
            7 -> SOUTH_WEST
            else -> throw IllegalStateException()
        }
    }

    override fun getValue(): Int {
        return this.direction
    }
}
