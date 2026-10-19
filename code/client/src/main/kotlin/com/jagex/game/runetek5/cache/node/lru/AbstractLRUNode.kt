package com.jagex.game.runetek5.cache.node.lru

abstract class AbstractLRUNode(
    @JvmField val capacityRequirement: Int
) : CacheableNode() {
    abstract fun isReusable(): Boolean

    abstract fun getValue(): Any?
}
