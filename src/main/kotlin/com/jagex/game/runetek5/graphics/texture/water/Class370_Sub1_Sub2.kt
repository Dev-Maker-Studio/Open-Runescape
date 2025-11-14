package com.jagex.game.runetek5.graphics.texture.water

internal class Class370_Sub1_Sub2 : Class370_Sub1() {
    @JvmField
    var aByteArray10215: ByteArray? = null

    fun method15614(): ByteArray {
        aByteArray10215 = ByteArray(524288)
        method6322()
        return aByteArray10215!!
    }

    override fun set(i_1: Int, b_2: Byte) {
        var i_3 = i_1 * 2
        val i_4 = b_2.toInt() and 0xff
        aByteArray10215!![i_3++] = (i_4 * 3 shr 5).toByte()
        aByteArray10215!![i_3] = (i_4 * 3 shr 5).toByte()
    }
}
