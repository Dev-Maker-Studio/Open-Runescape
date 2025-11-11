package com.jagex.game.runetek5.config.sun;

import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.config.material.texture.TextureTypeList;
import com.jagex.game.runetek5.graphics.light.Skybox;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

import static com.jagex.game.runetek5.graphics.model.ModelFunctionMask.*;

public class Sun {

    public static final int[] SUN_CLIPPING = new int[4];
    public static Model sunModel;
    public static NativeSprite activeSun;
    public static NativeSprite activeAntiSun;
    public int sunSize2d;
    public int positionOffset;
    public NativeSprite sunSprite;
    public int sunUseDimension;
    public int sunRotationX;
    public int sunRotationY;
    public final int sunPositionX;
    public final int sunPositionY;
    public final int sunPositionZ;
    public final boolean invertPosition;
    public final int sunMaterialId;
    public final int sunColor;
    public final int positionOffsetStep;
    public final int sunActionType;
    public final int rotationX;
    public final int rotationY;
    public final int rotationZ;

    public Sun(int actionType, int sunMaterialId, int sunPositionX, int sunPositionY, int sunPositionZ, int positionOffsetStep, int sunColor, boolean invertPosition, int rotationX, int rotationY, int rotationZ) {
        this.sunPositionX = sunPositionX;
        this.sunPositionY = sunPositionY;
        this.sunPositionZ = sunPositionZ;
        this.invertPosition = invertPosition;
        this.sunMaterialId = sunMaterialId;
        this.sunColor = sunColor;
        this.positionOffsetStep = positionOffsetStep;
        this.sunActionType = actionType;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
    }

    public static void resetMedia() {
        sunModel = null;
        activeSun = null;
    }

    public static void generateModel(AbstractToolkit toolkit) {
        if (sunModel == null) {
            Mesh sunMesh = new Mesh(580, 1104, 1);
            sunMesh.addSunTexture();
            sunMesh.addVertex(0, 128, 0);
            sunMesh.addVertex(0, -128, 0);
            for (int index = 0; index <= 24; index++) {
                int rot = index * 8192 / 24;
                int rotateZ = MathUtils.SINE[rot];
                int rotateX = MathUtils.COSINE[rot];
                for (int subIndex = 1; subIndex < 24; subIndex++) {
                	int rotation = subIndex * 8192 / 24;
                	int y = MathUtils.COSINE[rotation] >> 7;
                    int z = MathUtils.SINE[rotation] * rotateZ >> 21;
                    int x = MathUtils.SINE[rotation] * rotateX >> 21;
                    sunMesh.addVertex(x, y, -z);
                }
                if (index > 0) {
                    int faceA = index * 23 + 2;
                    int faceB = faceA - 23;
                    sunMesh.addFace(0, faceB, faceA, (byte) 0, (byte) 0, (short) 127, (byte) 0, (short) 0);
                    for (int faceIndex = 1; faceIndex < 23; faceIndex++) {
                    	int faceC = faceB + 1;
                    	int faceD = faceA + 1;
                        sunMesh.addFace(faceB, faceC, faceA, (byte) 0, (byte) 0, (short) 127, (byte) 0, (short) 0);
                        sunMesh.addFace(faceC, faceD, faceA, (byte) 0, (byte) 0, (short) 127, (byte) 0, (short) 0);
                        faceB = faceC;
                        faceA = faceD;
                    }
                    sunMesh.addFace(faceA, faceB, 1, (byte) 0, (byte) 0, (short) 127, (byte) 0, (short) 0);
                }
            }
            sunMesh.maxDepth = sunMesh.numVertices;
            sunMesh.faceGroups = null;
            sunMesh.vertexGroups = null;
            sunMesh.facesLayer = null;
            sunModel = toolkit.createModel(sunMesh, HAS_TEXTURE_COORDS | RETEXTURE | RECOLOR, ALTERS_VERTICES | DISPLACE_X, 64, 768);
        }

    }

