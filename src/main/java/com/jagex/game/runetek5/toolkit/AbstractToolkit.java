package com.jagex.game.runetek5.toolkit;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;

import com.jagex.game.runetek5.buffer.NativeMemoryNode;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.model.Mesh;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator;
import com.jagex.game.runetek5.imageproducer.AbstractDisplay;
import com.jagex.game.runetek5.imageproducer.DelegatingDisplay;
import com.jagex.game.runetek5.imageproducer.DirectDisplay;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.js5.library.INativeLibraryLoader;
import com.jagex.game.runetek5.math.Matrix43;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.rendering.RenderInfo;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.FrameRenderException;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.ColorUtil;

public abstract class AbstractToolkit implements AutoCloseable {

    public static final boolean[] ACTIVE_RENDER_UNITS = new boolean[8];
    public final MaterialProvider materialProvider;
    public int renderId;
    public AbstractDisplay display;
    protected DelegatingDisplay delegatingDisplay;
    public final Hashtable<Canvas, DelegatingDisplay> canvasToProducerMap = new Hashtable<>();
    public int directDisplayCursorPosition = -1;
    public final DirectDisplay[] directDisplays = new DirectDisplay[4];
	public static int activeRenderMode = -1;
    public static INativeLibraryLoader nativeLibraryLoader;

    /**
     * 
     * @param renderMode Identifier for the display mode we're using
     * @param canvasToDrawOn EG: game canvas / inventory item canvas
     * @param materialProvider The wrapper around material management
     * @param shaderResource The resource containing all shaders
     * @param multiSamples Multi samples preference
     */
    public static AbstractToolkit create(int renderMode, Canvas canvasToDrawOn, MaterialProvider materialProvider, Js5ResourceProvider shaderResource, int multiSamples) {
        int screenWidth = 0;
        int screenHeight = 0;
        if (canvasToDrawOn != null) {
            Dimension dimension = canvasToDrawOn.getSize();
            screenWidth = dimension.width;
            screenHeight = dimension.height;
        }
        return toolkitForType(renderMode, canvasToDrawOn, materialProvider, shaderResource, multiSamples, screenWidth, screenHeight);
    }
    
    /**
     * Currently software mode does not work due to renamed native methods & potentially other issues
     */
    public static synchronized AbstractToolkit toolkitForType(int renderMode, Canvas gameCanvas, MaterialProvider materialProvider, Js5ResourceProvider shaderIndex, int multiSamples, int screenWidth, int screenHeight) {
    	String mode = (renderMode == 1 || renderMode == 5) ? "GL" : (renderMode == 0 ? "SM" : "DX");
    	System.out.println("[AbstractToolkit] Entering render mode: "+ renderMode +" = "+mode);
    	if (renderMode == 0) {
            return SafeModeToolkit.createToolkit(gameCanvas, materialProvider, screenWidth, screenHeight);
        } else if (renderMode == 2) {
            return SoftwareToolkit.createToolkit(gameCanvas, materialProvider, screenWidth, screenHeight);
        } else if (renderMode == 1) {
            return OpenGlToolkit.createToolkitLd(gameCanvas, materialProvider, multiSamples);
        } else if (renderMode == 5) {
            return OpenGlToolkit.createToolkitHd(gameCanvas, materialProvider, shaderIndex, multiSamples);
        } else if (renderMode == 3) {
            return AbstractNativeToolkit.createToolkit(gameCanvas, materialProvider, shaderIndex, multiSamples);
        } else {
            throw new IllegalArgumentException("");
        }
    }
    
    public static boolean isJaclibLoaded() {
        return AbstractToolkit.getNativeLibraryLoader().loadIntoMemory("jaclib");
    }
    
    public static void setNativeLibraryLoader(INativeLibraryLoader nativeLibraryLoader) {
        if (AbstractToolkit.nativeLibraryLoader != null) {
            throw new IllegalStateException("");
        }
        AbstractToolkit.nativeLibraryLoader = nativeLibraryLoader;
    }
    
    public static INativeLibraryLoader getNativeLibraryLoader() {
        if (AbstractToolkit.nativeLibraryLoader == null) {
            throw new IllegalStateException("");
        } 
        return AbstractToolkit.nativeLibraryLoader;
    }
    
