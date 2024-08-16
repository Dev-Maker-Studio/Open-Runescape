package com.jagex.game.runetek5.config.flo;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

/**
 * < Fully refactored >
 * @author Brainpower - 28/05/2020
 * 
 * Manages overlay floor files
 *
 */
public class FloTypeList {

	/**
	 * The last decoded overlay
	 */
    public int lastLoadedOverlayId;
    
    /**
     * The total amount of overlay floors in the cache
     */
    public final int totalFileCount;
    
    /**
     * Cached floors by most used
     */
    public final LRUCache overlayCache = new LRUCache(64);
    
    /**
     * The resource managing all flo files
     */
    public final Js5ResourceProvider floResource;

    /**
     * Load file count and set resource
     */
    public FloTypeList(Js5ResourceProvider overlayFloorResource) {
        this.floResource = overlayFloorResource;
        this.totalFileCount = this.floResource.getArchiveFileCount(SharedConfigsType.OVERLAYS.id);
    }

    /**
     * Decode a flo file by it's id
     */
    public FloType list(int overlayId) {
        FloType floType;
        synchronized (this.overlayCache) {
            floType = (FloType) this.overlayCache.get(overlayId);
        }
        if (floType != null) {
            return floType;
        } 
        byte[] floData;
        synchronized (this.floResource) {
            floData = this.floResource.getFile(SharedConfigsType.OVERLAYS.id, overlayId);
        }
        floType = new FloType();
        floType.floTypeList = this;
        floType.overlayId = overlayId;
        if (floData != null) {
            floType.decode(new Packet(floData));
        }
        floType.postDecode();
        synchronized (this.overlayCache) {
            this.overlayCache.put(floType, overlayId);
            return floType;
        }
    }

    /**
     * Reset the type cache
     */
    public void reset() {
        synchronized (this.overlayCache) {
            this.overlayCache.reset();
        }
    }

    /**
     * Increase cycle on cache entries
     * to keep track of how long each one has existed
     */
    public void cycle() {
        synchronized (this.overlayCache) {
            this.overlayCache.clean(5);
        }
    }

    /**
     * Remove all entries from the queue
     */
    public void clear() {
        synchronized (this.overlayCache) {
            this.overlayCache.resize();
        }
    }
}
