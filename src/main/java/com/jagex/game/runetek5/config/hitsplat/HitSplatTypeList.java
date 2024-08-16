package com.jagex.game.runetek5.config.hitsplat;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class HitSplatTypeList {

    public final LRUCache spriteCache = new LRUCache(20);
    public final LRUCache hitsplatTypeCache = new LRUCache(64);
    public final Js5ResourceProvider spriteResource;
    public final Js5ResourceProvider hitsplatResource;

    public HitSplatTypeList(Js5ResourceProvider hitsplatResource, Js5ResourceProvider spriteResource) {
        this.spriteResource = spriteResource;
        this.hitsplatResource = hitsplatResource;
        this.hitsplatResource.getArchiveFileCount(SharedConfigsType.HITSPLATS.id);
    }

    public HitSplatType list(int typeId) {
        HitSplatType hitsplattype;
        synchronized (this.hitsplatTypeCache) {
            hitsplattype = (HitSplatType) this.hitsplatTypeCache.get(typeId);
        }
        if (hitsplattype != null) {
            return hitsplattype;
        } 
        byte[] hitsplatData;
        synchronized (this.hitsplatResource) {
            hitsplatData = this.hitsplatResource.getFile(SharedConfigsType.HITSPLATS.id, typeId);
        }
        hitsplattype = new HitSplatType();
        hitsplattype.typelist = this;
        if (hitsplatData != null) {
            hitsplattype.decode(new Packet(hitsplatData));
        }
        synchronized (this.hitsplatTypeCache) {
            this.hitsplatTypeCache.put(hitsplattype, typeId);
            return hitsplattype;
        }
    }
    
    public void processCacheCycle() {
        synchronized (this.hitsplatTypeCache) {
            this.hitsplatTypeCache.clean(5);
        }
        synchronized (this.spriteCache) {
            this.spriteCache.clean(5);
        }
    }

    public void resetCaches() {
        synchronized (this.hitsplatTypeCache) {
            this.hitsplatTypeCache.reset();
        }
        synchronized (this.spriteCache) {
            this.spriteCache.reset();
        }
    }

    public void clearQueues() {
        synchronized (this.hitsplatTypeCache) {
            this.hitsplatTypeCache.resize();
        }
        synchronized (this.spriteCache) {
            this.spriteCache.resize();
        }
    }
}
