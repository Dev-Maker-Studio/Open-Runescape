package com.jagex.game.runetek5.toolkit

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.texture.OpenGl2DImageTexture
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat

class SpriteSurfaceGL(
    class505Sub11: OpenGlToolkit,
    width: Int,
    length: Int,
    pixels: ByteArray
) : ComponentSurface() {

    @JvmField val surface: OpenGl2DImageTexture

    init {
        this.surface = OpenGl2DImageTexture.createSurface(class505Sub11, GLFormat.ALPHA, GLActionFormat.PIXEL_FORMAT, width, length, pixels, GLFormat.ALPHA)
        this.surface.setRepeat(false, false)
    }

    companion object {
        @JvmStatic
        fun create(toolkit: OpenGlToolkit, width: Int, length: Int, rowStartX: IntArray, rowWidth: IntArray): SpriteSurfaceGL {
            val pixels = ByteArray(length * width)
            for (y in 0 until length) {
                var startX = rowStartX[y] + y * width
                for (i in 0 until rowWidth[y]) {
                    pixels[startX++] = -1
                }
            }
            return SpriteSurfaceGL(toolkit, width, length, pixels)
        }
    }
}
