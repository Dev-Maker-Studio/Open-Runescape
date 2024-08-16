package com.jagex.game.runetek5.config.flu;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class FluTypeList {

    public final LRUCache cache = new LRUCache(128);

    public final Js5ResourceProvider underlayResource;

    public FluTypeList(Js5ResourceProvider fluResource) {
        this.underlayResource = fluResource;
        this.underlayResource.getArchiveFileCount(SharedConfigsType.UNDERLAYS.id);
    }

    public FluType list(int underlayId) {
        FluType flutype;
        synchronized (this.cache) {
            flutype = (FluType) this.cache.get(underlayId);
        }
        if (flutype != null) {
            return flutype;
        } 
        byte[] fluData;
        synchronized (this.underlayResource) {
            fluData = this.underlayResource.getFile(SharedConfigsType.UNDERLAYS.id, underlayId);
        }
        flutype = new FluType();
        if (fluData != null) {
            flutype.decode(new Packet(fluData));
        }
        synchronized (this.cache) {
            this.cache.put(flutype, underlayId);
            return flutype;
        }
    }

    public void resetCache() {
        synchronized (this.cache) {
            this.cache.reset();
        }
    }

    public void processCycle() {
        synchronized (this.cache) {
            this.cache.clean(5);
        }
    }

    public void clearQueue() {
        synchronized (this.cache) {
            this.cache.resize();
        }
    }
}
