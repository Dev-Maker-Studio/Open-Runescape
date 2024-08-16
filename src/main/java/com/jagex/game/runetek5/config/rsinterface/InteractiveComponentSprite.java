package com.jagex.game.runetek5.config.rsinterface;

/**
 * 
 * @author Steven - 15/10/2023
 *
 */
public class InteractiveComponentSprite {

    public final int width;
    public final int height;
    public final int[] rowWidth;
    public final int[] rowStartX;
    public final ComponentSurface surface;
    public final int spriteId;

    public InteractiveComponentSprite(int width, int height, int[] rowWidth, int[] rowStartX, ComponentSurface surface, int spriteId) {
        this.width = width;
        this.height = height;
        this.rowWidth = rowWidth;
        this.rowStartX = rowStartX;
        this.surface = surface;
        this.spriteId = spriteId;
    }

    public boolean withinBounds(int x, int y) {
        if (y >= 0 && y < this.rowStartX.length) {
            int startX = this.rowStartX[y];
            return x >= startX && x <= startX + this.rowWidth[y];
        }
        return false;
    }

}
