package com.jagex.game.runetek5.graphics.map.worldmap

import com.jagex.game.runetek5.toolkit.AbstractToolkit

object WorldMapRasterManager {

    @JvmField
    var edgeBufferSize: Int = 0

    @JvmField
    var edgeBuffer: IntArray? = null

    @JvmField
    var scanEdgeIndex: Int = 0

    @JvmField
    var nextEdgeIndex: Int = 0

    @JvmField
    var activeEdgeStart: Int = 0

    @JvmField
    var scanlineY: Int = 0

    @JvmField
    var scanlineStartX: Int = 0

    @JvmField
    var scanlineEndX: Int = 0

    @JvmStatic
    fun fillPolygon(toolkit: AbstractToolkit, positioning: IntArray, lineColor: Int) {
        rasterizePolygon(toolkit, positioning, positioning.size, lineColor, null, null)
    }

    @JvmStatic
    fun sortEdgesByY(i_0: Int, i_1: Int) {
        if (i_1 > i_0 + 4) {
            var i_2 = i_0
            val i_3 = edgeBuffer!![i_0]
            val i_4 = edgeBuffer!![i_0 + 1]
            val i_5 = edgeBuffer!![i_0 + 2]
            val i_6 = edgeBuffer!![i_0 + 3]

            var i_7 = i_0 + 4
            while (i_7 < i_1) {
                val i_8 = edgeBuffer!![i_7 + 1]
                if (i_8 < i_4) {
                    edgeBuffer!![i_2] = edgeBuffer!![i_7]
                    edgeBuffer!![i_2 + 1] = i_8
                    edgeBuffer!![i_2 + 2] = edgeBuffer!![i_7 + 2]
                    edgeBuffer!![i_2 + 3] = edgeBuffer!![i_7 + 3]
                    i_2 += 4
                    edgeBuffer!![i_7] = edgeBuffer!![i_2]
                    edgeBuffer!![i_7 + 1] = edgeBuffer!![i_2 + 1]
                    edgeBuffer!![i_7 + 2] = edgeBuffer!![i_2 + 2]
                    edgeBuffer!![i_7 + 3] = edgeBuffer!![i_2 + 3]
                }
                i_7 += 4
            }

            edgeBuffer!![i_2] = i_3
            edgeBuffer!![i_2 + 1] = i_4
            edgeBuffer!![i_2 + 2] = i_5
            edgeBuffer!![i_2 + 3] = i_6
            sortEdgesByY(i_0, i_2)
            sortEdgesByY(i_2 + 4, i_1)
        }
    }

