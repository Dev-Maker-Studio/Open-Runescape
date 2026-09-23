package com.jagex.game.runetek5.graphics.modern.scene

import com.jagex.game.runetek5.entity.AbstractInteractiveEntity
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.entity.gameobject.StationaryGroundDecoration
import com.jagex.game.runetek5.entity.gameobject.StationaryInteractiveObject
import com.jagex.game.runetek5.entity.gameobject.StationaryWall
import com.jagex.game.runetek5.entity.gameobject.StationaryWallDecoration
import com.jagex.game.runetek5.graphics.model.ModelGL
import com.jagex.game.runetek5.graphics.modern.batch.InstancedModelBatcher
import com.jagex.game.runetek5.graphics.modern.batch.ModelCache
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.scene.RenderedEntityCache
import com.jagex.game.runetek5.scene.RetainedSceneHook
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.toolkit.ModernGLToolkit

/**
 * Retained submission of the scene's STATIC entities for the modern toolkit.
 *
 * The legacy scene walk costs ~1 µs per visible entity per frame — visibility,
 * projection, sort, then a draw that re-derives the same matrix and re-submits the
 * same geometry — and at a dense hub that is most of the frame. Static locs, walls
 * and decorations never change between frames, so their draws are captured ONCE per
 * 8x8-tile chunk (by running each entity's own draw() with the toolkit in capture
 * mode, so the captured geometry and matrix are exactly what the walk would have
 * produced) and resubmitted per frame from here:
 *
 *  - chunks are culled against the frustum and the walk's visible window;
 *  - each instance applies the walk's plane/roof rule (Scene.isHidden) at the first
 *    footprint tile inside the window, exactly as the walk does;
 *  - translucent instances carry the walk's sort key (floorDepth), and the batchers
 *    order the whole translucent pass by that key, so they land where the walk's
 *    depth sort would have put them;
 *  - interactive instances get their picking box and RenderedEntity registered as
 *    the walk would (InteractiveEntityCache).
 *
 * Deliberately NOT reproduced: the software occlusion culling (hideTile/hideWall)
 * and the per-tile frustum grid — both only ever REMOVE geometry that is not
 * visible, so skipping them changes nothing on screen, only what the GPU discards.
 *
 * Scene mutations invalidate the affected chunk (RetainedSceneHook), and its
 * entities fall back to the walk until the chunk is recaptured on its next visit.
 * `-Drender.retained=false` disables the whole mechanism.
 */
class RetainedStaticScene(private val toolkit: ModernGLToolkit) : RetainedSceneHook {

    /** One model submission recorded while an entity's draw() ran in capture mode. */
    class Capture(
        val cached: ModelCache.CachedModel,
        val key: Long,
        /** Matrix the batcher draws with (yaw-composed for mesh-built geometry). */
        val matrix: Matrix43,
        /** The legacy model matrix, which the model's own bounds are expressed against. */
        val legacyMatrix: Matrix43,
        val model: ModelGL
    )

    private class Record(
        val entity: SceneGraphEntity,
        val cached: ModelCache.CachedModel,
        val key: Long,
        val matrix: Matrix43,
        val legacyMatrix: Matrix43,
        val model: ModelGL,
        val translucent: Boolean,
        /** Last record of its entity: registers the picking entry after the box is computed. */
        val pickable: Boolean,
        val absX: Float,
        val absY: Float,
        val absZ: Float,
        val plane: Int,
        val tx0: Int,
        val tx1: Int,
        val tz0: Int,
        val tz1: Int,
        /** World-space AABB of the model under its legacy matrix (per-instance frustum cull). */
        val bMinX: Float, val bMinY: Float, val bMinZ: Float,
        val bMaxX: Float, val bMaxY: Float, val bMaxZ: Float
    ) {
        /** Point-light selection memo (indices into the toolkit's frame light set). */
        val lightIdx = FloatArray(4)
        var lightEpoch = -1
        /** -1 unknown, else 0/1: cached so the hot loop never touches the ModelGL object. */
        var billboards = -1
        /** Translucent or billboard-bearing: never part of a chunk block. */
        var extra = false
        /** Cached legacy depth anchor (absY + minY/2). */
        var depthY = 0f
        var depthYSet = false
    }

    /** One resident mesh's packed instances inside a chunk block. */
    private class BlockGroup(val entry: InstancedModelBatcher.MeshEntry, val key: Long, val offset: Int, val count: Int)

