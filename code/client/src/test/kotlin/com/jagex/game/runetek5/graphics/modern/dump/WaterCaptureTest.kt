package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.lwjgl.opengl.GL33.*
import org.lwjgl.opengl.GL40.GL_PATCHES

class WaterCaptureTest {
    @Test
    fun cubeFacesRetainPositiveNegativeXyzOrder() {
        val visited = ArrayList<Int>()
        val packed = TexturePacking.pack(2, 2, 6, true) { face, out, offset, size ->
            visited.add(face)
            assertEquals(16, size)
            assertEquals(face * 16, offset)
            out.fill((face + 1).toByte(), offset, offset + size)
        }
        assertEquals((0..5).toList(), visited)
        for (face in 0..5) assertArrayEquals(ByteArray(16) { (face + 1).toByte() }, packed.copyOfRange(face * 16, (face + 1) * 16))
        assertThrows(IllegalArgumentException::class.java) { TexturePacking.pack(2, 3, 6, true) { _, _, _, _ -> } }
        assertThrows(IllegalArgumentException::class.java) { TexturePacking.pack(2, 2, 5, true) { _, _, _, _ -> } }
    }

    @Test
    fun volumeUsesOneContiguousXyzReadWithoutRowFlips() {
        var calls = 0
        val packed = TexturePacking.pack(2, 3, 4, false) { face, out, offset, size ->
            calls++
            assertEquals(0, face)
            assertEquals(0, offset)
            assertEquals(96, size)
            for (i in out.indices) out[i] = i.toByte()
        }
        assertEquals(1, calls)
        assertArrayEquals(ByteArray(96) { it.toByte() }, packed)
    }

    @Test
    fun dimensionsAndByteLimitsRejectBeforeAllocating() {
        for ((w, h, d) in listOf(Triple(0, 1, 1), Triple(1, -1, 1), Triple(1, 1, 0), Triple(8193, 1, 1), Triple(1, 1, 2049), Triple(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE), Triple(8192, 8192, 2048))) {
            assertThrows(IllegalArgumentException::class.java) { TexturePacking.byteSize(w, h, d) }
        }
        assertEquals(128 * 128 * 16 * 4, TexturePacking.byteSize(128, 128, 16))
        assertEquals(8192 * 1023 * 4, TexturePacking.byteSize(8192, 1023, 1))
        assertThrows(IllegalArgumentException::class.java) { TexturePacking.byteSize(8192, 1024, 1) }
    }

