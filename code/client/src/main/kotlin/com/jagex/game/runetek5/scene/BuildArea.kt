package com.jagex.game.runetek5.scene

data class BuildArea(@JvmField val id: Int, @JvmField val chunkSize: Int) {
    companion object {
        @JvmField val ORIGINAL_SIZE = BuildArea(0, 4)
        @JvmField val EXTENDED_SIZE = BuildArea(1, 5)
        @JvmField val LARGE_SIZE = BuildArea(2, 6)

        @JvmStatic
        fun getAll(): Array<BuildArea> {
            return arrayOf(LARGE_SIZE, ORIGINAL_SIZE, EXTENDED_SIZE)
        }

        @JvmStatic
        fun parse(id: Int): BuildArea? {
            val dimensions = getAll()
            for (dimension in dimensions) {
                if (id == dimension.id) {
                    return dimension
                }
            }
            return null
        }
    }
}
