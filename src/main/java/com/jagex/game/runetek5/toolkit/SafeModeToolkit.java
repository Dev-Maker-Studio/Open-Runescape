package com.jagex.game.runetek5.toolkit;

import java.awt.*;

import com.jagex.game.runetek5.buffer.NativeMemoryNode;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;
import com.jagex.game.runetek5.framebuffer.SafeModeRenderBuffer;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.font.FontRendererSM;
import com.jagex.game.runetek5.graphics.font.OpaqueFontRendererSM;
import com.jagex.game.runetek5.graphics.font.TransparentFontRendererSM;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelSM;
import com.jagex.game.runetek5.graphics.model.particle.ParticleNode;
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper;
import com.jagex.game.runetek5.graphics.model.particle.PointEntity;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.OpaqueSpriteSM;
import com.jagex.game.runetek5.graphics.sprite.PaletteSpriteSM;
import com.jagex.game.runetek5.graphics.sprite.SpriteSM;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.graphics.sprite.TransparentSpriteSM;
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator;
import com.jagex.game.runetek5.graphics.texture.SafeModeTexture2D;
import com.jagex.game.runetek5.imageproducer.AbstractImageProducerSM;
import com.jagex.game.runetek5.imageproducer.BufferedImageProducerSM;
import com.jagex.game.runetek5.imageproducer.DelegatingDisplay;
import com.jagex.game.runetek5.imageproducer.DirectDisplay;
import com.jagex.game.runetek5.imageproducer.ImageProducerSM1;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.rendering.RenderInfo;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.ground.GroundSM;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.safemode.RasterizerSM;
import com.jagex.game.runetek5.toolkit.safemode.ThreadResourceSM;
import com.jagex.game.runetek5.unidentified.*;
import com.jagex.game.runetek5.utils.ColorUtil;
import com.jagex.game.runetek5.utils.Utils;

public class SafeModeToolkit extends AbstractToolkit {

    public int currentCycle;
    public int anInt8998;
    public int anInt8999;
    public int anInt9000;
    public int anInt9001;
    public boolean initialized;
    public boolean shutdown;
    public float aFloat8976;
    public float aFloat9004;
    public float aFloat9003;
    public float aFloat8982;
    public int clipLeft;
    public int clipTop;
    public int clipRight;
    public int clipBottom;
    public float aFloat8978;
    public float aFloat8985;
    public int lowestContrast;
    public int highestContrast;
    public int lightX;
    public int lightY;
    public int lightZ;
    public int executingThreadCount;
    public ThreadResourceSM[] renderingThreads;
    public int renderedMaterialId;
    public NativeSprite renderedNativeSprite;
    public Matrix43 currentSunPosition;
    public int lightIntensity;
    public final float[][] aFloatArrayArray8989;
    public final int materialSize;
    public boolean forceSmallTexture;
    public final LRUCache animatedSpriteMaterials;
    public LRUCache animatedModelMaterials;
    public Matrix44 aClass3848994;
    public Matrix44 modelMatrix;
    public Matrix44 floorMatrix;
    public int viewportWidth;
    public int viewportHeight;
    public int[] colourBuffer;
    public float[] depthBuffer;

    public SafeModeToolkit(MaterialProvider interface221) {
        super(interface221);
        this.shutdown = false;
        this.initialized = false;
        this.clipLeft = 0;
        this.clipRight = 0;
        this.clipTop = 0;
        this.clipBottom = 0;
        this.lowestContrast = 45823;
        this.highestContrast = 78642;
        this.lightIntensity = 75518;
        this.aFloatArrayArray8989 = new float[6][4];
        this.aFloat8978 = 1.0F;
        this.aFloat8985 = 0.0F;
        this.materialSize = 128;
        this.forceSmallTexture = false;
        this.animatedSpriteMaterials = new LRUCache(16);
        this.renderedMaterialId = -1;
        try {
            this.animatedModelMaterials = new LRUCache(256);
            this.currentSunPosition = new Matrix43();
            this.aClass3848994 = new Matrix44();
            this.modelMatrix = new Matrix44();
            this.floorMatrix = new Matrix44();
            initializeThreads(1);
            mainThreadInitialize();
            ColorUtil.initializeTables(true, true);
            this.initialized = true;
            this.currentCycle = (int) Utils.time();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            cleanupAndShutdown();
            throw new RuntimeException("");
        }
    }
    
    public static AbstractToolkit createToolkit(Canvas gameCanvas, MaterialProvider materialProvider, int screenWidth, int screenHeight) {
        return new SafeModeToolkit(gameCanvas, materialProvider, screenWidth, screenHeight);
    }

    public SafeModeToolkit(Canvas gameCanvas, MaterialProvider materialProvider, int screenWidth, int screenHeight) {
        this(materialProvider);

        try {
            ensureImageproducerCreated(gameCanvas, screenWidth, screenHeight);
            setActiveCanvas(gameCanvas);
        } catch (Throwable throwable6) {
            throwable6.printStackTrace();
            cleanupAndShutdown();
            throw new RuntimeException("");
        }
    }
    
    @Override
    public void getClipping(int[] clipping) {
        clipping[0] = this.clipLeft;
        clipping[1] = this.clipTop;
        clipping[2] = this.clipRight;
        clipping[3] = this.clipBottom;
    }
    
    public void bind(int width, int height, int[] pixels, float[] depth) {
        this.viewportWidth = width;
        this.viewportHeight = height;
        this.colourBuffer = pixels;
        this.depthBuffer = depth;
        for (int threadIndex = 0; threadIndex < this.executingThreadCount; threadIndex++) {
            this.renderingThreads[threadIndex].initializeRasterizer();
        }
        resetClipping();
        calculateViewportCenter();
    }
    

    @Override
    public void resetClipping() {
        this.clipLeft = 0;
        this.clipTop = 0;
        this.clipRight = this.viewportWidth;
        this.clipBottom = this.viewportHeight;
        viewportUpdate();
    }
    
    /**************************************************************
     *					(TODO: textures) 
     *************************************************************/
    
    @Override
    public void animateTextures(int clientCycle) {
        int cycle = clientCycle - this.currentCycle; // ~30 avg ish
    	//System.out.println("CYCLE: "+cycle);
    	//cycle = 30;
        for (SafeModeTexture2D materialNode = (SafeModeTexture2D) this.animatedModelMaterials.getFirst(); materialNode != null; materialNode = (SafeModeTexture2D) this.animatedModelMaterials.getNext()) {
            if (materialNode.isAnimated) {
                materialNode.cycle += cycle;
                int materialCycle = materialNode.cycle / 50;
                if (materialCycle > 0) {
                    MaterialType material = super.materialProvider.getMaterial(materialNode.materialId);
                    float size = material.isSmall ? 64.0F : 128.0F;
                    materialNode.animate((int) (size * (material.textureSpeedU * ((float) cycle / 1000.0F) / 64.0F)), (int) (material.textureSpeedV * ((float) cycle / 1000.0F) / 64.0F * size));
                    materialNode.cycle -= materialCycle * 50;
                }
                materialNode.isAnimated = false;
            }
        }
        this.currentCycle = clientCycle;
        this.animatedSpriteMaterials.clean(5);
        this.animatedModelMaterials.clean(5);
    }
    
    public int[] getAnimatedMaterialArgb(int materialId) {
        SafeModeTexture2D animatedMaterial;
        synchronized (this.animatedModelMaterials) {
            animatedMaterial = ((SafeModeTexture2D) this.animatedModelMaterials.get(materialId | -0x8000000000000000L));
            if (animatedMaterial == null) {
                if (!super.materialProvider.isCached(materialId)) {
                    return null;
                }
                MaterialType material = super.materialProvider.getMaterial(materialId);
                int forcedTextureSize = (material.isSmall || this.forceSmallTexture ? 64 : this.materialSize);
                animatedMaterial = (new SafeModeTexture2D(materialId, forcedTextureSize, super.materialProvider.getPixelsArgb(materialId, forcedTextureSize, forcedTextureSize), material.effectCombiner != 1));
                this.animatedModelMaterials.put(animatedMaterial, materialId | -0x8000000000000000L);
            }
        }
        animatedMaterial.isAnimated = true;
        return animatedMaterial.getArgbColor();
    }

    public boolean isMaterialCached(int materialId) {
        return super.materialProvider.isCached(materialId);
    }

    public int getMaterialEffectCombiner(int materialId) {
        return super.materialProvider.getMaterial(materialId).effectCombiner;
    }

    public int getMaterialColor(int materialId) {
        return super.materialProvider.getMaterial(materialId).colorHsl & 0xffff;
    }

    public void clearDepth() {
        if (this.depthBuffer != null) {
            if (this.clipLeft == 0 && this.clipRight == this.viewportWidth && this.clipTop == 0 && this.clipBottom == this.viewportHeight) {
                int depthBufferLength = this.depthBuffer.length;
                int depth = depthBufferLength - (depthBufferLength & 0x7);
                int pixelIndex;
                for (pixelIndex = 0; pixelIndex < depth; this.depthBuffer[pixelIndex++] = 2.14748365E9F) {
                    this.depthBuffer[pixelIndex++] = 2.14748365E9F;
                    this.depthBuffer[pixelIndex++] = 2.14748365E9F;
                    this.depthBuffer[pixelIndex++] = 2.14748365E9F;
                    this.depthBuffer[pixelIndex++] = 2.14748365E9F;
                    this.depthBuffer[pixelIndex++] = 2.14748365E9F;
                    this.depthBuffer[pixelIndex++] = 2.14748365E9F;
                    this.depthBuffer[pixelIndex++] = 2.14748365E9F;
                }
                while (pixelIndex < depthBufferLength) {
                    this.depthBuffer[pixelIndex++] = 2.14748365E9F;
                }
            } else {
            	int width = this.clipRight - this.clipLeft;
            	int height = this.clipBottom - this.clipTop;
            	int scanOffset = this.viewportWidth - width;
                int i_4 = this.viewportWidth * this.clipTop + this.clipLeft;
                int i_5 = width >> 3;
                int i_6 = width & 0x7;
                width = i_4 - 1;
                for (int invertedHeight = -height; invertedHeight < 0; invertedHeight++) {
                    int i_8;
                    if (i_5 > 0) {
                        i_8 = i_5;
                        do {
                            ++width;
                            this.depthBuffer[width] = 2.14748365E9F;
                            ++width;
                            this.depthBuffer[width] = 2.14748365E9F;
                            ++width;
                            this.depthBuffer[width] = 2.14748365E9F;
                            ++width;
                            this.depthBuffer[width] = 2.14748365E9F;
                            ++width;
                            this.depthBuffer[width] = 2.14748365E9F;
                            ++width;
                            this.depthBuffer[width] = 2.14748365E9F;
                            ++width;
                            this.depthBuffer[width] = 2.14748365E9F;
                            ++width;
                            this.depthBuffer[width] = 2.14748365E9F;
                            --i_8;
                        } while (i_8 > 0);
                    }

                    if (i_6 > 0) {
                        i_8 = i_6;
                        do {
                            ++width;
                            this.depthBuffer[width] = 2.14748365E9F;
                            --i_8;
                        } while (i_8 > 0);
                    }
                    width += scanOffset;
                }
            }
        }
    }

    
    @Override
    public void accumulateViewportDimensions(int[] viewportDimensions) {
        viewportDimensions[0] = this.viewportWidth;
        viewportDimensions[1] = this.viewportHeight;
    }

    @Override
    public void calculateViewportCenter() {
        this.anInt8998 = 0;
        this.anInt8999 = 0;
        this.anInt9000 = this.viewportWidth;
        this.anInt9001 = this.viewportHeight;
        viewportUpdate();
    }

    @Override
    public void setDrawableArea(int i_1, int i_2, int i_3, int i_4) {
        this.anInt8998 = i_1;
        this.anInt8999 = i_2;
        this.anInt9000 = i_3;
        this.anInt9001 = i_4;
        viewportUpdate();
    }

    @Override
    public void shutdown() {
        if (this.initialized) {
            ColorUtil.resetTables(true, false);
            this.initialized = false;
        }
        this.shutdown = true;
    }

    public void viewportUpdate() {
        int clipWidth = this.clipRight - this.clipLeft;
        int clipHeight = this.clipBottom - this.clipTop;
        float f_3 = this.aFloat8976 = this.anInt9000 / 2.0F;
        float f_4 = this.aFloat9004 = this.anInt9001 / 2.0F;
        this.aFloat9003 = this.anInt8998 + f_3;
        this.aFloat8982 = this.anInt8999 + f_4;
        for (int threadIndex = 0; threadIndex < this.executingThreadCount; threadIndex++) {
            ThreadResourceSM renderingThread = this.renderingThreads[threadIndex];
            RasterizerSM rasterizer = renderingThread.rasterizer;
            rasterizer.offsetX = f_3;
            rasterizer.offsetY = f_4;
            rasterizer.centerX = this.aFloat9003 - this.clipLeft;
            rasterizer.centerY = this.aFloat8982 - this.clipTop;
            rasterizer.clipWidth = clipWidth;
            rasterizer.clipHeight = clipHeight;
        }
        int scanlineOffset = this.viewportWidth * this.clipTop + this.clipLeft;
        for (int y = this.clipTop; y < this.clipBottom; y++) {
            for (int threadIndex = 0; threadIndex < this.executingThreadCount; threadIndex++) {
                this.renderingThreads[threadIndex].rasterizer.scanlineOffsets[y - this.clipTop] = scanlineOffset;
            }
            scanlineOffset += this.viewportWidth;
        }
    }


