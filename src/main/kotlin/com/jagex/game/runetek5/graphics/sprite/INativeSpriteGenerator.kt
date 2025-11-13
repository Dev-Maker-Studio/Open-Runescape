package com.jagex.game.runetek5.graphics.sprite

interface INativeSpriteGenerator {
    fun createSprite(pixels: IntArray, width: Int, useWidth: Int, useHeight: Int): NativeSprite
}
