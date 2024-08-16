package com.jagex.game.runetek5.config.hitbar;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class HitbarTypeList {

    public final LRUCache hitbarCache = new LRUCache(64);
    final LRUCache spriteCache = new LRUCache(20);
    final Js5ResourceProvider spriteResource;
    public final Js5ResourceProvider hitbarResource;

    public HitbarTypeList(Js5ResourceProvider hitbarResource, Js5ResourceProvider spriteResource) {
        this.spriteResource = spriteResource;
        this.hitbarResource = hitbarResource;
        this.hitbarResource.getArchiveFileCount(SharedConfigsType.HITBARS.id);
    }

    public HitbarType list(int id) {
        HitbarType hitbarType;
        synchronized (this.hitbarCache) {
            hitbarType = (HitbarType) this.hitbarCache.get(id);
        }
        if (hitbarType != null) {
            return hitbarType;
        } 
        byte[] hitbarData;
        synchronized (this.hitbarResource) {
            hitbarData = this.hitbarResource.getFile(SharedConfigsType.HITBARS.id, id);
        }
        hitbarType = new HitbarType();
        hitbarType.hitbarTypeList = this;
        if (hitbarData != null) {
            hitbarType.decode(new Packet(hitbarData));
        }
        synchronized (this.hitbarCache) {
            this.hitbarCache.put(hitbarType, id);
            return hitbarType;
        }
    }

    public void resetCaches() {
        synchronized (this.hitbarCache) {
            this.hitbarCache.reset();
        }
        synchronized (this.spriteCache) {
            this.spriteCache.reset();
        }
    }

    public void processCycle() {
        synchronized (this.hitbarCache) {
            this.hitbarCache.clean(5);
        }
        synchronized (this.spriteCache) {
            this.spriteCache.clean(5);
        }
    }

    public void clearQueues() {
        synchronized (this.hitbarCache) {
            this.hitbarCache.resize();
        }
        synchronized (this.spriteCache) {
            this.spriteCache.resize();
        }
    }
}
