package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.toolkit.ComponentDimensionSM

abstract class SpriteSM(
    @JvmField val toolkit: SafeModeToolkit,
    @JvmField val imageWidth: Int,
    @JvmField val imageHeight: Int
) : NativeSprite() {

    @JvmField
    var offsetX: Int = 0
    @JvmField
    var offsetY: Int = 0
    @JvmField
    var trimRight: Int = 0
    @JvmField
    var trimBottom: Int = 0
    @JvmField
    var cachedClipping: IntArray? = null

    override fun setSize(minX: Int, minY: Int, i_3: Int, i_4: Int) {
        offsetX = minX
        offsetY = minY
        trimRight = i_3
        trimBottom = i_4
    }

    override fun getWidth(): Int {
        return imageWidth
    }

    override fun getHeight(): Int {
        return imageHeight
    }

    abstract fun drawAffineTransformed(var1: Int, var2: Int)

    override fun getSpriteHeight(): Int {
        return offsetY + imageHeight + trimBottom
    }

    override abstract fun draw(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int)

    override abstract fun drawWithSurface(var1: Int, var2: Int, var3: ComponentSurface, var4: Int, var5: Int)

    abstract fun drawParticle(
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

    override fun drawScaledColored(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        if (cachedClipping == null) {
            cachedClipping = IntArray(4)
        }
        toolkit.getClipping(cachedClipping!!)
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

    fun computeAffineTransform(f_1: Float, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float): Boolean {
        val i_7 = offsetX + imageWidth + trimRight
        val i_8 = offsetY + imageHeight + trimBottom
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
        if (i_7 != imageWidth || i_8 != imageHeight) {
            f_9 = (f_3Var - f_1Var) / i_7
            f_10 = (f_4Var - f_2Var) / i_7
            f_11 = (f_5Var - f_1Var) / i_8
            f_12 = (f_6Var - f_2Var) / i_8
            f_13 = f_11 * offsetY
            f_14 = f_12 * offsetY
            f_15 = f_9 * offsetX
            f_16 = f_10 * offsetX
            val f_17 = -f_9 * trimRight
            val f_18 = -f_10 * trimRight
            val f_19 = -f_11 * trimBottom
            val f_20 = -f_12 * trimBottom
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
                viewportStride = toolkit.viewportWidth
                pixelOffset = (f_13.toInt() * viewportStride + f_11).toInt()
                f_15 = (f_3Var - f_1Var) * (f_6Var - f_2Var) - (f_4Var - f_2Var) * (f_5Var - f_1Var)
                f_16 = (f_5Var - f_1Var) * (f_4Var - f_2Var) - (f_6Var - f_2Var) * (f_3Var - f_1Var)
                dudx = ((f_6Var - f_2Var) * 4096.0f * imageWidth / f_15).toInt()
                dvdx = ((f_4Var - f_2Var) * 4096.0f * imageHeight / f_16).toInt()
                dudy = ((f_5Var - f_1Var) * 4096.0f * imageWidth / f_16).toInt()
                dvdy = ((f_3Var - f_1Var) * 4096.0f * imageHeight / f_15).toInt()
                subpixelOffsetX = (f_11 * 16.0f + 8.0f - (f_1Var + f_3Var + f_5Var + f_9) / 4.0f * 16.0f).toInt()
                subpixelOffsetY = (f_13 * 16.0f + 8.0f - (f_2Var + f_4Var + f_6Var + f_10) / 4.0f * 16.0f).toInt()
                uBase = (imageWidth shr 1 shl 12) + (subpixelOffsetY * dudy shr 4)
                vBase = (imageHeight shr 1 shl 12) + (subpixelOffsetY * dvdy shr 4)
                uOffsetX = subpixelOffsetX * dudx shr 4
                vOffsetX = subpixelOffsetX * dvdx shr 4
                drawMinX = f_11.toInt()
                drawWidth = f_12.toInt()
                drawMinY = f_13.toInt()
                drawHeight = f_14.toInt()
                return true
            }
        }
    }

    override fun drawAffine(f_1: Float, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float, i_7: Int, i_8: Int, i_9: Int) {
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        } else {
            if (computeAffineTransform(f_1, f_2, f_3, f_4, f_5, f_6)) {
                blendColor = i_8
                if (i_7 != 1) {
                    colorAlpha = i_8 ushr 24
                    colorInverseAlpha = 256 - colorAlpha
                    if (i_7 == 0) {
                        colorRed = (i_8 and 0xff0000) shr 16
                        colorGreen = (i_8 and 0xff00) shr 8
                        colorBlue = i_8 and 0xff
                    } else if (i_7 == 2) {
                        overlayAlpha = i_8 ushr 24
                        overlayInverseAlpha = 256 - overlayAlpha
                        val i_11 = (i_8 and 0xff00ff) * overlayInverseAlpha and -16711936
                        val i_12 = (i_8 and 0xff00) * overlayInverseAlpha and 0xff0000
                        overlayPrecomputed = (i_11 or i_12) ushr 8
                    }
                }

                if (i_7 == 1) {
                    if (i_9 == 0) {
                        drawAffineTransformed(1, 0)
                    } else if (i_9 == 1) {
                        drawAffineTransformed(1, 1)
                    } else if (i_9 == 2) {
                        drawAffineTransformed(1, 2)
                    }
                } else if (i_7 == 0) {
                    if (i_9 == 0) {
                        drawAffineTransformed(0, 0)
                    } else if (i_9 == 1) {
                        drawAffineTransformed(0, 1)
                    } else if (i_9 == 2) {
                        drawAffineTransformed(0, 2)
                    }
                } else if (i_7 == 3) {
                    if (i_9 == 0) {
                        drawAffineTransformed(3, 0)
                    } else if (i_9 == 1) {
                        drawAffineTransformed(3, 1)
                    } else if (i_9 == 2) {
                        drawAffineTransformed(3, 2)
                    }
                } else if (i_7 == 2) {
                    if (i_9 == 0) {
                        drawAffineTransformed(2, 0)
                    } else if (i_9 == 1) {
                        drawAffineTransformed(2, 1)
                    } else if (i_9 == 2) {
                        drawAffineTransformed(2, 2)
                    }
                }
            }
        }
    }

    override fun getSpriteWidth(): Int {
        return offsetX + imageWidth + trimRight
    }

    abstract fun drawMasked(var1: IntArray, var2: IntArray, var3: Int, var4: Int)

    override fun cacheSizeInto(ints_1: IntArray) {
        ints_1[0] = offsetX
        ints_1[1] = offsetY
        ints_1[2] = trimRight
        ints_1[3] = trimBottom
    }

    override abstract fun drawColoredCircle(var1: Int, var2: Int, var3: Int, var4: Int, var5: Int, colorArgb: Int, blendMode: Int)

    override fun drawAffineMasked(f_1: Float, f_2: Float, f_3: Float, f_4: Float, f_5: Float, f_6: Float, i_7: Int, class4558: ComponentSurface, i_9: Int, i_10: Int) {
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        if (computeAffineTransform(f_1, f_2, f_3, f_4, f_5, f_6)) {
            val packetsdecoder11 = class4558 as ComponentDimensionSM
            drawMasked(packetsdecoder11.rowStartX, packetsdecoder11.rowWidth, drawMinX - i_9, -i_10 - (drawHeight - drawMinY))
        }
    }

    companion object {
        @JvmField
        var colorAlpha: Int = 0
        @JvmField
        var colorInverseAlpha: Int = 0
        @JvmField
        var colorRed: Int = 0
        @JvmField
        var colorGreen: Int = 0
        @JvmField
        var colorBlue: Int = 0
        @JvmField
        var overlayAlpha: Int = 0
        @JvmField
        var overlayInverseAlpha: Int = 0
        @JvmField
        var overlayPrecomputed: Int = 0
        @JvmField
        var viewportStride: Int = 0
        @JvmField
        var pixelOffset: Int = 0
        @JvmField
        var dudx: Int = 0
        @JvmField
        var dvdx: Int = 0
        @JvmField
        var dudy: Int = 0
        @JvmField
        var dvdy: Int = 0
        @JvmField
        var subpixelOffsetX: Int = 0
        @JvmField
        var subpixelOffsetY: Int = 0
        @JvmField
        var uBase: Int = 0
        @JvmField
        var vBase: Int = 0
        @JvmField
        var uOffsetX: Int = 0
        @JvmField
        var vOffsetX: Int = 0
        @JvmField
        var drawMinX: Int = 0
        @JvmField
        var drawWidth: Int = 0
        @JvmField
        var drawMinY: Int = 0
        @JvmField
        var drawHeight: Int = 0
        @JvmField
        var blendColor: Int = 0
    }
}
