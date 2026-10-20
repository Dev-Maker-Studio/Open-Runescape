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

object WorldMapDisplay {

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
    fun reset(mapElementId: Int, scrollX: Int, scrollY: Int, useTarget: Boolean) {
        if (PreferenceManager.preferences?.activeToolkit?.getValue() != 0) {
            AbstractToolkit.activeRenderMode = PreferenceManager.preferences?.activeToolkit?.getValue() ?: 0
            Client.loadAssets(true)
        } else {
            GraphicsCache.resetWorldmapCaches(false)
        }
        requestedScrollX = scrollX
        requestedScrollY = scrollY
        useTargetPosition = useTarget
        WorldMap.fetch(mapElementId)
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
                var playerMapX = -1
                var playerMapY = -1
                val absolute: Vector3 = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
                val base: CoordGrid = Resource.mapRegion!!.getBaseCoordinates()
                if (WorldMap.staticElement!!.gameToMapCoordsForPlane(PlayerUpdate.MY_PLAYER!!.plane.toInt(), (absolute.x.toInt() shr 9) + base.x, (absolute.z.toInt() shr 9) + base.y, placement)) {
                    playerMapX = placement[1] - WorldMap.mapSquareAbsX
                    playerMapY = placement[2] - WorldMap.mapSquareAbsY
                }

                if (!useTargetPosition && playerMapX >= 0 && playerMapX < WorldMap.mapSizeX && playerMapY >= 0 && playerMapY < WorldMap.mapSizeY) {
                    playerMapX += (Math.random() * 10.0).toInt() - 5
                    playerMapY += (Math.random() * 10.0).toInt() - 5
                    mapCenterX = playerMapX
                    mapCenterY = playerMapY
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
    fun setMapCenterX(x: Int) {
        mapCenterX = x
        scrollTargetX = -1
        scrollTargetY = -1
        clampMapCenter()
    }

    @JvmStatic
    fun getStaticElement(): WorldMapElement? {
        return WorldMap.staticElement
    }

    @JvmStatic
    fun setMapCenterY(y: Int) {
        mapCenterY = y
        scrollTargetY = -1
        clampMapCenter()
    }

    @JvmStatic
    fun highlightCategory(categoryId: Int) {
        someMecId = -1
        highlightedCategoryId = categoryId
        JSObjectManager.highlightFlashCycles = 3
        ServerEnvironment.flashCycleCounter = 100
    }

    @JvmStatic
    fun renderIconsAndNames(toolkit: AbstractToolkit, category: WorldMapCategory, mectype: MECType): Boolean {
        var boundsLeft = Int.MAX_VALUE
        var boundsRight = Int.MIN_VALUE
        var boundsBottom = Int.MAX_VALUE
        var boundsTop = Int.MIN_VALUE
        if (mectype.offsets != null) {
            boundsLeft = (WorldMap.pixelViewEndX - WorldMap.pixelViewStartX) * (mectype.minOffsetX + category.absX - WorldMap.tileViewStartX) / (WorldMap.tileViewEndX - WorldMap.tileViewStartX) + WorldMap.pixelViewStartX
            boundsRight = WorldMap.pixelViewStartX + (WorldMap.pixelViewEndX - WorldMap.pixelViewStartX) * (mectype.maxOffsetX + category.absX - WorldMap.tileViewStartX) / (WorldMap.tileViewEndX - WorldMap.tileViewStartX)
            boundsTop = WorldMap.pixelViewEndY - (WorldMap.pixelViewEndY - WorldMap.pixelViewStartY) * (mectype.minOffsetY + category.absY - WorldMap.tileViewStartY) / (WorldMap.tileViewEndY - WorldMap.tileViewStartY)
            boundsBottom = WorldMap.pixelViewEndY - (WorldMap.pixelViewEndY - WorldMap.pixelViewStartY) * (mectype.maxOffsetY + category.absY - WorldMap.tileViewStartY) / (WorldMap.tileViewEndY - WorldMap.tileViewStartY)
        }
        var iconSprite: NativeSprite? = null
        var spriteLeft = 0
        var spriteRight = 0
        var spriteBottom = 0
        var spriteTop = 0
        if (mectype.defaultIconArchive != -1) {
            iconSprite = if (category.isMouseOver && mectype.mouseOverIconArchive != -1) {
                mectype.getIcon(toolkit, true)
            } else {
                mectype.getIcon(toolkit, false)
            }
            if (iconSprite != null) {
                spriteLeft = category.screenX - (iconSprite.getSpriteWidth() + 1 shr 1)
                if (spriteLeft < boundsLeft) {
                    boundsLeft = spriteLeft
                }
                spriteRight = category.screenX + (iconSprite.getSpriteWidth() + 1 shr 1)
                if (spriteRight > boundsRight) {
                    boundsRight = spriteRight
                }
                spriteBottom = category.screenY - (iconSprite.getSpriteHeight() + 1 shr 1)
                if (spriteBottom < boundsBottom) {
                    boundsBottom = spriteBottom
                }
                spriteTop = category.screenY + (iconSprite.getSpriteHeight() + 1 shr 1)
                if (spriteTop > boundsTop) {
                    boundsTop = spriteTop
                }
            }
        }
        var font: HelveticaFont? = null
        var lineCount = 0
        var labelX = 0
        var labelY = 0
        var maxTextWidth = 0
        var textLeft = 0
        var textRight = 0
        var textBottom = 0
        var textTop = 0
        val areaName = mectype.areaName
        if (areaName != null) {
            font = getZoomedHelveticafont(mectype.baseTextzoom)
            if (font != null) {
                lineCount = FontManager.worldMapAreaFont!!.splitLinesToBuffer(areaName, null, RENDERED_WORLDMAP_AREA_NAMELINES, null)
                labelX = (WorldMap.pixelViewEndX - WorldMap.pixelViewStartX) * mectype.labelOffsetX / (WorldMap.tileViewEndX - WorldMap.tileViewStartX) + category.screenX
                labelY = category.screenY - (WorldMap.pixelViewEndY - WorldMap.pixelViewStartY) * mectype.labelOffsetY / (WorldMap.tileViewEndY - WorldMap.tileViewStartY)
                if (iconSprite == null) {
                    labelY -= font.getHeight() * lineCount / 2
                } else {
                    labelY -= (iconSprite.getSpriteHeight() shr 1) + font.getLineHeight() * lineCount
                }
                for (index in 0 until lineCount) {
                    val lineText = RENDERED_WORLDMAP_AREA_NAMELINES[index]
                    if (lineText != null) {
                        val text = if (index < lineCount - 1) {
                            lineText.substring(0, lineText.length - 4)
                        } else {
                            lineText
                        }
                        val textWidth = font.getTextWidth(text)
                        if (textWidth > maxTextWidth) {
                            maxTextWidth = textWidth
                        }
                    }
                }
                textLeft = labelX - maxTextWidth / 2
                if (textLeft < boundsLeft) {
                    boundsLeft = textLeft
                }
                textRight = maxTextWidth / 2 + labelX
                if (textRight > boundsRight) {
                    boundsRight = textRight
                }
                textBottom = labelY
                if (textBottom < boundsBottom) {
                    boundsBottom = textBottom
                }
                textTop = labelY + lineCount * font.getLineHeight()
                if (textTop > boundsTop) {
                    boundsTop = textTop
                }
            }
        }
        if (boundsRight >= WorldMap.pixelViewStartX && boundsLeft <= WorldMap.pixelViewEndX && boundsTop >= WorldMap.pixelViewStartY && boundsBottom <= WorldMap.pixelViewEndY) {
            WorldMap.renderCategoryLines(toolkit, category, mectype)
            if (iconSprite != null) {
                if (JSObjectManager.highlightFlashCycles > 0 && (someMecId != -1 && someMecId == category.mecId || highlightedCategoryId != -1 && mectype.categoryId == highlightedCategoryId)) {
                    val alpha = if (ServerEnvironment.flashCycleCounter > 50) {
                        200 - ServerEnvironment.flashCycleCounter * 2
                    } else {
                        ServerEnvironment.flashCycleCounter * 2
                    }
                    val color = alpha shl 24 or 0xffff00
                    toolkit.drawMapCategory(category.screenX, category.screenY, iconSprite.getWidth() / 2 + 7, color)
                    toolkit.drawMapCategory(category.screenX, category.screenY, iconSprite.getWidth() / 2 + 5, color)
                    toolkit.drawMapCategory(category.screenX, category.screenY, iconSprite.getWidth() / 2 + 3, color)
                    toolkit.drawMapCategory(category.screenX, category.screenY, iconSprite.getWidth() / 2 + 1, color)
                    toolkit.drawMapCategory(category.screenX, category.screenY, iconSprite.getWidth() / 2, color)
                }
                iconSprite.draw(category.screenX - (iconSprite.getSpriteWidth() shr 1), category.screenY - (iconSprite.getSpriteHeight() shr 1))
            }
            if (mectype.areaName != null && font != null) {
                WorldMapElement.renderWorldmapAreaText(toolkit, category, mectype, lineCount, labelX, labelY, maxTextWidth, font)
            }
            if (mectype.defaultIconArchive != -1 || mectype.areaName != null) {
                val clickzone = ClickzoneMEC(category)
                clickzone.spriteLeft = spriteLeft
                clickzone.spriteRight = spriteRight
                clickzone.spriteBottom = spriteBottom
                clickzone.spriteTop = spriteTop
                clickzone.textLeft = textLeft
                clickzone.textRight = textRight
                clickzone.textBottom = textBottom
                clickzone.textTop = textTop
                WorldMapInteraction.MEC_CLICKZONES!!.addFirst(clickzone)
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
    fun renderInformation(toolkit: AbstractToolkit, categories: NodeCollection) {
        WorldMapInteraction.MEC_CLICKZONES!!.remove_all()
        if (!hideIcons) {
            var category = categories.getFirst() as? WorldMapCategory
            while (category != null) {
                val mectype = WorldMap.MECTYPE_LIST!!.list(category.mecId)
                if (shouldDraw(mectype)) {
                    val offscreen = renderIconsAndNames(toolkit, category, mectype)
                    if (offscreen) {
                        renderOffscreenIndicator(toolkit, category, mectype)
                    }
                }
                category = categories.next() as? WorldMapCategory
            }
        }
    }

    @JvmStatic
    fun shouldDraw(mectype: MECType?): Boolean {
        return mectype != null && mectype.displayedOnWorldmap && mectype.shouldDraw(WorldMap.PLAYER_VAR_PROVIDER!!) &&
               hiddenMecIds.get(mectype.mectypeId.toLong()) == null && hiddenCategoryIds.get(mectype.categoryId.toLong()) == null
    }

    @JvmStatic
    fun renderOffscreenIndicator(toolkit: AbstractToolkit, category: WorldMapCategory, mectype: MECType) {
        val sprite = mectype.getSprite(toolkit) ?: return

        var spriteDimension = sprite.getWidth()
        if (sprite.getHeight() > spriteDimension) {
            spriteDimension = sprite.getHeight()
        }

        val margin: Byte = 10
        var spriteCenterX = category.screenX
        var spriteCenterY = category.screenY
        var lineCount = 0
        var maxTextWidth = 0
        var textHeight = 0
        var tmp: Int
        val areaName = mectype.areaName
        if (areaName != null) {
            lineCount = FontManager.worldMapAreaFont!!.splitLinesToBuffer(areaName, null, RENDERED_WORLDMAP_AREA_NAMELINES, null)

            for (index in 0 until lineCount) {
                val lineText = RENDERED_WORLDMAP_AREA_NAMELINES[index]
                if (lineText != null) {
                    val text = if (index < lineCount - 1) {
                        lineText.substring(0, lineText.length - 4)
                    } else {
                        lineText
                    }

                    tmp = Resource.helvetica14Font!!.getTextWidth(text)
                    if (tmp > maxTextWidth) {
                        maxTextWidth = tmp
                    }
                }
            }

            textHeight = Resource.helvetica14Font!!.getLineHeight() * lineCount + Resource.helvetica14Font!!.getHeight() / 2
        }

        var textX = spriteDimension / 2 + category.screenX
        var textY = category.screenY
        if (spriteCenterX < spriteDimension + WorldMap.pixelViewStartX) {
            spriteCenterX = WorldMap.pixelViewStartX
            textX = spriteDimension / 2 + maxTextWidth / 2 + margin + WorldMap.pixelViewStartX + 5
        } else if (spriteCenterX > WorldMap.pixelViewEndX - spriteDimension) {
            spriteCenterX = WorldMap.pixelViewEndX - spriteDimension
            textX = WorldMap.pixelViewEndX - spriteDimension / 2 - margin - maxTextWidth / 2 - 5
        }

        if (spriteCenterY < spriteDimension + WorldMap.pixelViewStartY) {
            spriteCenterY = WorldMap.pixelViewStartY
            textY = spriteDimension / 2 + margin + WorldMap.pixelViewStartY
        } else if (spriteCenterY > WorldMap.pixelViewEndY - spriteDimension) {
            spriteCenterY = WorldMap.pixelViewEndY - spriteDimension
            textY = WorldMap.pixelViewEndY - spriteDimension / 2 - margin - textHeight
        }

        tmp = (atan2((spriteCenterX - category.screenX).toDouble(), (spriteCenterY - category.screenY).toDouble()) / Math.PI * 32767.0).toInt() and 0xffff
        sprite.drawRotatedCenter(spriteCenterX + spriteDimension / 2.0f, spriteDimension / 2.0f + spriteCenterY, 4096, tmp)
        var textLeft = -2
        var textBottom = -2
        var textRight = -2
        var textTop = -2
        if (mectype.areaName != null) {
            textLeft = textX - maxTextWidth / 2 - 5
            textBottom = textY
            textRight = textLeft + maxTextWidth + 10
            textTop = textY + Resource.helvetica14Font!!.getLineHeight() * lineCount + 3
            if (mectype.backgroundColor != 0) {
                toolkit.clearPixels(textLeft, textY, textRight - textLeft, textTop - textY, mectype.backgroundColor)
            }

            if (mectype.outlineColor != 0) {
                toolkit.drawOpaqueRectangle(textLeft, textY, textRight - textLeft, textTop - textY, mectype.outlineColor)
            }

            for (index in 0 until lineCount) {
                val lineText = RENDERED_WORLDMAP_AREA_NAMELINES[index]
                if (lineText != null) {
                    val text = if (index < lineCount - 1) {
                        lineText.substring(0, lineText.length - 4)
                    } else {
                        lineText
                    }

                    Resource.helvetica14Font!!.drawClippedText(toolkit, text, textX, textY, mectype.defaultTextColor)
                    textY += Resource.helvetica14Font!!.getLineHeight()
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
