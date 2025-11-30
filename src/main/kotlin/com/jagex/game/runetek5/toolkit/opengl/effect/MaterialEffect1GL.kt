package com.jagex.game.runetek5.toolkit.opengl.effect

import com.jagex.game.runetek5.graphics.texture.OpenGlArbCubeMapTexture
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*
import jaggl.OpenGL
import kotlin.math.pow
import kotlin.math.sqrt

class MaterialEffect1GL(
    toolkit: OpenGlToolkit
) : MaterialEffectGL(toolkit) {

    @JvmField var listUtils: GlListUtils? = null
    @JvmField var enabled: Boolean = false
    @JvmField var aBool9025: Boolean = false
    @JvmField var cubeMapTextures: Array<OpenGlArbCubeMapTexture>? = null

    init {
        if (toolkit.arbTextureCubeMap) {
            aBool9025 = toolkit.maxTextureUnits < 3
            val i_2 = if (aBool9025) 48 else 127
            val bytes_3 = Array(6) { ByteArray(4096) }
            val bytes_4 = Array(6) { ByteArray(4096) }
            val bytes_5 = Array(6) { ByteArray(4096) }
            var i_6 = 0

            for (i_7 in 0 until 64) {
                for (i_8 in 0 until 64) {
                    var f_9 = 2.0f * i_8 / 64.0f - 1.0f
                    var f_10 = 2.0f * i_7 / 64.0f - 1.0f
                    var f_11 = (1.0 / sqrt((f_9 * f_9 + 1.0f + f_10 * f_10).toDouble())).toFloat()
                    f_9 *= f_11
                    f_10 *= f_11

                    for (i_12 in 0 until 6) {
                        val f_13: Float = when (i_12) {
                            0 -> -f_9
                            1 -> f_9
                            2 -> f_10
                            3 -> -f_10
                            4 -> -f_11
                            else -> f_11
                        }

                        val i_14: Int
                        val i_15: Int
                        val i_16: Int
                        if (f_13 > 0.0f) {
                            i_16 = (f_13.toDouble().pow(96.0) * i_2).toInt()
                            i_15 = (f_13.toDouble().pow(36.0) * i_2).toInt()
                            i_14 = (f_13.toDouble().pow(12.0) * i_2).toInt()
                        } else {
                            i_14 = 0
                            i_15 = 0
                            i_16 = 0
                        }

                        bytes_4[i_12][i_6] = i_16.toByte()
                        bytes_5[i_12][i_6] = i_15.toByte()
                        bytes_3[i_12][i_6] = i_14.toByte()
                    }

                    i_6++
                }
            }

            cubeMapTextures = arrayOf(
                OpenGlArbCubeMapTexture(glToolkit, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, bytes_4, GLFormat.RGBA),
                OpenGlArbCubeMapTexture(glToolkit, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, bytes_5, GLFormat.RGBA),
                OpenGlArbCubeMapTexture(glToolkit, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, bytes_3, GLFormat.RGBA)
            )
            method14411()
        }
    }

    fun method14411() {
        listUtils = GlListUtils(2)
        listUtils!!.newList(0)
        glToolkit.activateTexture(1)
        OpenGL.glTexGeni(GL_S, GL_TEXTURE_GEN_MODE, GL_NORMAL_MAP)
        OpenGL.glTexGeni(GL_T, GL_TEXTURE_GEN_MODE, GL_NORMAL_MAP)
        OpenGL.glTexGeni(GL_R, GL_TEXTURE_GEN_MODE, GL_NORMAL_MAP)
        OpenGL.glEnable(GL_TEXTURE_GEN_S)
        OpenGL.glEnable(GL_TEXTURE_GEN_T)
        OpenGL.glEnable(GL_TEXTURE_GEN_R)
        OpenGL.glMatrixMode(GL_TEXTURE)
        OpenGL.glLoadIdentity()
        OpenGL.glRotatef(22.5f, 1.0f, 0.0f, 0.0f)
        OpenGL.glMatrixMode(GL_MODELVIEW)
        if (!aBool9025) {
            glToolkit.loadTextureCombineMode(GL_REPLACE, GL_MODULATE)
            glToolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR)
            glToolkit.activateTexture(2)
            glToolkit.loadTextureCombineMode(GL_ADD, GL_REPLACE)
            glToolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR)
            glToolkit.setTexture(1, GL_PREVIOUS, GL_SRC_ALPHA)
        } else {
            glToolkit.loadTextureCombineMode(GL_ADD, GL_REPLACE)
            glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_ALPHA)
        }
        glToolkit.setTextureTransparency(0, GL_PRIMARY_COLOR)

        glToolkit.activateTexture(0)
        listUtils!!.endList()
        listUtils!!.newList(1)
        glToolkit.activateTexture(1)
        OpenGL.glDisable(GL_TEXTURE_GEN_S)
        OpenGL.glDisable(GL_TEXTURE_GEN_T)
        OpenGL.glDisable(GL_TEXTURE_GEN_R)
        OpenGL.glMatrixMode(GL_TEXTURE)
        OpenGL.glLoadIdentity()
        OpenGL.glMatrixMode(GL_MODELVIEW)
        if (!aBool9025) {
            glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
            glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
            glToolkit.activateTexture(2)
            glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
            glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
            glToolkit.setTexture(1, GL_PREVIOUS, GL_SRC_COLOR)
        } else {
            glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
            glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
        }
        glToolkit.setTextureTransparency(0, GL_TEXTURE)

        glToolkit.activateTexture(0)
        listUtils!!.endList()
    }

    override fun isInitialized(): Boolean {
        return true
    }

    override fun toggle(bool1: Boolean) {
        if (listUtils != null && bool1) {
            if (!aBool9025) {
                glToolkit.activateTexture(2)
                glToolkit.setActiveTexture(glToolkit.texture2d)
                glToolkit.activateTexture(0)
            }

            listUtils!!.callList('\u0000'.code)
            enabled = true
        } else {
            glToolkit.setTextureTransparency(0, GL_PREVIOUS)
        }
    }

    override fun loadCombineMode() {
        glToolkit.loadTextureCombineMode(GL_MODULATE, GL_REPLACE)
    }

    override fun disable() {
        if (enabled) {
            if (!aBool9025) {
                glToolkit.activateTexture(2)
                glToolkit.setActiveTexture(null)
            }

            glToolkit.activateTexture(1)
            glToolkit.setActiveTexture(null)
            glToolkit.activateTexture(0)
            listUtils!!.callList(1)
            enabled = false
        } else {
            glToolkit.setTextureTransparency(0, GL_TEXTURE)
        }

        glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
    }

    override fun loadCombiner(texture: OpenGlTexture?, combineMode: Int) {
        glToolkit.setActiveTexture(texture)
        glToolkit.loadTextureCombineMode(combineMode)
    }

    override fun applyEffectParams(param1: Int, param2: Int) {
        if (enabled) {
            glToolkit.activateTexture(1)
            glToolkit.setActiveTexture(cubeMapTextures!![param1 - 1])
            glToolkit.activateTexture(0)
        }
    }
}
