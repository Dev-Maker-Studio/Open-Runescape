package com.jagex.game.runetek5.graphics.map.worldmap;

import com.jagex.game.runetek5.cache.node.Node;

public class WorldMapPlacement extends Node {

    public final int plane;
    public final int startX;
    public final int startY;
    public final int endX;
    public final int endY;
    public final int anInt7701;
    public final int anInt7703;
    public final int anInt7704;
    public final int anInt7705;

    public WorldMapPlacement(int plane, int startX, int startY, int endX, int endY, int i_6, int i_7, int i_8, int i_9) {
        this.plane = plane;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.anInt7701 = i_6;
        this.anInt7703 = i_7;
        this.anInt7704 = i_8;
        this.anInt7705 = i_9;
    }

    public boolean withinPlaneBounds(int plane, int x, int y) {
        return this.plane == plane && x >= this.startX && x <= this.endX && y >= this.startY && y <= this.endY;
    }

    public boolean method12409(int i_1, int i_2) {
        return i_1 >= this.anInt7701 && i_1 <= this.anInt7704 && i_2 >= this.anInt7703 && i_2 <= this.anInt7705;
    }

    public void method12410(int i_1, int i_2, int[] position) {
        position[0] = this.plane;
        position[1] = this.startX - this.anInt7701 + i_1;
        position[2] = this.startY - this.anInt7703 + i_2;
    }

    public void cache(int offsetX, int offsetY, int[] position) {
        position[0] = 0;
        position[1] = this.anInt7701 - this.startX + offsetX;
        position[2] = this.anInt7703 - this.startY + offsetY;
    }

    public boolean withinBounds(int x, int y) {
        return x >= this.startX && x <= this.endX && y >= this.startY && y <= this.endY;
    }

}
