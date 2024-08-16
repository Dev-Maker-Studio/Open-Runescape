package com.jagex.game.runetek5.graphics.texture.water;

class Class370_Sub2_Sub1 extends Class370_Sub2 {

    public byte[] data;

    Class370_Sub2_Sub1() {
    }

    byte[] getData() {
        this.data = new byte[524288];
        method6322();
        return this.data;
    }
    
    @Override
    void method12517(int i_1, byte b_2) {
        int i_3 = i_1 * 2;
        b_2 = (byte) (((b_2 & 0xff) >> 1) + 127);
        this.data[i_3++] = b_2;
        this.data[i_3] = b_2;
    }
}
