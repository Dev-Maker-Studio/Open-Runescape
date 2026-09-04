package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.js5.Js5ResourceProvider

/**
 * Reference to a Vorbis audio file with streaming support.
 */
class VorbisFileReference(
    @JvmField val resourceProvider: Js5ResourceProvider,
    @JvmField val fileId: Int
) : JagTheoraManager() {

    @JvmField var values: IntArray? = null
    @JvmField var currentChunkIndex: Int = 0
    @JvmField val chunkBuffers: Array<ByteArray?> = arrayOfNulls(10)
    @JvmField val currentChunkPacket: Packet = Packet()
    @JvmField val byteBuffer: Packet = Packet()

    override fun decode(bytes: ByteArray): Int {
        if (values == null) {
            if (!resourceProvider.isCached(fileId, 0)) {
                return 0
            }
            val data = resourceProvider.getFile(fileId, 0) ?: throw IllegalStateException("")
            byteBuffer.buffer = data
            byteBuffer.currentPosition = 0
            val len = data.size shr 1
            values = IntArray(len)
            for (i in 0 until len) {
                values!![i] = byteBuffer.readUShort()
            }
        }
        if (currentChunkIndex >= values!!.size) {
            return -1
        }
        prefetchChunks()
        byteBuffer.buffer = bytes
        byteBuffer.currentPosition = 0
        do {
            if (byteBuffer.currentPosition >= byteBuffer.buffer.size) {
                byteBuffer.buffer = ByteArray(0)
                return bytes.size
            }
            if (currentChunkPacket.buffer.isEmpty()) {
                if (chunkBuffers[0] == null) {
                    byteBuffer.buffer = ByteArray(0)
                    return byteBuffer.currentPosition
                }
                currentChunkPacket.buffer = chunkBuffers[0]!!
            }
            val availableBuffer = byteBuffer.buffer.size - byteBuffer.currentPosition
            val availablePacket = currentChunkPacket.buffer.size - currentChunkPacket.currentPosition
            if (availableBuffer < availablePacket) {
                currentChunkPacket.readBytes(byteBuffer.buffer, byteBuffer.currentPosition, availableBuffer)
                byteBuffer.buffer = ByteArray(0)
                return bytes.size
            }
            byteBuffer.writeBytes(currentChunkPacket.buffer, currentChunkPacket.currentPosition, availablePacket)
            currentChunkPacket.buffer = ByteArray(0)
            currentChunkPacket.currentPosition = 0
            ++currentChunkIndex
            System.arraycopy(chunkBuffers, 1, chunkBuffers, 0, 9)
            chunkBuffers[9] = null
        } while (currentChunkIndex < values!!.size)
        byteBuffer.buffer = ByteArray(0)
        return byteBuffer.currentPosition
    }

    fun prefetchChunks() {
        if (values != null) {
            for (i in 0 until 10) {
                if (i + currentChunkIndex < values!!.size) {
                    if (chunkBuffers[i] == null && resourceProvider.isCached(values!![i + currentChunkIndex], 0)) {
                        chunkBuffers[i] = resourceProvider.getFile(values!![i + currentChunkIndex], 0)
                    }
                }
            }
        }
    }
}
