package com.jagex.game.runetek5.config.rsinterface

import com.jagex.game.runetek5.cache.node.Node

@Suppress("UNCHECKED_CAST")
open class SubInterface(
    @JvmField val subInterfaceId: Int,
    @JvmField val overlay: Int
) : Node<Any?>() {
    open fun executeTrigger(): Boolean {
        return true
    }
}