    public static void generateSunSprites(AbstractToolkit toolkit) {
        if (activeSun == null) {
            int[] antiSunColor = new int[16384];
            int[] sunColor = new int[16384];
            for (int i_3 = 0; i_3 < 64; i_3++) {
                int i_4 = 64 - i_3;
                i_4 *= i_4;
                int i_5 = 128 - i_3 - 1;
                int offsetA = i_3 * 128;
                int offsetC = i_5 * 128;
                for (int offsetB = 0; offsetB < 64; offsetB++) {
                    int i_9 = 64 - offsetB;
                    i_9 *= i_9;
                    int offsetD = 128 - offsetB - 1;
                    int alpha = 256 - (i_9 + i_4 << 8) / 4096;
                    alpha = alpha * 3072 / 1536;
                    if (alpha < 0) {
                        alpha = 0;
                    } else if (alpha > 255) {
                        alpha = 255;
                    }
                    int halvedAlpha = alpha / 2;
                    sunColor[offsetA + offsetB] = sunColor[offsetA + offsetD] = sunColor[offsetC + offsetB] = sunColor[offsetC + offsetD] = ~0xffffff | alpha << 16;
                    antiSunColor[offsetA + offsetB] = antiSunColor[offsetA + offsetD] = antiSunColor[offsetC + offsetB] = antiSunColor[offsetC + offsetD] = 127 - halvedAlpha << 24 | 0xffffff;
                }
            }
            activeSun = toolkit.createNativeSprite(sunColor, 128, 128, 128);
            activeAntiSun = toolkit.createNativeSprite(antiSunColor, 128, 128, 128);
        }
    }

    public boolean isSunGenerated(AbstractToolkit toolkit, Sun sun) {
        return this.sunSprite != null || performAction(toolkit, sun);
    }

    public boolean move(int deltaX, int deltaY, int deltaZ, int offsetMultiplier) {
        int x;
        int y;
        int z;
        if (this.invertPosition) {
            this.sunSize2d = 1073741823;
            x = this.sunPositionX;
            y = this.sunPositionY;
            z = this.sunPositionZ;
        } else {
            x = this.sunPositionX - deltaX;
            y = this.sunPositionY - deltaY;
            z = this.sunPositionZ - deltaZ;
            this.sunSize2d = (int) Math.sqrt(x * x + y * y + z * z);
            if (this.sunSize2d == 0) {
                this.sunSize2d = 1;
            }
            x = (x << 8) / this.sunSize2d;
            y = (y << 8) / this.sunSize2d;
            z = (z << 8) / this.sunSize2d;
        }
        int sunScaling = (int) (Math.sqrt(x * x + y * y + z * z) * 256.0D);
        if (sunScaling > 128) {
            x = (x << 16) / sunScaling;
            y = (y << 16) / sunScaling;
            z = (z << 16) / sunScaling;
            this.positionOffset = offsetMultiplier * this.positionOffsetStep / (this.invertPosition ? 1024 : this.sunSize2d);
        } else {
            this.positionOffset = 0;
        }
        if (this.positionOffset < 8) {
            this.sunSprite = null;
            return false;
        } 
        int dimension = MathUtils.nextPowerOfTwo(this.positionOffset);
        if (dimension > offsetMultiplier) {
            dimension = MathUtils.furtestBitValue(offsetMultiplier);
        }
        if (dimension > 512) {
            dimension = 512;
        }
        if (dimension != this.sunUseDimension) {
            this.sunUseDimension = dimension;
        }
        this.sunRotationX = (int) (Math.asin(y / 256.0F) * 2607.5945876176133D) & 0x3fff;
        this.sunRotationY = (int) (Math.atan2(x, -z) * 2607.5945876176133D) & 0x3fff;
        this.sunSprite = null;
        return true;
    }

