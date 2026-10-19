package com.jagex.game.runetek5.graphics.model

import com.jagex.game.runetek5.buffer.ArrayBufferGL
import com.jagex.game.runetek5.buffer.ElementsBufferGL
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.entity.BoundingBoxPolygon
import com.jagex.game.runetek5.graphics.model.billboard.Billboard
import com.jagex.game.runetek5.graphics.model.billboard.BillboardSurfaceGL
import com.jagex.game.runetek5.graphics.model.billboard.BillboardEffectGL
import com.jagex.game.runetek5.graphics.model.billboard.BillboardType
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList
import com.jagex.game.runetek5.graphics.model.buffer.ModelElementsBufferGL
import com.jagex.game.runetek5.graphics.model.normal.FaceNormalGL
import com.jagex.game.runetek5.graphics.model.normal.ModelNormalsGL
import com.jagex.game.runetek5.graphics.model.normal.VertexNormalGL
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle
import com.jagex.game.runetek5.graphics.modern.util.ModernModelBuilder
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.graphics.shadow.ShadowGL
import com.jagex.game.runetek5.graphics.texture.TextureTransformationMatrix
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.toolkit.ModernGLToolkit
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer
import com.jagex.game.runetek5.toolkit.opengl.*
import com.jagex.game.runetek5.utils.ArrayUtils
import com.jagex.game.runetek5.utils.ColorUtil
import com.jagex.game.runetek5.utils.MathUtils
import jaclib.memory.Stream
import jaggl.OpenGL
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

class ModelGL : Model {

    /**
     * Drops the mesh-built geometry because a mutation changed this model beyond what
     * the per-instance matrix can express, and records WHY. The reason histogram
     * (-Dmodel.invalidatediag) shows which mutations force the legacy-capture path, and
     * therefore which ones are worth expressing as a matrix instead of a vertex bake.
     */
    private fun invalidateModernBuilt(reason: Int) {
        if (INVALIDATE_DIAG) invalidateCounts[reason]++
        // Colour-only mutations keep the geometry: a tint/recolour changes no position,
        // normal or UV, so only the 4 colour bytes per vertex are rebuilt
        // (ModernModelBuilder.recolour) and the cache re-keys on the colour state.
        //
        // This needs meshFaceIndices to bridge index spaces -- the builder is indexed
        // by MESH face, while ModelGL rebuilds facesColour over its own filtered,
        // sorted face list. Reading the raw array with a mesh index picked up the
        // wrong material's brightness and saturated recoloured models to white.
        // -Drender.verifyrecolour asserts recolour reproduces the emission bytes
        // exactly; it reports 0 mismatches.
        // DISABLED. Colour-only mutations look like they could keep the geometry and
        // rebuild just the colour bytes, but the recolour path is not proven for the
        // case that matters: -Drender.verifyrecolour only compares models with
        // modernColorRevision == 0, i.e. models that never took a colour mutation --
        // exactly the ones this branch would NOT affect. It reported 0 mismatches while
        // recoloured bushes still rendered white. Re-enable only once the check covers
        // MUTATED models.
        // Colour-only reasons still invalidate (rendering unchanged), but record the
        // revision and keep the built geometry aside so the recolour path can be
        // verified against the currently-correct output on MUTATED models -- the case
        // the previous check never covered.
        if (reason == 4 || reason == 6 || reason == 7 || reason == 8) {
            this.modernColorRevision++
            if (VERIFY_RECOLOUR_CAPTURE && this.modernBuiltShadow == null) {
                this.modernBuiltShadow = this.modernBuilt
            }
        }
        this.modernBuilt = null
    }

    companion object {
        @JvmStatic val INVALIDATE_DIAG = System.getProperty("model.invalidatediag") != null
        @JvmStatic val VERIFY_RECOLOUR_CAPTURE = System.getProperty("render.verifyrecolour") != null
        @JvmStatic val INVALIDATE_REASONS = arrayOf("yaxisRotate", "translate", "mirror", "scale", "recolor", "rematerial", "tint", "setAmbient", "envAlpha", "offsetSubmesh", "xaxisRotate", "zaxisRotate", "contourToGround", "hillChange", "normalMerge")
        @JvmStatic val invalidateCounts = LongArray(16)

        /** How often [calculateBounds] actually recomputes, and over how many vertices. */
@JvmStatic var boundsCalls = 0L
        @JvmStatic var boundsVertices = 0L
        @JvmStatic fun invalidateSummary(): String =
            INVALIDATE_REASONS.indices.joinToString(" ") { "${INVALIDATE_REASONS[it]}=${invalidateCounts[it]}" }
        // Picking scratch (calculateProjection) — game thread only. Construction-time
        // scratch must NOT live here: models are built concurrently by the game thread
        // (NPC/player/item models) and the async map loader (loc models).
        @JvmField
        val matrixX = IntArray(8)

        @JvmField
        val matrixY = IntArray(8)

        @JvmField
        val matrixZ = IntArray(8)

        @JvmField
        var originX: Int = 0

        @JvmField
        var originY: Int = 0

        @JvmField
        var originZ: Int = 0

        @JvmField
        var withinOrigin: Boolean = false

        /** Source of unique face-content stamps; see [facesRevision]. */
        private val FACES_REVISION_COUNTER = AtomicLong(1L)

        /**
         * XORed into [facesRevision] by [mirror]. Mirroring swaps facesB/facesC, which is
         * its own inverse, and XOR mirrors that: applying the salt twice restores the
         * original stamp. The high bits guarantee a mirrored stamp can never collide with
         * an unmirrored counter value.
         */
        private const val MIRRORED_FACES_REVISION_SALT = 0x517cc1b727220a95L

        @JvmStatic
        fun clone(src: FloatArray, size: Int): FloatArray {
            val dst = FloatArray(size)
            System.arraycopy(src, 0, dst, 0, size)
            return dst
        }

        @JvmStatic
        fun clone(src: ShortArray, size: Int): ShortArray {
            val dst = ShortArray(size)
            System.arraycopy(src, 0, dst, 0, size)
            return dst
        }

        @JvmStatic
        fun clone(src: ByteArray, size: Int): ByteArray {
            val dst = ByteArray(size)
            System.arraycopy(src, 0, dst, 0, size)
            return dst
        }

        @JvmStatic
        fun adjustLuminance(hsl: Int, luminanceFactor: Int): Int {
            var adjustedLuminance = luminanceFactor
            adjustedLuminance = (hsl and 0x7f) * adjustedLuminance shr 7
            if (adjustedLuminance < 2) {
                adjustedLuminance = 2
            } else if (adjustedLuminance > 126) {
                adjustedLuminance = 126
            }
            return (hsl and 0xff80) + adjustedLuminance
        }
    }

    /**
     * Normal-dedup scratch used only while constructing from a Mesh; nulled when
     * construction completes. Instance-level (not static) because model construction
     * runs concurrently on the game thread and the async map loader thread.
     */
    private var normalsUid: LongArray? = null

    @JvmField
    var boundsCalculated: Boolean = false

    @JvmField
    var hasAlphaTransition: Boolean = false

    @JvmField
    var textureAnimating: Boolean = false

    @JvmField
    var numVertices: Int = 0

    @JvmField
    var maxDepth: Int = 0

    @JvmField
    var numNormals: Int = 0

    @JvmField
    var numFaces: Int = 0

    @JvmField
    var renderedFaceCount: Int = 0

    @JvmField
    var updateRequired: Boolean = true

    @JvmField
    val toolkit: OpenGlToolkit

    @JvmField
    var functionMask: Int = 0

    /**
     * True when this model is a per-frame ANIMATED POSE snapshot (produced by
     * copyModelData with a vertex-altering mask, i.e. a sequence-augmented mask).
     *
     * Captured at COPY time and never cleared by applyShading, because the animated
     * pose paths (NPCType.getAnimatedBodymodel/head, SpotAnimType, ItemType) call
     * applyShading(<base caller mask>) AFTER animating — that overwrites functionMask
     * (dropping ALTERS_VERTICES) and nulls vertexGroups (releaseUploadedData), which
     * destroys BOTH signals the modern renderer's cache gate relied on. Every such
     * pose has different vertices, so if wrongly admitted to the content cache /
     * persistent instanced store each pose becomes a unique mesh — flooding the store,
     * forcing capacity-overflow rebuilds, and serving substituted geometry (the
     * green-quad impling/signpost bug). The modern gate reads this flag so these
     * snapshots always stream fresh and are never content-cached.
     */
    @JvmField
    var animatedPoseSnapshot: Boolean = false

    /**
     * Modern-renderer geometry built from the CLEAN decoded mesh at createModel
     * time (direct vertex indices, proper texture-projection UVs). Shared across
     * copies (immutable). Nulled by any vertex/color mutation the modern path
     * cannot represent in the per-instance model matrix (translate, mirror,
     * scale, recolor, rematerial, tint, ambient change, non-yaw rotation,
     * ground contouring) — the modern renderer then falls back to capturing the
     * legacy arrays. Yaw rotations are representable: they accumulate in
     * [modernYaw] and are applied via the model matrix instead of re-baking.
     */
    @JvmField
    var modernBuilt: ModernModelBuilder.BuiltModel? = null

    /**
     * Accumulated Y-axis rotation (0..16383, legacy angle units) baked into this
     * model's LEGACY vertices by rotateWithNormals. The modern path's base
     * geometry ([modernBuilt]) is unrotated, so the modern renderer pre-multiplies
     * this yaw into the per-instance model matrix.
     */
    @JvmField
    var modernYaw: Int = 0

    /** model-face index -> source mesh face index, after filtering and sorting. */
    @JvmField
    var meshFaceIndices: IntArray? = null

    /** Built geometry kept past a colour mutation, for verification only. */
    @JvmField
    var modernBuiltShadow: ModernModelBuilder.BuiltModel? = null

    /** Bumped by colour-only mutations; geometry stays valid (see invalidateModernBuilt). */
    @JvmField
    var modernColorRevision: Int = 0

    /**
     * Memoised modern-renderer content hash, and the geometry the modern path
     * built from THESE vertices.
     *
     * Both are keyed by [modernContentValid], which every method that writes to a
     * geometry array clears (see invalidateModernContent and its call sites — the
     * set was derived mechanically from every assignment into verticesX/Y/Z,
     * faces*, normals*, texture*, triangles, vertexNormalIndices and the counts).
     *
     * Rationale: the modern renderer used to recompute an O(vertices + faces)
     * rolling hash for EVERY model EVERY frame (~12% of a logged-in frame at 1400
     * fallback-path models), and re-run the whole per-face build for every animated
     * model every frame (~22%). Neither input changes between render frames — game
     * logic mutates models at 50Hz while the render loop is uncapped — so both are
     * pure repeated work. Memoising them makes a render frame that follows another
     * render frame with no intervening logic tick nearly free on the CPU.
     *
     * `-Drender.verifyhash` recomputes the hash every frame and reports any
     * disagreement with the memoised value, which is the direct test that the
     * invalidation set above is complete.
     */
    @JvmField
    var modernContentValid: Boolean = false

    /** Valid only while [modernContentValid]. */
    @JvmField
    var modernContentHash: Long = 0

    /**
     * Per-instance geometry built from the CURRENT (post-animation) vertices,
     * owned by this model. Valid only while [modernContentValid]. Typed as [Any]
     * to keep ModelGL free of a dependency on the modern batch package; the modern
     * renderer is the only reader and casts it back.
     */
    @JvmField
    var modernStreamGeometry: Any? = null

    /** Valid only while true; cleared alongside [modernContentValid]. */
    @JvmField
    var modernStreamValid: Boolean = false

    /**
     * The shared-cache entry this model resolved to last time, and the key it was
     * resolved under. Lets the modern renderer skip the cache's hash-map lookup
     * (an access-ordered LinkedHashMap keyed by a boxed Long, which restructures
     * on every get) for a model whose geometry has not changed.
     *
     * The entry can still be evicted and freed underneath us, so the fast path also
     * checks the entry's own eviction flag; a miss there just falls back to the map.
     */
    @JvmField
    var modernSharedGeometry: Any? = null

    @JvmField
    var modernSharedKey: Long = 0

    @JvmField
    var modernSharedValid: Boolean = false

    /**
     * Stable identity for this model's per-pose geometry in the instanced store,
     * assigned once and never reused.
     *
     * Animated models cannot be keyed by content — every pose differs — but they can
     * be keyed by INSTANCE, which is what lets their geometry live in the persistent
     * store and be rewritten in place when the pose changes, instead of being copied
     * into a streaming buffer on every render frame.
     */
    @JvmField
    var modernStreamKey: Long = 0
    /** Entity whose pose memo should receive the geometry the toolkit builds next (see PoseMemoOwner). */
    @JvmField var modernPoseMemoOwner: Any? = null

    /**
     * Set on copies owned by a PoseCopyPair: nobody but the owner writes this object
     * between the scene walk and the frame flush, so its geometry may be built off
     * the render thread. Shared toolkit copy slots are overwritten by the next entity.
     */
    @JvmField var privatePoseCopy: Boolean = false
    /** Deferred-build bookkeeping (ModernGLToolkit.enqueueDeferredBuild). */
    @JvmField var modernDeferredStamp: Int = -1
    @JvmField var modernDeferredIndex: Int = 0


    /**
     * Last point-light selection made for this model, and the world origin and light
     * epoch it was made at.
     *
     * Selecting the four strongest lights for a model is a scan over every light
     * whose influence reaches its cell. A static loc sits at the same place frame
     * after frame with the same lights around it, so the answer is the same every
     * time — this makes the repeat a four-float comparison. The epoch invalidates
     * every memo at once when the frame's light set is rebuilt.
     */
    @JvmField
    var modernLightEpoch: Int = -1

    @JvmField
    var modernLightX: Float = 0f

    @JvmField
    var modernLightY: Float = 0f

    @JvmField
    var modernLightZ: Float = 0f

    /** Four light indices as floats, matching the per-instance attribute layout. */
    @JvmField
    val modernLightIdx: FloatArray = FloatArray(4)

    /**
     * Clears everything memoised from this model's geometry arrays. Called at the
     * head of every mutating method; idempotent and cheap enough to be free on the
     * paths that call it thousands of times per tick.
     *
     * The stream geometry BUFFER is deliberately kept (only its validity is
     * dropped) so the next rebuild can write into it instead of allocating.
     */
    fun invalidateModernContent() {
        modernContentValid = false
        modernStreamValid = false
        modernSharedValid = false
    }

    @JvmField
    var preferenceFlags: Int = 0

    @JvmField
    var vertexBuffer: OpenGlArrayDrawBuffer? = null

    @JvmField
    var textureCoordinatesBuffer: OpenGlArrayDrawBuffer? = null

    @JvmField
    var normalsBuffer: OpenGlArrayDrawBuffer? = null

    @JvmField
    var colorsBuffer: OpenGlArrayDrawBuffer? = null

    @JvmField
    var modelElementsBuffer: ModelElementsBufferGL? = null

    @JvmField
    var triangles: IntArray? = null

    @JvmField
    var verticesX: IntArray? = null

    @JvmField
    var verticesY: IntArray? = null

    @JvmField
    var verticesZ: IntArray? = null

    @JvmField
    var verticesSubmeshes: ShortArray? = null

    @JvmField
    var emissiveTriangles: Array<EmissiveTriangle>? = null

    @JvmField
    var effectiveVertices: Array<EffectiveVertex>? = null

    @JvmField
    var billboardCount: Int = 0

    @JvmField
    var billboardEffects: Array<BillboardEffectGL?>? = null

    @JvmField
    var billboardSurfaces: Array<BillboardSurfaceGL?>? = null

    @JvmField
    var normalsX: ShortArray? = null

    @JvmField
    var normalsY: ShortArray? = null

    @JvmField
    var normalsZ: ShortArray? = null

    @JvmField
    var magnitudes: ByteArray? = null

    @JvmField
    var textureU: FloatArray? = null

    @JvmField
    var textureV: FloatArray? = null

    @JvmField
    var facesColor: ShortArray? = null

    @JvmField
    var facesAlpha: ByteArray? = null

    @JvmField
    var facesA: ShortArray? = null

    @JvmField
    var facesB: ShortArray? = null

    @JvmField
    var facesC: ShortArray? = null

    @JvmField
    var facesMaterial: ShortArray? = null

    @JvmField
    var facesSubmeshes: ShortArray? = null

    @JvmField
    var ambient: Short = 0

    @JvmField
    var contrast: Short = 0

    @JvmField
    var vertexNormalIndices: ShortArray? = null

    @JvmField
    var materialBatchOffsets: IntArray? = null

    @JvmField
    var vertexGroups: Array<IntArray?>? = null

    @JvmField
    var billboardGroups: Array<IntArray?>? = null

    @JvmField
    var faceGroups: Array<IntArray?>? = null

    @JvmField
    var mergedNormals: ModelNormalsGL? = null

    @JvmField
    var renderState: Byte = 0

    @JvmField
    var size3d: Int = 0

    @JvmField
    var size2d: Int = 0

    @JvmField
    var minX: Short = 0

    @JvmField
    var maxX: Short = 0

    @JvmField
    var minY: Short = 0

    @JvmField
    var maxY: Short = 0

    @JvmField
    var minZ: Short = 0

    @JvmField
    var maxZ: Short = 0

    @JvmField
    var dynamicElementsBuffer: ElementsBufferGL? = null

    @JvmField
    var dynamicArrayBuffer: ArrayBufferGL? = null

    /**
     * Content stamp for facesA/facesB/facesC. Every freshly built model gets a unique
     * stamp; copies inherit the source's stamp (identical index content) and [mirror]
     * XORs a salt (mirroring twice restores both the windings and the stamp). Used to
     * skip rebuilding and re-uploading [dynamicElementsBuffer] when it already holds
     * exactly this model's indices — animation only moves vertices, so the index data
     * of an animated model is identical from frame to frame.
     */
    private var facesRevision: Long = FACES_REVISION_COUNTER.getAndIncrement()

    /** Faces stamp and face count [dynamicElementsBuffer] was last built from. */
    private var dynamicElementsRevision: Long = -1L
    private var dynamicElementsFaceCount: Int = -1

    /** Toolkit context generation the buffer's upload belongs to. */
    private var dynamicElementsGeneration: Int = -1

    /**
     * Reusable 3x3 fixed-point matrix scratch for [animateTransform]. Skeletal animation
     * previously allocated these per bone group per entity per render frame. Instance
     * fields (not companion scratch) because models are built and animated concurrently
     * on the game thread and the async map loader thread; per-instance reuse is exactly
     * as thread-safe as the vertex arrays the same code writes.
     */
    private var animScratchMatrixA: IntArray? = null
    private var animScratchMatrixB: IntArray? = null
    private var animScratchMatrixC: IntArray? = null

