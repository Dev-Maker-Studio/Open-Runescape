package com.jagex.game.runetek5.graphics.texture.water

internal class Class370_Sub2_Sub1 : Class370_Sub2() {
    @JvmField
    var data: ByteArray? = null

    fun getData(): ByteArray {
        data = ByteArray(524288)
        method6322()
        return data!!
    }

    override fun method12517(i_1: Int, b_2: Byte) {
        var i_3 = i_1 * 2
        var processedByte = (((b_2.toInt() and 0xff) shr 1) + 127).toByte()
        data!![i_3++] = processedByte
        data!![i_3] = processedByte
    }
}
