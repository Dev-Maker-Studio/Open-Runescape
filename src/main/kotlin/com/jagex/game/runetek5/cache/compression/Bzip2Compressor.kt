package com.jagex.game.runetek5.cache.compression

object Bzip2Compressor {

    @JvmField val BLOCK_ENTRY = Bzip2BlockEntry()
    @JvmField var transformTable: IntArray? = null

    @JvmStatic
    fun decompress(decompressed: ByteArray, decompressedLength: Int, compressed: ByteArray): Int {
        var length = decompressedLength
        synchronized(BLOCK_ENTRY) {
            BLOCK_ENTRY.compressed = compressed
            BLOCK_ENTRY.startOffset = 9
            BLOCK_ENTRY.decompressed = decompressed
            BLOCK_ENTRY.outputPosition = 0
            BLOCK_ENTRY.decompressedLength = length
            BLOCK_ENTRY.bitBufferLength = 0
            BLOCK_ENTRY.bitBuffer = 0
            BLOCK_ENTRY.bytesRead = 0
            BLOCK_ENTRY.blockCrc = 0
            decompress(BLOCK_ENTRY)
            length -= BLOCK_ENTRY.decompressedLength
            BLOCK_ENTRY.compressed = null
            BLOCK_ENTRY.decompressed = null
            return length
        }
    }

    @JvmStatic
    fun readByte(entry: Bzip2BlockEntry): Byte {
        return readBits(8, entry).toByte()
    }

    @JvmStatic
    fun readBit(entry: Bzip2BlockEntry): Byte {
        return readBits(1, entry).toByte()
    }

    @JvmStatic
    fun readBits(numBits: Int, entry: Bzip2BlockEntry): Int {
        while (entry.bitBufferLength < numBits) {
            entry.bitBuffer = entry.bitBuffer shl 8 or (entry.compressed!![entry.startOffset].toInt() and 0xff)
            entry.bitBufferLength += 8
            ++entry.startOffset
            entry.bytesRead += 1564222761
            val bytesReadSnapshot = entry.bytesRead
        }

        val result = entry.bitBuffer shr entry.bitBufferLength - numBits and (1 shl numBits) - 1
        entry.bitBufferLength -= numBits
        return result
    }

    @JvmStatic
    fun buildHuffmanTable(limitTable: IntArray, baseTable: IntArray, permTable: IntArray, lengthTable: ByteArray, minLength: Int, maxLength: Int, symbolCount: Int) {
        var permIndex = 0

        var bitLength: Int
        var symbolIndex: Int
        bitLength = minLength
        while (bitLength <= maxLength) {
            symbolIndex = 0
            while (symbolIndex < symbolCount) {
                if (bitLength.toByte() == lengthTable[symbolIndex]) {
                    permTable[permIndex] = symbolIndex
                    ++permIndex
                }
                symbolIndex++
            }
            bitLength++
        }

        bitLength = 0
        while (bitLength < 23) {
            baseTable[bitLength] = 0
            bitLength++
        }

        bitLength = 0
        while (bitLength < symbolCount) {
            ++baseTable[lengthTable[bitLength].toInt() + 1]
            bitLength++
        }

        bitLength = 1
        while (bitLength < 23) {
            baseTable[bitLength] += baseTable[bitLength - 1]
            bitLength++
        }

        bitLength = 0
        while (bitLength < 23) {
            limitTable[bitLength] = 0
            bitLength++
        }

        bitLength = 0

        symbolIndex = minLength
        while (symbolIndex <= maxLength) {
            bitLength += baseTable[symbolIndex + 1] - baseTable[symbolIndex]
            limitTable[symbolIndex] = bitLength - 1
            bitLength = bitLength shl 1
            symbolIndex++
        }

        symbolIndex = minLength + 1
        while (symbolIndex <= maxLength) {
            baseTable[symbolIndex] = (limitTable[symbolIndex - 1] + 1 shl 1) - baseTable[symbolIndex]
            symbolIndex++
        }
    }

