package com.jagex.game.runetek5.graphics.modern.batch

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.FLOAT
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_2D
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TRIANGLES
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_BYTE
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_INT
import com.jagex.game.runetek5.math.Matrix43
import org.lwjgl.opengl.GL33.*
import org.lwjgl.opengl.GL42.glDrawElementsInstancedBaseInstance
import org.lwjgl.opengl.GL43.GL_DRAW_INDIRECT_BUFFER
import org.lwjgl.opengl.GL43.glMultiDrawElementsIndirect
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * Instanced model batcher that uses glDrawElementsInstanced for repeated models.
 *
 * When multiple instances of the same mesh (identified by content hash from ModelCache)
 * appear in a frame, this batcher uploads the mesh geometry ONCE and draws all instances
 * with a single instanced draw call per material batch.
 *
 * Architecture:
 * - Mesh geometry is uploaded once per unique mesh (vertices + indices)
 * - Per-instance data (model matrix as mat4) is stored in a separate instance buffer
 * - The vertex shader reads the instance matrix via per-instance attributes (divisor = 1)
 * - Each material batch within a mesh gets one glDrawElementsInstanced call
 *
 * Per-instance data layout (64 bytes per instance):
 * - mat4 model matrix: 4 x vec4 (64 bytes) at locations 5, 6, 7, 8
 *
 * Vertex format (36 bytes per vertex, no modelIndex needed):
 * - position: vec3 float (12 bytes) at location 0
 * - normal: vec3 float (12 bytes) at location 1
 * - texcoord: vec2 float (8 bytes) at location 2
 * - color: vec4 ubyte (4 bytes) at location 3
 */
class InstancedModelBatcher {

    companion object {
        /** Fold the texture alpha channel into model fragment alpha.
         *  Legacy model faces take alpha from the face colour, so this is OFF. */
        /** Must match MAX_POINT_LIGHTS in the vertex shader. */
        /** Raise with -Drender.maxlights. */
        @JvmStatic val MAX_POINT_LIGHTS = System.getProperty("render.maxlights")?.toIntOrNull()?.coerceIn(1, 256) ?: 128

        @JvmStatic val CULL_FACE = System.getProperty("render.cullface")?.equals("false", true) != true
        /** GL_BACK. */
        @JvmStatic val CULL_MODE = 0x0405
        /** GL_CCW. Front faces come out counter-clockwise for this batcher's
         *  geometry; verified against legacy (GL_CW culled the wrong side and left
         *  the signpost's underside quads visible). -Drender.cullwind=cw to flip. */
        @JvmStatic val CULL_WIND = if (System.getProperty("render.cullwind") == "cw") 0x0900 else 0x0901
        /** Fold the texture alpha channel into model fragment alpha.
         *
         *  ON, because that is what legacy does: loadTextureCombineMode maps every
         *  material mode onto an alpha combiner that reads the texture (MODULATE ->
         *  texA*vertA, REPLACE -> texA), and glAlphaFunc(GL_GREATER, 0) discards only
         *  fully transparent fragments -- the alpha combiner source is never
         *  redirected away from the texture. Forcing alpha to come from the vertex
         *  colour instead made the Canifis lampposts solid, but it also made every
         *  texture that legitimately uses its alpha opaque (cape icon backgrounds,
         *  roof tiles). -Drender.texalpha=false restores that behaviour. */
        @JvmStatic val TEX_ALPHA = System.getProperty("render.texalpha")?.equals("false", true) != true
        /**
         * Alpha cutoff for the model pass.
         *
         * Legacy uses glAlphaFunc(GL_GREATER, 0) — it only discards fully
         * transparent texels — and gets away with it because Scene draws its
         * layered/alpha bucket LAST and sorted (Scene.drawTiles). This batcher
         * sorts by MATERIAL instead, so a low-alpha texel drawn early both blends
         * AND writes depth, occluding whatever is behind it: grass fringes were
         * punching holes through lamp posts and signposts, which reads as the model
         * being transparent.
         *
         * Discarding the fringe restores solid geometry. It deviates from legacy's
         * exact threshold, deliberately: reproducing GL_GREATER(0) faithfully would
         * require reproducing legacy's per-entity draw order too, which the
         * material-batched design cannot do.
         */
        const val ALPHA_CUTOFF = 0f

        /**
         * Vertex stride: position(12) + normal(12) + texcoord(8) + color(4) + 4 bytes
         * padding (the ModelCache 40-byte format's modelIndex slot, ignored by the
         * instanced VAO). Keeping the cache's native stride lets addInstance bulk-copy
         * cached vertex data with a single ByteBuffer.put instead of a per-byte
         * strip-copy loop.
         */
        const val VERTEX_STRIDE = 40

        /** Per-instance data: mat4 = 16 floats = 64 bytes */
        /** mat4 (16 floats) + light indices (4 floats). */
        const val INSTANCE_FLOATS = 20
        const val INSTANCE_STRIDE = INSTANCE_FLOATS * 4

        /** Maximum instances per instanced draw call */
        private const val MAX_INSTANCES_PER_MESH = 1024

        /**
         * Capacity of the frame-wide instance buffer.
         *
         * Every instance in the frame is packed into ONE buffer and uploaded ONCE.
         * The previous design uploaded each mesh's matrices separately, immediately
         * before that mesh's draws — ~800 glBufferSubData calls per frame into a
         * buffer the preceding draws were still reading. That is a write-after-read
         * hazard on every one of them, and the driver resolves it by serialising:
         * the instanced pass measured 6.9ms of GPU time for a scene that shades in a
         * small fraction of that. One upload plus base-instance draws removes the
         * hazard entirely.
         */
        private const val MAX_INSTANCES_PER_FRAME = 65536

        /** Maximum unique meshes tracked per frame */
        private const val MAX_UNIQUE_MESHES = 256

        /**
         * Maximum total vertices across all unique meshes.
         *
         * Sized against the measured WORKING SET, not a guess: a Canifis-density
         * frame submits ~1600 meshes averaging ~650 vertices, i.e. ~1M vertices
         * that are ALL live in the same frame. At the old 500k the working set was
         * twice capacity, which is textbook LRU thrashing — only previous-frame
         * entries are evictable, so each frame evicted meshes it then immediately
         * needed again (531 evictions and 1171 overflows in a single frame), paid a
         * full arena compaction each time, and re-uploaded the whole store after
         * every one. Overflowing meshes fell through to the streaming batcher and
         * were silently dropped there, which is what deleted walls and punched
         * holes in lamp posts and the signpost.
         *
         * 2M gives ~2x headroom over the observed working set. Cost is 80MB of
         * staging plus 80MB on the GPU.
         */
        private const val MAX_TOTAL_VERTICES = 2000000

        /**
         * Maximum total indices. Both cache paths emit 3 unshared vertices per face
         * with sequential indices, so index count tracks vertex count exactly —
         * this is deliberately equal to [MAX_TOTAL_VERTICES] rather than a
         * multiple, and was never the binding constraint.
         */
        private const val MAX_TOTAL_INDICES = 2000000

        /**
         * On overflow, evict down to this fraction of capacity rather than just
         * far enough to fit the incoming mesh.
         *
         * Compaction costs a full pass over the resident arena, so evicting the
         * bare minimum makes it run on EVERY subsequent insert — the store pins
         * itself at 100% and every new mesh pays a whole-arena copy plus a full
         * VBO re-upload. Freeing a large block at once amortises that across the
         * many inserts that follow.
         */
        private const val EVICT_TARGET_RATIO = 0.65f

        /** Minimum instance count to use instancing (below this, falls through to regular batcher) */
        const val INSTANCING_THRESHOLD = 2

        /** Ints per glMultiDrawElementsIndirect command: count, instanceCount, firstIndex, baseVertex, baseInstance. */
        const val INDIRECT_INTS = 5
    }

