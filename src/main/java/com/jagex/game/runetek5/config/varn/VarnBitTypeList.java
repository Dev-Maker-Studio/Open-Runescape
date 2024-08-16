package com.jagex.game.runetek5.config.varn;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

/**
 * 
 * @author Brainpower - 25/03/2023
 *
 */
public class VarnBitTypeList {

    public final LRUCache cache = new LRUCache(64);

    public final Js5ResourceProvider varnbitProvider;

    public VarnBitTypeList(Js5ResourceProvider varnbitProvider) {
        this.varnbitProvider = varnbitProvider;
        if (this.varnbitProvider != null) {
        	this.varnbitProvider.getArchiveFileCount(SharedConfigsType.VARNBIT.id);
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

    public VarnBitType list(int id) {
        VarnBitType varnbitType;
        synchronized (this.cache) {
            varnbitType = (VarnBitType) this.cache.get(id);
        }
        if (varnbitType != null) {
            return varnbitType;
        } 
        byte[] data = this.varnbitProvider.getFile(SharedConfigsType.VARNBIT.id, id);
        varnbitType = new VarnBitType();
        if (data != null) {
            varnbitType.decode(new Packet(data));
        }
        synchronized (this.cache) {
            this.cache.put(varnbitType, id);
            return varnbitType;
        }
    }
}
