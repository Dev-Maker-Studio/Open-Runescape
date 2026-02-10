package com.jagex.game.runetek5.toolkit.opengl.effect

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.toolkit.opengl.ProgramGL
import jaggl.OpenGL

/**
 * @author Steven - 19/07/2023
 * Displays unanimated water with blended edges,
 * without this on you'll see the underlay under the water
 * Seems to only be used for water tiles
 */
class StillWaterEffect(openglToolkit: OpenGlToolkit) : MaterialEffectGL(openglToolkit) {
    var calculateLights: Boolean = false
    var bound: Boolean = false
    var isUsingFallbackTexture: Boolean = false
    val initialized: Boolean
    var waterNodepthColor: ProgramGL? = null
    var waterNodepthReflection: ProgramGL? = null
    var waterDepthColor: ProgramGL? = null
    var waterDepthReflection: ProgramGL? = null
    var openglTexture: OpenGl2DTexture? = null

    init {
        if (glToolkit.arbVertexProgram) {
            this.waterNodepthColor = ProgramGL.generateProgram(
                glToolkit,
                """!!ARBvp1.0
ATTRIB  iPos         = vertex.position;
ATTRIB  iColour      = vertex.color;
ATTRIB  iTexCoord    = vertex.texcoord[0];
OUTPUT  oPos         = result.position;
OUTPUT  oColour      = result.color;
OUTPUT  oTexCoord0   = result.texcoord[0];
OUTPUT  oTexCoord1   = result.texcoord[1];
OUTPUT  oFogCoord    = result.fogcoord;
PARAM   fogParams    = program.local[0];
PARAM   waterPlane   = program.local[1];
PARAM   tMatrix[4]   = { state.matrix.texture[0] };
PARAM   pMatrix[4]   = { state.matrix.projection };
PARAM   mvMatrix[4]  = { state.matrix.modelview };
TEMP    viewPos, fogFactor;
DP4   viewPos.x, mvMatrix[0], iPos;
DP4   viewPos.y, mvMatrix[1], iPos;
DP4   viewPos.z, mvMatrix[2], iPos;
DP4   viewPos.w, mvMatrix[3], iPos;
SUB   fogFactor.x, viewPos.z, fogParams.x;
MUL   fogFactor.x, fogFactor.x, 0.001953125;
MAD   fogFactor.y, iTexCoord.z, fogParams.z, fogParams.w;
SUB   fogFactor.z, viewPos.z, fogParams.y;
MUL   fogFactor.z, fogFactor.z, 0.00390625;
MUL   fogFactor.x, fogFactor.x, fogFactor.y;
MIN   fogFactor, fogFactor, 1;
MAX   fogFactor, fogFactor, 0;
MUL   fogFactor.z, fogFactor.z, iTexCoord.z;
MAD   viewPos.xyz, waterPlane.xyzw, fogFactor.zzzz, viewPos.xyzw;
MAX   oTexCoord1.xyz, fogFactor.xxxx, fogFactor.yyyy;
MOV   oTexCoord1.w, 1;
MOV   oColour, iColour;
DP4   oPos.x, pMatrix[0], viewPos;
DP4   oPos.y, pMatrix[1], viewPos;
DP4   oPos.z, pMatrix[2], viewPos;
DP4   oPos.w, pMatrix[3], viewPos;
MOV   oFogCoord.x, viewPos.z;
DP3   oTexCoord0.x, tMatrix[0], iTexCoord;
DP3   oTexCoord0.y, tMatrix[1], iTexCoord;
MOV   oTexCoord0.zw, iTexCoord;
END
"""
            )
            this.waterNodepthReflection = ProgramGL.generateProgram(
                glToolkit,
                """!!ARBvp1.0
ATTRIB  iPos         = vertex.position;
ATTRIB  iNormal      = vertex.normal;
ATTRIB  iColour      = vertex.color;
ATTRIB  iTexCoord    = vertex.texcoord[0];
OUTPUT  oPos         = result.position;
OUTPUT  oColour      = result.color;
OUTPUT  oTexCoord0   = result.texcoord[0];
OUTPUT  oTexCoord1   = result.texcoord[1];
OUTPUT  oFogCoord    = result.fogcoord;
PARAM   fogParams    = program.local[0];
PARAM   waterPlane   = program.local[1];
PARAM   tMatrix[4]   = { state.matrix.texture[0] };
PARAM   pMatrix[4]   = { state.matrix.projection };
PARAM   mvMatrix[4]  = { state.matrix.modelview };
TEMP    viewPos, viewNormal, fogFactor, colour, ndotl;
DP4   viewPos.x, mvMatrix[0], iPos;
DP4   viewPos.y, mvMatrix[1], iPos;
DP4   viewPos.z, mvMatrix[2], iPos;
DP4   viewPos.w, mvMatrix[3], iPos;
SUB   fogFactor.x, viewPos.z, fogParams.x;
MUL   fogFactor.x, fogFactor.x, 0.001953125;
MAD   fogFactor.y, iTexCoord.z, fogParams.z, fogParams.w;
SUB   fogFactor.z, viewPos.z, fogParams.y;
MUL   fogFactor.z, fogFactor.z, 0.00390625;
MUL   fogFactor.x, fogFactor.x, fogFactor.y;
MIN   fogFactor, fogFactor, 1;
MAX   fogFactor, fogFactor, 0;
MUL   fogFactor.z, fogFactor.z, iTexCoord.z;
MAD   viewPos.xyz, waterPlane.xyzw, fogFactor.zzzz, viewPos.xyzw;
MAX   oTexCoord1.xyz, fogFactor.xxxx, fogFactor.yyyy;
MOV   oTexCoord1.w, 1;
DP3   viewNormal.x, mvMatrix[0], iNormal;
DP3   viewNormal.y, mvMatrix[1], iNormal;
DP3   viewNormal.z, mvMatrix[2], iNormal;
DP3   ndotl.x, viewNormal, state.light[0].position;
DP3   ndotl.y, viewNormal, state.light[1].position;
MAX   ndotl, ndotl, 0;
MOV   colour, state.lightmodel.ambient;
MAD   colour, state.light[0].diffuse, ndotl.xxxx, colour;
MAD   colour, state.light[1].diffuse, ndotl.yyyy, colour;
MUL   oColour, iColour, colour;
DP4   oPos.x, pMatrix[0], viewPos;
DP4   oPos.y, pMatrix[1], viewPos;
DP4   oPos.z, pMatrix[2], viewPos;
DP4   oPos.w, pMatrix[3], viewPos;
MOV   oFogCoord.x, viewPos.z;
DP3   oTexCoord0.x, tMatrix[0], iTexCoord;
DP3   oTexCoord0.y, tMatrix[1], iTexCoord;
MOV   oTexCoord0.zw, iTexCoord;
END
"""
            )
            this.waterDepthColor = ProgramGL.generateProgram(
                glToolkit,
                """!!ARBvp1.0
ATTRIB  iPos         = vertex.position;
ATTRIB  iColour      = vertex.color;
ATTRIB  iTexCoord    = vertex.texcoord[0];
OUTPUT  oPos         = result.position;
OUTPUT  oColour      = result.color;
OUTPUT  oTexCoord0   = result.texcoord[0];
OUTPUT  oTexCoord1   = result.texcoord[1];
OUTPUT  oFogCoord    = result.fogcoord;
PARAM   fogParams    = program.local[0];
PARAM   waterPlane   = program.local[1];
PARAM   pMatrix[4]   = { state.matrix.projection };
PARAM   mvMatrix[4]  = { state.matrix.modelview };
PARAM   texMatrix[4] = { state.matrix.texture[0] };
TEMP    viewPos, fogFactor, depth;
DP4   viewPos.x, mvMatrix[0], iPos;
DP4   viewPos.y, mvMatrix[1], iPos;
DP4   viewPos.z, mvMatrix[2], iPos;
DP4   viewPos.w, mvMatrix[3], iPos;
SUB   fogFactor.x, viewPos.z, fogParams.x;
MUL   fogFactor.x, fogFactor.x, 0.001953125;
DP4   depth, waterPlane, viewPos;
MAD   fogFactor.y, -depth, fogParams.z, fogParams.w;
SUB   fogFactor.z, viewPos.z, fogParams.y;
MUL   fogFactor.z, fogFactor.z, 0.00390625;
MIN   fogFactor, fogFactor, 1;
MAX   fogFactor, fogFactor, 0;
MUL   fogFactor.z, fogFactor.z, -depth;
MAD   viewPos.xyz, waterPlane.xyzw, fogFactor.zzzz, viewPos.xyzw;
MAX   oTexCoord1.xyz, fogFactor.xxxx, fogFactor.yyyy;
MOV   oTexCoord1.w, 1;
MOV   oColour, iColour;
DP4   oPos.x, pMatrix[0], viewPos;
DP4   oPos.y, pMatrix[1], viewPos;
DP4   oPos.z, pMatrix[2], viewPos;
DP4   oPos.w, pMatrix[3], viewPos;
MOV   oFogCoord.x, viewPos.z;
DP4   oTexCoord0.x, texMatrix[0], iTexCoord;
DP4   oTexCoord0.y, texMatrix[1], iTexCoord;
DP4   oTexCoord0.z, texMatrix[2], iTexCoord;
MOV   oTexCoord0.w, 1;
END
"""
            )
            this.waterDepthReflection = ProgramGL.generateProgram(
                glToolkit,
                """!!ARBvp1.0
ATTRIB  iPos         = vertex.position;
ATTRIB  iNormal      = vertex.normal;
ATTRIB  iColour      = vertex.color;
ATTRIB  iTexCoord    = vertex.texcoord[0];
OUTPUT  oPos         = result.position;
OUTPUT  oColour      = result.color;
OUTPUT  oTexCoord0   = result.texcoord[0];
OUTPUT  oTexCoord1   = result.texcoord[1];
OUTPUT  oFogCoord    = result.fogcoord;
PARAM   fogParams    = program.local[0];
PARAM   waterPlane   = program.local[1];
PARAM   pMatrix[4]   = { state.matrix.projection };
PARAM   mvMatrix[4]  = { state.matrix.modelview };
PARAM   texMatrix[4] = { state.matrix.texture[0] };
TEMP    viewPos, viewNormal, fogFactor, depth, colour, ndotl;
DP4   viewPos.x, mvMatrix[0], iPos;
DP4   viewPos.y, mvMatrix[1], iPos;
DP4   viewPos.z, mvMatrix[2], iPos;
DP4   viewPos.w, mvMatrix[3], iPos;
SUB   fogFactor.x, viewPos.z, fogParams.x;
MUL   fogFactor.x, fogFactor.x, 0.001953125;
DP4   depth, waterPlane, viewPos;
MAD   fogFactor.y, -depth, fogParams.z, fogParams.w;
SUB   fogFactor.z, viewPos.z, fogParams.y;
MUL   fogFactor.z, fogFactor.z, 0.00390625;
MIN   fogFactor, fogFactor, 1;
MAX   fogFactor, fogFactor, 0;
MUL   fogFactor.z, fogFactor.z, -depth;
MAD   viewPos.xyz, waterPlane.xyzw, fogFactor.zzzz, viewPos.xyzw;
MAX   oTexCoord1.xyz, fogFactor.xxxx, fogFactor.yyyy;
MOV   oTexCoord1.w, 1;
DP3   viewNormal.x, mvMatrix[0], iNormal;
DP3   viewNormal.y, mvMatrix[1], iNormal;
DP3   viewNormal.z, mvMatrix[2], iNormal;
DP3   ndotl.x, viewNormal, state.light[0].position;
DP3   ndotl.y, viewNormal, state.light[1].position;
MAX   ndotl, ndotl, 0;
MOV   colour, state.lightmodel.ambient;
MAD   colour, state.light[0].diffuse, ndotl.xxxx, colour;
MAD   colour, state.light[1].diffuse, ndotl.yyyy, colour;
MUL   oColour, iColour, colour;
DP4   oPos.x, pMatrix[0], viewPos;
DP4   oPos.y, pMatrix[1], viewPos;
DP4   oPos.z, pMatrix[2], viewPos;
DP4   oPos.w, pMatrix[3], viewPos;
MOV   oFogCoord.x, viewPos.z;
DP4   oTexCoord0.x, texMatrix[0], iTexCoord;
DP4   oTexCoord0.y, texMatrix[1], iTexCoord;
DP4   oTexCoord0.z, texMatrix[2], iTexCoord;
MOV   oTexCoord0.w, 1;
END
"""
            )
            if (this.waterNodepthColor != null && this.waterNodepthReflection != null && this.waterDepthColor != null && this.waterDepthReflection != null) {
                this.openglTexture = OpenGl2DTexture(
                    openglToolkit,
                    GL_TEXTURE_2D,
                    GLFormat.RGBA,
                    GLActionFormat.PIXEL_FORMAT,
                    2,
                    1,
                    false,
                    byteArrayOf(0.toByte(), (-1).toByte()),
                    GLFormat.RGBA,
                    false
                )
                this.openglTexture!!.setRepeat(false, false)
                this.initialized = true
            } else {
                this.initialized = false
            }
        } else {
            this.initialized = false
        }
    }

