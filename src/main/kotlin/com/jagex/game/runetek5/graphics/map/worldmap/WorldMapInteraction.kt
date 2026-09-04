package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.cache.node.NodeCollection

object WorldMapInteraction {

    @JvmField
    var MEC_CLICKZONES: NodeCollection? = null

    @JvmStatic
    fun requestZoomChange(zoomPercent: Int) {
        if (zoomPercent == 37) {
            WorldMap.requestedMapZoom = 3.0f
        } else if (zoomPercent == 50) {
            WorldMap.requestedMapZoom = 4.0f
        } else if (zoomPercent == 75) {
            WorldMap.requestedMapZoom = 6.0f
        } else if (zoomPercent == 100) {
            WorldMap.requestedMapZoom = 8.0f
        } else if (zoomPercent == 200) {
            WorldMap.requestedMapZoom = 16.0f
        }
        WorldMapDisplay.scrollTargetY = -1
    }
}
