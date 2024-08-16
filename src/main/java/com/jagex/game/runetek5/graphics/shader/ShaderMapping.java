package com.jagex.game.runetek5.graphics.shader;

import com.jagex.game.runetek5.graphics.texture.sampler.TextureSampler;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.companion.Binding;

public abstract class ShaderMapping implements Binding {

    public String vertexShaderName;
    public String fragmentShaderName;
    public String shaderName;

    public abstract void uniform4fv(ShaderAttribute var1, Matrix44 var2);

    public String getShaderName() {
        return this.shaderName;
    }

    public abstract void addPositionAttribute3f(int variableProgramAndIndex, float x, float y, float z);

    public abstract void uniform1f(ShaderAttribute var1, float var2);

    public abstract void addPositionAttribute4f(int variableProgramAndIndex, float x, float y, float z, float w);

    public abstract void loadTextureSampler(ShaderAttribute var1, int var2, TextureSampler var3);

    public abstract void addPreloadedPositionAttribute(ShaderAttribute attribute, float x, float y, float z, float w);

    public abstract boolean createShaders();

    public abstract void uniform3fSavedSize16(int slotAndCopyDest, Matrix44 matrix);

    public abstract void uniform4fv(ShaderAttribute var1, float[] var2, int var3);

    public abstract void addPositionAttributeDynamic(int var1, float[] var2, int var3);

    public abstract void uniform3fSavedSize12(int slotAndCopyDest, Matrix44 matrix);

    public abstract void uniform3fSavedSize8(int slotAndCopyDest, Matrix44 matrix);

    public abstract void initializeTextureSampler(int var1, int slot, TextureSampler var3);

    public abstract void unform2f(ShaderAttribute var1, float var2, float var3);

    public abstract void uniform2fv(ShaderAttribute var1, Matrix44 var2);

    public abstract void uniform3f(ShaderAttribute var1, float var2, float var3, float var4);

}
