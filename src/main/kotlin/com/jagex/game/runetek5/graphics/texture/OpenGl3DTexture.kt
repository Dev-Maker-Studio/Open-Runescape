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

    constructor(class505Sub11: OpenGlToolkit, class1502: GLFormat, class763: GLActionFormat) : super(class505Sub11, GL_TEXTURE_3D, class1502, class763, 262144, false) {
        this.width = 128
        this.height = 128
        this.depth = 16
        this.activeToolkit.setActiveTexture(this)
        OpenGL.glTexImage3Dub(this.textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), this.width, this.height, this.depth, 0, OpenGlToolkit.format(this.aClass1501650), GL_UNSIGNED_BYTE, null, 0)
        setLinearTextureFilter(true)
    }

    constructor(class505Sub11: OpenGlToolkit, class1502: GLFormat, class763: GLActionFormat, bytes_7: ByteArray, class1508: GLFormat) : super(class505Sub11, GL_TEXTURE_3D, class1502, class763, GL_TEXTURE_BIT, false) {
        this.width = 128
        this.height = 128
        this.depth = 16
        this.activeToolkit.setActiveTexture(this)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        OpenGL.glTexImage3Dub(this.textureTarget, 0, OpenGlToolkit.getInternalTextureFormat(this.aClass1501650, this.aClass761651), this.width, this.height, this.depth, 0, OpenGlToolkit.format(class1508), GL_UNSIGNED_BYTE, bytes_7, 0)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
        setLinearTextureFilter(true)
    }

    fun copy(depth: Int, width: Int, height: Int) {
        this.activeToolkit.setActiveTexture(this)
        OpenGL.glCopyTexSubImage3D(this.textureTarget, 0, 0, 0, depth, 0, 0, width, height)
        OpenGL.glFlush()
    }
}
