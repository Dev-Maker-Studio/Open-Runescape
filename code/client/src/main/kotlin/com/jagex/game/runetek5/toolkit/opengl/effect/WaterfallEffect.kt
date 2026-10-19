package com.jagex.game.runetek5.toolkit.opengl.effect

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.graphics.texture.water.TextureContainerGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils
import com.jagex.game.runetek5.toolkit.opengl.GL_EYE_LINEAR
import com.jagex.game.runetek5.toolkit.opengl.GL_EYE_PLANE
import com.jagex.game.runetek5.toolkit.opengl.GL_OBJECT_LINEAR
import com.jagex.game.runetek5.toolkit.opengl.GL_OBJECT_PLANE
import com.jagex.game.runetek5.toolkit.opengl.GL_R
import com.jagex.game.runetek5.toolkit.opengl.GL_S
import com.jagex.game.runetek5.toolkit.opengl.GL_T
import com.jagex.game.runetek5.toolkit.opengl.GL_TEXTURE_GEN_MODE
import com.jagex.game.runetek5.toolkit.opengl.GL_TEXTURE_GEN_R
import com.jagex.game.runetek5.toolkit.opengl.GL_TEXTURE_GEN_S
import com.jagex.game.runetek5.toolkit.opengl.GL_TEXTURE_GEN_T
import jaggl.OpenGL

/**
 * @author Brainpower - 04/03/2023
 * Handles the display of waterfall,
 * without this the water flowing from
 * an edge downwards renders as a white texture
 * Example at coords: 2507, 3459, 0
 */
class WaterfallEffect(
    openglToolkit: OpenGlToolkit,
    @JvmField val textureContainer: TextureContainerGL
) : MaterialEffectGL(openglToolkit) {

    @JvmField val listUtils: GlListUtils = GlListUtils(2)

    init {
        listUtils.newList(0)
        glToolkit.activateTexture(1)
        if (textureContainer.texture3dSupported) {
            OpenGL.glTexGeni(GL_R, GL_TEXTURE_GEN_MODE, GL_OBJECT_LINEAR)
            OpenGL.glEnable(GL_TEXTURE_GEN_R)
        }

        OpenGL.glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_EYE_LINEAR)
        OpenGL.glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_EYE_LINEAR)
        OpenGL.glEnable(GL_TEXTURE_GEN_S)
        OpenGL.glEnable(GL_TEXTURE_GEN_T)
        glToolkit.activateTexture(0)
        listUtils.endList()
        listUtils.newList(1)
        glToolkit.activateTexture(1)
        if (textureContainer.texture3dSupported) {
            OpenGL.glDisable(GL_TEXTURE_GEN_R)
        }

        OpenGL.glDisable(GL_TEXTURE_GEN_S)
        OpenGL.glDisable(GL_TEXTURE_GEN_T)
        glToolkit.activateTexture(0)
        listUtils.endList()
    }

    override fun isInitialized(): Boolean {
        return true
    }

    override fun loadCombineMode() {
    }

    override fun toggle(enabled: Boolean) {
        if (Loader.MATERIAL_EFFECTS_DISABLED[5]) return
        listUtils.callList(0)
        if (textureContainer.texture3dSupported) {
            glToolkit.activateTexture(1)
            glToolkit.setActiveTexture(textureContainer.billowTexture3d)
            glToolkit.activateTexture(0)
        }
    }

    override fun disable() {
        listUtils.callList(1)
        glToolkit.activateTexture(1)
        glToolkit.setActiveTexture(null)
        glToolkit.activateTexture(0)
    }

    override fun applyEffectParams(param1: Int, param2: Int) {
        val wNeg = -5.0E-4f * ((param1 and 0x3) + 1)
        val wPos = 5.0E-4f * ((param1 shr 3 and 0x3) + 1)
        val position = if ((param1 and 0x40) == 0) 4.8828125E-4f else 9.765625E-4f
        val flipped = (param1 and 0x80) != 0
        glToolkit.activateTexture(1)
        if (flipped) {
            // X
            PLANE_PARAMETERS[0] = position
            PLANE_PARAMETERS[1] = 0.0f
            PLANE_PARAMETERS[2] = 0.0f
        } else {
            // Z
            PLANE_PARAMETERS[0] = 0.0f
            PLANE_PARAMETERS[1] = 0.0f
            PLANE_PARAMETERS[2] = position
        }
        PLANE_PARAMETERS[3] = 0.0f

        OpenGL.glTexGenfv(GL_S, GL_EYE_PLANE, PLANE_PARAMETERS, 0)

        // Y
        PLANE_PARAMETERS[0] = 0.0f
        PLANE_PARAMETERS[1] = position
        PLANE_PARAMETERS[2] = 0.0f
        PLANE_PARAMETERS[3] = glToolkit.textureAnimatingProgression * wNeg % 1.0f

        OpenGL.glTexGenfv(GL_T, GL_EYE_PLANE, PLANE_PARAMETERS, 0)
        if (!textureContainer.texture3dSupported) {
            val texture = (wPos * glToolkit.textureAnimatingProgression * 16.0f).toInt()
            glToolkit.setActiveTexture(textureContainer.billowTextures2d!![texture % 16])
        } else {
            PLANE_PARAMETERS[0] = 0.0f
            PLANE_PARAMETERS[1] = 0.0f
            PLANE_PARAMETERS[2] = 0.0f
            PLANE_PARAMETERS[3] = glToolkit.textureAnimatingProgression * wPos % 1.0f
            OpenGL.glTexGenfv(GL_R, GL_OBJECT_PLANE, PLANE_PARAMETERS, 0)
        }

        glToolkit.activateTexture(0)
    }

    override fun loadCombiner(texture: OpenGlTexture?, combineMode: Int) {
        glToolkit.setActiveTexture(texture)
        glToolkit.loadTextureCombineMode(combineMode)
    }

    companion object {
        /**
         * Example: https://cpp.hotexamples.com/examples/-/-/glTexGenfv/cpp-gltexgenfv-function-examples.html
         * float X[4] = { 1,0,0,0 };
         * float Y[4] = { 0,1,0,0 };
         * float Z[4] = { 0,0,1,0 };
         * glTexGenfv( GL_S, GL_OBJECT_PLANE, X );
         * glTexGenfv( GL_T, GL_OBJECT_PLANE, Y );
         * glTexGenfv( GL_R, GL_OBJECT_PLANE, Z );
         */
        @JvmField val PLANE_PARAMETERS = FloatArray(4)
    }
}
