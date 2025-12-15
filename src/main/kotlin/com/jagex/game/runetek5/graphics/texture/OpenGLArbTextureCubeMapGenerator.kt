package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat

/**
 * Generates OpenGL ARB cube map textures from material IDs.
 */
class OpenGLArbTextureCubeMapGenerator(
    @JvmField val openglToolkit: OpenGlToolkit,
    @JvmField val materialAtPositiveX: Int,
    @JvmField val materialAtNegativeX: Int,
    @JvmField val materialAtPositiveY: Int,
    @JvmField val materialAtNegativeY: Int,
    @JvmField val materialAtPositiveZ: Int,
    @JvmField val materialAtNegativeZ: Int
) : AbstractOpenGlCubeTextureGenerator() {

    @JvmField var arbCubeMapTexture: OpenGlArbCubeMapTexture? = null

    override fun getCubeTexture(): OpenGlArbCubeMapTexture {
        if (arbCubeMapTexture == null) {
            val materialProvider = openglToolkit.materialProvider
            MATERIAL_BUFFER[0] = materialAtPositiveX
            MATERIAL_BUFFER[1] = materialAtNegativeX
            MATERIAL_BUFFER[2] = materialAtPositiveY
            MATERIAL_BUFFER[3] = materialAtNegativeY
            MATERIAL_BUFFER[4] = materialAtPositiveZ
            MATERIAL_BUFFER[5] = materialAtNegativeZ
            var mipmap = false
            var largestMaterialSize = 0

            for (index in 0 until 6) {
                if (!materialProvider!!.isCached(MATERIAL_BUFFER[index])) {
                    return arbCubeMapTexture!!
                }

                val material = materialProvider!!.getMaterial(MATERIAL_BUFFER[index])
                val size = if (material.isSmall) 64 else 128
                if (size > largestMaterialSize) {
                    largestMaterialSize = size
                }

                if (material.mipmapping.toInt() > 0) {
                    mipmap = true
                }
            }

            val pixelArrays = Array(6) { index ->
                openglToolkit.materialProvider!!.getPixels(MATERIAL_BUFFER[index], 1.0f, largestMaterialSize, largestMaterialSize, false)
            }

            arbCubeMapTexture = OpenGlArbCubeMapTexture(
                openglToolkit,
                GLFormat.aClass1501950,
                GLActionFormat.PIXEL_FORMAT,
                largestMaterialSize,
                mipmap,
                pixelArrays
            )
        }

        return arbCubeMapTexture!!
    }

    companion object {
        @JvmField val MATERIAL_BUFFER = IntArray(6)
    }
}
