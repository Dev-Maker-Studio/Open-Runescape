package com.jagex.game.runetek5.imageproducer;

import java.awt.*;

import com.jagex.game.runetek5.toolkit.SoftwareToolkit;
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d;
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject;
import com.jagex.game.runetek5.utils.Utils;

public class ImageProducerSw3d extends DelegatingDisplay implements INativeObject {

    public static boolean aBool10291;
    public static long aLong10286;
    public int anInt10288;
    public int anInt10287;
    public long nativeid;
    public Canvas aCanvas10290;
    public final SoftwareToolkit aja10289;

    public ImageProducerSw3d(SoftwareToolkit ja1, Canvas canvas2, int i_3, int i_4) {
        this.aja10289 = ja1;
        this.aCanvas10290 = canvas2;
        this.anInt10288 = i_3;
        this.anInt10287 = i_4;
        n(ja1, this.aCanvas10290, i_3, i_4);
    }

    @Override
    protected void finalize() {
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this);
        }

    }

    @Override
	public boolean reload() {
        this.aja10289.PA(this);
        return true;
    }

    native void n(SoftwareToolkit var1, Canvas var2, int var3, int var4);

    native void H(int var1, int var2, int var3, int var4);

    native void wa(Canvas var1, int var2, int var3);

    @Override
    public int draw() {
        return draw(0, 0);
    }

    @Override
    public void resize(int i_1, int i_2) {
        this.anInt10288 = i_1;
        this.anInt10287 = i_2;
        wa(this.aCanvas10290, i_1, i_2);
        if (this.aja10289.getDisplay() == this) {
            reload();
        }

    }

    @Override
    public int getWidth() {
        return this.anInt10288;
    }

    @Override
    public int draw(int i_1, int i_2) {
        try {
            synchronized (this.aCanvas10290.getTreeLock()) {
                Dimension dimension4 = this.aCanvas10290.getSize();
                H(i_1, i_2, dimension4.width, dimension4.height);
                aBool10291 = false;
                return 0;
            }
        } catch (Exception exception6) {
            method15706(exception6);
            return -1;
        }
    }

    @Override
    public void cleanup() {
        if (this.aja10289.getDisplay() == this) {
            setDepthStencilSurface();
        }

        destroy(true);
        this.nativeid = 0L;
        this.aCanvas10290 = null;
    }

    native void as(int var1, int var2, int var3, int var4);

    native void ai(Canvas var1, int var2, int var3);

    native void ah(Canvas var1, int var2, int var3);

    @Override
    public native void destroy(boolean var1);

    native void ax(int var1, int var2, int var3, int var4);

    public void method15706(Exception exception1) {
        if (!aBool10291) {
            aLong10286 = Utils.time();
            aBool10291 = true;
        } else {
            if (Utils.time() - aLong10286 >= 30000L) {
                throw new RuntimeException(exception1.getMessage());
            }

            this.aCanvas10290.repaint();
        }

    }

    @Override
    public int getHeight() {
        return this.anInt10287;
    }

    @Override
    public boolean setDepthStencilSurface() {
        return true;
    }

    native void az(SoftwareToolkit var1, Canvas var2, int var3, int var4);

    native void ap(int var1, int var2, int var3, int var4);

    native void al(int var1, int var2, int var3, int var4);

}
