package com.jagex.game.runetek5.config.sun;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class SunTypeList {

    public final LRUCache cache = new LRUCache(16);

    public final Js5ResourceProvider sunResource;

    public SunTypeList(Js5ResourceProvider sunResource) {
        this.sunResource = sunResource;
        this.sunResource.getArchiveFileCount(SharedConfigsType.SUN.id);
    }

    public SunType list(int id) {
        SunType sunType;
        synchronized (this.cache) {
            sunType = (SunType) this.cache.get(id);
        }
        if (sunType != null) {
            return sunType;
        } 
        byte[] data;
        synchronized (this.sunResource) {
            data = this.sunResource.getFile(SharedConfigsType.SUN.id, id);
        }
        sunType = new SunType();
        if (data != null) {
            sunType.decode(new Packet(data));
        }
        synchronized (this.cache) {
        	this.cache.put(sunType, id);
            return sunType;
        }
    }

    public void resetCache() {
        synchronized (this.cache) {
        	this.cache.reset();
        }
    }

    public void sweepCache() {
        synchronized (this.cache) {
        	this.cache.clean(5);
        }
    }

    public void clearCacheQueue() {
        synchronized (this.cache) {
        	this.cache.resize();
        }
    }
}
