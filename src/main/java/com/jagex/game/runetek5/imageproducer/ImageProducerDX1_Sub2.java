package com.jagex.game.runetek5.imageproducer;

import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;
import com.jagex.game.runetek5.graphics.texture.TextureFramebufferGenerator;
import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit;

import jaggl.OpenGL;

public class ImageProducerDX1_Sub2 extends AbstractImageProducerDX1 implements AutoCloseable {

    public final TextureFramebufferGenerator[] anInterface28Array10449 = new TextureFramebufferGenerator[4];
    public final OpenGLNativeToolkit aGraphicalRendererSub2Sub110446;
    public int anInt10443;
    public int anInt10444;
    public TextureFramebufferGenerator anInterface2810448;
    public int anInt10445;
    public int anInt10441;
    public int anInt10447;

    public ImageProducerDX1_Sub2(OpenGLNativeToolkit class505Sub2Sub11) {
        super(class505Sub2Sub11);
        this.aGraphicalRendererSub2Sub110446 = class505Sub2Sub11;
        int[] ints_2 = new int[1];
        OpenGL.glGenFramebuffersEXT(1, ints_2, 0);
        this.anInt10443 = ints_2[0];
    }

    @Override
    public int getWidth() {
        return this.anInt10441;
    }

    @Override
    public void setRenderBuffer(RenderBuffer interface81) {
        TextureFramebufferGenerator interface282 = (TextureFramebufferGenerator) interface81;
        if (interface81 == null) {
            this.anInt10444 &= -17;
            this.anInterface2810448 = null;
            if (this.anInt10444 == 0) {
                this.anInt10445 = 0;
                this.anInt10441 = 0;
            }
        } else {
            if (this.anInt10444 != 0) {
                if (this.anInt10441 != interface282.getUseWidth() || this.anInt10445 != interface282.getUseHeight()) {
                    throw new RuntimeException();
                }
            } else {
                this.anInt10445 = interface282.getUseHeight();
                this.anInt10441 = interface282.getUseWidth();
                method15562();
            }

            this.anInt10444 |= 0x10;
            this.anInterface2810448 = interface282;
        }

        if (this == this.aGraphicalRendererSub2Sub110446.getDisplay()) {
            method15982();
        } else {
            this.anInt10447 |= 0x10;
        }

    }

    public void method15981(int i_1) {
        TextureFramebufferGenerator interface282 = this.anInterface28Array10449[i_1];
        if (interface282 == null) {
            OpenGL.glFramebufferRenderbufferEXT(36160, i_1 + 36064, 36161, 0);
        } else {
            interface282.generateTextureFramebuffer(i_1 + 36064);
        }

    }

    public void method15982() {
        if (this.anInterface2810448 == null) {
            OpenGL.glFramebufferRenderbufferEXT(36160, 36096, 36161, 0);
        } else {
            this.anInterface2810448.generateTextureFramebuffer(36096);
        }

    }

    @Override
    public boolean reload() {
        OpenGL.glBindFramebufferEXT(36160, this.anInt10443);

        for (int i_1 = 0; i_1 < 4; i_1++) {
            if ((this.anInt10447 & 1 << i_1) != 0) {
                method15981(i_1);
            }
        }

        if ((this.anInt10447 & 0x10) != 0) {
            method15982();
        }

        this.anInt10447 = 0;
        return super.reload();
    }

    @Override
    public void cleanup() {
        if (this.anInt10443 != 0) {
            this.aGraphicalRendererSub2Sub110446.requestFrameBufferDeletion(this.anInt10443);
            this.anInt10443 = 0;
        }

    }

    @Override
    public void close() {
        cleanup();
    }

    @Override
    public boolean isFramebufferComplete() {
        int i_1 = OpenGL.glCheckFramebufferStatusEXT(36160);
        return i_1 == 36053;
    }

    @Override
    public void setFramebuffer(int i_1, FrameBuffer interface92) {
        int i_3 = 1 << i_1;
        TextureFramebufferGenerator interface284 = (TextureFramebufferGenerator) interface92;
        if (interface92 == null) {
            this.anInt10444 &= ~i_3;
            this.anInterface28Array10449[i_1] = null;
            if (this.anInt10444 == 0) {
                this.anInt10445 = 0;
                this.anInt10441 = 0;
            }
        } else {
            if (this.anInt10444 != 0) {
                if (this.anInt10441 != interface284.getUseWidth() || this.anInt10445 != interface284.getUseHeight()) {
                    throw new RuntimeException();
                }
            } else {
                this.anInt10445 = interface284.getUseHeight();
                this.anInt10441 = interface284.getUseWidth();
                method15562();
            }

            this.anInt10444 |= i_3;
            this.anInterface28Array10449[i_1] = interface284;
        }

        if (this == this.aGraphicalRendererSub2Sub110446.getDisplay()) {
            method15981(i_1);
        } else {
            this.anInt10447 |= i_3;
        }
    }

    @Override
    public int getHeight() {
        return this.anInt10445;
    }

    @Override
    public boolean setDepthStencilSurface() {
        OpenGL.glBindFramebufferEXT(36160, 0);
        return true;
    }
}
