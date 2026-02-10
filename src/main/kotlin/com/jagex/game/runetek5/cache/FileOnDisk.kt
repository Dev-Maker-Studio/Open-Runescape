package com.jagex.game.runetek5.cache

import java.io.EOFException
import java.io.IOException

class FileOnDisk(
    @JvmField val fs32: JFS32,
    capacity: Int
) {

    @JvmField
    var currentPosition: Long = 0L

    @JvmField
    var fileChannelPosition: Long = 0L

    @JvmField
    var initialCapacity: Long

    @JvmField
    var obfuscatedWriteBufferSize: Int = 0

    @JvmField
    var writeBufferFilePosition: Long = -1L

    @JvmField
    var occupiedSize: Long

    @JvmField
    var readBufferFilePosition: Long = -1L

    @JvmField
    var readBuffer: ByteArray

    @JvmField
    var writeBuffer: ByteArray

    @JvmField
    var readBufferValidBytes: Int = 0

    init {
        initialCapacity = fs32.length()
        occupiedSize = initialCapacity
        readBuffer = ByteArray(capacity)
        writeBuffer = ByteArray(0)
        currentPosition = 0L
    }

    fun getInitialSize(): Long {
        return initialCapacity
    }

    @Throws(IOException::class)
    fun close() {
        flushWriteBuffer()
        fs32.close()
    }

    @Throws(IOException::class)
    fun setPosition(position: Long) {
        if (position < 0L) {
            throw IOException()
        }
        currentPosition = position
    }

    @Throws(IOException::class)
    fun load0(buffer: ByteArray, i_2: Int, i_3: Int) {
        var i_2_mod = i_2
        var i_3_mod = i_3
        try {
            if (i_3_mod + i_2_mod > buffer.size) {
                throw ArrayIndexOutOfBoundsException(i_3_mod + i_2_mod - buffer.size)
            }

            if (writeBufferFilePosition != -1L && currentPosition >= writeBufferFilePosition && currentPosition + i_3_mod <= (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition) {
                System.arraycopy(writeBuffer, (currentPosition - writeBufferFilePosition).toInt(), buffer, i_2_mod, i_3_mod)
                currentPosition += i_3_mod
                return
            }

            val long_5 = currentPosition
            val i_8 = i_3_mod
            var i_9: Int
            if (currentPosition >= readBufferFilePosition && currentPosition < readBufferValidBytes + readBufferFilePosition) {
                i_9 = (readBufferValidBytes - (currentPosition - readBufferFilePosition)).toInt()
                if (i_9 > i_3_mod) {
                    i_9 = i_3_mod
                }

                System.arraycopy(readBuffer, (currentPosition - readBufferFilePosition).toInt(), buffer, i_2_mod, i_9)
                currentPosition += i_9
                i_2_mod += i_9
                i_3_mod -= i_9
            }

            if (i_3_mod > readBuffer.size) {
                fs32.seek(currentPosition)

                fileChannelPosition = currentPosition
                while (i_3_mod > 0) {
                    i_9 = fs32.getSeekOffset(buffer, i_2_mod, i_3_mod)
                    if (i_9 == -1) {
                        break
                    }

                    fileChannelPosition += i_9
                    currentPosition += i_9
                    i_2_mod += i_9
                    i_3_mod -= i_9
                }
            } else if (i_3_mod > 0) {
                seek()
                i_9 = kotlin.math.min(i_3_mod, readBufferValidBytes)

                System.arraycopy(readBuffer, 0, buffer, i_2_mod, i_9)
                i_2_mod += i_9
                i_3_mod -= i_9
                currentPosition += i_9
            }

            if (writeBufferFilePosition != -1L) {
                if (writeBufferFilePosition > currentPosition && i_3_mod > 0) {
                    i_9 = i_2_mod + (writeBufferFilePosition - currentPosition).toInt()
                    if (i_9 > i_3_mod + i_2_mod) {
                        i_9 = i_3_mod + i_2_mod
                    }

                    while (i_2_mod < i_9) {
                        buffer[i_2_mod++] = 0
                        --i_3_mod
                        ++currentPosition
                    }
                }

                var long_14 = -1L
                var long_11 = -1L
                if (writeBufferFilePosition >= long_5 && writeBufferFilePosition < i_8 + long_5) {
                    long_14 = writeBufferFilePosition
                } else if (long_5 >= writeBufferFilePosition && long_5 < (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition) {
                    long_14 = long_5
                }

                if ((obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition > long_5 && (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition <= i_8 + long_5) {
                    long_11 = (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition
                } else if (i_8 + long_5 > writeBufferFilePosition && long_5 + i_8 <= writeBufferFilePosition + (obfuscatedWriteBufferSize * 675653231L)) {
                    long_11 = i_8 + long_5
                }

                if (long_14 > -1L && long_11 > long_14) {
                    val i_13 = (long_11 - long_14).toInt()
                    System.arraycopy(writeBuffer, (long_14 - writeBufferFilePosition).toInt(), buffer, (long_14 - long_5).toInt() + i_2_mod, i_13)
                    if (long_11 > currentPosition) {
                        i_3_mod = (i_3_mod - (long_11 - currentPosition)).toInt()
                        currentPosition = long_11
                    }
                }
            }
        } catch (ioexception17: IOException) {
            fileChannelPosition = -1L
            throw ioexception17
        }

        if (i_3_mod > 0) {
            throw EOFException()
        }
    }

    @Throws(IOException::class)
    fun load(bytes_1: ByteArray) {
        load0(bytes_1, 0, bytes_1.size)
    }

    @Throws(IOException::class)
    fun write(bytes_1: ByteArray, i_2: Int, i_3: Int) {
        try {
            var i_2_mod = i_2
            var i_3_mod = i_3
            if (i_3_mod + currentPosition > initialCapacity) {
                initialCapacity = currentPosition + i_3_mod
            }

            if (writeBufferFilePosition != -1L && (currentPosition < writeBufferFilePosition || currentPosition > (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition)) {
                flushWriteBuffer()
            }

            if (writeBufferFilePosition != -1L && currentPosition + i_3_mod > writeBufferFilePosition + writeBuffer.size) {
                val i_5 = (writeBuffer.size - (currentPosition - writeBufferFilePosition)).toInt()
                System.arraycopy(bytes_1, i_2_mod, writeBuffer, (currentPosition - writeBufferFilePosition).toInt(), i_5)
                currentPosition += i_5
                i_2_mod += i_5
                i_3_mod -= i_5
                obfuscatedWriteBufferSize = writeBuffer.size * -1070827377
                flushWriteBuffer()
            }

            if (i_3_mod > writeBuffer.size) {
                if (currentPosition != fileChannelPosition) {
                    fs32.seek(currentPosition)
                    fileChannelPosition = currentPosition
                }

                fs32.write(bytes_1, i_2_mod, i_3_mod)
                fileChannelPosition += i_3_mod
                if (fileChannelPosition > occupiedSize) {
                    occupiedSize = fileChannelPosition
                }

                var long_10 = -1L
                var long_7 = -1L
                if (currentPosition >= readBufferFilePosition && currentPosition < readBufferFilePosition + readBufferValidBytes) {
                    long_10 = currentPosition
                } else if (readBufferFilePosition >= currentPosition && readBufferFilePosition < i_3_mod + currentPosition) {
                    long_10 = readBufferFilePosition
                }

                if (i_3_mod + currentPosition > readBufferFilePosition && currentPosition + i_3_mod <= readBufferFilePosition + readBufferValidBytes) {
                    long_7 = i_3_mod + currentPosition
                } else if (readBufferFilePosition + readBufferValidBytes > currentPosition && readBufferFilePosition + readBufferValidBytes <= currentPosition + i_3_mod) {
                    long_7 = readBufferValidBytes + readBufferFilePosition
                }

                if (long_10 > -1L && long_7 > long_10) {
                    val i_9 = (long_7 - long_10).toInt()
                    System.arraycopy(bytes_1, (long_10 + i_2_mod - currentPosition).toInt(), readBuffer, (long_10 - readBufferFilePosition).toInt(), i_9)
                }

                currentPosition += i_3_mod
            } else if (i_3_mod > 0) {
                if (writeBufferFilePosition == -1L) {
                    writeBufferFilePosition = currentPosition
                }

                System.arraycopy(bytes_1, i_2_mod, writeBuffer, (currentPosition - writeBufferFilePosition).toInt(), i_3_mod)
                currentPosition += i_3_mod
                if (currentPosition - writeBufferFilePosition > (obfuscatedWriteBufferSize * 675653231L)) {
                    obfuscatedWriteBufferSize = ((currentPosition - writeBufferFilePosition).toInt() * -1070827377)
                }
            }

        } catch (ioexception13: IOException) {
            fileChannelPosition = -1L
            throw ioexception13
        }
    }

    @Throws(IOException::class)
    fun flushWriteBuffer() {
        if (writeBufferFilePosition != -1L) {
            if (writeBufferFilePosition != fileChannelPosition) {
                fs32.seek(writeBufferFilePosition)
                fileChannelPosition = writeBufferFilePosition
            }
            fs32.write(writeBuffer, 0, obfuscatedWriteBufferSize * 675653231)
            fileChannelPosition += 675653231L * obfuscatedWriteBufferSize
            if (fileChannelPosition > occupiedSize) {
                occupiedSize = fileChannelPosition
            }
            var long_2 = -1L
            var long_4 = -1L
            if (writeBufferFilePosition >= readBufferFilePosition && writeBufferFilePosition < readBufferValidBytes + readBufferFilePosition) {
                long_2 = writeBufferFilePosition
            } else if (readBufferFilePosition >= writeBufferFilePosition && readBufferFilePosition < writeBufferFilePosition + (obfuscatedWriteBufferSize * 675653231L)) {
                long_2 = readBufferFilePosition
            }
            if ((obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition > readBufferFilePosition && (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition <= readBufferValidBytes + readBufferFilePosition) {
                long_4 = (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition
            } else if (readBufferFilePosition + readBufferValidBytes > writeBufferFilePosition && readBufferValidBytes + readBufferFilePosition <= writeBufferFilePosition + (obfuscatedWriteBufferSize * 675653231L)) {
                long_4 = readBufferFilePosition + readBufferValidBytes
            }
            if (long_2 > -1L && long_4 > long_2) {
                val i_6 = (long_4 - long_2).toInt()
                System.arraycopy(writeBuffer, (long_2 - writeBufferFilePosition).toInt(), readBuffer, (long_2 - readBufferFilePosition).toInt(), i_6)
            }
            writeBufferFilePosition = -1L
            obfuscatedWriteBufferSize = 0
        }
    }

    @Throws(IOException::class)
    fun seek() {
        readBufferValidBytes = 0
        if (currentPosition != fileChannelPosition) {
            fs32.seek(currentPosition)
            fileChannelPosition = currentPosition
        }
        var offset: Int
        readBufferFilePosition = currentPosition
        while (readBufferValidBytes < readBuffer.size) {
            var length = readBuffer.size - readBufferValidBytes
            if (length > 200000000) {
                length = 200000000
            }
            offset = fs32.getSeekOffset(readBuffer, readBufferValidBytes, length)
            if (offset == -1) {
                break
            }
            fileChannelPosition += offset
            readBufferValidBytes += offset
        }
    }

    companion object {
        @JvmField
        var anInt5357: Int = 0

        @JvmField
        var cacheFileIndices: Array<FileOnDisk?>? = null
    }
}
