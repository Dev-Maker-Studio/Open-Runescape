package com.jagex.game.runetek5.config.varp;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class VarpbitTypeList {

    public LRUCache cache = new LRUCache(64);

    public final Js5ResourceProvider varbitResource;

    public VarpbitTypeList(Js5ResourceProvider resourceProvider) {
        this.varbitResource = resourceProvider;
        if (this.varbitResource != null) {
            int lastArchiveId = this.varbitResource.getArchiveCount() - 1;
            SharedConfigsType.VARPBIT.archiveFileCapacity();
            this.varbitResource.getArchiveFileCount(lastArchiveId);
        }
    }

    public VarpbitType list(int id) {
        VarpbitType varbit;
        synchronized (this.cache) {
            varbit = (VarpbitType) this.cache.get(id);
        }
        if (varbit != null) {
            return varbit;
        }
        byte[] varbitData;
        synchronized (this.varbitResource) {
            varbitData = this.varbitResource.getFile(SharedConfigsType.VARPBIT.archiveId(id), SharedConfigsType.VARPBIT.archiveFileId(id));
        }
        varbit = new VarpbitType();
        if (varbitData != null) {
            varbit.decode(new Packet(varbitData));
        }
        synchronized (this.cache) {
            this.cache.put(varbit, id);
            return varbit;
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

    public void resetCache() {
        synchronized (this.cache) {
            this.cache.reset();
        }
    }

    public void resetCache(int capacity) {
        synchronized (this.cache) {
            this.cache.reset();
            this.cache = new LRUCache(capacity);
        }
    }
}
