package com.jagex.game.runetek5.cache.node.lru

class CacheableNodeByteArray(data: ByteArray) : CacheableNode() {
    @JvmField
    val data: ByteArray = data
}
