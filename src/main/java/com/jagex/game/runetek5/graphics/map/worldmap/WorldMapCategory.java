package com.jagex.game.runetek5.graphics.map.worldmap;

import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;

public class WorldMapCategory extends Node<WorldMapCategory> {

    public int absX;
    public int anInt7988;
    public int anInt7990;
    public int anInt7992;
    public int absY;
    public int mecId = -1;
    public boolean isMouseOver;

    public WorldMapCategory(int mecId) {
        this.mecId = mecId;
    }

    public static int method12702(int i_0, int i_1, int i_2) {
        if (ComponentMinimap.mapAssetLoadingPercentage < 100) {
            return -2;
        } else {
            int i_4 = -2;
            int i_5 = Integer.MAX_VALUE;
            int i_6 = i_1 - WorldMapSub1.mapSquareAbsX;
            int i_7 = i_2 - WorldMapSub1.mapSquareAbsY;

            for (WorldMapCategory class282Sub368 = (WorldMapCategory) WorldMapSub1.worldMapCategories.getFirst(); class282Sub368 != null; class282Sub368 = (WorldMapCategory) WorldMapSub1.worldMapCategories.next()) {
                if (i_0 == class282Sub368.mecId) {
                    int i_9 = class282Sub368.absX;
                    int i_10 = class282Sub368.absY;
                    int i_11 = i_9 + WorldMapSub1.mapSquareAbsX << 14 | i_10 + WorldMapSub1.mapSquareAbsY;
                    int i_12 = (i_6 - i_9) * (i_6 - i_9) + (i_7 - i_10) * (i_7 - i_10);
                    if (i_4 < 0 || i_12 < i_5) {
                        i_4 = i_11;
                        i_5 = i_12;
                    }
                }
            }

            return i_4;
        }
    }
}
