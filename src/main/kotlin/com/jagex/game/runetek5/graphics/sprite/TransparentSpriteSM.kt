package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.framebuffer.SafeModeFrameBuffer
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.toolkit.ComponentDimensionSM

class TransparentSpriteSM : SpriteSM {

    @JvmField
    val anIntArray10235: IntArray

    constructor(hardwarerenderer1: SafeModeToolkit, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, bool7: Boolean) : super(hardwarerenderer1, i_5, i_6) {
        if (bool7) {
            this.anIntArray10235 = IntArray(i_5 * i_6)
        } else {
            this.anIntArray10235 = ints_2
        }

        var adjustedI4 = i_4 - anInt8851
        var i_8 = 0
        var i_3Var = i_3

        for (i_9 in 0 until i_6) {
            for (i_10 in 0 until i_5) {
                this.anIntArray10235[i_8++] = ints_2[i_3Var++]
            }
            i_3Var += adjustedI4
        }
    }

    constructor(hardwarerenderer1: SafeModeToolkit, ints_2: IntArray, i_3: Int, i_4: Int) : super(hardwarerenderer1, i_3, i_4) {
        this.anIntArray10235 = ints_2
    }

    constructor(hardwarerenderer1: SafeModeToolkit, i_2: Int, i_3: Int) : super(hardwarerenderer1, i_2, i_3) {
        this.anIntArray10235 = IntArray(i_3 * i_2)
    }

    override fun getFramebuffer(): FrameBuffer {
        return SafeModeFrameBuffer(anInt8851, anInt8867, this.anIntArray10235)
    }

    override fun draw(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
        var i_26 = i_2
        var i_110 = i_1
        var i_41 = i_4
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        val ints_6 = toolkit.colourBuffer ?: return
        val i_7 = toolkit.viewportWidth
        i_110 += anInt8853
        i_26 += anInt8854
        var i_8 = i_7 * i_26 + i_110
        var i_9 = 0
        var i_10 = anInt8867
        var i_11 = anInt8851
        var i_12 = i_7 - i_11
        var i_13 = 0
        var i_14: Int
        if (i_26 < toolkit.clipTop) {
            i_14 = toolkit.clipTop - i_26
            i_10 -= i_14
            i_26 = toolkit.clipTop
            i_9 += i_14 * i_11
            i_8 += i_14 * i_7
        }

        if (i_10 + i_26 > toolkit.clipBottom) {
            i_10 -= i_10 + i_26 - toolkit.clipBottom
        }

        if (i_110 < toolkit.clipLeft) {
            i_14 = toolkit.clipLeft - i_110
            i_11 -= i_14
            i_110 = toolkit.clipLeft
            i_9 += i_14
            i_8 += i_14
            i_13 += i_14
            i_12 += i_14
        }

        if (i_11 + i_110 > toolkit.clipRight) {
            i_14 = i_11 + i_110 - toolkit.clipRight
            i_11 -= i_14
            i_13 += i_14
            i_12 += i_14
        }

        if (i_11 > 0 && i_10 > 0) {
            var i_15: Int
            var i_16: Int
            var i_17: Int
            var i_18: Int
            var i_19: Int
            var i_20: Int
            var i_21: Int
            var i_22: Int
            if (i_5 == 0) {
                if (i_3 == 1) {
                    i_14 = -i_10
                    while (i_14 < 0) {
                        i_15 = i_8 + i_11 - 3
                        while (i_8 < i_15) {
                            ints_6[i_8++] = this.anIntArray10235[i_9++]
                            ints_6[i_8++] = this.anIntArray10235[i_9++]
                            ints_6[i_8++] = this.anIntArray10235[i_9++]
                            ints_6[i_8++] = this.anIntArray10235[i_9++]
                        }

                        i_15 += 3
                        while (i_8 < i_15) {
                            ints_6[i_8++] = this.anIntArray10235[i_9++]
                        }

                        i_8 += i_12
                        i_9 += i_13
                        i_14++
                    }
                } else if (i_3 == 0) {
                    i_14 = (i_41 and 0xff0000) shr 16
                    i_15 = (i_41 and 0xff00) shr 8
                    i_16 = i_41 and 0xff

                    i_17 = -i_10
                    while (i_17 < 0) {
                        i_18 = -i_11
                        while (i_18 < 0) {
                            i_19 = this.anIntArray10235[i_9++]
                            i_20 = i_14 * (i_19 and 0xff0000) and 0xff000000.toInt()
                            i_21 = i_15 * (i_19 and 0xff00) and 0xff0000
                            i_22 = (i_19 and 0xff) * i_16 and 0xff00
                            ints_6[i_8++] = (i_20 or i_21 or i_22) ushr 8
                            i_18++
                        }

                        i_8 += i_12
                        i_9 += i_13
                        i_17++
                    }
                } else if (i_3 == 3) {
                    i_14 = -i_10
                    while (i_14 < 0) {
                        i_15 = -i_11
                        while (i_15 < 0) {
                            i_16 = this.anIntArray10235[i_9++]
                            i_17 = i_16 + i_41
                            i_18 = (i_16 and 0xff00ff) + (i_41 and 0xff00ff)
                            i_19 = (i_18 and 0x1000100) + (i_17 - i_18 and 0x10000)
                            ints_6[i_8++] = i_17 - i_19 or (i_19 - (i_19 ushr 8))
                            i_15++
                        }

                        i_8 += i_12
                        i_9 += i_13
                        i_14++
                    }
                } else {
                    if (i_3 != 2) {
                        throw IllegalArgumentException()
                    }

                    i_14 = i_41 ushr 24
                    i_15 = 256 - i_14
                    i_16 = i_15 * (i_41 and 0xff00ff) and -16711936
                    i_17 = i_15 * (i_41 and 0xff00) and 0xff0000
                    i_41 = (i_16 or i_17) ushr 8

                    i_18 = -i_10
                    while (i_18 < 0) {
                        i_19 = -i_11
                        while (i_19 < 0) {
                            i_20 = this.anIntArray10235[i_9++]
                            i_16 = (i_20 and 0xff00ff) * i_14 and -16711936
                            i_17 = i_14 * (i_20 and 0xff00) and 0xff0000
                            ints_6[i_8++] = i_41 + ((i_16 or i_17) ushr 8)
                            i_19++
                        }

                        i_8 += i_12
                        i_9 += i_13
                        i_18++
                    }
                }
            } else {
                var i_23: Int
                var i_24: Int
                var i_25: Int
                if (i_5 == 1) {
                    if (i_3 == 1) {
                        i_14 = -i_10
                        while (i_14 < 0) {
                            i_15 = -i_11
                            while (i_15 < 0) {
                                i_16 = this.anIntArray10235[i_9++]
                                i_17 = i_16 ushr 24
                                i_18 = 256 - i_17
                                i_19 = ints_6[i_8]
                                ints_6[i_8++] = (((i_16 and -16711936) ushr 8) * i_17 + ((i_19 and -16711936) ushr 8) * i_18 and -16711936) + (((i_16 and 0xff00ff) * i_17 + (i_19 and 0xff00ff) * i_18 and -16711936) shr 8)
                                i_15++
                            }

                            i_8 += i_12
                            i_9 += i_13
                            i_14++
                        }
                    } else if (i_3 == 0) {
                        if ((i_41 and 0xffffff) == 16777215) {
                            i_14 = -i_10
                            while (i_14 < 0) {
                                i_15 = -i_11
                                while (i_15 < 0) {
                                    i_16 = this.anIntArray10235[i_9++]
                                    i_17 = (i_16 ushr 24) * (i_41 ushr 24) shr 8
                                    i_18 = 256 - i_17
                                    i_19 = ints_6[i_8]
                                    ints_6[i_8++] = ((i_16 and 0xff00ff) * i_17 + (i_19 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_16 and 0xff00) + i_18 * (i_19 and 0xff00) and 0xff0000) shr 8
                                    i_15++
                                }

                                i_8 += i_12
                                i_9 += i_13
                                i_14++
                            }
                        } else {
                            i_14 = (i_41 and 0xff0000) shr 16
                            i_15 = (i_41 and 0xff00) shr 8
                            i_16 = i_41 and 0xff

                            i_17 = -i_10
                            while (i_17 < 0) {
                                i_18 = -i_11
                                while (i_18 < 0) {
                                    i_19 = this.anIntArray10235[i_9++]
                                    i_20 = (i_19 ushr 24) * (i_41 ushr 24) shr 8
                                    i_21 = 256 - i_20
                                    if (i_20 != 255) {
                                        i_22 = i_14 * (i_19 and 0xff0000) and 0xff000000.toInt()
                                        i_23 = i_15 * (i_19 and 0xff00) and 0xff0000
                                        i_24 = (i_19 and 0xff) * i_16 and 0xff00
                                        i_19 = (i_22 or i_23 or i_24) ushr 8
                                        i_25 = ints_6[i_8]
                                        ints_6[i_8++] = ((i_19 and 0xff00ff) * i_20 + (i_25 and 0xff00ff) * i_21 and -16711936) + (i_20 * (i_19 and 0xff00) + i_21 * (i_25 and 0xff00) and 0xff0000) shr 8
                                    } else {
                                        i_22 = i_14 * (i_19 and 0xff0000) and 0xff000000.toInt()
                                        i_23 = i_15 * (i_19 and 0xff00) and 0xff0000
                                        i_24 = (i_19 and 0xff) * i_16 and 0xff00
                                        ints_6[i_8++] = (i_22 or i_23 or i_24) ushr 8
                                    }
                                    i_18++
                                }

                                i_8 += i_12
                                i_9 += i_13
                                i_17++
                            }
                        }
                    } else if (i_3 == 3) {
                        i_14 = -i_10
                        while (i_14 < 0) {
                            i_15 = -i_11
                            while (i_15 < 0) {
                                i_16 = this.anIntArray10235[i_9++]
                                i_17 = i_16 + i_41
                                i_18 = (i_16 and 0xff00ff) + (i_41 and 0xff00ff)
                                i_19 = (i_18 and 0x1000100) + (i_17 - i_18 and 0x10000)
                                i_19 = i_17 - i_19 or (i_19 - (i_19 ushr 8))
                                i_20 = (i_19 ushr 24) * (i_41 ushr 24) shr 8
                                i_21 = 256 - i_20
                                if (i_20 != 255) {
                                    i_16 = i_19
                                    i_19 = ints_6[i_8]
                                    i_19 = ((i_16 and 0xff00ff) * i_20 + (i_19 and 0xff00ff) * i_21 and -16711936) + (i_20 * (i_16 and 0xff00) + i_21 * (i_19 and 0xff00) and 0xff0000) shr 8
                                }

                                ints_6[i_8++] = i_19
                                i_15++
                            }

                            i_8 += i_12
                            i_9 += i_13
                            i_14++
                        }
                    } else {
                        if (i_3 != 2) {
                            throw IllegalArgumentException()
                        }

                        i_14 = i_41 ushr 24
                        i_15 = 256 - i_14
                        i_16 = i_15 * (i_41 and 0xff00ff) and -16711936
                        i_17 = i_15 * (i_41 and 0xff00) and 0xff0000
                        i_41 = (i_16 or i_17) ushr 8

                        i_18 = -i_10
                        while (i_18 < 0) {
                            i_19 = -i_11
                            while (i_19 < 0) {
                                i_20 = this.anIntArray10235[i_9++]
                                i_21 = i_20 ushr 24
                                i_22 = 256 - i_21
                                i_16 = (i_20 and 0xff00ff) * i_14 and -16711936
                                i_17 = i_14 * (i_20 and 0xff00) and 0xff0000
                                i_20 = i_41 + ((i_16 or i_17) ushr 8)
                                i_23 = ints_6[i_8]
                                ints_6[i_8++] = ((i_20 and 0xff00ff) * i_21 + (i_23 and 0xff00ff) * i_22 and -16711936) + (i_21 * (i_20 and 0xff00) + i_22 * (i_23 and 0xff00) and 0xff0000) shr 8
                                i_19++
                            }

                            i_8 += i_12
                            i_9 += i_13
                            i_18++
                        }
                    }
                } else {
                    if (i_5 != 2) {
                        throw IllegalArgumentException()
                    }

                    if (i_3 == 1) {
                        i_14 = -i_10
                        while (i_14 < 0) {
                            i_15 = -i_11
                            while (i_15 < 0) {
                                i_16 = this.anIntArray10235[i_9++]
                                if (i_16 != 0) {
                                    i_17 = ints_6[i_8]
                                    i_18 = i_16 + i_17
                                    i_19 = (i_16 and 0xff00ff) + (i_17 and 0xff00ff)
                                    i_17 = (i_19 and 0x1000100) + (i_18 - i_19 and 0x10000)
                                    ints_6[i_8++] = i_18 - i_17 or (i_17 - (i_17 ushr 8))
                                } else {
                                    ++i_8
                                }
                                i_15++
                            }

                            i_8 += i_12
                            i_9 += i_13
                            i_14++
                        }
                    } else if (i_3 == 0) {
                        i_14 = (i_41 and 0xff0000) shr 16
                        i_15 = (i_41 and 0xff00) shr 8
                        i_16 = i_41 and 0xff

                        i_17 = -i_10
                        while (i_17 < 0) {
                            i_18 = -i_11
                            while (i_18 < 0) {
                                i_19 = this.anIntArray10235[i_9++]
                                if (i_19 != 0) {
                                    i_20 = i_14 * (i_19 and 0xff0000) and 0xff000000.toInt()
                                    i_21 = i_15 * (i_19 and 0xff00) and 0xff0000
                                    i_22 = (i_19 and 0xff) * i_16 and 0xff00
                                    i_19 = (i_20 or i_21 or i_22) ushr 8
                                    i_23 = ints_6[i_8]
                                    i_24 = i_19 + i_23
                                    i_25 = (i_19 and 0xff00ff) + (i_23 and 0xff00ff)
                                    i_23 = (i_25 and 0x1000100) + (i_24 - i_25 and 0x10000)
                                    ints_6[i_8++] = i_24 - i_23 or (i_23 - (i_23 ushr 8))
                                } else {
                                    ++i_8
                                }
                                i_18++
                            }

                            i_8 += i_12
                            i_9 += i_13
                            i_17++
                        }
                    } else if (i_3 == 3) {
                        i_14 = -i_10
                        while (i_14 < 0) {
                            i_15 = -i_11
                            while (i_15 < 0) {
                                i_16 = this.anIntArray10235[i_9++]
                                i_17 = i_16 + i_41
                                i_18 = (i_16 and 0xff00ff) + (i_41 and 0xff00ff)
                                i_19 = (i_18 and 0x1000100) + (i_17 - i_18 and 0x10000)
                                i_16 = i_17 - i_19 or (i_19 - (i_19 ushr 8))
                                i_19 = ints_6[i_8]
                                i_17 = i_16 + i_19
                                i_18 = (i_16 and 0xff00ff) + (i_19 and 0xff00ff)
                                i_19 = (i_18 and 0x1000100) + (i_17 - i_18 and 0x10000)
                                ints_6[i_8++] = i_17 - i_19 or (i_19 - (i_19 ushr 8))
                                i_15++
                            }

                            i_8 += i_12
                            i_9 += i_13
                            i_14++
                        }
                    } else {
                        if (i_3 != 2) {
                            throw IllegalArgumentException()
                        }

                        i_14 = i_41 ushr 24
                        i_15 = 256 - i_14
                        i_16 = i_15 * (i_41 and 0xff00ff) and -16711936
                        i_17 = i_15 * (i_41 and 0xff00) and 0xff0000
                        i_41 = (i_16 or i_17) ushr 8

                        i_18 = -i_10
                        while (i_18 < 0) {
                            i_19 = -i_11
                            while (i_19 < 0) {
                                i_20 = this.anIntArray10235[i_9++]
                                if (i_20 != 0) {
                                    i_16 = (i_20 and 0xff00ff) * i_14 and -16711936
                                    i_17 = i_14 * (i_20 and 0xff00) and 0xff0000
                                    i_20 = i_41 + ((i_16 or i_17) ushr 8)
                                    i_21 = ints_6[i_8]
                                    i_22 = i_20 + i_21
                                    i_23 = (i_20 and 0xff00ff) + (i_21 and 0xff00ff)
                                    i_21 = (i_23 and 0x1000100) + (i_22 - i_23 and 0x10000)
                                    ints_6[i_8++] = i_22 - i_21 or (i_21 - (i_21 ushr 8))
                                } else {
                                    ++i_8
                                }
                                i_19++
                            }

                            i_8 += i_12
                            i_9 += i_13
                            i_18++
                        }
                    }
                }
            }
        }
    }

