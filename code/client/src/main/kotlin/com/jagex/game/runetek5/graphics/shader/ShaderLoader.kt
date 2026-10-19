package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.graphics.shader.program.impl.NativeShaderMapping

class ShaderLoader(data: ByteArray) {

    @JvmField var name: String? = null

    @JvmField var primaryShaderVariables: Array<ShaderVariableDecoder>? = null

    @JvmField var secondaryShaderVariables: Array<ShaderVariableDecoder>? = null

    @JvmField var mappings: Array<NativeShaderMapping>? = null

    init {
        load(data)
    }

    @Throws(ShaderException::class)
    fun load(shaderData: ByteArray) {
        val decoder = ShaderBuffer(shaderData)
        val headerLength = decoder.getEntryCount()
        if (headerLength != 4) {
            throw ShaderException(this)
        }
        this.name = decoder.readString()
        this.primaryShaderVariables = Array(decoder.getEntryCount()) { ShaderVariableDecoder() }
        this.secondaryShaderVariables = Array(decoder.getEntryCount()) { ShaderVariableDecoder() }
        this.mappings = Array(decoder.getEntryCount()) { NativeShaderMapping() }
        /** Those are ALWAYS used **/
        for (index in this.primaryShaderVariables!!.indices) {
            this.primaryShaderVariables!![index].decode(decoder)
        }
        /** Those are NEVER unused **/
        for (index in this.secondaryShaderVariables!!.indices) {
            this.secondaryShaderVariables!![index].decode(decoder)
        }
        for (index in this.mappings!!.indices) {
            this.mappings!![index].decode(decoder)
        }
    }
}
