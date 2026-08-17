package com.jagex.game.runetek5.graphics.modern.util

import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.ModelGL
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList
import com.jagex.game.runetek5.graphics.modern.batch.ModelCache
import com.jagex.game.runetek5.utils.ColorUtil
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Builds modern-renderer geometry directly from a CLEAN decoded [Mesh], where
 * facesA/B/C are DIRECT vertex indices.
 *
 * This replaces the legacy-capture approach (reading ModelGL's post-connectNormal,
 * normal-slot-indexed arrays and reverse-mapping slots back to vertices), which
 * dropped whole submeshes on multi-model locs and produced wrong UVs on textured
 * faces. Working from the pre-connectNormal mesh sidesteps both problems: every
 * face references real vertices, and UVs are recomputed here with the same RS
 * texture-triangle projection the legacy ModelGL constructor uses.
 *
 * Conventions (all chosen to be drop-in for the existing modern pipeline):
 * - Positions are RAW model-local RS coordinates (ints as floats, no Y/Z flip).
 *   The modern pipeline keeps models in legacy space: the per-model Matrix43 and
 *   the Y-negated view-projection matrix perform the RS->GL mapping, exactly as
 *   in ModernGLToolkit.buildModelCache today.
 * - Normals are unit vectors in the same RS model space (no flips), matching the
 *   raw legacy normals the current cache path emits.
 * - UVs use the LEGACY orientation (NO 1-V flip). The reference engine flips V
 *   because it also flips texture rows at upload; darkan-bot samples the
 *   legacy-uploaded (unflipped) textures, so legacy-orientation UVs are correct.
 *   The projection math itself is identical (see Model.mapCylindericalUv etc.).
 * - Texture parameter arrays (texturesScaleX/Rotation/Direction/Speed/U-Vtrans)
 *   are indexed by the ABSOLUTE texture-triangle index, matching the legacy
 *   ModelGL constructor and Model.generateTextureMatrix. (The reference engine's
 *   ModelDefinition uses sparse complex/cube sub-indices instead - do not port
 *   that convention here.)
 * - Vertex layout is the 40-byte [ModelCache] format so the result plugs
 *   straight into OptimizedFrameBatcher.addCachedModel / InstancedModelBatcher:
 *   position vec3 (12) + normal vec3 (12) + uv vec2 (8) + rgba u8x4 (4) +
 *   modelIndex float placeholder (4).
 *
 * Face filtering — a VERBATIM port of legacy's count-time filter (ModelGL:402-419)
 * plus its render-type handling. Verified by GeometryDiff over the whole cache:
 * 157,236 mesh-built models, zero divergence from legacy in face count, vertex
 * position (all four rotations, yaw applied) and per-face RGBA.
 * - faceRenderType 0: smooth-shaded; contributes to per-vertex normals.
 * - faceRenderType 1: flat-shaded; uses the face normal for all three vertices.
 * - any other render type: hidden, skipped entirely.
 * - material.skipTriangles: face dropped (legacy ModelGL:407).
 * - detailsOnly material under [stripDetailLayers]: face DROPPED, not demoted to
 *   untextured (legacy ModelGL:410-412).
 * - facesAlpha is NOT a filter. RS alpha 255 means GL alpha 0, but legacy draws
 *   opaque scenery with GL_BLEND disabled, which ignores vertex alpha outright, so
 *   those faces rasterize solid. Skipping them deleted Canifis wall and door faces.
 *
 * Preconditions: ColorUtil.hslToRgb must be initialized (it always is once the
 * client has started); returns null otherwise rather than throwing.
 *
 * Wired in at model-creation time: ModernGLToolkit.createModel builds this from
 * the final combined Mesh and stores it on the ModelGL (modernBuilt). Legacy
 * post-create yaw baking is tracked (ModelGL.modernYaw) and re-applied via the
 * per-instance model matrix; any other post-create mutation nulls modernBuilt
 * and the renderer falls back to legacy-capture (buildModelCache).
 */
object ModernModelBuilder {

    /**
     * Geometry for one mesh, split into per-material batches.
     *
     * [vertexData] holds all vertices (3 per accepted face, in face order) in the
     * 40-byte [ModelCache] layout; each [TextureBatch] indexes into it. Storage is
     * a heap array (GC-managed, safe to keep on a ModelGL for its lifetime);
     * [toCachedModel] copies into a fresh native buffer each call, so LRU eviction
     * of the resulting [ModelCache.CachedModel] can free it and a later call can
     * rebuild it cheaply.
     *
     * [id] is a stable content hash of the geometry: identical meshes built at
     * different times (e.g. after the loc model cache evicted and recreated a
     * model) map to the same id, so the persistent instanced-geometry store and
     * the model cache dedupe them.
     */
    class BuiltModel(
        val id: Long,
        val faceCount: Int,
        val vertexCount: Int,
        val vertexData: ByteArray,
        val batches: List<TextureBatch>,
        /**
         * Why faces from the source mesh were not emitted, by rule. Populated for
         * diagnostics only (GeometryDiff): when this builder's face set diverges
         * from legacy's, this names the responsible rejection rule instead of
         * leaving it to be inferred.
         */
        val skipBillboardHidden: Int = 0,
        val skipRenderType: Int = 0,
        val skipInvalidVertex: Int = 0,
        val skipTransparent: Int = 0,
        val sourceFaceCount: Int = 0,
        val billboardsSeen: Int = 0,
        val billboardsWithUid: Int = 0,
        val billboardsOutOfRange: Int = 0,
        val skipMaterialFiltered: Int = 0,
        /**
         * Source-mesh face index for each emitted triangle, in emission order.
         * Lets colour be recomputed WITHOUT rebuilding geometry: colour lives at a
         * fixed offset inside each 40-byte vertex, so a tint/recolour only has to
         * rewrite 4 bytes per vertex while positions, normals and UVs stay valid.
         */
        val acceptedFaces: IntArray = IntArray(0),
        /**
         * RESOLVED material per emitted triangle, in emission order — the value the
         * face filter settled on, which is not always facesMaterial[face]. Colour
         * depends on it (material brightness/alpha feed faceColorRgba), so a recolour
         * must reuse it rather than re-read the raw array.
         */
        val acceptedMaterialIds: IntArray = IntArray(0)
    ) {
        /**
         * One draw batch: all faces sharing a material.
         *
         * @param materialId material/texture id, -1 for untextured
         * @param indices vertex indices into [vertexData] (multiple of 3)
         */
        class TextureBatch(val materialId: Int, val indices: IntArray)

        /**
         * Adapts this model to the structure the existing batchers consume.
         * Allocates a native copy of the vertex data; the returned CachedModel
         * owns it (its free() releases it, as the ModelCache LRU expects).
         */
        fun toCachedModel(): ModelCache.CachedModel = toCachedModel(vertexData)

        /** As [toCachedModel], but with colour bytes supplied by [recolour]. */
        fun toCachedModel(data: ByteArray): ModelCache.CachedModel {
            val buffer = MemoryUtil.memAlloc(data.size)
            buffer.put(data)
            buffer.flip()
            return ModelCache.CachedModel(
                faceCount = faceCount,
                vertexCount = vertexCount,
                vertexData = buffer,
                materialBatches = batches.map { batch ->
                    ModelCache.MaterialBatch(
                        materialId = batch.materialId,
                        indices = batch.indices,
                        indexCount = batch.indices.size
                    )
                }.toTypedArray()
            )
        }
    }