    /**
     * A copy slot must always own the draw-buffer objects the preallocated copy path
     * writes into; an earlier copy with a different mask can have pointed a slot's
     * field at the base's (possibly null) buffer. Restores the constructor's set.
     */
    fun ensureSlotBuffers() {
        if (this.vertexBuffer == null) this.vertexBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0)
        if (this.textureCoordinatesBuffer == null) this.textureCoordinatesBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 2, 0)
        if (this.normalsBuffer == null) this.normalsBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0)
        if (this.colorsBuffer == null) this.colorsBuffer = OpenGlArrayDrawBuffer(null, GL_UNSIGNED_BYTE, 4, 0)
        if (this.modelElementsBuffer == null) this.modelElementsBuffer = ModelElementsBufferGL()
    }

    constructor(toolkit: OpenGlToolkit) {
        this.toolkit = toolkit
        this.vertexBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0)
        this.textureCoordinatesBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 2, 0)
        this.normalsBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0)
        this.colorsBuffer = OpenGlArrayDrawBuffer(null, GL_UNSIGNED_BYTE, 4, 0)
        this.modelElementsBuffer = ModelElementsBufferGL()
    }

    constructor(renderer: OpenGlToolkit, mesh: Mesh, rasterizerFlags: Int, ambient: Int, contrast: Int, otherFlags: Int) {
        this.toolkit = renderer
        this.functionMask = rasterizerFlags
        this.preferenceFlags = otherFlags

        if (ModelFunctionMaskGL.hasVertices(rasterizerFlags)) {
            this.vertexBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0)
        }

        if (ModelFunctionMaskGL.hasTextureCoordinates(rasterizerFlags)) {
            this.textureCoordinatesBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 2, 0)
        }

        if (ModelFunctionMaskGL.hasNormals(rasterizerFlags, otherFlags)) {
            this.normalsBuffer = OpenGlArrayDrawBuffer(null, GL_FLOAT, 3, 0)
        }

        if (ModelFunctionMaskGL.hasColors(rasterizerFlags)) {
            this.colorsBuffer = OpenGlArrayDrawBuffer(null, GL_UNSIGNED_BYTE, 4, 0)
        }

        if (ModelFunctionMaskGL.hasElementsBuffer(rasterizerFlags)) {
            this.modelElementsBuffer = ModelElementsBufferGL()
        }

        val materialProvider: MaterialProvider = renderer.materialProvider!!
        val faces = IntArray(mesh.numFaces)
        this.triangles = IntArray(mesh.maxDepth + 1)

        for (faceIndex in 0 until mesh.numFaces) {
            if (mesh.faceRenderType == null || mesh.faceRenderType!![faceIndex].toInt() != 2) {
                if (mesh.facesMaterial != null && mesh.facesMaterial!![faceIndex].toInt() != -1) {
                    val meshMaterial = materialProvider.getMaterial(mesh.facesMaterial!![faceIndex].toInt() and 0xffff)
                    if (((this.preferenceFlags and 0x40) == 0 || !meshMaterial.detailsOnly) && meshMaterial.skipTriangles) {
                        continue
                    }
                }

                faces[this.numFaces++] = faceIndex
                this.triangles!![mesh.facesA!![faceIndex].toInt()]++
                this.triangles!![mesh.facesB!![faceIndex].toInt()]++
                this.triangles!![mesh.facesC!![faceIndex].toInt()]++
            }
        }

        this.renderedFaceCount = this.numFaces
        val billboardUids = LongArray(this.numFaces)
        val hasTransitions = (this.functionMask and 0x100) != 0

        var currentFace: Int
        var uidHigh: Int
        var material: MaterialType
        var faceMaterialId: Short
        var uidLow: Int

        for (triangleIndex in 0 until this.numFaces) {
            currentFace = faces[triangleIndex]
            var faceMaterial: MaterialType? = null
            uidHigh = 0
            var renderFlags: Byte = 0
            var effectId: Byte = 0
            var effectParam1: Byte = 0

            if (mesh.billboards != null) {
                var hasUid = false

                for (billboardIndex in mesh.billboards!!.indices) {
                    val billboard = mesh.billboards!![billboardIndex]
                    if (currentFace == billboard.appliesOnFace) {
                        val billboardType = BillboardTypeList.list(billboard.billboardId)
                        if (billboardType.hasUid) {
                            hasUid = true
                        }

                        if (billboardType.materialId != -1) {
                            material = materialProvider.getMaterial(billboardType.materialId)
                            if (material.effectCombiner.toInt() == 2) {
                                this.hasAlphaTransition = true
                            }
                        }
                    }
                }

                if (hasUid) {
                    billboardUids[triangleIndex] = Long.MAX_VALUE
                    this.renderedFaceCount--
                    continue
                }
            }

            faceMaterialId = -1

            if (mesh.facesMaterial != null) {
                faceMaterialId = mesh.facesMaterial!![currentFace]
                if (faceMaterialId.toInt() != -1) {
                    faceMaterial = materialProvider.getMaterial(faceMaterialId.toInt() and 0xffff)
                    if ((this.preferenceFlags and 0x40) != 0 && faceMaterial.detailsOnly) {
                        faceMaterialId = -1
                        faceMaterial = null
                    } else {
                        effectId = faceMaterial.effectId
                        effectParam1 = faceMaterial.effectParam1
                        if (faceMaterial.textureSpeedU.toInt() != 0 || faceMaterial.textureSpeedV.toInt() != 0) {
                            this.textureAnimating = true
                        }
                    }
                }
            }

            val alphaTransition = mesh.facesAlpha != null && mesh.facesAlpha!![currentFace].toInt() != 0 || faceMaterial != null && faceMaterial.effectCombiner.toInt() != 0
            if ((hasTransitions || alphaTransition) && mesh.facesLayer != null) {
                uidHigh += mesh.facesLayer!![currentFace].toInt() shl 17
            }

            if (alphaTransition) {
                uidHigh += 65536
            }

            uidHigh += (effectId.toInt() and 0xff) shl 8
            uidHigh += effectParam1.toInt() and 0xff
            uidLow = renderFlags + ((faceMaterialId.toInt() and 0xffff) shl 16)
            uidLow += triangleIndex and 0xffff
            billboardUids[triangleIndex] = (uidHigh.toLong() shl 32) + uidLow
            this.hasAlphaTransition = this.hasAlphaTransition or alphaTransition
            this.textureAnimating = this.textureAnimating or (faceMaterial != null && (faceMaterial.textureSpeedU.toInt() != 0 || faceMaterial.textureSpeedV.toInt() != 0))
        }

        Model.addBillboard(billboardUids, faces)
        // Retain model-face -> MESH-face after the sort. ModelGL rebuilds facesColor /
        // facesAlpha / facesMaterial over this filtered, sorted list, so anything that
        // starts from a mesh face index (ModernModelBuilder) needs this to reach the
        // model's per-face arrays. Without it the two index spaces silently disagree.
        this.meshFaceIndices = faces.copyOf(this.numFaces)
        this.numVertices = mesh.numVertices
        this.maxDepth = mesh.maxDepth
        this.verticesX = mesh.verticesX
        this.verticesY = mesh.verticesY
        this.verticesZ = mesh.verticesZ
        this.verticesSubmeshes = mesh.verticesSubmeshes
        val vertexNormals = arrayOfNulls<VertexNormalGL>(this.maxDepth)
        this.emissiveTriangles = mesh.particleEmitters
        this.effectiveVertices = mesh.particleEffectors

        if (mesh.billboards != null) {
            this.billboardCount = mesh.billboards!!.size
            this.billboardEffects = arrayOfNulls(this.billboardCount)
            this.billboardSurfaces = arrayOfNulls(this.billboardCount)

            var billboardIndex = 0
            while (billboardIndex < this.billboardCount) {
                val billboard = mesh.billboards!![billboardIndex]
                val billboardType = BillboardTypeList.list(billboard.billboardId)
                var effectFaceIndex = -1

                var faceIndex = 0
                while (faceIndex < this.numFaces) {
                    if (billboard.appliesOnFace == faces[faceIndex]) {
                        effectFaceIndex = faceIndex
                        break
                    }
                    faceIndex++
                }

                if (effectFaceIndex == -1) {
                    throw RuntimeException()
                }

                var diffuseColor = ColorUtil.hslToRgb!![mesh.facesColor!![billboard.appliesOnFace].toInt() and 0xffff] and 0xffffff
                diffuseColor = diffuseColor or ((255 - (if (mesh.facesAlpha != null) mesh.facesAlpha!![billboard.appliesOnFace].toInt() else 0)) shl 24)
                this.billboardEffects!![billboardIndex] = BillboardEffectGL(
                    effectFaceIndex,
                    mesh.facesA!![billboard.appliesOnFace].toInt(),
                    mesh.facesB!![billboard.appliesOnFace].toInt(),
                    mesh.facesC!![billboard.appliesOnFace].toInt(),
                    billboardType.size2d,
                    billboardType.size3d,
                    billboardType.materialId,
                    billboardType.shape,
                    billboardType.stationary,
                    billboard.distance
                )
                this.billboardSurfaces!![billboardIndex] = BillboardSurfaceGL(diffuseColor)
                billboardIndex++
            }
        }

        val normalCapacity = this.numFaces * 3
        this.normalsX = ShortArray(normalCapacity)
        this.normalsY = ShortArray(normalCapacity)
        this.normalsZ = ShortArray(normalCapacity)
        this.magnitudes = ByteArray(normalCapacity)
        this.textureU = FloatArray(normalCapacity)
        this.textureV = FloatArray(normalCapacity)
        this.facesColor = ShortArray(this.numFaces)
        this.facesAlpha = ByteArray(this.numFaces)
        this.facesA = ShortArray(this.numFaces)
        this.facesB = ShortArray(this.numFaces)
        this.facesC = ShortArray(this.numFaces)
        this.facesMaterial = ShortArray(this.numFaces)

        if (mesh.facesSubmeshes != null) {
            this.facesSubmeshes = ShortArray(this.numFaces)
        }

        this.ambient = ambient.toShort()
        this.contrast = contrast.toShort()
        this.vertexNormalIndices = ShortArray(normalCapacity)
        normalsUid = LongArray(normalCapacity)
        var normalSlotOffset = 0

        var vertexIndex = 0
        while (vertexIndex < mesh.maxDepth) {
            val triangleCount = this.triangles!![vertexIndex]
            this.triangles!![vertexIndex] = normalSlotOffset
            normalSlotOffset += triangleCount
            vertexNormals[vertexIndex] = VertexNormalGL()
            vertexIndex++
        }

        this.triangles!![mesh.maxDepth] = normalSlotOffset
        val transformMatrix = generateTextureMatrix(mesh, faces, this.numFaces)
        val faceNormals = arrayOfNulls<FaceNormalGL>(mesh.numFaces)
        // UV scratch is local: constructions run concurrently across threads.
        val uv = FloatArray(2)

        for (faceTriangleIndex in 0 until mesh.numFaces) {
            val faceA = mesh.facesA!![faceTriangleIndex]
            val faceB = mesh.facesB!![faceTriangleIndex]
            val faceC = mesh.facesC!![faceTriangleIndex]
            val deltaX1 = this.verticesX!![faceB.toInt()] - this.verticesX!![faceA.toInt()]
            val deltaY1 = this.verticesY!![faceB.toInt()] - this.verticesY!![faceA.toInt()]
            var deltaZ1 = this.verticesZ!![faceB.toInt()] - this.verticesZ!![faceA.toInt()]
            var deltaX2 = this.verticesX!![faceC.toInt()] - this.verticesX!![faceA.toInt()]
            var deltaY2 = this.verticesY!![faceC.toInt()] - this.verticesY!![faceA.toInt()]
            var deltaZ2 = this.verticesZ!![faceC.toInt()] - this.verticesZ!![faceA.toInt()]
            var x = deltaY1 * deltaZ2 - deltaY2 * deltaZ1
            var y = deltaZ1 * deltaX2 - deltaZ2 * deltaX1

            var z = deltaX1 * deltaY2 - deltaX2 * deltaY1
            while (x > 8192 || y > 8192 || z > 8192 || x < -8192 || y < -8192 || z < -8192) {
                x = x shr 1
                y = y shr 1
                z = z shr 1
            }

            var normalMagnitude = sqrt((x * x + y * y + z * z).toDouble()).toInt()
            if (normalMagnitude <= 0) {
                normalMagnitude = 1
            }

            x = x * 256 / normalMagnitude
            y = y * 256 / normalMagnitude
            z = z * 256 / normalMagnitude

            val faceRenderType: Byte = if (mesh.faceRenderType == null) 0 else mesh.faceRenderType!![faceTriangleIndex]
            if (faceRenderType.toInt() == 0) {
                var vertexNormal = vertexNormals[faceA.toInt()]!!
                vertexNormal.x += x
                vertexNormal.y += y
                vertexNormal.z += z
                vertexNormal.magnitude++
                vertexNormal = vertexNormals[faceB.toInt()]!!
                vertexNormal.x += x
                vertexNormal.y += y
                vertexNormal.z += z
                vertexNormal.magnitude++
                vertexNormal = vertexNormals[faceC.toInt()]!!
                vertexNormal.x += x
                vertexNormal.y += y
                vertexNormal.z += z
                vertexNormal.magnitude++
            } else if (faceRenderType.toInt() == 1) {
                val faceNormal = FaceNormalGL()
                faceNormals[faceTriangleIndex] = faceNormal
                faceNormal.x = x
                faceNormal.y = y
                faceNormal.z = z
            }
        }

        for (faceTriangleIndex in 0 until this.numFaces) {
            val texturedFace: Int
            val faceIndex = faces[faceTriangleIndex]
            val color = mesh.facesColor!![faceIndex].toInt() and 0xffff
            texturedFace = if (mesh.facesTexture == null) {
                -1
            } else {
                mesh.facesTexture!![faceIndex].toInt()
            }

            val alphaAmt = if (mesh.facesAlpha == null) {
                0
            } else {
                mesh.facesAlpha!![faceIndex].toInt() and 0xff
            }

            var materialId = if (mesh.facesMaterial == null) (-1).toShort() else mesh.facesMaterial!![faceIndex]
            if (materialId.toInt() != -1 && (this.preferenceFlags and 0x40) != 0) {
                material = materialProvider.getMaterial(materialId.toInt() and 0xffff)
                if (material.detailsOnly) {
                    materialId = -1
                }
            }

            var aU = 0.0f
            var aV = 0.0f
            var bU = 0.0f
            var bV = 0.0f
            var cU = 0.0f
            var cV = 0.0f
            var uidOffsetB: Byte = 0
            var uidOffsetC: Byte = 0
            var cubeFaceIndex = 0
            var renderType: Byte

            if (materialId.toInt() != -1) {
                if (texturedFace == -1) {
                    aU = 0.0f
                    aV = 1.0f
                    bU = 1.0f
                    bV = 1.0f
                    uidOffsetB = 1
                    cU = 0.0f
                    cV = 0.0f
                    uidOffsetC = 2
                } else {
                    val texFace = texturedFace and 0xff
                    renderType = mesh.texturesMapping!![texFace]
                    val faceA = mesh.facesA!![faceIndex]
                    val faceB = mesh.facesB!![faceIndex]
                    val faceC = mesh.facesC!![faceIndex]

                    if (renderType.toInt() == 0) {
                        val texVertexA = mesh.textureFaceX!![texFace]
                        val texVertexB = mesh.textureFaceY!![texFace]
                        val texVertexC = mesh.textureFaceZ!![texFace]
                        val texOriginX = mesh.verticesX!![texVertexA.toInt()].toFloat()
                        val texOriginY = mesh.verticesY!![texVertexA.toInt()].toFloat()
                        val texOriginZ = mesh.verticesZ!![texVertexA.toInt()].toFloat()
                        val texEdge1X = mesh.verticesX!![texVertexB.toInt()].toFloat() - texOriginX
                        val texEdge1Y = mesh.verticesY!![texVertexB.toInt()].toFloat() - texOriginY
                        val texEdge1Z = mesh.verticesZ!![texVertexB.toInt()].toFloat() - texOriginZ
                        val texEdge2X = mesh.verticesX!![texVertexC.toInt()].toFloat() - texOriginX
                        val texEdge2Y = mesh.verticesY!![texVertexC.toInt()].toFloat() - texOriginY
                        val texEdge2Z = mesh.verticesZ!![texVertexC.toInt()].toFloat() - texOriginZ
                        val vertexAOffX = mesh.verticesX!![faceA.toInt()].toFloat() - texOriginX
                        val vertexAOffY = mesh.verticesY!![faceA.toInt()].toFloat() - texOriginY
                        val vertexAOffZ = mesh.verticesZ!![faceA.toInt()].toFloat() - texOriginZ
                        val vertexBOffX = mesh.verticesX!![faceB.toInt()].toFloat() - texOriginX
                        val vertexBOffY = mesh.verticesY!![faceB.toInt()].toFloat() - texOriginY
                        val vertexBOffZ = mesh.verticesZ!![faceB.toInt()].toFloat() - texOriginZ
                        val vertexCOffX = mesh.verticesX!![faceC.toInt()].toFloat() - texOriginX
                        val vertexCOffY = mesh.verticesY!![faceC.toInt()].toFloat() - texOriginY
                        val vertexCOffZ = mesh.verticesZ!![faceC.toInt()].toFloat() - texOriginZ
                        val texCrossX = texEdge1Y * texEdge2Z - texEdge1Z * texEdge2Y
                        val texCrossY = texEdge1Z * texEdge2X - texEdge1X * texEdge2Z
                        val texCrossZ = texEdge1X * texEdge2Y - texEdge1Y * texEdge2X
                        var basisX = texEdge2Y * texCrossZ - texEdge2Z * texCrossY
                        var basisY = texEdge2Z * texCrossX - texEdge2X * texCrossZ
                        var basisZ = texEdge2X * texCrossY - texEdge2Y * texCrossX
                        var invDeterminant = 1.0f / (basisX * texEdge1X + basisY * texEdge1Y + basisZ * texEdge1Z)
                        aU = (basisX * vertexAOffX + basisY * vertexAOffY + basisZ * vertexAOffZ) * invDeterminant
                        bU = (basisX * vertexBOffX + basisY * vertexBOffY + basisZ * vertexBOffZ) * invDeterminant
                        cU = (basisX * vertexCOffX + basisY * vertexCOffY + basisZ * vertexCOffZ) * invDeterminant
                        basisX = texEdge1Y * texCrossZ - texEdge1Z * texCrossY
                        basisY = texEdge1Z * texCrossX - texEdge1X * texCrossZ
                        basisZ = texEdge1X * texCrossY - texEdge1Y * texCrossX
                        invDeterminant = 1.0f / (basisX * texEdge2X + basisY * texEdge2Y + basisZ * texEdge2Z)
                        aV = (basisX * vertexAOffX + basisY * vertexAOffY + basisZ * vertexAOffZ) * invDeterminant
                        bV = (basisX * vertexBOffX + basisY * vertexBOffY + basisZ * vertexBOffZ) * invDeterminant
                        cV = (basisX * vertexCOffX + basisY * vertexCOffY + basisZ * vertexCOffZ) * invDeterminant
                    } else {
                        val centerX = transformMatrix!!.centerX!![texFace]
                        val centerY = transformMatrix.centerY!![texFace]
                        val centerZ = transformMatrix.centerZ!![texFace]
                        val matrix = transformMatrix.matrix!![texFace]
                        val direction = mesh.texturesDirection!![texFace]
                        val textureSpeed = mesh.texturesSpeed!![texFace] / 256.0f

                        if (renderType.toInt() == 1) {
                            val cylindricalScale = mesh.texturesScaleZ!![texFace] / 1024.0f
                            mapCylindericalUv(mesh.verticesX!![faceA.toInt()], mesh.verticesY!![faceA.toInt()], mesh.verticesZ!![faceA.toInt()], centerX, centerY, centerZ, matrix, cylindricalScale, direction.toInt(), textureSpeed, uv)
                            aU = uv[0]
                            aV = uv[1]
                            mapCylindericalUv(mesh.verticesX!![faceB.toInt()], mesh.verticesY!![faceB.toInt()], mesh.verticesZ!![faceB.toInt()], centerX, centerY, centerZ, matrix, cylindricalScale, direction.toInt(), textureSpeed, uv)
                            bU = uv[0]
                            bV = uv[1]
                            mapCylindericalUv(mesh.verticesX!![faceC.toInt()], mesh.verticesY!![faceC.toInt()], mesh.verticesZ!![faceC.toInt()], centerX, centerY, centerZ, matrix, cylindricalScale, direction.toInt(), textureSpeed, uv)
                            cU = uv[0]
                            cV = uv[1]
                            val halfCylindricalScale = cylindricalScale / 2.0f

                            if ((direction.toInt() and 0x1) == 0) {
                                if (bU - aU > halfCylindricalScale) {
                                    bU -= cylindricalScale
                                    uidOffsetB = 1
                                } else if (aU - bU > halfCylindricalScale) {
                                    bU += cylindricalScale
                                    uidOffsetB = 2
                                }

                                if (cU - aU > halfCylindricalScale) {
                                    cU -= cylindricalScale
                                    uidOffsetC = 1
                                } else if (aU - cU > halfCylindricalScale) {
                                    cU += cylindricalScale
                                    uidOffsetC = 2
                                }
                            } else {
                                if (bV - aV > halfCylindricalScale) {
                                    bV -= cylindricalScale
                                    uidOffsetB = 1
                                } else if (aV - bV > halfCylindricalScale) {
                                    bV += cylindricalScale
                                    uidOffsetB = 2
                                }

                                if (cV - aV > halfCylindricalScale) {
                                    cV -= cylindricalScale
                                    uidOffsetC = 1
                                } else if (aV - cV > halfCylindricalScale) {
                                    cV += cylindricalScale
                                    uidOffsetC = 2
                                }
                            }
                        } else if (renderType.toInt() == 2) {
                            val uTrans = mesh.texturesUtrans!![texFace] / 256.0f
                            val vTrans = mesh.texturesVtrans!![texFace] / 256.0f
                            val edgeDX = mesh.verticesX!![faceB.toInt()] - mesh.verticesX!![faceA.toInt()]
                            val edgeDY = mesh.verticesY!![faceB.toInt()] - mesh.verticesY!![faceA.toInt()]
                            val edgeDZ = mesh.verticesZ!![faceB.toInt()] - mesh.verticesZ!![faceA.toInt()]
                            val edgeDX2 = mesh.verticesX!![faceC.toInt()] - mesh.verticesX!![faceA.toInt()]
                            val edgeDY2 = mesh.verticesY!![faceC.toInt()] - mesh.verticesY!![faceA.toInt()]
                            val edgeDZ2 = mesh.verticesZ!![faceC.toInt()] - mesh.verticesZ!![faceA.toInt()]
                            val crossX = edgeDY * edgeDZ2 - edgeDY2 * edgeDZ
                            val crossY = edgeDZ * edgeDX2 - edgeDZ2 * edgeDX
                            val crossZ = edgeDX * edgeDY2 - edgeDX2 * edgeDY
                            val scaleFactorX = 64.0f / mesh.texturesScaleX!![texFace]
                            val scaleFactorY = 64.0f / mesh.texturesScaleY!![texFace]
                            val scaleFactorZ = 64.0f / mesh.texturesScaleZ!![texFace]
                            val projectedNormalX = (crossX * matrix!![0] + crossY * matrix[1] + crossZ * matrix[2]) / scaleFactorX
                            val projectedNormalY = (crossX * matrix[3] + crossY * matrix[4] + crossZ * matrix[5]) / scaleFactorY
                            val projectedNormalZ = (crossX * matrix[6] + crossY * matrix[7] + crossZ * matrix[8]) / scaleFactorZ
                            cubeFaceIndex = getCubeFace(projectedNormalX, projectedNormalY, projectedNormalZ)
                            mapRectangularUv(mesh.verticesX!![faceA.toInt()], mesh.verticesY!![faceA.toInt()], mesh.verticesZ!![faceA.toInt()], centerX, centerY, centerZ, cubeFaceIndex, matrix, direction.toInt(), textureSpeed, uTrans, vTrans, uv)
                            aU = uv[0]
                            aV = uv[1]
                            mapRectangularUv(mesh.verticesX!![faceB.toInt()], mesh.verticesY!![faceB.toInt()], mesh.verticesZ!![faceB.toInt()], centerX, centerY, centerZ, cubeFaceIndex, matrix, direction.toInt(), textureSpeed, uTrans, vTrans, uv)
                            bU = uv[0]
                            bV = uv[1]
                            mapRectangularUv(mesh.verticesX!![faceC.toInt()], mesh.verticesY!![faceC.toInt()], mesh.verticesZ!![faceC.toInt()], centerX, centerY, centerZ, cubeFaceIndex, matrix, direction.toInt(), textureSpeed, uTrans, vTrans, uv)
                            cU = uv[0]
                            cV = uv[1]
                        } else if (renderType.toInt() == 3) {
                            mapSphericalUv(mesh.verticesX!![faceA.toInt()], mesh.verticesY!![faceA.toInt()], mesh.verticesZ!![faceA.toInt()], centerX, centerY, centerZ, matrix, direction.toInt(), textureSpeed, uv)
                            aU = uv[0]
                            aV = uv[1]
                            mapSphericalUv(mesh.verticesX!![faceB.toInt()], mesh.verticesY!![faceB.toInt()], mesh.verticesZ!![faceB.toInt()], centerX, centerY, centerZ, matrix, direction.toInt(), textureSpeed, uv)
                            bU = uv[0]
                            bV = uv[1]
                            mapSphericalUv(mesh.verticesX!![faceC.toInt()], mesh.verticesY!![faceC.toInt()], mesh.verticesZ!![faceC.toInt()], centerX, centerY, centerZ, matrix, direction.toInt(), textureSpeed, uv)
                            cU = uv[0]
                            cV = uv[1]

                            if ((direction.toInt() and 0x1) == 0) {
                                if (bU - aU > 0.5f) {
                                    bU--
                                    uidOffsetB = 1
                                } else if (aU - bU > 0.5f) {
                                    bU++
                                    uidOffsetB = 2
                                }

                                if (cU - aU > 0.5f) {
                                    cU--
                                    uidOffsetC = 1
                                } else if (aU - cU > 0.5f) {
                                    cU++
                                    uidOffsetC = 2
                                }
                            } else {
                                if (bV - aV > 0.5f) {
                                    bV--
                                    uidOffsetB = 1
                                } else if (aV - bV > 0.5f) {
                                    bV++
                                    uidOffsetB = 2
                                }

                                if (cV - aV > 0.5f) {
                                    cV--
                                    uidOffsetC = 1
                                } else if (aV - cV > 0.5f) {
                                    cV++
                                    uidOffsetC = 2
                                }
                            }
                        }
                    }
                }
            }

            renderType = if (mesh.faceRenderType == null) {
                0
            } else {
                mesh.faceRenderType!![faceIndex]
            }

            if (renderType.toInt() == 0) {
                val uid = (texturedFace.toLong() shl 2) + ((cubeFaceIndex.toLong() shl 24) + (color.toLong() shl 8) + alphaAmt shl 32)
                val faceA = mesh.facesA!![faceIndex]
                val faceB = mesh.facesB!![faceIndex]
                val faceC = mesh.facesC!![faceIndex]
                var vertexNormal = vertexNormals[faceA.toInt()]!!
                this.facesA!![faceTriangleIndex] = connectNormal(faceA.toInt(), uid, vertexNormal.x, vertexNormal.y, vertexNormal.z, vertexNormal.magnitude, aU, aV)
                vertexNormal = vertexNormals[faceB.toInt()]!!
                this.facesB!![faceTriangleIndex] = connectNormal(faceB.toInt(), uid + uidOffsetB, vertexNormal.x, vertexNormal.y, vertexNormal.z, vertexNormal.magnitude, bU, bV)
                vertexNormal = vertexNormals[faceC.toInt()]!!
                this.facesC!![faceTriangleIndex] = connectNormal(faceC.toInt(), uid + uidOffsetC, vertexNormal.x, vertexNormal.y, vertexNormal.z, vertexNormal.magnitude, cU, cV)
            } else if (renderType.toInt() == 1) {
                val faceNormal = faceNormals[faceIndex]!!
                val uid = ((texturedFace.toLong() shl 2) + (if (faceNormal.x > 0) 1024 else 2048) + ((faceNormal.y + 256).toLong() shl 12) + ((faceNormal.z + 256).toLong() shl 22)) + ((cubeFaceIndex.toLong() shl 24) + (color.toLong() shl 8) + alphaAmt shl 32)
                this.facesA!![faceTriangleIndex] = connectNormal(mesh.facesA!![faceIndex].toInt(), uid, faceNormal.x, faceNormal.y, faceNormal.z, 0, aU, aV)
                this.facesB!![faceTriangleIndex] = connectNormal(mesh.facesB!![faceIndex].toInt(), uid + uidOffsetB, faceNormal.x, faceNormal.y, faceNormal.z, 0, bU, bV)
                this.facesC!![faceTriangleIndex] = connectNormal(mesh.facesC!![faceIndex].toInt(), uid + uidOffsetC, faceNormal.x, faceNormal.y, faceNormal.z, 0, cU, cV)
            }

            if (mesh.facesAlpha != null) {
                this.facesAlpha!![faceTriangleIndex] = mesh.facesAlpha!![faceIndex]
            }

            if (mesh.facesSubmeshes != null) {
                this.facesSubmeshes!![faceTriangleIndex] = mesh.facesSubmeshes!![faceIndex]
            }

            this.facesColor!![faceTriangleIndex] = mesh.facesColor!![faceIndex]
            this.facesMaterial!![faceTriangleIndex] = materialId
        }

        var batchIndex = 0
        var lastMaterial: Short = -10000

        for (face in 0 until this.renderedFaceCount) {
            val materialId = this.facesMaterial!![face]
            if (materialId != lastMaterial) {
                batchIndex++
                lastMaterial = materialId
            }
        }

        this.materialBatchOffsets = IntArray(batchIndex + 1)
        batchIndex = 0
        lastMaterial = -10000

        for (face in 0 until this.renderedFaceCount) {
            val materialId = this.facesMaterial!![face]
            if (materialId != lastMaterial) {
                this.materialBatchOffsets!![batchIndex++] = face
                lastMaterial = materialId
            }
        }

        this.materialBatchOffsets!![batchIndex] = this.renderedFaceCount
        normalsUid = null
        this.normalsX = clone(this.normalsX!!, this.numNormals)
        this.normalsY = clone(this.normalsY!!, this.numNormals)
        this.normalsZ = clone(this.normalsZ!!, this.numNormals)
        this.magnitudes = clone(this.magnitudes!!, this.numNormals)
        this.textureU = clone(this.textureU!!, this.numNormals)
        this.textureV = clone(this.textureV!!, this.numNormals)

        if (mesh.vertexGroups != null && ModelFunctionMaskGL.altersVertices(rasterizerFlags)) {
            this.vertexGroups = mesh.createVertexGroups(false) as Array<IntArray?>?
        }

        if (mesh.billboards != null && ModelFunctionMaskGL.altersBillboards(rasterizerFlags)) {
            this.billboardGroups = mesh.createBillboardGroups() as Array<IntArray?>?
        }

        if (mesh.faceGroups != null && ModelFunctionMaskGL.altersFaces(rasterizerFlags)) {
            var maxGroupIndex = 0
            val groupCounts = IntArray(256)

            for (face in 0 until this.numFaces) {
                val group = mesh.faceGroups!![faces[face]]
                if (group >= 0) {
                    ++groupCounts[group]
                    if (group > maxGroupIndex) {
                        maxGroupIndex = group
                    }
                }
            }

            this.faceGroups = arrayOfNulls(maxGroupIndex + 1)

            for (group in 0..maxGroupIndex) {
                this.faceGroups!![group] = IntArray(groupCounts[group])
                groupCounts[group] = 0
            }

            for (face in 0 until this.numFaces) {
                val group = mesh.faceGroups!![faces[face]]
                if (group >= 0) {
                    this.faceGroups!![group]!![groupCounts[group]++] = face
                }
            }
        }
    }

    private fun connectNormal(face: Int, uid: Long, x: Int, y: Int, z: Int, magnitude: Int, u: Float, v: Float): Short {
        invalidateModernContent()
        val startIndex = this.triangles!![face]
        val endIndex = this.triangles!![face + 1]
        var slotIndex = 0

        for (searchIndex in startIndex until endIndex) {
            val normalRef = this.vertexNormalIndices!![searchIndex]
            if (normalRef.toInt() == 0) {
                slotIndex = searchIndex
                break
            }

            if (normalsUid!![searchIndex] == uid) {
                return (normalRef - 1).toShort()
            }
        }

        this.vertexNormalIndices!![slotIndex] = (this.numNormals + 1).toShort()
        normalsUid!![slotIndex] = uid
        this.normalsX!![this.numNormals] = x.toShort()
        this.normalsY!![this.numNormals] = y.toShort()
        this.normalsZ!![this.numNormals] = z.toShort()
        this.magnitudes!![this.numNormals] = magnitude.toByte()
        this.textureU!![this.numNormals] = u
        this.textureV!![this.numNormals] = v
        return (this.numNormals++).toShort()
    }

    private fun projectShadow(shadow: ShadowGL, shadowOffsetX: Int, shadowOffsetZ: Int, shadowScale: Int) {
        val scratch = this.toolkit.projectionScratch(this.numNormals)
        val projectedVerticesX = scratch.verticesX
        val projectedVerticesY = scratch.verticesY

        for (v in 0 until this.maxDepth) {
            val x = ((this.verticesX!![v] - (shadowOffsetX * this.verticesY!![v] shr 8)) shr shadowScale) - shadow.startX
            val y = ((this.verticesZ!![v] - (shadowOffsetZ * this.verticesY!![v] shr 8)) shr shadowScale) - shadow.startY
            val triStart = this.triangles!![v]
            val triEnd = this.triangles!![v + 1]

            for (triIdx in triStart until triEnd) {
                val normalIdx = (this.vertexNormalIndices!![triIdx].toInt() and 0xffff) - 1
                if (normalIdx == -1) {
                    break
                }

                projectedVerticesX[normalIdx] = x
                projectedVerticesY[normalIdx] = y
            }
        }

        for (face in 0 until this.renderedFaceCount) {
            val faceA = this.facesA!![face].toInt() and 0xffff
            val faceB = this.facesB!![face].toInt() and 0xffff
            val faceC = this.facesC!![face].toInt() and 0xffff
            val xA = projectedVerticesX[faceA]
            val xB = projectedVerticesX[faceB]
            val xC = projectedVerticesX[faceC]
            val yA = projectedVerticesY[faceA]
            val yB = projectedVerticesY[faceB]
            val yC = projectedVerticesY[faceC]
            if ((xA - xB) * (yB - yC) - (yB - yA) * (xC - xB) > 0) {
                shadow.markShadedTriangle(yA, yB, yC, xA, xB, xC)
            }
        }
    }

    override fun applyShading(functionMask: Int) {
        invalidateModernContent()
        this.functionMask = functionMask
        if (this.mergedNormals != null && (this.functionMask and 0x10000) == 0) {
            this.normalsX = this.mergedNormals!!.normalsX
            this.normalsY = this.mergedNormals!!.normalsY
            this.normalsZ = this.mergedNormals!!.normalsZ
            this.magnitudes = this.mergedNormals!!.magnitude
            this.mergedNormals = null
        }

        this.updateRequired = true
        releaseUploadedData()
    }

    override fun copy(copyType: Byte, functionMask: Int, recalculateBeforeCopy: Boolean): Model? {
        var usesPreallocated = false
        val dst: ModelGL
        val src: ModelGL

        if (copyType > 0 && copyType <= 7) {
            src = this.toolkit.copyDst[copyType - 1]!!
            dst = this.toolkit.copySrc[copyType - 1]!!
            usesPreallocated = true
        } else {
            dst = ModelGL(this.toolkit)
            src = dst
        }

        return copyModelData(dst, src, functionMask, usesPreallocated)
    }

    /**
     * Gives this copy its own normal arrays wherever [copyModelData] left it sharing
     * [base]'s. A copy that outlives the frame (a private pose kept across ticks) can be
     * merged with its neighbours (applyShading(model, ...)), and through shared arrays
     * that mutates the base every other instance and neighbour derives its lighting from.
     */
    fun detachSharedNormals(base: ModelGL) {
        if (this.normalsX != null && this.normalsX === base.normalsX) this.normalsX = this.normalsX!!.copyOf()
        if (this.normalsY != null && this.normalsY === base.normalsY) this.normalsY = this.normalsY!!.copyOf()
        if (this.normalsZ != null && this.normalsZ === base.normalsZ) this.normalsZ = this.normalsZ!!.copyOf()
        if (this.magnitudes != null && this.magnitudes === base.magnitudes) this.magnitudes = this.magnitudes!!.copyOf()
        val merged = this.mergedNormals
        if (merged != null && merged === base.mergedNormals) {
            val copy = ModelNormalsGL()
            copy.normalsX = merged.normalsX?.copyOf()
            copy.normalsY = merged.normalsY?.copyOf()
            copy.normalsZ = merged.normalsZ?.copyOf()
            copy.magnitude = merged.magnitude?.copyOf()
            this.mergedNormals = copy
        }
    }

    fun copyModelData(dst: ModelGL, src: ModelGL, flags: Int, usesPreallocated: Boolean): Model {
        dst.invalidateModernContent()
        dst.renderState = 0
        dst.functionMask = flags
        // Capture animation intent NOW, from the copy mask, before any later
        // applyShading(<base mask>) overwrites functionMask / nulls vertexGroups.
        // A vertex-altering copy mask means this is (or will be animated into) a
        // per-frame pose snapshot the modern content cache must never key on.
        // Set unconditionally (SET, not OR) so reused preallocated copy slots never
        // carry a stale true into a later non-animated copy.
        dst.animatedPoseSnapshot = ModelFunctionMaskGL.altersVertices(flags) ||
            (this.vertexGroups != null && ModelFunctionMaskGL.altersAnyVerticeType(flags))
        // Modern base geometry is immutable and shared with copies; each copy
        // tracks its own accumulated yaw. SET (not OR) so reused preallocated
        // copy slots never carry stale values from an earlier copy.
        dst.modernBuilt = this.modernBuilt
        dst.modernYaw = this.modernYaw
        dst.preferenceFlags = this.preferenceFlags
        dst.ambient = this.ambient
        dst.contrast = this.contrast
        dst.numVertices = this.numVertices
        // Clamp maxDepth to numVertices to prevent ArrayIndexOutOfBoundsException
        dst.maxDepth = minOf(this.maxDepth, this.numVertices)
        dst.numNormals = this.numNormals
        dst.numFaces = this.numFaces
        dst.renderedFaceCount = this.renderedFaceCount
        dst.billboardCount = this.billboardCount

        if ((flags and 0x100) != 0) {
            dst.hasAlphaTransition = true
        } else {
            dst.hasAlphaTransition = this.hasAlphaTransition
        }

        dst.textureAnimating = this.textureAnimating
        val copyVerticesX = ModelFunctionMaskGL.altersVerticesX(flags)
        val copyVerticesY = ModelFunctionMaskGL.altersVerticesY(flags)
        val copyVerticesZ = ModelFunctionMaskGL.altersVerticesZ(flags)
        val copyAnyVertices = copyVerticesX or copyVerticesY or copyVerticesZ
        var arraySize: Int

        if (copyAnyVertices) {
            if (copyVerticesX) {
                if (src.verticesX != null && src.verticesX!!.size >= this.numVertices) {
                    dst.verticesX = src.verticesX
                } else {
                    dst.verticesX = IntArray(this.numVertices)
                    src.verticesX = dst.verticesX
                }
            } else {
                dst.verticesX = this.verticesX
            }

            if (copyVerticesY) {
                if (src.verticesY != null && src.verticesY!!.size >= this.numVertices) {
                    dst.verticesY = src.verticesY
                } else {
                    dst.verticesY = IntArray(this.numVertices)
                    src.verticesY = dst.verticesY
                }
            } else {
                dst.verticesY = this.verticesY
            }

            if (copyVerticesZ) {
                if (src.verticesZ != null && src.verticesZ!!.size >= this.numVertices) {
                    dst.verticesZ = src.verticesZ
                } else {
                    dst.verticesZ = IntArray(this.numVertices)
                    src.verticesZ = dst.verticesZ
                }
            } else {
                dst.verticesZ = this.verticesZ
            }

            for (i in 0 until this.numVertices) {
                if (copyVerticesX) {
                    dst.verticesX!![i] = this.verticesX!![i]
                }

                if (copyVerticesY) {
                    dst.verticesY!![i] = this.verticesY!![i]
                }

                if (copyVerticesZ) {
                    dst.verticesZ!![i] = this.verticesZ!![i]
                }
            }
        } else {
            dst.verticesX = this.verticesX
            dst.verticesY = this.verticesY
            dst.verticesZ = this.verticesZ
        }

        if (ModelFunctionMaskGL.altersAnyVerticeType(flags)) {
            if (usesPreallocated) {
                dst.renderState = (dst.renderState.toInt() or 0x1).toByte()
            }

            dst.vertexBuffer = src.vertexBuffer
            dst.vertexBuffer!!.offset = this.vertexBuffer!!.offset
            dst.vertexBuffer!!.buffer = this.vertexBuffer!!.buffer
        } else if (ModelFunctionMaskGL.hasVertices(flags)) {
            dst.vertexBuffer = this.vertexBuffer
        } else {
            dst.vertexBuffer = null
        }

        if (ModelFunctionMaskGL.altersColor(flags)) {
            if (src.facesColor != null && src.facesColor!!.size >= this.numFaces) {
                dst.facesColor = src.facesColor
            } else {
                dst.facesColor = ShortArray(this.numFaces)
                src.facesColor = dst.facesColor
            }

            for (i in 0 until this.numFaces) {
                dst.facesColor!![i] = this.facesColor!![i]
            }
        } else {
            dst.facesColor = this.facesColor
        }

        if (ModelFunctionMaskGL.altersAlpha(flags)) {
            if (src.facesAlpha != null && src.facesAlpha!!.size >= this.numFaces) {
                dst.facesAlpha = src.facesAlpha
            } else {
                dst.facesAlpha = ByteArray(this.numFaces)
                src.facesAlpha = dst.facesAlpha
            }

            for (i in 0 until this.numFaces) {
                dst.facesAlpha!![i] = this.facesAlpha!![i]
            }
        } else {
            dst.facesAlpha = this.facesAlpha
        }

        if (ModelFunctionMaskGL.requiresColorBufferRebuild(flags, this.preferenceFlags)) {
            if (usesPreallocated) {
                dst.renderState = (dst.renderState.toInt() or 0x2).toByte()
            }

            dst.colorsBuffer = src.colorsBuffer
            dst.colorsBuffer!!.offset = this.colorsBuffer!!.offset
            dst.colorsBuffer!!.buffer = this.colorsBuffer!!.buffer
        } else if (ModelFunctionMaskGL.hasColors(flags)) {
            dst.colorsBuffer = this.colorsBuffer
        } else {
            dst.colorsBuffer = null
        }

        var normalCount: Int
        if (ModelFunctionMaskGL.altersLight(flags)) {
            if (src.normalsX != null && src.normalsX!!.size >= this.numNormals) {
                dst.normalsX = src.normalsX
                dst.normalsY = src.normalsY
                dst.normalsZ = src.normalsZ
            } else {
                arraySize = this.numNormals
                dst.normalsX = ShortArray(arraySize)
                src.normalsX = dst.normalsX
                dst.normalsY = ShortArray(arraySize)
                src.normalsY = dst.normalsY
                dst.normalsZ = ShortArray(arraySize)
                src.normalsZ = dst.normalsZ
            }

            if (this.mergedNormals != null) {
                if (src.mergedNormals == null) {
                    src.mergedNormals = ModelNormalsGL()
                }

                val dstMergedNormals = src.mergedNormals!!
                dst.mergedNormals = dstMergedNormals

                if (dstMergedNormals.normalsX == null || dstMergedNormals.normalsX!!.size < this.numNormals) {
                    normalCount = this.numNormals
                    dstMergedNormals.normalsX = ShortArray(normalCount)
                    dstMergedNormals.normalsY = ShortArray(normalCount)
                    dstMergedNormals.normalsZ = ShortArray(normalCount)
                    dstMergedNormals.magnitude = ByteArray(normalCount)
                }

                for (idx in 0 until this.numNormals) {
                    dst.normalsX!![idx] = this.normalsX!![idx]
                    dst.normalsY!![idx] = this.normalsY!![idx]
                    dst.normalsZ!![idx] = this.normalsZ!![idx]
                    dstMergedNormals.normalsX!![idx] = this.mergedNormals!!.normalsX!![idx]
                    dstMergedNormals.normalsY!![idx] = this.mergedNormals!!.normalsY!![idx]
                    dstMergedNormals.normalsZ!![idx] = this.mergedNormals!!.normalsZ!![idx]
                    dstMergedNormals.magnitude!![idx] = this.mergedNormals!!.magnitude!![idx]
                }
            } else {
                for (idx in 0 until this.numNormals) {
                    dst.normalsX!![idx] = this.normalsX!![idx]
                    dst.normalsY!![idx] = this.normalsY!![idx]
                    dst.normalsZ!![idx] = this.normalsZ!![idx]
                }
            }
        } else {
            dst.mergedNormals = this.mergedNormals
            dst.normalsX = this.normalsX
            dst.normalsY = this.normalsY
            dst.normalsZ = this.normalsZ
        }

        dst.magnitudes = this.magnitudes

        if (ModelFunctionMaskGL.requiresNormalsBufferRebuild(flags, this.preferenceFlags)) {
            if (usesPreallocated) {
                dst.renderState = (dst.renderState.toInt() or 0x4).toByte()
            }

            dst.normalsBuffer = src.normalsBuffer
            dst.normalsBuffer!!.offset = this.normalsBuffer!!.offset
            dst.normalsBuffer!!.buffer = this.normalsBuffer!!.buffer
        } else if (ModelFunctionMaskGL.hasNormals(flags, this.preferenceFlags)) {
            dst.normalsBuffer = this.normalsBuffer
        } else {
            dst.normalsBuffer = null
        }

        dst.textureU = this.textureU
        dst.textureV = this.textureV

        if (ModelFunctionMaskGL.altersTextureCoordinates()) {
            if (usesPreallocated) {
                dst.renderState = (dst.renderState.toInt() or 0x8).toByte()
            }

            dst.textureCoordinatesBuffer = src.textureCoordinatesBuffer
            dst.textureCoordinatesBuffer!!.offset = this.textureCoordinatesBuffer!!.offset
            dst.textureCoordinatesBuffer!!.buffer = this.textureCoordinatesBuffer!!.buffer
        } else if (ModelFunctionMaskGL.hasTextureCoordinates(flags)) {
            dst.textureCoordinatesBuffer = this.textureCoordinatesBuffer
        } else {
            dst.textureCoordinatesBuffer = null
        }

        if (ModelFunctionMaskGL.altersFacing(flags)) {
            if (src.facesA != null && src.facesA!!.size >= this.numFaces) {
                dst.facesA = src.facesA
                dst.facesB = src.facesB
                dst.facesC = src.facesC
            } else {
                arraySize = this.numFaces
                dst.facesA = ShortArray(arraySize)
                src.facesA = dst.facesA
                dst.facesB = ShortArray(arraySize)
                src.facesB = dst.facesB
                dst.facesC = ShortArray(arraySize)
                src.facesC = dst.facesC
            }

            for (i in 0 until this.numFaces) {
                dst.facesA!![i] = this.facesA!![i]
                dst.facesB!![i] = this.facesB!![i]
                dst.facesC!![i] = this.facesC!![i]
            }
        } else {
            dst.facesA = this.facesA
            dst.facesB = this.facesB
            dst.facesC = this.facesC
        }

        // Whether shared or copied, the destination's face indices now match the source.
        // Note: dst.dynamicElementsRevision is deliberately untouched — it describes the
        // contents of dst's persistent dynamic element buffer, not dst's face arrays.
        dst.facesRevision = this.facesRevision

        if (ModelFunctionMaskGL.copyElementsBuffer(flags)) {
            if (usesPreallocated) {
                dst.renderState = (dst.renderState.toInt() or 0x10).toByte()
            }

            dst.modelElementsBuffer = src.modelElementsBuffer
            dst.modelElementsBuffer!!.openglElementsBuffer = this.modelElementsBuffer!!.openglElementsBuffer
        } else if (ModelFunctionMaskGL.hasElementsBuffer(flags)) {
            dst.modelElementsBuffer = this.modelElementsBuffer
        } else {
            dst.modelElementsBuffer = null
        }

        if (ModelFunctionMaskGL.altersTextures(flags)) {
            if (src.facesMaterial != null && src.facesMaterial!!.size >= this.numFaces) {
                dst.facesMaterial = src.facesMaterial
            } else {
                arraySize = this.numFaces
                dst.facesMaterial = ShortArray(arraySize)
                src.facesMaterial = dst.facesMaterial
            }

            for (i in 0 until this.numFaces) {
                dst.facesMaterial!![i] = this.facesMaterial!![i]
            }
        } else {
            dst.facesMaterial = this.facesMaterial
        }

        if (ModelFunctionMaskGL.copyBillboards(flags)) {
            if (src.billboardSurfaces != null && src.billboardSurfaces!!.size >= this.billboardCount) {
                dst.billboardSurfaces = src.billboardSurfaces

                for (i in 0 until this.billboardCount) {
                    dst.billboardSurfaces!![i]!!.copyFrom(this.billboardSurfaces!![i]!!)
                }
            } else {
                arraySize = this.billboardCount
                dst.billboardSurfaces = arrayOfNulls(arraySize)
                src.billboardSurfaces = dst.billboardSurfaces

                for (idx in 0 until this.billboardCount) {
                    dst.billboardSurfaces!![idx] = this.billboardSurfaces!![idx]!!.copy()
                }
            }
        } else {
            dst.billboardSurfaces = this.billboardSurfaces
        }

        dst.billboardEffects = this.billboardEffects

        if (this.boundsCalculated) {
            dst.size3d = this.size3d
            dst.size2d = this.size2d
            dst.minX = this.minX
            dst.maxX = this.maxX
            dst.minY = this.minY
            dst.maxY = this.maxY
            dst.minZ = this.minZ
            dst.maxZ = this.maxZ
            dst.boundsCalculated = true
        } else {
            dst.boundsCalculated = false
        }

        dst.vertexGroups = this.vertexGroups
        dst.faceGroups = this.faceGroups
        dst.billboardGroups = this.billboardGroups
        dst.vertexNormalIndices = this.vertexNormalIndices
        dst.triangles = this.triangles
        dst.verticesSubmeshes = this.verticesSubmeshes
        dst.facesSubmeshes = this.facesSubmeshes
        dst.materialBatchOffsets = this.materialBatchOffsets
        dst.emissiveTriangles = this.emissiveTriangles
        dst.effectiveVertices = this.effectiveVertices
        return dst
    }

    fun releaseUploadedData() {
        invalidateModernContent()
        if (this.updateRequired) {
            this.updateRequired = false
            if (this.emissiveTriangles == null && this.effectiveVertices == null && this.billboardEffects == null && !ModelFunctionMaskGL.altersVerticePosition(this.functionMask)) {
                var verticesXUploaded = false
                var verticesYUploaded = false
                var verticesZUploaded = false

                if (this.verticesX != null && !ModelFunctionMaskGL.retainsVerticesX(this.functionMask)) {
                    if (this.vertexBuffer != null && this.vertexBuffer!!.buffer == null) {
                        this.updateRequired = true
                    } else {
                        if (!this.boundsCalculated) {
                            calculateBounds()
                        }

                        verticesXUploaded = true
                    }
                }

                if (this.verticesY != null && !ModelFunctionMaskGL.retainsVerticesY(this.functionMask)) {
                    if (this.vertexBuffer != null && this.vertexBuffer!!.buffer == null) {
                        this.updateRequired = true
                    } else {
                        if (!this.boundsCalculated) {
                            calculateBounds()
                        }

                        verticesYUploaded = true
                    }
                }

                if (this.verticesZ != null && !ModelFunctionMaskGL.retainsVerticesZ(this.functionMask)) {
                    if (this.vertexBuffer != null && this.vertexBuffer!!.buffer == null) {
                        this.updateRequired = true
                    } else {
                        if (!this.boundsCalculated) {
                            calculateBounds()
                        }

                        verticesZUploaded = true
                    }
                }

                if (verticesXUploaded) {
                    this.verticesX = null
                }

                if (verticesYUploaded) {
                    this.verticesY = null
                }

                if (verticesZUploaded) {
                    this.verticesZ = null
                }
            }

            if (this.vertexNormalIndices != null && this.verticesX == null && this.verticesY == null && this.verticesZ == null) {
                this.vertexNormalIndices = null
                this.triangles = null
            }

            if (this.magnitudes != null && !ModelFunctionMaskGL.retainsNormals(this.functionMask, this.preferenceFlags)) {
                if (this.normalsBuffer != null) {
                    if (this.normalsBuffer!!.buffer != null) {
                        this.normalsZ = null
                        this.normalsY = null
                        this.normalsX = null
                        this.magnitudes = null
                    } else {
                        this.updateRequired = true
                    }
                } else if (this.colorsBuffer != null && this.colorsBuffer!!.buffer == null) {
                    this.updateRequired = true
                } else {
                    this.normalsZ = null
                    this.normalsY = null
                    this.normalsX = null
                    this.magnitudes = null
                }
            }

            if (this.facesColor != null && !ModelFunctionMaskGL.retainsFaceColors(this.functionMask, this.preferenceFlags)) {
                if (this.colorsBuffer != null && this.colorsBuffer!!.buffer == null) {
                    this.updateRequired = true
                } else {
                    this.facesColor = null
                }
            }

            if (this.facesAlpha != null && !ModelFunctionMaskGL.retainsFaceAlphas(this.functionMask, this.preferenceFlags)) {
                if (this.colorsBuffer != null && this.colorsBuffer!!.buffer == null) {
                    this.updateRequired = true
                } else {
                    this.facesAlpha = null
                }
            }

            if (this.textureU != null && !ModelFunctionMaskGL.altersTextureUV()) {
                if (this.textureCoordinatesBuffer != null && this.textureCoordinatesBuffer!!.buffer == null) {
                    this.updateRequired = true
                } else {
                    this.textureV = null
                    this.textureU = null
                }
            }

            if (this.facesMaterial != null && !ModelFunctionMaskGL.retainsFaceMaterials(this.functionMask, this.preferenceFlags)) {
                if (this.colorsBuffer != null && this.colorsBuffer!!.buffer == null) {
                    this.updateRequired = true
                } else {
                    this.facesMaterial = null
                }
            }

            if (this.facesA != null && !ModelFunctionMaskGL.retainsFaceIndices(this.functionMask, this.preferenceFlags)) {
                if ((this.modelElementsBuffer != null && this.modelElementsBuffer!!.openglElementsBuffer == null) || (this.colorsBuffer != null && this.colorsBuffer!!.buffer == null)) {
                    this.updateRequired = true
                } else {
                    this.facesC = null
                    this.facesB = null
                    this.facesA = null
                }
            }

            if (this.faceGroups != null && !ModelFunctionMaskGL.altersFaces(this.functionMask)) {
                this.faceGroups = null
                this.facesSubmeshes = null
            }

            if (this.vertexGroups != null && !ModelFunctionMaskGL.altersVertices(this.functionMask)) {
                this.vertexGroups = null
                this.verticesSubmeshes = null
            }

            if (this.billboardGroups != null && !ModelFunctionMaskGL.altersBillboards(this.functionMask)) {
                this.billboardGroups = null
            }

            if (this.materialBatchOffsets != null && (this.functionMask and 0x800) == 0 && (this.functionMask and 0x40000) == 0) {
                this.materialBatchOffsets = null
            }
        }
    }

    override fun yaxisRotateWithoutNormals(rotation: Int) {
        invalidateModernContent()
        // Rotates vertices but deliberately NOT normals — the modern matrix path
        // would rotate both, changing lighting. Not representable; invalidate.
        invalidateModernBuilt(0)
        val sin = MathUtils.SINE[rotation]
        val cos = MathUtils.COSINE[rotation]

        for (v in 0 until this.maxDepth) {
            val tmp = (sin * this.verticesZ!![v] + cos * this.verticesX!![v]) shr 14
            this.verticesZ!![v] = (cos * this.verticesZ!![v] - sin * this.verticesX!![v]) shr 14
            this.verticesX!![v] = tmp
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun rotateWithNormals(rotation: Int) {
        invalidateModernContent()
        // Yaw is representable in the modern per-instance model matrix; track it
        // instead of invalidating the modern base geometry.
        this.modernYaw = (this.modernYaw + rotation) and 0x3fff
        val sin = MathUtils.SINE[rotation]
        val cos = MathUtils.COSINE[rotation]

        var tmp: Int

        for (v in 0 until this.maxDepth) {
            tmp = (sin * this.verticesZ!![v] + cos * this.verticesX!![v]) shr 14
            this.verticesZ!![v] = (cos * this.verticesZ!![v] - sin * this.verticesX!![v]) shr 14
            this.verticesX!![v] = tmp
        }

        for (n in 0 until this.numNormals) {
            tmp = (this.normalsZ!![n] * sin + this.normalsX!![n] * cos) shr 14
            this.normalsZ!![n] = ((this.normalsZ!![n] * cos - this.normalsX!![n] * sin) shr 14).toShort()
            this.normalsX!![n] = tmp.toShort()
        }

        if (this.normalsBuffer == null && this.colorsBuffer != null) {
            this.colorsBuffer!!.buffer = null
        }

        if (this.normalsBuffer != null) {
            this.normalsBuffer!!.buffer = null
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    fun drawOpaqueFaces() {
        if (this.renderedFaceCount != 0) {
            if (this.renderState.toInt() != 0) {
                upload(true)
            }

            upload(false)
            val modelElements = this.modelElementsBuffer
            if (modelElements != null) {
                if (modelElements.openglElementsBuffer == null) {
                    buildElementsBuffer((this.renderState.toInt() and 0x10) != 0)
                }

                val glElementsBuffer = modelElements.openglElementsBuffer
                val materialBatchOffsetsLocal = this.materialBatchOffsets
                val facesMaterialLocal = this.facesMaterial
                if (glElementsBuffer != null && materialBatchOffsetsLocal != null && facesMaterialLocal != null) {
                    this.toolkit.setLightingEnabled(this.normalsBuffer != null)
                    this.toolkit.toggleDrawingClientstates(this.vertexBuffer, this.normalsBuffer, this.colorsBuffer, this.textureCoordinatesBuffer)
                    val batchCount = materialBatchOffsetsLocal.size - 1

                    for (batchIndex in 0 until batchCount) {
                        val batchStart = materialBatchOffsetsLocal[batchIndex]
                        val batchEnd = materialBatchOffsetsLocal[batchIndex + 1]
                        var materialId = facesMaterialLocal[batchStart].toInt() and 0xffff
                        if (materialId == 65535) {
                            materialId = -1
                        }

                        this.toolkit.renderTexture(materialId, this.normalsBuffer != null)
                        this.toolkit.drawElements(glElementsBuffer, batchStart * 3, (batchEnd - batchStart) * 3)
                    }
                }
            }

            releaseUploadedData()
        }
    }

    fun drawBillboards(modelViewMatrix: Matrix44) {
        val billboardEffectsLocal = this.billboardEffects
        val billboardSurfacesLocal = this.billboardSurfaces
        val verticesXLocal = this.verticesX
        val verticesYLocal = this.verticesY
        val verticesZLocal = this.verticesZ
        val elements = modelViewMatrix.elements

        if (billboardEffectsLocal != null && billboardSurfacesLocal != null &&
            verticesXLocal != null && verticesYLocal != null && verticesZLocal != null && elements != null) {
            val matrix = this.toolkit.billboardMatrix
            this.toolkit.pushMatrix()
            this.toolkit.setDepthBuffering(!this.hasAlphaTransition)
            this.toolkit.setLightingEnabled(false)
            this.toolkit.toggleDrawingClientstates(this.toolkit.quadVertexBuffer, null, null, this.toolkit.quadTexCoordBuffer)

            for (b in 0 until this.billboardCount) {
                val effect = billboardEffectsLocal[b] ?: continue
                val surface = billboardSurfacesLocal[b] ?: continue
                if (!effect.stationary || !this.toolkit.bloomEnabled()) {
                    val centerX = (verticesXLocal[effect.faceA] + verticesXLocal[effect.faceB] + verticesXLocal[effect.faceC]) * 0.3333333f
                    val centerY = (verticesYLocal[effect.faceA] + verticesYLocal[effect.faceB] + verticesYLocal[effect.faceC]) * 0.3333333f
                    val centerZ = (verticesZLocal[effect.faceA] + verticesZLocal[effect.faceB] + verticesZLocal[effect.faceC]) * 0.3333333f
                    val eyeX = elements[0] * centerX + elements[4] * centerY + elements[8] * centerZ + elements[12]
                    val eyeY = elements[1] * centerX + elements[5] * centerY + elements[9] * centerZ + elements[13]
                    val eyeZ = elements[2] * centerX + elements[6] * centerY + elements[10] * centerZ + elements[14]
                    val distanceScale = (1.0 / sqrt((eyeX * eyeX + eyeY * eyeY + eyeZ * eyeZ).toDouble())).toFloat() * effect.distance
                    matrix.setRotatedTranslation(surface.rotation, (surface.scaleX * effect.sizeX) shr 7, (surface.scaleY * effect.sizeY) shr 7, eyeX + surface.offsetX - eyeX * distanceScale, eyeY + surface.offsetY - eyeY * distanceScale, eyeZ - eyeZ * distanceScale)
                    this.toolkit.loadMatrix(matrix)
                    val diffuseColor = surface.diffuseColor
                    OpenGL.glColor4ub((diffuseColor shr 16).toByte(), (diffuseColor shr 8).toByte(), diffuseColor.toByte(), (diffuseColor shr 24).toByte())
                    this.toolkit.renderTexture(effect.materialId.toInt())
                    this.toolkit.setBlendMode(effect.shape.toInt())
                    this.toolkit.drawArrays(4)
                }
            }

            this.toolkit.setDepthBuffering(true)
            this.toolkit.popMatrix()
        }
    }

    fun buildElementsBuffer(reuseBuffer: Boolean) {
        val facesALocal = this.facesA
        val facesBLocal = this.facesB
        val facesCLocal = this.facesC
        val modelElements = this.modelElementsBuffer

        if (facesALocal == null || facesBLocal == null || facesCLocal == null || modelElements == null) {
            return
        }

        if (reuseBuffer) {
            // Animation only displaces vertices; the index data and material batching
            // only change when the face composition itself changes. If the dynamic
            // element buffer already holds exactly this model's indices (same face
            // content stamp and count), skip the rebuild and the GL re-upload.
            val existingBuffer = this.dynamicElementsBuffer
            if (existingBuffer != null &&
                this.dynamicElementsRevision == this.facesRevision &&
                this.dynamicElementsFaceCount == this.renderedFaceCount &&
                this.dynamicElementsGeneration == this.toolkit.glContextGeneration
            ) {
                modelElements.openglElementsBuffer = existingBuffer
                return
            }
        }

        if (this.toolkit.textureBuffer.buffer.size < this.renderedFaceCount * 6) {
            this.toolkit.textureBuffer = GlBitBuffer((this.renderedFaceCount + 100) * 6)
        } else {
            this.toolkit.textureBuffer.currentPosition = 0
        }

        val textureBuffer = this.toolkit.textureBuffer
        if (this.toolkit.bigEndianess) {
            for (faceIndex in 0 until this.renderedFaceCount) {
                textureBuffer.writeShort(facesALocal[faceIndex].toInt())
                textureBuffer.writeShort(facesBLocal[faceIndex].toInt())
                textureBuffer.writeShort(facesCLocal[faceIndex].toInt())
            }
        } else {
            for (faceIndex in 0 until this.renderedFaceCount) {
                textureBuffer.writeShortLittle(facesALocal[faceIndex].toInt())
                textureBuffer.writeShortLittle(facesBLocal[faceIndex].toInt())
                textureBuffer.writeShortLittle(facesCLocal[faceIndex].toInt())
            }
        }

        if (textureBuffer.currentPosition != 0) {
            if (reuseBuffer) {
                val existingBuffer = this.dynamicElementsBuffer
                if (existingBuffer == null) {
                    this.dynamicElementsBuffer = this.toolkit.createElementsArrayBuffer(textureBuffer.buffer, textureBuffer.currentPosition, true)
                } else {
                    existingBuffer.putElements(textureBuffer.buffer, textureBuffer.currentPosition)
                }

                this.dynamicElementsRevision = this.facesRevision
                this.dynamicElementsFaceCount = this.renderedFaceCount
                this.dynamicElementsGeneration = this.toolkit.glContextGeneration
                modelElements.openglElementsBuffer = this.dynamicElementsBuffer
            } else {
                modelElements.openglElementsBuffer = this.toolkit.createElementsArrayBuffer(textureBuffer.buffer, textureBuffer.currentPosition, false)
            }

            if (!reuseBuffer) {
                this.updateRequired = true
            }
        }
    }

    fun upload(checkFlags: Boolean) {
        val colorsBufferLocal = this.colorsBuffer
        val normalsBufferLocal = this.normalsBuffer
        val vertexBufferLocal = this.vertexBuffer
        val textureCoordinatesBufferLocal = this.textureCoordinatesBuffer

        var hasColors = colorsBufferLocal != null && colorsBufferLocal.buffer == null
        var hasNormals = normalsBufferLocal != null && normalsBufferLocal.buffer == null
        var hasVertices = vertexBufferLocal != null && vertexBufferLocal.buffer == null
        var hasTextures = textureCoordinatesBufferLocal != null && textureCoordinatesBufferLocal.buffer == null
        if (checkFlags) {
            hasColors = hasColors && (this.renderState.toInt() and 0x2) != 0
            hasNormals = hasNormals && (this.renderState.toInt() and 0x4) != 0
            hasVertices = hasVertices && (this.renderState.toInt() and 0x1) != 0
            hasTextures = hasTextures && (this.renderState.toInt() and 0x8) != 0
        }

        var entrySize = 0
        val verticesOffset = 0
        var colorsOffset = 0
        var normalsOffset = 0
        var texturesOffset = 0
        if (hasVertices) {
            entrySize += 12
        }

        if (hasColors) {
            colorsOffset = entrySize
            entrySize += 4
        }

        if (hasNormals) {
            normalsOffset = entrySize
            entrySize += 12
        }

        if (hasTextures) {
            texturesOffset = entrySize
            entrySize += 8
        }

        if (entrySize != 0) {
            if (this.toolkit.textureBuffer.buffer.size < entrySize * this.numNormals) {
                this.toolkit.textureBuffer = GlBitBuffer((this.numNormals + 100) * entrySize)
            } else {
                this.toolkit.textureBuffer.currentPosition = 0
            }

            val bitBuffer = this.toolkit.textureBuffer
            if (hasVertices) {
                val verticesXLocal = this.verticesX
                val verticesYLocal = this.verticesY
                val verticesZLocal = this.verticesZ
                val trianglesLocal = this.triangles
                val vertexNormalIndicesLocal = this.vertexNormalIndices

                if (verticesXLocal == null || verticesYLocal == null || verticesZLocal == null ||
                    trianglesLocal == null || vertexNormalIndicesLocal == null) {
                    return
                }

                if (this.toolkit.bigEndianess) {
                    for (v in 0 until this.maxDepth) {
                        val vertexX = Stream.floatToRawIntBits(verticesXLocal[v].toFloat())
                        val vertexY = Stream.floatToRawIntBits(verticesYLocal[v].toFloat())
                        val vertexZ = Stream.floatToRawIntBits(verticesZLocal[v].toFloat())
                        val triStart = trianglesLocal[v]
                        val triEnd = trianglesLocal[v + 1]

                        for (triIdx in triStart until triEnd) {
                            val normalIdx = vertexNormalIndicesLocal[triIdx] - 1
                            if (normalIdx == -1) {
                                break
                            }

                            bitBuffer.currentPosition = normalIdx * entrySize
                            bitBuffer.writeInt(vertexX)
                            bitBuffer.writeInt(vertexY)
                            bitBuffer.writeInt(vertexZ)
                        }
                    }
                } else {
                    for (v in 0 until this.maxDepth) {
                        val vertexX = Stream.floatToRawIntBits(verticesXLocal[v].toFloat())
                        val vertexY = Stream.floatToRawIntBits(verticesYLocal[v].toFloat())
                        val vertexZ = Stream.floatToRawIntBits(verticesZLocal[v].toFloat())
                        val triStart = trianglesLocal[v]
                        val triEnd = trianglesLocal[v + 1]

                        for (triIdx in triStart until triEnd) {
                            val normalIdx = (vertexNormalIndicesLocal[triIdx].toInt() and 0xffff) - 1
                            if (normalIdx == -1) {
                                break
                            }

                            bitBuffer.currentPosition = normalIdx * entrySize
                            bitBuffer.writeIntLittle(vertexX)
                            bitBuffer.writeIntLittle(vertexY)
                            bitBuffer.writeIntLittle(vertexZ)
                        }
                    }
                }
            }

            var normalScale: Float
            var normalsXArr: ShortArray?
            var normalsYArr: ShortArray?
            var normalsZArr: ShortArray?
            var magnitudesArr: ByteArray?
            var sunX: Float
            var sunY: Float
            if (hasColors) {
                val facesColorLocal = this.facesColor
                val facesMaterialLocal = this.facesMaterial
                val facesAlphaLocal = this.facesAlpha
                val facesALocal = this.facesA
                val facesBLocal = this.facesB
                val facesCLocal = this.facesC

                if (facesColorLocal == null || facesMaterialLocal == null || facesAlphaLocal == null ||
                    facesALocal == null || facesBLocal == null || facesCLocal == null) {
                    return
                }

                if (normalsBufferLocal == null) {
                    val mergedNormalsLocal = this.mergedNormals
                    if (mergedNormalsLocal != null) {
                        normalsXArr = mergedNormalsLocal.normalsX
                        normalsYArr = mergedNormalsLocal.normalsY
                        normalsZArr = mergedNormalsLocal.normalsZ
                        magnitudesArr = mergedNormalsLocal.magnitude
                    } else {
                        normalsXArr = this.normalsX
                        normalsYArr = this.normalsY
                        normalsZArr = this.normalsZ
                        magnitudesArr = this.magnitudes
                    }

                    val sunDirection = this.toolkit.sunDirection
                    if (sunDirection == null || normalsXArr == null || normalsYArr == null ||
                        normalsZArr == null || magnitudesArr == null) {
                        return
                    }

                    sunX = sunDirection[0]
                    sunY = sunDirection[1]
                    val sunZ = sunDirection[2]
                    val sunBrightness = this.toolkit.sunBrightness
                    normalScale = this.toolkit.sunColorIntensity * 768.0f / this.contrast
                    val antiSunScale = this.toolkit.antiSunColorIntensity * 768.0f / this.contrast

                    for (faceIdx in 0 until this.numFaces) {
                        val rgbColor = hslToRgb(facesColorLocal[faceIdx], facesMaterialLocal[faceIdx], this.ambient, facesAlphaLocal[faceIdx])
                        val redComponent = (rgbColor ushr 24) * this.toolkit.sunColorRed
                        val greenComponent = ((rgbColor shr 16) and 0xff) * this.toolkit.sunColorGreen
                        val blueComponent = ((rgbColor shr 8) and 0xff) * this.toolkit.sunColorBlue

                        var vertexRef = facesALocal[faceIdx]
                        var normalMag = magnitudesArr[vertexRef.toInt()].toInt()
                        var lightFactor: Float
                        if (normalMag == 0) {
                            lightFactor = (sunX * normalsXArr[vertexRef.toInt()] + sunY * normalsYArr[vertexRef.toInt()] + sunZ * normalsZArr[vertexRef.toInt()]) * 0.0026041667f
                        } else {
                            lightFactor = (sunX * normalsXArr[vertexRef.toInt()] + sunY * normalsYArr[vertexRef.toInt()] + sunZ * normalsZArr[vertexRef.toInt()]) / (normalMag * 256)
                        }

                        var brightness = sunBrightness + lightFactor * (if (lightFactor < 0.0f) antiSunScale else normalScale)
                        var red = (redComponent * brightness).toInt()
                        if (red < 0) {
                            red = 0
                        } else if (red > 255) {
                            red = 255
                        }

                        var green = (greenComponent * brightness).toInt()
                        if (green < 0) {
                            green = 0
                        } else if (green > 255) {
                            green = 255
                        }

                        var blue = (blueComponent * brightness).toInt()
                        if (blue < 0) {
                            blue = 0
                        } else if (blue > 255) {
                            blue = 255
                        }

                        bitBuffer.currentPosition = colorsOffset + vertexRef * entrySize
                        bitBuffer.writeByte(red)
                        bitBuffer.writeByte(green)
                        bitBuffer.writeByte(blue)
                        bitBuffer.writeByte(255 - (facesAlphaLocal[faceIdx].toInt() and 0xff))
                        vertexRef = facesBLocal[faceIdx]
                        normalMag = magnitudesArr[vertexRef.toInt()].toInt()
                        if (normalMag == 0) {
                            lightFactor = (sunX * normalsXArr[vertexRef.toInt()] + sunY * normalsYArr[vertexRef.toInt()] + sunZ * normalsZArr[vertexRef.toInt()]) * 0.0026041667f
                        } else {
                            lightFactor = (sunX * normalsXArr[vertexRef.toInt()] + sunY * normalsYArr[vertexRef.toInt()] + sunZ * normalsZArr[vertexRef.toInt()]) / (normalMag * 256)
                        }

                        brightness = sunBrightness + lightFactor * (if (lightFactor < 0.0f) antiSunScale else normalScale)
                        red = (redComponent * brightness).toInt()
                        if (red < 0) {
                            red = 0
                        } else if (red > 255) {
                            red = 255
                        }

                        green = (greenComponent * brightness).toInt()
                        if (green < 0) {
                            green = 0
                        } else if (green > 255) {
                            green = 255
                        }

                        blue = (blueComponent * brightness).toInt()
                        if (blue < 0) {
                            blue = 0
                        } else if (blue > 255) {
                            blue = 255
                        }

                        bitBuffer.currentPosition = colorsOffset + vertexRef * entrySize
                        bitBuffer.writeByte(red)
                        bitBuffer.writeByte(green)
                        bitBuffer.writeByte(blue)
                        bitBuffer.writeByte(255 - (facesAlphaLocal[faceIdx].toInt() and 0xff))
                        vertexRef = facesCLocal[faceIdx]
                        normalMag = magnitudesArr[vertexRef.toInt()].toInt()
                        if (normalMag == 0) {
                            lightFactor = (sunX * normalsXArr[vertexRef.toInt()] + sunY * normalsYArr[vertexRef.toInt()] + sunZ * normalsZArr[vertexRef.toInt()]) * 0.0026041667f
                        } else {
                            lightFactor = (sunX * normalsXArr[vertexRef.toInt()] + sunY * normalsYArr[vertexRef.toInt()] + sunZ * normalsZArr[vertexRef.toInt()]) / (normalMag * 256)
                        }

                        brightness = sunBrightness + lightFactor * (if (lightFactor < 0.0f) antiSunScale else normalScale)
                        red = (redComponent * brightness).toInt()
                        if (red < 0) {
                            red = 0
                        } else if (red > 255) {
                            red = 255
                        }

                        green = (greenComponent * brightness).toInt()
                        if (green < 0) {
                            green = 0
                        } else if (green > 255) {
                            green = 255
                        }

                        blue = (blueComponent * brightness).toInt()
                        if (blue < 0) {
                            blue = 0
                        } else if (blue > 255) {
                            blue = 255
                        }

                        bitBuffer.currentPosition = colorsOffset + vertexRef * entrySize
                        bitBuffer.writeByte(red)
                        bitBuffer.writeByte(green)
                        bitBuffer.writeByte(blue)
                        bitBuffer.writeByte(255 - (facesAlphaLocal[faceIdx].toInt() and 0xff))
                    }
                } else {
                    for (faceIdx in 0 until this.numFaces) {
                        val rgbColor = hslToRgb(facesColorLocal[faceIdx], facesMaterialLocal[faceIdx], this.ambient, facesAlphaLocal[faceIdx])
                        bitBuffer.currentPosition = colorsOffset + entrySize * (facesALocal[faceIdx].toInt() and 0xffff)
                        bitBuffer.writeInt(rgbColor)
                        bitBuffer.currentPosition = colorsOffset + entrySize * (facesBLocal[faceIdx].toInt() and 0xffff)
                        bitBuffer.writeInt(rgbColor)
                        bitBuffer.currentPosition = colorsOffset + entrySize * (facesCLocal[faceIdx].toInt() and 0xffff)
                        bitBuffer.writeInt(rgbColor)
                    }
                }
            }

            if (hasNormals) {
                val mergedNormalsLocal = this.mergedNormals
                if (mergedNormalsLocal != null) {
                    normalsXArr = mergedNormalsLocal.normalsX
                    normalsYArr = mergedNormalsLocal.normalsY
                    normalsZArr = mergedNormalsLocal.normalsZ
                    magnitudesArr = mergedNormalsLocal.magnitude
                } else {
                    normalsXArr = this.normalsX
                    normalsYArr = this.normalsY
                    normalsZArr = this.normalsZ
                    magnitudesArr = this.magnitudes
                }

                if (normalsXArr == null || normalsYArr == null || normalsZArr == null || magnitudesArr == null) {
                    return
                }

                sunX = 3.0f / this.contrast
                sunY = 3.0f / (this.contrast / 2 + this.contrast)
                bitBuffer.currentPosition = normalsOffset
                if (this.toolkit.bigEndianess) {
                    for (n in 0 until this.numNormals) {
                        val magnitude = magnitudesArr[n].toInt() and 0xff
                        if (magnitude == 0) {
                            bitBuffer.writeFloatBigEndian(normalsXArr[n] * sunY)
                            bitBuffer.writeFloatBigEndian(normalsYArr[n] * sunY)
                            bitBuffer.writeFloatBigEndian(normalsZArr[n] * sunY)
                        } else {
                            normalScale = sunX / magnitude
                            bitBuffer.writeFloatBigEndian(normalsXArr[n] * normalScale)
                            bitBuffer.writeFloatBigEndian(normalsYArr[n] * normalScale)
                            bitBuffer.writeFloatBigEndian(normalsZArr[n] * normalScale)
                        }

                        bitBuffer.currentPosition += entrySize - 12
                    }
                } else {
                    for (n in 0 until this.numNormals) {
                        val magnitude = magnitudesArr[n].toInt() and 0xff
                        if (magnitude == 0) {
                            bitBuffer.writeFloatLittleEndian(normalsXArr[n] * sunY)
                            bitBuffer.writeFloatLittleEndian(normalsYArr[n] * sunY)
                            bitBuffer.writeFloatLittleEndian(normalsZArr[n] * sunY)
                        } else {
                            normalScale = sunX / magnitude
                            bitBuffer.writeFloatLittleEndian(normalsXArr[n] * normalScale)
                            bitBuffer.writeFloatLittleEndian(normalsYArr[n] * normalScale)
                            bitBuffer.writeFloatLittleEndian(normalsZArr[n] * normalScale)
                        }

                        bitBuffer.currentPosition += entrySize - 12
                    }
                }
            }

            if (hasTextures) {
                val textureULocal = this.textureU
                val textureVLocal = this.textureV
                if (textureULocal == null || textureVLocal == null) {
                    return
                }

                bitBuffer.currentPosition = texturesOffset
                if (this.toolkit.bigEndianess) {
                    for (n in 0 until this.numNormals) {
                        bitBuffer.writeFloatBigEndian(textureULocal[n])
                        bitBuffer.writeFloatBigEndian(textureVLocal[n])
                        bitBuffer.currentPosition += entrySize - 8
                    }
                } else {
                    for (n in 0 until this.numNormals) {
                        bitBuffer.writeFloatLittleEndian(textureULocal[n])
                        bitBuffer.writeFloatLittleEndian(textureVLocal[n])
                        bitBuffer.currentPosition += entrySize - 8
                    }
                }
            }

            bitBuffer.currentPosition = entrySize * this.numNormals
            val buffer: ArrayBufferGL?
            if (checkFlags) {
                val existingBuffer = this.dynamicArrayBuffer
                if (existingBuffer == null) {
                    this.dynamicArrayBuffer = this.toolkit.createArrayBuffer(entrySize, bitBuffer.buffer, bitBuffer.currentPosition, true)
                } else {
                    existingBuffer.putArray(entrySize, bitBuffer.buffer, bitBuffer.currentPosition)
                }

                buffer = this.dynamicArrayBuffer
                this.renderState = 0
            } else {
                buffer = this.toolkit.createArrayBuffer(entrySize, bitBuffer.buffer, bitBuffer.currentPosition, false)
                this.updateRequired = true
            }

            if (hasVertices) {
                vertexBufferLocal?.let {
                    it.buffer = buffer
                    it.offset = verticesOffset.toByte()
                }
            }

            if (hasTextures) {
                textureCoordinatesBufferLocal?.let {
                    it.buffer = buffer
                    it.offset = texturesOffset.toByte()
                }
            }

            if (hasColors) {
                colorsBufferLocal?.let {
                    it.buffer = buffer
                    it.offset = colorsOffset.toByte()
                }
            }

            if (hasNormals) {
                normalsBufferLocal?.let {
                    it.buffer = buffer
                    it.offset = normalsOffset.toByte()
                }
            }
        }
    }

    override fun uploadToGpu() {
        if (this.numNormals > 0 && this.renderedFaceCount > 0) {
            upload(false)
            val elementsBuffer = this.modelElementsBuffer
            if ((this.renderState.toInt() and 0x10) == 0 && elementsBuffer != null && elementsBuffer.openglElementsBuffer == null) {
                buildElementsBuffer(false)
            }

            releaseUploadedData()
        }
    }

    override fun awaitLock() {
        while (this.isLocked) {
            // Wait for lock to be released
        }
    }

    override fun unlock() {
        this.isLocked = false
    }

    override fun calculateProjection(camX: Int, camY: Int, modelMatrix: Matrix43, notRendered: Boolean, sizeShift: Int): Boolean {
        val mvpMatrix = this.toolkit.tempModelMatrix
        mvpMatrix.fromMatrix(modelMatrix)
        mvpMatrix.multiplyBy(this.toolkit.viewProjectionMatrix)
        var isInView = false
        var screenMinX = Int.MAX_VALUE
        var screenMaxX = Int.MIN_VALUE
        var screenMinY = Int.MAX_VALUE
        var screenMaxY = Int.MIN_VALUE
        if (!this.boundsCalculated) {
            calculateBounds()
        }

        val centerX = (this.maxX - this.minX) shr 1
        val centerY = (this.maxY - this.minY) shr 1
        val centerZ = (this.maxZ - this.minZ) shr 1
        val boundsOriginX = centerX + this.minX
        val boundsOriginY = centerY + this.minY
        val boundsOriginZ = centerZ + this.minZ
        val bbMinX = boundsOriginX - (centerX shl sizeShift)
        val bbMinY = boundsOriginY - (centerY shl sizeShift)
        val bbMinZ = boundsOriginZ - (centerZ shl sizeShift)
        val bbMaxX = boundsOriginX + (centerX shl sizeShift)
        val bbMaxY = boundsOriginY + (centerY shl sizeShift)
        val bbMaxZ = boundsOriginZ + (centerZ shl sizeShift)
        matrixX[0] = bbMinX
        matrixY[0] = bbMinY
        matrixZ[0] = bbMinZ
        matrixX[1] = bbMaxX
        matrixY[1] = bbMinY
        matrixZ[1] = bbMinZ
        matrixX[2] = bbMinX
        matrixY[2] = bbMaxY
        matrixZ[2] = bbMinZ
        matrixX[3] = bbMaxX
        matrixY[3] = bbMaxY
        matrixZ[3] = bbMinZ
        matrixX[4] = bbMinX
        matrixY[4] = bbMinY
        matrixZ[4] = bbMaxZ
        matrixX[5] = bbMaxX
        matrixY[5] = bbMinY
        matrixZ[5] = bbMaxZ
        matrixX[6] = bbMinX
        matrixY[6] = bbMaxY
        matrixZ[6] = bbMaxZ
        matrixX[7] = bbMaxX
        matrixY[7] = bbMaxY
        matrixZ[7] = bbMaxZ

        for (cornerIdx in 0 until 8) {
            val cornerX = matrixX[cornerIdx]
            val cornerY = matrixY[cornerIdx]
            val cornerZ = matrixZ[cornerIdx]
            val clipZ = mvpMatrix.elements!![2] * cornerX + mvpMatrix.elements!![6] * cornerY + mvpMatrix.elements!![10] * cornerZ + mvpMatrix.elements!![14]
            val clipW = mvpMatrix.elements!![3] * cornerX + mvpMatrix.elements!![7] * cornerY + mvpMatrix.elements!![11] * cornerZ + mvpMatrix.elements!![15]
            if (clipZ >= -clipW) {
                val clipX = mvpMatrix.elements!![0] * cornerX + mvpMatrix.elements!![4] * cornerY + mvpMatrix.elements!![8] * cornerZ + mvpMatrix.elements!![12]
                val clipY = mvpMatrix.elements!![1] * cornerX + mvpMatrix.elements!![5] * cornerY + mvpMatrix.elements!![9] * cornerZ + mvpMatrix.elements!![13]
                val screenX = (this.toolkit.viewportOriginX + this.toolkit.imageProducerCenterX * clipX / clipW).toInt()
                val screenY = (this.toolkit.viewportOriginY + this.toolkit.imageProducerCenterY * clipY / clipW).toInt()
                if (screenX < screenMinX) {
                    screenMinX = screenX
                }

                if (screenX > screenMaxX) {
                    screenMaxX = screenX
                }

                if (screenY < screenMinY) {
                    screenMinY = screenY
                }

                if (screenY > screenMaxY) {
                    screenMaxY = screenY
                }

                isInView = true
            }
        }

        if (isInView && camX > screenMinX && camX < screenMaxX && camY > screenMinY && camY < screenMaxY) {
            if (notRendered) {
                return true
            }

            val scratch = this.toolkit.projectionScratch(this.numNormals)
            val projectedVerticesX = scratch.verticesX
            val projectedVerticesY = scratch.verticesY

            for (vertIdx in 0 until this.maxDepth) {
                val vx = this.verticesX!![vertIdx]
                val vy = this.verticesY!![vertIdx]
                val vz = this.verticesZ!![vertIdx]
                val clipZ = mvpMatrix.elements!![2] * vx + mvpMatrix.elements!![6] * vy + mvpMatrix.elements!![10] * vz + mvpMatrix.elements!![14]
                val clipW = mvpMatrix.elements!![3] * vx + mvpMatrix.elements!![7] * vy + mvpMatrix.elements!![11] * vz + mvpMatrix.elements!![15]
                if (clipZ >= -clipW) {
                    val clipX = mvpMatrix.elements!![0] * vx + mvpMatrix.elements!![4] * vy + mvpMatrix.elements!![8] * vz + mvpMatrix.elements!![12]
                    val clipY = mvpMatrix.elements!![1] * vx + mvpMatrix.elements!![5] * vy + mvpMatrix.elements!![9] * vz + mvpMatrix.elements!![13]
                    val triStart = this.triangles!![vertIdx]
                    val triEnd = this.triangles!![vertIdx + 1]

                    for (triIdx in triStart until triEnd) {
                        val normalIdx = this.vertexNormalIndices!![triIdx] - 1
                        if (normalIdx == -1) {
                            break
                        }

                        projectedVerticesX[normalIdx] = (this.toolkit.viewportOriginX + this.toolkit.imageProducerCenterX * clipX / clipW).toInt()
                        projectedVerticesY[normalIdx] = (this.toolkit.viewportOriginY + this.toolkit.imageProducerCenterY * clipY / clipW).toInt()
                    }
                } else {
                    val triStart = this.triangles!![vertIdx]
                    val triEnd = this.triangles!![vertIdx + 1]

                    for (triIdx in triStart until triEnd) {
                        val normalIdx = this.vertexNormalIndices!![triIdx] - 1
                        if (normalIdx == -1) {
                            break
                        }

                        projectedVerticesX[this.vertexNormalIndices!![triIdx] - 1] = -999999
                    }
                }
            }

            for (faceIdx in 0 until this.numFaces) {
                if (projectedVerticesX[this.facesA!![faceIdx].toInt()] != -999999 && projectedVerticesX[this.facesB!![faceIdx].toInt()] != -999999 && projectedVerticesX[this.facesC!![faceIdx].toInt()] != -999999 && isHovering(camX, camY, projectedVerticesY[this.facesA!![faceIdx].toInt()], projectedVerticesY[this.facesB!![faceIdx].toInt()], projectedVerticesY[this.facesC!![faceIdx].toInt()], projectedVerticesX[this.facesA!![faceIdx].toInt()], projectedVerticesX[this.facesB!![faceIdx].toInt()], projectedVerticesX[this.facesC!![faceIdx].toInt()])) {
                    return true
                }
            }
        }

        return false
    }

    fun isHovering(mouseX: Int, mouseY: Int, yA: Int, yB: Int, yC: Int, xA: Int, xB: Int, xC: Int): Boolean {
        return (mouseY >= yA || mouseY >= yB || mouseY >= yC) && ((mouseY <= yA || mouseY <= yB || mouseY <= yC) && ((mouseX >= xA || mouseX >= xB || mouseX >= xC) && (mouseX <= xA || mouseX <= xB || mouseX <= xC)))
    }

    override fun drawMatrixThreaded(projectionMatrix: Matrix43, boundingBox: BoundingBoxPolygon?, rasterizerFunctionMask: Int) {
        // Check if the ACTIVE renderer is modern (not model's cached toolkit reference)
        // Models cache their toolkit at creation time, but we switch toolkits at runtime.
        // Under the modern toolkit the modern batcher is the ONLY path — when its gate
        // rejects (renderer not initialized, wrong thread, offscreen target), the model
        // draws NOTHING; the miss is counted in [ModernDiag] modelGate=accepted/total.
        val activeToolkit = Renderers.activeToolkit
        if (activeToolkit is ModernGLToolkit) {
            if (activeToolkit.isModernRenderingActive()) {
                // Render using modern pipeline
                activeToolkit.renderModelGLModern(this, projectionMatrix)
                // Still calculate bounding box for mouse interaction
                if (boundingBox != null && this.numNormals != 0) {
                    calculateBoundingBoxForModern(projectionMatrix, boundingBox)
                }
            } else {
                // Gate rejected (wrong thread / renderer not initialized): the model is
                // dropped — count it loudly so the diag exposes the gap.
                activeToolkit.countLegacySkip("ModelGL.drawMatrixThreaded(gate-rejected)")
            }
            return
        }

        if ((rasterizerFunctionMask and 0x2) != 0) {
            OpenGL.glPolygonMode(1032, 6913)
        }

        if (this.numNormals != 0) {
            val currentModelMatrix = this.toolkit.currentModelMatrix
            val modelMatrix = this.toolkit.tempModelMatrix
            val mvpMatrix = this.toolkit.tempProjectionMatrix
            modelMatrix.fromMatrix(projectionMatrix)
            mvpMatrix.copyFrom(modelMatrix)
            mvpMatrix.multiplyBy(this.toolkit.viewProjectionMatrix)
            if (!this.boundsCalculated) {
                calculateBounds()
            }

            val scaleBuffer = this.toolkit.lightParameterBuffer
            modelMatrix.scale(0.0f, this.minY.toFloat(), 0.0f, scaleBuffer)
            val minWorldX = scaleBuffer[0]
            val minWorldY = scaleBuffer[1]
            val minWorldZ = scaleBuffer[2]
            modelMatrix.scale(0.0f, this.maxY.toFloat(), 0.0f, scaleBuffer)
            val maxWorldX = scaleBuffer[0]
            val maxWorldY = scaleBuffer[1]
            val maxWorldZ = scaleBuffer[2]

            for (planeIdx in 0 until 6) {
                val frustumPlane = this.toolkit.frustumPlanes!![planeIdx]
                val distMin = frustumPlane[0] * minWorldX + frustumPlane[1] * minWorldY + frustumPlane[2] * minWorldZ + frustumPlane[3] + this.size2d
                val distMax = frustumPlane[0] * maxWorldX + frustumPlane[1] * maxWorldY + frustumPlane[2] * maxWorldZ + frustumPlane[3] + this.size2d
                if (distMin < 0.0f && distMax < 0.0f) {
                    return
                }
            }

            if (boundingBox != null) {
                var hasBehindFrustum = false
                var isVisible = true
                val centerModelX = (this.minX + this.maxX) shr 1
                val centerModelZ = (this.minZ + this.maxZ) shr 1
                var modelY = this.minY
                var clipMinX = mvpMatrix.elements!![0] * centerModelX + mvpMatrix.elements!![4] * modelY + mvpMatrix.elements!![8] * centerModelZ + mvpMatrix.elements!![12]
                var clipMinY = mvpMatrix.elements!![1] * centerModelX + mvpMatrix.elements!![5] * modelY + mvpMatrix.elements!![9] * centerModelZ + mvpMatrix.elements!![13]
                var clipMinZ = mvpMatrix.elements!![2] * centerModelX + mvpMatrix.elements!![6] * modelY + mvpMatrix.elements!![10] * centerModelZ + mvpMatrix.elements!![14]
                var clipMinW = mvpMatrix.elements!![3] * centerModelX + mvpMatrix.elements!![7] * modelY + mvpMatrix.elements!![11] * centerModelZ + mvpMatrix.elements!![15]
                if (clipMinZ >= -clipMinW) {
                    boundingBox.left = (this.toolkit.viewportOriginX + this.toolkit.imageProducerCenterX * clipMinX / clipMinW).toInt()
                    boundingBox.bottom = (this.toolkit.viewportOriginY + this.toolkit.imageProducerCenterY * clipMinY / clipMinW).toInt()
                } else {
                    hasBehindFrustum = true
                }

                modelY = this.maxY
                val clipMaxX = mvpMatrix.elements!![0] * centerModelX + mvpMatrix.elements!![4] * modelY + mvpMatrix.elements!![8] * centerModelZ + mvpMatrix.elements!![12]
                val clipMaxY = mvpMatrix.elements!![1] * centerModelX + mvpMatrix.elements!![5] * modelY + mvpMatrix.elements!![9] * centerModelZ + mvpMatrix.elements!![13]
                val clipMaxZ = mvpMatrix.elements!![2] * centerModelX + mvpMatrix.elements!![6] * modelY + mvpMatrix.elements!![10] * centerModelZ + mvpMatrix.elements!![14]
                val clipMaxW = mvpMatrix.elements!![3] * centerModelX + mvpMatrix.elements!![7] * modelY + mvpMatrix.elements!![11] * centerModelZ + mvpMatrix.elements!![15]
                if (clipMaxZ >= -clipMaxW) {
                    boundingBox.right = (this.toolkit.viewportOriginX + this.toolkit.imageProducerCenterX * clipMaxX / clipMaxW).toInt()
                    boundingBox.top = (this.toolkit.viewportOriginY + this.toolkit.imageProducerCenterY * clipMaxY / clipMaxW).toInt()
                } else {
                    hasBehindFrustum = true
                }

                var interpFactor: Float
                var interpX: Float
                if (hasBehindFrustum) {
                    if (clipMinZ < -clipMinW && clipMaxZ < -clipMaxW) {
                        isVisible = false
                    } else {
                        var interpY: Float
                        var interpW: Float
                        if (clipMinZ < -clipMinW) {
                            interpFactor = (clipMinZ + clipMinW) / (clipMaxZ + clipMaxW) - 1.0f
                            interpX = clipMinX + interpFactor * (clipMaxX - clipMinX)
                            interpY = clipMinY + interpFactor * (clipMaxY - clipMinY)
                            interpW = clipMinW + interpFactor * (clipMaxW - clipMinW)
                            boundingBox.left = (this.toolkit.viewportOriginX + this.toolkit.imageProducerCenterX * interpX / interpW).toInt()
                            boundingBox.bottom = (this.toolkit.viewportOriginY + this.toolkit.imageProducerCenterY * interpY / interpW).toInt()
                        } else if (clipMaxZ < -clipMaxW) {
                            interpFactor = (clipMaxZ + clipMaxW) / (clipMinZ + clipMinW) - 1.0f
                            interpX = clipMaxX + interpFactor * (clipMinX - clipMaxX)
                            interpY = clipMaxY + interpFactor * (clipMinY - clipMaxY)
                            interpW = clipMaxW + interpFactor * (clipMinW - clipMaxW)
                            boundingBox.right = (this.toolkit.viewportOriginX + this.toolkit.imageProducerCenterX * interpX / interpW).toInt()
                            boundingBox.top = (this.toolkit.viewportOriginY + this.toolkit.imageProducerCenterY * interpY / interpW).toInt()
                        }
                    }
                }

                if (isVisible) {
                    if (clipMinZ / clipMinW > clipMaxZ / clipMaxW) {
                        interpFactor = clipMinX + currentModelMatrix.elements!![0] * this.size2d + currentModelMatrix.elements!![12]
                        interpX = clipMinW + currentModelMatrix.elements!![3] * this.size2d + currentModelMatrix.elements!![15]
                        boundingBox.size = (this.toolkit.viewportOriginX - boundingBox.left + this.toolkit.imageProducerCenterX * interpFactor / interpX).toInt()
                    } else {
                        interpFactor = clipMaxX + currentModelMatrix.elements!![0] * this.size2d + currentModelMatrix.elements!![12]
                        interpX = clipMaxW + currentModelMatrix.elements!![3] * this.size2d + currentModelMatrix.elements!![15]
                        boundingBox.size = (this.toolkit.viewportOriginX - boundingBox.right + this.toolkit.imageProducerCenterX * interpFactor / interpX).toInt()
                    }

                    boundingBox.positionCalculated = true
                }
            }

            this.toolkit.setupMatrixRendering()
            this.toolkit.multiMatrix(modelMatrix)
            drawOpaqueFaces()
            this.toolkit.popMatrix()
            modelMatrix.multiplyBy(this.toolkit.viewMatrix44)
            drawBillboards(modelMatrix)
            if ((rasterizerFunctionMask and 0x2) != 0) {
                OpenGL.glPolygonMode(1032, 6914)  // Reset GL_FRONT_AND_BACK to GL_FILL
            }
        }
    }

    /**
     * Calculates bounding box for mouse interaction when using modern renderer.
     * This is a simplified version that doesn't require legacy GL matrix operations.
     */
    internal fun calculateBoundingBoxForModern(projectionMatrix: Matrix43, boundingBox: BoundingBoxPolygon) {
        if (!this.boundsCalculated) {
            calculateBounds()
        }

        val mvpMatrix = this.toolkit.tempProjectionMatrix
        val workingMatrix = this.toolkit.tempModelMatrix
        workingMatrix.fromMatrix(projectionMatrix)
        mvpMatrix.copyFrom(workingMatrix)
        mvpMatrix.multiplyBy(this.toolkit.viewProjectionMatrix)

        val frustumPlanes = this.toolkit.frustumPlanes
        val scaleBuffer = this.toolkit.lightParameterBuffer
        workingMatrix.scale(0.0f, this.minY.toFloat(), 0.0f, scaleBuffer)
        val minWorldX = scaleBuffer[0]
        val minWorldY = scaleBuffer[1]
        val minWorldZ = scaleBuffer[2]
        workingMatrix.scale(0.0f, this.maxY.toFloat(), 0.0f, scaleBuffer)
        val maxWorldX = scaleBuffer[0]
        val maxWorldY = scaleBuffer[1]
        val maxWorldZ = scaleBuffer[2]
        for (planeIdx in 0 until 6) {
            val plane = frustumPlanes[planeIdx]
            val distMin = plane[0] * minWorldX + plane[1] * minWorldY + plane[2] * minWorldZ + plane[3] + this.size2d
            val distMax = plane[0] * maxWorldX + plane[1] * maxWorldY + plane[2] * maxWorldZ + plane[3] + this.size2d
            if (distMin < 0.0f && distMax < 0.0f) {
                return
            }
        }

        val m = mvpMatrix.elements!!
        val originX = this.toolkit.viewportOriginX
        val originY = this.toolkit.viewportOriginY
        val halfWidth = this.toolkit.imageProducerCenterX
        val halfHeight = this.toolkit.imageProducerCenterY

        val centerX = (this.minX + this.maxX) shr 1
        val centerZ = (this.minZ + this.maxZ) shr 1
        var currentY = this.minY

        val clipX1 = m[0] * centerX + m[4] * currentY + m[8] * centerZ + m[12]
        val clipY1 = m[1] * centerX + m[5] * currentY + m[9] * centerZ + m[13]
        val clipZ1 = m[2] * centerX + m[6] * currentY + m[10] * centerZ + m[14]
        val clipW1 = m[3] * centerX + m[7] * currentY + m[11] * centerZ + m[15]

        currentY = this.maxY
        val clipX2 = m[0] * centerX + m[4] * currentY + m[8] * centerZ + m[12]
        val clipY2 = m[1] * centerX + m[5] * currentY + m[9] * centerZ + m[13]
        val clipZ2 = m[2] * centerX + m[6] * currentY + m[10] * centerZ + m[14]
        val clipW2 = m[3] * centerX + m[7] * currentY + m[11] * centerZ + m[15]

        val minBehind = clipZ1 < -clipW1
        val maxBehind = clipZ2 < -clipW2
        if (minBehind && maxBehind) {
            return
        }

        if (!minBehind) {
            boundingBox.left = (originX + halfWidth * clipX1 / clipW1).toInt()
            boundingBox.bottom = (originY + halfHeight * clipY1 / clipW1).toInt()
        } else {
            val t = (clipZ1 + clipW1) / (clipZ2 + clipW2) - 1.0f
            val w = clipW1 + t * (clipW2 - clipW1)
            boundingBox.left = (originX + halfWidth * (clipX1 + t * (clipX2 - clipX1)) / w).toInt()
            boundingBox.bottom = (originY + halfHeight * (clipY1 + t * (clipY2 - clipY1)) / w).toInt()
        }

        if (!maxBehind) {
            boundingBox.right = (originX + halfWidth * clipX2 / clipW2).toInt()
            boundingBox.top = (originY + halfHeight * clipY2 / clipW2).toInt()
        } else {
            val t = (clipZ2 + clipW2) / (clipZ1 + clipW1) - 1.0f
            val w = clipW2 + t * (clipW1 - clipW2)
            boundingBox.right = (originX + halfWidth * (clipX2 + t * (clipX1 - clipX2)) / w).toInt()
            boundingBox.top = (originY + halfHeight * (clipY2 + t * (clipY1 - clipY2)) / w).toInt()
        }

        val currentModelElements = this.toolkit.currentModelMatrix.elements!!
        if (clipZ1 / clipW1 > clipZ2 / clipW2) {
            val interpX = clipX1 + currentModelElements[0] * this.size2d + currentModelElements[12]
            val interpW = clipW1 + currentModelElements[3] * this.size2d + currentModelElements[15]
            boundingBox.size = (originX - boundingBox.left + halfWidth * interpX / interpW).toInt()
        } else {
            val interpX = clipX2 + currentModelElements[0] * this.size2d + currentModelElements[12]
            val interpW = clipW2 + currentModelElements[3] * this.size2d + currentModelElements[15]
            boundingBox.size = (originX - boundingBox.right + halfWidth * interpX / interpW).toInt()
        }

        boundingBox.positionCalculated = true
    }

    override fun translate(x: Int, y: Int, z: Int) {
        invalidateModernContent()
        invalidateModernBuilt(1)
        for (i in 0 until this.maxDepth) {
            this.verticesX!![i] += x
            this.verticesY!![i] += y
            this.verticesZ!![i] += z
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun mirror() {
        invalidateModernContent()
        // Mirroring flips winding; not representable in the modern matrix path
        // without a per-draw cull flip. Invalidate.
        invalidateModernBuilt(2)
        for (i in 0 until this.maxDepth) {
            this.verticesZ!![i] = -this.verticesZ!![i]
        }

        for (i in 0 until this.numNormals) {
            this.normalsZ!![i] = (-this.normalsZ!![i].toInt()).toShort()
        }

        if (this.mergedNormals != null) {
            for (i in 0 until this.numNormals) {
                this.mergedNormals!!.normalsZ!![i] = (-this.mergedNormals!!.normalsZ!![i].toInt()).toShort()
            }
        }

        // Negating a single axis above flips the model's handedness, which
        // reverses the on-screen winding of every triangle. Reverse the winding
        // back by swapping two corners per face, so hardware backface culling
        // still treats the visible side as front-facing. Each face corner is a
        // single index into the expanded vertex/normal/UV/colour arrays, so
        // swapping the indices moves position, normal, UV and colour together
        // and keeps them correctly associated. Only per-corner data (facesB and
        // facesC) is affected; per-face data (colour/alpha/material) is not.
        val facesBLocal = this.facesB
        val facesCLocal = this.facesC
        if (facesBLocal != null && facesCLocal != null) {
            for (i in 0 until this.numFaces) {
                val swap = facesBLocal[i]
                facesBLocal[i] = facesCLocal[i]
                facesCLocal[i] = swap
            }

            // The GL element (index) buffer is built from facesA/B/C and may
            // have been shared from the cached source model during copy(). Drop
            // it so it is rebuilt with the corrected winding on the next draw.
            this.modelElementsBuffer?.openglElementsBuffer = null

            // XOR (not a fresh counter value) so a double mirror restores the stamp,
            // and so every copy of the same source mirrored the same way shares one
            // stamp and can reuse an already-uploaded dynamic element buffer.
            this.facesRevision = this.facesRevision xor MIRRORED_FACES_REVISION_SALT
        }

        if (this.normalsBuffer == null && this.colorsBuffer != null) {
            this.colorsBuffer!!.buffer = null
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        if (this.normalsBuffer != null) {
            this.normalsBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun scale(x: Int, y: Int, z: Int) {
        invalidateModernContent()
        invalidateModernBuilt(3)
        for (i in 0 until this.maxDepth) {
            this.verticesX!![i] = (this.verticesX!![i] * x) shr 7
            this.verticesY!![i] = (this.verticesY!![i] * y) shr 7
            this.verticesZ!![i] = (this.verticesZ!![i] * z) shr 7
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun setContrast(contrast: Int) {
        invalidateModernContent()
        // Mesh-built normals carry 768/contrast baked in (ModernModelBuilder).
        invalidateModernBuilt(7)
        this.contrast = contrast.toShort()
        if (this.colorsBuffer != null) {
            this.colorsBuffer!!.buffer = null
        }
        if (this.normalsBuffer != null) {
            this.normalsBuffer!!.buffer = null
        }
    }

    override fun recolor(src: Short, dst: Short) {
        invalidateModernContent()
        invalidateModernBuilt(4)
        for (face in 0 until this.numFaces) {
            if (this.facesColor!![face] == src) {
                this.facesColor!![face] = dst
            }
        }

        if (this.billboardEffects != null) {
            for (billboard in 0 until this.billboardCount) {
                val effect = this.billboardEffects!![billboard]!!
                val surface = this.billboardSurfaces!![billboard]!!
                surface.diffuseColor = (surface.diffuseColor and -16777216) or (ColorUtil.hslToRgb!![this.facesColor!![effect.appliesOnFace].toInt() and 0xffff] and 0xffffff)
            }
        }

        if (this.colorsBuffer != null) {
            this.colorsBuffer!!.buffer = null
        }
    }

    override fun rematerial(src: Short, dst: Short) {
        invalidateModernContent()
        invalidateModernBuilt(5)
        val provider = this.toolkit.materialProvider!!

        for (face in 0 until this.numFaces) {
            if (this.facesMaterial!![face] == src) {
                this.facesMaterial!![face] = dst
            }
        }

        var srcAlpha: Byte = 0
        var srcBrightness: Byte = 0
        if (src.toInt() != -1) {
            val srcMaterial = provider.getMaterial(src.toInt() and 0xffff)
            srcAlpha = srcMaterial.alpha
            srcBrightness = srcMaterial.brightness
        }

        var dstAlpha: Byte = 0
        var dstBrightness: Byte = 0
        if (dst.toInt() != -1) {
            val dstMaterial = provider.getMaterial(dst.toInt() and 0xffff)
            dstAlpha = dstMaterial.alpha
            dstBrightness = dstMaterial.brightness
            if (dstMaterial.textureSpeedU.toInt() != 0 || dstMaterial.textureSpeedV.toInt() != 0) {
                this.textureAnimating = true
            }
        }

        if ((srcAlpha != dstAlpha) or (srcBrightness != dstBrightness)) {
            if (this.billboardEffects != null) {
                for (billboard in 0 until this.billboardCount) {
                    val effect = this.billboardEffects!![billboard]!!
                    val surface = this.billboardSurfaces!![billboard]!!
                    surface.diffuseColor = (surface.diffuseColor and -16777216) or (ColorUtil.hslToRgb!![this.facesColor!![effect.appliesOnFace].toInt() and 0xffff] and 0xffffff)
                }
            }

            if (this.colorsBuffer != null) {
                this.colorsBuffer!!.buffer = null
            }
        }
    }

    override fun tint(hue: Int, saturation: Int, lightness: Int, opacity: Int) {
        invalidateModernContent()
        invalidateModernBuilt(6)
        for (i in 0 until this.numFaces) {
            val hsl = this.facesColor!![i].toInt() and 0xffff
            var h = (hsl shr 10) and 0x3f
            var s = (hsl shr 7) and 0x7
            var l = hsl and 0x7f
            if (hue != -1) {
                h += (hue - h) * opacity shr 7
            }
            if (saturation != -1) {
                s += opacity * (saturation - s) shr 7
            }
            if (lightness != -1) {
                l += (lightness - l) * opacity shr 7
            }
            this.facesColor!![i] = ((h shl 10) or (s shl 7) or l).toShort()
        }

        if (this.billboardEffects != null) {
            for (billboard in 0 until this.billboardCount) {
                val effect = this.billboardEffects!![billboard]!!
                val surface = this.billboardSurfaces!![billboard]!!
                surface.diffuseColor = (surface.diffuseColor and -16777216) or (ColorUtil.hslToRgb!![this.facesColor!![effect.appliesOnFace].toInt() and 0xffff] and 0xffffff)
            }
        }

        if (this.colorsBuffer != null) {
            this.colorsBuffer!!.buffer = null
        }
    }

    override fun setAmbient(ambient: Int) {
        invalidateModernContent()
        // Modern base colors are baked with the createModel-time ambient.
        invalidateModernBuilt(7)
        this.ambient = ambient.toShort()
        if (this.colorsBuffer != null) {
            this.colorsBuffer!!.buffer = null
        }
    }

    override fun applyEnvironmentAlpha(alpha: Byte, transparency: ByteArray?) {
        invalidateModernContent()
        invalidateModernBuilt(8)
        if (transparency == null) {
            for (f in 0 until this.numFaces) {
                this.facesAlpha!![f] = alpha
            }
        } else {
            for (f in 0 until this.numFaces) {
                val antiAlpha = 255 - (255 - (alpha.toInt() and 0xff)) * (255 - (transparency[f].toInt() and 0xff)) / 255
                this.facesAlpha!![f] = antiAlpha.toByte()
            }
        }

        if (this.colorsBuffer != null) {
            this.colorsBuffer!!.buffer = null
        }
    }

    fun hslToRgb(colorHsl: Short, materialId: Short, ambient: Short, alpha: Byte): Int {
        var rgb = ColorUtil.hslToRgb!![adjustLuminance(colorHsl.toInt(), ambient.toInt())]
        if (materialId.toInt() != -1) {
            val materialType = this.toolkit.materialProvider!!.getMaterial(materialId.toInt() and 0xffff)
            val a = materialType.alpha.toInt() and 0xff
            var tempVal: Int
            if (a != 0) {
                val ambientRgb: Int
                if (ambient < 0) {
                    ambientRgb = 0
                } else if (ambient > 127) {
                    ambientRgb = 16777215
                } else {
                    ambientRgb = ambient.toInt() * 131586
                }

                tempVal = 256 - a
                rgb = (((ambientRgb and 0xff00ff) * a + (rgb and 0xff00ff) * tempVal and -16711936) + (a * (ambientRgb and 0xff00) + tempVal * (rgb and 0xff00) and 0xff0000)) shr 8
            }

            val b = (materialType.brightness.toInt() and 0xff)
            if (b != 0) {
                val brightness = b + 256
                var scaledR = brightness * ((rgb and 0xff0000) shr 16)
                if (scaledR > 65535) {
                    scaledR = 65535
                }

                tempVal = brightness * ((rgb and 0xff00) shr 8)
                if (tempVal > 65535) {
                    tempVal = 65535
                }

                var scaledB = (rgb and 0xff) * brightness
                if (scaledB > 65535) {
                    scaledB = 65535
                }

                rgb = (scaledB shr 8) + (tempVal and 0xff00) + ((scaledR shl 8) and 0xff0000)
            }
        }

        return (rgb shl 8) or (255 - (alpha.toInt() and 0xff))
    }

    override fun createShadowSprite(shadow: Shadow?): Shadow? {
        if (this.numNormals == 0) {
            return null
        }
        if (!this.boundsCalculated) {
            calculateBounds()
        }

        // Project the model's AABB along the sun direction onto the ground plane
        // (same projection as projectShadow applies per vertex) to size the sprite.
        val shadowOffsetX = this.toolkit.bakeShadowOffsetX()
        val shadowOffsetZ = this.toolkit.bakeShadowOffsetZ()
        val shadowScale = this.toolkit.shadowScale
        val startX: Int
        val endX: Int
        if (shadowOffsetX > 0) {
            startX = (this.minX - (shadowOffsetX * this.maxY shr 8)) shr shadowScale
            endX = (this.maxX - (shadowOffsetX * this.minY shr 8)) shr shadowScale
        } else {
            startX = (this.minX - (shadowOffsetX * this.minY shr 8)) shr shadowScale
            endX = (this.maxX - (shadowOffsetX * this.maxY shr 8)) shr shadowScale
        }

        val startY: Int
        val endY: Int
        if (shadowOffsetZ > 0) {
            startY = (this.minZ - (shadowOffsetZ * this.maxY shr 8)) shr shadowScale
            endY = (this.maxZ - (shadowOffsetZ * this.minY shr 8)) shr shadowScale
        } else {
            startY = (this.minZ - (shadowOffsetZ * this.minY shr 8)) shr shadowScale
            endY = (this.maxZ - (shadowOffsetZ * this.maxY shr 8)) shr shadowScale
        }

        val sizeX = endX - startX + 1
        val sizeY = endY - startY + 1
        val existing = shadow as ShadowGL?
        val shadowGL: ShadowGL
        if (existing != null && existing.fitsDimension(sizeX, sizeY)) {
            shadowGL = existing
            existing.reset()
        } else {
            shadowGL = ShadowGL(sizeX, sizeY)
        }

        shadowGL.setDimension(startX, startY, endX, endY)
        projectShadow(shadowGL, shadowOffsetX, shadowOffsetZ, shadowScale)
        return shadowGL
    }

    override fun applyShading(model: Model, x: Int, y: Int, z: Int, renderTypeAdjustment: Boolean) {
        invalidateModernContent()
        // Normal merging with an adjacent loc changes this model's LIGHTING (legacy
        // uploads mergedNormals in place of normals), which the mesh-built geometry
        // cannot express: it lit itself from the mesh's own normals at build time.
        // Drop it so the legacy-capture path, which reads the merged normals, draws
        // this model. Without this, building walls rendered visibly brighter than
        // legacy along every shared edge.
        invalidateModernBuilt(14)
        val openglModel = model as ModelGL
        if (this.numFaces != 0 && openglModel.numFaces != 0) {
            val glMaxDepth = openglModel.maxDepth
            val vertX = openglModel.verticesX
            val vertY = openglModel.verticesY
            val vertZ = openglModel.verticesZ
            val normX = openglModel.normalsX
            val normY = openglModel.normalsY
            val normZ = openglModel.normalsZ
            val normMag = openglModel.magnitudes
            var mergedNormalsX: ShortArray?
            var mergedNormalsY: ShortArray?
            var mergedNormalsZ: ShortArray?
            var mergedMagnitudes: ByteArray?
            if (this.mergedNormals != null) {
                mergedNormalsX = this.mergedNormals!!.normalsX
                mergedNormalsY = this.mergedNormals!!.normalsY
                mergedNormalsZ = this.mergedNormals!!.normalsZ
                mergedMagnitudes = this.mergedNormals!!.magnitude
            } else {
                mergedNormalsX = null
                mergedNormalsY = null
                mergedNormalsZ = null
                mergedMagnitudes = null
            }

            var otherNormalsX: ShortArray?
            var otherNormalsY: ShortArray?
            var otherNormalsZ: ShortArray?
            var otherMagnitudes: ByteArray?
            if (openglModel.mergedNormals != null) {
                otherNormalsX = openglModel.mergedNormals!!.normalsX
                otherNormalsY = openglModel.mergedNormals!!.normalsY
                otherNormalsZ = openglModel.mergedNormals!!.normalsZ
                otherMagnitudes = openglModel.mergedNormals!!.magnitude
            } else {
                otherNormalsX = null
                otherNormalsY = null
                otherNormalsZ = null
                otherMagnitudes = null
            }

            val otherTriangles = openglModel.triangles
            val otherNormalIndices = openglModel.vertexNormalIndices
            if (!openglModel.boundsCalculated) {
                openglModel.calculateBounds()
            }

            val otherMinY = openglModel.minY
            val otherMaxY = openglModel.maxY
            val otherMinX = openglModel.minX
            val otherMaxX = openglModel.maxX
            val otherMinZ = openglModel.minZ
            val otherMaxZ = openglModel.maxZ

            for (vertexIdx in 0 until this.maxDepth) {
                val vertexY = this.verticesY!![vertexIdx] - y
                if (vertexY >= otherMinY && vertexY <= otherMaxY) {
                    val vertexX = this.verticesX!![vertexIdx] - x
                    if (vertexX >= otherMinX && vertexX <= otherMaxX) {
                        val vertexZ = this.verticesZ!![vertexIdx] - z
                        if (vertexZ >= otherMinZ && vertexZ <= otherMaxZ) {
                            var thisNormalIdx = -1
                            var bucketStart = this.triangles!![vertexIdx]
                            var bucketEnd = this.triangles!![vertexIdx + 1]

                            var searchIdx = bucketStart
                            while (searchIdx < bucketEnd) {
                                thisNormalIdx = this.vertexNormalIndices!![searchIdx] - 1
                                if (thisNormalIdx == -1 || this.magnitudes!![thisNormalIdx].toInt() != 0) {
                                    break
                                }
                                searchIdx++
                            }

                            if (thisNormalIdx != -1) {
                                searchIdx = 0
                                while (searchIdx < glMaxDepth) {
                                    if (vertexX == vertX!![searchIdx] && vertexZ == vertZ!![searchIdx] && vertexY == vertY!![searchIdx]) {
                                        var otherNormalIdx = -1
                                        bucketStart = otherTriangles!![searchIdx]
                                        bucketEnd = otherTriangles[searchIdx + 1]

                                        for (bucketIdx in bucketStart until bucketEnd) {
                                            otherNormalIdx = otherNormalIndices!![bucketIdx] - 1
                                            if (otherNormalIdx == -1 || normMag!![otherNormalIdx].toInt() != 0) {
                                                break
                                            }
                                        }

                                        if (otherNormalIdx != -1) {
                                            if (mergedNormalsX == null) {
                                                this.mergedNormals = ModelNormalsGL()
                                                mergedNormalsX = ArrayUtils.arraycopy(this.normalsX)
                                                this.mergedNormals!!.normalsX = mergedNormalsX
                                                mergedNormalsY = ArrayUtils.arraycopy(this.normalsY)
                                                this.mergedNormals!!.normalsY = mergedNormalsY
                                                mergedNormalsZ = ArrayUtils.arraycopy(this.normalsZ)
                                                this.mergedNormals!!.normalsZ = mergedNormalsZ
                                                mergedMagnitudes = ArrayUtils.safeFullArraycopy(this.magnitudes)
                                                this.mergedNormals!!.magnitude = mergedMagnitudes
                                            }

                                            if (otherNormalsX == null) {
                                                val otherMergedNormalsGL = ModelNormalsGL()
                                                openglModel.mergedNormals = otherMergedNormalsGL
                                                otherNormalsX = ArrayUtils.arraycopy(openglModel.normalsX)
                                                otherMergedNormalsGL.normalsX = otherNormalsX
                                                otherNormalsY = ArrayUtils.arraycopy(openglModel.normalsY)
                                                otherMergedNormalsGL.normalsY = otherNormalsY
                                                otherNormalsZ = ArrayUtils.arraycopy(openglModel.normalsZ)
                                                otherMergedNormalsGL.normalsZ = otherNormalsZ
                                                otherMagnitudes = ArrayUtils.safeFullArraycopy(openglModel.magnitudes)
                                                otherMergedNormalsGL.magnitude = otherMagnitudes
                                            }

                                            // Every normal slot of the shared vertex (one per colour/texture/alpha
                                            // combination) receives the other model's ORIGINAL normal, and vice
                                            // versa. Merging only the first slot left seams on multi-material
                                            // vertices, and the scan continues so coincident duplicate vertices
                                            // of the other model all contribute.
                                            val thisX = this.normalsX!![thisNormalIdx]
                                            val thisY = this.normalsY!![thisNormalIdx]
                                            val thisZ = this.normalsZ!![thisNormalIdx]
                                            val thisMag = this.magnitudes!![thisNormalIdx]
                                            for (slot in bucketStart until bucketEnd) {
                                                val slotIdx = otherNormalIndices!![slot] - 1
                                                if (slotIdx == -1) {
                                                    break
                                                }
                                                if (otherMagnitudes!![slotIdx].toInt() != 0) {
                                                    otherNormalsX!![slotIdx] = (otherNormalsX[slotIdx] + thisX).toShort()
                                                    otherNormalsY!![slotIdx] = (otherNormalsY[slotIdx] + thisY).toShort()
                                                    otherNormalsZ!![slotIdx] = (otherNormalsZ[slotIdx] + thisZ).toShort()
                                                    otherMagnitudes[slotIdx] = (otherMagnitudes[slotIdx] + thisMag).toByte()
                                                }
                                            }
                                            val otherX = normX!![otherNormalIdx]
                                            val otherY = normY!![otherNormalIdx]
                                            val otherZ = normZ!![otherNormalIdx]
                                            val otherMag = normMag!![otherNormalIdx]
                                            val thisStart = this.triangles!![vertexIdx]
                                            val thisEnd = this.triangles!![vertexIdx + 1]
                                            for (slot in thisStart until thisEnd) {
                                                val slotIdx = this.vertexNormalIndices!![slot] - 1
                                                if (slotIdx == -1) {
                                                    break
                                                }
                                                if (mergedMagnitudes!![slotIdx].toInt() != 0) {
                                                    mergedNormalsX!![slotIdx] = (mergedNormalsX[slotIdx] + otherX).toShort()
                                                    mergedNormalsY!![slotIdx] = (mergedNormalsY[slotIdx] + otherY).toShort()
                                                    mergedNormalsZ!![slotIdx] = (mergedNormalsZ[slotIdx] + otherZ).toShort()
                                                    mergedMagnitudes[slotIdx] = (mergedMagnitudes[slotIdx] + otherMag).toByte()
                                                }
                                            }
                                        }
                                    }
                                    searchIdx++
                                }
                            }
                        }
                    }
                }
            }

            if (this.normalsBuffer != null) {
                this.normalsBuffer!!.buffer = null
            }

            if (this.colorsBuffer != null) {
                this.colorsBuffer!!.buffer = null
            }

            if (openglModel.normalsBuffer != null) {
                openglModel.normalsBuffer!!.buffer = null
            }

            if (openglModel.colorsBuffer != null) {
                openglModel.colorsBuffer!!.buffer = null
            }
        }
    }

    override fun offsetSubmesh(matrix: Matrix43, functionMask: Int, updateIdentityMatrix: Boolean) {
        invalidateModernContent()
        invalidateModernBuilt(9)
        if (this.verticesSubmeshes != null) {
            var submeshMatrix = matrix
            if (updateIdentityMatrix) {
                submeshMatrix = this.toolkit.inverseSunMatrix43
                submeshMatrix.updateMatrix(matrix)
            }

            val resultBuffer = FloatArray(3)

            for (vertexIdx in 0 until this.maxDepth) {
                if ((functionMask and this.verticesSubmeshes!![vertexIdx].toInt()) != 0) {
                    submeshMatrix.calculatePerspective(this.verticesX!![vertexIdx].toFloat(), this.verticesY!![vertexIdx].toFloat(), this.verticesZ!![vertexIdx].toFloat(), resultBuffer)
                    this.verticesX!![vertexIdx] = resultBuffer[0].toInt()
                    this.verticesY!![vertexIdx] = resultBuffer[1].toInt()
                    this.verticesZ!![vertexIdx] = resultBuffer[2].toInt()
                }
            }
        }
    }

    override fun updateParticlePosition(projectionMatrix: Matrix43) {
        val matrix = this.toolkit.tempModelMatrix
        matrix.fromMatrix(projectionMatrix)
        if (this.emissiveTriangles != null) {
            for (idx in this.emissiveTriangles!!.indices) {
                val emitter = this.emissiveTriangles!![idx]
                val activeEmitter = if (emitter.child != null) emitter.child!! else emitter

                activeEmitter.verticeXA = (matrix.elements!![12] + matrix.elements!![0] * this.verticesX!![emitter.faceA] + matrix.elements!![4] * this.verticesY!![emitter.faceA] + matrix.elements!![8] * this.verticesZ!![emitter.faceA]).toInt()
                activeEmitter.verticeYA = (matrix.elements!![13] + matrix.elements!![1] * this.verticesX!![emitter.faceA] + matrix.elements!![5] * this.verticesY!![emitter.faceA] + matrix.elements!![9] * this.verticesZ!![emitter.faceA]).toInt()
                activeEmitter.verticeZA = (matrix.elements!![14] + matrix.elements!![2] * this.verticesX!![emitter.faceA] + matrix.elements!![6] * this.verticesY!![emitter.faceA] + matrix.elements!![10] * this.verticesZ!![emitter.faceA]).toInt()
                activeEmitter.verticeXB = (matrix.elements!![12] + matrix.elements!![0] * this.verticesX!![emitter.faceB] + matrix.elements!![4] * this.verticesY!![emitter.faceB] + matrix.elements!![8] * this.verticesZ!![emitter.faceB]).toInt()
                activeEmitter.verticeYB = (matrix.elements!![13] + matrix.elements!![1] * this.verticesX!![emitter.faceB] + matrix.elements!![5] * this.verticesY!![emitter.faceB] + matrix.elements!![9] * this.verticesZ!![emitter.faceB]).toInt()
                activeEmitter.verticeZB = (matrix.elements!![14] + matrix.elements!![2] * this.verticesX!![emitter.faceB] + matrix.elements!![6] * this.verticesY!![emitter.faceB] + matrix.elements!![10] * this.verticesZ!![emitter.faceB]).toInt()
                activeEmitter.verticeXC = (matrix.elements!![12] + matrix.elements!![0] * this.verticesX!![emitter.faceC] + matrix.elements!![4] * this.verticesY!![emitter.faceC] + matrix.elements!![8] * this.verticesZ!![emitter.faceC]).toInt()
                activeEmitter.verticeYC = (matrix.elements!![13] + matrix.elements!![1] * this.verticesX!![emitter.faceC] + matrix.elements!![5] * this.verticesY!![emitter.faceC] + matrix.elements!![9] * this.verticesZ!![emitter.faceC]).toInt()
                activeEmitter.verticeZC = (matrix.elements!![14] + matrix.elements!![2] * this.verticesX!![emitter.faceC] + matrix.elements!![6] * this.verticesY!![emitter.faceC] + matrix.elements!![10] * this.verticesZ!![emitter.faceC]).toInt()
            }
        }

        if (this.effectiveVertices != null) {
            for (idx in this.effectiveVertices!!.indices) {
                val effector = this.effectiveVertices!![idx]
                val activeEffector = if (effector.child != null) effector.child!! else effector

                if (effector.matrix != null) {
                    effector.matrix!!.copyFrom(matrix)
                } else {
                    effector.matrix = Matrix44(matrix)
                }

                activeEffector.verticeX = (matrix.elements!![12] + matrix.elements!![0] * this.verticesX!![effector.appliesOnVertex] + matrix.elements!![4] * this.verticesY!![effector.appliesOnVertex] + matrix.elements!![8] * this.verticesZ!![effector.appliesOnVertex]).toInt()
                activeEffector.verticeY = (matrix.elements!![13] + matrix.elements!![1] * this.verticesX!![effector.appliesOnVertex] + matrix.elements!![5] * this.verticesY!![effector.appliesOnVertex] + matrix.elements!![9] * this.verticesZ!![effector.appliesOnVertex]).toInt()
                activeEffector.verticeZ = (matrix.elements!![14] + matrix.elements!![2] * this.verticesX!![effector.appliesOnVertex] + matrix.elements!![6] * this.verticesY!![effector.appliesOnVertex] + matrix.elements!![10] * this.verticesZ!![effector.appliesOnVertex]).toInt()
            }
        }
    }

    override fun getSize2d(): Int {
        if (!this.boundsCalculated) calculateBounds()
        return this.size2d
    }

    override fun getSize3d(): Int {
        if (!this.boundsCalculated) calculateBounds()
        return this.size3d
    }

    override fun getMinX(): Int {
        if (!this.boundsCalculated) calculateBounds()
        return this.minX.toInt()
    }

    override fun getMaxX(): Int {
        if (!this.boundsCalculated) calculateBounds()
        return this.maxX.toInt()
    }

    override fun getMinY(): Int {
        if (!this.boundsCalculated) calculateBounds()
        return this.minY.toInt()
    }

    override fun getMaxY(): Int {
        if (!this.boundsCalculated) calculateBounds()
        return this.maxY.toInt()
    }

    override fun getMinZ(): Int {
        if (!this.boundsCalculated) calculateBounds()
        return this.minZ.toInt()
    }

    override fun getMaxZ(): Int {
        if (!this.boundsCalculated) calculateBounds()
        return this.maxZ.toInt()
    }

    override fun getFunctionMask(): Int = this.functionMask

    override fun getAmbient(): Int = this.ambient.toInt()

    override fun getContrast(): Int = this.contrast.toInt()

    override fun getAlpha(): ByteArray? = this.facesAlpha

    override fun getEmissiveTriangles(): Array<EmissiveTriangle>? = this.emissiveTriangles

    override fun getEffectiveVertices(): Array<EffectiveVertex>? = this.effectiveVertices

    override fun areTexturesCached(): Boolean {
        if (this.facesMaterial != null) {
            for (face in this.facesMaterial!!.indices) {
                if (this.facesMaterial!![face].toInt() != -1 && !this.toolkit.materialProvider!!.isCached(this.facesMaterial!![face].toInt())) {
                    return false
                }
            }
        }
        return true
    }

    override fun hasAnimatedTexture(): Boolean = this.textureAnimating

    override fun accountForLayers(): Boolean = this.hasAlphaTransition

    override fun xaxisRotateWithoutNormals(rotation: Int) {
        invalidateModernContent()
        invalidateModernBuilt(10)
        val sin = MathUtils.SINE[rotation]
        val cos = MathUtils.COSINE[rotation]

        for (i in 0 until this.maxDepth) {
            val y = (cos * this.verticesY!![i] - sin * this.verticesZ!![i]) shr 14
            this.verticesZ!![i] = (sin * this.verticesY!![i] + cos * this.verticesZ!![i]) shr 14
            this.verticesY!![i] = y
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun zaxisRotateWithoutNormals(rotation: Int) {
        invalidateModernContent()
        invalidateModernBuilt(11)
        val sin = MathUtils.SINE[rotation]
        val cos = MathUtils.COSINE[rotation]

        for (i in 0 until this.maxDepth) {
            val x = (cos * this.verticesX!![i] + sin * this.verticesY!![i]) shr 14
            this.verticesY!![i] = (cos * this.verticesY!![i] - sin * this.verticesX!![i]) shr 14
            this.verticesX!![i] = x
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun startAnimation(): Boolean {
        invalidateModernContent()
        if (this.vertexGroups == null) {
            return false
        }
        // Scale vertices up by 16 (shift left 4) for animation precision
        for (v in 0 until this.numVertices) {
            this.verticesX!![v] = this.verticesX!![v] shl 4
            this.verticesY!![v] = this.verticesY!![v] shl 4
            this.verticesZ!![v] = this.verticesZ!![v] shl 4
        }
        originX = 0
        originY = 0
        originZ = 0
        return true
    }

    override fun endAnimation() {
        invalidateModernContent()
        // Scale vertices back down by 16 (shift right 4) after animation
        // Adding 7 before shift provides proper rounding
        for (v in 0 until this.numVertices) {
            this.verticesX!![v] = this.verticesX!![v] + 7 shr 4
            this.verticesY!![v] = this.verticesY!![v] + 7 shr 4
            this.verticesZ!![v] = this.verticesZ!![v] + 7 shr 4
        }

        if (this.vertexBuffer != null) {
            this.vertexBuffer!!.buffer = null
        }

        this.boundsCalculated = false
    }

    override fun animatePartialtransform(transformationType: Int, labels: IntArray, xIn: Int, yIn: Int, zIn: Int, rotation: Int, lights: Boolean) {
        invalidateModernContent()
        var x = xIn
        var y = yIn
        var z = zIn
        val labelCount = labels.size

        if (transformationType == 0) {
            x = x shl 4
            y = y shl 4
            z = z shl 4
            var originVertexCount = 0
            originX = 0
            originY = 0
            originZ = 0

            for (labelIndex in 0 until labelCount) {
                val label = labels[labelIndex]
                if (label < (this.vertexGroups?.size ?: 0)) {
                    val vertexGroup = this.vertexGroups!![label]!!
                    for (i in vertexGroup.indices) {
                        val v = vertexGroup[i]
                        originX += this.verticesX!![v]
                        originY += this.verticesY!![v]
                        originZ += this.verticesZ!![v]
                        originVertexCount++
                    }
                }
            }

            if (originVertexCount > 0) {
                originX = x + originX / originVertexCount
                originY = y + originY / originVertexCount
                originZ = z + originZ / originVertexCount
            } else {
                originX = x
                originY = y
                originZ = z
            }
        } else if (transformationType == 1) {
            x = x shl 4
            y = y shl 4
            z = z shl 4

            for (labelIndex in 0 until labelCount) {
                val label = labels[labelIndex]
                if (label < (this.vertexGroups?.size ?: 0)) {
                    val vertexGroup = this.vertexGroups!![label]!!
                    for (i in vertexGroup.indices) {
                        val v = vertexGroup[i]
                        this.verticesX!![v] += x
                        this.verticesY!![v] += y
                        this.verticesZ!![v] += z
                    }
                }
            }
        } else if (transformationType == 2) {
            for (labelIndex in 0 until labelCount) {
                val label = labels[labelIndex]
                if (label < (this.vertexGroups?.size ?: 0)) {
                    val vertexGroup = this.vertexGroups!![label]!!
                    if ((rotation and 0x1) == 0) {
                        for (i in vertexGroup.indices) {
                            val v = vertexGroup[i]
                            this.verticesX!![v] -= originX
                            this.verticesY!![v] -= originY
                            this.verticesZ!![v] -= originZ
                            if (z != 0) {
                                val sin = MathUtils.SINE[z]
                                val cos = MathUtils.COSINE[z]
                                val tmp = ((sin * this.verticesY!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                                this.verticesY!![v] = ((cos * this.verticesY!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                                this.verticesX!![v] = tmp
                            }
                            if (x != 0) {
                                val sin = MathUtils.SINE[x]
                                val cos = MathUtils.COSINE[x]
                                val tmp = ((cos * this.verticesY!![v] - sin * this.verticesZ!![v] + 16383) shr 14)
                                this.verticesZ!![v] = ((sin * this.verticesY!![v] + cos * this.verticesZ!![v] + 16383) shr 14)
                                this.verticesY!![v] = tmp
                            }
                            if (y != 0) {
                                val sin = MathUtils.SINE[y]
                                val cos = MathUtils.COSINE[y]
                                val tmp = ((sin * this.verticesZ!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                                this.verticesZ!![v] = ((cos * this.verticesZ!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                                this.verticesX!![v] = tmp
                            }
                            this.verticesX!![v] += originX
                            this.verticesY!![v] += originY
                            this.verticesZ!![v] += originZ
                        }
                    } else {
                        for (i in vertexGroup.indices) {
                            val v = vertexGroup[i]
                            this.verticesX!![v] -= originX
                            this.verticesY!![v] -= originY
                            this.verticesZ!![v] -= originZ
                            if (x != 0) {
                                val sin = MathUtils.SINE[x]
                                val cos = MathUtils.COSINE[x]
                                val tmp = ((cos * this.verticesY!![v] - sin * this.verticesZ!![v] + 16383) shr 14)
                                this.verticesZ!![v] = ((sin * this.verticesY!![v] + cos * this.verticesZ!![v] + 16383) shr 14)
                                this.verticesY!![v] = tmp
                            }
                            if (z != 0) {
                                val sin = MathUtils.SINE[z]
                                val cos = MathUtils.COSINE[z]
                                val tmp = ((sin * this.verticesY!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                                this.verticesY!![v] = ((cos * this.verticesY!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                                this.verticesX!![v] = tmp
                            }
                            if (y != 0) {
                                val sin = MathUtils.SINE[y]
                                val cos = MathUtils.COSINE[y]
                                val tmp = ((sin * this.verticesZ!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                                this.verticesZ!![v] = ((cos * this.verticesZ!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                                this.verticesX!![v] = tmp
                            }
                            this.verticesX!![v] += originX
                            this.verticesY!![v] += originY
                            this.verticesZ!![v] += originZ
                        }
                    }
                }
            }

            if (lights) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < (this.vertexGroups?.size ?: 0)) {
                        val vertexGroup = this.vertexGroups!![label]!!
                        for (i in vertexGroup.indices) {
                            val v = vertexGroup[i]
                            val triStart = this.triangles!![v]
                            val triEnd = this.triangles!![v + 1]
                            for (triIdx in triStart until triEnd) {
                                val n = this.vertexNormalIndices!![triIdx].toInt() - 1
                                if (n == -1) break
                                if (z != 0) {
                                    val sin = MathUtils.SINE[z]
                                    val cos = MathUtils.COSINE[z]
                                    val tmp = ((this.normalsY!![n] * sin + this.normalsX!![n] * cos + 16383) shr 14)
                                    this.normalsY!![n] = (((this.normalsY!![n] * cos - this.normalsX!![n] * sin + 16383) shr 14)).toShort()
                                    this.normalsX!![n] = tmp.toShort()
                                }
                                if (x != 0) {
                                    val sin = MathUtils.SINE[x]
                                    val cos = MathUtils.COSINE[x]
                                    val tmp = ((this.normalsY!![n] * cos - this.normalsZ!![n] * sin + 16383) shr 14)
                                    this.normalsZ!![n] = (((this.normalsY!![n] * sin + this.normalsZ!![n] * cos + 16383) shr 14)).toShort()
                                    this.normalsY!![n] = tmp.toShort()
                                }
                                if (y != 0) {
                                    val sin = MathUtils.SINE[y]
                                    val cos = MathUtils.COSINE[y]
                                    val tmp = ((this.normalsZ!![n] * sin + this.normalsX!![n] * cos + 16383) shr 14)
                                    this.normalsZ!![n] = (((this.normalsZ!![n] * cos - this.normalsX!![n] * sin + 16383) shr 14)).toShort()
                                    this.normalsX!![n] = tmp.toShort()
                                }
                            }
                        }
                    }
                }

                if (this.normalsBuffer == null && this.colorsBuffer != null) {
                    this.colorsBuffer!!.buffer = null
                }
                if (this.normalsBuffer != null) {
                    this.normalsBuffer!!.buffer = null
                }
            }
        } else if (transformationType == 3) {
            for (labelIndex in 0 until labelCount) {
                val label = labels[labelIndex]
                if (label < (this.vertexGroups?.size ?: 0)) {
                    val vertexGroup = this.vertexGroups!![label]!!
                    for (i in vertexGroup.indices) {
                        val v = vertexGroup[i]
                        this.verticesX!![v] -= originX
                        this.verticesY!![v] -= originY
                        this.verticesZ!![v] -= originZ
                        this.verticesX!![v] = (x * this.verticesX!![v]) shr 7
                        this.verticesY!![v] = (y * this.verticesY!![v]) shr 7
                        this.verticesZ!![v] = (z * this.verticesZ!![v]) shr 7
                        this.verticesX!![v] += originX
                        this.verticesY!![v] += originY
                        this.verticesZ!![v] += originZ
                    }
                }
            }
        } else if (transformationType == 5) {
            if (this.faceGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.faceGroups!!.size) {
                        val faceGroup = this.faceGroups!![label]!!
                        for (i in faceGroup.indices) {
                            val face = faceGroup[i]
                            var alpha = (this.facesAlpha!![face].toInt() and 0xff) + x * 8
                            if (alpha < 0) alpha = 0
                            else if (alpha > 255) alpha = 255
                            this.facesAlpha!![face] = alpha.toByte()
                        }
                        if (faceGroup.isNotEmpty() && this.colorsBuffer != null) {
                            this.colorsBuffer!!.buffer = null
                        }
                    }
                }
                if (this.billboardEffects != null) {
                    for (b in 0 until this.billboardCount) {
                        val effect = this.billboardEffects!![b]!!
                        val surface = this.billboardSurfaces!![b]!!
                        surface.diffuseColor = (surface.diffuseColor and 0xffffff) or ((255 - (this.facesAlpha!![effect.appliesOnFace].toInt() and 0xff)) shl 24)
                    }
                }
            }
        } else if (transformationType == 7) {
            if (this.faceGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.faceGroups!!.size) {
                        val faceGroup = this.faceGroups!![label]!!
                        for (i in faceGroup.indices) {
                            val face = faceGroup[i]
                            val hsl = this.facesColor!![face].toInt() and 0xffff
                            var h = (hsl shr 10) and 0x3f
                            var s = (hsl shr 7) and 0x7
                            var l = hsl and 0x7f
                            h = (x + h) and 0x3f
                            s += y / 4
                            if (s < 0) s = 0
                            else if (s > 7) s = 7
                            l += z
                            if (l < 0) l = 0
                            else if (l > 127) l = 127
                            this.facesColor!![face] = ((h shl 10) or (s shl 7) or l).toShort()
                        }
                        if (faceGroup.isNotEmpty() && this.colorsBuffer != null) {
                            this.colorsBuffer!!.buffer = null
                        }
                    }
                }
                if (this.billboardEffects != null) {
                    for (b in 0 until this.billboardCount) {
                        val effect = this.billboardEffects!![b]!!
                        val surface = this.billboardSurfaces!![b]!!
                        surface.diffuseColor = (surface.diffuseColor and -16777216) or (ColorUtil.hslToRgb!![this.facesColor!![effect.appliesOnFace].toInt() and 0xffff] and 0xffffff)
                    }
                }
            }
        } else if (transformationType == 8) {
            if (this.billboardGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.billboardGroups!!.size) {
                        val billboardGroup = this.billboardGroups!![label]!!
                        for (i in billboardGroup.indices) {
                            val surface = this.billboardSurfaces!![billboardGroup[i]]!!
                            surface.offsetX += x
                            surface.offsetY += y
                        }
                    }
                }
            }
        } else if (transformationType == 9) {
            if (this.billboardGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.billboardGroups!!.size) {
                        val billboardGroup = this.billboardGroups!![label]!!
                        for (i in billboardGroup.indices) {
                            val surface = this.billboardSurfaces!![billboardGroup[i]]!!
                            surface.rotation = (x + surface.rotation) and 0x3fff
                        }
                    }
                }
            }
        } else if (transformationType == 10) {
            if (this.billboardGroups != null) {
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < this.billboardGroups!!.size) {
                        val billboardGroup = this.billboardGroups!![label]!!
                        for (i in billboardGroup.indices) {
                            val surface = this.billboardSurfaces!![billboardGroup[i]]!!
                            surface.scaleX = (x * surface.scaleX) shr 7
                            surface.scaleY = (y * surface.scaleY) shr 7
                        }
                    }
                }
            }
        }
    }

    override fun animateTransform(transformationType: Int, labels: IntArray, xIn: Int, yIn: Int, zIn: Int, lights: Boolean, submesh: Int, verticesData: IntArray) {
        invalidateModernContent()
        var x = xIn
        var y = yIn
        var z = zIn
        val labelCount = labels.size

        if (transformationType == 0) {
            x = x shl 4
            y = y shl 4
            z = z shl 4
            var originVertexCount = 0
            originX = 0
            originY = 0
            originZ = 0

            for (labelIndex in 0 until labelCount) {
                val label = labels[labelIndex]
                if (label < (this.vertexGroups?.size ?: 0)) {
                    val vertexGroup = this.vertexGroups!![label]!!
                    for (i in vertexGroup.indices) {
                        val v = vertexGroup[i]
                        if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![v].toInt()) != 0) {
                            originX += this.verticesX!![v]
                            originY += this.verticesY!![v]
                            originZ += this.verticesZ!![v]
                            originVertexCount++
                        }
                    }
                }
            }

            if (originVertexCount > 0) {
                originX = x + originX / originVertexCount
                originY = y + originY / originVertexCount
                originZ = z + originZ / originVertexCount
                withinOrigin = true
            } else {
                originX = x
                originY = y
                originZ = z
            }
        } else if (transformationType == 1) {
            if (verticesData.isNotEmpty()) {
                var transformedX = ((z * verticesData[2] + verticesData[1] * y + x * verticesData[0] + 8192) shr 14)
                var transformedY = ((x * verticesData[3] + z * verticesData[5] + y * verticesData[4] + 8192) shr 14)
                var transformedZ = ((x * verticesData[6] + verticesData[7] * y + z * verticesData[8] + 8192) shr 14)
                x = transformedX
                y = transformedY
                z = transformedZ
            }

            x = x shl 4
            y = y shl 4
            z = z shl 4

            for (labelIndex in 0 until labelCount) {
                val label = labels[labelIndex]
                if (label < (this.vertexGroups?.size ?: 0)) {
                    val vertexGroup = this.vertexGroups!![label]!!
                    for (i in vertexGroup.indices) {
                        val v = vertexGroup[i]
                        if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![v].toInt()) != 0) {
                            this.verticesX!![v] += x
                            this.verticesY!![v] += y
                            this.verticesZ!![v] += z
                        }
                    }
                }
            }
        } else if (transformationType == 2) {
            if (verticesData.isNotEmpty()) {
                val parentOriginX = verticesData[9] shl 4
                val parentOriginY = verticesData[10] shl 4
                val parentOriginZ = verticesData[11] shl 4
                val parentTranslateX = verticesData[12] shl 4
                val parentTranslateY = verticesData[13] shl 4
                val parentTranslateZ = verticesData[14] shl 4
                if (withinOrigin) {
                    var txOriginX = ((verticesData[0] * originX + verticesData[6] * originZ + verticesData[3] * originY + 8192) shr 14)
                    var txOriginY = ((verticesData[7] * originZ + verticesData[4] * originY + verticesData[1] * originX + 8192) shr 14)
                    var txOriginZ = ((verticesData[8] * originZ + verticesData[2] * originX + verticesData[5] * originY + 8192) shr 14)
                    txOriginX += parentTranslateX
                    txOriginY += parentTranslateY
                    txOriginZ += parentTranslateZ
                    originX = txOriginX
                    originY = txOriginY
                    originZ = txOriginZ
                    withinOrigin = false
                }

                val rotationMatrix = this.animScratchMatrixA ?: IntArray(9).also { this.animScratchMatrixA = it }
                val cosX = MathUtils.COSINE[x]
                val sinX = MathUtils.SINE[x]
                val cosY = MathUtils.COSINE[y]
                val sinY = MathUtils.SINE[y]
                val cosZ = MathUtils.COSINE[z]
                val sinZ = MathUtils.SINE[z]
                val sinXcosZ = ((sinX * cosZ + 8192) shr 14)
                val sinXsinZ = ((sinX * sinZ + 8192) shr 14)
                rotationMatrix[0] = ((cosY * cosZ + sinY * sinXsinZ + 8192) shr 14)
                rotationMatrix[1] = ((sinY * sinXcosZ + -cosY * sinZ + 8192) shr 14)
                rotationMatrix[2] = ((sinY * cosX + 8192) shr 14)
                rotationMatrix[3] = ((cosX * sinZ + 8192) shr 14)
                rotationMatrix[4] = ((cosX * cosZ + 8192) shr 14)
                rotationMatrix[5] = -sinX
                rotationMatrix[6] = ((cosY * sinXsinZ + -sinY * cosZ + 8192) shr 14)
                rotationMatrix[7] = ((sinY * sinZ + cosY * sinXcosZ + 8192) shr 14)
                rotationMatrix[8] = ((cosY * cosX + 8192) shr 14)
                val invOriginRotX = ((rotationMatrix[1] * -originY + rotationMatrix[0] * -originX + rotationMatrix[2] * -originZ + 8192) shr 14)
                val invOriginRotY = ((-originY * rotationMatrix[4] + rotationMatrix[3] * -originX + -originZ * rotationMatrix[5] + 8192) shr 14)
                val invOriginRotZ = ((rotationMatrix[7] * -originY + rotationMatrix[6] * -originX + rotationMatrix[8] * -originZ + 8192) shr 14)
                val rotOriginOffsetX = invOriginRotX + originX
                val rotOriginOffsetY = invOriginRotY + originY
                val rotOriginOffsetZ = invOriginRotZ + originZ
                val combinedRotMatrix = this.animScratchMatrixB ?: IntArray(9).also { this.animScratchMatrixB = it }

                for (rowIdx in 0 until 3) {
                    for (colIdx in 0 until 3) {
                        var dotProduct = 0
                        for (innerIdx in 0 until 3) {
                            dotProduct += verticesData[innerIdx + colIdx * 3] * rotationMatrix[innerIdx + rowIdx * 3]
                        }
                        combinedRotMatrix[colIdx + rowIdx * 3] = ((dotProduct + 8192) shr 14)
                    }
                }

                var translatedX = ((parentTranslateY * rotationMatrix[1] + parentTranslateX * rotationMatrix[0] + parentTranslateZ * rotationMatrix[2] + 8192) shr 14)
                var translatedY = ((parentTranslateY * rotationMatrix[4] + parentTranslateX * rotationMatrix[3] + parentTranslateZ * rotationMatrix[5] + 8192) shr 14)
                var translatedZ = ((rotationMatrix[6] * parentTranslateX + parentTranslateZ * rotationMatrix[8] + rotationMatrix[7] * parentTranslateY + 8192) shr 14)
                translatedX += rotOriginOffsetX
                translatedY += rotOriginOffsetY
                translatedZ += rotOriginOffsetZ
                val finalMatrix = this.animScratchMatrixC ?: IntArray(9).also { this.animScratchMatrixC = it }

                for (rowIdx in 0 until 3) {
                    for (colIdx in 0 until 3) {
                        var dotProduct = 0
                        for (innerIdx in 0 until 3) {
                            dotProduct += combinedRotMatrix[colIdx + innerIdx * 3] * verticesData[innerIdx + rowIdx * 3]
                        }
                        finalMatrix[colIdx + rowIdx * 3] = ((dotProduct + 8192) shr 14)
                    }
                }

                var finalTranslateX = ((translatedZ * verticesData[2] + translatedX * verticesData[0] + translatedY * verticesData[1] + 8192) shr 14)
                var finalTranslateY = ((translatedX * verticesData[3] + translatedY * verticesData[4] + verticesData[5] * translatedZ + 8192) shr 14)
                var finalTranslateZ = ((translatedX * verticesData[6] + translatedZ * verticesData[8] + verticesData[7] * translatedY + 8192) shr 14)
                finalTranslateX += parentOriginX
                finalTranslateY += parentOriginY
                finalTranslateZ += parentOriginZ

                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < (this.vertexGroups?.size ?: 0)) {
                        val vertexGroup = this.vertexGroups!![label]!!
                        for (i in vertexGroup.indices) {
                            val v = vertexGroup[i]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![v].toInt()) != 0) {
                                var newX = ((finalMatrix[0] * this.verticesX!![v] + finalMatrix[2] * this.verticesZ!![v] + finalMatrix[1] * this.verticesY!![v] + 8192) shr 14)
                                var newY = ((finalMatrix[4] * this.verticesY!![v] + finalMatrix[5] * this.verticesZ!![v] + finalMatrix[3] * this.verticesX!![v] + 8192) shr 14)
                                var newZ = ((finalMatrix[6] * this.verticesX!![v] + finalMatrix[7] * this.verticesY!![v] + finalMatrix[8] * this.verticesZ!![v] + 8192) shr 14)
                                newX += finalTranslateX
                                newY += finalTranslateY
                                newZ += finalTranslateZ
                                this.verticesX!![v] = newX
                                this.verticesY!![v] = newY
                                this.verticesZ!![v] = newZ
                            }
                        }
                    }
                }
            } else {
                // No verticesData - simple rotation
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < (this.vertexGroups?.size ?: 0)) {
                        val vertexGroup = this.vertexGroups!![label]!!
                        for (i in vertexGroup.indices) {
                            val v = vertexGroup[i]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![v].toInt()) != 0) {
                                this.verticesX!![v] -= originX
                                this.verticesY!![v] -= originY
                                this.verticesZ!![v] -= originZ
                                if (z != 0) {
                                    val sin = MathUtils.SINE[z]
                                    val cos = MathUtils.COSINE[z]
                                    val tmp = ((sin * this.verticesY!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                                    this.verticesY!![v] = ((cos * this.verticesY!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                                    this.verticesX!![v] = tmp
                                }
                                if (x != 0) {
                                    val sin = MathUtils.SINE[x]
                                    val cos = MathUtils.COSINE[x]
                                    val tmp = ((cos * this.verticesY!![v] - sin * this.verticesZ!![v] + 16383) shr 14)
                                    this.verticesZ!![v] = ((sin * this.verticesY!![v] + cos * this.verticesZ!![v] + 16383) shr 14)
                                    this.verticesY!![v] = tmp
                                }
                                if (y != 0) {
                                    val sin = MathUtils.SINE[y]
                                    val cos = MathUtils.COSINE[y]
                                    val tmp = ((sin * this.verticesZ!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                                    this.verticesZ!![v] = ((cos * this.verticesZ!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                                    this.verticesX!![v] = tmp
                                }
                                this.verticesX!![v] += originX
                                this.verticesY!![v] += originY
                                this.verticesZ!![v] += originZ
                            }
                        }
                    }
                }

                if (lights) {
                    for (labelIndex in 0 until labelCount) {
                        val label = labels[labelIndex]
                        if (label < (this.vertexGroups?.size ?: 0)) {
                            val vertexGroup = this.vertexGroups!![label]!!
                            for (i in vertexGroup.indices) {
                                val v = vertexGroup[i]
                                if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![v].toInt()) != 0) {
                                    val triStart = this.triangles!![v]
                                    val triEnd = this.triangles!![v + 1]
                                    for (triIdx in triStart until triEnd) {
                                        val normalIdx = this.vertexNormalIndices!![triIdx].toInt() - 1
                                        if (normalIdx == -1) break
                                        if (z != 0) {
                                            val sin = MathUtils.SINE[z]
                                            val cos = MathUtils.COSINE[z]
                                            val tmp = ((this.normalsY!![normalIdx] * sin + this.normalsX!![normalIdx] * cos + 16383) shr 14)
                                            this.normalsY!![normalIdx] = (((this.normalsY!![normalIdx] * cos - this.normalsX!![normalIdx] * sin + 16383) shr 14)).toShort()
                                            this.normalsX!![normalIdx] = tmp.toShort()
                                        }
                                        if (x != 0) {
                                            val sin = MathUtils.SINE[x]
                                            val cos = MathUtils.COSINE[x]
                                            val tmp = ((this.normalsY!![normalIdx] * cos - this.normalsZ!![normalIdx] * sin + 16383) shr 14)
                                            this.normalsZ!![normalIdx] = (((this.normalsY!![normalIdx] * sin + this.normalsZ!![normalIdx] * cos + 16383) shr 14)).toShort()
                                            this.normalsY!![normalIdx] = tmp.toShort()
                                        }
                                        if (y != 0) {
                                            val sin = MathUtils.SINE[y]
                                            val cos = MathUtils.COSINE[y]
                                            val tmp = ((this.normalsZ!![normalIdx] * sin + this.normalsX!![normalIdx] * cos + 16383) shr 14)
                                            this.normalsZ!![normalIdx] = (((this.normalsZ!![normalIdx] * cos - this.normalsX!![normalIdx] * sin + 16383) shr 14)).toShort()
                                            this.normalsX!![normalIdx] = tmp.toShort()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (this.normalsBuffer == null && this.colorsBuffer != null) {
                        this.colorsBuffer!!.buffer = null
                    }
                    if (this.normalsBuffer != null) {
                        this.normalsBuffer!!.buffer = null
                    }
                }
            }
        } else if (transformationType == 3) {
            if (verticesData.isNotEmpty()) {
                val parentOriginX = verticesData[9] shl 4
                val parentOriginY = verticesData[10] shl 4
                val parentOriginZ = verticesData[11] shl 4
                val parentTranslateX = verticesData[12] shl 4
                val parentTranslateY = verticesData[13] shl 4
                val parentTranslateZ = verticesData[14] shl 4
                if (withinOrigin) {
                    var txOriginX = ((verticesData[0] * originX + verticesData[6] * originZ + verticesData[3] * originY + 8192) shr 14)
                    var txOriginY = ((verticesData[7] * originZ + verticesData[4] * originY + verticesData[1] * originX + 8192) shr 14)
                    var txOriginZ = ((verticesData[8] * originZ + verticesData[2] * originX + verticesData[5] * originY + 8192) shr 14)
                    txOriginX += parentTranslateX
                    txOriginY += parentTranslateY
                    txOriginZ += parentTranslateZ
                    originX = txOriginX
                    originY = txOriginY
                    originZ = txOriginZ
                    withinOrigin = false
                }

                val scaleX = (x shl 15) shr 7
                val scaleY = (y shl 15) shr 7
                val scaleZ = (z shl 15) shr 7
                val scaledInvOriginX = ((scaleX * -originX + 8192) shr 14)
                val scaledInvOriginY = ((scaleY * -originY + 8192) shr 14)
                val scaledInvOriginZ = ((scaleZ * -originZ + 8192) shr 14)
                val scaleOriginOffsetX = scaledInvOriginX + originX
                val scaleOriginOffsetY = scaledInvOriginY + originY
                val scaleOriginOffsetZ = scaledInvOriginZ + originZ
                val scaleMatrix = this.animScratchMatrixA ?: IntArray(9).also { this.animScratchMatrixA = it }
                scaleMatrix[0] = (scaleX * verticesData[0] + 8192) shr 14
                scaleMatrix[1] = (scaleX * verticesData[3] + 8192) shr 14
                scaleMatrix[2] = (scaleX * verticesData[6] + 8192) shr 14
                scaleMatrix[3] = (scaleY * verticesData[1] + 8192) shr 14
                scaleMatrix[4] = (scaleY * verticesData[4] + 8192) shr 14
                scaleMatrix[5] = (verticesData[7] * scaleY + 8192) shr 14
                scaleMatrix[6] = (scaleZ * verticesData[2] + 8192) shr 14
                scaleMatrix[7] = (verticesData[5] * scaleZ + 8192) shr 14
                scaleMatrix[8] = (scaleZ * verticesData[8] + 8192) shr 14
                var scaledTranslateX = ((scaleX * parentTranslateX + 8192) shr 14)
                var scaledTranslateY = ((scaleY * parentTranslateY + 8192) shr 14)
                var scaledTranslateZ = ((scaleZ * parentTranslateZ + 8192) shr 14)
                scaledTranslateX += scaleOriginOffsetX
                scaledTranslateY += scaleOriginOffsetY
                scaledTranslateZ += scaleOriginOffsetZ
                val combinedScaleMatrix = this.animScratchMatrixB ?: IntArray(9).also { this.animScratchMatrixB = it }

                for (rowIdx in 0 until 3) {
                    for (colIdx in 0 until 3) {
                        var dotProduct = 0
                        for (innerIdx in 0 until 3) {
                            dotProduct += scaleMatrix[colIdx + innerIdx * 3] * verticesData[innerIdx + rowIdx * 3]
                        }
                        combinedScaleMatrix[colIdx + rowIdx * 3] = ((dotProduct + 8192) shr 14)
                    }
                }

                var finalTranslateX = ((scaledTranslateZ * verticesData[2] + scaledTranslateX * verticesData[0] + scaledTranslateY * verticesData[1] + 8192) shr 14)
                var finalTranslateY = ((scaledTranslateX * verticesData[3] + scaledTranslateY * verticesData[4] + verticesData[5] * scaledTranslateZ + 8192) shr 14)
                var finalTranslateZ = ((scaledTranslateX * verticesData[6] + scaledTranslateZ * verticesData[8] + verticesData[7] * scaledTranslateY + 8192) shr 14)
                finalTranslateX += parentOriginX
                finalTranslateY += parentOriginY
                finalTranslateZ += parentOriginZ

                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < (this.vertexGroups?.size ?: 0)) {
                        val vertexGroup = this.vertexGroups!![label]!!
                        for (i in vertexGroup.indices) {
                            val v = vertexGroup[i]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![v].toInt()) != 0) {
                                var newX = ((combinedScaleMatrix[0] * this.verticesX!![v] + combinedScaleMatrix[2] * this.verticesZ!![v] + combinedScaleMatrix[1] * this.verticesY!![v] + 8192) shr 14)
                                var newY = ((combinedScaleMatrix[4] * this.verticesY!![v] + combinedScaleMatrix[5] * this.verticesZ!![v] + combinedScaleMatrix[3] * this.verticesX!![v] + 8192) shr 14)
                                var newZ = ((combinedScaleMatrix[6] * this.verticesX!![v] + combinedScaleMatrix[7] * this.verticesY!![v] + combinedScaleMatrix[8] * this.verticesZ!![v] + 8192) shr 14)
                                newX += finalTranslateX
                                newY += finalTranslateY
                                newZ += finalTranslateZ
                                this.verticesX!![v] = newX
                                this.verticesY!![v] = newY
                                this.verticesZ!![v] = newZ
                            }
                        }
                    }
                }
            } else {
                // No verticesData - simple scale
                for (labelIndex in 0 until labelCount) {
                    val label = labels[labelIndex]
                    if (label < (this.vertexGroups?.size ?: 0)) {
                        val vertexGroup = this.vertexGroups!![label]!!
                        for (i in vertexGroup.indices) {
                            val v = vertexGroup[i]
                            if (this.verticesSubmeshes == null || (submesh and this.verticesSubmeshes!![v].toInt()) != 0) {
                                this.verticesX!![v] -= originX
                                this.verticesY!![v] -= originY
                                this.verticesZ!![v] -= originZ
                                this.verticesX!![v] = (x * this.verticesX!![v]) shr 7
                                this.verticesY!![v] = (y * this.verticesY!![v]) shr 7
                                this.verticesZ!![v] = (z * this.verticesZ!![v]) shr 7
                                this.verticesX!![v] += originX
                                this.verticesY!![v] += originY
                                this.verticesZ!![v] += originZ
                            }
                        }
                    }
                }
            }
        } else {
            // Transform types 5, 7, 8, 9, 10 - face/billboard transforms
            if (transformationType == 5) {
                // Alpha transform
                if (this.faceGroups != null) {
                    for (labelIndex in 0 until labelCount) {
                        val label = labels[labelIndex]
                        if (label < this.faceGroups!!.size) {
                            val faceGroup = this.faceGroups!![label]!!
                            for (i in faceGroup.indices) {
                                val face = faceGroup[i]
                                if (this.facesSubmeshes == null || (submesh and this.facesSubmeshes!![face].toInt()) != 0) {
                                    var alpha = (this.facesAlpha!![face].toInt() and 0xff) + x * 8
                                    if (alpha < 0) alpha = 0
                                    else if (alpha > 255) alpha = 255
                                    this.facesAlpha!![face] = alpha.toByte()
                                    if (this.colorsBuffer != null) {
                                        this.colorsBuffer!!.buffer = null
                                    }
                                }
                            }
                        }
                    }

                    if (this.billboardEffects != null) {
                        for (b in 0 until this.billboardCount) {
                            val effect = this.billboardEffects!![b]!!
                            val surface = this.billboardSurfaces!![b]!!
                            surface.diffuseColor = (surface.diffuseColor and 0xffffff) or ((255 - (this.facesAlpha!![effect.appliesOnFace].toInt() and 0xff)) shl 24)
                        }
                    }
                }
            } else if (transformationType == 7) {
                // Color transform
                if (this.faceGroups != null) {
                    for (labelIndex in 0 until labelCount) {
                        val label = labels[labelIndex]
                        if (label < this.faceGroups!!.size) {
                            val faceGroup = this.faceGroups!![label]!!
                            for (i in faceGroup.indices) {
                                val face = faceGroup[i]
                                if (this.facesSubmeshes == null || (submesh and this.facesSubmeshes!![face].toInt()) != 0) {
                                    val hsl = this.facesColor!![face].toInt() and 0xffff
                                    var h = (hsl shr 10) and 0x3f
                                    var s = (hsl shr 7) and 0x7
                                    var l = hsl and 0x7f
                                    h = (x + h) and 0x3f
                                    s += y / 4
                                    if (s < 0) s = 0
                                    else if (s > 7) s = 7
                                    l += z
                                    if (l < 0) l = 0
                                    else if (l > 127) l = 127
                                    this.facesColor!![face] = ((h shl 10) or (s shl 7) or l).toShort()
                                    if (this.colorsBuffer != null) {
                                        this.colorsBuffer!!.buffer = null
                                    }
                                }
                            }
                        }
                    }

                    if (this.billboardEffects != null) {
                        for (b in 0 until this.billboardCount) {
                            val effect = this.billboardEffects!![b]!!
                            val surface = this.billboardSurfaces!![b]!!
                            surface.diffuseColor = (surface.diffuseColor and -16777216) or (ColorUtil.hslToRgb!![this.facesColor!![effect.appliesOnFace].toInt() and 0xffff] and 0xffffff)
                        }
                    }
                }
            } else {
                if (transformationType == 8) {
                    // Billboard offset transform
                    if (this.billboardGroups != null) {
                        for (labelIndex in 0 until labelCount) {
                            val label = labels[labelIndex]
                            if (label < this.billboardGroups!!.size) {
                                val billboardGroup = this.billboardGroups!![label]!!
                                for (i in billboardGroup.indices) {
                                    val surface = this.billboardSurfaces!![billboardGroup[i]]!!
                                    surface.offsetX += x
                                    surface.offsetY += y
                                }
                            }
                        }
                    }
                } else if (transformationType == 10) {
                    // Billboard scale transform
                    if (this.billboardGroups != null) {
                        for (labelIndex in 0 until labelCount) {
                            val label = labels[labelIndex]
                            if (label < this.billboardGroups!!.size) {
                                val billboardGroup = this.billboardGroups!![label]!!
                                for (i in billboardGroup.indices) {
                                    val surface = this.billboardSurfaces!![billboardGroup[i]]!!
                                    surface.scaleX = (x * surface.scaleX) shr 7
                                    surface.scaleY = (y * surface.scaleY) shr 7
                                }
                            }
                        }
                    }
                } else if (transformationType == 9 && this.billboardGroups != null) {
                    // Billboard rotation transform
                    for (labelIndex in 0 until labelCount) {
                        val label = labels[labelIndex]
                        if (label < this.billboardGroups!!.size) {
                            val billboardGroup = this.billboardGroups!![label]!!
                            for (i in billboardGroup.indices) {
                                val surface = this.billboardSurfaces!![billboardGroup[i]]!!
                                surface.rotation = (x + surface.rotation) and 0x3fff
                            }
                        }
                    }
                }
            }
        }
    }

    override fun animateShadowtransform(transformType: Int, x: Int, y: Int, z: Int) {
        invalidateModernContent()
        if (transformType == 0) {
            var originVertexCount = 0
            originX = 0
            originY = 0
            originZ = 0
            for (v in 0 until this.maxDepth) {
                originX += this.verticesX!![v]
                originY += this.verticesY!![v]
                originZ += this.verticesZ!![v]
                originVertexCount++
            }
            if (originVertexCount > 0) {
                originX = x + originX / originVertexCount
                originY = y + originY / originVertexCount
                originZ = z + originZ / originVertexCount
            } else {
                originX = x
                originY = y
                originZ = z
            }
        } else if (transformType == 1) {
            for (v in 0 until this.maxDepth) {
                this.verticesX!![v] += x
                this.verticesY!![v] += y
                this.verticesZ!![v] += z
            }
        } else if (transformType == 2) {
            for (v in 0 until this.maxDepth) {
                this.verticesX!![v] -= originX
                this.verticesY!![v] -= originY
                this.verticesZ!![v] -= originZ
                if (z != 0) {
                    val sin = MathUtils.SINE[z]
                    val cos = MathUtils.COSINE[z]
                    val tmp = ((sin * this.verticesY!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                    this.verticesY!![v] = ((cos * this.verticesY!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                    this.verticesX!![v] = tmp
                }
                if (x != 0) {
                    val sin = MathUtils.SINE[x]
                    val cos = MathUtils.COSINE[x]
                    val tmp = ((cos * this.verticesY!![v] - sin * this.verticesZ!![v] + 16383) shr 14)
                    this.verticesZ!![v] = ((sin * this.verticesY!![v] + cos * this.verticesZ!![v] + 16383) shr 14)
                    this.verticesY!![v] = tmp
                }
                if (y != 0) {
                    val sin = MathUtils.SINE[y]
                    val cos = MathUtils.COSINE[y]
                    val tmp = ((sin * this.verticesZ!![v] + cos * this.verticesX!![v] + 16383) shr 14)
                    this.verticesZ!![v] = ((cos * this.verticesZ!![v] - sin * this.verticesX!![v] + 16383) shr 14)
                    this.verticesX!![v] = tmp
                }
                this.verticesX!![v] += originX
                this.verticesY!![v] += originY
                this.verticesZ!![v] += originZ
            }
        } else if (transformType == 3) {
            for (v in 0 until this.maxDepth) {
                this.verticesX!![v] -= originX
                this.verticesY!![v] -= originY
                this.verticesZ!![v] -= originZ
                this.verticesX!![v] = x * this.verticesX!![v] / 128
                this.verticesY!![v] = y * this.verticesY!![v] / 128
                this.verticesZ!![v] = z * this.verticesZ!![v] / 128
                this.verticesX!![v] += originX
                this.verticesY!![v] += originY
                this.verticesZ!![v] += originZ
            }
        } else if (transformType == 5) {
            for (f in 0 until this.numFaces) {
                var alpha = (this.facesAlpha!![f].toInt() and 0xff) + x * 8
                if (alpha < 0) alpha = 0
                else if (alpha > 255) alpha = 255
                this.facesAlpha!![f] = alpha.toByte()
            }
            if (this.colorsBuffer != null) {
                this.colorsBuffer!!.buffer = null
            }
            if (this.billboardEffects != null) {
                for (b in 0 until this.billboardCount) {
                    val effect = this.billboardEffects!![b]!!
                    val surface = this.billboardSurfaces!![b]!!
                    surface.diffuseColor = (surface.diffuseColor and 0xffffff) or ((255 - (this.facesAlpha!![effect.appliesOnFace].toInt() and 0xff)) shl 24)
                }
            }
        } else if (transformType == 7) {
            for (f in 0 until this.numFaces) {
                val hsl = this.facesColor!![f].toInt() and 0xffff
                var h = (hsl shr 10) and 0x3f
                var s = (hsl shr 7) and 0x7
                var l = hsl and 0x7f
                h = (h + x) and 0x3f
                s += y / 4
                if (s < 0) s = 0
                else if (s > 7) s = 7
                l += z
                if (l < 0) l = 0
                else if (l > 127) l = 127
                this.facesColor!![f] = ((h shl 10) or (s shl 7) or l).toShort()
            }
            if (this.colorsBuffer != null) {
                this.colorsBuffer!!.buffer = null
            }
            if (this.billboardEffects != null) {
                for (b in 0 until this.billboardCount) {
                    val effect = this.billboardEffects!![b]!!
                    val surface = this.billboardSurfaces!![b]!!
                    surface.diffuseColor = (surface.diffuseColor and -16777216) or (ColorUtil.hslToRgb!![this.facesColor!![effect.appliesOnFace].toInt() and 0xffff] and 0xffffff)
                }
            }
        } else if (transformType == 8) {
            for (b in 0 until this.billboardCount) {
                val surface = this.billboardSurfaces!![b]!!
                surface.offsetX += x
                surface.offsetY += y
            }
        } else if (transformType == 9) {
            for (b in 0 until this.billboardCount) {
                val surface = this.billboardSurfaces!![b]!!
                surface.rotation = (x + surface.rotation) and 0x3fff
            }
        } else if (transformType == 10) {
            for (b in 0 until this.billboardCount) {
                val surface = this.billboardSurfaces!![b]!!
                surface.scaleX = (x * surface.scaleX) shr 7
                surface.scaleY = (y * surface.scaleY) shr 7
            }
        }
    }

    override fun contourToGround(contourType: Int, contourModifier: Int, currentLvl: Ground?, nextLvl: Ground?, sceneX: Int, averageHeight: Int, sceneY: Int) {
        invalidateModernContent()
        // Ground contouring reshapes vertices per-tile; not matrix-representable.
        invalidateModernBuilt(12)
        if (!this.boundsCalculated) {
            calculateBounds()
        }

        var minTileX = sceneX + this.minX
        var maxTileX = sceneX + this.maxX
        var minTileY = sceneY + this.minZ
        var maxTileY = sceneY + this.maxZ
        if (contourType != 1 && contourType != 2 && contourType != 3 && contourType != 5 || minTileX >= 0 && (maxTileX + currentLvl!!.tileUnits shr currentLvl.tileScale) < currentLvl.width && minTileY >= 0 && (maxTileY + currentLvl.tileUnits shr currentLvl.tileScale) < currentLvl.length) {
            if (contourType != 4 && contourType != 5) {
                minTileX = minTileX shr currentLvl!!.tileScale
                maxTileX = (maxTileX + (currentLvl.tileUnits - 1)) shr currentLvl.tileScale
                minTileY = minTileY shr currentLvl.tileScale
                maxTileY = (maxTileY + (currentLvl.tileUnits - 1)) shr currentLvl.tileScale
                if (currentLvl.getHeight(minTileX, minTileY) == averageHeight && currentLvl.getHeight(maxTileX, minTileY) == averageHeight && currentLvl.getHeight(minTileX, maxTileY) == averageHeight && currentLvl.getHeight(maxTileX, maxTileY) == averageHeight) {
                    return
                }
            } else if (nextLvl == null || minTileX < 0 || (nextLvl.tileUnits + maxTileX shr nextLvl.tileScale) >= nextLvl.width || minTileY < 0 || (nextLvl.tileUnits + maxTileY shr nextLvl.tileScale) >= nextLvl.length) {
                return
            }

            if (contourType == 1) {
                for (v in 0 until this.maxDepth) {
                    this.verticesY!![v] = this.verticesY!![v] + currentLvl!!.getAverageHeight(this.verticesX!![v] + sceneX, this.verticesZ!![v] + sceneY) - averageHeight
                }
            } else if (contourType == 2) {
                val minYVal = this.minY
                if (minYVal.toInt() == 0) {
                    return
                }

                for (v in 0 until this.maxDepth) {
                    val heightRatio = (this.verticesY!![v] shl 16) / minYVal
                    if (heightRatio < contourModifier) {
                        this.verticesY!![v] += (currentLvl!!.getAverageHeight(this.verticesX!![v] + sceneX, this.verticesZ!![v] + sceneY) - averageHeight) * (contourModifier - heightRatio) / contourModifier
                    }
                }
            } else if (contourType == 3) {
                val width = (contourModifier and 0xff) * 4
                val length = ((contourModifier shr 8) and 0xff) * 4
                val xan = ((contourModifier shr 16) and 0xff) shl 6
                val zan = ((contourModifier shr 24) and 0xff) shl 6
                if (sceneX - (width shr 1) < 0 || sceneX + currentLvl!!.tileUnits + (width shr 1) >= currentLvl.width shl currentLvl.tileScale || sceneY - (length shr 1) < 0 || currentLvl.tileUnits + (length shr 1) + sceneY >= currentLvl.length shl currentLvl.tileScale) {
                    return
                }

                invalidateModernBuilt(13)
                hillChange(currentLvl, sceneX, averageHeight, sceneY, width, length, xan, zan)
            } else if (contourType == 4) {
                val sizeY = this.maxY - this.minY

                for (v in 0 until this.maxDepth) {
                    this.verticesY!![v] += (nextLvl!!.getAverageHeight(this.verticesX!![v] + sceneX, this.verticesZ!![v] + sceneY) - averageHeight) + sizeY
                }
            } else if (contourType == 5) {
                val sizeY = this.maxY - this.minY

                for (v in 0 until this.maxDepth) {
                    val x = sceneX + this.verticesX!![v]
                    val z = sceneY + this.verticesZ!![v]
                    val currentAvgHeight = currentLvl!!.getAverageHeight(x, z)
                    val nextAvgHeight = nextLvl!!.getAverageHeight(x, z)
                    val delta = currentAvgHeight - nextAvgHeight - contourModifier
                    this.verticesY!![v] = (delta * ((this.verticesY!![v] shl 8) / sizeY) shr 8) - (averageHeight - currentAvgHeight)
                }
            }

            if (this.vertexBuffer != null) {
                this.vertexBuffer!!.buffer = null
            }

            this.boundsCalculated = false
        }
    }

    private fun calculateBounds() {
        boundsCalls++
        boundsVertices += maxDepth.toLong()
        if (this.verticesX == null || this.verticesY == null || this.verticesZ == null) {
            this.boundsCalculated = true
            return
        }

        var minXVal = 32767
        var minYVal = 32767
        var minZVal = 32767
        var maxXVal = -32768
        var maxYVal = -32768
        var maxZVal = -32768
        var size2dSquared = 0
        var size3dSquared = 0

        val depth = minOf(this.maxDepth, this.verticesX!!.size, this.verticesY!!.size, this.verticesZ!!.size)
        for (v in 0 until depth) {
            val vertexX = this.verticesX!![v]
            val vertexY = this.verticesY!![v]
            val vertexZ = this.verticesZ!![v]

            if (vertexX < minXVal) minXVal = vertexX
            if (vertexX > maxXVal) maxXVal = vertexX
            if (vertexY < minYVal) minYVal = vertexY
            if (vertexY > maxYVal) maxYVal = vertexY
            if (vertexZ < minZVal) minZVal = vertexZ
            if (vertexZ > maxZVal) maxZVal = vertexZ

            val s2d = vertexX * vertexX + vertexZ * vertexZ
            if (s2d > size2dSquared) {
                size2dSquared = s2d
            }

            val s3d = vertexX * vertexX + vertexZ * vertexZ + vertexY * vertexY
            if (s3d > size3dSquared) {
                size3dSquared = s3d
            }
        }

        this.minX = minXVal.toShort()
        this.maxX = maxXVal.toShort()
        this.minY = minYVal.toShort()
        this.maxY = maxYVal.toShort()
        this.minZ = minZVal.toShort()
        this.maxZ = maxZVal.toShort()
        this.size2d = (sqrt(size2dSquared.toDouble()) + 0.99).toInt()
        this.size3d = (sqrt(size3dSquared.toDouble()) + 0.99).toInt()

        this.boundsCalculated = true
    }
}
