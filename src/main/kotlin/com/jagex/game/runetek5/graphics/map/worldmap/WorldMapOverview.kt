package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.client.JSObjectManager
import com.jagex.game.runetek5.client.ServerEnvironment
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.mec.MECType
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.DebugDisplay

/**
 * Handles rendering the world map overview window.
 */
object WorldMapOverview {

    /**
     * Draws a small map containing the entire world,
     * used to allow user to reposition position
     * on worldmap
     *
     * @param toolkit The rendering toolkit
     * @param x The x position
     * @param y The y position
     * @param width The width
     * @param height The height
     */
    @JvmStatic
    fun drawWorldmapOverview(toolkit: AbstractToolkit, x: Int, y: Int, width: Int, height: Int) {
        var adjustedX = x
        var adjustedY = y
        toolkit.setClipping(x, y, width + x, y + height)
        toolkit.clearPixels(x, y, width, height, 0xffffff.inv())

        if (ComponentMinimap.mapAssetLoadingPercentage >= 100) {
            val aspectRatio = WorldMap.mapSizeY.toFloat() / WorldMap.mapSizeX
            var scaledWidth = width
            var scaledHeight = height

            if (aspectRatio < 1.0f) {
                scaledHeight = (aspectRatio * width).toInt()
            } else {
                scaledWidth = (height / aspectRatio).toInt()
            }

            adjustedX += (width - scaledWidth) / 2
            adjustedY += (height - scaledHeight) / 2

            if (ComponentMinimap.worldMapOverviewSprite == null ||
                ComponentMinimap.worldMapOverviewSprite!!.getSpriteWidth() != scaledWidth ||
                ComponentMinimap.worldMapOverviewSprite!!.getSpriteHeight() != scaledHeight) {
                WorldMap.setViewportBounds(
                    WorldMap.mapSquareAbsX,
                    WorldMap.mapSquareAbsY + WorldMap.mapSizeY,
                    WorldMap.mapSizeX + WorldMap.mapSquareAbsX,
                    WorldMap.mapSquareAbsY,
                    adjustedX,
                    adjustedY,
                    scaledWidth + adjustedX,
                    scaledHeight + adjustedY
                )
                WorldMap.renderMap(toolkit)
                ComponentMinimap.worldMapOverviewSprite = toolkit.createNativeSprite(adjustedX, adjustedY, scaledWidth, scaledHeight, false)
            }

            ComponentMinimap.worldMapOverviewSprite!!.draw(adjustedX, adjustedY)

            val backgroundWidth = scaledWidth * StaticMethods.worldMapViewWidth / WorldMap.mapSizeX
            val backgroundHeight = scaledHeight * ComponentMinimap.mapViewportScale / WorldMap.mapSizeY
            val backgroundX = scaledWidth * DebugDisplay.worldMapViewLeftTileX / WorldMap.mapSizeX + adjustedX
            val backgroundY = scaledHeight + adjustedY - scaledHeight * WorldMapDisplay.viewBottomTileY / WorldMap.mapSizeY - backgroundHeight
            var backgroundColor = -1996554240

            if (Game.STELLAR_DAWN == Client.CURRENT_GAME) {
                backgroundColor = -1996488705
            }

            toolkit.drawFilledRectangle(backgroundX, backgroundY, backgroundWidth, backgroundHeight, backgroundColor, 1)
            toolkit.drawRectangleOutline(backgroundX, backgroundY, backgroundWidth, backgroundHeight, backgroundColor, 0)

            if (JSObjectManager.highlightFlashCycles > 0) {
                val randomizedColor: Int = if (ServerEnvironment.flashCycleCounter > 50) {
                    500 - ServerEnvironment.flashCycleCounter * 5
                } else {
                    ServerEnvironment.flashCycleCounter * 5
                }

                var mapCategory = WorldMap.worldMapCategories.getFirst() as WorldMapCategory?
                while (mapCategory != null) {
                    val mectype: MECType? = WorldMap.MECTYPE_LIST!!.list(mapCategory.mecId)
                    if (WorldMapDisplay.shouldDraw(mectype)) {
                        if (mapCategory.mecId == WorldMapDisplay.someMecId) {
                            val mecStartX = scaledWidth * mapCategory.absX / WorldMap.mapSizeX + adjustedX
                            val mecStartY = scaledHeight * (WorldMap.mapSizeY - mapCategory.absY) / WorldMap.mapSizeY + adjustedY
                            toolkit.clearPixels(mecStartX - 2, mecStartY - 2, 4, 4, randomizedColor shl 24 or 0xffff00)
                        } else if (WorldMapDisplay.highlightedCategoryId != -1 && mectype!!.categoryId == WorldMapDisplay.highlightedCategoryId) {
                            val mecStartX = scaledWidth * mapCategory.absX / WorldMap.mapSizeX + adjustedX
                            val mecStartY = scaledHeight * (WorldMap.mapSizeY - mapCategory.absY) / WorldMap.mapSizeY + adjustedY
                            toolkit.clearPixels(mecStartX - 2, mecStartY - 2, 4, 4, randomizedColor shl 24 or 0xffff00)
                        }
                    }
                    mapCategory = WorldMap.worldMapCategories.next() as WorldMapCategory?
                }
            }
        }
    }
}
