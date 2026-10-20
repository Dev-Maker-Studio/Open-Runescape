package com.jagex.game.runetek5.config.material.texture.operation

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.TextureOperation
import com.jagex.game.runetek5.config.material.texture.TextureRaster

class TextureOpColorGradient : TextureOperation(1, false) {
    var colorStops: Array<IntArray>? = null
    val gradientLookup = IntArray(257)

    override fun getColorOutput(y: Int): Array<IntArray> {
        val palette = colorImageCache.getPalette(y)
        if (colorImageCache.dirty) {
            val childPixels = getChildMonochromeOutput(0, y)
            val outRed = palette[0]
            val outGreen = palette[1]
            val outBlue = palette[2]

            for (x in 0 until TextureRaster.currentWidth) {
                var index = childPixels[x] shr 4
                if (index < 0) {
                    index = 0
                }

                if (index > 256) {
                    index = 256
                }

                index = this.gradientLookup[index]
                outRed[x] = (index and 0xff0000) shr 12
                outGreen[x] = (index and 0xff00) shr 4
                outBlue[x] = (index and 0xff) shl 4
            }
        }

        return palette
    }

    fun buildLookupTable() {
        val stopCount = this.colorStops?.size ?: 0
        if (stopCount > 0) {
            for (i in 0..256) {
                var stopIdx = 0
                val inputValue = i shl 4

                var j = 0
                while (j < stopCount && inputValue >= this.colorStops!![j][0]) {
                    ++stopIdx
                    j++
                }

                val interpRed: Int
                val interpGreen: Int
                val currentStop: IntArray
                if (stopIdx < stopCount) {
                    currentStop = this.colorStops!![stopIdx]
                    if (stopIdx > 0) {
                        val prevStop = this.colorStops!![stopIdx - 1]
                        val lerpFactor = (inputValue - prevStop[0] shl 12) / (currentStop[0] - prevStop[0])
                        val invLerp = 4096 - lerpFactor
                        j = lerpFactor * currentStop[1] + invLerp * prevStop[1] shr 12
                        interpRed = lerpFactor * currentStop[2] + invLerp * prevStop[2] shr 12
                        interpGreen = lerpFactor * currentStop[3] + invLerp * prevStop[3] shr 12
                    } else {
                        j = currentStop[1]
                        interpRed = currentStop[2]
                        interpGreen = currentStop[3]
                    }
                } else {
                    currentStop = this.colorStops!![stopCount - 1]
                    j = currentStop[1]
                    interpRed = currentStop[2]
                    interpGreen = currentStop[3]
                }

                var red = j shr 4
                var green = interpRed shr 4
                var blue = interpGreen shr 4

                if (red < 0) {
                    red = 0
                } else if (red > 255) {
                    red = 255
                }

                if (green < 0) {
                    green = 0
                } else if (green > 255) {
                    green = 255
                }

                if (blue < 0) {
                    blue = 0
                } else if (blue > 255) {
                    blue = 255
                }

                this.gradientLookup[i] = red shl 16 or (green shl 8) or blue
            }
        }
    }

