package com.jagex.game.runetek5.graphics.modern.dump

import com.darkan.bot.FrameCapture
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.network.PingRequester
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.lwjgl.opengl.GL33.*
import org.lwjgl.opengl.GL42.glDrawElementsInstancedBaseInstance
import org.lwjgl.opengl.GL43.glMultiDrawElementsIndirect

object FrameDump {
    private class Request(val path: Path, val quiet: Boolean = false, val onBegin: () -> Unit = {}, val background: Boolean = false, val onProfile: (Map<String, Long>) -> Unit = {}, val cache: EncodedRecordCache? = null, val done: CompletableFuture<String> = CompletableFuture()) {
        @Volatile var cancelled = false
    }

    private class Run(val seq: FrameSequence, val tick: () -> Int, val done: CompletableFuture<String> = CompletableFuture()) {
        @Volatile var cancelled = false
    }

    private val completion = Executors.newSingleThreadExecutor { task -> Thread(task, "orf-completion").apply { isDaemon = true } }
    @Volatile private var completing = false
    private val requests = ConcurrentLinkedQueue<Request>()
    private val runs = ConcurrentLinkedQueue<Run>()
    private var run: Run? = null
    private val names = HashMap<Int, String>()
    private val handles = HashMap<Long, Int>()
    private val target = System.getProperty("render.dump")?.toIntOrNull() ?: -1
    private var frame = 0
    private var recorder: GlRecorder? = null
    private var surface: CaptureSurface? = null
    private var current: List<Request> = emptyList()

    @JvmField @Volatile var active = false

    @JvmOverloads
    fun request(path: Path, quiet: Boolean = false, onBegin: () -> Unit = {}): String {
        return await(Request(path, quiet, onBegin))
    }

    fun streamRequest(path: Path, cache: EncodedRecordCache, onProfile: (Map<String, Long>) -> Unit, onBegin: () -> Unit): String =
        await(Request(path, true, onBegin, true, onProfile, cache))

