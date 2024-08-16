package com.jagex.game.runetek5.graphics.font;

public interface FontProvider {

    Object createFontRenderer(byte[] fontData, FontMetrics fontMetrics, boolean monospaced);

}
