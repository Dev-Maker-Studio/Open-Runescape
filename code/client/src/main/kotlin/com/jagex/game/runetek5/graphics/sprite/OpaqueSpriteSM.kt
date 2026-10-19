package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.framebuffer.SafeModeFrameBuffer
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.toolkit.ComponentDimensionSM

class OpaqueSpriteSM : SpriteSM {

    @JvmField
    val pixels: IntArray

    constructor(safeModeToolkit: SafeModeToolkit, srcPixels: IntArray, srcOffset: Int, srcStride: Int, width: Int, height: Int, copyPixels: Boolean) : super(safeModeToolkit, width, height) {
        if (copyPixels) {
            this.pixels = IntArray(width * height)
        } else {
            this.pixels = srcPixels
        }

        var srcRowGap = srcStride - imageWidth
        var dstIndex = 0
        var srcIndex = srcOffset

        for (y in 0 until height) {
            for (x in 0 until width) {
                val srcPixel = srcPixels[srcIndex++]
                if (srcPixel ushr 24 == 255) {
                    this.pixels[dstIndex++] = if ((srcPixel and 0xffffff) == 0) -16777215 else srcPixel
                } else {
                    this.pixels[dstIndex++] = 0
                }
            }
            srcIndex += srcRowGap
        }
    }

    constructor(safeModeToolkit: SafeModeToolkit, width: Int, height: Int) : super(safeModeToolkit, width, height) {
        this.pixels = IntArray(height * width)
    }

    constructor(safeModeToolkit: SafeModeToolkit, srcPixels: IntArray, width: Int, height: Int) : super(safeModeToolkit, width, height) {
        this.pixels = srcPixels
    }

    override fun captureScreenRegion(dstX: Int, dstY: Int, width: Int, height: Int, srcX: Int, srcY: Int) {
        val screenPixels = toolkit.colourBuffer ?: throw IllegalStateException("")
        for (row in 0 until height) {
            val dstIndex = (dstY + row) * imageWidth + dstX
            val srcIndex = (srcY + row) * toolkit.viewportWidth + srcX
            if (width >= 0) {
                System.arraycopy(screenPixels, srcIndex + 0, this.pixels, dstIndex + 0, width)
            }
        }
    }

    override fun getFramebuffer(): FrameBuffer {
        return SafeModeFrameBuffer(imageWidth, imageHeight, this.pixels)
    }

    override fun draw(x: Int, y: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        var drawY = y
        var drawX = x
        var color = colorArgb
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        val screenPixels = toolkit.colourBuffer ?: return
        val viewportWidth = toolkit.viewportWidth
        drawX += offsetX
        drawY += offsetY
        var dstIndex = viewportWidth * drawY + drawX
        var srcIndex = 0
        var h = imageHeight
        var w = imageWidth
        var dstStep = viewportWidth - w
        var srcStep = 0
        var clipDelta: Int
        if (drawY < toolkit.clipTop) {
            clipDelta = toolkit.clipTop - drawY
            h -= clipDelta
            drawY = toolkit.clipTop
            srcIndex += clipDelta * w
            dstIndex += clipDelta * viewportWidth
        }

        if (h + drawY > toolkit.clipBottom) {
            h -= h + drawY - toolkit.clipBottom
        }

        if (drawX < toolkit.clipLeft) {
            clipDelta = toolkit.clipLeft - drawX
            w -= clipDelta
            drawX = toolkit.clipLeft
            srcIndex += clipDelta
            dstIndex += clipDelta
            srcStep += clipDelta
            dstStep += clipDelta
        }

        if (w + drawX > toolkit.clipRight) {
            clipDelta = w + drawX - toolkit.clipRight
            w -= clipDelta
            srcStep += clipDelta
            dstStep += clipDelta
        }

        if (w > 0 && h > 0) {
            if (blendMode == 0) {
                if (combineMode == 1) {
                    var row = -h
                    while (row < 0) {
                        var rowEnd = dstIndex + w - 3
                        while (dstIndex < rowEnd) {
                            screenPixels[dstIndex++] = this.pixels[srcIndex++]
                            screenPixels[dstIndex++] = this.pixels[srcIndex++]
                            screenPixels[dstIndex++] = this.pixels[srcIndex++]
                            screenPixels[dstIndex++] = this.pixels[srcIndex++]
                        }

                        rowEnd += 3
                        while (dstIndex < rowEnd) {
                            screenPixels[dstIndex++] = this.pixels[srcIndex++]
                        }

                        dstIndex += dstStep
                        srcIndex += srcStep
                        row++
                    }
                } else if (combineMode == 0) {
                    var mulR = (color and 0xff0000) shr 16
                    var mulG = (color and 0xff00) shr 8
                    var mulB = color and 0xff

                    var row = -h
                    while (row < 0) {
                        var col = -w
                        while (col < 0) {
                            var srcPixel = this.pixels[srcIndex++]
                            var r = mulR * (srcPixel and 0xff0000) and 0xff000000.toInt()
                            var g = mulG * (srcPixel and 0xff00) and 0xff0000
                            var b = (srcPixel and 0xff) * mulB and 0xff00
                            screenPixels[dstIndex++] = (r or g or b) ushr 8
                            col++
                        }

                        dstIndex += dstStep
                        srcIndex += srcStep
                        row++
                    }
                } else if (combineMode == 3) {
                    var row = -h
                    while (row < 0) {
                        var col = -w
                        while (col < 0) {
                            var srcPixel = this.pixels[srcIndex++]
                            var sum = srcPixel + color
                            var lowSum = (srcPixel and 0xff00ff) + (color and 0xff00ff)
                            var carry = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                            screenPixels[dstIndex++] = sum - carry or (carry - (carry ushr 8))
                            col++
                        }

                        dstIndex += dstStep
                        srcIndex += srcStep
                        row++
                    }
                } else {
                    if (combineMode != 2) {
                        throw IllegalArgumentException()
                    }

                    var alpha = color ushr 24
                    var invAlpha = 256 - alpha
                    var rbTerm = invAlpha * (color and 0xff00ff) and -16711936
                    var gTerm = invAlpha * (color and 0xff00) and 0xff0000
                    color = (rbTerm or gTerm) ushr 8

                    var row = -h
                    while (row < 0) {
                        var col = -w
                        while (col < 0) {
                            var srcPixel = this.pixels[srcIndex++]
                            rbTerm = (srcPixel and 0xff00ff) * alpha and -16711936
                            gTerm = alpha * (srcPixel and 0xff00) and 0xff0000
                            screenPixels[dstIndex++] = color + ((rbTerm or gTerm) ushr 8)
                            col++
                        }

                        dstIndex += dstStep
                        srcIndex += srcStep
                        row++
                    }
                }
            } else {
                if (blendMode == 1) {
                    if (combineMode == 1) {
                        var row = -h
                        while (row < 0) {
                            var rowEnd = dstIndex + w - 3

                            while (dstIndex < rowEnd) {
                                var srcPixel = this.pixels[srcIndex++]
                                if (srcPixel != 0) {
                                    screenPixels[dstIndex++] = srcPixel
                                } else {
                                    ++dstIndex
                                }

                                srcPixel = this.pixels[srcIndex++]
                                if (srcPixel != 0) {
                                    screenPixels[dstIndex++] = srcPixel
                                } else {
                                    ++dstIndex
                                }

                                srcPixel = this.pixels[srcIndex++]
                                if (srcPixel != 0) {
                                    screenPixels[dstIndex++] = srcPixel
                                } else {
                                    ++dstIndex
                                }

                                srcPixel = this.pixels[srcIndex++]
                                if (srcPixel != 0) {
                                    screenPixels[dstIndex++] = srcPixel
                                } else {
                                    ++dstIndex
                                }
                            }

                            rowEnd += 3

                            while (dstIndex < rowEnd) {
                                var srcPixel = this.pixels[srcIndex++]
                                if (srcPixel != 0) {
                                    screenPixels[dstIndex++] = srcPixel
                                } else {
                                    ++dstIndex
                                }
                            }

                            dstIndex += dstStep
                            srcIndex += srcStep
                            row++
                        }
                    } else if (combineMode == 0) {
                        if ((color and 0xffffff) == 16777215) {
                            var alpha = color ushr 24
                            var invAlpha = 256 - alpha

                            var row = -h
                            while (row < 0) {
                                var col = -w
                                while (col < 0) {
                                    var srcPixel = this.pixels[srcIndex++]
                                    if (srcPixel != 0) {
                                        var dstPixel = screenPixels[dstIndex]
                                        screenPixels[dstIndex++] = ((srcPixel and 0xff00ff) * alpha + (dstPixel and 0xff00ff) * invAlpha and -16711936) + (alpha * (srcPixel and 0xff00) + invAlpha * (dstPixel and 0xff00) and 0xff0000) shr 8
                                    } else {
                                        ++dstIndex
                                    }
                                    col++
                                }

                                dstIndex += dstStep
                                srcIndex += srcStep
                                row++
                            }
                        } else {
                            var mulR = (color and 0xff0000) shr 16
                            var mulG = (color and 0xff00) shr 8
                            var mulB = color and 0xff
                            var alpha = color ushr 24
                            var invAlpha = 256 - alpha

                            var row = -h
                            while (row < 0) {
                                var col = -w
                                while (col < 0) {
                                    var srcPixel = this.pixels[srcIndex++]
                                    if (srcPixel != 0) {
                                        if (alpha != 255) {
                                            var r = mulR * (srcPixel and 0xff0000) and 0xff000000.toInt()
                                            var g = mulG * (srcPixel and 0xff00) and 0xff0000
                                            var b = (srcPixel and 0xff) * mulB and 0xff00
                                            srcPixel = (r or g or b) ushr 8
                                            var dstPixel = screenPixels[dstIndex]
                                            screenPixels[dstIndex++] = ((srcPixel and 0xff00ff) * alpha + (dstPixel and 0xff00ff) * invAlpha and -16711936) + (alpha * (srcPixel and 0xff00) + invAlpha * (dstPixel and 0xff00) and 0xff0000) shr 8
                                        } else {
                                            var r = mulR * (srcPixel and 0xff0000) and 0xff000000.toInt()
                                            var g = mulG * (srcPixel and 0xff00) and 0xff0000
                                            var b = (srcPixel and 0xff) * mulB and 0xff00
                                            screenPixels[dstIndex++] = (r or g or b) ushr 8
                                        }
                                    } else {
                                        ++dstIndex
                                    }
                                    col++
                                }

                                dstIndex += dstStep
                                srcIndex += srcStep
                                row++
                            }
                        }
                    } else if (combineMode == 3) {
                        var alpha = color ushr 24
                        var invAlpha = 256 - alpha

                        var row = -h
                        while (row < 0) {
                            var col = -w
                            while (col < 0) {
                                var srcPixel = this.pixels[srcIndex++]
                                var sum = srcPixel + color
                                var lowSum = (srcPixel and 0xff00ff) + (color and 0xff00ff)
                                var blended = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                                blended = sum - blended or (blended - (blended ushr 8))
                                if (srcPixel == 0 && alpha != 255) {
                                    srcPixel = blended
                                    blended = screenPixels[dstIndex]
                                    blended = ((srcPixel and 0xff00ff) * alpha + (blended and 0xff00ff) * invAlpha and -16711936) + (alpha * (srcPixel and 0xff00) + invAlpha * (blended and 0xff00) and 0xff0000) shr 8
                                }

                                screenPixels[dstIndex++] = blended
                                col++
                            }

                            dstIndex += dstStep
                            srcIndex += srcStep
                            row++
                        }
                    } else {
                        if (combineMode != 2) {
                            throw IllegalArgumentException()
                        }

                        var alpha = color ushr 24
                        var invAlpha = 256 - alpha
                        var rbTerm = invAlpha * (color and 0xff00ff) and -16711936
                        var gTerm = invAlpha * (color and 0xff00) and 0xff0000
                        color = (rbTerm or gTerm) ushr 8

                        var row = -h
                        while (row < 0) {
                            var col = -w
                            while (col < 0) {
                                var srcPixel = this.pixels[srcIndex++]
                                if (srcPixel != 0) {
                                    rbTerm = (srcPixel and 0xff00ff) * alpha and -16711936
                                    gTerm = alpha * (srcPixel and 0xff00) and 0xff0000
                                    screenPixels[dstIndex++] = color + ((rbTerm or gTerm) ushr 8)
                                } else {
                                    ++dstIndex
                                }
                                col++
                            }

                            dstIndex += dstStep
                            srcIndex += srcStep
                            row++
                        }
                    }
                } else {
                    if (blendMode != 2) {
                        throw IllegalArgumentException()
                    }

                    if (combineMode == 1) {
                        var row = -h
                        while (row < 0) {
                            var col = -w
                            while (col < 0) {
                                var srcPixel = this.pixels[srcIndex++]
                                if (srcPixel != 0) {
                                    var dstPixel = screenPixels[dstIndex]
                                    var sum = srcPixel + dstPixel
                                    var lowSum = (srcPixel and 0xff00ff) + (dstPixel and 0xff00ff)
                                    dstPixel = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                                    screenPixels[dstIndex++] = sum - dstPixel or (dstPixel - (dstPixel ushr 8))
                                } else {
                                    ++dstIndex
                                }
                                col++
                            }

                            dstIndex += dstStep
                            srcIndex += srcStep
                            row++
                        }
                    } else if (combineMode == 0) {
                        var mulR = (color and 0xff0000) shr 16
                        var mulG = (color and 0xff00) shr 8
                        var mulB = color and 0xff

                        var row = -h
                        while (row < 0) {
                            var col = -w
                            while (col < 0) {
                                var srcPixel = this.pixels[srcIndex++]
                                if (srcPixel != 0) {
                                    var r = mulR * (srcPixel and 0xff0000) and 0xff000000.toInt()
                                    var g = mulG * (srcPixel and 0xff00) and 0xff0000
                                    var b = (srcPixel and 0xff) * mulB and 0xff00
                                    srcPixel = (r or g or b) ushr 8
                                    var dstPixel = screenPixels[dstIndex]
                                    var sum = srcPixel + dstPixel
                                    var lowSum = (srcPixel and 0xff00ff) + (dstPixel and 0xff00ff)
                                    dstPixel = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                                    screenPixels[dstIndex++] = sum - dstPixel or (dstPixel - (dstPixel ushr 8))
                                } else {
                                    ++dstIndex
                                }
                                col++
                            }

                            dstIndex += dstStep
                            srcIndex += srcStep
                            row++
                        }
                    } else if (combineMode == 3) {
                        var row = -h
                        while (row < 0) {
                            var col = -w
                            while (col < 0) {
                                var srcPixel = this.pixels[srcIndex++]
                                var sum = srcPixel + color
                                var lowSum = (srcPixel and 0xff00ff) + (color and 0xff00ff)
                                var carry = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                                srcPixel = sum - carry or (carry - (carry ushr 8))
                                carry = screenPixels[dstIndex]
                                sum = srcPixel + carry
                                lowSum = (srcPixel and 0xff00ff) + (carry and 0xff00ff)
                                carry = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                                screenPixels[dstIndex++] = sum - carry or (carry - (carry ushr 8))
                                col++
                            }

                            dstIndex += dstStep
                            srcIndex += srcStep
                            row++
                        }
                    } else {
                        if (combineMode != 2) {
                            throw IllegalArgumentException()
                        }

                        var alpha = color ushr 24
                        var invAlpha = 256 - alpha
                        var rbTerm = invAlpha * (color and 0xff00ff) and -16711936
                        var gTerm = invAlpha * (color and 0xff00) and 0xff0000
                        color = (rbTerm or gTerm) ushr 8

                        var row = -h
                        while (row < 0) {
                            var col = -w
                            while (col < 0) {
                                var srcPixel = this.pixels[srcIndex++]
                                if (srcPixel != 0) {
                                    rbTerm = (srcPixel and 0xff00ff) * alpha and -16711936
                                    gTerm = alpha * (srcPixel and 0xff00) and 0xff0000
                                    srcPixel = color + ((rbTerm or gTerm) ushr 8)
                                    var dstPixel = screenPixels[dstIndex]
                                    var sum = srcPixel + dstPixel
                                    var lowSum = (srcPixel and 0xff00ff) + (dstPixel and 0xff00ff)
                                    dstPixel = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                                    screenPixels[dstIndex++] = sum - dstPixel or (dstPixel - (dstPixel ushr 8))
                                } else {
                                    ++dstIndex
                                }
                                col++
                            }

                            dstIndex += dstStep
                            srcIndex += srcStep
                            row++
                        }
                    }
                }
            }
        }
    }

