package com.jagex.game.runetek5.sound

class Resampler(inputSampleRate: Int, outputSampleRate: Int) {

    @JvmField var inputSampleRate: Int = 0
    @JvmField var outputSampleRate: Int = 0
    @JvmField var resampleTable: Array<IntArray>? = null

    init {
        var inputRate = inputSampleRate
        var outputRate = outputSampleRate
        if (outputRate != inputRate) {
            val gcd = step(inputRate, outputRate)
            inputRate /= gcd
            outputRate /= gcd
            this.inputSampleRate = inputRate
            this.outputSampleRate = outputRate
            resampleTable = Array(inputRate) { IntArray(14) }
            for (phaseIndex in 0 until inputRate) {
                val coefficients = resampleTable!![phaseIndex]
                val centerPosition = phaseIndex.toDouble() / inputRate + 6.0
                var windowStart = (Math.floor(centerPosition - 7.0 + 1.0)).toInt()
                if (windowStart < 0) {
                    windowStart = 0
                }
                var windowEnd = (Math.ceil(7.0 + centerPosition)).toInt()
                if (windowEnd > 14) {
                    windowEnd = 14
                }
                val gainFactor = outputRate.toDouble() / inputRate
                var tapIndex = windowStart
                while (tapIndex < windowEnd) {
                    val sincArg = (tapIndex - centerPosition) * Math.PI
                    var tapValue = gainFactor
                    if (sincArg < -1.0E-4 || sincArg > 1.0E-4) {
                        tapValue = gainFactor * (Math.sin(sincArg) / sincArg)
                    }
                    tapValue *= 0.54 + 0.46 * Math.cos(0.2243994752564138 * (tapIndex - centerPosition))
                    coefficients[tapIndex] = (Math.floor(tapValue * 65536.0 + 0.5)).toInt()
                    tapIndex++
                }
            }
        }
    }

    fun resample(inputSignal: ByteArray): ByteArray {
        var outputSignal = inputSignal
        if (resampleTable != null) {
            val outputLength = ((outputSignal.size.toLong() * outputSampleRate / inputSampleRate) + 14).toInt()
            val resampledData = IntArray(outputLength)
            var outputIndex = 0
            var phaseAccumulator = 0
            for (inputIndex in outputSignal.indices) {
                val inputSample = outputSignal[inputIndex]
                val resampleCoefficients = resampleTable!![phaseAccumulator]
                for (tapIndex in 0 until 14) {
                    resampledData[outputIndex + tapIndex] += inputSample * resampleCoefficients[tapIndex]
                }
                phaseAccumulator += outputSampleRate
                val phaseAdvance = phaseAccumulator / inputSampleRate
                outputIndex += phaseAdvance
                phaseAccumulator -= phaseAdvance * inputSampleRate
            }
            outputSignal = ByteArray(outputLength)
            for (sampleIndex in 0 until outputLength) {
                val clampedSample = resampledData[sampleIndex] + 32768 shr 16
                outputSignal[sampleIndex] = when {
                    clampedSample < -128 -> -128
                    clampedSample > 127 -> 127
                    else -> clampedSample.toByte()
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
            var outputIndex = 0
            var phaseAccumulator = 0
            for (inputIndex in outputSignal.indices) {
                val inputSample = outputSignal[inputIndex]
                val resampleCoefficients = resampleTable!![phaseAccumulator]
                for (tapIndex in 0 until 14) {
                    resampledData[tapIndex + outputIndex] += resampleCoefficients[tapIndex] * inputSample shr 2
                }
                phaseAccumulator += outputSampleRate
                val phaseAdvance = phaseAccumulator / inputSampleRate
                outputIndex += phaseAdvance
                phaseAccumulator -= phaseAdvance * inputSampleRate
            }
            outputSignal = ShortArray(outputLength)
            for (sampleIndex in 0 until outputLength) {
                val resampledSample = resampledData[sampleIndex] + 8192 shr 14
                outputSignal[sampleIndex] = when {
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
