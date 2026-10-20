package com.jagex.game.runetek5.graphics.modern.util

import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.utils.ColorUtil
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import kotlin.math.sqrt

object MeshConverter {

    data class ConvertedMesh(
        val vertexData: ByteBuffer,
        val indexData: ShortBuffer,
        val vertexCount: Int,
        val indexCount: Int
    ) {
        fun free() {
            MemoryUtil.memFree(vertexData)
            MemoryUtil.memFree(indexData)
        }
    }

    fun convert(mesh: Mesh, defaultColor: Int = 0xFFFFFFFF.toInt()): ConvertedMesh? {
        val numFaces = mesh.numFaces
        if (numFaces <= 0) return null

        val facesA = mesh.facesA ?: return null
        val facesB = mesh.facesB ?: return null
        val facesC = mesh.facesC ?: return null
        val verticesX = mesh.verticesX ?: return null
        val verticesY = mesh.verticesY ?: return null
        val verticesZ = mesh.verticesZ ?: return null

        val vertexCount = numFaces * 3
        val indexCount = numFaces * 3

        val vertexData = MemoryUtil.memAlloc(vertexCount * 36)
        val indexData = MemoryUtil.memAllocShort(indexCount)

        try {
            var vertexIndex = 0

            for (faceIdx in 0 until numFaces) {
                val a = facesA[faceIdx].toInt() and 0xFFFF
                val b = facesB[faceIdx].toInt() and 0xFFFF
                val c = facesC[faceIdx].toInt() and 0xFFFF

                val ax = verticesX[a].toFloat()
                val ay = verticesY[a].toFloat()
                val az = verticesZ[a].toFloat()

                val bx = verticesX[b].toFloat()
                val by = verticesY[b].toFloat()
                val bz = verticesZ[b].toFloat()

                val cx = verticesX[c].toFloat()
                val cy = verticesY[c].toFloat()
                val cz = verticesZ[c].toFloat()

                val e1x = bx - ax
                val e1y = by - ay
                val e1z = bz - az
                val e2x = cx - ax
                val e2y = cy - ay
                val e2z = cz - az

                var nx = e1y * e2z - e1z * e2y
                var ny = e1z * e2x - e1x * e2z
                var nz = e1x * e2y - e1y * e2x

                val len = sqrt(nx * nx + ny * ny + nz * nz)
                if (len > 0.0001f) {
                    nx /= len
                    ny /= len
                    nz /= len
                } else {
                    nx = 0f
                    ny = 1f
                    nz = 0f
                }

                val rgba = if (mesh.facesColor != null && faceIdx < mesh.facesColor!!.size) {
                    hslToRgba(mesh.facesColor!![faceIdx].toInt() and 0xFFFF)
                } else {
                    defaultColor
                }

                val r = (rgba shr 16) and 0xFF
                val g = (rgba shr 8) and 0xFF
                val blue = rgba and 0xFF
                val alpha = (rgba shr 24) and 0xFF

                writeVertex(vertexData, ax, ay, az, nx, ny, nz, 0f, 0f, r, g, blue, alpha)

                writeVertex(vertexData, bx, by, bz, nx, ny, nz, 0f, 0f, r, g, blue, alpha)

                writeVertex(vertexData, cx, cy, cz, nx, ny, nz, 0f, 0f, r, g, blue, alpha)

                indexData.put(vertexIndex.toShort())
                indexData.put((vertexIndex + 1).toShort())
                indexData.put((vertexIndex + 2).toShort())

                vertexIndex += 3
            }

            vertexData.flip()
            indexData.flip()

            return ConvertedMesh(vertexData, indexData, vertexCount, indexCount)

        } catch (e: Exception) {
            MemoryUtil.memFree(vertexData)
            MemoryUtil.memFree(indexData)
            throw e
        }
    }

