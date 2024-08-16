package com.jagex.game.runetek5.graphics.model;

import com.jagex.game.runetek5.entity.BoundingBoxPolygon;
import com.jagex.game.runetek5.graphics.model.billboard.Billboard;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardType;
import com.jagex.game.runetek5.graphics.model.billboard.BillboardTypeList;
import com.jagex.game.runetek5.graphics.model.particle.EffectiveVertex;
import com.jagex.game.runetek5.graphics.model.particle.EmissiveTriangle;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.toolkit.SoftwareToolkit;
import com.jagex.game.runetek5.toolkit.sw3d.FinalizeCacheSw3d;
import com.jagex.game.runetek5.toolkit.sw3d.INativeObject;
import com.jagex.game.runetek5.toolkit.sw3d.ba;

//h
public class ModelSw3d extends Model implements INativeObject {

    public long nativeid;
    public final SoftwareToolkit aja7140;
    public final ba aba7141;
    public EmissiveTriangle[] aClass87Array7139;
    public EffectiveVertex[] aClass172Array7142;

    public ModelSw3d(SoftwareToolkit ja1, ba ba2, Mesh rsmesh3, int i_4, int i_5, int i_6, int i_7) {
        this.aja7140 = ja1;
        this.aba7141 = ba2;
        this.aClass87Array7139 = rsmesh3.particleEmitters;
        this.aClass172Array7142 = rsmesh3.particleEffectors;
        int i_8 = rsmesh3.particleEmitters == null ? 0 : rsmesh3.particleEmitters.length;
        int i_9 = rsmesh3.particleEffectors == null ? 0 : rsmesh3.particleEffectors.length;
        int i_10 = 0;
        int[] ints_11 = new int[i_9 + i_8 * 3];

        int i_12;
        for (i_12 = 0; i_12 < i_8; i_12++) {
            ints_11[i_10++] = this.aClass87Array7139[i_12].faceA;
            ints_11[i_10++] = this.aClass87Array7139[i_12].faceB;
            ints_11[i_10++] = this.aClass87Array7139[i_12].faceC;
        }

        for (i_12 = 0; i_12 < i_9; i_12++) {
            ints_11[i_10++] = this.aClass172Array7142[i_12].appliesOnVertex;
        }

        i_12 = rsmesh3.billboards == null ? 0 : rsmesh3.billboards.length;
        int[] ints_13 = new int[i_12 * 8];
        int i_14 = 0;

        int i_15;
        Billboard class8416;
        for (i_15 = 0; i_15 < i_12; i_15++) {
            class8416 = rsmesh3.billboards[i_15];
            BillboardType class34717 = BillboardTypeList.list(class8416.billboardId);
            ints_13[i_14++] = class8416.appliesOnFace;
            ints_13[i_14++] = class34717.size2d;
            ints_13[i_14++] = class34717.size3d;
            ints_13[i_14++] = class34717.materialId;
            ints_13[i_14++] = class34717.blendType;
            ints_13[i_14++] = class34717.shape;
            ints_13[i_14++] = class34717.hasUid ? -1 : 0;
        }

        for (i_15 = 0; i_15 < i_12; i_15++) {
            class8416 = rsmesh3.billboards[i_15];
            ints_13[i_14++] = class8416.depth;
        }

        U(this.aja7140, this.aba7141, rsmesh3.numVertices, rsmesh3.maxDepth, rsmesh3.verticesX, rsmesh3.verticesY, rsmesh3.verticesZ, rsmesh3.vertexGroups, rsmesh3.verticesSubmeshes, rsmesh3.numFaces, rsmesh3.facesA, rsmesh3.facesB, rsmesh3.facesC, rsmesh3.faceRenderType, rsmesh3.facesLayer, rsmesh3.facesAlpha, rsmesh3.facesTexture, rsmesh3.facesColor, rsmesh3.facesMaterial, rsmesh3.faceGroups, rsmesh3.priority, rsmesh3.facesSubmeshes, rsmesh3.numTextures, rsmesh3.texturesMapping, rsmesh3.textureFaceX, rsmesh3.textureFaceY, rsmesh3.textureFaceZ, rsmesh3.texturesScaleX, rsmesh3.texturesScaleY, rsmesh3.texturesScaleZ, rsmesh3.texturesRotation, rsmesh3.texturesDirection, rsmesh3.texturesSpeed, rsmesh3.texturesUtrans, rsmesh3.texturesVtrans, ints_11, i_8, i_9, i_4, i_5, i_6, i_7, ints_13);
    }

    public ModelSw3d(SoftwareToolkit ja1) {
        this.aja7140 = ja1;
        this.aba7141 = null;
        ba(ja1);
    }


