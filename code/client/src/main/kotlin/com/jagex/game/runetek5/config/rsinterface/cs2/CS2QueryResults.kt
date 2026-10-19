package com.jagex.game.runetek5.config.rsinterface.cs2

/**
 * Result set of the CS2 `*_FIND` / `*_FINDNEXT` opcode family.
 *
 * A script runs a search (`OC_FIND`, `CHATPHRASE_FIND`, …) and then walks the
 * matches one at a time with `*_FINDNEXT`, so the ids, the match count and the
 * read cursor are a single piece of state with a single lifetime.
 *
 * They used to live on three unrelated classes — the ids on `ColorImageCache`,
 * the cursor on `StaticElements`, the count on `CS2Interpreter` — which made it
 * possible to update one and not the others.
 */
object CS2QueryResults {

    /** Matching ids, or `null` when no search has run. */
    @JvmField
    var ids: ShortArray? = null

    /**
     * Number of matches.
     *
     * `-1` means "search ran and matched nothing"; scripts read this value
     * directly, so the distinction from `0` is observable and must be kept.
     */
    @JvmField
    var count: Int = 0

    /** Read position for `*_FINDNEXT`. */
    @JvmField
    var cursor: Int = 0

    /** Records that a search matched nothing. */
    @JvmStatic
    fun clear() {
        ids = null
        count = -1
        cursor = 0
    }

    /** Publishes a fresh result set and rewinds the cursor. */
    @JvmStatic
    fun publish(results: ShortArray?, matchCount: Int) {
        ids = results
        count = matchCount
        cursor = 0
    }

    /** Whether [next] would return another match. */
    @JvmStatic
    fun hasNext(): Boolean = ids != null && cursor < count

    /** Returns the next matching id, advancing the cursor. */
    @JvmStatic
    fun next(): Int = ids!![cursor++].toInt() and 0xffff
}
