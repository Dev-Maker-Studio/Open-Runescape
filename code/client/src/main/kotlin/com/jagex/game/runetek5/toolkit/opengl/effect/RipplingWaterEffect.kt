package com.jagex.game.runetek5.toolkit.opengl.effect

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.graphics.texture.OpenGl1DTexture
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.graphics.texture.water.TextureContainerGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils
import com.jagex.game.runetek5.toolkit.opengl.*
import jaggl.OpenGL

/**
 * Material effect for rendering white rippling waves on water tiles.
 *
 * @author Steven - 19/07/2023
 */
class RipplingWaterEffect(
    toolkit: OpenGlToolkit,
    val textureContainer: TextureContainerGL
) : MaterialEffectGL(toolkit) {

    lateinit var glLists: GlListUtils
    val texture1d: OpenGl1DTexture

    init {
        initializeGLLists()
        texture1d = OpenGl1DTexture(
            glToolkit,
            GLFormat.ALPHA,
            GLActionFormat.PIXEL_FORMAT,
            byteArrayOf(0.toByte(), (-1).toByte()),
            GLFormat.ALPHA
        )
        texture1d.setActive()
    }

    override fun isInitialized(): Boolean = true

    override fun loadCombineMode() {
        glToolkit.loadTextureCombineMode(GL_ADD, GL_MODULATE)
    }

    override fun disable() {
        glLists.callList(1)
        if (glToolkit.fogDensity > 0) {
            glToolkit.activateTexture(1)
            glToolkit.setActiveTexture(null)
            glToolkit.configureFog(1.0f, 0.0f)
            glToolkit.activateTexture(0)
        }

        glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
        OpenGL.glMatrixMode(GL_TEXTURE)
        OpenGL.glPopMatrix()
        OpenGL.glMatrixMode(GL_MODELVIEW)
    }

    override fun toggle(enabled: Boolean) {
        if (Loader.MATERIAL_EFFECTS_DISABLED[4]) return

        if (glToolkit.fogDensity > 0) {
            val density = -0.5f / glToolkit.fogDensity
            glToolkit.activateTexture(1)
            PLANE_PARAMETERS[0] = 0.0f
            PLANE_PARAMETERS[1] = 0.0f
            PLANE_PARAMETERS[2] = density
            PLANE_PARAMETERS[3] = glToolkit.fogStartPosition * density + 0.25f
            OpenGL.glPushMatrix()
            OpenGL.glLoadIdentity()
            OpenGL.glTexGenfv(GL_S, GL_EYE_PLANE, PLANE_PARAMETERS, 0)
            OpenGL.glPopMatrix()
            glToolkit.configureFog(0.5f, glToolkit.fogDensity.toFloat())
            glToolkit.setActiveTexture(texture1d)
            glToolkit.activateTexture(0)
        }

        glLists.callList(0)
        OpenGL.glMatrixMode(GL_TEXTURE)
        OpenGL.glPushMatrix()
        OpenGL.glScalef(0.25f, 0.25f, 1.0f)
        OpenGL.glMatrixMode(GL_MODELVIEW)
    }

    override fun loadCombiner(combiner: OpenGlTexture?, combineMode: Int) {
        // NOOP
    }

    fun initializeGLLists() {
        glLists = GlListUtils(2)
        glLists.newList(0)
        glToolkit.activateTexture(1)
        glToolkit.loadTextureCombineMode(GL_REPLACE, GL_ADD)
        glToolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR)
        OpenGL.glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_EYE_LINEAR)
        OpenGL.glEnable(GL_TEXTURE_GEN_S)
        glToolkit.activateTexture(0)
        OpenGL.glTexEnvf(GL_TEXTURE_ENV, GL_RGB_SCALE, 2.0f)

        if (textureContainer.texture3dSupported) {
            OpenGL.glTexGeni(GL_R, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR)
            OpenGL.glTexGeni(GL_Q, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR)
            OpenGL.glTexGenfv(GL_Q, GL_OBJECT_PLANE, floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f), 0)
            OpenGL.glEnable(GL_TEXTURE_GEN_R)
            OpenGL.glEnable(GL_TEXTURE_GEN_Q)
        }

        glLists.endList()
        glLists.newList(1)
        glToolkit.activateTexture(1)
        glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
        glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
        OpenGL.glDisable(GL_TEXTURE_GEN_S)
        glToolkit.activateTexture(0)
        OpenGL.glTexEnvf(GL_TEXTURE_ENV, GL_RGB_SCALE, 1.0f)
        if (textureContainer.texture3dSupported) {
            OpenGL.glDisable(GL_TEXTURE_GEN_R)
            OpenGL.glDisable(GL_TEXTURE_GEN_Q)
        }

        glLists.endList()
    }

    override fun applyEffectParams(param1: Int, param2: Int) {
        if ((param1 and 0x1) == 1) {
            if (textureContainer.texture3dSupported) {
                glToolkit.setActiveTexture(textureContainer.texture3d)
                PLANE_PARAMETERS[0] = 0.0f
                PLANE_PARAMETERS[1] = 0.0f
                PLANE_PARAMETERS[2] = 0.0f
                PLANE_PARAMETERS[3] = (glToolkit.textureAnimatingProgression % 4000) / 4000.0f
                OpenGL.glTexGenfv(GL_R, GL_OBJECT_PLANE, PLANE_PARAMETERS, 0)
            } else {
                val step = glToolkit.textureAnimatingProgression % 4000 * 16 / 4000
                glToolkit.setActiveTexture(textureContainer.textures2d!![step])
            }
        } else if (textureContainer.texture3dSupported) {
            glToolkit.setActiveTexture(textureContainer.texture3d)
            PLANE_PARAMETERS[0] = 0.0f
            PLANE_PARAMETERS[1] = 0.0f
            PLANE_PARAMETERS[2] = 0.0f
            PLANE_PARAMETERS[3] = 0.0f
            OpenGL.glTexGenfv(GL_R, GL_OBJECT_PLANE, PLANE_PARAMETERS, 0)
        } else {
            glToolkit.setActiveTexture(textureContainer.textures2d!![0])
        }
    }

    companion object {
        @JvmField
        val PLANE_PARAMETERS = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
    }
}
