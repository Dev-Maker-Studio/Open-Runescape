package com.jagex.game.runetek5.config.material.texture

import com.jagex.game.runetek5.cache.node.LinkedList

/**
 * Cache for color texture operation output with RGB channels.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
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

    /**
     * Clears and releases the cache.
     */
    fun clear() {
        cacheEntries = emptyArray()
        rgbData = emptyArray()
        lruList!!.clear()
        lruList = null
    }

    /**
     * Gets a palette row, managing the cache.
     *
     * @param index The row index
     * @return The RGB palette data for this row
     */
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

    /**
     * Gets all palette data.
     *
     * @return All RGB palette data
     */
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
