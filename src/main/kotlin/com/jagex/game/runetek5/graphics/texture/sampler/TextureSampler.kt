package com.jagex.game.runetek5.graphics.texture.sampler

interface TextureSampler {
    fun deallocate()
    fun setSamplerMode(samplerMode: SampleMode)
    fun bindTexture()
}
