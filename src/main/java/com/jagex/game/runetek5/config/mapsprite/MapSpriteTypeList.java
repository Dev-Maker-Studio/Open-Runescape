package com.jagex.game.runetek5.config.mapsprite;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.loc.LocType;
import com.jagex.game.runetek5.entity.gameobject.GameObject;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class MapSpriteTypeList {

	/**
	 * Definitions about map sprites
	 */
    public LRUCache mapspriteTypeCache = new LRUCache(64);
    
    /**
     * The drawable map sprite data
     */
    LRUCache mapspriteCache = new LRUCache(64);
    public final Js5ResourceProvider mapspriteTypeResource;
    final Js5ResourceProvider mapspriteResource;

    public MapSpriteTypeList(Js5ResourceProvider mapspriteTypeResource, Js5ResourceProvider mapspriteResource) {
        this.mapspriteTypeResource = mapspriteTypeResource;
        this.mapspriteResource = mapspriteResource;
        this.mapspriteTypeResource.getArchiveFileCount(SharedConfigsType.MAP_SPRITES.id);
    }

    public static boolean isCached(GameObject gameObject) {
        LocType locType = Resource.mapRegion.getLoctypeList().list(gameObject.getId());
        if (locType.mapIconId == -1) {
            return true;
        } 
        MapSpriteType mapSpriteType = TypeLists.MAP_SPRITE.list(locType.mapIconId);
        return mapSpriteType.fileId == -1 || mapSpriteType.isCached();
    }

    public MapSpriteType list(int mapspriteId) {
        MapSpriteType mapspriteType;
        synchronized (this.mapspriteTypeCache) {
            mapspriteType = (MapSpriteType) this.mapspriteTypeCache.get(mapspriteId);
        }
        if (mapspriteType != null) {
            return mapspriteType;
        } 
        byte[] data;
        synchronized (this.mapspriteTypeResource) {
            data = this.mapspriteTypeResource.getFile(SharedConfigsType.MAP_SPRITES.id, mapspriteId);
        }
        mapspriteType = new MapSpriteType();
        mapspriteType.mapspriteTypeList = this;
        if (data != null) {
            mapspriteType.decode(new Packet(data));
        }
        synchronized (this.mapspriteTypeCache) {
            this.mapspriteTypeCache.put(mapspriteType, mapspriteId);
            return mapspriteType;
        }
    }

    public void sweepCaches() {
        synchronized (this.mapspriteTypeCache) {
            this.mapspriteTypeCache.clean(5);
        }
        synchronized (this.mapspriteCache) {
            this.mapspriteCache.clean(5);
        }
    }

    public void resetCaches() {
        synchronized (this.mapspriteTypeCache) {
            this.mapspriteTypeCache.reset();
        }
        synchronized (this.mapspriteCache) {
            this.mapspriteCache.reset();
        }
    }

    public void resetCaches(int typeCapacity, int spriteCapacity) {
        this.mapspriteTypeCache = new LRUCache(typeCapacity);
        this.mapspriteCache = new LRUCache(spriteCapacity);
    }

    public void clearCacheQueues() {
        synchronized (this.mapspriteTypeCache) {
            this.mapspriteTypeCache.resize();
        }
        synchronized (this.mapspriteCache) {
            this.mapspriteCache.resize();
        }
    }
}
