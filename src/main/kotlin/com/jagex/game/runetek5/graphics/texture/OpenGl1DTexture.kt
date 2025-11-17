package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*
import jaggl.OpenGL

class OpenGl1DTexture(
    toolkit: OpenGlToolkit,
    controllerA: GLFormat,
    format: GLActionFormat,
    data: ByteArray,
    controllerB: GLFormat
) : OpenGlTexture(toolkit, GL_TEXTURE_1D, controllerA, format, 2, false) {

    @JvmField val width: Int = 2

    init {
        super.activeToolkit.setActiveTexture(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        OpenGL.glTexImage1Dub(super.textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(super.aClass1501650, super.aClass761651), this.width, 0, OpenGlToolkit.format(controllerB), GL_UNSIGNED_BYTE, data, 0)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
        setLinearTextureFilter(true)
    }

    fun setActive() {
        super.activeToolkit.setActiveTexture(this)
        OpenGL.glTexParameteri(super.textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    }
}
