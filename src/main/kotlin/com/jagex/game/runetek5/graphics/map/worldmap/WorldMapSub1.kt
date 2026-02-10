package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.cache.node.NodeIterator
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.GraphicsCache
import com.jagex.game.runetek5.client.JSObjectManager
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.ServerEnvironment
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.mec.MECType
import com.jagex.game.runetek5.config.staticelements.StaticElementTypeList
import com.jagex.game.runetek5.config.staticelements.StaticElements
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.graphics.font.HelveticaFont
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.movement.CoordGrid
import com.jagex.game.runetek5.network.PingRequester
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.BufferUtils
import com.jagex.game.runetek5.utils.Utils
import kotlin.math.atan2

object WorldMapSub1 {

    @JvmField
    var hideIcons = false

    @JvmField
    val hiddenMecIds = IterableNodeMap<Node<Any?>>(8)

    @JvmField
    val hiddenCategoryIds = IterableNodeMap<Node<Any?>>(8)

    @JvmField
    var scrollTargetX = -1

    @JvmField
    var scrollTargetY = -1

    @JvmField
    var isInteractive = false

    @JvmField
    var requestedScrollX = -1

    @JvmField
    var requestedScrollY = -1

    @JvmField
    var useTargetPosition = false

    /**
     * (White) text above worldmap areas
     *
     * Toll<br>
     * gate
     *
     * Mage<br>
     * Training<br>
     * Arena
     */
    @JvmField
    val RENDERED_WORLDMAP_AREA_NAMELINES = arrayOfNulls<String>(5)

    @JvmField
    var someMecId = -1

    @JvmField
    var highlightedCategoryId = -1

    @JvmField
    val categoryIterator = NodeIterator(NodeCollection())

    @JvmField
    var mapCenterX = 0

    @JvmField
    var mapCenterY = 0

    @JvmField
    var viewBottomTileY = 0

    @JvmField
    var pendingMapElementId = 0

    @JvmStatic
    fun reset(i_0: Int, i_1: Int, i_2: Int, bool3: Boolean) {
        if (PreferenceManager.preferences?.activeToolkit?.getValue() != 0) {
            AbstractToolkit.activeRenderMode = PreferenceManager.preferences?.activeToolkit?.getValue() ?: 0
            Client.loadAssets(0, true)
        } else {
            GraphicsCache.resetWorldmapCaches(false)
        }
        requestedScrollX = i_1
        requestedScrollY = i_2
        useTargetPosition = bool3
        WorldMap.fetch(i_0)
    }

    @JvmStatic
    fun markDestination(x: Int, y: Int, width: Int, height: Int) {
        val fitXInY = WorldMap.mapSizeY.toFloat() / WorldMap.mapSizeX
        var offsetX = width
        var offsetY = height
        if (fitXInY < 1.0f) {
            offsetY = (width * fitXInY).toInt()
        } else {
            offsetX = (height / fitXInY).toInt()
        }
        val adjustedX = x - (width - offsetX) / 2
        val adjustedY = y - (height - offsetY) / 2
        mapCenterX = adjustedX * WorldMap.mapSizeX / offsetX
        mapCenterY = WorldMap.mapSizeY - WorldMap.mapSizeY * adjustedY / offsetY
        scrollTargetX = -1
        scrollTargetY = -1
        clampMapCenter()
    }

