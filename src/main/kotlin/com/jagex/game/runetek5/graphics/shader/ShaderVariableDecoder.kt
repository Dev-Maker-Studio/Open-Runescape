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

    @JvmField var mappingProgramIndices: IntArray? = null
    @JvmField var mappingSlotIndices: IntArray? = null
    @JvmField var variableType: ShaderVariableType? = null
    @JvmField var elementType: ShaderVariableType? = null
    @JvmField var arraySize = -1

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
        variableType = ShaderVariableType.getAll()[mainEntryCount]
        if (variableType == ShaderVariableType.aClass1111109) {
            val i_4 = shaderBuffer.getEntryCount()
            elementType = ShaderVariableType.getAll()[i_4]
            arraySize = shaderBuffer.getEntryCount()
        }
        primaryShaderAttributeName = shaderBuffer.readString()
        secondaryShaderAttributeName = shaderBuffer.readString()
        val count = shaderBuffer.getEntryCount()
        mappingProgramIndices = IntArray(count)
        mappingSlotIndices = IntArray(count)
        for (index in 0 until count) {
            mappingProgramIndices!![index] = shaderBuffer.getEntryCount()
            mappingSlotIndices!![index] = shaderBuffer.getEntryCount()
        }
    }
}