    /** Byte offset of the RGBA colour within a 40-byte vertex (pos12+normal12+uv8). */
    const val COLOR_OFFSET = 32

    /**
     * Returns a copy of [built]'s vertex data with ONLY the colour bytes recomputed
     * from the model's current face colours.
     *
     * Colour-only mutations (tint, recolor, setAmbient, environment alpha) change no
     * geometry, yet they used to discard the mesh-built model entirely and drop it onto
     * the legacy-capture path — where it is content-hashed over every vertex, every
     * frame, forever. Measured over one scene those four accounted for 13,383 of 19,684
     * invalidations. Rewriting 4 bytes per vertex keeps the geometry.
     */
    @JvmStatic
    fun recolour(
        built: BuiltModel,
        facesColor: ShortArray?,
        facesAlpha: ByteArray?,
        @Suppress("UNUSED_PARAMETER") facesMaterial: ShortArray?,
        ambient: Int,
        colorTable: IntArray,
        materialProvider: MaterialProvider?,
        /** ModelGL.meshFaceIndices: model-face -> mesh-face, needed to invert. */
        meshFaceIndices: IntArray?
    ): ByteArray {
        val out = built.vertexData.copyOf()
        if (facesColor == null || meshFaceIndices == null) return out
        // Invert to mesh-face -> model-face: the builder is mesh-indexed, the model's
        // colour arrays are indexed over its own filtered/sorted face list.
        var maxMesh = 0
        for (v in meshFaceIndices) if (v > maxMesh) maxMesh = v
        val meshToModel = IntArray(maxMesh + 1) { -1 }
        for (m in meshFaceIndices.indices) meshToModel[meshFaceIndices[m]] = m
        val faces = built.acceptedFaces
        val stride = ModelCache.BYTES_PER_VERTEX
        for (i in faces.indices) {
            val meshFace = faces[i]
            if (meshFace >= meshToModel.size) continue
            val face = meshToModel[meshFace]
            if (face < 0 || face >= facesColor.size) continue
            // The RESOLVED material, exactly as emission used it. Re-reading
            // facesMaterial[face] here instead applied a different material's
            // brightness through faceColorRgba, which saturates to white — that is
            // what turned recoloured bushes white while everything else looked fine.
            val materialId = if (i < built.acceptedMaterialIds.size) built.acceptedMaterialIds[i] else -1
            val alpha = facesAlpha?.getOrNull(face)?.toInt()?.and(0xff) ?: 0
            val rgba = faceColorRgba(
                colorTable, facesColor[face].toInt() and 0xffff,
                materialId, ambient, alpha, materialProvider
            )
            var base = i * 3 * stride + COLOR_OFFSET
            for (k in 0 until 3) {
                out[base] = ((rgba ushr 24) and 0xff).toByte()
                out[base + 1] = ((rgba ushr 16) and 0xff).toByte()
                out[base + 2] = ((rgba ushr 8) and 0xff).toByte()
                out[base + 3] = (rgba and 0xff).toByte()
                base += stride
            }
        }
        return out
    }

    @JvmField val MB_TINT = System.getProperty("render.mbtint")?.equals("true", true) == true

    /** Aggregate skip accounting across every build this session. */
    @JvmField var aggSource = 0L
    @JvmField var aggBillboard = 0L
    @JvmField var aggRenderType = 0L
    @JvmField var aggInvalidVertex = 0L
    @JvmField var aggTransparent = 0L
    @JvmField var aggMaterial = 0L

    @JvmStatic
    fun skipSummary(): String =
        "mbSkip[src=$aggSource bb=$aggBillboard rt=$aggRenderType " +
        "badVert=$aggInvalidVertex alpha=$aggTransparent mat=$aggMaterial]"

