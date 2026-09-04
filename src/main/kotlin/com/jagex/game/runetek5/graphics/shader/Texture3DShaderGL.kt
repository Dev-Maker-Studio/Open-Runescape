package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.buffer.OpenGlRenderBuffer
import com.jagex.game.runetek5.graphics.texture.OpenGl3DTexture
import com.jagex.game.runetek5.toolkit.ModernGLToolkit
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GL_UNPACK_ALIGNMENT
import com.jagex.game.runetek5.toolkit.opengl.GL_FLOAT
import com.jagex.game.runetek5.toolkit.opengl.GL_RGBA
import com.jagex.game.runetek5.toolkit.opengl.GL_TEXTURE_3D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.*
import jaggl.OpenGL

/**
 * 3D texture shader for OpenGL.
 *
 * @author Brainpower - 22/04/2022
 */
class Texture3DShaderGL(
    @JvmField val openglToolkit: OpenGlToolkit
) {
    /**
     * The modern toolkit's version of [activate]: the same normal-map derivation the
     * fragment shader performs, done on the CPU and uploaded as float RGBA — no
     * fixed-function quad. Exactness notes: the shader samples slice z = d/depth,
     * which with linear filtering is the midpoint between slice centres d-1 and d, so
     * every output slice is derived from the AVERAGE of those two height slices (with
     * wrap); the +-sampleSize taps land on neighbouring texel centres, so the
     * derivative is a plain neighbour difference with wrap-around.
     */
    private fun deriveOnCpu(toolkit: ModernGLToolkit, hdrTexture3d: OpenGl3DTexture, texture3d: OpenGl3DTexture, heightData: ByteArray): Boolean {
        val w = texture3d.width
        val h = texture3d.height
        val d = texture3d.depth
        if (heightData.size < w * h * d) return false
        val outDepth = hdrTexture3d.depth
        val slice = FloatArray(w * h)
        // Whole texture as native-order float RGBA bytes: uploaded through jaggl (the
        // toolkit is still being constructed here, before LWJGL capabilities exist).
        val bytes = ByteArray(w * h * outDepth * 4 * 4)
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
        val rcpRelief = 0.5f
        for (z in 0 until outDepth) {
            val zA = ((z - 1) + d) % d
            val zB = z % d
            for (i in 0 until w * h) {
                val a = (heightData[zA * w * h + i].toInt() and 0xff) / 255f
                val b = (heightData[zB * w * h + i].toInt() and 0xff) / 255f
                slice[i] = (a + b) * 0.5f
            }
            for (y in 0 until h) {
                val yM = ((y - 1) + h) % h
                val yP = (y + 1) % h
                for (x in 0 until w) {
                    val xM = ((x - 1) + w) % w
                    val xP = (x + 1) % w
                    val dx = slice[y * w + xM] - slice[y * w + xP]
                    val dy = slice[yM * w + x] - slice[yP * w + x]
                    val len = Math.sqrt((dx * dx + dy * dy + rcpRelief * rcpRelief).toDouble()).toFloat()
                    bb.putFloat(0.5f + dx / len * 0.5f).putFloat(0.5f + dy / len * 0.5f).putFloat(0.5f + rcpRelief / len * 0.5f).putFloat(slice[y * w + x])
                }
            }
        }
        toolkit.setActiveTexture(hdrTexture3d)
        OpenGL.glPixelStorei(GL_UNPACK_ALIGNMENT, 4)
        OpenGL.glTexImage3Dub(
            GL_TEXTURE_3D, 0, OpenGlToolkit.getInternalTextureFormat(GLFormat.HDR, GLActionFormat.PIXEL_FORMAT),
            w, h, outDepth, 0, GL_RGBA, GL_FLOAT, bytes, 0
        )
        return true
    }

    @JvmField var shader: OpenGLShaderManager? = null

    fun createShader(): Boolean {
        if (openglToolkit.intelGpu && openglToolkit.arbFragmentShader && shader == null) {
            val shaderProgram = OpenGlShader.createShader(
                openglToolkit, GL_FRAGMENT_SHADER,
                "uniform float rcpRelief;\n" +
                        "uniform vec2 sampleSize;\n" +
                        "uniform sampler3D heightMap;\n" +
                        "void main() {\n" +
                        "float dx = texture3D(heightMap, vec3(-sampleSize.x, 0.0, 0.0)+gl_TexCoord[0].xyz).r - texture3D(heightMap, vec3(sampleSize.x, 0.0, 0.0)+gl_TexCoord[0].xyz).r;\n" +
                        "float dy = texture3D(heightMap, vec3(0.0, -sampleSize.y, 0.0)+gl_TexCoord[0].xyz).r - texture3D(heightMap, vec3(0.0, sampleSize.y, 0.0)+gl_TexCoord[0].xyz).r;\n" +
                        "gl_FragColor = vec4(0.5+normalize(vec3(dx, dy, rcpRelief))*0.5, texture3D(heightMap, gl_TexCoord[0].xyz).r);\n}\n"
            )
            if (shaderProgram != null) {
                shader = OpenGLShaderManager.addShaders(openglToolkit, arrayOf(shaderProgram))
            }
        }
        return shader != null
    }

    fun activate(hdrTexture3d: OpenGl3DTexture, texture3d: OpenGl3DTexture, heightData: ByteArray): Boolean {
        if (openglToolkit is ModernGLToolkit) {
            return deriveOnCpu(openglToolkit, hdrTexture3d, texture3d, heightData)
        }
        if (!createShader()) {
            return false
        }
        val openglDisplay = openglToolkit.integratedGraphicsImageProducer!!
        val openglRenderBuffer = OpenGlRenderBuffer(openglToolkit, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, hdrTexture3d.width, hdrTexture3d.height)
        var ready = false
        openglToolkit.setDisplay(openglDisplay)
        openglDisplay.setFramebuffer(0, openglRenderBuffer)
        if (openglDisplay.isFramebufferComplete()) {
            OpenGL.glPushMatrix()
            OpenGL.glLoadIdentity()
            OpenGL.glMatrixMode(GL_PROJECTION)
            OpenGL.glPushMatrix()
            OpenGL.glLoadIdentity()
            OpenGL.glOrtho(0.0, 1.0, 0.0, 1.0, -1.0, 1.0)
            OpenGL.glPushAttrib(GL_VIEWPORT_BIT)
            OpenGL.glViewport(0, 0, hdrTexture3d.width, hdrTexture3d.height)
            OpenGL.glUseProgram(shader!!.programHandle)
            OpenGL.glUniform1i(OpenGL.glGetUniformLocation(shader!!.programHandle, "heightMap"), 0)
            OpenGL.glUniform1f(OpenGL.glGetUniformLocation(shader!!.programHandle, "rcpRelief"), 0.5f)
            OpenGL.glUniform2f(OpenGL.glGetUniformLocation(shader!!.programHandle, "sampleSize"), 1.0f / texture3d.width, 1.0f / texture3d.height)

            for (depth in 0 until hdrTexture3d.depth) {
                val r = depth.toFloat() / hdrTexture3d.depth
                openglToolkit.setActiveTexture(texture3d)
                openglToolkit.legacyDrawTrace()
                OpenGL.glBegin(7)
                OpenGL.glTexCoord3f(0.0f, 0.0f, r)
                OpenGL.glVertex2f(0.0f, 0.0f)
                OpenGL.glTexCoord3f(1.0f, 0.0f, r)
                OpenGL.glVertex2f(1.0f, 0.0f)
                OpenGL.glTexCoord3f(1.0f, 1.0f, r)
                OpenGL.glVertex2f(1.0f, 1.0f)
                OpenGL.glTexCoord3f(0.0f, 1.0f, r)
                OpenGL.glVertex2f(0.0f, 1.0f)
                OpenGL.glEnd()
                hdrTexture3d.copy(depth, hdrTexture3d.width, hdrTexture3d.height)
            }

            OpenGL.glUseProgram(0)
            OpenGL.glPopAttrib()
            OpenGL.glPopMatrix()
            OpenGL.glMatrixMode(GL_MODELVIEW)
            OpenGL.glPopMatrix()
            ready = true
        }

        openglDisplay!!.setFramebuffer(0, null)
        openglToolkit.resizeDisplay(openglDisplay)
        return ready
    }
}