    // GPU resources
    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var instanceVbo: VertexBuffer? = null
    private var shader: ShaderProgram? = null

    // Uniform locations
    private var uTextureLocation = -1
    private var uTextureEnabledLocation = -1
    private var uCombineModeLocation = -1
    private var uTexAlphaLocation = -1
    /** Per-material: does this texture actually carry an alpha channel? */
    private var texHasAlpha: (Int) -> Boolean = { true }
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

    // Fog state, mirrored from the toolkit each frame (legacy GL_LINEAR fog).
    private var viewMatrix: FloatArray? = null
    private var fogR = 0f; private var fogG = 0f; private var fogB = 0f
    private var fogStart = 0f; private var fogEnd = 0f; private var fogOn = false

    // Per-frame lighting parameters (set once per frame)
    private var sunDirX = 0f
    private var sunDirY = 1f
    private var sunDirZ = 0f
    private var sunBrightness = 0.5f
    private var sunColorR = 1f
    private var sunColorG = 1f
    private var sunColorB = 1f
    private var contrastPos = 1f
    private var contrastNeg = 1f

    // View-projection matrix from legacy system
    private var vpMatrix: FloatArray? = null

    // CPU staging buffers
    private var vertexBuffer: ByteBuffer? = null
    private var indexBuffer: IntBuffer? = null
    private var instanceBuffer: FloatBuffer? = null

    /**
     * Tracks a unique mesh (persistent across frames) and its instances (per frame).
     */
    class MeshEntry(
        /** Content hash identifying this unique mesh */
        val modelId: Long,
        /** Offset into the shared vertex buffer (in vertices) */
        var vertexOffset: Int = 0,
        /** Number of vertices for this mesh */
        var vertexCount: Int = 0,
        /** Material batches with index offsets into the shared index buffer */
        val materialBatches: MutableList<MaterialBatchEntry> = mutableListOf()
    ) {
        /** Per-instance model matrices for the CURRENT frame (16 floats each, in place — no per-instance allocation) */
        var instanceData: FloatArray = FloatArray(INSTANCE_FLOATS * 16)
        var instanceCount: Int = 0

        /**
         * Presented-frame counter at the last [addInstance] touch. Drives LRU
         * eviction; entries carrying the CURRENT frame are never evicted, so
         * compaction can never pull geometry out from under this frame's draws.
         */
        var lastUsedFrame: Long = 0

        /**
         * Index of this mesh's first instance in the frame-wide instance buffer,
         * assigned during [endFrame]'s packing pass and passed to each draw as its
         * base instance.
         */
        var instanceBase: Int = 0

        /**
         * False once this entry has been evicted from the store.
         *
         * Callers may hold a direct reference to an entry to skip the map lookup
         * (see ModelCache.CachedModel.instanceEntry). Its key stays the same after
         * eviction, so the key alone cannot tell them the reference is stale — and
         * using a stale entry would draw whatever mesh now occupies that arena slot.
         */
        var resident: Boolean = true

        /**
         * Size of the arena block this entry OWNS, which is what it was first
         * appended with. [vertexCount] and the material batches may describe less
         * than this after a refit; the difference stays owned rather than becoming a
         * hole, so the arena cannot fragment.
         */
        var vertexCapacity: Int = 0
        var indexCapacity: Int = 0

        /** First index this entry owns; its material batches are laid out from here. */
        var indexBase: Int = 0

        /** Total indices this mesh owns — one contiguous run in the shared IBO. */
        fun indexSpan(): Int {
            var span = 0
            for (batch in materialBatches) span += batch.indexCount
            return span
        }

        fun resetInstances() {
            instanceCount = 0
        }

        /** Reserves space for one more instance and returns its base offset into [instanceData]. */
        fun reserveInstanceSlot(): Int {
            val base = instanceCount * INSTANCE_FLOATS
            if (base + INSTANCE_FLOATS > instanceData.size) {
                instanceData = instanceData.copyOf(instanceData.size * 2)
            }
            instanceCount++
            return base
        }
    }

    /**
     * Tracks a material batch within a mesh, with its index range in the shared IBO.
     */
    class MaterialBatchEntry(
        val materialId: Int,
        /** Offset into the shared index buffer (in indices, not bytes) */
        var indexOffset: Int = 0,
        /** Number of indices for this material batch */
        var indexCount: Int = 0
    )

    // PERSISTENT mesh tracking: geometry appended once per unique mesh and kept across
    // frames (per-frame re-copy + re-upload of all mesh geometry was the dominant CPU
    // cost). Only the per-entry instance matrices are per-frame state.
    private val meshEntries = LinkedHashMap<Long, MeshEntry>(MAX_UNIQUE_MESHES)

    /**
     * One draw the frame may issue: a mesh's index run for a single material.
     *
     * The index offset lives on the [MaterialBatchEntry] itself and is rewritten in
     * place by compaction, so these references survive an arena compaction without
     * needing to be rebuilt.
     */
    private class DrawRef(val mesh: MeshEntry, val batch: MaterialBatchEntry)

