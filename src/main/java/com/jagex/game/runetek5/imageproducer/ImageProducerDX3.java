package com.jagex.game.runetek5.imageproducer;

import jaggl.OpenGL;

import java.awt.*;

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit;

public class ImageProducerDX3 extends AbstractImageProducerDX {

    public final Canvas aCanvas10302;
    public final OpenGL anOpenGL10299;
    public final long aLong10300;
    public int anInt10298;
    public int anInt10301;

    public ImageProducerDX3(OpenGLNativeToolkit class505Sub2Sub11, Canvas canvas2, long long_3) {
        super(class505Sub2Sub11);
        this.aCanvas10302 = canvas2;
        this.anOpenGL10299 = class505Sub2Sub11.opengl;
        this.aLong10300 = long_3;
        method15723();
    }

    public ImageProducerDX3(OpenGLNativeToolkit class505Sub2Sub11, Canvas canvas2) {
        this(class505Sub2Sub11, canvas2, class505Sub2Sub11.opengl.prepareSurface(canvas2));
    }

    @Override
    public int getHeight() {
        return this.anInt10298;
    }

    public void method15723() {
        Dimension dimension1 = this.aCanvas10302.getSize();
        this.anInt10298 = dimension1.height;
        this.anInt10301 = dimension1.width;
    }

    @Override
    public int draw() {
        this.anOpenGL10299.swapBuffers(this.aLong10300);
        return 0;
    }

    @Override
    public int draw(int i_1, int i_2) {
        return 0;
    }

    @Override
    public boolean reload() {
        return this.anOpenGL10299.setSurface(this.aLong10300) && super.reload();
    }

    @Override
    public void cleanup() {
        this.anOpenGL10299.releaseSurface(this.aCanvas10302, this.aLong10300);
    }

    @Override
    public void resize(int i_1, int i_2) {
        this.anOpenGL10299.surfaceResized(this.aLong10300);
        method15723();
        super.resize(i_1, i_2);
    }

    @Override
    public boolean setDepthStencilSurface() {
        return true;
    }

    @Override
    public int getWidth() {
        return this.anInt10301;
    }
}
