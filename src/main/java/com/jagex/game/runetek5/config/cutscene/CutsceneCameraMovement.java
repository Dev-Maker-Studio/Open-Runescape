package com.jagex.game.runetek5.config.cutscene;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.client;

public class CutsceneCameraMovement {

    public final int[] anIntArray763;

    public final int[] anIntArray760;

    public final int[] anIntArray762;

    public final int[] anIntArray759;

    public final int[] anIntArray761;

    public final int[] anIntArray764;

    public final int[] anIntArray765;

    CutsceneCameraMovement(Packet rsbytebuffer1) {
        int i_2 = rsbytebuffer1.readUSmart();
        this.anIntArray763 = new int[i_2];
        this.anIntArray760 = new int[i_2];
        this.anIntArray762 = new int[i_2];
        this.anIntArray759 = new int[i_2];
        this.anIntArray761 = new int[i_2];
        this.anIntArray764 = new int[i_2];
        this.anIntArray765 = new int[i_2];
        for (int i_3 = 0; i_3 < i_2; i_3++) {
            this.anIntArray763[i_3] = rsbytebuffer1.readUShort() - 5120;
            this.anIntArray762[i_3] = rsbytebuffer1.readUShort() - 5120;
            this.anIntArray760[i_3] = rsbytebuffer1.readShort();
            this.anIntArray761[i_3] = rsbytebuffer1.readUShort() - 5120;
            this.anIntArray765[i_3] = rsbytebuffer1.readUShort() - 5120;
            this.anIntArray764[i_3] = rsbytebuffer1.readShort();
            this.anIntArray759[i_3] = rsbytebuffer1.readShort();
        }
    }

    public void method1363(int i_1) {
        int[][] ints_3 = new int[this.anIntArray763.length << 1][4];
        for (int i_4 = 0; i_4 < this.anIntArray763.length; i_4++) {
            ints_3[i_4 * 2][0] = this.anIntArray763[i_4];
            ints_3[i_4 * 2][1] = this.anIntArray760[i_4];
            ints_3[i_4 * 2][2] = this.anIntArray762[i_4];
            ints_3[i_4 * 2][3] = this.anIntArray759[i_4];
            ints_3[i_4 * 2 + 1][0] = this.anIntArray761[i_4];
            ints_3[i_4 * 2 + 1][1] = this.anIntArray764[i_4];
            ints_3[i_4 * 2 + 1][2] = this.anIntArray765[i_4];
            ints_3[i_4 * 2 + 1][3] = this.anIntArray759[i_4];
        }
        client.anIntArrayArrayArray7302[i_1] = ints_3;
    }
}
