package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap

class WorldMapCategory(
    mecId: Int
) : Node<WorldMapCategory>() {

    @JvmField var absX: Int = 0
    @JvmField var anInt7988: Int = 0
    @JvmField var anInt7990: Int = 0
    @JvmField var anInt7992: Int = 0
    @JvmField var absY: Int = 0
    @JvmField var mecId: Int = -1
    @JvmField var isMouseOver: Boolean = false

    init {
        this.mecId = mecId
    }

    companion object {
        @JvmStatic
        fun method12702(i_0: Int, i_1: Int, i_2: Int): Int {
            if (ComponentMinimap.mapAssetLoadingPercentage < 100) {
                return -2
            } else {
                var i_4 = -2
                var i_5 = Int.MAX_VALUE
                val i_6 = i_1 - WorldMapSub1.mapSquareAbsX
                val i_7 = i_2 - WorldMapSub1.mapSquareAbsY

                var class282Sub368 = WorldMapSub1.worldMapCategories.getFirst() as WorldMapCategory?
                while (class282Sub368 != null) {
                    if (i_0 == class282Sub368.mecId) {
                        val i_9 = class282Sub368.absX
                        val i_10 = class282Sub368.absY
                        val i_11 = i_9 + WorldMapSub1.mapSquareAbsX shl 14 or (i_10 + WorldMapSub1.mapSquareAbsY)
                        val i_12 = (i_6 - i_9) * (i_6 - i_9) + (i_7 - i_10) * (i_7 - i_10)
                        if (i_4 < 0 || i_12 < i_5) {
                            i_4 = i_11
                            i_5 = i_12
                        }
                    }
                    class282Sub368 = WorldMapSub1.worldMapCategories.next() as WorldMapCategory?
                }

                return i_4
            }
        }
    }
}
