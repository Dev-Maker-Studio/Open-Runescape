package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.utils.ArrayUtils

class Class149_Sub4(
    i_1: Int,
    i_2: Int,
    i_3: Int,
    i_4: Int,
    i_5: Int,
    i_6: Int,
    i_7: Int
) : Class149(i_5, i_6, i_7) {

    @JvmField val anInt9391: Int = i_1
    @JvmField val anInt9393: Int = i_2
    @JvmField val anInt9392: Int = i_3
    @JvmField val anInt9394: Int = i_4

    override fun method2561(i_1: Int, i_2: Int) {
        val i_4 = anInt9391 * i_1 shr 12
        val i_5 = anInt9392 * i_1 shr 12
        val i_6 = i_2 * anInt9393 shr 12
        val i_7 = i_2 * anInt9394 shr 12
        method1805(i_4, i_5, i_6, i_7, anInt1741, anInt1742)
    }

    override fun method2556(i_1: Int, i_2: Int) {
        val i_4 = anInt9391 * i_1 shr 12
        val i_5 = anInt9392 * i_1 shr 12
        val i_6 = i_2 * anInt9393 shr 12
        val i_7 = i_2 * anInt9394 shr 12
        StaticMethods.method6772(i_4, i_5, i_6, i_7, anInt1743)
    }

    override fun method2557(i_1: Int, i_2: Int) {
        val i_4 = anInt9391 * i_1 shr 12
        val i_5 = anInt9392 * i_1 shr 12
        val i_6 = i_2 * anInt9393 shr 12
        val i_7 = i_2 * anInt9394 shr 12
        StaticMethods.method6731(i_4, i_5, i_6, i_7, anInt1743, anInt1741, anInt1742)
    }

    companion object {
        @JvmStatic
        fun decode(rsbytebuffer0: Packet): Class149_Sub4 {
            return Class149_Sub4(
                rsbytebuffer0.readShort(),
                rsbytebuffer0.readShort(),
                rsbytebuffer0.readShort(),
                rsbytebuffer0.readShort(),
                rsbytebuffer0.readUMedium(),
                rsbytebuffer0.readUMedium(),
                rsbytebuffer0.readUByte()
            )
        }

        @JvmStatic
        fun method4561(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
            val i_7 = i_5 + i_2
            val i_8 = i_3 - i_5
            var i_9: Int
            i_9 = i_2
            while (i_9 < i_7) {
                ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072!![i_9], i_0, i_1, i_4)
                i_9++
            }
            i_9 = i_3
            while (i_9 > i_8) {
                ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072!![i_9], i_0, i_1, i_4)
                --i_9
            }
            i_9 = i_5 + i_0
            val i_10 = i_1 - i_5
            for (i_11 in i_7..i_8) {
                val ints_12 = Class149.anIntArrayArray7072!![i_11]
                ArrayUtils.setArrayValuesTo(ints_12, i_0, i_9, i_4)
                ArrayUtils.setArrayValuesTo(ints_12, i_10, i_1, i_4)
            }
        }

        @JvmStatic
        fun method1388(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
            var i_01 = i_0
            var i_11 = i_1
            var i_21 = i_2
            var i_31 = i_3
            if (i_21 <= TextureOpRasterizer.anInt7068 && i_31 >= TextureOpRasterizer.anInt7070) {
                val bool6: Boolean
                if (i_01 < TextureOpRasterizer.anInt7071) {
                    i_01 = TextureOpRasterizer.anInt7071
                    bool6 = false
                } else if (i_01 > TextureOpRasterizer.anInt7069) {
                    i_01 = TextureOpRasterizer.anInt7069
                    bool6 = false
                } else {
                    bool6 = true
                }
                val bool7: Boolean
                if (i_11 < TextureOpRasterizer.anInt7071) {
                    i_11 = TextureOpRasterizer.anInt7071
                    bool7 = false
                } else if (i_11 > TextureOpRasterizer.anInt7069) {
                    i_11 = TextureOpRasterizer.anInt7069
                    bool7 = false
                } else {
                    bool7 = true
                }
                if (i_21 >= TextureOpRasterizer.anInt7070) {
                    ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072!![i_21++], i_01, i_11, i_4)
                } else {
                    i_21 = TextureOpRasterizer.anInt7070
                }
                if (i_31 <= TextureOpRasterizer.anInt7068) {
                    ArrayUtils.setArrayValuesTo(Class149.anIntArrayArray7072!![i_31--], i_01, i_11, i_4)
                } else {
                    i_31 = TextureOpRasterizer.anInt7068
                }
                var i_8: Int
                if (bool6 && bool7) {
                    i_8 = i_21
                    while (i_8 <= i_31) {
                        val ints_9 = Class149.anIntArrayArray7072!![i_8]
                        ints_9[i_01] = i_4
                        ints_9[i_11] = i_4
                        i_8++
                    }
                } else if (bool6) {
                    i_8 = i_21
                    while (i_8 <= i_31) {
                        Class149.anIntArrayArray7072!![i_8][i_01] = i_4
                        i_8++
                    }
                } else if (bool7) {
                    i_8 = i_21
                    while (i_8 <= i_31) {
                        Class149.anIntArrayArray7072!![i_8][i_11] = i_4
                        i_8++
                    }
                }
            }
        }

        @JvmStatic
        fun method1805(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
            if (i_0 >= TextureOpRasterizer.anInt7071 && i_1 <= TextureOpRasterizer.anInt7069 && i_2 >= TextureOpRasterizer.anInt7070 && i_3 <= TextureOpRasterizer.anInt7068) {
                if (i_5 == 1) {
                    StaticMethods.method14572(i_0, i_1, i_2, i_3, i_4)
                } else {
                    method4561(i_0, i_1, i_2, i_3, i_4, i_5)
                }
            } else if (i_5 == 1) {
                method1388(i_0, i_1, i_2, i_3, i_4)
            } else {
                ArrayUtils.method744(i_0, i_1, i_2, i_3, i_4, i_5)
            }
        }
    }
}
