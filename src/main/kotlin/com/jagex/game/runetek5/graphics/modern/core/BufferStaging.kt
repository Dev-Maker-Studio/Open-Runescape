package com.jagex.game.runetek5.graphics.modern.core

import org.lwjgl.opengl.GL31.GL_COPY_READ_BUFFER
import org.lwjgl.opengl.GL31.GL_COPY_WRITE_BUFFER
import org.lwjgl.opengl.GL31.glCopyBufferSubData
import org.lwjgl.opengl.GL15.GL_STREAM_DRAW
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glBufferData
import org.lwjgl.opengl.GL15.glBufferSubData
import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL15.glGenBuffers
import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * Sub-range updates of large resident buffers (the instanced geometry store) without
 * a CPU/GPU sync.
 *
 * `glBufferSubData` into a buffer the GPU is still reading (the previous frame's
 * draws) makes the driver either wait for those draws or copy the whole store —
 * with a ~30 MB vertex store that showed up as 20–95 ms `swap` stalls on exactly the
 * frames where many animated locs rebuilt their geometry. Writing the bytes into a
 * freshly orphaned streaming buffer never syncs, and `glCopyBufferSubData` from it
 * into the store is queued on the GPU behind the draws that still use the old
 * contents, so it is both stall-free and ordered.
 *
 * Uploads larger than [MAX_STAGED] (the initial bulk fill) go straight through
 * `glBufferSubData` — a store that has never been drawn from cannot be busy.
 */
object BufferStaging {
    private const val MAX_STAGED = 64L shl 20
    private var stagingId = 0
    private var capacity = 0L
    private var cursor = 0L

    /** Copies staged through [BufferStaging] (diag). */
    @JvmStatic var stagedCopies = 0L
    @JvmStatic var directUploads = 0L

    private var enabled = System.getProperty("render.stagedupload")?.equals("false", true) != true

    private fun ensureStaging(bytes: Long) {
        if (stagingId == 0) stagingId = glGenBuffers()
        glBindBuffer(GL_COPY_READ_BUFFER, stagingId)
        if (capacity < bytes) capacity = maxOf(bytes, 1L shl 20)
        if (cursor + bytes > capacity || cursor == 0L && capacity == 0L) {
            // Orphan: fresh storage, no dependency on copies queued from the old one.
            glBufferData(GL_COPY_READ_BUFFER, capacity, GL_STREAM_DRAW)
            cursor = 0
        }
    }

    /** Writes [data] (position..limit) into buffer [targetId] at [targetOffset]; position is preserved. */
    @JvmStatic
    fun upload(targetId: Int, targetTarget: Int, targetOffset: Long, data: ByteBuffer) {
        val bytes = data.remaining().toLong()
        if (bytes == 0L) return
        if (!enabled || bytes > MAX_STAGED) {
            directUploads++
            glBindBuffer(targetTarget, targetId)
            glBufferSubData(targetTarget, targetOffset, data)
            return
        }
        ensureStaging(bytes)
        if (cursor == 0L) glBufferData(GL_COPY_READ_BUFFER, capacity, GL_STREAM_DRAW)
        glBufferSubData(GL_COPY_READ_BUFFER, cursor, data)
        glBindBuffer(GL_COPY_WRITE_BUFFER, targetId)
        glCopyBufferSubData(GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER, cursor, targetOffset, bytes)
        cursor += bytes
        stagedCopies++
    }

    @JvmStatic
    fun upload(targetId: Int, targetTarget: Int, targetOffset: Long, data: IntBuffer) {
        val bytes = data.remaining().toLong() * 4
        if (bytes == 0L) return
        if (!enabled || bytes > MAX_STAGED) {
            directUploads++
            glBindBuffer(targetTarget, targetId)
            glBufferSubData(targetTarget, targetOffset, data)
            return
        }
        ensureStaging(bytes)
        if (cursor == 0L) glBufferData(GL_COPY_READ_BUFFER, capacity, GL_STREAM_DRAW)
        glBufferSubData(GL_COPY_READ_BUFFER, cursor, data)
        glBindBuffer(GL_COPY_WRITE_BUFFER, targetId)
        glCopyBufferSubData(GL_COPY_READ_BUFFER, GL_COPY_WRITE_BUFFER, cursor, targetOffset, bytes)
        cursor += bytes
        stagedCopies++
    }

    @JvmStatic
    fun destroy() {
        if (stagingId != 0) glDeleteBuffers(stagingId)
        stagingId = 0
        capacity = 0
        cursor = 0
    }
}
