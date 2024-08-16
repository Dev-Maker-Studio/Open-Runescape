package com.jagex.game.runetek5.graphics.loginscreen.component;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.gametip.component.RotatableSpritePositioning;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class RotatedNewsSprite extends NewsSprite {

    public int angle;

    public RotatedNewsSprite(Js5ResourceProvider spriteResource, RotatableSpritePositioning spritePositioning) {
        super(spriteResource, spritePositioning);
    }

    @Override
    public void draw(boolean draw) {
        int x = super.spritePositioning.horizontalAnchor.calculateDrawX(super.sprite.getSpriteWidth(), client.GAME_WIDTH) + super.spritePositioning.spriteOffsetX;
        int y = super.spritePositioning.verticalAnchor.calculateDrawY(super.sprite.getSpriteHeight(), client.GAME_HEIGHT) + super.spritePositioning.spriteOffsetY;
        super.sprite.method2758((x + super.sprite.getSpriteWidth() / 2), (y + super.sprite.getSpriteHeight() / 2), 4096, this.angle);
        this.angle += ((RotatableSpritePositioning) super.spritePositioning).utAngle;
    }
}
