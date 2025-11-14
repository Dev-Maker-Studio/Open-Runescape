package com.jagex.game.runetek5.cache.node.impl

import com.jagex.game.runetek5.graphics.sprite.IItemRenderer
import com.jagex.game.runetek5.graphics.sprite.NativeSprite

class DirectSpriteRenderNode(
    interface181: IItemRenderer,
    @JvmField val anObject10381: NativeSprite,
    i_3: Int
) : SpriteRenderNode(interface181, i_3) {

    override fun createNativeSprite(): NativeSprite {
        return anObject10381
    }

    override fun hasSoftReference(): Boolean {
        return false
    }
}
