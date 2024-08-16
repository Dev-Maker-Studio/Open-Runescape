package com.jagex.game.runetek5.graphics.hint;

import com.jagex.game.runetek5.cache.node.lru.LRUCache;

public class HintArrow {

    public static int hintPreferenceFlags;
    public static final LRUCache hintCache = new LRUCache(4);
    public int y;
    public int targetType;
    public int distance;
    public int targetIndex;
    public int x;
    public int modelId = -1;
    public int plane;
    public int arrowSprite;
    public int height;
    public int denominator;

    public static void resetCache(int preferenceFlags) {
        hintPreferenceFlags = preferenceFlags;
        hintCache.reset();
    }

    public static void sweepCache() {
        hintCache.clean(5);
    }

    public static void resetCache() {
        hintCache.reset();
    }

	public static void clearHintarrows() {
	    hintCache.resize();
	}
}
