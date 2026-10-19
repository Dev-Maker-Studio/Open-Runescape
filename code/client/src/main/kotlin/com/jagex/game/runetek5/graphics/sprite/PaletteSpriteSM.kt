package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.toolkit.ComponentDimensionSM

class PaletteSpriteSM(
    toolkit: SafeModeToolkit,
    @JvmField val spritePixels: ByteArray,
    @JvmField val spritePalette: IntArray,
    width: Int,
    height: Int
) : SpriteSM(toolkit, width, height) {

    override fun captureScreenRegion(dstX: Int, dstY: Int, width: Int, height: Int, srcX: Int, srcY: Int) {
        throw IllegalStateException()
    }

    override fun getFramebuffer(): FrameBuffer {
        throw IllegalStateException()
    }

    override fun pixelTransfer() {
        throw IllegalStateException()
    }

    override fun drawColoredCircle(x: Int, y: Int, width: Int, height: Int, combineMode: Int, colorArgb: Int, blendMode: Int) {
        var drawX = x
        var drawY = y
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

            dstIndex = viewportWidth * drawY + drawX
            var dstStep = viewportWidth - w
            if (drawY + h > toolkit.clipBottom) {
                h -= drawY + h - toolkit.clipBottom
            }

            var t1: Int
            if (drawY < toolkit.clipTop) {
                t1 = toolkit.clipTop - drawY
                h -= t1
                dstIndex += t1 * viewportWidth
                v += dv * t1
            }

            if (w + drawX > toolkit.clipRight) {
                t1 = w + drawX - toolkit.clipRight
                w -= t1
                dstStep += t1
            }

            if (drawX < toolkit.clipLeft) {
                t1 = toolkit.clipLeft - drawX
                w -= t1
                dstIndex += t1
                u += du * t1
                dstStep += t1
            }

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
            var paletteIndex: Byte
            if (blendMode == 0) {
                if (combineMode == 1) {
                    t1 = u

                    t2 = -h
                    while (t2 < 0) {
                        t3 = imageWidth * (v shr 16)

                        t4 = -w
                        while (t4 < 0) {
                            screenPixels[dstIndex++] = spritePalette[spritePixels[t3 + (u shr 16)].toInt() and 0xff]
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
                            t8 = spritePalette[spritePixels[t6 + (u shr 16)].toInt() and 0xff]
                            t9 = t1 * (t8 and 0xff0000) and 0xff000000.toInt()
                            t10 = t2 * (t8 and 0xff00) and 0xff0000
                            t11 = (t8 and 0xff) * t3 and 0xff00
                            screenPixels[dstIndex++] = (t9 or t10 or t11) ushr 8
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
                            paletteIndex = spritePixels[t3 + (u shr 16)]
                            t6 = if (paletteIndex > 0) spritePalette[paletteIndex.toInt()] else 0
                            t7 = t6 + color
                            t8 = (t6 and 0xff00ff) + (color and 0xff00ff)
                            t9 = (t8 and 0x1000100) + (t7 - t8 and 0x10000)
                            screenPixels[dstIndex++] = t7 - t9 or (t9 - (t9 ushr 8))
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
                            t9 = spritePalette[spritePixels[t7 + (u shr 16)].toInt() and 0xff]
                            val srcRb = (t9 and 0xff00ff) * t1 and -16711936
                            val srcG = t1 * (t9 and 0xff00) and 0xff0000
                            screenPixels[dstIndex++] = ((srcRb or srcG) ushr 8) + color
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
                var t15: Int
                var paletteIndex2: Byte
                if (blendMode == 1) {
                    if (combineMode == 1) {
                        t1 = u

                        t2 = -h
                        while (t2 < 0) {
                            t3 = imageWidth * (v shr 16)

                            t4 = -w
                            while (t4 < 0) {
                                paletteIndex = spritePixels[t3 + (u shr 16)]
                                if (paletteIndex.toInt() != 0) {
                                    screenPixels[dstIndex++] = spritePalette[paletteIndex.toInt() and 0xff]
                                } else {
                                    ++dstIndex
                                }

                                u += du
                                t4++
                            }

                            v += dv
                            u = t1
                            dstIndex += dstStep
                            t2++
                        }
                    } else {
                        var paletteIndex3: Byte
                        if (combineMode == 0) {
                            t1 = u
                            if ((color and 0xffffff) == 16777215) {
                                t2 = color ushr 24
                                t3 = 256 - t2

                                t4 = -h
                                while (t4 < 0) {
                                    t5 = imageWidth * (v shr 16)

                                    t6 = -w
                                    while (t6 < 0) {
                                        paletteIndex3 = spritePixels[t5 + (u shr 16)]
                                        if (paletteIndex3.toInt() != 0) {
                                            t8 = spritePalette[paletteIndex3.toInt() and 0xff]
                                            t9 = screenPixels[dstIndex]
                                            screenPixels[dstIndex++] = ((t8 and 0xff00ff) * t2 + (t9 and 0xff00ff) * t3 and -16711936) + (t2 * (t8 and 0xff00) + t3 * (t9 and 0xff00) and 0xff0000) shr 8
                                        } else {
                                            ++dstIndex
                                        }

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
                                        val paletteIndex4 = spritePixels[t8 + (u shr 16)]
                                        if (paletteIndex4.toInt() != 0) {
                                            t11 = spritePalette[paletteIndex4.toInt() and 0xff]
                                            if (t5 != 255) {
                                                t12 = t2 * (t11 and 0xff0000) and 0xff000000.toInt()
                                                t13 = t3 * (t11 and 0xff00) and 0xff0000
                                                t14 = (t11 and 0xff) * t4 and 0xff00
                                                t11 = (t12 or t13 or t14) ushr 8
                                                t15 = screenPixels[dstIndex]
                                                screenPixels[dstIndex++] = ((t11 and 0xff00ff) * t5 + (t15 and 0xff00ff) * t6 and -16711936) + (t5 * (t11 and 0xff00) + t6 * (t15 and 0xff00) and 0xff0000) shr 8
                                            } else {
                                                t12 = t2 * (t11 and 0xff0000) and 0xff000000.toInt()
                                                t13 = t3 * (t11 and 0xff00) and 0xff0000
                                                t14 = (t11 and 0xff) * t4 and 0xff00
                                                screenPixels[dstIndex++] = (t12 or t13 or t14) ushr 8
                                            }
                                        } else {
                                            ++dstIndex
                                        }

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
                                    paletteIndex3 = spritePixels[t5 + (u shr 16)]
                                    t8 = if (paletteIndex3 > 0) spritePalette[paletteIndex3.toInt()] else 0
                                    t9 = t8 + color
                                    t10 = (t8 and 0xff00ff) + (color and 0xff00ff)
                                    t11 = (t10 and 0x1000100) + (t9 - t10 and 0x10000)
                                    t11 = t9 - t11 or (t11 - (t11 ushr 8))
                                    if (t8 == 0 && t2 != 255) {
                                        t8 = t11
                                        t11 = screenPixels[dstIndex]
                                        t11 = ((t8 and 0xff00ff) * t2 + (t11 and 0xff00ff) * t3 and -16711936) + (t2 * (t8 and 0xff00) + t3 * (t11 and 0xff00) and 0xff0000) shr 8
                                    }

                                    screenPixels[dstIndex++] = t11
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
                                    paletteIndex2 = spritePixels[t7 + (u shr 16)]
                                    if (paletteIndex2.toInt() != 0) {
                                        t10 = spritePalette[paletteIndex2.toInt() and 0xff]
                                        val srcRb = (t10 and 0xff00ff) * t1 and -16711936
                                        val srcG = t1 * (t10 and 0xff00) and 0xff0000
                                        screenPixels[dstIndex++] = ((srcRb or srcG) ushr 8) + color
                                    } else {
                                        ++dstIndex
                                    }

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
                                paletteIndex = spritePixels[t3 + (u shr 16)]
                                if (paletteIndex.toInt() != 0) {
                                    t6 = spritePalette[paletteIndex.toInt() and 0xff]
                                    t7 = screenPixels[dstIndex]
                                    t8 = t6 + t7
                                    t9 = (t6 and 0xff00ff) + (t7 and 0xff00ff)
                                    t7 = (t9 and 0x1000100) + (t8 - t9 and 0x10000)
                                    screenPixels[dstIndex++] = t8 - t7 or (t7 - (t7 ushr 8))
                                } else {
                                    ++dstIndex
                                }

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
                                val paletteIndex5 = spritePixels[t6 + (u shr 16)]
                                if (paletteIndex5.toInt() != 0) {
                                    t9 = spritePalette[paletteIndex5.toInt() and 0xff]
                                    t10 = t2 * (t9 and 0xff0000) and 0xff000000.toInt()
                                    t11 = t3 * (t9 and 0xff00) and 0xff0000
                                    t12 = (t9 and 0xff) * t4 and 0xff00
                                    t9 = (t10 or t11 or t12) ushr 8
                                    t13 = screenPixels[dstIndex]
                                    t14 = t9 + t13
                                    t15 = (t9 and 0xff00ff) + (t13 and 0xff00ff)
                                    t13 = (t15 and 0x1000100) + (t14 - t15 and 0x10000)
                                    screenPixels[dstIndex++] = t14 - t13 or (t13 - (t13 ushr 8))
                                } else {
                                    ++dstIndex
                                }

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
                                paletteIndex = spritePixels[t3 + (u shr 16)]
                                t6 = if (paletteIndex > 0) spritePalette[paletteIndex.toInt()] else 0
                                t7 = t6 + color
                                t8 = (t6 and 0xff00ff) + (color and 0xff00ff)
                                t9 = (t8 and 0x1000100) + (t7 - t8 and 0x10000)
                                t6 = t7 - t9 or (t9 - (t9 ushr 8))
                                t9 = screenPixels[dstIndex]
                                t7 = t6 + t9
                                t8 = (t6 and 0xff00ff) + (t9 and 0xff00ff)
                                t9 = (t8 and 0x1000100) + (t7 - t8 and 0x10000)
                                screenPixels[dstIndex++] = t7 - t9 or (t9 - (t9 ushr 8))
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
                                paletteIndex2 = spritePixels[t7 + (u shr 16)]
                                if (paletteIndex2.toInt() != 0) {
                                    t10 = spritePalette[paletteIndex2.toInt() and 0xff]
                                    val srcRb = (t10 and 0xff00ff) * t1 and -16711936
                                    val srcG = t1 * (t10 and 0xff00) and 0xff0000
                                    t10 = ((srcRb or srcG) ushr 8) + color
                                    t11 = screenPixels[dstIndex]
                                    t12 = t10 + t11
                                    t13 = (t10 and 0xff00ff) + (t11 and 0xff00ff)
                                    t11 = (t13 and 0x1000100) + (t12 - t13 and 0x10000)
                                    screenPixels[dstIndex++] = t12 - t11 or (t11 - (t11 ushr 8))
                                } else {
                                    ++dstIndex
                                }

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
        var t1: Int
        if (drawY < toolkit.clipTop) {
            t1 = toolkit.clipTop - drawY
            h -= t1
            drawY = toolkit.clipTop
            srcIndex += t1 * w
            dstIndex += t1 * viewportWidth
        }

        if (h + drawY > toolkit.clipBottom) {
            h -= h + drawY - toolkit.clipBottom
        }

        if (drawX < toolkit.clipLeft) {
            t1 = toolkit.clipLeft - drawX
            w -= t1
            drawX = toolkit.clipLeft
            srcIndex += t1
            dstIndex += t1
            srcStep += t1
            dstStep += t1
        }

        if (w + drawX > toolkit.clipRight) {
            t1 = w + drawX - toolkit.clipRight
            w -= t1
            srcStep += t1
            dstStep += t1
        }

        if (w > 0 && h > 0) {
            var t2: Int
            var t3: Int
            var t4: Int
            var t5: Int
            var t6: Int
            var t7: Int
            var t8: Int
            var t9: Int
            if (blendMode == 0) {
                if (combineMode == 1) {
                    t1 = -h
                    while (t1 < 0) {
                        t2 = dstIndex + w - 3
                        while (dstIndex < t2) {
                            screenPixels[dstIndex++] = spritePalette[spritePixels[srcIndex++].toInt() and 0xff]
                            screenPixels[dstIndex++] = spritePalette[spritePixels[srcIndex++].toInt() and 0xff]
                            screenPixels[dstIndex++] = spritePalette[spritePixels[srcIndex++].toInt() and 0xff]
                            screenPixels[dstIndex++] = spritePalette[spritePixels[srcIndex++].toInt() and 0xff]
                        }

                        t2 += 3
                        while (dstIndex < t2) {
                            screenPixels[dstIndex++] = spritePalette[spritePixels[srcIndex++].toInt() and 0xff]
                        }

                        dstIndex += dstStep
                        srcIndex += srcStep
                        t1++
                    }
                } else if (combineMode == 0) {
                    t1 = (color and 0xff0000) shr 16
                    t2 = (color and 0xff00) shr 8
                    t3 = color and 0xff

                    t4 = -h
                    while (t4 < 0) {
                        t5 = -w
                        while (t5 < 0) {
                            t6 = spritePalette[spritePixels[srcIndex++].toInt() and 0xff]
                            t7 = t1 * (t6 and 0xff0000) and 0xff000000.toInt()
                            t8 = t2 * (t6 and 0xff00) and 0xff0000
                            t9 = (t6 and 0xff) * t3 and 0xff00
                            screenPixels[dstIndex++] = (t7 or t8 or t9) ushr 8
                            t5++
                        }

                        dstIndex += dstStep
                        srcIndex += srcStep
                        t4++
                    }
                } else if (combineMode == 3) {
                    t1 = -h
                    while (t1 < 0) {
                        t2 = -w
                        while (t2 < 0) {
                            t3 = spritePalette[spritePixels[srcIndex++].toInt() and 0xff]
                            t4 = t3 + color
                            t5 = (t3 and 0xff00ff) + (color and 0xff00ff)
                            t6 = (t5 and 0x1000100) + (t4 - t5 and 0x10000)
                            screenPixels[dstIndex++] = t4 - t6 or (t6 - (t6 ushr 8))
                            t2++
                        }

                        dstIndex += dstStep
                        srcIndex += srcStep
                        t1++
                    }
                } else {
                    if (combineMode != 2) {
                        throw IllegalArgumentException()
                    }

                    t1 = color ushr 24
                    t2 = 256 - t1
                    t3 = t2 * (color and 0xff00ff) and -16711936
                    t4 = t2 * (color and 0xff00) and 0xff0000
                    color = (t3 or t4) ushr 8

                    t5 = -h
                    while (t5 < 0) {
                        t6 = -w
                        while (t6 < 0) {
                            t7 = spritePalette[spritePixels[srcIndex++].toInt() and 0xff]
                            t3 = (t7 and 0xff00ff) * t1 and -16711936
                            t4 = t1 * (t7 and 0xff00) and 0xff0000
                            screenPixels[dstIndex++] = color + ((t3 or t4) ushr 8)
                            t6++
                        }

                        dstIndex += dstStep
                        srcIndex += srcStep
                        t5++
                    }
                }
            } else {
                var t10: Int
                var t11: Int
                var t12: Int
                var t13: Int
                var paletteIndex: Byte
                var paletteIndex2: Byte
                if (blendMode == 1) {
                    if (combineMode == 1) {
                        t1 = -h
                        while (t1 < 0) {
                            t2 = -w
                            while (t2 < 0) {
                                paletteIndex = spritePixels[srcIndex++]
                                if (paletteIndex.toInt() != 0) {
                                    t4 = spritePalette[paletteIndex.toInt() and 0xff] or 0xff000000.toInt()
                                    val alpha: Short = 255
                                    val invAlpha: Byte = 0
                                    t7 = screenPixels[dstIndex]
                                    screenPixels[dstIndex++] = (((t4 and -16711936) ushr 8) * alpha + ((t7 and -16711936) ushr 8) * invAlpha and -16711936) + (((t4 and 0xff00ff) * alpha + (t7 and 0xff00ff) * invAlpha and -16711936) shr 8)
                                } else {
                                    ++dstIndex
                                }
                                t2++
                            }

                            dstIndex += dstStep
                            srcIndex += srcStep
                            t1++
                        }
                    } else {
                        var paletteIndex3: Byte
                        if (combineMode == 0) {
                            if ((color and 0xffffff) == 16777215) {
                                t1 = color ushr 24
                                t2 = 256 - t1

                                t3 = -h
                                while (t3 < 0) {
                                    t4 = -w
                                    while (t4 < 0) {
                                        paletteIndex3 = spritePixels[srcIndex++]
                                        if (paletteIndex3.toInt() != 0) {
                                            t6 = spritePalette[paletteIndex3.toInt() and 0xff]
                                            t7 = screenPixels[dstIndex]
                                            screenPixels[dstIndex++] = ((t6 and 0xff00ff) * t1 + (t7 and 0xff00ff) * t2 and -16711936) + (t1 * (t6 and 0xff00) + t2 * (t7 and 0xff00) and 0xff0000) shr 8
                                        } else {
                                            ++dstIndex
                                        }
                                        t4++
                                    }

                                    dstIndex += dstStep
                                    srcIndex += srcStep
                                    t3++
                                }
                            } else {
                                t1 = (color and 0xff0000) shr 16
                                t2 = (color and 0xff00) shr 8
                                t3 = color and 0xff
                                t4 = color ushr 24
                                t5 = 256 - t4

                                t6 = -h
                                while (t6 < 0) {
                                    t7 = -w
                                    while (t7 < 0) {
                                        val paletteIndex4 = spritePixels[srcIndex++]
                                        if (paletteIndex4.toInt() != 0) {
                                            t9 = spritePalette[paletteIndex4.toInt() and 0xff]
                                            if (t4 != 255) {
                                                t10 = t1 * (t9 and 0xff0000) and 0xff000000.toInt()
                                                t11 = t2 * (t9 and 0xff00) and 0xff0000
                                                t12 = (t9 and 0xff) * t3 and 0xff00
                                                t9 = (t10 or t11 or t12) ushr 8
                                                t13 = screenPixels[dstIndex]
                                                screenPixels[dstIndex++] = ((t9 and 0xff00ff) * t4 + (t13 and 0xff00ff) * t5 and -16711936) + (t4 * (t9 and 0xff00) + t5 * (t13 and 0xff00) and 0xff0000) shr 8
                                            } else {
                                                t10 = t1 * (t9 and 0xff0000) and 0xff000000.toInt()
                                                t11 = t2 * (t9 and 0xff00) and 0xff0000
                                                t12 = (t9 and 0xff) * t3 and 0xff00
                                                screenPixels[dstIndex++] = (t10 or t11 or t12) ushr 8
                                            }
                                        } else {
                                            ++dstIndex
                                        }
                                        t7++
                                    }

                                    dstIndex += dstStep
                                    srcIndex += srcStep
                                    t6++
                                }
                            }
                        } else if (combineMode == 3) {
                            t1 = color ushr 24
                            t2 = 256 - t1

                            t3 = -h
                            while (t3 < 0) {
                                t4 = -w
                                while (t4 < 0) {
                                    paletteIndex3 = spritePixels[srcIndex++]
                                    t6 = if (paletteIndex3 > 0) spritePalette[paletteIndex3.toInt()] else 0
                                    t7 = t6 + color
                                    t8 = (t6 and 0xff00ff) + (color and 0xff00ff)
                                    t9 = (t8 and 0x1000100) + (t7 - t8 and 0x10000)
                                    t9 = t7 - t9 or (t9 - (t9 ushr 8))
                                    if (t6 == 0 && t1 != 255) {
                                        t6 = t9
                                        t9 = screenPixels[dstIndex]
                                        t9 = ((t6 and 0xff00ff) * t1 + (t9 and 0xff00ff) * t2 and -16711936) + (t1 * (t6 and 0xff00) + t2 * (t9 and 0xff00) and 0xff0000) shr 8
                                    }

                                    screenPixels[dstIndex++] = t9
                                    t4++
                                }

                                dstIndex += dstStep
                                srcIndex += srcStep
                                t3++
                            }
                        } else {
                            if (combineMode != 2) {
                                throw IllegalArgumentException()
                            }

                            t1 = color ushr 24
                            t2 = 256 - t1
                            t3 = t2 * (color and 0xff00ff) and -16711936
                            t4 = t2 * (color and 0xff00) and 0xff0000
                            color = (t3 or t4) ushr 8

                            t5 = -h
                            while (t5 < 0) {
                                t6 = -w
                                while (t6 < 0) {
                                    paletteIndex2 = spritePixels[srcIndex++]
                                    if (paletteIndex2.toInt() != 0) {
                                        t8 = spritePalette[paletteIndex2.toInt() and 0xff]
                                        t3 = (t8 and 0xff00ff) * t1 and -16711936
                                        t4 = t1 * (t8 and 0xff00) and 0xff0000
                                        screenPixels[dstIndex++] = color + ((t3 or t4) ushr 8)
                                    } else {
                                        ++dstIndex
                                    }
                                    t6++
                                }

                                dstIndex += dstStep
                                srcIndex += srcStep
                                t5++
                            }
                        }
                    }
                } else {
                    if (blendMode != 2) {
                        throw IllegalArgumentException()
                    }

                    if (combineMode == 1) {
                        t1 = -h
                        while (t1 < 0) {
                            t2 = -w
                            while (t2 < 0) {
                                paletteIndex = spritePixels[srcIndex++]
                                if (paletteIndex.toInt() != 0) {
                                    t4 = spritePalette[paletteIndex.toInt() and 0xff]
                                    t5 = screenPixels[dstIndex]
                                    t6 = t4 + t5
                                    t7 = (t4 and 0xff00ff) + (t5 and 0xff00ff)
                                    t5 = (t7 and 0x1000100) + (t6 - t7 and 0x10000)
                                    screenPixels[dstIndex++] = t6 - t5 or (t5 - (t5 ushr 8))
                                } else {
                                    ++dstIndex
                                }
                                t2++
                            }

                            dstIndex += dstStep
                            srcIndex += srcStep
                            t1++
                        }
                    } else if (combineMode == 0) {
                        t1 = (color and 0xff0000) shr 16
                        t2 = (color and 0xff00) shr 8
                        t3 = color and 0xff

                        t4 = -h
                        while (t4 < 0) {
                            t5 = -w
                            while (t5 < 0) {
                                val paletteIndex5 = spritePixels[srcIndex++]
                                if (paletteIndex5.toInt() != 0) {
                                    t7 = spritePalette[paletteIndex5.toInt() and 0xff]
                                    t8 = t1 * (t7 and 0xff0000) and 0xff000000.toInt()
                                    t9 = t2 * (t7 and 0xff00) and 0xff0000
                                    t10 = (t7 and 0xff) * t3 and 0xff00
                                    t7 = (t8 or t9 or t10) ushr 8
                                    t11 = screenPixels[dstIndex]
                                    t12 = t7 + t11
                                    t13 = (t7 and 0xff00ff) + (t11 and 0xff00ff)
                                    t11 = (t13 and 0x1000100) + (t12 - t13 and 0x10000)
                                    screenPixels[dstIndex++] = t12 - t11 or (t11 - (t11 ushr 8))
                                } else {
                                    ++dstIndex
                                }
                                t5++
                            }

                            dstIndex += dstStep
                            srcIndex += srcStep
                            t4++
                        }
                    } else if (combineMode == 3) {
                        t1 = -h
                        while (t1 < 0) {
                            t2 = -w
                            while (t2 < 0) {
                                paletteIndex = spritePixels[srcIndex++]
                                t4 = if (paletteIndex > 0) spritePalette[paletteIndex.toInt()] else 0
                                t5 = t4 + color
                                t6 = (t4 and 0xff00ff) + (color and 0xff00ff)
                                t7 = (t6 and 0x1000100) + (t5 - t6 and 0x10000)
                                t4 = t5 - t7 or (t7 - (t7 ushr 8))
                                t7 = screenPixels[dstIndex]
                                t5 = t4 + t7
                                t6 = (t4 and 0xff00ff) + (t7 and 0xff00ff)
                                t7 = (t6 and 0x1000100) + (t5 - t6 and 0x10000)
                                screenPixels[dstIndex++] = t5 - t7 or (t7 - (t7 ushr 8))
                                t2++
                            }

                            dstIndex += dstStep
                            srcIndex += srcStep
                            t1++
                        }
                    } else {
                        if (combineMode != 2) {
                            throw IllegalArgumentException()
                        }

                        t1 = color ushr 24
                        t2 = 256 - t1
                        t3 = t2 * (color and 0xff00ff) and -16711936
                        t4 = t2 * (color and 0xff00) and 0xff0000
                        color = (t3 or t4) ushr 8

                        t5 = -h
                        while (t5 < 0) {
                            t6 = -w
                            while (t6 < 0) {
                                paletteIndex2 = spritePixels[srcIndex++]
                                if (paletteIndex2.toInt() != 0) {
                                    t8 = spritePalette[paletteIndex2.toInt() and 0xff]
                                    t3 = (t8 and 0xff00ff) * t1 and -16711936
                                    t4 = t1 * (t8 and 0xff00) and 0xff0000
                                    t8 = color + ((t3 or t4) ushr 8)
                                    t9 = screenPixels[dstIndex]
                                    t10 = t8 + t9
                                    t11 = (t8 and 0xff00ff) + (t9 and 0xff00ff)
                                    t9 = (t11 and 0x1000100) + (t10 - t11 and 0x10000)
                                    screenPixels[dstIndex++] = t10 - t9 or (t9 - (t9 ushr 8))
                                } else {
                                    ++dstIndex
                                }
                                t6++
                            }

                            dstIndex += dstStep
                            srcIndex += srcStep
                            t5++
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
        var drawX = x
        var drawY = y
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

            var clipAmount: Int
            if (drawY < toolkit.clipTop) {
                clipAmount = toolkit.clipTop - drawY
                h -= clipAmount
                dstIndex += toolkit.viewportWidth * clipAmount
                v += dv * clipAmount
            }

            if (drawX + w > toolkit.clipRight) {
                clipAmount = drawX + w - toolkit.clipRight
                w -= clipAmount
                dstStep += clipAmount
            }

            if (drawX < toolkit.clipLeft) {
                clipAmount = toolkit.clipLeft - drawX
                w -= clipAmount
                dstIndex += clipAmount
                u += du * clipAmount
                dstStep += clipAmount
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
            var paletteIndex: Byte
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
                                    screenPixels!![dstIndex] = spritePalette[spritePixels[t3 + (u shr 16)].toInt() and 0xff]
                                }

                                if (depthTest && depthWrite) {
                                    depthBuffer!![dstIndex] = depth
                                }
                            }

                            u += du
                            ++dstIndex
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
                                if (colorWrite) {
                                    t8 = spritePalette[spritePixels[t6 + (u shr 16)].toInt() and 0xff]
                                    t9 = t1 * (t8 and 0xff0000) and 0xff000000.toInt()
                                    t10 = t2 * (t8 and 0xff00) and 0xff0000
                                    t11 = (t8 and 0xff) * t3 and 0xff00
                                    screenPixels!![dstIndex] = (t9 or t10 or t11) ushr 8
                                }

                                if (depthTest && depthWrite) {
                                    depthBuffer!![dstIndex] = depth
                                }
                            }

                            u += du
                            ++dstIndex
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
                                if (colorWrite) {
                                    paletteIndex = spritePixels[t3 + (u shr 16)]
                                    t6 = if (paletteIndex > 0) spritePalette[paletteIndex.toInt()] else 0
                                    t7 = color + t6
                                    t8 = (t6 and 0xff00ff) + (color and 0xff00ff)
                                    t9 = (t8 and 0x1000100) + (t7 - t8 and 0x10000)
                                    screenPixels!![dstIndex] = t7 - t9 or (t9 - (t9 ushr 8))
                                }

                                if (depthTest && depthWrite) {
                                    depthBuffer!![dstIndex] = depth
                                }
                            }

                            u += du
                            ++dstIndex
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
                    t4 = (color and 0xff00) * t2 and 0xff0000
                    color = (t3 or t4) ushr 8
                    t5 = u

                    t6 = -h
                    while (t6 < 0) {
                        t7 = imageWidth * (v shr 16)

                        t8 = -w
                        while (t8 < 0) {
                            if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                if (colorWrite) {
                                    t9 = spritePalette[spritePixels[t7 + (u shr 16)].toInt() and 0xff]
                                    t3 = (t9 and 0xff00ff) * t1 and -16711936
                                    t4 = t1 * (t9 and 0xff00) and 0xff0000
                                    screenPixels!![dstIndex] = color + ((t3 or t4) ushr 8)
                                }

                                if (depthTest && depthWrite) {
                                    depthBuffer!![dstIndex] = depth
                                }
                            }

                            u += du
                            ++dstIndex
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
                var t15: Int
                var paletteIndex2: Byte
                if (blendMode == 1) {
                    if (combineMode == 1) {
                        t1 = u

                        t2 = -h
                        while (t2 < 0) {
                            t3 = imageWidth * (v shr 16)

                            t4 = -w
                            while (t4 < 0) {
                                if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                    paletteIndex = spritePixels[t3 + (u shr 16)]
                                    if (paletteIndex.toInt() != 0) {
                                        if (colorWrite) {
                                            screenPixels!![dstIndex] = spritePalette[paletteIndex.toInt() and 0xff]
                                        }

                                        if (depthTest && depthWrite) {
                                            depthBuffer!![dstIndex] = depth
                                        }
                                    }
                                }

                                u += du
                                ++dstIndex
                                t4++
                            }

                            v += dv
                            u = t1
                            dstIndex += dstStep
                            t2++
                        }
                    } else {
                        var paletteIndex3: Byte
                        if (combineMode == 0) {
                            t1 = u
                            if ((color and 0xffffff) == 16777215) {
                                t2 = color ushr 24
                                t3 = 256 - t2

                                t4 = -h
                                while (t4 < 0) {
                                    t5 = imageWidth * (v shr 16)

                                    t6 = -w
                                    while (t6 < 0) {
                                        if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                            paletteIndex3 = spritePixels[t5 + (u shr 16)]
                                            if (paletteIndex3.toInt() != 0) {
                                                if (colorWrite) {
                                                    t8 = spritePalette[paletteIndex3.toInt() and 0xff]
                                                    t9 = screenPixels!![dstIndex]
                                                    screenPixels[dstIndex] = ((t8 and 0xff00ff) * t2 + (t9 and 0xff00ff) * t3 and -16711936) + (t2 * (t8 and 0xff00) + t3 * (t9 and 0xff00) and 0xff0000) shr 8
                                                }

                                                if (depthTest && depthWrite) {
                                                    depthBuffer!![dstIndex] = depth
                                                }
                                            }
                                        }

                                        u += du
                                        ++dstIndex
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
                                        if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                            val paletteIndex4 = spritePixels[t8 + (u shr 16)]
                                            if (paletteIndex4.toInt() != 0) {
                                                t11 = spritePalette[paletteIndex4.toInt() and 0xff]
                                                if (t5 != 255) {
                                                    if (colorWrite) {
                                                        t12 = t2 * (t11 and 0xff0000) and 0xff000000.toInt()
                                                        t13 = t3 * (t11 and 0xff00) and 0xff0000
                                                        t14 = (t11 and 0xff) * t4 and 0xff00
                                                        t11 = (t12 or t13 or t14) ushr 8
                                                        t15 = screenPixels!![dstIndex]
                                                        screenPixels[dstIndex] = ((t11 and 0xff00ff) * t5 + (t15 and 0xff00ff) * t6 and -16711936) + (t5 * (t11 and 0xff00) + t6 * (t15 and 0xff00) and 0xff0000) shr 8
                                                    }
                                                } else {
                                                    if (colorWrite) {
                                                        t12 = t2 * (t11 and 0xff0000) and 0xff000000.toInt()
                                                        t13 = t3 * (t11 and 0xff00) and 0xff0000
                                                        t14 = (t11 and 0xff) * t4 and 0xff00
                                                        screenPixels!![dstIndex] = (t12 or t13 or t14) ushr 8
                                                    }
                                                }
                                                if (depthTest && depthWrite) {
                                                    depthBuffer!![dstIndex] = depth
                                                }
                                            }
                                        }

                                        u += du
                                        ++dstIndex
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
                                    if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                        if (colorWrite) {
                                            paletteIndex3 = spritePixels[t5 + (u shr 16)]
                                            t8 = if (paletteIndex3 > 0) spritePalette[paletteIndex3.toInt()] else 0
                                            t9 = color + t8
                                            t10 = (t8 and 0xff00ff) + (color and 0xff00ff)
                                            t11 = (t10 and 0x1000100) + (t9 - t10 and 0x10000)
                                            t11 = t9 - t11 or (t11 - (t11 ushr 8))
                                            if (t8 == 0 && t2 != 255) {
                                                t8 = t11
                                                t11 = screenPixels!![dstIndex]
                                                t11 = ((t8 and 0xff00ff) * t2 + (t11 and 0xff00ff) * t3 and -16711936) + (t2 * (t8 and 0xff00) + t3 * (t11 and 0xff00) and 0xff0000) shr 8
                                            }

                                            screenPixels!![dstIndex] = t11
                                        }

                                        if (depthTest && depthWrite) {
                                            depthBuffer!![dstIndex] = depth
                                        }
                                    }

                                    u += du
                                    ++dstIndex
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
                            t4 = (color and 0xff00) * t2 and 0xff0000
                            color = (t3 or t4) ushr 8
                            t5 = u

                            t6 = -h
                            while (t6 < 0) {
                                t7 = imageWidth * (v shr 16)

                                t8 = -w
                                while (t8 < 0) {
                                    if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                        paletteIndex2 = spritePixels[t7 + (u shr 16)]
                                        if (paletteIndex2.toInt() != 0) {
                                            if (colorWrite) {
                                                t10 = spritePalette[paletteIndex2.toInt() and 0xff]
                                                t3 = (t10 and 0xff00ff) * t1 and -16711936
                                                t4 = t1 * (t10 and 0xff00) and 0xff0000
                                                screenPixels!![dstIndex] = color + ((t3 or t4) ushr 8)
                                            }

                                            if (depthTest && depthWrite) {
                                                depthBuffer!![dstIndex] = depth
                                            }
                                        }
                                    }

                                    u += du
                                    ++dstIndex
                                    t8++
                                }

                                v += dv
                                u = t5
                                dstIndex += dstStep
                                t6++
                            }
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
                                if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                    paletteIndex = spritePixels[t3 + (u shr 16)]
                                    if (paletteIndex.toInt() != 0) {
                                        if (colorWrite) {
                                            t6 = spritePalette[paletteIndex.toInt() and 0xff]
                                            t7 = screenPixels!![dstIndex]
                                            t8 = t6 + t7
                                            t9 = (t6 and 0xff00ff) + (t7 and 0xff00ff)
                                            t7 = (t9 and 0x1000100) + (t8 - t9 and 0x10000)
                                            screenPixels[dstIndex] = t8 - t7 or (t7 - (t7 ushr 8))
                                        }

                                        if (depthTest && depthWrite) {
                                            depthBuffer!![dstIndex] = depth
                                        }
                                    }
                                }

                                u += du
                                ++dstIndex
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
                                if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                    val paletteIndex5 = spritePixels[t6 + (u shr 16)]
                                    if (paletteIndex5.toInt() != 0) {
                                        if (colorWrite) {
                                            t9 = spritePalette[paletteIndex5.toInt() and 0xff]
                                            t10 = t2 * (t9 and 0xff0000) and 0xff000000.toInt()
                                            t11 = t3 * (t9 and 0xff00) and 0xff0000
                                            t12 = (t9 and 0xff) * t4 and 0xff00
                                            t9 = (t10 or t11 or t12) ushr 8
                                            t13 = screenPixels!![dstIndex]
                                            t14 = t9 + t13
                                            t15 = (t9 and 0xff00ff) + (t13 and 0xff00ff)
                                            t13 = (t15 and 0x1000100) + (t14 - t15 and 0x10000)
                                            screenPixels[dstIndex] = t14 - t13 or (t13 - (t13 ushr 8))
                                        }

                                        if (depthTest && depthWrite) {
                                            depthBuffer!![dstIndex] = depth
                                        }
                                    }
                                }

                                u += du
                                ++dstIndex
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
                                if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                    if (colorWrite) {
                                        paletteIndex = spritePixels[t3 + (u shr 16)]
                                        t6 = if (paletteIndex > 0) spritePalette[paletteIndex.toInt()] else 0
                                        t7 = color + t6
                                        t8 = (t6 and 0xff00ff) + (color and 0xff00ff)
                                        t9 = (t8 and 0x1000100) + (t7 - t8 and 0x10000)
                                        t6 = t7 - t9 or (t9 - (t9 ushr 8))
                                        t9 = screenPixels!![dstIndex]
                                        t7 = t6 + t9
                                        t8 = (t6 and 0xff00ff) + (t9 and 0xff00ff)
                                        t9 = (t8 and 0x1000100) + (t7 - t8 and 0x10000)
                                        screenPixels[dstIndex] = t7 - t9 or (t9 - (t9 ushr 8))
                                    }

                                    if (depthTest && depthWrite) {
                                        depthBuffer!![dstIndex] = depth
                                    }
                                }

                                u += du
                                ++dstIndex
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
                        t4 = (color and 0xff00) * t2 and 0xff0000
                        color = (t3 or t4) ushr 8
                        t5 = u

                        t6 = -h
                        while (t6 < 0) {
                            t7 = imageWidth * (v shr 16)

                            t8 = -w
                            while (t8 < 0) {
                                if (!depthTest || depth < depthBuffer!![dstIndex]) {
                                    paletteIndex2 = spritePixels[t7 + (u shr 16)]
                                    if (paletteIndex2.toInt() != 0) {
                                        if (colorWrite) {
                                            t10 = spritePalette[paletteIndex2.toInt() and 0xff]
                                            t3 = (t10 and 0xff00ff) * t1 and -16711936
                                            t4 = t1 * (t10 and 0xff00) and 0xff0000
                                            t10 = color + ((t3 or t4) ushr 8)
                                            t11 = screenPixels!![dstIndex]
                                            t12 = t10 + t11
                                            t13 = (t10 and 0xff00ff) + (t11 and 0xff00ff)
                                            t11 = (t13 and 0x1000100) + (t12 - t13 and 0x10000)
                                            screenPixels[dstIndex] = t12 - t11 or (t11 - (t11 ushr 8))
                                        }

                                        if (depthTest && depthWrite) {
                                            depthBuffer!![dstIndex] = depth
                                        }
                                    }
                                }

                                u += du
                                ++dstIndex
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
        var clipAmount: Int
        if (drawY < toolkit.clipTop) {
            clipAmount = toolkit.clipTop - drawY
            h -= clipAmount
            drawY = toolkit.clipTop
            srcIndex += clipAmount * w
            dstIndex += clipAmount * viewportWidth
        }

        if (h + drawY > toolkit.clipBottom) {
            h -= h + drawY - toolkit.clipBottom
        }

        if (drawX < toolkit.clipLeft) {
            clipAmount = toolkit.clipLeft - drawX
            w -= clipAmount
            drawX = toolkit.clipLeft
            srcIndex += clipAmount
            dstIndex += clipAmount
            srcStep += clipAmount
            dstStep += clipAmount
        }

        if (w + drawX > toolkit.clipRight) {
            clipAmount = w + drawX - toolkit.clipRight
            w -= clipAmount
            srcStep += clipAmount
            dstStep += clipAmount
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

            val endY = minOf(rowStartX.size + maskY, h + drawY)

            for (rowY in startY until endY) {
                val maskRowX = rowStartX[rowY - maskY] + maskX
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

                var i = -maskRowWidth
                while (i < 0) {
                    val paletteIndex = spritePixels[srcIndex++]
                    if (paletteIndex.toInt() != 0) {
                        screenPixels[dstIndex++] = spritePalette[paletteIndex.toInt() and 0xff]
                    } else {
                        ++dstIndex
                    }
                    i++
                }

                srcIndex += skip + srcStep
                dstIndex += skip + dstStep
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
        var steps: Int
        var srcIndex: Int
        var writeIndex: Int
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
                            srcIndex = (u shr 12) + (v shr 12) * imageWidth
                            writeIndex = dstIndex++
                            blendPixel(screenPixels, writeIndex, srcIndex, combineMode, blendMode)
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
                        steps = v - (imageHeight shl 12)
                        if (steps >= 0) {
                            steps = (dvdx - steps) / dvdx
                            col += steps
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
                            steps = (dvdx - 1 - v) / dvdx
                            col += steps
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
        val paletteIndex = spritePixels[srcIndex]
        if (blendMode == 0) {
            if (combineMode == 1) {
                screenPixels[dstIndex] = spritePalette[paletteIndex.toInt() and 0xff]
            } else if (combineMode == 0) {
                srcPixel = spritePalette[paletteIndex.toInt() and 0xff]
                t1 = colorRed * (srcPixel and 0xff0000) and 0xff000000.toInt()
                t2 = (srcPixel and 0xff00) * colorGreen and 0xff0000
                t3 = (srcPixel and 0xff) * colorBlue and 0xff00
                screenPixels[dstIndex] = (t1 or t2 or t3) ushr 8
            } else if (combineMode == 3) {
                srcPixel = spritePalette[paletteIndex.toInt() and 0xff]
                t1 = blendColor
                t2 = srcPixel + t1
                t3 = (srcPixel and 0xff00ff) + (t1 and 0xff00ff)
                t4 = (t3 and 0x1000100) + (t2 - t3 and 0x10000)
                screenPixels[dstIndex] = t2 - t4 or (t4 - (t4 ushr 8))
            } else {
                if (combineMode != 2) {
                    throw IllegalArgumentException()
                }
                srcPixel = spritePalette[paletteIndex.toInt() and 0xff]
                t1 = (srcPixel and 0xff00ff) * colorAlpha and -16711936
                t2 = (srcPixel and 0xff00) * colorAlpha and 0xff0000
                screenPixels[dstIndex] = ((t1 or t2) ushr 8) + overlayPrecomputed
            }
        } else if (blendMode == 1) {
            if (combineMode == 1) {
                if (paletteIndex.toInt() != 0) {
                    screenPixels[dstIndex] = spritePalette[paletteIndex.toInt() and 0xff]
                }
            } else if (combineMode == 0) {
                if (paletteIndex.toInt() != 0) {
                    srcPixel = spritePalette[paletteIndex.toInt() and 0xff]
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
                srcPixel = if (paletteIndex > 0) spritePalette[paletteIndex.toInt()] else 0
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
                if (paletteIndex.toInt() != 0) {
                    srcPixel = spritePalette[paletteIndex.toInt() and 0xff]
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
                if (paletteIndex.toInt() != 0) {
                    srcPixel = spritePalette[paletteIndex.toInt() and 0xff]
                    t1 = screenPixels[dstIndex]
                    t2 = srcPixel + t1
                    t3 = (srcPixel and 0xff00ff) + (t1 and 0xff00ff)
                    t1 = (t3 and 0x1000100) + (t2 - t3 and 0x10000)
                    screenPixels[dstIndex] = t2 - t1 or (t1 - (t1 ushr 8))
                }
            } else if (combineMode == 0) {
                if (paletteIndex.toInt() != 0) {
                    srcPixel = spritePalette[paletteIndex.toInt() and 0xff]
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
                srcPixel = if (paletteIndex > 0) spritePalette[paletteIndex.toInt()] else 0
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
                if (paletteIndex.toInt() != 0) {
                    srcPixel = spritePalette[paletteIndex.toInt() and 0xff]
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
        var steps: Int
        var maskRowX: Int
        var maskSpan: Int
        var skip: Int
        var paletteIndex: Byte
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
                                paletteIndex = spritePixels[(u shr 12) + (v shr 12) * imageWidth]
                                if (paletteIndex.toInt() != 0) {
                                    screenPixels[dstIndex++] = spritePalette[paletteIndex.toInt() and 0xff]
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
                            steps = v - (imageHeight shl 12)
                            if (steps >= 0) {
                                steps = (dvdx - steps) / dvdx
                                col += steps
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
                                paletteIndex = spritePixels[(u shr 12) + (v shr 12) * imageWidth]
                                if (paletteIndex.toInt() != 0) {
                                    screenPixels[dstIndex++] = spritePalette[paletteIndex.toInt() and 0xff]
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
                                steps = (dvdx - 1 - v) / dvdx
                                col += steps
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
                                paletteIndex = spritePixels[(u shr 12) + (v shr 12) * imageWidth]
                                if (paletteIndex.toInt() != 0) {
                                    screenPixels[dstIndex++] = spritePalette[paletteIndex.toInt() and 0xff]
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
        var paletteIndex: Byte
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
                            paletteIndex = spritePixels[(u shr 12) + (v shr 12) * imageWidth]
                            if (paletteIndex.toInt() != 0) {
                                screenPixels[dstIndex++] = spritePalette[paletteIndex.toInt() and 0xff]
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
                        paletteIndex = spritePixels[(u shr 12) + (v shr 12) * imageWidth]
                        if (paletteIndex.toInt() != 0) {
                            screenPixels[dstIndex++] = spritePalette[paletteIndex.toInt() and 0xff]
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
                        paletteIndex = spritePixels[(u shr 12) + (v shr 12) * imageWidth]
                        if (paletteIndex.toInt() != 0) {
                            screenPixels[dstIndex++] = spritePalette[paletteIndex.toInt() and 0xff]
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
        var paletteIndex: Byte
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
                            paletteIndex = spritePixels[(u shr 12) + (v shr 12) * imageWidth]
                            if (paletteIndex.toInt() != 0) {
                                screenPixels[dstIndex++] = spritePalette[paletteIndex.toInt() and 0xff]
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
                        paletteIndex = spritePixels[(u shr 12) + (v shr 12) * imageWidth]
                        if (paletteIndex.toInt() != 0) {
                            screenPixels[dstIndex++] = spritePalette[paletteIndex.toInt() and 0xff]
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
                        paletteIndex = spritePixels[(u shr 12) + (v shr 12) * imageWidth]
                        if (paletteIndex.toInt() != 0) {
                            screenPixels[dstIndex++] = spritePalette[paletteIndex.toInt() and 0xff]
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
}