    private class Chunk {
        val records = ArrayList<Record>(32)
        var valid = false
        // Per-chunk instance block: every opaque, billboard-free record visible under
        // the current roof/window key, packed once as INSTANCE_FLOATS per instance and
        // grouped by resident mesh, so a visible chunk costs one arraycopy per mesh
        // instead of a lookup/refit/light pass per instance.
        var blockFloats = FloatArray(0)
        val blockGroups = ArrayList<BlockGroup>(16)
        val blockRecords = ArrayList<Record>(32)
        var blockValid = false
        var blockKey = 0L
        var blockLightEpoch = -1
        /** Prebuilt opaque multi-draw for the block (bindless batcher only). */
        var blockDraw: InstancedModelBatcher.BlockDraw? = null
        // Translucent (non-billboard) records: packed instance floats, command
        // templates and material words prebuilt; only the depth key is per frame.
        val transRecords = ArrayList<Record>(16)
        var transEntries = arrayOfNulls<InstancedModelBatcher.MeshEntry>(0)
        var transFloats = FloatArray(0)
        var transCmds = IntArray(0)
        var transCmdStart = IntArray(0)
        var transCmdCount = IntArray(0)
        var transCmdTotal = 0
        var transMatIds = IntArray(0)
        var transMatWords = IntArray(0)
        var transMatRev = -1
        var transLayoutRev = -1
        var transEntryLayout = IntArray(0)
        var transValid = false
        var transLightEpoch = -1
        /** Visibility per record (chunk.records order) the block was packed for. */
        var blockMask = BooleanArray(0)
        var minX = 0f; var minY = 0f; var minZ = 0f
        var maxX = 0f; var maxY = 0f; var maxZ = 0f
    }

    private var scene: Scene? = null
    private var chunks: Array<Array<Array<Chunk?>>> = emptyArray()
    private var planes = 0
    private var chunksX = 0
    private var chunksY = 0

    private fun ensureScene(s: Scene): Boolean {
        if (scene === s) return true
        reset()
        scene = s
        planes = s.tiles.size
        chunksX = (s.sceneDimensionX + CHUNK_SIZE - 1) shr CHUNK_SHIFT
        chunksY = (s.sceneDimensionY + CHUNK_SIZE - 1) shr CHUNK_SHIFT
        chunks = Array(planes) { Array(chunksX) { arrayOfNulls<Chunk>(chunksY) } }
        return true
    }

    private fun reset() {
        for (p in chunks) for (col in p) for (c in col) if (c != null) invalidate(c)
        chunks = emptyArray()
        scene = null
    }

    private fun invalidate(chunk: Chunk) {
        val records = chunk.records
        if (records.isNotEmpty()) scene?.let { it.walkRevision++ }
        for (i in records.indices) records[i].entity.modernRetained = false
        records.clear()
        chunk.valid = false
        chunk.blockValid = false
        chunk.blockGroups.clear()
        chunk.blockRecords.clear()
        chunk.transRecords.clear()
        chunk.transValid = false
    }

    override fun active(scene: Scene): Boolean {
        // Only while THIS toolkit is the one drawing: after a runtime toolkit switch the
        // modern instance stays initialized, and taking the statics away from the walk
        // would leave the legacy renderer with nothing to draw for them.
        if (!ENABLED || Renderers.activeToolkit !== toolkit) return false
        if (!toolkit.isModernRenderingActive() || !toolkit.canSubmitRetained()) return false
        val ok = ensureScene(scene)
        if (ok && OCCLUSION_OFF && scene.culling.cullingEnabled) {
            // The software occluder pass (Culling.processCulling + per-entity hideTile)
            // only ever removes geometry that is already invisible; with statics
            // resubmitted from here it costs more per frame than the overdraw it saves.
            scene.culling.cullingEnabled = false
        }
        return ok
    }

    override fun chunkChanged(scene: Scene, plane: Int, tileX: Int, tileY: Int) {
        if (scene !== this.scene) return
        if (plane < 0 || plane >= planes) return
        val cx = tileX shr CHUNK_SHIFT
        val cy = tileY shr CHUNK_SHIFT
        if (cx < 0 || cy < 0 || cx >= chunksX || cy >= chunksY) return
        chunks[plane][cx][cy]?.let { invalidate(it) }
    }

    override fun sceneReset(scene: Scene) {
        if (scene === this.scene) reset()
    }