    override fun drawWithSurface(x: Int, y: Int, surface: ComponentSurface, maskX: Int, maskY: Int) {
        var drawY = y
        var drawX = x
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        val screenPixels = toolkit.colourBuffer ?: return
        drawX += offsetX
        drawY += offsetY
        var srcIndex = 0
        val viewportWidth = toolkit.viewportWidth
        var w = imageWidth
        var h = imageHeight
        var dstStep = viewportWidth - w
        var srcStep = 0
        var dstIndex = viewportWidth * drawY + drawX
        var clipDelta: Int
        if (drawY < toolkit.clipTop) {
            clipDelta = toolkit.clipTop - drawY
            h -= clipDelta
            drawY = toolkit.clipTop
            srcIndex += clipDelta * w
            dstIndex += clipDelta * viewportWidth
        }

        if (h + drawY > toolkit.clipBottom) {
            h -= h + drawY - toolkit.clipBottom
        }

        if (drawX < toolkit.clipLeft) {
            clipDelta = toolkit.clipLeft - drawX
            w -= clipDelta
            drawX = toolkit.clipLeft
            srcIndex += clipDelta
            dstIndex += clipDelta
            srcStep += clipDelta
            dstStep += clipDelta
        }

        if (w + drawX > toolkit.clipRight) {
            clipDelta = w + drawX - toolkit.clipRight
            w -= clipDelta
            srcStep += clipDelta
            dstStep += clipDelta
        }

        if (w > 0 && h > 0) {
            val dimensionSM = surface as ComponentDimensionSM
            val rowStartX = dimensionSM.rowStartX
            val rowWidth = dimensionSM.rowWidth
            var startY = drawY
            if (maskY > drawY) {
                startY = maskY
                dstIndex += viewportWidth * (maskY - drawY)
                srcIndex += (maskY - drawY) * imageWidth
            }

            val endY = Math.min(rowStartX.size + maskY, h + drawY)

            for (rowY in startY until endY) {
                var maskRowX = rowStartX[rowY - maskY] + maskX
                var maskRowWidth = rowWidth[rowY - maskY]
                var spanWidth = w
                var skip: Int
                if (drawX > maskRowX) {
                    skip = drawX - maskRowX
                    if (skip >= maskRowWidth) {
                        srcIndex += w + srcStep
                        dstIndex += w + dstStep
                        continue
                    }

                    maskRowWidth -= skip
                } else {
                    skip = maskRowX - drawX
                    if (skip >= w) {
                        srcIndex += w + srcStep
                        dstIndex += w + dstStep
                        continue
                    }

                    srcIndex += skip
                    spanWidth = w - skip
                    dstIndex += skip
                }

                skip = 0
                if (spanWidth < maskRowWidth) {
                    maskRowWidth = spanWidth
                } else {
                    skip = spanWidth - maskRowWidth
                }

                for (i in -maskRowWidth until 0) {
                    val srcPixel = this.pixels[srcIndex++]
                    if (srcPixel != 0) {
                        screenPixels[dstIndex++] = srcPixel
                    } else {
                        ++dstIndex
                    }
                }

                srcIndex += skip + srcStep
                dstIndex += skip + dstStep
            }
        }
    }

