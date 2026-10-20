package com.jagex.game.runetek5.graphics.modern.debug

import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask
import com.jagex.game.runetek5.graphics.model.ModelGL
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.ColorUtil
import com.jagex.game.runetek5.utils.MathUtils

object GeometryDiff {

    private var hasRun = false

    private const val MAX_REPORTS = 60

    private const val RETAIN_MASK =
        ModelFunctionMask.PROJECTION or
            ModelFunctionMask.ALTERS_COLOR or
            ModelFunctionMask.ALTERS_ALPHA or
            ModelFunctionMask.HAS_TEXTURE_COORDS

    private const val WALL_MASK = ModelFunctionMask.HAS_TEXTURE_COORDS

    private val setting: String? by lazy { System.getProperty("geodiff")?.trim() }

    private fun locCount(locList: ObjectTypeList): Int {
        val config = locList.configResource ?: return 0
        val lastArchive = config.getArchiveCount() - 1
        if (lastArchive < 0) return 0
        val capacity = SharedConfigsType.OBJECTS.archiveFileCapacity()
        return lastArchive * capacity + config.getArchiveFileCount(lastArchive)
    }

    fun runOnce(toolkit: AbstractToolkit) {
        if (hasRun) return
        val mode = setting ?: return
        val locList = TypeLists.LOC ?: return
        hasRun = true

        val locIds: IntArray = if (mode.equals("scan", ignoreCase = true)) {
            val count = locCount(locList)
            System.err.println("[GeoDiff] scanning all $count locs")
            IntArray(count) { it }
        } else {
            mode.split(',').mapNotNull { it.trim().toIntOrNull() }.toIntArray()
        }
        if (locIds.isEmpty()) {
            System.err.println("[GeoDiff] nothing to scan (geodiff=$mode)")
            return
        }

        val startNs = System.nanoTime()
        System.err.println("[GeoDiff] ==== start (${locIds.size} locs) ====")
        for (locId in locIds) {
            try {
                diffLoc(toolkit, locList.list(locId), locId)
            } catch (e: Throwable) {
                stat.buildErrors++
            }
        }
        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
        System.err.println(
            "[GeoDiff] ==== end in ${elapsedMs}ms — models=${stat.models} " +
                "meshBuilt=${stat.meshBuilt} legacyCapture=${stat.legacyCapture} " +
                "countMISMATCH=${stat.mismatches} (locs=${stat.mismatchLocs.size}) " +
                "posMISMATCH=${stat.posMismatches} (locs=${stat.posMismatchLocs.size}) " +
                "colourMISMATCH=${stat.colourMismatches} (locs=${stat.colourMismatchLocs.size}) " +
                "posSkipped=${stat.posSkipped} colourSkipped=${stat.colourSkipped} buildErrors=${stat.buildErrors} pairModels=${stat.pairModels} pairErrors=${stat.pairErrors} ===="
        )
        if (stat.colourMismatchLocs.isNotEmpty()) {
            System.err.println(
                "[GeoDiff] colour-mismatching locs: " +
                    stat.colourMismatchLocs.sorted().take(60).joinToString(",")
            )
        }
        if (stat.mismatchNames.isNotEmpty()) {
            System.err.println("[GeoDiff] diverging locs by name (${stat.mismatchNames.size}):")
            stat.mismatchNames.sorted().chunked(6).take(40).forEach {
                System.err.println("[GeoDiff]   " + it.joinToString(" | "))
            }
        }
    }

