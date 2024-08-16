package com.jagex.game.runetek5.graphics.shader;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.runetek5.graphics.shader.program.impl.NativeShaderMapping;
import com.jagex.game.runetek5.graphics.texture.sampler.MipmapTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.TextureSampler;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit;
import com.jagex.game.runetek5.unidentified.Class111;
import com.jagex.game.runetek5.utils.ArrayUtils;

import jaggl.OpenGL;

public class ShaderMappingGL extends ShaderMapping {

    public static final boolean $assertionsDisabled = !ShaderMappingGL.class.desiredAssertionStatus();
    public static final int[] STATUS_BUFFER = new int[2];
    public static final float[] aFloatArray9427 = new float[16];
    public int programHandle;
    public int vertexShaderHandle;
    public int fragmentShaderHandle;
    public boolean aBool9425;
    public final int[] anIntArray9426;
    public String vertexShaderProgramString;
    public String fragmentShaderProgramString;
    public float[] aFloatArray9429;
    public float[] aFloatArray9422;
    public float[] aFloatArray9431;
    public float[] aFloatArray9432;
    public int[] textureSlots;
    public ShaderGL openglShader;
    public OpenGLNativeToolkit toolkit;

    public ShaderMappingGL(OpenGLNativeToolkit toolkit, NativeShaderMapping shaderMapping) {
        this.programHandle = 0;
        this.vertexShaderHandle = 0;
        this.fragmentShaderHandle = 0;
        this.aBool9425 = false;
        this.anIntArray9426 = new int[13];
        super.shaderName = shaderMapping.mappingName;
        StringBuilder programString = new StringBuilder();
        if (shaderMapping.vertexShaderName != null) {
            super.vertexShaderName = shaderMapping.vertexShaderName;
            if (shaderMapping.vertexShaderVariables != null) {
            	ShaderVariable[] vertexVariables = shaderMapping.vertexShaderVariables;
                for (int variableIndex = 0; variableIndex < vertexVariables.length; variableIndex++) {
                	ShaderVariable shaderVariable = vertexVariables[variableIndex];
                    programString.append("#define ").append(shaderVariable.programVariableName).append(" ").append(shaderVariable.programVariableValue).append('\n');
                }
            }
            programString.append(new String(toolkit.getGLShaderData(super.vertexShaderName)));
            this.vertexShaderProgramString = programString.toString();
        }
        if (shaderMapping.fragmentShaderName != null) {
            super.fragmentShaderName = shaderMapping.fragmentShaderName;
            programString.setLength(0);
            if (shaderMapping.fragmentShaderVariables != null) {
            	ShaderVariable[] fragmentVariables = shaderMapping.fragmentShaderVariables;
                for (int variableIndex = 0; variableIndex < fragmentVariables.length; variableIndex++) {
                	ShaderVariable shaderVariable = fragmentVariables[variableIndex];
                    programString.append("#define ").append(shaderVariable.programVariableName).append(" ").append(shaderVariable.programVariableValue).append('\n');
                }
            }
            programString.append(new String(toolkit.getGLShaderData(super.fragmentShaderName)));
            this.fragmentShaderProgramString = programString.toString();
        }
        ArrayUtils.setVariables(this.anIntArray9426, 0, this.anIntArray9426.length, -1);
    }

    public ShaderMappingGL(OpenGLNativeToolkit toolkit, ShaderGL openglShader, NativeShaderMapping hardwareShaderMapping) {
        this(toolkit, hardwareShaderMapping);
        this.openglShader = openglShader;
        this.toolkit = toolkit;
    }

    public int method14671(int i_1, String string_2) {
        if (string_2 == null) {
            return 0;
        } else {
            int i_4 = OpenGL.glCreateShader(i_1);
            OpenGL.glShaderSource(i_4, string_2);
            OpenGL.glCompileShader(i_4);
            OpenGL.glGetShaderiv(i_4, GL_COMPILE_STATUS, STATUS_BUFFER, 0);
            if (STATUS_BUFFER[0] == 0) {
                OpenGL.glDeleteShader(i_4);
                i_4 = 0;
            }

            return i_4;
        }
    }