    fun convertShared(mesh: Mesh, defaultColor: Int = 0xFFFFFFFF.toInt()): ConvertedMesh? {
        val numVertices = mesh.numVertices
        val numFaces = mesh.numFaces
        if (numVertices <= 0 || numFaces <= 0) return null

        val facesA = mesh.facesA ?: return null
        val facesB = mesh.facesB ?: return null
        val facesC = mesh.facesC ?: return null
        val verticesX = mesh.verticesX ?: return null
        val verticesY = mesh.verticesY ?: return null
        val verticesZ = mesh.verticesZ ?: return null

        val normalsX = FloatArray(numVertices)
        val normalsY = FloatArray(numVertices)
        val normalsZ = FloatArray(numVertices)

        for (faceIdx in 0 until numFaces) {
            val a = facesA[faceIdx].toInt() and 0xFFFF
            val b = facesB[faceIdx].toInt() and 0xFFFF
            val c = facesC[faceIdx].toInt() and 0xFFFF

            val ax = verticesX[a].toFloat()
            val ay = verticesY[a].toFloat()
            val az = verticesZ[a].toFloat()

            val bx = verticesX[b].toFloat()
            val by = verticesY[b].toFloat()
            val bz = verticesZ[b].toFloat()

            val cx = verticesX[c].toFloat()
            val cy = verticesY[c].toFloat()
            val cz = verticesZ[c].toFloat()

            val e1x = bx - ax
            val e1y = by - ay
            val e1z = bz - az
            val e2x = cx - ax
            val e2y = cy - ay
            val e2z = cz - az

            val nx = e1y * e2z - e1z * e2y
            val ny = e1z * e2x - e1x * e2z
            val nz = e1x * e2y - e1y * e2x

            normalsX[a] += nx
            normalsY[a] += ny
            normalsZ[a] += nz
            normalsX[b] += nx
            normalsY[b] += ny
            normalsZ[b] += nz
            normalsX[c] += nx
            normalsY[c] += ny
            normalsZ[c] += nz
        }

        for (i in 0 until numVertices) {
            val len = sqrt(normalsX[i] * normalsX[i] + normalsY[i] * normalsY[i] + normalsZ[i] * normalsZ[i])
            if (len > 0.0001f) {
                normalsX[i] /= len
                normalsY[i] /= len
                normalsZ[i] /= len
            } else {
                normalsY[i] = 1f
            }
        }

        val colorR = IntArray(numVertices)
        val colorG = IntArray(numVertices)
        val colorB = IntArray(numVertices)
        val colorCount = IntArray(numVertices)

        for (faceIdx in 0 until numFaces) {
            val a = facesA[faceIdx].toInt() and 0xFFFF
            val b = facesB[faceIdx].toInt() and 0xFFFF
            val c = facesC[faceIdx].toInt() and 0xFFFF

            val rgba = if (mesh.facesColor != null && faceIdx < mesh.facesColor!!.size) {
                hslToRgba(mesh.facesColor!![faceIdx].toInt() and 0xFFFF)
            } else {
                defaultColor
            }

            val r = (rgba shr 16) and 0xFF
            val g = (rgba shr 8) and 0xFF
            val blue = rgba and 0xFF

            colorR[a] += r; colorG[a] += g; colorB[a] += blue; colorCount[a]++
            colorR[b] += r; colorG[b] += g; colorB[b] += blue; colorCount[b]++
            colorR[c] += r; colorG[c] += g; colorB[c] += blue; colorCount[c]++
        }

        val vertexData = MemoryUtil.memAlloc(numVertices * 36)
        val indexData = MemoryUtil.memAllocShort(numFaces * 3)

        try {
            for (i in 0 until numVertices) {
                val px = verticesX[i].toFloat()
                val py = verticesY[i].toFloat()
                val pz = verticesZ[i].toFloat()

                val cnt = colorCount[i].coerceAtLeast(1)
                val r = (colorR[i] / cnt).coerceIn(0, 255)
                val g = (colorG[i] / cnt).coerceIn(0, 255)
                val b = (colorB[i] / cnt).coerceIn(0, 255)

                writeVertex(vertexData, px, py, pz, normalsX[i], normalsY[i], normalsZ[i], 0f, 0f, r, g, b, 255)
            }

            for (faceIdx in 0 until numFaces) {
                indexData.put(facesA[faceIdx])
                indexData.put(facesB[faceIdx])
                indexData.put(facesC[faceIdx])
            }

            vertexData.flip()
            indexData.flip()

            return ConvertedMesh(vertexData, indexData, numVertices, numFaces * 3)

        } catch (e: Exception) {
            MemoryUtil.memFree(vertexData)
            MemoryUtil.memFree(indexData)
            throw e
        }
    }

    private fun writeVertex(
        buffer: ByteBuffer,
        px: Float, py: Float, pz: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float,
        r: Int, g: Int, b: Int, a: Int
    ) {
        buffer.putFloat(px)
        buffer.putFloat(py)
        buffer.putFloat(pz)
        buffer.putFloat(nx)
        buffer.putFloat(ny)
        buffer.putFloat(nz)
        buffer.putFloat(u)
        buffer.putFloat(v)
        buffer.put(r.toByte())
        buffer.put(g.toByte())
        buffer.put(b.toByte())
        buffer.put(a.toByte())
    }

    private fun hslToRgba(packedHsl: Int): Int {
        if (packedHsl == 0) return 0xFF808080.toInt()

        val hue6 = (packedHsl shr 10) and 0x3F
        val sat3 = (packedHsl shr 7) and 0x7
        val lum7 = packedHsl and 0x7F

        val h = hue6 / 63f
        val s = sat3 / 7f
        val l = lum7 / 127f

        return hslToRgb(h, s, l)
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): Int {
        val r: Float
        val g: Float
        val b: Float

        if (s == 0f) {
            r = l
            g = l
            b = l
        } else {
            val q = if (l < 0.5f) l * (1 + s) else l + s - l * s
            val p = 2 * l - q
            r = hueToRgb(p, q, h + 1f / 3f)
            g = hueToRgb(p, q, h)
            b = hueToRgb(p, q, h - 1f / 3f)
        }

        val ri = (r * 255).toInt().coerceIn(0, 255)
        val gi = (g * 255).toInt().coerceIn(0, 255)
        val bi = (b * 255).toInt().coerceIn(0, 255)

        return (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
    }

    private fun hueToRgb(p: Float, q: Float, t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }
}