    override fun drawWithSurface(i_1: Int, i_2: Int, class4553: ComponentSurface, i_4: Int, i_5: Int) {
        var i_29 = i_2
        var i_15 = i_1
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        val ints_6 = toolkit.colourBuffer ?: return
        i_15 += anInt8853
        i_29 += anInt8854
        var i_7 = 0
        val i_8 = toolkit.viewportWidth
        var i_9 = anInt8851
        var i_10 = anInt8867
        var i_11 = i_8 - i_9
        var i_12 = 0
        var i_13 = i_8 * i_29 + i_15
        var i_14: Int
        if (i_29 < toolkit.clipTop) {
            i_14 = toolkit.clipTop - i_29
            i_10 -= i_14
            i_29 = toolkit.clipTop
            i_7 += i_14 * i_9
            i_13 += i_14 * i_8
        }

        if (i_10 + i_29 > toolkit.clipBottom) {
            i_10 -= i_10 + i_29 - toolkit.clipBottom
        }

        if (i_15 < toolkit.clipLeft) {
            i_14 = toolkit.clipLeft - i_15
            i_9 -= i_14
            i_15 = toolkit.clipLeft
            i_7 += i_14
            i_13 += i_14
            i_12 += i_14
            i_11 += i_14
        }

        if (i_9 + i_15 > toolkit.clipRight) {
            i_14 = i_9 + i_15 - toolkit.clipRight
            i_9 -= i_14
            i_12 += i_14
            i_11 += i_14
        }

        if (i_9 > 0 && i_10 > 0) {
            val packetsdecoder29 = class4553 as ComponentDimensionSM
            val ints_15 = packetsdecoder29.anIntArray9077
            val ints_16 = packetsdecoder29.anIntArray9078
            var i_17 = i_29
            if (i_5 > i_29) {
                i_17 = i_5
                i_13 += i_8 * (i_5 - i_29)
                i_7 += (i_5 - i_29) * anInt8851
            }

            val i_18 = minOf(ints_15.size + i_5, i_10 + i_29)

            for (i_19 in i_17 until i_18) {
                var i_20 = ints_15[i_19 - i_5] + i_4
                var i_21 = ints_16[i_19 - i_5]
                var i_22 = i_9
                var i_23: Int
                if (i_15 > i_20) {
                    i_23 = i_15 - i_20
                    if (i_23 >= i_21) {
                        i_7 += i_9 + i_12
                        i_13 += i_9 + i_11
                        continue
                    }

                    i_21 -= i_23
                } else {
                    i_23 = i_20 - i_15
                    if (i_23 >= i_9) {
                        i_7 += i_9 + i_12
                        i_13 += i_9 + i_11
                        continue
                    }

                    i_7 += i_23
                    i_22 = i_9 - i_23
                    i_13 += i_23
                }

                i_23 = 0
                if (i_22 < i_21) {
                    i_21 = i_22
                } else {
                    i_23 = i_22 - i_21
                }

                for (i_24 in -i_21 until 0) {
                    val i_25 = this.anIntArray10235[i_7++]
                    val i_26 = i_25 ushr 24
                    val i_27 = 256 - i_26
                    val i_28 = ints_6[i_13]
                    ints_6[i_13++] = ((i_25 and 0xff00ff) * i_26 + (i_28 and 0xff00ff) * i_27 and -16711936) + (i_26 * (i_25 and 0xff00) + i_27 * (i_28 and 0xff00) and 0xff0000) shr 8
                }

                i_7 += i_23 + i_12
                i_13 += i_23 + i_11
            }
        }
    }

