package com.jagex.game.runetek5.imageproducer;

import jagdx.D3DPRESENT_PARAMETERS;
import jagdx.IDirect3DDevice;
import jagdx.IDirect3DSwapChain;
import jagdx.IUnknown;

import java.awt.*;

import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit;
import com.jagex.game.runetek5.utils.Utils;

public class DirectXDelegatingDisplay extends AbstractImageProducerDX implements DirectXDisplay {

    public int backBufferWidth;
    public int backBufferHeight;
    public D3DPRESENT_PARAMETERS aD3DPRESENTPARAMETERS10480;
    public long swapChain;
    public long backBuffer;
    public long depthStencilSurface;
    public boolean reload;
    public final boolean bind;
    public final Canvas canvas;
    public final DirectXNativeToolkit toolkit;

    public DirectXDelegatingDisplay(DirectXNativeToolkit directxNativeToolkit, Canvas canvas, int width, int length, boolean bind) {
        super(directxNativeToolkit);
        this.canvas = canvas;
        this.toolkit = directxNativeToolkit;
        this.backBufferWidth = width;
        this.backBufferHeight = length;
        this.bind = bind;
        setWindowed();
        this.toolkit.method15642(this);
    }

    @Override
    public void setWindowed() {
        cleanup();
        this.aD3DPRESENTPARAMETERS10480 = new D3DPRESENT_PARAMETERS(this.canvas);
        this.aD3DPRESENTPARAMETERS10480.Windowed = true;
        this.aD3DPRESENTPARAMETERS10480.BackBufferCount = 1;
        this.aD3DPRESENTPARAMETERS10480.BackBufferWidth = this.backBufferWidth;
        this.aD3DPRESENTPARAMETERS10480.BackBufferHeight = this.backBufferHeight;
        if (this.bind) {
            this.swapChain = IDirect3DDevice.GetSwapChain(this.toolkit.renderTarget, 0);
            this.backBuffer = IDirect3DSwapChain.GetBackBuffer(this.swapChain, 0, 0);
            this.depthStencilSurface = IDirect3DDevice.GetDepthStencilSurface(this.toolkit.renderTarget);
        } else {
            if (!DirectXNativeToolkit.supports(this.toolkit.anInt10260, this.toolkit.anInt10244, this.toolkit.aLong10246, this.toolkit.anInt8709, this.aD3DPRESENTPARAMETERS10480)) {
                throw new RuntimeException();
            }

            int stencilFormat = this.aD3DPRESENTPARAMETERS10480.AutoDepthStencilFormat;
            this.swapChain = IDirect3DDevice.CreateAdditionalSwapChain(this.toolkit.renderTarget, this.aD3DPRESENTPARAMETERS10480);
            this.backBuffer = IDirect3DSwapChain.GetBackBuffer(this.swapChain, 0, 0);
            this.depthStencilSurface = IDirect3DDevice.CreateDepthStencilSurface(this.toolkit.renderTarget, this.backBufferWidth, this.backBufferHeight, stencilFormat, this.aD3DPRESENTPARAMETERS10480.MultiSampleType, this.aD3DPRESENTPARAMETERS10480.MultiSampleQuality, false);
        }

        if (this.reload) {
            reload();
        }
    }


    @Override
    public int getHeight() {
        return this.backBufferHeight;
    }

    @Override
    public boolean setDepthStencilSurface() {
        this.reload = false;
        return Utils.equalOrAboveZero(IDirect3DDevice.SetDepthStencilSurface(this.toolkit.renderTarget, 0L));
    }

    @Override
    public int draw(int x, int y) {
        return IDirect3DSwapChain.Present(this.swapChain, 0);
    }

    @Override
    public void resize(int width, int height) {
        cleanup();
        this.backBufferWidth = width;
        this.backBufferHeight = height;
        if (this.bind) {
        	this.toolkit.bindRenderTarget();
        }

        setWindowed();
        super.resize(width, height);
    }

    @Override
    public void cleanup() {
        if (this.backBuffer != 0L) {
            IUnknown.Release(this.backBuffer);
            this.backBuffer = 0L;
        }

        if (this.depthStencilSurface != 0L) {
            IUnknown.Release(this.depthStencilSurface);
            this.depthStencilSurface = 0L;
        }

        if (this.swapChain != 0L) {
            IUnknown.Release(this.swapChain);
            this.swapChain = 0L;
        }

        this.toolkit.release(this);
    }

    public void release() {
        if (this.backBuffer != 0L) {
            IUnknown.Release(this.backBuffer);
            this.backBuffer = 0L;
        }

        if (this.depthStencilSurface != 0L) {
            IUnknown.Release(this.depthStencilSurface);
            this.depthStencilSurface = 0L;
        }

        if (this.swapChain != 0L) {
            IUnknown.Release(this.swapChain);
            this.swapChain = 0L;
        }

        this.toolkit.release(this);
    }

    @Override
    protected void finalize() throws Throwable {
        release();
        super.finalize();
    }

    @Override
    public int getWidth() {
        return this.backBufferWidth;
    }

    @Override
    public int draw() {
        return IDirect3DSwapChain.Present(this.swapChain, 0);
    }

    @Override
    public boolean reload() {
        this.reload = true;
        return !Utils.belowZero(IDirect3DDevice.SetRenderTarget(this.toolkit.renderTarget, 0, this.backBuffer))
        		&& (!Utils.belowZero(IDirect3DDevice.SetDepthStencilSurface(this.toolkit.renderTarget, this.depthStencilSurface))
        				&& super.reload());
    }

}
