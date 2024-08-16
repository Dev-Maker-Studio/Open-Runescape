package com.jagex.game.runetek5.graphics.shader.program.impl;

import com.jagex.game.runetek5.graphics.shader.program.AbstractSpriteShader;
import com.jagex.game.runetek5.graphics.texture.TextureStageState;
import com.jagex.game.runetek5.graphics.texture.TextureState;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode;
import com.jagex.game.runetek5.toolkit.companion.TextureCombineMode;

public class NativeSpriteShaderLd extends AbstractSpriteShader {

    public NativeSpriteShaderLd(AbstractNativeToolkit nativeToolkit) {
        super(nativeToolkit);
    }

    @Override
    public void loadNormal() {
        load();
    }

    public void load() {
        super.activeToolkit.method14003();
        super.activeToolkit.loadWorldViewProjectionMatrix(super.worldViewProjectionMatrix);
        super.activeToolkit.setMatrixTransform(AbstractNativeToolkit.WORLD_VIEW_PROJECTION_MATRIX, AbstractNativeToolkit.WORLD_VIEW_PROJECTION_MATRIX, super.worldViewProjectionMatrix);
        super.activeToolkit.setTextureSlot(0);
        super.activeToolkit.setTextureSampler(super.anInterface6443);
        super.activeToolkit.method13967(0, TextureStageState.aClass67680);
        super.activeToolkit.method13969(0, TextureStageState.aClass67680);
        super.activeToolkit.method13967(1, TextureStageState.aClass67677);
        super.activeToolkit.method13969(1, TextureStageState.aClass67677);
        super.activeToolkit.method13973().copyFrom(super.aClass384440);
        super.activeToolkit.setTextureState(TextureState.PARTIAL_COPY_LAST);
        super.activeToolkit.setStreamSource(0, super.anInterface4445);
        super.activeToolkit.setVertexDeclaration(super.aClass70447);
        super.activeToolkit.drawPrimitive(PrimitiveDrawMode.TRIANGLE_FAN, super.anInt446, 2);
    }

    @Override
    public void setColours(int type, int value) {
        super.activeToolkit.setColourType(type);
        super.activeToolkit.setColourValue(value);
    }

    @Override
    public void loadMasked() {
        super.activeToolkit.setTextureSlot(1);
        super.activeToolkit.setTextureSampler(super.anInterface6441);
        super.activeToolkit.method13973().copyFrom(super.aClass384444);
        super.activeToolkit.setTextureState(TextureState.PARTIAL_COPY_LAST);
        super.activeToolkit.method13966(TextureCombineMode.REPLACE, TextureCombineMode.MODULATE);
        super.activeToolkit.method13967(0, TextureStageState.aClass67679);
        load();
        super.activeToolkit.setTextureSlot(1);
        super.activeToolkit.setTextureOnCurrentSlot();
    }
}
