package com.jagex.game.runetek5.graphics.sprite.meta;

import com.jagex.game.runetek5.client.GameShell;

public enum AnchorY {
    TOP,
    CENTER,
    BOTTOM;

    public int calculateDrawY(int scaleHeight, int gameHeight) {
        int maxHeight = Math.max(GameShell.screenCanvasHeight * -969250379, gameHeight);
        return this == TOP ? 0 : (this == BOTTOM ? maxHeight - scaleHeight : (this == CENTER ? (maxHeight - scaleHeight) / 2 : 0));
    }
}
