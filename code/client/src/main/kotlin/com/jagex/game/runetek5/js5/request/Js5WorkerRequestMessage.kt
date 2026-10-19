package com.jagex.game.runetek5.js5.request

import com.jagex.game.runetek5.cache.node.lru.CacheableNode

/**
 * [J]
 * @author Steven - 11/10/2023
 *
 */
abstract class Js5WorkerRequestMessage : CacheableNode() {
    @JvmField
    var highPriority: Boolean = false

    @JvmField
    var isCompleted: Boolean = false

    @Volatile
    @JvmField
    var waiting: Boolean = true

    abstract fun getPayload(): ByteArray?

    abstract fun getCompletion(): Int
}
