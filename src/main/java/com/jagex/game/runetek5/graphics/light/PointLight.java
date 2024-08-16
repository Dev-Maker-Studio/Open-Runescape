package com.jagex.game.runetek5.graphics.light;

import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.math.Vector3;

public class PointLight extends Node<PointLight> {

    public final Vector3 position = new Vector3();
    public final int size2d; // Distance?
    public final int color;
    public float transparency;

    public PointLight(int x, int y, int z, int size2d, int color) {
        this.position.set(x, y, z);
        this.size2d = size2d;
        this.color = color;
        this.transparency = (float) 1.0;
    }

    public int getX() {
        return (int) this.position.x;
    }

    public int getY() {
        return (int) this.position.y;
    }

    public int getSize2d() {
        return this.size2d;
    }

    public int getColor() {
        return this.color;
    }

    public void setTransparency(float transparency) {
        this.transparency = transparency;
    }

    public void setPosition(int x, int y, int z) {
        this.position.set(x, y, z);
    }

    public int getZ() {
        return (int) this.position.z;
    }

    public float getAlpha() {
        return this.transparency;
    }

}
