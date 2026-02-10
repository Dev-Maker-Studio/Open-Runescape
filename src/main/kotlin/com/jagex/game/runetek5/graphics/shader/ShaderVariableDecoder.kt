package com.jagex.game.runetek5.graphics.shader

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
class ShaderVariableDecoder {

    @JvmField var anIntArray1531: IntArray? = null
    @JvmField var anIntArray1534: IntArray? = null
    @JvmField var aClass1111529: ShaderVariableType? = null
    @JvmField var aClass1111533: ShaderVariableType? = null
    @JvmField var anInt1528 = -1

    /**
     * Always used
     */
    @JvmField var primaryShaderAttributeName: String? = null

    /**
     * Always unused
     */
    @JvmField var secondaryShaderAttributeName: String? = null

    fun decode(shaderBuffer: ShaderBuffer) {
        val mainEntryCount = shaderBuffer.getEntryCount()
        aClass1111529 = ShaderVariableType.getAll()[mainEntryCount]
        if (aClass1111529 == ShaderVariableType.aClass1111109) {
            val i_4 = shaderBuffer.getEntryCount()
            aClass1111533 = ShaderVariableType.getAll()[i_4]
            anInt1528 = shaderBuffer.getEntryCount()
        }
        primaryShaderAttributeName = shaderBuffer.readString()
        secondaryShaderAttributeName = shaderBuffer.readString()
        val count = shaderBuffer.getEntryCount()
        anIntArray1531 = IntArray(count)
        anIntArray1534 = IntArray(count)
        for (index in 0 until count) {
            anIntArray1531!![index] = shaderBuffer.getEntryCount()
            anIntArray1534!![index] = shaderBuffer.getEntryCount()
        }
    }
}
