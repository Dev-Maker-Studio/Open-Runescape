package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.lwjgl.opengl.GL33.GL_DOUBLE
import org.lwjgl.opengl.GL33.GL_FLOAT
import org.lwjgl.opengl.GL33.GL_HALF_FLOAT
import org.lwjgl.opengl.GL33.GL_INT
import org.lwjgl.opengl.GL33.GL_SHORT
import org.lwjgl.opengl.GL33.GL_UNSIGNED_BYTE

class SpriteCoverageTest {
    private val width = 100
    private val height = 80
    private val whole = intArrayOf(0, 0, width, height)
    private val indices = intArrayOf(0, 1, 2, 0, 2, 3)
    private val projection = ColorClear.projection(width, height)

    private fun vertices(geometry: IntArray, clip: FloatArray): ByteArray = ColorClear.vertices(geometry, FloatArray(4)).also { data ->
        val bytes = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        repeat(4) { vertex -> clip.forEachIndexed { component, value -> bytes.putFloat(vertex * Passes.sprite.stride + 24 + component * 4, value) } }
    }

    private fun pixels(rect: IntArray): Set<Pair<Int, Int>> = buildSet {
        for (y in rect[1] until rect[1] + rect[3]) for (x in rect[0] until rect[0] + rect[2]) add(x to y)
    }

    private fun remaining(coverage: FrameCoverage) = coverage.inherited().flatMap { pixels(it) }.toSet()

    private fun outside(coverage: FrameCoverage, probe: IntArray) {
        coverage.cover(intArrayOf(0, 0, width, probe[1]))
        coverage.cover(intArrayOf(0, probe[1] + probe[3], width, height - probe[1] - probe[3]))
        coverage.cover(intArrayOf(0, probe[1], probe[0], probe[3]))
        coverage.cover(intArrayOf(probe[0] + probe[2], probe[1], width - probe[0] - probe[2], probe[3]))
    }

    @Test
    fun fullCanvasZeroAlphaSpriteStillExcludesEveryPixelInsideItsProvenClip() {
        val coverage = FrameCoverage(width, height)
        val data = vertices(intArrayOf(-5, -5, width + 10, height + 10), floatArrayOf(1f, 2f, 99f, 79f))
        assertTrue(coverage.affine2d(Passes.sprite, data, indices, projection, whole, null))
        val expected = pixels(whole).filter { (x, y) -> x < 1 || x >= 99 || y < 1 || y >= 78 }.toSet()
        assertEquals(expected, remaining(coverage))
        assertFalse(remaining(coverage).contains(50 to 40))
    }

    @Test
    fun clipCoordinatesAreNativeBottomLeftAndFramebufferAbsoluteDespiteOffsetViewport() {
        val coverage = FrameCoverage(width, height)
        val probe = intArrayOf(20, 55, 4, 15)
        outside(coverage, probe)
        val viewport = intArrayOf(0, 10, width, 60)
        val data = vertices(intArrayOf(0, 0, width, 60), floatArrayOf(0f, 20f, 100f, 80f))
        assertTrue(coverage.affine2d(Passes.sprite, data, indices, ColorClear.projection(width, 60), viewport, null))
        assertEquals(pixels(probe).filter { (_, y) -> y >= 60 }.toSet(), remaining(coverage))
    }

    @Test
    fun fractionalClipEdgesConservativelyIncludeEveryIntersectingPixelCell() {
        val coverage = FrameCoverage(width, height)
        val data = vertices(whole, floatArrayOf(1.75f, 1.75f, 98.25f, 79.75f))
        assertTrue(coverage.affine2d(Passes.sprite, data, indices, projection, whole, null))
        assertEquals(pixels(whole).filter { (x, y) -> x == 0 || x == 99 || y == 79 }.toSet(), remaining(coverage))
    }

    @Test
    fun separateTrianglesMayHaveDifferentConstantClipsWithoutCoveringTheGap() {
        val coverage = FrameCoverage(width, height)
        val data = vertices(whole, floatArrayOf(0f, 0f, 49f, 80f)) + vertices(whole, floatArrayOf(51f, 0f, 100f, 80f))
        assertTrue(coverage.affine2d(Passes.sprite, data, indices + indices.map { it + 4 }.toIntArray(), projection, whole, null))
        assertArrayEquals(intArrayOf(49, 0, 2, height), coverage.inherited().single())
    }

