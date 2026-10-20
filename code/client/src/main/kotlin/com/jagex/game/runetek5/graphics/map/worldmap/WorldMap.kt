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
        mapSpriteTypeList: MapSpriteTypeList,
        varProvider: VarDomain
    ) {
        MAP_AREA_RESOURCE = areaResource
        FLOTYPE_LIST = floTypeList
        FLUTYPE_LIST = fluTypeList
        LOCTYPE_LIST = locTypeList
        MECTYPE_LIST = mecTypeList
        MAP_SPRITE_TYPE_LIST = mapSpriteTypeList
        PLAYER_VAR_PROVIDER = varProvider
        WORLDMAP_DEFS.clear()
        val detailsArchiveId = MAP_AREA_RESOURCE!!.getArchiveId("details")
        val detailsFileIds = MAP_AREA_RESOURCE!!.getValidFileIds(detailsArchiveId)
        if (detailsFileIds != null) {
            for (index in detailsFileIds.indices) {
                val mapElement = WorldMapElement.list(MAP_AREA_RESOURCE!!, detailsArchiveId, detailsFileIds[index])
                WORLDMAP_DEFS.put(mapElement, mapElement.elementId.toLong())
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
    fun getRenderedElements(coordX: Int, coordY: Int): Queue<WorldMapElement> {
        val renderedElements = Queue<WorldMapElement>()
        var mapElement = WORLDMAP_DEFS.first() as? WorldMapElement
        while (mapElement != null) {
            if (mapElement.shouldRender && mapElement.allElementsInBounds(coordX, coordY)) {
                renderedElements.insert(mapElement)
            }
            mapElement = WORLDMAP_DEFS.next() as? WorldMapElement
        }
        return renderedElements
    }

    @JvmStatic
    fun fetch(elementId: Int) {
        staticElement = WORLDMAP_DEFS.get(elementId.toLong()) as? WorldMapElement
    }

    @JvmStatic
    fun poll(elementId: Int): WorldMapElement? {
        return WORLDMAP_DEFS.get(elementId.toLong()) as? WorldMapElement
    }

    @JvmStatic
    fun renderMap(toolkit: AbstractToolkit, pixelsPerTileX: Int, pixelsPerTileY: Int) {
        var tileCountX = tileViewEndX - tileViewStartX
        var tileCountY = tileViewEndY - tileViewStartY
        if (tileViewEndX < mapSizeX) {
            tileCountX++
        }

        if (tileViewEndY < mapSizeY) {
            tileCountY++
        }

        for (tileX in 0 until tileCountX) {
            val startX = (tileX * pixelsPerTileX shr 16) + pixelViewStartX
            val endX = ((tileX + 1) * pixelsPerTileX shr 16) + pixelViewStartX
            val width = endX - startX
            if (width > 0) {
                val mapTileX = tileX + tileViewStartX
                if (mapTileX >= 0 && mapTileX < mapSizeX) {
                    for (tileY in 0 until tileCountY) {
                        val startY = pixelViewEndY - (pixelsPerTileY * (tileY + 1) shr 16)
                        val endY = pixelViewEndY - (tileY * pixelsPerTileY shr 16)
                        val height = endY - startY
                        if (height > 0) {
                            val mapTileY = tileY + tileViewStartY
                            val tileIndex = mapTileX + mapTileY * mapSizeX
                            var underlayColor = 0
                            var overlayId = 0
                            var drawnTile: Any? = null
                            if (mapTileY >= 0 && mapTileY < mapSizeY) {
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
                                } else if (tileX + tileViewStartX and 0x4 != tileY + tileViewEndY and 0x4) {
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
                    for (tileY in 0 until tileCountY) {
                        val startY = pixelViewEndY - (pixelsPerTileY * (tileY + 1) shr 16)
                        val endY = pixelViewEndY - (tileY * pixelsPerTileY shr 16)
                        val height = endY - startY
                        var overlayFloorColor = if (staticElement!!.color != -1) {
                            0xff000000.toInt() or staticElement!!.color
                        } else if (tileX + tileViewStartX and 0x4 != tileY + tileViewEndY and 0x4) {
                            -11840664
                        } else {
                            overlayFloorColors!![FLOTYPE_LIST!!.lastLoadedOverlayId + 1]
                        }

                        if (overlayFloorColor == 0) {
                            overlayFloorColor = 0xff000000.toInt()
                        }

                        toolkit.drawFilledRectangle(startX, startY, width, height, overlayFloorColor, 0)
                    }
                }
            }
        }

        for (tileX in -16 until tileCountX + 16) {
            val startX = (tileX * pixelsPerTileX shr 16) + pixelViewStartX
            val endX = ((tileX + 1) * pixelsPerTileX shr 16) + pixelViewStartX
            val width = endX - startX
            if (width > 0) {
                val mapTileX = tileX + tileViewStartX
                if (mapTileX >= 0 && mapTileX < mapSizeX) {
                    for (tileY in -16 until tileCountY + 16) {
                        val startY = pixelViewEndY - (pixelsPerTileY * (tileY + 1) shr 16)
                        val endY = pixelViewEndY - (tileY * pixelsPerTileY shr 16)
                        val height = endY - startY
                        if (height > 0) {
                            val mapTileY = tileY + tileViewStartY
                            if (mapTileY >= 0 && mapTileY < mapSizeY) {
                                val drawnTile = tileDrawInfo!![mapTileX + mapTileY * mapSizeX]
                                if (drawnTile != null) {
                                    if (drawnTile is LocOnWorldMap) {
                                        renderObjectSprites(toolkit, startX, startY, width, height, drawnTile.locIds, drawnTile.shapeAndRotation)
                                    } else {
                                        val locId = drawnTile as Int
                                        LOC_ON_TILE[0] = locId
                                        SHAPE_AND_ROTATION_ON_TILE[0] = shapesAndRotations!![mapTileX + mapTileY * mapSizeX]
                                        renderObjectSprites(toolkit, startX, startY, width, height, LOC_ON_TILE, SHAPE_AND_ROTATION_ON_TILE)
                                    }
                                } else {
                                    renderObjectSprites(toolkit, startX, startY, width, height, null, null)
                                }
                            }
                        }
                    }
                }
            }
        }

        var mapSquareStartX = tileViewStartX shr 6
        var mapSquareStartY = tileViewStartY shr 6
        if (mapSquareStartX < 0) {
            mapSquareStartX = 0
        }

        if (mapSquareStartY < 0) {
            mapSquareStartY = 0
        }

        var mapSquareEndX = tileViewEndX shr 6
        var mapSquareEndY = tileViewEndY shr 6
        if (mapSquareEndX >= worldMapTiles!![0].size) {
            mapSquareEndX = worldMapTiles!![0].size - 1
        }

        if (mapSquareEndY >= worldMapTiles!![0][0].size) {
            mapSquareEndY = worldMapTiles!![0][0].size - 1
        }

        for (plane in 0..2) {
            for (mapSquareX in mapSquareStartX..mapSquareEndX) {
                for (mapSquareY in mapSquareStartY..mapSquareEndY) {
                    val tiles = worldMapTiles!![plane][mapSquareX][mapSquareY]
                    if (tiles != null) {
                        val absX = ((mapSquareAbsX shr 6) + mapSquareX) * 64
                        val absY = ((mapSquareAbsY shr 6) + mapSquareY) * 64
                        val tilesIt = tiles.iterator()

                        while (tilesIt.hasNext()) {
                            val tile = tilesIt.next()
                            val viewTileX = absX + tile.mapsquarePosX - mapSquareAbsX - tileViewStartX
                            val viewTileY = absY + tile.mapsquarePosY - mapSquareAbsY - tileViewStartY
                            val pixelX = (viewTileX * pixelsPerTileX shr 16) + pixelViewStartX
                            val pixelEndX = ((viewTileX + 1) * pixelsPerTileX shr 16) + pixelViewStartX
                            val pixelY = pixelViewEndY - (pixelsPerTileY * (viewTileY + 1) shr 16)
                            val pixelEndY = pixelViewEndY - (viewTileY * pixelsPerTileY shr 16)
                            renderTile(
                                toolkit, pixelX, pixelY, pixelEndX - pixelX, pixelEndY - pixelY, tile.underlayIdOrColor, (tile.overlayId.toInt() and 0xff),
                                tile.tileShapeAndRotation.toInt(), tile.locIds, tile.shapeAndRotations, false
                            )
                        }
                    }
                }
            }

            for (mapSquareX in mapSquareStartX..mapSquareEndX) {
                for (mapSquareY in mapSquareStartY..mapSquareEndY) {
                    val tiles = worldMapTiles!![plane][mapSquareX][mapSquareY]
                    if (tiles != null) {
                        val absX = ((mapSquareAbsX shr 6) + mapSquareX) * 64
                        val absY = ((mapSquareAbsY shr 6) + mapSquareY) * 64
                        val tilesIt = tiles.iterator()

                        while (tilesIt.hasNext()) {
                            val tile = tilesIt.next()
                            val viewTileX = absX + tile.mapsquarePosX - mapSquareAbsX - tileViewStartX
                            val viewTileY = absY + tile.mapsquarePosY - mapSquareAbsY - tileViewStartY
                            val pixelX = (viewTileX * pixelsPerTileX shr 16) + pixelViewStartX
                            val pixelEndX = ((viewTileX + 1) * pixelsPerTileX shr 16) + pixelViewStartX
                            val pixelY = pixelViewEndY - (pixelsPerTileY * (viewTileY + 1) shr 16)
                            val pixelEndY = pixelViewEndY - (viewTileY * pixelsPerTileY shr 16)
                            renderObjectSprites(toolkit, pixelX, pixelY, pixelEndX - pixelX, pixelEndY - pixelY, tile.locIds, tile.shapeAndRotations)
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun calculateRunetekPalette(materialProvider: MaterialProvider, hueOffset: Int, lightnessOffset: Int) {
        for (overlayId in 0 until FLOTYPE_LIST!!.totalFileCount) {
            overlayFloorColors!![overlayId + 1] = getOverlayFloorColor(materialProvider, overlayId, hueOffset, lightnessOffset)
        }
    }

    @JvmStatic
    fun getViewportCategories(): NodeCollection {
        val tileCountX = tileViewEndX - tileViewStartX
        val tileCountY = tileViewEndY - tileViewStartY
        val pixelsPerTileX = (pixelViewEndX - pixelViewStartX shl 16) / tileCountX
        val pixelsPerTileY = (pixelViewEndY - pixelViewStartY shl 16) / tileCountY
        return positionAllCategories(pixelsPerTileX, pixelsPerTileY)
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
            for (mapSquareX in 0 until worldMapTiles!![0].size) {
                mapSquareYLoop@ for (mapSquareY in 0 until worldMapTiles!![0][0].size) {
                    val tiles = worldMapTiles!![plane][mapSquareX][mapSquareY]
                    if (tiles != null) {
                        val tilesIt = tiles.iterator()

                        while (true) {
                            var tile: TileOnWorldMap
                            do {
                                if (!tilesIt.hasNext()) {
                                    continue@mapSquareYLoop
                                }

                                tile = tilesIt.next()
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
                                    category.absX = ((mapSquareAbsX shr 6) + mapSquareX) * 64 + tile.mapsquarePosX - mapSquareAbsX
                                    category.absY = ((mapSquareAbsY shr 6) + mapSquareY) * 64 + tile.mapsquarePosY - mapSquareAbsY
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
    fun setViewportBounds(
        absTileStartX: Int,
        absTileEndY: Int,
        absTileEndX: Int,
        absTileStartY: Int,
        pixelStartX: Int,
        pixelStartY: Int,
        pixelEndX: Int,
        pixelEndY: Int
    ) {
        tileViewStartX = absTileStartX - mapSquareAbsX
        tileViewEndY = absTileEndY - mapSquareAbsY
        tileViewEndX = absTileEndX - mapSquareAbsX
        tileViewStartY = absTileStartY - mapSquareAbsY
        pixelViewStartX = pixelStartX
        pixelViewStartY = pixelStartY
        pixelViewEndX = pixelEndX
        pixelViewEndY = pixelEndY
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
        locIds: IntArray?,
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

        if (locIds != null) {
            val endX = if (width == 1) x else width + x - 1
            val endY = if (height == 1) y else y + height - 1

            for (index in locIds.indices) {
                val shape = locShapesAndRotations!![index].toInt() and 0x3f
                if (shape == ObjectShapes.WALL_STRAIGHT.shape || shape == ObjectShapes.WALL_WHOLE_CORNER.shape ||
                    shape == ObjectShapes.WALL_STRAIGHT_CORNER.shape || shape == ObjectShapes.WALL_INTERACT.shape
                ) {
                    val locType = LOCTYPE_LIST!!.list(locIds[index])
                    if (locType!!.mapIconId == -1) {
                        var color = -3355444
                        if (locType.hasActions == 1) {
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
                                    for (i in 0 until height) {
                                        toolkit.drawTransparentHorizontalLine(i + x, i + y, 1, color, 0)
                                    }
                                } else {
                                    for (i in 0 until height) {
                                        toolkit.drawTransparentHorizontalLine(i + x, endY - i, 1, color, 0)
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
                val category = WorldMapCategory(staticElements!!.mapCategoryIds[index])
                category.absX = placement[1] - mapSquareAbsX
                category.absY = placement[2] - mapSquareAbsY
                worldMapCategories.addFirst(category)
            }
        }
    }

    @JvmStatic
    fun positionCategory(category: WorldMapCategory, pixelsPerTileX: Int, pixelsPerTileY: Int) {
        category.screenX = (pixelsPerTileX * (category.absX - tileViewStartX) shr 16) + pixelViewStartX
        category.screenY = pixelViewEndY - (pixelsPerTileY * (category.absY - tileViewStartY) shr 16)
    }

    @JvmStatic
    fun renderCategoryLines(toolkit: AbstractToolkit, category: WorldMapCategory, mecType: MECType) {
        if (mecType.offsets != null) {
            val polygonPoints = IntArray(mecType.offsets!!.size)
            for (index in 0 until polygonPoints.size / 2) {
                val absX = mecType.offsets!![index * 2] + category.absX
                val absY = category.absY + mecType.offsets!![index * 2 + 1]
                polygonPoints[index * 2] = pixelViewStartX + (pixelViewEndX - pixelViewStartX) * (absX - tileViewStartX) / (tileViewEndX - tileViewStartX)
                polygonPoints[index * 2 + 1] = pixelViewEndY - (pixelViewEndY - pixelViewStartY) * (absY - tileViewStartY) / (tileViewEndY - tileViewStartY)
            }

            WorldMapRasterManager.fillPolygon(toolkit, polygonPoints, mecType.lineColor)
            if (mecType.dashLineSpacing > 0) {
                for (index in 0 until polygonPoints.size / 2 - 1) {
                    var startX = polygonPoints[index * 2]
                    var startY = polygonPoints[index * 2 + 1]
                    var endX = polygonPoints[(index + 1) * 2]
                    var endY = polygonPoints[(index + 1) * 2 + 1]
                    if (endX < startX) {
                        val tmpX = startX
                        val tmpY = startY
                        startX = endX
                        startY = endY
                        endX = tmpX
                        endY = tmpY
                    } else if (endX == startX && endY < startY) {
                        val tmpY = startY
                        startY = endY
                        endY = tmpY
                    }

                    toolkit.drawLine(startX, startY, endX, endY, mecType.colors!![mecType.colorPointers!![index].toInt() and 0xff], mecType.dashLineSpacing, mecType.dashLineLength, mecType.dashLineOffset)
                }

                var startX = polygonPoints[polygonPoints.size - 2]
                var startY = polygonPoints[polygonPoints.size - 1]
                var endX = polygonPoints[0]
                var endY = polygonPoints[1]
                if (endX < startX) {
                    val tmpX = startX
                    val tmpY = startY
                    startX = endX
                    startY = endY
                    endX = tmpX
                    endY = tmpY
                } else if (endX == startX && endY < startY) {
                    val tmpY = startY
                    startY = endY
                    endY = tmpY
                }

                toolkit.drawLine(startX, startY, endX, endY, mecType.colors!![mecType.colorPointers!![mecType.colorPointers!!.size - 1].toInt() and 0xff], mecType.dashLineSpacing, mecType.dashLineLength, mecType.dashLineOffset)
            } else {
                for (index in 0 until polygonPoints.size / 2 - 1) {
                    toolkit.drawOpaqueLine(polygonPoints[index * 2], polygonPoints[index * 2 + 1], polygonPoints[(index + 1) * 2], polygonPoints[(index + 1) * 2 + 1], mecType.colors!![mecType.colorPointers!![index].toInt() and 0xff])
                }
                toolkit.drawOpaqueLine(polygonPoints[polygonPoints.size - 2], polygonPoints[polygonPoints.size - 1], polygonPoints[0], polygonPoints[1], mecType.colors!![mecType.colorPointers!![mecType.colorPointers!!.size - 1].toInt() and 0xff])
            }
        }
    }

    @JvmStatic
    fun prepareMapdata(buffer: Packet, mapSquareX: Int, mapSquareY: Int, x: Int, y: Int, underlayIdTable: IntArray, overlayIdTable: IntArray) {
        val flags = buffer.readUByte()
        if (flags and 0x1 == 0) {
            val isUnderlay = flags and 0x2 == 0
            var floorId = flags shr 2 and 0x3f
            if (floorId != 62) {
                if (floorId == 63) {
                    floorId = buffer.readUByte()
                } else if (isUnderlay) {
                    floorId = underlayIdTable[floorId]
                } else {
                    floorId = overlayIdTable[floorId]
                }

                if (isUnderlay) {
                    underlayIds!![y * mapSizeX + x] = floorId.toByte()
                    overlayIds!![y * mapSizeX + x] = 0
                } else {
                    overlayIds!![y * mapSizeX + x] = floorId.toByte()
                    tileShapeAndRotations!![y * mapSizeX + x] = 0
                    underlayIds!![y * mapSizeX + x] = buffer.readByte()
                }
            }
        } else {
            val levelCount = (flags shr 1 and 0x3) + 1
            val hasOverlay = flags and 0x8 != 0
            val hasLocs = flags and 0x10 != 0

            for (level in 0 until levelCount) {
                val underlayId = buffer.readUByte()
                var overlayId = 0
                var tileShapeAndRotation = 0
                if (hasOverlay) {
                    overlayId = buffer.readUByte()
                    tileShapeAndRotation = buffer.readUByte()
                }

                var locCount = 0
                if (hasLocs) {
                    locCount = buffer.readUByte()
                }

                val locIds: IntArray?
                val locShapesAndRotations: ByteArray?
                if (level == 0) {
                    underlayIds!![y * mapSizeX + x] = underlayId.toByte()
                    overlayIds!![y * mapSizeX + x] = overlayId.toByte()
                    tileShapeAndRotations!![y * mapSizeX + x] = tileShapeAndRotation.toByte()
                    if (locCount == 1) {
                        tileDrawInfo!![y * mapSizeX + x] = buffer.readBigSmart()
                        shapesAndRotations!![y * mapSizeX + x] = buffer.readByte()
                    } else if (locCount > 1) {
                        locIds = IntArray(locCount)
                        locShapesAndRotations = ByteArray(locCount)

                        for (index in 0 until locCount) {
                            locIds[index] = buffer.readBigSmart()
                            locShapesAndRotations[index] = buffer.readByte()
                        }

                        tileDrawInfo!![y * mapSizeX + x] = LocOnWorldMap(locIds, locShapesAndRotations)
                    }
                } else {
                    if (locCount > 0) {
                        locIds = IntArray(locCount)
                        locShapesAndRotations = ByteArray(locCount)

                        for (index in 0 until locCount) {
                            locIds[index] = buffer.readBigSmart()
                            locShapesAndRotations[index] = buffer.readByte()
                        }
                    } else {
                        locIds = null
                        locShapesAndRotations = null
                    }

                    if (worldMapTiles!![level - 1][mapSquareX - (mapSquareAbsX shr 6)][mapSquareY - (mapSquareAbsY shr 6)] == null) {
                        worldMapTiles!![level - 1][mapSquareX - (mapSquareAbsX shr 6)][mapSquareY - (mapSquareAbsY shr 6)] = ArrayList()
                    }

                    val tile = TileOnWorldMap(x and 0x3f, y and 0x3f, underlayId, overlayId, tileShapeAndRotation, locIds, locShapesAndRotations)
                    worldMapTiles!![level - 1][mapSquareX - (mapSquareAbsX shr 6)][mapSquareY - (mapSquareAbsY shr 6)]!!.add(tile)
                }
            }
        }
    }

    @JvmStatic
    fun getOverlayFloorColor(materialProvider: MaterialProvider, overlayId: Int, hueOffset: Int, lightnessOffset: Int): Int {
        val overlay = FLOTYPE_LIST!!.list(overlayId) ?: return 0
        var textureId = overlay.texture
        if (textureId >= 0 && materialProvider.getMaterial(textureId)!!.detailsOnly) {
            textureId = -1
        }

        val finalFloorColor: Int
        if (overlay.minimapColorHsl >= 0) {
            val colorHsl = overlay.minimapColorHsl
            var lightness = lightnessOffset + (colorHsl and 0x7f)
            if (lightness < 0) {
                lightness = 0
            } else if (lightness > 127) {
                lightness = 127
            }

            val modifiedHsl = lightness + ((colorHsl + hueOffset) and 0xfc00) + (colorHsl and 0x380)
            finalFloorColor = 0xff000000.toInt() or ColorUtil.hsvToRgbTable!![(ColorUtil.adjustHsv(ColorUtil.repackHslWithSentinel(modifiedHsl, 96)).toInt() and 0xffff)]
        } else if (textureId >= 0) {
            finalFloorColor = 0xff000000.toInt() or ColorUtil.hsvToRgbTable!![(ColorUtil.adjustHsv(ColorUtil.repackHslWithSentinel(materialProvider.getMaterial(textureId)!!.colorHsl.toInt(), 96)).toInt() and 0xffff)]
        } else if (overlay.tileColorHsl == -1) {
            finalFloorColor = 0
        } else {
            val colorHsl = overlay.tileColorHsl
            var lightness = lightnessOffset + (colorHsl and 0x7f)
            if (lightness < 0) {
                lightness = 0
            } else if (lightness > 127) {
                lightness = 127
            }

            val modifiedHsl = lightness + ((colorHsl + hueOffset) and 0xfc00) + (colorHsl and 0x380)
            finalFloorColor = 0xff000000.toInt() or ColorUtil.hsvToRgbTable!![(ColorUtil.adjustHsv(ColorUtil.repackHslWithSentinel(modifiedHsl, 96)).toInt() and 0xffff)]
        }

        return finalFloorColor
    }

    @JvmStatic
    fun renderObjectSprites(toolkit: AbstractToolkit, x: Int, y: Int, width: Int, height: Int, locIds: IntArray?, locShapesAndRotations: ByteArray?) {
        if (locIds != null) {
            for (index in locIds.indices) {
                val locType = LOCTYPE_LIST!!.list(locIds[index])
                val mapIconId = locType!!.mapIconId
                if (mapIconId != -1) {
                    val mapSpriteType = MAP_SPRITE_TYPE_LIST!!.list(mapIconId)
                    val nativeSprite = mapSpriteType!!.createRotatedNativeSprite(toolkit, if (locType.mapIconRotates) locShapesAndRotations!![index].toInt() shr 6 and 0x3 else 0, locType.mapIconFlipped && locType.inverted)
                    if (nativeSprite != null) {
                        var drawWidth = width * nativeSprite.getSpriteWidth() shr 2
                        var drawHeight = height * nativeSprite.getSpriteHeight() shr 2
                        if (mapSpriteType.requiresUpscaling) {
                            var sizeX = locType.sizeX
                            var sizeY = locType.sizeY
                            if (locShapesAndRotations!![index].toInt() shr 6 and 0x1 == 1) {
                                val tmpSizeX = sizeX
                                sizeX = sizeY
                                sizeY = tmpSizeX
                            }

                            drawWidth = sizeX * width
                            drawHeight = sizeY * height
                        }

                        if (drawWidth != 0 && drawHeight != 0) {
                            if (mapSpriteType.backgroundColor != 0) {
                                nativeSprite.drawCircle(x, height + (y - drawHeight), drawWidth, drawHeight, 0, 0xff000000.toInt() or mapSpriteType.backgroundColor, 1)
                            } else {
                                nativeSprite.drawCircle(x, height + (y - drawHeight), drawWidth, drawHeight)
                            }
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun getMapDef(coordX: Int, coordY: Int): WorldMapElement? {
        var mapElement = WORLDMAP_DEFS.first() as? WorldMapElement
        while (mapElement != null) {
            if (mapElement.shouldRender && mapElement.allElementsInBounds(coordX, coordY)) {
                return mapElement
            }
            mapElement = WORLDMAP_DEFS.next() as? WorldMapElement
        }
        return null
    }

    @JvmStatic
    fun positionAllCategories(pixelsPerTileX: Int, pixelsPerTileY: Int): NodeCollection {
        var category = worldMapCategories.getFirst() as? WorldMapCategory
        while (category != null) {
            positionCategory(category, pixelsPerTileX, pixelsPerTileY)
            category = worldMapCategories.next() as? WorldMapCategory
        }

        return worldMapCategories
    }

    @JvmStatic
    fun renderMap(toolkit: AbstractToolkit) {
        val tileCountX = tileViewEndX - tileViewStartX
        val tileCountY = tileViewEndY - tileViewStartY
        val pixelsPerTileX = (pixelViewEndX - pixelViewStartX shl 16) / tileCountX
        val pixelsPerTileY = (pixelViewEndY - pixelViewStartY shl 16) / tileCountY
        renderMap(toolkit, pixelsPerTileX, pixelsPerTileY)
    }

    @JvmStatic
    fun loadMapTileData(hueOffset: Int, lightnessOffset: Int) {
        val buffer = Packet(MAP_AREA_RESOURCE!!.getNamedFile(staticElement!!.filenamePrefix, "area")!!)
        val underlayCount = buffer.readUByte()
        val underlayIdTable = IntArray(underlayCount)

        for (index in 0 until underlayCount) {
            underlayIdTable[index] = buffer.readUByte()
        }

        val overlayCount = buffer.readUByte()
        val overlayIdTable = IntArray(overlayCount)

        for (index in 0 until overlayCount) {
            overlayIdTable[index] = buffer.readUByte()
        }

        while (true) {
            while (buffer.currentPosition < buffer.buffer.size) {
                if (buffer.readUByte() == 0) {
                    val mapSquareX = buffer.readUByte()
                    val mapSquareY = buffer.readUByte()

                    for (tileX in 0..63) {
                        for (tileY in 0..63) {
                            val x = mapSquareX * 64 + tileX - mapSquareAbsX
                            val y = mapSquareY * 64 + tileY - mapSquareAbsY
                            prepareMapdata(buffer, mapSquareX, mapSquareY, x, y, underlayIdTable, overlayIdTable)
                        }
                    }
                } else {
                    val mapSquareX = buffer.readUByte()
                    val mapSquareY = buffer.readUByte()
                    val chunkX = buffer.readUByte()
                    val chunkY = buffer.readUByte()

                    for (chunkPosX in 0..7) {
                        for (chunkPosY in 0..7) {
                            val x = mapSquareX * 64 + chunkPosX + chunkX * 8 - mapSquareAbsX
                            val y = mapSquareY * 64 + chunkPosY + chunkY * 8 - mapSquareAbsY
                            prepareMapdata(buffer, mapSquareX, mapSquareY, x, y, underlayIdTable, overlayIdTable)
                        }
                    }
                }
            }

            underlayRgbColors = ByteArray(mapSizeX * mapSizeY)
            underlayGreenBlue = ShortArray(mapSizeX * mapSizeY)

            for (plane in 0..2) {
                val underlays = ByteArray(mapSizeX * mapSizeY)

                for (mapSquareX in 0 until worldMapTiles!![plane].size) {
                    for (mapSquareY in 0 until worldMapTiles!![plane][0].size) {
                        val tiles = worldMapTiles!![plane][mapSquareX][mapSquareY]
                        if (tiles != null) {
                            for (tile in tiles) {
                                underlays[mapSquareX * 64 + tile.mapsquarePosX + (mapSquareY * 64 + tile.mapsquarePosY) * mapSizeX] = tile.underlayIdOrColor.toByte()
                            }
                        }
                    }
                }

                decodeUnderlayColors(underlays, underlayRgbColors!!, underlayGreenBlue!!, hueOffset, lightnessOffset)

                for (mapSquareX in 0 until worldMapTiles!![plane].size) {
                    for (mapSquareY in 0 until worldMapTiles!![plane][0].size) {
                        val tiles = worldMapTiles!![plane][mapSquareX][mapSquareY]
                        if (tiles != null) {
                            for (tile in tiles) {
                                val tileIndex = mapSquareX * 64 + tile.mapsquarePosX + (mapSquareY * 64 + tile.mapsquarePosY) * mapSizeX
                                tile.underlayIdOrColor = (underlayRgbColors!![tileIndex].toInt() and 0xff shl 16) or (underlayGreenBlue!![tileIndex].toInt() and 0xffff)
                                if (tile.underlayIdOrColor != 0) {
                                    tile.underlayIdOrColor = tile.underlayIdOrColor or 0xff000000.toInt()
                                }
                            }
                        }
                    }
                }
            }

            decodeUnderlayColors(underlayIds!!, underlayRgbColors!!, underlayGreenBlue!!, hueOffset, lightnessOffset)
            underlayIds = null
            buildMapCategories()
            return
        }
    }

    @JvmStatic
    fun decodeUnderlayColors(tileUnderlayIds: ByteArray, tileRed: ByteArray, tileGreenBlue: ShortArray, hueOffset: Int, lightnessOffset: Int) {
        val underlayHue = IntArray(mapSizeY)
        val underlaySaturation = IntArray(mapSizeY)
        val underlayLightness = IntArray(mapSizeY)
        val underlayDivisor = IntArray(mapSizeY)
        val underlayTileCount = IntArray(mapSizeY)

        for (x in -5 until mapSizeX) {
            val positiveX = x + 5
            val negativeX = x - 5

            for (y in 0 until mapSizeY) {
                if (positiveX < mapSizeX) {
                    val underlayId = tileUnderlayIds[positiveX + y * mapSizeX].toInt() and 0xff
                    if (underlayId > 0) {
                        val underlay = FLUTYPE_LIST!!.list(underlayId - 1)
                        underlayHue[y] += underlay!!.hue
                        underlaySaturation[y] += underlay.saturation
                        underlayLightness[y] += underlay.lightness
                        underlayDivisor[y] += underlay.hueMultiplier
                        underlayTileCount[y]++
                    }
                }

                if (negativeX >= 0) {
                    val underlayId = tileUnderlayIds[negativeX + y * mapSizeX].toInt() and 0xff
                    if (underlayId > 0) {
                        val underlay = FLUTYPE_LIST!!.list(underlayId - 1)
                        underlayHue[y] -= underlay!!.hue
                        underlaySaturation[y] -= underlay.saturation
                        underlayLightness[y] -= underlay.lightness
                        underlayDivisor[y] -= underlay.hueMultiplier
                        underlayTileCount[y]--
                    }
                }
            }

            if (x >= 0) {
                var hue = 0
                var saturation = 0
                var lightness = 0
                var divisor = 0
                var tileCount = 0

                for (y in -5 until mapSizeY) {
                    val positiveY = y + 5
                    if (positiveY < mapSizeY) {
                        hue += underlayHue[positiveY]
                        saturation += underlaySaturation[positiveY]
                        lightness += underlayLightness[positiveY]
                        divisor += underlayDivisor[positiveY]
                        tileCount += underlayTileCount[positiveY]
                    }

                    val negativeY = y - 5
                    if (negativeY >= 0) {
                        hue -= underlayHue[negativeY]
                        saturation -= underlaySaturation[negativeY]
                        lightness -= underlayLightness[negativeY]
                        divisor -= underlayDivisor[negativeY]
                        tileCount -= underlayTileCount[negativeY]
                    }

                    if (y >= 0 && tileCount > 0) {
                        if (tileUnderlayIds[x + y * mapSizeX].toInt() and 0xff == 0) {
                            val tileIndex = x + y * mapSizeX
                            tileRed[tileIndex] = 0
                            tileGreenBlue[tileIndex] = 0
                        } else {
                            val hsl24 = if (divisor == 0) 0 else ColorUtil.hsl16to24(hue * 256 / divisor, saturation / tileCount, lightness / tileCount)
                            var clampedLightness = (hsl24 and 0x7f) + lightnessOffset
                            if (clampedLightness < 0) {
                                clampedLightness = 0
                            } else if (clampedLightness > 127) {
                                clampedLightness = 127
                            }

                            val modifiedHsl = clampedLightness + ((hueOffset + hsl24) and 0xfc00) + (hsl24 and 0x380)
                            val tileIndex = x + y * mapSizeX
                            val rgb = ColorUtil.hsvToRgbTable!![(ColorUtil.adjustHsv(ColorUtil.repackHslSafe(modifiedHsl, 96)).toInt() and 0xffff)]
                            tileRed[tileIndex] = ((rgb shr 16) and 0xff).toByte()
                            tileGreenBlue[tileIndex] = (rgb and 0xffff).toShort()
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun renderWorldmapTile(
        toolkit: AbstractToolkit,
        tileShapePatterns: Array<Array<ByteArray?>>,
        halfZoomLevel: Int,
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
        if (shape != 0 && halfZoomLevel != 0) {
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
            toolkit.renderWorldmapTile(x, y, width, height, underlayColor, overlayColor, tileShapePatterns[shape - 1][rotation]!!, halfZoomLevel, clearType)
        }
    }

    @JvmStatic
    fun getNextActiveCategory(): WorldMapCategory? {
        var category = WorldMapDisplay.categoryIterator.next() as? WorldMapCategory
        while (category != null) {
            val mecType = MECTYPE_LIST!!.list(category.mecId)
            if (mecType != null && mecType.visible && mecType.shouldDraw(PLAYER_VAR_PROVIDER!!)) {
                return category
            }
            category = WorldMapDisplay.categoryIterator.next() as? WorldMapCategory
        }
        return null
    }

    @JvmStatic
    fun drawWorldmap(toolkit: AbstractToolkit, materials: MaterialProvider, left: Int, top: Int, width: Int, height: Int) {
        if (ComponentMinimap.mapAssetLoadingPercentage < 100) {
            WorldMapDisplay.loadAssets(toolkit, materials)
        }
        toolkit.setClipping(left, top, left + width, top + height)
        if (ComponentMinimap.mapAssetLoadingPercentage < 100) {
            val textOffsetY: Byte = 20
            val centerX = width / 2 + left
            val centerY = top + height / 2 - 18 - textOffsetY
            toolkit.drawFilledRectangle(left, top, width, height, 0xff000000.toInt(), 0)
            toolkit.drawRectangleOutline(centerX - 152, centerY, 304, 34, Client.DEBUG_BG_COLOR[Client.DEBUG_DISPLAY_COLOR_INDEX]!!.getRGB(), 0)
            toolkit.drawFilledRectangle(centerX - 150, centerY + 2, ComponentMinimap.mapAssetLoadingPercentage * 3, 30, Client.DEBUG_LOAD_COLOR[Client.DEBUG_DISPLAY_COLOR_INDEX]!!.getRGB(), 0)
            MiniMenu.minimenuHeader!!.renderCentered(LocalizedText.LOADING.translate(Client.CURRENT_LANGUAGE!!), centerX, textOffsetY + centerY, Client.DEBUG_TEXT_COLOR[Client.DEBUG_DISPLAY_COLOR_INDEX]!!.getRGB(), -1)
        } else {
            val viewLeftTileX = WorldMapDisplay.mapCenterX - (width / mapZoom).toInt()
            val viewTopTileY = (height / mapZoom).toInt() + WorldMapDisplay.mapCenterY
            val viewRightTileX = (width / mapZoom).toInt() + WorldMapDisplay.mapCenterX
            val viewBottomTileY = WorldMapDisplay.mapCenterY - (height / mapZoom).toInt()
            DebugDisplay.worldMapViewLeftTileX = WorldMapDisplay.mapCenterX - (width / mapZoom).toInt()
            WorldMapDisplay.viewBottomTileY = WorldMapDisplay.mapCenterY - (height / mapZoom).toInt()
            StaticMethods.worldMapViewWidth = ((width * 2) / mapZoom).toInt()
            ComponentMinimap.mapViewportScale = ((height * 2) / mapZoom).toInt()
            setViewportBounds(viewLeftTileX + mapSquareAbsX, viewTopTileY + mapSquareAbsY, viewRightTileX + mapSquareAbsX, viewBottomTileY + mapSquareAbsY, left, top, left + width, top + height + 1)
            renderMap(toolkit)
            val categories = getViewportCategories()
            WorldMapDisplay.renderInformation(toolkit, categories)
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
                val usedMemoryKb = ((runtime.totalMemory() - runtime.freeMemory()) / 1024L).toInt()
                var textColor = 16776960
                if (usedMemoryKb > 65536) {
                    textColor = 16711680
                }
                FontManager.debugOverlayFont!!.renderLeftAligned("Mem:" + usedMemoryKb + "k", textX, textY, textColor, -1)
                textY -= 15
            }
        }
    }
}
