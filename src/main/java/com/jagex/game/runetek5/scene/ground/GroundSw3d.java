package com.jagex.game.runetek5.scene.ground;

import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.SoftwareToolkit;
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d;
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject;
import com.jagex.game.runetek5.toolkit.sw3d.ba;

public class GroundSw3d extends Ground implements INativeObject, AutoCloseable {

    public long nativeid;
    public final NodeCollection aClass4827143 = new NodeCollection();
    public final SoftwareToolkit aja7144;

    public GroundSw3d(SoftwareToolkit ja1, ba ba2, int i_3, int i_4, int[][] ints_5, int[][] ints_6, int i_8, int i_9) {
        super(i_3, i_4, 512, ints_5);
        this.aja7144 = ja1;
        G(this.aja7144, ba2, i_3, i_4, super.tileHeights, ints_6, 512, i_8, i_9);
    }

    native void G(SoftwareToolkit var1, ba var2, int var3, int var4, int[][] var5, int[][] var6, int var7, int var8, int var9);

    native void ap(float[] var1);

    @Override
    public native void setTileShadow(int var1, int var2, int var3);

    @Override
    public void addBlendedTile(int i_1, int i_2, int[] ints_3, int[] ints_4, int[] ints_5, int[] ints_6, int[] ints_7, int[] ints_8, int[] ints_9, int[] ints_10, WaterTile class9011, boolean bool12) {
        ya(i_1, i_2, ints_3, ints_4, ints_5, ints_6, ints_7, ints_8, ints_9, ints_10, class9011.color, class9011.scale, class9011.intensity, bool12);
    }

    @Override
    public void calculateBlockTiles(int i_1, int i_2, int[] ints_3, int[] ints_4, int[] ints_5, int[] ints_6, int[] ints_7, int[] ints_8, int[] ints_9, int[] ints_10, int[] ints_11, int[] ints_12, int[] ints_13, WaterTile class9014) {
        boolean bool16 = false;
        if (ints_10 != null) {
            int[] ints_17 = ints_10;

            for (int i_18 = 0; i_18 < ints_17.length; i_18++) {
                int i_33 = ints_17[i_18];
                if (i_33 != -1) {
                    bool16 = true;
                    break;
                }
            }
        }

        assert ints_10 != null;
        int i_31 = ints_10.length;
        int[] ints_32 = new int[i_31 * 3];
        int[] ints_19 = new int[i_31 * 3];
        int[] ints_20 = new int[i_31 * 3];
        int[] ints_21 = new int[i_31 * 3];
        int[] ints_22 = new int[i_31 * 3];
        int[] ints_23 = ints_11 != null ? new int[i_31 * 3] : null;
        int[] ints_24 = ints_4 != null ? new int[i_31 * 3] : null;
        int[] ints_25 = ints_6 != null ? new int[i_31 * 3] : null;
        int i_26 = 0;

        for (int i_27 = 0; i_27 < i_31; i_27++) {
            int i_28 = ints_7[i_27];
            int i_29 = ints_8[i_27];
            int i_30 = ints_9[i_27];
            ints_32[i_26] = ints_3[i_28];
            ints_19[i_26] = ints_5[i_28];
            ints_20[i_26] = ints_10[i_27];
            ints_21[i_26] = ints_12[i_27];
            ints_22[i_26] = ints_13[i_27];
            if (ints_11 != null) {
                ints_23[i_26] = ints_11[i_27];
            }

            if (ints_4 != null) {
                ints_24[i_26] = ints_4[i_28];
            }

            if (ints_6 != null) {
                ints_25[i_26] = ints_6[i_28];
            }

            ++i_26;
            ints_32[i_26] = ints_3[i_29];
            ints_19[i_26] = ints_5[i_29];
            ints_20[i_26] = ints_10[i_27];
            ints_21[i_26] = ints_12[i_27];
            ints_22[i_26] = ints_13[i_27];
            if (ints_11 != null) {
                ints_23[i_26] = ints_11[i_27];
            }

            if (ints_4 != null) {
                ints_24[i_26] = ints_4[i_29];
            }

            if (ints_6 != null) {
                ints_25[i_26] = ints_6[i_29];
            }

            ++i_26;
            ints_32[i_26] = ints_3[i_30];
            ints_19[i_26] = ints_5[i_30];
            ints_20[i_26] = ints_10[i_27];
            ints_21[i_26] = ints_12[i_27];
            ints_22[i_26] = ints_13[i_27];
            if (ints_11 != null) {
                ints_23[i_26] = ints_11[i_27];
            }

            if (ints_4 != null) {
                ints_24[i_26] = ints_4[i_30];
            }

            if (ints_6 != null) {
                ints_25[i_26] = ints_6[i_30];
            }

            ++i_26;
        }

        if (bool16 || ints_23 != null) {
            ya(i_1, i_2, ints_32, ints_24, ints_19, ints_25, ints_20, ints_23, ints_21, ints_22, class9014.color, class9014.scale, class9014.intensity, false);
        }

    }

