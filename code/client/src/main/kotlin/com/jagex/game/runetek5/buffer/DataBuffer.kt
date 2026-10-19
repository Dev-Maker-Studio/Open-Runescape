package com.jagex.game.runetek5.buffer

import java.nio.ByteBuffer
import java.util.Arrays

object DataBuffer {

    @JvmStatic
    fun createDataBuffer(data: ByteArray?): Any? {
        if (data == null) {
            return null
        }
        if (data.size > 136) {
            val bytebuffer = ByteBuffer.allocateDirect(data.size)
            bytebuffer.position(0)
            bytebuffer.put(data)
            return bytebuffer
        }
        return data
    }

    @JvmStatic
    fun unpackDataBuffer(dataBuffer: Any?, copyBuffer: Boolean): ByteArray? {
        if (dataBuffer == null) {
            return null
        }
        if (dataBuffer is ByteArray) {
            return if (copyBuffer) Arrays.copyOf(dataBuffer, dataBuffer.size) else dataBuffer
        }
        if (dataBuffer is ByteBuffer) {
            val data = ByteArray(dataBuffer.capacity())
            dataBuffer.position(0)
            dataBuffer.get(data)
            return data
        }
        throw IllegalArgumentException()
    }

    @JvmStatic
    fun readDataBuffer(`object`: Any?, position: Int): ByteArray? {
        if (`object` == null) {
            return null
        }
        if (`object` is ByteArray) {
            return Arrays.copyOfRange(`object`, position, 32768 + position)
        }
        if (`object` is ByteBuffer) {
            val data = ByteArray(32768)
            `object`.position(position)
            `object`.get(data, 0, 32768)
            return data
        }
        throw IllegalArgumentException()
    }
}
