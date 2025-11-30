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
                var part = 0
                var i_10 = 0
                while (true) {
                    if (part < size) {
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
                            if (currentIndex != file || currentPart != i_10 || currentFile != this.index) {
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
                        if (size - part <= 512) {
                            nextSector = 0
                        }
                        if (file > 65535) {
                            buffer[0] = (file shr 24).toByte()
                            buffer[1] = (file shr 16).toByte()
                            buffer[2] = (file shr 8).toByte()
                            buffer[3] = file.toByte()
                            buffer[4] = (i_10 shr 8).toByte()
                            buffer[5] = i_10.toByte()
                            buffer[6] = (nextSector shr 16).toByte()
                            buffer[7] = (nextSector shr 8).toByte()
                            buffer[8] = nextSector.toByte()
                            buffer[9] = this.index.toByte()
                            this.dataFile.setPosition(sector * 520L)
                            this.dataFile.write(buffer, 0, 10)
                            currentIndex = size - part
                            if (currentIndex > 510) {
                                currentIndex = 510
                            }
                        } else {
                            buffer[0] = (file shr 8).toByte()
                            buffer[1] = file.toByte()
                            buffer[2] = (i_10 shr 8).toByte()
                            buffer[3] = i_10.toByte()
                            buffer[4] = (nextSector shr 16).toByte()
                            buffer[5] = (nextSector shr 8).toByte()
                            buffer[6] = nextSector.toByte()
                            buffer[7] = this.index.toByte()
                            this.dataFile.setPosition(sector * 520L)
                            this.dataFile.write(buffer, 0, 8)
                            currentIndex = size - part
                            if (currentIndex > 512) {
                                currentIndex = 512
                            }
                        }
                        this.dataFile.write(data, part, currentIndex)
                        part += currentIndex
                        sector = nextSector
                        i_10++
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

    fun decompress(i_1: Int): ByteArray? {
        synchronized(this.dataFile) {
            try {
                if (this.idxFile.getInitialSize() < (i_1 * 6L + 6)) {
                    return null
                }
                this.idxFile.setPosition(i_1 * 6L)
                this.idxFile.load0(buffer, 0, 6)
                val i_18 = ((buffer[0].toInt() and 0xff) shl 16) + (buffer[2].toInt() and 0xff) + ((buffer[1].toInt() and 0xff) shl 8)
                var i_6 = (buffer[5].toInt() and 0xff) + ((buffer[3].toInt() and 0xff) shl 16) + ((buffer[4].toInt() and 0xff) shl 8)
                if (i_18 > this.sizeCap) {
                    return null
                }
                if (i_6 > 0 && i_6 <= this.dataFile.getInitialSize() / 520L) {
                    val bytes_24 = ByteArray(i_18)
                    var i_8 = 0
                    var i_9 = 0
                    while (i_8 < i_18) {
                        if (i_6 == 0) {
                            return null
                        }
                        this.dataFile.setPosition(i_6 * 520L)
                        var i_19 = i_18 - i_8
                        val b_11: Byte
                        val i_12: Int
                        val i_13: Int
                        val i_14: Int
                        val i_15: Int
                        if (i_1 > 65535) {
                            if (i_19 > 510) {
                                i_19 = 510
                            }
                            b_11 = 10
                            this.dataFile.load0(buffer, 0, b_11 + i_19)
                            i_12 = ((buffer[1].toInt() and 0xff) shl 16) + ((buffer[0].toInt() and 0xff) shl 24) + (buffer[3].toInt() and 0xff) + ((buffer[2].toInt() and 0xff) shl 8)
                            i_13 = (buffer[5].toInt() and 0xff) + ((buffer[4].toInt() and 0xff) shl 8)
                            i_14 = (buffer[8].toInt() and 0xff) + ((buffer[7].toInt() and 0xff) shl 8) + ((buffer[6].toInt() and 0xff) shl 16)
                            i_15 = buffer[9].toInt() and 0xff
                        } else {
                            if (i_19 > 512) {
                                i_19 = 512
                            }
                            b_11 = 8
                            this.dataFile.load0(buffer, 0, b_11 + i_19)
                            i_12 = (buffer[1].toInt() and 0xff) + ((buffer[0].toInt() and 0xff) shl 8)
                            i_13 = (buffer[3].toInt() and 0xff) + ((buffer[2].toInt() and 0xff) shl 8)
                            i_14 = ((buffer[5].toInt() and 0xff) shl 8) + ((buffer[4].toInt() and 0xff) shl 16) + (buffer[6].toInt() and 0xff)
                            i_15 = buffer[7].toInt() and 0xff
                        }
                        if (i_12 == i_1 && i_13 == i_9 && i_15 == this.index) {
                            if (i_14 >= 0 && i_14 <= this.dataFile.getInitialSize() / 520L) {
                                val i_20 = i_19 + b_11
                                for (i_17 in b_11 until i_20) {
                                    bytes_24[i_8++] = buffer[i_17]
                                }
                                i_6 = i_14
                                ++i_9
                                continue
                            }
                            return null
                        }
                        return null
                    }
                    val bytes_4 = bytes_24
                    return bytes_4
                }
            } catch (ioexception22: IOException) {
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