    @JvmStatic
    fun rasterizePolygon(toolkit: AbstractToolkit, ints_1: IntArray, i_3: Int, color: Int, ints_5: IntArray?, ints_6: IntArray?) {
        val clipping = IntArray(4)
        toolkit.getClipping(clipping)
        if (ints_5 != null && ints_5.size != clipping[3] - clipping[1]) {
            throw IllegalStateException()
        } else {
            resetEdgeBuffer()
            buildEdgesFromPolygon(ints_1, i_3)
            initializeScanline(clipping[1])

            while (true) {
                var x: Int
                var width: Int
                var y: Int
                while (true) {
                    if (!advanceScanline(clipping[3])) {
                        return
                    }

                    x = scanlineStartX
                    width = scanlineEndX
                    y = scanlineY
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
    fun resetEdgeBuffer() {
        edgeBufferSize = 0
    }

    @JvmStatic
    fun buildEdgesFromPolygon(ints_0: IntArray, i_2: Int) {
        val i_3 = (i_2 shl 1) + edgeBufferSize
        var i_5: Int
        if (edgeBuffer == null || edgeBuffer!!.size < i_3) {
            val ints_8 = IntArray(i_3)

            i_5 = 0
            while (i_5 < edgeBufferSize) {
                ints_8[i_5] = edgeBuffer!![i_5]
                i_5++
            }

            edgeBuffer = ints_8
        }

        val i_4 = i_2 - 2

        i_5 = 0
        while (i_5 < i_2) {
            val i_6 = ints_0[i_4 + 1]
            val i_7 = ints_0[i_5 + 1]
            if (i_6 < i_7) {
                edgeBuffer!![edgeBufferSize++] = ints_0[i_4]
                edgeBuffer!![edgeBufferSize++] = i_6
                edgeBuffer!![edgeBufferSize++] = ints_0[i_5]
                edgeBuffer!![edgeBufferSize++] = i_7
            } else if (i_7 < i_6) {
                edgeBuffer!![edgeBufferSize++] = ints_0[i_5]
                edgeBuffer!![edgeBufferSize++] = i_7
                edgeBuffer!![edgeBufferSize++] = ints_0[i_4]
                edgeBuffer!![edgeBufferSize++] = i_6
            }

            i_5 += 2
        }
    }

    @JvmStatic
    fun initializeScanline(i_0: Int) {
        if (edgeBufferSize < 0) {
            scanEdgeIndex = 0
            nextEdgeIndex = 0
            activeEdgeStart = 0
            scanlineY = 2147483646
        } else {
            sortEdgesByY(0, edgeBufferSize)
            var i_1 = edgeBuffer!![1]
            if (i_1 < i_0) {
                i_1 = i_0
            }

            val b_2: Byte = 0

            var i_3: Int
            i_3 = 0
            while (i_3 < edgeBufferSize) {
                val i_4 = edgeBuffer!![i_3 + 1]
                if (i_1 < i_4) {
                    break
                }

                val i_5 = edgeBuffer!![i_3]
                val i_6 = edgeBuffer!![i_3 + 2]
                val i_7 = edgeBuffer!![i_3 + 3]
                val i_8 = (i_6 - i_5 shl 16) / (i_7 - i_4)
                val i_9 = (i_5 shl 16) + 32768
                edgeBuffer!![i_3] = i_9 + i_8 * (i_1 - i_4)
                edgeBuffer!![i_3 + 2] = i_8
                i_3 += 4
            }

            activeEdgeStart = b_2.toInt()
            nextEdgeIndex = i_3
            scanEdgeIndex = i_3
            scanlineY = i_1 - 1
        }
    }

    @JvmStatic
    fun advanceScanline(i_0: Int): Boolean {
        var i_1 = nextEdgeIndex
        var i_2 = scanEdgeIndex

        var i_4: Int
        var i_3 = scanlineY
        while (i_2 >= i_1) {
            ++i_3
            scanlineY = i_3
            if (i_3 >= i_0) {
                return false
            }

            var i_5: Int
            var i_6: Int
            i_4 = activeEdgeStart
            while (i_1 < edgeBufferSize) {
                i_5 = edgeBuffer!![i_1 + 1]
                if (i_3 < i_5) {
                    break
                }

                i_6 = edgeBuffer!![i_1]
                val i_7 = edgeBuffer!![i_1 + 2]
                val i_8 = edgeBuffer!![i_1 + 3]
                val i_9 = (i_7 - i_6 shl 16) / (i_8 - i_5)
                val i_10 = (i_6 shl 16) + 32768
                edgeBuffer!![i_1] = i_10
                edgeBuffer!![i_1 + 2] = i_9
                i_1 += 4
            }

            i_5 = i_4
            while (i_5 < i_1) {
                i_6 = edgeBuffer!![i_5 + 3]
                if (i_3 >= i_6) {
                    edgeBuffer!![i_5] = edgeBuffer!![i_4]
                    edgeBuffer!![i_5 + 1] = edgeBuffer!![i_4 + 1]
                    edgeBuffer!![i_5 + 2] = edgeBuffer!![i_4 + 2]
                    edgeBuffer!![i_5 + 3] = edgeBuffer!![i_4 + 3]
                    i_4 += 4
                }
                i_5 += 4
            }

            if (i_4 == edgeBufferSize) {
                edgeBufferSize = 0
                return false
            }

            sortActiveEdgesByX(i_4, i_1)
            activeEdgeStart = i_4
            nextEdgeIndex = i_1
            i_2 = i_4
        }

        scanlineStartX = edgeBuffer!![i_2] shr 16
        scanlineEndX = edgeBuffer!![i_2 + 4] shr 16
        edgeBuffer!![i_2] += edgeBuffer!![i_2 + 2]
        edgeBuffer!![i_2 + 4] += edgeBuffer!![i_2 + 6]
        i_2 += 8
        scanEdgeIndex = i_2
        return true
    }

    @JvmStatic
    fun sortActiveEdgesByX(i_0: Int, i_1: Int) {
        var i_1_mod = i_1
        while (true) {
            if (i_1_mod >= i_0 + 8) {
                var bool2 = true

                var i_3 = i_0 + 4
                while (i_3 < i_1_mod) {
                    var i_4 = edgeBuffer!![i_3 - 4]
                    val i_5 = edgeBuffer!![i_3]
                    if (i_4 > i_5) {
                        bool2 = false
                        edgeBuffer!![i_3 - 4] = i_5
                        edgeBuffer!![i_3] = i_4
                        i_4 = edgeBuffer!![i_3 - 2]
                        edgeBuffer!![i_3 - 2] = edgeBuffer!![i_3 + 2]
                        edgeBuffer!![i_3 + 2] = i_4
                        i_4 = edgeBuffer!![i_3 - 1]
                        edgeBuffer!![i_3 - 1] = edgeBuffer!![i_3 + 3]
                        edgeBuffer!![i_3 + 3] = i_4
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
    fun fillPolygonMasked(graphicalrenderer0: AbstractToolkit, ints_1: IntArray, i_2: Int, ints_3: IntArray?, ints_4: IntArray?) {
        rasterizePolygon(graphicalrenderer0, ints_1, ints_1.size, i_2, ints_3, ints_4)
    }
}
