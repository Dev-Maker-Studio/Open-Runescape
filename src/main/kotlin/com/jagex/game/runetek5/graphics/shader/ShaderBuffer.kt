package com.jagex.game.runetek5.graphics.shader

class ShaderBuffer(
    @JvmField val shaderData: ByteArray
) {
    @JvmField var currentPosition: Int = 0

    fun getEntryCount(): Int {
        var entryCount: Short = 0
        for (index in 0..1) {
            entryCount = (entryCount.toInt() or ((shaderData[currentPosition++].toInt() and 0xff) shl (index * 8))).toShort()
        }
        return entryCount.toInt()
    }

    fun readString(): String? {
        val length = getEntryCount()
        if (length == -1) {
            return null
        }
        if (length > 256) {
            throw ShaderBufferException()
        }
        val str = String(shaderData, currentPosition, length)
        currentPosition += length
        return str
    }
}
