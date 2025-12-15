package com.jagex.game.runetek5.unidentified

import com.jagex.game.runetek5.buffer.OpenGlRenderBuffer
import com.jagex.game.runetek5.cache.node.LinkedList
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.graphics.texture.Node_Sub5
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture
import com.jagex.game.runetek5.imageproducer.IntegratedGraphicsDisplayGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*
import jaggl.OpenGL

class Class170(val activeToolkit: OpenGlToolkit) {
    var renderBuffer: OpenGlRenderBuffer? = null
    var activeTexture: OpenGl2DTexture? = null
    var hdrRenderBuffer: OpenGlRenderBuffer? = null
    var aBool2103 = true
    var aBool2109 = true
    var aBool2105 = true
    var pixelFormatSet = true
    var aBool2100 = false
    var s = 1
    var t = 1
    var vertexCoordinateX = 0
    var vertexCoordinateY = 0
    var format: GLActionFormat = GLActionFormat.PIXEL_FORMAT
    var loadTexture = false
    var texturedFaceCount = 0
    val aClass4732108 = LinkedList()
    var textures2d: Array<OpenGl2DTexture?>? = arrayOfNulls<OpenGl2DTexture>(2)
    var aClass158Sub1Sub42099: IntegratedGraphicsDisplayGL? = null
    var aClass158Sub1Sub42095: IntegratedGraphicsDisplayGL? = null
    var aClass158Sub1Sub42101: IntegratedGraphicsDisplayGL? = null

    init {
        if (activeToolkit.intelGpu && activeToolkit.arbRectangleTexturesSupported) {
            aClass158Sub1Sub42099 = IntegratedGraphicsDisplayGL(activeToolkit)
            aClass158Sub1Sub42095 = aClass158Sub1Sub42099
            if (activeToolkit.multiSamplesPreference > 1 && activeToolkit.framebufferBlit && activeToolkit.framebufferMultisample) {
                aClass158Sub1Sub42101 = IntegratedGraphicsDisplayGL(activeToolkit)
                aClass158Sub1Sub42095 = aClass158Sub1Sub42101
            }
        }
    }

    fun method2881(): Boolean {
        if (aBool2103) {
            if (renderBuffer != null) {
                renderBuffer!!.deallocate()
                renderBuffer = null
            }
            if (activeTexture != null) {
                activeTexture!!.delete()
                activeTexture = null
            }
        }
        if (pixelFormatSet) {
            if (hdrRenderBuffer != null) {
                hdrRenderBuffer!!.deallocate()
                hdrRenderBuffer = null
            }
            if (textures2d?.get(0) != null) {
                textures2d?.get(0)?.delete()
                textures2d?.set(0, null)
            }
            if (textures2d?.get(1) != null) {
                textures2d?.get(1)?.delete()
                textures2d?.set(1, null)
            }
        }
        if (aBool2103) {
            if (aClass158Sub1Sub42101 != null) {
                renderBuffer = OpenGlRenderBuffer(activeToolkit, GLFormat.aClass1501953, GLActionFormat.DEPTH_COMPONENT_24, s, t, activeToolkit.multiSamplesPreference)
            }

            if (loadTexture) {
                activeTexture = OpenGl2DTexture(activeToolkit, 34037, GLFormat.aClass1501953, GLActionFormat.DEPTH_COMPONENT_24, s, t)
            } else if (renderBuffer == null) {
                renderBuffer = OpenGlRenderBuffer(activeToolkit, GLFormat.aClass1501953, GLActionFormat.DEPTH_COMPONENT_24, s, t)
            }

            aBool2103 = false
            aBool2109 = true
            aBool2105 = true
        }

        if (pixelFormatSet) {
            if (aClass158Sub1Sub42101 != null) {
                hdrRenderBuffer = OpenGlRenderBuffer(activeToolkit, GLFormat.HDR, format, s, t, activeToolkit.multiSamplesPreference)
            }

            textures2d = arrayOfNulls<OpenGl2DTexture>(2)
            textures2d?.set(0, OpenGl2DTexture(activeToolkit, 34037, GLFormat.HDR, format, s, t))
            textures2d?.set(1, if (texturedFaceCount > 1) OpenGl2DTexture(activeToolkit, 34037, GLFormat.HDR, format, s, t) else null)
            pixelFormatSet = false
            aBool2109 = true
            aBool2105 = true
        }

        if (aBool2109) {
            if (aClass158Sub1Sub42101 != null) {
                activeToolkit.setDisplay(aClass158Sub1Sub42099!!)
                aClass158Sub1Sub42099!!.setRenderBuffer(null)
                aClass158Sub1Sub42099!!.setFramebuffer(0, null)
                aClass158Sub1Sub42099!!.setFramebuffer(1, null)
                aClass158Sub1Sub42099!!.setFramebuffer(0, textures2d?.get(0)?.createFrameBuffer())
                aClass158Sub1Sub42099!!.setFramebuffer(1, if (texturedFaceCount > 1) textures2d?.get(1)?.createFrameBuffer() else null)
                if (loadTexture) {
                    aClass158Sub1Sub42099!!.setRenderBuffer(activeTexture!!.createRenderBuffer())
                }

                activeToolkit.resizeDisplay(aClass158Sub1Sub42099!!)
                activeToolkit.setDisplay(aClass158Sub1Sub42101!!)
                aClass158Sub1Sub42101!!.setRenderBuffer(null)
                aClass158Sub1Sub42101!!.setFramebuffer(0, null)
                aClass158Sub1Sub42101!!.setFramebuffer(0, hdrRenderBuffer)
                aClass158Sub1Sub42101!!.setRenderBuffer(renderBuffer)
                activeToolkit.resizeDisplay(aClass158Sub1Sub42101!!)
            } else {
                activeToolkit.setDisplay(aClass158Sub1Sub42099!!)
                aClass158Sub1Sub42099!!.setRenderBuffer(null)
                aClass158Sub1Sub42099!!.setFramebuffer(0, null)
                aClass158Sub1Sub42099!!.setFramebuffer(1, null)
                aClass158Sub1Sub42099!!.setFramebuffer(0, textures2d?.get(0)?.createFrameBuffer())
                aClass158Sub1Sub42099!!.setFramebuffer(1, if (texturedFaceCount > 1) textures2d?.get(1)?.createFrameBuffer() else null)
                if (loadTexture) {
                    aClass158Sub1Sub42099!!.setRenderBuffer(activeTexture!!.createRenderBuffer())
                } else {
                    aClass158Sub1Sub42099!!.setRenderBuffer(renderBuffer)
                }

                activeToolkit.resizeDisplay(aClass158Sub1Sub42099!!)
            }

            aBool2109 = false
            aBool2105 = true
        }

        if (aBool2105) {
            activeToolkit.setDisplay(aClass158Sub1Sub42095!!)
            aBool2105 = !aClass158Sub1Sub42095!!.isFramebufferComplete()
            activeToolkit.resizeDisplay(aClass158Sub1Sub42095!!)
        }

        return !aBool2105
    }

