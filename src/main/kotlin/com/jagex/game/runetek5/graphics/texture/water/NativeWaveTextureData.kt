package com.jagex.game.runetek5.graphics.texture.water

internal class NativeWaveTextureData : WaveNoiseGenerator() {
    @JvmField
    var aByteArray9998: ByteArray? = null

    fun getData(): ByteArray {
        aByteArray9998 = ByteArray(524288)
        method6322()
        return aByteArray9998!!
    }

    override fun set(i_1: Int, b_2: Byte) {
        var i_3 = i_1 * 2
        val i_4 = b_2.toInt() and 0xff
        aByteArray9998!![i_3++] = -1
        aByteArray9998!![i_3] = (i_4 * 3 shr 5).toByte()
    }
}