    override fun submitStatics(
        scene: Scene,
        settingBits: Array<Array<ByteArray>>?,
        lowestRenderedPlane: Int,
        roofPreference: Byte
    ) {
        capturesThisFrame = 0
        if (scene !== this.scene) return
        val winX0 = scene.visibleToXnegative
        val winX1 = scene.visibleToXpositive
        val winY0 = scene.visibleToYnegative
        val winY1 = scene.visibleToYpositive
        if (winX1 <= winX0 || winY1 <= winY0) return
        val cx0 = winX0 shr CHUNK_SHIFT
        val cx1 = minOf(chunksX - 1, (winX1 - 1) shr CHUNK_SHIFT)
        val cy0 = winY0 shr CHUNK_SHIFT
        val cy1 = minOf(chunksY - 1, (winY1 - 1) shr CHUNK_SHIFT)
        val picking = scene.interactiveEntities
        val lightEpoch = toolkit.lightEpochNow()
        val batcher = toolkit.retainedBatcher()
        if (DIAG) { diagTick++; diagLines = 0 }
        for (plane in 0 until planes) {
            val planeChunks = chunks[plane]
            for (cx in cx0..cx1) {
                val column = planeChunks[cx]
                for (cy in cy0..cy1) {
                    var chunk = column[cy]
                    if (chunk == null) {
                        chunk = Chunk()
                        column[cy] = chunk
                    }
                    if (!chunk.valid) {
                        // Capturing every chunk of a fresh region in one frame is a
                        // ~250 ms stall. Spread it: an uncaptured chunk's entities are
                        // simply not retained yet, so the walk draws them this frame.
                        if (capturesThisFrame >= CAPTURE_BUDGET) continue
                        capturesThisFrame++
                        captureChunk(scene, chunk, plane, cx, cy)
                    }
                    val records = chunk.records
                    if (records.isEmpty()) continue
                    val visibleChunk = toolkit.isWorldAabbVisible(chunk.minX, chunk.minY, chunk.minZ, chunk.maxX, chunk.maxY, chunk.maxZ)
                    if (DIAG && diagTick % 300 == 150 && diagLines < 12) {
                        diagLines++
                        System.err.println("[RetainedDiag] plane=$plane chunk=($cx,$cy) records=${records.size} visible=$visibleChunk " +
                            "bounds=(${chunk.minX},${chunk.minY},${chunk.minZ})-(${chunk.maxX},${chunk.maxY},${chunk.maxZ}) cam=(${scene.cameraPositionX},${scene.cameraPositionY},${scene.cameraPositionZ}) win=($winX0..$winX1,$winY0..$winY1)")
                    }
                    if (!visibleChunk) {
                        toolkit.retainedChunksCulled++
                        continue
                    }
                    // A chunk wholly inside the frustum needs no per-instance tests.
                    val chunkInside = toolkit.isWorldAabbFullyInside(chunk.minX, chunk.minY, chunk.minZ, chunk.maxX, chunk.maxY, chunk.maxZ)
                    // Picking data (bounding box + entity ref) only matters for entities the
                    // cursor can be over; a chunk whose screen bounds exclude the cursor skips it.
                    val pickHere = chunkNearMouse(chunk)
                    var usedBlock = false
                    if (BLOCKS && batcher != null) {
                        // Roof hiding stamps settingBits with the frame byte (roofPreference)
                        // every frame, so it cannot be part of a key: the block is keyed on
                        // the window/plane and its per-record visibility is re-derived each
                        // frame and compared with the mask it was packed for; only a change
                        // repacks.
                        val visKey = (winX0.toLong() and 0xff) or ((winX1.toLong() and 0xff) shl 8) or
                            ((winY0.toLong() and 0xff) shl 16) or ((winY1.toLong() and 0xff) shl 24) or
                            ((lowestRenderedPlane.toLong() and 0xf) shl 32)
                        if (chunk.blockValid && chunk.blockKey == visKey &&
                            !blockMaskUnchanged(chunk, winX0, winX1, winY0, winY1, settingBits, lowestRenderedPlane, roofPreference)
                        ) {
                            chunk.blockValid = false
                        }
                        if (!chunk.blockValid || chunk.blockKey != visKey) {
                            buildBlock(chunk, visKey, winX0, winX1, winY0, winY1, settingBits, lowestRenderedPlane, roofPreference, lightEpoch)
                        }
                        if (chunk.blockValid) {
                            if (chunk.blockLightEpoch != lightEpoch) repackBlockLights(chunk, lightEpoch)
                            usedBlock = submitBlock(chunk, batcher)
                            if (!usedBlock) toolkit.retainedBlockFallbacks++
                        }
                    }
                    var usedTrans = false
                    if (usedBlock && TRANS_TPL && batcher != null && batcher.bindlessActive && chunk.transRecords.isNotEmpty()) {
                        if (!transTemplatesValid(chunk, batcher)) buildTransTemplates(chunk, batcher)
                        if (chunk.transValid) {
                            if (chunk.transMatRev != batcher.materialWordsRevision) {
                                val complete = toolkit.fillMaterialWords(batcher, chunk.transMatIds, chunk.transCmdTotal, chunk.transMatWords)
                                chunk.transMatRev = if (complete) batcher.materialWordsRevision else -1
                            }
                            if (chunk.transLightEpoch != lightEpoch) repackTransLights(chunk, lightEpoch)
                            val tl = chunk.transRecords
                            val stride = InstancedModelBatcher.INSTANCE_FLOATS
                            var sent = 0
                            for (k in tl.indices) {
                                val r = tl[k]
                                if (!recordVisible(r, winX0, winX1, winY0, winY1, settingBits, lowestRenderedPlane, roofPreference)) continue
                                if (!chunkInside && !toolkit.isWorldAabbVisible(r.bMinX, r.bMinY, r.bMinZ, r.bMaxX, r.bMaxY, r.bMaxZ)) continue
                                if (!r.depthYSet) { r.depthY = r.absY + (r.entity.getMinY() shr 1); r.depthYSet = true }
                                val depth = toolkit.retainedDepth(r.absX, r.depthY, r.absZ)
                                val cs = chunk.transCmdStart[k]
                                if (batcher.appendTranslucentTemplate(chunk.transEntries[k]!!, chunk.transFloats, k * stride, toolkit.nextTranslucentSeq(), depth,
                                        chunk.transCmds, cs * 5, chunk.transCmdCount[k], chunk.transMatWords, cs * 4)) sent++
                            }
                            usedTrans = true
                            toolkit.retainedTransTpl += sent
                            toolkit.retainedSubmitted += sent
                        }
                    }
                    var i = 0
                    var recaptured = false
                    while (i < records.size) {
                        val r = records[i]
                        i++
                        val pickThis = r.pickable && pickHere
                        if (usedBlock && !r.extra && !pickThis) continue
                        val inTrans = usedTrans && r.translucent && r.billboards == 0
                        if (inTrans && !pickThis) continue
                        if (SKIP_TRANS_TEST && r.translucent && !pickThis) continue
                        // The walk decides visibility at the first footprint tile inside
                        // the window (Scene.isHidden): draw iff the plane/roof rule holds
                        // there. No tile in the window means hidden.
                        var visible = false
                        var decided = false
                        var fx = r.tx0
                        while (fx <= r.tx1 && !decided) {
                            if (fx >= winX0 && fx < winX1) {
                                var fz = r.tz0
                                while (fz <= r.tz1) {
                                    if (fz >= winY0 && fz < winY1) {
                                        visible = settingBits == null || r.plane < lowestRenderedPlane ||
                                            roofPreference != settingBits[r.plane][fx][fz]
                                        decided = true
                                        break
                                    }
                                    fz++
                                }
                            }
                            fx++
                        }
                        if (!visible) continue
                        // Per-instance frustum test, the same conservative AABB test the
                        // walk's model submission applies (isModelVisible).
                        if (!chunkInside && !toolkit.isWorldAabbVisible(r.bMinX, r.bMinY, r.bMinZ, r.bMaxX, r.bMaxY, r.bMaxZ)) {
                            toolkit.retainedInstancesCulled++
                            continue
                        }
                        if (r.cached.evicted) {
                            if (recaptured) continue
                            recaptured = true
                            invalidate(chunk)
                            captureChunk(scene, chunk, plane, cx, cy)
                            i--
                            continue
                        }
                        if (r.billboards < 0) r.billboards = if (r.model.billboardCount > 0) 1 else 0
                        val hasBillboards = r.billboards == 1
                        val inBlock = (usedBlock && !r.extra) || inTrans
                        val needsDepth = r.translucent || pickThis || hasBillboards
                        val depth = if (needsDepth) {
                            toolkit.retainedDepth(r.absX, r.absY + (r.entity.getMinY() shr 1), r.absZ)
                        } else 0
                        // Light selection memoised on the record: touching the ModelGL memo
                        // for every instance was a cache-miss per record (13% of the spin frame).
                        if (!inBlock) {
                            if (r.lightEpoch != lightEpoch) {
                                toolkit.selectLightsInto(r.matrix.xD, r.matrix.yD, r.matrix.zD, r.lightIdx)
                                r.lightEpoch = lightEpoch
                            }
                            // Translucent records whose mesh is resident skip the per-instance
                            // lookup/refit path: just the ordered append with this frame's key.
                            val te = if (r.translucent && batcher != null) r.cached.instanceEntry as? InstancedModelBatcher.MeshEntry else null
                            if (te != null && batcher != null && te.resident && te.modelId == r.key &&
                                batcher.appendTranslucentEntry(te, r.matrix, r.lightIdx, true, toolkit.nextTranslucentSeq(), depth)
                            ) {
                                toolkit.retainedTranslucentFast++
                            } else {
                                toolkit.submitRetainedLit(r.cached, r.key, r.matrix, r.translucent, depth, r.lightIdx)
                            }
                            toolkit.retainedSubmitted++
                            if (hasBillboards) toolkit.submitRetainedBillboards(r.model, r.legacyMatrix, depth)
                        }
                        if (pickThis) {
                            val boxes = r.entity.boundingBoxPolygons
                            if (boxes != null && boxes.isNotEmpty()) {
                                r.model.calculateBoundingBoxForModern(r.legacyMatrix, boxes[0])
                                r.entity.floorDepth = depth
                                val ref = RenderedEntityCache.generate(true)
                                ref.entity = r.entity
                                picking.add(ref)
                            }
                        }
                    }
                }
            }
        }
    }

