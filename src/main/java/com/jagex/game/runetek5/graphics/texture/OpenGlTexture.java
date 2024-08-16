package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

import jaggl.OpenGL;
import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

public abstract class OpenGlTexture implements AutoCloseable {

    public static final int[] HANDLE_ALLOCATE = new int[1];
    public boolean linearTextureFilter;
    public boolean mipmappingEnabled;
    public int activeHandle;
    public final OpenGlToolkit activeToolkit;
    public final int textureTarget;
    public final GLFormat aClass1501650;
    public final GLActionFormat aClass761651;
    public final int pixelCount;

    public OpenGlTexture(OpenGlToolkit toolkit, int textureTarget, GLFormat class1503, GLActionFormat class764, int pixelCount, boolean mipmapping) {
        this.activeToolkit = toolkit;
        this.textureTarget = textureTarget;
        this.aClass1501650 = class1503;
        this.aClass761651 = class764;
        this.mipmappingEnabled = mipmapping;
        this.pixelCount = pixelCount;
        OpenGL.glGenTextures(1, HANDLE_ALLOCATE, 0);
        this.activeHandle = HANDLE_ALLOCATE[0];
        updateOccupiedSize(0);
    }

    public void setLinearTextureFilter(boolean linear) {
        if (this.linearTextureFilter != linear) {
            this.linearTextureFilter = linear;
            prepareTextureRendering();
        }
    }

    public void prepareTextureRendering() {
    	this.activeToolkit.setActiveTexture(this);
        if (this.linearTextureFilter) {
            OpenGL.glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, this.mipmappingEnabled ? GL_LINEAR_MIPMAP_LINEAR : GL_LINEAR);
            OpenGL.glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            OpenGL.glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, this.mipmappingEnabled ? GL_NEAREST_MIPMAP_NEAREST : GL_NEAREST);
            OpenGL.glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public boolean generateMipmaps() {
        if (this.activeToolkit.intelGpu) {
            int sizeBefore = getBufferSizeRequirement();
            this.activeToolkit.setActiveTexture(this);
            OpenGL.glGenerateMipmapEXT(this.textureTarget);
            this.mipmappingEnabled = true;
            prepareTextureRendering();
            updateOccupiedSize(sizeBefore);
            return true;
        } 
        return false;
    }

    void setMipmapping(boolean mipmapping) {
        if (this.mipmappingEnabled != mipmapping) {
            int sizeBefore = getBufferSizeRequirement();
            this.mipmappingEnabled = true;
            prepareTextureRendering();
            updateOccupiedSize(sizeBefore);
        }
    }

    public void delete() {
        if (this.activeHandle > 0) {
        	this.activeToolkit.textureBuffersSize -= getBufferSizeRequirement();
            int[] handleToDelete = {this.activeHandle};
            OpenGL.glDeleteTextures(1, handleToDelete, 0);
            this.activeHandle = 0;
        }
    }

    public void updateOccupiedSize(int sizeBefore) {
    	this.activeToolkit.textureBuffersSize -= sizeBefore;
    	this.activeToolkit.textureBuffersSize += getBufferSizeRequirement();
    }

    public int getBufferSizeRequirement() {
        int size = this.aClass1501650.bytesPerPixel * this.aClass761651.bytesPerEntry * this.pixelCount;
        return this.mipmappingEnabled ? size * 4 / 3 : size;
    }

    @Override
    public void close() {
        dispose();
    }

    public void dispose() {
        if (this.activeHandle > 0) {
        	this.activeToolkit.requestTextureCleanup(this.activeHandle, getBufferSizeRequirement());
        	this.activeHandle = 0;
        }
    }

    public static boolean isPowerOfTwo(int i_0) {
	    return i_0 == (i_0 & -i_0);
	}

	static void method2359(int i_0, int i_1, int i_2, int i_3, GLFormat class1504, byte[] bytes_5) {
        int i_21 = i_2;
        int i_31 = i_3;
        if (i_21 > 0 && !OpenGlTexture.isPowerOfTwo(i_21)) {
            throw new IllegalArgumentException("");
        } 
        if (i_31 > 0 && !OpenGlTexture.isPowerOfTwo(i_31)) {
            throw new IllegalArgumentException("");
        }
        int i_6 = class1504.bytesPerPixel;
        int i_7 = 0;
        int i_8 = Math.min(i_21, i_31);
        int i_9 = i_21 >> 1;
        int i_10 = i_31 >> 1;
        byte[] bytes_11 = bytes_5;
        byte[] bytes_12 = new byte[i_9 * i_10 * i_6];

        while (true) {
            OpenGL.glTexImage2Dub(i_0, i_7, i_1, i_21, i_31, 0, OpenGlToolkit.format(class1504), 5121, bytes_11, 0);
            if (i_8 <= 1) {
                return;
            }

            int i_13 = i_6 * i_21;

            for (int i_14 = 0; i_14 < i_6; i_14++) {
                int i_15 = i_14;
                int i_16 = i_14;
                int i_17 = i_14 + i_13;

                for (int i_18 = 0; i_18 < i_10; i_18++) {
                    for (int i_19 = 0; i_19 < i_9; i_19++) {
                        byte b_20 = bytes_11[i_16];
                        i_16 += i_6;
                        int i_22 = bytes_11[i_16] + b_20;
                        i_16 += i_6;
                        i_22 += bytes_11[i_17];
                        i_17 += i_6;
                        i_22 += bytes_11[i_17];
                        i_17 += i_6;
                        bytes_12[i_15] = (byte) (i_22 >> 2);
                        i_15 += i_6;
                    }

                    i_16 += i_13;
                    i_17 += i_13;
                }
            }

            byte[] bytes_21 = bytes_12;
            bytes_12 = bytes_11;
            bytes_11 = bytes_21;
            i_21 = i_9;
            i_31 = i_10;
            i_9 >>= 1;
            i_10 >>= 1;
            i_8 >>= 1;
            ++i_7;
        }
    }

