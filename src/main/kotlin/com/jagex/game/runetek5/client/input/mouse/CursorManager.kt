package com.jagex.game.runetek5.client.input.mouse

import java.awt.Component
import java.awt.Point
import java.awt.Robot
import java.awt.image.BufferedImage

class CursorManager {
    @JvmField val positionManager: Robot = Robot()

    fun moveToPosition(x: Int, y: Int) {
        positionManager.mouseMove(x, y)
    }

    fun setCustomCursor(component: Component, cursorPixelsRgb: IntArray?, width: Int, height: Int, hotspotPoint: Point) {
        if (cursorPixelsRgb != null) {
            val bufferedImage = BufferedImage(width, height, 2)
            bufferedImage.setRGB(0, 0, width, height, cursorPixelsRgb, 0, width)
            component.cursor = component.toolkit.createCustomCursor(bufferedImage, hotspotPoint, null)
        } else {
            component.cursor = null
        }
    }

    companion object {
        @JvmField var defaultAttackCursor: Int = 0
        @JvmField var defaultCursorId: Int = 0
        @JvmField var activeCursorId: Int = 0
        @JvmField var instance: CursorManager? = null
    }
}
