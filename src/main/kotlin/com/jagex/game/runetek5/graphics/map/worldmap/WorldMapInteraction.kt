package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.cache.node.NodeCollection

object WorldMapInteraction {

    @JvmField
    var MEC_CLICKZONES: NodeCollection? = null

    @JvmStatic
    fun requestZoomChange(i_0: Int) {
        if (i_0 == 37) {
            WorldMap.requestedMapZoom = 3.0f
        } else if (i_0 == 50) {
            WorldMap.requestedMapZoom = 4.0f
        } else if (i_0 == 75) {
            WorldMap.requestedMapZoom = 6.0f
        } else if (i_0 == 100) {
            WorldMap.requestedMapZoom = 8.0f
        } else if (i_0 == 200) {
            WorldMap.requestedMapZoom = 16.0f
        }
        WorldMapSub1.scrollTargetY = -1
    }
}