    @JvmStatic
    fun verifyLive(model: ModelGL, built: com.jagex.game.runetek5.graphics.modern.util.ModernModelBuilder.BuiltModel) {
        if (liveBudget <= 0) return

        liveCountChecked++
        contrastHist[model.contrast.toInt()] = (contrastHist[model.contrast.toInt()] ?: 0) + 1
        if (built.faceCount != model.renderedFaceCount) {
            liveCountMismatch++
            if (liveCountReported++ < 10) {
                System.err.println(
                    "[LiveDiff] FACECOUNT mesh=${built.faceCount} legacy=${model.renderedFaceCount} " +
                        "delta=${built.faceCount - model.renderedFaceCount} mask=${model.functionMask} " +
                        "yaw=${model.modernYaw} src=${built.sourceFaceCount} " +
                        "skips[bb=${built.skipBillboardHidden} rt=${built.skipRenderType} " +
                        "bv=${built.skipInvalidVertex} tr=${built.skipTransparent} " +
                        "mat=${built.skipMaterialFiltered}]"
                )
            }
        }

        run {
            val tu = model.textureU
            val tv = model.textureV
            val fa = model.facesA
            if (tu != null && tv != null && fa != null) {
                val stride = com.jagex.game.runetek5.graphics.modern.batch.ModelCache.BYTES_PER_VERTEX
                val data = java.nio.ByteBuffer.wrap(built.vertexData).order(java.nio.ByteOrder.nativeOrder())
                var uvBad = 0
                val n = minOf(built.faceCount, model.renderedFaceCount)
                for (f in 0 until n) {
                    val slot = fa[f].toInt() and 0xFFFF
                    if (slot >= tu.size) continue
                    val meshU = data.getFloat(f * 3 * stride + 24)
                    val meshV = data.getFloat(f * 3 * stride + 28)
                    if (kotlin.math.abs(meshU - tu[slot]) > 0.01f ||
                        kotlin.math.abs(meshV - tv[slot]) > 0.01f
                    ) uvBad++
                }
                if (uvBad > 0) {
                    liveUvMismatch++
                    if (liveUvReported++ < 8) {
                        liveBudget--
                        val slot0 = fa[0].toInt() and 0xFFFF
                        System.err.println(
                            "[LiveDiff] UV mismatch on $uvBad/$n faces  " +
                                "mesh=(%.3f,%.3f) legacy=(%.3f,%.3f) mask=${model.functionMask}"
                                    .format(
                                        data.getFloat(24), data.getFloat(28),
                                        if (slot0 < tu.size) tu[slot0] else -1f,
                                        if (slot0 < tv.size) tv[slot0] else -1f
                                    )
                        )
                    }
                }
            }
        }

        val legacy = legacyFaceKeys(model, model.renderedFaceCount) ?: return
        val mesh = meshFaceKeys(built, model.modernYaw and 0x3fff)
        val missing = subtractMultiset(legacy, mesh)
        val extra = subtractMultiset(mesh, legacy)
        liveChecked++

        val legacyMats = HashMap<Int, Int>()
        model.facesMaterial?.let { mats ->
            for (f in 0 until model.renderedFaceCount) {
                val m = mats[f].toInt()
                legacyMats[m] = (legacyMats[m] ?: 0) + 1
            }
        }
        val meshMats = HashMap<Int, Int>()
        for (b in built.batches) {
            meshMats[b.materialId] = (meshMats[b.materialId] ?: 0) + b.indices.size / 3
        }
        val matDiff = legacyMats.keys.union(meshMats.keys)
            .count { (legacyMats[it] ?: 0) != (meshMats[it] ?: 0) }
        if (matDiff > 0) {
            liveMaterialMismatch++
            if (liveMatReported++ < 10) {
                liveBudget--
                System.err.println(
                    "[LiveDiff] MATERIAL mismatch: legacy=${legacyMats.toSortedMap()} " +
                        "mesh=${meshMats.toSortedMap()} faces=${model.renderedFaceCount}"
                )
            }
        }

        if (missing == 0 && extra == 0) return
        liveStale++
        if (liveReported++ < 15) {
            liveBudget--
            System.err.println(
                "[LiveDiff] STALE modernBuilt: meshFaces=${built.faceCount} " +
                    "legacyFaces=${model.renderedFaceCount} missingFromMesh=$missing " +
                    "extraInMesh=$extra yaw=${model.modernYaw} mask=${model.functionMask}"
            )
        }
    }

    @JvmStatic
    fun liveSummary(): String = "live[countChecked=$liveCountChecked countBad=$liveCountMismatch deepChecked=$liveChecked stale=$liveStale matBad=$liveMaterialMismatch uvBad=$liveUvMismatch]"

    private var liveBudget = 4000
    private var liveChecked = 0
    private var liveStale = 0
    private var liveReported = 0
    private var liveMaterialMismatch = 0
    private var liveMatReported = 0
    private var liveCountChecked = 0
    private var liveCountMismatch = 0
    private var liveCountReported = 0
    private var liveUvMismatch = 0
    private var liveUvReported = 0
    private val contrastHist = HashMap<Int, Int>()
    private val groundHist = HashMap<String, Int>()
    @JvmStatic fun recordGround(mat: Int, combine: Int, vbo: Int, hasData: Boolean) {
        val k = "mat${if (mat < 0) "none" else "tex"}/comb$combine/vbo${if (vbo != 0) "Y" else "N"}/data${if (hasData) "Y" else "N"}"
        groundHist[k] = (groundHist[k] ?: 0) + 1
    }
    @JvmStatic fun groundSummary(): String =
        "ground" + groundHist.entries.sortedByDescending { it.value }.take(5)
            .joinToString(",", "[", "]") { "${it.key}x${it.value}" }

