package com.jagex.game.runetek5.entity.grounditem

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.NodeCollection

@Suppress("UNCHECKED_CAST")
class GroundItemWrapper : Node<Any?>() {
    @JvmField
    val groundItems: NodeCollection = NodeCollection()
}
