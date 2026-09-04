package com.jagex.game.runetek5.math

import kotlin.math.cos
import kotlin.math.sin

class MatrixRotations {
    companion object {
        @JvmField
        val SIN: FloatArray = FloatArray(16384)

        @JvmField
        val COS: FloatArray = FloatArray(16384)

        init {
            val angle = 3.834951969714103E-4
            for (index in 0 until 16384) {
                SIN[index] = sin(index * angle).toFloat()
                COS[index] = cos(index * angle).toFloat()
            }
        }
    }
}
