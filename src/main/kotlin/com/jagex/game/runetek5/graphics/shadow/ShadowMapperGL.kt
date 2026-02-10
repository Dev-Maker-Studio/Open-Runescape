package com.jagex.game.runetek5.graphics.shadow

import com.jagex.game.runetek5.scene.ground.GroundGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer
import jaggl.OpenGL

class ShadowMapperGL(
    val toolkit: OpenGlToolkit,
    val ground: GroundGL
) {

    val width: Int
    val length: Int
    val shadowMapperData: ByteArray
    val sizeShift: Int
    val sizeX: Int
    val sizeY: Int
    var shadowTiles: Array<Array<ShadowTileGL?>>? = null

    init {
        width = (ground.width * ground.tileUnits shr toolkit.shadowScale) + 2
        length = (ground.length * ground.tileUnits shr toolkit.shadowScale) + 2
        shadowMapperData = ByteArray(width * length)
        sizeShift = toolkit.shadowScale + 7 - ground.tileScale
        sizeX = ground.width shr sizeShift
        sizeY = ground.length shr sizeShift
    }

    fun renderShadowOverlay(i_1: Int, i_2: Int, i_3: Int, bools_4: Array<BooleanArray>, bool5: Boolean) {
        toolkit.setLightingEnabled(false)
        toolkit.setDepthMask(false)
        toolkit.renderTexture(-2)
        toolkit.loadTextureCombineMode(1)
        toolkit.setBlendMode(1)
        val f_6 = 1.0f / (toolkit.openglFlags * 128)
        var i_7: Int
        var i_8: Int
        var i_9: Int
        var i_10: Int
        var i_11: Int
        var i_12: Int
        var i_13: Int
        if (bool5) {
            i_7 = 0
            while (i_7 < sizeY) {
                i_8 = i_7 shl sizeShift
                i_9 = (i_7 + 1) shl sizeShift

                label136@ for (i_10 in 0 until sizeX) {
                    i_11 = i_10 shl sizeShift
                    i_12 = (i_10 + 1) shl sizeShift

                    for (i_13 in i_11 until i_12) {
                        if (i_13 - i_1 >= -i_3 && i_13 - i_1 <= i_3) {
                            for (i_20 in i_8 until i_9) {
                                if (i_20 - i_2 >= -i_3 && i_20 - i_2 <= i_3 && bools_4[i_3 + (i_13 - i_1)][i_20 - i_2 + i_3]) {
                                    OpenGL.glMatrixMode(5890)
                                    OpenGL.glLoadIdentity()
                                    OpenGL.glScalef(f_6, f_6, 1.0f)
                                    OpenGL.glTranslatef((-i_10) / f_6, (-i_7) / f_6, 1.0f)
                                    OpenGL.glMatrixMode(5888)
                                    shadowTiles!![i_10][i_7]!!.drawShadow()
                                    continue@label136
                                }
                            }
                        }
                    }
                }
                i_7++
            }
        } else {
            i_7 = 0
            while (i_7 < sizeY) {
                i_8 = i_7 shl sizeShift
                i_9 = (i_7 + 1) shl sizeShift

                for (i_10 in 0 until sizeX) {
                    i_11 = 0
                    i_12 = i_10 shl sizeShift
                    i_13 = (i_10 + 1) shl sizeShift
                    val class282Sub35Sub114 = toolkit.textureBuffer
                    class282Sub35Sub114.currentPosition = 0

                    for (i_15 in i_8 until i_9) {
                        if (i_15 - i_2 >= -i_3 && i_15 - i_2 <= i_3) {
                            var i_16 = i_12 + ground.width * i_15

                            for (i_17 in i_12 until i_13) {
                                if (i_17 - i_1 >= -i_3 && i_17 - i_1 <= i_3 && bools_4[i_3 + (i_17 - i_1)][i_15 - i_2 + i_3]) {
                                    val shorts_18 = ground.materialIndices[i_16]
                                    if (shorts_18 != null) {
                                        var i_19: Int
                                        if (toolkit.bigEndianess) {
                                            i_19 = 0
                                            while (i_19 < shorts_18.size) {
                                                class282Sub35Sub114.writeShort(shorts_18[i_19].toInt() and 0xffff)
                                                ++i_11
                                                i_19++
                                            }
                                        } else {
                                            i_19 = 0
                                            while (i_19 < shorts_18.size) {
                                                class282Sub35Sub114.writeShortLittle(shorts_18[i_19].toInt() and 0xffff)
                                                ++i_11
                                                i_19++
                                            }
                                        }
                                    }
                                }

                                ++i_16
                            }
                        }
                    }

                    if (i_11 > 0) {
                        OpenGL.glMatrixMode(5890)
                        OpenGL.glLoadIdentity()
                        OpenGL.glScalef(f_6, f_6, 1.0f)
                        OpenGL.glTranslatef((-i_10) / f_6, (-i_7) / f_6, 1.0f)
                        OpenGL.glMatrixMode(5888)
                        shadowTiles!![i_10][i_7]!!.drawShadowWithIndices(class282Sub35Sub114.buffer, i_11)
                    }
                }
                i_7++
            }
        }

        OpenGL.glMatrixMode(5890) //GL_TEXTURE
        OpenGL.glLoadIdentity()
        OpenGL.glMatrixMode(5888) //GL_MODELVIEW
    }

    fun applyShadow(class282Sub50Sub171: Shadow, i_2: Int, i_3: Int): Boolean {
        var i_31 = i_3
        var i_21 = i_2
        val class282Sub50Sub17Sub14 = class282Sub50Sub171 as ShadowGL
        i_21 += class282Sub50Sub17Sub14.startX + 1
        i_31 += class282Sub50Sub17Sub14.startY + 1
        var i_5 = i_21 + i_31 * width
        var i_6 = class282Sub50Sub17Sub14.sizeY
        var i_7 = class282Sub50Sub17Sub14.sizeX
        var i_8 = width - i_7
        var i_9: Int
        if (i_31 <= 0) {
            i_9 = 1 - i_31
            i_6 -= i_9
            i_5 += i_9 * width
            i_31 = 1
        }

        if (i_31 + i_6 >= length) {
            i_9 = i_31 + i_6 + 1 - length
            i_6 -= i_9
        }

        if (i_21 <= 0) {
            i_9 = 1 - i_21
            i_7 -= i_9
            i_5 += i_9
            i_8 += i_9
            i_21 = 1
        }

        if (i_7 + i_21 >= width) {
            i_9 = i_7 + i_21 + 1 - width
            i_7 -= i_9
            i_8 += i_9
        }

        if (i_7 > 0 && i_6 > 0) {
            val b_10: Byte = 8
            i_8 += (b_10 - 1) * width
            return hasShadowData(shadowMapperData, i_5, i_7, i_6, i_8, b_10.toInt())
        } else {
            return false
        }
    }

    fun invalidateShadowTiles(i_1: Int, i_2: Int, i_3: Int, i_4: Int) {
        if (shadowTiles != null) {
            val i_5 = (i_1 - 1) shr 7
            val i_6 = (i_3 + (i_1 - 1) - 1) shr 7
            val i_7 = (i_2 - 1) shr 7
            val i_8 = (i_2 - 1 + i_4 - 1) shr 7

            for (i_9 in i_5..i_6) {
                val arr_10 = shadowTiles!![i_9]

                for (i_11 in i_7..i_8) {
                    arr_10[i_11]!!.needsTextureRegeneration = true
                }
            }
        }
    }

    fun initializeShadowTiles() {
        shadowTiles = Array(sizeX) { arrayOfNulls(sizeY) }

        for (y in 0 until sizeY) {
            for (x in 0 until sizeX) {
                shadowTiles!![x][y] = ShadowTileGL(toolkit, this, ground, x, y, sizeShift, x * 128 + 1, y * 128 + 1)
            }
        }
    }

    fun addShadow(class282Sub50Sub171: Shadow, i_2: Int, i_3: Int) {
        var i_31 = i_3
        var i_21 = i_2
        val shadow = class282Sub50Sub171 as ShadowGL
        i_21 += shadow.startX + 1
        i_31 += shadow.startY + 1
        var i_5 = i_21 + i_31 * width
        var i_6 = 0
        var i_7 = shadow.sizeY
        var i_8 = shadow.sizeX
        var i_9 = width - i_8
        var i_10 = 0
        var i_11: Int
        if (i_31 <= 0) {
            i_11 = 1 - i_31
            i_7 -= i_11
            i_6 += i_11 * i_8
            i_5 += i_11 * width
            i_31 = 1
        }

        if (i_31 + i_7 >= length) {
            i_11 = i_31 + i_7 + 1 - length
            i_7 -= i_11
        }

        if (i_21 <= 0) {
            i_11 = 1 - i_21
            i_8 -= i_11
            i_6 += i_11
            i_5 += i_11
            i_10 += i_11
            i_9 += i_11
            i_21 = 1
        }

        if (i_8 + i_21 >= width) {
            i_11 = i_8 + i_21 + 1 - width
            i_8 -= i_11
            i_10 += i_11
            i_9 += i_11
        }

        if (i_8 > 0 && i_7 > 0) {
            addShadowData(shadowMapperData, shadow.shade, i_6, i_5, i_8, i_7, i_9, i_10)
            invalidateShadowTiles(i_21, i_31, i_8, i_7)
        }
    }

    fun removeShadow(class282Sub50Sub171: Shadow, i_2: Int, i_3: Int) {
        var i_31 = i_3
        var i_21 = i_2
        val class282Sub50Sub17Sub14 = class282Sub50Sub171 as ShadowGL
        i_21 += class282Sub50Sub17Sub14.startX + 1
        i_31 += class282Sub50Sub17Sub14.startY + 1
        var i_5 = i_21 + i_31 * width
        var i_6 = 0
        var i_7 = class282Sub50Sub17Sub14.sizeY
        var i_8 = class282Sub50Sub17Sub14.sizeX
        var i_9 = width - i_8
        var i_10 = 0
        var i_11: Int
        if (i_31 <= 0) {
            i_11 = 1 - i_31
            i_7 -= i_11
            i_6 += i_11 * i_8
            i_5 += i_11 * width
            i_31 = 1
        }

        if (i_31 + i_7 >= length) {
            i_11 = i_31 + i_7 + 1 - length
            i_7 -= i_11
        }

        if (i_21 <= 0) {
            i_11 = 1 - i_21
            i_8 -= i_11
            i_6 += i_11
            i_5 += i_11
            i_10 += i_11
            i_9 += i_11
            i_21 = 1
        }

        if (i_8 + i_21 >= width) {
            i_11 = i_8 + i_21 + 1 - width
            i_8 -= i_11
            i_10 += i_11
            i_9 += i_11
        }

        if (i_8 > 0 && i_7 > 0) {
            subtractShadowData(shadowMapperData, class282Sub50Sub17Sub14.shade, i_6, i_5, i_8, i_7, i_9, i_10)
            invalidateShadowTiles(i_21, i_31, i_8, i_7)
        }
    }

    companion object {
        @JvmStatic
        fun subtractShadowData(bytes_0: ByteArray, bytes_1: ByteArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
            var dstPos = i_3
            var srcPos = i_2
            val i_8 = -(i_4 shr 2)
            val negWidth = -(i_4 and 0x3)

            for (i_9 in -i_5 until 0) {
                var i_10 = i_8
                while (i_10 < 0) {
                    val i_10001 = dstPos++
                    bytes_0[i_10001] = (bytes_0[i_10001] + bytes_1[srcPos++]).toByte()
                    val i_10002 = dstPos++
                    bytes_0[i_10002] = (bytes_0[i_10002] + bytes_1[srcPos++]).toByte()
                    val i_10003 = dstPos++
                    bytes_0[i_10003] = (bytes_0[i_10003] + bytes_1[srcPos++]).toByte()
                    val i_10004 = dstPos++
                    bytes_0[i_10004] = (bytes_0[i_10004] + bytes_1[srcPos++]).toByte()
                    i_10++
                }

                i_10 = negWidth
                while (i_10 < 0) {
                    val i_10001 = dstPos++
                    bytes_0[i_10001] = (bytes_0[i_10001] + bytes_1[srcPos++]).toByte()
                    i_10++
                }

                dstPos += i_6
                srcPos += i_7
            }
        }

        @JvmStatic
        fun addShadowData(dst: ByteArray, shade: ByteArray, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
            var dstPos = i_3
            var srcPos = i_2
            val i_8 = -(i_4 shr 2)
            val negWidth = -(i_4 and 0x3)

            for (i_9 in -i_5 until 0) {
                var i_10 = i_8
                while (i_10 < 0) {
                    val i_10001 = dstPos++
                    dst[i_10001] = (dst[i_10001] - shade[srcPos++]).toByte()
                    val i_10002 = dstPos++
                    dst[i_10002] = (dst[i_10002] - shade[srcPos++]).toByte()
                    val i_10003 = dstPos++
                    dst[i_10003] = (dst[i_10003] - shade[srcPos++]).toByte()
                    val i_10004 = dstPos++
                    dst[i_10004] = (dst[i_10004] - shade[srcPos++]).toByte()
                    i_10++
                }

                i_10 = negWidth
                while (i_10 < 0) {
                    val i_10001 = dstPos++
                    dst[i_10001] = (dst[i_10001] - shade[srcPos++]).toByte()
                    i_10++
                }

                dstPos += i_6
                srcPos += i_7
            }
        }

        @JvmStatic
        fun hasShadowData(bytes_0: ByteArray, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int): Boolean {
            var pos = i_1
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
                    if (bytes_0[pos] == 0.toByte()) {
                        return true
                    }

                    pos += i_5
                }

                pos -= i_7
                if (bytes_0[pos - 1] == 0.toByte()) {
                    return true
                }

                pos += i_4
            }

            return false
        }
    }
}
