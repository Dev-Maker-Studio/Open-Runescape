package com.jagex.game.runetek5.buffer

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit
import com.jagex.game.runetek5.toolkit.directx.DirectXFrameBufferObject
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import jagdx.IDirect3DDevice
import jagdx.IUnknown

/**
 * DirectX native render buffer implementation.
 */
class DirectXNativeRenderBuffer(
    private val toolkit: DirectXNativeToolkit,
    private val format: GLActionFormat,
    private val width: Int,
    private val height: Int
) : DirectXFrameBufferObject, AutoCloseable {

    @JvmField var surfaceHandle: Long = 0L

    init {
        surfaceHandle = IDirect3DDevice.CreateDepthStencilSurface(
            toolkit.renderTarget,
            width,
            height,
            getDepthStencilFormat(format),
            0,
            0,
            false
        )
        toolkit.addWeakReference(this)
    }

    override fun getUseWidth(): Int {
        return width
    }

    override fun getUseHeight(): Int {
        return height
    }

    override fun getSurfaceLevel(): Long {
        return surfaceHandle
    }

    override fun deallocate() {
        if (surfaceHandle != 0L) {
            IUnknown.Release(surfaceHandle)
            surfaceHandle = 0L
        }
        toolkit.clearReference(this)
    }

    override fun close() {
        destroy()
    }

    override fun destroy() {
        if (surfaceHandle != 0L) {
            toolkit.requestHandleCleanup(surfaceHandle)
            surfaceHandle = 0L
        }
    }

    companion object {
        @JvmStatic
        fun getDepthStencilFormat(format: GLActionFormat): Int {
            return when (format) {
                GLActionFormat.TEXTURE_FORMAT -> 80
                GLActionFormat.DEPTH_COMPONENT_24 -> 77
                else -> throw IllegalArgumentException("")
            }
        }
    }
}
