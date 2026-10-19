package com.jagex.game.runetek5.scene

data class MapSize(@JvmField val id: Int, @JvmField val size: Int) {
    companion object {
        @JvmField val MAPSIZE_104_DEFAULT = MapSize(0, 104)
        @JvmField val MAPSIZE_120_EXT1 = MapSize(1, 120)
        @JvmField val MAPSIZE_136_EXT2 = MapSize(2, 136)
        @JvmField val MAPSIZE_168_EXT3 = MapSize(3, 168)
        @JvmField val MAPSIZE_72_MINI_CS = MapSize(4, 72)

        @JvmStatic
        fun allMapsizes(): Array<MapSize> {
            return arrayOf(MAPSIZE_136_EXT2, MAPSIZE_168_EXT3, MAPSIZE_72_MINI_CS, MAPSIZE_120_EXT1, MAPSIZE_104_DEFAULT)
        }

        @JvmStatic
        fun get(mapsizeId: Int): MapSize? {
            val allMapsizes = allMapsizes()
            for (mapsize in allMapsizes) {
                if (mapsizeId == mapsize.id) {
                    return mapsize
                }
            }
            return null
        }
    }
}
