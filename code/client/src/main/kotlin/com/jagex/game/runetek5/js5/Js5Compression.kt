package com.jagex.game.runetek5.js5

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.compression.Bzip2Compressor
import com.jagex.game.runetek5.cache.compression.GzipDecompressor

object Js5Compression {

    @JvmField val GZIP = GzipDecompressor()
    @JvmField var maxSize: Int = 0

    @JvmStatic
    fun decompress(compressed: ByteArray): ByteArray {
        return try {
            val buffer = Packet(compressed)
            val compressionType = buffer.readUByte()
            val payloadLength = buffer.readInt()
            if (payloadLength >= 0 && (maxSize == 0 || payloadLength <= maxSize)) {
                if (compressionType == 0) {
                    val uncompressedPayload = ByteArray(payloadLength)
                    buffer.readBytes(uncompressedPayload, 0, payloadLength)
                    return uncompressedPayload
                }
                val decompressedLength = buffer.readInt()
                if (decompressedLength < 0 || maxSize != 0 && decompressedLength > maxSize) {
                    throw RuntimeException()
                }
                val decompressed = ByteArray(decompressedLength)
                if (compressionType == 1) {
                    Bzip2Compressor.decompress(decompressed, decompressedLength, compressed)
                } else {
                    synchronized(GZIP) {
                        GZIP.decompress(buffer, decompressed)
                    }
                }
                return decompressed
            }
            throw RuntimeException()
        } catch (throwable: Throwable) {
            ByteArray(100)
        }
    }
}