    /**
     * Builds renderable geometry from a clean decoded mesh.
     *
     * @param mesh the combined, decoded mesh (direct vertex indices)
     * @param ambient model ambient lightness (legacy createModel ambient, e.g. 64)
     * @param materialProvider used for material alpha/brightness color modulation
     *   and detailsOnly filtering; when null those steps are skipped
     * @param stripDetailLayers when true, faces whose material is detailsOnly are
     *   rendered untextured (legacy preferenceFlags bit 0x40 behavior)
     * @return built geometry, or null if the mesh has no drawable faces
     */
    @JvmStatic
    fun build(
        mesh: Mesh,
        ambient: Int,
        materialProvider: MaterialProvider? = null,
        stripDetailLayers: Boolean = false
    ): BuiltModel? {
        val numFaces = mesh.numFaces
        val numVertices = mesh.numVertices
        if (numFaces <= 0 || numVertices <= 0) return null
        val facesA = mesh.facesA ?: return null
        val facesB = mesh.facesB ?: return null
        val facesC = mesh.facesC ?: return null
        val verticesX = mesh.verticesX ?: return null
        val verticesY = mesh.verticesY ?: return null
        val verticesZ = mesh.verticesZ ?: return null
        val colorTable = ColorUtil.hslToRgb ?: return null
        val facesColor = mesh.facesColor
        val facesAlpha = mesh.facesAlpha
        val facesMaterial = mesh.facesMaterial
        val faceRenderType = mesh.faceRenderType

        fun vertexA(face: Int) = facesA[face].toInt() and 0xffff
        fun vertexB(face: Int) = facesB[face].toInt() and 0xffff
        fun vertexC(face: Int) = facesC[face].toInt() and 0xffff
        fun isValidVertex(index: Int) = index < numVertices

        // ---- Pass 1: smooth vertex normals (render type 0 faces only) --------
        val smoothX = FloatArray(numVertices)
        val smoothY = FloatArray(numVertices)
        val smoothZ = FloatArray(numVertices)
        val smoothCount = IntArray(numVertices)
        val faceNormal = FloatArray(3)

        fun computeFaceNormal(face: Int) {
            val a = vertexA(face)
            val b = vertexB(face)
            val c = vertexC(face)
            val e1x = (verticesX[b] - verticesX[a]).toFloat()
            val e1y = (verticesY[b] - verticesY[a]).toFloat()
            val e1z = (verticesZ[b] - verticesZ[a]).toFloat()
            val e2x = (verticesX[c] - verticesX[a]).toFloat()
            val e2y = (verticesY[c] - verticesY[a]).toFloat()
            val e2z = (verticesZ[c] - verticesZ[a]).toFloat()
            var nx = e1y * e2z - e1z * e2y
            var ny = e1z * e2x - e1x * e2z
            var nz = e1x * e2y - e1y * e2x
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            if (len > 0f) {
                nx /= len
                ny /= len
                nz /= len
            }
            faceNormal[0] = nx
            faceNormal[1] = ny
            faceNormal[2] = nz
        }

        for (face in 0 until numFaces) {
            val renderType = faceRenderType?.get(face)?.toInt() ?: 0
            if (renderType != 0) continue
            val a = vertexA(face)
            val b = vertexB(face)
            val c = vertexC(face)
            if (!isValidVertex(a) || !isValidVertex(b) || !isValidVertex(c)) continue
            computeFaceNormal(face)
            smoothX[a] += faceNormal[0]; smoothY[a] += faceNormal[1]; smoothZ[a] += faceNormal[2]; smoothCount[a]++
            smoothX[b] += faceNormal[0]; smoothY[b] += faceNormal[1]; smoothZ[b] += faceNormal[2]; smoothCount[b]++
            smoothX[c] += faceNormal[0]; smoothY[c] += faceNormal[1]; smoothZ[c] += faceNormal[2]; smoothCount[c]++
        }

        // Faces hidden by billboards: legacy removes a face from rasterization
        // when ANY attached billboard's type has hasUid set (the billboard quad
        // replaces the anchor face). Drawing them anyway is the "green quad"
        // bug - anchor faces carry the billboard tint as their face color.
        var billboardHiddenFaces: HashSet<Int>? = null
        var billboardsSeen = 0
        var billboardsWithUid = 0
        var billboardsOutOfRange = 0
        mesh.billboards?.let { billboards ->
            for (billboard in billboards) {
                billboardsSeen++
                if (billboard.appliesOnFace < 0 || billboard.appliesOnFace >= numFaces) {
                    billboardsOutOfRange++
                    continue
                }
                if (BillboardTypeList.list(billboard.billboardId).hasUid) {
                    billboardsWithUid++
                    (billboardHiddenFaces ?: HashSet<Int>().also { billboardHiddenFaces = it })
                        .add(billboard.appliesOnFace)
                }
            }
        }

        // ---- Pass 2: select drawable faces and resolve materials -------------
        val acceptedFaces = IntArray(numFaces)
        val acceptedMaterials = IntArray(numFaces)
        var acceptedCount = 0

        var skipBillboardHidden = 0
        var skipRenderType = 0
        var skipInvalidVertex = 0
        var skipTransparent = 0
        var skipMaterialFiltered = 0

        for (face in 0 until numFaces) {
            if (billboardHiddenFaces?.contains(face) == true) { skipBillboardHidden++; continue }

            val renderType = faceRenderType?.get(face)?.toInt() ?: 0
            // Legacy skips render type 2 and NOTHING ELSE — the ModelGL constructor's
            // filter is `faceRenderType[i] != 2` (ModelGL.kt:403). Restricting to types
            // 0 and 1 additionally dropped every face of any other type, which removed
            // whole wall surfaces from buildings while leaving their frames and roofs.
            if (renderType == 2) { skipRenderType++; continue }

            val a = vertexA(face)
            val b = vertexB(face)
            val c = vertexC(face)
            if (!isValidVertex(a) || !isValidVertex(b) || !isValidVertex(c)) { skipInvalidVertex++; continue }

            // DO NOT skip RS alpha == 255 faces. The obvious reading is "fully
            // transparent, nothing to draw", but legacy draws opaque scenery with
            // GL_BLEND DISABLED (OpenGlToolkit.setBlendMode(0)), and with blending
            // off the vertex alpha is ignored entirely — the face rasterizes solid.
            // Legacy's renderedFaceCount confirms it: for Canifis walls it equals
            // the full source face count, so legacy discards none of them.
            //
            // Dropping them here deleted whole wall/door faces that legacy renders,
            // which is exactly the missing-Canifis-walls symptom (GeometryDiff:
            // every wall/door divergence was skips[transparent=N], nothing else).
            // The alpha still travels in the vertex colour; whether it is honoured
            // is the blend state's decision at draw time, not the builder's.

            // SIGNED read: -1 = untextured, 0..N = valid material (including 0).
            var materialId = facesMaterial?.get(face)?.toInt() ?: -1
            if (materialId != -1 && materialProvider != null) {
                // Port legacy's count-time face filter VERBATIM (ModelGL:402-419):
                //
                //   if (!stripDetail || !detailsOnly) { if (skipTriangles) continue }
                //   else                              { continue }
                //
                // Two rules the builder previously lacked. skipTriangles was not
                // checked at all, and a detailsOnly face under stripDetail was
                // DEMOTED to untextured and still drawn rather than dropped. Both
                // emit faces legacy never rasterizes; being untextured they render
                // as flat quads carrying the raw face tint — the green quads.
                val material = materialProvider.getMaterial(materialId and 0xffff)
                if (stripDetailLayers && material.detailsOnly) {
                    skipMaterialFiltered++
                    continue
                }
                if (material.skipTriangles) {
                    skipMaterialFiltered++
                    continue
                }
            }

            acceptedFaces[acceptedCount] = face
            acceptedMaterials[acceptedCount] = materialId
            acceptedCount++
        }
        if (acceptedCount == 0) return null

        // ---- Texture transform data (centers + matrices per texture) --------
        val transforms = if (mesh.facesTexture != null && mesh.numTextures > 0) {
            computeTextureTransforms(mesh)
        } else {
            null
        }

        // ---- Pass 3: emit 40-byte vertices in face order ---------------------
        val vertexCount = acceptedCount * 3
        val vertexArray = ByteArray(vertexCount * ModelCache.BYTES_PER_VERTEX)
        // NATIVE byte order. ByteBuffer.wrap defaults to BIG_ENDIAN, but
        // toCachedModel copies these bytes verbatim into a MemoryUtil.memAlloc buffer
        // (native = little-endian on x86) that is uploaded straight to the GPU, so
        // big-endian floats were read byte-swapped: every position became garbage and
        // no mesh-built geometry ever appeared on screen. The legacy-capture path
        // writes native order (ModernGLToolkit.buildModelCache), which is why only
        // this path was affected — and why the build-time scan passed, since both the
        // data and the oracle read it as big-endian and never exercised the GPU view.
        val vertexData = ByteBuffer.wrap(vertexArray).order(java.nio.ByteOrder.nativeOrder())
        val uv = FloatArray(6)

        fun writeVertex(vertex: Int, nx: Float, ny: Float, nz: Float, u: Float, v: Float, rgba: Int) {
            vertexData.putFloat(verticesX[vertex].toFloat())
            vertexData.putFloat(verticesY[vertex].toFloat())
            vertexData.putFloat(verticesZ[vertex].toFloat())
            vertexData.putFloat(nx)
            vertexData.putFloat(ny)
            vertexData.putFloat(nz)
            vertexData.putFloat(u)
            vertexData.putFloat(v)
            vertexData.put(((rgba ushr 24) and 0xff).toByte())
            vertexData.put(((rgba ushr 16) and 0xff).toByte())
            vertexData.put(((rgba ushr 8) and 0xff).toByte())
            vertexData.put((rgba and 0xff).toByte())
            vertexData.putFloat(0f)  // modelIndex placeholder, filled at render time
        }

        for (i in 0 until acceptedCount) {
            val face = acceptedFaces[i]
            val materialId = acceptedMaterials[i]
            val a = vertexA(face)
            val b = vertexB(face)
            val c = vertexC(face)

            computeUVs(mesh, transforms, face, materialId, uv)

            val hsl = facesColor?.get(face)?.toInt()?.and(0xffff) ?: 0
            val alpha = (facesAlpha?.get(face) ?: 0).toInt() and 0xff
            val rgba = faceColorRgba(colorTable, hsl, materialId, ambient, alpha, materialProvider)

            val renderType = faceRenderType?.get(face)?.toInt() ?: 0
            if (renderType == 0) {
                writeVertex(a, smoothNormalX(smoothX, smoothY, smoothZ, smoothCount, a, 0),
                    smoothNormalX(smoothX, smoothY, smoothZ, smoothCount, a, 1),
                    smoothNormalX(smoothX, smoothY, smoothZ, smoothCount, a, 2), uv[0], uv[1], rgba)
                writeVertex(b, smoothNormalX(smoothX, smoothY, smoothZ, smoothCount, b, 0),
                    smoothNormalX(smoothX, smoothY, smoothZ, smoothCount, b, 1),
                    smoothNormalX(smoothX, smoothY, smoothZ, smoothCount, b, 2), uv[2], uv[3], rgba)
                writeVertex(c, smoothNormalX(smoothX, smoothY, smoothZ, smoothCount, c, 0),
                    smoothNormalX(smoothX, smoothY, smoothZ, smoothCount, c, 1),
                    smoothNormalX(smoothX, smoothY, smoothZ, smoothCount, c, 2), uv[4], uv[5], rgba)
            } else {
                // Flat shading: the face normal on all three vertices.
                computeFaceNormal(face)
                writeVertex(a, faceNormal[0], faceNormal[1], faceNormal[2], uv[0], uv[1], rgba)
                writeVertex(b, faceNormal[0], faceNormal[1], faceNormal[2], uv[2], uv[3], rgba)
                writeVertex(c, faceNormal[0], faceNormal[1], faceNormal[2], uv[4], uv[5], rgba)
            }
        }
        // ---- Group face indices by material (untextured first, then by id) ---
        val materialToIndices = LinkedHashMap<Int, MutableList<Int>>()
        for (i in 0 until acceptedCount) {
            val materialId = if (acceptedMaterials[i] < 0) -1 else acceptedMaterials[i]
            val indices = materialToIndices.getOrPut(materialId) { mutableListOf() }
            val base = i * 3
            indices.add(base)
            indices.add(base + 1)
            indices.add(base + 2)
        }
        val batches = materialToIndices.keys
            .sortedBy { if (it < 0) Int.MIN_VALUE else it }
            .map { materialId ->
                BuiltModel.TextureBatch(materialId, materialToIndices[materialId]!!.toIntArray())
            }

        return BuiltModel(
            id = contentHash(vertexArray, batches),
            faceCount = acceptedCount,
            vertexCount = vertexCount,
            vertexData = vertexArray,
            batches = batches,
            acceptedFaces = acceptedFaces.copyOf(acceptedCount),
            acceptedMaterialIds = acceptedMaterials.copyOf(acceptedCount),
            skipBillboardHidden = skipBillboardHidden.also {
                aggSource += numFaces
                aggBillboard += skipBillboardHidden
                aggRenderType += skipRenderType
                aggInvalidVertex += skipInvalidVertex
                aggTransparent += skipTransparent
                aggMaterial += skipMaterialFiltered
            },
            skipRenderType = skipRenderType,
            skipInvalidVertex = skipInvalidVertex,
            skipTransparent = skipTransparent,
            sourceFaceCount = numFaces,
            billboardsSeen = billboardsSeen,
            billboardsWithUid = billboardsWithUid,
            billboardsOutOfRange = billboardsOutOfRange,
            skipMaterialFiltered = skipMaterialFiltered
        )
    }

