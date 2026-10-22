package com.darkan.bot

import kotlinx.serialization.json.*
import com.jagex.game.runetek5.graphics.modern.dump.EncodedRecordCache
import com.jagex.game.runetek5.graphics.modern.dump.OrfWriter
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class NativeStreamTest {
    private fun waitFor(test: () -> Boolean) {
        val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!test()) {
            check(System.nanoTime() < end) { "condition timed out" }
            Thread.sleep(5)
        }
    }

    private fun metadata(dir: Path) = Json.parseToJsonElement(Files.readString(dir.resolve("latest.json"))).jsonObject

    @Test fun slowCapturesDoNotWaitForAnExtraFutureSlot() {
        val pacer = StreamPacer(5, 0L)
        assertEquals(0L, pacer.delay(230_000_000L))
        assertEquals(1L, pacer.drops)
        assertEquals(0L, pacer.delay(460_000_000L))
        assertEquals(2L, pacer.drops)
        assertEquals(170_000_000L, pacer.delay(490_000_000L))
    }

    @Test fun requestedRateBelowCaptureCapacityStillWaits() {
        val pacer = StreamPacer(1, 0L)
        assertEquals(970_000_000L, pacer.delay(30_000_000L))
        assertEquals(970_000_000L, pacer.delay(1_030_000_000L))
        assertEquals(0L, pacer.drops)
    }

    @Test fun rolloverPublishesCompleteBoundedFrames() {
        val dir = Files.createTempDirectory("stream-test")
        val calls = AtomicInteger()
        val producer = StreamProducer { path ->
            val value = calls.incrementAndGet()
            Files.writeString(path, "ORF3-$value")
            StreamCapture(800, 600, value, value.toLong(), value.toLong(), "capture $value", mapOf("textureReadbackNanos" to 123L))
        }
        try {
            producer.start(dir, 30)
            waitFor { producer.state()["frames"]!!.jsonPrimitive.long >= 5 }
            producer.stop()
            val data = metadata(dir)
            val frame = data["frame"]!!.jsonPrimitive.long
            assertEquals(frame and 1, data["slot"]!!.jsonPrimitive.long)
            val bytes = Files.readAllBytes(dir.resolve(data["file"]!!.jsonPrimitive.content))
            assertEquals("ORF3-$frame", String(bytes))
            val hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))
            assertEquals(hash, data["sha256"]!!.jsonPrimitive.content)
            assertEquals(frame, data["inputSeq"]!!.jsonPrimitive.long)
            assertEquals(800, data["width"]!!.jsonPrimitive.int)
            val timings = data["profile"]!!.jsonObject
            assertEquals(123L, timings["textureReadbackNanos"]!!.jsonPrimitive.long)
            assertTrue(timings["captureRequestNanos"]!!.jsonPrimitive.long > 0)
            assertTrue(timings["checksumNanos"]!!.jsonPrimitive.long > 0)
            assertTrue(producer.state()["profile"]!!.jsonObject["publicationNanos"]!!.jsonPrimitive.long > 0)
            assertEquals(setOf("frame-0.orf", "frame-1.orf", "latest.json"), Files.list(dir).use { paths -> paths.map { it.fileName.toString() }.toList().toSet() })
            producer.start(dir, 30)
            waitFor { producer.state()["frames"]!!.jsonPrimitive.long > 0 }
            producer.stop()
            assertTrue(metadata(dir)["frame"]!!.jsonPrimitive.long > frame)
        } finally { producer.stop(); dir.toFile().deleteRecursively() }
    }

    @Test fun stopCancelsPendingCaptureAndCleansTemps() {
        val dir = Files.createTempDirectory("stream-stop")
        val entered = CountDownLatch(1)
        val producer = StreamProducer { path ->
            Files.writeString(path, "partial")
            entered.countDown()
            CountDownLatch(1).await()
            error("unreachable")
        }
        try {
            producer.start(dir)
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            producer.stop()
            assertFalse(producer.state()["running"]!!.jsonPrimitive.boolean)
            assertEquals(0L, producer.state()["frames"]!!.jsonPrimitive.long)
            assertEquals(0, Files.list(dir).use { it.count() }.toInt())
            producer.stop()
        } finally { producer.stop(); dir.toFile().deleteRecursively() }
    }

    @Test fun errorsAreVisibleAndStartValidationIsStrict() {
        val dir = Files.createTempDirectory("stream-error")
        val producer = StreamProducer { error("capture failed") }
        try {
            assertThrows(IllegalArgumentException::class.java) { producer.start(dir, 31) }
            producer.start(dir)
            waitFor { !producer.state()["running"]!!.jsonPrimitive.boolean }
            assertEquals("capture failed", producer.state()["error"]!!.jsonPrimitive.content)
            producer.stop()
            assertFalse(Files.exists(dir.resolve("latest.json")))
            val file = dir.resolve("file")
            Files.writeString(file, "x")
            assertThrows(java.nio.file.FileAlreadyExistsException::class.java) { producer.start(file) }
        } finally { producer.stop(); dir.toFile().deleteRecursively() }
    }

    @Test fun streamLifecycleClearsEncodedCacheOnStopAndCaptureError() {
        val dir = Files.createTempDirectory("stream-cache-lifecycle")
        val epochs = arrayListOf<EncodedRecordCache>()
        var cache: EncodedRecordCache? = null
        var fail = false
        val producer = StreamProducer(onStart = {
            cache = EncodedRecordCache().also {
                it.put(EncodedRecordCache.Key(OrfWriter.TEXTURE, "fingerprint", "header", 16), EncodedRecordCache.EncodedRecord(OrfWriter.TEXTURE, 16, ByteArray(16), 0))
                epochs += it
            }
        }, onStop = { cache?.close() }) { path ->
            if (fail) error("capture failed")
            Files.writeString(path, "ORF3")
            StreamCapture(1, 1, 0, 0, 0, "ok")
        }
        try {
            producer.start(dir)
            waitFor { producer.state()["frames"]!!.jsonPrimitive.long > 0 }
            producer.stop()
            assertTrue(epochs.single().stats().closed)
            assertEquals(0L, epochs.single().stats().bytes)
            fail = true
            producer.start(dir)
            waitFor { !producer.state()["running"]!!.jsonPrimitive.boolean }
            producer.stop()
            assertEquals("capture failed", producer.state()["error"]!!.jsonPrimitive.content)
            assertEquals(2, epochs.size)
            assertNotSame(epochs[0], epochs[1])
            assertTrue(epochs.all { it.stats().closed && it.stats().entries == 0 && it.stats().bytes == 0L })
        } finally {
            producer.stop()
            dir.toFile().deleteRecursively()
        }
    }

    @Test fun startFailureAlsoRunsLifecycleCleanup() {
        val dir = Files.createTempDirectory("stream-start-failure")
        val cache = EncodedRecordCache()
        val producer = StreamProducer(onStart = { error("start failed") }, onStop = { cache.close() }) { error("unreachable") }
        try {
            assertThrows(IllegalStateException::class.java) { producer.start(dir) }
            assertTrue(cache.stats().closed)
        } finally {
            producer.stop()
            dir.toFile().deleteRecursively()
        }
    }

    @Test fun noConcurrentCapturesAndMissedDeadlinesCountDrops() {
        val dir = Files.createTempDirectory("stream-bounded")
        val entered = CountDownLatch(1)
        val producer = StreamProducer { path ->
            entered.countDown()
            Thread.sleep(100)
            Files.writeString(path, "ORF3")
            StreamCapture(1, 1, 0, 0, 0, "ok")
        }
        try {
            producer.start(dir, 30)
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            assertThrows(IllegalStateException::class.java) { producer.start(dir) }
            waitFor { producer.state()["frames"]!!.jsonPrimitive.long >= 2 }
            assertTrue(producer.state()["drops"]!!.jsonPrimitive.long > 0)
        } finally { producer.stop(); dir.toFile().deleteRecursively() }
    }
}
