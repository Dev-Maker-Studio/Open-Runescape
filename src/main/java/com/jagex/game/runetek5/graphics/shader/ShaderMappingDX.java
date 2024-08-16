package com.jagex.game.runetek5.graphics.shader;

import com.jagex.game.runetek5.graphics.shader.program.impl.NativeShaderMapping;
import com.jagex.game.runetek5.graphics.texture.sampler.TextureSampler;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit;
import com.jagex.game.runetek5.unidentified.Class111;
import com.jagex.game.runetek5.utils.ArrayUtils;

import jagdx.IDirect3DDevice;
import jagdx.IUnknown;

public class ShaderMappingDX extends ShaderMapping {

    public static final float[] MATRIX = new float[16];
    public boolean isBuild;
    public final float[][] programVariables;
    public final boolean[] variableLoadedIntoProgram;
    public byte[] vertexShaderData;
    public byte[] fragmentShaderData;
    public long vertexShaderHandle;
    public long fragmentShaderHandle;
    public DirectXNativeToolkit activeToolkit;
    public ShaderDX activeShader;

    public ShaderMappingDX(DirectXNativeToolkit nativeToolkit, NativeShaderMapping programBuilder) {
        this.isBuild = false;
        this.programVariables = new float[2][];
        this.variableLoadedIntoProgram = new boolean[2];
        super.shaderName = programBuilder.mappingName;
        if (programBuilder.vertexShaderName != null) {
            super.vertexShaderName = programBuilder.vertexShaderName;
            this.vertexShaderData = nativeToolkit.getShaderData(super.vertexShaderName);
        }

        if (programBuilder.fragmentShaderName != null) {
            super.fragmentShaderName = programBuilder.fragmentShaderName;
            this.fragmentShaderData = nativeToolkit.getShaderData(super.fragmentShaderName);
        }

        nativeToolkit.addWeakReference(this);
    }

    public ShaderMappingDX(DirectXNativeToolkit directxNativeToolkit, ShaderDX activeShader, NativeShaderMapping shaderProgramBuilder) {
        this(directxNativeToolkit, shaderProgramBuilder);
        this.activeToolkit = directxNativeToolkit;
        this.activeShader = activeShader;
    }

    @Override
    public void uniform1f(ShaderAttribute class282Sub21Sub11, float f_2) {
        int i_3 = ((ShaderAttributeDX) class282Sub21Sub11).method15782();
        int i_4 = ((ShaderAttributeDX) class282Sub21Sub11).method15783();
        if (class282Sub21Sub11.method15457() != Class111.aClass1111219 && class282Sub21Sub11.method15457() != Class111.aClass1111182) {
            throw new IllegalShaderArgumentException();
        } else {
            if (i_3 >= 0) {
                i_3 *= 4;
                this.programVariables[0][i_3] = f_2;
                this.variableLoadedIntoProgram[0] = true;
            }

            if (i_4 >= 0) {
                i_4 *= 4;
                this.programVariables[1][i_4] = f_2;
                this.variableLoadedIntoProgram[1] = true;
            }

        }
    }

    @Override
    public void unform2f(ShaderAttribute class282Sub21Sub11, float f_2, float f_3) {
        int i_4 = ((ShaderAttributeDX) class282Sub21Sub11).method15782();
        int i_5 = ((ShaderAttributeDX) class282Sub21Sub11).method15783();
        if (class282Sub21Sub11.method15457() != Class111.aClass1111132) {
            throw new IllegalShaderArgumentException();
        } 
        if (i_4 >= 0) {
            i_4 *= 4;
            this.programVariables[0][i_4] = f_2;
            this.programVariables[0][i_4 + 1] = f_3;
            this.variableLoadedIntoProgram[0] = true;
        }

        if (i_5 >= 0) {
            i_5 *= 4;
            this.programVariables[1][i_5] = f_2;
            this.programVariables[1][i_5 + 1] = f_3;
            this.variableLoadedIntoProgram[1] = true;
        }
    }

    @Override
    public void initializeTextureSampler(int i_1, int i_2, TextureSampler interface303) {
        this.activeToolkit.setTextureSlot(i_2);
        this.activeToolkit.setTextureSampler(interface303);
    }

    @Override
    public void uniform2fv(ShaderAttribute class282Sub21Sub11, Matrix44 matrix442) {
        int i_3 = ((ShaderAttributeDX) class282Sub21Sub11).method15782();
        int i_4 = ((ShaderAttributeDX) class282Sub21Sub11).method15783();
        if (class282Sub21Sub11.method15457() != Class111.aClass1111168) {
            throw new IllegalShaderArgumentException();
        } else {
            if (i_3 >= 0) {
                method14589(i_3, matrix442.copyXYInto(MATRIX), 8);
            }

            if (i_4 >= 0) {
                method14590(i_4, matrix442.copyXYInto(MATRIX), 8);
            }

        }
    }

