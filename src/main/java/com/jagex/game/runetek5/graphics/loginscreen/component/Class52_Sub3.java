package com.jagex.game.runetek5.graphics.loginscreen.component;

import com.jagex.game.runetek5.config.gametip.component.Class350_Sub2;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.rendering.Renderers;

public class Class52_Sub3 extends LBC {

    public Class52_Sub3(Js5ResourceProvider spriteResource, Js5ResourceProvider fontResource, Class350_Sub2 class350Sub23) {
        super(spriteResource, fontResource, class350Sub23);
    }

    @Override
    public void drawBackground(int x, int y) {
        int width = loadPercentDecimalFullout() * super.anchoredElement.width / 10000;
        Renderers.activeToolkit.drawFilledRectangle(x, y + 2, width, super.anchoredElement.height - 2, ((Class350_Sub2) super.anchoredElement).anInt7813, 0);
        Renderers.activeToolkit.drawFilledRectangle(width + x, y + 2, super.anchoredElement.width - width, super.anchoredElement.height - 2, 0, 0);
    }

    @Override
    public void drawOutline(boolean draw, int x, int y) {
        Renderers.activeToolkit.drawRectangleOutline(x - 2, y, super.anchoredElement.width + 4, super.anchoredElement.height + 2, ((Class350_Sub2) super.anchoredElement).anInt7814, 0);
        Renderers.activeToolkit.drawRectangleOutline(x - 1, y + 1, super.anchoredElement.width + 2, super.anchoredElement.height, 0, 0);
    }
}
