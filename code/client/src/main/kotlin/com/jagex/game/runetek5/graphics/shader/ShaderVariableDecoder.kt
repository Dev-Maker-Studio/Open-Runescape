package com.jagex.game.runetek5.graphics.shader

class ShaderVariableDecoder {

    @JvmField var mappingProgramIndices: IntArray? = null
    @JvmField var mappingSlotIndices: IntArray? = null
    @JvmField var variableType: ShaderVariableType? = null
    @JvmField var elementType: ShaderVariableType? = null
    @JvmField var arraySize = -1

    @JvmField var primaryShaderAttributeName: String? = null

    @JvmField var secondaryShaderAttributeName: String? = null

    fun decode(shaderBuffer: ShaderBuffer) {
        val variableTypeId = shaderBuffer.getEntryCount()
        variableType = ShaderVariableType.getAll()[variableTypeId]
        if (variableType == ShaderVariableType.ARRAY) {
            val elementTypeId = shaderBuffer.getEntryCount()
            elementType = ShaderVariableType.getAll()[elementTypeId]
            arraySize = shaderBuffer.getEntryCount()
        }
        primaryShaderAttributeName = shaderBuffer.readString()
        secondaryShaderAttributeName = shaderBuffer.readString()
        val mappingCount = shaderBuffer.getEntryCount()
        mappingProgramIndices = IntArray(mappingCount)
        mappingSlotIndices = IntArray(mappingCount)
        for (index in 0 until mappingCount) {
            mappingProgramIndices!![index] = shaderBuffer.getEntryCount()
            mappingSlotIndices!![index] = shaderBuffer.getEntryCount()
        }
    }
}
