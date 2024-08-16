package com.jagex.game.runetek5.graphics.shader.program.impl;

import com.jagex.game.runetek5.graphics.shader.Shader;
import com.jagex.game.runetek5.graphics.shader.ShaderAttribute;
import com.jagex.game.runetek5.graphics.shader.ShaderBuilderException;
import com.jagex.game.runetek5.graphics.shader.ShaderMapping;
import com.jagex.game.runetek5.graphics.shader.program.AbstractWaterShader;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;

public abstract class NativeWaterShader extends AbstractWaterShader {

    public static final Matrix44 VIEW_PROJECTION_MATRIX = new Matrix44();
    protected Shader activeShader;
    public ShaderAttribute eyePositionOsMatrixAttribute;
    public ShaderAttribute textureMatrixAttribute;
    public ShaderAttribute modelMatrix;
    public ShaderAttribute viewMatrixAttribute;
    public ShaderAttribute projectionMatrixAttribute;
    public ShaderAttribute modelViewMatrixAttribute;
    public ShaderAttribute modelViewProjectionMatrixAttribute;
    public ShaderAttribute viewProjectionMatrixAttribute;
    public ShaderAttribute ambientColour;
    public ShaderAttribute sunDirection;
    public ShaderAttribute sunColour;
    public ShaderAttribute antiSunAttribute;
    public ShaderAttribute sunExponentAttribute;
    public ShaderAttribute eyePositionAttribute;
    public ShaderAttribute eyePositionOS;
    public ShaderAttribute sunDirectionOS;
    public int mappedWaterCount;
    public int[] mappedWaterFlags;
    public final float[] eyePosition = new float[3];
    public final Matrix44[] matrices;

    public NativeWaterShader(AbstractNativeToolkit toolkit) {
        super(toolkit);
        new Matrix43();
        this.matrices = new Matrix44[toolkit.maxConcurrentTextures];
        for (int i = 0; i < toolkit.maxConcurrentTextures; i++) {
            this.matrices[i] = new Matrix44();
        }
    }

    @Override
    public void method877() {
    }

    public boolean loadShaderWithAttributes(String shaderName) throws ShaderBuilderException {
        this.activeShader = super.toolkit.loadShader(shaderName);
        if (this.activeShader == null) {
            throw new ShaderBuilderException("");
        } 
        this.textureMatrixAttribute = this.activeShader.loadAttribute("textureMatrix");
        this.modelMatrix = this.activeShader.loadAttribute("modelMatrix");
        this.viewMatrixAttribute = this.activeShader.loadAttribute("viewMatrix");
        this.projectionMatrixAttribute = this.activeShader.loadAttribute("projectionMatrix");
        this.modelViewMatrixAttribute = this.activeShader.loadAttribute("modelViewMatrix");
        this.modelViewProjectionMatrixAttribute = this.activeShader.loadAttribute("modelViewProjectionMatrix");
        this.viewProjectionMatrixAttribute = this.activeShader.loadAttribute("viewProjectionMatrix");
        this.ambientColour = this.activeShader.loadAttribute("ambientColour");
        this.sunDirection = this.activeShader.loadAttribute("sunDirection");
        this.sunColour = this.activeShader.loadAttribute("sunColour");
        this.antiSunAttribute = this.activeShader.loadAttribute("antiSunColour");
        this.sunExponentAttribute = this.activeShader.loadAttribute("sunExponent");
        this.eyePositionAttribute = this.activeShader.loadAttribute("eyePosition");
        this.eyePositionOS = this.activeShader.loadAttribute("eyePositionOS");
        this.sunDirectionOS = this.activeShader.loadAttribute("sunDirectionOS");
        if (!buildProgram()) {
            this.activeShader = null;
            this.textureMatrixAttribute = null;
            this.modelMatrix = null;
            this.viewMatrixAttribute = null;
            this.projectionMatrixAttribute = null;
            this.modelViewMatrixAttribute = null;
            this.modelViewProjectionMatrixAttribute = null;
            this.viewProjectionMatrixAttribute = null;
            this.ambientColour = null;
            this.sunDirection = null;
            this.sunColour = null;
            this.antiSunAttribute = null;
            this.sunExponentAttribute = null;
            this.eyePositionAttribute = null;
            this.eyePositionOS = null;
            this.sunDirectionOS = null;
            return false;
        } 
        if (this.activeShader.getActiveMapping() == null) {
            ShaderMapping mappedWater = this.activeShader.createSingleMapping();
            if (mappedWater == null) {
                throw new ShaderBuilderException(this.activeShader.getShaderName());
            }
            this.activeShader.loadProgram(mappedWater);
        }
        this.mappedWaterCount = this.activeShader.getMappingCount();
        this.mappedWaterFlags = new int[this.mappedWaterCount];
        return true;
    }