    @Override
    public void uniform1f(ShaderAttribute class282Sub21Sub11, float f_2) {
        int i_3 = ((ShaderAttributeGL) class282Sub21Sub11).getActiveUniformLocation();
        if (i_3 != -1 && this.aFloatArray9429[i_3] != f_2) {
            this.aFloatArray9429[i_3] = f_2;
            if (class282Sub21Sub11.method15457() != Class111.aClass1111219 && class282Sub21Sub11.method15457() != Class111.aClass1111182) {
                throw new IllegalShaderArgumentException();
            }

            OpenGL.glUniform1f(i_3, f_2);
        }

    }

    @Override
    public void unform2f(ShaderAttribute class282Sub21Sub11, float f_2, float f_3) {
        int i_4 = ((ShaderAttributeGL) class282Sub21Sub11).getActiveUniformLocation();
        if (i_4 != -1 && (this.aFloatArray9429[i_4] != f_2 || this.aFloatArray9422[i_4] != f_3)) {
            this.aFloatArray9429[i_4] = f_2;
            this.aFloatArray9422[i_4] = f_3;
            if (class282Sub21Sub11.method15457() != Class111.aClass1111132) {
                throw new IllegalShaderArgumentException();
            }

            OpenGL.glUniform2f(i_4, f_2, f_3);
        }

    }

    @Override
    public void uniform3f(ShaderAttribute class282Sub21Sub11, float f_2, float f_3, float f_4) {
        int i_5 = ((ShaderAttributeGL) class282Sub21Sub11).getActiveUniformLocation();
        if (i_5 != -1 && (this.aFloatArray9429[i_5] != f_2 || this.aFloatArray9422[i_5] != f_3 || this.aFloatArray9431[i_5] != f_4)) {
            this.aFloatArray9429[i_5] = f_2;
            this.aFloatArray9422[i_5] = f_3;
            this.aFloatArray9431[i_5] = f_4;
            if (class282Sub21Sub11.method15457() != Class111.aClass1111133) {
                throw new IllegalShaderArgumentException();
            }

            OpenGL.glUniform3f(i_5, f_2, f_3, f_4);
        }

    }

    @Override
    public void uniform4fv(ShaderAttribute class282Sub21Sub11, float[] floats_2, int i_3) {
        int i_4 = ((ShaderAttributeGL) class282Sub21Sub11).getActiveUniformLocation();
        if (i_4 != -1) {
            OpenGL.glUniform4fv(i_4, i_3, floats_2, 0);
        }

    }

    @Override
    public void uniform2fv(ShaderAttribute class282Sub21Sub11, Matrix44 matrix442) {
        if (!$assertionsDisabled && class282Sub21Sub11.method15457() != Class111.aClass1111168) {
            throw new AssertionError();
        } else {
            int i_3 = ((ShaderAttributeGL) class282Sub21Sub11).getActiveUniformLocation();
            if (i_3 != -1) {
                OpenGL.glUniform2fv(i_3, 4, matrix442.getComponenetsXY(aFloatArray9427), 0);
            }

        }
    }

    @Override
    public void uniform4fv(ShaderAttribute class282Sub21Sub11, Matrix44 matrix442) {
        if (!$assertionsDisabled && class282Sub21Sub11.method15457() != Class111.aClass1111150) {
            throw new AssertionError();
        } else {
            int i_3 = ((ShaderAttributeGL) class282Sub21Sub11).getActiveUniformLocation();
            if (i_3 != -1) {
                OpenGL.glUniform4fv(i_3, 4, matrix442.copyInto(aFloatArray9427), 0);
            }

        }
    }

    @Override
    public void loadTextureSampler(ShaderAttribute attribute, int textureSlot, TextureSampler textureSampler) {
        int index = ((ShaderAttributeGL) attribute).getActiveUniformLocation();
        if (index != -1) {
            if (textureSampler == null) {
                textureSampler = this.toolkit.activeTextureSampler;
            }

            if (textureSlot < this.toolkit.maxConcurrentTextures) {
                this.toolkit.setTextureSlot(textureSlot);
                this.toolkit.setTextureSampler(textureSampler);
            } else {
                OpenGL.glActiveTexture(textureSlot + 33984);
                OpenGL.glBindTexture(((MipmapTextureSampler) textureSampler).textureTarget, ((MipmapTextureSampler) textureSampler).texture);
            }

            if (textureSlot != this.textureSlots[index]) {
                this.textureSlots[index] = textureSlot;
                OpenGL.glUniform1i(index, textureSlot);
            }
        }

    }

