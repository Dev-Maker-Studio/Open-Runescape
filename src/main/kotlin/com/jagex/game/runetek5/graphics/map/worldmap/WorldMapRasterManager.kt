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
    fun fillPolygon(toolkit: AbstractToolkit, polygonPoints: IntArray, color: Int) {
        rasterizePolygon(toolkit, polygonPoints, polygonPoints.size, color, null, null)
    }

    @JvmStatic
    fun sortEdgesByY(start: Int, end: Int) {
        if (end > start + 4) {
            var partition = start
            val pivotStartX = edgeBuffer!![start]
            val pivotStartY = edgeBuffer!![start + 1]
            val pivotEndX = edgeBuffer!![start + 2]
            val pivotEndY = edgeBuffer!![start + 3]

            var i = start + 4
            while (i < end) {
                val startY = edgeBuffer!![i + 1]
                if (startY < pivotStartY) {
                    edgeBuffer!![partition] = edgeBuffer!![i]
                    edgeBuffer!![partition + 1] = startY
                    edgeBuffer!![partition + 2] = edgeBuffer!![i + 2]
                    edgeBuffer!![partition + 3] = edgeBuffer!![i + 3]
                    partition += 4
                    edgeBuffer!![i] = edgeBuffer!![partition]
                    edgeBuffer!![i + 1] = edgeBuffer!![partition + 1]
                    edgeBuffer!![i + 2] = edgeBuffer!![partition + 2]
                    edgeBuffer!![i + 3] = edgeBuffer!![partition + 3]
                }
                i += 4
            }

            edgeBuffer!![partition] = pivotStartX
            edgeBuffer!![partition + 1] = pivotStartY
            edgeBuffer!![partition + 2] = pivotEndX
            edgeBuffer!![partition + 3] = pivotEndY
            sortEdgesByY(start, partition)
            sortEdgesByY(partition + 4, end)
        }
    }

    @JvmStatic
    fun rasterizePolygon(toolkit: AbstractToolkit, polygonPoints: IntArray, pointCount: Int, color: Int, rowStartX: IntArray?, rowWidth: IntArray?) {
        val clipping = IntArray(4)
        toolkit.getClipping(clipping)
        if (rowStartX != null && rowStartX.size != clipping[3] - clipping[1]) {
            throw IllegalStateException()
        } else {
            resetEdgeBuffer()
            buildEdgesFromPolygon(polygonPoints, pointCount)
            initializeScanline(clipping[1])

            while (true) {
                var startX: Int
                var endX: Int
                var y: Int
                while (true) {
                    if (!advanceScanline(clipping[3])) {
                        return
                    }

                    startX = scanlineStartX
                    endX = scanlineEndX
                    y = scanlineY
                    if (rowStartX == null) {
                        break
                    }

                    val row = y - clipping[1]
                    var clippedStartX = startX
                    if (clippedStartX < rowStartX[row] + clipping[0]) {
                        clippedStartX = rowStartX[row] + clipping[0]
                    }

                    var clippedEndX = endX
                    if (clippedEndX > rowStartX[row] + rowWidth!![row] + clipping[0]) {
                        clippedEndX = rowStartX[row] + rowWidth[row] + clipping[0]
                    }

                    if (clippedEndX - clippedStartX > 0) {
                        toolkit.drawTransparentHorizontalLine(clippedStartX, y, clippedEndX - clippedStartX, color, 1)
                        break
                    }
                }

                if (rowStartX == null) {
                    toolkit.drawTransparentHorizontalLine(startX, y, endX - startX, color, 1)
                }
            }
        }
    }

    @JvmStatic
    fun resetEdgeBuffer() {
        edgeBufferSize = 0
    }

    @JvmStatic
    fun buildEdgesFromPolygon(polygonPoints: IntArray, pointCount: Int) {
        val requiredSize = (pointCount shl 1) + edgeBufferSize
        var i: Int
        if (edgeBuffer == null || edgeBuffer!!.size < requiredSize) {
            val resized = IntArray(requiredSize)

            i = 0
            while (i < edgeBufferSize) {
                resized[i] = edgeBuffer!![i]
                i++
            }

            edgeBuffer = resized
        }

        val lastPointIndex = pointCount - 2

        i = 0
        while (i < pointCount) {
            val lastY = polygonPoints[lastPointIndex + 1]
            val y = polygonPoints[i + 1]
            if (lastY < y) {
                edgeBuffer!![edgeBufferSize++] = polygonPoints[lastPointIndex]
                edgeBuffer!![edgeBufferSize++] = lastY
                edgeBuffer!![edgeBufferSize++] = polygonPoints[i]
                edgeBuffer!![edgeBufferSize++] = y
            } else if (y < lastY) {
                edgeBuffer!![edgeBufferSize++] = polygonPoints[i]
                edgeBuffer!![edgeBufferSize++] = y
                edgeBuffer!![edgeBufferSize++] = polygonPoints[lastPointIndex]
                edgeBuffer!![edgeBufferSize++] = lastY
            }

            i += 2
        }
    }

    @JvmStatic
    fun initializeScanline(topY: Int) {
        if (edgeBufferSize < 0) {
            scanEdgeIndex = 0
            nextEdgeIndex = 0
            activeEdgeStart = 0
            scanlineY = 2147483646
        } else {
            sortEdgesByY(0, edgeBufferSize)
            var y = edgeBuffer!![1]
            if (y < topY) {
                y = topY
            }

            val firstActiveEdge: Byte = 0

            var edgeIndex: Int
            edgeIndex = 0
            while (edgeIndex < edgeBufferSize) {
                val startY = edgeBuffer!![edgeIndex + 1]
                if (y < startY) {
                    break
                }

                val startX = edgeBuffer!![edgeIndex]
                val endX = edgeBuffer!![edgeIndex + 2]
                val endY = edgeBuffer!![edgeIndex + 3]
                val gradient = (endX - startX shl 16) / (endY - startY)
                val xFixed = (startX shl 16) + 32768
                edgeBuffer!![edgeIndex] = xFixed + gradient * (y - startY)
                edgeBuffer!![edgeIndex + 2] = gradient
                edgeIndex += 4
            }

            activeEdgeStart = firstActiveEdge.toInt()
            nextEdgeIndex = edgeIndex
            scanEdgeIndex = edgeIndex
            scanlineY = y - 1
        }
    }

    @JvmStatic
    fun advanceScanline(maxY: Int): Boolean {
        var nextEdge = nextEdgeIndex
        var scanEdge = scanEdgeIndex

        var activeStart: Int
        var y = scanlineY
        while (scanEdge >= nextEdge) {
            ++y
            scanlineY = y
            if (y >= maxY) {
                return false
            }

            var i: Int
            var edgeEndY: Int
            activeStart = activeEdgeStart
            while (nextEdge < edgeBufferSize) {
                val startY = edgeBuffer!![nextEdge + 1]
                if (y < startY) {
                    break
                }

                val startX = edgeBuffer!![nextEdge]
                val endX = edgeBuffer!![nextEdge + 2]
                val endY = edgeBuffer!![nextEdge + 3]
                val gradient = (endX - startX shl 16) / (endY - startY)
                val xFixed = (startX shl 16) + 32768
                edgeBuffer!![nextEdge] = xFixed
                edgeBuffer!![nextEdge + 2] = gradient
                nextEdge += 4
            }

            i = activeStart
            while (i < nextEdge) {
                edgeEndY = edgeBuffer!![i + 3]
                if (y >= edgeEndY) {
                    edgeBuffer!![i] = edgeBuffer!![activeStart]
                    edgeBuffer!![i + 1] = edgeBuffer!![activeStart + 1]
                    edgeBuffer!![i + 2] = edgeBuffer!![activeStart + 2]
                    edgeBuffer!![i + 3] = edgeBuffer!![activeStart + 3]
                    activeStart += 4
                }
                i += 4
            }

            if (activeStart == edgeBufferSize) {
                edgeBufferSize = 0
                return false
            }

            sortActiveEdgesByX(activeStart, nextEdge)
            activeEdgeStart = activeStart
            nextEdgeIndex = nextEdge
            scanEdge = activeStart
        }

        scanlineStartX = edgeBuffer!![scanEdge] shr 16
        scanlineEndX = edgeBuffer!![scanEdge + 4] shr 16
        edgeBuffer!![scanEdge] += edgeBuffer!![scanEdge + 2]
        edgeBuffer!![scanEdge + 4] += edgeBuffer!![scanEdge + 6]
        scanEdge += 8
        scanEdgeIndex = scanEdge
        return true
    }

    @JvmStatic
    fun sortActiveEdgesByX(start: Int, end: Int) {
        var limit = end
        while (true) {
            if (limit >= start + 8) {
                var sorted = true

                var i = start + 4
                while (i < limit) {
                    var tmp = edgeBuffer!![i - 4]
                    val x = edgeBuffer!![i]
                    if (tmp > x) {
                        sorted = false
                        edgeBuffer!![i - 4] = x
                        edgeBuffer!![i] = tmp
                        tmp = edgeBuffer!![i - 2]
                        edgeBuffer!![i - 2] = edgeBuffer!![i + 2]
                        edgeBuffer!![i + 2] = tmp
                        tmp = edgeBuffer!![i - 1]
                        edgeBuffer!![i - 1] = edgeBuffer!![i + 3]
                        edgeBuffer!![i + 3] = tmp
                    }
                    i += 4
                }

                if (!sorted) {
                    limit -= 4
                    continue
                }
            }

            return
        }
    }

    @JvmStatic
    fun fillPolygonMasked(toolkit: AbstractToolkit, polygonPoints: IntArray, color: Int, rowStartX: IntArray?, rowWidth: IntArray?) {
        rasterizePolygon(toolkit, polygonPoints, polygonPoints.size, color, rowStartX, rowWidth)
    }
}
