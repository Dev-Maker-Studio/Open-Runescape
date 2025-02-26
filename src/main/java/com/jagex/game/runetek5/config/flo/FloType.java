package com.jagex.game.runetek5.config.flo;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.utils.ColorUtil;

public class FloType {

    public int slot = 8;
    public int tileColorHsl;
    public int texture = -1;
    public boolean occlude = true;
    public int minimapColorHsl = -1;
    public int textureScale = 512;
    public boolean shadowed = true;
    public boolean blendsWithUnderlay;
    public int waterColor = 1190717;
    public int waterFogDepth = 512;
    public int waterIntensity = 255;
    public int opcode20 = 63;
    public int unusedOpcode21;
    public int unusedOpcode22 = 64;
    int overlayId;
    FloTypeList floTypeList;

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
            this.tileColorHsl = rgbToHsl(stream.readUMedium());
        } else if (opcode == 2) {
            this.texture = stream.readUByte();
        } else if (opcode == 3) {
            this.texture = stream.readUShort();
            if (this.texture == 0xFFFF) {
                this.texture = -1;
            }
        } else if (opcode == 5) {
            this.occlude = false;
        } else if (opcode == 7) {
            this.minimapColorHsl = rgbToHsl(stream.readUMedium());
        } else if (opcode == 8) {
            this.floTypeList.lastLoadedOverlayId = this.overlayId;
        } else if (opcode == 9) {
            this.textureScale = stream.readUShort() << 2;
        } else if (opcode == 10) {
            this.shadowed = false;
        } else if (opcode == 11) {
            this.slot = stream.readUByte();
        } else if (opcode == 12) {
            this.blendsWithUnderlay = true;
        } else if (opcode == 13) {
            this.waterColor = stream.readUMedium();
        } else if (opcode == 14) {
            this.waterFogDepth = (stream.readUByte() << 2);
        } else if (opcode == 16) {
            this.waterIntensity = stream.readUByte();
        } else if (opcode == 20) {
            this.opcode20 = stream.readUShort(); // water scale?
        } else if (opcode == 21) {
            this.unusedOpcode21 = stream.readUByte();
        } else if (opcode == 22) {
            this.unusedOpcode22 = stream.readUShort();
        }
    }
    
    public static int rgbToHsl(int hsl) {
        return hsl == 16711935 ? -1 : ColorUtil.rgbToHsl24(hsl);
    }
    
    void postDecode() {
        this.slot = this.slot << 8 | this.overlayId;
    }
}
