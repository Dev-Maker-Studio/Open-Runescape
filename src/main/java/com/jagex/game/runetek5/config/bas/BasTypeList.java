package com.jagex.game.runetek5.config.bas;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.config.SharedConfigsType;
import com.jagex.game.runetek5.config.defaults.EquipmentDefaultsType;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;

public class BasTypeList {

    public final LRUCache basCache = new LRUCache(64);

    public final Js5ResourceProvider basResource;

    public final EquipmentDefaultsType equipmentDefaults;

    public BasTypeList(Js5ResourceProvider basResource, EquipmentDefaultsType equipmentDefaults) {
        this.basResource = basResource;
        this.basResource.getArchiveFileCount(SharedConfigsType.RENDER_ANIMS.id);
        this.equipmentDefaults = equipmentDefaults;
    }

    public BasType list(int basFileId) {
        BasType bastype;
        synchronized (this.basCache) {
            bastype = (BasType) this.basCache.get(basFileId);
        }
        if (bastype != null) {
            return bastype;
        } 
        byte[] basData;
        synchronized (this.basResource) {
            basData = this.basResource.getFile(SharedConfigsType.RENDER_ANIMS.id, basFileId);
        }
        bastype = new BasType();
        bastype.basTypeList = this;
        if (basData != null) {
            bastype.decode(new Packet(basData));
        }
        synchronized (this.basCache) {
            this.basCache.put(bastype, basFileId);
            return bastype;
        }
    }

    public void cleanCache() {
        synchronized (this.basCache) {
            this.basCache.clean(5);
        }
    }
    
    public void resetCache() {
        synchronized (this.basCache) {
            this.basCache.reset();
        }
    }

    public void resizeCache() {
        synchronized (this.basCache) {
            this.basCache.resize();
        }
    }
}
