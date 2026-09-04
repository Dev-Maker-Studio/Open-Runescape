package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.toolkit.SafeModeToolkit
import java.awt.Canvas
import java.awt.Image
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.awt.image.DirectColorModel
import java.awt.image.Raster
import java.util.Hashtable

class BufferedImageProducerSM(
    toolkit: SafeModeToolkit,
    canvas: Canvas,
    width: Int,
    height: Int
) : AbstractImageProducerSM(toolkit, canvas, width, height) {

    @JvmField var image: Image? = null

    init {
        Rectangle()
        initialize()
    }

    override fun initialize() {
        super.initialize()
        val dataBuffer = DataBufferInt(super.colorBuffer!!, super.colorBuffer!!.size)
        val colorModel = DirectColorModel(32, 16711680, 65280, 255)
        val writableRaster = Raster.createWritableRaster(colorModel.createCompatibleSampleModel(super.width, super.height), dataBuffer, null)
        this.image = BufferedImage(colorModel, writableRaster, false, Hashtable<Any, Any>())
    }

    override fun draw(): Int {
        return draw(0, 0)
    }

    override fun draw(x: Int, y: Int): Int {
        val graphics = super.canvas.graphics
        graphics.drawImage(this.image, x, y, super.canvas)
        return 0
    }

    override fun cleanup() {
    }
}
