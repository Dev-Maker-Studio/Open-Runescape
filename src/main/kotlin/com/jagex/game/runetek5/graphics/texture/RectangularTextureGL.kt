package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.graphics.shader.OpenGLShaderManager
import com.jagex.game.runetek5.graphics.shader.OpenGlShader
import com.jagex.game.runetek5.imageproducer.IntegratedGraphicsDisplayGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*
import com.jagex.game.runetek5.utils.MathUtils
import jaggl.OpenGL

/**
 *
 * @author Brainpower - 22/04/2022
 *
 */
class RectangularTextureGL(toolkit: OpenGlToolkit) : Node_Sub5(toolkit) {

    var display: IntegratedGraphicsDisplayGL? = null
    var aClass137Sub110038: OpenGl2DTexture? = null
    var aClass137Sub110034: OpenGl2DTexture? = null
    var scenetexShader: OpenGLShaderManager? = null
    var mipmappedScenetexShader: OpenGLShaderManager? = null
    var bloomScenetexShader: OpenGLShaderManager? = null
    var basetexShader: OpenGLShaderManager? = null
    var s: Int = 0
    var t: Int = 0
    var textureDisplay: IntegratedGraphicsDisplayGL? = null
    var mipmaps: Array<OpenGl2DTexture?>? = null
    var width: Int = 0
    var height: Int = 0

