package com.jagex.game.runetek5.config.identitykit;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

/**
 * < Mostly refactored >
 * @author Brainpower - 30/05/2020
 * 
 * Manages #IdkType
 *
 */
public class IdkTypeList {

    public final LRUCache cache = new LRUCache(64);
    public final Js5ResourceProvider identityResource;
    final Js5ResourceProvider modelResource;

    public IdkTypeList(Js5ResourceProvider identityResource, Js5ResourceProvider modelResource) {
        this.identityResource = identityResource;
        this.modelResource = modelResource;
        this.identityResource.getArchiveFileCount(SharedConfigsType.IDENTITY_KIT.id);
    }

    public IdkType list(int identityId) {
        IdkType identityType;
        synchronized (this.cache) {
            identityType = (IdkType) this.cache.get(identityId);
        }
        if (identityType != null) {
            return identityType;
        } 
        byte[] data;
        synchronized (this.identityResource) {
            data = this.identityResource.getFile(SharedConfigsType.IDENTITY_KIT.id, identityId);
        }
        identityType = new IdkType();
        identityType.identityTypeList = this;
        if (data != null) {
            identityType.decode(new Packet(data));
        }
        synchronized (this.cache) {
        	this.cache.put(identityType, identityId);
            return identityType;
        }
    }

    public void reset() {
        synchronized (this.cache) {
        	this.cache.reset();
        }
    }

    public void cycleCache() {
        synchronized (this.cache) {
        	this.cache.clean(5);
        }
    }

    public void remove() {
        synchronized (this.cache) {
        	this.cache.resize();
        }
    }
}
