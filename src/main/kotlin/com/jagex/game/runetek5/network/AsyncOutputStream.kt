package com.jagex.game.runetek5.network

import java.io.IOException
import java.io.OutputStream

class AsyncOutputStream(outputstream: OutputStream, capacity: Int) : Runnable {

    @JvmField var occurredException: IOException? = null
    @JvmField var writeOffset = 0
    @JvmField var readOffset = 0
    @JvmField var outputstream: OutputStream
    @JvmField var shutdown = false
    @JvmField val bufferSize: Int
    @JvmField val buffer: ByteArray
    @JvmField val executingThread: Thread

    init {
        this.outputstream = outputstream
        bufferSize = capacity + 1
        buffer = ByteArray(bufferSize)
        executingThread = Thread(this)
        executingThread.isDaemon = true
        executingThread.start()
    }

    fun closed(): Boolean {
        if (shutdown) {
            try {
                outputstream.close()
                if (occurredException == null) {
                    occurredException = IOException("")
                }
            } catch (ioexception: IOException) {
                if (occurredException == null) {
                    occurredException = IOException(ioexception)
                }
            }
            return true
        }
        return false
    }

    override fun run() {
        do {
            var length: Int
            synchronized(this) {
                while (true) {
                    if (occurredException != null) {
                        return
                    }
                    length = if (writeOffset <= readOffset) {
                        readOffset - writeOffset
                    } else {
                        bufferSize - writeOffset + readOffset
                    }
                    if (length > 0) {
                        break
                    }
                    try {
                        outputstream.flush()
                    } catch (ioexception: IOException) {
                        occurredException = ioexception
                        return
                    }
                    if (closed()) {
                        return
                    }
                    try {
                        (this as Object).wait()
                    } catch (exception: InterruptedException) {
                        // NOOP
                    }
                }
            }
            try {
                if (length + writeOffset <= bufferSize) {
                    outputstream.write(buffer, writeOffset, length)
                } else {
                    val sizeOffset = bufferSize - writeOffset
                    outputstream.write(buffer, writeOffset, sizeOffset)
                    outputstream.write(buffer, 0, length - sizeOffset)
                }
            } catch (ioexception: IOException) {
                synchronized(this) {
                    occurredException = ioexception
                    return
                }
            }
            synchronized(this) {
                writeOffset = (length + writeOffset) % bufferSize
            }
        } while (!closed())
    }

    @Throws(IOException::class)
    fun write(readingBuffer: ByteArray, srcPosition: Int, size: Int) {
        if (size >= 0 && srcPosition >= 0 && size + srcPosition <= readingBuffer.size) {
            synchronized(this) {
                if (occurredException != null) {
                    throw IOException(occurredException.toString())
                }
                val sizeLeft: Int = if (writeOffset <= readOffset) {
                    bufferSize - readOffset + writeOffset - 1
                } else {
                    writeOffset - readOffset - 1
                }
                if (sizeLeft < size) {
                    throw IOException("")
                }
                if (size + readOffset <= bufferSize) {
                    System.arraycopy(readingBuffer, srcPosition, buffer, readOffset, size)
                } else {
                    val length = bufferSize - readOffset
                    System.arraycopy(readingBuffer, srcPosition, buffer, readOffset, length)
                    System.arraycopy(readingBuffer, length + srcPosition, buffer, 0, size - length)
                }
                readOffset = (size + readOffset) % bufferSize
                (this as Object).notifyAll()
            }
        } else {
            throw IOException()
        }
    }

    fun drop() {
        outputstream = DroppedOutputStream()
    }

    fun shutdown() {
        synchronized(this) {
            shutdown = true
            (this as Object).notifyAll()
        }
        try {
            executingThread.join()
        } catch (exception: InterruptedException) {
            // NOOP
        }
    }
}