    /**
     * Stable 64-bit FNV-1a hash over the emitted vertex bytes plus batch material
     * ids. Identical geometry always maps to the same id, so re-created models
     * (loc model cache churn) reuse existing cache/instanced-store entries.
     */
    private fun contentHash(vertexData: ByteArray, batches: List<BuiltModel.TextureBatch>): Long {
        var hash = -0x340d631b7bdddcdbL  // FNV-1a 64-bit offset basis
        for (byte in vertexData) {
            hash = hash xor (byte.toLong() and 0xff)
            hash *= 0x100000001b3L
        }
        for (batch in batches) {
            hash = hash xor batch.materialId.toLong()
            hash *= 0x100000001b3L
        }
        return hash
    }

    /** Averaged, renormalized smooth vertex normal component; +Y up fallback. */
    private fun smoothNormalX(
        sx: FloatArray, sy: FloatArray, sz: FloatArray, count: IntArray,
        vertex: Int, component: Int
    ): Float {
        if (count[vertex] == 0) return if (component == 1) 1f else 0f
        val nx = sx[vertex]
        val ny = sy[vertex]
        val nz = sz[vertex]
        val len = sqrt(nx * nx + ny * ny + nz * nz)
        if (len <= 0f) return if (component == 1) 1f else 0f
        return when (component) {
            0 -> nx / len
            1 -> ny / len
            else -> nz / len
        }
    }

    // ------------------------------------------------------------------------
    // Color
    // ------------------------------------------------------------------------

    /**
     * Pure port of ModelGL.hslToRgb: ambient-adjusted HSL lookup plus optional
     * material ambient-alpha blend and brightness boost. Returns packed 0xRRGGBBAA
     * with GL alpha = 255 - RS alpha.
     */
    private fun faceColorRgba(
        colorTable: IntArray,
        colorHsl: Int,
        materialId: Int,
        ambient: Int,
        alpha: Int,
        materialProvider: MaterialProvider?
    ): Int {
        // -Drender.mbtint=true paints every mesh-built face magenta, so a single
        // capture shows exactly which geometry takes this path.
        if (MB_TINT) return 0xFF00FFFF.toInt()
        var rgb = colorTable[ModelGL.adjustLuminance(colorHsl, ambient)]
        if (materialId != -1 && materialProvider != null) {
            val material = materialProvider.getMaterial(materialId and 0xffff)
            val materialAlpha = material.alpha.toInt() and 0xff
            if (materialAlpha != 0) {
                val ambientRgb = when {
                    ambient < 0 -> 0
                    ambient > 127 -> 16777215
                    else -> ambient * 131586
                }
                val inverse = 256 - materialAlpha
                rgb = (((ambientRgb and 0xff00ff) * materialAlpha + (rgb and 0xff00ff) * inverse and -16711936) +
                    (materialAlpha * (ambientRgb and 0xff00) + inverse * (rgb and 0xff00) and 0xff0000)) shr 8
            }
            val brightnessRaw = material.brightness.toInt() and 0xff
            if (brightnessRaw != 0) {
                val brightness = brightnessRaw + 256
                var scaledR = brightness * ((rgb and 0xff0000) shr 16)
                if (scaledR > 65535) scaledR = 65535
                var scaledG = brightness * ((rgb and 0xff00) shr 8)
                if (scaledG > 65535) scaledG = 65535
                var scaledB = (rgb and 0xff) * brightness
                if (scaledB > 65535) scaledB = 65535
                rgb = (scaledB shr 8) + (scaledG and 0xff00) + ((scaledR shl 8) and 0xff0000)
            }
        }
        return (rgb shl 8) or (255 - alpha)
    }

