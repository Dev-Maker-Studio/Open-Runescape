package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.zip.Inflater
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrfWriterTest {
    private fun pattern(size: Int, seed: Int) = ByteArray(size) { ((it * 31 + seed) and 0xff).toByte() }

    private fun rect(x: Int, y: Int, w: Int, h: Int) = intArrayOf(x, y, w, h)

    @Test
    fun syntheticFrameStreamsDedupedRecords() {
        val dir = Files.createTempDirectory("orf")
        val path = dir.resolve("synthetic.orf")
        val writer = OrfWriter(path, 320, 240, floatArrayOf(0.1f, 0.2f, 0.3f, 1f))
        val rec = Recorder(writer, 320, 240)
        val modelVertices = pattern(3 * Passes.model.stride, 1)
        val triangle = Rec(12).u32(0, 1, 2).array()
        val spriteVertices = pattern(4 * Passes.sprite.stride, 2)
        val quad = Rec(24).u32(0, 1, 2, 0, 2, 3).array()
        val model = rec.mesh(Passes.model, modelVertices, triangle)
        assertEquals(model, rec.mesh(Passes.model, modelVertices, triangle))
        val sprite = rec.mesh(Passes.sprite, spriteVertices, quad)
        val plain = rec.texture(2, 2, 1, pattern(16, 3))
        assertEquals(plain, rec.texture(2, 2, 1, pattern(16, 3)))
        val atlas = rec.texture(4, 4, 2, pattern(128, 4))
        val modelBlock = Passes.model.pack(mapOf("uModelViewProjection" to FloatArray(16) { if (it % 5 == 0) 1f else 0f }, "uTextureEnabled" to floatArrayOf(1f), "uFogColor" to floatArrayOf(0.5f, 0.6f, 0.7f)))
        val block = rec.uniform(modelBlock)
        assertEquals(block, rec.uniform(modelBlock))
        val projection = rec.uniform(Passes.sprite.pack(mapOf("uProjection" to FloatArray(16) { 2f })))
        val inst = rec.instances(2, pattern(160, 5))
        val state = 3 or (1 shl 7)
        val vp = rect(0, 0, 320, 240)
        val none = IntArray(4)
        rec.draw(Recorder.DrawRec(Passes.model.id, model, block, intArrayOf(plain), state, vp, none, Recorder.NONE, "model"))
        rec.draw(Recorder.DrawRec(Passes.model.id, model, block, intArrayOf(plain), state, vp, none, Recorder.NONE, "model"))
        rec.draw(Recorder.DrawRec(Passes.instanced.id, model, block, intArrayOf(Recorder.NONE), state, vp, none, inst, "instanced"))
        rec.skip("groundTextured.water", state, vp, none)
        rec.draw(Recorder.DrawRec(Passes.sprite.id, sprite, projection, intArrayOf(atlas), 4 or (7 shl 7), vp, rect(8, 8, 100, 50), Recorder.NONE, "sprite"))
        writer.close()
        assertEquals(mapOf("model" to 2, "instanced" to 1, "sprite" to 1), rec.drawn)
        assertEquals(mapOf("groundTextured.water" to 1), rec.skipped)
        assertEquals("drawn instanced=1,model=2,sprite=1 skipped groundTextured.water=1 meshes 2 textures 2 uniforms 2 instances 1", rec.summary())

        val bytes = Files.readAllBytes(path)
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertArrayEquals(OrfWriter.MAGIC, bytes.copyOf(4))
        b.position(4)
        assertEquals(OrfWriter.GL_DEPTH or OrfWriter.DEFLATE, b.int)
        assertEquals(320, b.int)
        assertEquals(240, b.int)
        assertEquals(0.1f, b.float, 0f)
        b.position(32)
        val kinds = ArrayList<Int>()
        var compressed = 0
        while (true) {
            val kind = b.int
            val raw = b.int
            val packed = b.int
            if (kind == OrfWriter.END) break
            kinds += kind
            val payload = ByteArray(if (packed == 0) raw else packed).also(b::get)
            val record = if (packed == 0) payload else ByteArray(raw).also { out ->
                compressed++
                val inflater = Inflater()
                inflater.setInput(payload)
                assertEquals(raw, inflater.inflate(out))
                assertTrue(inflater.finished())
                inflater.end()
            }
            val head = ByteBuffer.wrap(record).order(ByteOrder.LITTLE_ENDIAN)
            when (kind) {
                OrfWriter.MESH -> if (head.getInt(0) == model) assertArrayEquals(modelVertices, record.copyOfRange(16, 16 + modelVertices.size))
                OrfWriter.TEXTURE -> if (head.getInt(0) == atlas) assertArrayEquals(pattern(128, 4), record.copyOfRange(16, record.size))
                OrfWriter.UNIFORM -> if (head.getInt(0) == block) assertArrayEquals(modelBlock, record.copyOfRange(8, record.size))
            }
        }
        assertEquals(0, b.remaining())
        assertEquals(listOf(0, 0, 1, 1, 2, 2, 3, 4, 4, 4, 4, 4), kinds)
        assertEquals(5, compressed)
        println("[OrfWriterTest] ${path.toAbsolutePath()} ${bytes.size} bytes")
    }

    @Test
    fun failedWriterLeavesNoFile() {
        val dir = Files.createTempDirectory("orf")
        val path = dir.resolve("broken.orf")
        val writer = OrfWriter(path, 8, 8, FloatArray(4))
        writer.uniform(0, ByteArray(64))
        val error = runCatching { writer.close(IllegalStateException("gl lost")) }.exceptionOrNull()
        assertEquals("gl lost", error?.message)
        assertTrue(Files.notExists(path))
    }
}