    override fun loadCombineMode() {
    }

    override fun toggle(lights: Boolean) {
        // System.out.println("Enabled material effect "+this.getClass().getSimpleName());
        if (Loader.MATERIAL_EFFECTS_DISABLED[3]) return
        this.calculateLights = lights
        glToolkit.activateTexture(1)
        glToolkit.setActiveTexture(this.openglTexture)
        glToolkit.loadTextureCombineMode(GL_INTERPOLATE, GL_REPLACE)
        glToolkit.setTexture(0, GL_CONSTANT, GL_SRC_COLOR)
        glToolkit.setTexture(2, GL_TEXTURE, GL_SRC_ALPHA)
        glToolkit.setTextureTransparency(0, GL_PREVIOUS)
        glToolkit.activateTexture(0)
        enable()
    }

    override fun isInitialized(): Boolean {
        return this.initialized
    }

    fun enable() {
        val matrix = glToolkit.viewMatrix44
        if (this.calculateLights) {
            OpenGL.glBindProgramARB(
                GL_VERTEX_PROGRAM_ARB,
                if (glToolkit.waterDepth == Integer.MAX_VALUE) this.waterNodepthReflection!!.programHandle else this.waterDepthReflection!!.programHandle
            )
        } else {
            OpenGL.glBindProgramARB(
                GL_VERTEX_PROGRAM_ARB,
                if (glToolkit.waterDepth == Integer.MAX_VALUE) this.waterNodepthColor!!.programHandle else this.waterDepthColor!!.programHandle
            )
        }

        val depth = glToolkit.waterDepth.toFloat()
        val scaleX = matrix.elements[4] * depth + matrix.elements[12]
        val scaleY = matrix.elements[5] * depth + matrix.elements[13]
        val scaleZ = matrix.elements[6] * depth + matrix.elements[14]
        XYZW[0] = -matrix.elements[4]
        XYZW[1] = -matrix.elements[5]
        XYZW[2] = -matrix.elements[6]
        XYZW[3] = -(XYZW[0] * scaleX + XYZW[1] * scaleY + XYZW[2] * scaleZ)
        OpenGL.glProgramLocalParameter4fARB(GL_VERTEX_PROGRAM_ARB, 1, XYZW[0], XYZW[1], XYZW[2], XYZW[3])
        OpenGL.glEnable(GL_VERTEX_PROGRAM_ARB)
        this.bound = true
        activate()
    }

