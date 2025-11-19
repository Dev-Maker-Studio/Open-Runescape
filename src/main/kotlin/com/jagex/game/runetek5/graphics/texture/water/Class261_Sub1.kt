package com.jagex.game.runetek5.graphics.texture.water

import java.util.Random

internal class Class261_Sub1 : Class261() {

    @JvmField val anIntArray8042: IntArray = IntArray(512)

    init {
        val random2 = Random(419684)

        var i_3: Int
        for (i_3 in 0 until 256) {
            this.anIntArray8042[i_3 + 256] = i_3
            this.anIntArray8042[i_3] = this.anIntArray8042[i_3 + 256]
        }

        for (i_3 in 0 until 256) {
            val i_4 = random2.nextInt() and 0xff
            val i_5 = this.anIntArray8042[i_4]
            this.anIntArray8042[i_4 + 256] = this.anIntArray8042[i_3]
            this.anIntArray8042[i_4] = this.anIntArray8042[i_4 + 256]
            this.anIntArray8042[i_3 + 256] = i_5
            this.anIntArray8042[i_3] = this.anIntArray8042[i_3 + 256]
        }
    }

    override fun method4634(i_1: Int, f_5: Float, f_6: Float, f_7: Float, f_8: Float, floats_9: FloatArray) {
        var start = 0
        var i_11 = (128 * f_5 - 1.0f).toInt()
        i_11 = i_11 and 0xff
        var i_12 = (128 * f_6 - 1.0f).toInt()
        i_12 = i_12 and 0xff
        var i_13 = (16 * f_7 - 1.0f).toInt()
        i_13 = i_13 and 0xff
        val f_14 = i_1 * f_7
        var i_15 = f_14.toInt()
        var i_16 = i_15 + 1
        val f_17 = f_14 - i_15
        val f_18 = 1.0f - f_17
        val f_19 = method13372(f_17)
        i_15 = i_15 and i_13
        i_16 = i_16 and i_13
        val i_20 = this.anIntArray8042[i_15]
        val i_21 = this.anIntArray8042[i_16]

        for (i_22 in 0 until 128) {
            val f_23 = i_22 * f_6
            var i_24 = f_23.toInt()
            var i_25 = i_24 + 1
            val f_26 = f_23 - i_24
            val f_27 = 1.0f - f_26
            val f_28 = method13372(f_26)
            i_24 = i_24 and i_12
            i_25 = i_25 and i_12
            val i_29 = this.anIntArray8042[i_24 + i_20]
            val i_30 = this.anIntArray8042[i_25 + i_20]
            val i_31 = this.anIntArray8042[i_24 + i_21]
            val i_32 = this.anIntArray8042[i_25 + i_21]

            for (i_33 in 0 until 128) {
                val f_34 = i_33 * f_5
                var i_35 = f_34.toInt()
                var i_36 = i_35 + 1
                val f_37 = f_34 - i_35
                val f_38 = 1.0f - f_37
                val f_39 = method13372(f_37)
                i_35 = i_35 and i_11
                i_36 = i_36 and i_11
                floats_9[start++] = f_8 * method13369(method13369(method13369(method13370(this.anIntArray8042[i_35 + i_29] and 0x7, f_38, f_27, f_18), method13370(this.anIntArray8042[i_36 + i_29] and 0x7, f_37, f_27, f_18), f_39), method13369(method13370(this.anIntArray8042[i_35 + i_30] and 0x7, f_38, f_26, f_18), method13370(this.anIntArray8042[i_36 + i_30] and 0x7, f_37, f_26, f_18), f_39), f_28), method13369(method13369(method13370(this.anIntArray8042[i_35 + i_31] and 0x7, f_38, f_27, f_17), method13370(this.anIntArray8042[i_36 + i_31] and 0x7, f_37, f_27, f_17), f_39), method13369(method13370(this.anIntArray8042[i_35 + i_32] and 0x7, f_38, f_26, f_17), method13370(this.anIntArray8042[i_36 + i_32] and 0x7, f_37, f_26, f_17), f_39), f_28), f_19)
            }
        }
    }

    companion object {
        @JvmField val aFloatArrayArray8043: Array<FloatArray> = arrayOf(floatArrayOf(-0.333333f, -0.333333f, -0.333333f), floatArrayOf(0.333333f, -0.333333f, -0.333333f), floatArrayOf(-0.333333f, 0.333333f, -0.333333f), floatArrayOf(0.333333f, 0.333333f, -0.333333f), floatArrayOf(-0.333333f, -0.333333f, 0.333333f), floatArrayOf(0.333333f, -0.333333f, 0.333333f), floatArrayOf(-0.333333f, 0.333333f, 0.333333f), floatArrayOf(0.333333f, 0.333333f, 0.333333f))

        @JvmStatic
        fun method13369(f_0: Float, f_1: Float, f_2: Float): Float {
            return f_0 + (f_1 - f_0) * f_2
        }

        @JvmStatic
        fun method13370(i_0: Int, f_1: Float, f_2: Float, f_3: Float): Float {
            val floats_4 = aFloatArrayArray8043[i_0]
            return floats_4[0] * f_1 + floats_4[1] * f_2 + floats_4[2] * f_3
        }

        @JvmStatic
        fun method13372(f_0: Float): Float {
            return f_0 * f_0 * f_0 * (10.0f + (f_0 * 6.0f - 15.0f) * f_0)
        }
    }
}
