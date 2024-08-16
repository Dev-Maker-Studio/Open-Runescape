package com.jagex.game.runetek5.config.param;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class ParamTypeList {

    public final LRUCache cache = new LRUCache(64);
    public final Js5ResourceProvider paramResource;

    public ParamTypeList(Js5ResourceProvider paramsResource) {
    	this.paramResource = paramsResource;
        if (this.paramResource != null) {
        	this.paramResource.getArchiveFileCount(SharedConfigsType.PARAMS.id);
        }
    }

    public void clearCacheQueue() {
        synchronized (this.cache) {
        	this.cache.resize();
        }
    }

    public void sweepCache() {
        synchronized (this.cache) {
        	this.cache.clean(5);
        }
    }

    public ParamType list(int id) {
        ParamType paramType;
        synchronized (this.cache) {
            paramType = (ParamType) this.cache.get(id);
        }
        if (paramType != null) {
            return paramType;
        } 
        byte[] data;
        synchronized (this.paramResource) {
            data = this.paramResource.getFile(SharedConfigsType.PARAMS.id, id);
        }
        paramType = new ParamType();
        if (data != null) {
            paramType.decode(new Packet(data));
        }
        synchronized (this.cache) {
        	this.cache.put(paramType, id);
            return paramType;
        }
    }

    public void resetCache() {
        synchronized (this.cache) {
        	this.cache.reset();
        }
    }
}
