package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.unidentified.ComponentDimensionSM

class PaletteSpriteSM(
    toolkit: SafeModeToolkit,
    @JvmField val spritePixels: ByteArray,
    @JvmField val spritePalette: IntArray,
    width: Int,
    height: Int
) : SpriteSM(toolkit, width, height) {

    override fun method2750(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int) {
        throw IllegalStateException()
    }

    override fun getFramebuffer(): FrameBuffer {
        throw IllegalStateException()
    }

    override fun pixelTransfer() {
        throw IllegalStateException()
    }

    override fun drawColoredCircle(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
        var i_34 = i_3
        var i_41 = i_4
        var i_61 = i_6
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        if (i_34 > 0 && i_41 > 0) {
            val ints_9 = toolkit.colourBuffer ?: return
            var i_10 = 0
            var i_11 = 0
            val i_12 = toolkit.viewportWidth
            val i_13 = anInt8853 + anInt8851 + anInt8855
            val i_14 = anInt8854 + anInt8867 + anInt8856
            val i_15 = (i_13 shl 16) / i_34
            val i_16 = (i_14 shl 16) / i_41
            var i_17: Int
            if (anInt8853 > 0) {
                i_17 = (i_15 + (anInt8853 shl 16) - 1) / i_15
                i_1 + i_17
                i_10 += i_17 * i_15 - (anInt8853 shl 16)
            }

            if (anInt8854 > 0) {
                i_17 = (i_16 + (anInt8854 shl 16) - 1) / i_16
                i_2 + i_17
                i_11 += i_17 * i_16 - (anInt8854 shl 16)
            }

            if (anInt8851 < i_13) {
                i_34 = ((anInt8851 shl 16) - i_10 + i_15 - 1) / i_15
            }

            if (anInt8867 < i_14) {
                i_41 = ((anInt8867 shl 16) - i_11 + i_16 - 1) / i_16
            }

            var x = i_1
            var y = i_2
            if (anInt8853 > 0) {
                i_17 = (i_15 + (anInt8853 shl 16) - 1) / i_15
                x += i_17
            }
            if (anInt8854 > 0) {
                i_17 = (i_16 + (anInt8854 shl 16) - 1) / i_16
                y += i_17
            }

            i_17 = i_12 * y + x
            var i_18 = i_12 - i_34
            if (y + i_41 > toolkit.clipBottom) {
                i_41 -= y + i_41 - toolkit.clipBottom
            }

            var i_19: Int
            if (y < toolkit.clipTop) {
                i_19 = toolkit.clipTop - y
                i_41 -= i_19
                i_17 += i_19 * i_12
                i_11 += i_16 * i_19
            }

            if (i_34 + x > toolkit.clipRight) {
                i_19 = i_34 + x - toolkit.clipRight
                i_34 -= i_19
                i_18 += i_19
            }

            if (x < toolkit.clipLeft) {
                i_19 = toolkit.clipLeft - x
                i_34 -= i_19
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
            var b_34: Byte
            if (i_7 == 0) {
                if (i_5 == 1) {
                    i_19 = i_10

                    i_20 = -i_41
                    while (i_20 < 0) {
                        i_21 = anInt8851 * (i_11 shr 16)

                        i_22 = -i_34
                        while (i_22 < 0) {
                            ints_9[i_17++] = spritePalette[spritePixels[i_21 + (i_10 shr 16)].toInt() and 0xff]
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

                        i_25 = -i_34
                        while (i_25 < 0) {
                            i_26 = spritePalette[spritePixels[i_24 + (i_10 shr 16)].toInt() and 0xff]
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

                        i_22 = -i_34
                        while (i_22 < 0) {
                            b_34 = spritePixels[i_21 + (i_10 shr 16)]
                            i_24 = if (b_34 > 0) spritePalette[b_34.toInt()] else 0
                            i_25 = i_24 + i_61
                            i_26 = (i_24 and 0xff00ff) + (i_61 and 0xff00ff)
                            i_27 = (i_26 and 0x1000100) + (i_25 - i_26 and 0x10000)
                            ints_9[i_17++] = i_25 - i_27 or (i_27 - (i_27 ushr 8))
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

                        i_26 = -i_34
                        while (i_26 < 0) {
                            i_27 = spritePalette[spritePixels[i_25 + (i_10 shr 16)].toInt() and 0xff]
                            val i_21_temp = (i_27 and 0xff00ff) * i_19 and -16711936
                            val i_22_temp = i_19 * (i_27 and 0xff00) and 0xff0000
                            ints_9[i_17++] = ((i_21_temp or i_22_temp) ushr 8) + i_61
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
                var i_33: Int
                var b_37: Byte
                if (i_7 == 1) {
                    if (i_5 == 1) {
                        i_19 = i_10

                        i_20 = -i_41
                        while (i_20 < 0) {
                            i_21 = anInt8851 * (i_11 shr 16)

                            i_22 = -i_34
                            while (i_22 < 0) {
                                b_34 = spritePixels[i_21 + (i_10 shr 16)]
                                if (b_34.toInt() != 0) {
                                    ints_9[i_17++] = spritePalette[b_34.toInt() and 0xff]
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
                    } else {
                        var b_35: Byte
                        if (i_5 == 0) {
                            i_19 = i_10
                            if ((i_61 and 0xffffff) == 16777215) {
                                i_20 = i_61 ushr 24
                                i_21 = 256 - i_20

                                i_22 = -i_41
                                while (i_22 < 0) {
                                    i_23 = anInt8851 * (i_11 shr 16)

                                    i_24 = -i_34
                                    while (i_24 < 0) {
                                        b_35 = spritePixels[i_23 + (i_10 shr 16)]
                                        if (b_35.toInt() != 0) {
                                            i_26 = spritePalette[b_35.toInt() and 0xff]
                                            i_27 = ints_9[i_17]
                                            ints_9[i_17++] = ((i_26 and 0xff00ff) * i_20 + (i_27 and 0xff00ff) * i_21 and -16711936) + (i_20 * (i_26 and 0xff00) + i_21 * (i_27 and 0xff00) and 0xff0000) shr 8
                                        } else {
                                            ++i_17
                                        }

                                        i_10 += i_15
                                        i_24++
                                    }

                                    i_11 += i_16
                                    i_10 = i_19
                                    i_17 += i_18
                                    i_22++
                                }
                            } else {
                                i_20 = (i_61 and 0xff0000) shr 16
                                i_21 = (i_61 and 0xff00) shr 8
                                i_22 = i_61 and 0xff
                                i_23 = i_61 ushr 24
                                i_24 = 256 - i_23

                                i_25 = -i_41
                                while (i_25 < 0) {
                                    i_26 = anInt8851 * (i_11 shr 16)

                                    i_27 = -i_34
                                    while (i_27 < 0) {
                                        val b_36 = spritePixels[i_26 + (i_10 shr 16)]
                                        if (b_36.toInt() != 0) {
                                            i_29 = spritePalette[b_36.toInt() and 0xff]
                                            if (i_23 != 255) {
                                                i_30 = i_20 * (i_29 and 0xff0000) and 0xff000000.toInt()
                                                i_31 = i_21 * (i_29 and 0xff00) and 0xff0000
                                                i_32 = (i_29 and 0xff) * i_22 and 0xff00
                                                i_29 = (i_30 or i_31 or i_32) ushr 8
                                                i_33 = ints_9[i_17]
                                                ints_9[i_17++] = ((i_29 and 0xff00ff) * i_23 + (i_33 and 0xff00ff) * i_24 and -16711936) + (i_23 * (i_29 and 0xff00) + i_24 * (i_33 and 0xff00) and 0xff0000) shr 8
                                            } else {
                                                i_30 = i_20 * (i_29 and 0xff0000) and 0xff000000.toInt()
                                                i_31 = i_21 * (i_29 and 0xff00) and 0xff0000
                                                i_32 = (i_29 and 0xff) * i_22 and 0xff00
                                                ints_9[i_17++] = (i_30 or i_31 or i_32) ushr 8
                                            }
                                        } else {
                                            ++i_17
                                        }

                                        i_10 += i_15
                                        i_27++
                                    }

                                    i_11 += i_16
                                    i_10 = i_19
                                    i_17 += i_18
                                    i_25++
                                }
                            }
                        } else if (i_5 == 3) {
                            i_19 = i_10
                            i_20 = i_61 ushr 24
                            i_21 = 256 - i_20

                            i_22 = -i_41
                            while (i_22 < 0) {
                                i_23 = anInt8851 * (i_11 shr 16)

                                i_24 = -i_34
                                while (i_24 < 0) {
                                    b_35 = spritePixels[i_23 + (i_10 shr 16)]
                                    i_26 = if (b_35 > 0) spritePalette[b_35.toInt()] else 0
                                    i_27 = i_26 + i_61
                                    i_28 = (i_26 and 0xff00ff) + (i_61 and 0xff00ff)
                                    i_29 = (i_28 and 0x1000100) + (i_27 - i_28 and 0x10000)
                                    i_29 = i_27 - i_29 or (i_29 - (i_29 ushr 8))
                                    if (i_26 == 0 && i_20 != 255) {
                                        i_26 = i_29
                                        i_29 = ints_9[i_17]
                                        i_29 = ((i_26 and 0xff00ff) * i_20 + (i_29 and 0xff00ff) * i_21 and -16711936) + (i_20 * (i_26 and 0xff00) + i_21 * (i_29 and 0xff00) and 0xff0000) shr 8
                                    }

                                    ints_9[i_17++] = i_29
                                    i_10 += i_15
                                    i_24++
                                }

                                i_11 += i_16
                                i_10 = i_19
                                i_17 += i_18
                                i_22++
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

                                i_26 = -i_34
                                while (i_26 < 0) {
                                    b_37 = spritePixels[i_25 + (i_10 shr 16)]
                                    if (b_37.toInt() != 0) {
                                        i_28 = spritePalette[b_37.toInt() and 0xff]
                                        val i_21_temp = (i_28 and 0xff00ff) * i_19 and -16711936
                                        val i_22_temp = i_19 * (i_28 and 0xff00) and 0xff0000
                                        ints_9[i_17++] = ((i_21_temp or i_22_temp) ushr 8) + i_61
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
                } else {
                    if (i_7 != 2) {
                        throw IllegalArgumentException()
                    }

                    if (i_5 == 1) {
                        i_19 = i_10

                        i_20 = -i_41
                        while (i_20 < 0) {
                            i_21 = anInt8851 * (i_11 shr 16)

                            i_22 = -i_34
                            while (i_22 < 0) {
                                b_34 = spritePixels[i_21 + (i_10 shr 16)]
                                if (b_34.toInt() != 0) {
                                    i_24 = spritePalette[b_34.toInt() and 0xff]
                                    i_25 = ints_9[i_17]
                                    i_26 = i_24 + i_25
                                    i_27 = (i_24 and 0xff00ff) + (i_25 and 0xff00ff)
                                    i_25 = (i_27 and 0x1000100) + (i_26 - i_27 and 0x10000)
                                    ints_9[i_17++] = i_26 - i_25 or (i_25 - (i_25 ushr 8))
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

                            i_25 = -i_34
                            while (i_25 < 0) {
                                val b_38 = spritePixels[i_24 + (i_10 shr 16)]
                                if (b_38.toInt() != 0) {
                                    i_27 = spritePalette[b_38.toInt() and 0xff]
                                    i_28 = i_20 * (i_27 and 0xff0000) and 0xff000000.toInt()
                                    i_29 = i_21 * (i_27 and 0xff00) and 0xff0000
                                    i_30 = (i_27 and 0xff) * i_22 and 0xff00
                                    i_27 = (i_28 or i_29 or i_30) ushr 8
                                    i_31 = ints_9[i_17]
                                    i_32 = i_27 + i_31
                                    i_33 = (i_27 and 0xff00ff) + (i_31 and 0xff00ff)
                                    i_31 = (i_33 and 0x1000100) + (i_32 - i_33 and 0x10000)
                                    ints_9[i_17++] = i_32 - i_31 or (i_31 - (i_31 ushr 8))
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

                            i_22 = -i_34
                            while (i_22 < 0) {
                                b_34 = spritePixels[i_21 + (i_10 shr 16)]
                                i_24 = if (b_34 > 0) spritePalette[b_34.toInt()] else 0
                                i_25 = i_24 + i_61
                                i_26 = (i_24 and 0xff00ff) + (i_61 and 0xff00ff)
                                i_27 = (i_26 and 0x1000100) + (i_25 - i_26 and 0x10000)
                                i_24 = i_25 - i_27 or (i_27 - (i_27 ushr 8))
                                i_27 = ints_9[i_17]
                                i_25 = i_24 + i_27
                                i_26 = (i_24 and 0xff00ff) + (i_27 and 0xff00ff)
                                i_27 = (i_26 and 0x1000100) + (i_25 - i_26 and 0x10000)
                                ints_9[i_17++] = i_25 - i_27 or (i_27 - (i_27 ushr 8))
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

                            i_26 = -i_34
                            while (i_26 < 0) {
                                b_37 = spritePixels[i_25 + (i_10 shr 16)]
                                if (b_37.toInt() != 0) {
                                    i_28 = spritePalette[b_37.toInt() and 0xff]
                                    val i_21_temp = (i_28 and 0xff00ff) * i_19 and -16711936
                                    val i_22_temp = i_19 * (i_28 and 0xff00) and 0xff0000
                                    i_28 = ((i_21_temp or i_22_temp) ushr 8) + i_61
                                    i_29 = ints_9[i_17]
                                    i_30 = i_28 + i_29
                                    i_31 = (i_28 and 0xff00ff) + (i_29 and 0xff00ff)
                                    i_29 = (i_31 and 0x1000100) + (i_30 - i_31 and 0x10000)
                                    ints_9[i_17++] = i_30 - i_29 or (i_29 - (i_29 ushr 8))
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

    override fun draw(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int) {
        var i_27 = i_2
        var i_110 = i_1
        var i_41 = i_4
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        val ints_6 = toolkit.colourBuffer ?: return
        val i_7 = toolkit.viewportWidth
        i_110 += anInt8853
        i_27 += anInt8854
        var i_8 = i_7 * i_27 + i_110
        var i_9 = 0
        var i_10 = anInt8867
        var i_11 = anInt8851
        var i_12 = i_7 - i_11
        var i_13 = 0
        var i_14: Int
        if (i_27 < toolkit.clipTop) {
            i_14 = toolkit.clipTop - i_27
            i_10 -= i_14
            i_27 = toolkit.clipTop
            i_9 += i_14 * i_11
            i_8 += i_14 * i_7
        }

        if (i_10 + i_27 > toolkit.clipBottom) {
            i_10 -= i_10 + i_27 - toolkit.clipBottom
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
                            ints_6[i_8++] = spritePalette[spritePixels[i_9++].toInt() and 0xff]
                            ints_6[i_8++] = spritePalette[spritePixels[i_9++].toInt() and 0xff]
                            ints_6[i_8++] = spritePalette[spritePixels[i_9++].toInt() and 0xff]
                            ints_6[i_8++] = spritePalette[spritePixels[i_9++].toInt() and 0xff]
                        }

                        i_15 += 3
                        while (i_8 < i_15) {
                            ints_6[i_8++] = spritePalette[spritePixels[i_9++].toInt() and 0xff]
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
                            i_19 = spritePalette[spritePixels[i_9++].toInt() and 0xff]
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
                            i_16 = spritePalette[spritePixels[i_9++].toInt() and 0xff]
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
                            i_20 = spritePalette[spritePixels[i_9++].toInt() and 0xff]
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
                var i_26: Int
                var b_27: Byte
                var b_32: Byte
                if (i_5 == 1) {
                    if (i_3 == 1) {
                        i_14 = -i_10
                        while (i_14 < 0) {
                            i_15 = -i_11
                            while (i_15 < 0) {
                                b_27 = spritePixels[i_9++]
                                if (b_27.toInt() != 0) {
                                    i_17 = spritePalette[b_27.toInt() and 0xff] or 0xff000000.toInt()
                                    val s_28: Short = 255
                                    val b_30: Byte = 0
                                    i_20 = ints_6[i_8]
                                    ints_6[i_8++] = (((i_17 and -16711936) ushr 8) * s_28 + ((i_20 and -16711936) ushr 8) * b_30 and -16711936) + (((i_17 and 0xff00ff) * s_28 + (i_20 and 0xff00ff) * b_30 and -16711936) shr 8)
                                } else {
                                    ++i_8
                                }
                                i_15++
                            }

                            i_8 += i_12
                            i_9 += i_13
                            i_14++
                        }
                    } else {
                        var b_29: Byte
                        if (i_3 == 0) {
                            if ((i_41 and 0xffffff) == 16777215) {
                                i_14 = i_41 ushr 24
                                i_15 = 256 - i_14

                                i_16 = -i_10
                                while (i_16 < 0) {
                                    i_17 = -i_11
                                    while (i_17 < 0) {
                                        b_29 = spritePixels[i_9++]
                                        if (b_29.toInt() != 0) {
                                            i_19 = spritePalette[b_29.toInt() and 0xff]
                                            i_20 = ints_6[i_8]
                                            ints_6[i_8++] = ((i_19 and 0xff00ff) * i_14 + (i_20 and 0xff00ff) * i_15 and -16711936) + (i_14 * (i_19 and 0xff00) + i_15 * (i_20 and 0xff00) and 0xff0000) shr 8
                                        } else {
                                            ++i_8
                                        }
                                        i_17++
                                    }

                                    i_8 += i_12
                                    i_9 += i_13
                                    i_16++
                                }
                            } else {
                                i_14 = (i_41 and 0xff0000) shr 16
                                i_15 = (i_41 and 0xff00) shr 8
                                i_16 = i_41 and 0xff
                                i_17 = i_41 ushr 24
                                i_18 = 256 - i_17

                                i_19 = -i_10
                                while (i_19 < 0) {
                                    i_20 = -i_11
                                    while (i_20 < 0) {
                                        val b_31 = spritePixels[i_9++]
                                        if (b_31.toInt() != 0) {
                                            i_22 = spritePalette[b_31.toInt() and 0xff]
                                            if (i_17 != 255) {
                                                i_23 = i_14 * (i_22 and 0xff0000) and 0xff000000.toInt()
                                                i_24 = i_15 * (i_22 and 0xff00) and 0xff0000
                                                i_25 = (i_22 and 0xff) * i_16 and 0xff00
                                                i_22 = (i_23 or i_24 or i_25) ushr 8
                                                i_26 = ints_6[i_8]
                                                ints_6[i_8++] = ((i_22 and 0xff00ff) * i_17 + (i_26 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_22 and 0xff00) + i_18 * (i_26 and 0xff00) and 0xff0000) shr 8
                                            } else {
                                                i_23 = i_14 * (i_22 and 0xff0000) and 0xff000000.toInt()
                                                i_24 = i_15 * (i_22 and 0xff00) and 0xff0000
                                                i_25 = (i_22 and 0xff) * i_16 and 0xff00
                                                ints_6[i_8++] = (i_23 or i_24 or i_25) ushr 8
                                            }
                                        } else {
                                            ++i_8
                                        }
                                        i_20++
                                    }

                                    i_8 += i_12
                                    i_9 += i_13
                                    i_19++
                                }
                            }
                        } else if (i_3 == 3) {
                            i_14 = i_41 ushr 24
                            i_15 = 256 - i_14

                            i_16 = -i_10
                            while (i_16 < 0) {
                                i_17 = -i_11
                                while (i_17 < 0) {
                                    b_29 = spritePixels[i_9++]
                                    i_19 = if (b_29 > 0) spritePalette[b_29.toInt()] else 0
                                    i_20 = i_19 + i_41
                                    i_21 = (i_19 and 0xff00ff) + (i_41 and 0xff00ff)
                                    i_22 = (i_21 and 0x1000100) + (i_20 - i_21 and 0x10000)
                                    i_22 = i_20 - i_22 or (i_22 - (i_22 ushr 8))
                                    if (i_19 == 0 && i_14 != 255) {
                                        i_19 = i_22
                                        i_22 = ints_6[i_8]
                                        i_22 = ((i_19 and 0xff00ff) * i_14 + (i_22 and 0xff00ff) * i_15 and -16711936) + (i_14 * (i_19 and 0xff00) + i_15 * (i_22 and 0xff00) and 0xff0000) shr 8
                                    }

                                    ints_6[i_8++] = i_22
                                    i_17++
                                }

                                i_8 += i_12
                                i_9 += i_13
                                i_16++
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
                                    b_32 = spritePixels[i_9++]
                                    if (b_32.toInt() != 0) {
                                        i_21 = spritePalette[b_32.toInt() and 0xff]
                                        i_16 = (i_21 and 0xff00ff) * i_14 and -16711936
                                        i_17 = i_14 * (i_21 and 0xff00) and 0xff0000
                                        ints_6[i_8++] = i_41 + ((i_16 or i_17) ushr 8)
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
                } else {
                    if (i_5 != 2) {
                        throw IllegalArgumentException()
                    }

                    if (i_3 == 1) {
                        i_14 = -i_10
                        while (i_14 < 0) {
                            i_15 = -i_11
                            while (i_15 < 0) {
                                b_27 = spritePixels[i_9++]
                                if (b_27.toInt() != 0) {
                                    i_17 = spritePalette[b_27.toInt() and 0xff]
                                    i_18 = ints_6[i_8]
                                    i_19 = i_17 + i_18
                                    i_20 = (i_17 and 0xff00ff) + (i_18 and 0xff00ff)
                                    i_18 = (i_20 and 0x1000100) + (i_19 - i_20 and 0x10000)
                                    ints_6[i_8++] = i_19 - i_18 or (i_18 - (i_18 ushr 8))
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
                                val b_33 = spritePixels[i_9++]
                                if (b_33.toInt() != 0) {
                                    i_20 = spritePalette[b_33.toInt() and 0xff]
                                    i_21 = i_14 * (i_20 and 0xff0000) and 0xff000000.toInt()
                                    i_22 = i_15 * (i_20 and 0xff00) and 0xff0000
                                    i_23 = (i_20 and 0xff) * i_16 and 0xff00
                                    i_20 = (i_21 or i_22 or i_23) ushr 8
                                    i_24 = ints_6[i_8]
                                    i_25 = i_20 + i_24
                                    i_26 = (i_20 and 0xff00ff) + (i_24 and 0xff00ff)
                                    i_24 = (i_26 and 0x1000100) + (i_25 - i_26 and 0x10000)
                                    ints_6[i_8++] = i_25 - i_24 or (i_24 - (i_24 ushr 8))
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
                                b_27 = spritePixels[i_9++]
                                i_17 = if (b_27 > 0) spritePalette[b_27.toInt()] else 0
                                i_18 = i_17 + i_41
                                i_19 = (i_17 and 0xff00ff) + (i_41 and 0xff00ff)
                                i_20 = (i_19 and 0x1000100) + (i_18 - i_19 and 0x10000)
                                i_17 = i_18 - i_20 or (i_20 - (i_20 ushr 8))
                                i_20 = ints_6[i_8]
                                i_18 = i_17 + i_20
                                i_19 = (i_17 and 0xff00ff) + (i_20 and 0xff00ff)
                                i_20 = (i_19 and 0x1000100) + (i_18 - i_19 and 0x10000)
                                ints_6[i_8++] = i_18 - i_20 or (i_20 - (i_20 ushr 8))
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
                                b_32 = spritePixels[i_9++]
                                if (b_32.toInt() != 0) {
                                    i_21 = spritePalette[b_32.toInt() and 0xff]
                                    i_16 = (i_21 and 0xff00ff) * i_14 and -16711936
                                    i_17 = i_14 * (i_21 and 0xff00) and 0xff0000
                                    i_21 = i_41 + ((i_16 or i_17) ushr 8)
                                    i_22 = ints_6[i_8]
                                    i_23 = i_21 + i_22
                                    i_24 = (i_21 and 0xff00ff) + (i_22 and 0xff00ff)
                                    i_22 = (i_24 and 0x1000100) + (i_23 - i_24 and 0x10000)
                                    ints_6[i_8++] = i_23 - i_22 or (i_22 - (i_22 ushr 8))
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

    override fun method14247(
        bool1: Boolean,
        bool2: Boolean,
        bool3: Boolean,
        i_4: Int,
        i_5: Int,
        f_6: Float,
        i_7: Int,
        i_8: Int,
        blendType: Int,
        i_10: Int,
        i_11: Int,
        bool13: Boolean
    ) {
        var i_4Var = i_4
        var i_5Var = i_5
        var i_71 = i_7
        var i_81 = i_8
        var i_101 = i_10
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
            var b_40: Byte
            if (i_11 == 0) {
                if (blendType == 1) {
                    i_24 = i_14

                    i_25 = -i_81
                    while (i_25 < 0) {
                        i_26 = anInt8851 * (i_15 shr 16)

                        i_27 = -i_71
                        while (i_27 < 0) {
                            if (!bool2 || f_6 < floats_39!![i_20]) {
                                if (bool1) {
                                    ints_23!![i_20] = spritePalette[spritePixels[i_26 + (i_14 shr 16)].toInt() and 0xff]
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
                } else if (blendType == 0) {
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
                                    i_31 = spritePalette[spritePixels[i_29 + (i_14 shr 16)].toInt() and 0xff]
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
                } else if (blendType == 3) {
                    i_24 = i_14

                    i_25 = -i_81
                    while (i_25 < 0) {
                        i_26 = anInt8851 * (i_15 shr 16)

                        i_27 = -i_71
                        while (i_27 < 0) {
                            if (!bool2 || f_6 < floats_39!![i_20]) {
                                if (bool1) {
                                    b_40 = spritePixels[i_26 + (i_14 shr 16)]
                                    i_29 = if (b_40 > 0) spritePalette[b_40.toInt()] else 0
                                    i_30 = i_101 + i_29
                                    i_31 = (i_29 and 0xff00ff) + (i_101 and 0xff00ff)
                                    i_32 = (i_31 and 0x1000100) + (i_30 - i_31 and 0x10000)
                                    ints_23!![i_20] = i_30 - i_32 or (i_32 - (i_32 ushr 8))
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
                    if (blendType != 2) {
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
                                    i_32 = spritePalette[spritePixels[i_30 + (i_14 shr 16)].toInt() and 0xff]
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
                var i_38: Int
                var b_43: Byte
                if (i_11 == 1) {
                    if (blendType == 1) {
                        i_24 = i_14

                        i_25 = -i_81
                        while (i_25 < 0) {
                            i_26 = anInt8851 * (i_15 shr 16)

                            i_27 = -i_71
                            while (i_27 < 0) {
                                if (!bool2 || f_6 < floats_39!![i_20]) {
                                    b_40 = spritePixels[i_26 + (i_14 shr 16)]
                                    if (b_40.toInt() != 0) {
                                        if (bool1) {
                                            ints_23!![i_20] = spritePalette[b_40.toInt() and 0xff]
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
                    } else {
                        var b_41: Byte
                        if (blendType == 0) {
                            i_24 = i_14
                            if ((i_101 and 0xffffff) == 16777215) {
                                i_25 = i_101 ushr 24
                                i_26 = 256 - i_25

                                i_27 = -i_81
                                while (i_27 < 0) {
                                    i_28 = anInt8851 * (i_15 shr 16)

                                    i_29 = -i_71
                                    while (i_29 < 0) {
                                        if (!bool2 || f_6 < floats_39!![i_20]) {
                                            b_41 = spritePixels[i_28 + (i_14 shr 16)]
                                            if (b_41.toInt() != 0) {
                                                if (bool1) {
                                                    i_31 = spritePalette[b_41.toInt() and 0xff]
                                                    i_32 = ints_23!![i_20]
                                                    ints_23[i_20] = ((i_31 and 0xff00ff) * i_25 + (i_32 and 0xff00ff) * i_26 and -16711936) + (i_25 * (i_31 and 0xff00) + i_26 * (i_32 and 0xff00) and 0xff0000) shr 8
                                                }

                                                if (bool2 && bool13) {
                                                    floats_39!![i_20] = f_6
                                                }
                                            }
                                        }

                                        i_14 += i_18
                                        ++i_20
                                        i_29++
                                    }

                                    i_15 += i_19
                                    i_14 = i_24
                                    i_20 += i_21
                                    i_27++
                                }
                            } else {
                                i_25 = (i_101 and 0xff0000) shr 16
                                i_26 = (i_101 and 0xff00) shr 8
                                i_27 = i_101 and 0xff
                                i_28 = i_101 ushr 24
                                i_29 = 256 - i_28

                                i_30 = -i_81
                                while (i_30 < 0) {
                                    i_31 = anInt8851 * (i_15 shr 16)

                                    i_32 = -i_71
                                    while (i_32 < 0) {
                                        if (!bool2 || f_6 < floats_39!![i_20]) {
                                            val b_42 = spritePixels[i_31 + (i_14 shr 16)]
                                            if (b_42.toInt() != 0) {
                                                i_34 = spritePalette[b_42.toInt() and 0xff]
                                                if (i_28 != 255) {
                                                    if (bool1) {
                                                        i_35 = i_25 * (i_34 and 0xff0000) and 0xff000000.toInt()
                                                        i_36 = i_26 * (i_34 and 0xff00) and 0xff0000
                                                        i_37 = (i_34 and 0xff) * i_27 and 0xff00
                                                        i_34 = (i_35 or i_36 or i_37) ushr 8
                                                        i_38 = ints_23!![i_20]
                                                        ints_23[i_20] = ((i_34 and 0xff00ff) * i_28 + (i_38 and 0xff00ff) * i_29 and -16711936) + (i_28 * (i_34 and 0xff00) + i_29 * (i_38 and 0xff00) and 0xff0000) shr 8
                                                    }
                                                } else {
                                                    if (bool1) {
                                                        i_35 = i_25 * (i_34 and 0xff0000) and 0xff000000.toInt()
                                                        i_36 = i_26 * (i_34 and 0xff00) and 0xff0000
                                                        i_37 = (i_34 and 0xff) * i_27 and 0xff00
                                                        ints_23!![i_20] = (i_35 or i_36 or i_37) ushr 8
                                                    }
                                                }
                                                if (bool2 && bool13) {
                                                    floats_39!![i_20] = f_6
                                                }
                                            }
                                        }

                                        i_14 += i_18
                                        ++i_20
                                        i_32++
                                    }

                                    i_15 += i_19
                                    i_14 = i_24
                                    i_20 += i_21
                                    i_30++
                                }
                            }
                        } else if (blendType == 3) {
                            i_24 = i_14
                            i_25 = i_101 ushr 24
                            i_26 = 256 - i_25

                            i_27 = -i_81
                            while (i_27 < 0) {
                                i_28 = anInt8851 * (i_15 shr 16)

                                i_29 = -i_71
                                while (i_29 < 0) {
                                    if (!bool2 || f_6 < floats_39!![i_20]) {
                                        if (bool1) {
                                            b_41 = spritePixels[i_28 + (i_14 shr 16)]
                                            i_31 = if (b_41 > 0) spritePalette[b_41.toInt()] else 0
                                            i_32 = i_101 + i_31
                                            i_33 = (i_31 and 0xff00ff) + (i_101 and 0xff00ff)
                                            i_34 = (i_33 and 0x1000100) + (i_32 - i_33 and 0x10000)
                                            i_34 = i_32 - i_34 or (i_34 - (i_34 ushr 8))
                                            if (i_31 == 0 && i_25 != 255) {
                                                i_31 = i_34
                                                i_34 = ints_23!![i_20]
                                                i_34 = ((i_31 and 0xff00ff) * i_25 + (i_34 and 0xff00ff) * i_26 and -16711936) + (i_25 * (i_31 and 0xff00) + i_26 * (i_34 and 0xff00) and 0xff0000) shr 8
                                            }

                                            ints_23!![i_20] = i_34
                                        }

                                        if (bool2 && bool13) {
                                            floats_39!![i_20] = f_6
                                        }
                                    }

                                    i_14 += i_18
                                    ++i_20
                                    i_29++
                                }

                                i_15 += i_19
                                i_14 = i_24
                                i_20 += i_21
                                i_27++
                            }
                        } else {
                            if (blendType != 2) {
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
                                        b_43 = spritePixels[i_30 + (i_14 shr 16)]
                                        if (b_43.toInt() != 0) {
                                            if (bool1) {
                                                i_33 = spritePalette[b_43.toInt() and 0xff]
                                                i_26 = (i_33 and 0xff00ff) * i_24 and -16711936
                                                i_27 = i_24 * (i_33 and 0xff00) and 0xff0000
                                                ints_23!![i_20] = i_101 + ((i_26 or i_27) ushr 8)
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
                } else {
                    if (i_11 != 2) {
                        throw IllegalArgumentException()
                    }

                    if (blendType == 1) {
                        i_24 = i_14

                        i_25 = -i_81
                        while (i_25 < 0) {
                            i_26 = anInt8851 * (i_15 shr 16)

                            i_27 = -i_71
                            while (i_27 < 0) {
                                if (!bool2 || f_6 < floats_39!![i_20]) {
                                    b_40 = spritePixels[i_26 + (i_14 shr 16)]
                                    if (b_40.toInt() != 0) {
                                        if (bool1) {
                                            i_29 = spritePalette[b_40.toInt() and 0xff]
                                            i_30 = ints_23!![i_20]
                                            i_31 = i_29 + i_30
                                            i_32 = (i_29 and 0xff00ff) + (i_30 and 0xff00ff)
                                            i_30 = (i_32 and 0x1000100) + (i_31 - i_32 and 0x10000)
                                            ints_23[i_20] = i_31 - i_30 or (i_30 - (i_30 ushr 8))
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
                    } else if (blendType == 0) {
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
                                    val b_44 = spritePixels[i_29 + (i_14 shr 16)]
                                    if (b_44.toInt() != 0) {
                                        if (bool1) {
                                            i_32 = spritePalette[b_44.toInt() and 0xff]
                                            i_33 = i_25 * (i_32 and 0xff0000) and 0xff000000.toInt()
                                            i_34 = i_26 * (i_32 and 0xff00) and 0xff0000
                                            i_35 = (i_32 and 0xff) * i_27 and 0xff00
                                            i_32 = (i_33 or i_34 or i_35) ushr 8
                                            i_36 = ints_23!![i_20]
                                            i_37 = i_32 + i_36
                                            i_38 = (i_32 and 0xff00ff) + (i_36 and 0xff00ff)
                                            i_36 = (i_38 and 0x1000100) + (i_37 - i_38 and 0x10000)
                                            ints_23[i_20] = i_37 - i_36 or (i_36 - (i_36 ushr 8))
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
                    } else if (blendType == 3) {
                        i_24 = i_14

                        i_25 = -i_81
                        while (i_25 < 0) {
                            i_26 = anInt8851 * (i_15 shr 16)

                            i_27 = -i_71
                            while (i_27 < 0) {
                                if (!bool2 || f_6 < floats_39!![i_20]) {
                                    if (bool1) {
                                        b_40 = spritePixels[i_26 + (i_14 shr 16)]
                                        i_29 = if (b_40 > 0) spritePalette[b_40.toInt()] else 0
                                        i_30 = i_101 + i_29
                                        i_31 = (i_29 and 0xff00ff) + (i_101 and 0xff00ff)
                                        i_32 = (i_31 and 0x1000100) + (i_30 - i_31 and 0x10000)
                                        i_29 = i_30 - i_32 or (i_32 - (i_32 ushr 8))
                                        i_32 = ints_23!![i_20]
                                        i_30 = i_29 + i_32
                                        i_31 = (i_29 and 0xff00ff) + (i_32 and 0xff00ff)
                                        i_32 = (i_31 and 0x1000100) + (i_30 - i_31 and 0x10000)
                                        ints_23[i_20] = i_30 - i_32 or (i_32 - (i_32 ushr 8))
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
                        if (blendType != 2) {
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
                                    b_43 = spritePixels[i_30 + (i_14 shr 16)]
                                    if (b_43.toInt() != 0) {
                                        if (bool1) {
                                            i_33 = spritePalette[b_43.toInt() and 0xff]
                                            i_26 = (i_33 and 0xff00ff) * i_24 and -16711936
                                            i_27 = i_24 * (i_33 and 0xff00) and 0xff0000
                                            i_33 = i_101 + ((i_26 or i_27) ushr 8)
                                            i_34 = ints_23!![i_20]
                                            i_35 = i_33 + i_34
                                            i_36 = (i_33 and 0xff00ff) + (i_34 and 0xff00ff)
                                            i_34 = (i_36 and 0x1000100) + (i_35 - i_36 and 0x10000)
                                            ints_23[i_20] = i_35 - i_34 or (i_34 - (i_34 ushr 8))
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

    override fun method2773(i_1: Int, i_2: Int, class4553: ComponentSurface, i_4: Int, i_5: Int) {
        var i_25 = i_2
        var i_15 = i_1
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        val ints_6 = toolkit.colourBuffer ?: return
        i_15 += anInt8853
        i_25 += anInt8854
        var i_7 = 0
        val i_8 = toolkit.viewportWidth
        var i_9 = anInt8851
        var i_10 = anInt8867
        var i_11 = i_8 - i_9
        var i_12 = 0
        var i_13 = i_8 * i_25 + i_15
        var i_14: Int
        if (i_25 < toolkit.clipTop) {
            i_14 = toolkit.clipTop - i_25
            i_10 -= i_14
            i_25 = toolkit.clipTop
            i_7 += i_14 * i_9
            i_13 += i_14 * i_8
        }

        if (i_10 + i_25 > toolkit.clipBottom) {
            i_10 -= i_10 + i_25 - toolkit.clipBottom
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
            val packetsdecoder26 = class4553 as ComponentDimensionSM
            val ints_15 = packetsdecoder26.anIntArray9077
            val ints_16 = packetsdecoder26.anIntArray9078
            var i_17 = i_25
            if (i_5 > i_25) {
                i_17 = i_5
                i_13 += i_8 * (i_5 - i_25)
                i_7 += (i_5 - i_25) * anInt8851
            }

            val i_18 = minOf(ints_15.size + i_5, i_10 + i_25)

            for (i_19 in i_17 until i_18) {
                val i_20 = ints_15[i_19 - i_5] + i_4
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

                var i_24 = -i_21
                while (i_24 < 0) {
                    val b_25 = spritePixels[i_7++]
                    if (b_25.toInt() != 0) {
                        ints_6[i_13++] = spritePalette[b_25.toInt() and 0xff]
                    } else {
                        ++i_13
                    }
                    i_24++
                }

                i_7 += i_23 + i_12
                i_13 += i_23 + i_11
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
                            renderPixel14245(ints_3, i_10, i_9, i_1, i_2)
                            i_8++
                        }
                    }
                    anInt8870 += anInt8850
                    i_4++
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
                            renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                            i_7 += anInt8868
                            i_8++
                        }
                    }
                    anInt8870 += anInt8850
                    i_4++
                    anInt8873 += anInt8869
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
                            renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                            i_7 += anInt8868
                            i_8++
                        }
                    }
                    anInt8870 += anInt8850
                    i_4++
                    anInt8873 += anInt8869
                }
            }
        } else if (anInt8865 < 0) {
            method14245NegX(ints_3, i_1, i_2)
        } else {
            method14245PosX(ints_3, i_1, i_2)
        }
    }

    private fun method14245NegX(ints_3: IntArray, i_1: Int, i_2: Int) {
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
                        renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                        i_6 += anInt8865
                        i_8++
                    }
                }
                anInt8870 += anInt8850
                i_4++
                anInt8874 += anInt8876
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
                    renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                    i_6 += anInt8865
                    i_7 += anInt8868
                    i_8++
                }
                anInt8870 += anInt8850
                i_4++
                anInt8873 += anInt8869
                anInt8874 += anInt8876
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
                    renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                    i_6 += anInt8865
                    i_7 += anInt8868
                    i_8++
                }
                anInt8870 += anInt8850
                i_4++
                anInt8873 += anInt8869
                anInt8874 += anInt8876
            }
        }
    }

    private fun method14245PosX(ints_3: IntArray, i_1: Int, i_2: Int) {
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
                        renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                        i_6 += anInt8865
                        i_8++
                    }
                }
                anInt8870 += anInt8850
                i_4++
                anInt8874 += anInt8876
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
                    renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                    i_6 += anInt8865
                    i_7 += anInt8868
                    i_8++
                }
                anInt8870 += anInt8850
                i_4++
                anInt8873 += anInt8869
                anInt8874 += anInt8876
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
                    renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                    i_6 += anInt8865
                    i_7 += anInt8868
                    i_8++
                }
                anInt8870 += anInt8850
                i_4++
                anInt8873 += anInt8869
                anInt8874 += anInt8876
            }
        }
    }

    private fun renderPixel14245(ints_3: IntArray, destIdx: Int, srcIdx: Int, blendMode: Int, transparencyMode: Int) {
        var i_11: Int
        var i_12: Int
        var i_13: Int
        var i_14: Int
        var i_15: Int
        var i_16: Int
        var i_17: Int
        var i_18: Int
        val b_src = spritePixels[srcIdx]
        if (transparencyMode == 0) {
            if (blendMode == 1) {
                ints_3[destIdx] = spritePalette[b_src.toInt() and 0xff]
            } else if (blendMode == 0) {
                i_11 = spritePalette[b_src.toInt() and 0xff]
                i_12 = anInt8880 * (i_11 and 0xff0000) and 0xff000000.toInt()
                i_13 = (i_11 and 0xff00) * anInt8852 and 0xff0000
                i_14 = (i_11 and 0xff) * anInt8882 and 0xff00
                ints_3[destIdx] = (i_12 or i_13 or i_14) ushr 8
            } else if (blendMode == 3) {
                i_11 = spritePalette[b_src.toInt() and 0xff]
                i_12 = anInt8877
                i_13 = i_11 + i_12
                i_14 = (i_11 and 0xff00ff) + (i_12 and 0xff00ff)
                i_15 = (i_14 and 0x1000100) + (i_13 - i_14 and 0x10000)
                ints_3[destIdx] = i_13 - i_15 or (i_15 - (i_15 ushr 8))
            } else {
                if (blendMode != 2) {
                    throw IllegalArgumentException()
                }
                i_11 = spritePalette[b_src.toInt() and 0xff]
                i_12 = (i_11 and 0xff00ff) * anInt8878 and -16711936
                i_13 = (i_11 and 0xff00) * anInt8878 and 0xff0000
                ints_3[destIdx] = ((i_12 or i_13) ushr 8) + anInt8885
            }
        } else if (transparencyMode == 1) {
            if (blendMode == 1) {
                if (b_src.toInt() != 0) {
                    ints_3[destIdx] = spritePalette[b_src.toInt() and 0xff]
                }
            } else if (blendMode == 0) {
                if (b_src.toInt() != 0) {
                    i_11 = spritePalette[b_src.toInt() and 0xff]
                    if ((anInt8877 and 0xffffff) == 16777215) {
                        i_12 = anInt8877 ushr 24
                        i_13 = 256 - i_12
                        i_14 = ints_3[destIdx]
                        ints_3[destIdx] = ((i_11 and 0xff00ff) * i_12 + (i_14 and 0xff00ff) * i_13 and -16711936) + (i_12 * (i_11 and 0xff00) + i_13 * (i_14 and 0xff00) and 0xff0000) shr 8
                    } else if (anInt8878 != 255) {
                        i_12 = anInt8880 * (i_11 and 0xff0000) and 0xff000000.toInt()
                        i_13 = (i_11 and 0xff00) * anInt8852 and 0xff0000
                        i_14 = (i_11 and 0xff) * anInt8882 and 0xff00
                        i_11 = (i_12 or i_13 or i_14) ushr 8
                        i_15 = ints_3[destIdx]
                        ints_3[destIdx] = ((i_11 and 0xff00ff) * anInt8878 + (i_15 and 0xff00ff) * anInt8879 and -16711936) + ((i_11 and 0xff00) * anInt8878 + (i_15 and 0xff00) * anInt8879 and 0xff0000) shr 8
                    } else {
                        i_12 = anInt8880 * (i_11 and 0xff0000) and 0xff000000.toInt()
                        i_13 = (i_11 and 0xff00) * anInt8852 and 0xff0000
                        i_14 = (i_11 and 0xff) * anInt8882 and 0xff00
                        ints_3[destIdx] = (i_12 or i_13 or i_14) ushr 8
                    }
                }
            } else if (blendMode == 3) {
                i_11 = if (b_src > 0) spritePalette[b_src.toInt()] else 0
                i_12 = anInt8877
                i_13 = i_11 + i_12
                i_14 = (i_11 and 0xff00ff) + (i_12 and 0xff00ff)
                i_15 = (i_14 and 0x1000100) + (i_13 - i_14 and 0x10000)
                i_15 = i_13 - i_15 or (i_15 - (i_15 ushr 8))
                if (i_11 == 0 && anInt8878 != 255) {
                    i_11 = i_15
                    i_15 = ints_3[destIdx]
                    i_15 = ((i_11 and 0xff00ff) * anInt8878 + (i_15 and 0xff00ff) * anInt8879 and -16711936) + ((i_11 and 0xff00) * anInt8878 + (i_15 and 0xff00) * anInt8879 and 0xff0000) shr 8
                }
                ints_3[destIdx] = i_15
            } else {
                if (blendMode != 2) {
                    throw IllegalArgumentException()
                }
                if (b_src.toInt() != 0) {
                    i_11 = spritePalette[b_src.toInt() and 0xff]
                    i_12 = (i_11 and 0xff00ff) * anInt8878 and -16711936
                    i_13 = (i_11 and 0xff00) * anInt8878 and 0xff0000
                    ints_3[destIdx] = ((i_12 or i_13) ushr 8) + anInt8885
                }
            }
        } else {
            if (transparencyMode != 2) {
                throw IllegalArgumentException()
            }
            if (blendMode == 1) {
                if (b_src.toInt() != 0) {
                    i_11 = spritePalette[b_src.toInt() and 0xff]
                    i_12 = ints_3[destIdx]
                    i_13 = i_11 + i_12
                    i_14 = (i_11 and 0xff00ff) + (i_12 and 0xff00ff)
                    i_12 = (i_14 and 0x1000100) + (i_13 - i_14 and 0x10000)
                    ints_3[destIdx] = i_13 - i_12 or (i_12 - (i_12 ushr 8))
                }
            } else if (blendMode == 0) {
                if (b_src.toInt() != 0) {
                    i_11 = spritePalette[b_src.toInt() and 0xff]
                    i_12 = anInt8880 * (i_11 and 0xff0000) and 0xff000000.toInt()
                    i_13 = (i_11 and 0xff00) * anInt8852 and 0xff0000
                    i_14 = (i_11 and 0xff) * anInt8882 and 0xff00
                    i_11 = (i_12 or i_13 or i_14) ushr 8
                    i_15 = ints_3[destIdx]
                    i_16 = i_11 + i_15
                    i_17 = (i_11 and 0xff00ff) + (i_15 and 0xff00ff)
                    i_15 = (i_17 and 0x1000100) + (i_16 - i_17 and 0x10000)
                    ints_3[destIdx] = i_16 - i_15 or (i_15 - (i_15 ushr 8))
                }
            } else if (blendMode == 3) {
                i_11 = if (b_src > 0) spritePalette[b_src.toInt()] else 0
                i_12 = anInt8877
                i_13 = i_11 + i_12
                i_14 = (i_11 and 0xff00ff) + (i_12 and 0xff00ff)
                i_15 = (i_14 and 0x1000100) + (i_13 - i_14 and 0x10000)
                i_11 = i_13 - i_15 or (i_15 - (i_15 ushr 8))
                i_15 = ints_3[destIdx]
                i_13 = i_11 + i_15
                i_14 = (i_11 and 0xff00ff) + (i_15 and 0xff00ff)
                i_15 = (i_14 and 0x1000100) + (i_13 - i_14 and 0x10000)
                ints_3[destIdx] = i_13 - i_15 or (i_15 - (i_15 ushr 8))
            } else if (blendMode == 2) {
                if (b_src.toInt() != 0) {
                    i_11 = spritePalette[b_src.toInt() and 0xff]
                    i_12 = (i_11 and 0xff00ff) * anInt8878 and -16711936
                    i_13 = (i_11 and 0xff00) * anInt8878 and 0xff0000
                    i_11 = ((i_12 or i_13) ushr 8) + anInt8885
                    i_14 = ints_3[destIdx]
                    i_15 = i_11 + i_14
                    i_16 = (i_11 and 0xff00ff) + (i_14 and 0xff00ff)
                    i_14 = (i_16 and 0x1000100) + (i_15 - i_16 and 0x10000)
                    ints_3[destIdx] = i_15 - i_14 or (i_14 - (i_14 ushr 8))
                }
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
        var b_16: Byte
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
                                b_16 = spritePixels[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                                if (b_16.toInt() != 0) {
                                    ints_5[i_8++] = spritePalette[b_16.toInt() and 0xff]
                                } else {
                                    ++i_8
                                }
                                i_11++
                            }
                        }
                    }
                    anInt8870 += anInt8850
                    i_6++
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
                                b_16 = spritePixels[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                                if (b_16.toInt() != 0) {
                                    ints_5[i_8++] = spritePalette[b_16.toInt() and 0xff]
                                } else {
                                    ++i_8
                                }
                                i_10 += anInt8868
                                i_11++
                            }
                        }
                    }
                    anInt8870 += anInt8850
                    i_6++
                    anInt8873 += anInt8869
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
                                b_16 = spritePixels[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                                if (b_16.toInt() != 0) {
                                    ints_5[i_8++] = spritePalette[b_16.toInt() and 0xff]
                                } else {
                                    ++i_8
                                }
                                i_10 += anInt8868
                                i_11++
                            }
                        }
                    }
                    anInt8870 += anInt8850
                    i_6++
                    anInt8873 += anInt8869
                }
            }
        } else if (anInt8865 < 0) {
            method14250NegX(ints_5, ints_1, ints_2, i_3, i_4)
        } else {
            method14250PosX(ints_5, ints_1, ints_2, i_3, i_4)
        }
    }

    private fun method14250NegX(ints_5: IntArray, ints_1: IntArray, ints_2: IntArray, i_3: Int, i_4: Int) {
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
        var b_16: Byte
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
                            b_16 = spritePixels[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                            if (b_16.toInt() != 0) {
                                ints_5[i_8++] = spritePalette[b_16.toInt() and 0xff]
                            } else {
                                ++i_8
                            }
                            i_9 += anInt8865
                            i_11++
                        }
                    }
                }
                anInt8870 += anInt8850
                i_6++
                anInt8874 += anInt8876
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
                        b_16 = spritePixels[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                        if (b_16.toInt() != 0) {
                            ints_5[i_8++] = spritePalette[b_16.toInt() and 0xff]
                        } else {
                            ++i_8
                        }
                        i_9 += anInt8865
                        i_10 += anInt8868
                        i_11++
                    }
                }
                anInt8870 += anInt8850
                i_6++
                anInt8873 += anInt8869
                anInt8874 += anInt8876
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
                        b_16 = spritePixels[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                        if (b_16.toInt() != 0) {
                            ints_5[i_8++] = spritePalette[b_16.toInt() and 0xff]
                        } else {
                            ++i_8
                        }
                        i_9 += anInt8865
                        i_10 += anInt8868
                        i_11++
                    }
                }
                anInt8870 += anInt8850
                i_6++
                anInt8873 += anInt8869
                anInt8874 += anInt8876
            }
        }
    }

    private fun method14250PosX(ints_5: IntArray, ints_1: IntArray, ints_2: IntArray, i_3: Int, i_4: Int) {
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
        var b_16: Byte
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
                            b_16 = spritePixels[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                            if (b_16.toInt() != 0) {
                                ints_5[i_8++] = spritePalette[b_16.toInt() and 0xff]
                            } else {
                                ++i_8
                            }
                            i_9 += anInt8865
                            i_11++
                        }
                    }
                }
                anInt8870 += anInt8850
                i_6++
                anInt8873 += anInt8869
                anInt8874 += anInt8876
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
                        b_16 = spritePixels[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                        if (b_16.toInt() != 0) {
                            ints_5[i_8++] = spritePalette[b_16.toInt() and 0xff]
                        } else {
                            ++i_8
                        }
                        i_9 += anInt8865
                        i_10 += anInt8868
                        i_11++
                    }
                }
                anInt8870 += anInt8850
                i_6++
                anInt8873 += anInt8869
                anInt8874 += anInt8876
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
                        b_16 = spritePixels[(i_9 shr 12) + (i_10 shr 12) * anInt8851]
                        if (b_16.toInt() != 0) {
                            ints_5[i_8++] = spritePalette[b_16.toInt() and 0xff]
                        } else {
                            ++i_8
                        }
                        i_9 += anInt8865
                        i_10 += anInt8868
                        i_11++
                    }
                }
                anInt8870 += anInt8850
                i_6++
                anInt8873 += anInt8869
                anInt8874 += anInt8876
            }
        }
    }
}
