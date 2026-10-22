package com.jagex.game.runetek5.graphics.modern.dump

import java.awt.Canvas

internal class CaptureSurface(canvas: Canvas?, framebuffer: Int) {
    private val canvas = requireNotNull(canvas) { "capture requires a native canvas" }
    private val size = this.canvas.size
    val width = size.width
    val height = size.height

    init {
        require(framebuffer == 0) { "capture requires the default drawable framebuffer" }
        require(width in 1..8192 && height in 1..8192) { "capture drawable dimensions $width x $height" }
    }

    fun validate(current: Canvas?) {
        require(current === canvas && canvas.size == size) { "capture drawable changed or resized" }
    }
}