    @Override
    public void uniform4fv(ShaderAttribute shaderAttribute, Matrix44 matrix) {
        int i_3 = ((ShaderAttributeDX) shaderAttribute).method15782();
        int i_4 = ((ShaderAttributeDX) shaderAttribute).method15783();
        if (shaderAttribute.method15457() != Class111.aClass1111150) {
            throw new IllegalShaderArgumentException();
        } 
        if (i_3 >= 0) {
            method14589(i_3, matrix.copyFullyInto(MATRIX), 16);
        }

        if (i_4 >= 0) {
            method14590(i_4, matrix.copyFullyInto(MATRIX), 16);
        }
    }

    @Override
    public void addPositionAttribute3f(int i_1, float f_2, float f_3, float f_4) {
        int i_5 = i_1 >> 16;
        int i_6 = (i_1 & 0xffff) * 4;
        this.programVariables[i_5][i_6] = f_2;
        this.programVariables[i_5][i_6 + 1] = f_3;
        this.programVariables[i_5][i_6 + 2] = f_4;
        this.variableLoadedIntoProgram[i_5] = true;
    }

    @Override
    public void addPositionAttribute4f(int i_1, float f_2, float f_3, float f_4, float f_5) {
        int i_6 = i_1 >> 16;
        int i_7 = (i_1 & 0xffff) * 4;
        this.programVariables[i_6][i_7] = f_2;
        this.programVariables[i_6][i_7 + 1] = f_3;
        this.programVariables[i_6][i_7 + 2] = f_4;
        this.programVariables[i_6][i_7 + 3] = f_5;
        this.variableLoadedIntoProgram[i_6] = true;
    }

    @Override
    public void uniform3fSavedSize12(int i_1, Matrix44 matrix442) {
        int i_3 = i_1 >> 16;
        int i_4 = (i_1 & 0xffff) * 4;
        System.arraycopy(matrix442.copyFullyInto(MATRIX), 0, this.programVariables[i_3], i_4, 12);
        this.variableLoadedIntoProgram[i_3] = true;
    }

    @Override
    public void uniform3fSavedSize8(int i_1, Matrix44 matrix442) {
        int i_3 = i_1 >> 16;
        int i_4 = (i_1 & 0xffff) * 4;
        System.arraycopy(matrix442.copyXYInto(MATRIX), 0, this.programVariables[i_3], i_4, 8);
        this.variableLoadedIntoProgram[i_3] = true;
    }

    @Override
    public void uniform3fSavedSize16(int i_1, Matrix44 matrix442) {
        int i_3 = i_1 >> 16;
        int i_4 = (i_1 & 0xffff) * 4;
        System.arraycopy(matrix442.copyFullyInto(MATRIX), 0, this.programVariables[i_3], i_4, 16);
        this.variableLoadedIntoProgram[i_3] = true;
    }

    @Override
    public void addPositionAttributeDynamic(int i_1, float[] floats_2, int i_3) {
        int i_4 = i_1 >> 16;
        int i_5 = (i_1 & 0xffff) * 4;
        System.arraycopy(floats_2, 0, this.programVariables[i_4], i_5, i_3);
        this.variableLoadedIntoProgram[i_4] = true;
    }

    public void method14589(int i_1, float[] floats_2, int i_3) {
        ArrayUtils.arrayCopy(floats_2, 0, this.programVariables[0], i_1 * 4, i_3);
        this.variableLoadedIntoProgram[0] = true;
    }

    public void method14590(int i_1, float[] floats_2, int i_3) {
        ArrayUtils.arrayCopy(floats_2, 0, this.programVariables[1], i_1 * 4, i_3);
        this.variableLoadedIntoProgram[1] = true;
    }

    //non multiplier original
    /*public void method14591_2() {
        int i_1;
        if (aBoolArray9285[0]) {
            for (i_1 = 0; i_1 < aFloatArrayArray9283[0].length; i_1++) {
                aGraphicalRenderer_Sub2_Sub2_9293.the_unsafe.putFloat(aGraphicalRenderer_Sub2_Sub2_9293.direct_buffer_address + (i_1 * 4), aFloatArrayArray9283[0][i_1]);
            }
            IDirect3DDevice.SetVertexShaderConstantF(aGraphicalRenderer_Sub2_Sub2_9293.render_target, 0, aGraphicalRenderer_Sub2_Sub2_9293.direct_buffer_address, aFloatArrayArray9283[0].length / 4);
            aBoolArray9285[0] = false;
        }

        if (aBoolArray9285[1]) {
            for (i_1 = 0; i_1 < aFloatArrayArray9283[1].length; i_1++) {
                aGraphicalRenderer_Sub2_Sub2_9293.the_unsafe.putFloat(aGraphicalRenderer_Sub2_Sub2_9293.direct_buffer_address + (i_1 * 4), aFloatArrayArray9283[1][i_1]);
            }
            IDirect3DDevice.SetPixelShaderConstantF(aGraphicalRenderer_Sub2_Sub2_9293.render_target, 0, aGraphicalRenderer_Sub2_Sub2_9293.direct_buffer_address, aFloatArrayArray9283[1].length / 4);
            aBoolArray9285[1] = false;
        }
    }*/
    
