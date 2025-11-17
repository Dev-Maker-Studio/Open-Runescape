package com.jagex.game.runetek5.client.periodic

import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.utils.Utils

class PulseEvent(type: Int, data: Long) : CacheableNode() {
    @JvmField var string: String? = null
    @JvmField var secondary: Int = 0
    @JvmField var tertiary: Int = 0
    @JvmField var primary: Int = 0

    init {
        this.pointer = (type.toLong() shl 56) or data
    }

    fun getType(): Int {
        return ((super.pointer ushr 56) and 0xffL).toInt()
    }

    fun executeAfter(): Long {
        return super.indexReference and 0x7fffffffffffffffL
    }

    fun getValue(): Long {
        return super.pointer and 0xffffffffffffffL
    }

    fun queueLowPriority() {
        super.indexReference = super.indexReference and Long.MIN_VALUE or (Utils.time() + 500L)
        PulseEventHandler.LOW_PRIORITY.insert(this)
    }

    fun queueHighPriority() {
        super.indexReference = super.indexReference or Long.MIN_VALUE
        if (executeAfter() == 0L) {
            PulseEventHandler.HIGH_PRIORITY.insert(this)
        }
    }
}