    // ------------------------------------------------------------------------
    // Texture transforms (port of Model.generateTextureMatrix, mesh-only)
    // ------------------------------------------------------------------------

    private class TextureTransforms(
        val centerX: IntArray,
        val centerY: IntArray,
        val centerZ: IntArray,
        val matrix: Array<FloatArray>
    )

    /**
     * Computes per-texture-triangle bounding-box centers and 3x3 projection
     * matrices for complex mappings (types 1-3). All arrays are indexed by the
     * ABSOLUTE texture-triangle index, matching legacy ModelGL.
     */
    private fun computeTextureTransforms(mesh: Mesh): TextureTransforms {
        val numTextures = mesh.numTextures
        val facesTexture = mesh.facesTexture!!
        val verticesX = mesh.verticesX!!
        val verticesY = mesh.verticesY!!
        val verticesZ = mesh.verticesZ!!
        val minX = IntArray(numTextures) { Int.MAX_VALUE }
        val maxX = IntArray(numTextures) { Int.MIN_VALUE }
        val minY = IntArray(numTextures) { Int.MAX_VALUE }
        val maxY = IntArray(numTextures) { Int.MIN_VALUE }
        val minZ = IntArray(numTextures) { Int.MAX_VALUE }
        val maxZ = IntArray(numTextures) { Int.MIN_VALUE }

        for (face in 0 until mesh.numFaces) {
            if (facesTexture[face].toInt() == -1) continue
            val texture = facesTexture[face].toInt() and 0xff
            if (texture >= numTextures) continue
            for (axis in 0..2) {
                val vertex = when (axis) {
                    0 -> mesh.facesA!![face]
                    1 -> mesh.facesB!![face]
                    else -> mesh.facesC!![face]
                }.toInt() and 0xffff
                if (vertex >= mesh.numVertices) continue
                val x = verticesX[vertex]
                val y = verticesY[vertex]
                val z = verticesZ[vertex]
                if (x < minX[texture]) minX[texture] = x
                if (x > maxX[texture]) maxX[texture] = x
                if (y < minY[texture]) minY[texture] = y
                if (y > maxY[texture]) maxY[texture] = y
                if (z < minZ[texture]) minZ[texture] = z
                if (z > maxZ[texture]) maxZ[texture] = z
            }
        }

        val centerX = IntArray(numTextures)
        val centerY = IntArray(numTextures)
        val centerZ = IntArray(numTextures)
        val matrix = Array(numTextures) { FloatArray(0) }
        for (texture in 0 until numTextures) {
            val mapping = mesh.texturesMapping!![texture].toInt()
            if (mapping <= 0) continue
            centerX[texture] = (minX[texture] + maxX[texture]) / 2
            centerY[texture] = (minY[texture] + maxY[texture]) / 2
            centerZ[texture] = (minZ[texture] + maxZ[texture]) / 2
            val scaleX: Float
            val scaleY: Float
            val scaleZ: Float
            if (mapping == 1) {
                val x = scaleParam(mesh.texturesScaleX, texture, 0)
                if (x == 0) {
                    scaleX = 1.0f
                    scaleZ = 1.0f
                } else if (x > 0) {
                    scaleX = 1.0f
                    scaleZ = x / 1024.0f
                } else {
                    scaleZ = 1.0f
                    scaleX = (-x) / 1024.0f
                }
                scaleY = 64.0f / nonZero(scaleParam(mesh.texturesScaleY, texture, 1))
            } else if (mapping == 2) {
                scaleX = 64.0f / nonZero(scaleParam(mesh.texturesScaleX, texture, 1))
                scaleY = 64.0f / nonZero(scaleParam(mesh.texturesScaleY, texture, 1))
                scaleZ = 64.0f / nonZero(scaleParam(mesh.texturesScaleZ, texture, 1))
            } else {
                scaleX = scaleParam(mesh.texturesScaleX, texture, 1024) / 1024.0f
                scaleY = scaleParam(mesh.texturesScaleY, texture, 1024) / 1024.0f
                scaleZ = scaleParam(mesh.texturesScaleZ, texture, 1024) / 1024.0f
            }
            matrix[texture] = createTextureMatrix(
                mesh.textureFaceX!![texture].toInt(),
                mesh.textureFaceY!![texture].toInt(),
                mesh.textureFaceZ!![texture].toInt(),
                mesh.texturesRotation?.get(texture)?.toInt()?.and(0xff) ?: 0,
                scaleX, scaleY, scaleZ
            )
        }
        return TextureTransforms(centerX, centerY, centerZ, matrix)
    }

    /** Bounds-guarded read of the (occasionally short-allocated) texture params. */
    private fun scaleParam(array: IntArray?, index: Int, fallback: Int): Int {
        if (array == null || index >= array.size) return fallback
        return array[index]
    }

    private fun nonZero(value: Int): Int = if (value == 0) 1 else value

