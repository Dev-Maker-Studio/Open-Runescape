package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import java.awt.Canvas

/**
 * Safe mode image producer with software rendering.
 *
 * @author Brainpower - 22/04/2022
 */
abstract class AbstractImageProducerSM(
    @JvmField val toolkit: SafeModeToolkit,
    @JvmField val canvas: Canvas,
    width: Int,
    height: Int
) : DelegatingDisplay() {

    @JvmField var bind: Boolean = false
    @JvmField var width: Int = width
    @JvmField var height: Int = height
    @JvmField var colorBuffer: IntArray? = null
    @JvmField var depthBuffer: FloatArray? = null

    override fun reload(): Boolean {
        toolkit.bind(width, height, colorBuffer, depthBuffer)
        bind = true
        return true
    }

    override fun resize(width: Int, height: Int) {
        if (this.width != width || height != this.height) {
            this.width = width
            this.height = height
            initialize()
        }
    }

    override fun getWidth(): Int {
        return width
    }

    override fun getHeight(): Int {
        return height
    }

    override fun setDepthStencilSurface(): Boolean {
        bind = false
        return true
    }

    open fun initialize() {
        colorBuffer = IntArray(height * width)
        depthBuffer = FloatArray(height * width)
        if (bind) {
            toolkit.bind(width, height, colorBuffer, depthBuffer)
        }
    }
}
