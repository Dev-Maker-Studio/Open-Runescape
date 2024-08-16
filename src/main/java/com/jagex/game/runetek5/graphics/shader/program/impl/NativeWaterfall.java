package com.jagex.game.runetek5.graphics.shader.program.impl;

import com.jagex.game.runetek5.graphics.shader.ShaderAttribute;
import com.jagex.game.runetek5.graphics.shader.ShaderBuilderException;
import com.jagex.game.runetek5.graphics.shader.ShaderMapping;
import com.jagex.game.runetek5.graphics.texture.water.NativeWaterTexture;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.math.Vector4;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode;

public class NativeWaterfall extends NativeWaterShader {

    public int anInt10142;

    public int anInt10152;

    public int anInt10153;

    public int anInt10155;
    public final Matrix44 aClass38410138 = new Matrix44();
    public final Matrix44 aClass38410146 = new Matrix44();
    public ShaderAttribute worldMatrix;
    public ShaderAttribute wvpMatrix;
    public ShaderAttribute uGenerationPlane;
    public ShaderAttribute vGenerationPlane;
    public ShaderAttribute time;
    public ShaderAttribute billowSampler3d;
    public ShaderMapping waterfall2d;
    public ShaderMapping waterfall3d;
    public final float[] aFloatArray10148 = new float[4];

    public final float[] aFloatArray10150 = new float[4];

    public final NativeWaterTexture aClass4910147;

    public float aFloat10149;

    public NativeWaterfall(AbstractNativeToolkit class505Sub21, NativeWaterTexture class492) throws ShaderBuilderException {
        super(class505Sub21);
        this.aClass4910147 = class492;
        if (this.aClass4910147.readyToSample() && class505Sub21.supportsArbVertexShader()) {
            loadShaderWithAttributes("Waterfall");
        }
    }


    @Override
    public boolean buildProgram() throws ShaderBuilderException {
        this.worldMatrix = super.activeShader.getAttribute("WorldMatrix");
        this.wvpMatrix = super.activeShader.getAttribute("WVPMatrix");
        this.uGenerationPlane = super.activeShader.getAttribute("UGenerationPlane");
        this.vGenerationPlane = super.activeShader.getAttribute("VGenerationPlane");
        this.time = super.activeShader.getAttribute("Time");
        this.billowSampler3d = super.activeShader.getAttribute("billowSampler3D");
        if (this.aClass4910147.hasDepth) {
            this.waterfall3d = super.activeShader.getShaderMapping("Waterfall3D");
            super.activeShader.loadProgram(this.waterfall3d);
        } else {
            this.waterfall2d = super.activeShader.getShaderMapping("Waterfall2D");
            super.activeShader.loadProgram(this.waterfall2d);
        }
        return true;
    }

    public void method15549() {
        if (this.aClass4910147.hasDepth) {
            super.activeShader.loadProgram(this.waterfall3d);
        } else {
            super.activeShader.loadProgram(this.waterfall2d);
        }
        super.activeShader.setActiveMapping();
        if (this.aClass4910147.hasDepth) {
            super.activeShader.loadTextureSampler(this.billowSampler3d, 0, this.aClass4910147.billowSampler);
        }
        super.activeShader.loadMatrix(this.worldMatrix, this.aClass38410146);
        super.activeShader.loadMatrix(this.wvpMatrix, this.aClass38410138);
        super.activeShader.insertAttribute(this.uGenerationPlane, new Vector4(this.aFloatArray10148[0], this.aFloatArray10148[1], this.aFloatArray10148[2], this.aFloatArray10148[3]));
        super.activeShader.insertAttribute(this.vGenerationPlane, new Vector4(this.aFloatArray10150[0], this.aFloatArray10150[1], this.aFloatArray10150[2], this.aFloatArray10150[3]));
        super.activeShader.insertAttribute(this.time, new Vector4(this.aFloat10149, 0.0F, 0.0F, 0.0F));
        super.toolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, this.anInt10152, this.anInt10153, this.anInt10142, this.anInt10155);
    }


    public void method15550(int i_1) {
        float f_4 = ((i_1 & 0x3) + 1) * -5.0E-4f;
        float f_5 = ((i_1 >> 3 & 0x3) + 1) * 5.0E-4F;
        float f_6 = (i_1 & 0x40) == 0 ? 4.8828125E-4F : 9.765625E-4F;
        boolean bool7 = (i_1 & 0x80) != 0;
        if (bool7) {
            this.aFloatArray10148[0] = f_6;
            this.aFloatArray10148[1] = 0.0F;
            this.aFloatArray10148[2] = 0.0F;
        } else {
            this.aFloatArray10148[0] = 0.0F;
            this.aFloatArray10148[1] = 0.0F;
            this.aFloatArray10148[2] = f_6;
        }
        this.aFloatArray10148[3] = 0.0F;
        this.aFloatArray10150[0] = 0.0F;
        this.aFloatArray10150[1] = f_6;
        this.aFloatArray10150[2] = 0.0F;
        this.aFloatArray10150[3] = super.toolkit.animatingCycle * f_4 % 1.0F;
        if (!this.aClass4910147.hasDepth) {
            int i_8 = (int) (f_5 * super.toolkit.animatingCycle * 16.0F);
            super.toolkit.setTextureSampler(this.aClass4910147.textureSamplers[i_8 % 16]);
        } else {
            this.aFloat10149 = (float) ((double) super.toolkit.animatingCycle * f_5 % 1.0D);
        }
    }
}
