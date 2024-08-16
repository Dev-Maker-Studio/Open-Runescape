package com.jagex.game.runetek5.graphics.shader.program.impl;

import com.jagex.game.runetek5.graphics.shader.Shader;
import com.jagex.game.runetek5.graphics.shader.ShaderAttribute;
import com.jagex.game.runetek5.graphics.shader.ShaderBuilderException;
import com.jagex.game.runetek5.graphics.shader.program.AbstractParticleShader;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode;

public class NativeParticleShaderHd extends AbstractParticleShader {

    public Shader particleShader;
    public ShaderAttribute wvpMatrixAttribute;
    public ShaderAttribute diffuseSamplerAttribute;
    public ShaderAttribute textureCoordinateMatrixAttribute;
    public ShaderAttribute diffuseColorAttribute;
    public final Matrix44 wvpMatrix = new Matrix44();

    public NativeParticleShaderHd(AbstractNativeToolkit toolkit) throws ShaderBuilderException {
        super(toolkit);
        loadAttributes();
    }

    public boolean loadAttributes() throws ShaderBuilderException {
        this.particleShader = super.activeToolkit.loadShader("Particle");
        this.wvpMatrixAttribute = this.particleShader.getAttribute("WVPMatrix");
        this.diffuseSamplerAttribute = this.particleShader.getAttribute("DiffuseSampler");
        this.textureCoordinateMatrixAttribute = this.particleShader.getAttribute("TexCoordMatrix");
        this.diffuseColorAttribute = this.particleShader.getAttribute("DiffuseColour");
        this.particleShader.loadProgram(this.particleShader.createSingleMapping());
        return true;
    }

    @Override
    public void method1788(Matrix44 matrix441) {
        this.wvpMatrix.copyFrom(matrix441);
        this.wvpMatrix.multiplyBy(super.activeToolkit.aClass3848729);
    }

    @Override
    public void method1787(int i_1) {
        loadProgram();
        super.activeToolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, 0, i_1 * 4, 0, i_1 * 2);
    }

    @Override
    public void method1790() {
        loadProgram();
        super.activeToolkit.method14150();
    }

    public void loadProgram() {
        this.particleShader.loadProgram(this.particleShader.createSingleMapping());
        this.particleShader.setActiveMapping();
        this.particleShader.loadTextureSampler(this.diffuseSamplerAttribute, 0, super.diffuseSampler);
        this.particleShader.loadMatrix(this.wvpMatrixAttribute, this.wvpMatrix);
        this.particleShader.loadTextureMatrix(this.textureCoordinateMatrixAttribute, super.textureCoordinateMatrix);
        this.particleShader.loadRgbaColorAttribute(this.diffuseColorAttribute, super.diffuseColor);
    }

}