    @JvmStatic
    fun decodeBlock(entry: Bzip2BlockEntry) {
        var currentByte = entry.currentByte
        var runCount = entry.runCount
        var blockPosition = entry.blockPosition
        var lastByte = entry.lastByte
        val table = transformTable!!
        var bwtPointer = entry.bwtPointer
        val output = entry.decompressed!!
        var outputPos = entry.outputPosition
        var remaining = entry.decompressedLength
        val blockEnd = entry.blockSize + 1

        label56@ while (true) {
            if (runCount > 0) {
                while (true) {
                    if (remaining == 0) {
                        break@label56
                    }

                    if (runCount == 1) {
                        output[outputPos] = currentByte
                        ++outputPos
                        --remaining
                        break
                    }

                    output[outputPos] = currentByte
                    --runCount
                    ++outputPos
                    --remaining
                }
            }

            while (blockPosition != blockEnd) {
                currentByte = lastByte.toByte()
                bwtPointer = table[bwtPointer]
                var transformedByte = bwtPointer.toByte()
                bwtPointer = bwtPointer shr 8
                ++blockPosition
                if (transformedByte != lastByte.toByte()) {
                    lastByte = transformedByte.toInt()
                } else {
                    if (blockPosition != blockEnd) {
                        runCount = 2
                        bwtPointer = table[bwtPointer]
                        transformedByte = bwtPointer.toByte()
                        bwtPointer = bwtPointer shr 8
                        ++blockPosition
                        if (blockPosition != blockEnd) {
                            if (transformedByte != lastByte.toByte()) {
                                lastByte = transformedByte.toInt()
                            } else {
                                runCount = 3
                                bwtPointer = table[bwtPointer]
                                transformedByte = bwtPointer.toByte()
                                bwtPointer = bwtPointer shr 8
                                ++blockPosition
                                if (blockPosition != blockEnd) {
                                    if (transformedByte != lastByte.toByte()) {
                                        lastByte = transformedByte.toInt()
                                    } else {
                                        bwtPointer = table[bwtPointer]
                                        transformedByte = bwtPointer.toByte()
                                        bwtPointer = bwtPointer shr 8
                                        ++blockPosition
                                        runCount = (transformedByte.toInt() and 0xff) + 4
                                        bwtPointer = table[bwtPointer]
                                        lastByte = bwtPointer.toByte().toInt()
                                        bwtPointer = bwtPointer shr 8
                                        ++blockPosition
                                    }
                                }
                            }
                        }
                        continue@label56
                    }
                }
                if (remaining == 0) {
                    runCount = 1
                    break@label56
                }
                output[outputPos] = currentByte
                ++outputPos
                --remaining
            }

            runCount = 0
            break
        }

        val crcSnapshot = entry.blockCrc
        val crcCheck = entry.blockCrc * -1541698763
        entry.currentByte = currentByte
        entry.runCount = runCount
        entry.blockPosition = blockPosition
        entry.lastByte = lastByte
        transformTable = table
        entry.bwtPointer = bwtPointer
        entry.decompressed = output
        entry.outputPosition = outputPos
        entry.decompressedLength = remaining
    }

    @JvmStatic
    fun buildSymbolMap(entry: Bzip2BlockEntry) {
        entry.symbolCount = 0

        for (symbol in 0 until 256) {
            if (entry.inUse[symbol]) {
                entry.seqToUnseq[entry.symbolCount] = symbol.toByte()
                ++entry.symbolCount
            }
        }
    }

