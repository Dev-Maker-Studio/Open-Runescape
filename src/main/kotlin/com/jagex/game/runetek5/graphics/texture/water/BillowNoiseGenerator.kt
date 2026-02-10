package com.jagex.game.runetek5.graphics.texture.water

internal open class BillowNoiseGenerator : WaterTextureGenerator(12, 16, 2) {

    @JvmField var outputBuffer: ByteArray? = null
    @JvmField var writePosition: Int = 0
    @JvmField var accumulator: Int = 0
    @JvmField val amplitudeWeights: IntArray

    init {
        this.amplitudeWeights = IntArray(super.octaveCount)

        for (i_7 in 0 until super.octaveCount) {
            this.amplitudeWeights[i_7] = (Math.pow(0.45, i_7.toDouble()) * 4096.0).toInt()
        }
    }

    open fun writeSample(i_1: Int, b_2: Byte) {
        this.outputBuffer!![this.writePosition++] = ((b_2.toInt() and 0xff shr 1) + 127).toByte()
    }

    override fun update(i_1: Int, i_2: Int) {
        this.accumulator += i_2 * this.amplitudeWeights[i_1] shr 12
    }

    override fun clear() {
        this.writePosition = 0
        this.accumulator = 0
    }

    override fun reset() {
        this.accumulator = Math.abs(this.accumulator)
        if (this.accumulator >= 4096) {
            this.accumulator = 4095
        }

        writeSample(this.writePosition++, (this.accumulator shr 4).toByte())
        this.accumulator = 0
    }
}
