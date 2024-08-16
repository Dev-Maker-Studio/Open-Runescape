package com.jagex.game.runetek5.graphics.texture.sampler;

import jagdx.IDirect3DDevice;
import jagdx.IDirect3DTexture;

import java.nio.ByteBuffer;

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.utils.MathUtils;

public class TextureDirect3D extends Direct3DTextureSampler implements NativeTextureSampler {

    public final int anInt9154;
    public final int anInt9157;
    public boolean aBool9156;
    public boolean aBool9155;

    public TextureDirect3D(DirectXNativeToolkit class505Sub2Sub21, GLFormat class1502, int i_3, int i_4, boolean bool5, byte[] bytes_6) {
        super(class505Sub2Sub21, class1502, GLActionFormat.PIXEL_FORMAT, bool5 && class505Sub2Sub21.SUPPORTS_CUBE_MAP);
        int i_81 = 0;
        if (!super.dxToolkit.IS_NO_MIPMAP) {
            this.anInt9154 = MathUtils.nextPowerOfTwo(i_3);
            this.anInt9157 = MathUtils.nextPowerOfTwo(i_4);
        } else {
            this.anInt9154 = i_3;
            this.anInt9157 = i_4;
        }

        if (bool5) {
            super.handle = IDirect3DDevice.CreateTexture(super.dxToolkit.renderTarget, this.anInt9154, this.anInt9157, 0, 1024, DirectXNativeToolkit.method15652(super.glformat, GLActionFormat.PIXEL_FORMAT), 1);
        } else {
            super.handle = IDirect3DDevice.CreateTexture(super.dxToolkit.renderTarget, this.anInt9154, this.anInt9157, 1, 0, DirectXNativeToolkit.method15652(super.glformat, GLActionFormat.PIXEL_FORMAT), 1);
        }

        i_81 = i_3;

        ByteBuffer bytebuffer9 = super.dxToolkit.directBuffer;
        bytebuffer9.clear();
        GLFormat class15010 = super.glformat;
        GLFormat class15010000 = super.glformat;
        if (class15010 == GLFormat.aClass1501956) {
            bytebuffer9.put(bytes_6, 0, i_3 * i_4 / 2);
            IDirect3DTexture.Upload(super.handle, 0, 0, 0, i_3, i_4 / 4, i_3 / 4 * 8, 0, super.dxToolkit.directBufferAddress);
        } else {
            GLFormat class15011 = super.glformat;
            if (class15011 == GLFormat.aClass1501957) {
                bytebuffer9.put(bytes_6, 0, i_3 * i_4);
                IDirect3DTexture.Upload(super.handle, 0, 0, 0, i_3, i_4 / 4, i_3 / 4 * 16, 0, super.dxToolkit.directBufferAddress);
            } else {
                bytebuffer9.put(bytes_6, 0, i_81 * i_4);
                IDirect3DTexture.Upload(super.handle, 0, 0, 0, i_3, i_4, super.glformat.bytesPerPixel * i_81, 0, super.dxToolkit.directBufferAddress);
            }
        }

    }

    protected TextureDirect3D(DirectXNativeToolkit class505Sub2Sub21, GLFormat class1502, GLActionFormat class763, int i_4, int i_5, int i_6, int i_7) {
        super(class505Sub2Sub21, class1502, class763, false);
        if (!super.dxToolkit.IS_NO_MIPMAP) {
            this.anInt9154 = MathUtils.nextPowerOfTwo(i_4);
            this.anInt9157 = MathUtils.nextPowerOfTwo(i_5);
        } else {
            this.anInt9154 = i_4;
            this.anInt9157 = i_5;
        }

        super.handle = IDirect3DDevice.CreateTexture(super.dxToolkit.renderTarget, i_4, i_5, 0, i_6, DirectXNativeToolkit.method15652(super.glformat, super.glactionFormat), i_7);
    }

    public TextureDirect3D(DirectXNativeToolkit class505Sub2Sub21, GLFormat class1502, int i_3, int i_4, float[] floats_6) {
        super(class505Sub2Sub21, class1502, GLActionFormat.PIXEL_FORMAT, class505Sub2Sub21.SUPPORTS_CUBE_MAP);
        int i_81 = 0;
        if (!super.dxToolkit.IS_NO_MIPMAP) {
            this.anInt9154 = MathUtils.nextPowerOfTwo(i_3);
            this.anInt9157 = MathUtils.nextPowerOfTwo(i_4);
        } else {
            this.anInt9154 = i_3;
            this.anInt9157 = i_4;
        }

        super.handle = IDirect3DDevice.CreateTexture(super.dxToolkit.renderTarget, this.anInt9154, this.anInt9157, 0, 1024, DirectXNativeToolkit.method15652(super.glformat, GLActionFormat.FLOAT_32_ARB), 1);

        i_81 = i_3;

        ByteBuffer bytebuffer9 = super.dxToolkit.directBuffer;
        bytebuffer9.clear();
        bytebuffer9.asFloatBuffer().put(floats_6, 0, super.glformat.bytesPerPixel * i_81 * i_4);
        IDirect3DTexture.Upload(super.handle, 0, 0, 0, i_3, i_4, super.glformat.bytesPerPixel * i_81 * 4, 0, super.dxToolkit.directBufferAddress);
    }

