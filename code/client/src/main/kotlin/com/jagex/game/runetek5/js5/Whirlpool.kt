package com.jagex.game.runetek5.js5

import java.util.Arrays

class Whirlpool {

    var bufferPos: Int = 0
    var bufferBits: Int = 0
    val bitLength: ByteArray = ByteArray(32)
    val buffer: ByteArray = ByteArray(64)
    val hash: LongArray = LongArray(8)
    val K: LongArray = LongArray(8)
    val L: LongArray = LongArray(8)
    val block: LongArray = LongArray(8)
    val state: LongArray = LongArray(8)

    fun NESSIEinit() {
        Arrays.fill(this.bitLength, 0.toByte())
        this.bufferPos = 0
        this.bufferBits = this.bufferPos
        this.buffer[0] = 0
        Arrays.fill(this.hash, 0L)
    }

    fun NESSIEadd(source: ByteArray, sourceBits: Long) {
        var sourcePos = 0
        val sourceGap = (8 - (sourceBits.toInt() and 7)) and 7
        val bufferRem = this.bufferBits and 7
        var b: Int
        var value = sourceBits
        var carry = 0
        for (i in 31 downTo 0) {
            carry += (this.bitLength[i].toInt() and 0xff) + (value.toInt() and 0xff)
            this.bitLength[i] = carry.toByte()
            carry = carry ushr 8
            value = value ushr 8
        }
        var remainingBits = sourceBits
        while (remainingBits > 8) {
            b = ((source[sourcePos].toInt() shl sourceGap) and 0xff) or
                ((source[sourcePos + 1].toInt() and 0xff) ushr (8 - sourceGap))
            if (b < 0 || b >= 256) {
                throw RuntimeException("LOGIC ERROR")
            }
            this.buffer[this.bufferPos++] = (this.buffer[this.bufferPos - 1].toInt() or (b ushr bufferRem)).toByte()
            this.bufferBits += 8 - bufferRem
            if (this.bufferBits == 512) {
                processBuffer()
                this.bufferPos = 0
                this.bufferBits = this.bufferPos
            }
            this.buffer[this.bufferPos] = ((b shl (8 - bufferRem)) and 0xff).toByte()
            this.bufferBits += bufferRem
            remainingBits -= 8
            sourcePos++
        }
        b = if (remainingBits > 0) {
            (source[sourcePos].toInt() shl sourceGap) and 0xff
        } else {
            0
        }
        if (bufferRem + remainingBits < 8) {
            this.buffer[this.bufferPos] = (this.buffer[this.bufferPos].toInt() or (b ushr bufferRem)).toByte()
            this.bufferBits += remainingBits.toInt()
        } else {
            this.buffer[this.bufferPos++] = (this.buffer[this.bufferPos - 1].toInt() or (b ushr bufferRem)).toByte()
            this.bufferBits += 8 - bufferRem
            remainingBits -= (8 - bufferRem).toLong()
            if (this.bufferBits == 512) {
                processBuffer()
                this.bufferPos = 0
                this.bufferBits = this.bufferPos
            }
            this.buffer[this.bufferPos] = ((b shl (8 - bufferRem)) and 0xff).toByte()
            this.bufferBits += remainingBits.toInt()
        }
    }

    fun NESSIEfinalize(digest: ByteArray) {
        this.buffer[this.bufferPos] = (this.buffer[this.bufferPos].toInt() or (0x80 ushr (this.bufferBits and 7))).toByte()
        this.bufferPos++
        if (this.bufferPos > 32) {
            while (this.bufferPos < 64) {
                this.buffer[this.bufferPos++] = 0
            }
            processBuffer()
            this.bufferPos = 0
        }
        while (this.bufferPos < 32) {
            this.buffer[this.bufferPos++] = 0
        }
        System.arraycopy(this.bitLength, 0, this.buffer, 32, 32)
        processBuffer()
        var j = 0
        for (i in 0..7) {
            val h = this.hash[i]
            digest[j] = (h ushr 56).toByte()
            digest[j + 1] = (h ushr 48).toByte()
            digest[j + 2] = (h ushr 40).toByte()
            digest[j + 3] = (h ushr 32).toByte()
            digest[j + 4] = (h ushr 24).toByte()
            digest[j + 5] = (h ushr 16).toByte()
            digest[j + 6] = (h ushr 8).toByte()
            digest[j + 7] = h.toByte()
            j += 8
        }
    }

    fun processBuffer() {
        var j = 0
        for (i in 0..7) {
            this.block[i] =
                ((this.buffer[j].toLong()) shl 56) xor
                ((this.buffer[j + 1].toLong() and 0xffL) shl 48) xor
                ((this.buffer[j + 2].toLong() and 0xffL) shl 40) xor
                ((this.buffer[j + 3].toLong() and 0xffL) shl 32) xor
                ((this.buffer[j + 4].toLong() and 0xffL) shl 24) xor
                ((this.buffer[j + 5].toLong() and 0xffL) shl 16) xor
                ((this.buffer[j + 6].toLong() and 0xffL) shl 8) xor
                ((this.buffer[j + 7].toLong() and 0xffL))
            j += 8
        }
        for (i in 0..7) {
            this.state[i] = this.block[i] xor (this.hash[i].also { this.K[i] = it })
        }
        for (r in 1..ROUNDS) {
            for (i in 0..7) {
                this.L[i] = 0L
                var s = 56
                for (t in 0..7) {
                    this.L[i] = this.L[i] xor CIRCULANT_TABLE[t][(this.K[(i - t) and 7] ushr s).toInt() and 0xff]
                    s -= 8
                }
            }
            System.arraycopy(this.L, 0, this.K, 0, 8)
            this.K[0] = this.K[0] xor ROUND_CONSTANTS[r]
            for (i in 0..7) {
                this.L[i] = this.K[i]
                var s = 56
                for (t in 0..7) {
                    this.L[i] = this.L[i] xor CIRCULANT_TABLE[t][(this.state[(i - t) and 7] ushr s).toInt() and 0xff]
                    s -= 8
                }
            }
            System.arraycopy(this.L, 0, this.state, 0, 8)
        }
        for (i in 0..7) {
            this.hash[i] = this.hash[i] xor this.state[i] xor this.block[i]
        }
    }

