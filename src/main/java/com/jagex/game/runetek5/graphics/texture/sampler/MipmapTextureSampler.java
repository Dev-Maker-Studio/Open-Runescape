package com.jagex.game.runetek5.graphics.texture.sampler;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.runetek5.graphics.texture.OpenGlTexture;
import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

import jaggl.OpenGL;

public abstract class MipmapTextureSampler implements TextureSampler {

    public static final int[] textures = new int[1];
    public int texture;
    public boolean mipmapping;
    public SampleMode sampleMode;
    public final OpenGLNativeToolkit activeToolkit;
    public final int textureTarget;
    public final GLFormat pixelFormat;
    public final GLActionFormat pixelActionFormat;
    public final int pixelCount;

    public MipmapTextureSampler(OpenGLNativeToolkit toolkit, int textureTarget, GLFormat pixelFormat, GLActionFormat pixelActionFormat, int pixelCount, boolean mipmapping) {
        this.sampleMode = SampleMode.LINEAR;
        this.activeToolkit = toolkit;
        this.textureTarget = textureTarget;
        this.pixelFormat = pixelFormat;
        this.pixelActionFormat = pixelActionFormat;
        this.mipmapping = mipmapping;
        this.pixelCount = pixelCount;
        OpenGL.glGenTextures(1, textures, 0);
        this.texture = textures[0];
        initializeSampler();
        updateTexturesSizeSum(0);
    }

    public int getSize() {
        int size = this.pixelFormat.bytesPerPixel * this.pixelActionFormat.bytesPerEntry * this.pixelCount;
        return this.mipmapping ? size * 4 / 3 : size;
    }

    public void updateTexturesSizeSum(int size) {
        this.activeToolkit.texturesSizeOccupied -= size;
        this.activeToolkit.texturesSizeOccupied += getSize();
    }

    @Override
    public void deallocate() {
        if (this.texture > 0) {
            this.activeToolkit.requestTextureDeletion(this.texture, getSize());
            this.texture = 0;
        }
    }

    public void initializeSampler() {
        this.activeToolkit.setTextureSampler(this);
        if (this.sampleMode == SampleMode.LINEAR) {
            OpenGL.glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, this.mipmapping ? GL_LINEAR_MIPMAP_LINEAR : GL_LINEAR);
            OpenGL.glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            OpenGL.glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, this.mipmapping ? GL_NEAREST_MIPMAP_NEAREST : GL_NEAREST);
            OpenGL.glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public boolean enableMipmapping() {
        int size = getSize();
        this.activeToolkit.setTextureSampler(this);
        OpenGL.glGenerateMipmapEXT(this.textureTarget);
        this.mipmapping = true;
        initializeSampler();
        updateTexturesSizeSum(size);
        return true;
    }

    @SuppressWarnings("removal")
	@Override
    protected void finalize() throws Throwable {
        deallocate();
        super.finalize();
    }

    @Override
    public void bindTexture() {
        int slot = this.activeToolkit.getCurrentTextureSlot();
        int type = this.activeToolkit.textureSlots[slot];
        if (type != this.textureTarget) {
            if (type != 0) {
                OpenGL.glBindTexture(type, 0);
                OpenGL.glDisable(type);
            }

            OpenGL.glEnable(this.textureTarget);
            this.activeToolkit.textureSlots[slot] = this.textureTarget;
        }

        OpenGL.glBindTexture(this.textureTarget, this.texture);
    }

    @Override
    public void setSamplerMode(SampleMode sampleMode) {
        if (this.sampleMode != sampleMode) {
        	this.sampleMode = sampleMode;
            initializeSampler();
        }
    }
    
    public void generateMipmaps(int i_1, int i_2, int i_3, float[] floats_4) {
        int i_21 = i_2;
        int i_31 = i_3;
        if (i_21 > 0 && !OpenGlTexture.isPowerOfTwo(i_21)) {
            throw new IllegalArgumentException("");
        } 
        if (i_31 > 0 && !OpenGlTexture.isPowerOfTwo(i_31)) {
            throw new IllegalArgumentException("");
        } 
        int i_5 = this.pixelFormat.bytesPerPixel;
        int i_6 = 0;
        int i_7 = Math.min(i_21, i_31);
        int i_8 = i_21 >> 1;
        int i_9 = i_31 >> 1;
        float[] floats_10 = floats_4;
        float[] floats_11 = new float[i_8 * i_9 * i_5];

        while (true) {
            OpenGL.glTexImage2Df(i_1, i_6, OpenGLNativeToolkit.texture2dInternalFormat(this.pixelFormat, this.pixelActionFormat), i_21, i_31, 0, OpenGLNativeToolkit.texture2dFormat(this.pixelFormat), 5126, floats_10, 0);
            if (i_7 <= 1) {
                return;
            }

            int i_12 = i_5 * i_21;

            for (int i_13 = 0; i_13 < i_5; i_13++) {
                int i_14 = i_13;
                int i_15 = i_13;
                int i_16 = i_13 + i_12;

                for (int i_17 = 0; i_17 < i_9; i_17++) {
                    for (int i_18 = 0; i_18 < i_8; i_18++) {
                        float f_19 = floats_10[i_15];
                        i_15 += i_5;
                        f_19 += floats_10[i_15];
                        i_15 += i_5;
                        f_19 += floats_10[i_16];
                        i_16 += i_5;
                        f_19 += floats_10[i_16];
                        i_16 += i_5;
                        floats_11[i_14] = f_19 * 0.25F;
                        i_14 += i_5;
                    }

                    i_15 += i_12;
                    i_16 += i_12;
                }
            }

            float[] floats_20 = floats_11;
            floats_11 = floats_10;
            floats_10 = floats_20;
            i_21 = i_8;
            i_31 = i_9;
            i_8 >>= 1;
            i_9 >>= 1;
            i_7 >>= 1;
            ++i_6;
        }
    }
    
