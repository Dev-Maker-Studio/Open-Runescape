package com.jagex.game.runetek5.buffer

interface ElementsBufferGL {
    fun getBufferAddress(): Long
    fun putElements(data: ByteArray, size: Int)
    fun getId(): Int
}
