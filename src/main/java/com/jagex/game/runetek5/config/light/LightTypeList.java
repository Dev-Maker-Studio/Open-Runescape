package com.jagex.game.runetek5.config.light;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

/**
 * < Mostly refactored >
 * @author Brainpower - 30/05/2020
 * 
 * Manages #LightType
 *
 */
public class LightTypeList {

    public final LRUCache lightCache = new LRUCache(64);
    public final Js5ResourceProvider lightResource;

    public LightTypeList(Js5ResourceProvider lightResource) {
        this.lightResource = lightResource;
        this.lightResource.getArchiveFileCount(SharedConfigsType.LIGHT_INTENSITIES.id);
    }

    public LightType list(int lightId) {
        LightType lightType;
        synchronized (this.lightCache) {
            lightType = (LightType) this.lightCache.get(lightId);
        }
        if (lightType != null) {
            return lightType;
        } 
        byte[] lightData;
        synchronized (this.lightResource) {
            lightData = this.lightResource.getFile(SharedConfigsType.LIGHT_INTENSITIES.id, lightId);
        }
        lightType = new LightType();
        if (lightData != null) {
            lightType.decode(new Packet(lightData));
        }
        synchronized (this.lightCache) {
            this.lightCache.put(lightType, lightId);
            return lightType;
        }
    }

    public void cacheReset() {
        synchronized (this.lightCache) {
            this.lightCache.reset();
        }
    }

    public void cacheClean() {
        synchronized (this.lightCache) {
            this.lightCache.clean(5);
        }
    }

    public void cacheResize() {
        synchronized (this.lightCache) {
            this.lightCache.resize();
        }
    }
}
