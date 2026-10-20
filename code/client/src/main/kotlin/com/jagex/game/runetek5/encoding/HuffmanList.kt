package com.jagex.game.runetek5.encoding

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.Utils

object HuffmanList {

    @JvmField var HUFFMAN: Huffman? = null

    @JvmStatic
    fun initialize(huffman: Huffman) {
        HUFFMAN = huffman
    }

    @JvmStatic
    fun decode(buffer: Packet): String {
        return decodeHuffmanMessage(buffer)
    }

    @JvmStatic
    fun decodeHuffmanMessage(buffer: Packet): String {
        return try {
            var size = buffer.readUSmart()
            if (size > 32767) {
                size = 32767
            }
            val data = ByteArray(size)
            buffer.currentPosition += HUFFMAN!!.calculateDecompressedLength(buffer.buffer, buffer.currentPosition, data, 0, size)
            Utils.readString(data, 0, size)
        } catch (exception: Exception) {
            "Cabbage"
        }
    }
}
