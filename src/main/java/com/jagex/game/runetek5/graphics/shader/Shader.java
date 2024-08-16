package com.jagex.game.runetek5.graphics.shader;

import com.jagex.game.runetek5.graphics.shader.program.impl.NativeShaderMapping;
import com.jagex.game.runetek5.graphics.texture.sampler.TextureSampler;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.math.Vector4;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;

public abstract class Shader {

    public static final IProgramAttributeHandler anInterface111012 = new ProgramAttributeHandler();

    protected int occupyingSlot = -1;

    public final String shaderName;

    public final int primaryShaderVariableCount;

    public final ProgramBuilder primaryShaderBuilder;

    public final int secondaryShaderVariableCount;

    public final ProgramBuilder secondaryShaderBuilder;

    public final ShaderMapping[] shaderMapping;

    protected Shader(AbstractNativeToolkit toolkit, ShaderLoader shaderloader) {
        this.shaderName = shaderloader.name;
        this.primaryShaderVariableCount = shaderloader.primaryShaderVariables.length;
        this.primaryShaderBuilder = new ProgramBuilder(this.primaryShaderVariableCount, anInterface111012);
        for (int index = 0; index < this.primaryShaderVariableCount; index++) {
            this.primaryShaderBuilder.addAttribute(index, shaderloader.primaryShaderVariables[index].primaryShaderAttributeName, decode(shaderloader.primaryShaderVariables[index]));
        }
        this.secondaryShaderVariableCount = shaderloader.secondaryShaderVariables.length;
        this.secondaryShaderBuilder = new ProgramBuilder(this.secondaryShaderVariableCount, anInterface111012);
        for (int index = 0; index < this.secondaryShaderVariableCount; index++) {
            this.secondaryShaderBuilder.addAttribute(index, shaderloader.secondaryShaderVariables[index].primaryShaderAttributeName, decode(shaderloader.secondaryShaderVariables[index]));
        }
        this.shaderMapping = new ShaderMapping[shaderloader.mappings.length];
        for (int index = 0; index < shaderloader.mappings.length; index++) {
            this.shaderMapping[index] = loadMapping(toolkit, shaderloader.mappings[index]);
        }
    }

    public abstract void setActiveMapping();

    public abstract void reset();

    public abstract boolean isActiveMapping();

    public int getMappingCount() {
        return this.shaderMapping.length;
    }

    public ShaderMapping getShaderMapping(String requestedMapping) throws MappedShaderCreationException {
        ShaderMapping[] mapped = this.shaderMapping;
        for (int index = 0; index < mapped.length; index++) {
            ShaderMapping mapping = mapped[index];
            String shaderName = mapping.getShaderName();
            if (shaderName != null && shaderName.equals(requestedMapping)) {
                if (!mapping.createShaders()) {
                    throw new MappedShaderCreationException(requestedMapping);
                }
                return mapping;
            }
        }
        throw new MappedShaderCreationException(requestedMapping);
    }

    public ShaderMapping getMapping(int slot) {
        return this.shaderMapping[slot];
    }

    public int getMappingSlot(ShaderMapping mapped) {
        for (int slot = 0; slot < this.shaderMapping.length; slot++) {
            if (this.shaderMapping[slot] == mapped) {
                return slot;
            }
        }
        return -1;
    }

    public ShaderMapping createSingleMapping() {
        ShaderMapping[] mapped = this.shaderMapping;
        for (int slot = 0; slot < mapped.length; slot++) {
            ShaderMapping mapping = mapped[slot];
            if (mapping.createShaders()) {
                return mapping;
            }
        }
        return null;
    }

    public abstract boolean loadProgram(ShaderMapping mapping);

    public ShaderMapping getActiveMapping() {
        return this.occupyingSlot >= 0 ? this.shaderMapping[this.occupyingSlot] : null;
    }

    public void unform2f(ShaderAttribute attribute, float f1, float f2) {
        this.shaderMapping[this.occupyingSlot].unform2f(attribute, f1, f2);
    }

    public void uniform1f(ShaderAttribute attribute, float variable) {
        this.shaderMapping[this.occupyingSlot].uniform1f(attribute, variable);
    }

    public void uniform3f(ShaderAttribute attribute, float variableA, float variableB, float variableC) {
        this.shaderMapping[this.occupyingSlot].uniform3f(attribute, variableA, variableB, variableC);
    }

    //active_shader.load_rgba_color_attribute(eye_pos_and_time, aClass385_10089.x, aClass385_10089.y, aClass385_10089.z, (float) (i_2 * toolkit.anInt8820 % 40000) / 40000.0F);
    public void loadRgbaColorAttribute(ShaderAttribute shaderAttribute, float x, float y, float z, float time) {
        this.shaderMapping[this.occupyingSlot].addPreloadedPositionAttribute(shaderAttribute, x, y, z, time);
    }

