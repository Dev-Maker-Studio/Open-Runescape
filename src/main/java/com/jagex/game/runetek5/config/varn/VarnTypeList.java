package com.jagex.game.runetek5.config.varn;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class VarnTypeList {

    public final LRUCache cache = new LRUCache(64);

    public final Js5ResourceProvider configResource;

    public VarnTypeList(Js5ResourceProvider configResource) {
        this.configResource = configResource;
        if (this.configResource != null) {
        	this.configResource.getArchiveFileCount(SharedConfigsType.VARN.id);
        }
    }

    public VarnType list(int id) {
        VarnType varnType;
        synchronized (this.cache) {
            varnType = (VarnType) this.cache.get(id);
        }
        if (varnType != null) {
            return varnType;
        } 
        byte[] data = this.configResource.getFile(SharedConfigsType.VARN.id, id);
        varnType = new VarnType();
        if (data != null) {
            varnType.decode(new Packet(data));
        }
        synchronized (this.cache) {
            this.cache.put(varnType, id);
            return varnType;
        }
    }

    public void sweep() {
        synchronized (this.cache) {
            this.cache.clean(5);
        }
    }

    public void clearQueue() {
        synchronized (this.cache) {
            this.cache.resize();
        }
    }

    public void reset() {
        synchronized (this.cache) {
            this.cache.reset();
        }
    }
}
