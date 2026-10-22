package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.BitSet
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

class FrameCoverage(val width: Int, val height: Int) {
    private val count = Math.multiplyExact(width, height)
    private val touched = BitSet(count)
    internal var affineFailure: CoverageFallback? = null
        private set

    private var firstUntouched = 0

    internal fun fullyCovered(): Boolean {
        firstUntouched = touched.nextClearBit(firstUntouched)
        return firstUntouched >= count
    }

    internal fun eligiblePixels(): Int = count - touched.cardinality()

    private fun failed(reason: CoverageFallback): Boolean {
        affineFailure = reason
        return false
    }

    init {
        require(width in 1..8192 && height in 1..8192)
    }

    private fun intersection(a: IntArray, b: IntArray): IntArray {
        require(a.size == 4 && b.size == 4 && a[2] >= 0 && a[3] >= 0 && b[2] >= 0 && b[3] >= 0)
        val x = maxOf(0L, a[0].toLong(), b[0].toLong()).coerceAtMost(width.toLong())
        val y = maxOf(0L, a[1].toLong(), b[1].toLong()).coerceAtMost(height.toLong())
        val right = minOf(width.toLong(), a[0].toLong() + a[2], b[0].toLong() + b[2]).coerceAtLeast(x)
        val bottom = minOf(height.toLong(), a[1].toLong() + a[3], b[1].toLong() + b[3]).coerceAtLeast(y)
        return intArrayOf(x.toInt(), y.toInt(), (right - x).toInt(), (bottom - y).toInt())
    }

    fun cover(rect: IntArray, scissor: IntArray? = null) {
        val r = intersection(rect, scissor ?: intArrayOf(0, 0, width, height))
        for (y in r[1] until r[1] + r[3]) touched.set(y * width + r[0], y * width + r[0] + r[2])
    }

    fun affine2d(pass: Pass, vertices: ByteArray, indices: IntArray, projection: FloatArray, viewport: IntArray, scissor: IntArray?): Boolean {
        affineFailure = null
        if (!supports2d(pass)) return false
        val stride = pass.stride
        if (projection.size != 16 || projection.any { !it.isFinite() } || projection[3] != 0f || projection[7] != 0f || projection[15] != 1f) return failed(CoverageFallback.PROJECTION)
        if (indices.size % 3 != 0 || vertices.size % stride != 0) return failed(CoverageFallback.LAYOUT)
        val bytes = ByteBuffer.wrap(vertices).order(ByteOrder.LITTLE_ENDIAN)
        val positions = DoubleArray(6)
        val spriteClip = if (pass === Passes.sprite) FloatArray(4) else null
        val clipOffset = if (spriteClip != null) pass.attrs.first { it.location == 5 }.offset else 0
        val clip = intersection(viewport, scissor ?: intArrayOf(0, 0, width, height))
        for (i in indices.indices step 3) {
            for (j in 0..2) {
                val index = indices[i + j]
                if (index !in 0 until vertices.size / stride) return failed(CoverageFallback.INDEX)
                if (spriteClip != null) for (k in spriteClip.indices) {
                    val value = bytes.getFloat(index * stride + clipOffset + k * 4)
                    if (!value.isFinite()) return failed(CoverageFallback.CLIP_FINITE)
                    if (j != 0 && value != spriteClip[k]) return failed(CoverageFallback.CLIP_VARYING)
                    spriteClip[k] = value
                }
                val x = bytes.getFloat(index * stride).toDouble()
                val y = bytes.getFloat(index * stride + 4).toDouble()
                val sx = viewport[0] + ((projection[0] * x + projection[4] * y + projection[12]) + 1) * 0.5 * viewport[2]
                val sy = viewport[1] + (1 - (projection[1] * x + projection[5] * y + projection[13])) * 0.5 * viewport[3]
                val errorX = (abs(projection[0] * x) + abs(projection[4] * y) + abs(projection[12].toDouble()) + 1) * 0.000001 * viewport[2]
                val errorY = (abs(projection[1] * x) + abs(projection[5] * y) + abs(projection[13].toDouble()) + 1) * 0.000001 * viewport[3]
                if (!sx.isFinite() || !sy.isFinite() || sx !in -1e8..1e8 || sy !in -1e8..1e8 || errorX > 0.25 || errorY > 0.25) return failed(CoverageFallback.TRANSFORM)
                positions[j * 2] = sx
                positions[j * 2 + 1] = sy
            }
            val left = floor(minOf(positions[0], positions[2], positions[4])).toInt() - 1
            val top = floor(minOf(positions[1], positions[3], positions[5])).toInt() - 1
            val right = ceil(maxOf(positions[0], positions[2], positions[4])).toInt() + 1
            val bottom = ceil(maxOf(positions[1], positions[3], positions[5])).toInt() + 1
            val fragmentClip = if (spriteClip == null) clip else {
                if (spriteClip[0] >= spriteClip[2] || spriteClip[1] >= spriteClip[3]) return failed(CoverageFallback.CLIP_ORDER)
                val x0 = floor(spriteClip[0].toDouble().coerceIn(0.0, width.toDouble())).toInt()
                val y0 = floor((height - spriteClip[3].toDouble()).coerceIn(0.0, height.toDouble())).toInt()
                val x1 = ceil(spriteClip[2].toDouble().coerceIn(0.0, width.toDouble())).toInt()
                val y1 = ceil((height - spriteClip[1].toDouble()).coerceIn(0.0, height.toDouble())).toInt()
                intersection(clip, intArrayOf(x0, y0, x1 - x0, y1 - y0))
            }
            cover(intArrayOf(left, top, right - left, bottom - top), fragmentClip)
        }
        return true
    }

    fun inherited(): List<IntArray> {
        val pixels = count - touched.cardinality()
        require(pixels.toLong() * 10 <= count && pixels.toLong() * 4 <= MAX_BYTES) { "untouched history budget: $pixels of $count pixels" }
        if (pixels == 0) return emptyList()
        val rectangles = ArrayList<IntArray>()
        var previous = HashMap<Long, IntArray>()
        for (y in 0 until height) {
            val current = HashMap<Long, IntArray>()
            val end = (y + 1) * width
            var at = touched.nextClearBit(y * width)
            while (at < end) {
                val next = minOf(end, touched.nextSetBit(at).let { if (it < 0) end else it })
                val x = at - y * width
                val w = next - at
                val key = (x.toLong() shl 32) or w.toLong()
                val rect = previous[key]?.also { it[3]++ } ?: intArrayOf(x, y, w, 1).also {
                    require(rectangles.size < MAX_REGIONS) { "untouched history region budget" }
                    rectangles.add(it)
                }
                current[key] = rect
                at = touched.nextClearBit(next)
            }
            previous = current
        }
        return rectangles
    }

    companion object {
        fun supports2d(pass: Pass?): Boolean = pass === Passes.sprite || pass === Passes.surfaceMask

        const val MAX_BYTES = 32 * 1024 * 1024
        const val MAX_REGIONS = 4096
    }
}
