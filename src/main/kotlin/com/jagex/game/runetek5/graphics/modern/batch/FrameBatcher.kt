package com.jagex.game.runetek5.graphics.modern.batch

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.core.VertexFormats
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_2D
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TRIANGLES
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_INT
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * Frame-level geometry batcher that collects all model geometry across the entire frame
 * and renders with minimal draw calls by batching same-material faces together.
 *
 * Performance optimization strategy:
 * - Uses pooled CPU staging buffers (reused each frame, no per-draw allocation)
 * - Groups geometry by texture ID across ALL models in the scene
 * - Issues one draw call per unique texture
 * - Uses 32-bit indices to support large batches (>65536 vertices)
 * - Uses a passthrough shader (vertices are pre-transformed to NDC on CPU)
 *
 * Target: <100 draw calls per frame (down from potentially 1000+)
 */
class FrameBatcher {

    companion object {
        const val BYTES_PER_VERTEX = 36  // position(12) + normal(12) + texcoord(8) + color(4)
        private const val INITIAL_VERTEX_CAPACITY = 262144  // ~9MB vertex data
        private const val INITIAL_INDEX_CAPACITY = 524288   // ~2MB index data
        private const val MAX_BATCHES = 256
    }

    // GPU resources (single VAO/VBO/IBO for all batched geometry)
    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null

    // Own shader (passthrough for pre-transformed vertices)
    private var shader: ShaderProgram? = null
    private var uTextureLocation = -1
    private var uTextureEnabledLocation = -1
    private var uAlphaThresholdLocation = -1

    // Pooled CPU staging buffers - reused each frame
    private var vertexBuffer: ByteBuffer? = null
    private var indexBuffer: IntBuffer? = null

    // Per-material batch tracking - stores indices per material for proper batching
    // Key: textureId, Value: list of index triplets (face indices)
    private val materialIndices = mutableMapOf<Int, MutableList<Int>>()
    private val sortedTextureIds = mutableListOf<Int>()

    // Current state
    private var totalVertexCount = 0
    private var totalIndexCount = 0
    private var isInitialized = false
    private var isFrameActive = false

    // Statistics
    private var drawCallsThisFrame = 0
    private var trianglesThisFrame = 0
    private var stateChangesThisFrame = 0
    private var modelsThisFrame = 0

    /**
     * Initializes GPU resources. Must be called before use.
     */
    fun initialize() {
        if (isInitialized) return

        // Create passthrough shader for pre-transformed vertices
        shader = ShaderProgram.createFromResources("/shaders/batched.vert", "/shaders/batched.frag")

        // Cache uniform locations
        shader!!.bind()
        uTextureLocation = shader!!.getUniformLocation("uTexture")
        uTextureEnabledLocation = shader!!.getUniformLocation("uTextureEnabled")
        uAlphaThresholdLocation = shader!!.getUniformLocation("uAlphaThreshold")
        shader!!.unbind()

        // Create GPU resources with 32-bit indices for large batches
        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)  // STREAM for per-frame updates
        ibo = IndexBuffer(IndexBuffer.BufferUsage.STREAM, IndexBuffer.IndexType.UNSIGNED_INT)

        // Setup VAO with model vertex format
        vao!!.bind()
        vbo!!.bind()
        VertexFormats.setupModelFormat(vao!!)
        ibo!!.bind()
        vao!!.unbind()

        // Allocate pooled staging buffers
        vertexBuffer = MemoryUtil.memAlloc(INITIAL_VERTEX_CAPACITY * BYTES_PER_VERTEX)
        indexBuffer = MemoryUtil.memAllocInt(INITIAL_INDEX_CAPACITY)

