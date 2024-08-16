package com.jagex.game.runetek5.graphics.shader;

import com.jagex.game.runetek5.graphics.shader.program.impl.NativeShaderMapping;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit;

import jaggl.OpenGL;

public class ShaderGL extends Shader {

    public final OpenGLNativeToolkit activeToolkit;
    public ShaderMappingGL activeShaderMapping;
    public int programHandle;

    public ShaderGL(OpenGLNativeToolkit toolkit, ShaderLoader shaderLoader) {
        super(toolkit, shaderLoader);
        this.activeToolkit = toolkit;
    }

    @Override
    public ShaderMapping loadMapping(AbstractNativeToolkit toolkit, NativeShaderMapping shaderMapping) {
        return new ShaderMappingGL((OpenGLNativeToolkit) toolkit, this, shaderMapping);
    }

    @Override
    public boolean loadProgram(ShaderMapping mapping) {
        if (mapping == this.activeShaderMapping) {
            return true;
        } 
        if (!mapping.createShaders()) {
            return false;
        } 
        boolean setProgramToManipulate = isActiveMapping();
        this.activeShaderMapping = (ShaderMappingGL) mapping;
        super.occupyingSlot = getMappingSlot(mapping);
        if (super.occupyingSlot == -1) {
            throw new IllegalArgumentException();
        } 
        this.programHandle = this.activeShaderMapping.programHandle;
        if (setProgramToManipulate) {
            OpenGL.glUseProgram(this.programHandle);
            this.activeToolkit.activeShaderMapping = this.activeShaderMapping;
        }
        return true;
    }

    @Override
    public ShaderAttribute decode(ShaderVariableDecoder shaderVariableDecoder) {
        return new ShaderAttributeGL(this, shaderVariableDecoder);
    }

    @Override
    public void reset() {
    }

    @Override
    public boolean isActiveMapping() {
        return this.activeToolkit.activeShaderMapping == this.activeShaderMapping;
    }

    @SuppressWarnings("removal")
	@Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    @Override
    public void dispose() {
        for (int slot = 0; slot < getMappingCount(); slot++) {
            getMapping(slot).deallocate();
        }
        super.dispose();
    }

    @Override
    public void setActiveMapping() {
        if (this.activeToolkit.activeShaderMapping != this.activeShaderMapping) {
            if (this.activeShaderMapping == null) {
                throw new ShaderHardwareException();
            }
            OpenGL.glUseProgram(this.programHandle);
            this.activeToolkit.activeShaderMapping = this.activeShaderMapping;
        }
    }
}