    fun activate() {
        if (this.bound) {
            val f_1 = glToolkit.fogFarDistance
            val f_2 = glToolkit.fogMinimumStartPosition
            val f_3 = f_1 - (f_1 - f_2) * 0.125F
            val f_4 = f_1 - (f_1 - f_2) * 0.25F
            OpenGL.glProgramLocalParameter4fARB(
                GL_VERTEX_PROGRAM_ARB,
                0,
                f_4,
                f_3,
                256.0F / glToolkit.waterTile!!.scale,
                glToolkit.waterTile!!.intensity / 255.0F
            )
            glToolkit.activateTexture(1)
            glToolkit.setTextureEnvColor(glToolkit.waterTile!!.color)
            glToolkit.activateTexture(0)
        }
    }

    override fun loadCombiner(texture: OpenGlTexture?, combineMode: Int) {
        if (texture == null) {
            if (!this.isUsingFallbackTexture) {
                glToolkit.setActiveTexture(glToolkit.texture2d)
                glToolkit.loadTextureCombineMode(1)
                glToolkit.setTexture(0, GL_PREVIOUS, GL_SRC_COLOR)
                glToolkit.setTextureTransparency(0, GL_PREVIOUS)
                this.isUsingFallbackTexture = true
            }
        } else {
            if (this.isUsingFallbackTexture) {
                glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
                glToolkit.setTextureTransparency(0, GL_TEXTURE)
                this.isUsingFallbackTexture = false
            }
            glToolkit.setActiveTexture(texture)
            glToolkit.loadTextureCombineMode(combineMode)
        }
    }

    override fun applyEffectParams(param1: Int, param2: Int) {
        // NOOP
    }

    override fun disable() {
        if (this.bound) {
            OpenGL.glBindProgramARB(GL_VERTEX_PROGRAM_ARB, 0)
            OpenGL.glDisable(GL_FRAGMENT_PROGRAM_ARB)
            OpenGL.glDisable(GL_VERTEX_PROGRAM_ARB)
            this.bound = false
        }

        glToolkit.activateTexture(1)
        glToolkit.setActiveTexture(null)
        glToolkit.loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
        glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
        glToolkit.setTexture(2, GL_CONSTANT, GL_SRC_ALPHA)
        glToolkit.setTextureTransparency(0, GL_TEXTURE)
        glToolkit.activateTexture(0)
        if (this.isUsingFallbackTexture) {
            glToolkit.setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
            glToolkit.setTextureTransparency(0, GL_TEXTURE)
            this.isUsingFallbackTexture = false
        }
    }

    companion object {
        @JvmField
        val XYZW = floatArrayOf(0.0F, -1.0f, 0.0F, 0.0F)
    }
}
