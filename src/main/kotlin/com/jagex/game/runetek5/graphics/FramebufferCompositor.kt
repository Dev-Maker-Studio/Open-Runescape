package com.jagex.game.runetek5.graphics

import com.jagex.game.runetek5.buffer.OpenGlRenderBuffer
import com.jagex.game.runetek5.cache.node.LinkedList
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.graphics.texture.PostProcessEffect
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture
import com.jagex.game.runetek5.imageproducer.IntegratedGraphicsDisplayGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.*
import jaggl.OpenGL

class FramebufferCompositor(val activeToolkit: OpenGlToolkit) {
    var renderBuffer: OpenGlRenderBuffer? = null
    var activeTexture: OpenGl2DTexture? = null
    var hdrRenderBuffer: OpenGlRenderBuffer? = null
    var isDepthBufferDirty = true
    var isFboAttachmentDirty = true
    var isFramebufferDirty = true
    var pixelFormatSet = true
    var isCompositionPending = false
    var s = 1
    var t = 1
    var vertexCoordinateX = 0
    var vertexCoordinateY = 0
    var format: GLActionFormat = GLActionFormat.PIXEL_FORMAT
    var loadTexture = false
    var texturedFaceCount = 0
    val effectsList = LinkedList()
    var textures2d: Array<OpenGl2DTexture?>? = arrayOfNulls<OpenGl2DTexture>(2)
    var resolveDisplay: IntegratedGraphicsDisplayGL? = null
    var activeDisplay: IntegratedGraphicsDisplayGL? = null
    var multisampleDisplay: IntegratedGraphicsDisplayGL? = null

    init {
        if (activeToolkit.intelGpu && activeToolkit.arbRectangleTexturesSupported) {
            resolveDisplay = IntegratedGraphicsDisplayGL(activeToolkit)
            activeDisplay = resolveDisplay
            if (activeToolkit.multiSamplesPreference > 1 && activeToolkit.framebufferBlit && activeToolkit.framebufferMultisample) {
                multisampleDisplay = IntegratedGraphicsDisplayGL(activeToolkit)
                activeDisplay = multisampleDisplay
            }
        }
    }

