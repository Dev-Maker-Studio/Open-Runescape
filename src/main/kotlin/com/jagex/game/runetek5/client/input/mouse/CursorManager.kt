package com.jagex.game.runetek5.client.input.mouse

import java.awt.Component
import java.awt.Cursor
import java.awt.Point
import java.awt.Robot
import java.awt.image.BufferedImage

class CursorManager {
    @JvmField val positionManager: Robot = Robot()
    private val argbCursors = X11ArgbCursors.createOrNull()

    fun moveToPosition(x: Int, y: Int) {
        positionManager.mouseMove(x, y)
    }

    fun setCustomCursor(component: Component, cursorPixelsRgb: IntArray?, width: Int, height: Int, hotspotPoint: Point) {
        val cursor = cursorPixelsRgb?.let {
            argbCursors?.create(it, width, height, hotspotPoint) ?: createAwtCursor(component, it, width, height, hotspotPoint)
        }
        component.cursor = cursor
        argbCursors?.define(component, cursor)
    }

    private fun createAwtCursor(component: Component, argb: IntArray, width: Int, height: Int, hotspot: Point): Cursor {
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        bufferedImage.setRGB(0, 0, width, height, argb, 0, width)
        return component.toolkit.createCustomCursor(bufferedImage, hotspot, null)
    }

    companion object {
        @JvmField var defaultAttackCursor: Int = 0
        @JvmField var defaultCursorId: Int = 0
        @JvmField var activeCursorId: Int = 0
        @JvmField var instance: CursorManager? = null
    }
}
