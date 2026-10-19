package com.jagex.game.runetek5.cache

import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Jagex File System wrapper for RandomAccessFile with max capacity enforcement.
 *
 * @author Brainpower
 */
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
            outputFile.delete()
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

    /**
     * Writes data to the file, throwing EOFException if capacity exceeded.
     *
     * @param data The data to write
     * @param writeOffset The offset in the data array
     * @param seekOffset The number of bytes to write
     * @throws IOException if an I/O error occurs
     * @throws EOFException if writing would exceed max capacity
     */
    @Throws(IOException::class)
    fun write(data: ByteArray, writeOffset: Int, seekOffset: Int) {
        if (seekPosition + seekOffset > maxCapacity) {
            rwFile!!.seek(maxCapacity)
            rwFile!!.write(1)
            throw EOFException()
        } else {
            rwFile!!.write(data, writeOffset, seekOffset)
            seekPosition += seekOffset
        }
    }

    /**
     * Returns the length of the underlying file.
     *
     * @return The file length in bytes
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    fun length(): Long {
        return rwFile!!.length()
    }

    /**
     * Reads data from the file and updates the seek position.
     *
     * @param data The buffer to read into
     * @param offset The offset in the buffer
     * @param length The maximum number of bytes to read
     * @return The number of bytes actually read
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    fun getSeekOffset(data: ByteArray, offset: Int, length: Int): Int {
        val seekOffset = rwFile!!.read(data, offset, length)
        if (seekOffset > 0) {
            seekPosition += seekOffset
        }
        return seekOffset
    }

    /**
     * Seeks to a specific position in the file.
     *
     * @param seekTo The position to seek to
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    fun seek(seekTo: Long) {
        rwFile!!.seek(seekTo)
        seekPosition = seekTo
    }
}
