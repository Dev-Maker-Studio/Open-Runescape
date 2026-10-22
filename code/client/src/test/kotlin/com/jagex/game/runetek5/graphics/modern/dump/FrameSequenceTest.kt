package com.jagex.game.runetek5.graphics.modern.dump

import com.darkan.bot.FrameCapture
import java.nio.ByteBuffer
import java.nio.file.Files
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FrameSequenceTest {
    @Test
    fun everyNthFrameIsDueUntilCountIsReached() {
        val seq = FrameSequence(Files.createTempDirectory("seq"), 3, 2)
        val due = ArrayList<Int>()
        var render = 0
        while (!seq.done) {
            if (seq.due()) {
                due += render
                seq.record(render, render * 10, render * 1000L)
            }
            render++
        }
        assertEquals(listOf(0, 2, 4), due)
        assertFalse(seq.due())
        assertEquals(listOf(0, 1, 2), seq.entries.map { it.index })
    }

    @Test
    fun defaultCapturesConsecutiveRenderedFrames() {
        val seq = FrameSequence(Files.createTempDirectory("seq"), 3, 1)
        for (frame in 20..22) {
            assertTrue(seq.due())
            seq.record(frame, 4, frame * 1000L)
        }
        assertEquals(listOf(20, 21, 22), seq.entries.map { it.frame })
        assertFalse(seq.due())
    }

    @Test
    fun filesAreNumberedWithFourDigits() {
        val dir = Files.createTempDirectory("seq")
        val seq = FrameSequence(dir, 12, 1)
        assertEquals(dir.resolve("frame-0000.orf"), seq.orf())
        assertEquals(dir.resolve("frame-0000.png"), seq.png())
        seq.record(5, 1, 1L)
        assertEquals(dir.resolve("frame-0001.orf"), seq.orf())
        assertEquals(dir.resolve("frame-0011.png"), seq.png(11))
    }

    @Test
    fun framesJsonListsFrameTickAndNanos() {
        val dir = Files.createTempDirectory("seq")
        val seq = FrameSequence(dir, 2, 3)
        seq.record(7, 40, 1_000_000_123L)
        seq.record(10, 41, 1_050_000_456L)
        seq.save()
        val root = Json.parseToJsonElement(Files.readString(dir.resolve("frames.json"))).jsonObject
        assertEquals(2, root["count"]!!.jsonPrimitive.int)
        assertEquals(3, root["every"]!!.jsonPrimitive.int)
        val frames = root["frames"]!!.jsonArray.map { it.jsonObject }
        assertEquals(listOf(7, 10), frames.map { it["frame"]!!.jsonPrimitive.int })
        assertEquals(listOf(40, 41), frames.map { it["tick"]!!.jsonPrimitive.int })
        assertEquals(1_050_000_456L, frames[1]["nanos"]!!.jsonPrimitive.long)
        assertEquals("frame-0001.orf", frames[1]["orf"]!!.jsonPrimitive.content)
        assertTrue(Files.notExists(dir.resolve("frames.json.part")))
    }

    @Test
    fun invalidArgumentsAreRejected() {
        val dir = Files.createTempDirectory("seq")
        for ((count, every) in listOf(0 to 1, FrameSequence.MAX_COUNT + 1 to 1, 1 to 0)) {
            try {
                FrameSequence(dir, count, every)
                fail("accepted $count $every")
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    @Test
    fun failedFrameLeavesNoPartialFiles() {
        val dir = Files.createTempDirectory("seq")
        val seq = FrameSequence(dir, 2, 1)
        seq.record(1, 1, 1L)
        val writer = OrfWriter(seq.orf(), 8, 8, FloatArray(4))
        writer.uniform(0, ByteArray(64))
        runCatching { writer.close(IllegalStateException("gl lost")) }
        val shortBuffer = FrameCapture.Pixels(ByteBuffer.allocateDirect(4), 8, 8)
        val error = runCatching { FrameCapture.write(shortBuffer, seq.png()) }.exceptionOrNull()
        assertNotNull(error)
        seq.discardUnrecorded()
        seq.save(error.toString())
        val names = Files.list(dir).use { s -> s.map { it.fileName.toString() }.sorted().toList() }
        assertEquals(listOf("frames.json"), names)
        val root = Json.parseToJsonElement(Files.readString(dir.resolve("frames.json"))).jsonObject
        assertEquals(1, root["count"]!!.jsonPrimitive.int)
        assertNotNull(root["error"])
    }

    @Test
    fun pngWriteIsAtomic() {
        val dir = Files.createTempDirectory("seq")
        val seq = FrameSequence(dir, 1, 1)
        FrameCapture.write(FrameCapture.Pixels(ByteBuffer.allocateDirect(4 * 2 * 3), 4, 2), seq.png())
        val names = Files.list(dir).use { s -> s.map { it.fileName.toString() }.toList() }
        assertEquals(listOf("frame-0000.png"), names)
    }
}
