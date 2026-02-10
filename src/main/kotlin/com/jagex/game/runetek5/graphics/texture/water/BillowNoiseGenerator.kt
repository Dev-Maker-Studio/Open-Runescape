package com.jagex.game.runetek5.graphics.texture.water

internal open class BillowNoiseGenerator : WaterTextureGenerator(12, 16, 2) {

    @JvmField var aByteArray7759: ByteArray? = null
    @JvmField var anInt7758: Int = 0
    @JvmField var anInt7760: Int = 0
    @JvmField val anIntArray7761: IntArray

    init {
        this.anIntArray7761 = IntArray(super.anInt4290)

        for (i_7 in 0 until super.anInt4290) {
            this.anIntArray7761[i_7] = (Math.pow(0.45, i_7.toDouble()) * 4096.0).toInt()
        }
    }

    open fun method12517(i_1: Int, b_2: Byte) {
        this.aByteArray7759!![this.anInt7758++] = ((b_2.toInt() and 0xff shr 1) + 127).toByte()
    }

    override fun update(i_1: Int, i_2: Int) {
        this.anInt7760 += i_2 * this.anIntArray7761[i_1] shr 12
    }

    override fun clear() {
        this.anInt7758 = 0
        this.anInt7760 = 0
    }

    override fun reset() {
        this.anInt7760 = Math.abs(this.anInt7760)
        if (this.anInt7760 >= 4096) {
            this.anInt7760 = 4095
        }

        method12517(this.anInt7758++, (this.anInt7760 shr 4).toByte())
        this.anInt7760 = 0
    }
}
