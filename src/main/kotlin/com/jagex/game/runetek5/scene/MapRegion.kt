package com.jagex.game.runetek5.scene

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.buffer.Packet.Bit
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.cache.node.impl.*
import com.jagex.game.runetek5.client.*
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.console.Console
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.client.window.ApplicationType
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.cutscene.CutsceneArea
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.`object`.ObjectTypeCustomisation
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.config.mapsprite.MapSpriteTypeList
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.config.sequence.Projectile
import com.jagex.game.runetek5.config.staticelements.StaticElementTypeList
import com.jagex.game.runetek5.config.staticelements.StaticElements
import com.jagex.game.runetek5.entity.*
import com.jagex.game.runetek5.entity.gameobject.*
import com.jagex.game.runetek5.entity.grounditem.*
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.entity.spotanimation.SpotAnimation
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.graphics.hint.HintArrow
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.graphics.map.worldmap.*
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.math.Vector3Pool
import com.jagex.game.runetek5.movement.CoordGrid
import com.jagex.game.runetek5.network.PingRequester
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.rendering.MessageBox
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.environment.EnvironmentManager
import com.jagex.game.runetek5.scene.tile.Tile
import com.jagex.game.runetek5.sound.player.area.*
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.Utils
import kotlin.math.abs

class MapRegion(var asyncMaploading: Boolean) {
    var debugRegionLoadtimeStart: Long = 0
    var debugRegionLoadtime: Boolean = false
    @JvmField
    var previousLoadingType: RegionLoadingType? = null

    @JvmField
    var lastCoordgrid: CoordGrid = CoordGrid()

    @JvmField
    var xteas: Array<IntArray> = emptyArray()

    @JvmField
    var regionIds: IntArray = IntArray(0)

    @JvmField
    var surfaceMapArchives: IntArray = IntArray(0)

    @JvmField
    var surfaceLocArchives: IntArray = IntArray(0)

    @JvmField
    var npcSpawnArchives: IntArray = IntArray(0)

    @JvmField
    var surfaceMapBuffer: Array<ByteArray?> = emptyArray()

    @JvmField
    var surfaceLocBuffer: Array<ByteArray?> = emptyArray()

    @JvmField
    var npcSpawnBuffer: Array<ByteArray?> = emptyArray()

    @JvmField
    var underwaterMapArchives: IntArray = IntArray(0)

    @JvmField
    var underwaterLocArchives: IntArray = IntArray(0)

    @JvmField
    var underwaterMapBuffer: Array<ByteArray?> = emptyArray()

    @JvmField
    var underwaterLocBuffer: Array<ByteArray?> = emptyArray()

    @JvmField
    var loadingType: RegionLoadingType = RegionLoadingType.STANDARD

    var bits: Array<Array<ByteArray>>? = null

    @JvmField var loctypeList: ObjectTypeList? = null

    @JvmField var scene: Scene? = null
    var currentLoadingStage: RegionLoadingStage? = null
    var missingMapCount: Int = 0
    var totalMapCount: Int = 1
    var locsStillLoading: Int = 0
    var totalLocsLoading: Int = 1

    @JvmField var environmentManager: EnvironmentManager? = null
    var viewRangeStart: Int = 0
    var viewRangeEnd: Int = 0
    @JvmField
    var sceneSizeY: Int = 0
    @JvmField
    var sceneSizeX: Int = 0
    var chunkBitpackedPosition: Array<Array<IntArray?>?> = arrayOfNulls(4)
    var drawPriority: Array<IntArray> = emptyArray()

    /**
     * Amount of entities on one tile
     * sharing the highest draw priority
     */
    var drawPrioritySharedby: Array<IntArray> = emptyArray()
    var tileSettingsBits: SettingsBits = SettingsBits(0, 0)

    lateinit var activeMapSize: MapSize
    var baseX: Int = 0
    var baseY: Int = 0
    @JvmField var baseCoordinates: CoordGrid = CoordGrid()
    var worldMapType: WorldMapElement? = null
    @JvmField
    var staticElements: StaticElements = StaticElements(0)
    var underwaterMap: SceneGraph? = null
    var surfaceSceneGraph: SceneGraph? = null
    @JvmField
    var dynamicTint: DynamicTint = DynamicTint()
    var randomColorOffset: Float = 0f
    @JvmField
    var collisionMaps: Array<CollisionMap> = arrayOfNulls<CollisionMap>(4) as Array<CollisionMap>

    fun initialize(regionLoadRequest: RegionLoadRequest) {
        this.loadingType = regionLoadRequest.loadingType!!
        if (this.loadingType == RegionLoadingType.ANIMATED_LOGIN_SCREEN) {
            initializeAnimatedLoginscreen()
        } else if (this.loadingType == RegionLoadingType.STANDARD) {
            initializeRegion(regionLoadRequest.buffer)
        } else if (this.loadingType == RegionLoadingType.CUTSCENE) {
            initializeCutscene()
        } else if (this.loadingType.constructedMap()) {
            initializeConstructedMap(regionLoadRequest.buffer)
        }
    }

    fun initializeAnimatedLoginscreen() {
        setMapSize(MapSize.get(PreferenceManager.preferences!!.mapSize!!.getValue())!!)
        val baseX = this.baseCoordinates.x
        val baseY = this.baseCoordinates.y
        val mapBaseX: Int = (Camera.cameraPositionAbsoluteX shr 12) + (baseX shr 3)
        val mapBaseY: Int = (Camera.cameraPositionAbsoluteY shr 12) + (baseY shr 3)
        PlayerUpdate.MY_PLAYER!!.plane = 0
        PlayerUpdate.MY_PLAYER_PLANE = 0
        PlayerUpdate.MY_PLAYER!!.move(8, 8)
        val numRegions = 18
        this.xteas = Array(numRegions) { IntArray(4) }
        this.regionIds = IntArray(numRegions)
        this.surfaceMapArchives = IntArray(numRegions)
        this.surfaceLocArchives = IntArray(numRegions)
        this.npcSpawnArchives = IntArray(numRegions)
        this.surfaceMapBuffer = arrayOfNulls(numRegions)
        this.surfaceLocBuffer = arrayOfNulls(numRegions)
        this.npcSpawnBuffer = arrayOfNulls(numRegions)
        this.underwaterMapArchives = IntArray(numRegions)
        this.underwaterLocArchives = IntArray(numRegions)
        this.underwaterMapBuffer = arrayOfNulls(numRegions)
        this.underwaterLocBuffer = arrayOfNulls(numRegions)
        var mapIndex = 0
        for (regionX in (mapBaseX - (this.sceneSizeX shr 4)) / 8..(mapBaseX + (this.sceneSizeX shr 4)) / 8) {
            for (regionY in (mapBaseY - (this.sceneSizeY shr 4)) / 8..(mapBaseY + (this.sceneSizeY shr 4)) / 8) {
                val regionId = regionY + (regionX shl 8)
                this.regionIds[mapIndex] = regionId
                this.surfaceMapArchives[mapIndex] = Resource.MAPS!!.getArchiveId(getArchiveName(true, false, regionX, regionY))
                this.surfaceLocArchives[mapIndex] = Resource.MAPS!!.getArchiveId(getArchiveName(false, false, regionX, regionY))
                this.npcSpawnArchives!![mapIndex] = Resource.MAPS!!.getArchiveId("n" + regionX + '_' + regionY)
                this.underwaterMapArchives[mapIndex] = Resource.MAPS!!.getArchiveId(getArchiveName(true, true, regionX, regionY))
                this.underwaterLocArchives[mapIndex] = Resource.MAPS!!.getArchiveId(getArchiveName(false, true, regionX, regionY))
                if (this.npcSpawnArchives!![mapIndex] == -1) {
                    this.surfaceMapArchives[mapIndex] = -1
                    this.surfaceLocArchives[mapIndex] = -1
                    this.underwaterMapArchives[mapIndex] = -1
                    this.underwaterLocArchives[mapIndex] = -1
                }
                mapIndex++
            }
        }
        for (index in mapIndex..<this.npcSpawnArchives!!.size) {
            this.npcSpawnArchives!![index] = -1
            this.surfaceMapArchives[index] = -1
            this.surfaceLocArchives[index] = -1
            this.underwaterMapArchives[index] = -1
            this.underwaterLocArchives[index] = -1
        }
        val newGamestate: GameState?
        if (Client.gameState == GameState.UNK_5) {
            newGamestate = GameState.UNK_11
        } else if (Client.gameState == GameState.UNK_7) {
            newGamestate = GameState.UNK_6
        } else if (Client.gameState == GameState.UNK_0) {
            newGamestate = GameState.UNK_2
        } else if (Client.gameState == GameState.IN_ACCOUNT_CREATION) {
            newGamestate = GameState.UNK_9
        } else {
            throw RuntimeException("" + Client.gameState)
        }
        performUpdate(mapBaseX, mapBaseY, newGamestate, false)
    }


    fun initializeRegion(instream: Bit?) {
        val stream = instream!!
        val mapsizeType: Int = instream.readUByte()
        val chunkX: Int = instream.readUShort()
        val chunkY: Int = instream.readUShort()
        val positionUpdate = instream.readUByte() == 1
        if (!this.asyncMaploading) {
            resetNonCutsceneEntities()
        }
        setMapSize(MapSize.get(mapsizeType)!!)
        var regionCount: Int = (instream.buffer.size - instream.currentPosition) / 16
        this.xteas = Array(regionCount) { IntArray(4) }
        var regionX: Int
        var regionY: Int
        regionX = 0
        while (regionX < regionCount) {
            regionY = 0
            while (regionY < 4) {
                this.xteas[regionX][regionY] = stream.readInt()
                regionY++
            }
            regionX++
        }
        this.regionIds = IntArray(regionCount)
        this.surfaceMapArchives = IntArray(regionCount)
        this.surfaceLocArchives = IntArray(regionCount)
        this.npcSpawnArchives = IntArray(regionCount) { -1 }
        this.surfaceMapBuffer = arrayOfNulls<ByteArray>(regionCount)
        this.surfaceLocBuffer = arrayOfNulls<ByteArray>(regionCount)
        this.npcSpawnBuffer = arrayOfNulls(regionCount)
        this.underwaterMapArchives = IntArray(regionCount)
        this.underwaterLocArchives = IntArray(regionCount)
        this.underwaterMapBuffer = arrayOfNulls<ByteArray>(regionCount)
        this.underwaterLocBuffer = arrayOfNulls<ByteArray>(regionCount)
        regionCount = 0
        regionX = (chunkX - (this.sceneSizeX shr 4)) / 8
        while (regionX <= (chunkX + (this.sceneSizeX shr 4)) / 8) {
            regionY = (chunkY - (this.sceneSizeY shr 4)) / 8
            while (regionY <= (chunkY + (this.sceneSizeY shr 4)) / 8) {
                this.regionIds[regionCount] = regionY + (regionX shl 8)
                this.surfaceMapArchives[regionCount] = Resource.MAPS!!.getArchiveId(getArchiveName(true, false, regionX, regionY))
                this.surfaceLocArchives[regionCount] = Resource.MAPS!!.getArchiveId(getArchiveName(false, false, regionX, regionY))
                this.underwaterMapArchives[regionCount] = Resource.MAPS!!.getArchiveId(getArchiveName(true, true, regionX, regionY))
                this.underwaterLocArchives[regionCount] = Resource.MAPS!!.getArchiveId(getArchiveName(false, true, regionX, regionY))
                regionCount++
                regionY++
            }
            regionX++
        }
        performUpdate(chunkX, chunkY, GameState.LOADING_PLEASE_WAIT, positionUpdate)
    }

