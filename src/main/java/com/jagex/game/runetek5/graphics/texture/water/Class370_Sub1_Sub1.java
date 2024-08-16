package com.jagex.game.runetek5.graphics.texture.water;

class Class370_Sub1_Sub1 extends Class370_Sub1 {

    public byte[] aByteArray9998;

    Class370_Sub1_Sub1() {
    }

    byte[] getData() {
        this.aByteArray9998 = new byte[524288];
        method6322();
        return this.aByteArray9998;
    }

    @Override
    void set(int i_1, byte b_2) {
        int i_3 = i_1 * 2;
        int i_4 = b_2 & 0xff;
        this.aByteArray9998[i_3++] = -1;
        this.aByteArray9998[i_3] = (byte) (i_4 * 3 >> 5);
    }

}
