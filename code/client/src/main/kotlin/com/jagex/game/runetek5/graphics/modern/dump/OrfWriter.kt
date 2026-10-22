package com.jagex.game.runetek5.graphics.modern.dump

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.lang.management.ManagementFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.zip.Deflater
import kotlin.concurrent.thread

class Rec(size: Int) {
    private val b = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
    fun u32(vararg v: Int) = apply { v.forEach(b::putInt) }
    fun f32(v: FloatArray) = apply { v.forEach(b::putFloat) }
    fun bytes(v: ByteArray) = apply { b.put(v) }
    fun array(): ByteArray = b.array()
}

class OrfWriter @JvmOverloads constructor(val path: Path, width: Int, height: Int, clear: FloatArray, private val cache: EncodedRecordCache? = null) : AutoCloseable {
    private class Chunk(val kind: Int, val data: ByteArray, val key: EncodedRecordCache.Key? = null) {
        val permits = minOf(QUEUE_KIB, (data.size.toLong() + 1023L).div(1024).toInt())
    }

    private var typedTextures = false
    @Volatile private var bloomRecorded = false
    val profile = CaptureProfile()
    private val budget = Semaphore(QUEUE_KIB)
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
        val deflater = Deflater(Deflater.BEST_SPEED)
        val packed = ByteArrayOutputStream()
        val buffer = ByteArray(1 shl 16)
        val started = System.nanoTime()
        try {
            while (true) {
                val c = queue.take()
                if (c === end) break
                try {
                    val cached = c.key?.let { cache?.get(it) }
                    if (cached != null) {
                        profile.add("encodedCacheHits", 1)
                        profile.add("encodedCacheHitRawBytes", c.data.size.toLong())
                        cached.writeTo(out)
                        continue
                    }
                    if (c.key != null) profile.add("encodedCacheMisses", 1)
                    var packedLength = 0
                    if (c.data.size >= STORED_MAX) {
                        val cpuStart = if (THREAD_TIMES.isCurrentThreadCpuTimeSupported && THREAD_TIMES.isThreadCpuTimeEnabled) THREAD_TIMES.currentThreadCpuTime else -1L
                        profile.measure("compress") {
                            packed.reset()
                            deflater.setInput(c.data)
                            deflater.finish()
                            while (!deflater.finished()) {
                                val size = deflater.deflate(buffer)
                                packed.write(buffer, 0, size)
                            }
                            deflater.reset()
                        }
                        if (cpuStart >= 0) {
                            val cpuEnd = THREAD_TIMES.currentThreadCpuTime
                            if (cpuEnd >= cpuStart) profile.add("compressCpuNanos", cpuEnd - cpuStart)
                        }
                        if (packed.size() < c.data.size) packedLength = packed.size()
                    }
                    if (c.key != null && cache?.accepts(c.key, 12L + if (packedLength == 0) c.data.size else packedLength) == true) {
                        val record = EncodedRecordCache.EncodedRecord(c.kind, c.data.size, if (packedLength == 0) c.data else packed.toByteArray(), packedLength)
                        cache.put(c.key, record)
                        record.writeTo(out)
                    } else {
                        out.write(Rec(12).u32(c.kind, c.data.size, packedLength).array())
                        if (packedLength == 0) out.write(c.data) else packed.writeTo(out)
                    }
                } finally {
                    budget.release(c.permits)
                }
            }
            out.write(Rec(12).u32(END, 0, 0).array())
            out.flush()
        } catch (e: Throwable) {
            failure = failure ?: e
            cache?.close()
        } finally {
            profile.add("writerNanos", System.nanoTime() - started)
            deflater.end()
        }
    }

    private fun put(kind: Int, data: ByteArray, fingerprint: String? = null, headerLength: Int = 0) {
        profile.add("rawBytes", data.size.toLong())
        profile.measure("writerQueue") {
            val key = if (cache != null && fingerprint != null) {
                require(headerLength in 1..data.size)
                EncodedRecordCache.Key(kind, fingerprint, String(data, 0, headerLength, Charsets.ISO_8859_1), data.size)
            } else null
            val c = Chunk(kind, data, key)
            while (!budget.tryAcquire(c.permits, 100, TimeUnit.MILLISECONDS)) failure?.let { throw it }
            var queued = false
            try {
                failure?.let { throw it }
                while (!queue.offer(c, 100, TimeUnit.MILLISECONDS)) failure?.let { throw it }
                queued = true
                failure?.let { throw it }
            } finally {
                if (!queued) budget.release(c.permits)
            }
        }
    }

    @JvmOverloads
    fun mesh(id: Int, pass: Int, vertices: Int, indices: Int, vertexData: ByteArray, indexData: ByteArray, fingerprint: String? = null) =
        put(MESH, Rec(16 + vertexData.size + indexData.size).u32(id, pass, vertices, indices).bytes(vertexData).bytes(indexData).array(), fingerprint, 16)

    @JvmOverloads
    fun texture(id: Int, width: Int, height: Int, layers: Int, rgba: ByteArray, format: RecordedTextureFormat = RecordedTextureFormat.RGBA8, sampler: Int = 9, fingerprint: String? = null) {
        require(rgba.size == TexturePacking.byteSize(width, height, layers, format))
        if (format == RecordedTextureFormat.RGBA8) {
            put(TEXTURE, Rec(16 + rgba.size).u32(id, width, height, layers).bytes(rgba).array(), fingerprint, 16)
        } else {
            require(layers == 1) { "typed cube/volume recording unsupported" }
            require((sampler and 255) == sampler && (sampler and 7) <= 5 && ((sampler shr 4) and 3) <= 2 && ((sampler shr 6) and 3) <= 2) { "typed sampler bits $sampler" }
            typedTextures = true
            put(TYPED_TEXTURE, Rec(28 + rgba.size).u32(id, width, height, layers, format.id, rgba.size, sampler).bytes(rgba).array(), fingerprint, 28)
        }
    }

    fun uniform(id: Int, block: ByteArray) = put(UNIFORM, Rec(8 + block.size).u32(id, block.size).bytes(block).array())

    fun instances(id: Int, count: Int, data: ByteArray) = put(INSTANCES, Rec(12 + data.size).u32(id, count, data.size).bytes(data).array())

    fun bloom(whitePoint: Float, bloom: Float, brightpass: Float): Boolean {
        if (bloomRecorded || !whitePoint.isFinite() || !brightpass.isFinite() || !bloom.isFinite() || bloom <= 0f) return false
        bloomRecorded = true
        put(BLOOM, Rec(12).f32(floatArrayOf(whitePoint, bloom, brightpass)).array())
        return true
    }

    fun draw(d: Recorder.DrawRec) {
        val name = d.name.toByteArray()
        put(DRAW, Rec(60 + d.textures.size * 4 + name.size).u32(d.pass, d.mesh, d.uniform, d.textures.size).u32(*d.textures).u32(d.state).u32(*d.viewport).u32(*d.scissor).u32(d.inst, name.size).bytes(name).array())
    }

    override fun close() = close(null)

    fun close(error: Throwable?) {
        if (error != null) failure = failure ?: error
        try {
            if (failure == null) {
                while (!queue.offer(end, 100, TimeUnit.MILLISECONDS)) if (failure != null) break
            } else thread.interrupt()
            thread.join()
            out.close()
            failure?.let { throw it }
            if (bloomRecorded || typedTextures) {
                Files.newByteChannel(path, StandardOpenOption.WRITE).use { channel ->
                    val magic = ByteBuffer.wrap((if (bloomRecorded) "ORF5" else "ORF4").toByteArray())
                    while (magic.hasRemaining()) channel.write(magic)
                }
            }
        } catch (e: Throwable) {
            failure = failure ?: e
            cache?.close()
            thread.interrupt()
            thread.join()
            runCatching { out.close() }
            Files.deleteIfExists(path)
            throw failure!!
        } finally {
            cache?.stats()?.let {
                profile.add("encodedCacheBytes", it.bytes)
                profile.add("encodedCacheEntries", it.entries.toLong())
                profile.add("encodedCacheEvictions", it.evictions)
            }
        }
    }

    companion object {
        private val THREAD_TIMES = ManagementFactory.getThreadMXBean()
        val MAGIC = "ORF3".toByteArray()
        const val GL_DEPTH = 1
        const val DEFLATE = 2
        const val STORED_MAX = 128
        private const val QUEUE_KIB = 64 * 1024
        const val MESH = 0
        const val TEXTURE = 1
        const val UNIFORM = 2
        const val INSTANCES = 3
        const val DRAW = 4
        const val END = 5
        const val TYPED_TEXTURE = 6
        const val BLOOM = 7
    }
}