    /**
     * Draws grouped by material, maintained as meshes are added and evicted rather
     * than rebuilt per frame.
     *
     * Drawing mesh-major meant the material — and therefore the bound texture and
     * the combine-mode/alpha uniforms — changed on very nearly every one of ~1500
     * draws. Grouping by material collapses that to one state change per material
     * actually present in the scene.
     */
    private val drawsByMaterial = HashMap<Int, ArrayList<DrawRef>>()

    /** Material ids in draw order (ascending, untextured first). Rebuilt when the set changes. */
    private var materialOrder = IntArray(0)
    private var materialOrderDirty = true

    /**
     * Entries that received at least one instance since [beginFrame].
     *
     * The resident store holds every mesh the session has seen — hundreds of them —
     * but a frame only draws the handful the scene walk actually reached. Resetting
     * and packing by walking the whole store made both ends of the frame scale with
     * the store rather than with the scene.
     */
    private val activeEntries = ArrayList<MeshEntry>(MAX_UNIQUE_MESHES)

    /** Heap staging for the frame's packed instance data; see [endFrame]. */
    private var instancePackScratch = FloatArray(4096 * INSTANCE_FLOATS)
    private var totalVertexCount = 0
    private var totalIndexCount = 0

    /**
     * High-water marks of what the GPU already holds. Geometry is APPEND-ONLY
     * between compactions, so each frame only needs the new tail range uploaded
     * — a whole-store upload was re-sending ~19MB of unchanged vertex data every
     * frame that any mesh appeared (the 11.6ms `geoUp` in the frame profile).
     * Compaction rewrites every offset, so it resets both marks to 0.
     */
    private var uploadedVertexCount = 0
    private var uploadedIndexCount = 0

    /**
     * Vertex ranges already inside [uploadedVertexCount] whose contents changed and
     * must be re-sent, as (firstVertex, vertexCount) pairs.
     *
     * Animated models live in this store too: their geometry is rebuilt only when the
     * pose actually changes (at the 50Hz logic rate, not the render rate), and the
     * rebuild overwrites the mesh in place rather than appending a new one. Without
     * in-place update every pose would append a single-use mesh and thrash the LRU;
     * without the dirty list the GPU would keep drawing the previous pose.
     */
    private var dirtyRanges = IntArray(256)
    private var dirtyRangeCount = 0

    /** Times a resident entry was found not to describe the incoming geometry. */
    private var mismatchReports = 0

    /**
     * Whether [entry]'s resident geometry still has the same shape as [cachedModel].
     *
     * Vertex count alone is not enough: an in-place refresh reuses the stored index
     * runs, so the material grouping has to match too or the refreshed vertices would
     * be drawn under the wrong material.
     */
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

    /**
     * Rewrites [entry] to describe [cachedModel] inside the arena block it already
     * owns, or returns false if the new geometry does not fit.
     *
     * Animated models are keyed by the ModelGL instance, and those instances come
     * from a pool of preallocated copy slots — so a slot that held one entity's pose
     * is later reused for a completely different model. Dropping and re-appending on
     * every such reuse leaks the old block until the next whole-arena compaction, and
     * the store climbed toward capacity in a couple of minutes of play. Refitting
     * keeps the arena bounded by the number of live slots instead.
     */
    private fun refitEntry(entry: MeshEntry, cachedModel: ModelCache.CachedModel): Boolean {
        val newVertexCount = cachedModel.vertexCount
        var newIndexCount = 0
        for (b in cachedModel.materialBatches) newIndexCount += b.indexCount
        if (newVertexCount > entry.vertexCapacity || newIndexCount > entry.indexCapacity) return false
        // Never rewrite geometry another submission in THIS frame is already drawing
        // from — that submission recorded its instances against the old contents.
        if (entry.lastUsedFrame == frameCounter) return false

        copyVerticesInto(cachedModel, entry.vertexOffset, newVertexCount)
        markVertexRangeDirty(entry.vertexOffset, newVertexCount)

        // Rebuild the material batches over the entry's own index range.
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
        cachedModel.instanceEntry = entry
        refits++
        return true
    }

    /** Entries refitted in place since the last stats reset. */
    private var refits = 0


    /** Index ranges already uploaded whose contents changed; see [dirtyRanges]. */
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

