package com.jagex.game.runetek5.graphics

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

    @JvmField var triangleVertexStream: NativeStream? = null
    @JvmField var triangleVertexDeclaration: VertexDeclaration? = null
    @JvmField var format: GLActionFormat
    @JvmField var directDisplay: DirectDisplay? = null
    @JvmField var pingTextureSampler: RectangleTextureSampler? = null
    @JvmField var pingFramebuffer: FrameBuffer? = null
    @JvmField var pongTextureSampler: RectangleTextureSampler? = null
    @JvmField var pongFramebuffer: FrameBuffer? = null
    @JvmField var compositeTextureSampler: RectangleTextureSampler? = null
    @JvmField var compositeFramebuffer: FrameBuffer? = null
    @JvmField var depthRenderBuffer: RenderBuffer? = null
    @JvmField var formatType: Int = 0
    @JvmField val bloomVector: Vector<AbstractBloomShader> = Vector<AbstractBloomShader>()
    @JvmField var isCapturingBloom: Boolean = false

    init {
        this.format = GLActionFormat.PIXEL_FORMAT
    }

    fun initialize(bloomShader: AbstractBloomShader): Boolean {
        return initialize(this.bloomVector.size, bloomShader)
    }

    fun initializeTriangleStream() {
        if (this.triangleVertexStream == null) {
            this.triangleVertexStream = this.toolkit.createStream(false)
            this.triangleVertexStream!!.create(12, 4)
            val buffer = this.toolkit.directBuffer
            buffer.clear()
            buffer.putFloat(0.0F)
            buffer.putFloat(1.0F)
            buffer.putFloat(2.0F)
            this.triangleVertexStream!!.upload(0, buffer.position(), this.toolkit.directBufferAddress)
            this.triangleVertexDeclaration = this.toolkit.createVertexDeclaration(arrayOf(VertexDeclarationComponent(VertexDeclarationAction.TEXCOORD_1F)))
        }

    }

    fun destroy() {
        this.directDisplay!!.cleanup()
        this.pingFramebuffer!!.deallocate()
        this.pongFramebuffer!!.deallocate()
        this.compositeFramebuffer!!.deallocate()
        this.pingTextureSampler!!.deallocate()
        this.pongTextureSampler!!.deallocate()
        this.compositeTextureSampler!!.deallocate()
        this.depthRenderBuffer!!.deallocate()
        val count = this.bloomVector.size
        for (index in 0 until count) {
            val bloomShader = this.bloomVector.elementAt(index)
            bloomShader.destroy()
        }
    }

    fun getFormat() {
        initializeTriangleStream()
        when (this.formatType) {
            0 -> this.format = GLActionFormat.PIXEL_FORMAT
            1 -> this.format = GLActionFormat.FLOAT_16_ARB
            2 -> this.format = GLActionFormat.FLOAT_32_ARB
            else -> throw RuntimeException()
        }

        this.directDisplay = this.toolkit.getImageProducer()
        this.pingTextureSampler = this.toolkit.createRectangleTexture(GLFormat.RGBA, this.format, this.width, this.height)
        this.pingFramebuffer = this.pingTextureSampler!!.getFramebuffer()
        this.pongTextureSampler = this.toolkit.createRectangleTexture(GLFormat.RGBA, this.format, this.width, this.height)
        this.pongFramebuffer = this.pongTextureSampler!!.getFramebuffer()
        this.compositeTextureSampler = this.toolkit.createRectangleTexture(GLFormat.RGBA, this.format, this.width, this.height)
        this.compositeFramebuffer = this.compositeTextureSampler!!.getFramebuffer()
        this.depthRenderBuffer = this.toolkit.getRenderBuffer(this.width, this.height)
        val count = this.bloomVector.size

        for (index in 0 until count) {
            val bloomShader = this.bloomVector.elementAt(index)
            bloomShader.onRendererInitialized()
        }

    }

    fun drawTriangles() {
        this.toolkit.setStreamSource(0, this.triangleVertexStream!!)
        this.toolkit.setVertexDeclaration(this.triangleVertexDeclaration!!)
        this.toolkit.drawPrimitive(PrimitiveDrawMode.TRIANGLES, 0, 1)
    }

    fun initialize(insertAtIndex: Int, bloomShader: AbstractBloomShader): Boolean {
        if (!bloomShader.isLoaded() && !bloomShader.loadShader()) {
            removeBloomShader(bloomShader)
            return true
        }
        this.bloomVector.insertElementAt(bloomShader, insertAtIndex)
        bloomShader.onRendererInitialized()
        val shaderFormatType = bloomShader.getFormatType()
        if (shaderFormatType > this.formatType) {
            this.formatType = shaderFormatType
        }
        bloomShader.hasLinkedVector = true
        return true
    }

    fun removeBloomShader(bloomShader: AbstractBloomShader) {
        bloomShader.destroy()
        bloomShader.hasLinkedVector = false
        this.bloomVector.removeElement(bloomShader)
    }

    fun draw() {
        this.toolkit.setupUiRenderState()
        this.toolkit.toggleBlending(0)
        this.toolkit.setColourType(1)
        this.toolkit.resetClipping()
        this.directDisplay!!.setRenderBuffer(null)
        this.toolkit.resetViewportOffsets()
        val count = this.bloomVector.size
        this.compositeTextureSampler!!.enableMipmapping()

        for (index in 0 until count) {
            val bloomShader = this.bloomVector.elementAt(index)
            val iterations = bloomShader.getIterations()
            val resize = index == count - 1

            for (type in 0 until iterations) {
                if (resize && type == iterations - 1) {
                    this.toolkit.resizeDisplay(this.directDisplay!!)
                } else {
                    this.directDisplay!!.setFramebuffer(0, this.pongFramebuffer)
                }

                var compositeSampler = this.pingTextureSampler
                if (type == 0) {
                    compositeSampler = this.compositeTextureSampler
                }

                bloomShader.initialize(type, this.directDisplay!!, compositeSampler!!, this.compositeTextureSampler!!)
                drawTriangles()
                bloomShader.reset()
                val swapSampler = this.pingTextureSampler
                this.pingTextureSampler = this.pongTextureSampler
                this.pongTextureSampler = swapSampler
                val swapFramebuffer = this.pingFramebuffer
                this.pingFramebuffer = this.pongFramebuffer
                this.pongFramebuffer = swapFramebuffer
            }
        }

        this.toolkit.toggleBlending(1)
        this.toolkit.setColourType(0)
    }

    fun beginCapture(w: Int, h: Int): Boolean {
        if (this.bloomVector.isEmpty()) {
            return false
        }
        if (w != this.width || h != this.height) {
            this.width = w
            this.height = h
            destroy()
            getFormat()
        }

        this.directDisplay!!.setFramebuffer(0, this.compositeFramebuffer)
        if (this.depthRenderBuffer != null) {
            this.directDisplay!!.setRenderBuffer(this.depthRenderBuffer)
        }

        this.toolkit.setDisplay(this.directDisplay!!)
        this.toolkit.applyClearFlags(3, 0xffffff.inv())
        this.toolkit.colorMask(15)
        this.toolkit.resetAlphaBlendMode()
        if (!this.directDisplay!!.isFramebufferComplete()) {
            throw RuntimeException("")
        }
        this.isCapturingBloom = true
        return true
    }

    fun endCapture() {
        if (this.isCapturingBloom) {
            draw()
            this.isCapturingBloom = false
        }
    }
}
