package com.jagex.game.runetek5.graphics.texture.water

import kotlin.math.pow

/**
 * Water texture generator with wave simulation.
 */
internal open class Class370_Sub1 : WaterTextureGenerator(8, 8, 8) {

    @JvmField var aByteArray7752: ByteArray? = null
    @JvmField var intensity: Int = 0
    @JvmField var displacement: Int = 0
    @JvmField var attenuation: Int = 0
    @JvmField var velocity: Int = 0
    @JvmField var turbulence: Int = 0
    @JvmField val initialIntensity: Int
    @JvmField val attenuationFactor: Int
    @JvmField val displacementFactor: Int

    init {
        initialIntensity = (3.0f * 4096.0f).toInt()
        attenuationFactor = (0.55f * 4096.0f).toInt()
        velocity = (0.5.pow(-0.1) * 4096.0).toInt()
        displacementFactor = velocity
    }

    override fun update(i_1: Int, i_2: Int) {
        if (i_1 != 0) {
            intensity = displacement * initialIntensity shr 12
            if (intensity < 0) {
                intensity = 0
            } else if (intensity > 4096) {
                intensity = 4096
            }

            displacement = attenuationFactor - if (i_2 < 0) -i_2 else i_2
            displacement = displacement * displacement shr 12
            displacement = displacement * intensity shr 12
            attenuation += displacement * velocity shr 12
            velocity = velocity * displacementFactor shr 12
        } else {
            displacement = attenuationFactor - if (i_2 < 0) -i_2 else i_2
            displacement = displacement * displacement shr 12
            intensity = 4096
            attenuation = displacement
        }
    }

    open fun set(index: Int, value: Byte) {
        aByteArray7752!![index] = value
    }

    override fun reset() {
        velocity = displacementFactor
        attenuation = attenuation shr 4
        if (attenuation < 0) {
            attenuation = 0
        } else if (attenuation > 255) {
            attenuation = 255
        }

        set(turbulence++, attenuation.toByte())
        attenuation = 0
    }

    override fun clear() {
        turbulence = 0
        attenuation = 0
    }
}
