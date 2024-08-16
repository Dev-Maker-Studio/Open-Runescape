package com.jagex.game.runetek5.scene;

public class MapSize {

    public static final MapSize MAPSIZE_104_DEFAULT = new MapSize(0, 104);
    public static final MapSize MAPSIZE_120_EXT1 = new MapSize(1, 120);
    public static final MapSize MAPSIZE_136_EXT2 = new MapSize(2, 136);
    public static final MapSize MAPSIZE_168_EXT3 = new MapSize(3, 168);
    public static final MapSize MAPSIZE_72_MINI_CS = new MapSize(4, 72);

    public final int id;
    public final int size;

    public MapSize(int id, int sizeDimensionTiles) {
        this.id = id;
        this.size = sizeDimensionTiles;
    }

    public static MapSize[] allMapsizes() {
        return new MapSize[]{MAPSIZE_136_EXT2, MAPSIZE_168_EXT3, MAPSIZE_72_MINI_CS, MAPSIZE_120_EXT1, MAPSIZE_104_DEFAULT};
    }

	public static MapSize get(int mapsizeId) {
	    MapSize[] allMapsizes = allMapsizes();
	    for (int index = 0; index < allMapsizes.length; index++) {
	        MapSize mapsize = allMapsizes[index];
	        if (mapsizeId == mapsize.id) {
	            return mapsize;
	        }
	    }
	    return null;
	}
}
