package com.jagex.game.runetek5.graphics.modern.texture

import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.CLAMP_TO_EDGE
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.LINEAR
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.LINEAR_MIPMAP_LINEAR
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.NEAREST
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.REPEAT
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.RGBA
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.RGBA8
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_2D
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_MAG_FILTER
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_MIN_FILTER
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_WRAP_S
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TEXTURE_WRAP_T
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_BYTE
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * Modern texture management system.
 *
 * Handles texture creation, caching, and binding for the modern pipeline.
 * Supports mipmapping, filtering modes, and various pixel formats.
 */
class TextureManager {

    private val textures = mutableMapOf<Int, ManagedTexture>()
    private val texturesByName = mutableMapOf<String, Int>()
    private var nextTextureId = 1

    // Currently bound textures per unit
    private val boundTextures = IntArray(16) { 0 }
    private var activeUnit = 0

    /**
     * Creates a new texture from RGBA pixel data.
     *
     * @param width Texture width in pixels
     * @param height Texture height in pixels
     * @param pixels RGBA pixel data (4 bytes per pixel)
     * @param options Texture creation options
     * @return Texture handle for binding
     */
    fun createTexture(
        width: Int,
        height: Int,
        pixels: ByteArray,
        options: TextureOptions = TextureOptions()
    ): Int {
        require(pixels.size >= width * height * 4) { "Pixel data too small" }

        val glId = GLWrapper.genTextures()
        GLWrapper.bindTexture(TEXTURE_2D, glId)

        // Set filtering
        val minFilter = if (options.generateMipmaps) {
            if (options.filtering == FilterMode.LINEAR) LINEAR_MIPMAP_LINEAR else NEAREST
        } else {
            if (options.filtering == FilterMode.LINEAR) LINEAR else NEAREST
        }
        val magFilter = if (options.filtering == FilterMode.LINEAR) LINEAR else NEAREST

        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, minFilter)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, magFilter)

        // Set wrap mode
        val wrapMode = when (options.wrapMode) {
            WrapMode.REPEAT -> REPEAT
            WrapMode.CLAMP -> CLAMP_TO_EDGE
        }
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, wrapMode)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, wrapMode)

        // Upload pixel data
        val buffer = MemoryUtil.memAlloc(pixels.size)
        try {
            buffer.put(pixels).flip()
            GLWrapper.texImage2D(TEXTURE_2D, 0, RGBA8, width, height, 0, RGBA, UNSIGNED_BYTE, buffer)
        } finally {
            MemoryUtil.memFree(buffer)
        }

        // Generate mipmaps if requested
        if (options.generateMipmaps) {
            GLWrapper.generateMipmap(TEXTURE_2D)
        }

        GLWrapper.bindTexture(TEXTURE_2D, 0)

        val handle = nextTextureId++
        textures[handle] = ManagedTexture(
            handle = handle,
            glId = glId,
            width = width,
            height = height,
            hasMipmaps = options.generateMipmaps
        )

        return handle
    }

    /**
     * Creates a texture from a ByteBuffer.
     */
    fun createTexture(
        width: Int,
        height: Int,
        pixels: ByteBuffer,
        options: TextureOptions = TextureOptions()
    ): Int {
        val glId = GLWrapper.genTextures()
        GLWrapper.bindTexture(TEXTURE_2D, glId)

        // Set filtering
        val minFilter = if (options.generateMipmaps) {
            if (options.filtering == FilterMode.LINEAR) LINEAR_MIPMAP_LINEAR else NEAREST
        } else {
            if (options.filtering == FilterMode.LINEAR) LINEAR else NEAREST
        }
        val magFilter = if (options.filtering == FilterMode.LINEAR) LINEAR else NEAREST

        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, minFilter)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, magFilter)

        // Set wrap mode
        val wrapMode = when (options.wrapMode) {
            WrapMode.REPEAT -> REPEAT
            WrapMode.CLAMP -> CLAMP_TO_EDGE
        }
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, wrapMode)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, wrapMode)

        // Upload pixel data
        GLWrapper.texImage2D(TEXTURE_2D, 0, RGBA8, width, height, 0, RGBA, UNSIGNED_BYTE, pixels)

        // Generate mipmaps if requested
        if (options.generateMipmaps) {
            GLWrapper.generateMipmap(TEXTURE_2D)
        }

        GLWrapper.bindTexture(TEXTURE_2D, 0)

        val handle = nextTextureId++
        textures[handle] = ManagedTexture(
            handle = handle,
            glId = glId,
            width = width,
            height = height,
            hasMipmaps = options.generateMipmaps
        )

        return handle
    }

    /**
     * Creates an empty texture for render targets.
     */
    fun createRenderTexture(width: Int, height: Int, options: TextureOptions = TextureOptions()): Int {
        val glId = GLWrapper.genTextures()
        GLWrapper.bindTexture(TEXTURE_2D, glId)

        val minFilter = if (options.filtering == FilterMode.LINEAR) LINEAR else NEAREST
        val magFilter = minFilter

        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, minFilter)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, magFilter)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, CLAMP_TO_EDGE)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, CLAMP_TO_EDGE)

        // Allocate empty texture
        GLWrapper.texImage2D(TEXTURE_2D, 0, RGBA8, width, height, 0, RGBA, UNSIGNED_BYTE, null)

        GLWrapper.bindTexture(TEXTURE_2D, 0)

        val handle = nextTextureId++
        textures[handle] = ManagedTexture(
            handle = handle,
            glId = glId,
            width = width,
            height = height,
            hasMipmaps = false,
            isRenderTarget = true
        )

        return handle
    }

    /**
     * Registers an existing OpenGL texture with the manager.
     * Useful for integrating with legacy texture loading code.
     *
     * @param glId The existing OpenGL texture ID
     * @param width Texture width
     * @param height Texture height
     * @param name Optional name for lookup
     * @return Texture handle
     */
    fun registerExistingTexture(glId: Int, width: Int, height: Int, name: String? = null): Int {
        val handle = nextTextureId++
        textures[handle] = ManagedTexture(
            handle = handle,
            glId = glId,
            width = width,
            height = height,
            hasMipmaps = false,
            isExternal = true
        )
        if (name != null) {
            texturesByName[name] = handle
        }
        return handle
    }

    /**
     * Updates texture data.
     */
    fun updateTexture(handle: Int, x: Int, y: Int, width: Int, height: Int, pixels: ByteBuffer) {
        val texture = textures[handle] ?: return
        GLWrapper.bindTexture(TEXTURE_2D, texture.glId)
        GLWrapper.texSubImage2D(TEXTURE_2D, 0, x, y, width, height, RGBA, UNSIGNED_BYTE, pixels)
    }

    /**
     * Binds a texture to a texture unit.
     *
     * @param handle Texture handle (0 to unbind)
     * @param unit Texture unit (0-15)
     */
    fun bind(handle: Int, unit: Int = 0) {
        if (boundTextures[unit] == handle) return

        if (activeUnit != unit) {
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + unit)
            activeUnit = unit
        }

        val glId = if (handle == 0) 0 else textures[handle]?.glId ?: 0
        GLWrapper.bindTexture(TEXTURE_2D, glId)
        boundTextures[unit] = handle
    }

    /**
     * Gets the OpenGL texture ID for a handle.
     */
    fun getGLId(handle: Int): Int = textures[handle]?.glId ?: 0

    /**
     * Gets texture info.
     */
    fun getTexture(handle: Int): ManagedTexture? = textures[handle]

    /**
     * Gets a texture by name.
     */
    fun getTextureByName(name: String): Int = texturesByName[name] ?: 0

    /**
     * Deletes a texture.
     */
    fun deleteTexture(handle: Int) {
        val texture = textures.remove(handle) ?: return

        // Unbind if currently bound
        for (unit in boundTextures.indices) {
            if (boundTextures[unit] == handle) {
                bind(0, unit)
            }
        }

        // Don't delete external textures (managed elsewhere)
        if (!texture.isExternal) {
            GLWrapper.deleteTextures(texture.glId)
        }
    }

    /**
     * Deletes all textures.
     */
    fun deleteAll() {
        // Unbind all
        for (unit in boundTextures.indices) {
            if (boundTextures[unit] != 0) {
                bind(0, unit)
            }
        }

        // Delete non-external textures
        textures.values
            .filter { !it.isExternal }
            .forEach { GLWrapper.deleteTextures(it.glId) }

        textures.clear()
        texturesByName.clear()
    }

    /**
     * Gets statistics about texture usage.
     */
    fun getStats(): TextureStats {
        var totalMemory = 0L
        textures.values.forEach { tex ->
            val bpp = 4 // RGBA8
            var size = tex.width.toLong() * tex.height * bpp
            if (tex.hasMipmaps) {
                size = (size * 1.33).toLong() // Approximate mipmap overhead
            }
            totalMemory += size
        }
        return TextureStats(
            textureCount = textures.size,
            estimatedMemoryBytes = totalMemory
        )
    }
}

/**
 * Texture creation options.
 */
data class TextureOptions(
    val filtering: FilterMode = FilterMode.LINEAR,
    val wrapMode: WrapMode = WrapMode.REPEAT,
    val generateMipmaps: Boolean = true
)

/**
 * Texture filtering modes.
 */
enum class FilterMode {
    NEAREST,
    LINEAR
}

/**
 * Texture wrap modes.
 */
enum class WrapMode {
    REPEAT,
    CLAMP
}

/**
 * Information about a managed texture.
 */
data class ManagedTexture(
    val handle: Int,
    val glId: Int,
    val width: Int,
    val height: Int,
    val hasMipmaps: Boolean = false,
    val isRenderTarget: Boolean = false,
    val isExternal: Boolean = false
)

/**
 * Texture manager statistics.
 */
data class TextureStats(
    val textureCount: Int,
    val estimatedMemoryBytes: Long
)
