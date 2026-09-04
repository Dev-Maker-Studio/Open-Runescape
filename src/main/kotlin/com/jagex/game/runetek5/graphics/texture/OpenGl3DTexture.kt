package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.GL_TEXTURE_3D
import com.jagex.game.runetek5.toolkit.opengl.GL_TEXTURE_BIT
import com.jagex.game.runetek5.toolkit.opengl.GL_UNPACK_ALIGNMENT
import com.jagex.game.runetek5.toolkit.opengl.GL_UNSIGNED_BYTE
import jaggl.OpenGL

class OpenGl3DTexture : OpenGlTexture {

    @JvmField val width: Int
    @JvmField val height: Int
    @JvmField val depth: Int

    constructor(toolkit: OpenGlToolkit, internalFormat: GLFormat, actionFormat: GLActionFormat) : super(toolkit, GL_TEXTURE_3D, internalFormat, actionFormat, 262144, false) {
        this.width = 128
        this.height = 128
        this.depth = 16
        this.activeToolkit.setActiveTexture(this)
        OpenGL.glTexImage3Dub(this.textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(this.internalFormat, this.actionFormat), this.width, this.height, this.depth, 0, OpenGlToolkit.format(this.internalFormat), GL_UNSIGNED_BYTE, null, 0)
        setLinearTextureFilter(true)
    }

    constructor(toolkit: OpenGlToolkit, internalFormat: GLFormat, actionFormat: GLActionFormat, data: ByteArray, pixelFormat: GLFormat) : super(toolkit, GL_TEXTURE_3D, internalFormat, actionFormat, GL_TEXTURE_BIT, false) {
        this.width = 128
        this.height = 128
        this.depth = 16
        this.activeToolkit.setActiveTexture(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        OpenGL.glTexImage3Dub(this.textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(this.internalFormat, this.actionFormat), this.width, this.height, this.depth, 0, OpenGlToolkit.format(pixelFormat), GL_UNSIGNED_BYTE, data, 0)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
        setLinearTextureFilter(true)
    }

    fun copy(depth: Int, width: Int, height: Int) {
        this.activeToolkit.setActiveTexture(this)
        OpenGL.glCopyTexSubImage3D(this.textureTarget, 0, 0, 0, depth, 0, 0, width, height)
        OpenGL.glFlush()
    }
}
