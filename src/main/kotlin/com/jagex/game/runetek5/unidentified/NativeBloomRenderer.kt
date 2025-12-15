package com.jagex.game.runetek5.unidentified

import java.nio.ByteBuffer
import java.util.Vector

import com.jagex.game.runetek5.framebuffer.FrameBuffer
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import com.jagex.game.runetek5.graphics.shader.program.AbstractBloomShader
import com.jagex.game.runetek5.graphics.texture.sampler.RectangleTextureSampler
import com.jagex.game.runetek5.imageproducer.DirectDisplay
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.companion.NativeStream
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclaration
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationAction
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationComponent

class NativeBloomRenderer(
    @JvmField val toolkit: AbstractNativeToolkit,
    @JvmField var width: Int,
    @JvmField var height: Int
) {

    @JvmField var anInterface4376: NativeStream? = null
    @JvmField var aClass70377: VertexDeclaration? = null
    @JvmField var format: GLActionFormat
    @JvmField var directDisplay: DirectDisplay? = null
    @JvmField var anInterface29363: RectangleTextureSampler? = null
    @JvmField var anInterface9364: FrameBuffer? = null
    @JvmField var anInterface29372: RectangleTextureSampler? = null
    @JvmField var anInterface9368: FrameBuffer? = null
    @JvmField var anInterface29366: RectangleTextureSampler? = null
    @JvmField var anInterface9369: FrameBuffer? = null
    @JvmField var anInterface8362: RenderBuffer? = null
    @JvmField var formatType: Int = 0
    @JvmField val bloomVector: Vector<AbstractBloomShader> = Vector<AbstractBloomShader>()
    @JvmField var aBool370: Boolean = false

    init {
        this.format = GLActionFormat.PIXEL_FORMAT
    }

    fun initialize(bloomShader: AbstractBloomShader): Boolean {
        return initialize(this.bloomVector.size, bloomShader)
    }

    fun method817() {
        if (this.anInterface4376 == null) {
            this.anInterface4376 = this.toolkit.createStream(false)
            this.anInterface4376!!.create(12, 4)
            val buffer = this.toolkit.directBuffer
            buffer.clear()
            buffer.putFloat(0.0F)
            buffer.putFloat(1.0F)
            buffer.putFloat(2.0F)
            this.anInterface4376!!.upload(0, buffer.position(), this.toolkit.directBufferAddress)
            this.aClass70377 = this.toolkit.method13995(arrayOf(VertexDeclarationComponent(VertexDeclarationAction.aClass69691)))
        }

    }

    fun destroy() {
        this.directDisplay!!.cleanup()
        this.anInterface9364!!.deallocate()
        this.anInterface9368!!.deallocate()
        this.anInterface9369!!.deallocate()
        this.anInterface29363!!.deallocate()
        this.anInterface29372!!.deallocate()
        this.anInterface29366!!.deallocate()
        this.anInterface8362!!.deallocate()
        val elements = this.bloomVector.size
        for (index in 0 until elements) {
            val bloom = this.bloomVector.elementAt(index)
            bloom.destroy()
        }
    }

    fun getFormat() {
        method817()
        when (this.formatType) {
            0 -> this.format = GLActionFormat.PIXEL_FORMAT
            1 -> this.format = GLActionFormat.FLOAT_16_ARB
            2 -> this.format = GLActionFormat.FLOAT_32_ARB
            else -> throw RuntimeException()
        }

        this.directDisplay = this.toolkit.getImageProducer()
        this.anInterface29363 = this.toolkit.method14064(GLFormat.HDR, this.format, this.width, this.height)
        this.anInterface9364 = this.anInterface29363!!.getFramebuffer()
        this.anInterface29372 = this.toolkit.method14064(GLFormat.HDR, this.format, this.width, this.height)
        this.anInterface9368 = this.anInterface29372!!.getFramebuffer()
        this.anInterface29366 = this.toolkit.method14064(GLFormat.HDR, this.format, this.width, this.height)
        this.anInterface9369 = this.anInterface29366!!.getFramebuffer()
        this.anInterface8362 = this.toolkit.getRenderBuffer(this.width, this.height)
        val i_1 = this.bloomVector.size

        for (i_2 in 0 until i_1) {
            val class553 = this.bloomVector.elementAt(i_2)
            class553.method1102()
        }

    }

    fun drawTriangles() {
        this.toolkit.setStreamSource(0, this.anInterface4376)
        this.toolkit.setVertexDeclaration(this.aClass70377)
        this.toolkit.drawPrimitive(PrimitiveDrawMode.TRIANGLES, 0, 1)
    }

    fun initialize(insertAtIndex: Int, bloom: AbstractBloomShader): Boolean {
        if (!bloom.isLoaded() && !bloom.method1097()) {
            method822(bloom)
            return true
        }
        this.bloomVector.insertElementAt(bloom, insertAtIndex)
        bloom.method1102()
        val format = bloom.getFormatType()
        if (format > this.formatType) {
            this.formatType = format
        }
        bloom.hasLinkedVector = true
        return true
    }

    fun method822(bloom: AbstractBloomShader) {
        bloom.destroy()
        bloom.hasLinkedVector = false
        this.bloomVector.removeElement(bloom)
    }

    fun draw() {
        this.toolkit.method14003()
        this.toolkit.toggleBlending(0)
        this.toolkit.setColourType(1)
        this.toolkit.resetClipping()
        this.directDisplay!!.setRenderBuffer(null)
        this.toolkit.method13919()
        val count = this.bloomVector.size
        this.anInterface29366!!.enableMipmapping()

        for (index in 0 until count) {
            val bloomShader = this.bloomVector.elementAt(index)
            val iterations = bloomShader.getIterations()
            val resize = index == count - 1

            for (type in 0 until iterations) {
                if (resize && type == iterations - 1) {
                    this.toolkit.resizeDisplay(this.directDisplay!!)
                } else {
                    this.directDisplay!!.setFramebuffer(0, this.anInterface9368)
                }

                var compositeSampler = this.anInterface29363
                if (type == 0) {
                    compositeSampler = this.anInterface29366
                }

                bloomShader.initialize(type, this.directDisplay!!, compositeSampler!!, this.anInterface29366!!)
                drawTriangles()
                bloomShader.reset()
                val interface298 = this.anInterface29363
                this.anInterface29363 = this.anInterface29372
                this.anInterface29372 = interface298
                val interface99 = this.anInterface9364
                this.anInterface9364 = this.anInterface9368
                this.anInterface9368 = interface99
            }
        }

        this.toolkit.toggleBlending(1)
        this.toolkit.setColourType(0)
    }

    fun method830(w: Int, h: Int): Boolean {
        if (this.bloomVector.isEmpty()) {
            return false
        }
        if (w != this.width || h != this.height) {
            this.width = w
            this.height = h
            destroy()
            getFormat()
        }

        this.directDisplay!!.setFramebuffer(0, this.anInterface9369)
        if (this.anInterface8362 != null) {
            this.directDisplay!!.setRenderBuffer(this.anInterface8362)
        }

        this.toolkit.setDisplay(this.directDisplay!!)
        this.toolkit.applyClearFlags(3, 0xffffff.inv())
        this.toolkit.colorMask(15)
        this.toolkit.method13985()
        if (!this.directDisplay!!.isFramebufferComplete()) {
            throw RuntimeException("")
        }
        this.aBool370 = true
        return true
    }

    fun method833() {
        if (this.aBool370) {
            draw()
            this.aBool370 = false
        }
    }
}
