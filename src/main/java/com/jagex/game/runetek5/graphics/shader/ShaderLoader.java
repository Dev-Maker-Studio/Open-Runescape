package com.jagex.game.runetek5.graphics.shader;

import com.jagex.game.runetek5.graphics.shader.program.impl.NativeShaderMapping;

public class ShaderLoader {

    String name;

    ShaderVariableDecoder[] primaryShaderVariables;

    ShaderVariableDecoder[] secondaryShaderVariables;

    NativeShaderMapping[] mappings;

    public ShaderLoader(byte[] data) throws ShaderException {
        load(data);
    }

    public void load(byte[] shaderData) throws ShaderException {
        ShaderBuffer decoder = new ShaderBuffer(shaderData);
        int headerLength = decoder.getEntryCount();
        if (headerLength != 4) {
            throw new ShaderException(this);
        } 
        this.name = decoder.readString();
        this.primaryShaderVariables = new ShaderVariableDecoder[decoder.getEntryCount()];
        this.secondaryShaderVariables = new ShaderVariableDecoder[decoder.getEntryCount()];
        this.mappings = new NativeShaderMapping[decoder.getEntryCount()];
        /** Those are ALWAYS used **/
        for (int index = 0; index < this.primaryShaderVariables.length; index++) {
            this.primaryShaderVariables[index] = new ShaderVariableDecoder();
            this.primaryShaderVariables[index].decode(decoder);
        }
        /** Those are NEVER unused **/
        for (int index = 0; index < this.secondaryShaderVariables.length; index++) {
            this.secondaryShaderVariables[index] = new ShaderVariableDecoder();
            this.secondaryShaderVariables[index].decode(decoder);
        }
        for (int index = 0; index < this.mappings.length; index++) {
            this.mappings[index] = new NativeShaderMapping();
            this.mappings[index].decode(decoder);
        }
    }
}