    private val mouseProj = FloatArray(3)
    /** Conservative: true unless the chunk's projected AABB lies clearly away from the cursor. */
    private fun chunkNearMouse(chunk: Chunk): Boolean {
        if (!PICK_GATE) return true
        val recorder = MouseRecord.recorder ?: return true
        val mx = recorder.getMouseX().toFloat(); val my = recorder.getMouseY().toFloat()
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        val p = mouseProj
        for (corner in 0 until 8) {
            val x = if (corner and 1 == 0) chunk.minX else chunk.maxX
            val y = if (corner and 2 == 0) chunk.minY else chunk.maxY
            val z = if (corner and 4 == 0) chunk.minZ else chunk.maxZ
            toolkit.calculateProjection(x, y, z, p)
            if (!(p[2] > 1f)) return true
            if (p[0] < minX) minX = p[0]; if (p[0] > maxX) maxX = p[0]
            if (p[1] < minY) minY = p[1]; if (p[1] > maxY) maxY = p[1]
        }
        val m = PICK_MARGIN
        return mx >= minX - m && mx <= maxX + m && my >= minY - m && my <= maxY + m
    }

    /** Roof/window visibility of a record, exactly the per-record rule of the fallback loop. */
    private fun recordVisible(r: Record, winX0: Int, winX1: Int, winY0: Int, winY1: Int,
                              settingBits: Array<Array<ByteArray>>?, lowestRenderedPlane: Int, roofPreference: Byte): Boolean {
        var fx = r.tx0
        while (fx <= r.tx1) {
            if (fx >= winX0 && fx < winX1) {
                var fz = r.tz0
                while (fz <= r.tz1) {
                    if (fz >= winY0 && fz < winY1) {
                        return settingBits == null || r.plane < lowestRenderedPlane ||
                            roofPreference != settingBits[r.plane][fx][fz]
                    }
                    fz++
                }
            }
            fx++
        }
        return false
    }

