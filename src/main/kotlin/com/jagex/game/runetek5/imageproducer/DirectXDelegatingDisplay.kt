package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit
import com.jagex.game.runetek5.utils.Utils
import jagdx.*
import java.awt.Canvas

/**
 * DirectX display surface wrapper that manages swap chains and rendering surfaces.
 */
class DirectXDelegatingDisplay(
    val toolkit: DirectXNativeToolkit,
    val canvas: Canvas,
    backBufferWidth: Int,
    backBufferHeight: Int,
    val bind: Boolean
) : AbstractImageProducerDX(toolkit), DirectXDisplay, AutoCloseable {

    var backBufferWidth: Int = backBufferWidth
        private set
    var backBufferHeight: Int = backBufferHeight
        private set
    var aD3DPRESENTPARAMETERS10480: D3DPRESENT_PARAMETERS? = null
    var swapChain: Long = 0L
    var backBuffer: Long = 0L
    var depthStencilSurface: Long = 0L
    var reload: Boolean = false

    init {
        setWindowed()
        toolkit.method15642(this)
    }

    override fun setWindowed() {
        cleanup()
        aD3DPRESENTPARAMETERS10480 = D3DPRESENT_PARAMETERS(canvas).apply {
            Windowed = true
            BackBufferCount = 1
            BackBufferWidth = this@DirectXDelegatingDisplay.backBufferWidth
            BackBufferHeight = this@DirectXDelegatingDisplay.backBufferHeight
        }

        if (bind) {
            swapChain = IDirect3DDevice.GetSwapChain(toolkit.renderTarget, 0)
            backBuffer = IDirect3DSwapChain.GetBackBuffer(swapChain, 0, 0)
            depthStencilSurface = IDirect3DDevice.GetDepthStencilSurface(toolkit.renderTarget)
        } else {
            if (!DirectXNativeToolkit.supports(
                    toolkit.anInt10260,
                    toolkit.anInt10244,
                    toolkit.aLong10246,
                    toolkit.anInt8709,
                    aD3DPRESENTPARAMETERS10480
                )
            ) {
                throw RuntimeException()
            }

            val params = aD3DPRESENTPARAMETERS10480!!
            val stencilFormat = params.AutoDepthStencilFormat
            swapChain = IDirect3DDevice.CreateAdditionalSwapChain(toolkit.renderTarget, params)
            backBuffer = IDirect3DSwapChain.GetBackBuffer(swapChain, 0, 0)
            depthStencilSurface = IDirect3DDevice.CreateDepthStencilSurface(
                toolkit.renderTarget,
                this.backBufferWidth,
                this.backBufferHeight,
                stencilFormat,
                params.MultiSampleType,
                params.MultiSampleQuality,
                false
            )
        }

        if (reload) {
            reload()
        }
    }

    override fun getHeight(): Int = backBufferHeight

    override fun setDepthStencilSurface(): Boolean {
        reload = false
        return Utils.equalOrAboveZero(IDirect3DDevice.SetDepthStencilSurface(toolkit.renderTarget, 0L))
    }

    override fun draw(x: Int, y: Int): Int {
        return IDirect3DSwapChain.Present(swapChain, 0)
    }

    override fun resize(width: Int, height: Int) {
        cleanup()
        backBufferWidth = width
        backBufferHeight = height
        if (bind) {
            toolkit.bindRenderTarget()
        }

        setWindowed()
        super.resize(width, height)
    }

    override fun cleanup() {
        if (backBuffer != 0L) {
            IUnknown.Release(backBuffer)
            backBuffer = 0L
        }

        if (depthStencilSurface != 0L) {
            IUnknown.Release(depthStencilSurface)
            depthStencilSurface = 0L
        }

        if (swapChain != 0L) {
            IUnknown.Release(swapChain)
            swapChain = 0L
        }

        toolkit.release(this)
    }

    fun release() {
        if (backBuffer != 0L) {
            IUnknown.Release(backBuffer)
            backBuffer = 0L
        }

        if (depthStencilSurface != 0L) {
            IUnknown.Release(depthStencilSurface)
            depthStencilSurface = 0L
        }

        if (swapChain != 0L) {
            IUnknown.Release(swapChain)
            swapChain = 0L
        }

        toolkit.release(this)
    }

    override fun close() {
        release()
    }

    override fun getWidth(): Int = backBufferWidth

    override fun draw(): Int {
        return IDirect3DSwapChain.Present(swapChain, 0)
    }

    override fun reload(): Boolean {
        reload = true
        return !Utils.belowZero(IDirect3DDevice.SetRenderTarget(toolkit.renderTarget, 0, backBuffer)) &&
                !Utils.belowZero(IDirect3DDevice.SetDepthStencilSurface(toolkit.renderTarget, depthStencilSurface)) &&
                super.reload()
    }
}
