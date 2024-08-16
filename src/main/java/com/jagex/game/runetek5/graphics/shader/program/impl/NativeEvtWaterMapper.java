package com.jagex.game.runetek5.graphics.shader.program.impl;

import com.jagex.game.runetek5.graphics.shader.ShaderAttribute;
import com.jagex.game.runetek5.graphics.shader.ShaderBuilderException;
import com.jagex.game.runetek5.graphics.shader.ShaderMapping;
import com.jagex.game.runetek5.graphics.texture.water.NativeWaterTexture;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.math.Vector4;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode;

public class NativeEvtWaterMapper extends NativeWaterShader {

    public int anInt10094;

    public int effectParam1;

    public int anInt10110;

    public int anInt10111;

    public int anInt10112;
    public final Matrix44 waterViewingPlaneMatrix = new Matrix44();
    public final Matrix44 textureCoordinateMatrix = new Matrix44();
    public final Vector3 eye = new Vector3();
    public final Vector4 heightFogPlaneVector = new Vector4();
    public final Vector3 heightFogColorVector = new Vector3();
    public final Vector4 distanceFogPlaneVector = new Vector4();
    public final Vector3 distanceFogColorVector = new Vector3();
    public ShaderAttribute normalSamplerAttribute;
    public ShaderAttribute envMapSamplerAttribute;
    public ShaderAttribute waterViewingPlaneMatrixAttribute;
    public ShaderAttribute textureCoordinateMatrixAttribute;
    public ShaderAttribute eyePosAndTimeAttribute;
    public ShaderAttribute sunDirectionAndExponentAttribute;
    public ShaderAttribute sunColorAndWaveExponentAttribute;
    public ShaderAttribute waveIntensityAndBreakWaterDepthAndOffsetAttribute;
    public ShaderAttribute heightFogPlaneAttribute;
    public ShaderAttribute heightFogColorAttribute;
    public ShaderAttribute distanceFogPlaneAttribute;
    public ShaderAttribute distanceFogColorAttribute;
    public float aFloat10114;
    public final Vector4 sunDirectionAndExponentVector = new Vector4();
    public final Vector4 sunColorAndWaveExponentVector = new Vector4();
    public final Vector4 waveIntensityAndBreakWaterDepthAndOffsetVector = new Vector4();
    public final NativeWaterTexture nativeWaterTexture;

    public final boolean renderSea;

    public NativeEvtWaterMapper(AbstractNativeToolkit class505Sub21, NativeWaterTexture class492) throws ShaderBuilderException {
        super(class505Sub21);
        new Matrix44();
        this.nativeWaterTexture = class492;
        this.renderSea = true;
        if (class505Sub21.supportsArbVertexShader() && class505Sub21.supportsArbFragmentShader()) {
            loadShaderWithAttributes("EnvMappedWater");
        }
    }

    @Override
    public boolean buildProgram() throws ShaderBuilderException {
        if (!this.nativeWaterTexture.loadSampledTexture()) {
            return false;
        } 
        this.normalSamplerAttribute = super.activeShader.getAttribute("NormalSampler");
        this.envMapSamplerAttribute = super.activeShader.getAttribute("EnvMapSampler");
        this.waterViewingPlaneMatrixAttribute = super.activeShader.getAttribute("WVPMatrix");
        this.textureCoordinateMatrixAttribute = super.activeShader.getAttribute("TexCoordMatrix");
        this.eyePosAndTimeAttribute = super.activeShader.getAttribute("EyePosAndTime");
        this.sunDirectionAndExponentAttribute = super.activeShader.getAttribute("SunDirectionAndExponent");
        this.sunColorAndWaveExponentAttribute = super.activeShader.getAttribute("SunColourAndWaveExponent");
        this.waveIntensityAndBreakWaterDepthAndOffsetAttribute = super.activeShader.getAttribute("WaveIntensityAndBreakWaterDepthAndOffset");
        this.heightFogPlaneAttribute = super.activeShader.getAttribute("HeightFogPlane");
        this.heightFogColorAttribute = super.activeShader.getAttribute("HeightFogColour");
        this.distanceFogPlaneAttribute = super.activeShader.getAttribute("DistanceFogPlane");
        this.distanceFogColorAttribute = super.activeShader.getAttribute("DistanceFogColour");
        ShaderMapping mappedWater = super.activeShader.getShaderMapping(this.renderSea ? "EnvMappedSea" : "EnvMappedWater");
        super.activeShader.loadProgram(mappedWater);
        return true;       
    }

