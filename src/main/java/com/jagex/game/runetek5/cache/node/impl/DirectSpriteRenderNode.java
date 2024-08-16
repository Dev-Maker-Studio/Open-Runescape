package com.jagex.game.runetek5.cache.node.impl;

import com.jagex.game.runetek5.graphics.sprite.IItemRenderer;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;

public class DirectSpriteRenderNode extends SpriteRenderNode {

    public final NativeSprite anObject10381;

    public DirectSpriteRenderNode(IItemRenderer interface181, NativeSprite object2, int i_3) {
        super(interface181, i_3);
        this.anObject10381 = object2;
    }

    @Override
    public NativeSprite createNativeSprite() {
        return this.anObject10381;
    }

    @Override
    public boolean hasSoftReference() {
        return false;
    }

}
