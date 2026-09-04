package com.jagex.game.runetek5.encoding

class Huffman(codeLengths: ByteArray) {

    val codes: IntArray
    val codeLengths: ByteArray
    var table: IntArray

    init {
        val length = codeLengths.size
        this.codes = IntArray(length)
        this.codeLengths = codeLengths
        val bitLengthCounts = IntArray(33)
        this.table = IntArray(8)
        var currentCode = 0
        for (i in 0 until length) {
            val codeLength = codeLengths[i]
            if (codeLength.toInt() != 0) {
                val code = 1 shl 32 - codeLength
                val bitLengthCount = bitLengthCounts[codeLength.toInt()]
                this.codes[i] = bitLengthCount
                val newCode: Int
                if ((bitLengthCount and code) != 0) {
                    newCode = bitLengthCounts[codeLength - 1]
                } else {
                    newCode = bitLengthCount or code
                    var j = codeLength - 1
                    while (j >= 1) {
                        val bitLength = bitLengthCounts[j]
                        if (bitLength != bitLengthCount) {
                            break
                        }
                        val bit = 1 shl 32 - j
                        if ((bitLength and bit) != 0) {
                            bitLengthCounts[j] = bitLengthCounts[j - 1]
                            break
                        }
                        bitLengthCounts[j] = bitLength or bit
                        --j
                    }
                }
                bitLengthCounts[codeLength.toInt()] = newCode
                for (j in codeLength + 1..32) {
                    if (bitLengthCount == bitLengthCounts[j]) {
                        bitLengthCounts[j] = newCode
                    }
                }
                var tableIndex = 0
                for (j in 0 until codeLength.toInt()) {
                    val bit = Int.MIN_VALUE ushr j
                    if ((bitLengthCount and bit) != 0) {
                        if (this.table[tableIndex] == 0) {
                            this.table[tableIndex] = currentCode
                        }
                        tableIndex = this.table[tableIndex]
                    } else {
                        tableIndex++
                    }
                    if (tableIndex >= this.table.size) {
                        val newTable = IntArray(this.table.size * 2)
                        System.arraycopy(this.table, 0, newTable, 0, this.table.size)
                        this.table = newTable
                    }
                }
                this.table[tableIndex] = i.inv()
                if (tableIndex >= currentCode) {
                    currentCode = tableIndex + 1
                }
            }
        }
    }

    /**
     * Decompresses Huffman-encoded data
     *
     * @param inputBytes the input byte array to be decompressed
     * @param inputOffset the starting offset in the input byte array
     * @param inputLength the number of bytes to be decompressed
     * @param outputBytes the output byte array to hold the decompressed data
     * @param outputOffset the starting offset in the output byte array
     * @return the number of bytes written to the output byte array
     */
    fun decompress(inputBytes: ByteArray, inputOffset: Int, inputLength: Int,
                   outputBytes: ByteArray, outputOffset: Int): Int {
        var bitBuffer = 0
        var bitCount = outputOffset shl 3
        val inputEnd = inputOffset + inputLength
        for (i in inputOffset until inputEnd) {
            val symbol = inputBytes[i].toInt() and 0xff
            val code = this.codes[symbol]
            val codeLength = this.codeLengths[symbol]
            if (codeLength.toInt() == 0) {
                throw RuntimeException("Invalid code length for symbol: $symbol")
            }
            var outputOffsetBytes = bitCount shr 3
            var outputOffsetBits = bitCount and 0x7
            bitBuffer = bitBuffer and (-outputOffsetBits shr 31)
            val outputEnd = (outputOffsetBits + codeLength - 1 shr 3) + outputOffsetBytes
            outputOffsetBits += 24
            outputBytes[outputOffsetBytes] = (bitBuffer or (code ushr outputOffsetBits)).toByte().also { bitBuffer = bitBuffer or (code ushr outputOffsetBits) }
            if (outputOffsetBytes < outputEnd) {
                outputOffsetBytes++
                outputOffsetBits -= 8
                outputBytes[outputOffsetBytes] = (code ushr outputOffsetBits).toByte().also { bitBuffer = code ushr outputOffsetBits }
                if (outputOffsetBytes < outputEnd) {
                    outputOffsetBytes++
                    outputOffsetBits -= 8
                    outputBytes[outputOffsetBytes] = (code ushr outputOffsetBits).toByte().also { bitBuffer = code ushr outputOffsetBits }
                    if (outputOffsetBytes < outputEnd) {
                        outputOffsetBytes++
                        outputOffsetBits -= 8
                        outputBytes[outputOffsetBytes] = (code ushr outputOffsetBits).toByte().also { bitBuffer = code ushr outputOffsetBits }
                        if (outputOffsetBytes < outputEnd) {
                            outputOffsetBytes++
                            outputOffsetBits -= 8
                            outputBytes[outputOffsetBytes] = (code shl -outputOffsetBits).toByte().also { bitBuffer = code shl -outputOffsetBits }
                        }
                    }
                }
            }
            bitCount += codeLength
        }
        return (bitCount + 7 shr 3) - outputOffset
    }

