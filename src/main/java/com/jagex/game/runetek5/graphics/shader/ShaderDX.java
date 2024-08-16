package com.jagex.game.runetek5.graphics.shader;

import com.jagex.game.runetek5.graphics.shader.program.impl.NativeShaderMapping;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit;

/**
 * 
 * @author Steven - 17/09/2023
 *
 */
public class ShaderDX extends Shader {

    public boolean active;
    public final DirectXNativeToolkit toolkit;
    public ShaderMappingDX shaderMapping;

    public ShaderDX(DirectXNativeToolkit toolkit, ShaderLoader shaderLoader) {
        super(toolkit, shaderLoader);
        this.toolkit = toolkit;
        this.active = false;
    }

    @Override
    public ShaderMapping loadMapping(AbstractNativeToolkit toolkit, NativeShaderMapping shaderMapping) {
        return new ShaderMappingDX((DirectXNativeToolkit) toolkit, this, shaderMapping);
    }

    @Override
    public boolean loadProgram(ShaderMapping shaderMapping) {
        if (shaderMapping == this.shaderMapping) {
            return true;
        } 
        if (!shaderMapping.createShaders()) {
            return false;
        } 
        this.shaderMapping = (ShaderMappingDX) shaderMapping;
        this.occupyingSlot = getMappingSlot(shaderMapping);
        if (this.occupyingSlot == -1) {
            throw new IllegalArgumentException();
        } 
        if (this.active) {
        	this.toolkit.setVertexShader(this.shaderMapping.vertexShaderHandle);
        	this.toolkit.setPixelShader(this.shaderMapping.fragmentShaderHandle);
        	this.toolkit.activeShaderMapping = this.shaderMapping;
        }

        return true;
    }

    @Override
    public boolean isActiveMapping() {
        return this.toolkit.activeShaderMapping == this.shaderMapping;
    }

    @Override
    public void reset() {
        this.toolkit.setVertexShader(0L);
        this.toolkit.setPixelShader(0L);
        this.active = false;
        this.toolkit.activeShaderMapping = null;
        this.toolkit.setTextureSlot(1);
        this.toolkit.setTextureSampler(null);
        this.toolkit.setTextureSlot(0);
        this.toolkit.setTextureSampler(null);
    }

    @Override
    public ShaderAttribute decode(ShaderVariableDecoder shaderVariableDecoder) {
        return new ShaderAttributeDX(this, shaderVariableDecoder);
    }

    @Override
    public void setActiveMapping() {
        if (this.shaderMapping == null) {
            throw new ShaderHardwareException();
        } 
        this.toolkit.setVertexShader(this.shaderMapping.vertexShaderHandle);
        this.toolkit.setPixelShader(this.shaderMapping.fragmentShaderHandle);
        this.toolkit.activeShaderMapping = this.shaderMapping;
        this.active = true;
    }
}
