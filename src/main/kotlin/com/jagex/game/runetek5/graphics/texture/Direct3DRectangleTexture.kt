package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.graphics.texture.sampler.RectangleTextureSampler
import com.jagex.game.runetek5.graphics.texture.sampler.SampleMode
import com.jagex.game.runetek5.graphics.texture.sampler.TextureDirect3D
import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit
import com.jagex.game.runetek5.toolkit.directx.DirectXWeakReferencedTexture
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.utils.Utils
import jagdx.IDirect3DDevice
import jagdx.IDirect3DSurface
import jagdx.IDirect3DTexture
import jagdx.IUnknown

class Direct3DRectangleTexture(
    dxToolkit: DirectXNativeToolkit,
    format: GLFormat,
    actionFormat: GLActionFormat,
    width: Int,
    height: Int
) : TextureDirect3D(dxToolkit, format, actionFormat, width, height, 1025, 0), RectangleTextureSampler {

    override fun getFramebuffer(): FrameBuffer {
        return DirectXWeakReferencedTexture(this, 0)
    }

    override fun getTextureWidth(): Int {
        return super.getTextureWidth()
    }

    override fun method47(value: Float): Float {
        return super.method47(value)
    }

    override fun enableMipmapping(): Boolean {
        return super.enableMipmapping()
    }

    override fun method46(): Boolean {
        return super.method46()
    }

    override fun setRepeat(repeatX: Boolean, repeatY: Boolean) {
        super.setRepeat(repeatX, repeatY)
    }

    override fun method48(x: Int, y: Int, width: Int, height: Int, pixels: IntArray, offset: Int, stride: Int) {
        super.method48(x, y, width, height, pixels, offset, stride)
    }

    override fun method52(x: Int, y: Int, width: Int, height: Int, data: ByteArray, format: GLFormat, offset: Int, stride: Int) {
        super.method52(x, y, width, height, data, format, offset, stride)
    }

    override fun method53(width: Int, height: Int, pixels: IntArray) {
        val surfaceHandle = IDirect3DTexture.GetSurfaceLevel(handle, 0)
        val renderTargetHandle = IDirect3DDevice.CreateRenderTarget(dxToolkit.renderTarget, width, height, 21, 0, 0, true)
        if (Utils.equalOrAboveZero(IDirect3DDevice.StretchRect(dxToolkit.renderTarget, surfaceHandle, 0, 0, width, height, renderTargetHandle, 0, 0, width, height, 1))) {
            IDirect3DSurface.Download(renderTargetHandle, 0, 0, width, height, width * 4, 16, dxToolkit.directBufferAddress)
            dxToolkit.directBuffer.clear()
            dxToolkit.directBuffer.asIntBuffer().get(pixels)
        }

        IUnknown.Release(surfaceHandle)
        IUnknown.Release(renderTargetHandle)
    }

    override fun setSamplerMode(samplerMode: SampleMode) {
        super.setSamplerMode(samplerMode)
    }

    override fun deallocate() {
        super.deallocate()
    }

    override fun getTextureHeight(): Int {
        return super.getTextureHeight()
    }

    override fun method62(value: Float): Float {
        return super.method62(value)
    }

    override fun bindTexture() {
        super.bindTexture()
    }
}
