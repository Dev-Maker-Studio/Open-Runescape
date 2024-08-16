package com.jagex.game.runetek5.config.sun;

import com.jagex.game.runetek5.buffer.Packet;

public class SunType {

    public int positionOffsetStep = 8;
    public boolean invertPosition;
    public int positionX;
    public int positionY;
    public int positionZ;
    public int actionType;
    public int materialId;
    public int displayColor = 16777215;
    public int rotationX;
    public int rotationY;
    public int rotationZ;

    public void decode(Packet stream, int opcode) {
        if (opcode == 1) {
            this.positionOffsetStep = stream.readUnsignedShort();
        } else if (opcode == 2) {
            this.invertPosition = true;
        } else if (opcode == 3) {
            this.positionX = stream.readShort();
            this.positionY = stream.readShort();
            this.positionZ = stream.readShort();
        } else if (opcode == 4) {
            this.actionType = stream.readUnsignedByte();
        } else if (opcode == 5) {
            this.materialId = stream.readBigSmart();
        } else if (opcode == 6) {
            this.displayColor = stream.read24BitUnsignedInteger();
        } else if (opcode == 7) {
            this.rotationX = stream.readShort();
            this.rotationY = stream.readShort();
            this.rotationZ = stream.readShort();
        }
    }

    void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUnsignedByte();
            if (opcode == 0) {
                return;
            }
            decode(buffer, opcode);
        }
    }
}
