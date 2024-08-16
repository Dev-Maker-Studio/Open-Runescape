package com.jagex.game.runetek5.imageproducer;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;

import static com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*;

import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.framebuffer.NativeRenderBuffer;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;

import jaggl.OpenGL;

/**
 * 
 * @author Brainpower - 22/04/2022
 *
 */
public class IntegratedGraphicsDisplayGL extends DirectDisplay {

    public int handle;
    public int occupiedSlots;
    public int renderBufferFlags;
    public int height;
    public int width;
    public final NativeRenderBuffer[] colorBuffers = new NativeRenderBuffer[4];
    public OpenGlToolkit toolkit;
    public NativeRenderBuffer depthBuffer;

    public IntegratedGraphicsDisplayGL(OpenGlToolkit toolkit) {
        if (!toolkit.intelGpu) {
            throw new IllegalStateException("");
        } 
        this.toolkit = toolkit;
        int[] buffer = new int[1];
        OpenGL.glGenFramebuffersEXT(1, buffer, 0);
        this.handle = buffer[0];
    }

    @Override
    public boolean reload() {
        OpenGL.glBindFramebufferEXT(GL_FRAMEBUFFER, this.handle);
        for (int slot = 0; slot < 4; slot++) {
            if ((this.occupiedSlots & 1 << slot) != 0) {
                createColorBuffer(slot);
            }
        }
        if ((this.occupiedSlots & 0x10) != 0) {
            createDepthBuffer();
        }
        this.occupiedSlots = 0;
        this.toolkit.method13578();
        return true;
    }

    @Override
    public void setFramebuffer(int slot, FrameBuffer nativeHandle) {
        int bitFlag = 1 << slot;
        NativeRenderBuffer renderBuffer = (NativeRenderBuffer) nativeHandle;
        if (nativeHandle == null) {
        	this.renderBufferFlags &= ~bitFlag;
            this.colorBuffers[slot] = null;
            if (this.renderBufferFlags == 0) {
            	this.height = 0;
                this.width = 0;
            }
        } else {
            if (bitFlag != (this.renderBufferFlags | bitFlag)) {
                if (this.width != renderBuffer.getUseWidth() || this.height != renderBuffer.getUseHeight()) {
                    throw new RuntimeException();
                }
            } else {
            	this.height = renderBuffer.getUseHeight();
                this.width = renderBuffer.getUseWidth();
                if (this == this.toolkit.getDisplay()) {
                	this.toolkit.method13639();
                }
            }

            this.renderBufferFlags |= bitFlag;
            this.colorBuffers[slot] = renderBuffer;
        }

        if (this == this.toolkit.getDisplay()) {
            createColorBuffer(slot);
        } else {
        	this.occupiedSlots |= bitFlag;
        }
    }
    
    public void createDepthBuffer() {
        if (this.depthBuffer == null) {
            OpenGL.glFramebufferRenderbufferEXT(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, 0);
        } else {
        	this.depthBuffer.getFrameRenderBuffer(GL_DEPTH_ATTACHMENT);
        }
    }

    public void createColorBuffer(int colorAttachmentSlot) {
        NativeRenderBuffer colorBuffer = this.colorBuffers[colorAttachmentSlot];
        if (colorBuffer == null) {
            OpenGL.glFramebufferRenderbufferEXT(GL_FRAMEBUFFER, colorAttachmentSlot + GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, 0);
        } else {
            colorBuffer.getFrameRenderBuffer(colorAttachmentSlot + GL_COLOR_ATTACHMENT0);
        }
    }

    @Override
    public boolean isFramebufferComplete() {
        int status = OpenGL.glCheckFramebufferStatusEXT(GL_FRAMEBUFFER);
        return status == GL_FRAMEBUFFER_COMPLETE;
    }

    public void bindFrameBuffer(int i_3, int i_4, boolean bool8) {
        int height = this.height;
        int displayHeight = this.toolkit.getDisplay().getHeight();
        int flags = 0;
        if (bool8) {
            flags |= 0x100;
        }

        flags |= 0x4000;

        OpenGL.glBindFramebufferEXT(GL_READ_FRAMEBUFFER, this.handle);
        OpenGL.glBlitFramebufferEXT(0, height - i_4, i_3, height, 0, displayHeight - i_4, i_3, displayHeight, flags, GL_NEAREST);
        OpenGL.glBindFramebufferEXT(GL_READ_FRAMEBUFFER, 0);

    }

    @Override
    public boolean setDepthStencilSurface() {
        OpenGL.glBindFramebufferEXT(GL_FRAMEBUFFER, 0);
        return true;
    }

    @Override
    public void cleanup() {
        if (this.handle != 0) {
        	this.toolkit.requestFrameBufferCleanup(this.handle);
            this.handle = 0;
        }
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void setRenderBuffer(RenderBuffer renderBuffer) {
        NativeRenderBuffer nativeRenderBuffer = (NativeRenderBuffer) renderBuffer;
        if (renderBuffer == null) {
        	this.renderBufferFlags &= -17;
            this.depthBuffer = null;
            if (this.renderBufferFlags == 0) {
            	this.height = 0;
            	this.width = 0;
            }
        } else {
            if ((this.renderBufferFlags | 0x10) != 16) {
                if (this.width != nativeRenderBuffer.getUseWidth() || this.height != nativeRenderBuffer.getUseHeight()) {
                    throw new RuntimeException();
                }
            } else {
            	this.height = nativeRenderBuffer.getUseHeight();
            	this.width = nativeRenderBuffer.getUseWidth();
                if (this == this.toolkit.getDisplay()) {
                	this.toolkit.method13639();
                }
            }

            this.renderBufferFlags |= 0x10;
            this.depthBuffer = nativeRenderBuffer;
        }

        if (this == this.toolkit.getDisplay()) {
            createDepthBuffer();
        } else {
        	this.occupiedSlots |= 0x10;
        }
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    public void drawBuffer(int colorAttachmentSlot) {
        OpenGL.glDrawBuffer(colorAttachmentSlot + GL_COLOR_ATTACHMENT0);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        cleanup();
    }
}