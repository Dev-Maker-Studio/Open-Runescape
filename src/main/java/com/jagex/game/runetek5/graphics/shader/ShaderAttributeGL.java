package com.jagex.game.runetek5.graphics.shader;

import jaggl.OpenGL;

public class ShaderAttributeGL extends ShaderAttribute {

	public int[] uniformLocations;
    public final ShaderGL shaderGl;

    public ShaderAttributeGL(ShaderGL shader, ShaderVariableDecoder shaderDecoder) {
        super(shaderDecoder);
        this.shaderGl = shader;
    }

    @Override
    public int getUniformLocation(int index) {
        return this.uniformLocations[index];
    }

    public int getActiveUniformLocation() {
        return this.uniformLocations[this.shaderGl.getOccupyingSlot()];
    }

    @Override
    public boolean loadUniformLocation(int activeInSlot) {
        if (this.uniformLocations == null) {
            this.uniformLocations = new int[this.shaderGl.getMappingCount()];
        }
        ShaderMappingGL shaderMapping = (ShaderMappingGL) this.shaderGl.getMapping(activeInSlot);
        this.uniformLocations[activeInSlot] = OpenGL.glGetUniformLocation(shaderMapping.programHandle, getPrimaryShaderAttributeName());
        return this.uniformLocations[activeInSlot] != -1;
    }
}