    fun calculateDecompressedLength(compressedBytes: ByteArray, compressedOffset: Int, decompressedBytes: ByteArray, decompressedOffset: Int, decompressedLength: Int): Int {
        if (decompressedLength == 0) {
            return 0
        }
        var tableIndex = 0
        val decompressedEnd = decompressedLength + decompressedOffset
        var compressedIndex = compressedOffset
        var decompressedOffset = decompressedOffset
        while (true) {
            val flags = compressedBytes[compressedIndex]
            if (flags < 0) {
                tableIndex = this.table[tableIndex]
            } else {
                ++tableIndex
            }
            var value: Int
            if (this.table[tableIndex].also { value = it } < 0) {
                decompressedBytes[decompressedOffset++] = value.inv().toByte()
                if (decompressedOffset >= decompressedEnd) {
                    break
                }
                tableIndex = 0
            }
            if ((flags.toInt() and 0x40) != 0) {
                tableIndex = this.table[tableIndex]
            } else {
                ++tableIndex
            }
            if (this.table[tableIndex].also { value = it } < 0) {
                decompressedBytes[decompressedOffset++] = value.inv().toByte()
                if (decompressedOffset >= decompressedEnd) {
                    break
                }
                tableIndex = 0
            }
            if ((flags.toInt() and 0x20) != 0) {
                tableIndex = this.table[tableIndex]
            } else {
                ++tableIndex
            }
            if (this.table[tableIndex].also { value = it } < 0) {
                decompressedBytes[decompressedOffset++] = value.inv().toByte()
                if (decompressedOffset >= decompressedEnd) {
                    break
                }
                tableIndex = 0
            }
            if ((flags.toInt() and 0x10) != 0) {
                tableIndex = this.table[tableIndex]
            } else {
                ++tableIndex
            }
            if (this.table[tableIndex].also { value = it } < 0) {
                decompressedBytes[decompressedOffset++] = value.inv().toByte()
                if (decompressedOffset >= decompressedEnd) {
                    break
                }
                tableIndex = 0
            }
            if ((flags.toInt() and 0x8) != 0) {
                tableIndex = this.table[tableIndex]
            } else {
                ++tableIndex
            }
            if (this.table[tableIndex].also { value = it } < 0) {
                decompressedBytes[decompressedOffset++] = value.inv().toByte()
                if (decompressedOffset >= decompressedEnd) {
                    break
                }
                tableIndex = 0
            }
            if ((flags.toInt() and 0x4) != 0) {
                tableIndex = this.table[tableIndex]
            } else {
                ++tableIndex
            }
            if (this.table[tableIndex].also { value = it } < 0) {
                decompressedBytes[decompressedOffset++] = value.inv().toByte()
                if (decompressedOffset >= decompressedEnd) {
                    break
                }
                tableIndex = 0
            }
            if ((flags.toInt() and 0x2) != 0) {
                tableIndex = this.table[tableIndex]
            } else {
                ++tableIndex
            }
            if (this.table[tableIndex].also { value = it } < 0) {
                decompressedBytes[decompressedOffset++] = value.inv().toByte()
                if (decompressedOffset >= decompressedEnd) {
                    break
                }
                tableIndex = 0
            }
            if ((flags.toInt() and 0x1) != 0) {
                tableIndex = this.table[tableIndex]
            } else {
                ++tableIndex
            }
            if (this.table[tableIndex].also { value = it } < 0) {
                decompressedBytes[decompressedOffset++] = value.inv().toByte()
                if (decompressedOffset >= decompressedEnd) {
                    break
                }
                tableIndex = 0
            }
            ++compressedIndex
        }
        return compressedIndex + 1 - compressedOffset
    }
}
