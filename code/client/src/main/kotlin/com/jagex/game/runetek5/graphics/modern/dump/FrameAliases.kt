package com.jagex.game.runetek5.graphics.modern.dump

import org.lwjgl.opengl.GL33.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL33.GL_NEAREST

class FrameAliases(private val root: Int, private val width: Int, private val height: Int) {
    private val declared = hashSetOf(root)
    private val initialized = hashSetOf(root)
    private val whole = intArrayOf(0, 0, width, height)

    fun register(source: Int, target: Int, targetWidth: Int, targetHeight: Int): Boolean {
        if (target == root) return false
        initialized.remove(target)
        if (source !in declared || source == target || targetWidth != width || targetHeight != height) {
            declared.remove(target)
            return false
        }
        declared.add(target)
        return true
    }

    fun identityCopy(source: Int, target: Int, sourceBounds: IntArray, targetBounds: IntArray, mask: Int, filter: Int, scissored: Boolean): Boolean {
        if (source == target || source !in initialized || target !in declared || mask != GL_COLOR_BUFFER_BIT || filter != GL_NEAREST || scissored || !sourceBounds.contentEquals(whole) || !targetBounds.contentEquals(whole)) return false
        initialized.add(target)
        return true
    }
}
