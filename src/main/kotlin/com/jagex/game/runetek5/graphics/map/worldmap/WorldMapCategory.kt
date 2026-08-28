package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap

class WorldMapCategory(
    mecId: Int
) : Node<WorldMapCategory>() {

    @JvmField var absX: Int = 0
    @JvmField var plane: Int = 0
    @JvmField var screenX: Int = 0
    @JvmField var screenY: Int = 0
    @JvmField var absY: Int = 0
    @JvmField var mecId: Int = -1
    @JvmField var isMouseOver: Boolean = false

    init {
        this.mecId = mecId
    }

    companion object {
        @JvmStatic
        fun findNearestElement(mecId: Int, coordX: Int, coordY: Int): Int {
            if (ComponentMinimap.mapAssetLoadingPercentage < 100) {
                return -2
            } else {
                var nearestCoord = -2
                var nearestDistSq = Int.MAX_VALUE
                val localX = coordX - WorldMap.mapSquareAbsX
                val localY = coordY - WorldMap.mapSquareAbsY

                var category = WorldMap.worldMapCategories.getFirst() as WorldMapCategory?
                while (category != null) {
                    if (mecId == category.mecId) {
                        val categoryX = category.absX
                        val categoryY = category.absY
                        val coord = categoryX + WorldMap.mapSquareAbsX shl 14 or (categoryY + WorldMap.mapSquareAbsY)
                        val distSq = (localX - categoryX) * (localX - categoryX) + (localY - categoryY) * (localY - categoryY)
                        if (nearestCoord < 0 || distSq < nearestDistSq) {
                            nearestCoord = coord
                            nearestDistSq = distSq
                        }
                    }
                    category = WorldMap.worldMapCategories.next() as WorldMapCategory?
                }

                return nearestCoord
            }
        }
    }
}
