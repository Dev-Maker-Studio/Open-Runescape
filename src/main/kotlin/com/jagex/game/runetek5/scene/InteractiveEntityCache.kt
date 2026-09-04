package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.entity.RenderedEntity

/**
 *
 * @author Steven - 20/09/2023
 *
 */
class InteractiveEntityCache {
    /**
     * Entities drawn this frame that the mouse can interact with, far to near.
     *
     * The order matters (it is the order the mini-menu lists overlapping entities),
     * but it is only READ once the frame's draws are done. It used to be maintained by
     * inserting every entity into a sorted LinkedList — a linear scan per insertion,
     * quadratic per frame, several milliseconds at a few thousand drawn entities.
     * Now additions append and the list is sorted once, lazily, when first read.
     */
    private val backing = ArrayList<RenderedEntity>(512)
    private var sorted = true

    /**
     * Always false
     */
    @JvmField val removeDuplicates: Boolean = false

    /**
     * Always 0
     */
    @JvmField var hoverOffsetX: Int = 0

    /**
     * Always 0
     */
    @JvmField var hoverOffsetY: Int = 0

    /**
     * Far-to-near: floorDepth descending, and among equal depths the entity added LAST
     * comes first — exactly what the old "insert before the first entry with a depth
     * <= mine" produced.
     */
    val entities: List<RenderedEntity>
        get() {
            if (!sorted) {
                backing.reverse()
                backing.sortWith(compareByDescending { it.entity?.floorDepth ?: Int.MIN_VALUE })
                sorted = true
            }
            return backing
        }

    fun clear() {
        for (i in backing.indices) RenderedEntityCache.clear(backing[i])
        backing.clear()
        sorted = true
    }

    fun add(addedReference: RenderedEntity) {
        val addition = addedReference.entity!!
        var skip = true
        val boundingBox = addition.boundingBoxPolygons!!
        for (index in boundingBox.indices) {
            if (boundingBox[index].positionCalculated) {
                skip = false
                break
            }
        }
        if (!skip) {
            addedReference.parent = this
            if (removeDuplicates) {
                val iterator = backing.iterator()
                while (iterator.hasNext()) {
                    val existing = iterator.next()
                    if (addition === existing.entity) {
                        iterator.remove()
                        RenderedEntityCache.clear(existing)
                    }
                }
            }
            backing.add(addedReference)
            sorted = false
        }
    }
}
