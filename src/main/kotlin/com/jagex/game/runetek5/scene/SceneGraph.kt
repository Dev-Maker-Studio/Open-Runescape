package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.entity.gameobject.GameObject
import com.jagex.game.runetek5.entity.gameobject.AbstractGroundDecoration
import com.jagex.game.runetek5.entity.gameobject.AbstractWall
import com.jagex.game.runetek5.entity.gameobject.AbstractWallDecoration
import com.jagex.game.runetek5.entity.gameobject.AnimatedGroundDecoration
import com.jagex.game.runetek5.entity.gameobject.AnimatedInteractiveObject
import com.jagex.game.runetek5.entity.gameobject.AnimatedWall
import com.jagex.game.runetek5.entity.gameobject.AnimatedWallDecoration
import com.jagex.game.runetek5.entity.gameobject.StationaryGroundDecoration
import com.jagex.game.runetek5.entity.gameobject.StationaryInteractiveObject
import com.jagex.game.runetek5.entity.gameobject.StationaryWall
import com.jagex.game.runetek5.entity.gameobject.StationaryWallDecoration
import com.jagex.game.runetek5.graphics.light.FlickeringEffect
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.scene.environment.EnvironmentManager
import com.jagex.game.runetek5.scene.environment.Environment
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.sound.player.area.AmbientSound
import com.jagex.game.runetek5.toolkit.AbstractToolkit

