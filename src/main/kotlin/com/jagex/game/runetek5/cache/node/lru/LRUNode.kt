package com.jagex.game.runetek5.cache.node.lru

class LRUNode(
    @JvmField val value: Any?,
    capacity: Int
) : AbstractLRUNode(capacity) {
    override fun isReusable(): Boolean {
        return false
    }

    override fun getValue(): Any? {
        return value
    }
}
