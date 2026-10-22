package com.darkan.bot

import com.jagex.game.runetek5.graphics.modern.dump.FrameDump
import com.jagex.game.runetek5.graphics.modern.dump.EncodedRecordCache
import kotlinx.serialization.json.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

internal data class StreamCapture(val width: Int, val height: Int, val tick: Int, val inputSeq: Long, val nanos: Long, val summary: String, val profile: Map<String, Long> = emptyMap())

internal class StreamPacer(rate: Int, private var deadline: Long) {
    private val period = 1_000_000_000L / rate
    var drops = 0L
        private set

    fun delay(now: Long): Long {
        deadline += period
        if (now > deadline) {
            drops += (now - deadline) / period + 1
            deadline = now
        }
        return deadline - now
    }
}

internal class StreamProducer(private val onStart: () -> Unit = {}, private val onStop: () -> Unit = {}, private val capture: (Path) -> StreamCapture) {
    private val serial = AtomicLong()
    @Volatile private var worker: Thread? = null
    @Volatile private var running = false
    @Volatile private var directory: Path? = null
    @Volatile private var fps = 5
    @Volatile private var frames = 0L
    @Volatile private var drops = 0L
    @Volatile private var profile: Map<String, Long> = emptyMap()
    @Volatile private var error: String? = null

    @Synchronized
    fun start(dir: Path, rate: Int = 5): JsonObject {
        require(rate in 1..30) { "fps must be from 1 to 30" }
        check(worker?.isAlive != true) { "stream already running or still stopping" }
        val root = dir.toAbsolutePath().normalize()
        Files.createDirectories(root)
        require(Files.isDirectory(root)) { "stream directory is not a directory" }
        clean(root)
        try {
            onStart()
        } catch (e: Throwable) {
            runCatching { onStop() }
            throw e
        }
        directory = root
        fps = rate
        frames = 0
        drops = 0
        error = null
        profile = emptyMap()
        running = true
        worker = thread(start = false, isDaemon = true, name = "native-stream") { pump(root, rate) }
        worker!!.start()
        return state()
    }

    @Synchronized
    fun stop(): JsonObject {
        running = false
        worker?.interrupt()
        worker?.join(5000)
        if (worker?.isAlive == true) {
            error = "stream capture still stopping; retry stream-state before restarting"
            throw IllegalStateException(error)
        }
        return state()
    }

    fun state(): JsonObject = buildJsonObject {
        put("running", running)
        put("stopping", !running && worker?.isAlive == true)
        put("directory", directory?.toString()?.let(::JsonPrimitive) ?: JsonNull)
        put("fps", fps)
        put("frames", frames)
        put("frame", serial.get())
        put("drops", drops)
        put("error", error?.let(::JsonPrimitive) ?: JsonNull)
        put("profile", buildJsonObject { profile.forEach { (key, value) -> put(key, value) } })
    }

    private fun clean(root: Path) {
        listOf("latest.json", "latest.json.tmp", "frame-0.orf", "frame-1.orf", "frame-0.orf.tmp", "frame-1.orf.tmp").forEach { Files.deleteIfExists(root.resolve(it)) }
    }

    private fun pump(root: Path, rate: Int) {
        val pacer = StreamPacer(rate, System.nanoTime())
        try {
            while (running) {
                val next = serial.get() + 1
                check(next <= 0xffffffffL) { "stream frame sequence exhausted" }
                val slot = (next and 1).toInt()
                val name = "frame-$slot.orf"
                val temp = root.resolve("$name.tmp")
                val captureStart = System.nanoTime()
                val result = capture(temp)
                val captureNanos = System.nanoTime() - captureStart
                if (!running) break
                require(result.width > 0 && result.height > 0) { "invalid capture dimensions" }
                val checksumStart = System.nanoTime()
                val digest = java.security.MessageDigest.getInstance("SHA-256")
                Files.newInputStream(temp).use { source ->
                    val buffer = ByteArray(65536)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                }
                val sha256 = java.util.HexFormat.of().formatHex(digest.digest())
                val checksumNanos = System.nanoTime() - checksumStart
                val publicationStart = System.nanoTime()
                Files.move(temp, root.resolve(name), ATOMIC_MOVE, REPLACE_EXISTING)
                val measured = result.profile + mapOf("captureRequestNanos" to captureNanos, "checksumNanos" to checksumNanos)
                val metadata = buildJsonObject {
                    put("frame", next)
                    put("slot", slot)
                    put("file", name)
                    put("width", result.width)
                    put("height", result.height)
                    put("tick", result.tick)
                    put("inputSeq", result.inputSeq)
                    put("nanos", result.nanos)
                    put("summary", result.summary)
                    put("sha256", sha256)
                    put("profile", buildJsonObject { measured.forEach { (key, value) -> put(key, value) } })
                }
                Files.writeString(root.resolve("latest.json.tmp"), metadata.toString())
                Files.move(root.resolve("latest.json.tmp"), root.resolve("latest.json"), ATOMIC_MOVE, REPLACE_EXISTING)
                profile = measured + ("publicationNanos" to (System.nanoTime() - publicationStart))
                serial.set(next)
                frames++
                val remaining = pacer.delay(System.nanoTime())
                drops = pacer.drops
                if (remaining > 0) java.util.concurrent.TimeUnit.NANOSECONDS.sleep(remaining)
            }
        } catch (e: InterruptedException) {
            if (running) error = "capture interrupted"
        } catch (e: Throwable) {
            error = e.message ?: e.javaClass.simpleName
        } finally {
            running = false
            runCatching { onStop() }.exceptionOrNull()?.let { if (error == null) error = it.message ?: it.javaClass.simpleName }
            listOf("latest.json.tmp", "frame-0.orf.tmp", "frame-1.orf.tmp").forEach { runCatching { Files.deleteIfExists(root.resolve(it)) } }
        }
    }
}

object NativeStream {
    private var encodedCache: EncodedRecordCache? = null
    private val producer = StreamProducer(onStart = {
        NativeInput.reset()
        encodedCache = EncodedRecordCache()
    }, onStop = {
        encodedCache?.close()
        encodedCache = null
    }) { path ->
        var tick = 0
        var seq = 0L
        var nanos = 0L
        var profile = emptyMap<String, Long>()
        val summary = FrameDump.streamRequest(path, checkNotNull(encodedCache), { profile = it }) {
            tick = Footprints.gameTick
            seq = NativeInput.appliedSequence
            nanos = System.nanoTime()
        }
        val header = ByteArray(16)
        Files.newInputStream(path).use { require(it.readNBytes(header, 0, header.size) == header.size) { "short ORF header" } }
        require(header[0] == 79.toByte() && header[1] == 82.toByte() && header[2] == 70.toByte() && header[3] in byteArrayOf(51, 52)) { "capture is not ORF3 or ORF4" }
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        StreamCapture(buffer.getInt(8), buffer.getInt(12), tick, seq, nanos, summary, profile)
    }

    fun start(args: List<String>): JsonObject {
        require(args.size in 1..2) { "usage: stream-start <directory> [fps]" }
        val rate = args.getOrNull(1)?.let { it.toIntOrNull() ?: error("fps must be an integer") } ?: 5
        return producer.start(Path.of(args[0]), rate)
    }

    fun stop() = producer.stop().also { NativeInput.release() }
    fun state() = producer.state()
}
