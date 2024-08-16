package com.jagex.game.runetek5.graphics.sprite;

import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.PixelGrabber;

import com.jagex.game.runetek5.cache.compression.GzipDecompressor;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpIrregularBricks;
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public abstract class NativeSprite implements Sprite {

	public static int anInt3556;
	public static int anInt4350;
	public static int anInt156;
	public static final byte[] GZIPPED_SPRITE_DATA = {(byte) 31, (byte) -117, (byte) 8, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) -5, (byte) 127, (byte) -29, (byte) -1, (byte) 109, (byte) 6, (byte) 103, (byte) 6, (byte) 14, (byte) 54, (byte) 54, (byte) 118, (byte) 54, (byte) 86, (byte) 14, (byte) 118, (byte) 118, (byte) 118, (byte) 78, (byte) 78, (byte) 14, (byte) 46, (byte) 30, (byte) 17, (byte) 94, (byte) 30, (byte) 110, (byte) 110, (byte) 30, (byte) 73, (byte) 33, (byte) 97, (byte) 126, (byte) 17, (byte) 89, (byte) 41, (byte) 121, (byte) 57, (byte) 89, (byte) 41, (byte) 25, (byte) 25, (byte) 5, (byte) 21, (byte) 61, (byte) 117, (byte) 5, (byte) 37, (byte) 29, (byte) 101, (byte) 25, (byte) 25, (byte) 13, (byte) 115, (byte) 77, (byte) 29, (byte) 3, (byte) 67, (byte) 19, (byte) 19, (byte) 19, (byte) 121, (byte) 117, (byte) 75, (byte) 91, (byte) 11, (byte) 35, (byte) 27, (byte) 61, (byte) 99, (byte) 19, (byte) 35, (byte) -112, (byte) 33, (byte) -116, (byte) -100, (byte) -100, (byte) -100, (byte) 60, (byte) -36, (byte) 60, (byte) 18, (byte) -68, (byte) -68, (byte) 18, (byte) 70, (byte) -118, (byte) 50, (byte) -118, (byte) 70, (byte) 36, (byte) -125, (byte) -1, (byte) 7, (byte) 24, (byte) 68, (byte) 56, (byte) 24, (byte) 24, (byte) 25, (byte) 24, (byte) 89, (byte) 24, (byte) -107, (byte) 24, (byte) -104, (byte) 4, (byte) 25, (byte) -103, (byte) 5, (byte) 25, (byte) 89, (byte) -108, (byte) 24, (byte) -2, (byte) 31, (byte) 97, (byte) -112, (byte) 103, (byte) 96, (byte) 96, (byte) 100, (byte) 101, (byte) 4, (byte) 3, (byte) 6, (byte) 40, (byte) 96, (byte) 100, (byte) 98, (byte) 102, (byte) 97, (byte) 101, (byte) 99, (byte) -25, (byte) -32, (byte) -28, (byte) -30, (byte) 6, (byte) 42, (byte) -40, (byte) 42, (byte) -64, (byte) -64, (byte) -60, (byte) -56, (byte) -52, (byte) -52, (byte) -60, (byte) -62, (byte) -52, (byte) -54, (byte) -54, (byte) -62, (byte) 2, (byte) -108, (byte) -83, (byte) 5, (byte) -54, (byte) 51, (byte) -80, (byte) 8, (byte) -78, (byte) 10, (byte) 41, (byte) 26, (byte) 58, (byte) -78, (byte) 9, (byte) 7, (byte) 38, (byte) -78, (byte) 43, (byte) 21, (byte) -118, (byte) 24, (byte) 53, (byte) 78, (byte) 92, (byte) -56, (byte) -95, (byte) -20, (byte) -76, (byte) -15, (byte) -96, (byte) 104, (byte) -48, (byte) -59, (byte) 15, (byte) 42, (byte) -58, (byte) 73, (byte) 69, (byte) 77, (byte) -100, (byte) 92, (byte) 98, (byte) -30, (byte) 18, (byte) -110, (byte) 82, (byte) -86, (byte) 106, (byte) -22, (byte) 26, (byte) -102, (byte) 90, (byte) 38, (byte) -90, (byte) 102, (byte) -26, (byte) 22, (byte) -106, (byte) 86, (byte) -50, (byte) 46, (byte) -82, (byte) 110, (byte) -18, (byte) 30, (byte) -98, (byte) 94, (byte) -63, (byte) 33, (byte) -95, (byte) 97, (byte) -31, (byte) 17, (byte) -111, (byte) 81, (byte) -55, (byte) 41, (byte) -87, (byte) 105, (byte) -23, (byte) 25, (byte) -103, (byte) 89, (byte) -59, (byte) 37, (byte) -91, (byte) 101, (byte) -27, (byte) 21, (byte) -107, (byte) 85, (byte) -51, (byte) 45, (byte) -83, (byte) 109, (byte) -19, (byte) 29, (byte) -99, (byte) 93, (byte) -109, (byte) 38, (byte) 79, (byte) -103, (byte) 58, (byte) 109, (byte) -6, (byte) -116, (byte) -103, (byte) -77, (byte) 22, (byte) 45, (byte) 94, (byte) -78, (byte) 116, (byte) -39, (byte) -14, (byte) 21, (byte) 43, (byte) 87, (byte) 109, (byte) -38, (byte) -68, (byte) 101, (byte) -21, (byte) -74, (byte) -19, (byte) 59, (byte) 118, (byte) -18, (byte) 58, (byte) 116, (byte) -8, (byte) -56, (byte) -47, (byte) 99, (byte) -57, (byte) 79, (byte) -100, (byte) 60, (byte) 117, (byte) -23, (byte) -14, (byte) -107, (byte) -85, (byte) -41, (byte) -82, (byte) -33, (byte) -72, (byte) 121, (byte) -21, (byte) -31, (byte) -93, (byte) -57, (byte) 79, (byte) -98, (byte) 62, (byte) 123, (byte) -2, (byte) -30, (byte) -27, (byte) -85, (byte) -113, (byte) -97, (byte) 62, (byte) 127, (byte) -7, (byte) -6, (byte) -19, (byte) -5, (byte) -113, (byte) -97, (byte) -65, (byte) 64, (byte) -2, (byte) 98, (byte) 100, (byte) 96, (byte) 102, (byte) -124, (byte) 1, (byte) -84, (byte) -2, (byte) 18, (byte) 4, (byte) -6, (byte) -117, (byte) -119, (byte) -123, (byte) -123, (byte) -103, (byte) -123, (byte) 29, (byte) -28, (byte) 47, (byte) 70, (byte) -90, (byte) 114, (byte) -112, (byte) 2, (byte) 65, (byte) 22, (byte) 86, (byte) 69, (byte) 67, (byte) 54, (byte) 33, (byte) -57, (byte) 64, (byte) -10, (byte) -60, (byte) 66, (byte) 97, (byte) 37, (byte) -93, (byte) 70, (byte) 14, (byte) 17, (byte) -89, (byte) -119, (byte) 11, (byte) 55, (byte) 30, (byte) -28, (byte) 84, (byte) 54, (byte) 14, (byte) -6, (byte) 32, (byte) -102, (byte) 84, (byte) 116, (byte) -111, (byte) 75, (byte) 76, (byte) -59, (byte) -28, (byte) -95, (byte) -22, (byte) 71, (byte) -112, (byte) -41, (byte) -64, (byte) 62, (byte) 35, (byte) -50, (byte) 99, (byte) 77, (byte) 100, (byte) -7, (byte) 12, (byte) -18, (byte) 49, (byte) -124, (byte) -65, (byte) 110, (byte) 49, (byte) -16, (byte) -79, (byte) 48, (byte) 2, (byte) -29, (byte) 15, (byte) -24, (byte) 78, (byte) 6,
	(byte) 6, (byte) 123, (byte) -122, (byte) -97, (byte) -1, (byte) -70, (byte) 52, (byte) 22, (byte) -16, (byte) -49, (byte) 127, (byte) -79, (byte) -88, (byte) -21, (byte) -1, (byte) 77, (byte) 0, (byte) 38, (byte) 27, (byte) -28, (byte) 10, (byte) 110, (byte) 2, (byte) 0, (byte) 0};

	
    public static NativeSprite fromJs5(Js5ResourceProvider spriteResource, int fileId) {
        NativeSprite sprite = (NativeSprite) NativeSprite.SPRITE_LRU_CACHE.get(fileId);
        if (sprite == null) {
            if (Resource.USING_JAGEX_IMAGE_FORMAT) {
                sprite = Renderers.activeToolkit.generateSprite(SpriteType.listSingle(spriteResource, fileId), true);
            } else {
                sprite = NativeSprite.fromPixelgrabber(spriteResource.getFile(fileId));
            }
            NativeSprite.SPRITE_LRU_CACHE.put(sprite, fileId);
        }
        return sprite;
    }

    public static void method12680(AbstractToolkit toolkit, int x, int y, int width, int i_4, int height, AbstractFontRenderer fontrenderer, FontMetrics fontmetrics, String string_8) {
        int i_10 = 255 - NativeSprite.anInt3556 - NativeSprite.anInt156;
        if (i_10 < 0) {
            i_10 = 0;
        }

        if (StaticMedia.aNativeSprite5576 == null || StaticMedia.aNativeSprite647 == null) {
            if (Resource.SPRITES.isFileCached(StaticMethods.anInt5438) && Resource.SPRITES.isFileCached(NativeSprite.anInt4350)) {
                StaticMedia.aNativeSprite5576 = toolkit.generateSprite(SpriteType.listSingle(Resource.SPRITES, StaticMethods.anInt5438, 0), true);
                SpriteType spriteType = SpriteType.listSingle(Resource.SPRITES, NativeSprite.anInt4350, 0);
                StaticMedia.aNativeSprite647 = toolkit.generateSprite(spriteType, true);
                assert spriteType != null;
                spriteType.flipHorizontal();
                StaticMethods.aNativeSprite612 = toolkit.generateSprite(spriteType, true);
            } else {
                toolkit.drawFilledRectangle(x, y, width, height, i_10 << 24 | TextureOpIrregularBricks.anInt9792, 1);
            }
        }

        if (StaticMedia.aNativeSprite5576 != null && StaticMedia.aNativeSprite647 != null) {
            int i_11 = (width - StaticMedia.aNativeSprite647.getWidth() * 2) / StaticMedia.aNativeSprite5576.getWidth();
            for (int i_12 = 0; i_12 < i_11; i_12++) {
                StaticMedia.aNativeSprite5576.draw(x + StaticMedia.aNativeSprite647.getWidth() + i_12 * StaticMedia.aNativeSprite5576.getWidth(), y);
            }
            StaticMedia.aNativeSprite647.draw(x, y);
            StaticMethods.aNativeSprite612.draw(width + x - StaticMethods.aNativeSprite612.getWidth(), y);
        }

        fontrenderer.renderPlain(string_8, x + 3, (20 - fontmetrics.topPadding) / 2 + y + fontmetrics.topPadding, MiniMenu.anInt4722 | -16777216, -1);
        toolkit.drawFilledRectangle(x, height + y, width, i_4 - height, i_10 << 24 | TextureOpIrregularBricks.anInt9792, 1);
    }

	public static final LRUCache SPRITE_LRU_CACHE = new LRUCache(128, 4);

    public abstract void draw(int x, int y, int combineMode, int colorArgb, int blendMode);

    public abstract void setSize(int var1, int var2, int var3, int var4);

    public abstract void cacheSizeInto(int[] var1);

    public abstract int getWidth();

    public abstract int getSpriteHeight();


    public abstract void method2750(int var1, int var2, int var3, int var4, int var5, int var6);

    public abstract void pixelTransfer();

    public void draw(int x, int y) {
        draw(x, y, 1, 0, 1);
    }


    public void drawCircle(int x, int y, int width, int length, int i_5, int color, int alpha) {
        drawColoredCircle(x, y, width, length, i_5, color, alpha);
    }

    /**
     * 
     */
    public abstract void drawColoredCircle(int x, int y, int width, int length, int var5, int color, int blendMode);

    public void method2756(int x, int y, int width, int height) {
        method2772(x, y, width, height, 1, 0, 1);
    }


    public void method2758(float f_1, float f_2, int i_3, int rotation) {
        method2760(f_1, f_2, getSpriteWidth() / 2.0F, getSpriteHeight() / 2.0F, i_3, rotation, 1, 0);
    }

    public void method2760(float x, float y, float scaleX, float scaleY, int offset, int rotation, int i_7, int i_8) {
        if (offset != 0) {
            double rotationRadians = (rotation & 0xffff) * 9.587379924285257E-5D;
            float sinRotation = (float) StrictMath.sin(rotationRadians) * offset;
            float cosRotation = (float) StrictMath.cos(rotationRadians) * offset;
            float transformedX1 = (-scaleX * cosRotation + -scaleY * sinRotation) / 4096.0F + x;
            float transformedY1 = (--scaleX * sinRotation + -scaleY * cosRotation) / 4096.0F + y;
            float transformedX2 = ((getSpriteWidth() - scaleX) * cosRotation + -scaleY * sinRotation) / 4096.0F + x;
            float transformedY2 = (-(getSpriteWidth() - scaleX) * sinRotation + -scaleY * cosRotation) / 4096.0F + y;
            float transformedX3 = (-scaleX * cosRotation + (getSpriteHeight() - scaleY) * sinRotation) / 4096.0F + x;
            float transformedY3 = (--scaleX * sinRotation + (getSpriteHeight() - scaleY) * cosRotation) / 4096.0F + y;
            method2763(transformedX1, transformedY1, transformedX2, transformedY2, transformedX3, transformedY3, i_7, i_8, 1);
        }
    }

    public void drawRotatedSprite(float f_1, float f_2, int i_4, ComponentSurface class4555, int i_6, int i_7) {
        rotate(f_1, f_2, getSpriteWidth() / 2.0F, getSpriteHeight() / 2.0F, 4141, i_4, class4555, i_6, i_7);
    }

    public void rotate(float f_1, float f_2, float f_3, float f_4, int i_5, int i_6, ComponentSurface class4557, int i_8, int i_9) {
        if (i_5 != 0) {
            double d_10 = (i_6 & 0xffff) * 9.587379924285257E-5D;
            float f_12 = (float) StrictMath.sin(d_10) * i_5;
            float f_13 = (float) StrictMath.cos(d_10) * i_5;
            float f_14 = (-f_3 * f_13 + -f_4 * f_12) / 4096.0F + f_1;
            float f_15 = (--f_3 * f_12 + -f_4 * f_13) / 4096.0F + f_2;
            float f_16 = ((getSpriteWidth() - f_3) * f_13 + -f_4 * f_12) / 4096.0F + f_1;
            float f_17 = (-(getSpriteWidth() - f_3) * f_12 + -f_4 * f_13) / 4096.0F + f_2;
            float f_18 = (-f_3 * f_13 + (getSpriteHeight() - f_4) * f_12) / 4096.0F + f_1;
            float f_19 = (--f_3 * f_12 + (getSpriteHeight() - f_4) * f_13) / 4096.0F + f_2;
            method2797(f_14, f_15, f_16, f_17, f_18, f_19, class4557, i_8, i_9);
        }

    }

    public void method2763(float f_1, float f_2, float f_3, float f_4, float f_5, float f_6, int i_7, int i_8, int i_9) {
        method2764(f_1, f_2, f_3, f_4, f_5, f_6, i_7, i_8, i_9);
    }

    public abstract void method2764(float var1, float var2, float var3, float var4, float var5, float var6, int var7, int var8, int var9);


    public abstract void method2766(float var1, float var2, float var3, float var4, float var5, float var6, int var7, ComponentSurface var8, int var9, int var10);


    public abstract void method2772(int var1, int var2, int var3, int var4, int var5, int var6, int var7);

    public abstract void method2773(int var1, int var2, ComponentSurface var3, int var4, int var5);



    /**
     * Draws a black opaque circle
     */
    public void drawCircle(int x, int y, int width, int length) {
        drawColoredCircle(x, y, width, length, 1, 0, 1);
    }

    public void method2790(float f_1, float f_2, int i_3, int i_4, int i_6) {
        method2760(f_1, f_2, getSpriteWidth() / 2.0F, getSpriteHeight() / 2.0F, i_3, i_4, 0, i_6);
    }

    public abstract int getHeight();

    public void method2797(float f_1, float f_2, float f_3, float f_4, float f_5, float f_6, ComponentSurface class4557, int i_8, int i_9) {
        method2766(f_1, f_2, f_3, f_4, f_5, f_6, 1, class4557, i_8, i_9);
    }

    @Override
    public abstract int getSpriteWidth();

    public abstract FrameBuffer getFramebuffer();

	public static void resetCache() {
	    SPRITE_LRU_CACHE.reset();
	}

	public static boolean supportsGzippedSprites() {
	    try {
	        GzipDecompressor gzip = new GzipDecompressor();
	        byte[] spriteData = gzip.decompressCustomSize(GZIPPED_SPRITE_DATA);
	        fromPixelgrabber(spriteData);
	        return true;
	    } catch (Exception exception) {
	        return false;
	    }
	}

	public static NativeSprite fromPixelgrabber(byte[] imageData) {
	    if (imageData == null) {
	        throw new RuntimeException("");
	    } 
	    while (true) {
	        try {
	            Image image = Toolkit.getDefaultToolkit().createImage(imageData);
	            MediaTracker mediaTracker = new MediaTracker(GameShell.suppliedApplet);
	            mediaTracker.addImage(image, 0);
	            mediaTracker.waitForAll();
	            int width = image.getWidth(GameShell.suppliedApplet);
	            int height = image.getHeight(GameShell.suppliedApplet);
	            if (!mediaTracker.isErrorAny() && width >= 0 && height >= 0) {
	                int[] pixels = new int[width * height];
	                PixelGrabber pixelGrabber = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);
	                pixelGrabber.grabPixels();
	                NativeSprite nativeSprite = Renderers.activeToolkit.createNativeSprite(pixels, width, width, height);
	                return nativeSprite;
	            }
	            throw new RuntimeException("");
	        } catch (InterruptedException ignored) {
	        	// NOOP
	        }
	    }
	}

}
