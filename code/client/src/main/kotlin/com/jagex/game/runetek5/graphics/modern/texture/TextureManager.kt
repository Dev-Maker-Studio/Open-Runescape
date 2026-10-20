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

class TextureManager {

    private val textures = mutableMapOf<Int, ManagedTexture>()
    private val texturesByName = mutableMapOf<String, Int>()
    private var nextTextureId = 1

    private val boundTextures = IntArray(16) { 0 }
    private var activeUnit = 0

    fun createTexture(
        width: Int,
        height: Int,
        pixels: ByteArray,
        options: TextureOptions = TextureOptions()
    ): Int {
        require(pixels.size >= width * height * 4) { "Pixel data too small" }

        val glId = GLWrapper.genTextures()
        GLWrapper.bindTexture(TEXTURE_2D, glId)

        val minFilter = if (options.generateMipmaps) {
            if (options.filtering == FilterMode.LINEAR) LINEAR_MIPMAP_LINEAR else NEAREST
        } else {
            if (options.filtering == FilterMode.LINEAR) LINEAR else NEAREST
        }
        val magFilter = if (options.filtering == FilterMode.LINEAR) LINEAR else NEAREST

        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, minFilter)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, magFilter)

        val wrapMode = when (options.wrapMode) {
            WrapMode.REPEAT -> REPEAT
            WrapMode.CLAMP -> CLAMP_TO_EDGE
        }
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, wrapMode)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, wrapMode)

        val buffer = MemoryUtil.memAlloc(pixels.size)
        try {
            buffer.put(pixels).flip()
            GLWrapper.texImage2D(TEXTURE_2D, 0, RGBA8, width, height, 0, RGBA, UNSIGNED_BYTE, buffer)
        } finally {
            MemoryUtil.memFree(buffer)
        }

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

    fun createTexture(
        width: Int,
        height: Int,
        pixels: ByteBuffer,
        options: TextureOptions = TextureOptions()
    ): Int {
        val glId = GLWrapper.genTextures()
        GLWrapper.bindTexture(TEXTURE_2D, glId)

        val minFilter = if (options.generateMipmaps) {
            if (options.filtering == FilterMode.LINEAR) LINEAR_MIPMAP_LINEAR else NEAREST
        } else {
            if (options.filtering == FilterMode.LINEAR) LINEAR else NEAREST
        }
        val magFilter = if (options.filtering == FilterMode.LINEAR) LINEAR else NEAREST

        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, minFilter)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, magFilter)

        val wrapMode = when (options.wrapMode) {
            WrapMode.REPEAT -> REPEAT
            WrapMode.CLAMP -> CLAMP_TO_EDGE
        }
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, wrapMode)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, wrapMode)

        GLWrapper.texImage2D(TEXTURE_2D, 0, RGBA8, width, height, 0, RGBA, UNSIGNED_BYTE, pixels)

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

    fun createRenderTexture(width: Int, height: Int, options: TextureOptions = TextureOptions()): Int {
        val glId = GLWrapper.genTextures()
        GLWrapper.bindTexture(TEXTURE_2D, glId)

        val minFilter = if (options.filtering == FilterMode.LINEAR) LINEAR else NEAREST
        val magFilter = minFilter

        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, minFilter)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, magFilter)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, CLAMP_TO_EDGE)
        GLWrapper.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, CLAMP_TO_EDGE)

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

    fun updateTexture(handle: Int, x: Int, y: Int, width: Int, height: Int, pixels: ByteBuffer) {
        val texture = textures[handle] ?: return
        GLWrapper.bindTexture(TEXTURE_2D, texture.glId)
        GLWrapper.texSubImage2D(TEXTURE_2D, 0, x, y, width, height, RGBA, UNSIGNED_BYTE, pixels)
    }

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

    fun getGLId(handle: Int): Int = textures[handle]?.glId ?: 0

    fun getTexture(handle: Int): ManagedTexture? = textures[handle]

    fun getTextureByName(name: String): Int = texturesByName[name] ?: 0

    fun deleteTexture(handle: Int) {
        val texture = textures.remove(handle) ?: return

        for (unit in boundTextures.indices) {
            if (boundTextures[unit] == handle) {
                bind(0, unit)
            }
        }

        if (!texture.isExternal) {
            GLWrapper.deleteTextures(texture.glId)
        }
    }

    fun deleteAll() {
        for (unit in boundTextures.indices) {
            if (boundTextures[unit] != 0) {
                bind(0, unit)
            }
        }

        textures.values
            .filter { !it.isExternal }
            .forEach { GLWrapper.deleteTextures(it.glId) }

        textures.clear()
        texturesByName.clear()
    }

    fun getStats(): TextureStats {
        var totalMemory = 0L
        textures.values.forEach { tex ->
            val bpp = 4
            var size = tex.width.toLong() * tex.height * bpp
            if (tex.hasMipmaps) {
                size = (size * 1.33).toLong()
            }
            totalMemory += size
        }
        return TextureStats(
            textureCount = textures.size,
            estimatedMemoryBytes = totalMemory
        )
    }
}

data class TextureOptions(
    val filtering: FilterMode = FilterMode.LINEAR,
    val wrapMode: WrapMode = WrapMode.REPEAT,
    val generateMipmaps: Boolean = true
)

enum class FilterMode {
    NEAREST,
    LINEAR
}

enum class WrapMode {
    REPEAT,
    CLAMP
}

data class ManagedTexture(
    val handle: Int,
    val glId: Int,
    val width: Int,
    val height: Int,
    val hasMipmaps: Boolean = false,
    val isRenderTarget: Boolean = false,
    val isExternal: Boolean = false
)

data class TextureStats(
    val textureCount: Int,
    val estimatedMemoryBytes: Long
)
