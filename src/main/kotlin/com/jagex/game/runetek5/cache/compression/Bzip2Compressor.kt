package com.jagex.game.runetek5.cache.compression

object Bzip2Compressor {

    @JvmField val BLOCK_ENTRY = Bzip2BlockEntry()
    @JvmField var anIntArray3288: IntArray? = null

    @JvmStatic
    fun decompress(decompressed: ByteArray, decompressedLength: Int, compressed: ByteArray): Int {
        var length = decompressedLength
        synchronized(BLOCK_ENTRY) {
            BLOCK_ENTRY.compressed = compressed
            BLOCK_ENTRY.startOffset = 9
            BLOCK_ENTRY.decompressed = decompressed
            BLOCK_ENTRY.anInt5506 = 0
            BLOCK_ENTRY.decompressedLength = length
            BLOCK_ENTRY.anInt5532 = 0
            BLOCK_ENTRY.anInt5511 = 0
            BLOCK_ENTRY.anInt5499 = 0
            BLOCK_ENTRY.anInt5508 = 0
            decompress(BLOCK_ENTRY)
            length -= BLOCK_ENTRY.decompressedLength
            BLOCK_ENTRY.compressed = null
            BLOCK_ENTRY.decompressed = null
            return length
        }
    }

    @JvmStatic
    fun method7831(class4590: Bzip2BlockEntry): Byte {
        return method7833(8, class4590).toByte()
    }

    @JvmStatic
    fun method7832(class4590: Bzip2BlockEntry): Byte {
        return method7833(1, class4590).toByte()
    }

    @JvmStatic
    fun method7833(i_0: Int, class4591: Bzip2BlockEntry): Int {
        while (class4591.anInt5532 < i_0) {
            class4591.anInt5511 = class4591.anInt5511 shl 8 or (class4591.compressed!![class4591.startOffset].toInt() and 0xff)
            class4591.anInt5532 += 8
            ++class4591.startOffset
            class4591.anInt5499 += 1564222761
            val i_10000 = class4591.anInt5499
        }

        val i_2 = class4591.anInt5511 shr class4591.anInt5532 - i_0 and (1 shl i_0) - 1
        class4591.anInt5532 -= i_0
        return i_2
    }

    @JvmStatic
    fun method7835(ints_0: IntArray, ints_1: IntArray, ints_2: IntArray, bytes_3: ByteArray, i_4: Int, i_5: Int, i_6: Int) {
        var i_7 = 0

        var i_8: Int
        var i_9: Int
        i_8 = i_4
        while (i_8 <= i_5) {
            i_9 = 0
            while (i_9 < i_6) {
                if (i_8.toByte() == bytes_3[i_9]) {
                    ints_2[i_7] = i_9
                    ++i_7
                }
                i_9++
            }
            i_8++
        }

        i_8 = 0
        while (i_8 < 23) {
            ints_1[i_8] = 0
            i_8++
        }

        i_8 = 0
        while (i_8 < i_6) {
            ++ints_1[bytes_3[i_8].toInt() + 1]
            i_8++
        }

        i_8 = 1
        while (i_8 < 23) {
            ints_1[i_8] += ints_1[i_8 - 1]
            i_8++
        }

        i_8 = 0
        while (i_8 < 23) {
            ints_0[i_8] = 0
            i_8++
        }

        i_8 = 0

        i_9 = i_4
        while (i_9 <= i_5) {
            i_8 += ints_1[i_9 + 1] - ints_1[i_9]
            ints_0[i_9] = i_8 - 1
            i_8 = i_8 shl 1
            i_9++
        }

        i_9 = i_4 + 1
        while (i_9 <= i_5) {
            ints_1[i_9] = (ints_0[i_9 - 1] + 1 shl 1) - ints_1[i_9]
            i_9++
        }
    }