    /**
     * Packs the chunk's block for [visKey]. Leaves [Chunk.blockValid] false when some
     * record has no resident mesh yet (the fallback loop resolves it this frame).
     */
    private fun buildBlock(chunk: Chunk, visKey: Long, winX0: Int, winX1: Int, winY0: Int, winY1: Int,
                           settingBits: Array<Array<ByteArray>>?, lowestRenderedPlane: Int, roofPreference: Byte, lightEpoch: Int) {
        chunk.blockValid = false
        chunk.blockGroups.clear()
        chunk.blockRecords.clear()
        val records = chunk.records
        if (chunk.blockMask.size < records.size) chunk.blockMask = BooleanArray(records.size)
        val mask = chunk.blockMask
        val transList = chunk.transRecords
        transList.clear()
        chunk.transValid = false
        val byEntry = LinkedHashMap<InstancedModelBatcher.MeshEntry, ArrayList<Record>>()
        for (i in records.indices) {
            val r = records[i]
            if (r.billboards < 0) r.billboards = if (r.model.billboardCount > 0) 1 else 0
            r.extra = r.translucent || r.billboards == 1
            val vis = !r.extra && recordVisible(r, winX0, winX1, winY0, winY1, settingBits, lowestRenderedPlane, roofPreference)
            mask[i] = vis
            if (r.translucent && r.billboards == 0) transList.add(r)
            if (!vis) continue
            val entry = r.cached.instanceEntry as? InstancedModelBatcher.MeshEntry ?: return
            if (!entry.resident || entry.modelId != r.key) return
            byEntry.getOrPut(entry) { ArrayList(4) }.add(r)
        }
        var total = 0
        for (list in byEntry.values) total += list.size
        val stride = InstancedModelBatcher.INSTANCE_FLOATS
        if (chunk.blockFloats.size < total * stride) chunk.blockFloats = FloatArray(total * stride)
        val f = chunk.blockFloats
        var n = 0
        for ((entry, list) in byEntry) {
            chunk.blockGroups.add(BlockGroup(entry, entry.modelId, n, list.size))
            for (r in list) {
                val m = r.matrix
                val b = n * stride
                f[b] = m.xA; f[b + 1] = m.yA; f[b + 2] = m.zA; f[b + 3] = 0f
                f[b + 4] = m.xB; f[b + 5] = m.yB; f[b + 6] = m.zB; f[b + 7] = 0f
                f[b + 8] = m.xC; f[b + 9] = m.yC; f[b + 10] = m.zC; f[b + 11] = 0f
                f[b + 12] = m.xD; f[b + 13] = m.yD; f[b + 14] = m.zD; f[b + 15] = 1f
                if (r.lightEpoch != lightEpoch) {
                    toolkit.selectLightsInto(m.xD, m.yD, m.zD, r.lightIdx)
                    r.lightEpoch = lightEpoch
                }
                f[b + 16] = r.lightIdx[0]; f[b + 17] = r.lightIdx[1]; f[b + 18] = r.lightIdx[2]; f[b + 19] = r.lightIdx[3]
                chunk.blockRecords.add(r)
                n++
            }
        }
        chunk.blockKey = visKey
        chunk.blockLightEpoch = lightEpoch
        chunk.blockValid = true
        chunk.blockDraw = null
        // Translucent records: pack once (matrix + lights); templates come later.
        val tn = transList.size
        if (chunk.transFloats.size < tn * stride) chunk.transFloats = FloatArray(tn * stride)
        val tf = chunk.transFloats
        for (k in 0 until tn) {
            val r = transList[k]
            val m = r.matrix
            val b = k * stride
            tf[b] = m.xA; tf[b + 1] = m.yA; tf[b + 2] = m.zA; tf[b + 3] = 0f
            tf[b + 4] = m.xB; tf[b + 5] = m.yB; tf[b + 6] = m.zB; tf[b + 7] = 0f
            tf[b + 8] = m.xC; tf[b + 9] = m.yC; tf[b + 10] = m.zC; tf[b + 11] = 0f
            tf[b + 12] = m.xD; tf[b + 13] = m.yD; tf[b + 14] = m.zD; tf[b + 15] = 1f
            if (r.lightEpoch != lightEpoch) {
                toolkit.selectLightsInto(m.xD, m.yD, m.zD, r.lightIdx)
                r.lightEpoch = lightEpoch
            }
            tf[b + 16] = r.lightIdx[0]; tf[b + 17] = r.lightIdx[1]; tf[b + 18] = r.lightIdx[2]; tf[b + 19] = r.lightIdx[3]
        }
        chunk.transLightEpoch = lightEpoch
        toolkit.retainedBlockBuilds++
    }

