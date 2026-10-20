package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.entity.RenderedEntity

class InteractiveEntityCache {
    private val backing = ArrayList<RenderedEntity>(512)
    private var sorted = true

    @JvmField val removeDuplicates: Boolean = false

    @JvmField var hoverOffsetX: Int = 0

    @JvmField var hoverOffsetY: Int = 0

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
