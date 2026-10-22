package com.jagex.game.runetek5.graphics.modern.dump

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.Inflater
import org.junit.Assert.*
import org.junit.Test

class EncodedRecordCacheTest {
    private fun write(path: Path, cache: EncodedRecordCache? = null, block: (Recorder) -> Unit): Map<String, Long> {
        val writer = OrfWriter(path, 8, 8, FloatArray(4), cache)
        writer.use { block(Recorder(it, 8, 8)) }
        return writer.profile.snapshot()
    }

    private fun records(path: Path): List<Pair<Int, ByteArray>> {
        val b = ByteBuffer.wrap(Files.readAllBytes(path)).order(ByteOrder.LITTLE_ENDIAN)
        b.position(32)
        val records = arrayListOf<Pair<Int, ByteArray>>()
        while (true) {
            val kind = b.int
            val raw = b.int
            val packed = b.int
            if (kind == OrfWriter.END) break
            val bytes = ByteArray(if (packed == 0) raw else packed).also(b::get)
            records += kind to if (packed == 0) bytes else ByteArray(raw).also { output ->
                val inflater = Inflater()
                try {
                    inflater.setInput(bytes)
                    assertEquals(raw, inflater.inflate(output))
                    assertTrue(inflater.finished())
                } finally {
                    inflater.end()
                }
            }
        }
        assertEquals(0, b.remaining())
        return records
    }

    @Test fun cacheHitsEmitIdenticalCompleteFilesAndCanonicalDraws() {
        val dir = Files.createTempDirectory("orf-cache-hit")
        val cache = EncodedRecordCache()
        try {
            val pixels = ByteArray(4096) { (it % 7).toByte() }
            val vertices = ByteArray(3 * Passes.model.stride)
            val indices = Rec(12).u32(0, 1, 2).array()
            val encode: (Recorder) -> Unit = { recorder ->
                val texture = recorder.texture(32, 32, 1, pixels)
                val mesh = recorder.mesh(Passes.model, vertices, indices)
                recorder.draw(Recorder.DrawRec(0, mesh, Recorder.NONE, intArrayOf(texture), 0, IntArray(4), IntArray(4), Recorder.NONE, ""))
            }
            write(dir.resolve("offline.orf"), block = encode)
            write(dir.resolve("cold.orf"), cache, encode)
            val warm = write(dir.resolve("warm.orf"), cache, encode)
            assertEquals(2L, warm["encodedCacheHits"])
            assertEquals(0L, warm["compressNanos"] ?: 0L)
            assertArrayEquals(Files.readAllBytes(dir.resolve("offline.orf")), Files.readAllBytes(dir.resolve("cold.orf")))
            assertArrayEquals(Files.readAllBytes(dir.resolve("offline.orf")), Files.readAllBytes(dir.resolve("warm.orf")))
            val decoded = records(dir.resolve("warm.orf"))
            assertArrayEquals(pixels, decoded.first { it.first == OrfWriter.TEXTURE }.second.copyOfRange(16, 4112))
            assertEquals(64, decoded.single { it.first == OrfWriter.DRAW }.second.size)
        } finally {
            cache.close()
            dir.toFile().deleteRecursively()
        }
    }

    @Test fun freshContentAndCurrentHeaderIdsInvalidateReuse() {
        val dir = Files.createTempDirectory("orf-cache-change")
        val cache = EncodedRecordCache()
        try {
            val pixels = ByteArray(1024)
            write(dir.resolve("first.orf"), cache) { it.texture(16, 16, 1, pixels) }
            pixels[0] = 91
            val changed = write(dir.resolve("changed.orf"), cache) { it.texture(16, 16, 1, pixels) }
            assertEquals(0L, changed["encodedCacheHits"] ?: 0L)
            assertArrayEquals(pixels, records(dir.resolve("changed.orf")).single().second.copyOfRange(16, 1040))
            val reordered = write(dir.resolve("reordered.orf"), cache) {
                it.texture(1, 1, 1, ByteArray(4))
                it.texture(16, 16, 1, pixels)
            }
            assertEquals(0L, reordered["encodedCacheHits"] ?: 0L)
            val texture = records(dir.resolve("reordered.orf")).last().second
            assertEquals(1, ByteBuffer.wrap(texture).order(ByteOrder.LITTLE_ENDIAN).int)
            assertArrayEquals(pixels, texture.copyOfRange(16, texture.size))
        } finally {
            cache.close()
            dir.toFile().deleteRecursively()
        }
    }

    @Test fun typedFormatSamplerAndUncompressedPolicyStayExact() {
        val dir = Files.createTempDirectory("orf-cache-typed")
        val cache = EncodedRecordCache()
        try {
            val half = ByteArray(65536).also { java.util.Random(42).nextBytes(it) }
            val encode: (Recorder) -> Unit = { it.texture(128, 64, 1, half, RecordedTextureFormat.RGBA16F, 9) }
            write(dir.resolve("cold.orf"), cache, encode)
            val warm = write(dir.resolve("warm.orf"), cache, encode)
            assertEquals(1L, warm["encodedCacheHits"])
            assertArrayEquals(Files.readAllBytes(dir.resolve("cold.orf")), Files.readAllBytes(dir.resolve("warm.orf")))
            val bytes = Files.readAllBytes(dir.resolve("warm.orf"))
            assertEquals("ORF4", String(bytes, 0, 4))
            assertEquals(0, ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(40))
            val typed = records(dir.resolve("warm.orf")).single()
            assertEquals(OrfWriter.TYPED_TEXTURE, typed.first)
            assertArrayEquals(half, typed.second.copyOfRange(28, typed.second.size))
            val changed = write(dir.resolve("sampler.orf"), cache) { it.texture(128, 64, 1, half, RecordedTextureFormat.RGBA16F, 25) }
            assertEquals(0L, changed["encodedCacheHits"] ?: 0L)
            assertEquals(25, ByteBuffer.wrap(records(dir.resolve("sampler.orf")).single().second).order(ByteOrder.LITTLE_ENDIAN).getInt(24))
        } finally {
            cache.close()
            dir.toFile().deleteRecursively()
        }
    }

