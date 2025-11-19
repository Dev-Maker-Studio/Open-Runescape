package com.jagex.game.runetek5.toolkit.sw3d

import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.cache.node.impl.ObjectNode

object FinalizeCacheSw3d {

    @JvmField var capacity: Int = 0
    @JvmField var shutdown: Boolean = false
    @JvmField val finalizedObjects: NodeCollection = NodeCollection()

    @JvmStatic
    @Synchronized
    fun addFinalizedObject(nativeObject: INativeObject) {
        if (!shutdown) {
            if (capacity > 0) {
                val objectNode = ObjectNode(nativeObject)
                finalizedObjects.addFirst(objectNode)
            } else {
                nativeObject.destroy(false)
            }
        }
    }

    @JvmStatic
    @Synchronized
    fun incrementCapacity() {
        capacity++
    }

    @JvmStatic
    @Synchronized
    fun decrementCapacity() {
        if (--capacity == 0) {
            destroyAll()
        }
    }

    @JvmStatic
    @Synchronized
    fun shutdown() {
        shutdown = true
    }

    @JvmStatic
    @Synchronized
    fun destroyAll() {
        while (true) {
            val objectNode = finalizedObjects.popHead() as ObjectNode? ?: return
            (objectNode.value as INativeObject).destroy(true)
            objectNode.unlink()
        }
    }
}
