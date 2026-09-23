package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.sound.player.PcmPlayerManager

open class AudioBufferStream(
    buffer: AudioBuffer,
    sampleRate: Int,
    volume: Int,
    pan: Int
) : AreaSoundStream() {

    init {
        this.buffer = buffer
        this.start = buffer.start
        this.end = buffer.end
        this.enableLoop = buffer.enableLoop
        this.sampleRate = sampleRate
        this.targetVolume = volume
        this.panPosition = pan
        this.playbackPosition = 0
        recalculateChannelVolumes()
    }

    companion object {
        @JvmStatic
        fun mixBackwardStereoInterpolated(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            leftVolume: Int,
            rightVolume: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream,
            step: Int,
            wrapSample: Float
        ): Int {
            var sampleIndex = 0
            var sampleLimit: Int
            var interpolated = 0.0f
            var posVar = position
            var outVar = outputIndex
            var leftVol = leftVolume * 128
            var rightVol = rightVolume * 128
            sampleLimit = if (step == 0) maxOutputIndex else outVar + (endPosition + 256 - posVar + step) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            outVar = outVar shl 1
            var bufIdx: Int
            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                sampleIndex = posVar shr 8
                interpolated = sampleData[sampleIndex - 1]
                interpolated = interpolated * 256.0f + (sampleData[sampleIndex] - interpolated) * (posVar and 0xff)
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * leftVol).toInt() shr 6
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * rightVol).toInt() shr 6
                posVar += step
            }

            sampleLimit = if (step == 0) maxOutputIndex else (outVar shr 1) + (endPosition - posVar + step) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                interpolated = wrapSample * 256.0f + (sampleData[posVar shr 8] - wrapSample) * (posVar and 0xff)
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * leftVol).toInt() shr 6
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * rightVol).toInt() shr 6
                posVar += step
            }

            stream.playbackPosition = posVar
            return outVar shr 1
        }

        @JvmStatic
        fun mixBackwardStereoConstant(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            leftVolume: Int,
            rightVolume: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream
        ): Int {
            var sampleLimit: Int
            var sample = 0.0f
            var samplePos = position shr 8
            var endPos = endPosition shr 8
            var leftVol = leftVolume shl 2
            var rightVol = rightVolume shl 2
            leftVol *= 128
            rightVol *= 128
            sampleLimit = samplePos + outputIndex - (endPos - 1)
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var outVar = outputIndex shl 1
            sampleLimit = sampleLimit shl 1

            var bufIdx: Int
            sampleLimit -= 6
            while (outVar < sampleLimit) {
                sample = sampleData[samplePos--]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                sample = sampleData[samplePos--]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                sample = sampleData[samplePos--]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                sample = sampleData[samplePos--]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
            }

            sampleLimit += 6
            while (outVar < sampleLimit) {
                sample = sampleData[samplePos--]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
            }

            stream.playbackPosition = samplePos shl 8
            return outVar shr 1
        }

        @JvmStatic
        fun mixBackwardMonoInterpolated(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            volume: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream,
            step: Int,
            wrapSample: Float
        ): Int {
            var sampleIndex = 0
            var sampleLimit: Int
            var interpolated = 0.0f
            var scaledVolume = volume * 128
            var posVar = position
            var outVar = outputIndex
            sampleLimit = if (step == 0) maxOutputIndex else outVar + (step + (endPosition + 256 - posVar)) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var bufIdx: Int
            while (outVar < sampleLimit) {
                sampleIndex = posVar shr 8
                interpolated = sampleData[sampleIndex - 1]
                bufIdx = outVar++
                mixBuffer[bufIdx] += ((interpolated * 256.0f + (sampleData[sampleIndex] - interpolated) * (posVar and 0xff)) * scaledVolume).toInt() shr 6
                posVar += step
            }

            sampleLimit = if (step == 0) maxOutputIndex else outVar + (step + (endPosition - posVar)) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            interpolated = wrapSample
            while (outVar < sampleLimit) {
                bufIdx = outVar++
                mixBuffer[bufIdx] += ((interpolated * 256.0f + (sampleData[posVar shr 8] - interpolated) * (posVar and 0xff)) * scaledVolume).toInt() shr 6
                posVar += step
            }

            stream.playbackPosition = posVar
            return outVar
        }

        @JvmStatic
        fun mixForwardMonoInterpolatedFading(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            volume: Int,
            volumeDelta: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream,
            step: Int,
            wrapSample: Float
        ): Int {
            var sampleIndex = 0
            var sampleLimit: Int
            var interpolated = 0.0f
            var scaledVolume = volume * 128
            var scaledVolumeDelta = volumeDelta * 128
            var outVar = outputIndex
            var posVar = position
            stream.leftChannelVolume -= stream.leftVolumeFadeDelta * outVar
            stream.rightChannelVolume -= stream.rightVolumeFadeDelta * outVar
            sampleLimit = if (step == 0) maxOutputIndex else outVar + (endPosition - posVar + step - 257) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var bufIdx: Int
            while (outVar < sampleLimit) {
                sampleIndex = posVar shr 8
                interpolated = sampleData[sampleIndex]
                bufIdx = outVar++
                mixBuffer[bufIdx] += ((interpolated * 256.0f + (sampleData[sampleIndex + 1] - interpolated) * (posVar and 0xff)) * scaledVolume).toInt() shr 6
                scaledVolume += scaledVolumeDelta
                posVar += step
            }

            sampleLimit = if (step == 0) maxOutputIndex else outVar + (endPosition - posVar + step - 1) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            while (outVar < sampleLimit) {
                interpolated = sampleData[posVar shr 8]
                bufIdx = outVar++
                mixBuffer[bufIdx] += ((interpolated * 256.0f + (wrapSample - interpolated) * (posVar and 0xff)) * scaledVolume).toInt() shr 6
                scaledVolume += scaledVolumeDelta
                posVar += step
            }

            stream.leftChannelVolume += stream.leftVolumeFadeDelta * outVar
            stream.rightChannelVolume += stream.rightVolumeFadeDelta * outVar
            stream.currentVolume = scaledVolume / 128
            stream.playbackPosition = posVar
            return outVar
        }

        @JvmStatic
        fun mixForwardStereoConstantFading(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            leftVolume: Int,
            rightVolume: Int,
            leftVolumeDelta: Int,
            rightVolumeDelta: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream
        ): Int {
            var sampleLimit: Int
            var sample = 0.0f
            var samplePos = position shr 8
            var endPos = endPosition shr 8
            var leftVol = leftVolume shl 2
            var rightVol = rightVolume shl 2
            var leftDelta = leftVolumeDelta shl 2
            var rightDelta = rightVolumeDelta shl 2
            leftVol *= 128
            leftDelta *= 128
            rightVol *= 128
            rightDelta *= 128
            sampleLimit = endPos + outputIndex - samplePos
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            stream.currentVolume += stream.volumeFadeDelta * (sampleLimit - outputIndex)
            var outVar = outputIndex shl 1
            sampleLimit = sampleLimit shl 1

            var bufIdx: Int
            sampleLimit -= 6
            while (outVar < sampleLimit) {
                sample = sampleData[samplePos++]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                rightVol += rightDelta
                sample = sampleData[samplePos++]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                rightVol += rightDelta
                sample = sampleData[samplePos++]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                rightVol += rightDelta
                sample = sampleData[samplePos++]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                rightVol += rightDelta
            }

            sampleLimit += 6
            while (outVar < sampleLimit) {
                sample = sampleData[samplePos++]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                rightVol += rightDelta
            }

            stream.leftChannelVolume = leftVol / 128 shr 2
            stream.rightChannelVolume = rightVol / 128 shr 2
            stream.playbackPosition = samplePos shl 8
            return outVar shr 1
        }

        @JvmStatic
        fun mixBackwardMonoConstantFading(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            volume: Int,
            volumeDelta: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream
        ): Int {
            var sampleLimit: Int
            var samplePos = position shr 8
            var endPos = endPosition shr 8
            var scaledVolume = volume shl 2
            var scaledDelta = volumeDelta shl 2
            scaledVolume *= 128
            scaledDelta *= 128
            sampleLimit = outputIndex + samplePos - (endPos - 1)
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            stream.leftChannelVolume += stream.leftVolumeFadeDelta * (sampleLimit - outputIndex)
            stream.rightChannelVolume += stream.rightVolumeFadeDelta * (sampleLimit - outputIndex)

            var bufIdx: Int
            var outVar = outputIndex
            sampleLimit -= 3
            while (outVar < sampleLimit) {
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos--] * scaledVolume).toInt()
                scaledVolume += scaledDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos--] * scaledVolume).toInt()
                scaledVolume += scaledDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos--] * scaledVolume).toInt()
                scaledVolume += scaledDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos--] * scaledVolume).toInt()
                scaledVolume += scaledDelta
            }

            sampleLimit += 3
            while (outVar < sampleLimit) {
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos--] * scaledVolume).toInt()
                scaledVolume += scaledDelta
            }

            stream.currentVolume = scaledVolume / 128 shr 2
            stream.playbackPosition = samplePos shl 8
            return outVar
        }

        @JvmStatic
        fun mixBackwardStereoConstantFading(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            leftVolume: Int,
            rightVolume: Int,
            leftVolumeDelta: Int,
            rightVolumeDelta: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream
        ): Int {
            var sampleLimit: Int
            var sample = 0.0f
            var samplePos = position shr 8
            var endPos = endPosition shr 8
            var leftVol = leftVolume shl 2
            var rightVol = rightVolume shl 2
            var leftDelta = leftVolumeDelta shl 2
            var rightDelta = rightVolumeDelta shl 2
            leftVol *= 128
            leftDelta *= 128
            rightVol *= 128
            rightDelta *= 128
            sampleLimit = samplePos + outputIndex - (endPos - 1)
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            stream.currentVolume += stream.volumeFadeDelta * (sampleLimit - outputIndex)
            var outVar = outputIndex shl 1
            sampleLimit = sampleLimit shl 1

            var bufIdx: Int
            sampleLimit -= 6
            while (outVar < sampleLimit) {
                sample = sampleData[samplePos--]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                rightVol += rightDelta
                sample = sampleData[samplePos--]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                rightVol += rightDelta
                sample = sampleData[samplePos--]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                rightVol += rightDelta
                sample = sampleData[samplePos--]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                rightVol += rightDelta
            }

            sampleLimit += 6
            while (outVar < sampleLimit) {
                sample = sampleData[samplePos--]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                rightVol += rightDelta
            }

            stream.leftChannelVolume = leftVol / 128 shr 2
            stream.rightChannelVolume = rightVol / 128 shr 2
            stream.playbackPosition = samplePos shl 8
            return outVar shr 1
        }

        @JvmStatic
        fun mixForwardMonoConstant(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            volume: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream
        ): Int {
            var sampleLimit: Int
            var samplePos = position shr 8
            var endPos = endPosition shr 8
            var scaledVolume = volume shl 2
            scaledVolume *= 128
            sampleLimit = outputIndex + endPos - samplePos
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var bufIdx: Int
            var outVar = outputIndex
            sampleLimit -= 3
            while (outVar < sampleLimit) {
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos++] * scaledVolume).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos++] * scaledVolume).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos++] * scaledVolume).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos++] * scaledVolume).toInt()
            }

            sampleLimit += 3
            while (outVar < sampleLimit) {
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos++] * scaledVolume).toInt()
            }

            stream.playbackPosition = samplePos shl 8
            return outVar
        }

        @JvmStatic
        fun mixForwardStereoInterpolatedFading(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            leftVolume: Int,
            rightVolume: Int,
            leftVolumeDelta: Int,
            rightVolumeDelta: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream,
            step: Int,
            wrapSample: Float
        ): Int {
            var sampleIndex = 0
            var sampleLimit: Int
            var interpolated = 0.0f
            var leftVol = leftVolume * 128
            var leftDelta = leftVolumeDelta * 128
            var rightVol = rightVolume * 128
            var rightDelta = rightVolumeDelta * 128
            var outVar = outputIndex
            var posVar = position
            stream.currentVolume -= outVar * stream.volumeFadeDelta
            sampleLimit = if (step == 0) maxOutputIndex else outVar + (endPosition - posVar + step - 257) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            outVar = outVar shl 1
            var bufIdx: Int
            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                sampleIndex = posVar shr 8
                interpolated = sampleData[sampleIndex]
                interpolated = interpolated * 256.0f + (sampleData[sampleIndex + 1] - interpolated) * (posVar and 0xff)
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * leftVol).toInt() shr 6
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * rightVol).toInt() shr 6
                rightVol += rightDelta
                posVar += step
            }

            sampleLimit = if (step == 0) maxOutputIndex else (outVar shr 1) + (endPosition - posVar + step - 1) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                interpolated = sampleData[posVar shr 8]
                interpolated = interpolated * 256.0f + (wrapSample - interpolated) * (posVar and 0xff)
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * leftVol).toInt() shr 6
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * rightVol).toInt() shr 6
                rightVol += rightDelta
                posVar += step
            }

            outVar = outVar shr 1
            stream.currentVolume += stream.volumeFadeDelta * outVar
            stream.leftChannelVolume = leftVol / 128
            stream.rightChannelVolume = rightVol / 128
            stream.playbackPosition = posVar
            return outVar
        }

        @JvmStatic
        fun mixBackwardMonoInterpolatedFading(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            volume: Int,
            volumeDelta: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream,
            step: Int,
            wrapSample: Float
        ): Int {
            var sampleIndex = 0
            var sampleLimit: Int
            var interpolated = 0.0f
            var scaledVolume = volume * 128
            var scaledVolumeDelta = volumeDelta * 128
            var outVar = outputIndex
            var posVar = position
            stream.leftChannelVolume -= stream.leftVolumeFadeDelta * outVar
            stream.rightChannelVolume -= stream.rightVolumeFadeDelta * outVar
            sampleLimit = if (step == 0) maxOutputIndex else outVar + (endPosition + 256 - posVar + step) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var bufIdx: Int
            while (outVar < sampleLimit) {
                sampleIndex = posVar shr 8
                interpolated = sampleData[sampleIndex - 1]
                bufIdx = outVar++
                mixBuffer[bufIdx] += ((interpolated * 256.0f + (sampleData[sampleIndex] - interpolated) * (posVar and 0xff)) * scaledVolume).toInt() shr 6
                scaledVolume += scaledVolumeDelta
                posVar += step
            }

            sampleLimit = if (step == 0) maxOutputIndex else outVar + (endPosition - posVar + step) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            interpolated = wrapSample
            while (outVar < sampleLimit) {
                bufIdx = outVar++
                mixBuffer[bufIdx] += ((interpolated * 256.0f + (sampleData[posVar shr 8] - interpolated) * (posVar and 0xff)) * scaledVolume).toInt() shr 6
                scaledVolume += scaledVolumeDelta
                posVar += step
            }

            stream.leftChannelVolume += stream.leftVolumeFadeDelta * outVar
            stream.rightChannelVolume += stream.rightVolumeFadeDelta * outVar
            stream.currentVolume = scaledVolume / 128
            stream.playbackPosition = posVar
            return outVar
        }

        @JvmStatic
        fun mixBackwardStereoInterpolatedFading(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            leftVolume: Int,
            rightVolume: Int,
            leftVolumeDelta: Int,
            rightVolumeDelta: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream,
            step: Int,
            wrapSample: Float
        ): Int {
            var sampleIndex = 0
            var sampleLimit: Int
            var interpolated = 0.0f
            var leftVol = leftVolume * 128
            var leftDelta = leftVolumeDelta * 128
            var rightVol = rightVolume * 128
            var rightDelta = rightVolumeDelta * 128
            var outVar = outputIndex
            var posVar = position
            stream.currentVolume -= outVar * stream.volumeFadeDelta
            sampleLimit = if (step == 0) maxOutputIndex else outVar + (endPosition + 256 - posVar + step) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            outVar = outVar shl 1
            var bufIdx: Int
            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                sampleIndex = posVar shr 8
                interpolated = sampleData[sampleIndex - 1]
                interpolated = interpolated * 256.0f + (sampleData[sampleIndex] - interpolated) * (posVar and 0xff)
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * leftVol).toInt() shr 6
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * rightVol).toInt() shr 6
                rightVol += rightDelta
                posVar += step
            }

            sampleLimit = if (step == 0) maxOutputIndex else (outVar shr 1) + (endPosition - posVar + step) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                interpolated = wrapSample * 256.0f + (sampleData[posVar shr 8] - wrapSample) * (posVar and 0xff)
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * leftVol).toInt() shr 6
                leftVol += leftDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * rightVol).toInt() shr 6
                rightVol += rightDelta
                posVar += step
            }

            outVar = outVar shr 1
            stream.currentVolume += stream.volumeFadeDelta * outVar
            stream.leftChannelVolume = leftVol / 128
            stream.rightChannelVolume = rightVol / 128
            stream.playbackPosition = posVar
            return outVar
        }

        @JvmStatic
        fun mixForwardStereoInterpolated(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            leftVolume: Int,
            rightVolume: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream,
            step: Int,
            wrapSample: Float
        ): Int {
            var sampleIndex = 0
            var sampleLimit: Int
            var interpolated = 0.0f
            var leftVol = leftVolume * 128
            var rightVol = rightVolume * 128
            var outVar = outputIndex
            var posVar = position
            sampleLimit = if (step == 0) maxOutputIndex else outVar + (endPosition - posVar + step - 257) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            outVar = outVar shl 1
            var bufIdx: Int
            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                sampleIndex = posVar shr 8
                interpolated = sampleData[sampleIndex]
                interpolated = interpolated * 256.0f + (sampleData[sampleIndex + 1] - interpolated) * (posVar and 0xff)
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * leftVol).toInt() shr 6
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * rightVol).toInt() shr 6
                posVar += step
            }

            sampleLimit = if (step == 0) maxOutputIndex else (outVar shr 1) + (endPosition - posVar + step - 1) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            sampleLimit = sampleLimit shl 1
            while (outVar < sampleLimit) {
                interpolated = sampleData[posVar shr 8]
                interpolated = interpolated * 256.0f + (wrapSample - interpolated) * (posVar and 0xff)
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * leftVol).toInt() shr 6
                bufIdx = outVar++
                mixBuffer[bufIdx] += (interpolated * rightVol).toInt() shr 6
                posVar += step
            }

            stream.playbackPosition = posVar
            return outVar shr 1
        }

        @JvmStatic
        fun mixForwardMonoInterpolated(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            volume: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream,
            step: Int,
            wrapSample: Float
        ): Int {
            var sampleIndex = 0
            var sampleLimit: Int
            var interpolated = 0.0f
            var scaledVolume = volume * 128
            var outVar = outputIndex
            var posVar = position
            sampleLimit = if (step == 0) maxOutputIndex else outVar + (step + (endPosition - posVar) - 257) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var bufIdx: Int
            while (outVar < sampleLimit) {
                sampleIndex = posVar shr 8
                interpolated = sampleData[sampleIndex]
                bufIdx = outVar++
                mixBuffer[bufIdx] += ((interpolated * 256.0f + (sampleData[sampleIndex + 1] - interpolated) * (posVar and 0xff)) * scaledVolume).toInt() shr 6
                posVar += step
            }

            sampleLimit = if (step == 0) maxOutputIndex else outVar + (step + (endPosition - posVar) - 1) / step
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            while (outVar < sampleLimit) {
                interpolated = sampleData[posVar shr 8]
                bufIdx = outVar++
                mixBuffer[bufIdx] += ((interpolated * 256.0f + (wrapSample - interpolated) * (posVar and 0xff)) * scaledVolume).toInt() shr 6
                posVar += step
            }

            stream.playbackPosition = posVar
            return outVar
        }

        @JvmStatic
        fun mixForwardStereoConstant(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            leftVolume: Int,
            rightVolume: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream
        ): Int {
            var sampleLimit: Int
            var sample = 0.0f
            var samplePos = position shr 8
            var endPos = endPosition shr 8
            var leftVol = leftVolume shl 2
            var rightVol = rightVolume shl 2
            leftVol *= 128
            rightVol *= 128
            sampleLimit = outputIndex + endPos - samplePos
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var outVar = outputIndex shl 1
            sampleLimit = sampleLimit shl 1

            var bufIdx: Int
            sampleLimit -= 6
            while (outVar < sampleLimit) {
                sample = sampleData[samplePos++]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                sample = sampleData[samplePos++]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                sample = sampleData[samplePos++]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
                sample = sampleData[samplePos++]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
            }

            sampleLimit += 6
            while (outVar < sampleLimit) {
                sample = sampleData[samplePos++]
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * leftVol).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sample * rightVol).toInt()
            }

            stream.playbackPosition = samplePos shl 8
            return outVar shr 1
        }

        @JvmStatic
        fun mixBackwardMonoConstant(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            volume: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream
        ): Int {
            var sampleLimit: Int
            var samplePos = position shr 8
            var endPos = endPosition shr 8
            var scaledVolume = volume shl 2
            scaledVolume *= 128
            sampleLimit = outputIndex + samplePos - (endPos - 1)
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            var bufIdx: Int
            var outVar = outputIndex
            sampleLimit -= 3
            while (outVar < sampleLimit) {
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos--] * scaledVolume).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos--] * scaledVolume).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos--] * scaledVolume).toInt()
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos--] * scaledVolume).toInt()
            }

            sampleLimit += 3
            while (outVar < sampleLimit) {
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos--] * scaledVolume).toInt()
            }

            stream.playbackPosition = samplePos shl 8
            return outVar
        }

        @JvmStatic
        fun mixForwardMonoConstantFading(
            sampleData: FloatArray,
            mixBuffer: IntArray,
            position: Int,
            outputIndex: Int,
            volume: Int,
            volumeDelta: Int,
            maxOutputIndex: Int,
            endPosition: Int,
            stream: AudioBufferStream
        ): Int {
            var sampleLimit: Int
            var samplePos = position shr 8
            var endPos = endPosition shr 8
            var scaledVolume = volume shl 2
            var scaledDelta = volumeDelta shl 2
            scaledVolume *= 128
            scaledDelta *= 128
            sampleLimit = outputIndex + endPos - samplePos
            if (sampleLimit > maxOutputIndex) {
                sampleLimit = maxOutputIndex
            }

            stream.leftChannelVolume += stream.leftVolumeFadeDelta * (sampleLimit - outputIndex)
            stream.rightChannelVolume += stream.rightVolumeFadeDelta * (sampleLimit - outputIndex)

            var bufIdx: Int
            var outVar = outputIndex
            sampleLimit -= 3
            while (outVar < sampleLimit) {
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos++] * scaledVolume).toInt()
                scaledVolume += scaledDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos++] * scaledVolume).toInt()
                scaledVolume += scaledDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos++] * scaledVolume).toInt()
                scaledVolume += scaledDelta
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos++] * scaledVolume).toInt()
                scaledVolume += scaledDelta
            }

            sampleLimit += 3
            while (outVar < sampleLimit) {
                bufIdx = outVar++
                mixBuffer[bufIdx] = (mixBuffer[bufIdx] + sampleData[samplePos++] * scaledVolume).toInt()
                scaledVolume += scaledDelta
            }

            stream.currentVolume = scaledVolume / 128 shr 2
            stream.playbackPosition = samplePos shl 8
            return outVar
        }
    }

    @Synchronized
    override fun fill(mixBuffer: IntArray, offset: Int, length: Int) {
        if (super.targetVolume == 0 && super.fadeSamplesRemaining == 0) {
            skip(length)
        } else {
            var startPos = super.start shl 8
            var endPos = super.end shl 8
            val totalSamples = ((buffer as AudioBuffer).getTotalSamples()) shl 8
            val loopLength = endPos - startPos
            if (loopLength <= 0) {
                super.loopsRemaining = 0
            }

            var currentOffset = offset
            var endOffset = length + offset
            if (super.playbackPosition < 0) {
                if (super.sampleRate <= 0) {
                    cancelFade()
                    unlink()
                    return
                }

                super.playbackPosition = 0
            }

            if (super.playbackPosition >= totalSamples) {
                if (super.sampleRate >= 0) {
                    cancelFade()
                    unlink()
                    return
                }

                super.playbackPosition = totalSamples - 1
            }

            if (super.loopsRemaining < 0) {
                if (super.enableLoop) {
                    if (super.sampleRate < 0) {
                        currentOffset = mixSamplesBackward(mixBuffer, offset, startPos, endOffset, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                        if (super.playbackPosition >= startPos) {
                            return
                        }

                        super.playbackPosition = startPos + startPos - 1 - super.playbackPosition
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                    }

                    while (true) {
                        currentOffset = mixSamplesForward(mixBuffer, currentOffset, endPos, endOffset, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                        if (super.playbackPosition < endPos) {
                            break
                        }

                        super.playbackPosition = endPos + endPos - 1 - super.playbackPosition
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                        currentOffset = mixSamplesBackward(mixBuffer, currentOffset, startPos, endOffset, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                        if (super.playbackPosition >= startPos) {
                            break
                        }

                        super.playbackPosition = startPos + startPos - 1 - super.playbackPosition
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                    }
                } else if (super.sampleRate < 0) {
                    while (true) {
                        currentOffset = mixSamplesBackward(mixBuffer, currentOffset, startPos, endOffset, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                        if (super.playbackPosition >= startPos) {
                            break
                        }

                        super.playbackPosition = endPos - 1 - (endPos - 1 - super.playbackPosition) % loopLength
                    }
                } else {
                    while (true) {
                        currentOffset = mixSamplesForward(mixBuffer, currentOffset, endPos, endOffset, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                        if (super.playbackPosition < endPos) {
                            break
                        }

                        super.playbackPosition = startPos + (super.playbackPosition - startPos) % loopLength
                    }
                }
            } else {
                if (super.loopsRemaining > 0) {
                    if (super.enableLoop) {
                        if (super.sampleRate < 0) {
                            currentOffset = mixSamplesBackward(mixBuffer, offset, startPos, endOffset, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                            if (super.playbackPosition >= startPos) {
                                return
                            }

                            super.playbackPosition = startPos + startPos - 1 - super.playbackPosition
                            super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                            if (--super.loopsRemaining == 0) {
                                // continue to next section
                            } else {
                                do {
                                    currentOffset = mixSamplesForward(mixBuffer, currentOffset, endPos, endOffset, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                                    if (super.playbackPosition < endPos) {
                                        return
                                    }

                                    super.playbackPosition = endPos + endPos - 1 - super.playbackPosition
                                    super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                    if (--super.loopsRemaining == 0) {
                                        break
                                    }

                                    currentOffset = mixSamplesBackward(mixBuffer, currentOffset, startPos, endOffset, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                                    if (super.playbackPosition >= startPos) {
                                        return
                                    }

                                    super.playbackPosition = startPos + startPos - 1 - super.playbackPosition
                                    super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                } while (--super.loopsRemaining != 0)
                            }
                        } else {
                            do {
                                currentOffset = mixSamplesForward(mixBuffer, currentOffset, endPos, endOffset, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                                if (super.playbackPosition < endPos) {
                                    return
                                }

                                super.playbackPosition = endPos + endPos - 1 - super.playbackPosition
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                if (--super.loopsRemaining == 0) {
                                    break
                                }

                                currentOffset = mixSamplesBackward(mixBuffer, currentOffset, startPos, endOffset, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                                if (super.playbackPosition >= startPos) {
                                    return
                                }

                                super.playbackPosition = startPos + startPos - 1 - super.playbackPosition
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                            } while (--super.loopsRemaining != 0)
                        }
                    } else {
                        var loopSkips: Int
                        if (super.sampleRate < 0) {
                            while (true) {
                                currentOffset = mixSamplesBackward(mixBuffer, currentOffset, startPos, endOffset, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                                if (super.playbackPosition >= startPos) {
                                    return
                                }

                                loopSkips = (endPos - 1 - super.playbackPosition) / loopLength
                                if (loopSkips >= super.loopsRemaining) {
                                    super.playbackPosition += loopLength * super.loopsRemaining
                                    super.loopsRemaining = 0
                                    break
                                }

                                super.playbackPosition += loopLength * loopSkips
                                super.loopsRemaining -= loopSkips
                            }
                        } else {
                            while (true) {
                                currentOffset = mixSamplesForward(mixBuffer, currentOffset, endPos, endOffset, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                                if (super.playbackPosition < endPos) {
                                    return
                                }

                                loopSkips = (super.playbackPosition - startPos) / loopLength
                                if (loopSkips >= super.loopsRemaining) {
                                    super.playbackPosition -= loopLength * super.loopsRemaining
                                    super.loopsRemaining = 0
                                    break
                                }

                                super.playbackPosition -= loopLength * loopSkips
                                super.loopsRemaining -= loopSkips
                            }
                        }
                    }
                }

                if (super.sampleRate < 0) {
                    mixSamplesBackward(mixBuffer, currentOffset, 0, endOffset, 0.0f)
                    if (super.playbackPosition < 0) {
                        super.playbackPosition = -1
                        cancelFade()
                        unlink()
                    }
                } else {
                    mixSamplesForward(mixBuffer, currentOffset, totalSamples, endOffset, 0.0f)
                    if (super.playbackPosition >= totalSamples) {
                        super.playbackPosition = totalSamples
                        cancelFade()
                        unlink()
                    }
                }
            }
        }
    }

    private fun mixSamplesBackward(mixBuffer: IntArray, outputOffset: Int, endPosition: Int, maxOutputOffset: Int, wrapSample: Float): Int {
        var currentOffset = outputOffset
        do {
            if (super.fadeSamplesRemaining <= 0) {
                if (super.sampleRate == -256 && (super.playbackPosition and 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return mixBackwardStereoConstant((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, maxOutputOffset, endPosition, this)
                    }

                    return mixBackwardMonoConstant((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, maxOutputOffset, endPosition, this)
                }

                if (PcmPlayerManager.stereo) {
                    return mixBackwardStereoInterpolated((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, maxOutputOffset, endPosition, this, super.sampleRate, wrapSample)
                }

                return mixBackwardMonoInterpolated((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, maxOutputOffset, endPosition, this, super.sampleRate, wrapSample)
            }

            var fadeEnd = currentOffset + super.fadeSamplesRemaining
            if (fadeEnd > maxOutputOffset) {
                fadeEnd = maxOutputOffset
            }

            super.fadeSamplesRemaining += currentOffset
            if (super.sampleRate == -256 && (super.playbackPosition and 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    currentOffset = mixBackwardStereoConstantFading((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, fadeEnd, endPosition, this)
                } else {
                    currentOffset = mixBackwardMonoConstantFading((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, super.volumeFadeDelta, fadeEnd, endPosition, this)
                }
            } else if (PcmPlayerManager.stereo) {
                currentOffset = mixBackwardStereoInterpolatedFading((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, fadeEnd, endPosition, this, super.sampleRate, wrapSample)
            } else {
                currentOffset = mixBackwardMonoInterpolatedFading((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, super.volumeFadeDelta, fadeEnd, endPosition, this, super.sampleRate, wrapSample)
            }

            super.fadeSamplesRemaining -= currentOffset
            if (super.fadeSamplesRemaining != 0) {
                return currentOffset
            }
        } while (!updateFadeState())

        return maxOutputOffset
    }

    private fun mixSamplesForward(mixBuffer: IntArray, outputOffset: Int, endPosition: Int, maxOutputOffset: Int, wrapSample: Float): Int {
        var currentOffset = outputOffset
        do {
            if (super.fadeSamplesRemaining <= 0) {
                if (super.sampleRate == 256 && (super.playbackPosition and 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return mixForwardStereoConstant((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, maxOutputOffset, endPosition, this)
                    }

                    return mixForwardMonoConstant((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, maxOutputOffset, endPosition, this)
                }

                if (PcmPlayerManager.stereo) {
                    return mixForwardStereoInterpolated((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, maxOutputOffset, endPosition, this, super.sampleRate, wrapSample)
                }

                return mixForwardMonoInterpolated((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, maxOutputOffset, endPosition, this, super.sampleRate, wrapSample)
            }

            var fadeEnd = currentOffset + super.fadeSamplesRemaining
            if (fadeEnd > maxOutputOffset) {
                fadeEnd = maxOutputOffset
            }

            super.fadeSamplesRemaining += currentOffset
            if (super.sampleRate == 256 && (super.playbackPosition and 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    currentOffset = mixForwardStereoConstantFading((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, fadeEnd, endPosition, this)
                } else {
                    currentOffset = mixForwardMonoConstantFading((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, super.volumeFadeDelta, fadeEnd, endPosition, this)
                }
            } else if (PcmPlayerManager.stereo) {
                currentOffset = mixForwardStereoInterpolatedFading((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, fadeEnd, endPosition, this, super.sampleRate, wrapSample)
            } else {
                currentOffset = mixForwardMonoInterpolatedFading((buffer as AudioBuffer).getSamples(), mixBuffer, super.playbackPosition, currentOffset, super.currentVolume, super.volumeFadeDelta, fadeEnd, endPosition, this, super.sampleRate, wrapSample)
            }

            super.fadeSamplesRemaining -= currentOffset
            if (super.fadeSamplesRemaining != 0) {
                return currentOffset
            }
        } while (!updateFadeState())

        return maxOutputOffset
    }
}
