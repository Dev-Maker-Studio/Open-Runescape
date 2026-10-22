package com.jagex.game.runetek5.cache

import org.junit.Assert.*
import org.junit.Test
import java.io.EOFException
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files

class DownloadedCachePersistenceTest {
    @Test
    fun aDownloadedFileBeyondTheOldLimitIsPreserved() {
        val path = Files.createTempFile("downloaded-cache", ".dat2")
        val length = 524288001L
        try {
            RandomAccessFile(path.toFile(), "rw").use { it.setLength(length) }
            JFS32(path.toFile(), CacheLimits.DATA_BYTES).use {
                assertEquals(length, it.length())
            }
            assertEquals(length, Files.size(path))
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun aSparseIndexBeyondTheOldLimitIsPreserved() {
        val path = Files.createTempFile("downloaded-index", ".idx5")
        val length = 1048577L
        try {
            RandomAccessFile(path.toFile(), "rw").use { it.setLength(length) }
            JFS32(path.toFile(), CacheLimits.INDEX_BYTES).use { assertEquals(length, it.length()) }
            assertEquals(length, Files.size(path))
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun anOversizedBoundedFileIsRejectedWithoutDeletingIt() {
        val path = Files.createTempFile("preserved-cache", ".dat")
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        try {
            Files.write(path, bytes)
            assertThrows(IOException::class.java) { JFS32(path.toFile(), 4) }
            assertArrayEquals(bytes, Files.readAllBytes(path))
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun overflowDoesNotWriteATruncationMarker() {
        val path = Files.createTempFile("bounded-cache", ".dat")
        try {
            JFS32(path.toFile(), 4).use { file ->
                file.write(byteArrayOf(1, 2, 3, 4), 0, 4)
                assertThrows(EOFException::class.java) { file.write(byteArrayOf(9), 0, 1) }
                assertEquals(4, file.length())
            }
            assertArrayEquals(byteArrayOf(1, 2, 3, 4), Files.readAllBytes(path))
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun largeDownloadedArchivesAndReferenceTablesRoundTripAfterReopen() {
        val dir = Files.createTempDirectory("native-cache")
        val data = dir.resolve("main_file_cache.dat2").toFile()
        val index = dir.resolve("main_file_cache.idx36").toFile()
        val references = dir.resolve("main_file_cache.idx255").toFile()
        val archive = ByteArray(2 * 1024 * 1024 + 17) { ((it * 31) xor (it ushr 8)).toByte() }
        val table = ByteArray(1800) { (it * 7).toByte() }
        fun open(read: Boolean) {
            val dat = FileOnDisk(JFS32(data, CacheLimits.DATA_BYTES), 5200)
            val idx = FileOnDisk(JFS32(index, CacheLimits.INDEX_BYTES), 6000)
            val ref = FileOnDisk(JFS32(references, 1048576), 6000)
            try {
                val store = Decompressor(36, dat, idx)
                val master = Decompressor(255, dat, ref)
                if (read) {
                    assertArrayEquals(archive, store.decompress(200001))
                    assertArrayEquals(table, master.decompress(36))
                } else {
                    assertTrue(store.put(200001, archive, archive.size))
                    assertTrue(master.put(36, table, table.size))
                    assertThrows(IllegalArgumentException::class.java) {
                        store.put(1, ByteArray(0), CacheLimits.ARCHIVE_BYTES + 1)
                    }
                }
            } finally {
                ref.close()
                idx.close()
                dat.close()
            }
        }
        try {
            open(false)
            open(true)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
