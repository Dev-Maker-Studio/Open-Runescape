package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.graphics.texture.sampler.CubeTextureSampler

abstract class AbstractNativeCubeTextureGenerator : AbstractCubeTextureGenerator() {
    abstract fun getNativeCubeTexture(): CubeTextureSampler
}