    public abstract boolean buildProgram() throws ShaderBuilderException;

    public void method14526() {
        loadLights(super.toolkit.aBool8684);
        loadTextureMatrix(super.toolkit.getTextureMatrix());
        loadMatrices();
        method877();
    }

    @Override
    public void method885() {
        if (this.activeShader != null) {
            this.activeShader.reset();
        }
    }

    public void loadTextureMatrix(Matrix44 matrix44) {
        if (this.activeShader != null && this.activeShader.isActiveMapping() && super.toolkit.getCurrentTextureSlot() == 0) {
            int mappedWaterCursorPosition = this.activeShader.getOccupyingSlot();
            if ((this.mappedWaterFlags[mappedWaterCursorPosition] & 0x80) != 0 && this.textureMatrixAttribute != null) {
                this.activeShader.loadTextureMatrix(this.textureMatrixAttribute, matrix44);
                this.mappedWaterFlags[mappedWaterCursorPosition] &= -129;
            }
        }
    }

    public void loadMatrices() {
        if (this.activeShader != null && this.activeShader.isActiveMapping()) {
            int occupiedSlot = this.activeShader.getOccupyingSlot();
            int waterFlags = this.mappedWaterFlags[occupiedSlot];
            if ((waterFlags & 0x1) != 0 && this.modelMatrix != null) {
                this.activeShader.loadMatrix(this.modelMatrix, super.toolkit.getModelMatrix());
            }

            if ((waterFlags & 0x2) != 0) {
                if (this.viewMatrixAttribute != null) {
                    this.activeShader.loadMatrix(this.viewMatrixAttribute, super.toolkit.getViewMatrix());
                }

                if (this.eyePositionAttribute != null) {
                    this.eyePosition[0] = 0.0F;
                    this.eyePosition[1] = 0.0F;
                    this.eyePosition[2] = 0.0F;
                    super.toolkit.copySunPosition().calculateEyePosition(this.eyePosition[0], this.eyePosition[1], this.eyePosition[2], this.eyePosition);
                    this.activeShader.uniform3f(this.eyePositionAttribute, this.eyePosition[0], this.eyePosition[1], this.eyePosition[2]);
                }
            }

            if ((waterFlags & 0x10) != 0 && this.modelViewMatrixAttribute != null) {
                this.activeShader.loadMatrix(this.modelViewMatrixAttribute, super.toolkit.getModelViewMatrix());
            }

            if ((waterFlags & 0x8) != 0 && this.eyePositionOsMatrixAttribute != null) {
                this.activeShader.loadMatrix(this.eyePositionOsMatrixAttribute, super.toolkit.getEyePositionOsMatrix());
            }

            if ((waterFlags & 0x4) != 0 && this.projectionMatrixAttribute != null) {
                this.activeShader.loadMatrix(this.projectionMatrixAttribute, super.toolkit.getProjectionMatrix());
            }

            if ((waterFlags & 0x20) != 0 && this.modelViewProjectionMatrixAttribute != null) {
                VIEW_PROJECTION_MATRIX.calculatePerspective(super.toolkit.getModelViewMatrix(), super.toolkit.getProjectionMatrix());
                this.activeShader.loadMatrix(this.modelViewProjectionMatrixAttribute, VIEW_PROJECTION_MATRIX);
            }

            if ((waterFlags & 0x40) != 0 && this.viewProjectionMatrixAttribute != null) {
                VIEW_PROJECTION_MATRIX.calculatePerspective(super.toolkit.getViewMatrix(), super.toolkit.getProjectionMatrix());
                this.activeShader.loadMatrix(this.viewProjectionMatrixAttribute, VIEW_PROJECTION_MATRIX);
            }

            if (super.toolkit.getCurrentTextureSlot() == 0 && (this.mappedWaterFlags[occupiedSlot] & 0x80) != 0 && this.textureMatrixAttribute != null) {
                this.activeShader.loadTextureMatrix(this.textureMatrixAttribute, super.toolkit.getTextureMatrix());
                this.mappedWaterFlags[occupiedSlot] &= -129;
            }

            this.mappedWaterFlags[occupiedSlot] = 0;
        }

    }