    /** Command templates for the chunk's translucent records; false if some mesh is not resident yet. */
    private fun buildTransTemplates(chunk: Chunk, batcher: InstancedModelBatcher): Boolean {
        chunk.transValid = false
        val list = chunk.transRecords
        val n = list.size
        if (chunk.transEntries.size < n) { chunk.transEntries = arrayOfNulls(n); chunk.transEntryLayout = IntArray(n); chunk.transCmdStart = IntArray(n); chunk.transCmdCount = IntArray(n) }
        var total = 0
        for (k in 0 until n) {
            val r = list[k]
            val e = r.cached.instanceEntry as? InstancedModelBatcher.MeshEntry ?: return false
            if (!e.resident || e.modelId != r.key) return false
            chunk.transEntries[k] = e
            chunk.transEntryLayout[k] = e.layoutStamp
            chunk.transCmdStart[k] = total
            chunk.transCmdCount[k] = e.materialBatches.size
            total += e.materialBatches.size
        }
        if (chunk.transCmds.size < total * 5) { chunk.transCmds = IntArray(total * 5); chunk.transMatIds = IntArray(total); chunk.transMatWords = IntArray(total * 4) }
        var c = 0
        for (k in 0 until n) {
            val batches = chunk.transEntries[k]!!.materialBatches
            for (bi in batches.indices) {
                val batch = batches[bi]
                chunk.transCmds[c * 5] = batch.indexCount; chunk.transCmds[c * 5 + 1] = 1; chunk.transCmds[c * 5 + 2] = batch.indexOffset; chunk.transCmds[c * 5 + 3] = 0; chunk.transCmds[c * 5 + 4] = 0
                chunk.transMatIds[c] = batch.materialId
                c++
            }
        }
        chunk.transCmdTotal = total
        chunk.transLayoutRev = batcher.storeLayoutRevision
        chunk.transMatRev = -1
        chunk.transValid = true
        return true
    }

    private fun transTemplatesValid(chunk: Chunk, batcher: InstancedModelBatcher): Boolean {
        if (!chunk.transValid || chunk.transLayoutRev != batcher.storeLayoutRevision) return false
        val es = chunk.transEntries
        val ls = chunk.transEntryLayout
        for (k in 0 until chunk.transRecords.size) {
            val e = es[k] ?: return false
            if (!e.resident || e.layoutStamp != ls[k]) return false
        }
        return true
    }

    private fun repackTransLights(chunk: Chunk, lightEpoch: Int) {
        val stride = InstancedModelBatcher.INSTANCE_FLOATS
        val f = chunk.transFloats
        val list = chunk.transRecords
        for (k in list.indices) {
            val r = list[k]
            val m = r.matrix
            if (r.lightEpoch != lightEpoch) {
                toolkit.selectLightsInto(m.xD, m.yD, m.zD, r.lightIdx)
                r.lightEpoch = lightEpoch
            }
            val b = k * stride
            f[b + 16] = r.lightIdx[0]; f[b + 17] = r.lightIdx[1]; f[b + 18] = r.lightIdx[2]; f[b + 19] = r.lightIdx[3]
        }
        chunk.transLightEpoch = lightEpoch
    }

    /** Builds (or rebuilds) the chunk's command template from its groups' current arena layout. */
    private fun buildBlockDraw(chunk: Chunk, batcher: InstancedModelBatcher): InstancedModelBatcher.BlockDraw {
        val b = chunk.blockDraw ?: InstancedModelBatcher.BlockDraw().also { chunk.blockDraw = it }
        val groups = chunk.blockGroups
        var nCmd = 0
        for (i in groups.indices) nCmd += groups[i].entry.materialBatches.size
        if (b.cmds.size < nCmd * 5) { b.cmds = IntArray(nCmd * 5); b.mats = IntArray(nCmd) }
        if (b.entryLayout.size < groups.size) b.entryLayout = IntArray(groups.size)
        b.entries.clear()
        var c = 0; var k = 0
        for (i in groups.indices) {
            val g = groups[i]
            val e = g.entry
            b.entries.add(e)
            b.entryLayout[i] = e.layoutStamp
            val batches = e.materialBatches
            for (bi in batches.indices) {
                val batch = batches[bi]
                b.cmds[c] = batch.indexCount; b.cmds[c + 1] = g.count; b.cmds[c + 2] = batch.indexOffset; b.cmds[c + 3] = 0; b.cmds[c + 4] = g.offset
                b.mats[k++] = batch.materialId
                c += 5
            }
        }
        b.cmdCount = nCmd
        b.matWordsRevision = -1
        b.floats = chunk.blockFloats
        b.count = chunk.blockRecords.size
        b.layoutRevision = batcher.storeLayoutRevision
        return b
    }

    /** True when every block-eligible record's roof visibility still matches the packed mask. */
    private fun blockMaskUnchanged(chunk: Chunk, winX0: Int, winX1: Int, winY0: Int, winY1: Int,
                                   settingBits: Array<Array<ByteArray>>?, lowestRenderedPlane: Int, roofPreference: Byte): Boolean {
        val records = chunk.records
        val mask = chunk.blockMask
        if (mask.size < records.size) return false
        for (i in records.indices) {
            val r = records[i]
            if (r.extra) continue
            if (mask[i] != recordVisible(r, winX0, winX1, winY0, winY1, settingBits, lowestRenderedPlane, roofPreference)) return false
        }
        return true
    }

