package com.jagex.game.runetek5.graphics.modern.batch

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.material.TextureScroll
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.FLOAT
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_2D
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TRIANGLES
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_BYTE
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_INT
import com.jagex.game.runetek5.math.Matrix43
import org.lwjgl.opengl.ARBBindlessTexture.glGetTextureHandleARB
import org.lwjgl.opengl.ARBBindlessTexture.glMakeTextureHandleNonResidentARB
import org.lwjgl.opengl.ARBBindlessTexture.glMakeTextureHandleResidentARB
import org.lwjgl.opengl.GL33.*
import com.jagex.game.runetek5.graphics.modern.dump.FrameDump
import org.lwjgl.opengl.GL43.GL_DRAW_INDIRECT_BUFFER
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

class InstancedModelBatcher {

    companion object {
        @JvmStatic val MAX_POINT_LIGHTS = System.getProperty("render.maxlights")?.toIntOrNull()?.coerceIn(1, 256) ?: 128

        @JvmStatic val CULL_FACE = System.getProperty("render.cullface")?.equals("false", true) != true
        @JvmStatic val CULL_MODE = 0x0405
        @JvmStatic val CULL_WIND = if (System.getProperty("render.cullwind") == "cw") 0x0900 else 0x0901
        @JvmStatic val TEX_ALPHA = System.getProperty("render.texalpha")?.equals("false", true) != true

        @JvmStatic val BINDLESS_ENABLED = System.getProperty("render.bindless")?.equals("false", true) != true

        @JvmStatic val LIGHT_ONLY_DEBUG = System.getProperty("render.lightonly") != null
        const val ALPHA_CUTOFF = 0f

        const val VERTEX_STRIDE = 40

        const val INSTANCE_FLOATS = 20
        const val INSTANCE_STRIDE = INSTANCE_FLOATS * 4

        private const val MAX_INSTANCES_PER_MESH = 1024

        private const val MAX_INSTANCES_PER_FRAME = 65536

        private const val MAX_UNIQUE_MESHES = 256

        private const val MAX_TOTAL_VERTICES = 2000000

        private const val MAX_TOTAL_INDICES = 2000000

        private const val EVICT_TARGET_RATIO = 0.65f
        private const val STALE_ENTRY_FRAMES = 600L

        @JvmStatic val TRANS_SORT = System.getProperty("render.transsort")?.equals("false", true) != true

        const val INSTANCING_THRESHOLD = 2

        const val INDIRECT_INTS = 5
    }

    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var instanceVbo: VertexBuffer? = null
    private var shader: ShaderProgram? = null

    private var uTextureLocation = -1
    private var uTextureEnabledLocation = -1
    private var uCombineModeLocation = -1
    private var uTexAlphaLocation = -1
    private var texHasAlpha: (Int) -> Boolean = { true }
    private var uTextureSpeedLocation = -1
    private var uTextureTimeLocation = -1
    private var textureScrollOf: (Int) -> Int = { TextureScroll.NONE }
    private var textureTime = 0f
    private var uAlphaThresholdLocation = -1
    private var uViewProjectionLocation = -1
    private var uSunDirectionLocation = -1
    private var uPointLightCountLocation = -1
    private var uPointLightPosLocation = -1
    private var uPointLightColorLocation = -1
    private var uSunBrightnessLocation = -1
    private var uSunColorLocation = -1
    private var uContrastPosLocation = -1
    private var uContrastNegLocation = -1
    private var uViewMatrixLocation = -1
    private var uFogColorLocation = -1
    private var uFogColor2Location = -1
    private var uFogStartLocation = -1
    private var uFogEndLocation = -1
    private var uFogEnabledLocation = -1

    private var viewMatrix: FloatArray? = null
    private var fogR = 0f; private var fogG = 0f; private var fogB = 0f
    private var fogStart = 0f; private var fogEnd = 0f; private var fogOn = false

    private var sunDirX = 0f
    private var sunDirY = 1f
    private var sunDirZ = 0f
    private var sunBrightness = 0.5f
    private var sunColorR = 1f
    private var sunColorG = 1f
    private var sunColorB = 1f
    private var contrastPos = 1f
    private var contrastNeg = 1f

    private var vpMatrix: FloatArray? = null

    private var vertexBuffer: ByteBuffer? = null
    private var indexBuffer: IntBuffer? = null
    private var instanceBuffer: FloatBuffer? = null

    class MeshEntry(
        val modelId: Long,
        var vertexOffset: Int = 0,
        var vertexCount: Int = 0,
        val materialBatches: MutableList<MaterialBatchEntry> = mutableListOf()
    ) {
        var instanceData: FloatArray = FloatArray(INSTANCE_FLOATS * 16)
        var layoutStamp: Int = 0
        var instanceCount: Int = 0

        var lastUsedFrame: Long = 0

        var instanceBase: Int = 0

        var resident: Boolean = true

        var vertexCapacity: Int = 0
        var indexCapacity: Int = 0

        var indexBase: Int = 0

        fun indexSpan(): Int {
            var span = 0
            for (batch in materialBatches) span += batch.indexCount
            return span
        }

        fun resetInstances() {
            instanceCount = 0
        }

        fun ensureInstanceCapacity(instances: Int) {
            val needed = instances * INSTANCE_FLOATS
            if (needed > instanceData.size) instanceData = instanceData.copyOf(maxOf(instanceData.size * 2, needed))
        }

        fun reserveInstanceSlot(): Int {
            val base = instanceCount * INSTANCE_FLOATS
            if (base + INSTANCE_FLOATS > instanceData.size) {
                instanceData = instanceData.copyOf(instanceData.size * 2)
            }
            instanceCount++
            return base
        }
    }

    class MaterialBatchEntry(
        val materialId: Int,
        var indexOffset: Int = 0,
        var indexCount: Int = 0
    )

    private val meshEntries = LinkedHashMap<Long, MeshEntry>(MAX_UNIQUE_MESHES)

    private class DrawRef(val mesh: MeshEntry, val batch: MaterialBatchEntry)

    class BlockDraw {
        var floats = FloatArray(0)
        var count = 0
        var cmds = IntArray(0)
        var cmdCount = 0
        var mats = IntArray(0)
        var matWords = IntArray(0)
        var matWordsRevision = -1
        val entries = ArrayList<MeshEntry>(16)
        var entryLayout = IntArray(0)
        var layoutRevision = -1
        var base = 0
    }
    var storeLayoutRevision = 0
        private set
    val bindlessActive: Boolean get() = bindless
    private val blockDraws = ArrayList<BlockDraw>(256)
    private var templatesQueued = false
    private var deferredCompactVertices = -1
    private var deferredCompactIndices = -1
    @Volatile
    private var staleEvictionRequested = false

    fun requestStaleEviction() {
        staleEvictionRequested = true
    }

    private fun evictStaleEntries() {
        val horizon = frameCounter - STALE_ENTRY_FRAMES
        val stale = HashSet<Long>()
        for (entry in meshEntries.values) {
            if (entry.lastUsedFrame < horizon) {
                stale.add(entry.modelId)
                entry.resident = false
            }
        }
        if (stale.isEmpty()) return
        meshEntries.keys.removeAll(stale)
        meshesEvicted += stale.size
        val iterator = drawsByMaterial.entries.iterator()
        while (iterator.hasNext()) {
            val bucket = iterator.next()
            bucket.value.removeAll { stale.contains(it.mesh.modelId) }
            if (bucket.value.isEmpty()) iterator.remove()
        }
        materialOrderDirty = true
        activeEntries.removeAll { stale.contains(it.modelId) }
        compactGeometryStore()
    }

    fun blockDrawValid(b: BlockDraw): Boolean {
        if (b.layoutRevision != storeLayoutRevision) return false
        val es = b.entries
        for (i in es.indices) {
            val e = es[i]
            if (!e.resident || e.layoutStamp != b.entryLayout[i]) return false
        }
        return true
    }

    fun submitBlockDraw(b: BlockDraw): Boolean {
        if (!isFrameActive || !bindless || b.count == 0) return false
        blockDraws.add(b)
        templatesQueued = true
        val es = b.entries
        for (i in es.indices) es[i].lastUsedFrame = frameCounter
        instancedModels += b.count
        instancesThisBatch += b.count
        return true
    }
    private var cmdKeys = LongArray(4096)
    private var cmdEntry = arrayOfNulls<MeshEntry>(4096)
    private var cmdBatch = arrayOfNulls<MaterialBatchEntry>(4096)

    private val drawsByMaterial = HashMap<Int, ArrayList<DrawRef>>()

    private var materialOrder = IntArray(0)
    private var materialOrderDirty = true

    private val activeEntries = ArrayList<MeshEntry>(MAX_UNIQUE_MESHES)

    private var instancePackScratch = FloatArray(4096 * INSTANCE_FLOATS)

    private var translucentEntries = arrayOfNulls<MeshEntry>(256)
    private var translucentMatrices = FloatArray(256 * INSTANCE_FLOATS)
    private var translucentDepthWrite = BooleanArray(256)
    private var translucentSeq = IntArray(256)
    private var translucentKey = IntArray(256)
    private var translucentCount = 0
    private var translucentSrc = arrayOfNulls<FloatArray>(256)
    private var translucentSrcOff = IntArray(256)
    private var translucentTpl = arrayOfNulls<IntArray>(256)
    private var translucentTplOff = IntArray(256)
    private var translucentTplCount = IntArray(256)
    private var translucentMatW = arrayOfNulls<IntArray>(256)
    private var translucentMatOff = IntArray(256)

    private fun ensureTranslucentCapacity() {
        if (translucentCount < translucentEntries.size) return
        val grown = translucentEntries.size * 2
        translucentEntries = translucentEntries.copyOf(grown)
        translucentMatrices = translucentMatrices.copyOf(grown * INSTANCE_FLOATS)
        translucentDepthWrite = translucentDepthWrite.copyOf(grown)
        translucentSeq = translucentSeq.copyOf(grown)
        translucentKey = translucentKey.copyOf(grown)
        translucentSrc = translucentSrc.copyOf(grown)
        translucentSrcOff = translucentSrcOff.copyOf(grown)
        translucentTpl = translucentTpl.copyOf(grown)
        translucentTplOff = translucentTplOff.copyOf(grown)
        translucentTplCount = translucentTplCount.copyOf(grown)
        translucentMatW = translucentMatW.copyOf(grown)
        translucentMatOff = translucentMatOff.copyOf(grown)
    }

    fun appendTranslucentTemplate(entry: MeshEntry, src: FloatArray, srcOff: Int, seq: Int, depthKey: Int,
                                  tpl: IntArray, tplOff: Int, tplCount: Int, matW: IntArray, matOff: Int): Boolean {
        if (!isFrameActive || !bindless || !entry.resident) return false
        ensureTranslucentCapacity()
        val i = translucentCount++
        translucentEntries[i] = entry
        translucentDepthWrite[i] = true
        translucentSeq[i] = seq
        translucentKey[i] = depthKey
        translucentSrc[i] = src; translucentSrcOff[i] = srcOff
        translucentTpl[i] = tpl; translucentTplOff[i] = tplOff; translucentTplCount[i] = tplCount
        templatesQueued = true
        translucentMatW[i] = matW; translucentMatOff[i] = matOff
        entry.lastUsedFrame = frameCounter
        instancedModels++
        instancesThisBatch++
        return true
    }

