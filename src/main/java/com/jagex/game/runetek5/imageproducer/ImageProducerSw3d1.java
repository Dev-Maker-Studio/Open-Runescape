package com.jagex.game.runetek5.imageproducer;

import com.jagex.game.runetek5.framebuffer.FrameBuffer;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;
import com.jagex.game.runetek5.framebuffer.SoftwareRenderBuffer;
import com.jagex.game.runetek5.graphics.sprite.SpriteSw3d;
import com.jagex.game.runetek5.toolkit.SoftwareToolkit;

public class ImageProducerSw3d1 extends DirectDisplay {

    public SoftwareRenderBuffer anoa9448;
    public int anInt9446;
    public int anInt9450;
    public SpriteSw3d afa9447;
    public final SoftwareToolkit aja9449;

    public ImageProducerSw3d1(SoftwareToolkit ja1) {
        this.aja9449 = ja1;
    }

    @Override
    public int getHeight() {
        return this.anInt9450;
    }

    @Override
    public boolean reload() {
        this.aja9449.d(this.afa9447 == null ? 0L : this.afa9447.nativeid, this.anoa9448 == null ? 0L : this.anoa9448.nativeid);
        return true;
    }

    @Override
    public void setFramebuffer(int i_1, FrameBuffer interface92) {
        if (i_1 != 0) {
            throw new RuntimeException();
        } else if (this.anoa9448 != null && interface92 != null && (this.anoa9448.getUseWidth() != interface92.getUseWidth() || this.anoa9448.getUseHeight() != interface92.getUseHeight())) {
            throw new RuntimeException();
        } else {
            this.afa9447 = (SpriteSw3d) interface92;
            if (interface92 != null) {
                this.anInt9446 = interface92.getUseWidth();
                this.anInt9450 = interface92.getUseHeight();
            } else if (this.anoa9448 == null) {
                this.anInt9446 = 0;
                this.anInt9450 = 0;
            }

            if (this.aja9449.getDisplay() == this) {
                reload();
            }

        }
    }

    @Override
    public boolean setDepthStencilSurface() {
        return true;
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void setRenderBuffer(RenderBuffer interface81) {
        if (this.afa9447 != null && interface81 != null && (this.afa9447.getUseWidth() != interface81.getUseWidth() || this.afa9447.getUseHeight() != interface81.getUseHeight())) {
            throw new RuntimeException();
        } else {
            this.anoa9448 = (SoftwareRenderBuffer) interface81;
            if (interface81 != null) {
                this.anInt9446 = interface81.getUseWidth();
                this.anInt9450 = interface81.getUseHeight();
            } else if (this.afa9447 == null) {
                this.anInt9446 = 0;
                this.anInt9450 = 0;
            }

            if (this.aja9449.getDisplay() == this) {
                reload();
            }

        }
    }

    @Override
    public boolean isFramebufferComplete() {
        return true;
    }

    @Override
    public int getWidth() {
        return this.anInt9446;
    }

}
