package com.jagex.game.runetek5.graphics.texture.water;

class Class370_Sub2 extends WaterTextureGenerator {

    public byte[] aByteArray7759;
    public int anInt7758;
    public int anInt7760;
    public final int[] anIntArray7761;

    Class370_Sub2() {
        super(12, 16, 2);
        this.anIntArray7761 = new int[super.anInt4290];

        for (int i_7 = 0; i_7 < super.anInt4290; i_7++) {
            this.anIntArray7761[i_7] = (short) ((int) (Math.pow((float) 0.45, i_7) * 4096.0D));
        }

    }

    void method12517(int i_1, byte b_2) {
        this.aByteArray7759[this.anInt7758++] = (byte) (((b_2 & 0xff) >> 1) + 127);
    }

    @Override
    void update(int i_1, int i_2) {
        this.anInt7760 += i_2 * this.anIntArray7761[i_1] >> 12;
    }

    @Override
    void clear() {
        this.anInt7758 = 0;
        this.anInt7760 = 0;
    }

    @Override
    void reset() {
        this.anInt7760 = Math.abs(this.anInt7760);
        if (this.anInt7760 >= 4096) {
            this.anInt7760 = 4095;
        }

        method12517(this.anInt7758++, (byte) (this.anInt7760 >> 4));
        this.anInt7760 = 0;
    }
}
