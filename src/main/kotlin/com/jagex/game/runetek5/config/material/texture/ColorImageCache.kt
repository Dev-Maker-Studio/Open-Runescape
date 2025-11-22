package com.jagex.game.runetek5.config.material.texture

import com.jagex.game.runetek5.cache.node.LinkedList

/**
 * Cache for color texture operation output with RGB channels.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class ColorImageCache(
    @JvmField val anInt3613: Int,
    @JvmField val anInt3611: Int,
    dataLength: Int
) {

    @JvmField var dirty: Boolean = false
    @JvmField var aNodeSub16Array3615: Array<ColorImageCacheEntry?>
    @JvmField var anIntArrayArrayArray3614: Array<Array<IntArray>>
    @JvmField var aClass4733612: LinkedList? = LinkedList()
    @JvmField var anInt3617: Int = 0
    @JvmField var anInt3616: Int = -1

    init {
        anIntArrayArrayArray3614 = Array(anInt3613) {
            Array(3) { IntArray(dataLength) }
        }
        aNodeSub16Array3615 = arrayOfNulls(anInt3611)
    }

    /**
     * Clears and releases the cache.
     */
    fun clear() {
        aNodeSub16Array3615 = emptyArray()
        anIntArrayArrayArray3614 = emptyArray()
        aClass4733612!!.clear()
        aClass4733612 = null
    }

    /**
     * Gets a palette row, managing the cache.
     *
     * @param index The row index
     * @return The RGB palette data for this row
     */
    fun getPalette(index: Int): Array<IntArray> {
        if (anInt3611 == anInt3613) {
            dirty = aNodeSub16Array3615[index] == null
            aNodeSub16Array3615[index] = aNodeSub163618
            return anIntArrayArrayArray3614[index]
        }

        if (anInt3613 == 1) {
            dirty = anInt3616 != index
            anInt3616 = index
            return anIntArrayArrayArray3614[0]
        }

        var entry = aNodeSub16Array3615[index]
        if (entry == null) {
            dirty = true
            if (anInt3617 >= anInt3613) {
                val oldEntry = aClass4733612!!.getNext() as ColorImageCacheEntry
                entry = ColorImageCacheEntry(index, oldEntry.index)
                aNodeSub16Array3615[oldEntry.row] = null
                oldEntry.unlink()
            } else {
                entry = ColorImageCacheEntry(index, anInt3617++)
            }
            aNodeSub16Array3615[index] = entry
        } else {
            dirty = false
        }
        aClass4733612!!.insertHead(entry)
        return anIntArrayArrayArray3614[entry.index]
    }

    /**
     * Gets all palette data.
     *
     * @return All RGB palette data
     */
    fun method5464(): Array<Array<IntArray>> {
        if (anInt3613 != anInt3611) {
            throw RuntimeException()
        }
        for (i in 0 until anInt3613) {
            aNodeSub16Array3615[i] = aNodeSub163618
        }
        return anIntArrayArrayArray3614
    }

    companion object {
        @JvmField var CS2_QUERY_RESULTS: ShortArray? = null
        @JvmField val aNodeSub163618 = ColorImageCacheEntry(0, 0)
    }
}
