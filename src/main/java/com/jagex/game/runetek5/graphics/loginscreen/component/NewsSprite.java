package com.jagex.game.runetek5.graphics.loginscreen.component;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.gametip.component.SpritePositioning;
import com.jagex.game.runetek5.graphics.loginscreen.DrawableItem;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class NewsSprite implements DrawableItem {

    NativeSprite sprite;

    public final Js5ResourceProvider spriteResource;

    final SpritePositioning spritePositioning;

    public NewsSprite(Js5ResourceProvider spriteResource, SpritePositioning spritePositioning) {
        this.spriteResource = spriteResource;
        this.spritePositioning = spritePositioning;
    }

    @Override
    public boolean isCached() {
        return this.spriteResource.isFileCached(this.spritePositioning.spriteId);
    }

    @Override
    public void initialize() {
        this.sprite = NativeSprite.fromJs5(this.spriteResource, this.spritePositioning.spriteId);
    }

    @Override
    public void draw(boolean draw) {
        if (draw) {
            int x = this.spritePositioning.horizontalAnchor.calculateDrawX(this.sprite.getSpriteWidth(), client.GAME_WIDTH) + this.spritePositioning.spriteOffsetX;
            int y = this.spritePositioning.verticalAnchor.calculateDrawY(this.sprite.getSpriteHeight(), client.GAME_HEIGHT) + this.spritePositioning.spriteOffsetY;
            this.sprite.draw(x, y);
        }
    }
}
