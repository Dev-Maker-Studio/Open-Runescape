package com.jagex.game.runetek5.toolkit

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat

/**
 * A component surface that wraps a native texture sampler, used for masked or custom-shaped textures.
 */
class MaskedTextureSurface : ComponentSurface {

    @JvmField val nativeTextureSampler: NativeTextureSampler

    /**
     * Creates a masked texture surface from byte data.
     *
     * @param nativeToolkit The toolkit for texture creation
     * @param width Texture width
     * @param height Texture height
     * @param bytes Pixel data as bytes
     */
    constructor(nativeToolkit: AbstractNativeToolkit, width: Int, height: Int, bytes: ByteArray) {
        nativeTextureSampler = nativeToolkit.createTextureSampler(GLFormat.ALPHA, width, height, false, bytes)
        nativeTextureSampler.setRepeat(false, false)
    }

    /**
     * Creates a masked texture surface from integer pixel data.
     *
     * @param nativeToolkit The toolkit for texture creation
     * @param width Texture width
     * @param height Texture height
     * @param pixels Pixel data as integers
     */
    constructor(nativeToolkit: AbstractNativeToolkit, width: Int, height: Int, pixels: IntArray) {
        nativeTextureSampler = nativeToolkit.createTexture(width, height, false, pixels)
        nativeTextureSampler.setRepeat(false, false)
    }

    companion object {
        /**
         * Creates a masked texture surface from row-based mask data.
         * Each row can have a different horizontal offset and width, allowing for custom shapes.
         *
         * @param nativeToolkit The toolkit for texture creation
         * @param width Total texture width
         * @param height Total texture height (number of rows)
         * @param rowOffsets Horizontal offset for each row
         * @param rowWidths Width of the masked region for each row
         * @return A new MaskedTextureSurface with the specified mask pattern
         */
        @JvmStatic
        fun createFromRowMask(
            nativeToolkit: AbstractNativeToolkit,
            width: Int,
            height: Int,
            rowOffsets: IntArray,
            rowWidths: IntArray
        ): MaskedTextureSurface {
            return if (nativeToolkit.supportsTextureFormat(GLFormat.ALPHA, GLActionFormat.PIXEL_FORMAT)) {
                val bytes = ByteArray(height * width)
                for (row in 0 until height) {
                    var pixelPtr = rowOffsets[row] + row * width
                    for (col in 0 until rowWidths[row]) {
                        bytes[pixelPtr++] = -1
                    }
                }
                MaskedTextureSurface(nativeToolkit, width, height, bytes)
            } else {
                val pixels = IntArray(height * width)
                for (row in 0 until height) {
                    var pixelPtr = rowOffsets[row] + row * width
                    for (col in 0 until rowWidths[row]) {
                        pixels[pixelPtr++] = 0xffffff.inv()
                    }
                }
                MaskedTextureSurface(nativeToolkit, width, height, pixels)
            }
        }
    }
}
