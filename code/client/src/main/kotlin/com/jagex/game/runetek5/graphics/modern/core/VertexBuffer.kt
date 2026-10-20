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

class VertexBuffer(
    private val usage: BufferUsage = BufferUsage.STATIC
) {
    val id: Int = GLWrapper.genBuffers()
    private var capacity: Long = 0
    private var currentSize: Long = 0

    enum class BufferUsage(val glUsage: Int) {
        STATIC(STATIC_DRAW),
        DYNAMIC(DYNAMIC_DRAW),
        STREAM(STREAM_DRAW)
    }

    fun bind() {
        GLWrapper.bindBuffer(ARRAY_BUFFER, id)
    }

    fun unbind() {
        GLWrapper.bindBuffer(ARRAY_BUFFER, 0)
    }

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

    fun upload(data: FloatBuffer) {
        bind()
        val sizeBytes = data.remaining().toLong() * 4
        if (sizeBytes > capacity) {
            GLWrapper.bufferData(ARRAY_BUFFER, data, usage.glUsage)
            capacity = sizeBytes
        } else if (usage == BufferUsage.STREAM) {
            GLWrapper.bufferData(ARRAY_BUFFER, capacity, usage.glUsage)
            data.mark()
            GLWrapper.bufferSubData(ARRAY_BUFFER, 0, data)
            data.reset()
        } else {
            data.mark()
            GLWrapper.bufferSubData(ARRAY_BUFFER, 0, data)
            data.reset()
        }
        currentSize = sizeBytes
    }

    fun upload(data: ByteBuffer) {
        bind()
        val sizeBytes = data.remaining().toLong()
        if (sizeBytes > capacity || usage == BufferUsage.STREAM) {
            GLWrapper.bufferData(ARRAY_BUFFER, data, usage.glUsage)
            if (sizeBytes > capacity) capacity = sizeBytes
        } else {
            data.mark()
            GLWrapper.bufferSubData(ARRAY_BUFFER, 0, data)
            data.reset()
        }
        currentSize = sizeBytes
    }

    fun uploadSubRange(byteOffset: Long, data: ByteBuffer) {
        if (!data.hasRemaining()) return
        bind()
        data.mark()
        BufferStaging.upload(id, ARRAY_BUFFER, byteOffset, data)
        data.reset()
        val end = byteOffset + data.remaining()
        if (end > currentSize) currentSize = end
    }

    fun allocate(sizeBytes: Long) {
        bind()
        GLWrapper.bufferData(ARRAY_BUFFER, sizeBytes, usage.glUsage)
        capacity = sizeBytes
        currentSize = 0
    }

    fun update(offset: Long, data: FloatBuffer) {
        bind()
        GLWrapper.bufferSubData(ARRAY_BUFFER, offset, data)
    }

    fun getSize(): Long = currentSize

    fun getCapacity(): Long = capacity

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

class InterleavedVertexBuilder(initialCapacity: Int = 1024) {
    private var buffer = MemoryUtil.memAllocFloat(initialCapacity)
    private var position = 0

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

    fun putColorRGBA(r: Int, g: Int, b: Int, a: Int): InterleavedVertexBuilder {
        ensureCapacity(1)
        val packed = (a and 0xFF shl 24) or (b and 0xFF shl 16) or (g and 0xFF shl 8) or (r and 0xFF)
        buffer.put(position++, java.lang.Float.intBitsToFloat(packed))
        return this
    }

    fun build(): FloatBuffer {
        buffer.position(0)
        buffer.limit(position)
        return buffer
    }

    fun getFloatCount(): Int = position

    fun clear() {
        position = 0
    }

    fun destroy() {
        MemoryUtil.memFree(buffer)
    }
}
