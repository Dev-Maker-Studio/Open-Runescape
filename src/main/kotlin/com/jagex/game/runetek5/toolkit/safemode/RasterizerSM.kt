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
    @JvmField var skipDepthTest: Boolean = false
    @JvmField var depthRangeNear: Float = 0f
    @JvmField var depthRangeFar: Float = 1.0f
    @JvmField val scanlineOffsets: IntArray = IntArray(4096)
    @JvmField val textureB: Int = -1
    @JvmField val textureA: Int = -1
    @JvmField var textureRepeat: Boolean = true
    @JvmField val textureC: Int = -1
    @JvmField var clipEdges: Boolean = false
    @JvmField var shadedColorMin: Float = 0f
    @JvmField var shadedColorMax: Float = 65535f
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
        slopeDepth: Float
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
            depth += slopeDepth * xA
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
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            if (colorWrite) {
                                pixels[bufferOffset] = color
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            if (colorWrite) {
                                pixels[bufferOffset] = color
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            if (colorWrite) {
                                pixels[bufferOffset] = color
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += slopeDepth
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
                        depth += slopeDepth
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
                        depth += slopeDepth
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
                        depth += slopeDepth
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
                        depth += slopeDepth
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
                        depth += slopeDepth
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
                        depth += slopeDepth
                    }
                    return
                }
                if (xA != 0 && xB <= clipWidth - 1) {
                    while (--x >= 0) {
                        bufferOffset++
                        if ((!depthWrite || depth < depthBuf[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if ((!depthWrite || depth < depthBuf[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if ((!depthWrite || depth < depthBuf[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if ((!depthWrite || depth < depthBuf[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += slopeDepth
                    }
                    x = xB - xA and 0x3
                    while (--x >= 0) {
                        bufferOffset++
                        if ((!depthWrite || depth < depthBuf[bufferOffset]) && colorWrite) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += slopeDepth
                    }
                }
            } else if (colorWrite) {
                if (blendingAlpha == 0) {
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = color
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = color
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = color
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = color
                        }
                        depth += slopeDepth
                    }
                    x = xB - xA and 0x3
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = color
                        }
                        depth += slopeDepth
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
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color)
                            } else {
                                pixels[bufferOffset] = ((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color
                            }
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color)
                            } else {
                                pixels[bufferOffset] = ((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color
                            }
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color)
                            } else {
                                pixels[bufferOffset] = ((pixel and 0xFF00FF) * alpha shr 8 and 0xFF00FF) + (alpha * (pixel and 0xFF00) shr 8 and 0xFF00) + color
                            }
                        }
                        depth += slopeDepth
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
                        depth += slopeDepth
                    }
                    return
                }
                if (xA != 0 && xB <= clipWidth - 1) {
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += slopeDepth
                    }
                    x = xB - xA and 0x3
                    while (--x >= 0) {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset - 1] = pixels[bufferOffset]
                        }
                        depth += slopeDepth
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
                val modulatedArgb = 0xff000000.toInt() or (materialColorRgb shr 16 and 0xff) * (floorColorA shr 16 and 0xff) shl 8 and 0xff0000 or (floorColorA shr 8 and 0xff) * (materialColorRgb shr 8 and 0xff) and 0xff00 or (materialColorRgb and 0xff) * (floorColorA and 0xff) shr 8
                drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, zNearA, zNearB, zNearC, ColorUtil.mixColorsWithTransparency(modulatedArgb, fogColor, fogDensityA), ColorUtil.mixColorsWithTransparency(modulatedArgb, fogColor, fogDensityB), ColorUtil.mixColorsWithTransparency(modulatedArgb, fogColor, fogDensityC))
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
                val modulatedArgb = 0xff000000.toInt() or (materialColorRgb shr 16 and 0xff) * (floorColorA shr 16 and 0xff) shl 8 and 0xff0000 or (floorColorA shr 8 and 0xff) * (materialColorRgb shr 8 and 0xff) and 0xff00 or (materialColorRgb and 0xff) * (floorColorA and 0xff) shr 8
                drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, zNearA, zNearB, zNearC, ColorUtil.mixColorsWithTransparency(modulatedArgb, fogColor, fogDensityA), ColorUtil.mixColorsWithTransparency(modulatedArgb, fogColor, fogDensityB), ColorUtil.mixColorsWithTransparency(modulatedArgb, fogColor, fogDensityC))
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
                val modulatedArgb = 0xff000000.toInt() or (materialColorRgb shr 16 and 0xff) * (floorColorA shr 16 and 0xff) shl 8 and 0xff0000 or (floorColorA shr 8 and 0xff) * (materialColorRgb shr 8 and 0xff) and 0xff00 or (materialColorRgb and 0xff) * (floorColorA and 0xff) shr 8
                drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, zNearA, zNearB, zNearC, ColorUtil.mixColorsWithTransparency(modulatedArgb, fogColor, fogDensityA), ColorUtil.mixColorsWithTransparency(modulatedArgb, fogColor, fogDensityB), ColorUtil.mixColorsWithTransparency(modulatedArgb, fogColor, fogDensityC))
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
        depthWrite: Boolean, transparent: Boolean,
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
            if (skipDepthTest) {
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
                                var currentPixel0 = pixels[invertedAlpha]
                                var pixelSum0 = colorRgb + currentPixel0
                                var channelSum0 = (currentPixel0 and 0xff00ff) + (colorRgb and 0xff00ff)
                                currentPixel0 = (channelSum0 and 0x1000100) + (pixelSum0 - channelSum0 and 0x10000)
                                pixels[invertedAlpha] = 0xff000000.toInt() or (pixelSum0 - currentPixel0) or (currentPixel0 - (currentPixel0 ushr 8))
                                pixelSum0 = bufferOffset++
                                var currentPixel1 = pixels[pixelSum0]
                                var pixelSum1 = colorRgb + currentPixel1
                                var channelSum1 = (currentPixel1 and 0xff00ff) + (colorRgb and 0xff00ff)
                                currentPixel1 = (channelSum1 and 0x1000100) + (pixelSum1 - channelSum1 and 0x10000)
                                pixels[pixelSum0] = 0xff000000.toInt() or (pixelSum1 - currentPixel1) or (currentPixel1 - (currentPixel1 ushr 8))
                                pixelSum1 = bufferOffset++
                                var currentPixel2 = pixels[pixelSum1]
                                var pixelSum2 = colorRgb + currentPixel2
                                var channelSum2 = (currentPixel2 and 0xff00ff) + (colorRgb and 0xff00ff)
                                currentPixel2 = (channelSum2 and 0x1000100) + (pixelSum2 - channelSum2 and 0x10000)
                                pixels[pixelSum1] = 0xff000000.toInt() or (pixelSum2 - currentPixel2) or (currentPixel2 - (currentPixel2 ushr 8))
                                pixelSum2 = bufferOffset++
                                var currentPixel3 = pixels[pixelSum2]
                                var pixelSum3 = colorRgb + currentPixel3
                                var channelSum3 = (currentPixel3 and 0xff00ff) + (colorRgb and 0xff00ff)
                                currentPixel3 = (channelSum3 and 0x1000100) + (pixelSum3 - channelSum3 and 0x10000)
                                pixels[pixelSum2] = 0xff000000.toInt() or (pixelSum3 - currentPixel3) or (currentPixel3 - (currentPixel3 ushr 8))
                            } while (--x > 0)
                        }
                        x = xB - xA and 0x3
                        if (x > 0) {
                            colorRgb = (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                            do {
                                invertedAlpha = bufferOffset++
                                var currentPixel = pixels[invertedAlpha]
                                var pixelSum = colorRgb + currentPixel
                                var channelSum = (currentPixel and 0xff00ff) + (colorRgb and 0xff00ff)
                                currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
                                pixels[invertedAlpha] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
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
                            var currentPixel = pixels[invertedAlpha]
                            var pixelSum = pixel + currentPixel
                            var channelSum = (pixel and 0xff00ff) + (currentPixel and 0xff00ff)
                            currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
                            pixels[invertedAlpha] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
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
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb
                                        if (depthWrite) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb
                                        if (depthWrite) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb
                                        if (depthWrite) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb
                                        if (depthWrite) {
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
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        pixels[bufferOffset] = colorRgb
                                        if (depthWrite) {
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
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        pixel = pixels[bufferOffset]
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                        }
                                        if (depthWrite) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        pixel = pixels[bufferOffset]
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                        }
                                        if (depthWrite) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        pixel = pixels[bufferOffset]
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                        }
                                        if (depthWrite) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        pixel = pixels[bufferOffset]
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                        }
                                        if (depthWrite) {
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
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        pixel = pixels[bufferOffset]
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                        }
                                        if (depthWrite) {
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
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        var currentPixel = pixels[bufferOffset]
                                        var pixelSum = colorRgb + currentPixel
                                        var channelSum = (currentPixel and 0xff00ff) + (colorRgb and 0xff00ff)
                                        currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
                                        pixels[bufferOffset] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
                                        if (depthWrite) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        var currentPixel = pixels[bufferOffset]
                                        var pixelSum = colorRgb + currentPixel
                                        var channelSum = (currentPixel and 0xff00ff) + (colorRgb and 0xff00ff)
                                        currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
                                        pixels[bufferOffset] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
                                        if (depthWrite) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        var currentPixel = pixels[bufferOffset]
                                        var pixelSum = colorRgb + currentPixel
                                        var channelSum = (currentPixel and 0xff00ff) + (colorRgb and 0xff00ff)
                                        currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
                                        pixels[bufferOffset] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
                                        if (depthWrite) {
                                            depthBuf[bufferOffset] = depth
                                        }
                                    }
                                    depth += slopeDepth
                                    bufferOffset++
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        var currentPixel = pixels[bufferOffset]
                                        var pixelSum = colorRgb + currentPixel
                                        var channelSum = (currentPixel and 0xff00ff) + (colorRgb and 0xff00ff)
                                        currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
                                        pixels[bufferOffset] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
                                        if (depthWrite) {
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
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        var currentPixel = pixels[bufferOffset]
                                        var pixelSum = colorRgb + currentPixel
                                        var channelSum = (currentPixel and 0xff00ff) + (colorRgb and 0xff00ff)
                                        currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
                                        pixels[bufferOffset] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
                                        if (depthWrite) {
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
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        pixel = (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                        var currentPixel = pixels[bufferOffset]
                                        var pixelSum = pixel + currentPixel
                                        var channelSum = (pixel and 0xff00ff) + (currentPixel and 0xff00ff)
                                        currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
                                        pixels[bufferOffset] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
                                        if (depthWrite) {
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
                                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                        colorRgb = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                        colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + ((colorRgb and 0xff00) * invertedAlpha shr 8 and 0xff00)
                                        pixel = pixels[bufferOffset]
                                        if (transparent) {
                                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                        } else {
                                            pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                        }

                                        if (depthWrite) {
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
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                    pixels[bufferOffset] = 0xff000000.toInt() or (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                    if (depthWrite) {
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
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                    pixels[bufferOffset] = colorRgb
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                    pixels[bufferOffset] = colorRgb
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                    pixels[bufferOffset] = colorRgb
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
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
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
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
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                    pixel = pixels[bufferOffset]
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                    }
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                    pixel = pixels[bufferOffset]
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                    }
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                    pixel = pixels[bufferOffset]
                                    if (transparent) {
                                        pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                    } else {
                                        pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                    }
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
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
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
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
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                    var currentPixel = pixels[bufferOffset]
                                    var pixelSum = colorRgb + currentPixel
                                    var channelSum = (currentPixel and 0xff00ff) + (colorRgb and 0xff00ff)
                                    currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
                                    pixels[bufferOffset] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                    var currentPixel = pixels[bufferOffset]
                                    var pixelSum = colorRgb + currentPixel
                                    var channelSum = (currentPixel and 0xff00ff) + (colorRgb and 0xff00ff)
                                    currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
                                    pixels[bufferOffset] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                    var currentPixel = pixels[bufferOffset]
                                    var pixelSum = colorRgb + currentPixel
                                    var channelSum = (currentPixel and 0xff00ff) + (colorRgb and 0xff00ff)
                                    currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
                                    pixels[bufferOffset] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
                                }
                                depth += slopeDepth
                                bufferOffset++
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                    var currentPixel = pixels[bufferOffset]
                                    var pixelSum = colorRgb + currentPixel
                                    var channelSum = (currentPixel and 0xff00ff) + (colorRgb and 0xff00ff)
                                    currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
                                    pixels[bufferOffset] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
                                }
                                depth += slopeDepth
                            } while (--x > 0)
                        }
                        x = xB - xA and 0x3
                        if (x > 0) {
                            colorRgb = (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                            do {
                                bufferOffset++
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                    var currentPixel = pixels[bufferOffset]
                                    var pixelSum = colorRgb + currentPixel
                                    var channelSum = (currentPixel and 0xff00ff) + (colorRgb and 0xff00ff)
                                    currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
                                    pixels[bufferOffset] = 0xff000000.toInt() or (pixelSum - currentPixel) or (currentPixel - (currentPixel ushr 8))
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
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                    pixel = (red.toInt() and 0xff0000) or (green.toInt() and 0xff00) or (blue.toInt() and 0xff)
                                    var currentPixel = pixels[bufferOffset]
                                    var pixelSum = pixel + currentPixel
                                    var channelSum = (pixel and 0xff00ff) + (currentPixel and 0xff00ff)
                                    currentPixel = (channelSum and 0x1000100) + (pixelSum - channelSum and 0x10000)
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
                                if (!depthWrite || depth < depthBuf[bufferOffset]) {
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
                            if (!depthWrite || depth < depthBuf[bufferOffset]) {
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
                    val materialColorRgb = ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(safemodeToolkit.getMaterialColor(materialId)).toInt() and 0xffff]
                    val modulatedArgb = 0xff000000.toInt() or (materialColorRgb shr 16 and 0xff) * (colorA shr 16 and 0xff) shl 8 and 0xff0000 or (colorA shr 8 and 0xff) * (materialColorRgb shr 8 and 0xff) and 0xff00 or (materialColorRgb and 0xff) * (colorA and 0xff) shr 8
                    drawTriangle(true, depthWrite, transparent, yA, yB, yC, xA, xB, xC, zNearA, zNearB, zNearC, ColorUtil.mixColorsWithTransparency(modulatedArgb, fogColor, fogDensityA), ColorUtil.mixColorsWithTransparency(modulatedArgb, fogColor, fogDensityB), ColorUtil.mixColorsWithTransparency(modulatedArgb, fogColor, fogDensityC))
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
                    var tmpInt = colorA
                    colorA = colorB
                    colorB = tmpInt
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
                    var tmpInt = colorA
                    colorA = colorC
                    colorC = tmpInt
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
            var slopeZNearBa = 0.0f
            var slopeZFarBa = 0.0f
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
                slopeZNearBa = (zNearB - zNearA) / lengthDeltaYAb
                slopeZFarBa = (zFarB - zFarA) / lengthDeltaYAb
                slopeUBa = (uB - uA) / lengthDeltaYAb
                slopeVBa = (vB - vA) / lengthDeltaYAb
                slopeFogDensityBa = (fogDensityB - fogDensityA) / lengthDeltaYAb
                slopeAlphaBa = (colorBAlpha - colorAAlpha) / lengthDeltaYAb
                slopeRedBa = (colorBRed - colorARed) / lengthDeltaYAb
                slopeGreenBa = (colorBGreen - colorAGreen) / lengthDeltaYAb
                slopeBlueBa = (colorBBlue - colorABlue) / lengthDeltaYAb
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
            if (yC != yB) {
                val lengthDeltaYCb = yC - yB
                slopeXCb = (xC - xB) / lengthDeltaYCb
                slopeZNearCb = (zNearC - zNearB) / lengthDeltaYCb
                slopeZFarCb = (zFarC - zFarB) / lengthDeltaYCb
                slopeUCb = (uC - uB) / lengthDeltaYCb
                slopeVCb = (vC - vB) / lengthDeltaYCb
                slopeFogDensityCb = (fogDensityC - fogDensityB) / lengthDeltaYCb
                slopeAlphaCb = (colorCAlpha - colorBAlpha) / lengthDeltaYCb
                slopeRedCb = (colorCRed - colorBRed) / lengthDeltaYCb
                slopeGreenCb = (colorCGreen - colorBGreen) / lengthDeltaYCb
                slopeBlueCb = (colorCBlue - colorBBlue) / lengthDeltaYCb
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
            if (yA != yC) {
                val lengthDeltaYAc = yA - yC
                slopeXAc = (xA - xC) / lengthDeltaYAc
                slopeZNearAc = (zNearA - zNearC) / lengthDeltaYAc
                slopeZFarAc = (zFarA - zFarC) / lengthDeltaYAc
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
                            yA += scanlineWidth
                        }
                        while (--yC >= 0.0f) {
                            drawMaterialisedScanline(depthWrite, transparent, colourBuffer!!, yA.toInt(), xC.toInt(), xB.toInt(), zNearC, zNearB, zFarC, zFarB, uC, uB, vC, vB, fogDensityC, fogDensityB, colorCAlpha, colorBAlpha, colorCRed, colorBRed, colorCGreen, colorBGreen, colorCBlue, colorBBlue)
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
                            yA += scanlineWidth
                        }
                        while (--yC >= 0.0f) {
                            drawMaterialisedScanline(depthWrite, transparent, colourBuffer!!, yA.toInt(), xB.toInt(), xC.toInt(), zNearB, zNearC, zFarB, zFarC, uB, uC, vB, vC, fogDensityB, fogDensityC, colorBAlpha, colorCAlpha, colorBRed, colorCRed, colorBGreen, colorCGreen, colorBBlue, colorCBlue)
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
                            yA += scanlineWidth
                        }
                        while (--yB >= 0.0f) {
                            drawMaterialisedScanline(depthWrite, transparent, colourBuffer!!, yA.toInt(), xA.toInt(), xC.toInt(), zNearA, zNearC, zFarA, zFarC, uA, uC, vA, vC, fogDensityA, fogDensityC, colorAAlpha, colorCAlpha, colorARed, colorCRed, colorAGreen, colorCGreen, colorABlue, colorCBlue)
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
                            yA += scanlineWidth
                        }
                        while (--yB >= 0.0f) {
                            drawMaterialisedScanline(depthWrite, transparent, colourBuffer!!, yA.toInt(), xC.toInt(), xA.toInt(), zNearC, zNearA, zFarC, zFarA, uC, uA, vC, vA, fogDensityC, fogDensityA, colorCAlpha, colorAAlpha, colorCRed, colorARed, colorCGreen, colorAGreen, colorCBlue, colorABlue)
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
                            yA += scanlineWidth
                        }
                    }
                }
            }
        }
    }

    fun drawTriangle(
        colorWrite: Boolean, depthWrite: Boolean, transparent: Boolean,
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
            drawFlatTriangle(false, depthWrite, transparent, yA, yB, yC, xA, xB, xC, zA, zB, zC, 0)
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
            val scaleXCb = if (yC != yB) (xC - xB) / (yC - yB) else 0.0f
            val scaleXBa = if (yB != yA) slopeXBa / slopeYBa else 0.0f
            val scaleXCa = if (yC != yA) slopeXCa / slopeYCa else 0.0f
            val determinant = slopeXBa * slopeYCa - slopeXCa * slopeYBa
            if (determinant != 0.0f) {
                val slopeDepthX = (slopeZBa * slopeYCa - slopeZCa * slopeYBa) / determinant
                val slopeDepthY = (slopeZCa * slopeXBa - slopeZBa * slopeXCa) / determinant
                val slopeRedX = (colorRedBa * slopeYCa - colorRedCa * slopeYBa) / determinant
                val slopeRedY = (colorRedCa * slopeXBa - colorRedBa * slopeXCa) / determinant
                val slopeGreenX = (colorGreenBa * slopeYCa - colorGreenCa * slopeYBa) / determinant
                val slopeGreenY = (colorGreenCa * slopeXBa - colorGreenBa * slopeXCa) / determinant
                val slopeBlueX = (colorBlueBa * slopeYCa - colorBlueCa * slopeYBa) / determinant
                val slopeBlueY = (colorBlueCa * slopeXBa - colorBlueBa * slopeXCa) / determinant
                var blue: Float
                var red: Float
                var green: Float
                val colourBuf = colourBuffer!!
                if (yA <= yB && yA <= yC) {
                    if (yA < clipHeight) {
                        if (yB > clipHeight) yB = clipHeight.toFloat()
                        if (yC > clipHeight) yC = clipHeight.toFloat()
                        zA = zA - slopeDepthX * xA + slopeDepthX
                        red = (colorA and 0xff0000) - slopeRedX * xA + slopeRedX
                        green = (colorA and 0xff00) - slopeGreenX * xA + slopeGreenX
                        blue = (colorA and 0xff) - slopeBlueX * xA + slopeBlueX
                        if (yB < yC) {
                            xC = xA
                            if (yA < 0.0f) {
                                xC = xA - scaleXCa * yA
                                xA -= scaleXBa * yA
                                zA -= slopeDepthY * yA
                                red -= slopeRedY * yA
                                green -= slopeGreenY * yA
                                blue -= slopeBlueY * yA
                                yA = 0.0f
                            }
                            if (yB < 0.0f) {
                                xB -= scaleXCb * yB
                                yB = 0.0f
                            }
                            if ((yA == yB || scaleXCa >= scaleXBa) && (yA != yB || scaleXCa <= scaleXCb)) {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt() - yB
                                yB -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yB >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yA.toInt(), xA.toInt(), xC.toInt(), zA, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xC += scaleXCa
                                    xA += scaleXBa
                                    zA += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yA += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yA.toInt(), xB.toInt(), xC.toInt(), zA, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xC += scaleXCa
                                    xB += scaleXCb
                                    zA += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yA += scanlineWidth
                                }
                            } else {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt() - yB
                                yB -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yB >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yA.toInt(), xC.toInt(), xA.toInt(), zA, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xC += scaleXCa
                                    xA += scaleXBa
                                    zA += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yA += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yA.toInt(), xC.toInt(), xB.toInt(), zA, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xC += scaleXCa
                                    xB += scaleXCb
                                    zA += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yA += scanlineWidth
                                }
                            }
                        } else {
                            xB = xA
                            if (yA < 0.0f) {
                                xB = xA - scaleXCa * yA
                                xA -= scaleXBa * yA
                                zA -= slopeDepthY * yA
                                red -= slopeRedY * yA
                                green -= slopeGreenY * yA
                                blue -= slopeBlueY * yA
                                yA = 0.0f
                            }
                            if (yC < 0.0f) {
                                xC -= scaleXCb * yC
                                yC = 0.0f
                            }
                            if (yA != yC && scaleXCa < scaleXBa || yA == yC && scaleXCb > scaleXBa) {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt() - yC
                                yC -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yA.toInt(), xB.toInt(), xA.toInt(), zA, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xB += scaleXCa
                                    xA += scaleXBa
                                    zA += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yA += scanlineWidth
                                }
                                while (--yB >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yA.toInt(), xC.toInt(), xA.toInt(), zA, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xC += scaleXCb
                                    xA += scaleXBa
                                    zA += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yA += scanlineWidth
                                }
                            } else {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt() - yC
                                yC -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yA.toInt(), xA.toInt(), xB.toInt(), zA, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xB += scaleXCa
                                    xA += scaleXBa
                                    zA += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yA += scanlineWidth
                                }
                                while (--yB >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yA.toInt(), xA.toInt(), xC.toInt(), zA, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xC += scaleXCb
                                    xA += scaleXBa
                                    zA += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yA += scanlineWidth
                                }
                            }
                        }
                    }
                } else if (yB <= yC) {
                    if (yB < clipHeight) {
                        if (yC > clipHeight) yC = clipHeight.toFloat()
                        if (yA > clipHeight) yA = clipHeight.toFloat()
                        zB = zB - slopeDepthX * xB + slopeDepthX
                        red = (colorB and 0xff0000) - slopeRedX * xB + slopeRedX
                        green = (colorB and 0xff00) - slopeGreenX * xB + slopeGreenX
                        blue = (colorB and 0xff) - slopeBlueX * xB + slopeBlueX
                        if (yC < yA) {
                            xA = xB
                            if (yB < 0.0f) {
                                xA = xB - scaleXBa * yB
                                xB -= scaleXCb * yB
                                zB -= slopeDepthY * yB
                                red -= slopeRedY * yB
                                green -= slopeGreenY * yB
                                blue -= slopeBlueY * yB
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
                                    drawScanline(depthWrite, transparent, colourBuf, yB.toInt(), xB.toInt(), xA.toInt(), zB, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xA += scaleXBa
                                    xB += scaleXCb
                                    zB += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yB += scanlineWidth
                                }
                                while (--yA >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yB.toInt(), xC.toInt(), xA.toInt(), zB, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xA += scaleXBa
                                    xC += scaleXCa
                                    zB += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yB += scanlineWidth
                                }
                            } else {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt() - yC
                                yC -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yB.toInt(), xA.toInt(), xB.toInt(), zB, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xA += scaleXBa
                                    xB += scaleXCb
                                    zB += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yB += scanlineWidth
                                }
                                while (--yA >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yB.toInt(), xA.toInt(), xC.toInt(), zB, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xA += scaleXBa
                                    xC += scaleXCa
                                    zB += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yB += scanlineWidth
                                }
                            }
                        } else {
                            xC = xB
                            if (yB < 0.0f) {
                                xC = xB - scaleXBa * yB
                                xB -= scaleXCb * yB
                                zB -= slopeDepthY * yB
                                red -= slopeRedY * yB
                                green -= slopeGreenY * yB
                                blue -= slopeBlueY * yB
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
                                    drawScanline(depthWrite, transparent, colourBuf, yB.toInt(), xC.toInt(), xB.toInt(), zB, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xC += scaleXBa
                                    xB += scaleXCb
                                    zB += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yB += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yB.toInt(), xA.toInt(), xB.toInt(), zB, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xA += scaleXCa
                                    xB += scaleXCb
                                    zB += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yB += scanlineWidth
                                }
                            } else {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt() - yA
                                yA -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yA >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yB.toInt(), xB.toInt(), xC.toInt(), zB, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xC += scaleXBa
                                    xB += scaleXCb
                                    zB += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yB += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawScanline(depthWrite, transparent, colourBuf, yB.toInt(), xB.toInt(), xA.toInt(), zB, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                    xA += scaleXCa
                                    xB += scaleXCb
                                    zB += slopeDepthY
                                    red += slopeRedY
                                    green += slopeGreenY
                                    blue += slopeBlueY
                                    yB += scanlineWidth
                                }
                            }
                        }
                    }
                } else if (yC < clipHeight) {
                    if (yA > clipHeight) yA = clipHeight.toFloat()
                    if (yB > clipHeight) yB = clipHeight.toFloat()
                    zC = zC - slopeDepthX * xC + slopeDepthX
                    red = (colorC and 0xff0000) - slopeRedX * xC + slopeRedX
                    green = (colorC and 0xff00) - slopeGreenX * xC + slopeGreenX
                    blue = (colorC and 0xff) - slopeBlueX * xC + slopeBlueX
                    if (yA < yB) {
                        xB = xC
                        if (yC < 0.0f) {
                            xB = xC - scaleXCb * yC
                            xC -= scaleXCa * yC
                            zC -= slopeDepthY * yC
                            red -= slopeRedY * yC
                            green -= slopeGreenY * yC
                            blue -= slopeBlueY * yC
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
                                drawScanline(depthWrite, transparent, colourBuf, yC.toInt(), xB.toInt(), xC.toInt(), zC, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                xB += scaleXCb
                                xC += scaleXCa
                                zC += slopeDepthY
                                red += slopeRedY
                                green += slopeGreenY
                                blue += slopeBlueY
                                yC += scanlineWidth
                            }
                            while (--yB >= 0.0f) {
                                drawScanline(depthWrite, transparent, colourBuf, yC.toInt(), xB.toInt(), xA.toInt(), zC, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                xB += scaleXCb
                                xA += scaleXBa
                                zC += slopeDepthY
                                red += slopeRedY
                                green += slopeGreenY
                                blue += slopeBlueY
                                yC += scanlineWidth
                            }
                        } else {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt() - yA
                            yA -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yA >= 0.0f) {
                                drawScanline(depthWrite, transparent, colourBuf, yC.toInt(), xC.toInt(), xB.toInt(), zC, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                xB += scaleXCb
                                xC += scaleXCa
                                zC += slopeDepthY
                                red += slopeRedY
                                green += slopeGreenY
                                blue += slopeBlueY
                                yC += scanlineWidth
                            }
                            while (--yB >= 0.0f) {
                                drawScanline(depthWrite, transparent, colourBuf, yC.toInt(), xA.toInt(), xB.toInt(), zC, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                xB += scaleXCb
                                xA += scaleXBa
                                zC += slopeDepthY
                                red += slopeRedY
                                green += slopeGreenY
                                blue += slopeBlueY
                                yC += scanlineWidth
                            }
                        }
                    } else {
                        xA = xC
                        if (yC < 0.0f) {
                            xA = xC - scaleXCb * yC
                            xC -= scaleXCa * yC
                            zC -= slopeDepthY * yC
                            red -= slopeRedY * yC
                            green -= slopeGreenY * yC
                            blue -= slopeBlueY * yC
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
                                drawScanline(depthWrite, transparent, colourBuf, yC.toInt(), xA.toInt(), xC.toInt(), zC, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                xA += scaleXCb
                                xC += scaleXCa
                                zC += slopeDepthY
                                red += slopeRedY
                                green += slopeGreenY
                                blue += slopeBlueY
                                yC += scanlineWidth
                            }
                            while (--yA >= 0.0f) {
                                drawScanline(depthWrite, transparent, colourBuf, yC.toInt(), xB.toInt(), xC.toInt(), zC, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                xB += scaleXBa
                                xC += scaleXCa
                                zC += slopeDepthY
                                red += slopeRedY
                                green += slopeGreenY
                                blue += slopeBlueY
                                yC += scanlineWidth
                            }
                        } else {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt() - yB
                            yB -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yB >= 0.0f) {
                                drawScanline(depthWrite, transparent, colourBuf, yC.toInt(), xC.toInt(), xA.toInt(), zC, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                xA += scaleXCb
                                xC += scaleXCa
                                zC += slopeDepthY
                                red += slopeRedY
                                green += slopeGreenY
                                blue += slopeBlueY
                                yC += scanlineWidth
                            }
                            while (--yA >= 0.0f) {
                                drawScanline(depthWrite, transparent, colourBuf, yC.toInt(), xC.toInt(), xB.toInt(), zC, slopeDepthX, red, slopeRedX, green, slopeGreenX, blue, slopeBlueX)
                                xB += scaleXBa
                                xC += scaleXCa
                                zC += slopeDepthY
                                red += slopeRedY
                                green += slopeGreenY
                                blue += slopeBlueY
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
            shadedColorMin = minOf(colorA, colorB, colorC)
            shadedColorMax = maxOf(colorA, colorB, colorC)
            var slopeXBa = xB - xA
            var slopeYBa = yB - yA
            var slopeXCa = xC - xA
            var slopeYCa = yC - yA
            var slopeColorBa = colorB - colorA
            var slopeColorCa = colorC - colorA
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

            var determinant = slopeXBa * slopeYCa - slopeXCa * slopeYBa
            if (determinant != 0.0f) {
                var slopeColorX = (slopeColorBa * slopeYCa - slopeColorCa * slopeYBa) / determinant
                var slopeColorY = (slopeColorCa * slopeXBa - slopeColorBa * slopeXCa) / determinant
                var slopeDepthX = (slopeZBa * slopeYCa - slopeZCa * slopeYBa) / determinant
                var slopeDepthY = (slopeZCa * slopeXBa - slopeZBa * slopeXCa) / determinant
                if (yA <= yB && yA <= yC) {
                    if (yA < clipHeight) {
                        if (yB > clipHeight) {
                            yB = clipHeight.toFloat()
                        }
                        if (yC > clipHeight) {
                            yC = clipHeight.toFloat()
                        }
                        colorA = colorA - slopeColorX * xA + slopeColorX
                        zA = zA - slopeDepthX * xA + slopeDepthX
                        if (yB < yC) {
                            xC = xA
                            if (yA < 0.0f) {
                                xC = xA - scaleXCa * yA
                                xA -= scaleXBa * yA
                                colorA -= slopeColorY * yA
                                zA -= slopeDepthY * yA
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
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xC.toInt(), xA.toInt(), colorA, slopeColorX, zA, slopeDepthX)
                                    xC += scaleXCa
                                    xA += scaleXBa
                                    colorA += slopeColorY
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xC.toInt(), xB.toInt(), colorA, slopeColorX, zA, slopeDepthX)
                                    xC += scaleXCa
                                    xB += scaleXCb
                                    colorA += slopeColorY
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                            } else {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = ((yC + 0.5f).toInt() - yB.toInt()).toFloat()
                                yB -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yB >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xA.toInt(), xC.toInt(), colorA, slopeColorX, zA, slopeDepthX)
                                    xC += scaleXCa
                                    xA += scaleXBa
                                    colorA += slopeColorY
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xB.toInt(), xC.toInt(), colorA, slopeColorX, zA, slopeDepthX)
                                    xC += scaleXCa
                                    xB += scaleXCb
                                    colorA += slopeColorY
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                            }
                        } else {
                            xB = xA
                            if (yA < 0.0f) {
                                xB = xA - scaleXCa * yA
                                xA -= scaleXBa * yA
                                colorA -= slopeColorY * yA
                                zA -= slopeDepthY * yA
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
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xA.toInt(), xB.toInt(), colorA, slopeColorX, zA, slopeDepthX)
                                    xB += scaleXCa
                                    xA += scaleXBa
                                    colorA += slopeColorY
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                                while (--yB >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xA.toInt(), xC.toInt(), colorA, slopeColorX, zA, slopeDepthX)
                                    xC += scaleXCb
                                    xA += scaleXBa
                                    colorA += slopeColorY
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                            } else {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yB = ((yB + 0.5f).toInt() - yC.toInt()).toFloat()
                                yC -= yA

                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xB.toInt(), xA.toInt(), colorA, slopeColorX, zA, slopeDepthX)
                                    xB += scaleXCa
                                    xA += scaleXBa
                                    colorA += slopeColorY
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                                while (--yB >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yA.toInt(), xC.toInt(), xA.toInt(), colorA, slopeColorX, zA, slopeDepthX)
                                    xC += scaleXCb
                                    xA += scaleXBa
                                    colorA += slopeColorY
                                    zA += slopeDepthY
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
                        colorB = colorB - slopeColorX * xB + slopeColorX
                        zB = zB - slopeDepthX * xB + slopeDepthX
                        if (yC < yA) {
                            xA = xB
                            if (yB < 0.0f) {
                                xA = xB - scaleXBa * yB
                                xB -= scaleXCb * yB
                                colorB -= slopeColorY * yB
                                zB -= slopeDepthY * yB
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
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xA.toInt(), xB.toInt(), colorB, slopeColorX, zB, slopeDepthX)
                                    xA += scaleXBa
                                    xB += scaleXCb
                                    colorB += slopeColorY
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                                while (--yA >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xA.toInt(), xC.toInt(), colorB, slopeColorX, zB, slopeDepthX)
                                    xA += scaleXBa
                                    xC += scaleXCa
                                    colorB += slopeColorY
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                            } else {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yA = ((yA + 0.5f).toInt() - yC.toInt()).toFloat()
                                yC -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xB.toInt(), xA.toInt(), colorB, slopeColorX, zB, slopeDepthX)
                                    xA += scaleXBa
                                    xB += scaleXCb
                                    colorB += slopeColorY
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                                while (--yA >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xC.toInt(), xA.toInt(), colorB, slopeColorX, zB, slopeDepthX)
                                    xA += scaleXBa
                                    xC += scaleXCa
                                    colorB += slopeColorY
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                            }
                        } else {
                            xC = xB
                            if (yB < 0.0f) {
                                xC = xB - scaleXBa * yB
                                xB -= scaleXCb * yB
                                colorB -= slopeColorY * yB
                                zB -= slopeDepthY * yB
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
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xC.toInt(), xB.toInt(), colorB, slopeColorX, zB, slopeDepthX)
                                    xC += scaleXBa
                                    xB += scaleXCb
                                    colorB += slopeColorY
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xA.toInt(), xB.toInt(), colorB, slopeColorX, zB, slopeDepthX)
                                    xA += scaleXCa
                                    xB += scaleXCb
                                    colorB += slopeColorY
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                            } else {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = ((yC + 0.5f).toInt() - yA.toInt()).toFloat()
                                yA -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yA >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xB.toInt(), xC.toInt(), colorB, slopeColorX, zB, slopeDepthX)
                                    xC += scaleXBa
                                    xB += scaleXCb
                                    colorB += slopeColorY
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawShadedScanline(depthWrite, transparent, colourBuffer, yB.toInt(), xB.toInt(), xA.toInt(), colorB, slopeColorX, zB, slopeDepthX)
                                    xA += scaleXCa
                                    xB += scaleXCb
                                    colorB += slopeColorY
                                    zB += slopeDepthY
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
                    colorC = colorC - slopeColorX * xC + slopeColorX
                    zC = zC - slopeDepthX * xC + slopeDepthX
                    if (yA < yB) {
                        xB = xC
                        if (yC < 0.0f) {
                            xB = xC - scaleXCb * yC
                            xC -= scaleXCa * yC
                            colorC -= slopeColorY * yC
                            zC -= slopeDepthY * yC
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
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xB.toInt(), xC.toInt(), colorC, slopeColorX, zC, slopeDepthX)
                                xB += scaleXCb
                                xC += scaleXCa
                                colorC += slopeColorY
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                            while (--yB >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xB.toInt(), xA.toInt(), colorC, slopeColorX, zC, slopeDepthX)
                                xB += scaleXCb
                                xA += scaleXBa
                                colorC += slopeColorY
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                        } else {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt().toFloat()
                            yB = ((yB + 0.5f).toInt() - yA.toInt()).toFloat()
                            yA -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yA >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xC.toInt(), xB.toInt(), colorC, slopeColorX, zC, slopeDepthX)
                                xB += scaleXCb
                                xC += scaleXCa
                                colorC += slopeColorY
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                            while (--yB >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xA.toInt(), xB.toInt(), colorC, slopeColorX, zC, slopeDepthX)
                                xB += scaleXCb
                                xA += scaleXBa
                                colorC += slopeColorY
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                        }
                    } else {
                        xA = xC
                        if (yC < 0.0f) {
                            xA = xC - scaleXCb * yC
                            xC -= scaleXCa * yC
                            colorC -= slopeColorY * yC
                            zC -= slopeDepthY * yC
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
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xA.toInt(), xC.toInt(), colorC, slopeColorX, zC, slopeDepthX)
                                xA += scaleXCb
                                xC += scaleXCa
                                colorC += slopeColorY
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                            while (--yA >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xB.toInt(), xC.toInt(), colorC, slopeColorX, zC, slopeDepthX)
                                xB += scaleXBa
                                xC += scaleXCa
                                colorC += slopeColorY
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                        } else {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt().toFloat()
                            yA = ((yA + 0.5f).toInt() - yB.toInt()).toFloat()
                            yB -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yB >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xC.toInt(), xA.toInt(), colorC, slopeColorX, zC, slopeDepthX)
                                xA += scaleXCb
                                xC += scaleXCa
                                colorC += slopeColorY
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                            while (--yA >= 0.0f) {
                                drawShadedScanline(depthWrite, transparent, colourBuffer, yC.toInt(), xC.toInt(), xB.toInt(), colorC, slopeColorX, zC, slopeDepthX)
                                xB += scaleXBa
                                xC += scaleXCa
                                colorC += slopeColorY
                                zC += slopeDepthY
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
            val determinant = slopeXBa * slopeYCa - slopeXCa * slopeYBa
            if (determinant != 0.0f) {
                val slopeDepthX = (slopeZBa * slopeYCa - slopeZCa * slopeYBa) / determinant
                val slopeDepthY = (slopeZCa * slopeXBa - slopeZBa * slopeXCa) / determinant
                val colourBuf = colourBuffer!!
                if (yA <= yB && yA <= yC) {
                    if (yA < clipHeight) {
                        if (yB > clipHeight) {
                            yB = clipHeight.toFloat()
                        }
                        if (yC > clipHeight) {
                            yC = clipHeight.toFloat()
                        }
                        zA = zA - slopeDepthX * xA + slopeDepthX
                        if (yB < yC) {
                            xC = xA
                            if (yA < 0.0f) {
                                xC = xA - scaleXCa * yA
                                xA -= scaleXBa * yA
                                zA -= slopeDepthY * yA
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
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xC.toInt(), xA.toInt(), zA, slopeDepthX)
                                    xC += scaleXCa
                                    xA += scaleXBa
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xC.toInt(), xB.toInt(), zA, slopeDepthX)
                                    xC += scaleXCa
                                    xB += scaleXCb
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                            } else {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt() - yB
                                yB -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yB >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xA.toInt(), xC.toInt(), zA, slopeDepthX)
                                    xC += scaleXCa
                                    xA += scaleXBa
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xB.toInt(), xC.toInt(), zA, slopeDepthX)
                                    xC += scaleXCa
                                    xB += scaleXCb
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                            }
                        } else {
                            xB = xA
                            if (yA < 0.0f) {
                                xB = xA - scaleXCa * yA
                                xA -= scaleXBa * yA
                                zA -= slopeDepthY * yA
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
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xA.toInt(), xB.toInt(), zA, slopeDepthX)
                                    xB += scaleXCa
                                    xA += scaleXBa
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                                while (--yB >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xA.toInt(), xC.toInt(), zA, slopeDepthX)
                                    xC += scaleXCb
                                    xA += scaleXBa
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                            } else {
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yB = (yB + 0.5f).toInt() - yC
                                yC -= yA
                                yA = scanlineOffsets[yA.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xB.toInt(), xA.toInt(), zA, slopeDepthX)
                                    xB += scaleXCa
                                    xA += scaleXBa
                                    zA += slopeDepthY
                                    yA += scanlineWidth
                                }
                                while (--yB >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yA.toInt(), color, xC.toInt(), xA.toInt(), zA, slopeDepthX)
                                    xC += scaleXCb
                                    xA += scaleXBa
                                    zA += slopeDepthY
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
                        zB = zB - slopeDepthX * xB + slopeDepthX
                        if (yC < yA) {
                            xA = xB
                            if (yB < 0.0f) {
                                xA = xB - scaleXBa * yB
                                xB -= scaleXCb * yB
                                zB -= slopeDepthY * yB
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
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xB.toInt(), xA.toInt(), zB, slopeDepthX)
                                    xA += scaleXBa
                                    xB += scaleXCb
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                                while (--yA >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xC.toInt(), xA.toInt(), zB, slopeDepthX)
                                    xA += scaleXBa
                                    xC += scaleXCa
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                            } else {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt() - yC
                                yC -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xA.toInt(), xB.toInt(), zB, slopeDepthX)
                                    xA += scaleXBa
                                    xB += scaleXCb
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                                while (--yA >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xA.toInt(), xC.toInt(), zB, slopeDepthX)
                                    xA += scaleXBa
                                    xC += scaleXCa
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                            }
                        } else {
                            xC = xB
                            if (yB < 0.0f) {
                                xC = xB - scaleXBa * yB
                                xB -= scaleXCb * yB
                                zB -= slopeDepthY * yB
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
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xC.toInt(), xB.toInt(), zB, slopeDepthX)
                                    xC += scaleXBa
                                    xB += scaleXCb
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xA.toInt(), xB.toInt(), zB, slopeDepthX)
                                    xA += scaleXCa
                                    xB += scaleXCb
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                            } else {
                                yB = (yB + 0.5f).toInt().toFloat()
                                yA = (yA + 0.5f).toInt().toFloat()
                                yC = (yC + 0.5f).toInt() - yA
                                yA -= yB
                                yB = scanlineOffsets[yB.toInt()].toFloat()
                                while (--yA >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xB.toInt(), xC.toInt(), zB, slopeDepthX)
                                    xC += scaleXBa
                                    xB += scaleXCb
                                    zB += slopeDepthY
                                    yB += scanlineWidth
                                }
                                while (--yC >= 0.0f) {
                                    drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yB.toInt(), color, xB.toInt(), xA.toInt(), zB, slopeDepthX)
                                    xA += scaleXCa
                                    xB += scaleXCb
                                    zB += slopeDepthY
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
                    zC = zC - slopeDepthX * xC + slopeDepthX
                    if (yA < yB) {
                        xB = xC
                        if (yC < 0.0f) {
                            xB = xC - scaleXCb * yC
                            xC -= scaleXCa * yC
                            zC -= slopeDepthY * yC
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
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xB.toInt(), xC.toInt(), zC, slopeDepthX)
                                xB += scaleXCb
                                xC += scaleXCa
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                            while (--yB >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xB.toInt(), xA.toInt(), zC, slopeDepthX)
                                xB += scaleXCb
                                xA += scaleXBa
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                        } else {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt() - yA
                            yA -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yA >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xC.toInt(), xB.toInt(), zC, slopeDepthX)
                                xB += scaleXCb
                                xC += scaleXCa
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                            while (--yB >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xA.toInt(), xB.toInt(), zC, slopeDepthX)
                                xB += scaleXCb
                                xA += scaleXBa
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                        }
                    } else {
                        xA = xC
                        if (yC < 0.0f) {
                            xA = xC - scaleXCb * yC
                            xC -= scaleXCa * yC
                            zC -= slopeDepthY * yC
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
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xA.toInt(), xC.toInt(), zC, slopeDepthX)
                                xA += scaleXCb
                                xC += scaleXCa
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                            while (--yA >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xB.toInt(), xC.toInt(), zC, slopeDepthX)
                                xB += scaleXBa
                                xC += scaleXCa
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                        } else {
                            yC = (yC + 0.5f).toInt().toFloat()
                            yB = (yB + 0.5f).toInt().toFloat()
                            yA = (yA + 0.5f).toInt() - yB
                            yB -= yC
                            yC = scanlineOffsets[yC.toInt()].toFloat()
                            while (--yB >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xC.toInt(), xA.toInt(), zC, slopeDepthX)
                                xA += scaleXCb
                                xC += scaleXCa
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                            while (--yA >= 0.0f) {
                                drawFlatScanline(colorWrite, depthWrite, transparent, colourBuf, yC.toInt(), color, xC.toInt(), xB.toInt(), zC, slopeDepthX)
                                xB += scaleXBa
                                xC += scaleXCa
                                zC += slopeDepthY
                                yC += scanlineWidth
                            }
                        }
                    }
                }
            }
        }
    }

    fun drawMaterialisedScanline(
        depthWrite: Boolean, transparent: Boolean,
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
        var slopeZNearBa = (zNearB - zNearA) * scaleX
        var slopeZFarBa = (zFarB - zFarA) * scaleX
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
                zNearA -= slopeZNearBa * xA
                zFarA -= slopeZFarBa * xA
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
                var depthNear = 1.0f / zNearA
                var depthFar = 1.0f / zFarA
                if (!depthWrite || depthNear < depthBuf[bufferOffset]) {
                    var u = (uA * depthFar * textureSizeA).toInt()
                    if (textureRepeat) {
                        u = u and textureMaskA
                    } else if (u < 0) {
                        u = 0
                    } else if (u > textureMaskA) {
                        u = textureMaskA
                    }

                    var v = (vA * depthFar * textureSizeA).toInt()
                    if (textureRepeat) {
                        v = v and textureMaskA
                    } else if (v < 0) {
                        v = 0
                    } else if (v > textureMaskA) {
                        v = textureMaskA
                    }

                    var texel = texturePixelsA[u + v * textureSizeA]
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
                        if (depthWrite) {
                            depthBuf[bufferOffset] = depthNear
                        }
                    }
                }
                bufferOffset++
                zNearA += slopeZNearBa
                zFarA += slopeZFarBa
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
        depthWrite: Boolean, transparent: Boolean,
        pixels: IntArray, bufferOffset: Int,
        startX: Int, endX: Int,
        color: Float, slopeColor: Float,
        depth: Float, slopeDepth: Float
    ) {
        var bufferOffset = bufferOffset
        var startX = startX
        var endX = endX
        var color = color
        var slopeColor = slopeColor
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
        color += slopeColor * startX
        depth += slopeDepth * startX
        // Sub-pixel slivers sample the gradient outside the triangle, and an HSL index past the vertex range wraps through the table (grey -> red).
        val colorEnd = color + slopeColor * (endX - startX - 1)
        if (color < shadedColorMin || color > shadedColorMax || colorEnd < shadedColorMin || colorEnd > shadedColorMax) {
            val clampedStart = color.coerceIn(shadedColorMin, shadedColorMax)
            val clampedEnd = colorEnd.coerceIn(shadedColorMin, shadedColorMax)
            slopeColor = if (endX - startX > 1) (clampedEnd - clampedStart) / (endX - startX - 1) else 0.0f
            color = clampedStart
        }
        if (threadResource.zbuffering) {
            if (smoothEdges) {
                var x = endX - startX shr 2
                slopeColor *= 4.0f
                if (blendingAlpha == 0) {
                    if (x > 0) {
                        do {
                            val colorRgb = ColorUtil.hsvToRgbTable!![color.toInt() and 0xffff]
                            color += slopeColor
                            bufferOffset++
                            if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                pixels[bufferOffset] = colorRgb
                                if (depthWrite) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += slopeDepth
                            bufferOffset++
                            if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                pixels[bufferOffset] = colorRgb
                                if (depthWrite) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += slopeDepth
                            bufferOffset++
                            if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                pixels[bufferOffset] = colorRgb
                                if (depthWrite) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += slopeDepth
                            bufferOffset++
                            if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                pixels[bufferOffset] = colorRgb
                                if (depthWrite) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += slopeDepth
                        } while (--x > 0)
                    }
                    x = endX - startX and 0x3
                    if (x > 0) {
                        val colorRgb = ColorUtil.hsvToRgbTable!![color.toInt() and 0xffff]
                        do {
                            bufferOffset++
                            if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                pixels[bufferOffset] = colorRgb
                                if (depthWrite) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += slopeDepth
                        } while (--x > 0)
                    }
                } else {
                    val alpha = blendingAlpha
                    val invertedAlpha = 256 - blendingAlpha
                    if (x > 0) {
                        do {
                            var colorRgb = ColorUtil.hsvToRgbTable!![color.toInt() and 0xffff]
                            color += slopeColor
                            colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + (invertedAlpha * (colorRgb and 0xff00) shr 8 and 0xff00)
                            bufferOffset++
                            if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                } else {
                                    pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                }
                                if (depthWrite) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += slopeDepth
                            bufferOffset++
                            if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                } else {
                                    pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                }
                                if (depthWrite) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += slopeDepth
                            bufferOffset++
                            if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                } else {
                                    pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                }
                                if (depthWrite) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += slopeDepth
                            bufferOffset++
                            if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                } else {
                                    pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                }
                                if (depthWrite) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += slopeDepth
                        } while (--x > 0)
                    }
                    x = endX - startX and 0x3
                    if (x > 0) {
                        var colorRgb = ColorUtil.hsvToRgbTable!![color.toInt() and 0xffff]
                        colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + (invertedAlpha * (colorRgb and 0xff00) shr 8 and 0xff00)
                        do {
                            bufferOffset++
                            if (!depthWrite || depth < depthBuf[bufferOffset]) {
                                val pixel = pixels[bufferOffset]
                                if (transparent) {
                                    pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                                } else {
                                    pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                                }
                                if (depthWrite) {
                                    depthBuf[bufferOffset] = depth
                                }
                            }
                            depth += slopeDepth
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
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            var colorRgb = ColorUtil.hsvToRgbTable!![color.toInt() and 0xffff]
                            colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + (invertedAlpha * (colorRgb and 0xff00) shr 8 and 0xff00)
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                            } else {
                                pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                            }
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        color += slopeColor
                        depth += slopeDepth
                    } while (--x > 0)
                } else {
                    do {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = ColorUtil.hsvToRgbTable!![color.toInt() and 0xffff]
                            if (depthWrite) {
                                depthBuf[bufferOffset] = depth
                            }
                        }
                        depth += slopeDepth
                        color += slopeColor
                    } while (--x > 0)
                }
            }
        } else if (smoothEdges) {
            var x = endX - startX shr 2
            slopeColor *= 4.0f
            if (blendingAlpha == 0) {
                if (x > 0) {
                    do {
                        val colorRgb = ColorUtil.hsvToRgbTable!![color.toInt() and 0xffff]
                        color += slopeColor
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = colorRgb
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = colorRgb
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = colorRgb
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = colorRgb
                        }
                        depth += slopeDepth
                    } while (--x > 0)
                }
                x = endX - startX and 0x3
                if (x > 0) {
                    val colorRgb = ColorUtil.hsvToRgbTable!![color.toInt() and 0xffff]
                    do {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            pixels[bufferOffset] = colorRgb
                        }
                        depth += slopeDepth
                    } while (--x > 0)
                }
            } else {
                val alpha = blendingAlpha
                val invertedAlpha = 256 - blendingAlpha
                if (x > 0) {
                    do {
                        var colorRgb = ColorUtil.hsvToRgbTable!![color.toInt() and 0xffff]
                        color += slopeColor
                        colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + (invertedAlpha * (colorRgb and 0xff00) shr 8 and 0xff00)
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                            } else {
                                pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                            }
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                            } else {
                                pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                            }
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                            } else {
                                pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                            }
                        }
                        depth += slopeDepth
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                            } else {
                                pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                            }
                        }
                        depth += slopeDepth
                    } while (--x > 0)
                }
                x = endX - startX and 0x3
                if (x > 0) {
                    var colorRgb = ColorUtil.hsvToRgbTable!![color.toInt() and 0xffff]
                    colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + (invertedAlpha * (colorRgb and 0xff00) shr 8 and 0xff00)
                    do {
                        bufferOffset++
                        if (!depthWrite || depth < depthBuf[bufferOffset]) {
                            val pixel = pixels[bufferOffset]
                            if (transparent) {
                                pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                            } else {
                                pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                            }
                        }
                        depth += slopeDepth
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
                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                        var colorRgb = ColorUtil.hsvToRgbTable!![color.toInt() and 0xffff]
                        colorRgb = ((colorRgb and 0xff00ff) * invertedAlpha shr 8 and 0xff00ff) + (invertedAlpha * (colorRgb and 0xff00) shr 8 and 0xff00)
                        val pixel = pixels[bufferOffset]
                        if (transparent) {
                            pixels[bufferOffset] = (invertedAlpha or (pixel shr 24)) shl 24 or (colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00))
                        } else {
                            pixels[bufferOffset] = colorRgb + ((pixel and 0xff00ff) * alpha shr 8 and 0xff00ff) + (alpha * (pixel and 0xff00) shr 8 and 0xff00)
                        }
                    }
                    color += slopeColor
                    depth += slopeDepth
                } while (--x > 0)
            } else {
                do {
                    bufferOffset++
                    if (!depthWrite || depth < depthBuf[bufferOffset]) {
                        pixels[bufferOffset] = ColorUtil.hsvToRgbTable!![color.toInt() and 0xffff]
                    }
                    depth += slopeDepth
                    color += slopeColor
                } while (--x > 0)
            }
        }
    }
}