    fun initializeCutscene() {
        this.previousLoadingType = this.loadingType
        setMapSize(MapSize.MAPSIZE_104_DEFAULT)
        for (z in 0..3) {
            for (chunkX in 0..<(this.sceneSizeX shr 3)) {
                for (chunkY in 0..<(this.sceneSizeY shr 3)) {
                    this.chunkBitpackedPosition[z]!![chunkX]!![chunkY] = -1
                }
            }
        }
        var cutsceneArea: CutsceneArea? = CutsceneTypeList.CUTSCENE_AREAS.getFirst() as CutsceneArea?
        while (cutsceneArea != null) {
            val rotation: Int = cutsceneArea.rotation
            val isRotated = (rotation and 0x1) == 1
            val chunkX: Int = cutsceneArea.regionX shr 3
            val chunkY: Int = cutsceneArea.regionY shr 3
            val cutsceneChunkXBase: Int = cutsceneArea.anInt7480
            val cutsceneChunkYBase: Int = cutsceneArea.anInt7483
            val z: Int = cutsceneArea.plane
            val cutscenePlane: Int = cutsceneArea.cutscenePlane
            val stepX: Int = cutsceneArea.width
            val stepY: Int = cutsceneArea.length
            var offsetY = 0
            var offX = 0
            var offXStep: Byte = 1
            var offYStep: Byte = 1
            if (rotation == 1) {
                offX = stepX - 1
                offXStep = -1
            } else if (rotation == 2) {
                offX = stepX - 1
                offsetY = stepY - 1
                offXStep = -1
                offYStep = -1
            } else if (rotation == 3) {
                offsetY = stepY - 1
                offXStep = 1
                offYStep = -1
            }
            var y = chunkY
            while (y < stepY + chunkY) {
                var offsetX = offX
                var x = chunkX
                while (x < stepX + chunkX) {
                    if (isRotated) {
                        this.chunkBitpackedPosition[cutscenePlane]!![cutsceneChunkXBase + offsetY]!![offsetX + cutsceneChunkYBase] = (x shl 14) + (y shl 3) + (z shl 24) + (rotation shl 1)
                    } else {
                        this.chunkBitpackedPosition[cutscenePlane]!![offsetX + cutsceneChunkXBase]!![cutsceneChunkYBase + offsetY] = (x shl 14) + (y shl 3) + (z shl 24) + (rotation shl 1)
                    }
                    x++
                    offsetX += offXStep.toInt()
                }
                y++
                offsetY += offYStep.toInt()
            }
            cutsceneArea = CutsceneTypeList.CUTSCENE_AREAS.next() as CutsceneArea?
        }
        var mapIndex: Int = CUTSCENE_MAP_XTEAS!!.size
        this.regionIds = IntArray(mapIndex)
        this.surfaceMapArchives = IntArray(mapIndex)
        this.surfaceLocArchives = IntArray(mapIndex)
        this.npcSpawnArchives = IntArray(mapIndex) { -1 }
        this.surfaceMapBuffer = arrayOfNulls<ByteArray>(mapIndex)
        this.surfaceLocBuffer = arrayOfNulls<ByteArray>(mapIndex)
        this.npcSpawnBuffer = arrayOfNulls(mapIndex)
        this.underwaterMapArchives = IntArray(mapIndex)
        this.underwaterLocArchives = IntArray(mapIndex)
        this.underwaterMapBuffer = arrayOfNulls<ByteArray>(mapIndex)
        this.underwaterLocBuffer = arrayOfNulls<ByteArray>(mapIndex)
        mapIndex = 0
        var area: CutsceneArea? = CutsceneTypeList.CUTSCENE_AREAS.getFirst() as CutsceneArea?
        while (area != null) {
            val chunkX: Int = area.regionX ushr 3
            val chunkY: Int = area.regionY ushr 3
            var regionXEnd: Int = chunkX + area.width
            if ((regionXEnd and 0x7) == 0) {
                regionXEnd--
            }
            regionXEnd = regionXEnd ushr 3
            var regionYEnd: Int = chunkY + area.length
            if ((regionYEnd and 0x7) == 0) {
                regionYEnd--
            }
            regionYEnd = regionYEnd ushr 3
            for (regionX in (chunkX ushr 3)..regionXEnd) {
                LoadMapArchives@ for (regionY in (chunkY ushr 3)..regionYEnd) {
                    val regionId: Int = regionX shl 8 or regionY
                    for (map in 0..<mapIndex) {
                        if (regionId == this.regionIds[map]) {
                            continue@LoadMapArchives
                        }
                    }
                    this.regionIds[mapIndex] = regionId
                    this.surfaceMapArchives[mapIndex] = Resource.MAPS!!.getArchiveId(getArchiveName(true, false, regionX, regionY))
                    this.surfaceLocArchives[mapIndex] = Resource.MAPS!!.getArchiveId(getArchiveName(false, false, regionX, regionY))
                    this.underwaterMapArchives[mapIndex] = Resource.MAPS!!.getArchiveId(getArchiveName(true, true, regionX, regionY))
                    this.underwaterLocArchives[mapIndex] = Resource.MAPS!!.getArchiveId(getArchiveName(false, true, regionX, regionY))
                    mapIndex++
                }
            }
            area = CutsceneTypeList.CUTSCENE_AREAS.next() as CutsceneArea?
        }
        this.xteas = CUTSCENE_MAP_XTEAS!!
        CUTSCENE_MAP_XTEAS = null
        performUpdate(this.sceneSizeX shr 4, this.sceneSizeY shr 4, GameState.LOADING_PLEASE_WAIT, false)
    }

    fun initializeConstructedMap(stream: Bit?) {
        val instream = stream!!
        val regionUpdateMask: Int = instream.readUByteInverse()
        val forcedPositionUpdate = (regionUpdateMask and 0x1) != 0
        val type: Int = instream.readUByteSubtract()
        if (type == 1) {
            this.loadingType = RegionLoadingType.INSTANCE
        } else if (type == 2) {
            this.loadingType = RegionLoadingType.INSTANCE_NO_ENTITY_RESET
        } else if (type == 3) {
            this.loadingType = RegionLoadingType.INSTANCE_LARGE
        } else if (type == 4) {
            this.loadingType = RegionLoadingType.INSTANCE_LARGE_NO_RELOAD_UPDATEZONES
        }
        val mapSizeType: Int = instream.readUByteAdd()
        val mapBaseY: Int = instream.readUShortAdd()
        val mapBaseX: Int = instream.readUShort()
        if (!this.asyncMaploading) {
            resetNonCutsceneEntities()
        }
        setMapSize(MapSize.get(mapSizeType)!!)
        instream.initBitAccess()
        for (chunkPlane in 0..3) {
            for (chunkIndexX in 0..<(this.sceneSizeX shr 3)) {
                for (chunkIndexY in 0..<(this.sceneSizeY shr 3)) {
                    val hasChunkFlags: Int = instream.readBits(1)
                    if (hasChunkFlags == 1) {
                        this.chunkBitpackedPosition[chunkPlane]!![chunkIndexX]!![chunkIndexY] = instream.readBits(26)
                    } else {
                        this.chunkBitpackedPosition[chunkPlane]!![chunkIndexX]!![chunkIndexY] = -1
                    }
                }
            }
        }
        instream.finishBitAccess()
        var mapCount: Int = (instream.buffer.size - instream.currentPosition) / 16
        this.xteas = Array(mapCount) { IntArray(4) }
        for (region in 0..<mapCount) {
            for (map in 0..3) {
                this.xteas[region][map] = instream.readInt()
            }
        }
        this.regionIds = IntArray(mapCount)
        this.surfaceMapArchives = IntArray(mapCount)
        this.surfaceLocArchives = IntArray(mapCount)
        this.npcSpawnArchives = IntArray(mapCount) { -1 }
        this.surfaceMapBuffer = arrayOfNulls<ByteArray>(mapCount)
        this.surfaceLocBuffer = arrayOfNulls<ByteArray>(mapCount)
        this.npcSpawnBuffer = arrayOfNulls(mapCount)
        this.underwaterMapArchives = IntArray(mapCount)
        this.underwaterLocArchives = IntArray(mapCount)
        this.underwaterMapBuffer = arrayOfNulls<ByteArray>(mapCount)
        this.underwaterLocBuffer = arrayOfNulls<ByteArray>(mapCount)
        mapCount = 0
        for (chunkPlane in 0..3) {
            for (chunkIndexX in 0..<(this.sceneSizeX shr 3)) {
                for (chunkIndexY in 0..<(this.sceneSizeY shr 3)) {
                    val bitpackedPosition = this.chunkBitpackedPosition[chunkPlane]!![chunkIndexX]!![chunkIndexY]
                    if (bitpackedPosition != -1) {
                        val regionX = bitpackedPosition shr 14 and 0x3ff
                        val regionY = bitpackedPosition shr 3 and 0x7ff
                        var regionId = (regionX / 8 shl 8) + regionY / 8
                        for (index in 0..<mapCount) {
                            if (regionId == this.regionIds[index]) {
                                regionId = -1
                                break
                            }
                        }
                        if (regionId != -1) {
                            this.regionIds[mapCount] = regionId
                            val RegionX = regionId shr 8 and 0xff
                            val RegionY = regionId and 0xff
                            this.surfaceMapArchives[mapCount] = Resource.MAPS!!.getArchiveId(getArchiveName(true, false, RegionX, RegionY))
                            this.surfaceLocArchives[mapCount] = Resource.MAPS!!.getArchiveId(getArchiveName(false, false, RegionX, RegionY))
                            this.underwaterMapArchives[mapCount] = Resource.MAPS!!.getArchiveId(getArchiveName(true, true, RegionX, RegionY))
                            this.underwaterLocArchives[mapCount] = Resource.MAPS!!.getArchiveId(getArchiveName(false, true, RegionX, RegionY))
                            mapCount++
                        }
                    }
                }
            }
        }
        performUpdate(mapBaseX, mapBaseY, GameState.LOADING_PLEASE_WAIT, forcedPositionUpdate)
    }

    fun buildRegionBase(mapRegion: MapRegion) {
        val multiThread = mapRegion.asyncMaploading
        mapRegion.asyncMaploading = this.asyncMaploading
        this.asyncMaploading = multiThread
        val loadingType = mapRegion.previousLoadingType
        mapRegion.previousLoadingType = this.previousLoadingType
        this.previousLoadingType = loadingType
        mapRegion.lastCoordgrid = this.baseCoordinates
        this.lastCoordgrid = mapRegion.baseCoordinates
        this.environmentManager!!.buildEnvironment(mapRegion.getEnvironmentManager())
    }