    native void U(SoftwareToolkit var1, ba var2, int var3, int var4, int[] var5, int[] var6, int[] var7, int[] var8, short[] var9, int var10, short[] var11, short[] var12, short[] var13, byte[] var14, byte[] var15, byte[] var16, byte[] var17, short[] var18, short[] var19, int[] var20, byte var21, short[] var22, int var23, byte[] var24, short[] var25, short[] var26, short[] var27, int[] var28, int[] var29, int[] var30, byte[] var31, byte[] var32, int[] var33, int[] var34, int[] var35, int[] var36, int var37, int var38, int var39, int var40, int var41, int var42, int[] var43);

    native void ba(SoftwareToolkit var1);

    @Override
    protected void finalize() {
        if (this.nativeid != 0L) {
            FinalizeCacheSw3d.addFinalizedObject(this);
        }
    }

    @Override
    public native void destroy(boolean var1);

    @Override
    public void updateParticlePosition(Matrix43 projectionMatrix) {
        method11604(SoftwareToolkit.aa, projectionMatrix);
        int i_2 = 0;
        int i_3;
        if (this.aClass87Array7139 != null) {
            for (i_3 = 0; i_3 < this.aClass87Array7139.length; i_3++) {
                EmissiveTriangle class874 = this.aClass87Array7139[i_3];
                class874.verticeXA = SoftwareToolkit.aa[i_2++];
                class874.verticeYA = SoftwareToolkit.aa[i_2++];
                class874.verticeZA = SoftwareToolkit.aa[i_2++];
                class874.verticeXB = SoftwareToolkit.aa[i_2++];
                class874.verticeYB = SoftwareToolkit.aa[i_2++];
                class874.verticeZB = SoftwareToolkit.aa[i_2++];
                class874.verticeXC = SoftwareToolkit.aa[i_2++];
                class874.verticeYC = SoftwareToolkit.aa[i_2++];
                class874.verticeZC = SoftwareToolkit.aa[i_2++];
            }
        }

        if (this.aClass172Array7142 != null) {
            for (i_3 = 0; i_3 < this.aClass172Array7142.length; i_3++) {
                EffectiveVertex class1726 = this.aClass172Array7142[i_3];
                EffectiveVertex class1725 = class1726;
                if (class1726.child != null) {
                    class1725 = class1726.child;
                }

                if (class1726.matrix == null) {
                    class1726.matrix = new Matrix44();
                }

                class1726.matrix.fromMatrix(projectionMatrix);
                class1725.verticeX = SoftwareToolkit.aa[i_2++];
                class1725.verticeY = SoftwareToolkit.aa[i_2++];
                class1725.verticeZ = SoftwareToolkit.aa[i_2++];
            }
        }

    }

    public native void BA(ModelSw3d var1, ModelSw3d var2, int var3, boolean var4, boolean var5);

    @Override
    public native void applyShading(int var1);

    @Override
    public native int getFunctionMask();

    @Override
    public void uploadToGpu() {
    	// NOOP
    }

    @Override
    public native int getSize3d();


    @Override
    public native void zaxisRotateWithoutNormals(int var1);

    @Override
    public native void translate(int var1, int var2, int var3);

    @Override
    public native void mirror();

    @Override
    public native void scale(int var1, int var2, int var3);

    @Override
    public void awaitLock() {
        if (this.aja7140.as > 1) {
            synchronized (this) {
                while (super.isLocked) {
                    try {
                        wait();
                    } catch (InterruptedException ignored) {
                    }
                }

                super.isLocked = true;
            }
        }

    }

    @Override
    public native int getMaxZ();

    @Override
    public native boolean startAnimation();

    native void J(long var1, int var3, int[] var4, int var5, int var6, int var7, int var8, boolean var9);

    @Override
    public native void animateTransform(int var1, int[] var2, int var3, int var4, int var5, boolean var6, int var7, int[] var8);


    @Override
    public native void endAnimation();


    native void aa(float[] var1, int var2, boolean var3);


    @Override
    public boolean calculateProjection(int i_1, int i_2, Matrix43 matrix44var3, boolean bool4, int i_5) {
        return this.aja7140.nx().method335(this, i_1, i_2, matrix44var3, bool4);
    }

    @Override
    public native void tint(int var1, int var2, int var3, int var4);

    native void ft(SoftwareToolkit var1);


    @Override
    public native int getMaxX();

    @Override
    public native int getMinY();

    @Override
    public native int getMaxY();

    @Override
    public native int getMinZ();

    native void IA(byte var1, byte[] var2);

    @Override
    public native void setContrast(int var1);

    @Override
    public void unlock() {
        if (this.aja7140.as > 1) {
            synchronized (this) {
                super.isLocked = false;
                notifyAll();
            }
        }

    }


    @Override
    public native byte[] getAlpha();

    @Override
    public native void recolor(short var1, short var2);

    @Override
    public void applyEnvironmentAlpha(byte b_1, byte[] bytes_2) {
        IA(b_1, bytes_2);
    }


    @Override
    public native void rematerial(short var1, short var2);

    @Override
    public void offsetSubmesh(Matrix43 matrix44var1, int i_2, boolean bool3) {
        Matrix44 matrix444 = this.aja7140.nx().aClass38451;
        matrix444.fromMatrix(matrix44var1);
        aa(matrix444.faceList, i_2, bool3);
    }


