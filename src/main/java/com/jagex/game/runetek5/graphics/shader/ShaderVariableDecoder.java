package com.jagex.game.runetek5.graphics.shader;

import com.jagex.game.runetek5.unidentified.Class111;

/**
 * 
 * @author Brainpower
 *
 *	Content table:
 *
A: DiffuseSampler
A: EnvironmentSampler
A: WVPMatrix
A: TexCoordMatrix
A: EyePos
A: DistanceFogPlane
A: DistanceFogColour
A: HeightFogPlane
A: HeightFogColour
A: SpecularExponent
A: SunDir
A: SunColour
A: AntiSunColour
A: AmbientColour
A: ColourModifier
A: WorldMatrix
A: PointLightsPosAndRadiusSq
A: PointLightsDiffuseColour
A: SpriteSampler
A: MaskSampler
A: WVPMatrix
A: SpriteTexCoordMatrix
A: MaskTexCoordMatrix
A: MulColour
A: AddColour
A: WVPMatrix
A: TexCoordMatrix
A: DiffuseSampler
A: DiffuseColour
A: WVPMatrix
A: TexCoordMatrix
A: EyePosAndTime
A: DistanceFogPlane
A: DistanceFogColour
A: HeightFogPlane
A: HeightFogColour
A: SunDir
A: SunColour
A: AmbientColour
A: WorldMatrix
A: SunDirectionAndExponent
A: SunColourAndWaveExponent
A: WaveIntensityAndBreakWaterDepthAndOffset
A: NormalSampler
A: EnvMapSampler
A: WorldMatrix
A: WVPMatrix
A: UGenerationPlane
A: VGenerationPlane
A: Time
A: billowSampler3D
A: billowSampler2D
A: modelView
A: proj
A: PosAndTexCoords
A: sceneTex
A: params
A: pixelOffsetAndBloomScale
A: sampleSize
A: bloomTex1
A: modelView
A: proj
A: PosAndTexCoords
A: sceneTex
A: params
A: pixelOffsetAndBloomScale 
A: sampleSize
A: bloomTex1
 *
 */
public class ShaderVariableDecoder {

    public int[] anIntArray1531;
    public int[] anIntArray1534;
    public Class111 aClass1111529;
    public Class111 aClass1111533;
    public int anInt1528 = -1;
    
    /**
     * Always used
     */
    public String primaryShaderAttributeName;
    
    /**
     * Always unused
     */
    public String secondaryShaderAttributeName;

    public void decode(ShaderBuffer shaderBuffer) {
        int mainEntryCount = shaderBuffer.getEntryCount();
        this.aClass1111529 = Class111.getAll()[mainEntryCount];
        if (this.aClass1111529 == Class111.aClass1111109) {
            int i_4 = shaderBuffer.getEntryCount();
            this.aClass1111533 = Class111.getAll()[i_4];
            this.anInt1528 = shaderBuffer.getEntryCount();
        }
        this.primaryShaderAttributeName = shaderBuffer.readString();
        this.secondaryShaderAttributeName = shaderBuffer.readString();
        int count = shaderBuffer.getEntryCount();
        this.anIntArray1531 = new int[count];
        this.anIntArray1534 = new int[count];
        for (int index = 0; index < count; index++) {
            this.anIntArray1531[index] = shaderBuffer.getEntryCount();
            this.anIntArray1534[index] = shaderBuffer.getEntryCount();
        }
    }
}
