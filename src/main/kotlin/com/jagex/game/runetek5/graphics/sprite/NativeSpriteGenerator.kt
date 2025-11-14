package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.toolkit.AbstractToolkit

class NativeSpriteGenerator : INativeSpriteGenerator {
    @JvmField
    var toolkit: AbstractToolkit? = null

    override fun createSprite(pixels: IntArray, width: Int, useWidth: Int, useHeight: Int): NativeSprite {
        return toolkit!!.createNativeSprite(pixels, 0, width, useWidth, useHeight, false)
    }
}