    protected AbstractToolkit(MaterialProvider materialProvider) {
        this.materialProvider = materialProvider;
        int renderId = -1;
        for (int index = 0; index < 8; index++) {
            if (!ACTIVE_RENDER_UNITS[index]) {
                ACTIVE_RENDER_UNITS[index] = true;
                renderId = index;
                break;
            }
        }
        if (renderId == -1) {
            throw new IllegalStateException("");
        }
        this.renderId = renderId;
    }

    public static void drawOutlinedFilledOpaqueRectangle(AbstractToolkit toolkit, int x, int y, int width, int height, int colorArgb) {
        toolkit.clearPixels(x, y, width, height, colorArgb);
        toolkit.clearPixels(x + 1, y + 1, width - 2, 16, ~0xffffff);
        toolkit.drawOpaqueRectangle(x + 1, y + 18, width - 2, height - 19, ~0xffffff);
    }

    public abstract RenderInfo getRenderInfo();

    public void renderFrame() throws FrameRenderException {
        render();
    }

    public abstract void awaitRenderCompletion();

    public void cleanupAndShutdown() {
        ACTIVE_RENDER_UNITS[this.renderId] = false;
        Enumeration<Canvas> canvasProducerMapping = this.canvasToProducerMap.keys();
        while (canvasProducerMapping.hasMoreElements()) {
            Canvas canvas = canvasProducerMapping.nextElement();
            DelegatingDisplay producer = this.canvasToProducerMap.get(canvas);
            producer.cleanup();
        }
        shutdown();
    }

    public abstract void shutdown();

    public abstract void animateTextures(int delta);

    public abstract int getOffheap();

    @Override
    public void close() {
        cleanupAndShutdown();
    }

    public abstract boolean utilisesNativeHeap();

    public abstract boolean hasShadows();

    public abstract boolean hasBloomSupport();

    public abstract boolean method8404();

    public abstract boolean hasAntiAliasing();

    public abstract boolean hasTextures();

    public abstract void drawFilledRectangle(int x, int y, int width, int height, int color, int type);

    public abstract boolean replaceCanvasOnDisplayUpdate();

    public void accumulateViewportDimensions(int[] cacheInto) {
        if (this.delegatingDisplay != null) {
            cacheInto[0] = this.delegatingDisplay.getWidth();
            cacheInto[1] = this.delegatingDisplay.getHeight();
        } else {
            cacheInto[1] = 0;
            cacheInto[0] = 0;
        }
    }

    public void removeCanvas(Canvas canvas) {
        if (this.canvasToProducerMap.containsKey(canvas)) {
            DelegatingDisplay producer = this.canvasToProducerMap.get(canvas);
            producer.cleanup();
            this.canvasToProducerMap.remove(canvas);
        }
    }

    public void addImageProducer(Canvas canvas, DelegatingDisplay producer) {
        if (producer == null) {
            throw new RuntimeException();
        }
        this.canvasToProducerMap.put(canvas, producer);
    }

    public void setActiveCanvas(Canvas canvas) {
        DelegatingDisplay delegating = this.canvasToProducerMap.get(canvas);
        if (delegating == null) {
            throw new RuntimeException();
        }
        if (this.directDisplayCursorPosition > 0 || this.delegatingDisplay != this.display) {
            throw new RuntimeException();
        }
        if (this.display != null) {
            this.display.setDepthStencilSurface();
        }
        if (this.directDisplayCursorPosition < 0) {
            this.display = delegating;
        }
        this.delegatingDisplay = delegating;
        delegating.reload();
    }

    public void resizeGameCanvas(Canvas gameCanvas, int screenCanvasWidth, int screenCanvasHeight) {
        DelegatingDisplay imageProducer = this.canvasToProducerMap.get(gameCanvas);
        if (imageProducer == null) {
            throw new RuntimeException();
        }
        imageProducer.resize(screenCanvasWidth, screenCanvasHeight);
    }

    public void resizeDisplay(DirectDisplay directDisplay) {
        if (this.directDisplayCursorPosition < 0 || directDisplay != this.directDisplays[this.directDisplayCursorPosition]) {
            throw new RuntimeException();
        }
        this.directDisplays[--this.directDisplayCursorPosition + 1] = null;
        directDisplay.setDepthStencilSurface();
        if (this.directDisplayCursorPosition >= 0) {
            this.display = this.directDisplays[this.directDisplayCursorPosition];
            this.directDisplays[this.directDisplayCursorPosition].reload();
        } else {
            this.display = this.delegatingDisplay;
            this.delegatingDisplay.reload();
        }
    }

