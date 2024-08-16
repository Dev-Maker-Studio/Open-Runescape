package com.jagex.game.runetek5.graphics.loginscreen.component;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.gametip.component.AnchoredTextMeta;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.loginscreen.DrawableItem;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.rendering.Renderers;

public class AnchoredText implements DrawableItem {

    public final AnchoredTextMeta item;

    public final Js5ResourceProvider spriteResource;

    public final Js5ResourceProvider fontResource;

    public AbstractFontRenderer frontRenderer;

    public AnchoredText(Js5ResourceProvider spriteResource, Js5ResourceProvider fontResource, AnchoredTextMeta class623) {
    	this.item = class623;
        this.spriteResource = spriteResource;
        this.fontResource = fontResource;
    }

    @Override
    public void draw(boolean draw) {
        if (draw) {
            int x = this.item.anchorX.calculateDrawX(this.item.width, client.GAME_WIDTH) + this.item.offsetX;
            int y = this.item.anchorY.calculateDrawY(this.item.height, client.GAME_HEIGHT) + this.item.offsetY;
            this.frontRenderer.drawText(this.item.tipText, x, y, this.item.width, this.item.height, this.item.textColor, this.item.textShadowColor,
            		this.item.textAllignment, this.item.textVerticalAllignment, this.item.lineSpacing, null, null, null, 0, 0);
        }
    }


    @Override
    public boolean isCached() {
        boolean cached = this.spriteResource.isFileCached(this.item.fileId);
        if (!this.fontResource.isFileCached(this.item.fileId)) {
            cached = false;
        }
        return cached;
    }

    @Override
    public void initialize() {
        FontMetrics fontMetrics = FontMetrics.list(this.fontResource, this.item.fileId);
        this.frontRenderer = Renderers.activeToolkit.createFontRenderer(fontMetrics, SpriteType.listGroup(this.spriteResource, this.item.fileId), true);
    }
}
