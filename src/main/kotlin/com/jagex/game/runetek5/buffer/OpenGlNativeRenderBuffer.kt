package com.jagex.game.runetek5.buffer

import com.jagex.game.runetek5.graphics.texture.TextureFramebufferGenerator
import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import jaggl.OpenGL

/**
 * OpenGL native render buffer for texture framebuffer generation.
 */
class OpenGlNativeRenderBuffer(
    toolkit: OpenGLNativeToolkit,
    format: GLFormat,
    actionFormat: GLActionFormat,
    width: Int,
    height: Int
) : TextureFramebufferGenerator, AutoCloseable {

    @JvmField var handle: Int
    @JvmField val aClass1503880: GLFormat = format
    @JvmField val aClass763878: GLActionFormat = actionFormat
    @JvmField val useWidth: Int = width
    @JvmField val useHeight: Int = height
    @JvmField val aGraphicalRendererSub2Sub13881: OpenGLNativeToolkit = toolkit

    init {
        val bufferHandle = IntArray(1)
        OpenGL.glGenRenderbuffersEXT(1, bufferHandle, 0)
        this.handle = bufferHandle[0]
        OpenGL.glBindRenderbufferEXT(36161, this.handle)
        OpenGL.glRenderbufferStorageEXT(
            36161,
            OpenGLNativeToolkit.texture2dInternalFormat(this.aClass1503880, this.aClass763878),
            width,
            height
        )
    }

    override fun close() {
        method5931()
    }

    override fun getUseWidth(): Int {
        return useWidth
    }

    override fun getUseHeight(): Int {
        return useHeight
    }

    @JvmName("method5931")
    fun method5931() {
        if (handle > 0) {
            aGraphicalRendererSub2Sub13881.requestRenderBufferDeletion(
                handle,
                aClass763878.bytesPerEntry * aClass1503880.bytesPerPixel * useWidth * useHeight
            )
            handle = 0
        }
    }

    override fun deallocate() {
        if (handle > 0) {
            val bufferHandle = intArrayOf(handle)
            OpenGL.glDeleteRenderbuffersEXT(1, bufferHandle, 0)
            handle = 0
        }
    }

    override fun generateTextureFramebuffer(attachmentPoint: Int) {
        OpenGL.glFramebufferRenderbufferEXT(36160, attachmentPoint, 36161, handle)
    }
}