    private fun repackBlockLights(chunk: Chunk, lightEpoch: Int) {
        val stride = InstancedModelBatcher.INSTANCE_FLOATS
        val f = chunk.blockFloats
        val list = chunk.blockRecords
        for (i in list.indices) {
            val r = list[i]
            val m = r.matrix
            if (r.lightEpoch != lightEpoch) {
                toolkit.selectLightsInto(m.xD, m.yD, m.zD, r.lightIdx)
                r.lightEpoch = lightEpoch
            }
            val b = i * stride
            f[b + 16] = r.lightIdx[0]; f[b + 17] = r.lightIdx[1]; f[b + 18] = r.lightIdx[2]; f[b + 19] = r.lightIdx[3]
        }
        chunk.blockLightEpoch = lightEpoch
    }

    /** Appends the whole block; validates every group first so a failure never leaves a partial submission. */
    private fun submitBlock(chunk: Chunk, batcher: InstancedModelBatcher): Boolean {
        val groups = chunk.blockGroups
        for (i in groups.indices) {
            val g = groups[i]
            if (!g.entry.resident || g.entry.modelId != g.key || !batcher.canAddPacked(g.entry, g.count)) {
                chunk.blockValid = false
                return false
            }
        }
        if (BLOCK_DRAW && batcher.bindlessActive) {
            var b = chunk.blockDraw
            if (b == null || !batcher.blockDrawValid(b)) b = buildBlockDraw(chunk, batcher)
            if (batcher.submitBlockDraw(b)) {
                toolkit.retainedBlockInstances += b.count
                toolkit.retainedSubmitted += b.count
                toolkit.retainedBlockDraws++
                return true
            }
        }
        val stride = InstancedModelBatcher.INSTANCE_FLOATS
        var n = 0
        for (i in groups.indices) {
            val g = groups[i]
            batcher.addInstancesPacked(g.entry, chunk.blockFloats, g.offset * stride, g.count)
            n += g.count
        }
        toolkit.retainedBlockInstances += n
        toolkit.retainedSubmitted += n
        return true
    }

    private fun captureChunk(scene: Scene, chunk: Chunk, plane: Int, cx: Int, cy: Int) {
        chunk.records.clear()
        chunk.valid = true
        chunk.minX = Float.MAX_VALUE; chunk.minY = Float.MAX_VALUE; chunk.minZ = Float.MAX_VALUE
        chunk.maxX = -Float.MAX_VALUE; chunk.maxY = -Float.MAX_VALUE; chunk.maxZ = -Float.MAX_VALUE
        toolkit.retainedCaptures++
        val planeTiles = scene.tiles[plane]
        val x0 = cx shl CHUNK_SHIFT
        val y0 = cy shl CHUNK_SHIFT
        val x1 = minOf(x0 + CHUNK_SIZE, scene.sceneDimensionX)
        val y1 = minOf(y0 + CHUNK_SIZE, scene.sceneDimensionY)
        for (x in x0 until x1) {
            val column = planeTiles[x]
            for (y in y0 until y1) {
                val tile = column[y] ?: continue
                (tile.primaryWall as? StationaryWall)?.let { captureEntity(scene, chunk, it, x, y) }
                (tile.secondaryWall as? StationaryWall)?.let { captureEntity(scene, chunk, it, x, y) }
                (tile.primaryWallDecoration as? StationaryWallDecoration)?.let { captureEntity(scene, chunk, it, x, y) }
                (tile.secondaryWallDecoration as? StationaryWallDecoration)?.let { captureEntity(scene, chunk, it, x, y) }
                (tile.groundDecoration as? StationaryGroundDecoration)?.let { captureEntity(scene, chunk, it, x, y) }
                var container = tile.interactives
                while (container != null) {
                    val interactive = container.interactiveObject
                    // Multi-tile locs sit in every tile they cover; capture at their origin only.
                    if (interactive is StationaryInteractiveObject &&
                        interactive.startX.toInt() == x && interactive.startZ.toInt() == y
                    ) {
                        captureEntity(scene, chunk, interactive, x, y)
                    }
                    container = container.child
                }
            }
        }
        if (chunk.records.isEmpty()) {
            chunk.minX = 0f; chunk.minY = 0f; chunk.minZ = 0f
            chunk.maxX = 0f; chunk.maxY = 0f; chunk.maxZ = 0f
        }
    }

