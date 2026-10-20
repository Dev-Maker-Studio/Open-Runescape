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

object BufferStaging {
    private const val MAX_STAGED = 64L shl 20
    private var stagingId = 0
    private var capacity = 0L
    private var cursor = 0L
    private var allocated = 0L

    @JvmStatic var stagedCopies = 0L
    @JvmStatic var directUploads = 0L

    private var enabled = System.getProperty("render.stagedupload")?.equals("false", true) != true

    private fun ensureStaging(bytes: Long) {
        if (stagingId == 0) stagingId = glGenBuffers()
        glBindBuffer(GL_COPY_READ_BUFFER, stagingId)
        if (capacity < bytes) capacity = maxOf(bytes * 2, 4L shl 20)
        if (allocated != capacity || cursor + bytes > capacity) {
            glBufferData(GL_COPY_READ_BUFFER, capacity, GL_STREAM_DRAW)
            allocated = capacity
            cursor = 0
        }
    }

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
        allocated = 0
        cursor = 0
    }
}