    @Override
    public void addPositionAttribute3f(int i_1, float f_2, float f_3, float f_4) {
        OpenGL.glUniform3f(i_1, f_2, f_3, f_4);
    }

    @Override
    public void addPositionAttribute4f(int i_1, float f_2, float f_3, float f_4, float f_5) {
        OpenGL.glUniform4f(i_1, f_2, f_3, f_4, f_5);
    }

    @Override
    public void addPreloadedPositionAttribute(ShaderAttribute class282Sub21Sub11, float f_2, float f_3, float f_4, float f_5) {
        int i_6 = ((ShaderAttributeGL) class282Sub21Sub11).getActiveUniformLocation();
        if (i_6 != -1 && (this.aFloatArray9429[i_6] != f_2 || this.aFloatArray9422[i_6] != f_3 || this.aFloatArray9431[i_6] != f_4 || this.aFloatArray9432[i_6] != f_5)) {
            this.aFloatArray9429[i_6] = f_2;
            this.aFloatArray9422[i_6] = f_3;
            this.aFloatArray9431[i_6] = f_4;
            this.aFloatArray9432[i_6] = f_5;
            if (class282Sub21Sub11.method15457() != Class111.aClass1111134) {
                throw new IllegalShaderArgumentException();
            }

            OpenGL.glUniform4f(i_6, f_2, f_3, f_4, f_5);
        }

    }

    @Override
    public void uniform3fSavedSize12(int i_1, Matrix44 matrix442) {
        OpenGL.glUniform3fv(i_1, 3, matrix442.copy3x3NearMatrixInto(aFloatArray9427), 0);
    }

    @Override
    public void uniform3fSavedSize8(int i_1, Matrix44 matrix442) {
        OpenGL.glUniform2fv(i_1, 4, matrix442.getComponenetsXY(aFloatArray9427), 0);
    }

    @Override
    public void uniform3fSavedSize16(int location, Matrix44 matrix4x4) {
        OpenGL.glUniform4fv(location, 4, matrix4x4.copyInto(aFloatArray9427), 0);
    }

    @Override
    protected void finalize() {
        deallocate();
    }


    @Override
    public void initializeTextureSampler(int location, int slot, TextureSampler sampler) {
        this.toolkit.setTextureSlot(slot);
        this.toolkit.setTextureSampler(sampler);
        OpenGL.glUniform1i(location, slot);
    }

    @Override
    public void addPositionAttributeDynamic(int location, float[] values, int count) {
        OpenGL.glUniform4fv(location, count, values, 0);
    }

    @Override
    public void deallocate() {
        if (this.programHandle != 0) {
            this.toolkit.destroyProgram(this.programHandle);
            if (this.vertexShaderHandle != 0) {
                this.toolkit.requestShaderDeletion(this.vertexShaderHandle);
            }

            if (this.fragmentShaderHandle != 0) {
                this.toolkit.requestShaderDeletion(this.fragmentShaderHandle);
            }

            this.programHandle = 0;
            this.vertexShaderHandle = 0;
            this.fragmentShaderHandle = 0;
        }

    }

