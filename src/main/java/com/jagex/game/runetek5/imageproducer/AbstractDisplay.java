package com.jagex.game.runetek5.imageproducer;

public abstract class AbstractDisplay {

    public abstract int getWidth();

    public abstract int getHeight();

    public abstract void cleanup();

    public abstract boolean setDepthStencilSurface();

    public abstract boolean reload();
}