    @JvmStatic fun recordContrast(c: Int) { contrastHist[c] = (contrastHist[c] ?: 0) + 1 }

    @JvmStatic fun contrastSummary(): String =
        "contrast" + contrastHist.entries.sortedByDescending { it.value }.take(6)
            .joinToString(",", "[", "]") { "${it.key}x${it.value}" }

    private class Stats {
        var models = 0
        var meshBuilt = 0
        var legacyCapture = 0
        var mismatches = 0
        var posMismatches = 0
        var posSkipped = 0
        var colourMismatches = 0
        var colourSkipped = 0
        var buildErrors = 0
        var reported = 0
        val mismatchLocs = HashSet<Int>()
        val posMismatchLocs = HashSet<Int>()
        val colourMismatchLocs = HashSet<Int>()
        var colourDumps = 0
        val nullReasons = HashSet<String>()
        val mismatchNames = HashSet<String>()
        var pairModels = 0
        var pairErrors = 0
    }

    private val stat = Stats()

    private fun diffLoc(toolkit: AbstractToolkit, type: ObjectType, locId: Int) {
        val shapes = type.shapes
        if (shapes == null || shapes.isEmpty()) return

        for (shape in shapes) {
            for (rotation in 0..3) {
                val model = try {
                    type.getStationaryModel(toolkit, RETAIN_MASK, shape.toInt(), rotation, null)
                } catch (e: Throwable) {
                    stat.buildErrors++
                    continue
                } as? ModelGL ?: continue

                report(locId, shape.toInt(), rotation, model, type.name)

                val pairModel = try {
                    type.getPair(
                        toolkit, WALL_MASK, shape.toInt(), rotation,
                        null, null, 0, 0, 0, false, null
                    )?.mainObject as? ModelGL
                } catch (e: Throwable) {
                    stat.pairErrors++
                    null
                }
                if (pairModel != null) report(locId, shape.toInt(), rotation, pairModel, type.name, viaPair = true)
            }
        }
    }

    private fun report(
        locId: Int, shape: Int, rotation: Int, model: ModelGL, locName: String,
        viaPair: Boolean = false
    ) {
        stat.models++
        if (viaPair) stat.pairModels++
        val built = model.modernBuilt
        val legacyDrawn = model.renderedFaceCount
        val tag = "loc=$locId shape=$shape rot=$rotation"

        if (built == null) {
            stat.legacyCapture++
            return
        }
        stat.meshBuilt++

        val delta = built.faceCount - legacyDrawn

        var posVerdict = ""
        run {
            val legacyFaces = legacyFaceKeys(model, legacyDrawn)
            val meshFaces = meshFaceKeys(built, model.modernYaw and 0x3fff)
            if (legacyFaces == null) {
                stat.posSkipped++
            } else {
                val onlyLegacy = subtractMultiset(legacyFaces, meshFaces)
                val onlyMesh = subtractMultiset(meshFaces, legacyFaces)
                if (onlyLegacy > 0 || onlyMesh > 0) {
                    stat.posMismatches++
                    stat.posMismatchLocs.add(locId)
                    posVerdict = " POS missingFromMesh=$onlyLegacy extraInMesh=$onlyMesh"
                }

                val legacyCols = legacyColourKeys(model, legacyDrawn)
                val meshCols = meshColourKeys(built, model.modernYaw and 0x3fff)
                if (legacyCols == null) {
                    stat.colourSkipped++
                } else {
                    val colOnlyLegacy = subtractMultiset(legacyCols, meshCols)
                    val colOnlyMesh = subtractMultiset(meshCols, legacyCols)
                    if (colOnlyLegacy > 0 || colOnlyMesh > 0) {
                        stat.colourMismatches++
                        stat.colourMismatchLocs.add(locId)
                        posVerdict += " COLOUR differing=$colOnlyLegacy"
                        dumpColourDetail(locId, shape, rotation, model, built, legacyDrawn)
                    }
                }
            }
        }

        if (delta == 0 && posVerdict.isEmpty()) return

        if (delta != 0) {
            stat.mismatches++
            stat.mismatchLocs.add(locId)
        }
        stat.mismatchNames.add("$locId:$locName")
        if (stat.reported++ >= MAX_REPORTS) return

        System.err.println(
            "[GeoDiff] $tag '$locName' MISMATCH " +
                "meshFaces=${built.faceCount} legacyFaces=$legacyDrawn delta=$delta$posVerdict " +
                "srcFaces=${built.sourceFaceCount} skips[billboard=${built.skipBillboardHidden} " +
                "renderType=${built.skipRenderType} badVertex=${built.skipInvalidVertex} " +
                "transparent=${built.skipTransparent} matFilter=${built.skipMaterialFiltered}] bb[seen=${built.billboardsSeen} uid=${built.billboardsWithUid} oor=${built.billboardsOutOfRange}] yaw=${model.modernYaw}"
        )
    }

