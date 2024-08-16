package com.jagex.game.runetek5.config.varp;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class VarpTypeList {

    public final int size;
    public final LRUCache vardefinitionCache = new LRUCache(64);
    public final Js5ResourceProvider resourceProvider;

    public VarpTypeList(Js5ResourceProvider vardefinitionResourceprovider) {
        this.resourceProvider = vardefinitionResourceprovider;
        if (this.resourceProvider != null) {
        	this.size = this.resourceProvider.getArchiveFileCount(SharedConfigsType.VARS.id);
        } else {
        	this.size = 0;
        }
    }

    public VarpType list(int id) {
        VarpType varType;
        synchronized (this.vardefinitionCache) {
            varType = (VarpType) this.vardefinitionCache.get(id);
        }
        if (varType != null) {
            return varType;
        } 
        byte[] data;
        synchronized (this.resourceProvider) {
            data = this.resourceProvider.getFile(SharedConfigsType.VARS.id, id);
        }
        varType = new VarpType();
        if (data != null) {
            varType.decode(new Packet(data));
        }
        synchronized (this.vardefinitionCache) {
            this.vardefinitionCache.put(varType, id);
            return varType;
        }
    }

    public void sweepCache() {
        synchronized (this.vardefinitionCache) {
            this.vardefinitionCache.clean(5);
        }
    }

    public void resetCache() {
        synchronized (this.vardefinitionCache) {
            this.vardefinitionCache.reset();
        }
    }

    public void clearCacheQueue() {
        synchronized (this.vardefinitionCache) {
            this.vardefinitionCache.resize();
        }
    }
}
