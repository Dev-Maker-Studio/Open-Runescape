package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import jaggl.OpenGL
import java.awt.Canvas

/**
 * OpenGL image producer for DirectX 3 compatible rendering.
 */
class ImageProducerDX3 : AbstractImageProducerDX {

    @JvmField val canvas: Canvas
    @JvmField val opengl: OpenGL
    @JvmField val surfaceHandle: Long
    @JvmField var height: Int = 0
    @JvmField var width: Int = 0

    constructor(toolkit: OpenGLNativeToolkit, canvas: Canvas, surfaceHandle: Long) : super(toolkit) {
        this.canvas = canvas
        this.opengl = toolkit.opengl
        this.surfaceHandle = surfaceHandle
        updateDimensions()
    }

    constructor(toolkit: OpenGLNativeToolkit, canvas: Canvas) : this(
        toolkit,
        canvas,
        toolkit.opengl.prepareSurface(canvas)
    )

    override fun getHeight(): Int {
        return height
    }

    override fun getWidth(): Int {
        return width
    }

    /**
     * Updates the cached dimensions from the canvas size.
     */
    fun updateDimensions() {
        val dimension = canvas.size
        height = dimension.height
        width = dimension.width
    }

    override fun draw(): Int {
        opengl.swapBuffers(surfaceHandle)
        return 0
    }

    override fun draw(x: Int, y: Int): Int {
        return 0
    }

    override fun reload(): Boolean {
        return opengl.setSurface(surfaceHandle) && super.reload()
    }

    override fun cleanup() {
        opengl.releaseSurface(canvas, surfaceHandle)
    }

    override fun resize(newWidth: Int, newHeight: Int) {
        opengl.surfaceResized(surfaceHandle)
        updateDimensions()
        super.resize(newWidth, newHeight)
    }

    override fun setDepthStencilSurface(): Boolean {
        return true
    }
}
