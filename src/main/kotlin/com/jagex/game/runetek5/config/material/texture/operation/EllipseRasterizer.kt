package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.ArrayUtils

class EllipseRasterizer(
    cx: Int,
    cy: Int,
    rx: Int,
    ry: Int,
    fill: Int,
    stroke: Int,
    strokeW: Int
) : TextureShapeRasterizer(fill, stroke, strokeW) {

    val centerX: Int = cx
    val centerY: Int = cy
    val radiusX: Int = rx
    val radiusY: Int = ry

    override fun drawStroked(width: Int, height: Int) {
    }

    override fun drawFilled(width: Int, height: Int) {
        val scaledCenterX = centerX * width shr 12
        val scaledRadiusX = radiusX * width shr 12
        val scaledCenterY = height * centerY shr 12
        val scaledRadiusY = height * radiusY shr 12
        ArrayUtils.drawFilledEllipse(scaledCenterX, scaledCenterY, scaledRadiusX, scaledRadiusY, fillColor)
    }

    override fun drawFilledAndStroked(width: Int, height: Int) {
        val scaledCenterX = centerX * width shr 12
        val scaledRadiusX = radiusX * width shr 12
        val scaledCenterY = height * centerY shr 12
        val scaledRadiusY = height * radiusY shr 12
        drawEllipse(scaledCenterX, scaledCenterY, scaledRadiusX, scaledRadiusY, fillColor, strokeColor, strokeWidth)
    }

    companion object {
        @JvmStatic
        fun decode(buffer: Packet): EllipseRasterizer {
            return EllipseRasterizer(
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readUMedium(),
                buffer.readUMedium(),
                buffer.readUByte()
            )
        }

        @JvmStatic
        fun drawClippedEllipse(cx: Int, cy: Int, outerRx: Int, outerRy: Int, fillColor: Int, strokeColor: Int, thickness: Int) {
            var outerX = 0
            var outerY = outerRy
            var innerX = 0
            val innerRx = outerRx - thickness
            val innerRy = outerRy - thickness
            val outerRxSq = outerRx * outerRx
            val outerRySq = outerRy * outerRy
            val innerRxSq = innerRx * innerRx
            val innerRySq = innerRy * innerRy
            val outerRySq2 = outerRySq shl 1
            val outerRxSq2 = outerRxSq shl 1
            val innerRySq2 = innerRySq shl 1
            val innerRxSq2 = innerRxSq shl 1
            val outerDiam = outerRy shl 1
            val innerDiam = innerRy shl 1
            var outerDecA = outerRySq2 + (1 - outerDiam) * outerRxSq
            var outerDecB = outerRySq - outerRxSq2 * (outerDiam - 1)
            var innerDecA = innerRySq2 + (1 - innerDiam) * innerRxSq
            var innerDecB = innerRySq - innerRxSq2 * (innerDiam - 1)
            val outerRxSq4 = outerRxSq shl 2
            val outerRySq4 = outerRySq shl 2
            val innerRxSq4 = innerRxSq shl 2
            val innerRySq4 = innerRySq shl 2
            var outerGradA = outerRySq2 * 3
            var outerGradB = outerRxSq2 * (outerDiam - 3)
            var innerGradA = innerRySq2 * 3
            var innerGradB = innerRxSq2 * (innerDiam - 3)
            var outerStepA = outerRySq4
            var outerStepB = (outerRy - 1) * outerRxSq4
            var innerStepA = innerRySq4
            var innerStepB = innerRxSq4 * (innerRy - 1)
            var left: Int
            var right: Int
            var innerLeft: Int
            var innerRight: Int
            if (cy >= TextureOpRasterizer.clipMinY && cy <= TextureOpRasterizer.clipMaxY) {
                val row = TextureShapeRasterizer.pixelBuffer!![cy]
                left = ArrayUtils.clamp(cx - outerRx, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                right = ArrayUtils.clamp(cx + outerRx, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                innerLeft = ArrayUtils.clamp(cx - innerRx, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                innerRight = ArrayUtils.clamp(cx + innerRx, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                ArrayUtils.setArrayValuesTo(row, left, innerLeft, strokeColor)
                ArrayUtils.setArrayValuesTo(row, innerLeft, innerRight, fillColor)
                ArrayUtils.setArrayValuesTo(row, innerRight, right, strokeColor)
            }

            while (outerY > 0) {
                val withinInner = outerY <= innerRy
                if (withinInner) {
                    if (innerDecA < 0) {
                        while (innerDecA < 0) {
                            innerDecA += innerGradA
                            innerDecB += innerStepA
                            innerGradA += innerRySq4
                            innerStepA += innerRySq4
                            ++innerX
                        }
                    }

                    if (innerDecB < 0) {
                        innerDecA += innerGradA
                        innerDecB += innerStepA
                        innerGradA += innerRySq4
                        innerStepA += innerRySq4
                        ++innerX
                    }

                    innerDecA += -innerStepB
                    innerDecB += -innerGradB
                    innerGradB -= innerRxSq4
                    innerStepB -= innerRxSq4
                }

                if (outerDecA < 0) {
                    while (outerDecA < 0) {
                        outerDecA += outerGradA
                        outerDecB += outerStepA
                        outerGradA += outerRySq4
                        outerStepA += outerRySq4
                        ++outerX
                    }
                }

                if (outerDecB < 0) {
                    outerDecA += outerGradA
                    outerDecB += outerStepA
                    outerGradA += outerRySq4
                    outerStepA += outerRySq4
                    ++outerX
                }

                outerDecA += -outerStepB
                outerDecB += -outerGradB
                outerGradB -= outerRxSq4
                outerStepB -= outerRxSq4
                --outerY
                left = cy - outerY
                right = outerY + cy
                if (right >= TextureOpRasterizer.clipMinY && left <= TextureOpRasterizer.clipMaxY) {
                    innerLeft = ArrayUtils.clamp(cx + outerX, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                    innerRight = ArrayUtils.clamp(cx - outerX, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                    if (withinInner) {
                        val innerRightEdge = ArrayUtils.clamp(cx + innerX, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                        val innerLeftEdge = ArrayUtils.clamp(cx - innerX, TextureOpRasterizer.clipMinX, TextureOpRasterizer.clipMaxX)
                        var row: IntArray
                        if (left >= TextureOpRasterizer.clipMinY) {
                            row = TextureShapeRasterizer.pixelBuffer!![left]
                            ArrayUtils.setArrayValuesTo(row, innerRight, innerLeftEdge, strokeColor)
                            ArrayUtils.setArrayValuesTo(row, innerLeftEdge, innerRightEdge, fillColor)
                            ArrayUtils.setArrayValuesTo(row, innerRightEdge, innerLeft, strokeColor)
                        }

                        if (right <= TextureOpRasterizer.clipMaxY) {
                            row = TextureShapeRasterizer.pixelBuffer!![right]
                            ArrayUtils.setArrayValuesTo(row, innerRight, innerLeftEdge, strokeColor)
                            ArrayUtils.setArrayValuesTo(row, innerLeftEdge, innerRightEdge, fillColor)
                            ArrayUtils.setArrayValuesTo(row, innerRightEdge, innerLeft, strokeColor)
                        }
                    } else {
                        if (left >= TextureOpRasterizer.clipMinY) {
                            ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![left], innerRight, innerLeft, strokeColor)
                        }

                        if (right <= TextureOpRasterizer.clipMaxY) {
                            ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![right], innerRight, innerLeft, strokeColor)
                        }
                    }
                }
            }
        }

        @JvmStatic
        fun drawUnclippedEllipse(cx: Int, cy: Int, outerRx: Int, outerRy: Int, fillColor: Int, strokeColor: Int, thickness: Int) {
            var outerX = 0
            var outerY = outerRy
            var innerX = 0
            val innerRx = outerRx - thickness
            val innerRy = outerRy - thickness
            val outerRxSq = outerRx * outerRx
            val outerRySq = outerRy * outerRy
            val innerRxSq = innerRx * innerRx
            val innerRySq = innerRy * innerRy
            val outerRySq2 = outerRySq shl 1
            val outerRxSq2 = outerRxSq shl 1
            val innerRySq2 = innerRySq shl 1
            val innerRxSq2 = innerRxSq shl 1
            val outerDiam = outerRy shl 1
            val innerDiam = innerRy shl 1
            var outerDecA = outerRySq2 + (1 - outerDiam) * outerRxSq
            var outerDecB = outerRySq - outerRxSq2 * (outerDiam - 1)
            var innerDecA = innerRySq2 + (1 - innerDiam) * innerRxSq
            var innerDecB = innerRySq - innerRxSq2 * (innerDiam - 1)
            val outerRxSq4 = outerRxSq shl 2
            val outerRySq4 = outerRySq shl 2
            val innerRxSq4 = innerRxSq shl 2
            val innerRySq4 = innerRySq shl 2
            var outerGradA = outerRySq2 * 3
            var outerGradB = outerRxSq2 * (outerDiam - 3)
            var innerGradA = innerRySq2 * 3
            var innerGradB = innerRxSq2 * (innerDiam - 3)
            var outerStepA = outerRySq4
            var outerStepB = (outerRy - 1) * outerRxSq4
            var innerStepA = innerRySq4
            var innerStepB = innerRxSq4 * (innerRy - 1)
            val centerRow = TextureShapeRasterizer.pixelBuffer!![cy]
            ArrayUtils.setArrayValuesTo(centerRow, cx - outerRx, cx - innerRx, strokeColor)
            ArrayUtils.setArrayValuesTo(centerRow, cx - innerRx, cx + innerRx, fillColor)
            ArrayUtils.setArrayValuesTo(centerRow, cx + innerRx, cx + outerRx, strokeColor)

            while (outerY > 0) {
                val withinInner = outerY <= innerRy
                if (withinInner) {
                    if (innerDecA < 0) {
                        while (innerDecA < 0) {
                            innerDecA += innerGradA
                            innerDecB += innerStepA
                            innerGradA += innerRySq4
                            innerStepA += innerRySq4
                            ++innerX
                        }
                    }

                    if (innerDecB < 0) {
                        innerDecA += innerGradA
                        innerDecB += innerStepA
                        innerGradA += innerRySq4
                        innerStepA += innerRySq4
                        ++innerX
                    }

                    innerDecA += -innerStepB
                    innerDecB += -innerGradB
                    innerGradB -= innerRxSq4
                    innerStepB -= innerRxSq4
                }

                if (outerDecA < 0) {
                    while (outerDecA < 0) {
                        outerDecA += outerGradA
                        outerDecB += outerStepA
                        outerGradA += outerRySq4
                        outerStepA += outerRySq4
                        ++outerX
                    }
                }

                if (outerDecB < 0) {
                    outerDecA += outerGradA
                    outerDecB += outerStepA
                    outerGradA += outerRySq4
                    outerStepA += outerRySq4
                    ++outerX
                }

                outerDecA += -outerStepB
                outerDecB += -outerGradB
                outerGradB -= outerRxSq4
                outerStepB -= outerRxSq4
                --outerY
                val topRow = cy - outerY
                val bottomRow = outerY + cy
                val rightEdge = cx + outerX
                val leftEdge = cx - outerX
                if (withinInner) {
                    val innerRightEdge = cx + innerX
                    val innerLeftEdge = cx - innerX
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![topRow], leftEdge, innerLeftEdge, strokeColor)
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![topRow], innerLeftEdge, innerRightEdge, fillColor)
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![topRow], innerRightEdge, rightEdge, strokeColor)
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![bottomRow], leftEdge, innerLeftEdge, strokeColor)
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![bottomRow], innerLeftEdge, innerRightEdge, fillColor)
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![bottomRow], innerRightEdge, rightEdge, strokeColor)
                } else {
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![topRow], leftEdge, rightEdge, strokeColor)
                    ArrayUtils.setArrayValuesTo(TextureShapeRasterizer.pixelBuffer!![bottomRow], leftEdge, rightEdge, strokeColor)
                }
            }
        }

        @JvmStatic
        fun drawCircle(cx: Int, cy: Int, radius: Int, fillColor: Int, strokeColor: Int, thickness: Int) {
            if (cx - radius >= TextureOpRasterizer.clipMinX && cx + radius <= TextureOpRasterizer.clipMaxX && cy - radius >= TextureOpRasterizer.clipMinY && radius + cy <= TextureOpRasterizer.clipMaxY) {
                ArrayUtils.drawUnclippedFilledAndStrokedCircle(cx, cy, radius, fillColor, strokeColor, thickness)
            } else {
                ArrayUtils.drawClippedFilledAndStrokedCircle(cx, cy, radius, fillColor, strokeColor, thickness)
            }
        }

        @JvmStatic
        fun drawEllipse(cx: Int, cy: Int, rx: Int, ry: Int, fillColor: Int, strokeColor: Int, thickness: Int) {
            if (ry == rx) {
                drawCircle(cx, cy, rx, fillColor, strokeColor, thickness)
            } else if (cx - rx >= TextureOpRasterizer.clipMinX && cx + rx <= TextureOpRasterizer.clipMaxX && cy - ry >= TextureOpRasterizer.clipMinY && ry + cy <= TextureOpRasterizer.clipMaxY) {
                drawUnclippedEllipse(cx, cy, rx, ry, fillColor, strokeColor, thickness)
            } else {
                drawClippedEllipse(cx, cy, rx, ry, fillColor, strokeColor, thickness)
            }
        }
    }
}
