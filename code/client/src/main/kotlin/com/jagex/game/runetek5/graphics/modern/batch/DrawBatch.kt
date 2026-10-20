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

class DrawBatch(
    initialVertexCapacity: Int = 4096,
    initialIndexCapacity: Int = 8192
) {
    companion object {
        const val BYTES_PER_VERTEX = 36
        const val FLOATS_PER_VERTEX = 8
        const val BYTES_PER_INDEX = 2
    }

    private val vao = VertexArray()
    private val vbo = VertexBuffer(VertexBuffer.BufferUsage.DYNAMIC)
    private val ibo = IndexBuffer(IndexBuffer.BufferUsage.DYNAMIC, IndexBuffer.IndexType.UNSIGNED_SHORT)

    private var vertexData: ByteBuffer = MemoryUtil.memAlloc(initialVertexCapacity * BYTES_PER_VERTEX)
    private var indexData: ShortBuffer = MemoryUtil.memAllocShort(initialIndexCapacity)

    private var vertexCount = 0
    private var indexCount = 0
    private var isDirty = false

    var textureId: Int = 0
        private set
    var materialId: Int = 0
        private set

    init {
        vao.bind()
        vbo.bind()
        VertexFormats.setupModelFormat(vao)
        ibo.bind()
        vao.unbind()
    }

    fun configure(textureId: Int, materialId: Int = 0) {
        this.textureId = textureId
        this.materialId = materialId
    }

    fun clear() {
        vertexData.clear()
        indexData.clear()
        vertexCount = 0
        indexCount = 0
        isDirty = false
    }

    fun addVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float,
        r: Int, g: Int, b: Int, a: Int
    ): Int {
        ensureVertexCapacity(1)

        vertexData.putFloat(x)
        vertexData.putFloat(y)
        vertexData.putFloat(z)

        vertexData.putFloat(nx)
        vertexData.putFloat(ny)
        vertexData.putFloat(nz)

        vertexData.putFloat(u)
        vertexData.putFloat(v)

        vertexData.put(r.toByte())
        vertexData.put(g.toByte())
        vertexData.put(b.toByte())
        vertexData.put(a.toByte())

        isDirty = true
        return vertexCount++
    }

    fun addTriangle(a: Int, b: Int, c: Int) {
        ensureIndexCapacity(3)
        indexData.put(a.toShort())
        indexData.put(b.toShort())
        indexData.put(c.toShort())
        indexCount += 3
        isDirty = true
    }

    fun addQuad(bl: Int, br: Int, tr: Int, tl: Int) {
        ensureIndexCapacity(6)
        indexData.put(bl.toShort())
        indexData.put(br.toShort())
        indexData.put(tr.toShort())
        indexData.put(bl.toShort())
        indexData.put(tr.toShort())
        indexData.put(tl.toShort())
        indexCount += 6
        isDirty = true
    }

    fun copyVertices(source: ByteBuffer, sourceVertexCount: Int): Int {
        ensureVertexCapacity(sourceVertexCount)
        val baseIndex = vertexCount

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

    fun upload() {
        if (!isDirty || indexCount == 0) return

        vertexData.flip()
        indexData.flip()

        vbo.upload(vertexData)
        ibo.upload(indexData)

        vertexData.clear()
        indexData.clear()

        isDirty = false
    }

    fun render() {
        if (indexCount == 0) return

        upload()

        vao.bind()
        ibo.bind()
        GLWrapper.drawElements(TRIANGLES, indexCount, UNSIGNED_SHORT, 0)
    }

    fun getVertexCount(): Int = vertexCount

    fun getIndexCount(): Int = indexCount

    fun isEmpty(): Boolean = indexCount == 0

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
