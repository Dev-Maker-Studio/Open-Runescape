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
        /**
         * Vertex stride: position(12) + normal(12) + texcoord(8) + color(4) + 4 bytes
         * padding (the ModelCache 40-byte format's modelIndex slot, ignored by the
         * instanced VAO). Keeping the cache's native stride lets addInstance bulk-copy
         * cached vertex data with a single ByteBuffer.put instead of a per-byte
         * strip-copy loop.
         */
        const val VERTEX_STRIDE = 40

        /** Per-instance data: mat4 = 16 floats = 64 bytes */
        const val INSTANCE_STRIDE = 64

        /** Maximum instances per instanced draw call */
        private const val MAX_INSTANCES_PER_MESH = 1024

        /** Maximum unique meshes tracked per frame */
        private const val MAX_UNIQUE_MESHES = 256

        /** Maximum total vertices across all unique meshes */
        private const val MAX_TOTAL_VERTICES = 500000

        /** Maximum total indices across all unique meshes */
        private const val MAX_TOTAL_INDICES = 1000000

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
    private var uAlphaThresholdLocation = -1
    private var uViewProjectionLocation = -1
    private var uSunDirectionLocation = -1
    private var uSunBrightnessLocation = -1
    private var uSunColorLocation = -1
    private var uContrastPosLocation = -1
    private var uContrastNegLocation = -1

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
        var instanceData: FloatArray = FloatArray(16 * 16)
        var instanceCount: Int = 0

        /**
         * Presented-frame counter at the last [addInstance] touch. Drives LRU
         * eviction; entries carrying the CURRENT frame are never evicted, so
         * compaction can never pull geometry out from under this frame's draws.
         */
        var lastUsedFrame: Long = 0

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
            val base = instanceCount * 16
            if (base + 16 > instanceData.size) {
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
        uAlphaThresholdLocation = shader!!.getUniformLocation("uAlphaThreshold")
        uViewProjectionLocation = shader!!.getUniformLocation("uViewProjection")
        uSunDirectionLocation = shader!!.getUniformLocation("uSunDirection")
        uSunBrightnessLocation = shader!!.getUniformLocation("uSunBrightness")
        uSunColorLocation = shader!!.getUniformLocation("uSunColor")
        uContrastPosLocation = shader!!.getUniformLocation("uContrastPos")
        uContrastNegLocation = shader!!.getUniformLocation("uContrastNeg")
        shader!!.unbind()

        // Create GPU resources
        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.STREAM, IndexBuffer.IndexType.UNSIGNED_INT)
        instanceVbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)

        // Setup VAO with vertex format + instanced attributes
        setupVAO()

        // Reserve the GPU store at full size up front. uploadSubRange never grows a
        // buffer, and pre-allocating also means no glBufferData reallocation churn.
        vbo!!.allocate(MAX_TOTAL_VERTICES.toLong() * VERTEX_STRIDE)
        ibo!!.allocate(MAX_TOTAL_INDICES)

        // Allocate CPU staging buffers
        vertexBuffer = MemoryUtil.memAlloc(MAX_TOTAL_VERTICES * VERTEX_STRIDE)
        indexBuffer = MemoryUtil.memAllocInt(MAX_TOTAL_INDICES)
        instanceBuffer = MemoryUtil.memAllocFloat(MAX_INSTANCES_PER_MESH * 16)

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

            uniform mat4 uViewProjection;

            // Lighting uniforms
            uniform vec3 uSunDirection;
            uniform float uSunBrightness;
            uniform vec3 uSunColor;
            uniform float uContrastPos;
            uniform float uContrastNeg;

            out vec2 vTexCoord;
            out vec4 vColor;

            void main() {
                // Reconstruct model matrix from per-instance attributes
                mat4 modelMat = mat4(aModelMatCol0, aModelMatCol1, aModelMatCol2, aModelMatCol3);

                vec4 worldPos = modelMat * vec4(aPosition, 1.0);
                gl_Position = uViewProjection * worldPos;

                vTexCoord = aTexCoord;

                // GPU-side lighting calculation matching legacy formula exactly
                mat3 normalMat = mat3(modelMat);
                vec3 worldNormal = normalize(normalMat * aNormal);

                float dotProduct = dot(uSunDirection, worldNormal);
                float lightIntensity = uSunBrightness + dotProduct * (dotProduct < 0.0 ? uContrastNeg : uContrastPos);

                vec3 litRGB = aColor.rgb * uSunColor * lightIntensity;
                litRGB = clamp(litRGB, 0.0, 1.0);

                vColor = vec4(litRGB, aColor.a);
            }
        """.trimIndent()

        val fragmentSource = """
            #version 330 core

            in vec2 vTexCoord;
            in vec4 vColor;

            uniform sampler2D uTexture;
            uniform bool uTextureEnabled;
            uniform float uAlphaThreshold;
            // Legacy per-material texture combine mode (see OptimizedFrameBatcher /
            // groundTextured.frag). REPLACE(1) ignores the per-face vColor — required
            // or textured faces with junk face colors render tinted (green-quad bug).
            uniform int uCombineMode;

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
                } else {
                    color = vColor;
                }

                if (color.a <= uAlphaThreshold) {
                    discard;
                }

                fragColor = color;
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

    /**
     * Sets the lighting parameters for this frame.
     */
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

        // Geometry persists across frames — only reset the per-BATCH instance lists.
        for (entry in meshEntries.values) {
            entry.resetInstances()
        }
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
        modelMatrix: Matrix43
    ): Boolean {
        if (!isFrameActive) return false

        var entry = meshEntries[modelId]

        if (entry != null && entry.vertexCount != cachedModel.vertexCount) {
            // COLLISION GUARD: the persistent store must never serve another mesh's
            // geometry. If the incoming cached model disagrees with the stored entry
            // (hash collision or a stale entry after external invalidation), drop
            // just that entry and re-append below — no whole-store rebuild.
            println("[InstancedModelBatcher] HASH COLLISION/STALE ENTRY for key=$modelId (stored=${entry.vertexCount}v incoming=${cachedModel.vertexCount}v) — re-appending this mesh")
            meshEntries.remove(modelId)
            entry = null
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

            // Bulk copy vertex data: staging stride is the cache's native 40-byte
            // format (the 4-byte modelIndex slot is padding the VAO ignores), so a
            // single ByteBuffer.put replaces the old per-byte strip-copy loop.
            val vb = vertexBuffer!!
            val srcData = cachedModel.vertexData
            srcData.position(0)
            srcData.limit(vertexCount * VERTEX_STRIDE)
            vb.position(entry.vertexOffset * VERTEX_STRIDE)
            vb.put(srcData)
            srcData.clear()

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
        } else {
            instancedMeshesReused++
        }

        // Stamp the LRU clock: entries touched this presented frame are pinned
        // against eviction for the rest of it.
        entry.lastUsedFrame = frameCounter

        // Check instance limit
        if (entry.instanceCount >= MAX_INSTANCES_PER_MESH) return false

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
            freedVertices += candidate.vertexCount
            freedIndices += candidate.indexSpan()
        }

        // Cold entries alone may not reach the target; succeeding only requires
        // that the incoming mesh now fits.
        if (totalVertexCount - freedVertices + neededVertices > MAX_TOTAL_VERTICES ||
            totalIndexCount - freedIndices + neededIndices > MAX_TOTAL_INDICES
        ) {
            return false
        }

        if (evicted.isNotEmpty()) {
            meshEntries.keys.removeAll(evicted)
            meshesEvicted += evicted.size
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
            for (batch in entry.materialBatches) {
                val src = batch.indexOffset
                for (i in 0 until batch.indexCount) {
                    ib.put(writeIndex + i, ib.get(src + i) + vertexDelta)
                }
                batch.indexOffset = writeIndex
                writeIndex += batch.indexCount
            }
            entry.vertexOffset = writeVertex
            writeVertex += entry.vertexCount
        }

        totalVertexCount = writeVertex
        totalIndexCount = writeIndex
        // Every surviving mesh moved, so the GPU copy is stale end to end.
        uploadedVertexCount = 0
        uploadedIndexCount = 0
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
        needsBlendOf: (Int) -> Boolean = { false }
    ) {
        check(isFrameActive) { "No active frame" }

        // Render ALL meshes, whether they have 1 or many instances.
        // glDrawElementsInstanced with instanceCount=1 is equivalent to a regular draw.
        // This is simpler than routing single-instance meshes back to the regular batcher.
        val allEntries = meshEntries.values
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

        // Bind shader and set per-frame uniforms
        sh.bind()
        sh.setUniform(uTextureLocation, 0)
        sh.setUniform(uAlphaThresholdLocation, 0f)

        // Set VP matrix (already in column-major format for GLSL)
        glUniformMatrix4fv(uViewProjectionLocation, false, vp)

        // Set lighting uniforms
        sh.setUniform(uSunDirectionLocation, sunDirX, sunDirY, sunDirZ)
        sh.setUniform(uSunBrightnessLocation, sunBrightness)
        sh.setUniform(uSunColorLocation, sunColorR, sunColorG, sunColorB)
        sh.setUniform(uContrastPosLocation, contrastPos)
        sh.setUniform(uContrastNegLocation, contrastNeg)

        // Bind VAO (has both vertex and instance attribute pointers configured)
        vao!!.bind()
        ibo!!.bind()

        var lastTextureHandle = -1
        // -1 = unknown, forces the first material batch to establish the state.
        var lastBlend = -1

        // Render each mesh (instanced or not)
        for (entry in allEntries) {
            val count = entry.instanceCount
            if (count == 0) continue

            // Upload instance matrices for this mesh (bulk copy from the flat array)
            val t1 = System.nanoTime()
            val instBuf = instanceBuffer!!
            instBuf.clear()
            instBuf.put(entry.instanceData, 0, count * 16)
            instBuf.flip()
            instanceVbo!!.upload(instBuf)
            timeInstanceUpload += System.nanoTime() - t1

            // Render each material batch with instancing
            for (matBatch in entry.materialBatches) {
                // -1 = untextured; 0..N = textured (material 0 is valid — see
                // buildModelCache). Test `>= 0`, not `> 0`.
                val texId = matBatch.materialId
                val textureHandle = if (texId >= 0) textureCache(texId) else 0

                if (textureHandle != lastTextureHandle) {
                    if (textureHandle > 0) {
                        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                        GLWrapper.bindTexture(TEXTURE_2D, textureHandle)
                        sh.setUniform(uTextureEnabledLocation, 1)
                        // Legacy per-material combine mode (REPLACE ignores vColor).
                        sh.setUniform(uCombineModeLocation, if (texId >= 0) combineModeOf(texId) else 0)
                    } else {
                        sh.setUniform(uTextureEnabledLocation, 0)
                    }
                    lastTextureHandle = textureHandle
                }

                // Per-material blend state (legacy setBlendMode parity). Opaque
                // materials MUST draw with GL_BLEND off, or their alpha-carrying
                // faces go translucent and depth-reject what is behind them.
                val blend = if (needsBlendOf(texId)) 1 else 0
                if (blend != lastBlend) {
                    if (blend == 1) {
                        GLWrapper.enable(GLWrapper.Constants.BLEND)
                        GLWrapper.blendFunc(
                            GLWrapper.Constants.SRC_ALPHA,
                            GLWrapper.Constants.ONE_MINUS_SRC_ALPHA
                        )
                    } else {
                        GLWrapper.disable(GLWrapper.Constants.BLEND)
                    }
                    lastBlend = blend
                }

                val t2 = System.nanoTime()
                GLWrapper.drawElementsInstanced(
                    TRIANGLES,
                    matBatch.indexCount,
                    UNSIGNED_INT,
                    matBatch.indexOffset.toLong() * 4,
                    count
                )
                timeDraw += System.nanoTime() - t2

                instancedDrawCalls++
                instancedTriangles += (matBatch.indexCount / 3) * count
            }
        }

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
        val geomUploadMs: Float = 0f,
        val instanceUploadMs: Float = 0f,
        val drawMs: Float = 0f
    )
}
