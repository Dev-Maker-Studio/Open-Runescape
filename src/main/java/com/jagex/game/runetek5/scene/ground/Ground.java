package com.jagex.game.runetek5.scene.ground;

import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.scene.tile.WaterTile;

public abstract class Ground {

    public final int width;

    public final int length;

    public final int tileUnits;

    public final int tileScale;

    public final int[][] tileHeights;

    Ground(int width, int length, int scaling, int[][] tileHeights) {
        this.width = width;
        this.length = length;
        int scale;
        for (scale = 0; scaling > 1; scaling >>= 1) {
            scale++;
        }
        this.tileUnits = 1 << scale;
        this.tileScale = scale;
        this.tileHeights = tileHeights;
    }   

    public abstract void setTileShadow(int tileX, int tileY, int displacement);

    public abstract void addBlendedTile(int tileX, int tileY, int[] offsetsX, int[] var4, int[] offsetsZ, int[] var6, int[] colors, int[] blendColors, int[] waterMaterials, int[] waterScaling, WaterTile waterTile, boolean hasShadows);

    public abstract void calculateBlockTiles(int x, int y, int[] sizesX, int[] var4, int[] sizesY, int[] var6, int[] var7, int[] var8, int[] var9, int[] floorColor, int[] minimapColor, int[] overlayTextureIds, int[] textureScales, WaterTile waterTile);

    public abstract void resetLight();

    public int getAverageHeight(int x, int y) {
        int scaledX = x >> this.tileScale;
        int scaledY = y >> this.tileScale;
        if (scaledX >= 0 && scaledY >= 0 && scaledX <= this.width - 1 && scaledY <= this.length - 1) {
            int i_6 = x & this.tileUnits - 1;
            int i_7 = y & this.tileUnits - 1;
            int i_8 = this.tileHeights[scaledX][scaledY] * (this.tileUnits - i_6) + i_6 * this.tileHeights[scaledX + 1][scaledY] >> this.tileScale;
            int i_9 = this.tileHeights[scaledX][scaledY + 1] * (this.tileUnits - i_6) + this.tileHeights[scaledX + 1][scaledY + 1] * i_6 >> this.tileScale;
            return i_9 * i_7 + i_8 * (this.tileUnits - i_7) >> this.tileScale;
        } 
        return 0;
    }

    public abstract void addShadow(Shadow shadow, int scaledX, int averageHeight, int scaledY);

    public abstract void removeShadow(Shadow shadow, int x, int groundOffset, int y);

    public abstract boolean hasRenderingShadows(Shadow shadow, int x, int planesHeightDelta, int y);

    public abstract void addLight(PointLight light, int[] var2);
    
    public abstract void renderMinimapFloor(int var1, int var2, int var4, int var5, int var6, int var7, boolean[][] visibleTiles);

    public abstract void renderFloor(int var1, int var2, int var3, boolean[][] var4, boolean var5);

    public int getHeight(int x, int y) {
        return this.tileHeights[x][y];
    }

    public abstract Shadow createShadowAt(int x, int y, Shadow shadow);
}
