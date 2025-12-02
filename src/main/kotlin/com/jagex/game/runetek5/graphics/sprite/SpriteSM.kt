package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.unidentified.ComponentDimensionSM

abstract class SpriteSM(
    @JvmField val toolkit: SafeModeToolkit,
    @JvmField val anInt8851: Int,
    @JvmField val anInt8867: Int
) : NativeSprite() {

    @JvmField
    var anInt8853: Int = 0
    @JvmField
    var anInt8854: Int = 0
    @JvmField
    var anInt8855: Int = 0
    @JvmField
    var anInt8856: Int = 0
    @JvmField
    var cachedClipping: IntArray? = null

    override fun setSize(minX: Int, minY: Int, i_3: Int, i_4: Int) {
        anInt8853 = minX
        anInt8854 = minY
        anInt8855 = i_3
        anInt8856 = i_4
    }

    override fun getWidth(): Int {
        return anInt8851
    }

    override fun getHeight(): Int {
        return anInt8867
    }

    abstract fun method14245(var1: Int, var2: Int)

    override fun getSpriteHeight(): Int {
        return anInt8854 + anInt8867 + anInt8856
    }

    override abstract fun draw(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int)

    override abstract fun method2773(var1: Int, var2: Int, var3: ComponentSurface, var4: Int, var5: Int)

    abstract fun method14247(
        colorWrite: Boolean,
        depthWrite: Boolean,
        transparent: Boolean,
        var4: Int,
        var5: Int,
        var6: Float,
        var7: Int,
        var8: Int,
        blendType: Int,
        color: Int,
        type: Int,
        var13: Boolean
    )

    override fun method2772(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        if (cachedClipping == null) {
            cachedClipping = IntArray(4)
        }
        toolkit.getClipping(cachedClipping)
        toolkit.expandClipping(toolkit.clipLeft, toolkit.clipTop, width + x, y + height)
        val i_8 = getSpriteWidth()
        val i_9 = getSpriteHeight()
        val i_10 = (width + i_8 - 1) / i_8
        val i_11 = (i_9 + height - 1) / i_9

        for (i_12 in 0 until i_11) {
            val i_13 = i_12 * i_9

            for (i_14 in 0 until i_10) {
                draw(i_14 * i_8 + x, i_13 + y, combineMode, colorArgb, blendMode)
            }
        }

        toolkit.setClipping(cachedClipping!![0], cachedClipping!![1], cachedClipping!![2], cachedClipping!![3])
    }

    fun method14248(f_1: Float, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float): Boolean {
        val i_7 = anInt8853 + anInt8851 + anInt8855
        val i_8 = anInt8854 + anInt8867 + anInt8856
        var f_9: Float
        var f_10: Float
        var f_11: Float
        var f_12: Float
        var f_13: Float
        var f_14: Float
        var f_15: Float
        var f_16: Float
        var f_1Var = f_1
        var f_2Var = f_2
        var f_3Var = f_3
        var f_4Var = f_4
        var f_5Var = f_5
        var f_6Var = f_6
        if (i_7 != anInt8851 || i_8 != anInt8867) {
            f_9 = (f_3Var - f_1Var) / i_7
            f_10 = (f_4Var - f_2Var) / i_7
            f_11 = (f_5Var - f_1Var) / i_8
            f_12 = (f_6Var - f_2Var) / i_8
            f_13 = f_11 * anInt8854
            f_14 = f_12 * anInt8854
            f_15 = f_9 * anInt8853
            f_16 = f_10 * anInt8853
            val f_17 = -f_9 * anInt8855
            val f_18 = -f_10 * anInt8855
            val f_19 = -f_11 * anInt8856
            val f_20 = -f_12 * anInt8856
            f_1Var += f_15 + f_13
            f_2Var += f_16 + f_14
            f_3Var += f_17 + f_13
            f_4Var += f_18 + f_14
            f_5Var += f_15 + f_19
            f_6Var += f_16 + f_20
        }

        f_9 = f_5Var + (f_3Var - f_1Var)
        f_10 = f_4Var + (f_6Var - f_2Var)
        if (f_1Var < f_3Var) {
            f_11 = f_1Var
            f_12 = f_3Var
        } else {
            f_11 = f_3Var
            f_12 = f_1Var
        }

        if (f_5Var < f_11) {
            f_11 = f_5Var
        }

        if (f_9 < f_11) {
            f_11 = f_9
        }

        if (f_5Var > f_12) {
            f_12 = f_5Var
        }

        if (f_9 > f_12) {
            f_12 = f_9
        }

        if (f_2Var < f_4Var) {
            f_13 = f_2Var
            f_14 = f_4Var
        } else {
            f_13 = f_4Var
            f_14 = f_2Var
        }

        if (f_6Var < f_13) {
            f_13 = f_6Var
        }

        if (f_10 < f_13) {
            f_13 = f_10
        }

        if (f_6Var > f_14) {
            f_14 = f_6Var
        }

        if (f_10 > f_14) {
            f_14 = f_10
        }

        if (f_11 < toolkit.clipLeft) {
            f_11 = toolkit.clipLeft.toFloat()
        }

        if (f_12 > toolkit.clipRight) {
            f_12 = toolkit.clipRight.toFloat()
        }

        if (f_13 < toolkit.clipTop) {
            f_13 = toolkit.clipTop.toFloat()
        }

        if (f_14 > toolkit.clipBottom) {
            f_14 = toolkit.clipBottom.toFloat()
        }

        f_12 = f_11 - f_12
        if (f_12 >= 0.0f) {
            return false
        } else {
            f_14 = f_13 - f_14
            if (f_14 >= 0.0f) {
                return false
            } else {
                anInt8850 = toolkit.viewportWidth
                anInt8870 = (f_13.toInt() * anInt8850 + f_11).toInt()
                f_15 = (f_3Var - f_1Var) * (f_6Var - f_2Var) - (f_4Var - f_2Var) * (f_5Var - f_1Var)
                f_16 = (f_5Var - f_1Var) * (f_4Var - f_2Var) - (f_6Var - f_2Var) * (f_3Var - f_1Var)
                anInt8865 = ((f_6Var - f_2Var) * 4096.0f * anInt8851 / f_15).toInt()
                anInt8868 = ((f_4Var - f_2Var) * 4096.0f * anInt8867 / f_16).toInt()
                anInt8869 = ((f_5Var - f_1Var) * 4096.0f * anInt8851 / f_16).toInt()
                anInt8876 = ((f_3Var - f_1Var) * 4096.0f * anInt8867 / f_15).toInt()
                anInt8871 = (f_11 * 16.0f + 8.0f - (f_1Var + f_3Var + f_5Var + f_9) / 4.0f * 16.0f).toInt()
                anInt8872 = (f_13 * 16.0f + 8.0f - (f_2Var + f_4Var + f_6Var + f_10) / 4.0f * 16.0f).toInt()
                anInt8873 = (anInt8851 shr 1 shl 12) + (anInt8872 * anInt8869 shr 4)
                anInt8874 = (anInt8867 shr 1 shl 12) + (anInt8872 * anInt8876 shr 4)
                anInt8858 = anInt8871 * anInt8865 shr 4
                anInt8866 = anInt8871 * anInt8868 shr 4
                anInt8861 = f_11.toInt()
                anInt8862 = f_12.toInt()
                anInt8863 = f_13.toInt()
                anInt8864 = f_14.toInt()
                return true
            }
        }
    }

    override fun method2764(f_1: Float, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float, i_7: Int, i_8: Int, i_9: Int) {
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        } else {
            if (method14248(f_1, f_2, f_3, f_4, f_5, f_6)) {
                anInt8877 = i_8
                if (i_7 != 1) {
                    anInt8878 = i_8 ushr 24
                    anInt8879 = 256 - anInt8878
                    if (i_7 == 0) {
                        anInt8880 = (i_8 and 0xff0000) shr 16
                        anInt8852 = (i_8 and 0xff00) shr 8
                        anInt8882 = i_8 and 0xff
                    } else if (i_7 == 2) {
                        anInt8883 = i_8 ushr 24
                        anInt8884 = 256 - anInt8883
                        val i_11 = (i_8 and 0xff00ff) * anInt8884 and -16711936
                        val i_12 = (i_8 and 0xff00) * anInt8884 and 0xff0000
                        anInt8885 = (i_11 or i_12) ushr 8
                    }
                }

                if (i_7 == 1) {
                    if (i_9 == 0) {
                        method14245(1, 0)
                    } else if (i_9 == 1) {
                        method14245(1, 1)
                    } else if (i_9 == 2) {
                        method14245(1, 2)
                    }
                } else if (i_7 == 0) {
                    if (i_9 == 0) {
                        method14245(0, 0)
                    } else if (i_9 == 1) {
                        method14245(0, 1)
                    } else if (i_9 == 2) {
                        method14245(0, 2)
                    }
                } else if (i_7 == 3) {
                    if (i_9 == 0) {
                        method14245(3, 0)
                    } else if (i_9 == 1) {
                        method14245(3, 1)
                    } else if (i_9 == 2) {
                        method14245(3, 2)
                    }
                } else if (i_7 == 2) {
                    if (i_9 == 0) {
                        method14245(2, 0)
                    } else if (i_9 == 1) {
                        method14245(2, 1)
                    } else if (i_9 == 2) {
                        method14245(2, 2)
                    }
                }
            }
        }
    }

    override fun getSpriteWidth(): Int {
        return anInt8853 + anInt8851 + anInt8855
    }

    abstract fun method14250(var1: IntArray, var2: IntArray, var3: Int, var4: Int)

    override fun cacheSizeInto(ints_1: IntArray) {
        ints_1[0] = anInt8853
        ints_1[1] = anInt8854
        ints_1[2] = anInt8855
        ints_1[3] = anInt8856
    }

    override abstract fun drawColoredCircle(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, colorArgb: Int, blendMode: Int)

    override fun method2766(f_1: Float, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float, i_7: Int, class4558: ComponentSurface, i_9: Int, i_10: Int) {
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        if (method14248(f_1, f_2, f_3, f_4, f_5, f_6)) {
            val packetsdecoder11 = class4558 as ComponentDimensionSM
            method14250(packetsdecoder11.anIntArray9077, packetsdecoder11.anIntArray9078, anInt8861 - i_9, -i_10 - (anInt8864 - anInt8863))
        }
    }

    companion object {
        @JvmField
        var anInt8878: Int = 0
        @JvmField
        var anInt8879: Int = 0
        @JvmField
        var anInt8880: Int = 0
        @JvmField
        var anInt8852: Int = 0
        @JvmField
        var anInt8882: Int = 0
        @JvmField
        var anInt8883: Int = 0
        @JvmField
        var anInt8884: Int = 0
        @JvmField
        var anInt8885: Int = 0
        @JvmField
        var anInt8850: Int = 0
        @JvmField
        var anInt8870: Int = 0
        @JvmField
        var anInt8865: Int = 0
        @JvmField
        var anInt8868: Int = 0
        @JvmField
        var anInt8869: Int = 0
        @JvmField
        var anInt8876: Int = 0
        @JvmField
        var anInt8871: Int = 0
        @JvmField
        var anInt8872: Int = 0
        @JvmField
        var anInt8873: Int = 0
        @JvmField
        var anInt8874: Int = 0
        @JvmField
        var anInt8858: Int = 0
        @JvmField
        var anInt8866: Int = 0
        @JvmField
        var anInt8861: Int = 0
        @JvmField
        var anInt8862: Int = 0
        @JvmField
        var anInt8863: Int = 0
        @JvmField
        var anInt8864: Int = 0
        @JvmField
        var anInt8877: Int = 0
    }
}