        isInitialized = true
    }

    /**
     * Begins a new frame. Clears all batches and resets staging buffers.
     */
    fun beginFrame() {
        check(isInitialized) { "FrameBatcher not initialized" }
        check(!isFrameActive) { "Frame already active - call endFrame() first" }

        // Reset staging buffers
        vertexBuffer!!.clear()
        // Note: indexBuffer is rebuilt at endFrame time, no need to clear here

        // Clear batch tracking
        materialIndices.clear()
        sortedTextureIds.clear()

        // Reset counters
        totalVertexCount = 0
        totalIndexCount = 0
        drawCallsThisFrame = 0
        trianglesThisFrame = 0
        stateChangesThisFrame = 0
        modelsThisFrame = 0

        isFrameActive = true
    }

    /**
     * Adds geometry for a single face to the current frame batch.
     * Geometry is grouped by textureId for efficient batching.
     *
     * @param textureId The texture ID for this face (-1 or 0 for untextured)
     * @param vertices Array of 3 vertex data blocks (108 bytes total = 3 * 36 bytes)
     */
    fun addFace(
        textureId: Int,
        // Vertex 0
        x0: Float, y0: Float, z0: Float,
        nx0: Float, ny0: Float, nz0: Float,
        u0: Float, v0: Float,
        r0: Byte, g0: Byte, b0: Byte, a0: Byte,
        // Vertex 1
        x1: Float, y1: Float, z1: Float,
        nx1: Float, ny1: Float, nz1: Float,
        u1: Float, v1: Float,
        r1: Byte, g1: Byte, b1: Byte, a1: Byte,
        // Vertex 2
        x2: Float, y2: Float, z2: Float,
        nx2: Float, ny2: Float, nz2: Float,
        u2: Float, v2: Float,
        r2: Byte, g2: Byte, b2: Byte, a2: Byte
    ) {
        // Silently skip faces added outside active frame (e.g., interface models)
        if (!isFrameActive) return

        // Ensure buffer capacity
        ensureVertexCapacity(3)

        val vb = vertexBuffer!!

        // Get or create index list for this material
        val normalizedTextureId = if (textureId <= 0) 0 else textureId
        val indices = materialIndices.getOrPut(normalizedTextureId) {
            sortedTextureIds.add(normalizedTextureId)
            mutableListOf()
        }

        val baseVertex = totalVertexCount

        // Write vertex 0
        vb.putFloat(x0).putFloat(y0).putFloat(z0)
        vb.putFloat(nx0).putFloat(ny0).putFloat(nz0)
        vb.putFloat(u0).putFloat(v0)
        vb.put(r0).put(g0).put(b0).put(a0)

        // Write vertex 1
        vb.putFloat(x1).putFloat(y1).putFloat(z1)
        vb.putFloat(nx1).putFloat(ny1).putFloat(nz1)
        vb.putFloat(u1).putFloat(v1)
        vb.put(r1).put(g1).put(b1).put(a1)

        // Write vertex 2
        vb.putFloat(x2).putFloat(y2).putFloat(z2)
        vb.putFloat(nx2).putFloat(ny2).putFloat(nz2)
        vb.putFloat(u2).putFloat(v2)
        vb.put(r2).put(g2).put(b2).put(a2)

        // Store indices per-material for proper batching at endFrame
        indices.add(baseVertex)
        indices.add(baseVertex + 1)
        indices.add(baseVertex + 2)

        // Update counters
        totalVertexCount += 3
        totalIndexCount += 3
    }

    /**
     * Signals that a model has been fully added to the batch.
     * Used for statistics tracking.
     */
    fun endModel() {
        modelsThisFrame++
    }

    /**
     * Ends the frame and renders all batched geometry.
     *
     * @param textureCache Function to resolve texture ID to GL texture handle
     */
    fun endFrame(textureCache: (Int) -> Int) {
        check(isFrameActive) { "No active frame" }

        if (totalIndexCount == 0) {
            isFrameActive = false
            return
        }

        val sh = shader ?: return

        // Flip vertex buffer for reading
        vertexBuffer!!.flip()

        // Build index buffer with contiguous regions per material
        // This ensures each material's indices are grouped together for proper draw calls
        ensureIndexCapacity(totalIndexCount)
        val ib = indexBuffer!!
        ib.clear()

        // Sort texture IDs to minimize state changes (group untextured first)
        sortedTextureIds.sortBy { if (it == 0) Int.MIN_VALUE else it }

        // Track start offset and count for each material
        data class BatchInfo(val startIndex: Int, val indexCount: Int)
        val batchInfos = mutableMapOf<Int, BatchInfo>()

        var currentOffset = 0
        for (textureId in sortedTextureIds) {
            val indices = materialIndices[textureId] ?: continue
            if (indices.isEmpty()) continue

            batchInfos[textureId] = BatchInfo(currentOffset, indices.size)

            // Write this material's indices contiguously
            for (idx in indices) {
                ib.put(idx)
            }
            currentOffset += indices.size
        }

        ib.flip()

        // Upload all geometry in one go
        vbo!!.upload(vertexBuffer!!)
        ibo!!.upload(ib)

        // Bind our passthrough shader
        sh.bind()
        sh.setUniform(uTextureLocation, 0)  // Texture unit 0
        sh.setUniform(uAlphaThresholdLocation, 0f)

        // Bind VAO once for all draws
        vao!!.bind()
        ibo!!.bind()

        var lastTextureHandle = -1

        // Render each material batch
        for (textureId in sortedTextureIds) {
            val batchInfo = batchInfos[textureId] ?: continue
            if (batchInfo.indexCount == 0) continue

            // Bind texture if changed
            val textureHandle = if (textureId > 0) textureCache(textureId) else 0

            if (textureHandle != lastTextureHandle) {
                if (textureHandle > 0) {
                    GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                    GLWrapper.bindTexture(TEXTURE_2D, textureHandle)
                    sh.setUniform(uTextureEnabledLocation, 1)
                } else {
                    sh.setUniform(uTextureEnabledLocation, 0)
                }
                lastTextureHandle = textureHandle
                stateChangesThisFrame++
            }

            // Issue draw call for this material batch
            GLWrapper.drawElements(
                TRIANGLES,
                batchInfo.indexCount,
                UNSIGNED_INT,
                batchInfo.startIndex.toLong() * 4  // Byte offset (4 bytes per int index)
            )

            drawCallsThisFrame++
            trianglesThisFrame += batchInfo.indexCount / 3
        }

        // Cleanup state
        vao!!.unbind()
        sh.unbind()
        isFrameActive = false
    }

    /**
     * Returns true if a frame is currently active (between beginFrame and endFrame).
     */
    fun isActive(): Boolean = isFrameActive

    /** True when the open frame has collected geometry that has not been flushed yet. */
    fun hasPendingGeometry(): Boolean = isFrameActive && totalVertexCount > 0

    /**
     * Gets rendering statistics for the completed frame.
     */
    fun getStats(): FrameStats = FrameStats(
        drawCalls = drawCallsThisFrame,
        triangles = trianglesThisFrame,
        stateChanges = stateChangesThisFrame,
        models = modelsThisFrame,
        uniqueMaterials = materialIndices.size,
        vertexBytes = totalVertexCount * BYTES_PER_VERTEX,
        indexBytes = totalIndexCount * 4
    )

    /**
     * Releases all GPU and CPU resources.
     */
    fun destroy() {
        shader?.destroy()
        vao?.destroy()
        vbo?.destroy()
        ibo?.destroy()

        vertexBuffer?.let { MemoryUtil.memFree(it) }
        indexBuffer?.let { MemoryUtil.memFree(it) }

        shader = null
        vao = null
        vbo = null
        ibo = null
        vertexBuffer = null
        indexBuffer = null

        isInitialized = false
    }

    private fun ensureVertexCapacity(additionalVertices: Int) {
        val requiredBytes = (totalVertexCount + additionalVertices) * BYTES_PER_VERTEX
        val vb = vertexBuffer!!
        if (requiredBytes > vb.capacity()) {
            val newCapacity = (vb.capacity() * 2).coerceAtLeast(requiredBytes)
            val newBuffer = MemoryUtil.memAlloc(newCapacity)
            vb.flip()
            newBuffer.put(vb)
            MemoryUtil.memFree(vb)
            vertexBuffer = newBuffer
        }
    }

    private fun ensureIndexCapacity(additionalIndices: Int) {
        val required = totalIndexCount + additionalIndices
        val ib = indexBuffer!!
        if (required > ib.capacity()) {
            val newCapacity = (ib.capacity() * 2).coerceAtLeast(required)
            val newBuffer = MemoryUtil.memAllocInt(newCapacity)
            ib.flip()
            newBuffer.put(ib)
            MemoryUtil.memFree(ib)
            indexBuffer = newBuffer
        }
    }

    data class FrameStats(
        val drawCalls: Int,
        val triangles: Int,
        val stateChanges: Int,
        val models: Int,
        val uniqueMaterials: Int,
        val vertexBytes: Int,
        val indexBytes: Int
    ) {
        val vertexMB: Float get() = vertexBytes / (1024f * 1024f)
        val indexMB: Float get() = indexBytes / (1024f * 1024f)
        val totalMB: Float get() = vertexMB + indexMB
    }
}
