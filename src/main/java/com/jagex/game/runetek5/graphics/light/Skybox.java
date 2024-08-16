package com.jagex.game.runetek5.graphics.light;

import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.config.material.texture.TextureTypeList;
import com.jagex.game.runetek5.config.skybox.BackgroundMode;
import com.jagex.game.runetek5.config.sun.Sun;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.ArrayUtils;
import com.jagex.game.runetek5.utils.MathUtils;

public class Skybox {

    public static MaterialProvider materialProvider;

    public int srcBackgroundColor;

    public int dstBackgroundColor;

    public NativeSprite backgroundSprite;

    public int skyboxAlpha;

    public int anInt3054;

    public boolean copyAlpha;

    public Skybox skybox;

    public int anInt3032 = -1;

    public boolean prepareSun = true;

    public Model skyModel;

    public int useSize;

    public int availableSunCount;

    public byte[] faceAlpha;

    public int lastAlpha;

    public final int positionX;

    public final int positionY;

    public final int positionZ;

    public final int currentMaterialId;

    public final Sun[] initialSuns;

    public final BackgroundMode backgroundMode;

    public final Sun[] availableSuns;

    public final Sun activeSun;

    public final int skyboxArchiveId;

    public Skybox(int materialId, Sun[] suns, int defaultSunIndex, int x, int y, int z, BackgroundMode backgroundMode, int skyboxArchiveId) {
        this.positionX = x;
        this.positionY = y;
        this.positionZ = z;
        this.currentMaterialId = materialId;
        this.initialSuns = suns;
        this.backgroundMode = backgroundMode;
        if (suns != null) {
        	this.availableSuns = new Sun[suns.length];
        	this.activeSun = defaultSunIndex >= 0 ? suns[defaultSunIndex] : null;
        } else {
        	this.availableSuns = null;
        	this.activeSun = null;
        }
        this.skyboxArchiveId = skyboxArchiveId;
    }

    public void method4206(AbstractToolkit toolkit, int skyboxAngle, int x, int y, int width, int height, int i_7, int i_8, int i_9, int color, boolean clearCurrentBackground, int alpha) {
        int invertedAlpha = 255 - alpha;
        if (this.skyModel == null) {
            toolkit.applyClearFlags(2, 0);
            i_8 = i_8 + skyboxAngle & 0x3fff;
            if (this.currentMaterialId != -1 && this.useSize != 0) {
                MaterialType material = materialProvider.getMaterial(this.currentMaterialId);
                if (this.backgroundSprite == null && materialProvider.isCached(this.currentMaterialId)) {
                    int[] materialPixels = material.effectCombiner == 2 ? materialProvider.getPixelsArgb(this.currentMaterialId, this.useSize, this.useSize) : materialProvider.getPixels(this.currentMaterialId, 0.7F, this.useSize, this.useSize, false);
                    this.srcBackgroundColor = materialPixels[0];
                    this.dstBackgroundColor = materialPixels[materialPixels.length - 1];
                    this.backgroundSprite = toolkit.createNativeSprite(materialPixels, this.useSize, this.useSize, this.useSize);
                }
                int combineMode = invertedAlpha == 255 ? (material.effectCombiner == 2 ? 1 : 0) : 1;
                if (combineMode == 1 && clearCurrentBackground) {
                    toolkit.drawFilledRectangle(x, y, width, height, color, 0);
                }
                if (this.backgroundSprite != null) {
                    int i_18 = i_7 * height / -4096;
                    int i_19;
                    for (i_19 = i_8 * height / 4096 + (width - height) / 2; i_19 > height; i_19 -= height) {
                    }
                    while (i_19 < 0) {
                        i_19 += height;
                    }
                    int i_20;
                    if (this.backgroundMode != BackgroundMode.COLOR_TRANSITION) {
                        while (i_18 > height) {
                            i_18 -= height;
                        }
                        while (i_18 < 0) {
                            i_18 += height;
                        }
                        for (i_20 = i_19 - height; i_20 < width; i_20 += height) {
                            for (int i_21 = i_18 - height; i_21 < height; i_21 += height) {
                                this.backgroundSprite.drawCircle(x + i_20, i_21 + y, height, height, 0, invertedAlpha << 24 | 0xffffff, combineMode);
                            }
                        }
                    } else {
                        for (i_20 = i_19 - height; i_20 < width; i_20 += height) {
                            this.backgroundSprite.drawCircle(x + i_20, i_18 + y, height, height, 0, invertedAlpha << 24 | 0xffffff, combineMode);
                        }
                        if ((this.srcBackgroundColor & -16777216) != 0) {
                            toolkit.clearPixels(0, 0, width, i_18 + y + 1, this.srcBackgroundColor);
                        }
                        if ((this.dstBackgroundColor & -16777216) != 0) {
                            toolkit.clearPixels(0, i_18 + y + height, width, height - (i_18 + y + height), this.dstBackgroundColor);
                        }
                    }
                }
            } else {
                toolkit.drawFilledRectangle(x, y, width, height, invertedAlpha << 24 | color, 1);
            }
        } else {
            if (clearCurrentBackground) {
                toolkit.applyClearFlags(3, color);
            }
            method4231(toolkit, i_7, i_8, i_9, alpha);
        }
        for (int index = this.availableSunCount - 1; index >= 0; --index) {
            this.availableSuns[index].calculateProjection(toolkit, width, height, this.positionX, this.positionY, this.positionZ, invertedAlpha);
        }
        toolkit.applyClearFlags(2, 0);
    }

    public void method4207(int i_1, int i_2) {
        this.skyboxAlpha = (i_2 - this.anInt3054) * i_1 / 255 + this.anInt3054;
    }

