package com.jagex.game.runetek5.graphics.texture.water

import java.util.Random
import com.jagex.game.runetek5.staticmethods.StaticMethods
import kotlin.math.pow

abstract class WaterTextureGenerator internal constructor(
    @JvmField val anInt4283: Int,
    i_3: Int,
    i_4: Int
) {

    @JvmField val aShortArray4285: ShortArray = ShortArray(512)
    @JvmField var anInt4286: Int = 4
    @JvmField var anInt4288: Int = 4
    @JvmField var anInt4289: Int = 4
    @JvmField var anInt4290: Int = 4
    @JvmField var aShortArray4281: ShortArray? = null

    init {
        anInt4286 = i_3
        anInt4288 = i_4
        anInt4289 = 2
        anInt4290 = 5
        method6323()
        method6325()
    }

    abstract fun reset()

    fun method6322() {
        val ints_4 = IntArray(128)
        val ints_5 = IntArray(128)
        val ints_6 = IntArray(16)

        var i_7: Int
        for (i_7 in 0 until 128) {
            ints_4[i_7] = (i_7 shl 12) / 128
        }

        for (i_7 in 0 until 128) {
            ints_5[i_7] = (i_7 shl 12) / 128
        }

        for (i_7 in 0 until 16) {
            ints_6[i_7] = (i_7 shl 12) / 16
        }

        clear()

        for (i_7 in 0 until 16) {
            for (i_8 in 0 until 128) {
                for (i_9 in 0 until 128) {
                    for (i_10 in 0 until anInt4290) {
                        val i_11 = aShortArray4281!![i_10].toInt() shl 12
                        val i_12 = i_11 * anInt4286 shr 12
                        val i_13 = i_11 * anInt4288 shr 12
                        val i_14 = i_11 * anInt4289 shr 12
                        var i_15 = i_11 * ints_4[i_9] shr 12
                        var i_16 = i_11 * ints_5[i_8] shr 12
                        var i_17 = i_11 * ints_6[i_7] shr 12
                        i_15 *= anInt4286
                        i_16 *= anInt4288
                        i_17 *= anInt4289
                        var i_18 = i_15 shr 12
                        var i_19 = i_18 + 1
                        i_18 = i_18 and 0xff
                        var i_20 = i_16 shr 12
                        var i_21 = i_20 + 1
                        i_20 = i_20 and 0xff
                        var i_22 = i_17 shr 12
                        var i_23 = i_22 + 1
                        i_22 = i_22 and 0xff
                        if (i_19 >= i_12) {
                            i_19 = 0
                        } else {
                            i_19 = i_19 and 0xff
                        }

                        if (i_21 >= i_13) {
                            i_21 = 0
                        } else {
                            i_21 = i_21 and 0xff
                        }

                        if (i_23 >= i_14) {
                            i_23 = 0
                        } else {
                            i_23 = i_23 and 0xff
                        }

                        i_15 = i_15 and 0xfff
                        i_16 = i_16 and 0xfff
                        i_17 = i_17 and 0xfff
                        val i_24 = SINE_WAVE[i_15]
                        val i_25 = SINE_WAVE[i_16]
                        val i_26 = SINE_WAVE[i_17]
                        val i_27 = i_15 - 4096
                        val i_28 = i_16 - 4096
                        val i_29 = i_17 - 4096
                        val s_30 = aShortArray4285[i_22]
                        val s_31 = aShortArray4285[i_23]
                        val s_32 = aShortArray4285[i_20 + s_30]
                        val s_33 = aShortArray4285[i_21 + s_30]
                        val s_34 = aShortArray4285[i_20 + s_31]
                        val s_35 = aShortArray4285[i_21 + s_31]
                        var i_36 = method6324(aShortArray4285[i_18 + s_32].toInt(), i_15, i_16, i_17)
                        var i_37 = method6324(aShortArray4285[i_19 + s_32].toInt(), i_27, i_16, i_17)
                        val i_38 = i_36 + (i_24 * (i_37 - i_36) shr 12)
                        i_36 = method6324(aShortArray4285[i_18 + s_33].toInt(), i_15, i_28, i_17)
                        i_37 = method6324(aShortArray4285[i_19 + s_33].toInt(), i_27, i_28, i_17)
                        val i_39 = i_36 + (i_24 * (i_37 - i_36) shr 12)
                        val i_40 = i_38 + (i_25 * (i_39 - i_38) shr 12)
                        i_36 = method6324(aShortArray4285[i_18 + s_34].toInt(), i_15, i_16, i_29)
                        i_37 = method6324(aShortArray4285[i_19 + s_34].toInt(), i_27, i_16, i_29)
                        val i_38b = i_36 + (i_24 * (i_37 - i_36) shr 12)
                        i_36 = method6324(aShortArray4285[i_18 + s_35].toInt(), i_15, i_28, i_29)
                        i_37 = method6324(aShortArray4285[i_19 + s_35].toInt(), i_27, i_28, i_29)
                        val i_39b = i_36 + (i_24 * (i_37 - i_36) shr 12)
                        val i_41 = i_38b + (i_25 * (i_39b - i_38b) shr 12)
                        update(i_10, i_40 + (i_26 * (i_41 - i_40) shr 12))
                    }

                    reset()
                }
            }
        }
    }

    fun method6323() {
        aShortArray4281 = ShortArray(anInt4290)

        for (i_1 in 0 until anInt4290) {
            aShortArray4281!![i_1] = 2.0.pow(i_1.toDouble()).toInt().toShort()
        }
    }

    fun method6325() {
        val random1 = Random(anInt4283.toLong())

        var i_2: Int
        for (i_2 in 0 until 255) {
            aShortArray4285[i_2] = i_2.toShort()
        }

        for (i_2 in 0 until 255) {
            val i_3 = 255 - i_2
            val i_4 = StaticMethods.method7931(random1, i_3)
            val s_5 = aShortArray4285[i_4]
            aShortArray4285[i_4] = aShortArray4285[i_3]
            aShortArray4285[i_3 + 256] = s_5
            aShortArray4285[i_3] = aShortArray4285[i_3 + 256]
        }
    }

    abstract fun clear()

    abstract fun update(var1: Int, var2: Int)

    companion object {
        @JvmField
        val SINE_WAVE: IntArray = IntArray(4096)

        init {
            for (i_0 in 0 until 4096) {
                SINE_WAVE[i_0] = method6326(i_0)
            }
        }

        @JvmStatic
        fun method6324(i_0: Int, i_1: Int, i_2: Int, i_3: Int): Int {
            val i_4 = i_0 and 0xf
            val i_5 = if (i_4 < 8) i_1 else i_2
            val i_6 = if (i_4 < 4) i_2 else if (i_4 != 12 && i_4 != 14) i_3 else i_1
            return (if ((i_4 and 0x1) == 0) i_5 else -i_5) + (if ((i_4 and 0x2) == 0) i_6 else -i_6)
        }

        @JvmStatic
        fun method6326(i_0: Int): Int {
            val i_1 = i_0 * (i_0 * i_0 shr 12) shr 12
            val i_2 = i_0 * 6 - 61440
            val i_3 = (i_0 * i_2 shr 12) + 40960
            return i_3 * i_1 shr 12
        }
    }
}
