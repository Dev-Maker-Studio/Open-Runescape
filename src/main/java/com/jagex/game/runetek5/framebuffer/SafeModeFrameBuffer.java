package com.jagex.game.runetek5.framebuffer;

public class SafeModeFrameBuffer implements FrameBuffer {

	public final int width;

    public final int height;

    public final int[] pixels;

    public SafeModeFrameBuffer(int width, int height, int[] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
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
