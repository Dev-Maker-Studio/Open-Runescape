package com.jagex.game.runetek5.cache.node.impl

import com.jagex.game.runetek5.cache.node.Node

@Suppress("UNCHECKED_CAST")
class LongNode(value: Long) : @Suppress("UNCHECKED_CAST") Node<Any?>() {
    @JvmField
    var bitpacked: Long = value
}
