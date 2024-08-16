package com.jagex.game.runetek5.graphics.texture.sampler;

import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

public interface NativeTextureSampler extends TextureSampler {

    boolean method46();

    float method47(float var1);

    void method48(int var1, int var2, int var3, int var4, int[] var5, int var6, int var7);

    void setRepeat(boolean repeatS, boolean repeatT);

    void method52(int var1, int var2, int var3, int var4, byte[] var5, GLFormat var6, int var7, int var8);

    void method53(int var3, int var4, int[] var5);

    boolean enableMipmapping();

    int getTextureWidth();

    int getTextureHeight();

    float method62(float var1);

}
