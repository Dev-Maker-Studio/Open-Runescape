package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.Inflater
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

object ColorClearFixture {
    fun write(path: Path) {
        OrfWriter(path, 12, 10, floatArrayOf(1f, 0f, 1f, 1f)).use { writer ->
            val rec = Recorder(writer, 12, 10)
            fun sprite(rect: IntArray, color: FloatArray) {
                val vertices = ColorClear.vertices(rect, color)
                val buffer = ByteBuffer.wrap(vertices).order(ByteOrder.LITTLE_ENDIAN)
                repeat(4) { buffer.putFloat(it * 40 + 20, -1f) }
                rec.draw(Recorder.DrawRec(Passes.sprite.id, rec.mesh(Passes.sprite, vertices, Rec(24).u32(0, 1, 2, 0, 2, 3).array()), rec.uniform(Passes.sprite.pack(mapOf("uProjection" to ColorClear.projection(12, 10)))), intArrayOf(Recorder.NONE), 7 shl 7, intArrayOf(0, 0, 12, 10), IntArray(4), Recorder.NONE, "sprite"))
            }
            val mask = BooleanArray(4) { true }
            rec.colorClear(floatArrayOf(0f, 0f, 1f / 255, 0f), null, mask)
            sprite(intArrayOf(4, 3, 4, 6), floatArrayOf(41f / 255, 38f / 255, 33f / 255, 1f))
            rec.colorClear(floatArrayOf(141f / 255, 164f / 255, 194f / 255, 0.5f), intArrayOf(2, 4, 8, 6), mask)
            sprite(intArrayOf(7, 2, 2, 2), floatArrayOf(200f / 255, 30f / 255, 50f / 255, 1f))
        }
    }

    @JvmStatic
    fun main(args: Array<String>) = write(Path.of(args.single()))
}

class ColorClearTest {
    @Test
    fun clippingUsesFramebufferAndBottomLeftScissorNotViewport() {
        assertArrayEquals(intArrayOf(0, 0, 12, 10), ColorClear.rectangle(12, 10, null))
        assertArrayEquals(intArrayOf(2, 0, 8, 6), ColorClear.rectangle(12, 10, intArrayOf(2, 4, 8, 6)))
        assertArrayEquals(intArrayOf(0, 7, 3, 3), ColorClear.rectangle(12, 10, intArrayOf(-2, -3, 5, 6)))
        assertArrayEquals(intArrayOf(11, 0, 1, 1), ColorClear.rectangle(12, 10, intArrayOf(11, 9, Int.MAX_VALUE, Int.MAX_VALUE)))
        assertNull(ColorClear.rectangle(12, 10, intArrayOf(12, 0, 4, 4)))
        assertNull(ColorClear.rectangle(12, 10, intArrayOf(0, 0, 0, 10)))
    }

    @Test
    fun masksEitherWriteAllWriteNoneOrReject() {
        assertTrue(ColorClear.enabled(BooleanArray(4) { true }))
        assertFalse(ColorClear.enabled(BooleanArray(4)))
        assertTrue(runCatching { ColorClear.enabled(booleanArrayOf(true, true, true, false)) }.isFailure)
    }

    @Test
    fun verticesHaveExactRgbaTopLeftClipAndNoTexture() {
        val vertices = ByteBuffer.wrap(ColorClear.vertices(intArrayOf(2, 0, 8, 6), floatArrayOf(-1f, 0.5f, 2f, 0f))).order(ByteOrder.LITTLE_ENDIAN)
        val positions = arrayOf(floatArrayOf(2f, 0f), floatArrayOf(10f, 0f), floatArrayOf(10f, 6f), floatArrayOf(2f, 6f))
        repeat(4) { i ->
            assertEquals(positions[i][0], vertices.getFloat(i * 40), 0f)
            assertEquals(positions[i][1], vertices.getFloat(i * 40 + 4), 0f)
            assertArrayEquals(byteArrayOf(0, -128, -1, 0), vertices.array().copyOfRange(i * 40 + 16, i * 40 + 20))
            assertEquals(-2f, vertices.getFloat(i * 40 + 20), 0f)
            assertArrayEquals(floatArrayOf(2f, 0f, 10f, 6f), FloatArray(4) { vertices.getFloat(i * 40 + 24 + it * 4) }, 0f)
        }
        val projection = ColorClear.projection(12, 10)
        fun clip(x: Float, y: Float) = floatArrayOf(projection[0] * x + projection[12], projection[5] * y + projection[13], projection[14])
        assertArrayEquals(floatArrayOf(-1f, 1f, 0f), clip(0f, 0f), 0f)
        assertArrayEquals(floatArrayOf(1f, -1f, 0f), clip(12f, 10f), 0f)
        assertEquals(0.5f, (clip(0f, 0f)[2] + 1f) * 0.5f, 0f)
    }

    @Test
    fun serializedDrawOrderAndClearStateArePreserved() {
        val path = Files.createTempFile("color-clears", ".orf")
        try {
            ColorClearFixture.write(path)
            val bytes = ByteBuffer.wrap(Files.readAllBytes(path)).order(ByteOrder.LITTLE_ENDIAN)
            bytes.position(32)
            val names = ArrayList<String>()
            while (true) {
                val kind = bytes.int
                val raw = bytes.int
                val packed = bytes.int
                if (kind == OrfWriter.END) break
                val payload = ByteArray(if (packed == 0) raw else packed).also(bytes::get)
                val record = if (packed == 0) payload else ByteArray(raw).also { out ->
                    val inflater = Inflater()
                    inflater.setInput(payload)
                    assertEquals(raw, inflater.inflate(out))
                    assertTrue(inflater.finished())
                    inflater.end()
                }
                if (kind != OrfWriter.DRAW) continue
                val draw = ByteBuffer.wrap(record).order(ByteOrder.LITTLE_ENDIAN)
                val nameOffset = 56 + draw.getInt(12) * 4
                val nameLength = draw.getInt(nameOffset)
                val name = String(record, nameOffset + 4, nameLength, Charsets.UTF_8)
                names.add(name)
                if (name.startsWith("clear.color.")) {
                    assertEquals(7 shl 7, draw.getInt(20))
                    assertArrayEquals(intArrayOf(0, 0, 12, 10), IntArray(4) { draw.getInt(24 + it * 4) })
                }
            }
            assertEquals(listOf("clear.color.full", "sprite", "clear.color.scissored", "sprite"), names)
        } finally {
            Files.deleteIfExists(path)
        }
    }
}
