package com.jagex.game.runetek5.graphics.shader.program;

import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;

public abstract class AbstractParticleShader {

    public int diffuseColor;
    public NativeTextureSampler diffuseSampler;
    public final Matrix44 textureCoordinateMatrix = new Matrix44();
    protected final AbstractNativeToolkit activeToolkit;

    protected AbstractParticleShader(AbstractNativeToolkit toolkit) {
        this.activeToolkit = toolkit;
    }

    public abstract void method1787(int var1);

    public abstract void method1788(Matrix44 var1);

    public abstract void method1790();

}