    private fun legacyFaceKeys(model: ModelGL, faceCount: Int, includeColour: Boolean = false): LongArray? {
        val missing = when {
            model.facesA == null -> "facesA"
            model.facesB == null -> "facesB"
            model.facesC == null -> "facesC"
            model.verticesX == null -> "verticesX"
            model.verticesY == null -> "verticesY"
            model.verticesZ == null -> "verticesZ"
            else -> null
        }
        if (missing != null) {
            if (stat.nullReasons.add(missing)) {
                System.err.println(
                    "[GeoDiff] legacy arrays unavailable: $missing is null " +
                        "(numFaces=${model.numFaces} renderedFaceCount=${model.renderedFaceCount} " +
                        "maxDepth=${model.maxDepth} numNormals=${model.numNormals})"
                )
            }
            return null
        }
        val facesA = model.facesA!!
        val facesB = model.facesB!!
        val facesC = model.facesC!!
        val vx = model.verticesX!!
        val vy = model.verticesY!!
        val vz = model.verticesZ!!
        val triangles = model.triangles
        val normalMapping = model.vertexNormalIndices
        val maxDepth = model.maxDepth
        val numNormals = model.numNormals

        val direct = triangles == null || normalMapping == null
        val normalToVertex = if (direct) null else IntArray(numNormals) { -1 }.also { map ->
            for (vertIdx in 0 until maxDepth) {
                for (i in triangles!![vertIdx] until triangles[vertIdx + 1]) {
                    val n = (normalMapping!![i].toInt() and 0xFFFF) - 1
                    if (n in 0 until numNormals) map[n] = vertIdx
                }
            }
        }

        fun vertexOf(slot: Int): Int {
            if (direct) return slot
            return if (slot < numNormals) normalToVertex!![slot] else -1
        }

        if (includeColour) {
            val missingColour = when {
                model.facesColor == null -> "facesColor"
                model.facesMaterial == null -> "facesMaterial"
                model.facesAlpha == null -> "facesAlpha"
                else -> null
            }
            if (missingColour != null) {
                if (stat.nullReasons.add(missingColour)) {
                    System.err.println("[GeoDiff] colour arrays unavailable: $missingColour is null")
                }
                return null
            }
        }
        val facesColor = model.facesColor
        val facesMaterial = model.facesMaterial
        val facesAlpha = model.facesAlpha
        val ambient = model.ambient

        val keys = ArrayList<Long>(faceCount)
        for (f in 0 until faceCount) {
            val a = vertexOf(facesA[f].toInt() and 0xFFFF)
            val b = vertexOf(facesB[f].toInt() and 0xFFFF)
            val c = vertexOf(facesC[f].toInt() and 0xFFFF)
            if (a < 0 || b < 0 || c < 0 || a >= maxDepth || b >= maxDepth || c >= maxDepth) continue
            var key = faceKey(vx[a], vy[a], vz[a], vx[b], vy[b], vz[b], vx[c], vy[c], vz[c])
            if (includeColour) {
                val hsl = facesColor?.get(f)?.toInt()?.and(0xFFFF) ?: 0
                val material = facesMaterial?.get(f) ?: (-1).toShort()
                val alpha = facesAlpha?.get(f) ?: 0.toByte()
                val baseRgba = model.hslToRgb(hsl.toShort(), material, ambient, alpha)
                val packed = (baseRgba and 0xFFFFFF00.toInt()) or (255 - (alpha.toInt() and 0xFF))
                key = key * 31 + packed
            }
            keys.add(key)
        }
        return keys.toLongArray().also { it.sort() }
    }

