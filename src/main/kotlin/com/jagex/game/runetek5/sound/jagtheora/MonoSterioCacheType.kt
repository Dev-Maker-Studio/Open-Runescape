package com.jagex.game.runetek5.sound.jagtheora

class MonoSterioCacheType(
    @JvmField val type: Int
) {
    companion object {
        @JvmField
        val TIMED: MonoSterioCacheType = MonoSterioCacheType(1)

        @JvmField
        internal val COUNTER: MonoSterioCacheType = MonoSterioCacheType(0)
    }
}
