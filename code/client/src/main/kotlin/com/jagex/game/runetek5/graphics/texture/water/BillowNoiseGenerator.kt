package com.jagex.game.runetek5.graphics.texture.water

internal open class BillowNoiseGenerator : WaterTextureGenerator(12, 16, 2) {

    @JvmField var outputBuffer: ByteArray? = null
    @JvmField var writePosition: Int = 0
    @JvmField var accumulator: Int = 0
    @JvmField val amplitudeWeights: IntArray

    init {
        this.amplitudeWeights = IntArray(super.octaveCount)

        for (octave in 0 until super.octaveCount) {
            this.amplitudeWeights[octave] = (Math.pow(0.45, octave.toDouble()) * 4096.0).toInt()
        }
    }

    open fun writeSample(index: Int, value: Byte) {
        this.outputBuffer!![this.writePosition++] = ((value.toInt() and 0xff shr 1) + 127).toByte()
    }

    override fun update(octave: Int, noise: Int) {
        this.accumulator += noise * this.amplitudeWeights[octave] shr 12
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