    @Test
    fun geometryEntirelyOutsideFragmentClipDoesNotExcludeHistoryInsideItsBoundingBox() {
        val coverage = FrameCoverage(width, height)
        val probe = intArrayOf(5, 5, 2, 2)
        outside(coverage, probe)
        val data = vertices(intArrayOf(0, 0, 10, 10), floatArrayOf(90f, 0f, 100f, 80f))
        assertTrue(coverage.affine2d(Passes.sprite, data, indices, projection, whole, null))
        assertEquals(pixels(probe), remaining(coverage))
    }

    @Test
    fun coverageIntersectsGeometryFragmentClipViewportAndScissor() {
        val coverage = FrameCoverage(width, height)
        val probe = intArrayOf(45, 35, 10, 5)
        outside(coverage, probe)
        val data = vertices(intArrayOf(45, 35, 4, 4), floatArrayOf(47f, 40f, 100f, 80f))
        assertTrue(coverage.affine2d(Passes.sprite, data, indices, projection, whole, intArrayOf(0, 0, 49, height)))
        assertEquals(pixels(probe).filter { (x, _) -> x !in 47..48 }.toSet(), remaining(coverage))
    }

    @Test
    fun varyingClipsAtAnyTriangleVertexRequireConservativeFallbackIndependentOfProvokingVertex() {
        for (vertex in 0..2) for (component in 0..3) {
            val coverage = FrameCoverage(width, height)
            val data = vertices(whole, floatArrayOf(1f, 2f, 99f, 79f))
            val bytes = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val offset = vertex * Passes.sprite.stride + 24 + component * 4
            bytes.putFloat(offset, bytes.getFloat(offset) + 1f)
            assertFalse(coverage.affine2d(Passes.sprite, data, intArrayOf(0, 1, 2), projection, whole, null))
            coverage.cover(whole)
            assertTrue(coverage.inherited().isEmpty())
        }
    }

    @Test
    fun nonFiniteEmptyAndReversedClipsNeverAuthorizeSparseHistory() {
        val clips = listOf(
            floatArrayOf(50f, 0f, 50f, 80f),
            floatArrayOf(51f, 0f, 50f, 80f),
            floatArrayOf(0f, 40f, 100f, 40f),
            floatArrayOf(0f, 41f, 100f, 40f)
        ) + (0..3).flatMap { component ->
            listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).map { invalid ->
                floatArrayOf(1f, 2f, 99f, 79f).also { it[component] = invalid }
            }
        }
        for (clip in clips) {
            val coverage = FrameCoverage(width, height)
            assertFalse(coverage.affine2d(Passes.sprite, vertices(whole, clip), indices, projection, whole, null))
            coverage.cover(whole)
            assertTrue(coverage.inherited().isEmpty())
        }
    }

    @Test
    fun malformedLaterTriangleCannotRetainHistoryAfterEarlierTightCoverage() {
        val coverage = FrameCoverage(width, height)
        val data = vertices(whole, floatArrayOf(1f, 2f, 99f, 79f)) + vertices(whole, floatArrayOf(Float.NaN, 0f, 100f, 80f))
        assertFalse(coverage.affine2d(Passes.sprite, data, indices + indices.map { it + 4 }.toIntArray(), projection, whole, null))
        coverage.cover(whole)
        assertTrue(coverage.inherited().isEmpty())
    }

    @Test
    fun hugeFiniteClipBoundsClampWithoutOverflowOrOmittingGeometry() {
        val coverage = FrameCoverage(width, height)
        val data = vertices(whole, floatArrayOf(-Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE))
        assertTrue(coverage.affine2d(Passes.sprite, data, indices, projection, whole, null))
        assertTrue(coverage.inherited().isEmpty())
    }

    @Test
    fun spriteClipProofRequiresExactPerVertexFloatVec4AttributeLayout() {
        assertTrue(CaptureTopology.spriteClipAttribute(GL_FLOAT, 4, 0, false))
        for (type in listOf(GL_DOUBLE, GL_HALF_FLOAT, GL_INT, GL_SHORT, GL_UNSIGNED_BYTE)) assertFalse(CaptureTopology.spriteClipAttribute(type, 4, 0, false))
        for (size in 1..3) assertFalse(CaptureTopology.spriteClipAttribute(GL_FLOAT, size, 0, false))
        assertFalse(CaptureTopology.spriteClipAttribute(GL_FLOAT, 4, 1, false))
        assertFalse(CaptureTopology.spriteClipAttribute(GL_FLOAT, 4, 0, true))
    }
}
