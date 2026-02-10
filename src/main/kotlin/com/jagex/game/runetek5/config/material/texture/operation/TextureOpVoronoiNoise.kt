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

    @JvmField var permutationTable: ByteArray = ByteArray(512)
    @JvmField var pointPositions: ShortArray = ShortArray(512)
    @JvmField var randomSeed: Int = 0
    @JvmField var pointJitter: Int = 2048
    @JvmField var cellCountX: Int = 5
    @JvmField var cellCountY: Int = 5
    @JvmField var distanceOutputMode: Int = 2
    @JvmField var distanceMetric: Int = 1

    override fun getMonochromeOutput(index: Int): IntArray {
        val output = monochromeImageCache.getPalette(index)!!
        if (monochromeImageCache.dirty) {
            val verticalPos = TextureRaster.verticalTable!![index] * cellCountY + 2048
            val verticalCell = verticalPos shr 12
            val verticalCellNext = verticalCell + 1

            for (x in 0 until TextureRaster.currentWidth) {
                fourthNearestDistance = Int.MAX_VALUE
                thirdNearestDistance = Int.MAX_VALUE
                StaticMethods.anInt118 = Int.MAX_VALUE
                nearestDistance = Int.MAX_VALUE

                val horizontalPos = TextureRaster.horizontalTable!![x] * cellCountX + 2048
                val horizontalCell = horizontalPos shr 12
                val horizontalCellNext = horizontalCell + 1

                for (cellY in (verticalCell - 1)..verticalCellNext) {
                    val yIndex = permutationTable[(if (cellY >= cellCountY) cellY - cellCountY else cellY) and 0xff].toInt() and 0xff

                    for (cellX in (horizontalCell - 1)..horizontalCellNext) {
                        val pointIndex = 2 * (permutationTable[yIndex + (if (cellX >= cellCountX) cellX - cellCountX else cellX) and 0xff].toInt() and 0xff)
                        var deltaX = horizontalPos - (pointPositions[pointIndex].toInt() + (cellX shl 12))
                        var deltaY = verticalPos - (pointPositions[pointIndex + 1].toInt() + (cellY shl 12))

                        val distance = when (distanceMetric) {
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

                        if (distance < nearestDistance) {
                            fourthNearestDistance = thirdNearestDistance
                            thirdNearestDistance = StaticMethods.anInt118
                            StaticMethods.anInt118 = nearestDistance
                            nearestDistance = distance
                        } else if (distance < StaticMethods.anInt118) {
                            fourthNearestDistance = thirdNearestDistance
                            thirdNearestDistance = StaticMethods.anInt118
                            StaticMethods.anInt118 = distance
                        } else if (distance < thirdNearestDistance) {
                            fourthNearestDistance = thirdNearestDistance
                            thirdNearestDistance = distance
                        } else if (distance < fourthNearestDistance) {
                            fourthNearestDistance = distance
                        }
                    }
                }

                output[x] = when (distanceOutputMode) {
                    0 -> nearestDistance
                    1 -> StaticMethods.anInt118
                    2 -> StaticMethods.anInt118 - nearestDistance
                    3 -> thirdNearestDistance
                    4 -> fourthNearestDistance
                    else -> 0
                }
            }
        }
        return output
    }

    override fun postDecode() {
        permutationTable = ArrayUtils.generateRandomlySeededBytearray(randomSeed)
        initializePointPositions()
    }

    fun initializePointPositions() {
        val random = Random(randomSeed.toLong())
        pointPositions = ShortArray(512)
        if (pointJitter > 0) {
            for (i in 0 until 512) {
                pointPositions[i] = StaticMethods.method7931(random, pointJitter).toShort()
            }
        }
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        when (opcode) {
            0 -> {
                cellCountX = buffer.readUByte()
                cellCountY = cellCountX
            }
            1 -> randomSeed = buffer.readUByte()
            2 -> pointJitter = buffer.readUShort()
            3 -> distanceOutputMode = buffer.readUByte()
            4 -> distanceMetric = buffer.readUByte()
            5 -> cellCountX = buffer.readUByte()
            6 -> cellCountY = buffer.readUByte()
        }
    }

    companion object {
        @JvmStatic
        var nearestDistance: Int = 0

        @JvmStatic
        var thirdNearestDistance: Int = 0

        @JvmStatic
        var fourthNearestDistance: Int = 0
    }
}
