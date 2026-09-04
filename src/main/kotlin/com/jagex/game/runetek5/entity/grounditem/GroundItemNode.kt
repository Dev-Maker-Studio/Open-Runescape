package com.jagex.game.runetek5.entity.grounditem

import com.jagex.game.runetek5.cache.node.Node

@Suppress("UNCHECKED_CAST")
class GroundItemNode(
    @JvmField val id: Int,
    @JvmField var amount: Int
) : Node<Any?>()