    /**
     * Port of Model.createTextureMatrix: Y-axis rotation combined with the
     * axis-angle rotation derived from the texture triangle "vertices"
     * (which encode an orientation for complex mappings), then scaled.
     */
    private fun createTextureMatrix(
        n: Int, m: Int, p: Int, rotation: Int,
        scaleX: Float, scaleY: Float, scaleZ: Float
    ): FloatArray {
        val rotationMap = FloatArray(9)
        val result = FloatArray(9)
        val cosR = cos(rotation * 0.024543693f)
        val sinR = sin(rotation * 0.024543693f)
        rotationMap[0] = cosR
        rotationMap[2] = sinR
        rotationMap[4] = 1.0f
        rotationMap[6] = -sinR
        rotationMap[8] = cosR
        val matrix3x3 = FloatArray(9)
        var x = 1.0f
        var y = 0.0f
        val cosA = m / 32767.0f
        val sinA = -sqrt(1.0f - cosA * cosA)
        val omc = 1.0f - cosA
        val size = sqrt((p * p + n * n).toFloat())
        if (size == 0.0f && cosA == 0.0f) {
            return rotationMap
        }
        if (size != 0.0f) {
            x = (-p) / size
            y = n / size
        }
        matrix3x3[0] = cosA + x * x * omc
        matrix3x3[1] = y * sinA
        matrix3x3[2] = y * x * omc
        matrix3x3[3] = -y * sinA
        matrix3x3[4] = cosA
        matrix3x3[5] = x * sinA
        matrix3x3[6] = x * y * omc
        matrix3x3[7] = -x * sinA
        matrix3x3[8] = cosA + y * y * omc
        result[0] = rotationMap[0] * matrix3x3[0] + rotationMap[1] * matrix3x3[3] + rotationMap[2] * matrix3x3[6]
        result[1] = rotationMap[0] * matrix3x3[1] + rotationMap[1] * matrix3x3[4] + rotationMap[2] * matrix3x3[7]
        result[2] = rotationMap[0] * matrix3x3[2] + rotationMap[1] * matrix3x3[5] + rotationMap[2] * matrix3x3[8]
        result[3] = rotationMap[3] * matrix3x3[0] + rotationMap[4] * matrix3x3[3] + rotationMap[5] * matrix3x3[6]
        result[4] = rotationMap[3] * matrix3x3[1] + rotationMap[4] * matrix3x3[4] + rotationMap[5] * matrix3x3[7]
        result[5] = rotationMap[3] * matrix3x3[2] + rotationMap[4] * matrix3x3[5] + rotationMap[5] * matrix3x3[8]
        result[6] = rotationMap[6] * matrix3x3[0] + rotationMap[7] * matrix3x3[3] + rotationMap[8] * matrix3x3[6]
        result[7] = rotationMap[6] * matrix3x3[1] + rotationMap[7] * matrix3x3[4] + rotationMap[8] * matrix3x3[7]
        result[8] = rotationMap[6] * matrix3x3[2] + rotationMap[7] * matrix3x3[5] + rotationMap[8] * matrix3x3[8]
        result[0] *= scaleX
        result[1] *= scaleX
        result[2] *= scaleX
        result[3] *= scaleY
        result[4] *= scaleY
        result[5] *= scaleY
        result[6] *= scaleZ
        result[7] *= scaleZ
        result[8] *= scaleZ
        return result
    }

    // ------------------------------------------------------------------------
    // UV computation (ports of the legacy ModelGL constructor's UV block)
    // ------------------------------------------------------------------------

    /**
     * Computes the six UV components (aU, aV, bU, bV, cU, cV) for a face into [uv].
     * Untextured faces get zeros; textured faces without a texture pointer get the
     * legacy fixed fallback (0,1)-(1,1)-(0,0).
     */
    private fun computeUVs(mesh: Mesh, transforms: TextureTransforms?, face: Int, materialId: Int, uv: FloatArray) {
        uv.fill(0f)
        if (materialId == -1) return

        val texturePointer = mesh.facesTexture?.get(face)?.toInt() ?: -1
        if (texturePointer == -1) {
            // Legacy fixed UVs for material-without-texture-triangle faces.
            uv[0] = 0.0f; uv[1] = 1.0f
            uv[2] = 1.0f; uv[3] = 1.0f
            uv[4] = 0.0f; uv[5] = 0.0f
            return
        }
        val texture = texturePointer and 0xff
        if (texture >= mesh.numTextures) {
            uv[0] = 0.0f; uv[1] = 1.0f
            uv[2] = 1.0f; uv[3] = 1.0f
            uv[4] = 0.0f; uv[5] = 0.0f
            return
        }

        when (mesh.texturesMapping!![texture].toInt()) {
            0 -> computePlanarUVs(mesh, texture, face, uv)
            1 -> computeCylindricalUVs(mesh, transforms, texture, face, uv)
            2 -> computeCubeUVs(mesh, transforms, texture, face, uv)
            3 -> computeSphericalUVs(mesh, transforms, texture, face, uv)
        }
    }

    /**
     * Type 0: planar projection onto the texture triangle's plane. Direct port of
     * the legacy ModelGL constructor's simple-mapping block, with a zero-
     * determinant guard for degenerate texture triangles.
     */
    private fun computePlanarUVs(mesh: Mesh, texture: Int, face: Int, uv: FloatArray) {
        val verticesX = mesh.verticesX!!
        val verticesY = mesh.verticesY!!
        val verticesZ = mesh.verticesZ!!
        val texVertexA = mesh.textureFaceX!![texture].toInt() and 0xffff
        val texVertexB = mesh.textureFaceY!![texture].toInt() and 0xffff
        val texVertexC = mesh.textureFaceZ!![texture].toInt() and 0xffff
        if (texVertexA >= mesh.numVertices || texVertexB >= mesh.numVertices || texVertexC >= mesh.numVertices) {
            uv[0] = 0.0f; uv[1] = 1.0f
            uv[2] = 1.0f; uv[3] = 1.0f
            uv[4] = 0.0f; uv[5] = 0.0f
            return
        }
        val a = mesh.facesA!![face].toInt() and 0xffff
        val b = mesh.facesB!![face].toInt() and 0xffff
        val c = mesh.facesC!![face].toInt() and 0xffff

        val originX = verticesX[texVertexA].toFloat()
        val originY = verticesY[texVertexA].toFloat()
        val originZ = verticesZ[texVertexA].toFloat()
        val edge1X = verticesX[texVertexB] - originX
        val edge1Y = verticesY[texVertexB] - originY
        val edge1Z = verticesZ[texVertexB] - originZ
        val edge2X = verticesX[texVertexC] - originX
        val edge2Y = verticesY[texVertexC] - originY
        val edge2Z = verticesZ[texVertexC] - originZ
        val aOffX = verticesX[a] - originX
        val aOffY = verticesY[a] - originY
        val aOffZ = verticesZ[a] - originZ
        val bOffX = verticesX[b] - originX
        val bOffY = verticesY[b] - originY
        val bOffZ = verticesZ[b] - originZ
        val cOffX = verticesX[c] - originX
        val cOffY = verticesY[c] - originY
        val cOffZ = verticesZ[c] - originZ

        val crossX = edge1Y * edge2Z - edge1Z * edge2Y
        val crossY = edge1Z * edge2X - edge1X * edge2Z
        val crossZ = edge1X * edge2Y - edge1Y * edge2X

        var basisX = edge2Y * crossZ - edge2Z * crossY
        var basisY = edge2Z * crossX - edge2X * crossZ
        var basisZ = edge2X * crossY - edge2Y * crossX
        var determinant = basisX * edge1X + basisY * edge1Y + basisZ * edge1Z
        if (determinant == 0f) determinant = 1f
        var scale = 1.0f / determinant
        uv[0] = (basisX * aOffX + basisY * aOffY + basisZ * aOffZ) * scale
        uv[2] = (basisX * bOffX + basisY * bOffY + basisZ * bOffZ) * scale
        uv[4] = (basisX * cOffX + basisY * cOffY + basisZ * cOffZ) * scale

        basisX = edge1Y * crossZ - edge1Z * crossY
        basisY = edge1Z * crossX - edge1X * crossZ
        basisZ = edge1X * crossY - edge1Y * crossX
        determinant = basisX * edge2X + basisY * edge2Y + basisZ * edge2Z
        if (determinant == 0f) determinant = 1f
        scale = 1.0f / determinant
        uv[1] = (basisX * aOffX + basisY * aOffY + basisZ * aOffZ) * scale
        uv[3] = (basisX * bOffX + basisY * bOffY + basisZ * bOffZ) * scale
        uv[5] = (basisX * cOffX + basisY * cOffY + basisZ * cOffZ) * scale
    }

