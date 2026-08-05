package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import org.joml.Matrix4f
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * Immediate-mode renderer for SURFACE-MASKED sprite quads — the real modern
 * implementation of the legacy multi-texture surface overlay:
 *
 * Legacy reference (SpriteGL.drawWithSurface, SpriteGL.kt:271-313 and
 * drawAffineMasked, SpriteGL.kt:489-519):
 *   unit 0 = sprite texture, combine REPLACE           → rgb/alpha from sprite
 *   unit 1 = surface (alpha mask), REPLACE(rgb=GL_PREVIOUS)/MODULATE(alpha)
 *                                                      → rgb = sprite.rgb,
 *                                                        alpha = sprite.a * surface.a
 *   glColor3f(1,1,1), setBlendMode(1) (SRC_ALPHA, ONE_MINUS_SRC_ALPHA)
 *
 * Used by the minimap (circular mask over the rotated map sprite and its icons —
 * ComponentMinimap.kt:292, NativeSprite.kt:89/119) and component-surface overlays.
 *
 * Draws one quad per call, immediately (no batching) — these are a handful of
 * quads per frame and must interleave exactly in submission order with the
 * sprite batch (the caller flushes the batch first).
 *
 * Vertex format (24 bytes): position vec2 | baseUV vec2 | maskUV vec2.
 */
class ModernSurfaceMaskRenderer {

    private var shader: ShaderProgram? = null
    private var vao = 0
    private var vbo = 0
    private var ibo = 0
    private var vertexData: ByteBuffer? = null

    private var uProjectionLocation = -1
    private var uBaseLocation = -1
    private var uMaskLocation = -1

    private val projection = Matrix4f()

    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return

        shader = ShaderProgram.createFromSource(
            """
            #version 330 core
            layout(location = 0) in vec2 aPosition;
            layout(location = 1) in vec2 aBaseUV;
            layout(location = 2) in vec2 aMaskUV;
            uniform mat4 uProjection;
            out vec2 vBaseUV;
            out vec2 vMaskUV;
            void main() {
                gl_Position = uProjection * vec4(aPosition, 0.0, 1.0);
                vBaseUV = aBaseUV;
                vMaskUV = aMaskUV;
            }
            """.trimIndent(),
            """
            #version 330 core
            in vec2 vBaseUV;
            in vec2 vMaskUV;
            uniform sampler2D uBase;
            uniform sampler2D uMask;
            out vec4 fragColor;
            void main() {
                // Legacy combine: rgb = sprite.rgb (REPLACE from GL_PREVIOUS),
                // alpha = sprite.a * surface.a (MODULATE) — SpriteGL.kt:278-282.
                vec4 base = texture(uBase, vBaseUV);
                float mask = texture(uMask, vMaskUV).a;
                fragColor = vec4(base.rgb, base.a * mask);
            }
            """.trimIndent()
        )
        shader!!.bind()
        uProjectionLocation = shader!!.getUniformLocation("uProjection")
        uBaseLocation = shader!!.getUniformLocation("uBase")
        uMaskLocation = shader!!.getUniformLocation("uMask")
        shader!!.unbind()

        vao = glGenVertexArrays()
        vbo = glGenBuffers()
        ibo = glGenBuffers()

        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, (4 * 24).toLong(), GL_STREAM_DRAW)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 24, 0L)
        glEnableVertexAttribArray(1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 24, 8L)
        glEnableVertexAttribArray(2)
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 24, 16L)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)
        val indices = MemoryUtil.memAllocShort(6)
        indices.put(0).put(1).put(2).put(0).put(2).put(3)
        indices.flip()
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)
        MemoryUtil.memFree(indices)
        glBindVertexArray(0)

        vertexData = MemoryUtil.memAlloc(4 * 24)
        isInitialized = true
    }

    fun isReady(): Boolean = isInitialized

    /**
     * Draws one masked quad immediately. Corner order matches the legacy
     * glBegin(GL_QUADS) submission (v0..v3); screen coordinates, top-left origin.
     * The caller is responsible for flushing the sprite batch first (submission
     * order) and for restoring legacy state caches afterwards.
     */
    fun drawMaskedQuad(
        baseHandle: Int, maskHandle: Int,
        screenWidth: Int, screenHeight: Int,
        x0: Float, y0: Float, b0u: Float, b0v: Float, m0u: Float, m0v: Float,
        x1: Float, y1: Float, b1u: Float, b1v: Float, m1u: Float, m1v: Float,
        x2: Float, y2: Float, b2u: Float, b2v: Float, m2u: Float, m2v: Float,
        x3: Float, y3: Float, b3u: Float, b3v: Float, m3u: Float, m3v: Float
    ) {
        if (!isInitialized || baseHandle <= 0 || maskHandle <= 0) return
        val s = shader ?: return

        // Match the sprite batch's presentation conventions: full-target ortho with
        // top-left origin, full-target viewport (legacy 2D parity — see
        // ModernSpriteRenderer.begin/flush).
        GLWrapper.viewport(0, 0, screenWidth, screenHeight)
        projection.identity().ortho(0f, screenWidth.toFloat(), screenHeight.toFloat(), 0f, -1f, 1f)

        GLWrapper.enable(GLWrapper.Constants.BLEND)
        GLWrapper.blendFunc(GLWrapper.Constants.SRC_ALPHA, GLWrapper.Constants.ONE_MINUS_SRC_ALPHA)
        GLWrapper.disable(GLWrapper.Constants.DEPTH_TEST)
        GLWrapper.depthMask(false)

        s.bind()
        s.setUniformMatrix4(uProjectionLocation, projection)
        s.setUniform(uBaseLocation, 0)
        s.setUniform(uMaskLocation, 1)

        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
        GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, baseHandle)
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + 1)
        GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, maskHandle)
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)

        val vb = vertexData!!
        vb.clear()
        vb.putFloat(x0).putFloat(y0).putFloat(b0u).putFloat(b0v).putFloat(m0u).putFloat(m0v)
        vb.putFloat(x1).putFloat(y1).putFloat(b1u).putFloat(b1v).putFloat(m1u).putFloat(m1v)
        vb.putFloat(x2).putFloat(y2).putFloat(b2u).putFloat(b2v).putFloat(m2u).putFloat(m2v)
        vb.putFloat(x3).putFloat(y3).putFloat(b3u).putFloat(b3v).putFloat(m3u).putFloat(m3v)
        vb.flip()

        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0L, vb)
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0L)
        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        s.unbind()
    }

    fun destroy() {
        shader?.destroy()
        shader = null
        if (vao != 0) { glDeleteVertexArrays(vao); vao = 0 }
        if (vbo != 0) { glDeleteBuffers(vbo); vbo = 0 }
        if (ibo != 0) { glDeleteBuffers(ibo); ibo = 0 }
        vertexData?.let { MemoryUtil.memFree(it) }
        vertexData = null
        isInitialized = false
    }
}
