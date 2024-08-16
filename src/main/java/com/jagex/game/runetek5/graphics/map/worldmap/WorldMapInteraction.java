package com.jagex.game.runetek5.graphics.map.worldmap;

import com.jagex.game.runetek5.cache.node.NodeCollection;

public class WorldMapInteraction {

	public static NodeCollection MEC_CLICKZONES;

	public static void requestZoomChange(int i_0) {
	    if (i_0 == 37) {
	        WorldMapSub1.requestedMapZoom = 3.0F;
	    } else if (i_0 == 50) {
	        WorldMapSub1.requestedMapZoom = 4.0F;
	    } else if (i_0 == 75) {
	        WorldMapSub1.requestedMapZoom = 6.0F;
	    } else if (i_0 == 100) {
	        WorldMapSub1.requestedMapZoom = 8.0F;
	    } else if (i_0 == 200) {
	        WorldMapSub1.requestedMapZoom = 16.0F;
	    }
	    WorldMapSub1.anInt8016 = -1;
	}

}
