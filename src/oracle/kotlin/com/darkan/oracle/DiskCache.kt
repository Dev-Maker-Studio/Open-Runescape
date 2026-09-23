package com.darkan.oracle

import com.jagex.game.runetek5.cache.Decompressor
import com.jagex.game.runetek5.cache.FileOnDisk
import com.jagex.game.runetek5.cache.JFS32
import com.jagex.game.runetek5.js5.Js5MasterIndex
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.js5.request.FileWorker
import com.jagex.game.runetek5.tools.Hashing
import java.io.File

class DiskCache(dir: File) {

    private val data = open(File(dir, "main_file_cache.dat2"), 5200)
    private val stores = HashMap<Int, Decompressor?>()

    init {
        stores[255] = Decompressor(255, data, open(File(dir, "main_file_cache.idx255"), 6000))
        for (index in 0 until 255) {
            val idx = File(dir, "main_file_cache.idx$index")
            if (idx.isFile) {
                stores[index] = Decompressor(index, data, open(idx, 6000))
            }
        }
    }

    fun store(index: Int): Decompressor = stores[index] ?: error("no idx$index in the cache")

    fun group(index: Int, group: Int): ByteArray? = store(index).decompress(group)

    fun masterIndex(index: Int): Js5MasterIndex {
        val table = group(255, index) ?: error("no reference table for index $index")
        return Js5MasterIndex(table, Hashing.getCrc32(table, table.size), null)
    }

    fun provider(index: Int): Js5ResourceProvider =
        Js5ResourceProvider(Worker(index), DIRECT_MEMORY_INDICES.contains(index), if (index == 36) 2 else 1)

    private inner class Worker(private val index: Int) : FileWorker() {
        private val table by lazy { masterIndex(index) }

        override fun getMasterIndex(): Js5MasterIndex = table

        override fun requestArchive(archiveId: Int): ByteArray? {
            val payload = group(index, archiveId) ?: return null
            if (payload.size <= 2) {
                return null
            }
            if (Hashing.getCrc32(payload, payload.size - 2) != table.crcs[archiveId]) {
                return null
            }
            val version = (payload[payload.size - 1].toInt() and 0xff) + ((payload[payload.size - 2].toInt() and 0xff) shl 8)
            if (version != (table.versions[archiveId] and 0xffff)) {
                return null
            }
            return payload
        }

        override fun prefetch(fileId: Int) {}

        override fun getProgress(fileId: Int): Int = 100
    }

    companion object {
        private val DIRECT_MEMORY_INDICES = setOf(5, 6, 23, 26, 28, 30, 31, 35, 36)

        private fun open(file: File, bufferSize: Int): FileOnDisk {
            require(file.isFile) { "missing $file" }
            return FileOnDisk(JFS32(file, -1L), bufferSize)
        }
    }
}