    public void method4208(Skybox class2471) {
        if (this.copyAlpha) {
            this.anInt3054 = this.skyboxAlpha;
        } else if (class2471 != null && class2471.copyAlpha) {
            this.anInt3054 = 255 - class2471.skyboxAlpha;
        } else {
            this.anInt3054 = 0;
        }
        this.copyAlpha = true;
        this.skybox = class2471;
        this.skyboxAlpha = 0;
    }

    public void method4209() {
        this.copyAlpha = false;
        this.skybox = null;
        this.skyboxAlpha = 0;
    }

    public boolean method4210() {
        return this.copyAlpha;
    }

    public Skybox get() {
        return this.skybox;
    }

    public void method4214(AbstractToolkit toolkit, int x, int y, int width, int height, int i_7, int i_8) {
        render(toolkit, 0, x, y, width, height, i_7, i_8, 0, 0);
    }

    public void render(AbstractToolkit toolkit, int angle, int x, int y, int width, int height, int i_7, int i_8, int i_9, int fogColor) {
        int alpha = 0;
        if (this.copyAlpha) {
            alpha = this.skyboxAlpha;
        }
        if (this.skybox != null) {
            Skybox class24715 = this;
            Skybox class24716 = this.skybox;
            if (hashCode() > class24716.hashCode()) {
                class24715 = this.skybox;
                class24716 = this;
                alpha = 255 - alpha;
            }
            class24715.method4206(toolkit, angle, x, y, width, height, i_7, i_8, i_9, fogColor, true, alpha);
            class24716.method4206(toolkit, angle, x, y, width, height, i_7, i_8, i_9, fogColor, false, 255 - alpha);
        } else {
            method4206(toolkit, angle, x, y, width, height, i_7, i_8, i_9, fogColor, true, alpha);
        }
    }

    public boolean method4217(AbstractToolkit toolkit, int componentHeight, int skyboxPreference) {
        if (componentHeight != this.anInt3032) {
            this.anInt3032 = componentHeight;
            int i_5 = MathUtils.furtestBitValue(componentHeight);
            if (i_5 > 512) {
                i_5 = 512;
            }
            if (i_5 <= 0) {
                i_5 = 1;
            }
            if (i_5 != this.useSize) {
                this.useSize = i_5;
                this.backgroundSprite = null;
            }
            if (this.initialSuns != null) {
                this.availableSunCount = 0;
                int[] ints_6 = new int[this.initialSuns.length];
                for (int i_7 = 0; i_7 < this.initialSuns.length; i_7++) {
                    Sun class3348 = this.initialSuns[i_7];
                    if (class3348.move(this.positionX, this.positionY, this.positionZ, this.anInt3032)) {
                        ints_6[this.availableSunCount] = class3348.sunSize2d;
                        this.availableSuns[++this.availableSunCount - 1] = class3348;
                    }
                }
                ArrayUtils.quicksort(ints_6, this.availableSuns, 0, this.availableSunCount - 1);
            }
            this.prepareSun = true;
        }
        boolean ready = false;
        if (this.prepareSun) {
            this.prepareSun = false;
            for (int index = this.availableSunCount - 1; index >= 0; --index) {
                boolean sunReady = this.availableSuns[index].isSunGenerated(toolkit, this.activeSun);
                this.prepareSun |= !sunReady;
                ready |= sunReady;
            }
        }
        if (skyboxPreference != 0 && toolkit.method8404()) {
            if (this.skyModel == null && this.skyboxArchiveId >= 0) {
                setupModel(toolkit);
            }
        } else {
            this.skyModel = null;
        }
        if (this.skybox != null && this != this.skybox) {
            this.skybox.method4209();
            ready |= this.skybox.method4217(toolkit, componentHeight, skyboxPreference);
        }
        return ready;
    }

    /**
     * [J]
     */
    public void setupModel(AbstractToolkit toolkit) {
        try {
            boolean archiveLoaded = TextureTypeList.modelResource.loadArchive(this.skyboxArchiveId);
            if (archiveLoaded) {
                toolkit.applyLighting(16777215, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F);
                Mesh mesh = Mesh.fromResource(TextureTypeList.modelResource, this.skyboxArchiveId);
                this.skyModel = toolkit.createModel(mesh, 1099776, 0, 255, 1);
                byte[] alpha = this.skyModel.getAlpha();
                if (alpha == null) {
                    this.faceAlpha = null;
                } else {
                    this.faceAlpha = new byte[alpha.length];
                    System.arraycopy(alpha, 0, this.faceAlpha, 0, alpha.length);
                }
            }
        } catch (Exception exception) {
        	// NOOP
        }
    }

    public void method4231(AbstractToolkit toolkit, int rotationY, int rotationX, int rotationZ, int alpha) {
        Matrix43 sunMatrix = toolkit.copySunPosition();
        Matrix43 newSunPosition = new Matrix43();
        newSunPosition.setViewPosition(0.0F, 0.0F, 0.0F);
        newSunPosition.rotate(0.0F, -1.0f, 0.0F, MathUtils.getTheta(-rotationX & 0x3fff));
        newSunPosition.rotate(-1.0f, 0.0F, 0.0F, MathUtils.getTheta(-rotationY & 0x3fff));
        newSunPosition.rotate(0.0F, 0.0F, -1.0f, MathUtils.getTheta(-rotationZ & 0x3fff));
        toolkit.setSunPosition(newSunPosition);
        Matrix43 drawnMatrix = new Matrix43();
        if (alpha != this.lastAlpha) {
            this.skyModel.applyEnvironmentAlpha((byte) alpha, this.faceAlpha);
            this.lastAlpha = alpha;
        }
        this.skyModel.drawMatrixThreaded(drawnMatrix, null, ModelFunctionMaskGL.EMPTY);
        toolkit.setSunPosition(sunMatrix);
    }
}
