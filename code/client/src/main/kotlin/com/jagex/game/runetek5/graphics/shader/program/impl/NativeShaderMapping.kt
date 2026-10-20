package com.jagex.game.runetek5.graphics.shader.program.impl

import com.jagex.game.runetek5.graphics.shader.ShaderBuffer
import com.jagex.game.runetek5.graphics.shader.ShaderVariable

class NativeShaderMapping {

    @JvmField var mappingName: String? = null
    @JvmField var vertexShaderName: String? = null
    @JvmField var fragmentShaderName: String? = null
    @JvmField var vertexShaderVariables: Array<ShaderVariable?>? = null
    @JvmField var fragmentShaderVariables: Array<ShaderVariable?>? = null

    fun decode(shaderBuffer: ShaderBuffer) {
        mappingName = shaderBuffer.readString()
        vertexShaderName = shaderBuffer.readString()
        fragmentShaderName = shaderBuffer.readString()
        val vertexShaderCount = shaderBuffer.getEntryCount()
        val fragmentShaderCount = shaderBuffer.getEntryCount()
        vertexShaderVariables = if (vertexShaderCount == 0) null else arrayOfNulls(vertexShaderCount)
        fragmentShaderVariables = if (fragmentShaderCount == 0) null else arrayOfNulls(fragmentShaderCount)

        for (shaderIndex in 0 until vertexShaderCount) {
            vertexShaderVariables!![shaderIndex] = ShaderVariable()
            vertexShaderVariables!![shaderIndex]!!.decode(shaderBuffer)
        }

        for (shaderIndex in 0 until fragmentShaderCount) {
            fragmentShaderVariables!![shaderIndex] = ShaderVariable()
            fragmentShaderVariables!![shaderIndex]!!.decode(shaderBuffer)
        }
    }
}
