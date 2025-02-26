package com.jagex.game.runetek5.graphics.model.billboard;

import com.jagex.game.runetek5.buffer.Packet;

public class BillboardType {

    public int materialId = -1;
    
    /**
     * The horizontal size / ingame width
     */
    public int size2d = 64;
    
    /**
     * The vertical size / ingame height
     */
    public int size3d = 64;
    
    /**
     * FIXME: incorrectly named? should be something related to color blending?
	    0 = rectangle
	    1 = circle
	    2 = brightened rectangle
	  > 2 = invalid, hides model billboard is attached to
    */
    public int shape = 2;
    public int blendType = 1;
    public boolean stationary;
    public boolean hasUid;

    public void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) {
                return;
            }
            decode(buffer, opcode);
        }
    }

    public void decode(Packet buffer, int opcode) {
        if (opcode == 1) {
            this.materialId = buffer.readUShort();
            if (this.materialId == 65535) {
                this.materialId = -1;
            }
        } else if (opcode == 2) {
            this.size2d = buffer.readUShort() + 1;
            this.size3d = buffer.readUShort() + 1;
        } else if (opcode == 3) {
            buffer.readByte();
        } else if (opcode == 4) {
            this.shape = buffer.readUByte();
        } else if (opcode == 5) {
            this.blendType = buffer.readUByte();
        } else if (opcode == 6) {
            this.stationary = true;
        } else if (opcode == 7) {
            this.hasUid = true;
        }
    }
}
