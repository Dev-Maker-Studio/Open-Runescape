package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.graphics.shader.program.impl.NativeShaderMapping
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import jaggl.OpenGL

/**
 * OpenGL shader implementation.
 */
class ShaderGL(
    @JvmField val activeToolkit: OpenGLNativeToolkit,
    shaderLoader: ShaderLoader
) : Shader(activeToolkit, shaderLoader) {

    @JvmField var activeShaderMapping: ShaderMappingGL? = null
    @JvmField var programHandle: Int = 0

    override fun loadMapping(toolkit: AbstractNativeToolkit, shaderMapping: NativeShaderMapping): ShaderMapping {
        return ShaderMappingGL(toolkit as OpenGLNativeToolkit, this, shaderMapping)
    }

    override fun loadProgram(mapping: ShaderMapping): Boolean {
        if (mapping === activeShaderMapping) {
            return true
        }
        if (!mapping.createShaders()) {
            return false
        }
        val setProgramToManipulate = isActiveMapping()
        activeShaderMapping = mapping as ShaderMappingGL
        occupyingSlot = getMappingSlot(mapping)
        if (occupyingSlot == -1) {
            throw IllegalArgumentException()
        }
        programHandle = activeShaderMapping!!.programHandle
        if (setProgramToManipulate) {
            OpenGL.glUseProgram(programHandle)
            activeToolkit.activeShaderMapping = activeShaderMapping
        }
        return true
    }

    override fun decode(shaderVariableDecoder: ShaderVariableDecoder): ShaderAttribute {
        return ShaderAttributeGL(this, shaderVariableDecoder)
    }

    override fun reset() {
    }

    override fun isActiveMapping(): Boolean {
        return activeToolkit.activeShaderMapping === activeShaderMapping
    }

    @Suppress("removal", "DEPRECATION")
    @Deprecated("Deprecated in Java")
    protected fun finalize() {
        dispose()
    }

    override fun dispose() {
        for (slot in 0 until getMappingCount()) {
            getMapping(slot).deallocate()
        }
        super.dispose()
    }

    override fun setActiveMapping() {
        if (activeToolkit.activeShaderMapping !== activeShaderMapping) {
            if (activeShaderMapping == null) {
                throw ShaderHardwareException()
            }
            OpenGL.glUseProgram(programHandle)
            activeToolkit.activeShaderMapping = activeShaderMapping
        }
    }
}
