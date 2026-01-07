package com.jagex.game.runetek5.scene.ground

import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.scene.ground.GroundFunctionMask.*
import com.jagex.game.runetek5.scene.tile.*
import com.jagex.game.runetek5.scene.tile.TileFunctionMask.COMPLETELY_FLAT
import com.jagex.game.runetek5.scene.tile.TileFunctionMask.DISABLE_TEXTURES
import com.jagex.game.runetek5.scene.tile.TileFunctionMask.HD_LIGHT
import com.jagex.game.runetek5.scene.tile.TileFunctionMask.TEXTURE_MOVEMENT
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.toolkit.safemode.RasterizerSM
import com.jagex.game.runetek5.toolkit.safemode.ThreadResourceSM
import com.jagex.game.runetek5.utils.ColorUtil

/**
 * Software-mode ground renderer for safe mode rendering.
 * Handles tile rendering, shadows, and minimap floor rendering.
 */
class GroundSM(
    @JvmField val toolkit: SafeModeToolkit,
    @JvmField val groundFunctionMask: Int,
    regionSizeX: Int,
    regionSizeY: Int,
    otherTileHeights: Array<IntArray>,
    tileHeights: Array<IntArray>
) : Ground(regionSizeX, regionSizeY, 512, otherTileHeights) {

    /** Tiles that render as squares [MIN] */
    @JvmField var scenePlainTiles: Array<Array<PlainTile?>>? = null
    @JvmField var sceneShapedTiles: Array<Array<ShapedTile?>>? = null

    /** Tiles that blend colors from overlay / underlay together [LOW / MED] */
    @JvmField var plainBlendedTiles: Array<Array<PlainTileBlended?>>? = null
    @JvmField var shapedBlendedTiles: Array<Array<ShapedTileBlended?>>? = null

    /** Tiles that blend colors and textures from overlay / underlay together [HIGH] */
    @JvmField var blendedTexturedTiles: Array<Array<TextureBlendedTile?>>? = null

    /** Color adjustment */
    @JvmField var directionalTileLight: Array<ByteArray>?
    @JvmField var tileShadowing: Array<ByteArray>?

    /** Projection matrix */
    @JvmField var vertexXA: Float = 0f
    @JvmField var vertexYA: Float = 0f
    @JvmField var vertexZNearA: Float = 0f
    @JvmField var vertexZFarA: Float = 0f
    @JvmField var vertexXB: Float = 0f
    @JvmField var vertexYB: Float = 0f
    @JvmField var vertexZNearB: Float = 0f
    @JvmField var vertexZFarB: Float = 0f
    @JvmField var vertexXC: Float = 0f
    @JvmField var vertexYC: Float = 0f
    @JvmField var vertexZNearC: Float = 0f
    @JvmField var vertexZFarC: Float = 0f
    @JvmField var vertexXD: Float = 0f
    @JvmField var vertexYD: Float = 0f
    @JvmField var vertexZNearD: Float = 0f
    @JvmField var vertexZFarD: Float = 0f

    init {
        directionalTileLight = Array(regionSizeX + 1) { ByteArray(regionSizeY + 1) }
        val baseLight = toolkit.lightIntensity shr 9
        for (y in 1 until regionSizeY) {
            for (x in 1 until regionSizeX) {
                val heightDifferenceX = tileHeights[x + 1][y] - tileHeights[x - 1][y]
                val heightDifferenceY = tileHeights[x][y + 1] - tileHeights[x][y - 1]
                val normalLength = Math.sqrt((512 * 512 + heightDifferenceX * heightDifferenceX + heightDifferenceY * heightDifferenceY).toDouble()).toInt()
                val normalizedNormalX = (heightDifferenceX shl 8) / normalLength
                val normalizedNormalY = 512 * -512 / normalLength
                val normalizedNormalZ = (heightDifferenceY shl 8) / normalLength
                var directionalLightIntensity = baseLight + (toolkit.lightX * normalizedNormalX + toolkit.lightY * normalizedNormalY + toolkit.lightZ * normalizedNormalZ shr 17)
                directionalLightIntensity = directionalLightIntensity shr 1
                directionalLightIntensity = when {
                    directionalLightIntensity < 2 -> 2
                    directionalLightIntensity > 126 -> 126
                    else -> directionalLightIntensity
                }
                directionalTileLight!![x][y] = directionalLightIntensity.toByte()
            }
        }
        tileShadowing = Array(regionSizeX + 1) { ByteArray(regionSizeY + 1) }
    }

    override fun setTileShadow(tileX: Int, tileY: Int, displacement: Int) {
        if (tileShadowing!![tileX][tileY] < displacement) {
            tileShadowing!![tileX][tileY] = displacement.toByte()
        }
    }

    override fun calculateBlockTiles(
        x: Int,
        y: Int,
        offsetsX: IntArray,
        heightOffsets: IntArray,
        offsetsY: IntArray,
        unused: IntArray,
        trianglesA: IntArray,
        trianglesB: IntArray,
        trianglesC: IntArray,
        primaryColors: IntArray,
        secondaryColors: IntArray,
        primaryMaterialIds: IntArray,
        secondaryMaterialIds: IntArray,
        waterTile: WaterTile?
    ) {
        if (scenePlainTiles == null) {
            scenePlainTiles = Array(width) { arrayOfNulls(length) }
            sceneShapedTiles = Array(width) { arrayOfNulls(length) }
        } else if (plainBlendedTiles != null || blendedTexturedTiles != null) {
            throw IllegalStateException()
        }

        var offsetMatchesTileUnits = false
        if (primaryColors.size == 2 && trianglesA.size == 2 &&
            (primaryColors[0] == primaryColors[1] || primaryMaterialIds[0] != -1 && primaryMaterialIds[1] == primaryMaterialIds[0])) {
            offsetMatchesTileUnits = true
            for (triangleIndex in 1 until 2) {
                val offX = offsetsX[trianglesA[triangleIndex]]
                val offY = offsetsY[trianglesA[triangleIndex]]
                if (offX != 0 && offX != tileUnits || offY != 0 && offY != tileUnits) {
                    offsetMatchesTileUnits = false
                    break
                }
            }
        }

        if (!offsetMatchesTileUnits) {
            val shapedTile = ShapedTile()
            val offsetCount = offsetsX.size.toShort()
            val colorCount = primaryColors.size.toShort()
            shapedTile.count = offsetCount
            shapedTile.light = ShortArray(offsetCount.toInt())
            shapedTile.offsetX = ShortArray(offsetCount.toInt())
            shapedTile.offsetZ = ShortArray(offsetCount.toInt())
            shapedTile.offsetY = ShortArray(offsetCount.toInt())

            for (index in 0 until offsetCount) {
                val offsetX = offsetsX[index]
                val offsetY = offsetsY[index]
                val light = when {
                    offsetX == 0 && offsetY == 0 ->
                        (directionalTileLight!![x][y] - tileShadowing!![x][y]).toShort()
                    offsetX == 0 && offsetY == tileUnits ->
                        (directionalTileLight!![x][y + 1] - tileShadowing!![x][y + 1]).toShort()
                    offsetX == tileUnits && offsetY == tileUnits ->
                        (directionalTileLight!![x + 1][y + 1] - tileShadowing!![x + 1][y + 1]).toShort()
                    offsetX == tileUnits && offsetY == 0 ->
                        (directionalTileLight!![x + 1][y] - tileShadowing!![x + 1][y]).toShort()
                    else -> {
                        val lightA = (directionalTileLight!![x + 1][y] - tileShadowing!![x + 1][y]) * offsetX +
                                    (directionalTileLight!![x][y] - tileShadowing!![x][y]) * (tileUnits - offsetX)
                        val lightB = (directionalTileLight!![x][y + 1] - tileShadowing!![x][y + 1]) * (tileUnits - offsetX) +
                                    (directionalTileLight!![x + 1][y + 1] - tileShadowing!![x + 1][y + 1]) * offsetX
                        (lightA * (tileUnits - offsetY) + lightB * offsetY shr tileScale * 2).toShort()
                    }
                }

                val sceneX = offsetX + (x shl tileScale)
                val sceneY = offsetY + (y shl tileScale)
                shapedTile.offsetX!![index] = offsetX.toShort()
                shapedTile.offsetY!![index] = offsetY.toShort()
                shapedTile.offsetZ!![index] = (getAverageHeight(sceneX, sceneY) + (heightOffsets[index])).toShort()

                shapedTile.light!![index] = if (light < 2) 2 else light
            }

            var texturedFloor = false
            var triangleCount = 0
            for (index in 0 until colorCount) {
                if (primaryColors[index] >= 0 || secondaryColors[index] >= 0) {
                    triangleCount++
                }
                val materialId = primaryMaterialIds[index]
                if (materialId != -1) {
                    val material = toolkit.materialProvider!!.getMaterial(materialId)
                    if (!material.detailsOnly) {
                        texturedFloor = true
                        if (flagMatch(material.effectId.toInt()) || material.textureSpeedU.toInt() != 0 || material.textureSpeedV.toInt() != 0) {
                            shapedTile.functionMask = (shapedTile.functionMask.toInt() or TEXTURE_MOVEMENT).toByte()
                        }
                    }
                }
            }

            shapedTile.primaryColors = IntArray(triangleCount)
            shapedTile.secondaryColors = IntArray(triangleCount)
            shapedTile.triangleA = ShortArray(triangleCount)
            shapedTile.triangleB = ShortArray(triangleCount)
            shapedTile.triangleC = ShortArray(triangleCount)

            if (texturedFloor) {
                shapedTile.primaryMaterialIds = ShortArray(triangleCount)
                shapedTile.secondaryMaterialIds = ShortArray(triangleCount)
            }

            for (triangleIndex in 0 until colorCount) {
                if (primaryColors[triangleIndex] >= 0 || secondaryColors[triangleIndex] >= 0) {
                    shapedTile.primaryColors!![shapedTile.triangleCount.toInt()] = if (primaryColors[triangleIndex] >= 0) {
                        ColorUtil.adjustHsv(primaryColors[triangleIndex]).toInt()
                    } else {
                        -1
                    }

                    shapedTile.secondaryColors!![shapedTile.triangleCount.toInt()] = if (secondaryColors[triangleIndex] != -1) {
                        ColorUtil.adjustHsv(secondaryColors[triangleIndex]).toInt()
                    } else {
                        -1
                    }

                    shapedTile.triangleA!![shapedTile.triangleCount.toInt()] = trianglesA[triangleIndex].toShort()
                    shapedTile.triangleB!![shapedTile.triangleCount.toInt()] = trianglesB[triangleIndex].toShort()
                    shapedTile.triangleC!![shapedTile.triangleCount.toInt()] = trianglesC[triangleIndex].toShort()

                    if (texturedFloor) {
                        if (primaryMaterialIds[triangleIndex] != -1 && !toolkit.materialProvider!!.getMaterial(primaryMaterialIds[triangleIndex]).detailsOnly) {
                            shapedTile.primaryMaterialIds!![shapedTile.triangleCount.toInt()] = primaryMaterialIds[triangleIndex].toShort()
                            shapedTile.secondaryMaterialIds!![shapedTile.triangleCount.toInt()] = secondaryMaterialIds[triangleIndex].toShort()
                        } else {
                            shapedTile.primaryMaterialIds!![shapedTile.triangleCount.toInt()] = -1
                        }
                    }
                    shapedTile.triangleCount++
                }
            }
            sceneShapedTiles!![x][y] = shapedTile
        } else if (primaryColors[0] >= 0 || secondaryColors[0] >= 0) {
            val plainTile = PlainTile()
            val color = primaryColors[0]
            val materialId = primaryMaterialIds[0]

            plainTile.color = ColorUtil.repackHsl(ColorUtil.adjustHsv(secondaryColors[0]).toInt(), directionalTileLight!![x][y] - tileShadowing!![x][y])
            if (color == -1) {
                plainTile.functionMask = (plainTile.functionMask.toInt() or HD_LIGHT).toByte()
            }

            if (tileHeights[x][y] == tileHeights[x + 1][y] &&
                tileHeights[x][y] == tileHeights[x + 1][y + 1] &&
                tileHeights[x][y + 1] == tileHeights[x][y]) {
                plainTile.functionMask = (plainTile.functionMask.toInt() or COMPLETELY_FLAT).toByte()
            }

            var material: MaterialType? = null
            if (materialId != -1) {
                material = toolkit.materialProvider!!.getMaterial(materialId)
            }

            if (material != null && (plainTile.functionMask.toInt() and HD_LIGHT) == 0 && !material.detailsOnly) {
                plainTile.colorSouthWest = (directionalTileLight!![x][y] - tileShadowing!![x][y]).toShort()
                plainTile.colorSouthEast = (directionalTileLight!![x + 1][y] - tileShadowing!![x + 1][y]).toShort()
                plainTile.colorNorthEast = (directionalTileLight!![x + 1][y + 1] - tileShadowing!![x + 1][y + 1]).toShort()
                plainTile.colorNorthWest = (directionalTileLight!![x][y + 1] - tileShadowing!![x][y + 1]).toShort()
                plainTile.materialId = materialId.toShort()
                if (flagMatch(material.effectId.toInt()) || material.textureSpeedU.toInt() != 0 || material.textureSpeedV.toInt() != 0) {
                    plainTile.functionMask = (plainTile.functionMask.toInt() or TEXTURE_MOVEMENT).toByte()
                }
            } else {
                val hsl = ColorUtil.adjustHsv(color).toInt()
                plainTile.colorSouthWest = ColorUtil.repackHsl(hsl, directionalTileLight!![x][y] - tileShadowing!![x][y]).toShort()
                plainTile.colorSouthEast = ColorUtil.repackHsl(hsl, directionalTileLight!![x + 1][y] - tileShadowing!![x + 1][y]).toShort()
                plainTile.colorNorthEast = ColorUtil.repackHsl(hsl, directionalTileLight!![x + 1][y + 1] - tileShadowing!![x + 1][y + 1]).toShort()
                plainTile.colorNorthWest = ColorUtil.repackHsl(hsl, directionalTileLight!![x][y + 1] - tileShadowing!![x][y + 1]).toShort()
                plainTile.materialId = -1
            }
            scenePlainTiles!![x][y] = plainTile
        }
    }

    override fun addBlendedTile(
        x: Int,
        y: Int,
        verticesX: IntArray?,
        heightOffsets: IntArray?,
        verticesY: IntArray?,
        faceAlphas: IntArray?,
        primaryColors: IntArray,
        materialColors: IntArray?,
        materialIds: IntArray,
        textureScales: IntArray,
        waterTile: WaterTile?,
        hasShadows: Boolean
    ) {
        val hasTextures = (groundFunctionMask and DISABLE_TEXTURES) == 0

        if (plainBlendedTiles == null && !hasTextures) {
            plainBlendedTiles = Array(width) { arrayOfNulls(length) }
            shapedBlendedTiles = Array(width) { arrayOfNulls(length) }
        } else if (blendedTexturedTiles == null && hasTextures) {
            blendedTexturedTiles = Array(width) { arrayOfNulls(length) }
        } else if (scenePlainTiles != null) {
            throw IllegalStateException()
        }

        if (verticesX == null || verticesX.isEmpty()) return

        val adjustedPrimaryColors = primaryColors.clone()
        for (index in adjustedPrimaryColors.indices) {
            adjustedPrimaryColors[index] = if (adjustedPrimaryColors[index] == -1) {
                0
            } else {
                ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(adjustedPrimaryColors[index]).toInt() and 0xffff] shl 8 or 0xff
            }
        }

        val adjustedMaterialColors = materialColors?.clone()
        if (adjustedMaterialColors != null) {
            for (index in adjustedMaterialColors.indices) {
                adjustedMaterialColors[index] = if (adjustedMaterialColors[index] == -1) {
                    0
                } else {
                    ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(adjustedMaterialColors[index]).toInt() and 0xffff] shl 8 or 0xff
                }
            }
        }

        if (hasTextures) {
            val blendedTexturedTile = TextureBlendedTile()
            blendedTexturedTile.verticeCount = verticesX.size.toShort()
            blendedTexturedTile.faceCount = (verticesX.size / 3).toShort()
            blendedTexturedTile.positionX = ShortArray(blendedTexturedTile.verticeCount.toInt())
            blendedTexturedTile.tileDrawHeight = ShortArray(blendedTexturedTile.verticeCount.toInt())
            blendedTexturedTile.positionZ = ShortArray(blendedTexturedTile.verticeCount.toInt())
            blendedTexturedTile.colors = IntArray(blendedTexturedTile.verticeCount.toInt())
            blendedTexturedTile.materialId = ShortArray(blendedTexturedTile.verticeCount.toInt())
            blendedTexturedTile.aShortArray2254 = ShortArray(blendedTexturedTile.verticeCount.toInt())
            blendedTexturedTile.lightIntensity = ByteArray(blendedTexturedTile.verticeCount.toInt())
            if (faceAlphas != null) {
                blendedTexturedTile.alpha = ShortArray(blendedTexturedTile.verticeCount.toInt())
            }

            for (vertice in 0 until blendedTexturedTile.verticeCount) {
                val verticeX = verticesX[vertice]
                val verticeY = verticesY!![vertice]
                var lightIntensity = when {
                    verticeX == 0 && verticeY == 0 ->
                        directionalTileLight!![x][y] - tileShadowing!![x][y]
                    verticeX == 0 && verticeY == tileUnits ->
                        directionalTileLight!![x][y + 1] - tileShadowing!![x][y + 1]
                    verticeX == tileUnits && verticeY == tileUnits ->
                        directionalTileLight!![x + 1][y + 1] - tileShadowing!![x + 1][y + 1]
                    verticeX == tileUnits && verticeY == 0 ->
                        directionalTileLight!![x + 1][y] - tileShadowing!![x + 1][y]
                    else -> {
                        val lightNorth = (directionalTileLight!![x + 1][y] - tileShadowing!![x + 1][y]) * verticeX +
                                        (directionalTileLight!![x][y] - tileShadowing!![x][y]) * (tileUnits - verticeX)
                        val lightSouth = (directionalTileLight!![x][y + 1] - tileShadowing!![x][y + 1]) * (tileUnits - verticeX) +
                                        (directionalTileLight!![x + 1][y + 1] - tileShadowing!![x + 1][y + 1]) * verticeX
                        lightNorth * (tileUnits - verticeY) + lightSouth * verticeY shr tileScale * 2
                    }
                }

                val sceneX = verticeX + (x shl tileScale)
                val sceneY = verticeY + (y shl tileScale)
                blendedTexturedTile.positionX!![vertice] = verticeX.toShort()
                blendedTexturedTile.positionZ!![vertice] = verticeY.toShort()
                blendedTexturedTile.tileDrawHeight!![vertice] = (getAverageHeight(sceneX, sceneY) + (heightOffsets?.get(vertice) ?: 0)).toShort()

                if (lightIntensity < 0) lightIntensity = 0

                if (adjustedPrimaryColors[vertice] == 0) {
                    blendedTexturedTile.colors!![vertice] = 0
                } else {
                    var alpha = 0
                    if (faceAlphas != null) {
                        blendedTexturedTile.alpha!![vertice] = faceAlphas[vertice].toShort()
                        if (waterTile!!.scale != 0) {
                            alpha = faceAlphas[vertice] * 255 / waterTile.scale
                            alpha = alpha.coerceIn(0, 255)
                        }
                    }

                    var baseColor = 0xff000000.toInt()
                    if (materialIds[vertice] != -1 && flagMatch(toolkit.materialProvider!!.getMaterial(materialIds[vertice]).effectId.toInt())) {
                        baseColor = -1694498816
                    }
                    blendedTexturedTile.colors!![vertice] = baseColor or ColorUtil.blendColorsWithAlpha(
                        ColorUtil.brighten(adjustedPrimaryColors[vertice] shr 8, lightIntensity),
                        waterTile!!.color,
                        alpha
                    )
                }

                if (adjustedMaterialColors != null) {
                    blendedTexturedTile.lightIntensity!![vertice] = lightIntensity.toByte()
                }

                blendedTexturedTile.materialId!![vertice] = materialIds[vertice].toShort()
                blendedTexturedTile.aShortArray2254!![vertice] = textureScales[vertice].toShort()
            }

            if (adjustedMaterialColors != null) {
                blendedTexturedTile.materialColors = IntArray(blendedTexturedTile.faceCount.toInt())
            }

            for (face in 0 until blendedTexturedTile.faceCount) {
                val vertice = face * 3
                if (adjustedMaterialColors != null && adjustedMaterialColors[vertice] != 0) {
                    blendedTexturedTile.materialColors!![face] = 0xff000000.toInt() or (adjustedMaterialColors[vertice] shr 8)
                }
            }

            blendedTexturedTiles!![x][y] = blendedTexturedTile
        } else {
            var fullColorMatch = true
            var indexSouthWest = -1
            var indexSouthEast = -1
            var indexNorthEast = -1
            var indexNorthWest = -1

            if (verticesX.size != 6) {
                fullColorMatch = false
            } else {
                for (index in 0 until 6) {
                    when {
                        verticesX[index] == 0 && verticesY!![index] == 0 -> {
                            if (indexSouthWest != -1 && adjustedPrimaryColors[indexSouthWest] != adjustedPrimaryColors[index]) {
                                fullColorMatch = false
                                break
                            }
                            indexSouthWest = index
                        }
                        verticesX[index] == tileUnits && verticesY!![index] == 0 -> {
                            if (indexSouthEast != -1 && adjustedPrimaryColors[indexSouthEast] != adjustedPrimaryColors[index]) {
                                fullColorMatch = false
                                break
                            }
                            indexSouthEast = index
                        }
                        verticesX[index] == tileUnits && verticesY!![index] == tileUnits -> {
                            if (indexNorthEast != -1 && adjustedPrimaryColors[indexNorthEast] != adjustedPrimaryColors[index]) {
                                fullColorMatch = false
                                break
                            }
                            indexNorthEast = index
                        }
                        verticesX[index] == 0 && verticesY!![index] == tileUnits -> {
                            if (indexNorthWest != -1 && adjustedPrimaryColors[indexNorthWest] != adjustedPrimaryColors[index]) {
                                fullColorMatch = false
                                break
                            }
                            indexNorthWest = index
                        }
                    }
                }

                if (indexSouthWest == -1 || indexSouthEast == -1 || indexNorthEast == -1 || indexNorthWest == -1) {
                    fullColorMatch = false
                }

                if (fullColorMatch && heightOffsets != null) {
                    for (z in 0 until 4) {
                        if (heightOffsets[z] != 0) {
                            fullColorMatch = false
                            break
                        }
                    }
                }

                if (fullColorMatch) {
                    for (z in 1 until 4) {
                        if (verticesX[z] != verticesX[0] && verticesX[z] != verticesX[0] + tileUnits && verticesX[0] - tileUnits != verticesX[z]) {
                            fullColorMatch = false
                            break
                        }
                        if (verticesY!![z] != verticesY[0] && verticesY[z] != verticesY[0] + tileUnits && verticesY[z] != verticesY[0] - tileUnits) {
                            fullColorMatch = false
                            break
                        }
                    }
                }
            }

            if (fullColorMatch) {
                val blendedTile = PlainTileBlended()
                val primaryColor = adjustedPrimaryColors[0]
                val materialId = materialIds[0]

                if (adjustedMaterialColors != null) {
                    blendedTile.materialColor = adjustedMaterialColors[0] shr 8
                    if (primaryColor == 0) {
                        blendedTile.functionMask = (blendedTile.functionMask.toInt() or HD_LIGHT).toByte()
                    }
                } else if (primaryColor == 0) {
                    return
                }

                if (tileHeights[x][y] == tileHeights[x + 1][y] &&
                    tileHeights[x][y] == tileHeights[x + 1][y + 1] &&
                    tileHeights[x][y + 1] == tileHeights[x][y]) {
                    blendedTile.functionMask = (blendedTile.functionMask.toInt() or COMPLETELY_FLAT).toByte()
                }

                if (materialId != -1 && (blendedTile.functionMask.toInt() and HD_LIGHT) == 0 && !toolkit.materialProvider!!.getMaterial(materialId).detailsOnly) {
                    var alpha = if (faceAlphas != null && waterTile!!.scale != 0) {
                        (faceAlphas[indexSouthWest] * 255 / waterTile.scale).coerceIn(0, 255)
                    } else 0

                    blendedTile.colorSouthWest = ColorUtil.blendColorsWithAlpha(
                        ColorUtil.brighten(adjustedPrimaryColors[indexSouthWest] shr 8, directionalTileLight!![x][y] - tileShadowing!![x][y]),
                        waterTile!!.color, alpha
                    )
                    if (blendedTile.materialColor != 0) {
                        blendedTile.colorSouthWest = blendedTile.colorSouthWest or ((255 - (directionalTileLight!![x][y] - tileShadowing!![x][y])) shl 25)
                    }

                    alpha = if (faceAlphas != null && waterTile.scale != 0) {
                        (faceAlphas[indexSouthEast] * 255 / waterTile.scale).coerceIn(0, 255)
                    } else 0

                    blendedTile.colorSouthEast = ColorUtil.blendColorsWithAlpha(
                        ColorUtil.brighten(adjustedPrimaryColors[indexSouthEast] shr 8, directionalTileLight!![x + 1][y] - tileShadowing!![x + 1][y]),
                        waterTile.color, alpha
                    )
                    if (blendedTile.materialColor != 0) {
                        blendedTile.colorSouthEast = blendedTile.colorSouthEast or ((255 - (directionalTileLight!![x + 1][y] - tileShadowing!![x + 1][y])) shl 25)
                    }

                    alpha = if (faceAlphas != null && waterTile.scale != 0) {
                        (faceAlphas[indexNorthEast] * 255 / waterTile.scale).coerceIn(0, 255)
                    } else 0

                    blendedTile.colorNorthEast = ColorUtil.blendColorsWithAlpha(
                        ColorUtil.brighten(adjustedPrimaryColors[indexNorthEast] shr 8, directionalTileLight!![x + 1][y + 1] - tileShadowing!![x + 1][y + 1]),
                        waterTile.color, alpha
                    )
                    if (blendedTile.materialColor != 0) {
                        blendedTile.colorNorthEast = blendedTile.colorNorthEast or ((255 - (directionalTileLight!![x + 1][y + 1] - tileShadowing!![x + 1][y + 1])) shl 25)
                    }

                    alpha = if (faceAlphas != null && waterTile.scale != 0) {
                        (faceAlphas[indexNorthWest] * 255 / waterTile.scale).coerceIn(0, 255)
                    } else 0

                    blendedTile.colorNorthWest = ColorUtil.blendColorsWithAlpha(
                        ColorUtil.brighten(adjustedPrimaryColors[indexNorthWest] shr 8, directionalTileLight!![x][y + 1] - tileShadowing!![x][y + 1]),
                        waterTile.color, alpha
                    )
                    blendedTile.materialId = materialId.toShort()
                } else {
                    var alpha = if (faceAlphas != null && waterTile!!.scale != 0) {
                        (faceAlphas[indexSouthWest] * 255 / waterTile.scale).coerceIn(0, 255)
                    } else 0

                    blendedTile.colorSouthWest = ColorUtil.blendColorsWithAlpha(
                        ColorUtil.brighten(adjustedPrimaryColors[indexSouthWest] shr 8, directionalTileLight!![x][y] - tileShadowing!![x][y]),
                        waterTile!!.color, alpha
                    )
                    if (blendedTile.materialColor != 0) {
                        blendedTile.colorSouthWest = blendedTile.colorSouthWest or ((255 - (directionalTileLight!![x][y] - tileShadowing!![x][y])) shl 25)
                    }

                    alpha = if (faceAlphas != null && waterTile.scale != 0) {
                        (faceAlphas[indexSouthEast] * 255 / waterTile.scale).coerceIn(0, 255)
                    } else 0

                    blendedTile.colorSouthEast = ColorUtil.blendColorsWithAlpha(
                        ColorUtil.brighten(adjustedPrimaryColors[indexSouthEast] shr 8, directionalTileLight!![x + 1][y] - tileShadowing!![x + 1][y]),
                        waterTile.color, alpha
                    )
                    if (blendedTile.materialColor != 0) {
                        blendedTile.colorSouthEast = blendedTile.colorSouthEast or ((255 - (directionalTileLight!![x + 1][y] - tileShadowing!![x + 1][y])) shl 25)
                    }

                    alpha = if (faceAlphas != null && waterTile.scale != 0) {
                        (faceAlphas[indexNorthEast] * 255 / waterTile.scale).coerceIn(0, 255)
                    } else 0

                    blendedTile.colorNorthEast = ColorUtil.blendColorsWithAlpha(
                        ColorUtil.brighten(adjustedPrimaryColors[indexNorthEast] shr 8, directionalTileLight!![x + 1][y + 1] - tileShadowing!![x + 1][y + 1]),
                        waterTile.color, alpha
                    )
                    if (blendedTile.materialColor != 0) {
                        blendedTile.colorNorthEast = blendedTile.colorNorthEast or ((255 - (directionalTileLight!![x + 1][y + 1] - tileShadowing!![x + 1][y + 1])) shl 25)
                    }

                    alpha = if (faceAlphas != null && waterTile.scale != 0) {
                        (faceAlphas[indexNorthWest] * 255 / waterTile.scale).coerceIn(0, 255)
                    } else 0

                    blendedTile.colorNorthWest = ColorUtil.blendColorsWithAlpha(
                        ColorUtil.brighten(adjustedPrimaryColors[indexNorthWest] shr 8, directionalTileLight!![x][y + 1] - tileShadowing!![x][y + 1]),
                        waterTile.color, alpha
                    )
                    if (blendedTile.materialColor != 0) {
                        blendedTile.colorNorthWest = blendedTile.colorNorthWest or ((255 - (directionalTileLight!![x][y + 1] - tileShadowing!![x][y + 1])) shl 25)
                    }
                    blendedTile.materialId = -1
                }

                if (faceAlphas != null) {
                    blendedTile.alphaNorthEast = faceAlphas[indexNorthEast].toShort()
                    blendedTile.alphaNorthWest = faceAlphas[indexNorthWest].toShort()
                    blendedTile.alphaSouthEast = faceAlphas[indexSouthEast].toShort()
                    blendedTile.alphaSouthWest = faceAlphas[indexSouthWest].toShort()
                }

                plainBlendedTiles!![x][y] = blendedTile
            } else {
                val shapedBlendedTile = ShapedTileBlended()
                shapedBlendedTile.verticeCount = verticesX.size.toShort()
                shapedBlendedTile.faceCount = (verticesX.size / 3).toShort()
                shapedBlendedTile.positionX = ShortArray(shapedBlendedTile.verticeCount.toInt())
                shapedBlendedTile.drawHeight = ShortArray(shapedBlendedTile.verticeCount.toInt())
                shapedBlendedTile.positionY = ShortArray(shapedBlendedTile.verticeCount.toInt())
                shapedBlendedTile.colors = IntArray(shapedBlendedTile.verticeCount.toInt())
                if (faceAlphas != null) {
                    shapedBlendedTile.alpha = ShortArray(shapedBlendedTile.verticeCount.toInt())
                }

                for (index in 0 until shapedBlendedTile.verticeCount) {
                    val verticeX = verticesX[index]
                    val verticeY = verticesY!![index]
                    var lightShadowAdjustment = when {
                        verticeX == 0 && verticeY == 0 ->
                            directionalTileLight!![x][y] - tileShadowing!![x][y]
                        verticeX == 0 && verticeY == tileUnits ->
                            directionalTileLight!![x][y + 1] - tileShadowing!![x][y + 1]
                        verticeX == tileUnits && verticeY == tileUnits ->
                            directionalTileLight!![x + 1][y + 1] - tileShadowing!![x + 1][y + 1]
                        verticeX == tileUnits && verticeY == 0 ->
                            directionalTileLight!![x + 1][y] - tileShadowing!![x + 1][y]
                        else -> {
                            val sceneX = (directionalTileLight!![x + 1][y] - tileShadowing!![x + 1][y]) * verticeX +
                                        (directionalTileLight!![x][y] - tileShadowing!![x][y]) * (tileUnits - verticeX)
                            val sceneY = (directionalTileLight!![x][y + 1] - tileShadowing!![x][y + 1]) * (tileUnits - verticeX) +
                                        (directionalTileLight!![x + 1][y + 1] - tileShadowing!![x + 1][y + 1]) * verticeX
                            sceneX * (tileUnits - verticeY) + sceneY * verticeY shr tileScale * 2
                        }
                    }

                    val sceneX = verticeX + (x shl tileScale)
                    val sceneY = verticeY + (y shl tileScale)
                    shapedBlendedTile.positionX!![index] = verticeX.toShort()
                    shapedBlendedTile.positionY!![index] = verticeY.toShort()
                    shapedBlendedTile.drawHeight!![index] = (getAverageHeight(sceneX, sceneY) + (heightOffsets?.get(index) ?: 0)).toShort()

                    if (lightShadowAdjustment < 0) lightShadowAdjustment = 0

                    if (adjustedPrimaryColors[index] == 0) {
                        shapedBlendedTile.colors!![index] = if (adjustedMaterialColors != null) {
                            lightShadowAdjustment shl 25
                        } else 0
                    } else {
                        var alpha = 0
                        if (faceAlphas != null) {
                            shapedBlendedTile.alpha!![index] = faceAlphas[index].toShort()
                            if (waterTile!!.scale != 0) {
                                alpha = (faceAlphas[index] * 255 / waterTile.scale).coerceIn(0, 255)
                            }
                        }
                        shapedBlendedTile.colors!![index] = ColorUtil.blendColorsWithAlpha(
                            ColorUtil.brighten(adjustedPrimaryColors[index] shr 8, lightShadowAdjustment),
                            waterTile!!.color, alpha
                        )
                        if (adjustedMaterialColors != null) {
                            shapedBlendedTile.colors!![index] = shapedBlendedTile.colors!![index] or (lightShadowAdjustment shl 25)
                        }
                    }
                }

                var textured = false
                for (face in 0 until shapedBlendedTile.faceCount) {
                    if (materialIds[face * 3] != -1 && !toolkit.materialProvider!!.getMaterial(materialIds[face * 3]).detailsOnly) {
                        textured = true
                    }
                }

                if (adjustedMaterialColors != null) {
                    shapedBlendedTile.materialColors = IntArray(shapedBlendedTile.faceCount.toInt())
                }

                if (textured) {
                    shapedBlendedTile.materialIds = ShortArray(shapedBlendedTile.faceCount.toInt())
                    shapedBlendedTile.aShortArray2422 = ShortArray(shapedBlendedTile.faceCount.toInt())
                }

                for (face in 0 until shapedBlendedTile.faceCount) {
                    val verticeA = face * 3
                    if (adjustedMaterialColors != null && adjustedMaterialColors[verticeA] != 0) {
                        shapedBlendedTile.materialColors!![face] = adjustedMaterialColors[verticeA] shr 8
                    }

                    if (textured) {
                        val verticeB = verticeA + 1
                        val verticeC = verticeB + 1
                        var texturesEnabled = false
                        var detailsOnly = true

                        var materialId = materialIds[verticeA]
                        if (materialId != -1 && !toolkit.materialProvider!!.getMaterial(materialId).detailsOnly) {
                            texturesEnabled = true
                        } else {
                            detailsOnly = false
                        }

                        materialId = materialIds[verticeB]
                        if (materialId != -1 && !toolkit.materialProvider!!.getMaterial(materialId).detailsOnly) {
                            texturesEnabled = true
                        } else {
                            detailsOnly = false
                        }

                        materialId = materialIds[verticeC]
                        if (materialId != -1 && !toolkit.materialProvider!!.getMaterial(materialId).detailsOnly) {
                            texturesEnabled = true
                        } else {
                            detailsOnly = false
                        }

                        if (detailsOnly) {
                            shapedBlendedTile.materialIds!![face] = materialId.toShort()
                            shapedBlendedTile.aShortArray2422!![face] = textureScales[verticeA].toShort()
                        } else {
                            if (texturesEnabled) {
                                materialId = materialIds[verticeA]
                                if (materialId != -1 && !toolkit.materialProvider!!.getMaterial(materialId).detailsOnly) {
                                    shapedBlendedTile.colors!![verticeA] = ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(toolkit.materialProvider!!.getMaterial(materialId).colorHsl.toInt() and 0xffff).toInt() and 0xffff]
                                }

                                materialId = materialIds[verticeB]
                                if (materialId != -1 && !toolkit.materialProvider!!.getMaterial(materialId).detailsOnly) {
                                    shapedBlendedTile.colors!![verticeB] = ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(toolkit.materialProvider!!.getMaterial(materialId).colorHsl.toInt() and 0xffff).toInt() and 0xffff]
                                }

                                materialId = materialIds[verticeC]
                                if (materialId != -1 && !toolkit.materialProvider!!.getMaterial(materialId).detailsOnly) {
                                    shapedBlendedTile.colors!![verticeC] = ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(toolkit.materialProvider!!.getMaterial(materialId).colorHsl.toInt() and 0xffff).toInt() and 0xffff]
                                }
                            }
                            shapedBlendedTile.materialIds!![face] = -1
                        }
                    }
                }
                shapedBlendedTiles!![x][y] = shapedBlendedTile
            }
        }
    }

    override fun resetLight() {
        directionalTileLight = null
        tileShadowing = null
    }

    fun renderBlendedUntexturedTile(
        tileX: Int,
        tileY: Int,
        hasWaterUpdate: Boolean,
        thread: ThreadResourceSM,
        rasterizer: RasterizerSM,
        floatsX: FloatArray,
        floatsY: FloatArray,
        floatsZNear: FloatArray,
        floatsZFar: FloatArray,
        floatsAlpha: FloatArray,
        tileFlags: Int
    ) {
        val tile = plainBlendedTiles!![tileX][tileY]
        if (tile != null) {
            if ((tile.functionMask.toInt() and HD_LIGHT) == 0) {
                if (tileFlags != 0) {
                    if ((tile.functionMask.toInt() and TEXTURE_MOVEMENT) != 0) {
                        if ((tileFlags and COMPLETELY_FLAT) != 0) return
                    } else if ((tileFlags and HD_LIGHT) != 0) {
                        return
                    }
                }

                val startX = tileUnits * tileX
                val endX = startX + tileUnits
                val startY = tileY * tileUnits
                val endY = startY + tileUnits
                var fogAlphaA = 0.0f
                var fogAlphaB = 0.0f
                var fogAlphaC = 0.0f
                var fogAlphaD = 0.0f

                val farZA: Float
                val farZB: Float
                val farZC: Float
                val farZD: Float
                val perspZA: Float
                val perspZB: Float
                val perspZC: Float
                val perspZD: Float
                val xA: Float
                val yA: Float
                val xB: Float
                val yB: Float
                val xC: Float
                val yC: Float
                val xD: Float
                val yD: Float

                if ((tile.functionMask.toInt() and COMPLETELY_FLAT) != 0 && !hasWaterUpdate) {
                    val height = tileHeights[tileX][tileY]
                    val nearZOffset = vertexZNearB * height
                    val farZOffset = vertexZFarB * height

                    val nearZA = vertexZNearD + vertexZNearA * startX + nearZOffset + vertexZNearC * startY
                    farZA = vertexZFarD + vertexZFarA * startX + farZOffset + vertexZFarC * startY
                    if (nearZA < -farZA) return

                    val nearZB = vertexZNearD + vertexZNearA * endX + nearZOffset + vertexZNearC * startY
                    farZB = vertexZFarD + vertexZFarA * endX + farZOffset + vertexZFarC * startY
                    if (nearZB < -farZB) return

                    val nearZC = vertexZNearD + vertexZNearA * endX + nearZOffset + vertexZNearC * endY
                    farZC = vertexZFarD + vertexZFarA * endX + farZOffset + vertexZFarC * endY
                    if (nearZC < -farZC) return

                    val nearZD = vertexZNearD + vertexZNearA * startX + nearZOffset + vertexZNearC * endY
                    farZD = vertexZFarD + vertexZFarA * startX + farZOffset + vertexZFarC * endY
                    if (nearZD < -farZD) return

                    perspZA = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZA / farZA
                    perspZB = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZB / farZB
                    perspZC = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZC / farZC
                    perspZD = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZD / farZD

                    if (thread.aBool2304) {
                        var dist = nearZA - thread.aFloat2305
                        if (dist > 0.0f) {
                            fogAlphaA = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                        }
                        dist = nearZB - thread.aFloat2305
                        if (dist > 0.0f) {
                            fogAlphaB = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                        }
                        dist = nearZC - thread.aFloat2305
                        if (dist > 0.0f) {
                            fogAlphaC = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                        }
                        dist = nearZD - thread.aFloat2305
                        if (dist > 0.0f) {
                            fogAlphaD = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                        }
                    }

                    val xOffset = vertexXB * height
                    val yOffset = vertexYB * height

                    val viewXA = vertexXD + vertexXA * startX + xOffset + vertexXC * startY
                    xA = rasterizer.centerX + rasterizer.offsetX * viewXA / farZA
                    val viewYA = vertexYD + vertexYA * startX + yOffset + vertexYC * startY
                    yA = rasterizer.centerY + rasterizer.offsetY * viewYA / farZA

                    val viewXB = vertexXD + vertexXA * endX + xOffset + vertexXC * startY
                    xB = rasterizer.centerX + rasterizer.offsetX * viewXB / farZB
                    val viewYB = vertexYD + vertexYA * endX + yOffset + vertexYC * startY
                    yB = rasterizer.centerY + rasterizer.offsetY * viewYB / farZB

                    val viewXC = vertexXD + vertexXA * endX + xOffset + vertexXC * endY
                    xC = rasterizer.centerX + rasterizer.offsetX * viewXC / farZC
                    val viewYC = vertexYD + vertexYA * endX + yOffset + vertexYC * endY
                    yC = rasterizer.centerY + rasterizer.offsetY * viewYC / farZC

                    val viewXD = vertexXD + vertexXA * startX + xOffset + vertexXC * endY
                    xD = rasterizer.centerX + rasterizer.offsetX * viewXD / farZD
                    val viewYD = vertexYD + vertexYA * startX + yOffset + vertexYC * endY
                    yD = rasterizer.centerY + rasterizer.offsetY * viewYD / farZD
                } else {
                    var heightA = tileHeights[tileX][tileY]
                    var heightB = tileHeights[tileX + 1][tileY]
                    var heightC = tileHeights[tileX + 1][tileY + 1]
                    var heightD = tileHeights[tileX][tileY + 1]

                    val nearZA = vertexZNearD + vertexZNearA * startX + vertexZNearB * heightA + vertexZNearC * startY
                    farZA = vertexZFarD + vertexZFarA * startX + vertexZFarB * heightA + vertexZFarC * startY
                    if (nearZA < -farZA) return

                    val nearZB = vertexZNearD + vertexZNearA * endX + vertexZNearB * heightB + vertexZNearC * startY
                    farZB = vertexZFarD + vertexZFarA * endX + vertexZFarB * heightB + vertexZFarC * startY
                    if (nearZB < -farZB) return

                    val nearZC = vertexZNearD + vertexZNearA * endX + vertexZNearB * heightC + vertexZNearC * endY
                    farZC = vertexZFarD + vertexZFarA * endX + vertexZFarB * heightC + vertexZFarC * endY
                    if (nearZC < -farZC) return

                    val nearZD = vertexZNearD + vertexZNearA * startX + vertexZNearB * heightD + vertexZNearC * endY
                    farZD = vertexZFarD + vertexZFarA * startX + vertexZFarB * heightD + vertexZFarC * endY
                    if (nearZD < -farZD) return

                    perspZA = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZA / farZA
                    perspZB = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZB / farZB
                    perspZC = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZC / farZC
                    perspZD = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZD / farZD

                    if (hasWaterUpdate) {
                        var dist = nearZA - thread.aFloat2305
                        if (dist > 0.0f) {
                            dist = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                            fogAlphaA = dist
                            val alphaAdjust = (tile.alphaSouthWest * dist).toInt()
                            if (alphaAdjust > 0) heightA -= alphaAdjust
                        }
                        dist = nearZB - thread.aFloat2305
                        if (dist > 0.0f) {
                            dist = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                            fogAlphaB = dist
                            val alphaAdjust = (tile.alphaSouthEast * dist).toInt()
                            if (alphaAdjust > 0) heightB -= alphaAdjust
                        }
                        dist = nearZC - thread.aFloat2305
                        if (dist > 0.0f) {
                            dist = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                            fogAlphaC = dist
                            val alphaAdjust = (tile.alphaNorthEast * dist).toInt()
                            if (alphaAdjust > 0) heightC -= alphaAdjust
                        }
                        dist = nearZD - thread.aFloat2305
                        if (dist > 0.0f) {
                            dist = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                            fogAlphaD = dist
                            val alphaAdjust = (tile.alphaNorthWest * dist).toInt()
                            if (alphaAdjust > 0) heightD -= alphaAdjust
                        }
                    } else if (thread.aBool2304) {
                        var dist = nearZA - thread.aFloat2305
                        if (dist > 0.0f) {
                            fogAlphaA = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                        }
                        dist = nearZB - thread.aFloat2305
                        if (dist > 0.0f) {
                            fogAlphaB = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                        }
                        dist = nearZC - thread.aFloat2305
                        if (dist > 0.0f) {
                            fogAlphaC = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                        }
                        dist = nearZD - thread.aFloat2305
                        if (dist > 0.0f) {
                            fogAlphaD = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                        }
                    }

                    val viewXA = vertexXD + vertexXA * startX + vertexXB * heightA + vertexXC * startY
                    xA = rasterizer.centerX + rasterizer.offsetX * viewXA / farZA
                    val viewYA = vertexYD + vertexYA * startX + vertexYB * heightA + vertexYC * startY
                    yA = rasterizer.centerY + rasterizer.offsetY * viewYA / farZA

                    val viewXB = vertexXD + vertexXA * endX + vertexXB * heightB + vertexXC * startY
                    xB = rasterizer.centerX + rasterizer.offsetX * viewXB / farZB
                    val viewYB = vertexYD + vertexYA * endX + vertexYB * heightB + vertexYC * startY
                    yB = rasterizer.centerY + rasterizer.offsetY * viewYB / farZB

                    val viewXC = vertexXD + vertexXA * endX + vertexXB * heightC + vertexXC * endY
                    xC = rasterizer.centerX + rasterizer.offsetX * viewXC / farZC
                    val viewYC = vertexYD + vertexYA * endX + vertexYB * heightC + vertexYC * endY
                    yC = rasterizer.centerY + rasterizer.offsetY * viewYC / farZC

                    val viewXD = vertexXD + vertexXA * startX + vertexXB * heightD + vertexXC * endY
                    xD = rasterizer.centerX + rasterizer.offsetX * viewXD / farZD
                    val viewYD = vertexYD + vertexYA * startX + vertexYB * heightD + vertexYC * endY
                    yD = rasterizer.centerY + rasterizer.offsetY * viewYD / farZD
                }

                val hasWaterEffect = tile.materialId != (-1).toShort() && flagMatch(toolkit.materialProvider!!.getMaterial(tile.materialId.toInt()).effectId.toInt())
                val combinedFogBCD = fogAlphaB + fogAlphaC + fogAlphaD

                // Triangle 1: C-D-B (NorthEast-NorthWest-SouthEast)
                if ((xC - xD) * (yB - yD) - (yC - yD) * (xB - xD) > 0.0f) {
                    rasterizer.clipEdges = xC < 0.0f || xD < 0.0f || xB < 0.0f || xC > rasterizer.clipWidth || xD > rasterizer.clipWidth || xB > rasterizer.clipWidth
                    if (combinedFogBCD < 3.0f) {
                        if (combinedFogBCD > 0.0f) {
                            if (tile.materialId >= 0) {
                                var colorMask = 0xff000000.toInt()
                                if (hasWaterEffect) colorMask = -1694498816
                                rasterizer.drawMaterializedTriangle(true, true, false, yC, yD, yB, xC, xD, xB, perspZC, perspZD, perspZB, farZC, farZD, farZB, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, colorMask or (tile.colorNorthEast and 0xffffff), colorMask or (tile.colorNorthWest and 0xffffff), colorMask or (tile.colorSouthEast and 0xffffff), thread.fogColor, fogAlphaC * 255.0f, fogAlphaD * 255.0f, fogAlphaB * 255.0f, tile.materialId.toInt())
                            } else {
                                if (hasWaterEffect) rasterizer.blendingAlpha = 100
                                rasterizer.drawTriangle(true, true, false, yC, yD, yB, xC, xD, xB, perspZC, perspZD, perspZB, ColorUtil.blendMaterialColor(tile.colorNorthEast, (fogAlphaC * 255.0f).toInt() shl 24 or thread.fogColor), ColorUtil.blendMaterialColor(tile.colorNorthWest, (fogAlphaD * 255.0f).toInt() shl 24 or thread.fogColor), ColorUtil.blendMaterialColor(tile.colorSouthEast, (fogAlphaB * 255.0f).toInt() shl 24 or thread.fogColor))
                                rasterizer.blendingAlpha = 0
                            }
                        } else if (tile.materialId >= 0) {
                            var colorMask = 0xff000000.toInt()
                            if (hasWaterEffect) colorMask = -1694498816
                            rasterizer.drawMaterializedTriangle(true, true, false, yC, yD, yB, xC, xD, xB, perspZC, perspZD, perspZB, farZC, farZD, farZB, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, colorMask or (tile.colorNorthEast and 0xffffff), colorMask or (tile.colorNorthWest and 0xffffff), colorMask or (tile.colorSouthEast and 0xffffff), 0, 0.0f, 0.0f, 0.0f, tile.materialId.toInt())
                        } else {
                            if (hasWaterEffect) rasterizer.blendingAlpha = 100
                            rasterizer.drawTriangle(true, true, false, yC, yD, yB, xC, xD, xB, perspZC, perspZD, perspZB, tile.colorNorthEast, tile.colorNorthWest, tile.colorSouthEast)
                            rasterizer.blendingAlpha = 0
                        }
                    } else {
                        rasterizer.drawFlatTriangle(true, true, false, yC, yD, yB, xC, xD, xB, perspZC, perspZD, perspZB, thread.fogColor)
                    }
                }

                val combinedFogABD = fogAlphaA + fogAlphaB + fogAlphaD
                // Triangle 2: A-B-D (SouthWest-SouthEast-NorthWest)
                if ((xA - xB) * (yD - yB) - (yA - yB) * (xD - xB) > 0.0f) {
                    rasterizer.clipEdges = xA < 0.0f || xB < 0.0f || xD < 0.0f || xA > rasterizer.clipWidth || xB > rasterizer.clipWidth || xD > rasterizer.clipWidth
                    if (combinedFogABD < 3.0f) {
                        if (hasWaterEffect) rasterizer.blendingAlpha = -1694498816
                        if (combinedFogABD > 0.0f) {
                            if (tile.materialId >= 0) {
                                var colorMask = 0xff000000.toInt()
                                if (hasWaterEffect) colorMask = -1694498816
                                rasterizer.drawMaterializedTriangle(true, true, false, yA, yB, yD, xA, xB, xD, perspZA, perspZB, perspZD, farZA, farZB, farZD, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, colorMask or (tile.colorSouthWest and 0xffffff), colorMask or (tile.colorSouthEast and 0xffffff), colorMask or (tile.colorNorthWest and 0xffffff), thread.fogColor, fogAlphaA * 255.0f, fogAlphaB * 255.0f, fogAlphaD * 255.0f, tile.materialId.toInt())
                            } else {
                                if (hasWaterEffect) rasterizer.blendingAlpha = 100
                                rasterizer.drawTriangle(true, true, false, yA, yB, yD, xA, xB, xD, perspZA, perspZB, perspZD, ColorUtil.blendMaterialColor(tile.colorSouthWest, (fogAlphaA * 255.0f).toInt() shl 24 or thread.fogColor), ColorUtil.blendMaterialColor(tile.colorSouthEast, (fogAlphaB * 255.0f).toInt() shl 24 or thread.fogColor), ColorUtil.blendMaterialColor(tile.colorNorthWest, (fogAlphaD * 255.0f).toInt() shl 24 or thread.fogColor))
                                rasterizer.blendingAlpha = 0
                            }
                        } else if (tile.materialId >= 0) {
                            var colorMask = 0xff000000.toInt()
                            if (hasWaterEffect) colorMask = -1694498816
                            rasterizer.drawMaterializedTriangle(true, true, false, yA, yB, yD, xA, xB, xD, perspZA, perspZB, perspZD, farZA, farZB, farZD, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, colorMask or (tile.colorSouthWest and 0xffffff), colorMask or (tile.colorSouthEast and 0xffffff), colorMask or (tile.colorNorthWest and 0xffffff), 0, 0.0f, 0.0f, 0.0f, tile.materialId.toInt())
                        } else {
                            if (hasWaterEffect) rasterizer.blendingAlpha = 100
                            rasterizer.drawTriangle(true, true, false, yA, yB, yD, xA, xB, xD, perspZA, perspZB, perspZD, tile.colorSouthWest, tile.colorSouthEast, tile.colorNorthWest)
                            rasterizer.blendingAlpha = 0
                        }
                    } else {
                        rasterizer.drawFlatTriangle(true, true, false, yA, yB, yD, xA, xB, xD, perspZA, perspZB, perspZD, thread.fogColor)
                    }
                }
            }
        } else {
            val shapedTile = shapedBlendedTiles!![tileX][tileY]
            if (shapedTile != null) {
                if (tileFlags != 0) {
                    if ((shapedTile.functionMask.toInt() and TEXTURE_MOVEMENT) != 0) {
                        if ((tileFlags and COMPLETELY_FLAT) != 0) return
                    } else if ((tileFlags and HD_LIGHT) != 0) {
                        return
                    }
                }

                for (vertIdx in 0 until shapedTile.verticeCount) {
                    val posX = shapedTile.positionX!![vertIdx] + (tileX shl tileScale)
                    var posZ = shapedTile.drawHeight!![vertIdx].toInt()
                    val posY = shapedTile.positionY!![vertIdx] + (tileY shl tileScale)
                    val nearZ = vertexZNearD + vertexZNearA * posX + vertexZNearB * posZ + vertexZNearC * posY
                    val farZ = vertexZFarD + vertexZFarA * posX + vertexZFarB * posZ + vertexZFarC * posY
                    if (nearZ < -farZ) return

                    val perspZ = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZ / farZ
                    floatsAlpha[vertIdx] = 0.0f

                    if (hasWaterUpdate) {
                        var dist = nearZ - thread.aFloat2305
                        if (dist > 0.0f) {
                            dist = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                            floatsAlpha[vertIdx] = dist
                            val alphaAdjust = (shapedTile.alpha!![vertIdx] * dist).toInt()
                            if (alphaAdjust > 0) posZ -= alphaAdjust
                        }
                    } else if (thread.aBool2304) {
                        val dist = nearZ - thread.aFloat2305
                        if (dist > 0.0f) {
                            floatsAlpha[vertIdx] = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                        }
                    }

                    val viewX = vertexXD + vertexXA * posX + vertexXB * posZ + vertexXC * posY
                    val viewY = vertexYD + vertexYA * posX + vertexYB * posZ + vertexYC * posY
                    floatsX[vertIdx] = rasterizer.centerX + rasterizer.offsetX * viewX / farZ
                    floatsY[vertIdx] = rasterizer.centerY + rasterizer.offsetY * viewY / farZ
                    floatsZNear[vertIdx] = perspZ
                    floatsZFar[vertIdx] = farZ
                }

                if (shapedTile.materialIds != null) {
                    for (faceIdx in 0 until shapedTile.faceCount) {
                        val a = faceIdx * 3
                        val b = a + 1
                        val c = b + 1
                        val xA = floatsX[a]
                        val xB = floatsX[b]
                        val xC = floatsX[c]
                        val yA = floatsY[a]
                        val yB = floatsY[b]
                        val yC = floatsY[c]
                        val combinedFog = floatsAlpha[a] + floatsAlpha[b] + floatsAlpha[c]

                        if ((xA - xB) * (yC - yB) - (yA - yB) * (xC - xB) > 0.0f) {
                            rasterizer.clipEdges = xA < 0.0f || xB < 0.0f || xC < 0.0f || xA > rasterizer.clipWidth || xB > rasterizer.clipWidth || xC > rasterizer.clipWidth
                            val materialId = shapedTile.materialIds!![faceIdx]

                            if (combinedFog < 3.0f) {
                                if (combinedFog > 0.0f) {
                                    if (materialId != (-1).toShort()) {
                                        var colorMask = 0xff000000.toInt()
                                        if (flagMatch(toolkit.materialProvider!!.getMaterial(materialId.toInt()).effectId.toInt())) {
                                            colorMask = -1694498816
                                        }
                                        rasterizer.drawMaterializedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, floatsZNear[a], floatsZNear[b], floatsZNear[c], floatsZFar[a], floatsZFar[b], floatsZFar[c], shapedTile.positionX!![a].toFloat() / tileUnits, shapedTile.positionX!![b].toFloat() / tileUnits, shapedTile.positionX!![c].toFloat() / tileUnits, shapedTile.positionY!![a].toFloat() / tileUnits, shapedTile.positionY!![b].toFloat() / tileUnits, shapedTile.positionY!![c].toFloat() / tileUnits, colorMask or (shapedTile.colors!![a] and 0xffffff), colorMask or (shapedTile.colors!![b] and 0xffffff), colorMask or (shapedTile.colors!![c] and 0xffffff), thread.fogColor, floatsAlpha[a] * 255.0f, floatsAlpha[b] * 255.0f, floatsAlpha[c] * 255.0f, materialId.toInt())
                                    } else if ((shapedTile.colors!![a] and 0xffffff) != 0) {
                                        rasterizer.drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, floatsZNear[a], floatsZNear[b], floatsZNear[c], ColorUtil.blendMaterialColor(shapedTile.colors!![a], (floatsAlpha[a] * 255.0f).toInt() shl 24 or thread.fogColor), ColorUtil.blendMaterialColor(shapedTile.colors!![b], (floatsAlpha[b] * 255.0f).toInt() shl 24 or thread.fogColor), ColorUtil.blendMaterialColor(shapedTile.colors!![c], (floatsAlpha[c] * 255.0f).toInt() shl 24 or thread.fogColor))
                                        rasterizer.blendingAlpha = 0
                                    }
                                } else if (materialId != (-1).toShort()) {
                                    var colorMask = 0xff000000.toInt()
                                    if (flagMatch(toolkit.materialProvider!!.getMaterial(materialId.toInt()).effectId.toInt())) {
                                        colorMask = -1694498816
                                    }
                                    rasterizer.drawMaterializedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, floatsZNear[a], floatsZNear[b], floatsZNear[c], floatsZFar[a], floatsZFar[b], floatsZFar[c], shapedTile.positionX!![a].toFloat() / tileUnits, shapedTile.positionX!![b].toFloat() / tileUnits, shapedTile.positionX!![c].toFloat() / tileUnits, shapedTile.positionY!![a].toFloat() / tileUnits, shapedTile.positionY!![b].toFloat() / tileUnits, shapedTile.positionY!![c].toFloat() / tileUnits, colorMask or (shapedTile.colors!![a] and 0xffffff), colorMask or (shapedTile.colors!![b] and 0xffffff), colorMask or (shapedTile.colors!![c] and 0xffffff), 0, 0.0f, 0.0f, 0.0f, materialId.toInt())
                                } else if ((shapedTile.colors!![a] and 0xffffff) != 0) {
                                    rasterizer.drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, floatsZNear[a], floatsZNear[b], floatsZNear[c], shapedTile.colors!![a], shapedTile.colors!![b], shapedTile.colors!![c])
                                    rasterizer.blendingAlpha = 0
                                }
                            } else {
                                rasterizer.drawFlatTriangle(true, true, false, yA, yB, yC, xA, xB, xC, floatsZNear[a], floatsZNear[b], floatsZNear[c], thread.fogColor)
                            }
                        }
                    }
                } else {
                    for (faceIdx in 0 until shapedTile.faceCount) {
                        val a = faceIdx * 3
                        val b = a + 1
                        val c = b + 1
                        val xA = floatsX[a]
                        val xB = floatsX[b]
                        val xC = floatsX[c]
                        val yA = floatsY[a]
                        val yB = floatsY[b]
                        val yC = floatsY[c]
                        val combinedFog = floatsAlpha[a] + floatsAlpha[b] + floatsAlpha[c]

                        if ((xA - xB) * (yC - yB) - (yA - yB) * (xC - xB) > 0.0f) {
                            rasterizer.clipEdges = xA < 0.0f || xB < 0.0f || xC < 0.0f || xA > rasterizer.clipWidth || xB > rasterizer.clipWidth || xC > rasterizer.clipWidth
                            if (combinedFog < 3.0f) {
                                if (combinedFog > 0.0f) {
                                    if ((shapedTile.colors!![a] and 0xffffff) != 0) {
                                        rasterizer.drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, floatsZNear[a], floatsZNear[b], floatsZNear[c], ColorUtil.mixColorsWithTransparency(shapedTile.colors!![a], thread.fogColor, floatsAlpha[a] * 255.0f), ColorUtil.mixColorsWithTransparency(shapedTile.colors!![b], thread.fogColor, floatsAlpha[b] * 255.0f), ColorUtil.mixColorsWithTransparency(shapedTile.colors!![c], thread.fogColor, floatsAlpha[c] * 255.0f))
                                    }
                                } else if ((shapedTile.colors!![a] and 0xffffff) != 0) {
                                    rasterizer.drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, floatsZNear[a], floatsZNear[b], floatsZNear[c], shapedTile.colors!![a], shapedTile.colors!![b], shapedTile.colors!![c])
                                }
                            } else {
                                rasterizer.drawFlatTriangle(true, true, false, yA, yB, yC, xA, xB, xC, floatsZNear[a], floatsZNear[b], floatsZNear[c], thread.fogColor)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun renderFloor(x: Int, y: Int, visibleTiles: Int, onscreen: Array<BooleanArray>, dummy: Boolean) {
        if (toolkit.colourBuffer == null || toolkit.depthBuffer == null) {
            throw IllegalStateException("")
        }
        val floorMatrix = toolkit.floorMatrix!!
        vertexXA = floorMatrix.faceList[0]
        vertexYA = floorMatrix.faceList[1]
        vertexZNearA = floorMatrix.faceList[2]
        vertexZFarA = floorMatrix.faceList[3]
        vertexXB = floorMatrix.faceList[4]
        vertexYB = floorMatrix.faceList[5]
        vertexZNearB = floorMatrix.faceList[6]
        vertexZFarB = floorMatrix.faceList[7]
        vertexXC = floorMatrix.faceList[8]
        vertexYC = floorMatrix.faceList[9]
        vertexZNearC = floorMatrix.faceList[10]
        vertexZFarC = floorMatrix.faceList[11]
        vertexXD = floorMatrix.faceList[12]
        vertexYD = floorMatrix.faceList[13]
        vertexZNearD = floorMatrix.faceList[14]
        vertexZFarD = floorMatrix.faceList[15]

        for (startX in 0 until visibleTiles + visibleTiles) {
            for (startY in 0 until visibleTiles + visibleTiles) {
                if (onscreen[startX][startY]) {
                    val sceneX = startX + (x - visibleTiles)
                    val sceneY = startY + (y - visibleTiles)
                    if (sceneX >= 0 && sceneX < width && sceneY >= 0 && sceneY < length) {
                        render(sceneX, sceneY)
                    }
                }
            }
        }
    }

    fun renderHighDetailMinimap(
        offX: Int,
        offY: Int,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        onscreen: Array<BooleanArray>,
        thread: ThreadResourceSM,
        rasterizer: RasterizerSM,
        projectedVerticesX: FloatArray,
        projectedVerticesY: FloatArray
    ) {
        val stepY = 1024 * (endY - startY) / 256
        val baseOffset = 1024 shr 8
        val depthBuffering = thread.zbuffering
        toolkit.setDepthBuffering(false)
        rasterizer.smoothEdges = false
        rasterizer.blending = false
        var offsetX = offX
        var offsetY = stepY + offY

        for (x in startX until endX) {
            for (y in startY until endY) {
                if (onscreen[x - startX][y - startY]) {
                    if (plainBlendedTiles != null) {
                        val plainTileBlended = plainBlendedTiles!![x][y]
                        if (plainTileBlended != null) {
                            if (plainTileBlended.materialId != (-1).toShort() && (plainTileBlended.functionMask.toInt() and HD_LIGHT) == 0 && plainTileBlended.materialColor == 0) {
                                val materialColor = toolkit.getMaterialColor(plainTileBlended.materialId.toInt())
                                rasterizer.drawShadedTriangle(true, true, false, (offsetY - baseOffset).toFloat(), (offsetY - baseOffset).toFloat(), offsetY.toFloat(), (offsetX + baseOffset).toFloat(), offsetX.toFloat(), (offsetX + baseOffset).toFloat(), 100.0f, 100.0f, 100.0f, ColorUtil.repackHsl(materialColor, plainTileBlended.colorNorthEast).toFloat(), ColorUtil.repackHsl(materialColor, plainTileBlended.colorNorthWest).toFloat(), ColorUtil.repackHsl(materialColor, plainTileBlended.colorSouthEast).toFloat())
                                rasterizer.drawShadedTriangle(true, true, false, offsetY.toFloat(), offsetY.toFloat(), (offsetY - baseOffset).toFloat(), offsetX.toFloat(), (offsetX + baseOffset).toFloat(), offsetX.toFloat(), 100.0f, 100.0f, 100.0f, ColorUtil.repackHsl(materialColor, plainTileBlended.colorSouthWest).toFloat(), ColorUtil.repackHsl(materialColor, plainTileBlended.colorSouthEast).toFloat(), ColorUtil.repackHsl(materialColor, plainTileBlended.colorNorthWest).toFloat())
                            } else if (plainTileBlended.materialColor == 0) {
                                rasterizer.drawTriangle(true, true, false, (offsetY - baseOffset).toFloat(), (offsetY - baseOffset).toFloat(), offsetY.toFloat(), (offsetX + baseOffset).toFloat(), offsetX.toFloat(), (offsetX + baseOffset).toFloat(), 100.0f, 100.0f, 100.0f, plainTileBlended.colorNorthEast, plainTileBlended.colorNorthWest, plainTileBlended.colorSouthEast)
                                rasterizer.drawTriangle(true, true, false, offsetY.toFloat(), offsetY.toFloat(), (offsetY - baseOffset).toFloat(), offsetX.toFloat(), (offsetX + baseOffset).toFloat(), offsetX.toFloat(), 100.0f, 100.0f, 100.0f, plainTileBlended.colorSouthWest, plainTileBlended.colorSouthEast, plainTileBlended.colorNorthWest)
                            } else {
                                val materialColor = plainTileBlended.materialColor
                                rasterizer.drawTriangle(true, true, false, (offsetY - baseOffset).toFloat(), (offsetY - baseOffset).toFloat(), offsetY.toFloat(), (offsetX + baseOffset).toFloat(), offsetX.toFloat(), (offsetX + baseOffset).toFloat(), 100.0f, 100.0f, 100.0f, ColorUtil.blendMaterialColor(materialColor, plainTileBlended.colorNorthEast and 0xff000000.toInt()), ColorUtil.blendMaterialColor(materialColor, plainTileBlended.colorNorthWest and 0xff000000.toInt()), ColorUtil.blendMaterialColor(materialColor, plainTileBlended.colorSouthEast and 0xff000000.toInt()))
                                rasterizer.drawTriangle(true, true, false, offsetY.toFloat(), offsetY.toFloat(), (offsetY - baseOffset).toFloat(), offsetX.toFloat(), (offsetX + baseOffset).toFloat(), offsetX.toFloat(), 100.0f, 100.0f, 100.0f, ColorUtil.blendMaterialColor(materialColor, plainTileBlended.colorSouthWest and 0xff000000.toInt()), ColorUtil.blendMaterialColor(materialColor, plainTileBlended.colorSouthEast and 0xff000000.toInt()), ColorUtil.blendMaterialColor(materialColor, plainTileBlended.colorNorthWest and 0xff000000.toInt()))
                            }
                        } else {
                            val shapedBlendedTile = shapedBlendedTiles!![x][y]
                            if (shapedBlendedTile != null) {
                                for (vertice in 0 until shapedBlendedTile.verticeCount) {
                                    projectedVerticesX[vertice] = (offsetX + shapedBlendedTile.positionX!![vertice] * baseOffset / tileUnits).toFloat()
                                    projectedVerticesY[vertice] = (offsetY - shapedBlendedTile.positionY!![vertice] * baseOffset / tileUnits).toFloat()
                                }
                                for (face in 0 until shapedBlendedTile.faceCount) {
                                    val a = face * 3
                                    val b = a + 1
                                    val c = b + 1
                                    val xA = projectedVerticesX[a]
                                    val xB = projectedVerticesX[b]
                                    val xC = projectedVerticesX[c]
                                    val yA = projectedVerticesY[a]
                                    val yB = projectedVerticesY[b]
                                    val yC = projectedVerticesY[c]
                                    if (shapedBlendedTile.materialColors == null || shapedBlendedTile.materialColors!![face] == 0 || shapedBlendedTile.materialIds != null && shapedBlendedTile.materialIds!![face] != (-1).toShort()) {
                                        if (shapedBlendedTile.materialIds != null && shapedBlendedTile.materialIds!![face] != (-1).toShort()) {
                                            val materialColor = toolkit.getMaterialColor(shapedBlendedTile.materialIds!![face].toInt())
                                            rasterizer.drawShadedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0f, 100.0f, 100.0f, materialColor.toFloat(), materialColor.toFloat(), materialColor.toFloat())
                                        } else {
                                            rasterizer.drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0f, 100.0f, 100.0f, shapedBlendedTile.colors!![a], shapedBlendedTile.colors!![b], shapedBlendedTile.colors!![c])
                                        }
                                    } else {
                                        val materialColor = shapedBlendedTile.materialColors!![face]
                                        rasterizer.drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0f, 100.0f, 100.0f, ColorUtil.blendMaterialColor(materialColor, 0xff000000.toInt() - (shapedBlendedTile.colors!![a] and 0xff000000.toInt())), ColorUtil.blendMaterialColor(materialColor, 0xff000000.toInt() - (shapedBlendedTile.colors!![b] and 0xff000000.toInt())), ColorUtil.blendMaterialColor(materialColor, 0xff000000.toInt() - (shapedBlendedTile.colors!![c] and 0xff000000.toInt())))
                                    }
                                }
                            }
                        }
                    } else if (blendedTexturedTiles!![x][y] != null) {
                        val blendedTexturedTile = blendedTexturedTiles!![x][y]!!
                        for (vertice in 0 until blendedTexturedTile.verticeCount) {
                            projectedVerticesX[vertice] = (offsetX + blendedTexturedTile.positionX!![vertice] * baseOffset / tileUnits).toFloat()
                            projectedVerticesY[vertice] = (offsetY - blendedTexturedTile.positionZ!![vertice] * baseOffset / tileUnits).toFloat()
                        }
                        for (face in 0 until blendedTexturedTile.faceCount) {
                            val a = face * 3
                            val b = a + 1
                            val c = b + 1
                            val xA = projectedVerticesX[a]
                            val xB = projectedVerticesX[b]
                            val xC = projectedVerticesX[c]
                            val yA = projectedVerticesY[a]
                            val yB = projectedVerticesY[b]
                            val yC = projectedVerticesY[c]
                            if (blendedTexturedTile.materialColors != null && blendedTexturedTile.materialColors!![face] != 0) {
                                val materialColor = blendedTexturedTile.materialColors!![face]
                                rasterizer.drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0f, 100.0f, 100.0f, materialColor, materialColor, materialColor)
                            } else {
                                rasterizer.drawTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0f, 100.0f, 100.0f, blendedTexturedTile.colors!![a], blendedTexturedTile.colors!![b], blendedTexturedTile.colors!![c])
                            }
                        }
                    }
                }
                offsetY -= baseOffset
            }
            offsetY = stepY + offY
            offsetX += baseOffset
        }
        rasterizer.smoothEdges = true
        toolkit.setDepthBuffering(depthBuffering)
    }

    fun renderBlendedTexturesTile(
        x: Int,
        y: Int,
        hasWaterUpdate: Boolean,
        thread: ThreadResourceSM,
        rasterizer: RasterizerSM,
        perspectiveX: FloatArray,
        perspectiveY: FloatArray,
        perspectiveZ: FloatArray,
        perspectiveZB: FloatArray,
        floatsAlpha: FloatArray,
        tileFlags: Int
    ) {
        val blendedTexturedTile = blendedTexturedTiles!![x][y]
        if ((tileFlags == 0 || (tileFlags and HD_LIGHT) == 0) && blendedTexturedTile != null) {
            for (verticeIndex in 0 until blendedTexturedTile.verticeCount) {
                val posX = blendedTexturedTile.positionX!![verticeIndex] + (x shl tileScale)
                var posZ = blendedTexturedTile.tileDrawHeight!![verticeIndex].toInt()
                val posY = blendedTexturedTile.positionZ!![verticeIndex] + (y shl tileScale)
                val nearZ = vertexZNearD + vertexZNearA * posX + vertexZNearB * posZ + vertexZNearC * posY
                val farZ = vertexZFarD + vertexZFarA * posX + vertexZFarB * posZ + vertexZFarC * posY
                if (nearZ < -farZ) return

                floatsAlpha[verticeIndex] = 0.0f
                if (hasWaterUpdate) {
                    var dist = nearZ - thread.aFloat2305
                    if (dist > 0.0f) {
                        dist = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                        floatsAlpha[verticeIndex] = dist
                        val alpha = (blendedTexturedTile.alpha!![verticeIndex] * dist).toInt()
                        if (alpha > 0) posZ -= alpha
                    }
                } else if (thread.aBool2304) {
                    val dist = nearZ - thread.aFloat2305
                    if (dist > 0.0f) {
                        floatsAlpha[verticeIndex] = (dist / thread.aFloat2303).coerceAtMost(1.0f)
                    }
                }

                val viewX = vertexXD + vertexXA * posX + vertexXB * posZ + vertexXC * posY
                val viewY = vertexYD + vertexYA * posX + vertexYB * posZ + vertexYC * posY
                perspectiveX[verticeIndex] = rasterizer.centerX + rasterizer.offsetX * viewX / farZ
                perspectiveY[verticeIndex] = rasterizer.centerY + rasterizer.offsetY * viewY / farZ
                perspectiveZ[verticeIndex] = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZ / farZ
                perspectiveZB[verticeIndex] = farZ
            }

            val tileUnitsF = tileUnits.toFloat()

            for (face in 0 until blendedTexturedTile.faceCount) {
                val a = face * 3
                val b = a + 1
                val c = b + 1
                val xA = perspectiveX[a]
                val xB = perspectiveX[b]
                val xC = perspectiveX[c]
                val yA = perspectiveY[a]
                val yB = perspectiveY[b]
                val yC = perspectiveY[c]

                if ((xA - xB) * (yC - yB) - (yA - yB) * (xC - xB) > 0.0f) {
                    rasterizer.clipEdges = xA < 0.0f || xB < 0.0f || xC < 0.0f || xA > rasterizer.clipWidth || xB > rasterizer.clipWidth || xC > rasterizer.clipWidth
                    if (floatsAlpha[a] + floatsAlpha[b] + floatsAlpha[c] < 3.0f) {
                        val scaledX = x shl tileScale
                        val scaledY = y shl tileScale
                        if ((blendedTexturedTile.colors!![a] and 0xffffff) != 0) {
                            if (blendedTexturedTile.materialId!![a] == blendedTexturedTile.materialId!![b] && blendedTexturedTile.materialId!![a] == blendedTexturedTile.materialId!![c] && blendedTexturedTile.aShortArray2254!![a] == blendedTexturedTile.aShortArray2254!![b] && blendedTexturedTile.aShortArray2254!![a] == blendedTexturedTile.aShortArray2254!![c]) {
                                rasterizer.drawMaterializedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, perspectiveZ[a], perspectiveZ[b], perspectiveZ[c], perspectiveZB[a], perspectiveZB[b], perspectiveZB[c], (blendedTexturedTile.positionX!![a] + scaledX).toFloat() / blendedTexturedTile.aShortArray2254!![a], (blendedTexturedTile.positionX!![b] + scaledX).toFloat() / blendedTexturedTile.aShortArray2254!![b], (blendedTexturedTile.positionX!![c] + scaledX).toFloat() / blendedTexturedTile.aShortArray2254!![c], (blendedTexturedTile.positionZ!![a] + scaledY).toFloat() / blendedTexturedTile.aShortArray2254!![a], (blendedTexturedTile.positionZ!![b] + scaledY).toFloat() / blendedTexturedTile.aShortArray2254!![b], (blendedTexturedTile.positionZ!![c] + scaledY).toFloat() / blendedTexturedTile.aShortArray2254!![c], blendedTexturedTile.colors!![a], blendedTexturedTile.colors!![b], blendedTexturedTile.colors!![c], thread.fogColor, floatsAlpha[a] * 255.0f, floatsAlpha[b] * 255.0f, floatsAlpha[c] * 255.0f, blendedTexturedTile.materialId!![a].toInt())
                            } else {
                                rasterizer.drawMultiMaterializedTriangle(yA, yB, yC, xA, xB, xC, perspectiveZ[a], perspectiveZ[b], perspectiveZ[c], perspectiveZB[a], perspectiveZB[b], perspectiveZB[c], (blendedTexturedTile.positionX!![a] + scaledX) / tileUnitsF, (blendedTexturedTile.positionX!![b] + scaledX) / tileUnitsF, (blendedTexturedTile.positionX!![c] + scaledX) / tileUnitsF, (blendedTexturedTile.positionZ!![a] + scaledY) / tileUnitsF, (blendedTexturedTile.positionZ!![b] + scaledY) / tileUnitsF, (blendedTexturedTile.positionZ!![c] + scaledY) / tileUnitsF, blendedTexturedTile.colors!![a], blendedTexturedTile.colors!![b], blendedTexturedTile.colors!![c], thread.fogColor, floatsAlpha[a] * 255.0f, floatsAlpha[b] * 255.0f, floatsAlpha[c] * 255.0f, blendedTexturedTile.materialId!![a].toInt(), tileUnitsF / blendedTexturedTile.aShortArray2254!![a], blendedTexturedTile.materialId!![b].toInt(), tileUnitsF / blendedTexturedTile.aShortArray2254!![b], blendedTexturedTile.materialId!![c].toInt(), tileUnitsF / blendedTexturedTile.aShortArray2254!![c])
                            }
                        }
                    } else {
                        rasterizer.drawFlatTriangle(true, true, false, yA, yB, yC, xA, xB, xC, perspectiveZ[a], perspectiveZ[b], perspectiveZ[c], thread.fogColor)
                    }
                }
            }
        }
    }

    fun flagMatch(effectId: Int): Boolean {
        return (groundFunctionMask and 0x8) != 0 && (effectId == 4 || effectId == 8 || effectId == 9)
    }

    fun renderLowDetailMinimap(
        offX: Int,
        offY: Int,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        onScreen: Array<BooleanArray>,
        thread: ThreadResourceSM,
        rasterizer: RasterizerSM,
        renderX: FloatArray,
        renderY: FloatArray
    ) {
        val stepY = 1024 * (endY - startY) / 256
        val baseOffset = 1024 shr 8
        val enableZbuffer = thread.zbuffering
        toolkit.setDepthBuffering(false)
        rasterizer.smoothEdges = false
        rasterizer.blending = false
        var offsetX = offX
        var offsetY = stepY + offY

        for (tileX in startX until endX) {
            for (tileY in startY until endY) {
                if (onScreen[tileX - startX][tileY - startY]) {
                    val plainTile = scenePlainTiles!![tileX][tileY]
                    if (plainTile != null) {
                        if (plainTile.materialId != (-1).toShort() && (plainTile.functionMask.toInt() and HD_LIGHT) == 0 && plainTile.color == -1) {
                            val materialColor = toolkit.getMaterialColor(plainTile.materialId.toInt())
                            rasterizer.drawShadedTriangle(true, true, false, (offsetY - baseOffset).toFloat(), (offsetY - baseOffset).toFloat(), offsetY.toFloat(), (offsetX + baseOffset).toFloat(), offsetX.toFloat(), (offsetX + baseOffset).toFloat(), 100.0f, 100.0f, 100.0f, ColorUtil.repackHsl(materialColor, (plainTile.colorNorthEast.toInt() and 0xffff)).toFloat(), ColorUtil.repackHsl(materialColor, (plainTile.colorNorthWest.toInt() and 0xffff)).toFloat(), ColorUtil.repackHsl(materialColor, (plainTile.colorSouthEast.toInt() and 0xffff)).toFloat())
                            rasterizer.drawShadedTriangle(true, true, false, offsetY.toFloat(), offsetY.toFloat(), (offsetY - baseOffset).toFloat(), offsetX.toFloat(), (offsetX + baseOffset).toFloat(), offsetX.toFloat(), 100.0f, 100.0f, 100.0f, ColorUtil.repackHsl(materialColor, (plainTile.colorSouthWest.toInt() and 0xffff)).toFloat(), ColorUtil.repackHsl(materialColor, (plainTile.colorSouthEast.toInt() and 0xffff)).toFloat(), ColorUtil.repackHsl(materialColor, (plainTile.colorNorthWest.toInt() and 0xffff)).toFloat())
                        } else if (plainTile.color == -1) {
                            rasterizer.drawShadedTriangle(true, true, false, (offsetY - baseOffset).toFloat(), (offsetY - baseOffset).toFloat(), offsetY.toFloat(), (offsetX + baseOffset).toFloat(), offsetX.toFloat(), (offsetX + baseOffset).toFloat(), 100.0f, 100.0f, 100.0f, (plainTile.colorNorthEast.toInt() and 0xffff).toFloat(), (plainTile.colorNorthWest.toInt() and 0xffff).toFloat(), (plainTile.colorSouthEast.toInt() and 0xffff).toFloat())
                            rasterizer.drawShadedTriangle(true, true, false, offsetY.toFloat(), offsetY.toFloat(), (offsetY - baseOffset).toFloat(), offsetX.toFloat(), (offsetX + baseOffset).toFloat(), offsetX.toFloat(), 100.0f, 100.0f, 100.0f, (plainTile.colorSouthWest.toInt() and 0xffff).toFloat(), (plainTile.colorSouthEast.toInt() and 0xffff).toFloat(), (plainTile.colorNorthWest.toInt() and 0xffff).toFloat())
                        } else {
                            val tileColor = plainTile.color.toFloat()
                            rasterizer.drawShadedTriangle(true, true, false, (offsetY - baseOffset).toFloat(), (offsetY - baseOffset).toFloat(), offsetY.toFloat(), (offsetX + baseOffset).toFloat(), offsetX.toFloat(), (offsetX + baseOffset).toFloat(), 100.0f, 100.0f, 100.0f, tileColor, tileColor, tileColor)
                            rasterizer.drawShadedTriangle(true, true, false, offsetY.toFloat(), offsetY.toFloat(), (offsetY - baseOffset).toFloat(), offsetX.toFloat(), (offsetX + baseOffset).toFloat(), offsetX.toFloat(), 100.0f, 100.0f, 100.0f, tileColor, tileColor, tileColor)
                        }
                    } else {
                        val shapedTile = sceneShapedTiles!![tileX][tileY]
                        if (shapedTile != null) {
                            for (i in 0 until shapedTile.count) {
                                renderX[i] = (offsetX + shapedTile.offsetX!![i] * baseOffset / tileUnits).toFloat()
                                renderY[i] = (offsetY - shapedTile.offsetY!![i] * baseOffset / tileUnits).toFloat()
                            }
                            for (triangleIndex in 0 until shapedTile.triangleCount) {
                                val a = shapedTile.triangleA!![triangleIndex]
                                val b = shapedTile.triangleB!![triangleIndex]
                                val c = shapedTile.triangleC!![triangleIndex]
                                val xA = renderX[a.toInt()]
                                val xB = renderX[b.toInt()]
                                val xC = renderX[c.toInt()]
                                val yA = renderY[a.toInt()]
                                val yB = renderY[b.toInt()]
                                val yC = renderY[c.toInt()]

                                if (shapedTile.secondaryColors != null && shapedTile.secondaryColors!![triangleIndex] != -1) {
                                    val tileColor = shapedTile.secondaryColors!![triangleIndex]
                                    rasterizer.drawShadedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0f, 100.0f, 100.0f, ColorUtil.repackHsl(tileColor, shapedTile.light!![a.toInt()].toInt()).toFloat(), ColorUtil.repackHsl(tileColor, shapedTile.light!![b.toInt()].toInt()).toFloat(), ColorUtil.repackHsl(tileColor, shapedTile.light!![c.toInt()].toInt()).toFloat())
                                } else if (shapedTile.primaryMaterialIds != null && shapedTile.primaryMaterialIds!![triangleIndex] != (-1).toShort()) {
                                    val materialColor = toolkit.getMaterialColor(shapedTile.primaryMaterialIds!![triangleIndex].toInt())
                                    rasterizer.drawShadedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0f, 100.0f, 100.0f, ColorUtil.repackHsl(materialColor, shapedTile.light!![a.toInt()].toInt()).toFloat(), ColorUtil.repackHsl(materialColor, shapedTile.light!![b.toInt()].toInt()).toFloat(), ColorUtil.repackHsl(materialColor, shapedTile.light!![c.toInt()].toInt()).toFloat())
                                } else {
                                    val tileColor = shapedTile.primaryColors!![triangleIndex]
                                    rasterizer.drawShadedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, 100.0f, 100.0f, 100.0f, ColorUtil.repackHsl(tileColor, shapedTile.light!![a.toInt()].toInt()).toFloat(), ColorUtil.repackHsl(tileColor, shapedTile.light!![b.toInt()].toInt()).toFloat(), ColorUtil.repackHsl(tileColor, shapedTile.light!![c.toInt()].toInt()).toFloat())
                                }
                            }
                        }
                    }
                }
                offsetY -= baseOffset
            }
            offsetY = stepY + offY
            offsetX += baseOffset
        }
        rasterizer.smoothEdges = true
        toolkit.setDepthBuffering(enableZbuffer)
    }

    override fun createShadowAt(x: Int, y: Int, shadow: Shadow?): Shadow? = null

    override fun addShadow(shadow: Shadow?, scaledX: Int, averageHeight: Int, scaledY: Int) {
        // NOOP
    }

    override fun hasRenderingShadows(shadow: Shadow?, x: Int, planesHeightDelta: Int, y: Int): Boolean = false

    override fun renderMinimapFloor(
        var1: Int,
        var2: Int,
        tilesStartX: Int,
        tilesStartY: Int,
        tilesEndX: Int,
        tilesEndY: Int,
        tilesOnScreen: Array<BooleanArray>
    ) {
        val thread = toolkit.getThreadResource(Thread.currentThread())!!
        val rasterizer = thread.rasterizer!!
        rasterizer.blendingAlpha = 0
        rasterizer.clipEdges = true
        toolkit.clearDepth()

        if (plainBlendedTiles == null && blendedTexturedTiles == null) {
            if (scenePlainTiles != null) {
                renderLowDetailMinimap(var1, var2, tilesStartX, tilesStartY, tilesEndX, tilesEndY, tilesOnScreen, thread, rasterizer, thread.perspectiveX, thread.perspectiveY)
            }
        } else {
            renderHighDetailMinimap(var1, var2, tilesStartX, tilesStartY, tilesEndX, tilesEndY, tilesOnScreen, thread, rasterizer, thread.perspectiveX, thread.perspectiveY)
        }
    }

    override fun removeShadow(shadow: Shadow?, x: Int, groundOffset: Int, y: Int) {
        // NOOP
    }

    fun renderBlockTile(
        x: Int,
        y: Int,
        rasterizer: RasterizerSM,
        perspectiveX: FloatArray,
        perspectiveY: FloatArray,
        perspectiveZNear: FloatArray,
        perspectiveZFar: FloatArray,
        tileFlags: Int
    ) {
        val plainTile = scenePlainTiles!![x][y]
        if (plainTile != null) {
            if ((plainTile.functionMask.toInt() and HD_LIGHT) == 0) {
                if (tileFlags != 0) {
                    if ((plainTile.functionMask.toInt() and TEXTURE_MOVEMENT) != 0) {
                        if ((tileFlags and COMPLETELY_FLAT) != 0) return
                    } else if ((tileFlags and HD_LIGHT) != 0) {
                        return
                    }
                }

                val tileunitsStartX = tileUnits * x
                val tileunitsEndX = tileunitsStartX + tileUnits
                val tileunitsStartY = y * tileUnits
                val tileunitsEndY = tileunitsStartY + tileUnits

                val farZA: Float
                val farZB: Float
                val farZC: Float
                val farZD: Float
                val xA: Float
                val yA: Float
                val zA: Float
                val xB: Float
                val yB: Float
                val zB: Float
                val xC: Float
                val yC: Float
                val zC: Float
                val xD: Float
                val yD: Float
                val zD: Float

                if ((plainTile.functionMask.toInt() and COMPLETELY_FLAT) != 0) {
                    val tileHeight = tileHeights[x][y].toFloat()
                    val nearB = vertexZNearB * tileHeight
                    val farB = vertexZFarB * tileHeight

                    val nearZA = vertexZNearD + vertexZNearA * tileunitsStartX + nearB + vertexZNearC * tileunitsStartY
                    farZA = vertexZFarD + vertexZFarA * tileunitsStartX + farB + vertexZFarC * tileunitsStartY
                    if (nearZA < -farZA) return

                    val nearZB = vertexZNearD + vertexZNearA * tileunitsEndX + nearB + vertexZNearC * tileunitsStartY
                    farZB = vertexZFarD + vertexZFarA * tileunitsEndX + farB + vertexZFarC * tileunitsStartY
                    if (nearZB < -farZB) return

                    val nearZC = vertexZNearD + vertexZNearA * tileunitsEndX + nearB + vertexZNearC * tileunitsEndY
                    farZC = vertexZFarD + vertexZFarA * tileunitsEndX + farB + vertexZFarC * tileunitsEndY
                    if (nearZC < -farZC) return

                    val nearZD = vertexZNearD + vertexZNearA * tileunitsStartX + nearB + vertexZNearC * tileunitsEndY
                    farZD = vertexZFarD + vertexZFarA * tileunitsStartX + farB + vertexZFarC * tileunitsEndY
                    if (nearZD < -farZD) return

                    val heightXB = vertexXB * tileHeight
                    val heightYB = vertexYB * tileHeight

                    val viewXA = vertexXD + vertexXA * tileunitsStartX + heightXB + vertexXC * tileunitsStartY
                    xA = rasterizer.centerX + rasterizer.offsetX * viewXA / farZA
                    val viewYA = vertexYD + vertexYA * tileunitsStartX + heightYB + vertexYC * tileunitsStartY
                    yA = rasterizer.centerY + rasterizer.offsetY * viewYA / farZA
                    zA = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZA / farZA

                    val viewXB = vertexXD + vertexXA * tileunitsEndX + heightXB + vertexXC * tileunitsStartY
                    xB = rasterizer.centerX + rasterizer.offsetX * viewXB / farZB
                    val viewYB = vertexYD + vertexYA * tileunitsEndX + heightYB + vertexYC * tileunitsStartY
                    yB = rasterizer.centerY + rasterizer.offsetY * viewYB / farZB
                    zB = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZB / farZB

                    val viewXC = vertexXD + vertexXA * tileunitsEndX + heightXB + vertexXC * tileunitsEndY
                    xC = rasterizer.centerX + rasterizer.offsetX * viewXC / farZC
                    val viewYC = vertexYD + vertexYA * tileunitsEndX + heightYB + vertexYC * tileunitsEndY
                    yC = rasterizer.centerY + rasterizer.offsetY * viewYC / farZC
                    zC = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZC / farZC

                    val viewXD = vertexXD + vertexXA * tileunitsStartX + heightXB + vertexXC * tileunitsEndY
                    xD = rasterizer.centerX + rasterizer.offsetX * viewXD / farZD
                    val viewYD = vertexYD + vertexYA * tileunitsStartX + heightYB + vertexYC * tileunitsEndY
                    yD = rasterizer.centerY + rasterizer.offsetY * viewYD / farZD
                    zD = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZD / farZD
                } else {
                    val heightNorthWest = tileHeights[x][y]
                    val heightNorthEast = tileHeights[x + 1][y]
                    val heightSouthEast = tileHeights[x + 1][y + 1]
                    val heightSouthWest = tileHeights[x][y + 1]

                    val nearZA = vertexZNearD + vertexZNearA * tileunitsStartX + vertexZNearB * heightNorthWest + vertexZNearC * tileunitsStartY
                    farZA = vertexZFarD + vertexZFarA * tileunitsStartX + vertexZFarB * heightNorthWest + vertexZFarC * tileunitsStartY
                    if (nearZA < -farZA) return

                    val nearZB = vertexZNearD + vertexZNearA * tileunitsEndX + vertexZNearB * heightNorthEast + vertexZNearC * tileunitsStartY
                    farZB = vertexZFarD + vertexZFarA * tileunitsEndX + vertexZFarB * heightNorthEast + vertexZFarC * tileunitsStartY
                    if (nearZB < -farZB) return

                    val nearZC = vertexZNearD + vertexZNearA * tileunitsEndX + vertexZNearB * heightSouthEast + vertexZNearC * tileunitsEndY
                    farZC = vertexZFarD + vertexZFarA * tileunitsEndX + vertexZFarB * heightSouthEast + vertexZFarC * tileunitsEndY
                    if (nearZC < -farZC) return

                    val nearZD = vertexZNearD + vertexZNearA * tileunitsStartX + vertexZNearB * heightSouthWest + vertexZNearC * tileunitsEndY
                    farZD = vertexZFarD + vertexZFarA * tileunitsStartX + vertexZFarB * heightSouthWest + vertexZFarC * tileunitsEndY
                    if (nearZD < -farZD) return

                    val viewXA = vertexXD + vertexXA * tileunitsStartX + vertexXB * heightNorthWest + vertexXC * tileunitsStartY
                    xA = rasterizer.centerX + rasterizer.offsetX * viewXA / farZA
                    val viewYA = vertexYD + vertexYA * tileunitsStartX + vertexYB * heightNorthWest + vertexYC * tileunitsStartY
                    yA = rasterizer.centerY + rasterizer.offsetY * viewYA / farZA
                    zA = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZA / farZA

                    val viewXB = vertexXD + vertexXA * tileunitsEndX + vertexXB * heightNorthEast + vertexXC * tileunitsStartY
                    xB = rasterizer.centerX + rasterizer.offsetX * viewXB / farZB
                    val viewYB = vertexYD + vertexYA * tileunitsEndX + vertexYB * heightNorthEast + vertexYC * tileunitsStartY
                    yB = rasterizer.centerY + rasterizer.offsetY * viewYB / farZB
                    zB = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZB / farZB

                    val viewXC = vertexXD + vertexXA * tileunitsEndX + vertexXB * heightSouthEast + vertexXC * tileunitsEndY
                    xC = rasterizer.centerX + rasterizer.offsetX * viewXC / farZC
                    val viewYC = vertexYD + vertexYA * tileunitsEndX + vertexYB * heightSouthEast + vertexYC * tileunitsEndY
                    yC = rasterizer.centerY + rasterizer.offsetY * viewYC / farZC
                    zC = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZC / farZC

                    val viewXD = vertexXD + vertexXA * tileunitsStartX + vertexXB * heightSouthWest + vertexXC * tileunitsEndY
                    xD = rasterizer.centerX + rasterizer.offsetX * viewXD / farZD
                    val viewYD = vertexYD + vertexYA * tileunitsStartX + vertexYB * heightSouthWest + vertexYC * tileunitsEndY
                    yD = rasterizer.centerY + rasterizer.offsetY * viewYD / farZD
                    zD = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZD / farZD
                }

                // Triangle 1: C-D-B (NorthEast-NorthWest-SouthEast)
                if ((xC - xD) * (yB - yD) - (yC - yD) * (xB - xD) > 0.0f) {
                    rasterizer.clipEdges = xC < 0.0f || xD < 0.0f || xB < 0.0f || xC > rasterizer.clipWidth || xD > rasterizer.clipWidth || xB > rasterizer.clipWidth
                    if (plainTile.materialId >= 0) {
                        rasterizer.drawMaterializedTriangle(true, true, false, yC, yD, yB, xC, xD, xB, zC, zD, zB, farZC, farZD, farZB, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0xff000000.toInt() or (ColorUtil.hsvToRgbTable!![(plainTile.colorNorthEast.toInt() and 0xffff)] and 0xffffff), 0xff000000.toInt() or (ColorUtil.hsvToRgbTable!![(plainTile.colorNorthWest.toInt() and 0xffff)] and 0xffffff), 0xff000000.toInt() or (ColorUtil.hsvToRgbTable!![(plainTile.colorSouthEast.toInt() and 0xffff)] and 0xffffff), 0, 0.0f, 0.0f, 0.0f, plainTile.materialId.toInt())
                    } else {
                        rasterizer.drawShadedTriangle(true, true, false, yC, yD, yB, xC, xD, xB, zC, zD, zB, (plainTile.colorNorthEast.toInt() and 0xffff).toFloat(), (plainTile.colorNorthWest.toInt() and 0xffff).toFloat(), (plainTile.colorSouthEast.toInt() and 0xffff).toFloat())
                    }
                }

                // Triangle 2: A-B-D (SouthWest-SouthEast-NorthWest)
                if ((xA - xB) * (yD - yB) - (yA - yB) * (xD - xB) > 0.0f) {
                    rasterizer.clipEdges = xA < 0.0f || xB < 0.0f || xD < 0.0f || xA > rasterizer.clipWidth || xB > rasterizer.clipWidth || xD > rasterizer.clipWidth
                    if (plainTile.materialId >= 0) {
                        rasterizer.drawMaterializedTriangle(true, true, false, yA, yB, yD, xA, xB, xD, zA, zB, zD, farZA, farZB, farZD, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0xff000000.toInt() or (ColorUtil.hsvToRgbTable!![(plainTile.colorSouthWest.toInt() and 0xffff)] and 0xffffff), 0xff000000.toInt() or (ColorUtil.hsvToRgbTable!![(plainTile.colorSouthEast.toInt() and 0xffff)] and 0xffffff), 0xff000000.toInt() or (ColorUtil.hsvToRgbTable!![(plainTile.colorNorthWest.toInt() and 0xffff)] and 0xffffff), 0, 0.0f, 0.0f, 0.0f, plainTile.materialId.toInt())
                    } else {
                        rasterizer.drawShadedTriangle(true, true, false, yA, yB, yD, xA, xB, xD, zA, zB, zD, (plainTile.colorSouthWest.toInt() and 0xffff).toFloat(), (plainTile.colorSouthEast.toInt() and 0xffff).toFloat(), (plainTile.colorNorthWest.toInt() and 0xffff).toFloat())
                    }
                }
            }
        } else {
            val shapedTile = sceneShapedTiles!![x][y]
            if (shapedTile != null) {
                if (tileFlags != 0) {
                    if ((shapedTile.functionMask.toInt() and TEXTURE_MOVEMENT) != 0) {
                        if ((tileFlags and COMPLETELY_FLAT) != 0) return
                    } else if ((tileFlags and HD_LIGHT) != 0) {
                        return
                    }
                }

                for (triangleIndex in 0 until shapedTile.count) {
                    val offsetX = shapedTile.offsetX!![triangleIndex] + (x shl tileScale)
                    val offsetY = shapedTile.offsetY!![triangleIndex] + (y shl tileScale)
                    val offsetZ = shapedTile.offsetZ!![triangleIndex]
                    val nearZ = vertexZNearD + vertexZNearA * offsetX + vertexZNearB * offsetZ + vertexZNearC * offsetY
                    val farZ = vertexZFarD + vertexZFarA * offsetX + vertexZFarB * offsetZ + vertexZFarC * offsetY
                    if (nearZ < -farZ) return

                    val viewX = vertexXD + vertexXA * offsetX + vertexXB * offsetZ + vertexXC * offsetY
                    val viewY = vertexYD + vertexYA * offsetX + vertexYB * offsetZ + vertexYC * offsetY
                    perspectiveX[triangleIndex] = rasterizer.centerX + rasterizer.offsetX * viewX / farZ
                    perspectiveY[triangleIndex] = rasterizer.centerY + rasterizer.offsetY * viewY / farZ
                    perspectiveZNear[triangleIndex] = rasterizer.aFloat1682 + rasterizer.aFloat1683 * nearZ / farZ
                    perspectiveZFar[triangleIndex] = farZ
                }

                if (shapedTile.primaryMaterialIds != null) {
                    for (triangleIndex in 0 until shapedTile.triangleCount) {
                        val a = shapedTile.triangleA!![triangleIndex]
                        val b = shapedTile.triangleB!![triangleIndex]
                        val c = shapedTile.triangleC!![triangleIndex]
                        val xA = perspectiveX[a.toInt()]
                        val xB = perspectiveX[b.toInt()]
                        val xC = perspectiveX[c.toInt()]
                        val yA = perspectiveY[a.toInt()]
                        val yB = perspectiveY[b.toInt()]
                        val yC = perspectiveY[c.toInt()]

                        if ((xA - xB) * (yC - yB) - (yA - yB) * (xC - xB) > 0.0f) {
                            rasterizer.clipEdges = xA < 0.0f || xB < 0.0f || xC < 0.0f || xA > rasterizer.clipWidth || xB > rasterizer.clipWidth || xC > rasterizer.clipWidth
                            val materialA = shapedTile.primaryMaterialIds!![triangleIndex]
                            if (materialA != (-1).toShort()) {
                                rasterizer.drawMaterializedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, perspectiveZNear[a.toInt()], perspectiveZNear[b.toInt()], perspectiveZNear[c.toInt()], perspectiveZFar[a.toInt()], perspectiveZFar[b.toInt()], perspectiveZFar[c.toInt()], shapedTile.offsetX!![a.toInt()].toFloat() / tileUnits, shapedTile.offsetX!![b.toInt()].toFloat() / tileUnits, shapedTile.offsetX!![c.toInt()].toFloat() / tileUnits, shapedTile.offsetY!![a.toInt()].toFloat() / tileUnits, shapedTile.offsetY!![b.toInt()].toFloat() / tileUnits, shapedTile.offsetY!![c.toInt()].toFloat() / tileUnits, 0xff000000.toInt() or (ColorUtil.hsvToRgbTable!![(shapedTile.light!![a.toInt()].toInt() and 0xffff)] and 0xffffff), 0xff000000.toInt() or (ColorUtil.hsvToRgbTable!![(shapedTile.light!![b.toInt()].toInt() and 0xffff)] and 0xffffff), 0xff000000.toInt() or (ColorUtil.hsvToRgbTable!![(shapedTile.light!![c.toInt()].toInt() and 0xffff)] and 0xffffff), 0, 0.0f, 0.0f, 0.0f, materialA.toInt())
                            } else {
                                val primaryColor = shapedTile.primaryColors!![triangleIndex]
                                if (primaryColor != -1) {
                                    rasterizer.drawShadedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, perspectiveZNear[a.toInt()], perspectiveZNear[b.toInt()], perspectiveZNear[c.toInt()], ColorUtil.repackHsl(primaryColor, shapedTile.light!![a.toInt()].toInt()).toFloat(), ColorUtil.repackHsl(primaryColor, shapedTile.light!![b.toInt()].toInt()).toFloat(), ColorUtil.repackHsl(primaryColor, shapedTile.light!![c.toInt()].toInt()).toFloat())
                                }
                            }
                        }
                    }
                } else {
                    for (triangleIndex in 0 until shapedTile.triangleCount) {
                        val a = shapedTile.triangleA!![triangleIndex]
                        val b = shapedTile.triangleB!![triangleIndex]
                        val c = shapedTile.triangleC!![triangleIndex]
                        val xA = perspectiveX[a.toInt()]
                        val xB = perspectiveX[b.toInt()]
                        val xC = perspectiveX[c.toInt()]
                        val yA = perspectiveY[a.toInt()]
                        val yB = perspectiveY[b.toInt()]
                        val yC = perspectiveY[c.toInt()]

                        if ((xA - xB) * (yC - yB) - (yA - yB) * (xC - xB) > 0.0f) {
                            val primaryColor = shapedTile.primaryColors!![triangleIndex]
                            if (primaryColor != -1) {
                                rasterizer.clipEdges = xA < 0.0f || xB < 0.0f || xC < 0.0f || xA > rasterizer.clipWidth || xB > rasterizer.clipWidth || xC > rasterizer.clipWidth
                                rasterizer.drawShadedTriangle(true, true, false, yA, yB, yC, xA, xB, xC, perspectiveZNear[a.toInt()], perspectiveZNear[b.toInt()], perspectiveZNear[c.toInt()], ColorUtil.repackHsl(primaryColor, shapedTile.light!![a.toInt()].toInt()).toFloat(), ColorUtil.repackHsl(primaryColor, shapedTile.light!![b.toInt()].toInt()).toFloat(), ColorUtil.repackHsl(primaryColor, shapedTile.light!![c.toInt()].toInt()).toFloat())
                            }
                        }
                    }
                }
            }
        }
    }

    fun render(x: Int, y: Int) {
        val thread = toolkit.getThreadResource(Thread.currentThread())!!
        val rasterizer = thread.rasterizer!!
        rasterizer.blendingAlpha = 0
        when {
            plainBlendedTiles != null -> renderBlendedUntexturedTile(x, y, thread.hasWaterUpdate, thread, rasterizer, thread.perspectiveX, thread.perspectiveY, thread.perspectiveZNear, thread.perspectiveZFar, thread.aFloatArray2345, 0)
            scenePlainTiles != null -> renderBlockTile(x, y, rasterizer, thread.perspectiveX, thread.perspectiveY, thread.perspectiveZNear, thread.perspectiveZFar, 0)
            blendedTexturedTiles != null -> renderBlendedTexturesTile(x, y, thread.hasWaterUpdate, thread, rasterizer, thread.perspectiveX, thread.perspectiveY, thread.perspectiveZNear, thread.perspectiveZFar, thread.aFloatArray2345, 0)
        }
    }

    override fun addLight(light: PointLight, var2: IntArray) {
        // NOOP
    }
}
