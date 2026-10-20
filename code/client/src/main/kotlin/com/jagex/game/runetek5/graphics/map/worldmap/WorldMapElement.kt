package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.config.mec.MECType
import com.jagex.game.runetek5.graphics.font.HelveticaFont
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.scene.MapSize
import com.jagex.game.runetek5.toolkit.AbstractToolkit

class WorldMapElement(
    @JvmField val elementId: Int,
    @JvmField val filenamePrefix: String,
    @JvmField val displayName: String,
    @JvmField val bitpackedPlacement: Int,
    color: Int,
    @JvmField var shouldRender: Boolean,
    defaultZoomLevel: Int,
    mapsizeId: Int
) : CacheableNode() {

    @JvmField var defaultZoomLevel: Int = -1
    @JvmField var boundsMinX = 12800
    @JvmField var boundsMaxX = 0
    @JvmField var boundsMinY = 12800
    @JvmField var boundsMaxY = 0
    @JvmField var color = -1
    @JvmField val worldMapPlacements: NodeCollection

    init {
        this.color = color
        this.defaultZoomLevel = defaultZoomLevel
        if (this.defaultZoomLevel == 255) {
            this.defaultZoomLevel = 0
        }

        MapSize.get(mapsizeId)
        this.worldMapPlacements = NodeCollection()
    }

    fun mapToGameCoords(mapX: Int, mapY: Int, position: IntArray): Boolean {
        var placement: WorldMapPlacement? = worldMapPlacements.getFirst() as WorldMapPlacement?
        while (placement != null) {
            if (placement.withinMapBounds(mapX, mapY)) {
                placement.mapToGameCoords(mapX, mapY, position)
                return true
            }
            placement = worldMapPlacements.next() as WorldMapPlacement?
        }

        return false
    }

    fun gameToMapCoords(x: Int, y: Int, cacheInto: IntArray): Boolean {
        var placement: WorldMapPlacement? = worldMapPlacements.getFirst() as WorldMapPlacement?
        while (placement != null) {
            if (placement.withinBounds(x, y)) {
                placement.cache(x, y, cacheInto)
                return true
            }
            placement = worldMapPlacements.next() as WorldMapPlacement?
        }

        return false
    }

    fun gameToMapCoordsForPlane(plane: Int, x: Int, y: Int, cacheInto: IntArray): Boolean {
        var placement: WorldMapPlacement? = worldMapPlacements.getFirst() as WorldMapPlacement?
        while (placement != null) {
            if (placement.withinPlaneBounds(plane, x, y)) {
                placement.cache(x, y, cacheInto)
                return true
            }
            placement = worldMapPlacements.next() as WorldMapPlacement?
        }

        return false
    }

    fun recalculateBounds() {
        boundsMinX = 12800
        boundsMaxX = 0
        boundsMinY = 12800
        boundsMaxY = 0

        var placement: WorldMapPlacement? = worldMapPlacements.getFirst() as WorldMapPlacement?
        while (placement != null) {
            if (placement.mapMinX < boundsMinX) {
                boundsMinX = placement.mapMinX
            }

            if (placement.mapMaxX > boundsMaxX) {
                boundsMaxX = placement.mapMaxX
            }

            if (placement.mapMinY < boundsMinY) {
                boundsMinY = placement.mapMinY
            }

            if (placement.mapMaxY > boundsMaxY) {
                boundsMaxY = placement.mapMaxY
            }
            placement = worldMapPlacements.next() as WorldMapPlacement?
        }
    }

    fun allElementsInBounds(x: Int, y: Int): Boolean {
        var placement: WorldMapPlacement? = worldMapPlacements.getFirst() as WorldMapPlacement?
        while (placement != null) {
            if (placement.withinBounds(x, y)) {
                return true
            }
            placement = worldMapPlacements.next() as WorldMapPlacement?
        }

        return false
    }

    companion object {
        @JvmStatic
        fun list(resourceProvider: Js5ResourceProvider, arhiveId: Int, fileId: Int): WorldMapElement {
            val buffer = Packet(resourceProvider.getFile(arhiveId, fileId)!!)
            val def = WorldMapElement(
                fileId,
                buffer.readString(),
                buffer.readString(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUByte() == 1,
                buffer.readUByte(),
                buffer.readUByte()
            )
            val count = buffer.readUByte()
            for (index in 0 until count) {
                def.worldMapPlacements.addFirst(
                    WorldMapPlacement(
                        buffer.readUByte(),
                        buffer.readUShort(),
                        buffer.readUShort(),
                        buffer.readUShort(),
                        buffer.readUShort(),
                        buffer.readUShort(),
                        buffer.readUShort(),
                        buffer.readUShort(),
                        buffer.readUShort()
                    )
                )
            }
            def.recalculateBounds()
            return def
        }

        @JvmStatic
        fun renderWorldmapAreaText(
            toolkit: AbstractToolkit,
            category: WorldMapCategory,
            mectype: MECType,
            lineCount: Int,
            x: Int,
            y: Int,
            maxTextWidth: Int,
            font: HelveticaFont
        ) {
            val rectX = x - maxTextWidth / 2 - 5
            val rectY = y + 2
            if (mectype.backgroundColor != 0) {
                toolkit.clearPixels(
                    rectX,
                    rectY,
                    maxTextWidth + 10,
                    y + lineCount * font.getLineHeight() - rectY + 1,
                    mectype.backgroundColor
                )
            }
            if (mectype.outlineColor != 0) {
                toolkit.drawOpaqueRectangle(
                    rectX,
                    rectY,
                    maxTextWidth + 10,
                    y + lineCount * font.getLineHeight() - rectY + 1,
                    mectype.outlineColor
                )
            }
            var textColor = mectype.defaultTextColor
            if (category.isMouseOver && mectype.mouseOverTextColor != -1) {
                textColor = mectype.mouseOverTextColor
            }
            var currentY = y
            for (line in 0 until lineCount) {
                val lineText = WorldMapDisplay.RENDERED_WORLDMAP_AREA_NAMELINES[line]
                if (lineText != null) {
                    val areaNameLine = if (line < lineCount - 1) {
                        lineText.substring(0, lineText.length - 4)
                    } else {
                        lineText
                    }
                    font.drawClippedText(toolkit, areaNameLine, x, currentY, textColor)
                    currentY += font.getLineHeight()
                }
            }
        }
    }
}
