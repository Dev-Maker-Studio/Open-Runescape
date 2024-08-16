package com.jagex.game.runetek5.graphics.loginscreen.component;

import com.jagex.game.runetek5.config.gametip.component.Class350_Sub3;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.rendering.Renderers;

public class LoadingBar extends LBC {

    NativeSprite aNativeSprite9162;
    public NativeSprite aNativeSprite9161;
    public NativeSprite aNativeSprite9160;
    public NativeSprite aNativeSprite9163;
    public NativeSprite aNativeSprite9164;
    public NativeSprite aNativeSprite9165;

    public LoadingBar(Js5ResourceProvider index1, Js5ResourceProvider index2, Class350_Sub3 class350Sub33) {
        super(index1, index2, class350Sub33);
    }

    @Override
    public void initialize() {
        super.initialize();
        Class350_Sub3 class350Sub32 = (Class350_Sub3) super.anchoredElement;
        this.aNativeSprite9162 = NativeSprite.fromJs5(super.spriteResource, class350Sub32.spriteA);
        this.aNativeSprite9161 = NativeSprite.fromJs5(super.spriteResource, class350Sub32.spriteB);
        this.aNativeSprite9160 = NativeSprite.fromJs5(super.spriteResource, class350Sub32.spriteC);
        this.aNativeSprite9163 = NativeSprite.fromJs5(super.spriteResource, class350Sub32.spriteD);
        this.aNativeSprite9164 = NativeSprite.fromJs5(super.spriteResource, class350Sub32.spriteE);
        this.aNativeSprite9165 = NativeSprite.fromJs5(super.spriteResource, class350Sub32.spriteF);
    }

    @Override
    public void drawOutline(boolean draw, int x, int y) {
        if (draw) {
            int[] clipping = new int[4];
            Renderers.activeToolkit.getClipping(clipping);
            Renderers.activeToolkit.setClipping(x, y, super.anchoredElement.width + x, y + super.anchoredElement.height);
            int width = this.aNativeSprite9160.getSpriteWidth();
            int i_7 = this.aNativeSprite9160.getSpriteHeight();
            int i_8 = this.aNativeSprite9163.getSpriteWidth();
            int i_9 = this.aNativeSprite9163.getSpriteHeight();
            this.aNativeSprite9160.draw(x, y + (super.anchoredElement.height - i_7) / 2);
            this.aNativeSprite9163.draw(super.anchoredElement.width + x - i_8, y + (super.anchoredElement.height - i_9) / 2);
            Renderers.activeToolkit.setClipping(x, y, super.anchoredElement.width + x, y + this.aNativeSprite9164.getSpriteHeight());
            this.aNativeSprite9164.method2756(width + x, y, super.anchoredElement.width - width - i_8, super.anchoredElement.height);
            int height = this.aNativeSprite9165.getSpriteHeight();
            Renderers.activeToolkit.setClipping(x, y + super.anchoredElement.height - height, super.anchoredElement.width + x, y + super.anchoredElement.height);
            this.aNativeSprite9165.method2756(width + x, y + super.anchoredElement.height - height, super.anchoredElement.width - width - i_8, super.anchoredElement.height);
            Renderers.activeToolkit.setClipping(clipping[0], clipping[1], clipping[2], clipping[3]);
        }

    }

    @Override
    public void drawBackground(int x, int y) {
        int clipLeft = x + this.aNativeSprite9160.getSpriteWidth();
        int clipRight = super.anchoredElement.width + x - this.aNativeSprite9163.getSpriteWidth();
        int clipTop = y + this.aNativeSprite9164.getSpriteHeight();
        int clipBottom = y + super.anchoredElement.height - this.aNativeSprite9165.getSpriteHeight();
        int width = clipRight - clipLeft;
        int height = clipBottom - clipTop;
        int loadedPartWidth = loadPercentDecimalFullout() * width / 10000;
        int[] clipping = new int[4];
        Renderers.activeToolkit.getClipping(clipping);
        Renderers.activeToolkit.setClipping(clipLeft, clipTop, clipLeft + loadedPartWidth, clipBottom);
        method14498(clipLeft, clipTop, width, height);
        Renderers.activeToolkit.setClipping(clipLeft + loadedPartWidth, clipTop, clipRight, clipBottom);
        this.aNativeSprite9161.method2756(clipLeft, clipTop, width, height);
        Renderers.activeToolkit.setClipping(clipping[0], clipping[1], clipping[2], clipping[3]);
    }

    void method14498(int x, int y, int width, int height) {
        this.aNativeSprite9162.method2756(x, y, width, height);
    }

    @Override
    public boolean isCached() {
        if (!super.isCached()) {
            return false;
        } else {
            Class350_Sub3 class350Sub32 = (Class350_Sub3) super.anchoredElement;
            return super.spriteResource.isFileCached(class350Sub32.spriteA) &&
            		(super.spriteResource.isFileCached(class350Sub32.spriteB) &&
            				(super.spriteResource.isFileCached(class350Sub32.spriteC) &&
            						(super.spriteResource.isFileCached(class350Sub32.spriteD) &&
            								(super.spriteResource.isFileCached(class350Sub32.spriteE) &&
            										super.spriteResource.isFileCached(class350Sub32.spriteF)))));
        }
    }
}
