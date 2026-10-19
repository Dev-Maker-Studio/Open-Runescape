package com.jagex.game.runetek5.graphics.texture.water

internal class NativeBillowTextureData : BillowNoiseGenerator() {
    @JvmField
    var data: ByteArray? = null

    fun getData(): ByteArray {
        data = ByteArray(524288)
        generateNoise()
        return data!!
    }

    override fun writeSample(index: Int, value: Byte) {
        var offset = index * 2
        var processedByte = (((value.toInt() and 0xff) shr 1) + 127).toByte()
        data!![offset++] = processedByte
        data!![offset] = processedByte
    }
}
