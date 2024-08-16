package com.jagex.game.runetek5.cache.node.impl;

import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.graphics.sprite.IItemRenderer;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;

public abstract class SpriteRenderNode extends CacheableNode {

    public final IItemRenderer itemRenderer;
    public final int size;

    public SpriteRenderNode(IItemRenderer interface181, int i_2) {
        this.itemRenderer = interface181;
        this.size = i_2;
    }

    public abstract NativeSprite createNativeSprite();

    public abstract boolean hasSoftReference();

}
