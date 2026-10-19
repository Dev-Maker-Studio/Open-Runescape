package com.jagex.game.runetek5.graphics.modern.core

import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.ARRAY_BUFFER
import org.lwjgl.opengl.GL
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.DYNAMIC_DRAW
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.STATIC_DRAW
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.STREAM_DRAW
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/**
 * GPU vertex buffer wrapper for modern OpenGL rendering.
 *
 * Supports both static and dynamic buffer usage patterns.
 * Handles memory management for buffer data uploads.
 */
class VertexBuffer(
    private val usage: BufferUsage = BufferUsage.STATIC
) {
    val id: Int = GLWrapper.genBuffers()
    private var capacity: Long = 0
    private var currentSize: Long = 0

    enum class BufferUsage(val glUsage: Int) {
        STATIC(STATIC_DRAW),    // Data set once, used many times
        DYNAMIC(DYNAMIC_DRAW),  // Data modified occasionally
        STREAM(STREAM_DRAW)     // Data modified every frame
    }

    /**
     * Binds this buffer to GL_ARRAY_BUFFER.
     */
    fun bind() {
        GLWrapper.bindBuffer(ARRAY_BUFFER, id)
    }

    /**
     * Unbinds any buffer from GL_ARRAY_BUFFER.
     */
    fun unbind() {
        GLWrapper.bindBuffer(ARRAY_BUFFER, 0)
    }

    /**
     * Uploads float data to the buffer.
     * Reallocates if necessary.
     *
     * @param data Float array to upload
     */
    fun upload(data: FloatArray) {
        bind()
        val buffer = MemoryUtil.memAllocFloat(data.size)
        try {
            buffer.put(data).flip()
            upload(buffer)
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    /**
     * Uploads float buffer data to the GPU.
     *
     * @param data FloatBuffer containing the data
     */
    fun upload(data: FloatBuffer) {
        bind()
        val sizeBytes = data.remaining().toLong() * 4
        if (sizeBytes > capacity) {
            // Reallocate buffer
            GLWrapper.bufferData(ARRAY_BUFFER, data, usage.glUsage)
            capacity = sizeBytes
        } else if (usage == BufferUsage.STREAM) {
            GLWrapper.bufferData(ARRAY_BUFFER, capacity, usage.glUsage)
            data.mark()
            GLWrapper.bufferSubData(ARRAY_BUFFER, 0, data)
            data.reset()
        } else {
            // Update existing buffer
            data.mark()
            GLWrapper.bufferSubData(ARRAY_BUFFER, 0, data)
            data.reset()
        }
        currentSize = sizeBytes
    }

    /**
     * Uploads byte data to the buffer.
     *
     * @param data ByteBuffer containing the data
     */
    fun upload(data: ByteBuffer) {
        bind()
        val sizeBytes = data.remaining().toLong()
        if (sizeBytes > capacity || usage == BufferUsage.STREAM) {
            // STREAM buffers are rewritten every frame while the previous frame's
            // draws may still read them: glBufferData orphans the old storage instead
            // of making glBufferSubData wait for (or copy around) those draws.
            GLWrapper.bufferData(ARRAY_BUFFER, data, usage.glUsage)
            if (sizeBytes > capacity) capacity = sizeBytes
        } else {
            data.mark()
            GLWrapper.bufferSubData(ARRAY_BUFFER, 0, data)
            data.reset()
        }
        currentSize = sizeBytes
    }

    /**
     * Uploads [data] at [byteOffset], leaving the rest of the buffer untouched.
     *
     * For append-only stores where only a tail range changes each frame, this
     * replaces a whole-buffer [upload] (which always writes from offset 0).
     * The caller must have reserved the range via [allocate] first — this never
     * grows the buffer.
     */
    fun uploadSubRange(byteOffset: Long, data: ByteBuffer) {
        if (!data.hasRemaining()) return
        bind()
        data.mark()
        BufferStaging.upload(id, ARRAY_BUFFER, byteOffset, data)
        data.reset()
        val end = byteOffset + data.remaining()
        if (end > currentSize) currentSize = end
    }

    /**
     * Pre-allocates buffer storage without uploading data.
     *
     * @param sizeBytes Size in bytes to allocate
     */
    fun allocate(sizeBytes: Long) {
        bind()
        GLWrapper.bufferData(ARRAY_BUFFER, sizeBytes, usage.glUsage)
        capacity = sizeBytes
        currentSize = 0
    }

    /**
     * Updates a portion of the buffer.
     *
     * @param offset Offset in bytes from start of buffer
     * @param data Data to upload
     */
    fun update(offset: Long, data: FloatBuffer) {
        bind()
        GLWrapper.bufferSubData(ARRAY_BUFFER, offset, data)
    }

    /**
     * Gets current buffer size in bytes.
     */
    fun getSize(): Long = currentSize

    /**
     * Gets buffer capacity in bytes.
     */
    fun getCapacity(): Long = capacity

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
 * Builder for interleaved vertex data.
 *
 * Helps construct vertex buffers with multiple attributes
 * (position, normal, texcoord, color) interleaved.
 */
class InterleavedVertexBuilder(initialCapacity: Int = 1024) {
    private var buffer = MemoryUtil.memAllocFloat(initialCapacity)
    private var position = 0

    /**
     * Ensures capacity for additional floats.
     */
    private fun ensureCapacity(additionalFloats: Int) {
        if (position + additionalFloats > buffer.capacity()) {
            val newCapacity = (buffer.capacity() * 2).coerceAtLeast(position + additionalFloats)
            val newBuffer = MemoryUtil.memAllocFloat(newCapacity)
            buffer.flip()
            newBuffer.put(buffer)
            MemoryUtil.memFree(buffer)
            buffer = newBuffer
        }
    }

    fun putFloat(value: Float): InterleavedVertexBuilder {
        ensureCapacity(1)
        buffer.put(position++, value)
        return this
    }

    fun putVec2(x: Float, y: Float): InterleavedVertexBuilder {
        ensureCapacity(2)
        buffer.put(position++, x)
        buffer.put(position++, y)
        return this
    }

    fun putVec3(x: Float, y: Float, z: Float): InterleavedVertexBuilder {
        ensureCapacity(3)
        buffer.put(position++, x)
        buffer.put(position++, y)
        buffer.put(position++, z)
        return this
    }

    fun putVec4(x: Float, y: Float, z: Float, w: Float): InterleavedVertexBuilder {
        ensureCapacity(4)
        buffer.put(position++, x)
        buffer.put(position++, y)
        buffer.put(position++, z)
        buffer.put(position++, w)
        return this
    }

    /**
     * Packs RGBA color (0-255 each) into a single float.
     * Suitable for GL_UNSIGNED_BYTE vertex attributes.
     */
    fun putColorRGBA(r: Int, g: Int, b: Int, a: Int): InterleavedVertexBuilder {
        ensureCapacity(1)
        val packed = (a and 0xFF shl 24) or (b and 0xFF shl 16) or (g and 0xFF shl 8) or (r and 0xFF)
        buffer.put(position++, java.lang.Float.intBitsToFloat(packed))
        return this
    }

    /**
     * Gets the buffer ready for upload.
     */
    fun build(): FloatBuffer {
        buffer.position(0)
        buffer.limit(position)
        return buffer
    }

    /**
     * Returns current vertex count (assuming stride is known).
     */
    fun getFloatCount(): Int = position

    /**
     * Resets builder for reuse.
     */
    fun clear() {
        position = 0
    }

    /**
     * Frees native memory.
     */
    fun destroy() {
        MemoryUtil.memFree(buffer)
    }
}
