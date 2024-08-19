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

    public void renderSkybox(AbstractToolkit toolkit, int skyboxAngle, int x, int y, int width, int height, int rotationY, int rotationX, int rotationZ, int backgroundColor, boolean clearBackground, int alpha) {
        int invertedAlpha = 255 - alpha;

        if (this.skyModel == null) {
            toolkit.applyClearFlags(2, 0);
            rotationX = rotationX + skyboxAngle & 0x3fff;

            if (this.currentMaterialId != -1 && this.useSize != 0) {
                MaterialType material = materialProvider.getMaterial(this.currentMaterialId);
                if (this.backgroundSprite == null && materialProvider.isCached(this.currentMaterialId)) {
                    int[] materialPixels = material.effectCombiner == 2
                            ? materialProvider.getPixelsArgb(this.currentMaterialId, this.useSize, this.useSize)
                            : materialProvider.getPixels(this.currentMaterialId, 0.7F, this.useSize, this.useSize, false);

                    this.srcBackgroundColor = materialPixels[0];
                    this.dstBackgroundColor = materialPixels[materialPixels.length - 1];
                    this.backgroundSprite = toolkit.createNativeSprite(materialPixels, this.useSize, this.useSize, this.useSize);
                }

                int combineMode = (invertedAlpha == 255 && material.effectCombiner != 2) ? 0 : 1;

                if (combineMode == 1 && clearBackground)
                    toolkit.drawFilledRectangle(x, y, width, height, backgroundColor, 0);

                if (this.backgroundSprite != null) {
                    int yOffset = rotationY * height / -4096;
                    int xOffset = rotationX * height / 4096 + (width - height) / 2;

                    xOffset = xOffset % height;
                    yOffset = yOffset % height;

                    if (this.backgroundMode != BackgroundMode.COLOR_TRANSITION)
                        drawRepeatingBackground(toolkit, x, y, width, height, xOffset, yOffset, invertedAlpha, combineMode);
                    else
                        drawTransitioningBackground(toolkit, x, y, width, height, xOffset, yOffset, invertedAlpha, combineMode);
                }
            } else
                toolkit.drawFilledRectangle(x, y, width, height, invertedAlpha << 24 | backgroundColor, 1);
        } else {
            if (clearBackground)
                toolkit.applyClearFlags(3, backgroundColor);
            applySunRotation(toolkit, rotationY, rotationX, rotationZ, alpha);
        }
        // Render additional sun projections
        for (int index = this.availableSunCount - 1; index >= 0; --index)
            this.availableSuns[index].calculateProjection(toolkit, width, height, this.positionX, this.positionY, this.positionZ, invertedAlpha);
        toolkit.applyClearFlags(2, 0);
    }

    private void drawRepeatingBackground(AbstractToolkit toolkit, int x, int y, int width, int height, int xOffset, int yOffset, int invertedAlpha, int combineMode) {
        for (int i = xOffset - height; i < width; i += height) {
            for (int j = yOffset - height; j < height; j += height) {
                this.backgroundSprite.drawCircle(x + i, y + j, height, height, 0, invertedAlpha << 24 | 0xffffff, combineMode);
            }
        }
    }

    private void drawTransitioningBackground(AbstractToolkit toolkit, int x, int y, int width, int height, int xOffset, int yOffset, int invertedAlpha, int combineMode) {
        this.backgroundSprite.drawCircle(x + xOffset, y + yOffset, height, height, 0, invertedAlpha << 24 | 0xffffff, combineMode);
        if ((this.srcBackgroundColor & -16777216) != 0)
            toolkit.clearPixels(0, 0, width, yOffset + y + 1, this.srcBackgroundColor);
        if ((this.dstBackgroundColor & -16777216) != 0)
            toolkit.clearPixels(0, yOffset + y + height, width, height - (yOffset + y + height), this.dstBackgroundColor);
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

    public void render(AbstractToolkit toolkit, int x, int y, int width, int height, int rotationX, int rotationY) {
        render(toolkit, 0, x, y, width, height, rotationX, rotationY, 0, 0);
    }

    public void render(AbstractToolkit toolkit, int angle, int x, int y, int width, int height, int rotationX, int rotationY, int rotationZ, int fogColor) {
        int alpha = this.copyAlpha ? this.skyboxAlpha : 0;

        if (this.skybox != null) {
            Skybox primarySkybox = this;
            Skybox secondarySkybox = this.skybox;

            // Swap if necessary based on hashCode to ensure consistent rendering order
            if (hashCode() > secondarySkybox.hashCode()) {
                primarySkybox = this.skybox;
                secondarySkybox = this;
                alpha = 255 - alpha;
            }

            // Render the primary and secondary skyboxes
            primarySkybox.renderSkybox(toolkit, angle, x, y, width, height, rotationX, rotationY, rotationZ, fogColor, true, alpha);
            secondarySkybox.renderSkybox(toolkit, angle, x, y, width, height, rotationX, rotationY, rotationZ, fogColor, false, 255 - alpha);
        } else {
            // Render the single skybox if no secondary is present
            renderSkybox(toolkit, angle, x, y, width, height, rotationX, rotationY, rotationZ, fogColor, true, alpha);
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

    public void applySunRotation(AbstractToolkit toolkit, int rotationY, int rotationX, int rotationZ, int alpha) {
        // Backup the current sun position
        Matrix43 originalSunPosition = toolkit.copySunPosition();
        // Create a new matrix for the sun position and apply the rotations
        Matrix43 rotatedSunPosition = new Matrix43();
        rotatedSunPosition.setViewPosition(0.0F, 0.0F, 0.0F);
        rotatedSunPosition.rotate(0.0F, -1.0f, 0.0F, MathUtils.getTheta(-rotationX & 0x3fff));
        rotatedSunPosition.rotate(-1.0f, 0.0F, 0.0F, MathUtils.getTheta(-rotationY & 0x3fff));
        rotatedSunPosition.rotate(0.0F, 0.0F, -1.0f, MathUtils.getTheta(-rotationZ & 0x3fff));
        // Update the sun position in the toolkit
        toolkit.setSunPosition(rotatedSunPosition);
        // Update the sky model's alpha value if it has changed
        if (alpha != this.lastAlpha) {
            this.skyModel.applyEnvironmentAlpha((byte) alpha, this.faceAlpha);
            this.lastAlpha = alpha;
        }
        // Draw the sky model using the rotated matrix
        Matrix43 drawnMatrix = new Matrix43();
        this.skyModel.drawMatrixThreaded(drawnMatrix, null, ModelFunctionMaskGL.EMPTY);
        // Restore the original sun position
        toolkit.setSunPosition(originalSunPosition);
    }
}
