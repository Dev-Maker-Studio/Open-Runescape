package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.cache.node.Queue
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.JSObjectManager
import com.jagex.game.runetek5.client.ServerEnvironment
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.config.flo.FloTypeList
import com.jagex.game.runetek5.config.flu.FluTypeList
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.config.mapsprite.MapSpriteTypeList
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.mec.MECType
import com.jagex.game.runetek5.config.mec.MECTypeList
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.staticelements.StaticElements
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.DebugDisplay
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.ColorUtil
import com.jagex.game.runetek5.vardomain.VarDomain

object WorldMap {

    @JvmField
    var requestedMapZoom = 0f

    @JvmField
    var mapZoom = 0f

    @JvmField
    var mapSquareAbsX = 0

    @JvmField
    var mapSquareAbsY = 0

    @JvmField
    var staticElement: WorldMapElement? = null

    @JvmField
    var tileShapePatterns: Array<Array<ByteArray?>>? = null

    @JvmField
    var halfZoomLevel = 0

    @JvmField
    var staticElements: StaticElements? = null

    @JvmField
    var mapSizeX = 0

    @JvmField
    var mapSizeY = 0

    @JvmField
    var hueRandomisation = (Math.random() * 11.0).toInt() - 5

    @JvmField
    var lightnessRandomisation = (Math.random() * 17.0).toInt() - 8

    @JvmField
    val worldMapCategories = NodeCollection()

    @JvmField
    var MAP_AREA_RESOURCE: Js5ResourceProvider? = null

    @JvmField
    var LOCTYPE_LIST: ObjectTypeList? = null

    @JvmField
    var MECTYPE_LIST: MECTypeList? = null

    @JvmField
    var MAP_SPRITE_TYPE_LIST: MapSpriteTypeList? = null

    @JvmField
    var PLAYER_VAR_PROVIDER: VarDomain? = null

    @JvmField
    var tileViewStartX = 0

    @JvmField
    var tileViewEndY = 0

    @JvmField
    var tileViewEndX = 0

    @JvmField
    var tileViewStartY = 0

    @JvmField
    var pixelViewStartX = 0

    @JvmField
    var pixelViewStartY = 0

    @JvmField
    var pixelViewEndX = 0

    @JvmField
    var pixelViewEndY = 0

    @JvmField
    val WORLDMAP_DEFS = IterableNodeMap<WorldMapElement>(16)

    @JvmField
    val LOC_ON_TILE = IntArray(1)

    @JvmField
    val SHAPE_AND_ROTATION_ON_TILE = ByteArray(1)

    @JvmField
    var FLOTYPE_LIST: FloTypeList? = null

    @JvmField
    var FLUTYPE_LIST: FluTypeList? = null

    @JvmField
    var underlayIds: ByteArray? = null

    @JvmField
    var overlayIds: ByteArray? = null

    @JvmField
    var tileShapeAndRotations: ByteArray? = null

    @JvmField
    var tileDrawInfo: Array<Any?>? = null

    @JvmField
    var shapesAndRotations: ByteArray? = null

    @JvmField
    var worldMapTiles: Array<Array<Array<ArrayList<TileOnWorldMap>?>>>? = null

    @JvmField
    var overlayFloorColors: IntArray? = null

    @JvmField
    var underlayRgbColors: ByteArray? = null

    @JvmField
    var underlayGreenBlue: ShortArray? = null

    @JvmField
    var worldmapComponent: Component? = null

    @JvmStatic
    fun initialize(
        areaResource: Js5ResourceProvider,
        floTypeList: FloTypeList,
        fluTypeList: FluTypeList,
        locTypeList: ObjectTypeList,
        mecTypeList: MECTypeList,
        mapspriteTypeList: MapSpriteTypeList,
        varProvider: VarDomain
    ) {
        MAP_AREA_RESOURCE = areaResource
        FLOTYPE_LIST = floTypeList
        FLUTYPE_LIST = fluTypeList
        LOCTYPE_LIST = locTypeList
        MECTYPE_LIST = mecTypeList
        MAP_SPRITE_TYPE_LIST = mapspriteTypeList
        PLAYER_VAR_PROVIDER = varProvider
        WORLDMAP_DEFS.clear()
        val detailsArchiveId = MAP_AREA_RESOURCE!!.getArchiveId("details")
        val detailsFileIds = MAP_AREA_RESOURCE!!.getValidFileIds(detailsArchiveId)
        if (detailsFileIds != null) {
            for (index in detailsFileIds.indices) {
                val element = WorldMapElement.list(MAP_AREA_RESOURCE!!, detailsArchiveId, detailsFileIds[index])
                WORLDMAP_DEFS.put(element, element.elementId.toLong())
            }
        }

        ColorUtil.initializeTables(true, false)
    }

    @JvmStatic
    fun initialize() {
        underlayIds = ByteArray(mapSizeX * mapSizeY)
        overlayIds = ByteArray(mapSizeX * mapSizeY)
        tileShapeAndRotations = ByteArray(mapSizeX * mapSizeY)
        tileDrawInfo = arrayOfNulls(mapSizeX * mapSizeY)
        shapesAndRotations = ByteArray(mapSizeX * mapSizeY)
        worldMapTiles = Array(3) { Array(mapSizeX shr 6) { arrayOfNulls<ArrayList<TileOnWorldMap>?>(mapSizeY shr 6) } }
        overlayFloorColors = IntArray(FLOTYPE_LIST!!.totalFileCount + 1)
    }

    @JvmStatic
    fun getRenderedElements(x: Int, y: Int): Queue<WorldMapElement> {
        val renderedElements = Queue<WorldMapElement>()
        var worldmapDef = WORLDMAP_DEFS.first() as? WorldMapElement
        while (worldmapDef != null) {
            if (worldmapDef.shouldRender && worldmapDef.allElementsInBounds(x, y)) {
                renderedElements.insert(worldmapDef)
            }
            worldmapDef = WORLDMAP_DEFS.next() as? WorldMapElement
        }
        return renderedElements
    }

    @JvmStatic
    fun fetch(key: Int) {
        staticElement = WORLDMAP_DEFS.get(key.toLong()) as? WorldMapElement
    }

    @JvmStatic
    fun poll(key: Int): WorldMapElement? {
        return WORLDMAP_DEFS.get(key.toLong()) as? WorldMapElement
    }