    override fun draw(texture: OpenGl2DTexture) {
        OpenGL.glPushAttrib(GL_VIEWPORT_BIT)
        OpenGL.glMatrixMode(GL_PROJECTION)
        OpenGL.glPushMatrix()
        OpenGL.glLoadIdentity()
        OpenGL.glOrtho(0.0, 1.0, 0.0, 1.0, -1.0, 1.0)
        if (mipmaps != null) {
            activeToolkit.setDisplay(textureDisplay!!)
            var width = MathUtils.furtestBitValue(s)
            var height = MathUtils.furtestBitValue(t)

            var mipmapLevel = 0
            while (width > 256 || height > 256) {
                OpenGL.glViewport(0, 0, width, height)
                textureDisplay!!.setFramebuffer(0, mipmaps!![mipmapLevel]!!.createFrameBuffer())
                if (mipmapLevel == 0) {
                    activeToolkit.setActiveTexture(texture)
                    OpenGL.glBegin(GL_QUADS)
                    OpenGL.glTexCoord2f(0.0f, 0.0f)
                    OpenGL.glVertex2i(0, 0)
                    OpenGL.glTexCoord2f(s.toFloat(), 0.0f)
                    OpenGL.glVertex2i(1, 0)
                    OpenGL.glTexCoord2f(s.toFloat(), t.toFloat())
                    OpenGL.glVertex2i(1, 1)
                    OpenGL.glTexCoord2f(0.0f, t.toFloat())
                } else {
                    activeToolkit.setActiveTexture(mipmaps!![mipmapLevel - 1])
                    OpenGL.glBegin(GL_QUADS)
                    OpenGL.glTexCoord2f(0.0f, 0.0f)
                    OpenGL.glVertex2i(0, 0)
                    OpenGL.glTexCoord2f(1.0f, 0.0f)
                    OpenGL.glVertex2i(1, 0)
                    OpenGL.glTexCoord2f(1.0f, 1.0f)
                    OpenGL.glVertex2i(1, 1)
                    OpenGL.glTexCoord2f(0.0f, 1.0f)
                }
                OpenGL.glVertex2i(0, 1)
                OpenGL.glEnd()

                if (width > 256) {
                    width = width shr 1
                }

                if (height > 256) {
                    height = height shr 1
                }
                mipmapLevel++
            }

            activeToolkit.resizeDisplay(textureDisplay!!)
            activeToolkit.setActiveTexture(mipmaps!![mipmapLevel - 1])
            activeToolkit.setDisplay(display!!)
            display!!.drawBuffer(0)
            OpenGL.glViewport(0, 0, 256, 256)
            val handle = mipmappedScenetexShader!!.programHandle
            OpenGL.glUseProgram(handle)
            OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle, "sceneTex"), 0)
            OpenGL.glUniform3f(OpenGL.glGetUniformLocation(handle, "params"), whitePoint, 0.0f, 0.0f)
            OpenGL.glBegin(GL_QUADS)
            OpenGL.glTexCoord2f(0.0f, 0.0f)
            OpenGL.glVertex2i(0, 0)
            OpenGL.glTexCoord2f(1.0f, 0.0f)
            OpenGL.glVertex2i(1, 0)
            OpenGL.glTexCoord2f(1.0f, 1.0f)
            OpenGL.glVertex2i(1, 1)
            OpenGL.glTexCoord2f(0.0f, 1.0f)
        } else {
            activeToolkit.setActiveTexture(texture)
            activeToolkit.setDisplay(display!!)
            display!!.drawBuffer(0)
            OpenGL.glViewport(0, 0, 256, 256)
            val handle = scenetexShader!!.programHandle
            OpenGL.glUseProgram(handle)
            OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle, "sceneTex"), 0)
            OpenGL.glUniform3f(OpenGL.glGetUniformLocation(handle, "params"), whitePoint, 0.0f, 0.0f)
            OpenGL.glBegin(GL_QUADS)
            OpenGL.glTexCoord2f(0.0f, 0.0f)
            OpenGL.glVertex2i(0, 0)
            OpenGL.glTexCoord2f(s.toFloat(), 0.0f)
            OpenGL.glVertex2i(1, 0)
            OpenGL.glTexCoord2f(s.toFloat(), t.toFloat())
            OpenGL.glVertex2i(1, 1)
            OpenGL.glTexCoord2f(0.0f, t.toFloat())
        }
        OpenGL.glVertex2i(0, 1)
        OpenGL.glEnd()

        display!!.drawBuffer(1)
        activeToolkit.setActiveTexture(aClass137Sub110038)
        val handle = basetexShader!!.programHandle
        OpenGL.glUseProgram(handle)
        OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle, "baseTex"), 0)
        OpenGL.glUniform3f(OpenGL.glGetUniformLocation(handle, "step"), 0.00390625f, 0.0f, 0.0f)
        OpenGL.glBegin(GL_QUADS)
        OpenGL.glTexCoord2f(0.0f, 0.0f)
        OpenGL.glVertex2i(0, 0)
        OpenGL.glTexCoord2f(1.0f, 0.0f)
        OpenGL.glVertex2i(1, 0)
        OpenGL.glTexCoord2f(1.0f, 1.0f)
        OpenGL.glVertex2i(1, 1)
        OpenGL.glTexCoord2f(0.0f, 1.0f)
        OpenGL.glVertex2i(0, 1)
        OpenGL.glEnd()
        display!!.drawBuffer(0)
        activeToolkit.setActiveTexture(aClass137Sub110034)
        OpenGL.glUniform3f(OpenGL.glGetUniformLocation(handle, "step"), 0.0f, 0.00390625f, 0.0f)
        OpenGL.glBegin(GL_QUADS)
        OpenGL.glTexCoord2f(0.0f, 0.0f)
        OpenGL.glVertex2i(0, 0)
        OpenGL.glTexCoord2f(1.0f, 0.0f)
        OpenGL.glVertex2i(1, 0)
        OpenGL.glTexCoord2f(1.0f, 1.0f)
        OpenGL.glVertex2i(1, 1)
        OpenGL.glTexCoord2f(0.0f, 1.0f)
        OpenGL.glVertex2i(0, 1)
        OpenGL.glEnd()
        OpenGL.glPopAttrib()
        OpenGL.glPopMatrix()
        OpenGL.glMatrixMode(GL_MODELVIEW)
        activeToolkit.resizeDisplay(display!!)
        val handle2 = bloomScenetexShader!!.programHandle
        OpenGL.glUseProgram(handle2)
        OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle2, "sceneTex"), 0)
        OpenGL.glUniform1i(OpenGL.glGetUniformLocation(handle2, "bloomTex"), 1)
        OpenGL.glUniform3f(OpenGL.glGetUniformLocation(handle2, "params"), brightpass, bloom, 0.0f)
        activeToolkit.activateTexture(1)
        activeToolkit.setActiveTexture(aClass137Sub110038)
        activeToolkit.activateTexture(0)
        activeToolkit.setActiveTexture(texture)
    }

    fun rectangularTextureSupport(): Boolean {
        return activeToolkit.intelGpu && activeToolkit.arbFragmentShader && activeToolkit.arbTextureFloat
    }

    override fun displayPresent(): Boolean {
        return display != null
    }

    override fun createShaders(): Boolean {
        if (activeToolkit.intelGpu && activeToolkit.arbFragmentShader && activeToolkit.arbTextureFloat) {
            display = IntegratedGraphicsDisplayGL(super.activeToolkit)
            aClass137Sub110038 = OpenGl2DTexture(activeToolkit, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.FLOAT_16_ARB, 256, 256)
            aClass137Sub110038!!.setRepeat(false, false)
            aClass137Sub110034 = OpenGl2DTexture(activeToolkit, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.FLOAT_16_ARB, 256, 256)
            aClass137Sub110034!!.setRepeat(false, false)
            activeToolkit.setDisplay(display!!)
            display!!.setFramebuffer(0, aClass137Sub110038!!.createFrameBuffer())
            display!!.setFramebuffer(1, aClass137Sub110034!!.createFrameBuffer())
            display!!.drawBuffer(0)
            if (!display!!.isFramebufferComplete()) {
                activeToolkit.resizeDisplay(display!!)
                return false
            }
            activeToolkit.resizeDisplay(display!!)
            scenetexShader = OpenGLShaderManager.addShaders(activeToolkit, arrayOf(
                OpenGlShader.createShader(super.activeToolkit, GL_FRAGMENT_SHADER,
                    """#extension GL_ARB_texture_rectangle : enable
uniform vec3 params;
uniform sampler2DRect sceneTex;
const vec3 lumCoef = vec3(0.2126, 0.7152, 0.0722);
void main() {    vec4 col = texture2DRect(sceneTex, gl_TexCoord[0].xy);    gl_FragColor = col*step(params.x, dot(lumCoef, col.rgb));
}
""")!!))
            mipmappedScenetexShader = OpenGLShaderManager.addShaders(activeToolkit, arrayOf(
                OpenGlShader.createShader(super.activeToolkit, GL_FRAGMENT_SHADER,
                    """uniform vec3 params;
uniform sampler2D sceneTex;
const vec3 lumCoef = vec3(0.2126, 0.7152, 0.0722);
void main() {    vec4 col = texture2D(sceneTex, gl_TexCoord[0].xy);    gl_FragColor = col*step(params.x, dot(lumCoef, col.rgb));
}
""")!!))
            bloomScenetexShader = OpenGLShaderManager.addShaders(activeToolkit, arrayOf(
                OpenGlShader.createShader(super.activeToolkit, GL_FRAGMENT_SHADER,
                    """#extension GL_ARB_texture_rectangle : enable
uniform vec3 params;
uniform vec3 dimScale;
uniform sampler2D bloomTex;
uniform sampler2DRect sceneTex;
const vec3 lumCoef = vec3(0.2126, 0.7152, 0.0722);
void main() {	 vec4 bloomCol = texture2D(bloomTex, gl_TexCoord[1].xy);	 vec4 sceneCol = texture2DRect(sceneTex, gl_TexCoord[0].xy);	 float preLum = 0.99*dot(lumCoef, sceneCol.rgb)+0.01;    float postLum = preLum*(1.0+(preLum/params.y))/(preLum+1.0);	 gl_FragColor = sceneCol*(postLum/preLum)+bloomCol*params.x;
}
""")!!))
            basetexShader = OpenGLShaderManager.addShaders(activeToolkit, arrayOf(
                OpenGlShader.createShader(super.activeToolkit, GL_FRAGMENT_SHADER,
                    """uniform vec3 step;
uniform sampler2D baseTex;
void main() {	vec4 fragCol = texture2D(baseTex, gl_TexCoord[0].xy)*0.091396265;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-1.0*step.xy))*0.088584304;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 1.0*step.xy))*0.088584304;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-2.0*step.xy))*0.08065692;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 2.0*step.xy))*0.08065692;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-3.0*step.xy))*0.068989515;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 3.0*step.xy))*0.068989515;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-4.0*step.xy))*0.055434637;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 4.0*step.xy))*0.055434637;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-5.0*step.xy))*0.04184426;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 5.0*step.xy))*0.04184426;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-6.0*step.xy))*0.029672023;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 6.0*step.xy))*0.029672023;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-7.0*step.xy))*0.019765828;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 7.0*step.xy))*0.019765828;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+(-8.0*step.xy))*0.012369139;	fragCol += texture2D(baseTex, gl_TexCoord[0].xy+( 8.0*step.xy))*0.012369139;	gl_FragColor = fragCol;
}
""")!!))
            return mipmappedScenetexShader != null && scenetexShader != null && bloomScenetexShader != null && basetexShader != null
        }
        return false
    }

    override fun calculateMipmaps(s: Int, t: Int) {
        this.s = s
        this.t = t
        val width = MathUtils.furtestBitValue(this.s)
        val height = MathUtils.furtestBitValue(this.t)
        if (width != this.width || height != this.height) {
            if (mipmaps != null) {
                for (level in mipmaps!!.indices) {
                    mipmaps!![level]?.delete()
                }

                mipmaps = null
            }

            if (width <= 256 && height <= 256) {
                textureDisplay = null
            } else {
                var w = width
                var h = height
                var level = 0

                GenerateMipmaps@ while (true) {
                    if (w <= 256 && h <= 256) {
                        if (textureDisplay == null) {
                            textureDisplay = IntegratedGraphicsDisplayGL(activeToolkit)
                        }

                        mipmaps = arrayOfNulls(level)
                        w = width
                        h = height
                        level = 0

                        while (true) {
                            if (w <= 256 && h <= 256) {
                                break@GenerateMipmaps
                            }

                            mipmaps!![level++] = OpenGl2DTexture(activeToolkit, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.FLOAT_16_ARB, w, h)
                            if (w > 256) {
                                w = w shr 1
                            }

                            if (h > 256) {
                                h = h shr 1
                            }
                        }
                    }

                    if (w > 256) {
                        w = w shr 1
                    }

                    if (h > 256) {
                        h = h shr 1
                    }

                    level++
                }
            }

            this.width = width
            this.height = height
        }
    }

    override fun getFormat(): GLActionFormat {
        return GLActionFormat.FLOAT_16_ARB
    }

    override fun resetTexture() {
        OpenGL.glUseProgram(0)
        activeToolkit.activateTexture(1)
        activeToolkit.setActiveTexture(null)
        activeToolkit.activateTexture(0)
    }

    override fun destroy() {
        height = -1
        width = -1
        textureDisplay = null
        mipmaps = null
        display = null
        aClass137Sub110038 = null
        aClass137Sub110034 = null
        scenetexShader = null
        mipmappedScenetexShader = null
        bloomScenetexShader = null
        basetexShader = null
    }

    companion object {
        @JvmField var brightpass = 0.25f
        @JvmField var bloom = 1.0f
        @JvmField var whitePoint = 1.0f
    }
}