    /** Type 1: cylindrical projection with U (or V) seam correction. */
    private fun computeCylindricalUVs(mesh: Mesh, transforms: TextureTransforms?, texture: Int, face: Int, uv: FloatArray) {
        if (transforms == null || transforms.matrix[texture].isEmpty()) return
        val matrix = transforms.matrix[texture]
        val centerX = transforms.centerX[texture]
        val centerY = transforms.centerY[texture]
        val centerZ = transforms.centerZ[texture]
        val direction = mesh.texturesDirection?.get(texture)?.toInt() ?: 0
        val speed = scaleParam(mesh.texturesSpeed, texture, 0) / 256.0f
        val offset = scaleParam(mesh.texturesScaleZ, texture, 1024) / 1024.0f
        val a = mesh.facesA!![face].toInt() and 0xffff
        val b = mesh.facesB!![face].toInt() and 0xffff
        val c = mesh.facesC!![face].toInt() and 0xffff
        val verticesX = mesh.verticesX!!
        val verticesY = mesh.verticesY!!
        val verticesZ = mesh.verticesZ!!
        val scratch = FloatArray(2)

        mapCylindricalUv(verticesX[a], verticesY[a], verticesZ[a], centerX, centerY, centerZ, matrix, offset, direction, speed, scratch)
        uv[0] = scratch[0]; uv[1] = scratch[1]
        mapCylindricalUv(verticesX[b], verticesY[b], verticesZ[b], centerX, centerY, centerZ, matrix, offset, direction, speed, scratch)
        uv[2] = scratch[0]; uv[3] = scratch[1]
        mapCylindricalUv(verticesX[c], verticesY[c], verticesZ[c], centerX, centerY, centerZ, matrix, offset, direction, speed, scratch)
        uv[4] = scratch[0]; uv[5] = scratch[1]

        val half = offset / 2.0f
        if ((direction and 0x1) == 0) {
            if (uv[2] - uv[0] > half) uv[2] -= offset
            else if (uv[0] - uv[2] > half) uv[2] += offset
            if (uv[4] - uv[0] > half) uv[4] -= offset
            else if (uv[0] - uv[4] > half) uv[4] += offset
        } else {
            if (uv[3] - uv[1] > half) uv[3] -= offset
            else if (uv[1] - uv[3] > half) uv[3] += offset
            if (uv[5] - uv[1] > half) uv[5] -= offset
            else if (uv[1] - uv[5] > half) uv[5] += offset
        }
    }

    /**
     * Type 2: cube/rectangular projection. The face normal (transformed by the
     * texture matrix and un-scaled) selects which cube face to project onto.
     */
    private fun computeCubeUVs(mesh: Mesh, transforms: TextureTransforms?, texture: Int, face: Int, uv: FloatArray) {
        if (transforms == null || transforms.matrix[texture].isEmpty()) return
        val matrix = transforms.matrix[texture]
        val centerX = transforms.centerX[texture]
        val centerY = transforms.centerY[texture]
        val centerZ = transforms.centerZ[texture]
        val direction = mesh.texturesDirection?.get(texture)?.toInt() ?: 0
        val speed = scaleParam(mesh.texturesSpeed, texture, 0) / 256.0f
        val uTrans = scaleParam(mesh.texturesUtrans, texture, 0) / 256.0f
        val vTrans = scaleParam(mesh.texturesVtrans, texture, 0) / 256.0f
        val a = mesh.facesA!![face].toInt() and 0xffff
        val b = mesh.facesB!![face].toInt() and 0xffff
        val c = mesh.facesC!![face].toInt() and 0xffff
        val verticesX = mesh.verticesX!!
        val verticesY = mesh.verticesY!!
        val verticesZ = mesh.verticesZ!!

        // Face normal via cross product, transformed by the (scaled) texture
        // matrix then divided by the scale so cube-face selection is based on
        // the un-scaled orientation - exactly as the legacy constructor does.
        val edge1X = verticesX[b] - verticesX[a]
        val edge1Y = verticesY[b] - verticesY[a]
        val edge1Z = verticesZ[b] - verticesZ[a]
        val edge2X = verticesX[c] - verticesX[a]
        val edge2Y = verticesY[c] - verticesY[a]
        val edge2Z = verticesZ[c] - verticesZ[a]
        val crossX = edge1Y * edge2Z - edge2Y * edge1Z
        val crossY = edge1Z * edge2X - edge2Z * edge1X
        val crossZ = edge1X * edge2Y - edge2X * edge1Y
        val scaleX = 64.0f / nonZero(scaleParam(mesh.texturesScaleX, texture, 1))
        val scaleY = 64.0f / nonZero(scaleParam(mesh.texturesScaleY, texture, 1))
        val scaleZ = 64.0f / nonZero(scaleParam(mesh.texturesScaleZ, texture, 1))
        val normalX = (crossX * matrix[0] + crossY * matrix[1] + crossZ * matrix[2]) / scaleX
        val normalY = (crossX * matrix[3] + crossY * matrix[4] + crossZ * matrix[5]) / scaleY
        val normalZ = (crossX * matrix[6] + crossY * matrix[7] + crossZ * matrix[8]) / scaleZ
        val cubeFace = getCubeFace(normalX, normalY, normalZ)

        val scratch = FloatArray(2)
        mapRectangularUv(verticesX[a], verticesY[a], verticesZ[a], centerX, centerY, centerZ, cubeFace, matrix, direction, speed, uTrans, vTrans, scratch)
        uv[0] = scratch[0]; uv[1] = scratch[1]
        mapRectangularUv(verticesX[b], verticesY[b], verticesZ[b], centerX, centerY, centerZ, cubeFace, matrix, direction, speed, uTrans, vTrans, scratch)
        uv[2] = scratch[0]; uv[3] = scratch[1]
        mapRectangularUv(verticesX[c], verticesY[c], verticesZ[c], centerX, centerY, centerZ, cubeFace, matrix, direction, speed, uTrans, vTrans, scratch)
        uv[4] = scratch[0]; uv[5] = scratch[1]
    }

