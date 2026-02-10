package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.framebuffer.SafeModeFrameBuffer
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import com.jagex.game.runetek5.toolkit.ComponentDimensionSM

class OpaqueSpriteSM : SpriteSM {

    @JvmField
    val pixels: IntArray

    constructor(hardwarerenderer1: SafeModeToolkit, ints_2: IntArray, i_3: Int, i_4: Int, i_5: Int, i_6: Int, bool7: Boolean) : super(hardwarerenderer1, i_5, i_6) {
        if (bool7) {
            this.pixels = IntArray(i_5 * i_6)
        } else {
            this.pixels = ints_2
        }

        var adjustedI4 = i_4 - imageWidth
        var i_8 = 0
        var i_3Var = i_3

        for (i_9 in 0 until i_6) {
            for (i_10 in 0 until i_5) {
                val i_11 = ints_2[i_3Var++]
                if (i_11 ushr 24 == 255) {
                    this.pixels[i_8++] = if ((i_11 and 0xffffff) == 0) -16777215 else i_11
                } else {
                    this.pixels[i_8++] = 0
                }
            }
            i_3Var += adjustedI4
        }
    }

    constructor(hardwarerenderer1: SafeModeToolkit, i_2: Int, i_3: Int) : super(hardwarerenderer1, i_2, i_3) {
        this.pixels = IntArray(i_3 * i_2)
    }

    constructor(hardwarerenderer1: SafeModeToolkit, ints_2: IntArray, i_3: Int, i_4: Int) : super(hardwarerenderer1, i_3, i_4) {
        this.pixels = ints_2
    }

    override fun captureScreenRegion(i: Int, i151: Int, i152: Int, i153: Int, i154: Int, i155: Int) {
        val ints = toolkit.colourBuffer ?: throw IllegalStateException("")
        for (i156 in 0 until i153) {
            val i157 = (i151 + i156) * imageWidth + i
            val i158 = (i155 + i156) * toolkit.viewportWidth + i154
            if (i152 >= 0) {
                System.arraycopy(ints, i158 + 0, this.pixels, i157 + 0, i152)
            }
        }
    }

    override fun getFramebuffer(): FrameBuffer {
        return SafeModeFrameBuffer(imageWidth, imageHeight, this.pixels)
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
        i_110 += offsetX
        i_26 += offsetY
        var i_8 = i_7 * i_26 + i_110
        var i_9 = 0
        var i_10 = imageHeight
        var i_11 = imageWidth
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
                            ints_6[i_8++] = this.pixels[i_9++]
                            ints_6[i_8++] = this.pixels[i_9++]
                            ints_6[i_8++] = this.pixels[i_9++]
                            ints_6[i_8++] = this.pixels[i_9++]
                        }

