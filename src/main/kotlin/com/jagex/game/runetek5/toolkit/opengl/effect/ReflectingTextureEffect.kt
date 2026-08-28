package com.jagex.game.runetek5.toolkit.opengl.effect

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.graphics.texture.OpenGlArbCubeMapTexture
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils
import com.jagex.game.runetek5.toolkit.opengl.*
import jaggl.OpenGL

/**
 *
 * @author Brainpower - 19/07/2023
 * Handles textures that reflect light
 * Example at coords; 2854, 3168, 0
 *
 */
class ReflectingTextureEffect(toolkit: OpenGlToolkit) : MaterialEffectGL(toolkit) {

    @JvmField var loaded: Boolean = false
    @JvmField var listUtils: GlListUtils? = null

    init {
        if (toolkit.arbTextureCubeMap) {
            listUtils = GlListUtils(2)
            listUtils!!.newList(0)
            glToolkit.activateTexture(1)
            glToolkit.loadTextureCombineMode(GL_INTERPOLATE, GL_REPLACE)
            glToolkit.setTexture(2, GL_PREVIOUS, GL_SRC_ALPHA)
            glToolkit.setTextureTransparency(0, GL_PRIMARY_COLOR)
            OpenGL.glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_REFLECTION_MAP)
            OpenGL.glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_REFLECTION_MAP)
            OpenGL.glTexGeni(GL_R, GL_TEXTURE_GEN_MODE, GL_REFLECTION_MAP)
            OpenGL.glEnable(GL_TEXTURE_GEN_S)
            OpenGL.glEnable(GL_TEXTURE_GEN_T)
            OpenGL.glEnable(GL_TEXTURE_GEN_R)
            glToolkit.activateTexture(0)
            listUtils!!.endList()
            listUtils!!.newList(1)
            glToolkit.activateTexture(1)
            glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
            glToolkit.setTexture(2, GL_CONSTANT, GL_SRC_ALPHA)
            glToolkit.setTextureTransparency(0, GL_TEXTURE)
            OpenGL.glDisable(GL_TEXTURE_GEN_S)
            OpenGL.glDisable(GL_TEXTURE_GEN_T)
            OpenGL.glDisable(GL_TEXTURE_GEN_R)
            OpenGL.glMatrixMode(GL_TEXTURE)
            OpenGL.glLoadIdentity()
            OpenGL.glMatrixMode(GL_MODELVIEW)
            glToolkit.activateTexture(0)
            listUtils!!.endList()
        }
    }

    override fun disable() {
        if (loaded) {
            listUtils!!.callList(1)
            glToolkit.activateTexture(1)
            glToolkit.setActiveTexture(null)
            glToolkit.activateTexture(0)
        } else {
            glToolkit.setTextureTransparency(0, GL_TEXTURE)
        }

        glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
        loaded = false
    }

    override fun applyEffectParams(param1: Int, param2: Int) {
        // NOOP
    }

    override fun loadCombiner(combiner: OpenGlTexture?, combineMode: Int) {
        glToolkit.setActiveTexture(combiner)
        glToolkit.loadTextureCombineMode(combineMode)
    }

    override fun isInitialized(): Boolean {
        return true
    }

    override fun loadCombineMode() {
        glToolkit.loadTextureCombineMode(GL_MODULATE, GL_REPLACE)
    }

    override fun toggle(enabled: Boolean) {
        //System.out.println("Enabled material effect "+this.getClass().getSimpleName());
        if (Loader.MATERIAL_EFFECTS_DISABLED[7]) return
        val cubeMap = glToolkit.getCubeMap()
        if (listUtils != null && cubeMap != null && enabled) {
            listUtils!!.callList(0)
            glToolkit.activateTexture(1)
            glToolkit.setActiveTexture(cubeMap)
            OpenGL.glMatrixMode(GL_TEXTURE)
            OpenGL.glLoadMatrixf(glToolkit.inverseSunMatrix44.extractRotationInto(glToolkit.textureMatrix), 0)
            OpenGL.glMatrixMode(GL_MODELVIEW)
            glToolkit.activateTexture(0)
            loaded = true
        } else {
            glToolkit.setTextureTransparency(0, GL_PREVIOUS)
        }
    }
}
