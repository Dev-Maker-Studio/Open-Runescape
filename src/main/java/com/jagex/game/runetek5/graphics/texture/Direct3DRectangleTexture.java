package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.graphics.texture.sampler.RectangleTextureSampler;
import com.jagex.game.runetek5.graphics.texture.sampler.SampleMode;
import com.jagex.game.runetek5.graphics.texture.sampler.TextureDirect3D;
import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit;
import com.jagex.game.runetek5.toolkit.directx.DirectXWeakReferencedTexture;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;
import com.jagex.game.runetek5.utils.Utils;

import jagdx.IDirect3DDevice;
import jagdx.IDirect3DSurface;
import jagdx.IDirect3DTexture;
import jagdx.IUnknown;

public class Direct3DRectangleTexture extends TextureDirect3D implements RectangleTextureSampler {

    public Direct3DRectangleTexture(DirectXNativeToolkit class505Sub2Sub21, GLFormat class1502, GLActionFormat class763, int i_4, int i_5) {
        super(class505Sub2Sub21, class1502, class763, i_4, i_5, 1025, 0);
    }

    @Override
    public FrameBuffer getFramebuffer() {
        return new DirectXWeakReferencedTexture(this, 0);
    }

    @Override
    public int getTextureWidth() {
        return super.getTextureWidth();
    }

    @Override
    public float method47(float f_1) {
        return super.method47(f_1);
    }

    @Override
    public boolean enableMipmapping() {
        return super.enableMipmapping();
    }

    @Override
    public boolean method46() {
        return super.method46();
    }

    @Override
    public void setRepeat(boolean bool1, boolean bool2) {
        super.setRepeat(bool1, bool2);
    }

    @Override
    public void method48(int i_1, int i_2, int i_3, int i_4, int[] ints_5, int i_6, int i_7) {
        super.method48(i_1, i_2, i_3, i_4, ints_5, i_6, i_7);
    }

    @Override
    public void method52(int i_1, int i_2, int i_3, int i_4, byte[] bytes_5, GLFormat class1506, int i_7, int i_8) {
        super.method52(i_1, i_2, i_3, i_4, bytes_5, class1506, i_7, i_8);
    }

    @Override
    public void method53(int i_3, int i_4, int[] ints_5) {
        long long_7 = IDirect3DTexture.GetSurfaceLevel(super.handle, 0);
        long long_9 = IDirect3DDevice.CreateRenderTarget(super.dxToolkit.renderTarget, i_3, i_4, 21, 0, 0, true);
        if (Utils.equalOrAboveZero(IDirect3DDevice.StretchRect(super.dxToolkit.renderTarget, long_7, 0, 0, i_3, i_4, long_9, 0, 0, i_3, i_4, 1))) {
            IDirect3DSurface.Download(long_9, 0, 0, i_3, i_4, i_3 * 4, 16, super.dxToolkit.directBufferAddress);
            super.dxToolkit.directBuffer.clear();
            super.dxToolkit.directBuffer.asIntBuffer().get(ints_5);
        }

        IUnknown.Release(long_7);
        IUnknown.Release(long_9);
    }

    @Override
    public void setSamplerMode(SampleMode class351) {
        super.setSamplerMode(class351);
    }

    @Override
    public void deallocate() {
        super.deallocate();
    }

    @Override
    public int getTextureHeight() {
        return super.getTextureHeight();
    }

    @Override
    public float method62(float f_1) {
        return super.method62(f_1);
    }

    @Override
    public void bindTexture() {
        super.bindTexture();
    }
}
