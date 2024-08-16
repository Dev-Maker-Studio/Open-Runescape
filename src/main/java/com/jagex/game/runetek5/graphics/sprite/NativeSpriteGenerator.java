package com.jagex.game.runetek5.graphics.sprite;

import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class NativeSpriteGenerator implements INativeSpriteGenerator {

    public AbstractToolkit toolkit;

    @Override
    public NativeSprite createSprite(int[] pixels, int width, int useWidth, int useHeight) {
        return this.toolkit.createNativeSprite(pixels, 0, width, useWidth, useHeight, false);
    }

}
