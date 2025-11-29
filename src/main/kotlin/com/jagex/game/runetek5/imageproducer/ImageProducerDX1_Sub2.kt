package com.jagex.game.runetek5.imageproducer

import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import com.jagex.game.runetek5.graphics.texture.TextureFramebufferGenerator
import com.jagex.game.runetek5.toolkit.OpenGLNativeToolkit
import jaggl.OpenGL

class ImageProducerDX1_Sub2(
    @JvmField val aGraphicalRendererSub2Sub110446: OpenGLNativeToolkit
) : AbstractImageProducerDX1(aGraphicalRendererSub2Sub110446), AutoCloseable {

    @JvmField val anInterface28Array10449: Array<TextureFramebufferGenerator?> = arrayOfNulls(4)
    @JvmField var anInt10443: Int
    @JvmField var anInt10444: Int = 0
    @JvmField var anInterface2810448: TextureFramebufferGenerator? = null
    @JvmField var anInt10445: Int = 0
    @JvmField var anInt10441: Int = 0
    @JvmField var anInt10447: Int = 0

    init {
        val ints_2 = IntArray(1)
        OpenGL.glGenFramebuffersEXT(1, ints_2, 0)
        anInt10443 = ints_2[0]
    }

    override fun getWidth(): Int {
        return anInt10441
    }

    override fun setRenderBuffer(interface81: RenderBuffer?) {
        val interface282 = interface81 as TextureFramebufferGenerator?
        if (interface81 == null) {
            anInt10444 = anInt10444 and -17
            anInterface2810448 = null
            if (anInt10444 == 0) {
                anInt10445 = 0
                anInt10441 = 0
            }
        } else {
            if (anInt10444 != 0) {
                if (anInt10441 != interface282!!.getUseWidth() || anInt10445 != interface282.getUseHeight()) {
                    throw RuntimeException()
                }
            } else {
                anInt10445 = interface282!!.getUseHeight()
                anInt10441 = interface282.getUseWidth()
                method15562()
            }

            anInt10444 = anInt10444 or 0x10
            anInterface2810448 = interface282
        }

        if (this === aGraphicalRendererSub2Sub110446.getDisplay()) {
            method15982()
        } else {
            anInt10447 = anInt10447 or 0x10
        }
    }

    fun method15981(i_1: Int) {
        val interface282 = anInterface28Array10449[i_1]
        if (interface282 == null) {
            OpenGL.glFramebufferRenderbufferEXT(36160, i_1 + 36064, 36161, 0)
        } else {
            interface282.generateTextureFramebuffer(i_1 + 36064)
        }
    }

    fun method15982() {
        if (anInterface2810448 == null) {
            OpenGL.glFramebufferRenderbufferEXT(36160, 36096, 36161, 0)
        } else {
            anInterface2810448!!.generateTextureFramebuffer(36096)
        }
    }

    override fun reload(): Boolean {
        OpenGL.glBindFramebufferEXT(36160, anInt10443)

        for (i_1 in 0 until 4) {
            if ((anInt10447 and (1 shl i_1)) != 0) {
                method15981(i_1)
            }
        }

        if ((anInt10447 and 0x10) != 0) {
            method15982()
        }

        anInt10447 = 0
        return super.reload()
    }

    override fun cleanup() {
        if (anInt10443 != 0) {
            aGraphicalRendererSub2Sub110446.requestFrameBufferDeletion(anInt10443)
            anInt10443 = 0
        }
    }

    override fun close() {
        cleanup()
    }

    override fun isFramebufferComplete(): Boolean {
        val i_1 = OpenGL.glCheckFramebufferStatusEXT(36160)
        return i_1 == 36053
    }

    override fun setFramebuffer(i_1: Int, interface92: FrameBuffer?) {
        val i_3 = 1 shl i_1
        val interface284 = interface92 as TextureFramebufferGenerator?
        if (interface92 == null) {
            anInt10444 = anInt10444 and i_3.inv()
            anInterface28Array10449[i_1] = null
            if (anInt10444 == 0) {
                anInt10445 = 0
                anInt10441 = 0
            }
        } else {
            if (anInt10444 != 0) {
                if (anInt10441 != interface284!!.getUseWidth() || anInt10445 != interface284.getUseHeight()) {
                    throw RuntimeException()
                }
            } else {
                anInt10445 = interface284!!.getUseHeight()
                anInt10441 = interface284.getUseWidth()
                method15562()
            }

            anInt10444 = anInt10444 or i_3
            anInterface28Array10449[i_1] = interface284
        }

        if (this === aGraphicalRendererSub2Sub110446.getDisplay()) {
            method15981(i_1)
        } else {
            anInt10447 = anInt10447 or i_3
        }
    }

    override fun getHeight(): Int {
        return anInt10445
    }

    override fun setDepthStencilSurface(): Boolean {
        OpenGL.glBindFramebufferEXT(36160, 0)
        return true
    }
}
