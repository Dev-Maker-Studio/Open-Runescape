package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.entity.RenderedEntity
import java.util.LinkedList

/**
 *
 * @author Steven - 20/09/2023
 *
 */
class InteractiveEntityCache {

    @JvmField val entities: MutableList<RenderedEntity> = LinkedList()

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

    fun clear() {
        val interactivesIterator = entities.iterator()
        while (interactivesIterator.hasNext()) {
            val interactive = interactivesIterator.next()
            interactivesIterator.remove()
            RenderedEntityCache.clear(interactive)
        }
    }

    fun add(addedReference: RenderedEntity) {
        val addition = addedReference.entity!!
        var skip = true
        val boundingBox = addedReference.entity!!.boundingBoxPolygons!!
        for (index in boundingBox.indices) {
            if (boundingBox[index].positionCalculated) {
                skip = false
                break
            }
        }
        if (!skip) {
            addedReference.parent = this
            if (removeDuplicates) {
                val interactivesIterator = entities.iterator()
                while (interactivesIterator.hasNext()) {
                    val prexistingInteractive = interactivesIterator.next()
                    if (addition === prexistingInteractive.entity) {
                        interactivesIterator.remove()
                        RenderedEntityCache.clear(prexistingInteractive)
                    }
                }
            }
            val listIterator = entities.listIterator()
            while (listIterator.hasNext()) {
                val renderedObject = listIterator.next()
                if (addition.floorDepth >= renderedObject.entity!!.floorDepth) {
                    listIterator.previous()
                    listIterator.add(addedReference)
                    return
                }
            }
            entities.add(addedReference)
        }
    }
}