    private fun await(job: Request): String {
        requests.add(job)
        return try {
            job.done.get(120, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            job.cancelled = true
            if (!requests.remove(job)) runCatching { job.done.join() }
            throw e
        } catch (e: TimeoutException) {
            job.cancelled = true
            if (!requests.remove(job)) runCatching { job.done.join() }
            throw e
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    fun sequence(dir: Path, count: Int, every: Int, tick: () -> Int): String {
        val job = Run(FrameSequence(dir, count, every), tick)
        runs.add(job)
        return try {
            job.done.get(900, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            job.cancelled = true
            runs.remove(job)
            throw e
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    @JvmStatic
    fun name(program: Int, name: String) {
        names[program] = name
    }

    @JvmStatic
    fun source(program: Int, vertex: String, fragment: String) {
        names.getOrPut(program) { ShaderNames.of(vertex, fragment) }
    }

    @JvmStatic
    fun handle(handle: Long, texture: Int) {
        handles[handle] = texture
    }

    fun textureOf(handle: Long) = handles[handle] ?: 0

    @JvmStatic
    fun shader(program: Int) = names[program] ?: "program$program"

    @JvmStatic
    fun frameEnd() {
        if (run?.cancelled == true) {
            val error = TimeoutException("dump-frames timed out")
            if (active) abort(error) else closeRun(error)
        }
        if (active) finish()
        frame++
        if (completing) return
        if (run == null) run = runs.poll()
        val job = run
        if (job != null) {
            if (job.seq.due()) begin(listOf(Request(job.seq.orf())))
            return
        }
        val batch = generateSequence { requests.poll() }.filter { if (it.cancelled) { it.done.cancel(false); false } else true }.toList()
        if (batch.isEmpty() && frame + 1 != target) return
        begin(batch.ifEmpty { listOf(Request(Path.of(System.getProperty("render.dump.path") ?: "frame-$target.orf"))) })
    }

    private fun begin(batch: List<Request>) {
        current = batch
        try {
            val drawable = CaptureSurface(GameShell.gameCanvas, GLWrapper.getInteger(GL_DRAW_FRAMEBUFFER_BINDING))
            surface = drawable
            val clear = FloatArray(4).also { glGetFloatv(GL_COLOR_CLEAR_VALUE, it) }
            val cache = batch.first().cache?.takeIf { candidate -> batch.all { it.background && it.cache === candidate } }
            recorder = GlRecorder(OrfWriter(current[0].path, drawable.width, drawable.height, clear, cache), drawable.width, drawable.height)
            CaptureProfiler.begin(recorder!!.writer.profile)
            current.forEach { it.onBegin() }
            active = true
            if (current.any { !it.quiet }) println("[FrameDump] recording frame ${frame + 1} to ${current[0].path}")
        } catch (e: Throwable) {
            fail(e)
        }
    }

    private fun finish() {
        try {
            surface!!.validate(GameShell.gameCanvas)
            recorder!!.captureHistory()
            surface!!.validate(GameShell.gameCanvas)
        } catch (e: Throwable) {
            abort(e)
            return
        }
        active = false
        val rec = recorder!!
        recorder = null
        surface = null
        CaptureProfiler.end()
        val batch = current
        current = emptyList()
        if (run == null && batch.all { it.background }) {
            completing = true
            completion.execute {
                try {
                    complete(rec, batch)
                } catch (e: Throwable) {
                    batch.forEach { it.done.completeExceptionally(e) }
                } finally {
                    completing = false
                }
            }
        } else {
            try {
                complete(rec, batch)
                run?.let { sequenceFrame(it) }
            } catch (e: Throwable) {
                batch.forEach { it.done.completeExceptionally(e) }
                fail(e)
            }
        }
    }

    private fun complete(rec: GlRecorder, batch: List<Request>) {
        rec.writer.profile.measure("writerClose") { rec.writer.close() }
        val path = rec.writer.path
        batch.drop(1).forEach { Files.copy(path, it.path, StandardCopyOption.REPLACE_EXISTING) }
        val summary = "${path.toAbsolutePath()} ${Files.size(path)} bytes ${rec.summary()}"
        val profile = rec.writer.profile.snapshot()
        if (batch.any { !it.quiet }) println("[FrameDump] $summary")
        batch.forEach { it.onProfile(profile); it.done.complete(summary) }
    }

    private fun sequenceFrame(job: Run) {
        val seq = job.seq
        val nanos = System.nanoTime()
        val tick = job.tick()
        val state = Client.gameState.toString()
        FrameCapture.write(FrameCapture.read(), seq.png())
        seq.record(frame + 1, tick, nanos, state)
        PingRequester.keepalive(true)
        if (seq.done) closeRun(null)
    }

    private fun closeRun(error: Throwable?) {
        val job = run ?: return
        run = null
        val cleanup = if (error != null) runCatching { job.seq.discardUnrecorded() }.exceptionOrNull() else null
        val failure = error ?: cleanup
        val saved = runCatching { job.seq.save(failure?.toString()) }.exceptionOrNull()
        val problem = failure ?: saved
        if (problem == null) {
            val summary = "${job.seq.dir.toAbsolutePath()} ${job.seq.entries.size} frames"
            println("[FrameDump] sequence $summary")
            job.done.complete(summary)
        } else {
            println("[FrameDump] sequence failed: $problem")
            job.done.completeExceptionally(problem)
        }
    }

    private fun fail(e: Throwable) {
        surface = null
        CaptureProfiler.end()
        println("[FrameDump] failed: $e")
        e.printStackTrace()
        current.forEach { it.done.completeExceptionally(e) }
        current = emptyList()
        closeRun(e)
    }

    private fun abort(e: Throwable) {
        active = false
        val rec = recorder ?: return
        recorder = null
        runCatching { rec.writer.close(e) }
        fail(e)
    }

    @JvmStatic
    fun draw(mode: Int, count: Int, type: Int, offset: Long, instances: Int, baseInstance: Int, baseVertex: Int = 0, drawId: Int = 0) {
        if (!active) return
        try {
            recorder!!.draw(mode, count, type, offset, instances, baseInstance, baseVertex, drawId)
        } catch (e: Throwable) {
            abort(e)
        }
    }

    @JvmStatic
    fun bloom(whitePoint: Float, bloom: Float, brightpass: Float) {
        if (!active) return
        try {
            recorder!!.bloom(whitePoint, bloom, brightpass)
        } catch (e: Throwable) {
            abort(e)
        }
    }

    @JvmStatic
    fun clear(mask: Int) {
        if (!active) return
        try {
            recorder!!.clear(mask)
        } catch (e: Throwable) {
            abort(e)
        }
    }

    @JvmStatic
    fun nativeDraw() {
        if (!active) return
        try {
            recorder!!.nativeDraw()
        } catch (e: Throwable) {
            abort(e)
        }
    }

    @JvmStatic
    fun blit(srcX0: Int, srcY0: Int, srcX1: Int, srcY1: Int, dstX0: Int, dstY0: Int, dstX1: Int, dstY1: Int, mask: Int, filter: Int) {
        if (active) {
            try {
                recorder!!.nativeBlit(intArrayOf(srcX0, srcY0, srcX1, srcY1), intArrayOf(dstX0, dstY0, dstX1, dstY1), mask, filter)
            } catch (e: Throwable) {
                abort(e)
            }
        }
        glBlitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter)
    }

    fun aliasTarget(source: Int, target: Int, width: Int, height: Int) {
        if (active) recorder!!.aliasTarget(source, target, width, height)
    }

    @JvmStatic
    fun elements(mode: Int, count: Int, type: Int, offset: Long) {
        draw(mode, count, type, offset, 1, 0)
        glDrawElements(mode, count, type, offset)
    }

    @JvmStatic
    fun arrays(mode: Int, first: Int, count: Int) {
        draw(mode, count, 0, first.toLong(), 1, 0)
        glDrawArrays(mode, first, count)
    }

    @JvmStatic
    fun instanced(mode: Int, count: Int, type: Int, offset: Long, instances: Int) {
        draw(mode, count, type, offset, instances, 0)
        glDrawElementsInstanced(mode, count, type, offset, instances)
    }

    @JvmStatic
    fun instancedBase(mode: Int, count: Int, type: Int, offset: Long, instances: Int, base: Int) {
        draw(mode, count, type, offset, instances, base)
        glDrawElementsInstancedBaseInstance(mode, count, type, offset, instances, base)
    }

    @JvmStatic
    fun multiIndirect(mode: Int, type: Int, offset: Long, commands: Int, stride: Int) {
        if (active) {
            try {
                recorder!!.indirect(mode, type, offset, commands, stride)
            } catch (e: Throwable) {
                abort(e)
            }
        }
        glMultiDrawElementsIndirect(mode, type, offset, commands, stride)
    }
}