    @JvmStatic
    fun loadAssets(toolkit: AbstractToolkit, materialProvider: MaterialProvider) {
        if (WorldMap.staticElement != null) {
            Utils.time()
            Utils.time()
            if (ComponentMinimap.mapAssetLoadingPercentage < 10) {
                if (!WorldMap.MAP_AREA_RESOURCE!!.isCached(WorldMap.staticElement!!.filenamePrefix)) {
                    ComponentMinimap.mapAssetLoadingPercentage = Resource.WORLD_MAP!!.getArchiveLoadingPercent(WorldMap.staticElement!!.filenamePrefix) / 10
                    return
                }

                StaticMethods.resetSceneAndCaches()
                ComponentMinimap.mapAssetLoadingPercentage = 10
            }

            if (ComponentMinimap.mapAssetLoadingPercentage == 10) {
                WorldMap.mapSquareAbsX = WorldMap.staticElement!!.boundsMinX shr 6 shl 6
                WorldMap.mapSquareAbsY = WorldMap.staticElement!!.boundsMinY shr 6 shl 6
                WorldMap.mapSizeX = (WorldMap.staticElement!!.boundsMaxX shr 6 shl 6) - WorldMap.mapSquareAbsX + 64
                WorldMap.mapSizeY = (WorldMap.staticElement!!.boundsMaxY shr 6 shl 6) - WorldMap.mapSquareAbsY + 64
                val placement = IntArray(3)
                var i_4 = -1
                var i_5 = -1
                val absolute: Vector3 = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
                val base: CoordGrid = Resource.mapRegion!!.getBaseCoordinates()
                if (WorldMap.staticElement!!.gameToMapCoordsForPlane(PlayerUpdate.MY_PLAYER!!.plane.toInt(), (absolute.x.toInt() shr 9) + base.x, (absolute.z.toInt() shr 9) + base.y, placement)) {
                    i_4 = placement[1] - WorldMap.mapSquareAbsX
                    i_5 = placement[2] - WorldMap.mapSquareAbsY
                }

                if (!useTargetPosition && i_4 >= 0 && i_4 < WorldMap.mapSizeX && i_5 >= 0 && i_5 < WorldMap.mapSizeY) {
                    i_4 += (Math.random() * 10.0).toInt() - 5
                    i_5 += (Math.random() * 10.0).toInt() - 5
                    mapCenterX = i_4
                    mapCenterY = i_5
                } else if (requestedScrollX != -1 && requestedScrollY != -1) {
                    WorldMap.staticElement!!.gameToMapCoords(requestedScrollX, requestedScrollY, placement)
                    mapCenterX = placement[1] - WorldMap.mapSquareAbsX
                    mapCenterY = placement[2] - WorldMap.mapSquareAbsY

                    requestedScrollY = -1
                    requestedScrollX = -1
                    useTargetPosition = false
                } else {
                    WorldMap.staticElement!!.gameToMapCoords(WorldMap.staticElement!!.bitpackedPlacement shr 14 and 0x3fff, WorldMap.staticElement!!.bitpackedPlacement and 0x3fff, placement)
                    mapCenterX = placement[1] - WorldMap.mapSquareAbsX
                    mapCenterY = placement[2] - WorldMap.mapSquareAbsY
                }

                when (WorldMap.staticElement!!.defaultZoomLevel) {
                    37 -> {
                        WorldMap.mapZoom = 3.0f
                        WorldMap.requestedMapZoom = 3.0f
                    }
                    50 -> {
                        WorldMap.mapZoom = 4.0f
                        WorldMap.requestedMapZoom = 4.0f
                    }
                    75 -> {
                        WorldMap.mapZoom = 6.0f
                        WorldMap.requestedMapZoom = 6.0f
                    }
                    100 -> {
                        WorldMap.mapZoom = 8.0f
                        WorldMap.requestedMapZoom = 8.0f
                    }
                    200 -> {
                        WorldMap.mapZoom = 16.0f
                        WorldMap.requestedMapZoom = 16.0f
                    }
                    else -> {
                        WorldMap.mapZoom = 8.0f
                        WorldMap.requestedMapZoom = 8.0f
                    }
                }

                WorldMap.halfZoomLevel = WorldMap.mapZoom.toInt() shr 1
                WorldMap.tileShapePatterns = BufferUtils.generateBytePatterns(WorldMap.halfZoomLevel)
                clampMapCenter()
                WorldMap.initialize()
                WorldMapInteraction.MEC_CLICKZONES = NodeCollection()
                WorldMap.hueRandomisation += (Math.random() * 5.0).toInt() - 2
                if (WorldMap.hueRandomisation < -8) {
                    WorldMap.hueRandomisation = -8
                }

                if (WorldMap.hueRandomisation > 8) {
                    WorldMap.hueRandomisation = 8
                }

                WorldMap.lightnessRandomisation += (Math.random() * 5.0).toInt() - 2
                if (WorldMap.lightnessRandomisation < -16) {
                    WorldMap.lightnessRandomisation = -16
                }

                if (WorldMap.lightnessRandomisation > 16) {
                    WorldMap.lightnessRandomisation = 16
                }

                WorldMap.calculateRunetekPalette(materialProvider, WorldMap.hueRandomisation shr 2 shl 10, WorldMap.lightnessRandomisation shr 1)
                WorldMap.MECTYPE_LIST!!.createCache(1024, 256)
                WorldMap.MAP_SPRITE_TYPE_LIST!!.resetCaches(256, 256)
                WorldMap.LOCTYPE_LIST!!.resetLruCache(4096)
                TypeLists.VARPBIT!!.resetCache(256)
                ComponentMinimap.mapAssetLoadingPercentage = 20
            } else if (ComponentMinimap.mapAssetLoadingPercentage == 20) {
                PingRequester.keepalive(true)
                WorldMap.loadMapTileData(WorldMap.hueRandomisation, WorldMap.lightnessRandomisation)
                ComponentMinimap.mapAssetLoadingPercentage = 60
                PingRequester.keepalive(true)
                GameShell.resetDrawCache()
            } else if (ComponentMinimap.mapAssetLoadingPercentage == 60) {
                if (WorldMap.MAP_AREA_RESOURCE!!.validFile(WorldMap.staticElement!!.filenamePrefix + "_staticelements")) {
                    if (!WorldMap.MAP_AREA_RESOURCE!!.isCached(WorldMap.staticElement!!.filenamePrefix + "_staticelements")) {
                        return
                    }

                    WorldMap.staticElements = StaticElementTypeList.list(WorldMap.MAP_AREA_RESOURCE!!, WorldMap.staticElement!!.filenamePrefix + "_staticelements", Client.memberWorld)
                } else {
                    WorldMap.staticElements = StaticElements(0)
                }

                WorldMap.loadWorldMapCategories()
                ComponentMinimap.mapAssetLoadingPercentage = 70
                PingRequester.keepalive(true)
                GameShell.resetDrawCache()
            } else if (ComponentMinimap.mapAssetLoadingPercentage == 70) {
                Resource.helvetica11Font = HelveticaFont(toolkit, 11, GameShell.gameCanvas!!)
                ComponentMinimap.mapAssetLoadingPercentage = 73
                PingRequester.keepalive(true)
                GameShell.resetDrawCache()
            } else if (ComponentMinimap.mapAssetLoadingPercentage == 73) {
                Resource.helvetica12Font = HelveticaFont(toolkit, 12, GameShell.gameCanvas!!)
                ComponentMinimap.mapAssetLoadingPercentage = 76
                PingRequester.keepalive(true)
                GameShell.resetDrawCache()
            } else if (ComponentMinimap.mapAssetLoadingPercentage == 76) {
                Resource.helvetica14Font = HelveticaFont(toolkit, 14, GameShell.gameCanvas!!)
                ComponentMinimap.mapAssetLoadingPercentage = 79
                PingRequester.keepalive(true)
                GameShell.resetDrawCache()
            } else if (ComponentMinimap.mapAssetLoadingPercentage == 79) {
                Resource.helvetica17Font = HelveticaFont(toolkit, 17, GameShell.gameCanvas!!)
                ComponentMinimap.mapAssetLoadingPercentage = 82
                PingRequester.keepalive(true)
                GameShell.resetDrawCache()
            } else if (ComponentMinimap.mapAssetLoadingPercentage == 82) {
                Resource.helvetica19Font = HelveticaFont(toolkit, 19, GameShell.gameCanvas!!)
                ComponentMinimap.mapAssetLoadingPercentage = 85
                PingRequester.keepalive(true)
                GameShell.resetDrawCache()
            } else if (ComponentMinimap.mapAssetLoadingPercentage == 85) {
                Resource.helvetica22Font = HelveticaFont(toolkit, 22, GameShell.gameCanvas!!)
                ComponentMinimap.mapAssetLoadingPercentage = 88
                PingRequester.keepalive(true)
                GameShell.resetDrawCache()
            } else if (ComponentMinimap.mapAssetLoadingPercentage == 88) {
                Resource.helvetica26Font = HelveticaFont(toolkit, 26, GameShell.gameCanvas!!)
                ComponentMinimap.mapAssetLoadingPercentage = 91
                PingRequester.keepalive(true)
                GameShell.resetDrawCache()
            } else {
                Resource.helvetica30Font = HelveticaFont(toolkit, 30, GameShell.gameCanvas!!)
                ComponentMinimap.mapAssetLoadingPercentage = 100
                PingRequester.keepalive(true)
                GameShell.resetDrawCache()
                System.gc()
            }
        }
    }