    fun loadRegion(): Boolean {
        if (!this.asyncMaploading) {
            PingRequester.keepalive(false)
        }
        this.missingMapCount = 0
        for (index in this.surfaceMapBuffer.indices) {
            if (this.surfaceMapArchives[index] != -1 && this.surfaceMapBuffer[index] == null) {
                this.surfaceMapBuffer[index] = Resource.MAPS!!.getFile(this.surfaceMapArchives[index], 0)
                if (this.surfaceMapBuffer[index] == null) {
                    this.missingMapCount++
                }
            }
            if (this.surfaceLocArchives[index] != -1 && this.surfaceLocBuffer[index] == null) {
                this.surfaceLocBuffer[index] = Resource.MAPS!!.getFile(this.surfaceLocArchives[index], 0, this.xteas[index])
                if (this.surfaceLocBuffer[index] == null) {
                    this.missingMapCount++
                }
            }

            if (this.underwaterMapArchives[index] != -1 && this.underwaterMapBuffer[index] == null) {
                this.underwaterMapBuffer[index] = Resource.MAPS!!.getFile(this.underwaterMapArchives[index], 0)
                if (this.underwaterMapBuffer[index] == null) {
                    this.missingMapCount++
                }
            }

            if (this.underwaterLocArchives[index] != -1 && this.underwaterLocBuffer[index] == null) {
                this.underwaterLocBuffer[index] = Resource.MAPS!!.getFile(this.underwaterLocArchives[index], 0)
                if (this.underwaterLocBuffer[index] == null) {
                    this.missingMapCount++
                }
            }
            if (this.npcSpawnArchives != null && this.npcSpawnBuffer!![index] == null && this.npcSpawnArchives!![index] != -1) {
                this.npcSpawnBuffer!![index] = Resource.MAPS!!.getFile(this.npcSpawnArchives!![index], 0, this.xteas[index])
                if (this.npcSpawnBuffer!![index] == null) {
                    this.missingMapCount++
                }
            }
        }
        if (this.staticElements == null) {
            val worldMapTypeLocal = this.worldMapType
            val worldMapLocal = Resource.WORLD_MAP
            if (worldMapTypeLocal != null && worldMapLocal != null && worldMapLocal.validFile(worldMapTypeLocal.filenamePrefix + "_staticelements")) {
                if (!worldMapLocal.isCached(worldMapTypeLocal.filenamePrefix + "_staticelements")) {
                    this.missingMapCount++
                } else {
                    this.staticElements = StaticElementTypeList.list(worldMapLocal, worldMapTypeLocal.filenamePrefix + "_staticelements", Client.memberWorld)
                }
            } else {
                this.staticElements = StaticElements(0)
            }
        }
        if (this.missingMapCount > 0) {
            if (this.totalMapCount < this.missingMapCount) {
                this.totalMapCount = this.missingMapCount
            }
            this.currentLoadingStage = RegionLoadingStage.LOADING_MAPS
            return false
        }
        this.locsStillLoading = 0
        for (mapPtr in this.surfaceMapBuffer.indices) {
            var mapdata = this.surfaceLocBuffer[mapPtr]
            if (mapdata != null) {
                var x = (this.regionIds[mapPtr] shr 8) * 64 - this.baseCoordinates!!.x
                var y = (this.regionIds[mapPtr] and 0xff) * 64 - this.baseCoordinates!!.y
                if (this.loadingType!!.constructedMap()) {
                    x = 10
                    y = 10
                }
                val incompleteLocs = SceneGraph.loadLocs(this.loctypeList!!, mapdata, x, y, this.sceneSizeX, this.sceneSizeY)
                if (incompleteLocs > 0) {
                    this.locsStillLoading += incompleteLocs
                }
            }
            mapdata = this.underwaterLocBuffer[mapPtr]
            if (mapdata != null) {
                var x = (this.regionIds[mapPtr] shr 8) * 64 - this.baseCoordinates!!.x
                var y = (this.regionIds[mapPtr] and 0xff) * 64 - this.baseCoordinates!!.y
                if (this.loadingType!!.constructedMap()) {
                    x = 10
                    y = 10
                }
                val incompleteLocs = SceneGraph.loadLocs(this.loctypeList!!, mapdata, x, y, this.sceneSizeX, this.sceneSizeY)
                if (incompleteLocs > 0) {
                    this.locsStillLoading += incompleteLocs
                }
            }
        }
        if (this.locsStillLoading > 0) {
            if (this.totalLocsLoading < this.locsStillLoading) {
                this.totalLocsLoading = this.locsStillLoading
            }
            this.currentLoadingStage = RegionLoadingStage.LOADING_LOCS
            return false
        }
        if (!this.asyncMaploading && this.currentLoadingStage != RegionLoadingStage.LOADING) {
            MessageBox.draw(LocalizedText.LOADING_PLEASE_WAIT.translate(Client.CURRENT_LANGUAGE!!) + "<br>" + "(100%)", true, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont)
        }
        this.currentLoadingStage = RegionLoadingStage.PREPARE_DISPLAY
        if (!this.asyncMaploading) {
            SoundEffectPlayer.updatePlayers() // instanced in 910
        }
        if (!this.asyncMaploading) {
            for (index in 0..2047) {
                val playerEntity: PlayerEntity? = PlayerUpdate.players[index]
                if (playerEntity != null) {
                    playerEntity.scene = null
                }
            }
            for (index in Client.NPC_ARRAY.indices) {
                val npcObjectNode: ObjectNode? = Client.NPC_ARRAY[index]
                if (npcObjectNode != null) {
                    (npcObjectNode.value as SceneGraphEntity).scene = null
                }
            }
        }
        if (!this.asyncMaploading) {
            TypeLists.resetCachedData(true)
        }
        if (!this.asyncMaploading) {
            StaticMethods.method4250() // not in 910
        }
        var hasUnderwaterMap = false
        if (PreferenceManager.preferences!!.water!!.getValue() == 2) {
            for (map in this.surfaceMapBuffer.indices) {
                if (this.underwaterLocBuffer[map] != null || this.underwaterMapBuffer[map] != null) {
                    hasUnderwaterMap = true
                    break
                }
            }
        }
        var buildAreaChunks = BuildArea.parse(PreferenceManager.preferences!!.buildArea!!.getValue())!!.chunkSize * 8 * Loader.RENDER_DISTANCE_MULTIPLIER
        if (Renderers.activeToolkit!!.extendBuildArea()) {
            buildAreaChunks++
        }
        calculateViewRange()
        clear()
        this.scene = Scene(Renderers.activeToolkit!!, this.sceneSizeX, this.sceneSizeY, buildAreaChunks, hasUnderwaterMap, Renderers.activeToolkit!!.getMaxLights() > 0)
        this.scene!!.setUnderwater(false)
        this.scene!!.empty1()
        if (Client.sceneDummyOp != 0) {
            this.scene!!.empty2()
        } else {
            this.scene!!.empty2()
        }
        this.dynamicTint = DynamicTint()
        this.randomColorOffset = -0.05f + (Math.random() / 10.0).toFloat()
        this.surfaceSceneGraph = SceneGraph(this.scene!!, this.loctypeList!!, 4, this.sceneSizeX, this.sceneSizeY, false, this.tileSettingsBits!!, this.environmentManager!!)
        this.surfaceSceneGraph!!.enableLdShadows()
        this.surfaceSceneGraph!!.hdShadows = PreferenceManager.preferences!!.sceneryShadows!!.getValue()
        this.surfaceSceneGraph!!.hdWater = PreferenceManager.preferences!!.water!!.getValue() == 2
        this.surfaceSceneGraph!!.hdLight = PreferenceManager.preferences!!.lightDetail!!.getValue() == 1
        this.surfaceSceneGraph!!.blendFloor = PreferenceManager.preferences!!.goundBlending!!.getValue() == 1
        this.surfaceSceneGraph!!.enableTextures = PreferenceManager.preferences!!.textures!!.getValue() == 1
        if (!this.loadingType!!.constructedMap()) {
            decodeClientUnderlayMasks(this.surfaceSceneGraph!!, this.surfaceMapBuffer)
        } else {
            decodeServerUnderlayMasks(this.surfaceSceneGraph!!, this.surfaceMapBuffer)
        }
        if (this.asyncMaploading) {
            sleep(50)
        }
        //ADDED
        if (this.asyncMaploading) {
            Utils.time()
            while (!Renderers.activeToolkit!!.readyForRendering()) {
                println("Waiting for renderer")
                sleep(1)
            }
        }
        this.environmentManager!!.initializeEnvironment(this.sceneSizeX shr 4, this.sceneSizeY shr 4)
        this.environmentManager!!.updateAtmosphere(this)
        if (hasUnderwaterMap) {
            this.scene!!.setUnderwater(true)
            this.underwaterMap = SceneGraph(this.scene!!, this.loctypeList!!, 1, this.sceneSizeX, this.sceneSizeY, true, this.tileSettingsBits!!, this.environmentManager!!)
            this.underwaterMap!!.enableLdShadows()
            this.underwaterMap!!.hdShadows = PreferenceManager.preferences!!.sceneryShadows!!.getValue()
            this.underwaterMap!!.hdWater = PreferenceManager.preferences!!.water!!.getValue() == 2
            this.underwaterMap!!.hdLight = PreferenceManager.preferences!!.lightDetail!!.getValue() == 1
            this.underwaterMap!!.blendFloor = PreferenceManager.preferences!!.goundBlending!!.getValue() == 1
            this.underwaterMap!!.enableTextures = PreferenceManager.preferences!!.textures!!.getValue() == 1
            if (!this.loadingType!!.constructedMap()) {
                decodeClientUnderlayMasks(this.underwaterMap!!, this.underwaterMapBuffer)
                if (!this.asyncMaploading) {
                    PingRequester.keepalive(true)
                }
            } else {
                decodeServerUnderlayMasks(this.underwaterMap!!, this.underwaterMapBuffer)
                if (!this.asyncMaploading) {
                    PingRequester.keepalive(true)
                }
            }
            this.underwaterMap!!.offsetTileHeights(this.surfaceSceneGraph!!.tileHeights[0])
            this.underwaterMap!!.initializeFloor(Renderers.activeToolkit!!, null, null)
            this.scene!!.setUnderwater(false)
            if (this.asyncMaploading) {
                sleep(50)
            }
        }
        this.surfaceSceneGraph!!.initializeFloor(Renderers.activeToolkit!!, if (hasUnderwaterMap) this.underwaterMap!!.tileHeights else null, this.collisionMaps)
        if (!this.loadingType!!.constructedMap()) {
            if (!this.asyncMaploading) {
                PingRequester.keepalive(true)
            }
            decodeLocMapsClient(this.surfaceSceneGraph!!, this.surfaceLocBuffer)
            if (this.npcSpawnBuffer != null) {
                decodeNpcSpawns()
            }
        } else {
            if (!this.asyncMaploading) {
                PingRequester.keepalive(true)
            }
            decodeLocMapsServer(this.surfaceSceneGraph!!, this.surfaceLocBuffer)
        }
        if (!this.asyncMaploading) {
            PingRequester.keepalive(true)
        }
        if (this.asyncMaploading) {
            Utils.time()
            while (!Renderers.activeToolkit!!.readyForRendering()) {
                sleep(1)
            }
        }
        this.surfaceSceneGraph!!.calculateUnderlayPalette(Renderers.activeToolkit!!, if (hasUnderwaterMap) this.scene!!.underwaterHeightmap!![0] else null, null)
        if (this.asyncMaploading) {
            sleep(75)
        }
        this.surfaceSceneGraph!!.calculateVisibility(false)
        if (this.asyncMaploading) {
            sleep(75)
        }
        if (!this.asyncMaploading) {
            PingRequester.keepalive(true)
        }
        if (hasUnderwaterMap) {
            this.scene!!.setUnderwater(true)
            if (!this.asyncMaploading) {
                PingRequester.keepalive(true)
            }
            if (!this.loadingType!!.constructedMap()) {
                decodeLocMapsClient(this.underwaterMap!!, this.underwaterLocBuffer)
            } else {
                decodeLocMapsServer(this.underwaterMap!!, this.underwaterLocBuffer)
            }
            if (!this.asyncMaploading) {
                PingRequester.keepalive(true)
            }
            //ADDED
            if (this.asyncMaploading) {
                Utils.time()
                while (!Renderers.activeToolkit!!.readyForRendering()) {
                    sleep(1)
                }
            }
            this.underwaterMap!!.calculateUnderlayPalette(Renderers.activeToolkit!!, null, this.scene!!.surfaceHeightmap[0])
            this.underwaterMap!!.calculateVisibility(true)
            if (!this.asyncMaploading) {
                PingRequester.keepalive(true)
            }
            this.scene!!.setUnderwater(false)
            if (this.asyncMaploading) {
                sleep(50)
            }
        }
        this.surfaceSceneGraph!!.resetUnderlays()
        if (this.underwaterMap != null) {
            this.underwaterMap!!.resetUnderlays()
        }
        this.scene!!.resetSurfaceTiles()
        if (this.asyncMaploading) {
            Utils.time()
            while (!Renderers.activeToolkit!!.readyForRendering()) {
                sleep(1)
            }
        }
        var async = false
        if (this.asyncMaploading) {
            val mapRegion = Resource.mapRegion
            buildRegionBase(mapRegion!!)
            Resource.mapRegionLoaderTask!!.setMapRegion(mapRegion)
            async = true
            Utils.time()
            synchronized(Client.ASYNC_MAPLOADING_LOCK) {
                Client.maploadingLockTaken = true
                try {
                    (Client.ASYNC_MAPLOADING_LOCK as Object).wait()
                } catch (interruptedException: InterruptedException) {
                    // NOOP
                }
            }
            Resource.mapRegion = this
            mapRegion.clear()
            updateEntities(GameState.LOADING_PLEASE_WAIT)
            resetNonCutsceneEntities()
        } else {
            SoundEffectPlayer.updatePlayers()
            this.environmentManager!!.calculateFog()
            GameShell.resetDrawCache()
        }
        for (plane in 0..3) {
            for (sceneX in 0..<this.sceneSizeX) {
                for (sceneY in 0..<this.sceneSizeY) {
                    GroundItemUpdating.removeGroundItem(plane, sceneX, sceneY)
                }
            }
        }
        for (index in Client.HINT_TRAILS.indices) {
            if (Client.HINT_TRAILS[index] != null) {
                Client.HINT_TRAILS[index]!!.addTo(this.scene)
            }
        }
        Scene.setRoofOverride()
        LocAction.onRegionChanged()
        if (GameShell.getApplicationType() == ApplicationType.APPLET && Client.GAME_CONNECTION_CONTEXT.getConnection() != null && Client.gameState == GameState.LOADING_PLEASE_WAIT) {
            val packet: TcpPacket = PacketEncoder.createPacket(ClientProt.UNK_82, Client.GAME_CONNECTION_CONTEXT.isaac)
            packet.buffer.writeInt(1057001181)
            Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
        }
        if (!this.loadingType!!.constructedMap()) {
            val regionStartX = (this.baseX - (this.sceneSizeX shr 4)) / 8
            val regionEndX = (this.baseX + (this.sceneSizeX shr 4)) / 8
            val regionStartY = (this.baseY - (this.sceneSizeY shr 4)) / 8
            val regionEndY = (this.baseY + (this.sceneSizeY shr 4)) / 8
            for (regionX in regionStartX - 1..regionEndX + 1) {
                for (regionY in regionStartY - 1..regionEndY + 1) {
                    if (regionX < regionStartX || regionX > regionEndX || regionY < regionStartY || regionY > regionEndY) {
                        Resource.MAPS!!.prefetch(getArchiveName(true, false, regionX, regionY))
                        Resource.MAPS!!.prefetch(getArchiveName(false, false, regionX, regionY))
                        Resource.MAPS!!.prefetch(getArchiveName(true, true, regionX, regionY))
                        Resource.MAPS!!.prefetch(getArchiveName(false, true, regionX, regionY))
                    }
                }
            }
        }
        if (Client.gameState == GameState.UNK_11) {
            Client.setGameState(GameState.UNK_5)
        } else if (Client.gameState == GameState.UNK_2) {
            Client.setGameState(GameState.UNK_0)
        } else if (Client.gameState == GameState.UNK_6) {
            Client.setGameState(GameState.UNK_7)
        } else if (Client.gameState == GameState.UNK_9) {
            Client.setGameState(GameState.IN_ACCOUNT_CREATION)
        } else {
            Client.setGameState(GameState.LOGGED_INGAME)
            if (Client.GAME_CONNECTION_CONTEXT.getConnection() != null) {
                val packet: TcpPacket = PacketEncoder.createPacket(ClientProt.REGION_LOADED_CONFIRM, Client.GAME_CONNECTION_CONTEXT.isaac)
                Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
            }
        }

        WorldMapSub1.reset()
        if (this.debugRegionLoadtime) {
            Console.writeLine((Utils.time() - this.debugRegionLoadtimeStart).toString())
            this.debugRegionLoadtime = false
        }
        if (async) {
            synchronized(Client.MAP_REGION_LOADER_LOCK) {
                (Client.MAP_REGION_LOADER_LOCK as Object).notify()
            }
        }
        return true
    }