    public void uniform3f(ShaderAttribute attribute, Vector3 vector) {
        this.shaderMapping[this.occupyingSlot].uniform3f(attribute, vector.x, vector.y, vector.z);
    }

    public void loadTextureMatrix(ShaderAttribute class282Sub21Sub11, Matrix44 matrix442) {
        this.shaderMapping[this.occupyingSlot].uniform2fv(class282Sub21Sub11, matrix442);
    }

    public void loadMatrix(ShaderAttribute class282Sub21Sub11, Matrix44 matrix442) {
        this.shaderMapping[this.occupyingSlot].uniform4fv(class282Sub21Sub11, matrix442);
    }

    public void load3f(int i_1, float f_2, float f_3, float f_4) {
        this.shaderMapping[this.occupyingSlot].addPositionAttribute3f(i_1, f_2, f_3, f_4);
    }

    public void loadTextureSampler(ShaderAttribute attribute, int i_2, TextureSampler sampler) {
        this.shaderMapping[this.occupyingSlot].loadTextureSampler(attribute, i_2, sampler);
    }

    public void loadVector3f(int i_1, Vector3 vector32) {
        this.shaderMapping[this.occupyingSlot].addPositionAttribute3f(i_1, vector32.x, vector32.y, vector32.z);
    }

    public void method1673(int i_1, float[] floats_2, int i_3) {
        this.shaderMapping[this.occupyingSlot].addPositionAttributeDynamic(i_1, floats_2, i_3);
    }

    public void method1674(int i_1, Matrix44 matrix442) {
        this.shaderMapping[this.occupyingSlot].uniform3fSavedSize12(i_1, matrix442);
    }

    public void setTextureCoordinateMatrix(int i_1, Matrix44 matrix442) {
        this.shaderMapping[this.occupyingSlot].uniform3fSavedSize8(i_1, matrix442);
    }

    public void setWorldViewProjectionMatrix(int i_1, Matrix44 matrix442) {
        this.shaderMapping[this.occupyingSlot].uniform3fSavedSize16(i_1, matrix442);
    }

    public void setDiffuseSampler(int i_1, int slot, TextureSampler interface303) {
        this.shaderMapping[this.occupyingSlot].initializeTextureSampler(i_1, slot, interface303);
    }

    public int getSecondaryShaderVariableCount() {
        return this.secondaryShaderVariableCount;
    }

    public ShaderAttribute loadAttribute(String attributeName) {
        return (ShaderAttribute) this.secondaryShaderBuilder.loadAttribute(attributeName);
    }

    public int getPrimaryShaderVariableCount() {
        return this.primaryShaderVariableCount;
    }

    public void dispose() {
    }

    public void method1688(ShaderAttribute class282Sub21Sub11, float[] floats_2) {
        this.shaderMapping[this.occupyingSlot].uniform4fv(class282Sub21Sub11, floats_2, floats_2.length);
    }

    public void insertAttribute(ShaderAttribute attribute, Vector4 vector4) {
        this.shaderMapping[this.occupyingSlot].addPreloadedPositionAttribute(attribute, vector4.x, vector4.y, vector4.z, vector4.w);
    }

    public ShaderAttribute getAttribute(String attributeName) throws ShaderAttributeLoadingException {
        ShaderAttribute shaderAttribute = (ShaderAttribute) this.primaryShaderBuilder.loadAttribute(attributeName);
        if (shaderAttribute == null) {
            throw new ShaderAttributeLoadingException(attributeName);
        } 
        return shaderAttribute;
    }

    public void loadRgbaColorAttribute(ShaderAttribute shaderAttribute, int argb) {
        float r = (argb >> 16 & 0xff) / 255.0F;
        float g = (argb >> 8 & 0xff) / 255.0F;
        float b = (argb & 0xff) / 255.0F;
        float a = (argb >> 24 & 0xff) / 255.0F;
        loadRgbaColorAttribute(shaderAttribute, r, g, b, a);
    }

    public ShaderAttribute getSecondaryAttribute(int index) {
        return (ShaderAttribute) this.secondaryShaderBuilder.getAttribute(index);
    }

    public int getOccupyingSlot() {
        return this.occupyingSlot;
    }

    public String getShaderName() {
        return this.shaderName;
    }

    public ShaderAttribute getShaderAttribute(int index) {
        return (ShaderAttribute) this.primaryShaderBuilder.getAttribute(index);
    }

    public abstract ShaderMapping loadMapping(AbstractNativeToolkit toolkit, NativeShaderMapping mapping);

    public void load4f(int i_1, float f_2, float f_3, float f_4, float f_5) {
        this.shaderMapping[this.occupyingSlot].addPositionAttribute4f(i_1, f_2, f_3, f_4, f_5);
    }

    public abstract ShaderAttribute decode(ShaderVariableDecoder decoder);
}