open class SceneGraph(
    scene: Scene,
    val locTypeList: ObjectTypeList,
    dimensionZ: Int,
    dimensionX: Int,
    dimensionY: Int,
    hideOverlay: Boolean,
    regionMap: SettingsBits,
    val environmentManager: EnvironmentManager
) : MapLoader(scene, dimensionZ, dimensionX, dimensionY, hideOverlay, TypeLists.FLO!!, TypeLists.FLU!!, regionMap) {

    companion object {
        @JvmStatic
        var soundsOnly = false

        @JvmStatic
        var cullingAdjustment = false

        @JvmField
        val WALL_DECORATION_DISPLACE_X = intArrayOf(1, 0, -1, 0)
        @JvmField
        val WALL_DECORATION_DISPLACE_Y = intArrayOf(0, -1, 0, 1)

        @JvmField
        val ANIMATED_WALL_DECORATION_DISPLACE_X = intArrayOf(1, -1, -1, 1)
        @JvmField
        val ANIMATED_WALL_DECORATION_DISPLACE_Y = intArrayOf(-1, -1, 1, 1)

        @JvmStatic
        fun loadLocs(locTypeList: ObjectTypeList, mapdata: ByteArray, x: Int, y: Int, regionSizeX: Int, regionSizeY: Int): Int {
            var incompleteLocs = 0
            val stream = Packet(mapdata)
            var locId = -1
            label56@ while (true) {
                val idOffset = stream.readSum()
                if (idOffset == 0) {
                    return incompleteLocs
                }
                locId += idOffset
                var localPositionInfo = 0
                var cached = false
                while (true) {
                    var offset: Int
                    while (!cached) {
                        offset = stream.readUSmart()
                        if (offset == 0) {
                            continue@label56
                        }
                        localPositionInfo += offset - 1
                        val deltaY = localPositionInfo and 0x3f
                        val deltaX = localPositionInfo shr 6 and 0x3f
                        val type = stream.readUByte() shr 2
                        val sceneX = deltaX + x
                        val sceneY = y + deltaY
                        if (sceneX > 0 && sceneY > 0 && sceneX < regionSizeX - 1 && sceneY < regionSizeY - 1) {
                            val loctype = locTypeList.list(locId)
                            if (type != ObjectShapes.GROUND_DECORATION.shape || PreferenceManager.preferences!!.groundDecoration!!.getValue() != 0 || loctype!!.hasActions != 0 || loctype.blocksMovement == 1 || loctype.forceDisplayDecoration) {
                                if (!loctype.fullyCached()) {
                                    incompleteLocs++
                                }
                                cached = true
                            }
                        }
                    }
                    offset = stream.readUSmart()
                    if (offset == 0) {
                        break
                    }
                    stream.readUByte()
                }
            }
        }

        @JvmStatic
        fun animateObject(plane: Int, x: Int, y: Int, type: Int, shape: Int, rotation: Int, sequence: Int) {
            if (x >= 0 && y >= 0 && x < Resource.mapRegion!!.sceneSizeX - 1 && y < Resource.mapRegion!!.sceneSizeY - 1 && Resource.mapRegion!!.getScene()!! != null) {
                if (type == 0) {
                    val primaryWall = Resource.mapRegion!!.getScene()!!!!.getWall(plane, x, y) as? GameObject
                    val secondaryWall = Resource.mapRegion!!.getScene()!!!!.getSecondaryWall(plane, x, y) as? GameObject
                    if (primaryWall != null && shape != 2) {
                        if (primaryWall is AnimatedWall) {
                            primaryWall.gameObject.animate(sequence)
                        } else {
                            MapRegion.transformObject(plane, 0, x, y, primaryWall.id, rotation, shape, sequence)
                        }
                    }
                    if (secondaryWall != null) {
                        if (secondaryWall is AnimatedWall) {
                            secondaryWall.gameObject.animate(sequence)
                        } else {
                            MapRegion.transformObject(plane, type, x, y, secondaryWall.id, rotation, shape, sequence)
                        }
                    }
                } else if (type == 1) {
                    val wallDecoration = Resource.mapRegion!!.getScene()!!!!.getPrimaryWallDecoration(plane, x, y) as? GameObject
                    if (wallDecoration != null) {
                        if (wallDecoration is AnimatedWallDecoration) {
                            wallDecoration.wallDecoration.animate(sequence)
                        } else {
                            val id = wallDecoration.id
                            var shapeVar = shape
                            if (shape != 4 && shape != 5) {
                                if (shape == 6) {
                                    MapRegion.transformObject(plane, 1, x, y, id, rotation + 4, 4, sequence)
                                } else if (shape == 7) {
                                    MapRegion.transformObject(plane, type, x, y, id, (rotation + 2 and 0x3) + 4, 4, sequence)
                                } else if (shape == 8) {
                                    MapRegion.transformObject(plane, type, x, y, id, rotation + 4, 4, sequence)
                                    MapRegion.transformObject(plane, type, x, y, id, (rotation + 2 and 0x3) + 4, 4, sequence)
                                }
                            } else {
                                MapRegion.transformObject(plane, type, x, y, id, rotation, 4, sequence)
                            }
                        }
                    }
                } else if (type == 2) {
                    val interactive = Resource.mapRegion!!.getScene()!!!!.getInteractiveObject(plane, x, y, Client.INTERACTIVE_OBJECT_COMPARABLE) as? GameObject
                    if (interactive != null) {
                        var shapeVar = shape
                        if (shape == 11) {
                            shapeVar = 10
                        }
                        if (interactive is AnimatedInteractiveObject) {
                            interactive.activeGameObject.animate(sequence)
                        } else {
                            MapRegion.transformObject(plane, 2, x, y, interactive.id, rotation, shapeVar, sequence)
                        }
                    }
                } else if (type == 3) {
                    val groundDecoration = Resource.mapRegion!!.getScene()!!!!.getGroundDecoration(plane, x, y) as? GameObject
                    if (groundDecoration != null) {
                        if (groundDecoration is AnimatedGroundDecoration) {
                            groundDecoration.gameObject.animate(sequence)
                        } else {
                            MapRegion.transformObject(plane, 3, x, y, groundDecoration.id, rotation, shape, sequence)
                        }
                    }
                }
            }
        }
    }

    var activeLogicHeight = 99

    fun addObject(toolkit: AbstractToolkit, locPlane: Int, logicHeight: Int, baseX: Int, baseY: Int, locId: Int, locRotation: Int, locShape: Int, collisionMap: CollisionMap?, sequenceId: Int) {
        if (logicHeight < activeLogicHeight) {
            activeLogicHeight = logicHeight
        }
        val locType = locTypeList.list(locId)!!
        if (PreferenceManager.preferences!!.textures!!.getValue() != 0 || !locType.requiresTextures) {
            val locSizeX: Int
            val locSizeY: Int
            if (locRotation != 1 && locRotation != 3) {
                locSizeX = locType.sizeX
                locSizeY = locType.sizeY
            } else {
                locSizeX = locType.sizeY
                locSizeY = locType.sizeX
            }
            val xA: Int
            val xB: Int
            if (locSizeX + baseX <= regionSizeX) {
                xA = (locSizeX shr 1) + baseX
                xB = (locSizeX + 1 shr 1) + baseX
            } else {
                xA = baseX
                xB = baseX + 1
            }
            val yA: Int
            val yB: Int
            if (baseY + locSizeY <= regionSizeY) {
                yA = baseY + (locSizeY shr 1)
                yB = baseY + (locSizeY + 1 shr 1)
            } else {
                yA = baseY
                yB = baseY + 1
            }
            val surfaceGround = scene.localHeightmap[locPlane]
            val averageHeight = (surfaceGround!!.getHeight(xA, yA) + surfaceGround!!.getHeight(xB, yA) + surfaceGround!!.getHeight(xA, yB) + surfaceGround!!.getHeight(xB, yB)) shr 2
            val sceneX = (baseX shl 9) + (locSizeX shl 8)
            val sceneY = (baseY shl 9) + (locSizeY shl 8)
            val applyShading = lowDetailShadows && !overlayHidden && locType.delayShading
            if (locType.hasSound()) {
                AmbientSound.createAmbientSoundEffect(logicHeight, baseX, baseY, locRotation, locType, null, null)
            }
            val stationary = sequenceId == -1 && !locType.isAnimated() && locType.alternativeIds == null && !locType.hasAnimation && !locType.transforms
            if (!soundsOnly || (!ObjectShapes.isWall(locShape) || locType.occlusionMode == 1) && (!ObjectShapes.isRoof(locShape) || locType.occlusionMode != 0)) {
                if (locShape == ObjectShapes.GROUND_DECORATION.shape) {
                    if (PreferenceManager.preferences!!.groundDecoration!!.getValue() != 0 || locType.hasActions != 0 || locType.blocksMovement == 1 || locType.forceDisplayDecoration) {
                        val groundDecoration: AbstractGroundDecoration = if (stationary) {
                            val stationaryGroundDecoration = StationaryGroundDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, locRotation, applyShading)
                            if (stationaryGroundDecoration.hasRenderingShadow()) {
                                stationaryGroundDecoration.addShadow(toolkit)
                            }
                            stationaryGroundDecoration
                        } else {
                            AnimatedGroundDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, locRotation, sequenceId)
                        }
                        scene.addGroundDecoration(logicHeight, baseX, baseY, groundDecoration)
                        if (locType.blocksMovement == 1 && collisionMap != null) {
                            collisionMap.flagBlockedDecoration(baseX, baseY)
                        }
                    }
                } else if (locShape != ObjectShapes.SCENERY_INTERACT.shape && locShape != ObjectShapes.GROUND_INTERACT.shape) {
                    if (!ObjectShapes.isRoof(locShape) && !ObjectShapes.isRoofEdge(locShape)) {
                        if (!addWall(toolkit, locShape, locRotation, stationary, locType, sequenceId, logicHeight, locPlane, sceneX, averageHeight, sceneY, baseX, baseY, applyShading, surfaceGround!!, collisionMap, locSizeX, locSizeY)) {
                            addWallDecoration(toolkit, locShape, locRotation, stationary, locType, sequenceId, logicHeight, locPlane, sceneX, averageHeight, sceneY, baseX, baseY)
                        }
                    } else {
                        val interactive: AbstractInteractiveEntity = if (stationary) {
                            val stationaryInteractable = StationaryInteractiveObject(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, baseX, locSizeX + baseX - 1, baseY, baseY + locSizeY - 1, locShape, locRotation, applyShading)
                            if (stationaryInteractable.hasRenderingShadow()) {
                                stationaryInteractable.addShadow(toolkit)
                            }
                            stationaryInteractable
                        } else {
                            AnimatedInteractiveObject(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, baseX, locSizeX + baseX - 1, baseY, baseY + locSizeY - 1, locShape, locRotation, sequenceId)
                        }
                        scene.addInteractive(interactive, false)
                        if (lowDetailShadows && !overlayHidden && ObjectShapes.isRoof(locShape) && locShape != ObjectShapes.DIAGONAL_SLOPE_ROOF.shape && logicHeight > 0 && locType.occlusionMode != 0) {
                            cullingsMap!![logicHeight][baseX][baseY] = (cullingsMap!![logicHeight][baseX][baseY].toInt() or 0x4).toByte()
                        }
                        if (locType.blocksMovement != 0 && collisionMap != null) {
                            collisionMap.flagBlockedObject(baseX, baseY, locSizeX, locSizeY, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute)
                        }
                    }
                } else {
                    val addedInteractive: AbstractInteractiveEntity
                    var stationaryInteractable: StationaryInteractiveObject? = null
                    val displacement: Int
                    if (stationary) {
                        val interactive = StationaryInteractiveObject(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, baseX, locSizeX + baseX - 1, baseY, baseY + locSizeY - 1, locShape, locRotation, applyShading)
                        displacement = interactive.getShadowDisplacement()
                        addedInteractive = interactive
                        stationaryInteractable = interactive
                    } else {
                        addedInteractive = AnimatedInteractiveObject(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, baseX, locSizeX + baseX - 1, baseY, baseY + locSizeY - 1, locShape, locRotation, sequenceId)
                        displacement = 15
                    }
                    if (scene.addInteractive(addedInteractive, false)) {
                        if (stationaryInteractable != null && stationaryInteractable.hasRenderingShadow()) {
                            stationaryInteractable.addShadow(toolkit)
                        }
                        if (locType.staticShadow && lowDetailShadows) {
                            var displacementVar = displacement
                            if (displacement > 30) {
                                displacementVar = 30
                            }
                            for (baseXOffset in 0..locSizeX) {
                                for (baseYOffset in 0..locSizeY) {
                                    surfaceGround!!.setTileShadow(baseXOffset + baseX, baseY + baseYOffset, displacementVar)
                                }
                            }
                        }
                    }
                    if (locType.blocksMovement != 0 && collisionMap != null) {
                        collisionMap.flagBlockedObject(baseX, baseY, locSizeX, locSizeY, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute)
                    }
                }
            }
        }
    }

    fun decodeEffectsServer(toolkit: AbstractToolkit, mapBuffer: Packet, lightZ: Int, sceneStartX: Int, sceneStartY: Int, z: Int, sourceChunkX: Int, sourceChunkY: Int, rotation: Int) {
        if (!overlayHidden) {
            var hasActiveLights = false
            var environment: Environment? = null
            val chunkStartTileX = (sourceChunkX and 0x7) * 8
            val chunkStartTileY = (sourceChunkY and 0x7) * 8
            while (true) {
                while (mapBuffer.currentPosition < mapBuffer.buffer.size) {
                    val opcode = mapBuffer.readUByte()
                    if (opcode == 0) {
                        if (environment == null) {
                            environment = Environment(mapBuffer, environmentManager)
                        } else {
                            environment.decode(mapBuffer, environmentManager)
                        }
                    } else {
                        if (opcode == 1) {
                            val loops = mapBuffer.readUByte()
                            if (loops > 0) {
                                for (index in 0 until loops) {
                                    val flickeringEffect = FlickeringEffect(toolkit, scene.tileUnits, mapBuffer)
                                    if (flickeringEffect.type == 31) {
                                        val lightType = TypeLists.LIGHT!!.list(mapBuffer.readUShort())
                                        flickeringEffect.setType(lightType!!.effect, lightType.duration, lightType.ticker, lightType.surrounding)
                                    }
                                    if (toolkit.getMaxLights() > 0) {
                                        val light = flickeringEffect.pointLight!!
                                        val sceneX = light.getX() shr 9
                                        val sceneY = light.getZ() shr 9
                                        if (flickeringEffect.level == z && sceneX >= chunkStartTileX && sceneX < chunkStartTileX + 8 && sceneY >= chunkStartTileY && sceneY < chunkStartTileY + 8) {
                                            val positionX = (sceneStartX shl 9) + EnvironmentManager.rotatedX(light.getX() and 0xfff, light.getZ() and 0xfff, rotation)
                                            val positionY = (sceneStartY shl 9) + EnvironmentManager.rotatedZ(light.getX() and 0xfff, light.getZ() and 0xfff, rotation)
                                            val sceneXFinal = positionX shr 9
                                            val sceneYFinal = positionY shr 9
                                            if (sceneXFinal >= 0 && sceneYFinal >= 0 && sceneXFinal < regionSizeX && sceneYFinal < regionSizeY) {
                                                light.setPosition(positionX, tileHeights[z][sceneXFinal][sceneYFinal] - light.getY(), positionY)
                                                scene.calculateLightUid(flickeringEffect)
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (opcode == 2) {
                            if (environment == null) {
                                environment = Environment()
                            }
                            environment.decodeHdr(mapBuffer)
                        } else if (opcode == 128) {
                            if (environment == null) {
                                environment = Environment()
                            }
                            environment.decodeSkybox(mapBuffer, environmentManager)
                        } else {
                            if (opcode != 129) {
                                throw IllegalStateException("")
                            }
                            if (lighting == null) {
                                lighting = Array(4) { null }
                            }
                            for (currentZ in 0 until 4) {
                                val lightingDecodeType = mapBuffer.readByte()
                                if (lightingDecodeType.toInt() == 0 && lighting!![lightZ] != null) {
                                    if (currentZ <= z) {
                                        var minX = sceneStartX
                                        var maxX = sceneStartX + 7
                                        var minY = sceneStartY
                                        var maxY = sceneStartY + 7
                                        if (sceneStartX < 0) {
                                            minX = 0
                                        } else if (sceneStartX >= regionSizeX) {
                                            minX = regionSizeX
                                        }
                                        if (maxX < 0) {
                                            maxX = 0
                                        } else if (maxX >= regionSizeX) {
                                            maxX = regionSizeX
                                        }
                                        if (sceneStartY < 0) {
                                            minY = 0
                                        } else if (sceneStartY >= regionSizeY) {
                                            minY = regionSizeY
                                        }
                                        if (maxY < 0) {
                                            maxY = 0
                                        } else if (maxY >= regionSizeY) {
                                            maxY = regionSizeY
                                        }
                                        while (minX < maxX) {
                                            var minYVar = minY
                                            while (minYVar < maxY) {
                                                lighting!![lightZ]!![minX][minYVar] = 0
                                                minYVar++
                                            }
                                            minX++
                                        }
                                    }
                                } else if (lightingDecodeType.toInt() != 1) {
                                    if (lightingDecodeType.toInt() == 2) {
                                    }
                                } else {
                                    if (lighting!![lightZ] == null) {
                                        lighting!![lightZ] = Array(regionSizeX + 1) { ByteArray(regionSizeY + 1) }
                                    }
                                    for (insideRegionX in 0 until 64 step 4) {
                                        for (insideRegionY in 0 until 64 step 4) {
                                            val lightIntensity = mapBuffer.readByte()
                                            if (currentZ <= z) {
                                                for (x in insideRegionX until insideRegionX + 4) {
                                                    for (y in insideRegionY until insideRegionY + 4) {
                                                        if (x >= chunkStartTileX && x < chunkStartTileX + 8 && y >= chunkStartTileY && y < chunkStartTileY + 8) {
                                                            val lightSceneX = sceneStartX + EnvironmentManager.rotateTerrainBlockX(x and 0x7, y and 0x7, rotation)
                                                            val lightSceneY = sceneStartY + EnvironmentManager.rotateTerrainBlockY(x and 0x7, y and 0x7, rotation)
                                                            if (lightSceneX >= 0 && lightSceneX < regionSizeX && lightSceneY >= 0 && lightSceneY < regionSizeY) {
                                                                lighting!![lightZ]!![lightSceneX][lightSceneY] = lightIntensity
                                                                hasActiveLights = true
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (environment != null) {
                    environmentManager.addChunkEnvironment(sceneStartX shr 3, sceneStartY shr 3, environment)
                }
                if (!hasActiveLights && lighting != null && lighting!![lightZ] != null) {
                    val sceneEndX = sceneStartX + 7
                    val sceneEndY = sceneStartY + 7
                    for (lightSceneX in sceneStartX until sceneEndX) {
                        for (lightSceneY in sceneStartY until sceneEndY) {
                            lighting!![lightZ]!![lightSceneX][lightSceneY] = 0
                        }
                    }
                }
                break
            }
        }
    }

    fun decodeLocMapClient(toolkit: AbstractToolkit, locData: ByteArray, offsetX: Int, offsetY: Int, collisionMaps: Array<CollisionMap?>) {
        val buffer = Packet(locData)
        var locId = -1
        while (true) {
            val locIdOffset = buffer.readSum()
            if (locIdOffset == 0) {
                return
            }
            locId += locIdOffset
            var bitpackedPosition = 0
            while (true) {
                val positionOffset = buffer.readUSmart()
                if (positionOffset == 0) {
                    break
                }
                bitpackedPosition += positionOffset - 1
                val localY = bitpackedPosition and 0x3f
                val localX = bitpackedPosition shr 6 and 0x3f
                val plane = bitpackedPosition shr 12
                val bitpackedDescription = buffer.readUByte()
                val type = bitpackedDescription shr 2
                val rotation = bitpackedDescription and 0x3
                val sceneX = localX + offsetX
                val sceneY = localY + offsetY
                if (sceneX > 0 && sceneY > 0 && sceneX < regionSizeX - 1 && sceneY < regionSizeY - 1) {
                    var clipping: CollisionMap? = null
                    if (!overlayHidden) {
                        var collisionPlane = plane
                        if ((regionMap.tileFunctionMasks[1][sceneX][sceneY].toInt() and 0x2) == 2) {
                            collisionPlane = plane - 1
                        }
                        if (collisionPlane >= 0) {
                            clipping = collisionMaps[collisionPlane]
                        }
                    }
                    addObject(toolkit, plane, plane, sceneX, sceneY, locId, rotation, type, clipping, -1)
                }
            }
        }
    }

    fun decodeLocMapServer(toolkit: AbstractToolkit, locMapData: ByteArray, plane: Int, sceneX: Int, sceneY: Int, landscapeZ: Int, mapX: Int, mapY: Int, rotation: Int, clippings: Array<CollisionMap?>) {
        val locMapBuffer = Packet(locMapData)
        var locId = -1
        while (true) {
            val locOffset = locMapBuffer.readSum()
            if (locOffset == 0) {
                return
            }
            locId += locOffset
            var bitpackedPosition = 0
            while (true) {
                val localPositionOffset = locMapBuffer.readUSmart()
                if (localPositionOffset == 0) {
                    break
                }
                bitpackedPosition += localPositionOffset - 1
                val y = bitpackedPosition and 0x3f
                val x = bitpackedPosition shr 6 and 0x3f
                val z = bitpackedPosition shr 12
                val bitpackedLocDetails = locMapBuffer.readUByte()
                val type = bitpackedLocDetails shr 2
                val locRotation = bitpackedLocDetails and 0x3
                if (z == landscapeZ && x >= mapX && x < mapX + 8 && y >= mapY && y < mapY + 8) {
                    val loctype = locTypeList.list(locId)!!
                    val localX = sceneX + EnvironmentManager.localOffsetX(x and 0x7, y and 0x7, rotation, loctype.sizeX, loctype.sizeY, locRotation)
                    val localY = sceneY + EnvironmentManager.localOffsetY(x and 0x7, y and 0x7, rotation, loctype.sizeX, loctype.sizeY, locRotation)
                    if (localX > 0 && localY > 0 && localX < regionSizeX - 1 && localY < regionSizeY - 1) {
                        var clipping: CollisionMap? = null
                        if (!overlayHidden) {
                            var clipPlane = plane
                            if ((regionMap.tileFunctionMasks[1][localX][localY].toInt() and 0x2) == 2) {
                                clipPlane = plane - 1
                            }
                            if (clipPlane >= 0) {
                                clipping = clippings[clipPlane]
                            }
                        }
                        addObject(toolkit, plane, plane, localX, localY, locId, (locRotation + rotation) and 0x3, type, clipping, -1)
                    }
                }
            }
        }
    }

    /**
     * Contains logic for shading and culling
     */
    fun calculateVisibility(underwater: Boolean) {
        scene.applyShading()
        if (!underwater) {
            if (regionSizeZ > 1) {
                for (x in 0 until regionSizeX) {
                    for (y in 0 until regionSizeY) {
                        if ((regionMap.tileFunctionMasks[1][x][y].toInt() and 0x2) == 2) {
                            scene.linkBelow(x, y)
                        }
                    }
                }
            }
            for (z in 0 until regionSizeZ) {
                for (y in 0..regionSizeY) {
                    for (x in 0..regionSizeX) {
                        if ((cullingsMap!![z][x][y].toInt() and 0x4) != 0) {
                            var endX = x
                            var startX = x
                            val yA = y
                            var yB: Int
                            var yAVar = yA
                            yB = y
                            while (yAVar > 0 && (cullingsMap!![z][x][yAVar - 1].toInt() and 0x4) != 0 && yB - yAVar < 10) {
                                --yAVar
                            }
                            while (yB < regionSizeY && (cullingsMap!![z][x][yB + 1].toInt() and 0x4) != 0 && yB - yAVar < 10) {
                                ++yB
                            }
                            label112@ while (endX > 0 && startX - endX < 10) {
                                for (yC in yAVar..yB) {
                                    if ((cullingsMap!![z][endX - 1][yC].toInt() and 0x4) == 0) {
                                        break@label112
                                    }
                                }
                                --endX
                            }
                            label99@ while (startX < regionSizeX && startX - endX < 10) {
                                for (yD in yAVar..yB) {
                                    if ((cullingsMap!![z][startX + 1][yD].toInt() and 0x4) == 0) {
                                        break@label99
                                    }
                                }
                                ++startX
                            }
                            if ((startX - endX + 1) * (yB - yAVar + 1) >= 4) {
                                val height = tileHeights[z][endX][yAVar]
                                scene.culling.createTileCullingCluster(z, endX shl 9, (startX shl 9) + 512, yAVar shl 9, (yB shl 9) + 512, height, height)
                                for (cullingX in endX..startX) {
                                    for (cullingY in yAVar..yB) {
                                        cullingsMap!![z][cullingX][cullingY] = (cullingsMap!![z][cullingX][cullingY].toInt() and -5).toByte()
                                    }
                                }
                            }
                        }
                    }
                }
            }
            scene.culling.clear()
        }
        cullingsMap = null
    }

    fun addWall(toolkit: AbstractToolkit, locShape: Int, locRotation: Int, stationary: Boolean, locType: ObjectType, sequenceId: Int, locHeight: Int, locPlane: Int, sceneX: Int, averageHeight: Int, sceneY: Int, baseX: Int, baseY: Int, delayShading: Boolean, currentSurfaceGround: Ground, localCollisionMap: CollisionMap?, locSizeX: Int, locSizeY: Int): Boolean {
        if (locShape == ObjectShapes.WALL_STRAIGHT.shape) {
            var cullable = locType.occlusionMode
            if (cullingAdjustment && locType.occlusionMode == -1) {
                cullable = 1
            }
            val primaryWall: AbstractWall = if (stationary) {
                val stationaryWall = StationaryWall(scene, toolkit, locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, locShape, locRotation, delayShading)
                if (stationaryWall.hasRenderingShadow()) {
                    stationaryWall.addShadow(toolkit)
                }
                stationaryWall
            } else {
                AnimatedWall(scene, toolkit, locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, locShape, locRotation, sequenceId)
            }
            scene.addWall(locHeight, baseX, baseY, primaryWall, null)
            when (locRotation) {
                0 -> {
                    if (lowDetailShadows && locType.staticShadow) {
                        currentSurfaceGround.setTileShadow(baseX, baseY, 50)
                        currentSurfaceGround.setTileShadow(baseX, baseY + 1, 50)
                    }
                    if (cullable == 1 && !overlayHidden) {
                        scene.culling.addWallCulling(1, locHeight, baseX, baseY, locType.cullY, locType.cullXZ)
                    }
                }
                1 -> {
                    if (lowDetailShadows && locType.staticShadow) {
                        currentSurfaceGround.setTileShadow(baseX, baseY + 1, 50)
                        currentSurfaceGround.setTileShadow(baseX + 1, baseY + 1, 50)
                    }
                    if (cullable == 1 && !overlayHidden) {
                        scene.culling.addWallCulling(2, locHeight, baseX, baseY + 1, locType.cullY, -locType.cullXZ)
                    }
                }
                2 -> {
                    if (lowDetailShadows && locType.staticShadow) {
                        currentSurfaceGround.setTileShadow(baseX + 1, baseY, 50)
                        currentSurfaceGround.setTileShadow(baseX + 1, baseY + 1, 50)
                    }
                    if (cullable == 1 && !overlayHidden) {
                        scene.culling.addWallCulling(1, locHeight, baseX + 1, baseY, locType.cullY, -locType.cullXZ)
                    }
                }
                3 -> {
                    if (lowDetailShadows && locType.staticShadow) {
                        currentSurfaceGround.setTileShadow(baseX, baseY, 50)
                        currentSurfaceGround.setTileShadow(baseX + 1, baseY, 50)
                    }
                    if (cullable == 1 && !overlayHidden) {
                        scene.culling.addWallCulling(2, locHeight, baseX, baseY, locType.cullY, locType.cullXZ)
                    }
                }
            }
            if (locType.blocksMovement != 0 && localCollisionMap != null) {
                localCollisionMap.flagBoundary(baseX, baseY, locShape, locRotation, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute)
            }
            if (locType.decorDisplacement != 64) {
                scene.offsetWallDecoration(locHeight, baseX, baseY, locType.decorDisplacement)
            }
            return true
        } else {
            if (locShape == ObjectShapes.WALL_DIAGONAL_CORNER.shape) {
                val wall: AbstractWall = if (stationary) {
                    val stationaryWall = StationaryWall(scene, toolkit, locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, locShape, locRotation, delayShading)
                    if (stationaryWall.hasRenderingShadow()) {
                        stationaryWall.addShadow(toolkit)
                    }
                    stationaryWall
                } else {
                    AnimatedWall(scene, toolkit, locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, locShape, locRotation, sequenceId)
                }
                scene.addWall(locHeight, baseX, baseY, wall, null)
                if (locType.staticShadow && lowDetailShadows) {
                    when (locRotation) {
                        0 -> currentSurfaceGround.setTileShadow(baseX, baseY + 1, 50)
                        1 -> currentSurfaceGround.setTileShadow(baseX + 1, baseY + 1, 50)
                        2 -> currentSurfaceGround.setTileShadow(baseX + 1, baseY, 50)
                        3 -> currentSurfaceGround.setTileShadow(baseX, baseY, 50)
                    }
                }
                if (locType.blocksMovement != 0 && localCollisionMap != null) {
                    localCollisionMap.flagBoundary(baseX, baseY, locShape, locRotation, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute)
                }
                return true
            } else if (locShape == ObjectShapes.WALL_WHOLE_CORNER.shape) {
                val signedRotation = locRotation + 1 and 0x3
                val primaryWall: AbstractWall
                val secondaryWall: AbstractWall
                if (stationary) {
                    val firstWall = StationaryWall(scene, toolkit, locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, locShape, locRotation + 4, delayShading)
                    val secondWall = StationaryWall(scene, toolkit, locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, locShape, signedRotation, delayShading)
                    if (firstWall.hasRenderingShadow()) {
                        firstWall.addShadow(toolkit)
                    }
                    if (secondWall.hasRenderingShadow()) {
                        secondWall.addShadow(toolkit)
                    }
                    primaryWall = firstWall
                    secondaryWall = secondWall
                } else {
                    primaryWall = AnimatedWall(scene, toolkit, locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, locShape, locRotation + 4, sequenceId)
                    secondaryWall = AnimatedWall(scene, toolkit, locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, locShape, signedRotation, sequenceId)
                }
                scene.addWall(locHeight, baseX, baseY, primaryWall, secondaryWall)
                if ((locType.occlusionMode == 1 || cullingAdjustment && locType.occlusionMode == -1) && !overlayHidden) {
                    when (locRotation) {
                        0 -> {
                            scene.culling.addWallCulling(1, locHeight, baseX, baseY, locType.cullY, locType.cullXZ)
                            scene.culling.addWallCulling(2, locHeight, baseX, baseY + 1, locType.cullY, locType.cullXZ)
                        }
                        1 -> {
                            scene.culling.addWallCulling(1, locHeight, baseX + 1, baseY, locType.cullY, locType.cullXZ)
                            scene.culling.addWallCulling(2, locHeight, baseX, baseY + 1, locType.cullY, locType.cullXZ)
                        }
                        2 -> {
                            scene.culling.addWallCulling(1, locHeight, baseX + 1, baseY, locType.cullY, locType.cullXZ)
                            scene.culling.addWallCulling(2, locHeight, baseX, baseY, locType.cullY, locType.cullXZ)
                        }
                        3 -> {
                            scene.culling.addWallCulling(1, locHeight, baseX, baseY, locType.cullY, locType.cullXZ)
                            scene.culling.addWallCulling(2, locHeight, baseX, baseY, locType.cullY, locType.cullXZ)
                        }
                    }
                }
                if (locType.blocksMovement != 0 && localCollisionMap != null) {
                    localCollisionMap.flagBoundary(baseX, baseY, locShape, locRotation, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute)
                }
                if (locType.decorDisplacement != 64) {
                    scene.offsetWallDecoration(locHeight, baseX, baseY, locType.decorDisplacement)
                }
                return true
            } else if (locShape == ObjectShapes.WALL_STRAIGHT_CORNER.shape) {
                val wall: AbstractWall = if (stationary) {
                    val stationaryWall = StationaryWall(scene, toolkit, locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, locShape, locRotation, delayShading)
                    if (stationaryWall.hasRenderingShadow()) {
                        stationaryWall.addShadow(toolkit)
                    }
                    stationaryWall
                } else {
                    AnimatedWall(scene, toolkit, locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, locShape, locRotation, sequenceId)
                }
                scene.addWall(locHeight, baseX, baseY, wall, null)
                if (locType.staticShadow && lowDetailShadows) {
                    when (locRotation) {
                        0 -> currentSurfaceGround.setTileShadow(baseX, baseY + 1, 50)
                        1 -> currentSurfaceGround.setTileShadow(baseX + 1, baseY + 1, 50)
                        2 -> currentSurfaceGround.setTileShadow(baseX + 1, baseY, 50)
                        3 -> currentSurfaceGround.setTileShadow(baseX, baseY, 50)
                    }
                }
                if (locType.blocksMovement != 0 && localCollisionMap != null) {
                    localCollisionMap.flagBoundary(baseX, baseY, locShape, locRotation, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute)
                }
                return true
            } else if (locShape == ObjectShapes.WALL_INTERACT.shape) {
                val interactive: AbstractInteractiveEntity = if (stationary) {
                    val stationaryInteractive = StationaryInteractiveObject(scene, toolkit, locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, baseX, baseX, baseY, baseY, locShape, locRotation, delayShading)
                    if (stationaryInteractive.hasRenderingShadow()) {
                        stationaryInteractive.addShadow(toolkit)
                    }
                    stationaryInteractive
                } else {
                    AnimatedInteractiveObject(scene, toolkit, locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, baseX, locSizeX + baseX - 1, baseY, locSizeY + baseY - 1, locShape, locRotation, sequenceId)
                }
                scene.addInteractive(interactive, false)
                if (locType.occlusionMode == 1 && !overlayHidden) {
                    val displacement: Byte = if ((locRotation and 0x1) == 0) {
                        8
                    } else {
                        16
                    }
                    scene.culling.addWallCulling(displacement.toInt(), locHeight, baseX, baseY, locType.cullY, 0)
                }
                if (locType.blocksMovement != 0 && localCollisionMap != null) {
                    localCollisionMap.flagBlockedObject(baseX, baseY, locSizeX, locSizeY, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute)
                }
                if (locType.decorDisplacement != 64) {
                    scene.offsetWallDecoration(locHeight, baseX, baseY, locType.decorDisplacement)
                }
                return true
            }
            return false
        }
    }

    fun addWallDecoration(toolkit: AbstractToolkit, locShape: Int, locRotation: Int, stationary: Boolean, locType: ObjectType, sequenceId: Int, logicHeight: Int, locPlane: Int, sceneX: Int, averageHeight: Int, sceneY: Int, baseX: Int, baseY: Int): Boolean {
        if (locShape == ObjectShapes.STRAIGHT_INSIDE_WALL_DEC.shape) {
            val wallDecoration: AbstractWallDecoration = if (stationary) {
                val stationaryDecoration = StationaryWallDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, 0, 0, locShape, locRotation)
                if (stationaryDecoration.hasRenderingShadow()) {
                    stationaryDecoration.addShadow(toolkit)
                }
                stationaryDecoration
            } else {
                AnimatedWallDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, 0, 0, locShape, locRotation, sequenceId)
            }
            scene.addWallDecoration(logicHeight, baseX, baseY, wallDecoration, null)
            return true
        }
        if (locShape == ObjectShapes.STRAIGHT_OUSIDE_WALL_DEC.shape) {
            var decorationDisplacement = 65
            val gameObject = scene.getWall(logicHeight, baseX, baseY) as? GameObject
            if (gameObject != null) {
                decorationDisplacement = locTypeList.list(gameObject.id).decorDisplacement + 1
            }
            val wallDecoration: AbstractWallDecoration = if (stationary) {
                val stationaryWallDecoration = StationaryWallDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, decorationDisplacement * WALL_DECORATION_DISPLACE_X[locRotation], decorationDisplacement * WALL_DECORATION_DISPLACE_Y[locRotation], locShape, locRotation)
                if (stationaryWallDecoration.hasRenderingShadow()) {
                    stationaryWallDecoration.addShadow(toolkit)
                }
                stationaryWallDecoration
            } else {
                AnimatedWallDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, decorationDisplacement * WALL_DECORATION_DISPLACE_X[locRotation], decorationDisplacement * WALL_DECORATION_DISPLACE_Y[locRotation], locShape, locRotation, sequenceId)
            }
            scene.addWallDecoration(logicHeight, baseX, baseY, wallDecoration, null)
            return true
        }
        if (locShape == ObjectShapes.DIAGONAL_OUTSIDE_WALL_DEC.shape) {
            var decorationDisplacement = 33
            val gameObject = scene.getWall(logicHeight, baseX, baseY) as? GameObject
            if (gameObject != null) {
                decorationDisplacement = locTypeList.list(gameObject.id).decorDisplacement / 2 + 1
            }
            val wallDecoration: AbstractWallDecoration = if (stationary) {
                val stationaryWallDecoration = StationaryWallDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, decorationDisplacement * WALL_DECORATION_DISPLACE_X[locRotation], decorationDisplacement * WALL_DECORATION_DISPLACE_Y[locRotation], locShape, locRotation + 4)
                if (stationaryWallDecoration.hasRenderingShadow()) {
                    stationaryWallDecoration.addShadow(toolkit)
                }
                stationaryWallDecoration
            } else {
                AnimatedWallDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, decorationDisplacement * ANIMATED_WALL_DECORATION_DISPLACE_X[locRotation], decorationDisplacement * ANIMATED_WALL_DECORATION_DISPLACE_Y[locRotation], locShape, locRotation + 4, sequenceId)
            }
            scene.addWallDecoration(logicHeight, baseX, baseY, wallDecoration, null)
            return true
        }
        if (locShape == ObjectShapes.DIAGONAL_INSIDE_WALL_DEC.shape) {
            val signedRotation = locRotation + 2 and 0x3
            val wallDecoration: AbstractWallDecoration = if (stationary) {
                val stationaryWallDecoration = StationaryWallDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, 0, 0, locShape, signedRotation + 4)
                if (stationaryWallDecoration.hasRenderingShadow()) {
                    stationaryWallDecoration.addShadow(toolkit)
                }
                stationaryWallDecoration
            } else {
                AnimatedWallDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, 0, 0, locShape, signedRotation + 4, sequenceId)
            }
            scene.addWallDecoration(logicHeight, baseX, baseY, wallDecoration, null)
            return true
        }
        if (locShape == ObjectShapes.DIAGONAL_INWALL_DEC.shape) {
            val signedRotation = locRotation + 2 and 0x3
            var displacement = 33
            val gameObject = scene.getWall(logicHeight, baseX, baseY) as? GameObject
            if (gameObject != null) {
                displacement = locTypeList.list(gameObject.id).decorDisplacement / 2 + 1
            }
            val primaryWallDecoration: AbstractWallDecoration
            val secondaryWallDecoration: AbstractWallDecoration
            if (stationary) {
                val primaryStationaryWallDecoration = StationaryWallDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, displacement * ANIMATED_WALL_DECORATION_DISPLACE_X[locRotation], displacement * ANIMATED_WALL_DECORATION_DISPLACE_Y[locRotation], locShape, locRotation + 4)
                val secondaryStationaryWallDecoration = StationaryWallDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, 0, 0, locShape, signedRotation + 4)
                if (primaryStationaryWallDecoration.hasRenderingShadow()) {
                    primaryStationaryWallDecoration.addShadow(toolkit)
                }
                if (secondaryStationaryWallDecoration.hasRenderingShadow()) {
                    secondaryStationaryWallDecoration.addShadow(toolkit)
                }
                primaryWallDecoration = primaryStationaryWallDecoration
                secondaryWallDecoration = secondaryStationaryWallDecoration
            } else {
                primaryWallDecoration = AnimatedWallDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, displacement * ANIMATED_WALL_DECORATION_DISPLACE_X[locRotation], displacement * ANIMATED_WALL_DECORATION_DISPLACE_Y[locRotation], locShape, locRotation + 4, sequenceId)
                secondaryWallDecoration = AnimatedWallDecoration(scene, toolkit, locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, overlayHidden, 0, 0, locShape, signedRotation + 4, sequenceId)
            }
            scene.addWallDecoration(logicHeight, baseX, baseY, primaryWallDecoration, secondaryWallDecoration)
            return true
        }
        return false
    }

    fun getGameObject(z: Int, type: Int, x: Int, y: Int): GameObject? {
        var gameObject: GameObject? = null
        if (type == 0) {
            gameObject = scene.getWall(z, x, y) as? GameObject
        }
        if (type == 1) {
            gameObject = scene.getPrimaryWallDecoration(z, x, y) as? GameObject
        }
        if (type == 2) {
            gameObject = scene.getInteractiveObject(z, x, y, Client.INTERACTIVE_OBJECT_COMPARABLE) as? GameObject
        }
        if (type == 3) {
            gameObject = scene.getGroundDecoration(z, x, y) as? GameObject
        }
        return gameObject
    }

    fun decodeEffectsClient(toolkit: AbstractToolkit, buffer: Packet, regionStartX: Int, regionStartY: Int) {
        if (!overlayHidden) {
            var hasActiveLights = false
            var atmosphere: Environment? = null
            while (true) {
                while (buffer.currentPosition < buffer.buffer.size) {
                    val opcode = buffer.readUByte()
                    if (opcode == 0) {
                        if (atmosphere == null) {
                            atmosphere = Environment(buffer, environmentManager)
                        } else {
                            atmosphere.decode(buffer, environmentManager)
                        }
                    } else {
                        if (opcode == 1) {
                            val count = buffer.readUByte()
                            if (count > 0) {
                                for (effectIndex in 0 until count) {
                                    val flickeringEffect = FlickeringEffect(toolkit, scene.tileUnits, buffer)
                                    if (flickeringEffect.type == 31) {
                                        val lightType = TypeLists.LIGHT!!.list(buffer.readUShort())
                                        flickeringEffect.setType(lightType!!.effect, lightType.duration, lightType.ticker, lightType.surrounding)
                                    }
                                    if (toolkit.getMaxLights() > 0) {
                                        val lightSource = flickeringEffect.pointLight!!
                                        val lightAbsX = lightSource.getX() + (regionStartX shl 9)
                                        val lightAbsY = lightSource.getZ() + (regionStartY shl 9)
                                        val lightTileX = lightAbsX shr 9
                                        val lightTileY = lightAbsY shr 9
                                        if (lightTileX >= 0 && lightTileY >= 0 && lightTileX < regionSizeX && lightTileY < regionSizeY) {
                                            lightSource.setPosition(lightAbsX, tileHeights[flickeringEffect.level][lightTileX][lightTileY] - lightSource.getY(), lightAbsY)
                                            scene.calculateLightUid(flickeringEffect)
                                        }
                                    }
                                }
                            }
                        } else if (opcode == 2) {
                            if (atmosphere == null) {
                                atmosphere = Environment()
                            }
                            atmosphere.decodeHdr(buffer)
                        } else if (opcode == 128) {
                            if (atmosphere == null) {
                                atmosphere = Environment()
                            }
                            atmosphere.decodeSkybox(buffer, environmentManager)
                        } else {
                            if (opcode != 129) {
                                throw IllegalStateException("")
                            }
                            if (lighting == null) {
                                lighting = Array(4) { null }
                            }
                            for (plane in 0 until 4) {
                                val lightingDecodeType = buffer.readByte()
                                if (lightingDecodeType.toInt() == 0 && lighting!![plane] != null) {
                                    var minX = regionStartX
                                    var maxX = regionStartX + 64
                                    var minY = regionStartY
                                    var maxY = regionStartY + 64
                                    if (regionStartX < 0) {
                                        minX = 0
                                    } else if (regionStartX >= regionSizeX) {
                                        minX = regionSizeX
                                    }
                                    if (maxX < 0) {
                                        maxX = 0
                                    } else if (maxX >= regionSizeX) {
                                        maxX = regionSizeX
                                    }
                                    if (regionStartY < 0) {
                                        minY = 0
                                    } else if (regionStartY >= regionSizeY) {
                                        minY = regionSizeY
                                    }
                                    if (maxY < 0) {
                                        maxY = 0
                                    } else if (maxY >= regionSizeY) {
                                        maxY = regionSizeY
                                    }
                                    while (minX < maxX) {
                                        var minYVar = minY
                                        while (minYVar < maxY) {
                                            lighting!![plane]!![minX][minYVar] = 0
                                            minYVar++
                                        }
                                        minX++
                                    }
                                } else if (lightingDecodeType.toInt() == 1) {
                                    if (lighting!![plane] == null) {
                                        lighting!![plane] = Array(regionSizeX + 1) { ByteArray(regionSizeY + 1) }
                                    }
                                    for (innerX in 0 until 64 step 4) {
                                        for (innerY in 0 until 64 step 4) {
                                            val lightIntensity = buffer.readByte()
                                            for (sceneX in regionStartX + innerX until regionStartX + innerX + 4) {
                                                for (sceneY in innerY + regionStartY until innerY + regionStartY + 4) {
                                                    if (sceneX >= 0 && sceneX < regionSizeX && sceneY >= 0 && sceneY < regionSizeY) {
                                                        lighting!![plane]!![sceneX][sceneY] = lightIntensity
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (lightingDecodeType.toInt() == 2) {
                                    if (lighting!![plane] == null) {
                                        lighting!![plane] = Array(regionSizeX + 1) { ByteArray(regionSizeY + 1) }
                                    }
                                    if (plane > 0) {
                                        var minX = regionStartX
                                        var maxX = regionStartX + 64
                                        var minY = regionStartY
                                        var maxY = regionStartY + 64
                                        if (regionStartX < 0) {
                                            minX = 0
                                        } else if (regionStartX >= regionSizeX) {
                                            minX = regionSizeX
                                        }
                                        if (maxX < 0) {
                                            maxX = 0
                                        } else if (maxX >= regionSizeX) {
                                            maxX = regionSizeX
                                        }
                                        if (regionStartY < 0) {
                                            minY = 0
                                        } else if (regionStartY >= regionSizeY) {
                                            minY = regionSizeY
                                        }
                                        if (maxY < 0) {
                                            maxY = 0
                                        } else if (maxY >= regionSizeY) {
                                            maxY = regionSizeY
                                        }
                                        while (minX < maxX) {
                                            var minYVar = minY
                                            while (minYVar < maxY) {
                                                lighting!![plane]!![minX][minYVar] = lighting!![plane - 1]!![minX][minYVar]
                                                minYVar++
                                            }
                                            minX++
                                        }
                                    }
                                }
                            }
                            hasActiveLights = true
                        }
                    }
                }
                if (atmosphere != null) {
                    for (opcode in 0 until 8) {
                        for (lightAbsY in 0 until 8) {
                            val sceneChunkX = opcode + (regionStartX shr 3)
                            val sceneChunkY = lightAbsY + (regionStartY shr 3)
                            if (sceneChunkX >= 0 && sceneChunkX < regionSizeX shr 3 && sceneChunkY >= 0 && sceneChunkY < regionSizeY shr 3) {
                                environmentManager.addChunkEnvironment(sceneChunkX, sceneChunkY, atmosphere)
                            }
                        }
                    }
                }
                if (!hasActiveLights && lighting != null) {
                    for (lightZ in 0 until 4) {
                        if (lighting!![lightZ] != null) {
                            for (sceneChunkX in 0 until 16) {
                                for (sceneChunkY in 0 until 16) {
                                    val lightSceneX = sceneChunkX + (regionStartX shr 2)
                                    val lightSceneY = sceneChunkY + (regionStartY shr 2)
                                    if (lightSceneX >= 0 && lightSceneX < 26 && lightSceneY >= 0 && lightSceneY < 26) {
                                        lighting!![lightZ]!![lightSceneX][lightSceneY] = 0
                                    }
                                }
                            }
                        }
                    }
                }
                break
            }
        }
    }

    fun removeGameObject(toolkit: AbstractToolkit, z: Int, gameObjectType: Int, x: Int, y: Int, clipping: CollisionMap) {
        val gameObject = getGameObject(z, gameObjectType, x, y)
        if (gameObject != null) {
            val locType = locTypeList.list(gameObject.id)
            val type = gameObject.shape
            val rotation = gameObject.getRotation()
            if (locType.hasSound()) {
                AmbientSound.unlinkLocSound(z, x, y, locType)
            }
            if (gameObject.hasRenderingShadow()) {
                gameObject.removeShadow(toolkit)
            }
            when (gameObjectType) {
                0 -> {
                    scene.removePrimaryWall(z, x, y)
                    scene.removeSecondaryWall(z, x, y)
                    if (locType.blocksMovement != 0) {
                        clipping.unflagBoundary(x, y, type, rotation, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute)
                    }
                    if (locType.occlusionMode == 1) {
                        when (rotation) {
                            0 -> scene.culling.removeCullingCluster(1, z, x, y)
                            1 -> scene.culling.removeCullingCluster(2, z, x, y + 1)
                            2 -> scene.culling.removeCullingCluster(1, z, x + 1, y)
                            3 -> scene.culling.removeCullingCluster(2, z, x, y)
                        }
                    }
                }
                1 -> {
                    scene.removePrimaryWallDecoration(z, x, y)
                    scene.removeSecondaryWallDecoration(z, x, y)
                }
                2 -> {
                    scene.removeInteractiveOnCondition(z, x, y, Client.INTERACTIVE_OBJECT_COMPARABLE)
                    if (locType.blocksMovement != 0 && x + locType.sizeX < regionSizeX && y + locType.sizeX < regionSizeY && x + locType.sizeY < regionSizeX && y + locType.sizeY < regionSizeY) {
                        clipping.unflagBlocked(x, y, locType.sizeX, locType.sizeY, rotation, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute)
                    }
                    if (type == ObjectShapes.WALL_INTERACT.shape) {
                        if ((rotation and 0x1) == 0) {
                            scene.culling.removeCullingCluster(8, z, x, y)
                        } else {
                            scene.culling.removeCullingCluster(16, z, x, y)
                        }
                    }
                }
                3 -> {
                    scene.removeGroundDecoration(z, x, y)
                    if (locType.blocksMovement == 1) {
                        clipping.unflagFloorDecoration(x, y)
                    }
                }
            }
        }
    }
}
