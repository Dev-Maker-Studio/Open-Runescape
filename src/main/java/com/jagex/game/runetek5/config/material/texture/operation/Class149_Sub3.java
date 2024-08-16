package com.jagex.game.runetek5.config.material.texture.operation;

import com.jagex.game.runetek5.buffer.Packet;

public class Class149_Sub3 extends Class149 {

    public final int anInt9376;
    public final int anInt9375;
    public final int anInt9377;
    public final int anInt9379;

    public Class149_Sub3(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        super(-1, i_5, i_6);
        this.anInt9376 = i_1;
        this.anInt9375 = i_2;
        this.anInt9377 = i_3;
        this.anInt9379 = i_4;
    }

    public static Class149_Sub3 decode(Packet rsbytebuffer0) {
        return new Class149_Sub3(rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.readShort(), rsbytebuffer0.read24BitUnsignedInteger(), rsbytebuffer0.readUnsignedByte());
    }

    @Override
    public void method2561(int i_1, int i_2) {
        int i_4 = this.anInt9376 * i_1 >> 12;
        int i_5 = this.anInt9377 * i_1 >> 12;
        int i_6 = i_2 * this.anInt9375 >> 12;
        int i_7 = i_2 * this.anInt9379 >> 12;
        Class149.method6159(i_4, i_6, i_5, i_7, super.anInt1741);
    }

    @Override
    public void method2556(int i_1, int i_2) {
    }

    @Override
    public void method2557(int i_1, int i_2) {
    }

}
