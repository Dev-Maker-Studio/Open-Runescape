package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TypedTextureTest {
    private val pixel = byteArrayOf(0, 0x44, 0, 0xb8.toByte(), 0, 0, 0, 0x3c)

    @Test
    fun jvmWriterMatchesSharedTypedFixture() {
        val dir = Files.createTempDirectory("orf-half")
        try {
            val path = dir.resolve("typed.orf")
            OrfWriter(path, 2, 1, floatArrayOf(0f, 0f, 0f, 1f)).use {
                it.texture(7, 2, 1, 1, pixel + pixel, RecordedTextureFormat.RGBA16F)
            }
            assertArrayEquals(Files.readAllBytes(Path.of("../renderer/tests/fixtures/typed.orf")), Files.readAllBytes(path))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun halfComponentsSwizzleAndFlipWithoutQuantization() {
        val bytes = pixel + byteArrayOf(0, 0x80.toByte(), 1, 0, -1, 0x7b, 0, 0x3c)
        TexturePacking.swizzle(bytes, intArrayOf(1, 0, 5, 4), RecordedTextureFormat.RGBA16F)
        assertArrayEquals(byteArrayOf(0, 0xb8.toByte(), 0, 0x44, 0, 0x3c, 0, 0, 1, 0, 0, 0x80.toByte(), 0, 0x3c, 0, 0), bytes)
        val original = bytes.clone()
        TexturePacking.flipRows(bytes, 1, 2, 1, RecordedTextureFormat.RGBA16F)
        assertArrayEquals(original.copyOfRange(8, 16) + original.copyOfRange(0, 8), bytes)
        TexturePacking.flipRows(bytes, 1, 2, 1, RecordedTextureFormat.RGBA16F)
        assertArrayEquals(original, bytes)
        assertEquals(16, TexturePacking.byteSize(2, 1, 1, RecordedTextureFormat.RGBA16F))
        assertThrows(IllegalArgumentException::class.java) { TexturePacking.byteSize(8192, 512, 1, RecordedTextureFormat.RGBA16F) }
        assertThrows(IllegalArgumentException::class.java) { TexturePacking.byteSize(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE, RecordedTextureFormat.RGBA16F) }
    }

    @Test
    fun textureDedupIncludesFormatAndTypedSampler() {
        val dir = Files.createTempDirectory("orf-half-identity")
        try {
            OrfWriter(dir.resolve("identity.orf"), 2, 1, FloatArray(4)).use { writer ->
                val recorder = Recorder(writer, 2, 1)
                val a = recorder.texture(2, 1, 1, pixel + pixel, RecordedTextureFormat.RGBA16F)
                assertEquals(a, recorder.texture(2, 1, 1, pixel + pixel, RecordedTextureFormat.RGBA16F))
                val b = recorder.texture(2, 1, 1, pixel + pixel, RecordedTextureFormat.RGBA16F, 0)
                assertEquals(a + 1, b)
                assertEquals(b + 1, recorder.texture(2, 2, 1, pixel + pixel))
                assertThrows(IllegalArgumentException::class.java) { writer.texture(5, 2, 1, 1, pixel, RecordedTextureFormat.RGBA16F) }
                assertThrows(IllegalArgumentException::class.java) { writer.texture(5, 2, 1, 1, pixel + pixel, RecordedTextureFormat.RGBA16F, 256) }
            }
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
