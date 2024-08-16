package com.jagex.game.runetek5.toolkit.sw3d;

import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelSw3d;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.ground.GroundSw3d;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.toolkit.SoftwareToolkit;

public class ThreadResourceSw3d implements INativeObject {

    public long nativeid;
    public Runnable aRunnable48;
    public final ModelSw3d[] ahArray49 = new ModelSw3d[7];
    public final ModelSw3d[] ahArray47 = new ModelSw3d[7];
    public final SoftwareToolkit aja50;
    public final Matrix44 aClass38451;

    public ThreadResourceSw3d(SoftwareToolkit ja1, int i_2, int i_3) {
        this.aja50 = ja1;

        for (int i_4 = 0; i_4 < 7; i_4++) {
            this.ahArray49[i_4] = new ModelSw3d(this.aja50);
            this.ahArray47[i_4] = new ModelSw3d(this.aja50);
        }

        this.aClass38451 = new Matrix44();
        ha(this.nativeid, ja1, i_2, i_3);
    }

    public void method307() {
        this.aRunnable48 = Thread.currentThread();
        method310();
    }

    native void ha(long var1, AbstractToolkit var3, int var4, int var5);

    @Override
    protected void finalize() {
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this);
        }

    }

    @Override
    public void destroy(boolean bool1) {
        R(this.nativeid, bool1);
    }

    native void R(long var1, boolean var3);

    native void b(long var1, AbstractToolkit var3, int[] var4, int[] var5, int[] var6, short[] var7, int var8);

    native void ac(long var1, AbstractToolkit var3, int[] var4, int[] var5, int[] var6, short[] var7, int var8);

    public void method309(AbstractToolkit graphicalrenderer1, int[] ints_2, int[] ints_3, int[] ints_4, short[] shorts_5, int i_6) {
        b(this.nativeid, graphicalrenderer1, ints_2, ints_3, ints_4, shorts_5, i_6);
    }

    public void method310() {
        JA(this.nativeid);
    }

    public void method311(AbstractToolkit graphicalrenderer1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8) {
        K(this.nativeid, graphicalrenderer1, i_2, i_3, i_4, i_5, i_6, i_7, i_8);
    }

    public void method313(Model meshrasterizer1, Model meshrasterizer2, int i_3, int i_4, int i_5, boolean bool6) {
        P(this.nativeid, ((ModelSw3d) meshrasterizer1).nativeid, ((ModelSw3d) meshrasterizer2).nativeid, i_3, i_4, i_5, bool6);
    }

    native void aq(long var1, long var3, int[] var5, float[] var6);

    public void method314(Model meshrasterizer1, Matrix43 matrix44var2, int[] ints_3, int i_4) {
        this.aClass38451.fromMatrix(matrix44var2);
        wa(this.nativeid, ((ModelSw3d) meshrasterizer1).nativeid, this.aClass38451.faceList, ints_3, i_4);
    }

    native void wa(long var1, long var3, float[] var5, int[] var6, int var7);

    native boolean ya(long var1, long var3, int var5, int var6, float[] var7, boolean var8);

    native void a(long var1);

    native void J(long var1, long var3, int[] var5, float[] var6);

    public void method316(Ground class3901, int i_2, int i_3) {
        v(this.nativeid, ((GroundSw3d) class3901).nativeid, i_2, i_3);
    }

    public void method318(Ground class3901, int i_2, int i_3, int i_5, int i_6, int i_7, int i_8, boolean[][] bools_9) {
        i(this.nativeid, ((GroundSw3d) class3901).nativeid, i_2, i_3, 1024, i_5, i_6, i_7, i_8, bools_9);
    }

    native void v(long var1, long var3, int var5, int var6);

    native void bs(long var1, long var3, int var5, int var6, int var7, int var8, int var9, int var10, int var11, boolean[][] var12);

    native void s(long var1, AbstractToolkit var3, int var4, int var5);

    native void i(long var1, long var3, int var5, int var6, int var7, int var8, int var9, int var10, int var11, boolean[][] var12);

    native void bz(long var1, long var3, int var5, int var6);

    native void d(long var1);

    public Model method325(ModelSw3d h1, byte b_2, int i_3, boolean bool4) {
        boolean bool5 = false;
        ModelSw3d h6;
        ModelSw3d h7;
        if (b_2 > 0 && b_2 <= 7) {
            h7 = this.ahArray47[b_2 - 1];
            h6 = this.ahArray49[b_2 - 1];
            bool5 = true;
        } else {
            h6 = h7 = new ModelSw3d(this.aja50);
        }

        h1.BA(h6, h7, i_3, bool5, bool4);
        h6.aClass87Array7139 = h1.aClass87Array7139;
        h6.aClass172Array7142 = h1.aClass172Array7142;
        return h6;
    }

    native void au(long var1, AbstractToolkit var3, int[] var4, int[] var5, int[] var6, short[] var7, int var8);

    native void ay(long var1, AbstractToolkit var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10);

    native void at(long var1, AbstractToolkit var3, int[] var4, int[] var5, int[] var6, short[] var7, int var8);

    native void K(long var1, AbstractToolkit var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10);

    native void aw(long var1, long var3, long var5, int var7, int var8, int var9, boolean var10);

    native void az(long var1, long var3, long var5, int var7, int var8, int var9, boolean var10);

    public void method331(Model meshrasterizer1, int[] ints_2, Matrix43 matrix44var3) {
        this.aClass38451.fromMatrix(matrix44var3);
        J(this.nativeid, ((ModelSw3d) meshrasterizer1).nativeid, ints_2, this.aClass38451.faceList);
    }

    public boolean method335(Model meshrasterizer1, int i_2, int i_3, Matrix43 matrix44var4, boolean bool5) {
        this.aClass38451.fromMatrix(matrix44var4);
        return ya(this.nativeid, ((ModelSw3d) meshrasterizer1).nativeid, i_2, i_3, this.aClass38451.faceList, bool5);
    }

    native void ah(long var1, long var3, float[] var5, int[] var6, int var7);

    native void ai(long var1, long var3, float[] var5, int[] var6, int var7);

    native void P(long var1, long var3, long var5, int var7, int var8, int var9, boolean var10);

    native void k(long var1, AbstractToolkit var3, int var4, int var5);

    native void JA(long var1);

    native void bm(long var1, long var3, int var5, int var6, int var7, int var8, int var9, int var10, int var11, boolean[][] var12);

    native void u(long var1, AbstractToolkit var3, int var4, int var5);

}