    public void method14529(Matrix43 matrix44var2) {
        if (this.eyePositionOS != null) {
            Matrix44 eyePosOsMatrix = super.toolkit.getEyePositionOsMatrix();
            float[] eyePos = {eyePosOsMatrix.faceList[12], eyePosOsMatrix.faceList[13], eyePosOsMatrix.faceList[14]};
            float[] floats_5 = {0.0F, 0.0F, 0.0F};
            eyePosOsMatrix.method6528(floats_5);
            float[] floats_6 = {0.0F, 0.0F, 0.0F};
            super.toolkit.copySunPosition().calculatePerspective(0.0F, 0.0F, 0.0F, floats_6);
            matrix44var2.method5243(eyePos);
            this.activeShader.uniform3f(this.eyePositionOS, eyePos[0], eyePos[1], eyePos[2]);
        }

    }

    @Override
    public void method886() {
        method14532(Matrix43.aClass2943518);
    }

    public void loadLights(boolean dummy) {
        if (this.activeShader != null && this.activeShader.isActiveMapping()) {
            if (this.ambientColour != null) {
                this.activeShader.uniform3f(this.ambientColour, super.toolkit.baseLightColorRed * super.toolkit.lightIntensity, super.toolkit.baseLightColorGreen * super.toolkit.lightIntensity, super.toolkit.baseLightColorBlue * super.toolkit.lightIntensity);
            }

            if (this.sunDirection != null) {
                this.activeShader.uniform3f(this.sunDirection, super.toolkit.sunPosition[0], super.toolkit.sunPosition[1], super.toolkit.sunPosition[2]);
            }

            if (this.sunColour != null) {
                this.activeShader.uniform3f(this.sunColour, super.toolkit.baseLightColorRed * super.toolkit.sunColorIntensity, super.toolkit.baseLightColorGreen * super.toolkit.sunColorIntensity, super.toolkit.baseLightColorBlue * super.toolkit.sunColorIntensity);
            }

            if (this.antiSunAttribute != null) {
                this.activeShader.uniform3f(this.antiSunAttribute, -super.toolkit.baseLightColorRed * super.toolkit.antiSunColorIntensity, -super.toolkit.baseLightColorGreen * super.toolkit.antiSunColorIntensity, -super.toolkit.baseLightColorBlue * super.toolkit.antiSunColorIntensity);
            }

            if (this.sunExponentAttribute != null) {
                this.activeShader.uniform1f(this.sunExponentAttribute, 64.0F + Math.abs(super.toolkit.sunPosition[1]) * 928.0F);
            }
        }
    }

    @Override
    public void method879() {
        method14532(Matrix43.aClass2943518);
    }

    public void method14532(Matrix43 matrix44var1) {
        Matrix44 matrix442 = super.toolkit.aClass3848683;
        matrix442.fromMatrix(matrix44var1);
        int slot = this.activeShader.getOccupyingSlot();
        if (this.modelMatrix != null) {
            this.activeShader.loadMatrix(this.modelMatrix, matrix442);
            this.mappedWaterFlags[slot] &= -2;
        }

        if (this.modelViewMatrixAttribute != null) {
            VIEW_PROJECTION_MATRIX.calculatePerspective(matrix442, super.toolkit.getViewMatrix());
            this.activeShader.loadMatrix(this.modelViewMatrixAttribute, VIEW_PROJECTION_MATRIX);
            this.mappedWaterFlags[slot] &= -17;
        }

        if (this.modelViewProjectionMatrixAttribute != null) {
            VIEW_PROJECTION_MATRIX.calculatePerspective(matrix442, super.toolkit.method13931());
            this.activeShader.loadMatrix(this.modelViewProjectionMatrixAttribute, VIEW_PROJECTION_MATRIX);
            this.mappedWaterFlags[slot] &= -33;
        }

        method14529(matrix44var1);
        method14549(matrix44var1);
    }

    @Override
    public void method888() {
        this.activeShader.setActiveMapping();
        this.mappedWaterFlags[this.activeShader.getOccupyingSlot()] = -1;
        method14550();
        method14526();
    }

    @Override
    public void method889() {
        method14532(Matrix43.aClass2943518);
    }

    public void setTextured() {
        for (int slot = 0; slot < this.mappedWaterCount; slot++) {
            this.mappedWaterFlags[slot] |= 128;
        }
    }

    @Override
    public void method873() {
        this.activeShader.setActiveMapping();
        this.mappedWaterFlags[this.activeShader.getOccupyingSlot()] = -1;
        method14550();
        method14526();
    }

    public void method14549(Matrix43 matrix44var2) {
        if (this.sunDirectionOS != null) {
            float[] sunPosition = {super.toolkit.sunPosition[0], super.toolkit.sunPosition[1], super.toolkit.sunPosition[2]};
            matrix44var2.method5230(sunPosition);
            this.activeShader.uniform3f(this.sunDirectionOS, sunPosition[0], sunPosition[1], sunPosition[2]);
        }

    }

    public void method14550() {
        this.matrices[0].identity();
        setTextured();
    }

}
