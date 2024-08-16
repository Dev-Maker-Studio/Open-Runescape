package com.jagex.game.runetek5.graphics.texture.water;

class Class370_Sub1_Sub2 extends Class370_Sub1 {

    public byte[] aByteArray10215;

    Class370_Sub1_Sub2() {
    }

    byte[] method15614() {
        this.aByteArray10215 = new byte[524288];
        method6322();
        return this.aByteArray10215;
    }

    @Override
    void set(int i_1, byte b_2) {
        int i_3 = i_1 * 2;
        int i_4 = b_2 & 0xff;
        this.aByteArray10215[i_3++] = (byte) (i_4 * 3 >> 5);
        this.aByteArray10215[i_3] = (byte) (i_4 * 3 >> 5);
    }

}