    @JvmStatic
    fun renderMap(toolkit: AbstractToolkit, i_1: Int, i_2: Int) {
        var i_5 = tileViewEndX - tileViewStartX
        var i_6 = tileViewEndY - tileViewStartY
        if (tileViewEndX < mapSizeX) {
            i_5++
        }

        if (tileViewEndY < mapSizeY) {
            i_6++
        }

        for (i_7 in 0 until i_5) {
            val startX = (i_7 * i_1 shr 16) + pixelViewStartX
            val endX = ((i_7 + 1) * i_1 shr 16) + pixelViewStartX
            val width = endX - startX
            if (width > 0) {
                val i_11 = i_7 + tileViewStartX
                if (i_11 >= 0 && i_11 < mapSizeX) {
                    for (i_12 in 0 until i_6) {
                        val startY = pixelViewEndY - (i_2 * (i_12 + 1) shr 16)
                        val endY = pixelViewEndY - (i_12 * i_2 shr 16)
                        val height = endY - startY
                        if (height > 0) {
                            val i_16 = i_12 + tileViewStartY
                            val tileIndex = i_11 + i_16 * mapSizeX
                            var underlayColor = 0
                            var overlayId = 0
                            var drawnTile: Any? = null
                            if (i_16 >= 0 && i_16 < mapSizeY) {
                                underlayColor = (underlayRgbColors!![tileIndex].toInt() and 0xff shl 16) or (underlayGreenBlue!![tileIndex].toInt() and 0xffff)
                                if (underlayColor != 0) {
                                    underlayColor = underlayColor or 0xff000000.toInt()
                                }

                                overlayId = overlayIds!![tileIndex].toInt() and 0xff
                                drawnTile = tileDrawInfo!![tileIndex]
                            }

                            if (underlayColor == 0 && overlayId == 0 && drawnTile == null) {
                                underlayColor = if (staticElement!!.color != -1) {
                                    0xff000000.toInt() or staticElement!!.color
                                } else if (i_7 + tileViewStartX and 0x4 != i_12 + tileViewEndY and 0x4) {
                                    -11840664
                                } else {
                                    overlayFloorColors!![FLOTYPE_LIST!!.lastLoadedOverlayId + 1]
                                }

                                if (underlayColor == 0) {
                                    underlayColor = 0xff000000.toInt()
                                }

                                toolkit.drawFilledRectangle(startX, startY, width, height, underlayColor, 0)
                            } else if (drawnTile != null) {
                                if (drawnTile is LocOnWorldMap) {
                                    renderTile(
                                        toolkit, startX, startY, width, height, underlayColor, overlayId,
                                        tileShapeAndRotations!![tileIndex].toInt(), drawnTile.locIds, drawnTile.shapeAndRotation, true
                                    )
                                } else {
                                    val locId = drawnTile as Int
                                    LOC_ON_TILE[0] = locId
                                    SHAPE_AND_ROTATION_ON_TILE[0] = shapesAndRotations!![tileIndex]
                                    renderTile(
                                        toolkit, startX, startY, width, height, underlayColor, overlayId,
                                        tileShapeAndRotations!![tileIndex].toInt(), LOC_ON_TILE, SHAPE_AND_ROTATION_ON_TILE, true
                                    )
                                }
                            } else {
                                renderTile(
                                    toolkit, startX, startY, width, height, underlayColor, overlayId,
                                    tileShapeAndRotations!![tileIndex].toInt(), null, null, true
                                )
                            }
                        }
                    }
                } else {
                    for (i_12 in 0 until i_6) {
                        val i_13 = pixelViewEndY - (i_2 * (i_12 + 1) shr 16)
                        val i_14 = pixelViewEndY - (i_12 * i_2 shr 16)
                        val i_15 = i_14 - i_13
                        var overlayFloorColor = if (staticElement!!.color != -1) {
                            0xff000000.toInt() or staticElement!!.color
                        } else if (i_7 + tileViewStartX and 0x4 != i_12 + tileViewEndY and 0x4) {
                            -11840664
                        } else {
                            overlayFloorColors!![FLOTYPE_LIST!!.lastLoadedOverlayId + 1]
                        }

                        if (overlayFloorColor == 0) {
                            overlayFloorColor = 0xff000000.toInt()
                        }

                        toolkit.drawFilledRectangle(startX, i_13, width, i_15, overlayFloorColor, 0)
                    }
                }
            }
        }

        for (i_7 in -16 until i_5 + 16) {
            val i_8 = (i_7 * i_1 shr 16) + pixelViewStartX
            val i_9 = ((i_7 + 1) * i_1 shr 16) + pixelViewStartX
            val i_10 = i_9 - i_8
            if (i_10 > 0) {
                val i_11 = i_7 + tileViewStartX
                if (i_11 >= 0 && i_11 < mapSizeX) {
                    for (i_12 in -16 until i_6 + 16) {
                        val i_13 = pixelViewEndY - (i_2 * (i_12 + 1) shr 16)
                        val i_14 = pixelViewEndY - (i_12 * i_2 shr 16)
                        val i_15 = i_14 - i_13
                        if (i_15 > 0) {
                            val i_16 = i_12 + tileViewStartY
                            if (i_16 >= 0 && i_16 < mapSizeY) {
                                val object26 = tileDrawInfo!![i_11 + i_16 * mapSizeX]
                                if (object26 != null) {
                                    if (object26 is LocOnWorldMap) {
                                        renderObjectSprites(toolkit, i_8, i_13, i_10, i_15, object26.locIds, object26.shapeAndRotation)
                                    } else {
                                        val integer31 = object26 as Int
                                        LOC_ON_TILE[0] = integer31
                                        SHAPE_AND_ROTATION_ON_TILE[0] = shapesAndRotations!![i_11 + i_16 * mapSizeX]
                                        renderObjectSprites(toolkit, i_8, i_13, i_10, i_15, LOC_ON_TILE, SHAPE_AND_ROTATION_ON_TILE)
                                    }
                                } else {
                                    renderObjectSprites(toolkit, i_8, i_13, i_10, i_15, null, null)
                                }
                            }
                        }
                    }
                }
            }
        }

        var startX = tileViewStartX shr 6
        var startY = tileViewStartY shr 6
        if (startX < 0) {
            startX = 0
        }

        if (startY < 0) {
            startY = 0
        }

        var endX = tileViewEndX shr 6
        var endY = tileViewEndY shr 6
        if (endX >= worldMapTiles!![0].size) {
            endX = worldMapTiles!![0].size - 1
        }

        if (endY >= worldMapTiles!![0][0].size) {
            endY = worldMapTiles!![0][0].size - 1
        }

        for (plane in 0..2) {
            for (x in startX..endX) {
                for (y in startY..endY) {
                    val tiles = worldMapTiles!![plane][x][y]
                    if (tiles != null) {
                        val i_15 = ((mapSquareAbsX shr 6) + x) * 64
                        val i_16 = ((mapSquareAbsY shr 6) + y) * 64
                        val tilesIt = tiles.iterator()

                        while (tilesIt.hasNext()) {
                            val tile = tilesIt.next()
                            val i_19 = i_15 + tile.mapsquarePosX - mapSquareAbsX - tileViewStartX
                            val i_28 = i_16 + tile.mapsquarePosY - mapSquareAbsY - tileViewStartY
                            val i_21 = (i_19 * i_1 shr 16) + pixelViewStartX
                            val i_22 = ((i_19 + 1) * i_1 shr 16) + pixelViewStartX
                            val i_23 = pixelViewEndY - (i_2 * (i_28 + 1) shr 16)
                            val i_24 = pixelViewEndY - (i_28 * i_2 shr 16)
                            renderTile(
                                toolkit, i_21, i_23, i_22 - i_21, i_24 - i_23, tile.underlayIdOrColor, (tile.overlayId.toInt() and 0xff),
                                tile.tileShapeAndRotation.toInt(), tile.locIds, tile.shapeAndRotations, false
                            )
                        }
                    }
                }
            }

            for (x in startX..endX) {
                for (y in startY..endY) {
                    val tiles = worldMapTiles!![plane][x][y]
                    if (tiles != null) {
                        val absX = ((mapSquareAbsX shr 6) + x) * 64
                        val absY = ((mapSquareAbsY shr 6) + y) * 64
                        val tilesIt = tiles.iterator()

                        while (tilesIt.hasNext()) {
                            val class26932 = tilesIt.next()
                            val i_19 = absX + class26932.mapsquarePosX - mapSquareAbsX - tileViewStartX
                            val i_28 = absY + class26932.mapsquarePosY - mapSquareAbsY - tileViewStartY
                            val i_21 = (i_19 * i_1 shr 16) + pixelViewStartX
                            val i_22 = ((i_19 + 1) * i_1 shr 16) + pixelViewStartX
                            val i_23 = pixelViewEndY - (i_2 * (i_28 + 1) shr 16)
                            val i_24 = pixelViewEndY - (i_28 * i_2 shr 16)
                            renderObjectSprites(toolkit, i_21, i_23, i_22 - i_21, i_24 - i_23, class26932.locIds, class26932.shapeAndRotations)
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun calculateRunetekPalette(materialProvider: MaterialProvider, hueRandomisation: Int, lightnessRandomisation: Int) {
        for (overlayId in 0 until FLOTYPE_LIST!!.totalFileCount) {
            overlayFloorColors!![overlayId + 1] = getOverlayFloorColor(materialProvider, overlayId, hueRandomisation, lightnessRandomisation)
        }
    }

    @JvmStatic
    fun getViewportCategories(): NodeCollection {
        val i_1 = tileViewEndX - tileViewStartX
        val i_2 = tileViewEndY - tileViewStartY
        val i_3 = (pixelViewEndX - pixelViewStartX shl 16) / i_1
        val i_4 = (pixelViewEndY - pixelViewStartY shl 16) / i_2
        return positionAllCategories(i_3, i_4)
    }

    @JvmStatic
    fun buildMapCategories() {
        for (x in 0 until mapSizeX) {
            for (y in 0 until mapSizeY) {
                val drawInfo = tileDrawInfo!![x + y * mapSizeX]
                if (drawInfo != null) {
                    if (drawInfo is LocOnWorldMap) {
                        for (index in drawInfo.locIds.indices) {
                            var locType = LOCTYPE_LIST!!.list(drawInfo.locIds[index])
                            var mecId = locType!!.mapCategoryid
                            if (locType.alternativeIds != null) {
                                val multiLoc = locType.getMultiLoc(PLAYER_VAR_PROVIDER!!)
                                if (multiLoc != null) {
                                    locType = multiLoc
                                    mecId = locType.mapCategoryid
                                }
                            }

                            if (mecId != -1) {
                                val category = WorldMapCategory(mecId)
                                category.absX = x
                                category.absY = y
                                worldMapCategories.addFirst(category)
                            }
                        }
                    } else {
                        val locId = drawInfo as Int
                        var locType = LOCTYPE_LIST!!.list(locId)
                        var mecId = locType!!.mapCategoryid
                        if (locType.alternativeIds != null) {
                            val multiLoc = locType.getMultiLoc(PLAYER_VAR_PROVIDER!!)
                            if (multiLoc != null) {
                                locType = multiLoc
                                mecId = locType.mapCategoryid
                            }
                        }

                        if (mecId != -1) {
                            val category = WorldMapCategory(mecId)
                            category.absX = x
                            category.absY = y
                            worldMapCategories.addFirst(category)
                        }
                    }
                }
            }
        }

        for (plane in 0..2) {
            for (x in 0 until worldMapTiles!![0].size) {
                label90@ for (y in 0 until worldMapTiles!![0][0].size) {
                    val arraylist15 = worldMapTiles!![plane][x][y]
                    if (arraylist15 != null) {
                        val it = arraylist15.iterator()

                        while (true) {
                            var tile: TileOnWorldMap
                            do {
                                if (!it.hasNext()) {
                                    continue@label90
                                }

                                tile = it.next()
                            } while (tile.locIds == null)

                            for (index in tile.locIds!!.indices) {
                                var locType = LOCTYPE_LIST!!.list(tile.locIds!![index])
                                var mecId = locType!!.mapCategoryid
                                if (locType.alternativeIds != null) {
                                    val multiLoc = locType.getMultiLoc(PLAYER_VAR_PROVIDER!!)
                                    if (multiLoc != null) {
                                        locType = multiLoc
                                        mecId = locType.mapCategoryid
                                    }
                                }

                                if (mecId != -1) {
                                    val category = WorldMapCategory(mecId)
                                    category.absX = ((mapSquareAbsX shr 6) + x) * 64 + tile.mapsquarePosX - mapSquareAbsX
                                    category.absY = ((mapSquareAbsY shr 6) + y) * 64 + tile.mapsquarePosY - mapSquareAbsY
                                    worldMapCategories.addFirst(category)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun reset() {
        underlayIds = null
        underlayRgbColors = null
        underlayGreenBlue = null
        overlayIds = null
        tileShapeAndRotations = null
        tileDrawInfo = null
        shapesAndRotations = null
        worldMapTiles = null
        overlayFloorColors = null
    }

    @JvmStatic
    fun setViewportBounds(i_0: Int, i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
        tileViewStartX = i_0 - mapSquareAbsX
        tileViewEndY = i_1 - mapSquareAbsY
        tileViewEndX = i_2 - mapSquareAbsX
        tileViewStartY = i_3 - mapSquareAbsY
        pixelViewStartX = i_4
        pixelViewStartY = i_5
        pixelViewEndX = i_6
        pixelViewEndY = i_7
    }

    @JvmStatic
    fun renderTile(
        toolkit: AbstractToolkit,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        underlayColor: Int,
        overlayId: Int,
        tileShapeAndRotation: Int,
        locids: IntArray?,
        locShapesAndRotations: ByteArray?,
        clearPixels: Boolean
    ) {
        if (clearPixels || underlayColor != 0 || overlayId > 0) {
            if (overlayId == 0) {
                toolkit.drawFilledRectangle(x, y, width, height, underlayColor, 0)
            } else {
                val shape = tileShapeAndRotation and 0x3f
                if (shape != 0 && width > 1 && height > 1) {
                    val clearType = if (clearPixels) 0 else 1
                    renderWorldmapTile(
                        toolkit, tileShapePatterns!!, halfZoomLevel, x, y, underlayColor,
                        overlayFloorColors!![overlayId], width, height, shape, tileShapeAndRotation shr 6 and 0x3, clearType
                    )
                } else {
                    val floorColor = overlayFloorColors!![overlayId]
                    if (clearPixels || floorColor != 0) {
                        toolkit.drawFilledRectangle(x, y, width, height, floorColor, 0)
                    }
                }
            }
        }

        if (locids != null) {
            val endX = if (width == 1) x else width + x - 1
            val endY = if (height == 1) y else y + height - 1

            for (index in locids.indices) {
                val shape = locShapesAndRotations!![index].toInt() and 0x3f
                if (shape == ObjectShapes.WALL_STRAIGHT.shape || shape == ObjectShapes.WALL_WHOLE_CORNER.shape ||
                    shape == ObjectShapes.WALL_STRAIGHT_CORNER.shape || shape == ObjectShapes.WALL_INTERACT.shape
                ) {
                    val loctype = LOCTYPE_LIST!!.list(locids[index])
                    if (loctype!!.mapIconId == -1) {
                        var color = -3355444
                        if (loctype.hasActions == 1) {
                            color = -3407872
                        }

                        val rotation = locShapesAndRotations[index].toInt() shr 6 and 0x3
                        when (shape) {
                            ObjectShapes.WALL_STRAIGHT.shape -> {
                                when (rotation) {
                                    0 -> toolkit.drawTransparentVerticalLine(x, y, height, color, 0)
                                    1 -> toolkit.drawTransparentHorizontalLine(x, y, width, color, 0)
                                    2 -> toolkit.drawTransparentVerticalLine(endX, y, height, color, 0)
                                    else -> toolkit.drawTransparentHorizontalLine(x, endY, width, color, 0)
                                }
                            }
                            ObjectShapes.WALL_WHOLE_CORNER.shape -> {
                                when (rotation) {
                                    0 -> {
                                        toolkit.drawTransparentVerticalLine(x, y, height, -1, 0)
                                        toolkit.drawTransparentHorizontalLine(x, y, width, color, 0)
                                    }
                                    1 -> {
                                        toolkit.drawTransparentVerticalLine(endX, y, height, -1, 0)
                                        toolkit.drawTransparentHorizontalLine(x, y, width, color, 0)
                                    }
                                    2 -> {
                                        toolkit.drawTransparentVerticalLine(endX, y, height, -1, 0)
                                        toolkit.drawTransparentHorizontalLine(x, endY, width, color, 0)
                                    }
                                    else -> {
                                        toolkit.drawTransparentVerticalLine(x, y, height, -1, 0)
                                        toolkit.drawTransparentHorizontalLine(x, endY, width, color, 0)
                                    }
                                }
                            }
                            ObjectShapes.WALL_STRAIGHT_CORNER.shape -> {
                                when (rotation) {
                                    0 -> toolkit.drawTransparentHorizontalLine(x, y, 1, color, 0)
                                    1 -> toolkit.drawTransparentHorizontalLine(endX, y, 1, color, 0)
                                    2 -> toolkit.drawTransparentHorizontalLine(endX, endY, 1, color, 0)
                                    else -> toolkit.drawTransparentHorizontalLine(x, endY, 1, color, 0)
                                }
                            }
                            ObjectShapes.WALL_INTERACT.shape -> {
                                if (rotation != 0 && rotation != 2) {
                                    for (h in 0 until height) {
                                        toolkit.drawTransparentHorizontalLine(h + x, h + y, 1, color, 0)
                                    }
                                } else {
                                    for (h in 0 until height) {
                                        toolkit.drawTransparentHorizontalLine(h + x, endY - h, 1, color, 0)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun loadWorldMapCategories() {
        val placement = IntArray(3)
        for (index in 0 until staticElements!!.elementCount) {
            val addToMap = staticElement!!.gameToMapCoordsForPlane(
                staticElements!!.bitpackedPlacement[index] shr 28 and 0x3,
                staticElements!!.bitpackedPlacement[index] shr 14 and 0x3fff,
                staticElements!!.bitpackedPlacement[index] and 0x3fff,
                placement
            )
            if (addToMap) {
                val worldMapCategory = WorldMapCategory(staticElements!!.mapCategoryIds[index])
                worldMapCategory.absX = placement[1] - mapSquareAbsX
                worldMapCategory.absY = placement[2] - mapSquareAbsY
                worldMapCategories.addFirst(worldMapCategory)
            }
        }
    }

    @JvmStatic
    fun positionCategory(class282Sub361: WorldMapCategory, i_2: Int, i_3: Int) {
        class282Sub361.screenX = (i_2 * (class282Sub361.absX - tileViewStartX) shr 16) + pixelViewStartX
        class282Sub361.screenY = pixelViewEndY - (i_3 * (class282Sub361.absY - tileViewStartY) shr 16)
    }

    @JvmStatic
    fun renderCategoryLines(toolkit: AbstractToolkit, worldMapCategory: WorldMapCategory, mectype: MECType) {
        if (mectype.offsets != null) {
            val positioning = IntArray(mectype.offsets!!.size)
            for (i_4 in 0 until positioning.size / 2) {
                val i_5 = mectype.offsets!![i_4 * 2] + worldMapCategory.absX
                val i_6 = worldMapCategory.absY + mectype.offsets!![i_4 * 2 + 1]
                positioning[i_4 * 2] = pixelViewStartX + (pixelViewEndX - pixelViewStartX) * (i_5 - tileViewStartX) / (tileViewEndX - tileViewStartX)
                positioning[i_4 * 2 + 1] = pixelViewEndY - (pixelViewEndY - pixelViewStartY) * (i_6 - tileViewStartY) / (tileViewEndY - tileViewStartY)
            }

            WorldMapRasterManager.fillPolygon(toolkit, positioning, mectype.lineColor)
            if (mectype.dashLineSpacing > 0) {
                for (index in 0 until positioning.size / 2 - 1) {
                    var x = positioning[index * 2]
                    var y = positioning[index * 2 + 1]
                    var width = positioning[(index + 1) * 2]
                    var length = positioning[(index + 1) * 2 + 1]
                    if (width < x) {
                        val i_9 = x
                        val i_10 = y
                        x = width
                        y = length
                        width = i_9
                        length = i_10
                    } else if (width == x && length < y) {
                        val i_9 = y
                        y = length
                        length = i_9
                    }

                    toolkit.drawLine(x, y, width, length, mectype.colors!![mectype.colorPointers!![index].toInt() and 0xff], mectype.dashLineSpacing, mectype.dashLineLength, mectype.dashLineOffset)
                }

                var i_4 = positioning[positioning.size - 2]
                var i_5 = positioning[positioning.size - 1]
                var i_6 = positioning[0]
                var i_7 = positioning[1]
                if (i_6 < i_4) {
                    val i_8 = i_4
                    val i_9 = i_5
                    i_4 = i_6
                    i_5 = i_7
                    i_6 = i_8
                    i_7 = i_9
                } else if (i_6 == i_4 && i_7 < i_5) {
                    val i_8 = i_5
                    i_5 = i_7
                    i_7 = i_8
                }

                toolkit.drawLine(i_4, i_5, i_6, i_7, mectype.colors!![mectype.colorPointers!![mectype.colorPointers!!.size - 1].toInt() and 0xff], mectype.dashLineSpacing, mectype.dashLineLength, mectype.dashLineOffset)
            } else {
                for (index in 0 until positioning.size / 2 - 1) {
                    toolkit.drawOpaqueLine(positioning[index * 2], positioning[index * 2 + 1], positioning[(index + 1) * 2], positioning[(index + 1) * 2 + 1], mectype.colors!![mectype.colorPointers!![index].toInt() and 0xff])
                }
                toolkit.drawOpaqueLine(positioning[positioning.size - 2], positioning[positioning.size - 1], positioning[0], positioning[1], mectype.colors!![mectype.colorPointers!![mectype.colorPointers!!.size - 1].toInt() and 0xff])
            }
        }
    }

    @JvmStatic
    fun prepareMapdata(buffer: Packet, i_2: Int, i_3: Int, x: Int, y: Int, ints_6: IntArray, ints_7: IntArray) {
        val flags = buffer.readUByte()
        if (flags and 0x1 == 0) {
            val bool9 = flags and 0x2 == 0
            var i_10 = flags shr 2 and 0x3f
            if (i_10 != 62) {
                if (i_10 == 63) {
                    i_10 = buffer.readUByte()
                } else if (bool9) {
                    i_10 = ints_6[i_10]
                } else {
                    i_10 = ints_7[i_10]
                }

                if (bool9) {
                    underlayIds!![y * mapSizeX + x] = i_10.toByte()
                    overlayIds!![y * mapSizeX + x] = 0
                } else {
                    overlayIds!![y * mapSizeX + x] = i_10.toByte()
                    tileShapeAndRotations!![y * mapSizeX + x] = 0
                    underlayIds!![y * mapSizeX + x] = buffer.readByte()
                }
            }
        } else {
            val i_21 = (flags shr 1 and 0x3) + 1
            val bool22 = flags and 0x8 != 0
            val hasLocs = flags and 0x10 != 0

            for (tileType in 0 until i_21) {
                val underlay = buffer.readUByte()
                var overlay = 0
                var tileRotationAndShape = 0
                if (bool22) {
                    overlay = buffer.readUByte()
                    tileRotationAndShape = buffer.readUByte()
                }

                var locCount = 0
                if (hasLocs) {
                    locCount = buffer.readUByte()
                }

                val objectIds: IntArray?
                val locRotationAndShape: ByteArray?
                if (tileType == 0) {
                    underlayIds!![y * mapSizeX + x] = underlay.toByte()
                    overlayIds!![y * mapSizeX + x] = overlay.toByte()
                    tileShapeAndRotations!![y * mapSizeX + x] = tileRotationAndShape.toByte()
                    if (locCount == 1) {
                        tileDrawInfo!![y * mapSizeX + x] = buffer.readBigSmart()
                        shapesAndRotations!![y * mapSizeX + x] = buffer.readByte()
                    } else if (locCount > 1) {
                        objectIds = IntArray(locCount)
                        locRotationAndShape = ByteArray(locCount)

                        for (index in 0 until locCount) {
                            objectIds[index] = buffer.readBigSmart()
                            locRotationAndShape[index] = buffer.readByte()
                        }

                        tileDrawInfo!![y * mapSizeX + x] = LocOnWorldMap(objectIds, locRotationAndShape)
                    }
                } else {
                    if (locCount > 0) {
                        objectIds = IntArray(locCount)
                        locRotationAndShape = ByteArray(locCount)

                        for (index in 0 until locCount) {
                            objectIds[index] = buffer.readBigSmart()
                            locRotationAndShape[index] = buffer.readByte()
                        }
                    } else {
                        objectIds = null
                        locRotationAndShape = null
                    }

                    if (worldMapTiles!![tileType - 1][i_2 - (mapSquareAbsX shr 6)][i_3 - (mapSquareAbsY shr 6)] == null) {
                        worldMapTiles!![tileType - 1][i_2 - (mapSquareAbsX shr 6)][i_3 - (mapSquareAbsY shr 6)] = ArrayList()
                    }

                    val tile = TileOnWorldMap(x and 0x3f, y and 0x3f, underlay, overlay, tileRotationAndShape, objectIds, locRotationAndShape)
                    worldMapTiles!![tileType - 1][i_2 - (mapSquareAbsX shr 6)][i_3 - (mapSquareAbsY shr 6)]!!.add(tile)
                }
            }
        }
    }

    @JvmStatic
    fun getOverlayFloorColor(materialProvider: MaterialProvider, overlayId: Int, hueRandomisation: Int, lightnessRandomisation: Int): Int {
        val overlay = FLOTYPE_LIST!!.list(overlayId) ?: return 0
        var textureId = overlay.texture
        if (textureId >= 0 && materialProvider.getMaterial(textureId)!!.detailsOnly) {
            textureId = -1
        }

        val finalFloorColor: Int
        if (overlay.minimapColorHsl >= 0) {
            val colorHsl = overlay.minimapColorHsl
            var lightness = lightnessRandomisation + (colorHsl and 0x7f)
            if (lightness < 0) {
                lightness = 0
            } else if (lightness > 127) {
                lightness = 127
            }

            val modifiedHsl = lightness + ((colorHsl + hueRandomisation) and 0xfc00) + (colorHsl and 0x380)
            finalFloorColor = 0xff000000.toInt() or ColorUtil.hsvToRgbTable!![(ColorUtil.adjustHsv(ColorUtil.repackHslWithSentinel(modifiedHsl, 96)).toInt() and 0xffff)]
        } else if (textureId >= 0) {
            finalFloorColor = 0xff000000.toInt() or ColorUtil.hsvToRgbTable!![(ColorUtil.adjustHsv(ColorUtil.repackHslWithSentinel(materialProvider.getMaterial(textureId)!!.colorHsl.toInt(), 96)).toInt() and 0xffff)]
        } else if (overlay.tileColorHsl == -1) {
            finalFloorColor = 0
        } else {
            val colorHsl = overlay.tileColorHsl
            var lightness = lightnessRandomisation + (colorHsl and 0x7f)
            if (lightness < 0) {
                lightness = 0
            } else if (lightness > 127) {
                lightness = 127
            }

            val modifiedHsl = lightness + ((colorHsl + hueRandomisation) and 0xfc00) + (colorHsl and 0x380)
            finalFloorColor = 0xff000000.toInt() or ColorUtil.hsvToRgbTable!![(ColorUtil.adjustHsv(ColorUtil.repackHslWithSentinel(modifiedHsl, 96)).toInt() and 0xffff)]
        }

        return finalFloorColor
    }

    @JvmStatic
    fun renderObjectSprites(toolkit: AbstractToolkit, i_1: Int, i_2: Int, i_3: Int, i_4: Int, ints_5: IntArray?, spriteRotation: ByteArray?) {
        if (ints_5 != null) {
            for (index in ints_5.indices) {
                val loctype = LOCTYPE_LIST!!.list(ints_5[index])
                val i_9 = loctype!!.mapIconId
                if (i_9 != -1) {
                    val mapspriteType = MAP_SPRITE_TYPE_LIST!!.list(i_9)
                    val nativeSprite = mapspriteType!!.createRotatedNativeSprite(toolkit, if (loctype.mapIconRotates) spriteRotation!![index].toInt() shr 6 and 0x3 else 0, loctype.mapIconFlipped && loctype.inverted)
                    if (nativeSprite != null) {
                        var i_12 = i_3 * nativeSprite.getSpriteWidth() shr 2
                        var i_13 = i_4 * nativeSprite.getSpriteHeight() shr 2
                        if (mapspriteType.requiresUpscaling) {
                            var x = loctype.sizeX
                            var y = loctype.sizeY
                            /** Rotate the sprite **/
                            if (spriteRotation!![index].toInt() shr 6 and 0x1 == 1) {
                                val tmpX = x
                                x = y
                                y = tmpX
                            }

                            i_12 = x * i_3
                            i_13 = y * i_4
                        }

                        if (i_12 != 0 && i_13 != 0) {
                            if (mapspriteType.backgroundColor != 0) {
                                nativeSprite.drawCircle(i_1, i_4 + (i_2 - i_13), i_12, i_13, 0, 0xff000000.toInt() or mapspriteType.backgroundColor, 1)
                            } else {
                                nativeSprite.drawCircle(i_1, i_4 + (i_2 - i_13), i_12, i_13)
                            }
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun getMapDef(baseX: Int, baseY: Int): WorldMapElement? {
        var mapDef = WORLDMAP_DEFS.first() as? WorldMapElement
        while (mapDef != null) {
            if (mapDef.shouldRender && mapDef.allElementsInBounds(baseX, baseY)) {
                return mapDef
            }
            mapDef = WORLDMAP_DEFS.next() as? WorldMapElement
        }
        return null
    }

    @JvmStatic
    fun positionAllCategories(i_1: Int, i_2: Int): NodeCollection {
        var class282Sub365 = worldMapCategories.getFirst() as? WorldMapCategory
        while (class282Sub365 != null) {
            positionCategory(class282Sub365, i_1, i_2)
            class282Sub365 = worldMapCategories.next() as? WorldMapCategory
        }

        return worldMapCategories
    }

    @JvmStatic
    fun renderMap(graphicalrenderer0: AbstractToolkit) {
        val i_1 = tileViewEndX - tileViewStartX
        val i_2 = tileViewEndY - tileViewStartY
        val i_3 = (pixelViewEndX - pixelViewStartX shl 16) / i_1
        val i_4 = (pixelViewEndY - pixelViewStartY shl 16) / i_2
        renderMap(graphicalrenderer0, i_3, i_4)
    }

    @JvmStatic
    fun loadMapTileData(i_1: Int, i_2: Int) {
        val buffer = Packet(MAP_AREA_RESOURCE!!.getNamedFile(staticElement!!.filenamePrefix, "area")!!)
        val i_4 = buffer.readUByte()
        val ints_5 = IntArray(i_4)

        for (i_6 in 0 until i_4) {
            ints_5[i_6] = buffer.readUByte()
        }

        val i_6 = buffer.readUByte()
        val ints_7 = IntArray(i_6)

        for (i_8 in 0 until i_6) {
            ints_7[i_8] = buffer.readUByte()
        }

        while (true) {
            while (buffer.currentPosition < buffer.buffer.size) {
                if (buffer.readUByte() == 0) {
                    // By tiles in map square
                    val i_8 = buffer.readUByte()
                    val i_9 = buffer.readUByte()

                    for (tileX in 0..63) {
                        for (tileY in 0..63) {
                            val i_12 = i_8 * 64 + tileX - mapSquareAbsX
                            val i_13 = i_9 * 64 + tileY - mapSquareAbsY
                            prepareMapdata(buffer, i_8, i_9, i_12, i_13, ints_5, ints_7)
                        }
                    }
                } else {
                    // By chunks in map square
                    val i_8 = buffer.readUByte()
                    val i_9 = buffer.readUByte()
                    val i_18 = buffer.readUByte()
                    val i_11 = buffer.readUByte()

                    for (chunkPosX in 0..7) {
                        for (chunkPosY in 0..7) {
                            val i_20 = i_8 * 64 + chunkPosX + i_18 * 8 - mapSquareAbsX
                            val i_15 = i_9 * 64 + chunkPosY + i_11 * 8 - mapSquareAbsY
                            prepareMapdata(buffer, i_8, i_9, i_20, i_15, ints_5, ints_7)
                        }
                    }
                }
            }

            underlayRgbColors = ByteArray(mapSizeX * mapSizeY)
            underlayGreenBlue = ShortArray(mapSizeX * mapSizeY)

            for (plane in 0..2) {
                val underlays = ByteArray(mapSizeX * mapSizeY)

                for (x in 0 until worldMapTiles!![plane].size) {
                    for (y in 0 until worldMapTiles!![plane][0].size) {
                        val arraylist19 = worldMapTiles!![plane][x][y]
                        if (arraylist19 != null) {
                            for (tile in arraylist19) {
                                underlays[x * 64 + tile.mapsquarePosX + (y * 64 + tile.mapsquarePosY) * mapSizeX] = tile.underlayIdOrColor.toByte()
                            }
                        }
                    }
                }

                decodeUnderlayColors(underlays, underlayRgbColors!!, underlayGreenBlue!!, i_1, i_2)

                for (i_11 in 0 until worldMapTiles!![plane].size) {
                    for (i_12 in 0 until worldMapTiles!![plane][0].size) {
                        val arraylist19 = worldMapTiles!![plane][i_11][i_12]
                        if (arraylist19 != null) {
                            for (tile in arraylist19) {
                                val i_16 = i_11 * 64 + tile.mapsquarePosX + (i_12 * 64 + tile.mapsquarePosY) * mapSizeX
                                tile.underlayIdOrColor = (underlayRgbColors!![i_16].toInt() and 0xff shl 16) or (underlayGreenBlue!![i_16].toInt() and 0xffff)
                                if (tile.underlayIdOrColor != 0) {
                                    tile.underlayIdOrColor = tile.underlayIdOrColor or 0xff000000.toInt()
                                }
                            }
                        }
                    }
                }
            }

            decodeUnderlayColors(underlayIds!!, underlayRgbColors!!, underlayGreenBlue!!, i_1, i_2)
            underlayIds = null
            buildMapCategories()
            return
        }
    }

    @JvmStatic
    fun decodeUnderlayColors(underlayIds: ByteArray, bytes_1: ByteArray, shorts_2: ShortArray, i_3: Int, i_4: Int) {
        val underlayHue = IntArray(mapSizeY)
        val underlaySaturation = IntArray(mapSizeY)
        val underlayLightness = IntArray(mapSizeY)
        val underlayDivisor = IntArray(mapSizeY)
        val underlayDirection = IntArray(mapSizeY)

        for (x in -5 until mapSizeX) {
            val positiveX = x + 5
            val negativeX = x - 5

            for (y in 0 until mapSizeY) {
                if (positiveX < mapSizeX) {
                    val underlayId = underlayIds[positiveX + y * mapSizeX].toInt() and 0xff
                    if (underlayId > 0) {
                        val underlay = FLUTYPE_LIST!!.list(underlayId - 1)
                        underlayHue[y] += underlay!!.hue
                        underlaySaturation[y] += underlay.saturation
                        underlayLightness[y] += underlay.lightness
                        underlayDivisor[y] += underlay.divisor
                        underlayDirection[y]++
                    }
                }

                if (negativeX >= 0) {
                    val underlayId = underlayIds[negativeX + y * mapSizeX].toInt() and 0xff
                    if (underlayId > 0) {
                        val underlay = FLUTYPE_LIST!!.list(underlayId - 1)
                        underlayHue[y] -= underlay!!.hue
                        underlaySaturation[y] -= underlay.saturation
                        underlayLightness[y] -= underlay.lightness
                        underlayDivisor[y] -= underlay.divisor
                        underlayDirection[y]--
                    }
                }
            }

            if (x >= 0) {
                var hue = 0
                var saturation = 0
                var lightness = 0
                var divisor = 0
                var direction = 0

                for (y in -5 until mapSizeY) {
                    val positiveY = y + 5
                    if (positiveY < mapSizeY) {
                        hue += underlayHue[positiveY]
                        saturation += underlaySaturation[positiveY]
                        lightness += underlayLightness[positiveY]
                        divisor += underlayDivisor[positiveY]
                        direction += underlayDirection[positiveY]
                    }

                    val negativeY = y - 5
                    if (negativeY >= 0) {
                        hue -= underlayHue[negativeY]
                        saturation -= underlaySaturation[negativeY]
                        lightness -= underlayLightness[negativeY]
                        divisor -= underlayDivisor[negativeY]
                        direction -= underlayDirection[negativeY]
                    }

                    if (y >= 0 && direction > 0) {
                        if (underlayIds[x + y * mapSizeX].toInt() and 0xff == 0) {
                            val i_21 = x + y * mapSizeX
                            bytes_1[i_21] = 0
                            shorts_2[i_21] = 0
                        } else {
                            val hsl24 = if (divisor == 0) 0 else ColorUtil.hsl16to24(hue * 256 / divisor, saturation / direction, lightness / direction)
                            var clampedLightness = (hsl24 and 0x7f) + i_4
                            if (clampedLightness < 0) {
                                clampedLightness = 0
                            } else if (clampedLightness > 127) {
                                clampedLightness = 127
                            }

                            val i_23 = clampedLightness + ((i_3 + hsl24) and 0xfc00) + (hsl24 and 0x380)
                            val i_24 = x + y * mapSizeX
                            val rgb = ColorUtil.hsvToRgbTable!![(ColorUtil.adjustHsv(ColorUtil.repackHslSafe(i_23, 96)).toInt() and 0xffff)]
                            bytes_1[i_24] = ((rgb shr 16) and 0xff).toByte()
                            shorts_2[i_24] = (rgb and 0xffff).toShort()
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun renderWorldmapTile(
        toolkit: AbstractToolkit,
        bytes_1: Array<Array<ByteArray?>>,
        i_2: Int,
        x: Int,
        y: Int,
        underlayColor: Int,
        overlayColor: Int,
        width: Int,
        height: Int,
        tileShape: Int,
        tileRotation: Int,
        clearType: Int
    ) {
        var shape = tileShape
        var rotation = tileRotation
        if (shape != 0 && i_2 != 0) {
            when (shape) {
                9 -> {
                    shape = 1
                    rotation = (rotation + 1) and 0x3
                }
                10 -> {
                    shape = 1
                    rotation = (rotation + 3) and 0x3
                }
                11 -> {
                    shape = 8
                    rotation = (rotation + 3) and 0x3
                }
            }
            toolkit.renderWorldmapTile(x, y, width, height, underlayColor, overlayColor, bytes_1[shape - 1][rotation]!!, i_2, clearType)
        }
    }

    @JvmStatic
    fun getNextActiveCategory(): WorldMapCategory? {
        var category = WorldMapSub1.categoryIterator.next() as? WorldMapCategory
        while (category != null) {
            val mecType = MECTYPE_LIST!!.list(category.mecId)
            if (mecType != null && mecType.visible && mecType.shouldDraw(PLAYER_VAR_PROVIDER!!)) {
                return category
            }
            category = WorldMapSub1.categoryIterator.next() as? WorldMapCategory
        }
        return null
    }

    @JvmStatic
    fun drawWorldmap(toolkit: AbstractToolkit, materials: MaterialProvider, left: Int, top: Int, width: Int, height: Int) {
        if (ComponentMinimap.mapAssetLoadingPercentage < 100) {
            WorldMapSub1.loadAssets(toolkit, materials)
        }
        toolkit.setClipping(left, top, left + width, top + height)
        if (ComponentMinimap.mapAssetLoadingPercentage < 100) {
            val b_7: Byte = 20
            val centerX = width / 2 + left
            val centerY = top + height / 2 - 18 - b_7
            toolkit.drawFilledRectangle(left, top, width, height, 0xff000000.toInt(), 0)
            toolkit.drawRectangleOutline(centerX - 152, centerY, 304, 34, Client.DEBUG_BG_COLOR[Client.DEBUG_DISPLAY_COLOR_INDEX]!!.getRGB(), 0)
            toolkit.drawFilledRectangle(centerX - 150, centerY + 2, ComponentMinimap.mapAssetLoadingPercentage * 3, 30, Client.DEBUG_LOAD_COLOR[Client.DEBUG_DISPLAY_COLOR_INDEX]!!.getRGB(), 0)
            MiniMenu.minimenuHeader!!.renderCentered(LocalizedText.LOADING.translate(Client.CURRENT_LANGUAGE!!), centerX, b_7 + centerY, Client.DEBUG_TEXT_COLOR[Client.DEBUG_DISPLAY_COLOR_INDEX]!!.getRGB(), -1)
        } else {
            val i_17 = WorldMapSub1.mapCenterX - (width / mapZoom).toInt()
            val i_8 = (height / mapZoom).toInt() + WorldMapSub1.mapCenterY
            val i_9 = (width / mapZoom).toInt() + WorldMapSub1.mapCenterX
            val i_10 = WorldMapSub1.mapCenterY - (height / mapZoom).toInt()
            DebugDisplay.worldMapViewLeftTileX = WorldMapSub1.mapCenterX - (width / mapZoom).toInt()
            WorldMapSub1.viewBottomTileY = WorldMapSub1.mapCenterY - (height / mapZoom).toInt()
            StaticMethods.worldMapViewWidth = ((width * 2) / mapZoom).toInt()
            ComponentMinimap.mapViewportScale = ((height * 2) / mapZoom).toInt()
            setViewportBounds(i_17 + mapSquareAbsX, i_8 + mapSquareAbsY, i_9 + mapSquareAbsX, i_10 + mapSquareAbsY, left, top, left + width, top + height + 1)
            renderMap(toolkit)
            val nodecollection11 = getViewportCategories()
            WorldMapSub1.renderInformation(toolkit, nodecollection11)
            if (JSObjectManager.highlightFlashCycles > 0) {
                if (--ServerEnvironment.flashCycleCounter == 0) {
                    JSObjectManager.highlightFlashCycles--
                    ServerEnvironment.flashCycleCounter = 20
                }
            }
            if (Client.DRAW_DEBUG) {
                var textX = left + width - 5
                var textY = top + height - 8
                FontManager.debugOverlayFont!!.renderLeftAligned("Fps:" + GameShell.FPS, textX, textY, 16776960, -1)
                textY -= 15
                val runtime = Runtime.getRuntime()
                val freeMemoryKbs = ((runtime.totalMemory() - runtime.freeMemory()) / 1024L).toInt()
                var textColor = 16776960
                if (freeMemoryKbs > 65536) {
                    textColor = 16711680
                }
                FontManager.debugOverlayFont!!.renderLeftAligned("Mem:" + freeMemoryKbs + "k", textX, textY, textColor, -1)
                textY -= 15
            }
        }
    }
}
