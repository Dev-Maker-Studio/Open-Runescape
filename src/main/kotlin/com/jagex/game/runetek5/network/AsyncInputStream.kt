package com.jagex.game.runetek5.network

import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * < Fully refactored >
 * @author Brainpower - 30/05/2020
 *
 * Manages an inputstream on a separate thread
 */
class AsyncInputStream(inputstream: InputStream, size: Int) : Runnable {

    var readError: IOException? = null
    var readOffset: Int = 0
    var inputStream: InputStream = inputstream
    var readPosition: Int = 0
    val capacity: Int = size + 1
    val buffer: ByteArray = ByteArray(capacity)
    val executingThread: Thread

    init {
        executingThread = Thread(this)
        executingThread.isDaemon = true
        executingThread.start()
    }

    /**
     * Reads the inputstream and manages the offset
     */
    override fun run() {
        while (true) {
            var length: Int
            synchronized(this) {
                while (true) {
                    if (readError != null) {
                        return
                    }
                    length = when {
                        readPosition == 0 -> capacity - readOffset - 1
                        readPosition <= readOffset -> capacity - readOffset
                        else -> readPosition - readOffset - 1
                    }
                    if (length > 0) {
                        break
                    }
                    try {
                        (this as Object).wait()
                    } catch (interruptedException: InterruptedException) {
                        // NOOP
                    }
                }
            }
            val readResult: Int
            try {
                readResult = inputStream.read(buffer, readOffset, length)
                if (readResult == -1) {
                    throw EOFException()
                }
            } catch (ioexception: IOException) {
                synchronized(this) {
                    readError = ioexception
                    return
                }
            }
            synchronized(this) {
                readOffset = (readResult + readOffset) % capacity
            }
        }
    }

    /**
     * Reads a part of the inputstream's buffer into given buffer
     */
    fun read(destinationBuffer: ByteArray, destinationPosition: Int, copyLength: Int): Int {
        var actualCopyLength = copyLength
        if (actualCopyLength >= 0 && destinationPosition >= 0 && actualCopyLength + destinationPosition <= destinationBuffer.size) {
            synchronized(this) {
                val maxLength = if (readPosition <= readOffset) {
                    readOffset - readPosition
                } else {
                    capacity - readPosition + readOffset
                }
                if (actualCopyLength > maxLength) {
                    actualCopyLength = maxLength
                }
                if (actualCopyLength == 0 && readError != null) {
                    throw IOException(readError.toString())
                }
                if (actualCopyLength + readPosition <= capacity) {
                    System.arraycopy(buffer, readPosition, destinationBuffer, destinationPosition, actualCopyLength)
                } else {
                    val secondaryReadOffset = capacity - readPosition
                    System.arraycopy(buffer, readPosition, destinationBuffer, destinationPosition, secondaryReadOffset)
                    System.arraycopy(
                        buffer,
                        0,
                        destinationBuffer,
                        secondaryReadOffset + destinationPosition,
                        actualCopyLength - secondaryReadOffset
                    )
                }
                readPosition = (actualCopyLength + readPosition) % capacity
                (this as Object).notifyAll()
                return actualCopyLength
            }
        } else {
            throw IOException()
        }
    }

    /**
     * Checks if the inputstream has enough capacity to fit a certain amount of bytes
     */
    fun available(capacityRequired: Int): Boolean {
        if (capacityRequired > 0 && capacityRequired < capacity) {
            synchronized(this) {
                val length = if (readPosition <= readOffset) {
                    readOffset - readPosition
                } else {
                    capacity - readPosition + readOffset
                }
                if (length < capacityRequired) {
                    if (readError != null) {
                        throw IOException(readError.toString())
                    }
                    (this as Object).notifyAll()
                    return false
                }
                return true
            }
        }
        throw IOException()
    }

    /**
     * Checks how many bytes are left to be read
     */
    fun capacityLeft(): Int {
        synchronized(this) {
            val length = if (readPosition <= readOffset) {
                readOffset - readPosition
            } else {
                capacity - readPosition + readOffset
            }
            if (readError != null) {
                throw IOException(readError.toString())
            }
            (this as Object).notifyAll()
            return length
        }
    }

    /**
     * Drops the inputstream to be shutdown automatically in 30 seconds
     */
    fun drop() {
        inputStream = DroppedInputStream()
    }

    /**
     * Wakes up all the threads and joins them
     */
    fun shutdown() {
        synchronized(this) {
            if (readError == null) {
                readError = IOException("")
            }
            (this as Object).notifyAll()
        }
        try {
            executingThread.join()
        } catch (interruptedException: InterruptedException) {
            // NOOP
        }
    }
}
