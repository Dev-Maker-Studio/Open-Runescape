package com.jagex.game.runetek5.cache.compression

/**
 * Data container for Bzip2 decompression blocks.
 */
class Bzip2BlockEntry {

    @JvmField var anInt5496: Int = 0
    @JvmField var anInt5499: Int = 0
    @JvmField var compressed: ByteArray? = null
    @JvmField var anInt5505: Int = 0
    @JvmField var decompressedLength: Int = 0
    @JvmField var anInt5508: Int = 0
    @JvmField var aByte5509: Byte = 0
    @JvmField var anInt5511: Int = 0
    @JvmField var anInt5513: Int = 0
    @JvmField var anInt5514: Int = 0
    @JvmField var anInt5515: Int = 0
    @JvmField var anInt5516: Int = 0
    @JvmField var anInt5518: Int = 0
    @JvmField var anInt5520: Int = 0
    @JvmField var decompressed: ByteArray? = null
    @JvmField var anInt5532: Int = 0
    @JvmField var anInt5498: Int = 6
    @JvmField var anInt5500: Int = 50
    @JvmField var anInt5501: Int = 18002
    @JvmField var anInt5512: Int = 258
    @JvmField var anInt5521: Int = 16
    @JvmField var anInt5522: Int = 4096
    @JvmField var startOffset: Int = 0
    @JvmField var anInt5506: Int = 0
    @JvmField val unzftab: IntArray
    @JvmField val anIntArray5519: IntArray
    @JvmField val inUse: BooleanArray
    @JvmField val inUse16: BooleanArray
    @JvmField val seqToUnseq: ByteArray
    @JvmField val yy: ByteArray
    @JvmField val anIntArray5525: IntArray
    @JvmField val selector: ByteArray
    @JvmField val selectorMtf: ByteArray
    @JvmField val len: Array<ByteArray>
    @JvmField val limit: Array<IntArray>
    @JvmField val base: Array<IntArray>
    @JvmField val perm: Array<IntArray>
    @JvmField val minLens: IntArray

    init {
        unzftab = IntArray(256)
        anIntArray5519 = IntArray(257)
        inUse = BooleanArray(256)
        inUse16 = BooleanArray(16)
        seqToUnseq = ByteArray(256)
        yy = ByteArray(4096)
        anIntArray5525 = IntArray(16)
        selector = ByteArray(18002)
        selectorMtf = ByteArray(18002)
        len = Array(6) { ByteArray(258) }
        limit = Array(6) { IntArray(258) }
        base = Array(6) { IntArray(258) }
        perm = Array(6) { IntArray(258) }
        minLens = IntArray(6)
    }
}
