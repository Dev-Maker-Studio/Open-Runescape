package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.toolkit.opengl.effect.ProgramAttribute

abstract class ShaderAttribute protected constructor(class1221: ShaderVariableDecoder) : ProgramAttribute() {

    @JvmField val variableType: ShaderVariableType
    @JvmField val primaryShaderAttributeName: String
    @JvmField val secondaryShaderAttributeName: String?
    @JvmField val arraySize: Int
    @JvmField val elementType: ShaderVariableType

    init {
        this.variableType = class1221.variableType!!
        this.primaryShaderAttributeName = class1221.primaryShaderAttributeName!!
        this.secondaryShaderAttributeName = class1221.secondaryShaderAttributeName
        this.arraySize = class1221.arraySize
        this.elementType = class1221.elementType!!
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
        if (this.variableType != ShaderVariableType.aClass1111109) {
            throw IllegalShaderArgumentException()
        } else {
            return this.arraySize
        }
    }

    override fun toString(): String {
        var string_1 = this.variableType.toString() + " " + this.primaryShaderAttributeName
        if (this.secondaryShaderAttributeName != null && this.secondaryShaderAttributeName.isNotEmpty()) {
            string_1 = "$string_1 : ${this.secondaryShaderAttributeName}"
        }
        return string_1
    }

    abstract fun getUniformLocation(slot: Int): Int

    abstract fun loadUniformLocation(var1: Int): Boolean
}
