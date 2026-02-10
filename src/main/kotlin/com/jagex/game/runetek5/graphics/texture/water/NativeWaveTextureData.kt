package com.jagex.game.runetek5.graphics.texture.water

internal class NativeWaveTextureData : WaveNoiseGenerator() {
    @JvmField
    var textureData: ByteArray? = null

    fun getData(): ByteArray {
        textureData = ByteArray(524288)
        generateNoise()
        return textureData!!
    }

    override fun set(i_1: Int, b_2: Byte) {
        var i_3 = i_1 * 2
        val i_4 = b_2.toInt() and 0xff
        textureData!![i_3++] = -1
        textureData!![i_3] = (i_4 * 3 shr 5).toByte()
    }
}
