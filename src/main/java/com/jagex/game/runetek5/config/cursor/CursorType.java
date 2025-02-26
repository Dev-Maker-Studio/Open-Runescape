package com.jagex.game.runetek5.config.cursor;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;

/**
 * Fully refactored
 * @author Brainpower - 21/05/2020
 *
 */
public class CursorType {

	/**
	 * The manager of CursorTypes
	 */
    CursorTypeList cursorTypeList;
    
    /**
     * The cursor sprite
     */
    public int spriteId;
    
    /**
     * The position at which the click registers
     * IE: the tip of a point cursor
     */
    public int hotspotPointX;
    public int hotspotPointY;

    /**
     * Reads all data present in the buffer
     * @param buffer The buffer containing all data about the cursor
     */
    void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) {
                return;
            }
            decodeOpcode(buffer, opcode);
        }
    }

    /**
     * Reads the data for one opcode
     * @param buffer The buffer containing all data about the cursor
     * @param opcode The opcode to read
     */
    public void decodeOpcode(Packet buffer, int opcode) {
        if (opcode == 1) {
            this.spriteId = buffer.readBigSmart();
        } else if (opcode == 2) {
            this.hotspotPointX = buffer.readUByte();
            this.hotspotPointY = buffer.readUByte();
        }
    }

    /**
     * Fetches and caches the sprite of this cursor
     * @return The sprite representing this cursor
     */
    public synchronized SpriteType getSprite() {
        SpriteType spriteType = (SpriteType) this.cursorTypeList.spriteCache.get(this.spriteId);
        if (spriteType == null) {
            spriteType = SpriteType.listSingle(this.cursorTypeList.spriteResource, this.spriteId, 0);
            if (spriteType != null) {
                this.cursorTypeList.spriteCache.put(spriteType, this.spriteId);
            }
        }
        return spriteType;
    }
}
