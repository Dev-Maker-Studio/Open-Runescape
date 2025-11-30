package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.client.JSObjectManager
import com.jagex.game.runetek5.client.ServerEnvironment
import com.jagex.game.runetek5.client.client
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
            val aspectRatio = WorldMapSub1.mapSizeY.toFloat() / WorldMapSub1.mapSizeX
            var scaledWidth = width
            var scaledHeight = height

            if (aspectRatio < 1.0f) {
                scaledHeight = (aspectRatio * width).toInt()
            } else {
                scaledWidth = (height / aspectRatio).toInt()
            }

            adjustedX += (width - scaledWidth) / 2
            adjustedY += (height - scaledHeight) / 2

            if (ComponentMinimap.aNativeSprite5883 == null ||
                ComponentMinimap.aNativeSprite5883!!.getSpriteWidth() != width ||
                ComponentMinimap.aNativeSprite5883!!.getSpriteHeight() != height) {
                WorldMapSub1.method5139(
                    WorldMapSub1.mapSquareAbsX,
                    WorldMapSub1.mapSquareAbsY + WorldMapSub1.mapSizeY,
                    WorldMapSub1.mapSizeX + WorldMapSub1.mapSquareAbsX,
                    WorldMapSub1.mapSquareAbsY,
                    adjustedX,
                    adjustedY,
                    scaledWidth + adjustedX,
                    scaledHeight + adjustedY
                )
                WorldMapSub1.renderMap(toolkit)
                ComponentMinimap.aNativeSprite5883 = toolkit.createNativeSprite(adjustedX, adjustedY, scaledWidth, scaledHeight, false)
            }

            ComponentMinimap.aNativeSprite5883!!.draw(adjustedX, adjustedY)

            val backgroundWidth = scaledWidth * StaticMethods.anInt5760 / WorldMapSub1.mapSizeX
            val backgroundHeight = scaledHeight * ComponentMinimap.anInt4097 / WorldMapSub1.mapSizeY
            val backgroundX = scaledWidth * DebugDisplay.anInt3991 / WorldMapSub1.mapSizeX + adjustedX
            val backgroundY = scaledHeight + adjustedY - scaledHeight * WorldMapSub1.anInt5765 / WorldMapSub1.mapSizeY - backgroundHeight
            var backgroundColor = -1996554240

            if (Game.STELLAR_DAWN == client.CURRENT_GAME) {
                backgroundColor = -1996488705
            }

            toolkit.drawFilledRectangle(backgroundX, backgroundY, backgroundWidth, backgroundHeight, backgroundColor, 1)
            toolkit.drawRectangleOutline(backgroundX, backgroundY, backgroundWidth, backgroundHeight, backgroundColor, 0)

            if (JSObjectManager.anInt4185 > 0) {
                val randomizedColor: Int = if (ServerEnvironment.anInt9282 > 50) {
                    500 - ServerEnvironment.anInt9282 * 5
                } else {
                    ServerEnvironment.anInt9282 * 5
                }

                var mapCategory = WorldMapSub1.worldMapCategories.getFirst() as WorldMapCategory?
                while (mapCategory != null) {
                    val mectype: MECType? = WorldMapSub1.MECTYPE_LIST.list(mapCategory.mecId)
                    if (WorldMapSub1.shouldDraw(mectype)) {
                        if (mapCategory.mecId == WorldMapSub1.someMecId) {
                            val mecStartX = scaledWidth * mapCategory.absX / WorldMapSub1.mapSizeX + adjustedX
                            val mecStartY = scaledHeight * (WorldMapSub1.mapSizeY - mapCategory.absY) / WorldMapSub1.mapSizeY + adjustedY
                            toolkit.clearPixels(mecStartX - 2, mecStartY - 2, 4, 4, randomizedColor shl 24 or 0xffff00)
                        } else if (WorldMapSub1.anInt8026 != -1 && mectype!!.anInt2718 == WorldMapSub1.anInt8026) {
                            val mecStartX = scaledWidth * mapCategory.absX / WorldMapSub1.mapSizeX + adjustedX
                            val mecStartY = scaledHeight * (WorldMapSub1.mapSizeY - mapCategory.absY) / WorldMapSub1.mapSizeY + adjustedY
                            toolkit.clearPixels(mecStartX - 2, mecStartY - 2, 4, 4, randomizedColor shl 24 or 0xffff00)
                        }
                    }
                    mapCategory = WorldMapSub1.worldMapCategories.next() as WorldMapCategory?
                }
            }
        }
    }
}