    @Test fun entryAndByteBudgetsEvictLeastRecentlyUsedAndCloseDisablesWrites() {
        val cache = EncodedRecordCache(900, 2)
        val keys = (0..3).map { EncodedRecordCache.Key(OrfWriter.TEXTURE, "fp$it", "header$it", 64) }
        val data = ByteArray(64) { 7 }
        val record = EncodedRecordCache.EncodedRecord(OrfWriter.TEXTURE, data.size, data, 0)
        data[0] = 99
        cache.put(keys[0], record)
        cache.put(keys[1], record)
        assertNotNull(cache.get(keys[0]))
        assertNull(cache.get(keys[0].copy(kind = OrfWriter.MESH)))
        assertNull(cache.get(keys[0].copy(header = "changed-header")))
        cache.put(keys[2], record)
        assertNull(cache.get(keys[1]))
        assertTrue(cache.stats().bytes <= 900)
        assertEquals(2, cache.stats().entries)
        assertEquals(1L, cache.stats().evictions)
        val out = ByteArrayOutputStream()
        cache.get(keys[0])!!.writeTo(out)
        assertEquals(7, out.toByteArray()[12].toInt())
        cache.put(keys[3], EncodedRecordCache.EncodedRecord(1, 1000, ByteArray(1000), 0))
        assertEquals(2, cache.stats().entries)
        cache.close()
        cache.put(keys[3], record)
        assertTrue(cache.stats().closed)
        assertEquals(0L, cache.stats().bytes)
        assertEquals(0, cache.stats().entries)
        assertNull(cache.get(keys[0]))
        val byteLimited = EncodedRecordCache(300, 10)
        byteLimited.put(keys[0], record)
        byteLimited.put(keys[1], record)
        assertEquals(1, byteLimited.stats().entries)
        assertEquals(1L, byteLimited.stats().evictions)
        assertTrue(byteLimited.stats().bytes <= 300)
        byteLimited.put(keys[2].copy(fingerprint = "x".repeat(1000)), record)
        assertEquals(1, byteLimited.stats().entries)
        byteLimited.close()
    }

    @Test fun writerFailureDeletesPartialFileAndClearsCache() {
        val dir = Files.createTempDirectory("orf-cache-failure")
        val cache = EncodedRecordCache()
        try {
            write(dir.resolve("valid.orf"), cache) { it.texture(16, 16, 1, ByteArray(1024)) }
            assertTrue(cache.stats().entries > 0)
            val path = dir.resolve("failed.orf")
            val writer = OrfWriter(path, 8, 8, FloatArray(4), cache)
            assertEquals("capture failed", runCatching { writer.close(IllegalStateException("capture failed")) }.exceptionOrNull()?.message)
            assertTrue(Files.notExists(path))
            assertTrue(cache.stats().closed)
            assertEquals(0L, cache.stats().bytes)
        } finally {
            cache.close()
            dir.toFile().deleteRecursively()
        }
    }

    @Test fun repeated64MiBEncodingReportsColdAndCachedCompressionCpu() {
        val dir = Files.createTempDirectory("orf-cache-bench")
        val cache = EncodedRecordCache()
        try {
            val pixels = ByteArray(16 * 1024 * 1024) { (it % 251).toByte() }
            val encode: (Recorder) -> Unit = { recorder ->
                repeat(4) { pixels[0] = it.toByte(); recorder.texture(2048, 2048, 1, pixels) }
            }
            val cold = write(dir.resolve("cold.orf"), cache, encode)
            val warm = write(dir.resolve("warm.orf"), cache, encode)
            assertEquals(4L, warm["encodedCacheHits"])
            assertEquals(64L * 1024 * 1024 + 64, warm["encodedCacheHitRawBytes"])
            assertEquals(0L, warm["compressNanos"] ?: 0L)
            assertArrayEquals(Files.readAllBytes(dir.resolve("cold.orf")), Files.readAllBytes(dir.resolve("warm.orf")))
            println("[EncodedRecordCacheTest] rawBytes=${warm["rawBytes"]} coldCompressNanos=${cold["compressNanos"]} warmCompressNanos=${warm["compressNanos"] ?: 0L} coldCompressCpuNanos=${cold["compressCpuNanos"] ?: "unsupported"} warmCompressCpuNanos=${warm["compressCpuNanos"] ?: 0L} coldWallNanos=${cold["wallCaptureNanos"]} warmWallNanos=${warm["wallCaptureNanos"]} retainedBytes=${cache.stats().bytes}")
        } finally {
            cache.close()
            dir.toFile().deleteRecursively()
        }
    }
}
