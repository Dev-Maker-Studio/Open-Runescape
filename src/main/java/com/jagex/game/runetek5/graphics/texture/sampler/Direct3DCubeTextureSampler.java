package com.jagex.game.runetek5.graphics.texture.sampler;

import jagdx.IDirect3DCubeTexture;
import jagdx.IDirect3DDevice;

import java.nio.ByteBuffer;

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

public class Direct3DCubeTextureSampler extends Direct3DTextureSampler implements CubeTextureSampler {

    public final int anInt9200;

    public Direct3DCubeTextureSampler(DirectXNativeToolkit class505Sub2Sub21, int i_2, boolean bool3, int[][] ints_4) {
        super(class505Sub2Sub21, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, bool3 && class505Sub2Sub21.IS_NON_POW2);
        this.anInt9200 = i_2;
        if (super.aBool4166) {
            super.handle = IDirect3DDevice.CreateCubeTexture(super.dxToolkit.renderTarget, this.anInt9200, 0, 1024, 21, 1);
        } else {
            super.handle = IDirect3DDevice.CreateCubeTexture(super.dxToolkit.renderTarget, this.anInt9200, 1, 0, 21, 1);
        }

        ByteBuffer bytebuffer5 = super.dxToolkit.directBuffer;

        for (int i_6 = 0; i_6 < 6; i_6++) {
            bytebuffer5.clear();
            bytebuffer5.asIntBuffer().put(ints_4[i_6]);
            IDirect3DCubeTexture.Upload(super.handle, i_6, 0, 0, 0, this.anInt9200, this.anInt9200, this.anInt9200 * 4, 0, super.dxToolkit.directBufferAddress);
        }
    }

    @Override
    public void deallocate() {
        super.deallocate();
    }

    @Override
    public void setSamplerMode(SampleMode mode) {
        super.setSamplerMode(mode);
    }

    @Override
    public void bindTexture() {
        super.dxToolkit.method15662(this);
    }
}
