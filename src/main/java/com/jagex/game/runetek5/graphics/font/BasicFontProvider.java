package com.jagex.game.runetek5.graphics.font;

import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.rendering.Renderers;

public class BasicFontProvider implements FontProvider {

    @Override
    public Object createFontRenderer(byte[] spriteTypeBytes, FontMetrics fontMetrics, boolean bool3) {
        return Renderers.activeToolkit.createFontRenderer(fontMetrics, SpriteType.decode(spriteTypeBytes), bool3);
    }

}
