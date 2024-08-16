package com.jagex.game.runetek5.cache.node.impl;

import java.lang.ref.SoftReference;

import com.jagex.game.runetek5.graphics.sprite.IItemRenderer;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;

public class ReferencedSpriteRenderNode extends SpriteRenderNode {

    public final SoftReference<NativeSprite> aSoftReference10410;

    public ReferencedSpriteRenderNode(IItemRenderer interface181, NativeSprite object2, int i_3) {
        super(interface181, i_3);
        this.aSoftReference10410 = new SoftReference<NativeSprite>(object2);
    }

    @Override
    public boolean hasSoftReference() {
        return true;
    }

    @Override
    public NativeSprite createNativeSprite() {
        return this.aSoftReference10410.get();
    }

}
