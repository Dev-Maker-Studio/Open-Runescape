package com.jagex.game.runetek5.scene

import java.util.LinkedList

/**
 * Asynchronous map region loader.
 *
 * Builds new map regions on a dedicated background thread while the game thread keeps
 * rendering the current region. The background build is CPU-only (JS5 fetch + decode);
 * all GL uploads and shared-state mutation happen in [MapRegion.finalizeAsyncRegionSwap],
 * which the game thread runs at the top of its pulse once the loader parks in [State.READY].
 *
 * @author Brainpower - 11/03/2023
 */
class AsyncMapLoader : Runnable {

    enum class State {
        /** No build in progress. */
        IDLE,

        /** Loader thread is fetching/decoding a region. */
        BUILDING,

        /** Build complete; loader is parked waiting for the game thread to finalize. */
        READY,

        /** Game thread is running [MapRegion.finalizeAsyncRegionSwap]. */
        FINALIZING
    }

    @JvmField var region: MapRegion = MapRegion(true)

    @Volatile
    @JvmField var state: State = State.IDLE

    /** Guards [state]/[pendingRegion] transitions between READY and IDLE. */
    @JvmField val stateLock = Object()

    /** The fully built region awaiting finalization, non-null only in READY/FINALIZING. */
    @Volatile
    @JvmField var pendingRegion: MapRegion? = null

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
        return state != State.IDLE
    }

    fun getMapRegion(): MapRegion {
        return region
    }

    fun initializeRegion(loctypelist: com.jagex.game.runetek5.config.`object`.ObjectTypeList) {
        region.setLoctypelist(loctypelist)
    }

    override fun run() {
        while (true) {
            val request = synchronized(regionLoadRequests) {
                while (regionLoadRequests.isEmpty()) {
                    try {
                        (regionLoadRequests as Object).wait()
                    } catch (ie: InterruptedException) {
                        // NOOP
                    }
                }
                regionLoadRequests.pollFirst()!!
            }
            try {
                // Under stateLock so the game thread can atomically check IDLE before
                // touching this loader's region (e.g. GraphicsCache.resetGraphicCaches).
                synchronized(stateLock) {
                    state = State.BUILDING
                }
                if (request.loadingType == RegionLoadingType.RELOAD) {
                    region.reload()
                } else {
                    region.initialize(request)
                }
                while (!region.loadRegion()) {
                    // Re-entrant: false means JS5 data is still downloading; the game
                    // thread's file requester pumps it, so pace the retries.
                    Thread.sleep(10)
                }
                synchronized(stateLock) {
                    pendingRegion = region
                    state = State.READY
                    while (state != State.IDLE) {
                        (stateLock as Object).wait()
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                synchronized(stateLock) {
                    pendingRegion = null
                    state = State.IDLE
                }
            }
        }
    }
}
