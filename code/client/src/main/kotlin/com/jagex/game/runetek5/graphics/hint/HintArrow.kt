package com.jagex.game.runetek5.graphics.hint

import com.jagex.game.runetek5.cache.node.lru.LRUCache

class HintArrow {
    @JvmField var y: Int = 0
    @JvmField var targetType: Int = 0
    @JvmField var distance: Int = 0
    @JvmField var targetIndex: Int = 0
    @JvmField var x: Int = 0
    @JvmField var modelId: Int = -1
    @JvmField var plane: Int = 0
    @JvmField var arrowSprite: Int = 0
    @JvmField var height: Int = 0
    @JvmField var denominator: Int = 0

    companion object {
        @JvmField var hintPreferenceFlags: Int = 0
        @JvmField val hintCache = LRUCache(4)

        @JvmStatic
        fun resetCache(preferenceFlags: Int) {
            hintPreferenceFlags = preferenceFlags
            hintCache.reset()
        }

        @JvmStatic
        fun sweepCache() {
            hintCache.clean(5)
        }

        @JvmStatic
        fun resetCache() {
            hintCache.reset()
        }

        @JvmStatic
        fun clearHintarrows() {
            hintCache.resize()
        }
    }
}
