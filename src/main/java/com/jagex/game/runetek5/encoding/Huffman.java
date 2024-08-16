package com.jagex.game.runetek5.encoding;

public class Huffman {

    public final int[] codes;

    public final byte[] codeLengths;

    public int[] table;

    public Huffman(byte[] codeLengths) {
        int length = codeLengths.length;
        this.codes = new int[length];
        this.codeLengths = codeLengths;
        int[] bitLengthCounts = new int[33];
        this.table = new int[8];
        int currentCode = 0;
        for (int i = 0; i < length; i++) {
            byte codeLength = codeLengths[i];
            if (codeLength != 0) {
                int code = 1 << 32 - codeLength;
                int bitLengthCount = bitLengthCounts[codeLength];
                this.codes[i] = bitLengthCount;
                int newCode;
                if ((bitLengthCount & code) != 0) {
                    newCode = bitLengthCounts[codeLength - 1];
                } else {
                    newCode = bitLengthCount | code;
                    for (int j = codeLength - 1; j >= 1; --j) {
                        int bitLength = bitLengthCounts[j];
                        if (bitLength != bitLengthCount) {
                            break;
                        }
                        int bit = 1 << 32 - j;
                        if ((bitLength & bit) != 0) {
                            bitLengthCounts[j] = bitLengthCounts[j - 1];
                            break;
                        }
                        bitLengthCounts[j] = bitLength | bit;
                    }
                }
                bitLengthCounts[codeLength] = newCode;
                for (int j = codeLength + 1; j <= 32; j++) {
                    if (bitLengthCount == bitLengthCounts[j]) {
                        bitLengthCounts[j] = newCode;
                    }
                }
                int tableIndex = 0;
                for (int j = 0; j < codeLength; j++) {
                    int bit = Integer.MIN_VALUE >>> j;
                    if ((bitLengthCount & bit) != 0) {
                        if (this.table[tableIndex] == 0) {
                            this.table[tableIndex] = currentCode;
                        }
                        tableIndex = this.table[tableIndex];
                    } else {
                        tableIndex++;
                    }
                    if (tableIndex >= this.table.length) {
                        int[] newTable = new int[this.table.length * 2];
                        System.arraycopy(this.table, 0, newTable, 0, this.table.length);
                        this.table = newTable;
                    }
                    bit >>>= 1;
                }
                this.table[tableIndex] = ~i;
                if (tableIndex >= currentCode) {
                    currentCode = tableIndex + 1;
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
    public int decompress(byte[] inputBytes, int inputOffset, int inputLength,
                                 byte[] outputBytes, int outputOffset) {
        int bitBuffer = 0;
        int bitCount = outputOffset << 3;
        int inputEnd = inputOffset + inputLength;
        for (int i = inputOffset; i < inputEnd; i++) {
            int symbol = inputBytes[i] & 0xff;
            int code = this.codes[symbol];
            byte codeLength = this.codeLengths[symbol];
            if (codeLength == 0) {
                throw new RuntimeException("Invalid code length for symbol: " + symbol);
            }
            int outputOffsetBytes = bitCount >> 3;
            int outputOffsetBits = bitCount & 0x7;
            bitBuffer &= -outputOffsetBits >> 31;
            int outputEnd = (outputOffsetBits + codeLength - 1 >> 3) + outputOffsetBytes;
            outputOffsetBits += 24;
            outputBytes[outputOffsetBytes] = (byte) (bitBuffer |= code >>> outputOffsetBits);
            if (outputOffsetBytes < outputEnd) {
                outputOffsetBytes++;
                outputOffsetBits -= 8;
                outputBytes[outputOffsetBytes] = (byte) (bitBuffer = code >>> outputOffsetBits);
                if (outputOffsetBytes < outputEnd) {
                    outputOffsetBytes++;
                    outputOffsetBits -= 8;
                    outputBytes[outputOffsetBytes] = (byte) (bitBuffer = code >>> outputOffsetBits);
                    if (outputOffsetBytes < outputEnd) {
                        outputOffsetBytes++;
                        outputOffsetBits -= 8;
                        outputBytes[outputOffsetBytes] = (byte) (bitBuffer = code >>> outputOffsetBits);
                        if (outputOffsetBytes < outputEnd) {
                            outputOffsetBytes++;
                            outputOffsetBits -= 8;
                            outputBytes[outputOffsetBytes] = (byte) (bitBuffer = code << -outputOffsetBits);
                        }
                    }
                }
            }
            bitCount += codeLength;
        }
        return (bitCount + 7 >> 3) - outputOffset;
    }

    public int calculateDecompressedLength(byte[] compressedBytes, int compressedOffset, byte[] decompressedBytes, int decompressedOffset, int decompressedLength) {
        if (decompressedLength == 0) {
            return 0;
        } 
        int tableIndex = 0;
        decompressedLength += decompressedOffset;
        int compressedIndex = compressedOffset;
        while (true) {
            byte flags = compressedBytes[compressedIndex];
            if (flags < 0) {
                tableIndex = this.table[tableIndex];
            } else {
                ++tableIndex;
            }
            int value;
            if ((value = this.table[tableIndex]) < 0) {
                decompressedBytes[decompressedOffset++] = (byte) (~value);
                if (decompressedOffset >= decompressedLength) {
                    break;
                }
                tableIndex = 0;
            }
            if ((flags & 0x40) != 0) {
                tableIndex = this.table[tableIndex];
            } else {
                ++tableIndex;
            }
            if ((value = this.table[tableIndex]) < 0) {
                decompressedBytes[decompressedOffset++] = (byte) (~value);
                if (decompressedOffset >= decompressedLength) {
                    break;
                }
                tableIndex = 0;
            }
            if ((flags & 0x20) != 0) {
                tableIndex = this.table[tableIndex];
            } else {
                ++tableIndex;
            }
            if ((value = this.table[tableIndex]) < 0) {
                decompressedBytes[decompressedOffset++] = (byte) (~value);
                if (decompressedOffset >= decompressedLength) {
                    break;
                }
                tableIndex = 0;
            }
            if ((flags & 0x10) != 0) {
                tableIndex = this.table[tableIndex];
            } else {
                ++tableIndex;
            }
            if ((value = this.table[tableIndex]) < 0) {
                decompressedBytes[decompressedOffset++] = (byte) (~value);
                if (decompressedOffset >= decompressedLength) {
                    break;
                }
                tableIndex = 0;
            }
            if ((flags & 0x8) != 0) {
                tableIndex = this.table[tableIndex];
            } else {
                ++tableIndex;
            }
            if ((value = this.table[tableIndex]) < 0) {
                decompressedBytes[decompressedOffset++] = (byte) (~value);
                if (decompressedOffset >= decompressedLength) {
                    break;
                }
                tableIndex = 0;
            }
            if ((flags & 0x4) != 0) {
                tableIndex = this.table[tableIndex];
            } else {
                ++tableIndex;
            }
            if ((value = this.table[tableIndex]) < 0) {
                decompressedBytes[decompressedOffset++] = (byte) (~value);
                if (decompressedOffset >= decompressedLength) {
                    break;
                }
                tableIndex = 0;
            }
            if ((flags & 0x2) != 0) {
                tableIndex = this.table[tableIndex];
            } else {
                ++tableIndex;
            }
            if ((value = this.table[tableIndex]) < 0) {
                decompressedBytes[decompressedOffset++] = (byte) (~value);
                if (decompressedOffset >= decompressedLength) {
                    break;
                }
                tableIndex = 0;
            }
            if ((flags & 0x1) != 0) {
                tableIndex = this.table[tableIndex];
            } else {
                ++tableIndex;
            }
            if ((value = this.table[tableIndex]) < 0) {
                decompressedBytes[decompressedOffset++] = (byte) (~value);
                if (decompressedOffset >= decompressedLength) {
                    break;
                }
                tableIndex = 0;
            }
            ++compressedIndex;
        }
        return compressedIndex + 1 - compressedOffset;
    }
}