    companion object {
        const val ROUNDS = 10

        @JvmField
        val CIRCULANT_TABLE = Array(8) { LongArray(256) }

        @JvmField
        val ROUND_CONSTANTS = LongArray(11)

        private const val SBOX =
            "\u1823\uc6E8\u87B8\u014F\u36A6\ud2F5\u796F\u9152" +
            "\u60Bc\u9B8E\uA30c\u7B35\u1dE0\ud7c2\u2E4B\uFE57" +
            "\u1577\u37E5\u9FF0\u4AdA\u58c9\u290A\uB1A0\u6B85" +
            "\uBd5d\u10F4\ucB3E\u0567\uE427\u418B\uA77d\u95d8" +
            "\uFBEE\u7c66\udd17\u479E\ucA2d\uBF07\uAd5A\u8333" +
            "\u6302\uAA71\uc819\u49d9\uF2E3\u5B88\u9A26\u32B0" +
            "\uE90F\ud580\uBEcd\u3448\uFF7A\u905F\u2068\u1AAE" +
            "\uB454\u9322\u64F1\u7312\u4008\uc3Ec\udBA1\u8d3d" +
            "\u9700\ucF2B\u7682\ud61B\uB5AF\u6A50\u45F3\u30EF" +
            "\u3F55\uA2EA\u65BA\u2Fc0\udE1c\uFd4d\u9275\u068A" +
            "\uB2E6\u0E1F\u62d4\uA896\uF9c5\u2559\u8472\u394c" +
            "\u5E78\u388c\ud1A5\uE261\uB321\u9c1E\u43c7\uFc04" +
            "\u5199\u6d0d\uFAdF\u7E24\u3BAB\ucE11\u8F4E\uB7EB" +
            "\u3c81\u94F7\uB913\u2cd3\uE76E\uc403\u5644\u7FA9" +
            "\u2ABB\uc153\udc0B\u9d6c\u3174\uF646\uAc89\u14E1" +
            "\u163A\u6909\u70B6\ud0Ed\ucc42\u98A4\u285c\uF886"

        init {
            for (x in 0..255) {
                val c = SBOX[x / 2]
                val v1 = (if ((x and 1) == 0) c.code ushr 8 else c.code and 0xff).toLong()
                var v2 = v1 shl 1
                if (v2 >= 0x100L) {
                    v2 = v2 xor 0x11dL
                }
                var v4 = v2 shl 1
                if (v4 >= 0x100L) {
                    v4 = v4 xor 0x11dL
                }
                val v5 = v4 xor v1
                var v8 = v4 shl 1
                if (v8 >= 0x100L) {
                    v8 = v8 xor 0x11dL
                }
                val v9 = v8 xor v1
                CIRCULANT_TABLE[0][x] =
                    (v1 shl 56) or (v1 shl 48) or (v4 shl 40) or (v1 shl 32) or
                    (v8 shl 24) or (v5 shl 16) or (v2 shl 8) or (v9)
                for (t in 1..7) {
                    CIRCULANT_TABLE[t][x] = (CIRCULANT_TABLE[t - 1][x] ushr 8) or ((CIRCULANT_TABLE[t - 1][x] shl 56))
                }
            }

            ROUND_CONSTANTS[0] = 0L
            for (r in 1..ROUNDS) {
                val i = 8 * (r - 1)
                ROUND_CONSTANTS[r] =
                    (CIRCULANT_TABLE[0][i] and -0x100000000000000L) xor
                    (CIRCULANT_TABLE[1][i + 1] and 0x00ff000000000000L) xor
                    (CIRCULANT_TABLE[2][i + 2] and 0x0000ff0000000000L) xor
                    (CIRCULANT_TABLE[3][i + 3] and 0x000000ff00000000L) xor
                    (CIRCULANT_TABLE[4][i + 4] and 0x00000000ff000000L) xor
                    (CIRCULANT_TABLE[5][i + 5] and 0x0000000000ff0000L) xor
                    (CIRCULANT_TABLE[6][i + 6] and 0x000000000000ff00L) xor
                    (CIRCULANT_TABLE[7][i + 7] and 0x00000000000000ffL)
            }
        }

        @JvmStatic
        fun calculateDigest(src: ByteArray, srcPosition: Int, size: Int): ByteArray {
            val dst: ByteArray = if (srcPosition > 0) {
                val temp = ByteArray(size)
                System.arraycopy(src, srcPosition, temp, 0, size)
                temp
            } else {
                src
            }
            val whirlpool = Whirlpool()
            whirlpool.NESSIEinit()
            whirlpool.NESSIEadd(dst, (size * 8).toLong())
            val digest = ByteArray(64)
            whirlpool.NESSIEfinalize(digest)
            return digest
        }
    }
}
