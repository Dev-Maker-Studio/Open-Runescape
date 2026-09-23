package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.graphics.model.particle.PointEntity
import com.jagex.game.runetek5.utils.BufferUtils
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ShortBuffer

/**
 * Modern OpenGL 3.3 particle renderer.
 *
 * Replaces the legacy GlParticleRenderer which used:
 * - glGetFloatv(GL_MODELVIEW_MATRIX) to extract billboard vectors
 * - glDisable(GL_LIGHT0) / glDisable(GL_LIGHT1) for unlit rendering
 * - glNormal3f(0, -1, 0) for fixed normal
 * - OpenGlArrayDrawBuffer for vertex data upload
 *
 * This modern renderer:
 * - Computes billboard corners on the CPU using the view matrix columns
 *   (identical math to legacy, but sourced from the toolkit's view matrix
 *   rather than glGetFloatv)
 * - Renders all particles as camera-facing quads via VBO + VAO + IBO
 * - Depth-sorts particles back-to-front using radix-sort bucketing
 *   (matching legacy depth sorting exactly)
 * - Batches particles by material (texture) to minimize draw calls
 * - Uses ONLY GLWrapper (LWJGL3 GL33) -- zero jaggl imports
 *
 * Vertex format (24 bytes per vertex):
 *   - position: vec3 (12 bytes) at offset 0  -- world-space billboard corner
 *   - texcoord: vec2 (8 bytes) at offset 12  -- UV coordinates
 *   - color: vec4 ubyte (4 bytes) at offset 20 -- RGBA particle color
 *
 * Each particle is a quad (4 vertices, 6 indices).
 */
class ModernParticleRenderer {

    companion object {
        /** Bytes per vertex in the particle vertex format. */
        const val BYTES_PER_VERTEX = 24

        /** Maximum particles per draw call batch. */
        private const val MAX_PARTICLES = 8191

        /** Maximum depth buckets for radix-sort (matches legacy). */
        private const val MAX_DEPTH_BUCKETS = 1600

        /** Maximum particles per depth bucket (matches legacy). */
        private const val MAX_PER_BUCKET = 64

        /** Maximum overflow buckets (matches legacy). */
        private const val MAX_OVERFLOW_BUCKETS = 64
    }

    // GPU resources
    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var shader: ShaderProgram? = null

    // CPU staging buffer for vertex data
    private var vertexData: ByteBuffer? = null

    // Pre-built static index buffer for quads
    private var staticIndices: ShortBuffer? = null

    // Uniform locations (cached)
    private var uViewProjectionLocation = -1
    private var uTextureLocation = -1
    private var uTextureEnabledLocation = -1
    private var uFogEnabledLocation = -1
    private var uFogStartLocation = -1
    private var uFogEndLocation = -1
    private var uFogColorLocation = -1

    // State
    private var isInitialized = false

    // Statistics
    private var drawCallsThisFrame = 0
    private var particlesThisFrame = 0

    // Depth sorting arrays (matches legacy GlParticleRenderer structure exactly)
    private val particleDepths = IntArray(MAX_PARTICLES)
    private val bucketCounts = IntArray(MAX_DEPTH_BUCKETS)
    private val overflowCounts = IntArray(MAX_OVERFLOW_BUCKETS)
    private val primaryBuckets = Array(MAX_DEPTH_BUCKETS) { arrayOfNulls<PointEntity>(MAX_PER_BUCKET) }
    private val overflowBuckets = Array(MAX_OVERFLOW_BUCKETS) { arrayOfNulls<PointEntity>(768) }
    private var overflowBucketIndex = 0
    private val depthShift: Int = BufferUtils.ilog(MAX_DEPTH_BUCKETS)

    /**
     * Initializes the renderer. Must be called on the GL thread before rendering.
     */
    private val PARTICLE_POS_DIAG = System.getProperty("render.particlediag") != null
    private var posDiagTick = 0

    fun initialize() {
        if (isInitialized) return

        // Create shader from resources
        shader = ShaderProgram.createFromResources("/shaders/particle.vert", "/shaders/particle.frag")

        // Create GPU resources
        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.STATIC, IndexBuffer.IndexType.UNSIGNED_SHORT)

        // Setup VAO with particle vertex format (24 bytes per vertex)
        vao!!.bind()
        vbo!!.bind()

