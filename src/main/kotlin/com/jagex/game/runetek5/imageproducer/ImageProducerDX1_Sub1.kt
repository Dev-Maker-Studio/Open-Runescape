package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.framebuffer.DirectXFrameBuffer
import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit
import com.jagex.game.runetek5.toolkit.directx.DirectXFrameBufferObject
import com.jagex.game.runetek5.utils.Utils
import jagdx.IDirect3DDevice

class ImageProducerDX1_Sub1(
    @JvmField val dxToolkit: DirectXNativeToolkit
) : AbstractImageProducerDX1(dxToolkit), AutoCloseable {

    @JvmField var useDepthstencil: Boolean = false
    @JvmField var flags: Int = 0
    @JvmField var dxFbo: DirectXFrameBufferObject? = null
    @JvmField var height: Int = 0
    @JvmField var width: Int = 0
    @JvmField val dxFramebuffers: Array<DirectXFrameBuffer?> = arrayOfNulls(4)

    override fun setDepthStencilSurface(): Boolean {
        for (i_1 in 1 until 4) {
            val interface262 = dxFramebuffers[i_1]
            if (interface262 != null) {
                setRenderTarget(i_1, 0L)
            }
        }

        if (dxFbo != null) {
            setDepthstencilSurface(0L)
        }

        useDepthstencil = false
        return true
    }

    override fun getWidth(): Int {
        return width
    }

    override fun getHeight(): Int {
        return height
    }

    override fun setRenderBuffer(buf: RenderBuffer?) {
        val fbo = buf as DirectXFrameBufferObject?
        if (buf == null) {
            flags = flags and -17
            dxFbo = null
            if (useDepthstencil) {
                setDepthstencilSurface(0L)
            }

            if (flags == 0) {
                height = 0
                width = 0
            }
        } else {
            if (flags != 0) {
                if (width != fbo!!.getUseWidth() || height != fbo.getUseHeight()) {
                    throw RuntimeException()
                }
            } else {
                height = fbo!!.getUseHeight()
                width = fbo.getUseWidth()
                method15562()
            }

            flags = flags or 0x10
            dxFbo = fbo
            if (useDepthstencil) {
                setDepthstencilSurface(fbo!!.getSurfaceLevel())
            }
        }
    }

    override fun setFramebuffer(index: Int, handle: FrameBuffer?) {
        val flag = 1 shl index
        val framebuf = handle as DirectXFrameBuffer?
        if (handle == null) {
            flags = flags and flag.inv()
            dxFramebuffers[index] = null
            if (useDepthstencil) {
                setRenderTarget(index, 0L)
            }

            if (flags == 0) {
                height = 0
                width = 0
            }
        } else {
            if (flags != 0) {
                if (width != framebuf!!.getUseWidth() || height != framebuf.getUseHeight()) {
                    throw RuntimeException()
                }
            } else {
                height = framebuf!!.getUseHeight()
                width = framebuf.getUseWidth()
                method15562()
            }

            flags = flags or flag
            dxFramebuffers[index] = framebuf
            if (useDepthstencil) {
                setRenderTarget(index, framebuf!!.getSurfaceLevel())
            }
        }
    }

    fun setDepthstencilSurface(surfaceLevel: Long): Boolean {
        return Utils.equalOrAboveZero(IDirect3DDevice.SetDepthStencilSurface(dxToolkit.renderTarget, surfaceLevel))
    }

    override fun cleanup() {
        if (dxFbo != null) {
            dxFbo!!.deallocate()
        }

        for (i_1 in dxFramebuffers.indices) {
            if (dxFramebuffers[i_1] != null) {
                dxFramebuffers[i_1]!!.deallocate()
            }
        }
    }

    fun destroy() {
        if (dxFbo != null) {
            dxFbo!!.destroy()
        }

        for (i_1 in dxFramebuffers.indices) {
            if (dxFramebuffers[i_1] != null) {
                dxFramebuffers[i_1]!!.destroy()
            }
        }
    }

    override fun close() {
        destroy()
    }

    fun setRenderTarget(i_1: Int, surfaceLevel: Long): Boolean {
        return Utils.equalOrAboveZero(IDirect3DDevice.SetRenderTarget(dxToolkit.renderTarget, i_1, surfaceLevel))
    }

    override fun reload(): Boolean {
        for (i_1 in 0 until 4) {
            val frameBuffer = dxFramebuffers[i_1]
            if (frameBuffer != null) {
                val surfaceLevel = frameBuffer.getSurfaceLevel()
                setRenderTarget(i_1, surfaceLevel)
            }
        }

        if (dxFbo != null) {
            setDepthstencilSurface(dxFbo!!.getSurfaceLevel())
        }

        useDepthstencil = true
        return super.reload()
    }

    override fun isFramebufferComplete(): Boolean {
        return dxFramebuffers[0] != null
    }
}
