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

class FrameBatcher {

    companion object {
        const val BYTES_PER_VERTEX = 36
        private const val INITIAL_VERTEX_CAPACITY = 262144
        private const val INITIAL_INDEX_CAPACITY = 524288
        private const val MAX_BATCHES = 256
    }

    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null

    private var shader: ShaderProgram? = null
    private var uTextureLocation = -1
    private var uTextureEnabledLocation = -1
    private var uAlphaThresholdLocation = -1

    private var vertexBuffer: ByteBuffer? = null
    private var indexBuffer: IntBuffer? = null

    private val materialIndices = mutableMapOf<Int, MutableList<Int>>()
    private val sortedTextureIds = mutableListOf<Int>()

    private var totalVertexCount = 0
    private var totalIndexCount = 0
    private var isInitialized = false
    private var isFrameActive = false

    private var drawCallsThisFrame = 0
    private var trianglesThisFrame = 0
    private var stateChangesThisFrame = 0
    private var modelsThisFrame = 0

    fun initialize() {
        if (isInitialized) return

        shader = ShaderProgram.createFromResources("/shaders/batched.vert", "/shaders/batched.frag")

        shader!!.bind()
        uTextureLocation = shader!!.getUniformLocation("uTexture")
        uTextureEnabledLocation = shader!!.getUniformLocation("uTextureEnabled")
        uAlphaThresholdLocation = shader!!.getUniformLocation("uAlphaThreshold")
        shader!!.unbind()

        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.STREAM, IndexBuffer.IndexType.UNSIGNED_INT)

        vao!!.bind()
        vbo!!.bind()
        VertexFormats.setupModelFormat(vao!!)
        ibo!!.bind()
        vao!!.unbind()

        vertexBuffer = MemoryUtil.memAlloc(INITIAL_VERTEX_CAPACITY * BYTES_PER_VERTEX)
        indexBuffer = MemoryUtil.memAllocInt(INITIAL_INDEX_CAPACITY)

        isInitialized = true
    }

    fun beginFrame() {
        check(isInitialized) { "FrameBatcher not initialized" }
        check(!isFrameActive) { "Frame already active - call endFrame() first" }

        vertexBuffer!!.clear()

        materialIndices.clear()
        sortedTextureIds.clear()

        totalVertexCount = 0
        totalIndexCount = 0
        drawCallsThisFrame = 0
        trianglesThisFrame = 0
        stateChangesThisFrame = 0
        modelsThisFrame = 0

        isFrameActive = true
    }

    fun addFace(
        textureId: Int,
        x0: Float, y0: Float, z0: Float,
        nx0: Float, ny0: Float, nz0: Float,
        u0: Float, v0: Float,
        r0: Byte, g0: Byte, b0: Byte, a0: Byte,
        x1: Float, y1: Float, z1: Float,
        nx1: Float, ny1: Float, nz1: Float,
        u1: Float, v1: Float,
        r1: Byte, g1: Byte, b1: Byte, a1: Byte,
        x2: Float, y2: Float, z2: Float,
        nx2: Float, ny2: Float, nz2: Float,
        u2: Float, v2: Float,
        r2: Byte, g2: Byte, b2: Byte, a2: Byte
    ) {
        if (!isFrameActive) return

        ensureVertexCapacity(3)

        val vb = vertexBuffer!!

        val normalizedTextureId = if (textureId <= 0) 0 else textureId
        val indices = materialIndices.getOrPut(normalizedTextureId) {
            sortedTextureIds.add(normalizedTextureId)
            mutableListOf()
        }

        val baseVertex = totalVertexCount

        vb.putFloat(x0).putFloat(y0).putFloat(z0)
        vb.putFloat(nx0).putFloat(ny0).putFloat(nz0)
        vb.putFloat(u0).putFloat(v0)
        vb.put(r0).put(g0).put(b0).put(a0)

        vb.putFloat(x1).putFloat(y1).putFloat(z1)
        vb.putFloat(nx1).putFloat(ny1).putFloat(nz1)
        vb.putFloat(u1).putFloat(v1)
        vb.put(r1).put(g1).put(b1).put(a1)

        vb.putFloat(x2).putFloat(y2).putFloat(z2)
        vb.putFloat(nx2).putFloat(ny2).putFloat(nz2)
        vb.putFloat(u2).putFloat(v2)
        vb.put(r2).put(g2).put(b2).put(a2)

        indices.add(baseVertex)
        indices.add(baseVertex + 1)
        indices.add(baseVertex + 2)

        totalVertexCount += 3
        totalIndexCount += 3
    }

    fun endModel() {
        modelsThisFrame++
    }

    fun endFrame(textureCache: (Int) -> Int) {
        check(isFrameActive) { "No active frame" }

        if (totalIndexCount == 0) {
            isFrameActive = false
            return
        }

        val sh = shader ?: return

        vertexBuffer!!.flip()

        ensureIndexCapacity(totalIndexCount)
        val ib = indexBuffer!!
        ib.clear()

        sortedTextureIds.sortBy { if (it == 0) Int.MIN_VALUE else it }

        data class BatchInfo(val startIndex: Int, val indexCount: Int)
        val batchInfos = mutableMapOf<Int, BatchInfo>()

        var currentOffset = 0
        for (textureId in sortedTextureIds) {
            val indices = materialIndices[textureId] ?: continue
            if (indices.isEmpty()) continue

            batchInfos[textureId] = BatchInfo(currentOffset, indices.size)

            for (idx in indices) {
                ib.put(idx)
            }
            currentOffset += indices.size
        }

        ib.flip()

        vbo!!.upload(vertexBuffer!!)
        ibo!!.upload(ib)

        sh.bind()
        GLWrapper.enable(GLWrapper.Constants.CULL_FACE)
        GLWrapper.cullFace(0x0405)
        GLWrapper.frontFace(0x0901)
        sh.setUniform(uTextureLocation, 0)
        sh.setUniform(uAlphaThresholdLocation, 0f)

        vao!!.bind()
        ibo!!.bind()

        var lastTextureHandle = -1

        for (textureId in sortedTextureIds) {
            val batchInfo = batchInfos[textureId] ?: continue
            if (batchInfo.indexCount == 0) continue

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

            GLWrapper.drawElements(
                TRIANGLES,
                batchInfo.indexCount,
                UNSIGNED_INT,
                batchInfo.startIndex.toLong() * 4
            )

            drawCallsThisFrame++
            trianglesThisFrame += batchInfo.indexCount / 3
        }

        vao!!.unbind()
        sh.unbind()
        isFrameActive = false
    }

    fun isActive(): Boolean = isFrameActive

    fun hasPendingGeometry(): Boolean = isFrameActive && totalVertexCount > 0

    fun getStats(): FrameStats = FrameStats(
        drawCalls = drawCallsThisFrame,
        triangles = trianglesThisFrame,
        stateChanges = stateChangesThisFrame,
        models = modelsThisFrame,
        uniqueMaterials = materialIndices.size,
        vertexBytes = totalVertexCount * BYTES_PER_VERTEX,
        indexBytes = totalIndexCount * 4
    )

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
