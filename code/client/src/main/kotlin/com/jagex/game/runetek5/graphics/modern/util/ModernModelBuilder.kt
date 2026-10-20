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

object ModernModelBuilder {

    class BuiltModel(
        val id: Long,
        val faceCount: Int,
        val vertexCount: Int,
        val vertexData: ByteArray,
        val batches: List<TextureBatch>,
        val skipBillboardHidden: Int = 0,
        val skipRenderType: Int = 0,
        val skipInvalidVertex: Int = 0,
        val skipTransparent: Int = 0,
        val sourceFaceCount: Int = 0,
        val billboardsSeen: Int = 0,
        val billboardsWithUid: Int = 0,
        val billboardsOutOfRange: Int = 0,
        val skipMaterialFiltered: Int = 0,
        val acceptedFaces: IntArray = IntArray(0),
        val acceptedMaterialIds: IntArray = IntArray(0)
    ) {
        class TextureBatch(val materialId: Int, val indices: IntArray)

        fun toCachedModel(): ModelCache.CachedModel = toCachedModel(vertexData)

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

    const val COLOR_OFFSET = 32

    @JvmStatic
    fun recolour(
        built: BuiltModel,
        facesColor: ShortArray?,
        facesAlpha: ByteArray?,
        @Suppress("UNUSED_PARAMETER") facesMaterial: ShortArray?,
        ambient: Int,
        colorTable: IntArray,
        materialProvider: MaterialProvider?,
        meshFaceIndices: IntArray?
    ): ByteArray {
        val out = built.vertexData.copyOf()
        if (facesColor == null || meshFaceIndices == null) return out
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

    @JvmStatic
    fun build(
        mesh: Mesh,
        ambient: Int,
        materialProvider: MaterialProvider? = null,
        stripDetailLayers: Boolean = false,
        contrast: Int = 768
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

        val smoothX = IntArray(numVertices)
        val smoothY = IntArray(numVertices)
        val smoothZ = IntArray(numVertices)
        val smoothCount = IntArray(numVertices)
        val faceNormal = FloatArray(3)
        val contrastValue = if (contrast == 0) 768 else contrast
        val flatNormalScale = 2f / contrastValue
        val smoothNormalScale = 3f / contrastValue

        fun computeFaceNormal(face: Int) {
            val a = vertexA(face)
            val b = vertexB(face)
            val c = vertexC(face)
            val dx1 = verticesX[b] - verticesX[a]
            val dy1 = verticesY[b] - verticesY[a]
            val dz1 = verticesZ[b] - verticesZ[a]
            val dx2 = verticesX[c] - verticesX[a]
            val dy2 = verticesY[c] - verticesY[a]
            val dz2 = verticesZ[c] - verticesZ[a]
            var x = dy1 * dz2 - dy2 * dz1
            var y = dz1 * dx2 - dz2 * dx1
            var z = dx1 * dy2 - dx2 * dy1
            while (x > 8192 || y > 8192 || z > 8192 || x < -8192 || y < -8192 || z < -8192) {
                x = x shr 1
                y = y shr 1
                z = z shr 1
            }
            var normalMagnitude = sqrt((x * x + y * y + z * z).toDouble()).toInt()
            if (normalMagnitude <= 0) normalMagnitude = 1
            val nx = (x * 256 / normalMagnitude).toFloat()
            val ny = (y * 256 / normalMagnitude).toFloat()
            val nz = (z * 256 / normalMagnitude).toFloat()
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
            val fx = faceNormal[0].toInt(); val fy = faceNormal[1].toInt(); val fz = faceNormal[2].toInt()
            smoothX[a] += fx; smoothY[a] += fy; smoothZ[a] += fz; smoothCount[a]++
            smoothX[b] += fx; smoothY[b] += fy; smoothZ[b] += fz; smoothCount[b]++
            smoothX[c] += fx; smoothY[c] += fy; smoothZ[c] += fz; smoothCount[c]++
        }

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
            if (renderType == 2) { skipRenderType++; continue }

            val a = vertexA(face)
            val b = vertexB(face)
            val c = vertexC(face)
            if (!isValidVertex(a) || !isValidVertex(b) || !isValidVertex(c)) { skipInvalidVertex++; continue }

            var materialId = facesMaterial?.get(face)?.toInt() ?: -1
            if (materialId != -1 && materialProvider != null) {
                val material = materialProvider.getMaterial(materialId and 0xffff)
                if (stripDetailLayers && material.detailsOnly) {
                    materialId = -1
                } else if (material.skipTriangles) {
                    skipMaterialFiltered++
                    continue
                }
            }

            acceptedFaces[acceptedCount] = face
            acceptedMaterials[acceptedCount] = materialId
            acceptedCount++
        }
        if (acceptedCount == 0) return null

        run {
            val order = IntArray(acceptedCount) { it }
            val keys = IntArray(acceptedCount)
            for (i in 0 until acceptedCount) {
                val face = acceptedFaces[i]
                val materialId = acceptedMaterials[i]
                var alphaFace = (facesAlpha?.get(face)?.toInt() ?: 0) != 0
                if (!alphaFace && materialId >= 0 && materialProvider != null &&
                    materialProvider.getMaterial(materialId and 0xffff).effectCombiner != 0
                ) alphaFace = true
                keys[i] = (if (alphaFace) 1 shl 24 else 0) or ((materialId + 1) and 0xffffff)
            }
            val sorted = order.sortedBy { keys[it] }
            val faces = IntArray(acceptedCount)
            val materials = IntArray(acceptedCount)
            for (i in 0 until acceptedCount) {
                faces[i] = acceptedFaces[sorted[i]]
                materials[i] = acceptedMaterials[sorted[i]]
            }
            System.arraycopy(faces, 0, acceptedFaces, 0, acceptedCount)
            System.arraycopy(materials, 0, acceptedMaterials, 0, acceptedCount)
        }

        val transforms = if (mesh.facesTexture != null && mesh.numTextures > 0) {
            computeTextureTransforms(mesh)
        } else {
            null
        }

        val vertexCount = acceptedCount * 3
        val vertexArray = ByteArray(vertexCount * ModelCache.BYTES_PER_VERTEX)
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
            vertexData.putFloat(0f)
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
                val ka = if (smoothCount[a] == 0) 0f else smoothNormalScale / smoothCount[a]
                val kb = if (smoothCount[b] == 0) 0f else smoothNormalScale / smoothCount[b]
                val kc = if (smoothCount[c] == 0) 0f else smoothNormalScale / smoothCount[c]
                writeVertex(a, smoothX[a] * ka, smoothY[a] * ka, smoothZ[a] * ka, uv[0], uv[1], rgba)
                writeVertex(b, smoothX[b] * kb, smoothY[b] * kb, smoothZ[b] * kb, uv[2], uv[3], rgba)
                writeVertex(c, smoothX[c] * kc, smoothY[c] * kc, smoothZ[c] * kc, uv[4], uv[5], rgba)
            } else {
                computeFaceNormal(face)
                writeVertex(a, faceNormal[0] * flatNormalScale, faceNormal[1] * flatNormalScale, faceNormal[2] * flatNormalScale, uv[0], uv[1], rgba)
                writeVertex(b, faceNormal[0] * flatNormalScale, faceNormal[1] * flatNormalScale, faceNormal[2] * flatNormalScale, uv[2], uv[3], rgba)
                writeVertex(c, faceNormal[0] * flatNormalScale, faceNormal[1] * flatNormalScale, faceNormal[2] * flatNormalScale, uv[4], uv[5], rgba)
            }
        }
        val batches = ArrayList<BuiltModel.TextureBatch>()
        run {
            var runStart = 0
            while (runStart < acceptedCount) {
                val materialId = if (acceptedMaterials[runStart] < 0) -1 else acceptedMaterials[runStart]
                var runEnd = runStart + 1
                while (runEnd < acceptedCount &&
                    (if (acceptedMaterials[runEnd] < 0) -1 else acceptedMaterials[runEnd]) == materialId
                ) runEnd++
                val indices = IntArray((runEnd - runStart) * 3)
                var c = 0
                for (i in runStart until runEnd) {
                    val base = i * 3
                    indices[c] = base
                    indices[c + 1] = base + 1
                    indices[c + 2] = base + 2
                    c += 3
                }
                batches.add(BuiltModel.TextureBatch(materialId, indices))
                runStart = runEnd
            }
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

    private fun contentHash(vertexData: ByteArray, batches: List<BuiltModel.TextureBatch>): Long {
        var hash = -0x340d631b7bdddcdbL
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

    private fun faceColorRgba(
        colorTable: IntArray,
        colorHsl: Int,
        materialId: Int,
        ambient: Int,
        alpha: Int,
        materialProvider: MaterialProvider?
    ): Int {
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

    private class TextureTransforms(
        val centerX: IntArray,
        val centerY: IntArray,
        val centerZ: IntArray,
        val matrix: Array<FloatArray>
    )

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

    private fun scaleParam(array: IntArray?, index: Int, fallback: Int): Int {
        if (array == null || index >= array.size) return fallback
        return array[index]
    }

    private fun nonZero(value: Int): Int = if (value == 0) 1 else value

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
            rotationMap.copyInto(result, 0, 0, 9)
            return scaleTextureMatrix(result, scaleX, scaleY, scaleZ)
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
        return scaleTextureMatrix(result, scaleX, scaleY, scaleZ)
    }

    private fun scaleTextureMatrix(result: FloatArray, scaleX: Float, scaleY: Float, scaleZ: Float): FloatArray {
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

    private fun computeUVs(mesh: Mesh, transforms: TextureTransforms?, face: Int, materialId: Int, uv: FloatArray) {
        uv.fill(0f)
        if (materialId == -1) return

        val texturePointer = mesh.facesTexture?.get(face)?.toInt() ?: -1
        if (texturePointer == -1) {
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

    private fun applyDirection(direction: Int, u: Float, v: Float, uv: FloatArray) {
        when (direction) {
            1 -> { uv[0] = -v; uv[1] = u }
            2 -> { uv[0] = -u; uv[1] = -v }
            3 -> { uv[0] = v; uv[1] = -u }
            else -> { uv[0] = u; uv[1] = v }
        }
    }
}