    @JvmStatic
    fun clampMapCenter() {
        if (mapCenterX < 0) {
            mapCenterX = 0
            scrollTargetX = -1
            scrollTargetY = -1
        }
        if (mapCenterX > WorldMap.mapSizeX) {
            mapCenterX = WorldMap.mapSizeX
            scrollTargetX = -1
            scrollTargetY = -1
        }
        if (mapCenterY < 0) {
            mapCenterY = 0
            scrollTargetX = -1
            scrollTargetY = -1
        }
        if (mapCenterY > WorldMap.mapSizeY) {
            mapCenterY = WorldMap.mapSizeY
            scrollTargetX = -1
            scrollTargetY = -1
        }
    }

    @JvmStatic
    fun setMapCenterX(i_0: Int) {
        mapCenterX = i_0
        scrollTargetX = -1
        scrollTargetY = -1
        clampMapCenter()
    }

    @JvmStatic
    fun getStaticElement(): WorldMapElement? {
        return WorldMap.staticElement
    }

    @JvmStatic
    fun setMapCenterY(i_0: Int) {
        mapCenterY = i_0
        scrollTargetY = -1
        clampMapCenter()
    }

    @JvmStatic
    fun highlightCategory(i_0: Int) {
        someMecId = -1
        highlightedCategoryId = i_0
        JSObjectManager.anInt4185 = 3
        ServerEnvironment.flashCycleCounter = 100
    }

