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
    @JvmField var anInt8115: Int = 0
    @JvmField val aByteArrayArray8111: Array<ByteArray?> = arrayOfNulls(10)
    @JvmField val aNodeSub358112: Packet = Packet()
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
        if (anInt8115 >= values!!.size) {
            return -1
        }
        method13447()
        byteBuffer.buffer = bytes
        byteBuffer.currentPosition = 0
        do {
            if (byteBuffer.currentPosition >= byteBuffer.buffer.size) {
                byteBuffer.buffer = ByteArray(0)
                return bytes.size
            }
            if (aNodeSub358112.buffer.isEmpty()) {
                if (aByteArrayArray8111[0] == null) {
                    byteBuffer.buffer = ByteArray(0)
                    return byteBuffer.currentPosition
                }
                aNodeSub358112.buffer = aByteArrayArray8111[0]!!
            }
            val availableBuffer = byteBuffer.buffer.size - byteBuffer.currentPosition
            val availablePacket = aNodeSub358112.buffer.size - aNodeSub358112.currentPosition
            if (availableBuffer < availablePacket) {
                aNodeSub358112.readBytes(byteBuffer.buffer, byteBuffer.currentPosition, availableBuffer)
                byteBuffer.buffer = ByteArray(0)
                return bytes.size
            }
            byteBuffer.writeBytes(aNodeSub358112.buffer, aNodeSub358112.currentPosition, availablePacket)
            aNodeSub358112.buffer = ByteArray(0)
            aNodeSub358112.currentPosition = 0
            ++anInt8115
            System.arraycopy(aByteArrayArray8111, 1, aByteArrayArray8111, 0, 9)
            aByteArrayArray8111[9] = null
        } while (anInt8115 < values!!.size)
        byteBuffer.buffer = ByteArray(0)
        return byteBuffer.currentPosition
    }

    fun method13447() {
        if (values != null) {
            for (i in 0 until 10) {
                if (i + anInt8115 < values!!.size) {
                    if (aByteArrayArray8111[i] == null && resourceProvider.isCached(values!![i + anInt8115], 0)) {
                        aByteArrayArray8111[i] = resourceProvider.getFile(values!![i + anInt8115], 0)
                    }
                }
            }
        }
    }
}
