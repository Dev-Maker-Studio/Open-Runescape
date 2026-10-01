package com.jagex.game.runetek5.scene

import com.darkan.crash.CrashHandler
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.sound.player.area.AmbientSound
import com.jagex.game.runetek5.toolkit.SafeModeToolkit
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

    /** Game thread only. The scene the server's zone packets are relative to while it is still being built. */
    @JvmField var requestedFrame: RegionLoadRequest.SceneFrame? = null

    @Volatile
    private var buildFailed = false

    fun load(request: RegionLoadRequest) {
        if (Renderers.activeToolkit !is SafeModeToolkit) {
            requestMapUpdate(request)
            return
        }
        requestedFrame = null
        if (request.loadingType == RegionLoadingType.RELOAD) {
            Resource.mapRegion!!.reload()
            ComponentMinimap.reset()
        } else {
            Resource.mapRegion!!.initialize(request)
        }
    }

    private fun requestMapUpdate(regionLoadRequest: RegionLoadRequest) {
        regionLoadRequest.peekSceneFrame()?.let { requestedFrame = it }
        // Claimed here, not on the loader thread: the game thread must never observe IDLE with a request queued.
        synchronized(stateLock) {
            if (state == State.IDLE) {
                state = State.BUILDING
            }
        }
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

    fun initializeRegion(loctypelist: ObjectTypeList) {
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
                build(request)
                synchronized(stateLock) {
                    pendingRegion = region
                    state = State.READY
                    while (state != State.IDLE) {
                        (stateLock as Object).wait()
                    }
                }
            } catch (t: Throwable) {
                CrashHandler.noteError("async map build failed", t)
                synchronized(stateLock) {
                    pendingRegion = null
                    buildFailed = true
                    state = State.IDLE
                }
            }
        }
    }

    private fun build(request: RegionLoadRequest) {
        val requestStart = request.buffer?.currentPosition ?: 0
        var attempt = 1
        while (true) {
            // Buffer loc ambient sounds created during the build; the game thread
            // iterates the live sound lists every tick, so they are drained into
            // place by finalizeAsyncRegionSwap instead.
            region.pendingAmbientSounds = NodeCollection()
            AmbientSound.DEFERRED_LOC_SOUNDS.set(region.pendingAmbientSounds)
            try {
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
                return
            } catch (e: Exception) {
                if (attempt++ == BUILD_ATTEMPTS) {
                    throw e
                }
                e.printStackTrace()
                request.buffer?.currentPosition = requestStart
            } finally {
                AmbientSound.DEFERRED_LOC_SOUNDS.set(null)
            }
        }
    }

    fun takeBuildFailure(): Boolean {
        if (!buildFailed) {
            return false
        }
        buildFailed = false
        return true
    }

    private companion object {
        const val BUILD_ATTEMPTS = 3
    }
}
