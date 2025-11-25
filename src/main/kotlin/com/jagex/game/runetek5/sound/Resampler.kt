package com.jagex.game.runetek5.sound

/**
 * https://www.rune-server.ee/runescape-development/rs2-client/downloads/673441-167-osrs-sound-effects-dumped-wav-format-2.html
 * @author CrazyPanda
 *
 * @author Brainpower
 *
 */
class Resampler(inputSampleRate: Int, outputSampleRate: Int) {

    @JvmField var inputSampleRate: Int = 0
    @JvmField var outputSampleRate: Int = 0
    @JvmField var resampleTable: Array<IntArray>? = null

    init {
        var inputRate = inputSampleRate
        var outputRate = outputSampleRate
        if (outputRate != inputRate) {
            val i_3 = step(inputRate, outputRate)
            inputRate /= i_3
            outputRate /= i_3
            this.inputSampleRate = inputRate
            this.outputSampleRate = outputRate
            resampleTable = Array(inputRate) { IntArray(14) }
            for (i_4 in 0 until inputRate) {
                val ints_5 = resampleTable!![i_4]
                val d_6 = i_4.toDouble() / inputRate + 6.0
                var i_8 = (Math.floor(d_6 - 7.0 + 1.0)).toInt()
                if (i_8 < 0) {
                    i_8 = 0
                }
                var i_9 = (Math.ceil(7.0 + d_6)).toInt()
                if (i_9 > 14) {
                    i_9 = 14
                }
                val d_10 = outputRate.toDouble() / inputRate
                var i_8_loop = i_8
                while (i_8_loop < i_9) {
                    val d_12 = (i_8_loop - d_6) * Math.PI
                    var d_14 = d_10
                    if (d_12 < -1.0E-4 || d_12 > 1.0E-4) {
                        d_14 = d_10 * (Math.sin(d_12) / d_12)
                    }
                    d_14 *= 0.54 + 0.46 * Math.cos(0.2243994752564138 * (i_8_loop - d_6))
                    ints_5[i_8_loop] = (Math.floor(d_14 * 65536.0 + 0.5)).toInt()
                    i_8_loop++
                }
            }
        }
    }

    fun resample(inputSignal: ByteArray): ByteArray {
        var outputSignal = inputSignal
        if (resampleTable != null) {
            val outputLength = ((outputSignal.size.toLong() * outputSampleRate / inputSampleRate) + 14).toInt()
            val resampledData = IntArray(outputLength)
            var i = 0
            var j = 0
            for (k in outputSignal.indices) {
                val inputSample = outputSignal[k]
                val resampleCoefficients = resampleTable!![j]
                for (l in 0 until 14) {
                    resampledData[i + l] += inputSample * resampleCoefficients[l]
                }
                j += outputSampleRate
                val numInputSamples = j / inputSampleRate
                i += numInputSamples
                j -= numInputSamples * inputSampleRate
            }
            outputSignal = ByteArray(outputLength)
            for (k in 0 until outputLength) {
                val i_11 = resampledData[k] + 32768 shr 16
                outputSignal[k] = when {
                    i_11 < -128 -> -128
                    i_11 > 127 -> 127
                    else -> i_11.toByte()
                }
            }
        }
        return outputSignal
    }

    fun resample(inputSignal: ShortArray): ShortArray {
        var outputSignal = inputSignal
        if (resampleTable != null) {
            val outputLength = ((outputSignal.size.toLong() * outputSampleRate / inputSampleRate) + 14).toInt()
            val resampledData = IntArray(outputLength)
            var i = 0
            var j = 0
            for (k in outputSignal.indices) {
                val inputSample = outputSignal[k]
                val resampleCoefficients = resampleTable!![j]
                for (l in 0 until 14) {
                    resampledData[l + i] += resampleCoefficients[l] * inputSample shr 2
                }
                j += outputSampleRate
                val numInputSamples = j / inputSampleRate
                i += numInputSamples
                j -= numInputSamples * inputSampleRate
            }
            outputSignal = ShortArray(outputLength)
            for (k in 0 until outputLength) {
                val resampledSample = resampledData[k] + 8192 shr 14
                outputSignal[k] = when {
                    resampledSample < -32768 -> -32768
                    resampledSample > 32767 -> 32767
                    else -> resampledSample.toShort()
                }
            }
        }
        return outputSignal
    }

    fun scaleRate(sampleRate: Int): Int {
        var rate = sampleRate
        if (resampleTable != null) {
            rate = (rate.toLong() * outputSampleRate / inputSampleRate).toInt()
        }
        return rate
    }

    fun scalePosition(position: Int): Int {
        var pos = position
        if (resampleTable != null) {
            pos = (pos.toLong() * outputSampleRate / inputSampleRate).toInt() + 6
        }
        return pos
    }

    companion object {
        @JvmStatic
        fun step(inputSampleRate: Int, outputSampleRate: Int): Int {
            var input = inputSampleRate
            var output = outputSampleRate
            if (output > input) {
                val tmp = input
                input = output
                output = tmp
            }
            while (output != 0) {
                val tmp = input % output
                input = output
                output = tmp
            }
            return input
        }
    }
}
