package com.jagex.game.runetek5.graphics.texture.water

internal class GLWaveTextureData : WaveNoiseGenerator() {
    @JvmField
    var textureData: ByteArray? = null

    fun getData(): ByteArray {
        textureData = ByteArray(524288)
        generateNoise()
        return textureData!!
    }

    override fun set(index: Int, value: Byte) {
        var offset = index * 2
        val level = value.toInt() and 0xff
        textureData!![offset++] = (level * 3 shr 5).toByte()
        textureData!![offset] = (level * 3 shr 5).toByte()
    }
}
