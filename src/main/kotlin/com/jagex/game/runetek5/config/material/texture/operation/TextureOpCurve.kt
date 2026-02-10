package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

/**
 * Texture operation that applies curve interpolation.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpCurve : TextureOperation(1, true) {

    @JvmField var controlPoints: Array<IntArray>? = null
    @JvmField var extrapolatedStart: IntArray? = null
    @JvmField var extrapolatedEnd: IntArray? = null
    @JvmField var interpolationMode: Int = 0
    @JvmField val curveLookup: ShortArray = ShortArray(257)

    override fun getMonochromeOutput(i_1: Int): IntArray {
        val ints_3 = monochromeImageCache.getPalette(i_1)
        if (monochromeImageCache.dirty) {
            val ints_4 = getChildMonochromeOutput(0, i_1)

            for (i_5 in 0 until TextureRaster.currentWidth) {
                var i_6 = ints_4[i_5] shr 4
                if (i_6 < 0) {
                    i_6 = 0
                }

                if (i_6 > 256) {
                    i_6 = 256
                }

                ints_3[i_5] = curveLookup[i_6].toInt()
            }
        }

        return ints_3
    }

    fun getControlPoint(i_1: Int): IntArray {
        return when {
            i_1 < 0 -> extrapolatedStart!!
            i_1 >= controlPoints!!.size -> extrapolatedEnd!!
            else -> controlPoints!![i_1]
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
        var i_2: Int
        var i_3: Int
        var i_4: Int
        var ints_5: IntArray
        var ints_6: IntArray
        var i_7: Int
        var i_8: Int
        var i_9: Int
        var i_10: Int

        when (interpolationMode) {
            1 -> {
                for (i_2_loop in 0..256) {
                    i_3 = i_2_loop shl 4

                    i_4 = 1
                    while (i_4 < controlPoints!!.size - 1 && controlPoints!![i_4][0] <= i_3) {
                        i_4++
                    }

                    ints_5 = controlPoints!![i_4 - 1]
                    ints_6 = controlPoints!![i_4]
                    i_7 = (i_3 - ints_5[0] shl 12) / (ints_6[0] - ints_5[0])
                    i_8 = 4096 - TextureRaster.PALETTE_COS!![i_7 shr 5 and 0xff] shr 1
                    i_9 = 4096 - i_8
                    i_10 = i_8 * ints_6[1] + i_9 * ints_5[1] shr 12

                    if (i_10 <= -32768) {
                        i_10 = -32767
                    }

                    if (i_10 >= 32768) {
                        i_10 = 32767
                    }

                    curveLookup[i_2_loop] = i_10.toShort()
                }
            }
            2 -> {
                for (i_2_loop in 0..256) {
                    i_3 = i_2_loop shl 4

                    i_4 = 1
                    while (i_4 < controlPoints!!.size - 1 && controlPoints!![i_4][0] <= i_3) {
                        i_4++
                    }

                    ints_5 = controlPoints!![i_4 - 1]
                    ints_6 = controlPoints!![i_4]
                    i_7 = getControlPoint(i_4 - 2)[1]
                    i_8 = ints_5[1]
                    i_9 = ints_6[1]
                    i_10 = getControlPoint(i_4 + 1)[1]
                    val i_11 = (i_3 - ints_5[0] shl 12) / (ints_6[0] - ints_5[0])
                    val i_12 = i_11 * i_11 shr 12
                    val i_13 = i_8 + (i_10 - i_9 - i_7)
                    val i_14 = i_7 - i_8 - i_13
                    val i_15 = i_9 - i_7
                    val i_17 = i_12 * (i_13 * i_11 shr 12) shr 12
                    val i_18 = i_14 * i_12 shr 12
                    val i_19 = i_11 * i_15 shr 12
                    var i_20 = i_17 + i_18 + i_19 + i_8

                    if (i_20 <= -32768) {
                        i_20 = -32767
                    }

                    if (i_20 >= 32768) {
                        i_20 = 32767
                    }

                    curveLookup[i_2_loop] = i_20.toShort()
                }
            }
            else -> {
                for (i_2_loop in 0..256) {
                    i_3 = i_2_loop shl 4

                    i_4 = 1
                    while (i_4 < controlPoints!!.size - 1 && controlPoints!![i_4][0] <= i_3) {
                        i_4++
                    }

                    ints_5 = controlPoints!![i_4 - 1]
                    ints_6 = controlPoints!![i_4]
                    i_7 = (i_3 - ints_5[0] shl 12) / (ints_6[0] - ints_5[0])
                    i_8 = 4096 - i_7
                    i_9 = i_7 * ints_6[1] + i_8 * ints_5[1] shr 12

                    if (i_9 <= -32768) {
                        i_9 = -32767
                    }

                    if (i_9 >= 32768) {
                        i_9 = 32767
                    }

                    curveLookup[i_2_loop] = i_9.toShort()
                }
            }
        }
    }

    fun computeExtrapolatedEndpoints() {
        val ints_2 = controlPoints!![0]
        val ints_3 = controlPoints!![1]
        val ints_4 = controlPoints!![controlPoints!!.size - 2]
        val ints_5 = controlPoints!![controlPoints!!.size - 1]
        extrapolatedStart = intArrayOf(
            ints_2[0] - ints_3[0] + ints_2[0],
            ints_2[1] - ints_3[1] + ints_2[1]
        )
        extrapolatedEnd = intArrayOf(
            ints_4[0] - ints_5[0] + ints_4[0],
            ints_4[1] - ints_5[1] + ints_4[1]
        )
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        if (opcode == 0) {
            interpolationMode = buffer.readUByte()
            controlPoints = Array(buffer.readUByte()) { IntArray(2) }

            for (i_4 in controlPoints!!.indices) {
                controlPoints!![i_4][0] = buffer.readUShort()
                controlPoints!![i_4][1] = buffer.readUShort()
            }
        }
    }
}
