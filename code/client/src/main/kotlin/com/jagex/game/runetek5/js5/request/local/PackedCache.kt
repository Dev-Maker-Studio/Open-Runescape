package com.jagex.game.runetek5.js5.request.local

import java.io.File
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

object PackedCache {

    private const val SECTOR = 520
    private const val SEGMENT = SECTOR * 2_000_000L

    private val dir: File? = locate()

    val present: Boolean get() = dir != null

    private val data: Array<MappedByteBuffer> = dir?.let {
        FileChannel.open(File(it, "main_file_cache.dat2").toPath(), StandardOpenOption.READ).use { channel ->
            val size = channel.size()
            Array(((size + SEGMENT - 1) / SEGMENT).toInt()) { channel.map(FileChannel.MapMode.READ_ONLY, it * SEGMENT, minOf(SEGMENT, size - it * SEGMENT)) }
        }
    } ?: emptyArray()

    private val tables = arrayOfNulls<MappedByteBuffer>(256)

    private fun locate(): File? {
        val configured = System.getProperty("openrs.cache") ?: System.getenv("OPENRS_CACHE")
        if (configured == "off") return null
        val source = configured?.let(::File) ?: generateSequence(File("").absoluteFile) { it.parentFile }.map { File(it, "assets/cache") }.firstOrNull { it.isDirectory } ?: return null
        return listOf(source, File(source, ".packed")).firstOrNull { File(it, "main_file_cache.dat2").isFile }
    }

    private fun table(index: Int): MappedByteBuffer? = synchronized(tables) {
        tables[index] ?: File(dir, "main_file_cache.idx$index").takeIf { it.isFile }?.let {
            FileChannel.open(it.toPath(), StandardOpenOption.READ).use { channel -> channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()) }
        }?.also { tables[index] = it }
    }

    private fun MappedByteBuffer.u24(at: Int) = (get(at).toInt() and 0xff shl 16) or (get(at + 1).toInt() and 0xff shl 8) or (get(at + 2).toInt() and 0xff)

    fun read(index: Int, id: Int): ByteArray? {
        val idx = table(index) ?: return null
        val entry = id * 6L
        if (id < 0 || entry + 6 > idx.capacity()) return null
        val size = idx.u24(entry.toInt())
        var sector = idx.u24(entry.toInt() + 3)
        if (size == 0 || sector == 0) return null
        val head = if (id > 65535) 10 else 8
        val out = ByteArray(size)
        var done = 0
        while (done < size) {
            val position = sector * SECTOR.toLong()
            val segment = data[(position / SEGMENT).toInt()]
            val offset = (position % SEGMENT).toInt()
            val count = minOf(size - done, SECTOR - head)
            segment.get(offset + head, out, done, count)
            sector = segment.u24(offset + head - 4)
            done += count
        }
        return out
    }
}
