package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.ArrayUtils

/**
 * Texture operation bezier curve rendering class.
 */
class Class149_Sub2(
    @JvmField val anInt9306: Int,
    @JvmField val anInt9307: Int,
    @JvmField val anInt9305: Int,
    @JvmField val anInt9309: Int,
    @JvmField val anInt9308: Int,
    @JvmField val anInt9311: Int,
    @JvmField val anInt9312: Int,
    @JvmField val anInt9313: Int,
    i_9: Int,
    i_10: Int
) : Class149(-1, i_9, i_10) {

    override fun method2556(var1: Int, var2: Int) {
    }

    override fun method2557(var1: Int, var2: Int) {
    }

    override fun method2561(var1: Int, var2: Int) {
        val i_4 = anInt9306 * var1 shr 12
        val i_5 = var2 * anInt9307 shr 12
        val i_6 = anInt9305 * var1 shr 12
        val i_7 = var2 * anInt9309 shr 12
        val i_8 = anInt9308 * var1 shr 12
        val i_9 = var2 * anInt9311 shr 12
        val i_10 = anInt9312 * var1 shr 12
        val i_11 = var2 * anInt9313 shr 12
        method12399(i_4, i_5, i_6, i_7, i_8, i_9, i_10, i_11, anInt1741)
    }

    companion object {
        @JvmStatic
        fun method11220(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
            var i_01 = i_0
            var i_21 = i_2
            var i_15 = i_1
            var i_31 = i_3
            val i_6 = i_31 - i_15
            val i_7 = i_21 - i_01

            if (i_7 == 0) {
                if (i_6 != 0) {
                    Class149.method13411(i_01, i_15, i_31, i_4)
                }
            } else if (i_6 == 0) {
                ArrayUtils.method568(i_01, i_21, i_15, i_4)
            } else {
                var i_6_adj = if (i_6 < 0) -i_6 else i_6
                var i_7_adj = if (i_7 < 0) -i_7 else i_7
                val bool8 = i_6_adj > i_7_adj

                var i_9: Int
                var i_10: Int
                if (bool8) {
                    i_9 = i_01
                    i_10 = i_21
                    i_01 = i_15
                    i_15 = i_9
                    i_21 = i_31
                    i_31 = i_10
                }

                if (i_01 > i_21) {
                    i_9 = i_01
                    i_10 = i_15
                    i_01 = i_21
                    i_21 = i_9
                    i_15 = i_31
                    i_31 = i_10
                }

                i_9 = i_15
                i_10 = i_21 - i_01
                var i_11 = i_31 - i_15
                var i_12 = -(i_10 shr 1)
                val i_13 = if (i_15 < i_31) 1 else -1

                if (i_11 < 0) {
                    i_11 = -i_11
                }

                if (bool8) {
                    for (i_14 in i_01..i_21) {
                        Class149.anIntArrayArray7072!![i_14][i_9] = i_4
                        i_12 += i_11
                        if (i_12 > 0) {
                            i_9 += i_13
                            i_12 -= i_10
                        }
                    }
                } else {
                    for (i_14 in i_01..i_21) {
                        Class149.anIntArrayArray7072!![i_9][i_14] = i_4
                        i_12 += i_11
                        if (i_12 > 0) {
                            i_9 += i_13
                            i_12 -= i_10
                        }
                    }
                }
            }
        }

        @JvmStatic
        fun method12117(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int) {
            if (i_0 == i_2 && i_3 == i_1 && i_4 == i_6 && i_5 == i_7) {
                method11220(i_0, i_1, i_6, i_7, i_8)
            } else {
                var i_10 = i_0
                var i_11 = i_1
                val i_12 = i_0 * 3
                val i_13 = i_1 * 3
                val i_14 = i_2 * 3
                val i_15 = i_3 * 3
                val i_16 = i_4 * 3
                val i_17 = i_5 * 3
                val i_18 = i_6 - i_16 + i_14 - i_0
                val i_19 = i_15 + (i_7 - i_17) - i_1
                val i_20 = i_12 + (i_16 - i_14 - i_14)
                val i_21 = i_13 + (i_17 - i_15 - i_15)
                val i_22 = i_14 - i_12
                val i_23 = i_15 - i_13

                var i_24 = 128
                while (i_24 <= 4096) {
                    val i_25 = i_24 * i_24 shr 12
                    val i_26 = i_25 * i_24 shr 12
                    val i_27 = i_18 * i_26
                    val i_28 = i_26 * i_19
                    val i_29 = i_20 * i_25
                    val i_30 = i_25 * i_21
                    val i_31 = i_24 * i_22
                    val i_32 = i_24 * i_23
                    val i_33 = i_0 + (i_29 + i_27 + i_31 shr 12)
                    val i_34 = (i_28 + i_30 + i_32 shr 12) + i_1
                    method11220(i_10, i_11, i_33, i_34, i_8)
                    i_10 = i_33
                    i_11 = i_34
                    i_24 += 128
                }
            }
        }

        @JvmStatic
        fun method4779(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int) {
            if (i_0 == i_2 && i_3 == i_1 && i_4 == i_6 && i_5 == i_7) {
                Class149.method6159(i_0, i_1, i_6, i_7, i_8)
            } else {
                var i_10 = i_0
                var i_11 = i_1
                val i_12 = i_0 * 3
                val i_13 = i_1 * 3
                val i_14 = i_2 * 3
                val i_15 = i_3 * 3
                val i_16 = i_4 * 3
                val i_17 = i_5 * 3
                val i_18 = i_6 - i_16 + i_14 - i_0
                val i_19 = i_15 + (i_7 - i_17) - i_1
                val i_20 = i_12 + (i_16 - i_14 - i_14)
                val i_21 = i_13 + (i_17 - i_15 - i_15)
                val i_22 = i_14 - i_12
                val i_23 = i_15 - i_13

                var i_24 = 128
                while (i_24 <= 4096) {
                    val i_25 = i_24 * i_24 shr 12
                    val i_26 = i_24 * i_25 shr 12
                    val i_27 = i_18 * i_26
                    val i_28 = i_19 * i_26
                    val i_29 = i_25 * i_20
                    val i_30 = i_21 * i_25
                    val i_31 = i_22 * i_24
                    val i_32 = i_24 * i_23
                    val i_33 = i_0 + (i_29 + i_27 + i_31 shr 12)
                    val i_34 = (i_30 + i_28 + i_32 shr 12) + i_1
                    Class149.method6159(i_10, i_11, i_33, i_34, i_8)
                    i_10 = i_33
                    i_11 = i_34
                    i_24 += 128
                }
            }
        }

        @JvmStatic
        fun decode(buffer: Packet): Class149_Sub2 {
            return Class149_Sub2(
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readUMedium(),
                buffer.readUByte()
            )
        }

        @JvmStatic
        fun method12399(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int) {
            if (i_0 >= TextureOpRasterizer.anInt7071 && i_0 <= TextureOpRasterizer.anInt7069 &&
                i_2 >= TextureOpRasterizer.anInt7071 && i_2 <= TextureOpRasterizer.anInt7069 &&
                i_4 >= TextureOpRasterizer.anInt7071 && i_4 <= TextureOpRasterizer.anInt7069 &&
                i_6 >= TextureOpRasterizer.anInt7071 && i_6 <= TextureOpRasterizer.anInt7069 &&
                i_1 >= TextureOpRasterizer.anInt7070 && i_1 <= TextureOpRasterizer.anInt7068 &&
                i_3 >= TextureOpRasterizer.anInt7070 && i_3 <= TextureOpRasterizer.anInt7068 &&
                i_5 >= TextureOpRasterizer.anInt7070 && i_5 <= TextureOpRasterizer.anInt7068 &&
                i_7 >= TextureOpRasterizer.anInt7070 && i_7 <= TextureOpRasterizer.anInt7068
            ) {
                method12117(i_0, i_1, i_2, i_3, i_4, i_5, i_6, i_7, i_8)
            } else {
                method4779(i_0, i_1, i_2, i_3, i_4, i_5, i_6, i_7, i_8)
            }
        }
    }
}
