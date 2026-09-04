package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.toolkit.opengl.effect.ProgramAttribute

abstract class ShaderAttribute protected constructor(decoder: ShaderVariableDecoder) : ProgramAttribute() {

    @JvmField val variableType: ShaderVariableType
    @JvmField val primaryShaderAttributeName: String
    @JvmField val secondaryShaderAttributeName: String?
    @JvmField val arraySize: Int
    @JvmField val elementType: ShaderVariableType

    init {
        this.variableType = decoder.variableType!!
        this.primaryShaderAttributeName = decoder.primaryShaderAttributeName!!
        this.secondaryShaderAttributeName = decoder.secondaryShaderAttributeName
        this.arraySize = decoder.arraySize
        this.elementType = decoder.elementType!!
    }

    protected fun getPrimaryShaderAttributeName(): String {
        return this.primaryShaderAttributeName
    }

    fun getVariableType(): ShaderVariableType {
        return this.variableType
    }

    fun getElementType(): ShaderVariableType {
        return this.elementType
    }

    fun getArraySize(): Int {
        if (this.variableType != ShaderVariableType.ARRAY) {
            throw IllegalShaderArgumentException()
        } else {
            return this.arraySize
        }
    }

    override fun toString(): String {
        var text = this.variableType.toString() + " " + this.primaryShaderAttributeName
        if (this.secondaryShaderAttributeName != null && this.secondaryShaderAttributeName.isNotEmpty()) {
            text = "$text : ${this.secondaryShaderAttributeName}"
        }
        return text
    }

    abstract fun getUniformLocation(slot: Int): Int

    abstract fun loadUniformLocation(activeInSlot: Int): Boolean
}
