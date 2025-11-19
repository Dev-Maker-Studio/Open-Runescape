package com.jagex.game.runetek5.graphics.texture.water

internal class Class264 {

    init {
        throw Error()
    }

    companion object {
        @JvmStatic
        fun method4781(i_0: Int, class2615: Class261, f_6: Float, f_7: Float, f_8: Float, f_9: Float, bytes_11: ByteArray, i_12: Int) {
            val i_13 = 16384
            val floats_14 = FloatArray(i_13)

            var i_15: Int
            var i_16: Int
            var f_6_var = f_6
            var f_7_var = f_7
            var f_8_var = f_8
            var f_9_var = f_9

            for (i_15 in 0 until 8) {
                i_16 = i_12
                class2615.method4634(i_0, f_6_var / 128, f_7_var / 128, f_8_var / 16, f_9_var * 127.0f, floats_14)

                for (i_17 in 0 until i_13) {
                    bytes_11[i_16] = (bytes_11[i_16] + floats_14[i_17]).toInt().toByte()
                    ++i_16
                }

                f_6_var *= 2.0f
                f_7_var *= 2.0f
                f_8_var *= 2.0f
                f_9_var *= 0.6f
            }

            i_15 = i_12

            for (i_16 in 0 until i_13) {
                bytes_11[i_15] = (bytes_11[i_15] + 127).toByte()
                ++i_15
            }
        }

        @JvmStatic
        fun method4782(class2614: Class261): ByteArray {
            val bytes_10 = ByteArray(262144)
            method4783(class2614, bytes_10, 0)
            return bytes_10
        }

        @JvmStatic
        fun method4783(class2614: Class261, bytes_10: ByteArray, i_11: Int) {
            var i_11_var = i_11
            for (i_12 in 0 until 16) {
                method4781(i_12, class2614, 4.0f, 4.0f, 16.0f, 0.5f, bytes_10, i_11_var)
                i_11_var += 16384
            }
        }
    }
}
