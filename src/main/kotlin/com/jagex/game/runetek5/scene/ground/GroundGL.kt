package com.jagex.game.runetek5.scene.ground

import com.jagex.game.runetek5.cache.node.HashTable
import com.jagex.game.runetek5.cache.node.LinkedList
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ShortNode
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.graphics.light.LightGL
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.graphics.shadow.ShadowGL
import com.jagex.game.runetek5.graphics.shadow.ShadowMapperGL
import com.jagex.game.runetek5.graphics.texture.GlElementsArrayBuffer
import com.jagex.game.runetek5.imageproducer.AbstractDisplay
import com.jagex.game.runetek5.scene.ground.GroundFunctionMask.*
import com.jagex.game.runetek5.scene.tile.WaterTile
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GroundMaterialGL
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlVboArrayBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer
import com.jagex.game.runetek5.utils.ArrayUtils
import com.jagex.game.runetek5.utils.ColorUtil
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.buffer.ArrayBufferGL
import jaclib.memory.Stream
import jaclib.memory.heap.NativeHeapBuffer
import jaggl.OpenGL
import com.jagex.game.runetek5.toolkit.opengl.*
open class GroundGL(
    toolkit: OpenGlToolkit,
    floorFunctionMask: Int,
    waterFunctionMask: Int,
    regionSizeX: Int,
    regionSizeY: Int,
    heightsCurrentLvl: Array<IntArray>,
    heightsNextLvl: Array<IntArray>
) : Ground(regionSizeX, regionSizeY, 512, heightsCurrentLvl) {

    companion object {
        @JvmField val GROUND_MATRIX = FloatArray(16)
    }

    @JvmField var shadowMapper: ShadowMapperGL? = null
    @JvmField var tileOffsetsZ: Array<Array<IntArray?>>? = null
    @JvmField var tileOffsetsX: Array<Array<IntArray?>>? = null
    @JvmField var tileColors: Array<Array<IntArray?>>? = null
    @JvmField var anIntArrayArrayArray8662: Array<Array<IntArray?>>? = null
    @JvmField var tileBlendColors: Array<Array<IntArray?>>? = null
    @JvmField var anIntArrayArrayArray8649: Array<Array<IntArray?>>? = null
    @JvmField var materials: Array<Array<Array<GroundMaterialGL>?>>? = null
    @JvmField var shadowing: Array<ByteArray>? = null
    @JvmField var materialTable: HashTable? = null
    @JvmField var lightingZ: Array<FloatArray>? = null
    @JvmField var lightingY: Array<FloatArray>? = null
    @JvmField var lightingX: Array<FloatArray>? = null
    @JvmField var goundMaterialNodes: Array<Node<*>?>? = null
    @JvmField var dataArrayBuffer: ArrayBufferGL? = null
    @JvmField var vertexBuffer: OpenGlArrayDrawBuffer? = null
    @JvmField var colorBuffer: OpenGlArrayDrawBuffer? = null
    @JvmField var textureBuffer: OpenGlArrayDrawBuffer? = null
    @JvmField var normalsBuffer: OpenGlArrayDrawBuffer? = null
    @JvmField var amountOfTexturedFaces = 0
    @JvmField var totalMaterializedTiles = 0
    @JvmField var maxColorCount = 0
    @JvmField var currentColorCount = 0
    @JvmField val activeLights = LinkedList()
    @JvmField val activeToolkit: OpenGlToolkit
    @JvmField val quarterTileScale: Int
    @JvmField val quarterTileUnits: Int
    @JvmField val floorFunctionMask: Int
    @JvmField val waterFunctionMask: Int
    @JvmField val materialIndices: Array<ShortArray?>
    @JvmField val shadowFunctionMasks: Array<ByteArray>

    init {
        activeToolkit = toolkit
        this.floorFunctionMask = floorFunctionMask
        this.waterFunctionMask = waterFunctionMask
        quarterTileScale = tileScale - 2
        quarterTileUnits = 1 shl quarterTileScale
        anIntArrayArrayArray8649 = Array(regionSizeX) { Array(regionSizeY) { null } }
        materials = Array(regionSizeX) { Array(regionSizeY) { null } }
        tileOffsetsX = Array(regionSizeX) { Array(regionSizeY) { null } }
        tileOffsetsZ = Array(regionSizeX) { Array(regionSizeY) { null } }
        tileColors = Array(regionSizeX) { Array(regionSizeY) { null } }
        tileBlendColors = Array(regionSizeX) { Array(regionSizeY) { null } }
        materialIndices = Array(regionSizeY * regionSizeX) { null }
        shadowFunctionMasks = Array(regionSizeX) { ByteArray(regionSizeY) }
        shadowing = Array(regionSizeX + 1) { ByteArray(regionSizeY + 1) }
        lightingX = Array(width + 1) { FloatArray(length + 1) }
        lightingY = Array(width + 1) { FloatArray(length + 1) }
        lightingZ = Array(width + 1) { FloatArray(length + 1) }
        for (tileY in 1 until length) {
            for (tileX in 1 until width) {
                val heightDeltaHorizontal = heightsNextLvl[tileX + 1][tileY] - heightsNextLvl[tileX - 1][tileY]
                val hiehgtDeltaVertical = heightsNextLvl[tileX][tileY + 1] - heightsNextLvl[tileX][tileY - 1]
                val intensity = (1.0 / Math.sqrt((512 * 512 * 4 + heightDeltaHorizontal * heightDeltaHorizontal + hiehgtDeltaVertical * hiehgtDeltaVertical).toDouble())).toFloat()
                lightingX!![tileX][tileY] = heightDeltaHorizontal * intensity
                lightingY!![tileX][tileY] = (-512 * 2) * intensity
                lightingZ!![tileX][tileY] = hiehgtDeltaVertical * intensity
            }
        }
        materialTable = HashTable(128)
        if ((waterFunctionMask and GroundFunctionMask.HD_SHADOWS) != 0) {
            shadowMapper = ShadowMapperGL(activeToolkit, this)
        }
    }

    fun method13879(shadowGl: ShadowGL, offsetsXCount: Int, offsetsYCount: Int) {
        val offsetsX = tileOffsetsX!![offsetsXCount][offsetsYCount]!!
        val offsetsZ = tileOffsetsZ!![offsetsXCount][offsetsYCount]!!
        val totalOffsets = offsetsX.size
        if (activeToolkit.projectedVerticesX.size < totalOffsets) {
            activeToolkit.projectedVerticesX = IntArray(totalOffsets)
            activeToolkit.projectedVerticesY = IntArray(totalOffsets)
        }
        val sceneShadowOffX = activeToolkit.projectedVerticesX
        val sceneShadowOffZ = activeToolkit.projectedVerticesY
        for (index in 0 until totalOffsets) {
            sceneShadowOffX[index] = offsetsX[index] shr activeToolkit.shadowScale
            sceneShadowOffZ[index] = offsetsZ[index] shr activeToolkit.shadowScale
        }
        var count = 0
        while (count < totalOffsets) {
            val xA = sceneShadowOffX[count]
            val zA = sceneShadowOffZ[count++]
            val xB = sceneShadowOffX[count]
            val zB = sceneShadowOffZ[count++]
            val xC = sceneShadowOffX[count]
            val zC = sceneShadowOffZ[count++]
            if ((xA - xB) * (zB - zC) - (zB - zA) * (xC - xB) > 0) {
                shadowGl.markShadedTriangle(zA, zB, zC, xA, xB, xC)
            }
        }
    }

    override fun setTileShadow(x: Int, y: Int, shadow: Int) {
        if ((shadowing!![x][y].toInt() and 0xff) < shadow) {
            shadowing!![x][y] = shadow.toByte()
        }
    }

    override fun calculateBlockTiles(
        i_1: Int,
        i_2: Int,
        ints_3: IntArray,
        ints_4: IntArray,
        ints_5: IntArray,
        ints_6: IntArray,
        ints_7: IntArray,
        ints_8: IntArray,
        ints_9: IntArray,
        ints_10: IntArray,
        ints_11: IntArray,
        ints_12: IntArray,
        ints_13: IntArray,
        class9014: WaterTile?
    ) {
        val i_16 = ints_10.size
        val ints_17 = IntArray(i_16 * 3)
        val ints_18 = IntArray(i_16 * 3)
        val ints_19 = IntArray(i_16 * 3)
        val ints_20 = IntArray(i_16 * 3)
        val ints_21 = IntArray(i_16 * 3)
        val ints_22 = IntArray(i_16 * 3)
        val ints_23 = IntArray(i_16 * 3)
        val ints_24 = IntArray(i_16 * 3)
        var i_25 = 0

        for (i_26 in 0 until i_16) {
            val i_27 = ints_7[i_26]
            val i_28 = ints_8[i_26]
            val i_29 = ints_9[i_26]
            ints_17[i_25] = ints_3[i_27]
            ints_18[i_25] = ints_5[i_27]
            ints_19[i_25] = ints_10[i_26]
            ints_21[i_25] = ints_12[i_26]
            ints_22[i_25] = ints_13[i_26]
            ints_20[i_25] = ints_11[i_26]
            ints_23[i_25] = ints_4[i_27]
            ints_24[i_25] = ints_6[i_27]

            ++i_25
            ints_17[i_25] = ints_3[i_28]
            ints_18[i_25] = ints_5[i_28]
            ints_19[i_25] = ints_10[i_26]
            ints_21[i_25] = ints_12[i_26]
            ints_22[i_25] = ints_13[i_26]
            ints_20[i_25] = ints_11[i_26]
            ints_23[i_25] = ints_4[i_28]
            ints_24[i_25] = ints_6[i_28]

            ++i_25
            ints_17[i_25] = ints_3[i_29]
            ints_18[i_25] = ints_5[i_29]
            ints_19[i_25] = ints_10[i_26]
            ints_21[i_25] = ints_12[i_26]
            ints_22[i_25] = ints_13[i_26]
            ints_20[i_25] = ints_11[i_26]
            ints_23[i_25] = ints_4[i_29]
            ints_24[i_25] = ints_6[i_29]

            ++i_25
        }

        addBlendedTile(i_1, i_2, ints_17, ints_23, ints_18, ints_24, ints_19, ints_20, ints_21, ints_22, class9014, false)
    }

    fun method13881(i_1: Int, i_2: Int, i_3: Int, bools_4: Array<BooleanArray>, bool5: Boolean) {
        if (goundMaterialNodes != null) {
            var i_7 = i_3 + i_3 + 1
            i_7 *= i_7
            if (activeToolkit.anIntArray8502.size < i_7) {
                activeToolkit.anIntArray8502 = IntArray(i_7)
            }

            if (activeToolkit.textureBuffer.buffer.size < amountOfTexturedFaces * 2) {
                activeToolkit.textureBuffer = GlBitBuffer(amountOfTexturedFaces * 2)
            }

            var i_8 = i_1 - i_3
            val i_9 = i_8
            if (i_8 < 0) {
                i_8 = 0
            }

            var i_10 = i_2 - i_3
            val i_11 = i_10
            if (i_10 < 0) {
                i_10 = 0
            }

            var i_12 = i_3 + i_1
            if (i_12 > width - 1) {
                i_12 = width - 1
            }

            var i_13 = i_3 + i_2
            if (i_13 > length - 1) {
                i_13 = length - 1
            }

            var i_14 = 0
            val ints_15 = activeToolkit.anIntArray8502

            for (i_16 in i_8..i_12) {
                val bools_20 = bools_4[i_16 - i_9]

                for (i_21 in i_10..i_13) {
                    if (bools_20[i_21 - i_11]) {
                        ints_15[i_14++] = i_16 + i_21 * width
                    }
                }
            }

            activeToolkit.setupMatrixRendering()
            activeToolkit.setLightingEnabled((waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0)

            for (i_16 in goundMaterialNodes!!.indices) {
                (goundMaterialNodes!![i_16] as GroundMaterialGL).method12085(ints_15, i_14)
            }

            if (!activeLights.isEmpty()) {
                val fogColor = activeToolkit.fogColor
                val fogDensity = activeToolkit.fogDensity
                activeToolkit.calculateFog(0, fogDensity, activeToolkit.anInt8378)
                activeToolkit.setLightingEnabled(false)
                activeToolkit.setDepthMask(false)
                activeToolkit.setBlendMode(128)
                activeToolkit.renderTexture(-2)
                activeToolkit.setActiveTexture(activeToolkit.texture2d)
                activeToolkit.loadTextureCombineMode(GL_MODULATE, GL_REPLACE)
                activeToolkit.setTexture(0, GL_CONSTANT, 770)
                activeToolkit.setTextureTransparency(0, 34167)

                @Suppress("UNCHECKED_CAST")
                var light: Node<*>? = activeLights.getTail() as Node<*>?
                while (light != null) {
                    val glLight = light as LightGL
                    glLight.method12311(i_1, i_2, i_3, bools_4)
                    @Suppress("UNCHECKED_CAST")
                    light = activeLights.getPrevious() as Node<*>?
                }

                activeToolkit.setTexture(0, GL_TEXTURE, 768)
                activeToolkit.setTextureTransparency(0, GL_TEXTURE)
                activeToolkit.setActiveTexture(null)
                activeToolkit.calculateFog(fogColor, fogDensity, activeToolkit.anInt8378)
            }

            if (shadowMapper != null) {
                OpenGL.glPushMatrix()
                OpenGL.glTranslatef(0.0f, -1.0f, 0.0f)
                activeToolkit.toggleDrawingClientstates(vertexBuffer, null, null, textureBuffer)
                shadowMapper!!.method2290(i_1, i_2, i_3, bools_4, bool5)
                OpenGL.glPopMatrix()
            }
        }
    }

    override fun resetLight() {
        if (currentColorCount > 0) {
            val intensity = Array(width + 1) { ByteArray(length + 1) }
            for (x in 1 until width) {
                for (y in 1 until length) {
                    intensity[x][y] = (
                        (shadowing!![x][y - 1].toInt() shr 2) +    // south of center
                            (shadowing!![x][y].toInt() shr 1) +    // center
                            (shadowing!![x - 1][y].toInt() shr 2) + // west of center
                            (shadowing!![x][y + 1].toInt() shr 3) + // north of center
                            (shadowing!![x + 1][y].toInt() shr 3)   // east of center
                        ).toByte()
                }
            }
            goundMaterialNodes = arrayOfNulls<Node<*>>(materialTable!!.nodeCount())
            materialTable!!.values(goundMaterialNodes!!)
            for (nodeIndex in goundMaterialNodes!!.indices) {
                (goundMaterialNodes!![nodeIndex] as GroundMaterialGL).generateNativeBuffer(currentColorCount)
            }
            var bufferEntrySize = 24
            if (anIntArrayArrayArray8662 != null) {
                bufferEntrySize += 4
            }
            if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0) {
                bufferEntrySize += 12
            }
            val nativeHeapBuffer = activeToolkit.nativeHeap.createBuffer(bufferEntrySize * currentColorCount, false)
            val nativeStream = Stream(nativeHeapBuffer)
            val materialsByVertice = Array<GroundMaterialGL?>(currentColorCount) { null }
            var furthestBitValue = MathUtils.furtestBitValue(currentColorCount / 4)
            if (furthestBitValue < 1) {
                furthestBitValue = 1
            }

            val hashTable = HashTable(furthestBitValue)
            val someMaterialArray = Array<GroundMaterialGL?>(maxColorCount) { null }
            for (x in 0 until width) {
                for (y in 0 until length) {
                    if (tileColors!![x][y] != null) {
                        val arr_65 = materials!![x][y]!!
                        val offsetsX = tileOffsetsX!![x][y]!!
                        val offsetsZ = tileOffsetsZ!![x][y]!!
                        val mainColors = tileColors!![x][y]!!
                        var blendingColors = tileBlendColors!![x][y]
                        if (blendingColors == null) {
                            blendingColors = mainColors
                        }
                        val floorOffsets = if (anIntArrayArrayArray8649 != null) anIntArrayArrayArray8649!![x][y] else null
                        val ints_17 = if (anIntArrayArrayArray8662 != null) anIntArrayArrayArray8662!![x][y] else null

                        val lightingXSouthWest = lightingX!![x][y]
                        val lightingYSouthWest = lightingY!![x][y]
                        val lightingZSouthWest = lightingZ!![x][y]
                        val lightingXNorthWest = lightingX!![x][y + 1]
                        val lightingYNorthWest = lightingY!![x][y + 1]
                        val lightingZNorthWest = lightingZ!![x][y + 1]
                        val lightingXNorthEast = lightingX!![x + 1][y + 1]
                        val lightingYNorthEast = lightingY!![x + 1][y + 1]
                        val lightingZNorthEast = lightingZ!![x + 1][y + 1]
                        val lightingXSouthEast = lightingX!![x + 1][y]
                        val lightingYSouthEast = lightingY!![x + 1][y]
                        val lightingZSouthEast = lightingZ!![x + 1][y]
                        val lightStrengthSouthWest = intensity[x][y].toInt() and 0xff
                        val lightStrengthNorthWest = intensity[x][y + 1].toInt() and 0xff
                        val lightStrengthNorthEast = intensity[x + 1][y + 1].toInt() and 0xff
                        val lightStrengthSouthEast = intensity[x + 1][y].toInt() and 0xff
                        var i_34 = 0

                        loop@ for (colorIndex in mainColors.indices) {
                            val currentMaterialNode = arr_65[colorIndex]
                            for (index in 0 until i_34) {
                                if (currentMaterialNode === someMaterialArray[index]) {
                                    continue@loop
                                }
                            }
                            someMaterialArray[i_34++] = currentMaterialNode
                        }

                        val shorts_74 = ShortArray(mainColors.size)
                        @Suppress("UNCHECKED_CAST")
                        (materialIndices as Array<ShortArray?>)[x + y * width] = shorts_74

                        for (loop in mainColors.indices) {
                            val sceneX = (x shl tileScale) + offsetsX[loop]
                            val sceneY = (y shl tileScale) + offsetsZ[loop]
                            val downscaledX = sceneX shr quarterTileScale
                            val downscaledY = sceneY shr quarterTileScale
                            val mainColor = mainColors[loop]
                            val blendColor = blendingColors[loop]
                            val floorHeightOffset = if (floorOffsets != null) floorOffsets[loop] else 0
                            val uid = (blendColor.toLong() shl 48) or ((mainColor.toLong() shl 32)) or ((downscaledX.toLong() shl 16)) or downscaledY.toLong()
                            val offsetX = offsetsX[loop]
                            val offsetZ = offsetsZ[loop]
                            val baseStrength = 74
                            var directColor = 0
                            var f_50 = 1.0f
                            var lightX: Float
                            var lightY: Float
                            var lightZ: Float
                            var lightStrength: Int
                            if (offsetX == 0 && offsetZ == 0) {
                                lightX = lightingXSouthWest
                                lightY = lightingYSouthWest
                                lightZ = lightingZSouthWest
                                lightStrength = baseStrength - lightStrengthSouthWest
                            } else if (offsetX == 0 && offsetZ == tileUnits) {
                                lightX = lightingXNorthWest
                                lightY = lightingYNorthWest
                                lightZ = lightingZNorthWest
                                lightStrength = baseStrength - lightStrengthNorthWest
                            } else if (offsetX == tileUnits && offsetZ == tileUnits) {
                                lightX = lightingXNorthEast
                                lightY = lightingYNorthEast
                                lightZ = lightingZNorthEast
                                lightStrength = baseStrength - lightStrengthNorthEast
                            } else if (offsetX == tileUnits && offsetZ == 0) {
                                lightX = lightingXSouthEast
                                lightY = lightingYSouthEast
                                lightZ = lightingZSouthEast
                                lightStrength = baseStrength - lightStrengthSouthEast
                            } else {
                                val partualOffsetX = offsetX.toFloat() / tileUnits
                                val partualOffsetZ = offsetZ.toFloat() / tileUnits
                                val lightDeltaXSouth = lightingXSouthWest + (lightingXSouthEast - lightingXSouthWest) * partualOffsetX
                                val lightDeltaYSouth = lightingYSouthWest + (lightingYSouthEast - lightingYSouthWest) * partualOffsetX
                                val lightDeltaZSouth = lightingZSouthWest + (lightingZSouthEast - lightingZSouthWest) * partualOffsetX
                                val lightDeltaXNorth = lightingXNorthWest + (lightingXNorthEast - lightingXNorthWest) * partualOffsetX
                                val lightDeltaYNorth = lightingYNorthWest + (lightingYNorthEast - lightingYNorthWest) * partualOffsetX
                                val lightDeltaZNorth = lightingZNorthWest + (lightingZNorthEast - lightingZNorthWest) * partualOffsetX

                                lightX = lightDeltaXSouth + (lightDeltaXNorth - lightDeltaXSouth) * partualOffsetZ
                                lightY = lightDeltaYSouth + (lightDeltaYNorth - lightDeltaYSouth) * partualOffsetZ
                                lightZ = lightDeltaZSouth + (lightDeltaZNorth - lightDeltaZSouth) * partualOffsetZ
                                val mainLight = lightStrengthSouthWest + (offsetX * (lightStrengthSouthEast - lightStrengthSouthWest) shr tileScale)
                                val adjustmentLight = lightStrengthNorthWest + (offsetX * (lightStrengthNorthEast - lightStrengthNorthWest) shr tileScale)
                                lightStrength = baseStrength - (mainLight + (offsetZ * (adjustmentLight - mainLight) shr tileScale))
                            }

                            if (mainColor != -1) {
                                var lightness = lightStrength * (mainColor and 0x7f) shr 7
                                if (lightness < 2) {
                                    lightness = 2
                                } else if (lightness > 126) {
                                    lightness = 126
                                }

                                directColor = ColorUtil.hslToRgb!![mainColor and 0xff80 or lightness]
                                if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) == 0) {
                                    f_50 = activeToolkit.sunDirection[0] * lightX + activeToolkit.sunDirection[1] * lightY + activeToolkit.sunDirection[2] * lightZ
                                    f_50 = activeToolkit.sunBrightness + f_50 * (if (f_50 > 0.0f) activeToolkit.aFloat8433 else activeToolkit.aFloat8434)
                                }
                            }

                            var blendedTile: Node<*>? = null
                            if ((sceneX and (quarterTileUnits - 1)) == 0 && (sceneY and (quarterTileUnits - 1)) == 0) {
                                blendedTile = hashTable.get(uid)
                            }

                            var i_84: Int
                            var colorRgb: Int
                            if (blendedTile == null) {
                                if (blendColor != mainColor) {
                                    var lightness = lightStrength * (blendColor and 0x7f) shr 7
                                    if (lightness < 2) {
                                        lightness = 2
                                    } else if (lightness > 126) {
                                        lightness = 126
                                    }

                                    colorRgb = ColorUtil.hslToRgb!![blendColor and 0xff80 or lightness]
                                    if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) == 0) {
                                        val f_10000 = activeToolkit.sunDirection[0] * lightX + activeToolkit.sunDirection[1] * lightY + activeToolkit.sunDirection[2] * lightZ
                                        val lightStrengthMultiplier = activeToolkit.sunBrightness + f_50 * (if (f_50 > 0.0f) activeToolkit.aFloat8433 else activeToolkit.aFloat8434)
                                        var red = colorRgb shr 16 and 0xff
                                        var green = colorRgb shr 8 and 0xff
                                        var blue = colorRgb and 0xff
                                        red = (red * lightStrengthMultiplier).toInt()
                                        if (red < 0) {
                                            red = 0
                                        } else if (red > 255) {
                                            red = 255
                                        }
                                        green = (green * lightStrengthMultiplier).toInt()
                                        if (green < 0) {
                                            green = 0
                                        } else if (green > 255) {
                                            green = 255
                                        }
                                        blue = (blue * lightStrengthMultiplier).toInt()
                                        if (blue < 0) {
                                            blue = 0
                                        } else if (blue > 255) {
                                            blue = 255
                                        }
                                        colorRgb = red shl 16 or (green shl 8) or blue
                                    }
                                } else {
                                    colorRgb = directColor
                                }
                                if (activeToolkit.bigEndianess) {
                                    nativeStream.putFloatBigEndian(sceneX.toFloat())
                                    nativeStream.putFloatBigEndian((getAverageHeight(sceneX, sceneY) + floorHeightOffset).toFloat())
                                    nativeStream.putFloatBigEndian(sceneY.toFloat())
                                    nativeStream.putByte(colorRgb shr 16)
                                    nativeStream.putByte(colorRgb shr 8)
                                    nativeStream.putByte(colorRgb)
                                    nativeStream.putByte(-1)
                                    nativeStream.putFloatBigEndian(sceneX.toFloat())
                                    nativeStream.putFloatBigEndian(sceneY.toFloat())
                                    if (anIntArrayArrayArray8662 != null) {
                                        nativeStream.putFloatBigEndian(if (ints_17 != null) (ints_17[loop] - 1).toFloat() else 0.0f)
                                    }
                                    if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0) {
                                        nativeStream.putFloatBigEndian(lightX)
                                        nativeStream.putFloatBigEndian(lightY)
                                        nativeStream.putFloatBigEndian(lightZ)
                                    }
                                } else {
                                    nativeStream.putFloatLittleEndian(sceneX.toFloat())
                                    nativeStream.putFloatLittleEndian((getAverageHeight(sceneX, sceneY) + floorHeightOffset).toFloat())
                                    nativeStream.putFloatLittleEndian(sceneY.toFloat())
                                    nativeStream.putByte(colorRgb shr 16)
                                    nativeStream.putByte(colorRgb shr 8)
                                    nativeStream.putByte(colorRgb)
                                    nativeStream.putByte(-1)
                                    nativeStream.putFloatLittleEndian(sceneX.toFloat())
                                    nativeStream.putFloatLittleEndian(sceneY.toFloat())
                                    if (anIntArrayArrayArray8662 != null) {
                                        nativeStream.putFloatLittleEndian(if (ints_17 != null) (ints_17[loop] - 1).toFloat() else 0.0f)
                                    }
                                    if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0) {
                                        nativeStream.putFloatLittleEndian(lightX)
                                        nativeStream.putFloatLittleEndian(lightY)
                                        nativeStream.putFloatLittleEndian(lightZ)
                                    }
                                }
                                i_84 = totalMaterializedTiles++
                                shorts_74[loop] = i_84.toShort()
                                if (mainColor != -1) {
                                    materialsByVertice[i_84] = arr_65[loop]
                                }
                                hashTable.put(ShortNode(shorts_74[loop]), uid)
                            } else {
                                shorts_74[loop] = (blendedTile as ShortNode).aShort8067
                                i_84 = shorts_74[loop].toInt() and 0xffff
                                if (mainColor != -1 && arr_65[loop].pointer < materialsByVertice[i_84]!!.pointer) {
                                    materialsByVertice[i_84] = arr_65[loop]
                                }
                            }

                            for (materialIndex in 0 until i_34) {
                                someMaterialArray[materialIndex]!!.method12083(i_84, directColor, lightStrength, f_50)
                            }

                            amountOfTexturedFaces++
                        }
                    }
                }
            }

            for (i_9 in 0 until totalMaterializedTiles) {
                val class282Sub310 = materialsByVertice[i_9]
                if (class282Sub310 != null) {
                    class282Sub310.appendToNativeStream(i_9)
                }
            }

            for (x in 0 until width) {
                for (y in 0 until length) {
                    val vertices = materialIndices[x + y * width]
                    if (vertices != null) {
                        var faceCount = 0
                        var verticeCount = 0
                        while (verticeCount < vertices.size) {
                            val faceA = vertices[verticeCount++].toInt() and 0xffff
                            val faceB = vertices[verticeCount++].toInt() and 0xffff
                            val faceC = vertices[verticeCount++].toInt() and 0xffff
                            val texturedFaceA = materialsByVertice[faceA]
                            val texturedFaceB = materialsByVertice[faceB]
                            val texturedFaceC = materialsByVertice[faceC]
                            var texturedFaceD: GroundMaterialGL? = null
                            if (texturedFaceA != null) {
                                texturedFaceA.generateFunctionMask(x, y, faceCount)
                                texturedFaceD = texturedFaceA
                            }
                            if (texturedFaceB != null) {
                                texturedFaceB.generateFunctionMask(x, y, faceCount)
                                if (texturedFaceD == null || texturedFaceB.pointer < texturedFaceD.pointer) {
                                    texturedFaceD = texturedFaceB
                                }
                            }
                            if (texturedFaceC != null) {
                                texturedFaceC.generateFunctionMask(x, y, faceCount)
                                if (texturedFaceD == null || texturedFaceC.pointer < texturedFaceD.pointer) {
                                    texturedFaceD = texturedFaceC
                                }
                            }
                            if (texturedFaceD != null) {
                                if (texturedFaceA != null) {
                                    texturedFaceD.appendToNativeStream(faceA)
                                }
                                if (texturedFaceB != null) {
                                    texturedFaceD.appendToNativeStream(faceB)
                                }
                                if (texturedFaceC != null) {
                                    texturedFaceD.appendToNativeStream(faceC)
                                }
                                texturedFaceD.generateFunctionMask(x, y, faceCount)
                            }
                            faceCount++
                        }
                    }
                }
            }
            nativeStream.method2925()
            dataArrayBuffer = activeToolkit.createDataArrayBuffer(bufferEntrySize, nativeHeapBuffer, nativeStream.written())
            if (dataArrayBuffer is GlVboArrayBuffer) {
                nativeHeapBuffer.deallocateBuffer()
            }
            vertexBuffer = OpenGlArrayDrawBuffer(dataArrayBuffer!!, GL_FLOAT, 3, 0)
            colorBuffer = OpenGlArrayDrawBuffer(dataArrayBuffer!!, GL_UNSIGNED_BYTE, 4, 12)
            val pointerOffset: Int
            if (anIntArrayArrayArray8662 != null) {
                textureBuffer = OpenGlArrayDrawBuffer(dataArrayBuffer!!, GL_FLOAT, 3, 16)
                pointerOffset = 28
            } else {
                textureBuffer = OpenGlArrayDrawBuffer(dataArrayBuffer!!, GL_FLOAT, 2, 16)
                pointerOffset = 24
            }

            if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0) {
                normalsBuffer = OpenGlArrayDrawBuffer(dataArrayBuffer!!, GL_FLOAT, 3, pointerOffset)
            }

            val pointers = LongArray(goundMaterialNodes!!.size)

            for (nodeIndex in goundMaterialNodes!!.indices) {
                val materialNode = goundMaterialNodes!![nodeIndex] as GroundMaterialGL
                pointers[nodeIndex] = materialNode.pointer
                materialNode.createDataArrayBuffer(totalMaterializedTiles)
            }

            ArrayUtils.quicksort(pointers, goundMaterialNodes!!)
            if (shadowMapper != null) {
                shadowMapper!!.method2309()
            }
        } else {
            shadowMapper = null
        }

        if ((floorFunctionMask and GroundFunctionMask.HIDE) == 0) {
            tileOffsetsZ = null
            tileOffsetsX = null
            tileColors = null
        }

        anIntArrayArrayArray8662 = null
        tileBlendColors = null
        anIntArrayArrayArray8649 = null
        materials = null
        shadowing = null
        materialTable = null
        lightingZ = null
        lightingY = null
        lightingX = null
    }

    override fun renderMinimapFloor(
        i_1: Int,
        i_2: Int,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        materialized: Array<BooleanArray>
    ) {
        val activeDisplay = activeToolkit.getDisplay()
        if (currentColorCount > 0 && activeDisplay != null) {
            activeToolkit.method13586()
            activeToolkit.toggleFog(false)
            activeToolkit.setLightingEnabled(false)
            activeToolkit.toggleZbuffer(false)
            activeToolkit.setDepthMask(false)
            activeToolkit.setBlendMode(0)
            activeToolkit.renderTexture(-2)
            activeToolkit.setActiveTexture(null)
            GROUND_MATRIX[0] = 1024 / (128.0f * tileUnits * activeDisplay.getWidth())
            GROUND_MATRIX[1] = 0.0f
            GROUND_MATRIX[2] = 0.0f
            GROUND_MATRIX[3] = 0.0f
            GROUND_MATRIX[4] = 0.0f
            GROUND_MATRIX[5] = 1024 / (128.0f * tileUnits * activeDisplay.getHeight())
            GROUND_MATRIX[6] = 0.0f
            GROUND_MATRIX[7] = 0.0f
            GROUND_MATRIX[8] = 0.0f
            GROUND_MATRIX[9] = 0.0f
            GROUND_MATRIX[10] = 0.0f
            GROUND_MATRIX[11] = 0.0f
            GROUND_MATRIX[12] = -1.0f - ((1024 * startX) / 128.0f - (i_1 * 2)) / activeDisplay.getWidth()
            GROUND_MATRIX[13] = 1.0f - ((i_2 * 2) + (1024 * endY) / 128.0f) / activeDisplay.getHeight()
            GROUND_MATRIX[14] = 0.0f
            GROUND_MATRIX[15] = 1.0f
            OpenGL.glMatrixMode(5889)
            OpenGL.glLoadMatrixf(GROUND_MATRIX, 0)
            GROUND_MATRIX[0] = 1.0f
            GROUND_MATRIX[1] = 0.0f
            GROUND_MATRIX[2] = 0.0f
            GROUND_MATRIX[3] = 0.0f
            GROUND_MATRIX[4] = 0.0f
            GROUND_MATRIX[5] = 0.0f
            GROUND_MATRIX[6] = 1.0f
            GROUND_MATRIX[7] = 0.0f
            GROUND_MATRIX[8] = 0.0f
            GROUND_MATRIX[9] = 1.0f
            GROUND_MATRIX[10] = 0.0f
            GROUND_MATRIX[11] = 0.0f
            GROUND_MATRIX[12] = 0.0f
            GROUND_MATRIX[13] = 0.0f
            GROUND_MATRIX[14] = 0.0f
            GROUND_MATRIX[15] = 1.0f
            OpenGL.glMatrixMode(5888)
            OpenGL.glLoadMatrixf(GROUND_MATRIX, 0)
            if ((waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0) {
                activeToolkit.setLightingEnabled(true)
                activeToolkit.setLightingSourceParams()
            } else {
                activeToolkit.setLightingEnabled(false)
            }

            activeToolkit.toggleDrawingClientstates(vertexBuffer, normalsBuffer, colorBuffer, textureBuffer)
            if (activeToolkit.textureBuffer.buffer.size < amountOfTexturedFaces * 2) {
                activeToolkit.textureBuffer = GlBitBuffer(amountOfTexturedFaces * 2)
            } else {
                activeToolkit.textureBuffer.currentPosition = 0
            }

            val textureBuffer = activeToolkit.textureBuffer
            var texturesLoaded = 0
            if (activeToolkit.bigEndianess) {
                for (y in startY until endY) {
                    var mi = y * width + startX
                    for (x in startX until endX) {
                        if (materialized[x - startX][y - startY]) {
                            val materials = this.materialIndices[mi]
                            if (materials != null) {
                                for (index in materials.indices) {
                                    textureBuffer.writeShort(materials[index].toInt() and 0xffff)
                                    texturesLoaded++
                                }
                            }
                        }
                        mi++
                    }
                }
            } else {
                for (y in startY until endY) {
                    var mi = y * width + startX
                    for (x in startX until endX) {
                        if (materialized[x - startX][y - startY]) {
                            val materials = this.materialIndices[mi]
                            if (materials != null) {
                                for (index in materials.indices) {
                                    textureBuffer.writeShortLittle(materials[index].toInt() and 0xffff)
                                    texturesLoaded++
                                }
                            }
                        }
                        mi++
                    }
                }
            }
            if (texturesLoaded > 0) {
                val texture2d = GlElementsArrayBuffer(activeToolkit, textureBuffer.buffer, textureBuffer.currentPosition)
                activeToolkit.drawElements(texture2d, 0, texturesLoaded)
            }
        }
    }

    override fun addShadow(class282Sub50Sub171: Shadow?, i_2: Int, i_3: Int, i_4: Int) {
        if (shadowMapper != null && class282Sub50Sub171 != null) {
            val i_7 = i_2 - (i_3 * activeToolkit.anInt8438 shr 8) shr activeToolkit.shadowScale
            val i_8 = i_4 - (i_3 * activeToolkit.anInt8439 shr 8) shr activeToolkit.shadowScale
            shadowMapper!!.method2314(class282Sub50Sub171, i_7, i_8)
        }
    }

    override fun hasRenderingShadows(shadow: Shadow?, i_2: Int, i_3: Int, i_4: Int): Boolean {
        if (shadowMapper != null && shadow != null) {
            val i_7 = i_2 - (i_3 * activeToolkit.anInt8438 shr 8) shr activeToolkit.shadowScale
            val i_8 = i_4 - (i_3 * activeToolkit.anInt8439 shr 8) shr activeToolkit.shadowScale
            return shadowMapper!!.method2293(shadow, i_7, i_8)
        }
        return false
    }

    override fun removeShadow(shadow: Shadow?, i_2: Int, i_3: Int, i_4: Int) {
        if (shadowMapper != null && shadow != null) {
            val i_7 = i_2 - (i_3 * activeToolkit.anInt8438 shr 8) shr activeToolkit.shadowScale
            val i_8 = i_4 - (i_3 * activeToolkit.anInt8439 shr 8) shr activeToolkit.shadowScale
            shadowMapper!!.addShadow(shadow, i_7, i_8)
        }
    }

    override fun addLight(lightNode: PointLight, ints_2: IntArray) {
        activeLights.insertTail(LightGL(activeToolkit, this, lightNode, ints_2))
    }

    override fun addBlendedTile(
        tileX: Int,
        tileY: Int,
        offsetsX: IntArray?,
        lightGroundOffsetsA: IntArray?,
        offsetsZ: IntArray?,
        lightGroundOffsetsB: IntArray?,
        mainColors: IntArray,
        blendColors: IntArray?,
        waterMaterials: IntArray,
        waterScaling: IntArray,
        waterTile: WaterTile?,
        hasShadows: Boolean
    ) {
        val materialProvider = activeToolkit.materialProvider
        if (lightGroundOffsetsB != null && anIntArrayArrayArray8662 == null) {
            anIntArrayArrayArray8662 = Array(width) { Array(length) { null } }
        }
        if (lightGroundOffsetsA != null && anIntArrayArrayArray8649 == null) {
            anIntArrayArrayArray8649 = Array(width) { Array(length) { null } }
        }
        tileOffsetsX!![tileX][tileY] = offsetsX
        tileOffsetsZ!![tileX][tileY] = offsetsZ
        tileColors!![tileX][tileY] = mainColors
        tileBlendColors!![tileX][tileY] = blendColors
        if (lightGroundOffsetsB != null && anIntArrayArrayArray8662 == null) {
            // Already handled above
        }
        if (lightGroundOffsetsA != null && anIntArrayArrayArray8649 == null) {
            // Already handled below
        }
        if (anIntArrayArrayArray8662 != null) {
            anIntArrayArrayArray8662!![tileX][tileY] = lightGroundOffsetsB
        }
        if (anIntArrayArrayArray8649 != null) {
            anIntArrayArrayArray8649!![tileX][tileY] = lightGroundOffsetsA
        }
        val groundMaterial = Array<GroundMaterialGL?>(mainColors.size) { null }
        materials!![tileX][tileY] = @Suppress("UNCHECKED_CAST") (groundMaterial as Array<GroundMaterialGL>)
        for (index in mainColors.indices) {
            var materialId = waterMaterials[index]
            var scaling = waterScaling[index]
            if ((waterFunctionMask and 0x20) != 0 && materialId != -1 && activeToolkit.materialProvider?.getMaterial(materialId)?.detailsOnly == true) {
                scaling = 128
                materialId = -1
            }
            if (waterTile != null) {
                val uid = (waterTile.intensity.toLong() shl 48) or ((waterTile.scale.toLong() shl 42)) or ((waterTile.color.toLong() shl 28)) or (((scaling.toLong()) shl 14)) or (materialId.toLong())
                @Suppress("UNCHECKED_CAST")
                var activeGroundMaterial: Node<*>? = materialTable!!.get(uid) as Node<*>?
                while (activeGroundMaterial != null) {
                    val openglGroundMaterial = activeGroundMaterial as GroundMaterialGL
                    if (materialId == openglGroundMaterial.materialId && openglGroundMaterial.textureScale.toInt() == scaling && openglGroundMaterial.waterTile.equals(waterTile)) {
                        break
                    }
                    @Suppress("UNCHECKED_CAST")
                    activeGroundMaterial = materialTable!!.nextInBucket() as Node<*>?
                }
                if (activeGroundMaterial == null) {
                    groundMaterial[index] = GroundMaterialGL(this, materialId, scaling, waterTile)
                    materialTable!!.put(groundMaterial[index], uid)
                } else {
                    groundMaterial[index] = activeGroundMaterial as GroundMaterialGL
                }
            }
        }
        if (hasShadows) {
            shadowFunctionMasks[tileX][tileY] = (shadowFunctionMasks[tileX][tileY].toInt() or GroundFunctionMask.CONTAINS_SHADOW).toByte()
        }
        if (mainColors.size > maxColorCount) {
            maxColorCount = mainColors.size
        }
        currentColorCount += mainColors.size
    }

    override fun renderFloor(i_1: Int, i_2: Int, i_3: Int, bools_4: Array<BooleanArray>, bool5: Boolean) {
        method13881(i_1, i_2, i_3, bools_4, bool5)
    }

    override fun createShadowAt(tileX: Int, tileY: Int, shadow: Shadow?): Shadow? {
        if ((shadowFunctionMasks[tileX][tileY].toInt() and GroundFunctionMask.CONTAINS_SHADOW) == 0) {
            return shadow
        }
        val dimension = tileUnits shr activeToolkit.shadowScale
        val openglShadow = shadow as? ShadowGL
        val generatedOpenglShadow: ShadowGL
        if (openglShadow != null && openglShadow.fitsDimension(dimension, dimension)) {
            generatedOpenglShadow = openglShadow
            openglShadow.reset()
        } else {
            generatedOpenglShadow = ShadowGL(dimension, dimension)
        }

        generatedOpenglShadow.setDimension(0, 0, dimension, dimension)
        method13879(generatedOpenglShadow, tileX, tileY)
        return generatedOpenglShadow
    }
}
