package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.cache.node.NodeCollection

object WorldMapInteraction {

    @JvmField
    var MEC_CLICKZONES: NodeCollection? = null

    @JvmStatic
    fun requestZoomChange(i_0: Int) {
        if (i_0 == 37) {
            WorldMapSub1.requestedMapZoom = 3.0f
        } else if (i_0 == 50) {
            WorldMapSub1.requestedMapZoom = 4.0f
        } else if (i_0 == 75) {
            WorldMapSub1.requestedMapZoom = 6.0f
        } else if (i_0 == 100) {
            WorldMapSub1.requestedMapZoom = 8.0f
        } else if (i_0 == 200) {
            WorldMapSub1.requestedMapZoom = 16.0f
        }
        WorldMapSub1.anInt8016 = -1
    }
}
