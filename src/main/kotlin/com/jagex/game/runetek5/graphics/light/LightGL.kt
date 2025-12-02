package com.jagex.game.runetek5.graphics.light

import com.jagex.game.runetek5.buffer.ArrayBufferGL
import com.jagex.game.runetek5.buffer.ElementsBufferGL
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.HashTable
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ShortNode
import com.jagex.game.runetek5.scene.ground.GroundGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer
import com.jagex.game.runetek5.utils.MathUtils
import kotlin.math.sqrt

class LightGL(
    val aGraphicalRendererSub17650: OpenGlToolkit,
    val aGroundSub27654: GroundGL,
    val aNodeSub247641: PointLight,
    ints_4: IntArray
) : Node<LightGL>() {
    var anInt7646: Int = 0
    val anInt7642: Int
    val anInt7653: Int
    val anInt7644: Int
    val anInt7656: Int
    var aFloatArrayArray7652: Array<FloatArray>?
    var aFloatArrayArray7648: Array<FloatArray>?
    var aFloatArrayArray7645: Array<FloatArray>?
    val anInterface157639: ElementsBufferGL?
    val anInterface147649: ArrayBufferGL?
    val aClass1437647: OpenGlArrayDrawBuffer?
    val aClass1437643: OpenGlArrayDrawBuffer?
    var aNodeSub357655: Packet? = null
    var bitBuffer: GlBitBuffer? = null
    var aClass4537657: HashTable? = null
    var anInt7640: Int = 0

    init {
        val i_5 = this.aNodeSub247641.getSize2d() - (aGroundSub27654.tileUnits shr 1)
        this.anInt7642 = this.aNodeSub247641.getX() - i_5 shr aGroundSub27654.tileScale
        this.anInt7653 = this.aNodeSub247641.getX() + i_5 shr aGroundSub27654.tileScale
        this.anInt7644 = this.aNodeSub247641.getZ() - i_5 shr aGroundSub27654.tileScale
        this.anInt7656 = this.aNodeSub247641.getZ() + i_5 shr aGroundSub27654.tileScale
        val i_6 = this.anInt7653 - this.anInt7642 + 1
        val i_7 = this.anInt7656 - this.anInt7644 + 1
        this.aFloatArrayArray7652 = Array(i_6 + 1) { FloatArray(i_7 + 1) }
        this.aFloatArrayArray7648 = Array(i_6 + 1) { FloatArray(i_7 + 1) }
        this.aFloatArrayArray7645 = Array(i_6 + 1) { FloatArray(i_7 + 1) }

        var i_8: Int
        var i_9: Int
        var i_10: Int
        var i_11: Int
        var i_12: Int
        var i_13: Int
        i_8 = 0
        while (i_8 <= i_7) {
            i_9 = i_8 + this.anInt7644
            if (i_9 > 0 && i_9 < this.aGroundSub27654.length - 1) {
                i_10 = 0
                while (i_10 <= i_6) {
                    i_11 = i_10 + this.anInt7642
                    if (i_11 > 0 && i_11 < this.aGroundSub27654.width - 1) {
                        i_12 = aGroundSub27654.getHeight(i_11 + 1, i_9) - aGroundSub27654.getHeight(i_11 - 1, i_9)
                        i_13 = aGroundSub27654.getHeight(i_11, i_9 + 1) - aGroundSub27654.getHeight(i_11, i_9 - 1)
                        val f_14 = (1.0 / sqrt((i_12 * i_12 + i_13 * i_13 + 65536).toDouble())).toFloat()
                        this.aFloatArrayArray7652!![i_10][i_8] = i_12 * f_14
                        this.aFloatArrayArray7648!![i_10][i_8] = -256.0f * f_14
                        this.aFloatArrayArray7645!![i_10][i_8] = i_13 * f_14
                    }
                    i_10++
                }
            }
            i_8++
        }

        i_8 = 0

        i_9 = this.anInt7644
        while (i_9 <= this.anInt7656) {
            if (i_9 >= 0 && i_9 < aGroundSub27654.length) {
                i_10 = this.anInt7642
                while (i_10 <= this.anInt7653) {
                    if (i_10 >= 0 && i_10 < aGroundSub27654.width) {
                        i_11 = ints_4[i_8]
                        val ints_19 = aGroundSub27654.tileColors[i_10][i_9]
                        if (ints_19 != null && i_11 != 0) {
                            if (i_11 == 1) {
                                i_13 = 0

                                while (i_13 < ints_19.size) {
                                    if (ints_19[i_13++] != -1 && ints_19[i_13++] != -1 && ints_19[i_13++] != -1) {
                                        this.anInt7640 += 3
                                    }
                                }
                            } else {
                                this.anInt7640 += 3
                            }
                        }
                    }

                    ++i_8
                    i_10++
                }
            } else {
                i_8 += this.anInt7653 - this.anInt7642
            }
            i_9++
        }

        if (this.anInt7640 > 0) {
            this.aNodeSub357655 = Packet(this.anInt7640 * 2)
            this.bitBuffer = GlBitBuffer(this.anInt7640 * 16)
            this.aClass4537657 = HashTable(MathUtils.nextPowerOfTwo(this.anInt7640))
            i_9 = 0
            i_8 = 0

            i_10 = this.anInt7644
            while (i_10 <= this.anInt7656) {
                if (i_10 >= 0 && i_10 < aGroundSub27654.length) {
                    i_11 = 0

                    i_12 = this.anInt7642
                    while (i_12 <= this.anInt7653) {
                        if (i_12 >= 0 && i_12 < aGroundSub27654.width) {
                            i_13 = ints_4[i_8]
                            val ints_18 = aGroundSub27654.tileColors[i_12][i_10]
                            if (ints_18 != null && i_13 != 0) {
                                if (i_13 != 1) {
                                    if (i_13 == 3) {
                                        method12306(i_11, i_9, i_12, i_10, 0, 0)
                                        method12306(i_11, i_9, i_12, i_10, aGroundSub27654.tileUnits, 0)
                                        method12306(i_11, i_9, i_12, i_10, 0, aGroundSub27654.tileUnits)
                                    } else if (i_13 == 2) {
                                        method12306(i_11, i_9, i_12, i_10, aGroundSub27654.tileUnits, 0)
                                        method12306(i_11, i_9, i_12, i_10, aGroundSub27654.tileUnits, aGroundSub27654.tileUnits)
                                        method12306(i_11, i_9, i_12, i_10, 0, 0)
                                    } else if (i_13 == 5) {
                                        method12306(i_11, i_9, i_12, i_10, aGroundSub27654.tileUnits, aGroundSub27654.tileUnits)
                                        method12306(i_11, i_9, i_12, i_10, 0, aGroundSub27654.tileUnits)
                                        method12306(i_11, i_9, i_12, i_10, aGroundSub27654.tileUnits, 0)
                                    } else if (i_13 == 4) {
                                        method12306(i_11, i_9, i_12, i_10, 0, aGroundSub27654.tileUnits)
                                        method12306(i_11, i_9, i_12, i_10, 0, 0)
                                        method12306(i_11, i_9, i_12, i_10, aGroundSub27654.tileUnits, aGroundSub27654.tileUnits)
                                    }
                                } else {
                                    val ints_15 = aGroundSub27654.tileOffsetsX[i_12][i_10]
                                    val ints_16 = aGroundSub27654.tileOffsetsZ[i_12][i_10]
                                    var i_17 = 0

                                    label108@ while (true) {
                                        while (true) {
                                            if (i_17 >= ints_18.size) {
                                                break@label108
                                            }

                                            if (ints_18[i_17] != -1 && ints_18[i_17 + 1] != -1 && ints_18[i_17 + 2] != -1) {
                                                method12306(i_11, i_9, i_12, i_10, ints_15[i_17], ints_16[i_17])
                                                ++i_17
                                                method12306(i_11, i_9, i_12, i_10, ints_15[i_17], ints_16[i_17])
                                                ++i_17
                                                method12306(i_11, i_9, i_12, i_10, ints_15[i_17], ints_16[i_17])
                                                ++i_17
                                            } else {
                                                i_17 += 3
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ++i_8
                        ++i_11
                        i_12++
                    }
                } else {
                    i_8 += this.anInt7653 - this.anInt7642
                }

                ++i_9
                i_10++
            }

            this.anInterface157639 = this.aGraphicalRendererSub17650.createElementsArrayBuffer(this.aNodeSub357655!!.buffer, this.aNodeSub357655!!.currentPosition, false)
            this.anInterface147649 = this.aGraphicalRendererSub17650.createArrayBuffer(16, this.bitBuffer!!.buffer, this.bitBuffer!!.currentPosition, false)
            this.aClass1437647 = OpenGlArrayDrawBuffer(this.anInterface147649, 5126, 3, 0)
            this.aClass1437643 = OpenGlArrayDrawBuffer(this.anInterface147649, 5121, 4, 12)
        } else {
            this.anInterface157639 = null
            this.anInterface147649 = null
            this.aClass1437647 = null
            this.aClass1437643 = null
        }

        this.bitBuffer = null
        this.aNodeSub357655 = null
        this.aClass4537657 = null
        this.aFloatArrayArray7645 = null
        this.aFloatArrayArray7648 = null
        this.aFloatArrayArray7652 = null
    }

    fun method12305(s_1: Short) {
        if (this.aGraphicalRendererSub17650.bigEndianess) {
            this.aNodeSub357655!!.writeShort(s_1.toInt())
        } else {
            this.aNodeSub357655!!.writeShortLittle(s_1.toInt())
        }
    }

    fun method12306(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        var long_7 = -1L
        val i_9 = i_5 + (i_3 shl this.aGroundSub27654.tileScale)
        val i_10 = (i_4 shl this.aGroundSub27654.tileScale) + i_6
        val i_11 = this.aGroundSub27654.getAverageHeight(i_9, i_10)
        if ((i_5 and 0x7f) == 0 || (i_6 and 0x7f) == 0) {
            long_7 = (i_10.toLong() and 0xffffL) shl 16 or (i_9.toLong() and 0xffffL)
            val node29 = this.aClass4537657!!.get(long_7)
            if (node29 != null) {
                method12305((node29 as ShortNode).aShort8067)
                return
            }
        }

        val s_12 = (this.anInt7646++).toShort()
        if (long_7 != -1L) {
            this.aClass4537657!!.put(ShortNode(s_12), long_7)
        }

        val f_13: Float
        val f_14: Float
        val f_15: Float
        val f_16: Float
        val f_17: Float
        val f_18: Float
        val f_19: Float
        val f_20: Float
        val f_21: Float
        val f_22: Float
        val f_23: Float
        when {
            i_5 == 0 && i_6 == 0 -> {
                f_13 = this.aFloatArrayArray7652!![i_1][i_2]
                f_14 = this.aFloatArrayArray7648!![i_1][i_2]
                f_15 = this.aFloatArrayArray7645!![i_1][i_2]
            }
            i_5 == this.aGroundSub27654.tileUnits && i_6 == 0 -> {
                f_13 = this.aFloatArrayArray7652!![i_1 + 1][i_2]
                f_14 = this.aFloatArrayArray7648!![i_1 + 1][i_2]
                f_15 = this.aFloatArrayArray7645!![i_1 + 1][i_2]
            }
            i_5 == this.aGroundSub27654.tileUnits && this.aGroundSub27654.tileUnits == i_6 -> {
                f_13 = this.aFloatArrayArray7652!![i_1 + 1][i_2 + 1]
                f_14 = this.aFloatArrayArray7648!![i_1 + 1][i_2 + 1]
                f_15 = this.aFloatArrayArray7645!![i_1 + 1][i_2 + 1]
            }
            i_5 == 0 && this.aGroundSub27654.tileUnits == i_6 -> {
                f_13 = this.aFloatArrayArray7652!![i_1][i_2 + 1]
                f_14 = this.aFloatArrayArray7648!![i_1][i_2 + 1]
                f_15 = this.aFloatArrayArray7645!![i_1][i_2 + 1]
            }
            else -> {
                f_16 = i_5.toFloat() / this.aGroundSub27654.tileUnits
                f_17 = i_6.toFloat() / this.aGroundSub27654.tileUnits
                var f_18_var = this.aFloatArrayArray7652!![i_1][i_2]
                var f_19_var = this.aFloatArrayArray7648!![i_1][i_2]
                var f_20_var = this.aFloatArrayArray7645!![i_1][i_2]
                var f_21_var = this.aFloatArrayArray7652!![i_1 + 1][i_2]
                var f_22_var = this.aFloatArrayArray7648!![i_1 + 1][i_2]
                var f_23_var = this.aFloatArrayArray7645!![i_1 + 1][i_2]
                f_18_var += (this.aFloatArrayArray7652!![i_1][i_2 + 1] - f_18_var) * f_16
                f_19_var += (this.aFloatArrayArray7648!![i_1][i_2 + 1] - f_19_var) * f_16
                f_20_var += (this.aFloatArrayArray7645!![i_1][i_2 + 1] - f_20_var) * f_16
                f_21_var += (this.aFloatArrayArray7652!![i_1 + 1][i_2 + 1] - f_21_var) * f_16
                f_22_var += (this.aFloatArrayArray7648!![i_1 + 1][i_2 + 1] - f_22_var) * f_16
                f_23_var += (this.aFloatArrayArray7645!![i_1 + 1][i_2 + 1] - f_23_var) * f_16
                f_13 = f_18_var + (f_21_var - f_18_var) * f_17
                f_14 = f_19_var + (f_22_var - f_19_var) * f_17
                f_15 = f_20_var + (f_23_var - f_20_var) * f_17
            }
        }

        var deltaX = (this.aNodeSub247641.getX() - i_9).toFloat()
        var deltaY = (this.aNodeSub247641.getY() - i_11).toFloat()
        var deltaZ = (this.aNodeSub247641.getZ() - i_10).toFloat()
        val distance = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()
        val invDistance = 1.0F / distance
        deltaX *= invDistance
        deltaY *= invDistance
        deltaZ *= invDistance
        val attenuation = distance / this.aNodeSub247641.getSize2d()
        var intensity = 1.0F - attenuation * attenuation
        if (intensity < 0.0F) {
            intensity = 0.0F
        }

        var dotProduct = deltaX * f_13 + deltaY * f_14 + deltaZ * f_15
        if (dotProduct < 0.0F) {
            dotProduct = 0.0F
        }

        var lightFactor = dotProduct * intensity * 2.0F
        if (lightFactor > 1.0F) {
            lightFactor = 1.0F
        }

        val i_25 = this.aNodeSub247641.getColor()
        var i_26 = (lightFactor * (i_25 shr 16 and 0xff)).toInt()
        if (i_26 > 255) {
            i_26 = 255
        }

        var i_27 = (lightFactor * (i_25 shr 8 and 0xff)).toInt()
        if (i_27 > 255) {
            i_27 = 255
        }

        var i_28 = (lightFactor * (i_25 and 0xff)).toInt()
        if (i_28 > 255) {
            i_28 = 255
        }

        if (this.aGraphicalRendererSub17650.bigEndianess) {
            this.bitBuffer!!.writeFloatBigEndian(i_9.toFloat())
            this.bitBuffer!!.writeFloatBigEndian(i_11.toFloat())
            this.bitBuffer!!.writeFloatBigEndian(i_10.toFloat())
        } else {
            this.bitBuffer!!.writeFloatLittleEndian(i_9.toFloat())
            this.bitBuffer!!.writeFloatLittleEndian(i_11.toFloat())
            this.bitBuffer!!.writeFloatLittleEndian(i_10.toFloat())
        }

        this.bitBuffer!!.writeByte(i_26)
        this.bitBuffer!!.writeByte(i_27)
        this.bitBuffer!!.writeByte(i_28)
        this.bitBuffer!!.writeByte(255)
        method12305(s_12)
    }

    fun method12311(i_1: Int, i_2: Int, i_3: Int, bools_4: Array<BooleanArray>) {
        if (this.anInterface157639 != null && this.anInt7642 <= i_3 + i_1 && this.anInt7653 >= i_1 - i_3 && this.anInt7644 <= i_3 + i_2 && this.anInt7656 >= i_2 - i_3) {
            for (i_5 in this.anInt7644..this.anInt7656) {
                for (i_6 in this.anInt7642..this.anInt7653) {
                    val i_7 = i_6 - i_1
                    val i_8 = i_5 - i_2
                    if (i_7 > -i_3 && i_7 < i_3 && i_8 > -i_3 && i_8 < i_3 && bools_4[i_3 + i_7][i_3 + i_8]) {
                        this.aGraphicalRendererSub17650.setTextureEnvColor((this.aNodeSub247641.getAlpha() * 255.0F).toInt() shl 24)
                        this.aGraphicalRendererSub17650.toggleDrawingClientstates(this.aClass1437647, null, this.aClass1437643, null)
                        this.aGraphicalRendererSub17650.drawElements(this.anInterface157639!!, 0, this.anInt7640)
                        return
                    }
                }
            }
        }
    }
}
