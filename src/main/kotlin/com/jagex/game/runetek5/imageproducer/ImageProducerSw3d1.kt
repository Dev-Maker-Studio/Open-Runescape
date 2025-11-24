package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import com.jagex.game.runetek5.framebuffer.SoftwareRenderBuffer
import com.jagex.game.runetek5.graphics.sprite.SpriteSw3d
import com.jagex.game.runetek5.toolkit.SoftwareToolkit

class ImageProducerSw3d1(
    @JvmField val aja9449: SoftwareToolkit
) : DirectDisplay() {

    @JvmField var anoa9448: SoftwareRenderBuffer? = null
    @JvmField var anInt9446: Int = 0
    @JvmField var anInt9450: Int = 0
    @JvmField var afa9447: SpriteSw3d? = null

    override fun getHeight(): Int {
        return anInt9450
    }

    override fun reload(): Boolean {
        aja9449.d(if (afa9447 == null) 0L else afa9447!!.nativeid, if (anoa9448 == null) 0L else anoa9448!!.nativeid)
        return true
    }

    override fun setFramebuffer(i_1: Int, interface92: FrameBuffer?) {
        if (i_1 != 0) {
            throw RuntimeException()
        } else if (anoa9448 != null && interface92 != null && (anoa9448!!.getUseWidth() != interface92.getUseWidth() || anoa9448!!.getUseHeight() != interface92.getUseHeight())) {
            throw RuntimeException()
        } else {
            afa9447 = interface92 as SpriteSw3d?
            if (interface92 != null) {
                anInt9446 = interface92.getUseWidth()
                anInt9450 = interface92.getUseHeight()
            } else if (anoa9448 == null) {
                anInt9446 = 0
                anInt9450 = 0
            }

            if (aja9449.getDisplay() == this) {
                reload()
            }
        }
    }

    override fun setDepthStencilSurface(): Boolean {
        return true
    }

    override fun cleanup() {
    }

    override fun setRenderBuffer(interface81: RenderBuffer?) {
        if (afa9447 != null && interface81 != null && (afa9447!!.getUseWidth() != interface81.getUseWidth() || afa9447!!.getUseHeight() != interface81.getUseHeight())) {
            throw RuntimeException()
        } else {
            anoa9448 = interface81 as SoftwareRenderBuffer?
            if (interface81 != null) {
                anInt9446 = interface81.getUseWidth()
                anInt9450 = interface81.getUseHeight()
            } else if (afa9447 == null) {
                anInt9446 = 0
                anInt9450 = 0
            }

            if (aja9449.getDisplay() == this) {
                reload()
            }
        }
    }

    override fun isFramebufferComplete(): Boolean {
        return true
    }

    override fun getWidth(): Int {
        return anInt9446
    }
}
