package com.jagex.game.runetek5.graphics.sprite;

import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.toolkit.SoftwareToolkit;
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d;
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject;
import com.jagex.game.runetek5.toolkit.sw3d.wa;

public class SpriteSw3d extends NativeSprite implements INativeObject, FrameBuffer, AutoCloseable {

    public long nativeid;
    public boolean aBool7477;

    public SpriteSw3d(SoftwareToolkit ja1, int[] ints_2, int i_3, int i_4, int i_5, int i_6) {
        RA(ja1, ints_2, i_3, i_4, i_5, i_6, false);
    }

    public SpriteSw3d(SoftwareToolkit ja1, int i_2, int i_3, int i_4, int i_5, boolean bool6) {
        K(ja1, i_2, i_3, i_4, i_5, bool6);
    }

    public SpriteSw3d(SoftwareToolkit ja1, int[] ints_2, byte[] bytes_3, byte[] bytes_4, int i_6, int i_7, int i_8) {
        EA(ja1, ints_2, bytes_3, bytes_4, 0, i_6, i_7, i_8);
    }

    public SpriteSw3d(SoftwareToolkit ja1, int i_2, int i_3, boolean bool4) {
        this.aBool7477 = bool4;
        UA(ja1, i_2, i_3);
    }


    native void e(long var1, float var3, float var4, float var5, float var6, float var7, float var8, int var9, int var10, int var11, int var12);

    @Override
    public void method2750(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        ea(this.nativeid, i_1, i_2, i_3, i_4, i_5, i_6);
    }


    native void K(SoftwareToolkit var1, int var2, int var3, int var4, int var5, boolean var6);

    @Override
    public void close() {
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this);
        }

    }

    @Override
    public void destroy(boolean bool1) {
        M(this.nativeid, bool1);
    }

    @Override
    public void setSize(int i_1, int i_2, int i_3, int i_4) {
        U(this.nativeid, i_1, i_2, i_3, i_4);
    }


    native void RA(SoftwareToolkit var1, int[] var2, int var3, int var4, int var5, int var6, boolean var7);

    @Override
    public int getUseWidth() {
        return da(this.nativeid);
    }

    @Override
    public int getWidth() {
        return da(this.nativeid);
    }

    native int da(long var1);

    @Override
    public int getSpriteWidth() {
        return WA(this.nativeid);
    }

    native int WA(long var1);

    native int GA(long var1);


    native int ba(long var1);

    @Override
    public FrameBuffer getFramebuffer() {
        return this.aBool7477 ? this : null;
    }


    native void ea(long var1, int var3, int var4, int var5, int var6, int var7, int var8);

    @Override
    public void pixelTransfer() {
        j(this.nativeid, 0, 0, 3);
    }

    native void j(long var1, int var3, int var4, int var5);

    @Override
    public void draw(int i_1, int i_2, int i_3, int i_4, int i_5) {
        Y(this.nativeid, i_1, i_2, i_3, i_4, i_5);
    }

    native void Y(long var1, int var3, int var4, int var5, int var6, int var7);

    @Override
    public void method2773(int i_1, int i_2, ComponentSurface class4553, int i_4, int i_5) {
        o(this.nativeid, i_1, i_2, ((wa) class4553).nativeid, i_4, i_5);
    }

    native void o(long var1, int var3, int var4, long var5, int var7, int var8);

    @Override
    public void drawColoredCircle(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7) {
        ha(this.nativeid, i_1, i_2, i_3, i_4, i_5, i_6, i_7, 1);
    }

    @Override
    public void method2772(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7) {
        aa(this.nativeid, i_1, i_2, i_3, i_4, i_5, i_6, i_7);
    }

    native void aa(long var1, int var3, int var4, int var5, int var6, int var7, int var8, int var9);

    @Override
    public void method2766(float f_1, float f_2, float f_3, float f_4, float f_5, float f_6, int i_7, ComponentSurface class4558, int i_9, int i_10) {
        FA(this.nativeid, f_1, f_2, f_3, f_4, f_5, f_6, i_7, ((wa) class4558).nativeid, i_9, i_10, this.aBool7477);
    }

    native void FA(long var1, float var3, float var4, float var5, float var6, float var7, float var8, int var9, long var10, int var12, int var13, boolean var14);

    @Override
    public void deallocate() {
    }



    native void M(long var1, boolean var3);


    @Override
    public int getHeight() {
        return GA(this.nativeid);
    }


    native void F(long var1, int[] var3);

    @Override
    public void method2764(float f_1, float f_2, float f_3, float f_4, float f_5, float f_6, int i_7, int i_8, int i_9) {
        e(this.nativeid, f_1, f_2, f_3, f_4, f_5, f_6, i_7, i_8, i_9, 1);
    }

    native void EA(SoftwareToolkit var1, int[] var2, byte[] var3, byte[] var4, int var5, int var6, int var7, int var8);


    native void U(long var1, int var3, int var4, int var5, int var6);


    native void ha(long var1, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10);


    @Override
    public int getUseHeight() {
        return GA(this.nativeid);
    }

    @Override
    public int getSpriteHeight() {
        return ba(this.nativeid);
    }


    native void UA(SoftwareToolkit var1, int var2, int var3);


    @Override
    public void cacheSizeInto(int[] ints_1) {
        F(this.nativeid, ints_1);
    }
}