    @JvmStatic
    fun renderIconsAndNames(toolkit: AbstractToolkit, mapCategory: WorldMapCategory, mectype: MECType): Boolean {
        var i_6 = Int.MAX_VALUE
        var i_7 = Int.MIN_VALUE
        var i_8 = Int.MAX_VALUE
        var i_9 = Int.MIN_VALUE
        if (mectype.offsets != null) {
            i_6 = (WorldMap.pixelViewEndX - WorldMap.pixelViewStartX) * (mectype.minOffsetX + mapCategory.absX - WorldMap.tileViewStartX) / (WorldMap.tileViewEndX - WorldMap.tileViewStartX) + WorldMap.pixelViewStartX
            i_7 = WorldMap.pixelViewStartX + (WorldMap.pixelViewEndX - WorldMap.pixelViewStartX) * (mectype.maxOffsetX + mapCategory.absX - WorldMap.tileViewStartX) / (WorldMap.tileViewEndX - WorldMap.tileViewStartX)
            i_9 = WorldMap.pixelViewEndY - (WorldMap.pixelViewEndY - WorldMap.pixelViewStartY) * (mectype.minOffsetY + mapCategory.absY - WorldMap.tileViewStartY) / (WorldMap.tileViewEndY - WorldMap.tileViewStartY)
            i_8 = WorldMap.pixelViewEndY - (WorldMap.pixelViewEndY - WorldMap.pixelViewStartY) * (mectype.maxOffsetY + mapCategory.absY - WorldMap.tileViewStartY) / (WorldMap.tileViewEndY - WorldMap.tileViewStartY)
        }
        var iconSprite: NativeSprite? = null
        var spriteLeft = 0
        var spriteRight = 0
        var spriteBottom = 0
        var spriteTop = 0
        if (mectype.defaultIconArchive != -1) {
            iconSprite = if (mapCategory.isMouseOver && mectype.mouseOverIconArchive != -1) {
                mectype.getIcon(toolkit, true)
            } else {
                mectype.getIcon(toolkit, false)
            }
            if (iconSprite != null) {
                spriteLeft = mapCategory.anInt7990 - (iconSprite.getSpriteWidth() + 1 shr 1)
                if (spriteLeft < i_6) {
                    i_6 = spriteLeft
                }
                spriteRight = mapCategory.anInt7990 + (iconSprite.getSpriteWidth() + 1 shr 1)
                if (spriteRight > i_7) {
                    i_7 = spriteRight
                }
                spriteBottom = mapCategory.anInt7992 - (iconSprite.getSpriteHeight() + 1 shr 1)
                if (spriteBottom < i_8) {
                    i_8 = spriteBottom
                }
                spriteTop = mapCategory.anInt7992 + (iconSprite.getSpriteHeight() + 1 shr 1)
                if (spriteTop > i_9) {
                    i_9 = spriteTop
                }
            }
        }
        var font: HelveticaFont? = null
        var i_16 = 0
        var i_17 = 0
        var i_18 = 0
        var i_19 = 0
        var textLeft = 0
        var textRight = 0
        var textBottom = 0
        var textTop = 0
        val areaName = mectype.areaName
        if (areaName != null) {
            font = getZoomedHelveticafont(mectype.baseTextzoom)
            if (font != null) {
                i_16 = FontManager.worldMapAreaFont!!.splitLinesToBuffer(areaName, null, RENDERED_WORLDMAP_AREA_NAMELINES, null)
                i_17 = (WorldMap.pixelViewEndX - WorldMap.pixelViewStartX) * mectype.anInt2750 / (WorldMap.tileViewEndX - WorldMap.tileViewStartX) + mapCategory.anInt7990
                i_18 = mapCategory.anInt7992 - (WorldMap.pixelViewEndY - WorldMap.pixelViewStartY) * mectype.anInt2752 / (WorldMap.tileViewEndY - WorldMap.tileViewStartY)
                if (iconSprite == null) {
                    i_18 -= font.method3186() * i_16 / 2
                } else {
                    i_18 -= (iconSprite.getSpriteHeight() shr 1) + font.getLineHeight() * i_16
                }
                for (index in 0 until i_16) {
                    val lineText = RENDERED_WORLDMAP_AREA_NAMELINES[index]
                    if (lineText != null) {
                        val areaName = if (index < i_16 - 1) {
                            lineText.substring(0, lineText.length - 4)
                        } else {
                            lineText
                        }
                        val areaNameWidth = font.getTextWidth(areaName)
                        if (areaNameWidth > i_19) {
                            i_19 = areaNameWidth
                        }
                    }
                }
                textLeft = i_17 - i_19 / 2
                if (textLeft < i_6) {
                    i_6 = textLeft
                }
                textRight = i_19 / 2 + i_17
                if (textRight > i_7) {
                    i_7 = textRight
                }
                textBottom = i_18
                if (textBottom < i_8) {
                    i_8 = textBottom
                }
                textTop = i_18 + i_16 * font.getLineHeight()
                if (textTop > i_9) {
                    i_9 = textTop
                }
            }
        }
        if (i_7 >= WorldMap.pixelViewStartX && i_6 <= WorldMap.pixelViewEndX && i_9 >= WorldMap.pixelViewStartY && i_8 <= WorldMap.pixelViewEndY) {
            WorldMap.renderCategoryLines(toolkit, mapCategory, mectype)
            if (iconSprite != null) {
                if (JSObjectManager.anInt4185 > 0 && (someMecId != -1 && someMecId == mapCategory.mecId || highlightedCategoryId != -1 && mectype.anInt2718 == highlightedCategoryId)) {
                    val color = if (ServerEnvironment.flashCycleCounter > 50) {
                        200 - ServerEnvironment.flashCycleCounter * 2
                    } else {
                        ServerEnvironment.flashCycleCounter * 2
                    }
                    val alpha = color shl 24 or 0xffff00
                    toolkit.drawMapCategory(mapCategory.anInt7990, mapCategory.anInt7992, iconSprite.getWidth() / 2 + 7, alpha)
                    toolkit.drawMapCategory(mapCategory.anInt7990, mapCategory.anInt7992, iconSprite.getWidth() / 2 + 5, alpha)
                    toolkit.drawMapCategory(mapCategory.anInt7990, mapCategory.anInt7992, iconSprite.getWidth() / 2 + 3, alpha)
                    toolkit.drawMapCategory(mapCategory.anInt7990, mapCategory.anInt7992, iconSprite.getWidth() / 2 + 1, alpha)
                    toolkit.drawMapCategory(mapCategory.anInt7990, mapCategory.anInt7992, iconSprite.getWidth() / 2, alpha)
                }
                iconSprite.draw(mapCategory.anInt7990 - (iconSprite.getSpriteWidth() shr 1), mapCategory.anInt7992 - (iconSprite.getSpriteHeight() shr 1))
            }
            if (mectype.areaName != null && font != null) {
                WorldMapElement.renderWorldmapAreaText(toolkit, mapCategory, mectype, i_16, i_17, i_18, i_19, font)
            }
            if (mectype.defaultIconArchive != -1 || mectype.areaName != null) {
                val mecClickzone = ClickzoneMEC(mapCategory)
                mecClickzone.spriteLeft = spriteLeft
                mecClickzone.spriteRight = spriteRight
                mecClickzone.spriteBottom = spriteBottom
                mecClickzone.spriteTop = spriteTop
                mecClickzone.textLeft = textLeft
                mecClickzone.textRight = textRight
                mecClickzone.textBottom = textBottom
                mecClickzone.textTop = textTop
                WorldMapInteraction.MEC_CLICKZONES!!.addFirst(mecClickzone)
            }
            return false
        } else {
            return true
        }
    }