    private fun meshFaceKeys(
        built: com.jagex.game.runetek5.graphics.modern.util.ModernModelBuilder.BuiltModel,
        yaw: Int,
        includeColour: Boolean = false
    ): LongArray {
        val stride = com.jagex.game.runetek5.graphics.modern.batch.ModelCache.BYTES_PER_VERTEX
        val data = java.nio.ByteBuffer.wrap(built.vertexData).order(java.nio.ByteOrder.nativeOrder())
        val sin = if (yaw == 0) 0 else MathUtils.SINE[yaw]
        val cos = if (yaw == 0) 16384 else MathUtils.COSINE[yaw]
        val keys = LongArray(built.faceCount)
        val rx = IntArray(3)
        val ry = IntArray(3)
        val rz = IntArray(3)
        for (f in 0 until built.faceCount) {
            val base = f * 3 * stride
            for (v in 0 until 3) {
                val x = data.getFloat(base + v * stride).toInt()
                val y = data.getFloat(base + v * stride + 4).toInt()
                val z = data.getFloat(base + v * stride + 8).toInt()
                if (yaw == 0) {
                    rx[v] = x; ry[v] = y; rz[v] = z
                } else {
                    rx[v] = (sin * z + cos * x) shr 14
                    ry[v] = y
                    rz[v] = (cos * z - sin * x) shr 14
                }
            }
            var key = faceKey(rx[0], ry[0], rz[0], rx[1], ry[1], rz[1], rx[2], ry[2], rz[2])
            if (includeColour) {
                val o = base + 32
                val packed = ((built.vertexData[o].toInt() and 0xFF) shl 24) or
                    ((built.vertexData[o + 1].toInt() and 0xFF) shl 16) or
                    ((built.vertexData[o + 2].toInt() and 0xFF) shl 8) or
                    (built.vertexData[o + 3].toInt() and 0xFF)
                key = key * 31 + packed
            }
            keys[f] = key
        }
        keys.sort()
        return keys
    }

    private fun faceKey(
        ax: Int, ay: Int, az: Int,
        bx: Int, by: Int, bz: Int,
        cx: Int, cy: Int, cz: Int
    ): Long {
        fun v(x: Int, y: Int, z: Int): Long {
            var h = 1125899906842597L
            h = h * 31 + x
            h = h * 31 + y
            h = h * 31 + z
            return h
        }
        return v(ax, ay, az) + v(bx, by, bz) + v(cx, cy, cz)
    }

    private fun legacyColourKeys(model: ModelGL, faceCount: Int): LongArray? {
        val positions = legacyFaceKeys(model, faceCount, includeColour = true) ?: return null
        return positions
    }

    private fun meshColourKeys(
        built: com.jagex.game.runetek5.graphics.modern.util.ModernModelBuilder.BuiltModel,
        yaw: Int
    ): LongArray = meshFaceKeys(built, yaw, includeColour = true)

