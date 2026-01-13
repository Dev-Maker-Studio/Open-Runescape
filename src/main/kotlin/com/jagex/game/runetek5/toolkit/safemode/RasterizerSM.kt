package com.jagex.game.runetek5.toolkit.safemode

import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.utils.ColorUtil

/**
 * Only used for safemode
 * @author Brainpower
 */
class RasterizerSM(
    @JvmField val safemodeToolkit: SafeModeToolkit,
    @JvmField val threadResource: ThreadResourceSM
) {
    @JvmField var centerX: Float = 0f
    @JvmField var offsetX: Float = 0f
    @JvmField var centerY: Float = 0f
    @JvmField var clipWidth: Int = 0
    @JvmField var clipHeight: Int = 0
    @JvmField var offsetY: Float = 0f

    /** Draws the lines around the characters, triangles - flawed code though */
    @JvmField var wireframeMode: Boolean = false
    @JvmField var fogColour: Int = 0
    @JvmField var texturePixelsA: IntArray? = null
    @JvmField var textureScaleA: Float = 0f
    @JvmField var blendingAlpha: Int = 0
    @JvmField var texturePixelsB: IntArray? = null
    @JvmField var textureScaleB: Float = 0f
    @JvmField var texturePixelsC: IntArray? = null
    @JvmField var textureScaleC: Float = 0f
    @JvmField var textureSizeA: Int = 0
    @JvmField var textureMaskA: Int = 0
    @JvmField var alphaMode: Int = 0
    @JvmField var textureSizeB: Int = 0
    @JvmField var textureMaskB: Int = 0
    @JvmField var textureSizeC: Int = 0
    @JvmField var textureMaskC: Int = 0

    /** Always false */
    @JvmField var blending: Boolean = false

    @JvmField var smoothEdges: Boolean = true
    @JvmField var aBool1676: Boolean = false
    @JvmField var aFloat1682: Float = 0f
    @JvmField var aFloat1683: Float = 1.0f
    @JvmField val scanlineOffsets: IntArray = IntArray(4096)
    @JvmField val textureB: Int = -1
    @JvmField val textureA: Int = -1
    @JvmField var textureRepeat: Boolean = true
    @JvmField val textureC: Int = -1
    @JvmField var clipEdges: Boolean = false
    @JvmField val scanlineWidth: Int = safemodeToolkit.viewportWidth
    @JvmField val colourBuffer: IntArray? = safemodeToolkit.colourBuffer
    @JvmField val depthBuffer: FloatArray? = safemodeToolkit.depthBuffer

    val currentColumn: Int
        get() = scanlineOffsets[0] % scanlineWidth

    val currentRow: Int
        get() = scanlineOffsets[0] / scanlineWidth

    fun setWireframeMode(wireframeMode: Boolean) {
        this.wireframeMode = wireframeMode
    }

    fun drawFlatScanline(
        colorWrite: Boolean,
        depthWrite: Boolean,
        transparent: Boolean,
        pixels: IntArray,
        bufferOffset: Int,
        color: Int,
        xA: Int,
        xB: Int,
        depth: Float,
        depthOffset: Float
    ) {
        var bufferOffset = bufferOffset
        var color = color
        var xA = xA
        var xB = xB
        var depth = depth
        var x = 0
        if (clipEdges) {
            if (xB > clipWidth) {
                xB = clipWidth
            }
            if (xA < 0) {
                xA = 0
            }
        }
        if (xA < xB) {
            bufferOffset += xA - 1
            x = xB - xA shr 2
            depth += depthOffset * xA
            val depthBuf = depthBuffer!!
            if (threadResource.zbuffering) {
                if (blendingAlpha == 0) {
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            if (colorWrite) {
                                pixels[bufferOffset] = color
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            if (colorWrite) {
                                pixels[bufferOffset] = color
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            if (colorWrite) {
                                pixels[bufferOffset] = color
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            if (colorWrite) {
                                pixels[bufferOffset] = color
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += depthOffset
                    }
                    x = xB - xA and 0x3
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            if (colorWrite) {
                                pixels[bufferOffset] = color
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += depthOffset
                    }
                    return
                }
                if (blendingAlpha != 254) {
                    val alpha = blendingAlpha
                    val invertedAlpha = 256 - blendingAlpha
                    color = ((color and 0xFF00FF) * invertedAlpha shr 8 and 0xFF00FF) + (invertedAlpha * (color and 0xFF00) shr 8 and 0xFF00)
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            if (colorWrite) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color)
                                } else {
                                    pixels[bufferOffset] = ((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color
                                }
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            if (colorWrite) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color)
                                } else {
                                    pixels[bufferOffset] = ((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color
                                }
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            if (colorWrite) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color)
                                } else {
                                    pixels[bufferOffset] = ((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color
                                }
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            if (colorWrite) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color)
                                } else {
                                    pixels[bufferOffset] = ((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color
                                }
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += depthOffset
                    }
                    x = xB - xA and 0x3
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            if (colorWrite) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color)
                                } else {
                                    pixels[bufferOffset] = ((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color
                                }
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += depthOffset
                    }
                    return
                }
                if (xA != 0 && xB <= clipWidth - 1) {
                    while (--x >= 0) {
                        bufferOffset++
                        if ((!depthWrite || depth < depthBuf[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += depthOffset
                        bufferOffset++
                        if ((!depthWrite || depth < depthBuf[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += depthOffset
                        bufferOffset++
                        if ((!depthWrite || depth < depthBuf[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += depthOffset
                        bufferOffset++
                        if ((!depthWrite || depth < depthBuf[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += depthOffset
                    }
                    x = xB - xA and 0x3
                    while (--x >= 0) {
                        bufferOffset++
                        if ((!depthWrite || depth < depthBuf[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += depthOffset
                    }
                }
            } else if (colorWrite) {
                if (blendingAlpha == 0) {
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = color
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = color
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = color
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = color
                        }
                        depth += depthOffset
                    }
                    x = xB - xA and 0x3
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = color
                        }
                        depth += depthOffset
                    }
                    return
                }
                if (blendingAlpha != 254) {
                    val alpha = blendingAlpha
                    val invertedAlpha = 256 - blendingAlpha
                    color = ((color and 0xFF00FF) * invertedAlpha shr 8 and 0xFF00FF) + (invertedAlpha * (color and 0xFF00) shr 8 and 0xFF00)
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color)
                            } else {
                                pixels[bufferOffset] = ((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color
                            }
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color)
                            } else {
                                pixels[bufferOffset] = ((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color
                            }
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color)
                            } else {
                                pixels[bufferOffset] = ((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color
                            }
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color)
                            } else {
                                pixels[bufferOffset] = ((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color
                            }
                        }
                        depth += depthOffset
                    }
                    x = xB - xA and 0x3
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color)
                            } else {
                                pixels[bufferOffset] = ((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color
                            }
                        }
                        depth += depthOffset
                    }
                    return
                }
                if (xA != 0 && xB <= clipWidth - 1) {
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += depthOffset
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += depthOffset
                    }
                    x = xB - xA and 0x3
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += depthOffset
                    }
                }
            }
        }
    }

    fun drawMultiMaterializedTriangle(
        yA: Float, yB: Float, yC: Float,
        xA: Float, xB: Float, xC: Float,
        zNearA: Float, zNearB: Float, zNearC: Float,
        zFarA: Float, zFarB: Float, zFarC: Float,
        uA: Float, uB: Float, uC: Float,
        vA: Float, vB: Float, vC: Float,
        floorColorA: Int, floorColorB: Int, floorColorC: Int,
        fogColor: Int,
        fogDensityA: Float, fogDensityB: Float, fogDensityC: Float,
        materialA: Int, scaleA: Float,
        materialB: Int, scaleB: Float,
        materialC: Int, scaleC: Float
    ) {
        var yA = yA
        var yB = yB
        var yC = yC
        var xA = xA
        var xB = xB
        var xC = xC
        var zNearA = zNearA
        var zNearB = zNearB
        var zNearC = zNearC
        var zFarA = zFarA
        var zFarB = zFarB
        var zFarC = zFarC
        var uA = uA
        var uB = uB
        var uC = uC
        var vA = vA
        var vB = vB
        var vC = vC
        var floorColorA = floorColorA
        var floorColorB = floorColorB
        var floorColorC = floorColorC
        var fogDensityA = fogDensityA
        var fogDensityB = fogDensityB
        var fogDensityC = fogDensityC
        var materialA = materialA
        var materialB = materialB
        var materialC = materialC
        var scaleA = scaleA
        var scaleB = scaleB
        var scaleC = scaleC
        fogColour = fogColor
        if (yA > yB || yA > yC) {
            if (yB <= yC) {
                var tmpFloat = xA
                xA = xB
                xB = tmpFloat
                tmpFloat = yA
                yA = yB
                yB = tmpFloat
                tmpFloat = zNearA
                zNearA = zNearB
                zNearB = tmpFloat
                tmpFloat = uA
                uA = uB
                uB = tmpFloat
                tmpFloat = vA
                vA = vB
                vB = tmpFloat
                tmpFloat = zFarA
                zFarA = zFarB
                zFarB = tmpFloat
                tmpFloat = fogDensityA
                fogDensityA = fogDensityB
                fogDensityB = tmpFloat
                tmpFloat = scaleA
                scaleA = scaleB
                scaleB = tmpFloat
                var tmpInt = floorColorA
                floorColorA = floorColorB
                floorColorB = tmpInt
                tmpInt = materialA
                materialA = materialB
                materialB = tmpInt
            } else {
                var tmpFloat = xA
                xA = xC
                xC = tmpFloat
                tmpFloat = yA
                yA = yC
                yC = tmpFloat
                tmpFloat = zNearA
                zNearA = zNearC
                zNearC = tmpFloat
                tmpFloat = uA
                uA = uC
                uC = tmpFloat
                tmpFloat = vA
                vA = vC
                vC = tmpFloat
                tmpFloat = zFarA
                zFarA = zFarC
                zFarC = tmpFloat
                tmpFloat = fogDensityA
                fogDensityA = fogDensityC
                fogDensityC = tmpFloat
                tmpFloat = scaleA
                scaleA = scaleC
                scaleC = tmpFloat
                var tmpInt = floorColorA
                floorColorA = floorColorC
                floorColorC = tmpInt
                tmpInt = materialA
                materialA = materialC
                materialC = tmpInt
            }
        }

        // Texture a
        if (materialA != textureA) {
            texturePixelsA = safemodeToolkit.getAnimatedMaterialArgb(materialA)
            if (texturePixelsA == null) {
                blendingAlpha = 255 - (floorColorA shr 24 and 0xff)
                val materialColorRgb = ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(safemodeToolkit.getMaterialColor(materialA)).toInt() and 0xffff]
                val tmp2 = 0xff000000.toInt() or (materialColorRgb shr 16 and 0xff) * (floorColorA shr 16 and 0xff) shl 8 and 0xff0000 or (floorColorA shr 8 and 0xff) * (materialColorRgb shr 8 and 0xff) and 0xff00 or (materialColorRgb and 0xff) * (floorColorA and 0xff) shr 8
                drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, zNearA, zNearB, zNearC, ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityA), ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityB), ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityC))
                return
            }

            textureSizeA = if (safemodeToolkit.isSmallTexture(materialA)) 64 else safemodeToolkit.materialSize
            textureMaskA = textureSizeA - 1
            alphaMode = safemodeToolkit.getMaterialEffectCombiner(materialA)
        }
        textureScaleA = scaleA

        // Texture b
        if (textureB != materialB) {
            texturePixelsB = safemodeToolkit.getAnimatedMaterialArgb(materialB)
            if (texturePixelsB == null) {
                blendingAlpha = 255 - (floorColorA shr 24 and 0xff)
                val materialColorRgb = ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(safemodeToolkit.getMaterialColor(materialB)).toInt() and 0xffff]
                val tmp2 = 0xff000000.toInt() or (materialColorRgb shr 16 and 0xff) * (floorColorA shr 16 and 0xff) shl 8 and 0xff0000 or (floorColorA shr 8 and 0xff) * (materialColorRgb shr 8 and 0xff) and 0xff00 or (materialColorRgb and 0xff) * (floorColorA and 0xff) shr 8
                drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, zNearA, zNearB, zNearC, ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityA), ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityB), ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityC))
                return
            }

            textureSizeB = if (safemodeToolkit.isSmallTexture(materialB)) 64 else safemodeToolkit.materialSize
            textureMaskB = textureSizeB - 1
        }
        textureScaleB = scaleB

        // Texture c
        if (materialC != textureC) {
            texturePixelsC = safemodeToolkit.getAnimatedMaterialArgb(materialC)
            if (texturePixelsC == null) {
                blendingAlpha = 255 - (floorColorA shr 24 and 0xff)
                val materialColorRgb = ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(safemodeToolkit.getMaterialColor(materialC)).toInt() and 0xffff]
                val tmp2 = 0xff000000.toInt() or (materialColorRgb shr 16 and 0xff) * (floorColorA shr 16 and 0xff) shl 8 and 0xff0000 or (floorColorA shr 8 and 0xff) * (materialColorRgb shr 8 and 0xff) and 0xff00 or (materialColorRgb and 0xff) * (floorColorA and 0xff) shr 8
                drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, zNearA, zNearB, zNearC, ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityA), ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityB), ColorUtil.mixColorsWithTransparency(tmp2, fogColor, fogDensityC))
                return
            }

            textureSizeC = if (safemodeToolkit.isSmallTexture(materialC)) 64 else safemodeToolkit.materialSize
            textureMaskC = textureSizeC - 1
        }
        textureScaleC = scaleC

        uA /= zFarA
        uB /= zFarB
        uC /= zFarC
        vA /= zFarA
        vB /= zFarB
        vC /= zFarC
        zNearA = 1.0f / zNearA
        zNearB = 1.0f / zNearB
        zNearC = 1.0f / zNearC
        zFarA = 1.0f / zFarA
        zFarB = 1.0f / zFarB
        zFarC = 1.0f / zFarC
        var colorAAlpha = (floorColorA shr 24 and 0xff).toFloat()
        var colorBAlpha = (floorColorB shr 24 and 0xff).toFloat()
        var colorCAlpha = (floorColorC shr 24 and 0xff).toFloat()
        var colorARed = (floorColorA shr 16 and 0xff).toFloat()
        var colorBRed = (floorColorB shr 16 and 0xff).toFloat()
        var colorCRed = (floorColorC shr 16 and 0xff).toFloat()
        var colorAGreen = (floorColorA shr 8 and 0xff).toFloat()
        var colorBGreen = (floorColorB shr 8 and 0xff).toFloat()
        var colorCGreen = (floorColorC shr 8 and 0xff).toFloat()
        var colorABlue = (floorColorA and 0xff).toFloat()
        var colorBBlue = (floorColorB and 0xff).toFloat()
        var colorCBlue = (floorColorC and 0xff).toFloat()
        var textureAPercentageA = 1.0f
        var textureAPercentageB = 0.0f
        var textureAPercentageC = 0.0f
        var textureBPercentageA = 0.0f
        var textureBPercentageB = 1.0f
        var textureBPercentageC = 0.0f
        var slopeXBa = 0.0f
        var slopeZNearBa = 0.0f
        var slopeZFarBa = 0.0f
        var slopeUBa = 0.0f
        var slopeVBa = 0.0f
        var slopeFogDensityBa = 0.0f
        var slopeAlphaBa = 0.0f
        var slopeRedBa = 0.0f
        var slopeGreenBa = 0.0f
        var slopeBlueBa = 0.0f
        var slopeTextureAPercentageBa = 0.0f
        var slopeTextureBPercentageBa = 0.0f
        if (yB != yA) {
            val lengthY = yB - yA
            slopeXBa = (xB - xA) / lengthY
            slopeZNearBa = (zNearB - zNearA) / lengthY
            slopeZFarBa = (zFarB - zFarA) / lengthY
            slopeUBa = (uB - uA) / lengthY
            slopeVBa = (vB - vA) / lengthY
            slopeFogDensityBa = (fogDensityB - fogDensityA) / lengthY
            slopeAlphaBa = (colorBAlpha - colorAAlpha) / lengthY
            slopeRedBa = (colorBRed - colorARed) / lengthY
            slopeGreenBa = (colorBGreen - colorAGreen) / lengthY
            slopeBlueBa = (colorBBlue - colorABlue) / lengthY
            slopeTextureAPercentageBa = (textureAPercentageB - textureAPercentageA) / lengthY
            slopeTextureBPercentageBa = (textureBPercentageB - textureBPercentageA) / lengthY
        }
        var slopeXCb = 0.0f
        var slopeZNearCb = 0.0f
        var slopeZFarCb = 0.0f
        var slopeUCb = 0.0f
        var slopeVCb = 0.0f
        var slopeFogDensityCb = 0.0f
        var slopeAlphaCb = 0.0f
        var slopeRedCb = 0.0f
        var slopeGreenCb = 0.0f
        var slopeBlueCb = 0.0f
        var slopeTextureAPercentageCb = 0.0f
        var slopeTextureBPercentageCb = 0.0f
        if (yC != yB) {
            val lengthY = yC - yB
            slopeXCb = (xC - xB) / lengthY
            slopeZNearCb = (zNearC - zNearB) / lengthY
            slopeZFarCb = (zFarC - zFarB) / lengthY
            slopeUCb = (uC - uB) / lengthY
            slopeVCb = (vC - vB) / lengthY
            slopeFogDensityCb = (fogDensityC - fogDensityB) / lengthY
            slopeAlphaCb = (colorCAlpha - colorBAlpha) / lengthY
            slopeRedCb = (colorCRed - colorBRed) / lengthY
            slopeGreenCb = (colorCGreen - colorBGreen) / lengthY
            slopeBlueCb = (colorCBlue - colorBBlue) / lengthY
            slopeTextureAPercentageCb = (textureAPercentageC - textureAPercentageB) / lengthY
            slopeTextureBPercentageCb = (textureBPercentageC - textureBPercentageB) / lengthY
        }
        var slopeXAc = 0.0f
        var slopeZNearAc = 0.0f
        var slopeZFarAc = 0.0f
        var slopeUAc = 0.0f
        var slopeVAc = 0.0f
        var slopeFogDensityAc = 0.0f
        var slopeAlphaAc = 0.0f
        var slopeRedAc = 0.0f
        var slopeGreenAc = 0.0f
        var slopeBlueAc = 0.0f
        var slopeTextureAPercentageAc = 0.0f
        var slopeTextureBPercentageAc = 0.0f
        if (yA != yC) {
            val lengthY = yA - yC
            slopeXAc = (xA - xC) / lengthY
            slopeZNearAc = (zNearA - zNearC) / lengthY
            slopeZFarAc = (zFarA - zFarC) / lengthY
            slopeUAc = (uA - uC) / lengthY
            slopeVAc = (vA - vC) / lengthY
            slopeFogDensityAc = (fogDensityA - fogDensityC) / lengthY
            slopeAlphaAc = (colorAAlpha - colorCAlpha) / lengthY
            slopeRedAc = (colorARed - colorCRed) / lengthY
            slopeGreenAc = (colorAGreen - colorCGreen) / lengthY
            slopeBlueAc = (colorABlue - colorCBlue) / lengthY
            slopeTextureAPercentageAc = (textureAPercentageA - textureAPercentageC) / lengthY
            slopeTextureBPercentageAc = (textureBPercentageA - textureBPercentageC) / lengthY
        }
        if (yA < clipHeight) {
            if (yB > clipHeight) {
                yB = clipHeight.toFloat()
            }
            if (yC > clipHeight) {
                yC = clipHeight.toFloat()
            }
            if (yB < yC) {
                xC = xA
                zNearC = zNearA
                zFarC = zFarA
                uC = uA
                vC = vA
                fogDensityC = fogDensityA
                colorCAlpha = colorAAlpha
                colorCRed = colorARed
                colorCGreen = colorAGreen
                colorCBlue = colorABlue
                textureAPercentageC = textureAPercentageA
                textureBPercentageC = textureBPercentageA
                if (yA < 0.0f) {
                    xA -= slopeXBa * yA
                    xC -= slopeXAc * yA
                    zNearA -= slopeZNearBa * yA
                    zNearC -= slopeZNearAc * yA
                    zFarA -= slopeZFarBa * yA
                    zFarC -= slopeZFarAc * yA
                    uA -= slopeUBa * yA
                    uC -= slopeUAc * yA
                    vA -= slopeVBa * yA
                    vC -= slopeVAc * yA
                    fogDensityA -= slopeFogDensityBa * yA
                    fogDensityC -= slopeFogDensityAc * yA
                    colorAAlpha -= slopeAlphaBa * yA
                    colorCAlpha -= slopeAlphaAc * yA
                    colorARed -= slopeRedBa * yA
                    colorCRed -= slopeRedAc * yA
                    colorAGreen -= slopeGreenBa * yA
                    colorCGreen -= slopeGreenAc * yA
                    colorABlue -= slopeBlueBa * yA
                    colorCBlue -= slopeBlueAc * yA
                    textureAPercentageA -= slopeTextureAPercentageBa * yA
                    textureAPercentageC -= slopeTextureAPercentageAc * yA
                    textureBPercentageA -= slopeTextureBPercentageBa * yA
                    textureBPercentageC -= slopeTextureBPercentageAc * yA
                    yA = 0.0f
                }
                if (yB < 0.0f) {
                    xB -= slopeXCb * yB
                    zNearB -= slopeZNearCb * yB
                    zFarB -= slopeZFarCb * yB
                    uB -= slopeUCb * yB
                    vB -= slopeVCb * yB
                    fogDensityB -= slopeFogDensityCb * yB
                    colorBAlpha -= slopeAlphaCb * yB
                    colorBRed -= slopeRedCb * yB
                    colorBGreen -= slopeGreenCb * yB
                    colorBBlue -= slopeBlueCb * yB
                    textureAPercentageB -= slopeTextureAPercentageCb * yB
                    textureBPercentageB -= slopeTextureBPercentageCb * yB
                    yB = 0.0f
                }
                if ((yA == yB || slopeXAc >= slopeXBa) && (yA != yB || slopeXAc <= slopeXCb)) {
                    yA = (yA + 0.5f).toInt().toFloat()
                    yB = (yB + 0.5f).toInt().toFloat()
                    yC = (yC + 0.5f).toInt().toFloat() - yB
                    yB -= yA
                    yA = scanlineOffsets[yA.toInt()].toFloat()
                    while (--yB >= 0.0f) {
                        drawMultiMaterialzedScanline(colourBuffer!!, yA.toInt(), xA.toInt(), xC.toInt(), zNearA, zNearC, zFarA, zFarC, uA, uC, vA, vC, fogDensityA, fogDensityC, colorAAlpha, colorCAlpha, colorARed, colorCRed, colorAGreen, colorCGreen, colorABlue, colorCBlue, textureAPercentageA, textureAPercentageC, textureBPercentageA, textureBPercentageC)
                        xA += slopeXBa
                        xC += slopeXAc
                        zNearA += slopeZNearBa
                        zNearC += slopeZNearAc
                        zFarA += slopeZFarBa
                        zFarC += slopeZFarAc
                        uA += slopeUBa
                        uC += slopeUAc
                        vA += slopeVBa
                        vC += slopeVAc
                        fogDensityA += slopeFogDensityBa
                        fogDensityC += slopeFogDensityAc
                        colorAAlpha += slopeAlphaBa
                        colorCAlpha += slopeAlphaAc
                        colorARed += slopeRedBa
                        colorCRed += slopeRedAc
                        colorAGreen += slopeGreenBa
                        colorCGreen += slopeGreenAc
                        colorABlue += slopeBlueBa
                        colorCBlue += slopeBlueAc
                        textureAPercentageA += slopeTextureAPercentageBa
                        textureAPercentageC += slopeTextureAPercentageAc
                        textureBPercentageA += slopeTextureBPercentageBa
                        textureBPercentageC += slopeTextureBPercentageAc
                        yA += scanlineWidth
                    }
                    while (--yC >= 0.0f) {
                        drawMultiMaterialzedScanline(colourBuffer!!, yA.toInt(), xB.toInt(), xC.toInt(), zNearB, zNearC, zFarB, zFarC, uB, uC, vB, vC, fogDensityB, fogDensityC, colorBAlpha, colorCAlpha, colorBRed, colorCRed, colorBGreen, colorCGreen, colorBBlue, colorCBlue, textureAPercentageB, textureAPercentageC, textureBPercentageB, textureBPercentageC)
                        xB += slopeXCb
                        xC += slopeXAc
                        zNearB += slopeZNearCb
                        zNearC += slopeZNearAc
                        zFarB += slopeZFarCb
                        zFarC += slopeZFarAc
                        uB += slopeUCb
                        uC += slopeUAc
                        vB += slopeVCb
                        vC += slopeVAc
                        fogDensityB += slopeFogDensityCb
                        fogDensityC += slopeFogDensityAc
                        colorBAlpha += slopeAlphaCb
                        colorCAlpha += slopeAlphaAc
                        colorBRed += slopeRedCb
                        colorCRed += slopeRedAc
                        colorBGreen += slopeGreenCb
                        colorCGreen += slopeGreenAc
                        colorBBlue += slopeBlueCb
                        colorCBlue += slopeBlueAc
                        textureAPercentageB += slopeTextureAPercentageCb
                        textureAPercentageC += slopeTextureAPercentageAc
                        textureBPercentageB += slopeTextureBPercentageCb
                        textureBPercentageC += slopeTextureBPercentageAc
                        yA += scanlineWidth
                    }
                } else {
                    yA = (yA + 0.5f).toInt().toFloat()
                    yB = (yB + 0.5f).toInt().toFloat()
                    yC = (yC + 0.5f).toInt().toFloat() - yB
                    yB -= yA
                    yA = scanlineOffsets[yA.toInt()].toFloat()
                    while (--yB >= 0.0f) {
                        drawMultiMaterialzedScanline(colourBuffer!!, yA.toInt(), xC.toInt(), xA.toInt(), zNearC, zNearA, zFarC, zFarA, uC, uA, vC, vA, fogDensityC, fogDensityA, colorCAlpha, colorAAlpha, colorCRed, colorARed, colorCGreen, colorAGreen, colorCBlue, colorABlue, textureAPercentageC, textureAPercentageA, textureBPercentageC, textureBPercentageA)
                        xA += slopeXBa
                        xC += slopeXAc
                        zNearA += slopeZNearBa
                        zNearC += slopeZNearAc
                        zFarA += slopeZFarBa
                        zFarC += slopeZFarAc
                        uA += slopeUBa
                        uC += slopeUAc
                        vA += slopeVBa
                        vC += slopeVAc
                        fogDensityA += slopeFogDensityBa
                        fogDensityC += slopeFogDensityAc
                        colorAAlpha += slopeAlphaBa
                        colorCAlpha += slopeAlphaAc
                        colorARed += slopeRedBa
                        colorCRed += slopeRedAc
                        colorAGreen += slopeGreenBa
                        colorCGreen += slopeGreenAc
                        colorABlue += slopeBlueBa
                        colorCBlue += slopeBlueAc
                        textureAPercentageA += slopeTextureAPercentageBa
                        textureAPercentageC += slopeTextureAPercentageAc
                        textureBPercentageA += slopeTextureBPercentageBa
                        textureBPercentageC += slopeTextureBPercentageAc
                        yA += scanlineWidth
                    }
                    while (--yC >= 0.0f) {
                        drawMultiMaterialzedScanline(colourBuffer!!, yA.toInt(), xC.toInt(), xB.toInt(), zNearC, zNearB, zFarC, zFarB, uC, uB, vC, vB, fogDensityC, fogDensityB, colorCAlpha, colorBAlpha, colorCRed, colorBRed, colorCGreen, colorBGreen, colorCBlue, colorBBlue, textureAPercentageC, textureAPercentageB, textureBPercentageC, textureBPercentageB)
                        xB += slopeXCb
                        xC += slopeXAc
                        zNearB += slopeZNearCb
                        zNearC += slopeZNearAc
                        zFarB += slopeZFarCb
                        zFarC += slopeZFarAc
                        uB += slopeUCb
                        uC += slopeUAc
                        vB += slopeVCb
                        vC += slopeVAc
                        fogDensityB += slopeFogDensityCb
                        fogDensityC += slopeFogDensityAc
                        colorBAlpha += slopeAlphaCb
                        colorCAlpha += slopeAlphaAc
                        colorBRed += slopeRedCb
                        colorCRed += slopeRedAc
                        colorBGreen += slopeGreenCb
                        colorCGreen += slopeGreenAc
                        colorBBlue += slopeBlueCb
                        colorCBlue += slopeBlueAc
                        textureAPercentageB += slopeTextureAPercentageCb
                        textureAPercentageC += slopeTextureAPercentageAc
                        textureBPercentageB += slopeTextureBPercentageCb
                        textureBPercentageC += slopeTextureBPercentageAc
                        yA += scanlineWidth
                    }
                }
            } else {
                xB = xA
                zNearB = zNearA
                zFarB = zFarA
                uB = uA
                vB = vA
                fogDensityB = fogDensityA
                colorBAlpha = colorAAlpha
                colorBRed = colorARed
                colorBGreen = colorAGreen
                colorBBlue = colorABlue
                textureAPercentageB = textureAPercentageA
                textureBPercentageB = textureBPercentageA
                if (yA < 0.0f) {
                    xA -= slopeXBa * yA
                    xB -= slopeXAc * yA
                    zNearA -= slopeZNearBa * yA
                    zNearB -= slopeZNearAc * yA
                    zFarA -= slopeZFarBa * yA
                    zFarB -= slopeZFarAc * yA
                    uA -= slopeUBa * yA
                    uB -= slopeUAc * yA
                    vA -= slopeVBa * yA
                    vB -= slopeVAc * yA
                    fogDensityA -= slopeFogDensityBa * yA
                    fogDensityB -= slopeFogDensityAc * yA
                    colorAAlpha -= slopeAlphaBa * yA
                    colorBAlpha -= slopeAlphaAc * yA
                    colorARed -= slopeRedBa * yA
                    colorBRed -= slopeRedAc * yA
                    colorAGreen -= slopeGreenBa * yA
                    colorBGreen -= slopeGreenAc * yA
                    colorABlue -= slopeBlueBa * yA
                    colorBBlue -= slopeBlueAc * yA
                    textureAPercentageA -= slopeTextureAPercentageBa * yA
                    textureAPercentageB -= slopeTextureAPercentageAc * yA
                    textureBPercentageA -= slopeTextureBPercentageBa * yA
                    textureBPercentageB -= slopeTextureBPercentageAc * yA
                    yA = 0.0f
                }
                if (yC < 0.0f) {
                    xC -= slopeXCb * yC
                    zNearC -= slopeZNearCb * yC
                    zFarC -= slopeZFarCb * yC
                    uC -= slopeUCb * yC
                    vC -= slopeVCb * yC
                    fogDensityC -= slopeFogDensityCb * yC
                    colorCAlpha -= slopeAlphaCb * yC
                    colorCRed -= slopeRedCb * yC
                    colorCGreen -= slopeGreenCb * yC
                    colorCBlue -= slopeBlueCb * yC
                    textureAPercentageC -= slopeTextureAPercentageCb * yC
                    textureBPercentageC -= slopeTextureBPercentageCb * yC
                    yC = 0.0f
                }
                if (yA != yC && slopeXAc < slopeXBa || yA == yC && slopeXCb > slopeXBa) {
                    yA = (yA + 0.5f).toInt().toFloat()
                    yC = (yC + 0.5f).toInt().toFloat()
                    yB = (yB + 0.5f).toInt().toFloat() - yC
                    yC -= yA
                    yA = scanlineOffsets[yA.toInt()].toFloat()
                    while (--yC >= 0.0f) {
                        drawMultiMaterialzedScanline(colourBuffer!!, yA.toInt(), xB.toInt(), xA.toInt(), zNearB, zNearA, zFarB, zFarA, uB, uA, vB, vA, fogDensityB, fogDensityA, colorBAlpha, colorAAlpha, colorBRed, colorARed, colorBGreen, colorAGreen, colorBBlue, colorABlue, textureAPercentageB, textureAPercentageA, textureBPercentageB, textureBPercentageA)
                        xA += slopeXBa
                        xB += slopeXAc
                        zNearA += slopeZNearBa
                        zNearB += slopeZNearAc
                        zFarA += slopeZFarBa
                        zFarB += slopeZFarAc
                        uA += slopeUBa
                        uB += slopeUAc
                        vA += slopeVBa
                        vB += slopeVAc
                        fogDensityA += slopeFogDensityBa
                        fogDensityB += slopeFogDensityAc
                        colorAAlpha += slopeAlphaBa
                        colorBAlpha += slopeAlphaAc
                        colorARed += slopeRedBa
                        colorBRed += slopeRedAc
                        colorAGreen += slopeGreenBa
                        colorBGreen += slopeGreenAc
                        colorABlue += slopeBlueBa
                        colorBBlue += slopeBlueAc
                        textureAPercentageA += slopeTextureAPercentageBa
                        textureAPercentageB += slopeTextureAPercentageAc
                        textureBPercentageA += slopeTextureBPercentageBa
                        textureBPercentageB += slopeTextureBPercentageAc
                        yA += scanlineWidth
                    }
                    while (--yB >= 0.0f) {
                        drawMultiMaterialzedScanline(colourBuffer!!, yA.toInt(), xC.toInt(), xA.toInt(), zNearC, zNearA, zFarC, zFarA, uC, uA, vC, vA, fogDensityC, fogDensityA, colorCAlpha, colorAAlpha, colorCRed, colorARed, colorCGreen, colorAGreen, colorCBlue, colorABlue, textureAPercentageC, textureAPercentageA, textureBPercentageC, textureBPercentageA)
                        xC += slopeXCb
                        xA += slopeXBa
                        zNearC += slopeZNearCb
                        zNearA += slopeZNearBa
                        zFarC += slopeZFarCb
                        zFarA += slopeZFarBa
                        uC += slopeUCb
                        uA += slopeUBa
                        vC += slopeVCb
                        vA += slopeVBa
                        fogDensityC += slopeFogDensityCb
                        fogDensityA += slopeFogDensityBa
                        colorCAlpha += slopeAlphaCb
                        colorAAlpha += slopeAlphaBa
                        colorCRed += slopeRedCb
                        colorARed += slopeRedBa
                        colorCGreen += slopeGreenCb
                        colorAGreen += slopeGreenBa
                        colorCBlue += slopeBlueCb
                        colorABlue += slopeBlueBa
                        textureAPercentageC += slopeTextureAPercentageCb
                        textureAPercentageA += slopeTextureAPercentageBa
                        textureBPercentageC += slopeTextureBPercentageCb
                        textureBPercentageA += slopeTextureBPercentageBa
                        yA += scanlineWidth
                    }
                } else {
                    yA = (yA + 0.5f).toInt().toFloat()
                    yC = (yC + 0.5f).toInt().toFloat()
                    yB = (yB + 0.5f).toInt().toFloat() - yC
                    yC -= yA
                    yA = scanlineOffsets[yA.toInt()].toFloat()
                    while (--yC >= 0.0f) {
                        drawMultiMaterialzedScanline(colourBuffer!!, yA.toInt(), xA.toInt(), xB.toInt(), zNearA, zNearB, zFarA, zFarB, uA, uB, vA, vB, fogDensityA, fogDensityB, colorAAlpha, colorBAlpha, colorARed, colorBRed, colorAGreen, colorBGreen, colorABlue, colorBBlue, textureAPercentageA, textureAPercentageB, textureBPercentageA, textureBPercentageB)
                        xB += slopeXAc
                        xA += slopeXBa
                        zNearB += slopeZNearAc
                        zNearA += slopeZNearBa
                        zFarB += slopeZFarAc
                        zFarA += slopeZFarBa
                        uB += slopeUAc
                        uA += slopeUBa
                        vB += slopeVAc
                        vA += slopeVBa
                        fogDensityB += slopeFogDensityAc
                        fogDensityA += slopeFogDensityBa
                        colorBAlpha += slopeAlphaAc
                        colorAAlpha += slopeAlphaBa
                        colorBRed += slopeRedAc
                        colorARed += slopeRedBa
                        colorBGreen += slopeGreenAc
                        colorAGreen += slopeGreenBa
                        colorBBlue += slopeBlueAc
                        colorABlue += slopeBlueBa
                        textureAPercentageB += slopeTextureAPercentageAc
                        textureAPercentageA += slopeTextureAPercentageBa
                        textureBPercentageB += slopeTextureBPercentageAc
                        textureBPercentageA += slopeTextureBPercentageBa
                        yA += scanlineWidth
                    }
                    while (--yB >= 0.0f) {
                        drawMultiMaterialzedScanline(colourBuffer!!, yA.toInt(), xA.toInt(), xC.toInt(), zNearA, zNearC, zFarA, zFarC, uA, uC, vA, vC, fogDensityA, fogDensityC, colorAAlpha, colorCAlpha, colorARed, colorCRed, colorAGreen, colorCGreen, colorABlue, colorCBlue, textureAPercentageA, textureAPercentageC, textureBPercentageA, textureBPercentageC)
                        xA += slopeXBa
                        xC += slopeXCb
                        zNearA += slopeZNearBa
                        zNearC += slopeZNearCb
                        zFarA += slopeZFarBa
                        zFarC += slopeZFarCb
                        uA += slopeUBa
                        uC += slopeUCb
                        vA += slopeVBa
                        vC += slopeVCb
                        fogDensityA += slopeFogDensityBa
                        fogDensityC += slopeFogDensityCb
                        colorAAlpha += slopeAlphaBa
                        colorCAlpha += slopeAlphaCb
                        colorARed += slopeRedBa
                        colorCRed += slopeRedCb
                        colorAGreen += slopeGreenBa
                        colorCGreen += slopeGreenCb
                        colorABlue += slopeBlueBa
                        colorCBlue += slopeBlueCb
                        textureAPercentageA += slopeTextureAPercentageBa
                        textureAPercentageC += slopeTextureAPercentageCb
                        textureBPercentageA += slopeTextureBPercentageBa
                        textureBPercentageC += slopeTextureBPercentageCb
                        yA += scanlineWidth
                    }
                }
            }
        }
    }

    fun drawScanline(
        writeDepth: Boolean, transparent: Boolean,
        pixels: IntArray, bufferOffset: Int,
        xA: Int, xB: Int,
        depth: Float, slopeDepth: Float,
        red: Float, slopeRed: Float,
        green: Float, slopeGreen: Float,
        blue: Float, slopeBlue: Float
    ) {
        var bufferOffset = bufferOffset
        var xA = xA
        var xB = xB
        var depth = depth
        var red = red
        var slopeRed = slopeRed
        var green = green
        var slopeGreen = slopeGreen
        var blue = blue
        var slopeBlue = slopeBlue
        var x = 0
        var colorRgb = 0
        if (clipEdges) {
            if (xB > clipWidth) {
                xB = clipWidth
            }
            if (xA < 0) {
                xA = 0
            }
        }
        if (xA < xB) {
            var alpha: Int
            var pixel: Int
            var invertedAlpha: Int
            val depthBuf = depthBuffer!!
            if (aBool1676) {
                bufferOffset += xA
                red += slopeRed * xA
                green += slopeGreen * xA
                blue += slopeBlue * xA
                if (smoothEdges) {
                    x = xB - xA shr 2
                    slopeRed *= 4.0f
                    slopeGreen *= 4.0f
                    slopeBlue *= 4.0f
                    if (blendingAlpha == 0) {
                        if (x > 0) {
                            do {
                                colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                red += slopeRed
                                green += slopeGreen
                                blue += slopeBlue
                                pixels[bufferOffset++] = colorRgb
                                pixels[bufferOffset++] = colorRgb
                                pixels[bufferOffset++] = colorRgb
                                pixels[bufferOffset++] = colorRgb
                            } while (--x > 0)
                        }
                        x = xB - xA and 0x3
                        if (x > 0) {
                            colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                            do {
                                pixels[bufferOffset++] = colorRgb
                            } while (--x > 0)
                        }
                    } else if (!blending) {
                        alpha = blendingAlpha
                        invertedAlpha = 256 - blendingAlpha
                        if (x > 0) {
                            do {
                                colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                red += slopeRed
                                green += slopeGreen
                                blue += slopeBlue
                                colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + ((colorRgb and 0xff00) * invertedAlpha shr 8 and 0xff00)
                                pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset++] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                    pixel = pixels[bufferOffset]
                                    pixels[bufferOffset++] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                    pixel = pixels[bufferOffset]
                                    pixels[bufferOffset++] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                    pixel = pixels[bufferOffset]
                                    pixels[bufferOffset++] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                } else {
                                    pixels[bufferOffset++] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                    pixel = pixels[bufferOffset]
                                    pixels[bufferOffset++] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                    pixel = pixels[bufferOffset]
                                    pixels[bufferOffset++] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                    pixel = pixels[bufferOffset]
                                    pixels[bufferOffset++] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                }
                            } while (--x > 0)
                        }
                        x = xB - xA and 0x3
                        if (x > 0) {
                            colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                            colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + ((colorRgb and 0xff00) * invertedAlpha shr 8 and 0xff00)
                            do {
                                pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset++] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                } else {
                                    pixels[bufferOffset++] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                }
                            } while (--x > 0)
                        }
                    } else {
                        if (x > 0) {
                            do {
                                colorRgb = (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                red += slopeRed
                                green += slopeGreen
                                blue += slopeBlue
                                invertedAlpha = bufferOffset++
                                var i_21 = pixels[invertedAlpha]
                                var i_20 = colorRgb + i_21
                                var i_22 = (i_21 and 0xff00ff) + (colorRgb and 0xff00ff)
                                i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                                pixels[invertedAlpha] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                                i_20 = bufferOffset++
                                var i_23 = pixels[i_20]
                                var i_24 = colorRgb + i_23
                                var i_25 = (i_23 and 0xff00ff) + (colorRgb and 0xff00ff)
                                i_23 = (i_25 and 0x1000100) + (i_24 - i_25 and 0x10000)
                                pixels[i_20] = 0xff000000.toInt() or (i_24 - i_23) or (i_23 - (i_23 ushr 8))
                                i_24 = bufferOffset++
                                var i_26 = pixels[i_24]
                                var i_27 = colorRgb + i_26
                                var i_28 = (i_26 and 0xff00ff) + (colorRgb and 0xff00ff)
                                i_26 = (i_28 and 0x1000100) + (i_27 - i_28 and 0x10000)
                                pixels[i_24] = 0xff000000.toInt() or (i_27 - i_26) or (i_26 - (i_26 ushr 8))
                                i_27 = bufferOffset++
                                var i_29 = pixels[i_27]
                                var i_30 = colorRgb + i_29
                                var i_31 = (i_29 and 0xff00ff) + (colorRgb and 0xff00ff)
                                i_29 = (i_31 and 0x1000100) + (i_30 - i_31 and 0x10000)
                                pixels[i_27] = 0xff000000.toInt() or (i_30 - i_29) or (i_29 - (i_29 ushr 8))
                            } while (--x > 0)
                        }
                        x = xB - xA and 0x3
                        if (x > 0) {
                            colorRgb = (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                            do {
                                invertedAlpha = bufferOffset++
                                var i_21 = pixels[invertedAlpha]
                                var i_20 = colorRgb + i_21
                                var i_22 = (i_21 and 0xff00ff) + (colorRgb and 0xff00ff)
                                i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                                pixels[invertedAlpha] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                            } while (--x > 0)
                        }
                    }
                } else {
                    x = xB - xA
                    if (blendingAlpha == 0) {
                        do {
                            pixels[bufferOffset++] = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                            red += slopeRed
                            green += slopeGreen
                            blue += slopeBlue
                        } while (--x > 0)
                    } else if (!blending) {
                        alpha = blendingAlpha
                        invertedAlpha = 256 - blendingAlpha
                        do {
                            colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                            red += slopeRed
                            green += slopeGreen
                            blue += slopeBlue
                            colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + ((colorRgb and 0xff00) * invertedAlpha shr 8 and 0xff00)
                            pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset++] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                            } else {
                                pixels[bufferOffset++] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                            }
                        } while (--x > 0)
                    } else {
                        do {
                            invertedAlpha = bufferOffset++
                            pixel = (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                            var i_21 = pixels[invertedAlpha]
                            var i_20 = pixel + i_21
                            var i_22 = (pixel and 0xff00ff) + (i_21 and 0xff00ff)
                            i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                            pixels[invertedAlpha] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                            red += slopeRed
                            green += slopeGreen
                            blue += slopeBlue
                        } while (--x > 0)
                    }
                }
            } else {
                bufferOffset += xA - 1
                depth += slopeDepth * xA
                red += slopeRed * xA
                green += slopeGreen * xA
                blue += slopeBlue * xA
                if (threadResource.zbuffering) {
                    if (smoothEdges) {
                        x = xB - xA shr 2
                        slopeRed *= 4.0f
                        slopeGreen *= 4.0f
                        slopeBlue *= 4.0f
                        if (blendingAlpha == 0) {
                            if (x > 0) {
                                do {
                                    colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                    red += slopeRed
                                    green += slopeGreen
                                    blue += slopeBlue
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                } while (--x > 0)
                            }
                            x = xB - xA and 0x3
                            if (x > 0) {
                                colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                do {
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                } while (--x > 0)
                            }
                        } else if (!blending) {
                            alpha = blendingAlpha
                            invertedAlpha = 256 - blendingAlpha
                            if (x > 0) {
                                do {
                                    colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                    red += slopeRed
                                    green += slopeGreen
                                    blue += slopeBlue
                                    colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + ((colorRgb and 0xff00) * invertedAlpha shr 8 and 0xff00)
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        pixel = pixels[bufferOffset]
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                        }
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        pixel = pixels[bufferOffset]
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                        }
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        pixel = pixels[bufferOffset]
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                        }
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        pixel = pixels[bufferOffset]
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                        }
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                } while (--x > 0)
                            }
                            x = xB - xA and 0x3
                            if (x > 0) {
                                colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + ((colorRgb and 0xff00) * invertedAlpha shr 8 and 0xff00)
                                do {
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        pixel = pixels[bufferOffset]
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                        }
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                } while (--x > 0)
                            }
                        } else {
                            if (x > 0) {
                                do {
                                    colorRgb = (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                    red += slopeRed
                                    green += slopeGreen
                                    blue += slopeBlue
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        var i_21 = pixels[bufferOffset]
                                        var i_20 = colorRgb + i_21
                                        var i_22 = (i_21 and 0xff00ff) + (colorRgb and 0xff00ff)
                                        i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                                        pixels[bufferOffset] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        var i_21 = pixels[bufferOffset]
                                        var i_20 = colorRgb + i_21
                                        var i_22 = (i_21 and 0xff00ff) + (colorRgb and 0xff00ff)
                                        i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                                        pixels[bufferOffset] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        var i_21 = pixels[bufferOffset]
                                        var i_20 = colorRgb + i_21
                                        var i_22 = (i_21 and 0xff00ff) + (colorRgb and 0xff00ff)
                                        i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                                        pixels[bufferOffset] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        var i_21 = pixels[bufferOffset]
                                        var i_20 = colorRgb + i_21
                                        var i_22 = (i_21 and 0xff00ff) + (colorRgb and 0xff00ff)
                                        i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                                        pixels[bufferOffset] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                } while (--x > 0)
                            }
                            x = xB - xA and 0x3
                            if (x > 0) {
                                colorRgb = (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                do {
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        var i_21 = pixels[bufferOffset]
                                        var i_20 = colorRgb + i_21
                                        var i_22 = (i_21 and 0xff00ff) + (colorRgb and 0xff00ff)
                                        i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                                        pixels[bufferOffset] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                } while (--x > 0)
                            }
                        }
                    } else {
                        x = xB - xA
                        if (blendingAlpha != 0) {
                            if (blending) {
                                do {
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        pixel = (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                        var i_21 = pixels[bufferOffset]
                                        var i_20 = pixel + i_21
                                        var i_22 = (pixel and 0xff00ff) + (i_21 and 0xff00ff)
                                        i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                                        pixels[bufferOffset] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    red += slopeRed
                                    green += slopeGreen
                                    blue += slopeBlue
                                } while (--x > 0)
                            } else {
                                alpha = blendingAlpha
                                invertedAlpha = 256 - blendingAlpha
                                do {
                                    bufferOffset++
                                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                        colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                        colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + ((colorRgb and 0xff00) * invertedAlpha shr 8 and 0xff00)
                                        pixel = pixels[bufferOffset]
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                        }

                                        if (writeDepth) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    red += slopeRed
                                    green += slopeGreen
                                    blue += slopeBlue
                                } while (--x > 0)
                            }
                        } else {
                            do {
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    pixels[bufferOffset] = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                    if (writeDepth) {
                                        depthBuf[bufferOffset] = depth
                                    }
                                }
                                depth += slopeDepth
                                red += slopeRed
                                green += slopeGreen
                                blue += slopeBlue
                            } while (--x > 0)
                        }
                    }
                } else if (smoothEdges) {
                    x = xB - xA shr 2
                    slopeRed *= 4.0f
                    slopeGreen *= 4.0f
                    slopeBlue *= 4.0f
                    if (blendingAlpha == 0) {
                        if (x > 0) {
                            do {
                                colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                red += slopeRed
                                green += slopeGreen
                                blue += slopeBlue
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    pixels[bufferOffset] = colorRgb
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    pixels[bufferOffset] = colorRgb
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    pixels[bufferOffset] = colorRgb
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    pixels[bufferOffset] = colorRgb
                                }
                                depth += slopeDepth
                            } while (--x > 0)
                        }
                        x = xB - xA and 0x3
                        if (x > 0) {
                            colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                            do {
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    pixels[bufferOffset] = colorRgb
                                }
                                depth += slopeDepth
                            } while (--x > 0)
                        }
                    } else if (!blending) {
                        alpha = blendingAlpha
                        invertedAlpha = 256 - blendingAlpha
                        if (x > 0) {
                            do {
                                colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                red += slopeRed
                                green += slopeGreen
                                blue += slopeBlue
                                colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + ((colorRgb and 0xff00) * invertedAlpha shr 8 and 0xff00)
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    pixel = pixels[bufferOffset]
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                    }
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    pixel = pixels[bufferOffset]
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                    }
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    pixel = pixels[bufferOffset]
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                    }
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    pixel = pixels[bufferOffset]
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                    }
                                }
                                depth += slopeDepth
                            } while (--x > 0)
                        }
                        x = xB - xA and 0x3
                        if (x > 0) {
                            colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                            colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + ((colorRgb and 0xff00) * invertedAlpha shr 8 and 0xff00)
                            do {
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    pixel = pixels[bufferOffset]
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                    }
                                }
                                depth += slopeDepth
                            } while (--x > 0)
                        }
                    } else {
                        if (x > 0) {
                            do {
                                colorRgb = (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                red += slopeRed
                                green += slopeGreen
                                blue += slopeBlue
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    var i_21 = pixels[bufferOffset]
                                    var i_20 = colorRgb + i_21
                                    var i_22 = (i_21 and 0xff00ff) + (colorRgb and 0xff00ff)
                                    i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                                    pixels[bufferOffset] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    var i_21 = pixels[bufferOffset]
                                    var i_20 = colorRgb + i_21
                                    var i_22 = (i_21 and 0xff00ff) + (colorRgb and 0xff00ff)
                                    i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                                    pixels[bufferOffset] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    var i_21 = pixels[bufferOffset]
                                    var i_20 = colorRgb + i_21
                                    var i_22 = (i_21 and 0xff00ff) + (colorRgb and 0xff00ff)
                                    i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                                    pixels[bufferOffset] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    var i_21 = pixels[bufferOffset]
                                    var i_20 = colorRgb + i_21
                                    var i_22 = (i_21 and 0xff00ff) + (colorRgb and 0xff00ff)
                                    i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                                    pixels[bufferOffset] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                                }
                                depth += slopeDepth
                            } while (--x > 0)
                        }
                        x = xB - xA and 0x3
                        if (x > 0) {
                            colorRgb = (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                            do {
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    var i_21 = pixels[bufferOffset]
                                    var i_20 = colorRgb + i_21
                                    var i_22 = (i_21 and 0xff00ff) + (colorRgb and 0xff00ff)
                                    i_21 = (i_22 and 0x1000100) + (i_20 - i_22 and 0x10000)
                                    pixels[bufferOffset] = 0xff000000.toInt() or (i_20 - i_21) or (i_21 - (i_21 ushr 8))
                                }
                                depth += slopeDepth
                            } while (--x > 0)
                        }
                    }
                } else {
                    x = xB - xA
                    if (blendingAlpha != 0) {
                        if (blending) {
                            do {
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    pixel = (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                    var currentPixel = pixels[bufferOffset]
                                    var pixelSum = pixel + currentPixel
                                    var i_22 = (pixel and 0xff00ff) + (currentPixel and 0xff00ff)
                                    currentPixel = (i_22 and 0x1000100) + (pixelSum - i_22 and 0x10000)
                                    pixels[bufferOffset] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
                                }
                                depth += slopeDepth
                                red += slopeRed
                                green += slopeGreen
                                blue += slopeBlue
                            } while (--x > 0)
                        } else {
                            alpha = blendingAlpha
                            invertedAlpha = 256 - blendingAlpha
                            do {
                                bufferOffset++
                                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                    colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                    colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + ((colorRgb and 0xff00) * invertedAlpha shr 8 and 0xff00)
                                    pixel = pixels[bufferOffset]
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                    }
                                }
                                depth += slopeDepth
                                red += slopeRed
                                green += slopeGreen
                                blue += slopeBlue
                            } while (--x > 0)
                        }
                    } else {
                        do {
                            bufferOffset++
                            if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                pixels[bufferOffset] = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                            }
                            depth += slopeDepth
                            red += slopeRed
                            green += slopeGreen
                            blue += slopeBlue
                        } while (--x > 0)
                    }
                }
            }
        }
    }

    fun drawMaterializedTriangle(
        colorWrite: Boolean, depthWrite: Boolean, transparent: Boolean,
        yA: Float, yB: Float, yC: Float,
        xA: Float, xB: Float, xC: Float,
        zNearA: Float, zNearB: Float, zNearC: Float,
        zFarA: Float, zFarB: Float, zFarC: Float,
        uA: Float, uB: Float, uC: Float,
        vA: Float, vB: Float, vC: Float,
        colorA: Int, colorB: Int, colorC: Int,
        fogColor: Int,
        fogDensityA: Float, fogDensityB: Float, fogDensityC: Float,
        materialId: Int
    ) {
        var yA = yA
        var yB = yB
        var yC = yC
        var xA = xA
        var xB = xB
        var xC = xC
        var zNearA = zNearA
        var zNearB = zNearB
        var zNearC = zNearC
        var zFarA = zFarA
        var zFarB = zFarB
        var zFarC = zFarC
        var uA = uA
        var uB = uB
        var uC = uC
        var vA = vA
        var vB = vB
        var vC = vC
        var colorA = colorA
        var colorB = colorB
        var colorC = colorC
        var fogDensityA = fogDensityA
        var fogDensityB = fogDensityB
        var fogDensityC = fogDensityC
        if (!colorWrite) {
            drawFlatTriangle(false, depthWrite, transparent, yA, yB, yC, xA, xB, xC, zNearA, zNearB, zNearC, 0)
        } else {
            if (materialId != textureA) {
                texturePixelsA = safemodeToolkit.getAnimatedMaterialArgb(materialId)
                if (texturePixelsA == null) {
                    blendingAlpha = 255 - (colorA shr 24 and 0xff)
                    val materialARgb = ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(safemodeToolkit.getMaterialColor(materialId)).toInt() and 0xffff]
                    val centerColour = 0xff000000.toInt() or (materialARgb shr 16 and 0xff) * (colorA shr 16 and 0xff) shl 8 and 0xff0000 or (colorA shr 8 and 0xff) * (materialARgb shr 8 and 0xff) and 0xff00 or (materialARgb and 0xff) * (colorA and 0xff) shr 8
                    drawTriangle(true, depthWrite, transparent, yA, yB, yC, xA, xB, xC, zNearA, zNearB, zNearC, ColorUtil.mixColorsWithTransparency(centerColour, fogColor, fogDensityA), ColorUtil.mixColorsWithTransparency(centerColour, fogColor, fogDensityB), ColorUtil.mixColorsWithTransparency(centerColour, fogColor, fogDensityC))
                    return
                }
                textureSizeA = if (safemodeToolkit.isSmallTexture(materialId)) 64 else safemodeToolkit.materialSize
                textureMaskA = textureSizeA - 1
                alphaMode = safemodeToolkit.getMaterialEffectCombiner(materialId)
                textureRepeat = safemodeToolkit.hasRepeat(materialId)
            }
            fogColour = fogColor
            if (yA > yB || yA > yC) {
                if (yB <= yC) {
                    var tmp1 = xA
                    xA = xB
                    xB = tmp1
                    tmp1 = yA
                    yA = yB
                    yB = tmp1
                    tmp1 = zNearA
                    zNearA = zNearB
                    zNearB = tmp1
                    tmp1 = uA
                    uA = uB
                    uB = tmp1
                    tmp1 = vA
                    vA = vB
                    vB = tmp1
                    tmp1 = zFarA
                    zFarA = zFarB
                    zFarB = tmp1
                    tmp1 = fogDensityA
                    fogDensityA = fogDensityB
                    fogDensityB = tmp1
                    var tmp2 = colorA
                    colorA = colorB
                    colorB = tmp2
                } else {
                    var tmp1 = xA
                    xA = xC
                    xC = tmp1
                    tmp1 = yA
                    yA = yC
                    yC = tmp1
                    tmp1 = zNearA
                    zNearA = zNearC
                    zNearC = tmp1
                    tmp1 = uA
                    uA = uC
                    uC = tmp1
                    tmp1 = vA
                    vA = vC
                    vC = tmp1
                    tmp1 = zFarA
                    zFarA = zFarC
                    zFarC = tmp1
                    tmp1 = fogDensityA
                    fogDensityA = fogDensityC
                    fogDensityC = tmp1
                    var tmp = colorA
                    colorA = colorC
                    colorC = tmp
                }
            }
            uA /= zFarA
            uB /= zFarB
            uC /= zFarC
            vA /= zFarA
            vB /= zFarB
            vC /= zFarC
            zNearA = 1.0f / zNearA
            zNearB = 1.0f / zNearB
            zNearC = 1.0f / zNearC
            zFarA = 1.0f / zFarA
            zFarB = 1.0f / zFarB
            zFarC = 1.0f / zFarC
            var colorAAlpha = (colorA shr 24 and 0xff).toFloat()
            var colorBAlpha = (colorB shr 24 and 0xff).toFloat()
            var colorCAlpha = (colorC shr 24 and 0xff).toFloat()
            var colorARed = (colorA shr 16 and 0xff).toFloat()
            var colorBRed = (colorB shr 16 and 0xff).toFloat()
            var colorCRed = (colorC shr 16 and 0xff).toFloat()
            var colorAGreen = (colorA shr 8 and 0xff).toFloat()
            var colorBGreen = (colorB shr 8 and 0xff).toFloat()
            var colorCGreen = (colorC shr 8 and 0xff).toFloat()
            var colorABlue = (colorA and 0xff).toFloat()
            var colorBBlue = (colorB and 0xff).toFloat()
            var colorCBlue = (colorC and 0xff).toFloat()
            var slopeXBa = 0.0f
            var slopeZnearBa = 0.0f
            var slopeZfarBa = 0.0f
            var slopeUBa = 0.0f
            var slopeVBa = 0.0f
            var slopeFogDensityBa = 0.0f
            var slopeAlphaBa = 0.0f
            var slopeRedBa = 0.0f
            var slopeGreenBa = 0.0f
            var slopeBlueBa = 0.0f
            if (yB != yA) {
                val lengthDeltaYAb = yB - yA
                slopeXBa = (xB - xA) / lengthDeltaYAb
                slopeZnearBa = (zNearB - zNearA) / lengthDeltaYAb
                slopeZfarBa = (zFarB - zFarA) / lengthDeltaYAb
                slopeUBa = (uB - uA) / lengthDeltaYAb
                slopeVBa = (vB - vA) / lengthDeltaYAb
                slopeFogDensityBa = (fogDensityB - fogDensityA) / lengthDeltaYAb
                slopeAlphaBa = (colorBAlpha - colorAAlpha) / lengthDeltaYAb
                slopeRedBa = (colorBRed - colorARed) / lengthDeltaYAb
                slopeGreenBa = (colorBGreen - colorAGreen) / lengthDeltaYAb
                slopeBlueBa = (colorBBlue - colorABlue) / lengthDeltaYAb
            }
            var slopeXCb = 0.0f
            var slopeZnearCb = 0.0f
            var slopeZfarCb = 0.0f
            var slopeUCb = 0.0f
            var slopeVCb = 0.0f
            var slopeFogDensityCb = 0.0f
            var slopeAlphaCb = 0.0f
            var slopeRedCb = 0.0f
            var slopeGreenCb = 0.0f
            var slopeBlueCb = 0.0f
            if (yC != yB) {
                val lengthDeltaYCb = yC - yB
                slopeXCb = (xC - xB) / lengthDeltaYCb
                slopeZnearCb = (zNearC - zNearB) / lengthDeltaYCb
                slopeZfarCb = (zFarC - zFarB) / lengthDeltaYCb
                slopeUCb = (uC - uB) / lengthDeltaYCb
                slopeVCb = (vC - vB) / lengthDeltaYCb
                slopeFogDensityCb = (fogDensityC - fogDensityB) / lengthDeltaYCb
                slopeAlphaCb = (colorCAlpha - colorBAlpha) / lengthDeltaYCb
                slopeRedCb = (colorCRed - colorBRed) / lengthDeltaYCb
                slopeGreenCb = (colorCGreen - colorBGreen) / lengthDeltaYCb
                slopeBlueCb = (colorCBlue - colorBBlue) / lengthDeltaYCb
            }
            var slopeXAc = 0.0f
            var slopeZnearAc = 0.0f
            var slopeZfarAc = 0.0f
            var slopeUAc = 0.0f
            var slopeVAc = 0.0f
            var slopeFogDensityAc = 0.0f
            var slopeAlphaAc = 0.0f
            var slopeRedAc = 0.0f
            var slopeGreenAc = 0.0f
            var slopeBlueAc = 0.0f
            if (yA != yC) {
                val lengthDeltaYAc = yA - yC
                slopeXAc = (xA - xC) / lengthDeltaYAc
                slopeZnearAc = (zNearA - zNearC) / lengthDeltaYAc
                slopeZfarAc = (zFarA - zFarC) / lengthDeltaYAc
                slopeUAc = (uA - uC) / lengthDeltaYAc
                slopeVAc = (vA - vC) / lengthDeltaYAc
                slopeFogDensityAc = (fogDensityA - fogDensityC) / lengthDeltaYAc
                slopeAlphaAc = (colorAAlpha - colorCAlpha) / lengthDeltaYAc
                slopeRedAc = (colorARed - colorCRed) / lengthDeltaYAc
                slopeGreenAc = (colorAGreen - colorCGreen) / lengthDeltaYAc
                slopeBlueAc = (colorABlue - colorCBlue) / lengthDeltaYAc
            }
            if (yA < clipHeight) {
                if (yB > clipHeight) {
                    yB = clipHeight.toFloat()
                }
                if (yC > clipHeight) {
                    yC = clipHeight.toFloat()
                }
                if (yB < yC) {
                    xC = xA
                    zNearC = zNearA
                    zFarC = zFarA
                    uC = uA
                    vC = vA
                    fogDensityC = fogDensityA
                    colorCAlpha = colorAAlpha
                    colorCRed = colorARed
                    colorCGreen = colorAGreen
                    colorCBlue = colorABlue
                    if (yA < 0.0f) {
                        xA -= slopeXBa * yA
                        xC -= slopeXAc * yA
                        zNearA -= slopeZnearBa * yA
                        zNearC -= slopeZnearAc * yA
                        zFarA -= slopeZfarBa * yA
                        zFarC -= slopeZfarAc * yA
                        uA -= slopeUBa * yA
                        uC -= slopeUAc * yA
                        vA -= slopeVBa * yA
                        vC -= slopeVAc * yA
                        fogDensityA -= slopeFogDensityBa * yA
                        fogDensityC -= slopeFogDensityAc * yA
                        colorAAlpha -= slopeAlphaBa * yA
                        colorCAlpha -= slopeAlphaAc * yA
                        colorARed -= slopeRedBa * yA
                        colorCRed -= slopeRedAc * yA
                        colorAGreen -= slopeGreenBa * yA
                        colorCGreen -= slopeGreenAc * yA
                        colorABlue -= slopeBlueBa * yA
                        colorCBlue -= slopeBlueAc * yA
                        yA = 0.0f
                    }
                    if (yB < 0.0f) {
                        xB -= slopeXCb * yB
                        zNearB -= slopeZnearCb * yB
                        zFarB -= slopeZfarCb * yB
                        uB -= slopeUCb * yB
                        vB -= slopeVCb * yB
                        fogDensityB -= slopeFogDensityCb * yB
                        colorBAlpha -= slopeAlphaCb * yB
                        colorBRed -= slopeRedCb * yB
                        colorBGreen -= slopeGreenCb * yB
                        colorBBlue -= slopeBlueCb * yB
                        yB = 0.0f
                    }
                    if (yA != yB && slopeXAc < slopeXBa || yA == yB && slopeXAc > slopeXCb) {
                        yA = (yA + 0.5f).toInt().toFloat()
                        yB = (yB + 0.5f).toInt().toFloat()
                        yC = (yC + 0.5f).toInt().toFloat() - yB
                        yB -= yA
                        yA = scanlineOffsets[yA.toInt()].toFloat()
                        while (--yB >= 0.0f) {
                            drawMaterialisedScanline(depthWrite, transparent, colourBuffer!!, yA.toInt(), xC.toInt(), xA.toInt(), zNearC, zNearA, zFarC, zFarA, uC, uA, vC, vA, fogDensityC, fogDensityA, colorCAlpha, colorAAlpha, colorCRed, colorARed, colorCGreen, colorAGreen, colorCBlue, colorABlue)
                            xA += slopeXBa
                            xC += slopeXAc
                            zNearA += slopeZnearBa
                            zNearC += slopeZnearAc
                            zFarA += slopeZfarBa
                            zFarC += slopeZfarAc
                            uA += slopeUBa
                            uC += slopeUAc
                            vA += slopeVBa
                            vC += slopeVAc
                            fogDensityA += slopeFogDensityBa
                            fogDensityC += slopeFogDensityAc
                            colorAAlpha += slopeAlphaBa
                            colorCAlpha += slopeAlphaAc
                            colorARed += slopeRedBa
                            colorCRed += slopeRedAc
                            colorAGreen += slopeGreenBa
                            colorCGreen += slopeGreenAc
                            colorABlue += slopeBlueBa
                            colorCBlue += slopeBlueAc
                            yA += scanlineWidth
                        }
                        while (--yC >= 0.0f) {
                            drawMaterialisedScanline(depthWrite, transparent, colourBuffer!!, yA.toInt(), xC.toInt(), xB.toInt(), zNearC, zNearB, zFarC, zFarB, uC, uB, vC, vB, fogDensityC, fogDensityB, colorCAlpha, colorBAlpha, colorCRed, colorBRed, colorCGreen, colorBGreen, colorCBlue, colorBBlue)
                            xB += slopeXCb
                            xC += slopeXAc
                            zNearB += slopeZnearCb
                            zNearC += slopeZnearAc
                            zFarB += slopeZfarCb
                            zFarC += slopeZfarAc
                            uB += slopeUCb
                            uC += slopeUAc
                            vB += slopeVCb
                            vC += slopeVAc
                            fogDensityB += slopeFogDensityCb
                            fogDensityC += slopeFogDensityAc
                            colorBAlpha += slopeAlphaCb
                            colorCAlpha += slopeAlphaAc
                            colorBRed += slopeRedCb
                            colorCRed += slopeRedAc
                            colorBGreen += slopeGreenCb
                            colorCGreen += slopeGreenAc
                            colorBBlue += slopeBlueCb
                            colorCBlue += slopeBlueAc
                            yA += scanlineWidth
                        }
                    } else {
                        yA = (yA + 0.5f).toInt().toFloat()
                        yB = (yB + 0.5f).toInt().toFloat()
                        yC = (yC + 0.5f).toInt().toFloat() - yB
                        yB -= yA
                        yA = scanlineOffsets[yA.toInt()].toFloat()
                        while (--yB >= 0.0f) {
                            drawMaterialisedScanline(depthWrite, transparent, colourBuffer!!, yA.toInt(), xA.toInt(), xC.toInt(), zNearA, zNearC, zFarA, zFarC, uA, uC, vA, vC, fogDensityA, fogDensityC, colorAAlpha, colorCAlpha, colorARed, colorCRed, colorAGreen, colorCGreen, colorABlue, colorCBlue)
                            xA += slopeXBa
                            xC += slopeXAc
                            zNearA += slopeZnearBa
                            zNearC += slopeZnearAc
                            zFarA += slopeZfarBa
                            zFarC += slopeZfarAc
                            uA += slopeUBa
                            uC += slopeUAc
                            vA += slopeVBa
                            vC += slopeVAc
                            fogDensityA += slopeFogDensityBa
                            fogDensityC += slopeFogDensityAc
                            colorAAlpha += slopeAlphaBa
                            colorCAlpha += slopeAlphaAc
                            colorARed += slopeRedBa
                            colorCRed += slopeRedAc
                            colorAGreen += slopeGreenBa
                            colorCGreen += slopeGreenAc
                            colorABlue += slopeBlueBa
                            colorCBlue += slopeBlueAc
                            yA += scanlineWidth
                        }
                        while (--yC >= 0.0f) {
                            drawMaterialisedScanline(depthWrite, transparent, colourBuffer!!, yA.toInt(), xB.toInt(), xC.toInt(), zNearB, zNearC, zFarB, zFarC, uB, uC, vB, vC, fogDensityB, fogDensityC, colorBAlpha, colorCAlpha, colorBRed, colorCRed, colorBGreen, colorCGreen, colorBBlue, colorCBlue)
                            xB += slopeXCb
                            xC += slopeXAc
                            zNearB += slopeZnearCb
                            zNearC += slopeZnearAc
                            zFarB += slopeZfarCb
                            zFarC += slopeZfarAc
                            uB += slopeUCb
                            uC += slopeUAc
                            vB += slopeVCb
                            vC += slopeVAc
                            fogDensityB += slopeFogDensityCb
                            fogDensityC += slopeFogDensityAc
                            colorBAlpha += slopeAlphaCb
                            colorCAlpha += slopeAlphaAc
                            colorBRed += slopeRedCb
                            colorCRed += slopeRedAc
                            colorBGreen += slopeGreenCb
                            colorCGreen += slopeGreenAc
                            colorBBlue += slopeBlueCb
                            colorCBlue += slopeBlueAc
                            yA += scanlineWidth
                        }
                    }
                } else {
                    xB = xA
                    zNearB = zNearA
                    zFarB = zFarA
                    uB = uA
                    vB = vA
                    fogDensityB = fogDensityA
                    colorBAlpha = colorAAlpha
                    colorBRed = colorARed
                    colorBGreen = colorAGreen
                    colorBBlue = colorABlue
                    if (yA < 0.0f) {
                        xA -= slopeXBa * yA
                        xB -= slopeXAc * yA
                        zNearA -= slopeZnearBa * yA
                        zNearB -= slopeZnearAc * yA
                        zFarA -= slopeZfarBa * yA
                        zFarB -= slopeZfarAc * yA
                        uA -= slopeUBa * yA
                        uB -= slopeUAc * yA
                        vA -= slopeVBa * yA
                        vB -= slopeVAc * yA
                        fogDensityA -= slopeFogDensityBa * yA
                        fogDensityB -= slopeFogDensityAc * yA
                        colorAAlpha -= slopeAlphaBa * yA
                        colorBAlpha -= slopeAlphaAc * yA
                        colorARed -= slopeRedBa * yA
                        colorBRed -= slopeRedAc * yA
                        colorAGreen -= slopeGreenBa * yA
                        colorBGreen -= slopeGreenAc * yA
                        colorABlue -= slopeBlueBa * yA
                        colorBBlue -= slopeBlueAc * yA
                        yA = 0.0f
                    }
                    if (yC < 0.0f) {
                        xC -= slopeXCb * yC
                        zNearC -= slopeZnearCb * yC
                        zFarC -= slopeZfarCb * yC
                        uC -= slopeUCb * yC
                        vC -= slopeVCb * yC
                        fogDensityC -= slopeFogDensityCb * yC
                        colorCAlpha -= slopeAlphaCb * yC
                        colorCRed -= slopeRedCb * yC
                        colorCGreen -= slopeGreenCb * yC
                        colorCBlue -= slopeBlueCb * yC
                        yC = 0.0f
                    }
                    if ((yA == yC || slopeXAc >= slopeXBa) && (yA != yC || slopeXCb <= slopeXBa)) {
                        yA = (yA + 0.5f).toInt().toFloat()
                        yC = (yC + 0.5f).toInt().toFloat()
                        yB = (yB + 0.5f).toInt().toFloat() - yC
                        yC -= yA
                        yA = scanlineOffsets[yA.toInt()].toFloat()
                        while (--yC >= 0.0f) {
                            drawMaterialisedScanline(depthWrite, transparent, colourBuffer!!, yA.toInt(), xA.toInt(), xB.toInt(), zNearA, zNearB, zFarA, zFarB, uA, uB, vA, vB, fogDensityA, fogDensityB, colorAAlpha, colorBAlpha, colorARed, colorBRed, colorAGreen, colorBGreen, colorABlue, colorBBlue)
                            xB += slopeXAc
                            xA += slopeXBa
                            zNearB += slopeZnearAc
                            zNearA += slopeZnearBa
                            zFarB += slopeZfarAc
                            zFarA += slopeZfarBa
                            uB += slopeUAc
                            uA += slopeUBa
                            vB += slopeVAc
                            vA += slopeVBa
                            fogDensityB += slopeFogDensityAc
                            fogDensityA += slopeFogDensityBa
                            colorBAlpha += slopeAlphaAc
                            colorAAlpha += slopeAlphaBa
                            colorBRed += slopeRedAc
                            colorARed += slopeRedBa
                            colorBGreen += slopeGreenAc
                            colorAGreen += slopeGreenBa
                            colorBBlue += slopeBlueAc
                            colorABlue += slopeBlueBa
                            yA += scanlineWidth
                        }
                        while (--yB >= 0.0f) {
                            drawMaterialisedScanline(depthWrite, transparent, colourBuffer!!, yA.toInt(), xA.toInt(), xC.toInt(), zNearA, zNearC, zFarA, zFarC, uA, uC, vA, vC, fogDensityA, fogDensityC, colorAAlpha, colorCAlpha, colorARed, colorCRed, colorAGreen, colorCGreen, colorABlue, colorCBlue)
                            xA += slopeXBa
                            xC += slopeXCb
                            zNearA += slopeZnearBa
                            zNearC += slopeZnearCb
                            zFarA += slopeZfarBa
                            zFarC += slopeZfarCb
                            uA += slopeUBa
                            uC += slopeUCb
                            vA += slopeVBa
                            vC += slopeVCb
                            fogDensityA += slopeFogDensityBa
                            fogDensityC += slopeFogDensityCb
                            colorAAlpha += slopeAlphaBa
                            colorCAlpha += slopeAlphaCb
                            colorARed += slopeRedBa
                            colorCRed += slopeRedCb
                            colorAGreen += slopeGreenBa
                            colorCGreen += slopeGreenCb
                            colorABlue += slopeBlueBa
                            colorCBlue += slopeBlueCb
                            yA += scanlineWidth
                        }
                    } else {
                        yA = (yA + 0.5f).toInt().toFloat()
                        yC = (yC + 0.5f).toInt().toFloat()
                        yB = (yB + 0.5f).toInt().toFloat() - yC
                        yC -= yA
                        yA = scanlineOffsets[yA.toInt()].toFloat()
                        while (--yC >= 0.0f) {
                            drawMaterialisedScanline(depthWrite, transparent, colourBuffer!!, yA.toInt(), xB.toInt(), xA.toInt(), zNearB, zNearA, zFarB, zFarA, uB, uA, vB, vA, fogDensityB, fogDensityA, colorBAlpha, colorAAlpha, colorBRed, colorARed, colorBGreen, colorAGreen, colorBBlue, colorABlue)
                            xA += slopeXBa
                            xB += slopeXAc
                            zNearA += slopeZnearBa
                            zNearB += slopeZnearAc
                            zFarA += slopeZfarBa
                            zFarB += slopeZfarAc
                            uA += slopeUBa
                            uB += slopeUAc
                            vA += slopeVBa
                            vB += slopeVAc
                            fogDensityA += slopeFogDensityBa
                            fogDensityB += slopeFogDensityAc
                            colorAAlpha += slopeAlphaBa
                            colorBAlpha += slopeAlphaAc
                            colorARed += slopeRedBa
                            colorBRed += slopeRedAc
                            colorAGreen += slopeGreenBa
                            colorBGreen += slopeGreenAc
                            colorABlue += slopeBlueBa
                            colorBBlue += slopeBlueAc
                            yA += scanlineWidth
                        }
                        while (--yB >= 0.0f) {
                            drawMaterialisedScanline(depthWrite, transparent, colourBuffer!!, yA.toInt(), xC.toInt(), xA.toInt(), zNearC, zNearA, zFarC, zFarA, uC, uA, vC, vA, fogDensityC, fogDensityA, colorCAlpha, colorAAlpha, colorCRed, colorARed, colorCGreen, colorAGreen, colorCBlue, colorABlue)
                            xC += slopeXCb
                            xA += slopeXBa
                            zNearC += slopeZnearCb
                            zNearA += slopeZnearBa
                            zFarC += slopeZfarCb
                            zFarA += slopeZfarBa
                            uC += slopeUCb
                            uA += slopeUBa
                            vC += slopeVCb
                            vA += slopeVBa
                            fogDensityC += slopeFogDensityCb
                            fogDensityA += slopeFogDensityBa
                            colorCAlpha += slopeAlphaCb
                            colorAAlpha += slopeAlphaBa
                            colorCRed += slopeRedCb
                            colorARed += slopeRedBa
                            colorCGreen += slopeGreenCb
                            colorAGreen += slopeGreenBa
                            colorCBlue += slopeBlueCb
                            colorABlue += slopeBlueBa
                            yA += scanlineWidth
                        }
                    }
                }
            }
        }
    }

    fun drawTriangle(
        colorWrite: Boolean, depthWrite: Boolean, transparency: Boolean,
        yA: Float, yB: Float, yC: Float,
        xA: Float, xB: Float, xC: Float,
        zA: Float, zB: Float, zC: Float,
        colorA: Int, colorB: Int, colorC: Int
    ) {
        var yA = yA
        var yB = yB
        var yC = yC
        var xA = xA
        var xB = xB
        var xC = xC
        var zA = zA
        var zB = zB
        var zC = zC

        if (!colorWrite) {
            drawFlatTriangle(false, depthWrite, transparency, yA, yB, yC, xA, xB, xC, zA, zB, zC, 0)
        } else if (wireframeMode) {
            safemodeToolkit.drawOpaqueLine(xA.toInt(), yA.toInt(), xB.toInt(), yB.toInt(), 0xff000000.toInt() or colorA)
            safemodeToolkit.drawOpaqueLine(xB.toInt(), yB.toInt(), xC.toInt(), yC.toInt(), 0xff000000.toInt() or colorA)
            safemodeToolkit.drawOpaqueLine(xC.toInt(), yC.toInt(), xA.toInt(), yA.toInt(), 0xff000000.toInt() or colorA)
        } else {
            val slopeXBa = xB - xA
            val slopeYBa = yB - yA
            val slopeXCa = xC - xA
            val slopeYCa = yC - yA
            val slopeZBa = zB - zA
            val slopeZCa = zC - zA
            val colorRedBa = ((colorB and 0xff0000) - (colorA and 0xff0000)).toFloat()
            val colorRedCa = ((colorC and 0xff0000) - (colorA and 0xff0000)).toFloat()
            val colorGreenBa = ((colorB and 0xff00) - (colorA and 0xff00)).toFloat()
            val colorGreenCa = ((colorC and 0xff00) - (colorA and 0xff00)).toFloat()
            val colorBlueBa = ((colorB and 0xff) - (colorA and 0xff)).toFloat()
            val colorBlueCa = ((colorC and 0xff) - (colorA and 0xff)).toFloat()
            val scaleCb = if (yC != yB) (xC - xB) / (yC - yB) else 0.0f
            val scaleBa = if (yB != yA) slopeXBa / slopeYBa else 0.0f
            val scaleCa = if (yC != yA) slopeXCa / slopeYCa else 0.0f
            val f31 = slopeXBa * slopeYCa - slopeXCa * slopeYBa
            if (f31 != 0.0f) {
                val f32 = (slopeZBa * slopeYCa - slopeZCa * slopeYBa) / f31
                val f33 = (slopeZCa * slopeXBa - slopeZBa * slopeXCa) / f31
                val f34 = (colorRedBa * slopeYCa - colorRedCa * slopeYBa) / f31
                val f35 = (colorRedCa * slopeXBa - colorRedBa * slopeXCa) / f31
                val f36 = (colorGreenBa * slopeYCa - colorGreenCa * slopeYBa) / f31
                val f37 = (colorGreenCa * slopeXBa - colorGreenBa * slopeXCa) / f31
                val f38 = (colorBlueBa * slopeYCa - colorBlueCa * slopeYBa) / f31
                val f39 = (colorBlueCa * slopeXBa - colorBlueBa * slopeXCa) / f31
                var f40: Float
                var f41: Float
                var f42: Float
                val colourBuf = colourBuffer!!
                if (yA <= yB && yA <= yC) {
                    if (yA < clipHeight) {
                        if (yB > clipHeight) yB = clipHeight.toFloat()
                        if (yC > clipHeight) yC = clipHeight.toFloat()
                        zA = zA - f32 * xA + f32
                        f41 = (colorA and 0xff0000) - f34 * xA + f34
                        f42 = (colorA and 0xff00) - f36 * xA + f36
                        f40 = (colorA and 0xff) - f38 * xA + f38
                        if (yB < yC) {
                            xC = xA
                            if (yA < 0.0f) {
                                xC = xA - scaleCa * yA
                                xA -= scaleBa * yA
                                zA -= f33 * yA
                                f41 -= f35 * yA
                                f42 -= f37 * yA
                                f40 -= f39 * yA
                                yA = 0.0f
                            }
                            if (yB < 0.0f) {
                                xB -= scaleCb * yB
                                yB = 0.0f
                            }
                            if ((yA == yB || scaleCa >= scaleBa) && (yA != yB || scaleCa <= scaleCb)) {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt() - yB
                                yB -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yB >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yA.toInt(), xA.toInt(), xC.toInt(), zA, f32, f41, f34, f42, f36, f40, f38)
                                    xC += scaleCa
                                    xA += scaleBa
                                    zA += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yA += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yA.toInt(), xB.toInt(), xC.toInt(), zA, f32, f41, f34, f42, f36, f40, f38)
                                    xC += scaleCa
                                    xB += scaleCb
                                    zA += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yA += scanlineWidth
                                }
                            } else {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt() - yB
                                yB -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yB >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yA.toInt(), xC.toInt(), xA.toInt(), zA, f32, f41, f34, f42, f36, f40, f38)
                                    xC += scaleCa
                                    xA += scaleBa
                                    zA += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yA += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yA.toInt(), xC.toInt(), xB.toInt(), zA, f32, f41, f34, f42, f36, f40, f38)
                                    xC += scaleCa
                                    xB += scaleCb
                                    zA += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yA += scanlineWidth
                                }
                            }
                        } else {
                            xB = xA
                            if (yA < 0.0f) {
                                xB = xA - scaleCa * yA
                                xA -= scaleBa * yA
                                zA -= f33 * yA
                                f41 -= f35 * yA
                                f42 -= f37 * yA
                                f40 -= f39 * yA
                                yA = 0.0f
                            }
                            if (yC < 0.0f) {
                                xC -= scaleCb * yC
                                yC = 0.0f
                            }
                            if (yA != yC && scaleCa < scaleBa || yA == yC && scaleCb > scaleBa) {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt() - yC
                                yC -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yA.toInt(), xB.toInt(), xA.toInt(), zA, f32, f41, f34, f42, f36, f40, f38)
                                    xB += scaleCa
                                    xA += scaleBa
                                    zA += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yA += scanlineWidth
                                }
                                while (--yB >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yA.toInt(), xC.toInt(), xA.toInt(), zA, f32, f41, f34, f42, f36, f40, f38)
                                    xC += scaleCb
                                    xA += scaleBa
                                    zA += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yA += scanlineWidth
                                }
                            } else {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt() - yC
                                yC -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yA.toInt(), xA.toInt(), xB.toInt(), zA, f32, f41, f34, f42, f36, f40, f38)
                                    xB += scaleCa
                                    xA += scaleBa
                                    zA += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yA += scanlineWidth
                                }
                                while (--yB >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yA.toInt(), xA.toInt(), xC.toInt(), zA, f32, f41, f34, f42, f36, f40, f38)
                                    xC += scaleCb
                                    xA += scaleBa
                                    zA += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yA += scanlineWidth
                                }
                            }
                        }
                    }
                } else if (yB <= yC) {
                    if (yB < clipHeight) {
                        if (yC > clipHeight) yC = clipHeight.toFloat()
                        if (yA > clipHeight) yA = clipHeight.toFloat()
                        zB = zB - f32 * xB + f32
                        f41 = (colorB and 0xff0000) - f34 * xB + f34
                        f42 = (colorB and 0xff00) - f36 * xB + f36
                        f40 = (colorB and 0xff) - f38 * xB + f38
                        if (yC < yA) {
                            xA = xB
                            if (yB < 0.0f) {
                                xA = xB - scaleBa * yB
                                xB -= scaleCb * yB
                                zB -= f33 * yB
                                f41 -= f35 * yB
                                f42 -= f37 * yB
                                f40 -= f39 * yB
                                yB = 0.0f
                            }
                            if (yC < 0.0f) {
                                xC -= scaleCa * yC
                                yC = 0.0f
                            }
                            if ((yB == yC || scaleBa >= scaleCb) && (yB != yC || scaleBa <= scaleCa)) {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt() - yC
                                yC -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yB.toInt(), xB.toInt(), xA.toInt(), zB, f32, f41, f34, f42, f36, f40, f38)
                                    xA += scaleBa
                                    xB += scaleCb
                                    zB += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yB += scanlineWidth
                                }
                                while (--yA >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yB.toInt(), xC.toInt(), xA.toInt(), zB, f32, f41, f34, f42, f36, f40, f38)
                                    xA += scaleBa
                                    xC += scaleCa
                                    zB += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yB += scanlineWidth
                                }
                            } else {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt() - yC
                                yC -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yB.toInt(), xA.toInt(), xB.toInt(), zB, f32, f41, f34, f42, f36, f40, f38)
                                    xA += scaleBa
                                    xB += scaleCb
                                    zB += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yB += scanlineWidth
                                }
                                while (--yA >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yB.toInt(), xA.toInt(), xC.toInt(), zB, f32, f41, f34, f42, f36, f40, f38)
                                    xA += scaleBa
                                    xC += scaleCa
                                    zB += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yB += scanlineWidth
                                }
                            }
                        } else {
                            xC = xB
                            if (yB < 0.0f) {
                                xC = xB - scaleBa * yB
                                xB -= scaleCb * yB
                                zB -= f33 * yB
                                f41 -= f35 * yB
                                f42 -= f37 * yB
                                f40 -= f39 * yB
                                yB = 0.0f
                            }
                            if (yA < 0.0f) {
                                xA -= scaleCa * yA
                                yA = 0.0f
                            }
                            if (scaleBa < scaleCb) {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt() - yA
                                yA -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yA >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yB.toInt(), xC.toInt(), xB.toInt(), zB, f32, f41, f34, f42, f36, f40, f38)
                                    xC += scaleBa
                                    xB += scaleCb
                                    zB += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yB += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yB.toInt(), xA.toInt(), xB.toInt(), zB, f32, f41, f34, f42, f36, f40, f38)
                                    xA += scaleCa
                                    xB += scaleCb
                                    zB += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yB += scanlineWidth
                                }
                            } else {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt() - yA
                                yA -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yA >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yB.toInt(), xB.toInt(), xC.toInt(), zB, f32, f41, f34, f42, f36, f40, f38)
                                    xC += scaleBa
                                    xB += scaleCb
                                    zB += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yB += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparency, colourBuf, yB.toInt(), xB.toInt(), xA.toInt(), zB, f32, f41, f34, f42, f36, f40, f38)
                                    xA += scaleCa
                                    xB += scaleCb
                                    zB += f33
                                    f41 += f35
                                    f42 += f37
                                    f40 += f39
                                    yB += scanlineWidth
                                }
                            }
                        }
                    }
                } else if (yC < clipHeight) {
                    if (yA > clipHeight) yA = clipHeight.toFloat()
                    if (yB > clipHeight) yB = clipHeight.toFloat()
                    zC = zC - f32 * xC + f32
                    f41 = (colorC and 0xff0000) - f34 * xC + f34
                    f42 = (colorC and 0xff00) - f36 * xC + f36
                    f40 = (colorC and 0xff) - f38 * xC + f38
                    if (yA < yB) {
                        xB = xC
                        if (yC < 0.0f) {
                            xB = xC - scaleCb * yC
                            xC -= scaleCa * yC
                            zC -= f33 * yC
                            f41 -= f35 * yC
                            f42 -= f37 * yC
                            f40 -= f39 * yC
                            yC = 0.0f
                        }
                        if (yA < 0.0f) {
                            xA -= scaleBa * yA
                            yA = 0.0f
                        }
                        if (scaleCb < scaleCa) {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt() - yA
                            yA -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yA >= 0.0f) {
                                drawScanline(depthWrite, transparency, colourBuf, yC.toInt(), xB.toInt(), xC.toInt(), zC, f32, f41, f34, f42, f36, f40, f38)
                                xB += scaleCb
                                xC += scaleCa
                                zC += f33
                                f41 += f35
                                f42 += f37
                                f40 += f39
                                yC += scanlineWidth
                            }
                            while (--yB >= 0.0f) {
                                drawScanline(depthWrite, transparency, colourBuf, yC.toInt(), xB.toInt(), xA.toInt(), zC, f32, f41, f34, f42, f36, f40, f38)
                                xB += scaleCb
                                xA += scaleBa
                                zC += f33
                                f41 += f35
                                f42 += f37
                                f40 += f39
                                yC += scanlineWidth
                            }
                        } else {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt() - yA
                            yA -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yA >= 0.0f) {
                                drawScanline(depthWrite, transparency, colourBuf, yC.toInt(), xC.toInt(), xB.toInt(), zC, f32, f41, f34, f42, f36, f40, f38)
                                xB += scaleCb
                                xC += scaleCa
                                zC += f33
                                f41 += f35
                                f42 += f37
                                f40 += f39
                                yC += scanlineWidth
                            }
                            while (--yB >= 0.0f) {
                                drawScanline(depthWrite, transparency, colourBuf, yC.toInt(), xA.toInt(), xB.toInt(), zC, f32, f41, f34, f42, f36, f40, f38)
                                xB += scaleCb
                                xA += scaleBa
                                zC += f33
                                f41 += f35
                                f42 += f37
                                f40 += f39
                                yC += scanlineWidth
                            }
                        }
                    } else {
                        xA = xC
                        if (yC < 0.0f) {
                            xA = xC - scaleCb * yC
                            xC -= scaleCa * yC
                            zC -= f33 * yC
                            f41 -= f35 * yC
                            f42 -= f37 * yC
                            f40 -= f39 * yC
                            yC = 0.0f
                        }
                        if (yB < 0.0f) {
                            xB -= scaleBa * yB
                            yB = 0.0f
                        }
                        if (scaleCb < scaleCa) {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt() - yB
                            yB -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yB >= 0.0f) {
                                drawScanline(depthWrite, transparency, colourBuf, yC.toInt(), xA.toInt(), xC.toInt(), zC, f32, f41, f34, f42, f36, f40, f38)
                                xA += scaleCb
                                xC += scaleCa
                                zC += f33
                                f41 += f35
                                f42 += f37
                                f40 += f39
                                yC += scanlineWidth
                            }
                            while (--yA >= 0.0f) {
                                drawScanline(depthWrite, transparency, colourBuf, yC.toInt(), xB.toInt(), xC.toInt(), zC, f32, f41, f34, f42, f36, f40, f38)
                                xB += scaleBa
                                xC += scaleCa
                                zC += f33
                                f41 += f35
                                f42 += f37
                                f40 += f39
                                yC += scanlineWidth
                            }
                        } else {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt() - yB
                            yB -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yB >= 0.0f) {
                                drawScanline(depthWrite, transparency, colourBuf, yC.toInt(), xC.toInt(), xA.toInt(), zC, f32, f41, f34, f42, f36, f40, f38)
                                xA += scaleCb
                                xC += scaleCa
                                zC += f33
                                f41 += f35
                                f42 += f37
                                f40 += f39
                                yC += scanlineWidth
                            }
                            while (--yA >= 0.0f) {
                                drawScanline(depthWrite, transparency, colourBuf, yC.toInt(), xC.toInt(), xB.toInt(), zC, f32, f41, f34, f42, f36, f40, f38)
                                xB += scaleBa
                                xC += scaleCa
                                zC += f33
                                f41 += f35
                                f42 += f37
                                f40 += f39
                                yC += scanlineWidth
                            }
                        }
                    }
                }
            }
        }
    }

    fun drawShadedTriangle(
        colorWrite: Boolean, depthWrite: Boolean, transparent: Boolean,
        yA: Float, yB: Float, yC: Float,
        xA: Float, xB: Float, xC: Float,
        zA: Float, zB: Float, zC: Float,
        colorA: Float, colorB: Float, colorC: Float
    ) {
        var yA = yA
        var yB = yB
        var yC = yC
        var xA = xA
        var xB = xB
        var xC = xC
        var zA = zA
        var zB = zB
        var zC = zC
        var colorA = colorA
        var colorB = colorB
        var colorC = colorC
        val colourBuffer = colourBuffer!!
        if (!colorWrite) {
            drawFlatTriangle(false, depthWrite, transparent, yA, yB, yC, xA, xB, xC, zA, zB, zC, 0)
        } else if (wireframeMode) {
            safemodeToolkit.drawOpaqueLine(xA.toInt(), yA.toInt(), xB.toInt(), yB.toInt(), ColorUtil.hsvToRgbTable!![colorA.toInt() and 0xffff])
            safemodeToolkit.drawOpaqueLine(xB.toInt(), yB.toInt(), xC.toInt(), yC.toInt(), ColorUtil.hsvToRgbTable!![colorA.toInt() and 0xffff])
            safemodeToolkit.drawOpaqueLine(xC.toInt(), yC.toInt(), xA.toInt(), yA.toInt(), ColorUtil.hsvToRgbTable!![colorA.toInt() and 0xffff])
        } else {
            var slopeXBa = xB - xA
            var slopeYBa = yB - yA
            var slopeXCa = xC - xA
            var slopeYCa = yC - yA
            var slopeColorABa = colorB - colorA
            var slopeColorBBa = colorC - colorA
            var slopeZBa = zB - zA
            var slopeZCa = zC - zA
            var scaleXCb: Float
            if (yC != yB) {
                scaleXCb = (xC - xB) / (yC - yB)
            } else {
                scaleXCb = 0.0f
            }

            var scaleXBa: Float
            if (yB != yA) {
                scaleXBa = slopeXBa / slopeYBa
            } else {
                scaleXBa = 0.0f
            }

            var scaleXCa: Float
            if (yC != yA) {
                scaleXCa = slopeXCa / slopeYCa
            } else {
                scaleXCa = 0.0f
            }

            var f_27 = slopeXBa * slopeYCa - slopeXCa * slopeYBa
            if (f_27 != 0.0f) {
                var scaleColorB = (slopeColorABa * slopeYCa - slopeColorBBa * slopeYBa) / f_27
                var scaleColorA = (slopeColorBBa * slopeXBa - slopeColorABa * slopeXCa) / f_27
                var scaleZB = (slopeZBa * slopeYCa - slopeZCa * slopeYBa) / f_27
                var scaleZA = (slopeZCa * slopeXBa - slopeZBa * slopeXCa) / f_27
                if (yA <= yB && yA <= yC) {
                    if (yA < clipHeight) {
                        if (yB > clipHeight) {
                            yB = clipHeight.toFloat()
                        }
                        if (yC > clipHeight) {
                            yC = clipHeight.toFloat()
                        }
                        colorA = colorA - scaleColorB * xA + scaleColorB
                        zA = zA - scaleZB * xA + scaleZB
                        if (yB < yC) {
                            xC = xA
                            if (yA < 0.0f) {
                                xC = xA - scaleXCa * yA
                                xA -= scaleXBa * yA
                                colorA -= scaleColorA * yA
                                zA -= scaleZA * yA
                                yA = 0.0f
                            }
                            if (yB < 0.0f) {
                                xB -= scaleXCb * yB
                                yB = 0.0f
                            }
                            if (yA != yB && scaleXCa < scaleXBa || yA == yB && scaleXCa > scaleXCb) {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = ((yC + 0.5f).toInt() - yB.toInt()).toFloat()
                                yB -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yB >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xC.toInt(), xA.toInt(), colorA, scaleColorB, zA, scaleZB)
                                    xC += scaleXCa
                                    xA += scaleXBa
                                    colorA += scaleColorA
                                    zA += scaleZA
                                    yA += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xC.toInt(), xB.toInt(), colorA, scaleColorB, zA, scaleZB)
                                    xC += scaleXCa
                                    xB += scaleXCb
                                    colorA += scaleColorA
                                    zA += scaleZA
                                    yA += scanlineWidth
                                }
                            } else {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = ((yC + 0.5f).toInt() - yB.toInt()).toFloat()
                                yB -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yB >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xA.toInt(), xC.toInt(), colorA, scaleColorB, zA, scaleZB)
                                    xC += scaleXCa
                                    xA += scaleXBa
                                    colorA += scaleColorA
                                    zA += scaleZA
                                    yA += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xB.toInt(), xC.toInt(), colorA, scaleColorB, zA, scaleZB)
                                    xC += scaleXCa
                                    xB += scaleXCb
                                    colorA += scaleColorA
                                    zA += scaleZA
                                    yA += scanlineWidth
                                }
                            }
                        } else {
                            xB = xA
                            if (yA < 0.0f) {
                                xB = xA - scaleXCa * yA
                                xA -= scaleXBa * yA
                                colorA -= scaleColorA * yA
                                zA -= scaleZA * yA
                                yA = 0.0f
                            }
                            if (yC < 0.0f) {
                                xC -= scaleXCb * yC
                                yC = 0.0f
                            }
                            if ((yA == yC || scaleXCa >= scaleXBa) && (yA != yC || scaleXCb <= scaleXBa)) {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yB = ((yB + 0.5f).toInt() - yC.toInt()).toFloat()
                                yC -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xA.toInt(), xB.toInt(), colorA, scaleColorB, zA, scaleZB)
                                    xB += scaleXCa
                                    xA += scaleXBa
                                    colorA += scaleColorA
                                    zA += scaleZA
                                    yA += scanlineWidth
                                }
                                while (--yB >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xA.toInt(), xC.toInt(), colorA, scaleColorB, zA, scaleZB)
                                    xC += scaleXCb
                                    xA += scaleXBa
                                    colorA += scaleColorA
                                    zA += scaleZA
                                    yA += scanlineWidth
                                }
                            } else {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yB = ((yB + 0.5f).toInt() - yC.toInt()).toFloat()
                                yC -= yA

                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xB.toInt(), xA.toInt(), colorA, scaleColorB, zA, scaleZB)
                                    xB += scaleXCa
                                    xA += scaleXBa
                                    colorA += scaleColorA
                                    zA += scaleZA
                                    yA += scanlineWidth
                                }
                                while (--yB >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xC.toInt(), xA.toInt(), colorA, scaleColorB, zA, scaleZB)
                                    xC += scaleXCb
                                    xA += scaleXBa
                                    colorA += scaleColorA
                                    zA += scaleZA
                                    yA += scanlineWidth
                                }
                            }
                        }
                    }
                } else if (yB <= yC) {
                    if (yB < clipHeight) {
                        if (yC > clipHeight) {
                            yC = clipHeight.toFloat()
                        }
                        if (yA > clipHeight) {
                            yA = clipHeight.toFloat()
                        }
                        colorB = colorB - scaleColorB * xB + scaleColorB
                        zB = zB - scaleZB * xB + scaleZB
                        if (yC < yA) {
                            xA = xB
                            if (yB < 0.0f) {
                                xA = xB - scaleXBa * yB
                                xB -= scaleXCb * yB
                                colorB -= scaleColorA * yB
                                zB -= scaleZA * yB
                                yB = 0.0f
                            }
                            if (yC < 0.0f) {
                                xC -= scaleXCa * yC
                                yC = 0.0f
                            }
                            if (yB != yC && scaleXBa < scaleXCb || yB == yC && scaleXBa > scaleXCa) {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yA = ((yA + 0.5f).toInt() - yC.toInt()).toFloat()
                                yC -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xA.toInt(), xB.toInt(), colorB, scaleColorB, zB, scaleZB)
                                    xA += scaleXBa
                                    xB += scaleXCb
                                    colorB += scaleColorA
                                    zB += scaleZA
                                    yB += scanlineWidth
                                }
                                while (--yA >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xA.toInt(), xC.toInt(), colorB, scaleColorB, zB, scaleZB)
                                    xA += scaleXBa
                                    xC += scaleXCa
                                    colorB += scaleColorA
                                    zB += scaleZA
                                    yB += scanlineWidth
                                }
                            } else {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yA = ((yA + 0.5f).toInt() - yC.toInt()).toFloat()
                                yC -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xB.toInt(), xA.toInt(), colorB, scaleColorB, zB, scaleZB)
                                    xA += scaleXBa
                                    xB += scaleXCb
                                    colorB += scaleColorA
                                    zB += scaleZA
                                    yB += scanlineWidth
                                }
                                while (--yA >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xC.toInt(), xA.toInt(), colorB, scaleColorB, zB, scaleZB)
                                    xA += scaleXBa
                                    xC += scaleXCa
                                    colorB += scaleColorA
                                    zB += scaleZA
                                    yB += scanlineWidth
                                }
                            }
                        } else {
                            xC = xB
                            if (yB < 0.0f) {
                                xC = xB - scaleXBa * yB
                                xB -= scaleXCb * yB
                                colorB -= scaleColorA * yB
                                zB -= scaleZA * yB
                                yB = 0.0f
                            }
                            if (yA < 0.0f) {
                                xA -= scaleXCa * yA
                                yA = 0.0f
                            }
                            if (scaleXBa < scaleXCb) {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = ((yC + 0.5f).toInt() - yA.toInt()).toFloat()
                                yA -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yA >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xC.toInt(), xB.toInt(), colorB, scaleColorB, zB, scaleZB)
                                    xC += scaleXBa
                                    xB += scaleXCb
                                    colorB += scaleColorA
                                    zB += scaleZA
                                    yB += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xA.toInt(), xB.toInt(), colorB, scaleColorB, zB, scaleZB)
                                    xA += scaleXCa
                                    xB += scaleXCb
                                    colorB += scaleColorA
                                    zB += scaleZA
                                    yB += scanlineWidth
                                }
                            } else {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = ((yC + 0.5f).toInt() - yA.toInt()).toFloat()
                                yA -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yA >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xB.toInt(), xC.toInt(), colorB, scaleColorB, zB, scaleZB)
                                    xC += scaleXBa
                                    xB += scaleXCb
                                    colorB += scaleColorA
                                    zB += scaleZA
                                    yB += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xB.toInt(), xA.toInt(), colorB, scaleColorB, zB, scaleZB)
                                    xA += scaleXCa
                                    xB += scaleXCb
                                    colorB += scaleColorA
                                    zB += scaleZA
                                    yB += scanlineWidth
                                }
                            }
                        }
                    }
                } else if (yC < clipHeight) {
                    if (yA > clipHeight) {
                        yA = clipHeight.toFloat()
                    }
                    if (yB > clipHeight) {
                        yB = clipHeight.toFloat()
                    }
                    colorC = colorC - scaleColorB * xC + scaleColorB
                    zC = zC - scaleZB * xC + scaleZB
                    if (yA < yB) {
                        xB = xC
                        if (yC < 0.0f) {
                            xB = xC - scaleXCb * yC
                            xC -= scaleXCa * yC
                            colorC -= scaleColorA * yC
                            zC -= scaleZA * yC
                            yC = 0.0f
                        }
                        if (yA < 0.0f) {
                            xA -= scaleXBa * yA
                            yA = 0.0f
                        }
                        if (scaleXCb < scaleXCa) {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt().toFloat()
                            yB = ((yB + 0.5f).toInt() - yA.toInt()).toFloat()
                            yA -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yA >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xB.toInt(), xC.toInt(), colorC, scaleColorB, zC, scaleZB)
                                xB += scaleXCb
                                xC += scaleXCa
                                colorC += scaleColorA
                                zC += scaleZA
                                yC += scanlineWidth
                            }
                            while (--yB >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xB.toInt(), xA.toInt(), colorC, scaleColorB, zC, scaleZB)
                                xB += scaleXCb
                                xA += scaleXBa
                                colorC += scaleColorA
                                zC += scaleZA
                                yC += scanlineWidth
                            }
                        } else {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt().toFloat()
                            yB = ((yB + 0.5f).toInt() - yA.toInt()).toFloat()
                            yA -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yA >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xC.toInt(), xB.toInt(), colorC, scaleColorB, zC, scaleZB)
                                xB += scaleXCb
                                xC += scaleXCa
                                colorC += scaleColorA
                                zC += scaleZA
                                yC += scanlineWidth
                            }
                            while (--yB >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xA.toInt(), xB.toInt(), colorC, scaleColorB, zC, scaleZB)
                                xB += scaleXCb
                                xA += scaleXBa
                                colorC += scaleColorA
                                zC += scaleZA
                                yC += scanlineWidth
                            }
                        }
                    } else {
                        xA = xC
                        if (yC < 0.0f) {
                            xA = xC - scaleXCb * yC
                            xC -= scaleXCa * yC
                            colorC -= scaleColorA * yC
                            zC -= scaleZA * yC
                            yC = 0.0f
                        }
                        if (yB < 0.0f) {
                            xB -= scaleXBa * yB
                            yB = 0.0f
                        }
                        if (scaleXCb < scaleXCa) {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt().toFloat()
                            yA = ((yA + 0.5f).toInt() - yB.toInt()).toFloat()
                            yB -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yB >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xA.toInt(), xC.toInt(), colorC, scaleColorB, zC, scaleZB)
                                xA += scaleXCb
                                xC += scaleXCa
                                colorC += scaleColorA
                                zC += scaleZA
                                yC += scanlineWidth
                            }
                            while (--yA >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xB.toInt(), xC.toInt(), colorC, scaleColorB, zC, scaleZB)
                                xB += scaleXBa
                                xC += scaleXCa
                                colorC += scaleColorA
                                zC += scaleZA
                                yC += scanlineWidth
                            }
                        } else {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt().toFloat()
                            yA = ((yA + 0.5f).toInt() - yB.toInt()).toFloat()
                            yB -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yB >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xC.toInt(), xA.toInt(), colorC, scaleColorB, zC, scaleZB)
                                xA += scaleXCb
                                xC += scaleXCa
                                colorC += scaleColorA
                                zC += scaleZA
                                yC += scanlineWidth
                            }
                            while (--yA >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xC.toInt(), xB.toInt(), colorC, scaleColorB, zC, scaleZB)
                                xB += scaleXBa
                                xC += scaleXCa
                                colorC += scaleColorA
                                zC += scaleZA
                                yC += scanlineWidth
                            }
                        }
                    }
                }
            }
        }
    }

    fun drawMultiMaterialzedScanline(
        pixels: IntArray, y: Int,
        xA: Int, xB: Int,
        zNearA: Float, zNearB: Float,
        zFarA: Float, zFarB: Float,
        uA: Float, uB: Float,
        vA: Float, vB: Float,
        fogDensityA: Float, fogDensityB: Float,
        alphaA: Float, alphaB: Float,
        redA: Float, redB: Float,
        greenA: Float, greenB: Float,
        blueA: Float, blueB: Float,
        textureAPercentageA: Float, textureAPercentageB: Float,
        textureBPercentageA: Float, textureBPercentageB: Float
    ) {
        var y = y
        var xA = xA
        var xB = xB
        var zNearA = zNearA
        var zFarA = zFarA
        var uA = uA
        var vA = vA
        var fogDensityA = fogDensityA
        var alphaA = alphaA
        var redA = redA
        var greenA = greenA
        var blueA = blueA
        var textureAPercentageA = textureAPercentageA
        var textureBPercentageA = textureBPercentageA
        val depthBuf = depthBuffer!!
        val texturePixelsA = texturePixelsA!!
        val texturePixelsB = texturePixelsB!!
        val texturePixelsC = texturePixelsC!!
        var lengthX = xB - xA
        var scaleX = 1.0f / lengthX
        var slopeZNearBa = (zNearB - zNearA) * scaleX
        var slopeZFarBa = (zFarB - zFarA) * scaleX
        var slopeUBa = (uB - uA) * scaleX
        var slopeVBa = (vB - vA) * scaleX
        var slopeFogDensityBa = (fogDensityB - fogDensityA) * scaleX
        var slopeAlphaBa = (alphaB - alphaA) * scaleX
        var slopeRedBa = (redB - redA) * scaleX
        var slopeGreenBa = (greenB - greenA) * scaleX
        var slopeBlueBa = (blueB - blueA) * scaleX
        var slopeTextureAPercentageBa = (textureAPercentageB - textureAPercentageA) * scaleX
        var slopeTextureBPercentageBa = (textureBPercentageB - textureBPercentageA) * scaleX
        if (clipEdges) {
            if (xB > clipWidth) {
                xB = clipWidth
            }
            if (xA < 0) {
                zNearA -= slopeZNearBa * xA
                zFarA -= slopeZFarBa * xA
                uA -= slopeUBa * xA
                vA -= slopeVBa * xA
                fogDensityA -= slopeFogDensityBa * xA
                alphaA -= slopeAlphaBa * xA
                redA -= slopeRedBa * xA
                greenA -= slopeGreenBa * xA
                blueA -= slopeBlueBa * xA
                textureAPercentageA -= slopeTextureAPercentageBa * xA
                textureBPercentageA -= slopeTextureBPercentageBa * xA
                xA = 0
            }
        }
        if (xA < xB) {
            lengthX = xB - xA
            y += xA
            while (lengthX-- > 0) {
                var depthNear = 1.0f / zNearA
                var depthFar = 1.0f / zFarA
                if (depthNear < depthBuf[y]) {
                    /** Calculate base material colors **/
                    var u = uA * depthFar
                    var v = vA * depthFar
                    var textureUA = (u * textureSizeA * textureScaleA).toInt() and textureMaskA
                    var textureVA = (v * textureSizeA * textureScaleA).toInt() and textureMaskA
                    var texturePixelA = texturePixelsA[textureUA + textureVA * textureSizeA]
                    textureUA = (u * textureSizeB * textureScaleB).toInt() and textureMaskB
                    textureVA = (v * textureSizeB * textureScaleB).toInt() and textureMaskB
                    var texturePixelB = texturePixelsB[textureUA + textureVA * textureSizeB]
                    textureUA = (u * textureSizeC * textureScaleC).toInt() and textureMaskC
                    textureVA = (v * textureSizeC * textureScaleC).toInt() and textureMaskC
                    var texturePixelC = texturePixelsC[textureUA + textureVA * textureSizeC]
                    /** Mix texture colors **/
                    var texturePercentC = 1.0f - (textureAPercentageA + textureBPercentageA)
                    texturePixelA = 0xff000000.toInt() or ((textureAPercentageA * (texturePixelA shr 16 and 0xff)).toInt() shl 16) or ((textureAPercentageA * (texturePixelA shr 8 and 0xff)).toInt() shl 8) or (textureAPercentageA * (texturePixelA and 0xff)).toInt()
                    texturePixelB = 0xff000000.toInt() or ((textureBPercentageA * (texturePixelB shr 16 and 0xff)).toInt() shl 16) or ((textureBPercentageA * (texturePixelB shr 8 and 0xff)).toInt() shl 8) or (textureBPercentageA * (texturePixelB and 0xff)).toInt()
                    texturePixelC = 0xff000000.toInt() or ((texturePercentC * (texturePixelC shr 16 and 0xff)).toInt() shl 16) or ((texturePercentC * (texturePixelC shr 8 and 0xff)).toInt() shl 8) or (texturePercentC * (texturePixelC and 0xff)).toInt()
                    var colorSum = texturePixelA + texturePixelB + texturePixelC
                    var blendColor = 0xff000000.toInt() or ((redA * (colorSum shr 16 and 0xff)).toInt() shl 8 and 0xff0000) or ((greenA * (colorSum shr 8 and 0xff)).toInt() and 0xff00) or ((blueA * (colorSum and 0xff)).toInt() shr 8)
                    /** Blend fog into final color **/
                    if (fogDensityA != 0.0f) {
                        var fogTransparency = (255.0f - fogDensityA).toInt()
                        var fogBlendedColor = ((fogColour and 0xff00ff) * fogDensityA.toInt() and -16711936 or (fogDensityA.toInt() * (fogColour and 0xff00) and 0xff0000)) ushr 8
                        blendColor = fogBlendedColor + (((blendColor and 0xff00ff) * fogTransparency and -16711936 or (fogTransparency * (blendColor and 0xff00) and 0xff0000)) ushr 8)
                    }
                    pixels[y] = blendColor
                    depthBuf[y] = depthNear
                }
                y++
                zNearA += slopeZNearBa
                zFarA += slopeZFarBa
                uA += slopeUBa
                vA += slopeVBa
                fogDensityA += slopeFogDensityBa
                alphaA += slopeAlphaBa
                redA += slopeRedBa
                greenA += slopeGreenBa
                blueA += slopeBlueBa
                textureAPercentageA += slopeTextureAPercentageBa
                textureBPercentageA += slopeTextureBPercentageBa
            }
        }
    }

    fun drawFlatTriangle(
        colorWrite: Boolean, depthWrite: Boolean, transparent: Boolean,
        yA: Float, yB: Float, yC: Float,
        xA: Float, xB: Float, xC: Float,
        zA: Float, zB: Float, zC: Float,
        color: Int
    ) {
        var yA = yA
        var yB = yB
        var yC = yC
        var xA = xA
        var xB = xB
        var xC = xC
        var zA = zA
        var zB = zB
        var zC = zC

        if (wireframeMode) {
            safemodeToolkit.drawOpaqueLine(xA.toInt(), yA.toInt(), xB.toInt(), yB.toInt(), color)
            safemodeToolkit.drawOpaqueLine(xB.toInt(), yB.toInt(), xC.toInt(), yC.toInt(), color)
            safemodeToolkit.drawOpaqueLine(xC.toInt(), yC.toInt(), xA.toInt(), yA.toInt(), color)
        } else {
            val slopeXBa = xB - xA
            val slopeYBa = yB - yA
            val slopeXCa = xC - xA
            val slopeYCa = yC - yA
            val slopeZBa = zB - zA
            val slopeZCa = zC - zA
            var scaleXBa = 0.0f
            if (yB != yA) {
                scaleXBa = (xB - xA) / (yB - yA)
            }
            var scaleXCb = 0.0f
            if (yC != yB) {
                scaleXCb = (xC - xB) / (yC - yB)
            }
            var scaleXCa = 0.0f
            if (yC != yA) {
                scaleXCa = (xA - xC) / (yA - yC)
            }
            val length = slopeXBa * slopeYCa - slopeXCa * slopeYBa
            if (length != 0.0f) {
                val deltaA = (slopeZBa * slopeYCa - slopeZCa * slopeYBa) / length
                val scaleZAvg = (slopeZCa * slopeXBa - slopeZBa * slopeXCa) / length
                val colourBuf = colourBuffer!!
                if (yA <= yB && yA <= yC) {
                    if (yA < clipHeight) {
                        if (yB > clipHeight) {
                            yB = clipHeight.toFloat()
                        }
                        if (yC > clipHeight) {
                            yC = clipHeight.toFloat()
                        }
                        zA = zA - deltaA * xA + deltaA
                        if (yB < yC) {
                            xC = xA
                            if (yA < 0.0f) {
                                xC = xA - scaleXCa * yA
                                xA -= scaleXBa * yA
                                zA -= scaleZAvg * yA
                                yA = 0.0f
                            }
                            if (yB < 0.0f) {
                                xB -= scaleXCb * yB
                                yB = 0.0f
                            }
                            if (yA != yB && scaleXCa < scaleXBa || yA == yB && scaleXCa > scaleXCb) {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt() - yB
                                yB -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yB >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xC.toInt(), xA.toInt(), zA, deltaA)
                                    xC += scaleXCa
                                    xA += scaleXBa
                                    zA += scaleZAvg
                                    yA += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xC.toInt(), xB.toInt(), zA, deltaA)
                                    xC += scaleXCa
                                    xB += scaleXCb
                                    zA += scaleZAvg
                                    yA += scanlineWidth
                                }
                            } else {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt() - yB
                                yB -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yB >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xA.toInt(), xC.toInt(), zA, deltaA)
                                    xC += scaleXCa
                                    xA += scaleXBa
                                    zA += scaleZAvg
                                    yA += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xB.toInt(), xC.toInt(), zA, deltaA)
                                    xC += scaleXCa
                                    xB += scaleXCb
                                    zA += scaleZAvg
                                    yA += scanlineWidth
                                }
                            }
                        } else {
                            xB = xA
                            if (yA < 0.0f) {
                                xB = xA - scaleXCa * yA
                                xA -= scaleXBa * yA
                                zA -= scaleZAvg * yA
                                yA = 0.0f
                            }
                            if (yC < 0.0f) {
                                xC -= scaleXCb * yC
                                yC = 0.0f
                            }
                            if ((yA == yC || scaleXCa >= scaleXBa) && (yA != yC || scaleXCb <= scaleXBa)) {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt() - yC
                                yC -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xA.toInt(), xB.toInt(), zA, deltaA)
                                    xB += scaleXCa
                                    xA += scaleXBa
                                    zA += scaleZAvg
                                    yA += scanlineWidth
                                }
                                while (--yB >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xA.toInt(), xC.toInt(), zA, deltaA)
                                    xC += scaleXCb
                                    xA += scaleXBa
                                    zA += scaleZAvg
                                    yA += scanlineWidth
                                }
                            } else {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt() - yC
                                yC -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xB.toInt(), xA.toInt(), zA, deltaA)
                                    xB += scaleXCa
                                    xA += scaleXBa
                                    zA += scaleZAvg
                                    yA += scanlineWidth
                                }
                                while (--yB >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xC.toInt(), xA.toInt(), zA, deltaA)
                                    xC += scaleXCb
                                    xA += scaleXBa
                                    zA += scaleZAvg
                                    yA += scanlineWidth
                                }
                            }
                        }
                    }
                } else if (yB <= yC) {
                    if (yB < clipHeight) {
                        if (yC > clipHeight) {
                            yC = clipHeight.toFloat()
                        }
                        if (yA > clipHeight) {
                            yA = clipHeight.toFloat()
                        }
                        zB = zB - deltaA * xB + deltaA
                        if (yC < yA) {
                            xA = xB
                            if (yB < 0.0f) {
                                xA = xB - scaleXBa * yB
                                xB -= scaleXCb * yB
                                zB -= scaleZAvg * yB
                                yB = 0.0f
                            }
                            if (yC < 0.0f) {
                                xC -= scaleXCa * yC
                                yC = 0.0f
                            }
                            if ((yB == yC || scaleXBa >= scaleXCb) && (yB != yC || scaleXBa <= scaleXCa)) {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt() - yC
                                yC -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xB.toInt(), xA.toInt(), zB, deltaA)
                                    xA += scaleXBa
                                    xB += scaleXCb
                                    zB += scaleZAvg
                                    yB += scanlineWidth
                                }
                                while (--yA >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xC.toInt(), xA.toInt(), zB, deltaA)
                                    xA += scaleXBa
                                    xC += scaleXCa
                                    zB += scaleZAvg
                                    yB += scanlineWidth
                                }
                            } else {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt() - yC
                                yC -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xA.toInt(), xB.toInt(), zB, deltaA)
                                    xA += scaleXBa
                                    xB += scaleXCb
                                    zB += scaleZAvg
                                    yB += scanlineWidth
                                }
                                while (--yA >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xA.toInt(), xC.toInt(), zB, deltaA)
                                    xA += scaleXBa
                                    xC += scaleXCa
                                    zB += scaleZAvg
                                    yB += scanlineWidth
                                }
                            }
                        } else {
                            xC = xB
                            if (yB < 0.0f) {
                                xC = xB - scaleXBa * yB
                                xB -= scaleXCb * yB
                                zB -= scaleZAvg * yB
                                yB = 0.0f
                            }
                            if (yA < 0.0f) {
                                xA -= scaleXCa * yA
                                yA = 0.0f
                            }
                            if (scaleXBa < scaleXCb) {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt() - yA
                                yA -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yA >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xC.toInt(), xB.toInt(), zB, deltaA)
                                    xC += scaleXBa
                                    xB += scaleXCb
                                    zB += scaleZAvg
                                    yB += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xA.toInt(), xB.toInt(), zB, deltaA)
                                    xA += scaleXCa
                                    xB += scaleXCb
                                    zB += scaleZAvg
                                    yB += scanlineWidth
                                }
                            } else {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt() - yA
                                yA -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yA >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xB.toInt(), xC.toInt(), zB, deltaA)
                                    xC += scaleXBa
                                    xB += scaleXCb
                                    zB += scaleZAvg
                                    yB += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xB.toInt(), xA.toInt(), zB, deltaA)
                                    xA += scaleXCa
                                    xB += scaleXCb
                                    zB += scaleZAvg
                                    yB += scanlineWidth
                                }
                            }
                        }
                    }
                } else if (yC < clipHeight) {
                    if (yA > clipHeight) {
                        yA = clipHeight.toFloat()
                    }
                    if (yB > clipHeight) {
                        yB = clipHeight.toFloat()
                    }
                    zC = zC - deltaA * xC + deltaA
                    if (yA < yB) {
                        xB = xC
                        if (yC < 0.0f) {
                            xB = xC - scaleXCb * yC
                            xC -= scaleXCa * yC
                            zC -= scaleZAvg * yC
                            yC = 0.0f
                        }
                        if (yA < 0.0f) {
                            xA -= scaleXBa * yA
                            yA = 0.0f
                        }
                        if (scaleXCb < scaleXCa) {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt() - yA
                            yA -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yA >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xB.toInt(), xC.toInt(), zC, deltaA)
                                xB += scaleXCb
                                xC += scaleXCa
                                zC += scaleZAvg
                                yC += scanlineWidth
                            }
                            while (--yB >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xB.toInt(), xA.toInt(), zC, deltaA)
                                xB += scaleXCb
                                xA += scaleXBa
                                zC += scaleZAvg
                                yC += scanlineWidth
                            }
                        } else {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt() - yA
                            yA -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yA >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xC.toInt(), xB.toInt(), zC, deltaA)
                                xB += scaleXCb
                                xC += scaleXCa
                                zC += scaleZAvg
                                yC += scanlineWidth
                            }
                            while (--yB >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xA.toInt(), xB.toInt(), zC, deltaA)
                                xB += scaleXCb
                                xA += scaleXBa
                                zC += scaleZAvg
                                yC += scanlineWidth
                            }
                        }
                    } else {
                        xA = xC
                        if (yC < 0.0f) {
                            xA = xC - scaleXCb * yC
                            xC -= scaleXCa * yC
                            zC -= scaleZAvg * yC
                            yC = 0.0f
                        }
                        if (yB < 0.0f) {
                            xB -= scaleXBa * yB
                            yB = 0.0f
                        }
                        if (scaleXCb < scaleXCa) {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt() - yB
                            yB -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yB >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xA.toInt(), xC.toInt(), zC, deltaA)
                                xA += scaleXCb
                                xC += scaleXCa
                                zC += scaleZAvg
                                yC += scanlineWidth
                            }
                            while (--yA >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xB.toInt(), xC.toInt(), zC, deltaA)
                                xB += scaleXBa
                                xC += scaleXCa
                                zC += scaleZAvg
                                yC += scanlineWidth
                            }
                        } else {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt() - yB
                            yB -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yB >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xC.toInt(), xA.toInt(), zC, deltaA)
                                xA += scaleXCb
                                xC += scaleXCa
                                zC += scaleZAvg
                                yC += scanlineWidth
                            }
                            while (--yA >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xC.toInt(), xB.toInt(), zC, deltaA)
                                xB += scaleXBa
                                xC += scaleXCa
                                zC += scaleZAvg
                                yC += scanlineWidth
                            }
                        }
                    }
                }
            }
        }
    }

    fun drawMaterialisedScanline(
        writeDepth: Boolean, transparent: Boolean,
        pixels: IntArray, bufferOffset: Int,
        xA: Int, xB: Int,
        zNearA: Float, zNearB: Float,
        zFarA: Float, zFarB: Float,
        uA: Float, uB: Float,
        vA: Float, vB: Float,
        fogDensityA: Float, fogDensityB: Float,
        alphaA: Float, alphaB: Float,
        redA: Float, redB: Float,
        greenA: Float, greenB: Float,
        blueA: Float, blueB: Float
    ) {
        var bufferOffset = bufferOffset
        var xA = xA
        var xB = xB
        var zNearA = zNearA
        var zFarA = zFarA
        var uA = uA
        var vA = vA
        var fogDensityA = fogDensityA
        var alphaA = alphaA
        var redA = redA
        var greenA = greenA
        var blueA = blueA
        val depthBuf = depthBuffer!!
        val texturePixelsA = texturePixelsA!!
        var lengthX = xB - xA
        var scaleX = 1.0f / lengthX
        var slopeZnearBa = (zNearB - zNearA) * scaleX
        var slopeZfarBa = (zFarB - zFarA) * scaleX
        var slopeUBa = (uB - uA) * scaleX
        var slopeVBa = (vB - vA) * scaleX
        var slopeFogDensityBa = (fogDensityB - fogDensityA) * scaleX
        var slopeAlphaBa = (alphaB - alphaA) * scaleX
        var slopeRedBa = (redB - redA) * scaleX
        var slopeGreenBa = (greenB - greenA) * scaleX
        var slopeBlueBa = (blueB - blueA) * scaleX
        if (clipEdges) {
            if (xB > clipWidth) {
                xB = clipWidth
            }
            if (xA < 0) {
                zNearA -= slopeZnearBa * xA
                zFarA -= slopeZfarBa * xA
                uA -= slopeUBa * xA
                vA -= slopeVBa * xA
                fogDensityA -= slopeFogDensityBa * xA
                alphaA -= slopeAlphaBa * xA
                redA -= slopeRedBa * xA
                greenA -= slopeGreenBa * xA
                blueA -= slopeBlueBa * xA
                xA = 0
            }
        }
        if (xA < xB) {
            lengthX = xB - xA
            bufferOffset += xA
            while (lengthX-- > 0) {
                var depth = 1.0f / zNearA
                var aUvScale = 1.0f / zFarA
                if (!writeDepth || depth < depthBuf[bufferOffset]) {
                    var v = (uA * aUvScale * textureSizeA).toInt()
                    if (textureRepeat) {
                        v = v and textureMaskA
                    } else if (v < 0) {
                        v = 0
                    } else if (v > textureMaskA) {
                        v = textureMaskA
                    }

                    var u = (vA * aUvScale * textureSizeA).toInt()
                    if (textureRepeat) {
                        u = u and textureMaskA
                    } else if (u < 0) {
                        u = 0
                    } else if (u > textureMaskA) {
                        u = textureMaskA
                    }

                    var texel = texturePixelsA[v + u * textureSizeA]
                    var alpha: Int
                    if (alphaMode == 2) {
                        alpha = texel shr 24 and 0xff
                    } else if (alphaMode == 1) {
                        alpha = if (texel == 0) 0 else 255
                    } else {
                        alpha = alphaA.toInt()
                    }

                    if (alpha != 0) {
                        if (alpha != 255) {
                            var pixel = 0xff000000.toInt() or ((redA * (texel shr 16 and 0xff)).toInt() shl 8 and 0xff0000) or ((greenA * (texel shr 8 and 0xff)).toInt() and 0xff00) or ((blueA * (texel and 0xff)).toInt() shr 8)
                            if (fogDensityA != 0.0f) {
                                var fogTransparency = (255.0f - fogDensityA).toInt()
                                var blendedFogColor = ((fogColour and 0xff00ff) * fogDensityA.toInt() and -16711936 or ((fogColour and 0xff00) * fogDensityA.toInt() and 0xff0000)) ushr 8
                                pixel = blendedFogColor + (((pixel and 0xff00ff) * fogTransparency and -16711936 or (fogTransparency * (pixel and 0xff00) and 0xff0000)) ushr 8)
                            }
                            var currentPixel = pixels[bufferOffset]
                            var invertedAlpha = 255 - alpha
                            pixel = (((currentPixel and 0xff00ff) * invertedAlpha + (pixel and 0xff00ff) * alpha and -16711936) + (invertedAlpha * (currentPixel and 0xff00) + alpha * (pixel and 0xff00) and 0xff0000)) shr 8
                            if (transparent) {
                                pixels[bufferOffset] = ((alpha or (pixels[bufferOffset] shr 24)) shl 24) or pixel
                            } else {
                                pixels[bufferOffset] = pixel
                            }
                        } else {
                            var pixel = 0xff000000.toInt() or ((redA * (texel shr 16 and 0xff)).toInt() shl 8 and 0xff0000) or ((greenA * (texel shr 8 and 0xff)).toInt() and 0xff00) or ((blueA * (texel and 0xff)).toInt() shr 8)
                            if (fogDensityA != 0.0f) {
                                var fogTransparency = (255.0f - fogDensityA).toInt()
                                var blendedFogColor = ((fogColour and 0xff00ff) * fogDensityA.toInt() and -16711936 or ((fogColour and 0xff00) * fogDensityA.toInt() and 0xff0000)) ushr 8
                                pixel = blendedFogColor + (((pixel and 0xff00ff) * fogTransparency and -16711936 or (fogTransparency * (pixel and 0xff00) and 0xff0000)) ushr 8)
                            }
                            if (transparent) {
                                pixels[bufferOffset] = (alpha shl 24) or pixel
                            } else {
                                pixels[bufferOffset] = pixel
                            }
                        }
                        if (writeDepth) {
                            depthBuf[bufferOffset] = depth
                        }
                    }
                }
                bufferOffset++
                zNearA += slopeZnearBa
                zFarA += slopeZfarBa
                uA += slopeUBa
                vA += slopeVBa
                fogDensityA += slopeFogDensityBa
                alphaA += slopeAlphaBa
                redA += slopeRedBa
                greenA += slopeGreenBa
                blueA += slopeBlueBa
            }
        }
    }

    fun drawShadedScanline(
        writeDepth: Boolean, transparent: Boolean,
        pixels: IntArray, bufferOffset: Int,
        startX: Int, endX: Int,
        colour: Float, colourSlope: Float,
        depth: Float, depthSlope: Float
    ) {
        var bufferOffset = bufferOffset
        var startX = startX
        var endX = endX
        var colour = colour
        var colourSlope = colourSlope
        var depth = depth
        val depthBuf = depthBuffer!!

        if (clipEdges) {
            if (endX > clipWidth) {
                endX = clipWidth
            }
            if (startX < 0) {
                startX = 0
            }
        }

        if (startX >= endX) {
            return
        }

        bufferOffset += startX - 1
        colour += colourSlope * startX
        depth += depthSlope * startX
        if (threadResource.zbuffering) {
            if (smoothEdges) {
                var x = endX - startX shr 2
                colourSlope *= 4.0f
                if (blendingAlpha == 0) {
                    if (x > 0) {
                        do {
                            val background = ColorUtil.hsvToRgbTable!![colour.toInt() and 0xffff]
                            colour += colourSlope
                            bufferOffset++
                            if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                pixels[bufferOffset] = background
                                if (writeDepth) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += depthSlope
                            bufferOffset++
                            if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                pixels[bufferOffset] = background
                                if (writeDepth) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += depthSlope
                            bufferOffset++
                            if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                pixels[bufferOffset] = background
                                if (writeDepth) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += depthSlope
                            bufferOffset++
                            if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                pixels[bufferOffset] = background
                                if (writeDepth) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += depthSlope
                        } while (--x > 0)
                    }
                    x = endX - startX and 0x3
                    if (x > 0) {
                        val background = ColorUtil.hsvToRgbTable!![colour.toInt() and 0xffff]
                        do {
                            bufferOffset++
                            if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                pixels[bufferOffset] = background
                                if (writeDepth) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += depthSlope
                        } while (--x > 0)
                    }
                } else {
                    val alpha = blendingAlpha
                    val invertedAlpha = 256 - blendingAlpha
                    if (x > 0) {
                        do {
                            var background = ColorUtil.hsvToRgbTable!![colour.toInt() and 0xffff]
                            colour += colourSlope
                            background = ((background and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + (invertedAlpha * (background and 0xff00) shr 8 and 0xff00)
                            bufferOffset++
                            if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                } else {
                                    pixels[bufferOffset] = background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                }
                                if (writeDepth) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += depthSlope
                            bufferOffset++
                            if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                } else {
                                    pixels[bufferOffset] = background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                }
                                if (writeDepth) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += depthSlope
                            bufferOffset++
                            if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                } else {
                                    pixels[bufferOffset] = background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                }
                                if (writeDepth) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += depthSlope
                            bufferOffset++
                            if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                } else {
                                    pixels[bufferOffset] = background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                }
                                if (writeDepth) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += depthSlope
                        } while (--x > 0)
                    }
                    x = endX - startX and 0x3
                    if (x > 0) {
                        var background = ColorUtil.hsvToRgbTable!![colour.toInt() and 0xffff]
                        background = ((background and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + (invertedAlpha * (background and 0xff00) shr 8 and 0xff00)
                        do {
                            bufferOffset++
                            if (!writeDepth || depth < depthBuf[bufferOffset]) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                } else {
                                    pixels[bufferOffset] = background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                }
                                if (writeDepth) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += depthSlope
                        } while (--x > 0)
                    }
                }
            } else {
                var x = endX - startX
                if (blendingAlpha != 0) {
                    val alpha = blendingAlpha
                    val invertedAlpha = 256 - blendingAlpha
                    do {
                        bufferOffset++
                        if (!writeDepth || depth < depthBuf[bufferOffset]) {
                            var background = ColorUtil.hsvToRgbTable!![colour.toInt() and 0xffff]
                            background = ((background and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + (invertedAlpha * (background and 0xff00) shr 8 and 0xff00)
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                            } else {
                                pixels[bufferOffset] = background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                            }
                            if (writeDepth) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        colour += colourSlope
                        depth += depthSlope
                    } while (--x > 0)
                } else {
                    do {
                        bufferOffset++
                        if (!writeDepth || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = ColorUtil.hsvToRgbTable!![colour.toInt() and 0xffff]
                            if (writeDepth) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += depthSlope
                        colour += colourSlope
                    } while (--x > 0)
                }
            }
        } else if (smoothEdges) {
            var x = endX - startX shr 2
            colourSlope *= 4.0f
            if (blendingAlpha == 0) {
                if (x > 0) {
                    do {
                        val background = ColorUtil.hsvToRgbTable!![colour.toInt() and 0xffff]
                        colour += colourSlope
                        bufferOffset++
                        if (!writeDepth || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = background
                        }
                        depth += depthSlope
                        bufferOffset++
                        if (!writeDepth || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = background
                        }
                        depth += depthSlope
                        bufferOffset++
                        if (!writeDepth || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = background
                        }
                        depth += depthSlope
                        bufferOffset++
                        if (!writeDepth || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = background
                        }
                        depth += depthSlope
                    } while (--x > 0)
                }
                x = endX - startX and 0x3
                if (x > 0) {
                    val background = ColorUtil.hsvToRgbTable!![colour.toInt() and 0xffff]
                    do {
                        bufferOffset++
                        if (!writeDepth || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = background
                        }
                        depth += depthSlope
                    } while (--x > 0)
                }
            } else {
                val alpha = blendingAlpha
                val invertedAlpha = 256 - blendingAlpha
                if (x > 0) {
                    do {
                        var background = ColorUtil.hsvToRgbTable!![colour.toInt() and 0xffff]
                        colour += colourSlope
                        background = ((background and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + (invertedAlpha * (background and 0xff00) shr 8 and 0xff00)
                        bufferOffset++
                        if (!writeDepth || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                            } else {
                                pixels[bufferOffset] = background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                            }
                        }
                        depth += depthSlope
                        bufferOffset++
                        if (!writeDepth || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                            } else {
                                pixels[bufferOffset] = background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                            }
                        }
                        depth += depthSlope
                        bufferOffset++
                        if (!writeDepth || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                            } else {
                                pixels[bufferOffset] = background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                            }
                        }
                        depth += depthSlope
                        bufferOffset++
                        if (!writeDepth || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                            } else {
                                pixels[bufferOffset] = background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                            }
                        }
                        depth += depthSlope
                    } while (--x > 0)
                }
                x = endX - startX and 0x3
                if (x > 0) {
                    var background = ColorUtil.hsvToRgbTable!![colour.toInt() and 0xffff]
                    background = ((background and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + (invertedAlpha * (background and 0xff00) shr 8 and 0xff00)
                    do {
                        bufferOffset++
                        if (!writeDepth || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                            } else {
                                pixels[bufferOffset] = background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                            }
                        }
                        depth += depthSlope
                    } while (--x > 0)
                }
            }
        } else {
            var x = endX - startX
            if (blendingAlpha != 0) {
                val alpha = blendingAlpha
                val invertedAlpha = 256 - blendingAlpha
                do {
                    bufferOffset++
                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                        var background = ColorUtil.hsvToRgbTable!![colour.toInt() and 0xffff]
                        background = ((background and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + (invertedAlpha * (background and 0xff00) shr 8 and 0xff00)
                        val pixel = pixels[bufferOffset]
                        if (transparent) {
                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                        } else {
                            pixels[bufferOffset] = background + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                        }
                    }
                    colour += colourSlope
                    depth += depthSlope
                } while (--x > 0)
            } else {
                do {
                    bufferOffset++
                    if (!writeDepth || depth < depthBuf[bufferOffset]) {
                        pixels[bufferOffset] = ColorUtil.hsvToRgbTable!![colour.toInt() and 0xffff]
                    }
                    depth += depthSlope
                    colour += colourSlope
                } while (--x > 0)
            }
        }
    }
}