    @JvmStatic
    fun getZoomedHelveticafont(predefinedTextZoom: Int): HelveticaFont? {
        if (predefinedTextZoom == 0) {
            if (WorldMap.mapZoom == 3.0f) {
                return Resource.helvetica11Font
            }
            if (WorldMap.mapZoom == 4.0f) {
                return Resource.helvetica12Font
            }
            if (WorldMap.mapZoom == 6.0f) {
                return Resource.helvetica14Font
            }
            if (WorldMap.mapZoom >= 8.0f) {
                return Resource.helvetica17Font
            }
        } else if (predefinedTextZoom == 1) {
            if (WorldMap.mapZoom == 3.0f) {
                return Resource.helvetica14Font
            }
            if (WorldMap.mapZoom == 4.0f) {
                return Resource.helvetica17Font
            }
            if (WorldMap.mapZoom == 6.0f) {
                return Resource.helvetica19Font
            }
            if (WorldMap.mapZoom >= 8.0f) {
                return Resource.helvetica22Font
            }
        } else if (predefinedTextZoom == 2) {
            if (WorldMap.mapZoom == 3.0f) {
                return Resource.helvetica19Font
            }
            if (WorldMap.mapZoom == 4.0f) {
                return Resource.helvetica22Font
            }
            if (WorldMap.mapZoom == 6.0f) {
                return Resource.helvetica26Font
            }
            if (WorldMap.mapZoom >= 8.0f) {
                return Resource.helvetica30Font
            }
        }
        return null
    }