    override fun method14247(
        bool1: Boolean,
        bool2: Boolean,
        bool3: Boolean,
        i_4: Int,
        i_5: Int,
        f_6: Float,
        i_7: Int,
        i_8: Int,
        i_9: Int,
        i_10: Int,
        i_11: Int,
        bool13: Boolean
    ) {
        var i_71 = i_7
        var i_81 = i_8
        var i_101 = i_10
        var i_4Var = i_4
        var i_5Var = i_5
        if (i_71 > 0 && i_81 > 0 && (bool1 || bool2)) {
            var i_14 = 0
            var i_15 = 0
            val i_16 = anInt8853 + anInt8851 + anInt8855
            val i_17 = anInt8854 + anInt8867 + anInt8856
            val i_18 = (i_16 shl 16) / i_71
            val i_19 = (i_17 shl 16) / i_81
            var i_20: Int
            if (anInt8853 > 0) {
                i_20 = (i_18 + (anInt8853 shl 16) - 1) / i_18
                i_4Var += i_20
                i_14 += i_20 * i_18 - (anInt8853 shl 16)
            }

            if (anInt8854 > 0) {
                i_20 = (i_19 + (anInt8854 shl 16) - 1) / i_19
                i_5Var += i_20
                i_15 += i_20 * i_19 - (anInt8854 shl 16)
            }

            if (anInt8851 < i_16) {
                i_71 = ((anInt8851 shl 16) - i_14 + i_18 - 1) / i_18
            }

            if (anInt8867 < i_17) {
                i_81 = ((anInt8867 shl 16) - i_15 + i_19 - 1) / i_19
            }

            i_20 = i_5Var * toolkit.viewportWidth + i_4Var
            var i_21 = toolkit.viewportWidth - i_71
            if (i_5Var + i_81 > toolkit.clipBottom) {
                i_81 -= i_5Var + i_81 - toolkit.clipBottom
            }

            var i_22: Int
            if (i_5Var < toolkit.clipTop) {
                i_22 = toolkit.clipTop - i_5Var
                i_81 -= i_22
                i_20 += toolkit.viewportWidth * i_22
                i_15 += i_19 * i_22
            }

            if (i_4Var + i_71 > toolkit.clipRight) {
                i_22 = i_4Var + i_71 - toolkit.clipRight
                i_71 -= i_22
                i_21 += i_22
            }

            if (i_4Var < toolkit.clipLeft) {
                i_22 = toolkit.clipLeft - i_4Var
                i_71 -= i_22
                i_20 += i_22
                i_14 += i_18 * i_22
                i_21 += i_22
            }

            val floats_39 = toolkit.depthBuffer
            val ints_23 = toolkit.colourBuffer
            var i_24: Int
            var i_25: Int
            var i_26: Int
            var i_27: Int
            var i_28: Int
            var i_29: Int
            var i_30: Int
            var i_31: Int
            var i_32: Int
            var i_33: Int
            var i_34: Int
            if (i_11 == 0) {
                if (i_9 == 1) {
                    i_24 = i_14

                    i_25 = -i_81
                    while (i_25 < 0) {
                        i_26 = anInt8851 * (i_15 shr 16)

                        i_27 = -i_71
                        while (i_27 < 0) {
                            if (!bool2 || f_6 < floats_39!![i_20]) {
                                if (bool1) {
                                    ints_23!![i_20] = this.anIntArray10235[i_26 + (i_14 shr 16)]
                                }

                                if (bool2 && bool13) {
                                    floats_39!![i_20] = f_6
                                }
                            }

                            i_14 += i_18
                            ++i_20
                            i_27++
                        }

                        i_15 += i_19
                        i_14 = i_24
                        i_20 += i_21
                        i_25++
                    }
                } else if (i_9 == 0) {
                    i_24 = (i_101 and 0xff0000) shr 16
                    i_25 = (i_101 and 0xff00) shr 8
                    i_26 = i_101 and 0xff
                    i_27 = i_14

                    i_28 = -i_81
                    while (i_28 < 0) {
                        i_29 = anInt8851 * (i_15 shr 16)

                        i_30 = -i_71
                        while (i_30 < 0) {
                            if (!bool2 || f_6 < floats_39!![i_20]) {
                                if (bool1) {
                                    i_31 = this.anIntArray10235[i_29 + (i_14 shr 16)]
                                    i_32 = i_24 * (i_31 and 0xff0000) and 0xff000000.toInt()
                                    i_33 = i_25 * (i_31 and 0xff00) and 0xff0000
                                    i_34 = (i_31 and 0xff) * i_26 and 0xff00
                                    ints_23!![i_20] = (i_32 or i_33 or i_34) ushr 8
                                }

                                if (bool2 && bool13) {
                                    floats_39!![i_20] = f_6
                                }
                            }

                            i_14 += i_18
                            ++i_20
                            i_30++
                        }

                        i_15 += i_19
                        i_14 = i_27
                        i_20 += i_21
                        i_28++
                    }
                } else if (i_9 == 3) {
                    i_24 = i_14

                    i_25 = -i_81
                    while (i_25 < 0) {
                        i_26 = anInt8851 * (i_15 shr 16)

                        i_27 = -i_71
                        while (i_27 < 0) {
                            if (!bool2 || f_6 < floats_39!![i_20]) {
                                if (bool1) {
                                    i_28 = this.anIntArray10235[i_26 + (i_14 shr 16)]
                                    i_29 = i_101 + i_28
                                    i_30 = (i_28 and 0xff00ff) + (i_101 and 0xff00ff)
                                    i_31 = (i_30 and 0x1000100) + (i_29 - i_30 and 0x10000)
                                    ints_23!![i_20] = i_29 - i_31 or (i_31 - (i_31 ushr 8))
                                }

                                if (bool2 && bool13) {
                                    floats_39!![i_20] = f_6
                                }
                            }

                            i_14 += i_18
                            ++i_20
                            i_27++
                        }

                        i_15 += i_19
                        i_14 = i_24
                        i_20 += i_21
                        i_25++
                    }
                } else {
                    if (i_9 != 2) {
                        throw IllegalArgumentException()
                    }

                    i_24 = i_101 ushr 24
                    i_25 = 256 - i_24
                    i_26 = (i_101 and 0xff00ff) * i_25 and -16711936
                    i_27 = (i_101 and 0xff00) * i_25 and 0xff0000
                    i_101 = (i_26 or i_27) ushr 8
                    i_28 = i_14

                    i_29 = -i_81
                    while (i_29 < 0) {
                        i_30 = anInt8851 * (i_15 shr 16)

                        i_31 = -i_71
                        while (i_31 < 0) {
                            if (!bool2 || f_6 < floats_39!![i_20]) {
                                if (bool1) {
                                    i_32 = this.anIntArray10235[i_30 + (i_14 shr 16)]
                                    i_26 = (i_32 and 0xff00ff) * i_24 and -16711936
                                    i_27 = i_24 * (i_32 and 0xff00) and 0xff0000
                                    ints_23!![i_20] = i_101 + ((i_26 or i_27) ushr 8)
                                }

                                if (bool2 && bool13) {
                                    floats_39!![i_20] = f_6
                                }
                            }

                            i_14 += i_18
                            ++i_20
                            i_31++
                        }

                        i_15 += i_19
                        i_14 = i_28
                        i_20 += i_21
                        i_29++
                    }
                }
            } else {
                var i_35: Int
                var i_36: Int
                var i_37: Int
                if (i_11 == 1) {
                    if (i_9 == 1) {
                        i_24 = i_14

                        i_25 = -i_81
                        while (i_25 < 0) {
                            i_26 = anInt8851 * (i_15 shr 16)

                            i_27 = -i_71
                            while (i_27 < 0) {
                                if (!bool2 || f_6 < floats_39!![i_20]) {
                                    if (bool1) {
                                        i_28 = this.anIntArray10235[i_26 + (i_14 shr 16)]
                                        i_29 = i_28 ushr 24
                                        i_30 = 256 - i_29
                                        i_31 = ints_23!![i_20]
                                        ints_23[i_20] = (((i_28 and -16711936) ushr 8) * i_29 + ((i_31 and -16711936) ushr 8) * i_30 and -16711936) + (((i_28 and 0xff00ff) * i_29 + (i_31 and 0xff00ff) * i_30 and -16711936) shr 8)
                                    }

                                    if (bool2 && bool13) {
                                        floats_39!![i_20] = f_6
                                    }
                                }

                                i_14 += i_18
                                ++i_20
                                i_27++
                            }

                            i_15 += i_19
                            i_14 = i_24
                            i_20 += i_21
                            i_25++
                        }
                    } else if (i_9 == 0) {
                        i_24 = i_14
                        if ((i_101 and 0xffffff) == 16777215) {
                            i_25 = -i_81
                            while (i_25 < 0) {
                                i_26 = anInt8851 * (i_15 shr 16)

                                i_27 = -i_71
                                while (i_27 < 0) {
                                    if (!bool2 || f_6 < floats_39!![i_20]) {
                                        if (bool1) {
                                            i_28 = this.anIntArray10235[i_26 + (i_14 shr 16)]
                                            i_29 = (i_101 ushr 24) * (i_28 ushr 24) shr 8
                                            i_30 = 256 - i_29
                                            i_31 = ints_23!![i_20]
                                            ints_23[i_20] = ((i_28 and 0xff00ff) * i_29 + (i_31 and 0xff00ff) * i_30 and -16711936) + (i_29 * (i_28 and 0xff00) + i_30 * (i_31 and 0xff00) and 0xff0000) shr 8
                                        }

                                        if (bool2 && bool13) {
                                            floats_39!![i_20] = f_6
                                        }
                                    }

                                    i_14 += i_18
                                    ++i_20
                                    i_27++
                                }

                                i_15 += i_19
                                i_14 = i_24
                                i_20 += i_21
                                i_25++
                            }
                        } else {
                            i_25 = (i_101 and 0xff0000) shr 16
                            i_26 = (i_101 and 0xff00) shr 8
                            i_27 = i_101 and 0xff

                            i_28 = -i_81
                            while (i_28 < 0) {
                                i_29 = anInt8851 * (i_15 shr 16)

                                i_30 = -i_71
                                while (i_30 < 0) {
                                    if (!bool2 || f_6 < floats_39!![i_20]) {
                                        i_31 = this.anIntArray10235[i_29 + (i_14 shr 16)]
                                        i_32 = (i_101 ushr 24) * (i_31 ushr 24) shr 8
                                        i_33 = 256 - i_32
                                        if (i_32 != 255) {
                                            if (bool1) {
                                                i_34 = i_25 * (i_31 and 0xff0000) and 0xff000000.toInt()
                                                i_35 = i_26 * (i_31 and 0xff00) and 0xff0000
                                                i_36 = (i_31 and 0xff) * i_27 and 0xff00
                                                i_31 = (i_34 or i_35 or i_36) ushr 8
                                                i_37 = ints_23!![i_20]
                                                ints_23[i_20] = ((i_31 and 0xff00ff) * i_32 + (i_37 and 0xff00ff) * i_33 and -16711936) + (i_32 * (i_31 and 0xff00) + i_33 * (i_37 and 0xff00) and 0xff0000) shr 8
                                                if (bool3) {
                                                    var i_38 = i_32 + (i_37 ushr 24)
                                                    if (i_38 > 255) {
                                                        i_38 = 255
                                                    }

                                                    ints_23[i_20] = ints_23[i_20] or (i_38 shl 24)
                                                }
                                            }

                                        } else {
                                            if (bool1) {
                                                i_34 = i_25 * (i_31 and 0xff0000) and 0xff000000.toInt()
                                                i_35 = i_26 * (i_31 and 0xff00) and 0xff0000
                                                i_36 = (i_31 and 0xff) * i_27 and 0xff00
                                                ints_23!![i_20] = (i_34 or i_35 or i_36) ushr 8
                                            }

                                        }
                                        if (bool2 && bool13) {
                                            floats_39!![i_20] = f_6
                                        }
                                    }

                                    i_14 += i_18
                                    ++i_20
                                    i_30++
                                }

                                i_15 += i_19
                                i_14 = i_24
                                i_20 += i_21
                                i_28++
                            }
                        }
                    } else if (i_9 == 3) {
                        i_24 = i_14

                        i_25 = -i_81
                        while (i_25 < 0) {
                            i_26 = anInt8851 * (i_15 shr 16)

                            i_27 = -i_71
                            while (i_27 < 0) {
                                if (!bool2 || f_6 < floats_39!![i_20]) {
                                    if (bool1) {
                                        i_28 = this.anIntArray10235[i_26 + (i_14 shr 16)]
                                        i_29 = i_101 + i_28
                                        i_30 = (i_28 and 0xff00ff) + (i_101 and 0xff00ff)
                                        i_31 = (i_30 and 0x1000100) + (i_29 - i_30 and 0x10000)
                                        i_31 = i_29 - i_31 or (i_31 - (i_31 ushr 8))
                                        i_32 = (i_101 ushr 24) * (i_31 ushr 24) shr 8
                                        i_33 = 256 - i_32
                                        if (i_32 != 255) {
                                            i_28 = i_31
                                            i_31 = ints_23!![i_20]
                                            i_31 = ((i_28 and 0xff00ff) * i_32 + (i_31 and 0xff00ff) * i_33 and -16711936) + (i_32 * (i_28 and 0xff00) + i_33 * (i_31 and 0xff00) and 0xff0000) shr 8
                                        }

                                        ints_23!![i_20] = i_31
                                    }

                                    if (bool2 && bool13) {
                                        floats_39!![i_20] = f_6
                                    }
                                }

                                i_14 += i_18
                                ++i_20
                                i_27++
                            }

                            i_15 += i_19
                            i_14 = i_24
                            i_20 += i_21
                            i_25++
                        }
                    } else {
                        if (i_9 != 2) {
                            throw IllegalArgumentException()
                        }

                        i_24 = i_101 ushr 24
                        i_25 = 256 - i_24
                        i_26 = (i_101 and 0xff00ff) * i_25 and -16711936
                        i_27 = (i_101 and 0xff00) * i_25 and 0xff0000
                        i_101 = (i_26 or i_27) ushr 8
                        i_28 = i_14

                        i_29 = -i_81
                        while (i_29 < 0) {
                            i_30 = anInt8851 * (i_15 shr 16)

                            i_31 = -i_71
                            while (i_31 < 0) {
                                if (!bool2 || f_6 < floats_39!![i_20]) {
                                    if (bool1) {
                                        i_32 = this.anIntArray10235[i_30 + (i_14 shr 16)]
                                        i_33 = i_32 ushr 24
                                        i_34 = 256 - i_33
                                        i_26 = (i_32 and 0xff00ff) * i_24 and -16711936
                                        i_27 = i_24 * (i_32 and 0xff00) and 0xff0000
                                        i_32 = i_101 + ((i_26 or i_27) ushr 8)
                                        i_35 = ints_23!![i_20]
                                        ints_23[i_20] = ((i_32 and 0xff00ff) * i_33 + (i_35 and 0xff00ff) * i_34 and -16711936) + (i_33 * (i_32 and 0xff00) + i_34 * (i_35 and 0xff00) and 0xff0000) shr 8
                                    }

                                    if (bool2 && bool13) {
                                        floats_39!![i_20] = f_6
                                    }
                                }

                                i_14 += i_18
                                ++i_20
                                i_31++
                            }

                            i_15 += i_19
                            i_14 = i_28
                            i_20 += i_21
                            i_29++
                        }
                    }
                } else {
                    if (i_11 != 2) {
                        throw IllegalArgumentException()
                    }

                    if (i_9 == 1) {
                        i_24 = i_14

                        i_25 = -i_81
                        while (i_25 < 0) {
                            i_26 = anInt8851 * (i_15 shr 16)

                            i_27 = -i_71
                            while (i_27 < 0) {
                                if (!bool2 || f_6 < floats_39!![i_20]) {
                                    i_28 = this.anIntArray10235[i_26 + (i_14 shr 16)]
                                    if (i_28 != 0) {
                                        if (bool1) {
                                            i_29 = ints_23!![i_20]
                                            i_30 = i_28 + i_29
                                            i_31 = (i_28 and 0xff00ff) + (i_29 and 0xff00ff)
                                            i_29 = (i_31 and 0x1000100) + (i_30 - i_31 and 0x10000)
                                            ints_23[i_20] = i_30 - i_29 or (i_29 - (i_29 ushr 8))
                                        }

                                        if (bool2 && bool13) {
                                            floats_39!![i_20] = f_6
                                        }
                                    }
                                }

                                i_14 += i_18
                                ++i_20
                                i_27++
                            }

                            i_15 += i_19
                            i_14 = i_24
                            i_20 += i_21
                            i_25++
                        }
                    } else if (i_9 == 0) {
                        i_24 = i_14
                        i_25 = (i_101 and 0xff0000) shr 16
                        i_26 = (i_101 and 0xff00) shr 8
                        i_27 = i_101 and 0xff

                        i_28 = -i_81
                        while (i_28 < 0) {
                            i_29 = anInt8851 * (i_15 shr 16)

                            i_30 = -i_71
                            while (i_30 < 0) {
                                if (!bool2 || f_6 < floats_39!![i_20]) {
                                    i_31 = this.anIntArray10235[i_29 + (i_14 shr 16)]
                                    if (i_31 != 0) {
                                        if (bool1) {
                                            i_32 = i_25 * (i_31 and 0xff0000) and 0xff000000.toInt()
                                            i_33 = i_26 * (i_31 and 0xff00) and 0xff0000
                                            i_34 = (i_31 and 0xff) * i_27 and 0xff00
                                            i_31 = (i_32 or i_33 or i_34) ushr 8
                                            i_35 = ints_23!![i_20]
                                            i_36 = i_31 + i_35
                                            i_37 = (i_31 and 0xff00ff) + (i_35 and 0xff00ff)
                                            i_35 = (i_37 and 0x1000100) + (i_36 - i_37 and 0x10000)
                                            ints_23[i_20] = i_36 - i_35 or (i_35 - (i_35 ushr 8))
                                        }

                                        if (bool2 && bool13) {
                                            floats_39!![i_20] = f_6
                                        }
                                    }
                                }

                                i_14 += i_18
                                ++i_20
                                i_30++
                            }

                            i_15 += i_19
                            i_14 = i_24
                            i_20 += i_21
                            i_28++
                        }
                    } else if (i_9 == 3) {
                        i_24 = i_14

                        i_25 = -i_81
                        while (i_25 < 0) {
                            i_26 = anInt8851 * (i_15 shr 16)

                            i_27 = -i_71
                            while (i_27 < 0) {
                                if (!bool2 || f_6 < floats_39!![i_20]) {
                                    if (bool1) {
                                        i_28 = this.anIntArray10235[i_26 + (i_14 shr 16)]
                                        i_29 = i_101 + i_28
                                        i_30 = (i_28 and 0xff00ff) + (i_101 and 0xff00ff)
                                        i_31 = (i_30 and 0x1000100) + (i_29 - i_30 and 0x10000)
                                        i_28 = i_29 - i_31 or (i_31 - (i_31 ushr 8))
                                        i_31 = ints_23!![i_20]
                                        i_29 = i_28 + i_31
                                        i_30 = (i_28 and 0xff00ff) + (i_31 and 0xff00ff)
                                        i_31 = (i_30 and 0x1000100) + (i_29 - i_30 and 0x10000)
                                        ints_23[i_20] = i_29 - i_31 or (i_31 - (i_31 ushr 8))
                                    }

                                    if (bool2 && bool13) {
                                        floats_39!![i_20] = f_6
                                    }
                                }

                                i_14 += i_18
                                ++i_20
                                i_27++
                            }

                            i_15 += i_19
                            i_14 = i_24
                            i_20 += i_21
                            i_25++
                        }
                    } else {
                        if (i_9 != 2) {
                            throw IllegalArgumentException()
                        }

                        i_24 = i_101 ushr 24
                        i_25 = 256 - i_24
                        i_26 = (i_101 and 0xff00ff) * i_25 and -16711936
                        i_27 = (i_101 and 0xff00) * i_25 and 0xff0000
                        i_101 = (i_26 or i_27) ushr 8
                        i_28 = i_14

                        i_29 = -i_81
                        while (i_29 < 0) {
                            i_30 = anInt8851 * (i_15 shr 16)

                            i_31 = -i_71
                            while (i_31 < 0) {
                                if (!bool2 || f_6 < floats_39!![i_20]) {
                                    i_32 = this.anIntArray10235[i_30 + (i_14 shr 16)]
                                    if (i_32 != 0) {
                                        if (bool1) {
                                            i_26 = (i_32 and 0xff00ff) * i_24 and -16711936
                                            i_27 = i_24 * (i_32 and 0xff00) and 0xff0000
                                            i_32 = i_101 + ((i_26 or i_27) ushr 8)
                                            i_33 = ints_23!![i_20]
                                            i_34 = i_32 + i_33
                                            i_35 = (i_32 and 0xff00ff) + (i_33 and 0xff00ff)
                                            i_33 = (i_35 and 0x1000100) + (i_34 - i_35 and 0x10000)
                                            ints_23[i_20] = i_34 - i_33 or (i_33 - (i_33 ushr 8))
                                        }

                                        if (bool2 && bool13) {
                                            floats_39!![i_20] = f_6
                                        }
                                    }
                                }

                                i_14 += i_18
                                ++i_20
                                i_31++
                            }

                            i_15 += i_19
                            i_14 = i_28
                            i_20 += i_21
                            i_29++
                        }
                    }
                }
            }
        }
    }

    override fun captureScreenRegion(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        val ints_7 = toolkit.colourBuffer
            ?: throw IllegalStateException("")
        for (i_8 in 0 until i_4) {
            val i_9 = i_3 * (i_8 + i_2) + i_1
            val i_10 = i_5 + i_3 * (i_8 + i_6)

            for (i_11 in 0 until i_3) {
                this.anIntArray10235[i_9 + i_11] = ints_7[i_10 + i_11] and 0xffffff
            }
        }
    }

    override fun pixelTransfer() {
        val ints_4 = toolkit.colourBuffer
            ?: throw IllegalStateException("")
        for (i_5 in 0 until anInt8867) {
            val i_6 = i_5 * anInt8851
            val i_7 = toolkit.viewportWidth * i_5

            for (i_8 in 0 until anInt8851) {
                this.anIntArray10235[i_6 + i_8] = this.anIntArray10235[i_6 + i_8] and 0xffffff or (if (ints_4[i_7 + i_8] != 0) 0xff000000.toInt() else 0)
            }
        }
    }

