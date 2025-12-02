package com.jagex.game.runetek5.graphics.shadow

import com.jagex.game.runetek5.graphics.shader.program.AbstractEnvironmentShader
import com.jagex.game.runetek5.scene.ground.GroundDX
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.directx.DirectXBuffer
import java.nio.ByteBuffer

class ShadowMapperDX(
    val toolkit: AbstractNativeToolkit,
    val groundDx: GroundDX
) {

    val width: Int
    val length: Int
    val shadowMapperData: ByteArray
    val sizeShift: Int
    val sizeX: Int
    val sizeY: Int
    internal var tiles: Array<Array<ShadowTileDX?>>? = null

    init {
        width = (groundDx.width * groundDx.tileUnits shr toolkit.shadowSizeShift) + 2
        length = (groundDx.length * groundDx.tileUnits shr toolkit.shadowSizeShift) + 2
        shadowMapperData = ByteArray(width * length)
        sizeShift = toolkit.shadowSizeShift + 7 - groundDx.tileScale
        sizeX = groundDx.width shr sizeShift
        sizeY = groundDx.length shr sizeShift
    }

    fun method1312() {
        tiles = Array(sizeX) { arrayOfNulls(sizeY) }

        for (z in 0 until sizeY) {
            for (x in 0 until sizeX) {
                tiles!![x][z] = ShadowTileDX(toolkit, this, groundDx, x, z, sizeShift, x * 128 + 1, z * 128 + 1)
                if (tiles!![x][z]!!.anInt716 == 0) {
                    tiles!![x][z] = null
                }
            }
        }
    }

    fun method1314(shader: AbstractEnvironmentShader, camX: Int, camZ: Int, maxDistance: Int, visibleTiles: Array<BooleanArray>, fullRender: Boolean) {
        val invTileUnits = 1.0f / (toolkit.activeFlags * 128)

        if (fullRender) {
            for (z in 0 until sizeY) {
                val startZ = z shl sizeShift
                val endZ = (z + 1) shl sizeShift

                label147@ for (x in 0 until sizeX) {
                    if (tiles!![x][z] != null) {
                        val startX = x shl sizeShift
                        val endX = (x + 1) shl sizeShift

                        for (currX in startX until endX) {
                            if (currX - camX >= -maxDistance && currX - camX <= maxDistance) {
                                for (currZ in startZ until endZ) {
                                    if (currZ - camZ >= -maxDistance && currZ - camZ <= maxDistance && visibleTiles[currX - camX + maxDistance][maxDistance + (currZ - camZ)]) {
                                        shader.textureCoordinateMatrix.method6525(invTileUnits, invTileUnits, 1.0f, 1.0f)
                                        shader.textureCoordinateMatrix.faceList[12] = (-x).toFloat()
                                        shader.textureCoordinateMatrix.faceList[13] = (-z).toFloat()
                                        tiles!![x][z]!!.method1288(shader)
                                        continue@label147
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val ints_24 = IntArray(sizeY * sizeX)
            val bytebuffer9 = toolkit.directBuffer
            bytebuffer9.clear()
            var i_10 = 0

            for (z in 0 until sizeY) {
                val startZ = z shl sizeShift
                val endZ = (z + 1) shl sizeShift

                for (x in 0 until sizeX) {
                    val tile = tiles!![x][z]
                    var i_16 = 0
                    if (tile != null) {
                        val startX = x shl sizeShift
                        val endX = (x + 1) shl sizeShift

                        for (currZ in startZ until endZ) {
                            if (currZ - camZ >= -maxDistance && currZ - camZ <= maxDistance) {
                                var i_20 = startX + groundDx.width * currZ

                                for (currX in startX until endX) {
                                    if (currX - camX >= -maxDistance && currX - camX <= maxDistance && visibleTiles[currX - camX + maxDistance][maxDistance + (currZ - camZ)]) {
                                        val shorts_22 = groundDx.tileVertices[i_20]
                                        if (shorts_22 != null) {
                                            for (i_23 in shorts_22.indices) {
                                                bytebuffer9.putShort(shorts_22[i_23])
                                                ++i_16
                                            }
                                        }
                                    }

                                    ++i_20
                                }
                            }
                        }
                    }

                    ints_24[i_10] = i_16
                    ++i_10
                }
            }

            if (bytebuffer9.position() != 0) {
                val avar12 = bytebuffer9.position()
                val interface3212 = toolkit.method13911(avar12 / 2)
                interface3212.upload(0, avar12, toolkit.directBufferAddress)
                var i_13 = 0
                i_10 = 0

                for (z in 0 until sizeY) {
                    for (x in 0 until sizeX) {
                        if (ints_24[i_10] != 0) {
                            shader.textureCoordinateMatrix.method6525(invTileUnits, invTileUnits, 1.0f, 1.0f)
                            shader.textureCoordinateMatrix.faceList[12] = (-x).toFloat()
                            shader.textureCoordinateMatrix.faceList[13] = (-z).toFloat()
                            tiles!![x][z]!!.method1290(shader, interface3212, i_13, ints_24[i_10] / 3)
                            i_13 += ints_24[i_10]
                        }

                        ++i_10
                    }
                }
            }
        }
    }

    fun remove(shadow: Shadow, x: Int, z: Int) {
        var mutableZ = z
        var mutableX = x
        val shadowDx = shadow as ShadowDX
        mutableX += shadowDx.minX + 1
        mutableZ += shadowDx.minZ + 1
        var i_5 = mutableX + mutableZ * width
        var i_6 = 0
        var shadowSizeZ = shadowDx.sizeZ
        var shadowSizeX = shadowDx.sizeX
        var i_9 = width - shadowSizeX
        var i_10 = 0
        var i_11: Int
        if (mutableZ <= 0) {
            i_11 = 1 - mutableZ
            shadowSizeZ -= i_11
            i_6 += i_11 * shadowSizeX
            i_5 += i_11 * width
            mutableZ = 1
        }

        if (mutableZ + shadowSizeZ >= length) {
            i_11 = mutableZ + shadowSizeZ + 1 - length
            shadowSizeZ -= i_11
        }

        if (mutableX <= 0) {
            i_11 = 1 - mutableX
            shadowSizeX -= i_11
            i_6 += i_11
            i_5 += i_11
            i_10 += i_11
            i_9 += i_11
            mutableX = 1
        }

        if (shadowSizeX + mutableX >= width) {
            i_11 = shadowSizeX + mutableX + 1 - width
            shadowSizeX -= i_11
            i_10 += i_11
            i_9 += i_11
        }

        if (shadowSizeX > 0 && shadowSizeZ > 0) {
            method1330(shadowMapperData, shadowDx.shadowRaster, i_6, i_5, shadowSizeX, shadowSizeZ, i_9, i_10)
            requestZoneUpdate(mutableX, mutableZ, shadowSizeX, shadowSizeZ)
        }
    }

    fun hasShadow(shadow: Shadow, x: Int, z: Int): Boolean {
        var mutableZ = z
        var mutableX = x
        val shadowDx = shadow as ShadowDX
        mutableX += shadowDx.minX + 1
        mutableZ += shadowDx.minZ + 1
        var i_5 = mutableX + mutableZ * width
        var i_6 = shadowDx.sizeZ
        var i_7 = shadowDx.sizeX
        var i_8 = width - i_7
        var i_9: Int
        if (mutableZ <= 0) {
            i_9 = 1 - mutableZ
            i_6 -= i_9
            i_5 += i_9 * width
            mutableZ = 1
        }

        if (mutableZ + i_6 >= length) {
            i_9 = mutableZ + i_6 + 1 - length
            i_6 -= i_9
        }

        if (mutableX <= 0) {
            i_9 = 1 - mutableX
            i_7 -= i_9
            i_5 += i_9
            i_8 += i_9
            mutableX = 1
        }

        if (i_7 + mutableX >= width) {
            i_9 = i_7 + mutableX + 1 - width
            i_7 -= i_9
            i_8 += i_9
        }

        if (i_7 > 0 && i_6 > 0) {
            val b_10: Byte = 8
            i_8 += (b_10 - 1) * width
            return method1328(shadowMapperData, i_5, i_7, i_6, i_8, b_10.toInt())
        }
        return false
    }

    fun add(shadow: Shadow, x: Int, y: Int) {
        var mutableY = y
        var mutableX = x
        val shadowDx = shadow as ShadowDX
        mutableX += shadowDx.minX + 1
        mutableY += shadowDx.minZ + 1
        var i_5 = mutableX + mutableY * width
        var i_6 = 0
        var shadowSizeZ = shadowDx.sizeZ
        var shadowSizeX = shadowDx.sizeX
        var i_9 = width - shadowSizeX
        var i_10 = 0
        var i_11: Int
        if (mutableY <= 0) {
            i_11 = 1 - mutableY
            shadowSizeZ -= i_11
            i_6 += i_11 * shadowSizeX
            i_5 += i_11 * width
            mutableY = 1
        }

        if (mutableY + shadowSizeZ >= length) {
            i_11 = mutableY + shadowSizeZ + 1 - length
            shadowSizeZ -= i_11
        }

        if (mutableX <= 0) {
            i_11 = 1 - mutableX
            shadowSizeX -= i_11
            i_6 += i_11
            i_5 += i_11
            i_10 += i_11
            i_9 += i_11
            mutableX = 1
        }

        if (shadowSizeX + mutableX >= width) {
            i_11 = shadowSizeX + mutableX + 1 - width
            shadowSizeX -= i_11
            i_10 += i_11
            i_9 += i_11
        }

        if (shadowSizeX > 0 && shadowSizeZ > 0) {
            method1318(shadowMapperData, shadowDx.shadowRaster, i_6, i_5, shadowSizeX, shadowSizeZ, i_9, i_10)
            requestZoneUpdate(mutableX, mutableY, shadowSizeX, shadowSizeZ)
        }
    }

    fun requestZoneUpdate(startAbsX: Int, startAbsZ: Int, sizeX: Int, sizeZ: Int) {
        if (tiles != null) {
            val startX = (startAbsX - 1) shr 7
            val endX = (sizeX + (startAbsX - 1) - 1) shr 7
            val startZ = (startAbsZ - 1) shr 7
            val endZ = (startAbsZ - 1 + sizeZ - 1) shr 7

            for (x in startX..endX) {
                for (z in startZ..endZ) {
                    if (tiles!![x][z] != null) {
                        tiles!![x][z]!!.needsUpdate = true
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun method1318(shadowMapperData: ByteArray, bytes_1: ByteArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
            var dstPos = i_3
            var srcPos = i_2
            val i_8 = -(i_4 shr 2)
            val negWidth = -(i_4 and 0x3)

            for (i_9 in -i_5 until 0) {
                var i_10 = i_8
                while (i_10 < 0) {
                    shadowMapperData[++dstPos] = (shadowMapperData[dstPos] + bytes_1[srcPos++]).toByte()
                    shadowMapperData[++dstPos] = (shadowMapperData[dstPos] + bytes_1[srcPos++]).toByte()
                    shadowMapperData[++dstPos] = (shadowMapperData[dstPos] + bytes_1[srcPos++]).toByte()
                    shadowMapperData[++dstPos] = (shadowMapperData[dstPos] + bytes_1[srcPos++]).toByte()
                    i_10++
                }

                i_10 = negWidth
                while (i_10 < 0) {
                    shadowMapperData[++dstPos] = (shadowMapperData[dstPos] + bytes_1[srcPos++]).toByte()
                    i_10++
                }

                dstPos += i_6
                srcPos += i_7
            }
        }

        @JvmStatic
        fun method1328(shadowMapperData: ByteArray, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int): Boolean {
            var i_1 = i_1
            val i_6 = i_2 % i_5
            val i_7 = if (i_6 != 0) {
                i_5 - i_6
            } else {
                0
            }

            val i_8 = -((i_3 + i_5 - 1) / i_5)
            val i_9 = -((i_5 + i_2 - 1) / i_5)

            for (i_10 in i_8 until 0) {
                for (i_11 in i_9 until 0) {
                    if (shadowMapperData[i_1] == 0.toByte()) {
                        return true
                    }

                    i_1 += i_5
                }

                i_1 -= i_7
                if (shadowMapperData[i_1 - 1] == 0.toByte()) {
                    return true
                }

                i_1 += i_4
            }

            return false
        }

        @JvmStatic
        fun method1330(shadowMapperData: ByteArray, bytes_1: ByteArray, src: Int, dst: Int, i_4: Int, i_5: Int, dstOffsetStep: Int, srcOffsetStep: Int) {
            var dstPos = dst
            var srcPos = src
            val i_8 = -(i_4 shr 2)
            val negWidth = -(i_4 and 0x3)

            for (i_9 in -i_5 until 0) {
                var i_10 = i_8
                while (i_10 < 0) {
                    shadowMapperData[++dstPos] = (shadowMapperData[dstPos] - bytes_1[srcPos++]).toByte()
                    shadowMapperData[++dstPos] = (shadowMapperData[dstPos] - bytes_1[srcPos++]).toByte()
                    shadowMapperData[++dstPos] = (shadowMapperData[dstPos] - bytes_1[srcPos++]).toByte()
                    shadowMapperData[++dstPos] = (shadowMapperData[dstPos] - bytes_1[srcPos++]).toByte()
                    i_10++
                }

                i_10 = negWidth
                while (i_10 < 0) {
                    shadowMapperData[++dstPos] = (shadowMapperData[dstPos] - bytes_1[srcPos++]).toByte()
                    i_10++
                }

                dstPos += dstOffsetStep
                srcPos += srcOffsetStep
            }
        }
    }
}
