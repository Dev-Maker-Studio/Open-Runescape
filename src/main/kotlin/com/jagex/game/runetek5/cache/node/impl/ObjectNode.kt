package com.jagex.game.runetek5.cache.node.impl

import com.jagex.game.runetek5.cache.node.Node

class ObjectNode(value: Any?) : Node<Any?>() {
    @JvmField
    val value: Any? = value
}
