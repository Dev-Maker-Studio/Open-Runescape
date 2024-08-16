package com.jagex.game.runetek5.graphics.font;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.toolkit.SoftwareToolkit;
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d;
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject;
import com.jagex.game.runetek5.toolkit.sw3d.ba;

/**
 * The default font renderer for enhanced software mode, doesn't support alpha
 * @author Brainpower
 *
 */
public class FontRendererSw3d extends AbstractFontRenderer implements INativeObject {

    public long nativeid;

    public FontRendererSw3d(SoftwareToolkit ja1, ba ba2, FontMetrics fontmetrics3, SpriteType[] arr_4) {
        super(ja1, fontmetrics3);
        byte[][] bytes_6 = new byte[arr_4.length][];
        int[] ints_7 = new int[arr_4.length];
        int[] ints_8 = new int[arr_4.length];
        int[] ints_9 = new int[arr_4.length];
        int[] ints_10 = new int[arr_4.length];

        for (int i_11 = 0; i_11 < arr_4.length; i_11++) {
            bytes_6[i_11] = arr_4[i_11].pixels;
            ints_7[i_11] = arr_4[i_11].width;
            ints_8[i_11] = arr_4[i_11].height;
            ints_9[i_11] = arr_4[i_11].minX;
            ints_10[i_11] = arr_4[i_11].minY;
        }

        EA(ja1, ba2, bytes_6, arr_4[0].pallete, ints_7, ints_8, ints_9, ints_10);
    }

    native void EA(SoftwareToolkit var1, ba var2, byte[][] var3, int[] var4, int[] var5, int[] var6, int[] var7, int[] var8);

    @Override
    protected void finalize() {
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this);
        }

    }

    @Override
    public native void drawChar(int var1, int var2, int var3, int var4, boolean var5);

    native void GA(int var1, int var2, int var3, int var4, boolean var5, ComponentSurface var6, int var7, int var8);

    native void n(SoftwareToolkit var1, ba var2, byte[][] var3, int[] var4, int[] var5, int[] var6, int[] var7, int[] var8);

    @Override
    public native void destroy(boolean var1);

    @Override
    public void drawChar(int var_1, int i_2, int i_3, int i_4, boolean bool5, ComponentSurface class4556, int i_7, int i_8) {
        GA(var_1, i_2, i_3, i_4, bool5, class4556, i_7, i_8);
    }

    native void ae(SoftwareToolkit var1, ba var2, byte[][] var3, int[] var4, int[] var5, int[] var6, int[] var7, int[] var8);

    native void aq(char var1, int var2, int var3, int var4, boolean var5, ComponentSurface var6, int var7, int var8);

    native void av(char var1, int var2, int var3, int var4, boolean var5, ComponentSurface var6, int var7, int var8);

}
