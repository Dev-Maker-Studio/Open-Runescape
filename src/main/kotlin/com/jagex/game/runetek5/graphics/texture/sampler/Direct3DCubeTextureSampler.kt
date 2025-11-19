package com.jagex.game.runetek5.graphics.texture.sampler

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import jagdx.IDirect3DCubeTexture
import jagdx.IDirect3DDevice

class Direct3DCubeTextureSampler(
    class505Sub2Sub21: DirectXNativeToolkit,
    i_2: Int,
    bool3: Boolean,
    ints_4: Array<IntArray>
) : Direct3DTextureSampler(class505Sub2Sub21, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, bool3 && class505Sub2Sub21.IS_NON_POW2), CubeTextureSampler {

    @JvmField val anInt9200: Int

    init {
        this.anInt9200 = i_2
        if (super.aBool4166) {
            super.handle = IDirect3DDevice.CreateCubeTexture(super.dxToolkit.renderTarget, this.anInt9200, 0, 1024, 21, 1)
        } else {
            super.handle = IDirect3DDevice.CreateCubeTexture(super.dxToolkit.renderTarget, this.anInt9200, 1, 0, 21, 1)
        }

        val bytebuffer5 = super.dxToolkit.directBuffer

        for (i_6 in 0 until 6) {
            bytebuffer5.clear()
            bytebuffer5.asIntBuffer().put(ints_4[i_6])
            IDirect3DCubeTexture.Upload(super.handle, i_6, 0, 0, 0, this.anInt9200, this.anInt9200, this.anInt9200 * 4, 0, super.dxToolkit.directBufferAddress)
        }
    }

    override fun deallocate() {
        super.deallocate()
    }

    override fun setSamplerMode(mode: SampleMode) {
        super.setSamplerMode(mode)
    }

    override fun bindTexture() {
        super.dxToolkit.method15662(this)
    }
}
