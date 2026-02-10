package com.jagex.game.runetek5.entity

class InteractiveEntityContainer {
    @JvmField var interactiveObject: AbstractInteractiveEntity? = null
    @JvmField var child: InteractiveEntityContainer? = null

    fun release() {
        if (activeChildCount < 500) {
            this.interactiveObject = null
            this.child = mainInteractiveWrapper
            mainInteractiveWrapper = this
            activeChildCount++
        }
    }

    companion object {
        @JvmField var mainInteractiveWrapper: InteractiveEntityContainer? = null
        @JvmField var activeChildCount: Int = 0

        @JvmStatic
        fun add(interactive: AbstractInteractiveEntity): InteractiveEntityContainer {
            val wrapper = if (mainInteractiveWrapper == null) {
                InteractiveEntityContainer()
            } else {
                val temp = mainInteractiveWrapper!!
                mainInteractiveWrapper = mainInteractiveWrapper!!.child
                temp.child = null
                activeChildCount--
                temp
            }
            wrapper.interactiveObject = interactive
            return wrapper
        }
    }
}
