package com.jagex.game.runetek5.framebuffer;

public class SafeModeRenderBuffer implements RenderBuffer {

    public final int width;

    public final int height;

    public final float[] depthBuffer;

    public SafeModeRenderBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        this.depthBuffer = new float[width * height];
    }

    @Override
    public int getUseWidth() {
        return this.width;
    }

    @Override
    public int getUseHeight() {
        return this.height;
    }

    @Override
    public void deallocate() {
    	// NOOP
    }

}