    fun fillMaterialWords(mats: IntArray, count: Int, out: IntArray, textureCache: (Int) -> Int, combineModeOf: (Int) -> Int): Boolean {
        var complete = true
        for (k in 0 until count) {
            val slot = resolveMaterial(mats[k], textureCache, combineModeOf)
            out[k * 4] = matEntryLo[slot]; out[k * 4 + 1] = matEntryHi[slot]; out[k * 4 + 2] = matEntryFlags[slot]; out[k * 4 + 3] = matEntryScroll[slot]
            if (mats[k] >= 0 && matEntryFlags[slot] == 0) complete = false
        }
        return complete
    }
    private var translucentOrder = IntArray(256)
    private var translucentSortScratch = LongArray(256)
    private var translucentCursor = 0
    private var translucentGroupTex = IntArray(256)
    private var translucentGroupDepth = BooleanArray(256)
    private var translucentGroupFirst = IntArray(256)
    private var translucentGroupCount = IntArray(256)
    private var translucentBase = 0
    private var translucentPacked = 0
    private var instanceCapacity = MAX_INSTANCES_PER_FRAME
    private var totalVertexCount = 0
    private var totalIndexCount = 0

    private var uploadedVertexCount = 0
    private var uploadedIndexCount = 0

    private var dirtyRanges = IntArray(256)
    private var dirtyRangeCount = 0

    private var mismatchReports = 0
    private var sameFrameShapeConflicts = 0

    private fun entryMatches(entry: MeshEntry, cachedModel: ModelCache.CachedModel): Boolean {
        if (entry.vertexCount != cachedModel.vertexCount) return false
        val stored = entry.materialBatches
        val incoming = cachedModel.materialBatches
        if (stored.size != incoming.size) return false
        for (i in stored.indices) {
            if (stored[i].materialId != incoming[i].materialId) return false
            if (stored[i].indexCount != incoming[i].indexCount) return false
        }
        return true
    }

    private fun refitEntry(entry: MeshEntry, cachedModel: ModelCache.CachedModel): Boolean {
        val newVertexCount = cachedModel.vertexCount
        var newIndexCount = 0
        for (b in cachedModel.materialBatches) newIndexCount += b.indexCount
        if (newVertexCount > entry.vertexCapacity || newIndexCount > entry.indexCapacity) return false
        if (entry.lastUsedFrame == frameCounter) return false

        copyVerticesInto(cachedModel, entry.vertexOffset, newVertexCount)
        markVertexRangeDirty(entry.vertexOffset, newVertexCount)

        val stale = entry
        val iterator = drawsByMaterial.entries.iterator()
        while (iterator.hasNext()) {
            val bucket = iterator.next()
            bucket.value.removeAll { it.mesh === stale }
            if (bucket.value.isEmpty()) iterator.remove()
        }
        entry.materialBatches.clear()

        val ib = indexBuffer!!
        val baseVertex = entry.vertexOffset
        var writeIndex = entry.indexBase
        val firstIndex = writeIndex
        for (batch in cachedModel.materialBatches) {
            val matEntry = MaterialBatchEntry(
                materialId = batch.materialId,
                indexOffset = writeIndex,
                indexCount = batch.indexCount
            )
            for (i in 0 until batch.indexCount) {
                ib.put(writeIndex + i, baseVertex + batch.indices[i])
            }
            writeIndex += batch.indexCount
            entry.materialBatches.add(matEntry)
            drawsByMaterial.getOrPut(matEntry.materialId) { ArrayList() }.add(DrawRef(entry, matEntry))
        }
        materialOrderDirty = true
        markIndexRangeDirty(firstIndex, writeIndex - firstIndex)

        entry.vertexCount = newVertexCount
        entry.layoutStamp++
        cachedModel.instanceEntry = entry
        refits++
        return true
    }

    private var refits = 0
    @JvmField var translucentRunsMerged = 0L
    @JvmField var blockCommandsDrawn = 0L
    @JvmField var translucentTemplateItems = 0L
    private val MERGE_UPLOADS = System.getProperty("render.mergeuploads")?.equals("true", true) == true
    @JvmField var mergedUploadBytes = 0L

    private var dirtyIndexRanges = IntArray(128)
    private var dirtyIndexRangeCount = 0

    private fun markIndexRangeDirty(firstIndex: Int, count: Int) {
        if (count <= 0 || firstIndex >= uploadedIndexCount) return
        if (dirtyIndexRangeCount * 2 + 2 > dirtyIndexRanges.size) {
            dirtyIndexRanges = dirtyIndexRanges.copyOf(dirtyIndexRanges.size * 2)
        }
        dirtyIndexRanges[dirtyIndexRangeCount * 2] = firstIndex
        dirtyIndexRanges[dirtyIndexRangeCount * 2 + 1] = count
        dirtyIndexRangeCount++
    }

    private fun copyVerticesInto(cachedModel: ModelCache.CachedModel, firstVertex: Int, vertexCount: Int) {
        val vb = vertexBuffer!!
        val words = cachedModel.vertexWords
        if (words != null) {
            vb.clear()
            vb.position(firstVertex * VERTEX_STRIDE)
            vb.asIntBuffer().put(words, 0, vertexCount * (VERTEX_STRIDE / 4))
            vb.clear()
        } else {
            val srcData = cachedModel.vertexData
            srcData.position(0)
            srcData.limit(vertexCount * VERTEX_STRIDE)
            vb.position(firstVertex * VERTEX_STRIDE)
            vb.put(srcData)
            srcData.clear()
        }
    }

    private fun markVertexRangeDirty(firstVertex: Int, vertexCount: Int) {
        if (firstVertex >= uploadedVertexCount) return
        if (dirtyRangeCount * 2 + 2 > dirtyRanges.size) {
            dirtyRanges = dirtyRanges.copyOf(dirtyRanges.size * 2)
        }
        dirtyRanges[dirtyRangeCount * 2] = firstVertex
        dirtyRanges[dirtyRangeCount * 2 + 1] = vertexCount
        dirtyRangeCount++
    }

    private var frameCounter = 0L

    private val compactStaging = ByteArray(64 * 1024)

    private var meshesEvicted = 0

    private var capacityFallbacks = 0

    private var baseInstanceSupported = false

    private var indirectSupported = false

    private var bindless = false
    private var uDrawBaseLocation = -1
    private var drawMatVbo = 0
    private var drawMatScratch = IntArray(4096 * 4)
    private var drawMatBuffer: IntBuffer? = null
    private var drawMatCursor = 0
    private val bindlessHandles = HashMap<Int, Long>()
    private var matEntryLo = IntArray(4096)
    private var matEntryHi = IntArray(4096)
    private var matEntryFlags = IntArray(4096)
    private var matEntryScroll = IntArray(4096)
    private var matEntryStamp = IntArray(4096)
    private var matEntryPass = 0

    private fun beginMaterialPass() { matEntryPass++ }

    fun isBindless(): Boolean = bindless

    private fun textureHandle64(glTex: Int): Long {
        val known = bindlessHandles[glTex]
        if (known != null) return known
        val h = glGetTextureHandleARB(glTex)
        if (h != 0L) {
            glMakeTextureHandleResidentARB(h)
            bindlessHandles[glTex] = h
            FrameDump.handle(h, glTex)
            materialWordsRevision++
        }
        return h
    }

    fun releaseTextureHandle(glTex: Int) {
        val h = bindlessHandles.remove(glTex) ?: return
        glMakeTextureHandleNonResidentARB(h)
        materialWordsRevision++
    }

    var materialWordsRevision = 0
        private set

    private fun resolveMaterial(texId: Int, textureCache: (Int) -> Int, combineModeOf: (Int) -> Int): Int {
        val slot = texId + 1
        if (slot >= matEntryLo.size) growMaterialEntries(slot + 1)
        if (matEntryStamp[slot] != matEntryPass) {
            val glTex = if (texId >= 0) textureCache(texId) else 0
            val h = if (glTex > 0) textureHandle64(glTex) else 0L
            var f = 0
            if (h != 0L) f = 1 or ((combineModeOf(texId) and 7) shl 1) or (if (TEX_ALPHA && texHasAlpha(texId)) 16 else 0)
            matEntryLo[slot] = h.toInt(); matEntryHi[slot] = (h ushr 32).toInt(); matEntryFlags[slot] = f
            matEntryScroll[slot] = if (h != 0L) textureScrollOf(texId) else TextureScroll.NONE
            matEntryStamp[slot] = matEntryPass
        }
        return slot
    }

    private fun growMaterialEntries(minSize: Int) {
        val n = maxOf(minSize, matEntryLo.size * 2)
        matEntryLo = matEntryLo.copyOf(n); matEntryHi = matEntryHi.copyOf(n)
        matEntryFlags = matEntryFlags.copyOf(n); matEntryScroll = matEntryScroll.copyOf(n)
        matEntryStamp = matEntryStamp.copyOf(n)
    }

    private fun appendDrawMaterial(texId: Int, textureCache: (Int) -> Int, combineModeOf: (Int) -> Int) {
        val slot = resolveMaterial(texId, textureCache, combineModeOf)
        if (drawMatCursor + 4 > drawMatScratch.size) drawMatScratch = drawMatScratch.copyOf(drawMatScratch.size * 2)
        val b = drawMatScratch
        b[drawMatCursor] = matEntryLo[slot]; b[drawMatCursor + 1] = matEntryHi[slot]
        b[drawMatCursor + 2] = matEntryFlags[slot]; b[drawMatCursor + 3] = matEntryScroll[slot]
        drawMatCursor += 4
    }

