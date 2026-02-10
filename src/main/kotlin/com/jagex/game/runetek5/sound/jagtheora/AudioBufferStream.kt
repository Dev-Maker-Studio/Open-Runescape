package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.sound.player.PcmPlayerManager

open class AudioBufferStream(
    buffer: AudioBuffer,
    i_2: Int,
    volume: Int,
    i_4: Int
) : AreaSoundStream() {

    init {
        this.buffer = buffer
        this.start = buffer.start
        this.end = buffer.end
        this.enableLoop = buffer.enableLoop
        this.sampleRate = i_2
        this.targetVolume = volume
        this.panPosition = i_4
        this.playbackPosition = 0
        recalculateChannelVolumes()
    }

    companion object {
        @JvmStatic
        fun mixBackwardStereoInterpolated(
            floats_2: FloatArray,
            ints_3: IntArray,
            i_4: Int,
            i_5: Int,
            i_6: Int,
            i_7: Int,
            i_9: Int,
            i_10: Int,
            class282Sub15Sub5Sub111: AudioBufferStream,
            i_12: Int,
            f_13: Float
        ): Int {
            var i_11 = 0
            var i_81: Int
            var f_01 = 0.0f
            var i_4var = i_4
            var i_5var = i_5
            var i_6var = i_6 * 128
            var i_7var = i_7 * 128
            i_81 = i_5var + (i_10 + 256 - i_4var + i_12) / i_12
            if (i_12 == 0 || i_81 > i_9) {
                i_81 = i_9
            }

            i_5var = i_5var shl 1
            var i_10001: Int
            i_81 = i_81 shl 1
            while (i_5var < i_81) {
                i_11 = i_4var shr 8
                f_01 = floats_2[i_11 - 1]
                f_01 = f_01 * 256.0f + (floats_2[i_11] - f_01) * (i_4var and 0xff)
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_6var).toInt() shr 6
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_7var).toInt() shr 6
                i_4var += i_12
            }

            i_81 = (i_5var shr 1) + (i_10 - i_4var + i_12) / i_12
            if (i_12 == 0 || i_81 > i_9) {
                i_81 = i_9
            }

            i_81 = i_81 shl 1
            while (i_5var < i_81) {
                f_01 = f_13 * 256.0f + (floats_2[i_4var shr 8] - f_13) * (i_4var and 0xff)
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_6var).toInt() shr 6
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_7var).toInt() shr 6
                i_4var += i_12
            }

            class282Sub15Sub5Sub111.playbackPosition = i_4var
            return i_5var shr 1
        }

        @JvmStatic
        fun mixBackwardStereoConstant(
            floats_1: FloatArray,
            ints_2: IntArray,
            i_3: Int,
            i_4: Int,
            i_5: Int,
            i_6: Int,
            i_8: Int,
            i_9: Int,
            class282Sub15Sub5Sub110: AudioBufferStream
        ): Int {
            var i_71: Int
            var f_01 = 0.0f
            var i_3var = i_3 shr 8
            var i_9var = i_9 shr 8
            var i_5var = i_5 shl 2
            var i_6var = i_6 shl 2
            i_5var *= 128
            i_6var *= 128
            i_71 = i_3var + i_4 - (i_9var - 1)
            if (i_71 > i_8) {
                i_71 = i_8
            }

            var i_4var = i_4 shl 1
            i_71 = i_71 shl 1

            var i_10001: Int
            i_71 -= 6
            while (i_4var < i_71) {
                f_01 = floats_1[i_3var--]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                f_01 = floats_1[i_3var--]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                f_01 = floats_1[i_3var--]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                f_01 = floats_1[i_3var--]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
            }

            i_71 += 6
            while (i_4var < i_71) {
                f_01 = floats_1[i_3var--]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
            }

            class282Sub15Sub5Sub110.playbackPosition = i_3var shl 8
            return i_4var shr 1
        }

        @JvmStatic
        fun mixBackwardMonoInterpolated(
            floats_2: FloatArray,
            ints_3: IntArray,
            i_4: Int,
            i_5: Int,
            i_6: Int,
            i_8: Int,
            i_9: Int,
            class282Sub15Sub5Sub110: AudioBufferStream,
            i_11: Int,
            f_12: Float
        ): Int {
            var i_12 = 0
            var i_71: Int
            var f_01 = 0.0f
            var i_6var = i_6 * 128
            var i_4var = i_4
            var i_5var = i_5
            i_71 = i_5var + (i_11 + (i_9 + 256 - i_4var)) / i_11
            if (i_11 == 0 || i_71 > i_8) {
                i_71 = i_8
            }

            var i_10001: Int
            while (i_5var < i_71) {
                i_12 = i_4var shr 8
                f_01 = floats_2[i_12 - 1]
                i_10001 = i_5var++
                ints_3[i_10001] += ((f_01 * 256.0f + (floats_2[i_12] - f_01) * (i_4var and 0xff)) * i_6var).toInt() shr 6
                i_4var += i_11
            }

            i_71 = i_5var + (i_11 + (i_9 - i_4var)) / i_11
            if (i_11 == 0 || i_71 > i_8) {
                i_71 = i_8
            }

            f_01 = f_12
            while (i_5var < i_71) {
                i_10001 = i_5var++
                ints_3[i_10001] += ((f_01 * 256.0f + (floats_2[i_4var shr 8] - f_01) * (i_4var and 0xff)) * i_6var).toInt() shr 6
                i_4var += i_11
            }

            class282Sub15Sub5Sub110.playbackPosition = i_4var
            return i_5var
        }

        @JvmStatic
        fun mixForwardMonoInterpolatedFading(
            floats_2: FloatArray,
            ints_3: IntArray,
            i_4: Int,
            i_5: Int,
            i_6: Int,
            i_7: Int,
            i_9: Int,
            i_10: Int,
            class282Sub15Sub5Sub111: AudioBufferStream,
            i_12: Int,
            f_13: Float
        ): Int {
            var i_11 = 0
            var i_81: Int
            var f_01 = 0.0f
            var i_6var = i_6 * 128
            var i_7var = i_7 * 128
            var i_5var = i_5
            var i_4var = i_4
            class282Sub15Sub5Sub111.leftChannelVolume -= class282Sub15Sub5Sub111.leftVolumeFadeDelta * i_5var
            class282Sub15Sub5Sub111.rightChannelVolume -= class282Sub15Sub5Sub111.rightVolumeFadeDelta * i_5var
            i_81 = i_5var + (i_10 - i_4var + i_12 - 257) / i_12
            if (i_12 == 0 || i_81 > i_9) {
                i_81 = i_9
            }

            var i_10001: Int
            while (i_5var < i_81) {
                i_11 = i_4var shr 8
                f_01 = floats_2[i_11]
                i_10001 = i_5var++
                ints_3[i_10001] += ((f_01 * 256.0f + (floats_2[i_11 + 1] - f_01) * (i_4var and 0xff)) * i_6var).toInt() shr 6
                i_6var += i_7var
                i_4var += i_12
            }

            i_81 = i_5var + (i_10 - i_4var + i_12 - 1) / i_12
            if (i_12 == 0 || i_81 > i_9) {
                i_81 = i_9
            }

            while (i_5var < i_81) {
                f_01 = floats_2[i_4var shr 8]
                i_10001 = i_5var++
                ints_3[i_10001] += ((f_01 * 256.0f + (f_13 - f_01) * (i_4var and 0xff)) * i_6var).toInt() shr 6
                i_6var += i_7var
                i_4var += i_12
            }

            class282Sub15Sub5Sub111.leftChannelVolume += class282Sub15Sub5Sub111.leftVolumeFadeDelta * i_5var
            class282Sub15Sub5Sub111.rightChannelVolume += class282Sub15Sub5Sub111.rightVolumeFadeDelta * i_5var
            class282Sub15Sub5Sub111.currentVolume = i_6var / 128
            class282Sub15Sub5Sub111.playbackPosition = i_4var
            return i_5var
        }

        @JvmStatic
        fun mixForwardStereoConstantFading(
            floats_1: FloatArray,
            ints_2: IntArray,
            i_3: Int,
            i_4: Int,
            i_5: Int,
            i_6: Int,
            i_7: Int,
            i_8: Int,
            i_10: Int,
            i_11: Int,
            class282Sub15Sub5Sub112: AudioBufferStream
        ): Int {
            var i_91: Int
            var f_01 = 0.0f
            var i_3var = i_3 shr 8
            var i_11var = i_11 shr 8
            var i_5var = i_5 shl 2
            var i_6var = i_6 shl 2
            var i_7var = i_7 shl 2
            var i_8var = i_8 shl 2
            i_5var *= 128
            i_7var *= 128
            i_6var *= 128
            i_8var *= 128
            i_91 = i_11var + i_4 - i_3var
            if (i_91 > i_10) {
                i_91 = i_10
            }

            class282Sub15Sub5Sub112.currentVolume += class282Sub15Sub5Sub112.volumeFadeDelta * (i_91 - i_4)
            var i_4var = i_4 shl 1
            i_91 = i_91 shl 1

            var i_10001: Int
            i_91 -= 6
            while (i_4var < i_91) {
                f_01 = floats_1[i_3var++]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_5var += i_7var
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                i_6var += i_8var
                f_01 = floats_1[i_3var++]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_5var += i_7var
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                i_6var += i_8var
                f_01 = floats_1[i_3var++]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_5var += i_7var
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                i_6var += i_8var
                f_01 = floats_1[i_3var++]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_5var += i_7var
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                i_6var += i_8var
            }

            i_91 += 6
            while (i_4var < i_91) {
                f_01 = floats_1[i_3var++]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_5var += i_7var
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                i_6var += i_8var
            }

            class282Sub15Sub5Sub112.leftChannelVolume = i_5var / 128 shr 2
            class282Sub15Sub5Sub112.rightChannelVolume = i_6var / 128 shr 2
            class282Sub15Sub5Sub112.playbackPosition = i_3var shl 8
            return i_4var shr 1
        }

        @JvmStatic
        fun mixBackwardMonoConstantFading(
            floats_0: FloatArray,
            ints_1: IntArray,
            i_2: Int,
            i_3: Int,
            i_4: Int,
            i_5: Int,
            i_7: Int,
            i_8: Int,
            class282Sub15Sub5Sub19: AudioBufferStream
        ): Int {
            var i_61: Int
            var i_2var = i_2 shr 8
            var i_8var = i_8 shr 8
            var i_4var = i_4 shl 2
            var i_5var = i_5 shl 2
            i_4var *= 128
            i_5var *= 128
            i_61 = i_3 + i_2var - (i_8var - 1)
            if (i_61 > i_7) {
                i_61 = i_7
            }

            class282Sub15Sub5Sub19.leftChannelVolume += class282Sub15Sub5Sub19.leftVolumeFadeDelta * (i_61 - i_3)
            class282Sub15Sub5Sub19.rightChannelVolume += class282Sub15Sub5Sub19.rightVolumeFadeDelta * (i_61 - i_3)

            var i_10001: Int
            var i_3var = i_3
            i_61 -= 3
            while (i_3var < i_61) {
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var--] * i_4var).toInt()
                i_4var += i_5var
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var--] * i_4var).toInt()
                i_4var += i_5var
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var--] * i_4var).toInt()
                i_4var += i_5var
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var--] * i_4var).toInt()
                i_4var += i_5var
            }

            i_61 += 3
            while (i_3var < i_61) {
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var--] * i_4var).toInt()
                i_4var += i_5var
            }

            class282Sub15Sub5Sub19.currentVolume = i_4var / 128 shr 2
            class282Sub15Sub5Sub19.playbackPosition = i_2var shl 8
            return i_3var
        }

        @JvmStatic
        fun mixBackwardStereoConstantFading(
            floats_1: FloatArray,
            ints_2: IntArray,
            i_3: Int,
            i_4: Int,
            i_5: Int,
            i_6: Int,
            i_7: Int,
            i_8: Int,
            i_10: Int,
            i_11: Int,
            class282Sub15Sub5Sub112: AudioBufferStream
        ): Int {
            var i_91: Int
            var f_01 = 0.0f
            var i_3var = i_3 shr 8
            var i_11var = i_11 shr 8
            var i_5var = i_5 shl 2
            var i_6var = i_6 shl 2
            var i_7var = i_7 shl 2
            var i_8var = i_8 shl 2
            i_5var *= 128
            i_7var *= 128
            i_6var *= 128
            i_8var *= 128
            i_91 = i_3var + i_4 - (i_11var - 1)
            if (i_91 > i_10) {
                i_91 = i_10
            }

            class282Sub15Sub5Sub112.currentVolume += class282Sub15Sub5Sub112.volumeFadeDelta * (i_91 - i_4)
            var i_4var = i_4 shl 1
            i_91 = i_91 shl 1

            var i_10001: Int
            i_91 -= 6
            while (i_4var < i_91) {
                f_01 = floats_1[i_3var--]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_5var += i_7var
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                i_6var += i_8var
                f_01 = floats_1[i_3var--]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_5var += i_7var
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                i_6var += i_8var
                f_01 = floats_1[i_3var--]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_5var += i_7var
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                i_6var += i_8var
                f_01 = floats_1[i_3var--]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_5var += i_7var
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                i_6var += i_8var
            }

            i_91 += 6
            while (i_4var < i_91) {
                f_01 = floats_1[i_3var--]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_5var += i_7var
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                i_6var += i_8var
            }

            class282Sub15Sub5Sub112.leftChannelVolume = i_5var / 128 shr 2
            class282Sub15Sub5Sub112.rightChannelVolume = i_6var / 128 shr 2
            class282Sub15Sub5Sub112.playbackPosition = i_3var shl 8
            return i_4var shr 1
        }

        @JvmStatic
        fun mixForwardMonoConstant(
            floats_0: FloatArray,
            ints_1: IntArray,
            i_2: Int,
            i_3: Int,
            i_4: Int,
            i_6: Int,
            i_7: Int,
            class282Sub15Sub5Sub18: AudioBufferStream
        ): Int {
            var i_51: Int
            var i_2var = i_2 shr 8
            var i_7var = i_7 shr 8
            var i_4var = i_4 shl 2
            i_4var *= 128
            i_51 = i_3 + i_7var - i_2var
            if (i_51 > i_6) {
                i_51 = i_6
            }

            var i_10001: Int
            var i_3var = i_3
            i_51 -= 3
            while (i_3var < i_51) {
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var++] * i_4var).toInt()
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var++] * i_4var).toInt()
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var++] * i_4var).toInt()
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var++] * i_4var).toInt()
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var++] * i_4var).toInt()
            }

            i_51 += 3
            while (i_3var < i_51) {
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var++] * i_4var).toInt()
            }

            class282Sub15Sub5Sub18.playbackPosition = i_2var shl 8
            return i_3var
        }

        @JvmStatic
        fun mixForwardStereoInterpolatedFading(
            floats_2: FloatArray,
            ints_3: IntArray,
            i_4: Int,
            i_5: Int,
            i_6: Int,
            i_7: Int,
            i_8: Int,
            i_9: Int,
            i_11: Int,
            i_12: Int,
            class282Sub15Sub5Sub113: AudioBufferStream,
            i_14: Int,
            f_15: Float
        ): Int {
            var i_13 = 0
            var i_101: Int
            var f_01 = 0.0f
            var i_6var = i_6 * 128
            var i_8var = i_8 * 128
            var i_7var = i_7 * 128
            var i_9var = i_9 * 128
            var i_5var = i_5
            var i_4var = i_4
            class282Sub15Sub5Sub113.currentVolume -= i_5var * class282Sub15Sub5Sub113.volumeFadeDelta
            i_101 = i_5var + (i_12 - i_4var + i_14 - 257) / i_14
            if (i_14 == 0 || i_101 > i_11) {
                i_101 = i_11
            }

            i_5var = i_5var shl 1
            var i_10001: Int
            i_101 = i_101 shl 1
            while (i_5var < i_101) {
                i_13 = i_4var shr 8
                f_01 = floats_2[i_13]
                f_01 = f_01 * 256.0f + (floats_2[i_13 + 1] - f_01) * (i_4var and 0xff)
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_6var).toInt() shr 6
                i_6var += i_8var
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_7var).toInt() shr 6
                i_7var += i_9var
                i_4var += i_14
            }

            i_101 = (i_5var shr 1) + (i_12 - i_4var + i_14 - 1) / i_14
            if (i_14 == 0 || i_101 > i_11) {
                i_101 = i_11
            }

            i_101 = i_101 shl 1
            while (i_5var < i_101) {
                f_01 = floats_2[i_4var shr 8]
                f_01 = f_01 * 256.0f + (f_15 - f_01) * (i_4var and 0xff)
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_6var).toInt() shr 6
                i_6var += i_8var
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_7var).toInt() shr 6
                i_7var += i_9var
                i_4var += i_14
            }

            i_5var = i_5var shr 1
            class282Sub15Sub5Sub113.currentVolume += class282Sub15Sub5Sub113.volumeFadeDelta * i_5var
            class282Sub15Sub5Sub113.leftChannelVolume = i_6var / 128
            class282Sub15Sub5Sub113.rightChannelVolume = i_7var / 128
            class282Sub15Sub5Sub113.playbackPosition = i_4var
            return i_5var
        }

        @JvmStatic
        fun mixBackwardMonoInterpolatedFading(
            floats_2: FloatArray,
            ints_3: IntArray,
            i_4: Int,
            i_5: Int,
            i_6: Int,
            i_7: Int,
            i_9: Int,
            i_10: Int,
            class282Sub15Sub5Sub111: AudioBufferStream,
            i_12: Int,
            f_13: Float
        ): Int {
            var i_11 = 0
            var i_81: Int
            var f_01 = 0.0f
            var i_6var = i_6 * 128
            var i_7var = i_7 * 128
            var i_5var = i_5
            var i_4var = i_4
            class282Sub15Sub5Sub111.leftChannelVolume -= class282Sub15Sub5Sub111.leftVolumeFadeDelta * i_5var
            class282Sub15Sub5Sub111.rightChannelVolume -= class282Sub15Sub5Sub111.rightVolumeFadeDelta * i_5var
            i_81 = i_5var + (i_10 + 256 - i_4var + i_12) / i_12
            if (i_12 == 0 || i_81 > i_9) {
                i_81 = i_9
            }

            var i_10001: Int
            while (i_5var < i_81) {
                i_11 = i_4var shr 8
                f_01 = floats_2[i_11 - 1]
                i_10001 = i_5var++
                ints_3[i_10001] += ((f_01 * 256.0f + (floats_2[i_11] - f_01) * (i_4var and 0xff)) * i_6var).toInt() shr 6
                i_6var += i_7var
                i_4var += i_12
            }

            i_81 = i_5var + (i_10 - i_4var + i_12) / i_12
            if (i_12 == 0 || i_81 > i_9) {
                i_81 = i_9
            }

            f_01 = f_13
            while (i_5var < i_81) {
                i_10001 = i_5var++
                ints_3[i_10001] += ((f_01 * 256.0f + (floats_2[i_4var shr 8] - f_01) * (i_4var and 0xff)) * i_6var).toInt() shr 6
                i_6var += i_7var
                i_4var += i_12
            }

            class282Sub15Sub5Sub111.leftChannelVolume += class282Sub15Sub5Sub111.leftVolumeFadeDelta * i_5var
            class282Sub15Sub5Sub111.rightChannelVolume += class282Sub15Sub5Sub111.rightVolumeFadeDelta * i_5var
            class282Sub15Sub5Sub111.currentVolume = i_6var / 128
            class282Sub15Sub5Sub111.playbackPosition = i_4var
            return i_5var
        }

        @JvmStatic
        fun mixBackwardStereoInterpolatedFading(
            floats_2: FloatArray,
            ints_3: IntArray,
            i_4: Int,
            i_5: Int,
            i_6: Int,
            i_7: Int,
            i_8: Int,
            i_9: Int,
            i_11: Int,
            i_12: Int,
            class282Sub15Sub5Sub113: AudioBufferStream,
            i_14: Int,
            f_15: Float
        ): Int {
            var i_13 = 0
            var i_101: Int
            var f_01 = 0.0f
            var i_6var = i_6 * 128
            var i_8var = i_8 * 128
            var i_7var = i_7 * 128
            var i_9var = i_9 * 128
            var i_5var = i_5
            var i_4var = i_4
            class282Sub15Sub5Sub113.currentVolume -= i_5var * class282Sub15Sub5Sub113.volumeFadeDelta
            i_101 = i_5var + (i_12 + 256 - i_4var + i_14) / i_14
            if (i_14 == 0 || i_101 > i_11) {
                i_101 = i_11
            }

            i_5var = i_5var shl 1
            var i_10001: Int
            i_101 = i_101 shl 1
            while (i_5var < i_101) {
                i_13 = i_4var shr 8
                f_01 = floats_2[i_13 - 1]
                f_01 = f_01 * 256.0f + (floats_2[i_13] - f_01) * (i_4var and 0xff)
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_6var).toInt() shr 6
                i_6var += i_8var
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_7var).toInt() shr 6
                i_7var += i_9var
                i_4var += i_14
            }

            i_101 = (i_5var shr 1) + (i_12 - i_4var + i_14) / i_14
            if (i_14 == 0 || i_101 > i_11) {
                i_101 = i_11
            }

            i_101 = i_101 shl 1
            while (i_5var < i_101) {
                f_01 = f_15 * 256.0f + (floats_2[i_4var shr 8] - f_15) * (i_4var and 0xff)
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_6var).toInt() shr 6
                i_6var += i_8var
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_7var).toInt() shr 6
                i_7var += i_9var
                i_4var += i_14
            }

            i_5var = i_5var shr 1
            class282Sub15Sub5Sub113.currentVolume += class282Sub15Sub5Sub113.volumeFadeDelta * i_5var
            class282Sub15Sub5Sub113.leftChannelVolume = i_6var / 128
            class282Sub15Sub5Sub113.rightChannelVolume = i_7var / 128
            class282Sub15Sub5Sub113.playbackPosition = i_4var
            return i_5var
        }

        @JvmStatic
        fun mixForwardStereoInterpolated(
            floats_2: FloatArray,
            ints_3: IntArray,
            i_4: Int,
            i_5: Int,
            i_6: Int,
            i_7: Int,
            i_9: Int,
            i_10: Int,
            class282Sub15Sub5Sub111: AudioBufferStream,
            i_12: Int,
            f_13: Float
        ): Int {
            var i_11 = 0
            var i_81: Int
            var f_01 = 0.0f
            var i_6var = i_6 * 128
            var i_7var = i_7 * 128
            var i_5var = i_5
            var i_4var = i_4
            i_81 = i_5var + (i_10 - i_4var + i_12 - 257) / i_12
            if (i_12 == 0 || i_81 > i_9) {
                i_81 = i_9
            }

            i_5var = i_5var shl 1
            var i_10001: Int
            i_81 = i_81 shl 1
            while (i_5var < i_81) {
                i_11 = i_4var shr 8
                f_01 = floats_2[i_11]
                f_01 = f_01 * 256.0f + (floats_2[i_11 + 1] - f_01) * (i_4var and 0xff)
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_6var).toInt() shr 6
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_7var).toInt() shr 6
                i_4var += i_12
            }

            i_81 = (i_5var shr 1) + (i_10 - i_4var + i_12 - 1) / i_12
            if (i_12 == 0 || i_81 > i_9) {
                i_81 = i_9
            }

            i_81 = i_81 shl 1
            while (i_5var < i_81) {
                f_01 = floats_2[i_4var shr 8]
                f_01 = f_01 * 256.0f + (f_13 - f_01) * (i_4var and 0xff)
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_6var).toInt() shr 6
                i_10001 = i_5var++
                ints_3[i_10001] += (f_01 * i_7var).toInt() shr 6
                i_4var += i_12
            }

            class282Sub15Sub5Sub111.playbackPosition = i_4var
            return i_5var shr 1
        }

        @JvmStatic
        fun mixForwardMonoInterpolated(
            floats_2: FloatArray,
            ints_3: IntArray,
            i_4: Int,
            i_5: Int,
            i_6: Int,
            i_8: Int,
            i_9: Int,
            class282Sub15Sub5Sub110: AudioBufferStream,
            i_11: Int,
            f_12: Float
        ): Int {
            var i_12 = 0
            var i_71: Int
            var f_01 = 0.0f
            var i_6var = i_6 * 128
            var i_5var = i_5
            var i_4var = i_4
            i_71 = i_5var + (i_11 + (i_9 - i_4var) - 257) / i_11
            if (i_11 == 0 || i_71 > i_8) {
                i_71 = i_8
            }

            var i_10001: Int
            while (i_5var < i_71) {
                i_12 = i_4var shr 8
                f_01 = floats_2[i_12]
                i_10001 = i_5var++
                ints_3[i_10001] += ((f_01 * 256.0f + (floats_2[i_12 + 1] - f_01) * (i_4var and 0xff)) * i_6var).toInt() shr 6
                i_4var += i_11
            }

            i_71 = i_5var + (i_11 + (i_9 - i_4var) - 1) / i_11
            if (i_11 == 0 || i_71 > i_8) {
                i_71 = i_8
            }

            while (i_5var < i_71) {
                f_01 = floats_2[i_4var shr 8]
                i_10001 = i_5var++
                ints_3[i_10001] += ((f_01 * 256.0f + (f_12 - f_01) * (i_4var and 0xff)) * i_6var).toInt() shr 6
                i_4var += i_11
            }

            class282Sub15Sub5Sub110.playbackPosition = i_4var
            return i_5var
        }

        @JvmStatic
        fun mixForwardStereoConstant(
            floats_1: FloatArray,
            ints_2: IntArray,
            i_3: Int,
            i_4: Int,
            i_5: Int,
            i_6: Int,
            i_8: Int,
            i_9: Int,
            class282Sub15Sub5Sub110: AudioBufferStream
        ): Int {
            var i_71: Int
            var f_01 = 0.0f
            var i_3var = i_3 shr 8
            var i_9var = i_9 shr 8
            var i_5var = i_5 shl 2
            var i_6var = i_6 shl 2
            i_5var *= 128
            i_6var *= 128
            i_71 = i_4 + i_9var - i_3var
            if (i_71 > i_8) {
                i_71 = i_8
            }

            var i_4var = i_4 shl 1
            i_71 = i_71 shl 1

            var i_10001: Int
            i_71 -= 6
            while (i_4var < i_71) {
                f_01 = floats_1[i_3var++]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                f_01 = floats_1[i_3var++]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                f_01 = floats_1[i_3var++]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
                f_01 = floats_1[i_3var++]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
            }

            i_71 += 6
            while (i_4var < i_71) {
                f_01 = floats_1[i_3var++]
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_5var).toInt()
                i_10001 = i_4var++
                ints_2[i_10001] = (ints_2[i_10001] + f_01 * i_6var).toInt()
            }

            class282Sub15Sub5Sub110.playbackPosition = i_3var shl 8
            return i_4var shr 1
        }

        @JvmStatic
        fun mixBackwardMonoConstant(
            floats_0: FloatArray,
            ints_1: IntArray,
            i_2: Int,
            i_3: Int,
            i_4: Int,
            i_6: Int,
            i_7: Int,
            class282Sub15Sub5Sub18: AudioBufferStream
        ): Int {
            var i_51: Int
            var i_2var = i_2 shr 8
            var i_7var = i_7 shr 8
            var i_4var = i_4 shl 2
            i_4var *= 128
            i_51 = i_3 + i_2var - (i_7var - 1)
            if (i_51 > i_6) {
                i_51 = i_6
            }

            var i_10001: Int
            var i_3var = i_3
            i_51 -= 3
            while (i_3var < i_51) {
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var--] * i_4var).toInt()
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var--] * i_4var).toInt()
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var--] * i_4var).toInt()
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var--] * i_4var).toInt()
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var--] * i_4var).toInt()
            }

            i_51 += 3
            while (i_3var < i_51) {
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var--] * i_4var).toInt()
            }

            class282Sub15Sub5Sub18.playbackPosition = i_2var shl 8
            return i_3var
        }

        @JvmStatic
        fun mixForwardMonoConstantFading(
            floats_0: FloatArray,
            ints_1: IntArray,
            i_2: Int,
            i_3: Int,
            i_4: Int,
            i_5: Int,
            i_7: Int,
            i_8: Int,
            class282Sub15Sub5Sub19: AudioBufferStream
        ): Int {
            var i_61: Int
            var i_2var = i_2 shr 8
            var i_8var = i_8 shr 8
            var i_4var = i_4 shl 2
            var i_5var = i_5 shl 2
            i_4var *= 128
            i_5var *= 128
            i_61 = i_3 + i_8var - i_2var
            if (i_61 > i_7) {
                i_61 = i_7
            }

            class282Sub15Sub5Sub19.leftChannelVolume += class282Sub15Sub5Sub19.leftVolumeFadeDelta * (i_61 - i_3)
            class282Sub15Sub5Sub19.rightChannelVolume += class282Sub15Sub5Sub19.rightVolumeFadeDelta * (i_61 - i_3)

            var i_10001: Int
            var i_3var = i_3
            i_61 -= 3
            while (i_3var < i_61) {
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var++] * i_4var).toInt()
                i_4var += i_5var
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var++] * i_4var).toInt()
                i_4var += i_5var
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var++] * i_4var).toInt()
                i_4var += i_5var
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var++] * i_4var).toInt()
                i_4var += i_5var
            }

            i_61 += 3
            while (i_3var < i_61) {
                i_10001 = i_3var++
                ints_1[i_10001] = (ints_1[i_10001] + floats_0[i_2var++] * i_4var).toInt()
                i_4var += i_5var
            }

            class282Sub15Sub5Sub19.currentVolume = i_4var / 128 shr 2
            class282Sub15Sub5Sub19.playbackPosition = i_2var shl 8
            return i_3var
        }
    }

    @Synchronized
    override fun fill(ints_1: IntArray, i_2: Int, i_3: Int) {
        if (super.targetVolume == 0 && super.fadeSamplesRemaining == 0) {
            skip(i_3)
        } else {
            var i_4 = super.start shl 8
            var i_5 = super.end shl 8
            val i_6 = ((buffer as AudioBuffer).getTotalSamples()) shl 8
            val i_7 = i_5 - i_4
            if (i_7 <= 0) {
                super.loopsRemaining = 0
            }

            var i_8 = i_2
            var i_3var = i_3 + i_2
            if (super.playbackPosition < 0) {
                if (super.sampleRate <= 0) {
                    cancelFade()
                    unlink()
                    return
                }

                super.playbackPosition = 0
            }

            if (super.playbackPosition >= i_6) {
                if (super.sampleRate >= 0) {
                    cancelFade()
                    unlink()
                    return
                }

                super.playbackPosition = i_6 - 1
            }

            if (super.loopsRemaining < 0) {
                if (super.enableLoop) {
                    if (super.sampleRate < 0) {
                        i_8 = mixSamplesBackward(ints_1, i_2, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                        if (super.playbackPosition >= i_4) {
                            return
                        }

                        super.playbackPosition = i_4 + i_4 - 1 - super.playbackPosition
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                    }

                    while (true) {
                        i_8 = mixSamplesForward(ints_1, i_8, i_5, i_3var, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                        if (super.playbackPosition < i_5) {
                            break
                        }

                        super.playbackPosition = i_5 + i_5 - 1 - super.playbackPosition
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                        i_8 = mixSamplesBackward(ints_1, i_8, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                        if (super.playbackPosition >= i_4) {
                            break
                        }

                        super.playbackPosition = i_4 + i_4 - 1 - super.playbackPosition
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                    }
                } else if (super.sampleRate < 0) {
                    while (true) {
                        i_8 = mixSamplesBackward(ints_1, i_8, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                        if (super.playbackPosition >= i_4) {
                            break
                        }

                        super.playbackPosition = i_5 - 1 - (i_5 - 1 - super.playbackPosition) % i_7
                    }
                } else {
                    while (true) {
                        i_8 = mixSamplesForward(ints_1, i_8, i_5, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                        if (super.playbackPosition < i_5) {
                            break
                        }

                        super.playbackPosition = i_4 + (super.playbackPosition - i_4) % i_7
                    }
                }
            } else {
                if (super.loopsRemaining > 0) {
                    if (super.enableLoop) {
                        if (super.sampleRate < 0) {
                            i_8 = mixSamplesBackward(ints_1, i_2, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                            if (super.playbackPosition >= i_4) {
                                return
                            }

                            super.playbackPosition = i_4 + i_4 - 1 - super.playbackPosition
                            super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                            if (--super.loopsRemaining == 0) {
                                // continue to next section
                            } else {
                                do {
                                    i_8 = mixSamplesForward(ints_1, i_8, i_5, i_3var, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                                    if (super.playbackPosition < i_5) {
                                        return
                                    }

                                    super.playbackPosition = i_5 + i_5 - 1 - super.playbackPosition
                                    super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                    if (--super.loopsRemaining == 0) {
                                        break
                                    }

                                    i_8 = mixSamplesBackward(ints_1, i_8, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                                    if (super.playbackPosition >= i_4) {
                                        return
                                    }

                                    super.playbackPosition = i_4 + i_4 - 1 - super.playbackPosition
                                    super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                } while (--super.loopsRemaining != 0)
                            }
                        } else {
                            do {
                                i_8 = mixSamplesForward(ints_1, i_8, i_5, i_3var, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                                if (super.playbackPosition < i_5) {
                                    return
                                }

                                super.playbackPosition = i_5 + i_5 - 1 - super.playbackPosition
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                if (--super.loopsRemaining == 0) {
                                    break
                                }

                                i_8 = mixSamplesBackward(ints_1, i_8, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                                if (super.playbackPosition >= i_4) {
                                    return
                                }

                                super.playbackPosition = i_4 + i_4 - 1 - super.playbackPosition
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                            } while (--super.loopsRemaining != 0)
                        }
                    } else {
                        var i_9: Int
                        if (super.sampleRate < 0) {
                            while (true) {
                                i_8 = mixSamplesBackward(ints_1, i_8, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                                if (super.playbackPosition >= i_4) {
                                    return
                                }

                                i_9 = (i_5 - 1 - super.playbackPosition) / i_7
                                if (i_9 >= super.loopsRemaining) {
                                    super.playbackPosition += i_7 * super.loopsRemaining
                                    super.loopsRemaining = 0
                                    break
                                }

                                super.playbackPosition += i_7 * i_9
                                super.loopsRemaining -= i_9
                            }
                        } else {
                            while (true) {
                                i_8 = mixSamplesForward(ints_1, i_8, i_5, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                                if (super.playbackPosition < i_5) {
                                    return
                                }

                                i_9 = (super.playbackPosition - i_4) / i_7
                                if (i_9 >= super.loopsRemaining) {
                                    super.playbackPosition -= i_7 * super.loopsRemaining
                                    super.loopsRemaining = 0
                                    break
                                }

                                super.playbackPosition -= i_7 * i_9
                                super.loopsRemaining -= i_9
                            }
                        }
                    }
                }

                if (super.sampleRate < 0) {
                    mixSamplesBackward(ints_1, i_8, 0, i_3var, 0.0f)
                    if (super.playbackPosition < 0) {
                        super.playbackPosition = -1
                        cancelFade()
                        unlink()
                    }
                } else {
                    mixSamplesForward(ints_1, i_8, i_6, i_3var, 0.0f)
                    if (super.playbackPosition >= i_6) {
                        super.playbackPosition = i_6
                        cancelFade()
                        unlink()
                    }
                }
            }
        }
    }

    private fun mixSamplesBackward(ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, f_5: Float): Int {
        var currentI2 = i_2
        do {
            if (super.fadeSamplesRemaining <= 0) {
                if (super.sampleRate == -256 && (super.playbackPosition and 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return mixBackwardStereoConstant((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.leftChannelVolume, super.rightChannelVolume, i_4, i_3, this)
                    }

                    return mixBackwardMonoConstant((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.currentVolume, i_4, i_3, this)
                }

                if (PcmPlayerManager.stereo) {
                    return mixBackwardStereoInterpolated((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.leftChannelVolume, super.rightChannelVolume, i_4, i_3, this, super.sampleRate, f_5)
                }

                return mixBackwardMonoInterpolated((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.currentVolume, i_4, i_3, this, super.sampleRate, f_5)
            }

            var i_6 = currentI2 + super.fadeSamplesRemaining
            if (i_6 > i_4) {
                i_6 = i_4
            }

            super.fadeSamplesRemaining += currentI2
            if (super.sampleRate == -256 && (super.playbackPosition and 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    currentI2 = mixBackwardStereoConstantFading((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, i_6, i_3, this)
                } else {
                    currentI2 = mixBackwardMonoConstantFading((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.currentVolume, super.volumeFadeDelta, i_6, i_3, this)
                }
            } else if (PcmPlayerManager.stereo) {
                currentI2 = mixBackwardStereoInterpolatedFading((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, i_6, i_3, this, super.sampleRate, f_5)
            } else {
                currentI2 = mixBackwardMonoInterpolatedFading((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.currentVolume, super.volumeFadeDelta, i_6, i_3, this, super.sampleRate, f_5)
            }

            super.fadeSamplesRemaining -= currentI2
            if (super.fadeSamplesRemaining != 0) {
                return currentI2
            }
        } while (!updateFadeState())

        return i_4
    }

    private fun mixSamplesForward(ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, f_5: Float): Int {
        var currentI2 = i_2
        do {
            if (super.fadeSamplesRemaining <= 0) {
                if (super.sampleRate == 256 && (super.playbackPosition and 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return mixForwardStereoConstant((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.leftChannelVolume, super.rightChannelVolume, i_4, i_3, this)
                    }

                    return mixForwardMonoConstant((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.currentVolume, i_4, i_3, this)
                }

                if (PcmPlayerManager.stereo) {
                    return mixForwardStereoInterpolated((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.leftChannelVolume, super.rightChannelVolume, i_4, i_3, this, super.sampleRate, f_5)
                }

                return mixForwardMonoInterpolated((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.currentVolume, i_4, i_3, this, super.sampleRate, f_5)
            }

            var i_6 = currentI2 + super.fadeSamplesRemaining
            if (i_6 > i_4) {
                i_6 = i_4
            }

            super.fadeSamplesRemaining += currentI2
            if (super.sampleRate == 256 && (super.playbackPosition and 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    currentI2 = mixForwardStereoConstantFading((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, i_6, i_3, this)
                } else {
                    currentI2 = mixForwardMonoConstantFading((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.currentVolume, super.volumeFadeDelta, i_6, i_3, this)
                }
            } else if (PcmPlayerManager.stereo) {
                currentI2 = mixForwardStereoInterpolatedFading((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.leftChannelVolume, super.rightChannelVolume, super.leftVolumeFadeDelta, super.rightVolumeFadeDelta, i_6, i_3, this, super.sampleRate, f_5)
            } else {
                currentI2 = mixForwardMonoInterpolatedFading((buffer as AudioBuffer).getSamples(), ints_1, super.playbackPosition, currentI2, super.currentVolume, super.volumeFadeDelta, i_6, i_3, this, super.sampleRate, f_5)
            }

            super.fadeSamplesRemaining -= currentI2
            if (super.fadeSamplesRemaining != 0) {
                return currentI2
            }
        } while (!updateFadeState())

        return i_4
    }
}