                        i_15 += 3
                        while (i_8 < i_15) {
                            ints_6[i_8++] = this.pixels[i_9++]
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
                            i_19 = this.pixels[i_9++]
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
                            i_16 = this.pixels[i_9++]
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
                            i_20 = this.pixels[i_9++]
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
                            i_15 = i_8 + i_11 - 3

                            while (i_8 < i_15) {
                                i_16 = this.pixels[i_9++]
                                if (i_16 != 0) {
                                    ints_6[i_8++] = i_16
                                } else {
                                    ++i_8
                                }

                                i_16 = this.pixels[i_9++]
                                if (i_16 != 0) {
                                    ints_6[i_8++] = i_16
                                } else {
                                    ++i_8
                                }

                                i_16 = this.pixels[i_9++]
                                if (i_16 != 0) {
                                    ints_6[i_8++] = i_16
                                } else {
                                    ++i_8
                                }

                                i_16 = this.pixels[i_9++]
                                if (i_16 != 0) {
                                    ints_6[i_8++] = i_16
                                } else {
                                    ++i_8
                                }
                            }

                            i_15 += 3

                            while (i_8 < i_15) {
                                i_16 = this.pixels[i_9++]
                                if (i_16 != 0) {
                                    ints_6[i_8++] = i_16
                                } else {
                                    ++i_8
                                }
                            }

                            i_8 += i_12
                            i_9 += i_13
                            i_14++
                        }
                    } else if (i_3 == 0) {
                        if ((i_41 and 0xffffff) == 16777215) {
                            i_14 = i_41 ushr 24
                            i_15 = 256 - i_14

                            i_16 = -i_10
                            while (i_16 < 0) {
                                i_17 = -i_11
                                while (i_17 < 0) {
                                    i_18 = this.pixels[i_9++]
                                    if (i_18 != 0) {
                                        i_19 = ints_6[i_8]
                                        ints_6[i_8++] = ((i_18 and 0xff00ff) * i_14 + (i_19 and 0xff00ff) * i_15 and -16711936) + (i_14 * (i_18 and 0xff00) + i_15 * (i_19 and 0xff00) and 0xff0000) shr 8
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
                                    i_21 = this.pixels[i_9++]
                                    if (i_21 != 0) {
                                        if (i_17 != 255) {
                                            i_22 = i_14 * (i_21 and 0xff0000) and 0xff000000.toInt()
                                            i_23 = i_15 * (i_21 and 0xff00) and 0xff0000
                                            i_24 = (i_21 and 0xff) * i_16 and 0xff00
                                            i_21 = (i_22 or i_23 or i_24) ushr 8
                                            i_25 = ints_6[i_8]
                                            ints_6[i_8++] = ((i_21 and 0xff00ff) * i_17 + (i_25 and 0xff00ff) * i_18 and -16711936) + (i_17 * (i_21 and 0xff00) + i_18 * (i_25 and 0xff00) and 0xff0000) shr 8
                                        } else {
                                            i_22 = i_14 * (i_21 and 0xff0000) and 0xff000000.toInt()
                                            i_23 = i_15 * (i_21 and 0xff00) and 0xff0000
                                            i_24 = (i_21 and 0xff) * i_16 and 0xff00
                                            ints_6[i_8++] = (i_22 or i_23 or i_24) ushr 8
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
                                i_18 = this.pixels[i_9++]
                                i_19 = i_18 + i_41
                                i_20 = (i_18 and 0xff00ff) + (i_41 and 0xff00ff)
                                i_21 = (i_20 and 0x1000100) + (i_19 - i_20 and 0x10000)
                                i_21 = i_19 - i_21 or (i_21 - (i_21 ushr 8))
                                if (i_18 == 0 && i_14 != 255) {
                                    i_18 = i_21
                                    i_21 = ints_6[i_8]
                                    i_21 = ((i_18 and 0xff00ff) * i_14 + (i_21 and 0xff00ff) * i_15 and -16711936) + (i_14 * (i_18 and 0xff00) + i_15 * (i_21 and 0xff00) and 0xff0000) shr 8
                                }

                                ints_6[i_8++] = i_21
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
                                i_20 = this.pixels[i_9++]
                                if (i_20 != 0) {
                                    i_16 = (i_20 and 0xff00ff) * i_14 and -16711936
                                    i_17 = i_14 * (i_20 and 0xff00) and 0xff0000
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
                } else {
                    if (i_5 != 2) {
                        throw IllegalArgumentException()
                    }

                    if (i_3 == 1) {
                        i_14 = -i_10
                        while (i_14 < 0) {
                            i_15 = -i_11
                            while (i_15 < 0) {
                                i_16 = this.pixels[i_9++]
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
                                i_19 = this.pixels[i_9++]
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
                                i_16 = this.pixels[i_9++]
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
                                i_20 = this.pixels[i_9++]
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
        var i_26 = i_2
        var i_15Var = i_1
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        val ints_6 = toolkit.colourBuffer ?: return
        i_15Var += offsetX
        i_26 += offsetY
        var i_7 = 0
        val i_8 = toolkit.viewportWidth
        var i_9 = imageWidth
        var i_10 = imageHeight
        var i_11 = i_8 - i_9
        var i_12 = 0
        var i_13 = i_8 * i_26 + i_15Var
        var i_14: Int
        if (i_26 < toolkit.clipTop) {
            i_14 = toolkit.clipTop - i_26
            i_10 -= i_14
            i_26 = toolkit.clipTop
            i_7 += i_14 * i_9
            i_13 += i_14 * i_8
        }

        if (i_10 + i_26 > toolkit.clipBottom) {
            i_10 -= i_10 + i_26 - toolkit.clipBottom
        }

        if (i_15Var < toolkit.clipLeft) {
            i_14 = toolkit.clipLeft - i_15Var
            i_9 -= i_14
            i_15Var = toolkit.clipLeft
            i_7 += i_14
            i_13 += i_14
            i_12 += i_14
            i_11 += i_14
        }

        if (i_9 + i_15Var > toolkit.clipRight) {
            i_14 = i_9 + i_15Var - toolkit.clipRight
            i_9 -= i_14
            i_12 += i_14
            i_11 += i_14
        }

        if (i_9 > 0 && i_10 > 0) {
            val packetsdecoder26 = class4553 as ComponentDimensionSM
            val ints_15 = packetsdecoder26.anIntArray9077
            val ints_16 = packetsdecoder26.anIntArray9078
            var i_17 = i_26
            if (i_5 > i_26) {
                i_17 = i_5
                i_13 += i_8 * (i_5 - i_26)
                i_7 += (i_5 - i_26) * imageWidth
            }

            val i_18 = Math.min(ints_15.size + i_5, i_10 + i_26)

            for (i_19 in i_17 until i_18) {
                var i_20 = ints_15[i_19 - i_5] + i_4
                var i_21 = ints_16[i_19 - i_5]
                var i_22 = i_9
                var i_23: Int
                if (i_15Var > i_20) {
                    i_23 = i_15Var - i_20
                    if (i_23 >= i_21) {
                        i_7 += i_9 + i_12
                        i_13 += i_9 + i_11
                        continue
                    }

                    i_21 -= i_23
                } else {
                    i_23 = i_20 - i_15Var
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
                    val i_25 = this.pixels[i_7++]
                    if (i_25 != 0) {
                        ints_6[i_13++] = i_25
                    } else {
                        ++i_13
                    }
                }

                i_7 += i_23 + i_12
                i_13 += i_23 + i_11
            }
        }
    }

    override fun drawColoredCircle(i_1: Int, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, i_7: Int) {
        var i_33 = i_3
        var i_41 = i_4
        var i_61 = i_6
        if (toolkit.hasBeenShutdown()) {
            throw IllegalStateException()
        }
        if (i_33 > 0 && i_41 > 0) {
            val ints_9 = toolkit.colourBuffer ?: return
            var i_10 = 0
            var i_11 = 0
            val i_12 = toolkit.viewportWidth
            val i_13 = offsetX + imageWidth + trimRight
            val i_14 = offsetY + imageHeight + trimBottom
            val i_15 = (i_13 shl 16) / i_33
            val i_16 = (i_14 shl 16) / i_41
            var i_17: Int
            if (offsetX > 0) {
                i_17 = (i_15 + (offsetX shl 16) - 1) / i_15
                var adjustedI1 = i_1 + i_17
                i_10 += i_17 * i_15 - (offsetX shl 16)
            }

            if (offsetY > 0) {
                i_17 = (i_16 + (offsetY shl 16) - 1) / i_16
                var adjustedI2 = i_2 + i_17
                i_11 += i_17 * i_16 - (offsetY shl 16)
            }

            if (imageWidth < i_13) {
                i_33 = ((imageWidth shl 16) - i_10 + i_15 - 1) / i_15
            }

            if (imageHeight < i_14) {
                i_41 = ((imageHeight shl 16) - i_11 + i_16 - 1) / i_16
            }

            var adjustedI1 = i_1
            var adjustedI2 = i_2
            if (offsetX > 0) {
                i_17 = (i_15 + (offsetX shl 16) - 1) / i_15
                adjustedI1 = i_1 + i_17
            }
            if (offsetY > 0) {
                i_17 = (i_16 + (offsetY shl 16) - 1) / i_16
                adjustedI2 = i_2 + i_17
            }

            i_17 = i_12 * adjustedI2 + adjustedI1
            var i_18 = i_12 - i_33
            if (adjustedI2 + i_41 > toolkit.clipBottom) {
                i_41 -= adjustedI2 + i_41 - toolkit.clipBottom
            }

            var i_19: Int
            if (adjustedI2 < toolkit.clipTop) {
                i_19 = toolkit.clipTop - adjustedI2
                i_41 -= i_19
                i_17 += i_19 * i_12
                i_11 += i_16 * i_19
            }

            if (i_33 + adjustedI1 > toolkit.clipRight) {
                i_19 = i_33 + adjustedI1 - toolkit.clipRight
                i_33 -= i_19
                i_18 += i_19
            }

            if (adjustedI1 < toolkit.clipLeft) {
                i_19 = toolkit.clipLeft - adjustedI1
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
            if (i_7 == 0) {
                if (i_5 == 1) {
                    i_19 = i_10

                    i_20 = -i_41
                    while (i_20 < 0) {
                        i_21 = imageWidth * (i_11 shr 16)

                        i_22 = -i_33
                        while (i_22 < 0) {
                            ints_9[i_17++] = this.pixels[i_21 + (i_10 shr 16)]
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
                        i_24 = imageWidth * (i_11 shr 16)

                        i_25 = -i_33
                        while (i_25 < 0) {
                            i_26 = this.pixels[i_24 + (i_10 shr 16)]
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
                        i_21 = imageWidth * (i_11 shr 16)

                        i_22 = -i_33
                        while (i_22 < 0) {
                            i_23 = this.pixels[i_21 + (i_10 shr 16)]
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
                        i_25 = imageWidth * (i_11 shr 16)

                        i_26 = -i_33
                        while (i_26 < 0) {
                            i_27 = this.pixels[i_25 + (i_10 shr 16)]
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
                if (i_7 == 1) {
                    if (i_5 == 1) {
                        i_19 = i_10

                        i_20 = -i_41
                        while (i_20 < 0) {
                            i_21 = imageWidth * (i_11 shr 16)

                            i_22 = -i_33
                            while (i_22 < 0) {
                                i_23 = this.pixels[i_21 + (i_10 shr 16)]
                                if (i_23 != 0) {
                                    ints_9[i_17++] = i_23
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
                        if ((i_61 and 0xffffff) == 16777215) {
                            i_20 = i_61 ushr 24
                            i_21 = 256 - i_20

                            i_22 = -i_41
                            while (i_22 < 0) {
                                i_23 = imageWidth * (i_11 shr 16)

                                i_24 = -i_33
                                while (i_24 < 0) {
                                    i_25 = this.pixels[i_23 + (i_10 shr 16)]
                                    if (i_25 != 0) {
                                        i_26 = ints_9[i_17]
                                        ints_9[i_17++] = ((i_25 and 0xff00ff) * i_20 + (i_26 and 0xff00ff) * i_21 and -16711936) + (i_20 * (i_25 and 0xff00) + i_21 * (i_26 and 0xff00) and 0xff0000) shr 8
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
                                i_26 = imageWidth * (i_11 shr 16)

                                i_27 = -i_33
                                while (i_27 < 0) {
                                    i_28 = this.pixels[i_26 + (i_10 shr 16)]
                                    if (i_28 != 0) {
                                        if (i_23 != 255) {
                                            i_29 = i_20 * (i_28 and 0xff0000) and 0xff000000.toInt()
                                            i_30 = i_21 * (i_28 and 0xff00) and 0xff0000
                                            i_31 = (i_28 and 0xff) * i_22 and 0xff00
                                            i_28 = (i_29 or i_30 or i_31) ushr 8
                                            i_32 = ints_9[i_17]
                                            ints_9[i_17++] = ((i_28 and 0xff00ff) * i_23 + (i_32 and 0xff00ff) * i_24 and -16711936) + (i_23 * (i_28 and 0xff00) + i_24 * (i_32 and 0xff00) and 0xff0000) shr 8
                                        } else {
                                            i_29 = i_20 * (i_28 and 0xff0000) and 0xff000000.toInt()
                                            i_30 = i_21 * (i_28 and 0xff00) and 0xff0000
                                            i_31 = (i_28 and 0xff) * i_22 and 0xff00
                                            ints_9[i_17++] = (i_29 or i_30 or i_31) ushr 8
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
                            i_23 = imageWidth * (i_11 shr 16)

                            i_24 = -i_33
                            while (i_24 < 0) {
                                i_25 = this.pixels[i_23 + (i_10 shr 16)]
                                i_26 = i_25 + i_61
                                i_27 = (i_25 and 0xff00ff) + (i_61 and 0xff00ff)
                                i_28 = (i_27 and 0x1000100) + (i_26 - i_27 and 0x10000)
                                i_28 = i_26 - i_28 or (i_28 - (i_28 ushr 8))
                                if (i_25 == 0 && i_20 != 255) {
                                    i_25 = i_28
                                    i_28 = ints_9[i_17]
                                    i_28 = ((i_25 and 0xff00ff) * i_20 + (i_28 and 0xff00ff) * i_21 and -16711936) + (i_20 * (i_25 and 0xff00) + i_21 * (i_28 and 0xff00) and 0xff0000) shr 8
                                }

                                ints_9[i_17++] = i_28
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
                            i_25 = imageWidth * (i_11 shr 16)

                            i_26 = -i_33
                            while (i_26 < 0) {
                                i_27 = this.pixels[i_25 + (i_10 shr 16)]
                                if (i_27 != 0) {
                                    i_21 = (i_27 and 0xff00ff) * i_19 and -16711936
                                    i_22 = i_19 * (i_27 and 0xff00) and 0xff0000
                                    ints_9[i_17++] = ((i_21 or i_22) ushr 8) + i_61
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
                } else {
                    if (i_7 != 2) {
                        throw IllegalArgumentException()
                    }

                    if (i_5 == 1) {
                        i_19 = i_10

                        i_20 = -i_41
                        while (i_20 < 0) {
                            i_21 = imageWidth * (i_11 shr 16)

                            i_22 = -i_33
                            while (i_22 < 0) {
                                i_23 = this.pixels[i_21 + (i_10 shr 16)]
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
                            i_24 = imageWidth * (i_11 shr 16)

                            i_25 = -i_33
                            while (i_25 < 0) {
                                i_26 = this.pixels[i_24 + (i_10 shr 16)]
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
                            i_21 = imageWidth * (i_11 shr 16)

                            i_22 = -i_33
                            while (i_22 < 0) {
                                i_23 = this.pixels[i_21 + (i_10 shr 16)]
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
                            i_25 = imageWidth * (i_11 shr 16)

                            i_26 = -i_33
                            while (i_26 < 0) {
                                i_27 = this.pixels[i_25 + (i_10 shr 16)]
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

    override fun drawParticle(
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
        if (i_71 > 0 && i_81 > 0 && (bool1 || bool2)) {
            var i_14 = 0
            var i_15 = 0
            val i_16 = offsetX + imageWidth + trimRight
            val i_17 = offsetY + imageHeight + trimBottom
            val i_18 = (i_16 shl 16) / i_71
            val i_19 = (i_17 shl 16) / i_81
            var i_20: Int
            var adjustedI4 = i_4
            var adjustedI5 = i_5
            if (offsetX > 0) {
                i_20 = (i_18 + (offsetX shl 16) - 1) / i_18
                adjustedI4 += i_20
                i_14 += i_20 * i_18 - (offsetX shl 16)
            }

            if (offsetY > 0) {
                i_20 = (i_19 + (offsetY shl 16) - 1) / i_19
                adjustedI5 += i_20
                i_15 += i_20 * i_19 - (offsetY shl 16)
            }

            if (imageWidth < i_16) {
                i_71 = ((imageWidth shl 16) - i_14 + i_18 - 1) / i_18
            }

            if (imageHeight < i_17) {
                i_81 = ((imageHeight shl 16) - i_15 + i_19 - 1) / i_19
            }

            i_20 = adjustedI5 * toolkit.viewportWidth + adjustedI4
            var i_21 = toolkit.viewportWidth - i_71
            if (adjustedI5 + i_81 > toolkit.clipBottom) {
                i_81 -= adjustedI5 + i_81 - toolkit.clipBottom
            }

            var i_22: Int
            if (adjustedI5 < toolkit.clipTop) {
                i_22 = toolkit.clipTop - adjustedI5
                i_81 -= i_22
                i_20 += toolkit.viewportWidth * i_22
                i_15 += i_19 * i_22
            }

            if (adjustedI4 + i_71 > toolkit.clipRight) {
                i_22 = adjustedI4 + i_71 - toolkit.clipRight
                i_71 -= i_22
                i_21 += i_22
            }

            if (adjustedI4 < toolkit.clipLeft) {
                i_22 = toolkit.clipLeft - adjustedI4
                i_71 -= i_22
                i_20 += i_22
                i_14 += i_18 * i_22
                i_21 += i_22
            }

            val floats_38 = toolkit.depthBuffer
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
                        i_26 = imageWidth * (i_15 shr 16)

                        i_27 = -i_71
                        while (i_27 < 0) {
                            if (!bool2 || f_6 < floats_38!![i_20]) {
                                if (bool1) {
                                    ints_23!![i_20] = this.pixels[i_26 + (i_14 shr 16)]
                                }

                                if (bool2 && bool13) {
                                    floats_38!![i_20] = f_6
                                }
                            }

                            ++i_20
                            i_14 += i_18
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
                        i_29 = imageWidth * (i_15 shr 16)

                        i_30 = -i_71
                        while (i_30 < 0) {
                            if (!bool2 || f_6 < floats_38!![i_20]) {
                                i_31 = this.pixels[i_29 + (i_14 shr 16)]
                                if (bool1) {
                                    i_32 = i_24 * (i_31 and 0xff0000) and 0xff000000.toInt()
                                    i_33 = i_25 * (i_31 and 0xff00) and 0xff0000
                                    i_34 = (i_31 and 0xff) * i_26 and 0xff00
                                    ints_23!![i_20] = (i_32 or i_33 or i_34) ushr 8
                                }

                                if (bool2 && bool13) {
                                    floats_38!![i_20] = f_6
                                }
                            }

                            ++i_20
                            i_14 += i_18
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
                        i_26 = imageWidth * (i_15 shr 16)

                        i_27 = -i_71
                        while (i_27 < 0) {
                            if (!bool2 || f_6 < floats_38!![i_20]) {
                                i_28 = this.pixels[i_26 + (i_14 shr 16)]
                                if (bool1) {
                                    i_29 = i_28 + i_101
                                    i_30 = (i_28 and 0xff00ff) + (i_101 and 0xff00ff)
                                    i_31 = (i_30 and 0x1000100) + (i_29 - i_30 and 0x10000)
                                    ints_23!![i_20] = i_29 - i_31 or (i_31 - (i_31 ushr 8))
                                }

                                if (bool2 && bool13) {
                                    floats_38!![i_20] = f_6
                                }
                            }

                            ++i_20
                            i_14 += i_18
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
                    i_27 = i_25 * (i_101 and 0xff00) and 0xff0000
                    i_101 = (i_26 or i_27) ushr 8
                    i_28 = i_14

                    i_29 = -i_81
                    while (i_29 < 0) {
                        i_30 = imageWidth * (i_15 shr 16)

                        i_31 = -i_71
                        while (i_31 < 0) {
                            if (!bool2 || f_6 < floats_38!![i_20]) {
                                i_32 = this.pixels[i_30 + (i_14 shr 16)]
                                if (bool1) {
                                    i_26 = (i_32 and 0xff00ff) * i_24 and -16711936
                                    i_27 = i_24 * (i_32 and 0xff00) and 0xff0000
                                    ints_23!![i_20] = ((i_26 or i_27) ushr 8) + i_101
                                }

                                if (bool2 && bool13) {
                                    floats_38!![i_20] = f_6
                                }
                            }

                            ++i_20
                            i_14 += i_18
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
                            i_26 = imageWidth * (i_15 shr 16)

                            i_27 = -i_71
                            while (i_27 < 0) {
                                i_28 = this.pixels[i_26 + (i_14 shr 16)]
                                if (i_28 != 0 && (!bool2 || f_6 < floats_38!![i_20])) {
                                    if (bool1) {
                                        ints_23!![i_20] = i_28
                                    }

                                    if (bool2 && bool13) {
                                        floats_38!![i_20] = f_6
                                    }
                                }

                                ++i_20
                                i_14 += i_18
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
                            i_25 = i_101 ushr 24
                            i_26 = 256 - i_25

                            i_27 = -i_81
                            while (i_27 < 0) {
                                i_28 = imageWidth * (i_15 shr 16)

                                i_29 = -i_71
                                while (i_29 < 0) {
                                    i_30 = this.pixels[i_28 + (i_14 shr 16)]
                                    if (i_30 != 0 && (!bool2 || f_6 < floats_38!![i_20])) {
                                        if (bool1) {
                                            i_31 = ints_23!![i_20]
                                            ints_23[i_20] = ((i_30 and 0xff00ff) * i_25 + (i_31 and 0xff00ff) * i_26 and -16711936) + (i_25 * (i_30 and 0xff00) + i_26 * (i_31 and 0xff00) and 0xff0000) shr 8
                                        }

                                        if (bool2 && bool13) {
                                            floats_38!![i_20] = f_6
                                        }
                                    }

                                    ++i_20
                                    i_14 += i_18
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
                                i_31 = imageWidth * (i_15 shr 16)

                                i_32 = -i_71
                                while (i_32 < 0) {
                                    i_33 = this.pixels[i_31 + (i_14 shr 16)]
                                    if (i_33 != 0 && (!bool2 || f_6 < floats_38!![i_20])) {
                                        if (bool1) {
                                            if (i_28 != 255) {
                                                i_34 = i_25 * (i_33 and 0xff0000) and 0xff000000.toInt()
                                                i_35 = i_26 * (i_33 and 0xff00) and 0xff0000
                                                i_36 = (i_33 and 0xff) * i_27 and 0xff00
                                                i_33 = (i_34 or i_35 or i_36) ushr 8
                                                i_37 = ints_23!![i_20]
                                                ints_23[i_20] = ((i_33 and 0xff00ff) * i_28 + (i_37 and 0xff00ff) * i_29 and -16711936) + (i_28 * (i_33 and 0xff00) + i_29 * (i_37 and 0xff00) and 0xff0000) shr 8
                                            } else {
                                                i_34 = i_25 * (i_33 and 0xff0000) and 0xff000000.toInt()
                                                i_35 = i_26 * (i_33 and 0xff00) and 0xff0000
                                                i_36 = (i_33 and 0xff) * i_27 and 0xff00
                                                ints_23!![i_20] = (i_34 or i_35 or i_36) ushr 8
                                            }
                                        }

                                        if (bool2 && bool13) {
                                            floats_38!![i_20] = f_6
                                        }
                                    }

                                    ++i_20
                                    i_14 += i_18
                                    i_32++
                                }

                                i_15 += i_19
                                i_14 = i_24
                                i_20 += i_21
                                i_30++
                            }
                        }
                    } else if (i_9 == 3) {
                        i_24 = i_14
                        i_25 = i_101 ushr 24
                        i_26 = 256 - i_25

                        i_27 = -i_81
                        while (i_27 < 0) {
                            i_28 = imageWidth * (i_15 shr 16)

                            i_29 = -i_71
                            while (i_29 < 0) {
                                i_30 = this.pixels[i_28 + (i_14 shr 16)]
                                i_31 = i_30 + i_101
                                i_32 = (i_30 and 0xff00ff) + (i_101 and 0xff00ff)
                                i_33 = (i_32 and 0x1000100) + (i_31 - i_32 and 0x10000)
                                i_33 = i_31 - i_33 or (i_33 - (i_33 ushr 8))
                                if (i_30 == 0 && i_25 != 255) {
                                    if (!bool2 || f_6 < floats_38!![i_20]) {
                                        if (bool1) {
                                            i_30 = ints_23!![i_20]
                                            ints_23[i_20] = ((i_33 and 0xff00ff) * i_25 + (i_30 and 0xff00ff) * i_26 and -16711936) + (i_25 * (i_33 and 0xff00) + i_26 * (i_30 and 0xff00) and 0xff0000) shr 8
                                        }

                                        if (bool2 && bool13) {
                                            floats_38!![i_20] = f_6
                                        }
                                    }
                                } else if (!bool2 || f_6 < floats_38!![i_20]) {
                                    if (bool1) {
                                        ints_23!![i_20] = i_33
                                    }

                                    if (bool2 && bool13) {
                                        floats_38!![i_20] = f_6
                                    }
                                }

                                ++i_20
                                i_14 += i_18
                                i_29++
                            }

                            i_15 += i_19
                            i_14 = i_24
                            i_20 += i_21
                            i_27++
                        }
                    } else {
                        if (i_9 != 2) {
                            throw IllegalArgumentException()
                        }

                        i_24 = i_101 ushr 24
                        i_25 = 256 - i_24
                        i_26 = (i_101 and 0xff00ff) * i_25 and -16711936
                        i_27 = i_25 * (i_101 and 0xff00) and 0xff0000
                        i_101 = (i_26 or i_27) ushr 8
                        i_28 = i_14

                        i_29 = -i_81
                        while (i_29 < 0) {
                            i_30 = imageWidth * (i_15 shr 16)

                            i_31 = -i_71
                            while (i_31 < 0) {
                                i_32 = this.pixels[i_30 + (i_14 shr 16)]
                                if (i_32 != 0 && (!bool2 || f_6 < floats_38!![i_20])) {
                                    if (bool1) {
                                        i_26 = (i_32 and 0xff00ff) * i_24 and -16711936
                                        i_27 = i_24 * (i_32 and 0xff00) and 0xff0000
                                        ints_23!![i_20] = ((i_26 or i_27) ushr 8) + i_101
                                    }

                                    if (bool2 && bool13) {
                                        floats_38!![i_20] = f_6
                                    }
                                }

                                ++i_20
                                i_14 += i_18
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
                            i_26 = imageWidth * (i_15 shr 16)

                            i_27 = -i_71
                            while (i_27 < 0) {
                                i_28 = this.pixels[i_26 + (i_14 shr 16)]
                                if (i_28 != 0 && (!bool2 || f_6 < floats_38!![i_20])) {
                                    if (bool1) {
                                        i_29 = ints_23!![i_20]
                                        i_30 = i_28 + i_29
                                        i_31 = (i_28 and 0xff00ff) + (i_29 and 0xff00ff)
                                        i_29 = (i_31 and 0x1000100) + (i_30 - i_31 and 0x10000)
                                        ints_23[i_20] = i_30 - i_29 or (i_29 - (i_29 ushr 8))
                                    }

                                    if (bool2 && bool13) {
                                        floats_38!![i_20] = f_6
                                    }
                                }

                                ++i_20
                                i_14 += i_18
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
                            i_29 = imageWidth * (i_15 shr 16)

                            i_30 = -i_71
                            while (i_30 < 0) {
                                i_31 = this.pixels[i_29 + (i_14 shr 16)]
                                if (i_31 != 0 && (!bool2 || f_6 < floats_38!![i_20])) {
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
                                        floats_38!![i_20] = f_6
                                    }
                                }

                                ++i_20
                                i_14 += i_18
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
                            i_26 = imageWidth * (i_15 shr 16)

                            i_27 = -i_71
                            while (i_27 < 0) {
                                i_28 = this.pixels[i_26 + (i_14 shr 16)]
                                if (!bool2 || f_6 < floats_38!![i_20]) {
                                    if (bool1) {
                                        i_29 = i_28 + i_101
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
                                        floats_38!![i_20] = f_6
                                    }
                                }

                                ++i_20
                                i_14 += i_18
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
                        i_27 = i_25 * (i_101 and 0xff00) and 0xff0000
                        i_101 = (i_26 or i_27) ushr 8
                        i_28 = i_14

                        i_29 = -i_81
                        while (i_29 < 0) {
                            i_30 = imageWidth * (i_15 shr 16)

                            i_31 = -i_71
                            while (i_31 < 0) {
                                i_32 = this.pixels[i_30 + (i_14 shr 16)]
                                if (i_32 != 0 && (!bool2 || f_6 < floats_38!![i_20])) {
                                    if (bool1) {
                                        i_26 = (i_32 and 0xff00ff) * i_24 and -16711936
                                        i_27 = i_24 * (i_32 and 0xff00) and 0xff0000
                                        i_32 = ((i_26 or i_27) ushr 8) + i_101
                                        i_33 = ints_23!![i_20]
                                        i_34 = i_32 + i_33
                                        i_35 = (i_32 and 0xff00ff) + (i_33 and 0xff00ff)
                                        i_33 = (i_35 and 0x1000100) + (i_34 - i_35 and 0x10000)
                                        ints_23[i_20] = i_34 - i_33 or (i_33 - (i_33 ushr 8))
                                    }

                                    if (bool2 && bool13) {
                                        floats_38!![i_20] = f_6
                                    }
                                }

                                ++i_20
                                i_14 += i_18
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

    override fun drawAffineTransformed(i_1: Int, i_2: Int) {
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
        if (dudx == 0) {
            if (dvdx == 0) {
                i_4 = drawHeight
                while (i_4 < 0) {
                    i_5 = pixelOffset
                    i_6 = uBase
                    i_7 = vBase
                    i_8 = drawWidth
                    if (i_6 >= 0 && i_7 >= 0 && i_6 - (imageWidth shl 12) < 0 && i_7 - (imageHeight shl 12) < 0) {
                        while (i_8 < 0) {
                            i_9 = (i_6 shr 12) + (i_7 shr 12) * imageWidth
                            i_10 = i_5++
                            renderPixel14245(ints_3, i_10, i_9, i_1, i_2)
                            i_8++
                        }
                    }
                    pixelOffset += viewportStride
                    i_4++
                }
            } else if (dvdx < 0) {
                i_4 = drawHeight
                while (i_4 < 0) {
                    i_5 = pixelOffset
                    i_6 = uBase
                    i_7 = vBase + vOffsetX
                    i_8 = drawWidth
                    if (i_6 >= 0 && i_6 - (imageWidth shl 12) < 0) {
                        i_9 = i_7 - (imageHeight shl 12)
                        if (i_9 >= 0) {
                            i_9 = (dvdx - i_9) / dvdx
                            i_8 += i_9
                            i_7 += i_9 * dvdx
                            i_5 += i_9
                        }
                        i_9 = (i_7 - dvdx) / dvdx
                        if (i_9 > i_8) {
                            i_8 = i_9
                        }
                        while (i_8 < 0) {
                            i_10 = (i_6 shr 12) + (i_7 shr 12) * imageWidth
                            i_11 = i_5++
                            renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                            i_7 += dvdx
                            i_8++
                        }
                    }
                    pixelOffset += viewportStride
                    i_4++
                    uBase += dudy
                }
            } else {
                i_4 = drawHeight
                while (i_4 < 0) {
                    i_5 = pixelOffset
                    i_6 = uBase
                    i_7 = vBase + vOffsetX
                    i_8 = drawWidth
                    if (i_6 >= 0 && i_6 - (imageWidth shl 12) < 0) {
                        if (i_7 < 0) {
                            i_9 = (dvdx - 1 - i_7) / dvdx
                            i_8 += i_9
                            i_7 += i_9 * dvdx
                            i_5 += i_9
                        }
                        i_9 = (i_7 + 1 - (imageHeight shl 12) - dvdx) / dvdx
                        if (i_9 > i_8) {
                            i_8 = i_9
                        }
                        while (i_8 < 0) {
                            i_10 = (i_6 shr 12) + (i_7 shr 12) * imageWidth
                            i_11 = i_5++
                            renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                            i_7 += dvdx
                            i_8++
                        }
                    }
                    pixelOffset += viewportStride
                    i_4++
                    uBase += dudy
                }
            }
        } else if (dudx < 0) {
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
        if (dvdx == 0) {
            i_4 = drawHeight
            while (i_4 < 0) {
                i_5 = pixelOffset
                i_6 = uBase + uOffsetX
                i_7 = vBase
                i_8 = drawWidth
                if (i_7 >= 0 && i_7 - (imageHeight shl 12) < 0) {
                    i_9 = i_6 - (imageWidth shl 12)
                    if (i_9 >= 0) {
                        i_9 = (dudx - i_9) / dudx
                        i_8 += i_9
                        i_6 += i_9 * dudx
                        i_5 += i_9
                    }
                    i_9 = (i_6 - dudx) / dudx
                    if (i_9 > i_8) {
                        i_8 = i_9
                    }
                    while (i_8 < 0) {
                        i_10 = (i_6 shr 12) + (i_7 shr 12) * imageWidth
                        i_11 = i_5++
                        renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                        i_6 += dudx
                        i_8++
                    }
                }
                pixelOffset += viewportStride
                i_4++
                vBase += dvdy
            }
        } else if (dvdx < 0) {
            i_4 = drawHeight
            while (i_4 < 0) {
                i_5 = pixelOffset
                i_6 = uBase + uOffsetX
                i_7 = vBase + vOffsetX
                i_8 = drawWidth
                i_9 = i_6 - (imageWidth shl 12)
                if (i_9 >= 0) {
                    i_9 = (dudx - i_9) / dudx
                    i_8 += i_9
                    i_6 += i_9 * dudx
                    i_7 += i_9 * dvdx
                    i_5 += i_9
                }
                i_9 = (i_6 - dudx) / dudx
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                i_9 = i_7 - (imageHeight shl 12)
                if (i_9 >= 0) {
                    i_9 = (dvdx - i_9) / dvdx
                    i_8 += i_9
                    i_6 += i_9 * dudx
                    i_7 += i_9 * dvdx
                    i_5 += i_9
                }
                i_9 = (i_7 - dvdx) / dvdx
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                while (i_8 < 0) {
                    i_10 = (i_6 shr 12) + (i_7 shr 12) * imageWidth
                    i_11 = i_5++
                    renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                    i_6 += dudx
                    i_7 += dvdx
                    i_8++
                }
                pixelOffset += viewportStride
                i_4++
                uBase += dudy
                vBase += dvdy
            }
        } else {
            i_4 = drawHeight
            while (i_4 < 0) {
                i_5 = pixelOffset
                i_6 = uBase + uOffsetX
                i_7 = vBase + vOffsetX
                i_8 = drawWidth
                i_9 = i_6 - (imageWidth shl 12)
                if (i_9 >= 0) {
                    i_9 = (dudx - i_9) / dudx
                    i_8 += i_9
                    i_6 += i_9 * dudx
                    i_7 += i_9 * dvdx
                    i_5 += i_9
                }
                i_9 = (i_6 - dudx) / dudx
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                if (i_7 < 0) {
                    i_9 = (dvdx - 1 - i_7) / dvdx
                    i_8 += i_9
                    i_6 += i_9 * dudx
                    i_7 += i_9 * dvdx
                    i_5 += i_9
                }
                i_9 = (i_7 + 1 - (imageHeight shl 12) - dvdx) / dvdx
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                while (i_8 < 0) {
                    i_10 = (i_6 shr 12) + (i_7 shr 12) * imageWidth
                    i_11 = i_5++
                    renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                    i_6 += dudx
                    i_7 += dvdx
                    i_8++
                }
                pixelOffset += viewportStride
                i_4++
                uBase += dudy
                vBase += dvdy
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
        if (dvdx == 0) {
            i_4 = drawHeight
            while (i_4 < 0) {
                i_5 = pixelOffset
                i_6 = uBase + uOffsetX
                i_7 = vBase
                i_8 = drawWidth
                if (i_7 >= 0 && i_7 - (imageHeight shl 12) < 0) {
                    if (i_6 < 0) {
                        i_9 = (dudx - 1 - i_6) / dudx
                        i_8 += i_9
                        i_6 += i_9 * dudx
                        i_5 += i_9
                    }
                    i_9 = (i_6 + 1 - (imageWidth shl 12) - dudx) / dudx
                    if (i_9 > i_8) {
                        i_8 = i_9
                    }
                    while (i_8 < 0) {
                        i_10 = (i_6 shr 12) + (i_7 shr 12) * imageWidth
                        i_11 = i_5++
                        renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                        i_6 += dudx
                        i_8++
                    }
                }
                pixelOffset += viewportStride
                i_4++
                vBase += dvdy
            }
        } else if (dvdx < 0) {
            i_4 = drawHeight
            while (i_4 < 0) {
                i_5 = pixelOffset
                i_6 = uBase + uOffsetX
                i_7 = vBase + vOffsetX
                i_8 = drawWidth
                if (i_6 < 0) {
                    i_9 = (dudx - 1 - i_6) / dudx
                    i_8 += i_9
                    i_6 += i_9 * dudx
                    i_7 += i_9 * dvdx
                    i_5 += i_9
                }
                i_9 = (i_6 + 1 - (imageWidth shl 12) - dudx) / dudx
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                i_9 = i_7 - (imageHeight shl 12)
                if (i_9 >= 0) {
                    i_9 = (dvdx - i_9) / dvdx
                    i_8 += i_9
                    i_6 += i_9 * dudx
                    i_7 += i_9 * dvdx
                    i_5 += i_9
                }
                i_9 = (i_7 - dvdx) / dvdx
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                while (i_8 < 0) {
                    i_10 = (i_6 shr 12) + (i_7 shr 12) * imageWidth
                    i_11 = i_5++
                    renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                    i_6 += dudx
                    i_7 += dvdx
                    i_8++
                }
                pixelOffset += viewportStride
                i_4++
                uBase += dudy
                vBase += dvdy
            }
        } else {
            i_4 = drawHeight
            while (i_4 < 0) {
                i_5 = pixelOffset
                i_6 = uBase + uOffsetX
                i_7 = vBase + vOffsetX
                i_8 = drawWidth
                if (i_6 < 0) {
                    i_9 = (dudx - 1 - i_6) / dudx
                    i_8 += i_9
                    i_6 += i_9 * dudx
                    i_7 += i_9 * dvdx
                    i_5 += i_9
                }
                i_9 = (i_6 + 1 - (imageWidth shl 12) - dudx) / dudx
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                if (i_7 < 0) {
                    i_9 = (dvdx - 1 - i_7) / dvdx
                    i_8 += i_9
                    i_6 += i_9 * dudx
                    i_7 += i_9 * dvdx
                    i_5 += i_9
                }
                i_9 = (i_7 + 1 - (imageHeight shl 12) - dvdx) / dvdx
                if (i_9 > i_8) {
                    i_8 = i_9
                }
                while (i_8 < 0) {
                    i_10 = (i_6 shr 12) + (i_7 shr 12) * imageWidth
                    i_11 = i_5++
                    renderPixel14245(ints_3, i_11, i_10, i_1, i_2)
                    i_6 += dudx
                    i_7 += dvdx
                    i_8++
                }
                pixelOffset += viewportStride
                i_4++
                uBase += dudy
                vBase += dvdy
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
        if (transparencyMode == 0) {
            if (blendMode == 1) {
                ints_3[destIdx] = this.pixels[srcIdx]
            } else if (blendMode == 0) {
                i_11 = this.pixels[srcIdx]
                i_12 = colorRed * (i_11 and 0xff0000) and 0xff000000.toInt()
                i_13 = (i_11 and 0xff00) * colorGreen and 0xff0000
                i_14 = (i_11 and 0xff) * colorBlue and 0xff00
                ints_3[destIdx] = (i_12 or i_13 or i_14) ushr 8
            } else if (blendMode == 3) {
                i_11 = this.pixels[srcIdx]
                i_12 = blendColor
                i_13 = i_11 + i_12
                i_14 = (i_11 and 0xff00ff) + (i_12 and 0xff00ff)
                i_15 = (i_14 and 0x1000100) + (i_13 - i_14 and 0x10000)
                ints_3[destIdx] = i_13 - i_15 or (i_15 - (i_15 ushr 8))
            } else {
                if (blendMode != 2) {
                    throw IllegalArgumentException()
                }
                i_11 = this.pixels[srcIdx]
                i_12 = (i_11 and 0xff00ff) * colorAlpha and -16711936
                i_13 = (i_11 and 0xff00) * colorAlpha and 0xff0000
                ints_3[destIdx] = ((i_12 or i_13) ushr 8) + overlayPrecomputed
            }
        } else if (transparencyMode == 1) {
            if (blendMode == 1) {
                i_11 = this.pixels[srcIdx]
                if (i_11 != 0) {
                    ints_3[destIdx] = i_11
                }
            } else if (blendMode == 0) {
                i_11 = this.pixels[srcIdx]
                if (i_11 != 0) {
                    if ((blendColor and 0xffffff) == 16777215) {
                        i_12 = blendColor ushr 24
                        i_13 = 256 - i_12
                        i_14 = ints_3[destIdx]
                        ints_3[destIdx] = ((i_11 and 0xff00ff) * i_12 + (i_14 and 0xff00ff) * i_13 and -16711936) + (i_12 * (i_11 and 0xff00) + i_13 * (i_14 and 0xff00) and 0xff0000) shr 8
                    } else if (colorAlpha != 255) {
                        i_12 = colorRed * (i_11 and 0xff0000) and 0xff000000.toInt()
                        i_13 = (i_11 and 0xff00) * colorGreen and 0xff0000
                        i_14 = (i_11 and 0xff) * colorBlue and 0xff00
                        i_11 = (i_12 or i_13 or i_14) ushr 8
                        i_15 = ints_3[destIdx]
                        ints_3[destIdx] = ((i_11 and 0xff00ff) * colorAlpha + (i_15 and 0xff00ff) * colorInverseAlpha and -16711936) + ((i_11 and 0xff00) * colorAlpha + (i_15 and 0xff00) * colorInverseAlpha and 0xff0000) shr 8
                    } else {
                        i_12 = colorRed * (i_11 and 0xff0000) and 0xff000000.toInt()
                        i_13 = (i_11 and 0xff00) * colorGreen and 0xff0000
                        i_14 = (i_11 and 0xff) * colorBlue and 0xff00
                        ints_3[destIdx] = (i_12 or i_13 or i_14) ushr 8
                    }
                }
            } else if (blendMode == 3) {
                i_11 = this.pixels[srcIdx]
                i_12 = blendColor
                i_13 = i_11 + i_12
                i_14 = (i_11 and 0xff00ff) + (i_12 and 0xff00ff)
                i_15 = (i_14 and 0x1000100) + (i_13 - i_14 and 0x10000)
                i_15 = i_13 - i_15 or (i_15 - (i_15 ushr 8))
                if (i_11 == 0 && colorAlpha != 255) {
                    i_11 = i_15
                    i_15 = ints_3[destIdx]
                    i_15 = ((i_11 and 0xff00ff) * colorAlpha + (i_15 and 0xff00ff) * colorInverseAlpha and -16711936) + ((i_11 and 0xff00) * colorAlpha + (i_15 and 0xff00) * colorInverseAlpha and 0xff0000) shr 8
                }
                ints_3[destIdx] = i_15
            } else {
                if (blendMode != 2) {
                    throw IllegalArgumentException()
                }
                i_11 = this.pixels[srcIdx]
                if (i_11 != 0) {
                    i_12 = (i_11 and 0xff00ff) * colorAlpha and -16711936
                    i_13 = (i_11 and 0xff00) * colorAlpha and 0xff0000
                    ints_3[destIdx] = ((i_12 or i_13) ushr 8) + overlayPrecomputed
                }
            }
        } else {
            if (transparencyMode != 2) {
                throw IllegalArgumentException()
            }
            if (blendMode == 1) {
                i_11 = this.pixels[srcIdx]
                if (i_11 != 0) {
                    i_12 = ints_3[destIdx]
                    i_13 = i_11 + i_12
                    i_14 = (i_11 and 0xff00ff) + (i_12 and 0xff00ff)
                    i_12 = (i_14 and 0x1000100) + (i_13 - i_14 and 0x10000)
                    ints_3[destIdx] = i_13 - i_12 or (i_12 - (i_12 ushr 8))
                }
            } else if (blendMode == 0) {
                i_11 = this.pixels[srcIdx]
                if (i_11 != 0) {
                    i_12 = colorRed * (i_11 and 0xff0000) and 0xff000000.toInt()
                    i_13 = (i_11 and 0xff00) * colorGreen and 0xff0000
                    i_14 = (i_11 and 0xff) * colorBlue and 0xff00
                    i_11 = (i_12 or i_13 or i_14) ushr 8
                    i_15 = ints_3[destIdx]
                    i_16 = i_11 + i_15
                    i_17 = (i_11 and 0xff00ff) + (i_15 and 0xff00ff)
                    i_15 = (i_17 and 0x1000100) + (i_16 - i_17 and 0x10000)
                    ints_3[destIdx] = i_16 - i_15 or (i_15 - (i_15 ushr 8))
                }
            } else if (blendMode == 3) {
                i_11 = this.pixels[srcIdx]
                i_12 = blendColor
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
                i_11 = this.pixels[srcIdx]
                if (i_11 != 0) {
                    i_12 = (i_11 and 0xff00ff) * colorAlpha and -16711936
                    i_13 = (i_11 and 0xff00) * colorAlpha and 0xff0000
                    i_11 = ((i_12 or i_13) ushr 8) + overlayPrecomputed
                    i_14 = ints_3[destIdx]
                    i_15 = i_11 + i_14
                    i_16 = (i_11 and 0xff00ff) + (i_14 and 0xff00ff)
                    i_14 = (i_16 and 0x1000100) + (i_15 - i_16 and 0x10000)
                    ints_3[destIdx] = i_15 - i_14 or (i_14 - (i_14 ushr 8))
                }
            }
        }
    }

    override fun drawMasked(ints_1: IntArray, ints_2: IntArray, i_3: Int, i_4: Int) {
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
        if (dudx == 0) {
            if (dvdx == 0) {
                i_6 = drawHeight
                while (i_6 < 0) {
                    i_7 = i_6 + i_4
                    if (i_7 >= 0) {
                        if (i_7 >= ints_1.size) {
                            break
                        }
                        i_8 = pixelOffset
                        i_9 = uBase
                        i_10 = vBase
                        i_11 = drawWidth
                        if (i_9 >= 0 && i_10 >= 0 && i_9 - (imageWidth shl 12) < 0 && i_10 - (imageHeight shl 12) < 0) {
                            i_12 = ints_1[i_7] - i_3
                            i_13 = -ints_2[i_7]
                            i_14 = i_12 - (i_8 - pixelOffset)
                            if (i_14 > 0) {
                                i_8 += i_14
                                i_11 += i_14
                                i_9 += i_14 * dudx
                                i_10 += i_14 * dvdx
                            } else {
                                i_13 -= i_14
                            }
                            if (i_11 < i_13) {
                                i_11 = i_13
                            }
                            while (i_11 < 0) {
                                i_15 = this.pixels[(i_9 shr 12) + (i_10 shr 12) * imageWidth]
                                if (i_15 != 0) {
                                    ints_5[i_8++] = i_15
                                } else {
                                    ++i_8
                                }
                                i_11++
                            }
                        }
                    }
                    pixelOffset += viewportStride
                    i_6++
                }
            } else if (dvdx < 0) {
                i_6 = drawHeight
                while (i_6 < 0) {
                    i_7 = i_6 + i_4
                    if (i_7 >= 0) {
                        if (i_7 >= ints_1.size) {
                            break
                        }
                        i_8 = pixelOffset
                        i_9 = uBase
                        i_10 = vBase + vOffsetX
                        i_11 = drawWidth
                        if (i_9 >= 0 && i_9 - (imageWidth shl 12) < 0) {
                            i_12 = i_10 - (imageHeight shl 12)
                            if (i_12 >= 0) {
                                i_12 = (dvdx - i_12) / dvdx
                                i_11 += i_12
                                i_10 += i_12 * dvdx
                                i_8 += i_12
                            }
                            i_12 = (i_10 - dvdx) / dvdx
                            if (i_12 > i_11) {
                                i_11 = i_12
                            }
                            i_13 = ints_1[i_7] - i_3
                            i_14 = -ints_2[i_7]
                            i_15 = i_13 - (i_8 - pixelOffset)
                            if (i_15 > 0) {
                                i_8 += i_15
                                i_11 += i_15
                                i_9 += i_15 * dudx
                                i_10 += i_15 * dvdx
                            } else {
                                i_14 -= i_15
                            }
                            if (i_11 < i_14) {
                                i_11 = i_14
                            }
                            while (i_11 < 0) {
                                i_16 = this.pixels[(i_9 shr 12) + (i_10 shr 12) * imageWidth]
                                if (i_16 != 0) {
                                    ints_5[i_8++] = i_16
                                } else {
                                    ++i_8
                                }
                                i_10 += dvdx
                                i_11++
                            }
                        }
                    }
                    pixelOffset += viewportStride
                    i_6++
                    uBase += dudy
                }
            } else {
                i_6 = drawHeight
                while (i_6 < 0) {
                    i_7 = i_6 + i_4
                    if (i_7 >= 0) {
                        if (i_7 >= ints_1.size) {
                            break
                        }
                        i_8 = pixelOffset
                        i_9 = uBase
                        i_10 = vBase + vOffsetX
                        i_11 = drawWidth
                        if (i_9 >= 0 && i_9 - (imageWidth shl 12) < 0) {
                            if (i_10 < 0) {
                                i_12 = (dvdx - 1 - i_10) / dvdx
                                i_11 += i_12
                                i_10 += i_12 * dvdx
                                i_8 += i_12
                            }
                            i_12 = (i_10 + 1 - (imageHeight shl 12) - dvdx) / dvdx
                            if (i_12 > i_11) {
                                i_11 = i_12
                            }
                            i_13 = ints_1[i_7] - i_3
                            i_14 = -ints_2[i_7]
                            i_15 = i_13 - (i_8 - pixelOffset)
                            if (i_15 > 0) {
                                i_8 += i_15
                                i_11 += i_15
                                i_9 += i_15 * dudx
                                i_10 += i_15 * dvdx
                            } else {
                                i_14 -= i_15
                            }
                            if (i_11 < i_14) {
                                i_11 = i_14
                            }
                            while (i_11 < 0) {
                                i_16 = this.pixels[(i_9 shr 12) + (i_10 shr 12) * imageWidth]
                                if (i_16 != 0) {
                                    ints_5[i_8++] = i_16
                                } else {
                                    ++i_8
                                }
                                i_10 += dvdx
                                i_11++
                            }
                        }
                    }
                    pixelOffset += viewportStride
                    i_6++
                    uBase += dudy
                }
            }
        } else if (dudx < 0) {
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
        var i_16: Int
        if (dvdx == 0) {
            i_6 = drawHeight
            while (i_6 < 0) {
                i_7 = i_6 + i_4
                if (i_7 >= 0) {
                    if (i_7 >= ints_1.size) {
                        break
                    }
                    i_8 = pixelOffset
                    i_9 = uBase + uOffsetX
                    i_10 = vBase
                    i_11 = drawWidth
                    if (i_10 >= 0 && i_10 - (imageHeight shl 12) < 0) {
                        i_12 = i_9 - (imageWidth shl 12)
                        if (i_12 >= 0) {
                            i_12 = (dudx - i_12) / dudx
                            i_11 += i_12
                            i_9 += i_12 * dudx
                            i_8 += i_12
                        }
                        i_12 = (i_9 - dudx) / dudx
                        if (i_12 > i_11) {
                            i_11 = i_12
                        }
                        i_13 = ints_1[i_7] - i_3
                        i_14 = -ints_2[i_7]
                        i_15 = i_13 - (i_8 - pixelOffset)
                        if (i_15 > 0) {
                            i_8 += i_15
                            i_11 += i_15
                            i_9 += i_15 * dudx
                            i_10 += i_15 * dvdx
                        } else {
                            i_14 -= i_15
                        }
                        if (i_11 < i_14) {
                            i_11 = i_14
                        }
                        while (i_11 < 0) {
                            i_16 = this.pixels[(i_9 shr 12) + (i_10 shr 12) * imageWidth]
                            if (i_16 != 0) {
                                ints_5[i_8++] = i_16
                            } else {
                                ++i_8
                            }
                            i_9 += dudx
                            i_11++
                        }
                    }
                }
                pixelOffset += viewportStride
                i_6++
                vBase += dvdy
            }
        } else if (dvdx < 0) {
            i_6 = drawHeight
            while (i_6 < 0) {
                i_7 = i_6 + i_4
                if (i_7 >= 0) {
                    if (i_7 >= ints_1.size) {
                        break
                    }
                    i_8 = pixelOffset
                    i_9 = uBase + uOffsetX
                    i_10 = vBase + vOffsetX
                    i_11 = drawWidth
                    i_12 = i_9 - (imageWidth shl 12)
                    if (i_12 >= 0) {
                        i_12 = (dudx - i_12) / dudx
                        i_11 += i_12
                        i_9 += i_12 * dudx
                        i_10 += i_12 * dvdx
                        i_8 += i_12
                    }
                    i_12 = (i_9 - dudx) / dudx
                    if (i_12 > i_11) {
                        i_11 = i_12
                    }
                    i_12 = i_10 - (imageHeight shl 12)
                    if (i_12 >= 0) {
                        i_12 = (dvdx - i_12) / dvdx
                        i_11 += i_12
                        i_9 += i_12 * dudx
                        i_10 += i_12 * dvdx
                        i_8 += i_12
                    }
                    i_12 = (i_10 - dvdx) / dvdx
                    if (i_12 > i_11) {
                        i_11 = i_12
                    }
                    i_13 = ints_1[i_7] - i_3
                    i_14 = -ints_2[i_7]
                    i_15 = i_13 - (i_8 - pixelOffset)
                    if (i_15 > 0) {
                        i_8 += i_15
                        i_11 += i_15
                        i_9 += i_15 * dudx
                        i_10 += i_15 * dvdx
                    } else {
                        i_14 -= i_15
                    }
                    if (i_11 < i_14) {
                        i_11 = i_14
                    }
                    while (i_11 < 0) {
                        i_16 = this.pixels[(i_9 shr 12) + (i_10 shr 12) * imageWidth]
                        if (i_16 != 0) {
                            ints_5[i_8++] = i_16
                        } else {
                            ++i_8
                        }
                        i_9 += dudx
                        i_10 += dvdx
                        i_11++
                    }
                }
                pixelOffset += viewportStride
                i_6++
                uBase += dudy
                vBase += dvdy
            }
        } else {
            i_6 = drawHeight
            while (i_6 < 0) {
                i_7 = i_6 + i_4
                if (i_7 >= 0) {
                    if (i_7 >= ints_1.size) {
                        break
                    }
                    i_8 = pixelOffset
                    i_9 = uBase + uOffsetX
                    i_10 = vBase + vOffsetX
                    i_11 = drawWidth
                    i_12 = i_9 - (imageWidth shl 12)
                    if (i_12 >= 0) {
                        i_12 = (dudx - i_12) / dudx
                        i_11 += i_12
                        i_9 += i_12 * dudx
                        i_10 += i_12 * dvdx
                        i_8 += i_12
                    }
                    i_12 = (i_9 - dudx) / dudx
                    if (i_12 > i_11) {
                        i_11 = i_12
                    }
                    if (i_10 < 0) {
                        i_12 = (dvdx - 1 - i_10) / dvdx
                        i_11 += i_12
                        i_9 += i_12 * dudx
                        i_10 += i_12 * dvdx
                        i_8 += i_12
                    }
                    i_12 = (i_10 + 1 - (imageHeight shl 12) - dvdx) / dvdx
                    if (i_12 > i_11) {
                        i_11 = i_12
                    }
                    i_13 = ints_1[i_7] - i_3
                    i_14 = -ints_2[i_7]
                    i_15 = i_13 - (i_8 - pixelOffset)
                    if (i_15 > 0) {
                        i_8 += i_15
                        i_11 += i_15
                        i_9 += i_15 * dudx
                        i_10 += i_15 * dvdx
                    } else {
                        i_14 -= i_15
                    }
                    if (i_11 < i_14) {
                        i_11 = i_14
                    }
                    while (i_11 < 0) {
                        i_16 = this.pixels[(i_9 shr 12) + (i_10 shr 12) * imageWidth]
                        if (i_16 != 0) {
                            ints_5[i_8++] = i_16
                        } else {
                            ++i_8
                        }
                        i_9 += dudx
                        i_10 += dvdx
                        i_11++
                    }
                }
                pixelOffset += viewportStride
                i_6++
                uBase += dudy
                vBase += dvdy
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
        var i_16: Int
        if (dvdx == 0) {
            i_6 = drawHeight
            while (i_6 < 0) {
                i_7 = i_6 + i_4
                if (i_7 >= 0) {
                    if (i_7 >= ints_1.size) {
                        break
                    }
                    i_8 = pixelOffset
                    i_9 = uBase + uOffsetX
                    i_10 = vBase
                    i_11 = drawWidth
                    if (i_10 >= 0 && i_10 - (imageHeight shl 12) < 0) {
                        if (i_9 < 0) {
                            i_12 = (dudx - 1 - i_9) / dudx
                            i_11 += i_12
                            i_9 += i_12 * dudx
                            i_8 += i_12
                        }
                        i_12 = (i_9 + 1 - (imageWidth shl 12) - dudx) / dudx
                        if (i_12 > i_11) {
                            i_11 = i_12
                        }
                        i_13 = ints_1[i_7] - i_3
                        i_14 = -ints_2[i_7]
                        i_15 = i_13 - (i_8 - pixelOffset)
                        if (i_15 > 0) {
                            i_8 += i_15
                            i_11 += i_15
                            i_9 += i_15 * dudx
                            i_10 += i_15 * dvdx
                        } else {
                            i_14 -= i_15
                        }
                        if (i_11 < i_14) {
                            i_11 = i_14
                        }
                        while (i_11 < 0) {
                            i_16 = this.pixels[(i_9 shr 12) + (i_10 shr 12) * imageWidth]
                            if (i_16 != 0) {
                                ints_5[i_8++] = i_16
                            } else {
                                ++i_8
                            }
                            i_9 += dudx
                            i_11++
                        }
                    }
                }
                pixelOffset += viewportStride
                i_6++
                uBase += dudy
                vBase += dvdy
            }
        } else if (dvdx < 0) {
            i_6 = drawHeight
            while (i_6 < 0) {
                i_7 = i_6 + i_4
                if (i_7 >= 0) {
                    if (i_7 >= ints_1.size) {
                        break
                    }
                    i_8 = pixelOffset
                    i_9 = uBase + uOffsetX
                    i_10 = vBase + vOffsetX
                    i_11 = drawWidth
                    if (i_9 < 0) {
                        i_12 = (dudx - 1 - i_9) / dudx
                        i_11 += i_12
                        i_9 += i_12 * dudx
                        i_10 += i_12 * dvdx
                        i_8 += i_12
                    }
                    i_12 = (i_9 + 1 - (imageWidth shl 12) - dudx) / dudx
                    if (i_12 > i_11) {
                        i_11 = i_12
                    }
                    i_12 = i_10 - (imageHeight shl 12)
                    if (i_12 >= 0) {
                        i_12 = (dvdx - i_12) / dvdx
                        i_11 += i_12
                        i_9 += i_12 * dudx
                        i_10 += i_12 * dvdx
                        i_8 += i_12
                    }
                    i_12 = (i_10 - dvdx) / dvdx
                    if (i_12 > i_11) {
                        i_11 = i_12
                    }
                    i_13 = ints_1[i_7] - i_3
                    i_14 = -ints_2[i_7]
                    i_15 = i_13 - (i_8 - pixelOffset)
                    if (i_15 > 0) {
                        i_8 += i_15
                        i_11 += i_15
                        i_9 += i_15 * dudx
                        i_10 += i_15 * dvdx
                    } else {
                        i_14 -= i_15
                    }
                    if (i_11 < i_14) {
                        i_11 = i_14
                    }
                    while (i_11 < 0) {
                        i_16 = this.pixels[(i_9 shr 12) + (i_10 shr 12) * imageWidth]
                        if (i_16 != 0) {
                            ints_5[i_8++] = i_16
                        } else {
                            ++i_8
                        }
                        i_9 += dudx
                        i_10 += dvdx
                        i_11++
                    }
                }
                pixelOffset += viewportStride
                i_6++
                uBase += dudy
                vBase += dvdy
            }
        } else {
            i_6 = drawHeight
            while (i_6 < 0) {
                i_7 = i_6 + i_4
                if (i_7 >= 0) {
                    if (i_7 >= ints_1.size) {
                        break
                    }
                    i_8 = pixelOffset
                    i_9 = uBase + uOffsetX
                    i_10 = vBase + vOffsetX
                    i_11 = drawWidth
                    if (i_9 < 0) {
                        i_12 = (dudx - 1 - i_9) / dudx
                        i_11 += i_12
                        i_9 += i_12 * dudx
                        i_10 += i_12 * dvdx
                        i_8 += i_12
                    }
                    i_12 = (i_9 + 1 - (imageWidth shl 12) - dudx) / dudx
                    if (i_12 > i_11) {
                        i_11 = i_12
                    }
                    if (i_10 < 0) {
                        i_12 = (dvdx - 1 - i_10) / dvdx
                        i_11 += i_12
                        i_9 += i_12 * dudx
                        i_10 += i_12 * dvdx
                        i_8 += i_12
                    }
                    i_12 = (i_10 + 1 - (imageHeight shl 12) - dvdx) / dvdx
                    if (i_12 > i_11) {
                        i_11 = i_12
                    }
                    i_13 = ints_1[i_7] - i_3
                    i_14 = -ints_2[i_7]
                    i_15 = i_13 - (i_8 - pixelOffset)
                    if (i_15 > 0) {
                        i_8 += i_15
                        i_11 += i_15
                        i_9 += i_15 * dudx
                        i_10 += i_15 * dvdx
                    } else {
                        i_14 -= i_15
                    }
                    if (i_11 < i_14) {
                        i_11 = i_14
                    }
                    while (i_11 < 0) {
                        i_16 = this.pixels[(i_9 shr 12) + (i_10 shr 12) * imageWidth]
                        if (i_16 != 0) {
                            ints_5[i_8++] = i_16
                        } else {
                            ++i_8
                        }
                        i_9 += dudx
                        i_10 += dvdx
                        i_11++
                    }
                }
                pixelOffset += viewportStride
                i_6++
                uBase += dudy
                vBase += dvdy
            }
        }
    }

    override fun pixelTransfer() {
        throw IllegalStateException("")
    }
}
