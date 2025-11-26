package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.config.Resource
import java.util.LinkedList

/**
 * Asynchronous map region loader.
 *
 * @author Brainpower - 11/03/2023
 */
class AsyncMapLoader : Runnable {

    @JvmField var region: MapRegion = MapRegion(true)
    @Volatile
    @JvmField var loadingRegion: Boolean = false
    @JvmField val regionLoadRequests: LinkedList<RegionLoadRequest> = LinkedList()

    fun requestMapUpdate(regionLoadRequest: RegionLoadRequest) {
        synchronized(regionLoadRequests) {
            regionLoadRequests.add(regionLoadRequest)
            (regionLoadRequests as Object).notify()
        }
    }

    fun setMapRegion(mapRegion: MapRegion) {
        region = mapRegion
    }

    fun isLoadingRegion(): Boolean {
        return loadingRegion
    }

    fun getMapRegion(): MapRegion {
        return region
    }

    fun decodeAll() {
        val request: RegionLoadRequest?
        synchronized(regionLoadRequests) {
            try {
                (regionLoadRequests as Object).wait()
            } catch (ie: InterruptedException) {
                // NOOP
            }
            request = regionLoadRequests.pollFirst()
        }
        try {
            try {
                if (request != null) {
                    loadingRegion = true
                    decode(request)
                }
            } catch (exception: Exception) {
                loadingRegion = false
                return
            }
            loadingRegion = false
        } catch (exception: Exception) {
            loadingRegion = false
        }
    }

    fun decode(regionLoadRequest: RegionLoadRequest) {
        if (regionLoadRequest.loadingType == RegionLoadingType.RELOAD) {
            region.reload()
        } else {
            region.initialize(regionLoadRequest)
        }
        var regionLoaded = region.loadRegion()
        while (!regionLoaded) {
            regionLoaded = region.loadRegion()
        }

        region.setLoadingRegion()
        Resource.mapRegion!!.setLoadingRegion()
    }

    fun initializeRegion(loctypelist: com.jagex.game.runetek5.config.`object`.ObjectTypeList) {
        region.setLoctypelist(loctypelist)
    }

    override fun run() {
        while (true) {
            decodeAll()
        }
    }
}