    @JvmStatic
    fun method7836(class4590: Bzip2BlockEntry) {
        var b_1 = class4590.aByte5509
        var i_2 = class4590.anInt5496
        var i_3 = class4590.anInt5518
        var i_4 = class4590.anInt5516
        val ints_5 = anIntArray3288!!
        var i_6 = class4590.anInt5515
        val bytes_7 = class4590.decompressed!!
        var i_8 = class4590.anInt5506
        var i_9 = class4590.decompressedLength
        val i_11 = class4590.anInt5505 + 1

        label56@ while (true) {
            if (i_2 > 0) {
                while (true) {
                    if (i_9 == 0) {
                        break@label56
                    }

                    if (i_2 == 1) {
                        bytes_7[i_8] = b_1
                        ++i_8
                        --i_9
                        break
                    }

                    bytes_7[i_8] = b_1
                    --i_2
                    ++i_8
                    --i_9
                }
            }

            while (i_3 != i_11) {
                b_1 = i_4.toByte()
                i_6 = ints_5[i_6]
                var b_12 = i_6.toByte()
                i_6 = i_6 shr 8
                ++i_3
                if (b_12 != i_4.toByte()) {
                    i_4 = b_12.toInt()
                } else {
                    if (i_3 != i_11) {
                        i_2 = 2
                        i_6 = ints_5[i_6]
                        b_12 = i_6.toByte()
                        i_6 = i_6 shr 8
                        ++i_3
                        if (i_3 != i_11) {
                            if (b_12 != i_4.toByte()) {
                                i_4 = b_12.toInt()
                            } else {
                                i_2 = 3
                                i_6 = ints_5[i_6]
                                b_12 = i_6.toByte()
                                i_6 = i_6 shr 8
                                ++i_3
                                if (i_3 != i_11) {
                                    if (b_12 != i_4.toByte()) {
                                        i_4 = b_12.toInt()
                                    } else {
                                        i_6 = ints_5[i_6]
                                        b_12 = i_6.toByte()
                                        i_6 = i_6 shr 8
                                        ++i_3
                                        i_2 = (b_12.toInt() and 0xff) + 4
                                        i_6 = ints_5[i_6]
                                        i_4 = i_6.toByte().toInt()
                                        i_6 = i_6 shr 8
                                        ++i_3
                                    }
                                }
                            }
                        }
                        continue@label56
                    }
                }
                if (i_9 == 0) {
                    i_2 = 1
                    break@label56
                }
                bytes_7[i_8] = b_1
                ++i_8
                --i_9
            }

            i_2 = 0
            break
        }

        val i_13 = class4590.anInt5508
        val i_10000 = class4590.anInt5508 * -1541698763
        class4590.aByte5509 = b_1
        class4590.anInt5496 = i_2
        class4590.anInt5518 = i_3
        class4590.anInt5516 = i_4
        anIntArray3288 = ints_5
        class4590.anInt5515 = i_6
        class4590.decompressed = bytes_7
        class4590.anInt5506 = i_8
        class4590.decompressedLength = i_9
    }

    @JvmStatic
    fun method7844(class4590: Bzip2BlockEntry) {
        class4590.anInt5520 = 0

        for (i_1 in 0 until 256) {
            if (class4590.inUse[i_1]) {
                class4590.seqToUnseq[class4590.anInt5520] = i_1.toByte()
                ++class4590.anInt5520
            }
        }
    }

