package com.jagex.game.runetek5.toolkit.directx

import com.jagex.game.runetek5.framebuffer.DirectXFrameBuffer
import com.jagex.game.runetek5.graphics.texture.Direct3DRectangleTexture
import jagdx.IDirect3DTexture
import jagdx.IUnknown

/**
 * DirectX texture with weak reference for automatic cleanup.
 *
 * @author Brainpower - 02/05/2022
 */
class DirectXWeakReferencedTexture(
    @JvmField val texture: Direct3DRectangleTexture,
    @JvmField val level: Int
) : DirectXFrameBuffer, DirectXFrameBufferObject, AutoCloseable {

    @JvmField var surfaceLevel: Long = 0L

    init {
        texture.dxToolkit.addWeakReference(this)
    }

    override fun getUseWidth(): Int {
        return texture.getTextureWidth()
    }

    override fun getSurfaceLevel(): Long {
        if (surfaceLevel == 0L) {
            surfaceLevel = IDirect3DTexture.GetSurfaceLevel(texture.handle, level)
        }
        return surfaceLevel
    }

    override fun getUseHeight(): Int {
        return texture.getTextureHeight()
    }

    override fun destroy() {
        if (surfaceLevel != 0L) {
            texture.dxToolkit.requestHandleCleanup(surfaceLevel)
            surfaceLevel = 0L
        }
    }

    override fun close() {
        destroy()
    }

    override fun deallocate() {
        if (surfaceLevel != 0L) {
            IUnknown.Release(surfaceLevel)
            surfaceLevel = 0L
        }
        texture.dxToolkit.clearReference(this)
    }
}
