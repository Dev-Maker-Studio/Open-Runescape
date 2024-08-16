package com.jagex.game.runetek5.graphics.texture.sampler;

public interface TextureSampler {

    void deallocate();

    void setSamplerMode(SampleMode samplerMode);

    void bindTexture();

}
