package com.jagex.game.runetek5.cache.node.impl

import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.graphics.sprite.IItemRenderer
import com.jagex.game.runetek5.graphics.sprite.NativeSprite

abstract class SpriteRenderNode(
    @JvmField val itemRenderer: IItemRenderer,
    @JvmField val size: Int
) : CacheableNode() {

    abstract fun createNativeSprite(): NativeSprite?

    abstract fun hasSoftReference(): Boolean
}
