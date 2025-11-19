package com.jagex.game.runetek5.graphics.texture.sampler

import jagdx.IDirect3DDevice
import jagdx.IDirect3DVolumeTexture
import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat

class Direct3DVolumeTextureSampler(
    class505Sub2Sub21: DirectXNativeToolkit,
    class1502: GLFormat,
    bytes_7: ByteArray
) : Direct3DTextureSampler(class505Sub2Sub21, class1502, GLActionFormat.PIXEL_FORMAT, false), Texture3DSampler {

    @JvmField val anInt9123: Int
    @JvmField val anInt9121: Int
    @JvmField val anInt9122: Int

    init {
        this.anInt9123 = 128
        this.anInt9121 = 128
        this.anInt9122 = 16
        super.handle = IDirect3DDevice.CreateVolumeTexture(super.dxToolkit.renderTarget, 128, 128, 16, 1, 0, DirectXNativeToolkit.method15652(class1502, super.glactionFormat), 1)
        val bytebuffer8 = super.dxToolkit.directBuffer
        bytebuffer8.clear()
        bytebuffer8.put(bytes_7)
        IDirect3DVolumeTexture.Upload(super.handle, 0, 0, 0, this.anInt9123, this.anInt9121, this.anInt9122, super.dxToolkit.directBufferAddress, class1502.bytesPerPixel * this.anInt9123, 0, super.handle)
    }

    override fun deallocate() {
        super.deallocate()
    }

    override fun getHandle(): Long {
        return super.handle
    }

    override fun bindTexture() {
        super.dxToolkit.method15657(this)
    }

    override fun setSamplerMode(class351: SampleMode) {
        super.setSamplerMode(class351)
    }
}
