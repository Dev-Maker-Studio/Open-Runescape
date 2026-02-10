package com.jagex.game.runetek5.cache.compression

/**
 * Data container for Bzip2 decompression blocks.
 */
class Bzip2BlockEntry {

    @JvmField var runCount: Int = 0
    @JvmField var bytesRead: Int = 0
    @JvmField var compressed: ByteArray? = null
    @JvmField var blockSize: Int = 0
    @JvmField var decompressedLength: Int = 0
    @JvmField var blockCrc: Int = 0
    @JvmField var currentByte: Byte = 0
    @JvmField var bitBuffer: Int = 0
    @JvmField var blockSizeMultiplier: Int = 0
    @JvmField var origPtr: Int = 0
    @JvmField var bwtPointer: Int = 0
    @JvmField var lastByte: Int = 0
    @JvmField var blockPosition: Int = 0
    @JvmField var symbolCount: Int = 0
    @JvmField var decompressed: ByteArray? = null
    @JvmField var bitBufferLength: Int = 0
    @JvmField var MAX_GROUPS: Int = 6
    @JvmField var GROUP_SIZE: Int = 50
    @JvmField var MAX_SELECTORS: Int = 18002
    @JvmField var MAX_SYMBOLS: Int = 258
    @JvmField var MAX_ALPHA_SIZE_DIV_16: Int = 16
    @JvmField var YY_SIZE: Int = 4096
    @JvmField var startOffset: Int = 0
    @JvmField var outputPosition: Int = 0
    @JvmField val unzftab: IntArray
    @JvmField val cftab: IntArray
    @JvmField val inUse: BooleanArray
    @JvmField val inUse16: BooleanArray
    @JvmField val seqToUnseq: ByteArray
    @JvmField val yy: ByteArray
    @JvmField val mtfmBase: IntArray
    @JvmField val selector: ByteArray
    @JvmField val selectorMtf: ByteArray
    @JvmField val len: Array<ByteArray>
    @JvmField val limit: Array<IntArray>
    @JvmField val base: Array<IntArray>
    @JvmField val perm: Array<IntArray>
    @JvmField val minLens: IntArray

    init {
        unzftab = IntArray(256)
        cftab = IntArray(257)
        inUse = BooleanArray(256)
        inUse16 = BooleanArray(16)
        seqToUnseq = ByteArray(256)
        yy = ByteArray(4096)
        mtfmBase = IntArray(16)
        selector = ByteArray(18002)
        selectorMtf = ByteArray(18002)
        len = Array(6) { ByteArray(258) }
        limit = Array(6) { IntArray(258) }
        base = Array(6) { IntArray(258) }
        perm = Array(6) { IntArray(258) }
        minLens = IntArray(6)
    }
}
