package com.jagex.game.runetek5.config.hitsplat;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.TextUtils;

// HIT
// MISS (0)
// CRITICAL
// ...?
public class HitSplatType {

    public int fontId = -1;
    public int color = 16777215;
    public boolean hasColor;
    public int anInt2846;
    public int cyclesVisible = 70;
    public int anInt2833;
    public int anInt2847 = -1;
    public int displayType = -1;
    public int anInt2832;
    public HitSplatTypeList typelist;
    public int spriteA = -1;
    public int spriteB = -1;
    public int spriteC = -1;
    public int spriteD = -1;
    public String placementExampleString = "";

    public void decode(Packet stream, int opcode) {
        if (opcode == 1) {
            this.fontId = stream.readBigSmart();
        } else if (opcode == 2) {
            this.color = stream.read24BitUnsignedInteger();
            this.hasColor = true;
        } else if (opcode == 3) {
            this.spriteA = stream.readBigSmart();
        } else if (opcode == 4) {
            this.spriteB = stream.readBigSmart();
        } else if (opcode == 5) {
            this.spriteC = stream.readBigSmart();
        } else if (opcode == 6) {
            this.spriteD = stream.readBigSmart();
        } else if (opcode == 7) {
            this.anInt2846 = stream.readShort();
        } else if (opcode == 8) {
            this.placementExampleString = stream.readGJString();
        } else if (opcode == 9) {
            this.cyclesVisible = stream.readUnsignedShort();
        } else if (opcode == 10) {
            this.anInt2833 = stream.readShort();
        } else if (opcode == 11) {
            this.anInt2847 = 0;
        } else if (opcode == 12) {
            this.displayType = stream.readUnsignedByte();
        } else if (opcode == 13) {
            this.anInt2832 = stream.readShort();
        } else if (opcode == 14) {
            this.anInt2847 = stream.readUnsignedShort();
        }
    }
    
    public void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUnsignedByte();
            if (opcode == 0) {
                return;
            }
            decode(buffer, opcode);
        }
    }
    
    public void generateSprites(AbstractToolkit toolkit) {
        Js5ResourceProvider hitsplatSpriteResource = this.typelist.spriteResource;
        SpriteType spriteType;
        if (this.spriteA >= 0 && this.typelist.spriteCache.get(this.spriteA) == null && hitsplatSpriteResource.isFileCached(this.spriteA)) {
            spriteType = SpriteType.listSingle(hitsplatSpriteResource, this.spriteA);
            this.typelist.spriteCache.put(toolkit.generateSprite(spriteType, true), this.spriteA);
        }

        if (this.spriteC >= 0 && this.typelist.spriteCache.get(this.spriteC) == null && hitsplatSpriteResource.isFileCached(this.spriteC)) {
            spriteType = SpriteType.listSingle(hitsplatSpriteResource, this.spriteC);
            this.typelist.spriteCache.put(toolkit.generateSprite(spriteType, true), this.spriteC);
        }

        if (this.spriteB >= 0 && this.typelist.spriteCache.get(this.spriteB) == null && hitsplatSpriteResource.isFileCached(this.spriteB)) {
            spriteType = SpriteType.listSingle(hitsplatSpriteResource, this.spriteB);
            this.typelist.spriteCache.put(toolkit.generateSprite(spriteType, true), this.spriteB);
        }

        if (this.spriteD >= 0 && this.typelist.spriteCache.get(this.spriteD) == null && hitsplatSpriteResource.isFileCached(this.spriteD)) {
            spriteType = SpriteType.listSingle(hitsplatSpriteResource, this.spriteD);
            this.typelist.spriteCache.put(toolkit.generateSprite(spriteType, true), this.spriteD);
        }

    }

    public String getCenteredText(int hit) {
        String placement = this.placementExampleString;
        while (true) {
            int numberSlot = placement.indexOf("%1");
            if (numberSlot < 0) {
                return placement;
            }
            placement = placement.substring(0, numberSlot) + TextUtils.createNumberCenteredWithinText(hit, false) + placement.substring(numberSlot + 2);
        }
    }

    public NativeSprite method3835(AbstractToolkit graphicalrenderer1) {
        if (this.spriteA < 0) {
            return null;
        } 
        NativeSprite nativeSprite = (NativeSprite) this.typelist.spriteCache.get(this.spriteA);
        if (nativeSprite == null) {
            generateSprites(graphicalrenderer1);
            nativeSprite = (NativeSprite) this.typelist.spriteCache.get(this.spriteA);
        }
        return nativeSprite;
    }
    
    public NativeSprite method3832(AbstractToolkit graphicalrenderer1) {
        if (this.spriteB < 0) {
            return null;
        } 
        NativeSprite hitsplatSprite = (NativeSprite) this.typelist.spriteCache.get(this.spriteB);
        if (hitsplatSprite == null) {
            generateSprites(graphicalrenderer1);
            hitsplatSprite = (NativeSprite) this.typelist.spriteCache.get(this.spriteB);
        }
        return hitsplatSprite;
    }

    public NativeSprite method3836(AbstractToolkit graphicalrenderer1) {
        if (this.spriteC < 0) {
            return null;
        } 
        NativeSprite nativeSprite = (NativeSprite) this.typelist.spriteCache.get(this.spriteC);
        if (nativeSprite == null) {
            generateSprites(graphicalrenderer1);
            nativeSprite = (NativeSprite) this.typelist.spriteCache.get(this.spriteC);
        }
        return nativeSprite;
    }


    public NativeSprite method3838(AbstractToolkit graphicalrenderer1) {
        if (this.spriteD < 0) {
            return null;
        } 
        NativeSprite nativeSprite = (NativeSprite) this.typelist.spriteCache.get(this.spriteD);
        if (nativeSprite == null) {
            generateSprites(graphicalrenderer1);
            nativeSprite = (NativeSprite) this.typelist.spriteCache.get(this.spriteD);
        }
        return nativeSprite;
    }
}
