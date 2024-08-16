package com.jagex.game.runetek5.config.sequence.spot;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class SpotAnimTypeList {

    public final LRUCache definitionCache = new LRUCache(64);

    final LRUCache modelCache = new LRUCache(60);

    public final Js5ResourceProvider gfxResource;

    final Js5ResourceProvider modelResource;

    int preferenceMask;

    public SpotAnimTypeList(Js5ResourceProvider gfxResource, Js5ResourceProvider modelResource) {
        this.gfxResource = gfxResource;
        this.modelResource = modelResource;
        int archiveCount = gfxResource.getArchiveCount() - 1;
        SharedConfigsType.GFX.archiveFileCapacity();
        this.gfxResource.getArchiveFileCount(archiveCount);
    }

    public SpotAnimType list(int id) {
        SpotAnimType gfxType;
        synchronized (this.definitionCache) {
            gfxType = (SpotAnimType) this.definitionCache.get(id);
        }
        if (gfxType != null) {
            return gfxType;
        } 
        byte[] gfxData;
        synchronized (this.gfxResource) {
            gfxData = this.gfxResource.getFile(SharedConfigsType.GFX.archiveId(id), SharedConfigsType.GFX.archiveFileId(id));
        }
        gfxType = new SpotAnimType();
        gfxType.gfxTypeList = this;
        gfxType.currentId = id;
        if (gfxData != null) {
            gfxType.decode(new Packet(gfxData));
        }
        synchronized (this.definitionCache) {
        	this.definitionCache.put(gfxType, id);
            return gfxType;
        }
    }

    public void setPreferenceMask(int preferenceMask) {
        this.preferenceMask = preferenceMask;
        synchronized (this.modelCache) {
        	this.modelCache.reset();
        }
    }

    public void sweepCache() {
        synchronized (this.definitionCache) {
        	this.definitionCache.clean(5);
        }
        synchronized (this.modelCache) {
        	this.modelCache.clean(5);
        }
    }

    public void resetCache() {
        synchronized (this.definitionCache) {
        	this.definitionCache.reset();
        }
        synchronized (this.modelCache) {
        	this.modelCache.reset();
        }
    }

    public void clearCacheQueue() {
        synchronized (this.definitionCache) {
        	this.definitionCache.resize();
        }
        synchronized (this.modelCache) {
        	this.modelCache.resize();
        }
    }
}
