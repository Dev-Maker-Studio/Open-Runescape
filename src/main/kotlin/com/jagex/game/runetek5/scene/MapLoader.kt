package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.flo.FloType
import com.jagex.game.runetek5.config.flo.FloTypeList
import com.jagex.game.runetek5.config.flu.FluType
import com.jagex.game.runetek5.config.flu.FluTypeList
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.entity.gameobject.GameObject
import com.jagex.game.runetek5.scene.environment.EnvironmentManager
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.scene.tile.TileFunctionMask
import com.jagex.game.runetek5.scene.tile.WaterTile
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.ColorUtil

open class MapLoader(
    val scene: Scene,
@JvmField val regionSizeZ: Int,
    val regionSizeX: Int,
    val regionSizeY: Int,
@JvmField val overlayHidden: Boolean,
    val overlayTypeList: FloTypeList,
    val underlayTypeList: FluTypeList,
    val regionMap: SettingsBits
) {
@JvmField var lighting: Array<Array<ByteArray>?>? = null
@JvmField var hdShadows: Int = 0
@JvmField var hdWater: Boolean = false
@JvmField var hdLight: Boolean = false
@JvmField var blendFloor: Boolean = false
@JvmField var enableTextures: Boolean = false
@JvmField val tileHeights: Array<Array<IntArray>>
    var lowDetailShadows: Boolean = false
    var cullingsMap: Array<Array<ByteArray>>?
    var underlayHue: IntArray? = null
    var underlaySaturation: IntArray? = null
    var underlayLightness: IntArray? = null
    var underlayDivider: IntArray? = null
    var underlayCount: IntArray? = null
    var underlayFaceCount: Int = 0
    var overlayFaceCount: Int = 0
    var tileRotation: Int = 0
    var overlayTextureId: Int = 0
    var pathShape: Int = 0
    var hasShadows: Boolean = false
    var overlaySupportsBlending: Boolean = false
    var unblendable: Boolean = false
    var overlayTotalFaces: Int = 0
    var index: Int = 0
    var anIntArray3857: IntArray? = null
    var anIntArray3858: IntArray? = null
    var anIntArray3859: IntArray? = null
    var overlayFaceCounts: IntArray? = null
    var overlayHslColor: Int = 0
    var overlayTextureScale: Int = 0
    val overlayTileRotations = IntArray(6)
    val overlayTileColors = IntArray(13)
    val overlayTileRenderedColors = IntArray(13)
    val waterTextureIds = IntArray(13)
    val waterTextureScaling = IntArray(13)
    val waterSlots = IntArray(13)
    val overlayFunctionMask = IntArray(13)
    val underlayIds: Array<Array<ByteArray>>
    val overlayIds: Array<Array<ByteArray>>
    val overlayPathShapes: Array<Array<ByteArray>>
    val overlayRotations: Array<Array<ByteArray>>
    var overlayHasMultiColor: Boolean = false

    init {
        underlayIds = Array(regionSizeZ) { Array(regionSizeX) { ByteArray(regionSizeY) } }
        overlayIds = Array(regionSizeZ) { Array(regionSizeX) { ByteArray(regionSizeY) } }
        overlayPathShapes = Array(regionSizeZ) { Array(regionSizeX) { ByteArray(regionSizeY) } }
        overlayRotations = Array(regionSizeZ) { Array(regionSizeX) { ByteArray(regionSizeY) } }
        tileHeights = Array(regionSizeZ) { Array(regionSizeX + 1) { IntArray(regionSizeY + 1) } }
        cullingsMap = Array(regionSizeZ) { Array(regionSizeX + 1) { ByteArray(regionSizeY + 1) } }
    }

    fun clearAllPlanes(x: Int, y: Int) {
        for (z in 0 until regionSizeZ) {
            clearHeights(z, x, y, 64, 64)
        }
    }

    fun resetUnderlays() {
        underlayHue = null
        underlaySaturation = null
        underlayLightness = null
        underlayDivider = null
        underlayCount = null
        lowDetailShadows = false
    }

    fun enableLdShadows() {
        lowDetailShadows = true
    }

    fun calculateUnderlayPalette(toolkit: AbstractToolkit, heightsCurrentLvl: Ground?, heightsNextLvl: Ground?) {
        val paletteIndices = Array(regionSizeX) { IntArray(regionSizeY) }
        if (underlayHue == null || underlayHue!!.size != regionSizeY) {
            underlayHue = IntArray(regionSizeY)
            underlaySaturation = IntArray(regionSizeY)
            underlayLightness = IntArray(regionSizeY)
            underlayDivider = IntArray(regionSizeY)
            underlayCount = IntArray(regionSizeY)
        }
        for (z in 0 until regionSizeZ) {
            for (index in 0 until regionSizeY) {
                underlayHue!![index] = 0
                underlaySaturation!![index] = 0
                underlayLightness!![index] = 0
                underlayDivider!![index] = 0
                underlayCount!![index] = 0
            }
            for (x in -5 until regionSizeX) {
                for (y in 0 until regionSizeY) {
                    val forwardX = x + 5
                    if (forwardX < regionSizeX) {
                        val underlayId = underlayIds[z][forwardX][y].toInt() and 0xff
                        if (underlayId > 0) {
                            val underlay = underlayTypeList.list(underlayId - 1)
                            underlayHue!![y] += underlay.hue
                            underlaySaturation!![y] += underlay.saturation
                            underlayLightness!![y] += underlay.lightness
                            underlayDivider!![y] += underlay.divisor
                            underlayCount!![y]++
                        }
                    }
                    val backwardX = x - 5
                    if (backwardX >= 0) {
                        val underlayId = underlayIds[z][backwardX][y].toInt() and 0xff
                        if (underlayId > 0) {
                            val underlay = underlayTypeList.list(underlayId - 1)
                            underlayHue!![y] -= underlay.hue
                            underlaySaturation!![y] -= underlay.saturation
                            underlayLightness!![y] -= underlay.lightness
                            underlayDivider!![y] -= underlay.divisor
                            underlayCount!![y]--
                        }
                    }
                }
                if (x >= 0) {
                    var hueSum = 0
                    var saturationSum = 0
                    var lightnessSum = 0
                    var dividerSum = 0
                    var depth = 0
                    for (y in -5 until regionSizeY) {
                        val forwardY = y + 5
                        if (forwardY < regionSizeY) {
                            hueSum += underlayHue!![forwardY]
                            saturationSum += underlaySaturation!![forwardY]
                            lightnessSum += underlayLightness!![forwardY]
                            dividerSum += underlayDivider!![forwardY]
                            depth += underlayCount!![forwardY]
                        }
                        val backwardY = y - 5
                        if (backwardY >= 0) {
                            hueSum -= underlayHue!![backwardY]
                            saturationSum -= underlaySaturation!![backwardY]
                            lightnessSum -= underlayLightness!![backwardY]
                            dividerSum -= underlayDivider!![backwardY]
                            depth -= underlayCount!![backwardY]
                        }
                        if (y >= 0 && dividerSum > 0 && depth > 0) {
                            paletteIndices[x][y] = ColorUtil.hsl16to24(hueSum * 256 / dividerSum, saturationSum / depth, lightnessSum / depth)
                        }
                    }
                }
            }
            if (blendFloor) {
                addBlendedTiles(toolkit, scene.localHeightmap[z], z, paletteIndices, if (z == 0) heightsCurrentLvl else null, if (z == 0) heightsNextLvl else null)
            } else {
                addRegularTile(scene.localHeightmap[z], z, paletteIndices, if (z == 0) heightsCurrentLvl else null, if (z == 0) heightsNextLvl else null)
            }
            underlayIds[z] = emptyArray()
            overlayIds[z] = emptyArray()
            overlayPathShapes[z] = emptyArray()
            overlayRotations[z] = emptyArray()
        }
        if (!overlayHidden) {
            if (hdShadows != 0) {
                scene.addHdShadow()
            }
            if (hdLight) {
                scene.addHdLight()
            }
        }
        for (z in 0 until regionSizeZ) {
            scene.localHeightmap[z].resetLight()
        }
    }

    fun clearHeights(z: Int, x: Int, y: Int, offsetX: Int, offsetY: Int) {
        for (nextY in y until y + offsetY) {
            for (nextX in x until x + offsetX) {
                if (nextX >= 0 && nextX < regionSizeX && nextY >= 0 && nextY < regionSizeY) {
                    tileHeights[z][nextX][nextY] = if (z > 0) tileHeights[z - 1][nextX][nextY] - 960 else 0
                }
            }
        }
        if (x > 0 && x < regionSizeX) {
            for (nextY in y + 1 until y + offsetY) {
                if (nextY >= 0 && nextY < regionSizeY) {
                    tileHeights[z][x][nextY] = tileHeights[z][x - 1][nextY]
                }
            }
        }
        if (y > 0 && y < regionSizeY) {
            for (nextY in x + 1 until x + offsetX) {
                if (nextY >= 0 && nextY < regionSizeX) {
                    tileHeights[z][nextY][y] = tileHeights[z][nextY][y - 1]
                }
            }
        }
        if (x >= 0 && y >= 0 && x < regionSizeX && y < regionSizeY) {
            if (z == 0) {
                if (x > 0 && tileHeights[0][x - 1][y] != 0) {
                    tileHeights[z][x][y] = tileHeights[z][x - 1][y]
                } else if (y > 0 && tileHeights[z][x][y - 1] != 0) {
                    tileHeights[z][x][y] = tileHeights[z][x][y - 1]
                } else if (x > 0 && y > 0 && tileHeights[z][x - 1][y - 1] != 0) {
                    tileHeights[z][x][y] = tileHeights[z][x - 1][y - 1]
                }
            } else if (x > 0 && tileHeights[z - 1][x - 1][y] != tileHeights[z][x - 1][y]) {
                tileHeights[z][x][y] = tileHeights[z][x - 1][y]
            } else if (y > 0 && tileHeights[z][x][y - 1] != tileHeights[z - 1][x][y - 1]) {
                tileHeights[z][x][y] = tileHeights[z][x][y - 1]
            } else if (x > 0 && y > 0 && tileHeights[z - 1][x - 1][y - 1] != tileHeights[z][x - 1][y - 1]) {
                tileHeights[z][x][y] = tileHeights[z][x - 1][y - 1]
            }
        }
    }

    fun decodeTilesClient(stream: Packet, chunkX: Int, chunkY: Int, baseX: Int, baseY: Int, clippings: Array<CollisionMap>) {
        if (!overlayHidden) {
            for (level in 0..3) {
                val clipping = clippings[level]
                for (regionPositionX in 0..63) {
                    for (regionPositionY in 0..63) {
                        val sceneX = regionPositionX + chunkX
                        val sceneY = regionPositionY + chunkY
                        if (sceneX >= 0 && sceneX < regionSizeX && sceneY >= 0 && sceneY < regionSizeY) {
                            clipping.unblock(sceneX, sceneY)
                        }
                    }
                }
            }
        }
        val absoluteX = chunkX + baseX
        val absoluteY = chunkY + baseY
        for (level in 0 until regionSizeZ) {
            for (regionPositionX in 0..63) {
                for (regionPositionY in 0..63) {
                    decodeTile(stream, level, chunkX + regionPositionX, chunkY + regionPositionY, 0, 0, absoluteX + regionPositionX, absoluteY + regionPositionY, 0, false)
                }
            }
        }
    }

    fun decodeTilesServer(rsbytebuffer1: Packet, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, arr_9: Array<CollisionMap>) {
        val i_11 = (i_6 and 0x7) * 8
        val i_12 = (i_7 and 0x7) * 8
        var i_14: Int
        var i_17: Int
        if (!overlayHidden) {
            val clipmap13 = arr_9[i_2]
            i_14 = 0
            while (i_14 < 8) {
                for (i_15 in 0..7) {
                    val i_16 = i_3 + EnvironmentManager.rotateTerrainBlockX(i_14 and 0x7, i_15 and 0x7, i_8)
                    i_17 = i_4 + EnvironmentManager.rotateTerrainBlockY(i_14 and 0x7, i_15 and 0x7, i_8)
                    if (i_16 > 0 && i_16 < regionSizeX - 1 && i_17 > 0 && i_17 < regionSizeY - 1) {
                        clipmap13.unblock(i_16, i_17)
                    }
                }
                i_14++
            }
        }
        val i_28 = (i_6 and -8) shl 3
        i_14 = (i_7 and -8) shl 3
        var b_29: Byte = 0
        var b_30: Byte = 0
        if (i_8 == 1) {
            b_30 = 1
        } else if (i_8 == 2) {
            b_29 = 1
            b_30 = 1
        } else if (i_8 == 3) {
            b_29 = 1
        }
        i_17 = 0
        while (i_17 < regionSizeZ) {
            for (i_18 in 0..63) {
                for (i_19 in 0..63) {
                    if (i_5 == i_17 && i_18 >= i_11 && i_18 <= i_11 + 8 && i_19 >= i_12 && i_19 <= i_12 + 8) {
                        val i_20: Int
                        val i_21: Int
                        if (i_18 != i_11 + 8 && i_12 + 8 != i_19) {
                            i_20 = i_3 + EnvironmentManager.rotateTerrainBlockX(i_18 and 0x7, i_19 and 0x7, i_8)
                            i_21 = i_4 + EnvironmentManager.rotateTerrainBlockY(i_18 and 0x7, i_19 and 0x7, i_8)
                            decodeTile(rsbytebuffer1, i_2, i_20, i_21, b_29.toInt(), b_30.toInt(), i_28 + i_18, i_14 + i_19, i_8, false)
                        } else {
                            if (i_8 == 0) {
                                i_20 = i_3 + (i_18 - i_11)
                                i_21 = i_19 - i_12 + i_4
                            } else if (i_8 == 1) {
                                i_20 = i_3 + (i_19 - i_12)
                                i_21 = i_4 + 8 - (i_18 - i_11)
                            } else if (i_8 == 2) {
                                i_20 = i_3 + 8 - (i_18 - i_11)
                                i_21 = i_4 + 8 - (i_19 - i_12)
                            } else {
                                i_20 = i_3 + 8 - (i_19 - i_12)
                                i_21 = i_18 - i_11 + i_4
                            }
                            decodeTile(rsbytebuffer1, i_2, i_20, i_21, 0, 0, i_28 + i_18, i_14 + i_19, 0, true)
                        }
                        if (i_18 == 63 || i_19 == 63) {
                            val b_22: Byte = if (i_18 == 63 && i_19 == 63) 3 else 1
                            for (i_23 in 0 until b_22) {
                                var i_24 = i_18
                                var i_25 = i_19
                                if (i_23 == 0) {
                                    i_24 = if (i_18 == 63) 64 else i_18
                                    i_25 = if (i_19 == 63) 64 else i_19
                                } else if (i_23 == 1) {
                                    i_24 = 64
                                } else {
                                    i_25 = 64
                                }
                                val i_26: Int
                                val i_27: Int
                                if (i_8 == 0) {
                                    i_26 = i_3 + (i_24 - i_11)
                                    i_27 = i_25 - i_12 + i_4
                                } else if (i_8 == 1) {
                                    i_26 = i_3 + (i_25 - i_12)
                                    i_27 = i_4 + 8 - (i_24 - i_11)
                                } else if (i_8 == 2) {
                                    i_26 = i_3 + 8 - (i_24 - i_11)
                                    i_27 = i_4 + 8 - (i_25 - i_12)
                                } else {
                                    i_26 = i_3 + 8 - (i_25 - i_12)
                                    i_27 = i_24 - i_11 + i_4
                                }
                                if (i_26 >= 0 && i_26 < regionSizeX && i_27 >= 0 && i_27 < regionSizeY) {
                                    tileHeights[i_2][i_26][i_27] = tileHeights[i_2][i_20 + b_29][b_30 + i_21]
                                }
                            }
                        }
                    } else {
                        decodeTile(rsbytebuffer1, 0, -1, -1, 0, 0, 0, 0, 0, false)
                    }
                }
            }
            i_17++
        }
    }

    fun initializeFloor(toolkit: AbstractToolkit, heightsNextLvl: Array<Array<IntArray>>?, clipping: Array<CollisionMap>?) {
        if (!overlayHidden) {
            for (plane in 0..3) {
                for (x in 0 until regionSizeX) {
                    for (y in 0 until regionSizeY) {
                        if ((regionMap.tileFunctionMasks[plane][x][y].toInt() and 0x1) != 0) {
                            var height = plane
                            if ((regionMap.tileFunctionMasks[1][x][y].toInt() and 0x2) != 0) {
                                height = plane - 1
                            }
                            if (height >= 0) {
                                clipping!![height].setUnwalkable(x, y)
                            }
                        }
                    }
                }
            }
        }
        for (height in 0 until regionSizeZ) {
            var floorFunctionMask = 0
            var waterFunctionMask = 0
            if (!overlayHidden) {
                if (hdWater) {
                    waterFunctionMask = waterFunctionMask or TileFunctionMask.HD_WATER
                }
                if (hdLight) {
                    floorFunctionMask = floorFunctionMask or TileFunctionMask.HD_LIGHT
                }
                if (hdShadows != 0) {
                    floorFunctionMask = floorFunctionMask or TileFunctionMask.COMPLETELY_FLAT
                    waterFunctionMask = waterFunctionMask or TileFunctionMask.HD_SHADING
                }
            }
            if (hdLight) {
                waterFunctionMask = waterFunctionMask or TileFunctionMask.HAS_WATER_LIGHTING
            }
            if (!enableTextures) {
                waterFunctionMask = waterFunctionMask or TileFunctionMask.DISABLE_TEXTURES
            }
            val nextHeights = if (heightsNextLvl != null && height < heightsNextLvl.size) heightsNextLvl[height] else tileHeights[height]
            scene.setLocalGround(height, toolkit.createGround(regionSizeX, regionSizeY, tileHeights[height], nextHeights, floorFunctionMask, waterFunctionMask))
        }
    }

    fun addRegularTile(localHeights: Ground, plane: Int, floorColors: Array<IntArray>, currentHeights: Ground?, nextHeights: Ground?) {
        for (x in 0 until regionSizeX) {
            for (y in 0 until regionSizeY) {
                var overlayShape = overlayPathShapes[plane][x][y]
                val overlayRotation = overlayRotations[plane][x][y]
                val overlayId = overlayIds[plane][x][y].toInt() and 0xff
                val underlayId = underlayIds[plane][x][y].toInt() and 0xff
                var overlay: FloType? = if (overlayId != 0) overlayTypeList.list(overlayId - 1) else null
                val underlay: FluType? = if (underlayId != 0) underlayTypeList.list(underlayId - 1) else null
                if (overlayShape.toInt() == 0 && overlay == null) {
                    overlayShape = 12
                }
                val Overlay = overlay
                if (overlay != null && overlay.tileColorHsl == -1 && overlay.minimapColorHsl == -1) {
                    overlay = null
                }
                if (overlay != null || underlay != null) {
                    underlayFaceCount = UNDERLAY_FACE_COUNT[overlayShape.toInt()]
                    overlayFaceCount = OVERLAY_FACE_COUNT[overlayShape.toInt()]
                    val faceCount = (if (underlay != null) underlayFaceCount else 0) + (if (overlay != null) overlayFaceCount else 0)
                    var tileCount = 0
                    tileRotation = 0
                    overlayTextureId = overlay?.texture ?: -1
                    val underlayTetureId = underlay?.texture ?: -1
                    val ints_20 = IntArray(faceCount)
                    val ints_21 = IntArray(faceCount)
                    val ints_22 = IntArray(faceCount)
                    val floorColor = IntArray(faceCount)
                    val overlayTextures = IntArray(faceCount)
                    val textureScale = IntArray(faceCount)
                    val minimapColor = if (overlay != null && overlay.minimapColorHsl != -1) IntArray(faceCount) else null
                    if (overlay != null) {
                        for (index in 0 until overlayFaceCount) {
                            ints_20[tileCount] = anIntArrayArray3824[overlayShape.toInt()][tileRotation]
                            ints_21[tileCount] = anIntArrayArray3860[overlayShape.toInt()][tileRotation]
                            ints_22[tileCount] = anIntArrayArray3815[overlayShape.toInt()][tileRotation]
                            overlayTextures[tileCount] = overlayTextureId
                            textureScale[tileCount] = overlay.textureScale
                            floorColor[tileCount] = overlay.tileColorHsl
                            if (minimapColor != null) {
                                minimapColor[tileCount] = overlay.minimapColorHsl
                            }
                            tileCount++
                            tileRotation++
                        }
                        if (!overlayHidden && plane == 0) {
                            scene.addWater(x, y, overlay.waterColor, overlay.waterFogDepth, overlay.waterIntensity, overlay.opcode20, overlay.unusedOpcode21, overlay.unusedOpcode22)
                        }
                    } else {
                        tileRotation += overlayFaceCount
                    }
                    if (underlay != null) {
                        for (index in 0 until underlayFaceCount) {
                            ints_20[tileCount] = anIntArrayArray3824[overlayShape.toInt()][tileRotation]
                            ints_21[tileCount] = anIntArrayArray3860[overlayShape.toInt()][tileRotation]
                            ints_22[tileCount] = anIntArrayArray3815[overlayShape.toInt()][tileRotation]
                            overlayTextures[tileCount] = underlayTetureId
                            textureScale[tileCount] = underlay.scale
                            floorColor[tileCount] = floorColors[x][y]
                            if (minimapColor != null) {
                                minimapColor[tileCount] = floorColor[tileCount]
                            }
                            tileCount++
                            tileRotation++
                        }
                    }
                    val rotationCount = tileSizeDeltasX.size
                    val sizesX = IntArray(rotationCount)
                    val sizesY = IntArray(rotationCount)
                    val nextLvlHeightDifference = if (nextHeights != null) IntArray(rotationCount) else null
                    val localLvlHeightDifference = if (nextHeights == null && currentHeights == null) null else IntArray(rotationCount)
                    for (rotation in 0 until rotationCount) {
                        val sizeDeltaX = tileSizeDeltasX[rotation]
                        val sizeDeltaY = tileSizeDeltasY[rotation]
                        if (overlayRotation.toInt() == 0) {
                            sizesX[rotation] = sizeDeltaX
                            sizesY[rotation] = sizeDeltaY
                        } else if (overlayRotation.toInt() == 1) {
                            sizesX[rotation] = sizeDeltaY
                            sizesY[rotation] = 512 - sizeDeltaX
                        } else if (overlayRotation.toInt() == 2) {
                            sizesX[rotation] = 512 - sizeDeltaX
                            sizesY[rotation] = 512 - sizeDeltaY
                        } else if (overlayRotation.toInt() == 3) {
                            sizesX[rotation] = 512 - sizeDeltaY
                            sizesY[rotation] = sizeDeltaX
                        }
                        if (nextLvlHeightDifference != null && OVERLAY_SHAPE_SUPPORTS_HEIGHT[overlayShape.toInt()][rotation]) {
                            val sceneX = (x shl 9) + sizesX[rotation]
                            val sceneY = (y shl 9) + sizesY[rotation]
                            nextLvlHeightDifference[rotation] = nextHeights!!.getAverageHeight(sceneX, sceneY) - localHeights.getAverageHeight(sceneX, sceneY)
                        }
                        if (localLvlHeightDifference != null) {
                            if (nextHeights != null && !OVERLAY_SHAPE_SUPPORTS_HEIGHT[overlayShape.toInt()][rotation]) {
                                val sceneX = (x shl 9) + sizesX[rotation]
                                val sceneY = (y shl 9) + sizesY[rotation]
                                localLvlHeightDifference[rotation] = localHeights.getAverageHeight(sceneX, sceneY) - nextHeights.getAverageHeight(sceneX, sceneY)
                            } else if (currentHeights != null && !aBoolArrayArray3823[overlayShape.toInt()][rotation]) {
                                val sceneX = (x shl 9) + sizesX[rotation]
                                val sceneY = (y shl 9) + sizesY[rotation]
                                localLvlHeightDifference[rotation] = currentHeights.getAverageHeight(sceneX, sceneY) - localHeights.getAverageHeight(sceneX, sceneY)
                            }
                        }
                    }
                    val heightSw = localHeights.getHeight(x, y)
                    val heightSe = localHeights.getHeight(x + 1, y)
                    val heightNe = localHeights.getHeight(x + 1, y + 1)
                    val heightNw = localHeights.getHeight(x, y + 1)
                    val hideRoofs = regionMap.areRoofsHidden(x, y)
                    if ((if (hideRoofs) plane > 1 else plane > 0)) {
                        var hideOverlay = true
                        if (underlay != null && !underlay.occlude) {
                            hideOverlay = false
                        } else if (underlayId == 0 && overlayShape.toInt() != 0) {
                            hideOverlay = false
                        } else if (overlayId > 0 && Overlay != null && !Overlay.occlude) {
                            hideOverlay = false
                        }
                        if (hideOverlay && heightSw == heightSe && heightSw == heightNe && heightSw == heightNw) {
                            cullingsMap!![plane]!![x][y] = (cullingsMap!![plane]!![x][y].toInt() or 0x4).toByte()
                        }
                    }
                    val tile = WaterTile()
                    if (overlayHidden) {
                        tile.color = scene.getColor(x, y)
                        tile.scale = scene.getScale(x, y)
                        tile.intensity = scene.getIntensity(x, y)
                        tile.floOpcode20 = scene.getWaterOpcode20(x, y)
                        tile.floUnusedOpcode21 = scene.unusedOpcode21(x, y)
                        tile.floUnusedOpcode22 = scene.unusedOpcode22(x, y)
                    }
                    localHeights.calculateBlockTiles(x, y, sizesX, nextLvlHeightDifference!!, sizesY, localLvlHeightDifference!!, ints_20!!, ints_21!!, ints_22!!, floorColor, minimapColor!!, overlayTextures, textureScale, tile)
                    scene.loadTiles(plane, x, y)
                }
            }
        }
    }

    /**
     * Used every display mode above min
     */
    fun addBlendedTiles(toolkit: AbstractToolkit, ground: Ground, z: Int, paletteIndices: Array<IntArray>, heightsCurrentLvl: Ground?, heightsNextLvl: Ground?) {
        val localPathShape = overlayPathShapes[z]
        val localOverlayRotation = overlayRotations[z]
        val localUnderlays = underlayIds[z]
        val localOverlays = overlayIds[z]
        val hasFacesOn = BooleanArray(4)
        for (x in 0 until regionSizeX) {
            val nextX = if (x < regionSizeX - 1) x + 1 else x
            for (y in 0 until regionSizeY) {
                val nextY = if (y < regionSizeY - 1) y + 1 else y
                pathShape = localPathShape[x][y].toInt()
                tileRotation = localOverlayRotation[x][y].toInt()
                val overlayId = localOverlays[x][y].toInt() and 0xff
                val underlayId = localUnderlays[x][y].toInt() and 0xff
                if (overlayId != 0 || underlayId != 0) {
                    var initialOverlay: FloType? = if (overlayId != 0) overlayTypeList.list(overlayId - 1) else null
                    val initialUnderlay: FluType? = if (underlayId != 0) underlayTypeList.list(underlayId - 1) else null
                    if (pathShape == 0 && initialOverlay == null) {
                        pathShape = 12
                    }
                    hasShadows = false
                    overlaySupportsBlending = false
                    hasFacesOn[3] = false
                    hasFacesOn[2] = false
                    hasFacesOn[1] = false
                    hasFacesOn[0] = false
                    val Overlay = initialOverlay
                    if (initialOverlay != null) {
                        if (initialOverlay.tileColorHsl == -1 && initialOverlay.minimapColorHsl == -1) {
                            initialOverlay = null
                        } else if (initialUnderlay != null && pathShape != 0) {
                            overlaySupportsBlending = initialOverlay.blendsWithUnderlay
                        }
                    }
                    tileRotation = calculateUnderlayRotation(underlayId, x, y, nextX, nextY, ground, localUnderlays)
                    for (slot in 0..12) {
                        waterSlots[slot] = -1
                        overlayFunctionMask[slot] = 1
                    }
                    calculateOverlayRotation(toolkit, initialOverlay, x, y, localPathShape, localOverlayRotation, localOverlays, hasFacesOn)
                    unblendable = !overlaySupportsBlending && !hasFacesOn[0] && !hasFacesOn[2] && !hasFacesOn[1] && !hasFacesOn[3]
                    calculateFaceCounts(initialOverlay, initialUnderlay)
                    var floorFaceCount = underlayFaceCount + overlayFaceCount
                    if (floorFaceCount <= 0) {
                        scene.loadTiles(z, x, y)
                    } else {
                        if (hasFacesOn[0]) {
                            floorFaceCount++
                        }
                        if (hasFacesOn[2]) {
                            floorFaceCount++
                        }
                        if (hasFacesOn[1]) {
                            floorFaceCount++
                        }
                        if (hasFacesOn[3]) {
                            floorFaceCount++
                        }
                        overlayTotalFaces = 0
                        index = 0
                        val floorVerticeCount = floorFaceCount * 3
                        val blendingColors = if (overlayHasMultiColor) IntArray(floorVerticeCount) else null
                        val offsetsX = IntArray(floorVerticeCount)
                        val offsetsZ = IntArray(floorVerticeCount)
                        val mainColors = IntArray(floorVerticeCount)
                        val waterMaterials = IntArray(floorVerticeCount)
                        val waterScaling = IntArray(floorVerticeCount)
                        val drawHeightsA = if (heightsNextLvl != null) IntArray(floorVerticeCount) else null
                        val drawHeightsB = if (heightsNextLvl == null && heightsCurrentLvl == null) null else IntArray(floorVerticeCount)
                        addOverlayTiles(toolkit, z, x, y, initialOverlay, hasFacesOn, blendingColors, offsetsX, offsetsZ, mainColors, waterMaterials, waterScaling, drawHeightsA, drawHeightsB, ground, heightsNextLvl, heightsCurrentLvl)
                        val underlaySouthWest = localUnderlays[x][nextY].toInt() and 0xff
                        val underlaySouthEast = localUnderlays[nextX][nextY].toInt() and 0xff
                        val underlayNorthEast = localUnderlays[nextX][y].toInt() and 0xff
                        addUnderlayTiles(x, y, nextX, nextY, initialUnderlay, underlayId, underlaySouthWest, underlaySouthEast, underlayNorthEast, hasFacesOn, blendingColors, offsetsX, offsetsZ, mainColors, waterMaterials, waterScaling, drawHeightsA, drawHeightsB, paletteIndices, ground, heightsNextLvl, heightsCurrentLvl)
                        setupCulling(ground, initialUnderlay, Overlay, z, x, y, nextX, nextY, underlayId, overlayId)
                        val waterTile = WaterTile()
                        if (overlayHidden) {
                            waterTile.color = scene.getColor(x, y)
                            waterTile.scale = scene.getScale(x, y)
                            waterTile.intensity = scene.getIntensity(x, y)
                            waterTile.floOpcode20 = scene.getWaterOpcode20(x, y)
                            waterTile.floUnusedOpcode21 = scene.unusedOpcode21(x, y)
                            waterTile.floUnusedOpcode22 = scene.unusedOpcode22(x, y)
                        }
                        ground.addBlendedTile(x, y, offsetsX, drawHeightsA, offsetsZ, drawHeightsB, mainColors, blendingColors, waterMaterials, waterScaling, waterTile, hasShadows)
                        scene.loadTiles(z, x, y)
                    }
                }
            }
        }
    }

    fun calculateOverlayDisplay(toolkit: AbstractToolkit, overlaytype: FloType?, x: Int, y: Int, rSizeX: Int, rSizeY: Int, lOverlay: Array<ByteArray>, lOverlayShape: Array<ByteArray>, lOverlayRotation: Array<ByteArray>, hasFacesOn: BooleanArray) {
        val blendingAttributes = if (overlaytype != null && overlaytype.blendsWithUnderlay) pathBlendings[pathShape] else pathSquares[pathShape]
        if (y > 0) {
            if (x > 0) {
                val overlayId = lOverlay[x - 1][y - 1].toInt() and 0xff
                if (overlayId > 0) {
                    val overlay = overlayTypeList.list(overlayId - 1)
                    if (overlay.tileColorHsl != -1 && overlay.blendsWithUnderlay) {
                        val shape = lOverlayShape[x - 1][y - 1]
                        val rotation = lOverlayRotation[x - 1][y - 1] * 2 + 4 and 0x7
                        val renderColor = ColorUtil.getOverlayColorHsl(toolkit, overlay)
                        if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape.toInt()][rotation]) {
                            overlayTileColors[0] = overlay.tileColorHsl
                            overlayTileRenderedColors[0] = renderColor
                            waterTextureIds[0] = overlay.texture
                            waterTextureScaling[0] = overlay.textureScale
                            waterSlots[0] = overlay.slot
                            overlayFunctionMask[0] = 256
                        }
                    }
                }
            }
            if (x < rSizeX - 1) {
                val overlayId = lOverlay[x + 1][y - 1].toInt() and 0xff
                if (overlayId > 0) {
                    val overlay = overlayTypeList.list(overlayId - 1)
                    if (overlay.tileColorHsl != -1 && overlay.blendsWithUnderlay) {
                        val shape = lOverlayShape[x + 1][y - 1]
                        val rotation = lOverlayRotation[x + 1][y - 1] * 2 + 6 and 0x7
                        val renderColor = ColorUtil.getOverlayColorHsl(toolkit, overlay)
                        if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape.toInt()][rotation]) {
                            overlayTileColors[2] = overlay.tileColorHsl
                            overlayTileRenderedColors[2] = renderColor
                            waterTextureIds[2] = overlay.texture
                            waterTextureScaling[2] = overlay.textureScale
                            waterSlots[2] = overlay.slot
                            overlayFunctionMask[2] = 512
                        }
                    }
                }
            }
        }
        if (y < rSizeY - 1) {
            if (x > 0) {
                val underlayId = lOverlay[x - 1][y + 1].toInt() and 0xff
                if (underlayId > 0) {
                    val overlay = overlayTypeList.list(underlayId - 1)
                    if (overlay.tileColorHsl != -1 && overlay.blendsWithUnderlay) {
                        val shape = lOverlayShape[x - 1][y + 1]
                        val rotation = lOverlayRotation[x - 1][y + 1] * 2 + 2 and 0x7
                        val renderColor = ColorUtil.getOverlayColorHsl(toolkit, overlay)
                        if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape.toInt()][rotation]) {
                            overlayTileColors[6] = overlay.tileColorHsl
                            overlayTileRenderedColors[6] = renderColor
                            waterTextureIds[6] = overlay.texture
                            waterTextureScaling[6] = overlay.textureScale
                            waterSlots[6] = overlay.slot
                            overlayFunctionMask[6] = 64
                        }
                    }
                }
            }
            if (x < rSizeX - 1) {
                val overlayId = lOverlay[x + 1][y + 1].toInt() and 0xff
                if (overlayId > 0) {
                    val overlay = overlayTypeList.list(overlayId - 1)
                    if (overlay.tileColorHsl != -1 && overlay.blendsWithUnderlay) {
                        val shape = lOverlayShape[x + 1][y + 1]
                        val rotation = lOverlayRotation[x + 1][y + 1] * 2 and 0x7
                        val renderColor = ColorUtil.getOverlayColorHsl(toolkit, overlay)
                        if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape.toInt()][rotation]) {
                            overlayTileColors[4] = overlay.tileColorHsl
                            overlayTileRenderedColors[4] = renderColor
                            waterTextureIds[4] = overlay.texture
                            waterTextureScaling[4] = overlay.textureScale
                            waterSlots[4] = overlay.slot
                            overlayFunctionMask[4] = 128
                        }
                    }
                }
            }
        }

        if (y > 0) {
            val overlayId = lOverlay[x][y - 1].toInt() and 0xff
            if (overlayId > 0) {
                val overlay = overlayTypeList.list(overlayId - 1)
                if (overlay.tileColorHsl != -1) {
                    val shape = lOverlayShape[x][y - 1]
                    val rotation = lOverlayRotation[x][y - 1]
                    if (overlay.blendsWithUnderlay) {
                        var signedRotationA = 2
                        var signedRotationB = rotation * 2 + 4
                        val i_20 = ColorUtil.getOverlayColorHsl(toolkit, overlay)
                        for (i_21 in 0..2) {
                            signedRotationB = signedRotationB and 0x7
                            signedRotationA = signedRotationA and 0x7
                            if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape.toInt()][signedRotationB] && waterSlots[signedRotationA] <= overlay.slot) {
                                overlayTileColors[signedRotationA] = overlay.tileColorHsl
                                overlayTileRenderedColors[signedRotationA] = i_20
                                waterTextureIds[signedRotationA] = overlay.texture
                                waterTextureScaling[signedRotationA] = overlay.textureScale
                                if (overlay.slot == waterSlots[signedRotationA]) {
                                    overlayFunctionMask[signedRotationA] = overlayFunctionMask[signedRotationA] or 0x20
                                } else {
                                    overlayFunctionMask[signedRotationA] = 32
                                }
                                waterSlots[signedRotationA] = overlay.slot
                            }
                            signedRotationB++
                            signedRotationA--
                        }
                        if (!blendingAttributes[tileRotation and 0x3]) {
                            hasFacesOn[0] = pathBlendings[shape.toInt()][rotation + 2 and 0x3]
                        }
                    } else if (!blendingAttributes[tileRotation and 0x3]) {
                        hasFacesOn[0] = pathSquares[shape.toInt()][rotation + 2 and 0x3]
                    }
                }
            }
        }
        if (y < rSizeY - 1) {
            val overlayId = lOverlay[x][y + 1].toInt() and 0xff
            if (overlayId > 0) {
                val overlay = overlayTypeList.list(overlayId - 1)
                if (overlay.tileColorHsl != -1) {
                    val shape = lOverlayShape[x][y + 1]
                    val rotation = lOverlayRotation[x][y + 1]
                    if (overlay.blendsWithUnderlay) {
                        var signedRotationA = 4
                        var signedRotationB = rotation * 2 + 2
                        val i_20 = ColorUtil.getOverlayColorHsl(toolkit, overlay)
                        for (i_21 in 0..2) {
                            signedRotationB = signedRotationB and 0x7
                            signedRotationA = signedRotationA and 0x7
                            if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape.toInt()][signedRotationB] && waterSlots[signedRotationA] <= overlay.slot) {
                                overlayTileColors[signedRotationA] = overlay.tileColorHsl
                                overlayTileRenderedColors[signedRotationA] = i_20
                                waterTextureIds[signedRotationA] = overlay.texture
                                waterTextureScaling[signedRotationA] = overlay.textureScale
                                if (overlay.slot == waterSlots[signedRotationA]) {
                                    overlayFunctionMask[signedRotationA] = overlayFunctionMask[signedRotationA] or 0x10
                                } else {
                                    overlayFunctionMask[signedRotationA] = 16
                                }
                                waterSlots[signedRotationA] = overlay.slot
                            }
                            signedRotationB--
                            signedRotationA++
                        }
                        if (!blendingAttributes[tileRotation + 2 and 0x3]) {
                            hasFacesOn[2] = pathBlendings[shape.toInt()][rotation.toInt() and 0x3]
                        }
                    } else if (!blendingAttributes[tileRotation + 2 and 0x3]) {
                        hasFacesOn[2] = pathSquares[shape.toInt()][rotation.toInt() and 0x3]
                    }
                }
            }
        }
        if (x > 0) {
            val overlayId = lOverlay[x - 1][y].toInt() and 0xff
            if (overlayId > 0) {
                val overlay = overlayTypeList.list(overlayId - 1)
                if (overlay.tileColorHsl != -1) {
                    val shape = lOverlayShape[x - 1][y]
                    val rotation = lOverlayRotation[x - 1][y]
                    if (overlay.blendsWithUnderlay) {
                        var signedRotationA = 6
                        var signedRotationB = rotation * 2 + 4
                        val i_20 = ColorUtil.getOverlayColorHsl(toolkit, overlay)
                        for (i_21 in 0..2) {
                            signedRotationB = signedRotationB and 0x7
                            signedRotationA = signedRotationA and 0x7
                            if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape.toInt()][signedRotationB] && waterSlots[signedRotationA] <= overlay.slot) {
                                overlayTileColors[signedRotationA] = overlay.tileColorHsl
                                overlayTileRenderedColors[signedRotationA] = i_20
                                waterTextureIds[signedRotationA] = overlay.texture
                                waterTextureScaling[signedRotationA] = overlay.textureScale
                                if (overlay.slot == waterSlots[signedRotationA]) {
                                    overlayFunctionMask[signedRotationA] = overlayFunctionMask[signedRotationA] or 0x8
                                } else {
                                    overlayFunctionMask[signedRotationA] = 8
                                }
                                waterSlots[signedRotationA] = overlay.slot
                            }
                            signedRotationB--
                            signedRotationA++
                        }
                        if (!blendingAttributes[tileRotation + 3 and 0x3]) {
                            hasFacesOn[3] = pathBlendings[shape.toInt()][rotation + 1 and 0x3]
                        }
                    } else if (!blendingAttributes[tileRotation + 3 and 0x3]) {
                        hasFacesOn[3] = pathSquares[shape.toInt()][rotation + 1 and 0x3]
                    }
                }
            }
        }
        if (x < rSizeX - 1) {
            val underlayId = lOverlay[x + 1][y].toInt() and 0xff
            if (underlayId > 0) {
                val overlay = overlayTypeList.list(underlayId - 1)
                if (overlay.tileColorHsl != -1) {
                    val shape = lOverlayShape[x + 1][y]
                    val rotation = lOverlayRotation[x + 1][y]
                    if (overlay.blendsWithUnderlay) {
                        var signedRotationB = 4
                        var signedRotationA = rotation * 2 + 6
                        val renderColorHsl = ColorUtil.getOverlayColorHsl(toolkit, overlay)
                        for (vertice in 0..2) {
                            signedRotationA = signedRotationA and 0x7
                            signedRotationB = signedRotationB and 0x7
                            if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape.toInt()][signedRotationA] && waterSlots[signedRotationB] <= overlay.slot) {
                                overlayTileColors[signedRotationB] = overlay.tileColorHsl
                                overlayTileRenderedColors[signedRotationB] = renderColorHsl
                                waterTextureIds[signedRotationB] = overlay.texture
                                waterTextureScaling[signedRotationB] = overlay.textureScale
                                if (overlay.slot == waterSlots[signedRotationB]) {
                                    overlayFunctionMask[signedRotationB] = overlayFunctionMask[signedRotationB] or 0x4
                                } else {
                                    overlayFunctionMask[signedRotationB] = 4
                                }
                                waterSlots[signedRotationB] = overlay.slot
                            }
                            ++signedRotationA
                            --signedRotationB
                        }
                        if (!blendingAttributes[tileRotation + 1 and 0x3]) {
                            hasFacesOn[1] = pathBlendings[shape.toInt()][rotation + 3 and 0x3]
                        }
                    } else if (!blendingAttributes[tileRotation + 1 and 0x3]) {
                        hasFacesOn[1] = pathSquares[shape.toInt()][rotation + 3 and 0x3]
                    }
                }
            }
        }
        if (overlaytype != null && overlaytype.blendsWithUnderlay) {
            val colorHsl = ColorUtil.getOverlayColorHsl(toolkit, overlaytype)
            for (index in 0..7) {
                val signedRotation = index - tileRotation * 2 and 0x7
                if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[pathShape][index] && waterSlots[signedRotation] <= overlaytype.slot) {
                    overlayTileColors[signedRotation] = overlaytype.tileColorHsl
                    overlayTileRenderedColors[signedRotation] = colorHsl
                    waterTextureIds[signedRotation] = overlaytype.texture
                    waterTextureScaling[signedRotation] = overlaytype.textureScale
                    if (overlaytype.slot == waterSlots[signedRotation]) {
                        overlayFunctionMask[signedRotation] = overlayFunctionMask[signedRotation] or 0x2
                    } else {
                        overlayFunctionMask[signedRotation] = 2
                    }
                    waterSlots[signedRotation] = overlaytype.slot
                }
            }
        }
    }

    fun calculateFaceCounts(overlay: FloType?, underlay: FluType?) {
        if (unblendable) {
            anIntArray3857 = anIntArrayArray3824[pathShape]
            anIntArray3858 = anIntArrayArray3860[pathShape]
            anIntArray3859 = anIntArrayArray3815[pathShape]
            overlayFaceCount = if (overlay != null) OVERLAY_FACE_COUNT[pathShape] else 0
            underlayFaceCount = if (underlay != null) UNDERLAY_FACE_COUNT[pathShape] else 0
        } else if (overlaySupportsBlending) {
            anIntArray3857 = anIntArrayArray3775[pathShape]
            anIntArray3858 = anIntArrayArray3821[pathShape]
            anIntArray3859 = anIntArrayArray3836[pathShape]
            overlayFaceCount = if (overlay != null) OVERLAY_FACE_COUNT_BLENDED[pathShape] else 0
            underlayFaceCount = if (underlay != null) UNDERLAY_FACE_COUNT_BLENDED[pathShape] else 0
            overlayFaceCounts = anIntArrayArray3833[pathShape]
        } else {
            anIntArray3857 = anIntArrayArray3774[pathShape]
            anIntArray3858 = anIntArrayArray3830[pathShape]
            anIntArray3859 = anIntArrayArray3831[pathShape]
            overlayFaceCount = if (overlay != null) anIntArray3826[pathShape] else 0
            underlayFaceCount = if (underlay != null) anIntArray3847[pathShape] else 0
            overlayFaceCounts = anIntArrayArray3828[pathShape]
        }
    }

    fun addOverlayTiles(toolkit: AbstractToolkit, i_2: Int, x: Int, y: Int, overlay: FloType?, bools_6: BooleanArray, ints_7: IntArray?, sizesX: IntArray, sizesY: IntArray, ints_10: IntArray, waterTextures: IntArray, waterTexturesScale: IntArray, groundDrawHeights: IntArray?, ints_14: IntArray?, groundA: Ground, groundB: Ground?, groundC: Ground?) {
        overlayHslColor = -1
        overlayTextureId = -1
        overlayTextureScale = 256
        if (overlay != null) {
            overlayHslColor = overlay.tileColorHsl
            overlayTextureId = overlay.texture
            overlayTextureScale = overlay.textureScale
            val overlayBaseColor = ColorUtil.getOverlayColorHsl(toolkit, overlay)
            for (overlayFace in 0 until overlayFaceCount) {
                val b_22: Byte
                if (bools_6[-tileRotation and 0x3] && overlayTotalFaces == overlayFaceCounts!![0]) {
                    overlayTileRotations[0] = anIntArray3857!![overlayTotalFaces]
                    overlayTileRotations[1] = 1
                    overlayTileRotations[2] = anIntArray3859!![overlayTotalFaces]
                    overlayTileRotations[3] = 1
                    overlayTileRotations[4] = anIntArray3858!![overlayTotalFaces]
                    overlayTileRotations[5] = anIntArray3859!![overlayTotalFaces]
                    b_22 = 6
                } else if (bools_6[2 - tileRotation and 0x3] && overlayFaceCounts!![2] == overlayTotalFaces) {
                    overlayTileRotations[0] = anIntArray3857!![overlayTotalFaces]
                    overlayTileRotations[1] = 5
                    overlayTileRotations[2] = anIntArray3859!![overlayTotalFaces]
                    overlayTileRotations[3] = 5
                    overlayTileRotations[4] = anIntArray3858!![overlayTotalFaces]
                    overlayTileRotations[5] = anIntArray3859!![overlayTotalFaces]
                    b_22 = 6
                } else if (bools_6[1 - tileRotation and 0x3] && overlayFaceCounts!![1] == overlayTotalFaces) {
                    overlayTileRotations[0] = anIntArray3857!![overlayTotalFaces]
                    overlayTileRotations[1] = 3
                    overlayTileRotations[2] = anIntArray3859!![overlayTotalFaces]
                    overlayTileRotations[3] = 3
                    overlayTileRotations[4] = anIntArray3858!![overlayTotalFaces]
                    overlayTileRotations[5] = anIntArray3859!![overlayTotalFaces]
                    b_22 = 6
                } else if (bools_6[3 - tileRotation and 0x3] && overlayFaceCounts!![3] == overlayTotalFaces) {
                    overlayTileRotations[0] = anIntArray3857!![overlayTotalFaces]
                    overlayTileRotations[1] = 7
                    overlayTileRotations[2] = anIntArray3859!![overlayTotalFaces]
                    overlayTileRotations[3] = 7
                    overlayTileRotations[4] = anIntArray3858!![overlayTotalFaces]
                    overlayTileRotations[5] = anIntArray3859!![overlayTotalFaces]
                    b_22 = 6
                } else {
                    overlayTileRotations[0] = anIntArray3857!![overlayTotalFaces]
                    overlayTileRotations[1] = anIntArray3858!![overlayTotalFaces]
                    overlayTileRotations[2] = anIntArray3859!![overlayTotalFaces]
                    b_22 = 3
                }
                for (i_23 in 0 until b_22) {
                    val rotation = overlayTileRotations[i_23]
                    val i_25 = rotation - tileRotation * 2 and 0x7
                    val i_26 = tileSizeDeltasX[rotation]
                    val i_27 = tileSizeDeltasY[rotation]
                    val sizeX: Int
                    val sizeY: Int
                    if (tileRotation == 1) {
                        sizeX = i_27
                        sizeY = 512 - i_26
                    } else if (tileRotation == 2) {
                        sizeX = 512 - i_26
                        sizeY = 512 - i_27
                    } else if (tileRotation == 3) {
                        sizeX = 512 - i_27
                        sizeY = i_26
                    } else {
                        sizeX = i_26
                        sizeY = i_27
                    }
                    sizesX[index] = sizeX
                    sizesY[index] = sizeY
                    var sceneX: Int
                    var sceneY: Int
                    if (groundDrawHeights != null && OVERLAY_SHAPE_SUPPORTS_HEIGHT[pathShape][rotation]) {
                        sceneX = (x shl 9) + sizeX
                        sceneY = (y shl 9) + sizeY
                        groundDrawHeights[index] = groundB!!.getAverageHeight(sceneX, sceneY) - groundA.getAverageHeight(sceneX, sceneY)
                    }
                    if (ints_14 != null) {
                        if (groundB != null && !OVERLAY_SHAPE_SUPPORTS_HEIGHT[pathShape][rotation]) {
                            sceneX = (x shl 9) + sizeX
                            sceneY = (y shl 9) + sizeY
                            ints_14[index] = groundA.getAverageHeight(sceneX, sceneY) - groundB.getAverageHeight(sceneX, sceneY)
                        } else if (groundC != null && !aBoolArrayArray3823[pathShape][rotation]) {
                            sceneX = (x shl 9) + sizeX
                            sceneY = (y shl 9) + sizeY
                            ints_14[index] = groundC.getAverageHeight(sceneX, sceneY) - groundA.getAverageHeight(sceneX, sceneY)
                        }
                    }
                    if (rotation < 8 && waterSlots[i_25] > overlay.slot) {
                        if (ints_7 != null) {
                            ints_7[index] = overlayTileRenderedColors[i_25]
                        }
                        waterTexturesScale[index] = waterTextureScaling[i_25]
                        waterTextures[index] = waterTextureIds[i_25]
                        ints_10[index] = overlayTileColors[i_25]
                    } else {
                        if (ints_7 != null) {
                            ints_7[index] = overlayBaseColor
                        }
                        waterTextures[index] = overlay.texture
                        waterTexturesScale[index] = overlay.textureScale
                        ints_10[index] = overlayHslColor
                    }
                    ++index
                }
                ++overlayTotalFaces
            }
            if (!overlayHidden && i_2 == 0) {
                scene.addWater(x, y, overlay.waterColor, overlay.waterFogDepth, overlay.waterIntensity, overlay.opcode20, overlay.unusedOpcode21, overlay.unusedOpcode22)
            }
            if (pathShape != 12 && overlay.tileColorHsl != -1 && overlay.shadowed) {
                hasShadows = true
            }
        } else if (unblendable) {
            overlayTotalFaces += OVERLAY_FACE_COUNT[pathShape]
        } else if (overlaySupportsBlending) {
            overlayTotalFaces += OVERLAY_FACE_COUNT_BLENDED[pathShape]
        } else {
            overlayTotalFaces += anIntArray3826[pathShape]
        }
    }

    fun addUnderlayTiles(x: Int, y: Int, nextX: Int, nextY: Int, underlay: FluType?, underlayIdA: Int, underlayIdB: Int, underlayIdC: Int, underlayIdD: Int, blendable: BooleanArray, overRenderColor: IntArray?, sizesX: IntArray, sizesY: IntArray, overColor: IntArray, overTexture: IntArray, overScale: IntArray, groundHeightsA: IntArray?, groundHeightsB: IntArray?, overlayColors: Array<IntArray>, groundA: Ground, groundB: Ground?, groundC: Ground?) {
        if (underlay != null) {
            var underlayIdB = underlayIdB
            var underlayIdC = underlayIdC
            var underlayIdD = underlayIdD
            if (underlayIdB == 0) {
                underlayIdB = underlayIdA
            }
            if (underlayIdC == 0) {
                underlayIdC = underlayIdA
            }
            if (underlayIdD == 0) {
                underlayIdD = underlayIdA
            }
            val underlayA = underlayTypeList.list(underlayIdA - 1)
            val underlayB = underlayTypeList.list(underlayIdB - 1)
            val underlayC = underlayTypeList.list(underlayIdC - 1)
            val underlayD = underlayTypeList.list(underlayIdD - 1)
            for (underlayFace in 0 until underlayFaceCount) {
                val b_32: Byte
                if (blendable[-tileRotation and 0x3] && overlayTotalFaces == overlayFaceCounts!![0]) {
                    overlayTileRotations[0] = anIntArray3857!![overlayTotalFaces]
                    overlayTileRotations[1] = 1
                    overlayTileRotations[2] = anIntArray3859!![overlayTotalFaces]
                    overlayTileRotations[3] = 1
                    overlayTileRotations[4] = anIntArray3858!![overlayTotalFaces]
                    overlayTileRotations[5] = anIntArray3859!![overlayTotalFaces]
                    b_32 = 6
                } else if (blendable[2 - tileRotation and 0x3] && overlayFaceCounts!![2] == overlayTotalFaces) {
                    overlayTileRotations[0] = anIntArray3857!![overlayTotalFaces]
                    overlayTileRotations[1] = 5
                    overlayTileRotations[2] = anIntArray3859!![overlayTotalFaces]
                    overlayTileRotations[3] = 5
                    overlayTileRotations[4] = anIntArray3858!![overlayTotalFaces]
                    overlayTileRotations[5] = anIntArray3859!![overlayTotalFaces]
                    b_32 = 6
                } else if (blendable[1 - tileRotation and 0x3] && overlayFaceCounts!![1] == overlayTotalFaces) {
                    overlayTileRotations[0] = anIntArray3857!![overlayTotalFaces]
                    overlayTileRotations[1] = 3
                    overlayTileRotations[2] = anIntArray3859!![overlayTotalFaces]
                    overlayTileRotations[3] = 3
                    overlayTileRotations[4] = anIntArray3858!![overlayTotalFaces]
                    overlayTileRotations[5] = anIntArray3859!![overlayTotalFaces]
                    b_32 = 6
                } else if (blendable[3 - tileRotation and 0x3] && overlayFaceCounts!![3] == overlayTotalFaces) {
                    overlayTileRotations[0] = anIntArray3857!![overlayTotalFaces]
                    overlayTileRotations[1] = 7
                    overlayTileRotations[2] = anIntArray3859!![overlayTotalFaces]
                    overlayTileRotations[3] = 7
                    overlayTileRotations[4] = anIntArray3858!![overlayTotalFaces]
                    overlayTileRotations[5] = anIntArray3859!![overlayTotalFaces]
                    b_32 = 6
                } else {
                    overlayTileRotations[0] = anIntArray3857!![overlayTotalFaces]
                    overlayTileRotations[1] = anIntArray3858!![overlayTotalFaces]
                    overlayTileRotations[2] = anIntArray3859!![overlayTotalFaces]
                    b_32 = 3
                }
                for (i_33 in 0 until b_32) {
                    val rotation = overlayTileRotations[i_33]
                    val slot = rotation - tileRotation * 2 and 0x7
                    val dX = tileSizeDeltasX[rotation]
                    val dY = tileSizeDeltasY[rotation]
                    val sizeX: Int
                    val sizeY: Int
                    if (tileRotation == 1) {
                        sizeX = dY
                        sizeY = 512 - dX
                    } else if (tileRotation == 2) {
                        sizeX = 512 - dX
                        sizeY = 512 - dY
                    } else if (tileRotation == 3) {
                        sizeX = 512 - dY
                        sizeY = dX
                    } else {
                        sizeX = dX
                        sizeY = dY
                    }
                    sizesX[index] = sizeX
                    sizesY[index] = sizeY
                    if (groundHeightsA != null && OVERLAY_SHAPE_SUPPORTS_HEIGHT[pathShape][rotation]) {
                        val sceneX = (x shl 9) + sizeX
                        val sceneY = (y shl 9) + sizeY
                        groundHeightsA[index] = groundB!!.getAverageHeight(sceneX, sceneY) - groundA.getAverageHeight(sceneX, sceneY)
                    }
                    if (groundHeightsB != null) {
                        if (groundB != null && !OVERLAY_SHAPE_SUPPORTS_HEIGHT[pathShape][rotation]) {
                            val sceneX = (x shl 9) + sizeX
                            val sceneY = (y shl 9) + sizeY
                            groundHeightsB[index] = groundA.getAverageHeight(sceneX, sceneY) - groundB.getAverageHeight(sceneX, sceneY)
                        } else if (groundC != null && !aBoolArrayArray3823[pathShape][rotation]) {
                            val sceneX = (x shl 9) + sizeX
                            val sceneY = (y shl 9) + sizeY
                            groundHeightsB[index] = groundC.getAverageHeight(sceneX, sceneY) - groundA.getAverageHeight(sceneX, sceneY)
                        }
                    }
                    if (rotation < 8 && waterSlots[slot] >= 0) {
                        if (overRenderColor != null) {
                            overRenderColor[index] = overlayTileRenderedColors[slot]
                        }
                        overScale[index] = waterTextureScaling[slot]
                        overTexture[index] = waterTextureIds[slot]
                        overColor[index] = overlayTileColors[slot]
                    } else {
                        if (overlaySupportsBlending && OVERLAY_SHAPE_SUPPORTS_HEIGHT[pathShape][rotation]) {
                            overTexture[index] = overlayTextureId
                            overScale[index] = overlayTextureScale
                            overColor[index] = overlayHslColor
                        } else if (sizeX == 0 && sizeY == 0) {
                            overColor[index] = overlayColors[x][y]
                            overTexture[index] = underlayA.texture
                            overScale[index] = underlayA.scale
                        } else if (sizeX == 0 && sizeY == 512) {
                            overColor[index] = overlayColors[x][nextY]
                            overTexture[index] = underlayB.texture
                            overScale[index] = underlayB.scale
                        } else if (sizeX == 512 && sizeY == 512) {
                            overColor[index] = overlayColors[nextX][nextY]
                            overTexture[index] = underlayC.texture
                            overScale[index] = underlayC.scale
                        } else if (sizeX == 512 && sizeY == 0) {
                            overColor[index] = overlayColors[nextX][y]
                            overTexture[index] = underlayD.texture
                            overScale[index] = underlayD.scale
                        } else {
                            if (sizeX < 256) {
                                if (sizeY < 256) {
                                    overTexture[index] = underlayA.texture
                                    overScale[index] = underlayA.scale
                                } else {
                                    overTexture[index] = underlayB.texture
                                    overScale[index] = underlayB.scale
                                }
                            } else if (sizeY < 256) {
                                overTexture[index] = underlayD.texture
                                overScale[index] = underlayD.scale
                            } else {
                                overTexture[index] = underlayC.texture
                                overScale[index] = underlayC.scale
                            }
                            val overlayColorA = ColorUtil.blend(overlayColors[x][y], overlayColors[nextX][y], sizeX shl 7 shr 9)
                            val overlayColorB = ColorUtil.blend(overlayColors[x][nextY], overlayColors[nextX][nextY], sizeX shl 7 shr 9)
                            overColor[index] = ColorUtil.blend(overlayColorA, overlayColorB, sizeY shl 7 shr 9)
                        }
                        if (overRenderColor != null) {
                            overRenderColor[index] = overColor[index]
                        }
                    }
                    index++
                }
                overlayTotalFaces++
            }
            if (pathShape != 0 && underlay.shadowed) {
                hasShadows = true
            }
        }
    }

    fun setupCulling(ground: Ground, underlay: FluType?, overlay: FloType?, z: Int, x: Int, y: Int, nextX: Int, nextY: Int, underlayId: Int, overlayId: Int) {
        val heightNw = ground.getHeight(x, y)
        val heightNe = ground.getHeight(nextX, y)
        val heightSe = ground.getHeight(nextX, nextY)
        val heightSw = ground.getHeight(x, nextY)
        val toggleRoofs = regionMap.areRoofsHidden(x, y)
        if ((if (toggleRoofs) z > 1 else z > 0)) {
            var occludes = true
            if (underlay != null && !underlay.occlude) {
                occludes = false
            } else if (underlayId == 0 && pathShape != 0) {
                occludes = false
            } else if (overlayId > 0 && overlay != null && !overlay.occlude) {
                occludes = false
            }
            if (occludes && heightNe == heightNw && heightNw == heightSe && heightNw == heightSw) {
                cullingsMap!![z]!![x][y] = (cullingsMap!![z]!![x][y].toInt() or CullingFunctionMask.COMPLETELY_FLAT).toByte()
            }
        }
    }

    fun offsetTileHeights(heights2: Array<IntArray>) {
        val heights = tileHeights[0]
        for (x in 0 until regionSizeX + 1) {
            for (y in 0 until regionSizeY + 1) {
                heights[x][y] += heights2[x][y]
            }
        }
    }

    fun calculateOverlayRotation(drawingarea: AbstractToolkit, overlayType: FloType?, x: Int, y: Int, lPathShape: Array<ByteArray>, lOverlayRotation: Array<ByteArray>, lOverlay: Array<ByteArray>, hasFacesOn: BooleanArray) {
        val blendings = if (overlayType != null && overlayType.blendsWithUnderlay) pathBlendings[pathShape] else pathSquares[pathShape]
        calculateOverlayDisplay(drawingarea, overlayType, x, y, regionSizeX, regionSizeY, lOverlay, lPathShape, lOverlayRotation, hasFacesOn)
        overlayHasMultiColor = overlayType != null && overlayType.tileColorHsl != overlayType.minimapColorHsl
        if (!overlayHasMultiColor) {
            for (slot in 0..7) {
                if (waterSlots[slot] >= 0 && overlayTileRenderedColors[slot] != overlayTileColors[slot]) {
                    overlayHasMultiColor = true
                    break
                }
            }
        }
        if (!blendings[tileRotation + 1 and 0x3]) {
            hasFacesOn[1] = hasFacesOn[1] or ((overlayFunctionMask[2] and overlayFunctionMask[4]) == 0)
        }
        if (!blendings[tileRotation + 3 and 0x3]) {
            hasFacesOn[3] = hasFacesOn[3] or ((overlayFunctionMask[6] and overlayFunctionMask[0]) == 0)
        }
        if (!blendings[tileRotation and 0x3]) {
            hasFacesOn[0] = hasFacesOn[0] or ((overlayFunctionMask[0] and overlayFunctionMask[2]) == 0)
        }
        if (!blendings[tileRotation + 2 and 0x3]) {
            hasFacesOn[2] = hasFacesOn[2] or ((overlayFunctionMask[4] and overlayFunctionMask[6]) == 0)
        }
        if (!overlaySupportsBlending && (pathShape == 0 || pathShape == 12)) {
            if (hasFacesOn[0] && !hasFacesOn[1] && !hasFacesOn[2] && hasFacesOn[3]) {
                hasFacesOn[3] = false
                hasFacesOn[0] = false
                pathShape = if (pathShape == 0) 13 else 14
                tileRotation = 0
            } else if (hasFacesOn[0] && hasFacesOn[1] && !hasFacesOn[2] && !hasFacesOn[3]) {
                hasFacesOn[1] = false
                hasFacesOn[0] = false
                pathShape = if (pathShape == 0) 13 else 14
                tileRotation = 3
            } else if (!hasFacesOn[0] && hasFacesOn[1] && hasFacesOn[2] && !hasFacesOn[3]) {
                hasFacesOn[2] = false
                hasFacesOn[1] = false
                pathShape = if (pathShape == 0) 13 else 14
                tileRotation = 2
            } else if (!hasFacesOn[0] && !hasFacesOn[1] && hasFacesOn[2] && hasFacesOn[3]) {
                hasFacesOn[3] = false
                hasFacesOn[2] = false
                pathShape = if (pathShape == 0) 13 else 14
                tileRotation = 1
            }
        }
    }

    fun calculateUnderlayRotation(underlayId: Int, x: Int, y: Int, nextX: Int, nextY: Int, ground: Ground, underlays: Array<ByteArray>): Int {
        if ((pathShape == 0 || pathShape == 12) && x > 0 && y > 0 && x < regionSizeX && y < regionSizeY) {
            var underlaySouthWest = if (underlays[x - 1][y - 1].toInt() == underlayId) 1 else -1
            var underlaySouthEast = if (underlays[nextX][y - 1].toInt() == underlayId) 1 else -1
            var underlayNorthEast = if (underlays[nextX][nextY].toInt() == underlayId) 1 else -1
            var underlayNorthWest = if (underlays[x - 1][nextY].toInt() == underlayId) 1 else -1
            if (underlays[x][y - 1].toInt() == underlayId) {
                underlaySouthWest++
                underlaySouthEast++
            } else {
                underlaySouthWest--
                underlaySouthEast--
            }
            if (underlays[nextX][y].toInt() == underlayId) {
                underlaySouthEast++
                underlayNorthEast++
            } else {
                underlaySouthEast--
                underlayNorthEast--
            }
            if (underlays[x][nextY].toInt() == underlayId) {
                underlayNorthEast++
                underlayNorthWest++
            } else {
                underlayNorthEast--
                underlayNorthWest--
            }
            if (underlays[x - 1][y].toInt() == underlayId) {
                underlayNorthWest++
                underlaySouthWest++
            } else {
                underlayNorthWest--
                underlaySouthWest--
            }
            var swNeDelta = underlaySouthWest - underlayNorthEast
            if (swNeDelta < 0) {
                swNeDelta = -swNeDelta
            }
            var seNwDelta = underlaySouthEast - underlayNorthWest
            if (seNwDelta < 0) {
                seNwDelta = -seNwDelta
            }
            if (swNeDelta == seNwDelta) {
                swNeDelta = ground.getHeight(x, y) - ground.getHeight(nextX, nextY)
                if (swNeDelta < 0) {
                    swNeDelta = -swNeDelta
                }
                seNwDelta = ground.getHeight(nextX, y) - ground.getHeight(x, nextY)
                if (seNwDelta < 0) {
                    seNwDelta = -seNwDelta
                }
            }
            return if (swNeDelta < seNwDelta) 1 else 0
        }
        return tileRotation
    }

    fun decodeTile(stream: Packet, plane: Int, x: Int, y: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int, i_9: Int, underwater: Boolean) {
        var deltaY = i_6
        var deltaX = i_5
        if (i_9 == 1) {
            deltaY = 1
        } else if (i_9 == 2) {
            deltaX = 1
            deltaY = 1
        } else if (i_9 == 3) {
            deltaX = 1
        }
        var opcode: Int
        if (x >= 0 && x < regionSizeX && y >= 0 && y < regionSizeY) {
            if (!overlayHidden && !underwater) {
                regionMap.tileFunctionMasks[plane][x][y] = 0
            }
            while (true) {
                opcode = stream.readUByte()
                if (opcode == 0) {
                    if (overlayHidden) {
                        tileHeights[0][x + deltaX][y + deltaY] = 0
                    } else if (plane == 0) {
                        tileHeights[0][x + deltaX][y + deltaY] = -TileUtils.calculateTileheight(i_7 + 932731, i_8 + 556238) * 8 shl 2
                    } else {
                        tileHeights[plane][x + deltaX][y + deltaY] = tileHeights[plane - 1][x + deltaX][y + deltaY] - 960
                    }
                    break
                }
                if (opcode == 1) {
                    var i_13 = stream.readUByte()
                    if (!overlayHidden) {
                        if (i_13 == 1) {
                            i_13 = 0
                        }
                        if (plane == 0) {
                            tileHeights[0][x + deltaX][y + deltaY] = -i_13 * 8 shl 2
                        } else {
                            tileHeights[plane][x + deltaX][y + deltaY] = tileHeights[plane - 1][x + deltaX][y + deltaY] - (i_13 * 8 shl 2)
                        }
                    } else {
                        tileHeights[0][x + deltaX][y + deltaY] = i_13 * 8 shl 2
                    }
                    break
                }
                if (opcode <= 49) {
                    if (underwater) {
                        stream.readUByte()
                    } else {
                        overlayIds[plane][x][y] = stream.readByte()
                        overlayPathShapes[plane][x][y] = ((opcode - 2) / 4).toByte()
                        overlayRotations[plane][x][y] = (opcode - 2 + i_9 and 0x3).toByte()
                    }
                } else if (opcode <= 81) {
                    if (!overlayHidden && !underwater) {
                        regionMap.tileFunctionMasks[plane][x][y] = (opcode - 49).toByte()
                    }
                } else if (!underwater) {
                    underlayIds[plane][x][y] = (opcode - 81).toByte()
                }
            }
        } else {
            while (true) {
                opcode = stream.readUByte()
                if (opcode == 0) {
                    break
                }
                if (opcode == 1) {
                    stream.readUByte()
                    break
                }
                if (opcode <= 49) {
                    stream.readUByte()
                }
            }
        }
    }

    companion object {
        @JvmField
        val OVERLAY_FACE_COUNT = intArrayOf(2, 1, 1, 1, 2, 2, 2, 1, 3, 3, 3, 2, 0, 4, 0)
        @JvmField
        val UNDERLAY_FACE_COUNT = intArrayOf(0, 1, 2, 2, 1, 1, 2, 3, 1, 3, 3, 4, 2, 0, 4)
        @JvmField
        val anIntArray3826 = intArrayOf(4, 2, 1, 1, 2, 2, 3, 1, 3, 3, 3, 2, 0)
        @JvmField
        val anIntArray3847 = intArrayOf(0, 2, 2, 2, 1, 1, 3, 3, 1, 3, 3, 4, 4)
        @JvmField
        val OVERLAY_FACE_COUNT_BLENDED = intArrayOf(4, 2, 1, 1, 2, 2, 3, 1, 3, 3, 3, 2, 0)
        @JvmField
        val UNDERLAY_FACE_COUNT_BLENDED = intArrayOf(0, 4, 3, 3, 1, 1, 3, 5, 1, 5, 3, 6, 4)
        @JvmField
        val tileSizeDeltasX = intArrayOf(0, 256, 512, 512, 512, 256, 0, 0, 128, 256, 128, 384, 256)
        @JvmField
        val tileSizeDeltasY = intArrayOf(0, 0, 0, 256, 512, 512, 512, 256, 256, 384, 128, 128, 256)
        @JvmField
        val OVERLAY_SHAPE_SUPPORTS_HEIGHT = arrayOf(booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true), booleanArrayOf(true, true, true, false, false, false, true, true, false, false, false, false, true), booleanArrayOf(true, false, false, false, false, true, true, true, false, false, false, false, false), booleanArrayOf(false, false, true, true, true, true, false, false, false, false, false, false, false), booleanArrayOf(true, true, true, true, true, true, false, false, false, false, false, false, false), booleanArrayOf(true, true, true, false, false, true, true, true, false, false, false, false, false), booleanArrayOf(true, true, false, false, false, true, true, true, false, false, false, false, true), booleanArrayOf(true, true, false, false, false, false, false, true, false, false, false, false, false), booleanArrayOf(false, true, true, true, true, true, true, true, false, false, false, false, false), booleanArrayOf(true, false, false, false, true, true, true, true, true, true, false, false, false), booleanArrayOf(true, true, true, true, true, false, false, false, true, true, false, false, false), booleanArrayOf(true, true, true, false, false, false, false, false, false, false, true, true, false), BooleanArray(13), booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true), BooleanArray(13))
        @JvmField
        val aBoolArrayArray3823 = arrayOf(BooleanArray(13), booleanArrayOf(false, false, true, true, true, true, true, false, false, false, false, false, true), booleanArrayOf(true, true, true, true, true, true, false, false, false, false, false, false, false), booleanArrayOf(true, true, true, false, false, true, true, true, false, false, false, false, false), booleanArrayOf(true, false, false, false, false, true, true, true, false, false, false, false, false), booleanArrayOf(false, false, true, true, true, true, false, false, false, false, false, false, false), booleanArrayOf(false, true, true, true, true, true, false, false, false, false, false, false, true), booleanArrayOf(false, true, true, true, true, true, true, true, false, false, false, false, true), booleanArrayOf(true, true, false, false, false, false, false, true, false, false, false, false, false), booleanArrayOf(true, true, true, true, true, false, false, false, true, true, false, false, false), booleanArrayOf(true, false, false, false, true, true, true, true, true, true, false, false, false), booleanArrayOf(true, false, true, true, true, true, true, true, false, false, true, true, false), booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true), BooleanArray(13), booleanArrayOf(true, true, true, true, true, true, true, true, true, true, true, true, true))
        @JvmField
        val anIntArrayArray3824 = arrayOf(intArrayOf(0, 2), intArrayOf(0, 2), intArrayOf(0, 0, 2), intArrayOf(2, 0, 0), intArrayOf(0, 2, 0), intArrayOf(0, 0, 2), intArrayOf(0, 5, 1, 4), intArrayOf(0, 4, 4, 4), intArrayOf(4, 4, 4, 0), intArrayOf(6, 6, 6, 2, 2, 2), intArrayOf(2, 2, 2, 6, 6, 6), intArrayOf(0, 11, 6, 6, 6, 4), intArrayOf(0, 2), intArrayOf(0, 4, 4, 4), intArrayOf(0, 4, 4, 4))
        @JvmField
        val anIntArrayArray3860 = arrayOf(intArrayOf(2, 4), intArrayOf(2, 4), intArrayOf(5, 2, 4), intArrayOf(4, 5, 2), intArrayOf(2, 4, 5), intArrayOf(5, 2, 4), intArrayOf(1, 6, 2, 5), intArrayOf(1, 6, 7, 1), intArrayOf(6, 7, 1, 1), intArrayOf(0, 8, 9, 8, 9, 4), intArrayOf(8, 9, 4, 0, 8, 9), intArrayOf(2, 10, 0, 10, 11, 11), intArrayOf(2, 4), intArrayOf(1, 6, 7, 1), intArrayOf(1, 6, 7, 1))
        @JvmField
        val anIntArrayArray3815 = arrayOf(intArrayOf(6, 6), intArrayOf(6, 6), intArrayOf(6, 5, 5), intArrayOf(5, 6, 5), intArrayOf(5, 5, 6), intArrayOf(6, 5, 5), intArrayOf(5, 0, 4, 1), intArrayOf(7, 7, 1, 2), intArrayOf(7, 1, 2, 7), intArrayOf(8, 9, 4, 0, 8, 9), intArrayOf(0, 8, 9, 8, 9, 4), intArrayOf(11, 0, 10, 11, 4, 2), intArrayOf(6, 6), intArrayOf(7, 7, 1, 2), intArrayOf(7, 7, 1, 2))
        @JvmField
        val pathSquares = arrayOf(BooleanArray(4), BooleanArray(4), booleanArrayOf(false, false, true, false), booleanArrayOf(false, false, true, false), booleanArrayOf(false, false, true, false), booleanArrayOf(false, false, true, false), booleanArrayOf(true, false, true, false), booleanArrayOf(true, false, false, true), booleanArrayOf(true, false, false, true), BooleanArray(4), BooleanArray(4), BooleanArray(4), BooleanArray(4))
        @JvmField
        val anIntArrayArray3828 = arrayOf(intArrayOf(0, 1, 2, 3), intArrayOf(1, 2, 3, 0), intArrayOf(1, 2, -1, 0), intArrayOf(2, 0, -1, 1), intArrayOf(0, 1, -1, 2), intArrayOf(1, 2, -1, 0), intArrayOf(-1, 4, -1, 1), intArrayOf(-1, 1, 3, -1), intArrayOf(-1, 0, 2, -1), intArrayOf(3, 5, 2, 0), intArrayOf(0, 2, 5, 3), intArrayOf(0, 2, 3, 5), intArrayOf(0, 1, 2, 3))
        @JvmField
        val anIntArrayArray3774 = arrayOf(intArrayOf(0, 2, 4, 6), intArrayOf(6, 0, 2, 4), intArrayOf(6, 0, 2), intArrayOf(2, 6, 0), intArrayOf(0, 2, 6), intArrayOf(6, 0, 2), intArrayOf(5, 6, 0, 1, 2, 4), intArrayOf(7, 2, 4, 4), intArrayOf(2, 4, 4, 7), intArrayOf(6, 6, 4, 0, 2, 2), intArrayOf(0, 2, 2, 6, 6, 4), intArrayOf(0, 2, 2, 4, 6, 6), intArrayOf(0, 2, 4, 6))
        @JvmField
        val anIntArrayArray3830 = arrayOf(intArrayOf(2, 4, 6, 0), intArrayOf(0, 2, 4, 6), intArrayOf(0, 2, 4), intArrayOf(4, 0, 2), intArrayOf(2, 4, 0), intArrayOf(0, 2, 4), intArrayOf(6, 0, 1, 2, 4, 5), intArrayOf(0, 4, 7, 6), intArrayOf(4, 7, 6, 0), intArrayOf(0, 8, 6, 2, 9, 4), intArrayOf(2, 9, 4, 0, 8, 6), intArrayOf(2, 11, 4, 6, 10, 0), intArrayOf(2, 4, 6, 0))
        @JvmField
        val anIntArrayArray3831 = arrayOf(intArrayOf(12, 12, 12, 12), intArrayOf(12, 12, 12, 12), intArrayOf(5, 5, 5), intArrayOf(5, 5, 5), intArrayOf(5, 5, 5), intArrayOf(5, 5, 5), intArrayOf(12, 12, 12, 12, 12, 12), intArrayOf(1, 1, 1, 7), intArrayOf(1, 1, 7, 1), intArrayOf(8, 9, 9, 8, 8, 9), intArrayOf(8, 8, 9, 8, 9, 9), intArrayOf(10, 10, 11, 11, 11, 10), intArrayOf(12, 12, 12, 12))
        @JvmField
        val pathBlendings = arrayOf(BooleanArray(4), booleanArrayOf(false, true, true, false), booleanArrayOf(true, false, true, false), booleanArrayOf(true, false, true, false), booleanArrayOf(false, false, true, false), booleanArrayOf(false, false, true, false), booleanArrayOf(true, false, true, false), booleanArrayOf(true, false, false, true), booleanArrayOf(true, false, false, true), booleanArrayOf(true, true, false, false), BooleanArray(4), booleanArrayOf(false, true, false, true), BooleanArray(4))
        @JvmField
        val anIntArrayArray3833 = arrayOf(intArrayOf(0, 1, 2, 3), intArrayOf(1, -1, -1, 0), intArrayOf(-1, 2, -1, 0), intArrayOf(-1, 0, -1, 2), intArrayOf(0, 1, -1, 2), intArrayOf(1, 2, -1, 0), intArrayOf(-1, 4, -1, 1), intArrayOf(-1, 3, 4, -1), intArrayOf(-1, 0, 2, -1), intArrayOf(-1, -1, 2, 0), intArrayOf(0, 2, 5, 3), intArrayOf(0, -1, 6, -1), intArrayOf(0, 1, 2, 3))
        @JvmField
        val anIntArrayArray3775 = arrayOf(intArrayOf(0, 2, 4, 6), intArrayOf(6, 0, 2, 3, 5, 3), intArrayOf(6, 0, 2, 4), intArrayOf(2, 5, 6, 1), intArrayOf(0, 2, 6), intArrayOf(6, 0, 2), intArrayOf(5, 6, 0, 1, 2, 4), intArrayOf(7, 7, 1, 2, 4, 6), intArrayOf(2, 4, 4, 7), intArrayOf(6, 6, 4, 0, 1, 1, 3, 3), intArrayOf(0, 2, 2, 6, 6, 4), intArrayOf(0, 2, 2, 3, 7, 0, 4, 3), intArrayOf(0, 2, 4, 6))
        @JvmField
        val anIntArrayArray3821 = arrayOf(intArrayOf(2, 4, 6, 0), intArrayOf(0, 2, 3, 5, 6, 4), intArrayOf(0, 1, 4, 5), intArrayOf(4, 6, 0, 2), intArrayOf(2, 4, 0), intArrayOf(0, 2, 4), intArrayOf(6, 0, 1, 2, 4, 5), intArrayOf(0, 1, 2, 4, 6, 7), intArrayOf(4, 7, 6, 0), intArrayOf(0, 8, 6, 1, 9, 2, 9, 4), intArrayOf(2, 9, 4, 0, 8, 6), intArrayOf(2, 11, 3, 7, 10, 10, 6, 6), intArrayOf(2, 4, 6, 0))
        @JvmField
        val anIntArrayArray3836 = arrayOf(intArrayOf(12, 12, 12, 12), intArrayOf(12, 12, 12, 12, 12, 5), intArrayOf(5, 5, 1, 1), intArrayOf(5, 1, 1, 5), intArrayOf(5, 5, 5), intArrayOf(5, 5, 5), intArrayOf(12, 12, 12, 12, 12, 12), intArrayOf(1, 12, 12, 12, 12, 12), intArrayOf(1, 1, 7, 1), intArrayOf(8, 9, 9, 8, 8, 3, 1, 9), intArrayOf(8, 8, 9, 8, 9, 9), intArrayOf(10, 10, 11, 11, 11, 7, 3, 7), intArrayOf(12, 12, 12, 12))

        @JvmStatic
        fun calculateLocUid(gameObject: GameObject, x: Int, y: Int): Long {
            val loctype = Resource.mapRegion!!.getLoctypeList().list(gameObject.id)
            var uid = x.toLong() or (y.toLong() shl 7) or (gameObject.shape.toLong() shl 14) or (gameObject.getRotation().toLong() shl 20) or 0x40000000
            if (loctype.hasActions == 0) {
                uid = uid or 0x8000000000000000u.toLong()
            }
            if (loctype.supportsItems == 1) {
                uid = uid or 0x400000L
            }
            uid = uid or (gameObject.id.toLong() shl 32)
            return uid
        }
    }
}
