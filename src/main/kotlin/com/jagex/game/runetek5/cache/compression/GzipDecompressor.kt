package com.jagex.game.runetek5.cache.compression

import com.jagex.game.runetek5.buffer.Packet
import java.util.zip.Inflater

class GzipDecompressor {
    @JvmField var inflater: Inflater? = null

    fun decompressCustomSize(compressed: ByteArray): ByteArray {
        val buffer = Packet(compressed)
        buffer.currentPosition = compressed.size - 4
        val size = buffer.readUIntLittle()
        val data = ByteArray(size)
        buffer.currentPosition = 0
        decompress(buffer, data)
        return data
    }

    fun decompress(stream: Packet, compressed: ByteArray) {
        if (stream.buffer[stream.currentPosition] == 31.toByte() && stream.buffer[stream.currentPosition + 1] == (-117).toByte()) {
            if (this.inflater == null) {
                this.inflater = Inflater(true)
            }

            try {
                this.inflater!!.setInput(stream.buffer, stream.currentPosition + 10, stream.buffer.size - (stream.currentPosition + 8 + 10))
                this.inflater!!.inflate(compressed)
            } catch (ex: Exception) {
                this.inflater!!.reset()
                throw RuntimeException("")
            }

            this.inflater!!.reset()
        } else {
            throw RuntimeException("")
        }
    }
}
