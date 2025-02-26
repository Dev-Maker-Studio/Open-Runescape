package com.jagex.game.runetek5.config.mapsprite;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class MapSpriteType {

    public int fileId;
    public int backgroundColor;
    
    /**
     * Requires adjustment?
     */
    public boolean requiresUpscaling;
    MapSpriteTypeList mapspriteTypeList;

    void decode(Packet stream) {
        while (true) {
            int opcode = stream.readUByte();
            if (opcode == 0) {
                return;
            }
            decode(stream, opcode);
        }
    }

    public void decode(Packet stream, int opcode) {
        if (opcode == 1) {
            this.fileId = stream.readBigSmart();
        } else if (opcode == 2) {
            this.backgroundColor = stream.readUMedium();
        } else if (opcode == 3) {
            this.requiresUpscaling = true;
        } else if (opcode == 4) {
            this.fileId = -1;
        }
    }

    public NativeSprite createRotatedNativeSprite(AbstractToolkit toolkit, int rotation, boolean flippedVertical) {
        long uid = this.fileId | (long) rotation << 16 | (flippedVertical ? 0x40000 : 0) | (long) toolkit.renderId << 19;
        NativeSprite nativeSprite = (NativeSprite) this.mapspriteTypeList.mapspriteCache.get(uid);
        if (nativeSprite != null) {
            return nativeSprite;
        } 
        if (!this.mapspriteTypeList.mapspriteResource.isFileCached(this.fileId)) {
            return null;
        } 
        SpriteType spriteType = SpriteType.listSingle(this.mapspriteTypeList.mapspriteResource, this.fileId, 0);
        if (spriteType != null) {
            spriteType.minY = 0;
            spriteType.borderX = 0;
            spriteType.minX = 0;
            spriteType.borderY = 0;
            if (flippedVertical) {
                spriteType.flipVertical();
            }
            for (int timesRotated = 0; timesRotated < rotation; timesRotated++) {
                spriteType.rotate90DegLeft();
            }
        }
        nativeSprite = toolkit.generateSprite(spriteType, true);
        if (nativeSprite != null) {
            this.mapspriteTypeList.mapspriteCache.put(nativeSprite, uid);
        }
        return nativeSprite;
    }

    public boolean isCached() {
        return this.mapspriteTypeList.mapspriteResource.isFileCached(this.fileId);
    }
}
