package com.jagex.game.runetek5.buffer

interface ArrayBufferGL {
    fun putArray(arraySize: Int, buffer: ByteArray, currentPosition: Int)
    fun getStride(): Int
    fun getId(): Int
    fun getBufferAddress(): Long
}
