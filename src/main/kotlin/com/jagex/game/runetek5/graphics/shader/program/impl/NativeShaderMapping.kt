package com.jagex.game.runetek5.graphics.shader.program.impl

import com.jagex.game.runetek5.graphics.shader.ShaderBuffer
import com.jagex.game.runetek5.graphics.shader.ShaderVariable

/**
 * Native shader mapping configuration.
 *
 * Maps shader names to vertex and fragment shader names with their variables.
 *
 * Content table:
 *
 * shader_name: Unlit,                         vertex_shader_name: Model.vertex.vs_2_0.0000000000000003,    fragment_shader_name: Model.fragment.ps_2_0.000000003
 * shader_name: Unlit_IgnoreAlpha,             vertex_shader_name: Model.vertex.vs_2_0.0000000000000003,    fragment_shader_name: Model.fragment.ps_2_0.100000003
 * shader_name: Standard_0PointLights,         vertex_shader_name: Model.vertex.vs_2_0.0000000000000000,    fragment_shader_name: Model.fragment.ps_2_0.000000000
 * shader_name: Standard_1PointLights,         vertex_shader_name: Model.vertex.vs_2_0.0000000100000000,    fragment_shader_name: Model.fragment.ps_2_0.000000000
 * shader_name: Standard_2PointLights,         vertex_shader_name: Model.vertex.vs_2_0.0000000200000000,    fragment_shader_name: Model.fragment.ps_2_0.000000000
 * shader_name: Standard_3PointLights,         vertex_shader_name: Model.vertex.vs_2_0.0000000300000000,    fragment_shader_name: Model.fragment.ps_2_0.000000000
 * shader_name: Standard_4PointLights,         vertex_shader_name: Model.vertex.vs_2_0.0000000400000000,    fragment_shader_name: Model.fragment.ps_2_0.000000000
 * shader_name: Specular_0PointLights,         vertex_shader_name: Model.vertex.vs_2_0.0000000000000001,    fragment_shader_name: Model.fragment.ps_2_0.000000001
 * shader_name: Specular_1PointLights,         vertex_shader_name: Model.vertex.vs_2_0.0000000100000001,    fragment_shader_name: Model.fragment.ps_2_0.000000001
 * shader_name: Specular_2PointLights,         vertex_shader_name: Model.vertex.vs_2_0.0000000200000001,    fragment_shader_name: Model.fragment.ps_2_0.000000001
 * shader_name: Specular_3PointLights,         vertex_shader_name: Model.vertex.vs_2_0.0000000300000001,    fragment_shader_name: Model.fragment.ps_2_0.000000001
 * shader_name: Specular_4PointLights,         vertex_shader_name: Model.vertex.vs_2_0.0000000400000001,    fragment_shader_name: Model.fragment.ps_2_0.000000001
 * shader_name: EnvironmentalMapping_0PointLights, vertex_shader_name: Model.vertex.vs_2_0.0000000000000002, fragment_shader_name: Model.fragment.ps_2_0.000000002
 * shader_name: EnvironmentalMapping_1PointLights, vertex_shader_name: Model.vertex.vs_2_0.0000000100000002, fragment_shader_name: Model.fragment.ps_2_0.000000002
 * shader_name: EnvironmentalMapping_2PointLights, vertex_shader_name: Model.vertex.vs_2_0.0000000200000002, fragment_shader_name: Model.fragment.ps_2_0.000000002
 * shader_name: EnvironmentalMapping_3PointLights, vertex_shader_name: Model.vertex.vs_2_0.0000000300000002, fragment_shader_name: Model.fragment.ps_2_0.000000002
 * shader_name: EnvironmentalMapping_4PointLights, vertex_shader_name: Model.vertex.vs_2_0.0000000400000002, fragment_shader_name: Model.fragment.ps_2_0.000000002
 * shader_name: UnderwaterGround,              vertex_shader_name: Model.vertex.vs_2_0.0000000000000004,    fragment_shader_name: Model.fragment.ps_2_0.000000004
 * shader_name: Normal,                        vertex_shader_name: Sprite.vertex.vs_2_0.0,                  fragment_shader_name: Sprite.fragment.ps_2_0.0
 * shader_name: Masked,                        vertex_shader_name: Sprite.vertex.vs_2_0.1,                  fragment_shader_name: Sprite.fragment.ps_2_0.1
 * shader_name: AlphaTex,                      vertex_shader_name: Sprite.vertex.vs_2_0.0,                  fragment_shader_name: Sprite.fragment.ps_2_0.2
 * shader_name: Particle,                      vertex_shader_name: Particle.vertex.vs_2_0,                  fragment_shader_name: Particle.fragment.ps_2_0
 * shader_name: EnvMappedSea,                  vertex_shader_name: EnvMappedWater.vertex.vs_2_0,            fragment_shader_name: EnvMappedWater.fragment.ps_2_0.1
 * shader_name: EnvMappedWater,                vertex_shader_name: EnvMappedWater.vertex.vs_2_0,            fragment_shader_name: EnvMappedWater.fragment.ps_2_0.0
 * shader_name: Waterfall3D,                   vertex_shader_name: Waterfall.vertex.vs_2_0,                 fragment_shader_name: Waterfall.fragment3D.ps_2_0
 * shader_name: Waterfall2D,                   vertex_shader_name: Waterfall.vertex.vs_2_0,                 fragment_shader_name: Waterfall.fragment2D.ps_2_0
 * shader_name: techFullscreenTri,             vertex_shader_name: FilterBloom.Filter_vertexD3D.vs_2_0,     fragment_shader_name: FilterBloom.passThrough.ps_2_0
 * shader_name: techDefault,                   vertex_shader_name: FilterBloom.Filter_vertexD3D.vs_2_0,     fragment_shader_name: FilterBloom.passThrough.ps_2_0
 * shader_name: brightpass,                    vertex_shader_name: FilterBloom.Filter_vertexD3D.vs_2_0,     fragment_shader_name: FilterBloom.brightpass.ps_2_0
 * shader_name: blur,                          vertex_shader_name: FilterBloom.Filter_vertexD3D.vs_3_0,     fragment_shader_name: FilterBloom.blur.ps_3_0
 * shader_name: composite,                     vertex_shader_name: FilterBloom.Filter_vertexD3D.vs_2_0,     fragment_shader_name: FilterBloom.composite.ps_2_0
 * shader_name: test,                          vertex_shader_name: FilterBloom.Filter_vertexD3D.vs_2_0,     fragment_shader_name: FilterBloom.test.ps_2_0
 */
class NativeShaderMapping {

    @JvmField var mappingName: String? = null
    @JvmField var vertexShaderName: String? = null
    @JvmField var fragmentShaderName: String? = null
    @JvmField var vertexShaderVariables: Array<ShaderVariable?>? = null
    @JvmField var fragmentShaderVariables: Array<ShaderVariable?>? = null

    /**
     * Decodes shader mapping data from a shader buffer.
     *
     * @param shaderBuffer The shader buffer
     */
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
