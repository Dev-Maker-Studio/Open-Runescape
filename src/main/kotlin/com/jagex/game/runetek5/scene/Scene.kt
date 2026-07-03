package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
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
    var adjacentTileOnScreen: Array<BooleanArray>
    var tileOnScreen: Array<BooleanArray>
    var adjacent_tile_on_screen: Array<BooleanArray>
    var anInt2618: Int = 5015
    var anInt2619: Int = 5040
    var anInt2622: Int = 16
    var anInt2634: Int = 10072
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
    val renderObjects3d: Array<SceneGraphEntity?>
    val renderObjects2d: Array<SceneGraphEntity?>
    val dynamicRenderObjects: Array<SceneGraphEntity?>

    /**
     * Objects of which face_render_priorities
     * can be disregarded
     * (Could be wrong, could be: opaque_graph_node)
     * graph_nodes_3d before
     */
    val depthRenderedEntities: Array<SceneGraphEntity?>

    /**
     * Objects that need to have their face_render_priority
     * taken into account for correct rendering
     * (could be wrong, could be: transparent_graph_node)
     */
    val priorityRenderedEntities: Array<SceneGraphEntity?>
    val interactiveObjects: Array<AbstractInteractiveEntity?>

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
        this.renderObjects3d = arrayOfNulls(2)
        this.renderObjects2d = arrayOfNulls(2)
        this.dynamicRenderObjects = arrayOfNulls(2)
        this.depthRenderedEntities = arrayOfNulls(10072)
        this.depthObjectCount = 0
        this.priorityRenderedEntities = arrayOfNulls(5015)
        this.layeredObjectCount = 0
        this.interactiveObjects = arrayOfNulls(5040)
        this.interactiveObjectCount = 0
        this.adjacent_tile_on_screen = Array(visibleTiles + visibleTiles + 1) { BooleanArray(visibleTiles + visibleTiles + 1) }
        this.tileOnScreen = Array(visibleTiles + visibleTiles + 2) { BooleanArray(visibleTiles + visibleTiles + 2) }
        this.adjacentTileOnScreen = this.adjacent_tile_on_screen
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

    fun getWall(z: Int, x: Int, y: Int): AbstractWall? {
        val tile = this.tiles[z][x][y]
        return tile?.primaryWall
    }

    fun linkBelow(x: Int, y: Int) {
        val baseFloorTile = this.tiles[0][x][y]
        for (plane in 0..2) {
            val tileNextLvl = this.tiles[plane + 1][x][y]
            this.tiles[plane][x][y] = tileNextLvl
            if (tileNextLvl != null) {
                var wrapper = tileNextLvl.interactives
                while (wrapper != null) {
                    val interactive = wrapper.interactiveObject
                    if (interactive!!.startX.toInt() == x && y == interactive!!.startZ.toInt()) {
                        interactive.plane = (interactive.plane - 1).toByte()
                    }
                    wrapper = wrapper.child
                }
                if (tileNextLvl.groundDecoration != null) {
                    tileNextLvl.groundDecoration!!.plane = (tileNextLvl.groundDecoration!!.plane - 1).toByte()
                }
                if (tileNextLvl.primaryWall != null) {
                    tileNextLvl.primaryWall!!.plane = (tileNextLvl.primaryWall!!.plane - 1).toByte()
                }
                if (tileNextLvl.secondaryWall != null) {
                    tileNextLvl.secondaryWall!!.plane = (tileNextLvl.secondaryWall!!.plane - 1).toByte()
                }
                if (tileNextLvl.primaryWallDecoration != null) {
                    tileNextLvl.primaryWallDecoration!!.plane = (tileNextLvl.primaryWallDecoration!!.plane - 1).toByte()
                }
                if (tileNextLvl.secondaryWallDecoration != null) {
                    tileNextLvl.secondaryWallDecoration!!.plane = (tileNextLvl.secondaryWallDecoration!!.plane - 1).toByte()
                }
            }
        }
        if (this.tiles[0][x][y] == null) {
            this.tiles[0][x][y] = Tile(0)
            this.tiles[0][x][y]!!.plane = 1
        }
        this.tiles[0][x][y]!!.bridge = baseFloorTile
        this.tiles[3][x][y] = null
    }

    fun loadTiles(z: Int, x: Int, y: Int) {
        val hasBridge = this.tiles[0][x][y] != null && this.tiles[0][x][y]!!.bridge != null
        var currentZ = z
        while (currentZ >= 0) {
            if (this.tiles[currentZ][x][y] == null) {
                val tile = Tile(currentZ)
                this.tiles[currentZ][x][y] = tile
                if (hasBridge) {
                    tile.plane++
                }
            }
            --currentZ
        }
    }

    fun setLocalGround(z: Int, ground: Ground?) {
        this.localHeightmap[z] = ground
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

    fun addWater(x: Int, y: Int, waterColor: Int, waterScale: Int, waterIntensity: Int, opcode20: Int, opcode21: Int, opcode22: Int) {
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
            this.opcode20!![x][y] = opcode20.toByte()
        }
        if (this.opcode21 != null) {
            this.opcode21!![x][y] = opcode21.toByte()
        }
        if (this.opcode22 != null) {
            this.opcode22!![x][y] = opcode22.toByte()
        }
    }

    fun addGroundDecoration(logicHeight: Int, baseX: Int, baseY: Int, groundDecoration: AbstractGroundDecoration?) {
        val tile = getTile(logicHeight, baseX, baseY)
        if (tile != null) {
            tile.groundDecoration = groundDecoration
            val type = if (this.localHeightmap === this.underwaterHeightmap) 1 else 0
            if (groundDecoration!!.disregardTextureMovement()) {
                if (groundDecoration.hasAlpha()) {
                    groundDecoration.child = this.renderObjects2d[type]
                    this.renderObjects2d[type] = groundDecoration
                } else {
                    groundDecoration.child = this.renderObjects3d[type]
                    this.renderObjects3d[type] = groundDecoration
                }
            } else {
                groundDecoration.child = this.dynamicRenderObjects[type]
                this.dynamicRenderObjects[type] = groundDecoration
            }
        }
    }

    fun addGroundItem(plane: Int, baseX: Int, baseY: Int, drawHeight: Int, groundItem: AbstractGroundItem?) {
        val tile = getTile(plane, baseX, baseY)
        if (tile != null) {
            groundItem!!.setBasePosition(Vector3(((baseX shl this.tileUnits) + this.halfTileOffset).toFloat(), drawHeight.toFloat(), ((baseY shl this.tileUnits) + this.halfTileOffset).toFloat()))
            tile.groundItem = groundItem
            val type = if (this.underwaterHeightmap === this.localHeightmap) 1 else 0
            if (groundItem.disregardTextureMovement()) {
                if (groundItem.hasAlpha()) {
                    groundItem.child = this.renderObjects2d[type]
                    this.renderObjects2d[type] = groundItem
                } else {
                    groundItem.child = this.renderObjects3d[type]
                    this.renderObjects3d[type] = groundItem
                }
            } else {
                groundItem.child = this.dynamicRenderObjects[type]
                this.dynamicRenderObjects[type] = groundItem
            }
        }
    }


    fun addWall(locHeight: Int, baseX: Int, baseY: Int, primaryWall: AbstractWall?, secondaryWall: AbstractWall?) {
        val tile = getTile(locHeight, baseX, baseY)
        if (tile != null) {
            tile.primaryWall = primaryWall
            tile.secondaryWall = secondaryWall
            val type = if (this.underwaterHeightmap === this.localHeightmap) 1 else 0
            if (primaryWall!!.disregardTextureMovement()) {
                if (primaryWall.hasAlpha()) {
                    primaryWall.child = this.renderObjects2d[type]
                    this.renderObjects2d[type] = primaryWall
                } else {
                    primaryWall.child = this.renderObjects3d[type]
                    this.renderObjects3d[type] = primaryWall
                }
            } else {
                primaryWall.child = this.dynamicRenderObjects[type]
                this.dynamicRenderObjects[type] = primaryWall
            }
            if (secondaryWall != null) {
                if (secondaryWall.disregardTextureMovement()) {
                    if (secondaryWall.hasAlpha()) {
                        secondaryWall.child = this.renderObjects2d[type]
                        this.renderObjects2d[type] = secondaryWall
                    } else {
                        secondaryWall.child = this.renderObjects3d[type]
                        this.renderObjects3d[type] = secondaryWall
                    }
                } else {
                    secondaryWall.child = this.dynamicRenderObjects[type]
                    this.dynamicRenderObjects[type] = secondaryWall
                }
            }
        }
    }

    fun addWallDecoration(wallPlane: Int, baseX: Int, baseY: Int, primaryWallDecoration: AbstractWallDecoration?, secondaryWallDecoration: AbstractWallDecoration?) {
        val tile = getTile(wallPlane, baseX, baseY)
        if (tile != null) {
            tile.primaryWallDecoration = primaryWallDecoration
            tile.secondaryWallDecoration = secondaryWallDecoration
            val renderType = if (this.underwaterHeightmap === this.localHeightmap) 1 else 0
            if (primaryWallDecoration!!.disregardTextureMovement()) {
                if (primaryWallDecoration.hasAlpha()) {
                    primaryWallDecoration.child = this.renderObjects2d[renderType]
                    this.renderObjects2d[renderType] = primaryWallDecoration
                } else {
                    primaryWallDecoration.child = this.renderObjects3d[renderType]
                    this.renderObjects3d[renderType] = primaryWallDecoration
                }
            } else {
                primaryWallDecoration.child = this.dynamicRenderObjects[renderType]
                this.dynamicRenderObjects[renderType] = primaryWallDecoration
            }
            if (secondaryWallDecoration != null) {
                if (secondaryWallDecoration.disregardTextureMovement()) {
                    if (secondaryWallDecoration.hasAlpha()) {
                        secondaryWallDecoration.child = this.renderObjects2d[renderType]
                        this.renderObjects2d[renderType] = secondaryWallDecoration
                    } else {
                        secondaryWallDecoration.child = this.renderObjects3d[renderType]
                        this.renderObjects3d[renderType] = secondaryWallDecoration
                    }
                } else {
                    secondaryWallDecoration.child = this.dynamicRenderObjects[renderType]
                    this.dynamicRenderObjects[renderType] = secondaryWallDecoration
                }
            }
        }
    }

    fun addInteractive(node: AbstractInteractiveEntity, cached: Boolean): Boolean {
        val isUnderwater = this.localHeightmap === this.underwaterHeightmap
        var color = 0
        var scale: Short = 0
        var intensity: Byte = 0
        node.calculatePosition()
        if (node.startX >= 0 && node.startZ >= 0 && node.endX < this.sceneDimensionX && node.endZ < this.sceneDimensionY) {
            var floorOffsetY: Short = 0
            for (x in node.startX..node.endX) {
                for (y in node.startZ..node.endZ) {
                    val tile = getTile(node.plane.toInt(), x, y)
                    if (tile != null) {
                        val wrapper = InteractiveEntityContainer.add(node)
                        val emptyChild = tile.interactives
                        if (emptyChild == null) {
                            tile.interactives = wrapper
                        } else {
                            var current = emptyChild
                            while (current!!.child != null) {
                                current = current!!.child!!!!
                            }
                            current.child = wrapper
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
                for (x in node.startX..node.endX) {
                    for (y in node.startZ..node.endZ) {
                        if ((this.colors!![x][y] and 0xffffff.inv()) == 0) {
                            this.colors!![x][y] = color
                            this.scales!![x][y] = scale
                            this.intensities!![x][y] = intensity
                        }
                    }
                }
            }
            if (cached) {
                this.interactiveObjects[++this.interactiveObjectCount - 1] = node
                node.scene = this
            } else {
                val nodePointer = if (this.underwaterHeightmap === this.localHeightmap) 1 else 0
                if (node.disregardTextureMovement()) {
                    if (node.hasAlpha()) {
                        node.child = this.renderObjects2d[nodePointer]
                        this.renderObjects2d[nodePointer] = node
                    } else {
                        node.child = this.renderObjects3d[nodePointer]
                        this.renderObjects3d[nodePointer] = node
                    }
                } else {
                    node.child = this.dynamicRenderObjects[nodePointer]
                    this.dynamicRenderObjects[nodePointer] = node
                }
            }
            if (cached) {
                val absolutePosition = Vector3Pool.vector3From(node.getAbsolutePosition().coords)
                absolutePosition.y -= floorOffsetY.toFloat()
                node.setBasePosition(absolutePosition)
                absolutePosition.pushVectorStack()
            }
            return true
        }
        return false
    }

    fun offsetWallDecoration(z: Int, x: Int, y: Int, displacement: Int) {
        val tile = this.tiles[z][x][y]
        if (tile != null) {
            val first = tile.primaryWallDecoration
            val second = tile.secondaryWallDecoration
            /** The upscaling happening below equals to 9 - 7 = 2, to match the model upscaling **/
            if (first != null) {
                first.positionX = (displacement * first.positionX / (16 shl this.tileUnits - 7)).toShort()
                first.positionY = (displacement * first.positionY / (16 shl this.tileUnits - 7)).toShort()
            }
            if (second != null) {
                second.positionX = (displacement * second.positionX / (16 shl this.tileUnits - 7)).toShort()
                second.positionY = (displacement * second.positionY / (16 shl this.tileUnits - 7)).toShort()
            }
        }
    }

    fun clearInteractives() {
        for (index in 0 until this.interactiveObjectCount) {
            val inteactive = this.interactiveObjects[index]
            removeInteractive(inteactive!!, true)
            this.interactiveObjects[index] = null
        }
        this.interactiveObjectCount = 0
    }

    fun applyShading() {
        for (z in 0 until this.regionSizeZ) {
            for (x in 0 until this.sceneDimensionX) {
                for (y in 0 until this.sceneDimensionY) {
                    val tile = this.tiles[z][x][y]
                    if (tile != null) {
                        val primaryWall = tile.primaryWall
                        val secondaryWall = tile.secondaryWall
                        if (primaryWall != null && primaryWall.hasTileShadow()) {
                            castShadow(primaryWall, z, x, y, 1, 1)
                            if (secondaryWall != null && secondaryWall.hasTileShadow()) {
                                castShadow(secondaryWall, z, x, y, 1, 1)
                                secondaryWall.castShadow(primaryWall, 0, 0, 0, false)
                                secondaryWall.removeTileShadow()
                            }
                            primaryWall.removeTileShadow()
                        }
                        var interactiveWrapper = tile.interactives
                        while (interactiveWrapper != null) {
                            val interactive = interactiveWrapper.interactiveObject
                            if (interactive != null && interactive.hasTileShadow()) {
                                castShadow(interactive, z, x, y, interactive.endX - interactive.startX + 1, interactive.endZ - interactive.startZ + 1)
                                interactive.removeTileShadow()
                            }
                            interactiveWrapper = interactiveWrapper.child
                        }
                        val groundDecoration = tile.groundDecoration
                        if (groundDecoration != null && groundDecoration.hasTileShadow()) {
                            castShadow(groundDecoration, z, x, y)
                            groundDecoration.removeTileShadow()
                        }
                    }
                }
            }
        }
    }

    fun removePrimaryWallDecoration(z: Int, x: Int, y: Int): AbstractWallDecoration? {
        val tile = this.tiles[z][x][y]
        if (tile != null) {
            pushIntoStack(tile.primaryWallDecoration)
            if (tile.primaryWallDecoration != null) {
                val wallDecoration = tile.primaryWallDecoration
                tile.primaryWallDecoration = null
                return wallDecoration
            }
        }
        return null
    }

    fun removeGroundDecoration(z: Int, x: Int, y: Int): AbstractGroundDecoration? {
        val tile = this.tiles[z][x][y]
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

    fun removeGroundItem(z: Int, x: Int, y: Int): AbstractGroundItem? {
        val tile = this.tiles[z][x][y]
        if (tile == null) {
            return null
        }
        val groundItem = tile.groundItem
        tile.groundItem = null
        pushIntoStack(groundItem)
        return groundItem
    }

    fun removeInteractive(interactive: AbstractInteractiveEntity, clearFully: Boolean) {
        for (x in interactive.startX..interactive.endX) {
            for (y in interactive.startZ..interactive.endZ) {
                val tile = this.tiles[interactive.plane.toInt()][x][y]
                if (tile != null) {
                    var interactives = tile.interactives
                    var child: InteractiveEntityContainer? = null
                    while (interactives != null) {
                        if (interactive === interactives.interactiveObject) {
                            if (child != null) {
                                child.child = interactives.child
                            } else {
                                tile.interactives = interactives.child
                            }
                            interactives.release()
                            break
                        }
                        child = interactives
                        interactives = interactives.child
                    }
                }
            }
        }
        if (!clearFully) {
            pushIntoStack(interactive)
        }
    }

    fun pushIntoStack(renderedNode: SceneGraphEntity?) {
        if (renderedNode != null) {
            renderedNode.detach()
            for (index in 0..1) {
                var childNode: SceneGraphEntity? = null
                var primaryNode: SceneGraphEntity? = this.renderObjects3d[index]
                while (primaryNode != null) {
                    if (primaryNode === renderedNode) {
                        if (childNode != null) {
                            childNode.child = primaryNode.child
                        } else {
                            this.renderObjects3d[index] = primaryNode.child
                        }
                        return
                    }
                    childNode = primaryNode
                    primaryNode = primaryNode.child
                }
                childNode = null
                primaryNode = this.renderObjects2d[index]
                while (primaryNode != null) {
                    if (primaryNode === renderedNode) {
                        if (childNode != null) {
                            childNode.child = primaryNode.child
                        } else {
                            this.renderObjects2d[index] = primaryNode.child
                        }
                        return
                    }
                    childNode = primaryNode
                    primaryNode = primaryNode.child
                }
                childNode = null
                primaryNode = this.dynamicRenderObjects[index]
                while (primaryNode != null) {
                    if (primaryNode === renderedNode) {
                        if (childNode != null) {
                            childNode.child = primaryNode.child
                        } else {
                            this.dynamicRenderObjects[index] = primaryNode.child
                        }
                        return
                    }
                    childNode = primaryNode
                    primaryNode = primaryNode.child
                }
            }
        }
    }

    fun getSecondaryWall(z: Int, x: Int, y: Int): AbstractWall? {
        val tile = this.tiles[z][x][y]
        return tile?.secondaryWall
    }

    fun getInteractiveObject(z: Int, x: Int, y: Int, comparable: ComparableGraphNode?): AbstractInteractiveEntity? {
        val tile = this.tiles[z][x][y]
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

    fun getGroundDecoration(z: Int, x: Int, y: Int): AbstractGroundDecoration? {
        val tile = this.tiles[z][x][y]
        return if (tile != null && tile.groundDecoration != null) tile.groundDecoration else null
    }

    fun castShadow(graphEntity: SceneGraphEntity, sceneZ: Int, sceneX: Int, sceneY: Int) {
        if (sceneX < this.sceneDimensionX) {
            val tile = this.tiles[sceneZ][sceneX + 1][sceneY]
            if (tile != null && tile.groundDecoration != null && tile.groundDecoration!!.hasTileShadow()) {
                graphEntity.castShadow((tile.groundDecoration as SceneGraphEntity), this.fullTileDimension, 0, 0, true)
            }
        }
        if (sceneY < this.sceneDimensionX) {
            val tile = this.tiles[sceneZ][sceneX][sceneY + 1]
            if (tile != null && tile.groundDecoration != null && tile.groundDecoration!!.hasTileShadow()) {
                graphEntity.castShadow((tile.groundDecoration as SceneGraphEntity), 0, 0, this.fullTileDimension, true)
            }
        }
        if (sceneX < this.sceneDimensionX && sceneY < this.sceneDimensionY) {
            val tile = this.tiles[sceneZ][sceneX + 1][sceneY + 1]
            if (tile != null && tile.groundDecoration != null && tile.groundDecoration!!.hasTileShadow()) {
                graphEntity.castShadow((tile.groundDecoration as SceneGraphEntity), this.fullTileDimension, 0, this.fullTileDimension, true)
            }
        }
        if (sceneX < this.sceneDimensionX && sceneY > 0) {
            val tile = this.tiles[sceneZ][sceneX + 1][sceneY - 1]
            if (tile != null && tile.groundDecoration != null && tile.groundDecoration!!.hasTileShadow()) {
                graphEntity.castShadow((tile.groundDecoration as SceneGraphEntity), this.fullTileDimension, 0, -this.fullTileDimension, true)
            }
        }
    }

    /**
     * Adjusts the depth of the object (perspective_z)
     * to prevent the floor rendering over an object
     */
    fun separateFromFloor(graphNode: SceneGraphEntity) {
        val absolutePosition = graphNode.getAbsolutePosition().coords
        this.activeToolkit.calculateProjection(absolutePosition.x.toInt().toFloat(), (absolutePosition.y.toInt() + (graphNode.getMinY() shr 1)).toFloat(), absolutePosition.z.toInt().toFloat(), this.renderedGraphNodePosition)
        graphNode.floorDepth = this.renderedGraphNodePosition[2].toInt()
    }

    fun removePrimaryWall(z: Int, x: Int, y: Int): AbstractWall? {
        val tile = this.tiles[z][x][y]
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
    fun hideGameobject(graphNode: SceneGraphEntity, hideInteractives: Boolean, tileSettingBits: Array<Array<ByteArray>>?, locPlane: Int, roofPreference: Byte): Boolean {
        if (!this.calculateTilesOnScreen) {
            return false
        }
        if (graphNode is AbstractInteractiveEntity) {
            val interactive = graphNode
            val endX = interactive.endX
            val endY = interactive.endZ
            val startX = interactive.startX
            val startY = interactive.startZ
            for (x in startX..endX) {
                for (y in startY..endY) {
                    if (graphNode.collisionPlane < this.regionSizeZ && x >= this.visibleToXnegative && x < this.visibleToXpositive && y >= this.visibleToYnegative && y < this.visibleToYpositive) {
                        if ((tileSettingBits == null || graphNode.plane.toInt() < locPlane || roofPreference != tileSettingBits[graphNode.plane.toInt()][x][y])
                                && graphNode.canRender() && !graphNode.isSoftHidden(this.activeToolkit)) {
                            return false
                        }
                        if (!hideInteractives && x >= this.cameraGridPositionX - 16 && x <= this.cameraGridPositionX + 16
                                               && y >= this.cameraGridPositionY - 16 && y <= this.cameraGridPositionY + 16) {
                            graphNode.particleCycle(this.activeToolkit)
                        }
                        return true
                    }
                }
            }
            return true
        } else {
            val absolutePosition = graphNode.getAbsolutePosition().coords
            val sceneX = absolutePosition.x.toInt() shr this.tileUnits
            val sceneY = absolutePosition.z.toInt() shr this.tileUnits
            if (graphNode.collisionPlane < this.regionSizeZ && sceneX >= this.visibleToXnegative && sceneX < this.visibleToXpositive
                                                           && sceneY >= this.visibleToYnegative && sceneY < this.visibleToYpositive) {
                if ((tileSettingBits == null || graphNode.plane.toInt() < locPlane || roofPreference != tileSettingBits[graphNode.plane.toInt()][sceneX][sceneY])
                        && graphNode.canRender() && !graphNode.isSoftHidden(this.activeToolkit)) {
                    return false
                }
                if (!hideInteractives && sceneX >= this.cameraGridPositionX - 16 && sceneX <= this.cameraGridPositionX + 16
                                       && sceneY >= this.cameraGridPositionY - 16 && sceneY <= this.cameraGridPositionY + 16) {
                    graphNode.particleCycle(this.activeToolkit)
                }
                return true
            }
            return true
        }
    }

    fun renderShadows(shadow: Shadow, highestDisplayedPlane: Int, x: Int, y: Int, shadowedTiles: BooleanArray?): Boolean {
        var shadowsAdded = false
        if (this.localHeightmap !== this.underwaterHeightmap) {
            val highestPlaneHeight = this.surfaceHeightmap[highestDisplayedPlane]!!.getAverageHeight(x, y)
            for (plane in 0..highestDisplayedPlane) {
                val ground = this.surfaceHeightmap[plane]
                if (ground != null) {
                    val heightDelta = highestPlaneHeight - ground.getAverageHeight(x, y)
                    if (shadowedTiles != null) {
                        shadowedTiles[plane] = ground.hasRenderingShadows(shadow, x, heightDelta, y)
                        if (!shadowedTiles[plane]) {
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

    fun removeShadow(shadow: Shadow, maxPlane: Int, x: Int, y: Int, castShadowAtPlane: BooleanArray?) {
        if (this.underwaterHeightmap !== this.localHeightmap) {
            val averageHeight = this.surfaceHeightmap[maxPlane]!!.getAverageHeight(x, y)
            for (plane in 0..maxPlane) {
                if (castShadowAtPlane == null || castShadowAtPlane[plane]) {
                    val ground = this.surfaceHeightmap[plane]
                    if (ground != null) {
                        ground.removeShadow(shadow, x, averageHeight - ground.getAverageHeight(x, y), y)
                    }
                }
            }
        }
    }

    fun resetSurfaceTiles() {
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
            var lightStartZ = positionedLight.level
            if (positionedLight.growsDownwards) {
                lightStartZ = 0
            }
            var lightEndZ = positionedLight.level
            if (positionedLight.growsUpwards) {
                lightEndZ = this.regionSizeZ - 1
            }
            for (z in lightStartZ..lightEndZ) {
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
                        val lightBaseUids = this.tileLightUids!![z][x][y]
                        if ((lightBaseUids and 0xffffL) == 0L) {
                            this.tileLightUids!![z][x][y] = lightBaseUids or this.lightSlot.toLong()
                        } else if ((lightBaseUids and 0xffff0000L) == 0L) {
                            this.tileLightUids!![z][x][y] = lightBaseUids or (this.lightSlot.toLong() shl 16)
                        } else if ((lightBaseUids and 0xffff00000000L) == 0L) {
                            this.tileLightUids!![z][x][y] = lightBaseUids or (this.lightSlot.toLong() shl 32)
                        } else if ((lightBaseUids and -281474976710656L) == 0L) {
                            this.tileLightUids!![z][x][y] = lightBaseUids or (this.lightSlot.toLong() shl 48)
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
                val lightSource = positionedLight.pointLight
                val z = positionedLight.level
                val offset = lightSource!!.getSize2d() - this.halfTileOffset
                val dimension = (offset * 2 shr this.tileUnits) + 1
                var Y = 0
                val lightDisplacement = IntArray(dimension * dimension)
                val deltaX = lightSource!!.getX() - offset shr this.tileUnits
                var sceneStartY = lightSource!!.getZ() - offset shr this.tileUnits
                var sceneEndY = lightSource!!.getZ() + offset shr this.tileUnits
                if (sceneStartY < 0) {
                    Y -= sceneStartY
                    sceneStartY = 0
                }
                if (sceneEndY >= this.sceneDimensionY) {
                    sceneEndY = this.sceneDimensionY - 1
                }
                for (y in sceneStartY..sceneEndY) {
                    val range = positionedLight.ranges[Y]
                    val sX = range.toInt() ushr 8
                    var X = sX + Y * dimension
                    var sceneStartX = (range.toInt() ushr 8) + deltaX
                    var sceneX = (range.toInt() and 0xff) + sceneStartX - 1
                    if (sceneStartX < 0) {
                        X -= sceneStartX
                        sceneStartX = 0
                    }
                    if (sceneX >= this.sceneDimensionX) {
                        sceneX = this.sceneDimensionX - 1
                    }
                    for (x in sceneStartX..sceneX) {
                        var displacement: Byte = 1
                        val interactive = getInteractiveObject(z, x, y, null)
                        if (interactive != null && interactive.rotation.toInt() != 0) {
                            var westWithinScene: Boolean
                            var eastWithinScene: Boolean
                            var bitpackedRange: Short
                            var startX: Int
                            var endX: Int
                            if (interactive.rotation.toInt() == 1) {
                                westWithinScene = x - 1 >= sceneStartX
                                eastWithinScene = x + 1 <= sceneX
                                if (!westWithinScene && y + 1 <= sceneEndY) {
                                    bitpackedRange = positionedLight.ranges[Y + 1]
                                    startX = (bitpackedRange.toInt() ushr 8) + deltaX
                                    endX = (bitpackedRange.toInt() and 0xff) + startX
                                    westWithinScene = x > startX && x < endX
                                }
                                if (!eastWithinScene && y - 1 >= sceneStartY) {
                                    bitpackedRange = positionedLight.ranges[Y - 1]
                                    startX = (bitpackedRange.toInt() ushr 8) + deltaX
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
                                eastWithinScene = x + 1 <= sceneX
                                if (!westWithinScene && y - 1 >= sceneStartY) {
                                    bitpackedRange = positionedLight.ranges[Y - 1]
                                    startX = (bitpackedRange.toInt() ushr 8) + deltaX
                                    endX = (bitpackedRange.toInt() and 0xff) + startX
                                    westWithinScene = x > startX && x < endX
                                }
                                if (!eastWithinScene && y + 1 <= sceneEndY) {
                                    bitpackedRange = positionedLight.ranges[Y + 1]
                                    startX = (bitpackedRange.toInt() ushr 8) + deltaX
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
                        lightDisplacement[X++] = displacement.toInt()
                    }
                    ++Y
                }
                this.lightSlotAvailable!![slotIndex] = true
                this.localHeightmap[z]!!.addLight(lightSource, lightDisplacement)
            }
        }
    }

    fun getWaterOpcode20(x: Int, y: Int): Int {
        return if (this.opcode20 != null) this.opcode20!![x][y].toInt() and 0xff else 0
    }

    fun getGroundItem(z: Int, x: Int, y: Int): AbstractGroundItem? {
        val tile = this.tiles[z][x][y]
        return tile?.groundItem
    }

    fun removeSecondaryWallDecoration(z: Int, x: Int, y: Int): AbstractWallDecoration? {
        val tile = this.tiles[z][x][y]
        if (tile != null) {
            pushIntoStack(tile.secondaryWallDecoration)
            if (tile.secondaryWallDecoration != null) {
                val wallDecoration = tile.secondaryWallDecoration
                tile.secondaryWallDecoration = null
                return wallDecoration
            }
        }
        return null
    }

    fun drawTiles(hideInteratives: Boolean, settingBits: Array<Array<ByteArray>>?, lowestRenderedPlane: Int, roofPreference: Byte) {
        val graphNodeIndex = if (hideInteratives) 1 else 0
        this.depthObjectCount = 0
        this.layeredObjectCount = 0
        this.tileDrawCycle++
        var graphNode = this.renderObjects3d[graphNodeIndex]
        while (graphNode != null) {
            separateFromFloor(graphNode)
            if (graphNode.floorDepth != -1 && !hideGameobject(graphNode, hideInteratives, settingBits, lowestRenderedPlane, roofPreference)) {
                this.depthRenderedEntities[++this.depthObjectCount - 1] = graphNode
            }
            graphNode = graphNode.child
        }
        graphNode = this.renderObjects2d[graphNodeIndex]
        while (graphNode != null) {
            separateFromFloor(graphNode)
            if (graphNode.floorDepth != -1 && !hideGameobject(graphNode, hideInteratives, settingBits, lowestRenderedPlane, roofPreference)) {
                this.priorityRenderedEntities[++this.layeredObjectCount - 1] = graphNode
            }
            graphNode = graphNode.child
        }
        graphNode = this.dynamicRenderObjects[graphNodeIndex]
        while (graphNode != null) {
            separateFromFloor(graphNode)
            if (graphNode.floorDepth != -1 && !hideGameobject(graphNode, hideInteratives, settingBits, lowestRenderedPlane, roofPreference)) {
                if (graphNode.hasAlpha()) {
                    this.priorityRenderedEntities[++this.layeredObjectCount - 1] = graphNode
                } else {
                    this.depthRenderedEntities[++this.depthObjectCount - 1] = graphNode
                }
            }
            graphNode = graphNode.child
        }
        if (!hideInteratives) {
            for (interactive in 0 until this.interactiveObjectCount) {
                separateFromFloor(this.interactiveObjects[interactive]!!)
                if (this.interactiveObjects[interactive]!!.floorDepth != -1 && !hideGameobject(this.interactiveObjects[interactive]!!, false, settingBits, lowestRenderedPlane, roofPreference)) {
                    if (this.interactiveObjects[interactive]!!.hasAlpha()) {
                        this.priorityRenderedEntities[++this.layeredObjectCount - 1] = this.interactiveObjects[interactive]
                    } else {
                        this.depthRenderedEntities[++this.depthObjectCount - 1] = this.interactiveObjects[interactive]
                    }
                }
            }
        }
        /**
         * Renders general scene objects, walls
         */
        if (this.depthObjectCount > 0) {
            quicksortBelow(this.depthRenderedEntities, 0, this.depthObjectCount - 1)
            for (objectIndex in 0 until this.depthObjectCount) {
                placeSceneryObjects(this.depthRenderedEntities[objectIndex]!!, this.activeLights)
            }
        }
        if (this.copyLights) {
            this.activeToolkit.copyLights(0, null as Array<PointLight>?)
        }
        for (z in 0 until this.regionSizeZ) {
            if (z >= lowestRenderedPlane && settingBits != null) {
                var visibleSizeX = this.adjacent_tile_on_screen.size
                if (this.adjacent_tile_on_screen.size + this.visibleToXnegative > this.sceneDimensionX) {
                    visibleSizeX -= this.adjacent_tile_on_screen.size + this.visibleToXnegative - this.sceneDimensionX
                }
                var visibleSizeY = this.adjacent_tile_on_screen[0].size
                if (this.adjacent_tile_on_screen[0].size + this.visibleToYnegative > this.sceneDimensionY) {
                    visibleSizeY -= this.adjacent_tile_on_screen[0].size + this.visibleToYnegative - this.sceneDimensionY
                }
                if (this.calculateTilesOnScreen) {
                    for (tileX in this.visibleToInvertedXnegative until visibleSizeX) {
                        val x = tileX + this.visibleToXnegative - this.visibleToInvertedXnegative
                        for (tileY in this.visibleToInvertedYnegative until visibleSizeY) {
                            this.tileOnScreen[tileX][tileY] = false
                            if (this.adjacent_tile_on_screen[tileX][tileY]) {
                                val y = tileY + this.visibleToYnegative - this.visibleToInvertedYnegative
                                var descendingPlane = z
                                while (descendingPlane >= 0) {
                                    if (this.tiles[descendingPlane][x][y] != null && this.tiles[descendingPlane][x][y]!!.plane.toInt() == z) {
                                        if ((descendingPlane < lowestRenderedPlane || settingBits[descendingPlane][x][y] != roofPreference) && !this.culling.isTileHidden(z, x, y)) {
                                            this.tileOnScreen[tileX][tileY] = true
                                            break
                                        }
                                        this.tileOnScreen[tileX][tileY] = false
                                        break
                                    }
                                    --descendingPlane
                                }
                            }
                        }
                    }
                }
                this.localHeightmap[z]!!.renderFloor(this.cameraGridPositionX, this.cameraGridPositionY, this.visibleTiles, this.tileOnScreen, false)
            } else {
                var adjacentTileRowCount = this.adjacent_tile_on_screen.size
                if (this.adjacent_tile_on_screen.size + this.visibleToXnegative > this.sceneDimensionX) {
                    adjacentTileRowCount -= this.adjacent_tile_on_screen.size + this.visibleToXnegative - this.sceneDimensionX
                }
                var adjacentTileColumnCount = this.adjacent_tile_on_screen[0].size
                if (this.adjacent_tile_on_screen[0].size + this.visibleToYnegative > this.sceneDimensionY) {
                    adjacentTileColumnCount -= this.adjacent_tile_on_screen[0].size + this.visibleToYnegative - this.sceneDimensionY
                }
                if (this.calculateTilesOnScreen) {
                    for (tileX in this.visibleToInvertedXnegative until adjacentTileRowCount) {
                        val adjustedX = tileX + this.visibleToXnegative - this.visibleToInvertedXnegative
                        for (tileY in this.visibleToInvertedYnegative until adjacentTileColumnCount) {
                            this.tileOnScreen[tileX][tileY] = this.adjacent_tile_on_screen[tileX][tileY] && !this.culling.isTileHidden(z, adjustedX, tileY + this.visibleToYnegative - this.visibleToInvertedYnegative)
                        }
                    }
                }
                this.localHeightmap[z]!!.renderFloor(this.cameraGridPositionX, this.cameraGridPositionY, this.visibleTiles, this.tileOnScreen, true)
            }
        }
        /**
         * Renders ground decoration, light rays, objects with particles, roof / next level decoration
         */
        if (this.layeredObjectCount > 0) {
            quicksortAbove(this.priorityRenderedEntities, 0, this.layeredObjectCount - 1)
            for (index in 0 until this.layeredObjectCount) {
                placeSceneryObjects(this.priorityRenderedEntities[index]!!, this.activeLights)
            }
        }
    }

    fun drawScene(rotation: Int, cameraX: Int, cameraZ: Int, caneraY: Int, bits: Array<Array<ByteArray>>?,
                          visibleBelowDrawheight: IntArray?, visibleAboveX: IntArray?, visibleBelowX: IntArray?,
                          visibleBelowY: IntArray?, visibleAboveY: IntArray?, nextPlane: Int, roofPreference: Byte,
                          disableFlickeringEffects: Boolean) {
        this.culling.drawingScene = true
        this.calculateTilesOnScreen = true
        this.cameraGridPositionX = cameraX shr this.tileUnits
        this.cameraGridPositionY = caneraY shr this.tileUnits
        this.cameraPositionX = cameraX
        this.cameraPositionZ = caneraY
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
        val unadjustedAdjacentTilesOnScreen = this.adjacent_tile_on_screen
        val unadjustedTilesOnScreen = this.tileOnScreen
        for (x in 0 until this.visibleTiles + this.visibleTiles + 2) {
            var previousOnScreen = 0
            var currentOnScreen = 0
            for (y in 0 until this.visibleTiles + this.visibleTiles + 2) {
                if (y > 1) {
                    this.tileVisibility[y - 2] = previousOnScreen
                }
                previousOnScreen = currentOnScreen
                val sceneX = x + (this.cameraGridPositionX - this.visibleTiles)
                val sceneY = y + (this.cameraGridPositionY - this.visibleTiles)
                if (sceneX >= 0 && sceneY >= 0 && sceneX < this.sceneDimensionX && sceneY < this.sceneDimensionY) {
                    val upscaledSceneX = sceneX shl this.tileUnits
                    val upscaledSceneY = sceneY shl this.tileUnits
                    val surfaceHeight = this.surfaceHeightmap[this.surfaceHeightmap.size - 1]!!.getHeight(sceneX, sceneY) - (1000 shl this.tileUnits - 7)
                    val waterHeight = (if (this.underwaterHeightmap != null) this.underwaterHeightmap!![0]!!.getHeight(sceneX, sceneY) + this.fullTileDimension else this.surfaceHeightmap[0]!!.getHeight(sceneX, sceneY) + this.fullTileDimension) + (1000 shl this.tileUnits - 7)
                    currentOnScreen = this.activeToolkit.isOnScreen(upscaledSceneX, surfaceHeight, upscaledSceneY, upscaledSceneX, waterHeight, upscaledSceneY)
                    this.tileOnScreen[x][y] = currentOnScreen == 0
                } else {
                    currentOnScreen = -1
                    this.tileOnScreen[x][y] = false
                }
                if (x > 0 && y > 0) {
                    val isOnscreen = this.tileVisibility[y - 1] and this.tileVisibility[y] and previousOnScreen and currentOnScreen
                    this.adjacent_tile_on_screen[x - 1][y - 1] = isOnscreen == 0
                }
            }
            this.tileVisibility[this.visibleTiles + this.visibleTiles] = previousOnScreen
            this.tileVisibility[this.visibleTiles + this.visibleTiles + 1] = currentOnScreen
        }
        this.culling.visibleBelowDrawheight = visibleBelowDrawheight
        this.culling.visibleAboveX = visibleAboveX
        this.culling.visibleBelowX = visibleBelowX
        this.culling.visibleBelowY = visibleBelowY
        this.culling.visibleAboveY = visibleAboveY
        this.culling.processCulling(this.activeToolkit, nextPlane)
        ParticleSystemManager.processParticleCycle(this, this.activeToolkit)
        if (!this.interactiveEntities.removeDuplicates) {
            val interactivesIterator = this.interactiveEntities.entities.iterator()
            while (interactivesIterator.hasNext()) {
                val reference = interactivesIterator.next()
                interactivesIterator.remove()
                RenderedEntityCache.clear(reference)
            }
        }
        if (this.copyLights) {
            for (slot in 0 until this.lightSlot) {
                this.lightSlotOccupant!![slot]!!.applyEffect(rotation, disableFlickeringEffects)
            }
        }
        if (this.waterTiles != null) {
            setUnderwater(true)
            this.activeToolkit.addWater(WaterTile())
            drawTiles(true, bits, nextPlane, roofPreference)
            this.activeToolkit.onTilesDrawn()
            setUnderwater(false)
        }
        drawTiles(false, bits, nextPlane, roofPreference)
        if (!this.calculateTilesOnScreen) {
            this.adjacent_tile_on_screen = unadjustedAdjacentTilesOnScreen
            this.tileOnScreen = unadjustedTilesOnScreen
        }
    }

    fun castShadow(rendered: SceneGraphEntity, lowestZ: Int, startX: Int, startY: Int, width: Int, length: Int) {
        var isSamePlane = true
        val lowestX = startX
        val highestX = startX + width
        val lowestY = startY - 1
        val highestY = startY + length
        for (z in lowestZ..lowestZ + 1) {
            if (z != this.regionSizeZ) {
                for (x in lowestX..highestX) {
                    if (x >= 0 && x < this.sceneDimensionX) {
                        for (y in lowestY..highestY) {
                            if (y >= 0 && y < this.sceneDimensionY && (!isSamePlane || x >= highestX || y >= highestY || y < startY && startX != x)) {
                                val tile = this.tiles[z][x][y]
                                if (tile != null) {
                                    val tileheightDifference =
                                            (this.localHeightmap[z]!!.getHeight(x, y) + this.localHeightmap[z]!!.getHeight(1 + x, y)
                                            + this.localHeightmap[z]!!.getHeight(x, 1 + y) + this.localHeightmap[z]!!.getHeight(1 + x, 1 + y)) / 4 -
                                            (this.localHeightmap[lowestZ]!!.getHeight(startX, startY) + this.localHeightmap[lowestZ]!!.getHeight(startX + 1, startY)
                                            + this.localHeightmap[lowestZ]!!.getHeight(startX, startY + 1) + this.localHeightmap[lowestZ]!!.getHeight(startX + 1, startY + 1)) / 4
                                    val primaryWall = tile.primaryWall
                                    val secondaryWall = tile.secondaryWall
                                    if (primaryWall != null && primaryWall.hasTileShadow()) {
                                        rendered.castShadow(primaryWall,
                                                (x - startX) * this.fullTileDimension + (1 - width) * this.halfTileOffset, tileheightDifference,
                                                (y - startY) * this.fullTileDimension + (1 - length) * this.halfTileOffset, isSamePlane)
                                    }
                                    if (secondaryWall != null && secondaryWall.hasTileShadow()) {
                                        rendered.castShadow(secondaryWall,
                                                (x - startX) * this.fullTileDimension + (1 - width) * this.halfTileOffset, tileheightDifference,
                                                (y - startY) * this.fullTileDimension + (1 - length) * this.halfTileOffset, isSamePlane)
                                    }
                                    var interactives = tile.interactives
                                    while (interactives != null) {
                                        val interactive = interactives.interactiveObject
                                        if (interactive != null && interactive.hasTileShadow() && (x == interactive.startX.toInt() || lowestX == x) && (y == interactive.startZ.toInt() || lowestY == y)) {
                                            val sizeX = interactive.endX - interactive.startX + 1
                                            val sizeZ = interactive.endZ - interactive.startZ + 1
                                            rendered.castShadow(interactive,
                                                    this.fullTileDimension * (interactive.startX - startX) + this.halfTileOffset * (sizeX - width), tileheightDifference,
                                                    this.fullTileDimension * (interactive.startZ - startY) + this.halfTileOffset * (sizeZ - length), isSamePlane)
                                        }
                                        interactives = interactives.child
                                    }
                                }
                            }
                        }
                    }
                }
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

    fun removeSecondaryWall(z: Int, x: Int, y: Int): AbstractWall? {
        val tile = this.tiles[z][x][y]
        if (tile != null) {
            pushIntoStack(tile.secondaryWall)
            if (tile.secondaryWall != null) {
                val wall = tile.secondaryWall
                tile.secondaryWall = null
                return wall
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
    fun quicksortBelow(graphNodes: Array<SceneGraphEntity?>, startPosition: Int, endPosition: Int) {
        if (startPosition < endPosition) {
            val center = (endPosition + startPosition) / 2
            var curPosition = startPosition
            val graphNode = graphNodes[center]
            graphNodes[center] = graphNodes[endPosition]
            graphNodes[endPosition] = graphNode
            val offset = graphNode!!.floorDepth
            for (index in startPosition until endPosition) {
                if (graphNodes[index]!!.floorDepth < (index and 0x1) + offset) {
                    val node = graphNodes[index]
                    graphNodes[index] = graphNodes[curPosition]
                    graphNodes[curPosition++] = node
                }
            }
            graphNodes[endPosition] = graphNodes[curPosition]
            graphNodes[curPosition] = graphNode
            quicksortBelow(graphNodes, startPosition, curPosition - 1)
            quicksortBelow(graphNodes, curPosition + 1, endPosition)
        }
    }

    fun quicksortAbove(graphNodes: Array<SceneGraphEntity?>, startPosition: Int, endPosition: Int) {
        if (startPosition < endPosition) {
            val center = (endPosition + startPosition) / 2
            var curPosition = startPosition
            val graphNode = graphNodes[center]
            graphNodes[center] = graphNodes[endPosition]
            graphNodes[endPosition] = graphNode
            val offset = graphNode!!.floorDepth
            for (index in startPosition until endPosition) {
                if (graphNodes[index]!!.floorDepth > (index and 0x1) + offset) {
                    val node = graphNodes[index]
                    graphNodes[index] = graphNodes[curPosition]
                    graphNodes[curPosition++] = node
                }
            }
            graphNodes[endPosition] = graphNodes[curPosition]
            graphNodes[curPosition] = graphNode
            quicksortAbove(graphNodes, startPosition, curPosition - 1)
            quicksortAbove(graphNodes, curPosition + 1, endPosition)
        }
    }

    fun addHdShadow(renderUpToPlane: Int) {
        var shadow: Shadow? = null
        for (z in 1 until renderUpToPlane) {
            val ground = this.surfaceHeightmap[z]
            if (ground != null) {
                for (y in 0 until this.sceneDimensionY) {
                    for (x in 0 until this.sceneDimensionX) {
                        shadow = ground.createShadowAt(x, y, shadow)
                        if (shadow != null) {
                            val scaledX = x shl this.tileUnits
                            val scaledY = y shl this.tileUnits
                            var maxZ = z - 1
                            while (maxZ >= 0) {
                                val nextGround = this.surfaceHeightmap[maxZ]
                                if (nextGround != null) {
                                    val heightSw = ground.getHeight(x, y) - nextGround.getHeight(x, y)
                                    val heightSe = ground.getHeight(x + 1, y) - nextGround.getHeight(x + 1, y)
                                    val heightNe = ground.getHeight(x + 1, y + 1) - nextGround.getHeight(x + 1, y + 1)
                                    val heightNw = ground.getHeight(x, y + 1) - nextGround.getHeight(x, y + 1)
                                    nextGround.addShadow(shadow, scaledX, (heightNw + heightNe + heightSe + heightSw) / 4, scaledY)
                                }
                                --maxZ
                            }
                        }
                    }
                }
            }
        }
    }

    fun getInteractiveObjects(z: Int, x: Int, y: Int): InteractiveEntityContainer? {
        val tile = this.tiles[z][x][y]
        return tile?.interactives
    }

    /**
     * Places any object/entity into the scene
     */
    fun placeSceneryObjects(entity: SceneGraphEntity, lights: Array<PointLight?>) {
        if (this.copyLights) {
            val activeLights = entity.getActiveLights(lights)
            this.activeToolkit.copyLights(activeLights, lights as Array<PointLight>)
        }
        if (this.underwaterHeightmap === this.localHeightmap) {
            val absolutePosition = entity.getAbsolutePosition().coords
            val startX: Int
            val startY: Int
            if (entity is AbstractInteractiveEntity) {
                startX = entity.startX.toInt()
                startY = entity.startZ.toInt()
            } else {
                startX = absolutePosition.x.toInt() shr this.tileUnits
                startY = absolutePosition.z.toInt() shr this.tileUnits
            }
            val waterTile = WaterTile()
            waterTile.color = getColor(startX, startY)
            waterTile.scale = getScale(startX, startY)
            waterTile.intensity = getIntensity(startX, startY)
            waterTile.floOpcode20 = getWaterOpcode20(startX, startY)
            waterTile.floUnusedOpcode21 = unusedOpcode21(startX, startY)
            waterTile.floUnusedOpcode22 = unusedOpcode22(startX, startY)
            this.activeToolkit.addWaterTile(this.surfaceHeightmap[0]!!.getAverageHeight(absolutePosition.x.toInt(), absolutePosition.z.toInt()), waterTile)
        }
        val entityReference = entity.draw(this.activeToolkit)
        if (entityReference != null) {
            if (entityReference.interactive) {
                entityReference.entity = entity
                this.interactiveEntities.add(entityReference)
            } else {
                RenderedEntityCache.clear(entityReference)
            }
        }
    }

    fun getPrimaryWallDecoration(z: Int, x: Int, y: Int): AbstractWallDecoration? {
        val tile = this.tiles[z][x][y]
        return tile?.primaryWallDecoration
    }

    fun removeInteractiveOnCondition(z: Int, x: Int, y: Int, comparable: ComparableGraphNode?): AbstractInteractiveEntity? {
        val tile = this.tiles[z][x][y]
        if (tile != null) {
            var interactives = tile.interactives
            while (interactives != null) {
                val interactive = interactives.interactiveObject
                if ((comparable == null || comparable.matches(interactive!!)) && x == interactive!!.startX.toInt() && y == interactive!!.startZ.toInt()) {
                    removeInteractive(interactive, false)
                    return interactive
                }
                interactives = interactives.child
            }
        }
        return null
    }

    fun getIntensity(x: Int, y: Int): Int {
        return if (this.intensities != null) this.intensities!![x][y].toInt() and 0xff else 0
    }

    companion object {
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
                if (Resource.mapRegion!!.getScene()!! != null) {
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
