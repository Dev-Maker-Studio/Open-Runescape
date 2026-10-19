package com.jagex.game.runetek5.entity

/**
 * Pooled linked-list node wrapping an interactive entity in a tile's interactives chain.
 *
 * The free list is shared between the game thread (players/NPCs added and released every
 * frame) and the async map loader thread (one node per loc during a background build), so
 * pool operations are guarded by [POOL_LOCK]. An unsynchronized pop can hand the same node
 * to both threads, and a concurrent release rewires [child] — which doubles as the tile
 * chain link — silently truncating a freshly built scene's interactives (missing roofs
 * and scenery).
 */
class InteractiveEntityContainer {
    @JvmField var interactiveObject: AbstractInteractiveEntity? = null
    @JvmField var child: InteractiveEntityContainer? = null

    fun release() {
        synchronized(POOL_LOCK) {
            if (activeChildCount < 500) {
                this.interactiveObject = null
                this.child = mainInteractiveWrapper
                mainInteractiveWrapper = this
                activeChildCount++
            }
        }
    }

    companion object {
        private val POOL_LOCK = Any()
        @JvmField var mainInteractiveWrapper: InteractiveEntityContainer? = null
        @JvmField var activeChildCount: Int = 0

        @JvmStatic
        fun add(interactive: AbstractInteractiveEntity): InteractiveEntityContainer {
            val wrapper = synchronized(POOL_LOCK) {
                if (mainInteractiveWrapper == null) {
                    InteractiveEntityContainer()
                } else {
                    val temp = mainInteractiveWrapper!!
                    mainInteractiveWrapper = temp.child
                    temp.child = null
                    activeChildCount--
                    temp
                }
            }
            wrapper.interactiveObject = interactive
            return wrapper
        }
    }
}
