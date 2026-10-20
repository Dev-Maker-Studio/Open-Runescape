package com.jagex.game.runetek5.graphics.modern.dump

import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil

object GlRead {
    fun buffer(buffer: Int, offset: Long, length: Int, target: Int = GL_COPY_READ_BUFFER): ByteArray {
        if (length <= 0) return ByteArray(0)
        val saved = glGetInteger(target)
        glBindBuffer(target, buffer)
        try {
            val size = glGetBufferParameteri(target, GL_BUFFER_SIZE).toLong()
            if (offset + length > size) throw IllegalStateException("buffer $buffer range $offset+$length exceeds $size")
            val bytes = MemoryUtil.memAlloc(length)
            try {
                glGetBufferSubData(target, offset, bytes)
                return ByteArray(length).also(bytes::get)
            } finally {
                MemoryUtil.memFree(bytes)
            }
        } finally {
            glBindBuffer(target, saved)
        }
    }

    class Pixels(val width: Int, val height: Int, val rgba: ByteArray)

    private val depthFormats = setOf(6402, 33189, 33190, 33191, 35056, 36012, 36013, 34041)

    fun binding(unit: Int): Int {
        val active = glGetInteger(GL_ACTIVE_TEXTURE)
        glActiveTexture(GL_TEXTURE0 + unit)
        try {
            return glGetInteger(GL_TEXTURE_BINDING_2D)
        } finally {
            glActiveTexture(active)
        }
    }

    fun glTexture(id: Int): Pixels? {
        val active = glGetInteger(GL_ACTIVE_TEXTURE)
        glActiveTexture(GL_TEXTURE0)
        val saved = glGetInteger(GL_TEXTURE_BINDING_2D)
        glBindTexture(GL_TEXTURE_2D, id)
        try {
            return bound(GL_TEXTURE_2D)
        } finally {
            glBindTexture(GL_TEXTURE_2D, saved)
            glActiveTexture(active)
        }
    }

    private fun bound(target: Int): Pixels? {
        val w = glGetTexLevelParameteri(target, 0, GL_TEXTURE_WIDTH)
        val h = glGetTexLevelParameteri(target, 0, GL_TEXTURE_HEIGHT)
        if (w <= 0 || h <= 0 || glGetTexLevelParameteri(target, 0, GL_TEXTURE_INTERNAL_FORMAT) in depthFormats) return null
        val align = glGetInteger(GL_PACK_ALIGNMENT)
        glPixelStorei(GL_PACK_ALIGNMENT, 1)
        val bytes = MemoryUtil.memAlloc(w * h * 4)
        try {
            glGetTexImage(target, 0, GL_RGBA, GL_UNSIGNED_BYTE, bytes)
            return Pixels(w, h, ByteArray(w * h * 4).also(bytes::get))
        } finally {
            MemoryUtil.memFree(bytes)
            glPixelStorei(GL_PACK_ALIGNMENT, align)
        }
    }

    fun targetSize(): Pair<Int, Int>? {
        val type = glGetFramebufferAttachmentParameteri(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE)
        val name = glGetFramebufferAttachmentParameteri(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME)
        return when (type) {
            GL_TEXTURE -> {
                val active = glGetInteger(GL_ACTIVE_TEXTURE)
                glActiveTexture(GL_TEXTURE0)
                val saved = glGetInteger(GL_TEXTURE_BINDING_2D)
                glBindTexture(GL_TEXTURE_2D, name)
                val r = glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH) to glGetTexLevelParameteri(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT)
                glBindTexture(GL_TEXTURE_2D, saved)
                glActiveTexture(active)
                r
            }
            GL_RENDERBUFFER -> {
                val saved = glGetInteger(GL_RENDERBUFFER_BINDING)
                glBindRenderbuffer(GL_RENDERBUFFER, name)
                val r = glGetRenderbufferParameteri(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH) to glGetRenderbufferParameteri(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT)
                glBindRenderbuffer(GL_RENDERBUFFER, saved)
                r
            }
            else -> null
        }
    }
}
