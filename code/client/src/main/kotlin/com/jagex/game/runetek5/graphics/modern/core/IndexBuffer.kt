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

    fun bind() {
        GLWrapper.bindBuffer(ELEMENT_ARRAY_BUFFER, id)
    }

    fun unbind() {
        GLWrapper.bindBuffer(ELEMENT_ARRAY_BUFFER, 0)
    }

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
                if (usage == BufferUsage.STREAM) GLWrapper.bufferData(ELEMENT_ARRAY_BUFFER, capacity, usage.glUsage)
                GLWrapper.bufferSubData(ELEMENT_ARRAY_BUFFER, 0, buffer)
            }
            indexCount = indices.size
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

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
                if (usage == BufferUsage.STREAM) GLWrapper.bufferData(ELEMENT_ARRAY_BUFFER, capacity, usage.glUsage)
                GLWrapper.bufferSubData(ELEMENT_ARRAY_BUFFER, 0, buffer)
            }
            indexCount = indices.size
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

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
            if (usage == BufferUsage.STREAM) GLWrapper.bufferData(ELEMENT_ARRAY_BUFFER, capacity, usage.glUsage)
            GLWrapper.bufferSubData(ELEMENT_ARRAY_BUFFER, 0, buffer)
            buffer.reset()
        }
        indexCount = count
    }

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
            if (usage == BufferUsage.STREAM) GLWrapper.bufferData(ELEMENT_ARRAY_BUFFER, capacity, usage.glUsage)
            GLWrapper.bufferSubData(ELEMENT_ARRAY_BUFFER, 0, buffer)
            buffer.reset()
        }
        indexCount = count
    }

    fun upload(data: ByteBuffer) {
        bind()
        val sizeBytes = data.remaining().toLong()
        if (sizeBytes > capacity) {
            GLWrapper.bufferData(ELEMENT_ARRAY_BUFFER, data, usage.glUsage)
            capacity = sizeBytes
        } else {
            data.mark()
            if (usage == BufferUsage.STREAM) GLWrapper.bufferData(ELEMENT_ARRAY_BUFFER, capacity, usage.glUsage)
            GLWrapper.bufferSubData(ELEMENT_ARRAY_BUFFER, 0, data)
            data.reset()
        }
        indexCount = (sizeBytes / indexType.bytesPerIndex).toInt()
    }

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

    fun getIndexCount(): Int = indexCount

    fun getGLType(): Int = indexType.glType

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

class IndexBuilder(initialCapacity: Int = 1024) {
    private var buffer = ShortArray(initialCapacity)
    private var position = 0

    private fun ensureCapacity(additional: Int) {
        if (position + additional > buffer.size) {
            val newCapacity = (buffer.size * 2).coerceAtLeast(position + additional)
            buffer = buffer.copyOf(newCapacity)
        }
    }

    fun put(index: Int): IndexBuilder {
        ensureCapacity(1)
        buffer[position++] = index.toShort()
        return this
    }

    fun putTriangle(a: Int, b: Int, c: Int): IndexBuilder {
        ensureCapacity(3)
        buffer[position++] = a.toShort()
        buffer[position++] = b.toShort()
        buffer[position++] = c.toShort()
        return this
    }

    fun putQuad(bl: Int, br: Int, tr: Int, tl: Int): IndexBuilder {
        ensureCapacity(6)
        buffer[position++] = bl.toShort()
        buffer[position++] = br.toShort()
        buffer[position++] = tr.toShort()
        buffer[position++] = bl.toShort()
        buffer[position++] = tr.toShort()
        buffer[position++] = tl.toShort()
        return this
    }

    fun getIndexCount(): Int = position

    fun build(): ShortArray {
        return buffer.copyOf(position)
    }

    fun clear() {
        position = 0
    }
}