    public void drawColoredSun(AbstractToolkit toolkit, Sun sun) {
        Mesh mesh = Mesh.fromResource(TextureTypeList.modelResource, this.sunMaterialId);
        if (mesh != null) {
            toolkit.getClipping(SUN_CLIPPING);
            toolkit.setClipping(0, 0, this.sunUseDimension, this.sunUseDimension);
            toolkit.applyClearFlags(2, 0);
            toolkit.drawFilledRectangle(0, 0, this.sunUseDimension, this.sunUseDimension, 0, 0);
            int positionX = 0;
            int positionY = 0;
            int positionZ = 256;
            if (sun != null) {
                if (sun.invertPosition) {
                    positionX = -sun.sunPositionX;
                    positionY = -sun.sunPositionY;
                    positionZ = -sun.sunPositionZ;
                } else {
                    positionX = this.sunPositionX - sun.sunPositionX;
                    positionY = this.sunPositionY - sun.sunPositionY;
                    positionZ = this.sunPositionZ - sun.sunPositionZ;
                }
            }
            if (this.sunRotationX != 0) {
            	int rotation = -this.sunRotationX & 0x3fff;
            	int xan = MathUtils.SINE[rotation];
            	int zan = MathUtils.COSINE[rotation];
            	int tmpY = positionY * zan - positionZ * xan >> 14;
                positionZ = positionY * xan + positionZ * zan >> 14;
                positionY = tmpY;
            }
            if (this.sunRotationY != 0) {
            	int rotation = -this.sunRotationY & 0x3fff;
            	int xan = MathUtils.SINE[rotation];
            	int zan = MathUtils.COSINE[rotation];
            	int tmpX = positionZ * xan + positionX * zan >> 14;
                positionZ = positionZ * zan - positionX * xan >> 14;
                positionX = tmpX;
            }
            toolkit.updateSun(1.0F);
            toolkit.applyLighting(this.sunColor, 1.0F, 1.0F, positionX, positionY, positionZ);
            mesh.rotateWithoutNormals(this.rotationX & 0x3fff, this.rotationY & 0x3fff, this.rotationZ & 0x3fff);
            Model sunModel = toolkit.createModel(mesh, 2048, 0, 64, 768);
            int sizeX = sunModel.getMaxX() - sunModel.getMinX();
            int sizeY = sunModel.getMaxY() - sunModel.getMinY();
            int highestDimension = Math.max(sizeX, sizeY);
            int size = this.sunUseDimension * 1024 / highestDimension;
            Matrix44 matrix4412 = toolkit.copyModelMatrix();
            Matrix44 matrix4413 = toolkit.method8449();
            matrix4413.method6532((this.sunUseDimension / 2), (this.sunUseDimension / 2), size, size, 50.0F, 50000.0F, toolkit.getDisplay().getWidth(), toolkit.getDisplay().getHeight(), 1024.0F);
            toolkit.setModelMatrix(matrix4413);
            Matrix43 sunPosition = new Matrix43();
            toolkit.setSunPosition(sunPosition);
            Matrix43 renderingMatrix = toolkit.getRenderingMatrix();
            renderingMatrix.setViewPosition(0.0F, 0.0F, (50 - sunModel.getMinZ()));
            sunModel.drawMatrixThreaded(renderingMatrix, null, ModelFunctionMaskGL.SMOOTH_EDGES);
            this.sunSprite = toolkit.createNativeSprite(0, 0, this.sunUseDimension, this.sunUseDimension, true);
            this.sunSprite.pixelTransfer();
            toolkit.setModelMatrix(matrix4412);
            toolkit.setClipping(SUN_CLIPPING[0], SUN_CLIPPING[1], SUN_CLIPPING[2], SUN_CLIPPING[3]);
        }

    }

    public boolean performAction(AbstractToolkit toolkit, Sun sun) {
        if (this.sunSprite == null) {
            if (this.sunActionType == 0) {
                if (Skybox.materialProvider.isCached(this.sunMaterialId)) {
                    int[] pixels = Skybox.materialProvider.getPixelsArgb(this.sunMaterialId, this.sunUseDimension, this.sunUseDimension);
                    this.sunSprite = toolkit.createNativeSprite(pixels, this.sunUseDimension, this.sunUseDimension, this.sunUseDimension);
                }
            } else if (this.sunActionType == 2) {
                drawColoredSun(toolkit, sun);
            } else if (this.sunActionType == 1) {
                drawTexturedSun(toolkit, sun);
            }
        }
        return this.sunSprite != null;
    }

