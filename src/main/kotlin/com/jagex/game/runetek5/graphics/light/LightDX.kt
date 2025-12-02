package com.jagex.game.runetek5.graphics.light

import com.jagex.game.runetek5.cache.node.HashTable
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ShortNode
import com.jagex.game.runetek5.scene.ground.GroundDX
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.NativeStream
import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer
import com.jagex.game.runetek5.utils.MathUtils
import java.nio.ByteBuffer
import kotlin.math.sqrt

class LightDX(
    class505Sub21: AbstractNativeToolkit,
    class390Sub12: GroundDX,
    light: PointLight,
    ints_4: IntArray
) : Node<LightDX>() {

    @JvmField
    var anInt7530: Int = 0

    @JvmField
    val toolkit: AbstractNativeToolkit = class505Sub21

    @JvmField
    val lightSource: PointLight = light

    @JvmField
    val ground: GroundDX = class390Sub12

    @JvmField
    val minX: Int

    @JvmField
    val maxX: Int

    @JvmField
    val minZ: Int

    @JvmField
    val maxZ: Int

    @JvmField
    var heightsXZ: Array<FloatArray>?

    @JvmField
    var normalsXZ: Array<FloatArray>?

    @JvmField
    var shadowRatioXZ: Array<FloatArray>?

    @JvmField
    var anInterface47525: NativeStream? = null

    @JvmField
    var anInterface327523: DirectXBuffer? = null

    @JvmField
    var aClass4537537: HashTable?

    @JvmField
    var numTriangles: Int = 0

    init {
        val size = lightSource.getSize2d() - (class390Sub12.tileUnits shr 1)
        minX = lightSource.getX() - size shr class390Sub12.tileScale
        maxX = lightSource.getX() + size shr class390Sub12.tileScale
        minZ = lightSource.getZ() - size shr class390Sub12.tileScale
        maxZ = lightSource.getZ() + size shr class390Sub12.tileScale
        val sizeX = maxX - minX + 1
        val sizeZ = maxZ - minZ + 1
        heightsXZ = Array(sizeX + 1) { FloatArray(sizeZ + 1) }
        normalsXZ = Array(sizeX + 1) { FloatArray(sizeZ + 1) }
        shadowRatioXZ = Array(sizeX + 1) { FloatArray(sizeZ + 1) }

        for (i_8 in 0..sizeZ) {
            val i_9 = i_8 + minZ
            if (i_9 > 0 && i_9 < ground.length - 1) {
                for (i_21 in 0..sizeX) {
                    val i_11 = i_21 + minX
                    if (i_11 > 0 && i_11 < ground.width - 1) {
                        val i_12 = class390Sub12.getHeight(i_11 + 1, i_9) - class390Sub12.getHeight(i_11 - 1, i_9)
                        val i_13 = class390Sub12.getHeight(i_11, i_9 + 1) - class390Sub12.getHeight(i_11, i_9 - 1)
                        val f_14 = (1.0 / sqrt((i_12 * i_12 + i_13 * i_13 + 65536).toDouble())).toFloat()
                        heightsXZ!![i_21][i_8] = i_12 * f_14
                        normalsXZ!![i_21][i_8] = -256.0f * f_14
                        shadowRatioXZ!![i_21][i_8] = i_13 * f_14
                    }
                }
            }
        }

        var i_8 = 0

        for (z in minZ..maxZ) {
            if (z >= 0 && z < class390Sub12.length) {
                for (x in minX..maxX) {
                    if (x >= 0 && x < class390Sub12.width) {
                        val i_11 = ints_4[i_8]
                        val ints_22 = class390Sub12.anIntArrayArrayArray8538[x][z]
                        if (ints_22 != null && i_11 != 0) {
                            if (i_11 == 1) {
                                var i_13 = 0
                                while (i_13 < ints_22.size) {
                                    if (ints_22[i_13++] != -1 && ints_22[i_13++] != -1 && ints_22[i_13++] != -1) {
                                        numTriangles += 3
                                    }
                                }
                            } else {
                                numTriangles += 3
                            }
                        }
                    }
                    ++i_8
                }
            } else {
                i_8 += maxX - minX
            }
        }

        if (numTriangles > 0) {
            aClass4537537 = HashTable(MathUtils.nextPowerOfTwo(numTriangles))
            val bytebuffer20 = toolkit.directBuffer
            bytebuffer20.clear()
            bytebuffer20.position(2097152)
            val bytebuffer10 = bytebuffer20.slice()
            bytebuffer20.position(0)
            bytebuffer20.limit(2097152)
            var i_11 = 0
            i_8 = 0

            for (z in minZ..maxZ) {
                if (z >= 0 && z < class390Sub12.length) {
                    var i_13 = 0

                    for (i_23 in minX..maxX) {
                        if (i_23 >= 0 && i_23 < class390Sub12.width) {
                            val i_15 = ints_4[i_8]
                            val ints_16 = class390Sub12.anIntArrayArrayArray8538[i_23][z]
                            if (ints_16 != null && i_15 != 0) {
                                if (i_15 != 1) {
                                    if (i_15 == 3) {
                                        method12175(i_13, i_11, i_23, z, 0, 0, bytebuffer10, bytebuffer20)
                                        method12175(i_13, i_11, i_23, z, class390Sub12.tileUnits, 0, bytebuffer10, bytebuffer20)
                                        method12175(i_13, i_11, i_23, z, 0, class390Sub12.tileUnits, bytebuffer10, bytebuffer20)
                                    } else if (i_15 == 2) {
                                        method12175(i_13, i_11, i_23, z, class390Sub12.tileUnits, 0, bytebuffer10, bytebuffer20)
                                        method12175(i_13, i_11, i_23, z, class390Sub12.tileUnits, class390Sub12.tileUnits, bytebuffer10, bytebuffer20)
                                        method12175(i_13, i_11, i_23, z, 0, 0, bytebuffer10, bytebuffer20)
                                    } else if (i_15 == 5) {
                                        method12175(i_13, i_11, i_23, z, class390Sub12.tileUnits, class390Sub12.tileUnits, bytebuffer10, bytebuffer20)
                                        method12175(i_13, i_11, i_23, z, 0, class390Sub12.tileUnits, bytebuffer10, bytebuffer20)
                                        method12175(i_13, i_11, i_23, z, class390Sub12.tileUnits, 0, bytebuffer10, bytebuffer20)
                                    } else if (i_15 == 4) {
                                        method12175(i_13, i_11, i_23, z, 0, class390Sub12.tileUnits, bytebuffer10, bytebuffer20)
                                        method12175(i_13, i_11, i_23, z, 0, 0, bytebuffer10, bytebuffer20)
                                        method12175(i_13, i_11, i_23, z, class390Sub12.tileUnits, class390Sub12.tileUnits, bytebuffer10, bytebuffer20)
                                    }
                                } else {
                                    val ints_17 = class390Sub12.anIntArrayArrayArray8540[i_23][z]
                                    val ints_18 = class390Sub12.anIntArrayArrayArray8533[i_23][z]
                                    var i_19 = 0

                                    while (i_19 < ints_16.size) {
                                        if (ints_16[i_19] != -1 && ints_16[i_19 + 1] != -1 && ints_16[i_19 + 2] != -1) {
                                            method12175(i_13, i_11, i_23, z, ints_17[i_19], ints_18[i_19], bytebuffer10, bytebuffer20)
                                            ++i_19
                                            method12175(i_13, i_11, i_23, z, ints_17[i_19], ints_18[i_19], bytebuffer10, bytebuffer20)
                                            ++i_19
                                            method12175(i_13, i_11, i_23, z, ints_17[i_19], ints_18[i_19], bytebuffer10, bytebuffer20)
                                            ++i_19
                                        } else {
                                            i_19 += 3
                                        }
                                    }
                                }
                            }
                        }

                        ++i_8
                        ++i_13
                    }
                } else {
                    i_8 += maxX - minX
                }

                ++i_11
            }

            anInterface327523 = toolkit.method13993(false)
            anInterface327523!!.create(numTriangles)
            anInterface47525 = toolkit.createStream(false)
            anInterface47525!!.create(anInt7530 * 16, 16)
            anInterface47525!!.upload(0, bytebuffer20.position(), toolkit.directBufferAddress)
            anInterface327523!!.upload(0, bytebuffer10.position(), toolkit.directBufferAddress + 2097152L)
        } else {
            anInterface47525 = null
            anInterface327523 = null
        }

        aClass4537537 = null
        shadowRatioXZ = null
        normalsXZ = null
        heightsXZ = null
    }

    fun method12175(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, bytebuffer7: ByteBuffer, bytebuffer8: ByteBuffer) {
        var hash = -1L
        val x = i_5 + (i_3 shl ground.tileScale)
        val y = (i_4 shl ground.tileScale) + i_6
        val i_13 = ground.getAverageHeight(x, y)
        if ((i_5 and 0x7f) == 0 || (i_6 and 0x7f) == 0) {
            hash = (y.toLong() and 0xffffL) shl 16 or (x.toLong() and 0xffffL)
            val node31 = aClass4537537!!.get(hash)
            if (node31 != null) {
                bytebuffer7.putShort((node31 as ShortNode).aShort8067)
                return
            }
        }

        val s_14 = (anInt7530++).toShort()
        if (hash != -1L) {
            aClass4537537!!.put(ShortNode(s_14), hash)
        }

        var f_15: Float
        var f_16: Float
        var f_17: Float
        var f_18: Float
        var f_19: Float
        var f_20: Float
        var f_21: Float
        var f_22: Float
        var f_23: Float
        var f_24: Float
        var f_25: Float

        if (i_5 == 0 && i_6 == 0) {
            f_15 = heightsXZ!![i_1][i_2]
            f_16 = normalsXZ!![i_1][i_2]
            f_17 = shadowRatioXZ!![i_1][i_2]
        } else if (i_5 == ground.tileUnits && i_6 == 0) {
            f_15 = heightsXZ!![i_1 + 1][i_2]
            f_16 = normalsXZ!![i_1 + 1][i_2]
            f_17 = shadowRatioXZ!![i_1 + 1][i_2]
        } else if (i_5 == ground.tileUnits && ground.tileUnits == i_6) {
            f_15 = heightsXZ!![i_1 + 1][i_2 + 1]
            f_16 = normalsXZ!![i_1 + 1][i_2 + 1]
            f_17 = shadowRatioXZ!![i_1 + 1][i_2 + 1]
        } else if (i_5 == 0 && ground.tileUnits == i_6) {
            f_15 = heightsXZ!![i_1][i_2 + 1]
            f_16 = normalsXZ!![i_1][i_2 + 1]
            f_17 = shadowRatioXZ!![i_1][i_2 + 1]
        } else {
            f_18 = i_5.toFloat() / ground.tileUnits
            f_19 = i_6.toFloat() / ground.tileUnits
            f_20 = heightsXZ!![i_1][i_2]
            f_21 = normalsXZ!![i_1][i_2]
            f_22 = shadowRatioXZ!![i_1][i_2]
            f_23 = heightsXZ!![i_1 + 1][i_2]
            f_24 = normalsXZ!![i_1 + 1][i_2]
            f_25 = shadowRatioXZ!![i_1 + 1][i_2]
            f_20 += (heightsXZ!![i_1][i_2 + 1] - f_20) * f_18
            f_21 += (normalsXZ!![i_1][i_2 + 1] - f_21) * f_18
            f_22 += (shadowRatioXZ!![i_1][i_2 + 1] - f_22) * f_18
            f_23 += (heightsXZ!![i_1 + 1][i_2 + 1] - f_23) * f_18
            f_24 += (normalsXZ!![i_1 + 1][i_2 + 1] - f_24) * f_18
            f_25 += (shadowRatioXZ!![i_1 + 1][i_2 + 1] - f_25) * f_18
            f_15 = f_20 + (f_23 - f_20) * f_19
            f_16 = f_21 + (f_24 - f_21) * f_19
            f_17 = f_22 + (f_25 - f_22) * f_19
        }

        f_18 = (lightSource.getX() - x).toFloat()
        f_19 = (lightSource.getY() - i_13).toFloat()
        f_20 = (lightSource.getZ() - y).toFloat()
        f_21 = sqrt(f_18 * f_18 + f_19 * f_19 + f_20 * f_20)
        f_22 = 1.0f / f_21
        f_18 *= f_22
        f_19 *= f_22
        f_20 *= f_22
        f_23 = f_21 / lightSource.getSize2d()
        f_24 = 1.0f - f_23 * f_23
        if (f_24 < 0.0f) {
            f_24 = 0.0f
        }

        f_25 = f_18 * f_15 + f_19 * f_16 + f_20 * f_17
        if (f_25 < 0.0f) {
            f_25 = 0.0f
        }

        var f_26 = f_25 * f_24 * 2.0f
        if (f_26 > 1.0f) {
            f_26 = 1.0f
        }

        val i_27 = lightSource.getColor()
        var i_28 = (f_26 * (i_27 shr 16 and 0xff)).toInt()
        if (i_28 > 255) {
            i_28 = 255
        }

        var i_29 = (f_26 * (i_27 shr 8 and 0xff)).toInt()
        if (i_29 > 255) {
            i_29 = 255
        }

        var i_30 = (f_26 * (i_27 and 0xff)).toInt()
        if (i_30 > 255) {
            i_30 = 255
        }

        bytebuffer8.putFloat(x.toFloat())
        bytebuffer8.putFloat(i_13.toFloat())
        bytebuffer8.putFloat(y.toFloat())
        if (toolkit.endiannessType == 0) {
            bytebuffer8.put(i_30.toByte())
            bytebuffer8.put(i_29.toByte())
            bytebuffer8.put(i_28.toByte())
        } else {
            bytebuffer8.put(i_28.toByte())
            bytebuffer8.put(i_29.toByte())
            bytebuffer8.put(i_30.toByte())
        }

        bytebuffer8.put((-1).toByte())
        bytebuffer7.putShort(s_14)
    }
}