    @Override
    public void addWaterTile(int averageHeight, WaterTile waterTile) {
        ThreadResourceSM viewport = getThreadResource(Thread.currentThread());
        viewport.waterAverageHeight = averageHeight;
        viewport.fogColor = waterTile.color;
        viewport.waterScale = waterTile.scale;
    }

    @Override
    public void onTilesDrawn() {
        for (int index = 0; index < this.renderingThreads.length; index++) {
            this.renderingThreads[index].fogColor = this.renderingThreads[index].lastWaterColor;
            this.renderingThreads[index].hasWaterUpdate = false;
        }
    }

    @Override
    public boolean utilisesNativeHeap() {
        return false;
    }

    @Override
    public RenderInfo getRenderInfo() {
        return new RenderInfo(0, "Pure Java", 1, "CPU", 0L);
    }

    @Override
    public void awaitRenderCompletion() {
    }


    @Override
    public void setBloomFilter(float f_1, float f_2, float f_3) {
    }


    public void renderParticles(boolean bool1, boolean bool2, ParticleWrapper particleWrapper) {
        ThreadResourceSM threadResource = getThreadResource(Thread.currentThread());
        ParticleNode mainNode = particleWrapper.particleBinaryTree.current;

        for (ParticleNode particleNode = mainNode.child; particleNode != mainNode; particleNode = particleNode.child) {
            PointEntity pointEntry = (PointEntity) particleNode;
            int positionX = pointEntry.x >> 12;
            int positionY = pointEntry.y >> 12;
            int positionZ = pointEntry.z >> 12;
            float f_12 = this.floorMatrix.faceList[14] + this.floorMatrix.faceList[10] * positionZ + positionX * this.floorMatrix.faceList[2] + positionY * this.floorMatrix.faceList[6];
            float f_13 = this.floorMatrix.faceList[15] + this.floorMatrix.faceList[7] * positionY + this.floorMatrix.faceList[3] * positionX + this.floorMatrix.faceList[11] * positionZ;
            if (f_12 >= -f_13) {
                float f_14 = this.aFloat8985 + f_12 * this.aFloat8978 / f_13;
                if (f_12 <= threadResource.aFloat2305) {
                    float f_15 = this.floorMatrix.faceList[0] * positionX + this.floorMatrix.faceList[4] * positionY + positionZ * this.floorMatrix.faceList[8] + this.floorMatrix.faceList[12];
                    float f_16 = positionZ * this.floorMatrix.faceList[9] + this.floorMatrix.faceList[1] * positionX + this.floorMatrix.faceList[5] * positionY + this.floorMatrix.faceList[13];
                    if (f_15 >= -f_13 && f_15 <= f_13 && f_16 >= -f_13 && f_16 <= f_13) {
                        float f_17 = pointEntry.size / 4096.0F;
                        float f_18 = f_17 * this.modelMatrix.faceList[0] + f_15;
                        float f_19 = f_13 + this.modelMatrix.faceList[3] * f_17;
                        float f_20 = this.aFloat9003 + f_15 * this.aFloat8976 / f_13;
                        float f_21 = this.aFloat9004 * f_16 / f_13 + this.aFloat8982;
                        float f_22 = this.aFloat9003 + f_18 * this.aFloat8976 / f_19;
                        renderMaterializedSprite(bool1, bool2, pointEntry, (int) f_20, (int) f_21, f_14, (int) (f_22 < f_20 ? f_20 - f_22 : f_22 - f_20));
                    }
                }
            }
        }

    }