    fun method2883() {
        if (aBool2100) {
            if (aClass158Sub1Sub42101 != null) {
                activeToolkit.resizeDisplay(aClass158Sub1Sub42101!!)
                activeToolkit.setDisplay(aClass158Sub1Sub42099!!)
                aClass158Sub1Sub42099!!.drawBuffer(0)
                aClass158Sub1Sub42101!!.bindFrameBuffer(s, t, loadTexture)
            }

            activeToolkit.setupSpriteRendering()
            activeToolkit.setBlendMode(0)
            activeToolkit.loadTextureCombineMode(1)
            activeToolkit.resetClipping()
            var textureIndex = 0
            var colorAttachmentSlot = 1

            var prev: Node_Sub5?
            var tail = aClass4732108.getTail() as Node_Sub5?
            while (tail != null) {
                prev = aClass4732108.getPrevious() as Node_Sub5?
                val numTexturedFaces = tail.getTexturedFaces()

                for (face in 0 until numTexturedFaces) {
                    textures2d?.get(textureIndex)?.let { tail.draw(it) }
                    if (prev == null && face == numTexturedFaces - 1) {
                        activeToolkit.resizeDisplay(aClass158Sub1Sub42099!!)
                        activeToolkit.method13640(0, 0)
                        OpenGL.glBegin(7)
                        OpenGL.glTexCoord2f(0.0f, t.toFloat())
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, 1.0f)
                        OpenGL.glVertex2i(vertexCoordinateX, vertexCoordinateY)
                        OpenGL.glTexCoord2f(0.0f, 0.0f)
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, 0.0f)
                        OpenGL.glVertex2i(vertexCoordinateX, vertexCoordinateY + t)
                        OpenGL.glTexCoord2f(s.toFloat(), 0.0f)
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 1.0f, 0.0f)
                        OpenGL.glVertex2i(vertexCoordinateX + s, vertexCoordinateY + t)
                        OpenGL.glTexCoord2f(s.toFloat(), t.toFloat())
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 1.0f, 1.0f)
                        OpenGL.glVertex2i(vertexCoordinateX + s, vertexCoordinateY)
                    } else {
                        aClass158Sub1Sub42099!!.drawBuffer(colorAttachmentSlot)
                        OpenGL.glBegin(7)
                        OpenGL.glTexCoord2f(0.0f, t.toFloat())
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, 1.0f)
                        OpenGL.glVertex2i(0, 0)
                        OpenGL.glTexCoord2f(0.0f, 0.0f)
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 0.0f, 0.0f)
                        OpenGL.glVertex2i(0, t)
                        OpenGL.glTexCoord2f(s.toFloat(), 0.0f)
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 1.0f, 0.0f)
                        OpenGL.glVertex2i(s, t)
                        OpenGL.glTexCoord2f(s.toFloat(), t.toFloat())
                        OpenGL.glMultiTexCoord2f(GL_TEXTURE1, 1.0f, 1.0f)
                        OpenGL.glVertex2i(s, 0)
                    }
                    OpenGL.glEnd()

                    tail.resetTexture()
                    colorAttachmentSlot = colorAttachmentSlot + 1 and 0x1
                    textureIndex = textureIndex + 1 and 0x1
                }

                tail = prev
            }

            aBool2100 = false
        }
    }

    fun method2884(s: Int, t: Int): Boolean {
        if (aClass158Sub1Sub42095 != null && !aClass4732108.isEmpty()) {
            if (s != this.s || t != this.t) {
                this.s = s
                this.t = t
                var node5: Any? = aClass4732108.getTail()
                while (node5 !== aClass4732108.head) {
                    (node5 as Node_Sub5).calculateMipmaps(this.s, this.t)
                    node5 = (node5 as? Node<*>)?.next
                }
                pixelFormatSet = true
                aBool2103 = true
                aBool2109 = true
            }
            if (method2881()) {
                vertexCoordinateX = 0
                vertexCoordinateY = 0
                aBool2100 = true
                activeToolkit.method13640(-vertexCoordinateX, t + vertexCoordinateY - activeToolkit.getDisplay()!!.getHeight())
                activeToolkit.setDisplay(aClass158Sub1Sub42095!!)
                activeToolkit.applyClearFlags(3, 0)
                aClass158Sub1Sub42095!!.drawBuffer(0)
                return true
            }
            return false
        }
        return false
    }

    fun method2885(class282Sub51: Node_Sub5) {
        class282Sub51.aBool7506 = false
        class282Sub51.destroy()
        class282Sub51.unlink()
        method2886()
    }

    fun method2886() {
        var texturedFaces = 0
        var loadTexture = false
        var pixelFormat = GLActionFormat.PIXEL_FORMAT

        var node = aClass4732108.getTail() as Node_Sub5?
        while (node != null) {
            val class767 = node.getFormat()
            if (class767.bytesPerEntry > pixelFormat.bytesPerEntry) {
                pixelFormat = class767
            }
            loadTexture = loadTexture or node.loadTexture()
            texturedFaces += node.getTexturedFaces()
            node = aClass4732108.getPrevious() as Node_Sub5?
        }

        if (pixelFormat != format) {
            format = pixelFormat
            pixelFormatSet = true
        }
        val currFaces = Math.min(2, texturedFaceCount)
        val newFaces = Math.min(2, texturedFaces)
        if (currFaces != newFaces) {
            pixelFormatSet = true
            aBool2109 = true
        }
        if (loadTexture != this.loadTexture) {
            this.loadTexture = loadTexture
            aBool2103 = true
        }
        texturedFaceCount = texturedFaces
    }

    fun method2896(): Boolean {
        return aClass158Sub1Sub42095 != null
    }

    fun shutdown() {
        aClass158Sub1Sub42099 = null
        aClass158Sub1Sub42101 = null
        aClass158Sub1Sub42095 = null
        renderBuffer = null
        activeTexture = null
        textures2d = null
        hdrRenderBuffer = null
        if (!aClass4732108.isEmpty()) {
            var node1: Any? = aClass4732108.getTail()
            while (node1 !== aClass4732108.head) {
                (node1 as Node_Sub5).destroy()
                node1 = (node1 as? Node<*>)?.next
            }
        }

        t = 1
        s = 1
    }

    fun method2900(node: Node_Sub5): Boolean {
        if (aClass158Sub1Sub42095 != null) {
            if (node.displayPresent() || node.createShaders()) {
                aClass4732108.insertTail(node)
                method2886()
                if (method2881()) {
                    if (s != -1 && t != -1) {
                        node.calculateMipmaps(s, t)
                    }
                    node.aBool7506 = true
                    return true
                }
            }
            method2885(node)
        }
        return false
    }
}
