package com.jagex.game.runetek5.graphics.loginscreen.component;

import com.jagex.game.runetek5.config.gametip.component.Class350_Sub1;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.rendering.Renderers;

public class Class52_Sub1 extends LBC {

    public NativeSprite aNativeSprite9150;

    public Class52_Sub1(Js5ResourceProvider index1, Js5ResourceProvider index2, Class350_Sub1 class350Sub13) {
        super(index1, index2, class350Sub13);
    }

    @Override
    public void drawOutline(boolean bool1, int i_2, int i_3) {
        Renderers.activeToolkit.drawRectangleOutline(i_2 - 2, i_3, super.anchoredElement.width + 4, super.anchoredElement.height + 2, ((Class350_Sub1) super.anchoredElement).backgroundColor, 0);
        Renderers.activeToolkit.drawRectangleOutline(i_2 - 1, i_3 + 1, super.anchoredElement.width + 2, super.anchoredElement.height, 0, 0);
    }

    @Override
    public void drawBackground(int x, int y) {
        int width = loadPercentDecimalFullout() * super.anchoredElement.width / 10000;
        int[] clipping = new int[4];
        Renderers.activeToolkit.getClipping(clipping);
        Renderers.activeToolkit.setClipping(x, y + 2, width + x, y + super.anchoredElement.height);
        this.aNativeSprite9150.method2756(x, y + 2, super.anchoredElement.width, super.anchoredElement.height);
        Renderers.activeToolkit.setClipping(clipping[0], clipping[1], clipping[2], clipping[3]);
    }

    @Override
    public void initialize() {
        super.initialize();
        this.aNativeSprite9150 = NativeSprite.fromJs5(super.spriteResource, ((Class350_Sub1) super.anchoredElement).spriteFileId);
    }

    @Override
    public boolean isCached() {
        return super.isCached() && super.spriteResource.isFileCached(((Class350_Sub1) super.anchoredElement).spriteFileId);
    }
}
