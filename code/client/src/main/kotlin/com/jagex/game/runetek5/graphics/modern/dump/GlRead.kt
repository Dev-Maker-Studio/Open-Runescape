package com.jagex.game.runetek5.graphics.modern.dump

import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteOrder

internal object TexturePacking {
    const val MAX_BYTES = 32 * 1024 * 1024 - 16

    fun byteSize(width: Int, height: Int, layers: Int, format: RecordedTextureFormat = RecordedTextureFormat.RGBA8): Int {
        require(width in 1..8192 && height in 1..8192 && layers in 1..2048) { "texture dimensions $width x $height x $layers" }
        val bytes = width.toLong() * height * layers * format.texelBytes
        require(bytes <= MAX_BYTES - if (format == RecordedTextureFormat.RGBA8) 0 else 12) { "texture byte limit $bytes" }
        return bytes.toInt()
    }

    fun pack(width: Int, height: Int, layers: Int, cube: Boolean, format: RecordedTextureFormat = RecordedTextureFormat.RGBA8, read: (Int, ByteArray, Int, Int) -> Unit): ByteArray {
        require(!cube || width == height && layers == 6) { "cube dimensions $width x $height x $layers" }
        val bytes = ByteArray(byteSize(width, height, layers, format))
        val size = if (cube) bytes.size / 6 else bytes.size
        for (i in 0 until if (cube) 6 else 1) read(i, bytes, i * size, size)
        return bytes
    }

    fun swizzle(bytes: ByteArray, channels: IntArray, format: RecordedTextureFormat = RecordedTextureFormat.RGBA8) {
        val component = format.componentBytes
        val stride = format.texelBytes
        require(bytes.size % stride == 0 && channels.size == 4 && channels.all { it in 0..5 })
        if (channels.contentEquals(intArrayOf(0, 1, 2, 3))) return
        val pixel = ByteArray(stride)
        for (i in bytes.indices step stride) {
            System.arraycopy(bytes, i, pixel, 0, stride)
            for (c in 0..3) for (j in 0 until component) bytes[i + c * component + j] = when (channels[c]) {
                in 0..3 -> pixel[channels[c] * component + j]
                4 -> 0.toByte()
                else -> (if (component == 1) -1 else if (j == 0) 0 else 0x3c).toByte()
            }
        }
    }

    fun flipRows(bytes: ByteArray, width: Int, height: Int, layers: Int, format: RecordedTextureFormat = RecordedTextureFormat.RGBA8) {
        require(bytes.size == byteSize(width, height, layers, format))
        val stride = width * format.texelBytes
        val row = ByteArray(stride)
        for (z in 0 until layers) for (y in 0 until height / 2) {
            val a = (z * height + y) * stride
            val b = (z * height + height - y - 1) * stride
            System.arraycopy(bytes, a, row, 0, stride)
            System.arraycopy(bytes, b, bytes, a, stride)
            System.arraycopy(row, 0, bytes, b, stride)
        }
    }
}

object GlRead {
    fun buffer(buffer: Int, offset: Long, length: Int, target: Int = GL_COPY_READ_BUFFER): ByteArray = CaptureProfiler.measure("bufferReadback") {
        require(offset >= 0 && length >= 0)
        if (length == 0) return@measure ByteArray(0)
        val saved = glGetInteger(target)
        GLWrapper.bindBuffer(target, buffer)
        try {
            val size = glGetBufferParameteri64(target, GL_BUFFER_SIZE)
            require(offset <= size && length.toLong() <= size - offset) { "buffer $buffer range $offset+$length exceeds $size" }
            val bytes = MemoryUtil.memAlloc(length)
            try {
                glGetBufferSubData(target, offset, bytes)
                ByteArray(length).also(bytes::get)
            } finally {
                MemoryUtil.memFree(bytes)
            }
        } finally {
            GLWrapper.bindBuffer(target, saved)
        }
    }