    public abstract DelegatingDisplay createDelegatingDisplay(Canvas var1, int var2, int var3);

    public abstract DirectDisplay getImageProducer();

    public abstract RenderBuffer getRenderBuffer(int width, int height);

    public abstract int[] copyPixelRectangle(int x, int y, int width, int height);

    public abstract void renderLastFrame();

    public abstract void calculateViewportCenter();

    public abstract void resetDepthClippingMask();

    public abstract void resetClipping();

    public abstract void expandClipping(int screenLeft, int screenTop, int screenRight, int screenBottom);

    public abstract void getClipping(int[] cacheInto);

    public abstract void applyClearFlags(int functionMask, int backgroundColorArgb);

    public abstract void setModelMatrix(Matrix44 var1);

    public void clearPixels(int x, int y, int width, int height, int color) {
        drawFilledRectangle(x, y, width, height, color, 1);
    }

    public void drawOpaqueVerticalLine(int x, int y, int height, int colorArgb) {
        drawTransparentVerticalLine(x, y, height, colorArgb, 1);
    }

    public void drawOpaqueLine(int x, int y, int width, int height, int colorArgb) {
        drawTransparentLine(x, y, width, height, colorArgb, 1);
    }

    public abstract void drawRectangleOutline(int x, int y, int width, int height, int color, int alpha);

    public abstract void drawCircle(int x, int y, int radius, int colorArgb);

    public abstract void drawTransparentHorizontalLine(int x, int y, int width, int colorArgb, int blendMode);

    public abstract void drawTransparentVerticalLine(int x, int y, int height, int colorArgb, int blendMode);

    public abstract void drawTransparentLine(int x, int y, int width, int height, int color, int alpha);

    public abstract void drawLine(int startX, int startY, int endX, int endY, int colorArgb, int lineSpacing, int lineLength, int offset);

    public abstract int isOnScreen(int var1, int var2, int var3, int var4, int var5, int var6);

    public abstract NativeMemoryNode getNativeMemory(int var1);

    public abstract void setNativeHeap(NativeMemoryNode nativeMemoryNode);

    public NativeSprite generateEmptySprite(int width, int height) {
        return generateEmptySprite(width, height, true, false);
    }

    
    public abstract NativeSprite createNativeSprite(int[] pixels, int defaultBackgroundColor, int width, int useWidth, int useHeight, boolean var6);

    public abstract NativeSprite generateSprite(SpriteType spriteType, boolean hasPalette);

    public abstract AbstractFontRenderer createFontRenderer(FontMetrics var1, SpriteType[] var2, boolean var3);

    public abstract Matrix44 method8449();

    public abstract Matrix43 getRenderingMatrix();

    /**
     * Creates a model and applies the function mask to set the correct attributes
     */
    public abstract Model createModel(Mesh mesh, int functionMask, int preferenceMask, int ambient, int contrast);

    /**
     * Checks if function_mask_a has all the functions of function_mask_b if so returns 0 otherwise returns 1
     */
    public abstract int mismatchingFunctionMask(int functionMaskA, int functionMaskB);

    public abstract Matrix44 copyModelMatrix();

    public abstract boolean extendBuildArea();

    public boolean readyForRendering() {
        return true;
    }

    public abstract void renderParticles(ParticleWrapper var1);

    public abstract void setSunPosition(Matrix43 var1);

    public abstract Matrix43 copySunPosition();

    public abstract void setDepthBuffering(boolean var1);

    public abstract void initialize();

    protected DelegatingDisplay getDelegatingDisplay() {
        return this.delegatingDisplay;
    }

    /**
     * Sets the general rendering brightness
     * @param brightness 0.0 - 1.0
     */
    public abstract void updateSun(float brightness);

    public abstract void applyLighting(int color, float intensity, float f_3, float positionX, float positionY, float positionZ);

    public abstract void generateFlags();

    public abstract int getMaxLights();

    public abstract boolean supportsFullscreen();

    public abstract AbstractCubeTextureGenerator createCubeTexture(int materialAtPositiveX, int materialAtNegativeX, int materialAtPositiveY, int materialAtNegativeY, int materialAtPositiveZ, int materialAtNegativeZ);

