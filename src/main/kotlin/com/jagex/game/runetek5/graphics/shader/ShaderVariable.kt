package com.jagex.game.runetek5.graphics.shader

/**
 * Content table:
 *
 *A> program_variable_name: PointLightCount, program_variable_value: 0
 *A> program_variable_name: ShaderMode, program_variable_value: 3
 *B> program_variable_name: ShaderMode, program_variable_value: 3
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 0
 *A> program_variable_name: ShaderMode, program_variable_value: 3
 *B> program_variable_name: ShaderMode, program_variable_value: 3
 *B> program_variable_name: IgnoreAlpha, program_variable_value: true
 *A> program_variable_name: PointLightCount, program_variable_value: 0
 *A> program_variable_name: ShaderMode, program_variable_value: 0
 *B> program_variable_name: ShaderMode, program_variable_value: 0
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 1
 *A> program_variable_name: ShaderMode, program_variable_value: 0
 *B> program_variable_name: ShaderMode, program_variable_value: 0
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 2
 *A> program_variable_name: ShaderMode, program_variable_value: 0
 *B> program_variable_name: ShaderMode, program_variable_value: 0
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 3
 *A> program_variable_name: ShaderMode, program_variable_value: 0
 *B> program_variable_name: ShaderMode, program_variable_value: 0
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 4
 *A> program_variable_name: ShaderMode, program_variable_value: 0
 *B> program_variable_name: ShaderMode, program_variable_value: 0
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 0
 *A> program_variable_name: ShaderMode, program_variable_value: 1
 *B> program_variable_name: ShaderMode, program_variable_value: 1
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 1
 *A> program_variable_name: ShaderMode, program_variable_value: 1
 *B> program_variable_name: ShaderMode, program_variable_value: 1
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 2
 *A> program_variable_name: ShaderMode, program_variable_value: 1
 *B> program_variable_name: ShaderMode, program_variable_value: 1
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 3
 *A> program_variable_name: ShaderMode, program_variable_value: 1
 *B> program_variable_name: ShaderMode, program_variable_value: 1
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 4
 *A> program_variable_name: ShaderMode, program_variable_value: 1
 *B> program_variable_name: ShaderMode, program_variable_value: 1
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 0
 *A> program_variable_name: ShaderMode, program_variable_value: 2
 *B> program_variable_name: ShaderMode, program_variable_value: 2
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 1
 *A> program_variable_name: ShaderMode, program_variable_value: 2
 *B> program_variable_name: ShaderMode, program_variable_value: 2
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 2
 *A> program_variable_name: ShaderMode, program_variable_value: 2
 *B> program_variable_name: ShaderMode, program_variable_value: 2
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 3
 *A> program_variable_name: ShaderMode, program_variable_value: 2
 *B> program_variable_name: ShaderMode, program_variable_value: 2
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 4
 *A> program_variable_name: ShaderMode, program_variable_value: 2
 *B> program_variable_name: ShaderMode, program_variable_value: 2
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: PointLightCount, program_variable_value: 0
 *A> program_variable_name: ShaderMode, program_variable_value: 4
 *B> program_variable_name: ShaderMode, program_variable_value: 4
 *B> program_variable_name: IgnoreAlpha, program_variable_value: false
 *A> program_variable_name: Masked, program_variable_value: false
 *B> program_variable_name: Masked, program_variable_value: false
 *B> program_variable_name: AlphaTex, program_variable_value: false
 *A> program_variable_name: Masked, program_variable_value: true
 *B> program_variable_name: Masked, program_variable_value: true
 *B> program_variable_name: AlphaTex, program_variable_value: false
 *A> program_variable_name: Masked, program_variable_value: false
 *B> program_variable_name: Masked, program_variable_value: false
 *B> program_variable_name: AlphaTex, program_variable_value: true
 *B> program_variable_name: waves, program_variable_value: true
 *B> program_variable_name: waves, program_variable_value: false
 *
 */
class ShaderVariable {

    /**
     * The name of the value in the program we're manipulating
     */
    @JvmField var programVariableName: String? = null

    /**
     * The value we're setting in the OpenGL program,
     * either int or boolean
     */
    @JvmField var programVariableValue: String? = null

    fun decode(shaderDecoder: ShaderBuffer) {
        programVariableName = shaderDecoder.readString()
        programVariableValue = shaderDecoder.readString()
    }
}
