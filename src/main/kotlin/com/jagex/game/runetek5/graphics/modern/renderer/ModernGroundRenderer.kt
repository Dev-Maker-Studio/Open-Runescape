package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.core.VertexFormats
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TRIANGLES
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_SHORT
import com.jagex.game.runetek5.math.Matrix44
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * Modern OpenGL renderer for ground/terrain.
 *
 * Handles terrain rendering with:
 * - Material-based batching
 * - Per-vertex lighting (baked into vertex colors)
 * - Shadow mapping support
 * - Fog
 *
 * Vertex format (28 bytes per vertex):
 * - Position: vec3 (12 bytes) at offset 0
 * - Normal: vec3 (12 bytes) at offset 12
 * - Color: RGBA (4 bytes) at offset 24
 */
class ModernGroundRenderer {

    companion object {
        const val BYTES_PER_VERTEX = 28
        private const val DEFAULT_VERTEX_CAPACITY = 65536
        private const val DEFAULT_INDEX_CAPACITY = 131072
    }

    // GPU resources for self-managed rendering
    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var shader: ShaderProgram? = null

    // Textured ground shader (for rendering with external legacy VBOs)
    private var texturedShader: ShaderProgram? = null
    private var externalVao: VertexArray? = null
    private var externalIbo: IndexBuffer? = null

    // Uniform locations for textured shader
    private var uTexViewProjectionLocation = -1
    private var uTexTextureScaleLocation = -1
    private var uTexHasNormalsLocation = -1
    private var uTexSunDirectionLocation = -1
    private var uTexSunColorLocation = -1
    private var uTexAmbientLocation = -1
    private var uTexContrastLocation = -1
    private var uTexFogStartLocation = -1
    private var uTexFogEndLocation = -1
    private var uTexFogEnabledLocation = -1
    private var uTexFogColorLocation = -1
    private var uTexTextureLocation = -1
    private var uTexHasTextureLocation = -1
    private var uTexCombineModeLocation = -1
    private var uTexSunBrightnessLocation = -1
    private var uTexContrastPosLocation = -1
    private var uTexContrastNegLocation = -1

    // Legacy sun parameters for HD_LIGHT grounds (raw, unnormalized values from
    // OpenGlToolkit.sunDirection / sunBrightness / sunColorIntensity / antiSunColorIntensity)
    private var legacySunDirX = 0f
    private var legacySunDirY = -1f
    private var legacySunDirZ = 0f
    private var legacySunBrightness = 1f
    private var legacyContrastPos = 0f
    private var legacyContrastNeg = 0f

    /**
     * Sets the raw legacy sun parameters used by the HD_LIGHT ground lighting formula
     * (GroundGL.kt:458-459). Values must be passed UNNORMALIZED, matching legacy.
     */
    // ===== Ground shadows (native) =====
    // Legacy composites dynamic scenery/entity shadows with a fixed-function overlay
    // (ShadowMapperGL.renderShadowOverlay). That must never run in this pipeline, so
    // the same source data -- ShadowMapperGL.shadowMapperData, one intensity byte per
    // shadow texel over the whole ground -- is uploaded as an R8 texture and sampled
    // in the ground shaders instead.
    private var shadowTex = 0
    private var shadowW = 0
    private var shadowH = 0
    private var shadowTexelSize = 32f
    private var shadowVersionUploaded = -1
    private var shadowVersion = 0
    private var shadowData: ByteArray? = null
    private var shadowDiagCount = 0
    private val GROUND_POINT_LIGHTS =
        System.getProperty("render.groundlights")?.equals("false", true) != true
    private val GROUND_SHADOWS =
        System.getProperty("render.groundshadows")?.equals("true", true) == true

    /**
     * [texelSize] = world units per shadow texel.
     *
     * OFF by default (-Drender.groundshadows=true to enable). The sampling path is
     * wired and verified end to end EXCEPT the fetch, which still returns ~0, so the
     * feature contributes nothing visually — while the 2.78MB map re-uploads whenever
     * ShadowMapperGL bumps its version (every entity shadow add/remove), which cost
     * ~11ms/frame. Not worth carrying until the fetch is fixed.
     */
    fun setShadowMap(data: ByteArray, w: Int, h: Int, texelSize: Float, version: Int) {
        if (!GROUND_SHADOWS) return
        shadowData = data; shadowW = w; shadowH = h
        shadowTexelSize = texelSize; shadowVersion = version
    }

