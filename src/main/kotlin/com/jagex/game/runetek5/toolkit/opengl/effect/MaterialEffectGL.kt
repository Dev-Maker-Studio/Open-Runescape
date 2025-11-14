package com.jagex.game.runetek5.toolkit.opengl.effect

import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.toolkit.OpenGlToolkit

abstract class MaterialEffectGL(
    @JvmField val glToolkit: OpenGlToolkit
) {

    abstract fun isInitialized(): Boolean

    abstract fun toggle(enabled: Boolean)

    abstract fun disable()

    abstract fun applyEffectParams(effectParam1: Int, effectParam2: Int)

    abstract fun loadCombiner(combiner: OpenGlTexture, combineMode: Int)

    abstract fun loadCombineMode()
}