    class Pixels(val width: Int, val height: Int, val rgba: ByteArray, val layers: Int = 1, val format: RecordedTextureFormat = RecordedTextureFormat.RGBA8, val sampler: Int = 9)
    class Image(val width: Int, val height: Int, val layers: Int, val format: Int)

    private val depthFormats = setOf(GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT16, GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT32, GL_DEPTH24_STENCIL8, GL_DEPTH_COMPONENT32F, GL_DEPTH32F_STENCIL8, GL_DEPTH_STENCIL)
    private val packParams = intArrayOf(GL_PACK_ALIGNMENT, GL_PACK_ROW_LENGTH, GL_PACK_IMAGE_HEIGHT, GL_PACK_SKIP_PIXELS, GL_PACK_SKIP_ROWS, GL_PACK_SKIP_IMAGES, GL_PACK_SWAP_BYTES, GL_PACK_LSB_FIRST)

    private fun bindingName(target: Int): Int = when (target) {
        GL_TEXTURE_2D -> GL_TEXTURE_BINDING_2D
        GL_TEXTURE_3D -> GL_TEXTURE_BINDING_3D
        GL_TEXTURE_CUBE_MAP -> GL_TEXTURE_BINDING_CUBE_MAP
        else -> error("texture target $target")
    }

    fun binding(unit: Int, target: Int = GL_TEXTURE_2D): Int {
        require(unit in 0 until glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS))
        val active = glGetInteger(GL_ACTIVE_TEXTURE)
        GLWrapper.activeTexture(GL_TEXTURE0 + unit)
        try {
            return glGetInteger(bindingName(target))
        } finally {
            GLWrapper.activeTexture(active)
        }
    }

    private fun <T> withTexture(id: Int, target: Int, read: () -> T): T {
        require(id != 0 && glIsTexture(id)) { "missing texture $id" }
        val active = glGetInteger(GL_ACTIVE_TEXTURE)
        GLWrapper.activeTexture(GL_TEXTURE0)
        val saved = glGetInteger(bindingName(target))
        try {
            GLWrapper.bindTexture(target, id)
            return read()
        } finally {
            GLWrapper.bindTexture(target, saved)
            GLWrapper.activeTexture(active)
        }
    }

    private fun image(target: Int): Image = Image(
        glGetTexLevelParameteri(target, 0, GL_TEXTURE_WIDTH),
        glGetTexLevelParameteri(target, 0, GL_TEXTURE_HEIGHT),
        if (target == GL_TEXTURE_3D) glGetTexLevelParameteri(target, 0, GL_TEXTURE_DEPTH) else 1,
        glGetTexLevelParameteri(target, 0, GL_TEXTURE_INTERNAL_FORMAT)
    )

    fun sceneTexture(id: Int, width: Int, height: Int, depth: Boolean) = withTexture(id, GL_TEXTURE_2D) {
        val image = image(GL_TEXTURE_2D)
        require(image.width == width && image.height == height && (image.format in depthFormats) == depth) { "scene ${if (depth) "depth" else "color"} texture ${image.width} x ${image.height}, format ${image.format}" }
        if (!depth) normalized(GL_TEXTURE_2D)
    }

    private fun normalized(target: Int) {
        val format = glGetTexLevelParameteri(target, 0, GL_TEXTURE_INTERNAL_FORMAT)
        require(format !in depthFormats && format != GL_SRGB8 && format != GL_SRGB8_ALPHA8) { "texture format $format requires typed recording" }
        for ((size, type) in arrayOf(GL_TEXTURE_RED_SIZE to GL_TEXTURE_RED_TYPE, GL_TEXTURE_GREEN_SIZE to GL_TEXTURE_GREEN_TYPE, GL_TEXTURE_BLUE_SIZE to GL_TEXTURE_BLUE_TYPE, GL_TEXTURE_ALPHA_SIZE to GL_TEXTURE_ALPHA_TYPE)) {
            val bits = glGetTexLevelParameteri(target, 0, size)
            require(bits == 0 || bits <= 8 && glGetTexLevelParameteri(target, 0, type) == GL_UNSIGNED_NORMALIZED) { "texture format $format requires typed recording" }
        }
    }

    fun glTexture(id: Int, target: Int = GL_TEXTURE_2D, flipY: Boolean = false): Pixels? = CaptureProfiler.measure("textureReadback") {
        if (id == 0) return@measure null
        withTexture(id, target) {
            val first = if (target == GL_TEXTURE_CUBE_MAP) GL_TEXTURE_CUBE_MAP_POSITIVE_X else target
            val image = image(first)
            val layers = if (target == GL_TEXTURE_CUBE_MAP) 6 else image.layers
            val format = if (image.format == GL_RGBA16F || image.format == GL_RGB16F) RecordedTextureFormat.RGBA16F else RecordedTextureFormat.RGBA8
            require(format == RecordedTextureFormat.RGBA8 || target == GL_TEXTURE_2D) { "typed cube/volume recording unsupported" }
            if (format == RecordedTextureFormat.RGBA8) normalized(first)
            val sampler = if (format == RecordedTextureFormat.RGBA16F) {
                require(glGetTexParameteri(target, GL_TEXTURE_BASE_LEVEL) == 0) { "typed texture base level unsupported" }
                val min = when (val filter = glGetTexParameteri(target, GL_TEXTURE_MIN_FILTER)) {
                    GL_NEAREST -> 0; GL_LINEAR -> 1; GL_NEAREST_MIPMAP_NEAREST -> 2; GL_LINEAR_MIPMAP_NEAREST -> 3; GL_NEAREST_MIPMAP_LINEAR -> 4; GL_LINEAR_MIPMAP_LINEAR -> 5
                    else -> error("typed texture min filter $filter")
                }
                val mag = when (val filter = glGetTexParameteri(target, GL_TEXTURE_MAG_FILTER)) { GL_NEAREST -> 0; GL_LINEAR -> 1; else -> error("typed texture mag filter $filter") }
                fun wrap(p: Int) = when (val mode = glGetTexParameteri(target, p)) { GL_REPEAT -> 0; GL_CLAMP_TO_EDGE -> 1; GL_MIRRORED_REPEAT -> 2; else -> error("typed texture wrap $mode") }
                min or (mag shl 3) or (wrap(GL_TEXTURE_WRAP_S) shl 4) or (wrap(GL_TEXTURE_WRAP_T) shl 6)
            } else 9
            val size = TexturePacking.byteSize(image.width, image.height, layers, format)
            if (target == GL_TEXTURE_CUBE_MAP) require(image.width == image.height) { "non-square cube" }
            val channels = intArrayOf(GL_TEXTURE_SWIZZLE_R, GL_TEXTURE_SWIZZLE_G, GL_TEXTURE_SWIZZLE_B, GL_TEXTURE_SWIZZLE_A).map { pname ->
                when (val channel = glGetTexParameteri(target, pname)) { GL_RED -> 0; GL_GREEN -> 1; GL_BLUE -> 2; GL_ALPHA -> 3; GL_ZERO -> 4; GL_ONE -> 5; else -> error("texture swizzle $channel") }
            }.toIntArray()
            val savedPack = packParams.map(::glGetInteger)
            val savedBuffer = glGetInteger(GL_PIXEL_PACK_BUFFER_BINDING)
            val bytes = MemoryUtil.memAlloc(if (target == GL_TEXTURE_CUBE_MAP) size / 6 else size)
            try {
                GLWrapper.bindBuffer(GL_PIXEL_PACK_BUFFER, 0)
                for (p in packParams) glPixelStorei(p, if (p == GL_PACK_ALIGNMENT) 1 else 0)
                val rgba = TexturePacking.pack(image.width, image.height, layers, target == GL_TEXTURE_CUBE_MAP, format) { face, out, offset, length ->
                    val readTarget = if (target == GL_TEXTURE_CUBE_MAP) GL_TEXTURE_CUBE_MAP_POSITIVE_X + face else target
                    val current = image(readTarget)
                    require(current.width == image.width && current.height == image.height && current.layers == image.layers && current.format == image.format) { "incomplete cube face $face" }
                    if (format == RecordedTextureFormat.RGBA8) normalized(readTarget)
                    bytes.clear()
                    glGetTexImage(readTarget, 0, GL_RGBA, if (format == RecordedTextureFormat.RGBA16F) GL_HALF_FLOAT else GL_UNSIGNED_BYTE, bytes)
                    bytes.get(out, offset, length)
                }
                if (format.componentBytes == 2 && ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
                    for (i in rgba.indices step 2) {
                        val low = rgba[i]
                        rgba[i] = rgba[i + 1]
                        rgba[i + 1] = low
                    }
                }
                TexturePacking.swizzle(rgba, channels, format)
                if (flipY) TexturePacking.flipRows(rgba, image.width, image.height, layers, format)
                Pixels(image.width, image.height, rgba, layers, format, sampler)
            } finally {
                MemoryUtil.memFree(bytes)
                for (i in packParams.indices) glPixelStorei(packParams[i], savedPack[i])
                GLWrapper.bindBuffer(GL_PIXEL_PACK_BUFFER, savedBuffer)
            }
        }
    }

    fun backRegion(rect: IntArray, width: Int, height: Int): ByteArray = CaptureProfiler.measure("historyReadback") {
        require(rect.size == 4 && rect[0] >= 0 && rect[1] >= 0 && rect[2] > 0 && rect[3] > 0 && rect[0].toLong() + rect[2] <= width && rect[1].toLong() + rect[3] <= height)
        val size = TexturePacking.byteSize(rect[2], rect[3], 1)
        val savedFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING)
        val savedBuffer = glGetInteger(GL_PIXEL_PACK_BUFFER_BINDING)
        val savedPack = packParams.map(::glGetInteger)
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0)
        val savedRead = glGetInteger(GL_READ_BUFFER)
        try {
            require(glGetBoolean(GL_DOUBLEBUFFER)) { "untouched history requires double-buffered BACK" }
            glReadBuffer(GL_BACK)
            GLWrapper.bindBuffer(GL_PIXEL_PACK_BUFFER, 0)
            for (p in packParams) glPixelStorei(p, if (p == GL_PACK_ALIGNMENT) 1 else 0)
            val bytes = MemoryUtil.memAlloc(size)
            try {
                glReadPixels(rect[0], height - rect[1] - rect[3], rect[2], rect[3], GL_RGBA, GL_UNSIGNED_BYTE, bytes)
                ByteArray(size).also { out ->
                    bytes.get(out)
                    TexturePacking.flipRows(out, rect[2], rect[3], 1)
                }
            } finally {
                MemoryUtil.memFree(bytes)
            }
        } finally {
            for (i in packParams.indices) glPixelStorei(packParams[i], savedPack[i])
            GLWrapper.bindBuffer(GL_PIXEL_PACK_BUFFER, savedBuffer)
            glReadBuffer(savedRead)
            glBindFramebuffer(GL_READ_FRAMEBUFFER, savedFbo)
        }
    }

    fun targetSize(): Pair<Int, Int>? {
        val type = glGetFramebufferAttachmentParameteri(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE)
        val name = glGetFramebufferAttachmentParameteri(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME)
        return when (type) {
            GL_TEXTURE -> withTexture(name, GL_TEXTURE_2D) { image(GL_TEXTURE_2D).let { it.width to it.height } }
            GL_RENDERBUFFER -> {
                val saved = glGetInteger(GL_RENDERBUFFER_BINDING)
                try {
                    glBindRenderbuffer(GL_RENDERBUFFER, name)
                    glGetRenderbufferParameteri(GL_RENDERBUFFER, GL_RENDERBUFFER_WIDTH) to glGetRenderbufferParameteri(GL_RENDERBUFFER, GL_RENDERBUFFER_HEIGHT)
                } finally {
                    glBindRenderbuffer(GL_RENDERBUFFER, saved)
                }
            }
            else -> null
        }
    }
}
