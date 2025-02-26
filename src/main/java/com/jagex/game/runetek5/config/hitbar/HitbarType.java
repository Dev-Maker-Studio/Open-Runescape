package com.jagex.game.runetek5.config.hitbar;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class HitbarType {

    public int anInt2446 = 255;
    public int priority = 255;
    public int anInt2439 = -1;
    public int anInt2443 = 70;
    public final int anInt2442 = 1;
    
    /**
     * 
     */
    HitbarTypeList hitbarTypeList;
    
    /**
     * The green par of a hitbar
     */
    public int greenBarSpriteId = -1;
    
    /**
     * The red par of a hitbar
     */
    public int redBarSpriteId = -1;
    
    /**
     * The green par of a hitbar,
     * for players with a P mapdot
     * [identical sprite to regular]
     */
    public int pGreenBarSpriteId = -1;
    
    /**
     * The red par of a hitbar,
     * for players with a P mapdot
     * [identical sprite to regular]
     */
    public int pRedBarSpriteId = -1;

    public void decode(Packet stream, int opcode) {
        if (opcode == 1) {
            stream.readUShort();
        } else if (opcode == 2) {
            this.anInt2446 = stream.readUByte();
        } else if (opcode == 3) {
            this.priority = stream.readUByte();
        } else if (opcode == 4) {
            this.anInt2439 = 0;
        } else if (opcode == 5) {
            this.anInt2443 = stream.readUShort();
        } else if (opcode == 6) {
            stream.readUByte();
        } else if (opcode == 7) {
            this.greenBarSpriteId = stream.readBigSmart();
        } else if (opcode == 8) {
            this.redBarSpriteId = stream.readBigSmart();
        } else if (opcode == 9) {
            this.pGreenBarSpriteId = stream.readBigSmart();
        } else if (opcode == 10) {
            this.pRedBarSpriteId = stream.readBigSmart();
        } else if (opcode == 11) {
            this.anInt2439 = stream.readUShort();
        }
    }

    void decode(Packet stream) {
        while (true) {
            int opcode = stream.readUByte();
            if (opcode == 0) {
                return;
            }
            decode(stream, opcode);
        }
    }

    public void generateSprites(AbstractToolkit toolkit) {
        Js5ResourceProvider resource = this.hitbarTypeList.spriteResource;
        SpriteType spriteType;
        if (this.greenBarSpriteId >= 0 && this.hitbarTypeList.spriteCache.get(this.greenBarSpriteId) == null && resource.isFileCached(this.greenBarSpriteId)) {
            spriteType = SpriteType.listSingle(resource, this.greenBarSpriteId);
            this.hitbarTypeList.spriteCache.put(toolkit.generateSprite(spriteType, true), this.greenBarSpriteId);
        }
        if (this.redBarSpriteId >= 0 && this.hitbarTypeList.spriteCache.get(this.redBarSpriteId) == null && resource.isFileCached(this.redBarSpriteId)) {
            spriteType = SpriteType.listSingle(resource, this.redBarSpriteId);
            this.hitbarTypeList.spriteCache.put(toolkit.generateSprite(spriteType, true), this.redBarSpriteId);
        }
        if (this.pGreenBarSpriteId >= 0 && this.hitbarTypeList.spriteCache.get(this.pGreenBarSpriteId) == null && resource.isFileCached(this.pGreenBarSpriteId)) {
            spriteType = SpriteType.listSingle(resource, this.pGreenBarSpriteId);
            this.hitbarTypeList.spriteCache.put(toolkit.generateSprite(spriteType, true), this.pGreenBarSpriteId);
        }
        if (this.pRedBarSpriteId >= 0 && this.hitbarTypeList.spriteCache.get(this.pRedBarSpriteId) == null && resource.isFileCached(this.pRedBarSpriteId)) {
            spriteType = SpriteType.listSingle(resource, this.pRedBarSpriteId);
            this.hitbarTypeList.spriteCache.put(toolkit.generateSprite(spriteType, true), this.pRedBarSpriteId);
        }
    }
    
    public NativeSprite getGreenBar(AbstractToolkit toolkit) {
        if (this.greenBarSpriteId < 0) {
            return null;
        } 
        NativeSprite hitbarSprite = (NativeSprite) this.hitbarTypeList.spriteCache.get(this.greenBarSpriteId);
        if (hitbarSprite == null) {
            generateSprites(toolkit);
            hitbarSprite = (NativeSprite) this.hitbarTypeList.spriteCache.get(this.greenBarSpriteId);
        }
        return hitbarSprite;
    }

    public NativeSprite getRedBar(AbstractToolkit graphicalrenderer1) {
        if (this.redBarSpriteId < 0) {
            return null;
        } 
        NativeSprite nativeSprite = (NativeSprite) this.hitbarTypeList.spriteCache.get(this.redBarSpriteId);
        if (nativeSprite == null) {
            generateSprites(graphicalrenderer1);
            nativeSprite = (NativeSprite) this.hitbarTypeList.spriteCache.get(this.redBarSpriteId);
        }
        return nativeSprite;
    }
    
    public NativeSprite getPGreenBar(AbstractToolkit toolkit) {
        if (this.pGreenBarSpriteId < 0) {
            return getGreenBar(toolkit);
        } 
        NativeSprite nativeSprite = (NativeSprite) this.hitbarTypeList.spriteCache.get(this.pGreenBarSpriteId);
        if (nativeSprite == null) {
            generateSprites(toolkit);
            nativeSprite = (NativeSprite) this.hitbarTypeList.spriteCache.get(this.pGreenBarSpriteId);
        }
        return nativeSprite;
    }

    public NativeSprite getPRedBar(AbstractToolkit toolkit) {
        if (this.pRedBarSpriteId < 0) {
            return getRedBar(toolkit);
        } 
        NativeSprite nativeSprite = (NativeSprite) this.hitbarTypeList.spriteCache.get(this.pRedBarSpriteId);
        if (nativeSprite == null) {
            generateSprites(toolkit);
            nativeSprite = (NativeSprite) this.hitbarTypeList.spriteCache.get(this.pRedBarSpriteId);
        }
        return nativeSprite;
    }
}
