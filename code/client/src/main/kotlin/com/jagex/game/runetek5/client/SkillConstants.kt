package com.jagex.game.runetek5.client

import kotlin.math.pow

class SkillConstants {
    companion object {
        @JvmField
        val SKILL_MAX_LEVEL: IntArray = intArrayOf(
            99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99,
            99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 120
        )

        @JvmField
        val SKILL_XP_FOR_LEVEL: IntArray = IntArray(120)

        init {
            var sum = 0
            for (level in 0 until 120) {
                val mod = level + 1
                val offset = (mod + 300.0 * 2.0.pow(mod / 7.0)).toInt()
                sum += offset
                SKILL_XP_FOR_LEVEL[level] = sum / 4
            }
        }
    }
}