    val completion: Int
        get() = 100 - this.missingMapCount * 100 / this.totalMapCount

    fun getLoctypeList(): ObjectTypeList {
        return this.loctypeList!!
    }

    fun getEnvironmentManager(): EnvironmentManager {
        return this.environmentManager!!
    }

    fun getScene(): Scene? {
        return this.scene
    }

    fun getBaseCoordinates(): CoordGrid {
        return this.baseCoordinates
    }
    fun decodeLocMapsServer(sceneGraph: SceneGraph, locBufferByRegion: Array<ByteArray?>) {
        for (z in 0..<sceneGraph.regionSizeZ) {
            if (!this.asyncMaploading) {
                SoundEffectPlayer.updatePlayers()
            }
            for (localChunkX in 0..<(this.sceneSizeX shr 3)) {
                for (localChunkY in 0..<(this.sceneSizeY shr 3)) {
                    val bitpackedRegionPosition = this.chunkBitpackedPosition[z]!![localChunkX]!![localChunkY]
                    if (bitpackedRegionPosition != -1) {
                        val chunkRotation = bitpackedRegionPosition shr 24 and 0x3
                        if (!sceneGraph.overlayHidden || chunkRotation == 0) {
                            val rotation = bitpackedRegionPosition shr 1 and 0x3
                            val regionX = bitpackedRegionPosition shr 14 and 0x3ff
                            val regionY = bitpackedRegionPosition shr 3 and 0x7ff
                            val regionId = (regionX / 8 shl 8) + regionY / 8
                            for (index in this.regionIds.indices) {
                                if (regionId == this.regionIds[index] && locBufferByRegion[index] != null) {
                                    sceneGraph.decodeLocMapServer(Renderers.activeToolkit!!, locBufferByRegion[index]!!, z, localChunkX * 8, localChunkY * 8, chunkRotation, (regionX and 0x7) * 8, (regionY and 0x7) * 8, rotation, this.collisionMaps as Array<CollisionMap?>)
                                    break
                                }
                            }
                        }
                    }
                    if (this.asyncMaploading) {
                        sleep(5)
                    }
                }
            }
        }
    }

    val settingsBits: SettingsBits
        get() = this.tileSettingsBits!!



    fun setActiveSettingsBits(settingsBits: Array<Array<ByteArray>>?) {
        this.bits = settingsBits
    }

    fun setLoctypelist(loctypelist: ObjectTypeList) {
        this.loctypeList = loctypelist
    }

    fun resetInteractiveEntities() {
        if (this.scene != null) {
            RenderedEntityCache.reset()
            this.scene!!.interactiveEntities.clear()
            this.scene = null
        }
    }

    fun setLoadingRegion() {
        this.currentLoadingStage = RegionLoadingStage.LOADING
        this.missingMapCount = 0
        this.totalMapCount = 1
        this.locsStillLoading = 0
        this.totalLocsLoading = 1
    }

    fun resetAtmosphere() {
        this.environmentManager = EnvironmentManager(Renderers.activeToolkit!!, this.sceneSizeX shr 3, this.sceneSizeY shr 3)
    }

    /**
     * In 317 you'll see the view range is 50 - 3500
     * this matches with what's calculated here, example: 104 * 34.46 = 3,583
     * since high revisions don't downscale models but move the camera further away
     * (proper model precision) upscale view range too to match this.
     * start: 50 in 317 --> << 2 --> 200
     * end: 3500 in 317 --> << 2 --> 14000
     * 
     */
    fun calculateViewRange() {
        this.viewRangeStart = 200
        this.viewRangeEnd = (34.46 * this.sceneSizeX).toInt()
        this.viewRangeEnd = this.viewRangeEnd shl 2
        if (Renderers.activeToolkit!!.extendBuildArea()) {
            this.viewRangeEnd += 512
        }
        this.viewRangeEnd += 3072 * Loader.FOG_DISTANCE_MULTIPLIER
    }

    fun getArchiveName(floorData: Boolean, underwater: Boolean, regionX: Int, regionY: Int): String {
        var mapName = ""
        if (floorData) {
            mapName = mapName + "m" + regionX + '_' + regionY
        } else {
            mapName = mapName + "l" + regionX + '_' + regionY
        }
        if (underwater) {
            mapName = 'u'.toString() + mapName
        }
        return mapName
    }

    fun setMapSize(size: MapSize) {
        if (!::activeMapSize.isInitialized || size !== this.activeMapSize) {
            this.sceneSizeY = size.size
            this.sceneSizeX = this.sceneSizeY
            this.chunkBitpackedPosition = Array<Array<IntArray?>?>(4) { Array<IntArray?>(this.sceneSizeX shr 3) { IntArray(this.sceneSizeY shr 3) } }
            this.drawPriority = Array<IntArray>(this.sceneSizeX) { IntArray(this.sceneSizeY) }
            this.drawPrioritySharedby = Array<IntArray>(this.sceneSizeX) { IntArray(this.sceneSizeY) }
            for (heightLevel in 0..3) {
                this.collisionMaps[heightLevel] = CollisionMap.createClipMap(this.sceneSizeX, this.sceneSizeY)
            }
            this.bits = Array<Array<ByteArray>>(4) { Array<ByteArray>(this.sceneSizeX) { ByteArray(this.sceneSizeY) } }
            this.tileSettingsBits = SettingsBits(this.sceneSizeX, this.sceneSizeY)
            resetAtmosphere()
            this.activeMapSize = size
        }
    }

    fun resetNonCutsceneEntities() {
        if (this.loadingType != RegionLoadingType.CUTSCENE && this.previousLoadingType != RegionLoadingType.CUTSCENE) {
            if (this.loadingType == RegionLoadingType.INSTANCE || this.loadingType == RegionLoadingType.INSTANCE_LARGE || this.previousLoadingType != this.loadingType && (this.loadingType == RegionLoadingType.STANDARD || this.previousLoadingType == RegionLoadingType.STANDARD)) {
                Client.NPC_UPDATE_INDEX = 0
                Client.npcCount = 0
                Client.NPC_MAP.clear()
            }
            this.previousLoadingType = this.loadingType
        }
    }

    fun performUpdate(BaseX: Int, BaseY: Int, newGamestate: GameState?, forcedPositionUpdate: Boolean) {
        if (Client.MAP_LOADING_STAGE == 2) {
            check(!this.asyncMaploading)
            Client.MAP_LOADING_STAGE = 1
            Client.CURRENT_CUTSCENE = -1
        }
        if (forcedPositionUpdate || this.baseX != BaseX || BaseY != this.baseY) {
            this.baseX = BaseX
            this.baseY = BaseY
            if (!this.asyncMaploading) {
                Client.setGameState(newGamestate!!)
                MessageBox.draw(LocalizedText.LOADING_PLEASE_WAIT.translate(Client.CURRENT_LANGUAGE!!), true, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont)
            }
            if (this.baseCoordinates != null) {
                this.lastCoordgrid = this.baseCoordinates
            } else {
                this.lastCoordgrid = CoordGrid(0, 0, 0)
            }
            this.baseCoordinates = CoordGrid(0, (this.baseX - (this.sceneSizeX shr 4)) * 8, (this.baseY - (this.sceneSizeY shr 4)) * 8)
            this.worldMapType = WorldMap.getMapDef(this.baseCoordinates!!.x, this.baseCoordinates!!.y)
            this.staticElements = StaticElements(0)
            if (!this.asyncMaploading) {
                updateEntities(newGamestate)
            }
        }
    }

