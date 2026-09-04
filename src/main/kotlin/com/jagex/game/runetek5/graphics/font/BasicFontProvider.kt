package com.jagex.game.runetek5.graphics.font

import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.rendering.Renderers

class BasicFontProvider : FontProvider {
    override fun createFontRenderer(fontData: ByteArray, fontMetrics: FontMetrics, monospaced: Boolean): Any {
        return Renderers.activeToolkit!!.createFontRenderer(fontMetrics, SpriteType.decode(fontData), monospaced)
    }
}
