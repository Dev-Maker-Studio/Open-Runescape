package com.jagex.game.runetek5.graphics.shader

import com.jagex.game.runetek5.buffer.OpenGlRenderBuffer
import com.jagex.game.runetek5.graphics.texture.OpenGl3DTexture
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*
import jaggl.OpenGL

/**
 * 3D texture shader for OpenGL.
 *
 * @author Brainpower - 22/04/2022
 */
class Texture3DShaderGL(
    @JvmField val openglToolkit: OpenGlToolkit
) {

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

    fun activate(hdrTexture3d: OpenGl3DTexture, texture3d: OpenGl3DTexture): Boolean {
        if (!createShader()) {
            return false
        }
        val openglDisplay = openglToolkit.integratedGraphicsImageProducer
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

        openglDisplay.setFramebuffer(0, null)
        openglToolkit.resizeDisplay(openglDisplay)
        return ready
    }
}
