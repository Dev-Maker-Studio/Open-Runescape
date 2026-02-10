package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.imageproducer.IntegratedGraphicsDisplayGL
import jaggl.OpenGL

class OpenGlCubeTextureGenerator(
    @JvmField val openglToolkit: OpenGlToolkit,
    i_2: Int
) : AbstractOpenGlCubeTextureGenerator() {

    @JvmField val cubeTexture: OpenGlArbCubeMapTexture = OpenGlArbCubeMapTexture(
        openglToolkit,
        GLFormat.HDR,
        GLActionFormat.PIXEL_FORMAT,
        i_2
    )

    override fun getCubeTexture(): OpenGlArbCubeMapTexture {
        return cubeTexture
    }

    fun drawQuad(class137Sub21: OpenGlArbCubeMapTexture, class137Sub22: OpenGlArbCubeMapTexture, alpha: Float): Boolean {
        var framebuffersReady = true
        val display = openglToolkit.integratedGraphicsImageProducer!!
        openglToolkit.getClipping(CLIPPINGS)
        openglToolkit.resetClipping()
        openglToolkit.setOrthoMatrixMode()
        OpenGL.glMatrixMode(GL_PROJECTION)
        OpenGL.glLoadIdentity()
        OpenGL.glOrtho(0.0, 1.0, 0.0, 1.0, -1.0, 1.0)
        OpenGL.glMatrixMode(GL_MODELVIEW)
        OpenGL.glLoadIdentity()
        OpenGL.glPushAttrib(GL_VIEWPORT_BIT)
        OpenGL.glViewport(0, 0, cubeTexture.largestMaterialSize, cubeTexture.largestMaterialSize)
        openglToolkit.toggleFog(false)
        openglToolkit.setLightingEnabled(false)
        openglToolkit.toggleZbuffer(false)
        openglToolkit.setDepthMask(false)
        openglToolkit.renderTexture(-2)
        openglToolkit.activateTexture(1)
        openglToolkit.updatePointSizeRange(0.0f, 0.0f, 0.0f, alpha)
        openglToolkit.loadTextureCombineMode(GL_INTERPOLATE, GL_INTERPOLATE)
        openglToolkit.setActiveTexture(class137Sub22)
        openglToolkit.activateTexture(0)
        openglToolkit.loadTextureCombineMode(1)
        openglToolkit.setActiveTexture(class137Sub21)
        openglToolkit.setDisplay(display)

        for (index in 0 until 6) {
            val side = index + GL_TEXTURE_CUBE_MAP_POSITIVE_X
            display.setFramebuffer(0, cubeTexture.getAttachableTexture(side))
            display.drawBuffer(0)
            if (!display.isFramebufferComplete()) {
                framebuffersReady = false
                break
            }

            OpenGL.glBegin(GL_QUADS)
            when (side) {
                GL_TEXTURE_CUBE_MAP_POSITIVE_X -> {
                    OpenGL.glTexCoord3i(65535, 65534, 65534)
                    OpenGL.glMultiTexCoord3i(33985, 65535, 65534, 65534)
                    OpenGL.glVertex2f(0.0f, 0.0f)
                    OpenGL.glTexCoord3i(65535, 65534, -65534)
                    OpenGL.glMultiTexCoord3i(33985, 65535, 65534, -65534)
                    OpenGL.glVertex2f(1.0f, 0.0f)
                    OpenGL.glTexCoord3i(65535, -65534, -65534)
                    OpenGL.glMultiTexCoord3i(33985, 65535, -65534, -65534)
                    OpenGL.glVertex2f(1.0f, 1.0f)
                    OpenGL.glTexCoord3i(65535, -65534, 65534)
                    OpenGL.glMultiTexCoord3i(33985, 65535, -65534, 65534)
                    OpenGL.glVertex2f(0.0f, 1.0f)
                }
                GL_TEXTURE_CUBE_MAP_NEGATIVE_X -> {
                    OpenGL.glTexCoord3i(-65535, 65534, -65534)
                    OpenGL.glMultiTexCoord3i(33985, -65535, 65534, -65534)
                    OpenGL.glVertex2f(0.0f, 0.0f)
                    OpenGL.glTexCoord3i(-65535, 65534, 65534)
                    OpenGL.glMultiTexCoord3i(33985, -65535, 65534, 65534)
                    OpenGL.glVertex2f(1.0f, 0.0f)
                    OpenGL.glTexCoord3i(-65535, -65534, 65534)
                    OpenGL.glMultiTexCoord3i(33985, -65535, -65534, 65534)
                    OpenGL.glVertex2f(1.0f, 1.0f)
                    OpenGL.glTexCoord3i(-65535, -65534, -65534)
                    OpenGL.glMultiTexCoord3i(33985, -65535, -65534, -65534)
                    OpenGL.glVertex2f(0.0f, 1.0f)
                }
                GL_TEXTURE_CUBE_MAP_POSITIVE_Y -> {
                    OpenGL.glTexCoord3i(-65534, 65535, -65534)
                    OpenGL.glMultiTexCoord3i(33985, -65534, 65535, -65534)
                    OpenGL.glVertex2f(0.0f, 0.0f)
                    OpenGL.glTexCoord3i(65534, 65535, -65534)
                    OpenGL.glMultiTexCoord3i(33985, 65534, 65535, -65534)
                    OpenGL.glVertex2f(1.0f, 0.0f)
                    OpenGL.glTexCoord3i(65534, 65535, 65534)
                    OpenGL.glMultiTexCoord3i(33985, 65534, 65535, 65534)
                    OpenGL.glVertex2f(1.0f, 1.0f)
                    OpenGL.glTexCoord3i(-65534, 65535, 65534)
                    OpenGL.glMultiTexCoord3i(33985, -65534, 65535, 65534)
                    OpenGL.glVertex2f(0.0f, 1.0f)
                }
                GL_TEXTURE_CUBE_MAP_NEGATIVE_Y -> {
                    OpenGL.glTexCoord3i(-65534, -65535, 65534)
                    OpenGL.glMultiTexCoord3i(33985, -65534, -65535, 65534)
                    OpenGL.glVertex2f(0.0f, 0.0f)
                    OpenGL.glTexCoord3i(65534, -65535, 65534)
                    OpenGL.glMultiTexCoord3i(33985, 65534, -65535, 65534)
                    OpenGL.glVertex2f(1.0f, 0.0f)
                    OpenGL.glTexCoord3i(65534, -65535, -65534)
                    OpenGL.glMultiTexCoord3i(33985, 65534, -65535, -65534)
                    OpenGL.glVertex2f(1.0f, 1.0f)
                    OpenGL.glTexCoord3i(-65534, -65535, -65534)
                    OpenGL.glMultiTexCoord3i(33985, -65534, -65535, -65534)
                    OpenGL.glVertex2f(0.0f, 1.0f)
                }
                GL_TEXTURE_CUBE_MAP_POSITIVE_Z -> {
                    OpenGL.glTexCoord3i(-65534, 65534, 65535)
                    OpenGL.glMultiTexCoord3i(33985, -65534, 65534, 65535)
                    OpenGL.glVertex2f(0.0f, 0.0f)
                    OpenGL.glTexCoord3i(65534, 65534, 65535)
                    OpenGL.glMultiTexCoord3i(33985, 65534, 65534, 65535)
                    OpenGL.glVertex2f(1.0f, 0.0f)
                    OpenGL.glTexCoord3i(65534, -65534, 65535)
                    OpenGL.glMultiTexCoord3i(33985, 65534, -65534, 65535)
                    OpenGL.glVertex2f(1.0f, 1.0f)
                    OpenGL.glTexCoord3i(-65534, -65534, 65535)
                    OpenGL.glMultiTexCoord3i(33985, -65534, -65534, 65535)
                    OpenGL.glVertex2f(0.0f, 1.0f)
                }
                GL_TEXTURE_CUBE_MAP_NEGATIVE_Z -> {
                    OpenGL.glTexCoord3i(65534, 65534, -65535)
                    OpenGL.glMultiTexCoord3i(33985, 65534, 65534, -65535)
                    OpenGL.glVertex2f(0.0f, 0.0f)
                    OpenGL.glTexCoord3i(-65534, 65534, -65535)
                    OpenGL.glMultiTexCoord3i(33985, -65534, 65534, -65535)
                    OpenGL.glVertex2f(1.0f, 0.0f)
                    OpenGL.glTexCoord3i(-65534, -65534, -65535)
                    OpenGL.glMultiTexCoord3i(33985, -65534, -65534, -65535)
                    OpenGL.glVertex2f(1.0f, 1.0f)
                    OpenGL.glTexCoord3i(65534, -65534, -65535)
                    OpenGL.glMultiTexCoord3i(33985, 65534, -65534, -65535)
                    OpenGL.glVertex2f(0.0f, 1.0f)
                }
            }

            OpenGL.glEnd()
        }

        display.setFramebuffer(0, null)
        openglToolkit.resizeDisplay(display)
        openglToolkit.activateTexture(1)
        openglToolkit.setActiveTexture(null)
        openglToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
        openglToolkit.activateTexture(0)
        openglToolkit.setActiveTexture(null)
        OpenGL.glPopAttrib()
        openglToolkit.setClipping(CLIPPINGS[0], CLIPPINGS[1], CLIPPINGS[2], CLIPPINGS[3])
        if (framebuffersReady && !openglToolkit.radeonGpu) {
            cubeTexture.generateMipmaps()
        }

        return framebuffersReady
    }

    fun getLargestMaterialSize(): Int {
        return cubeTexture.largestMaterialSize
    }

    companion object {
        @JvmField
        val CLIPPINGS: IntArray = IntArray(4)
    }
}