    override fun drawColoredCircle(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        var w = width
        var h = height
        var color = colorArgb
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        if (w > 0 && h > 0) {
            val screenPixels = toolkit.colourBuffer ?: return
            var u = 0
            var v = 0
            val viewportWidth = toolkit.viewportWidth
            val spriteWidth = offsetX + imageWidth + trimRight
            val spriteHeight = offsetY + imageHeight + trimBottom
            val du = (spriteWidth shl 16) / w
            val dv = (spriteHeight shl 16) / h
            var dstIndex: Int
            if (offsetX > 0) {
                dstIndex = (du + (offsetX shl 16) - 1) / du
                u += dstIndex * du - (offsetX shl 16)
            }

            if (offsetY > 0) {
                dstIndex = (dv + (offsetY shl 16) - 1) / dv
                v += dstIndex * dv - (offsetY shl 16)
            }

            if (imageWidth < spriteWidth) {
                w = ((imageWidth shl 16) - u + du - 1) / du
            }

            if (imageHeight < spriteHeight) {
                h = ((imageHeight shl 16) - v + dv - 1) / dv
            }

            var drawX = x
            var drawY = y
            if (offsetX > 0) {
                dstIndex = (du + (offsetX shl 16) - 1) / du
                drawX = x + dstIndex
            }
            if (offsetY > 0) {
                dstIndex = (dv + (offsetY shl 16) - 1) / dv
                drawY = y + dstIndex
            }

            dstIndex = viewportWidth * drawY + drawX
            var dstStep = viewportWidth - w
            if (drawY + h > toolkit.clipBottom) {
                h -= drawY + h - toolkit.clipBottom
            }

            var clipDelta: Int
            if (drawY < toolkit.clipTop) {
                clipDelta = toolkit.clipTop - drawY
                h -= clipDelta
                dstIndex += clipDelta * viewportWidth
                v += dv * clipDelta
            }

            if (w + drawX > toolkit.clipRight) {
                clipDelta = w + drawX - toolkit.clipRight
                w -= clipDelta
                dstStep += clipDelta
            }

            if (drawX < toolkit.clipLeft) {
                clipDelta = toolkit.clipLeft - drawX
                w -= clipDelta
                dstIndex += clipDelta
                u += du * clipDelta
                dstStep += clipDelta
            }

            if (blendMode == 0) {
                if (combineMode == 1) {
                    var startU = u

                    var row = -h
                    while (row < 0) {
                        var srcRowBase = imageWidth * (v shr 16)

                        var col = -w
                        while (col < 0) {
                            screenPixels[dstIndex++] = this.pixels[srcRowBase + (u shr 16)]
                            u += du
                            col++
                        }

                        v += dv
                        u = startU
                        dstIndex += dstStep
                        row++
                    }
                } else if (combineMode == 0) {
                    var mulR = (color and 0xff0000) shr 16
                    var mulG = (color and 0xff00) shr 8
                    var mulB = color and 0xff
                    var startU = u

                    var row = -h
                    while (row < 0) {
                        var srcRowBase = imageWidth * (v shr 16)

                        var col = -w
                        while (col < 0) {
                            var srcPixel = this.pixels[srcRowBase + (u shr 16)]
                            var r = mulR * (srcPixel and 0xff0000) and 0xff000000.toInt()
                            var g = mulG * (srcPixel and 0xff00) and 0xff0000
                            var b = (srcPixel and 0xff) * mulB and 0xff00
                            screenPixels[dstIndex++] = (r or g or b) ushr 8
                            u += du
                            col++
                        }

                        v += dv
                        u = startU
                        dstIndex += dstStep
                        row++
                    }
                } else if (combineMode == 3) {
                    var startU = u

                    var row = -h
                    while (row < 0) {
                        var srcRowBase = imageWidth * (v shr 16)

                        var col = -w
                        while (col < 0) {
                            var srcPixel = this.pixels[srcRowBase + (u shr 16)]
                            var sum = srcPixel + color
                            var lowSum = (srcPixel and 0xff00ff) + (color and 0xff00ff)
                            var carry = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                            screenPixels[dstIndex++] = sum - carry or (carry - (carry ushr 8))
                            u += du
                            col++
                        }

                        v += dv
                        u = startU
                        dstIndex += dstStep
                        row++
                    }
                } else {
                    if (combineMode != 2) {
                        throw IllegalArgumentException()
                    }

                    var alpha = color ushr 24
                    var invAlpha = 256 - alpha
                    var rbTerm = (color and 0xff00ff) * invAlpha and -16711936
                    var gTerm = invAlpha * (color and 0xff00) and 0xff0000
                    color = (rbTerm or gTerm) ushr 8
                    var startU = u

                    var row = -h
                    while (row < 0) {
                        var srcRowBase = imageWidth * (v shr 16)

                        var col = -w
                        while (col < 0) {
                            var srcPixel = this.pixels[srcRowBase + (u shr 16)]
                            rbTerm = (srcPixel and 0xff00ff) * alpha and -16711936
                            gTerm = alpha * (srcPixel and 0xff00) and 0xff0000
                            screenPixels[dstIndex++] = ((rbTerm or gTerm) ushr 8) + color
                            u += du
                            col++
                        }

                        v += dv
                        u = startU
                        dstIndex += dstStep
                        row++
                    }
                }
            } else {
                if (blendMode == 1) {
                    if (combineMode == 1) {
                        var startU = u

                        var row = -h
                        while (row < 0) {
                            var srcRowBase = imageWidth * (v shr 16)

                            var col = -w
                            while (col < 0) {
                                var srcPixel = this.pixels[srcRowBase + (u shr 16)]
                                if (srcPixel != 0) {
                                    screenPixels[dstIndex++] = srcPixel
                                } else {
                                    ++dstIndex
                                }

                                u += du
                                col++
                            }

                            v += dv
                            u = startU
                            dstIndex += dstStep
                            row++
                        }
                    } else if (combineMode == 0) {
                        var startU = u
                        if ((color and 0xffffff) == 16777215) {
                            var alpha = color ushr 24
                            var invAlpha = 256 - alpha

                            var row = -h
                            while (row < 0) {
                                var srcRowBase = imageWidth * (v shr 16)

                                var col = -w
                                while (col < 0) {
                                    var srcPixel = this.pixels[srcRowBase + (u shr 16)]
                                    if (srcPixel != 0) {
                                        var dstPixel = screenPixels[dstIndex]
                                        screenPixels[dstIndex++] = ((srcPixel and 0xff00ff) * alpha + (dstPixel and 0xff00ff) * invAlpha and -16711936) + (alpha * (srcPixel and 0xff00) + invAlpha * (dstPixel and 0xff00) and 0xff0000) shr 8
                                    } else {
                                        ++dstIndex
                                    }

                                    u += du
                                    col++
                                }

                                v += dv
                                u = startU
                                dstIndex += dstStep
                                row++
                            }
                        } else {
                            var mulR = (color and 0xff0000) shr 16
                            var mulG = (color and 0xff00) shr 8
                            var mulB = color and 0xff
                            var alpha = color ushr 24
                            var invAlpha = 256 - alpha

                            var row = -h
                            while (row < 0) {
                                var srcRowBase = imageWidth * (v shr 16)

                                var col = -w
                                while (col < 0) {
                                    var srcPixel = this.pixels[srcRowBase + (u shr 16)]
                                    if (srcPixel != 0) {
                                        if (alpha != 255) {
                                            var r = mulR * (srcPixel and 0xff0000) and 0xff000000.toInt()
                                            var g = mulG * (srcPixel and 0xff00) and 0xff0000
                                            var b = (srcPixel and 0xff) * mulB and 0xff00
                                            srcPixel = (r or g or b) ushr 8
                                            var dstPixel = screenPixels[dstIndex]
                                            screenPixels[dstIndex++] = ((srcPixel and 0xff00ff) * alpha + (dstPixel and 0xff00ff) * invAlpha and -16711936) + (alpha * (srcPixel and 0xff00) + invAlpha * (dstPixel and 0xff00) and 0xff0000) shr 8
                                        } else {
                                            var r = mulR * (srcPixel and 0xff0000) and 0xff000000.toInt()
                                            var g = mulG * (srcPixel and 0xff00) and 0xff0000
                                            var b = (srcPixel and 0xff) * mulB and 0xff00
                                            screenPixels[dstIndex++] = (r or g or b) ushr 8
                                        }
                                    } else {
                                        ++dstIndex
                                    }

                                    u += du
                                    col++
                                }

                                v += dv
                                u = startU
                                dstIndex += dstStep
                                row++
                            }
                        }
                    } else if (combineMode == 3) {
                        var startU = u
                        var alpha = color ushr 24
                        var invAlpha = 256 - alpha

                        var row = -h
                        while (row < 0) {
                            var srcRowBase = imageWidth * (v shr 16)

                            var col = -w
                            while (col < 0) {
                                var srcPixel = this.pixels[srcRowBase + (u shr 16)]
                                var sum = srcPixel + color
                                var lowSum = (srcPixel and 0xff00ff) + (color and 0xff00ff)
                                var blended = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                                blended = sum - blended or (blended - (blended ushr 8))
                                if (srcPixel == 0 && alpha != 255) {
                                    srcPixel = blended
                                    blended = screenPixels[dstIndex]
                                    blended = ((srcPixel and 0xff00ff) * alpha + (blended and 0xff00ff) * invAlpha and -16711936) + (alpha * (srcPixel and 0xff00) + invAlpha * (blended and 0xff00) and 0xff0000) shr 8
                                }

                                screenPixels[dstIndex++] = blended
                                u += du
                                col++
                            }

                            v += dv
                            u = startU
                            dstIndex += dstStep
                            row++
                        }
                    } else {
                        if (combineMode != 2) {
                            throw IllegalArgumentException()
                        }

                        var alpha = color ushr 24
                        var invAlpha = 256 - alpha
                        var rbTerm = (color and 0xff00ff) * invAlpha and -16711936
                        var gTerm = invAlpha * (color and 0xff00) and 0xff0000
                        color = (rbTerm or gTerm) ushr 8
                        var startU = u

                        var row = -h
                        while (row < 0) {
                            var srcRowBase = imageWidth * (v shr 16)

                            var col = -w
                            while (col < 0) {
                                var srcPixel = this.pixels[srcRowBase + (u shr 16)]
                                if (srcPixel != 0) {
                                    rbTerm = (srcPixel and 0xff00ff) * alpha and -16711936
                                    gTerm = alpha * (srcPixel and 0xff00) and 0xff0000
                                    screenPixels[dstIndex++] = ((rbTerm or gTerm) ushr 8) + color
                                } else {
                                    ++dstIndex
                                }

                                u += du
                                col++
                            }

                            v += dv
                            u = startU
                            dstIndex += dstStep
                            row++
                        }
                    }
                } else {
                    if (blendMode != 2) {
                        throw IllegalArgumentException()
                    }

                    if (combineMode == 1) {
                        var startU = u

                        var row = -h
                        while (row < 0) {
                            var srcRowBase = imageWidth * (v shr 16)

                            var col = -w
                            while (col < 0) {
                                var srcPixel = this.pixels[srcRowBase + (u shr 16)]
                                if (srcPixel != 0) {
                                    var dstPixel = screenPixels[dstIndex]
                                    var sum = srcPixel + dstPixel
                                    var lowSum = (srcPixel and 0xff00ff) + (dstPixel and 0xff00ff)
                                    dstPixel = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                                    screenPixels[dstIndex++] = sum - dstPixel or (dstPixel - (dstPixel ushr 8))
                                } else {
                                    ++dstIndex
                                }

                                u += du
                                col++
                            }

                            v += dv
                            u = startU
                            dstIndex += dstStep
                            row++
                        }
                    } else if (combineMode == 0) {
                        var startU = u
                        var mulR = (color and 0xff0000) shr 16
                        var mulG = (color and 0xff00) shr 8
                        var mulB = color and 0xff

                        var row = -h
                        while (row < 0) {
                            var srcRowBase = imageWidth * (v shr 16)

                            var col = -w
                            while (col < 0) {
                                var srcPixel = this.pixels[srcRowBase + (u shr 16)]
                                if (srcPixel != 0) {
                                    var r = mulR * (srcPixel and 0xff0000) and 0xff000000.toInt()
                                    var g = mulG * (srcPixel and 0xff00) and 0xff0000
                                    var b = (srcPixel and 0xff) * mulB and 0xff00
                                    srcPixel = (r or g or b) ushr 8
                                    var dstPixel = screenPixels[dstIndex]
                                    var sum = srcPixel + dstPixel
                                    var lowSum = (srcPixel and 0xff00ff) + (dstPixel and 0xff00ff)
                                    dstPixel = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                                    screenPixels[dstIndex++] = sum - dstPixel or (dstPixel - (dstPixel ushr 8))
                                } else {
                                    ++dstIndex
                                }

                                u += du
                                col++
                            }

                            v += dv
                            u = startU
                            dstIndex += dstStep
                            row++
                        }
                    } else if (combineMode == 3) {
                        var startU = u

                        var row = -h
                        while (row < 0) {
                            var srcRowBase = imageWidth * (v shr 16)

                            var col = -w
                            while (col < 0) {
                                var srcPixel = this.pixels[srcRowBase + (u shr 16)]
                                var sum = srcPixel + color
                                var lowSum = (srcPixel and 0xff00ff) + (color and 0xff00ff)
                                var carry = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                                srcPixel = sum - carry or (carry - (carry ushr 8))
                                carry = screenPixels[dstIndex]
                                sum = srcPixel + carry
                                lowSum = (srcPixel and 0xff00ff) + (carry and 0xff00ff)
                                carry = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                                screenPixels[dstIndex++] = sum - carry or (carry - (carry ushr 8))
                                u += du
                                col++
                            }

                            v += dv
                            u = startU
                            dstIndex += dstStep
                            row++
                        }
                    } else {
                        if (combineMode != 2) {
                            throw IllegalArgumentException()
                        }

                        var alpha = color ushr 24
                        var invAlpha = 256 - alpha
                        var rbTerm = (color and 0xff00ff) * invAlpha and -16711936
                        var gTerm = invAlpha * (color and 0xff00) and 0xff0000
                        color = (rbTerm or gTerm) ushr 8
                        var startU = u

                        var row = -h
                        while (row < 0) {
                            var srcRowBase = imageWidth * (v shr 16)

                            var col = -w
                            while (col < 0) {
                                var srcPixel = this.pixels[srcRowBase + (u shr 16)]
                                if (srcPixel != 0) {
                                    rbTerm = (srcPixel and 0xff00ff) * alpha and -16711936
                                    gTerm = alpha * (srcPixel and 0xff00) and 0xff0000
                                    srcPixel = ((rbTerm or gTerm) ushr 8) + color
                                    var dstPixel = screenPixels[dstIndex]
                                    var sum = srcPixel + dstPixel
                                    var lowSum = (srcPixel and 0xff00ff) + (dstPixel and 0xff00ff)
                                    dstPixel = (lowSum and 0x1000100) + (sum - lowSum and 0x10000)
                                    screenPixels[dstIndex++] = sum - dstPixel or (dstPixel - (dstPixel ushr 8))
                                } else {
                                    ++dstIndex
                                }

                                u += du
                                col++
                            }

                            v += dv
                            u = startU
                            dstIndex += dstStep
                            row++
                        }
                    }
                }
            }
        }
    }

