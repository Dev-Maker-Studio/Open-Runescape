package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.graphics.texture.sampler.CubeTextureSampler
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit

/**
 * Generates cube textures for native (non-OpenGL) rendering toolkits.
 */
class NativeCubeTextureGenerator(
    @JvmField val nativeToolkit: AbstractNativeToolkit,
    @JvmField val material1: Int,
    @JvmField val material2: Int,
    @JvmField val material3: Int,
    @JvmField val material4: Int,
    @JvmField val material5: Int,
    @JvmField val material6: Int
) : AbstractNativeCubeTextureGenerator() {

    @JvmField var nativeCubeTexture: CubeTextureSampler? = null

    override fun getNativeCubeTexture(): CubeTextureSampler {
        if (nativeCubeTexture == null) {
            val materialProvider = nativeToolkit.materialProvider
            MATERIAL_IDS[0] = material5
            MATERIAL_IDS[1] = material6
            MATERIAL_IDS[2] = material1
            MATERIAL_IDS[3] = material2
            MATERIAL_IDS[4] = material3
            MATERIAL_IDS[5] = material4
            var mipmappingEnabled = false
            var largestSize = 0

            for (index in 0 until 6) {
                if (!materialProvider.isCached(MATERIAL_IDS[index])) {
                    return nativeCubeTexture!!
                }

                val material = materialProvider.getMaterial(MATERIAL_IDS[index])
                val size = if (material.isSmall) 64 else 128
                if (size > largestSize) {
                    largestSize = size
                }

                if (material.mipmapping.toInt() > 0) {
                    mipmappingEnabled = true
                }
            }

            for (index in 0 until 6) {
                MATERIAL_PIXELS[index] = materialProvider.getPixels(MATERIAL_IDS[index], 1.0f, largestSize, largestSize, false)
            }

            nativeCubeTexture = nativeToolkit.generateNativeCubeTexture(largestSize, mipmappingEnabled, MATERIAL_PIXELS)
        }

        return nativeCubeTexture!!
    }

    companion object {
        @JvmField val MATERIAL_IDS = IntArray(6)
        @JvmField val MATERIAL_PIXELS = Array<IntArray?>(6) { null }
    }
}
