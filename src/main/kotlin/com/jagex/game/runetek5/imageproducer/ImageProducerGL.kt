package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import jaggl.OpenGL
import java.awt.Canvas

class ImageProducerGL : DelegatingDisplay {

    @JvmField var isDisposed: Boolean = false
    @JvmField val openGlToolkit: OpenGlToolkit
    @JvmField val canvas: Canvas
    @JvmField val openGL: OpenGL
    @JvmField val surfaceHandle: Long
    @JvmField var displayHeight: Int = 0
    @JvmField var displayWidth: Int = 0

    constructor(toolkit: OpenGlToolkit, canvasParam: Canvas, handle: Long) {
        isDisposed = false
        this.openGlToolkit = toolkit
        this.canvas = canvasParam
        this.openGL = toolkit.activeOpenglInstance!!
        this.surfaceHandle = handle
        updateDimensions()
    }

    constructor(toolkit: OpenGlToolkit, canvasParam: Canvas) : this(
        toolkit,
        canvasParam,
        toolkit.activeOpenglInstance!!.prepareSurface(canvasParam)
    )

    override fun getWidth(): Int {
        return displayWidth
    }

    override fun getHeight(): Int {
        return displayHeight
    }

    fun updateDimensions() {
        val dimension = canvas.size
        displayHeight = dimension.height
        displayWidth = dimension.width
    }

    override fun draw(): Int {
        if (isDisposed) {
            throw IllegalStateException()
        } else {
            openGL.swapBuffers(surfaceHandle)
            return 0
        }
    }

    override fun reload(): Boolean {
        if (isDisposed) {
            throw IllegalStateException()
        } else {
            openGL.setSurface(surfaceHandle)
            openGlToolkit.resetDisplay()
            return true
        }
    }

    override fun setDepthStencilSurface(): Boolean {
        return true
    }

    override fun cleanup() {
        if (isDisposed) {
            throw IllegalStateException()
        } else {
            openGL.releaseSurface(canvas, surfaceHandle)
            isDisposed = true
        }
    }

    override fun resize(width: Int, height: Int) {
        if (isDisposed) {
            throw IllegalStateException()
        } else {
            openGL.surfaceResized(surfaceHandle)
            updateDimensions()
            if (this == openGlToolkit.getDisplay()) {
                openGlToolkit.resetOrthoAndDisplay()
            }
        }
    }

    override fun draw(x: Int, y: Int): Int {
        return 0
    }
}
