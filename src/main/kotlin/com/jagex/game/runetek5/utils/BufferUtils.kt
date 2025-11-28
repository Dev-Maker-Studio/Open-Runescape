package com.jagex.game.runetek5.utils

object BufferUtils {

    @JvmStatic
    fun ilog(num: Int): Int {
        var num = num
        var sizeShift = 0
        if (num < 0 || num >= 65536) {
            num = num ushr 16
            sizeShift += 16
        }
        if (num >= 256) {
            num = num ushr 8
            sizeShift += 8
        }
        if (num >= 16) {
            num = num ushr 4
            sizeShift += 4
        }
        if (num >= 4) {
            num = num ushr 2
            sizeShift += 2
        }
        if (num >= 1) {
            num = num ushr 1
            sizeShift++
        }
        return num + sizeShift
    }

    @JvmStatic
    fun generateBytePatterns(i_0: Int): Array<Array<ByteArray?>> {
        val bytes_2 = Array(8) { arrayOfNulls<ByteArray>(4) }
        val i_3 = i_0
        val i_4 = i_0
        var bytes_5 = ByteArray(i_0 * i_0)
        var i_6 = 0

        var i_7: Int
        var i_8: Int
        i_7 = 0
        while (i_7 < i_4) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 <= i_7) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            i_7++
        }

        bytes_2[0][0] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = 0
            while (i_8 < i_4) {
                if (i_8 <= i_7) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            --i_7
        }

        bytes_2[0][1] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 >= i_7) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            i_7++
        }

        bytes_2[0][2] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 >= i_7) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            --i_7
        }

        bytes_2[0][3] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 <= i_7 shr 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            --i_7
        }

        bytes_2[1][0] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_6 >= 0 && i_6 < bytes_5.size) {
                    if (i_8 >= i_7 shl 1) {
                        bytes_5[i_6] = -1
                    }
                }
                ++i_6
                i_8++
            }
            i_7++
        }

        bytes_2[1][1] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = i_3 - 1
            while (i_8 >= 0) {
                if (i_8 <= i_7 shr 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                --i_8
            }
            i_7++
        }

        bytes_2[1][2] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = i_3 - 1
            while (i_8 >= 0) {
                if (i_8 >= i_7 shl 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                --i_8
            }
            --i_7
        }

        bytes_2[1][3] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = i_3 - 1
            while (i_8 >= 0) {
                if (i_8 <= i_7 shr 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                --i_8
            }
            --i_7
        }

        bytes_2[2][0] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 >= i_7 shl 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            --i_7
        }

        bytes_2[2][1] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 <= i_7 shr 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            i_7++
        }

        bytes_2[2][2] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = i_3 - 1
            while (i_8 >= 0) {
                if (i_8 >= i_7 shl 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                --i_8
            }
            i_7++
        }

        bytes_2[2][3] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 >= i_7 shr 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            --i_7
        }

        bytes_2[3][0] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 <= i_7 shl 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            i_7++
        }

        bytes_2[3][1] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = i_3 - 1
            while (i_8 >= 0) {
                if (i_8 >= i_7 shr 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                --i_8
            }
            i_7++
        }

        bytes_2[3][2] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = i_3 - 1
            while (i_8 >= 0) {
                if (i_8 <= i_7 shl 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                --i_8
            }
            --i_7
        }

        bytes_2[3][3] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = i_3 - 1
            while (i_8 >= 0) {
                if (i_8 >= i_7 shr 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                --i_8
            }
            --i_7
        }

        bytes_2[4][0] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 <= i_7 shl 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            --i_7
        }

        bytes_2[4][1] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 >= i_7 shr 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            i_7++
        }

        bytes_2[4][2] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = i_3 - 1
            while (i_8 >= 0) {
                if (i_8 <= i_7 shl 1) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                --i_8
            }
            i_7++
        }

        bytes_2[4][3] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 <= i_3 / 2) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            i_7++
        }

        bytes_2[5][0] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_7 <= i_4 / 2) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            i_7++
        }

        bytes_2[5][1] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 >= i_3 / 2) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            i_7++
        }

        bytes_2[5][2] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_7 >= i_4 / 2) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            i_7++
        }

        bytes_2[5][3] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 <= i_7 - i_4 / 2) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            i_7++
        }

        bytes_2[6][0] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 <= i_7 - i_4 / 2) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            --i_7
        }

        bytes_2[6][1] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = i_3 - 1
            while (i_8 >= 0) {
                if (i_8 <= i_7 - i_4 / 2) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                --i_8
            }
            --i_7
        }

        bytes_2[6][2] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = i_3 - 1
            while (i_8 >= 0) {
                if (i_8 <= i_7 - i_4 / 2) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                --i_8
            }
            i_7++
        }

        bytes_2[6][3] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 >= i_7 - i_4 / 2) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            i_7++
        }

        bytes_2[7][0] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = 0
            while (i_8 < i_3) {
                if (i_8 >= i_7 - i_4 / 2) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                i_8++
            }
            --i_7
        }

        bytes_2[7][1] = bytes_5
        bytes_5 = ByteArray(i_4 * i_3)
        i_6 = 0

        i_7 = i_4 - 1
        while (i_7 >= 0) {
            i_8 = i_3 - 1
            while (i_8 >= 0) {
                if (i_8 >= i_7 - i_4 / 2) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                --i_8
            }
            --i_7
        }

        bytes_2[7][2] = bytes_5
        bytes_5 = ByteArray(i_3 * i_4)
        i_6 = 0

        i_7 = 0
        while (i_7 < i_4) {
            i_8 = i_3 - 1
            while (i_8 >= 0) {
                if (i_8 >= i_7 - i_4 / 2) {
                    bytes_5[i_6] = -1
                }
                ++i_6
                --i_8
            }
            i_7++
        }

        bytes_2[7][3] = bytes_5
        return bytes_2
    }
}
