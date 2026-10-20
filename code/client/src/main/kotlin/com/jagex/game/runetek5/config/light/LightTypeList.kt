package com.jagex.game.runetek5.config.light

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class LightTypeList(
    @JvmField val lightResource: Js5ResourceProvider
) {

    @JvmField val lightCache = LRUCache(64)

    init {
        lightResource.getArchiveFileCount(SharedConfigsType.LIGHT_INTENSITIES.id)
    }

    fun list(lightId: Int): LightType {
        var lightType: LightType?
        synchronized(lightCache) {
            lightType = lightCache.get(lightId.toLong()) as LightType?
        }
        if (lightType != null) {
            return lightType!!
        }
        val lightData: ByteArray?
        synchronized(lightResource) {
            lightData = lightResource.getFile(SharedConfigsType.LIGHT_INTENSITIES.id, lightId)
        }
        lightType = LightType()
        if (lightData != null) {
            lightType!!.decode(Packet(lightData))
        }
        synchronized(lightCache) {
            lightCache.put(lightType, lightId.toLong())
            return lightType!!
        }
    }

    fun cacheReset() {
        synchronized(lightCache) {
            lightCache.reset()
        }
    }

    fun cacheClean() {
        synchronized(lightCache) {
            lightCache.clean(5)
        }
    }

    fun cacheResize() {
        synchronized(lightCache) {
            lightCache.resize()
        }
    }
}
