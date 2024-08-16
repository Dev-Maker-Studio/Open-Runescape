package com.jagex.game.runetek5.graphics.shader.program.impl;

import com.jagex.game.runetek5.graphics.shader.program.AbstractParticleShader;
import com.jagex.game.runetek5.graphics.texture.TextureStageState;
import com.jagex.game.runetek5.graphics.texture.TextureState;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode;
import com.jagex.game.runetek5.toolkit.companion.TextureCombineMode;

public class NativeParticleShaderLd extends AbstractParticleShader {

    public NativeParticleShaderLd(AbstractNativeToolkit nativeToolkit) {
        super(nativeToolkit);
    }

    @Override
    public void method1788(Matrix44 matrix441) {
        super.activeToolkit.setMatrixTransform(matrix441, super.activeToolkit.particleMatrix, super.activeToolkit.projectionMatrix);
    }

    @Override
    public void method1790() {
        super.activeToolkit.setColourValue(super.diffuseColor);
        super.activeToolkit.method13967(1, TextureStageState.aClass67677);
        super.activeToolkit.method13969(1, TextureStageState.aClass67677);
        method14513();
        super.activeToolkit.method14150();
    }

    @Override
    public void method1787(int i_1) {
        super.activeToolkit.method13967(1, TextureStageState.aClass67678);
        super.activeToolkit.method13969(1, TextureStageState.aClass67678);
        method14513();
        super.activeToolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, 0, i_1 * 4, 0, i_1 * 2);
    }

    public void method14513() {
        super.activeToolkit.setTextureSlot(0);
        super.activeToolkit.setTextureSampler(super.diffuseSampler);
        super.activeToolkit.method13967(0, TextureStageState.aClass67680);
        super.activeToolkit.method13969(0, TextureStageState.aClass67680);
        super.activeToolkit.method13966(TextureCombineMode.MODULATE, TextureCombineMode.MODULATE);
        super.activeToolkit.method13973().copyFrom(super.textureCoordinateMatrix);
        super.activeToolkit.setTextureState(TextureState.PARTIAL_COPY_LAST);
    }

}
