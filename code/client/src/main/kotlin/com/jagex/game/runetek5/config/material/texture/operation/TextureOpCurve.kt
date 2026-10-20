package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

class TextureOpCurve : TextureOperation(1, true) {

    @JvmField var controlPoints: Array<IntArray>? = null
    @JvmField var extrapolatedStart: IntArray? = null
    @JvmField var extrapolatedEnd: IntArray? = null
    @JvmField var interpolationMode: Int = 0
    @JvmField val curveLookup: ShortArray = ShortArray(257)

    override fun getMonochromeOutput(y: Int): IntArray {
        val output = monochromeImageCache.getPalette(y)
        if (monochromeImageCache.dirty) {
            val childPixels = getChildMonochromeOutput(0, y)

            for (x in 0 until TextureRaster.currentWidth) {
                var index = childPixels[x] shr 4
                if (index < 0) {
                    index = 0
                }

                if (index > 256) {
                    index = 256
                }

                output[x] = curveLookup[index].toInt()
            }
        }

        return output
    }

    fun getControlPoint(index: Int): IntArray {
        return when {
            index < 0 -> extrapolatedStart!!
            index >= controlPoints!!.size -> extrapolatedEnd!!
            else -> controlPoints!![index]
        }
    }

    override fun postDecode() {
        if (controlPoints == null) {
            controlPoints = arrayOf(intArrayOf(0, 0), intArrayOf(4096, 4096))
        }

        if (controlPoints!!.size < 2) {
            throw RuntimeException()
        } else {
            if (interpolationMode == 2) {
                computeExtrapolatedEndpoints()
            }

            TextureRaster.caculateTable()
            buildCurveLookup()
        }
    }

    fun buildCurveLookup() {
        var inputValue: Int
        var segment: Int
        var startPoint: IntArray
        var endPoint: IntArray
        var interpParam: Int
        var weightB: Int
        var weightA: Int
        var result: Int

        when (interpolationMode) {
            1 -> {
                for (i in 0..256) {
                    inputValue = i shl 4

                    segment = 1
                    while (segment < controlPoints!!.size - 1 && controlPoints!![segment][0] <= inputValue) {
                        segment++
                    }

                    startPoint = controlPoints!![segment - 1]
                    endPoint = controlPoints!![segment]
                    interpParam = (inputValue - startPoint[0] shl 12) / (endPoint[0] - startPoint[0])
                    weightB = 4096 - TextureRaster.PALETTE_COS!![interpParam shr 5 and 0xff] shr 1
                    weightA = 4096 - weightB
                    result = weightB * endPoint[1] + weightA * startPoint[1] shr 12

                    if (result <= -32768) {
                        result = -32767
                    }

                    if (result >= 32768) {
                        result = 32767
                    }

                    curveLookup[i] = result.toShort()
                }
            }
            2 -> {
                for (i in 0..256) {
                    inputValue = i shl 4

                    segment = 1
                    while (segment < controlPoints!!.size - 1 && controlPoints!![segment][0] <= inputValue) {
                        segment++
                    }

                    startPoint = controlPoints!![segment - 1]
                    endPoint = controlPoints!![segment]
                    interpParam = getControlPoint(segment - 2)[1]
                    weightB = startPoint[1]
                    weightA = endPoint[1]
                    result = getControlPoint(segment + 1)[1]
                    val tParam = (inputValue - startPoint[0] shl 12) / (endPoint[0] - startPoint[0])
                    val tSquared = tParam * tParam shr 12
                    val coeffA = weightB + (result - weightA - interpParam)
                    val coeffB = interpParam - weightB - coeffA
                    val coeffC = weightA - interpParam
                    val cubicTerm = tSquared * (coeffA * tParam shr 12) shr 12
                    val quadTerm = coeffB * tSquared shr 12
                    val linearTerm = tParam * coeffC shr 12
                    var splineResult = cubicTerm + quadTerm + linearTerm + weightB

                    if (splineResult <= -32768) {
                        splineResult = -32767
                    }

                    if (splineResult >= 32768) {
                        splineResult = 32767
                    }

                    curveLookup[i] = splineResult.toShort()
                }
            }
            else -> {
                for (i in 0..256) {
                    inputValue = i shl 4

                    segment = 1
                    while (segment < controlPoints!!.size - 1 && controlPoints!![segment][0] <= inputValue) {
                        segment++
                    }

                    startPoint = controlPoints!![segment - 1]
                    endPoint = controlPoints!![segment]
                    interpParam = (inputValue - startPoint[0] shl 12) / (endPoint[0] - startPoint[0])
                    weightB = 4096 - interpParam
                    weightA = interpParam * endPoint[1] + weightB * startPoint[1] shr 12

                    if (weightA <= -32768) {
                        weightA = -32767
                    }

                    if (weightA >= 32768) {
                        weightA = 32767
                    }

                    curveLookup[i] = weightA.toShort()
                }
            }
        }
    }

    fun computeExtrapolatedEndpoints() {
        val firstPoint = controlPoints!![0]
        val secondPoint = controlPoints!![1]
        val secondToLast = controlPoints!![controlPoints!!.size - 2]
        val lastPoint = controlPoints!![controlPoints!!.size - 1]
        extrapolatedStart = intArrayOf(
            firstPoint[0] - secondPoint[0] + firstPoint[0],
            firstPoint[1] - secondPoint[1] + firstPoint[1]
        )
        extrapolatedEnd = intArrayOf(
            secondToLast[0] - lastPoint[0] + secondToLast[0],
            secondToLast[1] - lastPoint[1] + secondToLast[1]
        )
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        if (opcode == 0) {
            interpolationMode = buffer.readUByte()
            controlPoints = Array(buffer.readUByte()) { IntArray(2) }

            for (i in controlPoints!!.indices) {
                controlPoints!![i][0] = buffer.readUShort()
                controlPoints!![i][1] = buffer.readUShort()
            }
        }
    }
}
