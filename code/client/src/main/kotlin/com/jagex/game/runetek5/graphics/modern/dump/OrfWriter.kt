package com.jagex.game.runetek5.graphics.modern.dump

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import kotlin.concurrent.thread

class Rec(size: Int) {
    private val b = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
    fun u32(vararg v: Int) = apply { v.forEach(b::putInt) }
    fun f32(v: FloatArray) = apply { v.forEach(b::putFloat) }
    fun bytes(v: ByteArray) = apply { b.put(v) }
    fun array(): ByteArray = b.array()
}

class OrfWriter(val path: Path, width: Int, height: Int, clear: FloatArray) : AutoCloseable {
    private class Chunk(val kind: Int, val data: ByteArray)

    private val queue = ArrayBlockingQueue<Chunk>(64)
    private val end = Chunk(END, ByteArray(0))
    private val out: OutputStream
    private val thread: Thread
    @Volatile var failure: Throwable? = null
        private set

    init {
        path.toAbsolutePath().parent?.let(Files::createDirectories)
        out = BufferedOutputStream(Files.newOutputStream(path), 1 shl 16)
        out.write(MAGIC)
        out.write(Rec(28).u32(GL_DEPTH or DEFLATE, width, height).f32(clear).array())
        thread = thread(name = "orf-writer", isDaemon = true) { pump() }
    }

    private fun pump() {
        val deflater = Deflater(3)
        val packed = ByteArrayOutputStream()
        try {
            while (true) {
                val c = queue.take()
                if (c === end) break
                if (c.data.size < STORED_MAX) {
                    out.write(Rec(12).u32(c.kind, c.data.size, 0).array())
                    out.write(c.data)
                    continue
                }
                packed.reset()
                DeflaterOutputStream(packed, deflater, 1 shl 16).use { it.write(c.data) }
                deflater.reset()
                out.write(Rec(12).u32(c.kind, c.data.size, packed.size()).array())
                packed.writeTo(out)
            }
            out.write(Rec(12).u32(END, 0, 0).array())
            out.flush()
        } catch (e: Throwable) {
            failure = failure ?: e
        } finally {
            deflater.end()
        }
    }

    private fun put(kind: Int, data: ByteArray) {
        val c = Chunk(kind, data)
        while (!queue.offer(c, 100, TimeUnit.MILLISECONDS)) failure?.let { throw it }
        failure?.let { throw it }
    }

    fun mesh(id: Int, pass: Int, vertices: Int, indices: Int, vertexData: ByteArray, indexData: ByteArray) =
        put(MESH, Rec(16 + vertexData.size + indexData.size).u32(id, pass, vertices, indices).bytes(vertexData).bytes(indexData).array())

    fun texture(id: Int, width: Int, height: Int, layers: Int, rgba: ByteArray) =
        put(TEXTURE, Rec(16 + rgba.size).u32(id, width, height, layers).bytes(rgba).array())

    fun uniform(id: Int, block: ByteArray) = put(UNIFORM, Rec(8 + block.size).u32(id, block.size).bytes(block).array())

    fun instances(id: Int, count: Int, data: ByteArray) = put(INSTANCES, Rec(12 + data.size).u32(id, count, data.size).bytes(data).array())

    fun draw(d: Recorder.DrawRec) {
        val name = d.name.toByteArray()
        put(DRAW, Rec(64 + d.textures.size * 4 + name.size).u32(d.pass, d.mesh, d.uniform, d.textures.size).u32(*d.textures).u32(d.state).u32(*d.viewport).u32(*d.scissor).u32(d.inst, name.size).bytes(name).array())
    }

    override fun close() = close(null)

    fun close(error: Throwable?) {
        if (error != null) failure = failure ?: error
        if (failure == null) queue.put(end) else thread.interrupt()
        thread.join()
        out.close()
        failure?.let {
            Files.deleteIfExists(path)
            throw it
        }
    }

    companion object {
        val MAGIC = "ORF3".toByteArray()
        const val GL_DEPTH = 1
        const val DEFLATE = 2
        const val STORED_MAX = 128
        const val MESH = 0
        const val TEXTURE = 1
        const val UNIFORM = 2
        const val INSTANCES = 3
        const val DRAW = 4
        const val END = 5
    }
}
