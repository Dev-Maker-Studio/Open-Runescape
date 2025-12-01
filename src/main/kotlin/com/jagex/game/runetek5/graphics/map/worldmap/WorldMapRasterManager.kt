package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.toolkit.AbstractToolkit

object WorldMapRasterManager {

    @JvmField
    var anInt1726: Int = 0

    @JvmField
    var anIntArray1728: IntArray? = null

    @JvmField
    var anInt1729: Int = 0

    @JvmField
    var anInt1722: Int = 0

    @JvmField
    var anInt1724: Int = 0

    @JvmField
    var currentY: Int = 0

    @JvmField
    var currentX: Int = 0

    @JvmField
    var currentWidth: Int = 0

    @JvmStatic
    fun method2471(toolkit: AbstractToolkit, positioning: IntArray, lineColor: Int) {
        method2474(toolkit, positioning, positioning.size, lineColor, null, null)
    }

    @JvmStatic
    fun method2472(i_0: Int, i_1: Int) {
        if (i_1 > i_0 + 4) {
            var i_2 = i_0
            val i_3 = anIntArray1728!![i_0]
            val i_4 = anIntArray1728!![i_0 + 1]
            val i_5 = anIntArray1728!![i_0 + 2]
            val i_6 = anIntArray1728!![i_0 + 3]

            var i_7 = i_0 + 4
            while (i_7 < i_1) {
                val i_8 = anIntArray1728!![i_7 + 1]
                if (i_8 < i_4) {
                    anIntArray1728!![i_2] = anIntArray1728!![i_7]
                    anIntArray1728!![i_2 + 1] = i_8
                    anIntArray1728!![i_2 + 2] = anIntArray1728!![i_7 + 2]
                    anIntArray1728!![i_2 + 3] = anIntArray1728!![i_7 + 3]
                    i_2 += 4
                    anIntArray1728!![i_7] = anIntArray1728!![i_2]
                    anIntArray1728!![i_7 + 1] = anIntArray1728!![i_2 + 1]
                    anIntArray1728!![i_7 + 2] = anIntArray1728!![i_2 + 2]
                    anIntArray1728!![i_7 + 3] = anIntArray1728!![i_2 + 3]
                }
                i_7 += 4
            }

            anIntArray1728!![i_2] = i_3
            anIntArray1728!![i_2 + 1] = i_4
            anIntArray1728!![i_2 + 2] = i_5
            anIntArray1728!![i_2 + 3] = i_6
            method2472(i_0, i_2)
            method2472(i_2 + 4, i_1)
        }
    }

    @JvmStatic
    fun method2474(toolkit: AbstractToolkit, ints_1: IntArray, i_3: Int, color: Int, ints_5: IntArray?, ints_6: IntArray?) {
        val clipping = IntArray(4)
        toolkit.getClipping(clipping)
        if (ints_5 != null && ints_5.size != clipping[3] - clipping[1]) {
            throw IllegalStateException()
        } else {
            method2475()
            method2476(ints_1, i_3)
            clipRight(clipping[1])

            while (true) {
                var x: Int
                var width: Int
                var y: Int
                while (true) {
                    if (!clipBottom(clipping[3])) {
                        return
                    }

                    x = currentX
                    width = currentWidth
                    y = currentY
                    if (ints_5 == null) {
                        break
                    }

                    val i_11 = y - clipping[1]
                    var x_mod = x
                    if (x_mod < ints_5[i_11] + clipping[0]) {
                        x_mod = ints_5[i_11] + clipping[0]
                    }

                    var width_mod = width
                    if (width_mod > ints_5[i_11] + ints_6!![i_11] + clipping[0]) {
                        width_mod = ints_5[i_11] + ints_6[i_11] + clipping[0]
                    }

                    if (width_mod - x_mod > 0) {
                        toolkit.drawTransparentHorizontalLine(x_mod, y, width_mod - x_mod, color, 1)
                        break
                    }
                }

                if (ints_5 == null) {
                    toolkit.drawTransparentHorizontalLine(x, y, width - x, color, 1)
                }
            }
        }
    }

    @JvmStatic
    fun method2475() {
        anInt1726 = 0
    }

    @JvmStatic
    fun method2476(ints_0: IntArray, i_2: Int) {
        val i_3 = (i_2 shl 1) + anInt1726
        var i_5: Int
        if (anIntArray1728 == null || anIntArray1728!!.size < i_3) {
            val ints_8 = IntArray(i_3)

            i_5 = 0
            while (i_5 < anInt1726) {
                ints_8[i_5] = anIntArray1728!![i_5]
                i_5++
            }

            anIntArray1728 = ints_8
        }

        val i_4 = i_2 - 2

        i_5 = 0
        while (i_5 < i_2) {
            val i_6 = ints_0[i_4 + 1]
            val i_7 = ints_0[i_5 + 1]
            if (i_6 < i_7) {
                anIntArray1728!![anInt1726++] = ints_0[i_4]
                anIntArray1728!![anInt1726++] = i_6
                anIntArray1728!![anInt1726++] = ints_0[i_5]
                anIntArray1728!![anInt1726++] = i_7
            } else if (i_7 < i_6) {
                anIntArray1728!![anInt1726++] = ints_0[i_5]
                anIntArray1728!![anInt1726++] = i_7
                anIntArray1728!![anInt1726++] = ints_0[i_4]
                anIntArray1728!![anInt1726++] = i_6
            }

            i_5 += 2
        }
    }