    override fun drawParticle(
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
    ) {
        var w = width
        var h = height
        var color = colorArgb
        if (w > 0 && h > 0 && (colorWrite || depthTest)) {
            var u = 0
            var v = 0
            val spriteWidth = offsetX + imageWidth + trimRight
            val spriteHeight = offsetY + imageHeight + trimBottom
            val du = (spriteWidth shl 16) / w
            val dv = (spriteHeight shl 16) / h
            var dstIndex: Int
            var drawX = x
            var drawY = y
            if (offsetX > 0) {
                dstIndex = (du + (offsetX shl 16) - 1) / du
                drawX += dstIndex
                u += dstIndex * du - (offsetX shl 16)
            }

            if (offsetY > 0) {
                dstIndex = (dv + (offsetY shl 16) - 1) / dv
                drawY += dstIndex
                v += dstIndex * dv - (offsetY shl 16)
            }

            if (imageWidth < spriteWidth) {
                w = ((imageWidth shl 16) - u + du - 1) / du
            }

            if (imageHeight < spriteHeight) {
                h = ((imageHeight shl 16) - v + dv - 1) / dv
            }

            dstIndex = drawY * toolkit.viewportWidth + drawX
            var dstStep = toolkit.viewportWidth - w
            if (drawY + h > toolkit.clipBottom) {
                h -= drawY + h - toolkit.clipBottom
            }

            var clipDelta: Int
            if (drawY < toolkit.clipTop) {
                clipDelta = toolkit.clipTop - drawY
                h -= clipDelta
                dstIndex += toolkit.viewportWidth * clipDelta
                v += dv * clipDelta
            }

            if (drawX + w > toolkit.clipRight) {
                clipDelta = drawX + w - toolkit.clipRight
                w -= clipDelta
                dstStep += clipDelta
            }

            if (drawX < toolkit.clipLeft) {
                clipDelta = toolkit.clipLeft - drawX
                w -= clipDelta
                dstIndex += clipDelta
                u += du * clipDelta
                dstStep += clipDelta
            }

            val depthBuffer = toolkit.depthBuffer
            val screenPixels = toolkit.colourBuffer
            var t1: Int
            var t2: Int
            var t3: Int
            var t4: Int
            var t5: Int
            var t6: Int
            var t7: Int
            var t8: Int
            var t9: Int
            var t10: Int
            var t11: Int
            if (blendMode == 0) {
                if (combineMode == 1) {
                    t1 = u

                    t2 = -h
                    while (t2 < 0) {
                        t3 = imageWidth * (v shr 16)

                        t4 = -w
                        while (t4 < 0) {
                            if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                if (colorWrite) {
                                    screenPixels!![dstIndex] = this.pixels[t3 + (u shr 16)]
                                }

                                if (depthTest && depthWrite) {
                                    depthBuffer!![dstIndex] = depth
                                }
                            }

                            ++dstIndex
                            u += du
                            t4++
                        }

                        v += dv
                        u = t1
                        dstIndex += dstStep
                        t2++
                    }
                } else if (combineMode == 0) {
                    t1 = (color and 0xff0000) shr 16
                    t2 = (color and 0xff00) shr 8
                    t3 = color and 0xff
                    t4 = u

                    t5 = -h
                    while (t5 < 0) {
                        t6 = imageWidth * (v shr 16)

                        t7 = -w
                        while (t7 < 0) {
                            if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                t8 = this.pixels[t6 + (u shr 16)]
                                if (colorWrite) {
                                    t9 = t1 * (t8 and 0xff0000) and 0xff000000.toInt()
                                    t10 = t2 * (t8 and 0xff00) and 0xff0000
                                    t11 = (t8 and 0xff) * t3 and 0xff00
                                    screenPixels!![dstIndex] = (t9 or t10 or t11) ushr 8
                                }

                                if (depthTest && depthWrite) {
                                    depthBuffer!![dstIndex] = depth
                                }
                            }

                            ++dstIndex
                            u += du
                            t7++
                        }

                        v += dv
                        u = t4
                        dstIndex += dstStep
                        t5++
                    }
                } else if (combineMode == 3) {
                    t1 = u

                    t2 = -h
                    while (t2 < 0) {
                        t3 = imageWidth * (v shr 16)

                        t4 = -w
                        while (t4 < 0) {
                            if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                t5 = this.pixels[t3 + (u shr 16)]
                                if (colorWrite) {
                                    t6 = t5 + color
                                    t7 = (t5 and 0xff00ff) + (color and 0xff00ff)
                                    t8 = (t7 and 0x1000100) + (t6 - t7 and 0x10000)
                                    screenPixels!![dstIndex] = t6 - t8 or (t8 - (t8 ushr 8))
                                }

                                if (depthTest && depthWrite) {
                                    depthBuffer!![dstIndex] = depth
                                }
                            }

                            ++dstIndex
                            u += du
                            t4++
                        }

                        v += dv
                        u = t1
                        dstIndex += dstStep
                        t2++
                    }
                } else {
                    if (combineMode != 2) {
                        throw IllegalArgumentException()
                    }

                    t1 = color ushr 24
                    t2 = 256 - t1
                    t3 = (color and 0xff00ff) * t2 and -16711936
                    t4 = t2 * (color and 0xff00) and 0xff0000
                    color = (t3 or t4) ushr 8
                    t5 = u

                    t6 = -h
                    while (t6 < 0) {
                        t7 = imageWidth * (v shr 16)

                        t8 = -w
                        while (t8 < 0) {
                            if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                t9 = this.pixels[t7 + (u shr 16)]
                                if (colorWrite) {
                                    t3 = (t9 and 0xff00ff) * t1 and -16711936
                                    t4 = t1 * (t9 and 0xff00) and 0xff0000
                                    screenPixels!![dstIndex] = ((t3 or t4) ushr 8) + color
                                }

                                if (depthTest && depthWrite) {
                                    depthBuffer!![dstIndex] = depth
                                }
                            }

                            ++dstIndex
                            u += du
                            t8++
                        }

                        v += dv
                        u = t5
                        dstIndex += dstStep
                        t6++
                    }
                }
            } else {
                var t12: Int
                var t13: Int
                var t14: Int
                if (blendMode == 1) {
                    if (combineMode == 1) {
                        t1 = u

                        t2 = -h
                        while (t2 < 0) {
                            t3 = imageWidth * (v shr 16)

                            t4 = -w
                            while (t4 < 0) {
                                t5 = this.pixels[t3 + (u shr 16)]
                                if (t5 != 0 && (!depthTest || depth < depthBuffer!![dstIndex])) {
                                    if (colorWrite) {
                                        screenPixels!![dstIndex] = t5
                                    }

                                    if (depthTest && depthWrite) {
                                        depthBuffer!![dstIndex] = depth
                                    }
                                }

                                ++dstIndex
                                u += du
                                t4++
                            }

                            v += dv
                            u = t1
                            dstIndex += dstStep
                            t2++
                        }
                    } else if (combineMode == 0) {
                        t1 = u
                        if ((color and 0xffffff) == 16777215) {
                            t2 = color ushr 24
                            t3 = 256 - t2

                            t4 = -h
                            while (t4 < 0) {
                                t5 = imageWidth * (v shr 16)

                                t6 = -w
                                while (t6 < 0) {
                                    t7 = this.pixels[t5 + (u shr 16)]
                                    if (t7 != 0 && (!depthTest || depth < depthBuffer!![dstIndex])) {
                                        if (colorWrite) {
                                            t8 = screenPixels!![dstIndex]
                                            screenPixels[dstIndex] = ((t7 and 0xff00ff) * t2 + (t8 and 0xff00ff) * t3 and -16711936) + (t2 * (t7 and 0xff00) + t3 * (t8 and 0xff00) and 0xff0000) shr 8
                                        }

                                        if (depthTest && depthWrite) {
                                            depthBuffer!![dstIndex] = depth
                                        }
                                    }

                                    ++dstIndex
                                    u += du
                                    t6++
                                }

                                v += dv
                                u = t1
                                dstIndex += dstStep
                                t4++
                            }
                        } else {
                            t2 = (color and 0xff0000) shr 16
                            t3 = (color and 0xff00) shr 8
                            t4 = color and 0xff
                            t5 = color ushr 24
                            t6 = 256 - t5

                            t7 = -h
                            while (t7 < 0) {
                                t8 = imageWidth * (v shr 16)

                                t9 = -w
                                while (t9 < 0) {
                                    t10 = this.pixels[t8 + (u shr 16)]
                                    if (t10 != 0 && (!depthTest || depth < depthBuffer!![dstIndex])) {
                                        if (colorWrite) {
                                            if (t5 != 255) {
                                                t11 = t2 * (t10 and 0xff0000) and 0xff000000.toInt()
                                                t12 = t3 * (t10 and 0xff00) and 0xff0000
                                                t13 = (t10 and 0xff) * t4 and 0xff00
                                                t10 = (t11 or t12 or t13) ushr 8
                                                t14 = screenPixels!![dstIndex]
                                                screenPixels[dstIndex] = ((t10 and 0xff00ff) * t5 + (t14 and 0xff00ff) * t6 and -16711936) + (t5 * (t10 and 0xff00) + t6 * (t14 and 0xff00) and 0xff0000) shr 8
                                            } else {
                                                t11 = t2 * (t10 and 0xff0000) and 0xff000000.toInt()
                                                t12 = t3 * (t10 and 0xff00) and 0xff0000
                                                t13 = (t10 and 0xff) * t4 and 0xff00
                                                screenPixels!![dstIndex] = (t11 or t12 or t13) ushr 8
                                            }
                                        }

                                        if (depthTest && depthWrite) {
                                            depthBuffer!![dstIndex] = depth
                                        }
                                    }

                                    ++dstIndex
                                    u += du
                                    t9++
                                }

                                v += dv
                                u = t1
                                dstIndex += dstStep
                                t7++
                            }
                        }
                    } else if (combineMode == 3) {
                        t1 = u
                        t2 = color ushr 24
                        t3 = 256 - t2

                        t4 = -h
                        while (t4 < 0) {
                            t5 = imageWidth * (v shr 16)

                            t6 = -w
                            while (t6 < 0) {
                                t7 = this.pixels[t5 + (u shr 16)]
                                t8 = t7 + color
                                t9 = (t7 and 0xff00ff) + (color and 0xff00ff)
                                t10 = (t9 and 0x1000100) + (t8 - t9 and 0x10000)
                                t10 = t8 - t10 or (t10 - (t10 ushr 8))
                                if (t7 == 0 && t2 != 255) {
                                    if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                        if (colorWrite) {
                                            t7 = screenPixels!![dstIndex]
                                            screenPixels[dstIndex] = ((t10 and 0xff00ff) * t2 + (t7 and 0xff00ff) * t3 and -16711936) + (t2 * (t10 and 0xff00) + t3 * (t7 and 0xff00) and 0xff0000) shr 8
                                        }

                                        if (depthTest && depthWrite) {
                                            depthBuffer!![dstIndex] = depth
                                        }
                                    }
                                } else if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                    if (colorWrite) {
                                        screenPixels!![dstIndex] = t10
                                    }

                                    if (depthTest && depthWrite) {
                                        depthBuffer!![dstIndex] = depth
                                    }
                                }

                                ++dstIndex
                                u += du
                                t6++
                            }

                            v += dv
                            u = t1
                            dstIndex += dstStep
                            t4++
                        }
                    } else {
                        if (combineMode != 2) {
                            throw IllegalArgumentException()
                        }

                        t1 = color ushr 24
                        t2 = 256 - t1
                        t3 = (color and 0xff00ff) * t2 and -16711936
                        t4 = t2 * (color and 0xff00) and 0xff0000
                        color = (t3 or t4) ushr 8
                        t5 = u

                        t6 = -h
                        while (t6 < 0) {
                            t7 = imageWidth * (v shr 16)

                            t8 = -w
                            while (t8 < 0) {
                                t9 = this.pixels[t7 + (u shr 16)]
                                if (t9 != 0 && (!depthTest || depth < depthBuffer!![dstIndex])) {
                                    if (colorWrite) {
                                        t3 = (t9 and 0xff00ff) * t1 and -16711936
                                        t4 = t1 * (t9 and 0xff00) and 0xff0000
                                        screenPixels!![dstIndex] = ((t3 or t4) ushr 8) + color
                                    }

                                    if (depthTest && depthWrite) {
                                        depthBuffer!![dstIndex] = depth
                                    }
                                }

                                ++dstIndex
                                u += du
                                t8++
                            }

                            v += dv
                            u = t5
                            dstIndex += dstStep
                            t6++
                        }
                    }
                } else {
                    if (blendMode != 2) {
                        throw IllegalArgumentException()
                    }

                    if (combineMode == 1) {
                        t1 = u

                        t2 = -h
                        while (t2 < 0) {
                            t3 = imageWidth * (v shr 16)

                            t4 = -w
                            while (t4 < 0) {
                                t5 = this.pixels[t3 + (u shr 16)]
                                if (t5 != 0 && (!depthTest || depth < depthBuffer!![dstIndex])) {
                                    if (colorWrite) {
                                        t6 = screenPixels!![dstIndex]
                                        t7 = t5 + t6
                                        t8 = (t5 and 0xff00ff) + (t6 and 0xff00ff)
                                        t6 = (t8 and 0x1000100) + (t7 - t8 and 0x10000)
                                        screenPixels[dstIndex] = t7 - t6 or (t6 - (t6 ushr 8))
                                    }

                                    if (depthTest && depthWrite) {
                                        depthBuffer!![dstIndex] = depth
                                    }
                                }

                                ++dstIndex
                                u += du
                                t4++
                            }

                            v += dv
                            u = t1
                            dstIndex += dstStep
                            t2++
                        }
                    } else if (combineMode == 0) {
                        t1 = u
                        t2 = (color and 0xff0000) shr 16
                        t3 = (color and 0xff00) shr 8
                        t4 = color and 0xff

                        t5 = -h
                        while (t5 < 0) {
                            t6 = imageWidth * (v shr 16)

                            t7 = -w
                            while (t7 < 0) {
                                t8 = this.pixels[t6 + (u shr 16)]
                                if (t8 != 0 && (!depthTest || depth < depthBuffer!![dstIndex])) {
                                    if (colorWrite) {
                                        t9 = t2 * (t8 and 0xff0000) and 0xff000000.toInt()
                                        t10 = t3 * (t8 and 0xff00) and 0xff0000
                                        t11 = (t8 and 0xff) * t4 and 0xff00
                                        t8 = (t9 or t10 or t11) ushr 8
                                        t12 = screenPixels!![dstIndex]
                                        t13 = t8 + t12
                                        t14 = (t8 and 0xff00ff) + (t12 and 0xff00ff)
                                        t12 = (t14 and 0x1000100) + (t13 - t14 and 0x10000)
                                        screenPixels[dstIndex] = t13 - t12 or (t12 - (t12 ushr 8))
                                    }

                                    if (depthTest && depthWrite) {
                                        depthBuffer!![dstIndex] = depth
                                    }
                                }

                                ++dstIndex
                                u += du
                                t7++
                            }

                            v += dv
                            u = t1
                            dstIndex += dstStep
                            t5++
                        }
                    } else if (combineMode == 3) {
                        t1 = u

                        t2 = -h
                        while (t2 < 0) {
                            t3 = imageWidth * (v shr 16)

                            t4 = -w
                            while (t4 < 0) {
                                t5 = this.pixels[t3 + (u shr 16)]
                                if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                    if (colorWrite) {
                                        t6 = t5 + color
                                        t7 = (t5 and 0xff00ff) + (color and 0xff00ff)
                                        t8 = (t7 and 0x1000100) + (t6 - t7 and 0x10000)
                                        t5 = t6 - t8 or (t8 - (t8 ushr 8))
                                        t8 = screenPixels!![dstIndex]
                                        t6 = t5 + t8
                                        t7 = (t5 and 0xff00ff) + (t8 and 0xff00ff)
                                        t8 = (t7 and 0x1000100) + (t6 - t7 and 0x10000)
                                        screenPixels[dstIndex] = t6 - t8 or (t8 - (t8 ushr 8))
                                    }

                                    if (depthTest && depthWrite) {
                                        depthBuffer!![dstIndex] = depth
                                    }
                                }

                                ++dstIndex
                                u += du
                                t4++
                            }

                            v += dv
                            u = t1
                            dstIndex += dstStep
                            t2++
                        }
                    } else {
                        if (combineMode != 2) {
                            throw IllegalArgumentException()
                        }

                        t1 = color ushr 24
                        t2 = 256 - t1
                        t3 = (color and 0xff00ff) * t2 and -16711936
                        t4 = t2 * (color and 0xff00) and 0xff0000
                        color = (t3 or t4) ushr 8
                        t5 = u

                        t6 = -h
                        while (t6 < 0) {
                            t7 = imageWidth * (v shr 16)

                            t8 = -w
                            while (t8 < 0) {
                                t9 = this.pixels[t7 + (u shr 16)]
                                if (t9 != 0 && (!depthTest || depth < depthBuffer!![dstIndex])) {
                                    if (colorWrite) {
                                        t3 = (t9 and 0xff00ff) * t1 and -16711936
                                        t4 = t1 * (t9 and 0xff00) and 0xff0000
                                        t9 = ((t3 or t4) ushr 8) + color
                                        t10 = screenPixels!![dstIndex]
                                        t11 = t9 + t10
                                        t12 = (t9 and 0xff00ff) + (t10 and 0xff00ff)
                                        t10 = (t12 and 0x1000100) + (t11 - t12 and 0x10000)
                                        screenPixels[dstIndex] = t11 - t10 or (t10 - (t10 ushr 8))
                                    }

                                    if (depthTest && depthWrite) {
                                        depthBuffer!![dstIndex] = depth
                                    }
                                }

                                ++dstIndex
                                u += du
                                t8++
                            }

                            v += dv
                            u = t5
                            dstIndex += dstStep
                            t6++
                        }
                    }
                }
            }
        }
    }

    override fun drawAffineTransformed(combineMode: Int, blendMode: Int) {
        val screenPixels = toolkit.colourBuffer ?: return
        var row: Int
        var dstIndex: Int
        var u: Int
        var v: Int
        var col: Int
        var t1: Int
        var t2: Int
        var t3: Int
        if (dudx == 0) {
            if (dvdx == 0) {
                row = drawHeight
                while (row < 0) {
                    dstIndex = pixelOffset
                    u = uBase
                    v = vBase
                    col = drawWidth
                    if (u >= 0 && v >= 0 && u - (imageWidth shl 12) < 0 && v - (imageHeight shl 12) < 0) {
                        while (col < 0) {
                            t1 = (u shr 12) + (v shr 12) * imageWidth
                            t2 = dstIndex++
                            blendPixel(screenPixels, t2, t1, combineMode, blendMode)
                            col++
                        }
                    }
                    pixelOffset += viewportStride
                    row++
                }
            } else if (dvdx < 0) {
                row = drawHeight
                while (row < 0) {
                    dstIndex = pixelOffset
                    u = uBase
                    v = vBase + vOffsetX
                    col = drawWidth
                    if (u >= 0 && u - (imageWidth shl 12) < 0) {
                        t1 = v - (imageHeight shl 12)
                        if (t1 >= 0) {
                            t1 = (dvdx - t1) / dvdx
                            col += t1
                            v += t1 * dvdx
                            dstIndex += t1
                        }
                        t1 = (v - dvdx) / dvdx
                        if (t1 > col) {
                            col = t1
                        }
                        while (col < 0) {
                            t2 = (u shr 12) + (v shr 12) * imageWidth
                            t3 = dstIndex++
                            blendPixel(screenPixels, t3, t2, combineMode, blendMode)
                            v += dvdx
                            col++
                        }
                    }
                    pixelOffset += viewportStride
                    row++
                    uBase += dudy
                }
            } else {
                row = drawHeight
                while (row < 0) {
                    dstIndex = pixelOffset
                    u = uBase
                    v = vBase + vOffsetX
                    col = drawWidth
                    if (u >= 0 && u - (imageWidth shl 12) < 0) {
                        if (v < 0) {
                            t1 = (dvdx - 1 - v) / dvdx
                            col += t1
                            v += t1 * dvdx
                            dstIndex += t1
                        }
                        t1 = (v + 1 - (imageHeight shl 12) - dvdx) / dvdx
                        if (t1 > col) {
                            col = t1
                        }
                        while (col < 0) {
                            t2 = (u shr 12) + (v shr 12) * imageWidth
                            t3 = dstIndex++
                            blendPixel(screenPixels, t3, t2, combineMode, blendMode)
                            v += dvdx
                            col++
                        }
                    }
                    pixelOffset += viewportStride
                    row++
                    uBase += dudy
                }
            }
        } else if (dudx < 0) {
            drawScaledRowNegX(screenPixels, combineMode, blendMode)
        } else {
            drawScaledRowPosX(screenPixels, combineMode, blendMode)
        }
    }

    private fun drawScaledRowNegX(screenPixels: IntArray, combineMode: Int, blendMode: Int) {
        var row: Int
        var dstIndex: Int
        var u: Int
        var v: Int
        var col: Int
        var steps: Int
        var srcIndex: Int
        var writeIndex: Int
        if (dvdx == 0) {
            row = drawHeight
            while (row < 0) {
                dstIndex = pixelOffset
                u = uBase + uOffsetX
                v = vBase
                col = drawWidth
                if (v >= 0 && v - (imageHeight shl 12) < 0) {
                    steps = u - (imageWidth shl 12)
                    if (steps >= 0) {
                        steps = (dudx - steps) / dudx
                        col += steps
                        u += steps * dudx
                        dstIndex += steps
                    }
                    steps = (u - dudx) / dudx
                    if (steps > col) {
                        col = steps
                    }
                    while (col < 0) {
                        srcIndex = (u shr 12) + (v shr 12) * imageWidth
                        writeIndex = dstIndex++
                        blendPixel(screenPixels, writeIndex, srcIndex, combineMode, blendMode)
                        u += dudx
                        col++
                    }
                }
                pixelOffset += viewportStride
                row++
                vBase += dvdy
            }
        } else if (dvdx < 0) {
            row = drawHeight
            while (row < 0) {
                dstIndex = pixelOffset
                u = uBase + uOffsetX
                v = vBase + vOffsetX
                col = drawWidth
                steps = u - (imageWidth shl 12)
                if (steps >= 0) {
                    steps = (dudx - steps) / dudx
                    col += steps
                    u += steps * dudx
                    v += steps * dvdx
                    dstIndex += steps
                }
                steps = (u - dudx) / dudx
                if (steps > col) {
                    col = steps
                }
                steps = v - (imageHeight shl 12)
                if (steps >= 0) {
                    steps = (dvdx - steps) / dvdx
                    col += steps
                    u += steps * dudx
                    v += steps * dvdx
                    dstIndex += steps
                }
                steps = (v - dvdx) / dvdx
                if (steps > col) {
                    col = steps
                }
                while (col < 0) {
                    srcIndex = (u shr 12) + (v shr 12) * imageWidth
                    writeIndex = dstIndex++
                    blendPixel(screenPixels, writeIndex, srcIndex, combineMode, blendMode)
                    u += dudx
                    v += dvdx
                    col++
                }
                pixelOffset += viewportStride
                row++
                uBase += dudy
                vBase += dvdy
            }
        } else {
            row = drawHeight
            while (row < 0) {
                dstIndex = pixelOffset
                u = uBase + uOffsetX
                v = vBase + vOffsetX
                col = drawWidth
                steps = u - (imageWidth shl 12)
                if (steps >= 0) {
                    steps = (dudx - steps) / dudx
                    col += steps
                    u += steps * dudx
                    v += steps * dvdx
                    dstIndex += steps
                }
                steps = (u - dudx) / dudx
                if (steps > col) {
                    col = steps
                }
                if (v < 0) {
                    steps = (dvdx - 1 - v) / dvdx
                    col += steps
                    u += steps * dudx
                    v += steps * dvdx
                    dstIndex += steps
                }
                steps = (v + 1 - (imageHeight shl 12) - dvdx) / dvdx
                if (steps > col) {
                    col = steps
                }
                while (col < 0) {
                    srcIndex = (u shr 12) + (v shr 12) * imageWidth
                    writeIndex = dstIndex++
                    blendPixel(screenPixels, writeIndex, srcIndex, combineMode, blendMode)
                    u += dudx
                    v += dvdx
                    col++
                }
                pixelOffset += viewportStride
                row++
                uBase += dudy
                vBase += dvdy
            }
        }
    }

    private fun drawScaledRowPosX(screenPixels: IntArray, combineMode: Int, blendMode: Int) {
        var row: Int
        var dstIndex: Int
        var u: Int
        var v: Int
        var col: Int
        var steps: Int
        var srcIndex: Int
        var writeIndex: Int
        if (dvdx == 0) {
            row = drawHeight
            while (row < 0) {
                dstIndex = pixelOffset
                u = uBase + uOffsetX
                v = vBase
                col = drawWidth
                if (v >= 0 && v - (imageHeight shl 12) < 0) {
                    if (u < 0) {
                        steps = (dudx - 1 - u) / dudx
                        col += steps
                        u += steps * dudx
                        dstIndex += steps
                    }
                    steps = (u + 1 - (imageWidth shl 12) - dudx) / dudx
                    if (steps > col) {
                        col = steps
                    }
                    while (col < 0) {
                        srcIndex = (u shr 12) + (v shr 12) * imageWidth
                        writeIndex = dstIndex++
                        blendPixel(screenPixels, writeIndex, srcIndex, combineMode, blendMode)
                        u += dudx
                        col++
                    }
                }
                pixelOffset += viewportStride
                row++
                vBase += dvdy
            }
        } else if (dvdx < 0) {
            row = drawHeight
            while (row < 0) {
                dstIndex = pixelOffset
                u = uBase + uOffsetX
                v = vBase + vOffsetX
                col = drawWidth
                if (u < 0) {
                    steps = (dudx - 1 - u) / dudx
                    col += steps
                    u += steps * dudx
                    v += steps * dvdx
                    dstIndex += steps
                }
                steps = (u + 1 - (imageWidth shl 12) - dudx) / dudx
                if (steps > col) {
                    col = steps
                }
                steps = v - (imageHeight shl 12)
                if (steps >= 0) {
                    steps = (dvdx - steps) / dvdx
                    col += steps
                    u += steps * dudx
                    v += steps * dvdx
                    dstIndex += steps
                }
                steps = (v - dvdx) / dvdx
                if (steps > col) {
                    col = steps
                }
                while (col < 0) {
                    srcIndex = (u shr 12) + (v shr 12) * imageWidth
                    writeIndex = dstIndex++
                    blendPixel(screenPixels, writeIndex, srcIndex, combineMode, blendMode)
                    u += dudx
                    v += dvdx
                    col++
                }
                pixelOffset += viewportStride
                row++
                uBase += dudy
                vBase += dvdy
            }
        } else {
            row = drawHeight
            while (row < 0) {
                dstIndex = pixelOffset
                u = uBase + uOffsetX
                v = vBase + vOffsetX
                col = drawWidth
                if (u < 0) {
                    steps = (dudx - 1 - u) / dudx
                    col += steps
                    u += steps * dudx
                    v += steps * dvdx
                    dstIndex += steps
                }
                steps = (u + 1 - (imageWidth shl 12) - dudx) / dudx
                if (steps > col) {
                    col = steps
                }
                if (v < 0) {
                    steps = (dvdx - 1 - v) / dvdx
                    col += steps
                    u += steps * dudx
                    v += steps * dvdx
                    dstIndex += steps
                }
                steps = (v + 1 - (imageHeight shl 12) - dvdx) / dvdx
                if (steps > col) {
                    col = steps
                }
                while (col < 0) {
                    srcIndex = (u shr 12) + (v shr 12) * imageWidth
                    writeIndex = dstIndex++
                    blendPixel(screenPixels, writeIndex, srcIndex, combineMode, blendMode)
                    u += dudx
                    v += dvdx
                    col++
                }
                pixelOffset += viewportStride
                row++
                uBase += dudy
                vBase += dvdy
            }
        }
    }

    private fun blendPixel(screenPixels: IntArray, dstIndex: Int, srcIndex: Int, combineMode: Int, blendMode: Int) {
        var srcPixel: Int
        var t1: Int
        var t2: Int
        var t3: Int
        var t4: Int
        var t5: Int
        var t6: Int
        var t7: Int
        if (blendMode == 0) {
            if (combineMode == 1) {
                screenPixels[dstIndex] = this.pixels[srcIndex]
            } else if (combineMode == 0) {
                srcPixel = this.pixels[srcIndex]
                t1 = colorRed * (srcPixel and 0xff0000) and 0xff000000.toInt()
                t2 = (srcPixel and 0xff00) * colorGreen and 0xff0000
                t3 = (srcPixel and 0xff) * colorBlue and 0xff00
                screenPixels[dstIndex] = (t1 or t2 or t3) ushr 8
            } else if (combineMode == 3) {
                srcPixel = this.pixels[srcIndex]
                t1 = blendColor
                t2 = srcPixel + t1
                t3 = (srcPixel and 0xff00ff) + (t1 and 0xff00ff)
                t4 = (t3 and 0x1000100) + (t2 - t3 and 0x10000)
                screenPixels[dstIndex] = t2 - t4 or (t4 - (t4 ushr 8))
            } else {
                if (combineMode != 2) {
                    throw IllegalArgumentException()
                }
                srcPixel = this.pixels[srcIndex]
                t1 = (srcPixel and 0xff00ff) * colorAlpha and -16711936
                t2 = (srcPixel and 0xff00) * colorAlpha and 0xff0000
                screenPixels[dstIndex] = ((t1 or t2) ushr 8) + overlayPrecomputed
            }
        } else if (blendMode == 1) {
            if (combineMode == 1) {
                srcPixel = this.pixels[srcIndex]
                if (srcPixel != 0) {
                    screenPixels[dstIndex] = srcPixel
                }
            } else if (combineMode == 0) {
                srcPixel = this.pixels[srcIndex]
                if (srcPixel != 0) {
                    if ((blendColor and 0xffffff) == 16777215) {
                        t1 = blendColor ushr 24
                        t2 = 256 - t1
                        t3 = screenPixels[dstIndex]
                        screenPixels[dstIndex] = ((srcPixel and 0xff00ff) * t1 + (t3 and 0xff00ff) * t2 and -16711936) + (t1 * (srcPixel and 0xff00) + t2 * (t3 and 0xff00) and 0xff0000) shr 8
                    } else if (colorAlpha != 255) {
                        t1 = colorRed * (srcPixel and 0xff0000) and 0xff000000.toInt()
                        t2 = (srcPixel and 0xff00) * colorGreen and 0xff0000
                        t3 = (srcPixel and 0xff) * colorBlue and 0xff00
                        srcPixel = (t1 or t2 or t3) ushr 8
                        t4 = screenPixels[dstIndex]
                        screenPixels[dstIndex] = ((srcPixel and 0xff00ff) * colorAlpha + (t4 and 0xff00ff) * colorInverseAlpha and -16711936) + ((srcPixel and 0xff00) * colorAlpha + (t4 and 0xff00) * colorInverseAlpha and 0xff0000) shr 8
                    } else {
                        t1 = colorRed * (srcPixel and 0xff0000) and 0xff000000.toInt()
                        t2 = (srcPixel and 0xff00) * colorGreen and 0xff0000
                        t3 = (srcPixel and 0xff) * colorBlue and 0xff00
                        screenPixels[dstIndex] = (t1 or t2 or t3) ushr 8
                    }
                }
            } else if (combineMode == 3) {
                srcPixel = this.pixels[srcIndex]
                t1 = blendColor
                t2 = srcPixel + t1
                t3 = (srcPixel and 0xff00ff) + (t1 and 0xff00ff)
                t4 = (t3 and 0x1000100) + (t2 - t3 and 0x10000)
                t4 = t2 - t4 or (t4 - (t4 ushr 8))
                if (srcPixel == 0 && colorAlpha != 255) {
                    srcPixel = t4
                    t4 = screenPixels[dstIndex]
                    t4 = ((srcPixel and 0xff00ff) * colorAlpha + (t4 and 0xff00ff) * colorInverseAlpha and -16711936) + ((srcPixel and 0xff00) * colorAlpha + (t4 and 0xff00) * colorInverseAlpha and 0xff0000) shr 8
                }
                screenPixels[dstIndex] = t4
            } else {
                if (combineMode != 2) {
                    throw IllegalArgumentException()
                }
                srcPixel = this.pixels[srcIndex]
                if (srcPixel != 0) {
                    t1 = (srcPixel and 0xff00ff) * colorAlpha and -16711936
                    t2 = (srcPixel and 0xff00) * colorAlpha and 0xff0000
                    screenPixels[dstIndex] = ((t1 or t2) ushr 8) + overlayPrecomputed
                }
            }
        } else {
            if (blendMode != 2) {
                throw IllegalArgumentException()
            }
            if (combineMode == 1) {
                srcPixel = this.pixels[srcIndex]
                if (srcPixel != 0) {
                    t1 = screenPixels[dstIndex]
                    t2 = srcPixel + t1
                    t3 = (srcPixel and 0xff00ff) + (t1 and 0xff00ff)
                    t1 = (t3 and 0x1000100) + (t2 - t3 and 0x10000)
                    screenPixels[dstIndex] = t2 - t1 or (t1 - (t1 ushr 8))
                }
            } else if (combineMode == 0) {
                srcPixel = this.pixels[srcIndex]
                if (srcPixel != 0) {
                    t1 = colorRed * (srcPixel and 0xff0000) and 0xff000000.toInt()
                    t2 = (srcPixel and 0xff00) * colorGreen and 0xff0000
                    t3 = (srcPixel and 0xff) * colorBlue and 0xff00
                    srcPixel = (t1 or t2 or t3) ushr 8
                    t4 = screenPixels[dstIndex]
                    t5 = srcPixel + t4
                    t6 = (srcPixel and 0xff00ff) + (t4 and 0xff00ff)
                    t4 = (t6 and 0x1000100) + (t5 - t6 and 0x10000)
                    screenPixels[dstIndex] = t5 - t4 or (t4 - (t4 ushr 8))
                }
            } else if (combineMode == 3) {
                srcPixel = this.pixels[srcIndex]
                t1 = blendColor
                t2 = srcPixel + t1
                t3 = (srcPixel and 0xff00ff) + (t1 and 0xff00ff)
                t4 = (t3 and 0x1000100) + (t2 - t3 and 0x10000)
                srcPixel = t2 - t4 or (t4 - (t4 ushr 8))
                t4 = screenPixels[dstIndex]
                t2 = srcPixel + t4
                t3 = (srcPixel and 0xff00ff) + (t4 and 0xff00ff)
                t4 = (t3 and 0x1000100) + (t2 - t3 and 0x10000)
                screenPixels[dstIndex] = t2 - t4 or (t4 - (t4 ushr 8))
            } else if (combineMode == 2) {
                srcPixel = this.pixels[srcIndex]
                if (srcPixel != 0) {
                    t1 = (srcPixel and 0xff00ff) * colorAlpha and -16711936
                    t2 = (srcPixel and 0xff00) * colorAlpha and 0xff0000
                    srcPixel = ((t1 or t2) ushr 8) + overlayPrecomputed
                    t3 = screenPixels[dstIndex]
                    t4 = srcPixel + t3
                    t5 = (srcPixel and 0xff00ff) + (t3 and 0xff00ff)
                    t3 = (t5 and 0x1000100) + (t4 - t5 and 0x10000)
                    screenPixels[dstIndex] = t4 - t3 or (t3 - (t3 ushr 8))
                }
            }
        }
    }

    override fun drawMasked(rowStartX: IntArray, rowWidth: IntArray, maskX: Int, maskY: Int) {
        val screenPixels = toolkit.colourBuffer ?: return
        var row: Int
        var maskRow: Int
        var dstIndex: Int
        var u: Int
        var v: Int
        var col: Int
        var t1: Int
        var t2: Int
        var t3: Int
        var t4: Int
        var srcPixel: Int
        if (dudx == 0) {
            if (dvdx == 0) {
                row = drawHeight
                while (row < 0) {
                    maskRow = row + maskY
                    if (maskRow >= 0) {
                        if (maskRow >= rowStartX.size) {
                            break
                        }
                        dstIndex = pixelOffset
                        u = uBase
                        v = vBase
                        col = drawWidth
                        if (u >= 0 && v >= 0 && u - (imageWidth shl 12) < 0 && v - (imageHeight shl 12) < 0) {
                            t1 = rowStartX[maskRow] - maskX
                            t2 = -rowWidth[maskRow]
                            t3 = t1 - (dstIndex - pixelOffset)
                            if (t3 > 0) {
                                dstIndex += t3
                                col += t3
                                u += t3 * dudx
                                v += t3 * dvdx
                            } else {
                                t2 -= t3
                            }
                            if (col < t2) {
                                col = t2
                            }
                            while (col < 0) {
                                t4 = this.pixels[(u shr 12) + (v shr 12) * imageWidth]
                                if (t4 != 0) {
                                    screenPixels[dstIndex++] = t4
                                } else {
                                    ++dstIndex
                                }
                                col++
                            }
                        }
                    }
                    pixelOffset += viewportStride
                    row++
                }
            } else if (dvdx < 0) {
                row = drawHeight
                while (row < 0) {
                    maskRow = row + maskY
                    if (maskRow >= 0) {
                        if (maskRow >= rowStartX.size) {
                            break
                        }
                        dstIndex = pixelOffset
                        u = uBase
                        v = vBase + vOffsetX
                        col = drawWidth
                        if (u >= 0 && u - (imageWidth shl 12) < 0) {
                            t1 = v - (imageHeight shl 12)
                            if (t1 >= 0) {
                                t1 = (dvdx - t1) / dvdx
                                col += t1
                                v += t1 * dvdx
                                dstIndex += t1
                            }
                            t1 = (v - dvdx) / dvdx
                            if (t1 > col) {
                                col = t1
                            }
                            t2 = rowStartX[maskRow] - maskX
                            t3 = -rowWidth[maskRow]
                            t4 = t2 - (dstIndex - pixelOffset)
                            if (t4 > 0) {
                                dstIndex += t4
                                col += t4
                                u += t4 * dudx
                                v += t4 * dvdx
                            } else {
                                t3 -= t4
                            }
                            if (col < t3) {
                                col = t3
                            }
                            while (col < 0) {
                                srcPixel = this.pixels[(u shr 12) + (v shr 12) * imageWidth]
                                if (srcPixel != 0) {
                                    screenPixels[dstIndex++] = srcPixel
                                } else {
                                    ++dstIndex
                                }
                                v += dvdx
                                col++
                            }
                        }
                    }
                    pixelOffset += viewportStride
                    row++
                    uBase += dudy
                }
            } else {
                row = drawHeight
                while (row < 0) {
                    maskRow = row + maskY
                    if (maskRow >= 0) {
                        if (maskRow >= rowStartX.size) {
                            break
                        }
                        dstIndex = pixelOffset
                        u = uBase
                        v = vBase + vOffsetX
                        col = drawWidth
                        if (u >= 0 && u - (imageWidth shl 12) < 0) {
                            if (v < 0) {
                                t1 = (dvdx - 1 - v) / dvdx
                                col += t1
                                v += t1 * dvdx
                                dstIndex += t1
                            }
                            t1 = (v + 1 - (imageHeight shl 12) - dvdx) / dvdx
                            if (t1 > col) {
                                col = t1
                            }
                            t2 = rowStartX[maskRow] - maskX
                            t3 = -rowWidth[maskRow]
                            t4 = t2 - (dstIndex - pixelOffset)
                            if (t4 > 0) {
                                dstIndex += t4
                                col += t4
                                u += t4 * dudx
                                v += t4 * dvdx
                            } else {
                                t3 -= t4
                            }
                            if (col < t3) {
                                col = t3
                            }
                            while (col < 0) {
                                srcPixel = this.pixels[(u shr 12) + (v shr 12) * imageWidth]
                                if (srcPixel != 0) {
                                    screenPixels[dstIndex++] = srcPixel
                                } else {
                                    ++dstIndex
                                }
                                v += dvdx
                                col++
                            }
                        }
                    }
                    pixelOffset += viewportStride
                    row++
                    uBase += dudy
                }
            }
        } else if (dudx < 0) {
            drawMaskedScaledRowNegX(screenPixels, rowStartX, rowWidth, maskX, maskY)
        } else {
            drawMaskedScaledRowPosX(screenPixels, rowStartX, rowWidth, maskX, maskY)
        }
    }

    private fun drawMaskedScaledRowNegX(screenPixels: IntArray, rowStartX: IntArray, rowWidth: IntArray, maskX: Int, maskY: Int) {
        var row: Int
        var maskRow: Int
        var dstIndex: Int
        var u: Int
        var v: Int
        var col: Int
        var steps: Int
        var maskRowX: Int
        var maskSpan: Int
        var skip: Int
        var srcPixel: Int
        if (dvdx == 0) {
            row = drawHeight
            while (row < 0) {
                maskRow = row + maskY
                if (maskRow >= 0) {
                    if (maskRow >= rowStartX.size) {
                        break
                    }
                    dstIndex = pixelOffset
                    u = uBase + uOffsetX
                    v = vBase
                    col = drawWidth
                    if (v >= 0 && v - (imageHeight shl 12) < 0) {
                        steps = u - (imageWidth shl 12)
                        if (steps >= 0) {
                            steps = (dudx - steps) / dudx
                            col += steps
                            u += steps * dudx
                            dstIndex += steps
                        }
                        steps = (u - dudx) / dudx
                        if (steps > col) {
                            col = steps
                        }
                        maskRowX = rowStartX[maskRow] - maskX
                        maskSpan = -rowWidth[maskRow]
                        skip = maskRowX - (dstIndex - pixelOffset)
                        if (skip > 0) {
                            dstIndex += skip
                            col += skip
                            u += skip * dudx
                            v += skip * dvdx
                        } else {
                            maskSpan -= skip
                        }
                        if (col < maskSpan) {
                            col = maskSpan
                        }
                        while (col < 0) {
                            srcPixel = this.pixels[(u shr 12) + (v shr 12) * imageWidth]
                            if (srcPixel != 0) {
                                screenPixels[dstIndex++] = srcPixel
                            } else {
                                ++dstIndex
                            }
                            u += dudx
                            col++
                        }
                    }
                }
                pixelOffset += viewportStride
                row++
                vBase += dvdy
            }
        } else if (dvdx < 0) {
            row = drawHeight
            while (row < 0) {
                maskRow = row + maskY
                if (maskRow >= 0) {
                    if (maskRow >= rowStartX.size) {
                        break
                    }
                    dstIndex = pixelOffset
                    u = uBase + uOffsetX
                    v = vBase + vOffsetX
                    col = drawWidth
                    steps = u - (imageWidth shl 12)
                    if (steps >= 0) {
                        steps = (dudx - steps) / dudx
                        col += steps
                        u += steps * dudx
                        v += steps * dvdx
                        dstIndex += steps
                    }
                    steps = (u - dudx) / dudx
                    if (steps > col) {
                        col = steps
                    }
                    steps = v - (imageHeight shl 12)
                    if (steps >= 0) {
                        steps = (dvdx - steps) / dvdx
                        col += steps
                        u += steps * dudx
                        v += steps * dvdx
                        dstIndex += steps
                    }
                    steps = (v - dvdx) / dvdx
                    if (steps > col) {
                        col = steps
                    }
                    maskRowX = rowStartX[maskRow] - maskX
                    maskSpan = -rowWidth[maskRow]
                    skip = maskRowX - (dstIndex - pixelOffset)
                    if (skip > 0) {
                        dstIndex += skip
                        col += skip
                        u += skip * dudx
                        v += skip * dvdx
                    } else {
                        maskSpan -= skip
                    }
                    if (col < maskSpan) {
                        col = maskSpan
                    }
                    while (col < 0) {
                        srcPixel = this.pixels[(u shr 12) + (v shr 12) * imageWidth]
                        if (srcPixel != 0) {
                            screenPixels[dstIndex++] = srcPixel
                        } else {
                            ++dstIndex
                        }
                        u += dudx
                        v += dvdx
                        col++
                    }
                }
                pixelOffset += viewportStride
                row++
                uBase += dudy
                vBase += dvdy
            }
        } else {
            row = drawHeight
            while (row < 0) {
                maskRow = row + maskY
                if (maskRow >= 0) {
                    if (maskRow >= rowStartX.size) {
                        break
                    }
                    dstIndex = pixelOffset
                    u = uBase + uOffsetX
                    v = vBase + vOffsetX
                    col = drawWidth
                    steps = u - (imageWidth shl 12)
                    if (steps >= 0) {
                        steps = (dudx - steps) / dudx
                        col += steps
                        u += steps * dudx
                        v += steps * dvdx
                        dstIndex += steps
                    }
                    steps = (u - dudx) / dudx
                    if (steps > col) {
                        col = steps
                    }
                    if (v < 0) {
                        steps = (dvdx - 1 - v) / dvdx
                        col += steps
                        u += steps * dudx
                        v += steps * dvdx
                        dstIndex += steps
                    }
                    steps = (v + 1 - (imageHeight shl 12) - dvdx) / dvdx
                    if (steps > col) {
                        col = steps
                    }
                    maskRowX = rowStartX[maskRow] - maskX
                    maskSpan = -rowWidth[maskRow]
                    skip = maskRowX - (dstIndex - pixelOffset)
                    if (skip > 0) {
                        dstIndex += skip
                        col += skip
                        u += skip * dudx
                        v += skip * dvdx
                    } else {
                        maskSpan -= skip
                    }
                    if (col < maskSpan) {
                        col = maskSpan
                    }
                    while (col < 0) {
                        srcPixel = this.pixels[(u shr 12) + (v shr 12) * imageWidth]
                        if (srcPixel != 0) {
                            screenPixels[dstIndex++] = srcPixel
                        } else {
                            ++dstIndex
                        }
                        u += dudx
                        v += dvdx
                        col++
                    }
                }
                pixelOffset += viewportStride
                row++
                uBase += dudy
                vBase += dvdy
            }
        }
    }

    private fun drawMaskedScaledRowPosX(screenPixels: IntArray, rowStartX: IntArray, rowWidth: IntArray, maskX: Int, maskY: Int) {
        var row: Int
        var maskRow: Int
        var dstIndex: Int
        var u: Int
        var v: Int
        var col: Int
        var steps: Int
        var maskRowX: Int
        var maskSpan: Int
        var skip: Int
        var srcPixel: Int
        if (dvdx == 0) {
            row = drawHeight
            while (row < 0) {
                maskRow = row + maskY
                if (maskRow >= 0) {
                    if (maskRow >= rowStartX.size) {
                        break
                    }
                    dstIndex = pixelOffset
                    u = uBase + uOffsetX
                    v = vBase
                    col = drawWidth
                    if (v >= 0 && v - (imageHeight shl 12) < 0) {
                        if (u < 0) {
                            steps = (dudx - 1 - u) / dudx
                            col += steps
                            u += steps * dudx
                            dstIndex += steps
                        }
                        steps = (u + 1 - (imageWidth shl 12) - dudx) / dudx
                        if (steps > col) {
                            col = steps
                        }
                        maskRowX = rowStartX[maskRow] - maskX
                        maskSpan = -rowWidth[maskRow]
                        skip = maskRowX - (dstIndex - pixelOffset)
                        if (skip > 0) {
                            dstIndex += skip
                            col += skip
                            u += skip * dudx
                            v += skip * dvdx
                        } else {
                            maskSpan -= skip
                        }
                        if (col < maskSpan) {
                            col = maskSpan
                        }
                        while (col < 0) {
                            srcPixel = this.pixels[(u shr 12) + (v shr 12) * imageWidth]
                            if (srcPixel != 0) {
                                screenPixels[dstIndex++] = srcPixel
                            } else {
                                ++dstIndex
                            }
                            u += dudx
                            col++
                        }
                    }
                }
                pixelOffset += viewportStride
                row++
                uBase += dudy
                vBase += dvdy
            }
        } else if (dvdx < 0) {
            row = drawHeight
            while (row < 0) {
                maskRow = row + maskY
                if (maskRow >= 0) {
                    if (maskRow >= rowStartX.size) {
                        break
                    }
                    dstIndex = pixelOffset
                    u = uBase + uOffsetX
                    v = vBase + vOffsetX
                    col = drawWidth
                    if (u < 0) {
                        steps = (dudx - 1 - u) / dudx
                        col += steps
                        u += steps * dudx
                        v += steps * dvdx
                        dstIndex += steps
                    }
                    steps = (u + 1 - (imageWidth shl 12) - dudx) / dudx
                    if (steps > col) {
                        col = steps
                    }
                    steps = v - (imageHeight shl 12)
                    if (steps >= 0) {
                        steps = (dvdx - steps) / dvdx
                        col += steps
                        u += steps * dudx
                        v += steps * dvdx
                        dstIndex += steps
                    }
                    steps = (v - dvdx) / dvdx
                    if (steps > col) {
                        col = steps
                    }
                    maskRowX = rowStartX[maskRow] - maskX
                    maskSpan = -rowWidth[maskRow]
                    skip = maskRowX - (dstIndex - pixelOffset)
                    if (skip > 0) {
                        dstIndex += skip
                        col += skip
                        u += skip * dudx
                        v += skip * dvdx
                    } else {
                        maskSpan -= skip
                    }
                    if (col < maskSpan) {
                        col = maskSpan
                    }
                    while (col < 0) {
                        srcPixel = this.pixels[(u shr 12) + (v shr 12) * imageWidth]
                        if (srcPixel != 0) {
                            screenPixels[dstIndex++] = srcPixel
                        } else {
                            ++dstIndex
                        }
                        u += dudx
                        v += dvdx
                        col++
                    }
                }
                pixelOffset += viewportStride
                row++
                uBase += dudy
                vBase += dvdy
            }
        } else {
            row = drawHeight
            while (row < 0) {
                maskRow = row + maskY
                if (maskRow >= 0) {
                    if (maskRow >= rowStartX.size) {
                        break
                    }
                    dstIndex = pixelOffset
                    u = uBase + uOffsetX
                    v = vBase + vOffsetX
                    col = drawWidth
                    if (u < 0) {
                        steps = (dudx - 1 - u) / dudx
                        col += steps
                        u += steps * dudx
                        v += steps * dvdx
                        dstIndex += steps
                    }
                    steps = (u + 1 - (imageWidth shl 12) - dudx) / dudx
                    if (steps > col) {
                        col = steps
                    }
                    if (v < 0) {
                        steps = (dvdx - 1 - v) / dvdx
                        col += steps
                        u += steps * dudx
                        v += steps * dvdx
                        dstIndex += steps
                    }
                    steps = (v + 1 - (imageHeight shl 12) - dvdx) / dvdx
                    if (steps > col) {
                        col = steps
                    }
                    maskRowX = rowStartX[maskRow] - maskX
                    maskSpan = -rowWidth[maskRow]
                    skip = maskRowX - (dstIndex - pixelOffset)
                    if (skip > 0) {
                        dstIndex += skip
                        col += skip
                        u += skip * dudx
                        v += skip * dvdx
                    } else {
                        maskSpan -= skip
                    }
                    if (col < maskSpan) {
                        col = maskSpan
                    }
                    while (col < 0) {
                        srcPixel = this.pixels[(u shr 12) + (v shr 12) * imageWidth]
                        if (srcPixel != 0) {
                            screenPixels[dstIndex++] = srcPixel
                        } else {
                            ++dstIndex
                        }
                        u += dudx
                        v += dvdx
                        col++
                    }
                }
                pixelOffset += viewportStride
                row++
                uBase += dudy
                vBase += dvdy
            }
        }
    }

    override fun pixelTransfer() {
        throw IllegalStateException("")
    }
}
