package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import org.lwjgl.opengl.GL33.*
import org.lwjgl.opengl.GL42.glDrawElementsInstancedBaseInstance
import org.lwjgl.opengl.GL43.glMultiDrawElementsIndirect

object FrameDump {
    private class Request(val path: Path, val done: CompletableFuture<String> = CompletableFuture())

    private val requests = ConcurrentLinkedQueue<Request>()
    private val names = HashMap<Int, String>()
    private val handles = HashMap<Long, Int>()
    private val target = System.getProperty("render.dump")?.toIntOrNull() ?: -1
    private var frame = 0
    private var recorder: GlRecorder? = null
    private var current: List<Request> = emptyList()

    @JvmField @Volatile var active = false

    fun request(path: Path): String = try {
        Request(path).also(requests::add).done.get(120, TimeUnit.SECONDS)
    } catch (e: ExecutionException) {
        throw e.cause ?: e
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
        if (active) finish()
        frame++
        val batch = generateSequence { requests.poll() }.toList()
        if (batch.isEmpty() && frame + 1 != target) return
        current = batch.ifEmpty { listOf(Request(Path.of(System.getProperty("render.dump.path") ?: "frame-$target.orf"))) }
        try {
            val viewport = IntArray(4).also { glGetIntegerv(GL_VIEWPORT, it) }
            val clear = FloatArray(4).also { glGetFloatv(GL_COLOR_CLEAR_VALUE, it) }
            recorder = GlRecorder(OrfWriter(current[0].path, viewport[2], viewport[3], clear), viewport[2], viewport[3])
            active = true
            println("[FrameDump] recording frame ${frame + 1} to ${current[0].path}")
        } catch (e: Throwable) {
            fail(e)
        }
    }

    private fun finish() {
        active = false
        val rec = recorder!!
        recorder = null
        try {
            rec.writer.close()
            val path = rec.writer.path
            current.drop(1).forEach { Files.copy(path, it.path, StandardCopyOption.REPLACE_EXISTING) }
            val summary = "${path.toAbsolutePath()} ${Files.size(path)} bytes ${rec.summary()}"
            println("[FrameDump] $summary")
            current.forEach { it.done.complete(summary) }
        } catch (e: Throwable) {
            fail(e)
        }
    }

    private fun fail(e: Throwable) {
        println("[FrameDump] failed: $e")
        e.printStackTrace()
        current.forEach { it.done.completeExceptionally(e) }
        current = emptyList()
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
