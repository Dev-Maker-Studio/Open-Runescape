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
        this.volume = volume
        this.anInt9847 = i_4
        this.anInt9853 = 0
        method15332()
    }

    companion object {
        @JvmStatic
        fun method15729(
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

            class282Sub15Sub5Sub111.anInt9853 = i_4var
            return i_5var shr 1
        }

        @JvmStatic
        fun method15731(
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

            class282Sub15Sub5Sub110.anInt9853 = i_3var shl 8
            return i_4var shr 1
        }

        @JvmStatic
        fun method15734(
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

            class282Sub15Sub5Sub110.anInt9853 = i_4var
            return i_5var
        }

        @JvmStatic
        fun method15735(
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
            class282Sub15Sub5Sub111.anInt9845 -= class282Sub15Sub5Sub111.anInt9852 * i_5var
            class282Sub15Sub5Sub111.anInt9850 -= class282Sub15Sub5Sub111.anInt9851 * i_5var
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

            class282Sub15Sub5Sub111.anInt9845 += class282Sub15Sub5Sub111.anInt9852 * i_5var
            class282Sub15Sub5Sub111.anInt9850 += class282Sub15Sub5Sub111.anInt9851 * i_5var
            class282Sub15Sub5Sub111.anInt9848 = i_6var / 128
            class282Sub15Sub5Sub111.anInt9853 = i_4var
            return i_5var
        }

        @JvmStatic
        fun method15737(
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

            class282Sub15Sub5Sub112.anInt9848 += class282Sub15Sub5Sub112.anInt9849 * (i_91 - i_4)
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

            class282Sub15Sub5Sub112.anInt9845 = i_5var / 128 shr 2
            class282Sub15Sub5Sub112.anInt9850 = i_6var / 128 shr 2
            class282Sub15Sub5Sub112.anInt9853 = i_3var shl 8
            return i_4var shr 1
        }

        @JvmStatic
        fun method15738(
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

            class282Sub15Sub5Sub19.anInt9845 += class282Sub15Sub5Sub19.anInt9852 * (i_61 - i_3)
            class282Sub15Sub5Sub19.anInt9850 += class282Sub15Sub5Sub19.anInt9851 * (i_61 - i_3)

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

            class282Sub15Sub5Sub19.anInt9848 = i_4var / 128 shr 2
            class282Sub15Sub5Sub19.anInt9853 = i_2var shl 8
            return i_3var
        }

        @JvmStatic
        fun method15739(
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

            class282Sub15Sub5Sub112.anInt9848 += class282Sub15Sub5Sub112.anInt9849 * (i_91 - i_4)
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

            class282Sub15Sub5Sub112.anInt9845 = i_5var / 128 shr 2
            class282Sub15Sub5Sub112.anInt9850 = i_6var / 128 shr 2
            class282Sub15Sub5Sub112.anInt9853 = i_3var shl 8
            return i_4var shr 1
        }

        @JvmStatic
        fun method15740(
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

            class282Sub15Sub5Sub18.anInt9853 = i_2var shl 8
            return i_3var
        }

        @JvmStatic
        fun method15741(
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
            class282Sub15Sub5Sub113.anInt9848 -= i_5var * class282Sub15Sub5Sub113.anInt9849
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
            class282Sub15Sub5Sub113.anInt9848 += class282Sub15Sub5Sub113.anInt9849 * i_5var
            class282Sub15Sub5Sub113.anInt9845 = i_6var / 128
            class282Sub15Sub5Sub113.anInt9850 = i_7var / 128
            class282Sub15Sub5Sub113.anInt9853 = i_4var
            return i_5var
        }

        @JvmStatic
        fun method15742(
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
            class282Sub15Sub5Sub111.anInt9845 -= class282Sub15Sub5Sub111.anInt9852 * i_5var
            class282Sub15Sub5Sub111.anInt9850 -= class282Sub15Sub5Sub111.anInt9851 * i_5var
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

            class282Sub15Sub5Sub111.anInt9845 += class282Sub15Sub5Sub111.anInt9852 * i_5var
            class282Sub15Sub5Sub111.anInt9850 += class282Sub15Sub5Sub111.anInt9851 * i_5var
            class282Sub15Sub5Sub111.anInt9848 = i_6var / 128
            class282Sub15Sub5Sub111.anInt9853 = i_4var
            return i_5var
        }

        @JvmStatic
        fun method15743(
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
            class282Sub15Sub5Sub113.anInt9848 -= i_5var * class282Sub15Sub5Sub113.anInt9849
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
            class282Sub15Sub5Sub113.anInt9848 += class282Sub15Sub5Sub113.anInt9849 * i_5var
            class282Sub15Sub5Sub113.anInt9845 = i_6var / 128
            class282Sub15Sub5Sub113.anInt9850 = i_7var / 128
            class282Sub15Sub5Sub113.anInt9853 = i_4var
            return i_5var
        }

        @JvmStatic
        fun method15746(
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

            class282Sub15Sub5Sub111.anInt9853 = i_4var
            return i_5var shr 1
        }

        @JvmStatic
        fun method15747(
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

            class282Sub15Sub5Sub110.anInt9853 = i_4var
            return i_5var
        }

        @JvmStatic
        fun method15770(
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

            class282Sub15Sub5Sub110.anInt9853 = i_3var shl 8
            return i_4var shr 1
        }

        @JvmStatic
        fun method15774(
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

            class282Sub15Sub5Sub18.anInt9853 = i_2var shl 8
            return i_3var
        }

        @JvmStatic
        fun method15777(
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

            class282Sub15Sub5Sub19.anInt9845 += class282Sub15Sub5Sub19.anInt9852 * (i_61 - i_3)
            class282Sub15Sub5Sub19.anInt9850 += class282Sub15Sub5Sub19.anInt9851 * (i_61 - i_3)

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

            class282Sub15Sub5Sub19.anInt9848 = i_4var / 128 shr 2
            class282Sub15Sub5Sub19.anInt9853 = i_2var shl 8
            return i_3var
        }
    }

    @Synchronized
    override fun fill(ints_1: IntArray, i_2: Int, i_3: Int) {
        if (super.volume == 0 && super.anInt9858 == 0) {
            skip(i_3)
        } else {
            var i_4 = super.start shl 8
            var i_5 = super.end shl 8
            val i_6 = ((buffer as AudioBuffer).getTotalSamples()) shl 8
            val i_7 = i_5 - i_4
            if (i_7 <= 0) {
                super.anInt9854 = 0
            }

            var i_8 = i_2
            var i_3var = i_3 + i_2
            if (super.anInt9853 < 0) {
                if (super.sampleRate <= 0) {
                    method15354()
                    unlink()
                    return
                }

                super.anInt9853 = 0
            }

            if (super.anInt9853 >= i_6) {
                if (super.sampleRate >= 0) {
                    method15354()
                    unlink()
                    return
                }

                super.anInt9853 = i_6 - 1
            }

            if (super.anInt9854 < 0) {
                if (super.enableLoop) {
                    if (super.sampleRate < 0) {
                        i_8 = method15732(ints_1, i_2, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                        if (super.anInt9853 >= i_4) {
                            return
                        }

                        super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                    }

                    while (true) {
                        i_8 = method15769(ints_1, i_8, i_5, i_3var, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                        if (super.anInt9853 < i_5) {
                            break
                        }

                        super.anInt9853 = i_5 + i_5 - 1 - super.anInt9853
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                        i_8 = method15732(ints_1, i_8, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                        if (super.anInt9853 >= i_4) {
                            break
                        }

                        super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853
                        super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                    }
                } else if (super.sampleRate < 0) {
                    while (true) {
                        i_8 = method15732(ints_1, i_8, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                        if (super.anInt9853 >= i_4) {
                            break
                        }

                        super.anInt9853 = i_5 - 1 - (i_5 - 1 - super.anInt9853) % i_7
                    }
                } else {
                    while (true) {
                        i_8 = method15769(ints_1, i_8, i_5, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                        if (super.anInt9853 < i_5) {
                            break
                        }

                        super.anInt9853 = i_4 + (super.anInt9853 - i_4) % i_7
                    }
                }
            } else {
                if (super.anInt9854 > 0) {
                    if (super.enableLoop) {
                        if (super.sampleRate < 0) {
                            i_8 = method15732(ints_1, i_2, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                            if (super.anInt9853 >= i_4) {
                                return
                            }

                            super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853
                            super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                            if (--super.anInt9854 == 0) {
                                // continue to next section
                            } else {
                                do {
                                    i_8 = method15769(ints_1, i_8, i_5, i_3var, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                                    if (super.anInt9853 < i_5) {
                                        return
                                    }

                                    super.anInt9853 = i_5 + i_5 - 1 - super.anInt9853
                                    super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                    if (--super.anInt9854 == 0) {
                                        break
                                    }

                                    i_8 = method15732(ints_1, i_8, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                                    if (super.anInt9853 >= i_4) {
                                        return
                                    }

                                    super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853
                                    super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                } while (--super.anInt9854 != 0)
                            }
                        } else {
                            do {
                                i_8 = method15769(ints_1, i_8, i_5, i_3var, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                                if (super.anInt9853 < i_5) {
                                    return
                                }

                                super.anInt9853 = i_5 + i_5 - 1 - super.anInt9853
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                                if (--super.anInt9854 == 0) {
                                    break
                                }

                                i_8 = method15732(ints_1, i_8, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                                if (super.anInt9853 >= i_4) {
                                    return
                                }

                                super.anInt9853 = i_4 + i_4 - 1 - super.anInt9853
                                super.sampleRate = -(super.sampleRate * 1927994969) * -177305111
                            } while (--super.anInt9854 != 0)
                        }
                    } else {
                        var i_9: Int
                        if (super.sampleRate < 0) {
                            while (true) {
                                i_8 = method15732(ints_1, i_8, i_4, i_3var, (buffer as AudioBuffer).getSamples()[super.end - 1].toFloat())
                                if (super.anInt9853 >= i_4) {
                                    return
                                }

                                i_9 = (i_5 - 1 - super.anInt9853) / i_7
                                if (i_9 >= super.anInt9854) {
                                    super.anInt9853 += i_7 * super.anInt9854
                                    super.anInt9854 = 0
                                    break
                                }

                                super.anInt9853 += i_7 * i_9
                                super.anInt9854 -= i_9
                            }
                        } else {
                            while (true) {
                                i_8 = method15769(ints_1, i_8, i_5, i_3var, (buffer as AudioBuffer).getSamples()[super.start].toFloat())
                                if (super.anInt9853 < i_5) {
                                    return
                                }

                                i_9 = (super.anInt9853 - i_4) / i_7
                                if (i_9 >= super.anInt9854) {
                                    super.anInt9853 -= i_7 * super.anInt9854
                                    super.anInt9854 = 0
                                    break
                                }

                                super.anInt9853 -= i_7 * i_9
                                super.anInt9854 -= i_9
                            }
                        }
                    }
                }

                if (super.sampleRate < 0) {
                    method15732(ints_1, i_8, 0, i_3var, 0.0f)
                    if (super.anInt9853 < 0) {
                        super.anInt9853 = -1
                        method15354()
                        unlink()
                    }
                } else {
                    method15769(ints_1, i_8, i_6, i_3var, 0.0f)
                    if (super.anInt9853 >= i_6) {
                        super.anInt9853 = i_6
                        method15354()
                        unlink()
                    }
                }
            }
        }
    }

    private fun method15732(ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, f_5: Float): Int {
        var currentI2 = i_2
        do {
            if (super.anInt9858 <= 0) {
                if (super.sampleRate == -256 && (super.anInt9853 and 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return method15731((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9845, super.anInt9850, i_4, i_3, this)
                    }

                    return method15774((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9848, i_4, i_3, this)
                }

                if (PcmPlayerManager.stereo) {
                    return method15729((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9845, super.anInt9850, i_4, i_3, this, super.sampleRate, f_5)
                }

                return method15734((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9848, i_4, i_3, this, super.sampleRate, f_5)
            }

            var i_6 = currentI2 + super.anInt9858
            if (i_6 > i_4) {
                i_6 = i_4
            }

            super.anInt9858 += currentI2
            if (super.sampleRate == -256 && (super.anInt9853 and 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    currentI2 = method15739((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this)
                } else {
                    currentI2 = method15738((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9848, super.anInt9849, i_6, i_3, this)
                }
            } else if (PcmPlayerManager.stereo) {
                currentI2 = method15743((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this, super.sampleRate, f_5)
            } else {
                currentI2 = method15742((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9848, super.anInt9849, i_6, i_3, this, super.sampleRate, f_5)
            }

            super.anInt9858 -= currentI2
            if (super.anInt9858 != 0) {
                return currentI2
            }
        } while (!method15324())

        return i_4
    }

    private fun method15769(ints_1: IntArray, i_2: Int, i_3: Int, i_4: Int, f_5: Float): Int {
        var currentI2 = i_2
        do {
            if (super.anInt9858 <= 0) {
                if (super.sampleRate == 256 && (super.anInt9853 and 0xff) == 0) {
                    if (PcmPlayerManager.stereo) {
                        return method15770((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9845, super.anInt9850, i_4, i_3, this)
                    }

                    return method15740((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9848, i_4, i_3, this)
                }

                if (PcmPlayerManager.stereo) {
                    return method15746((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9845, super.anInt9850, i_4, i_3, this, super.sampleRate, f_5)
                }

                return method15747((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9848, i_4, i_3, this, super.sampleRate, f_5)
            }

            var i_6 = currentI2 + super.anInt9858
            if (i_6 > i_4) {
                i_6 = i_4
            }

            super.anInt9858 += currentI2
            if (super.sampleRate == 256 && (super.anInt9853 and 0xff) == 0) {
                if (PcmPlayerManager.stereo) {
                    currentI2 = method15737((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this)
                } else {
                    currentI2 = method15777((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9848, super.anInt9849, i_6, i_3, this)
                }
            } else if (PcmPlayerManager.stereo) {
                currentI2 = method15741((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9845, super.anInt9850, super.anInt9852, super.anInt9851, i_6, i_3, this, super.sampleRate, f_5)
            } else {
                currentI2 = method15735((buffer as AudioBuffer).getSamples(), ints_1, super.anInt9853, currentI2, super.anInt9848, super.anInt9849, i_6, i_3, this, super.sampleRate, f_5)
            }

            super.anInt9858 -= currentI2
            if (super.anInt9858 != 0) {
                return currentI2
            }
        } while (!method15324())

        return i_4
    }
}
