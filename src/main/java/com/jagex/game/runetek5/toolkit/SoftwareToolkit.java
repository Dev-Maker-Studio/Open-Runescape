package com.jagex.game.runetek5.toolkit;

import java.awt.*;

import com.jagex.game.runetek5.unidentified.*;
import com.jagex.game.runetek5.buffer.NativeMemoryNode;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;
import com.jagex.game.runetek5.framebuffer.SoftwareRenderBuffer;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.font.FontRendererSw3d;
import com.jagex.game.runetek5.graphics.font.OpaqueFontRendererSw3d;
import com.jagex.game.runetek5.graphics.font.TransparentFontRendererSw3d;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelSw3d;
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper;
import com.jagex.game.runetek5.graphics.model.particle.PointEntity;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteSw3d;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator;
import com.jagex.game.runetek5.imageproducer.DelegatingDisplay;
import com.jagex.game.runetek5.imageproducer.DirectDisplay;
import com.jagex.game.runetek5.imageproducer.ImageProducerSw3d;
import com.jagex.game.runetek5.imageproducer.ImageProducerSw3d1;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.math.Quaternion;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.rendering.RenderInfo;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.ground.GroundSw3d;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d;
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject;
import com.jagex.game.runetek5.toolkit.sw3d.ThreadResourceSw3d;
import com.jagex.game.runetek5.toolkit.sw3d.ba;
import com.jagex.game.runetek5.toolkit.sw3d.wa;
import com.jagex.game.runetek5.utils.ColorUtil;

/**
 * EnhancedSoftwareRenderer / ToolkitSw3d
 * @author Brainpower
 *
 */
public class SoftwareToolkit extends AbstractToolkit implements INativeObject {

    public static final int[] ab = new int[Math.max(Math.max(104, 20), 24573)];
    public static final float[] ar = new float[20];
    public static final int[] ae = new int[6];
    public static final int[] aq;
    public static final float[] av;
    public static final int[] aa;
    public static final int[] bi;
    public static final int[] bw;
    public static final int[] bx;
    public static final short[] bz;
    public static final byte[] bj;

    static {
        aq = ab;
        av = ar;
        aa = ab;
        bi = ab;
        bw = new int[8191];
        bx = new int[8191];
        bz = new short[8191];
        bj = new byte[8191];
    }

    public ba z;
    public ThreadResourceSw3d[] ai;
    public Matrix44 af;
    public boolean bm;
    public boolean b;
    public float az;
    public float ap;
    public float ax;
    public float al;
    public int at = 4096;
    public int ac = 4096;
    public int as;
    public Matrix43 ay;
    public Matrix44 an;
    public long nativeid;
    public final NodeCollection au = new NodeCollection();
    public Matrix43 ag;
    public Matrix44 ao;
    public Matrix44 aw;

    public SoftwareToolkit(Canvas canvas1, MaterialProvider interface222, int i_3, int i_4) {
        super(interface222);

        try {
            if (!AbstractToolkit.getNativeLibraryLoader().loadIntoMemory("sw3d")) {
                throw new RuntimeException("");
            }
            FinalizeCacheSw3d.incrementCapacity();
            q(super.materialProvider, super.materialProvider.getSize(), 0);
            ColorUtil.initializeTables(false, true);
            this.bm = true;
            this.af = new Matrix44();
            new Vector3();
            this.ag = new Matrix43();
            new Quaternion();
            this.ao = new Matrix44();
            this.an = new Matrix44();
            this.aw = new Matrix44();
            setSunPosition(new Matrix43());
            setModelMatrix(new Matrix44());
            nu(1);
            ni();
            if (canvas1 != null) {
                ensureImageproducerCreated(canvas1, i_3, i_4);
                setActiveCanvas(canvas1);
            }

            int i_5 = super.materialProvider.getSize();

            for (short s_6 = 0; s_6 < i_5; s_6++) {
                MaterialType class1697 = super.materialProvider.getMaterial(s_6);
                if (class1697 != null) {
                    V(s_6, class1697.colorHsl, class1697.effectCombiner, class1697.effectId, class1697.effectParam1, class1697.effectParam2, class1697.isSmall, class1697.alpha, class1697.brightness, class1697.textureSpeedU, class1697.textureSpeedV, class1697.detailsOnly, class1697.skipTriangles, class1697.aBool2087, class1697.repeatS, class1697.repeatT, class1697.mipmapping, class1697.hdr, class1697.isBrickTile, class1697.combineMode);
                }
            }

        } catch (Throwable throwable) {
            throwable.printStackTrace(); // Added to debug unsatisfiedLinkErrors
            cleanupAndShutdown();
            throw new RuntimeException();
        }
    }
    
