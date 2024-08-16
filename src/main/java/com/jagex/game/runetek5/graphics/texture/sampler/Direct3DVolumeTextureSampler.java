package com.jagex.game.runetek5.graphics.texture.sampler;

import jagdx.IDirect3DDevice;
import jagdx.IDirect3DVolumeTexture;

import java.nio.ByteBuffer;

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

public class Direct3DVolumeTextureSampler extends Direct3DTextureSampler implements Texture3DSampler {

    public final int anInt9123;
    public final int anInt9121;
    public final int anInt9122;

    public Direct3DVolumeTextureSampler(DirectXNativeToolkit class505Sub2Sub21, GLFormat class1502, byte[] bytes_7) {
        super(class505Sub2Sub21, class1502, GLActionFormat.PIXEL_FORMAT, false);
        this.anInt9123 = 128;
        this.anInt9121 = 128;
        this.anInt9122 = 16;
        super.handle = IDirect3DDevice.CreateVolumeTexture(super.dxToolkit.renderTarget, 128, 128, 16, 1, 0, DirectXNativeToolkit.method15652(class1502, super.glactionFormat), 1);
        ByteBuffer bytebuffer8 = super.dxToolkit.directBuffer;
        bytebuffer8.clear();
        bytebuffer8.put(bytes_7);
        IDirect3DVolumeTexture.Upload(super.handle, 0, 0, 0, 0, this.anInt9123, this.anInt9121, this.anInt9122, class1502.bytesPerPixel * this.anInt9123, 0, super.dxToolkit.directBufferAddress);
    }

    @Override
    public void deallocate() {
        super.deallocate();
    }

    @Override
    public long getHandle() {
        return super.handle;
    }

    @Override
    public void bindTexture() {
        super.dxToolkit.method15657(this);
    }
    
    @Override
    public void setSamplerMode(SampleMode class351) {
        super.setSamplerMode(class351);
    }
}
