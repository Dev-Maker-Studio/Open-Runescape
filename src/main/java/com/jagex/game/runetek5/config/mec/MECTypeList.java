package com.jagex.game.runetek5.config.mec;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

/**
 * Map area
 * @author Brainpower
 *
 */
public class MECTypeList {

    public LRUCache definitionCache = new LRUCache(128);
    LRUCache spriteCache = new LRUCache(64);
    public final Js5ResourceProvider definitionResource;
    final Js5ResourceProvider spriteResource;

    public MECTypeList(Js5ResourceProvider mectypeDefinitionResource, Js5ResourceProvider spriteResource) {
        this.definitionResource = mectypeDefinitionResource;
        this.spriteResource = spriteResource;
        this.definitionResource.getArchiveFileCount(SharedConfigsType.MAP_AREAS.id);
    }

    public void sweepCache() {
        synchronized (this.definitionCache) {
            this.definitionCache.clean(5);
        }
        synchronized (this.spriteCache) {
            this.spriteCache.clean(5);
        }
    }

    public void clearCacheQueue() {
        synchronized (this.definitionCache) {
            this.definitionCache.resize();
        }
        synchronized (this.spriteCache) {
            this.spriteCache.resize();
        }
    }

    public void createCache(int defintionCapacity, int spriteCapacity) {
        this.definitionCache = new LRUCache(defintionCapacity);
        this.spriteCache = new LRUCache(spriteCapacity);
    }

    public void resetCache() {
        synchronized (this.definitionCache) {
            this.definitionCache.reset();
        }
        synchronized (this.spriteCache) {
            this.spriteCache.reset();
        }
    }

    public MECType list(int id) {
        MECType mectype;
        synchronized (this.definitionCache) {
            mectype = (MECType) this.definitionCache.get(id);
        }

        if (mectype != null) {
            return mectype;
        } 
        byte[] data;
        synchronized (this.definitionResource) {
            data = this.definitionResource.getFile(SharedConfigsType.MAP_AREAS.id, id);
        }

        mectype = new MECType();
        mectype.mectypeId = id;
        mectype.initialType = this;
        if (data != null) {
            mectype.decode(new Packet(data));
        }

        mectype.postDecode();
        synchronized (this.definitionCache) {
            this.definitionCache.put(mectype, id);
            return mectype;
        }
    }

}
