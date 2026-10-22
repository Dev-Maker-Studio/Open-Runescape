package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

object ColorClear {
    const val TEX_INDEX = -2f

    fun rectangle(width: Int, height: Int, scissor: IntArray?): IntArray? {
        require(width > 0 && height > 0)
        if (scissor == null) return intArrayOf(0, 0, width, height)
        require(scissor.size == 4 && scissor[2] >= 0 && scissor[3] >= 0)
        val left = scissor[0].toLong().coerceIn(0, width.toLong())
        val right = (scissor[0].toLong() + scissor[2]).coerceIn(0, width.toLong())
        val bottom = scissor[1].toLong().coerceIn(0, height.toLong())
        val top = (scissor[1].toLong() + scissor[3]).coerceIn(0, height.toLong())
        if (left >= right || bottom >= top) return null
        return intArrayOf(left.toInt(), height - top.toInt(), (right - left).toInt(), (top - bottom).toInt())
    }

    fun enabled(mask: BooleanArray): Boolean {
        require(mask.size == 4)
        if (mask.none { it }) return false
        require(mask.all { it }) { "color clear with partial color write mask unsupported" }
        return true
    }

    fun vertices(rect: IntArray, color: FloatArray): ByteArray {
        require(rect.size == 4 && color.size == 4 && color.all { it.isFinite() })
        val rgba = ByteArray(4) { (color[it].coerceIn(0f, 1f) * 255f).roundToInt().toByte() }
        val left = rect[0].toFloat()
        val top = rect[1].toFloat()
        val right = left + rect[2]
        val bottom = top + rect[3]
        val out = ByteBuffer.allocate(4 * Passes.sprite.stride).order(ByteOrder.LITTLE_ENDIAN)
        for ((x, y) in arrayOf(left to top, right to top, right to bottom, left to bottom)) {
            out.putFloat(x).putFloat(y).putFloat(0f).putFloat(0f).put(rgba).putFloat(TEX_INDEX)
            out.putFloat(left).putFloat(top).putFloat(right).putFloat(bottom)
        }
        return out.array()
    }

    fun projection(width: Int, height: Int) = floatArrayOf(
        2f / width, 0f, 0f, 0f,
        0f, -2f / height, 0f, 0f,
        0f, 0f, 1f, 0f,
        -1f, 1f, 0f, 1f
    )
}