    @JvmStatic
    fun renderInformation(toolkit: AbstractToolkit, collection: NodeCollection) {
        WorldMapInteraction.MEC_CLICKZONES!!.remove_all()
        if (!hideIcons) {
            var category = collection.getFirst() as? WorldMapCategory
            while (category != null) {
                val mecType = WorldMap.MECTYPE_LIST!!.list(category.mecId)
                if (shouldDraw(mecType)) {
                    val bool7 = renderIconsAndNames(toolkit, category, mecType)
                    if (bool7) {
                        renderOffscreenIndicator(toolkit, category, mecType)
                    }
                }
                category = collection.next() as? WorldMapCategory
            }
        }
    }

    @JvmStatic
    fun shouldDraw(mecType: MECType?): Boolean {
        return mecType != null && mecType.displayedOnWorldmap && mecType.shouldDraw(WorldMap.PLAYER_VAR_PROVIDER!!) &&
               hiddenMecIds.get(mecType.mectypeId.toLong()) == null && hiddenCategoryIds.get(mecType.anInt2718.toLong()) == null
    }

    @JvmStatic
    fun renderOffscreenIndicator(toolkit: AbstractToolkit, category: WorldMapCategory, mectype: MECType) {
        val sprite = mectype.getSprite(toolkit) ?: return

        var spriteDimension = sprite.getWidth()
        if (sprite.getHeight() > spriteDimension) {
            spriteDimension = sprite.getHeight()
        }

        val b_6: Byte = 10
        var spriteCenterX = category.anInt7990
        var spriteCenterY = category.anInt7992
        var i_9 = 0
        var i_10 = 0
        var i_11 = 0
        var i_14: Int
        val mecAreaName = mectype.areaName
        if (mecAreaName != null) {
            i_9 = FontManager.worldMapAreaFont!!.splitLinesToBuffer(mecAreaName, null, RENDERED_WORLDMAP_AREA_NAMELINES, null)

            for (iii in 0 until i_9) {
                val lineText = RENDERED_WORLDMAP_AREA_NAMELINES[iii]
                if (lineText != null) {
                    val string_22 = if (iii < i_9 - 1) {
                        lineText.substring(0, lineText.length - 4)
                    } else {
                        lineText
                    }

                    i_14 = Resource.helvetica14Font!!.getTextWidth(string_22)
                    if (i_14 > i_10) {
                        i_10 = i_14
                    }
                }
            }

            i_11 = Resource.helvetica14Font!!.getLineHeight() * i_9 + Resource.helvetica14Font!!.method3186() / 2
        }

        var x = spriteDimension / 2 + category.anInt7990
        var y = category.anInt7992
        if (spriteCenterX < spriteDimension + WorldMap.pixelViewStartX) {
            spriteCenterX = WorldMap.pixelViewStartX
            x = spriteDimension / 2 + i_10 / 2 + b_6 + WorldMap.pixelViewStartX + 5
        } else if (spriteCenterX > WorldMap.pixelViewEndX - spriteDimension) {
            spriteCenterX = WorldMap.pixelViewEndX - spriteDimension
            x = WorldMap.pixelViewEndX - spriteDimension / 2 - b_6 - i_10 / 2 - 5
        }

        if (spriteCenterY < spriteDimension + WorldMap.pixelViewStartY) {
            spriteCenterY = WorldMap.pixelViewStartY
            y = spriteDimension / 2 + b_6 + WorldMap.pixelViewStartY
        } else if (spriteCenterY > WorldMap.pixelViewEndY - spriteDimension) {
            spriteCenterY = WorldMap.pixelViewEndY - spriteDimension
            y = WorldMap.pixelViewEndY - spriteDimension / 2 - b_6 - i_11
        }

        i_14 = (atan2((spriteCenterX - category.anInt7990).toDouble(), (spriteCenterY - category.anInt7992).toDouble()) / Math.PI * 32767.0).toInt() and 0xffff
        sprite.drawRotatedCenter(spriteCenterX + spriteDimension / 2.0f, spriteDimension / 2.0f + spriteCenterY, 4096, i_14)
        var textLeft = -2
        var textBottom = -2
        var textRight = -2
        var textTop = -2
        if (mectype.areaName != null) {
            textLeft = x - i_10 / 2 - 5
            textBottom = y
            textRight = textLeft + i_10 + 10
            textTop = y + Resource.helvetica14Font!!.getLineHeight() * i_9 + 3
            if (mectype.backgroundColor != 0) {
                toolkit.clearPixels(textLeft, y, textRight - textLeft, textTop - y, mectype.backgroundColor)
            }

            if (mectype.outlineColor != 0) {
                toolkit.drawOpaqueRectangle(textLeft, y, textRight - textLeft, textTop - y, mectype.outlineColor)
            }

            for (i_19 in 0 until i_9) {
                val lineText = RENDERED_WORLDMAP_AREA_NAMELINES[i_19]
                if (lineText != null) {
                    val text = if (i_19 < i_9 - 1) {
                        lineText.substring(0, lineText.length - 4)
                    } else {
                        lineText
                    }

                    Resource.helvetica14Font!!.drawClippedText(toolkit, text, x, y, mectype.defaultTextColor)
                    y += Resource.helvetica14Font!!.getLineHeight()
                }
            }
        }

        if (mectype.defaultIconArchive != -1 || mectype.areaName != null) {
            val clickzone = ClickzoneMEC(category)
            var halfDimension = spriteDimension shr 1
            clickzone.spriteLeft = spriteCenterX - halfDimension
            clickzone.spriteRight = spriteCenterX + halfDimension
            clickzone.spriteBottom = spriteCenterY - halfDimension
            clickzone.spriteTop = spriteCenterY + halfDimension
            clickzone.textLeft = textLeft
            clickzone.textRight = textRight
            clickzone.textBottom = textBottom
            clickzone.textTop = textTop
            WorldMapInteraction.MEC_CLICKZONES!!.addFirst(clickzone)
        }
    }

    @JvmStatic
    fun getFirstActiveCategory(): WorldMapCategory? {
        categoryIterator.reinitialize(WorldMap.worldMapCategories)
        val category = categoryIterator.nextAndReset() as? WorldMapCategory ?: return null
        val mectype = WorldMap.MECTYPE_LIST!!.list(category.mecId)
        return if (mectype != null && mectype.visible && mectype.shouldDraw(WorldMap.PLAYER_VAR_PROVIDER!!)) {
            category
        } else {
            WorldMap.getNextActiveCategory()
        }
    }

    @JvmStatic
    fun reset() {
        if (pendingMapElementId != -1) {
            reset(pendingMapElementId, -1, -1, false)
            pendingMapElementId = -1
        }
    }
}
