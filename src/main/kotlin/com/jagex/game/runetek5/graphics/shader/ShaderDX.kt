package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.graphics.shader.program.impl.NativeShaderMapping
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit

/**
 * DirectX shader implementation.
 *
 * @author Steven - 17/09/2023
 */
class ShaderDX(
    @JvmField val toolkit: DirectXNativeToolkit,
    shaderLoader: ShaderLoader
) : Shader(toolkit, shaderLoader) {

    @JvmField var active: Boolean = false
    @JvmField var activeDxShaderMapping: ShaderMappingDX? = null

    override fun loadMapping(toolkit: AbstractNativeToolkit, shaderMapping: NativeShaderMapping): ShaderMapping {
        return ShaderMappingDX(toolkit as DirectXNativeToolkit, this, shaderMapping)
    }

    override fun loadProgram(shaderMapping: ShaderMapping): Boolean {
        if (shaderMapping === this.activeDxShaderMapping) {
            return true
        }
        if (!shaderMapping.createShaders()) {
            return false
        }
        this.activeDxShaderMapping = shaderMapping as ShaderMappingDX
        occupyingSlot = getMappingSlot(shaderMapping)
        if (occupyingSlot == -1) {
            throw IllegalArgumentException()
        }
        if (active) {
            toolkit.setVertexShader(this.activeDxShaderMapping!!.vertexShaderHandle)
            toolkit.setPixelShader(this.activeDxShaderMapping!!.fragmentShaderHandle)
            toolkit.activeShaderMapping = this.activeDxShaderMapping!!
        }
        return true
    }

    override fun isActiveMapping(): Boolean {
        return toolkit.activeShaderMapping === shaderMapping
    }

    override fun reset() {
        toolkit.setVertexShader(0L)
        toolkit.setPixelShader(0L)
        active = false
        toolkit.activeShaderMapping = null
        toolkit.setTextureSlot(1)
        toolkit.setTextureSampler(null)
        toolkit.setTextureSlot(0)
        toolkit.setTextureSampler(null)
    }

    override fun decode(shaderVariableDecoder: ShaderVariableDecoder): ShaderAttribute {
        return ShaderAttributeDX(this, shaderVariableDecoder)
    }

    override fun setActiveMapping() {
        if (shaderMapping == null) {
            throw ShaderHardwareException()
        }
        toolkit.setVertexShader((this.activeDxShaderMapping as ShaderMappingDX).vertexShaderHandle)
        toolkit.setPixelShader((this.activeDxShaderMapping as ShaderMappingDX).fragmentShaderHandle)
        toolkit.activeShaderMapping = shaderMapping as ShaderMappingDX
        active = true
    }
}
