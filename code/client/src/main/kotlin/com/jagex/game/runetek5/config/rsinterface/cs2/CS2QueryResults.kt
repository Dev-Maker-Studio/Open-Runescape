package com.jagex.game.runetek5.config.rsinterface.cs2

object CS2QueryResults {

    @JvmField
    var ids: ShortArray? = null

    @JvmField
    var count: Int = 0

    @JvmField
    var cursor: Int = 0

    @JvmStatic
    fun clear() {
        ids = null
        count = -1
        cursor = 0
    }

    @JvmStatic
    fun publish(results: ShortArray?, matchCount: Int) {
        ids = results
        count = matchCount
        cursor = 0
    }

    @JvmStatic
    fun hasNext(): Boolean = ids != null && cursor < count

    @JvmStatic
    fun next(): Int = ids!![cursor++].toInt() and 0xffff
}
