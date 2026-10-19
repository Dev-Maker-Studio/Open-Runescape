package com.jagex.game.runetek5.utils

object Base64 {

    @JvmField val ENCODE_TABLE = CharArray(64)
    @JvmField val URL_SAFE_ENCODE_TABLE: CharArray
    @JvmField val DECODE_TABLE: IntArray

    init {
        for (i in 0..25) {
            ENCODE_TABLE[i] = (i + 65).toChar()
        }
        for (i in 26..51) {
            ENCODE_TABLE[i] = (i + 97 - 26).toChar()
        }
        for (i in 52..61) {
            ENCODE_TABLE[i] = (i + 48 - 52).toChar()
        }
        ENCODE_TABLE[62] = 43.toChar()
        ENCODE_TABLE[63] = 47.toChar()
        URL_SAFE_ENCODE_TABLE = CharArray(64)
        for (i in 0..25) {
            URL_SAFE_ENCODE_TABLE[i] = (i + 65).toChar()
        }
        for (i in 26..51) {
            URL_SAFE_ENCODE_TABLE[i] = (i + 97 - 26).toChar()
        }
        for (i in 52..61) {
            URL_SAFE_ENCODE_TABLE[i] = (i + 48 - 52).toChar()
        }
        URL_SAFE_ENCODE_TABLE[62] = 42.toChar()
        URL_SAFE_ENCODE_TABLE[63] = 45.toChar()
        DECODE_TABLE = IntArray(128)
        for (i in DECODE_TABLE.indices) {
            DECODE_TABLE[i] = -1
        }
        for (i in 65..90) {
            DECODE_TABLE[i] = i - 65
        }
        for (i in 97..122) {
            DECODE_TABLE[i] = i - 97 + 26
        }
        for (i in 48..57) {
            DECODE_TABLE[i] = i - 48 + 52
        }
        DECODE_TABLE[43] = 62
        DECODE_TABLE[42] = 62
        DECODE_TABLE[47] = 63
        DECODE_TABLE[45] = 63
    }

    @JvmStatic
    fun decode(input: String): ByteArray {
        val inputLength = input.length
        if (inputLength == 0) {
            return ByteArray(0)
        }
        val paddedLength = inputLength + 3 and -4
        var outputLength = paddedLength / 4 * 3
        if (paddedLength - 2 < inputLength && getDecodedValue(input[paddedLength - 2]) != -1) {
            if (paddedLength - 1 >= inputLength || getDecodedValue(input[paddedLength - 1]) == -1) {
                outputLength--
            }
        } else {
            outputLength -= 2
        }
        val output = ByteArray(outputLength)
        decode(input, output, 0)
        return output
    }

    @JvmStatic
    fun decode(input: String, output: ByteArray, offset: Int): Int {
        val startOffset = offset
        val inputLength = input.length
        var currentOffset = offset
        var i = 0
        while (i < inputLength) {
            val val1 = getDecodedValue(input[i])
            val val2 = if (i + 1 < inputLength) getDecodedValue(input[i + 1]) else -1
            val val3 = if (i + 2 < inputLength) getDecodedValue(input[i + 2]) else -1
            val val4 = if (i + 3 < inputLength) getDecodedValue(input[i + 3]) else -1
            output[currentOffset++] = (val1 shl 2 or (val2 ushr 4)).toByte()
            if (val3 == -1) {
                break
            }
            output[currentOffset++] = ((val2 and 0xf) shl 4 or (val3 ushr 2)).toByte()
            if (val4 == -1) {
                break
            }
            output[currentOffset++] = ((val3 and 0x3) shl 6 or val4).toByte()
            i += 4
        }
        return currentOffset - startOffset
    }

    @JvmStatic
    fun getDecodedValue(c: Char): Int {
        return if (c.code < DECODE_TABLE.size) DECODE_TABLE[c.code] else -1
    }
}
