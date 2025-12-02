package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.unidentified.ComponentDimensionSM

class OpaqueFontRendererSM(
    val toolkit: SafeModeToolkit,
    fontMetrics: FontMetrics,
    sprites: Array<SpriteType>,
    val anIntArray8511: IntArray,
    val anIntArray8512: IntArray
) : AbstractFontRenderer(toolkit, fontMetrics) {
    val spritePixels: Array<ByteArray?>
    val spriteMinY: IntArray
    val spriteMinX: IntArray

    init {
        this.spritePixels = arrayOfNulls(sprites.size)
        this.spriteMinY = IntArray(sprites.size)
        this.spriteMinX = IntArray(sprites.size)

        for (index in sprites.indices) {
            this.spritePixels[index] = sprites[index].pixels
            this.spriteMinY[index] = sprites[index].minY
            this.spriteMinX[index] = sprites[index].minX
        }
    }

    override fun drawChar(var_1: Int, i_2: Int, i_3: Int, i_4: Int, bool5: Boolean) {
        var y = i_3
        var x = i_2
        if (this.toolkit.colourBuffer != null) {
            x += this.spriteMinX[var_1]
            y += this.spriteMinY[var_1]
            var i_6 = this.anIntArray8511[var_1]
            var i_7 = this.anIntArray8512[var_1]
            val i_8 = this.toolkit.viewportWidth
            var i_9 = y * i_8 + x
            var i_10 = i_8 - i_6
            var i_11 = 0
            var i_12 = 0
            var i_13: Int
            if (y < this.toolkit.clipTop) {
                i_13 = this.toolkit.clipTop - y
                i_7 -= i_13
                y = this.toolkit.clipTop
                i_12 += i_6 * i_13
                i_9 += i_8 * i_13
            }

            if (y + i_7 > this.toolkit.clipBottom) {
                i_7 -= y + i_7 - this.toolkit.clipBottom
            }

            if (x < this.toolkit.clipLeft) {
                i_13 = this.toolkit.clipLeft - x
                i_6 -= i_13
                x = this.toolkit.clipLeft
                i_12 += i_13
                i_9 += i_13
                i_11 += i_13
                i_10 += i_13
            }

            if (i_6 + x > this.toolkit.clipRight) {
                i_13 = i_6 + x - this.toolkit.clipRight
                i_6 -= i_13
                i_11 += i_13
                i_10 += i_13
            }

            if (i_6 > 0 && i_7 > 0) {
                if ((i_4 and 0xffffff.inv()) == 0xffffff.inv()) {
                    method13771(this.spritePixels[var_1]!!, this.toolkit.colourBuffer!!, i_4, i_12, i_9, i_6, i_7, i_10, i_11)
                } else if ((i_4 and 0xffffff.inv()) != 0) {
                    blendImage(this.spritePixels[var_1]!!, this.toolkit.colourBuffer!!, i_4, i_12, i_9, i_6, i_7, i_10, i_11)
                }
            }
        }
    }

    override fun drawChar(var_1: Int, i_2: Int, i_3: Int, i_4: Int, bool5: Boolean, class4556: ComponentSurface?, i_7: Int, i_8: Int) {
        var i_31 = i_3
        var i_21 = i_2
        if (this.toolkit.colourBuffer != null) {
            if (class4556 == null) {
                drawChar(var_1, i_21, i_31, i_4, bool5)
            } else {
                i_21 += this.spriteMinX[var_1]
                i_31 += this.spriteMinY[var_1]
                var i_9 = this.anIntArray8511[var_1]
                var i_10 = this.anIntArray8512[var_1]
                val i_11 = this.toolkit.viewportWidth
                var i_12 = i_31 * i_11 + i_21
                var i_13 = i_11 - i_9
                var i_14 = 0
                var i_15 = 0
                var i_16: Int
                if (i_31 < this.toolkit.clipTop) {
                    i_16 = this.toolkit.clipTop - i_31
                    i_10 -= i_16
                    i_31 = this.toolkit.clipTop
                    i_15 += i_9 * i_16
                    i_12 += i_11 * i_16
                }

                if (i_31 + i_10 > this.toolkit.clipBottom) {
                    i_10 -= i_31 + i_10 - this.toolkit.clipBottom
                }

                if (i_21 < this.toolkit.clipLeft) {
                    i_16 = this.toolkit.clipLeft - i_21
                    i_9 -= i_16
                    i_21 = this.toolkit.clipLeft
                    i_15 += i_16
                    i_12 += i_16
                    i_14 += i_16
                    i_13 += i_16
                }

                if (i_9 + i_21 > this.toolkit.clipRight) {
                    i_16 = i_9 + i_21 - this.toolkit.clipRight
                    i_9 -= i_16
                    i_14 += i_16
                    i_13 += i_16
                }

                if (i_9 > 0 && i_10 > 0) {
                    if ((i_4 and 0xffffff.inv()) == 0xffffff.inv()) {
                        drawImage(this.spritePixels[var_1]!!, this.toolkit.colourBuffer!!, i_4, i_15, i_12, i_9, i_10, i_13, i_14, i_21, i_31, this.anIntArray8511[var_1], class4556, i_7, i_8)
                    } else {
                        method13774(this.spritePixels[var_1]!!, this.toolkit.colourBuffer!!, i_4, i_15, i_12, i_9, i_10, i_13, i_14, i_21, i_31, this.anIntArray8511[var_1], class4556, i_7, i_8)
                    }
                }
            }
        }
    }

    fun method13771(bytes_1: ByteArray, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_9: Int) {
        var dstPos = i_4
        var srcPos = i_5
        val i_10 = -(i_6 shr 2)
        val negWidth = -(i_6 and 0x3)

        for (i_11 in -i_7 until 0) {
            var i_12: Int
            i_12 = i_10
            while (i_12 < 0) {
                if (bytes_1[dstPos++] != 0.toByte()) {
                    ints_2[srcPos++] = i_3
                } else {
                    ++srcPos
                }

                if (bytes_1[dstPos++] != 0.toByte()) {
                    ints_2[srcPos++] = i_3
                } else {
                    ++srcPos
                }

                if (bytes_1[dstPos++] != 0.toByte()) {
                    ints_2[srcPos++] = i_3
                } else {
                    ++srcPos
                }

                if (bytes_1[dstPos++] != 0.toByte()) {
                    ints_2[srcPos++] = i_3
                } else {
                    ++srcPos
                }
                i_12++
            }

            i_12 = negWidth
            while (i_12 < 0) {
                if (bytes_1[dstPos++] != 0.toByte()) {
                    ints_2[srcPos++] = i_3
                } else {
                    ++srcPos
                }
                i_12++
            }

            srcPos += i_8
            dstPos += i_9
        }
    }

    /**
     * Applies an alpha-blended color transformation to a rectangular region
     */
    fun blendImage(palette: ByteArray, pixels: IntArray, color: Int, dstPos: Int, srcPos: Int, width: Int, height: Int, srcScanSize: Int, dstScanSize: Int) {
        var currentDstPos = dstPos
        var currentSrcPos = srcPos
        val alpha = color ushr 24
        val invAlpha = 255 - alpha

        for (y in -height until 0) {
            for (x in -width until 0) {
                if (palette[currentDstPos++] != 0.toByte()) {
                    val blendedPixel = (alpha * (color and 0xff00) and 0xff0000) + ((color and 0xff00ff) * alpha and -16711936) shr 8
                    val dstPixel = pixels[currentSrcPos]
                    pixels[currentSrcPos++] = blendedPixel + ((invAlpha * (dstPixel and 0xff00) and 0xff0000) + ((dstPixel and 0xff00ff) * invAlpha and -16711936) shr 8)
                } else {
                    currentSrcPos++
                }
            }

            currentSrcPos += srcScanSize
            currentDstPos += dstScanSize
        }
    }

    fun drawImage(palette: ByteArray, pixels: IntArray, color: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_9: Int, i_10: Int, i_11: Int, i_12: Int, class45513: ComponentSurface, i_14: Int, i_15: Int) {
        var dstPos = i_4
        var srcPos = i_5
        val packetsdecoder16 = class45513 as ComponentDimensionSM
        val offsetsX = packetsdecoder16.anIntArray9077
        val offsetsY = packetsdecoder16.anIntArray9078
        var i_19 = i_11
        if (i_15 > i_11) {
            i_19 = i_15
            srcPos += this.toolkit.viewportWidth * (i_15 - i_11)
            dstPos += (i_15 - i_11) * i_12
        }

        val i_20 = Math.min(offsetsX.size + i_15, i_11 + i_7)

        for (i_21 in i_19 until i_20) {
            var offX = offsetsX[i_21 - i_15] + i_14
            var offY = offsetsY[i_21 - i_15]
            var i_24 = i_6
            val i_25: Int
            if (i_10 > offX) {
                i_25 = i_10 - offX
                if (i_25 >= offY) {
                    dstPos += i_9 + i_6
                    srcPos += i_8 + i_6
                    continue
                }

                offY -= i_25
            } else {
                i_25 = offX - i_10
                if (i_25 >= i_6) {
                    dstPos += i_9 + i_6
                    srcPos += i_8 + i_6
                    continue
                }

                dstPos += i_25
                i_24 = i_6 - i_25
                srcPos += i_25
            }

            val skipCount: Int
            if (i_24 < offY) {
                offY = i_24
                skipCount = 0
            } else {
                skipCount = i_24 - offY
            }

            for (i_26 in 0 until offY) {
                if (palette[dstPos++] != 0.toByte()) {
                    pixels[srcPos++] = color
                } else {
                    srcPos++
                }
            }

            dstPos = skipCount + dstPos + i_9
            srcPos += skipCount + i_8
        }
    }

    fun method13774(bytes_1: ByteArray, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_9: Int, i_10: Int, i_11: Int, i_12: Int, class45513: ComponentSurface, i_14: Int, i_15: Int) {
        var dstPos = i_4
        var srcPos = i_5
        val packetsdecoder16 = class45513 as ComponentDimensionSM
        val ints_17 = packetsdecoder16.anIntArray9077
        val ints_18 = packetsdecoder16.anIntArray9078
        val i_19 = i_10 - this.toolkit.clipLeft
        var i_20 = i_11
        if (i_15 > i_11) {
            i_20 = i_15
            srcPos += this.toolkit.viewportWidth * (i_15 - i_11)
            dstPos += (i_15 - i_11) * i_12
        }

        val i_21 = Math.min(ints_17.size + i_15, i_11 + i_7)
        val i_22 = i_3 ushr 24
        val i_23 = 255 - i_22

        for (i_24 in i_20 until i_21) {
            var i_25 = ints_17[i_24 - i_15] + i_14
            var i_26 = ints_18[i_24 - i_15]
            var i_27 = i_6
            val i_28: Int
            if (i_19 > i_25) {
                i_28 = i_19 - i_25
                if (i_28 >= i_26) {
                    dstPos += i_9 + i_6
                    srcPos += i_8 + i_6
                    continue
                }

                i_26 -= i_28
            } else {
                i_28 = i_25 - i_19
                if (i_28 >= i_6) {
                    dstPos += i_9 + i_6
                    srcPos += i_8 + i_6
                    continue
                }

                dstPos += i_28
                i_27 = i_6 - i_28
                srcPos += i_28
            }

            val skipCount: Int
            if (i_27 < i_26) {
                i_26 = i_27
                skipCount = 0
            } else {
                skipCount = i_27 - i_26
            }

            for (i_29 in -i_26 until 0) {
                if (bytes_1[dstPos++] != 0.toByte()) {
                    val i_30 = (i_22 * (i_3 and 0xff00) and 0xff0000) + ((i_3 and 0xff00ff) * i_22 and -16711936) shr 8
                    val i_31 = ints_2[srcPos]
                    ints_2[srcPos++] = (((i_31 and 0xff00ff) * i_23 and -16711936) + (i_23 * (i_31 and 0xff00) and 0xff0000) shr 8) + i_30
                } else {
                    ++srcPos
                }
            }

            dstPos = skipCount + dstPos + i_9
            srcPos += skipCount + i_8
        }
    }
}