        // position: vec3 at offset 0
        vao!!.setAttribute(
            VertexArray.Companion.Attributes.POSITION, 3,
            GLWrapper.Constants.FLOAT, false,
            BYTES_PER_VERTEX, 0
        )
        // texcoord: vec2 at offset 12
        vao!!.setAttribute(
            VertexArray.Companion.Attributes.TEXCOORD, 2,
            GLWrapper.Constants.FLOAT, false,
            BYTES_PER_VERTEX, 12
        )
        // color: vec4 ubyte normalized at offset 20
        vao!!.setAttribute(
            VertexArray.Companion.Attributes.COLOR, 4,
            GLWrapper.Constants.UNSIGNED_BYTE, true,
            BYTES_PER_VERTEX, 20
        )

        // Build static index buffer for MAX_PARTICLES quads
        // Each quad uses indices: [i*4+0, i*4+1, i*4+2, i*4+2, i*4+3, i*4+0]
        // This matches the legacy winding order from GlParticleRenderer/NativeParticleRenderer
        val indexData = MemoryUtil.memAllocShort(MAX_PARTICLES * 6)
        for (i in 0 until MAX_PARTICLES) {
            val base = i * 4
            indexData.put(base.toShort())
            indexData.put((base + 1).toShort())
            indexData.put((base + 2).toShort())
            indexData.put((base + 2).toShort())
            indexData.put((base + 3).toShort())
            indexData.put(base.toShort())
        }
        indexData.flip()
        ibo!!.upload(indexData)
        staticIndices = indexData
        // Note: indexData is kept alive since ibo.upload copies it

        ibo!!.bind()
        vao!!.unbind()

        // Allocate CPU staging buffer for vertex data
        vertexData = MemoryUtil.memAlloc(MAX_PARTICLES * 4 * BYTES_PER_VERTEX)

        // Cache uniform locations
        cacheUniformLocations()