    @Override
    public void loadTextureMatrix(Matrix44 matrix441) {
        Matrix44 matrix442 = new Matrix44();
        matrix442.method6525(this.aFloat10114, this.aFloat10114, 1.0F, 1.0F);
        matrix442.calculatePerspective(matrix441, matrix442);
        super.loadTextureMatrix(matrix442);
    }

    @Override
    public void loadLights(boolean dummy) {
        float intensityMultiplier = 1.0F + (1.0F - Math.abs(super.toolkit.sunPosition[1])) * 2.0F;
        float baseIntensity = super.toolkit.sunColorIntensity;
        super.toolkit.sunColorIntensity *= intensityMultiplier;
        super.loadLights(dummy);
        super.toolkit.sunColorIntensity = baseIntensity;
    }
    
    public void method15517() {
		this.aFloat10114 = 2.4414062E-4F;
		int z = 256;
		float w = 0.3F;
		float y = 0.4F;
		float x = 0.1F;
		int waveExponent = 5;
		this.textureCoordinateMatrix.method6525(this.aFloat10114, this.aFloat10114, this.aFloat10114, this.aFloat10114);
		this.sunDirectionAndExponentVector.set(-super.toolkit.sunPosition[0], -super.toolkit.sunPosition[1], -super.toolkit.sunPosition[2], 32.0F);
		this.sunColorAndWaveExponentVector.set(super.toolkit.baseLightColorRed, super.toolkit.baseLightColorGreen, super.toolkit.baseLightColorBlue, (float) waveExponent);
		this.waveIntensityAndBreakWaterDepthAndOffsetVector.set(x, y, (float) z, w);
	}

    public void method15520() {
        method15517(); 
        super.activeShader.loadProgram(super.activeShader.createSingleMapping());
        super.activeShader.setActiveMapping();
        super.activeShader.loadTextureSampler(this.normalSamplerAttribute, 0, this.nativeWaterTexture.normalSampler);
        super.activeShader.loadTextureSampler(this.envMapSamplerAttribute, 1, super.toolkit.getEnvironmentMapSampler());
        super.activeShader.loadMatrix(this.waterViewingPlaneMatrixAttribute, this.waterViewingPlaneMatrix);
        super.activeShader.loadTextureMatrix(this.textureCoordinateMatrixAttribute, this.textureCoordinateMatrix);
        int speed = 1 << (this.effectParam1 & 0x3);
        super.activeShader.loadRgbaColorAttribute(this.eyePosAndTimeAttribute, this.eye.x, this.eye.y, this.eye.z, (float) (speed * super.toolkit.animatingCycle % 40000) / 40000.0F);
        super.activeShader.insertAttribute(this.sunDirectionAndExponentAttribute, this.sunDirectionAndExponentVector);
        super.activeShader.insertAttribute(this.sunColorAndWaveExponentAttribute, this.sunColorAndWaveExponentVector);
        super.activeShader.insertAttribute(this.waveIntensityAndBreakWaterDepthAndOffsetAttribute, this.waveIntensityAndBreakWaterDepthAndOffsetVector);
        super.activeShader.insertAttribute(this.heightFogPlaneAttribute, this.heightFogPlaneVector);
        super.activeShader.uniform3f(this.heightFogColorAttribute, this.heightFogColorVector);
        super.activeShader.insertAttribute(this.distanceFogPlaneAttribute, this.distanceFogPlaneVector);
        super.activeShader.uniform3f(this.distanceFogColorAttribute, this.distanceFogColorVector);
        super.toolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, this.anInt10110, this.anInt10111, this.anInt10094, this.anInt10112);
    }
}