    @Override
    public void renderFloor(int i_1, int i_2, int i_3, boolean[][] bools_4, boolean bool5) {
        int i_7 = 0;
        float[] floats_8 = new float[this.aClass4827143.size()];

        for (PointLight class282Sub249 = (PointLight) this.aClass4827143.getFirst(); class282Sub249 != null; class282Sub249 = (PointLight) this.aClass4827143.next()) {
            floats_8[i_7++] = class282Sub249.getAlpha();
        }

        t(floats_8);

        for (int i_13 = 0; i_13 < i_3 + i_3; i_13++) {
            for (int i_10 = 0; i_10 < i_3 + i_3; i_10++) {
                if (bools_4[i_13][i_10]) {
                    int i_11 = i_13 + (i_1 - i_3);
                    int i_12 = i_10 + (i_2 - i_3);
                    if (i_11 >= 0 && i_11 < super.width && i_12 >= 0 && i_12 < super.length) {
                        this.aja7144.nx().method316(this, i_11, i_12);
                    }
                }
            }
        }

    }

    @Override
    public void renderMinimapFloor(int i_1, int i_2, int i_4, int i_5, int i_6, int i_7, boolean[][] bools_8) {
        this.aja7144.nx().method318(this, i_1, i_2, i_4, i_5, i_6, i_7, bools_8);
    }

    @Override
    public native Shadow createShadowAt(int var1, int var2, Shadow var3);

    @Override
    public native void addShadow(Shadow var1, int var2, int var3, int var4);

    @Override
    public native void removeShadow(Shadow var1, int var2, int var3, int var4);

    @Override
    public void addLight(PointLight class282Sub241, int[] ints_2) {
        this.aClass4827143.addFirst(class282Sub241);
        A(class282Sub241.hashCode(), class282Sub241.getX(), class282Sub241.getY(), class282Sub241.getZ(), class282Sub241.getSize2d(), class282Sub241.getColor(), ints_2);
    }

    native void A(int var1, int var2, int var3, int var4, int var5, int var6, int[] var7);

    native void t(float[] var1);



    @Override
    public void close() {
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this);
        }

    }


    native void ya(int var1, int var2, int[] var3, int[] var4, int[] var5, int[] var6, int[] var7, int[] var8, int[] var9, int[] var10, int var11, int var12, int var13, boolean var14);

    @Override
    public boolean hasRenderingShadows(Shadow class282Sub50Sub171, int i_2, int i_3, int i_4) {
        return true;
    }

    @Override
    public native void destroy(boolean var1);

    @Override
    public native void resetLight();

    native void az(SoftwareToolkit var1, ba var2, int var3, int var4, int[][] var5, int[][] var6, int var7, int var8, int var9);

    native void ax(int var1, int var2, int[] var3, int[] var4, int[] var5, int[] var6, int[] var7, int[] var8, int[] var9, int[] var10, int var11, int var12, int var13, boolean var14);

}
