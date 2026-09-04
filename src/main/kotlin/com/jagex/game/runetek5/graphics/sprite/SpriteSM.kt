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

    override fun setSize(minX: Int, minY: Int, maxX: Int, maxY: Int) {
        offsetX = minX
        offsetY = minY
        trimRight = maxX
        trimBottom = maxY
    }

    override fun getWidth(): Int {
        return imageWidth
    }

    override fun getHeight(): Int {
        return imageHeight
    }

    abstract fun drawAffineTransformed(combineMode: Int, blendMode: Int)

    override fun getSpriteHeight(): Int {
        return offsetY + imageHeight + trimBottom
    }

    override abstract fun draw(x: Int, y: Int, combineMode: Int, colorArgb: Int, blendMode: Int)

    override abstract fun drawWithSurface(x: Int, y: Int, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int)

    abstract fun drawParticle(
        colorWrite: Boolean,
        depthTest: Boolean,
        transparent: Boolean,
        x: Int,
        y: Int,
        depth: Float,
        width: Int,
        height: Int,
        combineMode: Int,
        colorArgb: Int,
        blendMode: Int,
        depthWrite: Boolean
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
        val spriteWidth = getSpriteWidth()
        val spriteHeight = getSpriteHeight()
        val tilesX = (width + spriteWidth - 1) / spriteWidth
        val tilesY = (spriteHeight + height - 1) / spriteHeight

        for (tileY in 0 until tilesY) {
            val drawY = tileY * spriteHeight

            for (tileX in 0 until tilesX) {
                draw(tileX * spriteWidth + x, drawY + y, combineMode, colorArgb, blendMode)
            }
        }

        toolkit.setClipping(cachedClipping!![0], cachedClipping!![1], cachedClipping!![2], cachedClipping!![3])
    }

    fun computeAffineTransform(topLeftX: Float, topLeftY: Float, topRightX: Float, topRightY: Float, bottomLeftX: Float, bottomLeftY: Float): Boolean {
        val fullWidth = offsetX + imageWidth + trimRight
        val fullHeight = offsetY + imageHeight + trimBottom
        var dxRight: Float
        var dyRight: Float
        var minX: Float
        var maxX: Float
        var minY: Float
        var maxY: Float
        var crossProductA: Float
        var crossProductB: Float
        var topLeftXVar = topLeftX
        var topLeftYVar = topLeftY
        var topRightXVar = topRightX
        var topRightYVar = topRightY
        var bottomLeftXVar = bottomLeftX
        var bottomLeftYVar = bottomLeftY
        if (fullWidth != imageWidth || fullHeight != imageHeight) {
            dxRight = (topRightXVar - topLeftXVar) / fullWidth
            dyRight = (topRightYVar - topLeftYVar) / fullWidth
            val dxDown = (bottomLeftXVar - topLeftXVar) / fullHeight
            val dyDown = (bottomLeftYVar - topLeftYVar) / fullHeight
            val offsetYDx = dxDown * offsetY
            val offsetYDy = dyDown * offsetY
            val offsetXDx = dxRight * offsetX
            val offsetXDy = dyRight * offsetX
            val trimRightDx = -dxRight * trimRight
            val trimRightDy = -dyRight * trimRight
            val trimBottomDx = -dxDown * trimBottom
            val trimBottomDy = -dyDown * trimBottom
            topLeftXVar += offsetXDx + offsetYDx
            topLeftYVar += offsetXDy + offsetYDy
            topRightXVar += trimRightDx + offsetYDx
            topRightYVar += trimRightDy + offsetYDy
            bottomLeftXVar += offsetXDx + trimBottomDx
            bottomLeftYVar += offsetXDy + trimBottomDy
        }

        dxRight = bottomLeftXVar + (topRightXVar - topLeftXVar)
        dyRight = topRightYVar + (bottomLeftYVar - topLeftYVar)
        if (topLeftXVar < topRightXVar) {
            minX = topLeftXVar
            maxX = topRightXVar
        } else {
            minX = topRightXVar
            maxX = topLeftXVar
        }

        if (bottomLeftXVar < minX) {
            minX = bottomLeftXVar
        }

        if (dxRight < minX) {
            minX = dxRight
        }

        if (bottomLeftXVar > maxX) {
            maxX = bottomLeftXVar
        }

        if (dxRight > maxX) {
            maxX = dxRight
        }

        if (topLeftYVar < topRightYVar) {
            minY = topLeftYVar
            maxY = topRightYVar
        } else {
            minY = topRightYVar
            maxY = topLeftYVar
        }

        if (bottomLeftYVar < minY) {
            minY = bottomLeftYVar
        }

        if (dyRight < minY) {
            minY = dyRight
        }

        if (bottomLeftYVar > maxY) {
            maxY = bottomLeftYVar
        }

        if (dyRight > maxY) {
            maxY = dyRight
        }

        if (minX < toolkit.clipLeft) {
            minX = toolkit.clipLeft.toFloat()
        }

        if (maxX > toolkit.clipRight) {
            maxX = toolkit.clipRight.toFloat()
        }

        if (minY < toolkit.clipTop) {
            minY = toolkit.clipTop.toFloat()
        }

        if (maxY > toolkit.clipBottom) {
            maxY = toolkit.clipBottom.toFloat()
        }

        maxX = minX - maxX
        if (maxX >= 0.0f) {
            return false
        } else {
            maxY = minY - maxY
            if (maxY >= 0.0f) {
                return false
            } else {
                viewportStride = toolkit.viewportWidth
                pixelOffset = (minY.toInt() * viewportStride + minX).toInt()
                crossProductA = (topRightXVar - topLeftXVar) * (bottomLeftYVar - topLeftYVar) - (topRightYVar - topLeftYVar) * (bottomLeftXVar - topLeftXVar)
                crossProductB = (bottomLeftXVar - topLeftXVar) * (topRightYVar - topLeftYVar) - (bottomLeftYVar - topLeftYVar) * (topRightXVar - topLeftXVar)
                dudx = ((bottomLeftYVar - topLeftYVar) * 4096.0f * imageWidth / crossProductA).toInt()
                dvdx = ((topRightYVar - topLeftYVar) * 4096.0f * imageHeight / crossProductB).toInt()
                dudy = ((bottomLeftXVar - topLeftXVar) * 4096.0f * imageWidth / crossProductB).toInt()
                dvdy = ((topRightXVar - topLeftXVar) * 4096.0f * imageHeight / crossProductA).toInt()
                subpixelOffsetX = (minX * 16.0f + 8.0f - (topLeftXVar + topRightXVar + bottomLeftXVar + dxRight) / 4.0f * 16.0f).toInt()
                subpixelOffsetY = (minY * 16.0f + 8.0f - (topLeftYVar + topRightYVar + bottomLeftYVar + dyRight) / 4.0f * 16.0f).toInt()
                uBase = (imageWidth shr 1 shl 12) + (subpixelOffsetY * dudy shr 4)
                vBase = (imageHeight shr 1 shl 12) + (subpixelOffsetY * dvdy shr 4)
                uOffsetX = subpixelOffsetX * dudx shr 4
                vOffsetX = subpixelOffsetX * dvdx shr 4
                drawMinX = minX.toInt()
                drawWidth = maxX.toInt()
                drawMinY = minY.toInt()
                drawHeight = maxY.toInt()
                return true
            }
        }
    }

    override fun drawAffine(topLeftX: Float, topLeftY: Float, topRightX: Float, topRightY: Float, bottomLeftX: Float, bottomLeftY: Float, combineMode: Int, colorArgb: Int, blendMode: Int) {
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        } else {
            if (computeAffineTransform(topLeftX, topLeftY, topRightX, topRightY, bottomLeftX, bottomLeftY)) {
                blendColor = colorArgb
                if (combineMode != 1) {
                    colorAlpha = colorArgb ushr 24
                    colorInverseAlpha = 256 - colorAlpha
                    if (combineMode == 0) {
                        colorRed = (colorArgb and 0xff0000) shr 16
                        colorGreen = (colorArgb and 0xff00) shr 8
                        colorBlue = colorArgb and 0xff
                    } else if (combineMode == 2) {
                        overlayAlpha = colorArgb ushr 24
                        overlayInverseAlpha = 256 - overlayAlpha
                        val rbComponents = (colorArgb and 0xff00ff) * overlayInverseAlpha and -16711936
                        val gComponent = (colorArgb and 0xff00) * overlayInverseAlpha and 0xff0000
                        overlayPrecomputed = (rbComponents or gComponent) ushr 8
                    }
                }

                if (combineMode == 1) {
                    if (blendMode == 0) {
                        drawAffineTransformed(1, 0)
                    } else if (blendMode == 1) {
                        drawAffineTransformed(1, 1)
                    } else if (blendMode == 2) {
                        drawAffineTransformed(1, 2)
                    }
                } else if (combineMode == 0) {
                    if (blendMode == 0) {
                        drawAffineTransformed(0, 0)
                    } else if (blendMode == 1) {
                        drawAffineTransformed(0, 1)
                    } else if (blendMode == 2) {
                        drawAffineTransformed(0, 2)
                    }
                } else if (combineMode == 3) {
                    if (blendMode == 0) {
                        drawAffineTransformed(3, 0)
                    } else if (blendMode == 1) {
                        drawAffineTransformed(3, 1)
                    } else if (blendMode == 2) {
                        drawAffineTransformed(3, 2)
                    }
                } else if (combineMode == 2) {
                    if (blendMode == 0) {
                        drawAffineTransformed(2, 0)
                    } else if (blendMode == 1) {
                        drawAffineTransformed(2, 1)
                    } else if (blendMode == 2) {
                        drawAffineTransformed(2, 2)
                    }
                }
            }
        }
    }

    override fun getSpriteWidth(): Int {
        return offsetX + imageWidth + trimRight
    }

    abstract fun drawMasked(rowStartX: IntArray, rowWidth: IntArray, maskOffsetX: Int, maskOffsetY: Int)

    override fun cacheSizeInto(sizeOut: IntArray) {
        sizeOut[0] = offsetX
        sizeOut[1] = offsetY
        sizeOut[2] = trimRight
        sizeOut[3] = trimBottom
    }

    override abstract fun drawColoredCircle(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int)

    override fun drawAffineMasked(topLeftX: Float, topLeftY: Float, topRightX: Float, topRightY: Float, bottomLeftX: Float, bottomLeftY: Float, combineMode: Int, surface: ComponentSurface, surfaceOffsetX: Int, surfaceOffsetY: Int) {
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        if (computeAffineTransform(topLeftX, topLeftY, topRightX, topRightY, bottomLeftX, bottomLeftY)) {
            val dimensionSM = surface as ComponentDimensionSM
            drawMasked(dimensionSM.rowStartX, dimensionSM.rowWidth, drawMinX - surfaceOffsetX, -surfaceOffsetY - (drawHeight - drawMinY))
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