    public void generateMipmaps(int TextureType, int Width, int Height, byte[] Pixels) {
        if (Width > 0 && !OpenGlTexture.isPowerOfTwo(Width)) {
            throw new IllegalArgumentException("");
        } 
        if (Height > 0 && !OpenGlTexture.isPowerOfTwo(Height)) {
            throw new IllegalArgumentException("");
        } 
        int pixelByteCount = this.pixelFormat.bytesPerPixel;
        int i_6 = 0;
        int shortestDimension = Math.min(Width, Height);
        int centerX = Width >> 1;
        int centerY = Height >> 1;
        byte[] bytes_10 = Pixels;
        byte[] bytes_11 = new byte[centerX * centerY * pixelByteCount];

        while (true) {
            OpenGL.glTexImage2Dub(TextureType, i_6, OpenGLNativeToolkit.texture2dInternalFormat(this.pixelFormat, this.pixelActionFormat), Width, Height, 0, OpenGLNativeToolkit.texture2dFormat(this.pixelFormat), GL_UNSIGNED_BYTE, bytes_10, 0);
            if (shortestDimension <= 1) {
                return;
            }

            int i_12 = pixelByteCount * Width;

            for (int i_13 = 0; i_13 < pixelByteCount; i_13++) {
                int i_14 = i_13;
                int i_15 = i_13;
                int i_16 = i_13 + i_12;

                for (int i_17 = 0; i_17 < centerY; i_17++) {
                    for (int i_18 = 0; i_18 < centerX; i_18++) {
                        byte b_19 = bytes_10[i_15];
                        i_15 += pixelByteCount;
                        int i_21 = bytes_10[i_15] + b_19;
                        i_15 += pixelByteCount;
                        i_21 += bytes_10[i_16];
                        i_16 += pixelByteCount;
                        i_21 += bytes_10[i_16];
                        i_16 += pixelByteCount;
                        bytes_11[i_14] = (byte) (i_21 >> 2);
                        i_14 += pixelByteCount;
                    }

                    i_15 += i_12;
                    i_16 += i_12;
                }
            }

            byte[] bytes_20 = bytes_11;
            bytes_11 = bytes_10;
            bytes_10 = bytes_20;
            Width = centerX;
            Height = centerY;
            centerX >>= 1;
            centerY >>= 1;
            shortestDimension >>= 1;
            ++i_6;
        }
    }

    public void generateMipmaps(int TextureTarget, int Width, int Height, int[] Pixels) {
        if (Width > 0 && !OpenGlTexture.isPowerOfTwo(Width)) {
            throw new IllegalArgumentException("");
        } 
        if (Height > 0 && !OpenGlTexture.isPowerOfTwo(Height)) {
            throw new IllegalArgumentException("");
        } 
        if (this.pixelFormat != GLFormat.HDR) {
            throw new IllegalArgumentException("");
        } 
        int i_5 = 0;
        int i_6 = Math.min(Width, Height);
        int i_7 = Width >> 1;
        int i_8 = Height >> 1;
        int[] ints_9 = Pixels;
        int[] ints_10 = new int[i_7 * i_8];

        while (true) {
            OpenGL.glTexImage2Di(TextureTarget, i_5, OpenGLNativeToolkit.texture2dInternalFormat(this.pixelFormat, this.pixelActionFormat), Width, Height, 0, 32993, this.activeToolkit.pixelType, ints_9, 0);
            if (i_6 <= 1) {
                return;
            }

            int i_11 = 0;
            int i_12 = 0;
            int i_13 = i_12 + Width;

            for (int i_14 = 0; i_14 < i_8; i_14++) {
                for (int i_15 = 0; i_15 < i_7; i_15++) {
                    int i_16 = ints_9[i_12++];
                    int i_17 = ints_9[i_12++];
                    int i_18 = ints_9[i_13++];
                    int i_19 = ints_9[i_13++];
                    int i_20 = i_16 >> 24 & 0xff;
                    int i_21 = i_16 >> 16 & 0xff;
                    int i_22 = i_16 >> 8 & 0xff;
                    int i_23 = i_16 & 0xff;
                    i_20 += i_17 >> 24 & 0xff;
                    i_21 += i_17 >> 16 & 0xff;
                    i_22 += i_17 >> 8 & 0xff;
                    i_23 += i_17 & 0xff;
                    i_20 += i_18 >> 24 & 0xff;
                    i_21 += i_18 >> 16 & 0xff;
                    i_22 += i_18 >> 8 & 0xff;
                    i_23 += i_18 & 0xff;
                    i_20 += i_19 >> 24 & 0xff;
                    i_21 += i_19 >> 16 & 0xff;
                    i_22 += i_19 >> 8 & 0xff;
                    i_23 += i_19 & 0xff;
                    ints_10[i_11++] = (i_20 & 0x3fc) << 22 | (i_21 & 0x3fc) << 14 | (i_22 & 0x3fc) << 6 | (i_23 & 0x3fc) >> 2;
                }

                i_12 += Width;
                i_13 += Width;
            }

            int[] ints_24 = ints_10;
            ints_10 = ints_9;
            ints_9 = ints_24;
            Width = i_7;
            Height = i_8;
            i_7 >>= 1;
            i_8 >>= 1;
            i_6 >>= 1;
            ++i_5;
        }        
    }
}
