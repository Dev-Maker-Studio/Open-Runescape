package com.jagex.game.runetek5.config.cursor

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.graphics.sprite.SpriteType

class CursorType {

    @JvmField var cursorTypeList: CursorTypeList? = null

    @JvmField var spriteId: Int = 0

    @JvmField var hotspotPointX: Int = 0
    @JvmField var hotspotPointY: Int = 0

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decodeOpcode(buffer, opcode)
        }
    }

    fun decodeOpcode(buffer: Packet, opcode: Int) {
        when (opcode) {
            1 -> spriteId = buffer.readBigSmart()
            2 -> {
                hotspotPointX = buffer.readUByte()
                hotspotPointY = buffer.readUByte()
            }
        }
    }

    @Synchronized
    fun getSprite(): SpriteType? {
        var spriteType = cursorTypeList!!.spriteCache.get(spriteId.toLong()) as SpriteType?
        if (spriteType == null) {
            spriteType = SpriteType.listSingle(cursorTypeList!!.spriteResource, spriteId, 0)
            if (spriteType != null) {
                cursorTypeList!!.spriteCache.put(spriteType, spriteId.toLong())
            }
        }
        return spriteType
    }
}