    /** Type 3: spherical projection with U (or V) seam correction. */
    private fun computeSphericalUVs(mesh: Mesh, transforms: TextureTransforms?, texture: Int, face: Int, uv: FloatArray) {
        if (transforms == null || transforms.matrix[texture].isEmpty()) return
        val matrix = transforms.matrix[texture]
        val centerX = transforms.centerX[texture]
        val centerY = transforms.centerY[texture]
        val centerZ = transforms.centerZ[texture]
        val direction = mesh.texturesDirection?.get(texture)?.toInt() ?: 0
        val speed = scaleParam(mesh.texturesSpeed, texture, 0) / 256.0f
        val a = mesh.facesA!![face].toInt() and 0xffff
        val b = mesh.facesB!![face].toInt() and 0xffff
        val c = mesh.facesC!![face].toInt() and 0xffff
        val verticesX = mesh.verticesX!!
        val verticesY = mesh.verticesY!!
        val verticesZ = mesh.verticesZ!!
        val scratch = FloatArray(2)

        mapSphericalUv(verticesX[a], verticesY[a], verticesZ[a], centerX, centerY, centerZ, matrix, direction, speed, scratch)
        uv[0] = scratch[0]; uv[1] = scratch[1]
        mapSphericalUv(verticesX[b], verticesY[b], verticesZ[b], centerX, centerY, centerZ, matrix, direction, speed, scratch)
        uv[2] = scratch[0]; uv[3] = scratch[1]
        mapSphericalUv(verticesX[c], verticesY[c], verticesZ[c], centerX, centerY, centerZ, matrix, direction, speed, scratch)
        uv[4] = scratch[0]; uv[5] = scratch[1]

        if ((direction and 0x1) == 0) {
            if (uv[2] - uv[0] > 0.5f) uv[2] -= 1.0f
            else if (uv[0] - uv[2] > 0.5f) uv[2] += 1.0f
            if (uv[4] - uv[0] > 0.5f) uv[4] -= 1.0f
            else if (uv[0] - uv[4] > 0.5f) uv[4] += 1.0f
        } else {
            if (uv[3] - uv[1] > 0.5f) uv[3] -= 1.0f
            else if (uv[1] - uv[3] > 0.5f) uv[3] += 1.0f
            if (uv[5] - uv[1] > 0.5f) uv[5] -= 1.0f
            else if (uv[1] - uv[5] > 0.5f) uv[5] += 1.0f
        }
    }

    // ------------------------------------------------------------------------
    // Per-vertex UV mapping primitives (ports of Model.mapCylindericalUv,
    // Model.mapSphericalUv, Model.mapRectangularUv, Model.getCubeFace -
    // legacy orientation, no V flip)
    // ------------------------------------------------------------------------

    private fun mapCylindricalUv(
        vertexX: Int, vertexY: Int, vertexZ: Int,
        centerX: Int, centerY: Int, centerZ: Int,
        matrix: FloatArray, offset: Float, direction: Int, speedOffset: Float,
        uv: FloatArray
    ) {
        val dx = (vertexX - centerX).toFloat()
        val dy = (vertexY - centerY).toFloat()
        val dz = (vertexZ - centerZ).toFloat()
        val y = dx * matrix[0] + dy * matrix[1] + dz * matrix[2]
        val height = dx * matrix[3] + dy * matrix[4] + dz * matrix[5]
        val x = dx * matrix[6] + dy * matrix[7] + dz * matrix[8]
        var u = atan2(y, x) / 6.2831855f + 0.5f
        if (offset != 1.0f) {
            u *= offset
        }
        val v = height + 0.5f + speedOffset
        applyDirection(direction, u, v, uv)
    }

    private fun mapSphericalUv(
        vertexX: Int, vertexY: Int, vertexZ: Int,
        centerX: Int, centerY: Int, centerZ: Int,
        matrix: FloatArray, direction: Int, speedOffset: Float,
        uv: FloatArray
    ) {
        val dx = (vertexX - centerX).toFloat()
        val dy = (vertexY - centerY).toFloat()
        val dz = (vertexZ - centerZ).toFloat()
        val tx = dx * matrix[0] + dy * matrix[1] + dz * matrix[2]
        val ty = dx * matrix[3] + dy * matrix[4] + dz * matrix[5]
        val tz = dx * matrix[6] + dy * matrix[7] + dz * matrix[8]
        val dist = sqrt(tx * tx + ty * ty + tz * tz)
        val u = atan2(tx, tz) / 6.2831855f + 0.5f
        val v = asin(ty / dist) / 3.1415927f + 0.5f + speedOffset
        applyDirection(direction, u, v, uv)
    }

    private fun mapRectangularUv(
        vertexX: Int, vertexY: Int, vertexZ: Int,
        centerX: Int, centerY: Int, centerZ: Int,
        cubeFace: Int, matrix: FloatArray, direction: Int,
        speedOffset: Float, uTrans: Float, vTrans: Float,
        uv: FloatArray
    ) {
        val dx = (vertexX - centerX).toFloat()
        val dy = (vertexY - centerY).toFloat()
        val dz = (vertexZ - centerZ).toFloat()
        val tx = dx * matrix[0] + dy * matrix[1] + dz * matrix[2]
        val ty = dx * matrix[3] + dy * matrix[4] + dz * matrix[5]
        val tz = dx * matrix[6] + dy * matrix[7] + dz * matrix[8]
        val u: Float
        val v: Float
        when (cubeFace) {
            0 -> { u = tx + speedOffset + 0.5f; v = -tz + vTrans + 0.5f }
            1 -> { u = tx + speedOffset + 0.5f; v = tz + vTrans + 0.5f }
            2 -> { u = -tx + speedOffset + 0.5f; v = -ty + uTrans + 0.5f }
            3 -> { u = tx + speedOffset + 0.5f; v = -ty + uTrans + 0.5f }
            4 -> { u = tz + vTrans + 0.5f; v = -ty + uTrans + 0.5f }
            else -> { u = -tz + vTrans + 0.5f; v = -ty + uTrans + 0.5f }
        }
        applyDirection(direction, u, v, uv)
    }

    /** Dominant-axis cube face selection (Y > Z > X tie-break, legacy order). */
    private fun getCubeFace(nx: Float, ny: Float, nz: Float): Int {
        val ax = if (nx < 0.0f) -nx else nx
        val ay = if (ny < 0.0f) -ny else ny
        val az = if (nz < 0.0f) -nz else nz
        return if (ay > ax && ay > az) {
            if (ny > 0.0f) 0 else 1
        } else if (az > ax && az > ay) {
            if (nz > 0.0f) 2 else 3
        } else {
            if (nx > 0.0f) 4 else 5
        }
    }

    /**
     * 90-degree-step UV rotation (texture direction 0-3). Legacy orientation:
     * no V flip - darkan-bot samples legacy-uploaded textures directly.
     */
    private fun applyDirection(direction: Int, u: Float, v: Float, uv: FloatArray) {
        when (direction) {
            1 -> { uv[0] = -v; uv[1] = u }
            2 -> { uv[0] = -u; uv[1] = -v }
            3 -> { uv[0] = v; uv[1] = -u }
            else -> { uv[0] = u; uv[1] = v }
        }
    }
}