    @JvmStatic
    fun decompress(entry: Bzip2BlockEntry) {
        var minLen = 0
        var huffmanLimits: IntArray? = null
        var huffmanBase: IntArray? = null
        var huffmanPerm: IntArray? = null
        entry.blockSizeMultiplier = 1
        if (transformTable == null) {
            transformTable = IntArray(entry.blockSizeMultiplier * 100000)
        }

        var hasMoreBlocks = true

        while (hasMoreBlocks) {
            var headerByte = readByte(entry)
            if (headerByte == 23.toByte()) {
                break
            }

            headerByte = readByte(entry)
            headerByte = readByte(entry)
            headerByte = readByte(entry)
            headerByte = readByte(entry)
            headerByte = readByte(entry)
            headerByte = readByte(entry)
            headerByte = readByte(entry)
            headerByte = readByte(entry)
            headerByte = readByte(entry)
            headerByte = readBit(entry)
            entry.origPtr = 0
            var origPtrByte = readByte(entry)
            entry.origPtr = entry.origPtr shl 8 or (origPtrByte.toInt() and 0xff)
            origPtrByte = readByte(entry)
            entry.origPtr = entry.origPtr shl 8 or (origPtrByte.toInt() and 0xff)
            origPtrByte = readByte(entry)
            entry.origPtr = entry.origPtr shl 8 or (origPtrByte.toInt() and 0xff)

            var groupIndex: Int
            groupIndex = 0
            while (groupIndex < 16) {
                headerByte = readBit(entry)
                entry.inUse16[groupIndex] = headerByte == 1.toByte()
                groupIndex++
            }

            groupIndex = 0
            while (groupIndex < 256) {
                entry.inUse[groupIndex] = false
                groupIndex++
            }

            var symbolIndex: Int
            groupIndex = 0
            while (groupIndex < 16) {
                if (entry.inUse16[groupIndex]) {
                    symbolIndex = 0
                    while (symbolIndex < 16) {
                        headerByte = readBit(entry)
                        if (headerByte == 1.toByte()) {
                            entry.inUse[symbolIndex + groupIndex * 16] = true
                        }
                        symbolIndex++
                    }
                }
                groupIndex++
            }

            buildSymbolMap(entry)
            groupIndex = entry.symbolCount + 2
            symbolIndex = readBits(3, entry)
            val numSelectors = readBits(15, entry)

            var selectorMtfLen: Int
            var selectorIndex = 0
            while (selectorIndex < numSelectors) {
                selectorMtfLen = 0

                while (true) {
                    headerByte = readBit(entry)
                    if (headerByte == 0.toByte()) {
                        entry.selectorMtf[selectorIndex] = selectorMtfLen.toByte()
                        break
                    }

                    ++selectorMtfLen
                }
                selectorIndex++
            }

            val mtfOrder = ByteArray(6)

            var mtfPosition: Byte = 0
            while (mtfPosition < symbolIndex) {
                mtfOrder[mtfPosition.toInt()] = mtfPosition
                mtfPosition++
            }

            var mtfValue: Byte
            var mtfLength: Byte
            selectorMtfLen = 0
            while (selectorMtfLen < numSelectors) {
                mtfValue = entry.selectorMtf[selectorMtfLen]

                mtfLength = mtfOrder[mtfValue.toInt()]
                while (mtfValue > 0) {
                    mtfOrder[mtfValue.toInt()] = mtfOrder[mtfValue - 1]
                    --mtfValue
                }

                mtfOrder[0] = mtfLength
                entry.selector[selectorMtfLen] = mtfLength
                selectorMtfLen++
            }

            var currentLength: Int
            var alphaIndex: Int
            selectorMtfLen = 0
            while (selectorMtfLen < symbolIndex) {
                currentLength = readBits(5, entry)

                alphaIndex = 0
                while (alphaIndex < groupIndex) {
                    while (true) {
                        headerByte = readBit(entry)
                        if (headerByte == 0.toByte()) {
                            entry.len[selectorMtfLen][alphaIndex] = currentLength.toByte()
                            break
                        }

                        headerByte = readBit(entry)
                        if (headerByte == 0.toByte()) {
                            ++currentLength
                        } else {
                            --currentLength
                        }
                    }
                    alphaIndex++
                }
                selectorMtfLen++
            }

            var alphaIdx: Int
            selectorMtfLen = 0
            while (selectorMtfLen < symbolIndex) {
                mtfValue = 32
                mtfLength = 0

                alphaIdx = 0
                while (alphaIdx < groupIndex) {
                    if (entry.len[selectorMtfLen][alphaIdx] > mtfLength) {
                        mtfLength = entry.len[selectorMtfLen][alphaIdx]
                    }

                    if (entry.len[selectorMtfLen][alphaIdx] < mtfValue) {
                        mtfValue = entry.len[selectorMtfLen][alphaIdx]
                    }
                    alphaIdx++
                }

                buildHuffmanTable(entry.limit[selectorMtfLen], entry.base[selectorMtfLen], entry.perm[selectorMtfLen], entry.len[selectorMtfLen], mtfValue.toInt(), mtfLength.toInt(), groupIndex)
                entry.minLens[selectorMtfLen] = mtfValue.toInt()
                selectorMtfLen++
            }

            selectorMtfLen = entry.symbolCount + 1
            currentLength = -1
            var groupSelector: Byte = 0

            alphaIdx = 0
            while (alphaIdx <= 255) {
                entry.unzftab[alphaIdx] = 0
                alphaIdx++
            }

            alphaIdx = 4095

            var yyBlockIdx: Int
            var yySymIdx: Int
            yyBlockIdx = 15
            while (yyBlockIdx >= 0) {
                yySymIdx = 15
                while (yySymIdx >= 0) {
                    entry.yy[alphaIdx] = (yySymIdx + yyBlockIdx * 16).toByte()
                    --alphaIdx
                    --yySymIdx
                }

                entry.mtfmBase[yyBlockIdx] = alphaIdx + 1
                --yyBlockIdx
            }

            yyBlockIdx = 0
            ++currentLength
            groupSelector = 50
            val activeSelector = entry.selector[currentLength]
            minLen = entry.minLens[activeSelector.toInt()].toInt()
            huffmanLimits = entry.limit[activeSelector.toInt()]
            huffmanPerm = entry.perm[activeSelector.toInt()]
            huffmanBase = entry.base[activeSelector.toInt()]

            alphaIndex = groupSelector - 1
            yySymIdx = minLen

            var decodedBit: Byte
            var codeValue: Int
            codeValue = readBits(minLen, entry)
            while (codeValue > huffmanLimits!![yySymIdx]) {
                ++yySymIdx
                decodedBit = readBit(entry)
                codeValue = codeValue shl 1 or decodedBit.toInt()
            }

            var decodedSymbol = huffmanPerm!![codeValue - huffmanBase!![yySymIdx]]

            while (true) {
                var mtfIndex: Int
                while (decodedSymbol != selectorMtfLen) {
                    var mtfBaseIdx: Int
                    if (decodedSymbol != 0 && decodedSymbol != 1) {
                        mtfIndex = decodedSymbol - 1
                        var mtfSubIdx: Int
                        if (mtfIndex < 16) {
                            mtfBaseIdx = entry.mtfmBase[0]

                            headerByte = entry.yy[mtfBaseIdx + mtfIndex]
                            while (mtfIndex > 3) {
                                mtfSubIdx = mtfBaseIdx + mtfIndex
                                entry.yy[mtfSubIdx] = entry.yy[mtfSubIdx - 1]
                                entry.yy[mtfSubIdx - 1] = entry.yy[mtfSubIdx - 2]
                                entry.yy[mtfSubIdx - 2] = entry.yy[mtfSubIdx - 3]
                                entry.yy[mtfSubIdx - 3] = entry.yy[mtfSubIdx - 4]
                                mtfIndex -= 4
                            }

                            while (mtfIndex > 0) {
                                entry.yy[mtfBaseIdx + mtfIndex] = entry.yy[mtfBaseIdx + mtfIndex - 1]
                                --mtfIndex
                            }

                            entry.yy[mtfBaseIdx] = headerByte
                        } else {
                            mtfBaseIdx = mtfIndex / 16
                            mtfSubIdx = mtfIndex % 16
                            var yyOffset = entry.mtfmBase[mtfBaseIdx] + mtfSubIdx

                            headerByte = entry.yy[yyOffset]
                            while (yyOffset > entry.mtfmBase[mtfBaseIdx]) {
                                entry.yy[yyOffset] = entry.yy[yyOffset - 1]
                                --yyOffset
                            }

                            ++entry.mtfmBase[mtfBaseIdx]

                            while (mtfBaseIdx > 0) {
                                --entry.mtfmBase[mtfBaseIdx]
                                entry.yy[entry.mtfmBase[mtfBaseIdx]] = entry.yy[entry.mtfmBase[mtfBaseIdx - 1] + 16 - 1]
                                --mtfBaseIdx
                            }

                            --entry.mtfmBase[0]
                            entry.yy[entry.mtfmBase[0]] = headerByte
                            if (entry.mtfmBase[0] == 0) {
                                alphaIdx = 4095

                                var blockIdx = 15
                                while (blockIdx >= 0) {
                                    var symIdx = 15
                                    while (symIdx >= 0) {
                                        entry.yy[alphaIdx] = entry.yy[entry.mtfmBase[blockIdx] + symIdx]
                                        --alphaIdx
                                        --symIdx
                                    }

                                    entry.mtfmBase[blockIdx] = alphaIdx + 1
                                    --blockIdx
                                }
                            }
                        }

                        ++entry.unzftab[entry.seqToUnseq[headerByte.toInt() and 0xff].toInt() and 0xff]
                        transformTable!![yyBlockIdx] = entry.seqToUnseq[headerByte.toInt() and 0xff].toInt() and 0xff
                        ++yyBlockIdx
                        if (alphaIndex == 0) {
                            ++currentLength
                            alphaIndex = 50
                            val nextSelector = entry.selector[currentLength]
                            minLen = entry.minLens[nextSelector.toInt()].toInt()
                            huffmanLimits = entry.limit[nextSelector.toInt()]
                            huffmanPerm = entry.perm[nextSelector.toInt()]
                            huffmanBase = entry.base[nextSelector.toInt()]
                        }

                        --alphaIndex
                        yySymIdx = minLen

                        codeValue = readBits(minLen, entry)
                        while (codeValue > huffmanLimits!![yySymIdx]) {
                            ++yySymIdx
                            decodedBit = readBit(entry)
                            codeValue = codeValue shl 1 or decodedBit.toInt()
                        }

                        decodedSymbol = huffmanPerm!![codeValue - huffmanBase!![yySymIdx]]
                    } else {
                        mtfIndex = -1
                        mtfBaseIdx = 1

                        do {
                            if (decodedSymbol == 0) {
                                mtfIndex += mtfBaseIdx
                            } else {
                                mtfIndex += mtfBaseIdx * 2
                            }

                            mtfBaseIdx *= 2
                            if (alphaIndex == 0) {
                                ++currentLength
                                alphaIndex = 50
                                val runSelector = entry.selector[currentLength]
                                minLen = entry.minLens[runSelector.toInt()].toInt()
                                huffmanLimits = entry.limit[runSelector.toInt()]
                                huffmanPerm = entry.perm[runSelector.toInt()]
                                huffmanBase = entry.base[runSelector.toInt()]
                            }

                            --alphaIndex
                            yySymIdx = minLen

                            codeValue = readBits(minLen, entry)
                            while (codeValue > huffmanLimits!![yySymIdx]) {
                                ++yySymIdx
                                decodedBit = readBit(entry)
                                codeValue = codeValue shl 1 or decodedBit.toInt()
                            }

                            decodedSymbol = huffmanPerm!![codeValue - huffmanBase!![yySymIdx]]
                        } while (decodedSymbol == 0 || decodedSymbol == 1)

                        ++mtfIndex
                        origPtrByte = entry.seqToUnseq[entry.yy[entry.mtfmBase[0]].toInt() and 0xff]

                        entry.unzftab[origPtrByte.toInt() and 0xff] += mtfIndex
                        while (mtfIndex > 0) {
                            transformTable!![yyBlockIdx] = origPtrByte.toInt() and 0xff
                            ++yyBlockIdx
                            --mtfIndex
                        }
                    }
                }

                entry.runCount = 0
                entry.currentByte = 0
                entry.cftab[0] = 0

                mtfIndex = 1
                while (mtfIndex <= 256) {
                    entry.cftab[mtfIndex] = entry.unzftab[mtfIndex - 1]
                    mtfIndex++
                }

                mtfIndex = 1
                while (mtfIndex <= 256) {
                    entry.cftab[mtfIndex] += entry.cftab[mtfIndex - 1]
                    mtfIndex++
                }

                mtfIndex = 0
                while (mtfIndex < yyBlockIdx) {
                    origPtrByte = (transformTable!![mtfIndex] and 0xff).toByte()
                    transformTable!![entry.cftab[origPtrByte.toInt() and 0xff]] = transformTable!![entry.cftab[origPtrByte.toInt() and 0xff]] or (mtfIndex shl 8)
                    ++entry.cftab[origPtrByte.toInt() and 0xff]
                    mtfIndex++
                }

                entry.bwtPointer = transformTable!![entry.origPtr] shr 8
                entry.blockPosition = 0
                entry.bwtPointer = transformTable!![entry.bwtPointer]
                entry.lastByte = (entry.bwtPointer and 0xff)
                entry.bwtPointer = entry.bwtPointer shr 8
                ++entry.blockPosition
                entry.blockSize = yyBlockIdx
                decodeBlock(entry)
                if (entry.blockSize + 1 == entry.blockPosition && entry.runCount == 0) {
                    hasMoreBlocks = true
                    break
                }

                hasMoreBlocks = false
                break
            }
        }
    }
}