    fun updateEntities(requestedGamestate: GameState?) {
        val deltaX = this.baseCoordinates!!.x - this.lastCoordgrid!!.x
        val deltaY = this.baseCoordinates!!.y - this.lastCoordgrid!!.y
        if (requestedGamestate == GameState.LOADING_PLEASE_WAIT) {
            for (npcIndex in 0..<Client.npcCount) {
                val objectNode: ObjectNode? = Client.NPC_ARRAY[npcIndex]
                if (objectNode != null) {
                    val npcEntity: NpcEntity? = objectNode.value as NpcEntity?
                    for (index in npcEntity!!.movementBaseX.indices) {
                        npcEntity!!.movementBaseX[index] -= deltaX
                        npcEntity!!.movementBaseY[index] -= deltaY
                    }
                    val abs: Vector3 = Vector3Pool.vector3From(npcEntity!!.getAbsolutePosition().coords)
                    abs.x -= (deltaX * 512).toFloat()
                    abs.z -= (deltaY * 512).toFloat()
                    npcEntity!!.setBasePosition(abs)
                    abs.pushVectorStack()
                }
            }
        } else {
            var copyExisting = false
            Client.NPC_UPDATE_INDEX = 0
            val x = this.sceneSizeX * 512 - 512
            val y = this.sceneSizeY * 512 - 512
            for (npcIndex in 0..<Client.npcCount) {
                val objectNode: ObjectNode? = Client.NPC_ARRAY[npcIndex]
                if (objectNode != null) {
                    val npcEntity: NpcEntity? = objectNode.value as NpcEntity?
                    val abs: Vector3 = Vector3Pool.vector3From(npcEntity!!.getAbsolutePosition().coords)
                    abs.x -= (deltaX * 512).toFloat()
                    abs.z -= (deltaY * 512).toFloat()
                    npcEntity!!.setBasePosition(abs)
                    if (abs.x.toInt() >= 0 && abs.x.toInt() <= x && abs.z.toInt() >= 0 && abs.z.toInt() <= y) {
                        var hasUpdate = true
                        for (index in npcEntity!!.movementBaseX.indices) {
                            npcEntity!!.movementBaseX[index] -= deltaX
                            npcEntity!!.movementBaseY[index] -= deltaY
                            if (npcEntity!!.movementBaseX[index] < 0 || npcEntity!!.movementBaseX[index] >= this.sceneSizeX || npcEntity!!.movementBaseY[index] < 0 || npcEntity!!.movementBaseY[index] >= this.sceneSizeY) {
                                hasUpdate = false
                            }
                        }
                        if (hasUpdate) {
                            Client.NPC_UPDATE_INDICES[++Client.NPC_UPDATE_INDEX - 1] = npcEntity!!.playerIndex
                        } else {
                            npcEntity!!.setDirection(null)
                            objectNode.unlink()
                            copyExisting = true
                        }
                    } else {
                        npcEntity!!.setDirection(null)
                        objectNode.unlink()
                        copyExisting = true
                    }
                    abs.pushVectorStack()
                }
            }
            if (copyExisting) {
                Client.npcCount = Client.NPC_MAP.size()
                var count = 0
                var objectNode: ObjectNode?
                val npcIt= Client.NPC_MAP.iterator()
                while (npcIt.hasNext()) {
                    objectNode = npcIt.next()
                    Client.NPC_ARRAY[count++] = objectNode
                }
            }
        }
        for (playerIndex in 0..2047) {
            val playerEntity: PlayerEntity? = PlayerUpdate.players[playerIndex]
            if (playerEntity != null) {
                for (index in playerEntity.movementBaseX.indices) {
                    playerEntity.movementBaseX[index] -= deltaX
                    playerEntity.movementBaseY[index] -= deltaY
                }
                val abs: Vector3 = Vector3Pool.vector3From(playerEntity.getAbsolutePosition().coords)
                abs.x -= (deltaX * 512).toFloat()
                abs.z -= (deltaY * 512).toFloat()
                playerEntity.setBasePosition(abs)
                abs.pushVectorStack()
            }
        }
        val hintArrows: Array<HintArrow?> = PlayerUpdate.HINT_ARROWS
        for (arrowIndex in hintArrows.indices) {
            val hintArrow: HintArrow? = hintArrows[arrowIndex]
            if (hintArrow != null) {
                hintArrow.x -= deltaX * 512
                hintArrow.y -= deltaY * 512
            }
        }
        run {
            var action = LocAction.pendingRemovals.getFirst() as LocAction?
            while (action != null) {
                action.x -= deltaX
                action.y -= deltaY
                if (this.loadingType != RegionLoadingType.INSTANCE_LARGE_NO_RELOAD_UPDATEZONES && (action.x < 0 || action.y < 0 || action.x >= this.sceneSizeX || action.y >= this.sceneSizeY)) {
                    action.unlink()
                }
                action = LocAction.pendingRemovals.next() as LocAction?
            }
        }
        var action = LocAction.pendingCustomizations.getFirst() as LocAction?
        while (action != null) {
            action.x -= deltaX
            action.y -= deltaY
            if (this.loadingType != RegionLoadingType.INSTANCE_LARGE_NO_RELOAD_UPDATEZONES && (action.x < 0 || action.y < 0 || action.x >= this.sceneSizeX || action.y >= this.sceneSizeY)) {
                action.unlink()
            }
            action = LocAction.pendingCustomizations.next() as LocAction?
        }
        var groundItems: GroundItemWrapper? = Client.GROUND_ITEMS.first()
        while (groundItems != null) {
            val rotation = (groundItems.pointer shr 28 and 0x3L).toInt()
            val sceneX = (groundItems.pointer and 0x3fffL).toInt()
            val nodeSizeX = sceneX - this.baseCoordinates!!.x
            val sceneY = (groundItems.pointer shr 14 and 0x3fffL).toInt()
            val nodeSizeY = sceneY - this.baseCoordinates!!.y
            if (this.scene != null) {
                if (nodeSizeX >= 0 && nodeSizeY >= 0 && nodeSizeX < this.sceneSizeX && nodeSizeY < this.sceneSizeY && nodeSizeX < this.scene!!.sceneDimensionX && nodeSizeY < this.scene!!.sceneDimensionY) {
                    if (this.scene!!.tiles != null) {
                        this.scene!!.removeGroundItem(rotation, nodeSizeX, nodeSizeY)
                    }
                } else if (this.loadingType != RegionLoadingType.INSTANCE_LARGE_NO_RELOAD_UPDATEZONES) {
                    groundItems.unlink()
                }
            }
            groundItems = Client.GROUND_ITEMS.next()
        }
        if (ComponentMinimap.minimapFlagX != 0) {
            ComponentMinimap.minimapFlagX -= deltaX
            ComponentMinimap.minimapFlagY -= deltaY
        }
        AreaSoundPlayer.initialize()
        AmbientSound.unlinkSounds(false)
        if (requestedGamestate == GameState.LOADING_PLEASE_WAIT) {
            Client.anInt7262 -= deltaX * 512
            Client.anInt7376 -= deltaY * 512
            Camera.cameraBaseX -= deltaX * 512
            Camera.cameraBaseY -= deltaY * 512
            if (Camera.cameraCalculationType != 4) {
                Camera.cameraCalculationType = 2
                StaticMethods.anInt833 = -1
                PingRequester.anInt5864 = -1
            }
        } else {
            Camera.cameraMovementLocalX -= deltaX
            Camera.cameraMovementLocalY -= deltaY
            Camera.lookX -= deltaX
            Camera.lookY -= deltaY
            Camera.cameraPositionAbsoluteX -= deltaX * 512
            Camera.cameraPositionAbsoluteY -= deltaY * 512
            if (abs(deltaX) > this.sceneSizeX || abs(deltaY) > this.sceneSizeY) {
                this.environmentManager!!.requestDirectEnvironmentTransition()
            }
        }
        MiniMenu.reset()
        ComponentMinimap.reset()
        Client.ACTIVE_GFX.clear()
        Client.PROJECTILES.remove_all()
        Client.TILE_MESSAGES.unlink()
        ParticleSystemManager.destroy()
    }

    fun clear() {
        this.underwaterMap = null
        this.surfaceSceneGraph = null
        for (z in 0..3) {
            if (this.collisionMaps[z] != null) {
                this.collisionMaps[z]!!.reset()
            }
        }
        if (this.tileSettingsBits != null) {
            this.tileSettingsBits!!.reset()
        }
        if (this.environmentManager != null) {
            this.environmentManager!!.resetChunkEnvironments()
        }
        if (this.scene != null) {
            this.scene!!.interactiveEntities.clear()
            this.scene = null
        }
    }

    fun sleep(ms: Int) {
//        try {
//            Thread.sleep(ms);
//        } catch (InterruptedException interruptedException) {
//        	// NOOP
//        }
    }

    fun decodeClientUnderlayMasks(sceneGraph: SceneGraph, mapDatas: Array<ByteArray?>) {
        val mapCount = mapDatas.size
        for (mapIndex in 0..<mapCount) {
            val mapData = mapDatas[mapIndex]
            if (mapData != null) {
                val stream = Packet(mapData)
                val x = this.regionIds[mapIndex] shr 8
                val y = this.regionIds[mapIndex] and 0xff
                val chunkX = x * 64 - this.baseCoordinates!!.x
                val chunkY = y * 64 - this.baseCoordinates!!.y
                if (!this.asyncMaploading) {
                    SoundEffectPlayer.updatePlayers()
                }
                sceneGraph.decodeTilesClient(stream, chunkX, chunkY, this.baseCoordinates!!.x, this.baseCoordinates!!.y, this.collisionMaps)
                sceneGraph.decodeEffectsClient(Renderers.activeToolkit!!, stream, chunkX, chunkY)
            }
        }
        for (mapIndex in 0..<mapCount) {
            val x = (this.regionIds[mapIndex] shr 8) * 64 - this.baseCoordinates!!.x
            val y = (this.regionIds[mapIndex] and 0xff) * 64 - this.baseCoordinates!!.y
            val mapBytes = mapDatas[mapIndex]
            if (mapBytes == null && this.baseY < 800) {
                if (!this.asyncMaploading) {
                    SoundEffectPlayer.updatePlayers()
                }
                sceneGraph.clearAllPlanes(x, y)
            }
        }
    }

