package com.jagex.game.runetek5.cache.node.impl

import java.lang.ref.SoftReference
import com.jagex.game.runetek5.graphics.sprite.IItemRenderer
import com.jagex.game.runetek5.graphics.sprite.NativeSprite

class ReferencedSpriteRenderNode(
    interface181: IItemRenderer,
    object2: NativeSprite,
    i_3: Int
) : SpriteRenderNode(interface181, i_3) {

    @JvmField
    val aSoftReference10410: SoftReference<NativeSprite> = SoftReference(object2)

    override fun hasSoftReference(): Boolean {
        return true
    }

    override fun createNativeSprite(): NativeSprite {
        return aSoftReference10410.get()!!
    }
}
