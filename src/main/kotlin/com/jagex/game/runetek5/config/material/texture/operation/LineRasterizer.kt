package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet

class LineRasterizer(
    @JvmField val anInt9376: Int,
    @JvmField val anInt9375: Int,
    @JvmField val anInt9377: Int,
    @JvmField val anInt9379: Int,
    i_5: Int,
    i_6: Int
) : TextureShapeRasterizer(-1, i_5, i_6) {

    override fun method2561(i_1: Int, i_2: Int) {
        val i_4 = this.anInt9376 * i_1 shr 12
        val i_5 = this.anInt9377 * i_1 shr 12
        val i_6 = i_2 * this.anInt9375 shr 12
        val i_7 = i_2 * this.anInt9379 shr 12
        TextureShapeRasterizer.method6159(i_4, i_6, i_5, i_7, super.anInt1741)
    }

    override fun method2556(i_1: Int, i_2: Int) {
    }

    override fun method2557(i_1: Int, i_2: Int) {
    }

    companion object {
        @JvmStatic
        fun decode(rsbytebuffer0: Packet): LineRasterizer {
            return LineRasterizer(rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readUMedium(), rsbytebuffer0.readUByte())
        }
    }
}