    static void generateMipmaps(int internalFormat, int width, int height, GLFormat format, float[] data) {
        if (width > 0 && !OpenGlTexture.isPowerOfTwo(width)) {
            throw new IllegalArgumentException("");
        } 
        if (height > 0 && !OpenGlTexture.isPowerOfTwo(height)) {
            throw new IllegalArgumentException("");
        } 
        int bytesPerPixel = format.bytesPerPixel;
        int level = 0;
        int minDimension = Math.min(width, height);
        int halfWidth = width >> 1;
        int halfHeight = height >> 1;
        float[] floats_12 = new float[halfWidth * halfHeight * bytesPerPixel];
        while (true) {
            OpenGL.glTexImage2Df(GL_TEXTURE_2D, level, internalFormat, width, height, 0, OpenGlToolkit.format(format), GL_FLOAT, data, 0);
            if (minDimension <= 1) {
                return;
            }
            int rowBytes = bytesPerPixel * width;
            for (int i_14 = 0; i_14 < bytesPerPixel; i_14++) {
                int i_15 = i_14;
                int i_16 = i_14;
                int i_17 = i_14 + rowBytes;
                for (int y = 0; y < halfHeight; y++) {
                    for (int x = 0; x < halfWidth; x++) {
                        float f_20 = data[i_16];
                        i_16 += bytesPerPixel;
                        f_20 += data[i_16];
                        i_16 += bytesPerPixel;
                        f_20 += data[i_17];
                        i_17 += bytesPerPixel;
                        f_20 += data[i_17];
                        i_17 += bytesPerPixel;
                        floats_12[i_15] = f_20 * 0.25F;
                        i_15 += bytesPerPixel;
                    }
                    i_16 += rowBytes;
                    i_17 += rowBytes;
                }
            }
            float[] floats_21 = floats_12;
            floats_12 = data;
            data = floats_21;
            width = halfWidth;
            height = halfHeight;
            halfWidth >>= 1;
            halfHeight >>= 1;
            minDimension >>= 1;
            level++;
        }
    }

    static void createMipmappedCubeTexture(int target, int i_1, int i_2, int i_3, int i_5, int[] ints_6) {
        int step = i_2;
        int i_31 = i_3;
        if (step > 0 && !OpenGlTexture.isPowerOfTwo(step)) {
            throw new IllegalArgumentException("");
        } 
        if (i_31 > 0 && !OpenGlTexture.isPowerOfTwo(i_31)) {
            throw new IllegalArgumentException("");
        } 
        int i_7 = 0;
        int i_8 = Math.min(step, i_31);
        int i_9 = step >> 1;
        int i_10 = i_31 >> 1;
        int[] argbPixels = ints_6;
        int[] replacementPixels = new int[i_9 * i_10];
        while (true) {
            OpenGL.glTexImage2Di(target, i_7, i_1, step, i_31, 0, GL_BGRA, i_5, argbPixels, 0);
            if (i_8 <= 1) {
                return;
            }
            int dstPixelIndex = 0;
            int srcPixelIndex = 0;
            int srcPixelIndexOffset = srcPixelIndex + step;
            for (int i_16 = 0; i_16 < i_10; i_16++) {
                for (int i_17 = 0; i_17 < i_9; i_17++) {
                    int colorArgbA = argbPixels[srcPixelIndex++];
                    int colorArgbB = argbPixels[srcPixelIndex++];
                    int colorArgbC = argbPixels[srcPixelIndexOffset++];
                    int colorArgbD = argbPixels[srcPixelIndexOffset++];
                    int red = colorArgbA >> 24 & 0xff;
                    int green = colorArgbA >> 16 & 0xff;
                    int blue = colorArgbA >> 8 & 0xff;
                    int alpha = colorArgbA & 0xff;
                    red += colorArgbB >> 24 & 0xff;
                    green += colorArgbB >> 16 & 0xff;
                    blue += colorArgbB >> 8 & 0xff;
                    alpha += colorArgbB & 0xff;
                    red += colorArgbC >> 24 & 0xff;
                    green += colorArgbC >> 16 & 0xff;
                    blue += colorArgbC >> 8 & 0xff;
                    alpha += colorArgbC & 0xff;
                    red += colorArgbD >> 24 & 0xff;
                    green += colorArgbD >> 16 & 0xff;
                    blue += colorArgbD >> 8 & 0xff;
                    alpha += colorArgbD & 0xff;
                    replacementPixels[dstPixelIndex++] = (red & 0x3fc) << 22 | (green & 0x3fc) << 14 | (blue & 0x3fc) << 6 | (alpha & 0x3fc) >> 2;
                }
                srcPixelIndex += step;
                srcPixelIndexOffset += step;
            }
            int[] ints_26 = replacementPixels;
            replacementPixels = argbPixels;
            argbPixels = ints_26;
            step = i_9;
            i_31 = i_10;
            i_9 >>= 1;
            i_10 >>= 1;
            i_8 >>= 1;
            ++i_7;
        }
    }
}
