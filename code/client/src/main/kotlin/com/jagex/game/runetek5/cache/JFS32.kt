package com.jagex.game.runetek5.cache

import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class JFS32 @Throws(IOException::class) constructor(
    outputFile: File,
    maxCapacity: Long
) : AutoCloseable {

    @JvmField var rwFile: RandomAccessFile?
    @JvmField var seekPosition: Long = 0L
    @JvmField var maxCapacity: Long

    init {
        var adjustedCapacity = maxCapacity
        if (adjustedCapacity == -1L) {
            adjustedCapacity = Long.MAX_VALUE
        }
        if (outputFile.length() > adjustedCapacity) {
            throw IOException("File exceeds its configured capacity; preserving ${outputFile.absolutePath}")
        }
        this.rwFile = RandomAccessFile(outputFile, "rw")
        this.maxCapacity = adjustedCapacity
        this.seekPosition = 0L
        val dataPoint = this.rwFile!!.read()
        if (dataPoint != -1) {
            this.rwFile!!.seek(0L)
            this.rwFile!!.write(dataPoint)
        }
        this.rwFile!!.seek(0L)
    }

    @Throws(IOException::class)
    override fun close() {
        if (rwFile != null) {
            rwFile!!.close()
            rwFile = null
        }
    }

    @Throws(IOException::class)
    fun write(data: ByteArray, writeOffset: Int, seekOffset: Int) {
        if (seekPosition > maxCapacity || seekOffset.toLong() > maxCapacity - seekPosition) {
            throw EOFException("Write exceeds configured capacity; existing data preserved")
        } else {
            rwFile!!.write(data, writeOffset, seekOffset)
            seekPosition += seekOffset
        }
    }

    @Throws(IOException::class)
    fun length(): Long {
        return rwFile!!.length()
    }

    @Throws(IOException::class)
    fun getSeekOffset(data: ByteArray, offset: Int, length: Int): Int {
        val seekOffset = rwFile!!.read(data, offset, length)
        if (seekOffset > 0) {
            seekPosition += seekOffset
        }
        return seekOffset
    }

    @Throws(IOException::class)
    fun seek(seekTo: Long) {
        rwFile!!.seek(seekTo)
        seekPosition = seekTo
    }
}
