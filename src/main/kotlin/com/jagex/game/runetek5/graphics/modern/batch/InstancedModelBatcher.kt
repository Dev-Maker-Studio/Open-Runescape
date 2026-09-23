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
import org.lwjgl.opengl.GL42.glDrawElementsInstancedBaseInstance
import org.lwjgl.opengl.GL43.GL_DRAW_INDIRECT_BUFFER
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER
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

        /** Per-draw bindless materials (one multi-draw per pass). -Drender.bindless=false disables. */
        @JvmStatic val BINDLESS_ENABLED = System.getProperty("render.bindless")?.equals("false", true) != true

        /** Debug: models show only their point-light term (-Drender.lightonly=1). */
        @JvmStatic val LIGHT_ONLY_DEBUG = System.getProperty("render.lightonly") != null
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
        private const val STALE_ENTRY_FRAMES = 600L

        /** -Drender.transsort=false draws translucent items in submission order (A/B aid). */
        @JvmStatic val TRANS_SORT = System.getProperty("render.transsort")?.equals("false", true) != true

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
        /** Bumped whenever this entry's index ranges change (refit); block templates compare it. */
        var layoutStamp: Int = 0
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
     * A prebuilt opaque draw for a group of resident meshes (the retained scene's
     * per-chunk block): packed instances plus an indirect-command template whose
     * baseInstance is relative to the block. A frame stitches it in with two array
     * copies and a base patch instead of a per-instance/per-command build.
     */
    class BlockDraw {
        var floats = FloatArray(0)
        var count = 0
        /** 5 ints per command: indexCount, instanceCount, firstIndex, 0, relative baseInstance. */
        var cmds = IntArray(0)
        var cmdCount = 0
        /** Material id per command. */
        var mats = IntArray(0)
        /** Cached SSBO words (4 per command) and the handle revision they were resolved at. */
        var matWords = IntArray(0)
        var matWordsRevision = -1
        val entries = ArrayList<MeshEntry>(16)
        var entryLayout = IntArray(0)
        var layoutRevision = -1
        /** Frame base assigned by endFrame packing. */
        var base = 0
    }
    /** Bumped by arena compaction (every entry's offsets move). */
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

    /** True while [b]'s template still describes its meshes' current arena layout. */
    fun blockDrawValid(b: BlockDraw): Boolean {
        if (b.layoutRevision != storeLayoutRevision) return false
        val es = b.entries
        for (i in es.indices) {
            val e = es[i]
            if (!e.resident || e.layoutStamp != b.entryLayout[i]) return false
        }
        return true
    }

    /** Queues a valid block for this frame's opaque multi-draw. */
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
    // Per-frame command build scratch (bindless path): sort keys + parallel refs.
    private var cmdKeys = LongArray(4096)
    private var cmdEntry = arrayOfNulls<MeshEntry>(4096)
    private var cmdBatch = arrayOfNulls<MaterialBatchEntry>(4096)

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

    /**
     * Translucent submissions of the open batch, in SUBMISSION order.
     *
     * Legacy draws the scene's alpha bucket after every opaque entity and the floor,
     * sorted back to front, and relies on nothing but that order for correct
     * blending. Grouping those draws by material — what the opaque pass does — puts
     * a translucent face in front of geometry that has not been drawn yet, and its
     * depth write then rejects that geometry outright: whole locs vanish behind a
     * tent canopy or a window. These items therefore never enter the per-entry
     * instance lists; they are packed after the opaque instances and drawn one by
     * one, in order, by [drawTranslucent].
     */
    private var translucentEntries = arrayOfNulls<MeshEntry>(256)
    private var translucentMatrices = FloatArray(256 * INSTANCE_FLOATS)
    private var translucentDepthWrite = BooleanArray(256)
    /** Frame-wide translucent sequence number of each item (toolkit-assigned, monotonic). */
    private var translucentSeq = IntArray(256)
    /**
     * Legacy sort key of each item: the scene's floorDepth (view-space depth of the
     * entity, larger = farther). The alpha bucket is drawn far to near by this key
     * (Scene.quicksortAbove), so the pass replays items sorted by it — which also lets
     * items submitted out of walk order (retained statics) fall into their place.
     */
    private var translucentKey = IntArray(256)
    private var translucentCount = 0
    // Template items (retained translucent records): instance floats live in the
    // chunk's array, commands and material words come from the chunk's templates.
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

    /**
     * Translucent item whose instance floats ([src] at [srcOff], INSTANCE_FLOATS) and
     * command template ([tpl] at [tplOff], [tplCount] commands of 5 ints, baseInstance
     * ignored) plus material words ([matW] at [matOff], 4 per command) are prebuilt by
     * the retained scene. Only the depth key is per frame.
     */
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

    /**
     * Resolves the SSBO words of [count] material ids into [out] (4 ints each).
     * Returns false when a textured material had no handle yet, in which case the
     * words are only good for this frame and must not be cached by revision.
     */
    fun fillMaterialWords(mats: IntArray, count: Int, out: IntArray, textureCache: (Int) -> Int, combineModeOf: (Int) -> Int): Boolean {
        var complete = true
        for (k in 0 until count) {
            val slot = resolveMaterial(mats[k], textureCache, combineModeOf)
            out[k * 4] = matEntryLo[slot]; out[k * 4 + 1] = matEntryHi[slot]; out[k * 4 + 2] = matEntryFlags[slot]; out[k * 4 + 3] = matEntryScroll[slot]
            if (mats[k] >= 0 && matEntryFlags[slot] == 0) complete = false
        }
        return complete
    }
    /** Item indices in draw order (key descending, then sequence), built by [endFrame]. */
    private var translucentOrder = IntArray(256)
    private var translucentSortScratch = LongArray(256)
    /** Next position in [translucentOrder] that [drawTranslucent] will draw. */
    private var translucentCursor = 0
    /** Per-material-run groups of the translucent command list (see [drawTranslucent]). */
    private var translucentGroupTex = IntArray(256)
    private var translucentGroupDepth = BooleanArray(256)
    private var translucentGroupFirst = IntArray(256)
    private var translucentGroupCount = IntArray(256)
    /** Packed-instance slot of translucent item 0; assigned by [endFrame]. */
    private var translucentBase = 0
    /** Translucent items packed by the last [endFrame], still drawable until the next [beginFrame]. */
    private var translucentPacked = 0
    /** Current capacity of the frame-wide instance buffer, in instances. Grows on demand. */
    private var instanceCapacity = MAX_INSTANCES_PER_FRAME
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
    /** Submissions streamed because their slot's resident shape was already drawn this frame. */
    private var sameFrameShapeConflicts = 0

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
        entry.layoutStamp++
        cachedModel.instanceEntry = entry
        refits++
        return true
    }

    /** Entries refitted in place since the last stats reset. */
    private var refits = 0
    /** Translucent items folded into a preceding same-mesh command (diag). */
    @JvmField var translucentRunsMerged = 0L
    /** Commands emitted from retained block templates (diag). */
    @JvmField var blockCommandsDrawn = 0L
    /** Translucent items drawn from templates (diag). */
    @JvmField var translucentTemplateItems = 0L
    /**
     * -Drender.mergeuploads=true uploads one merged span per frame instead of one copy
     * per dirty range. Experiment only: it removed nothing (the ~40 ms present stalls
     * turned out to be X-server contention, not per-copy GPU syncs) and a span across
     * the arena costs 20-30 MB a frame. Off by default.
     */
    private val MERGE_UPLOADS = System.getProperty("render.mergeuploads")?.equals("true", true) == true
    /** Bytes sent by merged dirty-span uploads (diag). */
    @JvmField var mergedUploadBytes = 0L


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

    // ---- Bindless per-draw materials ------------------------------------------------
    // With ARB_bindless_texture + ARB_shader_draw_parameters every indirect command
    // carries its own material (texture handle, combine mode, alpha rule) in an SSBO
    // indexed by gl_DrawID, so a whole pass is ONE multi-draw instead of one draw per
    // texture change (~1200 a frame in the translucent pass alone). Sampling is the
    // same texture object with the same sampler state, so the pixels do not change.
    private var bindless = false
    private var uDrawBaseLocation = -1
    private var drawMatVbo = 0
    private var drawMatScratch = IntArray(4096 * 4)
    private var drawMatBuffer: IntBuffer? = null
    private var drawMatCursor = 0
    private val bindlessHandles = HashMap<Int, Long>()
    /** Per-pass memo: material id -> (handle lo, handle hi, flags), stamped, no boxing. */
    private var matEntryLo = IntArray(4096)
    private var matEntryHi = IntArray(4096)
    private var matEntryFlags = IntArray(4096)
    private var matEntryScroll = IntArray(4096)
    private var matEntryStamp = IntArray(4096)
    private var matEntryPass = 0

    private fun beginMaterialPass() { matEntryPass++ }

    /** Whether the per-draw material path is active (diag). */
    fun isBindless(): Boolean = bindless

    private fun textureHandle64(glTex: Int): Long {
        val known = bindlessHandles[glTex]
        if (known != null) return known
        val h = glGetTextureHandleARB(glTex)
        if (h != 0L) {
            glMakeTextureHandleResidentARB(h)
            bindlessHandles[glTex] = h
            materialWordsRevision++
        }
        return h
    }

    /** The GL texture [glTex] is being deleted: its handle must not stay resident. */
    fun releaseTextureHandle(glTex: Int) {
        val h = bindlessHandles.remove(glTex) ?: return
        glMakeTextureHandleNonResidentARB(h)
        materialWordsRevision++
    }

    /**
     * Appends one SSBO entry for a command of material [texId]:
     * (handle lo, handle hi, flags, 0) with flags = textured | combine<<1 | texAlpha<<4.
     */
    /** Bumped whenever a bindless handle appears or goes away: cached per-draw material words are stale. */
    var materialWordsRevision = 0
        private set

    /** Resolves (and caches for this pass) the SSBO words of [texId]; returns the cache slot. */
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

    /** Uploads the entries appended since [drawMatCursor] was reset and binds them at SSBO binding 0. */
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

    /** Staging for the indirect command list; five ints per command. */
    private var indirectScratch = IntArray(MAX_UNIQUE_MESHES * 8 * INDIRECT_INTS)

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

        // Cache uniform locations
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
        if (bindless && !indirectSupported) bindless = false
        if (bindless) {
            drawMatVbo = glGenBuffers()
            drawMatBuffer = MemoryUtil.memAllocInt(drawMatScratch.size)
        }
        println("[InstancedModelBatcher] baseInstance=$baseInstanceSupported multiDrawIndirect=$indirectSupported bindless=$bindless uniforms[pos=$uPointLightPosLocation col=$uPointLightColorLocation count=$uPointLightCountLocation]")

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

    /**
     * Sets the view-projection matrix for this frame.
     */
    /** Element [i] of the frame's stored (Y-flipped) VP, for diagnostics. */
    fun vpElement(i: Int): Float = vpMatrix?.get(i) ?: Float.NaN

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
        geometryDirty: Boolean = false,
        /**
         * Draw this instance in the ordered translucent pass instead of the
         * material-grouped opaque pass (the scene's alpha bucket; see
         * [drawTranslucent]).
         */
        translucent: Boolean = false,
        /** Depth writes for this instance (legacy setDepthBuffering state at submission). */
        depthWrite: Boolean = true,
        /** Frame-wide translucent sequence number (orders this item against other passes). */
        seq: Int = 0,
        /** Legacy sort key (Scene floorDepth) for the translucent pass; see [translucentKey]. */
        depthKey: Int = 0
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
            //
            // Unless the entry was already drawn from THIS frame: pose models come
            // from a pool of ModelGL slots, and one slot can carry two different
            // entities' poses in a single frame. Re-appending here every frame grew
            // the arena, re-uploaded the tail each frame and forced compactions
            // (~0.45 ms/frame of geometry upload at the spawn hub). Let the caller
            // stream this one submission instead; the resident copy stays valid for
            // the pose that first claimed the slot this frame.
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
                if (templatesQueued) {
                    deferredCompactVertices = maxOf(deferredCompactVertices, vertexCount)
                    deferredCompactIndices = maxOf(deferredCompactIndices, meshIndexCount)
                    capacityFallbacks++
                    return false
                }
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

        if (translucent) {
            appendTranslucent(entry, modelMatrix, lightIdx, depthWrite, seq, depthKey)
            instancedModels++
            instancesThisBatch++
            return true
        }

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

    /** Whether [addInstancesPacked] would accept [count] more instances of [entry] this frame. */
    fun canAddPacked(entry: MeshEntry, count: Int): Boolean =
        isFrameActive && entry.resident && entry.instanceCount + count <= MAX_INSTANCES_PER_MESH

    /**
     * Bulk-appends [count] pre-packed opaque instances ([INSTANCE_FLOATS] each, read from
     * [floats] at [offset]) for the resident mesh [entry]: the retained scene's per-chunk
     * blocks skip the per-instance lookup/refit/light path entirely.
     */
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

    /**
     * Sorts the frame's translucent items far-to-near by legacy key, ties by sequence
     * (submission order). One 64-bit key per item, sorted as primitives.
     */
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
            // (key desc, seq asc, index asc) packed into one long: 32 + 20 + 12 bits.
            scratch[i] = (((keyPart shl 20) or seqPart) shl 12) or (i.toLong() and 0xFFFL) xor Long.MIN_VALUE
        }
        if (n <= 4096) {
            java.util.Arrays.sort(scratch, 0, n)
            for (i in 0 until n) translucentOrder[i] = (scratch[i] and 0xFFFL).toInt()
        } else {
            // Wide index: sort boxed pairs (rare — thousands of translucent items).
            val idx = (0 until n).sortedWith(compareBy({ Int.MAX_VALUE.toLong() - translucentKey[it] }, { translucentSeq[it] }, { it }))
            for (i in 0 until n) translucentOrder[i] = idx[i]
        }
    }

    /** Records one ordered translucent draw of [entry]; see [translucentEntries]. */
    /** Translucent submission for a mesh already known to be resident (retained records); skips the lookup/refit path. */
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
        storeLayoutRevision++
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
            if (MERGE_UPLOADS) {
                // One copy per frame: every separate copy into the in-use arena costs a
                // graphics/copy-engine sync on the GPU timeline (50-75 of them showed
                // up as ~40 ms GPU waits on frames where many animated locs rebuilt).
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

        // Bind shader and set per-frame uniforms
        sh.bind()
        // Legacy back-face culls model geometry (OpenGlToolkit init:
        // glEnable(GL_CULL_FACE) + glCullFace(GL_BACK), never turned off for the
        // scene). Asserted HERE rather than in openSceneBatch because state set
        // there is cleared before the draws land. The projection flips Y, which
        // reverses winding, so front faces are CW.
        // Asserted for the same reason: a batch opened for an interface model (chat
        // head, equipment screen) is flushed after the 2D pass has already disabled
        // the depth test, and without it the model's faces land in submission order.
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
        // The count uniform is not read by the shader (the loop follows per-instance
        // indices), so the driver reports it as -1; gating the whole upload on it, as
        // this used to, meant the light arrays were NEVER uploaded and point lights
        // had no effect on models at all.
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
        // Retained block draws: one copy each, base recorded for the command patch.
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
        // Translucent items follow the opaque instances, one slot each, in order.
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
            // Packed in draw (sorted) order: slot = translucentBase + position in the
            // order, so a run of consecutive same-mesh items is one instanced command.
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
            // The instance buffer used to be a fixed 65536 and silently zeroed any
            // mesh that would overflow it. Grow instead: an instance that was
            // submitted is drawn.
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
        if (bindless) {
            drawMatCursor = 0
            beginMaterialPass()
        }
        if (indirectSupported && bindless) {
            // Commands from this frame's ACTIVE meshes only. Walking every resident
            // mesh's DrawRefs per material (thousands with instanceCount == 0) was 8%
            // of the spin frame. Sort by material id so the emission order matches
            // the per-material walk (-1 untextured first).
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
            // Block templates: copy, patch baseInstance, one material append per command.
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
            // Every material in one multi-draw: the commands carry their materials.
            if (indirectCommandCount > 0) {
                uploadDrawMaterials()
                sh.setUniform(uDrawBaseLocation, 0)
                glMultiDrawElementsIndirect(TRIANGLES, UNSIGNED_INT, 0L, indirectCommandCount, 0)
                instancedDrawCalls++
            }
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
                glMultiDrawElementsIndirect(TRIANGLES, UNSIGNED_INT, first.toLong() * INDIRECT_INTS * 4, commands, 0)
                instancedDrawCalls++
            } else {
                for (i in first until first + commands) {
                    val seq = (cmdKeys[i] and 0xffffffffL).toInt()
                    val mesh = cmdEntry[seq]!!
                    val matBatch = cmdBatch[seq]!!
                    val count = mesh.instanceCount
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

    /** Number of translucent items packed by the last [endFrame] and not yet drawn. */
    fun pendingTranslucent(): Int = translucentPacked - translucentCursor

    /**
     * Draws the translucent items packed by the last [endFrame], in submission order,
     * one instance per draw. Call after EVERY opaque pass of the frame has been
     * issued (this batcher's and the streaming batcher's), so blended faces land on
     * a complete opaque scene exactly as legacy's alpha bucket does.
     *
     * Depth writes follow each item's legacy setDepthBuffering state: stationary
     * alpha locs keep writing depth (legacy never turns it off for them — back-to-
     * front order makes that harmless), while the entity paths that draw with it off
     * (NPC hint models, player hints) get it off here too.
     */
    /** Diagnostics: translucent state groups (each = 1 GL draw) and commands since the last read. */
    @JvmField var translucentGroupsDrawn = 0L
    @JvmField var translucentCommandsDrawn = 0L

    fun drawTranslucent(
        textureCache: (Int) -> Int,
        combineModeOf: (Int) -> Int = { 0 },
        /**
         * Draw only the items that sort before the boundary ([upToKey], [upToSeq]) —
         * key strictly greater (farther), or equal key and lower sequence — leaving
         * the rest for a later call. Lets the toolkit interleave other translucent
         * passes (deferred particle systems) at their legacy position in the order.
         */
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
        // Whatever ran between passes (the particle renderer, the streaming batcher)
        // may have left blend/depth state its own way; this pass wants legacy's model
        // state: alpha blend, depth test, depth writes per item.
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
            // Order is fixed by the scene, but consecutive runs sharing a material (a
            // row of the same tree, the panes of one building) need no state change
            // between them, so they go out as ONE multi-draw. Commands for the whole
            // pass are built and uploaded once; each group is a byte range into them.
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
                    // Prebuilt commands: copy, patch the slot, copy the material words.
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
                // Consecutive items of the same mesh with the same depth-write state
                // are one command with instanceCount = run (instances draw in order,
                // so blending order is unchanged).
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
                    // Bindless: the material rides with the command, so only a depth-write
                    // change splits a group.
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
                        // A lone command costs less as a direct draw than as a one-entry
                        // indirect list (the driver's indirect path is a CPU loop anyway).
                        val c = gFirst[g] * INDIRECT_INTS
                        glDrawElementsInstancedBaseInstance(
                            TRIANGLES, buf[c], UNSIGNED_INT, buf[c + 2].toLong() * 4, buf[c + 1], buf[c + 4]
                        )
                    } else {
                        glMultiDrawElementsIndirect(
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
                // An entry re-appended after a shape mismatch later in this batch left its
                // old block behind; those index ranges may since have been compacted over.
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
                        glDrawElementsInstancedBaseInstance(
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
