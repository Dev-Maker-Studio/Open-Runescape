package com.jagex.game.runetek5.config.flu

import com.jagex.game.runetek5.buffer.Packet
import kotlin.math.sqrt

class FluType {

    @JvmField var saturation: Int = 0
    @JvmField var lightness: Int = 0
    @JvmField var divisor: Int = 0
    @JvmField var hue: Int = 0
    @JvmField var texture = -1
    @JvmField var scale = 512
    @JvmField var shadowed = true
    @JvmField var occlude = true
    @JvmField var rgb: Int = 0

    fun decode(packet: Packet, opcode: Int) {
        when (opcode) {
            1 -> {
                rgb = packet.readUMedium()
                calculateHsl16(rgb)
            }
            2 -> {
                texture = packet.readUShort()
                if (texture == 65535) {
                    texture = -1
                }
            }
            3 -> scale = packet.readUShort() shl 2
            4 -> shadowed = false
            5 -> occlude = false
        }
    }

    fun decode(packet: Packet) {
        while (true) {
            val opcode = packet.readUByte()
            if (opcode == 0) {
                return
            }
            decode(packet, opcode)
        }
    }

    fun calculateHsl16(rgb: Int) {
        val red = (rgb shr 16 and 0xff) / 256.0
        val green = (rgb shr 8 and 0xff) / 256.0
        val blue = (rgb and 0xff) / 256.0
        var d_9 = red
        if (green < red) {
            d_9 = green
        }

        if (blue < d_9) {
            d_9 = blue
        }

        var d_11 = red
        if (green > red) {
            d_11 = green
        }

        if (blue > d_11) {
            d_11 = blue
        }

        var d_13 = 0.0
        var d_15 = 0.0
        val d_17 = (d_11 + d_9) / 2.0
        if (d_11 != d_9) {
            if (d_17 < 0.5) {
                d_15 = (d_11 - d_9) / (d_11 + d_9)
            }

            if (d_17 >= 0.5) {
                d_15 = (d_11 - d_9) / (2.0 - d_11 - d_9)
            }

            if (red == d_11) {
                d_13 = (green - blue) / (d_11 - d_9)
            } else if (d_11 == green) {
                d_13 = 2.0 + (blue - red) / (d_11 - d_9)
            } else if (d_11 == blue) {
                d_13 = (red - green) / (d_11 - d_9) + 4.0
            }
        }

        d_13 /= 6.0
        saturation = (d_15 * 256.0).toInt()
        lightness = (256.0 * d_17).toInt()
        if (saturation < 0) {
            saturation = 0
        } else if (saturation > 255) {
            saturation = 255
        }

        if (lightness < 0) {
            lightness = 0
        } else if (lightness > 255) {
            lightness = 255
        }

        if (d_17 > 0.5) {
            divisor = (d_15 * (1.0 - d_17) * 512.0).toInt()
        } else {
            divisor = (d_15 * d_17 * 512.0).toInt()
        }

        if (divisor < 1) {
            divisor = 1
        }

        hue = (d_13 * divisor).toInt()
    }
}
