package com.jagex.game.runetek5.client.window;

public class WindowSettings {

    public final int width;
    public final int height;
    public final int borderWidth;
    public final int borderHeight;
    public final String name;

    public WindowSettings(int width, int height, int borderWidth, int borderHeight, String name) {
        this.width = width;
        this.height = height;
        this.borderWidth = borderWidth;
        this.borderHeight = borderHeight;
        this.name = name;
    }

    public int borderWidth() {
        return this.borderWidth;
    }

    public String getName() {
        return this.name;
    }

    public int getWidth() {
        return this.width;
    }

    public int borderHeight() {
        return this.borderHeight;
    }

    public int getHeight() {
        return this.height;
    }
}