    fun loadPresetGradient(presetId: Int) {
        if (presetId != 0) {
            when (presetId) {
                1 -> {
                    this.colorStops = Array(2) { IntArray(4) }
                    this.colorStops!![0][0] = 0
                    this.colorStops!![0][1] = 0
                    this.colorStops!![0][2] = 0
                    this.colorStops!![0][3] = 0
                    this.colorStops!![1][0] = 4096
                    this.colorStops!![1][1] = 4096
                    this.colorStops!![1][2] = 4096
                    this.colorStops!![1][3] = 4096
                }
                2 -> {
                    this.colorStops = Array(8) { IntArray(4) }
                    this.colorStops!![0] = intArrayOf(0, 2650, 2602, 2361)
                    this.colorStops!![1] = intArrayOf(2867, 2313, 1799, 1558)
                    this.colorStops!![2] = intArrayOf(3072, 2618, 1734, 1413)
                    this.colorStops!![3] = intArrayOf(3276, 2296, 1220, 947)
                    this.colorStops!![4] = intArrayOf(3481, 2072, 963, 722)
                    this.colorStops!![5] = intArrayOf(3686, 2730, 2152, 1766)
                    this.colorStops!![6] = intArrayOf(3891, 2232, 1060, 915)
                    this.colorStops!![7] = intArrayOf(4096, 1686, 1413, 1140)
                }
                3 -> {
                    this.colorStops = Array(7) { IntArray(4) }
                    this.colorStops!![0] = intArrayOf(0, 0, 0, 4096)
                    this.colorStops!![1] = intArrayOf(663, 0, 4096, 4096)
                    this.colorStops!![2] = intArrayOf(1363, 0, 4096, 0)
                    this.colorStops!![3] = intArrayOf(2048, 4096, 4096, 0)
                    this.colorStops!![4] = intArrayOf(2727, 4096, 0, 0)
                    this.colorStops!![5] = intArrayOf(3411, 4096, 0, 4096)
                    this.colorStops!![6] = intArrayOf(4096, 0, 0, 4096)
                }
                4 -> {
                    this.colorStops = Array(6) { IntArray(4) }
                    this.colorStops!![0] = intArrayOf(0, 0, 0, 0)
                    this.colorStops!![1] = intArrayOf(1843, 0, 0, 1493)
                    this.colorStops!![2] = intArrayOf(2457, 0, 0, 2939)
                    this.colorStops!![3] = intArrayOf(2781, 0, 1124, 3565)
                    this.colorStops!![4] = intArrayOf(3481, 546, 3084, 4031)
                    this.colorStops!![5] = intArrayOf(4096, 4096, 4096, 4096)
                }
                5 -> {
                    this.colorStops = Array(16) { IntArray(4) }
                    this.colorStops!![0] = intArrayOf(0, 80, 192, 321)
                    this.colorStops!![1] = intArrayOf(155, 321, 449, 562)
                    this.colorStops!![2] = intArrayOf(389, 578, 690, 803)
                    this.colorStops!![3] = intArrayOf(671, 947, 995, 1140)
                    this.colorStops!![4] = intArrayOf(897, 1285, 1397, 1509)
                    this.colorStops!![5] = intArrayOf(1175, 1525, 1429, 1413)
                    this.colorStops!![6] = intArrayOf(1368, 1734, 1461, 1333)
                    this.colorStops!![7] = intArrayOf(1507, 1413, 1525, 1702)
                    this.colorStops!![8] = intArrayOf(1736, 1108, 1590, 2056)
                    this.colorStops!![9] = intArrayOf(2088, 1766, 2056, 2666)
                    this.colorStops!![10] = intArrayOf(2355, 2409, 2586, 3276)
                    this.colorStops!![11] = intArrayOf(2691, 3116, 3148, 3228)
                    this.colorStops!![12] = intArrayOf(3031, 3806, 3710, 3196)
                    this.colorStops!![13] = intArrayOf(3522, 3437, 3421, 3019)
                    this.colorStops!![14] = intArrayOf(3727, 3116, 3148, 3228)
                    this.colorStops!![15] = intArrayOf(4096, 2377, 2505, 2746)
                }
                6 -> {
                    this.colorStops = Array(4) { IntArray(4) }
                    this.colorStops!![0] = intArrayOf(2048, 0, 4096, 0)
                    this.colorStops!![1] = intArrayOf(2867, 4096, 4096, 0)
                    this.colorStops!![2] = intArrayOf(3276, 4096, 4096, 0)
                    this.colorStops!![3] = intArrayOf(4096, 4096, 0, 0)
                }
                else -> throw RuntimeException()
            }
        }
    }

    override fun postDecode() {
        if (this.colorStops == null) {
            loadPresetGradient(1)
        }

        buildLookupTable()
    }

    override fun decodeOpcode(opcode: Int, buffer: Packet) {
        if (opcode == 0) {
            val presetId = buffer.readUByte()
            if (presetId == 0) {
                this.colorStops = Array(buffer.readUByte()) { IntArray(4) }

                for (i in this.colorStops!!.indices) {
                    this.colorStops!![i][0] = buffer.readUShort()
                    this.colorStops!![i][1] = buffer.readUByte() shl 4
                    this.colorStops!![i][2] = buffer.readUByte() shl 4
                    this.colorStops!![i][3] = buffer.readUByte() shl 4
                }
            } else {
                loadPresetGradient(presetId)
            }
        }
    }
}