    fun rebuildFramebuffers(): Boolean {
        if (isDepthBufferDirty) {
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
        if (isDepthBufferDirty) {
            if (multisampleDisplay != null) {
                renderBuffer = OpenGlRenderBuffer(activeToolkit, GLFormat.DEPTH, GLActionFormat.DEPTH_COMPONENT_24, s, t, activeToolkit.multiSamplesPreference)
            }

            if (loadTexture) {
                activeTexture = OpenGl2DTexture(activeToolkit, 34037, GLFormat.DEPTH, GLActionFormat.DEPTH_COMPONENT_24, s, t)
            } else if (renderBuffer == null) {
                renderBuffer = OpenGlRenderBuffer(activeToolkit, GLFormat.DEPTH, GLActionFormat.DEPTH_COMPONENT_24, s, t)
            }

            isDepthBufferDirty = false
            isFboAttachmentDirty = true
            isFramebufferDirty = true
        }

        if (pixelFormatSet) {
            if (multisampleDisplay != null) {
                hdrRenderBuffer = OpenGlRenderBuffer(activeToolkit, GLFormat.HDR, format, s, t, activeToolkit.multiSamplesPreference)
            }

            textures2d = arrayOfNulls<OpenGl2DTexture>(2)
            textures2d?.set(0, OpenGl2DTexture(activeToolkit, 34037, GLFormat.HDR, format, s, t))
            textures2d?.set(1, if (texturedFaceCount > 1) OpenGl2DTexture(activeToolkit, 34037, GLFormat.HDR, format, s, t) else null)
            pixelFormatSet = false
            isFboAttachmentDirty = true
            isFramebufferDirty = true
        }

        if (isFboAttachmentDirty) {
            if (multisampleDisplay != null) {
                activeToolkit.setDisplay(resolveDisplay!!)
                resolveDisplay!!.setRenderBuffer(null)
                resolveDisplay!!.setFramebuffer(0, null)
                resolveDisplay!!.setFramebuffer(1, null)
                resolveDisplay!!.setFramebuffer(0, textures2d?.get(0)?.createFrameBuffer())
                resolveDisplay!!.setFramebuffer(1, if (texturedFaceCount > 1) textures2d?.get(1)?.createFrameBuffer() else null)
                if (loadTexture) {
                    resolveDisplay!!.setRenderBuffer(activeTexture!!.createRenderBuffer())
                }

                activeToolkit.resizeDisplay(resolveDisplay!!)
                activeToolkit.setDisplay(multisampleDisplay!!)
                multisampleDisplay!!.setRenderBuffer(null)
                multisampleDisplay!!.setFramebuffer(0, null)
                multisampleDisplay!!.setFramebuffer(0, hdrRenderBuffer)
                multisampleDisplay!!.setRenderBuffer(renderBuffer)
                activeToolkit.resizeDisplay(multisampleDisplay!!)
            } else {
                activeToolkit.setDisplay(resolveDisplay!!)
                resolveDisplay!!.setRenderBuffer(null)
                resolveDisplay!!.setFramebuffer(0, null)
                resolveDisplay!!.setFramebuffer(1, null)
                resolveDisplay!!.setFramebuffer(0, textures2d?.get(0)?.createFrameBuffer())
                resolveDisplay!!.setFramebuffer(1, if (texturedFaceCount > 1) textures2d?.get(1)?.createFrameBuffer() else null)
                if (loadTexture) {
                    resolveDisplay!!.setRenderBuffer(activeTexture!!.createRenderBuffer())
                } else {
                    resolveDisplay!!.setRenderBuffer(renderBuffer)
                }

                activeToolkit.resizeDisplay(resolveDisplay!!)
            }

            isFboAttachmentDirty = false
            isFramebufferDirty = true
        }

        if (isFramebufferDirty) {
            activeToolkit.setDisplay(activeDisplay!!)
            isFramebufferDirty = !activeDisplay!!.isFramebufferComplete()
            activeToolkit.resizeDisplay(activeDisplay!!)
        }

        return !isFramebufferDirty
    }

    fun compositeEffects() {
        if (isCompositionPending) {
            if (multisampleDisplay != null) {
                activeToolkit.resizeDisplay(multisampleDisplay!!)
                activeToolkit.setDisplay(resolveDisplay!!)
                resolveDisplay!!.drawBuffer(0)
                multisampleDisplay!!.bindFrameBuffer(s, t, loadTexture)
            }

            activeToolkit.setupSpriteRendering()
            activeToolkit.setBlendMode(0)
            activeToolkit.loadTextureCombineMode(1)
            activeToolkit.resetClipping()
            var textureIndex = 0
            var colorAttachmentSlot = 1

            var prevEffect: PostProcessEffect?
            var effect = effectsList.getTail() as PostProcessEffect?
            while (effect != null) {
                prevEffect = effectsList.getPrevious() as PostProcessEffect?
                val numTexturedFaces = effect.getTexturedFaces()

                for (face in 0 until numTexturedFaces) {
                    textures2d?.get(textureIndex)?.let { effect.draw(it) }
                    if (prevEffect == null && face == numTexturedFaces - 1) {
                        activeToolkit.resizeDisplay(resolveDisplay!!)
                        activeToolkit.setViewportOffset(0, 0)
                        activeToolkit.legacyDrawTrace()
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
                        resolveDisplay!!.drawBuffer(colorAttachmentSlot)
                        activeToolkit.legacyDrawTrace()
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

                    effect.resetTexture()
                    colorAttachmentSlot = colorAttachmentSlot + 1 and 0x1
                    textureIndex = textureIndex + 1 and 0x1
                }

                effect = prevEffect
            }

            isCompositionPending = false
        }
    }

    fun prepareForRendering(width: Int, height: Int): Boolean {
        if (activeDisplay != null && !effectsList.isEmpty()) {
            if (width != this.s || height != this.t) {
                this.s = width
                this.t = height
                var effect: Any? = effectsList.getTail()
                while (effect !== effectsList.head) {
                    (effect as PostProcessEffect).calculateMipmaps(this.s, this.t)
                    effect = (effect as? Node<*>)?.next
                }
                pixelFormatSet = true
                isDepthBufferDirty = true
                isFboAttachmentDirty = true
            }
            if (rebuildFramebuffers()) {
                vertexCoordinateX = 0
                vertexCoordinateY = 0
                isCompositionPending = true
                activeToolkit.setViewportOffset(-vertexCoordinateX, height + vertexCoordinateY - activeToolkit.getDisplay()!!.getHeight())
                activeToolkit.setDisplay(activeDisplay!!)
                activeToolkit.applyClearFlags(3, 0)
                activeDisplay!!.drawBuffer(0)
                return true
            }
            return false
        }
        return false
    }

    fun removeEffect(effect: PostProcessEffect) {
        effect.isActive = false
        effect.destroy()
        effect.unlink()
        recalculateFormat()
    }

    fun recalculateFormat() {
        var texturedFaces = 0
        var loadTexture = false
        var pixelFormat = GLActionFormat.PIXEL_FORMAT

        var effect = effectsList.getTail() as PostProcessEffect?
        while (effect != null) {
            val effectFormat = effect.getFormat()
            if (effectFormat.bytesPerEntry > pixelFormat.bytesPerEntry) {
                pixelFormat = effectFormat
            }
            loadTexture = loadTexture or effect.loadTexture()
            texturedFaces += effect.getTexturedFaces()
            effect = effectsList.getPrevious() as PostProcessEffect?
        }

        if (pixelFormat != format) {
            format = pixelFormat
            pixelFormatSet = true
        }
        val currFaces = Math.min(2, texturedFaceCount)
        val newFaces = Math.min(2, texturedFaces)
        if (currFaces != newFaces) {
            pixelFormatSet = true
            isFboAttachmentDirty = true
        }
        if (loadTexture != this.loadTexture) {
            this.loadTexture = loadTexture
            isDepthBufferDirty = true
        }
        texturedFaceCount = texturedFaces
    }

    fun isDisplayAvailable(): Boolean {
        return activeDisplay != null
    }

    fun shutdown() {
        resolveDisplay = null
        multisampleDisplay = null
        activeDisplay = null
        renderBuffer = null
        activeTexture = null
        textures2d = null
        hdrRenderBuffer = null
        if (!effectsList.isEmpty()) {
            var effect: Any? = effectsList.getTail()
            while (effect !== effectsList.head) {
                (effect as PostProcessEffect).destroy()
                effect = (effect as? Node<*>)?.next
            }
        }

        t = 1
        s = 1
    }

    fun addEffect(effect: PostProcessEffect): Boolean {
        if (activeDisplay != null) {
            if (effect.displayPresent() || effect.createShaders()) {
                effectsList.insertTail(effect)
                recalculateFormat()
                if (rebuildFramebuffers()) {
                    if (s != -1 && t != -1) {
                        effect.calculateMipmaps(s, t)
                    }
                    effect.isActive = true
                    return true
                }
            }
            removeEffect(effect)
        }
        return false
    }
}
