package com.jagex.game.runetek5.entity

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
