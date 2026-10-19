package com.jagex.game.runetek5.cache.node.lru

import java.lang.ref.SoftReference

class LRUSoftReference(
    value: Any?,
    capacity: Int
) : AbstractLRUNode(capacity) {
    @JvmField
    val softReference: SoftReference<Any?> = SoftReference(value)

    override fun getValue(): Any? {
        return softReference.get()
    }

    override fun isReusable(): Boolean {
        return true
    }
}
