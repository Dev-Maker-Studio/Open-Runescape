package com.jagex.game.runetek5.config.flo

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.utils.ColorUtil

class FloType {

    @JvmField var slot: Int = 8
    @JvmField var tileColorHsl: Int = 0
    @JvmField var texture: Int = -1
    @JvmField var occlude: Boolean = true
    @JvmField var minimapColorHsl: Int = -1
    @JvmField var textureScale: Int = 512
    @JvmField var shadowed: Boolean = true
    @JvmField var blendsWithUnderlay: Boolean = false
    @JvmField var waterColor: Int = 1190717
    @JvmField var waterFogDepth: Int = 512
    @JvmField var waterIntensity: Int = 255
    @JvmField var opcode20: Int = 63
    @JvmField var unusedOpcode21: Int = 0
    @JvmField var unusedOpcode22: Int = 64
    @JvmField var overlayId: Int = 0
    @JvmField var floTypeList: FloTypeList? = null

    fun decode(stream: Packet) {
        while (true) {
            val opcode = stream.readUByte()
            if (opcode == 0) {
                return
            }
            decode(stream, opcode)
        }
    }

    fun decode(stream: Packet, opcode: Int) {
        when (opcode) {
            1 -> tileColorHsl = rgbToHsl(stream.readUMedium())
            2 -> texture = stream.readUByte()
            3 -> {
                texture = stream.readUShort()
                if (texture == 0xFFFF) {
                    texture = -1
                }
            }
            5 -> occlude = false
            7 -> minimapColorHsl = rgbToHsl(stream.readUMedium())
            8 -> floTypeList!!.lastLoadedOverlayId = overlayId
            9 -> textureScale = stream.readUShort() shl 2
            10 -> shadowed = false
            11 -> slot = stream.readUByte()
            12 -> blendsWithUnderlay = true
            13 -> waterColor = stream.readUMedium()
            14 -> waterFogDepth = stream.readUByte() shl 5
            16 -> waterIntensity = stream.readUByte()
            20 -> opcode20 = stream.readUShort()
            21 -> unusedOpcode21 = stream.readUByte()
            22 -> unusedOpcode22 = stream.readUShort()
        }
    }

    fun postDecode() {
        slot = slot shl 8 or overlayId
    }

    companion object {
        @JvmStatic
        fun rgbToHsl(hsl: Int): Int {
            return if (hsl == 16711935) -1 else ColorUtil.rgbToHsl24(hsl)
        }
    }
}
