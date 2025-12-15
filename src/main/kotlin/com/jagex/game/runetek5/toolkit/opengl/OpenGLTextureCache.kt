package com.jagex.game.runetek5.toolkit.opengl

import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.material.MaterialTypeList
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.OpenGLConventions.*

/**
 * Cache for OpenGL 2D textures with material support.
 */
class OpenGLTextureCache(
    @JvmField val toolkit: OpenGlToolkit,
    @JvmField val materialProvider: MaterialProvider
) {

    @JvmField val textureCache = LRUCache(256)

    /**
     * Retrieves a texture for the specified material ID.
     *
     * @param textureId The texture/material ID
     * @return The OpenGL texture, or null if not cached
     */
    fun getTexture(textureId: Int): OpenGl2DTexture? {
        var textureSize = -1
        val cachedTexture = textureCache.get(textureId.toLong())
        if (cachedTexture != null) {
            return cachedTexture as OpenGl2DTexture
        }
        if (!materialProvider!!.isCached(textureId)) {
            return null
        }
        val material = materialProvider!!.getMaterial(textureId)
        textureSize = if (material.isSmall) 64 else toolkit.currentTextureSize

        val texture: OpenGl2DTexture
        if (material.hdr && toolkit.bloomEnabled()) {
            val pixels = materialProvider!!.getPixelsHdr(textureId, textureSize, textureSize)
            texture = OpenGl2DTexture(
                toolkit, GLFormat.HDR, GLActionFormat.FLOAT_16_ARB,
                textureSize, textureSize, material.mipmapping.toInt() != 0, pixels, GLFormat.HDR
            )
        } else {
            val pixels: IntArray
            if (material.effectCombiner != 2 && MaterialTypeList.isOpaqueEffect(material.effectId.toInt())) {
                pixels = materialProvider!!.getPixels(textureId, 0.7f, textureSize, textureSize, true)
            } else {
                pixels = materialProvider!!.getPixelsArgb(textureId, textureSize, textureSize)
            }
            texture = OpenGl2DTexture(
                toolkit, GL_TEXTURE_2D, textureSize, textureSize,
                material.mipmapping.toInt() != 0, pixels, 0, 0, false
            )
        }

        texture.setRepeat(material.repeatS, material.repeatT)
        textureCache.put(texture, textureId.toLong())
        return texture
    }

    /**
     * Alias for getTexture().
     *
     * @param textureId The texture ID
     * @return The OpenGL texture
     */
    fun get(textureId: Int): OpenGl2DTexture? {
        return getTexture(textureId)
    }

    /**
     * Cycles the bloom cache, cleaning up old entries.
     */
    fun cycleBloomCache() {
        textureCache.clean(5)
    }

    /**
     * Resets the bloom cache, clearing all cached textures.
     */
    fun resetBloomCache() {
        textureCache.reset()
    }
}
