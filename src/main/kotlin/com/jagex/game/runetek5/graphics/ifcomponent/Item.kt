package com.jagex.game.runetek5.graphics.ifcomponent

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.config.rsinterface.Component

@Suppress("UNCHECKED_CAST")
class Item(
    @JvmField val id: Int,
    @JvmField val amount: Int,
    @JvmField val outlineSize: Int,
    @JvmField val shadowColor: Int,
    @JvmField val stackMode: Component.StackMode,
    @JvmField val hasPlayerAppearance: Boolean
) : Node<Item>()
