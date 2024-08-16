package com.jagex.game.runetek5.config.staticelements;

public class StaticElements {

    public static int CS2_QUERY_RESULT_IDX;

    public int elementCount;

    public final int[] bitpackedPlacement;

    public final int[] mapCategoryIds;

    public StaticElements(int count) {
        this.elementCount = count;
        this.bitpackedPlacement = new int[this.elementCount];
        this.mapCategoryIds = new int[this.elementCount];
    }
}