	public void method14591() {
		if (this.variableLoadedIntoProgram[0]) {
			for (int i = 0; i < this.programVariables[0].length; i++)
				this.activeToolkit.theUnsafe.putFloat(this.activeToolkit.directBufferAddress + (long) (i * 4L), this.programVariables[0][i]);
			IDirect3DDevice.SetVertexShaderConstantF(this.activeToolkit.renderTarget, 0, this.activeToolkit.directBufferAddress, this.programVariables[0].length / 4);
			this.variableLoadedIntoProgram[0] = false;
		}
		if (this.variableLoadedIntoProgram[1]) {
			for (int i = 0; i < this.programVariables[1].length; i++)
				this.activeToolkit.theUnsafe.putFloat(this.activeToolkit.directBufferAddress + (long) (i * 4L), this.programVariables[1][i]);
			IDirect3DDevice.SetPixelShaderConstantF(this.activeToolkit.renderTarget, 0, this.activeToolkit.directBufferAddress, this.programVariables[1].length / 4);
			this.variableLoadedIntoProgram[1] = false;
		}
	}

    @Override
    public void deallocate() {
        if (this.vertexShaderHandle != 0L) {
            IUnknown.Release(this.vertexShaderHandle);
            this.vertexShaderHandle = 0L;
        }

        if (this.fragmentShaderHandle != 0L) {
            IUnknown.Release(this.fragmentShaderHandle);
            this.fragmentShaderHandle = 0L;
        }

        this.activeToolkit.clearReference(this);
    }

    public void method14592() {
        if (this.vertexShaderHandle != 0L) {
            this.activeToolkit.requestHandleCleanup(this.vertexShaderHandle);
            this.vertexShaderHandle = 0L;
        }

        if (this.fragmentShaderHandle != 0L) {
            this.activeToolkit.requestHandleCleanup(this.fragmentShaderHandle);
            this.fragmentShaderHandle = 0L;
        }

    }

    @Override
    protected void finalize() {
        method14592();
    }

    @Override
    public void uniform4fv(ShaderAttribute class282Sub21Sub11, float[] floats_2, int i_3) {
        int i_4 = ((ShaderAttributeDX) class282Sub21Sub11).method15782();
        int i_5 = ((ShaderAttributeDX) class282Sub21Sub11).method15783();
        if (class282Sub21Sub11.method15457() != Class111.aClass1111109) {
            throw new IllegalShaderArgumentException();
        } else {
            if (i_4 >= 0) {
                method14589(i_4, floats_2, i_3);
            }

            if (i_5 >= 0) {
                method14590(i_5, floats_2, i_3);
            }

        }
    }


    @Override
    public void addPreloadedPositionAttribute(ShaderAttribute class282Sub21Sub11, float f_2, float f_3, float f_4, float f_5) {
        int i_6 = ((ShaderAttributeDX) class282Sub21Sub11).method15782();
        int i_7 = ((ShaderAttributeDX) class282Sub21Sub11).method15783();
        if (class282Sub21Sub11.method15457() != Class111.aClass1111134) {
            throw new IllegalShaderArgumentException();
        } 
        if (i_6 >= 0) {
            i_6 *= 4;
            this.programVariables[0][i_6] = f_2;
            this.programVariables[0][i_6 + 1] = f_3;
            this.programVariables[0][i_6 + 2] = f_4;
            this.programVariables[0][i_6 + 3] = f_5;
            this.variableLoadedIntoProgram[0] = true;
        }

        if (i_7 >= 0) {
            i_7 *= 4;
            this.programVariables[1][i_7] = f_2;
            this.programVariables[1][i_7 + 1] = f_3;
            this.programVariables[1][i_7 + 2] = f_4;
            this.programVariables[1][i_7 + 3] = f_5;
            this.variableLoadedIntoProgram[1] = true;
        }
    }

