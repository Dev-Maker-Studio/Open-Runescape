package com.jagex.game.runetek5.config.skybox

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.SharedConfigsType
import com.jagex.game.runetek5.config.sun.Sun
import com.jagex.game.runetek5.config.sun.SunTypeList
import com.jagex.game.runetek5.graphics.light.Skybox
import com.jagex.game.runetek5.js5.Js5ResourceProvider

class SkyboxTypeList(
    @JvmField val skyboxResource: Js5ResourceProvider
) {

    @JvmField val cache: LRUCache = LRUCache(16)

    init {
        skyboxResource.getArchiveFileCount(SharedConfigsType.SKYBOX.id)
    }

    fun cacheClean() {
        synchronized(cache) {
            cache.clean(5)
        }
    }

    fun cacheReset() {
        synchronized(cache) {
            cache.reset()
        }
    }

    fun cacheResize() {
        synchronized(cache) {
            cache.resize()
        }
    }

    fun list(fileId: Int): SkyboxType {
        var skyboxType: SkyboxType?
        synchronized(cache) {
            skyboxType = cache.get(fileId.toLong()) as SkyboxType?
        }
        if (skyboxType != null) {
            return skyboxType!!
        }
        val data: ByteArray?
        synchronized(skyboxResource) {
            data = skyboxResource.getFile(SharedConfigsType.SKYBOX.id, fileId)
        }
        skyboxType = SkyboxType()
        if (data != null) {
            skyboxType!!.decode(Packet(data))
        }
        synchronized(cache) {
            cache.put(skyboxType, fileId.toLong())
            return skyboxType!!
        }
    }

    fun generateSkybox(skyboxId: Int, x: Int, y: Int, z: Int, suntypelist: SunTypeList): Skybox {
        var suns: Array<Sun?>? = null
        val skyboxType = list(skyboxId)
        if (skyboxType.sunDefinitionIds != null) {
            suns = arrayOfNulls(skyboxType.sunDefinitionIds!!.size)
            for (index in suns.indices) {
                val sunType = suntypelist.list(skyboxType.sunDefinitionIds!![index])
                suns[index] = Sun(
                    sunType.actionType,
                    sunType.materialId,
                    sunType.positionX,
                    sunType.positionY,
                    sunType.positionZ,
                    sunType.positionOffsetStep,
                    sunType.displayColor,
                    sunType.invertPosition,
                    sunType.rotationX,
                    sunType.rotationY,
                    sunType.rotationZ
                )
            }
        }
        return Skybox(
            skyboxType.materialId,
            suns,
            skyboxType.defaultSunIndex,
            x,
            y,
            z,
            skyboxType.backgroundMode,
            skyboxType.archiveId
        )
    }
}
