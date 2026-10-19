package com.jagex.game.runetek5.cache.node.impl

import com.jagex.game.runetek5.graphics.sprite.IItemRenderer
import com.jagex.game.runetek5.graphics.sprite.NativeSprite

class DirectSpriteRenderNode(
    itemRenderer: IItemRenderer,
    @JvmField val nativeSprite: NativeSprite,
    size: Int
) : SpriteRenderNode(itemRenderer, size) {

    override fun createNativeSprite(): NativeSprite {
        return nativeSprite
    }

    override fun hasSoftReference(): Boolean {
        return false
    }
}
