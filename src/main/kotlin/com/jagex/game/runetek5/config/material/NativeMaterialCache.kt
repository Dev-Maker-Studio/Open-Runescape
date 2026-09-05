package com.jagex.game.runetek5.config.material

import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLFormat

/**
 * Cache for native material textures with animation support.
 */
class NativeMaterialCache(
    @JvmField val toolkit: AbstractNativeToolkit,
    @JvmField val materialProvider: MaterialProvider
) {

    @JvmField val cache = LRUCache(256)

    /**
     * Animates and retrieves a texture for the specified material ID.
     *
     * @param materialId The material ID to animate
     * @return The animated texture sampler, or null if the material is not cached
     */
    fun animateTexture(materialId: Int): NativeTextureSampler? {
        val baseId = -1
        val hash = (baseId.toLong() shl 16) or materialId.toLong()
        val cachedTexture = cache.get(hash)
        if (cachedTexture != null) {
            return cachedTexture as NativeTextureSampler
        }
        if (!materialProvider!!.isCached(materialId)) {
            return null
        }
        val material = materialProvider!!.getMaterial(materialId)
        val size = if (material.isSmall) 64 else toolkit.materialSize.toInt()
        val texture: NativeTextureSampler
        if (material.hdr && toolkit.bloomEnabled()) {
            val pixelsHdr = materialProvider!!.getPixelsHdr(materialId, size, size)
            texture = toolkit.createHdrTexture(GLFormat.RGBA, size, size, pixelsHdr)
        } else {
            val pixels: IntArray
            if (material.effectCombiner != 2 && MaterialTypeList.isOpaqueEffect(material.effectId.toInt())) {
                pixels = materialProvider!!.getPixels(materialId, 0.7f, size, size, true)
            } else {
                pixels = materialProvider!!.getPixelsArgb(materialId, size, size)
            }
            texture = toolkit.createTexture(size, size, true, pixels)
        }
        texture.setRepeat(material.repeatS, material.repeatT)
        cache.put(texture, hash)
        return texture
    }

    /**
     * Processes a cache cycle, cleaning up old entries.
     */
    fun processCycle() {
        cache.clean(5)
    }

    /**
     * Resets the cache, clearing all cached textures.
     */
    fun reset() {
        cache.reset()
    }

    /**
     * Retrieves a texture sampler for the specified ID.
     *
     * @param id The material ID
     * @return The texture sampler
     */
    fun getTextureSampler(id: Int): NativeTextureSampler? {
        return animateTexture(id)
    }
}