    override fun method14245(i_1: Int, i_2: Int) {
        val ints_3 = toolkit.colourBuffer ?: return
        var i_4: Int
        var i_5: Int
        var i_6: Int
        var i_7: Int
        var i_8: Int
        var i_9: Int
        var i_10: Int
        var i_11: Int
        var i_12: Int
        var i_13: Int
        var i_14: Int
        var i_15: Int
        var i_16: Int
        var i_17: Int
        var i_18: Int
        if (anInt8865 == 0) {
            if (anInt8868 == 0) {
                i_4 = anInt8864
                while (i_4 < 0) {
                    i_5 = anInt8870
                    i_6 = anInt8873
                    i_7 = anInt8874
                    i_8 = anInt8862
                    if (i_6 >= 0 && i_7 >= 0 && i_6 - (anInt8851 shl 12) < 0 && i_7 - (anInt8867 shl 12) < 0) {
                        while (i_8 < 0) {
                            i_9 = (i_6 shr 12) + (i_7 shr 12) * anInt8851
                            i_10 = i_5++
                            if (i_2 == 0) {
                                if (i_1 == 1) {
                                    ints_3[i_10] = this.anIntArray10235[i_9]
                                } else if (i_1 == 0) {
                                    i_11 = this.anIntArray10235[i_9++]
                                    i_12 = anInt8880 * (i_11 and 0xff0000) and 0xff000000.toInt()
                                    i_13 = (i_11 and 0xff00) * anInt8852 and 0xff0000
                                    i_14 = (i_11 and 0xff) * anInt8882 and 0xff00
                                    ints_3[i_10] = (i_12 or i_13 or i_14) ushr 8
                                } else if (i_1 == 3) {
                                    i_11 = this.anIntArray10235[i_9++]
                                    i_12 = anInt8877
                                    i_13 = i_11 + i_12
                                    i_14 = (i_11 and 0xff00ff) + (i_12 and 0xff00ff)
                                    i_15 = (i_14 and 0x1000100) + (i_13 - i_14 and 0x10000)
                                    ints_3[i_10] = i_13 - i_15 or (i_15 - (i_15 ushr 8))
                                } else {
                                    if (i_1 != 2) {
                                        throw IllegalArgumentException()
                                    }
                                    i_11 = this.anIntArray10235[i_9]
                                    i_12 = (i_11 and 0xff00ff) * anInt8878 and -16711936
                                    i_13 = (i_11 and 0xff00) * anInt8878 and 0xff0000
                                    ints_3[i_10] = ((i_12 or i_13) ushr 8) + anInt8885
                                }
                            } else if (i_2 == 1) {
                                if (i_1 == 1) {
                                    i_11 = this.anIntArray10235[i_9]
                                    i_12 = i_11 ushr 24
                                    i_13 = 256 - i_12
                                    i_14 = ints_3[i_10]
                                    ints_3[i_10] = ((i_11 and 0xff00ff) * i_12 + (i_14 and 0xff00ff) * i_13 and -16711936) + (i_12 * (i_11 and 0xff00) + i_13 * (i_14 and 0xff00) and 0xff0000) shr 8
                                } else if (i_1 == 0) {
                                    i_11 = this.anIntArray10235[i_9]
                                    i_12 = (i_11 ushr 24) * anInt8878 shr 8
                                    i_13 = 256 - i_12
                                    if ((anInt8877 and 0xffffff) == 16777215) {
                                        i_14 = ints_3[i_10]
                                        ints_3[i_10] = ((i_11 and 0xff00ff) * i_12 + (i_14 and 0xff00ff) * i_13 and -16711936) + (i_12 * (i_11 and 0xff00) + i_13 * (i_14 and 0xff00) and 0xff0000) shr 8
                                    } else if (i_12 != 255) {
                                        i_14 = anInt8880 * (i_11 and 0xff0000) and 0xff000000.toInt()
                                        i_15 = (i_11 and 0xff00) * anInt8852 and 0xff0000
                                        i_16 = (i_11 and 0xff) * anInt8882 and 0xff00
                                        i_11 = (i_14 or i_15 or i_16) ushr 8
                                        i_17 = ints_3[i_10]
                                        ints_3[i_10] = ((i_11 and 0xff00ff) * i_12 + (i_17 and 0xff00ff) * i_13 and -16711936) + (i_12 * (i_11 and 0xff00) + i_13 * (i_17 and 0xff00) and 0xff0000) shr 8
                                    } else {
                                        i_14 = anInt8880 * (i_11 and 0xff0000) and 0xff000000.toInt()
                                        i_15 = (i_11 and 0xff00) * anInt8852 and 0xff0000
                                        i_16 = (i_11 and 0xff) * anInt8882 and 0xff00
                                        ints_3[i_10] = (i_14 or i_15 or i_16) ushr 8
                                    }
                                } else if (i_1 == 3) {
                                    i_11 = this.anIntArray10235[i_9]
                                    i_12 = anInt8877
                                    i_13 = i_11 + i_12
                                    i_14 = (i_11 and 0xff00ff) + (i_12 and 0xff00ff)
                                    i_15 = (i_14 and 0x1000100) + (i_13 - i_14 and 0x10000)
                                    i_15 = i_13 - i_15 or (i_15 - (i_15 ushr 8))
                                    i_16 = (i_11 ushr 24) * anInt8878 shr 8
                                    i_17 = 256 - i_16
                                    if (i_16 != 255) {
                                        i_11 = i_15
                                        i_15 = ints_3[i_10]
                                        i_15 = ((i_11 and 0xff00ff) * i_16 + (i_15 and 0xff00ff) * i_17 and -16711936) + (i_16 * (i_11 and 0xff00) + i_17 * (i_15 and 0xff00) and 0xff0000) shr 8
                                    }
                                    ints_3[i_10] = i_15
                                } else {
                                    if (i_1 != 2) {
                                        throw IllegalArgumentException()
                                    }
                                    i_11 = this.anIntArray10235[i_9]
                                    i_12 = i_11 ushr 24
                                    i_13 = 256 - i_12
                                    i_14 = (i_11 and 0xff00ff) * anInt8878 and -16711936
                                    i_15 = (i_11 and 0xff00) * anInt8878 and 0xff0000
                                    i_11 = ((i_14 or i_15) ushr 8) + anInt8885
                                    i_16 = ints_3[i_10]
                                    ints_3[i_10] = ((i_11 and 0xff00ff) * i_12 + (i_16 and 0xff00ff) * i_13 and -16711936) + (i_12 * (i_11 and 0xff00) + i_13 * (i_16 and 0xff00) and 0xff0000) shr 8
                                }
                            } else {
                                if (i_2 != 2) {
                                    throw IllegalArgumentException()
                                }
                                if (i_1 == 1) {
                                    i_11 = this.anIntArray10235[i_9]
                                    i_12 = ints_3[i_10]
                                    i_13 = i_11 + i_12
                                    i_14 = (i_11 and 0xff00ff) + (i_12 and 0xff00ff)
                                    i_12 = (i_14 and 0x1000100) + (i_13 - i_14 and 0x10000)
                                    ints_3[i_10] = i_13 - i_12 or (i_12 - (i_12 ushr 8))
                                } else if (i_1 == 0) {
                                    i_11 = this.anIntArray10235[i_9]
                                    i_12 = anInt8880 * (i_11 and 0xff0000) and 0xff000000.toInt()
                                    i_13 = (i_11 and 0xff00) * anInt8852 and 0xff0000
                                    i_14 = (i_11 and 0xff) * anInt8882 and 0xff00
                                    i_11 = (i_12 or i_13 or i_14) ushr 8
                                    i_15 = ints_3[i_10]
                                    i_16 = i_11 + i_15
                                    i_17 = (i_11 and 0xff00ff) + (i_15 and 0xff00ff)
                                    i_15 = (i_17 and 0x1000100) + (i_16 - i_17 and 0x10000)
                                    ints_3[i_10] = i_16 - i_15 or (i_15 - (i_15 ushr 8))
                                } else if (i_1 == 3) {
                                    i_11 = this.anIntArray10235[i_9]
                                    i_12 = anInt8877
                                    i_13 = i_11 + i_12
                                    i_14 = (i_11 and 0xff00ff) + (i_12 and 0xff00ff)
                                    i_15 = (i_14 and 0x1000100) + (i_13 - i_14 and 0x10000)
                                    i_11 = i_13 - i_15 or (i_15 - (i_15 ushr 8))
                                    i_15 = ints_3[i_10]
                                    i_13 = i_11 + i_15
                                    i_14 = (i_11 and 0xff00ff) + (i_15 and 0xff00ff)
                                    i_15 = (i_14 and 0x1000100) + (i_13 - i_14 and 0x10000)
                                    ints_3[i_10] = i_13 - i_15 or (i_15 - (i_15 ushr 8))
                                } else if (i_1 == 2) {
                                    i_11 = this.anIntArray10235[i_9]
                                    i_12 = (i_11 and 0xff00ff) * anInt8878 and -16711936
                                    i_13 = (i_11 and 0xff00) * anInt8878 and 0xff0000
                                    i_11 = ((i_12 or i_13) ushr 8) + anInt8885
                                    i_14 = ints_3[i_10]
                                    i_15 = i_11 + i_14
                                    i_16 = (i_11 and 0xff00ff) + (i_14 and 0xff00ff)
                                    i_14 = (i_16 and 0x1000100) + (i_15 - i_16 and 0x10000)
                                    ints_3[i_10] = i_15 - i_14 or (i_14 - (i_14 ushr 8))
                                }
                            }
                            i_8++
                        }
                    }
                    ++i_4
                    anInt8870 += anInt8850
                }
            } else if (anInt8868 < 0) {
                i_4 = anInt8864
                while (i_4 < 0) {
                    i_5 = anInt8870
                    i_6 = anInt8873
                    i_7 = anInt8874 + anInt8866
                    i_8 = anInt8862
                    if (i_6 >= 0 && i_6 - (anInt8851 shl 12) < 0) {
                        i_9 = i_7 - (anInt8867 shl 12)
                        if (i_9 >= 0) {
                            i_9 = (anInt8868 - i_9) / anInt8868
                            i_8 += i_9
                            i_7 += i_9 * anInt8868
                            i_5 += i_9
                        }
                        i_9 = (i_7 - anInt8868) / anInt8868
                        if (i_9 > i_8) {
                            i_8 = i_9
                        }
                        while (i_8 < 0) {
                            i_10 = (i_6 shr 12) + (i_7 shr 12) * anInt8851
                            i_11 = i_5++
                            if (i_2 == 0) {
                                if (i_1 == 1) {
                                    ints_3[i_11] = this.anIntArray10235[i_10]
                                } else if (i_1 == 0) {
                                    i_12 = this.anIntArray10235[i_10++]
                                    i_13 = anInt8880 * (i_12 and 0xff0000) and 0xff000000.toInt()
                                    i_14 = (i_12 and 0xff00) * anInt8852 and 0xff0000
                                    i_15 = (i_12 and 0xff) * anInt8882 and 0xff00
                                    ints_3[i_11] = (i_13 or i_14 or i_15) ushr 8
                                } else if (i_1 == 3) {
                                    i_12 = this.anIntArray10235[i_10++]
                                    i_13 = anInt8877
                                    i_14 = i_12 + i_13
                                    i_15 = (i_12 and 0xff00ff) + (i_13 and 0xff00ff)
                                    i_16 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                                    ints_3[i_11] = i_14 - i_16 or (i_16 - (i_16 ushr 8))
                                } else {
                                    if (i_1 != 2) {
                                        throw IllegalArgumentException()
                                    }
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = (i_12 and 0xff00ff) * anInt8878 and -16711936
                                    i_14 = (i_12 and 0xff00) * anInt8878 and 0xff0000
                                    ints_3[i_11] = ((i_13 or i_14) ushr 8) + anInt8885
                                }
                            } else if (i_2 == 1) {
                                if (i_1 == 1) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = i_12 ushr 24
                                    i_14 = 256 - i_13
                                    i_15 = ints_3[i_11]
                                    ints_3[i_11] = ((i_12 and 0xff00ff) * i_13 + (i_15 and 0xff00ff) * i_14 and -16711936) + (i_13 * (i_12 and 0xff00) + i_14 * (i_15 and 0xff00) and 0xff0000) shr 8
                                } else if (i_1 == 0) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = (i_12 ushr 24) * anInt8878 shr 8
                                    i_14 = 256 - i_13
                                    if ((anInt8877 and 0xffffff) == 16777215) {
                                        i_15 = ints_3[i_11]
                                        ints_3[i_11] = ((i_12 and 0xff00ff) * i_13 + (i_15 and 0xff00ff) * i_14 and -16711936) + (i_13 * (i_12 and 0xff00) + i_14 * (i_15 and 0xff00) and 0xff0000) shr 8
                                    } else if (i_13 != 255) {
                                        i_15 = anInt8880 * (i_12 and 0xff0000) and 0xff000000.toInt()
                                        i_16 = (i_12 and 0xff00) * anInt8852 and 0xff0000
                                        i_17 = (i_12 and 0xff) * anInt8882 and 0xff00
                                        i_12 = (i_15 or i_16 or i_17) ushr 8
                                        i_18 = ints_3[i_11]
                                        ints_3[i_11] = ((i_12 and 0xff00ff) * i_13 + (i_18 and 0xff00ff) * i_14 and -16711936) + (i_13 * (i_12 and 0xff00) + i_14 * (i_18 and 0xff00) and 0xff0000) shr 8
                                    } else {
                                        i_15 = anInt8880 * (i_12 and 0xff0000) and 0xff000000.toInt()
                                        i_16 = (i_12 and 0xff00) * anInt8852 and 0xff0000
                                        i_17 = (i_12 and 0xff) * anInt8882 and 0xff00
                                        ints_3[i_11] = (i_15 or i_16 or i_17) ushr 8
                                    }
                                } else if (i_1 == 3) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = anInt8877
                                    i_14 = i_12 + i_13
                                    i_15 = (i_12 and 0xff00ff) + (i_13 and 0xff00ff)
                                    i_16 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                                    i_16 = i_14 - i_16 or (i_16 - (i_16 ushr 8))
                                    i_17 = (i_12 ushr 24) * anInt8878 shr 8
                                    i_18 = 256 - i_17
                                    if (i_17 != 255) {
                                        i_12 = i_16
                                        i_16 = ints_3[i_11]
                                        i_16 = ((i_12 and 0xff00ff) * i_17 + (i_16 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_12 and 0xff00) + i_18 * (i_16 and 0xff00) and 0xff0000) shr 8
                                    }
                                    ints_3[i_11] = i_16
                                } else {
                                    if (i_1 != 2) {
                                        throw IllegalArgumentException()
                                    }
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = i_12 ushr 24
                                    i_14 = 256 - i_13
                                    i_15 = (i_12 and 0xff00ff) * anInt8878 and -16711936
                                    i_16 = (i_12 and 0xff00) * anInt8878 and 0xff0000
                                    i_12 = ((i_15 or i_16) ushr 8) + anInt8885
                                    i_17 = ints_3[i_11]
                                    ints_3[i_11] = ((i_12 and 0xff00ff) * i_13 + (i_17 and 0xff00ff) * i_14 and -16711936) + (i_13 * (i_12 and 0xff00) + i_14 * (i_17 and 0xff00) and 0xff0000) shr 8
                                }
                            } else {
                                if (i_2 != 2) {
                                    throw IllegalArgumentException()
                                }
                                if (i_1 == 1) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = ints_3[i_11]
                                    i_14 = i_12 + i_13
                                    i_15 = (i_12 and 0xff00ff) + (i_13 and 0xff00ff)
                                    i_13 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                                    ints_3[i_11] = i_14 - i_13 or (i_13 - (i_13 ushr 8))
                                } else if (i_1 == 0) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = anInt8880 * (i_12 and 0xff0000) and 0xff000000.toInt()
                                    i_14 = (i_12 and 0xff00) * anInt8852 and 0xff0000
                                    i_15 = (i_12 and 0xff) * anInt8882 and 0xff00
                                    i_12 = (i_13 or i_14 or i_15) ushr 8
                                    i_16 = ints_3[i_11]
                                    i_17 = i_12 + i_16
                                    i_18 = (i_12 and 0xff00ff) + (i_16 and 0xff00ff)
                                    i_16 = (i_18 and 0x1000100) + (i_17 - i_18 and 0x10000)
                                    ints_3[i_11] = i_17 - i_16 or (i_16 - (i_16 ushr 8))
                                } else if (i_1 == 3) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = anInt8877
                                    i_14 = i_12 + i_13
                                    i_15 = (i_12 and 0xff00ff) + (i_13 and 0xff00ff)
                                    i_16 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                                    i_12 = i_14 - i_16 or (i_16 - (i_16 ushr 8))
                                    i_16 = ints_3[i_11]
                                    i_14 = i_12 + i_16
                                    i_15 = (i_12 and 0xff00ff) + (i_16 and 0xff00ff)
                                    i_16 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                                    ints_3[i_11] = i_14 - i_16 or (i_16 - (i_16 ushr 8))
                                } else if (i_1 == 2) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = (i_12 and 0xff00ff) * anInt8878 and -16711936
                                    i_14 = (i_12 and 0xff00) * anInt8878 and 0xff0000
                                    i_12 = ((i_13 or i_14) ushr 8) + anInt8885
                                    i_15 = ints_3[i_11]
                                    i_16 = i_12 + i_15
                                    i_17 = (i_12 and 0xff00ff) + (i_15 and 0xff00ff)
                                    i_15 = (i_17 and 0x1000100) + (i_16 - i_17 and 0x10000)
                                    ints_3[i_11] = i_16 - i_15 or (i_15 - (i_15 ushr 8))
                                }
                            }
                            i_7 += anInt8868
                            ++i_8
                        }
                    }
                    ++i_4
                    anInt8873 += anInt8869
                    anInt8870 += anInt8850
                }
            } else {
                i_4 = anInt8864
                while (i_4 < 0) {
                    i_5 = anInt8870
                    i_6 = anInt8873
                    i_7 = anInt8874 + anInt8866
                    i_8 = anInt8862
                    if (i_6 >= 0 && i_6 - (anInt8851 shl 12) < 0) {
                        if (i_7 < 0) {
                            i_9 = (anInt8868 - 1 - i_7) / anInt8868
                            i_8 += i_9
                            i_7 += i_9 * anInt8868
                            i_5 += i_9
                        }
                        i_9 = (i_7 + 1 - (anInt8867 shl 12) - anInt8868) / anInt8868
                        if (i_9 > i_8) {
                            i_8 = i_9
                        }
                        while (i_8 < 0) {
                            i_10 = (i_6 shr 12) + (i_7 shr 12) * anInt8851
                            i_11 = i_5++
                            if (i_2 == 0) {
                                if (i_1 == 1) {
                                    ints_3[i_11] = this.anIntArray10235[i_10]
                                } else if (i_1 == 0) {
                                    i_12 = this.anIntArray10235[i_10++]
                                    i_13 = anInt8880 * (i_12 and 0xff0000) and 0xff000000.toInt()
                                    i_14 = (i_12 and 0xff00) * anInt8852 and 0xff0000
                                    i_15 = (i_12 and 0xff) * anInt8882 and 0xff00
                                    ints_3[i_11] = (i_13 or i_14 or i_15) ushr 8
                                } else if (i_1 == 3) {
                                    i_12 = this.anIntArray10235[i_10++]
                                    i_13 = anInt8877
                                    i_14 = i_12 + i_13
                                    i_15 = (i_12 and 0xff00ff) + (i_13 and 0xff00ff)
                                    i_16 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                                    ints_3[i_11] = i_14 - i_16 or (i_16 - (i_16 ushr 8))
                                } else {
                                    if (i_1 != 2) {
                                        throw IllegalArgumentException()
                                    }
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = (i_12 and 0xff00ff) * anInt8878 and -16711936
                                    i_14 = (i_12 and 0xff00) * anInt8878 and 0xff0000
                                    ints_3[i_11] = ((i_13 or i_14) ushr 8) + anInt8885
                                }
                            } else if (i_2 == 1) {
                                if (i_1 == 1) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = i_12 ushr 24
                                    i_14 = 256 - i_13
                                    i_15 = ints_3[i_11]
                                    ints_3[i_11] = ((i_12 and 0xff00ff) * i_13 + (i_15 and 0xff00ff) * i_14 and -16711936) + (i_13 * (i_12 and 0xff00) + i_14 * (i_15 and 0xff00) and 0xff0000) shr 8
                                } else if (i_1 == 0) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = (i_12 ushr 24) * anInt8878 shr 8
                                    i_14 = 256 - i_13
                                    if ((anInt8877 and 0xffffff) == 16777215) {
                                        i_15 = ints_3[i_11]
                                        ints_3[i_11] = ((i_12 and 0xff00ff) * i_13 + (i_15 and 0xff00ff) * i_14 and -16711936) + (i_13 * (i_12 and 0xff00) + i_14 * (i_15 and 0xff00) and 0xff0000) shr 8
                                    } else if (i_13 != 255) {
                                        i_15 = anInt8880 * (i_12 and 0xff0000) and 0xff000000.toInt()
                                        i_16 = (i_12 and 0xff00) * anInt8852 and 0xff0000
                                        i_17 = (i_12 and 0xff) * anInt8882 and 0xff00
                                        i_12 = (i_15 or i_16 or i_17) ushr 8
                                        i_18 = ints_3[i_11]
                                        ints_3[i_11] = ((i_12 and 0xff00ff) * i_13 + (i_18 and 0xff00ff) * i_14 and -16711936) + (i_13 * (i_12 and 0xff00) + i_14 * (i_18 and 0xff00) and 0xff0000) shr 8
                                    } else {
                                        i_15 = anInt8880 * (i_12 and 0xff0000) and 0xff000000.toInt()
                                        i_16 = (i_12 and 0xff00) * anInt8852 and 0xff0000
                                        i_17 = (i_12 and 0xff) * anInt8882 and 0xff00
                                        ints_3[i_11] = (i_15 or i_16 or i_17) ushr 8
                                    }
                                } else if (i_1 == 3) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = anInt8877
                                    i_14 = i_12 + i_13
                                    i_15 = (i_12 and 0xff00ff) + (i_13 and 0xff00ff)
                                    i_16 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                                    i_16 = i_14 - i_16 or (i_16 - (i_16 ushr 8))
                                    i_17 = (i_12 ushr 24) * anInt8878 shr 8
                                    i_18 = 256 - i_17
                                    if (i_17 != 255) {
                                        i_12 = i_16
                                        i_16 = ints_3[i_11]
                                        i_16 = ((i_12 and 0xff00ff) * i_17 + (i_16 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_12 and 0xff00) + i_18 * (i_16 and 0xff00) and 0xff0000) shr 8
                                    }
                                    ints_3[i_11] = i_16
                                } else {
                                    if (i_1 != 2) {
                                        throw IllegalArgumentException()
                                    }
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = i_12 ushr 24
                                    i_14 = 256 - i_13
                                    i_15 = (i_12 and 0xff00ff) * anInt8878 and -16711936
                                    i_16 = (i_12 and 0xff00) * anInt8878 and 0xff0000
                                    i_12 = ((i_15 or i_16) ushr 8) + anInt8885
                                    i_17 = ints_3[i_11]
                                    ints_3[i_11] = ((i_12 and 0xff00ff) * i_13 + (i_17 and 0xff00ff) * i_14 and -16711936) + (i_13 * (i_12 and 0xff00) + i_14 * (i_17 and 0xff00) and 0xff0000) shr 8
                                }
                            } else {
                                if (i_2 != 2) {
                                    throw IllegalArgumentException()
                                }
                                if (i_1 == 1) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = ints_3[i_11]
                                    i_14 = i_12 + i_13
                                    i_15 = (i_12 and 0xff00ff) + (i_13 and 0xff00ff)
                                    i_13 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                                    ints_3[i_11] = i_14 - i_13 or (i_13 - (i_13 ushr 8))
                                } else if (i_1 == 0) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = anInt8880 * (i_12 and 0xff0000) and 0xff000000.toInt()
                                    i_14 = (i_12 and 0xff00) * anInt8852 and 0xff0000
                                    i_15 = (i_12 and 0xff) * anInt8882 and 0xff00
                                    i_12 = (i_13 or i_14 or i_15) ushr 8
                                    i_16 = ints_3[i_11]
                                    i_17 = i_12 + i_16
                                    i_18 = (i_12 and 0xff00ff) + (i_16 and 0xff00ff)
                                    i_16 = (i_18 and 0x1000100) + (i_17 - i_18 and 0x10000)
                                    ints_3[i_11] = i_17 - i_16 or (i_16 - (i_16 ushr 8))
                                } else if (i_1 == 3) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = anInt8877
                                    i_14 = i_12 + i_13
                                    i_15 = (i_12 and 0xff00ff) + (i_13 and 0xff00ff)
                                    i_16 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                                    i_12 = i_14 - i_16 or (i_16 - (i_16 ushr 8))
                                    i_16 = ints_3[i_11]
                                    i_14 = i_12 + i_16
                                    i_15 = (i_12 and 0xff00ff) + (i_16 and 0xff00ff)
                                    i_16 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                                    ints_3[i_11] = i_14 - i_16 or (i_16 - (i_16 ushr 8))
                                } else if (i_1 == 2) {
                                    i_12 = this.anIntArray10235[i_10]
                                    i_13 = (i_12 and 0xff00ff) * anInt8878 and -16711936
                                    i_14 = (i_12 and 0xff00) * anInt8878 and 0xff0000
                                    i_12 = ((i_13 or i_14) ushr 8) + anInt8885
                                    i_15 = ints_3[i_11]
                                    i_16 = i_12 + i_15
                                    i_17 = (i_12 and 0xff00ff) + (i_15 and 0xff00ff)
                                    i_15 = (i_17 and 0x1000100) + (i_16 - i_17 and 0x10000)
                                    ints_3[i_11] = i_16 - i_15 or (i_15 - (i_15 ushr 8))
                                }
                            }
                            i_7 += anInt8868
                            ++i_8
                        }
                    }
                    ++i_4
                    anInt8873 += anInt8869
                    anInt8870 += anInt8850
                }
            }
        } else if (anInt8865 < 0) {
            method14245NegX(i_1, i_2, ints_3)
        } else {
            method14245PosX(i_1, i_2, ints_3)
        }
    }

    private fun method14245NegX(i_1: Int, i_2: Int, ints_3: IntArray) {
        var i_4: Int
        var i_5: Int
        var i_6: Int
        var i_7: Int
        var i_8: Int
        var i_9: Int
        var i_10: Int
        var i_11: Int
        var i_12: Int
        var i_13: Int
        var i_14: Int
        var i_15: Int
        var i_16: Int
        var i_17: Int
        var i_18: Int
        if (anInt8868 == 0) {
            i_4 = anInt8864
            while (i_4 < 0) {
                i_5 = anInt8870
                i_6 = anInt8873 + anInt8858
                i_7 = anInt8874
                i_8 = anInt8862
                if (i_7 >= 0 && i_7 - (anInt8867 shl 12) < 0) {
                    i_9 = i_6 - (anInt8851 shl 12)
                    if (i_9 >= 0) {
                        i_9 = (anInt8865 - i_9) / anInt8865
                        i_8 += i_9
                        i_6 += i_9 * anInt8865
                        i_5 += i_9
                    }
                    i_9 = (i_6 - anInt8865) / anInt8865
                    if (i_9 > i_8) {
                        i_8 = i_9
                    }
                    while (i_8 < 0) {
                        i_10 = (i_6 shr 12) + (i_7 shr 12) * anInt8851
                        i_11 = i_5++
                        renderPixel14245(i_1, i_2, ints_3, i_10, i_11)
                        i_6 += anInt8865
                        ++i_8
                    }
                }
                ++i_4
                anInt8874 += anInt8876
                anInt8870 += anInt8850
            }
        } else if (anInt8868 < 0) {
            i_4 = anInt8864
            while (i_4 < 0) {
                i_5 = anInt8870
                i_6 = anInt8873 + anInt8858
                i_7 = anInt8874 + anInt8866
                i_8 = anInt8862
                i_9 = i_6 - (anInt8851 shl 12)
                if (i_9 >= 0) {
                    i_9 = (anInt8865 - i_9) / anInt8865
                    i_8 += i_9
                    i_6 += i_9 * anInt8865
                    i_7 += i_9 * anInt8868
                    i_5 += i_9
                }
                i_9 = (i_6 - anInt8865) / anInt8865
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                i_9 = i_7 - (anInt8867 shl 12)
                if (i_9 >= 0) {
                    i_9 = (anInt8868 - i_9) / anInt8868
                    i_8 += i_9
                    i_6 += i_9 * anInt8865
                    i_7 += i_9 * anInt8868
                    i_5 += i_9
                }
                i_9 = (i_7 - anInt8868) / anInt8868
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                while (i_8 < 0) {
                    i_10 = (i_6 shr 12) + (i_7 shr 12) * anInt8851
                    i_11 = i_5++
                    renderPixel14245(i_1, i_2, ints_3, i_10, i_11)
                    i_6 += anInt8865
                    i_7 += anInt8868
                    ++i_8
                }
                ++i_4
                anInt8873 += anInt8869
                anInt8874 += anInt8876
                anInt8870 += anInt8850
            }
        } else {
            i_4 = anInt8864
            while (i_4 < 0) {
                i_5 = anInt8870
                i_6 = anInt8873 + anInt8858
                i_7 = anInt8874 + anInt8866
                i_8 = anInt8862
                i_9 = i_6 - (anInt8851 shl 12)
                if (i_9 >= 0) {
                    i_9 = (anInt8865 - i_9) / anInt8865
                    i_8 += i_9
                    i_6 += i_9 * anInt8865
                    i_7 += i_9 * anInt8868
                    i_5 += i_9
                }
                i_9 = (i_6 - anInt8865) / anInt8865
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                if (i_7 < 0) {
                    i_9 = (anInt8868 - 1 - i_7) / anInt8868
                    i_8 += i_9
                    i_6 += i_9 * anInt8865
                    i_7 += i_9 * anInt8868
                    i_5 += i_9
                }
                i_9 = (i_7 + 1 - (anInt8867 shl 12) - anInt8868) / anInt8868
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                while (i_8 < 0) {
                    i_10 = (i_6 shr 12) + (i_7 shr 12) * anInt8851
                    i_11 = i_5++
                    renderPixel14245(i_1, i_2, ints_3, i_10, i_11)
                    i_6 += anInt8865
                    i_7 += anInt8868
                    ++i_8
                }
                ++i_4
                anInt8873 += anInt8869
                anInt8874 += anInt8876
                anInt8870 += anInt8850
            }
        }
    }

    private fun method14245PosX(i_1: Int, i_2: Int, ints_3: IntArray) {
        var i_4: Int
        var i_5: Int
        var i_6: Int
        var i_7: Int
        var i_8: Int
        var i_9: Int
        var i_10: Int
        var i_11: Int
        if (anInt8868 == 0) {
            i_4 = anInt8864
            while (i_4 < 0) {
                i_5 = anInt8870
                i_6 = anInt8873 + anInt8858
                i_7 = anInt8874
                i_8 = anInt8862
                if (i_7 >= 0 && i_7 - (anInt8867 shl 12) < 0) {
                    if (i_6 < 0) {
                        i_9 = (anInt8865 - 1 - i_6) / anInt8865
                        i_8 += i_9
                        i_6 += i_9 * anInt8865
                        i_5 += i_9
                    }
                    i_9 = (i_6 + 1 - (anInt8851 shl 12) - anInt8865) / anInt8865
                    if (i_9 > i_8) {
                        i_8 = i_9
                    }
                    while (i_8 < 0) {
                        i_10 = (i_6 shr 12) + (i_7 shr 12) * anInt8851
                        i_11 = i_5++
                        renderPixel14245(i_1, i_2, ints_3, i_10, i_11)
                        i_6 += anInt8865
                        ++i_8
                    }
                }
                ++i_4
                anInt8873 += anInt8869
                anInt8874 += anInt8876
                anInt8870 += anInt8850
            }
        } else if (anInt8868 < 0) {
            i_4 = anInt8864
            while (i_4 < 0) {
                i_5 = anInt8870
                i_6 = anInt8873 + anInt8858
                i_7 = anInt8874 + anInt8866
                i_8 = anInt8862
                if (i_6 < 0) {
                    i_9 = (anInt8865 - 1 - i_6) / anInt8865
                    i_8 += i_9
                    i_6 += i_9 * anInt8865
                    i_7 += i_9 * anInt8868
                    i_5 += i_9
                }
                i_9 = (i_6 + 1 - (anInt8851 shl 12) - anInt8865) / anInt8865
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                i_9 = i_7 - (anInt8867 shl 12)
                if (i_9 >= 0) {
                    i_9 = (anInt8868 - i_9) / anInt8868
                    i_8 += i_9
                    i_6 += i_9 * anInt8865
                    i_7 += i_9 * anInt8868
                    i_5 += i_9
                }
                i_9 = (i_7 - anInt8868) / anInt8868
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                while (i_8 < 0) {
                    i_10 = (i_6 shr 12) + (i_7 shr 12) * anInt8851
                    i_11 = i_5++
                    renderPixel14245(i_1, i_2, ints_3, i_10, i_11)
                    i_6 += anInt8865
                    i_7 += anInt8868
                    ++i_8
                }
                ++i_4
                anInt8873 += anInt8869
                anInt8874 += anInt8876
                anInt8870 += anInt8850
            }
        } else {
            i_4 = anInt8864
            while (i_4 < 0) {
                i_5 = anInt8870
                i_6 = anInt8873 + anInt8858
                i_7 = anInt8874 + anInt8866
                i_8 = anInt8862
                if (i_6 < 0) {
                    i_9 = (anInt8865 - 1 - i_6) / anInt8865
                    i_8 += i_9
                    i_6 += i_9 * anInt8865
                    i_7 += i_9 * anInt8868
                    i_5 += i_9
                }
                i_9 = (i_6 + 1 - (anInt8851 shl 12) - anInt8865) / anInt8865
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                if (i_7 < 0) {
                    i_9 = (anInt8868 - 1 - i_7) / anInt8868
                    i_8 += i_9
                    i_6 += i_9 * anInt8865
                    i_7 += i_9 * anInt8868
                    i_5 += i_9
                }
                i_9 = (i_7 + 1 - (anInt8867 shl 12) - anInt8868) / anInt8868
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                while (i_8 < 0) {
                    i_10 = (i_6 shr 12) + (i_7 shr 12) * anInt8851
                    i_11 = i_5++
                    renderPixel14245(i_1, i_2, ints_3, i_10, i_11)
                    i_6 += anInt8865
                    i_7 += anInt8868
                    ++i_8
                }
                ++i_4
                anInt8873 += anInt8869
                anInt8874 += anInt8876
                anInt8870 += anInt8850
            }
        }
    }

    private fun renderPixel14245(i_1: Int, i_2: Int, ints_3: IntArray, i_10: Int, i_11: Int) {
        var i_12: Int
        var i_13: Int
        var i_14: Int
        var i_15: Int
        var i_16: Int
        var i_17: Int
        var i_18: Int
        if (i_2 == 0) {
            if (i_1 == 1) {
                ints_3[i_11] = this.anIntArray10235[i_10]
            } else if (i_1 == 0) {
                i_12 = this.anIntArray10235[i_10]
                i_13 = anInt8880 * (i_12 and 0xff0000) and 0xff000000.toInt()
                i_14 = (i_12 and 0xff00) * anInt8852 and 0xff0000
                i_15 = (i_12 and 0xff) * anInt8882 and 0xff00
                ints_3[i_11] = (i_13 or i_14 or i_15) ushr 8
            } else if (i_1 == 3) {
                i_12 = this.anIntArray10235[i_10]
                i_13 = anInt8877
                i_14 = i_12 + i_13
                i_15 = (i_12 and 0xff00ff) + (i_13 and 0xff00ff)
                i_16 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                ints_3[i_11] = i_14 - i_16 or (i_16 - (i_16 ushr 8))
            } else {
                if (i_1 != 2) {
                    throw IllegalArgumentException()
                }
                i_12 = this.anIntArray10235[i_10]
                i_13 = (i_12 and 0xff00ff) * anInt8878 and -16711936
                i_14 = (i_12 and 0xff00) * anInt8878 and 0xff0000
                ints_3[i_11] = ((i_13 or i_14) ushr 8) + anInt8885
            }
        } else if (i_2 == 1) {
            if (i_1 == 1) {
                i_12 = this.anIntArray10235[i_10]
                i_13 = i_12 ushr 24
                i_14 = 256 - i_13
                i_15 = ints_3[i_11]
                ints_3[i_11] = ((i_12 and 0xff00ff) * i_13 + (i_15 and 0xff00ff) * i_14 and -16711936) + (i_13 * (i_12 and 0xff00) + i_14 * (i_15 and 0xff00) and 0xff0000) shr 8
            } else if (i_1 == 0) {
                i_12 = this.anIntArray10235[i_10]
                i_13 = (i_12 ushr 24) * anInt8878 shr 8
                i_14 = 256 - i_13
                if ((anInt8877 and 0xffffff) == 16777215) {
                    i_15 = ints_3[i_11]
                    ints_3[i_11] = ((i_12 and 0xff00ff) * i_13 + (i_15 and 0xff00ff) * i_14 and -16711936) + (i_13 * (i_12 and 0xff00) + i_14 * (i_15 and 0xff00) and 0xff0000) shr 8
                } else if (i_13 != 255) {
                    i_15 = anInt8880 * (i_12 and 0xff0000) and 0xff000000.toInt()
                    i_16 = (i_12 and 0xff00) * anInt8852 and 0xff0000
                    i_17 = (i_12 and 0xff) * anInt8882 and 0xff00
                    i_12 = (i_15 or i_16 or i_17) ushr 8
                    i_18 = ints_3[i_11]
                    ints_3[i_11] = ((i_12 and 0xff00ff) * i_13 + (i_18 and 0xff00ff) * i_14 and -16711936) + (i_13 * (i_12 and 0xff00) + i_14 * (i_18 and 0xff00) and 0xff0000) shr 8
                } else {
                    i_15 = anInt8880 * (i_12 and 0xff0000) and 0xff000000.toInt()
                    i_16 = (i_12 and 0xff00) * anInt8852 and 0xff0000
                    i_17 = (i_12 and 0xff) * anInt8882 and 0xff00
                    ints_3[i_11] = (i_15 or i_16 or i_17) ushr 8
                }
            } else if (i_1 == 3) {
                i_12 = this.anIntArray10235[i_10]
                i_13 = anInt8877
                i_14 = i_12 + i_13
                i_15 = (i_12 and 0xff00ff) + (i_13 and 0xff00ff)
                i_16 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                i_16 = i_14 - i_16 or (i_16 - (i_16 ushr 8))
                i_17 = (i_12 ushr 24) * anInt8878 shr 8
                i_18 = 256 - i_17
                if (i_17 != 255) {
                    i_12 = i_16
                    i_16 = ints_3[i_11]
                    i_16 = ((i_12 and 0xff00ff) * i_17 + (i_16 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_12 and 0xff00) + i_18 * (i_16 and 0xff00) and 0xff0000) shr 8
                }
                ints_3[i_11] = i_16
            } else {
                if (i_1 != 2) {
                    throw IllegalArgumentException()
                }
                i_12 = this.anIntArray10235[i_10]
                i_13 = i_12 ushr 24
                i_14 = 256 - i_13
                i_15 = (i_12 and 0xff00ff) * anInt8878 and -16711936
                i_16 = (i_12 and 0xff00) * anInt8878 and 0xff0000
                i_12 = ((i_15 or i_16) ushr 8) + anInt8885
                i_17 = ints_3[i_11]
                ints_3[i_11] = ((i_12 and 0xff00ff) * i_13 + (i_17 and 0xff00ff) * i_14 and -16711936) + (i_13 * (i_12 and 0xff00) + i_14 * (i_17 and 0xff00) and 0xff0000) shr 8
            }
        } else {
            if (i_2 != 2) {
                throw IllegalArgumentException()
            }
            if (i_1 == 1) {
                i_12 = this.anIntArray10235[i_10]
                i_13 = ints_3[i_11]
                i_14 = i_12 + i_13
                i_15 = (i_12 and 0xff00ff) + (i_13 and 0xff00ff)
                i_13 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                ints_3[i_11] = i_14 - i_13 or (i_13 - (i_13 ushr 8))
            } else if (i_1 == 0) {
                i_12 = this.anIntArray10235[i_10]
                i_13 = anInt8880 * (i_12 and 0xff0000) and 0xff000000.toInt()
                i_14 = (i_12 and 0xff00) * anInt8852 and 0xff0000
                i_15 = (i_12 and 0xff) * anInt8882 and 0xff00
                i_12 = (i_13 or i_14 or i_15) ushr 8
                i_16 = ints_3[i_11]
                i_17 = i_12 + i_16
                i_18 = (i_12 and 0xff00ff) + (i_16 and 0xff00ff)
                i_16 = (i_18 and 0x1000100) + (i_17 - i_18 and 0x10000)
                ints_3[i_11] = i_17 - i_16 or (i_16 - (i_16 ushr 8))
            } else if (i_1 == 3) {
                i_12 = this.anIntArray10235[i_10]
                i_13 = anInt8877
                i_14 = i_12 + i_13
                i_15 = (i_12 and 0xff00ff) + (i_13 and 0xff00ff)
                i_16 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                i_12 = i_14 - i_16 or (i_16 - (i_16 ushr 8))
                i_16 = ints_3[i_11]
                i_14 = i_12 + i_16
                i_15 = (i_12 and 0xff00ff) + (i_16 and 0xff00ff)
                i_16 = (i_15 and 0x1000100) + (i_14 - i_15 and 0x10000)
                ints_3[i_11] = i_14 - i_16 or (i_16 - (i_16 ushr 8))
            } else if (i_1 == 2) {
                i_12 = this.anIntArray10235[i_10]
                i_13 = (i_12 and 0xff00ff) * anInt8878 and -16711936
                i_14 = (i_12 and 0xff00) * anInt8878 and 0xff0000
                i_12 = ((i_13 or i_14) ushr 8) + anInt8885
                i_15 = ints_3[i_11]
                i_16 = i_12 + i_15
                i_17 = (i_12 and 0xff00ff) + (i_15 and 0xff00ff)
                i_15 = (i_17 and 0x1000100) + (i_16 - i_17 and 0x10000)
                ints_3[i_11] = i_16 - i_15 or (i_15 - (i_15 ushr 8))
            }
        }
    }

    override fun method14250(ints_1: IntArray, ints_2: IntArray, i_3: Int, i_4: Int) {
        val ints_5 = toolkit.colourBuffer ?: return
        var i_6: Int
        var i_7: Int
        var i_8: Int
        var i_9: Int
        var i_10: Int
        var i_11: Int
        var i_12: Int
        var i_13: Int
        var i_14: Int
        var i_15: Int
        var i_16: Int
        var i_17: Int
        var i_18: Int
        var i_19: Int
        if (anInt8865 == 0) {
            if (anInt8868 == 0) {
                i_6 = anInt8864
                while (i_6 < 0) {
                    i_7 = i_6 + i_4
                    if (i_7 >= 0) {
                        if (i_7 >= ints_1.size) {
                            break
                        }

                        i_8 = anInt8870
                        i_9 = anInt8873
                        i_10 = anInt8874
                        i_11 = anInt8862
                        if (i_9 >= 0 && i_10 >= 0 && i_9 - (anInt8851 shl 12) < 0 && i_10 - (anInt8867 shl 12) < 0) {
                            i_12 = ints_1[i_7] - i_3
                            i_13 = -ints_2[i_7]
                            i_14 = i_12 - (i_8 - anInt8870)
                            if (i_14 > 0) {
                                i_8 += i_14
                                i_11 += i_14
                                i_9 += i_14 * anInt8865
                                i_10 += i_14 * anInt8868
                            } else {
                                i_13 -= i_14
                            }

                            if (i_11 < i_13) {
                                i_11 = i_13
                            }

                            while (i_11 < 0) {
                                i_15 = this.anIntArray10235[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                                i_16 = i_15 ushr 24
                                i_17 = 256 - i_16
                                i_18 = ints_5[i_8]
                                ints_5[i_8++] = ((i_15 and 0xff00ff) * i_16 + (i_18 and 0xff00ff) * i_17 and -16711936) + (i_16 * (i_15 and 0xff00) + i_17 * (i_18 and 0xff00) and 0xff0000) shr 8
                                ++i_11
                            }
                        }
                    }

                    ++i_6
                    anInt8870 += anInt8850
                }
            } else if (anInt8868 < 0) {
                i_6 = anInt8864
                while (i_6 < 0) {
                    i_7 = i_6 + i_4
                    if (i_7 >= 0) {
                        if (i_7 >= ints_1.size) {
                            break
                        }

                        i_8 = anInt8870
                        i_9 = anInt8873
                        i_10 = anInt8874 + anInt8866
                        i_11 = anInt8862
                        if (i_9 >= 0 && i_9 - (anInt8851 shl 12) < 0) {
                            i_12 = i_10 - (anInt8867 shl 12)
                            if (i_12 >= 0) {
                                i_12 = (anInt8868 - i_12) / anInt8868
                                i_11 += i_12
                                i_10 += i_12 * anInt8868
                                i_8 += i_12
                            }

                            i_12 = (i_10 - anInt8868) / anInt8868
                            if (i_12 > i_11) {
                                i_11 = i_12
                            }

                            i_13 = ints_1[i_7] - i_3
                            i_14 = -ints_2[i_7]
                            i_15 = i_13 - (i_8 - anInt8870)
                            if (i_15 > 0) {
                                i_8 += i_15
                                i_11 += i_15
                                i_9 += i_15 * anInt8865
                                i_10 += i_15 * anInt8868
                            } else {
                                i_14 -= i_15
                            }

                            if (i_11 < i_14) {
                                i_11 = i_14
                            }

                            while (i_11 < 0) {
                                i_16 = this.anIntArray10235[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                                i_17 = i_16 ushr 24
                                i_18 = 256 - i_17
                                i_19 = ints_5[i_8]
                                ints_5[i_8++] = ((i_16 and 0xff00ff) * i_17 + (i_19 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_16 and 0xff00) + i_18 * (i_19 and 0xff00) and 0xff0000) shr 8
                                i_10 += anInt8868
                                ++i_11
                            }
                        }
                    }

                    ++i_6
                    anInt8873 += anInt8869
                    anInt8870 += anInt8850
                }
            } else {
                i_6 = anInt8864
                while (i_6 < 0) {
                    i_7 = i_6 + i_4
                    if (i_7 >= 0) {
                        if (i_7 >= ints_1.size) {
                            break
                        }

                        i_8 = anInt8870
                        i_9 = anInt8873
                        i_10 = anInt8874 + anInt8866
                        i_11 = anInt8862
                        if (i_9 >= 0 && i_9 - (anInt8851 shl 12) < 0) {
                            if (i_10 < 0) {
                                i_12 = (anInt8868 - 1 - i_10) / anInt8868
                                i_11 += i_12
                                i_10 += i_12 * anInt8868
                                i_8 += i_12
                            }

                            i_12 = (i_10 + 1 - (anInt8867 shl 12) - anInt8868) / anInt8868
                            if (i_12 > i_11) {
                                i_11 = i_12
                            }

                            i_13 = ints_1[i_7] - i_3
                            i_14 = -ints_2[i_7]
                            i_15 = i_13 - (i_8 - anInt8870)
                            if (i_15 > 0) {
                                i_8 += i_15
                                i_11 += i_15
                                i_9 += i_15 * anInt8865
                                i_10 += i_15 * anInt8868
                            } else {
                                i_14 -= i_15
                            }

                            if (i_11 < i_14) {
                                i_11 = i_14
                            }

                            while (i_11 < 0) {
                                i_16 = this.anIntArray10235[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                                i_17 = i_16 ushr 24
                                i_18 = 256 - i_17
                                i_19 = ints_5[i_8]
                                ints_5[i_8++] = ((i_16 and 0xff00ff) * i_17 + (i_19 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_16 and 0xff00) + i_18 * (i_19 and 0xff00) and 0xff0000) shr 8
                                i_10 += anInt8868
                                ++i_11
                            }
                        }
                    }

                    ++i_6
                    anInt8873 += anInt8869
                    anInt8870 += anInt8850
                }
            }
        } else if (anInt8865 < 0) {
            if (anInt8868 == 0) {
                i_6 = anInt8864
                while (i_6 < 0) {
                    i_7 = i_6 + i_4
                    if (i_7 >= 0) {
                        if (i_7 >= ints_1.size) {
                            break
                        }

                        i_8 = anInt8870
                        i_9 = anInt8873 + anInt8858
                        i_10 = anInt8874
                        i_11 = anInt8862
                        if (i_10 >= 0 && i_10 - (anInt8867 shl 12) < 0) {
                            i_12 = i_9 - (anInt8851 shl 12)
                            if (i_12 >= 0) {
                                i_12 = (anInt8865 - i_12) / anInt8865
                                i_11 += i_12
                                i_9 += i_12 * anInt8865
                                i_8 += i_12
                            }

                            i_12 = (i_9 - anInt8865) / anInt8865
                            if (i_12 > i_11) {
                                i_11 = i_12
                            }

                            i_13 = ints_1[i_7] - i_3
                            i_14 = -ints_2[i_7]
                            i_15 = i_13 - (i_8 - anInt8870)
                            if (i_15 > 0) {
                                i_8 += i_15
                                i_11 += i_15
                                i_9 += i_15 * anInt8865
                                i_10 += i_15 * anInt8868
                            } else {
                                i_14 -= i_15
                            }

                            if (i_11 < i_14) {
                                i_11 = i_14
                            }

                            while (i_11 < 0) {
                                i_16 = this.anIntArray10235[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                                i_17 = i_16 ushr 24
                                i_18 = 256 - i_17
                                i_19 = ints_5[i_8]
                                ints_5[i_8++] = ((i_16 and 0xff00ff) * i_17 + (i_19 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_16 and 0xff00) + i_18 * (i_19 and 0xff00) and 0xff0000) shr 8
                                i_9 += anInt8865
                                ++i_11
                            }
                        }
                    }

                    ++i_6
                    anInt8874 += anInt8876
                    anInt8870 += anInt8850
                }
            } else if (anInt8868 < 0) {
                i_6 = anInt8864
                while (i_6 < 0) {
                    i_7 = i_6 + i_4
                    if (i_7 >= 0) {
                        if (i_7 >= ints_1.size) {
                            break
                        }

                        i_8 = anInt8870
                        i_9 = anInt8873 + anInt8858
                        i_10 = anInt8874 + anInt8866
                        i_11 = anInt8862
                        i_12 = i_9 - (anInt8851 shl 12)
                        if (i_12 >= 0) {
                            i_12 = (anInt8865 - i_12) / anInt8865
                            i_11 += i_12
                            i_9 += i_12 * anInt8865
                            i_10 += i_12 * anInt8868
                            i_8 += i_12
                        }

                        i_12 = (i_9 - anInt8865) / anInt8865
                        if (i_12 > i_11) {
                            i_11 = i_12
                        }

                        i_12 = i_10 - (anInt8867 shl 12)
                        if (i_12 >= 0) {
                            i_12 = (anInt8868 - i_12) / anInt8868
                            i_11 += i_12
                            i_9 += i_12 * anInt8865
                            i_10 += i_12 * anInt8868
                            i_8 += i_12
                        }

                        i_12 = (i_10 - anInt8868) / anInt8868
                        if (i_12 > i_11) {
                            i_11 = i_12
                        }

                        i_13 = ints_1[i_7] - i_3
                        i_14 = -ints_2[i_7]
                        i_15 = i_13 - (i_8 - anInt8870)
                        if (i_15 > 0) {
                            i_8 += i_15
                            i_11 += i_15
                            i_9 += i_15 * anInt8865
                            i_10 += i_15 * anInt8868
                        } else {
                            i_14 -= i_15
                        }

                        if (i_11 < i_14) {
                            i_11 = i_14
                        }

                        while (i_11 < 0) {
                            i_16 = this.anIntArray10235[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                            i_17 = i_16 ushr 24
                            i_18 = 256 - i_17
                            i_19 = ints_5[i_8]
                            ints_5[i_8++] = ((i_16 and 0xff00ff) * i_17 + (i_19 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_16 and 0xff00) + i_18 * (i_19 and 0xff00) and 0xff0000) shr 8
                            i_9 += anInt8865
                            i_10 += anInt8868
                            ++i_11
                        }
                    }

                    ++i_6
                    anInt8873 += anInt8869
                    anInt8874 += anInt8876
                    anInt8870 += anInt8850
                }
            } else {
                i_6 = anInt8864
                while (i_6 < 0) {
                    i_7 = i_6 + i_4
                    if (i_7 >= 0) {
                        if (i_7 >= ints_1.size) {
                            break
                        }

                        i_8 = anInt8870
                        i_9 = anInt8873 + anInt8858
                        i_10 = anInt8874 + anInt8866
                        i_11 = anInt8862
                        i_12 = i_9 - (anInt8851 shl 12)
                        if (i_12 >= 0) {
                            i_12 = (anInt8865 - i_12) / anInt8865
                            i_11 += i_12
                            i_9 += i_12 * anInt8865
                            i_10 += i_12 * anInt8868
                            i_8 += i_12
                        }

                        i_12 = (i_9 - anInt8865) / anInt8865
                        if (i_12 > i_11) {
                            i_11 = i_12
                        }

                        if (i_10 < 0) {
                            i_12 = (anInt8868 - 1 - i_10) / anInt8868
                            i_11 += i_12
                            i_9 += i_12 * anInt8865
                            i_10 += i_12 * anInt8868
                            i_8 += i_12
                        }

                        i_12 = (i_10 + 1 - (anInt8867 shl 12) - anInt8868) / anInt8868
                        if (i_12 > i_11) {
                            i_11 = i_12
                        }

                        i_13 = ints_1[i_7] - i_3
                        i_14 = -ints_2[i_7]
                        i_15 = i_13 - (i_8 - anInt8870)
                        if (i_15 > 0) {
                            i_8 += i_15
                            i_11 += i_15
                            i_9 += i_15 * anInt8865
                            i_10 += i_15 * anInt8868
                        } else {
                            i_14 -= i_15
                        }

                        if (i_11 < i_14) {
                            i_11 = i_14
                        }

                        while (i_11 < 0) {
                            i_16 = this.anIntArray10235[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                            i_17 = i_16 ushr 24
                            i_18 = 256 - i_17
                            i_19 = ints_5[i_8]
                            ints_5[i_8++] = ((i_16 and 0xff00ff) * i_17 + (i_19 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_16 and 0xff00) + i_18 * (i_19 and 0xff00) and 0xff0000) shr 8
                            i_9 += anInt8865
                            i_10 += anInt8868
                            ++i_11
                        }
                    }

                    ++i_6
                    anInt8873 += anInt8869
                    anInt8874 += anInt8876
                    anInt8870 += anInt8850
                }
            }
        } else if (anInt8868 == 0) {
            i_6 = anInt8864
            while (i_6 < 0) {
                i_7 = i_6 + i_4
                if (i_7 >= 0) {
                    if (i_7 >= ints_1.size) {
                        break
                    }

                    i_8 = anInt8870
                    i_9 = anInt8873 + anInt8858
                    i_10 = anInt8874
                    i_11 = anInt8862
                    if (i_10 >= 0 && i_10 - (anInt8867 shl 12) < 0) {
                        if (i_9 < 0) {
                            i_12 = (anInt8865 - 1 - i_9) / anInt8865
                            i_11 += i_12
                            i_9 += i_12 * anInt8865
                            i_8 += i_12
                        }

                        i_12 = (i_9 + 1 - (anInt8851 shl 12) - anInt8865) / anInt8865
                        if (i_12 > i_11) {
                            i_11 = i_12
                        }

                        i_13 = ints_1[i_7] - i_3
                        i_14 = -ints_2[i_7]
                        i_15 = i_13 - (i_8 - anInt8870)
                        if (i_15 > 0) {
                            i_8 += i_15
                            i_11 += i_15
                            i_9 += i_15 * anInt8865
                            i_10 += i_15 * anInt8868
                        } else {
                            i_14 -= i_15
                        }

                        if (i_11 < i_14) {
                            i_11 = i_14
                        }

                        while (i_11 < 0) {
                            i_16 = this.anIntArray10235[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                            i_17 = i_16 ushr 24
                            i_18 = 256 - i_17
                            i_19 = ints_5[i_8]
                            ints_5[i_8++] = ((i_16 and 0xff00ff) * i_17 + (i_19 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_16 and 0xff00) + i_18 * (i_19 and 0xff00) and 0xff0000) shr 8
                            i_9 += anInt8865
                            ++i_11
                        }
                    }
                }

                ++i_6
                anInt8873 += anInt8869
                anInt8874 += anInt8876
                anInt8870 += anInt8850
            }
        } else if (anInt8868 < 0) {
            i_6 = anInt8864
            while (i_6 < 0) {
                i_7 = i_6 + i_4
                if (i_7 >= 0) {
                    if (i_7 >= ints_1.size) {
                        break
                    }

                    i_8 = anInt8870
                    i_9 = anInt8873 + anInt8858
                    i_10 = anInt8874 + anInt8866
                    i_11 = anInt8862
                    if (i_9 < 0) {
                        i_12 = (anInt8865 - 1 - i_9) / anInt8865
                        i_11 += i_12
                        i_9 += i_12 * anInt8865
                        i_10 += i_12 * anInt8868
                        i_8 += i_12
                    }

                    i_12 = (i_9 + 1 - (anInt8851 shl 12) - anInt8865) / anInt8865
                    if (i_12 > i_11) {
                        i_11 = i_12
                    }

                    i_12 = i_10 - (anInt8867 shl 12)
                    if (i_12 >= 0) {
                        i_12 = (anInt8868 - i_12) / anInt8868
                        i_11 += i_12
                        i_9 += i_12 * anInt8865
                        i_10 += i_12 * anInt8868
                        i_8 += i_12
                    }

                    i_12 = (i_10 - anInt8868) / anInt8868
                    if (i_12 > i_11) {
                        i_11 = i_12
                    }

                    i_13 = ints_1[i_7] - i_3
                    i_14 = -ints_2[i_7]
                    i_15 = i_13 - (i_8 - anInt8870)
                    if (i_15 > 0) {
                        i_8 += i_15
                        i_11 += i_15
                        i_9 += i_15 * anInt8865
                        i_10 += i_15 * anInt8868
                    } else {
                        i_14 -= i_15
                    }

                    if (i_11 < i_14) {
                        i_11 = i_14
                    }

                    while (i_11 < 0) {
                        i_16 = this.anIntArray10235[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                        i_17 = i_16 ushr 24
                        i_18 = 256 - i_17
                        i_19 = ints_5[i_8]
                        ints_5[i_8++] = ((i_16 and 0xff00ff) * i_17 + (i_19 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_16 and 0xff00) + i_18 * (i_19 and 0xff00) and 0xff0000) shr 8
                        i_9 += anInt8865
                        i_10 += anInt8868
                        ++i_11
                    }
                }

                ++i_6
                anInt8873 += anInt8869
                anInt8874 += anInt8876
                anInt8870 += anInt8850
            }
        } else {
            i_6 = anInt8864
            while (i_6 < 0) {
                i_7 = i_6 + i_4
                if (i_7 >= 0) {
                    if (i_7 >= ints_1.size) {
                        break
                    }

                    i_8 = anInt8870
                    i_9 = anInt8873 + anInt8858
                    i_10 = anInt8874 + anInt8866
                    i_11 = anInt8862
                    if (i_9 < 0) {
                        i_12 = (anInt8865 - 1 - i_9) / anInt8865
                        i_11 += i_12
                        i_9 += i_12 * anInt8865
                        i_10 += i_12 * anInt8868
                        i_8 += i_12
                    }

                    i_12 = (i_9 + 1 - (anInt8851 shl 12) - anInt8865) / anInt8865
                    if (i_12 > i_11) {
                        i_11 = i_12
                    }

                    if (i_10 < 0) {
                        i_12 = (anInt8868 - 1 - i_10) / anInt8868
                        i_11 += i_12
                        i_9 += i_12 * anInt8865
                        i_10 += i_12 * anInt8868
                        i_8 += i_12
                    }

                    i_12 = (i_10 + 1 - (anInt8867 shl 12) - anInt8868) / anInt8868
                    if (i_12 > i_11) {
                        i_11 = i_12
                    }

                    i_13 = ints_1[i_7] - i_3
                    i_14 = -ints_2[i_7]
                    i_15 = i_13 - (i_8 - anInt8870)
                    if (i_15 > 0) {
                        i_8 += i_15
                        i_11 += i_15
                        i_9 += i_15 * anInt8865
                        i_10 += i_15 * anInt8868
                    } else {
                        i_14 -= i_15
                    }

                    if (i_11 < i_14) {
                        i_11 = i_14
                    }

                    while (i_11 < 0) {
                        i_16 = this.anIntArray10235[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                        i_17 = i_16 ushr 24
                        i_18 = 256 - i_17
                        i_19 = ints_5[i_8]
                        ints_5[i_8++] = ((i_16 and 0xff00ff) * i_17 + (i_19 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_16 and 0xff00) + i_18 * (i_19 and 0xff00) and 0xff0000) shr 8
                        i_9 += anInt8865
                        i_10 += anInt8868
                        ++i_11
                    }
                }

                ++i_6
                anInt8873 += anInt8869
                anInt8874 += anInt8876
                anInt8870 += anInt8850
            }
        }
    }

    override fun drawColoredCircle(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, colorArgb: Int, blendMode: Int) {
        var i_1Var = i_1
        var i_2Var = i_2
        var i_33 = i_3
        var i_41 = i_4
        var i_61 = colorArgb
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        if (i_33 > 0 && i_41 > 0) {
            val ints_9 = toolkit.colourBuffer ?: return
            var i_10 = 0
            var i_11 = 0
            val i_12 = toolkit.viewportWidth
            val i_13 = anInt8853 + anInt8851 + anInt8855
            val i_14 = anInt8854 + anInt8867 + anInt8856
            val i_15 = (i_13 shl 16) / i_33
            val i_16 = (i_14 shl 16) / i_41
            var i_17: Int
            if (anInt8853 > 0) {
                i_17 = (i_15 + (anInt8853 shl 16) - 1) / i_15
                i_1Var += i_17
                i_10 += i_17 * i_15 - (anInt8853 shl 16)
            }

            if (anInt8854 > 0) {
                i_17 = (i_16 + (anInt8854 shl 16) - 1) / i_16
                i_2Var += i_17
                i_11 += i_17 * i_16 - (anInt8854 shl 16)
            }

            if (anInt8851 < i_13) {
                i_33 = ((anInt8851 shl 16) - i_10 + i_15 - 1) / i_15
            }

            if (anInt8867 < i_14) {
                i_41 = ((anInt8867 shl 16) - i_11 + i_16 - 1) / i_16
            }

            i_17 = i_12 * i_2Var + i_1Var
            var i_18 = i_12 - i_33
            if (i_2Var + i_41 > toolkit.clipBottom) {
                i_41 -= i_2Var + i_41 - toolkit.clipBottom
            }

            var i_19: Int
            if (i_2Var < toolkit.clipTop) {
                i_19 = toolkit.clipTop - i_2Var
                i_41 -= i_19
                i_17 += i_19 * i_12
                i_11 += i_16 * i_19
            }

            if (i_33 + i_1Var > toolkit.clipRight) {
                i_19 = i_33 + i_1Var - toolkit.clipRight
                i_33 -= i_19
                i_18 += i_19
            }

            if (i_1Var < toolkit.clipLeft) {
                i_19 = toolkit.clipLeft - i_1Var
                i_33 -= i_19
                i_17 += i_19
                i_10 += i_15 * i_19
                i_18 += i_19
            }

            var i_20: Int
            var i_21: Int
            var i_22: Int
            var i_23: Int
            var i_24: Int
            var i_25: Int
            var i_26: Int
            var i_27: Int
            var i_28: Int
            var i_29: Int
            if (blendMode == 0) {
                if (i_5 == 1) {
                    i_19 = i_10

                    i_20 = -i_41
                    while (i_20 < 0) {
                        i_21 = anInt8851 * (i_11 shr 16)

                        i_22 = -i_33
                        while (i_22 < 0) {
                            ints_9[i_17++] = this.anIntArray10235[i_21 + (i_10 shr 16)]
                            i_10 += i_15
                            i_22++
                        }

                        i_11 += i_16
                        i_10 = i_19
                        i_17 += i_18
                        i_20++
                    }
                } else if (i_5 == 0) {
                    i_19 = (i_61 and 0xff0000) shr 16
                    i_20 = (i_61 and 0xff00) shr 8
                    i_21 = i_61 and 0xff
                    i_22 = i_10

                    i_23 = -i_41
                    while (i_23 < 0) {
                        i_24 = anInt8851 * (i_11 shr 16)

                        i_25 = -i_33
                        while (i_25 < 0) {
                            i_26 = this.anIntArray10235[i_24 + (i_10 shr 16)]
                            i_27 = i_19 * (i_26 and 0xff0000) and 0xff000000.toInt()
                            i_28 = i_20 * (i_26 and 0xff00) and 0xff0000
                            i_29 = (i_26 and 0xff) * i_21 and 0xff00
                            ints_9[i_17++] = (i_27 or i_28 or i_29) ushr 8
                            i_10 += i_15
                            i_25++
                        }

                        i_11 += i_16
                        i_10 = i_22
                        i_17 += i_18
                        i_23++
                    }
                } else if (i_5 == 3) {
                    i_19 = i_10

                    i_20 = -i_41
                    while (i_20 < 0) {
                        i_21 = anInt8851 * (i_11 shr 16)

                        i_22 = -i_33
                        while (i_22 < 0) {
                            i_23 = this.anIntArray10235[i_21 + (i_10 shr 16)]
                            i_24 = i_23 + i_61
                            i_25 = (i_23 and 0xff00ff) + (i_61 and 0xff00ff)
                            i_26 = (i_25 and 0x1000100) + (i_24 - i_25 and 0x10000)
                            ints_9[i_17++] = i_24 - i_26 or (i_26 - (i_26 ushr 8))
                            i_10 += i_15
                            i_22++
                        }

                        i_11 += i_16
                        i_10 = i_19
                        i_17 += i_18
                        i_20++
                    }
                } else {
                    if (i_5 != 2) {
                        throw IllegalArgumentException()
                    }

                    i_19 = i_61 ushr 24
                    i_20 = 256 - i_19
                    i_21 = (i_61 and 0xff00ff) * i_20 and -16711936
                    i_22 = i_20 * (i_61 and 0xff00) and 0xff0000
                    i_61 = (i_21 or i_22) ushr 8
                    i_23 = i_10

                    i_24 = -i_41
                    while (i_24 < 0) {
                        i_25 = anInt8851 * (i_11 shr 16)

                        i_26 = -i_33
                        while (i_26 < 0) {
                            i_27 = this.anIntArray10235[i_25 + (i_10 shr 16)]
                            i_21 = (i_27 and 0xff00ff) * i_19 and -16711936
                            i_22 = i_19 * (i_27 and 0xff00) and 0xff0000
                            ints_9[i_17++] = ((i_21 or i_22) ushr 8) + i_61
                            i_10 += i_15
                            i_26++
                        }

                        i_11 += i_16
                        i_10 = i_23
                        i_17 += i_18
                        i_24++
                    }
                }
            } else {
                var i_30: Int
                var i_31: Int
                var i_32: Int
                if (blendMode == 1) {
                    if (i_5 == 1) {
                        i_19 = i_10

                        i_20 = -i_41
                        while (i_20 < 0) {
                            i_21 = anInt8851 * (i_11 shr 16)

                            i_22 = -i_33
                            while (i_22 < 0) {
                                i_23 = this.anIntArray10235[i_21 + (i_10 shr 16)]
                                i_24 = i_23 ushr 24
                                i_25 = 256 - i_24
                                i_26 = ints_9[i_17]
                                ints_9[i_17++] = (((i_23 and -16711936) ushr 8) * i_24 + ((i_26 and -16711936) ushr 8) * i_25 and -16711936) + (((i_23 and 0xff00ff) * i_24 + (i_26 and 0xff00ff) * i_25 and -16711936) shr 8)
                                i_10 += i_15
                                i_22++
                            }

                            i_11 += i_16
                            i_10 = i_19
                            i_17 += i_18
                            i_20++
                        }
                    } else if (i_5 == 0) {
                        i_19 = i_10
                        if ((i_61 and 0xffffff) == 16777215) {
                            i_20 = -i_41
                            while (i_20 < 0) {
                                i_21 = anInt8851 * (i_11 shr 16)

                                i_22 = -i_33
                                while (i_22 < 0) {
                                    i_23 = this.anIntArray10235[i_21 + (i_10 shr 16)]
                                    i_24 = (i_61 ushr 24) * (i_23 ushr 24) shr 8
                                    i_25 = 256 - i_24
                                    i_26 = ints_9[i_17]
                                    ints_9[i_17++] = ((i_23 and 0xff00ff) * i_24 + (i_26 and 0xff00ff) * i_25 and -16711936) + (i_24 * (i_23 and 0xff00) + i_25 * (i_26 and 0xff00) and 0xff0000) shr 8
                                    i_10 += i_15
                                    i_22++
                                }

                                i_11 += i_16
                                i_10 = i_19
                                i_17 += i_18
                                i_20++
                            }
                        } else {
                            i_20 = (i_61 and 0xff0000) shr 16
                            i_21 = (i_61 and 0xff00) shr 8
                            i_22 = i_61 and 0xff

                            i_23 = -i_41
                            while (i_23 < 0) {
                                i_24 = anInt8851 * (i_11 shr 16)

                                i_25 = -i_33
                                while (i_25 < 0) {
                                    i_26 = this.anIntArray10235[i_24 + (i_10 shr 16)]
                                    i_27 = (i_61 ushr 24) * (i_26 ushr 24) shr 8
                                    i_28 = 256 - i_27
                                    if (i_27 != 255) {
                                        i_29 = i_20 * (i_26 and 0xff0000) and 0xff000000.toInt()
                                        i_30 = i_21 * (i_26 and 0xff00) and 0xff0000
                                        i_31 = (i_26 and 0xff) * i_22 and 0xff00
                                        i_26 = (i_29 or i_30 or i_31) ushr 8
                                        i_32 = ints_9[i_17]
                                        ints_9[i_17++] = ((i_26 and 0xff00ff) * i_27 + (i_32 and 0xff00ff) * i_28 and -16711936) + (i_27 * (i_26 and 0xff00) + i_28 * (i_32 and 0xff00) and 0xff0000) shr 8
                                    } else {
                                        i_29 = i_20 * (i_26 and 0xff0000) and 0xff000000.toInt()
                                        i_30 = i_21 * (i_26 and 0xff00) and 0xff0000
                                        i_31 = (i_26 and 0xff) * i_22 and 0xff00
                                        ints_9[i_17++] = (i_29 or i_30 or i_31) ushr 8
                                    }

                                    i_10 += i_15
                                    i_25++
                                }

                                i_11 += i_16
                                i_10 = i_19
                                i_17 += i_18
                                i_23++
                            }
                        }
                    } else if (i_5 == 3) {
                        i_19 = i_10

                        i_20 = -i_41
                        while (i_20 < 0) {
                            i_21 = anInt8851 * (i_11 shr 16)

                            i_22 = -i_33
                            while (i_22 < 0) {
                                i_23 = this.anIntArray10235[i_21 + (i_10 shr 16)]
                                i_24 = i_23 + i_61
                                i_25 = (i_23 and 0xff00ff) + (i_61 and 0xff00ff)
                                i_26 = (i_25 and 0x1000100) + (i_24 - i_25 and 0x10000)
                                i_26 = i_24 - i_26 or (i_26 - (i_26 ushr 8))
                                i_27 = (i_61 ushr 24) * (i_26 ushr 24) shr 8
                                i_28 = 256 - i_27
                                if (i_27 != 255) {
                                    i_23 = i_26
                                    i_26 = ints_9[i_17]
                                    i_26 = ((i_23 and 0xff00ff) * i_27 + (i_26 and 0xff00ff) * i_28 and -16711936) + (i_27 * (i_23 and 0xff00) + i_28 * (i_26 and 0xff00) and 0xff0000) shr 8
                                }

                                ints_9[i_17++] = i_26
                                i_10 += i_15
                                i_22++
                            }

                            i_11 += i_16
                            i_10 = i_19
                            i_17 += i_18
                            i_20++
                        }
                    } else {
                        if (i_5 != 2) {
                            throw IllegalArgumentException()
                        }

                        i_19 = i_61 ushr 24
                        i_20 = 256 - i_19
                        i_21 = (i_61 and 0xff00ff) * i_20 and -16711936
                        i_22 = i_20 * (i_61 and 0xff00) and 0xff0000
                        i_61 = (i_21 or i_22) ushr 8
                        i_23 = i_10

                        i_24 = -i_41
                        while (i_24 < 0) {
                            i_25 = anInt8851 * (i_11 shr 16)

                            i_26 = -i_33
                            while (i_26 < 0) {
                                i_27 = this.anIntArray10235[i_25 + (i_10 shr 16)]
                                i_28 = i_27 ushr 24
                                i_29 = 256 - i_28
                                i_21 = (i_27 and 0xff00ff) * i_19 and -16711936
                                i_22 = i_19 * (i_27 and 0xff00) and 0xff0000
                                i_27 = ((i_21 or i_22) ushr 8) + i_61
                                i_30 = ints_9[i_17]
                                ints_9[i_17++] = ((i_27 and 0xff00ff) * i_28 + (i_30 and 0xff00ff) * i_29 and -16711936) + (i_28 * (i_27 and 0xff00) + i_29 * (i_30 and 0xff00) and 0xff0000) shr 8
                                i_10 += i_15
                                i_26++
                            }

                            i_11 += i_16
                            i_10 = i_23
                            i_17 += i_18
                            i_24++
                        }
                    }
                } else {
                    if (blendMode != 2) {
                        throw IllegalArgumentException()
                    }

                    if (i_5 == 1) {
                        i_19 = i_10

                        i_20 = -i_41
                        while (i_20 < 0) {
                            i_21 = anInt8851 * (i_11 shr 16)

                            i_22 = -i_33
                            while (i_22 < 0) {
                                i_23 = this.anIntArray10235[i_21 + (i_10 shr 16)]
                                if (i_23 != 0) {
                                    i_24 = ints_9[i_17]
                                    i_25 = i_23 + i_24
                                    i_26 = (i_23 and 0xff00ff) + (i_24 and 0xff00ff)
                                    i_24 = (i_26 and 0x1000100) + (i_25 - i_26 and 0x10000)
                                    ints_9[i_17++] = i_25 - i_24 or (i_24 - (i_24 ushr 8))
                                } else {
                                    ++i_17
                                }

                                i_10 += i_15
                                i_22++
                            }

                            i_11 += i_16
                            i_10 = i_19
                            i_17 += i_18
                            i_20++
                        }
                    } else if (i_5 == 0) {
                        i_19 = i_10
                        i_20 = (i_61 and 0xff0000) shr 16
                        i_21 = (i_61 and 0xff00) shr 8
                        i_22 = i_61 and 0xff

                        i_23 = -i_41
                        while (i_23 < 0) {
                            i_24 = anInt8851 * (i_11 shr 16)

                            i_25 = -i_33
                            while (i_25 < 0) {
                                i_26 = this.anIntArray10235[i_24 + (i_10 shr 16)]
                                if (i_26 != 0) {
                                    i_27 = i_20 * (i_26 and 0xff0000) and 0xff000000.toInt()
                                    i_28 = i_21 * (i_26 and 0xff00) and 0xff0000
                                    i_29 = (i_26 and 0xff) * i_22 and 0xff00
                                    i_26 = (i_27 or i_28 or i_29) ushr 8
                                    i_30 = ints_9[i_17]
                                    i_31 = i_26 + i_30
                                    i_32 = (i_26 and 0xff00ff) + (i_30 and 0xff00ff)
                                    i_30 = (i_32 and 0x1000100) + (i_31 - i_32 and 0x10000)
                                    ints_9[i_17++] = i_31 - i_30 or (i_30 - (i_30 ushr 8))
                                } else {
                                    ++i_17
                                }

                                i_10 += i_15
                                i_25++
                            }

                            i_11 += i_16
                            i_10 = i_19
                            i_17 += i_18
                            i_23++
                        }
                    } else if (i_5 == 3) {
                        i_19 = i_10

                        i_20 = -i_41
                        while (i_20 < 0) {
                            i_21 = anInt8851 * (i_11 shr 16)

                            i_22 = -i_33
                            while (i_22 < 0) {
                                i_23 = this.anIntArray10235[i_21 + (i_10 shr 16)]
                                i_24 = i_23 + i_61
                                i_25 = (i_23 and 0xff00ff) + (i_61 and 0xff00ff)
                                i_26 = (i_25 and 0x1000100) + (i_24 - i_25 and 0x10000)
                                i_23 = i_24 - i_26 or (i_26 - (i_26 ushr 8))
                                i_26 = ints_9[i_17]
                                i_24 = i_23 + i_26
                                i_25 = (i_23 and 0xff00ff) + (i_26 and 0xff00ff)
                                i_26 = (i_25 and 0x1000100) + (i_24 - i_25 and 0x10000)
                                ints_9[i_17++] = i_24 - i_26 or (i_26 - (i_26 ushr 8))
                                i_10 += i_15
                                i_22++
                            }

                            i_11 += i_16
                            i_10 = i_19
                            i_17 += i_18
                            i_20++
                        }
                    } else {
                        if (i_5 != 2) {
                            throw IllegalArgumentException()
                        }

                        i_19 = i_61 ushr 24
                        i_20 = 256 - i_19
                        i_21 = (i_61 and 0xff00ff) * i_20 and -16711936
                        i_22 = i_20 * (i_61 and 0xff00) and 0xff0000
                        i_61 = (i_21 or i_22) ushr 8
                        i_23 = i_10

                        i_24 = -i_41
                        while (i_24 < 0) {
                            i_25 = anInt8851 * (i_11 shr 16)

                            i_26 = -i_33
                            while (i_26 < 0) {
                                i_27 = this.anIntArray10235[i_25 + (i_10 shr 16)]
                                if (i_27 != 0) {
                                    i_21 = (i_27 and 0xff00ff) * i_19 and -16711936
                                    i_22 = i_19 * (i_27 and 0xff00) and 0xff0000
                                    i_27 = ((i_21 or i_22) ushr 8) + i_61
                                    i_28 = ints_9[i_17]
                                    i_29 = i_27 + i_28
                                    i_30 = (i_27 and 0xff00ff) + (i_28 and 0xff00ff)
                                    i_28 = (i_30 and 0x1000100) + (i_29 - i_30 and 0x10000)
                                    ints_9[i_17++] = i_29 - i_28 or (i_28 - (i_28 ushr 8))
                                } else {
                                    ++i_17
                                }

                                i_10 += i_15
                                i_26++
                            }

                            i_11 += i_16
                            i_10 = i_23
                            i_17 += i_18
                            i_24++
                        }
                    }
                }
            }
        }
    }
}
