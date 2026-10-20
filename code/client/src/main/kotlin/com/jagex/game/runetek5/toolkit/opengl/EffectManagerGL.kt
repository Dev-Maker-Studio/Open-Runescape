package com.jagex.game.runetek5.toolkit.opengl

import com.jagex.game.runetek5.graphics.texture.water.TextureContainerGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit

class EffectManagerGL(openglToolkit: OpenGlToolkit) {
    @JvmField val textureContainer: TextureContainerGL = TextureContainerGL(openglToolkit)
}
