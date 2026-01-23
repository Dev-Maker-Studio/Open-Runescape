package com.jagex.game.runetek5.graphics.modern.batch

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.core.VertexFormats
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TRIANGLES
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_SHORT
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ShortBuffer

/**
 * A draw batch that accumulates geometry for efficient rendering.
 *
 * Batches group geometry that shares the same rendering state (material, texture, etc.)
 * to minimize draw call overhead. The batch pre-allocates buffers and grows them as needed.
 *
 * Vertex format (36 bytes per vertex):
 * - position: vec3 (12 bytes)
 * - normal: vec3 (12 bytes)
 * - texcoord: vec2 (8 bytes)
 * - color: rgba (4 bytes)
 */
class DrawBatch(
    initialVertexCapacity: Int = 4096,
    initialIndexCapacity: Int = 8192
) {
    companion object {
        const val BYTES_PER_VERTEX = 36
        const val FLOATS_PER_VERTEX = 8 // 3 pos + 3 normal + 2 tex
        const val BYTES_PER_INDEX = 2
    }

    // GPU resources
    private val vao = VertexArray()
    private val vbo = VertexBuffer(VertexBuffer.BufferUsage.DYNAMIC)
    private val ibo = IndexBuffer(IndexBuffer.BufferUsage.DYNAMIC, IndexBuffer.IndexType.UNSIGNED_SHORT)

    // CPU staging buffers
    private var vertexData: ByteBuffer = MemoryUtil.memAlloc(initialVertexCapacity * BYTES_PER_VERTEX)
    private var indexData: ShortBuffer = MemoryUtil.memAllocShort(initialIndexCapacity)

    // Tracking
    private var vertexCount = 0
    private var indexCount = 0
    private var isDirty = false

    // Batch state (what this batch is for)
    var textureId: Int = 0
        private set
    var materialId: Int = 0
        private set

    init {
        // Setup VAO with model vertex format
        vao.bind()
        vbo.bind()
        VertexFormats.setupModelFormat(vao)
        ibo.bind()
        vao.unbind()
    }

    /**
     * Configures the batch for a specific material/texture combination.
     */
    fun configure(textureId: Int, materialId: Int = 0) {
        this.textureId = textureId
        this.materialId = materialId
    }

    /**
     * Clears the batch for reuse.
     */
    fun clear() {
        vertexData.clear()
        indexData.clear()
        vertexCount = 0
        indexCount = 0
        isDirty = false
    }

    /**
     * Adds a vertex to the batch.
     *
     * @return The index of the added vertex (for use in indices)
     */
    fun addVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float,
        r: Int, g: Int, b: Int, a: Int
    ): Int {
        ensureVertexCapacity(1)

        // Position (12 bytes)
        vertexData.putFloat(x)
        vertexData.putFloat(y)
        vertexData.putFloat(z)

        // Normal (12 bytes)
        vertexData.putFloat(nx)
        vertexData.putFloat(ny)
        vertexData.putFloat(nz)

        // Texcoord (8 bytes)
        vertexData.putFloat(u)
        vertexData.putFloat(v)

        // Color RGBA (4 bytes)
        vertexData.put(r.toByte())
        vertexData.put(g.toByte())
        vertexData.put(b.toByte())
        vertexData.put(a.toByte())

        isDirty = true
        return vertexCount++
    }

    /**
     * Adds a triangle using vertex indices.
     */
    fun addTriangle(a: Int, b: Int, c: Int) {
        ensureIndexCapacity(3)
        indexData.put(a.toShort())
        indexData.put(b.toShort())
        indexData.put(c.toShort())
        indexCount += 3
        isDirty = true
    }

    /**
     * Adds a quad as two triangles.
     * Vertices should be: bottom-left, bottom-right, top-right, top-left
     */
    fun addQuad(bl: Int, br: Int, tr: Int, tl: Int) {
        ensureIndexCapacity(6)
        // First triangle: bl, br, tr
        indexData.put(bl.toShort())
        indexData.put(br.toShort())
        indexData.put(tr.toShort())
        // Second triangle: bl, tr, tl
        indexData.put(bl.toShort())
        indexData.put(tr.toShort())
        indexData.put(tl.toShort())
        indexCount += 6
        isDirty = true
    }

    /**
     * Copies raw interleaved vertex data from an existing buffer.
     *
     * @param source Source buffer with interleaved vertex data
     * @param sourceVertexCount Number of vertices to copy
     * @param baseIndex Base index to add to all indices
     * @return The base vertex index that was used
     */
    fun copyVertices(source: ByteBuffer, sourceVertexCount: Int): Int {
        ensureVertexCapacity(sourceVertexCount)
        val baseIndex = vertexCount

        // Copy vertex data
        val sourcePos = source.position()
        val bytesToCopy = sourceVertexCount * BYTES_PER_VERTEX
        for (i in 0 until bytesToCopy) {
            vertexData.put(source.get())
        }
        source.position(sourcePos + bytesToCopy)

        vertexCount += sourceVertexCount
        isDirty = true
        return baseIndex
    }

    /**
     * Copies indices with a base offset.
     */
    fun copyIndices(source: ShortBuffer, count: Int, baseVertex: Int) {
        ensureIndexCapacity(count)
        val sourcePos = source.position()
        for (i in 0 until count) {
            indexData.put((source.get().toInt() + baseVertex).toShort())
        }
        source.position(sourcePos + count)
        indexCount += count
        isDirty = true
    }

    /**
     * Uploads batch data to GPU if dirty.
     */
    fun upload() {
        if (!isDirty || indexCount == 0) return

        // Flip buffers for reading
        vertexData.flip()
        indexData.flip()

        // Upload to GPU
        vbo.upload(vertexData)
        ibo.upload(indexData)

        // Reset for next frame
        vertexData.clear()
        indexData.clear()

        isDirty = false
    }

    /**
     * Renders the batch.
     * Assumes shader is already bound and uniforms are set.
     */
    fun render() {
        if (indexCount == 0) return

        upload()

        vao.bind()
        GLWrapper.drawElements(TRIANGLES, indexCount, UNSIGNED_SHORT, 0)
    }

    /**
     * Gets the number of vertices in the batch.
     */
    fun getVertexCount(): Int = vertexCount

    /**
     * Gets the number of indices in the batch.
     */
    fun getIndexCount(): Int = indexCount

    /**
     * Returns true if the batch has no geometry.
     */
    fun isEmpty(): Boolean = indexCount == 0

    /**
     * Releases GPU resources.
     */
    fun destroy() {
        vao.destroy()
        vbo.destroy()
        ibo.destroy()
        MemoryUtil.memFree(vertexData)
        MemoryUtil.memFree(indexData)
    }

    private fun ensureVertexCapacity(additionalVertices: Int) {
        val requiredBytes = (vertexCount + additionalVertices) * BYTES_PER_VERTEX
        if (requiredBytes > vertexData.capacity()) {
            val newCapacity = (vertexData.capacity() * 2).coerceAtLeast(requiredBytes)
            val newBuffer = MemoryUtil.memAlloc(newCapacity)
            vertexData.flip()
            newBuffer.put(vertexData)
            MemoryUtil.memFree(vertexData)
            vertexData = newBuffer
        }
    }

    private fun ensureIndexCapacity(additionalIndices: Int) {
        val required = indexCount + additionalIndices
        if (required > indexData.capacity()) {
            val newCapacity = (indexData.capacity() * 2).coerceAtLeast(required)
            val newBuffer = MemoryUtil.memAllocShort(newCapacity)
            indexData.flip()
            newBuffer.put(indexData)
            MemoryUtil.memFree(indexData)
            indexData = newBuffer
        }
    }
}
