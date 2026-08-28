package com.jagex.game.runetek5.cache.node.impl

import com.jagex.game.runetek5.cache.node.Node

@Suppress("UNCHECKED_CAST")
class ShortNode(shortValue: Short) : @Suppress("UNCHECKED_CAST") Node<Any?>() {
    @JvmField
    val value: Short = shortValue
}
