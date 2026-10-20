package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.imageproducer.IntegratedGraphicsDisplayGL
import jaggl.OpenGL

class OpenGlCubeTextureGenerator(
    @JvmField val openglToolkit: OpenGlToolkit,
    largestMaterialSize: Int
) : AbstractOpenGlCubeTextureGenerator() {

    @JvmField val cubeTexture: OpenGlArbCubeMapTexture = OpenGlArbCubeMapTexture(
        openglToolkit,
        GLFormat.RGBA,
        GLActionFormat.PIXEL_FORMAT,
        largestMaterialSize
    )

    override fun getCubeTexture(): OpenGlArbCubeMapTexture? {
        return cubeTexture
    }

    fun drawQuad(cubeTextureA: OpenGlArbCubeMapTexture, cubeTextureB: OpenGlArbCubeMapTexture, alpha: Float): Boolean {
        var framebuffersReady = true
        val display = openglToolkit.integratedGraphicsImageProducer!!
        openglToolkit.getClipping(CLIPPINGS)
        openglToolkit.resetClipping()
        openglToolkit.setOrthoMatrixMode()
        OpenGL.glViewport(0, 0, cubeTexture.largestMaterialSize, cubeTexture.largestMaterialSize)
        openglToolkit.toggleFog(false)
        openglToolkit.setLightingEnabled(false)
        openglToolkit.toggleZbuffer(false)
        openglToolkit.setDepthMask(false)
        openglToolkit.renderTexture(-2)
        openglToolkit.activateTexture(1)
        openglToolkit.updatePointSizeRange(0.0f, 0.0f, 0.0f, alpha)
        openglToolkit.loadTextureCombineMode(GL_INTERPOLATE, GL_INTERPOLATE)
        openglToolkit.setActiveTexture(cubeTextureB)
        openglToolkit.activateTexture(0)
        openglToolkit.loadTextureCombineMode(1)
        openglToolkit.setActiveTexture(cubeTextureA)
        openglToolkit.setDisplay(display)

        for (index in 0 until 6) {
            val cubeFace = index + GL_TEXTURE_CUBE_MAP_POSITIVE_X
            display.setFramebuffer(0, cubeTexture.getAttachableTexture(cubeFace))
            display.drawBuffer(0)
            if (!display.isFramebufferComplete()) {
                framebuffersReady = false
                break
            }

            openglToolkit.legacyDrawTrace()
            when (cubeFace) {
                GL_TEXTURE_CUBE_MAP_POSITIVE_X -> {
                }
                GL_TEXTURE_CUBE_MAP_NEGATIVE_X -> {
                }
                GL_TEXTURE_CUBE_MAP_POSITIVE_Y -> {
                }
                GL_TEXTURE_CUBE_MAP_NEGATIVE_Y -> {
                }
                GL_TEXTURE_CUBE_MAP_POSITIVE_Z -> {
                }
                GL_TEXTURE_CUBE_MAP_NEGATIVE_Z -> {
                }
            }

        }

        display.setFramebuffer(0, null)
        openglToolkit.resizeDisplay(display)
        openglToolkit.activateTexture(1)
        openglToolkit.setActiveTexture(null)
        openglToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
        openglToolkit.activateTexture(0)
        openglToolkit.setActiveTexture(null)
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
