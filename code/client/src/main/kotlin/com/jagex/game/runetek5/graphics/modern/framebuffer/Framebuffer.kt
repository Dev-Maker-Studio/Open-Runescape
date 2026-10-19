package com.jagex.game.runetek5.graphics.modern.framebuffer

import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.COLOR_ATTACHMENT0
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.DEPTH24_STENCIL8
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.DEPTH_ATTACHMENT
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.DEPTH_STENCIL_ATTACHMENT
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.FRAMEBUFFER
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.FRAMEBUFFER_COMPLETE
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.LINEAR
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.NEAREST
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.RENDERBUFFER
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.RGBA
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.RGBA8
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_2D
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_MAG_FILTER
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_MIN_FILTER
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_WRAP_S
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_WRAP_T
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_BYTE
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.CLAMP_TO_EDGE
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.DEPTH_COMPONENT
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.DEPTH_COMPONENT24
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.FLOAT

/**
 * Framebuffer object wrapper for off-screen rendering.
 *
 * Supports color attachments (textures) and depth/stencil attachments
 * (either as texture or renderbuffer).
 *
 * Usage:
 * ```kotlin
 * val fbo = Framebuffer(1920, 1080)
 * fbo.addColorAttachment(0)
 * fbo.addDepthRenderbuffer()
 * fbo.create()
 *
 * // Render to framebuffer
 * fbo.bind()
 * // ... render scene ...
 * fbo.unbind()
 *
 * // Use color texture
 * val colorTexture = fbo.getColorTexture(0)
 * ```
 */
