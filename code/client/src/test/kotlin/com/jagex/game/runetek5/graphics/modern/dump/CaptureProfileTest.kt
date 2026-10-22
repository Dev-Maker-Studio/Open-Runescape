package com.jagex.game.runetek5.graphics.modern.dump

import org.junit.Assert.*
import org.junit.Test

class CaptureProfileTest {
    @Test fun denseAtlasPreservesSparseSlotPixelsAndPadding() {
        val p = GlRead.Pixels(2, 1, byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        val q = GlRead.Pixels(1, 2, byteArrayOf(9, 10, 11, 12, 13, 14, 15, 16))
        val atlas = SpriteAtlas(mapOf(7 to q, 3 to p))
        assertEquals(mapOf(3 to 0, 7 to 1), atlas.layers)
        assertEquals(32, atlas.rgba.size)
        assertArrayEquals(p.rgba, atlas.rgba.copyOfRange(0, 8))
        assertArrayEquals(ByteArray(8), atlas.rgba.copyOfRange(8, 16))
        assertArrayEquals(q.rgba.copyOfRange(0, 4), atlas.rgba.copyOfRange(16, 20))
        assertArrayEquals(q.rgba.copyOfRange(4, 8), atlas.rgba.copyOfRange(24, 28))
        assertEquals(2 to 1, atlas.sizes[3])
        assertEquals(1 to 2, atlas.sizes[7])
    }

    @Test fun missingTextureStillHasAnExplicitZeroLayer() {
        val atlas = SpriteAtlas(mapOf(7 to null))
        assertEquals(mapOf(7 to 0), atlas.layers)
        assertEquals(4, atlas.rgba.size)
        assertArrayEquals(ByteArray(4), atlas.rgba)
    }

    @Test fun profilerCountsFailuresAndStopsAtFrameBoundary() {
        val profile = CaptureProfile()
        CaptureProfiler.begin(profile)
        try {
            assertEquals(42, CaptureProfiler.measure("bufferReadback") { 42 })
            var nullableCalls = 0
            assertNull(CaptureProfiler.measure("nullableReadback") { nullableCalls++; null })
            assertEquals(1, nullableCalls)
            assertThrows(IllegalStateException::class.java) { CaptureProfiler.measure("textureReadback") { error("failed") } }
        } finally {
            CaptureProfiler.end()
        }
        CaptureProfiler.measure("outside") { 1 }
        val data = profile.snapshot()
        assertTrue(data.getValue("bufferReadbackNanos") > 0)
        assertTrue(data.getValue("textureReadbackNanos") > 0)
        assertFalse(data.containsKey("outsideNanos"))
        assertTrue(data.getValue("wallCaptureNanos") > 0)
    }
}
