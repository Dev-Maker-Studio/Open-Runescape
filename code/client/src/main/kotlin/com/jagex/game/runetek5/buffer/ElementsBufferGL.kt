package com.jagex.game.runetek5.buffer

/**
 *
 * @author Steven - 13/09/2023
 *
 */
interface ElementsBufferGL {
    fun getBufferAddress(): Long
    fun putElements(data: ByteArray, size: Int)
    fun getId(): Int
}
