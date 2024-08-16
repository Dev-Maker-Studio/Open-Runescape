package com.jagex.game.runetek5.config.skybox;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.Identifiable;

public class SkyboxType {

    int materialId = -1;
    int defaultSunIndex = -1;
    BackgroundMode backgroundMode;
    int archiveId;
    int[] sunDefinitionIds;

    SkyboxType() {
        this.backgroundMode = BackgroundMode.SINGLE_COLOR;
        this.archiveId = -1;
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

    public void decode(Packet buffer, int opcode) {
        if (opcode == 1) {
            this.materialId = buffer.readUnsignedShort();
        } else if (opcode == 2) {
            this.sunDefinitionIds = new int[buffer.readUnsignedByte()];
            for (int index = 0; index < this.sunDefinitionIds.length; index++) {
                this.sunDefinitionIds[index] = buffer.readUnsignedShort();
            }
        } else if (opcode == 3) {
            this.defaultSunIndex = buffer.readUnsignedByte();
        } else if (opcode == 4) {
            this.backgroundMode = (BackgroundMode) Identifiable.identify(BackgroundMode.values(), buffer.readUnsignedByte());
        } else if (opcode == 5) {
            this.archiveId = buffer.readBigSmart();
        }
    }
}