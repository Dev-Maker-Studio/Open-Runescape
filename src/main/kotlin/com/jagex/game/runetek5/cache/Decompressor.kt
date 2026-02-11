package com.jagex.game.runetek5.cache

import java.io.EOFException
import java.io.IOException

class Decompressor(
    @JvmField val index: Int,
    @JvmField val dataFile: FileOnDisk,
    @JvmField val idxFile: FileOnDisk
) {

    @JvmField var sizeCap: Int = 65000

    init {
        this.sizeCap = 1500000
    }

    fun put(file: Int, data: ByteArray, size: Int): Boolean {
        synchronized(this.dataFile) {
            if (size >= 0 && size <= this.sizeCap) {
                var successful = put(file, data, size, true)
                if (!successful) {
                    successful = put(file, data, size, false)
                }
                return successful
            }
            throw IllegalArgumentException()
        }
    }

    fun put(file: Int, data: ByteArray, size: Int, seek: Boolean): Boolean {
        synchronized(this.dataFile) {
            try {
                var sector: Int
                var mutableSeek = seek
                if (mutableSeek) {
                    if (this.idxFile.getInitialSize() < (file * 6L + 6)) {
                        return false
                    }
                    this.idxFile.setPosition(file * 6L)
                    this.idxFile.load0(buffer, 0, 6)
                    sector = (buffer[5].toInt() and 0xff) + ((buffer[3].toInt() and 0xff) shl 16) + ((buffer[4].toInt() and 0xff) shl 8)
                    if (sector <= 0 || sector > this.dataFile.getInitialSize() / 520L) {
                        return false
                    }
                } else {
                    sector = ((this.dataFile.getInitialSize() + 519L) / 520L).toInt()
                    if (sector == 0) {
                        sector = 1
                    }
                }
                buffer[0] = (size shr 16).toByte()
                buffer[1] = (size shr 8).toByte()
                buffer[2] = size.toByte()
                buffer[3] = (sector shr 16).toByte()
                buffer[4] = (sector shr 8).toByte()
                buffer[5] = sector.toByte()
                this.idxFile.setPosition(file * 6L)
                this.idxFile.write(buffer, 0, 6)
                var bytesWritten = 0
                var chunkIndex = 0
                while (true) {
                    if (bytesWritten < size) {
                        var nextSector = 0
                        var currentIndex: Int
                        if (mutableSeek) {
                            this.dataFile.setPosition(sector * 520L)
                            val currentPart: Int
                            val currentFile: Int
                            if (file > 65535) {
                                try {
                                    this.dataFile.load0(buffer, 0, 10)
                                } catch (eofexception: EOFException) {
                                    break
                                }
                                currentIndex = ((buffer[1].toInt() and 0xff) shl 16) + ((buffer[0].toInt() and 0xff) shl 24) + (buffer[3].toInt() and 0xff) + ((buffer[2].toInt() and 0xff) shl 8)
                                currentPart = (buffer[5].toInt() and 0xff) + ((buffer[4].toInt() and 0xff) shl 8)
                                nextSector = (buffer[8].toInt() and 0xff) + ((buffer[7].toInt() and 0xff) shl 8) + ((buffer[6].toInt() and 0xff) shl 16)
                                currentFile = buffer[9].toInt() and 0xff
                            } else {
                                try {
                                    this.dataFile.load0(buffer, 0, 8)
                                } catch (eofexception: EOFException) {
                                    break
                                }
                                currentIndex = (buffer[1].toInt() and 0xff) + ((buffer[0].toInt() and 0xff) shl 8)
                                currentPart = (buffer[3].toInt() and 0xff) + ((buffer[2].toInt() and 0xff) shl 8)
                                nextSector = ((buffer[5].toInt() and 0xff) shl 8) + ((buffer[4].toInt() and 0xff) shl 16) + (buffer[6].toInt() and 0xff)
                                currentFile = buffer[7].toInt() and 0xff
                            }
                            if (currentIndex != file || currentPart != chunkIndex || currentFile != this.index) {
                                return false
                            }
                            if (nextSector < 0 || nextSector > this.dataFile.getInitialSize() / 520L) {
                                return false
                            }
                        }
                        if (nextSector == 0) {
                            mutableSeek = false
                            nextSector = ((this.dataFile.getInitialSize() + 519L) / 520L).toInt()
                            if (nextSector == 0) {
                                nextSector++
                            }
                            if (sector == nextSector) {
                                nextSector++
                            }
                        }
                        if (size - bytesWritten <= 512) {
                            nextSector = 0
                        }
                        if (file > 65535) {
                            buffer[0] = (file shr 24).toByte()
                            buffer[1] = (file shr 16).toByte()
                            buffer[2] = (file shr 8).toByte()
                            buffer[3] = file.toByte()
                            buffer[4] = (chunkIndex shr 8).toByte()
                            buffer[5] = chunkIndex.toByte()
                            buffer[6] = (nextSector shr 16).toByte()
                            buffer[7] = (nextSector shr 8).toByte()
                            buffer[8] = nextSector.toByte()
                            buffer[9] = this.index.toByte()
                            this.dataFile.setPosition(sector * 520L)
                            this.dataFile.write(buffer, 0, 10)
                            currentIndex = size - bytesWritten
                            if (currentIndex > 510) {
                                currentIndex = 510
                            }
                        } else {
                            buffer[0] = (file shr 8).toByte()
                            buffer[1] = file.toByte()
                            buffer[2] = (chunkIndex shr 8).toByte()
                            buffer[3] = chunkIndex.toByte()
                            buffer[4] = (nextSector shr 16).toByte()
                            buffer[5] = (nextSector shr 8).toByte()
                            buffer[6] = nextSector.toByte()
                            buffer[7] = this.index.toByte()
                            this.dataFile.setPosition(sector * 520L)
                            this.dataFile.write(buffer, 0, 8)
                            currentIndex = size - bytesWritten
                            if (currentIndex > 512) {
                                currentIndex = 512
                            }
                        }
                        this.dataFile.write(data, bytesWritten, currentIndex)
                        bytesWritten += currentIndex
                        sector = nextSector
                        chunkIndex++
                        continue
                    }
                    return true
                }
            } catch (ioexception: IOException) {
                return false
            }
            return false
        }
    }

    fun decompress(fileId: Int): ByteArray? {
        synchronized(this.dataFile) {
            try {
                if (this.idxFile.getInitialSize() < (fileId * 6L + 6)) {
                    return null
                }
                this.idxFile.setPosition(fileId * 6L)
                this.idxFile.load0(buffer, 0, 6)
                val dataSize = ((buffer[0].toInt() and 0xff) shl 16) + (buffer[2].toInt() and 0xff) + ((buffer[1].toInt() and 0xff) shl 8)
                var currentSector = (buffer[5].toInt() and 0xff) + ((buffer[3].toInt() and 0xff) shl 16) + ((buffer[4].toInt() and 0xff) shl 8)
                if (dataSize > this.sizeCap) {
                    return null
                }
                if (currentSector > 0 && currentSector <= this.dataFile.getInitialSize() / 520L) {
                    val outputData = ByteArray(dataSize)
                    var bytesRead = 0
                    var chunkIndex = 0
                    while (bytesRead < dataSize) {
                        if (currentSector == 0) {
                            return null
                        }
                        this.dataFile.setPosition(currentSector * 520L)
                        var chunkSize = dataSize - bytesRead
                        val headerSize: Byte
                        val storedFileId: Int
                        val storedChunkIndex: Int
                        val nextSector: Int
                        val storedIndex: Int
                        if (fileId > 65535) {
                            if (chunkSize > 510) {
                                chunkSize = 510
                            }
                            headerSize = 10
                            this.dataFile.load0(buffer, 0, headerSize + chunkSize)
                            storedFileId = ((buffer[1].toInt() and 0xff) shl 16) + ((buffer[0].toInt() and 0xff) shl 24) + (buffer[3].toInt() and 0xff) + ((buffer[2].toInt() and 0xff) shl 8)
                            storedChunkIndex = (buffer[5].toInt() and 0xff) + ((buffer[4].toInt() and 0xff) shl 8)
                            nextSector = (buffer[8].toInt() and 0xff) + ((buffer[7].toInt() and 0xff) shl 8) + ((buffer[6].toInt() and 0xff) shl 16)
                            storedIndex = buffer[9].toInt() and 0xff
                        } else {
                            if (chunkSize > 512) {
                                chunkSize = 512
                            }
                            headerSize = 8
                            this.dataFile.load0(buffer, 0, headerSize + chunkSize)
                            storedFileId = (buffer[1].toInt() and 0xff) + ((buffer[0].toInt() and 0xff) shl 8)
                            storedChunkIndex = (buffer[3].toInt() and 0xff) + ((buffer[2].toInt() and 0xff) shl 8)
                            nextSector = ((buffer[5].toInt() and 0xff) shl 8) + ((buffer[4].toInt() and 0xff) shl 16) + (buffer[6].toInt() and 0xff)
                            storedIndex = buffer[7].toInt() and 0xff
                        }
                        if (storedFileId == fileId && storedChunkIndex == chunkIndex && storedIndex == this.index) {
                            if (nextSector >= 0 && nextSector <= this.dataFile.getInitialSize() / 520L) {
                                val totalReadSize = chunkSize + headerSize
                                for (bufferOffset in headerSize until totalReadSize) {
                                    outputData[bytesRead++] = buffer[bufferOffset]
                                }
                                currentSector = nextSector
                                ++chunkIndex
                                continue
                            }
                            return null
                        }
                        return null
                    }
                    return outputData
                }
            } catch (ioexception: IOException) {
                return null
            }
            return null
        }
    }

    override fun toString(): String {
        return "" + this.index
    }

    companion object {
        @JvmField val buffer: ByteArray = ByteArray(520)
    }
}
