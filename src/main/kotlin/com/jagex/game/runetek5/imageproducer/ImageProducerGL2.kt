package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import jaggl.OpenGL
import java.awt.Canvas

class ImageProducerGL2 : DelegatingDisplay {

    @JvmField var aBool10122: Boolean = false
    @JvmField val aGraphicalRendererSub110118: OpenGlToolkit
    @JvmField val aCanvas10120: Canvas
    @JvmField val anOpenGL10119: OpenGL
    @JvmField val aLong10117: Long
    @JvmField var anInt10121: Int = 0
    @JvmField var anInt10116: Int = 0

    constructor(openGlToolkit: OpenGlToolkit, canvas: Canvas, surfaceHandle: Long) {
        aBool10122 = false
        aGraphicalRendererSub110118 = openGlToolkit
        aCanvas10120 = canvas
        anOpenGL10119 = openGlToolkit.activeOpenglInstance!!
        aLong10117 = surfaceHandle
        method15526()
    }

    constructor(openGlToolkit: OpenGlToolkit, canvas: Canvas) : this(
        openGlToolkit,
        canvas,
        openGlToolkit.activeOpenglInstance!!.prepareSurface(canvas)
    )

    override fun getWidth(): Int {
        return anInt10116
    }

    override fun getHeight(): Int {
        return anInt10121
    }

    fun method15526() {
        val dimension = aCanvas10120.size
        anInt10121 = dimension.height
        anInt10116 = dimension.width
    }

    override fun draw(): Int {
        if (aBool10122) {
            throw IllegalStateException()
        } else {
            anOpenGL10119.swapBuffers(aLong10117)
            return 0
        }
    }

    override fun reload(): Boolean {
        if (aBool10122) {
            throw IllegalStateException()
        } else {
            anOpenGL10119.setSurface(aLong10117)
            aGraphicalRendererSub110118.resetDisplay()
            return true
        }
    }

    override fun setDepthStencilSurface(): Boolean {
        return true
    }

    override fun cleanup() {
        if (aBool10122) {
            throw IllegalStateException()
        } else {
            anOpenGL10119.releaseSurface(aCanvas10120, aLong10117)
            aBool10122 = true
        }
    }

    override fun resize(width: Int, height: Int) {
        if (aBool10122) {
            throw IllegalStateException()
        } else {
            anOpenGL10119.surfaceResized(aLong10117)
            method15526()
            if (this == aGraphicalRendererSub110118.getDisplay()) {
                aGraphicalRendererSub110118.resetOrthoAndDisplay()
            }
        }
    }

    override fun draw(x: Int, y: Int): Int {
        return 0
    }
}
