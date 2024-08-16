package com.jagex.game.runetek5.graphics.sprite.meta;

import com.jagex.game.runetek5.client.GameShell;

public enum AnchorX {
    LEFT,
    CENTER,
    RIGHT;

    public int calculateDrawX(int itemWidth, int gameWidth) {
        int maxWidth = Math.max(GameShell.screenCanvasWidth, gameWidth);
        return this == LEFT ? 0 : (this == RIGHT ? maxWidth - itemWidth : (this == CENTER ? (maxWidth - itemWidth) / 2 : 0));
    }
}