class Framebuffer(
    var width: Int,
    var height: Int
) {
    // OpenGL handles
    private var fboId: Int = 0
    private var depthRboId: Int = 0
    private var depthTextureId: Int = 0
    private val colorTextures = mutableMapOf<Int, Int>()

    // Configuration
    private var useDepthTexture = false
    private var hasDepth = false
    private var isCreated = false

    // Attachments to create
    private val pendingColorAttachments = mutableListOf<ColorAttachmentConfig>()
    private var pendingDepthConfig: DepthConfig? = null

    /**
     * Adds a color attachment at the specified index.
     *
     * @param index Attachment index (0-15)
     * @param filtering Use linear filtering (default) or nearest
     * @param format Internal format (RGBA8 by default)
     */
    fun addColorAttachment(
        index: Int = 0,
        filtering: FilterType = FilterType.LINEAR,
        format: Int = RGBA8
    ): Framebuffer {
        pendingColorAttachments.add(ColorAttachmentConfig(index, filtering, format))
        return this
    }

    /**
     * Adds a depth renderbuffer (not readable as texture).
     * More efficient than depth texture if depth values aren't needed.
     */
    fun addDepthRenderbuffer(): Framebuffer {
        pendingDepthConfig = DepthConfig(asTexture = false)
        return this
    }

    /**
     * Adds a depth texture attachment (readable for shadow mapping etc).
     */
    fun addDepthTexture(): Framebuffer {
        pendingDepthConfig = DepthConfig(asTexture = true)
        return this
    }

    /**
     * Creates the framebuffer and all configured attachments.
     *
     * @return true if framebuffer is complete, false otherwise
     */
    fun create(): Boolean {
        if (isCreated) {
            println("[Framebuffer] Already created, call destroy() first")
            return false
        }

        // Create framebuffer
        fboId = GLWrapper.genFramebuffers()
        GLWrapper.bindFramebuffer(FRAMEBUFFER, fboId)

        // Create color attachments
        for (config in pendingColorAttachments) {
            createColorAttachment(config)
        }

        // Create depth attachment
        pendingDepthConfig?.let { config ->
            if (config.asTexture) {
                createDepthTexture()
            } else {
                createDepthRenderbuffer()
            }
            hasDepth = true
        }

        // Check completeness
        val status = GLWrapper.checkFramebufferStatus(FRAMEBUFFER)
        if (status != FRAMEBUFFER_COMPLETE) {
            println("[Framebuffer] Incomplete: ${getStatusString(status)}")
            GLWrapper.bindFramebuffer(FRAMEBUFFER, 0)
            destroy()
            return false
        }

        GLWrapper.bindFramebuffer(FRAMEBUFFER, 0)
        isCreated = true
        return true
    }

    private fun createColorAttachment(config: ColorAttachmentConfig) {
        val texId = GLWrapper.genTextures()
        GLWrapper.bindTexture(TEXTURE_2D, texId)

        val filter = if (config.filtering == FilterType.LINEAR) LINEAR else NEAREST

        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, filter)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, filter)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, CLAMP_TO_EDGE)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, CLAMP_TO_EDGE)

        GLWrapper.texImage2D(TEXTURE_2D, 0, config.format, width, height, 0, RGBA, UNSIGNED_BYTE, null)
        GLWrapper.framebufferTexture2D(FRAMEBUFFER, COLOR_ATTACHMENT0 + config.index, TEXTURE_2D, texId, 0)

        colorTextures[config.index] = texId
    }

    private fun createDepthRenderbuffer() {
        depthRboId = GLWrapper.genRenderbuffers()
        GLWrapper.bindRenderbuffer(RENDERBUFFER, depthRboId)
        GLWrapper.renderbufferStorage(RENDERBUFFER, DEPTH24_STENCIL8, width, height)
        GLWrapper.framebufferRenderbuffer(FRAMEBUFFER, DEPTH_STENCIL_ATTACHMENT, RENDERBUFFER, depthRboId)
        useDepthTexture = false
    }

    private fun createDepthTexture() {
        depthTextureId = GLWrapper.genTextures()
        GLWrapper.bindTexture(TEXTURE_2D, depthTextureId)

        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, NEAREST)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, NEAREST)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, CLAMP_TO_EDGE)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, CLAMP_TO_EDGE)

        GLWrapper.texImage2D(TEXTURE_2D, 0, DEPTH_COMPONENT24, width, height, 0, DEPTH_COMPONENT, FLOAT, null)
        GLWrapper.framebufferTexture2D(FRAMEBUFFER, DEPTH_ATTACHMENT, TEXTURE_2D, depthTextureId, 0)

        useDepthTexture = true
    }

    /**
     * Binds this framebuffer for rendering.
     */
    fun bind() {
        if (!isCreated) {
            println("[Framebuffer] Not created, cannot bind")
            return
        }
        GLWrapper.bindFramebuffer(FRAMEBUFFER, fboId)
        GLWrapper.viewport(0, 0, width, height)
    }

    /**
     * Unbinds this framebuffer (returns to default framebuffer).
     */
    fun unbind() {
        GLWrapper.bindFramebuffer(FRAMEBUFFER, 0)
    }

    /**
     * Resizes the framebuffer and all attachments.
     */
    fun resize(newWidth: Int, newHeight: Int) {
        if (newWidth == width && newHeight == height) return
        if (!isCreated) {
            width = newWidth
            height = newHeight
            return
        }

        width = newWidth
        height = newHeight

        // Resize color textures
        for ((index, texId) in colorTextures) {
            GLWrapper.bindTexture(TEXTURE_2D, texId)
            val config = pendingColorAttachments.find { it.index == index }
            val format = config?.format ?: RGBA8
            GLWrapper.texImage2D(TEXTURE_2D, 0, format, width, height, 0, RGBA, UNSIGNED_BYTE, null)
        }

        // Resize depth
        if (hasDepth) {
            if (useDepthTexture) {
                GLWrapper.bindTexture(TEXTURE_2D, depthTextureId)
                GLWrapper.texImage2D(TEXTURE_2D, 0, DEPTH_COMPONENT24, width, height, 0, DEPTH_COMPONENT, FLOAT, null)
            } else {
                GLWrapper.bindRenderbuffer(RENDERBUFFER, depthRboId)
                GLWrapper.renderbufferStorage(RENDERBUFFER, DEPTH24_STENCIL8, width, height)
            }
        }

        GLWrapper.bindTexture(TEXTURE_2D, 0)
        GLWrapper.bindRenderbuffer(RENDERBUFFER, 0)
    }

    /**
     * Gets the color texture at the specified attachment index.
     */
    fun getColorTexture(index: Int = 0): Int = colorTextures[index] ?: 0

    /**
     * Gets the depth texture (if created as texture).
     */
    fun getDepthTexture(): Int = if (useDepthTexture) depthTextureId else 0

    /**
     * Gets the framebuffer ID.
     */
    fun getId(): Int = fboId

    /**
     * Checks if the framebuffer is valid.
     */
    fun isValid(): Boolean = isCreated

    /**
     * Destroys the framebuffer and all resources.
     */
    fun destroy() {
        if (!isCreated && fboId == 0) return

        for ((_, texId) in colorTextures) {
            GLWrapper.deleteTextures(texId)
        }
        colorTextures.clear()

        if (depthTextureId != 0) {
            GLWrapper.deleteTextures(depthTextureId)
            depthTextureId = 0
        }

        if (depthRboId != 0) {
            GLWrapper.deleteRenderbuffers(depthRboId)
            depthRboId = 0
        }

        if (fboId != 0) {
            GLWrapper.deleteFramebuffers(fboId)
            fboId = 0
        }

        isCreated = false
    }

    private fun getStatusString(status: Int): String {
        return when (status) {
            0x8CD5 -> "FRAMEBUFFER_COMPLETE"
            0x8CD6 -> "FRAMEBUFFER_INCOMPLETE_ATTACHMENT"
            0x8CD7 -> "FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT"
            0x8CDD -> "FRAMEBUFFER_UNSUPPORTED"
            0x8D56 -> "FRAMEBUFFER_INCOMPLETE_MULTISAMPLE"
            else -> "Unknown (0x${status.toString(16)})"
        }
    }

    private data class ColorAttachmentConfig(
        val index: Int,
        val filtering: FilterType,
        val format: Int
    )

    private data class DepthConfig(
        val asTexture: Boolean
    )
}