    fun decodeServerUnderlayMasks(sceneGraph: SceneGraph, mapdata: Array<ByteArray?>) {
        for (sceneZ in 0..<sceneGraph.regionSizeZ) {
            if (!this.asyncMaploading) {
                SoundEffectPlayer.updatePlayers()
            }
            for (chunkX in 0..<(this.sceneSizeX shr 3)) {
                for (chunkY in 0..<(this.sceneSizeY shr 3)) {
                    val bitpacked = this.chunkBitpackedPosition[sceneZ]!![chunkX]!![chunkY]
                    if (bitpacked != -1) {
                        val plane = bitpacked shr 24 and 0x3
                        if (!sceneGraph.overlayHidden || plane == 0) {
                            val rotation = bitpacked shr 1 and 0x3
                            val absoluteX = bitpacked shr 14 and 0x3ff
                            val absoluteY = bitpacked shr 3 and 0x7ff
                            val regionId = (absoluteX / 8 shl 8) + absoluteY / 8
                            for (mapIndex in this.regionIds.indices) {
                                if (regionId == this.regionIds[mapIndex] && mapdata[mapIndex] != null) {
                                    val stream = Packet(mapdata[mapIndex]!!)
                                    sceneGraph.decodeTilesServer(stream, sceneZ, chunkX * 8, chunkY * 8, plane, absoluteX, absoluteY, rotation, this.collisionMaps)
                                    sceneGraph.decodeEffectsServer(Renderers.activeToolkit!!, stream, sceneZ, chunkX * 8, chunkY * 8, plane, absoluteX, absoluteY, rotation)
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
        for (sceneZ in 0..<sceneGraph.regionSizeZ) {
            if (!this.asyncMaploading) {
                SoundEffectPlayer.updatePlayers()
            }
            for (chunkX in 0..<(this.sceneSizeX shr 3)) {
                for (chunkY in 0..<(this.sceneSizeY shr 3)) {
                    val bitpacked = this.chunkBitpackedPosition[sceneZ]!![chunkX]!![chunkY]
                    if (bitpacked == -1) {
                        sceneGraph.clearHeights(sceneZ, chunkX * 8, chunkY * 8, 8, 8)
                    }
                }
            }
        }
    }

    fun decodeLocMapsClient(sceneGraph: SceneGraph, locMapdataByRegion: Array<ByteArray?>) {
        val regionCount = this.surfaceMapBuffer.size
        for (regionIndex in 0..<regionCount) {
            val currentRegionBuffer = locMapdataByRegion[regionIndex]
            if (currentRegionBuffer != null) {
                val chunkX = (this.regionIds[regionIndex] shr 8) * 64 - this.baseCoordinates!!.x
                val chunkY = (this.regionIds[regionIndex] and 0xff) * 64 - this.baseCoordinates!!.y
                if (!this.asyncMaploading) {
                    SoundEffectPlayer.updatePlayers()
                }
                sceneGraph.decodeLocMapClient(Renderers.activeToolkit!!, currentRegionBuffer, chunkX, chunkY, this.collisionMaps as Array<CollisionMap?>)
                if (this.asyncMaploading) {
                    sleep(10)
                }
            }
        }
    }

    fun decodeNpcSpawns() {
        val npcMapCount = this.npcSpawnBuffer!!.size
        for (mapIndex in 0..<npcMapCount) {
            if (this.npcSpawnBuffer!![mapIndex] != null) {
                var cursorPosition = -1
                for (cursor in 0..<Client.npcSpawnCursorPos) {
                    if (Client.npcSpawnMaps[cursor] == this.regionIds[mapIndex]) {
                        cursorPosition = cursor
                        break
                    }
                }
                if (cursorPosition == -1) {
                    Client.npcSpawnMaps[Client.npcSpawnCursorPos] = this.regionIds[mapIndex]
                    cursorPosition = ++Client.npcSpawnCursorPos - 1
                }
                val stream = Packet(this.npcSpawnBuffer!![mapIndex]!!)
                var index = 0
                while (stream.currentPosition < this.npcSpawnBuffer!![mapIndex]!!.size && index < 511 && Client.NPC_UPDATE_INDEX < 1023) {
                    val slot = cursorPosition or (index++ shl 6)
                    val i_8 = stream.readUShort()
                    val plane = i_8 shr 14
                    val i_10 = i_8 shr 7 and 0x3f
                    val i_11 = i_8 and 0x3f
                    val i_12 = (this.regionIds[mapIndex] shr 8) * 64 - this.baseCoordinates!!.x + i_10
                    val i_13 = (this.regionIds[mapIndex] and 0xff) * 64 - this.baseCoordinates!!.y + i_11
                    val npcType: NPCType = TypeLists.NPC!!.list(stream.readUShort())
                    val npcObjectNode: ObjectNode? = Client.NPC_MAP.get(slot.toLong())
                    if (npcObjectNode == null && (npcType.walkingFlags.toInt() and 0x1) > 0 && i_12 >= 0 && i_12 + npcType.size < this.sceneSizeX && i_13 >= 0 && i_13 + npcType.size < this.sceneSizeY) {
                        val npcEntity: NpcEntity = NpcEntity(this.scene)
                        npcEntity.playerIndex = slot
                        val npcEntityObjectNode = ObjectNode(npcEntity)
                        Client.NPC_MAP.put(npcEntityObjectNode, slot.toLong())
                        Client.NPC_ARRAY[++Client.npcCount - 1] = npcEntityObjectNode
                        Client.NPC_UPDATE_INDICES[++Client.NPC_UPDATE_INDEX - 1] = slot
                        npcEntity.lastUpdate = Client.cycles
                        npcEntity.setDirection(npcType)
                        npcEntity.setSize(npcEntity.definitions!!.size)
                        npcEntity.turnDirection = npcEntity.definitions!!.turnDirection shl 3
                        npcEntity.turn(npcEntity.definitions!!.respawnDirection!!.getRotation().getValue() shl 11 and 0x3fff, true)
                        npcEntity.move(plane, i_12, i_13, true, npcEntity.getSize())
                    }
                }
            }
        }
    }

    fun resetBasePosition() {
        this.baseCoordinates = CoordGrid()
        this.baseY = 0
        this.baseX = 0
    }

    fun getLocalLighting(z: Int): Array<ByteArray>? {
        return if (this.surfaceSceneGraph != null && this.surfaceSceneGraph!!.lighting != null && this.surfaceSceneGraph!!.lighting!![z] != null) this.surfaceSceneGraph!!.lighting!![z] else null
    }


    fun getStaticElements(): StaticElements {
        return this.staticElements
    }

    fun locLoadingPercent(): Int {
        return 100 - this.locsStillLoading * 100 / this.totalLocsLoading
    }

    fun reload() {
        if (this.asyncMaploading) {
            setLoadingRegion()
            GraphicsCache.resetWorldmapCaches(true)
            val mapRegion = Resource.mapRegion
            this.regionIds = mapRegion!!.regionIds
            this.surfaceMapArchives = mapRegion.surfaceMapArchives
            this.surfaceLocArchives = mapRegion.surfaceLocArchives
            this.npcSpawnArchives = mapRegion.npcSpawnArchives
            this.underwaterMapArchives = mapRegion.underwaterMapArchives
            this.underwaterLocArchives = mapRegion.underwaterLocArchives
            this.xteas = mapRegion.xteas
            this.surfaceMapBuffer = mapRegion.surfaceMapBuffer
            this.surfaceLocBuffer = mapRegion.surfaceLocBuffer
            this.npcSpawnBuffer = mapRegion.npcSpawnBuffer
            this.underwaterMapBuffer = mapRegion.underwaterMapBuffer
            this.underwaterLocBuffer = mapRegion.underwaterLocBuffer
            this.collisionMaps = mapRegion.collisionMaps
            this.tileSettingsBits = mapRegion.tileSettingsBits
            this.environmentManager = mapRegion.environmentManager
            this.chunkBitpackedPosition = mapRegion.chunkBitpackedPosition
            this.viewRangeStart = mapRegion.viewRangeStart
            this.viewRangeEnd = mapRegion.viewRangeEnd
            this.worldMapType = mapRegion.worldMapType
            this.staticElements = mapRegion.staticElements
            this.drawPriority = mapRegion.drawPriority
            this.drawPrioritySharedby = mapRegion.drawPrioritySharedby
            this.bits = mapRegion.bits
            this.loadingType = mapRegion.loadingType
            this.activeMapSize = mapRegion.activeMapSize
            this.baseCoordinates = mapRegion.baseCoordinates
            this.lastCoordgrid = mapRegion.lastCoordgrid
            this.baseX = mapRegion.baseX
            this.baseY = mapRegion.baseY
            this.sceneSizeX = mapRegion.sceneSizeX
            this.sceneSizeY = mapRegion.sceneSizeY
        } else if (Client.gameState == GameState.UNK_5) {
            Client.setGameState(GameState.UNK_11)
        } else if (Client.gameState == GameState.UNK_0) {
            Client.setGameState(GameState.UNK_2)
        } else if (Client.gameState == GameState.UNK_7) {
            Client.setGameState(GameState.UNK_6)
        } else if (Client.gameState == GameState.LOGGED_INGAME) {
            Client.setGameState(GameState.LOADING_PLEASE_WAIT)
        } else if (Client.gameState == GameState.IN_ACCOUNT_CREATION) {
            Client.setGameState(GameState.UNK_9)
        }
    }

    fun getCollisionMap(z: Int): CollisionMap {
        return this.collisionMaps[z]!!
    }

    fun getLoadingType(): RegionLoadingType {
        return this.loadingType!!
    }

    companion object {
        /**
         * The color behind the scene / the default background pixel color
         */
        val BACKGROUND_COLOR: Int = 0xffffff.inv()

        var requiresClippingReset: Boolean = false
        var requiresDisplayResize: Boolean = false

        /**
         * Draws clipped tiles as pink on the minimap
         */
        var visualizeClippedTilesOnMinimap: Boolean = false
        @JvmField
        val staticElementsAwaitingDrawing: NodeCollection = NodeCollection()
        @JvmField
        var CUTSCENE_MAP_XTEAS: Array<IntArray>? = null

        @JvmStatic
        fun tileheightForAbsolutePosition(absoluteX: Int, absoluteY: Int, plane: Int): Int {
            if (Resource.mapRegion!!.getScene()!! == null) {
                return 0
            }
            val sceneX = absoluteX shr 9
            val sceneY = absoluteY shr 9
            if (sceneX >= 0 && sceneY >= 0 && sceneX <= Resource.mapRegion!!.sceneSizeX - 1 && sceneY <= Resource.mapRegion!!.sceneSizeY - 1) {
                var collisionPlane = plane
                if (plane < 3 && (Resource.mapRegion!!.settingsBits.tileFunctionMasks[1][sceneX][sceneY].toInt() and 0x2) != 0) {
                    collisionPlane = plane + 1
                }
                return Resource.mapRegion!!.getScene()!!!!.localHeightmap[collisionPlane]!!.getAverageHeight(absoluteX, absoluteY)
            }
            return 0
        }

        fun isMapSpriteCached(z: Int, x: Int, y: Int): Boolean {
            val scene = Resource.mapRegion!!.getScene()!!!!
            var fullyCached = true
            var gameObject: GameObject? = scene.getWall(z, x, y) as GameObject?
            if (gameObject != null) {
                fullyCached = fullyCached and MapSpriteTypeList.isCached(gameObject)
            }
            gameObject = scene.getInteractiveObject(z, x, y, Client.INTERACTIVE_OBJECT_COMPARABLE) as GameObject?
            if (gameObject != null) {
                fullyCached = fullyCached and MapSpriteTypeList.isCached(gameObject)
            }
            gameObject = scene.getGroundDecoration(z, x, y) as GameObject?
            if (gameObject != null) {
                fullyCached = fullyCached and MapSpriteTypeList.isCached(gameObject)
            }
            return fullyCached
        }

        @JvmStatic
        fun handleRoofDisplay() {
            if (PreferenceManager.preferences!!.removeRoofsOptionOverride!!.getValue() == 2) {
                val settingsBits = Resource.mapRegion!!.bits
                val b_2 = (Client.timesSceneRendered - 4 and 0xff).toByte()
                val x: Int = Client.timesSceneRendered % Resource.mapRegion!!.sceneSizeX
                for (plane in 0..3) {
                    for (y in 0..<Resource.mapRegion!!.sceneSizeY) {
                        settingsBits!![plane]!![x]!![y] = b_2
                    }
                }
                if (PlayerUpdate.MY_PLAYER_PLANE != 3) {
                    for (index in 0..1) {
                        Client.visibleBelowDrawheight!![index] = -1000000
                        Client.visibleAboveX!![index] = 1000000
                        Client.visibleBelowX!![index] = 0
                        Client.visibleAboveY!![index] = 1000000
                        Client.visibleBelowY!![index] = 0
                    }

                    val absolutePosition: Vector3 = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
                    var absoluteX = absolutePosition.x.toInt()
                    var absoluteZ = absolutePosition.z.toInt()
                    val regionMap = Resource.mapRegion!!.settingsBits
                    val scene = Resource.mapRegion!!.getScene()!!!!
                    if (Camera.cameraCalculationType != 2 && PingRequester.anInt5864 == -1) {
                        val tileheight: Int = tileheightForAbsolutePosition(Camera.cameraPositionAbsoluteX, Camera.cameraPositionAbsoluteY, PlayerUpdate.MY_PLAYER_PLANE)
                        if (tileheight - Camera.cameraPositionAbsoluteZ < 3200 && (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][Camera.cameraPositionAbsoluteX shr 9][Camera.cameraPositionAbsoluteY shr 9].toInt() and 0x4) != 0) {
                            Companion.calculateVisibilityAdjustments(scene.tiles as Array<Array<Array<Tile>?>?>?, 1, Camera.cameraPositionAbsoluteX shr 9, Camera.cameraPositionAbsoluteY shr 9, false)
                        }
                    } else {
                        if (Camera.cameraCalculationType != 2) {
                            absoluteX = PingRequester.anInt5864
                            absoluteZ = StaticMethods.anInt833
                        }

                        if ((regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][absoluteX shr 9][absoluteZ shr 9].toInt() and 0x4) != 0) {
                            Companion.calculateVisibilityAdjustments(scene.tiles as Array<Array<Array<Tile>?>?>?, 0, absoluteX shr 9, absoluteZ shr 9, false)
                        } else if (Camera.cameraYaw < 2560) {
                            var cameraBaseX: Int = Camera.cameraPositionAbsoluteX shr 9
                            var cameraBaseZ: Int = Camera.cameraPositionAbsoluteY shr 9
                            val baseX = absoluteX shr 9
                            val baseZ = absoluteZ shr 9

                            val cameraBaseXOffset: Int
                            if (baseX > cameraBaseX) {
                                cameraBaseXOffset = baseX - cameraBaseX
                            } else {
                                cameraBaseXOffset = cameraBaseX - baseX
                            }

                            val cameraBaseZOffset: Int
                            if (baseZ > cameraBaseZ) {
                                cameraBaseZOffset = baseZ - cameraBaseZ
                            } else {
                                cameraBaseZOffset = cameraBaseZ - baseZ
                            }

                            if ((cameraBaseXOffset != 0 || cameraBaseZOffset != 0) && cameraBaseXOffset > -Resource.mapRegion!!.sceneSizeX && cameraBaseXOffset < Resource.mapRegion!!.sceneSizeX && cameraBaseZOffset > -Resource.mapRegion!!.sceneSizeY && cameraBaseZOffset < Resource.mapRegion!!.sceneSizeY) {
                                var i_16: Int
                                val i_18: Int
                                if (cameraBaseXOffset > cameraBaseZOffset) {
                                    i_18 = cameraBaseZOffset * 65536 / cameraBaseXOffset
                                    i_16 = 32768

                                    while (cameraBaseX != baseX) {
                                        if (cameraBaseX < baseX) {
                                            ++cameraBaseX
                                        } else {
                                            --cameraBaseX
                                        }

                                        if ((regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][cameraBaseX][cameraBaseZ].toInt() and 0x4) != 0) {
                                            Companion.calculateVisibilityAdjustments(scene.tiles as Array<Array<Array<Tile>?>?>?, 1, cameraBaseX, cameraBaseZ, false)
                                            break
                                        }

                                        i_16 += i_18
                                        if (i_16 >= 65536) {
                                            i_16 -= 65536
                                            if (cameraBaseZ < baseZ) {
                                                ++cameraBaseZ
                                            } else if (cameraBaseZ > baseZ) {
                                                --cameraBaseZ
                                            }

                                            if ((regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][cameraBaseX][cameraBaseZ].toInt() and 0x4) != 0) {
                                                Companion.calculateVisibilityAdjustments(scene.tiles as Array<Array<Array<Tile>?>?>?, 1, cameraBaseX, cameraBaseZ, false)
                                                break
                                            }
                                        }
                                    }
                                } else {
                                    i_18 = cameraBaseXOffset * 65536 / cameraBaseZOffset
                                    i_16 = 32768

                                    while (baseZ != cameraBaseZ) {
                                        if (cameraBaseZ < baseZ) {
                                            ++cameraBaseZ
                                        } else {
                                            --cameraBaseZ
                                        }

                                        if ((regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][cameraBaseX][cameraBaseZ].toInt() and 0x4) != 0) {
                                            Companion.calculateVisibilityAdjustments(scene.tiles as Array<Array<Array<Tile>?>?>?, 1, cameraBaseX, cameraBaseZ, false)
                                            break
                                        }

                                        i_16 += i_18
                                        if (i_16 >= 65536) {
                                            i_16 -= 65536
                                            if (cameraBaseX < baseX) {
                                                ++cameraBaseX
                                            } else if (cameraBaseX > baseX) {
                                                --cameraBaseX
                                            }

                                            if ((regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][cameraBaseX][cameraBaseZ].toInt() and 0x4) != 0) {
                                                Companion.calculateVisibilityAdjustments(scene.tiles as Array<Array<Array<Tile>?>?>?, 1, cameraBaseX, cameraBaseZ, false)
                                                break
                                            }
                                        }
                                    }
                                }
                            } else {
                                val grid = Resource.mapRegion!!.getBaseCoordinates()
                                PacketEncoder.sendErrorToServer(cameraBaseX.toString() + "," + cameraBaseZ + " " + baseX + "," + baseZ + " " + grid.x + "," + grid.y, RuntimeException())
                            }
                        }
                    }
                }
            }
        }

        fun addCustomisation(z: Int, type: Int, x: Int, y: Int, locCustomisation: ObjectTypeCustomisation?) {
            if (x >= 1 && y >= 1 && x <= Resource.mapRegion!!.sceneSizeX - 2 && y <= Resource.mapRegion!!.sceneSizeY - 2 && Resource.mapRegion!!.getScene()!! != null) {
                val gameObject: GameObject? = Resource.mapRegion!!.surfaceSceneGraph!!.getGameObject(z, type, x, y)
                if (gameObject != null && locCustomisation != null) {
                    if (gameObject is AnimatedInteractiveObject) {
                        (gameObject as AnimatedInteractiveObject).addCustomisation(locCustomisation)
                    } else if (gameObject is AnimatedGroundDecoration) {
                        (gameObject as AnimatedGroundDecoration).addCustomisation(locCustomisation)
                    } else if (gameObject is AnimatedWall) {
                        (gameObject as AnimatedWall).addCustomisation(locCustomisation)
                    } else if (gameObject is AnimatedWallDecoration) {
                        (gameObject as AnimatedWallDecoration).addCustomisation(locCustomisation)
                    }
                }
            }
        }

        fun upload(tiles: Array<Array<Array<Tile?>?>?>) {
            for (z in tiles.indices) {
                val localTiles: Array<Array<Tile?>?> = tiles[z]!!
                for (x in localTiles.indices) {
                    for (y in localTiles[x]!!.indices) {
                        val tile = localTiles[x]!![y]
                        if (tile != null) {
                            if (tile.groundDecoration is GameObject) {
                                (tile.groundDecoration as GameObject).upload()
                            }

                            if (tile.primaryWallDecoration is GameObject) {
                                (tile.primaryWallDecoration as GameObject).upload()
                            }

                            if (tile.secondaryWallDecoration is GameObject) {
                                (tile.secondaryWallDecoration as GameObject).upload()
                            }

                            if (tile.primaryWall is GameObject) {
                                (tile.primaryWall as GameObject).upload()
                            }

                            if (tile.secondaryWall is GameObject) {
                                (tile.secondaryWall as GameObject).upload()
                            }

                            var interactives: InteractiveEntityContainer? = tile.interactives
                            while (interactives != null) {
                                val interactive: AbstractInteractiveEntity? = interactives.interactiveObject
                                if (interactive is GameObject) {
                                    (interactive as GameObject).upload()
                                }
                                interactives = interactives.child
                            }
                        }
                    }
                }
            }
        }

        fun resetDrawPriorities() {
            val drawPriorities = Resource.mapRegion!!.drawPriority
            val regionSizeX = Resource.mapRegion!!.sceneSizeX
            val regionSizeY = Resource.mapRegion!!.sceneSizeY

            for (x in 0..<regionSizeX) {
                val prios = drawPriorities[x]
                for (y in 0..<regionSizeY) {
                    prios[y] = 0
                }
            }
        }

        fun requestDebugMapUpdate() {
            if (PreferenceManager.preferences!!.asyncMaploading!!.getValue() == 1) {
                Resource.mapRegionLoaderTask!!.requestMapUpdate(RegionLoadRequest(RegionLoadingType.RELOAD, null))
            } else {
                Resource.mapRegion!!.reload()
                ComponentMinimap.reset()
            }
        }

        @JvmStatic
        fun addGfxsToScene() {
            for (positionedGfx in Client.ACTIVE_GFX) {
                val objectGfx: SpotAnimation = positionedGfx.objectGfx
                if (Resource.mapRegion!!.getScene()!! != null && objectGfx.notAccelerated()) {
                    Resource.mapRegion!!.getScene()!!!!.addInteractive(objectGfx, true)
                }
            }
        }

        @JvmStatic
        fun cycleGfx() {
            for (positionedGfx in Client.ACTIVE_GFX) {
                val spotAnimation: SpotAnimation = positionedGfx.objectGfx
                spotAnimation.progressCycle()
                if (spotAnimation.needsReset()) {
                    positionedGfx.unlink()
                    spotAnimation.resetCycle()
                }
            }
        }

        @JvmStatic
        fun addProjectileSpawnsToScene() {
            var spawnRequest: ProjectileSpawnRequest? = Client.PROJECTILES.getFirst() as ProjectileSpawnRequest?
            while (spawnRequest != null) {
                val projectile: Projectile = spawnRequest.projectile
                if (Client.cycles > projectile.cyclesUntilDisappear) {
                    spawnRequest.unlink()
                    projectile.nextParticleCycle()
                } else if (Client.cycles >= projectile.cycleToSpawnAt) {
                    projectile.cycle()
                    if (projectile.entityLockOn > 0) {
                        if (Client.MAP_LOADING_STAGE == 4) {
                            val animable3: PathingEntity = CutsceneTypeList.CUTSCENE_ENTITIES!![projectile.entityLockOn - 1].getEntity()
                            if (animable3 != null) {
                                val abs: Vector3 = animable3.getAbsolutePosition().coords
                                if (abs.x.toInt() >= 0 && abs.x.toInt() < Resource.mapRegion!!.sceneSizeX * 512 && abs.z.toInt() >= 0 && abs.z.toInt() < Resource.mapRegion!!.sceneSizeY * 512) {
                                    projectile.start(abs.x.toInt(), abs.z.toInt(), tileheightForAbsolutePosition(abs.x.toInt(), abs.z.toInt(), animable3.plane.toInt()) - projectile.endHeight, Client.cycles)
                                }
                            }
                        } else {
                            val objectNode: ObjectNode? = Client.NPC_MAP.get((projectile.entityLockOn - 1).toLong())
                            if (objectNode != null) {
                                val npcEntity: NpcEntity? = objectNode.value as NpcEntity?
                                val abs: Vector3 = npcEntity!!.getAbsolutePosition().coords
                                if (abs.x.toInt() >= 0 && abs.x.toInt() < Resource.mapRegion!!.sceneSizeX * 512 && abs.z.toInt() >= 0 && abs.z.toInt() < Resource.mapRegion!!.sceneSizeY * 512) {
                                    projectile.start(abs.x.toInt(), abs.z.toInt(), tileheightForAbsolutePosition(abs.x.toInt(), abs.z.toInt(), projectile.plane.toInt()) - projectile.endHeight, Client.cycles)
                                }
                            }
                        }
                    }
                    if (projectile.entityLockOn < 0) {
                        val lockOn: Int = -projectile.entityLockOn - 1
                        val player: PlayerEntity?
                        if (lockOn == PlayerUpdate.myPlayerIndex) {
                            player = PlayerUpdate.MY_PLAYER
                        } else {
                            player = PlayerUpdate.players[lockOn]
                        }
                        if (player != null) {
                            val abs: Vector3 = player.getAbsolutePosition().coords
                            if (abs.x.toInt() >= 0 && abs.x.toInt() < Resource.mapRegion!!.sceneSizeX * 512 && abs.z.toInt() >= 0 && abs.z.toInt() < Resource.mapRegion!!.sceneSizeY * 512) {
                                projectile.start(abs.x.toInt(), abs.z.toInt(), tileheightForAbsolutePosition(abs.x.toInt(), abs.z.toInt(), projectile.plane.toInt()) - projectile.endHeight, Client.cycles)
                            }
                        }
                    }
                    projectile.animate(Client.animatingStep)
                    Resource.mapRegion!!.getScene()!!!!.addInteractive(projectile, true)
                }
                spawnRequest = Client.PROJECTILES.next() as ProjectileSpawnRequest?
            }
        }

        fun applyGameobjectAction(action: LocAction, customize: Boolean) {
            if (action.inUpdateZone) {
                if (action.srcId < 0 || ObjectType.isTypeCached(Resource.mapRegion!!.getLoctypeList(), action.srcId, action.srcShape)) {
                    if (customize) {
                        addCustomisation(action.plane, action.gameObjectType, action.x, action.y, null)
                    } else {
                        transformObject(action.plane, action.gameObjectType, action.x, action.y, action.srcId, action.srcRotation, action.srcShape, -1)
                    }
                    action.unlink()
                }
            } else if (action.queued && action.x >= 1 && action.y >= 1 && action.x <= Resource.mapRegion!!.sceneSizeX - 2 && action.y <= Resource.mapRegion!!.sceneSizeY - 2 && (action.dstId < 0 || ObjectType.isTypeCached(Resource.mapRegion!!.getLoctypeList(), action.dstId, action.dstShape))) {
                if (customize) {
                    addCustomisation(action.plane, action.gameObjectType, action.x, action.y, action.dstCustomization)
                } else {
                    transformObject(action.plane, action.gameObjectType, action.x, action.y, action.dstId, action.dstRotation, action.dstShape, -1)
                }
                action.queued = false
                if (!customize && action.srcId == action.dstId && action.srcId == -1) {
                    action.unlink()
                } else if (!customize && action.srcId == action.dstId && action.srcRotation == action.dstRotation && action.dstShape == action.srcShape) {
                    action.unlink()
                }
            }
        }

        fun transformObject(plane: Int, type: Int, x: Int, y: Int, id: Int, rotation: Int, shape: Int, sequenceId: Int) {
            if (x >= 1 && y >= 1 && x <= Resource.mapRegion!!.sceneSizeX - 2 && y <= Resource.mapRegion!!.sceneSizeY - 2) {
                var collisionPlane = plane
                if (plane < 3 && Resource.mapRegion!!.settingsBits.areRoofsHidden(x, y)) {
                    collisionPlane = plane + 1
                }
                if (Resource.mapRegion!!.getScene()!! != null) {
                    Resource.mapRegion!!.surfaceSceneGraph!!.removeGameObject(Renderers.activeToolkit!!, plane, type, x, y, Resource.mapRegion!!.getCollisionMap(plane)!!)
                    if (id >= 0) {
                        val groundDecorationPreference: Int = PreferenceManager.preferences!!.groundDecoration!!.getValue()
                        PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.groundDecoration!!, 1)
                        Resource.mapRegion!!.surfaceSceneGraph!!.addObject(Renderers.activeToolkit!!, collisionPlane, plane, x, y, id, rotation, shape, Resource.mapRegion!!.getCollisionMap(plane), sequenceId)
                        PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.groundDecoration!!, groundDecorationPreference)
                    }
                }
            }
        }

