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
    fun load0(destBuffer: ByteArray, offset: Int, length: Int) {
        var destOffset = offset
        var remaining = length
        try {
            if (remaining + destOffset > destBuffer.size) {
                throw ArrayIndexOutOfBoundsException(remaining + destOffset - destBuffer.size)
            }

            if (writeBufferFilePosition != -1L && currentPosition >= writeBufferFilePosition && currentPosition + remaining <= (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition) {
                System.arraycopy(writeBuffer, (currentPosition - writeBufferFilePosition).toInt(), destBuffer, destOffset, remaining)
                currentPosition += remaining
                return
            }

            val savedPosition = currentPosition
            val requestedLength = remaining
            var bytesAvailable: Int
            if (currentPosition >= readBufferFilePosition && currentPosition < readBufferValidBytes + readBufferFilePosition) {
                bytesAvailable = (readBufferValidBytes - (currentPosition - readBufferFilePosition)).toInt()
                if (bytesAvailable > remaining) {
                    bytesAvailable = remaining
                }

                System.arraycopy(readBuffer, (currentPosition - readBufferFilePosition).toInt(), destBuffer, destOffset, bytesAvailable)
                currentPosition += bytesAvailable
                destOffset += bytesAvailable
                remaining -= bytesAvailable
            }

            if (remaining > readBuffer.size) {
                fs32.seek(currentPosition)

                fileChannelPosition = currentPosition
                while (remaining > 0) {
                    bytesAvailable = fs32.getSeekOffset(destBuffer, destOffset, remaining)
                    if (bytesAvailable == -1) {
                        break
                    }

                    fileChannelPosition += bytesAvailable
                    currentPosition += bytesAvailable
                    destOffset += bytesAvailable
                    remaining -= bytesAvailable
                }
            } else if (remaining > 0) {
                seek()
                bytesAvailable = kotlin.math.min(remaining, readBufferValidBytes)

                System.arraycopy(readBuffer, 0, destBuffer, destOffset, bytesAvailable)
                destOffset += bytesAvailable
                remaining -= bytesAvailable
                currentPosition += bytesAvailable
            }

            if (writeBufferFilePosition != -1L) {
                if (writeBufferFilePosition > currentPosition && remaining > 0) {
                    bytesAvailable = destOffset + (writeBufferFilePosition - currentPosition).toInt()
                    if (bytesAvailable > remaining + destOffset) {
                        bytesAvailable = remaining + destOffset
                    }

                    while (destOffset < bytesAvailable) {
                        destBuffer[destOffset++] = 0
                        --remaining
                        ++currentPosition
                    }
                }

                var overlapStart = -1L
                var overlapEnd = -1L
                if (writeBufferFilePosition >= savedPosition && writeBufferFilePosition < requestedLength + savedPosition) {
                    overlapStart = writeBufferFilePosition
                } else if (savedPosition >= writeBufferFilePosition && savedPosition < (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition) {
                    overlapStart = savedPosition
                }

                if ((obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition > savedPosition && (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition <= requestedLength + savedPosition) {
                    overlapEnd = (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition
                } else if (requestedLength + savedPosition > writeBufferFilePosition && savedPosition + requestedLength <= writeBufferFilePosition + (obfuscatedWriteBufferSize * 675653231L)) {
                    overlapEnd = requestedLength + savedPosition
                }

                if (overlapStart > -1L && overlapEnd > overlapStart) {
                    val overlapLength = (overlapEnd - overlapStart).toInt()
                    System.arraycopy(writeBuffer, (overlapStart - writeBufferFilePosition).toInt(), destBuffer, (overlapStart - savedPosition).toInt() + destOffset, overlapLength)
                    if (overlapEnd > currentPosition) {
                        remaining = (remaining - (overlapEnd - currentPosition)).toInt()
                        currentPosition = overlapEnd
                    }
                }
            }
        } catch (ioexception: IOException) {
            fileChannelPosition = -1L
            throw ioexception
        }

        if (remaining > 0) {
            throw EOFException()
        }
    }

    @Throws(IOException::class)
    fun load(data: ByteArray) {
        load0(data, 0, data.size)
    }

    @Throws(IOException::class)
    fun write(data: ByteArray, offset: Int, length: Int) {
        try {
            var srcOffset = offset
            var remaining = length
            if (remaining + currentPosition > initialCapacity) {
                initialCapacity = currentPosition + remaining
            }

            if (writeBufferFilePosition != -1L && (currentPosition < writeBufferFilePosition || currentPosition > (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition)) {
                flushWriteBuffer()
            }

            if (writeBufferFilePosition != -1L && currentPosition + remaining > writeBufferFilePosition + writeBuffer.size) {
                val spaceAvailable = (writeBuffer.size - (currentPosition - writeBufferFilePosition)).toInt()
                System.arraycopy(data, srcOffset, writeBuffer, (currentPosition - writeBufferFilePosition).toInt(), spaceAvailable)
                currentPosition += spaceAvailable
                srcOffset += spaceAvailable
                remaining -= spaceAvailable
                obfuscatedWriteBufferSize = writeBuffer.size * -1070827377
                flushWriteBuffer()
            }

            if (remaining > writeBuffer.size) {
                if (currentPosition != fileChannelPosition) {
                    fs32.seek(currentPosition)
                    fileChannelPosition = currentPosition
                }

                fs32.write(data, srcOffset, remaining)
                fileChannelPosition += remaining
                if (fileChannelPosition > occupiedSize) {
                    occupiedSize = fileChannelPosition
                }

                var overlapStart = -1L
                var overlapEnd = -1L
                if (currentPosition >= readBufferFilePosition && currentPosition < readBufferFilePosition + readBufferValidBytes) {
                    overlapStart = currentPosition
                } else if (readBufferFilePosition >= currentPosition && readBufferFilePosition < remaining + currentPosition) {
                    overlapStart = readBufferFilePosition
                }

                if (remaining + currentPosition > readBufferFilePosition && currentPosition + remaining <= readBufferFilePosition + readBufferValidBytes) {
                    overlapEnd = remaining + currentPosition
                } else if (readBufferFilePosition + readBufferValidBytes > currentPosition && readBufferFilePosition + readBufferValidBytes <= currentPosition + remaining) {
                    overlapEnd = readBufferValidBytes + readBufferFilePosition
                }

                if (overlapStart > -1L && overlapEnd > overlapStart) {
                    val overlapLength = (overlapEnd - overlapStart).toInt()
                    System.arraycopy(data, (overlapStart + srcOffset - currentPosition).toInt(), readBuffer, (overlapStart - readBufferFilePosition).toInt(), overlapLength)
                }

                currentPosition += remaining
            } else if (remaining > 0) {
                if (writeBufferFilePosition == -1L) {
                    writeBufferFilePosition = currentPosition
                }

                System.arraycopy(data, srcOffset, writeBuffer, (currentPosition - writeBufferFilePosition).toInt(), remaining)
                currentPosition += remaining
                if (currentPosition - writeBufferFilePosition > (obfuscatedWriteBufferSize * 675653231L)) {
                    obfuscatedWriteBufferSize = ((currentPosition - writeBufferFilePosition).toInt() * -1070827377)
                }
            }

        } catch (ioexception: IOException) {
            fileChannelPosition = -1L
            throw ioexception
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
            var overlapStart = -1L
            var overlapEnd = -1L
            if (writeBufferFilePosition >= readBufferFilePosition && writeBufferFilePosition < readBufferValidBytes + readBufferFilePosition) {
                overlapStart = writeBufferFilePosition
            } else if (readBufferFilePosition >= writeBufferFilePosition && readBufferFilePosition < writeBufferFilePosition + (obfuscatedWriteBufferSize * 675653231L)) {
                overlapStart = readBufferFilePosition
            }
            if ((obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition > readBufferFilePosition && (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition <= readBufferValidBytes + readBufferFilePosition) {
                overlapEnd = (obfuscatedWriteBufferSize * 675653231L) + writeBufferFilePosition
            } else if (readBufferFilePosition + readBufferValidBytes > writeBufferFilePosition && readBufferValidBytes + readBufferFilePosition <= writeBufferFilePosition + (obfuscatedWriteBufferSize * 675653231L)) {
                overlapEnd = readBufferFilePosition + readBufferValidBytes
            }
            if (overlapStart > -1L && overlapEnd > overlapStart) {
                val overlapLength = (overlapEnd - overlapStart).toInt()
                System.arraycopy(writeBuffer, (overlapStart - writeBufferFilePosition).toInt(), readBuffer, (overlapStart - readBufferFilePosition).toInt(), overlapLength)
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
        var bytesRead: Int
        readBufferFilePosition = currentPosition
        while (readBufferValidBytes < readBuffer.size) {
            var readLength = readBuffer.size - readBufferValidBytes
            if (readLength > 200000000) {
                readLength = 200000000
            }
            bytesRead = fs32.getSeekOffset(readBuffer, readBufferValidBytes, readLength)
            if (bytesRead == -1) {
                break
            }
            fileChannelPosition += bytesRead
            readBufferValidBytes += bytesRead
        }
    }

    companion object {
        @JvmField
        var lobbyGraceExpiry: Int = 0

        @JvmField
        var cacheFileIndices: Array<FileOnDisk?>? = null
    }
}
