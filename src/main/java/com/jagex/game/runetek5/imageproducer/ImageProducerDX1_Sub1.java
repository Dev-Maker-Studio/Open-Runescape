package com.jagex.game.runetek5.imageproducer;

import com.jagex.game.runetek5.framebuffer.DirectXFrameBuffer;
import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;
import com.jagex.game.runetek5.toolkit.DirectXNativeToolkit;
import com.jagex.game.runetek5.toolkit.directx.DirectXFrameBufferObject;
import com.jagex.game.runetek5.utils.Utils;

import jagdx.IDirect3DDevice;

public class ImageProducerDX1_Sub1 extends AbstractImageProducerDX1 {

    public boolean useDepthstencil;
    public int flags;
    public DirectXFrameBufferObject dxFbo;
    public int height;
    public int width;
    public final DirectXFrameBuffer[] dxFramebuffers = new DirectXFrameBuffer[4];
    public final DirectXNativeToolkit dxToolkit;

    public ImageProducerDX1_Sub1(DirectXNativeToolkit dxToolkit) {
        super(dxToolkit);
        this.dxToolkit = dxToolkit;
    }

    @Override
    public boolean setDepthStencilSurface() {
        for (int i_1 = 1; i_1 < 4; i_1++) {
            DirectXFrameBuffer interface262 = this.dxFramebuffers[i_1];
            if (interface262 != null) {
                setRenderTarget(i_1, 0L);
            }
        }

        if (this.dxFbo != null) {
            setDepthstencilSurface(0L);
        }

        this.useDepthstencil = false;
        return true;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void setRenderBuffer(RenderBuffer buf) {
        DirectXFrameBufferObject fbo = (DirectXFrameBufferObject) buf;
        if (buf == null) {
            this.flags &= -17;
            this.dxFbo = null;
            if (this.useDepthstencil) {
                setDepthstencilSurface(0L);
            }

            if (this.flags == 0) {
                this.height = 0;
                this.width = 0;
            }
        } else {
            if (this.flags != 0) {
                if (this.width != fbo.getUseWidth() || this.height != fbo.getUseHeight()) {
                    throw new RuntimeException();
                }
            } else {
                this.height = fbo.getUseHeight();
                this.width = fbo.getUseWidth();
                method15562();
            }

            this.flags |= 0x10;
            this.dxFbo = fbo;
            if (this.useDepthstencil) {
                setDepthstencilSurface(fbo.getSurfaceLevel());
            }
        }

    }

    @Override
    public void setFramebuffer(int index, FrameBuffer handle) {
        int flag = 1 << index;
        DirectXFrameBuffer framebuf = (DirectXFrameBuffer) handle;
        if (handle == null) {
            this.flags &= ~flag;
            this.dxFramebuffers[index] = null;
            if (this.useDepthstencil) {
                setRenderTarget(index, 0L);
            }

            if (this.flags == 0) {
                this.height = 0;
                this.width = 0;
            }
        } else {
            if (this.flags != 0) {
                if (this.width != framebuf.getUseWidth() || this.height != framebuf.getUseHeight()) {
                    throw new RuntimeException();
                }
            } else {
                this.height = framebuf.getUseHeight();
                this.width = framebuf.getUseWidth();
                method15562();
            }

            this.flags |= flag;
            this.dxFramebuffers[index] = framebuf;
            if (this.useDepthstencil) {
                setRenderTarget(index, framebuf.getSurfaceLevel());
            }
        }

    }

    public boolean setDepthstencilSurface(long surfaceLevel) {
        return Utils.equalOrAboveZero(IDirect3DDevice.SetDepthStencilSurface(this.dxToolkit.renderTarget, surfaceLevel));
    }

    @Override
    public void cleanup() {
        if (this.dxFbo != null) {
            this.dxFbo.deallocate();
        }

        for (int i_1 = 0; i_1 < this.dxFramebuffers.length; i_1++) {
            if (this.dxFramebuffers[i_1] != null) {
                this.dxFramebuffers[i_1].deallocate();
            }
        }

    }

    public void destroy() {
        if (this.dxFbo != null) {
            this.dxFbo.destroy();
        }

        for (int i_1 = 0; i_1 < this.dxFramebuffers.length; i_1++) {
            if (this.dxFramebuffers[i_1] != null) {
                this.dxFramebuffers[i_1].destroy();
            }
        }

    }

    @Override
    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }

    public boolean setRenderTarget(int i_1, long surfaceLevel) {
        return Utils.equalOrAboveZero(IDirect3DDevice.SetRenderTarget(this.dxToolkit.renderTarget, i_1, surfaceLevel));
    }

    @Override
    public boolean reload() {
        for (int i_1 = 0; i_1 < 4; i_1++) {
            DirectXFrameBuffer frameBuffer = this.dxFramebuffers[i_1];
            if (frameBuffer != null) {
                long surfaceLevel = frameBuffer.getSurfaceLevel();
                setRenderTarget(i_1, surfaceLevel);
            }
        }

        if (this.dxFbo != null) {
            setDepthstencilSurface(this.dxFbo.getSurfaceLevel());
        }

        this.useDepthstencil = true;
        return super.reload();
    }

    @Override
    public boolean isFramebufferComplete() {
        return this.dxFramebuffers[0] != null;
    }
}
