package com.jagex.game.runetek5.graphics.shader

import jaggl.OpenGL

class ShaderAttributeGL(
    @JvmField val shaderGl: ShaderGL,
    shaderDecoder: ShaderVariableDecoder
) : ShaderAttribute(shaderDecoder) {

    @JvmField var uniformLocations: IntArray? = null

    override fun getUniformLocation(index: Int): Int {
        return this.uniformLocations!![index]
    }

    fun getActiveUniformLocation(): Int {
        return this.uniformLocations!![this.shaderGl.getOccupyingSlot()]
    }

    override fun loadUniformLocation(activeInSlot: Int): Boolean {
        if (this.uniformLocations == null) {
            this.uniformLocations = IntArray(this.shaderGl.getMappingCount())
        }
        val shaderMapping = this.shaderGl.getMapping(activeInSlot) as ShaderMappingGL
        this.uniformLocations!![activeInSlot] = OpenGL.glGetUniformLocation(shaderMapping.programHandle, getPrimaryShaderAttributeName())
        return this.uniformLocations!![activeInSlot] != -1
    }
}