    public abstract AbstractCubeTextureGenerator drawQuad(AbstractCubeTextureGenerator var1, AbstractCubeTextureGenerator var2, float var3, AbstractCubeTextureGenerator var4);

    public void drawMapCategory(int x, int y, int radius, int colorArgb) {
        drawCircle(x, y, radius, colorArgb);
    }

    public abstract boolean bloomSupported();

    public abstract boolean bloomEnabled();

    public void updateBloomFilter(float whitepoint, float bloom, float brightpass) {
        setBloomFilter(whitepoint, bloom, brightpass);
    }

    public abstract void method8474();

    public abstract void addWater(WaterTile waterTile);

    public abstract void addWaterTile(int waterColor, WaterTile waterTile);

    public abstract void setSunCubeTexture(AbstractCubeTextureGenerator var1);

    public abstract Ground createGround(int regionSizeX, int regionSizeY, int[][] tileheightsCurrentLvl, int[][] tileheightsNextLvl, int floorFunctionMask, int waterFunctionMask);

    public abstract void calculateProjection(float width, float height, float length, float[] matrix);

    public abstract void debugModeChanged();

    public abstract void resetBloom();

    public abstract void drawThickLine(int leftX, int topY, int rightX, int bottomY, int colorArgb, int lineWidth, int blendMode);

    public abstract void setClipping(int left, int top, int right, int bottom);

    public abstract void onTilesDrawn();

    public abstract void computeWorldToScreen(float x, float y, float z, float[] screenCoords);

    public abstract void calculateFog(int color, int density, int var3);

    public AbstractDisplay getDisplay() {
        return this.display;
    }

    public void ensureImageproducerCreated(Canvas gameCanvas, int screenWidth, int screenHeight) {
        if (!this.canvasToProducerMap.containsKey(gameCanvas)) {
            setIgnoreRepaint(gameCanvas);
            addImageProducer(gameCanvas, createDelegatingDisplay(gameCanvas, screenWidth, screenHeight));
        }
    }
    
    public static void setIgnoreRepaint(Canvas gameCanvas) {
        try {
            Class<?> clazz = Class.forName("java.awt.Canvas");
            Method setIgnoreRepaint = clazz.getMethod("setIgnoreRepaint", Boolean.TYPE);
            setIgnoreRepaint.invoke(gameCanvas, Boolean.TRUE);
        } catch (Exception e) {
        	// NOOP
        }
    }

    public abstract void method8525(int screenCanvasWidth, int screenCanvasHeight);

    public abstract void clearRectangle(ComponentSurface dimension, int clipLeft, int clipTop);

    public abstract int mergeFunctionMasks(int functionMaskA, int functionMaskB);

    public abstract void copyLights(int lightCount, PointLight[] lights);

    public NativeSprite createNativeSprite(int[] pixels, int width, int useWidth, int useHeight) {
        return createNativeSprite(pixels, 0, width, useWidth, useHeight, true);
    }

    public abstract void render() throws FrameRenderException;

    public void drawOpaqueRectangle(int x, int y, int width, int height, int color) {
        drawRectangleOutline(x, y, width, height, color, 1);
    }

    public abstract void drawTexturedLine(int var1, int var2, int var3, int var4, int color, ComponentSurface dimension, int clipLeft, int clipTop, int var10, int var11, int var12);

    public abstract void resetTextureState();

    public abstract void setBloomFilter(float whitepoint, float bloom, float brightpass);

    public PointLight createPointLight(int x, int y, int z, int size2d, int color) {
        return new PointLight(x, y, z, size2d, color);
    }
    
    public abstract void setDrawableArea(int x, int y, int width, int height);

    public abstract ComponentSurface createComponentSurface(int sizeX, int sizeY, int[] rowStartX, int[] rowWidth);


    public void setDisplay(DirectDisplay activeDisplay) {
        if (this.directDisplayCursorPosition >= 3) {
            throw new RuntimeException();
        } 
        if (this.directDisplayCursorPosition >= 0) {
            this.directDisplays[this.directDisplayCursorPosition].setDepthStencilSurface();
        } else {
            this.delegatingDisplay.setDepthStencilSurface();
        }
        this.display = this.directDisplays[++this.directDisplayCursorPosition] = activeDisplay;
        activeDisplay.reload();
    }


