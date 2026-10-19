package com.jagex.game.runetek5.toolkit.opengl.buffer

import com.jagex.game.runetek5.buffer.Packet
import jaclib.memory.Stream

/**
 *
 * @author Brainpower - 07/05/2022
 *
 */
class GlBitBuffer(size: Int) : Packet(size) {

    fun writeFloatLittleEndian(value: Float) {
        val rawIntBits = Stream.floatToRawIntBits(value)
        super.buffer[++super.currentPosition - 1] = rawIntBits.toByte()
        super.buffer[++super.currentPosition - 1] = (rawIntBits shr 8).toByte()
        super.buffer[++super.currentPosition - 1] = (rawIntBits shr 16).toByte()
        super.buffer[++super.currentPosition - 1] = (rawIntBits shr 24).toByte()
    }

    fun writeFloatBigEndian(value: Float) {
        val rawIntBits = Stream.floatToRawIntBits(value)
        super.buffer[++super.currentPosition - 1] = (rawIntBits shr 24).toByte()
        super.buffer[++super.currentPosition - 1] = (rawIntBits shr 16).toByte()
        super.buffer[++super.currentPosition - 1] = (rawIntBits shr 8).toByte()
        super.buffer[++super.currentPosition - 1] = rawIntBits.toByte()
    }
}