        // 0x4 = renderOnLowerZ (render object from next plane on plane-1), 0x2 = hide roofs
        private fun getCullingType(bitpacked: Int): Int = bitpacked and 0x3

        // Something with roofs? - TODO: fix the horrible naming in this
        fun calculateVisibilityAdjustments(regionTiles: Array<Array<Array<Tile>?>?>?, anInt1234: Int, baseX: Int, baseY: Int, hideRoofs: Boolean): Boolean {
            val settingBits = Resource.mapRegion!!.bits
            val setting = if (hideRoofs) 1 else (Client.timesSceneRendered and 0xff).toByte()
            if (settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![baseX]!![baseY] == setting) {
                return false
            }
            val regionMap = Resource.mapRegion!!.settingsBits
            if ((regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][baseX][baseY].toInt() and 0x4) == 0) {
                return false
            }
            var currentPosition = 0
            var position = 1
            Client.bigX[0] = baseX
            Client.bigY[0] = baseY
            settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![baseX]!![baseY] = setting
            while (position != currentPosition) {
                val tileBaseX: Int = Client.bigX[currentPosition] and 0xffff
                val bitpackedTypeRotA: Int = Client.bigX[currentPosition] shr 16 and 0xff
                val sceneXHashB: Int = Client.bigX[currentPosition] shr 24 and 0xff
                var tileBaseY: Int = Client.bigY[currentPosition] and 0xffff
                val bitpackedTypeRotB: Int = Client.bigY[currentPosition] shr 16 and 0xff
                currentPosition = currentPosition + 1 and 0xfff
                val renderOnLowerZ = (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY].toInt() and 0x4) == 0
                var visibilityUpdate = false
                if (regionTiles != null) {
                    label270@ for (planeAbove in PlayerUpdate.MY_PLAYER_PLANE + 1..3) {
                        if (regionTiles[planeAbove] != null && (regionMap.tileFunctionMasks[planeAbove][tileBaseX][tileBaseY].toInt() and 0x8) == 0) {
                            if (renderOnLowerZ && regionTiles[planeAbove]!![tileBaseX]!![tileBaseY] != null) {
                                if (regionTiles[planeAbove]!![tileBaseX]!![tileBaseY].primaryWall != null) {
                                    val cullingType = getCullingType(bitpackedTypeRotA)
                                    if (regionTiles[planeAbove]!![tileBaseX]!![tileBaseY].primaryWall!!.cullingType.toInt() == cullingType || regionTiles[planeAbove]!![tileBaseX]!![tileBaseY].secondaryWall != null && regionTiles[planeAbove]!![tileBaseX]!![tileBaseY].secondaryWall!!.cullingType.toInt() == cullingType) {
                                        continue
                                    }
                                    if (sceneXHashB != 0) {
                                        val i_20 = getCullingType(sceneXHashB)
                                        if (regionTiles[planeAbove]!![tileBaseX]!![tileBaseY].primaryWall!!.cullingType.toInt() == i_20 || regionTiles[planeAbove]!![tileBaseX]!![tileBaseY].secondaryWall != null && regionTiles[planeAbove]!![tileBaseX]!![tileBaseY].secondaryWall!!.cullingType.toInt() == i_20) {
                                            continue
                                        }
                                    }
                                    if (bitpackedTypeRotB != 0) {
                                        val i_20 = getCullingType(bitpackedTypeRotB)
                                        if (regionTiles[planeAbove]!![tileBaseX]!![tileBaseY].primaryWall!!.cullingType.toInt() == i_20 || regionTiles[planeAbove]!![tileBaseX]!![tileBaseY].secondaryWall != null && regionTiles[planeAbove]!![tileBaseX]!![tileBaseY].secondaryWall!!.cullingType.toInt() == i_20) {
                                            continue
                                        }
                                    }
                                }
                                val tile = regionTiles[planeAbove]!![tileBaseX]!![tileBaseY]
                                if (tile.interactives != null) {
                                    var interactives: InteractiveEntityContainer? = tile.interactives
                                    while (interactives != null) {
                                        val abstractInteractive: AbstractInteractiveEntity? = interactives.interactiveObject
                                        if (abstractInteractive is GameObject) {
                                            var shape: Int = abstractInteractive.shape
                                            val rotation: Int = abstractInteractive.getRotation()
                                            if (shape == 21) {
                                                shape = 19
                                            }
                                            val bitpackedPlacement = shape or (rotation shl 6)
                                            if (bitpackedTypeRotA == bitpackedPlacement || sceneXHashB != 0 && sceneXHashB == bitpackedPlacement || bitpackedTypeRotB != 0 && bitpackedTypeRotB == bitpackedPlacement) {
                                                continue@label270
                                            }
                                        }
                                        interactives = interactives.child
                                    }
                                }
                            }
                            val tile: Tile? = regionTiles[planeAbove]!![tileBaseX]!![tileBaseY]
                            if (tile != null && tile.interactives != null) {
                                var interactives: InteractiveEntityContainer? = tile.interactives
                                while (interactives != null) {
                                    val abstractInteractive: AbstractInteractiveEntity? = interactives.interactiveObject
                                    if (abstractInteractive!!.endX != abstractInteractive!!.startX || abstractInteractive!!.endZ != abstractInteractive!!.startZ) {
                                        for (x in abstractInteractive!!.startX..abstractInteractive!!.endX) {
                                            for (y in abstractInteractive!!.startZ..abstractInteractive!!.endZ) {
                                                settingBits[planeAbove]!![x]!![y] = setting
                                            }
                                        }
                                    }
                                    interactives = interactives.child
                                }
                            }
                            settingBits!!!![planeAbove]!![tileBaseX]!![tileBaseY] = setting
                            visibilityUpdate = true
                        }
                    }
                }
                if (visibilityUpdate) {
                    val floorHeight = Resource.mapRegion!!.getScene()!!!!.localHeightmap[PlayerUpdate.MY_PLAYER_PLANE + 1]!!.getHeight(tileBaseX, tileBaseY)
                    if (Client.visibleBelowDrawheight!![anInt1234] < floorHeight) {
                        Client.visibleBelowDrawheight!![anInt1234] = floorHeight
                    }
                    val upscaledX = tileBaseX shl 9
                    val upscaledY = tileBaseY shl 9
                    if (Client.visibleAboveX!![anInt1234] > upscaledX) {
                        Client.visibleAboveX!![anInt1234] = upscaledX
                    } else if (Client.visibleBelowX!![anInt1234] < upscaledX) {
                        Client.visibleBelowX!![anInt1234] = upscaledX
                    }
                    if (Client.visibleAboveY!![anInt1234] > upscaledY) {
                        Client.visibleAboveY!![anInt1234] = upscaledY
                    } else if (Client.visibleBelowY!![anInt1234] < upscaledY) {
                        Client.visibleBelowY!![anInt1234] = upscaledY
                    }
                }
                if (!renderOnLowerZ) {
                    if (tileBaseX >= 1 && setting != settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX - 1]!![tileBaseY]) {
                        Client.bigX[position] = tileBaseX - 1 or 0x120000 or -754974720
                        Client.bigY[position] = tileBaseY or 0x130000
                        position = position + 1 and 0xfff
                        settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX - 1]!![tileBaseY] = setting
                    }
                    if (++tileBaseY < Resource.mapRegion!!.sceneSizeY) {
                        if (tileBaseX - 1 >= 0 && setting != settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX - 1]!![tileBaseY] && (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY].toInt() and 0x4) == 0 && (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX - 1][tileBaseY - 1].toInt() and 0x4) == 0) {
                            Client.bigX[position] = tileBaseX - 1 or 0x120000 or 0x52000000
                            Client.bigY[position] = tileBaseY or 0x130000
                            position = position + 1 and 0xfff
                            settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX - 1]!![tileBaseY] = setting
                        }
                        if (setting != settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX]!![tileBaseY]) {
                            Client.bigX[position] = tileBaseX or 0x520000 or 0x13000000
                            Client.bigY[position] = tileBaseY or 0x530000
                            position = position + 1 and 0xfff
                            settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX]!![tileBaseY] = setting
                        }
                        if (tileBaseX + 1 < Resource.mapRegion!!.sceneSizeX && settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX + 1]!![tileBaseY] != setting && (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY].toInt() and 0x4) == 0 && (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX + 1][tileBaseY - 1].toInt() and 0x4) == 0) {
                            Client.bigX[position] = tileBaseX + 1 or 0x520000 or -1845493760
                            Client.bigY[position] = tileBaseY or 0x530000
                            position = position + 1 and 0xfff
                            settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX + 1]!![tileBaseY] = setting
                        }
                    }
                    tileBaseY--
                    if (tileBaseX + 1 < Resource.mapRegion!!.sceneSizeX && setting != settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX + 1]!![tileBaseY]) {
                        Client.bigX[position] = tileBaseX + 1 or 0x920000 or 0x53000000
                        Client.bigY[position] = tileBaseY or 0x930000
                        position = position + 1 and 0xfff
                        settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX + 1]!![tileBaseY] = setting
                    }
                    if (--tileBaseY >= 0) {
                        if (tileBaseX - 1 >= 0 && setting != settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX - 1]!![tileBaseY] && (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY].toInt() and 0x4) == 0 && (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX - 1][tileBaseY + 1].toInt() and 0x4) == 0) {
                            Client.bigX[position] = tileBaseX - 1 or 0xd20000 or 0x12000000
                            Client.bigY[position] = tileBaseY or 0xd30000
                            position = position + 1 and 0xfff
                            settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX - 1]!![tileBaseY] = setting
                        }
                        if (setting != settingBits!!!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX]!![tileBaseY]) {
                            Client.bigX[position] = tileBaseX or 0xd20000 or -1828716544
                            Client.bigY[position] = tileBaseY or 0xd30000
                            position = position + 1 and 0xfff
                            settingBits!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX]!![tileBaseY] = setting
                        }
                        if (tileBaseX + 1 < Resource.mapRegion!!.sceneSizeX && settingBits!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX + 1]!![tileBaseY] != setting && (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY].toInt() and 0x4) == 0 && (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX + 1][tileBaseY + 1].toInt() and 0x4) == 0) {
                            Client.bigX[position] = tileBaseX + 1 or 0x920000 or -771751936
                            Client.bigY[position] = tileBaseY or 0x930000
                            position = position + 1 and 0xfff
                            settingBits!![PlayerUpdate.MY_PLAYER_PLANE]!![tileBaseX + 1]!![tileBaseY] = setting
                        }
                    }
                }
            }
            if (Client.visibleBelowDrawheight!![anInt1234] != -1000000) {
                Client.visibleBelowDrawheight!![anInt1234] += 40
                Client.visibleAboveX!![anInt1234] -= 512
                Client.visibleBelowX!![anInt1234] += 512
                Client.visibleBelowY!![anInt1234] += 512
                Client.visibleAboveY!![anInt1234] -= 512
            }
            return true
        }
    }
}
