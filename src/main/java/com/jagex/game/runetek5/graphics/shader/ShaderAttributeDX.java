package com.jagex.game.runetek5.graphics.shader;

public class ShaderAttributeDX extends ShaderAttribute {

    public final int[] anIntArray10306;
    public final int[] anIntArray10307;
    public final ShaderDX aClass101Sub210305;

    public ShaderAttributeDX(ShaderDX class101Sub21, ShaderVariableDecoder class1222) {
        super(class1222);
        this.aClass101Sub210305 = class101Sub21;
        this.anIntArray10306 = class1222.anIntArray1531;
        this.anIntArray10307 = class1222.anIntArray1534;
    }

    @Override
    public boolean loadUniformLocation(int i_1) {
        return true;
    }

    public int method15782() {
        return this.anIntArray10306[this.aClass101Sub210305.getOccupyingSlot()];
    }

    public int method15783() {
        return this.anIntArray10307[this.aClass101Sub210305.getOccupyingSlot()];
    }

    @Override
    public int getUniformLocation(int i_1) {
        int i_2 = this.anIntArray10306[i_1];
        int i_3 = this.anIntArray10307[i_1];
        return i_2 != -1 ? i_2 : (i_3 != -1 ? i_3 | 0x10000 : -1);
    }
}
