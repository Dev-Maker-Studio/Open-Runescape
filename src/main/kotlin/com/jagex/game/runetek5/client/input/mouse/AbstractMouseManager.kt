package com.jagex.game.runetek5.client.input.mouse

import java.awt.*

abstract class AbstractMouseManager {

    abstract fun leftButtonDown(): Boolean

    abstract fun rightButtonDown(): Boolean

    abstract fun middleButtonDown(): Boolean

    fun isMouseButtonPressed(): Boolean {
        return leftButtonDown() || rightButtonDown() || middleButtonDown()
    }

    abstract fun getMouseX(): Int

    abstract fun getMouseY(): Int

    abstract fun nextSubmission(): MouseRecord?

    abstract fun reset()

    abstract fun method3589()

    companion object {
        @JvmStatic
        fun inializeMouseRecorder(canvasComponent: Component): AbstractMouseManager {
            return MouseManager(canvasComponent)
        }
    }
}
