package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.entity.hint.HintTrailGraphNode
import com.jagex.game.runetek5.entity.gameobject.StationaryInteractiveObject
import com.jagex.game.runetek5.entity.*
import com.jagex.game.runetek5.entity.gameobject.AbstractGroundDecoration
import com.jagex.game.runetek5.entity.gameobject.AbstractWall
import com.jagex.game.runetek5.entity.gameobject.AbstractWallDecoration
import com.jagex.game.runetek5.entity.grounditem.AbstractGroundItem
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.light.FlickeringEffect
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.math.Vector3Pool
import com.jagex.game.runetek5.scene.culling.Culling
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.scene.tile.Tile
import com.jagex.game.runetek5.scene.tile.WaterTile
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.NanoTimer

class Scene(
    renderer: AbstractToolkit,
    sceneWidth: Int,
    sceneLength: Int,
    visibleTiles: Int,
    hasUnderwaterMap: Boolean,
    hasLights: Boolean
) {
@JvmField var tiles: Array<Array<Array<Tile?>>>
@JvmField var localHeightmap: Array<Ground?>
    var regionSizeZ: Int
@JvmField val tileUnits: Int
@JvmField val sceneDimensionX: Int
@JvmField val sceneDimensionY: Int
    val culling: Culling
    val surfaceTiles: Array<Array<Array<Tile?>>>
@JvmField val surfaceHeightmap: Array<Ground?>
    var waterTiles: Array<Array<Array<Tile?>>>?
@JvmField var underwaterHeightmap: Array<Ground?>?
@JvmField val interactiveEntities: InteractiveEntityCache
    var interactiveObjectCount: Int = 0
    var lightSlot: Int = 0
    var depthObjectCount: Int = 0
    var layeredObjectCount: Int = 0
    var tileDrawCycle: Int = 0
    var calculateTilesOnScreen: Boolean = false
    var cameraGridPositionX: Int = 0
    var cameraGridPositionY: Int = 0
    var cameraPositionX: Int = 0
    var cameraPositionZ: Int = 0
    var cameraPositionY: Int = 0
    var visibleToXnegative: Int = 0
    var visibleToInvertedXnegative: Int = 0
    var visibleToYnegative: Int = 0
    var visibleToInvertedYnegative: Int = 0
    var visibleToXpositive: Int = 0
    var visibleToYpositive: Int = 0
    var allTilesVisibleFallback: Array<BooleanArray>
    var tileOnScreen: Array<BooleanArray>
    var tileInFrustum: Array<BooleanArray>
    var priorityRenderedEntityCapacity: Int = 5015
    var interactiveObjectCapacity: Int = 5040
    var anInt2622: Int = 16
    var depthRenderedEntityCapacity: Int = 10072
    var anInt2643: Int = 1
    val renderedGraphNodePosition: FloatArray
    val activeLights: Array<PointLight?>
    val activeToolkit: AbstractToolkit
    val copyLights: Boolean
    val fullTileDimension: Int
    val halfTileOffset: Int
    val visibleTiles: Int
    val nanoTimer: NanoTimer
    var colors: Array<IntArray>?
    var intensities: Array<ByteArray>?
    var scales: Array<ShortArray>?
    var opcode20: Array<ByteArray>?
    var opcode21: Array<ByteArray>?
    var opcode22: Array<ByteArray>?
    var tileLightUids: Array<Array<LongArray>>?
    var lightSlotOccupant: Array<FlickeringEffect?>?
    var lightSlotAvailable: BooleanArray?
    val staticTextureOpaqueEntities: Array<SceneGraphEntity?>
    val alphaEntities: Array<SceneGraphEntity?>
    val dynamicRenderObjects: Array<SceneGraphEntity?>

    /**
     * Objects of which face_render_priorities
     * can be disregarded
     * (Could be wrong, could be: opaque_graph_node)
     * graph_nodes_3d before
     */
    var depthRenderedEntities: Array<SceneGraphEntity?>

    /**
     * Objects that need to have their face_render_priority
     * taken into account for correct rendering
     * (could be wrong, could be: transparent_graph_node)
     */
    var priorityRenderedEntities: Array<SceneGraphEntity?>
    var interactiveObjects: Array<AbstractInteractiveEntity?>

    /**
     * 0 = onscreen
     * 1 = offscreen
     */
    val tileVisibility: IntArray

    init {
        this.renderedGraphNodePosition = FloatArray(3)
        this.activeLights = arrayOfNulls(8)
        this.activeToolkit = renderer
        this.copyLights = this.activeToolkit.getMaxLights() > 0
        this.tileUnits = 9
        this.fullTileDimension = 1 shl this.tileUnits
        this.halfTileOffset = this.fullTileDimension shr 1
        this.regionSizeZ = 4
        this.sceneDimensionX = sceneWidth
        this.sceneDimensionY = sceneLength
        this.visibleTiles = visibleTiles
        this.nanoTimer = NanoTimer()
        this.culling = Culling(this)
        this.surfaceTiles = Array(4) { Array(sceneWidth) { arrayOfNulls<Tile>(sceneLength) } }
        this.surfaceHeightmap = arrayOfNulls(4)
        if (hasUnderwaterMap) {
            this.colors = Array(sceneWidth) { IntArray(sceneLength) }
            this.intensities = Array(sceneWidth) { ByteArray(sceneLength) }
            this.scales = Array(sceneWidth) { ShortArray(sceneLength) }
            this.opcode20 = Array(sceneWidth) { ByteArray(sceneLength) }
            this.opcode21 = Array(sceneWidth) { ByteArray(sceneLength) }
            this.opcode22 = Array(sceneWidth) { ByteArray(sceneLength) }
            this.waterTiles = Array(1) { Array(sceneWidth) { arrayOfNulls<Tile>(sceneLength) } }
            this.underwaterHeightmap = arrayOfNulls(1)
        } else {
            this.colors = null
            this.intensities = null
            this.scales = null
            this.opcode20 = null
            this.opcode21 = null
            this.opcode22 = null
            this.waterTiles = null
            this.underwaterHeightmap = null
        }
        if (hasLights) {
            this.tileLightUids = Array(4) { Array(sceneWidth) { LongArray(sceneLength) } }
            this.lightSlotOccupant = arrayOfNulls(65361)
            this.lightSlotAvailable = BooleanArray(65361)
            this.lightSlot = 0
        } else {
            this.tileLightUids = null
            this.lightSlotOccupant = null
            this.lightSlotAvailable = null
        }
        this.setUnderwater(false)
        this.staticTextureOpaqueEntities = arrayOfNulls(2)
        this.alphaEntities = arrayOfNulls(2)
        this.dynamicRenderObjects = arrayOfNulls(2)
        this.depthRenderedEntities = arrayOfNulls(10072)
        this.depthObjectCount = 0
        this.priorityRenderedEntities = arrayOfNulls(5015)
        this.layeredObjectCount = 0
        this.interactiveObjects = arrayOfNulls(5040)
        this.interactiveObjectCount = 0
        this.tileInFrustum = Array(visibleTiles + visibleTiles + 1) { BooleanArray(visibleTiles + visibleTiles + 1) }
        this.tileOnScreen = Array(visibleTiles + visibleTiles + 2) { BooleanArray(visibleTiles + visibleTiles + 2) }
        this.allTilesVisibleFallback = this.tileInFrustum
        this.tileVisibility = IntArray(visibleTiles + visibleTiles + 2)
        this.interactiveEntities = InteractiveEntityCache()
        this.tiles = this.surfaceTiles
        this.localHeightmap = this.surfaceHeightmap
    }

    fun empty1() {
    }

    fun empty2() {
    }

    fun setUnderwater(underwater: Boolean) {
        if (underwater) {
            this.tiles = this.waterTiles!!
            this.localHeightmap = this.underwaterHeightmap!!
        } else {
            this.tiles = this.surfaceTiles
            this.localHeightmap = this.surfaceHeightmap
        }
        this.regionSizeZ = this.tiles.size
    }

    fun getWall(plane: Int, x: Int, y: Int): AbstractWall? {
        val tile = this.tiles[plane][x][y]
        return tile?.primaryWall
    }

    fun linkBelow(x: Int, y: Int) {
        val bridgeTile = this.tiles[0][x][y]
        for (plane in 0..2) {
            val tileAbove = this.tiles[plane + 1][x][y]
            this.tiles[plane][x][y] = tileAbove
            if (tileAbove != null) {
                var container = tileAbove.interactives
                while (container != null) {
                    val interactive = container.interactiveObject
                    if (interactive!!.startX.toInt() == x && y == interactive!!.startZ.toInt()) {
                        interactive.plane = (interactive.plane - 1).toByte()
                    }
                    container = container.child
                }
                if (tileAbove.groundDecoration != null) {
                    tileAbove.groundDecoration!!.plane = (tileAbove.groundDecoration!!.plane - 1).toByte()
                }
                if (tileAbove.primaryWall != null) {
                    tileAbove.primaryWall!!.plane = (tileAbove.primaryWall!!.plane - 1).toByte()
                }
                if (tileAbove.secondaryWall != null) {
                    tileAbove.secondaryWall!!.plane = (tileAbove.secondaryWall!!.plane - 1).toByte()
                }
                if (tileAbove.primaryWallDecoration != null) {
                    tileAbove.primaryWallDecoration!!.plane = (tileAbove.primaryWallDecoration!!.plane - 1).toByte()
                }
                if (tileAbove.secondaryWallDecoration != null) {
                    tileAbove.secondaryWallDecoration!!.plane = (tileAbove.secondaryWallDecoration!!.plane - 1).toByte()
                }
            }
        }
        if (this.tiles[0][x][y] == null) {
            this.tiles[0][x][y] = Tile(0)
            this.tiles[0][x][y]!!.plane = 1
        }
        this.tiles[0][x][y]!!.bridge = bridgeTile
        this.tiles[3][x][y] = null
    }

    fun loadTiles(plane: Int, x: Int, y: Int) {
        val hasBridge = this.tiles[0][x][y] != null && this.tiles[0][x][y]!!.bridge != null
        var currentPlane = plane
        while (currentPlane >= 0) {
            if (this.tiles[currentPlane][x][y] == null) {
                val tile = Tile(currentPlane)
                this.tiles[currentPlane][x][y] = tile
                if (hasBridge) {
                    tile.plane++
                }
            }
            --currentPlane
        }
    }

    fun setLocalGround(plane: Int, ground: Ground?) {
        this.localHeightmap[plane] = ground
    }

    fun getScale(x: Int, y: Int): Int {
        return if (this.scales != null) this.scales!![x][y].toInt() and 0xffff else 0
    }

    fun getColor(x: Int, y: Int): Int {
        return if (this.colors != null) this.colors!![x][y] and 0xffffff else 0
    }

    fun unusedOpcode22(x: Int, y: Int): Int {
        return if (this.opcode22 != null) this.opcode22!![x][y].toInt() and 0xff else 0
    }

    fun addWater(x: Int, y: Int, waterColor: Int, waterScale: Int, waterIntensity: Int, waterOpcode20: Int, waterOpcode21: Int, waterOpcode22: Int) {
        if (this.colors != null) {
            this.colors!![x][y] = 0xffffff.inv() or waterColor
        }
        if (this.scales != null) {
            this.scales!![x][y] = waterScale.toShort()
        }
        if (this.intensities != null) {
            this.intensities!![x][y] = waterIntensity.toByte()
        }
        if (this.opcode20 != null) {
            this.opcode20!![x][y] = waterOpcode20.toByte()
        }
        if (this.opcode21 != null) {
            this.opcode21!![x][y] = waterOpcode21.toByte()
        }
        if (this.opcode22 != null) {
            this.opcode22!![x][y] = waterOpcode22.toByte()
        }
    }

    fun addGroundDecoration(plane: Int, baseX: Int, baseY: Int, groundDecoration: AbstractGroundDecoration?) {
        retainedChunkChanged(plane, baseX, baseY)
        val tile = getTile(plane, baseX, baseY)
        if (tile != null) {
            tile.groundDecoration = groundDecoration
            val layerIndex = if (this.localHeightmap === this.underwaterHeightmap) 1 else 0
            if (groundDecoration!!.disregardTextureMovement()) {
                if (groundDecoration.hasAlpha()) {
                    groundDecoration.child = this.alphaEntities[layerIndex]
                    this.alphaEntities[layerIndex] = groundDecoration
                } else {
                    groundDecoration.child = this.staticTextureOpaqueEntities[layerIndex]
                    this.staticTextureOpaqueEntities[layerIndex] = groundDecoration
                }
            } else {
                groundDecoration.child = this.dynamicRenderObjects[layerIndex]
                this.dynamicRenderObjects[layerIndex] = groundDecoration
            }
        }
    }

    fun addGroundItem(plane: Int, baseX: Int, baseY: Int, drawHeight: Int, groundItem: AbstractGroundItem?) {
        val tile = getTile(plane, baseX, baseY)
        if (tile != null) {
            groundItem!!.setBasePosition(Vector3(((baseX shl this.tileUnits) + this.halfTileOffset).toFloat(), drawHeight.toFloat(), ((baseY shl this.tileUnits) + this.halfTileOffset).toFloat()))
            tile.groundItem = groundItem
            val layerIndex = if (this.underwaterHeightmap === this.localHeightmap) 1 else 0
            if (groundItem.disregardTextureMovement()) {
                if (groundItem.hasAlpha()) {
                    groundItem.child = this.alphaEntities[layerIndex]
                    this.alphaEntities[layerIndex] = groundItem
                } else {
                    groundItem.child = this.staticTextureOpaqueEntities[layerIndex]
                    this.staticTextureOpaqueEntities[layerIndex] = groundItem
                }
            } else {
                groundItem.child = this.dynamicRenderObjects[layerIndex]
                this.dynamicRenderObjects[layerIndex] = groundItem
            }
        }
    }


    fun addWall(plane: Int, baseX: Int, baseY: Int, primaryWall: AbstractWall?, secondaryWall: AbstractWall?) {
        retainedChunkChanged(plane, baseX, baseY)
        val tile = getTile(plane, baseX, baseY)
        if (tile != null) {
            tile.primaryWall = primaryWall
            tile.secondaryWall = secondaryWall
            val layerIndex = if (this.underwaterHeightmap === this.localHeightmap) 1 else 0
            if (primaryWall!!.disregardTextureMovement()) {
                if (primaryWall.hasAlpha()) {
                    primaryWall.child = this.alphaEntities[layerIndex]
                    this.alphaEntities[layerIndex] = primaryWall
                } else {
                    primaryWall.child = this.staticTextureOpaqueEntities[layerIndex]
                    this.staticTextureOpaqueEntities[layerIndex] = primaryWall
                }
            } else {
                primaryWall.child = this.dynamicRenderObjects[layerIndex]
                this.dynamicRenderObjects[layerIndex] = primaryWall
            }
            if (secondaryWall != null) {
                if (secondaryWall.disregardTextureMovement()) {
                    if (secondaryWall.hasAlpha()) {
                        secondaryWall.child = this.alphaEntities[layerIndex]
                        this.alphaEntities[layerIndex] = secondaryWall
                    } else {
                        secondaryWall.child = this.staticTextureOpaqueEntities[layerIndex]
                        this.staticTextureOpaqueEntities[layerIndex] = secondaryWall
                    }
                } else {
                    secondaryWall.child = this.dynamicRenderObjects[layerIndex]
                    this.dynamicRenderObjects[layerIndex] = secondaryWall
                }
            }
        }
    }

    fun addWallDecoration(plane: Int, baseX: Int, baseY: Int, primaryWallDecoration: AbstractWallDecoration?, secondaryWallDecoration: AbstractWallDecoration?) {
        retainedChunkChanged(plane, baseX, baseY)
        val tile = getTile(plane, baseX, baseY)
        if (tile != null) {
            tile.primaryWallDecoration = primaryWallDecoration
            tile.secondaryWallDecoration = secondaryWallDecoration
            val layerIndex = if (this.underwaterHeightmap === this.localHeightmap) 1 else 0
            if (primaryWallDecoration!!.disregardTextureMovement()) {
                if (primaryWallDecoration.hasAlpha()) {
                    primaryWallDecoration.child = this.alphaEntities[layerIndex]
                    this.alphaEntities[layerIndex] = primaryWallDecoration
                } else {
                    primaryWallDecoration.child = this.staticTextureOpaqueEntities[layerIndex]
                    this.staticTextureOpaqueEntities[layerIndex] = primaryWallDecoration
                }
            } else {
                primaryWallDecoration.child = this.dynamicRenderObjects[layerIndex]
                this.dynamicRenderObjects[layerIndex] = primaryWallDecoration
            }
            if (secondaryWallDecoration != null) {
                if (secondaryWallDecoration.disregardTextureMovement()) {
                    if (secondaryWallDecoration.hasAlpha()) {
                        secondaryWallDecoration.child = this.alphaEntities[layerIndex]
                        this.alphaEntities[layerIndex] = secondaryWallDecoration
                    } else {
                        secondaryWallDecoration.child = this.staticTextureOpaqueEntities[layerIndex]
                        this.staticTextureOpaqueEntities[layerIndex] = secondaryWallDecoration
                    }
                } else {
                    secondaryWallDecoration.child = this.dynamicRenderObjects[layerIndex]
                    this.dynamicRenderObjects[layerIndex] = secondaryWallDecoration
                }
            }
        }
    }

    private fun notifyInteractiveChanged(interactive: AbstractInteractiveEntity) {
        // Only stationary locs are ever retained. Players, NPCs and animated locs are
        // re-added every frame (clearInteractives), and reporting those would
        // invalidate the chunk under every moving entity each frame.
        if (interactive is HintTrailGraphNode) {
            walkRevision++
            return
        }
        if (interactive !is StationaryInteractiveObject) return
        walkRevision++
        val hook = retainedHook ?: return
        val plane = interactive.plane.toInt()
        for (x in interactive.startX..interactive.endX) {
            for (y in interactive.startZ..interactive.endZ) {
                hook.chunkChanged(this, plane, x.toInt(), y.toInt())
            }
        }
    }

    private fun retainedChunkChanged(plane: Int, x: Int, y: Int) {
        walkRevision++
        retainedHook?.chunkChanged(this, plane, x, y)
    }

    private fun retainedSceneReset() {
        walkRevision++
        retainedHook?.sceneReset(this)
    }

    /**
     * Bumped on every change to the static walk lists (adds, removes, resets,
     * interactive churn) and by the retained renderer whenever it flips an entity's
     * [SceneGraphEntity.modernRetained]. The retained walk cache below is keyed on it.
     */
    @JvmField var walkRevision: Int = 0
    private var walkCacheRevision = -1
    private var walkCacheLayer = -1
    private var walkCacheOpaque: Array<SceneGraphEntity?> = arrayOfNulls(1024)
    private var walkCacheOpaqueCount = 0
    private var walkCacheAlpha: Array<SceneGraphEntity?> = arrayOfNulls(256)
    private var walkCacheAlphaCount = 0

    /**
     * Under the retained path most static entities are skipped by the walk, but the
     * walk still had to chase the whole singly-linked static lists (~10k nodes) to
     * find the few it must draw. Cache the non-retained subset as flat arrays and
     * rebuild only when the lists or the retained flags change.
     */
    private fun rebuildWalkCache(layerIndex: Int) {
        walkCacheRebuildsAcc++
        walkCacheRevision = walkRevision
        walkCacheLayer = layerIndex
        var n = 0
        var entity = this.staticTextureOpaqueEntities[layerIndex]
        while (entity != null) {
            if (!entity.modernRetained) {
                if (n == walkCacheOpaque.size) walkCacheOpaque = walkCacheOpaque.copyOf(n * 2)
                walkCacheOpaque[n++] = entity
            }
            entity = entity.child
        }
        if (n < walkCacheOpaqueCount) java.util.Arrays.fill(walkCacheOpaque, n, walkCacheOpaqueCount.coerceAtMost(walkCacheOpaque.size), null)
        walkCacheOpaqueCount = n
        n = 0
        entity = this.alphaEntities[layerIndex]
        while (entity != null) {
            if (!entity.modernRetained) {
                if (n == walkCacheAlpha.size) walkCacheAlpha = walkCacheAlpha.copyOf(n * 2)
                walkCacheAlpha[n++] = entity
            }
            entity = entity.child
        }
        if (n < walkCacheAlphaCount) java.util.Arrays.fill(walkCacheAlpha, n, walkCacheAlphaCount.coerceAtMost(walkCacheAlpha.size), null)
        walkCacheAlphaCount = n
    }

    fun addInteractive(interactive: AbstractInteractiveEntity, cached: Boolean): Boolean {
        val isUnderwater = this.localHeightmap === this.underwaterHeightmap
        var color = 0
        var scale: Short = 0
        var intensity: Byte = 0
        interactive.calculatePosition()
        notifyInteractiveChanged(interactive)
        if (interactive.startX >= 0 && interactive.startZ >= 0 && interactive.endX < this.sceneDimensionX && interactive.endZ < this.sceneDimensionY) {
            var floorOffsetY: Short = 0
            for (x in interactive.startX..interactive.endX) {
                for (y in interactive.startZ..interactive.endZ) {
                    val tile = getTile(interactive.plane.toInt(), x, y)
                    if (tile != null) {
                        val container = InteractiveEntityContainer.add(interactive)
                        val head = tile.interactives
                        if (head == null) {
                            tile.interactives = container
                        } else {
                            var current = head
                            while (current!!.child != null) {
                                current = current!!.child!!!!
                            }
                            current.child = container
                        }
                        if (isUnderwater && (this.colors!![x][y] and 0xffffff.inv()) != 0) {
                            color = this.colors!![x][y]
                            scale = this.scales!![x][y]
                            intensity = this.intensities!![x][y]
                        }
                        if (!cached && tile.groundDecoration != null && tile.groundDecoration!!.floorOffsetY > floorOffsetY) {
                            floorOffsetY = tile.groundDecoration!!.floorOffsetY
                        }
                    }
                }
            }
            if (isUnderwater && (color and 0xffffff.inv()) != 0) {
                for (x in interactive.startX..interactive.endX) {
                    for (y in interactive.startZ..interactive.endZ) {
                        if ((this.colors!![x][y] and 0xffffff.inv()) == 0) {
                            this.colors!![x][y] = color
                            this.scales!![x][y] = scale
                            this.intensities!![x][y] = intensity
                        }
                    }
                }
            }
            if (cached) {
                addInteractiveObject(interactive)
                interactive.scene = this
            } else {
                val layerIndex = if (this.underwaterHeightmap === this.localHeightmap) 1 else 0
                if (interactive.disregardTextureMovement()) {
                    if (interactive.hasAlpha()) {
                        interactive.child = this.alphaEntities[layerIndex]
                        this.alphaEntities[layerIndex] = interactive
                    } else {
                        interactive.child = this.staticTextureOpaqueEntities[layerIndex]
                        this.staticTextureOpaqueEntities[layerIndex] = interactive
                    }
                } else {
                    interactive.child = this.dynamicRenderObjects[layerIndex]
                    this.dynamicRenderObjects[layerIndex] = interactive
                }
            }
            if (cached) {
                val absolutePosition = Vector3Pool.vector3From(interactive.getAbsolutePosition().coords)
                absolutePosition.y -= floorOffsetY.toFloat()
                interactive.setBasePosition(absolutePosition)
                absolutePosition.pushVectorStack()
            }
            return true
        }
        return false
    }

    fun offsetWallDecoration(plane: Int, x: Int, y: Int, displacement: Int) {
        retainedChunkChanged(plane, x, y)
        val tile = this.tiles[plane][x][y]
        if (tile != null) {
            val primaryWallDecoration = tile.primaryWallDecoration
            val secondaryWallDecoration = tile.secondaryWallDecoration
            /** The upscaling happening below equals to 9 - 7 = 2, to match the model upscaling **/
            if (primaryWallDecoration != null) {
                primaryWallDecoration.positionX = (displacement * primaryWallDecoration.positionX / (16 shl this.tileUnits - 7)).toShort()
                primaryWallDecoration.positionY = (displacement * primaryWallDecoration.positionY / (16 shl this.tileUnits - 7)).toShort()
            }
            if (secondaryWallDecoration != null) {
                secondaryWallDecoration.positionX = (displacement * secondaryWallDecoration.positionX / (16 shl this.tileUnits - 7)).toShort()
                secondaryWallDecoration.positionY = (displacement * secondaryWallDecoration.positionY / (16 shl this.tileUnits - 7)).toShort()
            }
        }
    }

    fun clearInteractives() {
        for (index in 0 until this.interactiveObjectCount) {
            val interactive = this.interactiveObjects[index]
            removeInteractive(interactive!!, true)
            this.interactiveObjects[index] = null
        }
        this.interactiveObjectCount = 0
    }

    fun applyShading() {
        retainedSceneReset()
        for (plane in 0 until this.regionSizeZ) {
            for (x in 0 until this.sceneDimensionX) {
                for (y in 0 until this.sceneDimensionY) {
                    val tile = this.tiles[plane][x][y]
                    if (tile != null) {
                        val primaryWall = tile.primaryWall
                        val secondaryWall = tile.secondaryWall
                        if (primaryWall != null && primaryWall.hasTileShadow()) {
                            castShadow(primaryWall, plane, x, y, 1, 1)
                            if (secondaryWall != null && secondaryWall.hasTileShadow()) {
                                castShadow(secondaryWall, plane, x, y, 1, 1)
                                secondaryWall.castShadow(primaryWall, 0, 0, 0, false)
                                secondaryWall.removeTileShadow()
                            }
                            primaryWall.removeTileShadow()
                        }
                        var container = tile.interactives
                        while (container != null) {
                            val interactive = container.interactiveObject
                            if (interactive != null && interactive.hasTileShadow()) {
                                castShadow(interactive, plane, x, y, interactive.endX - interactive.startX + 1, interactive.endZ - interactive.startZ + 1)
                                interactive.removeTileShadow()
                            }
                            container = container.child
                        }
                        val groundDecoration = tile.groundDecoration
                        if (groundDecoration != null && groundDecoration.hasTileShadow()) {
                            castShadow(groundDecoration, plane, x, y)
                            groundDecoration.removeTileShadow()
                        }
                    }
                }
            }
        }
    }

    fun removePrimaryWallDecoration(plane: Int, x: Int, y: Int): AbstractWallDecoration? {
        retainedChunkChanged(plane, x, y)
        val tile = this.tiles[plane][x][y]
        if (tile != null) {
            pushIntoStack(tile.primaryWallDecoration)
            if (tile.primaryWallDecoration != null) {
                val primaryWallDecoration = tile.primaryWallDecoration
                tile.primaryWallDecoration = null
                return primaryWallDecoration
            }
        }
        return null
    }

    fun removeGroundDecoration(plane: Int, x: Int, y: Int): AbstractGroundDecoration? {
        retainedChunkChanged(plane, x, y)
        val tile = this.tiles[plane][x][y]
        if (tile == null) {
            return null
        }
        pushIntoStack(tile.groundDecoration)
        if (tile.groundDecoration != null) {
            val groundDecoration = tile.groundDecoration
            tile.groundDecoration = null
            return groundDecoration
        }
        return null
    }

    fun removeGroundItem(plane: Int, x: Int, y: Int): AbstractGroundItem? {
        val tile = this.tiles[plane][x][y]
        if (tile == null) {
            return null
        }
        val groundItem = tile.groundItem
        tile.groundItem = null
        pushIntoStack(groundItem)
        return groundItem
    }

    fun removeInteractive(interactive: AbstractInteractiveEntity, clearFully: Boolean) {
        notifyInteractiveChanged(interactive)
        for (x in interactive.startX..interactive.endX) {
            for (y in interactive.startZ..interactive.endZ) {
                val tile = this.tiles[interactive.plane.toInt()][x][y]
                if (tile != null) {
                    var container = tile.interactives
                    var previous: InteractiveEntityContainer? = null
                    while (container != null) {
                        if (interactive === container.interactiveObject) {
                            if (previous != null) {
                                previous.child = container.child
                            } else {
                                tile.interactives = container.child
                            }
                            container.release()
                            break
                        }
                        previous = container
                        container = container.child
                    }
                }
            }
        }
        if (!clearFully) {
            pushIntoStack(interactive)
        }
    }

    fun pushIntoStack(entity: SceneGraphEntity?) {
        if (entity != null) {
            entity.detach()
            for (index in 0..1) {
                var previous: SceneGraphEntity? = null
                var node: SceneGraphEntity? = this.staticTextureOpaqueEntities[index]
                while (node != null) {
                    if (node === entity) {
                        if (previous != null) {
                            previous.child = node.child
                        } else {
                            this.staticTextureOpaqueEntities[index] = node.child
                        }
                        return
                    }
                    previous = node
                    node = node.child
                }
                previous = null
                node = this.alphaEntities[index]
                while (node != null) {
                    if (node === entity) {
                        if (previous != null) {
                            previous.child = node.child
                        } else {
                            this.alphaEntities[index] = node.child
                        }
                        return
                    }
                    previous = node
                    node = node.child
                }
                previous = null
                node = this.dynamicRenderObjects[index]
                while (node != null) {
                    if (node === entity) {
                        if (previous != null) {
                            previous.child = node.child
                        } else {
                            this.dynamicRenderObjects[index] = node.child
                        }
                        return
                    }
                    previous = node
                    node = node.child
                }
            }
        }
    }

    fun getSecondaryWall(plane: Int, x: Int, y: Int): AbstractWall? {
        val tile = this.tiles[plane][x][y]
        return tile?.secondaryWall
    }

    fun getInteractiveObject(plane: Int, x: Int, y: Int, comparable: ComparableGraphNode?): AbstractInteractiveEntity? {
        val tile = this.tiles[plane][x][y]
        if (tile != null) {
            var container = tile.interactives
            while (container != null) {
                val interactive = container.interactiveObject
                if ((comparable == null || comparable.matches(interactive!!)) && x == interactive!!.startX.toInt() && y == interactive!!.startZ.toInt()) {
                    return interactive
                }
                container = container.child
            }
        }
        return null
    }

    fun getGroundDecoration(plane: Int, x: Int, y: Int): AbstractGroundDecoration? {
        val tile = this.tiles[plane][x][y]
        return if (tile != null && tile.groundDecoration != null) tile.groundDecoration else null
    }

    fun castShadow(entity: SceneGraphEntity, plane: Int, x: Int, y: Int) {
        retainedChunkChanged(plane, x, y)
        if (x < this.sceneDimensionX) {
            val tile = this.tiles[plane][x + 1][y]
            if (tile != null && tile.groundDecoration != null && tile.groundDecoration!!.hasTileShadow()) {
                entity.castShadow((tile.groundDecoration as SceneGraphEntity), this.fullTileDimension, 0, 0, true)
            }
        }
        if (y < this.sceneDimensionX) {
            val tile = this.tiles[plane][x][y + 1]
            if (tile != null && tile.groundDecoration != null && tile.groundDecoration!!.hasTileShadow()) {
                entity.castShadow((tile.groundDecoration as SceneGraphEntity), 0, 0, this.fullTileDimension, true)
            }
        }
        if (x < this.sceneDimensionX && y < this.sceneDimensionY) {
            val tile = this.tiles[plane][x + 1][y + 1]
            if (tile != null && tile.groundDecoration != null && tile.groundDecoration!!.hasTileShadow()) {
                entity.castShadow((tile.groundDecoration as SceneGraphEntity), this.fullTileDimension, 0, this.fullTileDimension, true)
            }
        }
        if (x < this.sceneDimensionX && y > 0) {
            val tile = this.tiles[plane][x + 1][y - 1]
            if (tile != null && tile.groundDecoration != null && tile.groundDecoration!!.hasTileShadow()) {
                entity.castShadow((tile.groundDecoration as SceneGraphEntity), this.fullTileDimension, 0, -this.fullTileDimension, true)
            }
        }
    }

    /**
     * Adjusts the depth of the object (perspective_z)
     * to prevent the floor rendering over an object
     */
    fun separateFromFloor(entity: SceneGraphEntity) {
        walkNodes++
        val absolutePosition = entity.getAbsolutePosition().coords
        this.activeToolkit.calculateProjection(absolutePosition.x.toInt().toFloat(), (absolutePosition.y.toInt() + (entity.getMinY() shr 1)).toFloat(), absolutePosition.z.toInt().toFloat(), this.renderedGraphNodePosition)
        entity.floorDepth = this.renderedGraphNodePosition[2].toInt()
    }

    fun removePrimaryWall(plane: Int, x: Int, y: Int): AbstractWall? {
        retainedChunkChanged(plane, x, y)
        val tile = this.tiles[plane][x][y]
        if (tile != null) {
            pushIntoStack(tile.primaryWall)
            if (tile.primaryWall != null) {
                val primaryWall = tile.primaryWall
                tile.primaryWall = null
                return primaryWall
            }
        }
        return null
    }

    /**
     * Determine if the game object on the tile is supposed to be culled or not
     * If the object is culled that DOES NOT mean the tile is culled too, it just means the gameobject is hidden
     */
    fun isHidden(entity: SceneGraphEntity, hideInteractives: Boolean, settingBits: Array<Array<ByteArray>>?, lowestRenderedPlane: Int, roofPreference: Byte): Boolean {
        if (!this.calculateTilesOnScreen) {
            return false
        }
        if (entity is AbstractInteractiveEntity) {
            val interactive = entity
            val endX = interactive.endX
            val endY = interactive.endZ
            val startX = interactive.startX
            val startY = interactive.startZ
            for (x in startX..endX) {
                for (y in startY..endY) {
                    if (entity.collisionPlane < this.regionSizeZ && x >= this.visibleToXnegative && x < this.visibleToXpositive && y >= this.visibleToYnegative && y < this.visibleToYpositive) {
                        if ((settingBits == null || entity.plane.toInt() < lowestRenderedPlane || roofPreference != settingBits[entity.plane.toInt()][x][y])
                                && entity.canRender() && !entity.isSoftHidden(this.activeToolkit)) {
                            return false
                        }
                        if (particleTickThisFrame && !hideInteractives && x >= this.cameraGridPositionX - 16 && x <= this.cameraGridPositionX + 16
                                               && y >= this.cameraGridPositionY - 16 && y <= this.cameraGridPositionY + 16) {
                            entity.particleCycle(this.activeToolkit)
                        }
                        return true
                    }
                }
            }
            return true
        } else {
            val absolutePosition = entity.getAbsolutePosition().coords
            val sceneX = absolutePosition.x.toInt() shr this.tileUnits
            val sceneY = absolutePosition.z.toInt() shr this.tileUnits
            if (entity.collisionPlane < this.regionSizeZ && sceneX >= this.visibleToXnegative && sceneX < this.visibleToXpositive
                                                           && sceneY >= this.visibleToYnegative && sceneY < this.visibleToYpositive) {
                val entityPlane = entity.plane.toInt()
                val locByLowPlane = entityPlane < lowestRenderedPlane
                val locByBits = settingBits != null && !locByLowPlane &&
                    roofPreference != settingBits[entityPlane][sceneX][sceneY]
                if ((settingBits == null || locByLowPlane || locByBits)
                        && entity.canRender() && !entity.isSoftHidden(this.activeToolkit)) {
                    return false
                }
                if (particleTickThisFrame && !hideInteractives && sceneX >= this.cameraGridPositionX - 16 && sceneX <= this.cameraGridPositionX + 16
                                       && sceneY >= this.cameraGridPositionY - 16 && sceneY <= this.cameraGridPositionY + 16) {
                    entity.particleCycle(this.activeToolkit)
                }
                return true
            }
            return true
        }
    }

    fun renderShadows(shadow: Shadow, casterPlane: Int, x: Int, y: Int, shadowedPlanes: BooleanArray?): Boolean {
        var shadowsAdded = false
        if (this.localHeightmap !== this.underwaterHeightmap) {
            val casterHeight = this.surfaceHeightmap[casterPlane]!!.getAverageHeight(x, y)
            for (plane in 0..casterPlane) {
                val ground = this.surfaceHeightmap[plane]
                if (ground != null) {
                    val heightDelta = casterHeight - ground.getAverageHeight(x, y)
                    if (shadowedPlanes != null) {
                        shadowedPlanes[plane] = ground.hasRenderingShadows(shadow, x, heightDelta, y)
                        if (!shadowedPlanes[plane]) {
                            continue
                        }
                    }
                    ground.addShadow(shadow, x, heightDelta, y)
                    shadowsAdded = true
                }
            }
        }
        return shadowsAdded
    }

    fun removeShadow(shadow: Shadow, casterPlane: Int, x: Int, y: Int, shadowedPlanes: BooleanArray?) {
        if (this.underwaterHeightmap !== this.localHeightmap) {
            val casterHeight = this.surfaceHeightmap[casterPlane]!!.getAverageHeight(x, y)
            for (plane in 0..casterPlane) {
                if (shadowedPlanes == null || shadowedPlanes[plane]) {
                    val ground = this.surfaceHeightmap[plane]
                    if (ground != null) {
                        ground.removeShadow(shadow, x, casterHeight - ground.getAverageHeight(x, y), y)
                    }
                }
            }
        }
    }

    fun resetSurfaceTiles() {
        retainedSceneReset()
        for (x in 0 until this.sceneDimensionX) {
            for (y in 0 until this.sceneDimensionY) {
                if (this.tiles[0][x][y] == null) {
                    this.tiles[0][x][y] = Tile(0)
                }
            }
        }
    }

    fun addHdShadow() {
        addHdShadow(this.regionSizeZ)
    }

    fun calculateLightUid(positionedLight: FlickeringEffect) {
        if (this.lightSlot < 65361) {
            val light = positionedLight.pointLight
            this.lightSlotOccupant!![this.lightSlot] = positionedLight
            this.lightSlotAvailable!![this.lightSlot] = false
            this.lightSlot++
            var lightStartPlane = positionedLight.level
            if (positionedLight.growsDownwards) {
                lightStartPlane = 0
            }
            var lightEndPlane = positionedLight.level
            if (positionedLight.growsUpwards) {
                lightEndPlane = this.regionSizeZ - 1
            }
            for (plane in lightStartPlane..lightEndPlane) {
                var rangeIndex = 0
                var lightStartY = light!!.getZ() - light!!.getSize2d() + this.halfTileOffset shr this.tileUnits
                if (lightStartY < 0) {
                    rangeIndex -= lightStartY
                    lightStartY = 0
                }
                var lightEndY = light!!.getZ() + light!!.getSize2d() - this.halfTileOffset shr this.tileUnits
                if (lightEndY >= this.sceneDimensionY) {
                    lightEndY = this.sceneDimensionY - 1
                }
                for (y in lightStartY..lightEndY) {
                    val range = positionedLight.ranges[rangeIndex++]
                    var lightStartX = (light!!.getX() - light!!.getSize2d() + this.halfTileOffset shr this.tileUnits) + (range.toInt() ushr 8)
                    var lightEndX = (range.toInt() and 0xff) + lightStartX - 1
                    if (lightStartX < 0) {
                        lightStartX = 0
                    }
                    if (lightEndX >= this.sceneDimensionX) {
                        lightEndX = this.sceneDimensionX - 1
                    }
                    for (x in lightStartX..lightEndX) {
                        val lightBaseUids = this.tileLightUids!![plane][x][y]
                        if ((lightBaseUids and 0xffffL) == 0L) {
                            this.tileLightUids!![plane][x][y] = lightBaseUids or this.lightSlot.toLong()
                        } else if ((lightBaseUids and 0xffff0000L) == 0L) {
                            this.tileLightUids!![plane][x][y] = lightBaseUids or (this.lightSlot.toLong() shl 16)
                        } else if ((lightBaseUids and 0xffff00000000L) == 0L) {
                            this.tileLightUids!![plane][x][y] = lightBaseUids or (this.lightSlot.toLong() shl 32)
                        } else if ((lightBaseUids and -281474976710656L) == 0L) {
                            this.tileLightUids!![plane][x][y] = lightBaseUids or (this.lightSlot.toLong() shl 48)
                        }
                    }
                }
            }
        }
    }

    // bake light?
    fun addHdLight() {
        for (slotIndex in 0 until this.lightSlot) {
            if (!this.lightSlotAvailable!![slotIndex]) {
                val positionedLight = this.lightSlotOccupant!![slotIndex]!!
                val light = positionedLight.pointLight
                val plane = positionedLight.level
                val offset = light!!.getSize2d() - this.halfTileOffset
                val dimension = (offset * 2 shr this.tileUnits) + 1
                var rangeIndex = 0
                val lightDisplacement = IntArray(dimension * dimension)
                val sceneBaseX = light!!.getX() - offset shr this.tileUnits
                var sceneStartY = light!!.getZ() - offset shr this.tileUnits
                var sceneEndY = light!!.getZ() + offset shr this.tileUnits
                if (sceneStartY < 0) {
                    rangeIndex -= sceneStartY
                    sceneStartY = 0
                }
                if (sceneEndY >= this.sceneDimensionY) {
                    sceneEndY = this.sceneDimensionY - 1
                }
                for (y in sceneStartY..sceneEndY) {
                    val range = positionedLight.ranges[rangeIndex]
                    val rangeStartOffset = range.toInt() ushr 8
                    var displacementIndex = rangeStartOffset + rangeIndex * dimension
                    var sceneStartX = (range.toInt() ushr 8) + sceneBaseX
                    var sceneEndX = (range.toInt() and 0xff) + sceneStartX - 1
                    if (sceneStartX < 0) {
                        displacementIndex -= sceneStartX
                        sceneStartX = 0
                    }
                    if (sceneEndX >= this.sceneDimensionX) {
                        sceneEndX = this.sceneDimensionX - 1
                    }
                    for (x in sceneStartX..sceneEndX) {
                        var displacement: Byte = 1
                        val interactive = getInteractiveObject(plane, x, y, null)
                        if (interactive != null && interactive.rotation.toInt() != 0) {
                            var westWithinScene: Boolean
                            var eastWithinScene: Boolean
                            var bitpackedRange: Short
                            var startX: Int
                            var endX: Int
                            if (interactive.rotation.toInt() == 1) {
                                westWithinScene = x - 1 >= sceneStartX
                                eastWithinScene = x + 1 <= sceneEndX
                                if (!westWithinScene && y + 1 <= sceneEndY) {
                                    bitpackedRange = positionedLight.ranges[rangeIndex + 1]
                                    startX = (bitpackedRange.toInt() ushr 8) + sceneBaseX
                                    endX = (bitpackedRange.toInt() and 0xff) + startX
                                    westWithinScene = x > startX && x < endX
                                }
                                if (!eastWithinScene && y - 1 >= sceneStartY) {
                                    bitpackedRange = positionedLight.ranges[rangeIndex - 1]
                                    startX = (bitpackedRange.toInt() ushr 8) + sceneBaseX
                                    endX = (bitpackedRange.toInt() and 0xff) + startX
                                    eastWithinScene = x > startX && x < endX
                                }
                                if (westWithinScene && !eastWithinScene) {
                                    displacement = 4
                                } else if (eastWithinScene && !westWithinScene) {
                                    displacement = 2
                                }
                            } else {
                                westWithinScene = x - 1 >= sceneStartX
                                eastWithinScene = x + 1 <= sceneEndX
                                if (!westWithinScene && y - 1 >= sceneStartY) {
                                    bitpackedRange = positionedLight.ranges[rangeIndex - 1]
                                    startX = (bitpackedRange.toInt() ushr 8) + sceneBaseX
                                    endX = (bitpackedRange.toInt() and 0xff) + startX
                                    westWithinScene = x > startX && x < endX
                                }
                                if (!eastWithinScene && y + 1 <= sceneEndY) {
                                    bitpackedRange = positionedLight.ranges[rangeIndex + 1]
                                    startX = (bitpackedRange.toInt() ushr 8) + sceneBaseX
                                    endX = (bitpackedRange.toInt() and 0xff) + startX
                                    eastWithinScene = x > startX && x < endX
                                }
                                if (westWithinScene && !eastWithinScene) {
                                    displacement = 3
                                } else if (eastWithinScene && !westWithinScene) {
                                    displacement = 5
                                }
                            }
                        }
                        lightDisplacement[displacementIndex++] = displacement.toInt()
                    }
                    ++rangeIndex
                }
                this.lightSlotAvailable!![slotIndex] = true
                this.localHeightmap[plane]!!.addLight(light, lightDisplacement)
            }
        }
    }

    fun getWaterOpcode20(x: Int, y: Int): Int {
        return if (this.opcode20 != null) this.opcode20!![x][y].toInt() and 0xff else 0
    }

    fun getGroundItem(plane: Int, x: Int, y: Int): AbstractGroundItem? {
        val tile = this.tiles[plane][x][y]
        return tile?.groundItem
    }

    fun removeSecondaryWallDecoration(plane: Int, x: Int, y: Int): AbstractWallDecoration? {
        retainedChunkChanged(plane, x, y)
        val tile = this.tiles[plane][x][y]
        if (tile != null) {
            pushIntoStack(tile.secondaryWallDecoration)
            if (tile.secondaryWallDecoration != null) {
                val secondaryWallDecoration = tile.secondaryWallDecoration
                tile.secondaryWallDecoration = null
                return secondaryWallDecoration
            }
        }
        return null
    }

    private fun addDepthEntity(entity: SceneGraphEntity) {
        if (this.depthObjectCount == this.depthRenderedEntities.size) {
            this.depthRenderedEntities = this.depthRenderedEntities.copyOf(this.depthObjectCount * 2)
        }
        this.depthRenderedEntities[this.depthObjectCount++] = entity
    }

    private fun addPriorityEntity(entity: SceneGraphEntity) {
        if (this.layeredObjectCount == this.priorityRenderedEntities.size) {
            this.priorityRenderedEntities = this.priorityRenderedEntities.copyOf(this.layeredObjectCount * 2)
        }
        this.priorityRenderedEntities[this.layeredObjectCount++] = entity
    }

    private fun addInteractiveObject(interactive: AbstractInteractiveEntity) {
        if (this.interactiveObjectCount == this.interactiveObjects.size) {
            this.interactiveObjects = this.interactiveObjects.copyOf(this.interactiveObjectCount * 2)
        }
        this.interactiveObjects[this.interactiveObjectCount++] = interactive
    }

    fun drawTiles(hideInteractives: Boolean, settingBits: Array<Array<ByteArray>>?, lowestRenderedPlane: Int, roofPreference: Byte) {
        val layerIndex = if (hideInteractives) 1 else 0
        this.depthObjectCount = 0
        this.layeredObjectCount = 0
        this.tileDrawCycle++
        val walkT0 = System.nanoTime()
        // Visibility BEFORE projection.
        //
        // isHidden rejects a node whose tiles all fall outside the visible
        // window, and does so without side effects — particleCycle only runs for
        // nodes that ARE in range, exactly as before. Roughly three quarters of the
        // scene graph is rejected that way on a typical frame, and every one of them
        // used to pay a full projection first. The only thing that changes is that a
        // rejected node's floorDepth is left at its previous value, which nothing
        // reads: it is the depth sort key for accepted nodes, and the interaction
        // cache only ever holds nodes that were drawn.
        // Retained statics: a deferred renderer may hold the static entities' draws
        // and resubmit them itself (RetainedSceneHook). Only on the surface pass — the
        // underwater pass (hideInteractives) is rare and keeps the full walk.
        val hook = retainedHook
        val retained = hook != null && !hideInteractives && hook.active(this)
        var entity: SceneGraphEntity?
        if (retained) {
            if (walkCacheRevision != walkRevision || walkCacheLayer != layerIndex) rebuildWalkCache(layerIndex)
            val opaque = walkCacheOpaque
            for (index in 0 until walkCacheOpaqueCount) {
                val e = opaque[index]!!
                if (!isHidden(e, hideInteractives, settingBits, lowestRenderedPlane, roofPreference)) {
                    separateFromFloor(e)
                    if (e.floorDepth != -1) {
                        addDepthEntity(e)
                    }
                }
            }
            val alpha = walkCacheAlpha
            for (index in 0 until walkCacheAlphaCount) {
                val e = alpha[index]!!
                if (!isHidden(e, hideInteractives, settingBits, lowestRenderedPlane, roofPreference)) {
                    separateFromFloor(e)
                    if (e.floorDepth != -1) {
                        addPriorityEntity(e)
                    }
                }
            }
        } else {
            entity = this.staticTextureOpaqueEntities[layerIndex]
            while (entity != null) {
                if (!isHidden(entity, hideInteractives, settingBits, lowestRenderedPlane, roofPreference)) {
                    separateFromFloor(entity)
                    if (entity.floorDepth != -1) {
                        addDepthEntity(entity)
                    }
                }
                entity = entity.child
            }
            entity = this.alphaEntities[layerIndex]
            while (entity != null) {
                if (!isHidden(entity, hideInteractives, settingBits, lowestRenderedPlane, roofPreference)) {
                    separateFromFloor(entity)
                    if (entity.floorDepth != -1) {
                        addPriorityEntity(entity)
                    }
                }
                entity = entity.child
            }
        }
        entity = this.dynamicRenderObjects[layerIndex]
        while (entity != null) {
            if (!isHidden(entity, hideInteractives, settingBits, lowestRenderedPlane, roofPreference)) {
                separateFromFloor(entity)
                if (entity.floorDepth != -1) {
                    if (entity.hasAlpha()) {
                        addPriorityEntity(entity)
                    } else {
                        addDepthEntity(entity)
                    }
                }
            }
            entity = entity.child
        }
        if (!hideInteractives) {
            for (index in 0 until this.interactiveObjectCount) {
                val interactive = this.interactiveObjects[index]!!
                if (retained && interactive.modernRetained) continue
                if (!isHidden(interactive, false, settingBits, lowestRenderedPlane, roofPreference)) {
                    separateFromFloor(interactive)
                    if (interactive.floorDepth != -1) {
                        if (interactive.hasAlpha()) {
                            addPriorityEntity(interactive)
                        } else {
                            addDepthEntity(interactive)
                        }
                    }
                }
            }
        }
        phaseWalkNs += System.nanoTime() - walkT0
        walkDrawn += this.depthObjectCount.toLong() + this.layeredObjectCount.toLong()

        if (retained) {
            val retainedT0 = System.nanoTime()
            hook!!.submitStatics(this, settingBits, lowestRenderedPlane, roofPreference)
            phaseRetainedNs += System.nanoTime() - retainedT0
        }

        /**
         * Renders general scene objects, walls
         */
        if (this.depthObjectCount > 0) {
            val sortT0 = System.nanoTime()
            quicksortBelow(this.depthRenderedEntities, 0, this.depthObjectCount - 1)
            phaseSortNs += System.nanoTime() - sortT0
            val drawT0 = System.nanoTime()
            for (index in 0 until this.depthObjectCount) {
                placeSceneryObjects(this.depthRenderedEntities[index]!!, this.activeLights)
            }
            phaseDrawNs += System.nanoTime() - drawT0
        }
        if (this.copyLights) {
            this.activeToolkit.copyLights(0, null as Array<PointLight>?)
        }
        val floorT0 = System.nanoTime()
        for (plane in 0 until this.regionSizeZ) {
            // A plane with no floor to draw does not need its per-tile visibility
            // computed: the only consumer is the renderFloor call below, which would
            // return immediately. tileOnScreen is fully rewritten by whichever plane
            // runs next, so skipping leaves nothing stale behind.
            val planeGround = this.localHeightmap[plane]
            if (planeGround == null || !planeGround.hasRenderableFloor()) {
                planesSkipped++
                continue
            }
            planesDrawn++
            if (plane >= lowestRenderedPlane && settingBits != null) {
                // These bounds are array indices, and index -> scene tile is
                // `tileX + visibleToXnegative - visibleToInvertedXnegative`, so the
                // overflow test has to subtract the inverted offset too. Without it the
                // window is clamped in scene space while used in index space, leaving the
                // tiles the camera actually sees untouched -- they then keep the raw
                // frustum visibility drawScene wrote and render with no roof test at all.
                var visibleSizeX = this.tileInFrustum.size
                if (this.tileInFrustum.size + this.visibleToXnegative - this.visibleToInvertedXnegative > this.sceneDimensionX) {
                    visibleSizeX -= this.tileInFrustum.size + this.visibleToXnegative - this.visibleToInvertedXnegative - this.sceneDimensionX
                }
                var visibleSizeY = this.tileInFrustum[0].size
                if (this.tileInFrustum[0].size + this.visibleToYnegative - this.visibleToInvertedYnegative > this.sceneDimensionY) {
                    visibleSizeY -= this.tileInFrustum[0].size + this.visibleToYnegative - this.visibleToInvertedYnegative - this.sceneDimensionY
                }
                if (this.calculateTilesOnScreen) {
                    val yBase = this.visibleToYnegative - this.visibleToInvertedYnegative
                    val cullingLocal = this.culling
                    // Which descending plane owns each tile of this floor is static per
                    // scene: resolve it once (refreshed periodically in case tiles are
                    // still streaming in) instead of chasing tiles[p][x][y] down the
                    // planes for every tile of every floor every frame.
                    val owner = floorOwnerPlane(plane)
                    val dimY = this.sceneDimensionY
                    val cullingOff = !cullingLocal.cullingEnabled
                    for (tileX in this.visibleToInvertedXnegative until visibleSizeX) {
                        val sceneX = tileX + this.visibleToXnegative - this.visibleToInvertedXnegative
                        val onScreenRow = this.tileOnScreen[tileX]
                        val adjacentRow = this.tileInFrustum[tileX]
                        val ownerBase = sceneX * dimY
                        for (tileY in this.visibleToInvertedYnegative until visibleSizeY) {
                            if (!adjacentRow[tileY]) { onScreenRow[tileY] = false; continue }
                            val sceneY = tileY + yBase
                            val descendingPlane = owner[ownerBase + sceneY].toInt()
                            if (descendingPlane < 0) { onScreenRow[tileY] = false; continue }
                            val byLowPlane = descendingPlane < lowestRenderedPlane
                            val byRoofBits = settingBits[descendingPlane][sceneX][sceneY] != roofPreference
                            onScreenRow[tileY] = (byLowPlane || byRoofBits) &&
                                (cullingOff || !cullingLocal.isTileHidden(plane, sceneX, sceneY))
                        }
                    }
                }
                this.localHeightmap[plane]!!.renderFloor(this.cameraGridPositionX, this.cameraGridPositionY, this.visibleTiles, this.tileOnScreen, false)
            } else {
                // Same index-space clamp as the roof-tested branch above.
                var visibleSizeX = this.tileInFrustum.size
                if (this.tileInFrustum.size + this.visibleToXnegative - this.visibleToInvertedXnegative > this.sceneDimensionX) {
                    visibleSizeX -= this.tileInFrustum.size + this.visibleToXnegative - this.visibleToInvertedXnegative - this.sceneDimensionX
                }
                var visibleSizeY = this.tileInFrustum[0].size
                if (this.tileInFrustum[0].size + this.visibleToYnegative - this.visibleToInvertedYnegative > this.sceneDimensionY) {
                    visibleSizeY -= this.tileInFrustum[0].size + this.visibleToYnegative - this.visibleToInvertedYnegative - this.sceneDimensionY
                }
                if (this.calculateTilesOnScreen) {
                    val yBase = this.visibleToYnegative - this.visibleToInvertedYnegative
                    val cullingLocal = this.culling
                    for (tileX in this.visibleToInvertedXnegative until visibleSizeX) {
                        val sceneX = tileX + this.visibleToXnegative - this.visibleToInvertedXnegative
                        val onScreenRow = this.tileOnScreen[tileX]
                        val adjacentRow = this.tileInFrustum[tileX]
                        for (tileY in this.visibleToInvertedYnegative until visibleSizeY) {
                            onScreenRow[tileY] = adjacentRow[tileY] &&
                                !cullingLocal.isTileHidden(plane, sceneX, tileY + yBase)
                        }
                    }
                }
                this.localHeightmap[plane]!!.renderFloor(this.cameraGridPositionX, this.cameraGridPositionY, this.visibleTiles, this.tileOnScreen, true)
            }
        }
        phaseFloorNs += System.nanoTime() - floorT0

        /**
         * Renders ground decoration, light rays, objects with particles, roof / next level decoration
         */
        if (DUMP_ALPHA && !hideInteractives) dumpAlphaTick++
        if (this.layeredObjectCount > 0) {
            val alphaT0 = System.nanoTime()
            quicksortAbove(this.priorityRenderedEntities, 0, this.layeredObjectCount - 1)
            // The alpha bucket. Legacy relies on nothing but submission order here:
            // these entities are drawn after every opaque entity and after the floor,
            // back to front, so their blended faces land over whatever is behind
            // them. A deferred/batched toolkit has no way to recover that ordering
            // from the models themselves (hasAlpha() is per-entity, not per-model),
            // so the walk announces the phase and the toolkit keeps these draws in
            // an ordered pass of their own.
            alphaPassActive = true
            try {
                for (index in 0 until this.layeredObjectCount) {
                    placeSceneryObjects(this.priorityRenderedEntities[index]!!, this.activeLights)
                }
            } finally {
                alphaPassActive = false
            }
            phaseAlphaNs += System.nanoTime() - alphaT0
            walkAlpha += this.layeredObjectCount.toLong()
        }
    }
    // Per floor plane: the descending plane whose tile carries that floor at each
    // (x, y), or -1. See drawTiles.
    private var floorOwnerPlanes: Array<ByteArray?> = arrayOfNulls(0)
    private var floorOwnerCycle = IntArray(0)
    private fun floorOwnerPlane(plane: Int): ByteArray {
        if (floorOwnerPlanes.size != this.regionSizeZ) {
            floorOwnerPlanes = arrayOfNulls(this.regionSizeZ)
            floorOwnerCycle = IntArray(this.regionSizeZ) { Int.MIN_VALUE }
        }
        var owner = floorOwnerPlanes[plane]
        val dimX = this.sceneDimensionX
        val dimY = this.sceneDimensionY
        if (owner == null || owner.size != dimX * dimY || this.tileDrawCycle - floorOwnerCycle[plane] > 64) {
            if (owner == null || owner.size != dimX * dimY) owner = ByteArray(dimX * dimY)
            for (x in 0 until dimX) {
                val base = x * dimY
                for (y in 0 until dimY) {
                    var found = -1
                    var p = plane
                    while (p >= 0) {
                        val tile = this.tiles[p][x][y]
                        if (tile != null && tile.plane.toInt() == plane) { found = p; break }
                        --p
                    }
                    owner[base + y] = found.toByte()
                }
            }
            floorOwnerPlanes[plane] = owner
            floorOwnerCycle[plane] = this.tileDrawCycle
        }
        return owner
    }
    private var lastParticlePulse = -1L
    /** True on frames that advance particle simulation (one per logic pulse). */
    @JvmField var particleTickThisFrame = true
    private val PARTICLE_PER_TICK = System.getProperty("render.particletick")?.equals("false", true) != true


    fun drawScene(rotation: Int, cameraX: Int, cameraZ: Int, cameraY: Int, settingBits: Array<Array<ByteArray>>?,
                          visibleBelowDrawheight: IntArray?, visibleAboveX: IntArray?, visibleBelowX: IntArray?,
                          visibleBelowY: IntArray?, visibleAboveY: IntArray?, lowestRenderedPlane: Int, roofPreference: Byte,
                          disableFlickeringEffects: Boolean) {
        this.culling.drawingScene = true
        this.calculateTilesOnScreen = true
        this.cameraGridPositionX = cameraX shr this.tileUnits
        this.cameraGridPositionY = cameraY shr this.tileUnits
        this.cameraPositionX = cameraX
        this.cameraPositionZ = cameraY
        this.cameraPositionY = cameraZ
        this.visibleToXnegative = this.cameraGridPositionX - this.visibleTiles
        if (this.visibleToXnegative < 0) {
            this.visibleToInvertedXnegative = -(this.visibleToXnegative * 1607871809) * -1275764031
            this.visibleToXnegative = 0
        } else {
            this.visibleToInvertedXnegative = 0
        }
        this.visibleToYnegative = this.cameraGridPositionY - this.visibleTiles
        if (this.visibleToYnegative < 0) {
            this.visibleToInvertedYnegative = -(this.visibleToYnegative * 1831414049) * -2139776287
            this.visibleToYnegative = 0
        } else {
            this.visibleToInvertedYnegative = 0
        }
        this.visibleToXpositive = this.visibleTiles + this.cameraGridPositionX
        if (this.visibleToXpositive > this.sceneDimensionX) {
            this.visibleToXpositive = this.sceneDimensionX
        }
        this.visibleToYpositive = this.visibleTiles + this.cameraGridPositionY
        if (this.visibleToYpositive > this.sceneDimensionY) {
            this.visibleToYpositive = this.sceneDimensionY
        }
        val unadjustedAdjacentTilesOnScreen = this.tileInFrustum
        val unadjustedTilesOnScreen = this.tileOnScreen
        // Everything the tile-visibility grid needs, read once. This loop runs over
        // (2*visibleTiles+2)^2 tiles — around eleven thousand — every frame, and each
        // of the fields below was being re-read from `this` on every one of them,
        // along with a fresh row lookup into each two-dimensional array.
        val gridSpan = this.visibleTiles + this.visibleTiles + 2
        val tileUnitsLocal = this.tileUnits
        val heightBias = 1000 shl (tileUnitsLocal - 7)
        val fullTile = this.fullTileDimension
        val dimX = this.sceneDimensionX
        val dimY = this.sceneDimensionY
        val originX = this.cameraGridPositionX - this.visibleTiles
        val originY = this.cameraGridPositionY - this.visibleTiles
        val toolkit = this.activeToolkit
        val visibility = this.tileVisibility
        val onScreenGrid = this.tileOnScreen
        val adjacentGrid = this.tileInFrustum
        val topSurface = this.surfaceHeightmap[this.surfaceHeightmap.size - 1]!!
        val lowSurface = this.underwaterHeightmap?.get(0) ?: this.surfaceHeightmap[0]!!
        for (tileX in 0 until gridSpan) {
            var previousOnScreen = 0
            var currentOnScreen = 0
            val sceneX = tileX + originX
            val xInRange = sceneX >= 0 && sceneX < dimX
            val upscaledSceneX = sceneX shl tileUnitsLocal
            // Row slices: the heightmaps and both grids are arrays of rows, so the
            // outer index only has to be resolved once per column of the sweep.
            val topRow = if (xInRange) topSurface.tileHeights[sceneX] else null
            val lowRow = if (xInRange) lowSurface.tileHeights[sceneX] else null
            val onScreenRow = onScreenGrid[tileX]
            val adjacentRow = if (tileX > 0) adjacentGrid[tileX - 1] else null

            // Clip flags for the whole column in one call. Testing them one at a time
            // meant a virtual dispatch per tile — more expensive than the arithmetic
            // it guarded, at eleven thousand tiles a frame.
            if (columnScratchZ.size < gridSpan) {
                columnScratchZ = IntArray(gridSpan)
                columnScratchLow = IntArray(gridSpan)
                columnScratchHigh = IntArray(gridSpan)
                columnScratchOut = IntArray(gridSpan)
            }
            val zs = columnScratchZ
            val lows = columnScratchLow
            val highs = columnScratchHigh
            val flags = columnScratchOut
            var runCount = 0
            var runFirstY = 0
            if (xInRange) {
                runFirstY = if (originY < 0) -originY else 0
                var tileY = runFirstY
                while (tileY < gridSpan) {
                    val sceneY = tileY + originY
                    if (sceneY >= dimY) break
                    zs[runCount] = sceneY shl tileUnitsLocal
                    lows[runCount] = topRow!![sceneY] - heightBias
                    highs[runCount] = lowRow!![sceneY] + fullTile + heightBias
                    runCount++
                    tileY++
                }
                if (runCount > 0) {
                    walkTiles += runCount.toLong()
                    toolkit.isColumnRunOnScreen(upscaledSceneX, zs, lows, highs, flags, runCount)
                    if (VERIFY_CULL) {
                        // Oracle: the batched run must agree with the original
                        // per-tile call for every tile, or the terrain and the
                        // entity window disagree about what is on screen.
                        for (i in 0 until runCount) {
                            val expected = toolkit.isOnScreen(
                                upscaledSceneX, lows[i], zs[i], upscaledSceneX, highs[i], zs[i]
                            )
                            cullVerifyChecks++
                            if (expected != flags[i]) {
                                cullVerifyBad++
                                if (cullVerifyBad <= 10) System.err.println(
                                    "[CullVerify] tile x=$upscaledSceneX z=${zs[i]} batched=${flags[i]} expected=$expected")
                            }
                        }
                    }
                }
            }

            for (tileY in 0 until gridSpan) {
                if (tileY > 1) {
                    visibility[tileY - 2] = previousOnScreen
                }
                previousOnScreen = currentOnScreen
                val runIndex = tileY - runFirstY
                if (runIndex in 0 until runCount) {
                    currentOnScreen = flags[runIndex]
                    onScreenRow[tileY] = currentOnScreen == 0
                } else {
                    currentOnScreen = -1
                    onScreenRow[tileY] = false
                }
                if (adjacentRow != null && tileY > 0) {
                    val isOnscreen = visibility[tileY - 1] and visibility[tileY] and previousOnScreen and currentOnScreen
                    adjacentRow[tileY - 1] = isOnscreen == 0
                }
            }
            visibility[gridSpan - 2] = previousOnScreen
            visibility[gridSpan - 1] = currentOnScreen
        }
        this.culling.visibleBelowDrawheight = visibleBelowDrawheight
        this.culling.visibleAboveX = visibleAboveX
        this.culling.visibleBelowX = visibleBelowX
        this.culling.visibleBelowY = visibleBelowY
        this.culling.visibleAboveY = visibleAboveY
        val cullT0 = System.nanoTime()
        this.culling.processCulling(this.activeToolkit, lowestRenderedPlane)
        phaseCullNs += System.nanoTime() - cullT0
        // Particle systems and emitters step once per CALL. The legacy client drew at
        // most 50 frames a second, so that was one step per logic tick; with the
        // render loop decoupled a 150-300 fps frame rate ran the simulation 3-6x too
        // fast and the cape sparkles fell far below the emitters. Step per logic pulse.
        particleTickThisFrame = !PARTICLE_PER_TICK || Client.logicPulses != lastParticlePulse
        lastParticlePulse = Client.logicPulses
        if (particleTickThisFrame) ParticleSystemManager.processParticleCycle(this, this.activeToolkit)
        if (!this.interactiveEntities.removeDuplicates) {
            this.interactiveEntities.clear()
        }
        if (this.copyLights) {
            for (slotIndex in 0 until this.lightSlot) {
                this.lightSlotOccupant!![slotIndex]!!.applyEffect(rotation, disableFlickeringEffects)
            }
        }
        if (this.waterTiles != null) {
            setUnderwater(true)
            this.activeToolkit.addWater(WaterTile())
            drawTiles(true, settingBits, lowestRenderedPlane, roofPreference)
            this.activeToolkit.onTilesDrawn()
            setUnderwater(false)
        }
        val tilesT0 = System.nanoTime()
        drawTiles(false, settingBits, lowestRenderedPlane, roofPreference)
        phaseTilesNs += System.nanoTime() - tilesT0
        phaseSamples++
        if (!this.calculateTilesOnScreen) {
            this.tileInFrustum = unadjustedAdjacentTilesOnScreen
            this.tileOnScreen = unadjustedTilesOnScreen
        }
    }

    fun castShadow(entity: SceneGraphEntity, basePlane: Int, startX: Int, startY: Int, width: Int, length: Int) {
        for (px in startX until startX + width) for (py in startY until startY + length) retainedChunkChanged(basePlane, px, py)
        var isSamePlane = true
        var lowestX = startX
        val highestX = startX + width
        val lowestY = startY - 1
        val highestY = startY + length
        for (plane in basePlane..basePlane + 1) {
            if (plane != this.regionSizeZ) {
                for (x in lowestX..highestX) {
                    if (x >= 0 && x < this.sceneDimensionX) {
                        for (y in lowestY..highestY) {
                            if (y >= 0 && y < this.sceneDimensionY && (!isSamePlane || x >= highestX || y >= highestY || y < startY && startX != x)) {
                                val tile = this.tiles[plane][x][y]
                                if (tile != null) {
                                    val heightDelta =
                                            (this.localHeightmap[plane]!!.getHeight(x, y) + this.localHeightmap[plane]!!.getHeight(1 + x, y)
                                            + this.localHeightmap[plane]!!.getHeight(x, 1 + y) + this.localHeightmap[plane]!!.getHeight(1 + x, 1 + y)) / 4 -
                                            (this.localHeightmap[basePlane]!!.getHeight(startX, startY) + this.localHeightmap[basePlane]!!.getHeight(startX + 1, startY)
                                            + this.localHeightmap[basePlane]!!.getHeight(startX, startY + 1) + this.localHeightmap[basePlane]!!.getHeight(startX + 1, startY + 1)) / 4
                                    val primaryWall = tile.primaryWall
                                    val secondaryWall = tile.secondaryWall
                                    if (primaryWall != null && primaryWall.hasTileShadow()) {
                                        entity.castShadow(primaryWall,
                                                (x - startX) * this.fullTileDimension + (1 - width) * this.halfTileOffset, heightDelta,
                                                (y - startY) * this.fullTileDimension + (1 - length) * this.halfTileOffset, isSamePlane)
                                    }
                                    if (secondaryWall != null && secondaryWall.hasTileShadow()) {
                                        entity.castShadow(secondaryWall,
                                                (x - startX) * this.fullTileDimension + (1 - width) * this.halfTileOffset, heightDelta,
                                                (y - startY) * this.fullTileDimension + (1 - length) * this.halfTileOffset, isSamePlane)
                                    }
                                    var container = tile.interactives
                                    while (container != null) {
                                        val interactive = container.interactiveObject
                                        if (interactive != null && interactive.hasTileShadow() && (x == interactive.startX.toInt() || lowestX == x) && (y == interactive.startZ.toInt() || lowestY == y)) {
                                            val sizeX = interactive.endX - interactive.startX + 1
                                            val sizeZ = interactive.endZ - interactive.startZ + 1
                                            entity.castShadow(interactive,
                                                    this.fullTileDimension * (interactive.startX - startX) + this.halfTileOffset * (sizeX - width), heightDelta,
                                                    this.fullTileDimension * (interactive.startZ - startY) + this.halfTileOffset * (sizeZ - length), isSamePlane)
                                        }
                                        container = container.child
                                    }
                                }
                            }
                        }
                    }
                }
                lowestX--
                isSamePlane = false
            }
        }
    }

    fun getTile(plane: Int, x: Int, y: Int): Tile? {
        if (this.tiles[plane][x][y] == null) {
            val isBridge = this.tiles[0][x][y] != null && this.tiles[0][x][y]!!.bridge != null
            if (isBridge && plane >= this.regionSizeZ - 1) {
                return null
            }
            loadTiles(plane, x, y)
        }
        return this.tiles[plane][x][y]
    }

    fun removeSecondaryWall(plane: Int, x: Int, y: Int): AbstractWall? {
        retainedChunkChanged(plane, x, y)
        val tile = this.tiles[plane][x][y]
        if (tile != null) {
            pushIntoStack(tile.secondaryWall)
            if (tile.secondaryWall != null) {
                val secondaryWall = tile.secondaryWall
                tile.secondaryWall = null
                return secondaryWall
            }
        }
        return null
    }

    fun unusedOpcode21(x: Int, y: Int): Int {
        return if (this.opcode21 != null) this.opcode21!![x][y].toInt() and 0xff else 0
    }

    /**
     * Disables lighting on objects if the light is off screen,
     * disabling this fixes some glitches
     * EG: object is half onscreen but the light is not
     */
    /**
     * Extracts each entity's [SceneGraphEntity.floorDepth] into [sortKeys] so the sort
     * compares ints in a packed array instead of dereferencing an entity per
     * comparison. The depths do not change during the sort, so this is exact; it just
     * turns ~26k pointer chases into one pass of [count] plus int compares.
     */
    private fun extractSortKeys(entities: Array<SceneGraphEntity?>, count: Int): IntArray {
        var keys = sortKeys
        if (keys.size < count) {
            keys = IntArray(count * 2)
            sortKeys = keys
        }
        for (i in 0 until count) keys[i] = entities[i]!!.floorDepth
        return keys
    }

    private var sortKeys = IntArray(0)

    /** -Drender.verifycull: cross-check the batched tile-visibility run against the per-tile call. */
    private val VERIFY_CULL = System.getProperty("render.verifycull") != null

    /** Per-column staging for the batched tile-visibility test in [drawScene]. */
    private var columnScratchZ = IntArray(0)
    private var columnScratchLow = IntArray(0)
    private var columnScratchHigh = IntArray(0)
    private var columnScratchOut = IntArray(0)

    fun quicksortBelow(entities: Array<SceneGraphEntity?>, startPosition: Int, endPosition: Int) {
        quicksortBelow(entities, extractSortKeys(entities, endPosition + 1), startPosition, endPosition)
    }

    private fun quicksortBelow(
        entities: Array<SceneGraphEntity?>, keys: IntArray, startPosition: Int, endPosition: Int
    ) {
        if (startPosition < endPosition) {
            val center = (endPosition + startPosition) / 2
            var curPosition = startPosition
            val pivotEntity = entities[center]
            val pivotKey = keys[center]
            entities[center] = entities[endPosition]
            keys[center] = keys[endPosition]
            entities[endPosition] = pivotEntity
            keys[endPosition] = pivotKey
            val offset = pivotKey
            for (index in startPosition until endPosition) {
                if (keys[index] < (index and 0x1) + offset) {
                    val entity = entities[index]
                    val key = keys[index]
                    entities[index] = entities[curPosition]
                    keys[index] = keys[curPosition]
                    entities[curPosition] = entity
                    keys[curPosition] = key
                    curPosition++
                }
            }
            entities[endPosition] = entities[curPosition]
            keys[endPosition] = keys[curPosition]
            entities[curPosition] = pivotEntity
            keys[curPosition] = pivotKey
            quicksortBelow(entities, keys, startPosition, curPosition - 1)
            quicksortBelow(entities, keys, curPosition + 1, endPosition)
        }
    }

    fun quicksortAbove(entities: Array<SceneGraphEntity?>, startPosition: Int, endPosition: Int) {
        quicksortAbove(entities, extractSortKeys(entities, endPosition + 1), startPosition, endPosition)
    }

    private fun quicksortAbove(
        entities: Array<SceneGraphEntity?>, keys: IntArray, startPosition: Int, endPosition: Int
    ) {
        if (startPosition < endPosition) {
            val center = (endPosition + startPosition) / 2
            var curPosition = startPosition
            val pivotEntity = entities[center]
            val pivotKey = keys[center]
            entities[center] = entities[endPosition]
            keys[center] = keys[endPosition]
            entities[endPosition] = pivotEntity
            keys[endPosition] = pivotKey
            val offset = pivotKey
            for (index in startPosition until endPosition) {
                if (keys[index] > (index and 0x1) + offset) {
                    val entity = entities[index]
                    val key = keys[index]
                    entities[index] = entities[curPosition]
                    keys[index] = keys[curPosition]
                    entities[curPosition] = entity
                    keys[curPosition] = key
                    curPosition++
                }
            }
            entities[endPosition] = entities[curPosition]
            keys[endPosition] = keys[curPosition]
            entities[curPosition] = pivotEntity
            keys[curPosition] = pivotKey
            quicksortAbove(entities, keys, startPosition, curPosition - 1)
            quicksortAbove(entities, keys, curPosition + 1, endPosition)
        }
    }

    fun addHdShadow(renderUpToPlane: Int) {
        var shadow: Shadow? = null
        for (plane in 1 until renderUpToPlane) {
            val ground = this.surfaceHeightmap[plane]
            if (ground != null) {
                for (y in 0 until this.sceneDimensionY) {
                    for (x in 0 until this.sceneDimensionX) {
                        shadow = ground.createShadowAt(x, y, shadow)
                        if (shadow != null) {
                            val scaledX = x shl this.tileUnits
                            val scaledY = y shl this.tileUnits
                            var lowerPlane = plane - 1
                            while (lowerPlane >= 0) {
                                val lowerGround = this.surfaceHeightmap[lowerPlane]
                                if (lowerGround != null) {
                                    val heightSw = ground.getHeight(x, y) - lowerGround.getHeight(x, y)
                                    val heightSe = ground.getHeight(x + 1, y) - lowerGround.getHeight(x + 1, y)
                                    val heightNe = ground.getHeight(x + 1, y + 1) - lowerGround.getHeight(x + 1, y + 1)
                                    val heightNw = ground.getHeight(x, y + 1) - lowerGround.getHeight(x, y + 1)
                                    lowerGround.addShadow(shadow, scaledX, (heightNw + heightNe + heightSe + heightSw) / 4, scaledY)
                                }
                                --lowerPlane
                            }
                        }
                    }
                }
            }
        }
    }

    fun getInteractiveObjects(plane: Int, x: Int, y: Int): InteractiveEntityContainer? {
        val tile = this.tiles[plane][x][y]
        return tile?.interactives
    }

    /**
     * Places any object/entity into the scene
     */
    fun placeSceneryObjects(entity: SceneGraphEntity, lights: Array<PointLight?>) {
        // Per-entity light selection feeds the fixed-function GL_LIGHT state, which a
        // shader toolkit doing its own per-model selection never reads — so under one
        // this is a per-entity, per-frame walk of the tile's light uids plus four
        // glLightfv/glLightf calls, all discarded.
        if (this.copyLights && this.activeToolkit.usesFixedFunctionLights()) {
            val lightCount = entity.getActiveLights(lights)
            this.activeToolkit.copyLights(lightCount, lights as Array<PointLight>)
        }
        if (this.underwaterHeightmap === this.localHeightmap) {
            val absolutePosition = entity.getAbsolutePosition().coords
            val sceneX: Int
            val sceneY: Int
            if (entity is AbstractInteractiveEntity) {
                sceneX = entity.startX.toInt()
                sceneY = entity.startZ.toInt()
            } else {
                sceneX = absolutePosition.x.toInt() shr this.tileUnits
                sceneY = absolutePosition.z.toInt() shr this.tileUnits
            }
            val waterTile = WaterTile()
            waterTile.color = getColor(sceneX, sceneY)
            waterTile.scale = getScale(sceneX, sceneY)
            waterTile.intensity = getIntensity(sceneX, sceneY)
            waterTile.floOpcode20 = getWaterOpcode20(sceneX, sceneY)
            waterTile.floUnusedOpcode21 = unusedOpcode21(sceneX, sceneY)
            waterTile.floUnusedOpcode22 = unusedOpcode22(sceneX, sceneY)
            this.activeToolkit.addWaterTile(this.surfaceHeightmap[0]!!.getAverageHeight(absolutePosition.x.toInt(), absolutePosition.z.toInt()), waterTile)
        }
        currentDrawEntity = entity
        if (DUMP_ALPHA && alphaPassActive && dumpAlphaTick in 120..121) {
            val abs = entity.getAbsolutePosition().coords
            val tx = abs.x.toInt() shr this.tileUnits
            val ty = abs.z.toInt() shr this.tileUnits
            if (Math.abs(tx - this.cameraGridPositionX) <= 6 && Math.abs(ty - this.cameraGridPositionY) <= 6) {
                System.err.println("[AlphaDump] ${entity.javaClass.simpleName} id=${(entity as? StationaryInteractiveObject)?.id ?: (entity as? com.jagex.game.runetek5.entity.gameobject.AnimatedInteractiveObject)?.id ?: -1} tile=($tx,$ty) plane=${entity.plane} depth=${entity.floorDepth} hasAlpha=${entity.hasAlpha()} retained=${entity.modernRetained}")
            }
        }
        val entityReference = entity.draw(this.activeToolkit)
        currentDrawEntity = null
        if (entityReference != null) {
            if (entityReference.interactive) {
                entityReference.entity = entity
                this.interactiveEntities.add(entityReference)
            } else {
                RenderedEntityCache.clear(entityReference)
            }
        }
    }

    fun getPrimaryWallDecoration(plane: Int, x: Int, y: Int): AbstractWallDecoration? {
        val tile = this.tiles[plane][x][y]
        return tile?.primaryWallDecoration
    }

    fun removeInteractiveOnCondition(plane: Int, x: Int, y: Int, comparable: ComparableGraphNode?): AbstractInteractiveEntity? {
        val tile = this.tiles[plane][x][y]
        if (tile != null) {
            var container = tile.interactives
            while (container != null) {
                val interactive = container.interactiveObject
                if ((comparable == null || comparable.matches(interactive!!)) && x == interactive!!.startX.toInt() && y == interactive!!.startZ.toInt()) {
                    removeInteractive(interactive, false)
                    return interactive
                }
                container = container.child
            }
        }
        return null
    }

    fun getIntensity(x: Int, y: Int): Int {
        return if (this.intensities != null) this.intensities!![x][y].toInt() and 0xff else 0
    }

    companion object {
        @JvmStatic var phaseCullNs = 0L
        @JvmStatic var phaseTilesNs = 0L
        @JvmStatic var phaseSamples = 0

        /** Scene-walk shape, per [ModernPerf] interval: nodes visited, tiles tested, entities drawn. */
        @JvmStatic var walkNodes = 0L
        @JvmStatic var walkTiles = 0L
        @JvmStatic var cullVerifyChecks = 0L
        @JvmStatic var cullVerifyBad = 0L
        @JvmStatic var planesDrawn = 0L
        @JvmStatic var planesSkipped = 0L
        @JvmStatic var walkDrawn = 0L
        @JvmStatic var walkAlpha = 0L
        @JvmStatic var walkCacheRebuildsAcc = 0

        /** Rebuilds since the previous call (the perf print reads it once per interval). */
        @JvmStatic fun walkCacheRebuildsTotal(): Int { val v = walkCacheRebuildsAcc; walkCacheRebuildsAcc = 0; return v }
        @JvmStatic var phaseAlphaNs = 0L
        @JvmStatic var phaseRetainedNs = 0L
        private val DUMP_ALPHA = System.getProperty("render.dumpalpha") != null
        private var dumpAlphaTick = 0
        /**
         * True while [drawTiles] is submitting the alpha bucket (the entities whose
         * hasAlpha() is set, drawn last and depth-sorted). Read by deferred toolkits
         * to route those draws into an ordered translucent pass.
         */
        @JvmStatic @Volatile var alphaPassActive = false
        /** Deferred-renderer takeover of static entities; null = legacy walk draws everything. */
        @JvmStatic var retainedHook: RetainedSceneHook? = null
        /** Entity whose draw() is executing (for renderers that need its floorDepth). */
        @JvmStatic var currentDrawEntity: SceneGraphEntity? = null
        @JvmStatic var phaseWalkNs = 0L
        @JvmStatic var phaseSortNs = 0L
        @JvmStatic var phaseFloorNs = 0L
        @JvmStatic var phaseDrawNs = 0L
        @JvmStatic
        fun setRoofOverride() {
            val roofToggle = PreferenceManager.preferences!!.removeRoofsOptionOverride!!.getValue()
            if (roofToggle == 0) {
                // Reset
                Resource.mapRegion!!.setActiveSettingsBits(null)
                initializeRoofSettings(0)
            } else if (roofToggle == 1) {
                // Always
                resetSettingsBits(0.toByte())
                initializeRoofSettings(512)
                if (Resource.mapRegion!!.getScene() != null) {
                    updateTileVisibility()
                }
            } else {
                // Selectively
                resetSettingsBits((Client.timesSceneRendered - 4 and 0xff).toByte())
                initializeRoofSettings(2)
            }

            Client.ROOF_OVERRIDE = PlayerUpdate.MY_PLAYER_PLANE
        }

        @JvmStatic
        fun updateTileVisibility() {
            var adjustmentCount = 0
            val scene = Resource.mapRegion!!.getScene()!!
            for (sceneX in 0 until Resource.mapRegion!!.sceneSizeX) {
                for (sceneY in 0 until Resource.mapRegion!!.sceneSizeY) {
                    if (MapRegion.calculateVisibilityAdjustments(scene!!.tiles as Array<Array<Array<Tile>?>?>?, adjustmentCount, sceneX, sceneY, true)) {
                        adjustmentCount++
                    }
                    if (adjustmentCount >= 512) {
                        return
                    }
                }
            }
        }

        @JvmStatic
        fun initializeRoofSettings(capacity: Int) {
            Client.visibleBelowDrawheight = IntArray(capacity)
            Client.visibleAboveX = IntArray(capacity)
            Client.visibleBelowX = IntArray(capacity)
            Client.visibleBelowY = IntArray(capacity)
            Client.visibleAboveY = IntArray(capacity)
        }

        @JvmStatic
        fun resetSettingsBits(defaultSettings: Byte) {
            var settingsBits = Resource.mapRegion!!.bits
            if (settingsBits == null) {
                settingsBits = Array(4) { Array(Resource.mapRegion!!.sceneSizeX) { ByteArray(Resource.mapRegion!!.sceneSizeY) } }
                Resource.mapRegion!!.setActiveSettingsBits(settingsBits)
            }
            for (plane in 0..3) {
                for (x in 0 until Resource.mapRegion!!.sceneSizeX) {
                    for (y in 0 until Resource.mapRegion!!.sceneSizeY) {
                        settingsBits[plane][x][y] = defaultSettings
                    }
                }
            }
        }
    }
}
