package com.jagex.game.runetek5.config.cursor

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.graphics.sprite.SpriteType

/**
 * Cursor type configuration with sprite and hotspot data.
 *
 * Fully refactored
 * @author Brainpower - 21/05/2020
 */
class CursorType {

    /**
     * The manager of CursorTypes
     */
    @JvmField var cursorTypeList: CursorTypeList? = null

    /**
     * The cursor sprite ID
     */
    @JvmField var spriteId: Int = 0

    /**
     * The position at which the click registers
     * IE: the tip of a point cursor
     */
    @JvmField var hotspotPointX: Int = 0
    @JvmField var hotspotPointY: Int = 0

    /**
     * Reads all data present in the buffer.
     *
     * @param buffer The buffer containing all data about the cursor
     */
    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decodeOpcode(buffer, opcode)
        }
    }

    /**
     * Reads the data for one opcode.
     *
     * @param buffer The buffer containing all data about the cursor
     * @param opcode The opcode to read
     */
    fun decodeOpcode(buffer: Packet, opcode: Int) {
        when (opcode) {
            1 -> spriteId = buffer.readBigSmart()
            2 -> {
                hotspotPointX = buffer.readUByte()
                hotspotPointY = buffer.readUByte()
            }
        }
    }

    /**
     * Fetches and caches the sprite of this cursor.
     *
     * @return The sprite representing this cursor
     */
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
