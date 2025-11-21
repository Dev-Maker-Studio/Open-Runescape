package com.jagex.game.runetek5.graphics.shader.program

import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.graphics.texture.sampler.CubeTextureSampler
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.math.Vector4
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit

/**
 * Abstract shader for environment-mapped rendering with lighting.
 */
abstract class AbstractEnvironmentShader(
    @JvmField protected val activeToolkit: AbstractNativeToolkit
) {

    @JvmField var anInterface31451: CubeTextureSampler? = null
    @JvmField var textureSampler: NativeTextureSampler? = null
    @JvmField var anInt467: Int = 0
    @JvmField var anInt468: Int = 0
    @JvmField var anInt469: Int = 0
    @JvmField var anInt470: Int = 0
    @JvmField val pointLightsDiffuseColor = FloatArray(16)
    @JvmField val pointLightsPosAndRadiusSq = FloatArray(16)
    @JvmField val textureCoordinateMatrix = Matrix44()
    @JvmField val eyePos = Vector3()
    @JvmField val heightFogPlane = Vector4()
    @JvmField val heightFogColor = Vector3()
    @JvmField val distanceFogPlane = Vector4()
    @JvmField val distanceFogColor = Vector3()
    @JvmField val sunPosition = Vector3()
    @JvmField val sunColor = Vector3()
    @JvmField val antiSunColor = Vector3()
    @JvmField val lightColor = Vector3()
    @JvmField val aClass384465 = Matrix44()
    @JvmField protected var aFloat456: Float = 0f
    @JvmField protected var aFloat453: Float = 0f

    /**
     * Identifies and configures material effect parameters.
     *
     * @param material The material type
     */
    fun indentifyMaterialEffect(material: MaterialType) {
        when (material.effectParam1.toInt()) {
            1 -> {
                aFloat456 = 32.0f
                aFloat453 = 0.5f
            }
            2 -> {
                aFloat456 = 4.0f
                aFloat453 = 0.65f
            }
            3 -> {
                aFloat456 = 2.0f
                aFloat453 = 0.8f
            }
        }
    }

    abstract fun loadUnlitProgram(var1: Boolean)

    abstract fun loadSpecularPointlight(var1: Int)

    abstract fun loadPointlightEnvironmentMapping(var1: Int)

    abstract fun loadUnderwaterGround()

    abstract fun loadWorldViewProjection(var1: Matrix44)

    abstract fun loadStandartPointlight(var1: Int)
}