    /**
     * Copies [cachedModel]'s vertex block into the staging buffer at [firstVertex].
     *
     * Model-owned geometry (animated poses) keeps its authoritative copy as raw
     * words, so it is transferred straight from there — no intermediate ByteBuffer,
     * and therefore one copy per changed pose instead of two.
     */
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
        if (firstVertex >= uploadedVertexCount) return  // still inside the un-sent tail
        if (dirtyRangeCount * 2 + 2 > dirtyRanges.size) {
            dirtyRanges = dirtyRanges.copyOf(dirtyRanges.size * 2)
        }
        dirtyRanges[dirtyRangeCount * 2] = firstVertex
        dirtyRanges[dirtyRangeCount * 2 + 1] = vertexCount
        dirtyRangeCount++
    }

    /**
     * Presented-frame counter (bumped by [resetFrameStats]). Used only for LRU
     * eviction bookkeeping — see [MeshEntry.lastUsedFrame].
     */
    private var frameCounter = 0L

    /** Reusable staging buffer for the overlapping forward copies in [compactGeometryStore]. */
    private val compactStaging = ByteArray(64 * 1024)

    /** Meshes evicted by [evictAndCompact] since the last stats reset. */
    private var meshesEvicted = 0

    /** Times a mesh could not fit even after eviction (live set alone oversubscribed). */
    private var capacityFallbacks = 0

    /**
     * Whether `glDrawElementsInstancedBaseInstance` (GL 4.2 / ARB_base_instance) is
     * available. When it is, one packed instance buffer serves the whole frame with
     * no attribute re-pointing at all; the 3.3 fallback re-points instead.
     */
    private var baseInstanceSupported = false

    /**
     * Whether `glMultiDrawElementsIndirect` (GL 4.3 / ARB_multi_draw_indirect) is
     * available, letting a whole material's draws go out as one GL call.
     */
    private var indirectSupported = false
    private var indirectVbo = 0
    private var indirectBuffer: IntBuffer? = null

    /** Staging for the indirect command list; five ints per command. */
    private var indirectScratch = IntArray(MAX_UNIQUE_MESHES * 8 * INDIRECT_INTS)

    /** Per-material slice of the frame's indirect command list. */
    private val indirectFirst = HashMap<Int, Int>()
    private val indirectCount = HashMap<Int, Int>()

    /**
     * Points the per-instance attributes at instance [base] within the packed buffer.
     * Only used on the no-base-instance fallback path.
     */
    private fun repointInstanceAttributes(base: Int) {
        val byteOffset = base.toLong() * INSTANCE_STRIDE
        instanceVbo!!.bind()
        GLWrapper.vertexAttribPointer(5, 4, FLOAT, false, INSTANCE_STRIDE, byteOffset)
        GLWrapper.vertexAttribPointer(6, 4, FLOAT, false, INSTANCE_STRIDE, byteOffset + 16)
        GLWrapper.vertexAttribPointer(7, 4, FLOAT, false, INSTANCE_STRIDE, byteOffset + 32)
        GLWrapper.vertexAttribPointer(8, 4, FLOAT, false, INSTANCE_STRIDE, byteOffset + 48)
        GLWrapper.vertexAttribPointer(9, 4, FLOAT, false, INSTANCE_STRIDE, byteOffset + 64)
    }

    // State
    private var isInitialized = false
    private var isFrameActive = false

    // Statistics (per presented frame; reset via resetFrameStats)
    private var instancedDrawCalls = 0
    private var instancedTriangles = 0
    private var instancedModels = 0
    private var instancedMeshesReused = 0

    /** Instances added since the current beginFrame (drives hasPendingGeometry). */
    private var instancesThisBatch = 0

    // Timing (nanoseconds, per endFrame)
    private var timeGeomUpload = 0L
    private var timeInstanceUpload = 0L
    private var timeDraw = 0L

    /**
     * Initializes GPU resources. Must be called on GL thread.
     */
    fun initialize() {
        if (isInitialized) return

        shader = createInstancedShader()

        // Cache uniform locations
        shader!!.bind()
        uTextureLocation = shader!!.getUniformLocation("uTexture")
        uTextureEnabledLocation = shader!!.getUniformLocation("uTextureEnabled")
        uCombineModeLocation = shader!!.getUniformLocation("uCombineMode")
        uTexAlphaLocation = shader!!.getUniformLocation("uTexAlpha")
        uAlphaThresholdLocation = shader!!.getUniformLocation("uAlphaThreshold")
        uViewProjectionLocation = shader!!.getUniformLocation("uViewProjection")
        uSunDirectionLocation = shader!!.getUniformLocation("uSunDirection")
        uPointLightCountLocation = shader!!.getUniformLocation("uPointLightCount")
        uPointLightPosLocation = shader!!.getUniformLocation("uPointLightPos")
        uPointLightColorLocation = shader!!.getUniformLocation("uPointLightColor")
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

        // Create GPU resources
        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.STREAM, IndexBuffer.IndexType.UNSIGNED_INT)
        instanceVbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)

        val caps = org.lwjgl.opengl.GL.getCapabilities()
        baseInstanceSupported = caps.OpenGL42 || caps.GL_ARB_base_instance
        // Indirect draws carry their own baseInstance, so they need that support too.
        indirectSupported = baseInstanceSupported && (caps.OpenGL43 || caps.GL_ARB_multi_draw_indirect)
        if (indirectSupported) {
            indirectVbo = glGenBuffers()
            indirectBuffer = MemoryUtil.memAllocInt(indirectScratch.size)
        }
        println("[InstancedModelBatcher] baseInstance=$baseInstanceSupported multiDrawIndirect=$indirectSupported")

        // Setup VAO with vertex format + instanced attributes
        setupVAO()

        // Reserve the GPU store at full size up front. uploadSubRange never grows a
        // buffer, and pre-allocating also means no glBufferData reallocation churn.
        vbo!!.allocate(MAX_TOTAL_VERTICES.toLong() * VERTEX_STRIDE)
        ibo!!.allocate(MAX_TOTAL_INDICES)

        // Allocate CPU staging buffers
        vertexBuffer = MemoryUtil.memAlloc(MAX_TOTAL_VERTICES * VERTEX_STRIDE)
        indexBuffer = MemoryUtil.memAllocInt(MAX_TOTAL_INDICES)
        instanceBuffer = MemoryUtil.memAllocFloat(MAX_INSTANCES_PER_FRAME * INSTANCE_FLOATS)

        isInitialized = true
    }

    private fun setupVAO() {
        val va = vao!!

        va.bind()

        // Bind mesh VBO and set per-vertex attributes
        vbo!!.bind()
        // Position: vec3 float at location 0
        GLWrapper.vertexAttribPointer(0, 3, FLOAT, false, VERTEX_STRIDE, 0)
        GLWrapper.enableVertexAttribArray(0)
        // Normal: vec3 float at location 1
        GLWrapper.vertexAttribPointer(1, 3, FLOAT, false, VERTEX_STRIDE, 12)
        GLWrapper.enableVertexAttribArray(1)
        // TexCoord: vec2 float at location 2
        GLWrapper.vertexAttribPointer(2, 2, FLOAT, false, VERTEX_STRIDE, 24)
        GLWrapper.enableVertexAttribArray(2)
        // Color: vec4 ubyte normalized at location 3
        GLWrapper.vertexAttribPointer(3, 4, UNSIGNED_BYTE, true, VERTEX_STRIDE, 32)
        GLWrapper.enableVertexAttribArray(3)

        // Bind instance VBO and set per-instance attributes (mat4 = 4 x vec4)
        instanceVbo!!.bind()
        // mat4 column 0 at location 5
        GLWrapper.vertexAttribPointer(5, 4, FLOAT, false, INSTANCE_STRIDE, 0)
        GLWrapper.enableVertexAttribArray(5)
        GLWrapper.vertexAttribDivisor(5, 1)
        // mat4 column 1 at location 6
        GLWrapper.vertexAttribPointer(6, 4, FLOAT, false, INSTANCE_STRIDE, 16)
        GLWrapper.enableVertexAttribArray(6)
        GLWrapper.vertexAttribDivisor(6, 1)
        // mat4 column 2 at location 7
        GLWrapper.vertexAttribPointer(7, 4, FLOAT, false, INSTANCE_STRIDE, 32)
        GLWrapper.enableVertexAttribArray(7)
        GLWrapper.vertexAttribDivisor(7, 1)
        // mat4 column 3 at location 8
        GLWrapper.vertexAttribPointer(8, 4, FLOAT, false, INSTANCE_STRIDE, 48)
        GLWrapper.enableVertexAttribArray(8)
        GLWrapper.vertexAttribDivisor(8, 1)
        // Per-instance light indices (4 slots, -1 = unused) at location 9
        GLWrapper.vertexAttribPointer(9, 4, FLOAT, false, INSTANCE_STRIDE, 64)
        GLWrapper.enableVertexAttribArray(9)
        GLWrapper.vertexAttribDivisor(9, 1)

        // Bind IBO
        ibo!!.bind()

        va.unbind()
    }

    /**
     * Creates the instanced rendering shader.
     * Uses per-instance attributes (mat4 at locations 5-8) instead of UBO + modelIndex.
     */
    private fun createInstancedShader(): ShaderProgram {
        val vertexSource = """
            #version 330 core
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

            void main() {
                // Reconstruct model matrix from per-instance attributes
                mat4 modelMat = mat4(aModelMatCol0, aModelMatCol1, aModelMatCol2, aModelMatCol3);

                vec4 worldPos = modelMat * vec4(aPosition, 1.0);
                gl_Position = uViewProjection * worldPos;

                vTexCoord = aTexCoord;

                // GPU-side lighting calculation matching legacy formula exactly
                mat3 normalMat = mat3(modelMat);
                vec3 worldNormal = normalMat * aNormal; // length carries 768/contrast — do NOT normalise

                float dotProduct = dot(uSunDirection, worldNormal);
                float lightIntensity = uSunBrightness + dotProduct * (dotProduct < 0.0 ? uContrastNeg : uContrastPos);

                vec3 litRGB = aColor.rgb * uSunColor * lightIntensity;

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
                    float att = 1.0 / max(uPointLightPos[i].w * d2, 1e-6);
                    float ndotl = max(dot(worldNormal, normalize(toLight)), 0.0);
                    litRGB += aColor.rgb * uPointLightColor[i] * (att * ndotl);
                }

                litRGB = clamp(litRGB, 0.0, 1.0);

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
            #version 330 core

            in vec2 vTexCoord;
            in vec4 vColor;
            in float vFogFactor;
            uniform vec3 uFogColor2;

            uniform sampler2D uTexture;
            uniform bool uTextureEnabled;
            uniform float uAlphaThreshold;
            // Legacy per-material texture combine mode (see OptimizedFrameBatcher /
            // groundTextured.frag). REPLACE(1) ignores the per-face vColor — required
            // or textured faces with junk face colors render tinted (green-quad bug).
            uniform int uCombineMode;
            uniform bool uTexAlpha;

            out vec4 fragColor;

            void main() {
                vec4 color;
                if (uTextureEnabled) {
                    vec4 texColor = texture(uTexture, vTexCoord);
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

    /**
     * Sets the view-projection matrix for this frame.
     */
    fun setViewProjection(vp: FloatArray) {
        // Copy VP and negate Y row: RS VP has Y-down clip space, OpenGL expects Y-up.
        val flipped = vp.copyOf()
        flipped[1] = -flipped[1]
        flipped[5] = -flipped[5]
        flipped[9] = -flipped[9]
        flipped[13] = -flipped[13]
        this.vpMatrix = flipped
    }

    /** Mirrors legacy's GL_LINEAR fog state for this frame. */
    fun setFog(view: FloatArray, enabled: Boolean, colorRgb: Int, start: Float, end: Float) {
        viewMatrix = view
        fogOn = enabled
        fogR = ((colorRgb shr 16) and 0xff) / 255f
        fogG = ((colorRgb shr 8) and 0xff) / 255f
        fogB = (colorRgb and 0xff) / 255f
        fogStart = start
        fogEnd = end
    }

    /**
     * Sets the lighting parameters for this frame.
     */
    // ===== Point lights =====
    // Legacy sets GL_LIGHT2+ per model in copyLights; this batcher draws models in
    // material batches, so the frame's lights are uploaded once as a small array and
    // every fragment accumulates them. Quadratic falloff means distant lights
    // contribute ~0, which is what legacy's per-model light selection approximates.
    private var pointLightCount = 0
    private val pointLightPos = FloatArray(MAX_POINT_LIGHTS * 4)
    private val pointLightColor = FloatArray(MAX_POINT_LIGHTS * 3)

    /** [count] lights; [pos] is xyzw per light (w = 1/size2d^2), [color] is rgb per light. */
    fun setTextureAlphaPredicate(p: (Int) -> Boolean) { texHasAlpha = p }

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

    /**
     * Begins a new frame.
     */
    fun beginFrame() {
        check(isInitialized) { "InstancedModelBatcher not initialized" }
        check(!isFrameActive) { "Frame already active" }

        // Geometry persists across frames — only reset the per-BATCH instance lists,
        // and only for the entries that actually collected instances.
        for (i in activeEntries.indices) activeEntries[i].resetInstances()
        activeEntries.clear()
        instancesThisBatch = 0

        // NOTE: the presented-frame stats (instancedDrawCalls/Triangles/Models/...)
        // are NOT reset here — beginFrame runs at every mid-frame batch reopen, which
        // made the diag read zeros. They reset once per presented frame via
        // resetFrameStats() from ModernGLToolkit.render().

        isFrameActive = true
    }

    /**
     * Resets the per-presented-frame statistics. Called once per presented frame
     * (ModernGLToolkit.render), after the diag has read them.
     */
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
        // Advance the LRU clock at the presented-frame boundary. Entries stamped
        // with the new value during the frame that follows are eviction-immune.
        frameCounter++
    }

    /**
     * Adds an instance of a cached model to the batch.
     *
     * If this is the first instance of this mesh (identified by modelId),
     * the geometry is recorded. Subsequent instances of the same mesh
     * only record the model matrix -- no geometry duplication.
     *
     * @param cachedModel Pre-processed model data from ModelCache
     * @param modelId Content hash identifying this unique mesh
     * @param modelMatrix The instance's world transform
     * @return true if the instance was added, false if capacity exceeded
     */
    fun addInstance(
        cachedModel: ModelCache.CachedModel,
        modelId: Long,
        modelMatrix: Matrix43,
        lightIdx: FloatArray? = null,
        /**
         * The caller rebuilt [cachedModel]'s vertex data since the last submission of
         * this [modelId] (an animated model that changed pose). The resident copy is
         * overwritten in place and queued for re-upload.
         */
        geometryDirty: Boolean = false
    ): Boolean {
        if (!isFrameActive) return false

        var entry = cachedModel.instanceEntry as? MeshEntry
        var shapeKnownGood = false
        if (entry != null && (!entry.resident || entry.modelId != modelId)) {
            cachedModel.instanceEntry = null
            entry = null
        } else if (entry != null && !geometryDirty) {
            // The memo was set when this exact entry was written from this exact
            // cached model, and a cached model's shape only changes when the caller
            // rebuilds it — which is what geometryDirty reports. So the structural
            // comparison below can be skipped entirely on the common path; it walks
            // two lists of material batches and, at ~1800 submissions a frame, showed
            // up as ~4% of the render thread.
            shapeKnownGood = true
        }
        if (entry == null) {
            // Keyed lookup only on the first submission of this geometry. The map is a
            // LinkedHashMap with boxed Long keys, and at ~1700 model draws a frame the
            // boxing and bucket walk were measurable on their own.
            entry = meshEntries[modelId]
            if (entry != null) cachedModel.instanceEntry = entry
        }

        if (!shapeKnownGood && entry != null && !entryMatches(entry, cachedModel) && refitEntry(entry, cachedModel)) {
            // Refit succeeded: the entry now describes the incoming geometry within
            // the arena block it already owns, so nothing was dropped or appended.
            entry.lastUsedFrame = frameCounter
        } else if (!shapeKnownGood && entry != null && !entryMatches(entry, cachedModel)) {
            // The persistent store must never serve another mesh's geometry, and an
            // in-place refresh must never land on a differently-shaped mesh. Either
            // means the resident entry no longer describes the incoming geometry
            // (hash collision, external invalidation, or an animated model whose face
            // set or material grouping changed) — drop that entry and re-append it
            // below. No whole-store rebuild.
            if (mismatchReports < 20) {
                mismatchReports++
                println("[InstancedModelBatcher] resident entry no longer matches key=$modelId " +
                    "(stored=${entry.vertexCount}v/${entry.materialBatches.size}batches " +
                    "incoming=${cachedModel.vertexCount}v/${cachedModel.materialBatches.size}batches) — re-appending")
            }
            cachedModel.instanceEntry = null
            meshEntries.remove(modelId)
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

        if (entry != null && geometryDirty) {
            // Same mesh, new contents. Overwrite the resident vertices in place; the
            // index run is unchanged because a pose change rewrites positions,
            // normals and colours but never the face set or its material grouping.
            copyVerticesInto(cachedModel, entry.vertexOffset, entry.vertexCount)
            markVertexRangeDirty(entry.vertexOffset, entry.vertexCount)
        }

        if (entry == null) {
            // First time this mesh is EVER seen -- append its geometry to the
            // persistent store (kept across frames; uploaded only when dirty).
            val vertexCount = cachedModel.vertexCount

            // Calculate index count needed
            var meshIndexCount = 0
            for (batch in cachedModel.materialBatches) {
                meshIndexCount += batch.indexCount
            }

            // The store is append-only within a frame; when it fills, evict the
            // least-recently-used meshes (never this frame's) and compact the arena.
            if (totalVertexCount + vertexCount > MAX_TOTAL_VERTICES ||
                totalIndexCount + meshIndexCount > MAX_TOTAL_INDICES
            ) {
                if (!evictAndCompact(vertexCount, meshIndexCount)) {
                    // This frame's live meshes alone exceed capacity — nothing is
                    // safe to evict. Stream this mesh via the regular batcher.
                    // Surfaced via the [ModernPerf] store[overflow=] counter rather
                    // than a println: this fires per model, not per frame.
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

            // Bulk copy vertex data: staging stride is the cache's native 40-byte
            // format (the 4-byte modelIndex slot is padding the VAO ignores), so a
            // single transfer replaces the old per-byte strip-copy loop.
            copyVerticesInto(cachedModel, entry.vertexOffset, vertexCount)

            // Record material batches with index offsets
            for (batch in cachedModel.materialBatches) {
                val matEntry = MaterialBatchEntry(
                    materialId = batch.materialId,
                    indexOffset = totalIndexCount,
                    indexCount = batch.indexCount
                )

                // Copy indices, adjusting by vertex offset
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

        // Stamp the LRU clock: entries touched this presented frame are pinned
        // against eviction for the rest of it.
        entry.lastUsedFrame = frameCounter

        // Check instance limit
        if (entry.instanceCount >= MAX_INSTANCES_PER_MESH) return false

        if (entry.instanceCount == 0) activeEntries.add(entry)

        // Record instance matrix in place (convert Matrix43 to column-major mat4);
        // no per-instance allocation.
        val base = entry.reserveInstanceSlot()
        val data = entry.instanceData
        // Column 0
        data[base] = modelMatrix.xA
        data[base + 1] = modelMatrix.yA
        data[base + 2] = modelMatrix.zA
        data[base + 3] = 0f
        // Column 1
        data[base + 4] = modelMatrix.xB
        data[base + 5] = modelMatrix.yB
        data[base + 6] = modelMatrix.zB
        data[base + 7] = 0f
        // Column 2
        data[base + 8] = modelMatrix.xC
        data[base + 9] = modelMatrix.yC
        data[base + 10] = modelMatrix.zC
        data[base + 11] = 0f
        // Column 3
        data[base + 12] = modelMatrix.xD
        data[base + 13] = modelMatrix.yD
        data[base + 14] = modelMatrix.zD
        data[base + 15] = 1f

        // Per-entity light indices (-1 = unused slot)
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

    /**
     * Frees room for a mesh of [neededVertices]/[neededIndices] by evicting the
     * least-recently-used entries and compacting the arena.
     *
     * The store used to be append-only with a whole-store nuke on overflow, which
     * freed nothing when the live working set was near capacity: it refilled and
     * overflowed again the same frame, re-uploading the entire VBO every time.
     * LRU eviction releases only cold meshes and keeps hot ones resident.
     *
     * Entries touched during the current presented frame are never candidates, so
     * geometry this frame's draws reference can never move out from under them.
     *
     * @return true if there is now room, false if the un-evictable (this-frame)
     *   set alone exceeds capacity — the caller should stream the mesh instead.
     */
    private fun evictAndCompact(neededVertices: Int, neededIndices: Int): Boolean {
        // Coldest first; entries stamped with the current frame are pinned.
        val candidates = ArrayList<MeshEntry>(meshEntries.size)
        for (entry in meshEntries.values) {
            if (entry.lastUsedFrame != frameCounter) candidates.add(entry)
        }
        candidates.sortBy { it.lastUsedFrame }

        // Evict past the point of merely fitting, down to the hysteresis target,
        // so the next few hundred inserts need no compaction at all.
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

        // Cold entries alone may not reach the target; succeeding only requires
        // that the incoming mesh now fits.
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

    /**
     * Slides the surviving entries down over the holes left by eviction, in
     * ascending arena order, and rebases their vertex/index offsets.
     *
     * Stored indices are ABSOLUTE vertex indices (addInstance writes
     * `baseVertex + batch.indices[i]`), so a moved mesh's indices shift by the
     * same delta as its vertex block.
     */
    private fun compactGeometryStore() {
        val vb = vertexBuffer!!
        val ib = indexBuffer!!
        // Arena order, not map order: entries must be packed in the direction they
        // already lie so every copy moves DOWN (dst < src) and can never clobber a
        // block it has not read yet.
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
            // Compaction packs entries tight, so the unused tail of a refitted block
            // is released here; capacity becomes exactly what is now in use.
            entry.indexCapacity = writeIndex - entry.indexBase
            entry.vertexOffset = writeVertex
            entry.vertexCapacity = entry.vertexCount
            writeVertex += entry.vertexCount
        }

        totalVertexCount = writeVertex
        totalIndexCount = writeIndex
        // Every surviving mesh moved, so the GPU copy is stale end to end — which
        // also makes every queued dirty range meaningless.
        uploadedVertexCount = 0
        uploadedIndexCount = 0
        dirtyRangeCount = 0
        dirtyIndexRangeCount = 0
    }

    /**
     * Forward chunked copy within the vertex arena. Always called with
     * `dstByte < srcByte`; staging each chunk out before writing it back keeps
     * overlapping ranges correct (ByteBuffer bulk puts give no overlap guarantee).
     */
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

    /**
     * Returns the set of mesh entries that have enough instances to benefit from instancing.
     * Entries below the threshold should be rendered by the regular batcher.
     */
    fun getInstancedEntries(): Collection<MeshEntry> {
        return meshEntries.values.filter { it.instanceCount >= INSTANCING_THRESHOLD }
    }

    /**
     * Returns mesh entries that do NOT meet the instancing threshold.
     * These should fall through to the regular OptimizedFrameBatcher path.
     */
    fun getNonInstancedEntries(): Collection<MeshEntry> {
        return meshEntries.values.filter { it.instanceCount < INSTANCING_THRESHOLD }
    }

    /**
     * Ends the frame and renders all instanced geometry.
     *
     * @param textureCache Lambda to resolve material IDs to GL texture handles
     */
    fun endFrame(
        textureCache: (Int) -> Int,
        combineModeOf: (Int) -> Int = { 0 },
        needsBlendOf: (Int) -> Boolean = { false },
        alphaDiscard: Boolean = true
    ) {
        check(isFrameActive) { "No active frame" }

        // Render ALL meshes, whether they have 1 or many instances.
        // glDrawElementsInstanced with instanceCount=1 is equivalent to a regular draw.
        // This is simpler than routing single-instance meshes back to the regular batcher.
        val allEntries = activeEntries
        if (allEntries.isEmpty() || vpMatrix == null) {
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

        // Upload ONLY the range appended since the last upload. Steady-state frames
        // that add a handful of meshes send a few hundred KB instead of the whole
        // ~19MB store; frames that add nothing send nothing.
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

        // Re-send meshes whose resident contents were overwritten in place.
        if (dirtyRangeCount > 0) {
            val t0 = System.nanoTime()
            val vbView = vertexBuffer!!.duplicate()
            for (r in 0 until dirtyRangeCount) {
                val first = dirtyRanges[r * 2]
                val count = dirtyRanges[r * 2 + 1]
                if (first >= uploadedVertexCount) continue
                vbView.clear()
                vbView.position(first * VERTEX_STRIDE)
                vbView.limit((first + count) * VERTEX_STRIDE)
                vbo!!.uploadSubRange(first.toLong() * VERTEX_STRIDE, vbView)
            }
            dirtyRangeCount = 0
            timeGeomUpload += System.nanoTime() - t0
        }
        if (dirtyIndexRangeCount > 0) {
            val t0 = System.nanoTime()
            val ibView = indexBuffer!!.duplicate()
            for (r in 0 until dirtyIndexRangeCount) {
                val first = dirtyIndexRanges[r * 2]
                val count = dirtyIndexRanges[r * 2 + 1]
                if (first >= uploadedIndexCount) continue
                ibView.clear()
                ibView.position(first)
                ibView.limit(first + count)
                ibo!!.uploadSubRange(first, ibView)
            }
            dirtyIndexRangeCount = 0
            timeGeomUpload += System.nanoTime() - t0
        }

        // Bind shader and set per-frame uniforms
        sh.bind()
        // Legacy back-face culls model geometry (OpenGlToolkit init:
        // glEnable(GL_CULL_FACE) + glCullFace(GL_BACK), never turned off for the
        // scene). Asserted HERE rather than in openSceneBatch because state set
        // there is cleared before the draws land. The projection flips Y, which
        // reverses winding, so front faces are CW.
        if (CULL_FACE) {
            GLWrapper.enable(GLWrapper.Constants.CULL_FACE)
            GLWrapper.cullFace(CULL_MODE)
            GLWrapper.frontFace(CULL_WIND)
        } else {
            GLWrapper.disable(GLWrapper.Constants.CULL_FACE)
        }
        sh.setUniform(uTextureLocation, 0)
        // -1 disables the shader's `color.a <= uAlphaThreshold` discard entirely,
        // so geometry lost to a zero alpha becomes visible (render alphatest).
        // Legacy: glAlphaFunc(GL_GREATER, 0) -> keep a > 0, discard a <= 0.
        sh.setUniform(
            uAlphaThresholdLocation,
            // -Drender.alphaprobe=<t> discards any fragment with alpha <= t.
            // At 0.99 only FULLY opaque fragments survive, which answers
            // "is this surface actually alpha < 255?" without guessing.
            System.getProperty("render.alphaprobe")?.toFloatOrNull()
                ?: (if (alphaDiscard) ALPHA_CUTOFF else -1f)
        )

        // Set VP matrix (already in column-major format for GLSL)
        glUniformMatrix4fv(uViewProjectionLocation, false, vp)

        // Set lighting uniforms
        sh.setUniform(uSunDirectionLocation, sunDirX, sunDirY, sunDirZ)
        // Point lights for this frame (see setPointLights).
        if (uPointLightCountLocation >= 0) {
            GLWrapper.uniform1i(uPointLightCountLocation, pointLightCount)
            if (pointLightCount > 0) {
                if (uPointLightPosLocation >= 0) GLWrapper.uniform4fv(uPointLightPosLocation, pointLightPos)
                if (uPointLightColorLocation >= 0) GLWrapper.uniform3fv(uPointLightColorLocation, pointLightColor)
            }
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

        // Bind VAO (has both vertex and instance attribute pointers configured)
        vao!!.bind()
        ibo!!.bind()

        // ---- Pack every instance in the frame into ONE buffer, upload ONCE ----
        //
        // See MAX_INSTANCES_PER_FRAME: uploading per mesh put a write-after-read
        // hazard between every draw and the next mesh's matrices, which the driver
        // resolved by serialising the whole pass.
        val t1 = System.nanoTime()
        // Gather on the heap first: array-to-array copies are plain memmoves, whereas
        // ~1800 small puts straight into the direct buffer each paid the NIO bounds
        // and pointer checks around Unsafe.copyMemory.
        var scratch = instancePackScratch
        var packedInstances = 0
        for (entry in allEntries) {
            val count = entry.instanceCount
            if (count == 0) continue
            if (packedInstances + count > MAX_INSTANCES_PER_FRAME) {
                // Nothing sensible to do but drop the overflow; surfaced through the
                // instance counter rather than a per-frame println.
                entry.instanceCount = 0
                continue
            }
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
        if (packedInstances > 0) {
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
            // Ascending material id: -1 (untextured) first, matching the order the
            // per-mesh loop used to produce.
            val ids = drawsByMaterial.keys.toIntArray()
            java.util.Arrays.sort(ids)
            materialOrder = ids
            materialOrderDirty = false
        }

        val t2 = System.nanoTime()

        // Build EVERY material's indirect commands into one buffer and upload it once.
        // Uploading per material re-orphaned the indirect buffer between draws that
        // were still reading it — the same write-after-read hazard that made the
        // per-mesh instance uploads serialise the whole pass.
        var indirectCommandCount = 0
        if (indirectSupported) {
            val buf = indirectScratch
            var cursor = 0
            for (texId in materialOrder) {
                val refs = drawsByMaterial[texId] ?: continue
                val first = indirectCommandCount
                for (r in refs) {
                    val mesh = r.mesh
                    val count = mesh.instanceCount
                    if (count == 0) continue
                    val matBatch = r.batch
                    if (cursor + INDIRECT_INTS > buf.size) break
                    buf[cursor] = matBatch.indexCount        // count
                    buf[cursor + 1] = count                  // instanceCount
                    buf[cursor + 2] = matBatch.indexOffset   // firstIndex
                    buf[cursor + 3] = 0                      // baseVertex (indices are absolute)
                    buf[cursor + 4] = mesh.instanceBase      // baseInstance
                    cursor += INDIRECT_INTS
                    indirectCommandCount++
                    instancedTriangles += (matBatch.indexCount / 3) * count
                }
                indirectFirst[texId] = first
                indirectCount[texId] = indirectCommandCount - first
            }
            if (indirectCommandCount > 0) {
                val ib = indirectBuffer!!
                ib.clear()
                ib.put(buf, 0, cursor)
                ib.flip()
                glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirectVbo)
                glBufferData(GL_DRAW_INDIRECT_BUFFER, ib, GL_STREAM_DRAW)
            }
        }

        for (texId in materialOrder) {
            val refs = drawsByMaterial[texId] ?: continue

            // One texture/uniform setup per material, not per draw.
            val textureHandle = if (texId >= 0) textureCache(texId) else 0
            if (textureHandle > 0) {
                GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                GLWrapper.bindTexture(TEXTURE_2D, textureHandle)
                sh.setUniform(uTextureEnabledLocation, 1)
                // Legacy per-material combine mode (REPLACE ignores vColor).
                sh.setUniform(uCombineModeLocation, combineModeOf(texId))
                sh.setUniform(uTexAlphaLocation, if (TEX_ALPHA && texHasAlpha(texId)) 1 else 0)
            } else {
                sh.setUniform(uTextureEnabledLocation, 0)
            }

            // Per-material blend state is deliberately NOT toggled here: legacy does
            // not do it for model faces (setBlendMode(1) once in setupMatrixRendering),
            // and doing it made translucent geometry render solid. The pass-wide state
            // is established in openSceneBatch.
            if (indirectSupported) {
                // One GL call for the whole material, reading the commands already
                // resident in the indirect buffer.
                val commands = indirectCount[texId] ?: 0
                if (commands > 0) {
                    val byteOffset = (indirectFirst[texId] ?: 0).toLong() * INDIRECT_INTS * 4
                    glMultiDrawElementsIndirect(TRIANGLES, UNSIGNED_INT, byteOffset, commands, 0)
                    instancedDrawCalls++
                }
            } else {
                for (r in refs) {
                    val mesh = r.mesh
                    val count = mesh.instanceCount
                    if (count == 0) continue
                    val matBatch = r.batch
                    if (baseInstanceSupported) {
                        glDrawElementsInstancedBaseInstance(
                            TRIANGLES,
                            matBatch.indexCount,
                            UNSIGNED_INT,
                            matBatch.indexOffset.toLong() * 4,
                            count,
                            mesh.instanceBase
                        )
                    } else {
                        // GL 3.3 fallback: re-point the instance attributes at this mesh's
                        // slice. Five cheap pointer updates, still no data transfer and
                        // still no hazard against the buffer.
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
        timeDraw += System.nanoTime() - t2

        vao!!.unbind()
        sh.unbind()

        isFrameActive = false
    }

    fun isActive(): Boolean = isFrameActive

    /** True when the open BATCH has collected instances that have not been flushed yet.
     *  (Mesh geometry persists across frames and the frame stats accumulate across
     *  batch reopens, so neither is the signal — only this batch's instance count.) */
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
        /** Meshes LRU-evicted from the geometry store this frame. */
        val meshesEvicted: Int = 0,
        /** Meshes that could not fit even after eviction and fell back to streaming. */
        val capacityFallbacks: Int = 0,
        /** Vertices currently resident in the persistent geometry store. */
        val storeVertices: Int = 0,
        /** Entries rewritten inside the arena block they already owned. */
        val refits: Int = 0,
        val geomUploadMs: Float = 0f,
        val instanceUploadMs: Float = 0f,
        val drawMs: Float = 0f
    )
}