    @JvmStatic
    fun clipRight(i_0: Int) {
        if (anInt1726 < 0) {
            anInt1729 = 0
            anInt1722 = 0
            anInt1724 = 0
            currentY = 2147483646
        } else {
            method2472(0, anInt1726)
            var i_1 = anIntArray1728!![1]
            if (i_1 < i_0) {
                i_1 = i_0
            }

            val b_2: Byte = 0

            var i_3: Int
            i_3 = 0
            while (i_3 < anInt1726) {
                val i_4 = anIntArray1728!![i_3 + 1]
                if (i_1 < i_4) {
                    break
                }

                val i_5 = anIntArray1728!![i_3]
                val i_6 = anIntArray1728!![i_3 + 2]
                val i_7 = anIntArray1728!![i_3 + 3]
                val i_8 = (i_6 - i_5 shl 16) / (i_7 - i_4)
                val i_9 = (i_5 shl 16) + 32768
                anIntArray1728!![i_3] = i_9 + i_8 * (i_1 - i_4)
                anIntArray1728!![i_3 + 2] = i_8
                i_3 += 4
            }

            anInt1724 = b_2.toInt()
            anInt1722 = i_3
            anInt1729 = i_3
            currentY = i_1 - 1
        }
    }

    @JvmStatic
    fun clipBottom(i_0: Int): Boolean {
        var i_1 = anInt1722
        var i_2 = anInt1729

        var i_4: Int
        var i_3 = currentY
        while (i_2 >= i_1) {
            ++i_3
            currentY = i_3
            if (i_3 >= i_0) {
                return false
            }

            var i_5: Int
            var i_6: Int
            i_4 = anInt1724
            while (i_1 < anInt1726) {
                i_5 = anIntArray1728!![i_1 + 1]
                if (i_3 < i_5) {
                    break
                }

                i_6 = anIntArray1728!![i_1]
                val i_7 = anIntArray1728!![i_1 + 2]
                val i_8 = anIntArray1728!![i_1 + 3]
                val i_9 = (i_7 - i_6 shl 16) / (i_8 - i_5)
                val i_10 = (i_6 shl 16) + 32768
                anIntArray1728!![i_1] = i_10
                anIntArray1728!![i_1 + 2] = i_9
                i_1 += 4
            }

            i_5 = i_4
            while (i_5 < i_1) {
                i_6 = anIntArray1728!![i_5 + 3]
                if (i_3 >= i_6) {
                    anIntArray1728!![i_5] = anIntArray1728!![i_4]
                    anIntArray1728!![i_5 + 1] = anIntArray1728!![i_4 + 1]
                    anIntArray1728!![i_5 + 2] = anIntArray1728!![i_4 + 2]
                    anIntArray1728!![i_5 + 3] = anIntArray1728!![i_4 + 3]
                    i_4 += 4
                }
                i_5 += 4
            }

            if (i_4 == anInt1726) {
                anInt1726 = 0
                return false
            }

            method2480(i_4, i_1)
            anInt1724 = i_4
            anInt1722 = i_1
            i_2 = i_4
        }

        currentX = anIntArray1728!![i_2] shr 16
        currentWidth = anIntArray1728!![i_2 + 4] shr 16
        anIntArray1728!![i_2] += anIntArray1728!![i_2 + 2]
        anIntArray1728!![i_2 + 4] += anIntArray1728!![i_2 + 6]
        i_2 += 8
        anInt1729 = i_2
        return true
    }

    @JvmStatic
    fun method2480(i_0: Int, i_1: Int) {
        var i_1_mod = i_1
        while (true) {
            if (i_1_mod >= i_0 + 8) {
                var bool2 = true

                var i_3 = i_0 + 4
                while (i_3 < i_1_mod) {
                    var i_4 = anIntArray1728!![i_3 - 4]
                    val i_5 = anIntArray1728!![i_3]
                    if (i_4 > i_5) {
                        bool2 = false
                        anIntArray1728!![i_3 - 4] = i_5
                        anIntArray1728!![i_3] = i_4
                        i_4 = anIntArray1728!![i_3 - 2]
                        anIntArray1728!![i_3 - 2] = anIntArray1728!![i_3 + 2]
                        anIntArray1728!![i_3 + 2] = i_4
                        i_4 = anIntArray1728!![i_3 - 1]
                        anIntArray1728!![i_3 - 1] = anIntArray1728!![i_3 + 3]
                        anIntArray1728!![i_3 + 3] = i_4
                    }
                    i_3 += 4
                }

                if (!bool2) {
                    i_1_mod -= 4
                    continue
                }
            }

            return
        }
    }

    @JvmStatic
    fun method2505(graphicalrenderer0: AbstractToolkit, ints_1: IntArray, i_2: Int, ints_3: IntArray?, ints_4: IntArray?) {
        method2474(graphicalrenderer0, ints_1, ints_1.size, i_2, ints_3, ints_4)
    }
}
