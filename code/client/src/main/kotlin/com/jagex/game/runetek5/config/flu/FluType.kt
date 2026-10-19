package com.jagex.game.runetek5.config.flu

import com.jagex.game.runetek5.buffer.Packet
import kotlin.math.sqrt

class FluType {

    @JvmField var saturation: Int = 0
    @JvmField var lightness: Int = 0
    @JvmField var hueMultiplier: Int = 0
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

    fun calculateHsl16(packedRgb: Int) {
        val red = (packedRgb shr 16 and 0xff) / 256.0
        val green = (packedRgb shr 8 and 0xff) / 256.0
        val blue = (packedRgb and 0xff) / 256.0
        var min = red
        if (green < red) {
            min = green
        }

        if (blue < min) {
            min = blue
        }

        var max = red
        if (green > red) {
            max = green
        }

        if (blue > max) {
            max = blue
        }

        var h = 0.0
        var s = 0.0
        val l = (max + min) / 2.0
        if (max != min) {
            if (l < 0.5) {
                s = (max - min) / (max + min)
            }

            if (l >= 0.5) {
                s = (max - min) / (2.0 - max - min)
            }

            if (red == max) {
                h = (green - blue) / (max - min)
            } else if (max == green) {
                h = 2.0 + (blue - red) / (max - min)
            } else if (max == blue) {
                h = (red - green) / (max - min) + 4.0
            }
        }

        h /= 6.0
        saturation = (s * 256.0).toInt()
        lightness = (256.0 * l).toInt()
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

        if (l > 0.5) {
            hueMultiplier = (s * (1.0 - l) * 512.0).toInt()
        } else {
            hueMultiplier = (s * l * 512.0).toInt()
        }

        if (hueMultiplier < 1) {
            hueMultiplier = 1
        }

        hue = (h * hueMultiplier).toInt()
    }
}