    @Test
    fun swizzleMatchesSampledLuminanceAlphaWithoutAliasing() {
        val pixels = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 80)
        TexturePacking.swizzle(pixels, intArrayOf(0, 0, 0, 1))
        assertArrayEquals(byteArrayOf(10, 10, 10, 20, 50, 50, 50, 60), pixels)
        TexturePacking.swizzle(pixels, intArrayOf(3, 2, 5, 4))
        assertArrayEquals(byteArrayOf(20, 10, -1, 0, 60, 50, -1, 0), pixels)
    }

    @Test
    fun reflectionRowsFlipWithinEveryLayerOnly() {
        val original = ByteArray(2 * 3 * 2 * 4) { it.toByte() }
        val pixels = original.clone()
        TexturePacking.flipRows(pixels, 2, 3, 2)
        for (z in 0..1) for (y in 0..2) {
            val from = (z * 3 + y) * 8
            val to = (z * 3 + 2 - y) * 8
            assertArrayEquals(original.copyOfRange(from, from + 8), pixels.copyOfRange(to, to + 8))
        }
        TexturePacking.flipRows(pixels, 2, 3, 2)
        assertArrayEquals(original, pixels)
    }

    @Test
    fun waterSamplersUseTheirDeclaredTargets() {
        for (sampler in listOf("uWaveNoise", "uFoamNoise", "uWaterNoise", "uNormalSampler", "uOverlayTexture3D", "uTexture3D")) assertEquals(sampler, GL_TEXTURE_3D, SamplerTargets.of(sampler))
        for (sampler in listOf("uEnvMap", "uEnvMapSampler", "uCubeMap")) assertEquals(sampler, GL_TEXTURE_CUBE_MAP, SamplerTargets.of(sampler))
        for (sampler in listOf("uSceneColor", "uSceneDepth", "uReflection", "uTexture", "uShadowTex")) assertEquals(sampler, GL_TEXTURE_2D, SamplerTargets.of(sampler))
        assertEquals(listOf("uTexture", "uGroundShadowTex", "uWaterNoise"), Passes.terrain.samplers)
    }

    @Test
    fun patchesRequireTheNamedHdWaterTriangleProgram() {
        assertTrue(CaptureTopology.supported("hdWater", GL_PATCHES, 3))
        assertTrue(CaptureTopology.supported("hdWater", GL_TRIANGLES, 0))
        assertFalse(CaptureTopology.supported("groundTextured", GL_PATCHES, 3))
        assertFalse(CaptureTopology.supported("hdWater", GL_PATCHES, 4))
        assertFalse(CaptureTopology.supported("hdWater", GL_TRIANGLE_STRIP, 3))
    }

    @Test
    fun indirectCaptureRejectsUnimplementedLayoutsBeforeReadingOrTrackingCoverage() {
        assertEquals(0, CaptureTopology.indirectBytes(0, GL_UNSIGNED_INT, 0))
        assertEquals(60, CaptureTopology.indirectBytes(3, GL_UNSIGNED_INT, 0))
        assertEquals(60, CaptureTopology.indirectBytes(3, GL_UNSIGNED_INT, 20))
        assertThrows(IllegalArgumentException::class.java) { CaptureTopology.indirectBytes(3, GL_UNSIGNED_SHORT, 0) }
        assertThrows(IllegalArgumentException::class.java) { CaptureTopology.indirectBytes(3, GL_UNSIGNED_INT, 24) }
        assertThrows(IllegalArgumentException::class.java) { CaptureTopology.indirectBytes(-1, GL_UNSIGNED_INT, 0) }
        assertThrows(IllegalArgumentException::class.java) { CaptureTopology.indirectBytes(Int.MAX_VALUE, GL_UNSIGNED_INT, 0) }
        assertThrows(IllegalArgumentException::class.java) { CaptureTopology.indirectBytes(TexturePacking.MAX_BYTES / 20 + 1, GL_UNSIGNED_INT, 0) }
    }

    @Test
    fun patchIndexOrderAndBaseVertexArePreserved() {
        val data = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
        for (i in listOf(65535, 1, 4, 4, 1, 6)) data.putShort(i.toShort())
        assertArrayEquals(intArrayOf(65542, 8, 11, 11, 8, 13), CaptureTopology.indices(data.array(), 2, 7))
        assertArrayEquals(intArrayOf(255, 0, 128), CaptureTopology.indices(byteArrayOf(-1, 0, -128), 1, 0))
        assertThrows(IllegalArgumentException::class.java) { CaptureTopology.indices(byteArrayOf(0), 2, 0) }
        assertThrows(IllegalArgumentException::class.java) { CaptureTopology.indices(ByteArray(4) { -1 }, 4, 1) }
    }

    @Test
    fun tessellationSizeOccupiesUnusedPaddingWithoutChangingShaderLayout() {
        val packed = ByteBuffer.wrap(Passes.hdWater.pack(mapOf("uTessPixels" to floatArrayOf(13f), "uShadowEnabled" to floatArrayOf(1f), "uInvViewProjection" to FloatArray(16) { it.toFloat() }))).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(4432, packed.capacity())
        assertEquals(1, packed.getInt(112))
        assertEquals(13f, packed.getFloat(116), 0f)
        assertEquals(0, packed.getInt(120))
        assertEquals(0f, packed.getFloat(128), 0f)
        assertEquals(15f, packed.getFloat(188), 0f)
        assertFalse(Passes.hdWater.slots.containsKey("uTessPixels"))
    }
}