    public static AbstractToolkit createToolkit(Canvas canvas0, MaterialProvider interface221, int i_2, int i_3) {
        return new SoftwareToolkit(canvas0, interface221, i_2, i_3);
    }

    native void q(MaterialProvider var1, int var2, int var3);

    native void R(int var1, int var2, int var3, int var4, int var5, int var6, ComponentSurface var7, int var8, int var9);

    native void Y(int var1);

    native void Q(int var1, int[] var2, float[] var3);

    native void wa(float[] var1);

    @Override
    protected synchronized void finalize() {
        cleanupAndShutdown();
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this);
        }

    }

    public void h() {
        System.gc();
        System.runFinalization();
        FinalizeCacheSw3d.destroyAll();
    }

    @Override
    public Matrix43 copySunPosition() {
        return this.ay;
    }

    @Override
    public native void setDepthBuffering(boolean var1);

    @Override
    public void awaitRenderCompletion() {
    }

    @Override
    public void animateTextures(int i_1) {
        FinalizeCacheSw3d.destroyAll();
        Y(i_1);

        for (ba ba2 = (ba) this.au.getFirst(); ba2 != null; ba2 = (ba) this.au.next()) {
            ba2.u();
        }

    }

    @Override
    public native int getOffheap();

    @Override
    public NativeSprite createNativeSprite(int[] ints_1, int i_2, int i_3, int i_4, int i_5, boolean bool6) {
        return new SpriteSw3d(this, ints_1, i_2, i_3, i_4, i_5);
    }

    @Override
    public boolean supportsFullscreen() {
        return true;
    }

    @Override
    public boolean forceFixedScreenmode() {
        return true;
    }

    @Override
    public boolean hasBloomSupport() {
        return false;
    }

    @Override
    public boolean method8404() {
        return false;
    }

    @Override
    public void calculateProjection(float f_1, float f_2, float f_3, float[] floats_4) {
        float f_5 = this.aw.faceList[15] + this.aw.faceList[3] * f_1 + this.aw.faceList[7] * f_2 + this.aw.faceList[11] * f_3;
        float f_6 = this.aw.faceList[12] + this.aw.faceList[0] * f_1 + this.aw.faceList[4] * f_2 + this.aw.faceList[8] * f_3;
        float f_7 = this.aw.faceList[13] + this.aw.faceList[1] * f_1 + this.aw.faceList[5] * f_2 + this.aw.faceList[9] * f_3;
        float f_8 = this.ao.faceList[14] + this.ao.faceList[2] * f_1 + this.ao.faceList[6] * f_2 + this.ao.faceList[10] * f_3;
        floats_4[0] = this.ax + this.az * f_6 / f_5;
        floats_4[1] = this.al + this.ap * f_7 / f_5;
        floats_4[2] = f_8;
    }

    @Override
    public boolean extendBuildArea() {
        return true;
    }

    @Override
    public boolean replaceCanvasOnDisplayUpdate() {
        return true;
    }

    @Override
    public int getMaxLights() {
        return 4;
    }

    @Override
    public void debugModeChanged() {
    }

    @Override
    public void renderLastFrame() {
    }

    @Override
    public DelegatingDisplay createDelegatingDisplay(Canvas canvas1, int i_2, int i_3) {
        return new ImageProducerSw3d(this, canvas1, i_2, i_3);
    }

    public native void PA(ImageProducerSw3d var1);

    public native void d(long var1, long var3);

    @Override
    public void calculateViewportCenter() {
        this.az = getDisplay().getWidth() / 2.0F;
        this.ap = getDisplay().getHeight() / 2.0F;
        this.ax = this.az;
        this.al = this.ap;
        k();
    }

    @Override
    public void setDrawableArea(int i_1, int i_2, int i_3, int i_4) {
        this.az = i_3 / 2.0F;
        this.ap = i_4 / 2.0F;
        this.ax = i_1 + this.az;
        this.al = i_2 + this.ap;
        A(i_1, i_2, i_3, i_4);
    }

    native void A(int var1, int var2, int var3, int var4);


    @Override
    public native void resetClipping();

    @Override
    public native void setClipping(int var1, int var2, int var3, int var4);

    @Override
    public native void getClipping(int[] var1);

    @Override
    public native void applyClearFlags(int var1, int var2);

    @Override
    public void drawRectangleOutline(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        drawTransparentHorizontalLine(i_1, i_2, i_3, i_5, i_6);
        drawTransparentHorizontalLine(i_1, i_2 + i_4 - 1, i_3, i_5, i_6);
        drawTransparentVerticalLine(i_1, i_2 + 1, i_4 - 1, i_5, i_6);
        drawTransparentVerticalLine(i_3 + i_1 - 1, i_2 + 1, i_4 - 1, i_5, i_6);
    }

    @Override
    public native void drawFilledRectangle(int var1, int var2, int var3, int var4, int var5, int var6);

    @Override
    public native void renderWorldmapTile(int var1, int var2, int var3, int var4, int var5, int var6, byte[] var7, int var8, int var9);

    @Override
    public void benchmark() {
        throw new IllegalStateException();
    }

    @Override
    public native void drawTransparentHorizontalLine(int var1, int var2, int var3, int var4, int var5);

    @Override
    public native void drawTransparentVerticalLine(int var1, int var2, int var3, int var4, int var5);

    @Override
    public void drawTransparentLine(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        YA(i_1, i_2, i_3, i_4, i_5, i_6);
    }

    @Override
    public boolean hasShadows() {
        return true;
    }

    @Override
    public native void drawCircle(int var1, int var2, int var3, int var4);

    @Override
    public void drawThickLine(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7) {
        nx().method311(this, i_1, i_2, i_3, i_4, i_5, i_6, i_7);
    }

    @Override
    public void method8669(int i_1, int i_2, int i_3, int i_4, int i_5, ComponentSurface class4557, int i_8, int i_9) {
        R(i_1, i_2, i_3, i_4, i_5, 1, class4557, i_8, i_9);
    }

    @Override
    public void drawLine(int i_1, int i_2, int i_3, int i_4, int i_5, int i_7, int i_8, int i_9) {
    }

    @Override
    public int isOnScreen(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        int i_7 = 0;
        float f_8 = this.aw.faceList[14] + this.aw.faceList[2] * i_1 + this.aw.faceList[6] * i_2 + this.aw.faceList[10] * i_3;
        float f_9 = this.aw.faceList[14] + this.aw.faceList[2] * i_4 + this.aw.faceList[6] * i_5 + this.aw.faceList[10] * i_6;
        float f_10 = this.aw.faceList[15] + this.aw.faceList[3] * i_1 + this.aw.faceList[7] * i_2 + this.aw.faceList[11] * i_3;
        float f_11 = this.aw.faceList[15] + this.aw.faceList[3] * i_4 + this.aw.faceList[7] * i_5 + this.aw.faceList[11] * i_6;
        if (f_8 < -f_10 && f_9 < -f_11) {
            i_7 |= 0x10;
        } else if (f_8 > f_10 && f_9 > f_11) {
            i_7 |= 0x20;
        }

        float f_12 = this.aw.faceList[12] + this.aw.faceList[0] * i_1 + this.aw.faceList[4] * i_2 + this.aw.faceList[8] * i_3;
        float f_13 = this.aw.faceList[12] + this.aw.faceList[0] * i_4 + this.aw.faceList[4] * i_5 + this.aw.faceList[8] * i_6;
        if (f_12 < -f_10 && f_13 < -f_11) {
            i_7 |= 0x1;
        }

        if (f_12 > f_10 && f_13 > f_11) {
            i_7 |= 0x2;
        }

        float f_14 = this.aw.faceList[13] + this.aw.faceList[1] * i_1 + this.aw.faceList[5] * i_2 + this.aw.faceList[9] * i_3;
        float f_15 = this.aw.faceList[13] + this.aw.faceList[1] * i_4 + this.aw.faceList[5] * i_5 + this.aw.faceList[9] * i_6;
        if (f_14 < -f_10 && f_15 < -f_11) {
            i_7 |= 0x4;
        }

        if (f_14 > f_10 && f_15 > f_11) {
            i_7 |= 0x8;
        }

        return i_7;
    }

    @Override
    public boolean hasTextures() {
        return true;
    }

    @Override
    public void computeWorldToScreen(float f_1, float f_2, float f_3, float[] floats_4) {
        float f_5 = this.aw.faceList[14] + this.aw.faceList[2] * f_1 + this.aw.faceList[6] * f_2 + this.aw.faceList[10] * f_3;
        float f_6 = this.aw.faceList[15] + this.aw.faceList[3] * f_1 + this.aw.faceList[7] * f_2 + this.aw.faceList[11] * f_3;
        if (f_5 >= -f_6 && f_5 <= f_6) {
            float f_7 = this.aw.faceList[12] + this.aw.faceList[0] * f_1 + this.aw.faceList[4] * f_2 + this.aw.faceList[8] * f_3;
            if (f_7 >= -f_6 && f_7 <= f_6) {
                float f_8 = this.aw.faceList[13] + this.aw.faceList[1] * f_1 + this.aw.faceList[5] * f_2 + this.aw.faceList[9] * f_3;
                if (f_8 >= -f_6 && f_8 <= f_6) {
                    float f_9 = this.ao.faceList[14] + this.ao.faceList[2] * f_1 + this.ao.faceList[6] * f_2 + this.ao.faceList[10] * f_3;
                    floats_4[0] = this.ax + this.az * f_7 / f_6;
                    floats_4[1] = this.al + this.ap * f_8 / f_6;
                    floats_4[2] = f_9;
                } else {
                    floats_4[2] = Float.NaN;
                    floats_4[1] = Float.NaN;
                    floats_4[0] = Float.NaN;
                }
            } else {
                floats_4[2] = Float.NaN;
                floats_4[1] = Float.NaN;
                floats_4[0] = Float.NaN;
            }
        } else {
            floats_4[2] = Float.NaN;
            floats_4[1] = Float.NaN;
            floats_4[0] = Float.NaN;
        }

    }

    @Override
    public NativeMemoryNode getNativeMemory(int i_1) {
        ba ba2 = new ba(this, i_1);
        this.au.addFirst(ba2);
        return ba2;
    }

    @Override
    public void setNativeHeap(NativeMemoryNode class282Sub11) {
        this.z = (ba) class282Sub11;
        n(class282Sub11);
    }

    @Override
    public NativeSprite generateEmptySprite(int i_1, int i_2, boolean bool3, boolean bool4) {
        return new SpriteSw3d(this, i_1, i_2, bool4);
    }


    @Override
    public native void clearRectangle(ComponentSurface var2, int var3, int var4);

    @Override
    public AbstractFontRenderer createFontRenderer(FontMetrics fontmetrics1, SpriteType[] arr_2, boolean bool3) {
        int[] ints_4 = new int[arr_2.length];
        int[] ints_5 = new int[arr_2.length];
        boolean transparent = false;

        for (int i_7 = 0; i_7 < arr_2.length; i_7++) {
            ints_4[i_7] = arr_2[i_7].width;
            ints_5[i_7] = arr_2[i_7].height;
            if (arr_2[i_7].alpha != null) {
                transparent = true;
            }
        }

        if (bool3) {
            if (transparent) {
                return new TransparentFontRendererSw3d(this, this.z, fontmetrics1, arr_2);
            } else {
                return new OpaqueFontRendererSw3d(this, this.z, fontmetrics1, arr_2);
            }
        } else if (transparent) {
            throw new IllegalArgumentException("");
        } else {
            return new FontRendererSw3d(this, this.z, fontmetrics1, arr_2);
        }
    }

    @Override
    public void initialize() {
        this.ac = this.at = 10000;
        if (this.as > 1) {
            throw new IllegalStateException();
        } else {
            nu(this.as);
            ni();
        }
    }

    @Override
    public Model createModel(Mesh rsmesh1, int i_2, int i_3, int i_4, int i_5) {
        return new ModelSw3d(this, this.z, rsmesh1, i_2, i_3, i_4, i_5);
    }

    @Override
    public int mismatchingFunctionMask(int i_1, int i_2) {
        int i_3 = i_1 & 0xfffff;
        int i_4 = i_2 & 0xfffff;
        return i_3 & i_4 ^ i_4;
    }


    @Override
    public boolean hasAntiAliasing() {
        return false;
    }

    @Override
    public boolean utilisesNativeHeap() {
        return true;
    }

    public void nu(int i_1) {
        this.as = i_1;
        this.ai = new ThreadResourceSw3d[this.as];

        for (int i_2 = 0; i_2 < this.as; i_2++) {
            this.ai[i_2] = new ThreadResourceSw3d(this, this.ac, this.at);
        }

    }

    public void ni() {
        this.ai[0].method307();
    }

    public ThreadResourceSw3d nx() {
        for (int i_1 = 0; i_1 < this.as; i_1++) {
            if (this.ai[i_1].aRunnable48 == Thread.currentThread()) {
                return this.ai[i_1];
            }
        }

        return null;
    }

    @Override
    public void renderParticles(ParticleWrapper class1511) {
        if (class1511.particleBinaryTree.getNodeCount() != 0) {
            ny(class1511);
            nx().method309(this, bi, bw, bx, bz, class1511.particleBinaryTree.getNodeCount());
        }

    }

    @Override
    public void setSunPosition(Matrix43 matrix44var1) {
        this.ay = matrix44var1;
        this.ao.fromMatrix(matrix44var1);
        AA(this.ao.faceList);
        this.aw.calculatePerspective(this.ao, this.an);
    }

    native void oo();

    native void AA(float[] var1);

    @Override
    public void setModelMatrix(Matrix44 matrix441) {
        this.an = matrix441;
        wa(this.an.faceList);
        this.aw.calculatePerspective(this.ao, this.an);
    }

    @Override
    public Matrix44 copyModelMatrix() {
        return this.an;
    }

    @Override
    public native void updateSun(float var1);

    @Override
    public native void applyLighting(int var1, float var2, float var3, float var4, float var5, float var6);

    @Override
    public native void generateFlags();

    native void YA(int var1, int var2, int var3, int var4, int var5, int var6);

    @Override
    public void copyLights(int i_1, PointLight[] arr_2) {
        int i_3 = 0;

        for (int i_4 = 0; i_4 < i_1; i_4++) {
            aq[i_3++] = arr_2[i_4].getX();
            aq[i_3++] = arr_2[i_4].getY();
            aq[i_3++] = arr_2[i_4].getZ();
            aq[i_3++] = arr_2[i_4].getSize2d();
            av[i_4] = arr_2[i_4].getAlpha();
            aq[i_3++] = arr_2[i_4].getColor();
        }

        Q(i_1, aq, av);
    }

    @Override
    public void resetTextureState() {
    }

    @Override
    public AbstractCubeTextureGenerator createCubeTexture(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        return null;
    }

    @Override
    public void setSunCubeTexture(AbstractCubeTextureGenerator class1521) {
    }

    @Override
    public void method8525(int i_3, int i_4) {
    }

    @Override
    public boolean bloomSupported() {
        return false;
    }

    @Override
    public void resetBloom() {
    }

    @Override
    public boolean bloomEnabled() {
        return false;
    }

    @Override
    public void setBloomFilter(float f_1, float f_2, float f_3) {
    }

    native void JA(int var1, int var2, int var3, int var4);

    native void M(int var1, int var2, int var3, int var4);

    @Override
    public void addWater(WaterTile class902) {
        M(-1, class902.color, class902.scale, class902.intensity);
    }

    @Override
    public DirectDisplay getImageProducer() {
        return new ImageProducerSw3d1(this);
    }

    @Override
    public RenderBuffer getRenderBuffer(int width, int height) {
        return new SoftwareRenderBuffer(width, height);
    }

    @Override
    public void addWaterTile(int i_1, WaterTile class902) {
        JA(i_1, class902.color, class902.scale, class902.intensity);
    }

    @Override
    public void render() {
        ImageProducerSw3d za3 = (ImageProducerSw3d) getDelegatingDisplay();
        if (za3 == null) {
            throw new IllegalStateException();
        } else {
            za3.draw(0, 0);
            if (super.materialProvider != null) {
                super.materialProvider.cycle();
            }

        }
    }

    native void F(short var1, int[] var2, short var3, int var4, byte var5, byte var6, int var7, boolean var8, byte var9, byte var10, byte var11, byte var12, boolean var13, boolean var14, boolean var15, boolean var16, boolean var17, byte var18, boolean var19, boolean var20, int var21);

    @Override
    public NativeSprite createNativeSprite(int i_1, int i_2, int i_3, int i_4, boolean bool5) {
        return new SpriteSw3d(this, i_1, i_2, i_3, i_4, !bool5);
    }

    @Override
    public Matrix44 method8449() {
        return this.af;
    }

    native void n(NativeMemoryNode var1);


    native void og(float[] var1);

    @Override
    public void shutdown() {
        if (!this.b) {
            this.ai = null;
            this.z = null;
            this.af = null;

            for (ba ba1 = (ba) this.au.getFirst(); ba1 != null; ba1 = (ba) this.au.next()) {
                ba1.ha();
            }

            this.au.remove_all();
            da();
            if (this.bm) {
                ColorUtil.resetTables(false, true);
                this.bm = false;
            }

            h();
            FinalizeCacheSw3d.decrementCapacity();
            this.b = true;
        }

    }


    @Override
    public native int[] copyPixelRectangle(int var1, int var2, int var3, int var4);

    @Override
    public void drawTexturedLine(int i_1, int i_2, int i_3, int i_4, int i_5, ComponentSurface class4557, int i_8, int i_9, int i_10, int i_11, int i_12) {
    }

    @Override
    public native void destroy(boolean var1);

    public void ny(ParticleWrapper class1511) {
        int i_3 = 0;
        int i_4 = 0;
        int i_5 = 0;
        int i_6 = 0;
        boolean bool7 = false;

        for (PointEntity class275Sub1Sub18 = (PointEntity) class1511.particleBinaryTree.getNext(); class275Sub1Sub18 != null; class275Sub1Sub18 = (PointEntity) class1511.particleBinaryTree.method7734()) {
            bi[i_3++] = class275Sub1Sub18.x;
            bi[i_3++] = class275Sub1Sub18.y;
            bi[i_3++] = class275Sub1Sub18.z;
            bw[i_4++] = class275Sub1Sub18.colorArgb;
            bz[i_6++] = (short) class275Sub1Sub18.materialId;
            bx[i_5++] = class275Sub1Sub18.size;
        }

    }

    native void oq(float[] var1);

    native void da();


    @Override
    public AbstractCubeTextureGenerator drawQuad(AbstractCubeTextureGenerator class1521, AbstractCubeTextureGenerator class1522, float f_3, AbstractCubeTextureGenerator class1524) {
        return null;
    }


    native void oj(int var1, int var2, int var3, int var4);


    @Override
    public native void calculateFog(int var1, int var2, int var3);

    @Override
    public native void expandClipping(int var1, int var2, int var3, int var4);

    native void on(int var1, int var2, int var3, int var4);

    @Override
    public native void resetDepthClippingMask();


    native void nn(int var1, int var2, int var3, int var4, int var5, int var6);


    native void k();


    @Override
    public Ground createGround(int i_1, int i_2, int[][] ints_3, int[][] ints_4, int i_6, int i_7) {
        return new GroundSw3d(this, this.z, i_1, i_2, ints_3, ints_4, i_6, i_7);
    }


    @Override
    public NativeSprite generateSprite(SpriteType class911, boolean bool2) {
        SpriteSw3d fa3 = new SpriteSw3d(this, class911.pallete, class911.pixels, class911.alpha, class911.width, class911.width, class911.height);
        fa3.setSize(class911.minX, class911.minY, class911.borderX, class911.borderY);
        return fa3;
    }

    @Override
    public RenderInfo getRenderInfo() {
        return new RenderInfo(0, "SSE", 1, "CPU", 0L);
    }

    native void V(short var1, short var2, int var3, byte var4, byte var5, int var6, boolean var7, byte var8, byte var9, byte var10, byte var11, boolean var12, boolean var13, boolean var14, boolean var15, boolean var16, byte var17, boolean var18, boolean var19, int var20);

    @Override
    public void method8474() {
    }

    @Override
    public Matrix43 getRenderingMatrix() {
        return this.ag;
    }

    @Override
    public native void onTilesDrawn();

    @Override
    public int mergeFunctionMasks(int i_1, int i_2) {
        return i_1 | i_2;
    }

    @Override
    public ComponentSurface createComponentSurface(int i_1, int i_2, int[] ints_3, int[] ints_4) {
        return new wa(this, this.z, i_1, i_2, ints_3, ints_4);
    }
}
