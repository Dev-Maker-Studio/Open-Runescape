package com.jagex.game.runetek5.scene.culling

import com.jagex.game.runetek5.entity.gameobject.AbstractWall
import com.jagex.game.runetek5.graphics.model.particle.Silhouette
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.scene.tile.Tile
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.toolkit.SoftwareToolkit

/**
 * Rename to Culling / SceneCulling?
 * @author Brainpower
 *
 */
open class Culling(
    @JvmField val scene: Scene
) {
    companion object {
        @JvmField
        var activeToolkit: AbstractToolkit? = null
    }

    @JvmField
    var cullingEnabled: Boolean = true

    @JvmField
    var visibleAboveY: IntArray? = null

    @JvmField
    var visibleAboveX: IntArray? = null

    @JvmField
    var visibleBelowX: IntArray? = null

    @JvmField
    var visibleBelowY: IntArray? = null

    @JvmField
    var visibleBelowDrawheight: IntArray? = null

    @JvmField
    var wallCullingCursorPosition: Int = 0

    @JvmField
    var frusumCullingCursorPos: Int = 0

    @JvmField
    var oneThirdViewportWidth: Int = -1 //FIXME: naming

    @JvmField
    var oneThirdViewportHeight: Int = -1 //FIXME: naming

    @JvmField
    var anIntArray2565: IntArray? = null

    @JvmField
    var tileCullingClusterCount: Int = 0

    @JvmField
    var drawingScene: Boolean = true

    @JvmField
    var cullingClustersCleared: Boolean = false

    @JvmField
    val projection: FloatArray = FloatArray(3)

    @JvmField
    val viewport: IntArray = IntArray(2)

    @JvmField
    val benchmarkCulling: Boolean = true

    /**
     * Always 512 (128 << upscaling)
     */
    @JvmField
    val fullTileDimensionX: Int

    @JvmField
    val fullTileDimensionY: Int

    @JvmField
    val tileCullingCluster: Array<CullingCluster?>

    @JvmField
    val interactiveCulling: Array<CullingCluster?>

    @JvmField
    var interactiveCullingCursorPos: Int = 0

    @JvmField
    val diagonalWallCulling: Array<CullingCluster?>

    @JvmField
    val frustumClusters: Array<CullingCluster?>

    @JvmField
    val tileDrawCycles: Array<Array<IntArray>>

    @JvmField
    var frustumCulling: FrustumCulling? = null

    init {
        fullTileDimensionX = scene.fullTileDimension
        fullTileDimensionY = scene.fullTileDimension
        tileCullingCluster = arrayOfNulls(509)
        tileCullingClusterCount = 0
        interactiveCulling = arrayOfNulls(2034)
        interactiveCullingCursorPos = 0
        diagonalWallCulling = arrayOfNulls(1027)
        wallCullingCursorPosition = 0
        frustumClusters = arrayOfNulls(1005)
        frusumCullingCursorPos = 0
        tileDrawCycles = Array(scene.regionSizeZ) {
            Array(scene.sceneDimensionX + 1) {
                IntArray(scene.sceneDimensionY + 1)
            }
        }
        cullingClustersCleared = false
        cullingEnabled = scene.activeToolkit !is SoftwareToolkit
        if (cullingEnabled) {
            frustumCulling = FrustumCulling(this)
        }
    }

    /**
     *
     * @param displacement
     * @param z
     * @param x
     * @param y
     * @param cullY - 960 default (240 << scaling)
     * @param cullXZ
     */
    fun addWallCulling(displacement: Int, z: Int, x: Int, y: Int, cullY: Int, cullXZ: Int) {
        if (displacement != 8 && displacement != 16) {
            var tile = scene.tiles[z][x][y]
            if (tile == null) {
                tile = Tile(z)
                scene.tiles[z][x][y] = tile
            }
            if (displacement == 1) {
                // rotation % 1 == 0
                tile.cullY = cullY.toShort()
                tile.cullX = cullXZ.toShort()
            } else if (displacement == 2) {
                // rotation % 1 == 1
                tile.interactiveSideViewHeightOffset = cullY.toShort()
                tile.cullZ = cullXZ.toShort()
            }

            if (cullingClustersCleared) {
                removeInteractiveCullingClusters()
            }
        } else if (displacement == 8) {
            // Wall placement on tiles	[   ][ / ]
            //						  	[ / ][   ]
            val startX = x shl scene.tileUnits
            val endX = startX + scene.fullTileDimension
            val startY = y shl scene.tileUnits
            val endY = startY + scene.fullTileDimension
            val heightSouthWest = scene.surfaceHeightmap[z].getHeight(x, y)
            val heightNorthEast = scene.surfaceHeightmap[z].getHeight(x + 1, y + 1)
            diagonalWallCulling[wallCullingCursorPosition++] = CullingCluster(
                scene, 8, z,
                startX, endX, endX, startX, heightSouthWest, heightNorthEast, heightNorthEast - cullY, heightSouthWest - cullY,
                startY, endY, endY, startY
            )
        } else {
            // Wall placement on tiles	[ \ ][   ]
            //						  	[   ][ \ ]
            val startX = scene.fullTileDimension + (x shl scene.tileUnits)
            val endX = startX - scene.fullTileDimension
            val startY = y shl scene.tileUnits
            val endY = scene.fullTileDimension + startY
            val heightSouthEast = scene.surfaceHeightmap[z].getHeight(x + 1, y)
            val heightNorthWest = scene.surfaceHeightmap[z].getHeight(x, y + 1)
            diagonalWallCulling[wallCullingCursorPosition++] = CullingCluster(
                scene, displacement, z,
                startX, endX, endX, startX, heightSouthEast, heightNorthWest, heightNorthWest - cullY, heightSouthEast - cullY,
                startY, endY, endY, startY
            )
        }
    }

    fun clear() {
        removeInteractiveCullingClusters()
    }

    fun processCulling(toolkit: AbstractToolkit, nextPlane: Int) {
        activeToolkit = toolkit
        if (cullingEnabled && drawingScene) {
            if (benchmarkCulling) {
                scene.nanoTimer.getTime()
            }

            activeToolkit!!.accumulateViewportDimensions(viewport)
            if (oneThirdViewportWidth != (viewport[0] / 3.0f).toInt() || (viewport[1] / 3.0f).toInt() != oneThirdViewportHeight) {
                oneThirdViewportWidth = (viewport[0] / 3.0f).toInt()
                oneThirdViewportHeight = (viewport[1] / 3.0f).toInt()
                anIntArray2565 = IntArray(oneThirdViewportWidth * oneThirdViewportHeight)
            }

            frusumCullingCursorPos = 0
            for (index in 0 until tileCullingClusterCount) {
                processCullingCluster(activeToolkit!!, tileCullingCluster[index]!!, nextPlane)
            }

            for (index in 0 until interactiveCullingCursorPos) {
                processCullingCluster(activeToolkit!!, interactiveCulling[index]!!, nextPlane)
            }

            for (index in 0 until wallCullingCursorPosition) {
                processCullingCluster(activeToolkit!!, diagonalWallCulling[index]!!, nextPlane)
            }

            frustumCulling!!.anInt2588 = 0
            if (frusumCullingCursorPos > 0) {
                val size = anIntArray2565!!.size
                var i = 0
                while (i < size) {
                    anIntArray2565!![i++] = Int.MAX_VALUE
                }
                frustumCulling!!.state = 1
                for (index in 0 until frusumCullingCursorPos) {
                    val cluster = frustumClusters[index]!!

                    frustumCulling!!.cull(
                        cluster.projectionY[0].toInt(), cluster.projectionY[1].toInt(), cluster.projectionY[3].toInt(),
                        cluster.projectionX[0].toInt(), cluster.projectionX[1].toInt(), cluster.projectionX[3].toInt(),
                        cluster.projectionZ[0].toInt(), cluster.projectionZ[1].toInt(), cluster.projectionZ[3].toInt()
                    )

                    frustumCulling!!.cull(
                        cluster.projectionY[1].toInt(), cluster.projectionY[2].toInt(), cluster.projectionY[3].toInt(),
                        cluster.projectionX[1].toInt(), cluster.projectionX[2].toInt(), cluster.projectionX[3].toInt(),
                        cluster.projectionZ[1].toInt(), cluster.projectionZ[2].toInt(), cluster.projectionZ[3].toInt()
                    )
                }
                frustumCulling!!.state = 2
            }
            if (benchmarkCulling) {
                scene.nanoTimer.getTime()
            }
        } else {
            frusumCullingCursorPos = 0
        }
    }

    fun createTileCullingCluster(plane: Int, xEnd: Int, xStart: Int, i_5: Int, i_6: Int, i_7: Int, i_8: Int) {
        tileCullingCluster[tileCullingClusterCount++] = CullingCluster(scene, 4, plane, xEnd, xStart, xStart, xEnd, i_7, i_8, i_8, i_7, i_5, i_5, i_6, i_6)
    }

    fun calculateProjection(cluster: CullingCluster, cullingIndex: Int): Boolean {
        if (!projectedPositionHidden(cluster.tileCullingsX[cullingIndex], cluster.drawHeights[cullingIndex].toFloat(), cluster.tileCullingsZ[cullingIndex])) {
            return false
        }
        cluster.projectionX[cullingIndex] = projection[0].toInt().toShort()
        cluster.projectionY[cullingIndex] = projection[1].toInt().toShort()
        cluster.projectionZ[cullingIndex] = projection[2].toInt().toShort()
        return true
    }

    fun projectedPositionHidden(x: Int, y: Float, z: Int): Boolean {
        activeToolkit!!.calculateProjection(x.toFloat(), y, z.toFloat(), projection)
        if (projection[2] < 50.0f) {
            return false
        }
        projection[0] /= 3.0f
        projection[1] /= 3.0f
        return true
    }

    fun projectedPositionHidden(x: Int, y: Int, z: Int): Boolean {
        activeToolkit!!.calculateProjection(x.toFloat(), y.toFloat(), z.toFloat(), projection)
        if (projection[2] < 50.0f) {
            return false
        }
        projection[0] /= 3.0f
        projection[1] /= 3.0f
        return true
    }

    /**
     * Checks if a tile is hidden (underlay & overlay not rendered)
     */
    fun isTileHidden(plane: Int, x: Int, y: Int): Boolean {
        if (cullingEnabled && drawingScene) {
            if (frustumCulling!!.anInt2588 < 102) {
                return false
            }
            val lastDrawCycle = tileDrawCycles[plane][x][y]
            if (lastDrawCycle == -scene.tileDrawCycle) {
                return false
            }
            if (scene.tileDrawCycle == lastDrawCycle) {
                return true
            }
            if (scene.localHeightmap === scene.underwaterHeightmap) {
                return false
            }
            val upscaledX = x shl scene.tileUnits
            val upscaledZ = y shl scene.tileUnits
            if (isCulled(
                    upscaledX + 1,
                    scene.localHeightmap[plane].getHeight(x, y),
                    upscaledZ + 1,
                    upscaledX - 1 + scene.fullTileDimension,
                    scene.localHeightmap[plane].getHeight(x + 1, y + 1),
                    upscaledZ - 1 + scene.fullTileDimension,
                    upscaledX + 1,
                    scene.localHeightmap[plane].getHeight(x, y + 1),
                    upscaledZ - 1 + scene.fullTileDimension
                ) &&
                isCulled(
                    upscaledX + 1,
                    scene.localHeightmap[plane].getHeight(x, y),
                    upscaledZ + 1,
                    upscaledX - 1 + scene.fullTileDimension,
                    scene.localHeightmap[plane].getHeight(x + 1, y),
                    upscaledZ + 1,
                    upscaledX - 1 + scene.fullTileDimension,
                    scene.localHeightmap[plane].getHeight(x + 1, y + 1),
                    upscaledZ - 1 + scene.fullTileDimension
                )
            ) {
                tileDrawCycles[plane][x][y] = scene.tileDrawCycle
                return true
            }
            tileDrawCycles[plane][x][y] = -scene.tileDrawCycle
            return false
        }
        return false
    }

    fun isTileHidden(z: Int, x: Int, y: Int, minY: Int): Boolean {
        if (cullingEnabled && drawingScene) {
            if (frustumCulling!!.anInt2588 < 102) {
                return false
            }
            if (!isTileHidden(z, x, y)) {
                return false
            }
            val upscaledX = x shl scene.tileUnits
            val upscaledY = y shl scene.tileUnits
            return boundaryCulled(upscaledX, scene.localHeightmap[z].getHeight(x, y), upscaledY, scene.fullTileDimension, minY, scene.fullTileDimension)
        }
        return false
    }

    fun hideTile(z: Int, startX: Int, endX: Int, startY: Int, endY: Int, axisAlignedBoundingBox: Silhouette?): Boolean {
        if (cullingEnabled && drawingScene) {
            if (frustumCulling!!.anInt2588 < 102) {
                return false
            }
            if (endX == startX && endY == startY) {
                return isTileHidden(z, startX, startY) && hideBoundary(axisAlignedBoundingBox)
            }
            for (x in startX..endX) {
                for (y in startY..endY) {
                    if (tileDrawCycles[z][x][y] == -scene.tileDrawCycle) {
                        return false
                    }
                }
            }
            return hideBoundary(axisAlignedBoundingBox)
        }
        return false
    }

    fun hideWall(wall: AbstractWall, z: Int, x: Int, y: Int): Boolean {
        if (cullingEnabled && drawingScene) {
            if (frustumCulling!!.anInt2588 < 102) {
                return false
            }
            if (!isTileHidden(z, x, y)) {
                return false
            }
            val upscaledX = x shl scene.tileUnits
            val upscaledY = y shl scene.tileUnits
            val groundHeight = scene.localHeightmap[z].getHeight(x, y) - 1
            val wallDrawHeight = groundHeight + wall.getMinY()
            if (wall.cullingType.toInt() == 1) {
                return (isCulled(upscaledX, groundHeight, upscaledY, upscaledX, wallDrawHeight, upscaledY, upscaledX, wallDrawHeight, scene.fullTileDimension + upscaledY) &&
                        isCulled(upscaledX, groundHeight, upscaledY, upscaledX, wallDrawHeight, scene.fullTileDimension + upscaledY, upscaledX, groundHeight, scene.fullTileDimension + upscaledY))
            }
            if (wall.cullingType.toInt() == 2) {
                return (isCulled(upscaledX, groundHeight, scene.fullTileDimension + upscaledY, scene.fullTileDimension + upscaledX, wallDrawHeight, scene.fullTileDimension + upscaledY, upscaledX, wallDrawHeight, scene.fullTileDimension + upscaledY) &&
                        isCulled(upscaledX, groundHeight, scene.fullTileDimension + upscaledY, scene.fullTileDimension + upscaledX, groundHeight, scene.fullTileDimension + upscaledY, scene.fullTileDimension + upscaledX, wallDrawHeight, scene.fullTileDimension + upscaledY))
            }
            if (wall.cullingType.toInt() == 4) {
                return (isCulled(scene.fullTileDimension + upscaledX, groundHeight, upscaledY, scene.fullTileDimension + upscaledX, wallDrawHeight, upscaledY, scene.fullTileDimension + upscaledX, wallDrawHeight, scene.fullTileDimension + upscaledY) &&
                        isCulled(scene.fullTileDimension + upscaledX, groundHeight, upscaledY, scene.fullTileDimension + upscaledX, wallDrawHeight, scene.fullTileDimension + upscaledY, scene.fullTileDimension + upscaledX, groundHeight, scene.fullTileDimension + upscaledY))
            }
            if (wall.cullingType.toInt() == 8) {
                return (isCulled(upscaledX, groundHeight, upscaledY, scene.fullTileDimension + upscaledX, wallDrawHeight, upscaledY, upscaledX, wallDrawHeight, upscaledY) &&
                        isCulled(upscaledX, groundHeight, upscaledY, scene.fullTileDimension + upscaledX, groundHeight, upscaledY, scene.fullTileDimension + upscaledX, wallDrawHeight, upscaledY))
            }
            if (wall.cullingType.toInt() == 16) {
                return boundaryCulled(upscaledX, groundHeight, scene.halfTileOffset + upscaledY, scene.halfTileOffset, wallDrawHeight, scene.halfTileOffset)
            }
            if (wall.cullingType.toInt() == 32) {
                return boundaryCulled(scene.halfTileOffset + upscaledX, groundHeight, scene.halfTileOffset + upscaledY, scene.halfTileOffset, wallDrawHeight, scene.halfTileOffset)
            }
            if (wall.cullingType.toInt() == 64) {
                return boundaryCulled(scene.halfTileOffset + upscaledX, groundHeight, upscaledY, scene.halfTileOffset, wallDrawHeight, scene.halfTileOffset)
            }
            if (wall.cullingType.toInt() == 128) {
                return boundaryCulled(upscaledX, groundHeight, upscaledY, scene.halfTileOffset, wallDrawHeight, scene.halfTileOffset)
            }
            return true
        }
        return false
    }

    /**
     *
     * @param minX - the lowest width coordinate
     * @param minY - the lowest height coordinate
     * @param minZ - the lowest length coordinate
     * @param sizeX - the width
     * @param sizeY - the height
     * @param sizeZ - the length
     * @return - culled
     */
    fun boundaryCulled(minX: Int, minY: Int, minZ: Int, sizeX: Int, sizeY: Int, sizeZ: Int): Boolean {
        val maxX = sizeX + minX
        val maxY = sizeY + minY
        val maxZ = sizeZ + minZ
        if (!isCulled(minX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ)) {
            return false
        }
        if (!isCulled(minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ)) {
            return false
        }
        if (minX < scene.cameraPositionX) {
            if (!isCulled(minX, minY, maxZ, minX, maxY, minZ, minX, maxY, maxZ)) {
                return false
            }

            if (!isCulled(minX, minY, maxZ, minX, minY, minZ, minX, maxY, minZ)) {
                return false
            }
        } else {
            if (!isCulled(maxX, minY, maxZ, maxX, maxY, minZ, maxX, maxY, maxZ)) {
                return false
            }

            if (!isCulled(maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ)) {
                return false
            }
        }
        if (minZ < scene.cameraPositionZ) {
            if (!isCulled(minX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ)) {
                return false
            }

            return isCulled(minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ)
        } else {
            if (!isCulled(minX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ)) {
                return false
            }
            return isCulled(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ)
        }
    }

    //max_x, min_y, max_z, max_x, min_y, min_z, max_x, max_y, min_z
    fun isCulled(xA: Int, yA: Int, zA: Int, xB: Int, yB: Int, zB: Int, xC: Int, yC: Int, zC: Int): Boolean {
        if (!projectedPositionHidden(xA, yA, zA)) {
            return false
        }
        val xAProj = projection[0].toInt()
        val yAProj = projection[1].toInt()
        val zAProj = projection[2].toInt()
        if (!projectedPositionHidden(xB, yB, zB)) {
            return false
        }
        val xBProj = projection[0].toInt()
        val yBProj = projection[1].toInt()
        val zBProj = projection[2].toInt()
        if (!projectedPositionHidden(xC, yC, zC)) {
            return false
        }
        val xCProj = projection[0].toInt()
        val yCProj = projection[1].toInt()
        val zCProj = projection[2].toInt()
        return frustumCulling!!.cull(yAProj, yBProj, yCProj, xAProj, xBProj, xCProj, zAProj, zBProj, zCProj)
    }

    fun processCullingCluster(toolkit: AbstractToolkit, cullingCluster: CullingCluster, plane: Int) {
        activeToolkit = toolkit

        /**
         * Check if rendering the tile should be forced
         */
        if (visibleBelowDrawheight != null && cullingCluster.plane >= plane) {
            for (index in visibleBelowDrawheight!!.indices) {
                if (visibleBelowDrawheight!![index] != -1000000 &&
                    (cullingCluster.drawHeights[0] <= visibleBelowDrawheight!![index]
                            || cullingCluster.drawHeights[1] <= visibleBelowDrawheight!![index]
                            || cullingCluster.drawHeights[2] <= visibleBelowDrawheight!![index]
                            || cullingCluster.drawHeights[3] <= visibleBelowDrawheight!![index]) &&
                    (cullingCluster.tileCullingsX[0] <= visibleBelowX!![index]
                            || cullingCluster.tileCullingsX[1] <= visibleBelowX!![index]
                            || cullingCluster.tileCullingsX[2] <= visibleBelowX!![index]
                            || cullingCluster.tileCullingsX[3] <= visibleBelowX!![index]) &&
                    (cullingCluster.tileCullingsX[0] >= visibleAboveX!![index]
                            || cullingCluster.tileCullingsX[1] >= visibleAboveX!![index]
                            || cullingCluster.tileCullingsX[2] >= visibleAboveX!![index]
                            || cullingCluster.tileCullingsX[3] >= visibleAboveX!![index]) &&
                    (cullingCluster.tileCullingsZ[0] <= visibleBelowY!![index]
                            || cullingCluster.tileCullingsZ[1] <= visibleBelowY!![index]
                            || cullingCluster.tileCullingsZ[2] <= visibleBelowY!![index]
                            || cullingCluster.tileCullingsZ[3] <= visibleBelowY!![index]) &&
                    (cullingCluster.tileCullingsZ[0] >= visibleAboveY!![index]
                            || cullingCluster.tileCullingsZ[1] >= visibleAboveY!![index]
                            || cullingCluster.tileCullingsZ[2] >= visibleAboveY!![index]
                            || cullingCluster.tileCullingsZ[3] >= visibleAboveY!![index])
                ) {
                    return // Ensure tile is rendered
                }
            }
        }
        if (cullingCluster.type.toInt() == 1) {
            val sceneX = scene.visibleTiles + (cullingCluster.startX - scene.cameraGridPositionX)
            if (sceneX in 0..(scene.visibleTiles + scene.visibleTiles)) {
                var sceneStartY = scene.visibleTiles + (cullingCluster.startY - scene.cameraGridPositionY)
                if (sceneStartY < 0) {
                    sceneStartY = 0
                } else if (sceneStartY > scene.visibleTiles + scene.visibleTiles) {
                    return
                }
                var sceneEndY = scene.visibleTiles + (cullingCluster.endY - scene.cameraGridPositionY)
                if (sceneEndY > scene.visibleTiles + scene.visibleTiles) {
                    sceneEndY = scene.visibleTiles + scene.visibleTiles
                } else if (sceneEndY < 0) {
                    return
                }
                var onScreen = false
                var tempSceneStartY = sceneStartY
                while (tempSceneStartY <= sceneEndY) {
                    if (scene.adjacent_tile_on_screen[sceneX][tempSceneStartY++]) {
                        onScreen = true
                        break
                    }
                }
                if (onScreen) {
                    var cameraOffsetX = (scene.cameraPositionX - cullingCluster.tileCullingsX[0].toFloat())
                    if (cameraOffsetX < 0.0f) {
                        cameraOffsetX *= -1.0f
                    }
                    if (cameraOffsetX >= fullTileDimensionX.toFloat() &&
                        calculateProjection(cullingCluster, 0) &&
                        calculateProjection(cullingCluster, 1) &&
                        calculateProjection(cullingCluster, 2) &&
                        calculateProjection(cullingCluster, 3)
                    ) {
                        frustumClusters[frusumCullingCursorPos++] = cullingCluster
                    }
                }
            }
        } else if (cullingCluster.type.toInt() == 2) {
            val y = scene.visibleTiles + (cullingCluster.startY - scene.cameraGridPositionY)
            if (y in 0..(scene.visibleTiles + scene.visibleTiles)) {
                var startX = scene.visibleTiles + (cullingCluster.startX - scene.cameraGridPositionX)
                if (startX < 0) {
                    startX = 0
                } else if (startX > scene.visibleTiles + scene.visibleTiles) {
                    return
                }
                var endX = scene.visibleTiles + (cullingCluster.endX - scene.cameraGridPositionX)
                if (endX > scene.visibleTiles + scene.visibleTiles) {
                    endX = scene.visibleTiles + scene.visibleTiles
                } else if (endX < 0) {
                    return
                }
                var onScreen = false
                var tempStartX = startX
                while (tempStartX <= endX) {
                    if (scene.adjacent_tile_on_screen[tempStartX++][y]) {
                        onScreen = true
                        break
                    }
                }
                if (onScreen) {
                    var cameraOffsetY = (scene.cameraPositionZ - cullingCluster.tileCullingsZ[0].toFloat())
                    if (cameraOffsetY < 0.0f) {
                        cameraOffsetY *= -1.0f
                    }
                    if (cameraOffsetY >= fullTileDimensionX.toFloat() &&
                        calculateProjection(cullingCluster, 0) &&
                        calculateProjection(cullingCluster, 1) &&
                        calculateProjection(cullingCluster, 2) &&
                        calculateProjection(cullingCluster, 3)
                    ) {
                        frustumClusters[frusumCullingCursorPos++] = cullingCluster
                    }
                }
            }
        } else if (cullingCluster.type.toInt() != 16 && cullingCluster.type.toInt() != 8) {
            if (cullingCluster.type.toInt() == 4) {
                val f_12 = (cullingCluster.drawHeights[0] - scene.cameraPositionY)
                if (f_12 > fullTileDimensionY) {
                    var startY = scene.visibleTiles + (cullingCluster.startY - scene.cameraGridPositionY)
                    if (startY < 0) {
                        startY = 0
                    } else if (startY > scene.visibleTiles + scene.visibleTiles) {
                        return
                    }
                    var endY = scene.visibleTiles + (cullingCluster.endY - scene.cameraGridPositionY)
                    if (endY > scene.visibleTiles + scene.visibleTiles) {
                        endY = scene.visibleTiles + scene.visibleTiles
                    } else if (endY < 0) {
                        return
                    }
                    var startX = scene.visibleTiles + (cullingCluster.startX - scene.cameraGridPositionX)
                    if (startX < 0) {
                        startX = 0
                    } else if (startX > scene.visibleTiles + scene.visibleTiles) {
                        return
                    }
                    var endX = scene.visibleTiles + (cullingCluster.endX - scene.cameraGridPositionX)
                    if (endX > scene.visibleTiles + scene.visibleTiles) {
                        endX = scene.visibleTiles + scene.visibleTiles
                    } else if (endX < 0) {
                        return
                    }
                    var onScreen = false
                    CheckOnScreen@ for (x in startX..endX) {
                        for (y in startY..endY) {
                            if (scene.adjacent_tile_on_screen[x][y]) {
                                onScreen = true
                                break@CheckOnScreen
                            }
                        }
                    }
                    if (onScreen &&
                        calculateProjection(cullingCluster, 0) &&
                        calculateProjection(cullingCluster, 1) &&
                        calculateProjection(cullingCluster, 2) &&
                        calculateProjection(cullingCluster, 3)
                    ) {
                        frustumClusters[frusumCullingCursorPos++] = cullingCluster
                    }
                }
            }
        } else {
            val startX = scene.visibleTiles + (cullingCluster.startX - scene.cameraGridPositionX)
            if (startX in 0..(scene.visibleTiles + scene.visibleTiles)) {
                val startY = scene.visibleTiles + (cullingCluster.startY - scene.cameraGridPositionY)
                if (startY in 0..(scene.visibleTiles + scene.visibleTiles) && scene.adjacent_tile_on_screen[startX][startY]) {
                    var cameraOffsetX = (scene.cameraPositionX - cullingCluster.tileCullingsX[0].toFloat())
                    if (cameraOffsetX < 0.0f) {
                        cameraOffsetX *= -1.0f
                    }
                    var cameraOffsetY = (scene.cameraPositionZ - cullingCluster.tileCullingsZ[0].toFloat())
                    if (cameraOffsetY < 0.0f) {
                        cameraOffsetY *= -1.0f
                    }
                    if ((cameraOffsetX >= fullTileDimensionX.toFloat() || cameraOffsetY >= fullTileDimensionX.toFloat()) &&
                        calculateProjection(cullingCluster, 0) &&
                        calculateProjection(cullingCluster, 1) &&
                        calculateProjection(cullingCluster, 2) &&
                        calculateProjection(cullingCluster, 3)
                    ) {
                        frustumClusters[frusumCullingCursorPos++] = cullingCluster
                    }
                }
            }
        }
    }

    fun removeCullingCluster(displacement: Int, z: Int, x: Int, y: Int) {
        if (displacement != 8 && displacement != 16) { // interactive
            val tile = scene.tiles[z][x][y]
            if (tile != null) {
                if (displacement == 1) {
                    tile.cullY = 0
                } else if (displacement == 2) {
                    tile.interactiveSideViewHeightOffset = 0
                }
            }
            removeInteractiveCullingClusters()
        } else { // non interactive
            for (clusterIndex in 0 until wallCullingCursorPosition) {
                val wallCluster = diagonalWallCulling[clusterIndex]!!
                if (wallCluster.type.toInt() == displacement && z == wallCluster.plane.toInt() && (x == wallCluster.startX.toInt() && y == wallCluster.startY.toInt() || x == wallCluster.endX.toInt() && y == wallCluster.endY.toInt())) {
                    System.arraycopy(diagonalWallCulling, clusterIndex + 1, diagonalWallCulling, clusterIndex, diagonalWallCulling.size - (clusterIndex + 1))
                    wallCullingCursorPosition--
                    break
                }
            }
        }
    }

    fun hideBoundary(axisAlignedBoundingBox: Silhouette?): Boolean {
        return axisAlignedBoundingBox != null && boundaryCulled(
            axisAlignedBoundingBox.minX, axisAlignedBoundingBox.minY, axisAlignedBoundingBox.minZ,
            axisAlignedBoundingBox.maxX - axisAlignedBoundingBox.minX,
            axisAlignedBoundingBox.maxY - axisAlignedBoundingBox.minY,
            axisAlignedBoundingBox.maxZ - axisAlignedBoundingBox.minZ
        )
    }

    fun removeInteractiveCullingClusters() {
        for (index in 0 until interactiveCullingCursorPos) {
            interactiveCulling[index] = null
        }
        interactiveCullingCursorPos = 0
        for (scenePlane in 0 until scene.regionSizeZ) {
            for (sceneY in 0 until scene.sceneDimensionX) {
                for (sceneX in 0 until scene.sceneDimensionY) {
                    val tile = scene.tiles[scenePlane][sceneX][sceneY]
                    if (tile != null) {
                        if (tile.cullY > 0) {
                            tile.cullY = (tile.cullY * -1).toShort()
                        }
                        if (tile.interactiveSideViewHeightOffset > 0) {
                            tile.interactiveSideViewHeightOffset = (tile.interactiveSideViewHeightOffset * -1).toShort()
                        }
                    }
                }
            }
        }
        for (scenePlane in 0 until scene.regionSizeZ) {
            for (sceneY in 0 until scene.sceneDimensionX) {
                for (sceneX in 0 until scene.sceneDimensionY) {
                    val tile = scene.tiles[scenePlane][sceneX][sceneY]
                    if (tile != null) {
                        val hasBridge = scene.tiles[0][sceneX][sceneY] != null && scene.tiles[0][sceneX][sceneY]!!.bridge != null
                        if (tile.cullY.toInt() < 0) {
                            var yStart = sceneY
                            var yEnd = sceneY
                            val zEnd = scenePlane
                            var nextTile = scene.tiles[scenePlane][sceneX][sceneY - 1]
                            val count = scene.surfaceHeightmap[scenePlane].getHeight(sceneX, sceneY)
                            while (nextTile != null && nextTile.cullY.toInt() < 0 && nextTile.cullY.toInt() == tile.cullY.toInt() && nextTile.cullX.toInt() == tile.cullX.toInt() && count == scene.surfaceHeightmap[scenePlane].getHeight(sceneX, yStart - 1) && (yStart - 1 <= 0 || count == scene.surfaceHeightmap[scenePlane].getHeight(sceneX, yStart - 2)) && yEnd - yStart <= 10) {
                                yStart--
                                nextTile = scene.tiles[scenePlane][sceneX][yStart - 1]
                            }
                            nextTile = scene.tiles[scenePlane][sceneX][yEnd + 1]
                            while (yEnd < scene.sceneDimensionY && nextTile != null && nextTile.cullY.toInt() < 0 && nextTile.cullY.toInt() == tile.cullY.toInt() && nextTile.cullX.toInt() == tile.cullX.toInt() && count == scene.surfaceHeightmap[scenePlane].getHeight(sceneX, yEnd + 1) && (yEnd + 1 >= scene.sceneDimensionY || count == scene.surfaceHeightmap[scenePlane].getHeight(sceneX, yEnd + 2)) && yEnd - yStart <= 10) {
                                yEnd++
                                nextTile = scene.tiles[scenePlane][sceneX][yEnd + 1]
                            }
                            val highestSouthWest = scene.surfaceHeightmap[if (hasBridge) scenePlane + 1 else scenePlane].getHeight(sceneX, yStart)
                            val i_14 = highestSouthWest + tile.cullY
                            val heighestNorthWest = scene.surfaceHeightmap[if (hasBridge) scenePlane + 1 else scenePlane].getHeight(sceneX, yEnd + 1)
                            val i_16 = heighestNorthWest + tile.cullY
                            val mainX = sceneX shl scene.tileUnits
                            val tileEndY2 = yStart shl scene.tileUnits
                            val i_19 = (yEnd shl scene.tileUnits) + scene.fullTileDimension
                            interactiveCulling[interactiveCullingCursorPos++] = CullingCluster(
                                scene, 1, scenePlane,
                                mainX + tile.cullX, mainX + tile.cullX, mainX + tile.cullX, mainX + tile.cullX,
                                highestSouthWest, heighestNorthWest, i_16, i_14,
                                tileEndY2, i_19, i_19, tileEndY2
                            )
                            for (tilePlane in scenePlane..zEnd) {
                                for (tileY in yStart..yEnd) {
                                    scene.tiles[tilePlane][sceneX][tileY]!!.cullY = (scene.tiles[tilePlane][sceneX][tileY]!!.cullY * -1).toShort()
                                }
                            }
                        }
                        if (tile.interactiveSideViewHeightOffset.toInt() < 0) {
                            var xStart = sceneX
                            var xEnd = sceneX
                            val zEnd = scenePlane
                            var nextTile = scene.tiles[scenePlane][sceneX - 1][sceneY]
                            val drawHeight = scene.surfaceHeightmap[scenePlane].getHeight(sceneX, sceneY)
                            while (nextTile != null &&
                                nextTile.interactiveSideViewHeightOffset.toInt() < 0 &&
                                nextTile.interactiveSideViewHeightOffset.toInt() == tile.interactiveSideViewHeightOffset.toInt() &&
                                nextTile.cullZ.toInt() == tile.cullZ.toInt() &&
                                drawHeight == scene.surfaceHeightmap[scenePlane].getHeight(xStart - 1, sceneY) &&
                                (xStart - 1 <= 0 || drawHeight == scene.surfaceHeightmap[scenePlane].getHeight(xStart - 2, sceneY)) &&
                                xEnd - xStart <= 10
                            ) {
                                xStart--
                                nextTile = scene.tiles[scenePlane][xStart - 1][sceneY]
                            }
                            nextTile = scene.tiles[scenePlane][xEnd + 1][sceneY]
                            while (xEnd < scene.sceneDimensionX && nextTile != null && nextTile.interactiveSideViewHeightOffset.toInt() < 0 &&
                                nextTile.interactiveSideViewHeightOffset.toInt() == tile.interactiveSideViewHeightOffset.toInt() &&
                                nextTile.cullZ.toInt() == tile.cullZ.toInt() && drawHeight == scene.surfaceHeightmap[scenePlane].getHeight(xEnd + 1, sceneY) &&
                                (xEnd + 1 >= scene.sceneDimensionX || drawHeight == scene.surfaceHeightmap[scenePlane].getHeight(xEnd + 2, sceneY)) &&
                                xEnd - xStart <= 10
                            ) {
                                xEnd++
                                nextTile = scene.tiles[scenePlane][xEnd + 1][sceneY]
                            }
                            val heightSouthWest = scene.surfaceHeightmap[if (hasBridge) scenePlane + 1 else scenePlane].getHeight(xStart, sceneY)
                            val i_14 = heightSouthWest + tile.interactiveSideViewHeightOffset
                            val heightSouthEast = scene.surfaceHeightmap[if (hasBridge) scenePlane + 1 else scenePlane].getHeight(xEnd + 1, sceneY)
                            val i_16 = heightSouthEast + tile.interactiveSideViewHeightOffset
                            val startX = xStart shl scene.tileUnits
                            val endX = (xEnd shl scene.tileUnits) + scene.fullTileDimension
                            val startY = sceneY shl scene.tileUnits
                            interactiveCulling[interactiveCullingCursorPos++] = CullingCluster(
                                scene, 2, scenePlane,
                                startX, endX, endX, startX,
                                heightSouthWest, heightSouthEast, i_16, i_14,
                                startY + tile.cullZ, startY + tile.cullZ, startY + tile.cullZ, startY + tile.cullZ
                            )
                            for (tilePlane in scenePlane..zEnd) {
                                for (tileX in xStart..xEnd) {
                                    scene.tiles[tilePlane][tileX][sceneY]!!.interactiveSideViewHeightOffset = (scene.tiles[tilePlane][tileX][sceneY]!!.interactiveSideViewHeightOffset * -1).toShort()
                                }
                            }
                        }
                    }
                }
            }
        }
        cullingClustersCleared = true
    }
}
