package com.jagex.game.runetek5.config.material.texture

import com.jagex.game.runetek5.cache.node.LinkedList

/**
 * Cache for monochrome texture operation output.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class MonochromeImageCache(
    @JvmField val paletteSize: Int,
    @JvmField val anInt3715: Int,
    dataLength: Int
) {

    @JvmField var dirty: Boolean = false
    @JvmField var nodes: Array<MonochromeImageCacheEntry?>
    @JvmField var paletteData: Array<IntArray?>
    @JvmField var nodelist: LinkedList? = LinkedList()
    @JvmField var anInt3714: Int = 0
    @JvmField var lastUsedNodeIndex: Int = -1

    init {
        paletteData = arrayOfNulls(paletteSize)
        for (i in 0 until paletteSize) {
            paletteData[i] = IntArray(dataLength)
        }
        nodes = arrayOfNulls(anInt3715)
    }

    /**
     * Resets and releases the cache.
     */
    fun reset() {
        for (i in 0 until paletteSize) {
            paletteData[i] = null
        }
        nodes = emptyArray()
        paletteData = emptyArray()
        nodelist!!.clear()
        nodelist = null
    }

    /**
     * Gets a palette row, managing the cache.
     *
     * @param index The row index
     * @return The palette data for this row
     */
    fun getPalette(index: Int): IntArray {
        if (anInt3715 == paletteSize) {
            dirty = nodes[index] == null
            nodes[index] = EMPTY
            return paletteData[index]!!
        }

        if (paletteSize != 1) {
            var entry = nodes[index]
            if (entry == null) {
                dirty = true
                if (anInt3714 >= paletteSize) {
                    val oldEntry = nodelist!!.getNext() as MonochromeImageCacheEntry
                    entry = MonochromeImageCacheEntry(index, oldEntry.anInt7689)
                    nodes[oldEntry.nodeRef] = null
                    oldEntry.unlink()
                } else {
                    entry = MonochromeImageCacheEntry(index, anInt3714)
                    anInt3714++
                }
                nodes[index] = entry
            } else {
                dirty = false
            }
            nodelist!!.insertHead(entry)
            return paletteData[entry.anInt7689]!!
        }

        dirty = lastUsedNodeIndex != index
        lastUsedNodeIndex = index
        return paletteData[0]!!
    }

    /**
     * Gets all palette data.
     *
     * @return All palette data
     */
    fun method5722(): Array<IntArray?> {
        if (paletteSize != anInt3715) {
            throw RuntimeException()
        }
        for (i in 0 until paletteSize) {
            nodes[i] = EMPTY
        }
        return paletteData
    }

    companion object {
        @JvmField val EMPTY = MonochromeImageCacheEntry(0, 0)
    }
}