    @Override
    public boolean supportsFullscreen() {
        return false;
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
    public boolean method8404() { // unknown
        return false;
    }

    @Override
    public boolean hasShadows() {
        return false;
    }

    @Override
    public boolean hasAntiAliasing() {
        return false;
    }

    @Override
    public boolean hasTextures() {
        return false;
    }

    @Override
    public boolean extendBuildArea() {
        return false;
    }

    @Override
    public boolean replaceCanvasOnDisplayUpdate() {
        return true;
    }

    @Override
    public DelegatingDisplay createDelegatingDisplay(Canvas canvas1, int i_2, int i_3) {
        return SafeModeToolkit.method14493(this, canvas1, i_2, i_3);
    }
    
    public static AbstractImageProducerSM method14493(SafeModeToolkit hardwarerenderer0, Canvas canvas1, int i_2, int i_3) {
        BufferedImageProducerSM class158Sub2Sub3Sub15 = new BufferedImageProducerSM(hardwarerenderer0, canvas1, i_2, i_3);
        return class158Sub2Sub3Sub15;
    }

    @Override
    public void setDepthBuffering(boolean useDepthBuffer) {
        ThreadResourceSM threadResource = getThreadResource(Thread.currentThread());
        threadResource.zbuffering = useDepthBuffer;
    }

    @Override
    public int[] copyPixelRectangle(int rotation, int offY, int width, int height) {
        if (this.colourBuffer == null) {
            throw new IllegalStateException("");
        } 
        int[] pixels = new int[width * height];
        int pixelIndex = 0;
        for (int y = 0; y < height; y++) {
            int off = (y + offY) * this.viewportWidth + rotation;
            for (int x = 0; x < width; x++) {
                pixels[pixelIndex++] = this.colourBuffer[off + x];
            }
        }
        return pixels;
    }

    @Override
    public void renderLastFrame() {
    }

    @Override
    public NativeSprite createNativeSprite(int[] ints_1, int i_2, int i_3, int i_4, int i_5, boolean bool6) {
        boolean bool7 = false;
        int i_8 = i_2;

        for (int i_9 = 0; i_9 < i_5; i_9++) {
            for (int i_10 = 0; i_10 < i_4; i_10++) {
                int i_11 = ints_1[i_8++] >>> 24;
                if (i_11 != 0 && i_11 != 255) {
                    bool7 = true;
                    return new TransparentSpriteSM(this, ints_1, i_2, i_3, i_4, i_5, bool6);
                }
            }
        }

        return new OpaqueSpriteSM(this, ints_1, i_2, i_3, i_4, i_5, bool6);
    }

    @Override
    public AbstractFontRenderer createFontRenderer(FontMetrics fontmetrics, SpriteType[] spriteTypes, boolean monochome) {
        int[] spriteWidths = new int[spriteTypes.length];
        int[] spriteHeights = new int[spriteTypes.length];
        boolean hasAlpha = false;
        for (int spriteIndex = 0; spriteIndex < spriteTypes.length; spriteIndex++) {
            spriteWidths[spriteIndex] = spriteTypes[spriteIndex].width;
            spriteHeights[spriteIndex] = spriteTypes[spriteIndex].height;
            if (spriteTypes[spriteIndex].alpha != null) {
                hasAlpha = true;
            }
        }
        if (monochome) {
            if (hasAlpha) {
                return new TransparentFontRendererSM(this, fontmetrics, spriteTypes, spriteWidths, spriteHeights);
            } else {
                return new OpaqueFontRendererSM(this, fontmetrics, spriteTypes, spriteWidths, spriteHeights);
            }
        } else if (hasAlpha) {
            throw new IllegalArgumentException("");
        } else {
            return new FontRendererSM(this, fontmetrics, spriteTypes, spriteWidths, spriteHeights);
        }
    }

    @Override
    public void expandClipping(int left, int top, int right, int bottom) {
        if (this.clipLeft < left) {
            this.clipLeft = left;
        }
        if (this.clipTop < top) {
            this.clipTop = top;
        }
        if (this.clipRight > right) {
            this.clipRight = right;
        }
        if (this.clipBottom > bottom) {
            this.clipBottom = bottom;
        }
        viewportUpdate();
    }

    @Override
    public void drawFilledRectangle(int x, int y, int width, int height, int color, int type) {
        if (this.colourBuffer != null) {
            if (x < this.clipLeft) {
                width -= this.clipLeft - x;
                x = this.clipLeft;
            }
            if (y < this.clipTop) {
                height -= this.clipTop - y;
                y = this.clipTop;
            }
            if (width + x > this.clipRight) {
                width = this.clipRight - x;
            }
            if (y + height > this.clipBottom) {
                height = this.clipBottom - y;
            }
            if (width > 0 && height > 0 && x <= this.clipRight && y <= this.clipBottom) {
                int i_7 = this.viewportWidth - width;
                int i_8 = y * this.viewportWidth + x;
                int alpha = color >>> 24;
                int transparency;
                int i_11;
                int i_12;
                if (type != 0 && (type != 1 || alpha != 255)) {
                    int lastColor;
                    if (type == 1) {
                        color = (alpha * ((color & -16711936) >>> 8) & -16711936) + (alpha * (color & 0xff00ff) >> 8 & 0xff00ff);
                        transparency = 256 - alpha;
                        for (i_11 = 0; i_11 < height; i_11++) {
                            for (i_12 = -width; i_12 < 0; i_12++) {
                                lastColor = this.colourBuffer[i_8];
                                lastColor = (((lastColor & -16711936) >>> 8) * transparency & -16711936) + ((lastColor & 0xff00ff) * transparency >> 8 & 0xff00ff);
                                this.colourBuffer[i_8++] = color + lastColor;
                            }
                            i_8 += i_7;
                        }
                    } else {
                        if (type != 2) {
                            throw new IllegalArgumentException();
                        }
                        for (transparency = 0; transparency < height; transparency++) {
                            for (i_11 = -width; i_11 < 0; i_11++) {
                                i_12 = this.colourBuffer[i_8];
                                lastColor = color + i_12;
                                int i_14 = (i_12 & 0xff00ff) + (color & 0xff00ff);
                                i_12 = (i_14 & 0x1000100) + (lastColor - i_14 & 0x10000);
                                this.colourBuffer[i_8++] = lastColor - i_12 | i_12 - (i_12 >>> 8);
                            }
                            i_8 += i_7;
                        }
                    }
                } else {
                    transparency = width >> 3;
                    i_11 = width & 0x7;
                    width = i_8 - 1;
                    for (i_12 = -height; i_12 < 0; i_12++) {
                        if (transparency > 0) {
                            x = transparency;
                            do {
                                ++width;
                                this.colourBuffer[width] = color;
                                ++width;
                                this.colourBuffer[width] = color;
                                ++width;
                                this.colourBuffer[width] = color;
                                ++width;
                                this.colourBuffer[width] = color;
                                ++width;
                                this.colourBuffer[width] = color;
                                ++width;
                                this.colourBuffer[width] = color;
                                ++width;
                                this.colourBuffer[width] = color;
                                ++width;
                                this.colourBuffer[width] = color;
                                --x;
                            } while (x > 0);
                        }
                        if (i_11 > 0) {
                            x = i_11;
                            do {
                                ++width;
                                this.colourBuffer[width] = color;
                                --x;
                            } while (x > 0);
                        }
                        width += i_7;
                    }
                }
            }
        }
    }

    @Override
    public void renderWorldmapTile(int x, int y, int width, int height, int color2, int color, byte[] bytes_7, int i_8, int i_9) {
        if (this.colourBuffer != null && width > 0 && height > 0) {
            int i_10 = 0;
            int i_11 = 0;
            int i_12 = (i_8 << 16) / width;
            int i_13 = (bytes_7.length / i_8 << 16) / height;
            int i_14 = y * this.viewportWidth + x;
            int i_15 = this.viewportWidth - width;
            if (y + height > this.clipBottom) {
                height -= y + height - this.clipBottom;
            }
            int i_16;
            if (y < this.clipTop) {
                i_16 = this.clipTop - y;
                height -= i_16;
                i_14 += i_16 * this.viewportWidth;
                i_11 += i_16 * i_13;
            }
            if (width + x > this.clipRight) {
                i_16 = width + x - this.clipRight;
                width -= i_16;
                i_15 += i_16;
            }
            if (x < this.clipLeft) {
                i_16 = this.clipLeft - x;
                width -= i_16;
                i_14 += i_16;
                i_10 += i_16 * i_12;
                i_15 += i_16;
            }
            i_16 = color2 >>> 24;
            int i_17 = color >>> 24;
            int i_18;
            int i_19;
            int i_20;
            int i_21;
            if (i_9 == 0 || i_9 == 1 && i_16 == 255 && i_17 == 255) {
                i_18 = i_10;
                for (i_19 = -height; i_19 < 0; i_19++) {
                    i_20 = i_8 * (i_11 >> 16);
                    for (i_21 = -width; i_21 < 0; i_21++) {
                        if (bytes_7[i_20 + (i_10 >> 16)] != 0) {
                            this.colourBuffer[i_14++] = color;
                        } else {
                            this.colourBuffer[i_14++] = color2;
                        }
                        i_10 += i_12;
                    }
                    i_11 += i_13;
                    i_10 = i_18;
                    i_14 += i_15;
                }
            } else {
                int i_22;
                int i_23;
                int i_24;
                int i_25;
                if (i_9 == 1) {
                    i_18 = i_10;
                    for (i_19 = -height; i_19 < 0; i_19++) {
                        i_20 = i_8 * (i_11 >> 16);
                        for (i_21 = -width; i_21 < 0; i_21++) {
                            i_22 = color2;
                            if (bytes_7[i_20 + (i_10 >> 16)] != 0) {
                                i_22 = color;
                            }
                            i_23 = i_22 >>> 24;
                            i_24 = 255 - i_23;
                            i_25 = this.colourBuffer[i_14];
                            this.colourBuffer[i_14++] = (i_24 * (i_25 & 0xff00) + i_23 * (i_22 & 0xff00) & 0xff0000) + ((i_25 & 0xff00ff) * i_24 + (i_22 & 0xff00ff) * i_23 & -16711936) >> 8;
                            i_10 += i_12;
                        }
                        i_11 += i_13;
                        i_10 = i_18;
                        i_14 += i_15;
                    }
                } else {
                    if (i_9 != 2) {
                        throw new IllegalArgumentException();
                    }
                    i_18 = i_10;
                    for (i_19 = -height; i_19 < 0; i_19++) {
                        i_20 = i_8 * (i_11 >> 16);
                        for (i_21 = -width; i_21 < 0; i_21++) {
                            i_22 = color2;
                            if (bytes_7[i_20 + (i_10 >> 16)] != 0) {
                                i_22 = color;
                            }
                            if (i_22 != 0) {
                                i_23 = this.colourBuffer[i_14];
                                i_24 = i_22 + i_23;
                                i_25 = (i_23 & 0xff00ff) + (i_22 & 0xff00ff);
                                i_23 = (i_25 & 0x1000100) + (i_24 - i_25 & 0x10000);
                                this.colourBuffer[i_14++] = i_24 - i_23 | i_23 - (i_23 >>> 8);
                            } else {
                                ++i_14;
                            }
                            i_10 += i_12;
                        }
                        i_11 += i_13;
                        i_10 = i_18;
                        i_14 += i_15;
                    }
                }
            }
        }
    }

    @Override
    public void drawCircle(int i_1, int i_2, int i_3, int i_4) {
        int i_22 = i_2;
        if (this.colourBuffer != null) {
            if (i_3 < 0) {
                i_3 = -i_3;
            }

            int i_6 = i_22 - i_3;
            if (i_6 < this.clipTop) {
                i_6 = this.clipTop;
            }

            int i_7 = i_3 + i_22 + 1;
            if (i_7 > this.clipBottom) {
                i_7 = this.clipBottom;
            }

            int i_8 = i_6;
            int i_9 = i_3 * i_3;
            int i_10 = 0;
            int i_11 = i_22 - i_6;
            int i_12 = i_11 * i_11;
            int i_13 = i_12 - i_11;
            if (i_22 > i_7) {
                i_22 = i_7;
            }

            int i_14 = i_4 >>> 24;
            int i_15;
            int i_16;
            int i_17;
            int i_18;
            if ((i_14 != 255)) {
                int i_19;
                int i_20;
                i_4 = (i_14 * (i_4 & 0xff00ff) >> 8 & 0xff00ff) + (i_14 * (i_4 & 0xff00) >> 8 & 0xff00) + (i_14 << 24);

                for (i_15 = 256 - i_14; i_8 < i_22; i_13 -= i_11 + i_11) {
                    while (i_13 <= i_9 || i_12 <= i_9) {
                        i_12 += i_10 + i_10;
                        i_13 += i_10++ + i_10;
                    }

                    i_16 = i_1 - i_10 + 1;
                    if (i_16 < this.clipLeft) {
                        i_16 = this.clipLeft;
                    }

                    i_17 = i_10 + i_1;
                    if (i_17 > this.clipRight) {
                        i_17 = this.clipRight;
                    }

                    i_18 = i_16 + i_8 * this.viewportWidth;

                    for (i_19 = i_16; i_19 < i_17; i_19++) {
                        i_20 = this.colourBuffer[i_18];
                        i_20 = ((i_20 & 0xff00ff) * i_15 >> 8 & 0xff00ff) + (i_15 * (i_20 & 0xff00) >> 8 & 0xff00);
                        this.colourBuffer[i_18++] = i_20 + i_4;
                    }

                    ++i_8;
                    i_12 -= i_11-- + i_11;
                }

                i_10 = i_3;
                i_11 = -i_11;
                i_13 = i_9 + i_11 * i_11;
                i_12 = i_13 - i_3;

                for (i_13 -= i_11; i_8 < i_7; i_12 += i_11++ + i_11) {
                    while (i_13 > i_9 && i_12 > i_9) {
                        i_13 -= i_10-- + i_10;
                        i_12 -= i_10 + i_10;
                    }

                    i_16 = i_1 - i_10;
                    if (i_16 < this.clipLeft) {
                        i_16 = this.clipLeft;
                    }

                    i_17 = i_10 + i_1;
                    if (i_17 > this.clipRight - 1) {
                        i_17 = this.clipRight - 1;
                    }

                    i_18 = i_16 + i_8 * this.viewportWidth;

                    for (i_19 = i_16; i_19 <= i_17; i_19++) {
                        i_20 = this.colourBuffer[i_18];
                        i_20 = (i_15 * (i_20 & 0xff00) >> 8 & 0xff00) + ((i_20 & 0xff00ff) * i_15 >> 8 & 0xff00ff);
                        this.colourBuffer[i_18++] = i_20 + i_4;
                    }

                    ++i_8;
                    i_13 += i_11 + i_11;
                }
            } else {
                while (i_8 < i_22) {
                    while (i_13 <= i_9 || i_12 <= i_9) {
                        i_12 += i_10 + i_10;
                        i_13 += i_10++ + i_10;
                    }

                    i_15 = i_1 - i_10 + 1;
                    if (i_15 < this.clipLeft) {
                        i_15 = this.clipLeft;
                    }

                    i_16 = i_10 + i_1;
                    if (i_16 > this.clipRight) {
                        i_16 = this.clipRight;
                    }

                    i_17 = i_15 + i_8 * this.viewportWidth;

                    for (i_18 = i_15; i_18 < i_16; i_18++) {
                        this.colourBuffer[i_17++] = i_4;
                    }

                    ++i_8;
                    i_12 -= i_11-- + i_11;
                    i_13 -= i_11 + i_11;
                }

                i_10 = i_3;
                i_11 = i_8 - i_22;
                i_13 = i_9 + i_11 * i_11;
                i_12 = i_13 - i_3;

                for (i_13 -= i_11; i_8 < i_7; i_12 += i_11++ + i_11) {
                    while (i_13 > i_9 && i_12 > i_9) {
                        i_13 -= i_10-- + i_10;
                        i_12 -= i_10 + i_10;
                    }

                    i_15 = i_1 - i_10;
                    if (i_15 < this.clipLeft) {
                        i_15 = this.clipLeft;
                    }

                    i_16 = i_10 + i_1;
                    if (i_16 > this.clipRight - 1) {
                        i_16 = this.clipRight - 1;
                    }

                    i_17 = i_15 + i_8 * this.viewportWidth;

                    for (i_18 = i_15; i_18 <= i_16; i_18++) {
                        this.colourBuffer[i_17++] = i_4;
                    }

                    ++i_8;
                    i_13 += i_11 + i_11;
                }

            }
        }

    }
    
    @Override
    public void drawTransparentHorizontalLine(int x, int y, int width, int color, int alpha) {
        int i_12 = x;
        int i_31 = width;
        if (this.colourBuffer != null && y >= this.clipTop && y < this.clipBottom) {
            if (i_12 < this.clipLeft) {
                i_31 -= this.clipLeft - i_12;
                i_12 = this.clipLeft;
            }

            if (i_31 + i_12 > this.clipRight) {
                i_31 = this.clipRight - i_12;
            }

            int i_6 = y * this.viewportWidth + i_12;
            int colorAlpha = color >>> 24;
            int i_8;
            if (alpha == 0 || alpha == 1 && colorAlpha == 255) {
                for (i_8 = 0; i_8 < i_31; i_8++) {
                    this.colourBuffer[i_6 + i_8] = color;
                }
            } else {
                int i_9;
                int i_10;
                if (alpha == 1) {
                    color = (colorAlpha * (color & 0xff00ff) >> 8 & 0xff00ff) + (colorAlpha << 24) + (colorAlpha * (color & 0xff00) >> 8 & 0xff00);
                    i_8 = 256 - colorAlpha;

                    for (i_9 = 0; i_9 < i_31; i_9++) {
                        i_10 = this.colourBuffer[i_9 + i_6];
                        i_10 = ((i_10 & 0xff00ff) * i_8 >> 8 & 0xff00ff) + (i_8 * (i_10 & 0xff00) >> 8 & 0xff00);
                        this.colourBuffer[i_9 + i_6] = i_10 + color;
                    }
                } else {
                    if (alpha != 2) {
                        throw new IllegalArgumentException();
                    }

                    for (i_8 = 0; i_8 < i_31; i_8++) {
                        i_9 = this.colourBuffer[i_6 + i_8];
                        i_10 = i_9 + color;
                        int i_11 = (i_9 & 0xff00ff) + (color & 0xff00ff);
                        i_9 = (i_11 & 0x1000100) + (i_10 - i_11 & 0x10000);
                        this.colourBuffer[i_8 + i_6] = i_10 - i_9 | i_9 - (i_9 >>> 8);
                    }
                }
            }
        }

    }

    public void method14366(int x, int y, int width, int color, int maxY, int i_7, int i_8) {
        if (this.colourBuffer != null && y >= this.clipTop && y < this.clipBottom) {
            int offset = y * this.viewportWidth + x;
            int alphaComponent = color >>> 24;
            int i_11 = i_7 + maxY;
            int deltaY = i_8 % i_11;
            if (alphaComponent == 255) {
                for (int deltaX = 0; deltaX < width; deltaY %= i_11) {
                    if (deltaX + x >= this.clipLeft && deltaX + x < this.clipRight && deltaY < maxY) {
                        this.colourBuffer[deltaX + offset] = color;
                    }

                    ++deltaX;
                    ++deltaY;
                }
            } else {
                int deltaX;
                int currentColor;
                color = (alphaComponent * (color & 0xff00) >> 8 & 0xff00) + (alphaComponent * (color & 0xff00ff) >> 8 & 0xff00ff) + (alphaComponent << 24);
                int transparency = 256 - alphaComponent;

                for (deltaX = 0; deltaX < width; deltaY %= i_11) {
                    if (deltaX + x >= this.clipLeft && deltaX + x < this.clipRight && deltaY < maxY) {
                        currentColor = this.colourBuffer[deltaX + offset];
                        currentColor = ((currentColor & 0xff00ff) * transparency >> 8 & 0xff00ff) + (transparency * (currentColor & 0xff00) >> 8 & 0xff00);
                        this.colourBuffer[deltaX + offset] = currentColor + color;
                    }

                    ++deltaX;
                    ++deltaY;
                }
            }
        }

    }

    @Override
    public void drawTransparentLine(int x, int y, int width, int height, int color, int alpha) {
        if (this.colourBuffer != null) {
            width -= x;
            height -= y;
            if (height == 0) {
                if (width >= 0) {
                    drawTransparentHorizontalLine(x, y, width + 1, color, alpha);
                } else {
                    drawTransparentHorizontalLine(width + x, y, -width + 1, color, alpha);
                }
            } else if (width == 0) {
                if (height >= 0) {
                    drawTransparentVerticalLine(x, y, height + 1, color, alpha);
                } else {
                    drawTransparentVerticalLine(x, y + height, -height + 1, color, alpha);
                }
            } else {
                if (width + height < 0) {
                    x += width;
                    width = -width;
                    y += height;
                    height = -height;
                }

                int i_7;
                int i_8;
                int i_9;
                int i_10;
                int i_11;
                int i_12;
                int i_13;
                if (width > height) {
                    y <<= 16;
                    y += 32768;
                    height <<= 16;
                    i_7 = (int) Math.floor(0.5D + (double) height / width);
                    width += x;
                    if (x < this.clipLeft) {
                        y += i_7 * (this.clipLeft - x);
                        x = this.clipLeft;
                    }

                    if (width >= this.clipRight) {
                        width = this.clipRight - 1;
                    }

                    i_8 = color >>> 24;
                    if (alpha != 0 && (alpha != 1 || i_8 != 255)) {
                        if (alpha == 1) {
                            color = (i_8 * (color & 0xff00ff) >> 8 & 0xff00ff) + (i_8 * (color & 0xff00) >> 8 & 0xff00) + (i_8 << 24);

                            for (i_9 = 256 - i_8; x <= width; x++) {
                                i_10 = y >> 16;
                                if (i_10 >= this.clipTop && i_10 < this.clipBottom) {
                                    i_11 = i_10 * this.viewportWidth + x;
                                    i_12 = this.colourBuffer[i_11];
                                    i_12 = (i_9 * (i_12 & 0xff00) >> 8 & 0xff00) + ((i_12 & 0xff00ff) * i_9 >> 8 & 0xff00ff);
                                    this.colourBuffer[i_11] = color + i_12;
                                }

                                y += i_7;
                            }
                        } else {
                            if (alpha != 2) {
                                throw new IllegalArgumentException();
                            }

                            while (x <= width) {
                                i_9 = y >> 16;
                                if (i_9 >= this.clipTop && i_9 < this.clipBottom) {
                                    i_10 = i_9 * this.viewportWidth + x;
                                    i_11 = this.colourBuffer[i_10];
                                    i_12 = color + i_11;
                                    i_13 = (i_11 & 0xff00ff) + (color & 0xff00ff);
                                    i_11 = (i_13 & 0x1000100) + (i_12 - i_13 & 0x10000);
                                    this.colourBuffer[i_10] = i_12 - i_11 | i_11 - (i_11 >>> 8);
                                }

                                y += i_7;
                                ++x;
                            }
                        }
                    } else {
                        while (x <= width) {
                            i_9 = y >> 16;
                            if (i_9 >= this.clipTop && i_9 < this.clipBottom) {
                                this.colourBuffer[i_9 * this.viewportWidth + x] = color;
                            }

                            y += i_7;
                            ++x;
                        }
                    }
                } else {
                    x <<= 16;
                    x += 32768;
                    width <<= 16;
                    i_7 = (int) Math.floor((double) width / height + 0.5D);
                    height += y;
                    if (y < this.clipTop) {
                        x += i_7 * (this.clipTop - y);
                        y = this.clipTop;
                    }

                    if (height >= this.clipBottom) {
                        height = this.clipBottom - 1;
                    }

                    i_8 = color >>> 24;
                    if (alpha == 0 || alpha == 1 && i_8 == 255) {
                        while (y <= height) {
                            i_9 = x >> 16;
                            if (i_9 >= this.clipLeft && i_9 < this.clipRight) {
                                this.colourBuffer[y * this.viewportWidth + i_9] = color;
                            }

                            x += i_7;
                            ++y;
                        }
                    } else if (alpha == 1) {
                        color = (i_8 * (color & 0xff00ff) >> 8 & 0xff00ff) + (i_8 * (color & 0xff00) >> 8 & 0xff00) + (i_8 << 24);

                        for (i_9 = 256 - i_8; y <= height; y++) {
                            i_10 = x >> 16;
                            if (i_10 >= this.clipLeft && i_10 < this.clipRight) {
                                i_11 = y * this.viewportWidth + i_10;
                                i_12 = this.colourBuffer[i_11];
                                i_12 = ((i_12 & 0xff00ff) * i_9 >> 8 & 0xff00ff) + (i_9 * (i_12 & 0xff00) >> 8 & 0xff00);
                                this.colourBuffer[y * this.viewportWidth + i_10] = color + i_12;
                            }

                            x += i_7;
                        }
                    } else {
                        if (alpha != 2) {
                            throw new IllegalArgumentException();
                        }

                        while (y <= height) {
                            i_9 = x >> 16;
                            if (i_9 >= this.clipLeft && i_9 < this.clipRight) {
                                i_10 = y * this.viewportWidth + i_9;
                                i_11 = this.colourBuffer[i_10];
                                i_12 = color + i_11;
                                i_13 = (i_11 & 0xff00ff) + (color & 0xff00ff);
                                i_11 = (i_13 & 0x1000100) + (i_12 - i_13 & 0x10000);
                                this.colourBuffer[i_10] = i_12 - i_11 | i_11 - (i_11 >>> 8);
                            }

                            x += i_7;
                            ++y;
                        }
                    }
                }
            }
        }
    }
    
    // 839
	/*@Override
	public void draw_transparent_line(int x, int y, int width, int height, int color, int alpha) {
		if (colour_buffer != null) {
			width -= x;
			height -= y;
			if (0 == height) {
				if (width >= 0) {
					draw_transparent_horizontal_line(x, y, width + 1, color, alpha);
				} else {
					draw_transparent_horizontal_line(x + width, y, 1 + -width, color, alpha);
				}
			} else if (0 == width) {
				if (height >= 0) {
					draw_transparent_vertical_line(x, y, height + 1, color, alpha);
				} else {
					draw_transparent_vertical_line(x, height + y, 1 + -height, color, alpha);
				}
			} else {
				if (height + width < 0) {
					x += width;
					width = -width;
					y += height;
					height = -height;
				}
				if (width > height) {
					y <<= 16;
					y += 32768;
					height <<= 16;
					int i_389_ = (int) Math.floor(0.5 + (double) height / (double) width);
					width += x;
					if (x < clip_left) {
						y += i_389_ * (clip_left - x);
						x = clip_left;
					}
					if (width >= clip_right) {
						width = clip_right - 1;
					}
					int i_390_ = color >>> 24;
					if (alpha == 0 || alpha == 1 && 255 == i_390_) {
						for (; x <= width; x++) {
							int i_391_ = y >> 16;
							if (i_391_ >= clip_bottom && i_391_ < clip_top * -68852597) {
								colour_buffer[viewport_width * i_391_ + x] = color;
							}
							y += i_389_;
						}
					} else if (alpha == 1) {
						color = (i_390_ << 24) + (i_390_ * (color & 0xff00ff) >> 8 & 0xff00ff) + (i_390_ * (color & 0xff00) >> 8 & 0xff00);
						int i_392_ = 256 - i_390_;
						for (; x <= width; x++) {
							int i_393_ = y >> 16;
							if (i_393_ >= clip_bottom && i_393_ < clip_top) {
								int i_394_ = x + viewport_width * i_393_;
								int i_395_ = colour_buffer[i_394_];
								i_395_ = (i_392_ * (i_395_ & 0xff00) >> 8 & 0xff00) + ((i_395_ & 0xff00ff) * i_392_ >> 8 & 0xff00ff);
								colour_buffer[i_394_] = i_395_ + color;
							}
							y += i_389_;
						}
					} else if (alpha == 2) {
						for (; x <= width; x++) {
							int i_396_ = y >> 16;
							if (i_396_ >= clip_bottom && i_396_ < clip_top) {
								int i_397_ = viewport_width * i_396_ + x;
								int i_398_ = colour_buffer[i_397_];
								int i_399_ = color + i_398_;
								int i_400_ = (i_398_ & 0xff00ff) + (color & 0xff00ff);
								i_398_ = (i_399_ - i_400_ & 0x10000) + (i_400_ & 0x1000100);
								colour_buffer[i_397_] = i_399_ - i_398_ | i_398_ - (i_398_ >>> 8);
							}
							y += i_389_;
						}
					} else {
						throw new IllegalArgumentException();
					}
				} else {
					x <<= 16;
					x += 32768;
					width <<= 16;
					int i_401_ = (int) Math.floor(0.5 + (double) width / (double) height);
					height += y;
					if (y < clip_bottom) {
						x += (clip_bottom - y) * i_401_;
						y = clip_bottom;
					}
					if (height >= clip_top) {
						height = clip_top - 1;
					}
					int i_402_ = color >>> 24;
					if (0 == alpha || alpha == 1 && i_402_ == 255) {
						for (; y <= height; y++) {
							int i_403_ = x >> 16;
							if (i_403_ >= clip_left && i_403_ < clip_right) {
								colour_buffer[y * viewport_width + i_403_] = color;
							}
							x += i_401_;
						}
					} else if (1 == alpha) {
						color = (i_402_ << 24) + (i_402_ * (color & 0xff00) >> 8 & 0xff00) + ((color & 0xff00ff) * i_402_ >> 8 & 0xff00ff);
						int i_404_ = 256 - i_402_;
						for (; y <= height; y++) {
							int i_405_ = x >> 16;
							if (i_405_ >= clip_left && i_405_ < clip_right) {
								int i_406_ = viewport_width * 464577083 * y + i_405_;
								int i_407_ = colour_buffer[i_406_];
								i_407_ = (i_404_ * (i_407_ & 0xff00) >> 8 & 0xff00) + (i_404_ * (i_407_ & 0xff00ff) >> 8 & 0xff00ff);
								colour_buffer[i_405_ + y * viewport_width * 464577083] = i_407_ + color;
							}
							x += i_401_;
						}
					} else if (alpha == 2) {
						for (; y <= height; y++) {
							int i_408_ = x >> 16;
							if (i_408_ >= clip_left && i_408_ < clip_right) {
								int i_409_ = i_408_ + viewport_width * y;
								int i_410_ = colour_buffer[i_409_];
								int i_411_ = i_410_ + color;
								int i_412_ = (color & 0xff00ff) + (i_410_ & 0xff00ff);
								i_410_ = (i_412_ & 0x1000100) + (i_411_ - i_412_ & 0x10000);
								colour_buffer[i_409_] = i_411_ - i_410_ | i_410_ - (i_410_ >>> 8);
							}
							x += i_401_;
						}
					} else {
						throw new IllegalArgumentException();
					}
				}
			}
		}
	}*/

    @Override
    public Matrix44 method8449() {
        ThreadResourceSM class1851 = getThreadResource(Thread.currentThread());
        return class1851.aClass3842346;
    }

    @Override
    public void method8669(int i_1, int i_2, int i_3, int i_4, int i_5, ComponentSurface class4557, int i_8, int i_9) {
        int i_11 = i_1;
        int i_31 = i_3;
        int i_24 = i_2;
        int i_41 = i_4;
        if (this.colourBuffer != null) {
            ComponentDimensionSM packetsdecoder10 = (ComponentDimensionSM) class4557;
            int[] ints_11 = packetsdecoder10.anIntArray9077;
            int[] ints_12 = packetsdecoder10.anIntArray9078;
            int i_13 = Math.max(this.clipTop, i_9);
            int i_14 = Math.min(this.clipBottom, ints_11.length + i_9);
            i_31 -= i_11;
            i_41 -= i_24;
            if (i_31 + i_41 < 0) {
                i_11 += i_31;
                i_31 = -i_31;
                i_24 += i_41;
                i_41 = -i_41;
            }

            int i_15;
            int i_16;
            int i_17;
            int i_18;
            int i_19;
            int i_20;
            int i_21;
            int i_22;
            int i_23;
            if (i_31 > i_41) {
                i_24 <<= 16;
                i_24 += 32768;
                i_41 <<= 16;
                i_15 = (int) Math.floor((double) i_41 / i_31 + 0.5D);
                i_31 += i_11;
                if (i_11 < this.clipLeft) {
                    i_24 += i_15 * (this.clipLeft - i_11);
                    i_11 = this.clipLeft;
                }

                if (i_31 >= this.clipRight) {
                    i_31 = this.clipRight - 1;
                }

                i_16 = i_5 >>> 24;
                if (i_16 == 255) {
                    while (i_11 <= i_31) {
                        i_17 = i_24 >> 16;
                        i_18 = i_17 - i_9;
                        if (i_17 >= i_13 && i_17 < i_14) {
                            i_19 = i_8 + ints_11[i_18];
                            if (i_11 >= i_19 && i_11 < i_19 + ints_12[i_18]) {
                                this.colourBuffer[i_17 * this.viewportWidth + i_11] = i_5;
                            }
                        }

                        i_24 += i_15;
                        ++i_11;
                    }
                } else {
                    i_5 = (i_16 * (i_5 & 0xff00ff) >> 8 & 0xff00ff) + (i_16 * (i_5 & 0xff00) >> 8 & 0xff00) + (i_16 << 24);

                    for (i_17 = 256 - i_16; i_11 <= i_31; i_11++) {
                        i_18 = i_24 >> 16;
                        i_19 = i_18 - i_9;
                        if (i_18 >= i_13 && i_18 < i_14) {
                            i_20 = i_8 + ints_11[i_19];
                            if (i_11 >= i_20 && i_11 < i_20 + ints_12[i_19]) {
                                i_21 = i_18 * this.viewportWidth + i_11;
                                i_22 = this.colourBuffer[i_21];
                                i_22 = (i_17 * (i_22 & 0xff00) >> 8 & 0xff00) + ((i_22 & 0xff00ff) * i_17 >> 8 & 0xff00ff);
                                this.colourBuffer[i_21] = i_5 + i_22;
                            }
                        }

                        i_24 += i_15;
                    }
                }
            } else {
                i_11 <<= 16;
                i_11 += 32768;
                i_31 <<= 16;
                i_15 = (int) Math.floor(0.5D + (double) i_31 / i_41);
                i_41 += i_24;
                if (i_24 < i_13) {
                    i_11 += (i_13 - i_24) * i_15;
                    i_24 = i_13;
                }

                if (i_41 >= i_14) {
                    i_41 = i_14 - 1;
                }

                i_16 = i_5 >>> 24;
                if ((i_16 != 255)) {
                    i_5 = (i_16 * (i_5 & 0xff00) >> 8 & 0xff00) + (i_16 * (i_5 & 0xff00ff) >> 8 & 0xff00ff) + (i_16 << 24);

                    for (i_17 = 256 - i_16; i_24 <= i_41; i_24++) {
                        i_18 = i_11 >> 16;
                        i_19 = i_24 - i_9;
                        i_20 = i_8 + ints_11[i_19];
                        if (i_18 >= this.clipLeft && i_18 < this.clipRight && i_18 >= i_20 && i_18 < i_20 + ints_12[i_19]) {
                            i_21 = i_24 * this.viewportWidth + i_18;
                            i_22 = this.colourBuffer[i_21];
                            i_22 = (i_17 * (i_22 & 0xff00) >> 8 & 0xff00) + ((i_22 & 0xff00ff) * i_17 >> 8 & 0xff00ff);
                            this.colourBuffer[i_24 * this.viewportWidth + i_18] = i_5 + i_22;
                        }

                        i_11 += i_15;
                    }
                } else {
                    while (i_24 <= i_41) {
                        i_17 = i_11 >> 16;
                        i_18 = i_24 - i_9;
                        i_19 = i_8 + ints_11[i_18];
                        if (i_17 >= this.clipLeft && i_17 < this.clipRight && i_17 >= i_19 && i_17 < i_19 + ints_12[i_18]) {
                            this.colourBuffer[i_24 * this.viewportWidth + i_17] = i_5;
                        }

                        i_11 += i_15;
                        ++i_24;
                    }
                }
            }
        }

    }

    @Override
    public void drawTexturedLine(int i_1, int i_2, int i_3, int i_4, int i_5, ComponentSurface class4557, int i_8, int i_9, int i_10, int i_11, int i_12) {
    	if (this.colourBuffer != null) {
            ComponentDimensionSM packetsdecoder13 = (ComponentDimensionSM) class4557;
            int[] ints_14 = packetsdecoder13.anIntArray9077;
            int[] ints_15 = packetsdecoder13.anIntArray9078;
            int i_16 = Math.max(this.clipTop, i_9);
            int i_17 = Math.min(this.clipBottom, ints_14.length + i_9);
            i_12 <<= 8;
            i_10 <<= 8;
            i_11 <<= 8;
            int i_18 = i_11 + i_10;
            i_12 %= i_18;
            i_3 -= i_1;
            i_4 -= i_2;
            int i_19;
            int i_20;
            if (i_3 + i_4 < 0) {
                i_19 = (int) (Math.sqrt(i_3 * i_3 + i_4 * i_4) * 256.0D);
                i_20 = i_19 % i_18;
                i_12 = i_10 + i_18 - i_12 - i_20;
                i_12 %= i_18;
                if (i_12 < 0) {
                    i_12 += i_18;
                }

                i_1 += i_3;
                i_3 = -i_3;
                i_2 += i_4;
                i_4 = -i_4;
            }

            int i_21;
            int i_22;
            int i_23;
            int i_24;
            int i_25;
            int i_26;
            int i_27;
            if (i_3 > i_4) {
                i_2 <<= 16;
                i_2 += 32768;
                i_4 <<= 16;
                i_19 = (int) Math.floor((double) i_4 / i_3 + 0.5D);
                i_3 += i_1;
                i_20 = i_5 >>> 24;
                i_21 = (int) Math.sqrt((i_19 >> 8) * (i_19 >> 8) + 65536);
                if (i_20 == 255) {
                    while (i_1 <= i_3) {
                        i_22 = i_2 >> 16;
                        i_23 = i_22 - i_9;
                        if (i_1 >= this.clipLeft && i_1 < this.clipRight && i_22 >= i_16 && i_22 < i_17 && i_12 < i_10) {
                            i_24 = i_8 + ints_14[i_23];
                            if (i_1 >= i_24 && i_1 < i_24 + ints_15[i_23]) {
                                this.colourBuffer[i_22 * this.viewportWidth + i_1] = i_5;
                            }
                        }

                        i_2 += i_19;
                        ++i_1;
                        i_12 += i_21;
                        i_12 %= i_18;
                    }
                } else {
                    i_5 = (i_20 * (i_5 & 0xff00ff) >> 8 & 0xff00ff) + (i_20 * (i_5 & 0xff00) >> 8 & 0xff00) + (i_20 << 24);

                    for (i_22 = 256 - i_20; i_1 <= i_3; i_12 %= i_18) {
                        i_23 = i_2 >> 16;
                        i_24 = i_23 - i_9;
                        if (i_1 >= this.clipLeft && i_1 < this.clipRight && i_23 >= i_16 && i_23 < i_17 && i_12 < i_10) {
                            i_25 = i_8 + ints_14[i_24];
                            if (i_1 >= i_25 && i_1 < i_25 + ints_15[i_24]) {
                                i_26 = i_23 * this.viewportWidth + i_1;
                                i_27 = this.colourBuffer[i_26];
                                i_27 = (i_22 * (i_27 & 0xff00) >> 8 & 0xff00) + ((i_27 & 0xff00ff) * i_22 >> 8 & 0xff00ff);
                                this.colourBuffer[i_26] = i_5 + i_27;
                            }
                        }

                        i_2 += i_19;
                        ++i_1;
                        i_12 += i_21;
                    }
                }
            } else {
                i_1 <<= 16;
                i_1 += 32768;
                i_3 <<= 16;
                i_19 = (int) Math.floor(0.5D + (double) i_3 / i_4);
                i_20 = (int) Math.sqrt((i_19 >> 8) * (i_19 >> 8) + 65536);
                i_4 += i_2;
                i_21 = i_5 >>> 24;
                if (i_21 == 255) {
                    while (i_2 <= i_4) {
                        i_22 = i_1 >> 16;
                        i_23 = i_2 - i_9;
                        if (i_2 >= i_16 && i_2 < i_17 && i_22 >= this.clipLeft && i_22 < this.clipRight && i_12 < i_10 && i_22 >= i_8 + ints_14[i_23] && i_22 < i_8 + ints_14[i_23] + ints_15[i_23]) {
                            this.colourBuffer[i_2 * this.viewportWidth + i_22] = i_5;
                        }

                        i_1 += i_19;
                        ++i_2;
                        i_12 += i_20;
                        i_12 %= i_18;
                    }
                } else {
                    i_5 = (i_21 * (i_5 & 0xff00ff) >> 8 & 0xff00ff) + (i_21 * (i_5 & 0xff00) >> 8 & 0xff00) + (i_21 << 24);

                    for (i_22 = 256 - i_21; i_2 <= i_4; i_12 %= i_18) {
                        i_23 = i_1 >> 16;
                        i_24 = i_2 - i_9;
                        if (i_2 >= i_16 && i_2 < i_17 && i_23 >= this.clipLeft && i_23 < this.clipRight && i_12 < i_10 && i_23 >= i_8 + ints_14[i_24] && i_23 < i_8 + ints_14[i_24] + ints_15[i_24]) {
                            i_25 = i_2 * this.viewportWidth + i_23;
                            i_26 = this.colourBuffer[i_25];
                            i_26 = (i_22 * (i_26 & 0xff00) >> 8 & 0xff00) + ((i_26 & 0xff00ff) * i_22 >> 8 & 0xff00ff);
                            this.colourBuffer[i_2 * this.viewportWidth + i_23] = i_5 + i_26;
                        }

                        i_1 += i_19;
                        ++i_2;
                        i_12 += i_20;
                    }
                }
            }
        }

    }

    @Override
    public int isOnScreen(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        int flags = 0;
        float f_8 = i_3 * this.floorMatrix.faceList[10] + i_2 * this.floorMatrix.faceList[6] + this.floorMatrix.faceList[14] + i_1 * this.floorMatrix.faceList[2];
        float f_9 = this.floorMatrix.faceList[6] * i_5 + this.floorMatrix.faceList[14] + this.floorMatrix.faceList[2] * i_4 + this.floorMatrix.faceList[10] * i_6;
        float f_10 = this.floorMatrix.faceList[15] + i_1 * this.floorMatrix.faceList[3] + i_2 * this.floorMatrix.faceList[7] + i_3 * this.floorMatrix.faceList[11];
        float f_11 = i_5 * this.floorMatrix.faceList[7] + this.floorMatrix.faceList[15] + this.floorMatrix.faceList[3] * i_4 + i_6 * this.floorMatrix.faceList[11];
        if (f_8 < -f_10 && f_9 < -f_11) {
            flags |= 0x10;
        } else if (f_8 > f_10 && f_9 > f_11) {
            flags |= 0x20;
        }

        float f_12 = this.floorMatrix.faceList[4] * i_2 + this.floorMatrix.faceList[12] + i_1 * this.floorMatrix.faceList[0] + i_3 * this.floorMatrix.faceList[8];
        float f_13 = i_6 * this.floorMatrix.faceList[8] + i_4 * this.floorMatrix.faceList[0] + this.floorMatrix.faceList[12] + this.floorMatrix.faceList[4] * i_5;
        if (f_12 < -f_10 && f_13 < -f_11) {
            flags |= 0x1;
        }

        if (f_12 > f_10 && f_13 > f_11) {
            flags |= 0x2;
        }

        float f_14 = this.floorMatrix.faceList[5] * i_2 + i_1 * this.floorMatrix.faceList[1] + this.floorMatrix.faceList[13] + this.floorMatrix.faceList[9] * i_3;
        float f_15 = this.floorMatrix.faceList[13] + this.floorMatrix.faceList[1] * i_4 + this.floorMatrix.faceList[5] * i_5 + i_6 * this.floorMatrix.faceList[9];
        if (f_14 < -f_10 && f_15 < -f_11) {
            flags |= 0x4;
        }

        if (f_14 > f_10 && f_15 > f_11) {
            flags |= 0x8;
        }

        return flags;
    }

    @Override
    public void setNativeHeap(NativeMemoryNode class282Sub11) {
    }

    @Override
    public NativeSprite generateEmptySprite(int i_1, int i_2, boolean bool3, boolean bool4) {
        return bool3 ? new TransparentSpriteSM(this, i_1, i_2) : new OpaqueSpriteSM(this, i_1, i_2);
    }

    @Override
    public void calculateProjection(float width, float height, float length, float[] matrix) {
        float z = length * this.floorMatrix.faceList[11] + this.floorMatrix.faceList[7] * height + width * this.floorMatrix.faceList[3] + this.floorMatrix.faceList[15];
        float x = height * this.floorMatrix.faceList[4] + width * this.floorMatrix.faceList[0] + this.floorMatrix.faceList[12] + this.floorMatrix.faceList[8] * length;
        float y = height * this.floorMatrix.faceList[5] + width * this.floorMatrix.faceList[1] + this.floorMatrix.faceList[13] + length * this.floorMatrix.faceList[9];
        float floorOffset = length * this.aClass3848994.faceList[10] + this.aClass3848994.faceList[6] * height + width * this.aClass3848994.faceList[2] + this.aClass3848994.faceList[14];
        matrix[0] = this.aFloat9003 + x * this.aFloat8976 / z;
        matrix[1] = this.aFloat8982 + y * this.aFloat9004 / z;
        matrix[2] = floorOffset;
    }

    @Override
    public NativeSprite createNativeSprite(int i_1, int i_2, int i_3, int i_4, boolean bool5) {
        if (this.colourBuffer == null) {
            throw new IllegalStateException("");
        } else {
            int[] ints_6 = new int[i_3 * i_4];
            int i_7 = i_2 * this.viewportWidth + i_1;
            int i_8 = this.viewportWidth - i_3;

            for (int i_9 = 0; i_9 < i_4; i_9++) {
                int i_10 = i_3 * i_9;

                for (int i_11 = 0; i_11 < i_3; i_11++) {
                    ints_6[i_10 + i_11] = this.colourBuffer[i_7++];
                }

                i_7 += i_8;
            }

            if (bool5) {
                return new TransparentSpriteSM(this, ints_6, i_3, i_4);
            } else {
                return new OpaqueSpriteSM(this, ints_6, i_3, i_4);
            }
        }
    }

    @Override
    public ComponentSurface createComponentSurface(int i_1, int i_2, int[] ints_3, int[] ints_4) {
        return new ComponentDimensionSM(ints_3, ints_4);
    }

    @Override
    public void resetDepthClippingMask() {
        this.aFloat8978 = 1.0f;
        this.aFloat8985 = 0.0f;

        for (int i_3 = 0; i_3 < this.executingThreadCount; i_3++) {
            ThreadResourceSM class1854 = this.renderingThreads[i_3];
            RasterizerSM choppyitemfixclass5 = class1854.rasterizer;
            choppyitemfixclass5.aFloat1683 = this.aFloat8978;
            choppyitemfixclass5.aFloat1682 = this.aFloat8985;
        }

    }

    @Override
    public Model createModel(Mesh mesh, int modelFlags, int preferenceFlags, int ambient, int contrast) {
        return new ModelSM(this, mesh, modelFlags, ambient, contrast, preferenceFlags);
    }

    @Override
    public int mergeFunctionMasks(int base, int mask) {
        return base | mask;
    }

    @Override
    public Ground createGround(int sizeX, int sizeY, int[][] heightsNext, int[][] heightsCurrent, int unused, int tileFunctionMask) {
        return new GroundSM(this, tileFunctionMask, sizeX, sizeY, heightsNext, heightsCurrent);
    }


    @Override
    public Matrix43 getRenderingMatrix() {
        ThreadResourceSM class1851 = getThreadResource(Thread.currentThread());
        return class1851.aClass2942314;
    }

    @Override
    public boolean bloomEnabled() {
        return false;
    }

    @Override
    public void copyLights(int i_1, PointLight[] arr_2) {
    }

    public void initializeThreads(int threadCount) {
        this.executingThreadCount = threadCount;
        this.renderingThreads = new ThreadResourceSM[this.executingThreadCount];
        for (int threadIndex = 0; threadIndex < this.executingThreadCount; threadIndex++) {
            this.renderingThreads[threadIndex] = new ThreadResourceSM(this);
        }
    }

    public ThreadResourceSM getThreadResource(Runnable runnable) {
        for (int threadIndex = 0; threadIndex < this.executingThreadCount; threadIndex++) {
            if (runnable == this.renderingThreads[threadIndex].runnable) {
                return this.renderingThreads[threadIndex];
            }
        }
        return null;
    }

    @Override
    public void renderParticles(ParticleWrapper particles) {
        renderParticles(this.colourBuffer != null, this.depthBuffer != null, particles);
    }


    public void renderMaterializedSprite(boolean bool1, boolean bool2, PointEntity pointEntity, int i_5, int i_6, float f_7, int i_8) {
        int materialId = pointEntity.materialId;
        int i_10 = i_8;
        i_8 <<= 1;
        if (materialId == -1) {
            method14373(bool2, i_5, i_6, f_7, i_10, pointEntity.colorArgb, 1);
        } else {
            if (materialId != this.renderedMaterialId) {
                NativeSprite textureSprite = (NativeSprite) this.animatedSpriteMaterials.get(materialId);
                if (textureSprite == null) {
                    int[] pizels = getAnimatedMaterialArgb(materialId);
                    if (pizels == null) {
                        return;
                    }
                    int spriteSize = isSmallTexture(materialId) ? 64 : this.materialSize;
                    textureSprite = createNativeSprite(pizels, spriteSize, spriteSize, spriteSize);
                    this.animatedSpriteMaterials.put(textureSprite, materialId);
                }
                this.renderedMaterialId = materialId;
                this.renderedNativeSprite = textureSprite;
            }
            ++i_8;
            ((SpriteSM) this.renderedNativeSprite).method14247(bool1, bool2, false, i_5 - i_10, i_6 - i_10, f_7, i_8, i_8, 0, pointEntity.colorArgb, 1, false);
        }

    }

    public void drawBillboard(boolean colorWrite, boolean depthWrite, boolean transparent, int x, int y, float z, int size2d, int size3d, int materialId, int color, int blendType, int shape) {
        if (size2d != 0 && size3d != 0) {
            if (materialId != 0xffff) {
                MaterialType material = super.materialProvider.getMaterial(materialId);
                if (!material.detailsOnly) {
                    if (materialId != this.renderedMaterialId) {
                        NativeSprite nativeSprite = (NativeSprite) this.animatedSpriteMaterials.get(materialId);
                        if (nativeSprite == null) {
                            int[] texturePixels = getAnimatedMaterialArgb(materialId);
                            if (texturePixels == null) {
                                return;
                            }
                            int size = isSmallTexture(materialId) ? 64 : this.materialSize;
                            nativeSprite = createNativeSprite(texturePixels, size, size, size);
                            this.animatedSpriteMaterials.put(nativeSprite, materialId);
                        }
                        this.renderedMaterialId = materialId;
                        this.renderedNativeSprite = nativeSprite;
                    }
                    ((SpriteSM) this.renderedNativeSprite).method14247(colorWrite, depthWrite, transparent, x - size2d, y - size3d, z, size2d << 1, size3d << 1, blendType, color, shape, material.effectCombiner != 2);
                    return;
                }
            }
            method14373(depthWrite, x, y, z, size2d, color, shape);
        }

    }

    public void method14373(boolean writeDepth, int x, int y, float z, int deltaY, int color, int shape) {
        if (this.colourBuffer != null) {
            if (deltaY < 0) {
                deltaY = -deltaY;
            }

            int highestY = y - deltaY;
            if (highestY < this.clipTop) {
                highestY = this.clipTop;
            }

            int lowestY = y + deltaY + 1;
            if (lowestY > this.clipBottom) {
                lowestY = this.clipBottom;
            }

            int i_10 = highestY;
            int i_11 = deltaY * deltaY;
            int deltaX = 0;
            int i_13 = y - highestY;
            int i_14 = i_13 * i_13;
            int i_15 = i_14 - i_13;
            if (y > lowestY) {
                y = lowestY;
            }

            int alpha = color >>> 24;
            int i_17;
            int i_18;
            int i_19;
            int i_20;
            if (shape != 0 && (shape != 1 || alpha != 255)) {
                int i_21;
                int i_22;
                if (shape == 1) {
                    color = ((color & 0xff00ff) * alpha >> 8 & 0xff00ff) + (alpha << 24) + (alpha * (color & 0xff00) >> 8 & 0xff00);

                    for (i_17 = 256 - alpha; i_10 < y; i_15 -= i_13 + i_13) {
                        while (i_15 <= i_11 || i_14 <= i_11) {
                            i_14 += deltaX + deltaX;
                            i_15 += deltaX++ + deltaX;
                        }

                        i_18 = x - deltaX + 1;
                        if (i_18 < this.clipLeft) {
                            i_18 = this.clipLeft;
                        }

                        i_19 = deltaX + x;
                        if (i_19 > this.clipRight) {
                            i_19 = this.clipRight;
                        }

                        i_20 = i_18 + i_10 * this.viewportWidth;

                        for (i_21 = i_18; i_21 < i_19; i_21++) {
                            if (!writeDepth || z < this.depthBuffer[i_20]) {
                                i_22 = this.colourBuffer[i_20];
                                i_22 = ((i_22 & 0xff00ff) * i_17 >> 8 & 0xff00ff) + (i_17 * (i_22 & 0xff00) >> 8 & 0xff00);
                                this.colourBuffer[i_20] = i_22 + color;
                            }

                            ++i_20;
                        }

                        ++i_10;
                        i_14 -= i_13-- + i_13;
                    }

                    deltaX = deltaY;
                    i_13 = -i_13;
                    i_15 = i_11 + i_13 * i_13;
                    i_14 = i_15 - deltaY;

                    for (i_15 -= i_13; i_10 < lowestY; i_14 += i_13++ + i_13) {
                        while (i_15 > i_11 && i_14 > i_11) {
                            i_15 -= deltaX-- + deltaX;
                            i_14 -= deltaX + deltaX;
                        }

                        i_18 = x - deltaX;
                        if (i_18 < this.clipLeft) {
                            i_18 = this.clipLeft;
                        }

                        i_19 = deltaX + x;
                        if (i_19 > this.clipRight - 1) {
                            i_19 = this.clipRight - 1;
                        }

                        i_20 = i_18 + i_10 * this.viewportWidth;

                        for (i_21 = i_18; i_21 <= i_19; i_21++) {
                            if (!writeDepth || z < this.depthBuffer[i_20]) {
                                i_22 = this.colourBuffer[i_20];
                                i_22 = ((i_22 & 0xff00ff) * i_17 >> 8 & 0xff00ff) + (i_17 * (i_22 & 0xff00) >> 8 & 0xff00);
                                this.colourBuffer[i_20] = i_22 + color;
                            }

                            ++i_20;
                        }

                        ++i_10;
                        i_15 += i_13 + i_13;
                    }
                } else {
                    if (shape != 2) {
                        throw new IllegalArgumentException();
                    }

                    int i_23;
                    while (i_10 < y) {
                        while (i_15 <= i_11 || i_14 <= i_11) {
                            i_14 += deltaX + deltaX;
                            i_15 += deltaX++ + deltaX;
                        }

                        i_17 = x - deltaX + 1;
                        if (i_17 < this.clipLeft) {
                            i_17 = this.clipLeft;
                        }

                        i_18 = deltaX + x;
                        if (i_18 > this.clipRight) {
                            i_18 = this.clipRight;
                        }

                        i_19 = i_17 + i_10 * this.viewportWidth;

                        for (i_20 = i_17; i_20 < i_18; i_20++) {
                            if (!writeDepth || z < this.depthBuffer[i_19]) {
                                i_21 = this.colourBuffer[i_19];
                                i_22 = i_21 + color;
                                i_23 = (i_21 & 0xff00ff) + (color & 0xff00ff);
                                i_21 = (i_23 & 0x1000100) + (i_22 - i_23 & 0x10000);
                                this.colourBuffer[i_19] = i_22 - i_21 | i_21 - (i_21 >>> 8);
                            }

                            ++i_19;
                        }

                        ++i_10;
                        i_14 -= i_13-- + i_13;
                        i_15 -= i_13 + i_13;
                    }

                    deltaX = deltaY;
                    i_13 = -i_13;
                    i_15 = i_11 + i_13 * i_13;
                    i_14 = i_15 - deltaY;

                    for (i_15 -= i_13; i_10 < lowestY; i_14 += i_13++ + i_13) {
                        while (i_15 > i_11 && i_14 > i_11) {
                            i_15 -= deltaX-- + deltaX;
                            i_14 -= deltaX + deltaX;
                        }

                        i_17 = x - deltaX;
                        if (i_17 < this.clipLeft) {
                            i_17 = this.clipLeft;
                        }

                        i_18 = deltaX + x;
                        if (i_18 > this.clipRight - 1) {
                            i_18 = this.clipRight - 1;
                        }

                        i_19 = i_17 + i_10 * this.viewportWidth;

                        for (i_20 = i_17; i_20 <= i_18; i_20++) {
                            if (!writeDepth || z < this.depthBuffer[i_19]) {
                                i_21 = this.colourBuffer[i_19];
                                i_22 = i_21 + color;
                                i_23 = (i_21 & 0xff00ff) + (color & 0xff00ff);
                                i_21 = (i_23 & 0x1000100) + (i_22 - i_23 & 0x10000);
                                this.colourBuffer[i_19] = i_22 - i_21 | i_21 - (i_21 >>> 8);
                            }

                            ++i_19;
                        }

                        ++i_10;
                        i_15 += i_13 + i_13;
                    }
                }
            } else {
                while (i_10 < y) {
                    while (i_15 <= i_11 || i_14 <= i_11) {
                        i_14 += deltaX + deltaX;
                        i_15 += deltaX++ + deltaX;
                    }

                    i_17 = x - deltaX + 1;
                    if (i_17 < this.clipLeft) {
                        i_17 = this.clipLeft;
                    }

                    i_18 = deltaX + x;
                    if (i_18 > this.clipRight) {
                        i_18 = this.clipRight;
                    }

                    i_19 = i_17 + i_10 * this.viewportWidth;

                    for (i_20 = i_17; i_20 < i_18; i_20++) {
                        if (!writeDepth || z < this.depthBuffer[i_19]) {
                            this.colourBuffer[i_19] = color;
                        }

                        ++i_19;
                    }

                    ++i_10;
                    i_14 -= i_13-- + i_13;
                    i_15 -= i_13 + i_13;
                }

                deltaX = deltaY;
                i_13 = i_10 - y;
                i_15 = i_11 + i_13 * i_13;
                i_14 = i_15 - deltaY;

                for (i_15 -= i_13; i_10 < lowestY; i_14 += i_13++ + i_13) {
                    while (i_15 > i_11 && i_14 > i_11) {
                        i_15 -= deltaX-- + deltaX;
                        i_14 -= deltaX + deltaX;
                    }

                    i_17 = x - deltaX;
                    if (i_17 < this.clipLeft) {
                        i_17 = this.clipLeft;
                    }

                    i_18 = deltaX + x;
                    if (i_18 > this.clipRight - 1) {
                        i_18 = this.clipRight - 1;
                    }

                    i_19 = i_17 + i_10 * this.viewportWidth;

                    for (i_20 = i_17; i_20 <= i_18; i_20++) {
                        if (!writeDepth || z < this.depthBuffer[i_19]) {
                            this.colourBuffer[i_19] = color;
                        }

                        ++i_19;
                    }

                    ++i_10;
                    i_15 += i_13 + i_13;
                }

            }
        }

    }

    @Override
    public void setSunPosition(Matrix43 sunPosition) {
        this.currentSunPosition = sunPosition;
        method14374();
    }

    @Override
    public Matrix43 copySunPosition() {
        return new Matrix43(this.currentSunPosition);
    }

    @Override
    public Matrix44 copyModelMatrix() {
        return new Matrix44(this.modelMatrix);
    }

    @Override
    public void applyLighting(int i_1, float f_2, float f_3, float f_4, float f_5, float f_6) {
        this.lowestContrast = (int) (65535.0F * f_2);
        this.highestContrast = (int) (65535.0F * f_3);
        float f_7 = (float) Math.sqrt(f_6 * f_6 + f_5 * f_5 + f_4 * f_4);
        this.lightX = (int) (f_4 * 65535.0F / f_7);
        this.lightY = (int) (65535.0F * f_5 / f_7);
        this.lightZ = (int) (f_6 * 65535.0F / f_7);
    }

    @Override
    public void generateFlags() {
    }

    @Override
    public void calculateFog(int baseColor, int count, int dummy) {
        for (int index = 0; index < this.renderingThreads.length; index++) {
            ThreadResourceSM resource = this.renderingThreads[index];
            resource.fogColor = baseColor & 0xffffff;
            int r = resource.fogColor >>> 16 & 0xff;
            if (r < 2) {
                r = 2;
            }
            int g = resource.fogColor >> 8 & 0xff;
            if (g < 2) {
                g = 2;
            }
            int b = resource.fogColor & 0xff;
            if (b < 2) {
                b = 2;
            }
            resource.fogColor = r << 16 | g << 8 | b;
            resource.aBool2304 = count >= 0;
        }
    }

    @Override
    public void resetTextureState() {
        this.forceSmallTexture = false;
        this.animatedModelMaterials.reset();
    }

    public void method14374() {
        this.aClass3848994.fromMatrix(this.currentSunPosition);
        this.floorMatrix.copyFrom(this.aClass3848994);
        this.floorMatrix.multiplyBy(this.modelMatrix);
        this.floorMatrix.method6539(this.aFloatArrayArray8989[0]);
        this.floorMatrix.method6540(this.aFloatArrayArray8989[1]);
        this.floorMatrix.method6535(this.aFloatArrayArray8989[2]);
        this.floorMatrix.method6536(this.aFloatArrayArray8989[3]);
        this.floorMatrix.method6613(this.aFloatArrayArray8989[4]);
        this.floorMatrix.method6538(this.aFloatArrayArray8989[5]);
        float f_1 = this.modelMatrix.getDepthPerspectiveCD();
        float f_2 = this.modelMatrix.faceList[10] * (f_1 - 255.0F) + this.modelMatrix.faceList[14];
        float f_3 = this.modelMatrix.faceList[10] * f_1 + this.modelMatrix.faceList[14];
        float f_4 = f_3 - f_2;

        for (int i_5 = 0; i_5 < this.executingThreadCount; i_5++) {
            ThreadResourceSM class1856 = this.renderingThreads[i_5];
            class1856.aFloat2305 = f_2;
            class1856.aFloat2303 = f_4;
        }

    }

    @Override
    public AbstractCubeTextureGenerator createCubeTexture(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6) {
        return null;
    }

    @Override
    public void resetBloom() {
    }

    @Override
    public void method8525(int i_3, int i_4) {
    }


    @Override
    public void benchmark() {
        boolean hasColor = this.colourBuffer != null;
        boolean hasDepth = this.depthBuffer != null;
        if (hasColor || hasDepth) {
            ThreadResourceSM dependencies = getThreadResource(Thread.currentThread());
            RasterizerSM rasterizer = dependencies.rasterizer;
            rasterizer.smoothEdges = false;
            rasterizer.clipEdges = rasterizer.clipWidth < 5 || rasterizer.clipWidth < 75;
            rasterizer.blendingAlpha = 0;
            rasterizer.blending = false;
            rasterizer.drawTriangle(hasColor, hasDepth, false, 10, 50, 90, 5, 75, 15, (float) 100.0, (float) 100.0, (float) 100.0, -65536, -65536, -65536);
            rasterizer.smoothEdges = true;
        }
    }

    @Override
    public void initialize() {
        ModelSM.maxModelVertices = 10000;
        ModelSM.maxModelTriangles = 10000;
        if (this.executingThreadCount > 1) {
            throw new IllegalStateException();
        } else {
            initializeThreads(this.executingThreadCount);
            mainThreadInitialize();
        }
    }

    @Override
    public RenderBuffer getRenderBuffer(int width, int height) {
        return new SafeModeRenderBuffer(width, height);
    }

    @Override
    public void computeWorldToScreen(float f_1, float f_2, float f_3, float[] floats_4) {
        float f_5 = this.floorMatrix.faceList[10] * f_3 + f_1 * this.floorMatrix.faceList[2] + this.floorMatrix.faceList[14] + f_2 * this.floorMatrix.faceList[6];
        float f_6 = this.floorMatrix.faceList[3] * f_1 + this.floorMatrix.faceList[15] + this.floorMatrix.faceList[7] * f_2 + f_3 * this.floorMatrix.faceList[11];
        if (f_5 >= -f_6 && f_5 <= f_6) {
            float f_7 = f_2 * this.floorMatrix.faceList[4] + this.floorMatrix.faceList[12] + f_1 * this.floorMatrix.faceList[0] + this.floorMatrix.faceList[8] * f_3;
            if (f_7 >= -f_6 && f_7 <= f_6) {
                float f_8 = f_3 * this.floorMatrix.faceList[9] + this.floorMatrix.faceList[13] + this.floorMatrix.faceList[1] * f_1 + this.floorMatrix.faceList[5] * f_2;
                if (f_8 >= -f_6 && f_8 <= f_6) {
                    float f_9 = this.aClass3848994.faceList[14] + this.aClass3848994.faceList[2] * f_1 + this.aClass3848994.faceList[6] * f_2 + this.aClass3848994.faceList[10] * f_3;
                    floats_4[0] = this.aFloat9003 + f_7 * this.aFloat8976 / f_6;
                    floats_4[1] = f_8 * this.aFloat9004 / f_6 + this.aFloat8982;
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
    public void clearRectangle(ComponentSurface class4552, int i_3, int i_4) {
        int i_41 = i_4;
        if (this.colourBuffer != null) {
            ComponentDimensionSM packetsdecoder5 = (ComponentDimensionSM) class4552;
            int[] ints_6 = packetsdecoder5.anIntArray9077;
            int[] ints_7 = packetsdecoder5.anIntArray9078;
            int i_8;
            if (this.clipBottom < ints_6.length + i_41) {
                i_8 = this.clipBottom - i_41;
            } else {
                i_8 = ints_6.length;
            }

            int i_9;
            if (this.clipTop > i_41) {
                i_9 = this.clipTop - i_41;
                i_41 = this.clipTop;
            } else {
                i_9 = 0;
            }

            if (i_8 - i_9 > 0) {
                int i_10 = i_41 * this.viewportWidth;

                for (int i_11 = i_9; i_11 < i_8; i_11++) {
                    int i_12 = i_3 + ints_6[i_11];
                    int i_13 = ints_7[i_11];
                    if (this.clipLeft > i_12) {
                        i_13 -= this.clipLeft - i_12;
                        i_12 = this.clipLeft;
                    }

                    if (this.clipRight < i_13 + i_12) {
                        i_13 = this.clipRight - i_12;
                    }

                    i_12 += i_10;

                    for (int i_14 = -i_13; i_14 < 0; i_14++) {
                        this.colourBuffer[i_12++] = ~0xffffff;
                    }

                    i_10 += this.viewportWidth;
                }
            }
        }

    }

    public boolean isSmallTexture(int materialId) {
        return this.forceSmallTexture || super.materialProvider.getMaterial(materialId).isSmall;
    }


    @Override
    public void setSunCubeTexture(AbstractCubeTextureGenerator class1521) {
    }


    @Override
    public DirectDisplay getImageProducer() {
        return new ImageProducerSM1(this);
    }


    @Override
	public
    void render() {
        super.delegatingDisplay.draw(0, 0);
        if (super.materialProvider != null) {
            super.materialProvider.cycle();
        }
    }


    @Override
    public void addWater(WaterTile waterTile) {
        for (int threadIndex = 0; threadIndex < this.renderingThreads.length; threadIndex++) {
            this.renderingThreads[threadIndex].lastWaterColor = this.renderingThreads[threadIndex].fogColor;
            this.renderingThreads[threadIndex].waterAverageHeight = -1;
            this.renderingThreads[threadIndex].fogColor = waterTile.color;
            this.renderingThreads[threadIndex].waterScale = waterTile.scale;
            this.renderingThreads[threadIndex].hasWaterUpdate = true;
        }
    }


    @Override
    public void setModelMatrix(Matrix44 modelMatrix) {
        this.modelMatrix.copyFrom(modelMatrix);
        method14374();
    }

    @Override
    public NativeSprite generateSprite(SpriteType spriteType, boolean paletteSprite) {
        int[] spritePalette = spriteType.pallete;
        byte[] spritePixels = spriteType.pixels;
        int spriteWidth = spriteType.width;
        int spriteHeight = spriteType.height;
        SpriteSM safeModeSprite;
        if (paletteSprite && spriteType.alpha == null) {
            int[] palette = new int[spritePalette.length];
            byte[] pixels = new byte[spriteWidth * spriteHeight];
            for (int h = 0; h < spriteHeight; h++) {
                int currentPos = h * spriteWidth;
                if (spriteWidth >= 0)
                    System.arraycopy(spritePixels, 0 + currentPos, pixels, 0 + currentPos, spriteWidth);
            }
            System.arraycopy(spritePalette, 0, palette, 0, spritePalette.length);
            safeModeSprite = new PaletteSpriteSM(this, pixels, palette, spriteWidth, spriteHeight);
        } else {
            int[] spriteTransparentPixels = new int[spriteHeight * spriteWidth];
            byte[] spriteAlpha = spriteType.alpha;
            if (spriteAlpha != null) {
                for (int h = 0; h < spriteHeight; h++) {
                    int currentPos = spriteWidth * h;
                    for (int w = 0; w < spriteWidth; w++) {
                        spriteTransparentPixels[w + currentPos] = spritePalette[spritePixels[w + currentPos] & 0xff] | spriteAlpha[w + currentPos] << 24;
                    }
                }
                safeModeSprite = new TransparentSpriteSM(this, spriteTransparentPixels, spriteWidth, spriteHeight);
            } else {
                for (int h = 0; h < spriteHeight; h++) {
                    int currentPos = h * spriteWidth;
                    for (int w = 0; w < spriteWidth; w++) {
                        int i_13 = spritePalette[spritePixels[w + currentPos] & 0xff];
                        spriteTransparentPixels[currentPos + w] = i_13 != 0 ? ~0xffffff | i_13 : 0;
                    }
                }
                safeModeSprite = new OpaqueSpriteSM(this, spriteTransparentPixels, spriteWidth, spriteHeight);
            }
        }
        safeModeSprite.setSize(spriteType.minX, spriteType.minY, spriteType.borderX, spriteType.borderY);
        return safeModeSprite;
    }


    @Override
    public void drawThickLine(int startX, int startY, int x, int y, int color, int thickness, int blendType) {
        if (this.colourBuffer != null) {
            ThreadResourceSM dependencies = getThreadResource(Thread.currentThread());
            RasterizerSM rasterizer = dependencies.rasterizer;
            int width = x - startX;
            int height = y - startY;
            int i_12 = width >= 0 ? width : -width;
            int i_13 = height >= 0 ? height : -height;
            int dimension = i_12;
            if (i_12 < i_13) {
                dimension = i_13;
            }
            if (dimension != 0) {
                int lineX = (width << 16) / dimension;
                int lineY = (height << 16) / dimension;
                width += lineX >> 16;
                height += lineY >> 16;
                if (lineY <= lineX) {
                    lineX = -lineX;
                } else {
                    lineY = -lineY;
                }
                int lineStartY = lineY * thickness >> 17;
                int lineEndY = lineY * thickness + 1 >> 17;
                int lineStartX = lineX * thickness >> 17;
                int lineEndX = lineX * thickness + 1 >> 17;
                startX -= rasterizer.getCurrentColumn();
                startY -= rasterizer.getCurrentRow();
                int xA = lineStartY + startX;
                int xB = startX - lineEndY;
                int xC = width + startX - lineEndY;
                int xC2 = width + lineStartY + startX;
                int yA = lineStartX + startY;
                int yB = startY - lineEndX;
                int yC = height + startY - lineEndX;
                int yC2 = height + lineStartX + startY;
                if (blendType == 0) {
                    rasterizer.blendingAlpha = 0;
                } else {
                    if (blendType != 1) {
                        throw new IllegalArgumentException();
                    }
                    rasterizer.blendingAlpha = 255 - (color >>> 24);
                }
                setDepthBuffering(false);
                rasterizer.clipEdges = xA < 0 || xA > rasterizer.clipWidth || xB < 0 || xB > rasterizer.clipWidth || xC < 0 || xC > rasterizer.clipWidth;
                rasterizer.drawFlatTriangle(true, false, false, yA, yB, yC, xA, xB, xC, 100.0F, 100.0F, 100.0F, color);
                rasterizer.clipEdges = xA < 0 || xA > rasterizer.clipWidth || xC < 0 || xC > rasterizer.clipWidth || xC2 < 0 || xC2 > rasterizer.clipWidth;
                rasterizer.drawFlatTriangle(true, false, false, yA, yC, yC2, xA, xC, xC2, 100.0F, 100.0F, 100.0F, color);
                setDepthBuffering(true);
            }
        }

    }


    @Override
    public void debugModeChanged() {
    }

    @Override
    public NativeMemoryNode getNativeMemory(int i_1) {
        return null;
    }


    @Override
    public int getMaxLights() {
        return 0;
    }

    public void method14399(int x, int y, int i_3, int color, int i_6, int i_7, int i_8) {
        if (this.colourBuffer != null && x >= this.clipLeft && x < this.clipRight) {
            int currentPos = y * this.viewportWidth + x;
            int alpha = color >>> 24;
            int i_11 = i_7 + i_6;
            int i_12 = i_8 % i_11;
            int i_13;
            if (alpha == 255) {
                for (i_13 = 0; i_13 < i_3; i_12 %= i_11) {
                    if (i_13 + y >= this.clipTop && i_13 + y < this.clipBottom && i_12 < i_6) {
                        this.colourBuffer[currentPos + i_13 * this.viewportWidth] = color;
                    }

                    ++i_13;
                    ++i_12;
                }
            } else {
                int i_14;
                int i_15;
                int i_16;
                color = (alpha * (color & 0xff00ff) >> 8 & 0xff00ff) + (alpha * (color & 0xff00) >> 8 & 0xff00) + (alpha << 24);
                i_13 = 256 - alpha;
                for (i_14 = 0; i_14 < i_3; i_12 %= i_11) {
                    if (i_14 + y >= this.clipTop && i_14 + y < this.clipBottom && i_12 < i_6) {
                        i_15 = currentPos + i_14 * this.viewportWidth;
                        i_16 = this.colourBuffer[i_15];
                        i_16 = (i_13 * (i_16 & 0xff00) >> 8 & 0xff00) + ((i_16 & 0xff00ff) * i_13 >> 8 & 0xff00ff);
                        this.colourBuffer[i_15] = i_16 + color;
                    }
                    ++i_14;
                    ++i_12;
                }
            }
        }
    }


    @Override
    public void applyClearFlags(int flags, int backgroundColor) {
        if ((flags & 0x1) != 0) {
            drawFilledRectangle(0, 0, this.viewportWidth, this.viewportHeight, backgroundColor, 0);
        }
        if ((flags & 0x2) != 0) {
            clearDepth();
        }
    }


    @Override
    public void drawTransparentVerticalLine(int x, int y, int height, int color, int alpha) {
        if (this.colourBuffer != null && x >= this.clipLeft && x < this.clipRight) {
            if (y < this.clipTop) {
                height -= this.clipTop - y;
                y = this.clipTop;
            }

            if (height + y > this.clipBottom) {
                height = this.clipBottom - y;
            }

            int i_6 = y * this.viewportWidth + x;
            int colorAlpha = color >>> 24;
            if (alpha == 0 || alpha == 1 && colorAlpha == 255) {
                for (int h = 0; h < height; h++) {
                    this.colourBuffer[i_6 + h * this.viewportWidth] = color;
                }
            } else {
                if (alpha == 1) {
                    color = (colorAlpha * (color & 0xff00ff) >> 8 & 0xff00ff) + (colorAlpha << 24) + (colorAlpha * (color & 0xff00) >> 8 & 0xff00);
                    int i_8 = 256 - colorAlpha;
                    for (int i_9 = 0; i_9 < height; i_9++) {
                    	int i_10 = i_6 + i_9 * this.viewportWidth;
                    	int i_11 = this.colourBuffer[i_10];
                    	i_11 = ((i_11 & 0xff00ff) * i_8 >> 8 & 0xff00ff) + (i_8 * (i_11 & 0xff00) >> 8 & 0xff00);
                        this.colourBuffer[i_10] = i_11 + color;
                    }
                } else {
                    if (alpha != 2) {
                        throw new IllegalArgumentException();
                    }
                    for (int i_8 = 0; i_8 < height; i_8++) {
                    	int colorIndex = i_6 + i_8 * this.viewportWidth;
                    	int originalColor = this.colourBuffer[colorIndex];
                    	int colorSum = originalColor + color;
                        int blendColor = (originalColor & 0xff00ff) + (color & 0xff00ff);
                        originalColor = (blendColor & 0x1000100) + (colorSum - blendColor & 0x10000);
                        this.colourBuffer[colorIndex] = colorSum - originalColor | originalColor - (originalColor >>> 8);
                    }
                }
            }
        }

    }

    public boolean hasRepeat(int materialId) {
        return super.materialProvider.getMaterial(materialId).repeatS || super.materialProvider.getMaterial(materialId).repeatT;
    }

    @Override
    public void drawRectangleOutline(int x, int y, int width, int height, int color, int alpha) {
        drawTransparentHorizontalLine(x, y, width, color, alpha);
        drawTransparentHorizontalLine(x, y + height - 1, width, color, alpha);
        drawTransparentVerticalLine(x, y + 1, height - 2, color, alpha);
        drawTransparentVerticalLine(width + x - 1, y + 1, height - 2, color, alpha);
    }

    @Override
    public void method8474() {
    }

    @Override
    public void updateSun(float multiplier) {
        this.lightIntensity = (int) (multiplier * 65535.0F);
    }


    @Override
    public void setClipping(int left, int top, int right, int bottom) {
        if (left < 0) {
            left = 0;
        }
        if (top < 0) {
            top = 0;
        }
        if (right > this.viewportWidth) {
            right = this.viewportWidth;
        }
        if (bottom > this.viewportHeight) {
            bottom = this.viewportHeight;
        }
        this.clipLeft = left;
        this.clipRight = right;
        this.clipTop = top;
        this.clipBottom = bottom;
        viewportUpdate();
    }

    @Override
    public AbstractCubeTextureGenerator drawQuad(AbstractCubeTextureGenerator class1521, AbstractCubeTextureGenerator class1522, float f_3, AbstractCubeTextureGenerator class1524) {
        return null;
    }


    public boolean hasBeenShutdown() {
        return this.shutdown;
    }


    @Override
    public void drawLine(int x, int y, int width, int length, int color, int i_7, int i_8, int i_9) {
        if (this.colourBuffer != null) {
            width -= x;
            length -= y;
            int i_10;
            if (length == 0) {
                if (width >= 0) {
                    method14366(x, y, width + 1, color, i_7, i_8, i_9);
                } else {
                    i_10 = i_8 + i_7;
                    i_9 %= i_10;
                    i_9 = i_10 + i_7 - i_9 - (-width + 1) % i_10;
                    i_9 %= i_10;
                    if (i_9 < 0) {
                        i_9 += i_10;
                    }

                    method14366(width + x, y, -width + 1, color, i_7, i_8, i_9);
                }
            } else if (width == 0) {
                if (length >= 0) {
                    method14399(x, y, length + 1, color, i_7, i_8, i_9);
                } else {
                    i_10 = i_8 + i_7;
                    i_9 %= i_10;
                    i_9 = i_10 + i_7 - i_9 - (-length + 1) % i_10;
                    i_9 %= i_10;
                    if (i_9 < 0) {
                        i_9 += i_10;
                    }

                    method14399(x, y + length, -length + 1, color, i_7, i_8, i_9);
                }
            } else {
                i_9 <<= 8;
                i_7 <<= 8;
                i_8 <<= 8;
                i_10 = i_8 + i_7;
                i_9 %= i_10;
                int i_11;
                int i_12;
                if (width + length < 0) {
                    i_11 = (int) (Math.sqrt(width * width + length * length) * 256.0D);
                    i_12 = i_11 % i_10;
                    i_9 = i_10 + i_7 - i_9 - i_12;
                    i_9 %= i_10;
                    if (i_9 < 0) {
                        i_9 += i_10;
                    }

                    x += width;
                    width = -width;
                    y += length;
                    length = -length;
                }

                int i_13;
                int i_14;
                int i_15;
                int i_16;
                int i_17;
                int i_18;
                if (width > length) {
                    y <<= 16;
                    y += 32768;
                    length <<= 16;
                    i_11 = (int) Math.floor((double) length / width + 0.5D);
                    width += x;
                    i_12 = color >>> 24;
                    i_13 = (int) Math.sqrt((i_11 >> 8) * (i_11 >> 8) + 65536);
                    if (i_12 == 255) {
                        while (x <= width) {
                            i_14 = y >> 16;
                            if (x >= this.clipLeft && x < this.clipRight && i_14 >= this.clipTop && i_14 < this.clipBottom && i_9 < i_7) {
                                this.colourBuffer[i_14 * this.viewportWidth + x] = color;
                            }

                            y += i_11;
                            ++x;
                            i_9 += i_13;
                            i_9 %= i_10;
                        }
                    } else {
                        color = (i_12 * (color & 0xff00ff) >> 8 & 0xff00ff) + (i_12 * (color & 0xff00) >> 8 & 0xff00) + (i_12 << 24);

                        for (i_14 = 256 - i_12; x <= width; i_9 %= i_10) {
                            i_15 = y >> 16;
                            if (x >= this.clipLeft && x < this.clipRight && i_15 >= this.clipTop && i_15 < this.clipBottom && i_9 < i_7) {
                                i_16 = i_15 * this.viewportWidth + x;
                                i_17 = this.colourBuffer[i_16];
                                i_17 = ((i_17 & 0xff00ff) * i_14 >> 8 & 0xff00ff) + (i_14 * (i_17 & 0xff00) >> 8 & 0xff00);
                                this.colourBuffer[i_16] = color + i_17;
                            }

                            y += i_11;
                            ++x;
                            i_9 += i_13;
                        }
                    }
                } else {
                    x <<= 16;
                    x += 32768;
                    width <<= 16;
                    i_11 = (int) Math.floor((double) width / length + 0.5D);
                    length += y;
                    i_12 = color >>> 24;
                    i_13 = (int) Math.sqrt((i_11 >> 8) * (i_11 >> 8) + 65536);
                    if ((i_12 != 255)) {
                        color = (i_12 * (color & 0xff00ff) >> 8 & 0xff00ff) + (i_12 * (color & 0xff00) >> 8 & 0xff00) + (i_12 << 24);

                        for (i_14 = 256 - i_12; y <= length; i_9 %= i_10) {
                            i_15 = x >> 16;
                            if (y >= this.clipTop && y < this.clipBottom && i_15 >= this.clipLeft && i_15 < this.clipRight && i_9 < i_7) {
                                i_16 = y * this.viewportWidth + i_15;
                                i_17 = this.colourBuffer[i_16];
                                i_17 = (i_14 * (i_17 & 0xff00) >> 8 & 0xff00) + ((i_17 & 0xff00ff) * i_14 >> 8 & 0xff00ff);
                                this.colourBuffer[y * this.viewportWidth + i_15] = color + i_17;
                            }

                            x += i_11;
                            ++y;
                            i_9 += i_13;
                        }
                    } else {
                        while (y <= length) {
                            i_14 = x >> 16;
                            if (y >= this.clipTop && y < this.clipBottom && i_14 >= this.clipLeft && i_14 < this.clipRight && i_9 < i_7) {
                                this.colourBuffer[y * this.viewportWidth + i_14] = color;
                            }

                            x += i_11;
                            ++y;
                            i_9 += i_13;
                            i_9 %= i_10;
                        }
                    }
                }
            }
        }

    }



    @Override
    public PointLight createPointLight(int i_1, int i_2, int i_3, int i_4, int i_5) {
        return null;
    }


    @Override
    public boolean bloomSupported() {
        return false;
    }


    @Override
    public int mismatchingFunctionMask(int toFlag, int xor) {
        toFlag |= 0x20800;
        return toFlag & xor ^ xor;
    }


    @Override
    public int getOffheap() {
        return 0;
    }


    public void mainThreadInitialize() {
        this.renderingThreads[0].setRunnable(Thread.currentThread());
    }


}