    @Override
    public boolean createShaders() {
        if (this.aBool9425) {
            return true;
        } 
        this.toolkit.activeShaderMapping = null;
        this.vertexShaderHandle = method14671(GL_VERTEX_SHADER, this.vertexShaderProgramString);
        this.fragmentShaderHandle = method14671(GL_FRAGMENT_SHADER, this.fragmentShaderProgramString);
        if (this.vertexShaderHandle != 0 && this.fragmentShaderHandle != 0) {
            this.programHandle = OpenGL.glCreateProgram();
            if (this.vertexShaderHandle != 0) {
                OpenGL.glAttachShader(this.programHandle, this.vertexShaderHandle);
            }
            if (this.fragmentShaderHandle != 0) {
                OpenGL.glAttachShader(this.programHandle, this.fragmentShaderHandle);
            }
            OpenGL.glLinkProgram(this.programHandle);
            OpenGL.glGetProgramiv(this.programHandle, GL_LINK_STATUS, STATUS_BUFFER, 0);
            if (STATUS_BUFFER[0] == 0) {
                if (this.vertexShaderHandle != 0) {
                    OpenGL.glDetachShader(this.programHandle, this.vertexShaderHandle);
                    OpenGL.glDeleteShader(this.vertexShaderHandle);
                }
                if (this.fragmentShaderHandle != 0) {
                    OpenGL.glDetachShader(this.programHandle, this.fragmentShaderHandle);
                    OpenGL.glDeleteShader(this.fragmentShaderHandle);
                }
                OpenGL.glDeleteProgram(this.programHandle);
                return false;
            } else {
                OpenGL.glUseProgram(this.programHandle);
                int activeInSlot = -1;
                for (int slot = 0; slot < this.openglShader.getMappingCount(); slot++) {
                    if (this.openglShader.getMapping(slot) == this) {
                        activeInSlot = slot;
                        break;
                    }
                }
                if (activeInSlot == -1) {
                    return false;
                } else {
                    int slot = 0;
                    int i_3 = 0;
                    Class111 class1116;
                    for (int variableIndex = 0; variableIndex < this.openglShader.getPrimaryShaderVariableCount(); variableIndex++) {
                    	ShaderAttribute shaderAttribute = this.openglShader.getShaderAttribute(variableIndex);
                        if (shaderAttribute != null) {
                            shaderAttribute.loadUniformLocation(activeInSlot);
                            class1116 = shaderAttribute.method15457();
                            int i_7 = ((ShaderAttributeGL) shaderAttribute).uniformLocations[activeInSlot];
                            if (class1116 != Class111.aClass1111219 && class1116 != Class111.aClass1111182 && class1116 != Class111.aClass1111132 && class1116 != Class111.aClass1111133 && class1116 != Class111.aClass1111134) {
                                i_3 = Math.max(i_7, i_3);
                            } else {
                                slot = Math.max(i_7, slot);
                            }
                        }
                    }

                    for (int variableIndex = 0; variableIndex < this.openglShader.getSecondaryShaderVariableCount(); variableIndex++) {
                    	ShaderAttribute shaderAttribute = this.openglShader.getSecondaryAttribute(variableIndex);
                        if (shaderAttribute != null) {
                            shaderAttribute.loadUniformLocation(activeInSlot);
                            class1116 = shaderAttribute.method15457();
                            int i_7 = ((ShaderAttributeGL) shaderAttribute).uniformLocations[activeInSlot];
                            if (class1116 != Class111.aClass1111219 && class1116 != Class111.aClass1111182 && class1116 != Class111.aClass1111132 && class1116 != Class111.aClass1111133 && class1116 != Class111.aClass1111134) {
                                i_3 = Math.max(i_7, i_3);
                            } else {
                                slot = Math.max(i_7, slot);
                            }
                        }
                    }

                    this.aFloatArray9429 = new float[slot + 1];
                    this.aFloatArray9422 = new float[slot + 1];
                    this.aFloatArray9431 = new float[slot + 1];
                    this.aFloatArray9432 = new float[slot + 1];
                    this.textureSlots = new int[i_3 + 1];

                    for (int index = 0; index < this.textureSlots.length; index++) {
                        this.textureSlots[index] = -1;
                    }

                    this.vertexShaderProgramString = null;
                    this.fragmentShaderProgramString = null;
                    this.aBool9425 = true;
                    return true;
                }
            }
        } else {
            if (this.vertexShaderHandle != 0) {
                OpenGL.glDeleteShader(this.vertexShaderHandle);
            }

            if (this.fragmentShaderHandle != 0) {
                OpenGL.glDeleteShader(this.fragmentShaderHandle);
            }

            return false;
        }
    }

}