    @JvmStatic
    fun decompress(class4590: Bzip2BlockEntry) {
        var i_19 = 0
        var ints_20: IntArray? = null
        var ints_21: IntArray? = null
        var ints_22: IntArray? = null
        class4590.anInt5513 = 1
        if (anIntArray3288 == null) {
            anIntArray3288 = IntArray(class4590.anInt5513 * 100000)
        }

        var bool23 = true

        while (bool23) {
            var b_24 = method7831(class4590)
            if (b_24 == 23.toByte()) {
                break
            }

            b_24 = method7831(class4590)
            b_24 = method7831(class4590)
            b_24 = method7831(class4590)
            b_24 = method7831(class4590)
            b_24 = method7831(class4590)
            b_24 = method7831(class4590)
            b_24 = method7831(class4590)
            b_24 = method7831(class4590)
            b_24 = method7831(class4590)
            b_24 = method7832(class4590)
            class4590.anInt5514 = 0
            var b_25 = method7831(class4590)
            class4590.anInt5514 = class4590.anInt5514 shl 8 or (b_25.toInt() and 0xff)
            b_25 = method7831(class4590)
            class4590.anInt5514 = class4590.anInt5514 shl 8 or (b_25.toInt() and 0xff)
            b_25 = method7831(class4590)
            class4590.anInt5514 = class4590.anInt5514 shl 8 or (b_25.toInt() and 0xff)

            var i_26: Int
            i_26 = 0
            while (i_26 < 16) {
                b_24 = method7832(class4590)
                class4590.inUse16[i_26] = b_24 == 1.toByte()
                i_26++
            }

            i_26 = 0
            while (i_26 < 256) {
                class4590.inUse[i_26] = false
                i_26++
            }

            var i_27: Int
            i_26 = 0
            while (i_26 < 16) {
                if (class4590.inUse16[i_26]) {
                    i_27 = 0
                    while (i_27 < 16) {
                        b_24 = method7832(class4590)
                        if (b_24 == 1.toByte()) {
                            class4590.inUse[i_27 + i_26 * 16] = true
                        }
                        i_27++
                    }
                }
                i_26++
            }

            method7844(class4590)
            i_26 = class4590.anInt5520 + 2
            i_27 = method7833(3, class4590)
            val i_28 = method7833(15, class4590)

            var i_30: Int
            var i_29 = 0
            while (i_29 < i_28) {
                i_30 = 0

                while (true) {
                    b_24 = method7832(class4590)
                    if (b_24 == 0.toByte()) {
                        class4590.selectorMtf[i_29] = i_30.toByte()
                        break
                    }

                    ++i_30
                }
                i_29++
            }

            val bytes_45 = ByteArray(6)

            var b_47: Byte = 0
            while (b_47 < i_27) {
                bytes_45[b_47.toInt()] = b_47
                b_47++
            }

            var b_31: Byte
            var b_32: Byte
            i_30 = 0
            while (i_30 < i_28) {
                b_31 = class4590.selectorMtf[i_30]

                b_32 = bytes_45[b_31.toInt()]
                while (b_31 > 0) {
                    bytes_45[b_31.toInt()] = bytes_45[b_31 - 1]
                    --b_31
                }

                bytes_45[0] = b_32
                class4590.selector[i_30] = b_32
                i_30++
            }

            var i_48: Int
            var i_49: Int
            i_30 = 0
            while (i_30 < i_27) {
                i_48 = method7833(5, class4590)

                i_49 = 0
                while (i_49 < i_26) {
                    while (true) {
                        b_24 = method7832(class4590)
                        if (b_24 == 0.toByte()) {
                            class4590.len[i_30][i_49] = i_48.toByte()
                            break
                        }

                        b_24 = method7832(class4590)
                        if (b_24 == 0.toByte()) {
                            ++i_48
                        } else {
                            --i_48
                        }
                    }
                    i_49++
                }
                i_30++
            }

            var i_33: Int
            i_30 = 0
            while (i_30 < i_27) {
                b_31 = 32
                b_32 = 0

                i_33 = 0
                while (i_33 < i_26) {
                    if (class4590.len[i_30][i_33] > b_32) {
                        b_32 = class4590.len[i_30][i_33]
                    }

                    if (class4590.len[i_30][i_33] < b_31) {
                        b_31 = class4590.len[i_30][i_33]
                    }
                    i_33++
                }

                method7835(class4590.limit[i_30], class4590.base[i_30], class4590.perm[i_30], class4590.len[i_30], b_31.toInt(), b_32.toInt(), i_26)
                class4590.minLens[i_30] = b_31.toInt()
                i_30++
            }

            i_30 = class4590.anInt5520 + 1
            i_48 = -1
            var b_50: Byte = 0

            i_33 = 0
            while (i_33 <= 255) {
                class4590.unzftab[i_33] = 0
                i_33++
            }

            i_33 = 4095

            var i_34: Int
            var i_35: Int
            i_34 = 15
            while (i_34 >= 0) {
                i_35 = 15
                while (i_35 >= 0) {
                    class4590.yy[i_33] = (i_35 + i_34 * 16).toByte()
                    --i_33
                    --i_35
                }

                class4590.anIntArray5525[i_34] = i_33 + 1
                --i_34
            }

            i_34 = 0
            ++i_48
            b_50 = 50
            val b_51 = class4590.selector[i_48]
            i_19 = class4590.minLens[b_51.toInt()].toInt()
            ints_20 = class4590.limit[b_51.toInt()]
            ints_22 = class4590.perm[b_51.toInt()]
            ints_21 = class4590.base[b_51.toInt()]

            i_49 = b_50 - 1
            i_35 = i_19

            var b_36: Byte
            var i_37: Int
            i_37 = method7833(i_19, class4590)
            while (i_37 > ints_20!![i_35]) {
                ++i_35
                b_36 = method7832(class4590)
                i_37 = i_37 shl 1 or b_36.toInt()
            }

            var i_38 = ints_22!![i_37 - ints_21!![i_35]]

            while (true) {
                var i_39: Int
                while (i_38 != i_30) {
                    var i_40: Int
                    if (i_38 != 0 && i_38 != 1) {
                        i_39 = i_38 - 1
                        var i_52: Int
                        if (i_39 < 16) {
                            i_40 = class4590.anIntArray5525[0]

                            b_24 = class4590.yy[i_40 + i_39]
                            while (i_39 > 3) {
                                i_52 = i_40 + i_39
                                class4590.yy[i_52] = class4590.yy[i_52 - 1]
                                class4590.yy[i_52 - 1] = class4590.yy[i_52 - 2]
                                class4590.yy[i_52 - 2] = class4590.yy[i_52 - 3]
                                class4590.yy[i_52 - 3] = class4590.yy[i_52 - 4]
                                i_39 -= 4
                            }

                            while (i_39 > 0) {
                                class4590.yy[i_40 + i_39] = class4590.yy[i_40 + i_39 - 1]
                                --i_39
                            }

                            class4590.yy[i_40] = b_24
                        } else {
                            i_40 = i_39 / 16
                            i_52 = i_39 % 16
                            var i_42 = class4590.anIntArray5525[i_40] + i_52

                            b_24 = class4590.yy[i_42]
                            while (i_42 > class4590.anIntArray5525[i_40]) {
                                class4590.yy[i_42] = class4590.yy[i_42 - 1]
                                --i_42
                            }

                            ++class4590.anIntArray5525[i_40]

                            while (i_40 > 0) {
                                --class4590.anIntArray5525[i_40]
                                class4590.yy[class4590.anIntArray5525[i_40]] = class4590.yy[class4590.anIntArray5525[i_40 - 1] + 16 - 1]
                                --i_40
                            }

                            --class4590.anIntArray5525[0]
                            class4590.yy[class4590.anIntArray5525[0]] = b_24
                            if (class4590.anIntArray5525[0] == 0) {
                                i_33 = 4095

                                var i_43 = 15
                                while (i_43 >= 0) {
                                    var i_44 = 15
                                    while (i_44 >= 0) {
                                        class4590.yy[i_33] = class4590.yy[class4590.anIntArray5525[i_43] + i_44]
                                        --i_33
                                        --i_44
                                    }

                                    class4590.anIntArray5525[i_43] = i_33 + 1
                                    --i_43
                                }
                            }
                        }

                        ++class4590.unzftab[class4590.seqToUnseq[b_24.toInt() and 0xff].toInt() and 0xff]
                        anIntArray3288!![i_34] = class4590.seqToUnseq[b_24.toInt() and 0xff].toInt() and 0xff
                        ++i_34
                        if (i_49 == 0) {
                            ++i_48
                            i_49 = 50
                            val b_53 = class4590.selector[i_48]
                            i_19 = class4590.minLens[b_53.toInt()].toInt()
                            ints_20 = class4590.limit[b_53.toInt()]
                            ints_22 = class4590.perm[b_53.toInt()]
                            ints_21 = class4590.base[b_53.toInt()]
                        }

                        --i_49
                        i_35 = i_19

                        i_37 = method7833(i_19, class4590)
                        while (i_37 > ints_20!![i_35]) {
                            ++i_35
                            b_36 = method7832(class4590)
                            i_37 = i_37 shl 1 or b_36.toInt()
                        }

                        i_38 = ints_22!![i_37 - ints_21!![i_35]]
                    } else {
                        i_39 = -1
                        i_40 = 1

                        do {
                            if (i_38 == 0) {
                                i_39 += i_40
                            } else {
                                i_39 += i_40 * 2
                            }

                            i_40 *= 2
                            if (i_49 == 0) {
                                ++i_48
                                i_49 = 50
                                val b_41 = class4590.selector[i_48]
                                i_19 = class4590.minLens[b_41.toInt()].toInt()
                                ints_20 = class4590.limit[b_41.toInt()]
                                ints_22 = class4590.perm[b_41.toInt()]
                                ints_21 = class4590.base[b_41.toInt()]
                            }

                            --i_49
                            i_35 = i_19

                            i_37 = method7833(i_19, class4590)
                            while (i_37 > ints_20!![i_35]) {
                                ++i_35
                                b_36 = method7832(class4590)
                                i_37 = i_37 shl 1 or b_36.toInt()
                            }

                            i_38 = ints_22!![i_37 - ints_21!![i_35]]
                        } while (i_38 == 0 || i_38 == 1)

                        ++i_39
                        b_25 = class4590.seqToUnseq[class4590.yy[class4590.anIntArray5525[0]].toInt() and 0xff]

                        class4590.unzftab[b_25.toInt() and 0xff] += i_39
                        while (i_39 > 0) {
                            anIntArray3288!![i_34] = b_25.toInt() and 0xff
                            ++i_34
                            --i_39
                        }
                    }
                }

                class4590.anInt5496 = 0
                class4590.aByte5509 = 0
                class4590.anIntArray5519[0] = 0

                i_39 = 1
                while (i_39 <= 256) {
                    class4590.anIntArray5519[i_39] = class4590.unzftab[i_39 - 1]
                    i_39++
                }

                i_39 = 1
                while (i_39 <= 256) {
                    class4590.anIntArray5519[i_39] += class4590.anIntArray5519[i_39 - 1]
                    i_39++
                }

                i_39 = 0
                while (i_39 < i_34) {
                    b_25 = (anIntArray3288!![i_39] and 0xff).toByte()
                    anIntArray3288!![class4590.anIntArray5519[b_25.toInt() and 0xff]] = anIntArray3288!![class4590.anIntArray5519[b_25.toInt() and 0xff]] or (i_39 shl 8)
                    ++class4590.anIntArray5519[b_25.toInt() and 0xff]
                    i_39++
                }

                class4590.anInt5515 = anIntArray3288!![class4590.anInt5514] shr 8
                class4590.anInt5518 = 0
                class4590.anInt5515 = anIntArray3288!![class4590.anInt5515]
                class4590.anInt5516 = (class4590.anInt5515 and 0xff)
                class4590.anInt5515 = class4590.anInt5515 shr 8
                ++class4590.anInt5518
                class4590.anInt5505 = i_34
                method7836(class4590)
                if (class4590.anInt5505 + 1 == class4590.anInt5518 && class4590.anInt5496 == 0) {
                    bool23 = true
                    break
                }

                bool23 = false
                break
            }
        }
    }
}