/**
 * Texture filtering type for framebuffer attachments.
 */
enum class FilterType {
    LINEAR,
    NEAREST
}

/**
 * Multi-sample framebuffer for anti-aliasing.
 */
class MultisampleFramebuffer(
    var width: Int,
    var height: Int,
    val samples: Int = 4
) {
    private var fboId: Int = 0
    private var colorRboId: Int = 0
    private var depthRboId: Int = 0
    private var isCreated = false

    /**
     * Creates the multisample framebuffer.
     */
    fun create(): Boolean {
        if (isCreated) return false

        // Create multisample FBO
        fboId = GLWrapper.genFramebuffers()
        GLWrapper.bindFramebuffer(FRAMEBUFFER, fboId)

        // Create multisample color renderbuffer
        colorRboId = GLWrapper.genRenderbuffers()
        GLWrapper.bindRenderbuffer(RENDERBUFFER, colorRboId)
        GLWrapper.renderbufferStorageMultisample(RENDERBUFFER, samples, RGBA8, width, height)
        GLWrapper.framebufferRenderbuffer(FRAMEBUFFER, COLOR_ATTACHMENT0, RENDERBUFFER, colorRboId)

        // Create multisample depth renderbuffer
        depthRboId = GLWrapper.genRenderbuffers()
        GLWrapper.bindRenderbuffer(RENDERBUFFER, depthRboId)
        GLWrapper.renderbufferStorageMultisample(RENDERBUFFER, samples, DEPTH24_STENCIL8, width, height)
        GLWrapper.framebufferRenderbuffer(FRAMEBUFFER, DEPTH_STENCIL_ATTACHMENT, RENDERBUFFER, depthRboId)

        val status = GLWrapper.checkFramebufferStatus(FRAMEBUFFER)
        if (status != FRAMEBUFFER_COMPLETE) {
            println("[MultisampleFramebuffer] Incomplete")
            GLWrapper.bindFramebuffer(FRAMEBUFFER, 0)
            destroy()
            return false
        }

        GLWrapper.bindFramebuffer(FRAMEBUFFER, 0)
        isCreated = true
        return true
    }

    /**
     * Binds for rendering.
     */
    fun bind() {
        if (!isCreated) return
        GLWrapper.bindFramebuffer(FRAMEBUFFER, fboId)
        GLWrapper.viewport(0, 0, width, height)
    }

    /** GL name of the multisampled framebuffer object. */
    fun getId(): Int = fboId

    /**
     * Resizes the framebuffer.
     */
    fun resize(newWidth: Int, newHeight: Int) {
        if (newWidth == width && newHeight == height) return
        if (!isCreated) {
            width = newWidth
            height = newHeight
            return
        }

        width = newWidth
        height = newHeight

        // Resize multisample renderbuffers
        GLWrapper.bindRenderbuffer(RENDERBUFFER, colorRboId)
        GLWrapper.renderbufferStorageMultisample(RENDERBUFFER, samples, RGBA8, width, height)

        GLWrapper.bindRenderbuffer(RENDERBUFFER, depthRboId)
        GLWrapper.renderbufferStorageMultisample(RENDERBUFFER, samples, DEPTH24_STENCIL8, width, height)

        GLWrapper.bindRenderbuffer(RENDERBUFFER, 0)
    }

    /**
     * Destroys all resources.
     */
    fun destroy() {
        if (colorRboId != 0) {
            GLWrapper.deleteRenderbuffers(colorRboId)
            colorRboId = 0
        }

        if (depthRboId != 0) {
            GLWrapper.deleteRenderbuffers(depthRboId)
            depthRboId = 0
        }

        if (fboId != 0) {
            GLWrapper.deleteFramebuffers(fboId)
            fboId = 0
        }

        isCreated = false
    }
}