    private fun uploadDrawMaterials() {
        if (drawMatBuffer!!.capacity() < drawMatCursor) {
            drawMatBuffer = MemoryUtil.memRealloc(drawMatBuffer!!, drawMatScratch.size)
        }
        val db = drawMatBuffer!!
        db.clear()
        db.put(drawMatScratch, 0, drawMatCursor)
        db.flip()
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, drawMatVbo)
        glBufferData(GL_SHADER_STORAGE_BUFFER, db, GL_STREAM_DRAW)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, drawMatVbo)
    }
    private var indirectVbo = 0
    private var indirectBuffer: IntBuffer? = null

    private var indirectScratch = IntArray(MAX_UNIQUE_MESHES * 8 * INDIRECT_INTS)

    private fun repointInstanceAttributes(base: Int) {
        val byteOffset = base.toLong() * INSTANCE_STRIDE
        instanceVbo!!.bind()
        GLWrapper.vertexAttribPointer(5, 4, FLOAT, false, INSTANCE_STRIDE, byteOffset)
        GLWrapper.vertexAttribPointer(6, 4, FLOAT, false, INSTANCE_STRIDE, byteOffset + 16)
        GLWrapper.vertexAttribPointer(7, 4, FLOAT, false, INSTANCE_STRIDE, byteOffset + 32)
        GLWrapper.vertexAttribPointer(8, 4, FLOAT, false, INSTANCE_STRIDE, byteOffset + 48)
        GLWrapper.vertexAttribPointer(9, 4, FLOAT, false, INSTANCE_STRIDE, byteOffset + 64)
    }

    private var isInitialized = false
    private var isFrameActive = false
    private var uClipPlaneLocation = -1
    private var reflectionCommands = 0

    private var instancedDrawCalls = 0
    private var instancedTriangles = 0
    private var instancedModels = 0
    private var instancedMeshesReused = 0

    private var instancesThisBatch = 0

    private var timeGeomUpload = 0L
    private var timeInstanceUpload = 0L
    private var timeDraw = 0L

    fun initialize() {
        if (isInitialized) return

        val caps0 = org.lwjgl.opengl.GL.getCapabilities()
        val bindlessCaps = caps0.GL_ARB_bindless_texture && caps0.GL_ARB_shader_draw_parameters &&
            (caps0.OpenGL43 || caps0.GL_ARB_shader_storage_buffer_object)
        bindless = false
        if (BINDLESS_ENABLED && bindlessCaps) {
            try {
                shader = createInstancedShader(bindless = true)
                bindless = true
            } catch (t: Throwable) {
                System.err.println("[InstancedModelBatcher] bindless shader failed, using per-material binding: $t")
                shader = null
            }
        }
        if (shader == null) shader = createInstancedShader(bindless = false)

        shader!!.bind()
        uDrawBaseLocation = shader!!.getUniformLocation("uDrawBase")
        uTextureLocation = shader!!.getUniformLocation("uTexture")
        uTextureEnabledLocation = shader!!.getUniformLocation("uTextureEnabled")
        uCombineModeLocation = shader!!.getUniformLocation("uCombineMode")
        uTexAlphaLocation = shader!!.getUniformLocation("uTexAlpha")
        uTextureSpeedLocation = shader!!.getUniformLocation("uTextureSpeed")
        uTextureTimeLocation = shader!!.getUniformLocation("uTextureTime")
        uAlphaThresholdLocation = shader!!.getUniformLocation("uAlphaThreshold")
        uViewProjectionLocation = shader!!.getUniformLocation("uViewProjection")
        uClipPlaneLocation = shader!!.getUniformLocation("uClipPlane")
        uSunDirectionLocation = shader!!.getUniformLocation("uSunDirection")
        uPointLightCountLocation = shader!!.getUniformLocation("uPointLightCount")
        uPointLightPosLocation = shader!!.getArrayUniformLocation("uPointLightPos")
        uPointLightColorLocation = shader!!.getArrayUniformLocation("uPointLightColor")
        uSunBrightnessLocation = shader!!.getUniformLocation("uSunBrightness")
        uSunColorLocation = shader!!.getUniformLocation("uSunColor")
        uContrastPosLocation = shader!!.getUniformLocation("uContrastPos")
        uContrastNegLocation = shader!!.getUniformLocation("uContrastNeg")
        uViewMatrixLocation = shader!!.getUniformLocation("uViewMatrix")
        uFogColorLocation = shader!!.getUniformLocation("uFogColor")
        uFogColor2Location = shader!!.getUniformLocation("uFogColor2")
        uFogStartLocation = shader!!.getUniformLocation("uFogStart")
        uFogEndLocation = shader!!.getUniformLocation("uFogEnd")
        uFogEnabledLocation = shader!!.getUniformLocation("uFogEnabled")
        shader!!.unbind()

        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.STREAM, IndexBuffer.IndexType.UNSIGNED_INT)
        instanceVbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)

        val caps = org.lwjgl.opengl.GL.getCapabilities()
        baseInstanceSupported = caps.OpenGL42 || caps.GL_ARB_base_instance
        indirectSupported = baseInstanceSupported && (caps.OpenGL43 || caps.GL_ARB_multi_draw_indirect)
        if (indirectSupported) {
            indirectVbo = glGenBuffers()
            indirectBuffer = MemoryUtil.memAllocInt(indirectScratch.size)
        }
        if (bindless && !indirectSupported) bindless = false
        if (bindless) {
            drawMatVbo = glGenBuffers()
            drawMatBuffer = MemoryUtil.memAllocInt(drawMatScratch.size)
        }
        println("[InstancedModelBatcher] baseInstance=$baseInstanceSupported multiDrawIndirect=$indirectSupported bindless=$bindless uniforms[pos=$uPointLightPosLocation col=$uPointLightColorLocation count=$uPointLightCountLocation]")

        setupVAO()

        vbo!!.allocate(MAX_TOTAL_VERTICES.toLong() * VERTEX_STRIDE)
        ibo!!.allocate(MAX_TOTAL_INDICES)

        vertexBuffer = MemoryUtil.memAlloc(MAX_TOTAL_VERTICES * VERTEX_STRIDE)
        indexBuffer = MemoryUtil.memAllocInt(MAX_TOTAL_INDICES)
        instanceBuffer = MemoryUtil.memAllocFloat(MAX_INSTANCES_PER_FRAME * INSTANCE_FLOATS)

        isInitialized = true
    }

    private fun setupVAO() {
        val va = vao!!

        va.bind()

        vbo!!.bind()
        GLWrapper.vertexAttribPointer(0, 3, FLOAT, false, VERTEX_STRIDE, 0)
        GLWrapper.enableVertexAttribArray(0)
        GLWrapper.vertexAttribPointer(1, 3, FLOAT, false, VERTEX_STRIDE, 12)
        GLWrapper.enableVertexAttribArray(1)
        GLWrapper.vertexAttribPointer(2, 2, FLOAT, false, VERTEX_STRIDE, 24)
        GLWrapper.enableVertexAttribArray(2)
        GLWrapper.vertexAttribPointer(3, 4, UNSIGNED_BYTE, true, VERTEX_STRIDE, 32)
        GLWrapper.enableVertexAttribArray(3)

        instanceVbo!!.bind()
        GLWrapper.vertexAttribPointer(5, 4, FLOAT, false, INSTANCE_STRIDE, 0)
        GLWrapper.enableVertexAttribArray(5)
        GLWrapper.vertexAttribDivisor(5, 1)
        GLWrapper.vertexAttribPointer(6, 4, FLOAT, false, INSTANCE_STRIDE, 16)
        GLWrapper.enableVertexAttribArray(6)
        GLWrapper.vertexAttribDivisor(6, 1)
        GLWrapper.vertexAttribPointer(7, 4, FLOAT, false, INSTANCE_STRIDE, 32)
        GLWrapper.enableVertexAttribArray(7)
        GLWrapper.vertexAttribDivisor(7, 1)
        GLWrapper.vertexAttribPointer(8, 4, FLOAT, false, INSTANCE_STRIDE, 48)
        GLWrapper.enableVertexAttribArray(8)
        GLWrapper.vertexAttribDivisor(8, 1)
        GLWrapper.vertexAttribPointer(9, 4, FLOAT, false, INSTANCE_STRIDE, 64)
        GLWrapper.enableVertexAttribArray(9)
        GLWrapper.vertexAttribDivisor(9, 1)

        ibo!!.bind()

        va.unbind()
    }

    private fun createInstancedShader(bindless: Boolean): ShaderProgram {
        val vertexSource = """
            ${if (bindless) "#version 430 core\n            #extension GL_ARB_shader_draw_parameters : require" else "#version 330 core"}
            #define MAX_POINT_LIGHTS $MAX_POINT_LIGHTS

            // Per-vertex attributes
            layout(location = 0) in vec3 aPosition;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec2 aTexCoord;
            layout(location = 3) in vec4 aColor;

            // Per-instance attributes (mat4 = 4 x vec4, divisor = 1)
            layout(location = 5) in vec4 aModelMatCol0;
            layout(location = 6) in vec4 aModelMatCol1;
            layout(location = 7) in vec4 aModelMatCol2;
            layout(location = 8) in vec4 aModelMatCol3;
            layout(location = 9) in vec4 aLightIdx;

            uniform mat4 uViewProjection;
            uniform vec4 uClipPlane;

            // Lighting uniforms
            uniform vec3 uSunDirection;
            uniform float uSunBrightness;
            uniform vec3 uSunColor;
            uniform float uContrastPos;
            uniform float uContrastNeg;

            // Legacy point lights (OpenGlToolkit.renderLights): GL_LIGHT2+ with
            // GL_POSITION, GL_DIFFUSE = colour * (transparency/255) and
            // GL_QUADRATIC_ATTENUATION = 1/size2d^2 (constant/linear terms are 0).
            // So att = 1/(kq*d^2). Legacy never enables GL_NORMALIZE, so the scaled
            // normal (length 768/contrast) feeds the diffuse term as-is, same as the
            // sun term above.
            uniform int uPointLightCount;
            uniform vec4 uPointLightPos[MAX_POINT_LIGHTS];   // xyz world, w = kq
            uniform vec3 uPointLightColor[MAX_POINT_LIGHTS]; // already * transparency/255

            // Linear fog, matching legacy: glFogi(GL_FOG_MODE, GL_LINEAR) with
            // GL_FOG_START/END from fogStartPosition/fogEndPosition. setupMatrixRendering
            // enables fog for every model draw, so omitting it here left all geometry
            // un-fogged and measurably brighter than legacy.
            uniform mat4 uViewMatrix;
            uniform vec3 uFogColor;
            uniform float uFogStart;
            uniform float uFogEnd;
            uniform int uFogEnabled;

            out vec2 vTexCoord;
            out vec4 vColor;
            out float vFogFactor;
            ${if (bindless) "flat out int vDrawId;" else ""}

            void main() {
                ${if (bindless) "vDrawId = gl_DrawIDARB;" else ""}
                // Reconstruct model matrix from per-instance attributes
                mat4 modelMat = mat4(aModelMatCol0, aModelMatCol1, aModelMatCol2, aModelMatCol3);

                vec4 worldPos = modelMat * vec4(aPosition, 1.0);
                gl_Position = uViewProjection * worldPos;
                gl_ClipDistance[0] = dot(worldPos, uClipPlane);

                vTexCoord = aTexCoord;

                // GPU-side lighting calculation matching legacy formula exactly
                mat3 normalMat = mat3(modelMat);
                vec3 worldNormal = normalMat * aNormal; // length carries 768/contrast — do NOT normalise

                float dotProduct = dot(uSunDirection, worldNormal);
                float lightIntensity = uSunBrightness + dotProduct * (dotProduct < 0.0 ? uContrastNeg : uContrastPos);

                vec3 litRGB = aColor.rgb * uSunColor * lightIntensity;
                ${if (LIGHT_ONLY_DEBUG) "litRGB = vec3(0.0);" else ""}

                // Point lights add on top of the sun term, exactly as fixed-function
                // accumulates GL_LIGHT2+ before the final clamp.
                // Only the lights legacy associated with THIS entity (copyLights ->
                // getActiveLights, max 4). Looping every scene light instead cost 5.2ms
                // of GPU time with 117 lights, and lit geometry the light never reached.
                for (int k = 0; k < 4; ++k) {
                    int i = int(aLightIdx[k]);
                    if (i < 0) continue;
                    vec3 toLight = uPointLightPos[i].xyz - worldPos.xyz;
                    float d2 = dot(toLight, toLight);
                    float att = 1.0 / max(uPointLightPos[i].w * d2, 1e-6); // legacy: kc=0, kl=0, kq=1/size^2
                    float ndotl = max(dot(worldNormal, normalize(toLight)), 0.0);
                    litRGB += aColor.rgb * uPointLightColor[i] * (att * ndotl);
                }

                litRGB = clamp(litRGB, 0.0, 1.0);
                ${if (LIGHT_ONLY_DEBUG) """
                {
                    int i0 = int(aLightIdx[0]);
                    if (i0 >= 0) {
                        vec3 tl = uPointLightPos[i0].xyz - worldPos.xyz;
                        float a0 = 1.0 / max(uPointLightPos[i0].w * dot(tl, tl), 1e-6);
                        float c0 = dot(normalize(worldNormal), normalize(tl));
                        litRGB = uPointLightColor[i0] * 2.0 + vec3(0.0, 0.0, 0.5 + 0.5 * c0) * 0.0;
                    } else {
                        litRGB = vec3(0.0, 0.0, 0.0);
                    }
                }
                """ else ""}

                vColor = vec4(litRGB, aColor.a);

                // GL linear fog factor f = (end - z) / (end - start), z = eye distance.
                if (uFogEnabled != 0) {
                    float eyeZ = -(uViewMatrix * worldPos).z;
                    vFogFactor = clamp((uFogEnd - eyeZ) / max(uFogEnd - uFogStart, 1e-4), 0.0, 1.0);
                } else {
                    vFogFactor = 1.0;
                }
            }
        """.trimIndent()

        val fragmentSource = """
            ${if (bindless) "#version 430 core\n            #extension GL_ARB_bindless_texture : require" else "#version 330 core"}

            in vec2 vTexCoord;
            in vec4 vColor;
            in float vFogFactor;
            uniform vec3 uFogColor2;

            uniform float uAlphaThreshold;
            uniform float uTextureTime;
            ${if (bindless) """
            // Per-draw material: (texture handle lo, hi, flags, scroll). flags bit0 = textured,
            // bits1-3 = legacy combine mode, bit4 = texture alpha rule; scroll packs the
            // material's textureSpeedU (bits 0-7) and textureSpeedV (bits 8-15) bytes.
            // Indexed by the multi-draw command index plus uDrawBase (the group's first command).
            layout(std430, binding = 0) readonly buffer DrawMaterials { uvec4 drawMat[]; };
            flat in int vDrawId;
            uniform int uDrawBase;
            """ else """
            uniform sampler2D uTexture;
            uniform bool uTextureEnabled;
            // Legacy per-material texture combine mode (see OptimizedFrameBatcher /
            // groundTextured.frag). REPLACE(1) ignores the per-face vColor — required
            // or textured faces with junk face colors render tinted (green-quad bug).
            uniform int uCombineMode;
            uniform bool uTexAlpha;
            uniform vec2 uTextureSpeed;
            """}

            out vec4 fragColor;

            void main() {
                vec4 color;
                ${if (bindless) """
                uvec4 m = drawMat[uDrawBase + vDrawId];
                bool uTextureEnabled = (m.z & 1u) != 0u;
                int uCombineMode = int((m.z >> 1u) & 7u);
                bool uTexAlpha = (m.z & 16u) != 0u;
                vec2 uTextureSpeed = vec2(int(m.w << 24u) >> 24, int(m.w << 16u) >> 24);
                """ else ""}
                if (uTextureEnabled) {
                    vec2 scroll = uTextureTime * uTextureSpeed / 64.0;
                    vec2 texCoord = vTexCoord + scroll - trunc(scroll);
                    ${if (bindless) "vec4 texColor = texture(sampler2D(m.xy), texCoord);" else "vec4 texColor = texture(uTexture, texCoord);"}
                    if (uCombineMode == 1) {
                        color = texColor;
                    } else if (uCombineMode == 3) {
                        color = vec4(clamp(texColor.rgb + vColor.rgb, 0.0, 1.0), texColor.a * vColor.a);
                    } else if (uCombineMode == 4) {
                        color = clamp(texColor - vColor, 0.0, 1.0);
                    } else {
                        color = texColor * vColor;
                    }
                    // Legacy model faces take their alpha from the face/vertex colour;
                    // folding the texture's alpha channel in here made fully-opaque
                    // geometry translucent (Canifis lampposts: vertex alpha 1.0,
                    // texture alpha ~0.55). -Drender.texalpha=true restores the old
                    // behaviour for comparison.
                    // Materials whose texture carries no alpha channel (loaded through
                    // getPixelsRgb) take alpha from the face colour; the rest use the
                    // texture's own alpha, as fixed-function does.
                    if (!uTexAlpha) color.a = vColor.a;
                } else {
                    color = vColor;
                }

                if (color.a <= uAlphaThreshold) {
                    discard;
                }

                // GL fog: C = f*Cfrag + (1-f)*Cfog. Alpha is not fogged.
                fragColor = vec4(mix(uFogColor2, color.rgb, vFogFactor), color.a);
            }
        """.trimIndent()

        return ShaderProgram.createFromSource(vertexSource, fragmentSource)
    }

    fun vpElement(i: Int): Float = vpMatrix?.get(i) ?: Float.NaN

    fun setViewProjection(vp: FloatArray) {
        val flipped = vp.copyOf()
        flipped[1] = -flipped[1]
        flipped[5] = -flipped[5]
        flipped[9] = -flipped[9]
        flipped[13] = -flipped[13]
        this.vpMatrix = flipped
    }

    fun setFog(view: FloatArray, enabled: Boolean, colorRgb: Int, start: Float, end: Float) {
        val snapshot = viewMatrix?.takeIf { it !== view && it.size == view.size } ?: FloatArray(view.size)
        view.copyInto(snapshot)
        viewMatrix = snapshot
        fogOn = enabled
        fogR = ((colorRgb shr 16) and 0xff) / 255f
        fogG = ((colorRgb shr 8) and 0xff) / 255f
        fogB = (colorRgb and 0xff) / 255f
        fogStart = start
        fogEnd = end
    }

    private var pointLightCount = 0
    private val pointLightPos = FloatArray(MAX_POINT_LIGHTS * 4)
    private val pointLightColor = FloatArray(MAX_POINT_LIGHTS * 3)

    fun setTextureAlphaPredicate(p: (Int) -> Boolean) { texHasAlpha = p }

    fun setTextureScrollLookup(packedSpeedOf: (Int) -> Int) { textureScrollOf = packedSpeedOf }

    fun setTextureTime(seconds: Float) { textureTime = seconds }

    private fun setTextureSpeedUniform(sh: ShaderProgram, texId: Int) {
        val packed = textureScrollOf(texId)
        sh.setUniform(uTextureSpeedLocation, TextureScroll.speedU(packed), TextureScroll.speedV(packed))
    }

    fun setPointLights(count: Int, pos: FloatArray, color: FloatArray) {
        val n = if (count > MAX_POINT_LIGHTS) MAX_POINT_LIGHTS else count
        pointLightCount = n
        if (n > 0) {
            System.arraycopy(pos, 0, pointLightPos, 0, n * 4)
            System.arraycopy(color, 0, pointLightColor, 0, n * 3)
        }
    }

    fun setLighting(
        sunDirX: Float, sunDirY: Float, sunDirZ: Float,
        brightness: Float,
        colorR: Float, colorG: Float, colorB: Float,
        contrastPositive: Float, contrastNegative: Float
    ) {
        this.sunDirX = sunDirX
        this.sunDirY = sunDirY
        this.sunDirZ = sunDirZ
        this.sunBrightness = brightness
        this.sunColorR = colorR
        this.sunColorG = colorG
        this.sunColorB = colorB
        this.contrastPos = contrastPositive
        this.contrastNeg = contrastNegative
    }

    fun drawReflection(mirroredViewProjection: FloatArray, clipPlane: FloatArray) {
        if (!bindless || reflectionCommands == 0) return
        val sh = shader ?: return
        val vp = vpMatrix ?: return
        sh.bind()
        GLWrapper.enable(GLWrapper.Constants.DEPTH_TEST)
        GLWrapper.depthFunc(GLWrapper.Constants.LEQUAL)
        if (CULL_FACE) {
            GLWrapper.enable(GLWrapper.Constants.CULL_FACE)
            GLWrapper.cullFace(CULL_MODE)
            GLWrapper.frontFace(if (CULL_WIND == 0x0900) 0x0901 else 0x0900)
        }
        glUniformMatrix4fv(uViewProjectionLocation, false, mirroredViewProjection)
        glUniform4f(uClipPlaneLocation, clipPlane[0], clipPlane[1], clipPlane[2], clipPlane[3])
        vao!!.bind()
        ibo!!.bind()
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectVbo)
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, drawMatVbo)
        sh.setUniform(uDrawBaseLocation, 0)
        FrameDump.multiIndirect(TRIANGLES, UNSIGNED_INT, 0L, reflectionCommands, 0)
        vao!!.unbind()
        glUniformMatrix4fv(uViewProjectionLocation, false, vp)
        glUniform4f(uClipPlaneLocation, 0f, 0f, 0f, 0f)
        GLWrapper.frontFace(CULL_WIND)
        sh.unbind()
    }

    fun beginFrame() {
        check(isInitialized) { "InstancedModelBatcher not initialized" }
        check(!isFrameActive) { "Frame already active" }

        for (i in activeEntries.indices) activeEntries[i].resetInstances()
        activeEntries.clear()
        reflectionCommands = 0
        blockDraws.clear()
        instancesThisBatch = 0
        for (i in 0 until translucentCount) translucentEntries[i] = null
        translucentCount = 0
        translucentPacked = 0
        translucentCursor = 0
        templatesQueued = false
        if (staleEvictionRequested) {
            staleEvictionRequested = false
            evictStaleEntries()
        }
        if (deferredCompactVertices >= 0) {
            evictAndCompact(deferredCompactVertices, deferredCompactIndices)
            deferredCompactVertices = -1
            deferredCompactIndices = -1
        }

        isFrameActive = true
    }

    fun resetFrameStats() {
        instancedDrawCalls = 0
        instancedTriangles = 0
        instancedModels = 0
        instancedMeshesReused = 0
        meshesEvicted = 0
        capacityFallbacks = 0
        refits = 0
        timeGeomUpload = 0L
        timeInstanceUpload = 0L
        timeDraw = 0L
        frameCounter++
    }

    fun addInstance(
        cachedModel: ModelCache.CachedModel,
        modelId: Long,
        modelMatrix: Matrix43,
        lightIdx: FloatArray? = null,
        geometryDirty: Boolean = false,
        translucent: Boolean = false,
        depthWrite: Boolean = true,
        seq: Int = 0,
        depthKey: Int = 0
    ): Boolean {
        if (!isFrameActive) return false

        var entry = cachedModel.instanceEntry as? MeshEntry
        var shapeKnownGood = false
        if (entry != null && (!entry.resident || entry.modelId != modelId)) {
            cachedModel.instanceEntry = null
            entry = null
        } else if (entry != null && !geometryDirty) {
            shapeKnownGood = true
        }
        if (entry == null) {
            entry = meshEntries[modelId]
            if (entry != null) cachedModel.instanceEntry = entry
        }

        if (!shapeKnownGood && entry != null && !entryMatches(entry, cachedModel) && refitEntry(entry, cachedModel)) {
            entry.lastUsedFrame = frameCounter
        } else if (!shapeKnownGood && entry != null && !entryMatches(entry, cachedModel)) {
            if (entry.lastUsedFrame == frameCounter) {
                sameFrameShapeConflicts++
                return false
            }
            if (mismatchReports < 20) {
                mismatchReports++
                println("[InstancedModelBatcher] resident entry no longer matches key=$modelId " +
                    "(stored=${entry.vertexCount}v/${entry.materialBatches.size}batches " +
                    "incoming=${cachedModel.vertexCount}v/${cachedModel.materialBatches.size}batches) — re-appending")
            }
            cachedModel.instanceEntry = null
            if (meshEntries[modelId] === entry) meshEntries.remove(modelId)
            val stale = entry
            val iterator = drawsByMaterial.entries.iterator()
            while (iterator.hasNext()) {
                val bucket = iterator.next()
                bucket.value.removeAll { it.mesh === stale }
                if (bucket.value.isEmpty()) iterator.remove()
            }
            materialOrderDirty = true
            activeEntries.removeAll { it === stale }
            stale.resident = false
            entry = null
        }

        if (entry != null && geometryDirty && entry.lastUsedFrame == frameCounter) {
            sameFrameShapeConflicts++
            return false
        }

        if (entry != null && geometryDirty) {
            copyVerticesInto(cachedModel, entry.vertexOffset, entry.vertexCount)
            markVertexRangeDirty(entry.vertexOffset, entry.vertexCount)
        }

        if (entry == null) {
            val vertexCount = cachedModel.vertexCount

            var meshIndexCount = 0
            for (batch in cachedModel.materialBatches) {
                meshIndexCount += batch.indexCount
            }

            if (totalVertexCount + vertexCount > MAX_TOTAL_VERTICES ||
                totalIndexCount + meshIndexCount > MAX_TOTAL_INDICES
            ) {
                if (templatesQueued) {
                    deferredCompactVertices = maxOf(deferredCompactVertices, vertexCount)
                    deferredCompactIndices = maxOf(deferredCompactIndices, meshIndexCount)
                    capacityFallbacks++
                    return false
                }
                if (!evictAndCompact(vertexCount, meshIndexCount)) {
                    capacityFallbacks++
                    return false
                }
            }

            entry = MeshEntry(
                modelId = modelId,
                vertexOffset = totalVertexCount,
                vertexCount = vertexCount
            )
            entry.vertexCapacity = vertexCount
            entry.indexCapacity = meshIndexCount
            entry.indexBase = totalIndexCount

            copyVerticesInto(cachedModel, entry.vertexOffset, vertexCount)

            for (batch in cachedModel.materialBatches) {
                val matEntry = MaterialBatchEntry(
                    materialId = batch.materialId,
                    indexOffset = totalIndexCount,
                    indexCount = batch.indexCount
                )

                val ib = indexBuffer!!
                val baseVertex = entry.vertexOffset
                for (i in 0 until batch.indexCount) {
                    ib.put(totalIndexCount + i, baseVertex + batch.indices[i])
                }

                entry.materialBatches.add(matEntry)
                totalIndexCount += batch.indexCount
            }

            totalVertexCount += vertexCount
            meshEntries[modelId] = entry
            cachedModel.instanceEntry = entry
            for (matEntry in entry.materialBatches) {
                drawsByMaterial.getOrPut(matEntry.materialId) { ArrayList() }.add(DrawRef(entry, matEntry))
            }
            materialOrderDirty = true
        } else {
            instancedMeshesReused++
        }

        entry.lastUsedFrame = frameCounter

        if (translucent) {
            appendTranslucent(entry, modelMatrix, lightIdx, depthWrite, seq, depthKey)
            instancedModels++
            instancesThisBatch++
            return true
        }

        if (entry.instanceCount >= MAX_INSTANCES_PER_MESH) return false

        if (entry.instanceCount == 0) activeEntries.add(entry)

        val base = entry.reserveInstanceSlot()
        val data = entry.instanceData
        data[base] = modelMatrix.xA
        data[base + 1] = modelMatrix.yA
        data[base + 2] = modelMatrix.zA
        data[base + 3] = 0f
        data[base + 4] = modelMatrix.xB
        data[base + 5] = modelMatrix.yB
        data[base + 6] = modelMatrix.zB
        data[base + 7] = 0f
        data[base + 8] = modelMatrix.xC
        data[base + 9] = modelMatrix.yC
        data[base + 10] = modelMatrix.zC
        data[base + 11] = 0f
        data[base + 12] = modelMatrix.xD
        data[base + 13] = modelMatrix.yD
        data[base + 14] = modelMatrix.zD
        data[base + 15] = 1f

        if (lightIdx != null) {
            data[base + 16] = lightIdx[0]
            data[base + 17] = lightIdx[1]
            data[base + 18] = lightIdx[2]
            data[base + 19] = lightIdx[3]
        } else {
            data[base + 16] = -1f
            data[base + 17] = -1f
            data[base + 18] = -1f
            data[base + 19] = -1f
        }

        instancedModels++
        instancesThisBatch++

        return true
    }

    fun canAddPacked(entry: MeshEntry, count: Int): Boolean =
        isFrameActive && entry.resident && entry.instanceCount + count <= MAX_INSTANCES_PER_MESH

    fun addInstancesPacked(entry: MeshEntry, floats: FloatArray, offset: Int, count: Int): Boolean {
        if (count <= 0) return true
        if (!canAddPacked(entry, count)) return false
        entry.lastUsedFrame = frameCounter
        if (entry.instanceCount == 0) activeEntries.add(entry)
        entry.ensureInstanceCapacity(entry.instanceCount + count)
        System.arraycopy(floats, offset, entry.instanceData, entry.instanceCount * INSTANCE_FLOATS, count * INSTANCE_FLOATS)
        entry.instanceCount += count
        instancedModels += count
        instancesThisBatch += count
        return true
    }

    private fun buildTranslucentOrder() {
        val n = translucentCount
        if (translucentOrder.size < n) {
            translucentOrder = IntArray(n * 2)
            translucentSortScratch = LongArray(n * 2)
        }
        if (!TRANS_SORT) {
            for (i in 0 until n) translucentOrder[i] = i
            return
        }
        val scratch = translucentSortScratch
        for (i in 0 until n) {
            val keyPart = (Int.MAX_VALUE.toLong() - translucentKey[i]) and 0xFFFFFFFFL
            val seqPart = (translucentSeq[i].toLong() and 0xFFFFFL)
            scratch[i] = (((keyPart shl 20) or seqPart) shl 12) or (i.toLong() and 0xFFFL) xor Long.MIN_VALUE
        }
        if (n <= 4096) {
            java.util.Arrays.sort(scratch, 0, n)
            for (i in 0 until n) translucentOrder[i] = (scratch[i] and 0xFFFL).toInt()
        } else {
            val idx = (0 until n).sortedWith(compareBy({ Int.MAX_VALUE.toLong() - translucentKey[it] }, { translucentSeq[it] }, { it }))
            for (i in 0 until n) translucentOrder[i] = idx[i]
        }
    }

    fun appendTranslucentEntry(entry: MeshEntry, modelMatrix: Matrix43, lightIdx: FloatArray?, depthWrite: Boolean, seq: Int, depthKey: Int): Boolean {
        if (!isFrameActive || !entry.resident) return false
        entry.lastUsedFrame = frameCounter
        appendTranslucent(entry, modelMatrix, lightIdx, depthWrite, seq, depthKey)
        instancedModels++
        instancesThisBatch++
        return true
    }

    private fun appendTranslucent(
        entry: MeshEntry,
        modelMatrix: Matrix43,
        lightIdx: FloatArray?,
        depthWrite: Boolean,
        seq: Int,
        depthKey: Int
    ) {
        ensureTranslucentCapacity()
        val i = translucentCount++
        translucentEntries[i] = entry
        translucentSrc[i] = null
        translucentTpl[i] = null
        translucentDepthWrite[i] = depthWrite
        translucentSeq[i] = seq
        translucentKey[i] = depthKey
        val data = translucentMatrices
        val base = i * INSTANCE_FLOATS
        data[base] = modelMatrix.xA
        data[base + 1] = modelMatrix.yA
        data[base + 2] = modelMatrix.zA
        data[base + 3] = 0f
        data[base + 4] = modelMatrix.xB
        data[base + 5] = modelMatrix.yB
        data[base + 6] = modelMatrix.zB
        data[base + 7] = 0f
        data[base + 8] = modelMatrix.xC
        data[base + 9] = modelMatrix.yC
        data[base + 10] = modelMatrix.zC
        data[base + 11] = 0f
        data[base + 12] = modelMatrix.xD
        data[base + 13] = modelMatrix.yD
        data[base + 14] = modelMatrix.zD
        data[base + 15] = 1f
        if (lightIdx != null) {
            data[base + 16] = lightIdx[0]
            data[base + 17] = lightIdx[1]
            data[base + 18] = lightIdx[2]
            data[base + 19] = lightIdx[3]
        } else {
            data[base + 16] = -1f
            data[base + 17] = -1f
            data[base + 18] = -1f
            data[base + 19] = -1f
        }
    }

    private fun evictAndCompact(neededVertices: Int, neededIndices: Int): Boolean {
        val candidates = ArrayList<MeshEntry>(meshEntries.size)
        for (entry in meshEntries.values) {
            if (entry.lastUsedFrame != frameCounter) candidates.add(entry)
        }
        candidates.sortBy { it.lastUsedFrame }

        val targetVertices = (MAX_TOTAL_VERTICES * EVICT_TARGET_RATIO).toInt() - neededVertices
        val targetIndices = (MAX_TOTAL_INDICES * EVICT_TARGET_RATIO).toInt() - neededIndices

        var freedVertices = 0
        var freedIndices = 0
        val evicted = HashSet<Long>()

        for (candidate in candidates) {
            if (totalVertexCount - freedVertices <= targetVertices &&
                totalIndexCount - freedIndices <= targetIndices
            ) break
            evicted.add(candidate.modelId)
            freedVertices += candidate.vertexCapacity
            freedIndices += candidate.indexCapacity
        }

        if (totalVertexCount - freedVertices + neededVertices > MAX_TOTAL_VERTICES ||
            totalIndexCount - freedIndices + neededIndices > MAX_TOTAL_INDICES
        ) {
            return false
        }

        if (evicted.isNotEmpty()) {
            for (candidate in candidates) {
                if (evicted.contains(candidate.modelId)) candidate.resident = false
            }
            meshEntries.keys.removeAll(evicted)
            meshesEvicted += evicted.size
            val iterator = drawsByMaterial.entries.iterator()
            while (iterator.hasNext()) {
                val bucket = iterator.next()
                bucket.value.removeAll { evicted.contains(it.mesh.modelId) }
                if (bucket.value.isEmpty()) iterator.remove()
            }
            materialOrderDirty = true
            activeEntries.removeAll { evicted.contains(it.modelId) }
            compactGeometryStore()
        }
        return true
    }

    private fun compactGeometryStore() {
        val vb = vertexBuffer!!
        val ib = indexBuffer!!
        val ordered = meshEntries.values.sortedBy { it.vertexOffset }

        var writeVertex = 0
        var writeIndex = 0
        for (entry in ordered) {
            val vertexDelta = writeVertex - entry.vertexOffset
            if (vertexDelta != 0) {
                moveVertexBytes(
                    vb,
                    entry.vertexOffset * VERTEX_STRIDE,
                    writeVertex * VERTEX_STRIDE,
                    entry.vertexCount * VERTEX_STRIDE
                )
            }
            entry.indexBase = writeIndex
            for (batch in entry.materialBatches) {
                val src = batch.indexOffset
                for (i in 0 until batch.indexCount) {
                    ib.put(writeIndex + i, ib.get(src + i) + vertexDelta)
                }
                batch.indexOffset = writeIndex
                writeIndex += batch.indexCount
            }
            entry.indexCapacity = writeIndex - entry.indexBase
            entry.vertexOffset = writeVertex
            entry.vertexCapacity = entry.vertexCount
            writeVertex += entry.vertexCount
        }

        totalVertexCount = writeVertex
        totalIndexCount = writeIndex
        uploadedVertexCount = 0
        uploadedIndexCount = 0
        dirtyRangeCount = 0
        dirtyIndexRangeCount = 0
        storeLayoutRevision++
    }

    private fun moveVertexBytes(vb: ByteBuffer, srcByte: Int, dstByte: Int, length: Int) {
        if (length == 0 || srcByte == dstByte) return
        val staging = compactStaging
        val src = vb.duplicate()
        val dst = vb.duplicate()
        var moved = 0
        while (moved < length) {
            val chunk = minOf(staging.size, length - moved)
            src.position(srcByte + moved)
            src.get(staging, 0, chunk)
            dst.position(dstByte + moved)
            dst.put(staging, 0, chunk)
            moved += chunk
        }
    }

    fun getInstancedEntries(): Collection<MeshEntry> {
        return meshEntries.values.filter { it.instanceCount >= INSTANCING_THRESHOLD }
    }

    fun getNonInstancedEntries(): Collection<MeshEntry> {
        return meshEntries.values.filter { it.instanceCount < INSTANCING_THRESHOLD }
    }

    fun endFrame(
        textureCache: (Int) -> Int,
        combineModeOf: (Int) -> Int = { 0 },
        needsBlendOf: (Int) -> Boolean = { false },
        alphaDiscard: Boolean = true
    ) {
        check(isFrameActive) { "No active frame" }

        val allEntries = activeEntries
        if ((allEntries.isEmpty() && translucentCount == 0) || vpMatrix == null) {
            isFrameActive = false
            return
        }

        val sh = shader ?: run {
            isFrameActive = false
            return
        }
        val vp = vpMatrix ?: run {
            isFrameActive = false
            return
        }

        if (totalVertexCount > uploadedVertexCount || totalIndexCount > uploadedIndexCount) {
            val t0 = System.nanoTime()
            if (totalVertexCount > uploadedVertexCount) {
                val vbView = vertexBuffer!!.duplicate()
                vbView.position(uploadedVertexCount * VERTEX_STRIDE)
                vbView.limit(totalVertexCount * VERTEX_STRIDE)
                vbo!!.uploadSubRange(uploadedVertexCount.toLong() * VERTEX_STRIDE, vbView)
                uploadedVertexCount = totalVertexCount
            }
            if (totalIndexCount > uploadedIndexCount) {
                val ibView = indexBuffer!!.duplicate()
                ibView.position(uploadedIndexCount)
                ibView.limit(totalIndexCount)
                ibo!!.uploadSubRange(uploadedIndexCount, ibView)
                uploadedIndexCount = totalIndexCount
            }
            timeGeomUpload += System.nanoTime() - t0
        }

        if (dirtyRangeCount > 0) {
            val t0 = System.nanoTime()
            val vbView = vertexBuffer!!.duplicate()
            if (MERGE_UPLOADS) {
                var lo = Int.MAX_VALUE; var hi = 0
                for (r in 0 until dirtyRangeCount) {
                    val first = dirtyRanges[r * 2]
                    val end = first + dirtyRanges[r * 2 + 1]
                    if (first >= uploadedVertexCount) continue
                    if (first < lo) lo = first
                    if (end > hi) hi = end
                }
                if (hi > lo) {
                    if (hi > uploadedVertexCount) hi = uploadedVertexCount
                    vbView.clear()
                    vbView.position(lo * VERTEX_STRIDE)
                    vbView.limit(hi * VERTEX_STRIDE)
                    vbo!!.uploadSubRange(lo.toLong() * VERTEX_STRIDE, vbView)
                    mergedUploadBytes += (hi - lo).toLong() * VERTEX_STRIDE
                }
            } else {
                for (r in 0 until dirtyRangeCount) {
                    val first = dirtyRanges[r * 2]
                    val count = dirtyRanges[r * 2 + 1]
                    if (first >= uploadedVertexCount) continue
                    vbView.clear()
                    vbView.position(first * VERTEX_STRIDE)
                    vbView.limit((first + count) * VERTEX_STRIDE)
                    vbo!!.uploadSubRange(first.toLong() * VERTEX_STRIDE, vbView)
                }
            }
            dirtyRangeCount = 0
            timeGeomUpload += System.nanoTime() - t0
        }
        if (dirtyIndexRangeCount > 0) {
            val t0 = System.nanoTime()
            val ibView = indexBuffer!!.duplicate()
            if (MERGE_UPLOADS) {
                var lo = Int.MAX_VALUE; var hi = 0
                for (r in 0 until dirtyIndexRangeCount) {
                    val first = dirtyIndexRanges[r * 2]
                    val end = first + dirtyIndexRanges[r * 2 + 1]
                    if (first >= uploadedIndexCount) continue
                    if (first < lo) lo = first
                    if (end > hi) hi = end
                }
                if (hi > lo) {
                    if (hi > uploadedIndexCount) hi = uploadedIndexCount
                    ibView.clear()
                    ibView.position(lo)
                    ibView.limit(hi)
                    ibo!!.uploadSubRange(lo, ibView)
                    mergedUploadBytes += (hi - lo).toLong() * 4
                }
            } else {
                for (r in 0 until dirtyIndexRangeCount) {
                    val first = dirtyIndexRanges[r * 2]
                    val count = dirtyIndexRanges[r * 2 + 1]
                    if (first >= uploadedIndexCount) continue
                    ibView.clear()
                    ibView.position(first)
                    ibView.limit(first + count)
                    ibo!!.uploadSubRange(first, ibView)
                }
            }
            dirtyIndexRangeCount = 0
            timeGeomUpload += System.nanoTime() - t0
        }

        sh.bind()
        GLWrapper.enable(GLWrapper.Constants.DEPTH_TEST)
        GLWrapper.depthFunc(GLWrapper.Constants.LEQUAL)
        if (CULL_FACE) {
            GLWrapper.enable(GLWrapper.Constants.CULL_FACE)
            GLWrapper.cullFace(CULL_MODE)
            GLWrapper.frontFace(CULL_WIND)
        } else {
            GLWrapper.disable(GLWrapper.Constants.CULL_FACE)
        }
        sh.setUniform(uTextureLocation, 0)
        sh.setUniform(uTextureTimeLocation, textureTime)
        sh.setUniform(
            uAlphaThresholdLocation,
            System.getProperty("render.alphaprobe")?.toFloatOrNull()
                ?: (if (alphaDiscard) ALPHA_CUTOFF else -1f)
        )

        glUniformMatrix4fv(uViewProjectionLocation, false, vp)

        sh.setUniform(uSunDirectionLocation, sunDirX, sunDirY, sunDirZ)
        if (uPointLightCountLocation >= 0) GLWrapper.uniform1i(uPointLightCountLocation, pointLightCount)
        if (pointLightCount > 0) {
            if (uPointLightPosLocation >= 0) GLWrapper.uniform4fv(uPointLightPosLocation, pointLightPos, pointLightCount)
            if (uPointLightColorLocation >= 0) GLWrapper.uniform3fv(uPointLightColorLocation, pointLightColor, pointLightCount)
        }
        sh.setUniform(uSunBrightnessLocation, sunBrightness)
        sh.setUniform(uSunColorLocation, sunColorR, sunColorG, sunColorB)
        sh.setUniform(uContrastPosLocation, contrastPos)
        sh.setUniform(uContrastNegLocation, contrastNeg)
        viewMatrix?.let { glUniformMatrix4fv(uViewMatrixLocation, false, it) }
        sh.setUniform(uFogEnabledLocation, if (fogOn) 1 else 0)
        sh.setUniform(uFogStartLocation, fogStart)
        sh.setUniform(uFogEndLocation, fogEnd)
        sh.setUniform(uFogColorLocation, fogR, fogG, fogB)
        sh.setUniform(uFogColor2Location, fogR, fogG, fogB)

        vao!!.bind()
        ibo!!.bind()

        val t1 = System.nanoTime()
        var scratch = instancePackScratch
        var packedInstances = 0
        for (entry in allEntries) {
            val count = entry.instanceCount
            if (count == 0) continue
            val floats = count * INSTANCE_FLOATS
            val at = packedInstances * INSTANCE_FLOATS
            if (at + floats > scratch.size) {
                scratch = scratch.copyOf(maxOf(scratch.size * 2, at + floats))
                instancePackScratch = scratch
            }
            entry.instanceBase = packedInstances
            System.arraycopy(entry.instanceData, 0, scratch, at, floats)
            packedInstances += count
        }
        for (bi in blockDraws.indices) {
            val b = blockDraws[bi]
            val floats = b.count * INSTANCE_FLOATS
            val at = packedInstances * INSTANCE_FLOATS
            if (at + floats > scratch.size) {
                scratch = scratch.copyOf(maxOf(scratch.size * 2, at + floats))
                instancePackScratch = scratch
            }
            System.arraycopy(b.floats, 0, scratch, at, floats)
            b.base = packedInstances
            packedInstances += b.count
        }
        translucentBase = packedInstances
        translucentPacked = translucentCount
        translucentCursor = 0
        if (translucentCount > 0) buildTranslucentOrder()
        if (translucentCount > 0) {
            val floats = translucentCount * INSTANCE_FLOATS
            val at = packedInstances * INSTANCE_FLOATS
            if (at + floats > scratch.size) {
                scratch = scratch.copyOf(maxOf(scratch.size * 2, at + floats))
                instancePackScratch = scratch
            }
            val order = translucentOrder
            val src = translucentMatrices
            for (oi in 0 until translucentCount) {
                val i = order[oi]
                val ext = translucentSrc[i]
                if (ext != null) System.arraycopy(ext, translucentSrcOff[i], scratch, at + oi * INSTANCE_FLOATS, INSTANCE_FLOATS)
                else System.arraycopy(src, i * INSTANCE_FLOATS, scratch, at + oi * INSTANCE_FLOATS, INSTANCE_FLOATS)
            }
            packedInstances += translucentCount
        }
        if (packedInstances > 0) {
            if (packedInstances > instanceCapacity) {
                var grown = instanceCapacity
                while (grown < packedInstances) grown *= 2
                instanceBuffer = MemoryUtil.memRealloc(instanceBuffer!!, grown * INSTANCE_FLOATS)
                instanceCapacity = grown
            }
            val instBuf = instanceBuffer!!
            instBuf.clear()
            instBuf.put(scratch, 0, packedInstances * INSTANCE_FLOATS)
            instBuf.flip()
            instanceVbo!!.upload(instBuf)
        }
        timeInstanceUpload += System.nanoTime() - t1

        if (packedInstances == 0) {
            vao!!.unbind()
            sh.unbind()
            isFrameActive = false
            return
        }

        if (materialOrderDirty) {
            val ids = drawsByMaterial.keys.toIntArray()
            java.util.Arrays.sort(ids)
            materialOrder = ids
            materialOrderDirty = false
        }

        val t2 = System.nanoTime()

        var indirectCommandCount = 0
        if (bindless) {
            drawMatCursor = 0
            beginMaterialPass()
        }
        if (indirectSupported && bindless) {
            var buf = indirectScratch
            var cursor = 0
            val nCmd = sortActiveCommands()
            for (i in 0 until nCmd) {
                val seq = (cmdKeys[i] and 0xffffffffL).toInt()
                val mesh = cmdEntry[seq]!!
                val matBatch = cmdBatch[seq]!!
                val count = mesh.instanceCount
                appendDrawMaterial(matBatch.materialId, textureCache, combineModeOf)
                if (cursor + INDIRECT_INTS > buf.size) {
                    buf = buf.copyOf(buf.size * 2)
                    indirectScratch = buf
                }
                buf[cursor] = matBatch.indexCount
                buf[cursor + 1] = count
                buf[cursor + 2] = matBatch.indexOffset
                buf[cursor + 3] = 0
                buf[cursor + 4] = mesh.instanceBase
                cursor += INDIRECT_INTS
                indirectCommandCount++
                instancedTriangles += (matBatch.indexCount / 3) * count
            }
            for (bi in blockDraws.indices) {
                val b = blockDraws[bi]
                val n = b.cmdCount * INDIRECT_INTS
                if (cursor + n > buf.size) {
                    buf = buf.copyOf(maxOf(buf.size * 2, cursor + n))
                    indirectScratch = buf
                }
                System.arraycopy(b.cmds, 0, buf, cursor, n)
                val base = b.base
                val mats = b.mats
                val words = b.cmdCount * 4
                if (b.matWordsRevision != materialWordsRevision || b.matWords.size < words) {
                    if (b.matWords.size < words) b.matWords = IntArray(words)
                    val w = b.matWords
                    var complete = true
                    for (k in 0 until b.cmdCount) {
                        val slot = resolveMaterial(mats[k], textureCache, combineModeOf)
                        w[k * 4] = matEntryLo[slot]; w[k * 4 + 1] = matEntryHi[slot]; w[k * 4 + 2] = matEntryFlags[slot]; w[k * 4 + 3] = matEntryScroll[slot]
                        if (mats[k] >= 0 && matEntryFlags[slot] == 0) complete = false
                    }
                    b.matWordsRevision = if (complete) materialWordsRevision else -1
                }
                if (drawMatCursor + words > drawMatScratch.size) drawMatScratch = drawMatScratch.copyOf(maxOf(drawMatScratch.size * 2, drawMatCursor + words))
                System.arraycopy(b.matWords, 0, drawMatScratch, drawMatCursor, words)
                drawMatCursor += words
                var c = cursor
                for (k in 0 until b.cmdCount) {
                    buf[c + 4] += base
                    instancedTriangles += (buf[c] / 3) * buf[c + 1]
                    c += INDIRECT_INTS
                }
                cursor += n
                indirectCommandCount += b.cmdCount
                blockCommandsDrawn += b.cmdCount
            }
            blockDraws.clear()
            java.util.Arrays.fill(cmdEntry, 0, nCmd, null)
            java.util.Arrays.fill(cmdBatch, 0, nCmd, null)
            if (indirectCommandCount > 0) {
                if (indirectBuffer!!.capacity() < cursor) {
                    indirectBuffer = MemoryUtil.memRealloc(indirectBuffer!!, buf.size)
                }
                val ib = indirectBuffer!!
                ib.clear()
                ib.put(buf, 0, cursor)
                ib.flip()
                glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectVbo)
                glBufferData(GL_DRAW_INDIRECT_BUFFER, ib, GL_STREAM_DRAW)
            }
        } else if (indirectSupported) {
            var buf = indirectScratch
            var cursor = 0
            val nCmd = sortActiveCommands()
            buildMaterialRuns(nCmd)
            for (i in 0 until nCmd) {
                val seq = (cmdKeys[i] and 0xffffffffL).toInt()
                val mesh = cmdEntry[seq]!!
                val matBatch = cmdBatch[seq]!!
                val count = mesh.instanceCount
                if (cursor + INDIRECT_INTS > buf.size) {
                    buf = buf.copyOf(buf.size * 2)
                    indirectScratch = buf
                }
                buf[cursor] = matBatch.indexCount
                buf[cursor + 1] = count
                buf[cursor + 2] = matBatch.indexOffset
                buf[cursor + 3] = 0
                buf[cursor + 4] = mesh.instanceBase
                cursor += INDIRECT_INTS
                indirectCommandCount++
                instancedTriangles += (matBatch.indexCount / 3) * count
            }
            if (indirectCommandCount > 0) {
                if (indirectBuffer!!.capacity() < cursor) {
                    indirectBuffer = MemoryUtil.memRealloc(indirectBuffer!!, buf.size)
                }
                val ib = indirectBuffer!!
                ib.clear()
                ib.put(buf, 0, cursor)
                ib.flip()
                glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectVbo)
                glBufferData(GL_DRAW_INDIRECT_BUFFER, ib, GL_STREAM_DRAW)
            }
        }

        if (bindless) {
            if (indirectCommandCount > 0) {
                uploadDrawMaterials()
                sh.setUniform(uDrawBaseLocation, 0)
                FrameDump.multiIndirect(TRIANGLES, UNSIGNED_INT, 0L, indirectCommandCount, 0)
                instancedDrawCalls++
            }
            reflectionCommands = indirectCommandCount
            timeDraw += System.nanoTime() - t2
            vao!!.unbind()
            sh.unbind()
            isFrameActive = false
            return
        }

        if (!indirectSupported) buildMaterialRuns(sortActiveCommands())
        for (run in 0 until materialRunCount) {
            val texId = runMaterial[run]
            val textureHandle = if (texId >= 0) textureCache(texId) else 0
            if (textureHandle > 0) {
                GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                GLWrapper.bindTexture(TEXTURE_2D, textureHandle)
                sh.setUniform(uTextureEnabledLocation, 1)
                sh.setUniform(uCombineModeLocation, combineModeOf(texId))
                sh.setUniform(uTexAlphaLocation, if (TEX_ALPHA && texHasAlpha(texId)) 1 else 0)
                setTextureSpeedUniform(sh, texId)
            } else {
                sh.setUniform(uTextureEnabledLocation, 0)
            }

            val first = runFirst[run]
            val commands = runCount[run]
            if (indirectSupported) {
                FrameDump.multiIndirect(TRIANGLES, UNSIGNED_INT, first.toLong() * INDIRECT_INTS * 4, commands, 0)
                instancedDrawCalls++
            } else {
                for (i in first until first + commands) {
                    val seq = (cmdKeys[i] and 0xffffffffL).toInt()
                    val mesh = cmdEntry[seq]!!
                    val matBatch = cmdBatch[seq]!!
                    val count = mesh.instanceCount
                    if (baseInstanceSupported) {
                        FrameDump.instancedBase(
                            TRIANGLES,
                            matBatch.indexCount,
                            UNSIGNED_INT,
                            matBatch.indexOffset.toLong() * 4,
                            count,
                            mesh.instanceBase
                        )
                    } else {
                        repointInstanceAttributes(mesh.instanceBase)
                        GLWrapper.drawElementsInstanced(
                            TRIANGLES,
                            matBatch.indexCount,
                            UNSIGNED_INT,
                            matBatch.indexOffset.toLong() * 4,
                            count
                        )
                    }
                    instancedDrawCalls++
                    instancedTriangles += (matBatch.indexCount / 3) * count
                }
            }
        }
        java.util.Arrays.fill(cmdEntry, 0, activeCommandCount, null)
        java.util.Arrays.fill(cmdBatch, 0, activeCommandCount, null)
        materialRunCount = 0
        timeDraw += System.nanoTime() - t2

        vao!!.unbind()
        sh.unbind()

        isFrameActive = false
    }

    private var activeCommandCount = 0
    private var materialRunCount = 0
    private var runMaterial = IntArray(256)
    private var runFirst = IntArray(256)
    private var runCount = IntArray(256)

    private fun sortActiveCommands(): Int {
        var nCmd = 0
        for (ei in activeEntries.indices) {
            val e = activeEntries[ei]
            if (e.instanceCount == 0) continue
            val batches = e.materialBatches
            for (bi in batches.indices) {
                if (nCmd >= cmdKeys.size) {
                    cmdKeys = cmdKeys.copyOf(cmdKeys.size * 2)
                    cmdEntry = cmdEntry.copyOf(cmdEntry.size * 2)
                    cmdBatch = cmdBatch.copyOf(cmdBatch.size * 2)
                }
                val b = batches[bi]
                cmdEntry[nCmd] = e
                cmdBatch[nCmd] = b
                cmdKeys[nCmd] = ((b.materialId + 1).toLong() shl 32) or nCmd.toLong()
                nCmd++
            }
        }
        java.util.Arrays.sort(cmdKeys, 0, nCmd)
        activeCommandCount = nCmd
        return nCmd
    }

    private fun buildMaterialRuns(nCmd: Int) {
        materialRunCount = 0
        var i = 0
        while (i < nCmd) {
            val material = (cmdKeys[i] ushr 32).toInt() - 1
            var j = i + 1
            while (j < nCmd && (cmdKeys[j] ushr 32).toInt() - 1 == material) j++
            if (materialRunCount == runMaterial.size) {
                runMaterial = runMaterial.copyOf(materialRunCount * 2)
                runFirst = runFirst.copyOf(materialRunCount * 2)
                runCount = runCount.copyOf(materialRunCount * 2)
            }
            runMaterial[materialRunCount] = material
            runFirst[materialRunCount] = i
            runCount[materialRunCount] = j - i
            materialRunCount++
            i = j
        }
    }

    fun pendingTranslucent(): Int = translucentPacked - translucentCursor

    @JvmField var translucentGroupsDrawn = 0L
    @JvmField var translucentCommandsDrawn = 0L

    fun drawTranslucent(
        textureCache: (Int) -> Int,
        combineModeOf: (Int) -> Int = { 0 },
        upToKey: Int = Int.MIN_VALUE,
        upToSeq: Int = Int.MAX_VALUE
    ) {
        val start = translucentCursor
        var end = start
        while (end < translucentPacked) {
            val i = translucentOrder[end]
            val k = translucentKey[i]
            if (k > upToKey || (k == upToKey && translucentSeq[i] < upToSeq)) end++ else break
        }
        if (end == start) return
        translucentCursor = end
        val sh = shader ?: return
        val t0 = System.nanoTime()
        sh.bind()
        GLWrapper.enable(GLWrapper.Constants.BLEND)
        GLWrapper.blendFunc(GLWrapper.Constants.SRC_ALPHA, GLWrapper.Constants.ONE_MINUS_SRC_ALPHA)
        GLWrapper.enable(GLWrapper.Constants.DEPTH_TEST)
        GLWrapper.depthMask(true)
        if (CULL_FACE) {
            GLWrapper.enable(GLWrapper.Constants.CULL_FACE)
            GLWrapper.cullFace(CULL_MODE)
            GLWrapper.frontFace(CULL_WIND)
        } else {
            GLWrapper.disable(GLWrapper.Constants.CULL_FACE)
        }
        vao!!.bind()
        ibo!!.bind()
        var lastTex = Int.MIN_VALUE
        var depthWriteOn = true

        if (indirectSupported) {
            var buf = indirectScratch
            var cursor = 0
            var groupCount = 0
            var gTex = translucentGroupTex
            var gDepth = translucentGroupDepth
            var gFirst = translucentGroupFirst
            var gCount = translucentGroupCount
            var commands = 0
            var curTex = Int.MIN_VALUE
            var curDepth = true
            var curFirst = 0
            if (bindless) {
                drawMatCursor = 0
                beginMaterialPass()
            }
            var oi = start
            while (oi < end) {
                val i = translucentOrder[oi]
                val entry = translucentEntries[i]
                if (entry == null || !entry.resident) { oi++; continue }
                val depthWrite = translucentDepthWrite[i]
                val slot = translucentBase + oi
                val tpl = translucentTpl[i]
                if (tpl != null && bindless) {
                    if (depthWrite != curDepth || commands == 0) {
                        if (commands > curFirst) {
                            if (groupCount == gTex.size) {
                                gTex = gTex.copyOf(groupCount * 2); gDepth = gDepth.copyOf(groupCount * 2)
                                gFirst = gFirst.copyOf(groupCount * 2); gCount = gCount.copyOf(groupCount * 2)
                            }
                            gTex[groupCount] = curTex; gDepth[groupCount] = curDepth
                            gFirst[groupCount] = curFirst; gCount[groupCount] = commands - curFirst
                            groupCount++
                        }
                        curDepth = depthWrite; curFirst = commands
                    }
                    val n = translucentTplCount[i]
                    val ints = n * INDIRECT_INTS
                    if (cursor + ints > buf.size) {
                        buf = buf.copyOf(maxOf(buf.size * 2, cursor + ints))
                        indirectScratch = buf
                    }
                    System.arraycopy(tpl, translucentTplOff[i], buf, cursor, ints)
                    var c = cursor
                    for (k in 0 until n) { buf[c + 1] = 1; buf[c + 4] = slot; instancedTriangles += buf[c] / 3; c += INDIRECT_INTS }
                    cursor += ints
                    val words = n * 4
                    if (drawMatCursor + words > drawMatScratch.size) drawMatScratch = drawMatScratch.copyOf(maxOf(drawMatScratch.size * 2, drawMatCursor + words))
                    System.arraycopy(translucentMatW[i]!!, translucentMatOff[i], drawMatScratch, drawMatCursor, words)
                    drawMatCursor += words
                    commands += n
                    translucentTemplateItems++
                    oi++
                    continue
                }
                var run = 1
                while (oi + run < end) {
                    val j = translucentOrder[oi + run]
                    if (translucentEntries[j] !== entry || translucentDepthWrite[j] != depthWrite) break
                    run++
                }
                oi += run
                translucentRunsMerged += run - 1
                val batches = entry.materialBatches
                for (bi in batches.indices) {
                    val batch = batches[bi]
                    val texId = batch.materialId
                    if (bindless) appendDrawMaterial(texId, textureCache, combineModeOf)
                    if ((!bindless && texId != curTex) || depthWrite != curDepth || commands == 0) {
                        if (commands > curFirst) {
                            if (groupCount == gTex.size) {
                                gTex = gTex.copyOf(groupCount * 2); gDepth = gDepth.copyOf(groupCount * 2)
                                gFirst = gFirst.copyOf(groupCount * 2); gCount = gCount.copyOf(groupCount * 2)
                            }
                            gTex[groupCount] = curTex; gDepth[groupCount] = curDepth
                            gFirst[groupCount] = curFirst; gCount[groupCount] = commands - curFirst
                            groupCount++
                        }
                        curTex = texId; curDepth = depthWrite; curFirst = commands
                    }
                    if (cursor + INDIRECT_INTS > buf.size) {
                        buf = buf.copyOf(buf.size * 2)
                        indirectScratch = buf
                    }
                    buf[cursor] = batch.indexCount
                    buf[cursor + 1] = run
                    buf[cursor + 2] = batch.indexOffset
                    buf[cursor + 3] = 0
                    buf[cursor + 4] = slot
                    cursor += INDIRECT_INTS
                    commands++
                    instancedTriangles += (batch.indexCount / 3) * run
                }
            }
            if (commands > curFirst) {
                if (groupCount == gTex.size) {
                    gTex = gTex.copyOf(groupCount * 2); gDepth = gDepth.copyOf(groupCount * 2)
                    gFirst = gFirst.copyOf(groupCount * 2); gCount = gCount.copyOf(groupCount * 2)
                }
                gTex[groupCount] = curTex; gDepth[groupCount] = curDepth
                gFirst[groupCount] = curFirst; gCount[groupCount] = commands - curFirst
                groupCount++
            }
            translucentGroupTex = gTex; translucentGroupDepth = gDepth
            translucentGroupFirst = gFirst; translucentGroupCount = gCount
            translucentGroupsDrawn += groupCount
            translucentCommandsDrawn += commands
            if (commands > 0) {
                if (indirectBuffer!!.capacity() < cursor) {
                    indirectBuffer = MemoryUtil.memRealloc(indirectBuffer!!, buf.size)
                }
                val ib = indirectBuffer!!
                ib.clear()
                ib.put(buf, 0, cursor)
                ib.flip()
                glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectVbo)
                glBufferData(GL_DRAW_INDIRECT_BUFFER, ib, GL_STREAM_DRAW)
                if (bindless) uploadDrawMaterials()
                for (g in 0 until groupCount) {
                    val texId = gTex[g]
                    if (bindless) {
                        sh.setUniform(uDrawBaseLocation, gFirst[g])
                    } else if (texId != lastTex) {
                        val textureHandle = if (texId >= 0) textureCache(texId) else 0
                        if (textureHandle > 0) {
                            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                            GLWrapper.bindTexture(TEXTURE_2D, textureHandle)
                            sh.setUniform(uTextureEnabledLocation, 1)
                            sh.setUniform(uCombineModeLocation, combineModeOf(texId))
                            sh.setUniform(uTexAlphaLocation, if (TEX_ALPHA && texHasAlpha(texId)) 1 else 0)
                            setTextureSpeedUniform(sh, texId)
                        } else {
                            sh.setUniform(uTextureEnabledLocation, 0)
                        }
                        lastTex = texId
                    }
                    if (gDepth[g] != depthWriteOn) {
                        GLWrapper.depthMask(gDepth[g])
                        depthWriteOn = gDepth[g]
                    }
                    if (gCount[g] == 1) {
                        val c = gFirst[g] * INDIRECT_INTS
                        FrameDump.instancedBase(
                            TRIANGLES, buf[c], UNSIGNED_INT, buf[c + 2].toLong() * 4, buf[c + 1], buf[c + 4]
                        )
                    } else {
                        FrameDump.multiIndirect(
                            TRIANGLES, UNSIGNED_INT,
                            gFirst[g].toLong() * INDIRECT_INTS * 4, gCount[g], 0
                        )
                    }
                    instancedDrawCalls++
                }
            }
        } else {
            for (oi in start until end) {
                val i = translucentOrder[oi]
                val entry = translucentEntries[i] ?: continue
                if (!entry.resident) continue
                val depthWrite = translucentDepthWrite[i]
                if (depthWrite != depthWriteOn) {
                    GLWrapper.depthMask(depthWrite)
                    depthWriteOn = depthWrite
                }
                val slot = translucentBase + oi
                val batches = entry.materialBatches
                for (bi in batches.indices) {
                    val batch = batches[bi]
                    val texId = batch.materialId
                    if (texId != lastTex) {
                        val textureHandle = if (texId >= 0) textureCache(texId) else 0
                        if (textureHandle > 0) {
                            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                            GLWrapper.bindTexture(TEXTURE_2D, textureHandle)
                            sh.setUniform(uTextureEnabledLocation, 1)
                            sh.setUniform(uCombineModeLocation, combineModeOf(texId))
                            sh.setUniform(uTexAlphaLocation, if (TEX_ALPHA && texHasAlpha(texId)) 1 else 0)
                            setTextureSpeedUniform(sh, texId)
                        } else {
                            sh.setUniform(uTextureEnabledLocation, 0)
                        }
                        lastTex = texId
                    }
                    if (baseInstanceSupported) {
                        FrameDump.instancedBase(
                            TRIANGLES, batch.indexCount, UNSIGNED_INT,
                            batch.indexOffset.toLong() * 4, 1, slot
                        )
                    } else {
                        repointInstanceAttributes(slot)
                        GLWrapper.drawElementsInstanced(
                            TRIANGLES, batch.indexCount, UNSIGNED_INT,
                            batch.indexOffset.toLong() * 4, 1
                        )
                    }
                    instancedDrawCalls++
                    instancedTriangles += batch.indexCount / 3
                }
            }
        }
        if (!depthWriteOn) GLWrapper.depthMask(true)
        vao!!.unbind()
        sh.unbind()
        timeDraw += System.nanoTime() - t0
    }

    fun isActive(): Boolean = isFrameActive

    fun hasPendingGeometry(): Boolean = isFrameActive && instancesThisBatch > 0

    fun getStats(): InstanceStats = InstanceStats(
        drawCalls = instancedDrawCalls,
        triangles = instancedTriangles,
        totalInstances = instancedModels,
        uniqueMeshes = meshEntries.size,
        instancedMeshes = meshEntries.values.count { it.instanceCount >= INSTANCING_THRESHOLD },
        meshesReused = instancedMeshesReused,
        meshesEvicted = meshesEvicted,
        capacityFallbacks = capacityFallbacks,
        storeVertices = totalVertexCount,
        refits = refits,
        geomUploadMs = timeGeomUpload / 1_000_000f,
        instanceUploadMs = timeInstanceUpload / 1_000_000f,
        drawMs = timeDraw / 1_000_000f
    )

    fun destroy() {
        for (entry in meshEntries.values) entry.resident = false
        for (entry in activeEntries) entry.resident = false
        meshEntries.clear()
        activeEntries.clear()
        drawsByMaterial.clear()
        blockDraws.clear()
        shader?.destroy()
        vao?.destroy()
        vbo?.destroy()
        ibo?.destroy()
        instanceVbo?.destroy()
        if (indirectVbo != 0) {
            glDeleteBuffers(indirectVbo)
            indirectVbo = 0
        }
        indirectBuffer?.let { MemoryUtil.memFree(it) }
        indirectBuffer = null
        if (drawMatVbo != 0) {
            glDeleteBuffers(drawMatVbo)
            drawMatVbo = 0
        }
        drawMatBuffer?.let { MemoryUtil.memFree(it) }
        drawMatBuffer = null
        for (handle in bindlessHandles.values) glMakeTextureHandleNonResidentARB(handle)
        bindlessHandles.clear()

        vertexBuffer?.let { MemoryUtil.memFree(it) }
        indexBuffer?.let { MemoryUtil.memFree(it) }
        instanceBuffer?.let { MemoryUtil.memFree(it) }

        shader = null
        vao = null
        vbo = null
        ibo = null
        instanceVbo = null
        vertexBuffer = null
        indexBuffer = null
        instanceBuffer = null

        isInitialized = false
    }

    data class InstanceStats(
        val drawCalls: Int,
        val triangles: Int,
        val totalInstances: Int,
        val uniqueMeshes: Int,
        val instancedMeshes: Int,
        val meshesReused: Int,
        val meshesEvicted: Int = 0,
        val capacityFallbacks: Int = 0,
        val storeVertices: Int = 0,
        val refits: Int = 0,
        val geomUploadMs: Float = 0f,
        val instanceUploadMs: Float = 0f,
        val drawMs: Float = 0f
    )
}