    @Override
    public void uniform3f(ShaderAttribute class282Sub21Sub11, float f_2, float f_3, float f_4) {
        int i_5 = ((ShaderAttributeDX) class282Sub21Sub11).method15782();
        int i_6 = ((ShaderAttributeDX) class282Sub21Sub11).method15783();
        if (class282Sub21Sub11.method15457() != Class111.aClass1111133) {
            throw new IllegalShaderArgumentException();
        } 
        if (i_5 >= 0) {
            i_5 *= 4;
            this.programVariables[0][i_5] = f_2;
            this.programVariables[0][i_5 + 1] = f_3;
            this.programVariables[0][i_5 + 2] = f_4;
            this.variableLoadedIntoProgram[0] = true;
        }

        if (i_6 >= 0) {
            i_6 *= 4;
            this.programVariables[1][i_6] = f_2;
            this.programVariables[1][i_6 + 1] = f_3;
            this.programVariables[1][i_6 + 2] = f_4;
            this.variableLoadedIntoProgram[1] = true;
        }
    }

    @Override
    public boolean createShaders() {
        if (this.isBuild) {
            return true;
        } 
        this.vertexShaderHandle = this.vertexShaderData == null ? 0L : IDirect3DDevice.CreateVertexShader(this.activeToolkit.renderTarget, this.vertexShaderData);
        this.fragmentShaderHandle = this.fragmentShaderData == null ? 0L : IDirect3DDevice.CreatePixelShader(this.activeToolkit.renderTarget, this.fragmentShaderData);
        if (this.vertexShaderHandle == 0L && this.fragmentShaderHandle == 0L) {
            return false;
        } 
        int slotInUse = -1;
        for (int slot = 0; slot < this.activeShader.getMappingCount(); slot++) {
            if (this.activeShader.getMapping(slot) == this) {
                slotInUse = slot;
                break;
            }
        }

        int i_2 = this.activeShader.getPrimaryShaderVariableCount();
        int i_3 = this.activeShader.getSecondaryShaderVariableCount();

        ShaderAttribute class282Sub21Sub112;
        for (int i_4 = 0; i_4 < i_2; i_4++) {
            class282Sub21Sub112 = this.activeShader.getShaderAttribute(i_4);
            class282Sub21Sub112.loadUniformLocation(slotInUse);
        }

        for (int i_4 = 0; i_4 < i_3; i_4++) {
            class282Sub21Sub112 = this.activeShader.getSecondaryAttribute(i_4);
            class282Sub21Sub112.loadUniformLocation(slotInUse);
        }

        int vertexShaderVariableCount = 0;
        int fragmentShaderVariableCount = 0;
        int i_6 = this.activeShader.getMappingSlot(this);

        for (int i_7 = 0; i_7 < i_2 + i_3; i_7++) {
            ShaderAttributeDX class282Sub21Sub1Sub18 = (ShaderAttributeDX) (i_7 < i_2 ? this.activeShader.getShaderAttribute(i_7) : this.activeShader.getSecondaryAttribute(i_7 - i_2));
            Class111 class1119 = class282Sub21Sub1Sub18.method15457();
            int i_10 = 1;
            if (class1119 == Class111.aClass1111109) {
                class1119 = class282Sub21Sub1Sub18.method15458();
                i_10 = class282Sub21Sub1Sub18.method15459();
            }

            switch (class1119.anInt1232) {
                case 5:
                case 26:
                case 49:
                case 64:
                    i_10 *= 3;
                case 8:
                case 10:
                case 54:
                case 95:
                    break;
                case 16:
                case 17:
                case 56:
                case 75:
                    i_10 *= 4;
                    break;
                case 34:
                case 41:
                case 80:
                case 94:
                    i_10 *= 2;
                    break;
                default:
                    i_10 = 0;
            }

            int i_11;
            if (class282Sub21Sub1Sub18.anIntArray10306[i_6] >= 0) {
                i_11 = i_10 + class282Sub21Sub1Sub18.anIntArray10306[i_6];
                if (vertexShaderVariableCount < i_11) {
                    vertexShaderVariableCount = i_11;
                }
            }

            if (class282Sub21Sub1Sub18.anIntArray10307[i_6] >= 0) {
                i_11 = i_10 + class282Sub21Sub1Sub18.anIntArray10307[i_6];
                if (fragmentShaderVariableCount < i_11) {
                    fragmentShaderVariableCount = i_11;
                }
            }
        }

        this.programVariables[0] = new float[vertexShaderVariableCount * 4];
        this.programVariables[1] = new float[fragmentShaderVariableCount * 4];
        this.isBuild = true;
        return true;
    }

    @Override
    public void loadTextureSampler(ShaderAttribute shaderAttribute, int slot, TextureSampler sampler) {
        this.activeToolkit.setTextureSlot(slot);
        this.activeToolkit.setTextureSampler(sampler);
    }

}
