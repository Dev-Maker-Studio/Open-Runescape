package com.jagex.game.runetek5.graphics.font

interface FontProvider {
    fun createFontRenderer(fontData: ByteArray, fontMetrics: FontMetrics, monospaced: Boolean): Any
}