    private fun ensureShadowTexture() {
        val data = shadowData ?: return
        if (shadowW <= 0 || shadowH <= 0) return
        if (shadowTex == 0) {
            shadowTex = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, shadowTex)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            shadowVersionUploaded = -1
        }
        if (shadowVersionUploaded == shadowVersion) return
        shadowVersionUploaded = shadowVersion
        val buf = MemoryUtil.memAlloc(data.size)
        buf.put(data); buf.flip()
        glBindTexture(GL_TEXTURE_2D, shadowTex)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, shadowW, shadowH, 0,
            GL_RED, GL_UNSIGNED_BYTE, buf)
        MemoryUtil.memFree(buf)
    }

    private fun uploadShadowUniforms(s: ShaderProgram) {
        val en = s.getUniformLocation("uGroundShadowEnabled")
        if (System.getProperty("shadow.diag") != null && shadowDiagCount++ % 2000 == 1500) {
            System.err.println("[GroundShadow] uniformLoc=$en data=${shadowData != null} " +
                "dims=${shadowW}x${shadowH} texel=$shadowTexelSize tex=$shadowTex ver=$shadowVersion")
        }
        if (en < 0) return
        val have = shadowData != null && shadowW > 0
        if (!have) { GLWrapper.uniform1i(en, 0); return }
        ensureShadowTexture()
        GLWrapper.uniform1i(en, 1)
        val sc = s.getUniformLocation("uGroundShadowScale")
        if (sc >= 0) GLWrapper.uniform1f(sc, 1f / (shadowTexelSize * shadowW))
        val tx = s.getUniformLocation("uGroundShadowTex")
        if (tx >= 0) {
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + 4)
            glBindTexture(GL_TEXTURE_2D, shadowTex)
            GLWrapper.uniform1i(tx, 4)
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
        }
    }

    /** Point lights for this frame, evaluated in the ground shaders (no FFP). */
    private var plCount = 0
    private var plPos = FloatArray(0)
    private var plColor = FloatArray(0)

    fun setPointLights(count: Int, pos: FloatArray, color: FloatArray) {
        plCount = count
        plPos = pos
        plColor = color
    }

    private fun uploadPointLights(s: ShaderProgram) {
        val loc = s.getUniformLocation("uPointLightCount")
        if (loc < 0) return
        if (!GROUND_POINT_LIGHTS) { GLWrapper.uniform1i(loc, 0); return }
        GLWrapper.uniform1i(loc, plCount)
        if (plCount > 0) {
            val lp = s.getUniformLocation("uPointLightPos")
            val lc = s.getUniformLocation("uPointLightColor")
            if (lp >= 0) GLWrapper.uniform4fv(lp, plPos)
            if (lc >= 0) GLWrapper.uniform3fv(lc, plColor)
        }
    }

    fun setLegacySunParams(
        dirX: Float, dirY: Float, dirZ: Float,
        brightness: Float, contrastPos: Float, contrastNeg: Float
    ) {
        legacySunDirX = dirX
        legacySunDirY = dirY
        legacySunDirZ = dirZ
        legacySunBrightness = brightness
        legacyContrastPos = contrastPos
        legacyContrastNeg = contrastNeg
    }

    // CPU staging buffer
    private var vertexData: ByteBuffer? = null
    private var indexData: java.nio.ShortBuffer? = null

    // Matrices
    private val viewMatrix = Matrix4f()
    private val projectionMatrix = Matrix4f()
    private val modelViewMatrix = Matrix4f()
    private val modelViewProjectionMatrix = Matrix4f()
    private val normalMatrix = Matrix3f()

    // Shadow matrix for shadow mapping
    private val shadowMatrix = Matrix4f()

    // Lighting
    private val sunDirection = Vector3f(0f, 1f, 0f)
    private val sunColor = Vector3f(1f, 1f, 1f)
    private var ambient = 0.3f
    private var contrast = 0.7f

    // Fog
    private var fogEnabled = false
    private var fogStart = 100f
    private var fogEnd = 500f
    private val fogColor = floatArrayOf(0.7f, 0.8f, 0.9f, 1f)

    // Shadow mapping
    private var shadowEnabled = false
    private var shadowMapTextureId = 0
    private var shadowBias = 0.005f
    private var shadowIntensity = 0.5f
    private var shadowMapSize = 2048f  // Shadow map dimensions for PCF texel size calculation

    // Uniform locations
    private var uMVPLocation = -1
    private var uMVLocation = -1
    private var uNormalMatrixLocation = -1
    private var uSunDirectionLocation = -1
    private var uSunColorLocation = -1
    private var uAmbientLocation = -1
    private var uContrastLocation = -1
    private var uFogStartLocation = -1
    private var uFogEndLocation = -1
    private var uFogEnabledLocation = -1
    private var uFogColorLocation = -1
    private var uShadowMatrixLocation = -1
    private var uShadowEnabledLocation = -1
    private var uShadowMapLocation = -1
    private var uShadowBiasLocation = -1
    private var uShadowIntensityLocation = -1
    private var uShadowMapSizeLocation = -1

    private var isInitialized = false

    // Statistics
    private var drawCallsThisFrame = 0
    private var trianglesThisFrame = 0

    /**
     * Initializes the renderer.
     */
    fun initialize() {
        if (isInitialized) return

        // Initialize the standard ground shader (for self-managed vertex data)
        shader = ShaderProgram.createFromResources("/shaders/ground.vert", "/shaders/ground.frag")

        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.DYNAMIC)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.DYNAMIC, IndexBuffer.IndexType.UNSIGNED_SHORT)

        vao!!.bind()
        vbo!!.bind()
        VertexFormats.setupGroundFormat(vao!!)
        ibo!!.bind()
        vao!!.unbind()

        vertexData = MemoryUtil.memAlloc(DEFAULT_VERTEX_CAPACITY * BYTES_PER_VERTEX)
        indexData = MemoryUtil.memAllocShort(DEFAULT_INDEX_CAPACITY)

        cacheUniformLocations()

        // Initialize textured ground shader (for external legacy VBOs)
        texturedShader = ShaderProgram.createFromResources("/shaders/groundTextured.vert", "/shaders/groundTextured.frag")
        externalVao = VertexArray()
        externalIbo = IndexBuffer(IndexBuffer.BufferUsage.DYNAMIC, IndexBuffer.IndexType.UNSIGNED_SHORT)

        cacheTexturedUniformLocations()

        isInitialized = true
    }

    private fun cacheUniformLocations() {
        val s = shader ?: return
        s.bind()
        uMVPLocation = s.getUniformLocation("uModelViewProjection")
        uMVLocation = s.getUniformLocation("uModelView")
        uNormalMatrixLocation = s.getUniformLocation("uNormalMatrix")
        uSunDirectionLocation = s.getUniformLocation("uSunDirection")
        uSunColorLocation = s.getUniformLocation("uSunColor")
        uAmbientLocation = s.getUniformLocation("uAmbient")
        uContrastLocation = s.getUniformLocation("uContrast")
        uFogStartLocation = s.getUniformLocation("uFogStart")
        uFogEndLocation = s.getUniformLocation("uFogEnd")
        uFogEnabledLocation = s.getUniformLocation("uFogEnabled")
        uFogColorLocation = s.getUniformLocation("uFogColor")
        uShadowMatrixLocation = s.getUniformLocation("uShadowMatrix")
        uShadowEnabledLocation = s.getUniformLocation("uShadowEnabled")
        uShadowMapLocation = s.getUniformLocation("uShadowMap")
        uShadowBiasLocation = s.getUniformLocation("uShadowBias")
        uShadowIntensityLocation = s.getUniformLocation("uShadowIntensity")
        uShadowMapSizeLocation = s.getUniformLocation("uShadowMapSize")
        s.unbind()
    }

    private fun cacheTexturedUniformLocations() {
        val s = texturedShader ?: return
        s.bind()
        uTexViewProjectionLocation = s.getUniformLocation("uViewProjection")
        uTexTextureScaleLocation = s.getUniformLocation("uTextureScale")
        uTexHasNormalsLocation = s.getUniformLocation("uHasNormals")
        uTexSunDirectionLocation = s.getUniformLocation("uSunDirection")
        uTexSunColorLocation = s.getUniformLocation("uSunColor")
        uTexAmbientLocation = s.getUniformLocation("uAmbient")
        uTexContrastLocation = s.getUniformLocation("uContrast")
        uTexFogStartLocation = s.getUniformLocation("uFogStart")
        uTexFogEndLocation = s.getUniformLocation("uFogEnd")
        uTexFogEnabledLocation = s.getUniformLocation("uFogEnabled")
        uTexFogColorLocation = s.getUniformLocation("uFogColor")
        uTexTextureLocation = s.getUniformLocation("uTexture")
        uTexHasTextureLocation = s.getUniformLocation("uHasTexture")
        uTexCombineModeLocation = s.getUniformLocation("uCombineMode")
        uTexSunBrightnessLocation = s.getUniformLocation("uSunBrightness")
        uTexContrastPosLocation = s.getUniformLocation("uContrastPos")
        uTexContrastNegLocation = s.getUniformLocation("uContrastNeg")
        s.unbind()
    }

    /**
     * Sets the projection matrix.
     */
    fun setProjection(matrix: Matrix4f) {
        projectionMatrix.set(matrix)
    }

    /**
     * Sets the view matrix.
     */
    fun setView(matrix: Matrix4f) {
        viewMatrix.set(matrix)
    }

    /**
     * Configures lighting.
     */
    fun setLighting(direction: Vector3f, color: Vector3f, ambientIntensity: Float, contrastIntensity: Float) {
        sunDirection.set(direction).normalize()
        sunColor.set(color)
        ambient = ambientIntensity
        contrast = contrastIntensity
    }

    /**
     * Configures fog.
     */
    fun setFog(enabled: Boolean, start: Float, end: Float, r: Float, g: Float, b: Float) {
        fogEnabled = enabled
        fogStart = start
        fogEnd = end
        fogColor[0] = r
        fogColor[1] = g
        fogColor[2] = b
    }

    /**
     * Configures shadow mapping.
     */
    fun setShadowMapping(enabled: Boolean, shadowMap: Int, matrix: Matrix4f, bias: Float = 0.005f, intensity: Float = 0.5f, mapSize: Int = 2048) {
        shadowEnabled = enabled
        shadowMapTextureId = shadowMap
        shadowMatrix.set(matrix)
        shadowBias = bias
        shadowIntensity = intensity
        shadowMapSize = mapSize.toFloat()
    }

    /**
     * Begins a render frame.
     */
    fun beginFrame() {
        check(isInitialized) { "Renderer not initialized" }

        drawCallsThisFrame = 0
        trianglesThisFrame = 0

        shader!!.bind()
        setLightingUniforms()
        setFogUniforms()
        setShadowUniforms()
    }

    /**
     * Ends a render frame.
     */
    fun endFrame() {
        shader?.unbind()
    }

    /**
     * Draws terrain with the provided vertex and index data.
     *
     * @param vertices ByteBuffer with interleaved vertex data (28 bytes per vertex)
     * @param indices ShortBuffer with indices
     * @param indexCount Number of indices
     */
    fun drawIndexed(vertices: ByteBuffer, indices: java.nio.ShortBuffer, indexCount: Int) {
        if (indexCount == 0) return

        // Use identity model matrix for terrain (world space)
        projectionMatrix.mul(viewMatrix, modelViewProjectionMatrix)
        viewMatrix.normal(normalMatrix)

        shader!!.setUniformMatrix4(uMVPLocation, modelViewProjectionMatrix)
        shader!!.setUniformMatrix4(uMVLocation, viewMatrix)
        shader!!.setUniform("uNormalMatrix", normalMatrix)

        vbo!!.upload(vertices)
        ibo!!.upload(indices)

        vao!!.bind()
        ibo!!.bind()
        GLWrapper.drawElements(TRIANGLES, indexCount, UNSIGNED_SHORT, 0)

        drawCallsThisFrame++
        trianglesThisFrame += indexCount / 3
    }

    /**
     * Draws a terrain tile with separate attribute arrays.
     *
     * @param positions Vertex positions [x,y,z, ...]
     * @param normals Vertex normals [nx,ny,nz, ...]
     * @param colors Vertex colors [r,g,b,a, ...]
     * @param indices Triangle indices
     * @param vertexCount Number of vertices
     * @param indexCount Number of indices
     */
    fun drawTile(
        positions: FloatArray,
        normals: FloatArray,
        colors: ByteArray,
        indices: ShortArray,
        vertexCount: Int,
        indexCount: Int
    ) {
        if (indexCount == 0 || vertexCount == 0) return

        ensureVertexCapacity(vertexCount)
        ensureIndexCapacity(indexCount)

        val vb = vertexData!!
        val ib = indexData!!

        vb.clear()
        ib.clear()

        // Interleave vertex data
        for (i in 0 until vertexCount) {
            // Position (12 bytes)
            vb.putFloat(positions[i * 3])
            vb.putFloat(positions[i * 3 + 1])
            vb.putFloat(positions[i * 3 + 2])

            // Normal (12 bytes)
            vb.putFloat(normals[i * 3])
            vb.putFloat(normals[i * 3 + 1])
            vb.putFloat(normals[i * 3 + 2])

            // Color (4 bytes)
            vb.put(colors[i * 4])
            vb.put(colors[i * 4 + 1])
            vb.put(colors[i * 4 + 2])
            vb.put(colors[i * 4 + 3])
        }

        ib.put(indices, 0, indexCount)

        vb.flip()
        ib.flip()

        drawIndexed(vb, ib, indexCount)
    }

    /**
     * Gets frame statistics.
     */
    fun getFrameStats(): FrameStats {
        return FrameStats(drawCallsThisFrame, trianglesThisFrame)
    }

    /**
     * Renders ground geometry using external legacy VBOs.
     *
     * This method binds to pre-existing VBOs created by the legacy GroundGL system
     * and renders them through the modern shader pipeline. The legacy VBO has
     * the following interleaved layout (from GroundGL.resetLight):
     *
     * Standard (24 bytes per vertex):
     * - Position: vec3 (12 bytes) at offset 0
     * - Color: RGBA (4 bytes) at offset 12
     * - TexCoord: vec2 (8 bytes) at offset 16
     *
     * With 3D texcoords (28 bytes per vertex):
     * - Position: vec3 (12 bytes) at offset 0
     * - Color: RGBA (4 bytes) at offset 12
     * - TexCoord: vec3 (12 bytes) at offset 16
     *
     * With HD_LIGHT normals (add 12 bytes):
     * - Normal: vec3 (12 bytes) at offset 24 or 28
     *
     * @param interleavedVboId GL buffer ID containing all vertex data (position, color, texcoord, normal)
     * @param stride Stride between vertices in the interleaved buffer
     * @param texCoordSize Size of texture coordinates (2 for vec2, 3 for vec3)
     * @param indexData Raw index data as byte array
     * @param indexByteCount Number of bytes in indexData that contain valid index data
     * @param indexCount Number of indices to draw
     * @param textureHandle GL texture handle (0 for no texture)
     * @param textureScale Scale factor for texture coordinates
     * @param hasNormals Whether HD lighting normals are available
     * @param viewProjection The combined view-projection matrix (Matrix44 from legacy)
     * @param isBigEndian Whether the index data is big-endian
     */
    fun renderWithExternalVBOs(
        interleavedVboId: Int,
        stride: Int,
        texCoordSize: Int,
        indexData: ByteArray,
        indexByteCount: Int,
        indexCount: Int,
        textureHandle: Int,
        textureScale: Float,
        hasNormals: Boolean,
        viewProjection: Matrix44,
        isBigEndian: Boolean
    ) {
        if (indexCount == 0) return

        val s = texturedShader ?: return
        val vao = externalVao ?: return
        val ibo = externalIbo ?: return

        // Bind shader
        s.bind()

        // Set view-projection matrix: RS elements is row-major, upload straight through
        // for GLSL column-vector trick. Negate Y row for OpenGL Y-up convention.
        val vp = viewProjection.elements ?: return
        val colMajor = FloatArray(16)
        for (i in 0..15) colMajor[i] = vp[i]
        colMajor[1] = -colMajor[1]
        colMajor[5] = -colMajor[5]
        colMajor[9] = -colMajor[9]
        colMajor[13] = -colMajor[13]
        glUniformMatrix4fv(uTexViewProjectionLocation, false, colMajor)

        // Set texture scaling
        s.setUniform(uTexTextureScaleLocation, textureScale)

        // Set lighting parameters
        s.setUniform(uTexHasNormalsLocation, if (hasNormals) 1 else 0)
        s.setUniform(uTexSunDirectionLocation, sunDirection.x, sunDirection.y, sunDirection.z)
        s.setUniform("uSunColor", sunColor)
        uploadPointLights(s)
        uploadShadowUniforms(s)
        s.setUniform(uTexAmbientLocation, ambient)
        s.setUniform(uTexContrastLocation, contrast)

        // Set fog parameters
        s.setUniform(uTexFogEnabledLocation, if (fogEnabled) 1 else 0)
        s.setUniform(uTexFogStartLocation, fogStart)
        s.setUniform(uTexFogEndLocation, fogEnd)
        s.setUniform(uTexFogColorLocation, fogColor[0], fogColor[1], fogColor[2], fogColor[3])

        // Set texture parameters
        val hasTexture = textureHandle > 0
        s.setUniform(uTexHasTextureLocation, if (hasTexture) 1 else 0)
        if (hasTexture) {
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
            GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, textureHandle)
            // Ensure texture tiles properly with world-space texture coordinates
            GLWrapper.texParameteri(GLWrapper.Constants.TEXTURE_2D, GLWrapper.Constants.TEXTURE_WRAP_S, GLWrapper.Constants.REPEAT)
            GLWrapper.texParameteri(GLWrapper.Constants.TEXTURE_2D, GLWrapper.Constants.TEXTURE_WRAP_T, GLWrapper.Constants.REPEAT)
            s.setUniform(uTexTextureLocation, 0)
        }


        // Setup VAO with external VBO
        vao.bind()

        // Bind the single interleaved VBO
        GLWrapper.bindBuffer(GLWrapper.Constants.ARRAY_BUFFER, interleavedVboId)

        // Position: vec3 at offset 0
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L)

        // Color: vec4 ubyte at offset 12
        glEnableVertexAttribArray(1)
        glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, stride, 12L)

        // TexCoord: vec2 or vec3 at offset 16
        glEnableVertexAttribArray(2)
        glVertexAttribPointer(2, texCoordSize, GL_FLOAT, false, stride, 16L)

        // Calculate texture coordinate byte size (for normal offset calculation)
        val texCoordBytes = texCoordSize * 4  // 8 for vec2, 12 for vec3

        // Normal: vec3 at offset 16 + texCoordBytes (if HD_LIGHT)
        if (hasNormals) {
            val normalOffset = 16L + texCoordBytes
            glEnableVertexAttribArray(3)
            glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, normalOffset)
        } else {
            glDisableVertexAttribArray(3)
        }

        // Upload index data - convert byte array to short buffer with correct endianness
        ensureIndexCapacity(indexCount)
        val shortBuffer = this.indexData!!
        shortBuffer.clear()
        for (i in 0 until indexCount) {
            val byteOffset = i * 2
            val idx = if (isBigEndian) {
                // Big-endian: MSB first
                ((indexData[byteOffset].toInt() and 0xFF) shl 8) or (indexData[byteOffset + 1].toInt() and 0xFF)
            } else {
                // Little-endian: LSB first
                (indexData[byteOffset].toInt() and 0xFF) or ((indexData[byteOffset + 1].toInt() and 0xFF) shl 8)
            }
            shortBuffer.put(idx.toShort())
        }
        shortBuffer.flip()
        ibo.upload(shortBuffer)

        // Draw
        GLWrapper.drawElements(TRIANGLES, indexCount, UNSIGNED_SHORT, 0)

        // Cleanup
        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)
        glDisableVertexAttribArray(2)
        glDisableVertexAttribArray(3)
        vao.unbind()

        if (hasTexture) {
            GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, 0)
        }

        drawCallsThisFrame++
        trianglesThisFrame += indexCount / 3
    }

    // VBO for modern-only rendering (no legacy dependency)
    private var modernVbo: Int = 0

    // Scratch VBO for per-material color data when the legacy color buffer is not a VBO
    private var modernColorVbo: Int = 0

    // Reused matrix upload scratch (avoid per-draw allocation)
    private val vpScratch = FloatArray(16)

    /**
     * Renders ground using a ByteBuffer directly (no legacy VBO involvement).
     * This is the pure LWJGL path with zero legacy dependency.
     *
     * Color source parity with legacy: GroundMaterialGL.drawMaterialTriangles draws with
     * toggleDrawingClientstates(ground.vertexBuffer, ground.normalsBuffer,
     * material.arrayBufferPointer, ground.textureBuffer) (GroundMaterialGL.kt:189/199) —
     * i.e. vertex COLORS come from the PER-MATERIAL color VBO (baked light/shadow written
     * by writeVertexColor, alpha mask via appendToNativeStream), NOT from the interleaved
     * ground buffer (whose color at offset 12 is black for textured vertices).
     *
     * @param colorVboId GL buffer id of the per-material color VBO (4 ubytes RGBA per
     *        vertex, same vertex indexing as the interleaved buffer), or 0 when unavailable
     * @param colorData client-memory copy of the per-material colors when colorVboId == 0
     * @param combineMode legacy MaterialType.combineMode (see groundTextured.frag)
     */
    fun renderWithModernData(
        vertexData: java.nio.ByteBuffer,
        stride: Int,
        texCoordSize: Int,
        indexData: ByteArray,
        indexByteCount: Int,
        indexCount: Int,
        textureHandle: Int,
        textureScale: Float,
        hasNormals: Boolean,
        viewProjection: Matrix44,
        isBigEndian: Boolean,
        colorVboId: Int = 0,
        colorData: java.nio.ByteBuffer? = null,
        combineMode: Int = 0
    ) {
        if (indexCount == 0) return

        val s = texturedShader ?: return
        val vao = externalVao ?: return
        val ibo = externalIbo ?: return

        // Create VBO if needed
        if (modernVbo == 0) {
            modernVbo = glGenBuffers()
        }

        // Upload vertex data to our own VBO
        vertexData.position(0)
        glBindBuffer(GL_ARRAY_BUFFER, modernVbo)
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_DYNAMIC_DRAW)

        // Bind shader
        s.bind()

        // Set view-projection matrix: RS elements is row-major, upload straight through
        // for GLSL column-vector trick (row-major + M*v = row-vector convention result).
        // Negate Y row (indices 1,5,9,13) for OpenGL Y-up convention (RS VP has Y-down).
        val vp = viewProjection.elements ?: return
        val colMajor = vpScratch
        for (i in 0..15) colMajor[i] = vp[i]
        colMajor[1] = -colMajor[1]
        colMajor[5] = -colMajor[5]
        colMajor[9] = -colMajor[9]
        colMajor[13] = -colMajor[13]
        glUniformMatrix4fv(uTexViewProjectionLocation, false, colMajor)

        // Set texture scaling
        s.setUniform(uTexTextureScaleLocation, textureScale)

        // Set lighting parameters. HD_LIGHT grounds bake sunDotProduct = 1.0 into the
        // vertex colors and expect runtime sun lighting (GroundGL.kt:457-460), so pass
        // the RAW legacy sun parameters for the shader's legacy-formula lighting.
        s.setUniform(uTexHasNormalsLocation, if (hasNormals) 1 else 0)
        s.setUniform(uTexSunDirectionLocation, legacySunDirX, legacySunDirY, legacySunDirZ)
        s.setUniform("uSunColor", sunColor)
        uploadPointLights(s)
        uploadShadowUniforms(s)
        s.setUniform(uTexAmbientLocation, ambient)
        s.setUniform(uTexContrastLocation, contrast)
        s.setUniform(uTexSunBrightnessLocation, legacySunBrightness)
        s.setUniform(uTexContrastPosLocation, legacyContrastPos)
        s.setUniform(uTexContrastNegLocation, legacyContrastNeg)

        // Set fog parameters
        s.setUniform(uTexFogEnabledLocation, if (fogEnabled) 1 else 0)
        s.setUniform(uTexFogStartLocation, fogStart)
        s.setUniform(uTexFogEndLocation, fogEnd)
        s.setUniform(uTexFogColorLocation, fogColor[0], fogColor[1], fogColor[2], fogColor[3])

        // Set texture parameters
        val hasTexture = textureHandle > 0
        s.setUniform(uTexHasTextureLocation, if (hasTexture) 1 else 0)
        s.setUniform(uTexCombineModeLocation, combineMode)
        if (hasTexture) {
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
            GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, textureHandle)
            GLWrapper.texParameteri(GLWrapper.Constants.TEXTURE_2D, GLWrapper.Constants.TEXTURE_WRAP_S, GLWrapper.Constants.REPEAT)
            GLWrapper.texParameteri(GLWrapper.Constants.TEXTURE_2D, GLWrapper.Constants.TEXTURE_WRAP_T, GLWrapper.Constants.REPEAT)
            s.setUniform(uTexTextureLocation, 0)
        }

        // Clear depth buffer on the CURRENT FBO before first ground draw call.
        // The depth buffer on this FBO has stale values (0.0) because the initial
        // glClear(GL_DEPTH_BUFFER_BIT) in setModelMatrix() runs on a different FBO.
        // Ground renders inline during scene traversal before batched models flush,
        // so this clear is safe — no valid depth data has been written yet.
        if (drawCallsThisFrame == 0) {
            glClearDepth(1.0)
            glDepthMask(true)
            glClear(GL_DEPTH_BUFFER_BIT)
        }

        // Ensure proper GL state for ground rendering.
        // Blend must be on: the per-material color VBO carries a per-vertex alpha MASK
        // (0 for vertices not owned by this material, 255 for owned — written by
        // GroundMaterialGL.appendToNativeStream), and legacy draws ground with
        // setBlendMode(1) (alpha blend) via setupMatrixRendering. Without blending the
        // cross-material tile edges lose their legacy feathering.
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LEQUAL)
        glDepthMask(true)
        glDisable(GL_CULL_FACE)
        glColorMask(true, true, true, true)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        // Setup VAO with vertex attributes
        vao.bind()
        glBindBuffer(GL_ARRAY_BUFFER, modernVbo)

        // Position: vec3 at offset 0
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L)

        // TexCoord: vec2 or vec3 at offset 16
        glEnableVertexAttribArray(2)
        glVertexAttribPointer(2, texCoordSize, GL_FLOAT, false, stride, 16L)

        // Normal: vec3 at offset 16 + texCoordBytes (if hasNormals)
        val texCoordBytes = texCoordSize * 4
        if (hasNormals) {
            val normalOffset = 16L + texCoordBytes
            glEnableVertexAttribArray(3)
            glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, normalOffset)
        } else {
            glDisableVertexAttribArray(3)
        }

        // Color: from the per-material color VBO (legacy parity — see kdoc above).
        // Attribute pointers latch the ARRAY_BUFFER binding at specification time,
        // so this must come after the interleaved attributes are specified.
        glEnableVertexAttribArray(1)
        when {
            colorVboId != 0 -> {
                glBindBuffer(GL_ARRAY_BUFFER, colorVboId)
                glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, 4, 0L)
            }
            colorData != null -> {
                if (modernColorVbo == 0) modernColorVbo = glGenBuffers()
                colorData.position(0)
                glBindBuffer(GL_ARRAY_BUFFER, modernColorVbo)
                glBufferData(GL_ARRAY_BUFFER, colorData, GL_DYNAMIC_DRAW)
                glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, 4, 0L)
            }
            else -> {
                // Fallback: interleaved color at offset 12 (black for textured vertices)
                glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, stride, 12L)
            }
        }

        // Upload index data
        ensureIndexCapacity(indexCount)
        val shortBuffer = this.indexData!!
        shortBuffer.clear()
        for (i in 0 until indexCount) {
            val byteOffset = i * 2
            val idx = if (isBigEndian) {
                ((indexData[byteOffset].toInt() and 0xFF) shl 8) or (indexData[byteOffset + 1].toInt() and 0xFF)
            } else {
                (indexData[byteOffset].toInt() and 0xFF) or ((indexData[byteOffset + 1].toInt() and 0xFF) shl 8)
            }
            shortBuffer.put(idx.toShort())
        }
        shortBuffer.flip()
        ibo.upload(shortBuffer)
        ibo.bind()

        // Draw
        GLWrapper.drawElements(TRIANGLES, indexCount, UNSIGNED_SHORT, 0)

        // Cleanup
        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)
        glDisableVertexAttribArray(2)
        glDisableVertexAttribArray(3)
        vao.unbind()

        if (hasTexture) {
            GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, 0)
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0)
        // Unbind the program: legacy fixed-function draws interleave with this path
        // (lights, UI, fallbacks), and a bound program hijacks their vertex processing.
        s.unbind()

        drawCallsThisFrame++
        trianglesThisFrame += indexCount / 3
    }

    /**
     * Releases resources.
     */
    fun destroy() {
        if (modernVbo != 0) {
            glDeleteBuffers(modernVbo)
            modernVbo = 0
        }
        if (modernColorVbo != 0) {
            glDeleteBuffers(modernColorVbo)
            modernColorVbo = 0
        }
        shader?.destroy()
        vao?.destroy()
        vbo?.destroy()
        ibo?.destroy()

        texturedShader?.destroy()
        externalVao?.destroy()
        externalIbo?.destroy()

        vertexData?.let { MemoryUtil.memFree(it) }
        indexData?.let { MemoryUtil.memFree(it) }

        shader = null
        vao = null
        vbo = null
        ibo = null
        texturedShader = null
        externalVao = null
        externalIbo = null
        vertexData = null
        indexData = null

        isInitialized = false
    }

    private fun setLightingUniforms() {
        val s = shader ?: return
        s.setUniform(uSunDirectionLocation, sunDirection.x, sunDirection.y, sunDirection.z)
        s.setUniform("uSunColor", sunColor)
        uploadPointLights(s)
        uploadShadowUniforms(s)
        s.setUniform(uAmbientLocation, ambient)
        s.setUniform(uContrastLocation, contrast)
    }

    private fun setFogUniforms() {
        val s = shader ?: return
        s.setUniform(uFogEnabledLocation, if (fogEnabled) 1 else 0)
        s.setUniform(uFogStartLocation, fogStart)
        s.setUniform(uFogEndLocation, fogEnd)
        s.setUniform(uFogColorLocation, fogColor[0], fogColor[1], fogColor[2], fogColor[3])
    }

    private fun setShadowUniforms() {
        val s = shader ?: return
        s.setUniform(uShadowEnabledLocation, if (shadowEnabled) 1 else 0)
        if (shadowEnabled) {
            s.setUniform("uShadowMatrix", shadowMatrix)
            s.setUniform(uShadowMapLocation, 1) // Texture unit 1
            s.setUniform(uShadowBiasLocation, shadowBias)
            s.setUniform(uShadowIntensityLocation, shadowIntensity)
            GLWrapper.uniform2f(uShadowMapSizeLocation, shadowMapSize, shadowMapSize)

            // Bind shadow map to texture unit 1
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + 1)
            GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, shadowMapTextureId)
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
        }
    }

    private fun ensureVertexCapacity(vertexCount: Int) {
        val requiredBytes = vertexCount * BYTES_PER_VERTEX
        val vb = vertexData
        if (vb == null || requiredBytes > vb.capacity()) {
            vb?.let { MemoryUtil.memFree(it) }
            val newCapacity = (requiredBytes * 1.5).toInt().coerceAtLeast(DEFAULT_VERTEX_CAPACITY * BYTES_PER_VERTEX)
            vertexData = MemoryUtil.memAlloc(newCapacity)
        }
    }

    private fun ensureIndexCapacity(indexCount: Int) {
        val ib = indexData
        if (ib == null || indexCount > ib.capacity()) {
            ib?.let { MemoryUtil.memFree(it) }
            val newCapacity = (indexCount * 1.5).toInt().coerceAtLeast(DEFAULT_INDEX_CAPACITY)
            indexData = MemoryUtil.memAllocShort(newCapacity)
        }
    }

    data class FrameStats(
        val drawCalls: Int,
        val triangles: Int
    )
}
