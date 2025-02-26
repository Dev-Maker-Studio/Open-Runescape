package com.jagex.game.runetek5.config.defaults;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.DefaultsFile;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class EntityDefaults {

    public int maximumHits = 4;
    public int[] hitOffsetsX;
    public int[] hitOffsetsY;
    public int anInt7030 = 4;
    public int maxHitbars = 4;
    public int anInt7028 = 7;
    public int profilingMesh = -1;
    public boolean aBool7044 = true;
    public boolean npcMessagesEnabled = true;
    public int npcMessageDuration = 2;
    public boolean enablePlayerMessages = true;
    public int playerMessageDuration = 3;
    public int gameWidthDefault = -1;
    public int gameHeightDefault = -1;
    public int loginInterfaceId;
    public int lobbyWindow;
    public short[][] recolorPaletteSrc;
    public short[][][] recolorPaletteDst;

    public EntityDefaults(Js5ResourceProvider index1) {
        decode(new Packet(index1.getFile(DefaultsFile.ENTITY.fileId)));
    }

    public void decode(Packet buffer) {
        boolean loadedOffsets = false;
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) {
                if (!loadedOffsets) {
                    if (this.hitOffsetsX == null) {
                        this.maximumHits = 4;
                        this.hitOffsetsX = new int[4];
                        this.hitOffsetsY = new int[4];
                    }
                    for (opcode = 0; opcode < this.hitOffsetsX.length; opcode++) {
                        this.hitOffsetsX[opcode] = 0;
                        this.hitOffsetsY[opcode] = opcode * 20;
                    }
                }
                return;
            }
            if (opcode == 1) {
                if (this.hitOffsetsX == null) {
                    this.maximumHits = 4;
                    this.hitOffsetsX = new int[4];
                    this.hitOffsetsY = new int[4];
                }
                for (int i_5 = 0; i_5 < this.hitOffsetsX.length; i_5++) {
                    this.hitOffsetsX[i_5] = buffer.readShort();
                    this.hitOffsetsY[i_5] = buffer.readShort();
                }
                loadedOffsets = true;
            } else if (opcode == 2) {
                this.profilingMesh = buffer.readBigSmart();
            } else if (opcode == 3) {
                this.maximumHits = buffer.readUByte();
                this.hitOffsetsX = new int[this.maximumHits];
                this.hitOffsetsY = new int[this.maximumHits];
            } else if (opcode == 4) {
                this.aBool7044 = false;
            } else if (opcode == 5) {
                this.loginInterfaceId = buffer.readUMedium();
            } else if (opcode == 6) {
                this.lobbyWindow = buffer.readUMedium();
            } else if (opcode == 7) {
                this.recolorPaletteSrc = new short[10][4];
                this.recolorPaletteDst = new short[10][4][];
                for (int i_5 = 0; i_5 < 10; i_5++) {
                    for (int i_6 = 0; i_6 < 4; i_6++) {
                        int i_7 = buffer.readUShort();
                        if (i_7 == 65535) {
                            i_7 = -1;
                        }
                        this.recolorPaletteSrc[i_5][i_6] = (short) i_7;
                        int i_8 = buffer.readUShort();
                        this.recolorPaletteDst[i_5][i_6] = new short[i_8];
                        for (int i_9 = 0; i_9 < i_8; i_9++) {
                            int i_10 = buffer.readUShort();
                            if (i_10 == 65535) {
                                i_10 = -1;
                            }
                            this.recolorPaletteDst[i_5][i_6][i_9] = (short) i_10;
                        }
                    }
                }
            } else if (opcode == 8) {
                this.npcMessagesEnabled = false;
            } else if (opcode == 9) {
                this.npcMessageDuration = buffer.readUByte();
            } else if (opcode == 10) {
                this.enablePlayerMessages = false;
            } else if (opcode == 11) {
                this.playerMessageDuration = buffer.readUByte();
            } else if (opcode == 12) {
                this.gameWidthDefault = buffer.readUShort();
                this.gameHeightDefault = buffer.readUShort();
            } else if (opcode == 13) {
                this.maxHitbars = buffer.readUByte();
            } else if (opcode == 14) { // Something with hitbars
                this.anInt7030 = buffer.readUByte();
            } else if (opcode == 15) {
                this.anInt7028 = buffer.readUByte();
            }
        }
    }
}
