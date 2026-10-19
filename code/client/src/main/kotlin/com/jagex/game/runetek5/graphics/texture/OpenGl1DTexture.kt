package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.*
import jaggl.OpenGL

class OpenGl1DTexture(
    toolkit: OpenGlToolkit,
    internalFormat: GLFormat,
    format: GLActionFormat,
    data: ByteArray,
    pixelFormat: GLFormat
) : OpenGlTexture(toolkit, GL_TEXTURE_1D, internalFormat, format, 2, false) {

    @JvmField val width: Int = 2

    init {
        super.activeToolkit.setActiveTexture(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        OpenGL.glTexImage1Dub(super.textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(super.internalFormat, super.actionFormat), this.width, 0, OpenGlToolkit.format(pixelFormat), GL_UNSIGNED_BYTE, data, 0)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
        setLinearTextureFilter(true)
    }

    fun setActive() {
        super.activeToolkit.setActiveTexture(this)
        OpenGL.glTexParameteri(super.textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    }
}