    public void drawTexturedSun(AbstractToolkit toolkit, Sun sun) {
        generateModel(toolkit);
        generateSunSprites(toolkit);
        toolkit.getClipping(SUN_CLIPPING);
        toolkit.setClipping(0, 0, this.sunUseDimension, this.sunUseDimension);
        toolkit.applyClearFlags(2, 0);
        toolkit.drawFilledRectangle(0, 0, this.sunUseDimension, this.sunUseDimension, ~0xffffff | this.sunColor, 0);
        int positionX = 0;
        int positionY = 0;
        int positionZ = 256;
        if (sun != null) {
            if (sun.invertPosition) {
                positionX = -sun.sunPositionX;
                positionY = -sun.sunPositionY;
                positionZ = -sun.sunPositionZ;
            } else {
                positionX = sun.sunPositionX - this.sunPositionX;
                positionY = sun.sunPositionY - this.sunPositionY;
                positionZ = sun.sunPositionZ - this.sunPositionZ;
            }
        }
        if (this.sunRotationX != 0) {
            int xan = MathUtils.SINE[this.sunRotationX];
            int zan = MathUtils.COSINE[this.sunRotationX];
            int tmp = positionY * zan - positionZ * xan >> 14;
            positionZ = positionY * xan + positionZ * zan >> 14;
            positionY = tmp;
        }
        if (this.sunRotationY != 0) {
        	int xan = MathUtils.SINE[this.sunRotationY];
        	int zan = MathUtils.COSINE[this.sunRotationY];
        	int tmp = positionZ * xan + positionX * zan >> 14;
            positionZ = positionZ * zan - positionX * xan >> 14;
            positionX = tmp;
        }
        Model copiedSunModel = sunModel.copy((byte) 0, HAS_TEXTURE_COORDS | RECOLOR | RETEXTURE, true);
        copiedSunModel.rematerial((short) 0, (short) this.sunMaterialId);
        toolkit.updateSun(1.0F);
        toolkit.applyLighting(16777215, 1.0F, 1.0F, positionX, positionY, positionZ);
        int size = this.sunUseDimension * 1024 / (copiedSunModel.getMaxX() - copiedSunModel.getMinX());
        if (this.sunColor != 0) {
            size = size * 13 / 16;
        }
        Matrix44 matrix4414 = toolkit.copyModelMatrix();
        Matrix44 matrix449 = toolkit.method8449();
        matrix449.method6532((this.sunUseDimension / 2), (this.sunUseDimension / 2), size, size, 50.0F, 50000.0F, toolkit.getDisplay().getWidth(), toolkit.getDisplay().getHeight(), 1024.0F);
        toolkit.setModelMatrix(matrix449);
        toolkit.setSunPosition(new Matrix43());
        Matrix43 sunProjectionMatrix = new Matrix43();
        sunProjectionMatrix.setViewPosition(0.0F, 0.0F, (50 - copiedSunModel.getMinZ()));
        copiedSunModel.drawMatrixThreaded(sunProjectionMatrix, null, ModelFunctionMaskGL.SMOOTH_EDGES);
        int useDimension = this.sunUseDimension * 13 / 16;
        int position = (this.sunUseDimension - useDimension) / 2;
        activeAntiSun.drawCircle(position, position, useDimension, useDimension, 0, ~0xffffff | this.sunColor, 1);
        this.sunSprite = toolkit.createNativeSprite(0, 0, this.sunUseDimension, this.sunUseDimension, true);
        toolkit.applyClearFlags(2, 0);
        toolkit.drawFilledRectangle(0, 0, this.sunUseDimension, this.sunUseDimension, 0, 0);
        activeSun.drawCircle(0, 0, this.sunUseDimension, this.sunUseDimension, 1, 0, 0);
        this.sunSprite.pixelTransfer();
        toolkit.setModelMatrix(matrix4414);
        toolkit.setClipping(SUN_CLIPPING[0], SUN_CLIPPING[1], SUN_CLIPPING[2], SUN_CLIPPING[3]);
    }

    public void calculateProjection(AbstractToolkit toolkit, int width, int height, int sunDeltaX, int sunDeltaY, int sunDeltaZ, int alpha) {
        if (this.sunSprite != null) {
            float[] sunProjection = new float[3];
            float sunX = (-(this.sunPositionX - sunDeltaX << 16));
            float sunY = (this.sunPositionY - sunDeltaY << 15);
            float sunZ = (-(this.sunPositionZ - sunDeltaZ << 16));
            float[] sunPosition = new float[3];
            toolkit.copySunPosition().cacheInto(sunPosition);
            sunX += sunPosition[0];
            sunY += sunPosition[1];
            sunZ += sunPosition[2];
            toolkit.calculateProjection(sunX, sunY, sunZ, sunProjection);
            if (sunProjection[2] >= 0.0F) {
                int x = (int) (sunProjection[0] - (this.positionOffset / 2));
                int y = (int) (sunProjection[1] - (this.positionOffset / 2));
                if (y < height && y + this.positionOffset > 0 && x < width && x + this.positionOffset > 0) {
                    this.sunSprite.drawCircle(x, y, this.positionOffset, this.positionOffset, 0, alpha << 24 | 0xffffff, 1);
                }
            }
        }
    }

	public static void reset() {
	    resetMedia();
	}
}
