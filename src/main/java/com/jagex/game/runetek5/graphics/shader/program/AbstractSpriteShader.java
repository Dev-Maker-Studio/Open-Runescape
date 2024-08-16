package com.jagex.game.runetek5.graphics.shader.program;

import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.NativeStream;
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclaration;

public abstract class AbstractSpriteShader {

    public NativeTextureSampler anInterface6441;
    public NativeTextureSampler anInterface6443;
    public NativeStream anInterface4445;
    public int anInt446;
    public VertexDeclaration aClass70447;
    public final Matrix44 worldViewProjectionMatrix = new Matrix44();
    public final Matrix44 aClass384440 = new Matrix44();
    public final Matrix44 aClass384444 = new Matrix44();
    protected final AbstractNativeToolkit activeToolkit;

    public AbstractSpriteShader(AbstractNativeToolkit nativeToolkit) {
        this.activeToolkit = nativeToolkit;
    }

    public abstract void loadNormal();

    public abstract void setColours(int type, int value);

    public abstract void loadMasked();

}
