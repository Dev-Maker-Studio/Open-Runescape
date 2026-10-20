package com.jagex.game.runetek5.graphics.shader

class ShaderVariable {

    @JvmField var programVariableName: String? = null

    @JvmField var programVariableValue: String? = null

    fun decode(shaderDecoder: ShaderBuffer) {
        programVariableName = shaderDecoder.readString()
        programVariableValue = shaderDecoder.readString()
    }
}