    @Override
    public native boolean accountForLayers();

    @Override
    public native boolean hasAnimatedTexture();

    @Override
    public EmissiveTriangle[] getEmissiveTriangles() {
        return this.aClass87Array7139;
    }

    @Override
    public EffectiveVertex[] getEffectiveVertices() {
        return this.aClass172Array7142;
    }




    @Override
    public void drawMatrixThreaded(Matrix43 matrix44var1, BoundingBoxPolygon class275Sub52, int i_3) {
        if (class275Sub52 == null) {
            this.aja7140.nx().method314(this, matrix44var1, null, i_3);
        } else {
            SoftwareToolkit.ae[5] = 0;
            this.aja7140.nx().method314(this, matrix44var1, SoftwareToolkit.ae, i_3);
            class275Sub52.left = SoftwareToolkit.ae[0];
            class275Sub52.bottom = SoftwareToolkit.ae[1];
            class275Sub52.right = SoftwareToolkit.ae[2];
            class275Sub52.top = SoftwareToolkit.ae[3];
            class275Sub52.size = SoftwareToolkit.ae[4];
            class275Sub52.positionCalculated = SoftwareToolkit.ae[5] != 0;
        }

    }



    @Override
    public native void setAmbient(int var1);



    @Override
    public native int getAmbient();



    public void method11604(int[] ints_1, Matrix43 matrix44var2) {
        this.aja7140.nx().method331(this, ints_1, matrix44var2);
    }

    @Override
    public native Shadow createShadowSprite(Shadow var1);


    @Override
    public void animatePartialtransform(int i_1, int[] ints_2, int i_3, int i_4, int i_5, int i_6, boolean bool7) {
        J(this.nativeid, i_1, ints_2, i_3, i_4, i_5, i_6, bool7);
    }

    @Override
    public native void xaxisRotateWithoutNormals(int var1);



    @Override
    public native void rotateWithNormals(int var1);






    @Override
    public native int getMinX();

    @Override
    public native void animateShadowtransform(int var1, int var2, int var3, int var4);





    @Override
    public boolean areTexturesCached() {
        return true;
    }



    @Override
    public Model copy(byte b_1, int i_2, boolean bool3) {
        return this.aja7140.nx().method325(this, b_1, i_2, bool3);
    }


    @Override
    public native void yaxisRotateWithoutNormals(int var1);

    

    @Override
    public void applyShading(Model meshrasterizer1, int i_2, int i_3, int i_4, boolean bool5) {
        this.aja7140.nx().method313(this, meshrasterizer1, i_2, i_3, i_4, bool5);
    }



    @Override
    public native int getSize2d();



    @Override
    public native int getContrast();


    @Override
    public native void contourToGround(int var1, int var2, Ground var3, Ground var4, int var5, int var6, int var7);

    native void fe(SoftwareToolkit var1, ba var2, int var3, int var4, int[] var5, int[] var6, int[] var7, int[] var8, short[] var9, int var10, short[] var11, short[] var12, short[] var13, byte[] var14, byte[] var15, byte[] var16, byte[] var17, short[] var18, short[] var19, int[] var20, byte var21, short[] var22, int var23, byte[] var24, short[] var25, short[] var26, short[] var27, int[] var28, int[] var29, int[] var30, byte[] var31, byte[] var32, int[] var33, int[] var34, int[] var35, int[] var36, int var37, int var38, int var39, int var40, int var41, int var42, int[] var43);

    native void fk(SoftwareToolkit var1, ba var2, int var3, int var4, int[] var5, int[] var6, int[] var7, int[] var8, short[] var9, int var10, short[] var11, short[] var12, short[] var13, byte[] var14, byte[] var15, byte[] var16, byte[] var17, short[] var18, short[] var19, int[] var20, byte var21, short[] var22, int var23, byte[] var24, short[] var25, short[] var26, short[] var27, int[] var28, int[] var29, int[] var30, byte[] var31, byte[] var32, int[] var33, int[] var34, int[] var35, int[] var36, int var37, int var38, int var39, int var40, int var41, int var42, int[] var43);

    native void fh(SoftwareToolkit var1);

    native void fg(ModelSw3d var1, ModelSw3d var2, int var3, boolean var4, boolean var5);

    native void fz(long var1, int var3, int[] var4, int var5, int var6, int var7, int var8, boolean var9);

    native void fp(long var1, int var3, int[] var4, int var5, int var6, int var7, int var8, boolean var9);

    native void fb(long var1, int var3, int[] var4, int var5, int var6, int var7, int var8, boolean var9);

    native void fv(long var1, int var3, int[] var4, int var5, int var6, int var7, int var8, boolean var9);


    native void ff(byte var1, byte[] var2);

    native void fr(byte var1, byte[] var2);

    native void fw(byte var1, byte[] var2);


}
