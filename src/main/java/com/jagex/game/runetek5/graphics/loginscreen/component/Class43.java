package com.jagex.game.runetek5.graphics.loginscreen.component;

import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.gametip.component.TipsSprite;
import com.jagex.game.runetek5.graphics.loginscreen.DrawableItem;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class Class43 implements DrawableItem {

    public final Js5ResourceProvider spriteResource;

    public final TipsSprite tipsSprite;

    public NativeSprite nativeSprite;

    public Class43(Js5ResourceProvider spriteResource, TipsSprite tipsSprite) {
        this.spriteResource = spriteResource;
        this.tipsSprite = tipsSprite;
    }

    @Override
    public void initialize() {
        this.nativeSprite = NativeSprite.fromJs5(this.spriteResource, this.tipsSprite.spriteFileId);
    }

    @Override
    public boolean isCached() {
        return this.spriteResource.isFileCached(this.tipsSprite.spriteFileId);
    }

    @Override
    public void draw(boolean draw) {
        if (draw) {
            int width = Math.max(GameShell.screenCanvasWidth, client.GAME_WIDTH);
            int height = Math.max(GameShell.screenCanvasHeight * -969250379, client.GAME_HEIGHT);
            int scaleWidth = this.nativeSprite.getSpriteWidth();
            int scaleHeight = this.nativeSprite.getSpriteHeight();
            int x = 0;
            int drawWidth = width;
            int drawLength = width * scaleHeight / scaleWidth;
            int y = (height - drawLength) / 2;
            if (drawLength > height) {
                y = 0;
                drawLength = height;
                drawWidth = height * scaleWidth / scaleHeight;
                x = (width - drawWidth) / 2;
            }
            this.nativeSprite.drawCircle(x, y, drawWidth, drawLength);
        }
    }
}