    public TextureDirect3D(DirectXNativeToolkit class505Sub2Sub21, GLFormat class1502, GLActionFormat class763, int i_4, int i_5) {
        this(class505Sub2Sub21, class1502, class763, i_4, i_5, 0, 1);
    }

    public TextureDirect3D(DirectXNativeToolkit class505Sub2Sub21, int i_2, int i_3, boolean bool4, int[] ints_5, int i_6, int i_7) {
        super(class505Sub2Sub21, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, bool4 && class505Sub2Sub21.SUPPORTS_CUBE_MAP);
        int i_71 = i_7;
        if (!super.dxToolkit.IS_NO_MIPMAP) {
            this.anInt9154 = MathUtils.nextPowerOfTwo(i_2);
            this.anInt9157 = MathUtils.nextPowerOfTwo(i_3);
        } else {
            this.anInt9154 = i_2;
            this.anInt9157 = i_3;
        }

        if (bool4) {
            super.handle = IDirect3DDevice.CreateTexture(super.dxToolkit.renderTarget, this.anInt9154, this.anInt9157, 0, 1024, 21, 1);
        } else {
            super.handle = IDirect3DDevice.CreateTexture(super.dxToolkit.renderTarget, this.anInt9154, this.anInt9157, 1, 0, 21, 1);
        }

        if (i_71 == 0) {
            i_71 = i_2;
        }

        ByteBuffer bytebuffer8 = super.dxToolkit.directBuffer;
        bytebuffer8.clear();
        bytebuffer8.asIntBuffer().put(ints_5, i_6, i_3 * i_71);
        IDirect3DTexture.Upload(super.handle, 0, 0, 0, i_2, i_3, super.glformat.bytesPerPixel * i_71, 0, super.dxToolkit.directBufferAddress);
    }

    @Override
    public boolean method46() {
        return true;
    }

    @Override
    public void method48(int i_1, int i_2, int i_3, int i_4, int[] src, int offset, int i_7) {
        int i_71 = i_7;
        if (super.glformat == GLFormat.HDR && super.glactionFormat == GLActionFormat.PIXEL_FORMAT) {
            if (i_71 == 0) {
                i_71 = i_3;
            }

            ByteBuffer buffer = super.dxToolkit.directBuffer;
            buffer.clear();
            buffer.asIntBuffer().put(src, offset, i_4 * i_71);
            IDirect3DTexture.Upload(super.handle, 0, i_1, i_2, i_3, i_4, super.glformat.bytesPerPixel * i_71, 0, super.dxToolkit.directBufferAddress);
        } else {
            throw new RuntimeException();
        }
    }

    @Override
    public long getHandle() {
        return super.handle;
    }

    @Override
    public float method62(float f_1) {
        return f_1 / this.anInt9157;
    }

    @Override
    public void method52(int i_1, int i_2, int i_3, int i_4, byte[] bytes_5, GLFormat class1506, int i_7, int i_8) {
        int i_81 = i_8;
        if (class1506 == super.glformat && super.glactionFormat == GLActionFormat.PIXEL_FORMAT) {
            if (i_81 == 0) {
                i_81 = i_3;
            }

            ByteBuffer bytebuffer9 = super.dxToolkit.directBuffer;
            bytebuffer9.clear();
            bytebuffer9.put(bytes_5, i_7, i_81 * i_4);
            IDirect3DTexture.Upload(super.handle, 0, i_1, i_2, i_3, i_4, super.glformat.bytesPerPixel * i_81, 0, super.dxToolkit.directBufferAddress);
        } else {
            throw new RuntimeException();
        }
    }

    @Override
    public void setRepeat(boolean bool1, boolean bool2) {
        this.aBool9156 = bool1;
        this.aBool9155 = bool2;
    }

    @Override
    public void method53(int i_3, int i_4, int[] ints_5) {
        if (super.glformat == GLFormat.HDR && super.glactionFormat == GLActionFormat.PIXEL_FORMAT) {
            ByteBuffer bytebuffer7 = super.dxToolkit.directBuffer;
            bytebuffer7.clear();
            IDirect3DTexture.Download(super.handle, 0, 0, 0, i_3, i_4, i_3 * 4, 0, super.dxToolkit.directBufferAddress);
            bytebuffer7.asIntBuffer().get(ints_5, 0, i_3 * i_4);
        } else {
            throw new RuntimeException();
        }
    }

    @Override
    public void bindTexture() {
        super.dxToolkit.setTextureSamplerState(this);
    }

    @Override
    public float method47(float f_1) {
        return f_1 / this.anInt9154;
    }

    @Override
    public void deallocate() {
        super.deallocate();
    }

    @Override
    public int getTextureHeight() {
        return this.anInt9157;
    }

    @Override
    public boolean enableMipmapping() {
        return false;
    }

    @Override
    public int getTextureWidth() {
        return this.anInt9154;
    }

    @Override
    public void setSamplerMode(SampleMode class351) {
        super.setSamplerMode(class351);
    }
}