        isInitialized = true
    }

    private fun cacheUniformLocations() {
        val s = shader ?: return
        s.bind()
        uViewProjectionLocation = s.getUniformLocation("uViewProjection")
        uTextureLocation = s.getUniformLocation("uTexture")
        uTextureEnabledLocation = s.getUniformLocation("uTextureEnabled")
        uFogEnabledLocation = s.getUniformLocation("uFogEnabled")
        uFogStartLocation = s.getUniformLocation("uFogStart")
        uFogEndLocation = s.getUniformLocation("uFogEnd")
        uFogColorLocation = s.getUniformLocation("uFogColor")
        s.unbind()
    }

    /**
     * Checks if the renderer is initialized and ready.
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Renders all particles from the given ParticleWrapper.
     *
     * This method replicates the exact rendering logic from the legacy
     * GlParticleRenderer.drawParticles(), including:
     * - Depth sorting with radix-sort bucketing
     * - Back-to-front rendering order
     * - Material (texture) batching
     * - Billboard quad computation from view matrix columns
     *
     * @param particleWrapper The particle container
     * @param viewMatrixFloats The 4x4 view (modelview) matrix as a 16-element float array.
     *                         This replaces the legacy glGetFloatv(GL_MODELVIEW_MATRIX) call.
     *                         The view matrix is sourced from the toolkit's particleMatrix field.
     * @param projectionMatrixFloats The projection matrix for depth sorting
     * @param lookupTextureHandle Function to resolve material ID to GL texture handle
     * @param fogEnabled Whether fog is active
     * @param fogStart Fog start distance
     * @param fogEnd Fog end distance
     * @param fogR Fog color red (0-1)
     * @param fogG Fog color green (0-1)
     * @param fogB Fog color blue (0-1)
     */
    fun renderParticles(
        particleWrapper: ParticleWrapper,
        viewMatrixFloats: FloatArray,
        projectionMatrixFloats: FloatArray,
        lookupTextureHandle: (Int) -> Int,
        fogEnabled: Boolean,
        fogStart: Float,
        fogEnd: Float,
        fogR: Float,
        fogG: Float,
        fogB: Float,
        /** The scene's tracked view-projection (row-major, RS clip space); when given it replaces proj x view. */
        vpOverride: FloatArray? = null
    ) {
        if (!isInitialized) return

        drawCallsThisFrame = 0
        particlesThisFrame = 0

        // Extract billboard vectors from the view matrix columns.
        // This is exactly what the legacy code does with glGetFloatv(GL_MODELVIEW_MATRIX).
        //
        // Column 0 (right vector): [0], [4], [8]
        // Column 1 (up vector):    [1], [5], [9]
        val rightX = viewMatrixFloats[0]
        val rightY = viewMatrixFloats[4]
        val rightZ = viewMatrixFloats[8]
        val upX = viewMatrixFloats[1]
        val upY = viewMatrixFloats[5]
        val upZ = viewMatrixFloats[9]

        // Diagonal offsets for quad corners (matches legacy exactly):
        // offDiag1 = right + up    (top-right / bottom-left diagonal)
        // offDiag2 = right - up    (top-left / bottom-right diagonal)
        // offAnti  = up - right    (opposite of offDiag2)
        val offDiag1X = rightX + upX
        val offDiag1Y = rightY + upY
        val offDiag1Z = rightZ + upZ

        val offDiag2X = rightX - upX
        val offDiag2Y = rightY - upY
        val offDiag2Z = rightZ - upZ

        val offAntiX = upX - rightX
        val offAntiY = upY - rightY
        val offAntiZ = upZ - rightZ

        // Depth-sort key: legacy parity — GlParticleRenderer computes per-particle
        // depth from the VIEW matrix's z row (toolkit.viewMatrix44.elements[2,6,10,14],
        // GlParticleRenderer.kt:48-51), i.e. view-space Z, not clip Z.
        val zNearA = viewMatrixFloats[2]
        val zNearB = viewMatrixFloats[6]
        val zNearC = viewMatrixFloats[10]
        val zNearD = viewMatrixFloats[14]

        // Build the combined VP matrix for the shader.
        // Both inputs are RS row-major/row-vector matrices; indexing them as
        // column-major here yields (V·P)^T in column-major form, so GLSL's M*v
        // reproduces the legacy row-vector clip result (the same "straight upload"
        // trick used by the ground/model batchers).
        val vpMatrix = FloatArray(16)
        if (vpOverride != null) {
            System.arraycopy(vpOverride, 0, vpMatrix, 0, 16)
            vpMatrix[1] = -vpMatrix[1]; vpMatrix[5] = -vpMatrix[5]; vpMatrix[9] = -vpMatrix[9]; vpMatrix[13] = -vpMatrix[13]
        } else {
        for (row in 0..3) {
            for (col in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += projectionMatrixFloats[row + k * 4] * viewMatrixFloats[k + col * 4]
                }
                vpMatrix[row + col * 4] = sum
            }
        }
        // RS clip space is Y-down; OpenGL is Y-up. Negate the clip-Y output row
        // (column-major indices 1,5,9,13), matching the ground/model batchers.
        vpMatrix[1] = -vpMatrix[1]
        vpMatrix[5] = -vpMatrix[5]
        vpMatrix[9] = -vpMatrix[9]
        vpMatrix[13] = -vpMatrix[13]
        }

        // Setup GL state for particle rendering
        GLWrapper.enable(GLWrapper.Constants.BLEND)
        GLWrapper.blendFunc(GLWrapper.Constants.SRC_ALPHA, GLWrapper.Constants.ONE_MINUS_SRC_ALPHA)
        GLWrapper.depthMask(false)  // Disable depth writes (particles are translucent)
        GLWrapper.enable(GLWrapper.Constants.DEPTH_TEST)

        // Bind shader and set per-frame uniforms
        val s = shader!!
        s.bind()
        GLWrapper.uniformMatrix4fv(uViewProjectionLocation, false, vpMatrix)
        s.setUniform(uTextureLocation, 0)
        s.setUniform(uFogEnabledLocation, if (fogEnabled) 1 else 0)
        if (fogEnabled) {
            s.setUniform(uFogStartLocation, fogStart)
            s.setUniform(uFogEndLocation, fogEnd)
            s.setUniform(uFogColorLocation, fogR, fogG, fogB, 1.0f)
        }

        // Phase 1: Compute depth for each particle
        val tree = particleWrapper.particleBinaryTree.current
        var totalParticles = 0
        var minDepth = Int.MAX_VALUE
        var maxDepth = 0

        var particle = tree.child
        while (particle != null && particle !== tree) {
            if (totalParticles >= MAX_PARTICLES) break
            val pe = particle as PointEntity
            val depth = (zNearA * (pe.x shr 12) + zNearB * (pe.y shr 12) + zNearC * (pe.z shr 12) + zNearD).toInt()
            if (depth > maxDepth) maxDepth = depth
            if (depth < minDepth) minDepth = depth
            particleDepths[totalParticles++] = depth
            particle = particle.child
        }

        if (totalParticles == 0) {
            s.unbind()
            GLWrapper.depthMask(true)
            return
        }
        if (PARTICLE_POS_DIAG && (tree.child as? PointEntity)?.materialId == 744 && posDiagTick++ % 40 == 0) {
            val sb = StringBuilder("[ParticlePos] modern n=$totalParticles")
            var q = tree.child; var k = 0
            while (q != null && q !== tree && k < 4) { val pe = q as PointEntity; sb.append(" p$k=(${pe.x shr 12},${pe.y shr 12},${pe.z shr 12} sz=${pe.size shr 12} mat=${pe.materialId})"); q = q.child; k++ }
            var yMin = Int.MAX_VALUE; var yMax = Int.MIN_VALUE; var qq = tree.child; while (qq != null && qq !== tree) { val yy = (qq as PointEntity).y shr 12; if (yy < yMin) yMin = yy; if (yy > yMax) yMax = yy; qq = qq.child }; sb.append(" yRange=$yMin..$yMax")
            sb.append(" view14=${viewMatrixFloats[14]} vp13=${vpOverride?.get(13)}")
            System.err.println(sb)
        }

        // Phase 2: Compute bucket parameters
        var depthRange = maxDepth - minDepth
        val bucketShift: Int
        if (depthRange + 2 > MAX_DEPTH_BUCKETS) {
            bucketShift = 1 + BufferUtils.ilog(depthRange) - depthShift
            depthRange = (depthRange shr bucketShift) + 2
        } else {
            bucketShift = 0
            depthRange += 2
        }

        // Phase 3: Iterate particles, group by material, sort into depth buckets, render
        var particleNode = tree.child
        var particlePtr = 0
        var currentMaterialId = -2
        var currentLightAdjust = true
        var needUpdate = true

        while (particleNode != null && particleNode !== tree) {
            overflowBucketIndex = 0

            // Clear bucket counts
            for (i in 0 until depthRange) {
                bucketCounts[i] = 0
            }
            for (i in 0 until MAX_OVERFLOW_BUCKETS) {
                overflowCounts[i] = 0
            }

            // Fill buckets for current material batch
            while (particleNode != null && particleNode !== tree) {
                val pe = particleNode as PointEntity
                if (needUpdate) {
                    currentMaterialId = pe.materialId
                    currentLightAdjust = pe.lightIntensityAdjustment
                    needUpdate = false
                }

                if (particlePtr > 0 && (currentMaterialId != pe.materialId || currentLightAdjust != pe.lightIntensityAdjustment)) {
                    needUpdate = true
                    break
                }

                if (particlePtr >= totalParticles) {
                    particleNode = tree
                    break
                }
                val depth = (particleDepths[particlePtr++] - minDepth) shr bucketShift
                if (depth < MAX_DEPTH_BUCKETS) {
                    if (bucketCounts[depth] < MAX_PER_BUCKET) {
                        primaryBuckets[depth][bucketCounts[depth]++] = pe
                    } else {
                        if (bucketCounts[depth] == MAX_PER_BUCKET) {
                            if (overflowBucketIndex == MAX_OVERFLOW_BUCKETS) {
                                particleNode = particleNode.child
                                continue
                            }
                            bucketCounts[depth] += 1 + overflowBucketIndex++
                        }
                        val overflowIdx = bucketCounts[depth] - MAX_PER_BUCKET - 1
                        overflowBuckets[overflowIdx][overflowCounts[overflowIdx]++] = pe
                    }
                }
                particleNode = particleNode.child
            }

            // Bind material texture
            val textureHandle = if (currentMaterialId >= 0) lookupTextureHandle(currentMaterialId) else 0
            if (textureHandle > 0) {
                GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, textureHandle)
                s.setUniform(uTextureEnabledLocation, 1)
            } else {
                s.setUniform(uTextureEnabledLocation, 0)
            }

            // Generate billboard quads for this material batch, back-to-front
            val vb = vertexData!!
            vb.clear()
            var quadCount = 0

            for (bucketIdx in depthRange - 1 downTo 0) {
                val primaryCount = Math.min(MAX_PER_BUCKET, bucketCounts[bucketIdx])
                if (primaryCount > 0) {
                    // Primary bucket particles
                    for (pIdx in primaryCount - 1 downTo 0) {
                        val pe = primaryBuckets[bucketIdx][pIdx]!!
                        writeParticleQuad(
                            vb, pe,
                            offDiag1X, offDiag1Y, offDiag1Z,
                            offDiag2X, offDiag2Y, offDiag2Z,
                            offAntiX, offAntiY, offAntiZ
                        )
                        quadCount++
                    }

                    // Overflow bucket particles
                    if (bucketCounts[bucketIdx] > MAX_PER_BUCKET) {
                        val overflowIdx = bucketCounts[bucketIdx] - MAX_PER_BUCKET - 1
                        for (oIdx in overflowCounts[overflowIdx] - 1 downTo 0) {
                            val pe = overflowBuckets[overflowIdx][oIdx]!!
                            writeParticleQuad(
                                vb, pe,
                                offDiag1X, offDiag1Y, offDiag1Z,
                                offDiag2X, offDiag2Y, offDiag2Z,
                                offAntiX, offAntiY, offAntiZ
                            )
                            quadCount++
                        }
                    }
                }
            }

            // Upload and draw if we have quads
            if (quadCount > 0) {
                vb.flip()
                vbo!!.upload(vb)

                vao!!.bind()
                ibo!!.bind()
                GLWrapper.drawElements(
                    GLWrapper.Constants.TRIANGLES,
                    quadCount * 6,
                    GLWrapper.Constants.UNSIGNED_SHORT,
                    0
                )

                drawCallsThisFrame++
                particlesThisFrame += quadCount
            }
        }

        // Restore GL state
        s.unbind()
        GLWrapper.depthMask(true)
    }

    /**
     * Writes a single particle's billboard quad (4 vertices) into the staging buffer.
     *
     * The quad corners are computed identically to the legacy GlParticleRenderer:
     *   corner0 = center + diag1 * (-size)    UV(0,0)
     *   corner1 = center + anti  * size        UV(0,1)
     *   corner2 = center + diag1 * size        UV(1,1)
     *   corner3 = center + diag2 * size        UV(1,0)
     */
    private fun writeParticleQuad(
        buffer: ByteBuffer,
        particle: PointEntity,
        d1x: Float, d1y: Float, d1z: Float,  // diag1 = right + up
        d2x: Float, d2y: Float, d2z: Float,  // diag2 = right - up
        ax: Float, ay: Float, az: Float       // anti  = up - right
    ) {
        val color = particle.colorArgb
        val red = ((color shr 16) and 0xFF).toByte()
        val green = ((color shr 8) and 0xFF).toByte()
        val blue = (color and 0xFF).toByte()
        val alpha = ((color ushr 24) and 0xFF).toByte()

        val x = (particle.x shr 12).toFloat()
        val y = (particle.y shr 12).toFloat()
        val z = (particle.z shr 12).toFloat()
        val size = (particle.size shr 12).toFloat()

        // Corner 0: center + diag1 * (-size), UV(0, 0)
        buffer.putFloat(x + d1x * (-size))
        buffer.putFloat(y + d1y * (-size))
        buffer.putFloat(z + d1z * (-size))
        buffer.putFloat(0.0f)  // u
        buffer.putFloat(0.0f)  // v
        buffer.put(red).put(green).put(blue).put(alpha)

        // Corner 1: center + anti * size, UV(0, 1)
        buffer.putFloat(x + ax * size)
        buffer.putFloat(y + ay * size)
        buffer.putFloat(z + az * size)
        buffer.putFloat(0.0f)  // u
        buffer.putFloat(1.0f)  // v
        buffer.put(red).put(green).put(blue).put(alpha)

        // Corner 2: center + diag1 * size, UV(1, 1)
        buffer.putFloat(x + d1x * size)
        buffer.putFloat(y + d1y * size)
        buffer.putFloat(z + d1z * size)
        buffer.putFloat(1.0f)  // u
        buffer.putFloat(1.0f)  // v
        buffer.put(red).put(green).put(blue).put(alpha)

        // Corner 3: center + diag2 * size, UV(1, 0)
        buffer.putFloat(x + d2x * size)
        buffer.putFloat(y + d2y * size)
        buffer.putFloat(z + d2z * size)
        buffer.putFloat(1.0f)  // u
        buffer.putFloat(0.0f)  // v
        buffer.put(red).put(green).put(blue).put(alpha)
    }

    /**
     * Gets rendering statistics for the current frame.
     */
    fun getFrameStats(): FrameStats {
        return FrameStats(drawCallsThisFrame, particlesThisFrame)
    }

    /**
     * Releases all GPU and CPU resources.
     */
    fun destroy() {
        shader?.destroy()
        vao?.destroy()
        vbo?.destroy()
        ibo?.destroy()

        vertexData?.let { MemoryUtil.memFree(it) }
        staticIndices?.let { MemoryUtil.memFree(it) }

        shader = null
        vao = null
        vbo = null
        ibo = null
        vertexData = null
        staticIndices = null

        isInitialized = false
    }

    data class FrameStats(
        val drawCalls: Int,
        val particles: Int
    )
}
