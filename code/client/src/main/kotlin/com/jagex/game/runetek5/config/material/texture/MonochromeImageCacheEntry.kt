package com.jagex.game.runetek5.config.material.texture

import com.jagex.game.runetek5.cache.node.Node

@Suppress("UNCHECKED_CAST")
class MonochromeImageCacheEntry(
    @JvmField val nodeRef: Int,
    @JvmField val paletteIndex: Int
) : Node<Any?>()