    private fun dumpColourDetail(
        locId: Int, shape: Int, rotation: Int,
        model: ModelGL,
        built: com.jagex.game.runetek5.graphics.modern.util.ModernModelBuilder.BuiltModel,
        legacyDrawn: Int
    ) {
        if (stat.colourDumps++ >= 3) return
        val facesA = model.facesA ?: return
        val facesB = model.facesB ?: return
        val facesC = model.facesC ?: return
        val vx = model.verticesX ?: return
        val vy = model.verticesY ?: return
        val vz = model.verticesZ ?: return
        val triangles = model.triangles
        val normalMapping = model.vertexNormalIndices
        val maxDepth = model.maxDepth
        val numNormals = model.numNormals
        val direct = triangles == null || normalMapping == null
        val normalToVertex = if (direct) null else IntArray(numNormals) { -1 }.also { map ->
            for (v in 0 until maxDepth) {
                for (i in triangles!![v] until triangles[v + 1]) {
                    val n = (normalMapping!![i].toInt() and 0xFFFF) - 1
                    if (n in 0 until numNormals) map[n] = v
                }
            }
        }
        fun vertexOf(slot: Int) = if (direct) slot else if (slot < numNormals) normalToVertex!![slot] else -1

        val byPos = HashMap<Long, IntArray>()
        for (f in 0 until legacyDrawn) {
            val a = vertexOf(facesA[f].toInt() and 0xFFFF)
            val b = vertexOf(facesB[f].toInt() and 0xFFFF)
            val c = vertexOf(facesC[f].toInt() and 0xFFFF)
            if (a < 0 || b < 0 || c < 0 || a >= maxDepth || b >= maxDepth || c >= maxDepth) continue
            val hsl = model.facesColor?.get(f)?.toInt()?.and(0xFFFF) ?: 0
            val mat = model.facesMaterial?.get(f) ?: (-1).toShort()
            val alpha = model.facesAlpha?.get(f) ?: 0.toByte()
            val packed = model.hslToRgb(hsl.toShort(), mat, model.ambient, alpha)
            byPos[faceKey(vx[a], vy[a], vz[a], vx[b], vy[b], vz[b], vx[c], vy[c], vz[c])] =
                intArrayOf(packed, hsl, mat.toInt(), alpha.toInt() and 0xFF)
        }

        val yaw = model.modernYaw and 0x3fff
        val sin = if (yaw == 0) 0 else MathUtils.SINE[yaw]
        val cos = if (yaw == 0) 16384 else MathUtils.COSINE[yaw]
        val stride = com.jagex.game.runetek5.graphics.modern.batch.ModelCache.BYTES_PER_VERTEX
        val data = java.nio.ByteBuffer.wrap(built.vertexData).order(java.nio.ByteOrder.nativeOrder())
        var shown = 0
        for (f in 0 until built.faceCount) {
            if (shown >= 4) break
            val base = f * 3 * stride
            val rx = IntArray(3); val ry = IntArray(3); val rz = IntArray(3)
            for (v in 0 until 3) {
                val x = data.getFloat(base + v * stride).toInt()
                val y = data.getFloat(base + v * stride + 4).toInt()
                val z = data.getFloat(base + v * stride + 8).toInt()
                if (yaw == 0) { rx[v] = x; ry[v] = y; rz[v] = z }
                else { rx[v] = (sin * z + cos * x) shr 14; ry[v] = y; rz[v] = (cos * z - sin * x) shr 14 }
            }
            val key = faceKey(rx[0], ry[0], rz[0], rx[1], ry[1], rz[1], rx[2], ry[2], rz[2])
            val legacy = byPos[key] ?: continue
            val o = base + 32
            val meshPacked = ((built.vertexData[o].toInt() and 0xFF) shl 24) or
                ((built.vertexData[o + 1].toInt() and 0xFF) shl 16) or
                ((built.vertexData[o + 2].toInt() and 0xFF) shl 8) or
                (built.vertexData[o + 3].toInt() and 0xFF)
            if (meshPacked == legacy[0]) continue
            val table = ColorUtil.hslToRgb
            val legacyIdx = ModelGL.adjustLuminance(legacy[1], model.ambient.toInt())
            val tableAtLegacyIdx = table?.getOrNull(legacyIdx) ?: -1
            val meshRgb = meshPacked ushr 8
            var meshIdxInCurrentTable = -1
            if (table != null) {
                for (i in table.indices) {
                    if (table[i] == meshRgb) { meshIdxInCurrentTable = i; break }
                }
            }
            System.err.println(
                ("[GeoDiff-COLOUR] loc=%d shape=%d rot=%d face=%d " +
                    "legacy=0x%08X mesh=0x%08X | hsl=%d material=%d rsAlpha=%d ambient=%d " +
                    "legacyIdx=%d table[legacyIdx]=0x%06X meshRgb=0x%06X meshIdxInTable=%d")
                    .format(
                        locId, shape, rotation, f,
                        legacy[0], meshPacked, legacy[1], legacy[2], legacy[3], model.ambient.toInt(),
                        legacyIdx, tableAtLegacyIdx, meshRgb, meshIdxInCurrentTable
                    )
            )
            shown++
        }
    }

    private fun subtractMultiset(a: LongArray, b: LongArray): Int {
        var i = 0
        var j = 0
        var unmatched = 0
        while (i < a.size) {
            when {
                j >= b.size -> { unmatched++; i++ }
                a[i] == b[j] -> { i++; j++ }
                a[i] < b[j] -> { unmatched++; i++ }
                else -> j++
            }
        }
        return unmatched
    }
}
