package com.jagex.game.runetek5.imageproducer;

import jaggl.OpenGL;

import java.awt.*;

import com.jagex.game.runetek5.toolkit.OpenGlToolkit;

public class ImageProducerGL2 extends DelegatingDisplay {

    public boolean aBool10122;
    public final OpenGlToolkit aGraphicalRendererSub110118;
    public final Canvas aCanvas10120;
    public final OpenGL anOpenGL10119;
    public final long aLong10117;
    public int anInt10121;
    public int anInt10116;

    public ImageProducerGL2(OpenGlToolkit class505Sub11, Canvas canvas2, long long_3) {
        this.aBool10122 = false;
        this.aGraphicalRendererSub110118 = class505Sub11;
        this.aCanvas10120 = canvas2;
        this.anOpenGL10119 = class505Sub11.activeOpenglInstance;
        this.aLong10117 = long_3;
        method15526();
    }

    public ImageProducerGL2(OpenGlToolkit class505Sub11, Canvas canvas2) {
        this(class505Sub11, canvas2, class505Sub11.activeOpenglInstance.prepareSurface(canvas2));
    }

    @Override
    public int getWidth() {
        return this.anInt10116;
    }

    @Override
    public int getHeight() {
        return this.anInt10121;
    }

    public void method15526() {
        Dimension dimension1 = this.aCanvas10120.getSize();
        this.anInt10121 = dimension1.height;
        this.anInt10116 = dimension1.width;
    }

    @Override
    public int draw() {
        if (this.aBool10122) {
            throw new IllegalStateException();
        } else {
            this.anOpenGL10119.swapBuffers(this.aLong10117);
            return 0;
        }
    }

    @Override
    public boolean reload() {
        if (this.aBool10122) {
            throw new IllegalStateException();
        } else {
            this.anOpenGL10119.setSurface(this.aLong10117);
            this.aGraphicalRendererSub110118.method13578();
            return true;
        }
    }

    @Override
    public boolean setDepthStencilSurface() {
        return true;
    }

    @Override
    public void cleanup() {
        if (this.aBool10122) {
            throw new IllegalStateException();
        } else {
            this.anOpenGL10119.releaseSurface(this.aCanvas10120, this.aLong10117);
            this.aBool10122 = true;
        }
    }

    @Override
    public void resize(int i_1, int i_2) {
        if (this.aBool10122) {
            throw new IllegalStateException();
        } else {
            this.anOpenGL10119.surfaceResized(this.aLong10117);
            method15526();
            if (this == this.aGraphicalRendererSub110118.getDisplay()) {
                this.aGraphicalRendererSub110118.method13639();
            }

        }
    }

    @Override
    public int draw(int i_1, int i_2) {
        return 0;
    }
}