    private fun captureEntity(scene: Scene, chunk: Chunk, entity: SceneGraphEntity, tileX: Int, tileY: Int) {
        if (entity.modernRetained) scene.walkRevision++
        entity.modernRetained = false
        // Animated textures scroll their UVs per frame through the live path; leave those to the walk.
        if (!entity.disregardTextureMovement()) return
        val captures = toolkit.beginCapture()
        val ref = entity.draw(toolkit)
        val unsupported = toolkit.endCapture()
        val pickable = ref?.interactive == true
        if (ref != null) RenderedEntityCache.clear(ref)
        if (unsupported || captures.isEmpty()) return
        val abs = entity.getAbsolutePosition().coords
        val translucent = entity.hasAlpha()
        val plane = entity.plane.toInt()
        val tx0: Int; val tx1: Int; val tz0: Int; val tz1: Int
        if (entity is AbstractInteractiveEntity) {
            tx0 = entity.startX.toInt(); tx1 = entity.endX.toInt()
            tz0 = entity.startZ.toInt(); tz1 = entity.endZ.toInt()
        } else {
            tx0 = tileX; tx1 = tileX; tz0 = tileY; tz1 = tileY
        }
        if (plane < 0 || plane >= planes) return
        for (ci in captures.indices) {
            val c = captures[ci]
            worldBounds(c.model, c.legacyMatrix)
            val record = Record(
                entity, c.cached, c.key, c.matrix, c.legacyMatrix, c.model, translucent,
                pickable && ci == captures.size - 1,
                abs.x, abs.y, abs.z, plane, tx0, tx1, tz0, tz1,
                boundsScratch[0], boundsScratch[1], boundsScratch[2],
                boundsScratch[3], boundsScratch[4], boundsScratch[5]
            )
            chunk.records.add(record)
            if (record.bMinX < chunk.minX) chunk.minX = record.bMinX
            if (record.bMinY < chunk.minY) chunk.minY = record.bMinY
            if (record.bMinZ < chunk.minZ) chunk.minZ = record.bMinZ
            if (record.bMaxX > chunk.maxX) chunk.maxX = record.bMaxX
            if (record.bMaxY > chunk.maxY) chunk.maxY = record.bMaxY
            if (record.bMaxZ > chunk.maxZ) chunk.maxZ = record.bMaxZ
        }
        entity.modernRetained = true
        scene.walkRevision++
    }

    private val boundsScratch = FloatArray(6)
    private var capturesThisFrame = 0

    /** World AABB of the model's bounds under its legacy matrix, into [boundsScratch] (min xyz, max xyz). */
    private fun worldBounds(model: ModelGL, m: Matrix43) {
        val minX = model.getMinX().toFloat(); val maxX = model.getMaxX().toFloat()
        val minY = model.getMinY().toFloat(); val maxY = model.getMaxY().toFloat()
        val minZ = model.getMinZ().toFloat(); val maxZ = model.getMaxZ().toFloat()
        var bx0 = Float.MAX_VALUE; var by0 = Float.MAX_VALUE; var bz0 = Float.MAX_VALUE
        var bx1 = -Float.MAX_VALUE; var by1 = -Float.MAX_VALUE; var bz1 = -Float.MAX_VALUE
        for (corner in 0 until 8) {
            val lx = if (corner and 1 == 0) minX else maxX
            val ly = if (corner and 2 == 0) minY else maxY
            val lz = if (corner and 4 == 0) minZ else maxZ
            val wx = lx * m.xA + ly * m.xB + lz * m.xC + m.xD
            val wy = lx * m.yA + ly * m.yB + lz * m.yC + m.yD
            val wz = lx * m.zA + ly * m.zB + lz * m.zC + m.zD
            if (wx < bx0) bx0 = wx
            if (wx > bx1) bx1 = wx
            if (wy < by0) by0 = wy
            if (wy > by1) by1 = wy
            if (wz < bz0) bz0 = wz
            if (wz > bz1) bz1 = wz
        }
        boundsScratch[0] = bx0; boundsScratch[1] = by0; boundsScratch[2] = bz0
        boundsScratch[3] = bx1; boundsScratch[4] = by1; boundsScratch[5] = bz1
    }

    private var diagTick = 0
    private var diagLines = 0

    companion object {
        /** Chunk captures allowed per frame (-Drender.capturebudget=N). */
        /** -Drender.retainedblocks=false restores per-record submission. */
        private val BLOCKS = System.getProperty("render.retainedblocks")?.equals("false", true) != true
        /** -Drender.blockdraw=false submits blocks per mesh instead of as command templates. */
        private val BLOCK_DRAW = System.getProperty("render.blockdraw")?.equals("false", true) != true
        /** -Drender.transtpl=false: translucent retained records through the per-record path. */
        private val TRANS_TPL = System.getProperty("render.transtpl")?.equals("false", true) != true
        /** TEST ONLY (-Drender.skiptrans): drops translucent retained records to measure their cost. */
        private val SKIP_TRANS_TEST = System.getProperty("render.skiptrans") != null
        /** -Drender.pickgate=false computes picking data for every visible pickable record. */
        private val PICK_GATE = System.getProperty("render.pickgate")?.equals("false", true) != true
        private val PICK_MARGIN = (System.getProperty("render.pickmargin")?.toIntOrNull() ?: 48).toFloat()
        private val CAPTURE_BUDGET = System.getProperty("render.capturebudget")?.toIntOrNull() ?: 6
        private val DIAG = System.getProperty("render.retaineddiag") != null
        const val CHUNK_SHIFT = 3
        const val CHUNK_SIZE = 1 shl CHUNK_SHIFT

        /** -Drender.retained=false hands every entity back to the legacy walk. */
        @JvmStatic val ENABLED = System.getProperty("render.retained")?.equals("false", true) != true
        /** -Drender.occlusion=true keeps the legacy software occluder pass running. */
        @JvmStatic val OCCLUSION_OFF = System.getProperty("render.occlusion")?.equals("true", true) != true
    }
}
