package com.jagex.game.runetek5.tools

object Hashing {

    @JvmField val CRC64_TABLE: LongArray = LongArray(256)
    @JvmField val CRC32_TABLE: IntArray = IntArray(256)

    init {
        for (index in 0 until 256) {
            var hash = index
            for (subIndex in 0 until 8) {
                if ((hash and 0x1) == 1) {
                    hash = hash ushr 1 xor -306674912
                } else {
                    hash = hash ushr 1
                }
            }
            CRC32_TABLE[index] = hash
        }
        for (index in 0 until 256) {
            var hash = index.toLong()
            for (subIndex in 0 until 8) {
                if ((hash and 0x1L) == 1L) {
                    hash = hash ushr 1 xor -3932672073523589310L
                } else {
                    hash = hash ushr 1
                }
            }
            CRC64_TABLE[index] = hash
        }
    }

    @JvmStatic
    fun getCrc32(data: ByteArray, start: Int, end: Int): Int {
        var crc32 = -1
        for (i in start until end) {
            crc32 = crc32 ushr 8 xor CRC32_TABLE[(crc32 xor data[i].toInt()) and 0xff]
        }
        crc32 = crc32.inv()
        return crc32
    }

    @JvmStatic
    fun getCrc32(data: ByteArray, length: Int): Int {
        return getCrc32(data, 0, length)
    }
}
