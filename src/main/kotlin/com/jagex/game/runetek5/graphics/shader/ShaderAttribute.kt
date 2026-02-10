package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.toolkit.opengl.effect.ProgramAttribute

abstract class ShaderAttribute protected constructor(class1221: ShaderVariableDecoder) : ProgramAttribute() {

    @JvmField val aClass11110058: ShaderVariableType
    @JvmField val primaryShaderAttributeName: String
    @JvmField val secondaryShaderAttributeName: String?
    @JvmField val anInt10059: Int
    @JvmField val aClass11110060: ShaderVariableType

    init {
        this.aClass11110058 = class1221.aClass1111529!!
        this.primaryShaderAttributeName = class1221.primaryShaderAttributeName!!
        this.secondaryShaderAttributeName = class1221.secondaryShaderAttributeName
        this.anInt10059 = class1221.anInt1528
        this.aClass11110060 = class1221.aClass1111533!!
    }

    protected fun getPrimaryShaderAttributeName(): String {
        return this.primaryShaderAttributeName
    }

    fun method15457(): ShaderVariableType {
        return this.aClass11110058
    }

    fun method15458(): ShaderVariableType {
        return this.aClass11110060
    }

    fun method15459(): Int {
        if (this.aClass11110058 != ShaderVariableType.aClass1111109) {
            throw IllegalShaderArgumentException()
        } else {
            return this.anInt10059
        }
    }

    override fun toString(): String {
        var string_1 = this.aClass11110058.toString() + " " + this.primaryShaderAttributeName
        if (this.secondaryShaderAttributeName != null && this.secondaryShaderAttributeName.isNotEmpty()) {
            string_1 = "$string_1 : ${this.secondaryShaderAttributeName}"
        }
        return string_1
    }

    abstract fun getUniformLocation(slot: Int): Int

    abstract fun loadUniformLocation(var1: Int): Boolean
}
