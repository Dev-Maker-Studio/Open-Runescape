package com.jagex.game.runetek5.graphics.shader.program.impl

import com.jagex.game.runetek5.graphics.shader.Shader
import com.jagex.game.runetek5.graphics.shader.ShaderAttribute
import com.jagex.game.runetek5.graphics.shader.ShaderBuilderException
import com.jagex.game.runetek5.graphics.shader.ShaderMapping
import com.jagex.game.runetek5.graphics.shader.program.AbstractEnvironmentShader
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode

class NativeEnvironmentShaderHd(
    nativeToolkit: AbstractNativeToolkit
) : AbstractEnvironmentShader(nativeToolkit) {

    @JvmField var activeUniformLocation: IntArray? = null
    @JvmField val worldViewProjectionMatrix: Matrix44 = Matrix44()
    @JvmField val mappings: Array<ShaderMapping?> = arrayOfNulls(18)
    @JvmField val uniformLocations: Array<IntArray> = Array(18) { IntArray(17) }
    @JvmField var modelShader: Shader? = null

    init {
        initialize()
    }

    override fun loadUnlitProgram(ignoreAlpha: Boolean) {
        if (ignoreAlpha) {
            modelShader!!.loadProgram(mappings[1]!!)
        } else {
            modelShader!!.loadProgram(mappings[0]!!)
        }

        activeUniformLocation = uniformLocations[modelShader!!.getOccupyingSlot()]
        modelShader!!.setActiveMapping()
        modelShader!!.setDiffuseSampler(activeUniformLocation!![2], 0, textureSampler!!)
        modelShader!!.setWorldViewProjectionMatrix(activeUniformLocation!![4], worldViewProjectionMatrix)
        modelShader!!.setTextureCoordinateMatrix(activeUniformLocation!![5], textureCoordinateMatrix)
        modelShader!!.load4f(activeUniformLocation!![7], heightFogPlane.x, heightFogPlane.y, heightFogPlane.z, heightFogPlane.w)
        modelShader!!.load3f(activeUniformLocation!![8], heightFogColor.x, heightFogColor.y, heightFogColor.z)
        modelShader!!.load4f(activeUniformLocation!![9], distanceFogPlane.x, distanceFogPlane.y, distanceFogPlane.z, distanceFogPlane.w)
        modelShader!!.load3f(activeUniformLocation!![10], distanceFogColor.x, distanceFogColor.y, distanceFogColor.z)
        activeToolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, startVertex, vertexCount, startIndex, primitiveCount)
    }

    override fun loadUnderwaterGround() {
        modelShader!!.loadProgram(mappings[17]!!)
        activeUniformLocation = uniformLocations[modelShader!!.getOccupyingSlot()]
        modelShader!!.setActiveMapping()
        loadUnit(0)
    }

    /**
     *             int slot = shader.get_mapping_slot(mappings[index]);
    uniform_locations[index][2] = DiffuseSampler.get_uniform_location(slot);
    uniform_locations[index][3] = EnvironmentSampler.get_uniform_location(slot);
    uniform_locations[index][1] = PointLightsPosAndRadiusSq.get_uniform_location(slot);
    uniform_locations[index][0] = PointLightsDiffuseColour.get_uniform_location(slot);
    uniform_locations[index][4] = WVPMatrix.get_uniform_location(slot);
    uniform_locations[index][5] = TexCoordMatrix.get_uniform_location(slot);
    uniform_locations[index][7] = HeightFogPlane.get_uniform_location(slot);
    uniform_locations[index][8] = HeightFogColour.get_uniform_location(slot);
    uniform_locations[index][9] = DistanceFogPlane.get_uniform_location(slot);
    uniform_locations[index][10] = DistanceFogColour.get_uniform_location(slot);
    uniform_locations[index][11] = SunDir.get_uniform_location(slot);
    uniform_locations[index][12] = SunColour.get_uniform_location(slot);
    uniform_locations[index][13] = AntiSunColour.get_uniform_location(slot);
    uniform_locations[index][14] = AmbientColour.get_uniform_location(slot);
    uniform_locations[index][6] = EyePos.get_uniform_location(slot);
    uniform_locations[index][15] = WorldMatrix.get_uniform_location(slot);
    uniform_locations[index][16] = SpecularExponent.get_uniform_location(slot);
     * @param unit
     */
    fun loadUnit(unit: Int) {
        modelShader!!.setDiffuseSampler(activeUniformLocation!![2], 0, textureSampler!!)
        modelShader!!.setWorldViewProjectionMatrix(activeUniformLocation!![4], worldViewProjectionMatrix)
        modelShader!!.setTextureCoordinateMatrix(activeUniformLocation!![5], textureCoordinateMatrix)
        modelShader!!.load4f(activeUniformLocation!![7], heightFogPlane.x, heightFogPlane.y, heightFogPlane.z, heightFogPlane.w)
        modelShader!!.load3f(activeUniformLocation!![8], heightFogColor.x, heightFogColor.y, heightFogColor.z)
        modelShader!!.load4f(activeUniformLocation!![9], distanceFogPlane.x, distanceFogPlane.y, distanceFogPlane.z, distanceFogPlane.w)
        modelShader!!.load3f(activeUniformLocation!![10], distanceFogColor.x, distanceFogColor.y, distanceFogColor.z)
        modelShader!!.loadVector3f(activeUniformLocation!![11], sunPosition)
        modelShader!!.loadVector3f(activeUniformLocation!![12], sunColor)
        modelShader!!.loadVector3f(activeUniformLocation!![13], antiSunColor)
        modelShader!!.loadVector3f(activeUniformLocation!![14], lightColor)
        if (unit > 0) {
            modelShader!!.uploadDynamicUniform4fv(activeUniformLocation!![1], pointLightsPosAndRadiusSq, unit * 4)
            modelShader!!.uploadDynamicUniform4fv(activeUniformLocation!![0], pointLightsDiffuseColor, unit * 4)
        }

        activeToolkit.drawIndexedPrimitive(PrimitiveDrawMode.TRIANGLES, startVertex, vertexCount, startIndex, primitiveCount)
    }

    override fun loadStandartPointlight(unit: Int) {
        modelShader!!.loadProgram(mappings[unit + 2]!!)
        activeUniformLocation = uniformLocations[modelShader!!.getOccupyingSlot()]
        modelShader!!.setActiveMapping()
        loadUnit(unit)
    }

    override fun loadSpecularPointlight(unit: Int) {
        modelShader!!.loadProgram(mappings[unit + 7]!!)
        activeUniformLocation = uniformLocations[modelShader!!.getOccupyingSlot()]
        modelShader!!.setActiveMapping()
        modelShader!!.load3f(activeUniformLocation!![6], eyePos.x, eyePos.y, eyePos.z)
        modelShader!!.load4f(activeUniformLocation!![16], specularExponent, specularIntensity, 0.0f, 0.0f)
        loadUnit(unit)
    }

    override fun loadPointlightEnvironmentMapping(unit: Int) {
        modelShader!!.loadProgram(mappings[unit + 12]!!)
        activeUniformLocation = uniformLocations[modelShader!!.getOccupyingSlot()]
        modelShader!!.setActiveMapping()
        modelShader!!.setWorldMatrix(activeUniformLocation!![15], worldMatrix)
        modelShader!!.load3f(activeUniformLocation!![6], eyePos.x, eyePos.y, eyePos.z)
        modelShader!!.setDiffuseSampler(activeUniformLocation!![3], 1, environmentSampler!!)
        loadUnit(unit)
    }

    fun initialize(): Boolean {
        modelShader = activeToolkit.loadShader("Model")
        val DiffuseSampler = modelShader!!.getAttribute("DiffuseSampler")
        val EnvironmentSampler = modelShader!!.getAttribute("EnvironmentSampler")
        val PointLightsPosAndRadiusSq = modelShader!!.getAttribute("PointLightsPosAndRadiusSq")
        val PointLightsDiffuseColour = modelShader!!.getAttribute("PointLightsDiffuseColour")
        val WVPMatrix = modelShader!!.getAttribute("WVPMatrix")
        val TexCoordMatrix = modelShader!!.getAttribute("TexCoordMatrix")
        val HeightFogPlane = modelShader!!.getAttribute("HeightFogPlane")
        val HeightFogColour = modelShader!!.getAttribute("HeightFogColour")
        val DistanceFogPlane = modelShader!!.getAttribute("DistanceFogPlane")
        val DistanceFogColour = modelShader!!.getAttribute("DistanceFogColour")
        val SunDir = modelShader!!.getAttribute("SunDir")
        val SunColour = modelShader!!.getAttribute("SunColour")
        val AntiSunColour = modelShader!!.getAttribute("AntiSunColour")
        val AmbientColour = modelShader!!.getAttribute("AmbientColour")
        val EyePos = modelShader!!.getAttribute("EyePos")
        val SpecularExponent = modelShader!!.getAttribute("SpecularExponent")
        val WorldMatrix = modelShader!!.getAttribute("WorldMatrix")
        mappings[0] = modelShader!!.getShaderMapping("Unlit")
        mappings[1] = modelShader!!.getShaderMapping("Unlit_IgnoreAlpha")
        mappings[17] = modelShader!!.getShaderMapping("UnderwaterGround")
        for (index in 0..4) {
            mappings[index + 2] = modelShader!!.getShaderMapping("Standard_${index}PointLights")
            mappings[index + 7] = modelShader!!.getShaderMapping("Specular_${index}PointLights")
            mappings[index + 12] = modelShader!!.getShaderMapping("EnvironmentalMapping_${index}PointLights")
        }
        for (index in 0 until 18) {
            val slot = modelShader!!.getMappingSlot(mappings[index]!!)
            uniformLocations[index][2] = DiffuseSampler.getUniformLocation(slot)
            uniformLocations[index][3] = EnvironmentSampler.getUniformLocation(slot)
            uniformLocations[index][1] = PointLightsPosAndRadiusSq.getUniformLocation(slot)
            uniformLocations[index][0] = PointLightsDiffuseColour.getUniformLocation(slot)
            uniformLocations[index][4] = WVPMatrix.getUniformLocation(slot)
            uniformLocations[index][5] = TexCoordMatrix.getUniformLocation(slot)
            uniformLocations[index][7] = HeightFogPlane.getUniformLocation(slot)
            uniformLocations[index][8] = HeightFogColour.getUniformLocation(slot)
            uniformLocations[index][9] = DistanceFogPlane.getUniformLocation(slot)
            uniformLocations[index][10] = DistanceFogColour.getUniformLocation(slot)
            uniformLocations[index][11] = SunDir.getUniformLocation(slot)
            uniformLocations[index][12] = SunColour.getUniformLocation(slot)
            uniformLocations[index][13] = AntiSunColour.getUniformLocation(slot)
            uniformLocations[index][14] = AmbientColour.getUniformLocation(slot)
            uniformLocations[index][6] = EyePos.getUniformLocation(slot)
            uniformLocations[index][15] = WorldMatrix.getUniformLocation(slot)
            uniformLocations[index][16] = SpecularExponent.getUniformLocation(slot)
        }
        modelShader!!.loadProgram(mappings[2]!!)
        return true
    }

    override fun loadWorldViewProjection(wvpMatrix: Matrix44) {
        worldViewProjectionMatrix.copyFrom(wvpMatrix)
        worldViewProjectionMatrix.multiplyBy(activeToolkit.frustumClipMatrix)
    }
}
