package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.utils.ArrayUtils
import java.util.Random
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Procedural Voronoi noise texture generator.
 *
 * @author Steven - 30/11/2023
 * @author OpenOSRS - naming
 */
class TextureOpVoronoiNoise : TextureOperation(0, true) {

    @JvmField var aByteArray9965: ByteArray = ByteArray(512)
    @JvmField var aShortArray9966: ShortArray = ShortArray(512)
    @JvmField var randomSeed: Int = 0
    @JvmField var anInt9960: Int = 2048
    @JvmField var anInt9961: Int = 5
    @JvmField var anInt9962: Int = 5
    @JvmField var anInt9963: Int = 2
    @JvmField var anInt9949: Int = 1

    override fun getMonochromeOutput(index: Int): IntArray {
        val output = monochromeImageCache.getPalette(index)!!
        if (monochromeImageCache.dirty) {
            val verticalPos = TextureRaster.verticalTable!![index] * anInt9962 + 2048
            val verticalCell = verticalPos shr 12
            val verticalCellNext = verticalCell + 1

            for (x in 0 until TextureRaster.currentWidth) {
                anInt10003 = Int.MAX_VALUE
                anInt4777 = Int.MAX_VALUE
                StaticMethods.anInt118 = Int.MAX_VALUE
                anInt4266 = Int.MAX_VALUE

                val horizontalPos = TextureRaster.horizontalTable!![x] * anInt9961 + 2048
                val horizontalCell = horizontalPos shr 12
                val horizontalCellNext = horizontalCell + 1

                for (cellY in (verticalCell - 1)..verticalCellNext) {
                    val yIndex = aByteArray9965[(if (cellY >= anInt9962) cellY - anInt9962 else cellY) and 0xff].toInt() and 0xff

                    for (cellX in (horizontalCell - 1)..horizontalCellNext) {
                        val pointIndex = 2 * (aByteArray9965[yIndex + (if (cellX >= anInt9961) cellX - anInt9961 else cellX) and 0xff].toInt() and 0xff)
                        var deltaX = horizontalPos - (aShortArray9966[pointIndex].toInt() + (cellX shl 12))
                        var deltaY = verticalPos - (aShortArray9966[pointIndex + 1].toInt() + (cellY shl 12))

                        val distance = when (anInt9949) {
                            1 -> (deltaY * deltaY + deltaX * deltaX) shr 12
                            2 -> (if (deltaX < 0) -deltaX else deltaX) + (if (deltaY < 0) -deltaY else deltaY)
                            3 -> {
                                deltaX = if (deltaX < 0) -deltaX else deltaX
                                deltaY = if (deltaY < 0) -deltaY else deltaY
                                max(deltaX, deltaY)
                            }
                            4 -> {
                                deltaX = (sqrt((if (deltaX < 0) -deltaX else deltaX) / 4096.0) * 4096.0).toInt()
                                deltaY = (sqrt((if (deltaY < 0) -deltaY else deltaY) / 4096.0) * 4096.0).toInt()
                                val sum = deltaX + deltaY
                                (sum * sum) shr 12
                            }
                            5 -> {
                                deltaX *= deltaX
                                deltaY *= deltaY
                                (sqrt(sqrt((deltaY + deltaX) / 1.6777216E7)) * 4096.0).toInt()
                            }
                            else -> {
                                (sqrt((deltaX * deltaX + deltaY * deltaY) / 1.6777216E7) * 4096.0).toInt()
                            }
                        }

                        if (distance < anInt4266) {
                            anInt10003 = anInt4777
                            anInt4777 = StaticMethods.anInt118
                            StaticMethods.anInt118 = anInt4266
                            anInt4266 = distance
                        } else if (distance < StaticMethods.anInt118) {
                            anInt10003 = anInt4777
                            anInt4777 = StaticMethods.anInt118
                            StaticMethods.anInt118 = distance
                        } else if (distance < anInt4777) {
                            anInt10003 = anInt4777
                            anInt4777 = distance
                        } else if (distance < anInt10003) {
                            anInt10003 = distance
                        }
                    }
                }

                output[x] = when (anInt9963) {
                    0 -> anInt4266
                    1 -> StaticMethods.anInt118
                    2 -> StaticMethods.anInt118 - anInt4266
                    3 -> anInt4777
                    4 -> anInt10003
                    else -> 0
                }
            }
        }
        return output
    }

    override fun postDecode() {
        aByteArray9965 = ArrayUtils.generateRandomlySeededBytearray(randomSeed)
        initializePointPositions()
    }

    fun initializePointPositions() {
        val random = Random(randomSeed.toLong())
        aShortArray9966 = ShortArray(512)
        if (anInt9960 > 0) {
            for (i in 0 until 512) {
                aShortArray9966[i] = StaticMethods.method7931(random, anInt9960).toShort()
            }
        }
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> {
                anInt9961 = buffer.readUByte()
                anInt9962 = anInt9961
            }
            1 -> randomSeed = buffer.readUByte()
            2 -> anInt9960 = buffer.readUShort()
            3 -> anInt9963 = buffer.readUByte()
            4 -> anInt9949 = buffer.readUByte()
            5 -> anInt9961 = buffer.readUByte()
            6 -> anInt9962 = buffer.readUByte()
        }
    }

    companion object {
        @JvmStatic
        var anInt4266: Int = 0

        @JvmStatic
        var anInt4777: Int = 0

        @JvmStatic
        var anInt10003: Int = 0
    }
}
