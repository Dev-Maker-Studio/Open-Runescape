package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

class TextureOp37 : TextureOperation(0, true) {

    @JvmField var anInt9830: Int = 2048
    @JvmField var anInt9829: Int = 0
    @JvmField var anInt9833: Int = 0
    @JvmField var anInt9831: Int = 2048
    @JvmField var anInt9832: Int = 12288
    @JvmField var anInt9828: Int = 4096
    @JvmField var anInt9834: Int = 8192

    override fun postDecode() {
        TextureRaster.caculateTable()
    }

    override fun decodeOpcode(i_1: Int, rsbytebuffer2: Packet) {
        when (i_1) {
            0 -> anInt9830 = rsbytebuffer2.readUShort()
            1 -> anInt9829 = rsbytebuffer2.readUShort()
            2 -> anInt9833 = rsbytebuffer2.readUShort()
            3 -> anInt9831 = rsbytebuffer2.readUShort()
            4 -> anInt9832 = rsbytebuffer2.readUShort()
            5 -> anInt9828 = rsbytebuffer2.readUShort()
            6 -> anInt9834 = rsbytebuffer2.readUShort()
        }
    }

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            val i_4 = TextureRaster.verticalTable!![i_1] - 2048

            for (i_5 in 0 until TextureRaster.currentWidth) {
                val i_6 = TextureRaster.horizontalTable!![i_5] - 2048
                var i_7 = i_6 + anInt9830
                i_7 = if (i_7 < -2048) i_7 + 4096 else i_7
                i_7 = if (i_7 > 2048) i_7 - 4096 else i_7
                var i_8 = i_4 + anInt9829
                i_8 = if (i_8 < -2048) i_8 + 4096 else i_8
                i_8 = if (i_8 > 2048) i_8 - 4096 else i_8
                var i_9 = i_6 + anInt9833
                i_9 = if (i_9 < -2048) i_9 + 4096 else i_9
                i_9 = if (i_9 > 2048) i_9 - 4096 else i_9
                var i_10 = i_4 + anInt9831
                i_10 = if (i_10 < -2048) i_10 + 4096 else i_10
                i_10 = if (i_10 > 2048) i_10 - 4096 else i_10
                ints_3[i_5] = if (!method15302(i_7, i_8) && !method15300(i_9, i_10)) 0 else 4096
            }
        }

        return ints_3
    }

    fun method15300(i_1: Int, i_2: Int): Boolean {
        val i_4 = (i_2 + i_1) * anInt9832 shr 12
        var i_5 = TextureRaster.PALETTE_COS!![i_4 * 255 shr 12 and 0xff]
        i_5 = (i_5 shl 12) / anInt9832
        i_5 = (i_5 shl 12) / anInt9834
        i_5 = i_5 * anInt9828 shr 12
        return i_2 - i_1 < i_5 && i_2 - i_1 > -i_5
    }

    fun method15302(i_1: Int, i_2: Int): Boolean {
        val i_4 = (i_2 - i_1) * anInt9832 shr 12
        var i_5 = TextureRaster.PALETTE_COS!![i_4 * 255 shr 12 and 0xff]
        i_5 = (i_5 shl 12) / anInt9832
        i_5 = (i_5 shl 12) / anInt9834
        i_5 = i_5 * anInt9828 shr 12
        return i_2 + i_1 < i_5 && i_2 + i_1 > -i_5
    }
}
