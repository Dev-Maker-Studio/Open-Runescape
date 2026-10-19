package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.graphics.model.Model

class TextureTransformationMatrix(
    @JvmField val model: Model,
    @JvmField val centerX: IntArray?,
    @JvmField val centerY: IntArray?,
    @JvmField val centerZ: IntArray?,
    @JvmField val matrix: Array<FloatArray>?
)
