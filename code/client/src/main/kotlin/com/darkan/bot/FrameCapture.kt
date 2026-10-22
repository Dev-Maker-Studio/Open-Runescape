package com.darkan.bot

import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import org.lwjgl.opengl.GL33.*

object FrameCapture {
    private class Request(val path: Path, val done: CompletableFuture<String> = CompletableFuture())

    private val requests = ConcurrentLinkedQueue<Request>()

    fun request(path: Path): String {
        val request = Request(path)
        requests.add(request)
        return request.done.get(10, TimeUnit.SECONDS)
    }

    class Pixels(val data: ByteBuffer, val width: Int, val height: Int)

    fun read(): Pixels {
        val viewport = IntArray(4).also { glGetIntegerv(GL_VIEWPORT, it) }
        val (width, height) = viewport[2] to viewport[3]
        val readFramebuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING)
        val readBuffer = glGetInteger(GL_READ_BUFFER)
        val alignment = glGetInteger(GL_PACK_ALIGNMENT)
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0)
        glReadBuffer(GL_BACK)
        glPixelStorei(GL_PACK_ALIGNMENT, 1)
        try {
            val pixels = ByteBuffer.allocateDirect(width * height * 3)
            glReadPixels(0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, pixels)
            return Pixels(pixels, width, height)
        } finally {
            glPixelStorei(GL_PACK_ALIGNMENT, alignment)
            glBindFramebuffer(GL_READ_FRAMEBUFFER, readFramebuffer)
            glReadBuffer(readBuffer)
        }
    }

    @JvmStatic
    fun poll() {
        if (requests.isEmpty()) return
        val batch = generateSequence { requests.poll() }.toList()
        try {
            val pixels = read()
            thread(isDaemon = true, name = "frame-capture") { encode(batch, pixels) }
        } catch (e: Throwable) {
            batch.forEach { it.done.completeExceptionally(e) }
        }
    }

    fun write(pixels: Pixels, path: Path) {
        val temp = path.resolveSibling(path.fileName.toString() + ".part")
        try {
            Files.createDirectories(path.toAbsolutePath().parent)
            check(ImageIO.write(image(pixels), "png", temp.toFile())) { "no png writer" }
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    private fun image(pixels: Pixels): BufferedImage {
        val (data, width, height) = Triple(pixels.data, pixels.width, pixels.height)
        require(data.capacity() >= width * height * 3) { "pixel buffer too small" }
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val row = IntArray(width)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = (y * width + x) * 3
                row[x] = (data.get(i).toInt() and 0xff shl 16) or (data.get(i + 1).toInt() and 0xff shl 8) or (data.get(i + 2).toInt() and 0xff)
            }
            image.setRGB(0, height - 1 - y, width, 1, row, 0, width)
        }
        return image
    }

    private fun encode(batch: List<Request>, pixels: Pixels) {
        val image = image(pixels)
        batch.forEach {
            try {
                Files.createDirectories(it.path.toAbsolutePath().parent)
                ImageIO.write(image, "png", it.path.toFile())
                it.done.complete(it.path.toAbsolutePath().toString())
            } catch (e: Throwable) {
                it.done.completeExceptionally(e)
            }
        }
    }
}
