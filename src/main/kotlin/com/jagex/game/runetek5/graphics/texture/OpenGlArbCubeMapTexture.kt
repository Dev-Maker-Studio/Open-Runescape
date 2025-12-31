package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.*
import jaggl.OpenGL

/**
 * OpenGL ARB cube map texture for environment mapping.
 */
class OpenGlArbCubeMapTexture : OpenGlTexture {

    @JvmField val largestMaterialSize: Int

    constructor(
        toolkit: OpenGlToolkit,
        format: GLFormat,
        actionFormat: GLActionFormat,
        largestMaterialSize: Int
    ) : super(
        toolkit,
        GL_TEXTURE_CUBE_MAP,
        format,
        actionFormat,
        largestMaterialSize * largestMaterialSize * 6,
        false
    ) {
        this.largestMaterialSize = largestMaterialSize
        activeToolkit.setActiveTexture(this)

        for (index in 0 until 6) {
            OpenGL.glTexImage2Dub(
                index + GL_TEXTURE_CUBE_MAP_POSITIVE_X,
                0,
                OpenGlToolkit.getInternalTextureFormat(aClass1501650, aClass761651),
                largestMaterialSize,
                largestMaterialSize,
                0,
                OpenGlToolkit.format(aClass1501650),
                GL_UNSIGNED_BYTE,
                null,
                0
            )
        }

        setLinearTextureFilter(true)
    }

    constructor(
        toolkit: OpenGlToolkit,
        format: GLFormat,
        actionFormat: GLActionFormat,
        largestMaterialSize: Int,
        mipmapping: Boolean,
        pixelsForTextures: Array<IntArray>
    ) : super(
        toolkit,
        GL_TEXTURE_CUBE_MAP,
        format,
        actionFormat,
        largestMaterialSize * largestMaterialSize * 6,
        mipmapping
    ) {
        this.largestMaterialSize = largestMaterialSize
        activeToolkit.setActiveTexture(this)
        if (mipmapping) {
            for (index in 0 until 6) {
                createMipmappedCubeTexture(
                    index + GL_TEXTURE_CUBE_MAP_POSITIVE_X,
                    OpenGlToolkit.getInternalTextureFormat(aClass1501650, aClass761651),
                    largestMaterialSize,
                    largestMaterialSize,
                    activeToolkit.pixelType,
                    pixelsForTextures[index]
                )
            }
        } else {
            for (index in 0 until 6) {
                OpenGL.glTexImage2Di(
                    index + GL_TEXTURE_CUBE_MAP_POSITIVE_X,
                    0,
                    OpenGlToolkit.getInternalTextureFormat(aClass1501650, aClass761651),
                    largestMaterialSize,
                    largestMaterialSize,
                    0,
                    GL_BGRA,
                    activeToolkit.pixelType,
                    pixelsForTextures[index],
                    0
                )
            }
        }

        setLinearTextureFilter(true)
    }

    constructor(
        toolkit: OpenGlToolkit,
        format: GLFormat,
        actionFormat: GLActionFormat,
        data: Array<ByteArray>,
        formatB: GLFormat
    ) : super(toolkit, GL_TEXTURE_CUBE_MAP, format, actionFormat, 24576, false) {
        this.largestMaterialSize = 64
        activeToolkit.setActiveTexture(this)

        for (index in 0 until 6) {
            OpenGL.glTexImage2Dub(
                index + GL_TEXTURE_CUBE_MAP_POSITIVE_X,
                0,
                OpenGlToolkit.getInternalTextureFormat(aClass1501650, aClass761651),
                64,
                64,
                0,
                OpenGlToolkit.format(formatB),
                GL_UNSIGNED_BYTE,
                data[index],
                0
            )
        }
        setLinearTextureFilter(true)
    }

    fun getAttachableTexture(faceIndex: Int): FrameBuffer {
        return OpenGlAttachableCubeMapTexture(this, faceIndex)
    }
}
