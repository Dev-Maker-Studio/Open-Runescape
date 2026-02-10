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
    val toolkit: OpenGlToolkit,
    val ground: GroundGL,
    val pointLight: PointLight,
    ints_4: IntArray
) : Node<LightGL>() {
    var vertexCount: Int = 0
    val minTileX: Int
    val maxTileX: Int
    val minTileZ: Int
    val maxTileZ: Int
    var tileNormalsX: Array<FloatArray>?
    var tileNormalsY: Array<FloatArray>?
    var tileNormalsZ: Array<FloatArray>?
    val indexBuffer: ElementsBufferGL?
    val vertexBuffer: ArrayBufferGL?
    val positionDrawBuffer: OpenGlArrayDrawBuffer?
    val colorDrawBuffer: OpenGlArrayDrawBuffer?
    var indexPacket: Packet? = null
    var bitBuffer: GlBitBuffer? = null
    var vertexHashTable: HashTable? = null
    var totalIndexCount: Int = 0

    init {
        val i_5 = this.pointLight.getSize2d() - (ground.tileUnits shr 1)
        this.minTileX = this.pointLight.getX() - i_5 shr ground.tileScale
        this.maxTileX = this.pointLight.getX() + i_5 shr ground.tileScale
        this.minTileZ = this.pointLight.getZ() - i_5 shr ground.tileScale
        this.maxTileZ = this.pointLight.getZ() + i_5 shr ground.tileScale
        val i_6 = this.maxTileX - this.minTileX + 1
        val i_7 = this.maxTileZ - this.minTileZ + 1
        this.tileNormalsX = Array(i_6 + 1) { FloatArray(i_7 + 1) }
        this.tileNormalsY = Array(i_6 + 1) { FloatArray(i_7 + 1) }
        this.tileNormalsZ = Array(i_6 + 1) { FloatArray(i_7 + 1) }

        var i_8: Int
        var i_9: Int
        var i_10: Int
        var i_11: Int
        var i_12: Int
        var i_13: Int
        i_8 = 0
        while (i_8 <= i_7) {
            i_9 = i_8 + this.minTileZ
            if (i_9 > 0 && i_9 < this.ground.length - 1) {
                i_10 = 0
                while (i_10 <= i_6) {
                    i_11 = i_10 + this.minTileX
                    if (i_11 > 0 && i_11 < this.ground.width - 1) {
                        i_12 = ground.getHeight(i_11 + 1, i_9) - ground.getHeight(i_11 - 1, i_9)
                        i_13 = ground.getHeight(i_11, i_9 + 1) - ground.getHeight(i_11, i_9 - 1)
                        val f_14 = (1.0 / sqrt((i_12 * i_12 + i_13 * i_13 + 65536).toDouble())).toFloat()
                        this.tileNormalsX!![i_10][i_8] = i_12 * f_14
                        this.tileNormalsY!![i_10][i_8] = -256.0f * f_14
                        this.tileNormalsZ!![i_10][i_8] = i_13 * f_14
                    }
                    i_10++
                }
            }
            i_8++
        }

        i_8 = 0

        i_9 = this.minTileZ
        while (i_9 <= this.maxTileZ) {
            if (i_9 >= 0 && i_9 < ground.length) {
                i_10 = this.minTileX
                while (i_10 <= this.maxTileX) {
                    if (i_10 >= 0 && i_10 < ground.width) {
                        i_11 = ints_4[i_8]
                        val ints_19 = ground.tileColors!![i_10][i_9]
                        if (ints_19 != null && i_11 != 0) {
                            if (i_11 == 1) {
                                i_13 = 0

                                while (i_13 < ints_19.size) {
                                    if (ints_19[i_13++] != -1 && ints_19[i_13++] != -1 && ints_19[i_13++] != -1) {
                                        this.totalIndexCount += 3
                                    }
                                }
                            } else {
                                this.totalIndexCount += 3
                            }
                        }
                    }

                    ++i_8
                    i_10++
                }
            } else {
                i_8 += this.maxTileX - this.minTileX
            }
            i_9++
        }

        if (this.totalIndexCount > 0) {
            this.indexPacket = Packet(this.totalIndexCount * 2)
            this.bitBuffer = GlBitBuffer(this.totalIndexCount * 16)
            this.vertexHashTable = HashTable(MathUtils.nextPowerOfTwo(this.totalIndexCount))
            i_9 = 0
            i_8 = 0

            i_10 = this.minTileZ
            while (i_10 <= this.maxTileZ) {
                if (i_10 >= 0 && i_10 < ground.length) {
                    i_11 = 0

                    i_12 = this.minTileX
                    while (i_12 <= this.maxTileX) {
                        if (i_12 >= 0 && i_12 < ground.width) {
                            i_13 = ints_4[i_8]
                            val ints_18 = ground.tileColors!![i_12][i_10]
                            if (ints_18 != null && i_13 != 0) {
                                if (i_13 != 1) {
                                    if (i_13 == 3) {
                                        addVertex(i_11, i_9, i_12, i_10, 0, 0)
                                        addVertex(i_11, i_9, i_12, i_10, ground.tileUnits, 0)
                                        addVertex(i_11, i_9, i_12, i_10, 0, ground.tileUnits)
                                    } else if (i_13 == 2) {
                                        addVertex(i_11, i_9, i_12, i_10, ground.tileUnits, 0)
                                        addVertex(i_11, i_9, i_12, i_10, ground.tileUnits, ground.tileUnits)
                                        addVertex(i_11, i_9, i_12, i_10, 0, 0)
                                    } else if (i_13 == 5) {
                                        addVertex(i_11, i_9, i_12, i_10, ground.tileUnits, ground.tileUnits)
                                        addVertex(i_11, i_9, i_12, i_10, 0, ground.tileUnits)
                                        addVertex(i_11, i_9, i_12, i_10, ground.tileUnits, 0)
                                    } else if (i_13 == 4) {
                                        addVertex(i_11, i_9, i_12, i_10, 0, ground.tileUnits)
                                        addVertex(i_11, i_9, i_12, i_10, 0, 0)
                                        addVertex(i_11, i_9, i_12, i_10, ground.tileUnits, ground.tileUnits)
                                    }
                                } else {
                                    val ints_15 = ground.tileOffsetsX!![i_12][i_10]
                                    val ints_16 = ground.tileOffsetsZ!![i_12][i_10]
                                    var i_17 = 0

                                    label108@ while (true) {
                                        while (true) {
                                            if (i_17 >= ints_18.size) {
                                                break@label108
                                            }

                                            if (ints_18[i_17] != -1 && ints_18[i_17 + 1] != -1 && ints_18[i_17 + 2] != -1) {
                                                addVertex(i_11, i_9, i_12, i_10, ints_15!![i_17], ints_16!![i_17])
                                                ++i_17
                                                addVertex(i_11, i_9, i_12, i_10, ints_15!![i_17], ints_16!![i_17])
                                                ++i_17
                                                addVertex(i_11, i_9, i_12, i_10, ints_15!![i_17], ints_16!![i_17])
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
                    i_8 += this.maxTileX - this.minTileX
                }

                ++i_9
                i_10++
            }

            this.indexBuffer = this.toolkit.createElementsArrayBuffer(this.indexPacket!!.buffer, this.indexPacket!!.currentPosition, false)
            this.vertexBuffer = this.toolkit.createArrayBuffer(16, this.bitBuffer!!.buffer, this.bitBuffer!!.currentPosition, false)
            this.positionDrawBuffer = OpenGlArrayDrawBuffer(this.vertexBuffer, 5126, 3, 0)
            this.colorDrawBuffer = OpenGlArrayDrawBuffer(this.vertexBuffer, 5121, 4, 12)
        } else {
            this.indexBuffer = null
            this.vertexBuffer = null
            this.positionDrawBuffer = null
            this.colorDrawBuffer = null
        }

        this.bitBuffer = null
        this.indexPacket = null
        this.vertexHashTable = null
        this.tileNormalsZ = null
        this.tileNormalsY = null
        this.tileNormalsX = null
    }

    fun writeIndex(s_1: Short) {
        if (this.toolkit.bigEndianess) {
            this.indexPacket!!.writeShort(s_1.toInt())
        } else {
            this.indexPacket!!.writeShortLittle(s_1.toInt())
        }
    }

    fun addVertex(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        var long_7 = -1L
        val i_9 = i_5 + (i_3 shl this.ground.tileScale)
        val i_10 = (i_4 shl this.ground.tileScale) + i_6
        val i_11 = this.ground.getAverageHeight(i_9, i_10)
        if ((i_5 and 0x7f) == 0 || (i_6 and 0x7f) == 0) {
            long_7 = (i_10.toLong() and 0xffffL) shl 16 or (i_9.toLong() and 0xffffL)
            val node29 = this.vertexHashTable!!.get(long_7)
            if (node29 != null) {
                writeIndex((node29 as ShortNode).value)
                return
            }
        }

        val s_12 = (this.vertexCount++).toShort()
        if (long_7 != -1L) {
            this.vertexHashTable!!.put(ShortNode(s_12), long_7)
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
                f_13 = this.tileNormalsX!![i_1][i_2]
                f_14 = this.tileNormalsY!![i_1][i_2]
                f_15 = this.tileNormalsZ!![i_1][i_2]
            }
            i_5 == this.ground.tileUnits && i_6 == 0 -> {
                f_13 = this.tileNormalsX!![i_1 + 1][i_2]
                f_14 = this.tileNormalsY!![i_1 + 1][i_2]
                f_15 = this.tileNormalsZ!![i_1 + 1][i_2]
            }
            i_5 == this.ground.tileUnits && this.ground.tileUnits == i_6 -> {
                f_13 = this.tileNormalsX!![i_1 + 1][i_2 + 1]
                f_14 = this.tileNormalsY!![i_1 + 1][i_2 + 1]
                f_15 = this.tileNormalsZ!![i_1 + 1][i_2 + 1]
            }
            i_5 == 0 && this.ground.tileUnits == i_6 -> {
                f_13 = this.tileNormalsX!![i_1][i_2 + 1]
                f_14 = this.tileNormalsY!![i_1][i_2 + 1]
                f_15 = this.tileNormalsZ!![i_1][i_2 + 1]
            }
            else -> {
                f_16 = i_5.toFloat() / this.ground.tileUnits
                f_17 = i_6.toFloat() / this.ground.tileUnits
                var f_18_var = this.tileNormalsX!![i_1][i_2]
                var f_19_var = this.tileNormalsY!![i_1][i_2]
                var f_20_var = this.tileNormalsZ!![i_1][i_2]
                var f_21_var = this.tileNormalsX!![i_1 + 1][i_2]
                var f_22_var = this.tileNormalsY!![i_1 + 1][i_2]
                var f_23_var = this.tileNormalsZ!![i_1 + 1][i_2]
                f_18_var += (this.tileNormalsX!![i_1][i_2 + 1] - f_18_var) * f_16
                f_19_var += (this.tileNormalsY!![i_1][i_2 + 1] - f_19_var) * f_16
                f_20_var += (this.tileNormalsZ!![i_1][i_2 + 1] - f_20_var) * f_16
                f_21_var += (this.tileNormalsX!![i_1 + 1][i_2 + 1] - f_21_var) * f_16
                f_22_var += (this.tileNormalsY!![i_1 + 1][i_2 + 1] - f_22_var) * f_16
                f_23_var += (this.tileNormalsZ!![i_1 + 1][i_2 + 1] - f_23_var) * f_16
                f_13 = f_18_var + (f_21_var - f_18_var) * f_17
                f_14 = f_19_var + (f_22_var - f_19_var) * f_17
                f_15 = f_20_var + (f_23_var - f_20_var) * f_17
            }
        }

        var deltaX = (this.pointLight.getX() - i_9).toFloat()
        var deltaY = (this.pointLight.getY() - i_11).toFloat()
        var deltaZ = (this.pointLight.getZ() - i_10).toFloat()
        val distance = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()
        val invDistance = 1.0F / distance
        deltaX *= invDistance
        deltaY *= invDistance
        deltaZ *= invDistance
        val attenuation = distance / this.pointLight.getSize2d()
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

        val i_25 = this.pointLight.getColor()
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

        if (this.toolkit.bigEndianess) {
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
        writeIndex(s_12)
    }

    fun drawLightMesh(i_1: Int, i_2: Int, i_3: Int, bools_4: Array<BooleanArray>) {
        if (this.indexBuffer != null && this.minTileX <= i_3 + i_1 && this.maxTileX >= i_1 - i_3 && this.minTileZ <= i_3 + i_2 && this.maxTileZ >= i_2 - i_3) {
            for (i_5 in this.minTileZ..this.maxTileZ) {
                for (i_6 in this.minTileX..this.maxTileX) {
                    val i_7 = i_6 - i_1
                    val i_8 = i_5 - i_2
                    if (i_7 > -i_3 && i_7 < i_3 && i_8 > -i_3 && i_8 < i_3 && bools_4[i_3 + i_7][i_3 + i_8]) {
                        this.toolkit.setTextureEnvColor((this.pointLight.getAlpha() * 255.0F).toInt() shl 24)
                        this.toolkit.toggleDrawingClientstates(this.positionDrawBuffer, null, this.colorDrawBuffer, null)
                        this.toolkit.drawElements(this.indexBuffer!!, 0, this.totalIndexCount)
                        return
                    }
                }
            }
        }
    }
}