    public abstract NativeSprite generateEmptySprite(int width, int height, boolean transparent, boolean var4);

    /**
     * Benchmarks the performance of given toolkit in order
     * to determine which display mode is optimal for your pc
     */
    public abstract void benchmark();

    public void drawOpaqueHorizontalLine(int x, int y, int width, int colorArgb) {
        drawTransparentHorizontalLine(x, y, width, colorArgb, 1);
    }


    public abstract boolean forceFixedScreenmode();

    /**
     * Give this a better name (?), currently only used for worldmap tiles
     */
    public abstract void renderWorldmapTile(int var1, int var2, int var3, int var4, int var5, int var6, byte[] var7, int var8, int var9);

    
    
    
    public abstract NativeSprite createNativeSprite(int var1, int var2, int var3, int var4, boolean var5);
    
    

    public abstract void method8669(int var1, int var2, int var3, int var4, int var5, ComponentSurface var7, int clipLeft, int clipTop);

	public static void calculateGameArea(int x, int y, int width, int height, boolean resetPixels) {
	    int w = width;
	    int h = height;
	    if (w < 1) {
	        w = 1;
	    }
	    if (h < 1) {
	        h = 1;
	    }
	    int i_6 = h - 334;
	    if (i_6 < 0) {
	        i_6 = 0;
	    } else if (i_6 > 100) {
	        i_6 = 100;
	    }
	    int i_7 = (client.aShort7437 - client.aShort7436) * i_6 / 100 + client.aShort7436;
	    int i_8 = h * i_7 * 512 / (w * 334);
	    int i_9;
	    int i_10;
	    short s_11;
	    if (i_8 < client.aShort7214) {
	        s_11 = client.aShort7214;
	        i_7 = s_11 * w * 334 / (h * 512);
	        if (i_7 > client.aShort7441) {
	            i_7 = client.aShort7441;
	            i_9 = h * i_7 * 512 / (s_11 * 334);
	            i_10 = (w - i_9) / 2;
	            if (resetPixels) {
	                Renderers.activeToolkit.resetClipping();
	                Renderers.activeToolkit.clearPixels(x, y, i_10, h, ~0xffffff);
	                Renderers.activeToolkit.clearPixels(x + w - i_10, y, i_10, h, ~0xffffff);
	            }
	            x += i_10;
	            w -= i_10 * 2;
	        }
	    } else if (i_8 > client.aShort7474) {
	        s_11 = client.aShort7474;
	        i_7 = s_11 * w * 334 / (h * 512);
	        if (i_7 < client.aShort7276) {
	            i_7 = client.aShort7276;
	            i_9 = s_11 * w * 334 / (i_7 * 512);
	            i_10 = (h - i_9) / 2;
	            if (resetPixels) {
	                Renderers.activeToolkit.resetClipping();
	                Renderers.activeToolkit.clearPixels(x, y, w, i_10, ~0xffffff);
	                Renderers.activeToolkit.clearPixels(x, h + y - i_10, w, i_10, ~0xffffff);
	            }
	            y += i_10;
	            h -= i_10 * 2;
	        }
	    }
	    client.anInt7451 = h * i_7 / 334;
	    client.hintArrowOffsetX = x;
	    client.hintArrowOffsetY = y;
	    client.gameAreaWidth = (short) w;
	    client.gameAreaHeight = (short) h;
	}

	public static void drawFadingFilledRectangle(AbstractToolkit toolkit, int baseLight, int baseSaturation, Component component, int hue) {
	    for (int saturation = 7; saturation >= 0; --saturation) {
	        for (int lightness = 127; lightness >= 0; --lightness) {
	            int hsl = (hue & 0x3f) << 10 | (saturation & 0x7) << 7 | lightness & 0x7f;
	            ColorUtil.initializeTables(false, true);
	            int rgb = ColorUtil.hslToRgb[hsl];
	            ColorUtil.resetTables(false, true);
	            toolkit.drawFilledRectangle((lightness * component.width >> 7) + baseLight, baseSaturation + ((7 - saturation) * component.height >> 3), (component.width >> 7) + 1, (component.height >> 3) + 1, rgb, 0);
	        }
	    }
	}

	public static boolean nativeLibraryLoaded() {
	    return nativeLibraryLoader != null;
	}
}