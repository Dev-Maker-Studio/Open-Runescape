package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.Inflater
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.lwjgl.opengl.GL33.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL33.GL_FILL
import org.lwjgl.opengl.GL33.GL_LINE
import org.lwjgl.opengl.GL33.GL_LINES
import org.lwjgl.opengl.GL33.GL_NEAREST
import org.lwjgl.opengl.GL33.GL_POINT
import org.lwjgl.opengl.GL33.GL_POINTS
import org.lwjgl.opengl.GL33.GL_TRIANGLES
import org.lwjgl.opengl.GL40.GL_PATCHES

object HistoryFixture {
    fun write(path: Path) {
        val width = 20
        val height = 10
        val coverage = FrameCoverage(width, height)
        val aliases = FrameAliases(0, width, height)
        val whole = intArrayOf(0, 0, width, height)
        check(aliases.register(0, 7, width, height))
        check(aliases.identityCopy(0, 7, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
        coverage.cover(intArrayOf(1, 0, 18, 10))
        check(aliases.identityCopy(7, 0, whole, whole, GL_COLOR_BUFFER_BIT, GL_NEAREST, false))
        OrfWriter(path, width, height, floatArrayOf(141f / 255, 164f / 255, 194f / 255, 1f)).use { writer ->
            val rec = Recorder(writer, width, height)
            rec.skip("unported.mesh", 0, intArrayOf(1, 0, 18, 10), IntArray(4))
            for (region in coverage.inherited()) {
                val bytes = ByteArray(region[2] * region[3] * 4)
                for (y in 0 until region[3]) for (x in 0 until region[2]) {
                    val offset = (y * region[2] + x) * 4
                    bytes[offset + 2] = (region[1] + y + 1).toByte()
                    bytes[offset + 3] = if ((region[1] + y) % 2 == 0) 0 else 127
                }
                rec.inherited(region, bytes)
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) = write(Path.of(args.single()))
}

class FrameCoverageTest {
    private fun contains(rect: IntArray, x: Int, y: Int) = x >= rect[0] && y >= rect[1] && x < rect[0] + rect[2] && y < rect[1] + rect[3]

    @Test
    fun unknownAndUnportedGeometryExcludeEntireViewportFromHistory() {
        val coverage = FrameCoverage(20, 10)
        coverage.cover(intArrayOf(1, 0, 18, 10))
        val regions = coverage.inherited()
        assertEquals(2, regions.size)
        assertArrayEquals(intArrayOf(0, 0, 1, 10), regions[0])
        assertArrayEquals(intArrayOf(19, 0, 1, 10), regions[1])
        for (y in 0 until 10) for (x in 1 until 19) assertFalse(regions.any { contains(it, x, y) })
        assertEquals(20, regions.sumOf { it[2] * it[3] })
    }

    @Test
    fun rejectsOverTenPercentWithoutClampingOrWholeFrameFallback() {
        val coverage = FrameCoverage(20, 10)
        coverage.cover(intArrayOf(2, 0, 17, 10))
        val failure = runCatching { coverage.inherited() }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure!!.message!!.contains("30 of 200"))
        assertTrue(runCatching { FrameCoverage(20, 10).inherited() }.isFailure)
    }

    @Test
    fun scissorClipsUnknownGeometryAndClearCoverage() {
        val coverage = FrameCoverage(20, 10)
        coverage.cover(intArrayOf(0, 0, 20, 10), intArrayOf(1, 0, 18, 10))
        coverage.cover(ColorClear.rectangle(20, 10, intArrayOf(0, 0, 1, 10))!!)
        assertEquals(1, coverage.inherited().size)
        assertArrayEquals(intArrayOf(19, 0, 1, 10), coverage.inherited().single())
    }

    @Test
    fun spriteTrianglesDoNotCoverTheSpaceBetweenSeparatedQuads() {
        val width = 128
        val height = 8
        val coverage = FrameCoverage(width, height)
        val vertices = ColorClear.vertices(intArrayOf(0, 0, 57, 8), FloatArray(4)) + ColorClear.vertices(intArrayOf(71, 0, 57, 8), FloatArray(4))
        val bytes = ByteBuffer.wrap(vertices).order(ByteOrder.LITTLE_ENDIAN)
        repeat(8) { vertex ->
            floatArrayOf(0f, 0f, width.toFloat(), height.toFloat()).forEachIndexed { component, value -> bytes.putFloat(vertex * Passes.sprite.stride + 24 + component * 4, value) }
        }
        assertTrue(coverage.affine2d(Passes.sprite,vertices, intArrayOf(0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7), ColorClear.projection(width, height), intArrayOf(0, 0, width, height), null))
        assertArrayEquals(intArrayOf(58, 0, 12, 8), coverage.inherited().single())
    }

    @Test
    fun surfaceMasksUseAuditedStride24TrianglesWithoutCoveringTheWholeBatch() {
        val coverage = FrameCoverage(128, 8)
        val source = ColorClear.vertices(intArrayOf(0, 0, 57, 8), FloatArray(4)) + ColorClear.vertices(intArrayOf(71, 0, 57, 8), FloatArray(4))
        val vertices = ByteArray(8 * Passes.surfaceMask.stride)
        repeat(8) { System.arraycopy(source, it * Passes.sprite.stride, vertices, it * Passes.surfaceMask.stride, 8) }
        val indices = intArrayOf(0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7)
        assertTrue(FrameCoverage.supports2d(Passes.surfaceMask))
        assertTrue(coverage.affine2d(Passes.surfaceMask, vertices, indices, ColorClear.projection(128, 8), intArrayOf(0, 0, 128, 8), null))
        assertArrayEquals(intArrayOf(58, 0, 12, 8), coverage.inherited().single())
        assertFalse(coverage.affine2d(Passes.surfaceMask, vertices.copyOf(vertices.size - 1), indices, ColorClear.projection(128, 8), intArrayOf(0, 0, 128, 8), null))
        assertFalse(coverage.affine2d(Passes.surfaceMask, vertices, intArrayOf(0, 1, 8), ColorClear.projection(128, 8), intArrayOf(0, 0, 128, 8), null))
    }

    @Test
    fun onlyNamedAuditedTwoDimensionalPassesCanUseTriangleCoverage() {
        val coverage = FrameCoverage(20, 10)
        val viewport = intArrayOf(1, 0, 18, 10)
        val vertices = ColorClear.vertices(intArrayOf(4, 4, 1, 1), FloatArray(4))
        for (pass in listOf(Passes.model, Passes.ground, Passes.instanced, Passes.terrain)) {
            assertFalse(FrameCoverage.supports2d(pass))
            assertFalse(coverage.affine2d(pass, vertices, intArrayOf(0, 1, 2), ColorClear.projection(20, 10), viewport, null))
        }
        assertFalse(FrameCoverage.supports2d(null))
        coverage.cover(viewport)
        assertTrue(coverage.inherited().none { contains(it, 4, 4) })
    }

    @Test
    fun invalidOrPerspectiveSpritesRequireConservativeViewportCoverage() {
        val coverage = FrameCoverage(20, 10)
        val viewport = intArrayOf(1, 0, 18, 10)
        val projection = ColorClear.projection(20, 10).also { it[3] = 0.1f }
        val vertices = ColorClear.vertices(intArrayOf(4, 4, 1, 1), FloatArray(4))
        assertFalse(coverage.affine2d(Passes.sprite,vertices, intArrayOf(0, 1, 2), projection, viewport, null))
        coverage.cover(viewport)
        assertEquals(20, coverage.inherited().sumOf { it[2] * it[3] })
        assertTrue(coverage.inherited().none { contains(it, 4, 4) })
    }

    @Test
    fun fullDestinationBlitLeavesNoEligibleHistory() {
        val coverage = FrameCoverage(20, 10)
        coverage.cover(intArrayOf(0, 0, 20, 10))
        assertTrue(coverage.inherited().isEmpty())
    }

    @Test
    fun excessiveSparseFragmentationRejectsInsteadOfReadingMorePixels() {
        val coverage = FrameCoverage(1000, 100)
        for (y in 0 until 100) for (x in 0 until 1000 step 10) {
            val gap = x + y % 2
            if (gap > x) coverage.cover(intArrayOf(x, y, gap - x, 1))
            coverage.cover(intArrayOf(gap + 1, y, x + 10 - gap - 1, 1))
        }
        val failure = runCatching { coverage.inherited() }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertEquals("untouched history region budget", failure!!.message)
    }

    @Test
    fun onlyAuditedFilledTrianglesHaveViewportBoundedRasterization() {
        assertTrue(CaptureTopology.viewportBounded(Passes.sprite, GL_TRIANGLES, GL_FILL))
        assertTrue(CaptureTopology.viewportBounded(Passes.model, GL_TRIANGLES, GL_FILL))
        assertTrue(CaptureTopology.viewportBounded(Passes.hdWater, GL_PATCHES, GL_FILL))
        assertFalse(CaptureTopology.viewportBounded(null, GL_TRIANGLES, GL_FILL))
        assertFalse(CaptureTopology.viewportBounded(Passes.sprite, GL_PATCHES, GL_FILL))
        assertFalse(CaptureTopology.viewportBounded(Passes.sprite, GL_POINTS, GL_FILL))
        assertFalse(CaptureTopology.viewportBounded(Passes.sprite, GL_LINES, GL_FILL))
        assertFalse(CaptureTopology.viewportBounded(Passes.sprite, GL_TRIANGLES, GL_LINE))
        assertFalse(CaptureTopology.viewportBounded(Passes.sprite, GL_TRIANGLES, GL_POINT))
    }

    @Test
    fun corePolygonModeQueryDoesNotRequireASecondStateValue() {
        val queried = IntArray(4)
        queried[0] = GL_FILL
        assertEquals(0, queried[1])
        for (pass in listOf(Passes.sprite, Passes.surfaceMask)) {
            assertTrue(CaptureTopology.viewportBounded(pass, GL_TRIANGLES, queried[0]))
            queried[1] = GL_POINT
            assertTrue(CaptureTopology.viewportBounded(pass, GL_TRIANGLES, queried[0]))
            queried[1] = 0
        }
    }

    @Test
    fun actualNonFillAndUnknownCorePolygonModesAlwaysRequireConservativeCoverage() {
        for (mode in intArrayOf(GL_LINE, GL_POINT, 0, -1, Int.MAX_VALUE)) {
            val queried = intArrayOf(mode, GL_FILL, GL_FILL, GL_FILL)
            for (pass in listOf(Passes.sprite, Passes.surfaceMask, Passes.model, Passes.hdWater)) {
                assertFalse(CaptureTopology.viewportBounded(pass, GL_TRIANGLES, queried[0]))
                assertFalse(CaptureTopology.viewportBounded(pass, GL_PATCHES, queried[0]))
            }
        }
    }

    @Test
    fun scalarCoreFillAllowsOnlyShaderProvenSparseBordersWhileNonFillExcludesThem() {
        val viewport = intArrayOf(0, 0, 20, 10)
        val vertices = ColorClear.vertices(intArrayOf(1, 0, 18, 10), floatArrayOf(1f, 1f, 1f, 0f))
        for (mode in intArrayOf(GL_FILL, GL_LINE, GL_POINT, 0)) {
            val queried = intArrayOf(mode, 0, 0, 0)
            val coverage = FrameCoverage(20, 10)
            val bounded = CaptureTopology.viewportBounded(Passes.sprite, GL_TRIANGLES, queried[0])
            if (!bounded || !coverage.affine2d(Passes.sprite, vertices, intArrayOf(0, 1, 2, 0, 2, 3), ColorClear.projection(20, 10), viewport, null)) coverage.cover(viewport)
            assertEquals(if (mode == GL_FILL) 20 else 0, coverage.eligiblePixels())
            assertEquals(if (mode == GL_FILL) 2 else 0, coverage.inherited().size)
        }
    }

    @Test
    fun unboundedRasterizationExcludesPixelsOutsideTheViewportButNotTheScissor() {
        val whole = intArrayOf(0, 0, 20, 10)
        val viewport = intArrayOf(5, 3, 10, 4)
        val scissor = intArrayOf(1, 0, 18, 10)
        val coverage = FrameCoverage(20, 10)
        val bounded = CaptureTopology.viewportBounded(null, GL_POINTS, GL_FILL)
        coverage.cover(if (bounded) viewport else whole, scissor)
        val regions = coverage.inherited()
        assertEquals(20, regions.sumOf { it[2] * it[3] })
        assertFalse(regions.any { contains(it, 2, 1) })
        coverage.cover(if (bounded) viewport else whole)
        assertTrue(coverage.inherited().isEmpty())
    }

    @Test
    fun nonFiniteAndCancellationSensitiveTransformsNeverAuthorizeTightCoverage() {
        val viewport = intArrayOf(0, 0, 128, 8)
        val vertices = ColorClear.vertices(intArrayOf(20, 0, 20, 8), FloatArray(4))
        for (bad in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            val projection = ColorClear.projection(128, 8).also { it[12] = bad }
            assertFalse(FrameCoverage(128, 8).affine2d(Passes.sprite, vertices, intArrayOf(0, 1, 2), projection, viewport, null))
            val data = vertices.copyOf().also { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putFloat(0, bad) }
            assertFalse(FrameCoverage(128, 8).affine2d(Passes.sprite, data, intArrayOf(0, 1, 2), ColorClear.projection(128, 8), viewport, null))
        }
        val projection = ColorClear.projection(128, 8).also { it[0] = 100000f; it[12] = -2000000f }
        assertFalse(FrameCoverage(128, 8).affine2d(Passes.sprite, vertices, intArrayOf(0, 1, 2), projection, viewport, null))
    }

    @Test
    fun clippedAndOverflowingRectanglesNeverUncoverPreviouslyExcludedPixels() {
        val coverage = FrameCoverage(20, 10)
        coverage.cover(intArrayOf(1, 0, Int.MAX_VALUE, 10), intArrayOf(0, 0, 19, 10))
        val before = coverage.inherited().map { it.copyOf() }
        coverage.cover(intArrayOf(Int.MAX_VALUE, 0, Int.MAX_VALUE, 10))
        coverage.cover(intArrayOf(Int.MIN_VALUE, 0, Int.MAX_VALUE, 10))
        coverage.cover(intArrayOf(0, 0, 20, 0))
        val after = coverage.inherited()
        assertEquals(before.size, after.size)
        before.indices.forEach { assertArrayEquals(before[it], after[it]) }
        coverage.cover(intArrayOf(0, 0, 20, 10))
        assertTrue(coverage.inherited().isEmpty())
    }

    @Test
    fun generatedHistoryFixtureIsBoundedAndReadable() {
        val path = Files.createTempFile("history", ".orf")
        try {
            HistoryFixture.write(path)
            assertTrue(Files.size(path) < 4096)
            val bytes = ByteBuffer.wrap(Files.readAllBytes(path)).order(ByteOrder.LITTLE_ENDIAN)
            bytes.position(32)
            val meshes = HashMap<Int, ByteBuffer>()
            val textures = HashMap<Int, ByteBuffer>()
            val draws = ArrayList<ByteBuffer>()
            val names = ArrayList<String>()
            while (true) {
                val kind = bytes.int
                val raw = bytes.int
                val packed = bytes.int
                if (kind == OrfWriter.END) break
                val payload = ByteArray(if (packed == 0) raw else packed).also(bytes::get)
                val record = if (packed == 0) payload else ByteArray(raw).also { out ->
                    val inflater = Inflater()
                    try {
                        inflater.setInput(payload)
                        assertEquals(raw, inflater.inflate(out))
                        assertTrue(inflater.finished())
                    } finally {
                        inflater.end()
                    }
                }
                val data = ByteBuffer.wrap(record).order(ByteOrder.LITTLE_ENDIAN)
                when (kind) {
                    OrfWriter.MESH -> meshes[data.getInt(0)] = data
                    OrfWriter.TEXTURE -> textures[data.getInt(0)] = data
                    OrfWriter.DRAW -> {
                        draws.add(data)
                        val offset = 56 + data.getInt(12) * 4
                        names.add(String(record, offset + 4, data.getInt(offset), Charsets.UTF_8))
                    }
                }
            }
            assertEquals(0, bytes.remaining())
            assertEquals(listOf("unported.mesh", "history.untouched", "history.untouched"), names)
            assertEquals(Recorder.SKIP, draws.first().getInt(0))
            assertEquals(1, textures.size)
            val regions = listOf(intArrayOf(0, 0, 1, 10), intArrayOf(19, 0, 1, 10))
            for ((i, draw) in draws.drop(1).withIndex()) {
                assertEquals(Passes.sprite.id, draw.getInt(0))
                assertEquals(1, draw.getInt(12))
                assertEquals(7 shl 7, draw.getInt(20))
                assertArrayEquals(intArrayOf(0, 0, 20, 10), IntArray(4) { draw.getInt(24 + it * 4) })
                assertArrayEquals(regions[i], IntArray(4) { draw.getInt(40 + it * 4) })
                val texture = textures.getValue(draw.getInt(16))
                assertEquals(1, texture.getInt(4))
                assertEquals(10, texture.getInt(8))
                assertEquals(1, texture.getInt(12))
                assertEquals(16 + 40, texture.capacity())
                for (y in 0 until 10) {
                    assertEquals(y + 1, texture.get(16 + y * 4 + 2).toInt())
                    assertEquals(if (y % 2 == 0) 0 else 127, texture.get(16 + y * 4 + 3).toInt())
                }
                val mesh = meshes.getValue(draw.getInt(4))
                assertEquals(Passes.sprite.id, mesh.getInt(4))
                assertEquals(4, mesh.getInt(8))
                assertEquals(6, mesh.getInt(12))
                repeat(4) { vertex ->
                    assertEquals(-3f, mesh.getFloat(16 + vertex * 40 + 20), 0f)
                    assertEquals(if (vertex == 1 || vertex == 2) 1f else 0f, mesh.getFloat(16 + vertex * 40 + 8), 0f)
                    assertEquals(if (vertex >= 2) 1f else 0f, mesh.getFloat(16 + vertex * 40 + 12), 0f)
                }
            }
        } finally {
            Files.deleteIfExists(path)
        }
    }
}
