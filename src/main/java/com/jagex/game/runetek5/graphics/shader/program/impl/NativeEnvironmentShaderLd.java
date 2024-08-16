package com.jagex.game.runetek5.graphics.shader.program.impl;

import com.jagex.game.runetek5.graphics.shader.program.AbstractEnvironmentShader;
import com.jagex.game.runetek5.graphics.texture.TextureStageState;
import com.jagex.game.runetek5.graphics.texture.TextureState;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode;
import com.jagex.game.runetek5.toolkit.companion.TextureCombineMode;

public class NativeEnvironmentShaderLd extends AbstractEnvironmentShader {

    public NativeEnvironmentShaderLd(AbstractNativeToolkit class505Sub21) {
        super(class505Sub21);
    }

    @Override
    public void loadWorldViewProjection(Matrix44 matrix441) {
        super.activeToolkit.setMatrixTransform(matrix441, super.activeToolkit.particleMatrix, super.activeToolkit.projectionMatrix);
    }

    @Override
    public void loadUnlitProgram(boolean bool1) {
        super.activeToolkit.setTextureSlot(0);
        super.activeToolkit.method13967(0, TextureStageState.aClass67680);
        super.activeToolkit.method13969(0, TextureStageState.aClass67680);
        super.activeToolkit.method13967(1, TextureStageState.aClass67678);
        super.activeToolkit.method13969(1, TextureStageState.aClass67678);
        super.activeToolkit.method13966(TextureCombineMode.MODULATE, TextureCombineMode.MODULATE);
        method14515();
    }

    public void method14515() {
        super.activeToolkit.setTextureSampler(super.textureSampler);
        super.activeToolkit.method13973().copyFrom(super.textureCoordinateMatrix);
        super.activeToolkit.setTextureState(TextureState.PARTIAL_COPY_LAST);
        super.activeToolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, super.anInt467, super.anInt468, super.anInt469, super.anInt470);
    }

    @Override
    public void loadUnderwaterGround() {
        loadStandartPointlight(0);
    }

    @Override
    public void loadPointlightEnvironmentMapping(int i_1) {
        super.activeToolkit.setTextureSlot(0);
        super.activeToolkit.method13966(TextureCombineMode.MODULATE, TextureCombineMode.REPLACE);
        super.activeToolkit.method13967(0, TextureStageState.aClass67680);
        super.activeToolkit.method13969(0, TextureStageState.aClass67678);
        super.activeToolkit.method13967(1, TextureStageState.aClass67678);
        super.activeToolkit.method13969(1, TextureStageState.aClass67678);
        method14515();
    }

    @Override
    public void loadSpecularPointlight(int i_1) {
        super.activeToolkit.setTextureSlot(0);
        super.activeToolkit.method13966(TextureCombineMode.MODULATE, TextureCombineMode.REPLACE);
        super.activeToolkit.method13967(0, TextureStageState.aClass67680);
        super.activeToolkit.method13969(0, TextureStageState.aClass67678);
        super.activeToolkit.method13967(1, TextureStageState.aClass67678);
        super.activeToolkit.method13969(1, TextureStageState.aClass67678);
        method14515();
    }

    @Override
    public void loadStandartPointlight(int i_1) {
        super.activeToolkit.setTextureSlot(0);
        super.activeToolkit.method13966(TextureCombineMode.MODULATE, TextureCombineMode.MODULATE);
        super.activeToolkit.method13967(0, TextureStageState.aClass67680);
        super.activeToolkit.method13969(0, TextureStageState.aClass67680);
        super.activeToolkit.method13967(1, TextureStageState.aClass67678);
        super.activeToolkit.method13969(1, TextureStageState.aClass67678);
        method14515();
    }

}
