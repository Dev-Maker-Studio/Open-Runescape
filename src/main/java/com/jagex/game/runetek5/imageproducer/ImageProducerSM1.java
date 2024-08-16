package com.jagex.game.runetek5.imageproducer;

import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;
import com.jagex.game.runetek5.framebuffer.SafeModeFrameBuffer;
import com.jagex.game.runetek5.framebuffer.SafeModeRenderBuffer;
import com.jagex.game.runetek5.toolkit.SafeModeToolkit;

public class ImageProducerSM1 extends DirectDisplay {

    public SafeModeFrameBuffer frameBuffer;
    public int width;
    public int height;
    public SafeModeRenderBuffer safemodeRenderBuffer;
    public final SafeModeToolkit safemodeToolkit;

    public ImageProducerSM1(SafeModeToolkit safemodeToolkit) {
        this.safemodeToolkit = safemodeToolkit;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public void setFramebuffer(int i_1, FrameBuffer handle) {
        if (i_1 != 0) {
            throw new RuntimeException();
        } 
        SafeModeFrameBuffer frameBuffer = (SafeModeFrameBuffer) handle;
        if (this.safemodeRenderBuffer != null && frameBuffer != null
        		&& (this.safemodeRenderBuffer.width != frameBuffer.width
        		|| this.safemodeRenderBuffer.height != frameBuffer.height)) {
            throw new RuntimeException();
        } 
        this.frameBuffer = frameBuffer;
        if (frameBuffer != null) {
        	this.width = frameBuffer.width;
        	this.height = frameBuffer.height;
        } else if (this.safemodeRenderBuffer == null) {
        	this.width = 0;
        	this.height = 0;
        }

        if (this == this.safemodeToolkit.getDisplay()) {
            reload();
        }
    }

    @Override
    public void setRenderBuffer(RenderBuffer renderBuffer) {
        SafeModeRenderBuffer safemodeRenderBuffer = (SafeModeRenderBuffer) renderBuffer;
        if (this.frameBuffer != null && safemodeRenderBuffer != null &&
        		(this.frameBuffer.width != safemodeRenderBuffer.width || this.frameBuffer.height != safemodeRenderBuffer.height)) {
            throw new RuntimeException();
        } 
        this.safemodeRenderBuffer = safemodeRenderBuffer;
        if (safemodeRenderBuffer != null) {
        	this.width = safemodeRenderBuffer.width;
        	this.height = safemodeRenderBuffer.height;
        } else if (this.frameBuffer == null) {
        	this.width = 0;
        	this.height = 0;
        }

        if (this == this.safemodeToolkit.getDisplay()) {
            reload();
        }
    }

    @Override
    public boolean isFramebufferComplete() {
        return true;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public boolean setDepthStencilSurface() {
        return true;
    }

    @Override
    public boolean reload() {
    	this.safemodeToolkit.bind(this.width, this.height, this.frameBuffer == null ? null : this.frameBuffer.pixels,
    			this.safemodeRenderBuffer == null ? null : this.safemodeRenderBuffer.depthBuffer);
        return true;
    }

    @Override
    public void cleanup() {
    }
}
