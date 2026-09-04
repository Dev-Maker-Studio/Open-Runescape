package com.jagex.game.runetek5.graphics.modern.core

import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.STATIC_DRAW
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.DYNAMIC_DRAW
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.STREAM_DRAW
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_SHORT
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_INT
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

/**
 * GPU index buffer (Element Buffer Object) wrapper for modern OpenGL rendering.
 *
 * Supports both 16-bit (GL_UNSIGNED_SHORT) and 32-bit (GL_UNSIGNED_INT) indices.
 * 16-bit is more memory efficient for models with < 65536 vertices.
 */
class IndexBuffer(
    private val usage: BufferUsage = BufferUsage.STATIC,
    val indexType: IndexType = IndexType.UNSIGNED_SHORT
) {
    val id: Int = GLWrapper.genBuffers()
    private var capacity: Long = 0
    private var indexCount: Int = 0

    enum class BufferUsage(val glUsage: Int) {
        STATIC(STATIC_DRAW),
        DYNAMIC(DYNAMIC_DRAW),
        STREAM(STREAM_DRAW)
    }

    enum class IndexType(val glType: Int, val bytesPerIndex: Int) {
        UNSIGNED_SHORT(GLWrapper.Constants.UNSIGNED_SHORT, 2),
        UNSIGNED_INT(GLWrapper.Constants.UNSIGNED_INT, 4)
    }

    /**
     * Binds this buffer to GL_ELEMENT_ARRAY_BUFFER.
     */
    fun bind() {
        GLWrapper.bindBuffer(ELEMENT_ARRAY_BUFFER, id)
    }

    /**
     * Unbinds any buffer from GL_ELEMENT_ARRAY_BUFFER.
     */
    fun unbind() {
        GLWrapper.bindBuffer(ELEMENT_ARRAY_BUFFER, 0)
    }

    /**
     * Uploads short array indices (16-bit).
     */
    fun upload(indices: ShortArray) {
        require(indexType == IndexType.UNSIGNED_SHORT) { "Buffer configured for UNSIGNED_INT indices" }
        bind()
        val buffer = MemoryUtil.memAllocShort(indices.size)
        try {
            buffer.put(indices).flip()
            val sizeBytes = indices.size.toLong() * 2
            if (sizeBytes > capacity) {
                GLWrapper.bufferData(ELEMENT_ARRAY_BUFFER, buffer, usage.glUsage)
                capacity = sizeBytes
            } else {
                GLWrapper.bufferSubData(ELEMENT_ARRAY_BUFFER, 0, buffer)
            }
            indexCount = indices.size
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    /**
     * Uploads int array indices (32-bit).
     */
    fun upload(indices: IntArray) {
        require(indexType == IndexType.UNSIGNED_INT) { "Buffer configured for UNSIGNED_SHORT indices" }
        bind()
        val buffer = MemoryUtil.memAllocInt(indices.size)
        try {
            buffer.put(indices).flip()
            val sizeBytes = indices.size.toLong() * 4
            if (sizeBytes > capacity) {
                GLWrapper.bufferData(ELEMENT_ARRAY_BUFFER, buffer, usage.glUsage)
                capacity = sizeBytes
            } else {
                GLWrapper.bufferSubData(ELEMENT_ARRAY_BUFFER, 0, buffer)
            }
            indexCount = indices.size
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    /**
     * Uploads from ShortBuffer directly.
     */
    fun upload(buffer: ShortBuffer) {
        require(indexType == IndexType.UNSIGNED_SHORT) { "Buffer configured for UNSIGNED_INT indices" }
        bind()
        val count = buffer.remaining()
        val sizeBytes = count.toLong() * 2
        if (sizeBytes > capacity) {
            GLWrapper.bufferData(ELEMENT_ARRAY_BUFFER, buffer, usage.glUsage)
            capacity = sizeBytes
        } else {
            buffer.mark()
            GLWrapper.bufferSubData(ELEMENT_ARRAY_BUFFER, 0, buffer)
            buffer.reset()
        }
        indexCount = count
    }

    /**
     * Uploads from IntBuffer directly.
     */
    fun upload(buffer: IntBuffer) {
        require(indexType == IndexType.UNSIGNED_INT) { "Buffer configured for UNSIGNED_SHORT indices" }
        bind()
        val count = buffer.remaining()
        val sizeBytes = count.toLong() * 4
        if (sizeBytes > capacity) {
            GLWrapper.bufferData(ELEMENT_ARRAY_BUFFER, buffer, usage.glUsage)
            capacity = sizeBytes
        } else {
            buffer.mark()
            GLWrapper.bufferSubData(ELEMENT_ARRAY_BUFFER, 0, buffer)
            buffer.reset()
        }
        indexCount = count
    }

    /**
     * Uploads raw byte data.
     */
    fun upload(data: ByteBuffer) {
        bind()
        val sizeBytes = data.remaining().toLong()
        if (sizeBytes > capacity) {
            GLWrapper.bufferData(ELEMENT_ARRAY_BUFFER, data, usage.glUsage)
            capacity = sizeBytes
        } else {
            data.mark()
            GLWrapper.bufferSubData(ELEMENT_ARRAY_BUFFER, 0, data)
            data.reset()
        }
        indexCount = (sizeBytes / indexType.bytesPerIndex).toInt()
    }

    /**
     * Pre-allocates buffer storage.
     */
    /**
     * Uploads [buffer] at [indexOffset] (in indices), leaving the rest untouched.
     * Companion to [VertexBuffer.uploadSubRange]; requires a prior [allocate].
     */
    fun uploadSubRange(indexOffset: Int, buffer: IntBuffer) {
        require(indexType == IndexType.UNSIGNED_INT) { "Buffer configured for UNSIGNED_SHORT indices" }
        if (!buffer.hasRemaining()) return
        bind()
        val end = indexOffset + buffer.remaining()
        buffer.mark()
        BufferStaging.upload(id, ELEMENT_ARRAY_BUFFER, indexOffset.toLong() * 4, buffer)
        buffer.reset()
        if (end > indexCount) indexCount = end
    }

    fun allocate(maxIndices: Int) {
        bind()
        val sizeBytes = maxIndices.toLong() * indexType.bytesPerIndex
        GLWrapper.bufferData(ELEMENT_ARRAY_BUFFER, sizeBytes, usage.glUsage)
        capacity = sizeBytes
        indexCount = 0
    }

    /**
     * Gets the number of indices in the buffer.
     */
    fun getIndexCount(): Int = indexCount

    /**
     * Gets the GL type constant for this index buffer.
     */
    fun getGLType(): Int = indexType.glType

    /**
     * Releases GPU resources.
     * Safe to call from any thread - will silently skip if no GL context is available.
     */
    fun destroy() {
        val hasContext = try {
            GL.getCapabilities() != null
        } catch (e: IllegalStateException) {
            false
        }
        if (hasContext) {
            GLWrapper.deleteBuffers(id)
        }
    }
}

/**
 * Builder for index data.
 *
 * Efficiently builds index arrays for indexed rendering.
 */
class IndexBuilder(initialCapacity: Int = 1024) {
    private var buffer = ShortArray(initialCapacity)
    private var position = 0

    /**
     * Ensures capacity for additional indices.
     */
    private fun ensureCapacity(additional: Int) {
        if (position + additional > buffer.size) {
            val newCapacity = (buffer.size * 2).coerceAtLeast(position + additional)
            buffer = buffer.copyOf(newCapacity)
        }
    }

    /**
     * Adds a single index.
     */
    fun put(index: Int): IndexBuilder {
        ensureCapacity(1)
        buffer[position++] = index.toShort()
        return this
    }

    /**
     * Adds a triangle (3 indices).
     */
    fun putTriangle(a: Int, b: Int, c: Int): IndexBuilder {
        ensureCapacity(3)
        buffer[position++] = a.toShort()
        buffer[position++] = b.toShort()
        buffer[position++] = c.toShort()
        return this
    }

    /**
     * Adds a quad as two triangles (6 indices).
     * Vertices should be in order: bottom-left, bottom-right, top-right, top-left
     */
    fun putQuad(bl: Int, br: Int, tr: Int, tl: Int): IndexBuilder {
        ensureCapacity(6)
        // First triangle: bl, br, tr
        buffer[position++] = bl.toShort()
        buffer[position++] = br.toShort()
        buffer[position++] = tr.toShort()
        // Second triangle: bl, tr, tl
        buffer[position++] = bl.toShort()
        buffer[position++] = tr.toShort()
        buffer[position++] = tl.toShort()
        return this
    }

    /**
     * Gets the current index count.
     */
    fun getIndexCount(): Int = position

    /**
     * Builds and returns the index array.
     */
    fun build(): ShortArray {
        return buffer.copyOf(position)
    }

    /**
     * Resets builder for reuse.
     */
    fun clear() {
        position = 0
    }
}
