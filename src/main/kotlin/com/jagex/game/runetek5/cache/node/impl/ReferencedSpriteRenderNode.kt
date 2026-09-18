package com.jagex.game.runetek5.cache.node.impl

import java.lang.ref.SoftReference
import com.jagex.game.runetek5.graphics.sprite.IItemRenderer
import com.jagex.game.runetek5.graphics.sprite.NativeSprite

class ReferencedSpriteRenderNode(
    itemRenderer: IItemRenderer,
    nativeSprite: NativeSprite,
    size: Int
) : SpriteRenderNode(itemRenderer, size) {

    @JvmField
    val nativeSpriteRef: SoftReference<NativeSprite> = SoftReference(nativeSprite)

    override fun hasSoftReference(): Boolean {
        return true
    }

    override fun createNativeSprite(): NativeSprite? {
        return nativeSpriteRef.get()
    }
}
