package com.jagex.game.runetek5.config.material.texture

import com.jagex.game.runetek5.cache.node.LinkedList

class ColorImageCache(
    @JvmField val maxCachedRows: Int,
    @JvmField val totalRows: Int,
    dataLength: Int
) {

    @JvmField var dirty: Boolean = false
    @JvmField var cacheEntries: Array<ColorImageCacheEntry?>
    @JvmField var rgbData: Array<Array<IntArray>>
    @JvmField var lruList: LinkedList? = LinkedList()
    @JvmField var cachedRowCount: Int = 0
    @JvmField var lastAccessedRow: Int = -1

    init {
        rgbData = Array(maxCachedRows) {
            Array(3) { IntArray(dataLength) }
        }
        cacheEntries = arrayOfNulls(totalRows)
    }

    fun clear() {
        cacheEntries = emptyArray()
        rgbData = emptyArray()
        lruList!!.clear()
        lruList = null
    }

    fun getPalette(index: Int): Array<IntArray> {
        if (totalRows == maxCachedRows) {
            dirty = cacheEntries[index] == null
            cacheEntries[index] = SENTINEL_ENTRY
            return rgbData[index]
        }

        if (maxCachedRows == 1) {
            dirty = lastAccessedRow != index
            lastAccessedRow = index
            return rgbData[0]
        }

        var entry = cacheEntries[index]
        if (entry == null) {
            dirty = true
            if (cachedRowCount >= maxCachedRows) {
                val oldEntry = lruList!!.getNext() as ColorImageCacheEntry
                entry = ColorImageCacheEntry(index, oldEntry.index)
                cacheEntries[oldEntry.row] = null
                oldEntry.unlink()
            } else {
                entry = ColorImageCacheEntry(index, cachedRowCount++)
            }
            cacheEntries[index] = entry
        } else {
            dirty = false
        }
        lruList!!.insertHead(entry)
        return rgbData[entry.index]
    }

    fun getAllPalettes(): Array<Array<IntArray>> {
        if (maxCachedRows != totalRows) {
            throw RuntimeException()
        }
        for (i in 0 until maxCachedRows) {
            cacheEntries[i] = SENTINEL_ENTRY
        }
        return rgbData
    }

    companion object {
        @JvmField val SENTINEL_ENTRY = ColorImageCacheEntry(0, 0)
    }
}
