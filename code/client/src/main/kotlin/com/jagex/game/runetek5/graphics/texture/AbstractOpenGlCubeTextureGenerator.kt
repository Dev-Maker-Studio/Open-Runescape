package com.jagex.game.runetek5.graphics.texture

abstract class AbstractOpenGlCubeTextureGenerator : AbstractCubeTextureGenerator() {
    abstract fun getCubeTexture(): OpenGlArbCubeMapTexture?
}
