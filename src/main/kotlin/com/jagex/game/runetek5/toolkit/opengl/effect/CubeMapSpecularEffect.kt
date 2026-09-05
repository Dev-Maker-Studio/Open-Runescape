package com.jagex.game.runetek5.toolkit.opengl.effect

import com.jagex.game.runetek5.graphics.texture.OpenGlArbCubeMapTexture
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.GlListUtils
import com.jagex.game.runetek5.toolkit.opengl.*
import jaggl.OpenGL
import kotlin.math.pow
import kotlin.math.sqrt

class CubeMapSpecularEffect(
    toolkit: OpenGlToolkit
) : MaterialEffectGL(toolkit) {

    @JvmField var listUtils: GlListUtils? = null
    @JvmField var enabled: Boolean = false
    @JvmField var isLimitedTextureUnits: Boolean = false
    @JvmField var cubeMapTextures: Array<OpenGlArbCubeMapTexture>? = null

    init {
        if (toolkit.arbTextureCubeMap) {
            isLimitedTextureUnits = toolkit.maxTextureUnits < 3
            val maxIntensity = if (isLimitedTextureUnits) 48 else 127
            val specular12 = Array(6) { ByteArray(4096) }
            val specular96 = Array(6) { ByteArray(4096) }
            val specular36 = Array(6) { ByteArray(4096) }
            var texelIndex = 0

            for (y in 0 until 64) {
                for (x in 0 until 64) {
                    var nx = 2.0f * x / 64.0f - 1.0f
                    var ny = 2.0f * y / 64.0f - 1.0f
                    var nz = (1.0 / sqrt((nx * nx + 1.0f + ny * ny).toDouble())).toFloat()
                    nx *= nz
                    ny *= nz

                    for (face in 0 until 6) {
                        val dot: Float = when (face) {
                            0 -> -nx
                            1 -> nx
                            2 -> ny
                            3 -> -ny
                            4 -> -nz
                            else -> nz
                        }

                        val intensity12: Int
                        val intensity36: Int
                        val intensity96: Int
                        if (dot > 0.0f) {
                            intensity96 = (dot.toDouble().pow(96.0) * maxIntensity).toInt()
                            intensity36 = (dot.toDouble().pow(36.0) * maxIntensity).toInt()
                            intensity12 = (dot.toDouble().pow(12.0) * maxIntensity).toInt()
                        } else {
                            intensity12 = 0
                            intensity36 = 0
                            intensity96 = 0
                        }

                        specular96[face][texelIndex] = intensity96.toByte()
                        specular36[face][texelIndex] = intensity36.toByte()
                        specular12[face][texelIndex] = intensity12.toByte()
                    }

                    texelIndex++
                }
            }

            cubeMapTextures = arrayOf(
                OpenGlArbCubeMapTexture(glToolkit, GLFormat.ALPHA, GLActionFormat.PIXEL_FORMAT, specular96, GLFormat.ALPHA),
                OpenGlArbCubeMapTexture(glToolkit, GLFormat.ALPHA, GLActionFormat.PIXEL_FORMAT, specular36, GLFormat.ALPHA),
                OpenGlArbCubeMapTexture(glToolkit, GLFormat.ALPHA, GLActionFormat.PIXEL_FORMAT, specular12, GLFormat.ALPHA)
            )
            initializeCombineLists()
        }
    }

    fun initializeCombineLists() {
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
        if (!isLimitedTextureUnits) {
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
        if (!isLimitedTextureUnits) {
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

    override fun toggle(enable: Boolean) {
        if (listUtils != null && enable) {
            if (!isLimitedTextureUnits) {
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
            if (!isLimitedTextureUnits) {
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

    override fun applyEffectParams(specularLevel: Int, effectParam2: Int) {
        if (enabled) {
            glToolkit.activateTexture(1)
            glToolkit.setActiveTexture(cubeMapTextures!![specularLevel - 1])
            glToolkit.activateTexture(0)
        }
    }
}
