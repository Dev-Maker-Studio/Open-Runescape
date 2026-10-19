package com.jagex.game.runetek5.buffer

import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.tools.Hashing
import com.jagex.game.runetek5.tools.Isaac
import com.jagex.game.runetek5.utils.Utils
import java.math.BigInteger

open class Packet : Node<Packet> {
    @JvmField var buffer: ByteArray
    @JvmField var currentPosition: Int

    constructor() {
        buffer = ByteArray(0)
        currentPosition = 0
    }

    constructor(bytes: ByteArray) {
        buffer = bytes
        currentPosition = 0
    }

    constructor(size: Int) {
        buffer = CircularBuffer.createBuffer(size)
        currentPosition = 0
    }

    fun flush() {
        buffer.let { CircularBuffer.stash(it) }
        buffer = ByteArray(0)
    }

    fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
        for (i in offset until offset + length) {
            buffer[currentPosition++] = bytes[i]
        }
    }

    fun readBytes(bytes: ByteArray, offset: Int, length: Int) {
        for (i in offset until offset + length) {
            bytes[i] = buffer[currentPosition++]
        }
    }

    fun writeByte(value: Int) {
        buffer[currentPosition++] = value.toByte()
    }

    fun writeByteInverse(value: Int) = writeByte(-value)

    fun writeByteAdd(value: Int) = writeByte(value + 128)

    fun writeByteSubtract(value: Int) = writeByte(128 - value)

    fun writeShort(value: Int) {
        writeByte(value shr 8)
        writeByte(value)
    }

    fun writeShortAdd(value: Int) {
        writeByte(value shr 8)
        writeByteAdd(value)
    }

    fun writeShortLittle(value: Int) {
        writeByte(value)
        writeByte(value shr 8)
    }

    fun writeShortAddLittle(value: Int) {
        writeByteAdd(value)
        writeByte(value shr 8)
    }

    fun writeMedium(value: Int) {
        writeByte(value shr 16)
        writeByte(value shr 8)
        writeByte(value)
    }

    fun writeInt(value: Int) {
        writeByte(value shr 24)
        writeByte(value shr 16)
        writeByte(value shr 8)
        writeByte(value)
    }

    fun writeIntLittle(value: Int) {
        writeByte(value)
        writeByte(value shr 8)
        writeByte(value shr 16)
        writeByte(value shr 24)
    }

    fun writeIntMiddle(value: Int) {
        writeByte(value shr 8)
        writeByte(value)
        writeByte(value shr 24)
        writeByte(value shr 16)
    }

    fun writeIntInverseMiddle(value: Int) {
        writeByte(value shr 16)
        writeByte(value shr 24)
        writeByte(value)
        writeByte(value shr 8)
    }

    fun write40BitLong(value: Long) {
        writeByte((value shr 32).toInt())
        writeInt(value.toInt())
    }

    fun writeLong(value: Long) {
        writeByte((value shr 56).toInt())
        writeByte((value shr 48).toInt())
        writeByte((value shr 40).toInt())
        writeByte((value shr 32).toInt())
        writeByte((value shr 24).toInt())
        writeByte((value shr 16).toInt())
        writeByte((value shr 8).toInt())
        writeByte(value.toInt())
    }

    fun writeVariableLengthLong(value: Long, size: Int) {
        val adjustedSize = size - 1
        require(adjustedSize in 0..7) { "Size must be between 1 and 8" }
        for (shift in adjustedSize * 8 downTo 0 step 8) {
            writeByte((value shr shift).toInt())
        }
    }

    fun writeString(string: String) {
        val nullPos = string.indexOf(0.toChar())
        require(nullPos < 0) { "String contains null character" }
        currentPosition += writeString(string, string.length, buffer, currentPosition)
        writeByte(0)
    }

    fun writeJagString(string: String) {
        val nullPos = string.indexOf(0.toChar())
        require(nullPos < 0) { "String contains null character" }
        writeByte(0)
        currentPosition += writeString(string, string.length, buffer, currentPosition)
        writeByte(0)
    }

    fun writePathReference(chars: CharSequence) {
        val length = chars.length
        var size = 0
        for (i in 0 until length) {
            when (chars[i]) {
                in '\u0000'..'\u007F' -> size++
                in '\u0000'..'\u07FF' -> size += 2
                else -> size += 3
            }
        }

        writeByte(0)
        writeFlags(size)
        currentPosition += calculateRequiredSize(buffer, currentPosition, chars)
    }

    fun writeFlags(flags: Int) {
        if (flags and -128 != 0) {
            if (flags and -16384 != 0) {
                if (flags and -2097152 != 0) {
                    if (flags and -268435456 != 0) {
                        writeByte(flags ushr 28 or 0x80)
                    }
                    writeByte(flags ushr 21 or 0x80)
                }
                writeByte(flags ushr 14 or 0x80)
            }
            writeByte(flags ushr 7 or 0x80)
        }
        writeByte(flags and 0x7f)
    }

    fun writeFlagsMaybeBetter(flags: Int) {
        if (flags and 0xF0000000.toInt() != 0) writeByte((flags ushr 28) or 0x80)
        if (flags and 0xFFE00000.toInt() != 0) writeByte((flags ushr 21) or 0x80)
        if (flags and 0xFFFFC000.toInt() != 0) writeByte((flags ushr 14) or 0x80)
        if (flags and 0xFFFFFF80.toInt() != 0) writeByte((flags ushr 7) or 0x80)
        writeByte(flags and 0x7F)
    }

    fun writeSmart(data: Int) {
        when (data) {
            in 0 until 128 -> writeByte(data)
            in 0 until 32768 -> writeShort(data + 32768)
            else -> throw IllegalArgumentException("Smart value out of range: $data")
        }
    }

    fun readByte(): Byte = buffer[currentPosition++]

    fun readByteInverse(): Byte = (-readByte()).toByte()

    fun readByteAdd(): Byte = (readByte() - 128).toByte()

    fun readByteSubtract(): Byte = (128 - readByte()).toByte()

    fun readUByte(): Int = readByte().toInt() and 0xff

    fun readUByteInverse(): Int = -readByte() and 0xff

    fun readUByteAdd(): Int = (readByte() - 128) and 0xff

    fun readUByteSubtract(): Int = (128 - readByte()) and 0xff

    fun readShort(): Int {
        var value = ((readByte().toInt() and 0xff) shl 8) or (readByte().toInt() and 0xff)
        if (value > 32767) value -= 65536
        return value
    }

    fun readShortAdd(): Int {
        var value = ((readByte().toInt() and 0xff) shl 8) or ((readByte() - 128) and 0xff)
        if (value > 32767) value -= 65536
        return value
    }

    fun readShortLittle(): Int {
        var value = (readByte().toInt() and 0xff) or ((readByte().toInt() and 0xff) shl 8)
        if (value > 32767) value -= 65536
        return value
    }

    fun readShortAddLittle(): Int {
        var value = ((readByte() - 128) and 0xff) or ((readByte().toInt() and 0xff) shl 8)
        if (value > 32767) value -= 65536
        return value
    }

    fun readUShort(): Int = ((readByte().toInt() and 0xff) shl 8) or (readByte().toInt() and 0xff)

    fun readUShortAdd(): Int = ((readByte().toInt() and 0xff) shl 8) or ((readByte() - 128) and 0xff)

    fun readUShortLittle(): Int = (readByte().toInt() and 0xff) or ((readByte().toInt() and 0xff) shl 8)

    fun readUShortAddLittle(): Int = ((readByte() - 128) and 0xff) or ((readByte().toInt() and 0xff) shl 8)

    fun readMedium(): Int {
        var value = ((readByte().toInt() and 0xff) shl 16) or ((readByte().toInt() and 0xff) shl 8) or (readByte().toInt() and 0xff)
        if (value > 8388607) value -= 16777216
        return value
    }

    fun readUMedium(): Int =
        ((readByte().toInt() and 0xff) shl 16) or ((readByte().toInt() and 0xff) shl 8) or (readByte().toInt() and 0xff)

    fun readUMediumAlternateOrder(): Int =
        ((readByte().toInt() and 0xff) shl 16) or (readByte().toInt() and 0xff) or ((readByte().toInt() and 0xff) shl 8)

    fun readInt(): Int =
        ((readByte().toInt() and 0xff) shl 24) or ((readByte().toInt() and 0xff) shl 16) or
                ((readByte().toInt() and 0xff) shl 8) or (readByte().toInt() and 0xff)

    fun readUIntLittle(): Int =
        (readByte().toInt() and 0xff) or ((readByte().toInt() and 0xff) shl 8) or
                ((readByte().toInt() and 0xff) shl 16) or ((readByte().toInt() and 0xff) shl 24)

    fun readUIntMiddle(): Int =
        ((readByte().toInt() and 0xff) shl 8) or (readByte().toInt() and 0xff) or
                ((readByte().toInt() and 0xff) shl 24) or ((readByte().toInt() and 0xff) shl 16)

    fun readUIntInverseMiddle(): Int =
        ((readByte().toInt() and 0xff) shl 16) or ((readByte().toInt() and 0xff) shl 24) or
                (readByte().toInt() and 0xff) or ((readByte().toInt() and 0xff) shl 8)

    fun readLong(): Long {
        val high = readInt().toLong() and 0xffffffffL
        val low = readInt().toLong() and 0xffffffffL
        return (high shl 32) or low
    }

    fun read40BitLong(): Long {
        val high = readUByte().toLong() and 0xffffffffL
        val low = readInt().toLong() and 0xffffffffL
        return (high shl 32) or low
    }

    fun readULongLittle(): Long {
        val low = readUIntLittle().toLong() and 0xffffffffL
        val high = readUIntLittle().toLong() and 0xffffffffL
        return low + (high shl 32)
    }

    fun bitpackLong(size: Int): Long {
        val adjustedSize = size - 1
        require(adjustedSize in 0..7) { "Size must be between 1 and 8" }
        var shift = adjustedSize * 8
        var value = 0L
        while (shift >= 0) {
            value = value or ((readByte().toLong() and 0xffL) shl shift)
            shift -= 8
        }
        return value
    }

    fun readString(): String {
        val start = currentPosition
        while (readByte() != 0.toByte()) {}
        val length = currentPosition - start - 1
        return if (length == 0) "" else Utils.readString(buffer, start, length)
    }

    fun readNullString(): String? {
        if (buffer[currentPosition] == 0.toByte()) {
            currentPosition++
            return null
        }
        return readString()
    }

    fun readJagString(): String {
        val firstByte = readByte()
        require(firstByte == 0.toByte()) { "First byte in JagString must be 0 but was $firstByte" }
        val start = currentPosition
        while (readByte() != 0.toByte()) {}
        val length = currentPosition - start - 1
        return if (length == 0) "" else Utils.readString(buffer, start, length)
    }

    fun readPathReference(): String {
        val firstByte = readByte()
        require(firstByte == 0.toByte()) { "First byte must be 0" }
        val length = readVarInt()
        require(length + currentPosition <= buffer.size) { "String too long" }
        val result = StaticMethods.decodeUtf8(buffer, currentPosition, length)
        currentPosition += length
        return result
    }

    fun readSmart(): Int {
        val first = buffer[currentPosition].toInt() and 0xff
        return if (first < 128) readUByte() - 64 else readUShort() - 49152
    }

    fun readUSmart(): Int {
        val first = buffer[currentPosition].toInt() and 0xff
        return if (first < 128) readUByte() else readUShort() - 32768
    }

    fun readBigSmart(): Int {
        if (buffer[currentPosition] < 0) {
            return readInt() and 0x7fffffff
        }
        val value = readUShort()
        return if (value == 32767) -1 else value
    }

    fun readUBigSmart(): Int =
        if (buffer[currentPosition] < 0) readInt() and 0x7fffffff else readUShort()

    fun readSum(): Int {
        var sum = 0
        var value: Int
        do {
            value = readUSmart()
            sum += value
        } while (value == 32767)
        return sum
    }

    fun readVarInt(): Int {
        var value = 0
        var b: Byte
        do {
            b = readByte()
            value = (value or (b.toInt() and 0x7f)) shl 7
        } while (b < 0)

        value = value shr 7
        return value or b.toInt()
    }

    fun writeIndex(length: Int) {
        buffer[currentPosition - length - 1] = length.toByte()
    }

    fun writeIntLength(length: Int) {
        buffer[currentPosition - length - 4] = (length shr 24).toByte()
        buffer[currentPosition - length - 3] = (length shr 16).toByte()
        buffer[currentPosition - length - 2] = (length shr 8).toByte()
        buffer[currentPosition - length - 1] = length.toByte()
    }

    fun writeShortLength(length: Int) {
        buffer[currentPosition - length - 2] = (length shr 8).toByte()
        buffer[currentPosition - length - 1] = length.toByte()
    }

    fun encryptXtea(keys: IntArray, start: Int, end: Int) {
        val originalPos = currentPosition
        currentPosition = start
        val blocks = (end - start) / 8

        repeat(blocks) {
            var v0 = readInt()
            var v1 = readInt()
            var sum = 0
            val delta = -1640531527

            repeat(32) {
                v0 += (((v1 shl 4) xor (v1 ushr 5)) + v1) xor (sum + keys[sum and 3])
                sum += delta
                v1 += (((v0 shl 4) xor (v0 ushr 5)) + v0) xor (sum + keys[(sum ushr 11) and 3])
            }

            currentPosition -= 8
            writeInt(v0)
            writeInt(v1)
        }

        currentPosition = originalPos
    }

    fun decryptFullXtea(keys: IntArray) {
        val blocks = currentPosition / 8
        currentPosition = 0

        repeat(blocks) {
            var v0 = readInt()
            var v1 = readInt()
            var sum = -957401312
            val delta = -1640531527

            repeat(32) {
                v1 -= (((v0 shl 4) xor (v0 ushr 5)) + v0) xor (sum + keys[(sum ushr 11) and 3])
                sum -= delta
                v0 -= (((v1 shl 4) xor (v1 ushr 5)) + v1) xor (sum + keys[sum and 3])
            }

            currentPosition -= 8
            writeInt(v0)
            writeInt(v1)
        }
    }

    fun decryptXtea(keys: IntArray, length: Int) {
        val originalPos = currentPosition
        currentPosition = 5
        val blocks = (length - 5) / 8

        repeat(blocks) {
            var v0 = readInt()
            var v1 = readInt()
            var sum = -957401312
            val delta = -1640531527

            repeat(32) {
                v1 -= (((v0 shl 4) xor (v0 ushr 5)) + v0) xor (sum + keys[(sum ushr 11) and 3])
                sum -= delta
                v0 -= (((v1 shl 4) xor (v1 ushr 5)) + v1) xor (sum + keys[sum and 3])
            }

            currentPosition -= 8
            writeInt(v0)
            writeInt(v1)
        }

        currentPosition = originalPos
    }

    fun applyRSA() {
        val length = currentPosition
        currentPosition = 0
        val data = ByteArray(length)
        readBytes(data, 0, length)

        val bigInt = BigInteger(data)
        val encryptedBigInt = bigInt.modPow(Loader.RSA_PUBLIC_EXPONENT, Loader.RSA_PUBLIC_MODULUS)
        val encryptedData = encryptedBigInt.toByteArray()

        currentPosition = 0
        writeShort(encryptedData.size)
        writeBytes(encryptedData, 0, encryptedData.size)
    }

    fun writeCrc(start: Int): Int {
        val crc = Hashing.getCrc32(buffer, start, currentPosition)
        writeInt(crc)
        return crc
    }

    fun checkCrc(): Boolean {
        currentPosition -= 4
        val calculated = Hashing.getCrc32(buffer, 0, currentPosition)
        val expected = readInt()
        return calculated == expected
    }

    open class Bit : Packet {
        private var isaac: Isaac? = null
        private var bitPosition: Int = 0

        constructor(size: Int) : super(size)

        fun setIsaacCipher(cipher: Isaac?) {
            isaac = cipher
        }

        fun nextIsSmart(): Boolean {
            val value = buffer[currentPosition] - (isaac?.nextKey() ?: 0) and 0xff
            return value >= 128
        }

        fun readEncryptedSmart(): Int {
            val value = buffer[currentPosition++] - (isaac?.getNextKey() ?: 0) and 0xff
            return if (value < 128) value
            else (value - 128 shl 8) + (buffer[currentPosition++] - (isaac?.getNextKey() ?: 0) and 0xff)
        }

        fun readBytes(bytes: ByteArray, length: Int) {
            for (i in 0 until length) {
                bytes[i] = (buffer[currentPosition++] - (isaac?.getNextKey() ?: 0)).toByte()
            }
        }

        fun writeIsaacByte(value: Int) {
            buffer[currentPosition++] = (value + (isaac?.getNextKey() ?: 0)).toByte()
        }

        fun initBitAccess() {
            bitPosition = currentPosition * 8
        }

        fun finishBitAccess() {
            currentPosition = (bitPosition + 7) / 8
        }

        fun readBits(numBits: Int): Int {
            var bytePos = bitPosition shr 3
            var bitOffset = 8 - (bitPosition and 0x7)
            var value = 0

            bitPosition += numBits
            var remainingBits = numBits

            while (remainingBits > bitOffset) {
                value += (buffer[bytePos++].toInt() and BIT_MASKS[bitOffset]) shl (remainingBits - bitOffset)
                remainingBits -= bitOffset
                bitOffset = 8
            }

            if (remainingBits == bitOffset) {
                value += buffer[bytePos].toInt() and BIT_MASKS[bitOffset]
            } else {
                value += (buffer[bytePos].toInt() shr (bitOffset - remainingBits)) and BIT_MASKS[remainingBits]
            }

            return value
        }

        fun readableBits(byteCount: Int): Int = byteCount * 8 - bitPosition

        companion object {
            private val BIT_MASKS = intArrayOf(
                0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095,
                8191, 16383, 32767, 65535, 131071, 262143, 524287, 1048575,
                2097151, 4194303, 8388607, 16777215, 33554431, 67108863,
                134217727, 268435455, 536870911, 1073741823, Int.MAX_VALUE, -1
            )
        }
    }

    companion object {
        @JvmStatic
        fun calculateRequiredSize(bytes: ByteArray, startPos: Int, chars: CharSequence): Int {
            val length = chars.length
            var pos = startPos

            for (i in 0 until length) {
                val c = chars[i]
                when {
                    c <= '\u007F' -> bytes[pos++] = c.code.toByte()
                    c <= '\u07FF' -> {
                        bytes[pos++] = (0xc0 or (c.code shr 6)).toByte()
                        bytes[pos++] = (0x80 or (c.code and 0x3f)).toByte()
                    }
                    else -> {
                        bytes[pos++] = (0xe0 or (c.code shr 12)).toByte()
                        bytes[pos++] = (0x80 or (c.code shr 6 and 0x3f)).toByte()
                        bytes[pos++] = (0x80 or (c.code and 0x3f)).toByte()
                    }
                }
            }

            return pos - startPos
        }

        @JvmStatic
        fun writeString(str: CharSequence, length: Int, buffer: ByteArray, position: Int): Int {
            for (i in 0 until length) {
                val c = str[i]
                buffer[i + position] = when {
                    (c.code > 0 && c.code < 128) || (c.code in 160..255) -> c.code.toByte()
                    else -> when (c.code) {
                        8364 -> -128
                        8218 -> -126
                        402 -> -125
                        8222 -> -124
                        8230 -> -123
                        8224 -> -122
                        8225 -> -121
                        710 -> -120
                        8240 -> -119
                        352 -> -118
                        8249 -> -117
                        338 -> -116
                        381 -> -114
                        8216 -> -111
                        8217 -> -110
                        8220 -> -109
                        8221 -> -108
                        8226 -> -107
                        8211 -> -106
                        8212 -> -105
                        732 -> -104
                        8482 -> -103
                        353 -> -102
                        8250 -> -101
                        339 -> -100
                        382 -> -98
                        376 -> -97
                        else -> 63
                    }.toByte()
                }
            }
            return length
        }
    }
}
